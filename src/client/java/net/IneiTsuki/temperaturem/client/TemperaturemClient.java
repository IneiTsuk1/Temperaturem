package net.IneiTsuki.temperaturem.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class TemperaturemClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TemperatureClientNetworking.registerReceiver();
        HudRenderCallback.EVENT.register(new TemperatureHudRenderer());
    }
}
