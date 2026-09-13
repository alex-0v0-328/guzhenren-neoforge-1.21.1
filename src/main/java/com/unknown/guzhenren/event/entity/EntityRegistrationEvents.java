package com.unknown.guzhenren.event.entity;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.FlyingGuEntity;
import com.unknown.guzhenren.entity.WildBoarEntity;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Where this mod's entities declare their attributes and where they are allowed to spawn.
 *
 * <p>Hope Gu registers {@link com.unknown.guzhenren.entity.FlyingGuEntity}'s attributes; the boar Gu and
 * rhinoceros beetle Gu variants share them with the raised resting flying speed
 * ({@code RESTING_GU_FLYING_SPEED}). Flying Gu families use one surface spawn placement, while the wild boar
 * mirrors {@link net.minecraft.world.entity.animal.Animal#checkAnimalSpawnRules} so it needs a grass block and
 * animal-appropriate light. The flying placement uses
 * {@link net.minecraft.world.level.levelgen.Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} and a custom check that
 * requires {@code pos.getY() >= level.getSeaLevel()} — NOT {@code canSeeSky}, because leaves count as cover
 * and would empty every forest floor.
 *
 * @author Alex
 * @version 1.0.0
 * @see com.unknown.guzhenren.entity.FlyingGuEntity
 * @since 1.0.0
 */

@EventBusSubscriber(modid = Guzhenren.MOD_ID)
public final class EntityRegistrationEvents {

    private static final double RESTING_GU_FLYING_SPEED = 0.3D;
    private EntityRegistrationEvents() {}
    @SubscribeEvent
    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.HOPE_GU_ENTITY.get(), FlyingGuEntity.createAttributes().build());
        event.put(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(), restingGuAttributes().build());
        event.put(ModEntityTypes.WILD_BOAR.get(), WildBoarEntity.createAttributes().build());
    }
    private static AttributeSupplier.Builder restingGuAttributes() {
        return FlyingGuEntity.createAttributes().add(Attributes.FLYING_SPEED, RESTING_GU_FLYING_SPEED);
    }
    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntityTypes.HOPE_GU_ENTITY.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityRegistrationEvents::onTheSurface,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntityTypes.WHITE_BOAR_GU_ENTITY.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityRegistrationEvents::onTheSurface,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntityTypes.BLACK_BOAR_GU_ENTITY.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityRegistrationEvents::onTheSurface,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityRegistrationEvents::onTheSurface,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        registerSurfaceSpawn(event, ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get());
        registerSurfaceSpawn(event, ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get());
        event.register(ModEntityTypes.WILD_BOAR.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below())
                        .is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON)
                        && (MobSpawnType.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static <T extends Mob> void registerSurfaceSpawn(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EntityRegistrationEvents::onTheSurface, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static <T extends Mob> boolean onTheSurface(EntityType<T> type, ServerLevelAccessor level,
                                                        MobSpawnType reason, BlockPos pos, RandomSource random) {
        return pos.getY() >= level.getLevel().getSeaLevel();
    }
}
