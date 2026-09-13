package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unknown.guzhenren.attachment.data.body.BodyData;
import com.unknown.guzhenren.custom.enums.body.ExtremePhysique;
import com.unknown.guzhenren.custom.enums.body.Physique;
import com.unknown.guzhenren.custom.enums.body.Race;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BodyDataTest {

    @Test
    void yearConversionAndAgingDoNotWrapAtLongLimits() {
        assertEquals(Long.MAX_VALUE, BodyData.parts(Long.MAX_VALUE));
        assertEquals(Long.MIN_VALUE, BodyData.parts(Long.MIN_VALUE));
        BodyData body = BodyData.DEFAULT.withAgeParts(Long.MAX_VALUE).withLifespanParts(Long.MIN_VALUE);
        BodyData lived = body.lived(1L, 1L);
        assertEquals(Long.MAX_VALUE, lived.ageParts());
        assertEquals(Long.MIN_VALUE, lived.lifespanParts());
    }
    @Test
    @DisplayName("physiques accumulate while zombie forms remain mutually exclusive")
    void physiquesAccumulateWithExclusiveZombieForms() {
        BodyData body = new BodyData(EnumSet.of(Physique.ZOMBIE, Physique.HALF_ZOMBIE, Physique.EXTREME),
                ExtremePhysique.VERDANT_GREAT_SUN, Race.HUMAN, 1L, 2L, 3L, 4L, 5L, 6, 7L);

        assertTrue(body.hasPhysique(Physique.ZOMBIE));
        assertFalse(body.hasPhysique(Physique.HALF_ZOMBIE));
        assertTrue(body.hasPhysique(Physique.EXTREME));
        assertEquals(ExtremePhysique.VERDANT_GREAT_SUN, body.extremePhysique());
        assertEquals(6, body.zombieTier());
        assertEquals(5L, body.halfZombieEndTick());
    }
    @Test
    @DisplayName("revival clears zombie physiques and anchors but keeps Extreme")
    void revivalClearsZombieState() {
        BodyData body = new BodyData(EnumSet.of(Physique.ZOMBIE, Physique.EXTREME),
                ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, Race.HUMAN, 1L, 2L, 3L, 4L, 5L, 6, 7L);

        BodyData revived = body.revived();

        assertFalse(revived.hasPhysique(Physique.ZOMBIE));
        assertFalse(revived.hasPhysique(Physique.HALF_ZOMBIE));
        assertTrue(revived.hasPhysique(Physique.EXTREME));
        assertEquals(ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, revived.extremePhysique());
        assertEquals(BodyData.UNTRACKED, revived.halfZombieEndTick());
        assertEquals(BodyData.NO_ZOMBIE_TIER, revived.zombieTier());
    }
}
