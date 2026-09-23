package com.unknown.guzhenren.world;

import com.unknown.guzhenren.registry.block.ModBlocks;
import com.unknown.guzhenren.registry.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * The Spirit Spring [元泉] surface structure: a 7x7 basin laid out as three layers around the
 * center column's ground height g ({@code MOTION_BLOCKING_NO_LEAVES} - 1).
 *
 * <p>Layer one at g-1 buries the calcite basin; layer two at g paves the ground with slabs and
 * raises the calcite pillar under the spring; layer three at g+1 is only the source block. The
 * layer-two ring around the pillar is cleared to air ({@code a}), so the four streams fall into
 * the calcite basin and the rim holds the water instead of letting it sheet outward (Alex,
 * 2026-09-23). Outer {@code x} cells keep the original terrain (dirt stays dirt).
 *
 * <p>Vegetation runs before us ({@code VEGETAL_DECORATION} precedes {@code TOP_LAYER_MODIFICATION}),
 * so above every non-{@code x} column the two cells over the ground are cleared to air -- the
 * grass that anchored to the replaced dirt must not hover over the stones (Alex, 2026-09-23; same
 * philosophy as vanilla lakes clearing their surface). Placement happens on flat land only (Alex,
 * 2026-09-24): any structure column whose own surface height differs from the center's vetoes the
 * spot, so bumps never leave plants floating above the carve, and stalk plants that dodge the
 * heightmap (bamboo, sugar cane) veto from the clear band.
 *
 * <p>⚠ Layout and rarity are Alex's picks (2026-09-23), not silent tunables. Symbol legend:
 * {@code x}=keep, {@code a}=air (the basin well), {@code y}=cobblestone, {@code t}=mossy
 * cobblestone, {@code f}=calcite, {@code c}=cobblestone slab (bottom), {@code m}=mossy
 * cobblestone slab (bottom).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
public class SpiritSpringFeature extends Feature<NoneFeatureConfiguration> {

    static final String[] LAYER_ONE = {
            "xxtttxx",
            "xtffftx",
            "tfffffy",
            "yfffffy",
            "yffffft",
            "xtfffyx",
            "xxytyxx"};
    static final String[] LAYER_TWO = {
            "xxyytxx",
            "xtmcmyx",
            "ycaaamy",
            "tmafact",
            "ymaaacy",
            "xyccmyx",
            "xxttyxx"};
    private static final int RADIUS = 3;
    /** Cells above ground cleared over every non-x column: covers double plants and snow layers. */
    private static final int CLEAR_HEIGHT_ABOVE = 2;

    public SpiritSpringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }
    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                origin.getX(), origin.getZ()) - 1;
        BlockPos groundCenter = new BlockPos(origin.getX(), groundY, origin.getZ());
        BlockState ground = level.getBlockState(groundCenter);
        if (ground.is(BlockTags.LOGS) || ground.is(BlockTags.LEAVES)) return false;
        if (!level.getFluidState(groundCenter).isEmpty()
                || !level.getFluidState(groundCenter.above()).isEmpty()) return false;
        if (unevenTerrain(level, groundCenter)) return false;
        placeStructure(level, groundCenter);
        return true;
    }
    /**
     * Flat-land rule (Alex, 2026-09-24): any structure column whose own surface height differs
     * from the center's vetoes the whole spot -- slopes, ponds and trees all move the heightmap,
     * so clearing above our stones can never leave plants floating on a carved bump. Stalk plants
     * that dodge the heightmap (no collision: bamboo, sugar cane) veto from the clear band.
     */
    private static boolean unevenTerrain(WorldGenLevel level, BlockPos groundCenter) {
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) == 'x') continue;
                int x = groundCenter.getX() + column - RADIUS;
                int z = groundCenter.getZ() + row - RADIUS;
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1
                        != groundCenter.getY()) return true;
                for (int dy = 1; dy <= CLEAR_HEIGHT_ABOVE; dy++) {
                    BlockState above = level.getBlockState(
                            groundCenter.offset(column - RADIUS, dy, row - RADIUS));
                    if (above.getBlock() instanceof BambooStalkBlock
                            || above.getBlock() instanceof SugarCaneBlock) return true;
                }
            }
        }
        return false;
    }
    /**
     * Lays the two grid layers and the spring source around {@code groundCenter} (the ground
     * block at layer two's level), clearing the two cells above ground over every non-x column
     * first. Exposed for GameTests, which run on a {@code ServerLevel}.
     */
    public static void placeStructure(LevelAccessor level, BlockPos groundCenter) {
        placeLayer(level, groundCenter.below(), LAYER_ONE);
        placeLayer(level, groundCenter, LAYER_TWO);
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) == 'x') continue;
                for (int dy = 1; dy <= CLEAR_HEIGHT_ABOVE; dy++) {
                    level.setBlock(groundCenter.offset(column - RADIUS, dy, row - RADIUS),
                            Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos spring = groundCenter.above();
        level.setBlock(spring, ModBlocks.SPIRIT_SPRING.get().defaultBlockState(), 2);
        level.scheduleTick(spring, ModFluids.SPIRIT_SPRING.get(), 0);
    }
    private static void placeLayer(LevelAccessor level, BlockPos layerCenter, String[] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length(); column++) {
                BlockState state = stateFor(grid[row].charAt(column));
                if (state == null) continue;
                level.setBlock(layerCenter.offset(column - RADIUS, 0, row - RADIUS), state, 2);
            }
        }
    }
    static @Nullable BlockState stateFor(char symbol) {
        return switch (symbol) {
            case 'y' -> Blocks.COBBLESTONE.defaultBlockState();
            case 't' -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 'f' -> Blocks.CALCITE.defaultBlockState();
            case 'c' -> Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case 'm' -> Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case 'a' -> Blocks.AIR.defaultBlockState();
            default -> null;
        };
    }
}
