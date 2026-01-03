package net.IneiTsuki.temperaturem.equipment;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Manages temperature protection from armor underlays.
 * Integrates with the main temperature system.
 */
public class UnderlayTemperatureManager {

    private static final int DURABILITY_DAMAGE_INTERVAL = 200; // Damage every 10 seconds
    private static final int BASE_DURABILITY_DAMAGE = 1;

    /**
     * Calculates the temperature protection bonus from a player's equipped armor.
     *
     * @param player The player to check
     * @return The temperature offset from underlays (positive reduces heat, negative reduces cold)
     */
    public static double calculateArmorProtection(ServerPlayerEntity player) {
        if (player == null) return 0.0;

        Iterable<ItemStack> armor = player.getArmorItems();
        return ArmorUnderlayComponent.getTotalProtection(armor);
    }

    /**
     * Applies wear to underlays based on environmental stress.
     * Should be called periodically (e.g., every tick from PlayerTemperatureManager).
     *
     * @param player The player whose armor to damage
     * @param currentTemp The player's current temperature
     * @param tickCounter The current tick counter
     */
    public static void damageUnderlays(ServerPlayerEntity player, int currentTemp, int tickCounter) {
        if (player == null || player.isCreative() || player.isSpectator()) return;

        // Only damage every N ticks
        if (tickCounter % DURABILITY_DAMAGE_INTERVAL != 0) return;

        // Determine if we're in extreme conditions
        boolean extremeCold = currentTemp <= -20;
        boolean extremeHeat = currentTemp >= 65;
        boolean moderateCold = currentTemp <= 0 && !extremeCold;
        boolean moderateHeat = currentTemp >= 40 && !extremeHeat;

        if (!extremeCold && !extremeHeat && !moderateCold && !moderateHeat) {
            return; // No damage in comfortable conditions
        }

        // Damage each armor piece with an underlay
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;

            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            TemperatureUnderlay underlay = ArmorUnderlayComponent.getUnderlay(stack);
            if (underlay == null || underlay.isBroken()) continue;

            // Calculate damage based on conditions and underlay type
            int damage = calculateUnderlayDamage(underlay, extremeCold, extremeHeat, moderateCold, moderateHeat);

            if (damage > 0) {
                boolean broke = ArmorUnderlayComponent.damageUnderlay(stack, damage);

                if (broke) {
                    Temperaturem.LOGGER.debug("Underlay broke on {} for player {}",
                            slot.getName(), player.getName().getString());
                    // Could add a message to player here
                }
            }
        }
    }

    /**
     * Calculates durability damage for an underlay based on conditions.
     */
    private static int calculateUnderlayDamage(TemperatureUnderlay underlay,
                                               boolean extremeCold, boolean extremeHeat,
                                               boolean moderateCold, boolean moderateHeat) {
        int damage = 0;

        // Underlays take damage when protecting against their intended conditions
        if (underlay.protectsFromCold()) {
            if (extremeCold) damage = BASE_DURABILITY_DAMAGE * 2;
            else if (moderateCold) damage = BASE_DURABILITY_DAMAGE;
        }

        if (underlay.protectsFromHeat()) {
            if (extremeHeat) damage = BASE_DURABILITY_DAMAGE * 2;
            else if (moderateHeat) damage = BASE_DURABILITY_DAMAGE;
        }

        return damage;
    }

    /**
     * Gets a description of the player's current underlay protection.
     * Useful for debugging or UI display.
     */
    public static String getProtectionSummary(ServerPlayerEntity player) {
        if (player == null) return "No player";

        StringBuilder sb = new StringBuilder();
        double totalProtection = 0.0;
        int underlayCount = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;

            ItemStack stack = player.getEquippedStack(slot);
            TemperatureUnderlay underlay = ArmorUnderlayComponent.getUnderlay(stack);

            if (underlay != null && !underlay.isBroken()) {
                double protection = underlay.getProtectionValue();
                totalProtection += protection;
                underlayCount++;

                sb.append(String.format("%s: %s (%.1f°C, %d%% durability)\n",
                        slot.getName(),
                        underlay.getType().getDisplayName(),
                        protection,
                        (int)(underlay.getDurabilityPercent() * 100)));
            }
        }

        if (underlayCount == 0) {
            return "No underlays equipped";
        }

        sb.append(String.format("\nTotal Protection: %.1f°C (%d pieces)", totalProtection, underlayCount));
        return sb.toString();
    }
}