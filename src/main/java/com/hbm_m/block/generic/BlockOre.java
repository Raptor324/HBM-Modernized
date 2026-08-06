package com.hbm_m.block.generic;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@link com.hbm.blocks.generic.BlockOre} 1.7.10.
 *
 * <p>В 1.7.10 {@code BlockOre} имеет {@code rad}-поле и {@code updateTick}, который при {@code rad > 0}
 * эмиттит радиацию в чанк и перепланирует себя каждые 20 тиков. Здесь используется та же логика через
 * {@link HazardSystem#getHazardLevelFromStack} — если у блока есть RADIATION hazard (например
 * {@code waste_trinitite}), он становится per-tick эмиттером, как BlockHazard.</p>
 */
public class BlockOre extends Block {

    /** 1.7.10 {@code BlockOre#tickRate} = 20 при rad > 0. */
    private static final int RAD_TICK_INTERVAL = 20;

    public BlockOre(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat(),
                pos.getY() + 1.1F,
                pos.getZ() + random.nextFloat(),
                0.0D, 0.0D, 0.0D);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (currentRad() > 0F && !level.isClientSide) {
            level.scheduleTick(pos, this, RAD_TICK_INTERVAL);
        }
    }

    /** Порт {@code BlockOre#updateTick} (1.7.10): эмиттит {@code rad} и перепланирует себя. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float rad = currentRad();
        if (rad > 0F) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), rad);
            level.scheduleTick(pos, this, RAD_TICK_INTERVAL);
        }
    }

    /** {@code rad = hazard × 0.1F} (как BlockHazard в 1.7.10). */
    protected float currentRad() {
        return HazardSystem.getHazardLevelFromStack(new ItemStack(this), HazardRegistry.RADIATION) * 0.1F;
    }
}
