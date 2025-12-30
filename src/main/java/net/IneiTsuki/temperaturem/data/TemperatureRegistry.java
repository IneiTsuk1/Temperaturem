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

public class TemperatureRegistry implements SimpleSynchronousResourceReloadListener {

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

                    // Validate reasonable temperature
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

            Temperaturem.LOGGER.info("Loaded {} block temperature entries", BLOCK_TEMPS.size());

        } catch (IOException e) {
            Temperaturem.LOGGER.error("Failed to load block temperatures (IO error)", e);
        } catch (Exception e) {
            Temperaturem.LOGGER.error("Failed to load block temperatures", e);
        }
    }

    public static int getTemperature(BlockState state) {
        if (state == null) return 0;
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return BLOCK_TEMPS.getOrDefault(id, 0);
    }

    public static boolean hasTemperature(BlockState state) {
        if (state == null) return false;
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return BLOCK_TEMPS.containsKey(id);
    }
}