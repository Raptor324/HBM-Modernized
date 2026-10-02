package com.hbm_m.explosion.vanillant.standard;

import java.util.HashSet;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IBlockAllocator;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockAllocatorGlyphidDig}: wie die Standard-Strahlverfolgung, bricht aber an Bloecken ueber
 * {@code maximum} Widerstand (und am Glyphiden-Spawner, sobald es ihn gibt) ab.
 */
public class BlockAllocatorGlyphidDig implements IBlockAllocator {

    protected double maximum;
    protected int resolution;

    public BlockAllocatorGlyphidDig(double maximum) {
        this(maximum, 16);
    }

    public BlockAllocatorGlyphidDig(double maximum, int resolution) {
        this.resolution = resolution;
        this.maximum = maximum;
    }

    @Override
    public HashSet<BlockPos> allocate(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {
        HashSet<BlockPos> affectedBlocks = new HashSet<>();

        for (int i = 0; i < this.resolution; ++i) {
            for (int j = 0; j < this.resolution; ++j) {
                for (int k = 0; k < this.resolution; ++k) {

                    if (i == 0 || i == this.resolution - 1 || j == 0 || j == this.resolution - 1 || k == 0 || k == this.resolution - 1) {

                        double d0 = (double) ((float) i / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d1 = (double) ((float) j / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d2 = (double) ((float) k / ((float) this.resolution - 1.0F) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;

                        double currentX = x;
                        double currentY = y;
                        double currentZ = z;

                        double dist = 0;

                        for (float stepSize = 0.3F; dist <= explosion.size;) {

                            double deltaX = currentX - x;
                            double deltaY = currentY - y;
                            double deltaZ = currentZ - z;
                            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                            BlockPos pos = new BlockPos(Mth.floor(currentX), Mth.floor(currentY), Mth.floor(currentZ));
                            BlockState block = world.getBlockState(pos);

                            if (!block.isAir()) {
                                float blockResistance = explosion.exploder != null
                                        ? explosion.exploder.getBlockExplosionResistance(explosion.compat, world, pos, block, world.getFluidState(pos), block.getBlock().getExplosionResistance())
                                        : block.getBlock().getExplosionResistance();
                                if (this.maximum < blockResistance || isGlyphidSpawner(block)) {
                                    break;
                                }
                            }

                            if (explosion.exploder == null || explosion.exploder.shouldBlockExplode(explosion.compat, world, pos, block, explosion.size)) {
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

    /** Original: {@code block == ModBlocks.glyphid_spawner} - der Block folgt mit den Glyphiden. */
    private static boolean isGlyphidSpawner(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key.getNamespace().equals(com.hbm_m.lib.RefStrings.MODID) && key.getPath().equals("glyphid_spawner");
    }
}
