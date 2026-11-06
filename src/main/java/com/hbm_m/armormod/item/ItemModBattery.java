package com.hbm_m.armormod.item;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Модификация батареи — увеличивает ёмкость силовой брони (аналог 1.7.10 {@code ItemModBattery}).
 */
public class ItemModBattery extends ItemArmorMod implements ITooltipProvider {

    public double mod;

    public ItemModBattery(double mod) {
        super(new Properties().stacksTo(1), ArmorModificationHelper.battery, true, true, true, true);
        this.mod = mod;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.mod.battery.description").withStyle(ChatFormatting.GRAY));
        super.appendHbmTooltip(stack, level, tooltip, flag);
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
