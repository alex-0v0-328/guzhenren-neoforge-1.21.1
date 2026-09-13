package com.unknown.guzhenren.attachment.data.mind;

import com.google.common.math.LongMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.unknown.guzhenren.custom.enums.wisdom.WisdomType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One thought [念] pool of one {@link WisdomType}; leaf record nested inside {@link MindData};
 * immutable, standard {@code DEFAULT}-codec-stream shape.
 *
 * <p>⚠ Not self-clamping: only some wisdom types may burst past their cap, so the clamp lives in {@link
 * com.unknown.guzhenren.attachment.service.mind.MindService} and every write passes through there. ⚠
 * {@code burstAt()} divides before multiplying ({@code max / DENOM * NUMER}) and saturates the final
 * product, preserving the intended floor-before-multiply ratio without overflow. ⚠ {@code slept()}
 * restores only HALF the deficit when the buffer was used -- never reduce {@code current}.
 *
 * @author Alex
 * @version 1.0.0
 * @see MindData
 * @see com.unknown.guzhenren.attachment.service.mind.MindService
 * @since 1.0.0
 */

public record MindPool(long current, long max, boolean bufferUsed) {

    public static final Codec<MindPool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("current", 0L).forGetter(MindPool::current),
            Codec.LONG.optionalFieldOf("max", 0L).forGetter(MindPool::max),
            Codec.BOOL.optionalFieldOf("buffer_used", false).forGetter(MindPool::bufferUsed)
    ).apply(instance, MindPool::new));
    public static final StreamCodec<ByteBuf, MindPool> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, MindPool::current,
            ByteBufCodecs.VAR_LONG, MindPool::max,
            ByteBufCodecs.BOOL, MindPool::bufferUsed,
            MindPool::new);
    public MindPool {
        current = Math.max(0L, current);
        max = Math.max(0L, max);
        bufferUsed = bufferUsed || current > max;
    }
    public static MindPool of(WisdomType type) {return new MindPool(0L, type.getDefaultCapacity(), false);}
    public long burstAt() {
        return LongMath.saturatedMultiply(max / WisdomType.BURST_DENOMINATOR, WisdomType.BURST_NUMERATOR);
    }
    public boolean isOverflowing() {return current > burstAt();}
    public MindPool withCurrent(long v) {return new MindPool(v, max, bufferUsed);}
    public MindPool withMax(long v) {return new MindPool(current, v, bufferUsed);}
    public MindPool slept() {
        long restored = bufferUsed && current < max ? current + (max - current) / 2 : Math.max(current, max);
        return new MindPool(restored, max, false);
    }
    public MindPool emptied() {return new MindPool(0L, max, false);}
}
