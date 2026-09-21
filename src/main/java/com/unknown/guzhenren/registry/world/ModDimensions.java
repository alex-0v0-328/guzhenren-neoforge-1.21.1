package com.unknown.guzhenren.registry.world;

import com.unknown.guzhenren.Guzhenren;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.phys.Vec3;

/**
 * Resource keys for the mod's custom dimensions and their datapack-owned pieces.
 *
 * <p>Holders only: the actual dimension type, biome and level stem are written by
 * {@link com.unknown.guzhenren.datagen.ModDatapackProvider} at datagen time.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ModDimensions {

    private ModDimensions() {}
    public static final ResourceKey<DimensionType> TREASURE_YELLOW_HEAVEN_TYPE = key(Registries.DIMENSION_TYPE);
    public static final ResourceKey<Biome> TREASURE_YELLOW_HEAVEN_BIOME = key(Registries.BIOME);
    public static final ResourceKey<LevelStem> TREASURE_YELLOW_HEAVEN_STEM = key(Registries.LEVEL_STEM);
    public static final ResourceKey<Level> TREASURE_YELLOW_HEAVEN = ResourceKey.create(
            Registries.DIMENSION, Guzhenren.id("treasure_yellow_heaven"));
    public static final Vec3 TREASURE_YELLOW_HEAVEN_SPAWN = new Vec3(0.5, 64.0, 0.5);

    /**
     * An anchored dimension the mod owns: {@code /guworld enter} may target it, and a player inside
     * is expected to leave through {@code /guworld exit} so the recorded return point is used.
     *
     * @param level the dimension's level key
     * @param spawn the fixed entry point every entrant arrives at
     * @param rank the dimension's 转 (6..9: 6-7 blessed land [福地], 8-9 grotto-heaven [洞天])
     */
    public record AnchoredDimension(ResourceKey<Level> level, Vec3 spawn, int rank) {}

    /**
     * The anchored dimension allow-list, by level key. Treasure Yellow Heaven is a rank-8
     * grotto-heaven; future Blessed Land / Grotto-Heaven dimensions register here.
     */
    public static final Map<ResourceKey<Level>, AnchoredDimension> ANCHORED_DIMENSIONS = Map.of(
            TREASURE_YELLOW_HEAVEN,
            new AnchoredDimension(TREASURE_YELLOW_HEAVEN, TREASURE_YELLOW_HEAVEN_SPAWN, 8));

    private static <T> ResourceKey<T> key(ResourceKey<Registry<T>> registry) {
        return ResourceKey.create(registry, Guzhenren.id("treasure_yellow_heaven"));
    }
}
