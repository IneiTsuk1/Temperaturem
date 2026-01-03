package net.IneiTsuki.temperaturem.equipment;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.IneiTsuki.temperaturem.items.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Handles applying underlays to armor pieces via inventory clicks.
 * This should be hooked into a screen handler or click event.
 */
public class UnderlayApplicationHandler {

    /**
     * Attempts to apply an underlay to armor.
     *
     * @param player The player performing the action
     * @param underlayStack The underlay item stack (in cursor/hand)
     * @param armorStack The armor piece to apply to
     * @return true if the underlay was successfully applied
     */
    public static boolean tryApplyUnderlay(PlayerEntity player, ItemStack underlayStack, ItemStack armorStack) {
        // Validate inputs
        if (player == null || underlayStack.isEmpty() || armorStack.isEmpty()) {
            return false;
        }

        // Check if the underlay stack is actually an underlay item
        if (!(underlayStack.getItem() instanceof UnderlayItem underlayItem)) {
            return false;
        }

        // Check if the target is armor
        if (!(armorStack.getItem() instanceof ArmorItem)) {
            player.sendMessage(Text.literal("Underlays can only be applied to armor pieces!")
                    .formatted(Formatting.RED), true);
            return false;
        }

        // Check if armor already has an underlay
        if (ArmorUnderlayComponent.hasUnderlay(armorStack)) {
            player.sendMessage(Text.literal("This armor already has an underlay! Remove it first.")
                    .formatted(Formatting.YELLOW), true);
            return false;
        }

        // Create and apply the underlay
        TemperatureUnderlay underlay = underlayItem.createUnderlay();
        ArmorUnderlayComponent.setUnderlay(armorStack, underlay);

        // Consume one underlay from the stack
        underlayStack.decrement(1);

        // Feedback
        player.sendMessage(
                Text.literal("Applied ").formatted(Formatting.GREEN)
                        .append(Text.literal(underlay.getType().getDisplayName()).formatted(Formatting.AQUA))
                        .append(Text.literal(" to armor!").formatted(Formatting.GREEN)),
                true
        );

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                SoundCategory.PLAYERS,
                1.0f,
                1.2f
        );

        Temperaturem.LOGGER.info("Player {} applied {} to armor",
                player.getName().getString(), underlay.getType().getDisplayName());

        return true;
    }

    /**
     * Attempts to remove an underlay from armor.
     *
     * @param player The player performing the action
     * @param armorStack The armor piece to remove underlay from
     * @return The removed underlay item stack, or empty if none was removed
     */
    public static ItemStack tryRemoveUnderlay(PlayerEntity player, ItemStack armorStack) {
        if (player == null || armorStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!(armorStack.getItem() instanceof ArmorItem)) {
            return ItemStack.EMPTY;
        }

        TemperatureUnderlay underlay = ArmorUnderlayComponent.removeUnderlay(armorStack);
        if (underlay == null) {
            player.sendMessage(Text.literal("This armor has no underlay!")
                    .formatted(Formatting.YELLOW), true);
            return ItemStack.EMPTY;
        }

        player.sendMessage(
                Text.literal("Removed ").formatted(Formatting.GREEN)
                        .append(Text.literal(underlay.getType().getDisplayName()).formatted(Formatting.AQUA))
                        .append(Text.literal(" from armor!").formatted(Formatting.GREEN)),
                true
        );

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                SoundCategory.PLAYERS,
                0.8f,
                0.9f
        );

        Temperaturem.LOGGER.info("Player {} removed {} from armor",
                player.getName().getString(), underlay.getType().getDisplayName());

        // Create the underlay item based on type
        ItemStack removedItem = createUnderlayItemStack(underlay);
        return removedItem;
    }

    /**
     * Creates an ItemStack for a removed underlay with preserved durability.
     */
    private static ItemStack createUnderlayItemStack(TemperatureUnderlay underlay) {
        // Map underlay type to the corresponding item
        ItemStack stack = switch (underlay.getType()) {
            case WOOL_LINING -> new ItemStack(ModItems.WOOL_LINING);
            case FUR_LINING -> new ItemStack(ModItems.FUR_LINING);
            case THERMAL_PADDING -> new ItemStack(ModItems.THERMAL_PADDING);
            case LEATHER_LINING -> new ItemStack(ModItems.LEATHER_LINING);
            case COOLING_MESH -> new ItemStack(ModItems.COOLING_MESH);
            case CLIMATE_WEAVE -> new ItemStack(ModItems.CLIMATE_WEAVE);
            case INSULATED_FABRIC -> new ItemStack(ModItems.INSULATED_FABRIC);
            case REFLECTIVE_LAYER -> new ItemStack(ModItems.REFLECTIVE_LAYER);
        };

        // Store the durability on the removed item so it can be reapplied later
        // Use NBT to preserve the durability state
        if (underlay.getDurability() < underlay.getMaxDurability()) {
            stack.getOrCreateNbt().putInt("StoredDurability", underlay.getDurability());
        }

        return stack;
    }

    /**
     * Checks if a player can apply an underlay (helper for UI enabling/disabling).
     */
    public static boolean canApplyUnderlay(ItemStack underlayStack, ItemStack armorStack) {
        if (underlayStack.isEmpty() || armorStack.isEmpty()) return false;
        if (!(underlayStack.getItem() instanceof UnderlayItem)) return false;
        if (!(armorStack.getItem() instanceof ArmorItem)) return false;
        if (ArmorUnderlayComponent.hasUnderlay(armorStack)) return false;
        return true;
    }

    /**
     * Adds underlay information to armor tooltips.
     */
    public static void addUnderlayTooltip(ItemStack armorStack, List<Text> tooltip) {
        if (armorStack.isEmpty() || !(armorStack.getItem() instanceof ArmorItem)) {
            return;
        }

        TemperatureUnderlay underlay = ArmorUnderlayComponent.getUnderlay(armorStack);
        if (underlay != null) {
            tooltip.add(Text.empty());
            tooltip.add(Text.literal("━━━ Underlay ━━━").formatted(Formatting.DARK_GRAY));

            // Underlay name
            tooltip.add(Text.literal(underlay.getType().getDisplayName())
                    .formatted(Formatting.AQUA));

            // Protection value with color coding
            double protection = underlay.getProtectionValue();
            Formatting protColor = protection > 0 ? Formatting.AQUA : Formatting.GOLD;
            String protType = protection > 0 ? "Cold Protection" : "Heat Protection";
            tooltip.add(Text.literal(protType + ": ").formatted(Formatting.GRAY)
                    .append(Text.literal(String.format("%.1f°C", Math.abs(protection)))
                            .formatted(protColor)));

            // Durability bar
            int durabilityPercent = (int)(underlay.getDurabilityPercent() * 100);
            Formatting durColor = durabilityPercent > 50 ? Formatting.GREEN :
                    durabilityPercent > 25 ? Formatting.YELLOW : Formatting.RED;

            tooltip.add(Text.literal("Durability: ").formatted(Formatting.GRAY)
                    .append(Text.literal(underlay.getDurability() + "/" + underlay.getMaxDurability())
                            .formatted(durColor)));

            if (underlay.isBroken()) {
                tooltip.add(Text.literal("BROKEN - No protection!").formatted(Formatting.RED, Formatting.BOLD));
            }

            tooltip.add(Text.literal("Shift + Click to remove").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
    }
}