package net.IneiTsuki.temperaturem.data;

import net.minecraft.util.math.random.Random;

public class IntRange {

    private final int min;
    private final int max;
    private final Random random = Random.create();

    public IntRange(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getAverage() {
        return (min + max) / 2;
    }

    public int random() {
        if (min == max) return min;
        return random.nextBetween(min, max);
    }

    public static IntRange parse(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("IntRange cannot be null or empty");
        }

        s = s.trim();
        int min, max;

        try {
            if (s.contains("|")) {
                String[] parts = s.split("\\|");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid IntRange format (expected 'min|max'): " + s);
                }
                min = Integer.parseInt(parts[0].trim());
                max = Integer.parseInt(parts[1].trim());
            } else {
                min = max = Integer.parseInt(s);
            }

            // Validate reasonable temperature ranges (-273°C is absolute zero, 1000°C is extreme)
            if (min < -273 || max < -273) {
                throw new IllegalArgumentException("Temperature below absolute zero: " + s);
            }
            if (min > 1000 || max > 1000) {
                throw new IllegalArgumentException("Temperature unreasonably high (>1000°C): " + s);
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid IntRange numbers: " + s, e);
        }

        return new IntRange(min, max);
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + "|" + max;
    }
}