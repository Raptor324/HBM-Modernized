package com.hbm_m.armormod.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Модификация батареи — увеличивает ёмкость силовой брони (аналог 1.7.10 {@code ItemModBattery}).
 */
public class ItemModBattery extends ItemArmorMod {

    public double mod;

    public ItemModBattery(double mod) {
        super(new Item.Properties(), ArmorModificationHelper.battery);
        this.mod = mod;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.mod.battery.description").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        if (mod >= 2.0D) {
            return List.of(Component.translatable("tooltip.hbm_m.mod.battery_mk3.effect"));
        }
        if (mod >= 1.5D) {
            return List.of(Component.translatable("tooltip.hbm_m.mod.battery_mk2.effect"));
        }
        return List.of(Component.translatable("tooltip.hbm_m.mod.battery.effect"));
    }
}
