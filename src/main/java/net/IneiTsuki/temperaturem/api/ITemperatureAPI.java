package net.IneiTsuki.temperaturem.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Public API for other mods to interact with the Temperature system.
 * This interface provides safe methods to query and modify player temperatures,
 * as well as register custom temperature sources.
 */
public interface ITemperatureAPI {

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
     *
     * @param player The player to modify
     * @param delta The temperature change in degrees Celsius
     */
    void modifyPlayerTemperature(ServerPlayerEntity player, double delta);

    /**
     * Sets a player's temperature to an exact value.
     * This bypasses gradual temperature changes.
     *
     * @param player The player to modify
     * @param temperature The new temperature in degrees Celsius
     */
    void setPlayerTemperature(ServerPlayerEntity player, double temperature);

    /**
     * Registers a custom temperature value for a block.
     * This will override any existing temperature for this block.
     *
     * @param block The block identifier (e.g., "modid:block_name")
     * @param temperature The temperature effect in degrees Celsius
     */
    void registerBlockTemperature(Identifier block, int temperature);

    /**
     * Registers a custom base temperature for a biome.
     * This will override the biome's vanilla temperature calculation.
     *
     * @param biome The biome identifier (e.g., "minecraft:desert")
     * @param temperature The base temperature in degrees Celsius
     */
    void registerBiomeTemperature(Identifier biome, int temperature);

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
}