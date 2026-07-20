package com.hbm_m.entity.logic;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.explosion.ExplosionTom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Gerald/Horizons' actual detonation - the authentic tektite-ring/lava crater
 * ({@link ExplosionTom}), not the plain sphere-clearing {@link com.hbm_m.explosion.ExplosionFleija}
 * used by the generic nukes. Shape mirrors {@link EntityNukeExplosionMK3} (same chunk-loading
 * base, same tick-speed ramp-up), just backed by a different crater algorithm.
 */
public class TomBlastEntity extends EntityExplosionChunkloading {

    public int destructionRange;
    public ExplosionTom expl;
    public int speed = 1;
    public boolean did;

    public TomBlastEntity(EntityType<? extends TomBlastEntity> type, Level level) {
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
            this.expl = new ExplosionTom((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
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
        did = tag.getBoolean("did");

        long time = tag.getLong("milliTime");
        if (ModClothConfig.get().limitExplosionLifespan > 0
                && System.currentTimeMillis() - time > ModClothConfig.get().limitExplosionLifespan * 1000L) {
            discard();
            return;
        }

        if (did) {
            expl = new ExplosionTom((int) getX(), (int) getY(), (int) getZ(), level(), destructionRange);
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

    public static TomBlastEntity create(Level level, double x, double y, double z, int range) {
        TomBlastEntity entity = new TomBlastEntity(ModEntities.TOM_BLAST.get(), level);
        entity.setPos(x, y, z);
        entity.destructionRange = range;
        entity.speed = ModClothConfig.get().blastSpeed;
        return entity;
    }
}
