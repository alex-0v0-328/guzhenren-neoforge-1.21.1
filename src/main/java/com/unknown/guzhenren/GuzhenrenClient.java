package com.unknown.guzhenren;

import com.unknown.guzhenren.client.dimension.TreasureYellowHeavenEffects;
import com.unknown.guzhenren.client.fluid.SpiritSpringClientExtensions;
import com.unknown.guzhenren.client.icon.GradedEffectIcon;
import com.unknown.guzhenren.client.icon.ItemEffectIcon;
import com.unknown.guzhenren.registry.effect.ModEffects;
import com.unknown.guzhenren.registry.fluid.ModFluidTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client-only entry point, so that nothing which would crash a dedicated server sits in {@link Guzhenren}.
 *
 * <p>Annotated {@code @Mod(dist = Dist.CLIENT)}. It registers every MobEffect's client extension --
 * the effect icons -- plus the Spirit Spring's fluid rendering and the Treasure Yellow Heaven
 * dimension special effects on the mod bus; all other client-side event subscribers live under the
 * {@code client/} package tree and are loaded only on the client.
 *
 * @author Alex
 * @version 1.0.0
 * @see Guzhenren
 * @since 1.0.0
 */

@Mod(value = Guzhenren.MOD_ID, dist = Dist.CLIENT)
public class GuzhenrenClient {

    public GuzhenrenClient(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(GuzhenrenClient::onRegisterClientExtensions);
        modEventBus.addListener(GuzhenrenClient::onRegisterDimensionSpecialEffects);
    }
    private static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Guzhenren.id("treasure_yellow_heaven"), new TreasureYellowHeavenEffects());
    }
    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(GradedEffectIcon.mobEffect("essence_qi", 1, 5), ModEffects.ESSENCE_QI);
        event.registerMobEffect(GradedEffectIcon.mobEffect("life_qi", 1, 5), ModEffects.LIFE_QI);
        event.registerMobEffect(GradedEffectIcon.mobEffect("strength_qi", 1, 5), ModEffects.STRENGTH_QI);
        event.registerMobEffect(GradedEffectIcon.mobEffect("all_out_effort", 3, 5), ModEffects.ALL_OUT_EFFORT);
        event.registerMobEffect(GradedEffectIcon.mobEffect("liquor_worm", 1, 4), ModEffects.LIQUOR_WORM);
        event.registerMobEffect(GradedEffectIcon.item("casual_gu", 1, 2), ModEffects.CASUAL_GU);
        event.registerMobEffect(GradedEffectIcon.item("malicious_thought_gu", 2, 5, 2),
                ModEffects.MALICIOUS_THOUGHT_GU);
        event.registerMobEffect(GradedEffectIcon.item("self_reliance_gu", 2, 4), ModEffects.SELF_RELIANCE_GU);
        event.registerMobEffect(new ItemEffectIcon("hardship_strength_gu"), ModEffects.HARDSHIP_STRENGTH_GU);
        event.registerMobEffect(new ItemEffectIcon("horizontal_crash_gu"), ModEffects.HORIZONTAL_CRASH_GU);
        event.registerMobEffect(new ItemEffectIcon("vertical_crash_gu"), ModEffects.VERTICAL_CRASH_GU);
        event.registerMobEffect(GradedEffectIcon.item("charging_crash_gu", 4, 5), ModEffects.CHARGING_CRASH_GU);
        event.registerMobEffect(new ItemEffectIcon("second_watch_gu"), ModEffects.SECOND_WATCH_GU);
        event.registerMobEffect(new ItemEffectIcon("third_watch_gu"), ModEffects.THIRD_WATCH_GU);
        event.registerFluidType(new SpiritSpringClientExtensions(), ModFluidTypes.SPIRIT_SPRING.get());
    }
}
