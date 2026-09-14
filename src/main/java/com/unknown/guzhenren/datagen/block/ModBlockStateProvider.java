package com.unknown.guzhenren.datagen.block;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.registry.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the blockstates and models for the mod's blocks. A fluid block only needs the particle
 * model -- the liquid surface itself is drawn by the fluid renderer from the FluidType textures
 * (see {@link com.unknown.guzhenren.client.fluid.SpiritSpringClientExtensions}) -- so each entry
 * matches vanilla's {@code block/water} shape: a blockstate variant pointing at a model whose
 * single texture is the still strip.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Guzhenren.MOD_ID, existingFileHelper);
    }
    @Override
    protected void registerStatesAndModels() {
        fluidBlock(ModBlocks.SPIRIT_SPRING.get(), Guzhenren.id("block/spirit_spring_still"));
    }
    private void fluidBlock(Block block, ResourceLocation particle) {
        simpleBlock(block, models().getBuilder(BuiltInRegistries.BLOCK.getKey(block).getPath())
                .texture("particle", particle));
    }
}
