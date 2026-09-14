package com.unknown.guzhenren.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unknown.guzhenren.block.SpiritSpringBlock;
import com.unknown.guzhenren.item.gu.MortalGuItem;
import com.unknown.guzhenren.item.material.GuMaterialItem;
import com.unknown.guzhenren.registry.effect.ModEffects;
import com.unknown.guzhenren.registry.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Registry-count snapshots over the mod's DeferredRegisters, resolved in the booted test runtime.
 *
 * <p>Pins the registered totals so an accidental drop or duplicate shows up as a count break, and
 * asserts every item is a Gu ({@code MortalGuItem}), a Gu material ({@code GuMaterialItem}) or a
 * fluid BlockItem -- the families every registration in ModItems is built from. Qi materials
 * arrive as {@code GuMaterialItem} subclasses, so one check covers them.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class RegistrySnapshotTest {

    @Test
    @DisplayName("ModItems registers exactly 103 items")
    void itemCountIsPinned() {
        assertEquals(103, ModItems.ITEMS.getEntries().size());
    }
    @Test
    @DisplayName("every registered item is a mortal Gu, a Gu material or the spring block")
    void itemClassesArePinned() {
        for (Holder<Item> holder : ModItems.ITEMS.getEntries()) {
            Item item = holder.value();
            assertTrue(item instanceof MortalGuItem || item instanceof GuMaterialItem
                            || item instanceof BlockItem blockItem
                            && blockItem.getBlock() instanceof SpiritSpringBlock,
                    () -> "unexpected item class: " + item);
        }
    }
    @Test
    @DisplayName("ModEffects registers exactly 20 effects")
    void effectCountIsPinned() {
        assertEquals(20, ModEffects.MOB_EFFECTS.getEntries().size());
    }
}
