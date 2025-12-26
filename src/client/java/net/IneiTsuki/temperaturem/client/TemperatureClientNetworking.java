package net.IneiTsuki.temperaturem.client;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

public class TemperatureClientNetworking {

    public static final Identifier UPDATE_TEMP_PACKET = new Identifier("temperaturem", "update_temp");

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(UPDATE_TEMP_PACKET, (client, handler, buf, responseSender) -> {
            try {
                int temp = buf.readInt();

                // Run on client thread to ensure thread safety
                client.execute(() -> {
                    ClientPlayerTemperature.set(temp);
                });
            } catch (Exception e) {
                Temperaturem.LOGGER.error("Failed to read temperature packet", e);
            }
        });
    }
}