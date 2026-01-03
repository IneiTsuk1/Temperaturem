package net.IneiTsuki.temperaturem.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Public API for other mods to interact with the Temperature system.
 * This interface provides safe methods to query and modify player temperatures,
 * register custom temperature sources, and configure temperature effects.
 *
 * @see TemperatureAPI#getInstance()
 */
public interface ITemperatureAPI {

    // ===== Player Temperature Methods =====

    /**
     * Gets the current temperature of a player.
     *
     * @param player The player to query
     * @return The player's current temperature in degrees Celsius, rounded to nearest integer
     */
    int getPlayerTemperature(ServerPlayerEntity player);

    /**
     * Gets the exact temperature of a player with decimal precision.
     *
     * @param player The player to query
     * @return The player's exact temperature in degrees Celsius
     */
    double getPlayerTemperatureExact(ServerPlayerEntity player);

    /**
     * Modifies a player's temperature by a delta amount.
     * Use positive values to heat, negative to cool.
     * Temperature will be clamped to [-50, 150]°C.
     *
     * @param player The player to modify
     * @param delta The temperature change in degrees Celsius
     */
    void modifyPlayerTemperature(ServerPlayerEntity player, double delta);

    /**
     * Sets a player's temperature to an exact value.
     * This bypasses gradual temperature changes.
     * Temperature will be clamped to [-50, 150]°C.
     *
     * @param player The player to modify
     * @param temperature The new temperature in degrees Celsius
     */
    void setPlayerTemperature(ServerPlayerEntity player, double temperature);

    // ===== Runtime Registration Methods =====

    /**
     * Registers a custom temperature value for a block at runtime.
     * This registration persists across resource reloads and takes precedence
     * over config file values.
     *
     * @param block The block identifier (e.g., new Identifier("modid", "block_name"))
     * @param temperature The temperature effect in degrees Celsius (-273 to 1000)
     * @throws IllegalArgumentException if block is null, doesn't exist, or temperature is out of range
     */
    void registerBlockTemperature(Identifier block, int temperature);

    /**
     * Registers a custom base temperature for a biome at runtime.
     * This registration persists across resource reloads and takes precedence
     * over config file values and vanilla temperature calculations.
     *
     * @param biome The biome identifier (e.g., new Identifier("minecraft", "desert"))
     * @param temperature The base temperature in degrees Celsius (-273 to 1000)
     * @throws IllegalArgumentException if biome is null or temperature is out of range
     */
    void registerBiomeTemperature(Identifier biome, int temperature);

    /**
     * Removes a runtime block temperature registration.
     * This does not affect config file entries.
     *
     * @param block The block identifier
     * @return true if a registration was removed, false if no registration existed
     */
    boolean unregisterBlockTemperature(Identifier block);

    /**
     * Removes a runtime biome temperature registration.
     * This does not affect config file entries.
     *
     * @param biome The biome identifier
     * @return true if a registration was removed, false if no registration existed
     */
    boolean unregisterBiomeTemperature(Identifier biome);

    // ===== Query Methods =====

    /**
     * Gets the temperature effect of a specific block.
     *
     * @param block The block identifier
     * @return The temperature effect, or 0 if the block has no effect
     */
    int getBlockTemperature(Identifier block);

    /**
     * Gets the base temperature of a specific biome.
     *
     * @param biome The biome identifier
     * @return The base temperature, or null if using vanilla calculation
     */
    Integer getBiomeTemperature(Identifier biome);

    /**
     * Checks if a block has a registered temperature effect.
     *
     * @param block The block identifier
     * @return true if the block affects temperature
     */
    boolean hasBlockTemperature(Identifier block);

    /**
     * Checks if a biome has a custom temperature override.
     *
     * @param biome The biome identifier
     * @return true if the biome has a custom temperature
     */
    boolean hasBiomeTemperature(Identifier biome);

    // ===== Effect Configuration Methods =====

    /**
     * Enable or disable temperature damage.
     * When enabled, extreme temperatures will damage players.
     *
     * @param enable true to enable damage, false to disable
     */
    void setEnableTemperatureDamage(boolean enable);

    /**
     * Enable or disable temperature status effects.
     * When enabled, temperature will apply effects like Slowness, Weakness, etc.
     *
     * @param enable true to enable effects, false to disable
     */
    void setEnableStatusEffects(boolean enable);

    /**
     * Enable or disable temperature warning messages.
     * When enabled, players will receive action bar warnings at dangerous temperatures.
     *
     * @param enable true to enable warnings, false to disable
     */
    void setEnableWarnings(boolean enable);

    /**
     * Set the interval between temperature damage ticks.
     *
     * @param ticks The number of ticks between damage (20 ticks = 1 second)
     * @throws IllegalArgumentException if ticks is less than 1
     */
    void setDamageInterval(int ticks);

    /**
     * Set the damage amount for cold temperatures.
     *
     * @param damage The damage per interval (1.0 = 0.5 hearts)
     * @throws IllegalArgumentException if damage is negative
     */
    void setColdDamage(float damage);

    /**
     * Set the damage amount for hot temperatures.
     *
     * @param damage The damage per interval (1.0 = 0.5 hearts)
     * @throws IllegalArgumentException if damage is negative
     */
    void setHotDamage(float damage);

    /**
     * Check if temperature damage is enabled.
     *
     * @return true if damage is enabled
     */
    boolean isTemperatureDamageEnabled();

    /**
     * Check if status effects are enabled.
     *
     * @return true if effects are enabled
     */
    boolean isStatusEffectsEnabled();

    /**
     * Check if warnings are enabled.
     *
     * @return true if warnings are enabled
     */
    boolean isWarningsEnabled();

    /**
     * Get the current damage interval.
     *
     * @return The number of ticks between damage
     */
    int getDamageInterval();

    /**
     * Get the current cold damage amount.
     *
     * @return The damage per interval
     */
    float getColdDamage();

    /**
     * Get the current hot damage amount.
     *
     * @return The damage per interval
     */
    float getHotDamage();
}