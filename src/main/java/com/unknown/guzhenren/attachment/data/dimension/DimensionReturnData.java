package com.unknown.guzhenren.attachment.data.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Return-point snapshot for anchored dimension travel [宝黄天 / 福地 / 洞天]. A player can only be inside
 * one anchored dimension at a time, so one optional {@link ReturnPoint} is enough. The record is
 * server-only: it is serialized to NBT but never synced to clients.
 *
 * <p>The compact constructor rejects a null outer {@code point}; the nested compact constructor rejects
 * a null level key.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.attachment.service.dimension.DimensionTravelService
 * @since 1.0.0
 */

public record DimensionReturnData(Optional<ReturnPoint> point) {

    public static final DimensionReturnData DEFAULT = new DimensionReturnData(Optional.empty());

    public static final Codec<DimensionReturnData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ReturnPoint.CODEC.optionalFieldOf("point").forGetter(DimensionReturnData::point)
    ).apply(instance, DimensionReturnData::new));

    public DimensionReturnData {
        point = Objects.requireNonNull(point, "point");
    }

    public boolean isPresent() {
        return point.isPresent();
    }

    public static DimensionReturnData empty() {
        return DEFAULT;
    }

    public static DimensionReturnData of(@NotNull ReturnPoint point) {
        return new DimensionReturnData(Optional.of(point));
    }

    public static DimensionReturnData of(@NotNull ResourceKey<Level> level, double x, double y, double z,
            float yaw, float pitch, boolean mayfly, boolean flying) {
        return of(new ReturnPoint(level, x, y, z, yaw, pitch, mayfly, flying));
    }

    public DimensionReturnData cleared() {
        return DEFAULT;
    }

    /**
     * One saved location: dimension, position, rotation, and the flight abilities that should be
     * restored on exit.
     */
    public record ReturnPoint(ResourceKey<Level> level, double x, double y, double z, float yaw, float pitch,
            boolean mayfly, boolean flying) {

        public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("level").forGetter(ReturnPoint::level),
                Codec.DOUBLE.fieldOf("x").forGetter(ReturnPoint::x),
                Codec.DOUBLE.fieldOf("y").forGetter(ReturnPoint::y),
                Codec.DOUBLE.fieldOf("z").forGetter(ReturnPoint::z),
                Codec.FLOAT.fieldOf("yaw").forGetter(ReturnPoint::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(ReturnPoint::pitch),
                Codec.BOOL.fieldOf("mayfly").forGetter(ReturnPoint::mayfly),
                Codec.BOOL.fieldOf("flying").forGetter(ReturnPoint::flying)
        ).apply(instance, ReturnPoint::new));

        public ReturnPoint {
            level = Objects.requireNonNull(level, "level");
        }
    }
}
