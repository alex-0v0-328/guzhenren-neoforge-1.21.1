package com.unknown.guzhenren.datagen;

import com.unknown.guzhenren.Guzhenren;
import com.unknown.guzhenren.entity.BoarGuEntity;
import com.unknown.guzhenren.entity.RhinocerosBeetleGuEntity;
import com.unknown.guzhenren.registry.damage.ModDamageTypes;
import com.unknown.guzhenren.registry.entity.ModEntityTypes;
import com.unknown.guzhenren.registry.world.ModBiomeTags;
import com.unknown.guzhenren.registry.world.ModDimensions;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The single provider for every datapack registry this mod writes.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider}. Builds damage
 * types and biome modifiers in one {@code RegistrySetBuilder}. The tag providers take
 * {@code getRegistryProvider()} from this instance, not the plain lookup, so the tag pass sees the
 * types this run generates.
 *
 * <p>⚠ There can only be one. The builtin-entries provider reports a fixed name, so a second instance
 * fails datagen outright; add a registry to this one's builder instead.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, ModDatapackProvider::damageTypes)
            .add(Registries.DIMENSION_TYPE, ModDatapackProvider::dimensionTypes)
            .add(Registries.BIOME, ModDatapackProvider::biomes)
            .add(Registries.LEVEL_STEM, ModDatapackProvider::levelStems)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModDatapackProvider::biomeModifiers);
    public ModDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Guzhenren.MOD_ID));
    }
    //region Damage types [伤害类型]
    private static void damageTypes(BootstrapContext<DamageType> context) {
        context.register(ModDamageTypes.LIFESPAN_EXHAUSTED, new DamageType("guzhenren.lifespan_exhausted", 0.0F));
        context.register(ModDamageTypes.SOUL_COLLAPSE, new DamageType("guzhenren.soul_collapse", 0.0F));
        context.register(ModDamageTypes.MIND_OCEAN_SHATTERED, new DamageType("guzhenren.mind_ocean_shattered", 0.0F));
        context.register(ModDamageTypes.APERTURE_PRESSURE_EXPLOSION,
                new DamageType("guzhenren.aperture_pressure_explosion", 0.0F));
        context.register(ModDamageTypes.TEN_EXTREME_DISASTER,
                new DamageType("guzhenren.ten_extreme_disaster", 0.0F));
        context.register(ModDamageTypes.VITAL_GU_LOST, new DamageType("guzhenren.vital_gu_lost", 0.0F));
    }
    //endregion

    //region Dimension type [维度类型]
    private static void dimensionTypes(BootstrapContext<DimensionType> context) {
        context.register(ModDimensions.TREASURE_YELLOW_HEAVEN_TYPE, new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                false,
                1.0,
                false,
                false,
                0,
                256,
                256,
                BlockTags.INFINIBURN_OVERWORLD,
                Guzhenren.id("treasure_yellow_heaven"),
                0.0F,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
        ));
    }
    //endregion

    //region Biome [生物群系]
    private static final int TREASURE_YELLOW_HEAVEN_SKY_COLOR = 0xF4D35E;
    private static void biomes(BootstrapContext<Biome> context) {
        context.register(ModDimensions.TREASURE_YELLOW_HEAVEN_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .fogColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .waterColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .waterFogColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build());
    }
    //endregion

    //region Level stem [维度层级源]
    private static void levelStems(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        Holder<Biome> biome = biomes.getOrThrow(ModDimensions.TREASURE_YELLOW_HEAVEN_BIOME);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biome, List.of());
        context.register(ModDimensions.TREASURE_YELLOW_HEAVEN_STEM, new LevelStem(
                dimensionTypes.getOrThrow(ModDimensions.TREASURE_YELLOW_HEAVEN_TYPE),
                new FlatLevelSource(settings)
        ));
    }
    //endregion

    //region Biome modifiers [生态修改] -- where wild entities [野生实体] spawn
    private static final ResourceKey<BiomeModifier> SPAWN_HOPE_GU = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            Guzhenren.id("spawn_hope_gu"));
    private static final ResourceKey<BiomeModifier> SPAWN_WHITE_BOAR_GU = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            Guzhenren.id("spawn_white_boar_gu"));
    private static final ResourceKey<BiomeModifier> SPAWN_BLACK_BOAR_GU = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            Guzhenren.id("spawn_black_boar_gu"));
    private static final ResourceKey<BiomeModifier> SPAWN_FLOWER_BOAR_GU = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS,
            Guzhenren.id("spawn_flower_boar_gu"));
    private static final int SPAWN_WEIGHT = 8;
    private static final ResourceKey<BiomeModifier> SPAWN_RHINOCEROS_BEETLE_GU = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS, Guzhenren.id("spawn_rhinoceros_beetle_gu"));
    private static final int PACK_MINIMUM = 1;
    private static final int PACK_MAXIMUM = 2;
    private static final int BOAR_SPAWN_WEIGHT = 4;
    private static final int BOAR_PACK_MINIMUM = 1;
    private static final int BOAR_PACK_MAXIMUM = 1;
    private static final int BEETLE_SPAWN_WEIGHT = 4;
    private static final int BEETLE_RANK_FOUR_SPAWN_WEIGHT = 2;
    private static final int BEETLE_RANK_FIVE_SPAWN_WEIGHT = 1;
    private static final int BEETLE_PACK_MINIMUM = 1;
    private static final int BEETLE_PACK_MAXIMUM = 1;
    private static final ResourceKey<BiomeModifier> SPAWN_WILD_BOAR = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS, Guzhenren.id("spawn_wild_boar"));
    private static final int WILD_BOAR_SPAWN_WEIGHT = 8;
    private static final int WILD_BOAR_PACK_MINIMUM = 1;
    private static final int WILD_BOAR_PACK_MAXIMUM = 3;
    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(SPAWN_HOPE_GU, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(ModBiomeTags.HOPE_GU_SPAWNS),
                List.of(new MobSpawnSettings.SpawnerData(
                        ModEntityTypes.HOPE_GU_ENTITY.get(), SPAWN_WEIGHT, PACK_MINIMUM, PACK_MAXIMUM))));
        context.register(SPAWN_WHITE_BOAR_GU, boarSpawns(biomes, ModEntityTypes.WHITE_BOAR_GU_ENTITY.get()));
        context.register(SPAWN_BLACK_BOAR_GU, boarSpawns(biomes, ModEntityTypes.BLACK_BOAR_GU_ENTITY.get()));
        context.register(SPAWN_FLOWER_BOAR_GU, boarSpawns(biomes, ModEntityTypes.FLOWER_BOAR_GU_ENTITY.get()));
        context.register(SPAWN_RHINOCEROS_BEETLE_GU, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(ModBiomeTags.RHINOCEROS_BEETLE_GU_SPAWNS), List.of(
                        beetleSpawns(ModEntityTypes.HORIZONTAL_CRASH_GU_ENTITY.get(), BEETLE_SPAWN_WEIGHT),
                        beetleSpawns(ModEntityTypes.VERTICAL_CRASH_GU_ENTITY.get(), BEETLE_SPAWN_WEIGHT),
                        beetleSpawns(ModEntityTypes.CHARGING_CRASH_GU_4_ENTITY.get(), BEETLE_RANK_FOUR_SPAWN_WEIGHT),
                        beetleSpawns(ModEntityTypes.CHARGING_CRASH_GU_5_ENTITY.get(), BEETLE_RANK_FIVE_SPAWN_WEIGHT))));
        context.register(SPAWN_WILD_BOAR, new BiomeModifiers.AddSpawnsBiomeModifier(
                biomes.getOrThrow(ModBiomeTags.WILD_BOAR_SPAWNS), List.of(new MobSpawnSettings.SpawnerData(
                        ModEntityTypes.WILD_BOAR.get(), WILD_BOAR_SPAWN_WEIGHT,
                        WILD_BOAR_PACK_MINIMUM, WILD_BOAR_PACK_MAXIMUM))));
    }
    private static MobSpawnSettings.SpawnerData beetleSpawns(EntityType<RhinocerosBeetleGuEntity> type,
                                                             int weight) {
        return new MobSpawnSettings.SpawnerData(type, weight, BEETLE_PACK_MINIMUM, BEETLE_PACK_MAXIMUM);
    }
    private static BiomeModifier boarSpawns(HolderGetter<Biome> biomes, EntityType<BoarGuEntity> type) {
        MobSpawnSettings.SpawnerData data = new MobSpawnSettings.SpawnerData(
                type, BOAR_SPAWN_WEIGHT, BOAR_PACK_MINIMUM, BOAR_PACK_MAXIMUM);
        return new BiomeModifiers.AddSpawnsBiomeModifier(biomes.getOrThrow(ModBiomeTags.BOAR_GU_SPAWNS), List.of(data));
    }
    //endregion
}
