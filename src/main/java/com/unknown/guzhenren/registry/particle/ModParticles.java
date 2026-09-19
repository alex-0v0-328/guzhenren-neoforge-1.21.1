package com.unknown.guzhenren.registry.particle;

import com.unknown.guzhenren.Guzhenren;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every particle type this mod registers, one constant per particle.
 *
 * <p>DeferredRegister holder. The sprites behind each type are datagen'd by
 * {@code ModParticleDescriptionProvider}; textures live under
 * {@code textures/particle/} as Alex's hand-drawn finals.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.client.particle.RingParticle
 * @since 1.0.0
 */

public final class ModParticles {

    private ModParticles() {}
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Guzhenren.MOD_ID);
    /**
     * The dash trail: rings listed small-to-large, each blooming in place where it was dropped
     * along the dash path (planted, Alex 2026-09-20). Hidden from the dashing player's own
     * first-person camera -- the trail is for third-person and bystanders.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCKWAVE_RING = PARTICLE_TYPES.register(
            "shockwave_ring", () -> new SimpleParticleType(false));
    /**
     * The punch trail: the same rings listed small-to-large, planted along the punch ray from the
     * strike point behind the target.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IMPACT_RING = PARTICLE_TYPES.register(
            "impact_ring", () -> new SimpleParticleType(false));
    public static void register(IEventBus modEventBus) {PARTICLE_TYPES.register(modEventBus);}
}
