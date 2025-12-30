package net.IneiTsuki.temperaturem.api;

import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.IneiTsuki.temperaturem.player.PlayerTemperature;
import net.IneiTsuki.temperaturem.player.PlayerTemperatureManager;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Implementation of the Temperature API for other mods to use.
 * Access via: TemperatureAPI.getInstance()
 *
 * Example usage:
 * <pre>
 * TemperatureAPI api = TemperatureAPI.getInstance();
 * int temp = api.getPlayerTemperature(player);
 * api.modifyPlayerTemperature(player, -10.0); // Cool player by 10 degrees
 * </pre>
 */
public class TemperatureAPI implements ITemperatureAPI {

    private static final TemperatureAPI INSTANCE = new TemperatureAPI();

    private TemperatureAPI() {
        // Private constructor - use getInstance()
    }

    /**
     * Gets the singleton instance of the Temperature API.
     *
     * @return The API instance
     */
    public static TemperatureAPI getInstance() {
        return INSTANCE;
    }

    @Override
    public int getPlayerTemperature(ServerPlayerEntity player) {
        if (player == null) return 0;
        PlayerTemperature temp = PlayerTemperatureManager.get(player);
        return temp != null ? temp.get() : 0;
    }

    @Override
    public double getPlayerTemperatureExact(ServerPlayerEntity player) {
        if (player == null) return 0.0;
        PlayerTemperature temp = PlayerTemperatureManager.get(player);
        return temp != null ? temp.getExact() : 0.0;
    }

    @Override
    public void modifyPlayerTemperature(ServerPlayerEntity player, double delta) {
        if (player == null) return;
        PlayerTemperature temp = PlayerTemperatureManager.get(player);
        if (temp != null) {
            temp.add(delta);
            // Clamp to reasonable bounds
            double current = temp.getExact();
            if (current < -50) temp.setExact(-50);
            if (current > 150) temp.setExact(150);

            // Sync to client
            PlayerTemperatureManager.sendTemperatureToClient(player, temp.get());
        }
    }

    @Override
    public void setPlayerTemperature(ServerPlayerEntity player, double temperature) {
        if (player == null) return;
        PlayerTemperature temp = PlayerTemperatureManager.get(player);
        if (temp != null) {
            // Clamp to reasonable bounds
            temperature = Math.max(-50, Math.min(150, temperature));
            temp.setExact(temperature);

            // Sync to client
            PlayerTemperatureManager.sendTemperatureToClient(player, temp.get());
        }
    }

    @Override
    public void registerBlockTemperature(Identifier block, int temperature) {
        // Note: This would require making TemperatureRegistry methods public
        // For now, users should add entries to the config file
        throw new UnsupportedOperationException(
                "Runtime block registration not yet supported. " +
                        "Please add blocks to config/temperaturem/blocks/temperature_blocks.json"
        );
    }

    @Override
    public void registerBiomeTemperature(Identifier biome, int temperature) {
        // Note: This would require making BiomeTemperatureRegistry methods public
        // For now, users should add entries to the config file
        throw new UnsupportedOperationException(
                "Runtime biome registration not yet supported. " +
                        "Please add biomes to config/temperaturem/biomes/biome_temperatures.json"
        );
    }

    @Override
    public int getBlockTemperature(Identifier block) {
        if (block == null) return 0;
        if (!Registries.BLOCK.containsId(block)) return 0;

        Block blockObj = Registries.BLOCK.get(block);
        return TemperatureRegistry.getTemperature(blockObj.getDefaultState());
    }


    @Override
    public Integer getBiomeTemperature(Identifier biomeId) {
        if (biomeId == null) return null;
        return BiomeTemperatureRegistry.getTemperatureOverride(biomeId);
    }


    @Override
    public boolean hasBlockTemperature(Identifier block) {
        if (block == null) return false;
        if (!Registries.BLOCK.containsId(block)) return false;

        Block blockObj = Registries.BLOCK.get(block);
        return TemperatureRegistry.hasTemperature(blockObj.getDefaultState());
    }


    @Override
    public boolean hasBiomeTemperature(Identifier biomeId) {
        if (biomeId == null) return false;
        return BiomeTemperatureRegistry.hasOverride(biomeId);
    }

}