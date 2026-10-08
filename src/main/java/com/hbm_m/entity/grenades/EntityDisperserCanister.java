package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.entity.grenade.EntityDisperserCanister} ({@code entity_disperser}): geworfener Dispersionskanister
 * bzw. Glyphiden-Druese; beim Aufprall eine {@link EntityMist} der Fluessigkeit (Flaeche 10 x 5, 80 Ticks). Anzeige wie
 * RenderGenericGrenade als geworfener Gegenstand (Overlay in Fluessigkeitsfarbe).
 */
public class EntityDisperserCanister extends EntityGrenadeBase implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> STACK = SynchedEntityData.defineId(EntityDisperserCanister.class, EntityDataSerializers.ITEM_STACK);

    public EntityDisperserCanister(EntityType<? extends EntityDisperserCanister> type, Level world) {
        super(type, world);
    }

    public EntityDisperserCanister(Level world, LivingEntity living) {
        super(ModEntities.DISPERSER_CANISTER.get(), world, living);
    }

    public EntityDisperserCanister(Level world, double x, double y, double z) {
        super(ModEntities.DISPERSER_CANISTER.get(), world, x, y, z);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(STACK, new ItemStack(ModItems.DISPERSER_CANISTER.get()));
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(STACK, new ItemStack(ModItems.DISPERSER_CANISTER.get()));
    }
    *///?}

    public EntityDisperserCanister setFluid(Fluid fluid) {
        ItemStack s = this.entityData.get(STACK).copy();
        ItemFluidTank.setFluid(s, fluid);
        this.entityData.set(STACK, s);
        return this;
    }

    public EntityDisperserCanister setType(Item item) {
        ItemStack s = new ItemStack(item);
        ItemFluidTank.setFluid(s, getFluid());
        this.entityData.set(STACK, s);
        return this;
    }

    public Fluid getFluid() {
        return ItemFluidTank.getFluid(this.entityData.get(STACK));
    }

    public Item getItemType() {
        return this.entityData.get(STACK).getItem();
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(STACK);
    }

    @Override
    public void explode() {
        if (!level().isClientSide) {
            EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), level());
            mist.setFluidType(FluidType.forFluid(getFluid()));
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 5);
            mist.setDuration(80);
            level().addFreshEntity(mist);
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("fluid", BuiltInRegistries.FLUID.getKey(getFluid()).toString());
        nbt.putString("item", BuiltInRegistries.ITEM.getKey(getItemType()).toString());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ResourceLocation item = ResourceLocation.tryParse(nbt.getString("item"));
        if (item != null && BuiltInRegistries.ITEM.containsKey(item)) setType(BuiltInRegistries.ITEM.get(item));
        ResourceLocation fluid = ResourceLocation.tryParse(nbt.getString("fluid"));
        if (fluid != null && BuiltInRegistries.FLUID.containsKey(fluid)) setFluid(BuiltInRegistries.FLUID.get(fluid));
    }
}
