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

/** 1:1 {@code ItemModIron} (cladding_iron): +0.5 Rueckstossresistenz. */
public class ItemModIron extends ItemArmorMod {

    public ItemModIron() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.cladding, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("+0.5 knockback resistance", ChatFormatting.WHITE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.WHITE, stack, " (+0.5 knockback resistence)"));
    }

    @Override
    //? if < 1.21.1 {
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
    //?} else {
    /*public com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
    *///?}
        multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, modifier(armor, "NTM Armor Mod Knockback", 0.5, AttributeOps.ADDITION));
        return multimap;
    }
}
