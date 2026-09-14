package com.unknown.guzhenren;

import com.mojang.logging.LogUtils;
import com.unknown.guzhenren.compat.EpicFightIntegration;
import com.unknown.guzhenren.registry.advancement.ModCriteriaTriggers;
import com.unknown.guzhenren.registry.attachment.ModAttachments;
import com.unknown.guzhenren.registry.block.ModBlocks;
import com.unknown.guzhenren.registry.effect.ModEffects;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import com.unknown.guzhenren.registry.fluid.ModFluidTypes;
import com.unknown.guzhenren.registry.fluid.ModFluids;
import com.unknown.guzhenren.registry.item.ModCreativeTabs;
import com.unknown.guzhenren.registry.item.ModDataComponents;
import com.unknown.guzhenren.registry.item.ModItems;
import com.unknown.guzhenren.registry.menu.ModMenus;
import com.unknown.guzhenren.registry.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Mod entry point: builds every registry holder and hands them to the mod event bus.
 *
 * <p>Holds the {@code MOD_ID} constant and the {@link #id} helper used across the codebase for
 * {@link ResourceLocation} creation. The constructor wires twelve {@code DeferredRegister} holders
 * (attachments, data components, effects, fluid types, fluids, blocks, entities, items, creative
 * tabs, menus, recipes, criterion triggers) to the mod event bus in the order NeoForge requires.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod(Guzhenren.MOD_ID)
public class Guzhenren {

    public static final String MOD_ID = "guzhenren";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);}
    public Guzhenren(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFluidTypes.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModCriteriaTriggers.register(modEventBus);
        modEventBus.addListener(EpicFightIntegration::onAnimationRegistry);
        EpicFightIntegration.initialize();
    }
}
