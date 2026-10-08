package com.hbm_m.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.entity.effect.EntityCloudSolinium}: die tuerkise Kugel der Solinium-Bombe.
 * Waechst jeden Tick um 1, schlaegt dabei jeden Tick einen Blitz 200 Bloecke ueber sich ein
 * und verschwindet nach {@code maxAge} (= Bombenradius) Ticks.
 */
public class EntityCloudSolinium extends Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(EntityCloudSolinium.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityCloudSolinium(EntityType<? extends EntityCloudSolinium> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
        this.age = 0;
        this.scale = 0;
    }

    public EntityCloudSolinium(EntityType<? extends EntityCloudSolinium> type, Level level, int maxAge) {
        this(type, level);
        this.setMaxAge(maxAge);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_AGE, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 0);
    }
    *///?}

    @Override
    public void tick() {
        this.age++;

        // Original: spawnEntityInWorld(new EntityLightningBolt(..., posY + 200, ...)) - Blitze kommen vom Server
        if (!level().isClientSide) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
            if (bolt != null) {
                bolt.moveTo(this.getX(), this.getY() + 200, this.getZ());
                level().addFreshEntity(bolt);
            }
        }

        if (this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }

        this.scale++;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getShort("age");
        this.scale = tag.getShort("scale");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("age", (short) this.age);
        tag.putShort("scale", (short) this.scale);
    }

    public void setMaxAge(int i) {
        this.entityData.set(MAX_AGE, i);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
