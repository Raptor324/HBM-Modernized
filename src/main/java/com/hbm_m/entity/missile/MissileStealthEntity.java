package com.hbm_m.entity.missile;

import com.hbm_m.explosion.MissileWarheadEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Стелс-ракета: не видна радару, корпус stealth.obj.
 */
public class MissileStealthEntity extends MissileBaseEntity {

    public MissileStealthEntity(EntityType<? extends MissileStealthEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean canBeDetectedByRadar() {
        return false;
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (level().isClientSide) {
            return;
        }
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            MissileWarheadEffects.warheadStealth(this, server, pos);
        }
    }
}
