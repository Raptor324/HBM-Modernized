package com.hbm_m.entity.train;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityRailCarBase.BoundingBoxDummyEntity}: unsichtbare Hilfsentitaeten, die die bewegliche Trefferform
 * des Zuges bilden und mit ihm drehen. Treffer und Rechtsklicks gehen an den Zug. Wird nicht gespeichert.
 */
public class BoundingBoxDummyEntity extends Entity {

    private static final EntityDataAccessor<Integer> TRAIN_ID = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(BoundingBoxDummyEntity.class, EntityDataSerializers.FLOAT);

    private int turnProgress;
    private double trainX;
    private double trainY;
    private double trainZ;
    public EntityRailCarBase train;

    public BoundingBoxDummyEntity(EntityType<? extends BoundingBoxDummyEntity> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    public BoundingBoxDummyEntity(Level world, EntityRailCarBase train, float width, float height) {
        this(ModEntities.TRAIN_BOUNDING_DUMMY.get(), world);
        this.setSize(width, height);
        this.train = train;
        if (train != null) this.entityData.set(TRAIN_ID, train.getId());
    }

    /** Original {@code setSize}: Groesse ueber den DataWatcher an den Client. */
    public void setSize(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        this.refreshDimensions();
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(TRAIN_ID, 0);
        this.entityData.define(WIDTH, 1F);
        this.entityData.define(HEIGHT, 1F);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(TRAIN_ID, 0);
        builder.define(WIDTH, 1F);
        builder.define(HEIGHT, 1F);
    }
    *///?}

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WIDTH.equals(key) || HEIGHT.equals(key)) this.refreshDimensions();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(this.entityData.get(WIDTH), this.entityData.get(HEIGHT));
    }

    @Override protected void addAdditionalSaveData(@NotNull CompoundTag nbt) { }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag nbt) { this.discard(); }
    @Override public boolean isPushable() { return true; }
    @Override public boolean isPickable() { return !this.isRemoved(); }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (train != null) return train.hurt(source, amount);
        return super.hurt(source, amount);
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (train != null) return train.interact(player, hand);
        return super.interact(player, hand);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (this.train == null || this.train.isRemoved()) {
                this.discard();
            }
        } else {

            if (this.turnProgress > 0) {
                this.yRotO = this.getYRot();
                double x = this.getX() + (this.trainX - this.getX()) / (double) this.turnProgress;
                double y = this.getY() + (this.trainY - this.getY()) / (double) this.turnProgress;
                double z = this.getZ() + (this.trainZ - this.getZ()) / (double) this.turnProgress;
                --this.turnProgress;
                this.setPos(x, y, z);
            } else {
                this.setPos(this.getX(), this.getY(), this.getZ());
            }
        }
    }

    @Override
    //? if < 1.21.1 {
    public void lerpTo(double posX, double posY, double posZ, float yaw, float pitch, int turnProg, boolean teleport) {
    //?} else {
    /*public void lerpTo(double posX, double posY, double posZ, float yaw, float pitch, int turnProg) {
    *///?}
        this.trainX = posX;
        this.trainY = posY;
        this.trainZ = posZ;
        this.turnProgress = turnProg + 2;
    }

    //? if < 1.21.1 {
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?}
}
