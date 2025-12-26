package net.IneiTsuki.temperaturem.util;

import net.IneiTsuki.temperaturem.data.BiomeTemperatureRegistry;
import net.IneiTsuki.temperaturem.data.TemperatureRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class TemperatureUtil {

    private static final int CLOSE_RADIUS = 1;
    private static final int MEDIUM_RADIUS = 2;
    private static final double CLOSE_WEIGHT = 1.5;
    private static final double MEDIUM_WEIGHT = 0.3;
    private static final double FEET_MULTIPLIER = 4.0;
    private static final double POSITION_MULTIPLIER = 3.0;

    public static double getTargetTemperature(World world, BlockPos playerPos) {
        double baseTemp = getBaseTemperature(world, playerPos);
        double blockInfluence = getNearbyBlockInfluence(world, playerPos);
        double environmentMod = getEnvironmentalModifiers(world, playerPos);

        return clamp(baseTemp + blockInfluence + environmentMod, -50, 150);
    }

    private static double getBaseTemperature(World world, BlockPos pos) {
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);

        // Check if biome has a custom temperature override
        Integer biomeOverride = BiomeTemperatureRegistry.getTemperatureOverride(biomeEntry);
        if (biomeOverride != null) {
            // Use the custom biome temperature directly
            double timeMod = getTimeTemperatureModifier(world);
            double altitudeMod = Math.max(-20, Math.min(10, (64 - pos.getY()) * 0.1));

            // For extreme biomes, reduce time/altitude modifiers impact
            if (Math.abs(biomeOverride) > 40) {
                timeMod *= 0.5; // Extreme biomes are less affected by time
                altitudeMod *= 0.3; // And less affected by altitude
            }

            return biomeOverride + timeMod + altitudeMod;
        }

        // Fall back to vanilla biome temperature if no override
        Biome biome = biomeEntry.value();
        double biomeTemp = biome.getTemperature() * 20.0;
        double altitudeMod = Math.max(-20, Math.min(10, (64 - pos.getY()) * 0.1));
        double timeMod = getTimeTemperatureModifier(world);

        return biomeTemp + altitudeMod + timeMod;
    }

    private static double getTimeTemperatureModifier(World world) {
        long time = world.getTimeOfDay() % 24000;
        double normalizedTime = (time / 24000.0) * 2 * Math.PI;
        return Math.cos(normalizedTime) * 3.0;
    }

    private static double getNearbyBlockInfluence(World world, BlockPos center) {
        double influence = 0;
        int heatSourceCount = 0;
        int coldSourceCount = 0;

        for (int x = -CLOSE_RADIUS; x <= CLOSE_RADIUS; x++) {
            for (int y = -CLOSE_RADIUS; y <= CLOSE_RADIUS; y++) {
                for (int z = -CLOSE_RADIUS; z <= CLOSE_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);
                    int blockTemp = TemperatureRegistry.getTemperature(world.getBlockState(pos));

                    if (blockTemp != 0) {
                        double weight = CLOSE_WEIGHT;

                        if (x == 0 && y == -1 && z == 0) {
                            weight *= FEET_MULTIPLIER;
                        } else if (x == 0 && y == 0 && z == 0) {
                            weight *= POSITION_MULTIPLIER;
                        }

                        influence += blockTemp * weight;

                        if (blockTemp > 0) heatSourceCount++;
                        else coldSourceCount++;
                    }
                }
            }
        }

        for (int x = -MEDIUM_RADIUS; x <= MEDIUM_RADIUS; x++) {
            for (int y = -MEDIUM_RADIUS; y <= MEDIUM_RADIUS; y++) {
                for (int z = -MEDIUM_RADIUS; z <= MEDIUM_RADIUS; z++) {
                    if (Math.abs(x) <= CLOSE_RADIUS &&
                            Math.abs(y) <= CLOSE_RADIUS &&
                            Math.abs(z) <= CLOSE_RADIUS) {
                        continue;
                    }

                    BlockPos pos = center.add(x, y, z);
                    int blockTemp = TemperatureRegistry.getTemperature(world.getBlockState(pos));

                    if (blockTemp != 0) {
                        double distance = Math.sqrt(x*x + y*y + z*z);
                        double weight = MEDIUM_WEIGHT / distance;
                        influence += blockTemp * weight;

                        if (blockTemp > 0) heatSourceCount++;
                        else coldSourceCount++;
                    }
                }
            }
        }

        int totalSources = heatSourceCount + coldSourceCount;
        if (totalSources > 3) {
            double diminishingFactor = Math.log(totalSources) / Math.log(3);
            influence = influence / diminishingFactor;
        }

        return influence;
    }

    private static double getEnvironmentalModifiers(World world, BlockPos pos) {
        double modifier = 0;

        if (isUnderRoof(world, pos)) {
            modifier += 5.0;
        }

        if (world.isRaining()) {
            modifier -= isUnderRoof(world, pos) ? 2.0 : 8.0;
        }

        if (world.isThundering()) {
            modifier -= 3.0;
        }

        if (pos.getY() < 50) {
            modifier += 3.0;
        }

        BlockState stateAtPos = world.getBlockState(pos);
        if (stateAtPos.getFluidState().isStill() || !stateAtPos.getFluidState().isEmpty()) {
            modifier -= 15.0;
        }

        return modifier;
    }

    private static boolean isUnderRoof(World world, BlockPos pos) {
        for (int y = 1; y <= 5; y++) {
            BlockPos checkPos = pos.up(y);
            BlockState state = world.getBlockState(checkPos);

            if (!state.isAir() && state.isOpaque()) {
                return true;
            }
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
}