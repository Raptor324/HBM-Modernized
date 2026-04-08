package com.hbm_m.explosion;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.NukeExplosionMK5Entity;
import com.hbm_m.explosion.command.ExplosionCommandOptions;
import com.hbm_m.particle.helper.NukeTorexCreator;
import com.hbm_m.util.explosions.nuclear.NuclearExplosionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        return start(level, pos.x, pos.y, pos.z, cfg, ExplosionCommandOptions.DEFAULT);
    }

    /**
     * Запускает ядерный взрыв по конфигу (координаты).
     */
    public static NukeExplosionMK5Entity start(Level level, BlockPos pos, NuclearExplosionConfig cfg) {
        return start(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cfg, ExplosionCommandOptions.DEFAULT);
    }

    /**
     * Запускает ядерный взрыв по конфигу. Только серверная логика; клиентский гриб — через пакет/NukeTorexCreator.
     */
    public static NukeExplosionMK5Entity start(Level level, double x, double y, double z, NuclearExplosionConfig cfg) {
        return start(level, x, y, z, cfg, ExplosionCommandOptions.DEFAULT);
    }

    /**
     * Запуск MK5 с переключателями команды / параметрами сценария.
     */
    public static NukeExplosionMK5Entity start(Level level, double x, double y, double z,
                                               NuclearExplosionConfig cfg, ExplosionCommandOptions cmd) {
        if (level.isClientSide) {
            return null;
        }

        int scaled = Math.round(cfg.baseStrength * cmd.amplifier());
        if (scaled <= 0) {
            scaled = 25;
        } else {
            scaled = Math.max(1, scaled);
        }
        int strength = scaled * 2;

        NukeExplosionMK5Entity entity = new NukeExplosionMK5Entity(ModEntities.NUKE_MK5.get(), level);
        entity.strength = strength;
        entity.speed = (int) Math.ceil(100000D / entity.strength);
        entity.setPos(x, y, z);
        entity.length = entity.strength / 2;

        entity.fallout = cfg.falloutEnabled;
        entity.setFalloutAdd(cfg.extraFalloutRadius);

        entity.destroyTerrain = cmd.crater();
        entity.applyEntityDamage = cmd.damage();
        entity.applyInstantPlayerRads = cfg.radiationEnabled && cmd.damage();
        entity.applyCraterBiomes = cmd.biomes();

        if (cmd.particles()) {
            float scale = (float) entity.length;
            if (cfg.mushroomType == 1) {
                NukeTorexCreator.statFacBale(level, x, y, z, scale);
            } else {
                NukeTorexCreator.statFacStandard(level, x, y, z, scale);
            }
        }

        if (cmd.sound()) {
            NuclearExplosionHelper.playStandardDetonationSound((ServerLevel) level, x, y, z);
        }

        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Удобный запуск взрыва в стиле Fat Man: радиус из конфига мода.
     */
    public static NukeExplosionMK5Entity startFatMan(Level level, BlockPos pos) {
        return startFatMan(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ExplosionCommandOptions.DEFAULT);
    }

    /**
     * То же по координатам (для блоков, вызывающих explode(level, x, y, z)).
     */
    public static NukeExplosionMK5Entity startFatMan(Level level, double x, double y, double z) {
        return startFatMan(level, x, y, z, ExplosionCommandOptions.DEFAULT);
    }

    public static NukeExplosionMK5Entity startFatMan(Level level, double x, double y, double z, ExplosionCommandOptions cmd) {
        int radius = Math.max(1, Math.round(getFatManRadius() * cmd.amplifier()));
        NuclearExplosionConfig cfg = NuclearExplosionConfig.builder(radius)
                .fallout(cmd.fallout())
                .radiation(true)
                .mushroomType(0)
                .build();
        return start(level, x, y, z, cfg, cmd);
    }

    private static int getFatManRadius() {
        try {
            return ModClothConfig.get().fatManRadius;
        } catch (Exception e) {
            return DEFAULT_FAT_MAN_RADIUS;
        }
    }
}
