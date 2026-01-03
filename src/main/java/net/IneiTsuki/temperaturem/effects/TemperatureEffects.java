package net.IneiTsuki.temperaturem.effects;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Handles temperature-based effects on players.
 * Effects are applied based on temperature thresholds.
 */
public class TemperatureEffects {

    // Temperature thresholds
    private static final int EXTREME_COLD = -20;
    private static final int VERY_COLD = -10;
    private static final int COLD = 0;
    private static final int COMFORTABLE_LOW = 12;
    private static final int COMFORTABLE_HIGH = 25;
    private static final int HOT = 40;
    private static final int VERY_HOT = 55;
    private static final int EXTREME_HOT = 65;

    // Effect configuration
    private static boolean enableDamage = true;
    private static boolean enableStatusEffects = true;
    private static boolean enableWarnings = true;

    private static int damageInterval = 40; // Ticks between damage (2 seconds)
    private static float coldDamage = 1.0f;
    private static float hotDamage = 1.0f;

    /**
     * Apply temperature effects to a player based on their current temperature.
     * Should be called periodically (e.g., every tick).
     */
    public static void applyEffects(ServerPlayerEntity player, int temperature, int tickCounter) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        // Apply status effects
        if (enableStatusEffects) {
            applyStatusEffects(player, temperature);
        }

        // Apply damage
        if (enableDamage && tickCounter % damageInterval == 0) {
            applyTemperatureDamage(player, temperature);
        }

        // Send warnings
        if (enableWarnings && tickCounter % 200 == 0) { // Every 10 seconds
            sendTemperatureWarning(player, temperature);
        }
    }

    /**
     * Apply status effects based on temperature.
     */
    private static void applyStatusEffects(ServerPlayerEntity player, int temperature) {
        // Cold effects
        if (temperature <= EXTREME_COLD) {
            // Extreme cold: Slowness III, Mining Fatigue II, Weakness II
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 60, 2, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.MINING_FATIGUE, 60, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS, 60, 1, false, false, true
            ));
        } else if (temperature <= VERY_COLD) {
            // Very cold: Slowness I, Mining Fatigue I
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 60, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.MINING_FATIGUE, 60, 0, false, false, true));
        } else if (temperature <= COLD) {
            // Cold: Slowness I
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 60, 0, false, false, true));
        }

        // Hot effects
        if (temperature >= EXTREME_HOT) {
            // Extreme heat: Weakness II, Nausea
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS, 60, 1, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA, 100, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 60, 1, false, false, true));
        } else if (temperature >= VERY_HOT) {
            // Very hot: Weakness I, Hunger
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS, 60, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HUNGER, 60, 0, false, false, true));
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 60, 0, false, false, true));
        } else if (temperature >= HOT) {
            // Hot: Hunger
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HUNGER, 60, 0, false, false, true));
        }

        // Comfortable range: slight regeneration bonus
        if (temperature >= COMFORTABLE_LOW && temperature <= COMFORTABLE_HIGH) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    /**
     * Apply direct damage from extreme temperatures.
     */
    private static void applyTemperatureDamage(ServerPlayerEntity player, int temperature) {
        float damage = 0;

        // Cold damage
        if (temperature <= EXTREME_COLD) {
            damage = coldDamage * 2.0f; // 2 damage (1 heart)
        } else if (temperature <= VERY_COLD) {
            damage = coldDamage; // 1 damage (0.5 hearts)
        }

        // Hot damage
        if (temperature >= EXTREME_HOT) {
            damage = hotDamage * 2.0f; // 2 damage (1 heart)
        } else if (temperature >= VERY_HOT) {
            damage = hotDamage; // 1 damage (0.5 hearts)
        }

        if (damage > 0) {
            player.damage(player.getDamageSources().freeze(), damage);
            Temperaturem.LOGGER.debug("Applied {} temperature damage to {}", damage, player.getName().getString());
        }
    }

    /**
     * Send warning messages to player about dangerous temperatures.
     */
    private static void sendTemperatureWarning(ServerPlayerEntity player, int temperature) {
        Text warning = null;
        Formatting color = Formatting.WHITE;

        if (temperature <= EXTREME_COLD) {
            warning = Text.literal("You're freezing! Find warmth immediately!").formatted(Formatting.AQUA, Formatting.BOLD);
        } else if (temperature <= VERY_COLD) {
            warning = Text.literal("You're very cold. Seek shelter or a heat source.").formatted(Formatting.BLUE);
        } else if (temperature >= EXTREME_HOT) {
            warning = Text.literal("Extreme heat! Cool down immediately!").formatted(Formatting.RED, Formatting.BOLD);
        } else if (temperature >= VERY_HOT) {
            warning = Text.literal("You're overheating. Find shade or water.").formatted(Formatting.GOLD);
        }

        if (warning != null) {
            player.sendMessage(warning, true); // true = action bar
        }
    }

    // Configuration methods

    public static void setEnableDamage(boolean enable) {
        enableDamage = enable;
    }

    public static void setEnableStatusEffects(boolean enable) {
        enableStatusEffects = enable;
    }

    public static void setEnableWarnings(boolean enable) {
        enableWarnings = enable;
    }

    public static void setDamageInterval(int ticks) {
        damageInterval = Math.max(1, ticks);
    }

    public static void setColdDamage(float damage) {
        coldDamage = Math.max(0, damage);
    }

    public static void setHotDamage(float damage) {
        hotDamage = Math.max(0, damage);
    }

    public static boolean isEnableDamage() {
        return enableDamage;
    }

    public static boolean isEnableStatusEffects() {
        return enableStatusEffects;
    }

    public static boolean isEnableWarnings() {
        return enableWarnings;
    }

    public static int getDamageInterval() {
        return damageInterval;
    }

    public static float getColdDamage() {
        return coldDamage;
    }

    public static float getHotDamage() {
        return hotDamage;
    }
}