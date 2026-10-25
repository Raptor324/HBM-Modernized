package com.hbm_m.entity.logic;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.entity.logic.EntityDeathBlast} (Orbitaler Todesstrahl): nach 60 Ticks Strahl eine
 * MK5-Explosion der Staerke 40 ohne Fallout, Muke-Partikel und Explosionsklang.
 */
public class EntityDeathBlast extends Entity {

    public static final int maxAge = 60;

    public EntityDeathBlast(EntityType<? extends EntityDeathBlast> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityDeathBlast(Level world) {
        this(ModEntities.DEATH_BLAST.get(), world);
    }

    //? if < 1.21.1 {
    @Override protected void defineSynchedData() { }
    //?} else {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }

    @Override
    public void tick() {
        if (this.tickCount >= maxAge && level() instanceof ServerLevel server) {
            this.discard();

            EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.start(server, 40, getX(), getY(), getZ());
            mk5.fallout = false;

            CompoundTag data = new CompoundTag();
            data.putString("type", "muke");
            IParticleCreator.sendPacket(server, getX(), getY() + 0.5, getZ(), 250, data);
            server.playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.mukeExplosion"), SoundSource.PLAYERS, 25.0F, 0.9F);
        }
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }
}
