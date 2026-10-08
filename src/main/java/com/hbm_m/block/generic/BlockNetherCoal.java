package com.hbm_m.block.generic;

import com.hbm_m.platform.PlatformHooks;

import java.util.function.Supplier;

import com.hbm_m.block.gas.OutgasBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockNetherCoal} ({@code ore_nether_coal}): {@code BlockOutgas(rock, false, 5, true)} mit Kohlenmonoxid -
 * beim Abbau bleibt Gas zurueck; wer darueber laeuft, brennt 3 s; an freien Seiten (ausser unten) Flammen und Rauch.
 */
public class BlockNetherCoal extends OutgasBlock {

    public BlockNetherCoal(Properties props, Supplier<Block> gas, boolean randomTick, boolean onBreak) {
        super(props, gas, randomTick, onBreak);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        PlatformHooks.setSecondsOnFire(entity, 3);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (Direction dir : Direction.values()) {

            if (dir == Direction.DOWN)
                continue;

            if (world.getBlockState(pos.relative(dir)).isAir()) {

                double ix = x + 0.5F + dir.getStepX() + rand.nextDouble() - 0.5D;
                double iy = y + 0.5F + dir.getStepY() + rand.nextDouble() - 0.5D;
                double iz = z + 0.5F + dir.getStepZ() + rand.nextDouble() - 0.5D;

                if (dir.getStepX() != 0)
                    ix = x + 0.5F + dir.getStepX() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepX();
                if (dir.getStepY() != 0)
                    iy = y + 0.5F + dir.getStepY() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepY();
                if (dir.getStepZ() != 0)
                    iz = z + 0.5F + dir.getStepZ() * 0.5 + rand.nextDouble() * 0.125 * dir.getStepZ();

                world.addParticle(ParticleTypes.FLAME, ix, iy, iz, 0.0, 0.0, 0.0);
                world.addParticle(ParticleTypes.SMOKE, ix, iy, iz, 0.0, 0.0, 0.0);
                world.addParticle(ParticleTypes.SMOKE, ix, iy, iz, 0.0, 0.1, 0.0);
            }
        }
    }
}
