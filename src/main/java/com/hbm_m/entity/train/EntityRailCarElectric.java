package com.hbm_m.entity.train;

import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.item.ModItems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityRailCarElectric}: Triebwagen mit Akku, der aus einem Batterieplatz geladen wird. */
public abstract class EntityRailCarElectric extends EntityRailCarRidable {

    /** Original DataWatcher 3 */
    private static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(EntityRailCarElectric.class, EntityDataSerializers.INT);

    public EntityRailCarElectric(EntityType<?> type, Level world) {
        super(type, world);
    }

    public abstract int getMaxPower();
    public abstract int getPowerConsumption();

    public boolean hasChargeSlot() { return false; }
    public int getChargeSlot() { return 0; }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(POWER, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0);
    }
    *///?}

    @Override public boolean canAccelerate() {
        return true;
        //return this.getPower() >= this.getPowerConsumption();
    }

    @Override public void consumeFuel() {
        //this.setPower(this.getPower() - this.getPowerConsumption());
    }

    public void setPower(int power) {
        this.entityData.set(POWER, power);
    }

    public int getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {

            if (this.hasChargeSlot()) {
                ItemStack stack = this.getItem(this.getChargeSlot());

                if (!stack.isEmpty() && stack.getItem() instanceof IBatteryItem battery) {
                    int powerNeeded = this.getMaxPower() - this.getPower();
                    long powerProvided = Math.min(battery.getDischargeRate(stack), battery.getCharge(stack));
                    int powerTransfered = (int) Math.min(powerNeeded, powerProvided);

                    if (powerTransfered > 0) {
                        battery.dischargeBattery(stack, powerTransfered);
                        this.setPower(this.getPower() + powerTransfered);
                    }
                } else if (!stack.isEmpty()) {
                    if (stack.getItem() == ModItems.CREATIVE_BATTERY.get()) {
                        this.setPower(this.getMaxPower());
                    }
                }
            }
        }
    }
}
