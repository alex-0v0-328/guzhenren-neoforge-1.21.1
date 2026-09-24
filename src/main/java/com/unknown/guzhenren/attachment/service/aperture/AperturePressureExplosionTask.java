package com.unknown.guzhenren.attachment.service.aperture;

import com.unknown.guzhenren.custom.enums.body.ExtremePhysique;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The block side of the ten-extreme pressure explosion [空窍压力爆炸]: the crater is carved over
 * several server ticks instead of one, so the largest blast (Great Strength True Martial [大力真武体],
 * radius 112) no longer freezes the world. Interior blocks go out silently ({@code UPDATE_CLIENTS}
 * only, no drops); one deferred neighbor pass over the rim [球壳边缘] keeps sand, fluids and torches
 * from floating. The shell [壳] walks center-outwards and each column's radius carries a per-explosion
 * jitter [噪声扰动]. The crater floor is physique-specific: layered ice [分层冰] for Northern Dark
 * Ice Soul, magma and lava for Blazing Glory, scattered gold for Myriad Gold; Northern Dark Ice Soul
 * also converts the surface soil of an outer ring [雪化环带] to snow. Entity damage, self-damage and
 * the pressure reset stay in {@code detonatePressure} -- they resolve at tick zero.
 *
 * @author Alex
 * @version 1.0.0
 * @see ApertureService#detonatePressure(net.minecraft.server.level.ServerPlayer)
 * @since 1.0.0
 */

public final class AperturePressureExplosionTask {

    private AperturePressureExplosionTask(@NotNull ServerLevel level, double x, double y, double z, int radius,
                                  @NotNull ExtremePhysique physique) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.seed = level.random.nextLong();
        this.physique = physique;
    }
    private static final List<AperturePressureExplosionTask> ACTIVE = new ArrayList<>();
    private static final int BLOCK_BUDGET = 65_536;
    private static final double NOISE_FLOOR = 0.9D;
    private static final double NOISE_RANGE = 0.1D;
    private static final int RING_MIN = 8;
    private static final double RING_NOISE_RANGE = 8.0D;
    private static final double LAVA_SHARE = 0.025D;
    private static final double GOLD_SHARE = 0.0125D;
    private static final double POWDER_SNOW_SHARE = 0.05D;
    private static final int BLUE_ICE_THRESHOLD = -32;
    private static final int GOLD_ORE_THRESHOLD = 32;
    private final ServerLevel level;
    private final double x;
    private final double y;
    private final double z;
    private final int radius;
    private final long seed;
    private final ExtremePhysique physique;
    private final LongArrayList shell = new LongArrayList();
    private final LongArrayList rim = new LongArrayList();
    private int shellDistance;
    private int shellIndex;
    private int floorCursor;
    private int ringCursor;
    private int rimIndex;
    private Phase phase = Phase.CLEAR;
    private enum Phase {CLEAR, FLOOR, RING, RIM}
    public static void start(@NotNull ServerLevel level, double x, double y, double z, int radius,
                             @NotNull ExtremePhysique physique) {
        ACTIVE.add(new AperturePressureExplosionTask(level, x, y, z, radius, physique));
    }
    public static void tickAll() {ACTIVE.removeIf(AperturePressureExplosionTask::tick);}
    public static void clear() {ACTIVE.clear();}
    static double columnJitter(int bx, int bz, long seed) {
        long h = bx * 0x9E3779B97F4A7C15L ^ bz * 0xC2B2AE3D27D4EB4FL ^ seed;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h & 0xFFFFFL) * (NOISE_RANGE / 0x10_0000L);
    }
    static double columnRadius(int radius, int bx, int bz, long seed) {
        return radius * (NOISE_FLOOR + columnJitter(bx, bz, seed));
    }
    static int columnFloorY(double centerY, double columnRadius, double horizontalSquared) {
        return (int) Math.ceil(centerY - Math.sqrt(columnRadius * columnRadius - horizontalSquared) - 0.5D) - 1;
    }
    static int iceTier(int floorY) {
        if (floorY < BLUE_ICE_THRESHOLD) return 2;
        return floorY < 0 ? 1 : 0;
    }
    static int goldTier(int floorY) {
        if (floorY < 0) return 0;
        return floorY < GOLD_ORE_THRESHOLD ? 1 : 2;
    }
    static boolean isLavaColumn(int bx, int bz, long seed) {
        return columnJitter(bx, bz, seed ^ 0x5DEECE66DL) < LAVA_SHARE;
    }
    static boolean isGoldColumn(int bx, int bz, long seed) {
        return columnJitter(bx, bz, seed ^ 0x2545F4914F6CDD1DL) < GOLD_SHARE;
    }
    static boolean isPowderSnowColumn(int bx, int bz, long seed) {
        return columnJitter(bx, bz, seed ^ 0x9E3779B9L) >= POWDER_SNOW_SHARE;
    }
    static double ringOuterRadius(int radius, int bx, int bz, long seed) {
        return radius + RING_MIN
                + columnJitter(bx, bz, seed ^ 0xC2B2AE3DL) / NOISE_RANGE * RING_NOISE_RANGE;
    }
    private boolean tick() {
        if (phase == Phase.CLEAR && tickClear()) {
            phase = hasFloor() ? Phase.FLOOR : nextAfterFloor();
        }
        if (phase == Phase.FLOOR && tickFloor()) phase = nextAfterFloor();
        if (phase == Phase.RING && tickRing()) phase = Phase.RIM;
        return phase == Phase.RIM && tickRim();
    }
    private Phase nextAfterFloor() {
        return physique == ExtremePhysique.NORTHERN_DARK_ICE_SOUL ? Phase.RING : Phase.RIM;
    }
    private boolean hasFloor() {
        return switch (physique) {
            case NORTHERN_DARK_ICE_SOUL, BLAZING_GLORY_LIGHTNING_BRILLIANCE, MYRIAD_GOLD_WONDROUS_ESSENCE -> true;
            default -> false;
        };
    }
    private boolean tickClear() {
        int budget = BLOCK_BUDGET;
        while (budget > 0) {
            if (shellIndex >= shell.size()) {
                if (shellDistance > radius) return true;
                fillShell();
                shellIndex = 0;
                continue;
            }
            clearOne(BlockPos.of(shell.getLong(shellIndex++)));
            budget--;
        }
        return false;
    }
    private boolean tickFloor() {
        int budget = BLOCK_BUDGET;
        int side = 2 * radius + 1;
        while (budget > 0 && floorCursor < side * side) {
            int i = floorCursor++;
            int bx = Mth.floor(x) - radius + i % side;
            int bz = Mth.floor(z) - radius + i / side;
            double rr = columnRadius(radius, bx, bz, seed);
            double dx = bx + 0.5D - x;
            double dz = bz + 0.5D - z;
            double horizontalSquared = dx * dx + dz * dz;
            if (horizontalSquared > rr * rr) continue;
            int floorY = columnFloorY(y, rr, horizontalSquared);
            if (floorY < level.getMinBuildHeight() || floorY >= level.getMaxBuildHeight()) continue;
            BlockPos floorPos = new BlockPos(bx, floorY, bz);
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(floorPos.getX()),
                    SectionPos.blockToSectionCoord(floorPos.getZ())) || level.getBlockState(floorPos).isAir()) {
                continue;
            }
            Block block = floorBlockAt(bx, floorY, bz);
            if (block == null) continue;
            level.setBlock(floorPos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
            budget--;
        }
        return floorCursor >= side * side;
    }
    private boolean tickRing() {
        int budget = BLOCK_BUDGET;
        int outer = radius + RING_MIN + (int) Math.ceil(RING_NOISE_RANGE);
        int side = 2 * outer + 1;
        while (budget > 0 && ringCursor < side * side) {
            int i = ringCursor++;
            int bx = Mth.floor(x) - outer + i % side;
            int bz = Mth.floor(z) - outer + i / side;
            double dx = bx + 0.5D - x;
            double dz = bz + 0.5D - z;
            double horizontalSquared = dx * dx + dz * dz;
            double rr = columnRadius(radius, bx, bz, seed);
            if (horizontalSquared <= rr * rr) continue;
            double ringOuter = ringOuterRadius(radius, bx, bz, seed);
            if (horizontalSquared >= ringOuter * ringOuter) continue;
            if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(bx),
                    SectionPos.blockToSectionCoord(bz))) continue;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) - 1;
            if (surfaceY < level.getMinBuildHeight()) continue;
            BlockPos surfacePos = new BlockPos(bx, surfaceY, bz);
            if (!isSoilSurface(level.getBlockState(surfacePos))) continue;
            Block snow = isPowderSnowColumn(bx, bz, seed) ? Blocks.POWDER_SNOW : Blocks.SNOW_BLOCK;
            level.setBlock(surfacePos, snow.defaultBlockState(), Block.UPDATE_CLIENTS);
            rim.add(surfacePos.asLong());
            budget--;
        }
        return ringCursor >= side * side;
    }
    private boolean tickRim() {
        int budget = BLOCK_BUDGET;
        while (budget > 0 && rimIndex < rim.size()) {
            level.updateNeighborsAt(BlockPos.of(rim.getLong(rimIndex++)), Blocks.AIR);
            budget--;
        }
        return rimIndex >= rim.size();
    }
    private @Nullable Block floorBlockAt(int bx, int floorY, int bz) {
        return switch (physique) {
            case NORTHERN_DARK_ICE_SOUL -> switch (iceTier(floorY)) {
                case 2 -> Blocks.BLUE_ICE;
                case 1 -> Blocks.PACKED_ICE;
                default -> Blocks.ICE;
            };
            case BLAZING_GLORY_LIGHTNING_BRILLIANCE ->
                    isLavaColumn(bx, bz, seed) ? Blocks.LAVA : Blocks.MAGMA_BLOCK;
            case MYRIAD_GOLD_WONDROUS_ESSENCE -> {
                if (!isGoldColumn(bx, bz, seed)) yield null;
                yield switch (goldTier(floorY)) {
                    case 0 -> Blocks.DEEPSLATE_GOLD_ORE;
                    case 1 -> Blocks.GOLD_ORE;
                    default -> Blocks.GOLD_BLOCK;
                };
            }
            default -> null;
        };
    }
    private boolean isSoilSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT);
    }
    private void fillShell() {
        shell.clear();
        int d = shellDistance++;
        int cx = Mth.floor(x);
        int cy = Mth.floor(y);
        int cz = Mth.floor(z);
        if (d == 0) {
            addInside(cx, cy, cz);
            return;
        }
        for (int dx = -d; dx <= d; dx++) {
            for (int dy = -d; dy <= d; dy++) {
                addInside(cx + dx, cy + dy, cz - d);
                addInside(cx + dx, cy + dy, cz + d);
            }
        }
        for (int dx = -d; dx <= d; dx++) {
            for (int dz = 1 - d; dz <= d - 1; dz++) {
                addInside(cx + dx, cy - d, cz + dz);
                addInside(cx + dx, cy + d, cz + dz);
            }
        }
        for (int dy = 1 - d; dy <= d - 1; dy++) {
            for (int dz = 1 - d; dz <= d - 1; dz++) {
                addInside(cx - d, cy + dy, cz + dz);
                addInside(cx + d, cy + dy, cz + dz);
            }
        }
    }
    private void addInside(int bx, int by, int bz) {
        if (by < level.getMinBuildHeight() || by >= level.getMaxBuildHeight()) return;
        if (!outsideSphere(bx, by, bz)) shell.add(BlockPos.asLong(bx, by, bz));
    }
    private void clearOne(BlockPos pos) {
        if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()))) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (isRim(pos)) rim.add(pos.asLong());
    }
    private boolean isRim(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (neighbor.getY() >= level.getMinBuildHeight() && neighbor.getY() < level.getMaxBuildHeight()
                    && outsideSphere(neighbor.getX(), neighbor.getY(), neighbor.getZ())) return true;
        }
        return false;
    }
    private boolean outsideSphere(int bx, int by, int bz) {
        double dx = bx + 0.5D - x;
        double dy = by + 0.5D - y;
        double dz = bz + 0.5D - z;
        double rr = columnRadius(radius, bx, bz, seed);
        return dx * dx + dy * dy + dz * dz > rr * rr;
    }
}
