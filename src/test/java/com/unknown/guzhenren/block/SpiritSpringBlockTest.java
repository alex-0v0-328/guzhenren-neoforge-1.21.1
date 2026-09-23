package com.unknown.guzhenren.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure pins for the Spirit Spring [元泉] production decision: the 128-block player gate (Alex,
 * 2026-09-23) and the nearby-stones cap. Lives in L2 -- loading the block class pulls in
 * {@code LiquidBlock}'s static game-event registration, which pureTest's un-bootstrapped JVM
 * rejects (reference §3).
 */
class SpiritSpringBlockTest {

    @Test
    @DisplayName("no player within the production horizon, no stones")
    void noPlayerNoProduction() {
        assertFalse(SpiritSpringBlock.shouldProduce(false, 0));
    }
    @Test
    @DisplayName("player near and pool empty, produce")
    void playerNearProduces() {
        assertTrue(SpiritSpringBlock.shouldProduce(true, 0));
    }
    @Test
    @DisplayName("player near but the cap radius already holds a full stack, pause")
    void capPausesProduction() {
        assertFalse(SpiritSpringBlock.shouldProduce(true, SpiritSpringBlock.NEARBY_STONES_CAP));
    }
    @Test
    @DisplayName("one stone below the cap still produces")
    void belowCapProduces() {
        assertTrue(SpiritSpringBlock.shouldProduce(true, SpiritSpringBlock.NEARBY_STONES_CAP - 1));
    }
}
