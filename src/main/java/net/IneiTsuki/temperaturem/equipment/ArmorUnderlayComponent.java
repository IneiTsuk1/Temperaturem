package net.IneiTsuki.temperaturem.equipment;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class ArmorUnderlayComponent {

    private static final String NBT_KEY = "TemperatureUnderlay";

    public static boolean hasUnderlay(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(NBT_KEY);
    }

    public static TemperatureUnderlay getUnderlay(ItemStack stack) {
        if (!hasUnderlay(stack)) return null;

        NbtCompound nbt = stack.getNbt();
        NbtCompound underlayNbt = nbt.getCompound(NBT_KEY);
        return TemperatureUnderlay.fromNbt(underlayNbt);
    }

    public static void setUnderlay(ItemStack stack, TemperatureUnderlay underlay) {
        if (stack == null || stack.isEmpty()) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        if (underlay != null) {
            nbt.put(NBT_KEY, underlay.toNbt());
        } else {
            nbt.remove(NBT_KEY);
        }
    }

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

    public static void repairUnderlay(ItemStack stack, int amount) {
        TemperatureUnderlay underlay = getUnderlay(stack);
        if (underlay == null) return;

        underlay.repair(amount);
        setUnderlay(stack, underlay);
    }

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