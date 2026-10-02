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

/** 1:1 {@code ItemModServos} (servo_set / servo_set_desh). */
public class ItemModServos extends ItemArmorMod {

    public ItemModServos() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.servos, false, true, true, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        if (this == ModItems.SERVO_SET.get()) {
            list.add(line("Chestplate: Haste I / Damage +50%", ChatFormatting.DARK_PURPLE));
            list.add(line("Leggings: Speed +25% / Jump II", ChatFormatting.DARK_PURPLE));
        }
        if (this == ModItems.SERVO_SET_DESH.get()) {
            list.add(line("Chestplate: Haste III / Damage +150%", ChatFormatting.DARK_PURPLE));
            list.add(line("Leggings: Speed +50% / Jump III", ChatFormatting.DARK_PURPLE));
        }
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        int t = armorType(armor);
        if (t == 1) {
            if (this == ModItems.SERVO_SET.get()) list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (Haste I / Damage +50%)"));
            if (this == ModItems.SERVO_SET_DESH.get()) list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (Haste III / Damage +150%)"));
        }
        if (t == 2) {
            if (this == ModItems.SERVO_SET.get()) list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (Speed +25% / Jump II)"));
            if (this == ModItems.SERVO_SET_DESH.get()) list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (Speed +50% / Jump III)"));
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        int t = armorType(armor);
        if (t == 1) {
            if (this == ModItems.SERVO_SET.get()) entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0));
            if (this == ModItems.SERVO_SET_DESH.get()) entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 2));
        }
        if (t == 2) {
            if (this == ModItems.SERVO_SET.get()) entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1));
            if (this == ModItems.SERVO_SET_DESH.get()) entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 2));
        }
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
        int t = armorType(armor);
        var op = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL;
        if (t == 1) {
            if (this == ModItems.SERVO_SET.get())
                multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, modifier(armor, "NTM Armor Mod Servos", 0.5, op));
            if (this == ModItems.SERVO_SET_DESH.get())
                multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, modifier(armor, "NTM Armor Mod Servos", 1.5, op));
        }
        if (t == 2) {
            if (this == ModItems.SERVO_SET.get())
                multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, modifier(armor, "NTM Armor Mod Servos", 0.25, op));
            if (this == ModItems.SERVO_SET_DESH.get())
                multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, modifier(armor, "NTM Armor Mod Servos", 0.5, op));
        }
        return multimap;
    }
}
