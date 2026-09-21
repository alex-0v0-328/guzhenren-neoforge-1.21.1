package com.unknown.guzhenren.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unknown.guzhenren.attachment.data.dimension.DimensionReturnData;
import com.unknown.guzhenren.attachment.data.dimension.DimensionReturnData.ReturnPoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NBT codec roundtrips for the server-only dimension-return attachment.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class DimensionReturnDataTest {

    @Test
    @DisplayName("DimensionReturnData NBT codec roundtrips a present return point")
    void presentRoundTrip() {
        ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("guzhenren", "treasure_yellow_heaven"));
        ReturnPoint point = new ReturnPoint(level, 1.5, 64.0, -2.5, 90.0F, 10.0F, true, false);
        DimensionReturnData expected = DimensionReturnData.of(point);

        DimensionReturnData decoded = nbtRoundTrip(expected);
        assertTrue(decoded.isPresent());
        ReturnPoint actual = decoded.point().orElseThrow();
        assertEquals(point.level(), actual.level());
        assertEquals(point.x(), actual.x(), 1.0E-6);
        assertEquals(point.y(), actual.y(), 1.0E-6);
        assertEquals(point.z(), actual.z(), 1.0E-6);
        assertEquals(point.yaw(), actual.yaw(), 1.0E-6);
        assertEquals(point.pitch(), actual.pitch(), 1.0E-6);
        assertEquals(point.mayfly(), actual.mayfly());
        assertEquals(point.flying(), actual.flying());
    }

    @Test
    @DisplayName("DimensionReturnData default (empty) roundtrips through its codec")
    void emptyRoundTrip() {
        DimensionReturnData decoded = nbtRoundTrip(DimensionReturnData.DEFAULT);
        assertFalse(decoded.isPresent());
        assertEquals(DimensionReturnData.DEFAULT, decoded);
    }

    private static DimensionReturnData nbtRoundTrip(DimensionReturnData data) {
        Tag tag = DimensionReturnData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        return DimensionReturnData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }
}
