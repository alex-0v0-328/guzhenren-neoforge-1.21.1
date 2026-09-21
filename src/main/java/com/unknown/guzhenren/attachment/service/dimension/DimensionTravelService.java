package com.unknown.guzhenren.attachment.service.dimension;

import com.unknown.guzhenren.attachment.data.dimension.DimensionReturnData;
import com.unknown.guzhenren.attachment.data.dimension.DimensionReturnData.ReturnPoint;
import com.unknown.guzhenren.registry.attachment.ModAttachments;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Generic anchored-dimension travel service. A single {@link DimensionReturnData} attachment stores the
 * one return point for the one anchored dimension a player may be inside at a time.
 *
 * <p>{@link #enter} snapshots the player's original location and flight abilities before teleporting;
 * {@link #exit} restores them. Death clears the record through {@link
 * com.unknown.guzhenren.attachment.PlayerDataService}.
 *
 * @author Alex
 * @version 1.0.0
 * @see DimensionReturnData
 * @since 1.0.0
 */

public final class DimensionTravelService {

    private DimensionTravelService() {}

    /**
     * @return {@code true} if the player is currently in the given dimension.
     */
    public static boolean isInside(@NotNull Player player, @NotNull ResourceKey<Level> dimension) {
        return player.level().dimension().equals(Objects.requireNonNull(dimension, "dimension"));
    }

    /**
     * Enters an anchored dimension.
     *
     * @return {@code true} if the player was teleported; {@code false} if they were already inside the
     *     dimension or the target level could not be resolved.
     */
    public static boolean enter(@NotNull ServerPlayer player, @NotNull ResourceKey<Level> dimension,
            @NotNull Vec3 spawn) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(spawn, "spawn");
        if (isInside(player, dimension)) {
            return false;
        }

        ServerLevel target = player.server.getLevel(dimension);
        if (target == null) {
            return false;
        }

        ReturnPoint point = new ReturnPoint(
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.getAbilities().mayfly,
                player.getAbilities().flying);
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.of(point));

        player.teleportTo(target, spawn.x, spawn.y, spawn.z, player.getYRot(), player.getXRot());
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        return true;
    }

    /**
     * Exits the anchored dimension.
     *
     * <p>If a return point exists, the player is teleported back to it and their saved flight abilities
     * are restored. If no record exists, the player is sent to the Overworld shared spawn as a fallback.
     * In both cases the record is cleared afterwards.
     *
     * @return {@code true} if a stored return point was used; {@code false} if the fallback was used.
     */
    public static boolean exit(@NotNull ServerPlayer player) {
        DimensionReturnData data = player.getData(ModAttachments.DIMENSION_RETURN);
        if (data.isPresent()) {
            ReturnPoint point = data.point().orElseThrow();
            ServerLevel target = player.server.getLevel(point.level());
            boolean used = target != null;
            if (used) {
                player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
                player.getAbilities().mayfly = point.mayfly();
                player.getAbilities().flying = point.flying();
                player.fallDistance = 0.0F;
                player.onUpdateAbilities();
            } else {
                fallbackToOverworldSpawn(player);
            }
            clear(player);
            return used;
        }

        fallbackToOverworldSpawn(player);
        clear(player);
        return false;
    }

    private static void fallbackToOverworldSpawn(@NotNull ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
    }

    /**
     * Clears the return record. Safe to call even when no record is present.
     */
    public static void clear(@NotNull Player player) {
        player.setData(ModAttachments.DIMENSION_RETURN, DimensionReturnData.DEFAULT);
    }

    public static void clear(@NotNull ServerPlayer player) {
        clear((Player) player);
    }

    /**
     * Re-grants flight permission without forcing {@code flying} on. Call this each tick while the
     * player is inside an anchored dimension so they may choose to hover or stop flying freely.
     */
    public static void ensureFlight(@NotNull ServerPlayer player) {
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    /**
     * Teleports the player back to the given spawn if they have fallen below the current level's minimum
     * build height.
     *
     * @return {@code true} if a rescue teleport happened.
     */
    public static boolean rescueIfBelowVoid(@NotNull ServerPlayer player, @NotNull Vec3 spawn) {
        if (player.getY() < player.level().getMinBuildHeight()) {
            player.teleportTo((ServerLevel) player.level(), spawn.x, spawn.y, spawn.z,
                    player.getYRot(), player.getXRot());
            player.fallDistance = 0.0F;
            return true;
        }
        return false;
    }
}
