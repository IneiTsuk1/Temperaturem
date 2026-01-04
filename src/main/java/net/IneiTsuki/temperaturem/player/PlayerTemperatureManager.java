package net.IneiTsuki.temperaturem.player;

import io.netty.buffer.Unpooled;
import net.IneiTsuki.temperaturem.effects.TemperatureEffects;
import net.IneiTsuki.temperaturem.equipment.UnderlayTemperatureManager;
import net.IneiTsuki.temperaturem.util.TemperatureUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTemperatureManager {

    private static final ConcurrentHashMap<UUID, PlayerTemperature> TEMPS = new ConcurrentHashMap<>();

    private static final double BASE_TEMP_CHANGE_RATE = 0.03;
    private static final int UPDATE_INTERVAL = 2;
    private static final int SYNC_INTERVAL = 3;

    private static int tickCounter = 0;
    private static final int TICK_WRAP = UPDATE_INTERVAL * SYNC_INTERVAL * 100;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerTemperatureManager::tick);

        ServerWorldEvents.LOAD.register((server, world) -> {
            TEMPS.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        tickCounter = (tickCounter + 1) % TICK_WRAP;
        boolean shouldUpdate = tickCounter % UPDATE_INTERVAL == 0;
        boolean shouldSync = tickCounter % SYNC_INTERVAL == 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isDead() || player.isSpectator()) {
                continue;
            }

            PlayerTemperature temp = TEMPS.computeIfAbsent(
                    player.getUuid(),
                    id -> new PlayerTemperature()
            );

            if (shouldUpdate) {
                // Calculate base environmental temperature
                double targetTemp = TemperatureUtil.getTargetTemperature(
                        player.getWorld(),
                        player.getBlockPos()
                );

                // Apply armor underlay protection
                double armorProtection = UnderlayTemperatureManager.calculateArmorProtection(player);

                // Protection works by moving target temperature toward comfortable range
                // Positive protection counters cold, negative counters heat
                targetTemp += armorProtection;

                double currentTemp = temp.getExact();
                double delta = targetTemp - currentTemp;
                double distance = Math.abs(delta);
                double changeRate = calculateChangeRate(distance);

                if (distance > 0.01) {
                    double change = Math.signum(delta) * Math.min(distance, changeRate);
                    currentTemp += change;
                    currentTemp = clamp(currentTemp, -50, 150);
                    temp.setExact(currentTemp);
                }

                // Damage underlays based on current temperature conditions
                UnderlayTemperatureManager.damageUnderlays(player, temp.get(), tickCounter);
            }

            // Apply temperature effects every tick
            TemperatureEffects.applyEffects(player, temp.get(), tickCounter);

            if (shouldSync) {
                sendTemperatureToClient(player, temp.get());
            }
        }
    }

    private static double calculateChangeRate(double distance) {
        double changeRate = BASE_TEMP_CHANGE_RATE;

        if (distance > 50) {
            changeRate *= 2.0;
        } else if (distance > 20) {
            double factor = 1.2 + 2.0 * ((distance - 20) / 30.0);
            changeRate *= factor;
        } else if (distance > 10) {
            double factor = 1.1 + 1.0 * ((distance - 10) / 10.0);
            changeRate *= factor;
        } else if (distance > 1) {
            double factor = 1.0 + 1.0 * ((distance - 1) / 9.0);
            changeRate *= factor;
        }

        return changeRate;
    }

    public static void sendTemperatureToClient(ServerPlayerEntity player, int temp) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(temp);
        ServerPlayNetworking.send(player, new Identifier("temperaturem", "update_temp"), buf);
    }

    public static PlayerTemperature get(ServerPlayerEntity player) {
        return TEMPS.computeIfAbsent(player.getUuid(), id -> new PlayerTemperature());
    }

    public static void remove(UUID playerId) {
        TEMPS.remove(playerId);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}