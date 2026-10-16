package com.hbm_m.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Конвенциональный взрыв многоцелевой бомбы: усиленный ТНТ,
 * опциональный поджог местности и газовое облако.
 */
public final class MultiBombExplosion {

    public static final float BASE_STRENGTH = 8.0F;

    private MultiBombExplosion() {}

    /** type2: 1 = порох (+1), 2 = ТНТ (+4); type5: 4 = огонь (r10), 6 = газ. */
    public static void detonate(ServerLevel level, double x, double y, double z, int type2, int type5) {
        float strength = BASE_STRENGTH;
        int fireRadius = 0;
        boolean gas = false;

        for (int type : new int[]{type2, type5}) {
            switch (type) {
                case 1 -> strength += 1;
                case 2 -> strength += 4;
                case 4 -> fireRadius += 10;
                case 6 -> gas = true;
                default -> {}
            }
        }

        com.hbm_m.platform.PlatformHooks.playSound(level, x, y, z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 4.0F, 1.0F);
        level.explode(null, x, y, z, strength, Level.ExplosionInteraction.TNT);

        if (fireRadius > 0) {
            igniteAllBlocks(level, (int) x, (int) y, (int) z, fireRadius);
        }
        if (gas) {
            // Газовое облако — ванильные эффектные частицы по площади.
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x, y + 1, z, 60, fireRadius > 0 ? 4 : 2.5, 1.5, fireRadius > 0 ? 4 : 2.5, 0.01);
        }
    }

    private static void igniteAllBlocks(Level level, int x, int y, int z, int radius) {
        BlockPos center = new BlockPos(x, y, z);
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius / 2, -radius), center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).isSolidRender(level, pos)
                    && level.getBlockState(pos.above()).isAir()
                    && level.random.nextInt(3) == 0) {
                level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
            }
        }
    }
}
