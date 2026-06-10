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
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateChunkTicket();
        }

        if (!this.did) {
            this.expl = new ExplosionFleija(
                    (int) getX(), (int) getY(), (int) getZ(),
                    level(), this.destructionRange, this.coefficient, this.coefficient2);
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

        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS,
                10000.0F, 0.8F + level().random.nextFloat() * 0.2F);
        ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), this.destructionRange * 2.0);
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
        return entity;
    }
}
