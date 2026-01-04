package net.IneiTsuki.temperaturem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.IneiTsuki.temperaturem.api.TemperatureAPI;
import net.IneiTsuki.temperaturem.zones.TemperatureZone;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Commands for managing temperature zones.
 * Requires operator permissions (level 2).
 */
public class ZoneCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tempzone")
                .requires(source -> source.hasPermissionLevel(2))

                // /tempzone create <name> <pos1> <pos2> <temperature> <type>
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.argument("pos1", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("pos2", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.argument("temperature", DoubleArgumentType.doubleArg(-273, 1000))
                                                        .then(CommandManager.argument("type", StringArgumentType.word())
                                                                .suggests((context, builder) -> {
                                                                    builder.suggest("ADDITIVE");
                                                                    builder.suggest("ABSOLUTE");
                                                                    builder.suggest("MULTIPLIER");
                                                                    return builder.buildFuture();
                                                                })
                                                                .executes(ZoneCommand::createZone)))))))

                // /tempzone remove <zone_id>
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("zone_id", StringArgumentType.word())
                                .executes(ZoneCommand::removeZone)))

                // /tempzone list
                .then(CommandManager.literal("list")
                        .executes(ZoneCommand::listZones))

                // /tempzone info <zone_id>
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("zone_id", StringArgumentType.word())
                                .executes(ZoneCommand::zoneInfo)))

                // /tempzone find <name>
                .then(CommandManager.literal("find")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ZoneCommand::findZones)))

                // /tempzone settemp <zone_id> <temperature>
                .then(CommandManager.literal("settemp")
                        .then(CommandManager.argument("zone_id", StringArgumentType.word())
                                .then(CommandManager.argument("temperature", DoubleArgumentType.doubleArg(-273, 1000))
                                        .executes(ZoneCommand::setTemperature))))

                // /tempzone setpriority <zone_id> <priority>
                .then(CommandManager.literal("setpriority")
                        .then(CommandManager.argument("zone_id", StringArgumentType.word())
                                .then(CommandManager.argument("priority", IntegerArgumentType.integer())
                                        .executes(ZoneCommand::setPriority))))

                // /tempzone toggle <zone_id>
                .then(CommandManager.literal("toggle")
                        .then(CommandManager.argument("zone_id", StringArgumentType.word())
                                .executes(ZoneCommand::toggleZone)))

                // /tempzone here
                .then(CommandManager.literal("here")
                        .executes(ZoneCommand::zonesHere))
        );
    }

    private static int createZone(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String name = StringArgumentType.getString(context, "name");
            BlockPos pos1 = BlockPosArgumentType.getBlockPos(context, "pos1");
            BlockPos pos2 = BlockPosArgumentType.getBlockPos(context, "pos2");
            double temperature = DoubleArgumentType.getDouble(context, "temperature");
            String typeStr = StringArgumentType.getString(context, "type");

            TemperatureZone.ZoneType type;
            try {
                type = TemperatureZone.ZoneType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                source.sendError(Text.literal("Invalid zone type. Use: ADDITIVE, ABSOLUTE, or MULTIPLIER"));
                return 0;
            }

            TemperatureZone zone = TemperatureAPI.getInstance().createZone(world, name, pos1, pos2, temperature, type);

            source.sendFeedback(() -> Text.literal("Created temperature zone: ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(zone.getName()).formatted(Formatting.AQUA))
                    .append(Text.literal(" (ID: " + zone.getId() + ")").formatted(Formatting.GRAY)), true);

            source.sendFeedback(() -> Text.literal("Temperature: " + temperature + "°C, Type: " + type)
                    .formatted(Formatting.GRAY), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to create zone: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeZone(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String zoneIdStr = StringArgumentType.getString(context, "zone_id");
            UUID zoneId = UUID.fromString(zoneIdStr);

            TemperatureZone zone = TemperatureAPI.getInstance().getZone(world, zoneId);
            if (zone == null) {
                source.sendError(Text.literal("Zone not found: " + zoneIdStr));
                return 0;
            }

            String zoneName = zone.getName();
            boolean removed = TemperatureAPI.getInstance().removeZone(world, zoneId);

            if (removed) {
                source.sendFeedback(() -> Text.literal("Removed zone: " + zoneName)
                        .formatted(Formatting.GREEN), true);
                return 1;
            } else {
                source.sendError(Text.literal("Failed to remove zone"));
                return 0;
            }
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Invalid zone ID format"));
            return 0;
        }
    }

    private static int listZones(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        var zones = TemperatureAPI.getInstance().getAllZones(world);

        if (zones.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No temperature zones in this world")
                    .formatted(Formatting.YELLOW), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("=== Temperature Zones ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);

        for (TemperatureZone zone : zones) {
            Formatting statusColor = zone.isEnabled() ? Formatting.GREEN : Formatting.RED;
            String status = zone.isEnabled() ? "✓" : "✗";

            source.sendFeedback(() -> Text.literal(status + " ")
                    .formatted(statusColor)
                    .append(Text.literal(zone.getName()).formatted(Formatting.AQUA))
                    .append(Text.literal(" - " + zone.getTemperature() + "°C").formatted(Formatting.WHITE))
                    .append(Text.literal(" [" + zone.getType() + "]").formatted(Formatting.GRAY))
                    .append(Text.literal("\n  ID: " + zone.getId()).formatted(Formatting.DARK_GRAY)), false);
        }

        source.sendFeedback(() -> Text.literal("Total: " + zones.size() + " zones")
                .formatted(Formatting.GRAY), false);

        return zones.size();
    }

    private static int zoneInfo(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String zoneIdStr = StringArgumentType.getString(context, "zone_id");
            UUID zoneId = UUID.fromString(zoneIdStr);

            TemperatureZone zone = TemperatureAPI.getInstance().getZone(world, zoneId);
            if (zone == null) {
                source.sendError(Text.literal("Zone not found: " + zoneIdStr));
                return 0;
            }

            source.sendFeedback(() -> Text.literal("=== Zone Info ===")
                    .formatted(Formatting.GOLD, Formatting.BOLD), false);

            source.sendFeedback(() -> Text.literal("Name: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getName()).formatted(Formatting.AQUA)), false);

            source.sendFeedback(() -> Text.literal("ID: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getId().toString()).formatted(Formatting.WHITE)), false);

            source.sendFeedback(() -> Text.literal("Temperature: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getTemperature() + "°C").formatted(Formatting.WHITE)), false);

            source.sendFeedback(() -> Text.literal("Type: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getType().toString()).formatted(Formatting.WHITE)), false);

            source.sendFeedback(() -> Text.literal("Priority: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(String.valueOf(zone.getPriority())).formatted(Formatting.WHITE)), false);

            source.sendFeedback(() -> Text.literal("Status: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.isEnabled() ? "Enabled" : "Disabled")
                            .formatted(zone.isEnabled() ? Formatting.GREEN : Formatting.RED)), false);

            source.sendFeedback(() -> Text.literal("Transition Range: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getTransitionRange() + " blocks").formatted(Formatting.WHITE)), false);

            var bounds = zone.getBounds();
            source.sendFeedback(() -> Text.literal("Bounds: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(String.format("(%.0f, %.0f, %.0f) to (%.0f, %.0f, %.0f)",
                                    bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ))
                            .formatted(Formatting.WHITE)), false);

            source.sendFeedback(() -> Text.literal("Volume: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getVolume() + " blocks³").formatted(Formatting.WHITE)), false);

            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Invalid zone ID format"));
            return 0;
        }
    }

    private static int findZones(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        String query = StringArgumentType.getString(context, "name");

        List<TemperatureZone> zones = TemperatureAPI.getInstance().findZonesByName(world, query);

        if (zones.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No zones found matching: " + query)
                    .formatted(Formatting.YELLOW), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Found " + zones.size() + " zone(s) matching '" + query + "':")
                .formatted(Formatting.GREEN), false);

        for (TemperatureZone zone : zones) {
            source.sendFeedback(() -> Text.literal("• ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getName()).formatted(Formatting.AQUA))
                    .append(Text.literal(" (ID: " + zone.getId() + ")").formatted(Formatting.DARK_GRAY)), false);
        }

        return zones.size();
    }

    private static int setTemperature(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String zoneIdStr = StringArgumentType.getString(context, "zone_id");
            UUID zoneId = UUID.fromString(zoneIdStr);
            double temperature = DoubleArgumentType.getDouble(context, "temperature");

            TemperatureAPI.getInstance().setZoneTemperature(world, zoneId, temperature);

            source.sendFeedback(() -> Text.literal("Updated zone temperature to " + temperature + "°C")
                    .formatted(Formatting.GREEN), true);

            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
    }

    private static int setPriority(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String zoneIdStr = StringArgumentType.getString(context, "zone_id");
            UUID zoneId = UUID.fromString(zoneIdStr);
            int priority = IntegerArgumentType.getInteger(context, "priority");

            TemperatureAPI.getInstance().setZonePriority(world, zoneId, priority);

            source.sendFeedback(() -> Text.literal("Updated zone priority to " + priority)
                    .formatted(Formatting.GREEN), true);

            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
    }

    private static int toggleZone(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String zoneIdStr = StringArgumentType.getString(context, "zone_id");
            UUID zoneId = UUID.fromString(zoneIdStr);

            TemperatureZone zone = TemperatureAPI.getInstance().getZone(world, zoneId);
            if (zone == null) {
                source.sendError(Text.literal("Zone not found"));
                return 0;
            }

            boolean newState = !zone.isEnabled();
            TemperatureAPI.getInstance().setZoneEnabled(world, zoneId, newState);

            source.sendFeedback(() -> Text.literal("Zone " + zone.getName() + " is now ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(newState ? "enabled" : "disabled")
                            .formatted(newState ? Formatting.GREEN : Formatting.RED)), true);

            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
    }

    private static int zonesHere(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());

        List<TemperatureZone> zones = TemperatureAPI.getInstance().getZonesAt(world, pos);

        if (zones.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No temperature zones at this location")
                    .formatted(Formatting.YELLOW), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Temperature zones at your location:")
                .formatted(Formatting.GREEN), false);

        for (TemperatureZone zone : zones) {
            double tempEffect = zone.getTemperatureAt(pos);
            source.sendFeedback(() -> Text.literal("• ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(zone.getName()).formatted(Formatting.AQUA))
                    .append(Text.literal(" - " + tempEffect + "°C").formatted(Formatting.WHITE))
                    .append(Text.literal(" [Priority: " + zone.getPriority() + "]").formatted(Formatting.GRAY)), false);
        }

        Double effectiveTemp = TemperatureAPI.getInstance().getZoneTemperatureAt(world, pos);
        if (effectiveTemp != null) {
            source.sendFeedback(() -> Text.literal("Effective temperature: " +
                    String.format("%.1f°C", effectiveTemp)).formatted(Formatting.GOLD), false);
        }

        return zones.size();
    }
}