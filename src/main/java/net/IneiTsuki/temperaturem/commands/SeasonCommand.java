package net.IneiTsuki.temperaturem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.IneiTsuki.temperaturem.api.TemperatureAPI;
import net.IneiTsuki.temperaturem.seasons.Season;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Commands for managing seasons.
 * Requires operator permissions (level 2).
 */
public class SeasonCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("season")
                .requires(source -> source.hasPermissionLevel(2))

                // /season get
                .then(CommandManager.literal("get")
                        .executes(SeasonCommand::getSeason))

                // /season set <season>
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("season", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("SPRING");
                                    builder.suggest("SUMMER");
                                    builder.suggest("AUTUMN");
                                    builder.suggest("WINTER");
                                    return builder.buildFuture();
                                })
                                .executes(SeasonCommand::setSeason)))

                // /season next
                .then(CommandManager.literal("next")
                        .executes(SeasonCommand::nextSeason))

                // /season info
                .then(CommandManager.literal("info")
                        .executes(SeasonCommand::seasonInfo))

                // /season toggle
                .then(CommandManager.literal("toggle")
                        .executes(SeasonCommand::toggleSeasons))

                // /season enable <true|false>
                .then(CommandManager.literal("enable")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(SeasonCommand::enableSeasons)))

                // /season length <days>
                .then(CommandManager.literal("length")
                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 1000))
                                .executes(SeasonCommand::setSeasonLength)))

                // /season notify <true|false>
                .then(CommandManager.literal("notify")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(SeasonCommand::setNotify)))
        );
    }

    private static int getSeason(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        Season season = TemperatureAPI.getInstance().getCurrentSeason(world);
        double progress = TemperatureAPI.getInstance().getSeasonProgress(world);
        int remaining = TemperatureAPI.getInstance().getRemainingDays(world);

        source.sendFeedback(() -> Text.literal("Current Season: ")
                .formatted(Formatting.GRAY)
                .append(season.toText()), false);

        source.sendFeedback(() -> Text.literal(String.format("Progress: %.1f%% (%d days remaining)",
                progress * 100, remaining)).formatted(Formatting.GRAY), false);

        return 1;
    }

    private static int setSeason(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            String seasonStr = StringArgumentType.getString(context, "season");

            Season season;
            try {
                season = Season.valueOf(seasonStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                source.sendError(Text.literal("Invalid season. Use: SPRING, SUMMER, AUTUMN, or WINTER"));
                return 0;
            }

            TemperatureAPI.getInstance().setCurrentSeason(world, season);

            source.sendFeedback(() -> Text.literal("Season set to: ")
                    .formatted(Formatting.GREEN)
                    .append(season.toText()), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to set season: " + e.getMessage()));
            return 0;
        }
    }

    private static int nextSeason(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        Season oldSeason = TemperatureAPI.getInstance().getCurrentSeason(world);
        TemperatureAPI.getInstance().skipToNextSeason(world);
        Season newSeason = TemperatureAPI.getInstance().getCurrentSeason(world);

        source.sendFeedback(() -> Text.literal("Advanced season: ")
                .formatted(Formatting.GREEN)
                .append(oldSeason.toText())
                .append(Text.literal(" → ").formatted(Formatting.GRAY))
                .append(newSeason.toText()), true);

        return 1;
    }

    private static int seasonInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        Season season = TemperatureAPI.getInstance().getCurrentSeason(world);
        boolean enabled = TemperatureAPI.getInstance().isSeasonsEnabled(world);
        int lengthDays = TemperatureAPI.getInstance().getSeasonLengthDays(world);
        double progress = TemperatureAPI.getInstance().getSeasonProgress(world);
        int remaining = TemperatureAPI.getInstance().getRemainingDays(world);
        double tempMod = TemperatureAPI.getInstance().getSeasonalTemperatureModifier(world);
        double tempMult = TemperatureAPI.getInstance().getSeasonalTemperatureMultiplier(world);
        boolean notify = TemperatureAPI.getInstance().isNotifySeasonChange(world);

        source.sendFeedback(() -> Text.literal("=== Season Info ===")
                .formatted(Formatting.GOLD, Formatting.BOLD), false);

        source.sendFeedback(() -> Text.literal("Current Season: ")
                .formatted(Formatting.GRAY)
                .append(season.toText()), false);

        source.sendFeedback(() -> Text.literal("Description: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(season.getDescription()).formatted(Formatting.WHITE)), false);

        source.sendFeedback(() -> Text.literal("Status: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(enabled ? "Enabled" : "Disabled")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED)), false);

        source.sendFeedback(() -> Text.literal("Season Length: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(lengthDays + " days").formatted(Formatting.WHITE)), false);

        source.sendFeedback(() -> Text.literal("Progress: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(String.format("%.1f%% (%d/%d days)",
                                progress * 100, lengthDays - remaining, lengthDays))
                        .formatted(Formatting.WHITE)), false);

        source.sendFeedback(() -> Text.literal("Temperature Effects:")
                .formatted(Formatting.GRAY), false);

        source.sendFeedback(() -> Text.literal("  Modifier: ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(String.format("%+.1f°C", tempMod))
                        .formatted(tempMod >= 0 ? Formatting.RED : Formatting.AQUA)), false);

        source.sendFeedback(() -> Text.literal("  Multiplier: ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(String.format("×%.2f", tempMult))
                        .formatted(Formatting.WHITE)), false);

        source.sendFeedback(() -> Text.literal("Notifications: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(notify ? "On" : "Off")
                        .formatted(notify ? Formatting.GREEN : Formatting.RED)), false);

        return 1;
    }

    private static int toggleSeasons(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();

        boolean current = TemperatureAPI.getInstance().isSeasonsEnabled(world);
        boolean newState = !current;

        TemperatureAPI.getInstance().setSeasonsEnabled(world, newState);

        source.sendFeedback(() -> Text.literal("Seasons ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(newState ? "enabled" : "disabled")
                        .formatted(newState ? Formatting.GREEN : Formatting.RED)), true);

        return 1;
    }

    private static int enableSeasons(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        TemperatureAPI.getInstance().setSeasonsEnabled(world, enabled);

        source.sendFeedback(() -> Text.literal("Seasons ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(enabled ? "enabled" : "disabled")
                        .formatted(enabled ? Formatting.GREEN : Formatting.RED)), true);

        return 1;
    }

    private static int setSeasonLength(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerWorld world = source.getWorld();
            int days = IntegerArgumentType.getInteger(context, "days");

            TemperatureAPI.getInstance().setSeasonLengthDays(world, days);

            source.sendFeedback(() -> Text.literal("Season length set to " + days + " days")
                    .formatted(Formatting.GREEN), true);

            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
    }

    private static int setNotify(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        boolean notify = BoolArgumentType.getBool(context, "enabled");

        TemperatureAPI.getInstance().setNotifySeasonChange(world, notify);

        source.sendFeedback(() -> Text.literal("Season change notifications ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(notify ? "enabled" : "disabled")
                        .formatted(notify ? Formatting.GREEN : Formatting.RED)), true);

        return 1;
    }
}