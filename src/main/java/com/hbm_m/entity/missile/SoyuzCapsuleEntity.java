package com.hbm_m.entity.missile;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Simplified stand-in for the legacy player-pilotable {@code EntitySoyuzCapsule}:
 * descends at a fixed speed from orbit height and drops its cargo payload on the
 * ground near the launcher's designated target, then removes itself. No parachute
 * animation/physics or player-boarding GUI - that's the separate legacy
 * {@code ItemSoyuz}/capsule-block feature, not part of the launcher itself.
 */
public class SoyuzCapsuleEntity extends Entity {

    private static final double DESCENT_SPEED = 0.6D;

    private final NonNullList<ItemStack> payload = NonNullList.withSize(18, ItemStack.EMPTY);

    public SoyuzCapsuleEntity(EntityType<? extends SoyuzCapsuleEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setPayload(List<ItemStack> items) {
        for (int i = 0; i < items.size() && i < payload.size(); i++) {
            payload.set(i, items.get(i).copy());
        }
    }

    @Override
    public void tick() {
        super.tick();

        setDeltaMovement(0.0D, -DESCENT_SPEED, 0.0D);
        move(MoverType.SELF, getDeltaMovement());

        if (level().isClientSide) {
            return;
        }

        if (this.onGround() || getY() < level().getMinBuildHeight()) {
            for (ItemStack stack : payload) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level(), getX(), getY(), getZ(), stack);
                }
            }
            this.discard();
        }
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

    
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ListTag list = tag.getList("payload", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < payload.size()) {
                payload.set(slot, ItemStack.of(itemTag));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < payload.size(); i++) {
            ItemStack stack = payload.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("payload", list);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500_000.0D * 500_000.0D;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
