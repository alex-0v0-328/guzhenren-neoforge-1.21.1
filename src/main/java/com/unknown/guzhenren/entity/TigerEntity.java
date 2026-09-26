package com.unknown.guzhenren.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A tiger; the orange and white coats register as two entity types sharing this class (they differ
 * only in texture).
 *
 * <p>Combat swipes at arm's length (hit frame at tick 8) and pounces across three to eight blocks:
 * the leap impulse fires at tick 12 with the direction locked at that moment, and the landing hit
 * is a swept-body test over the flight window. Vanilla has no tiger sounds, so the ocelot family
 * stands in at a lowered pitch; the roar reuses the polar bear warning slightly deepened.
 */
public final class TigerEntity extends BeastEntity {

    public static final int SWIPE_HIT_TICK = 8;
    public static final int SWIPE_END_TICKS = 16;
    public static final int POUNCE_LEAP_TICK = 12;
    public static final int POUNCE_WINDOW_END_TICKS = 18;
    public static final int POUNCE_END_TICKS = 28;
    public static final int ROAR_TICKS = 40;
    public static final int LIE_DOWN_TICKS = 24;
    public static final int GET_UP_TICKS = 20;
    public static final int DEATH_REMOVE_TICK = 32;
    public static final double POUNCE_MIN_DISTANCE = 3.0D;
    public static final double POUNCE_MAX_DISTANCE = 8.0D;
    public static final double POUNCE_REACH = 2.5D;
    public static final float SWIPE_DAMAGE = 8.0F;
    public static final float POUNCE_DAMAGE = 12.0F;
    public static final double MAX_HEALTH = 48.0D;
    private static final double FOLLOW_RANGE_BLOCKS = 24.0D;
    private static final double MOVEMENT_SPEED = 0.28D;
    private static final double POUNCE_UPWARD = 0.45D;
    private static final double POUNCE_MIN_SPEED = 0.6D;
    private static final double POUNCE_MAX_SPEED = 1.3D;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 pounceDirection = Vec3.ZERO;
    private Vec3 pounceLastPosition = Vec3.ZERO;
    private double pounceSpeed;

    public TigerEntity(EntityType<? extends TigerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    @Override
    protected int swipeHitTick() { return SWIPE_HIT_TICK; }
    @Override
    protected int swipeActionTicks() { return SWIPE_END_TICKS; }
    @Override
    protected int heavyHitTick() { return POUNCE_LEAP_TICK; }
    @Override
    protected int heavyActionTicks() { return POUNCE_END_TICKS; }
    @Override
    protected int roarActionTicks() { return ROAR_TICKS; }
    @Override
    protected int lieDownActionTicks() { return LIE_DOWN_TICKS; }
    @Override
    protected int getUpActionTicks() { return GET_UP_TICKS; }
    @Override
    protected int deathRemoveTick() { return DEATH_REMOVE_TICK; }
    @Override
    protected float swipeDamage() { return SWIPE_DAMAGE; }
    @Override
    protected float heavyDamage() { return POUNCE_DAMAGE; }
    @Override
    protected double swipeKnockback() { return 0.6D; }
    @Override
    protected double swipeUpward() { return 0.0D; }
    @Override
    protected boolean huntsActively() { return true; }
    @Override
    protected double pursuitSpeed() { return 1.5D; }
    @Override
    protected double cullExtraHeight() { return 0.2D; }
    @Override
    protected double cullHorizontalInflate() { return 0.8D; }

    @Override
    protected void chooseAttack(LivingEntity target) {
        double distance = this.distanceTo(target);
        if (distance <= SWIPE_REACH && this.swipeCooldown() == 0 && this.hasLineOfSight(target)) {
            this.startAction(Action.ATTACK_SWIPE);
        } else if (distance >= POUNCE_MIN_DISTANCE && distance <= POUNCE_MAX_DISTANCE
                && this.heavyCooldown() == 0 && this.canStartLunge(target, 6.0D)) {
            this.startAction(Action.ATTACK_HEAVY);
        }
    }

    @Override
    public boolean startAction(Action next) {
        boolean started = super.startAction(next);
        // Each pounce locks its own leap direction; a stale one from the previous pounce is never reused.
        if (started && next == Action.ATTACK_HEAVY) this.pounceDirection = Vec3.ZERO;
        return started;
    }

    /** The pounce leaps at tick 12 along the direction locked at that moment, then lands the hit. */
    @Override
    protected void tickHeavyAttack(long ticks) {
        LivingEntity target = this.actionTarget;
        if (ticks < POUNCE_LEAP_TICK) {
            if (target == null || !this.canAttackTarget(target)) this.finishAction();
            return;
        }
        if (this.pounceDirection.lengthSqr() == 0.0D) {
            // Leaps on the first tick at or after the leap frame, so an unticked frame cannot skip the lock;
            // a leap that could no longer reach the landing window is dropped.
            if (ticks >= POUNCE_WINDOW_END_TICKS || target == null || !this.canAttackTarget(target)) {
                this.finishAction();
                return;
            }
            this.pounceDirection = this.directionOrFacing(target);
            this.faceDirection(this.pounceDirection);
            this.pounceSpeed = Mth.clamp(this.distanceTo(target) / 6.0D, POUNCE_MIN_SPEED, POUNCE_MAX_SPEED);
            this.setDeltaMovement(this.pounceDirection.x * this.pounceSpeed, POUNCE_UPWARD,
                    this.pounceDirection.z * this.pounceSpeed);
            this.hasImpulse = true;
            this.pounceLastPosition = this.position();
            return;
        }
        if (this.horizontalCollision) {
            // A wall stopped the leap; the pounce ends early without a hit.
            this.finishAction();
            return;
        }
        Vec3 current = this.position();
        if (this.heavyKeepsMomentum(ticks)) {
            // The leap velocity is re-applied inside the landing window so the arc covers the locked
            // distance instead of bleeding off to drag; gravity still shapes the vertical arc. Past the
            // window tickAction zeroes the horizontal velocity, so the tiger never slides beyond it.
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(this.pounceDirection.x * this.pounceSpeed, velocity.y,
                    this.pounceDirection.z * this.pounceSpeed);
        }
        if (!this.heavyHit && ticks < POUNCE_WINDOW_END_TICKS && target != null
                && this.canAttackTarget(target)
                && this.sweptHit(target, this.pounceLastPosition, current)) {
            this.heavyHit = true;
            this.applyAttack(target, this.heavyDamage(), 1.0D, 0.2D, this.pounceDirection);
        }
        this.pounceLastPosition = current;
        if (ticks >= POUNCE_END_TICKS) this.finishAction();
    }

    /** Momentum is kept from the leap tick through the landing window; gravity supplies the arc. */
    @Override
    protected boolean heavyKeepsMomentum(long ticks) {
        return ticks >= POUNCE_LEAP_TICK && ticks < POUNCE_WINDOW_END_TICKS;
    }

    /** Tigers only sit; they have no roll or scratch animations. */
    @Override
    protected Action pickDaytimeAmbient(double roll) {
        return Action.SIT;
    }

    @Override
    protected SoundEvent roarSound() { return SoundEvents.POLAR_BEAR_WARNING; }
    @Override
    protected float roarPitch() { return 0.9F; }
    @Override
    protected SoundEvent stepSound() { return SoundEvents.WOLF_STEP; }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.OCELOT_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.OCELOT_DEATH; }
    @Override
    public float getVoicePitch() { return 0.8F; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new BeastAnimationController<>(this, this::animationState,
                this::action, this::actionTicks, this::actionSequence));
    }

    private PlayState animationState(AnimationState<TigerEntity> state) {
        return switch (this.action()) {
            case SIT -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.sit"));
            case LIE_DOWN -> state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.lie_down"));
            case LIE -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.lie"));
            case SLEEP -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.sleep"));
            case GET_UP -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.get_up"));
            case ROAR -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.roar"));
            case ATTACK_SWIPE -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.attack_swipe"));
            case ATTACK_HEAVY -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.attack_pounce"));
            case HURT_LEFT -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.hurt_left"));
            case HURT_RIGHT -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.hurt_right"));
            case DEATH -> state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.death"));
            case ROLL, BACK_SCRATCH -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle"));
            case IDLE -> {
                if (!state.isMoving()) yield state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle"));
                yield state.setAndContinue(this.pursuing()
                        ? RawAnimation.begin().thenLoop("animation.run")
                        : RawAnimation.begin().thenLoop("animation.walk"));
            }
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
