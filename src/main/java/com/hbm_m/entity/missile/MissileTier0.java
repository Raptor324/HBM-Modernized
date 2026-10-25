package com.hbm_m.entity.missile;

import api.hbm_m.entity.IRadarDetectable;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.FleijaExplosionAPI;
import com.hbm_m.explosion.MissileWarheadEffects;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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

            // 1:1 EntityMissileMicro: ExplosionNukeSmall.explode(posX, posY + 0.5, posZ, PARAMS_HIGH)
            com.hbm_m.explosion.ExplosionNukeSmall.explode(level(), getX(), getY() + 0.5D, getZ(), com.hbm_m.explosion.ExplosionNukeSmall.PARAMS_HIGH);
        }

        /** Original: {@code DictFrame.fromOne(ModItems.ammo_standard, EnumAmmo.NUKE_HIGH)}. */
        @Override
        protected ItemStack getDebrisRareDrop() {
            return new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.ammo(com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo.NUKE_HIGH));
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

            FleijaExplosionAPI.start(level(), pos);

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
            com.hbm_m.entity.effect.EntityEMPBlast wave = new com.hbm_m.entity.effect.EntityEMPBlast(level(), 50);
            wave.setPos(getX(), getY(), getZ());
            level().addFreshEntity(wave);

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

            // 1:1 EntityMissileBHole: createExplosion(this, posX, posY, posZ, 1.5F, true) (kein Feuer) + Schwarzes Loch 1.5
            level().explode(this, getX(), getY(), getZ(), 1.5F, false, Level.ExplosionInteraction.TNT);
            com.hbm_m.entity.effect.BlackHoleEntity hole = new com.hbm_m.entity.effect.BlackHoleEntity(com.hbm_m.entity.ModEntities.BLACK_HOLE.get(), level());
            hole.setPos(getX(), getY(), getZ());
            hole.setSize(1.5F);
            level().addFreshEntity(hole);

        }

        @Override

        protected ItemStack getDebrisRareDrop() {

            return new ItemStack(ModItems.BLACK_HOLE.get());

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

