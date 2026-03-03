package com.hbm_m.item.fekal_electric;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyCapabilityProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * @deprecated Используй {@link ModBatteryItem} вместо этого
 */
@Deprecated
public class ItemBattery extends ModBatteryItem {

    public ItemBattery(Properties pProperties, int capacity, int maxReceive, int maxExtract) {
        super(pProperties, capacity, maxReceive, maxExtract);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyCapabilityProvider(stack, this.capacity, this.maxReceive, this.maxExtract);
    }
}