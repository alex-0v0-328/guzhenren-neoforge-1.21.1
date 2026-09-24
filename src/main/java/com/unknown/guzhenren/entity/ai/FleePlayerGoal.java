package com.unknown.guzhenren.entity.ai;

import com.unknown.guzhenren.entity.RestingFlyingGuEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The escape reaction shared by flying Gu with a landing and rest lifecycle.
 *
 * <p>A non-creative, non-spectator player within six blocks is a threat. The goal holds until that
 * player is farther than ten blocks. The escape point uses {@link AirAndWaterRandomPos} because the
 * normal ground-only random position helper returns no point for a flying mob in open air. A resting
 * or landing Gu takes off before the escape navigation starts, so a threat cannot leave the entity
 * stuck in a ground phase.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class FleePlayerGoal extends Goal {

    private static final double FLEE_SPEED_MODIFIER = 3.0D;
    private static final int ESCAPE_HORIZONTAL_RANGE = 16;
    private static final int ESCAPE_VERTICAL_RANGE = 7;
    private static final float ESCAPE_CONE_ANGLE = (float) (Math.PI / 2);
    private final RestingFlyingGuEntity gu;
    private @Nullable Player threat;
    private @Nullable Vec3 escapeCourse;

    public FleePlayerGoal(RestingFlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        threat = nearestThreat(gu);
        if (threat == null) return false;
        escapeCourse = courseAwayFromThreat(threat);
        if (escapeCourse == null) return false;
        boolean courseCloserToThreat = threat.distanceToSqr(escapeCourse.x, escapeCourse.y, escapeCourse.z)
                < threat.distanceToSqr(gu);
        if (courseCloserToThreat) return false;
        return gu.getNavigation().createPath(escapeCourse.x, escapeCourse.y, escapeCourse.z, 0) != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Re-checked every tick: a player who switches to creative or spectator, or leaves the dimension, is
        // no longer the threat the goal started from.
        return threat != null && threat.isAlive() && isThreat(threat) && threat.level() == gu.level()
                && gu.distanceTo(threat) < RestingFlyingGuEntity.ESCAPE_RANGE;
    }

    @Override
    public void start() {
        if (gu.phase() != RestingFlyingGuEntity.FlightPhase.FLYING) gu.takeOff();
        if (escapeCourse != null) {
            gu.getNavigation().moveTo(escapeCourse.x, escapeCourse.y, escapeCourse.z, FLEE_SPEED_MODIFIER);
        }
    }

    @Override
    public void stop() {
        threat = null;
        escapeCourse = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {return true;}

    @Override
    public void tick() {
        if (threat == null || !gu.getNavigation().isDone()) return;
        Vec3 course = courseAwayFromThreat(threat);
        if (course != null) gu.getNavigation().moveTo(course.x, course.y, course.z, FLEE_SPEED_MODIFIER);
    }

    private @Nullable Vec3 courseAwayFromThreat(@NotNull Player threat) {
        Vec3 away = gu.position().subtract(threat.position());
        return AirAndWaterRandomPos.getPos(gu, ESCAPE_HORIZONTAL_RANGE, ESCAPE_VERTICAL_RANGE, 0,
                away.x, away.z, ESCAPE_CONE_ANGLE);
    }

    @SuppressWarnings("resource")
    static @Nullable Player nearestThreat(RestingFlyingGuEntity gu) {
        return gu.level().getNearestPlayer(gu.getX(), gu.getY(), gu.getZ(), RestingFlyingGuEntity.FLEE_RANGE,
                FleePlayerGoal::isThreat);
    }

    private static boolean isThreat(Entity entity) {
        return entity instanceof Player player && !player.isSpectator() && !player.isCreative();
    }
}
