package net.IneiTsuki.temperaturem;

import net.IneiTsuki.temperaturem.commands.SeasonCommand;
import net.IneiTsuki.temperaturem.commands.ZoneCommand;
import net.IneiTsuki.temperaturem.config.TemperatureEffectsConfig;
import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.IneiTsuki.temperaturem.items.ModItems;
import net.IneiTsuki.temperaturem.player.PlayerTemperatureManager;
import net.IneiTsuki.temperaturem.seasons.SeasonConfig;
import net.IneiTsuki.temperaturem.seasons.SeasonManager;
import net.IneiTsuki.temperaturem.zones.TemperatureZoneManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Temperaturem implements ModInitializer {
    public static final String MOD_ID = "temperaturem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static SeasonConfig SEASON_CONFIG;

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

        SEASON_CONFIG = SeasonConfig.load();

        // Register zone commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ZoneCommand.register(dispatcher);
            SeasonCommand.register(dispatcher);
        });

        // Tick zone managers for cache management
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var world : server.getWorlds()) {
                TemperatureZoneManager manager = TemperatureZoneManager.get(world);
                manager.tick();

                SeasonManager seasonManager =  SeasonManager.get(world);
                seasonManager.tick();
            }
        });

        // Clear zone manager instances on server stop
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TemperatureZoneManager.clearInstances();
            SeasonManager.clearInstances();
        });

        // Initialize player temperature manager
        PlayerTemperatureManager.init();

        // Load effects configuration
        TemperatureEffectsConfig.load();

        LOGGER.info("=== TemperatureMod initialized successfully ===");
    }

    public static SeasonConfig getSeasonConfig() {
        return SEASON_CONFIG;
    }
}