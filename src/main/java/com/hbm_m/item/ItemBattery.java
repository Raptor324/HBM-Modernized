package com.hbm_m.item;

import com.hbm_m.capability.EnergyCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemBattery extends Item {
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public ItemBattery(Properties pProperties, int capacity, int maxReceive, int maxExtract) {
        super(pProperties.stacksTo(1));
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return pStack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> Math.round(13.0F * storage.getEnergyStored() / storage.getMaxEnergyStored()))
                .orElse(0);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return 0x44B027; // Зеленый цвет из HBM
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            pTooltipComponents.add(Component.translatable("tooltip.hbm_m.energy_stored",
                Component.literal(String.format("%,d", storage.getEnergyStored())).withStyle(ChatFormatting.GREEN),
                Component.literal(String.format("%,d", storage.getMaxEnergyStored())).withStyle(ChatFormatting.GRAY)
            ));
        });
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // Возвращаем наш новый провайдер
        return new EnergyCapabilityProvider(stack, this.capacity, this.maxReceive, this.maxExtract);
    }
}