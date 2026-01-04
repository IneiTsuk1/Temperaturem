package net.IneiTsuki.temperaturem.api;

import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.IneiTsuki.temperaturem.effects.TemperatureEffects;
import net.IneiTsuki.temperaturem.player.PlayerTemperature;
import net.IneiTsuki.temperaturem.player.PlayerTemperatureManager;
import net.IneiTsuki.temperaturem.seasons.Season;
import net.IneiTsuki.temperaturem.seasons.SeasonManager;
import net.IneiTsuki.temperaturem.zones.TemperatureZone;
import net.IneiTsuki.temperaturem.zones.TemperatureZoneManager;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the Temperature API for other mods to use.
 * Access via: TemperatureAPI.getInstance()
 */
public class TemperatureAPI implements ITemperatureAPI, ITemperatureZoneAPI, ISeasonAPI {

    private static final TemperatureAPI INSTANCE = new TemperatureAPI();

    private TemperatureAPI() {
        // Private constructor - use getInstance()
    }

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

    // ===== Zone API Implementation =====

    @Override
    public TemperatureZone createZone(ServerWorld world, String name, BlockPos pos1, BlockPos pos2,
                                      double temperature, TemperatureZone.ZoneType type) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone name cannot be empty");
        }
        if (temperature < -273 || temperature > 1000) {
            throw new IllegalArgumentException("Temperature out of range (-273 to 1000°C)");
        }

        Box bounds = createBoundingBox(pos1, pos2);
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.createZone(name, bounds, temperature, type);
    }

    private Box createBoundingBox(BlockPos pos1, BlockPos pos2) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public TemperatureZone createZone(ServerWorld world, String name, Box bounds,
                                      double temperature, TemperatureZone.ZoneType type) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone name cannot be empty");
        }
        if (bounds == null) {
            throw new IllegalArgumentException("Bounds cannot be null");
        }
        if (temperature < -273 || temperature > 1000) {
            throw new IllegalArgumentException("Temperature out of range (-273 to 1000°C)");
        }

        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.createZone(name, bounds, temperature, type);
    }

    @Override
    public boolean removeZone(ServerWorld world, UUID zoneId) {
        if (world == null || zoneId == null) return false;
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.removeZone(zoneId);
    }

    @Override
    public TemperatureZone getZone(ServerWorld world, UUID zoneId) {
        if (world == null || zoneId == null) return null;
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getZone(zoneId);
    }

    @Override
    public Collection<TemperatureZone> getAllZones(ServerWorld world) {
        if (world == null) return List.of();
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getAllZones();
    }

    @Override
    public List<TemperatureZone> getZonesAt(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return List.of();
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getZonesAt(pos);
    }

    @Override
    public Double getZoneTemperatureAt(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return null;
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getZoneTemperatureAt(pos);
    }

    @Override
    public List<TemperatureZone> findZonesByName(ServerWorld world, String nameQuery) {
        if (world == null || nameQuery == null) return List.of();
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.findZonesByName(nameQuery);
    }

    @Override
    public List<TemperatureZone> getZonesInArea(ServerWorld world, Box area) {
        if (world == null || area == null) return List.of();
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getZonesInArea(area);
    }

    @Override
    public boolean isInZone(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return false;
        return !getZonesAt(world, pos).isEmpty();
    }

    @Override
    public void setZoneTemperature(ServerWorld world, UUID zoneId, double temperature) {
        if (temperature < -273 || temperature > 1000) {
            throw new IllegalArgumentException("Temperature out of range (-273 to 1000°C)");
        }

        TemperatureZone zone = getZone(world, zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }

        zone.setTemperature(temperature);
        TemperatureZoneManager.get(world).markDirty();
    }

    @Override
    public void setZoneBounds(ServerWorld world, UUID zoneId, BlockPos pos1, BlockPos pos2) {
        TemperatureZone zone = getZone(world, zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }

        Box bounds = createBoundingBox(pos1, pos2);
        zone.setBounds(bounds);
        TemperatureZoneManager.get(world).markDirty();
    }

    @Override
    public void setZonePriority(ServerWorld world, UUID zoneId, int priority) {
        TemperatureZone zone = getZone(world, zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }

        zone.setPriority(priority);
        TemperatureZoneManager.get(world).markDirty();
    }

    @Override
    public void setZoneEnabled(ServerWorld world, UUID zoneId, boolean enabled) {
        TemperatureZone zone = getZone(world, zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }

        zone.setEnabled(enabled);
        TemperatureZoneManager.get(world).markDirty();
    }

    @Override
    public void setZoneTransitionRange(ServerWorld world, UUID zoneId, double range) {
        if (range < 0) {
            throw new IllegalArgumentException("Transition range cannot be negative");
        }

        TemperatureZone zone = getZone(world, zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found: " + zoneId);
        }

        zone.setTransitionRange(range);
        TemperatureZoneManager.get(world).markDirty();
    }

    @Override
    public int getZoneCount(ServerWorld world) {
        if (world == null) return 0;
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getZoneCount();
    }

    @Override
    public int getEnabledZoneCount(ServerWorld world) {
        if (world == null) return 0;
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getEnabledZoneCount();
    }

    @Override
    public long getTotalZoneVolume(ServerWorld world) {
        if (world == null) return 0;
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        return manager.getTotalZoneVolume();
    }

    // ===== Season API Implementation =====

    @Override
    public Season getCurrentSeason(ServerWorld world) {
        if (world == null) return Season.SPRING;
        SeasonManager manager = SeasonManager.get(world);
        return manager.getCurrentSeason();
    }

    @Override
    public double getSeasonProgress(ServerWorld world) {
        if (world == null) return 0.0;
        SeasonManager manager = SeasonManager.get(world);
        return manager.getSeasonProgress();
    }

    @Override
    public int getRemainingDays(ServerWorld world) {
        if (world == null) return 0;
        SeasonManager manager = SeasonManager.get(world);
        return manager.getRemainingDays();
    }

    @Override
    public double getSeasonalTemperatureModifier(ServerWorld world) {
        if (world == null) return 0.0;
        SeasonManager manager = SeasonManager.get(world);
        return manager.getSeasonalTemperatureModifier();
    }

    @Override
    public double getSeasonalTemperatureMultiplier(ServerWorld world) {
        if (world == null) return 1.0;
        SeasonManager manager = SeasonManager.get(world);
        return manager.getSeasonalTemperatureMultiplier();
    }

    @Override
    public String getSeasonInfo(ServerWorld world) {
        if (world == null) return "No world";
        SeasonManager manager = SeasonManager.get(world);
        return manager.getSeasonInfo();
    }

    @Override
    public void setCurrentSeason(ServerWorld world, Season season) {
        if (world == null || season == null) return;
        SeasonManager manager = SeasonManager.get(world);
        manager.setCurrentSeason(season);
    }

    @Override
    public void skipToNextSeason(ServerWorld world) {
        if (world == null) return;
        SeasonManager manager = SeasonManager.get(world);
        manager.skipToNextSeason();
    }

    @Override
    public boolean isSeasonsEnabled(ServerWorld world) {
        if (world == null) return false;
        SeasonManager manager = SeasonManager.get(world);
        return manager.isEnabled();
    }

    @Override
    public void setSeasonsEnabled(ServerWorld world, boolean enabled) {
        if (world == null) return;
        SeasonManager manager = SeasonManager.get(world);
        manager.setEnabled(enabled);
    }

    @Override
    public int getSeasonLengthDays(ServerWorld world) {
        if (world == null) return 28;
        SeasonManager manager = SeasonManager.get(world);
        return manager.getSeasonLengthDays();
    }

    @Override
    public void setSeasonLengthDays(ServerWorld world, int days) {
        if (world == null) return;
        if (days < 1 || days > 1000) {
            throw new IllegalArgumentException("Season length must be between 1 and 1000 days");
        }
        SeasonManager manager = SeasonManager.get(world);
        manager.setSeasonLengthDays(days);
    }

    @Override
    public boolean isNotifySeasonChange(ServerWorld world) {
        if (world == null) return false;
        SeasonManager manager = SeasonManager.get(world);
        return manager.isNotifySeasonChange();
    }

    @Override
    public void setNotifySeasonChange(ServerWorld world, boolean notify) {
        if (world == null) return;
        SeasonManager manager = SeasonManager.get(world);
        manager.setNotifySeasonChange(notify);
    }
}