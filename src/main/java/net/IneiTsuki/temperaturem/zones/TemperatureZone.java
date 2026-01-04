package net.IneiTsuki.temperaturem.zones;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.UUID;

/**
 * Represents a defined temperature zone in the world.
 * Zones override environmental temperature calculations within their bounds.
 */
public class TemperatureZone {

    private final UUID id;
    private String name;
    private Box bounds;
    private double temperature;
    private ZoneType type;
    private int priority;
    private boolean enabled;

    // Optional advanced settings
    private double transitionRange; // Distance for smooth temperature transition at edges
    private boolean affectsPlayers;
    private boolean affectsMobs;

    public TemperatureZone(UUID id, String name, Box bounds, double temperature, ZoneType type) {
        this.id = id;
        this.name = name;
        this.bounds = bounds;
        this.temperature = temperature;
        this.type = type;
        this.priority = 0;
        this.enabled = true;
        this.transitionRange = 3.0;
        this.affectsPlayers = true;
        this.affectsMobs = false;
    }

    // ===== Getters and Setters =====

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Box getBounds() {
        return bounds;
    }

    public void setBounds(Box bounds) {
        this.bounds = bounds;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = Math.max(-273, Math.min(1000, temperature));
    }

    public ZoneType getType() {
        return type;
    }

    public void setType(ZoneType type) {
        this.type = type;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getTransitionRange() {
        return transitionRange;
    }

    public void setTransitionRange(double transitionRange) {
        this.transitionRange = Math.max(0, transitionRange);
    }

    public boolean affectsPlayers() {
        return affectsPlayers;
    }

    public void setAffectsPlayers(boolean affectsPlayers) {
        this.affectsPlayers = affectsPlayers;
    }

    public boolean affectsMobs() {
        return affectsMobs;
    }

    public void setAffectsMobs(boolean affectsMobs) {
        this.affectsMobs = affectsMobs;
    }

    // ===== Zone Logic =====

    /**
     * Check if a position is within this zone's bounds.
     */
    public boolean contains(BlockPos pos) {
        return bounds.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Check if a position is within this zone's bounds (with double precision).
     */
    public boolean contains(double x, double y, double z) {
        return bounds.contains(x, y, z);
    }

    /**
     * Calculate the temperature effect at a given position.
     * Uses smooth transitions at zone edges if transitionRange > 0.
     *
     * @param pos The position to check
     * @return The temperature effect (0 if outside zone, full temperature if inside)
     */
    public double getTemperatureAt(BlockPos pos) {
        if (!enabled) return 0.0;
        if (!contains(pos)) return 0.0;

        if (transitionRange <= 0 || type == ZoneType.ABSOLUTE) {
            return temperature;
        }

        // Calculate distance to nearest edge
        double distToEdge = getDistanceToEdge(pos);

        if (distToEdge >= transitionRange) {
            return temperature;
        }

        // Smooth transition using smoothstep
        double t = distToEdge / transitionRange;
        double smoothT = t * t * (3.0 - 2.0 * t); // Smoothstep interpolation

        return temperature * smoothT;
    }

    /**
     * Get the shortest distance from a position to the zone's edge.
     */
    private double getDistanceToEdge(BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double dx = Math.min(x - bounds.minX, bounds.maxX - x);
        double dy = Math.min(y - bounds.minY, bounds.maxY - y);
        double dz = Math.min(z - bounds.minZ, bounds.maxZ - z);

        return Math.min(dx, Math.min(dy, dz));
    }

    /**
     * Check if this zone overlaps with another zone.
     */
    public boolean overlaps(TemperatureZone other) {
        return this.bounds.intersects(other.bounds);
    }

    /**
     * Get the volume of this zone in blocks.
     */
    public long getVolume() {
        double dx = bounds.maxX - bounds.minX;
        double dy = bounds.maxY - bounds.minY;
        double dz = bounds.maxZ - bounds.minZ;
        return (long) (dx * dy * dz);
    }

    // ===== NBT Serialization =====

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putUuid("Id", id);
        nbt.putString("Name", name);
        nbt.putDouble("Temperature", temperature);
        nbt.putString("Type", type.name());
        nbt.putInt("Priority", priority);
        nbt.putBoolean("Enabled", enabled);
        nbt.putDouble("TransitionRange", transitionRange);
        nbt.putBoolean("AffectsPlayers", affectsPlayers);
        nbt.putBoolean("AffectsMobs", affectsMobs);

        // Bounds
        NbtCompound boundsNbt = new NbtCompound();
        boundsNbt.putDouble("MinX", bounds.minX);
        boundsNbt.putDouble("MinY", bounds.minY);
        boundsNbt.putDouble("MinZ", bounds.minZ);
        boundsNbt.putDouble("MaxX", bounds.maxX);
        boundsNbt.putDouble("MaxY", bounds.maxY);
        boundsNbt.putDouble("MaxZ", bounds.maxZ);
        nbt.put("Bounds", boundsNbt);

        return nbt;
    }

    public static TemperatureZone fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("Id");
        String name = nbt.getString("Name");
        double temperature = nbt.getDouble("Temperature");
        ZoneType type = ZoneType.valueOf(nbt.getString("Type"));

        NbtCompound boundsNbt = nbt.getCompound("Bounds");
        Box bounds = new Box(
                boundsNbt.getDouble("MinX"),
                boundsNbt.getDouble("MinY"),
                boundsNbt.getDouble("MinZ"),
                boundsNbt.getDouble("MaxX"),
                boundsNbt.getDouble("MaxY"),
                boundsNbt.getDouble("MaxZ")
        );

        TemperatureZone zone = new TemperatureZone(id, name, bounds, temperature, type);
        zone.setPriority(nbt.getInt("Priority"));
        zone.setEnabled(nbt.getBoolean("Enabled"));
        zone.setTransitionRange(nbt.getDouble("TransitionRange"));
        zone.setAffectsPlayers(nbt.getBoolean("AffectsPlayers"));
        zone.setAffectsMobs(nbt.getBoolean("AffectsMobs"));

        return zone;
    }

    // ===== Zone Types =====

    public enum ZoneType {
        /**
         * Adds to the existing temperature calculation.
         * Good for heating/cooling systems that supplement natural temperature.
         */
        ADDITIVE,

        /**
         * Completely overrides the environmental temperature.
         * Best for sealed climate-controlled areas.
         */
        ABSOLUTE,

        /**
         * Multiplies the environmental temperature by a factor.
         * Useful for insulation effects.
         */
        MULTIPLIER
    }

    @Override
    public String toString() {
        return String.format("Zone[%s: %.1f°C, %s, Priority=%d, %s]",
                name, temperature, type, priority, enabled ? "Enabled" : "Disabled");
    }
}