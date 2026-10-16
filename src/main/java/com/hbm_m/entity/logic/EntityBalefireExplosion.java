package com.hbm_m.entity.logic;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionBalefire;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.particle.helper.NukeTorexCreator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Длительный взрыв бейлфайра: спираль + зелёный гриб (Torex).
 */
public class EntityBalefireExplosion extends EntityExplosionChunkloading {

    public int destructionRange;
    public ExplosionBalefire expl;
    public int speed = 1;
    public boolean did;

    public EntityBalefireExplosion(EntityType<? extends EntityBalefireExplosion> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getChunkLoadRadius() {
        if (this.destructionRange <= 0) {
            return super.getChunkLoadRadius();
        }
        return Math.min(12, Math.max(super.getChunkLoadRadius(), (this.destructionRange + 15) >> 4) + 1);
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }
    *///?}

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateChunkTicket();
        }

        if (!this.did) {
            this.expl = new ExplosionBalefire(
                    (int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
            this.did = true;
            NukeTorexCreator.statFacBale(level(), getX(), getY() + 5, getZ(), (float) this.destructionRange);
        }

        this.speed += 1;

        if (!level().isClientSide) {
            for (int i = 0; i < this.speed; i++) {
                if (this.expl.update()) {
                    clearChunkTicket();
                    this.discard();
                    break;
                }
            }
        }

        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS,
                    10000.0F, 0.8F + level().random.nextFloat() * 0.2F);
            ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.destructionRange * 2.0);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("age");
        destructionRange = tag.getInt("destructionRange");
        speed = tag.getInt("speed");
        did = tag.getBoolean("did");

        long time = tag.getLong("milliTime");
        int lifespan;
        try {
            lifespan = ModClothConfig.get().limitExplosionLifespan;
        } catch (Exception e) {
            lifespan = 0;
        }
        if (lifespan > 0 && System.currentTimeMillis() - time > lifespan * 1000L) {
            discard();
            return;
        }

        if (did) {
            expl = new ExplosionBalefire(
                    (int) getX(), (int) getY(), (int) getZ(), level(), destructionRange);
            expl.readFromNbt(tag, "expl_");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.tickCount);
        tag.putInt("destructionRange", destructionRange);
        tag.putInt("speed", speed);
        tag.putBoolean("did", did);
        tag.putLong("milliTime", System.currentTimeMillis());
        if (expl != null) {
            expl.saveToNbt(tag, "expl_");
        }
    }

    public static EntityBalefireExplosion statFac(Level level, double x, double y, double z, int range) {
        EntityBalefireExplosion entity = new EntityBalefireExplosion(ModEntities.BALEFIRE_EXPLOSION.get(), level);
        entity.setPos(x, y, z);
        entity.destructionRange = range;
        entity.speed = ModClothConfig.get().blastSpeed;
        return entity;
    }
}
