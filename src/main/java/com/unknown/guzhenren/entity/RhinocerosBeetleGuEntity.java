package com.unknown.guzhenren.entity;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A wild Rhinoceros Beetle Gu [甲虫蛊] that shares the boar Gu flight and rest behavior.
 *
 * <p>All four registered variants use this entity class. The registration supplies the caught item,
 * dimensions and attributes; the renderer supplies the variant texture. The model's fixed animation
 * contract is {@code animation.idle} while resting, {@code animation.lift} for 0.6 seconds followed
 * by looping {@code animation.fly}, and {@code animation.land} for 0.6 seconds followed by looping
 * {@code animation.idle}. The exported {@code animation.walk} is intentionally never selected.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class RhinocerosBeetleGuEntity extends RestingFlyingGuEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation LIFT_ANIM =
            RawAnimation.begin().thenPlay("animation.lift");
    private static final RawAnimation LAND_ANIM =
            RawAnimation.begin().thenPlay("animation.land");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RhinocerosBeetleGuEntity(EntityType<? extends RhinocerosBeetleGuEntity> type, Level level,
                                    Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    public boolean seeks(Player player) {return false;}

    @Override
    protected void playLandingAnimation() {triggerAnim("main", "land");}

    @Override
    protected void playTakeoffAnimation() {triggerAnim("main", "lift");}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> switch (phase()) {
            case FLYING, LANDING -> state.setAndContinue(FLY_ANIM);
            case RESTING -> state.setAndContinue(IDLE_ANIM);
        }).triggerableAnim("lift", LIFT_ANIM).triggerableAnim("land", LAND_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
