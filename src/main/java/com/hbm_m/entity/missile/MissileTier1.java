package com.hbm_m.entity.missile;



import api.hbm_m.entity.IRadarDetectable;

import com.hbm_m.explosion.ExplosionChaos;

import com.hbm_m.explosion.MissileWarheadEffects;

import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.Level;



/**

 * Ракеты уровня 1 (корпус V2).

 */

public abstract class MissileTier1 extends MissileBaseEntity {



    protected MissileTier1(EntityType<? extends MissileTier1> type, Level level) {

        super(type, level);

    }

    @Override
    protected float getContrailScale() {
        return 0.5F;
    }



    @Override

    public IRadarDetectable.RadarTargetType getTargetType() {

        return IRadarDetectable.RadarTargetType.MISSILE_TIER1;

    }



    public static class MissileGeneric extends MissileTier1 {

        public MissileGeneric(EntityType<? extends MissileGeneric> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier1(this, server, pos, false);
            }
        }

    }



    public static class MissileIncendiary extends MissileTier1 {

        public MissileIncendiary(EntityType<? extends MissileIncendiary> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier1(this, server, pos, true);
            }
            ExplosionChaos.flameDeath(level(), pos.getX(), pos.getY(), pos.getZ(), 24);

        }

    }



    public static class MissileCluster extends MissileTier1 {

        public MissileCluster(EntityType<? extends MissileCluster> type, Level level) {

            super(type, level);

            this.isCluster = true;

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            level().explode(this, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.BLOCK);

            ExplosionChaos.cluster(level(), getX(), getY(), getZ(), 25,
                    getYRot(), getXRot(), (float) Math.PI * 0.25F, (float) Math.PI * 0.25F, 1.0F);

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



    public static class MissileBuster extends MissileTier1 {

        public MissileBuster(EntityType<? extends MissileBuster> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadBusterTier1(this, server, pos);
            }
        }

    }



    public static class MissileDecoy extends MissileTier1 {

        public MissileDecoy(EntityType<? extends MissileDecoy> type, Level level) {

            super(type, level);

        }



        @Override

        public IRadarDetectable.RadarTargetType getTargetType() {

            return IRadarDetectable.RadarTargetType.MISSILE_TIER4;

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            level().explode(this, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,

                    4.0F, Level.ExplosionInteraction.BLOCK);

        }

    }

}

