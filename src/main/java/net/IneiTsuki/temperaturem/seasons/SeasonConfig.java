package net.IneiTsuki.temperaturem.seasons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.IneiTsuki.temperaturem.Temperaturem;

import java.io.*;

/**
 * Global configuration for the seasons system.
 */
public class SeasonConfig {

    private static final String CONFIG_PATH = "config/temperaturem/seasons.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // General
    public boolean enabled = true;
    public int seasonLengthDays = 28;
    public boolean notifySeasonChange = true;
    public boolean syncAcrossWorlds = false;

    // Temperature modifiers
    public double springTempModifier = 0.0;
    public double summerTempModifier = 8.0;
    public double autumnTempModifier = -2.0;
    public double winterTempModifier = -12.0;

    public double springTempMultiplier = 1.0;
    public double summerTempMultiplier = 1.2;
    public double autumnTempMultiplier = 0.9;
    public double winterTempMultiplier = 0.7;

    // Visuals (future use)
    public boolean enableSeasonalWeather = true;
    public boolean enableSeasonalFoliage = false;

    public static SeasonConfig load() {
        File file = new File(CONFIG_PATH);

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Temperaturem.LOGGER.info("Loaded season config");
                return GSON.fromJson(reader, SeasonConfig.class);
            } catch (Exception e) {
                Temperaturem.LOGGER.error("Failed to load season config, using defaults", e);
            }
        }

        SeasonConfig config = new SeasonConfig();
        config.save();
        Temperaturem.LOGGER.info("Created default season config");
        return config;
    }

    public void save() {
        File file = new File(CONFIG_PATH);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
            Temperaturem.LOGGER.info("Saved season config");
        } catch (IOException e) {
            Temperaturem.LOGGER.error("Failed to save season config", e);
        }
    }
}
