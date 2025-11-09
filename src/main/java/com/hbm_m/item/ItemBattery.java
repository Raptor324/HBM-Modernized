package com.hbm_m.item;

import com.hbm_m.api.energy.EnergyCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

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