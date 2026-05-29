package com.hbm_m.block.nature;

import com.hbm_m.particle.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Базовый класс для радиоактивных блоков.
 * Частицы вокруг блока — порт {@code BlockHazard#sPart} / {@code ExtDisplayEffect.RADFOG} (1.7.10).
 */
public class RadioactiveBlock extends Block {

    public RadioactiveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        spawnRadFogParticles(level, pos, rand);
    }

    /**
     * Частицы «townaura» в соседних воздушных клетках. Порт {@link com.hbm.blocks.generic.BlockHazard#sPart}.
     */
    static void spawnRadFogParticles(Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(4) != 0) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            if (!level.getBlockState(adjacent).isAir()) {
                continue;
            }

            double ix = pos.getX() + 0.5D + dir.getStepX() + rand.nextDouble() * 3.0D - 1.5D;
            double iy = pos.getY() + 0.5D + dir.getStepY() + rand.nextDouble() * 3.0D - 1.5D;
            double iz = pos.getZ() + 0.5D + dir.getStepZ() + rand.nextDouble() * 3.0D - 1.5D;

            if (dir.getStepX() != 0) {
                ix = pos.getX() + 0.5D + dir.getStepX() * 0.5D + rand.nextDouble() * dir.getStepX();
            }
            if (dir.getStepY() != 0) {
                iy = pos.getY() + 0.5D + dir.getStepY() * 0.5D + rand.nextDouble() * dir.getStepY();
            }
            if (dir.getStepZ() != 0) {
                iz = pos.getZ() + 0.5D + dir.getStepZ() * 0.5D + rand.nextDouble() * dir.getStepZ();
            }

            level.addParticle(ModParticleTypes.TOWNAURA.get(), ix, iy, iz, 0.0D, 0.0D, 0.0D);
        }
    }
}
