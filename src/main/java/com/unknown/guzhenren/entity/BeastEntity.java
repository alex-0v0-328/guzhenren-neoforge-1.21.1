package com.unknown.guzhenren.entity;

import java.util.EnumSet;
import org.jetbrains.annotations.Nullable;
import com.unknown.guzhenren.registry.entity.ModEntityTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;

/**
 * Server-authoritative action state machine shared by the big beast entities (bears, tigers).
 *
 * <p>Mirrors {@link WildBoarEntity}'s contract: every pose is a synchronized {@link Action} with a
 * server game-time start and a monotonic sequence, so late trackers replay one-shot actions from
 * their first frame. Damage lands on animation hit frames, never on contact. Subclasses supply the
 * timing constants, the attack selection and the heavy-attack physics; this base supplies target
 * validation, directional hurt poses, the roar-on-first-lock rule, the night-sleep / daytime-rest
 * ambient chain, death timing and persistence.
 */
public abstract class BeastEntity extends PathfinderMob implements GeoEntity {

    public static final int SWIPE_COOLDOWN_TICKS = 20;
    public static final int HEAVY_COOLDOWN_TICKS = 80;
    public static final int ATTACK_RECOVERY_TICKS = 10;
    public static final int HURT_ACTION_TICKS = 11;
    public static final int SIGHT_LOSS_TICKS = 100;
    public static final int SIT_ACTION_TICKS = 160;
    public static final int LIE_ACTION_TICKS = 80;
    public static final int ROLL_ACTION_TICKS = 74;
    public static final int BACK_SCRATCH_ACTION_TICKS = 100;
    public static final double SWIPE_REACH = 2.5D;
    private static final double AMBIENT_REST_CHANCE = 0.35D;
    private static final double NIGHT_LIE_DOWN_CHANCE = 0.5D;

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ACTION_START = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIPE_COOLDOWN = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAVY_COOLDOWN = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PURSUING = SynchedEntityData.defineId(
            BeastEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    protected LivingEntity actionTarget;
    protected boolean heavyHit;
    private int lostSightTicks;
    private int recoveryTicks;
    private long ambientEndGameTime = Long.MIN_VALUE;

    protected BeastEntity(net.minecraft.world.entity.EntityType<? extends BeastEntity> type, Level level) {
        super(type, level);
    }

    /** Tick at which the swipe's hit frame lands. */
    protected abstract int swipeHitTick();
    /** Tick at which the swipe action ends and the pose returns to idle. */
    protected abstract int swipeActionTicks();
    /** Tick at which the heavy attack's hit frame lands (or its leap window opens). */
    protected abstract int heavyHitTick();
    /** Tick at which the heavy attack action ends. */
    protected abstract int heavyActionTicks();
    /** Length of the first-lock roar. */
    protected abstract int roarActionTicks();
    /** Length of the lie-down transition into lie/sleep. */
    protected abstract int lieDownActionTicks();
    /** Length of the get-up transition back to idle. */
    protected abstract int getUpActionTicks();
    /** Death-tick at which the corpse is removed (covers the full death animation). */
    protected abstract int deathRemoveTick();
    /** Flat swipe damage, before armor. */
    protected abstract float swipeDamage();
    /** Flat heavy-attack damage, before armor. */
    protected abstract float heavyDamage();
    /** Whether this beast seeks targets on its own (true) or only retaliates (false). */
    protected abstract boolean huntsActively();
    /** Movement-speed multiplier applied to the pursuit navigation. */
    protected abstract double pursuitSpeed();
    /** Selects and starts an attack against the current target; called while idle. */
    protected abstract void chooseAttack(LivingEntity target);
    /** Advances the heavy attack; hit frames and movement physics live here. */
    protected abstract void tickHeavyAttack(long ticks);
    /** Whether the heavy attack keeps its horizontal momentum at this tick (pounce flight). */
    protected boolean heavyKeepsMomentum(long ticks) {
        return false;
    }
    /** Daytime ambient actions this beast may pick, with relative weights. */
    protected abstract Action pickDaytimeAmbient(double roll);
    protected abstract SoundEvent roarSound();
    protected abstract float roarPitch();
    /** Extra height added to the render culling box so rearing poses are not clipped. */
    protected abstract double cullExtraHeight();
    /** Symmetric horizontal inflation of the render culling box (tail sweep, rearing width). */
    protected abstract double cullHorizontalInflate();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal(this));
        // Registered unconditionally: registerGoals runs inside the Mob constructor, before subclass
        // fields like the bear species are assigned, so the temperament check must happen at tick time.
        this.goalSelector.addGoal(2, new HuntGoal(this));
        this.goalSelector.addGoal(4, new RestGoal(this));
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
        builder.define(DATA_SWIPE_COOLDOWN, 0);
        builder.define(DATA_HEAVY_COOLDOWN, 0);
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

    public int swipeCooldown() {
        return this.entityData.get(DATA_SWIPE_COOLDOWN);
    }

    public int heavyCooldown() {
        return this.entityData.get(DATA_HEAVY_COOLDOWN);
    }

    public boolean pursuing() {
        return this.entityData.get(DATA_PURSUING);
    }

    /**
     * Starts a server action. Attacks refuse to start while another attack is in progress or under
     * cooldown; the target is captured now so a new attacker cannot redirect an in-progress attack.
     */
    public boolean startAction(Action next) {
        if (this.level().isClientSide() || next == null || this.isDeadOrDying() || this.action().isAttack()) {
            return false;
        }
        if (next.isAttack() && (this.recoveryTicks > 0 || !this.canAttackTarget(this.getTarget())
                || next == Action.ATTACK_SWIPE && this.swipeCooldown() > 0
                || next == Action.ATTACK_HEAVY && this.heavyCooldown() > 0)) return false;
        return this.startActionInternal(next, true);
    }

    private boolean startActionInternal(Action next, boolean applyCooldown) {
        if (next == Action.IDLE && this.action() == Action.IDLE) return false;
        if (next.isAttack()) {
            LivingEntity target = this.getTarget();
            this.actionTarget = target;
            this.heavyHit = false;
            if (target != null) this.faceDirection(this.directionTo(target));
            if (applyCooldown) {
                if (next == Action.ATTACK_SWIPE) this.setSwipeCooldown(SWIPE_COOLDOWN_TICKS);
                else this.setHeavyCooldown(HEAVY_COOLDOWN_TICKS);
            }
        } else if (next != Action.ROAR) {
            this.actionTarget = null;
        }
        if (next == Action.ROAR) this.playSound(this.roarSound(), 1.0F, this.roarPitch());
        this.ambientEndGameTime = next == Action.SIT
                ? this.level().getGameTime() + SIT_ACTION_TICKS : Long.MIN_VALUE;
        this.entityData.set(DATA_ACTION, next.id());
        this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        this.getNavigation().stop();
        return true;
    }

    protected void finishAction() {
        Action current = this.action();
        if (current != Action.IDLE && current != Action.DEATH) {
            this.recoveryTicks = current.isAttack() ? ATTACK_RECOVERY_TICKS : 0;
            this.actionTarget = null;
            this.ambientEndGameTime = Long.MIN_VALUE;
            this.entityData.set(DATA_ACTION, Action.IDLE.id());
            this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
            this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        }
    }

    private void setActionWithoutTarget(Action next) {
        this.actionTarget = null;
        this.ambientEndGameTime = Long.MIN_VALUE;
        this.entityData.set(DATA_ACTION, next.id());
        this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        this.getNavigation().stop();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.isDeadOrDying()) return;
        this.tickCooldowns();
        this.validateTarget();
        this.entityData.set(DATA_PURSUING, this.getTarget() != null);
        Action current = this.action();
        if (this.getTarget() != null && current.isAmbient()) {
            // Being engaged snaps the beast out of any rest pose without a get-up transition.
            this.finishAction();
            current = this.action();
        }
        if (current != Action.IDLE) {
            this.tickAction(current);
        } else if (this.recoveryTicks == 0 && this.getTarget() != null) {
            LivingEntity target = this.getTarget();
            if (this.canAttackTarget(target)) this.chooseAttack(target);
        }
    }

    private void tickAction(Action current) {
        long ticks = this.actionTicks();
        if (!this.heavyKeepsMomentum(ticks)) {
            this.stopInPlace();
            this.setZza(0.0F);
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, velocity.y, 0.0D);
        }
        switch (current) {
            case ROAR -> {
                if (ticks >= this.roarActionTicks()) this.finishAction();
            }
            case ATTACK_SWIPE -> {
                if (ticks >= this.swipeHitTick() && !this.heavyHit) {
                    this.heavyHit = true;
                    this.performSwipe();
                }
                if (ticks >= this.swipeActionTicks()) this.finishAction();
            }
            case ATTACK_HEAVY -> this.tickHeavyAttack(ticks);
            case HURT_LEFT, HURT_RIGHT -> {
                if (ticks >= HURT_ACTION_TICKS) this.finishAction();
            }
            case SIT -> {
                if (this.level().getGameTime() >= this.ambientEndGameTime) this.finishAction();
            }
            case LIE_DOWN -> {
                if (ticks >= this.lieDownActionTicks()) {
                    this.setActionWithoutTarget(Action.LIE);
                    this.ambientEndGameTime = this.level().getGameTime() + LIE_ACTION_TICKS;
                }
            }
            case LIE -> {
                if (!this.isNightTime()) this.startActionInternal(Action.GET_UP, false);
                else if (this.level().getGameTime() >= this.ambientEndGameTime) {
                    this.setActionWithoutTarget(Action.SLEEP);
                }
            }
            case SLEEP -> {
                if (!this.isNightTime()) this.startActionInternal(Action.GET_UP, false);
            }
            case GET_UP -> {
                if (ticks >= this.getUpActionTicks()) this.finishAction();
            }
            case ROLL -> {
                if (ticks >= ROLL_ACTION_TICKS) this.finishAction();
            }
            case BACK_SCRATCH -> {
                if (ticks >= BACK_SCRATCH_ACTION_TICKS) this.finishAction();
            }
            case DEATH, IDLE -> {
                // Death is advanced by tickDeath and remains synchronized until removal.
            }
        }
    }

    private void performSwipe() {
        LivingEntity target = this.actionTarget;
        if (target == null || !this.canAttackTarget(target) || this.distanceTo(target) > SWIPE_REACH
                || !this.hasLineOfSight(target)) return;
        this.applyAttack(target, this.swipeDamage(), this.swipeKnockback(), this.swipeUpward(),
                this.directionOrFacing(target));
    }

    /** Horizontal knockback strength of the swipe. */
    protected abstract double swipeKnockback();
    /** Upward knockback component of the swipe. */
    protected abstract double swipeUpward();

    /** Shared swept-body hit test for lunging attacks (tiger pounce). */
    protected boolean sweptHit(LivingEntity target, Vec3 from, Vec3 to) {
        AABB targetBox = target.getBoundingBox();
        double halfWidth = this.getBbWidth() * 0.5D;
        AABB contact = new AABB(targetBox.minX - halfWidth, targetBox.minY - this.getBbHeight(),
                targetBox.minZ - halfWidth, targetBox.maxX + halfWidth, targetBox.maxY,
                targetBox.maxZ + halfWidth);
        return this.hasLineOfSight(target) && (contact.contains(from) || contact.contains(to)
                || contact.clip(from, to).isPresent());
    }

    protected boolean applyAttack(LivingEntity target, float damage, double horizontalStrength,
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

    private void tickCooldowns() {
        if (this.swipeCooldown() > 0) this.entityData.set(DATA_SWIPE_COOLDOWN, this.swipeCooldown() - 1);
        if (this.heavyCooldown() > 0) this.entityData.set(DATA_HEAVY_COOLDOWN, this.heavyCooldown() - 1);
        if (this.recoveryTicks > 0) this.recoveryTicks--;
    }

    private void setSwipeCooldown(int ticks) {
        this.entityData.set(DATA_SWIPE_COOLDOWN, Mth.clamp(ticks, 0, SWIPE_COOLDOWN_TICKS));
    }

    private void setHeavyCooldown(int ticks) {
        this.entityData.set(DATA_HEAVY_COOLDOWN, Mth.clamp(ticks, 0, HEAVY_COOLDOWN_TICKS));
    }

    private void validateTarget() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.lostSightTicks = 0;
            if (this.action() == Action.ROAR || (this.action().isAttack() && !this.heavyHit)) this.finishAction();
            return;
        }
        if (!this.canAttackTarget(target)) {
            this.setTarget(null);
            this.lostSightTicks = 0;
            // A completed hit still needs its recovery pose, even when that hit killed the target.
            if (this.action() == Action.ROAR || (this.action().isAttack() && !this.heavyHit)) this.finishAction();
            return;
        }
        if (this.hasLineOfSight(target)) {
            this.lostSightTicks = 0;
        } else if (++this.lostSightTicks >= SIGHT_LOSS_TICKS) {
            this.setTarget(null);
            this.lostSightTicks = 0;
            if (this.action() == Action.ROAR || (this.action().isAttack() && !this.heavyHit)) this.finishAction();
        }
    }

    protected boolean canAttackTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || target.isRemoved() || !target.isAlive() || !target.isAttackable()) {
            return false;
        }
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (target.level() != this.level() || this.isAlliedTo(target)
                || this.distanceToSqr(target) > range * range) return false;
        if (target instanceof Player player
                && (player.isSpectator() || player.isCreative() || this.level().getDifficulty() == Difficulty.PEACEFUL)) {
            return false;
        }
        return true;
    }

    /** Collision-and-ground scan ahead of a lunging attack; refuses walls and ledges. */
    protected boolean canStartLunge(LivingEntity target, double maxDistance) {
        if (!this.canAttackTarget(target) || !this.onGround() || !this.hasLineOfSight(target)) return false;
        Vec3 direction = this.directionTo(target);
        double distance = Math.min(maxDistance, this.distanceTo(target));
        for (double step = 0.3D; step <= distance; step += 0.3D) {
            Vec3 offset = direction.scale(step);
            if (!this.level().noCollision(this, this.getBoundingBox().move(offset))
                    || !this.hasGroundAhead(this.position().add(offset))) return false;
        }
        return true;
    }

    protected boolean hasGroundAhead(Vec3 position) {
        double radius = this.getBbWidth() * 0.45D;
        for (double x : new double[]{-radius, radius}) {
            for (double z : new double[]{-radius, radius}) {
                net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(
                        position.x + x, position.y - 0.15D, position.z + z);
                if (!this.level().loadedAndEntityCanStandOn(below, this)) return false;
            }
        }
        return true;
    }

    protected Vec3 directionTo(@Nullable Entity target) {
        if (target == null) return Vec3.ZERO;
        Vec3 delta = target.position().subtract(this.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        return horizontal.lengthSqr() < 1.0E-8D ? Vec3.ZERO : horizontal.normalize();
    }

    protected Vec3 directionOrFacing(@Nullable Entity target) {
        Vec3 direction = this.directionTo(target);
        if (direction.lengthSqr() > 0.0D) return direction;
        float radians = this.getYRot() * ((float)Math.PI / 180.0F);
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    protected void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() == 0.0D) return;
        float yaw = (float)(Mth.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    private boolean isNightTime() {
        long dayTime = this.level().getDayTime() % 24000L;
        return dayTime >= 13000L && dayTime <= 23000L;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        // First lock of an engagement is announced by the roar; it never interrupts a running action.
        if (target != null && previous == null && !this.level().isClientSide() && !this.isDeadOrDying()
                && (this.action() == Action.IDLE || this.action().isAmbient())) {
            this.setActionWithoutTarget(Action.ROAR);
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (!accepted || this.level().isClientSide() || this.isDeadOrDying()) return accepted;
        LivingEntity attacker = resolveAttacker(source);
        if (this.canAttackTarget(attacker)) {
            this.setTarget(attacker);
            this.lostSightTicks = 0;
        }
        Action current = this.action();
        // A running hurt pose is never restarted: it outlasts vanilla's 10-tick damage window, so
        // restarting it would let steady hits stun-lock the beast out of every attack.
        if (!current.isAttack() && current != Action.DEATH && current != Action.ROAR
                && current != Action.HURT_LEFT && current != Action.HURT_RIGHT) {
            this.setActionWithoutTarget(this.directionalHurt(attacker));
        }
        return accepted;
    }

    /**
     * Picks the hurt pose from the attacker's bearing: an attacker on this beast's left side knocks
     * the head to the beast's right, playing {@code hurt_right}, and vice versa.
     */
    private Action directionalHurt(@Nullable Entity attacker) {
        if (attacker == null) return Action.HURT_LEFT;
        double dx = attacker.getX() - this.getX();
        double dz = attacker.getZ() - this.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) return Action.HURT_LEFT;
        double radians = this.getYRot() * (Math.PI / 180.0D);
        double leftness = dx * Math.cos(radians) + dz * Math.sin(radians);
        return leftness > 0.0D ? Action.HURT_RIGHT : Action.HURT_LEFT;
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
        if (this.deathTime >= this.deathRemoveTick() && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BeastSwipeCooldown", this.swipeCooldown());
        tag.putInt("BeastHeavyCooldown", this.heavyCooldown());
        tag.putInt("BeastRecoveryTicks", this.recoveryTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTarget(null);
        this.actionTarget = null;
        this.heavyHit = false;
        this.lostSightTicks = 0;
        this.recoveryTicks = 0;
        this.ambientEndGameTime = Long.MIN_VALUE;
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
        this.entityData.set(DATA_ACTION, Action.IDLE.id());
        this.entityData.set(DATA_ACTION_START, this.level().getGameTime());
        this.entityData.set(DATA_ACTION_SEQUENCE, this.actionSequence() + 1);
        this.setSwipeCooldown(Mth.clamp(tag.getInt("BeastSwipeCooldown"), 0, SWIPE_COOLDOWN_TICKS));
        this.setHeavyCooldown(Mth.clamp(tag.getInt("BeastHeavyCooldown"), 0, HEAVY_COOLDOWN_TICKS));
        this.recoveryTicks = Mth.clamp(tag.getInt("BeastRecoveryTicks"), 0, ATTACK_RECOVERY_TICKS);
        this.entityData.set(DATA_PURSUING, false);
        if (this.getHealth() <= 0.0F) {
            this.entityData.set(DATA_ACTION, Action.DEATH.id());
            this.entityData.set(DATA_ACTION_START, this.level().getGameTime() - this.deathTime);
        }
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
    public AABB getBoundingBoxForCulling() {
        AABB box = super.getBoundingBoxForCulling().inflate(this.cullHorizontalInflate());
        return box.setMaxY(box.maxY + this.cullExtraHeight());
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState block) {
        this.playSound(this.stepSound(), 0.15F, 1.0F);
    }

    protected abstract SoundEvent stepSound();

    /** Picks and starts an ambient action after a completed wander; nothing happens on a miss. */
    private void rollAmbient() {
        if (this.action() != Action.IDLE || this.getTarget() != null || this.isDeadOrDying() || !this.onGround()) {
            return;
        }
        if (this.isNightTime()) {
            if (this.random.nextDouble() < NIGHT_LIE_DOWN_CHANCE) this.startActionInternal(Action.LIE_DOWN, false);
            return;
        }
        double roll = this.random.nextDouble();
        if (roll >= AMBIENT_REST_CHANCE) return;
        Action pick = this.pickDaytimeAmbient(roll / AMBIENT_REST_CHANCE);
        if (pick == null) return;
        this.startActionInternal(pick, false);
    }

    private static final class CombatGoal extends Goal {
        private final BeastEntity beast;

        private CombatGoal(BeastEntity beast) {
            this.beast = beast;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.beast.getTarget() != null || !this.beast.action().loops();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.beast.getTarget();
            if (this.beast.action() == Action.IDLE && target != null) {
                this.beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.beast.getNavigation().moveTo(target, this.beast.pursuitSpeed());
            } else {
                this.beast.getNavigation().stop();
                if (target != null && this.beast.action() == Action.ROAR) {
                    this.beast.faceDirection(this.beast.directionTo(target));
                }
            }
        }
    }

    /** Active target search for hunting species: the nearest valid player or prey animal. */
    private static final class HuntGoal extends Goal {
        private static final int SCAN_INTERVAL_TICKS = 10;
        private final BeastEntity beast;

        private HuntGoal(BeastEntity beast) {
            this.beast = beast;
        }

        @Override
        public boolean canUse() {
            // Resting but awake beasts still notice prey, including the lie-down transition; only
            // actual sleep (and get-up) skips hunting.
            Action action = this.beast.action();
            return this.beast.huntsActively() && this.beast.getTarget() == null
                    && (action == Action.IDLE || action == Action.SIT || action == Action.LIE
                            || action == Action.LIE_DOWN);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.beast.tickCount % SCAN_INTERVAL_TICKS != 0) return;
            double range = this.beast.getAttributeValue(Attributes.FOLLOW_RANGE);
            AABB box = this.beast.getBoundingBox().inflate(range);
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : this.beast.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> (entity instanceof Player || entity.getType().is(ModEntityTypeTags.PREDATOR_PREY))
                            && this.beast.canAttackTarget(entity) && this.beast.hasLineOfSight(entity))) {
                double distance = this.beast.distanceToSqr(candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
            if (best != null) this.beast.setTarget(best);
        }
    }

    /** Holds navigation still while the beast sits, lies, sleeps, rolls or scratches. */
    private static final class RestGoal extends Goal {
        private final BeastEntity beast;

        private RestGoal(BeastEntity beast) {
            this.beast = beast;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.beast.action().isAmbient() && this.beast.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            this.beast.getNavigation().stop();
        }
    }

    private static final class WanderGoal extends WaterAvoidingRandomStrollGoal {
        private final BeastEntity beast;

        private WanderGoal(BeastEntity beast) {
            super(beast, 1.0D);
            this.beast = beast;
        }

        @Override
        public boolean canUse() {
            return this.beast.action() == Action.IDLE && this.beast.getTarget() == null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.beast.action() == Action.IDLE && this.beast.getTarget() == null
                    && super.canContinueToUse();
        }

        @Override
        public void stop() {
            boolean completed = !this.beast.getNavigation().isInProgress();
            super.stop();
            if (completed) this.beast.rollAmbient();
        }
    }

    /** Synchronized poses. The ordinal is the wire id; subclasses map the poses onto their animation set. */
    public enum Action {
        IDLE(true, false, false),
        SIT(true, false, true),
        LIE_DOWN(false, false, true),
        LIE(true, false, true),
        SLEEP(true, false, true),
        GET_UP(false, false, true),
        ROLL(false, false, true),
        BACK_SCRATCH(false, false, true),
        ROAR(false, false, false),
        ATTACK_SWIPE(false, true, false),
        ATTACK_HEAVY(false, true, false),
        HURT_LEFT(false, false, false),
        HURT_RIGHT(false, false, false),
        DEATH(false, false, false);

        private final boolean loop;
        private final boolean attack;
        private final boolean ambient;

        Action(boolean loop, boolean attack, boolean ambient) {
            this.loop = loop;
            this.attack = attack;
            this.ambient = ambient;
        }

        public boolean loops() { return this.loop; }
        public boolean isAttack() { return this.attack; }
        /** Rest poses (sit/lie/sleep/roll/scratch) that combat engagement interrupts. */
        public boolean isAmbient() { return this.ambient; }
        private byte id() { return (byte)this.ordinal(); }

        private static Action fromId(byte id) {
            Action[] actions = values();
            return id >= 0 && id < actions.length ? actions[id] : IDLE;
        }
    }
}
