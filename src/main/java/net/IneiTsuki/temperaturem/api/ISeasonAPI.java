package net.IneiTsuki.temperaturem.api;

import net.IneiTsuki.temperaturem.seasons.Season;
import net.minecraft.server.world.ServerWorld;

/**
 * API for interacting with the seasons system.
 * Allows other mods to query and control seasonal progression.
 */
public interface ISeasonAPI {

    // ===== Season Queries =====

    /**
     * Get the current season in a world.
     *
     * @param world The world to query
     * @return The current season
     */
    Season getCurrentSeason(ServerWorld world);

    /**
     * Get the progress through the current season (0.0 to 1.0).
     * 0.0 = just started, 1.0 = about to end
     *
     * @param world The world to query
     * @return Progress value between 0.0 and 1.0
     */
    double getSeasonProgress(ServerWorld world);

    /**
     * Get the number of days remaining in the current season.
     *
     * @param world The world to query
     * @return Days remaining
     */
    int getRemainingDays(ServerWorld world);

    /**
     * Get the temperature modifier from the current season.
     * This value is added to the base temperature.
     *
     * @param world The world to query
     * @return Temperature modifier in degrees Celsius
     */
    double getSeasonalTemperatureModifier(ServerWorld world);

    /**
     * Get the temperature multiplier from the current season.
     * Base temperature is multiplied by this value.
     *
     * @param world The world to query
     * @return Temperature multiplier (e.g., 1.2 = 20% increase)
     */
    double getSeasonalTemperatureMultiplier(ServerWorld world);

    /**
     * Get detailed season information as a string.
     *
     * @param world The world to query
     * @return Formatted season info
     */
    String getSeasonInfo(ServerWorld world);

    // ===== Season Control =====

    /**
     * Set the current season in a world.
     * This immediately changes the season without waiting for natural progression.
     *
     * @param world The world to modify
     * @param season The season to set
     */
    void setCurrentSeason(ServerWorld world, Season season);

    /**
     * Advance to the next season immediately.
     *
     * @param world The world to modify
     */
    void skipToNextSeason(ServerWorld world);

    /**
     * Check if seasons are enabled in a world.
     *
     * @param world The world to query
     * @return true if seasons are active
     */
    boolean isSeasonsEnabled(ServerWorld world);

    /**
     * Enable or disable seasons in a world.
     *
     * @param world The world to modify
     * @param enabled true to enable, false to disable
     */
    void setSeasonsEnabled(ServerWorld world, boolean enabled);

    // ===== Configuration =====

    /**
     * Get the length of each season in days.
     *
     * @param world The world to query
     * @return Days per season
     */
    int getSeasonLengthDays(ServerWorld world);

    /**
     * Set the length of each season in days.
     * Valid range: 1-1000 days
     *
     * @param world The world to modify
     * @param days Days per season
     * @throws IllegalArgumentException if days is out of range
     */
    void setSeasonLengthDays(ServerWorld world, int days);

    /**
     * Check if season change notifications are enabled.
     *
     * @param world The world to query
     * @return true if players are notified of season changes
     */
    boolean isNotifySeasonChange(ServerWorld world);

    /**
     * Enable or disable season change notifications.
     *
     * @param world The world to modify
     * @param notify true to notify players
     */
    void setNotifySeasonChange(ServerWorld world, boolean notify);
}