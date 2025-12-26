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
        s = s.trim();
        int min, max;

        try {
            if (s.contains("|")) {
                String[] parts = s.split("\\|");
                if (parts.length != 2)
                    throw new IllegalArgumentException("Invalid IntRange: " + s);
                min = Integer.parseInt(parts[0].trim());
                max = Integer.parseInt(parts[1].trim());
            } else {
                min = max = Integer.parseInt(s);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid IntRange: " + s, e);
        }

        return new IntRange(min, max);
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + "|" + max;
    }
}