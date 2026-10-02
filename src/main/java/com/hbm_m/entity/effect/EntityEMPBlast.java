package com.hbm_m.entity.effect;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityEMPBlast}: flacher Leuchtring, der jeden Tick um 1 waechst und nach {@code maxAge} Ticks endet. */
public class EntityEMPBlast extends Entity {

    private static final EntityDataAccessor<Integer> MAX_AGE = SynchedEntityData.defineId(EntityEMPBlast.class, EntityDataSerializers.INT);

    public int age;
    public float scale = 0;

    public EntityEMPBlast(EntityType<? extends EntityEMPBlast> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityEMPBlast(Level world, int maxAge) {
        this(ModEntities.EMP_BLAST.get(), world);
        this.setMaxAge(maxAge);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_AGE, 100);
    }

    @Override
    public void tick() {
        this.age++;
        if (this.age >= this.getMaxAge()) {
            this.age = 0;
            this.discard();
        }
        this.scale++;
    }

    public void setMaxAge(int i) { this.entityData.set(MAX_AGE, i); }
    public int getMaxAge() { return this.entityData.get(MAX_AGE); }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        age = nbt.getShort("age");
        scale = nbt.getShort("scale");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        nbt.putShort("age", (short) age);
        nbt.putShort("scale", (short) scale);
    }

    //? if < 1.21.1 {
    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?} else {
    /*@NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this, serverEntity);
    }
    *///?}
}
