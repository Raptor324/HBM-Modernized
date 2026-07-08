package com.hbm_m.entity.missile;



import api.hbm.entity.IRadarDetectable;

import com.hbm_m.explosion.ExplosionChaos;

import com.hbm_m.explosion.MissileWarheadEffects;

import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.Level;



/**

 * Ракеты уровня 2 (корпус Strong).

 */

public abstract class MissileTier2 extends MissileBaseEntity {



    protected MissileTier2(EntityType<? extends MissileTier2> type, Level level) {

        super(type, level);

    }



    @Override

    public IRadarDetectable.RadarTargetType getTargetType() {

        return IRadarDetectable.RadarTargetType.MISSILE_TIER2;

    }



    public static class MissileStrong extends MissileTier2 {

        public MissileStrong(EntityType<? extends MissileStrong> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier2(this, server, pos, false);
            }
        }

    }



    public static class MissileIncendiaryStrong extends MissileTier2 {

        public MissileIncendiaryStrong(EntityType<? extends MissileIncendiaryStrong> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier2(this, server, pos, true);
            }
            ExplosionChaos.flameDeath(level(), pos.getX(), pos.getY(), pos.getZ(), 25);

        }

    }



    public static class MissileClusterStrong extends MissileTier2 {

        public MissileClusterStrong(EntityType<? extends MissileClusterStrong> type, Level level) {

            super(type, level);

            this.isCluster = true;

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            level().explode(this, getX(), getY(), getZ(), 15.0F, Level.ExplosionInteraction.BLOCK);

            ExplosionChaos.cluster(level(), getX(), getY(), getZ(), 50,
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



    public static class MissileBusterStrong extends MissileTier2 {

        public MissileBusterStrong(EntityType<? extends MissileBusterStrong> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadBusterTier2(this, server, pos);
            }
        }

    }



    public static class MissileEmpStrong extends MissileTier2 {

        public MissileEmpStrong(EntityType<? extends MissileEmpStrong> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            MissileWarheadEffects.empPulse(level(), getX(), getY(), getZ());

        }

    }

}

