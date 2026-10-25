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
                // Original: nur explodeStandard(15F, 24, true) + composeEffectSmall, kein Zusatzfeuer
                MissileWarheadEffects.warheadTier1(this, server, pos, true);
            }

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

            // 1:1 EntityMissileBunkerBuster: 15x createExplosion(5F, true) nach unten, dann Partikel/Splitter/Truemmer je 5
            for (int i = 0; i < 15; i++) level().explode(this, getX(), getY() - i, getZ(), 5F, Level.ExplosionInteraction.TNT);
            com.hbm_m.explosion.ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 5);
            com.hbm_m.explosion.ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 5);
            com.hbm_m.explosion.ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 5);
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

            // Original newExplosion(this, posX, posY, posZ, 4F, false, false): kein Feuer, kein Blockschaden
            level().explode(this, getX(), getY(), getZ(), 4.0F, false, Level.ExplosionInteraction.NONE);

        }

    }

}

