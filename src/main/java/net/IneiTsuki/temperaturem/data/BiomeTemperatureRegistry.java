package net.IneiTsuki.temperaturem.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.IneiTsuki.temperaturem.Temperaturem;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeTemperatureRegistry implements SimpleSynchronousResourceReloadListener {

    // Persistent runtime registrations (survive reloads)
    private static final Map<Identifier, Integer> RUNTIME_TEMPS = new ConcurrentHashMap<>();

    // Loaded from config (cleared on reload)
    private static final Map<Identifier, Integer> BIOME_TEMPS = new HashMap<>(256);

    private static final String CONFIG_PATH = "config/temperaturem/biomes/biome_temperatures.json";

    @Override
    public Identifier getFabricId() {
        return new Identifier("temperaturem", "biome_temperature_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        BIOME_TEMPS.clear();
        File configFile = new File(CONFIG_PATH);
        JsonObject json;

        try {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                    Temperaturem.LOGGER.info("Loaded biome temperatures from config");
                }
            } else {
                InputStream defaultStream = BiomeTemperatureRegistry.class
                        .getClassLoader()
                        .getResourceAsStream("data/temperaturem/biomes/biome_temperatures.json");

                if (defaultStream == null) {
                    Temperaturem.LOGGER.error("Default biome_temperatures.json not found!");
                    return;
                }

                try (InputStreamReader reader = new InputStreamReader(defaultStream)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }

                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(json.toString());
                }
            }

            json.entrySet().forEach(entry -> {
                try {
                    int temp = Integer.parseInt(entry.getValue().getAsString());
                    BIOME_TEMPS.put(new Identifier(entry.getKey()), temp);
                } catch (Exception e) {
                    Temperaturem.LOGGER.error("Invalid biome temperature entry '{}'", entry.getKey(), e);
                }
            });

            Temperaturem.LOGGER.info("Loaded {} biome temperature entries ({} runtime)",
                    BIOME_TEMPS.size(), RUNTIME_TEMPS.size());

        } catch (Exception e) {
            Temperaturem.LOGGER.error("Failed to load biome temperatures", e);
        }
    }

    /**
     * Get temperature override for a biome.
     * Runtime registrations take precedence over config values.
     */
    public static Integer getTemperatureOverride(Identifier biomeId) {
        if (biomeId == null) return null;

        // Check runtime first
        if (RUNTIME_TEMPS.containsKey(biomeId)) {
            return RUNTIME_TEMPS.get(biomeId);
        }

        return BIOME_TEMPS.get(biomeId);
    }

    public static boolean hasOverride(Identifier biomeId) {
        if (biomeId == null) return false;
        return RUNTIME_TEMPS.containsKey(biomeId) || BIOME_TEMPS.containsKey(biomeId);
    }

    /**
     * Register a biome temperature at runtime.
     * This registration persists across resource reloads.
     *
     * @param biomeId The biome identifier
     * @param temperature The base temperature in degrees Celsius
     * @return true if successful
     */
    public static boolean registerRuntime(Identifier biomeId, int temperature) {
        if (biomeId == null) {
            Temperaturem.LOGGER.warn("Attempted to register null biome ID");
            return false;
        }

        if (temperature < -273 || temperature > 1000) {
            Temperaturem.LOGGER.warn("Temperature out of range for biome '{}': {}°C", biomeId, temperature);
            return false;
        }

        RUNTIME_TEMPS.put(biomeId, temperature);
        Temperaturem.LOGGER.info("Registered runtime temperature for biome '{}': {}°C", biomeId, temperature);
        return true;
    }

    /**
     * Remove a runtime temperature registration.
     *
     * @param biomeId The biome identifier
     * @return true if a registration was removed
     */
    public static boolean unregisterRuntime(Identifier biomeId) {
        boolean removed = RUNTIME_TEMPS.remove(biomeId) != null;
        if (removed) {
            Temperaturem.LOGGER.info("Unregistered runtime temperature for biome '{}'", biomeId);
        }
        return removed;
    }

    /**
     * Clear all runtime registrations.
     */
    public static void clearRuntimeRegistrations() {
        int count = RUNTIME_TEMPS.size();
        RUNTIME_TEMPS.clear();
        Temperaturem.LOGGER.info("Cleared {} runtime biome temperature registrations", count);
    }

    /**
     * Get all runtime registrations (unmodifiable view).
     */
    public static Map<Identifier, Integer> getRuntimeRegistrations() {
        return new HashMap<>(RUNTIME_TEMPS);
    }
}