package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.ItemGenericGrenade;

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

/**
 * 1:1 {@code com.hbm.entity.grenade.EntityGrenadeBouncyGeneric}: die Granate zu einem {@link ItemGenericGrenade};
 * Zeitzuender, Abprall und Explosion kommen vom Gegenstand. Anzeige als geworfenes Item (RenderGenericGrenade).
 */
public class EntityGrenadeBouncyGeneric extends EntityGrenadeBouncyBase implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> GRENADE = SynchedEntityData.defineId(EntityGrenadeBouncyGeneric.class, EntityDataSerializers.ITEM_STACK);

    public EntityGrenadeBouncyGeneric(EntityType<? extends EntityGrenadeBouncyGeneric> type, Level world) {
        super(type, world);
    }

    public EntityGrenadeBouncyGeneric(Level world, LivingEntity living) {
        super(ModEntities.GRENADE_BOUNCY_GENERIC.get(), world, living);
    }

    public EntityGrenadeBouncyGeneric(Level world, double x, double y, double z) {
        super(ModEntities.GRENADE_BOUNCY_GENERIC.get(), world, x, y, z);
    }

    public EntityGrenadeBouncyGeneric setType(ItemGenericGrenade grenade) {
        this.entityData.set(GRENADE, new ItemStack(grenade));
        return this;
    }

    public ItemGenericGrenade getGrenade() {
        Item item = this.entityData.get(GRENADE).getItem();
        return item instanceof ItemGenericGrenade gren ? gren : (ItemGenericGrenade) ModItems.STICK_DYNAMITE.get();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(getGrenade());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(GRENADE, ItemStack.EMPTY);
    }

    @Override
    public void explode() {
        getGrenade().explode(this, this.getThrower(), this.level(), getX(), getY(), getZ());
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("grenade", BuiltInRegistries.ITEM.getKey(getGrenade()).toString());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("grenade"));
        if (id != null && BuiltInRegistries.ITEM.get(id) instanceof ItemGenericGrenade gren) setType(gren);
    }

    @Override
    protected int getMaxTimer() {
        return getGrenade().getMaxTimer();
    }

    @Override
    protected double getBounceMod() {
        return getGrenade().getBounceMod();
    }
}
