package com.unknown.guzhenren.registry.item;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.block.SpiritSpringBlock;
import com.unknown.guzhenren.custom.enums.path.GuPath;
import com.unknown.guzhenren.item.gu.MortalGuItem;
import com.unknown.guzhenren.item.material.GuMaterialItem;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import static com.unknown.guzhenren.custom.enums.path.GuPath.STRENGTH;

/**
 * The creative tabs, filled by dispatching on the item's class and path.
 *
 * <p>DeferredRegister holder: three tabs ({@code mortal_gu}, {@code gu_material},
 * {@code strength_mortal_gu}), populated by predicates over {@link MortalGuItem},
 * {@link GuMaterialItem}, and {@link GuPath#STRENGTH}. An item extending neither middle class lands in
 * no tab at all -- except the Spirit Spring [元泉] BlockItem, routed into the material tab
 * explicitly.
 *
 * <p>⚠ That miss is silent: nothing fails and nothing warns, the item simply never appears. The three
 * tab constants stay unused by the language provider (it has no creative-tab overload).
 *
 * @author Alex
 * @version 1.0.0
 * @see ModItems
 * @since 1.0.0
 */

public final class ModCreativeTabs {

    private ModCreativeTabs() {}
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Guzhenren.MOD_ID);
    private static final ResourceLocation EPIC_FIGHT_ITEMS =
            ResourceLocation.fromNamespaceAndPath("epicfight", "items");
    public static final Supplier<CreativeModeTab> MORTAL_GU = CREATIVE_MODE_TABS.register("mortal_gu",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guzhenren.mortal_gu"))
                    .icon(() -> new ItemStack(ModItems.HOPE_GU.get()))
                    .withTabsBefore(Guzhenren.id("gu_material"))
                    .displayItems((parameters, output) -> accept(output, ModCreativeTabs::belongsInMortalGu))
                    .build());
    public static final Supplier<CreativeModeTab> GU_MATERIAL = CREATIVE_MODE_TABS.register("gu_material",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guzhenren.gu_material"))
                    .icon(() -> new ItemStack(ModItems.PRIMEVAL_STONE.get()))
                    .withTabsBefore(EPIC_FIGHT_ITEMS)
                    .displayItems((parameters, output) -> accept(output, ModCreativeTabs::belongsInGuMaterial))
                    .build());
    public static final Supplier<CreativeModeTab> STRENGTH_MORTAL_GU = CREATIVE_MODE_TABS.register(
            "strength_mortal_gu", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guzhenren.strength_mortal_gu"))
                    .icon(() -> new ItemStack(ModItems.FLOWER_BOAR_GU.get()))
                    .withTabsBefore(Guzhenren.id("mortal_gu"))
                    .displayItems((parameters, output) -> accept(output,
                            item -> item instanceof MortalGuItem gu && gu.path() == STRENGTH))
                    .build());
    private static void accept(CreativeModeTab.Output output, Predicate<Item> accepted) {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (accepted.test(item)) output.accept(item);
        }
    }
    static boolean belongsInMortalGu(Item item) {return item instanceof MortalGuItem gu && gu.path() != STRENGTH;}
    static boolean belongsInGuMaterial(Item item) {
        return GuMaterialItem.class.isInstance(item)
                || item instanceof BlockItem blockItem && blockItem.getBlock() instanceof SpiritSpringBlock;
    }
    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
