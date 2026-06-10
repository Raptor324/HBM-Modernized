package com.hbm_m.entity.missile;



import api.hbm.entity.IRadarDetectable;

import com.hbm_m.explosion.ExplosionNukeGeneric;

import com.hbm_m.explosion.MissileWarheadEffects;

import com.hbm_m.explosion.NuclearExplosionAPI;

import com.hbm_m.util.explosions.nuclear.NuclearExplosionHelper;

import net.minecraft.core.BlockPos;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.Level;



/**

 * Ракеты уровня 0 (микро-носители).

 */

public abstract class MissileTier0 extends MissileBaseEntity {



    protected MissileTier0(EntityType<? extends MissileTier0> type, Level level) {

        super(type, level);

    }

    @Override
    protected float getContrailScale() {
        return 0.5F;
    }



    @Override

    public IRadarDetectable.RadarTargetType getTargetType() {

        return IRadarDetectable.RadarTargetType.MISSILE_TIER0;

    }



    public static class MissileMicro extends MissileTier0 {

        public MissileMicro(EntityType<? extends MissileMicro> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            NuclearExplosionAPI.startFatMan(level(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

        }

    }



    public static class MissileSchrabidium extends MissileTier0 {

        public MissileSchrabidium(EntityType<? extends MissileSchrabidium> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            NuclearExplosionHelper.explodeStandardNuke(level(), pos);

        }

    }



    public static class MissileEmp extends MissileTier0 {

        public MissileEmp(EntityType<? extends MissileEmp> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            ExplosionNukeGeneric.empBlast(level(), (int) getX(), (int) getY(), (int) getZ(), 50);

        }

    }



    public static class MissileBHole extends MissileTier0 {

        public MissileBHole(EntityType<? extends MissileBHole> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            MissileWarheadEffects.blackHole(level(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

        }

    }



    public static class MissileTaint extends MissileTier0 {

        public MissileTaint(EntityType<? extends MissileTaint> type, Level level) {

            super(type, level);

        }



        @Override

        protected void onMissileImpact(BlockPos pos) {

            if (level().isClientSide) {

                return;

            }

            MissileWarheadEffects.scatterTaint(level(), pos);

        }

    }

}

