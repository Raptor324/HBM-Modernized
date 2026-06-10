package com.hbm_m.explosion;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.particle.helper.CloudCreator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Запуск Fleija-взрыва (анти-шрабидиевое стирание) и визуального облака.
 */
public final class FleijaExplosionAPI {

    private static final int DEFAULT_ASCHRAB_RADIUS = 20;

    private FleijaExplosionAPI() {
    }

    public static EntityNukeExplosionMK3 start(Level level, BlockPos pos) {
        return start(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, getASchrabRadius());
    }

    public static EntityNukeExplosionMK3 start(Level level, double x, double y, double z, int radius) {
        if (level.isClientSide) {
            return null;
        }

        EntityNukeExplosionMK3 explosion = EntityNukeExplosionMK3.statFacFleija(level, x, y, z, radius);
        if (!explosion.isRemoved()) {
            level.addFreshEntity(explosion);
            CloudCreator.composeEffect(level, x, y, z, radius);
        }
        return explosion;
    }

    private static int getASchrabRadius() {
        try {
            return Math.max(1, ModClothConfig.get().aSchrabRadius);
        } catch (Exception e) {
            return DEFAULT_ASCHRAB_RADIUS;
        }
    }
}
