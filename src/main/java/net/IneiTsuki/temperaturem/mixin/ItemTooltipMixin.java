package net.IneiTsuki.temperaturem.mixin;

import net.IneiTsuki.temperaturem.equipment.UnderlayApplicationHandler;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemTooltipMixin {

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void addUnderlayTooltip(ItemStack stack, World world, List<Text> tooltip,
                                    TooltipContext context, CallbackInfo ci) {
        // Only add tooltip if this is armor
        if (stack.getItem() instanceof ArmorItem) {
            UnderlayApplicationHandler.addUnderlayTooltip(stack, tooltip);
        }
    }
}