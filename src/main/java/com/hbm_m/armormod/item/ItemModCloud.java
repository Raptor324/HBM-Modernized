package com.hbm_m.armormod.item;

import com.hbm_m.platform.AttributeOps;

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

/** 1:1 {@code ItemModCloud} (bottled_cloud): drei Dashes, +12.5% Tempo. */
public class ItemModCloud extends ItemArmorMod implements IArmorModDash {

    private static final java.util.UUID speed = java.util.UUID.fromString("1d11e63e-28c4-4e14-b09f-fe0bd1be708f");

    public ItemModCloud() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.plate_only, false, true, false, false);
    }

    @Override
    //? if < 1.21.1 {
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
    //?} else {
    /*public com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
    *///?}
        multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, modifier(speed, "CLOUD SPEED", 0.125, AttributeOps.MULTIPLY_TOTAL));
        return multimap;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Grants horizontal dashes", ChatFormatting.WHITE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.RED, stack, " (Dashes)"));
    }

    @Override
    public int getDashes() {
        return 3;
    }
}
