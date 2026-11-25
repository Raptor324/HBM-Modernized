package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of 1.7.10 {@code com.hbm.blocks.generic.BlockHotHazard}.
 *
 * <p>Purely cosmetic additions on top of {@link BlockHazard}: a cloud puff above the block
 * while lightning can strike it (storm) and steam clouds on every non-bottom face touching
 * water. No gameplay changes. Applied to block_polonium and block_pu238
 * (1.7.10 ModBlocks:1346/1349; wired via {@code ModBlocks#HOT_INGOT_BLOCKS}).</p>
 */
public class BlockHotHazard extends BlockHazard {

    public BlockHotHazard(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        // 1.7.10: world.canLightningStrikeAt(x, y+1, z) — raining + sky visible above the block.
        if (level.isRaining() && level.canSeeSky(pos.above())) {
            level.addParticle(ParticleTypes.CLOUD,
                    pos.getX() + rand.nextFloat(), pos.getY() + 1.0D, pos.getZ() + rand.nextFloat(),
                    0.0D, 0.0D, 0.0D);
        }

        // 1.7.10: steam on every non-DOWN face whose neighbour is water (Material.water check).
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) {
                continue;
            }
            BlockPos adjacent = pos.relative(dir);
            if (!level.getFluidState(adjacent).is(FluidTags.WATER)) {
                continue;
            }

            double ix = pos.getX() + 0.5D + dir.getStepX() + rand.nextDouble() - 0.5D;
            double iy = pos.getY() + 0.5D + dir.getStepY() + rand.nextDouble() - 0.5D;
            double iz = pos.getZ() + 0.5D + dir.getStepZ() + rand.nextDouble() - 0.5D;

            if (dir.getStepX() != 0) {
                ix = pos.getX() + 0.5D + dir.getStepX() * 0.5D + rand.nextDouble() * 0.125D * dir.getStepX();
            }
            if (dir.getStepY() != 0) {
                iy = pos.getY() + 0.5D + dir.getStepY() * 0.5D + rand.nextDouble() * 0.125D * dir.getStepY();
            }
            if (dir.getStepZ() != 0) {
                iz = pos.getZ() + 0.5D + dir.getStepZ() * 0.5D + rand.nextDouble() * 0.125D * dir.getStepZ();
            }

            level.addParticle(ParticleTypes.CLOUD, ix, iy, iz, 0.0D, 0.0D, 0.0D);
        }
    }
}
