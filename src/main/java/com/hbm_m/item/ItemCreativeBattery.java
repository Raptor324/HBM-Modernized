package com.hbm_m.item;

import com.hbm_m.capability.EnergyCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCreativeBattery extends ItemBattery {
    public ItemCreativeBattery(Properties pProperties) {
        super(pProperties.rarity(Rarity.EPIC), 0, 0, 0);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        // Основная подсказка, например, "Provides infinite power."
        pTooltipComponents.add(Component.translatable("tooltip.hbm_m.creative_battery_desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        
        // Дополнительная "пасхалка"
        pTooltipComponents.add(Component.translatable("tooltip.hbm_m.creative_battery_flavor")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    // --- НОВЫЙ КОД ДЛЯ УДАЛЕНИЯ ПОЛОСКИ ---

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        // Никогда не показывать полоску прочности/заряда
        return false;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        // Так как полоска не видна, ширина не важна, но 0 - самое логичное значение.
        return 0;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // Возвращаем анонимный класс, так как логика уникальна
        return new EnergyCapabilityProvider(stack, Integer.MAX_VALUE, 0, Integer.MAX_VALUE) {
             @Override
             public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
                 if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) {
                     return net.minecraftforge.common.util.LazyOptional.of(() -> new IEnergyStorage() {
                        @Override public int receiveEnergy(int max, boolean s) { return 0; }
                        @Override public int extractEnergy(int max, boolean s) { return max; }
                        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
                        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
                        @Override public boolean canExtract() { return true; }
                        @Override public boolean canReceive() { return false; }
                     }).cast();
                 }
                 return net.minecraftforge.common.util.LazyOptional.empty();
             }
        };
    }
}