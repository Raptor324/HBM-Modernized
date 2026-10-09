package com.hbm_m.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityCloudFleijaRainbow}: die Regenbogen-Kugel der Euphemium-Stufe. Waechst jeden Tick um 1 und
 * schlaegt dabei jeden Tick einen Blitz 200 Bloecke ueber sich ein, bis {@code maxAge}.
 */
public class EntityCloudFleijaRainbow extends net.minecraft.world.entity.Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE = SynchedEntityData.defineId(EntityCloudFleijaRainbow.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityCloudFleijaRainbow(EntityType<? extends EntityCloudFleijaRainbow> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityCloudFleijaRainbow(EntityType<? extends EntityCloudFleijaRainbow> type, Level world, int maxAge) {
        this(type, world);
        this.setMaxAge(maxAge);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_AGE, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 0);
    }
    *///?}

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        this.age++;

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
        if (bolt != null) {
            bolt.moveTo(getX(), getY() + 200, getZ());
            level().addFreshEntity(bolt);
        }

        if (this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getShort("age");
        scale = tag.getShort("scale");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) age);
        tag.putShort("scale", (short) scale);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }
}
