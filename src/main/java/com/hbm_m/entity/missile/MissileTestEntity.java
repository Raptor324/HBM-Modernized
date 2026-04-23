package com.hbm_m.entity.missile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.util.explosions.nuclear.NuclearExplosionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Прототип баллистической ракеты (Tier 0, missile_test).
 *
 * При столкновении с поверхностью вызывает тот же ядерный взрыв,
 * что и авиационная бомба AirNukeBombProjectileEntity.
 */
public class MissileTestEntity extends MissileBaseEntity {

    public MissileTestEntity(EntityType<? extends MissileTestEntity> type, Level level) {
        super(type, level);
    }

    public MissileTestEntity(Level level) {
        this(ModEntities.MISSILE_TEST.get(), level);
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (this.level().isClientSide) {
            return;
        }
        NuclearExplosionHelper.explodeStandardNuke(this.level(), pos);
    }
}

