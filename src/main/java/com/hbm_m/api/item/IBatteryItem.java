package com.hbm_m.api.item;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code api.hbm.energymk2.IBatteryItem}: Gegenstaende mit eigenem Ladestand ("charge"). */
public interface IBatteryItem {

    void chargeBattery(ItemStack stack, long i);
    void setCharge(ItemStack stack, long i);
    void dischargeBattery(ItemStack stack, long i);
    long getCharge(ItemStack stack);
    long getMaxCharge(ItemStack stack);
    long getChargeRate(ItemStack stack);
    long getDischargeRate(ItemStack stack);

    /** Returns a string for the NBT tag name of the long storing power */
    default String getChargeTagName() {
        return "charge";
    }
}
