package com.hbm_m.explosion.command;

import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.explosion.NuclearExplosionConfig;
import com.hbm_m.util.explosions.nuclear.NuclearExplosionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Запуск выбранного типа взрыва на сервере.
 */
public final class ExplosionCommandExecutor {

    private ExplosionCommandExecutor() {}

    public static void execute(ServerLevel level, ExplosionType type, Vec3 position, ExplosionCommandOptions opt) {
        double x = position.x;
        double y = position.y;
        double z = position.z;
        BlockPos blockPos = BlockPos.containing(x, y, z);

        switch (type) {
            case EXPLOSION_NUKE_MK5 -> NuclearExplosionAPI.start(level, x, y, z,
                    NuclearExplosionConfig.builder(25)
                            .fallout(opt.fallout())
                            .radiation(true)
                            .mushroomType(0)
                            .build(),
                    opt);
            case EXPLOSION_NUKE_FATMAN -> NuclearExplosionAPI.startFatMan(level, x, y, z, opt);
            case EXPLOSION_NUKE_GENERIC -> NuclearExplosionHelper.explodeStandardNuke(level, blockPos, opt);
            case EXPLOSION_NUKE_CHARGE -> NuclearScenarioLaunchers.launchNuclearCharge(level, blockPos, opt);
            case EXPLOSION_NUKE_DUD -> NuclearScenarioLaunchers.launchDudNuke(level, blockPos, opt);
            case EXPLOSION_NUKE_MINE -> NuclearScenarioLaunchers.launchMineNuke(level, blockPos, opt);
            case EXPLOSION_NUKE_GRENADE -> NuclearScenarioLaunchers.launchGrenadeNuc(level, blockPos, opt, null);
        }
    }
}
