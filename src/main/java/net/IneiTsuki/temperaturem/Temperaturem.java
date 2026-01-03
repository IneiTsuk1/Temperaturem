package net.IneiTsuki.temperaturem;

import net.IneiTsuki.temperaturem.config.TemperatureEffectsConfig;
import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.IneiTsuki.temperaturem.items.ModItems;
import net.IneiTsuki.temperaturem.player.PlayerTemperatureManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Temperaturem implements ModInitializer {
    public static final String MOD_ID = "temperaturem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("=== TemperatureMod INITIALIZING ===");

        // Register items FIRST (before recipes are loaded)
        ModItems.initialize();
        // Register both block and biome temperature registries
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new TemperatureRegistry());

        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new BiomeTemperatureRegistry());

        // Initialize player temperature manager
        PlayerTemperatureManager.init();

        // Load effects configuration
        TemperatureEffectsConfig.load();

        LOGGER.info("=== TemperatureMod initialized successfully ===");
    }
}