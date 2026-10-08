package com.hbm_m.entity.train;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.util.Vec3NT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityRailCarRidable.SeatDummyEntity}: dynamische Sitze, die beim Klick nahe einem Sitzplatz entstehen
 * und mit dem Zug fahren und drehen. Wird nicht gespeichert.
 */
public class SeatDummyEntity extends Entity {

    private static final EntityDataAccessor<Integer> TRAIN_ID = SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEAT_INDEX = SynchedEntityData.defineId(SeatDummyEntity.class, EntityDataSerializers.INT);

    private int turnProgress;
    private double trainX;
    private double trainY;
    private double trainZ;
    public EntityRailCarRidable train;

    public SeatDummyEntity(EntityType<? extends SeatDummyEntity> type, Level world) {
        super(type, world);
    }

    public SeatDummyEntity(Level world, EntityRailCarRidable train, int index) {
        this(ModEntities.TRAIN_SEAT_DUMMY.get(), world);
        this.train = train;
        if (train != null) this.entityData.set(TRAIN_ID, train.getId());
        this.entityData.set(SEAT_INDEX, index);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TRAIN_ID, 0);
        this.entityData.define(SEAT_INDEX, 0);
    }

    @Override protected void addAdditionalSaveData(@NotNull CompoundTag nbt) { }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag nbt) { this.discard(); }

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
    public void lerpTo(double posX, double posY, double posZ, float yaw, float pitch, int turnProg, boolean teleport) {
        this.trainX = posX;
        this.trainY = posY;
        this.trainZ = posZ;
        this.turnProgress = turnProg + 2;
    }

    /** Original {@code updateRiderPosition}. */
    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction move) {

        if (train == null) {
            int eid = this.entityData.get(TRAIN_ID);
            Entity entity = level().getEntity(eid);
            if (entity instanceof EntityRailCarRidable ridable) {
                train = ridable;
            }
        }

        // Rueckfall, wenn der Zug fehlt
        if (train == null) {
            move.accept(passenger, getX(), getY() + 1 - riderEyeOffset(passenger), getZ());
            return;
        }

        // so statt mit der eigenen Position entfallen Abweichungen durch die Tick-Reihenfolge
        int index = this.entityData.get(SEAT_INDEX);
        Vec3NT rot = this.train.getPassengerSeats()[index];
        rot.rotateAroundX((float) (train.getXRot() * Math.PI / 180));
        rot.rotateAroundY((float) (-train.getYRot() * Math.PI / 180));
        double x = train.renderX + rot.xCoord;
        double y = train.renderY + rot.yCoord;
        double z = train.renderZ + rot.zCoord;
        move.accept(passenger, x, y - riderEyeOffset(passenger), z);
    }

    /** 1.7.10: die Y-Position eines Spielers liegt um {@code yOffset} 1.62 ueber den Fuessen. */
    static double riderEyeOffset(Entity passenger) {
        return passenger instanceof Player ? 1.62D : 0D;
    }

    //? if < 1.21.1 {
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?}
}
