package net.IneiTsuki.temperaturem.api;

import net.IneiTsuki.temperaturem.zones.TemperatureZone;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * API for managing temperature zones - defined areas with controlled temperatures.
 * Temperature zones can override environmental temperature calculations.
 */
public interface ITemperatureZoneAPI {

    // ===== Zone Creation and Management =====

    /**
     * Create a new temperature zone in a world.
     *
     * @param world The world to create the zone in
     * @param name The name of the zone
     * @param pos1 First corner of the zone
     * @param pos2 Opposite corner of the zone
     * @param temperature The temperature in degrees Celsius
     * @param type The zone type (ADDITIVE, ABSOLUTE, or MULTIPLIER)
     * @return The created zone
     * @throws IllegalArgumentException if parameters are invalid
     */
    TemperatureZone createZone(ServerWorld world, String name, BlockPos pos1, BlockPos pos2,
                               double temperature, TemperatureZone.ZoneType type);

    /**
     * Create a new temperature zone with a Box boundary.
     *
     * @param world The world to create the zone in
     * @param name The name of the zone
     * @param bounds The bounding box of the zone
     * @param temperature The temperature in degrees Celsius
     * @param type The zone type
     * @return The created zone
     */
    TemperatureZone createZone(ServerWorld world, String name, Box bounds,
                               double temperature, TemperatureZone.ZoneType type);

    /**
     * Remove a temperature zone.
     *
     * @param world The world containing the zone
     * @param zoneId The UUID of the zone to remove
     * @return true if the zone was removed, false if not found
     */
    boolean removeZone(ServerWorld world, UUID zoneId);

    /**
     * Get a specific zone by ID.
     *
     * @param world The world containing the zone
     * @param zoneId The UUID of the zone
     * @return The zone, or null if not found
     */
    TemperatureZone getZone(ServerWorld world, UUID zoneId);

    /**
     * Get all zones in a world.
     *
     * @param world The world to query
     * @return Collection of all zones (unmodifiable)
     */
    Collection<TemperatureZone> getAllZones(ServerWorld world);

    // ===== Zone Queries =====

    /**
     * Get all zones affecting a specific position.
     * Zones are sorted by priority (highest first).
     *
     * @param world The world to check
     * @param pos The position to check
     * @return List of zones at this position
     */
    List<TemperatureZone> getZonesAt(ServerWorld world, BlockPos pos);

    /**
     * Get the effective zone temperature at a position.
     * Takes into account all overlapping zones and their priorities.
     *
     * @param world The world to check
     * @param pos The position to check
     * @return The zone temperature, or null if no zones affect this position
     */
    Double getZoneTemperatureAt(ServerWorld world, BlockPos pos);

    /**
     * Find zones by name (partial, case-insensitive match).
     *
     * @param world The world to search
     * @param nameQuery The name to search for
     * @return List of matching zones
     */
    List<TemperatureZone> findZonesByName(ServerWorld world, String nameQuery);

    /**
     * Get all zones that overlap with a given area.
     *
     * @param world The world to check
     * @param area The bounding box to check
     * @return List of overlapping zones
     */
    List<TemperatureZone> getZonesInArea(ServerWorld world, Box area);

    /**
     * Check if a position is within any temperature zone.
     *
     * @param world The world to check
     * @param pos The position to check
     * @return true if at least one zone affects this position
     */
    boolean isInZone(ServerWorld world, BlockPos pos);

    // ===== Zone Configuration =====

    /**
     * Update a zone's temperature.
     *
     * @param world The world containing the zone
     * @param zoneId The zone ID
     * @param temperature The new temperature in degrees Celsius
     * @throws IllegalArgumentException if zone not found or temperature out of range
     */
    void setZoneTemperature(ServerWorld world, UUID zoneId, double temperature);

    /**
     * Update a zone's bounds.
     *
     * @param world The world containing the zone
     * @param zoneId The zone ID
     * @param pos1 First corner
     * @param pos2 Opposite corner
     * @throws IllegalArgumentException if zone not found
     */
    void setZoneBounds(ServerWorld world, UUID zoneId, BlockPos pos1, BlockPos pos2);

    /**
     * Update a zone's priority.
     * Higher priority zones take precedence when multiple zones overlap.
     *
     * @param world The world containing the zone
     * @param zoneId The zone ID
     * @param priority The new priority
     * @throws IllegalArgumentException if zone not found
     */
    void setZonePriority(ServerWorld world, UUID zoneId, int priority);

    /**
     * Enable or disable a zone.
     *
     * @param world The world containing the zone
     * @param zoneId The zone ID
     * @param enabled true to enable, false to disable
     * @throws IllegalArgumentException if zone not found
     */
    void setZoneEnabled(ServerWorld world, UUID zoneId, boolean enabled);

    /**
     * Set the transition range for smooth temperature blending at zone edges.
     *
     * @param world The world containing the zone
     * @param zoneId The zone ID
     * @param range The transition range in blocks (0 = no transition)
     * @throws IllegalArgumentException if zone not found
     */
    void setZoneTransitionRange(ServerWorld world, UUID zoneId, double range);

    // ===== Statistics =====

    /**
     * Get the number of zones in a world.
     *
     * @param world The world to check
     * @return The number of zones
     */
    int getZoneCount(ServerWorld world);

    /**
     * Get the number of enabled zones in a world.
     *
     * @param world The world to check
     * @return The number of enabled zones
     */
    int getEnabledZoneCount(ServerWorld world);

    /**
     * Get the total volume of all zones in a world.
     *
     * @param world The world to check
     * @return The total volume in cubic blocks
     */
    long getTotalZoneVolume(ServerWorld world);
}