package net.IneiTsuki.temperaturem.player;

import io.netty.buffer.Unpooled;
import net.IneiTsuki.temperaturem.util.TemperatureUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTemperatureManager {

    private static final Map<UUID, PlayerTemperature> TEMPS = new HashMap<>();

    private static final double BASE_TEMP_CHANGE_RATE = 0.1;
    private static final int UPDATE_INTERVAL = 2;
    private static final int SYNC_INTERVAL = 3;

    private static int tickCounter = 0;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerTemperatureManager::tick);

        ServerWorldEvents.LOAD.register((server, world) -> {
            TEMPS.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        tickCounter++;
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
                double targetTemp = TemperatureUtil.getTargetTemperature(
                        player.getWorld(),
                        player.getBlockPos()
                );

                double currentTemp = temp.getExact(); // Use exact double value
                double delta = targetTemp - currentTemp;
                double distance = Math.abs(delta);
                double changeRate = BASE_TEMP_CHANGE_RATE;

                // Adaptive change rate based on distance from target
                if (distance > 50) {
                    changeRate = BASE_TEMP_CHANGE_RATE * 5.0;
                } else if (distance > 20) {
                    changeRate = BASE_TEMP_CHANGE_RATE * 3.0;
                } else if (distance > 10) {
                    changeRate = BASE_TEMP_CHANGE_RATE * 2.0;
                }

                if (Math.abs(delta) > 0.01) { // Lowered threshold for better precision
                    double change = Math.signum(delta) * Math.min(distance, changeRate);
                    currentTemp += change;
                    currentTemp = clamp(currentTemp, -50, 150);
                    temp.setExact(currentTemp); // Store exact value
                }
            }

            if (shouldSync) {
                sendTemperatureToClient(player, temp.get());
            }
        }
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