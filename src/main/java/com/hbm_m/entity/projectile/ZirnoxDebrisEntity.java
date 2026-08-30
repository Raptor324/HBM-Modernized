package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ZirnoxDebrisEntity extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(ZirnoxDebrisEntity.class, EntityDataSerializers.INT);

    public float rot = 0F;
    public float lastRot = 0F;
    private int lifetime = 0;

    public ZirnoxDebrisEntity(EntityType<? extends ZirnoxDebrisEntity> type, Level level) {
        super(type, level);
        this.rot = this.random.nextFloat() * 360F;
        this.lastRot = this.rot;
        this.noPhysics = false;
    }

    public static ZirnoxDebrisEntity create(Level level, double x, double y, double z, DebrisType type) {
        ZirnoxDebrisEntity e = new ZirnoxDebrisEntity(ModEntities.ZIRNOX_DEBRIS.get(), level);
        e.setPos(x, y, z);
        e.setDebrisType(type);
        return e;
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        entityData.define(TYPE, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        builder.define(TYPE, 0);
    
    }
    *///?}

    public void setDebrisType(DebrisType type) {
        entityData.set(TYPE, type.ordinal());
    }

    public DebrisType getDebrisType() {
        DebrisType[] values = DebrisType.values();
        return values[Math.abs(entityData.get(TYPE)) % values.length];
    }

    @Override
    public void tick() {
        this.lastRot = this.rot;

        // Gravity
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, motion.y - 0.04, motion.z);

        this.move(MoverType.SELF, getDeltaMovement());

        motion = getDeltaMovement();
        if (this.onGround()) {
            setDeltaMovement(motion.x * 0.85, Math.abs(motion.y) > 0.05 ? motion.y * -0.5 : 0, motion.z * 0.85);
        } else {
            this.rot += 10F;
            if (this.rot >= 360F) {
                this.rot -= 360F;
                this.lastRot -= 360F;
            }
        }

        // Horizontal drag
        motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.98, motion.y, motion.z * 0.98);

        if (!level().isClientSide()) {
            if (getDebrisType() == DebrisType.ELEMENT) {
                radiateNearby();
            }

            lifetime++;
            if (lifetime > getMaxLifetime() + getId() % 50) {
                discard();
            }
        }
    }

    private void radiateNearby() {
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(2.5));
        for (LivingEntity entity : nearby) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 2000F);
        }
    }

    private int getMaxLifetime() {
        return switch (getDebrisType()) {
            case BLANK -> 3 * 60 * 20;
            case ELEMENT -> 10 * 60 * 20;
            case SHRAPNEL -> 15 * 60 * 20;
            case CONCRETE -> 60 * 20;
            case EXCHANGER -> 60 * 20;
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TYPE, tag.getInt("debtype"));
        lifetime = tag.getInt("lifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("debtype", entityData.get(TYPE));
        tag.putInt("lifetime", lifetime);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public enum DebrisType {
        BLANK,
        ELEMENT,
        SHRAPNEL,
        CONCRETE,
        EXCHANGER
    }
}
