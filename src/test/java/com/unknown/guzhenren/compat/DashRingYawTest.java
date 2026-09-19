package com.unknown.guzhenren.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * L2: the dash ring rides the actual motion direction, not the Epic Fight model yaw the payload
 * carries. The client bakes the strafe/diagonal angles (±45°/±90°) into that yaw already
 * ({@code ClientEvents}), so the only correction left is the backward dodge animation moving
 * opposite to the model facing -- which is why the offset takes no horizontal input: adding 90°
 * per diagonal would double-count what the model yaw already holds. Not L1: loading
 * {@link EpicFightIntegration} verifies its Epic Fight field types, which only the modded test
 * runtime carries.
 */
public class DashRingYawTest {

    @Test
    public void forwardAndSidewaysRideTheModelYaw() {
        assertEquals(0.0F, EpicFightIntegration.ringYawOffset(1), "forward dash: model yaw is the motion");
        assertEquals(0.0F, EpicFightIntegration.ringYawOffset(0), "sideways dash: model yaw is the motion");
    }

    @Test
    public void everyBackwardCombinationFlips180() {
        assertEquals(180.0F, EpicFightIntegration.ringYawOffset(-1),
                "backward animation moves opposite to the model facing, diagonals included");
    }
}
