package net.IneiTsuki.temperaturem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.IneiTsuki.temperaturem.Temperaturem;
import net.IneiTsuki.temperaturem.effects.TemperatureEffects;

import java.io.*;

public class TemperatureEffectsConfig {

    private static final String CONFIG_PATH = "config/temperaturem/effects.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Default configuration
    public boolean enableDamage = true;
    public boolean enableStatusEffects = true;
    public boolean enableWarnings = true;
    public int damageInterval = 40; // 2 seconds
    public float coldDamage = 1.0f; // 0.5 hearts
    public float hotDamage = 1.0f; // 0.5 hearts

    public static TemperatureEffectsConfig load() {
        File configFile = new File(CONFIG_PATH);
        TemperatureEffectsConfig config;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, TemperatureEffectsConfig.class);
                Temperaturem.LOGGER.info("Loaded temperature effects config");
            } catch (Exception e) {
                Temperaturem.LOGGER.error("Failed to load effects config, using defaults", e);
                config = new TemperatureEffectsConfig();
            }
        } else {
            config = new TemperatureEffectsConfig();
            config.save();
            Temperaturem.LOGGER.info("Created default effects config");
        }

        // Apply configuration to effects system
        config.apply();

        return config;
    }

    public void save() {
        File configFile = new File(CONFIG_PATH);
        File parentDir = configFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
            Temperaturem.LOGGER.info("Saved effects config");
        } catch (IOException e) {
            Temperaturem.LOGGER.error("Failed to save effects config", e);
        }
    }

    public void apply() {
        TemperatureEffects.setEnableDamage(enableDamage);
        TemperatureEffects.setEnableStatusEffects(enableStatusEffects);
        TemperatureEffects.setEnableWarnings(enableWarnings);
        TemperatureEffects.setDamageInterval(damageInterval);
        TemperatureEffects.setColdDamage(coldDamage);
        TemperatureEffects.setHotDamage(hotDamage);
    }
}