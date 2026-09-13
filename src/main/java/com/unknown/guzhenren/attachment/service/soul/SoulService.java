package com.unknown.guzhenren.attachment.service.soul;

import com.google.common.math.LongMath;
import com.unknown.guzhenren.attachment.data.soul.SoulData;
import com.unknown.guzhenren.registry.attachment.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The only writer of Soul [魂魄], the one pool whose bottom is lethal. Static service; the compact ctor
 * of {@link SoulData} already clamps current to {@code [0, max]}, so this service is mostly a
 * pass-through -- but it owns the {@code revive} and {@code refill} shapes the lifecycle needs.
 *
 * <p>⚠ Nothing here KILLS: emptying the soul only sets up a lethal state that the heartbeat's last step
 * ({@code checkLethalState}) notices -- there is no "kill" call to search for. ⚠ A cap of 0 also lands
 * current at 0 (one check catches both), so {@code revive} must restore {@code DEFAULT_MAX_SOUL} when
 * the cap itself was 0 -- a respawn may never hand back a value the lethal check would fire on. ⚠
 * Guts Gu [胆识蛊] raises {@code maxSoul}; do not "fix" the refining cost by softening the numbers.
 *
 * @author Alex
 * @version 1.0.0
 * @see SoulData
 * @since 1.0.0
 */

public final class SoulService {

    private SoulService() {}
    public static @NotNull SoulData get(@NotNull Player player) {return player.getData(ModAttachments.SOUL);}
    public static void setMax(@NotNull ServerPlayer p, long v) {store(p, get(p).withMaxSoul(v));}
    public static void addMax(@NotNull ServerPlayer p, long delta) {
        setMax(p, LongMath.saturatedAdd(get(p).maxSoul(), delta));
    }
    public static void setCurrent(@NotNull ServerPlayer p, long v) {store(p, get(p).withCurrentSoul(v));}
    public static void addCurrent(@NotNull ServerPlayer p, long delta) {
        setCurrent(p, LongMath.saturatedAdd(get(p).currentSoul(), delta));
    }
    public static void refill(@NotNull ServerPlayer p) {store(p, get(p).refilled());}
    public static void revive(@NotNull ServerPlayer p) {store(p, get(p).revived());}
    private static void store(ServerPlayer p, SoulData data) {p.setData(ModAttachments.SOUL, data);}
    public static boolean consume(@NotNull ServerPlayer player, long amount) {
        if (amount <= 0L) return true;
        SoulData soul = get(player);
        if (soul.currentSoul() < amount) return false;
        setCurrent(player, soul.currentSoul() - amount);
        return true;
    }
}
