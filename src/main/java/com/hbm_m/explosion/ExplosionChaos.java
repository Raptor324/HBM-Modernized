package com.hbm_m.explosion;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.ClusterRocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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

    /**
     * Разбрасывает суббоеприпасы по направлению полёта ракеты
     * (аналог {@code ExplosionChaos.cluster} + {@code EntityBulletBaseMK4} в 1.7.10).
     */
    public static void cluster(Level level, double x, double y, double z, int count,
                               float yaw, float pitch, float yawRand, float pitchRand, float speed) {
        if (level.isClientSide) {
            return;
        }

        for (int i = 0; i < count; i++) {
            float yawRad = yaw + (float) (yawRand * level.random.nextGaussian());
            float pitchRad = pitch + (float) (pitchRand * level.random.nextGaussian());

            float yawDeg = yawRad * 180.0F / (float) Math.PI;
            float pitchDeg = -pitchRad * 180.0F / (float) Math.PI;

            double motionX = -Mth.sin(yawDeg * ((float) Math.PI / 180.0F))
                    * Mth.cos(pitchDeg * ((float) Math.PI / 180.0F));
            double motionZ = Mth.cos(yawDeg * ((float) Math.PI / 180.0F))
                    * Mth.cos(pitchDeg * ((float) Math.PI / 180.0F));
            double motionY = -Mth.sin(pitchDeg * ((float) Math.PI / 180.0F));

            ClusterRocketEntity fragment = new ClusterRocketEntity(ModEntities.CLUSTER_ROCKET.get(), level);
            fragment.setPos(x, y, z);
            fragment.setDeltaMovement(motionX * speed, motionY * speed, motionZ * speed);
            level.addFreshEntity(fragment);
        }
    }
}
