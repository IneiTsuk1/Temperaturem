package net.IneiTsuki.temperaturem.items;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.IneiTsuki.temperaturem.equipment.TemperatureUnderlay;
import net.IneiTsuki.temperaturem.equipment.UnderlayItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    // Underlay items
    public static final Item WOOL_LINING = register("wool_lining",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.WOOL_LINING));

    public static final Item FUR_LINING = register("fur_lining",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.FUR_LINING));

    public static final Item THERMAL_PADDING = register("thermal_padding",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.THERMAL_PADDING));

    public static final Item LEATHER_LINING = register("leather_lining",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.LEATHER_LINING));

    public static final Item COOLING_MESH = register("cooling_mesh",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.COOLING_MESH));

    public static final Item CLIMATE_WEAVE = register("climate_weave",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.CLIMATE_WEAVE));

    public static final Item INSULATED_FABRIC = register("insulated_fabric",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.INSULATED_FABRIC));

    public static final Item REFLECTIVE_LAYER = register("reflective_layer",
            new UnderlayItem(new FabricItemSettings(), TemperatureUnderlay.UnderlayType.REFLECTIVE_LAYER));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Temperaturem.MOD_ID, name), item);
    }

    public static void initialize() {
        Temperaturem.LOGGER.info("Registering items for " + Temperaturem.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(WOOL_LINING);
            entries.add(FUR_LINING);
            entries.add(THERMAL_PADDING);
            entries.add(LEATHER_LINING);
            entries.add(COOLING_MESH);
            entries.add(CLIMATE_WEAVE);
            entries.add(INSULATED_FABRIC);
            entries.add(REFLECTIVE_LAYER);
        });
    }

}