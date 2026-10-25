package com.hbm_m.entity.projectile;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.entity.projectile.EntityDuchessGambit}: das vom Himmel fallende Boot (12ga "Boat"-Munition).
 * 50 Wolkenpartikel beim ersten Tick, faellt mit bis zu 1.5 Bloecken/Tick; beim Aufschlag Gambit-Alarm, 1000 Schaden
 * ("boat") in einem 10x4x18-Kasten, fuenf Explosionen entlang der Laengsachse, ein {@code boat}-Block und fuenf
 * Schockwellen.
 */
public class EntityDuchessGambit extends Entity {

    public EntityDuchessGambit(EntityType<? extends EntityDuchessGambit> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityDuchessGambit(Level world) {
        this(ModEntities.DUCHESS_GAMBIT.get(), world);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        // Original ueberschreibt onUpdate vollstaendig (kein Entity-Grundtick)
        if (level() instanceof ServerLevel server && this.tickCount == 1) {
            for (int i = 0; i < 50; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "bf");
                IParticleCreator.sendPacket(server,
                        getX() + (random.nextDouble() - 0.5) * 5,
                        getY() + (random.nextDouble() - 0.5) * 7,
                        getZ() + (random.nextDouble() - 0.5) * 20, 150, data);
            }
        }

        this.xOld = this.xo = getX();
        this.yOld = this.yo = getY();
        this.zOld = this.zo = getZ();
        this.setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);

        double motionY = getDeltaMovement().y - 0.03;
        if (motionY < -1.5)
            motionY = -1.5;
        this.setDeltaMovement(getDeltaMovement().x, motionY, getDeltaMovement().z);

        // Original: (int) posX usw. (Richtung null gerundet)
        if (!this.level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
            SoundEvent gambit = HbmSoundsNT.get("hbm:alarm.gambit");
            if (gambit != null) this.level().playSound(null, getX(), getY(), getZ(), gambit, SoundSource.PLAYERS, 10000.0F, 1F);
            this.discard();

            List<Entity> list = level().getEntities((Entity) null, new AABB(getX() - 5, getY() - 2, getZ() - 9, getX() + 5, getY() + 2, getZ() + 9));

            for (Entity e : list) {
                e.hurt(ModDamageSources.create(level(), ModDamageTypes.BOAT), 1000);
            }

            if (!level().isClientSide) {
                ExplosionLarge.explode(level(), getX(), getY(), getZ() - 6, 2, true, false, false);
                ExplosionLarge.explode(level(), getX(), getY(), getZ() - 3, 2, true, false, false);
                ExplosionLarge.explode(level(), getX(), getY(), getZ(), 2, true, false, false);
                ExplosionLarge.explode(level(), getX(), getY(), getZ() + 3, 2, true, false, false);
                ExplosionLarge.explode(level(), getX(), getY(), getZ() + 6, 2, true, false, false);

                level().setBlock(new BlockPos((int) (getX() - 0.5), (int) (getY() + 0.5), (int) (getZ() - 0.5)), ModBlocks.BOAT.get().defaultBlockState(), 3);
            }
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 1.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 1);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    //? if < 1.21.1 {
    @Override protected void defineSynchedData() { }
    //?} else {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }
}
