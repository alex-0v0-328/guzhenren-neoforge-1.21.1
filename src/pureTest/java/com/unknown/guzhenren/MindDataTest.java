package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unknown.guzhenren.attachment.data.mind.MindData;
import com.unknown.guzhenren.attachment.data.mind.MindPool;
import com.unknown.guzhenren.custom.enums.wisdom.Brilliance;
import com.unknown.guzhenren.custom.enums.wisdom.ThoughtTag;
import com.unknown.guzhenren.custom.enums.wisdom.WisdomType;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MindDataTest {

    private static MindData mind(long current, long evil) {
        return new MindData(Brilliance.ORDINARY,
                Map.of(WisdomType.THOUGHTS, new MindPool(current, 50_000L, false)),
                evil > 0L ? Map.of(ThoughtTag.EVIL, evil) : Map.of());
    }
    private static long sumTagged(MindData d) {
        return d.taggedThoughts().values().stream().mapToLong(Long::longValue).sum();
    }
    @Test
    void extremeCapDoesNotOverflowBurstThreshold() {
        MindPool pool = new MindPool(Long.MAX_VALUE, Long.MAX_VALUE, false);
        assertEquals(Long.MAX_VALUE, pool.burstAt());
        assertFalse(pool.isOverflowing());
    }
    @Test
    void largeTaggedThoughtsScaleWithoutMultiplicationOverflow() {
        assertEquals(Long.MAX_VALUE / 2, mind(Long.MAX_VALUE / 2, Long.MAX_VALUE)
                .taggedThoughts().get(ThoughtTag.EVIL));
    }
    @Test
    void malformedNegativeTagsAreRemoved() {
        MindData data = new MindData(Brilliance.ORDINARY, Map.of(), Map.of(ThoughtTag.EVIL, -1L));
        assertTrue(data.taggedThoughts().isEmpty());
    }
    @Test
    @DisplayName("tagged thoughts clamp to the pool's current -- the sum never exceeds it")
    void taggedClampedToCurrent() {
        MindData d = mind(30, 50);
        assertEquals(30L, d.taggedThoughts().get(ThoughtTag.EVIL));
        assertEquals(30L, sumTagged(d));
    }
    @Test
    @DisplayName("natural thoughts are derived: current minus the tagged sum")
    void naturalIsDerived() {
        MindData d = mind(100, 30);
        assertEquals(70L, d.pool(WisdomType.THOUGHTS).current() - sumTagged(d));
    }
    @Test
    @DisplayName("an empty mind holds no tagged thoughts -- they clamp to zero and drop out")
    void emptyMindDropsTagged() {
        assertTrue(mind(0, 50).taggedThoughts().isEmpty());
    }
    @Test
    @DisplayName("NATURAL is derived, never stored -- the ctor drops it")
    void naturalNeverStored() {
        MindData d = new MindData(Brilliance.ORDINARY,
                Map.of(WisdomType.THOUGHTS, new MindPool(100, 50_000L, false)),
                Map.of(ThoughtTag.NATURAL, 999L));
        assertFalse(d.taggedThoughts().containsKey(ThoughtTag.NATURAL));
        assertTrue(d.taggedThoughts().isEmpty());
    }
    @Test
    @DisplayName("withTagged removes a tag when the amount falls to zero")
    void withTaggedRemovesZero() {
        MindData d = mind(100, 30).withTagged(ThoughtTag.EVIL, 0);
        assertFalse(d.taggedThoughts().containsKey(ThoughtTag.EVIL));
    }
    @Test
    @DisplayName("emptied clears the tagged thoughts alongside the pools")
    void emptiedClearsTagged() {
        MindData d = mind(100, 30).emptied();
        assertTrue(d.taggedThoughts().isEmpty());
        assertEquals(0L, d.pool(WisdomType.THOUGHTS).current());
    }
}
