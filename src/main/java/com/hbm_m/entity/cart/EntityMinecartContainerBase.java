package com.hbm_m.entity.cart;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityMinecartContainerBase}: NTM-Lore mit Inventar beliebiger Groesse. Der Inhalt faellt beim
 * Zerstoeren nicht heraus (die Kisten-Lore nimmt ihn im Gegenstand mit).
 */
public abstract class EntityMinecartContainerBase extends AbstractMinecartContainer implements INTMCart {

    private static final EntityDataAccessor<Integer> CART_BASE = SynchedEntityData.defineId(EntityMinecartContainerBase.class, EntityDataSerializers.INT);

    protected EntityMinecartContainerBase(EntityType<?> type, Level world) {
        super(type, world);
    }

    protected EntityMinecartContainerBase(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base) {
        super(type, x, y, z, world);
        this.setBase(base);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CART_BASE, 0);
    }

    public void setBase(EnumCartBase type) {
        this.entityData.set(CART_BASE, type.ordinal());
    }

    @Override
    public EnumCartBase getBase() {
        return EnumCartBase.values()[Math.abs(this.entityData.get(CART_BASE)) % EnumCartBase.values().length];
    }

    @Override
    public @NotNull Type getMinecartType() {
        return Type.CHEST;
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

    @Override
    public void destroy(@NotNull DamageSource source) {
        this.kill();
        ItemStack itemstack = getCartItem();

        if (this.hasCustomName()) {
            itemstack.setHoverName(this.getCustomName());
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
