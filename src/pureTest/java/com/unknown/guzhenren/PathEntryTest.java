package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.unknown.guzhenren.attachment.data.path.PathEntry;
import com.unknown.guzhenren.custom.enums.path.GuAttainment;
import com.unknown.guzhenren.custom.enums.path.MarkTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PathEntryTest {

    @Test
    void totalMarksSaturateInsteadOfWrappingNegative() {
        PathEntry entry = PathEntry.DEFAULT.withMark(MarkTag.NATURAL, Long.MAX_VALUE)
                .withMark(MarkTag.RACE, 10L);
        assertEquals(Long.MAX_VALUE, entry.markTotal());
    }
    @Test
    @DisplayName("the removed legacy field is discarded while attainment and Dao marks survive migration")
    void removedLegacyFieldIsDiscarded() {
        JsonObject legacy = JsonParser.parseString("""
                {"attainment":"grandmaster","marks":{"natural":42,"extreme_physique":7},"specks":{"natural":99}}
                """).getAsJsonObject();

        PathEntry decoded = PathEntry.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();

        assertSame(GuAttainment.GRANDMASTER, decoded.attainment());
        assertEquals(42L, decoded.mark(MarkTag.NATURAL));
        assertEquals(7L, decoded.mark(MarkTag.EXTREME_PHYSIQUE));
        JsonObject migrated = PathEntry.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow().getAsJsonObject();
        assertFalse(migrated.has("specks"));
    }
}
