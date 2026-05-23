package com.hbm_m.explosion;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.ClusterRocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Вспомогательные эффекты взрывов (поджог, кластер) по образцу {@code com.hbm.explosion.ExplosionChaos}.
 */
public final class ExplosionChaos {

    private ExplosionChaos() {
    }

    /** Поджигает горючие блоки в сфере (flameDeath). */
    public static void flameDeath(Level level, int x, int y, int z, int bound) {
        int r = bound;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        BlockPos pos = new BlockPos(X, Y, Z);
                        if (level.getBlockState(pos).isFlammable(level, pos, Direction.UP)
                                && level.getBlockState(pos.above()).isAir()) {
                            level.setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /** Ставит огонь над непустыми блоками в сфере (burn). */
    public static void burn(Level level, int x, int y, int z, int bound) {
        int r = bound;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        BlockPos pos = new BlockPos(X, Y, Z);
                        BlockPos above = pos.above();
                        if ((level.getBlockState(above).isAir() || level.getBlockState(above).is(Blocks.SNOW))
                                && !level.getBlockState(pos).isAir()) {
                            level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /** Разбрасывает суббоеприпасы {@link ClusterRocketEntity}. */
    public static void cluster(Level level, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            double d1 = level.random.nextDouble();
            double d2 = level.random.nextDouble();
            double d3 = level.random.nextDouble();

            if (level.random.nextInt(2) == 0) {
                d1 *= -1;
            }
            if (level.random.nextInt(2) == 0) {
                d3 *= -1;
            }

            ClusterRocketEntity fragment = new ClusterRocketEntity(ModEntities.CLUSTER_ROCKET.get(), level);
            fragment.setPos(x + 0.5D, y + 0.5D, z + 0.5D);
            fragment.setDeltaMovement(new Vec3(d1, d2, d3));
            level.addFreshEntity(fragment);
        }
    }
}
