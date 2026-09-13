package com.unknown.guzhenren.entity.ai;

import com.unknown.guzhenren.entity.RestingFlyingGuEntity;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The landing and ground rest goal shared by the resting flying Gu entities.
 *
 * <p>A requested landing finds the ground in the entity's own column. Arrival starts a 160 to 200
 * tick rest with cleared motion and occasional head turns. A stalled landing or a failed navigation
 * target aborts through {@link RestingFlyingGuEntity#takeOff()}, so the goal cannot wedge the entity
 * in a non-flying phase.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class LandRestGoal extends Goal {

    private static final double LAND_SPEED_MODIFIER = 1.0D;
    private static final double ARRIVAL_RANGE = 1.5D;
    private static final int REST_TICKS = 160;
    private static final int REST_JITTER_TICKS = 41;
    private static final int LOOK_AROUND_ROLL = 80;
    private static final int LOOK_YAW_SPREAD = 181;
    private static final int LOOK_YAW_CENTER = 90;
    private static final int LANDING_TIMEOUT_TICKS = 600;
    private final RestingFlyingGuEntity gu;
    private Vec3 landingSpot = Vec3.ZERO;
    private int restRemaining;
    private int landingTicks;

    public LandRestGoal(RestingFlyingGuEntity gu) {
        this.gu = gu;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return gu.wantsToLand() && FleePlayerGoal.nearestThreat(gu) == null;
    }

    @Override
    public boolean canContinueToUse() {
        return gu.phase() != RestingFlyingGuEntity.FlightPhase.FLYING
                && FleePlayerGoal.nearestThreat(gu) == null;
    }

    @Override
    public void start() {
        gu.beginLanding();
        restRemaining = 0;
        landingTicks = 0;
        retargetGround();
    }

    @Override
    public void stop() {
        restRemaining = 0;
        gu.getNavigation().stop();
        // A threat can interrupt rest even when its escape path cannot start.
        if (gu.phase() != RestingFlyingGuEntity.FlightPhase.FLYING) gu.takeOff();
    }

    @Override
    public boolean requiresUpdateEveryTick() {return true;}

    @Override
    public void tick() {
        if (gu.phase() == RestingFlyingGuEntity.FlightPhase.LANDING) tickLanding();
        else tickResting();
    }

    private void tickLanding() {
        landingTicks++;
        if (gu.onGround() || gu.position().distanceTo(landingSpot) < ARRIVAL_RANGE) {
            gu.getNavigation().stop();
            gu.beginResting();
            restRemaining = REST_TICKS + gu.getRandom().nextInt(REST_JITTER_TICKS);
            gu.setDeltaMovement(Vec3.ZERO);
        } else if (landingTicks >= LANDING_TIMEOUT_TICKS) {
            gu.takeOff();
        } else if (gu.getNavigation().isDone() && !retargetGround()) {
            gu.takeOff();
        }
    }

    private void tickResting() {
        gu.setDeltaMovement(Vec3.ZERO);
        if (--restRemaining <= 0) {
            gu.takeOff();
            return;
        }
        if (gu.getRandom().nextInt(LOOK_AROUND_ROLL) == 0) {
            gu.setYRot(gu.getYRot() + gu.getRandom().nextInt(LOOK_YAW_SPREAD) - LOOK_YAW_CENTER);
        }
    }

    @SuppressWarnings("resource")
    private boolean retargetGround() {
        int x = Mth.floor(gu.getX());
        int z = Mth.floor(gu.getZ());
        int y = gu.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        landingSpot = new Vec3(x + 0.5D, y, z + 0.5D);
        return gu.getNavigation().moveTo(landingSpot.x, landingSpot.y, landingSpot.z, LAND_SPEED_MODIFIER);
    }
}
