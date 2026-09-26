package com.unknown.guzhenren.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Spec pins for the Spirit Spring [元泉] structure grids and their symbol mapping.
 *
 * <p>The two 7x7 layouts and the block each symbol stands for are Alex's design (2026-09-23); a
 * broken row length or a remapped block shows up here instead of as a malformed structure in game.
 * Lives in L2 because the mapping resolves registry objects ({@code Blocks.*}).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
class SpiritSpringFeatureTest {

    @Test
    @DisplayName("both structure layers are 7x7 over the known symbol set")
    void gridsAreSevenBySevenWithKnownSymbols() {
        String symbols = "xytfcma";
        for (String[] grid : new String[][]{SpiritSpringFeature.LAYER_ONE, SpiritSpringFeature.LAYER_TWO}) {
            assertEquals(7, grid.length, "a layer must have 7 rows");
            for (String row : grid) {
                assertEquals(7, row.length(), "a row must have 7 columns: " + row);
                for (char symbol : row.toCharArray()) {
                    assertTrue(symbols.indexOf(symbol) >= 0, () -> "unknown symbol: " + symbol);
                }
            }
        }
    }
    @Test
    @DisplayName("layer centers are the calcite pillar under the spring source")
    void centersAreCalcite() {
        assertSame(Blocks.CALCITE.defaultBlockState().getBlock(),
                SpiritSpringFeature.stateFor(SpiritSpringFeature.LAYER_ONE[3].charAt(3)).getBlock());
        assertSame(Blocks.CALCITE.defaultBlockState().getBlock(),
                SpiritSpringFeature.stateFor(SpiritSpringFeature.LAYER_TWO[3].charAt(3)).getBlock());
    }
    @Test
    @DisplayName("symbols map to cobblestone, mossy cobblestone, calcite and bottom slabs")
    void symbolMappingMatchesSpec() {
        assertSame(Blocks.COBBLESTONE.defaultBlockState(), SpiritSpringFeature.stateFor('y'));
        assertSame(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), SpiritSpringFeature.stateFor('t'));
        assertSame(Blocks.CALCITE.defaultBlockState(), SpiritSpringFeature.stateFor('f'));
        BlockState cobbleSlab = SpiritSpringFeature.stateFor('c');
        assertSame(Blocks.COBBLESTONE_SLAB, cobbleSlab.getBlock());
        assertEquals(SlabType.BOTTOM, cobbleSlab.getValue(SlabBlock.TYPE));
        BlockState mossySlab = SpiritSpringFeature.stateFor('m');
        assertSame(Blocks.MOSSY_COBBLESTONE_SLAB, mossySlab.getBlock());
        assertEquals(SlabType.BOTTOM, mossySlab.getValue(SlabBlock.TYPE));
        assertNull(SpiritSpringFeature.stateFor('x'), "x keeps the original terrain");
        assertSame(Blocks.AIR.defaultBlockState(), SpiritSpringFeature.stateFor('a'),
                "the basin well around the pillar is cleared to air");
        assertNull(SpiritSpringFeature.stateFor('?'), "unknown symbols place nothing");
    }
    @Test
    @DisplayName("cluster size weights sum to 100 over sizes 1..5")
    void clusterWeightsSumToHundred() {
        int total = 0;
        for (int weight : SpiritSpringFeature.CLUSTER_SIZE_WEIGHTS) total += weight;
        assertEquals(100, total, "the roll maps one nextInt(100) draw onto one size");
        assertEquals(5, SpiritSpringFeature.CLUSTER_SIZE_WEIGHTS.length, "sizes run 1..5");
    }
    @Test
    @DisplayName("cluster roll boundaries follow 50/20/15/10/5 (Alex, 2026-09-26)")
    void clusterRollBoundaries() {
        assertEquals(1, SpiritSpringFeature.clusterSizeForRoll(0));
        assertEquals(1, SpiritSpringFeature.clusterSizeForRoll(49));
        assertEquals(2, SpiritSpringFeature.clusterSizeForRoll(50));
        assertEquals(2, SpiritSpringFeature.clusterSizeForRoll(69));
        assertEquals(3, SpiritSpringFeature.clusterSizeForRoll(70));
        assertEquals(3, SpiritSpringFeature.clusterSizeForRoll(84));
        assertEquals(4, SpiritSpringFeature.clusterSizeForRoll(85));
        assertEquals(4, SpiritSpringFeature.clusterSizeForRoll(94));
        assertEquals(5, SpiritSpringFeature.clusterSizeForRoll(95));
        assertEquals(5, SpiritSpringFeature.clusterSizeForRoll(99));
    }
    @Test
    @DisplayName("cluster separation keeps the horizontal distance inside 8..16")
    void clusterSeparationRange() {
        assertTrue(SpiritSpringFeature.separationAcceptable(8, 0), "the minimum 8 holds");
        assertFalse(SpiritSpringFeature.separationAcceptable(7, 0), "7 is too close");
        assertTrue(SpiritSpringFeature.separationAcceptable(16, 0), "the maximum 16 holds");
        assertFalse(SpiritSpringFeature.separationAcceptable(17, 0), "17 is too far");
        assertTrue(SpiritSpringFeature.separationAcceptable(6, 6), "diagonal 8.49 inside");
        assertFalse(SpiritSpringFeature.separationAcceptable(5, 5), "diagonal 7.07 outside");
        assertTrue(SpiritSpringFeature.separationAcceptable(11, 11), "diagonal 15.56 inside");
        assertFalse(SpiritSpringFeature.separationAcceptable(12, 12), "diagonal 16.97 outside");
        assertFalse(SpiritSpringFeature.separationAcceptable(0, 0), "two springs never share a spot");
    }
}
