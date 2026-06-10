package com.hbm_m.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Порт {@code com.hbm.entity.effect.EntityVortex} — уменьшающийся вихрь.
 */
public class VortexEntity extends BlackHoleEntity {

    private static final EntityDataAccessor<Float> SHRINK_RATE =
            SynchedEntityData.defineId(VortexEntity.class, EntityDataSerializers.FLOAT);

    public VortexEntity(EntityType<? extends VortexEntity> type, Level level) {
        super(type, level);
    }

    public VortexEntity setShrinkRate(float shrinkRate) {
        this.entityData.set(SHRINK_RATE, shrinkRate);
        return this;
    }

    public float getShrinkRate() {
        return this.entityData.get(SHRINK_RATE);
    }

    @Override
    public void tick() {
        float next = this.getSize() - this.getShrinkRate();
        this.setSize(next);
        if (next <= 0) {
            this.discard();
            return;
        }
        super.tick();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHRINK_RATE, 0.0025F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SHRINK_RATE, tag.getFloat("ShrinkRate"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ShrinkRate", this.getShrinkRate());
    }
}
