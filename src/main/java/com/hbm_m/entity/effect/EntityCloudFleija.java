package com.hbm_m.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Расширяющееся циановое облако Fleija. Порт {@code com.hbm.entity.effect.EntityCloudFleija}.
 */
public class EntityCloudFleija extends Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudFleija.class, EntityDataSerializers.INT);

    public int age;

    public EntityCloudFleija(EntityType<? extends EntityCloudFleija> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public EntityCloudFleija(EntityType<? extends EntityCloudFleija> type, Level level, int maxAge) {
        this(type, level);
        setMaxAge(maxAge);
    }

    public void setMaxAge(int maxAge) {
        this.entityData.set(MAX_AGE, maxAge);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_AGE, 100);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (level().isClientSide) {
            level().setSkyFlashTime(2);
        } else if (this.age >= getMaxAge()) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getShort("age");
        if (tag.contains("maxAge")) {
            setMaxAge(tag.getInt("maxAge"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) this.age);
        tag.putInt("maxAge", getMaxAge());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000.0 * 25000.0;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
}
