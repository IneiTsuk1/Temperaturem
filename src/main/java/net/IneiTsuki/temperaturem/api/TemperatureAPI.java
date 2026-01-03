package net.IneiTsuki.temperaturem.api;

import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.IneiTsuki.temperaturem.effects.TemperatureEffects;
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
 *
 * // Player temperature manipulation
 * int temp = api.getPlayerTemperature(player);
 * api.modifyPlayerTemperature(player, -10.0); // Cool player by 10 degrees
 *
 * // Runtime registration
 * api.registerBlockTemperature(new Identifier("mymod", "hot_rock"), 80);
 * api.registerBiomeTemperature(new Identifier("mymod", "volcanic_wastes"), 95);
 *
 * // Effect configuration
 * api.setEnableTemperatureDamage(true);
 * api.setDamageInterval(40); // Damage every 2 seconds
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

    // ===== Player Temperature Methods =====

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

    // ===== Runtime Registration Methods =====

    @Override
    public void registerBlockTemperature(Identifier block, int temperature) {
        if (block == null) {
            throw new IllegalArgumentException("Block identifier cannot be null");
        }

        if (!TemperatureRegistry.registerRuntime(block, temperature)) {
            throw new IllegalArgumentException(
                    "Failed to register block temperature for '" + block + "'. " +
                            "Block may not exist or temperature is out of range (-273 to 1000°C)."
            );
        }
    }

    @Override
    public void registerBiomeTemperature(Identifier biome, int temperature) {
        if (biome == null) {
            throw new IllegalArgumentException("Biome identifier cannot be null");
        }

        if (!BiomeTemperatureRegistry.registerRuntime(biome, temperature)) {
            throw new IllegalArgumentException(
                    "Failed to register biome temperature for '" + biome + "'. " +
                            "Temperature may be out of range (-273 to 1000°C)."
            );
        }
    }

    @Override
    public boolean unregisterBlockTemperature(Identifier block) {
        return TemperatureRegistry.unregisterRuntime(block);
    }

    @Override
    public boolean unregisterBiomeTemperature(Identifier biome) {
        return BiomeTemperatureRegistry.unregisterRuntime(biome);
    }

    // ===== Query Methods =====

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

    // ===== Effect Configuration Methods =====

    @Override
    public void setEnableTemperatureDamage(boolean enable) {
        TemperatureEffects.setEnableDamage(enable);
    }

    @Override
    public void setEnableStatusEffects(boolean enable) {
        TemperatureEffects.setEnableStatusEffects(enable);
    }

    @Override
    public void setEnableWarnings(boolean enable) {
        TemperatureEffects.setEnableWarnings(enable);
    }

    @Override
    public void setDamageInterval(int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("Damage interval must be at least 1 tick");
        }
        TemperatureEffects.setDamageInterval(ticks);
    }

    @Override
    public void setColdDamage(float damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative");
        }
        TemperatureEffects.setColdDamage(damage);
    }

    @Override
    public void setHotDamage(float damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage cannot be negative");
        }
        TemperatureEffects.setHotDamage(damage);
    }

    @Override
    public boolean isTemperatureDamageEnabled() {
        return TemperatureEffects.isEnableDamage();
    }

    @Override
    public boolean isStatusEffectsEnabled() {
        return TemperatureEffects.isEnableStatusEffects();
    }

    @Override
    public boolean isWarningsEnabled() {
        return TemperatureEffects.isEnableWarnings();
    }

    @Override
    public int getDamageInterval() {
        return TemperatureEffects.getDamageInterval();
    }

    @Override
    public float getColdDamage() {
        return TemperatureEffects.getColdDamage();
    }

    @Override
    public float getHotDamage() {
        return TemperatureEffects.getHotDamage();
    }
}