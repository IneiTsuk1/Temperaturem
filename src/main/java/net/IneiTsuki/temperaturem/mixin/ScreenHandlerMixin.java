package net.IneiTsuki.temperaturem.mixin;

import net.IneiTsuki.temperaturem.equipment.ArmorUnderlayComponent;
import net.IneiTsuki.temperaturem.equipment.UnderlayApplicationHandler;
import net.IneiTsuki.temperaturem.equipment.UnderlayItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    @Inject(
            method = "onSlotClick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType,
                             PlayerEntity player, CallbackInfo ci) {

        ScreenHandler handler = (ScreenHandler) (Object) this;

        // Check if clicking with a valid slot index
        if (slotIndex < 0 || slotIndex >= handler.slots.size()) {
            return;
        }

        ItemStack cursorStack = handler.getCursorStack();
        ItemStack slotStack = handler.slots.get(slotIndex).getStack();

        // Handle shift+left-click on armor with underlay to remove it
        if (actionType == SlotActionType.QUICK_MOVE && button == 1) {
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof ArmorItem) {
                if (ArmorUnderlayComponent.hasUnderlay(slotStack)) {
                    ItemStack removed = UnderlayApplicationHandler.tryRemoveUnderlay(player, slotStack);
                    if (!removed.isEmpty()) {
                        // Give back to player
                        if (!player.giveItemStack(removed)) {
                            player.dropItem(removed, false);
                        }
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        // Handle regular right-click on armor with underlay while sneaking
        if (actionType == SlotActionType.PICKUP && button == 1 && player.isSneaking()) {
            if (!slotStack.isEmpty() && slotStack.getItem() instanceof ArmorItem) {
                if (ArmorUnderlayComponent.hasUnderlay(slotStack)) {
                    ItemStack removed = UnderlayApplicationHandler.tryRemoveUnderlay(player, slotStack);
                    if (!removed.isEmpty()) {
                        // Give back to player
                        if (!player.giveItemStack(removed)) {
                            player.dropItem(removed, false);
                        }
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        // Only handle normal left clicks for applying underlays
        if (actionType != SlotActionType.PICKUP || button != 0) {
            return;
        }

        // Try to apply underlay: cursor has underlay item, slot has armor
        if (!cursorStack.isEmpty() && cursorStack.getItem() instanceof UnderlayItem &&
                !slotStack.isEmpty() && slotStack.getItem() instanceof ArmorItem) {

            boolean applied = UnderlayApplicationHandler.tryApplyUnderlay(
                    player, cursorStack, slotStack
            );

            if (applied) {
                ci.cancel(); // Prevent normal item swap
            }
        }

        // Try to apply underlay: cursor has armor, slot has underlay item
        else if (!cursorStack.isEmpty() && cursorStack.getItem() instanceof ArmorItem &&
                !slotStack.isEmpty() && slotStack.getItem() instanceof UnderlayItem) {

            boolean applied = UnderlayApplicationHandler.tryApplyUnderlay(
                    player, slotStack, cursorStack
            );

            if (applied) {
                ci.cancel(); // Prevent normal item swap
            }
        }
    }
}