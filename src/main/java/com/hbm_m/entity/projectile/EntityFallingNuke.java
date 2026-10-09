package com.hbm_m.entity.projectile;

import com.hbm_m.explosion.CustomNukeExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityFallingNuke}: abgeworfene Baukastenbombe. Faellt mit 0.05/Tick bis -1, neigt sich bis -75 Grad und
 * zuendet mit ihren Stufenwerten, sobald sie in einem Block steckt. Die Blickrichtung kommt als Original-Meta (2-5).
 */
public class EntityFallingNuke extends Entity {

    private static final EntityDataAccessor<Byte> META = SynchedEntityData.defineId(EntityFallingNuke.class, EntityDataSerializers.BYTE);

    float tnt;
    float nuke;
    float hydro;
    float amat;
    float dirty;
    float schrab;
    float euph;

    public EntityFallingNuke(EntityType<? extends EntityFallingNuke> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityFallingNuke(EntityType<? extends EntityFallingNuke> type, Level world, float tnt, float nuke, float hydro, float amat, float dirty, float schrab, float euph) {
        this(type, world);
        this.tnt = tnt;
        this.nuke = nuke;
        this.hydro = hydro;
        this.amat = amat;
        this.dirty = dirty;
        this.schrab = schrab;
        this.euph = euph;
        this.yRotO = 90;
        this.setYRot(90);
        this.xRotO = 90;
        this.setXRot(90);
    }

    /** Port-FACING (vom Spieler weg) auf die Original-Meta von {@code NukeCustom.onBlockPlacedBy}. */
    public void setFacing(Direction facing) {
        byte meta = switch (facing) {
            case NORTH -> 5;
            case EAST -> 3;
            case SOUTH -> 4;
            default -> 2;
        };
        this.entityData.set(META, meta);
    }

    public byte getMeta() {
        return this.entityData.get(META);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(META, (byte) 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(META, (byte) 0);
    }
    *///?}

    @Override
    public void tick() {
        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();

        Vec3 m = getDeltaMovement();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);

        double motionY = m.y - 0.05D;
        if (motionY < -1)
            motionY = -1;
        this.setDeltaMovement(m.x * 0.99, motionY, m.z * 0.99);

        this.rotation();

        if (!level().getBlockState(new BlockPos(Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ()))).isAir()) {
            if (!level().isClientSide) {
                CustomNukeExplosion.explodeCustom(level(), getX(), getY(), getZ(), tnt, nuke, hydro, amat, dirty, schrab, euph);
                this.discard();
            }
        }
    }

    public void rotation() {
        this.xRotO = getXRot();

        if (getXRot() > -75)
            this.setXRot(getXRot() - 2);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("tnt", tnt);
        tag.putFloat("nuke", nuke);
        tag.putFloat("hydro", hydro);
        tag.putFloat("amat", amat);
        tag.putFloat("dirty", dirty);
        tag.putFloat("schrab", schrab);
        tag.putFloat("euph", euph);
        tag.putByte("meta", getMeta());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tnt = tag.getFloat("tnt");
        nuke = tag.getFloat("nuke");
        hydro = tag.getFloat("hydro");
        amat = tag.getFloat("amat");
        dirty = tag.getFloat("dirty");
        schrab = tag.getFloat("schrab");
        euph = tag.getFloat("euph");
        this.entityData.set(META, tag.getByte("meta"));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }
}
