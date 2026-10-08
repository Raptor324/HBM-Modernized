package com.hbm_m.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityCloudTom}: die Feuerwand des Tom-Einschlags. Waechst einen Block pro Tick und laesst den Himmel
 * dauerhaft blitzen ({@code lastLightningBolt = 2}), bis {@code maxAge}.
 */
public class EntityCloudTom extends Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE = SynchedEntityData.defineId(EntityCloudTom.class, EntityDataSerializers.INT);

    public int age;

    public EntityCloudTom(EntityType<? extends EntityCloudTom> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityCloudTom(EntityType<? extends EntityCloudTom> type, Level world, int maxAge) {
        this(type, world);
        this.setMaxAge(maxAge);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_AGE, 0);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {

        this.age++;
        if (level().isClientSide) com.hbm_m.client.ClientSkyFlash.flash(level(), 2);

        if (this.age >= this.getMaxAge()) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getShort("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) age);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
