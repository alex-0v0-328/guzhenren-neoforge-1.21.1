package com.unknown.guzhenren.entity;

import com.unknown.guzhenren.entity.ai.FleePlayerGoal;
import com.unknown.guzhenren.entity.ai.LandRestGoal;
import com.unknown.guzhenren.entity.ai.WanderCourseGoal;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * A flying Gu [飞行蛊] with a shared flight, landing and rest lifecycle.
 *
 * <p>The server owns the phase and landing request. Both values are synchronized entity data, so a
 * client can select the correct steady animation after spawning, tracking or loading an entity. The
 * server triggers the concrete entity's one-shot transition animation when a phase transition needs
 * one; a newly tracked or loaded client only selects the synchronized steady state.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public abstract class RestingFlyingGuEntity extends FlyingGuEntity {

    public static final double FLEE_RANGE = 6.0D;
    public static final double ESCAPE_RANGE = 10.0D;
    private static final EntityDataAccessor<Byte> DATA_FLIGHT_PHASE = SynchedEntityData.defineId(
            RestingFlyingGuEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_WANTS_TO_LAND = SynchedEntityData.defineId(
            RestingFlyingGuEntity.class, EntityDataSerializers.BOOLEAN);

    protected RestingFlyingGuEntity(EntityType<? extends RestingFlyingGuEntity> type, Level level,
                                    Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FleePlayerGoal(this));
        goalSelector.addGoal(1, new LandRestGoal(this));
        goalSelector.addGoal(2, new WanderCourseGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLIGHT_PHASE, FlightPhase.FLYING.id());
        builder.define(DATA_WANTS_TO_LAND, false);
    }

    public FlightPhase phase() {
        return FlightPhase.fromId(entityData.get(DATA_FLIGHT_PHASE));
    }

    public boolean wantsToLand() {
        return entityData.get(DATA_WANTS_TO_LAND);
    }

    public void requestLanding() {
        entityData.set(DATA_WANTS_TO_LAND, true);
    }

    public void beginLanding() {
        entityData.set(DATA_WANTS_TO_LAND, true);
        setPhase(FlightPhase.LANDING);
    }

    public void beginResting() {
        setPhase(FlightPhase.RESTING);
        entityData.set(DATA_WANTS_TO_LAND, false);
        if (!level().isClientSide()) playLandingAnimation();
    }

    public void takeOff() {
        setPhase(FlightPhase.FLYING);
        entityData.set(DATA_WANTS_TO_LAND, false);
        if (!level().isClientSide()) playTakeoffAnimation();
    }

    private void setPhase(FlightPhase next) {
        if (phase() != next) entityData.set(DATA_FLIGHT_PHASE, next.id());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("FlightPhase", phase().id());
        tag.putBoolean("WantsToLand", wantsToLand());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        FlightPhase savedPhase = FlightPhase.fromId(tag.getByte("FlightPhase"));
        entityData.set(DATA_FLIGHT_PHASE, savedPhase.id());
        boolean wantsToLand = tag.getBoolean("WantsToLand");
        // Goal state is not serialized. Re-arm a saved ground state so the new entity can enter the
        // landing goal and rebuild its rest timer instead of remaining permanently idle after reload.
        if (savedPhase != FlightPhase.FLYING) wantsToLand = true;
        entityData.set(DATA_WANTS_TO_LAND, wantsToLand);
    }

    protected abstract void playLandingAnimation();

    protected abstract void playTakeoffAnimation();

    public enum FlightPhase {
        FLYING,
        LANDING,
        RESTING;

        private byte id() {
            return (byte) ordinal();
        }

        private static FlightPhase fromId(byte id) {
            FlightPhase[] phases = values();
            return id >= 0 && id < phases.length ? phases[id] : FLYING;
        }
    }
}
