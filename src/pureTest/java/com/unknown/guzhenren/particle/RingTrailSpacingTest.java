package com.unknown.guzhenren.particle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** L1: the even-spacing accumulator behind the planted dash trail. */
class RingTrailSpacingTest {

    private static final double EPS = 1.0E-9;

    @Test
    void belowThresholdOnlyCarries() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.drops(0.0D, 1.9D, 7);
        assertEquals(0, drops.offsets().length);
        assertEquals(1.9D, drops.carry(), EPS);
        assertEquals(7, drops.ringsLeft());
    }

    @Test
    void crossingDropsAtTheThreshold() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.drops(0.0D, 2.0D, 7);
        assertArrayEquals(new double[] {2.0D}, drops.offsets(), EPS);
        assertEquals(0.0D, drops.carry(), EPS);
        assertEquals(6, drops.ringsLeft());
    }

    @Test
    void carryRollsAcrossTicks() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.drops(1.9D, 0.2D, 7);
        assertArrayEquals(new double[] {0.1D}, drops.offsets(), EPS);
        assertEquals(0.1D, drops.carry(), EPS);
        assertEquals(6, drops.ringsLeft());
    }

    @Test
    void aJumpSpreadsEvenlyAlongTheSegment() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.drops(0.0D, 6.5D, 7);
        assertArrayEquals(new double[] {2.0D, 4.0D, 6.0D}, drops.offsets(), EPS);
        assertEquals(0.5D, drops.carry(), EPS);
        assertEquals(4, drops.ringsLeft());
    }

    @Test
    void theRingBudgetCapsTheDrops() {
        RingTrailSpacing.Drops drops = RingTrailSpacing.drops(0.0D, 100.0D, 2);
        assertArrayEquals(new double[] {2.0D, 4.0D}, drops.offsets(), EPS);
        assertEquals(0, drops.ringsLeft());
    }
}
