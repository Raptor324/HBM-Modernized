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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.entity.projectile.EntityBoxcar}: ein vom Himmel fallender Gueterwaggon. Faellt mit bis zu
 * 1.5 Bloecken/Tick, erzeugt beim ersten Tick 50 Wolkenpartikel ("bf"), beim Aufprall den Zugaufprall-Klang,
 * drei Schockwellen, 1000 Schaden ("boxcar") im Umkreis von 2 und setzt einen {@code boxcar}-Block.
 */
public class EntityBoxcar extends Entity {

    public EntityBoxcar(EntityType<? extends EntityBoxcar> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityBoxcar(Level world) {
        this(ModEntities.BOXCAR.get(), world);
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
                        getX() + (random.nextDouble() - 0.5) * 3,
                        getY() + (random.nextDouble() - 0.5) * 15,
                        getZ() + (random.nextDouble() - 0.5) * 3, 150, data);
            }
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);

        double motionY = getDeltaMovement().y - 0.03;
        if (motionY < -1.5)
            motionY = -1.5;
        this.setDeltaMovement(getDeltaMovement().x, motionY, getDeltaMovement().z);

        // Original: (int) posX usw. (Richtung null gerundet)
        if (!this.level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
            this.level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.trainImpact"), SoundSource.PLAYERS, 100.0F, 1.0F);
            this.discard();
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2);

            List<Entity> list = level().getEntities((Entity) null, new AABB(getX() - 2, getY() - 2, getZ() - 2, getX() + 2, getY() + 2, getZ() + 2));

            for (Entity e : list) {
                e.hurt(ModDamageSources.create(level(), ModDamageTypes.BOXCAR), 1000);
            }

            if (!level().isClientSide)
                level().setBlock(new BlockPos(Mth.floor(getX()), Mth.floor(getY() + 0.5), Mth.floor(getZ())), ModBlocks.BOXCAR.get().defaultBlockState(), 3);
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
