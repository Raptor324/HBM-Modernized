package com.hbm_m.entity.cart;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityMinecartNTM}: Lore mit Basis (Holz/Stahl/lackiert), fest wie ein Boot, wirft beim Zerstoeren
 * ihren eigenen Gegenstand (mit Namen) ab und ist nicht reitbar.
 */
public abstract class EntityMinecartNTM extends AbstractMinecart implements INTMCart {

    private static final EntityDataAccessor<Integer> CART_BASE = SynchedEntityData.defineId(EntityMinecartNTM.class, EntityDataSerializers.INT);

    protected EntityMinecartNTM(EntityType<?> type, Level world) {
        super(type, world);
    }

    protected EntityMinecartNTM(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base) {
        super(type, world, x, y, z);
        this.setBase(base);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CART_BASE, 0); //EnumCartBase
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CART_BASE, 0); //EnumCartBase
    }
    *///?}

    public void setBase(EnumCartBase type) {
        this.entityData.set(CART_BASE, type.ordinal());
    }

    @Override
    public EnumCartBase getBase() {
        return EnumCartBase.values()[Math.abs(this.entityData.get(CART_BASE)) % EnumCartBase.values().length];
    }

    @Override
    public @NotNull Type getMinecartType() {
        return Type.RIDEABLE;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected @NotNull Item getDropItem() {
        return getCartItem().getItem();
    }

    @Override
    public @NotNull ItemStack getPickResult() {
        return getCartItem();
    }

    /** Original {@code killMinecart}: nur der eigene Gegenstand, mit Namen. */
    @Override
    public void destroy(@NotNull DamageSource source) {
        this.kill();
        ItemStack itemstack = getCartItem();

        if (this.hasCustomName()) {
            StackNbt.setCustomName(itemstack, this.getCustomName());
        }

        this.spawnAtLocation(itemstack, 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("base", this.entityData.get(CART_BASE));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(CART_BASE, nbt.getInt("base"));
    }
}
