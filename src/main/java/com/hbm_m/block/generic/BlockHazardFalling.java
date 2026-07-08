package com.hbm_m.block.generic;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Падающий hazard-блок (порт {@link com.hbm.blocks.generic.BlockHazardFalling} 1.7.10).
 * Раз в секунду добавляет ambient-радиацию в чанк (hazard × 0.1).
 */
public class BlockHazardFalling extends FallingBlock {

    private static final int RAD_TICK_INTERVAL = 20;

    public BlockHazardFalling(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        scheduleRadTick(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        emitAmbientRad(level, pos, state);
        scheduleRadTick(level, pos);
        super.tick(state, level, pos, random);
    }

    private static void scheduleRadTick(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, level.getBlockState(pos).getBlock(), RAD_TICK_INTERVAL);
        }
    }

    private static void emitAmbientRad(ServerLevel level, BlockPos pos, BlockState state) {
        float hazard = HazardSystem.getHazardLevelFromStack(new ItemStack(state.getBlock()), HazardRegistry.RADIATION);
        float rad = hazard * 0.1F;
        if (rad > 0F) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), rad);
        }
    }
}
