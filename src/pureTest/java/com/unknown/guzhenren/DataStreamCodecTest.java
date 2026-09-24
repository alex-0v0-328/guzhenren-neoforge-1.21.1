package com.unknown.guzhenren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unknown.guzhenren.attachment.data.aperture.Aperture;
import com.unknown.guzhenren.attachment.data.body.BodyData;
import com.unknown.guzhenren.custom.enums.aperture.Rank;
import com.unknown.guzhenren.custom.enums.aperture.Stage;
import com.unknown.guzhenren.custom.enums.body.ExtremePhysique;
import com.unknown.guzhenren.custom.enums.body.Physique;
import com.unknown.guzhenren.custom.enums.body.Race;
import com.unknown.guzhenren.custom.enums.path.GuPath;
import com.unknown.guzhenren.serialization.ModStreamCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataStreamCodecTest {

    @Test
    @DisplayName("Aperture stream codec preserves nullable paths, the second flag and all sentinels")
    void apertureRoundTrip() {
        Aperture expected = new Aperture(Rank.THREE, Stage.UPPER, 83, 12_345L, GuPath.TIME, null, 678L, 73,
                987_654L, 42, true, true, null, true);
        ByteBuf buffer = Unpooled.buffer();
        try {
            Aperture.STREAM_CODEC.encode(buffer, expected);
            assertEquals(expected.rank(), ModStreamCodecs.ofEnum(Rank.class).decode(buffer));
            assertEquals(expected.stage(), ModStreamCodecs.ofEnum(Stage.class).decode(buffer));
            assertEquals(expected.baseEssence(), ByteBufCodecs.VAR_INT.decode(buffer));
            assertEquals(expected.currentEssence(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.primaryPath(), ModStreamCodecs.ofNullableEnum(GuPath.class).decode(buffer));
            assertEquals(expected.secondaryPath(), ModStreamCodecs.ofNullableEnum(GuPath.class).decode(buffer));
            assertEquals(expected.distilledEssence(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.pressure(), ByteBufCodecs.VAR_INT.decode(buffer));
            assertEquals(expected.pressureDeadlineTick(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.nourishProgress(), ByteBufCodecs.VAR_INT.decode(buffer));
            assertEquals(expected.petrified(), ByteBufCodecs.BOOL.decode(buffer));
            assertEquals(expected.distilling(), ByteBufCodecs.BOOL.decode(buffer));
            assertEquals(expected.second(), ByteBufCodecs.BOOL.decode(buffer));
            assertEquals(0, buffer.readableBytes());

            ModStreamCodecs.ofEnum(Rank.class).encode(buffer, expected.rank());
            ModStreamCodecs.ofEnum(Stage.class).encode(buffer, expected.stage());
            ByteBufCodecs.VAR_INT.encode(buffer, expected.baseEssence());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.currentEssence());
            ModStreamCodecs.ofNullableEnum(GuPath.class).encode(buffer, expected.primaryPath());
            ModStreamCodecs.ofNullableEnum(GuPath.class).encode(buffer, expected.secondaryPath());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.distilledEssence());
            ByteBufCodecs.VAR_INT.encode(buffer, expected.pressure());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.pressureDeadlineTick());
            ByteBufCodecs.VAR_INT.encode(buffer, expected.nourishProgress());
            ByteBufCodecs.BOOL.encode(buffer, expected.petrified());
            ByteBufCodecs.BOOL.encode(buffer, expected.distilling());
            ByteBufCodecs.BOOL.encode(buffer, expected.second());
            assertEquals(expected, Aperture.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
    @Test
    @DisplayName("Enum codecs reject out-of-range and negative ordinals as malformed packets")
    void enumCodecsRejectOutOfRangeOrdinals() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufCodecs.VAR_INT.encode(buffer, Rank.values().length);
            assertThrows(DecoderException.class, () -> ModStreamCodecs.ofEnum(Rank.class).decode(buffer));
            buffer.clear();
            ByteBufCodecs.VAR_INT.encode(buffer, -1);
            assertThrows(DecoderException.class, () -> ModStreamCodecs.ofEnum(Rank.class).decode(buffer));
            buffer.clear();
            ByteBufCodecs.VAR_INT.encode(buffer, GuPath.values().length + 1);
            assertThrows(DecoderException.class, () -> ModStreamCodecs.ofNullableEnum(GuPath.class).decode(buffer));
            buffer.clear();
            ByteBufCodecs.VAR_INT.encode(buffer, -1);
            assertThrows(DecoderException.class, () -> ModStreamCodecs.readNullableEnum(buffer, GuPath.class));
            buffer.clear();

            GuPath last = GuPath.values()[GuPath.values().length - 1];
            ByteBufCodecs.VAR_INT.encode(buffer, GuPath.values().length);
            assertEquals(last, ModStreamCodecs.ofNullableEnum(GuPath.class).decode(buffer));
        } finally {
            buffer.release();
        }
    }
    @Test
    @DisplayName("BodyData stream codec preserves anchors and zombie tier")
    void bodyRoundTrip() {
        BodyData expected = new BodyData(Set.of(Physique.HALF_ZOMBIE, Physique.EXTREME),
                ExtremePhysique.GREAT_STRENGTH_TRUE_MARTIAL, Race.DRAGONMEN, 123L, 456L, 789L, 321L, 654L, 4,
                987L);
        ByteBuf buffer = Unpooled.buffer();
        try {
            BodyData.STREAM_CODEC.encode(buffer, expected);
            assertEquals(expected.physiques(), ModStreamCodecs.enumSet(Physique.class).decode(buffer));
            assertEquals(expected.extremePhysique(), ModStreamCodecs.ofEnum(ExtremePhysique.class).decode(buffer));
            assertEquals(expected.race(), ModStreamCodecs.ofEnum(Race.class).decode(buffer));
            assertEquals(expected.ageParts(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.lifespanParts(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.lastDayIndex(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.deathQiLifespanLost(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.halfZombieEndTick(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(expected.zombieTier(), ByteBufCodecs.VAR_INT.decode(buffer));
            assertEquals(expected.lastBilledTick(), ByteBufCodecs.VAR_LONG.decode(buffer));
            assertEquals(0, buffer.readableBytes());

            ModStreamCodecs.enumSet(Physique.class).encode(buffer, expected.physiques());
            ModStreamCodecs.ofEnum(ExtremePhysique.class).encode(buffer, expected.extremePhysique());
            ModStreamCodecs.ofEnum(Race.class).encode(buffer, expected.race());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.ageParts());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.lifespanParts());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.lastDayIndex());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.deathQiLifespanLost());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.halfZombieEndTick());
            ByteBufCodecs.VAR_INT.encode(buffer, expected.zombieTier());
            ByteBufCodecs.VAR_LONG.encode(buffer, expected.lastBilledTick());
            assertEquals(expected, BodyData.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
