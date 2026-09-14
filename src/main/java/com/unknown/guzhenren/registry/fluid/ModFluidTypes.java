package com.unknown.guzhenren.registry.fluid;

import com.unknown.guzhenren.Guzhenren;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The fluid types: the behavior half of a fluid pair, answering "what does standing in it feel
 * like". The Spirit Spring [元泉] type copies water's feel -- swim, drown, extinguish, push,
 * hydrate farmland, float boats -- and stays on every default the water experience implies,
 * except the one deliberately shut off: two adjacent sources never mint a new source.
 *
 * <p>Both fluid entries of a pair share the one type here; see {@link ModFluids}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModFluidTypes {

    private ModFluidTypes() {}
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Guzhenren.MOD_ID);
    public static final Supplier<FluidType> SPIRIT_SPRING = FLUID_TYPES.register("spirit_spring",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.guzhenren.spirit_spring")
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .canPushEntity(true)
                    .canHydrate(true)
                    .supportsBoating(true)
                    .canConvertToSource(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static void register(IEventBus modEventBus) {FLUID_TYPES.register(modEventBus);}
}
