package hrcaoc.bigitemdisplay;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;


public class CustomItems {
    public static final Item BIG_ITEM_DISPLAY = register(
            "big_item_display",
            new BigItemDisplayItem(CustomEntityType.BIG_ITEM_DISPLAY, new Item.Settings())
    );

    public static Item register(String id, Item item) {
        // Create the identifier for the item (2nd param), register the item and return
        return Registry.register(Registries.ITEM, Identifier.of(BigItemDisplay.MOD_ID, id), item);
    }

    public static void initialize() {
        // Get the event for modifying entries in item groups.
        // And register an event handler that adds custom items to item groups.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(itemGroup -> itemGroup.addAfter(Items.GLOW_ITEM_FRAME, BIG_ITEM_DISPLAY));
    }

}
