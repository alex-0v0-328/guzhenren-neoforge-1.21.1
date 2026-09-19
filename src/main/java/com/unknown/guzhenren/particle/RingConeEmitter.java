package com.unknown.guzhenren.particle;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.registry.particle.ModParticles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The server-side cone trail behind the shockwave ring [激波环] (Alex, 2026-09-20 planted-trail
 * spec): each ring blooms in place from smallest to largest where it was born.
 *
 * <p>The dash trail plants rings along the path, one every {@link RingTrailSpacing#SPACING} blocks
 * of ACTUAL travel -- the server-side dodge movement arrives in uneven chunks (Epic Fight locks
 * movement and lets the animation drive it), so a per-tick drop clusters the rings at the start
 * and end of the path (Alex, 2026-09-20), while spacing by measured travel stays uniform across
 * windups, jumps and wall truncations. The ring at the dash start appears first and the one near
 * the end last. The punch trail replays the same planted logic on a fixed ray: the ring at the
 * strike point (the victim's hitbox center) appears first, then one per tick every
 * {@link RingTrailSpacing#SPACING} blocks behind the target -- a long tail (Alex, 2026-09-20). The
 * ring velocity is only a facing-normal carrier ({@link #RING_NORMAL_DRIFT}): a zero vector would
 * trip {@code RingGeometry}'s no-direction fallback and lay the ring flat.
 *
 * <p>Future speed- or force-feel actions call the {@code dashCone}/{@code punchCone} entry points
 * (or add a sibling) rather than spawning rings directly. Pure visual feedback: no damage, no
 * hitbox.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */
@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class RingConeEmitter {

    /** Rings one dash may drop (at {@link RingTrailSpacing#SPACING} 2 blocks: ~16 blocks of path). */
    private static final int DASH_MAX_RINGS = 8;
    /** Safety net for the dash burst; the idle stop normally ends it with the dodge. */
    private static final int DASH_MAX_TICKS = 25;
    /** Ticks below the idle speed before the dash burst stops (a dodge windup is shorter). */
    private static final int DASH_IDLE_LIMIT = 3;
    /** Squared horizontal speed under which a tick counts as still (0.05 blocks/tick). */
    private static final double DASH_IDLE_SPEED_SQR = 0.0025D;
    /** Dash ring spawn height above the feet: mid-body of the standing ring (max radius 1.1). */
    private static final double DASH_RING_HEIGHT = 1.0D;
    /**
     * Ring drift along the burst direction, blocks per tick. This is ONLY the facing-normal
     * carrier -- both trails are planted (Alex, 2026-09-20), and a zero vector would trip
     * {@code RingGeometry}'s no-direction fallback and lay the ring flat on the ground. Over the
     * ring's whole life it moves under a quarter block, which reads as stationary.
     */
    private static final double RING_NORMAL_DRIFT = 0.03D;
    /**
     * Rings one punch plants behind the target: one per tick at {@link RingTrailSpacing#SPACING}
     * spacing along the punch ray, so the tail runs ~16 blocks past the strike point (Alex,
     * 2026-09-20: "the rings behind spread over a very long distance").
     */
    private static final int PUNCH_MAX_RINGS = 8;

    /**
     * One active burst per effect per player, on separate maps so a dash trail and a punch trail
     * coexist (charging-crash play mixes the two): a dash inside its own window is skipped (the
     * double-trigger replay fix), a punch restarts only the punch trail.
     */
    private static final Map<UUID, Burst> DASH_BURSTS = new HashMap<>();
    private static final Map<UUID, Burst> PUNCH_BURSTS = new HashMap<>();

    private RingConeEmitter() {}

    /**
     * The dash trail: rings plant along the path at even spacing and bloom in place. One trail at
     * a time -- a retrigger while a burst is alive (a second payload inside the window) replayed
     * the whole trail, Alex's 2026-09-19 "the cone plays twice" report, so the dash side skips
     * instead of replacing.
     */
    public static void dashCone(ServerPlayer player, Vec3 motionDirection) {
        if (DASH_BURSTS.containsKey(player.getUUID())) return;
        if (motionDirection.lengthSqr() < 1.0E-6D) return;
        DASH_BURSTS.put(player.getUUID(), new DashBurst(motionDirection.normalize(),
                DASH_MAX_TICKS, player.position(), 0.0D, DASH_MAX_RINGS, 0));
    }

    /**
     * The punch trail: rings plant along the punch ray starting at the strike point (the victim's
     * hitbox center) -- the nearest appears first, then one per tick every
     * {@link RingTrailSpacing#SPACING} blocks behind the target.
     */
    public static void punchCone(ServerPlayer player, Vec3 look, Vec3 strikePoint) {
        if (look.lengthSqr() < 1.0E-6D) return;
        PUNCH_BURSTS.put(player.getUUID(), new PunchBurst(look.normalize(), PUNCH_MAX_RINGS, strikePoint));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isRemoved() || player.isDeadOrDying()
                || !(player.level() instanceof ServerLevel level)) {
            DASH_BURSTS.remove(player.getUUID());
            PUNCH_BURSTS.remove(player.getUUID());
            return;
        }
        tickBurst(DASH_BURSTS, player, level);
        tickBurst(PUNCH_BURSTS, player, level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DASH_BURSTS.remove(event.getEntity().getUUID());
        PUNCH_BURSTS.remove(event.getEntity().getUUID());
    }

    private static void tickBurst(Map<UUID, Burst> bursts, ServerPlayer player, ServerLevel level) {
        Burst burst = bursts.get(player.getUUID());
        if (burst == null) return;
        Burst next = burst.tick(player, level);
        if (next == null) bursts.remove(player.getUUID());
        else bursts.put(player.getUUID(), next);
    }

    /**
     * One active burst per player. {@code tick} drops this tick's ring(s) and returns the next
     * state, or null once the burst is spent.
     */
    private interface Burst {

        Burst tick(ServerPlayer player, ServerLevel level);
    }

    /**
     * The planted dash trail: a ring every {@link RingTrailSpacing#SPACING} blocks of measured
     * travel, positioned along the tick's movement segment so even a chunked dodge spreads them
     * evenly. Ends when the ring budget is spent, the player stands still for
     * {@link #DASH_IDLE_LIMIT} ticks, or the safety cap runs out.
     */
    private record DashBurst(Vec3 direction, int ticksLeft, Vec3 lastPos, double carry,
                             int ringsLeft, int idleTicks) implements Burst {

        @Override
        public Burst tick(ServerPlayer player, ServerLevel level) {
            if (ticksLeft <= 0 || ringsLeft <= 0 || idleTicks >= DASH_IDLE_LIMIT) return null;
            Vec3 position = player.position();
            Vec3 segment = position.subtract(lastPos);
            double segLen = Math.sqrt(segment.x * segment.x + segment.z * segment.z);
            int idle = segLen * segLen < DASH_IDLE_SPEED_SQR ? idleTicks + 1 : 0;

            double newCarry = carry;
            int newRingsLeft = ringsLeft;
            if (segLen > 0.0D) {
                RingTrailSpacing.Drops drops = RingTrailSpacing.drops(carry, segLen, ringsLeft);
                if (drops.offsets().length > 0) {
                    Vec3 segDir = new Vec3(segment.x, 0.0D, segment.z).normalize();
                    Vec3 velocity = direction.scale(RING_NORMAL_DRIFT);
                    for (double offset : drops.offsets()) {
                        // Count 0 + speed 1.0 hands the particle the exact velocity, which the ring
                        // also reads as its facing normal (FACING_MOTION).
                        Vec3 anchor = lastPos.add(segDir.scale(offset));
                        level.sendParticles(ModParticles.SHOCKWAVE_RING.get(),
                                anchor.x, player.getY() + DASH_RING_HEIGHT, anchor.z,
                                0, velocity.x, velocity.y, velocity.z, 1.0D);
                    }
                }
                newCarry = drops.carry();
                newRingsLeft = drops.ringsLeft();
            }
            return new DashBurst(direction, ticksLeft - 1, position, newCarry, newRingsLeft, idle);
        }
    }

    /**
     * The punch trail: the same planted logic as the dash, on a fixed ray -- the ring at the
     * strike point appears first, then one per tick every {@link RingTrailSpacing#SPACING} blocks
     * behind the target, each blooming in place.
     */
    private record PunchBurst(Vec3 direction, int ringsLeft, Vec3 anchor) implements Burst {

        @Override
        public Burst tick(ServerPlayer player, ServerLevel level) {
            if (ringsLeft <= 0) return null;
            Vec3 velocity = direction.scale(RING_NORMAL_DRIFT);
            int index = PUNCH_MAX_RINGS - ringsLeft;
            Vec3 ring = anchor.add(direction.scale(RingTrailSpacing.SPACING * index));
            // Count 0 + speed 1.0 hands the particle the exact velocity, which the ring also reads
            // as its facing normal (FACING_MOTION).
            level.sendParticles(ModParticles.IMPACT_RING.get(), ring.x, ring.y, ring.z,
                    0, velocity.x, velocity.y, velocity.z, 1.0D);
            return new PunchBurst(direction, ringsLeft - 1, anchor);
        }
    }
}
