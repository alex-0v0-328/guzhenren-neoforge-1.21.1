package com.unknown.guzhenren.entity;

import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/** Maps synchronized boar actions onto GeckoLib 4's transition and playback clocks. */
final class WildBoarAnimationController<T extends GeoAnimatable> extends AnimationController<T> {

    private final Supplier<WildBoarEntity.Action> action;
    private final LongSupplier elapsedTicks;
    private final IntSupplier sequence;
    private int lastSequence = Integer.MIN_VALUE;
    private double partialTick;

    WildBoarAnimationController(T animatable, AnimationStateHandler<T> handler,
                               Supplier<WildBoarEntity.Action> action, LongSupplier elapsedTicks,
                               IntSupplier sequence) {
        super(animatable, "main", 3, handler);
        this.action = action;
        this.elapsedTicks = elapsedTicks;
        this.sequence = sequence;
    }

    @Override
    public void process(GeoModel<T> model, AnimationState<T> state, Map<String, GeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double tick, boolean crashIfBoneMissing) {
        if (this.lastSequence != this.sequence.getAsInt()) {
            this.lastSequence = this.sequence.getAsInt();
            this.forceAnimationReset();
            this.lastPollTime = Double.NEGATIVE_INFINITY;
        }
        this.partialTick = state.getPartialTick();
        this.transitionLength(this.action.get().loops() ? 3 : 0);
        super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        if (!this.action.get().loops() && this.getAnimationState() == State.TRANSITIONING
                && this.getCurrentAnimation() != null) {
            // GeckoLib polls at transition tick zero. Initialize then seek in the same frame,
            // otherwise a late tracker either has no pose or briefly displays the first frame.
            this.boneAnimationQueues.clear();
            super.process(model, state, bones, snapshots, tick, crashIfBoneMissing);
        }
    }

    @Override
    protected double adjustTick(double tick) {
        double normalTick = super.adjustTick(tick);
        if (this.action.get().loops()) return normalTick;
        return this.getAnimationState() == State.TRANSITIONING
                ? 0.0D : this.elapsedTicks.getAsLong() + this.partialTick;
    }
}
