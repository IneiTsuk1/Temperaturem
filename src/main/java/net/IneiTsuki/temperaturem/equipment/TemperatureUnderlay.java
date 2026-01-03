package net.IneiTsuki.temperaturem.equipment;

import net.minecraft.nbt.NbtCompound;

/**
 * Represents a temperature-regulating underlay that can be applied to armor pieces.
 * Underlays provide protection against hot or cold environments.
 */
public class TemperatureUnderlay {

    private final UnderlayType type;
    private int durability;

    public TemperatureUnderlay(UnderlayType type) {
        this.type = type;
        this.durability = type.getMaxDurability();
    }

    public TemperatureUnderlay(UnderlayType type, int durability) {
        this.type = type;
        this.durability = Math.min(durability, type.getMaxDurability());
    }

    public UnderlayType getType() {
        return type;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = Math.max(0, Math.min(durability, type.getMaxDurability()));
    }

    public int getMaxDurability() {
        return type.getMaxDurability();
    }

    public float getDurabilityPercent() {
        return (float) durability / type.getMaxDurability();
    }

    /**
     * Damages the underlay, reducing its durability.
     * @param amount Amount of durability to remove
     * @return true if the underlay broke (durability reached 0)
     */
    public boolean damage(int amount) {
        durability -= amount;
        if (durability <= 0) {
            durability = 0;
            return true;
        }
        return false;
    }

    /**
     * Repairs the underlay by the specified amount.
     * @param amount Amount of durability to restore
     */
    public void repair(int amount) {
        durability = Math.min(durability + amount, type.getMaxDurability());
    }

    /**
     * Gets the temperature protection value of this underlay.
     * Protection scales with durability (50-100% effectiveness based on condition).
     *
     * @return The temperature offset this underlay provides
     */
    public double getProtectionValue() {
        float condition = getDurabilityPercent();
        // Min 50% effectiveness at low durability, 100% at full
        float effectiveness = 0.5f + (condition * 0.5f);
        return type.getBaseProtection() * effectiveness;
    }

    /**
     * Checks if this underlay protects against cold temperatures.
     */
    public boolean protectsFromCold() {
        return type.getBaseProtection() > 0;
    }

    /**
     * Checks if this underlay protects against hot temperatures.
     */
    public boolean protectsFromHeat() {
        return type.getBaseProtection() < 0;
    }

    /**
     * Checks if the underlay is broken (no durability remaining).
     */
    public boolean isBroken() {
        return durability <= 0;
    }

    // ===== NBT Serialization =====

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Type", type.name());
        nbt.putInt("Durability", durability);
        return nbt;
    }

    public static TemperatureUnderlay fromNbt(NbtCompound nbt) {
        try {
            UnderlayType type = UnderlayType.valueOf(nbt.getString("Type"));
            int durability = nbt.getInt("Durability");
            return new TemperatureUnderlay(type, durability);
        } catch (IllegalArgumentException e) {
            // Invalid type, return null
            return null;
        }
    }

    // ===== Underlay Types =====

    public enum UnderlayType {
        // Cold Protection
        WOOL_LINING("Wool Lining", 8.0, 500, "Basic cold protection"),
        FUR_LINING("Fur Lining", 15.0, 800, "Good cold protection"),
        THERMAL_PADDING("Thermal Padding", 25.0, 1200, "Excellent cold protection"),

        // Heat Protection
        LEATHER_LINING("Leather Lining", -8.0, 500, "Basic heat protection"),
        COOLING_MESH("Cooling Mesh", -15.0, 800, "Good heat protection"),
        CLIMATE_WEAVE("Climate Weave", -25.0, 1200, "Excellent heat protection"),

        // Balanced
        INSULATED_FABRIC("Insulated Fabric", 12.0, 600, "Moderate cold protection with some durability"),
        REFLECTIVE_LAYER("Reflective Layer", -12.0, 600, "Moderate heat protection with some durability");

        private final String displayName;
        private final double baseProtection; // Positive = cold protection, Negative = heat protection
        private final int maxDurability;
        private final String description;

        UnderlayType(String displayName, double baseProtection, int maxDurability, String description) {
            this.displayName = displayName;
            this.baseProtection = baseProtection;
            this.maxDurability = maxDurability;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getBaseProtection() {
            return baseProtection;
        }

        public int getMaxDurability() {
            return maxDurability;
        }

        public String getDescription() {
            return description;
        }

        public boolean isColdProtection() {
            return baseProtection > 0;
        }

        public boolean isHeatProtection() {
            return baseProtection < 0;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%d/%d)", type.getDisplayName(), durability, type.getMaxDurability());
    }
}