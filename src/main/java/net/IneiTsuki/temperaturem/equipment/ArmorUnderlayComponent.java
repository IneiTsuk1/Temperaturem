package net.IneiTsuki.temperaturem.equipment;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Utility class for managing temperature underlays attached to armor pieces.
 * Uses NBT data to store underlay information on ItemStacks.
 */
public class ArmorUnderlayComponent {

    private static final String NBT_KEY = "TemperatureUnderlay";

    /**
     * Checks if an armor piece has an underlay attached.
     */
    public static boolean hasUnderlay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(NBT_KEY);
    }

    /**
     * Gets the underlay attached to an armor piece.
     * @return The underlay, or null if none is attached
     */
    public static TemperatureUnderlay getUnderlay(ItemStack stack) {
        if (!hasUnderlay(stack)) return null;

        NbtCompound nbt = stack.getNbt();
        NbtCompound underlayNbt = nbt.getCompound(NBT_KEY);
        return TemperatureUnderlay.fromNbt(underlayNbt);
    }

    /**
     * Attaches an underlay to an armor piece.
     * Replaces any existing underlay.
     */
    public static void setUnderlay(ItemStack stack, TemperatureUnderlay underlay) {
        if (stack == null || stack.isEmpty()) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        if (underlay != null) {
            nbt.put(NBT_KEY, underlay.toNbt());
        } else {
            nbt.remove(NBT_KEY);
        }
    }

    /**
     * Removes the underlay from an armor piece.
     * @return The removed underlay, or null if none existed
     */
    public static TemperatureUnderlay removeUnderlay(ItemStack stack) {
        TemperatureUnderlay underlay = getUnderlay(stack);
        if (underlay != null) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_KEY);
            }
        }
        return underlay;
    }

    /**
     * Damages the underlay on an armor piece.
     * @param amount Amount of damage to apply
     * @return true if the underlay broke
     */
    public static boolean damageUnderlay(ItemStack stack, int amount) {
        TemperatureUnderlay underlay = getUnderlay(stack);
        if (underlay == null) return false;

        boolean broke = underlay.damage(amount);

        if (broke) {
            removeUnderlay(stack);
            return true;
        } else {
            setUnderlay(stack, underlay);
            return false;
        }
    }

    /**
     * Repairs the underlay on an armor piece.
     */
    public static void repairUnderlay(ItemStack stack, int amount) {
        TemperatureUnderlay underlay = getUnderlay(stack);
        if (underlay == null) return;

        underlay.repair(amount);
        setUnderlay(stack, underlay);
    }

    /**
     * Gets the total temperature protection from all equipped armor with underlays.
     */
    public static double getTotalProtection(Iterable<ItemStack> armorItems) {
        double total = 0.0;

        for (ItemStack stack : armorItems) {
            TemperatureUnderlay underlay = getUnderlay(stack);
            if (underlay != null && !underlay.isBroken()) {
                total += underlay.getProtectionValue();
            }
        }

        return total;
    }
}