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
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A wild boar Gu [野生豕蛊] that wanders freely instead of seeking players.
 *
 * <p>The shared {@link RestingFlyingGuEntity} lifecycle supplies the three movement goals and the
 * synchronized flight phase. This entity keeps its existing GeckoLib animation names and transition
 * triggers: {@code takeoff} plays once before the phase handler loops {@code fly}, while {@code land}
 * plays once as the phase handler enters {@code RESTING}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class BoarGuEntity extends RestingFlyingGuEntity implements GeoEntity {

    private static final RawAnimation FLY_ANIM =
            RawAnimation.begin().thenLoop("animation.boar_gu.fly");
    private static final RawAnimation LAND_ANIM =
            RawAnimation.begin().thenPlay("animation.boar_gu.land");
    private static final RawAnimation TAKEOFF_ANIM =
            RawAnimation.begin().thenPlay("animation.boar_gu.takeoff");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BoarGuEntity(EntityType<? extends BoarGuEntity> type, Level level, Supplier<Item> caughtGu) {
        super(type, level, caughtGu);
    }

    @Override
    public boolean seeks(Player player) {return false;}

    @Override
    protected void playLandingAnimation() {triggerAnim("main", "land");}

    @Override
    protected void playTakeoffAnimation() {triggerAnim("main", "takeoff");}

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            if (phase() == FlightPhase.FLYING || phase() == FlightPhase.LANDING) {
                return state.setAndContinue(FLY_ANIM);
            }
            return PlayState.STOP;
        }).triggerableAnim("land", LAND_ANIM).triggerableAnim("takeoff", TAKEOFF_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {return cache;}
}
