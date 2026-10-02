package com.hbm_m.entity.logic;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionFleija;
import com.hbm_m.explosion.ExplosionNukeGeneric;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Длительный взрыв MK3 (Fleija / Solinium). Для шрабидиевой ракеты — {@link #statFacFleija}.
 * Порт {@code com.hbm.entity.logic.EntityNukeExplosionMK3} (только extType 0 — Fleija).
 */
public class EntityNukeExplosionMK3 extends EntityExplosionChunkloading {

    public int destructionRange;
    public ExplosionFleija expl;
    public int speed = 1;
    public float coefficient = 1.0F;
    public float coefficient2 = 1.0F;
    public boolean did;

    public EntityNukeExplosionMK3(EntityType<? extends EntityNukeExplosionMK3> type, Level level) {
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
            com.hbm_m.advancement.ModAdvancements.grantAll(level(),
                    com.hbm_m.advancement.ModAdvancements.MANHATTAN);
            this.expl = new ExplosionFleija(
                    (int) getX(), (int) getY(), (int) getZ(),
                    level(), this.destructionRange, this.coefficient, this.coefficient2);
            com.hbm_m.satellite.DetectorEvents.reportEvent(level(), com.hbm_m.satellite.DetectorEvents.DURATION_HIGH,
                    com.hbm_m.satellite.DetectorEvents.BurstIntensity.HIGH, getX(), getZ());

            this.did = true;
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
        coefficient = tag.getFloat("coefficient");
        coefficient2 = tag.getFloat("coefficient2");
        did = tag.getBoolean("did");

        long time = tag.getLong("milliTime");
        if (ModClothConfig.get().limitExplosionLifespan > 0
                && System.currentTimeMillis() - time > ModClothConfig.get().limitExplosionLifespan * 1000L) {
            discard();
            return;
        }

        if (did) {
            expl = new ExplosionFleija(
                    (int) getX(), (int) getY(), (int) getZ(),
                    level(), destructionRange, coefficient, coefficient2);
            expl.readFromNbt(tag, "expl_");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.tickCount);
        tag.putInt("destructionRange", destructionRange);
        tag.putInt("speed", speed);
        tag.putFloat("coefficient", coefficient);
        tag.putFloat("coefficient2", coefficient2);
        tag.putBoolean("did", did);
        tag.putLong("milliTime", System.currentTimeMillis());
        if (expl != null) {
            expl.saveToNbt(tag, "expl_");
        }
    }

    public static EntityNukeExplosionMK3 statFacFleija(Level level, double x, double y, double z, int range) {
        EntityNukeExplosionMK3 entity = new EntityNukeExplosionMK3(ModEntities.NUKE_MK3.get(), level);
        entity.setPos(x, y, z);
        entity.destructionRange = range;
        entity.speed = ModClothConfig.get().blastSpeed;
        entity.coefficient = 1.0F;
        entity.coefficient2 = 1.0F;

        // 1:1: ein Feldstoerer (field_disturber) im Umkreis 300 verhindert die Explosion
        java.util.Iterator<java.util.Map.Entry<ATEntry, Long>> it = at.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<ATEntry, Long> next = it.next();
            if (next.getValue() < level.getGameTime()) {
                it.remove();
                continue;
            }
            ATEntry entry = next.getKey();
            if (!entry.dim.equals(level.dimension().location().toString())) continue;
            double dx = x - entry.x, dy = y - entry.y, dz = z - entry.z;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 300) {
                entity.discard();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    for (int i = 0; i < 2; i++) {
                        double ix = i == 0 ? x : (entry.x + 0.5);
                        double iy = i == 0 ? y : (entry.y + 0.5);
                        double iz = i == 0 ? z : (entry.z + 0.5);
                        level.playSound(null, ix, iy, iz, com.hbm_m.sound.HbmSoundsNT.get("entity.ufoBlast"), net.minecraft.sounds.SoundSource.HOSTILE, 15.0F, 0.7F + level.random.nextFloat() * 0.2F);
                        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                        data.putString("type", "plasmablast");
                        data.putFloat("r", 0.0F);
                        data.putFloat("g", 0.75F);
                        data.putFloat("b", 1.0F);
                        data.putFloat("scale", 7.5F);
                        com.hbm_m.particle.helper.IParticleCreator.sendPacket(sl, ix, iy, iz, 150, data);
                    }
                }
                break;
            }
        }
        return entity;
    }

    /** 1:1 {@code EntityNukeExplosionMK3.at}: aktive Feldstoerer -> Ablauf-Weltzeit. */
    public static final java.util.HashMap<ATEntry, Long> at = new java.util.HashMap<>();

    public record ATEntry(String dim, int x, int y, int z) {}
}
