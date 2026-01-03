package net.IneiTsuki.temperaturem.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.IneiTsuki.temperaturem.Temperaturem;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TemperatureRegistry implements SimpleSynchronousResourceReloadListener {

    // Persistent runtime registrations (survive reloads)
    private static final Map<Identifier, Integer> RUNTIME_TEMPS = new ConcurrentHashMap<>();

    // Loaded from config (cleared on reload)
    private static final Map<Identifier, Integer> BLOCK_TEMPS = new HashMap<>(256);

    private static final String CONFIG_PATH = "config/temperaturem/blocks/temperature_blocks.json";

    @Override
    public Identifier getFabricId() {
        return new Identifier("temperaturem", "temperature_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        BLOCK_TEMPS.clear();
        File configFile = new File(CONFIG_PATH);

        try {
            JsonObject json;

            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                    Temperaturem.LOGGER.info("Loaded block temperatures from config");
                }
            } else {
                InputStream defaultStream = TemperatureRegistry.class.getClassLoader()
                        .getResourceAsStream("data/temperaturem/blocks/temperature_blocks.json");
                if (defaultStream == null) {
                    Temperaturem.LOGGER.error("Default temperature_blocks.json not found in mod jar!");
                    return;
                }
                try (InputStreamReader reader = new InputStreamReader(defaultStream)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }

                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    Temperaturem.LOGGER.error("Failed to create config directory: {}", parentDir.getAbsolutePath());
                    return;
                }

                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(json.toString());
                    Temperaturem.LOGGER.info("Created default config at " + CONFIG_PATH);
                }
            }

            json.entrySet().forEach(entry -> {
                try {
                    IntRange range = IntRange.parse(entry.getValue().getAsString());
                    int avgTemp = range.getAverage();

                    if (avgTemp < -273 || avgTemp > 1000) {
                        Temperaturem.LOGGER.warn("Temperature out of range for block '{}': {}°C",
                                entry.getKey(), avgTemp);
                        return;
                    }

                    BLOCK_TEMPS.put(new Identifier(entry.getKey()), avgTemp);
                } catch (IllegalArgumentException e) {
                    Temperaturem.LOGGER.error("Invalid temperature entry for block '{}': {}",
                            entry.getKey(), entry.getValue(), e);
                } catch (Exception e) {
                    Temperaturem.LOGGER.error("Failed to parse block entry '{}'", entry.getKey(), e);
                }
            });

            Temperaturem.LOGGER.info("Loaded {} block temperature entries ({} runtime)",
                    BLOCK_TEMPS.size(), RUNTIME_TEMPS.size());

        } catch (IOException e) {
            Temperaturem.LOGGER.error("Failed to load block temperatures (IO error)", e);
        } catch (Exception e) {
            Temperaturem.LOGGER.error("Failed to load block temperatures", e);
        }
    }

    /**
     * Get temperature for a block state.
     * Runtime registrations take precedence over config values.
     */
    public static int getTemperature(BlockState state) {
        if (state == null) return 0;
        Identifier id = Registries.BLOCK.getId(state.getBlock());

        // Check runtime first
        if (RUNTIME_TEMPS.containsKey(id)) {
            return RUNTIME_TEMPS.get(id);
        }

        return BLOCK_TEMPS.getOrDefault(id, 0);
    }

    public static boolean hasTemperature(BlockState state) {
        if (state == null) return false;
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return RUNTIME_TEMPS.containsKey(id) || BLOCK_TEMPS.containsKey(id);
    }

    /**
     * Register a block temperature at runtime.
     * This registration persists across resource reloads.
     *
     * @param blockId The block identifier
     * @param temperature The temperature effect in degrees Celsius
     * @return true if successful, false if block doesn't exist
     */
    public static boolean registerRuntime(Identifier blockId, int temperature) {
        if (blockId == null) {
            Temperaturem.LOGGER.warn("Attempted to register null block ID");
            return false;
        }

        if (!Registries.BLOCK.containsId(blockId)) {
            Temperaturem.LOGGER.warn("Block '{}' not found in registry", blockId);
            return false;
        }

        if (temperature < -273 || temperature > 1000) {
            Temperaturem.LOGGER.warn("Temperature out of range for block '{}': {}°C", blockId, temperature);
            return false;
        }

        RUNTIME_TEMPS.put(blockId, temperature);
        Temperaturem.LOGGER.info("Registered runtime temperature for block '{}': {}°C", blockId, temperature);
        return true;
    }

    /**
     * Remove a runtime temperature registration.
     *
     * @param blockId The block identifier
     * @return true if a registration was removed
     */
    public static boolean unregisterRuntime(Identifier blockId) {
        boolean removed = RUNTIME_TEMPS.remove(blockId) != null;
        if (removed) {
            Temperaturem.LOGGER.info("Unregistered runtime temperature for block '{}'", blockId);
        }
        return removed;
    }

    /**
     * Clear all runtime registrations.
     */
    public static void clearRuntimeRegistrations() {
        int count = RUNTIME_TEMPS.size();
        RUNTIME_TEMPS.clear();
        Temperaturem.LOGGER.info("Cleared {} runtime temperature registrations", count);
    }

    /**
     * Get all runtime registrations (unmodifiable view).
     */
    public static Map<Identifier, Integer> getRuntimeRegistrations() {
        return new HashMap<>(RUNTIME_TEMPS);
    }
}