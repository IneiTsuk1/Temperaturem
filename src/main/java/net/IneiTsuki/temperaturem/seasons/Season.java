package net.IneiTsuki.temperaturem.seasons;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Represents the four seasons with their characteristics.
 */
public enum Season {
    SPRING("Spring", 0, Formatting.GREEN, 0.0, 1.0, "Mild temperatures and blooming nature"),
    SUMMER("Summer", 1, Formatting.GOLD, 10.0, 1.3, "Hot days and warm nights"),
    AUTUMN("Autumn", 2, Formatting.DARK_RED, -4.0, 0.9, "Cooling temperatures and falling leaves"),
    WINTER("Winter", 3, Formatting.AQUA, -16.0, 0.6, "Cold temperatures and possible snow");

    private final String displayName;
    private final int id;
    private final Formatting color;
    private final double temperatureModifier;
    private final double temperatureMultiplier;
    private final String description;

    Season(String displayName, int id, Formatting color, double temperatureModifier,
           double temperatureMultiplier, String description) {
        this.displayName = displayName;
        this.id = id;
        this.color = color;
        this.temperatureModifier = temperatureModifier;
        this.temperatureMultiplier = temperatureMultiplier;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getId() {
        return id;
    }

    public Formatting getColor() {
        return color;
    }

    public double getTemperatureModifier() {
        return temperatureModifier;
    }

    public double getTemperatureMultiplier() {
        return temperatureMultiplier;
    }

    public String getDescription() {
        return description;
    }

    public Season next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public Season previous() {
        return values()[(this.ordinal() - 1 + values().length) % values().length];
    }

    public static Season fromId(int id) {
        Season[] values = values();
        return (id >= 0 && id < values.length) ? values[id] : SPRING;
    }

    public Text toText() {
        return Text.literal(displayName).formatted(color);
    }

    public double getTemperatureModifierWithTransition(double progress, Season nextSeason) {
        // Smooth transition in the last 25% of the season
        if (progress > 0.75) {
            double transitionProgress = (progress - 0.75) / 0.25;
            // Smoothstep interpolation
            transitionProgress = transitionProgress * transitionProgress * (3.0 - 2.0 * transitionProgress);

            double currentMod = this.temperatureModifier;
            double nextMod = nextSeason.temperatureModifier;

            return currentMod + (nextMod - currentMod) * transitionProgress;
        }

        return this.temperatureModifier;
    }

    public double getTemperatureMultiplierWithTransition(double progress, Season nextSeason) {
        if (progress > 0.75) {
            double transitionProgress = (progress - 0.75) / 0.25;
            transitionProgress = transitionProgress * transitionProgress * (3.0 - 2.0 * transitionProgress);

            double currentMult = this.temperatureMultiplier;
            double nextMult = nextSeason.temperatureMultiplier;

            return currentMult + (nextMult - currentMult) * transitionProgress;
        }

        return this.temperatureMultiplier;
    }
}