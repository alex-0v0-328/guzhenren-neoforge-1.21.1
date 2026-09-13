package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unknown.guzhenren.attachment.service.path.PathTimeFlowService;
import com.unknown.guzhenren.item.GuItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PathTimeFlowServiceTest {

    /** Every rate a player can reach from the two Watch Gu effects. */
    private static final int[] RATES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13};
    @Test
    @DisplayName("an ordinary clock changes nothing at all")
    void ordinaryClockIsIdentity() {
        assertEquals(20, PathTimeFlowService.waited(PathTimeFlowService.NORMAL_RATE, 20));
        assertEquals(7L, PathTimeFlowService.perStep(PathTimeFlowService.NORMAL_RATE, 7L));
    }
    @Test
    @DisplayName("the use ladder shortens but never reaches zero")
    void theUseLadderShortens() {
        assertEquals(2, PathTimeFlowService.waited(2, GuItem.USE_FAST_TICKS));
        assertEquals(5, PathTimeFlowService.waited(2, GuItem.USE_SAME_TICKS));
        assertEquals(Ticks.HALF_SECOND, PathTimeFlowService.waited(2, GuItem.USE_SLOW_TICKS));

        assertEquals(1, PathTimeFlowService.waited(3, GuItem.USE_FAST_TICKS));
        assertEquals(3, PathTimeFlowService.waited(3, GuItem.USE_SAME_TICKS));
        assertEquals(6, PathTimeFlowService.waited(3, GuItem.USE_SLOW_TICKS));
    }
    @Test
    @DisplayName("a wait shorter than the rate floors at one tick, never at none")
    void aShortWaitNeverBecomesNoWait() {
        for (int rate : RATES) {
            assertEquals(1, PathTimeFlowService.waited(rate, 1), "rate " + rate);
            assertTrue(PathTimeFlowService.waited(rate, GuItem.COOLDOWN_TICKS) >= 1, "rate " + rate);
        }
        assertEquals(0, PathTimeFlowService.waited(3, 0));
    }
    @Test
    @DisplayName("what a step pays out scales with the rate and nothing else")
    void aStepScalesWithTheRate() {
        for (int rate : RATES) {
            assertEquals(100L * rate, PathTimeFlowService.perStep(rate, 100L), "rate " + rate);
            assertEquals(0.5D * rate, PathTimeFlowService.perStep(rate, 0.5D), 0.0D, "rate " + rate);
        }
        assertEquals(Long.MAX_VALUE, PathTimeFlowService.perStep(2, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, PathTimeFlowService.perStep(13, Long.MAX_VALUE / 2));
    }
}
