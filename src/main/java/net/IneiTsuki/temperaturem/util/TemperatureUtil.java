package net.IneiTsuki.temperaturem.util;

import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.IneiTsuki.temperaturem.seasons.SeasonManager;
import net.IneiTsuki.temperaturem.zones.TemperatureZone;
import net.IneiTsuki.temperaturem.zones.TemperatureZoneManager;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;

public class TemperatureUtil {

    private static final int CLOSE_RADIUS = 1;
    private static final int MEDIUM_RADIUS = 2;

    // Block influence weights
    private static final double CLOSE_WEIGHT = 0.7;
    private static final double MEDIUM_WEIGHT = 0.35;
    private static final double FEET_MULTIPLIER = 0.5;
    private static final double POSITION_MULTIPLIER = 1.0;

    // Biome vs block weighting
    private static final double BIOME_WEIGHT = 1.0;
    private static final double BLOCK_WEIGHT = 0.3;

    public static double getTargetTemperature(World world, BlockPos playerPos) {
        // Check for temperature zones first (highest priority)
        if (world instanceof ServerWorld serverWorld) {
            Double zoneTemp = getZoneTemperature(serverWorld, playerPos);
            if (zoneTemp != null) {
                return clamp(zoneTemp, -50, 150);
            }
        }

        // Fall back to standard calculation
        double baseTemp = getBaseTemperature(world, playerPos) * BIOME_WEIGHT;
        double blockInfluence = getNearbyBlockInfluence(world, playerPos) * BLOCK_WEIGHT;
        double environmentMod = getEnvironmentalModifiers(world, playerPos);

        double finalTemp = baseTemp + blockInfluence + environmentMod;

        // Apply seasonal effects
        if (world instanceof ServerWorld serverWorld) {
            SeasonManager seasonManager = SeasonManager.get(serverWorld);
            if (seasonManager.isEnabled()) {
                // Apply multiplier first
                double multiplier = seasonManager.getSeasonalTemperatureMultiplier();
                finalTemp *= multiplier;

                // Then add modifier
                double modifier = seasonManager.getSeasonalTemperatureModifier();
                finalTemp += modifier;
            }
        }

        return clamp(finalTemp, -50, 150);
    }

    private static double getBaseTemperature(World world, BlockPos pos) {
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);

        Identifier biomeId = biomeEntry.getKey()
                .map(RegistryKey::getValue)
                .orElse(null);

        Integer biomeOverride = biomeId != null
                ? BiomeTemperatureRegistry.getTemperatureOverride(biomeId)
                : null;

        double altitudeMod = Math.max(-20, Math.min(10, (64 - pos.getY()) * 0.1));
        double timeMod = getTimeTemperatureModifier(world);

        if (biomeOverride != null) {
            // Reduce modifiers for extreme biomes
            if (Math.abs(biomeOverride) > 40) {
                timeMod *= 0.5;
                altitudeMod *= 0.3;
            }
            return biomeOverride + timeMod + altitudeMod;
        }

        Biome biome = biomeEntry.value();
        double biomeTemp = biome.getTemperature() * 20.0;

        return biomeTemp + altitudeMod + timeMod;
    }

    private static double getTimeTemperatureModifier(World world) {
        long time = world.getTimeOfDay() % 24000;
        double normalizedTime = ((time - 6000) / 24000.0) * 2 * Math.PI;
        return Math.cos(normalizedTime) * 3.0;
    }

    private static double getNearbyBlockInfluence(World world, BlockPos center) {
        double influence = 0;
        int heatSources = 0;
        int coldSources = 0;

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // Check center and feet blocks first
        BlockState[] specialStates = {
                world.getBlockState(center),
                world.getBlockState(center.down())
        };
        double[] multipliers = {POSITION_MULTIPLIER, FEET_MULTIPLIER};

        for (int i = 0; i < specialStates.length; i++) {
            int temp = TemperatureRegistry.getTemperature(specialStates[i]);
            if (temp != 0) {
                influence += temp * CLOSE_WEIGHT * multipliers[i];
                if (temp > 0) heatSources++;
                else coldSources++;
            }
        }

        // Close radius blocks
        for (int x = -CLOSE_RADIUS; x <= CLOSE_RADIUS; x++) {
            for (int y = -CLOSE_RADIUS; y <= CLOSE_RADIUS; y++) {
                for (int z = -CLOSE_RADIUS; z <= CLOSE_RADIUS; z++) {
                    if ((x == 0 && y == 0 && z == 0) || (x == 0 && y == -1 && z == 0)) continue;
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    int temp = TemperatureRegistry.getTemperature(world.getBlockState(mutablePos));
                    if (temp != 0) {
                        influence += temp * CLOSE_WEIGHT;
                        if (temp > 0) heatSources++;
                        else coldSources++;
                    }
                }
            }
        }

        // Medium radius blocks with distance weighting
        for (int x = -MEDIUM_RADIUS; x <= MEDIUM_RADIUS; x++) {
            for (int y = -MEDIUM_RADIUS; y <= MEDIUM_RADIUS; y++) {
                for (int z = -MEDIUM_RADIUS; z <= MEDIUM_RADIUS; z++) {
                    if (Math.abs(x) <= CLOSE_RADIUS && Math.abs(y) <= CLOSE_RADIUS && Math.abs(z) <= CLOSE_RADIUS)
                        continue;
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    int temp = TemperatureRegistry.getTemperature(world.getBlockState(mutablePos));
                    if (temp != 0) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        influence += temp * (MEDIUM_WEIGHT / distance);
                        if (temp > 0) heatSources++;
                        else coldSources++;
                    }
                }
            }
        }

        // Diminishing effect: only strong if multiple blocks
        int totalSources = heatSources + coldSources;
        if (totalSources > 0) {
            double factor = Math.min(1.0, totalSources / 3.0);
            influence *= factor;
        }

        return influence;
    }

    private static double getEnvironmentalModifiers(World world, BlockPos pos) {
        double modifier = 0;

        if (isUnderRoof(world, pos)) {
            modifier += 5.0;
        }

        if (world.isThundering()) modifier -= 12.0;
        else if (world.isRaining()) modifier -= 8.0;

        if (pos.getY() < 50) modifier += 3.0;

        BlockState state = world.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) {
            if (state.getFluidState().isOf(Fluids.WATER) || state.getFluidState().isOf(Fluids.FLOWING_WATER))
                modifier -= 15.0;
        }

        return modifier;
    }

    private static boolean isUnderRoof(World world, BlockPos pos) {
        for (int y = 1; y <= 5; y++) {
            BlockPos checkPos = pos.up(y);
            BlockState state = world.getBlockState(checkPos);
            if (!state.isAir() && state.isOpaque()) return true;
        }
        return false;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Deprecated
    public static double getTemperatureDelta(World world, BlockPos center, int radius) {
        return getTargetTemperature(world, center);
    }

    public static double getBlockContribution(World world, BlockPos pos) {
        return TemperatureRegistry.getTemperature(world.getBlockState(pos));
    }

    private static Double getZoneTemperature(ServerWorld world, BlockPos pos) {
        TemperatureZoneManager manager = TemperatureZoneManager.get(world);
        List<TemperatureZone> zones = manager.getZonesAt(pos);

        if (zones.isEmpty()) {
            return null;
        }

        // Get highest priority zone
        TemperatureZone primaryZone = zones.get(0);

        switch (primaryZone.getType()) {
            case ABSOLUTE:
                // Absolute zones completely override all other calculations
                // Note: Seasons still don't affect absolute zones - this is intentional
                // as absolute zones are meant to have exact temperatures
                return primaryZone.getTemperatureAt(pos);

            case ADDITIVE:
                // Additive zones modify the base temperature
                double baseTemp = getBaseTemperature(world, pos) * BIOME_WEIGHT;
                double blockInfluence = getNearbyBlockInfluence(world, pos) * BLOCK_WEIGHT;
                double environmentMod = getEnvironmentalModifiers(world, pos);
                double naturalTemp = baseTemp + blockInfluence + environmentMod;

                // Apply seasonal effects to natural temperature
                SeasonManager seasonManager = SeasonManager.get(world);
                if (seasonManager.isEnabled()) {
                    naturalTemp *= seasonManager.getSeasonalTemperatureMultiplier();
                    naturalTemp += seasonManager.getSeasonalTemperatureModifier();
                }

                // Add all additive zone effects
                double zoneModifier = 0;
                for (TemperatureZone zone : zones) {
                    if (zone.getType() == TemperatureZone.ZoneType.ADDITIVE) {
                        zoneModifier += zone.getTemperatureAt(pos);
                    }
                }

                return naturalTemp + zoneModifier;

            case MULTIPLIER:
                // Multiplier zones scale the natural temperature
                double naturalTemp2 = getBaseTemperature(world, pos) * BIOME_WEIGHT;
                naturalTemp2 += getNearbyBlockInfluence(world, pos) * BLOCK_WEIGHT;
                naturalTemp2 += getEnvironmentalModifiers(world, pos);

                // Apply seasonal effects first
                SeasonManager seasonManager2 = SeasonManager.get(world);
                if (seasonManager2.isEnabled()) {
                    naturalTemp2 *= seasonManager2.getSeasonalTemperatureMultiplier();
                    naturalTemp2 += seasonManager2.getSeasonalTemperatureModifier();
                }

                // Then apply zone multiplier
                double multiplier = primaryZone.getTemperatureAt(pos) / 100.0; // Zone temp as percentage
                return naturalTemp2 * multiplier;

            default:
                return null;
        }
    }
}