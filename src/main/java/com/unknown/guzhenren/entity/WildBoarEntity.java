package com.unknown.guzhenren.entity;

import java.util.EnumSet;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Server-authoritative wild boar with retaliatory charge and toss attacks. */
public final class WildBoarEntity extends PathfinderMob implements GeoEntity {

    public static final int FOLLOW_RANGE_BLOCKS = 16;
    public static final int CHARGE_COOLDOWN_TICKS = 100;
    public static final int TOSS_COOLDOWN_TICKS = 40;
    public static final int ATTACK_RECOVERY_TICKS = 10;
    public static final int CHARGE_WINDUP_TICKS = 6;
    public static final int CHARGE_WINDOW_END_TICKS = 16;
    public static final int CHARGE_ACTION_END_TICKS = 24;
    public static final int TOSS_HIT_TICK = 9;
    public static final int TOSS_ACTION_END_TICKS = 16;
    public static final int ALERT_ACTION_TICKS = 16;
    public static final int HURT_ACTION_TICKS = 7;
    public static final int GRAZE_ACTION_TICKS = 100;
    public static final int SIGHT_LOSS_TICKS = 100;
    public static final int DEATH_REMOVE_TICK = 32;

    private static final float CHARGE_DAMAGE = 8.0F;
    private static final float TOSS_DAMAGE = 6.0F;
    private static final double CHARGE_STEP = 0.6D;
    private static final double CHARGE_KNOCKBACK = 1.0D;
    private static final double TOSS_KNOCKBACK = 0.6D;
    private static final double TOSS_UPWARD = 0.5D;
    private static final double GRAZE_CHANCE = 0.25D;
    private static final double TARGET_MAX_DISTANCE_SQR = FOLLOW_RANGE_BLOCKS * FOLLOW_RANGE_BLOCKS;

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_START = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CHARGE_COOLDOWN = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOSS_COOLDOWN = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PURSUING = SynchedEntityData.defineId(
            WildBoarEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private LivingEntity actionTarget;
    private Vec3 chargeDirection = Vec3.ZERO;
    private boolean chargeHit;
    private int lostSightTicks;
    private int recoveryTicks;

    public WildBoarEntity(EntityType<? extends WildBoarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE_BLOCKS);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal(this));
        this.goalSelector.addGoal(4, new GrazeGoal(this));
        this.goalSelector.addGoal(6, new WanderGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, Action.IDLE.id());
        builder.define(DATA_ACTION_START, 0L);
        builder.define(DATA_ACTION_SEQUENCE, 0);
        builder.define(DATA_CHARGE_COOLDOWN, 0);
        builder.define(DATA_TOSS_COOLDOWN, 0);
        builder.define(DATA_PURSUING, false);
    }

    /** Current synchronized action, including one-shot actions for late trackers. */
    public Action action() {
        return Action.fromId(this.entityData.get(DATA_ACTION));
    }

    /** Elapsed server game ticks since the synchronized action start. */
    public long actionTicks() {
        if (this.action() == Action.IDLE) return 0L;
        long elapsed = this.level().getGameTime() - this.entityData.get(DATA_ACTION_START);
        return Math.max(0L, elapsed);
    }

    /** Monotonic action sequence used to distinguish repeated actions of one type. */
    public int actionSequence() {
        return this.entityData.get(DATA_ACTION_SEQUENCE);
    }

    /** Synchronized game time at which the current action began. */
    public long actionStartGameTime() {
        return this.entityData.get(DATA_ACTION_START);
    }

    public int chargeCooldown() {
        return this.entityData.get(DATA_CHARGE_COOLDOWN);
    }

    public int tossCooldown() {
        return this.entityData.get(DATA_TOSS_COOLDOWN);
    }

    /**
     * Starts a server action. Charge direction is locked only after its wind-up; the target is
     * captured now so a new attacker cannot redirect an attack already in progress.
     */
    public boolean startAction(Action next) {
        if (this.level().isClientSide() || next == null || this.isDeadOrDying() || this.action().isAttack()) return false;
        if (next.isAttack() && (this.recoveryTicks > 0 || !this.canAttackTarget(this.getTarget())
                || next == Action.ATTACK_CHARGE && this.chargeCooldown() > 0
                || next == Action.ATTACK_TOSS && this.tossCooldown() > 0)) return false;
        return this.startActionInternal(next, this.getTarget(), true);
    }

    private boolean startActionInternal(Action next, @Nullable LivingEntity target, boolean applyCooldown) {
        if (next == Action.IDLE && this.action() == Action.IDLE) return false;
        if (next.isAttack()) {
            if (target == null) target = this.getTarget();
            this.actionTarget = target;
            this.chargeHit = false;
            if (next == Action.ATTACK_CHARGE) {
                this.chargeDirection = Vec3.ZERO;
                if (applyCooldown) this.setChargeCooldown(CHARGE_COOLDOWN_TICKS);
            } else if (applyCooldown) {
                this.setTossCooldown(TOSS_COOLDOWN_TICKS);
            }
        } else if (next != Action.ALERT) {
            this.actionTarget = null;
        }
        this.entityData.set(DATA_ACTION, next.id());
        this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        if (next.isAttack() || next == Action.ALERT || next == Action.GRAZE) this.getNavigation().stop();
        return true;
    }

    private void finishAction() {
        Action current = this.action();
        if (current.isAttack() || current == Action.ALERT || current == Action.HURT || current == Action.GRAZE) {
            this.recoveryTicks = current.isAttack() ? ATTACK_RECOVERY_TICKS : 0;
            this.actionTarget = null;
            this.chargeDirection = Vec3.ZERO;
            this.chargeHit = false;
            this.entityData.set(DATA_ACTION, Action.IDLE.id());
            this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
            this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        }
    }

    private void setActionWithoutTarget(Action next) {
        this.actionTarget = null;
        this.entityData.set(DATA_ACTION, next.id());
        this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        this.getNavigation().stop();
    }

    private void beginGraze() {
        if (this.action() == Action.IDLE && this.getTarget() == null && !this.isDeadOrDying()) {
            this.startActionInternal(Action.GRAZE, null, false);
        }
    }

    private void chooseAttack() {
        LivingEntity target = this.getTarget();
        if (!this.canAttackTarget(target) || this.recoveryTicks > 0) return;
        double distance = this.distanceTo(target);
        if (distance <= 2.0D && this.tossCooldown() == 0 && this.hasLineOfSight(target)) {
            this.startActionInternal(Action.ATTACK_TOSS, target, true);
        } else if (distance >= 3.0D && distance <= 8.0D && this.chargeCooldown() == 0
                && this.canStartCharge(target)) {
            this.startActionInternal(Action.ATTACK_CHARGE, target, true);
        }
    }

    private void tickCooldowns() {
        if (this.chargeCooldown() > 0) this.entityData.set(DATA_CHARGE_COOLDOWN, this.chargeCooldown() - 1);
        if (this.tossCooldown() > 0) this.entityData.set(DATA_TOSS_COOLDOWN, this.tossCooldown() - 1);
        if (this.recoveryTicks > 0) this.recoveryTicks--;
    }

    private void setChargeCooldown(int ticks) {
        this.entityData.set(DATA_CHARGE_COOLDOWN, Mth.clamp(ticks, 0, CHARGE_COOLDOWN_TICKS));
    }

    private void setTossCooldown(int ticks) {
        this.entityData.set(DATA_TOSS_COOLDOWN, Mth.clamp(ticks, 0, TOSS_COOLDOWN_TICKS));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.isDeadOrDying()) return;
        this.tickCooldowns();
        this.validateTarget();
        this.entityData.set(DATA_PURSUING, this.getTarget() != null);
        Action current = this.action();
        if (current != Action.IDLE) {
            this.tickAction(current);
        } else if (this.recoveryTicks == 0) {
            this.chooseAttack();
        }
    }

    private void tickAction(Action current) {
        this.stopInPlace();
        this.setZza(0.0F);
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, velocity.y, 0.0D);
        long ticks = this.actionTicks();
        switch (current) {
            case ALERT -> {
                if (ticks >= ALERT_ACTION_TICKS) this.finishAction();
            }
            case ATTACK_CHARGE -> {
                if (ticks >= CHARGE_ACTION_END_TICKS) {
                    this.finishAction();
                } else if (ticks >= CHARGE_WINDUP_TICKS && ticks < CHARGE_WINDOW_END_TICKS) {
                    this.tickCharge();
                }
            }
            case ATTACK_TOSS -> {
                if (ticks >= TOSS_HIT_TICK && !this.chargeHit) {
                    this.chargeHit = true;
                    this.performToss();
                }
                if (ticks >= TOSS_ACTION_END_TICKS) this.finishAction();
            }
            case HURT -> {
                if (ticks >= HURT_ACTION_TICKS) this.finishAction();
            }
            case GRAZE -> {
                if (ticks >= GRAZE_ACTION_TICKS || this.getTarget() != null) this.finishAction();
            }
            case DEATH, IDLE -> {
                // Death is advanced by tickDeath and remains synchronized until removal.
            }
        }
    }

    private void tickCharge() {
        if (this.chargeHit) return;
        LivingEntity target = this.actionTarget;
        if (!this.canAttackTarget(target)) {
            this.finishAction();
            return;
        }
        if (this.chargeDirection.lengthSqr() == 0.0D) {
            this.chargeDirection = this.directionTo(target);
            this.faceDirection(this.chargeDirection);
        }
        Vec3 movement = this.chargeDirection.scale(CHARGE_STEP);
        if (!this.hasGroundAhead(this.position().add(movement))) {
            this.chargeHit = true;
            return;
        }
        Vec3 start = this.position();
        this.move(MoverType.SELF, movement);
        Vec3 end = this.position();
        // Minkowski expansion tests the actual swept body, not the diagonal broad-phase rectangle.
        AABB targetBox = target.getBoundingBox();
        double halfWidth = this.getBbWidth() * 0.5D;
        AABB contact = new AABB(targetBox.minX - halfWidth, targetBox.minY - this.getBbHeight(),
                targetBox.minZ - halfWidth, targetBox.maxX + halfWidth, targetBox.maxY,
                targetBox.maxZ + halfWidth);
        if (this.hasLineOfSight(target) && (contact.contains(start) || contact.clip(start, end).isPresent())) {
            this.chargeHit = true;
            this.applyAttack(target, CHARGE_DAMAGE, CHARGE_KNOCKBACK, 0.0D, this.chargeDirection);
        }
        if (this.horizontalCollision || end.distanceToSqr(start) < movement.lengthSqr() * 0.99D) {
            this.chargeHit = true;
        }
    }

    private void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() == 0.0D) return;
        float yaw = (float)(Mth.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    private void performToss() {
        LivingEntity target = this.actionTarget;
        if (target == null || !this.canAttackTarget(target) || this.distanceTo(target) > 2.0D
                || !this.hasLineOfSight(target)) return;
        Vec3 direction = directionTo(target);
        if (direction == Vec3.ZERO) direction = facingDirection();
        this.applyAttack(target, TOSS_DAMAGE, TOSS_KNOCKBACK, TOSS_UPWARD, direction);
    }

    private boolean applyAttack(LivingEntity target, float damage, double horizontalStrength,
                                double upward, Vec3 direction) {
        Vec3 oldMovement = target.getDeltaMovement();
        boolean hurt = target.hurt(this.damageSources().mobAttack(this), damage);
        if (!hurt) return false;
        double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double scale = Mth.clamp(1.0D - resistance, 0.0D, 1.0D);
        target.setDeltaMovement(direction.x * horizontalStrength * scale,
                oldMovement.y + upward * scale, direction.z * horizontalStrength * scale);
        target.hasImpulse = true;
        target.hurtMarked = true;
        return true;
    }

    private void validateTarget() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.lostSightTicks = 0;
            if (this.action() == Action.ALERT || (this.action().isAttack() && !this.chargeHit)) this.finishAction();
            return;
        }
        if (!this.canAttackTarget(target)) {
            this.setTarget(null);
            this.lostSightTicks = 0;
            // A completed hit still needs its recovery pose, even when that hit killed the target.
            if (this.action() == Action.ALERT || (this.action().isAttack() && !this.chargeHit)) this.finishAction();
            return;
        }
        if (this.hasLineOfSight(target)) {
            this.lostSightTicks = 0;
        } else if (++this.lostSightTicks >= SIGHT_LOSS_TICKS) {
            this.setTarget(null);
            this.lostSightTicks = 0;
            if (this.action() == Action.ALERT || (this.action().isAttack() && !this.chargeHit)) this.finishAction();
        }
    }

    private boolean canAttackTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || target.isRemoved() || !target.isAlive() || !target.isAttackable()) {
            return false;
        }
        if (target.level() != this.level() || this.isAlliedTo(target)
                || this.distanceToSqr(target) > TARGET_MAX_DISTANCE_SQR) return false;
        if (target instanceof Player player
                && (player.isSpectator() || player.isCreative() || this.level().getDifficulty() == Difficulty.PEACEFUL)) {
            return false;
        }
        return true;
    }

    private boolean canStartCharge(LivingEntity target) {
        if (!this.canAttackTarget(target) || !this.onGround() || !this.hasLineOfSight(target)) return false;
        Vec3 direction = this.directionTo(target);
        double distance = Math.min(6.0D, this.distanceTo(target));
        for (double step = 0.3D; step <= distance; step += 0.3D) {
            Vec3 offset = direction.scale(step);
            if (!this.level().noCollision(this, this.getBoundingBox().move(offset))
                    || !this.hasGroundAhead(this.position().add(offset))) return false;
        }
        return true;
    }

    private boolean hasGroundAhead(Vec3 position) {
        double radius = this.getBbWidth() * 0.45D;
        for (double x : new double[]{-radius, radius}) {
            for (double z : new double[]{-radius, radius}) {
                BlockPos below = BlockPos.containing(position.x + x, position.y - 0.15D, position.z + z);
                if (!this.level().loadedAndEntityCanStandOn(below, this)) return false;
            }
        }
        return true;
    }

    private Vec3 directionTo(@Nullable Entity target) {
        if (target == null) return Vec3.ZERO;
        Vec3 delta = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        return horizontal.lengthSqr() < 1.0E-8D ? Vec3.ZERO : horizontal.normalize();
    }

    private Vec3 facingDirection() {
        float radians = this.getYRot() * ((float)Math.PI / 180.0F);
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (!accepted || this.level().isClientSide() || this.isDeadOrDying()) return accepted;
        LivingEntity attacker = resolveAttacker(source);
        boolean newlyProvoked = this.getTarget() == null;
        if (this.canAttackTarget(attacker)) {
            this.setTarget(attacker);
            this.lostSightTicks = 0;
            if (!this.action().isAttack() && this.action() != Action.DEATH) {
                this.setActionWithoutTarget(newlyProvoked ? Action.ALERT : Action.HURT);
            }
        } else if (!this.action().isAttack() && this.action() != Action.ALERT) {
            this.setActionWithoutTarget(Action.HURT);
        }
        return accepted;
    }

    @Nullable
    private LivingEntity resolveAttacker(net.minecraft.world.damagesource.DamageSource source) {
        Entity sourceEntity = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        if (sourceEntity instanceof LivingEntity living) return living;
        return direct instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (this.dead && !wasDead) {
            this.setTarget(null);
            this.actionTarget = null;
            this.recoveryTicks = 0;
            this.chargeDirection = Vec3.ZERO;
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
            this.entityData.set(DATA_ACTION, Action.DEATH.id());
            this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
            this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.setDeltaMovement(Vec3.ZERO);
        if (this.deathTime >= DEATH_REMOVE_TICK && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("WildBoarChargeCooldown", this.chargeCooldown());
        tag.putInt("WildBoarTossCooldown", this.tossCooldown());
        tag.putInt("WildBoarRecoveryTicks", this.recoveryTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTarget(null);
        this.actionTarget = null;
        this.chargeDirection = Vec3.ZERO;
        this.chargeHit = false;
        this.lostSightTicks = 0;
        this.recoveryTicks = 0;
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
        this.entityData.set(DATA_ACTION, Action.IDLE.id());
        this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        this.setChargeCooldown(clampCooldown(tag.getInt("WildBoarChargeCooldown"), CHARGE_COOLDOWN_TICKS));
        this.setTossCooldown(clampCooldown(tag.getInt("WildBoarTossCooldown"), TOSS_COOLDOWN_TICKS));
        this.recoveryTicks = clampCooldown(tag.getInt("WildBoarRecoveryTicks"), ATTACK_RECOVERY_TICKS);
        this.entityData.set(DATA_PURSUING, false);
        if (this.getHealth() <= 0.0F) {
            this.entityData.set(DATA_ACTION, Action.DEATH.id());
            this.entityData.set(DATA_ACTION_START, this.level().getGameTime() - this.deathTime);
        }
    }

    private static int clampCooldown(int value, int maximum) {
        return Mth.clamp(value, 0, maximum);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 1 + this.random.nextInt(3);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new WildBoarAnimationController<>(this, this::animationState,
                this::action, this::actionTicks, this::actionSequence));
    }

    private PlayState animationState(AnimationState<WildBoarEntity> state) {
        return switch (this.action()) {
            case GRAZE -> state.setAndContinue(RawAnimation.begin().thenLoop("graze"));
            case ALERT -> state.setAndContinue(RawAnimation.begin().thenPlay("alert"));
            case ATTACK_CHARGE -> state.setAndContinue(RawAnimation.begin().thenPlay("attack_charge"));
            case ATTACK_TOSS -> state.setAndContinue(RawAnimation.begin().thenPlay("attack_toss"));
            case HURT -> state.setAndContinue(RawAnimation.begin().thenPlay("hurt"));
            case DEATH -> state.setAndContinue(RawAnimation.begin().thenPlayAndHold("death"));
            case IDLE -> {
                if (!state.isMoving()) yield state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
                yield state.setAndContinue(this.entityData.get(DATA_PURSUING)
                        ? RawAnimation.begin().thenLoop("run")
                        : RawAnimation.begin().thenLoop("walk"));
            }
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public enum Action {
        IDLE("idle", true, false),
        GRAZE("graze", true, false),
        ALERT("alert", false, false),
        ATTACK_CHARGE("attack_charge", false, true),
        ATTACK_TOSS("attack_toss", false, true),
        HURT("hurt", false, false),
        DEATH("death", false, false);

        private final String animation;
        private final boolean loop;
        private final boolean attack;

        Action(String animation, boolean loop, boolean attack) {
            this.animation = animation;
            this.loop = loop;
            this.attack = attack;
        }

        public String animation() { return this.animation; }
        public boolean loops() { return this.loop; }
        public boolean isAttack() { return this.attack; }
        private byte id() { return (byte)this.ordinal(); }

        private static Action fromId(byte id) {
            Action[] actions = values();
            return id >= 0 && id < actions.length ? actions[id] : IDLE;
        }
    }

    private static final class CombatGoal extends Goal {
        private final WildBoarEntity boar;

        private CombatGoal(WildBoarEntity boar) {
            this.boar = boar;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.boar.getTarget() != null || !this.boar.action().loops();
        }

        @Override
        public boolean requiresUpdateEveryTick() {return true;}

        @Override
        public void tick() {
            LivingEntity target = this.boar.getTarget();
            if (this.boar.action() == Action.IDLE && target != null) {
                this.boar.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.boar.getNavigation().moveTo(target, 1.4D);
            } else {
                this.boar.getNavigation().stop();
                if (target != null && (this.boar.action() != Action.ATTACK_CHARGE
                        || this.boar.actionTicks() < CHARGE_WINDUP_TICKS)) {
                    this.boar.faceDirection(this.boar.directionTo(target));
                }
            }
        }
    }

    private static final class GrazeGoal extends Goal {
        private final WildBoarEntity boar;

        private GrazeGoal(WildBoarEntity boar) {
            this.boar = boar;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.boar.action() == Action.GRAZE && this.boar.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && this.boar.actionTicks() < GRAZE_ACTION_TICKS;
        }

        @Override
        public void tick() {
            this.boar.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (this.boar.action() == Action.GRAZE) this.boar.finishAction();
        }
    }

    private static final class WanderGoal extends WaterAvoidingRandomStrollGoal {
        private final WildBoarEntity boar;

        private WanderGoal(WildBoarEntity boar) {
            super(boar, 1.0D);
            this.boar = boar;
        }

        @Override
        public boolean canUse() {
            return this.boar.action() == Action.IDLE && this.boar.getTarget() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.boar.action() == Action.IDLE && this.boar.getTarget() == null
                    && super.canContinueToUse();
        }

        @Override
        public void stop() {
            boolean completed = !this.boar.getNavigation().isInProgress();
            super.stop();
            if (completed && this.boar.action() == Action.IDLE && this.boar.getTarget() == null
                    && this.boar.random.nextDouble() < GRAZE_CHANCE) {
                this.boar.beginGraze();
            }
        }
    }
}
