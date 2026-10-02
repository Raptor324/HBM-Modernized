package com.hbm_m.armormod.item;

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
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemModMedal} (medal_liquidator): -0.5 RAD je Tick. */
public class ItemModMedal extends ItemArmorMod {

    public ItemModMedal() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.plate_only, true, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("-10 RAD/s", ChatFormatting.GOLD));
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.GOLD, stack, " (-10 RAD/s)"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide) {
            float rad = HbmLivingProps.getRadiation(entity);
            rad -= 0.5F;
            HbmLivingProps.setRadiation(entity, Math.max(rad, 0));
        }
    }
}
