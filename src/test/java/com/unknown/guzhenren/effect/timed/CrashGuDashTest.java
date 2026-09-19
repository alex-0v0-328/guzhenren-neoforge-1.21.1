package com.unknown.guzhenren.effect.timed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unknown.guzhenren.Ticks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CrashGuDashTest {

    @Test
    @DisplayName("dash distance stays 4.5x the Epic Fight dodge vector (whole family, one tier)")
    void dashCoordScaleIsPinned() {
        assertEquals(4.5D, CrashGuEffect.DASH_COORD_SCALE);
    }
    @Test
    @DisplayName("the shared duration helper still converts seconds to ticks")
    void durationHelperIsPinned() {
        assertEquals(30 * Ticks.SECOND, CrashGuEffect.duration(30));
    }
}
