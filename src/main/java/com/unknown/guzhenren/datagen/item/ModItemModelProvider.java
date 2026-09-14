package com.unknown.guzhenren.datagen.item;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.block.SpiritSpringBlock;
import com.unknown.guzhenren.registry.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes an item model per registered item, dispatching on the item's class.
 *
 * <p>Extends {@link net.neoforged.neoforge.client.model.generators.ItemModelProvider}. Iterates every
 * registered item and calls {@code basicItem} on it, except the Spirit Spring [元泉] BlockItem,
 * whose icon is the fluid's still strip instead of an item PNG of its own.
 *
 * <p>⚠ The texture existence check means a missing PNG fails datagen instead of shipping
 * as a missing-texture item nobody notices until they open the tab.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Guzhenren.MOD_ID, existingFileHelper);
    }
    @Override
    protected void registerModels() {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof SpiritSpringBlock) {
                getBuilder(entry.getId().getPath())
                        .parent(new ModelFile.UncheckedModelFile("minecraft:item/generated"))
                        .texture("layer0", Guzhenren.id("block/spirit_spring_still"));
                continue;
            }
            basicItem(item);
        }
    }
}
