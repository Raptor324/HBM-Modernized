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

/** 1:1 {@code ItemModBattery}: Kapazitaetsmultiplikator fuer Energie-Ruestung. */
public class ItemModBattery extends ItemArmorMod {

    public double mod;

    public ItemModBattery(double mod) {
        super(new Properties().stacksTo(1), ArmorModificationHelper.battery, true, true, true, true);
        this.mod = mod;
    }
}
