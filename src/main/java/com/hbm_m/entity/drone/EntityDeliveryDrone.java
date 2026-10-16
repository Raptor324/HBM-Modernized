package com.hbm_m.entity.drone;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm_m.platform.PlatformHooks;

import java.util.Arrays;

/**
 * Port von {@code com.hbm.entity.item.EntityDeliveryDrone} (1.7.10 Original) - Pipeline A ("manuelle
 * Verlinkung"). Reiner "Lastesel": traegt 18 Item-Slots (oder alternativ eine {@link FluidTank}) und
 * hat selbst keinerlei Entscheidungsfindung - Crates/Waypoints setzen sein Ziel und laden/entladen
 * seine Fracht extern (siehe {@link com.hbm_m.blockentity.network.MachineDroneCrateBlockEntity}).
 */
public class EntityDeliveryDrone extends EntityDroneBase {

    private static final EntityDataAccessor<Boolean> EXPRESS =
            SynchedEntityData.defineId(EntityDeliveryDrone.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHUNK_LOADING =
            SynchedEntityData.defineId(EntityDeliveryDrone.class, EntityDataSerializers.BOOLEAN);

    public static final int INVENTORY_SIZE = 18;

    private final ItemStack[] items = new ItemStack[INVENTORY_SIZE];
    private final FluidTank fluidTank = new FluidTank(64_000);

    public EntityDeliveryDrone(EntityType<? extends EntityDeliveryDrone> type, Level level) {
        super(type, level);
        Arrays.fill(items, ItemStack.EMPTY);
    }

    public static EntityDeliveryDrone create(Level level, double x, double y, double z, boolean express, boolean chunkLoading) {
        EntityDeliveryDrone drone = new EntityDeliveryDrone(ModEntities.DELIVERY_DRONE.get(), level);
        drone.setPos(x, y, z);
        drone.entityData.set(EXPRESS, express);
        drone.entityData.set(CHUNK_LOADING, chunkLoading);
        return drone;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EXPRESS, false);
        this.entityData.define(CHUNK_LOADING, false);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EXPRESS, false);
        builder.define(CHUNK_LOADING, false);
    }
    *///?}

    public boolean isExpress() { return this.entityData.get(EXPRESS); }
    public boolean isChunkLoading() { return this.entityData.get(CHUNK_LOADING); }

    @Override
    public double getSpeed() {
        return isExpress() ? 0.375D * 3 : 0.375D;
    }

    @Override
    protected void loadNeighboringChunks() {
        if (level().isClientSide || !isChunkLoading()) return;
        DroneChunkLoader.tick(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && isChunkLoading()) DroneChunkLoader.release(this);
        super.remove(reason);
    }

    // ── Item cargo (item-mode crates) ────────────────────────────────────────

    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.length ? items[slot] : ItemStack.EMPTY;
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.length) items[slot] = stack;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public boolean isCargoEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    public void clearCargo() {
        Arrays.fill(items, ItemStack.EMPTY);
    }

    // ── Fluid cargo (fluid-mode crates) ──────────────────────────────────────

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide) {
            hitByEntity(player);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!level().isClientSide && !this.isRemoved()) {
            hitByEntity(source.getEntity());
        }
        return true;
    }

    private void hitByEntity(Entity attacker) {
        this.discard();

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack));
        }
        if (!fluidTank.isEmpty()) {
            // Fluid cargo has no item-form equivalent to drop; it is simply lost, matching the
            // original's behaviour of not attempting to bottle spilled tank contents on drone loss.
        }

        var droneItem = isExpress()
                ? (isChunkLoading() ? ModItems.DRONE_PATROL_EXPRESS_CHUNKLOADING : ModItems.DRONE_PATROL_EXPRESS)
                : (isChunkLoading() ? ModItems.DRONE_PATROL_CHUNKLOADING : ModItems.DRONE_PATROL);
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(droneItem.get(), 1)));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(EXPRESS, tag.getBoolean("express"));
        this.entityData.set(CHUNK_LOADING, tag.getBoolean("chunkLoading"));

        Arrays.fill(items, ItemStack.EMPTY);
        ListTag list = tag.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < items.length) {
                items[slot] = PlatformHooks.itemStackOf(entry, this.level().registryAccess());
            }
        }

        if (tag.contains("fluidTank")) fluidTank.readNBT(tag.getCompound("fluidTank"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("express", isExpress());
        tag.putBoolean("chunkLoading", isChunkLoading());

        ListTag list = new ListTag();
        for (int i = 0; i < items.length; i++) {
            if (!items[i].isEmpty()) {
                CompoundTag entry = PlatformHooks.safeItemSave(items[i], this.level().registryAccess());
                entry.putInt("slot", i);
                list.add(entry);
            }
        }
        tag.put("items", list);
        tag.put("fluidTank", fluidTank.writeNBT(new CompoundTag()));
    }
}
