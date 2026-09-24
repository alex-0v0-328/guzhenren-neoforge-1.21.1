package com.unknown.guzhenren.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unknown.guzhenren.custom.enums.path.GuPath;
import com.unknown.guzhenren.network.payload.NourishAperturePayload;
import com.unknown.guzhenren.network.payload.SetSecondaryPathPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PayloadDecodeTest {

    @Test
    @DisplayName("a forged secondary-path ordinal fails to decode instead of indexing past GuPath")
    void forgedSecondaryPath() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufCodecs.VAR_INT.encode(buffer, 0);
            ByteBufCodecs.VAR_INT.encode(buffer, GuPath.values().length + 1);
            assertThrows(DecoderException.class, () -> SetSecondaryPathPayload.STREAM_CODEC.decode(buffer));
            buffer.clear();

            SetSecondaryPathPayload valid = new SetSecondaryPathPayload(1, GuPath.TIME);
            SetSecondaryPathPayload.STREAM_CODEC.encode(buffer, valid);
            assertEquals(valid, SetSecondaryPathPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
    @Test
    @DisplayName("a forged nourish action ordinal fails to decode")
    void forgedNourishAction() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufCodecs.VAR_INT.encode(buffer, NourishAperturePayload.Action.values().length);
            ByteBufCodecs.VAR_INT.encode(buffer, 0);
            assertThrows(DecoderException.class, () -> NourishAperturePayload.STREAM_CODEC.decode(buffer));
            buffer.clear();

            NourishAperturePayload valid = new NourishAperturePayload(NourishAperturePayload.Action.CANCEL, 0);
            NourishAperturePayload.STREAM_CODEC.encode(buffer, valid);
            assertEquals(valid, NourishAperturePayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
