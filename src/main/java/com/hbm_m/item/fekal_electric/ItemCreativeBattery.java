package com.hbm_m.item.fekal_electric;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemCreativeBattery extends ModBatteryItem {

    public ItemCreativeBattery(Properties pProperties) {
        super(pProperties.rarity(Rarity.EPIC).stacksTo(1), Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.hbm_m.creative_battery_desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        pTooltipComponents.add(Component.translatable("tooltip.hbm_m.creative_battery_flavor")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        return false;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        return 13; // Всегда полная
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0xFF00FF; // Фиолетовый для креатива
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // На Fabric батарейки читаются напрямую из NBT; на Forge capability-адаптер тоже читает NBT.
        // Поэтому для "креативной" батареи просто поддерживаем заряд на максимуме.
        if (ModBatteryItem.getEnergy(stack) != this.capacity) {
            ModBatteryItem.setEnergy(stack, this.capacity);
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}