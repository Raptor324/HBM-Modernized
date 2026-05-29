package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockAllocator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

public class BlockAllocatorStandard implements IBlockAllocator {

    protected int resolution;

    public BlockAllocatorStandard() {
        this(16);
    }

    public BlockAllocatorStandard(int resolution) {
        this.resolution = resolution;
    }

    @Override
    public HashSet<BlockPos> allocate(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {

        HashSet<BlockPos> affectedBlocks = new HashSet<>();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {

                    if (i == 0 || i == this.resolution - 1 || j == 0 || j == this.resolution - 1 || k == 0 || k == this.resolution - 1) {

                        double d0 = ((float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d1 = ((float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d2 = ((float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;

                        float powerRemaining = size * (0.7F + level.random.nextFloat() * 0.6F);
                        double currentX = x;
                        double currentY = y;
                        double currentZ = z;

                        for (float stepSize = 0.3F; powerRemaining > 0.0F; powerRemaining -= stepSize * 0.75F) {

                            BlockPos pos = BlockPos.containing(currentX, currentY, currentZ);
                            BlockState state = level.getBlockState(pos);

                            if (!state.isAir()) {
                                powerRemaining -= (state.getExplosionResistance(level, pos, explosion.compat) + 0.3F) * stepSize;
                            }

                            if (powerRemaining > 0.0F) {
                                affectedBlocks.add(pos);
                            }

                            currentX += d0 * (double) stepSize;
                            currentY += d1 * (double) stepSize;
                            currentZ += d2 * (double) stepSize;
                        }
                    }
                }
            }
        }

        return affectedBlocks;
    }
}
