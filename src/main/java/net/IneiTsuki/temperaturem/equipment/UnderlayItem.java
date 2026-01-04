package net.IneiTsuki.temperaturem.equipment;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

import static net.IneiTsuki.temperaturem.equipment.UnderlayApplicationHandler.tryRemoveUnderlay;

public class UnderlayItem extends Item {

    private final TemperatureUnderlay.UnderlayType underlayType;

    public UnderlayItem(Settings settings, TemperatureUnderlay.UnderlayType type) {
        super(settings.maxCount(16)); // Stackable up to 16
        this.underlayType = type;
    }

    public TemperatureUnderlay.UnderlayType getUnderlayType() {
        return underlayType;
    }

    public TemperatureUnderlay createUnderlay() {
        return new TemperatureUnderlay(underlayType);
    }

    public static TemperatureUnderlay createUnderlayFromStack(ItemStack stack) {
        if (!(stack.getItem() instanceof UnderlayItem item)) {
            return null;
        }

        TemperatureUnderlay underlay = new TemperatureUnderlay(item.getUnderlayType());

        // Check for stored durability
        if (stack.hasNbt() && stack.getNbt().contains("StoredDurability")) {
            int storedDurability = stack.getNbt().getInt("StoredDurability");
            underlay.setDurability(storedDurability);
        }

        return underlay;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            user.sendMessage(Text.literal("Right-click on armor in your inventory to apply this underlay."), false);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    public static boolean handleArmorClick(PlayerEntity player, ItemStack armorStack) {
        if (player.isSneaking() && ArmorUnderlayComponent.hasUnderlay(armorStack)) {
            return tryRemoveUnderlay(player, armorStack) != ItemStack.EMPTY;
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        // Description
        tooltip.add(Text.literal(underlayType.getDescription()).formatted(Formatting.GRAY));
        tooltip.add(Text.empty());

        // Protection value
        double protection = underlayType.getBaseProtection();
        if (protection > 0) {
            tooltip.add(Text.literal("Cold Protection: ").formatted(Formatting.AQUA)
                    .append(Text.literal(String.format("+%.0f°C", protection)).formatted(Formatting.WHITE)));
        } else {
            tooltip.add(Text.literal("Heat Protection: ").formatted(Formatting.GOLD)
                    .append(Text.literal(String.format("%.0f°C", protection)).formatted(Formatting.WHITE)));
        }

        // Durability
        tooltip.add(Text.literal("Durability: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(underlayType.getMaxDurability())).formatted(Formatting.WHITE)));

        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Apply to any armor piece").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        // Add enchantment glint to high-tier underlays
        return underlayType == TemperatureUnderlay.UnderlayType.THERMAL_PADDING ||
                underlayType == TemperatureUnderlay.UnderlayType.CLIMATE_WEAVE;
    }
}