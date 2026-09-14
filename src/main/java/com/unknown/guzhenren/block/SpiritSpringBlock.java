package com.unknown.guzhenren.block;

import com.unknown.guzhenren.registry.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.AABB;

/**
 * The Spirit Spring [元泉] liquid block: vanilla water behavior plus the stone heartbeat.
 *
 * <p>Production rides the block scheduled-tick channel, not the fluid's own flow tick, so its
 * cadence cannot collide with {@code LiquidBlock}'s 5t flow reschedules. The chain is armed from
 * {@code onPlace}/{@code neighborChanged}; after a save/reload the source's random tick
 * ({@link com.unknown.guzhenren.registry.fluid.ModFluids.Source}) re-arms it, because a settled
 * source receives no scheduled ticks on its own.
 *
 * <p>⚠ The four constants below are Alex's picks (2026-09-14), not silent tunables: one stack of
 * primeval stones every 100 ticks per source, never exhausting, pausing while the cap radius
 * already holds a full stack.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */
public class SpiritSpringBlock extends LiquidBlock {

    public static final int PRODUCTION_INTERVAL_TICKS = 100;
    public static final int STONES_PER_PRODUCTION = 64;
    public static final double NEARBY_STONES_CAP_RADIUS = 4.0;
    public static final int NEARBY_STONES_CAP = STONES_PER_PRODUCTION;

    public SpiritSpringBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        scheduleProduction(level, pos);
    }
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                   boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        scheduleProduction(level, pos);
    }
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getFluidState(pos).isSource()) return;
        if (nearbyStones(level, pos) < NEARBY_STONES_CAP) {
            produceStones(level, pos, random);
        }
        scheduleProduction(level, pos);
    }
    private void scheduleProduction(Level level, BlockPos pos) {
        if (level.isClientSide() || !level.getFluidState(pos).isSource()) return;
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, PRODUCTION_INTERVAL_TICKS);
        }
    }
    private void produceStones(ServerLevel level, BlockPos pos, RandomSource random) {
        int maxStack = new ItemStack(ModItems.PRIMEVAL_STONE.get()).getMaxStackSize();
        int remaining = STONES_PER_PRODUCTION;
        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    new ItemStack(ModItems.PRIMEVAL_STONE.get(), batch));
            drop.setDefaultPickUpDelay();
            drop.setDeltaMovement((random.nextDouble() - 0.5) * 0.2, 0.2, (random.nextDouble() - 0.5) * 0.2);
            level.addFreshEntity(drop);
            remaining -= batch;
        }
    }
    public static int nearbyStones(ServerLevel level, BlockPos pos) {
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(NEARBY_STONES_CAP_RADIUS));
        int total = 0;
        for (ItemEntity drop : drops) {
            if (drop.getItem().is(ModItems.PRIMEVAL_STONE.get())) total += drop.getItem().getCount();
        }
        return total;
    }
}
