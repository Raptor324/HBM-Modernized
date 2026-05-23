package com.hbm_m.entity.missile;



import api.hbm.entity.IRadarDetectable;

import com.hbm_m.explosion.ExplosionChaos;

import com.hbm_m.explosion.MissileWarheadEffects;

import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.Level;



/**

 * Ракеты уровня 3 (корпус Huge).

 */

public abstract class MissileTier3 extends MissileBaseEntity {



    protected MissileTier3(EntityType<? extends MissileTier3> type, Level level) {

        super(type, level);

    }



    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_TIER3;
    }

    @Override
    protected void spawnContrail() {
        net.minecraft.world.phys.Vec3 thrust = new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 0.5D);
        thrust = thrust.yRot(-(this.getYRot() + 90.0F) * ((float) Math.PI / 180.0F));
        thrust = thrust.xRot(-this.getXRot() * ((float) Math.PI / 180.0F));
        thrust = thrust.yRot((this.getYRot() + 90.0F) * ((float) Math.PI / 180.0F));

        spawnContrailWithOffset(thrust.x, thrust.y, thrust.z);
        spawnContrailWithOffset(-thrust.z, thrust.y, thrust.x);
        spawnContrailWithOffset(-thrust.x, -thrust.z, -thrust.z);
        spawnContrailWithOffset(thrust.z, -thrust.z, -thrust.x);
    }

    public static class MissileBurst extends MissileTier3 {

        public MissileBurst(EntityType<? extends MissileBurst> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier3Large(this, server, pos, false);
            }
        }

    }



    public static class MissileInferno extends MissileTier3 {

        public MissileInferno(EntityType<? extends MissileInferno> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier3Large(this, server, pos, true);
            }
            ExplosionChaos.burn(level(), pos.getX(), pos.getY(), pos.getZ(), 10);

            ExplosionChaos.flameDeath(level(), pos.getX(), pos.getY(), pos.getZ(), 25);

        }

    }



    public static class MissileRain extends MissileTier3 {

        public MissileRain(EntityType<? extends MissileRain> type, Level level) {

            super(type, level);

            this.isCluster = true;

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            level().explode(this, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,

                    25.0F, Level.ExplosionInteraction.BLOCK);

            ExplosionChaos.cluster(level(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 100);

        }



        @Override

        protected void cluster() {

            if (level().isClientSide || this.exploded) {

                return;

            }

            this.exploded = true;

            onMissileImpact(BlockPos.containing(getX(), getY(), getZ()));

            releaseChunkTicket();

            this.discard();

        }

    }



    public static class MissileDrill extends MissileTier3 {

        public MissileDrill(EntityType<? extends MissileDrill> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadDrill(this, server, pos);
            }
        }

    }

}

