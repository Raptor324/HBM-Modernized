package com.hbm_m.explosion;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.NukeExplosionMK5Entity;
import com.hbm_m.particle.helper.NukeTorexCreator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Модульный API для запуска ядерного взрыва MK5 из любой бомбы/блока/ракеты.
 */
public final class NuclearExplosionAPI {

    private NuclearExplosionAPI() {}

    /** Радиус по умолчанию для Fat Man, если в конфиге не задан. */
    private static final int DEFAULT_FAT_MAN_RADIUS = 50;

    /**
     * Запускает ядерный взрыв по конфигу. Вызывать на сервере; визуал гриба на клиенте — отдельно через NukeTorexCreator.
     */
    public static NukeExplosionMK5Entity start(Level level, Vec3 pos, NuclearExplosionConfig cfg) {
        return start(level, pos.x, pos.y, pos.z, cfg);
    }

    /**
     * Запускает ядерный взрыв по конфигу (координаты).
     */
    public static NukeExplosionMK5Entity start(Level level, BlockPos pos, NuclearExplosionConfig cfg) {
        return start(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cfg);
    }

    /**
     * Запускает ядерный взрыв по конфигу. Только серверная логика; клиентский гриб — через пакет/NukeTorexCreator.
     */
    public static NukeExplosionMK5Entity start(Level level, double x, double y, double z, NuclearExplosionConfig cfg) {
        if (level.isClientSide) {
            return null;
        }

        int strength = cfg.baseStrength;
        if (strength == 0) strength = 25;
        strength *= 2;

        NukeExplosionMK5Entity entity = new NukeExplosionMK5Entity(ModEntities.NUKE_MK5.get(), level);
        entity.strength = strength;
        entity.speed = (int) Math.ceil(100000D / entity.strength);
        entity.setPos(x, y, z);
        entity.length = entity.strength / 2;

        entity.fallout = cfg.falloutEnabled;
        entity.setFalloutAdd(cfg.extraFalloutRadius);

        level.addFreshEntity(entity);
        // Визуал гриба на клиентах: сервер отправляет пакет частиц
        NukeTorexCreator.statFacStandard(level, x, y, z, (float) entity.length);
        return entity;
    }

    /**
     * Удобный запуск взрыва в стиле Fat Man: радиус из конфига мода.
     */
    public static NukeExplosionMK5Entity startFatMan(Level level, BlockPos pos) {
        int radius = getFatManRadius();
        return start(level, pos, NuclearExplosionConfig.fatManDefault(radius));
    }

    /**
     * То же по координатам (для блоков, вызывающих explode(level, x, y, z)).
     */
    public static NukeExplosionMK5Entity startFatMan(Level level, double x, double y, double z) {
        int radius = getFatManRadius();
        return start(level, x, y, z, NuclearExplosionConfig.fatManDefault(radius));
    }

    private static int getFatManRadius() {
        try {
            return ModClothConfig.get().fatManRadius;
        } catch (Exception e) {
            return DEFAULT_FAT_MAN_RADIUS;
        }
    }
}
