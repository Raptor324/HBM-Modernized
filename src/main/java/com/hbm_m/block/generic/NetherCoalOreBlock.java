package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.gas.OutgasBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code com.hbm.blocks.generic.BlockNetherCoal} (1.7.10): адский уголь.
 * Как в оригинале: поджигает сущностей, которые встали на руду, пускает пламя и дым
 * со всех открытых граней (кроме нижней) и выделяет угарный газ — случайный тик
 * и газ при ломании унаследованы от OutgasBlock (ориг. BlockOutgas).
 */
public class NetherCoalOreBlock extends OutgasBlock {

    public NetherCoalOreBlock(Properties props) {
        super(props, () -> ModBlocks.GAS_MONOXIDE.get(), true, true);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // Ориг. onEntityWalking: entity.setFire(3) — 3 секунды горения.
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 60));
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) continue;
            BlockPos adjacent = pos.relative(direction);
            if (!level.getBlockState(adjacent).isAir()) continue;
            double x = adjacent.getX() + rand.nextDouble();
            double y = adjacent.getY() + rand.nextDouble();
            double z = adjacent.getZ() + rand.nextDouble();
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.1, 0.0);
        }
    }
}
