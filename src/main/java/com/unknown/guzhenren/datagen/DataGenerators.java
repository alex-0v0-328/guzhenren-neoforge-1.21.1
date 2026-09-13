package com.unknown.guzhenren.datagen;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.datagen.advancement.ModAdvancementProvider;
import com.unknown.guzhenren.datagen.curios.ModCuriosProvider;
import com.unknown.guzhenren.datagen.damage.ModDamageTypeTagsProvider;
import com.unknown.guzhenren.datagen.item.ModItemModelProvider;
import com.unknown.guzhenren.datagen.item.ModItemTagsProvider;
import com.unknown.guzhenren.datagen.lang.EnUsLanguageProvider;
import com.unknown.guzhenren.datagen.lang.ZhCnLanguageProvider;
import com.unknown.guzhenren.datagen.loot.WildBoarLootProvider;
import com.unknown.guzhenren.datagen.recipe.ModRecipeProvider;
import com.unknown.guzhenren.datagen.world.ModBiomeTagsProvider;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Wires every generator that runs at datagen time.
 *
 * <p>Annotated {@code @EventBusSubscriber}. On {@code GatherDataEvent} it adds every provider that
 * lives under {@code datagen/}. The output is a committed source set under
 * {@code src/generated/resources}.
 *
 * <p>⚠ What they write is a committed source set, so a provider changed without regenerating ships a
 * stale jar while the build stays perfectly green.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class DataGenerators {

    private DataGenerators() {}
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new EnUsLanguageProvider(packOutput));
        generator.addProvider(event.includeClient(), new ZhCnLanguageProvider(packOutput));

        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));

        ModDatapackProvider datapackProvider = generator.addProvider(event.includeServer(),
                new ModDatapackProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeServer(), new ModDamageTypeTagsProvider(
                packOutput, datapackProvider.getRegistryProvider(), existingFileHelper));

        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(packOutput, lookupProvider, existingFileHelper));

        generator.addProvider(event.includeServer(),
                new ModCuriosProvider(packOutput, existingFileHelper, lookupProvider));

        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(WildBoarLootProvider::new, LootContextParamSets.ENTITY)),
                lookupProvider));

        generator.addProvider(event.includeServer(),
                new ModAdvancementProvider(packOutput, lookupProvider, existingFileHelper));

        generator.addProvider(event.includeServer(),
                new ModBiomeTagsProvider(packOutput, datapackProvider.getRegistryProvider(), existingFileHelper));
    }
}
