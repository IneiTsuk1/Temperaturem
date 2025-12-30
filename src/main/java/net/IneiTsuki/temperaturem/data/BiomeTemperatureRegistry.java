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

public class BiomeTemperatureRegistry implements SimpleSynchronousResourceReloadListener {

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

            Temperaturem.LOGGER.info("Loaded {} biome temperature entries", BIOME_TEMPS.size());

        } catch (Exception e) {
            Temperaturem.LOGGER.error("Failed to load biome temperatures", e);
        }
    }

    // ✅ Correct lookup
    public static Integer getTemperatureOverride(Identifier biomeId) {
        return BIOME_TEMPS.get(biomeId);
    }

    public static boolean hasOverride(Identifier biomeId) {
        return BIOME_TEMPS.containsKey(biomeId);
    }

}
