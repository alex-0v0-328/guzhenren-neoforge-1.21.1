package com.unknown.guzhenren.registry.world;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.world.SpiritSpringFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's worldgen features [世界生成]: currently only the Spirit Spring [元泉] structure.
 *
 * <p>Placement data (configured/placed feature JSON, biome modifier) is datagen'd in
 * {@link com.unknown.guzhenren.datagen.ModDatapackProvider}; this class only holds the
 * {@code Feature} instance itself.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModFeatures {

    private ModFeatures() {}
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Guzhenren.MOD_ID);
    public static final DeferredHolder<Feature<?>, SpiritSpringFeature> SPIRIT_SPRING =
            FEATURES.register("spirit_spring", SpiritSpringFeature::new);
    public static void register(IEventBus modEventBus) {FEATURES.register(modEventBus);}
}
