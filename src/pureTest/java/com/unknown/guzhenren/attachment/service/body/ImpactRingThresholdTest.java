package com.unknown.guzhenren.attachment.service.body;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImpactRingThresholdTest {

    @Test
    @DisplayName("no ring at zero and under the threshold")
    void belowThresholdShowsNothing() {
        assertFalse(BodyAttackService.showsImpactRing(0.0D));
        assertFalse(BodyAttackService.showsImpactRing(1.0D));
        assertFalse(BodyAttackService.showsImpactRing(15.999D));
    }
    @Test
    @DisplayName("the threshold itself triggers -- it is inclusive")
    void thresholdIsInclusive() {
        assertTrue(BodyAttackService.showsImpactRing(BodyAttackService.IMPACT_RING_ATTACK_THRESHOLD));
        assertTrue(BodyAttackService.showsImpactRing(16.0D));
    }
    @Test
    @DisplayName("heavy panels keep the ring, however heavy")
    void aboveThresholdShowsTheRing() {
        assertTrue(BodyAttackService.showsImpactRing(16.001D));
        assertTrue(BodyAttackService.showsImpactRing(45_000.0D));
        assertTrue(BodyAttackService.showsImpactRing(Double.MAX_VALUE));
    }
}
