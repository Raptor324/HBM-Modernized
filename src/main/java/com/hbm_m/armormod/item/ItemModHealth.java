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

/** 1:1 {@code ItemModHealth} (Herzteile, black_diamond): +Lebenspunkte. */
public class ItemModHealth extends ItemArmorMod {

    float health;

    public ItemModHealth(float health) {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, false, true, false, false);
        this.health = health;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("+" + (Math.round(health * 10) * 0.1) + " health", blink(ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE)));
        list.add(Component.empty());
        if (this == ModItems.BLACK_DIAMOND.get()) {
            list.add(line("Nostalgia", ChatFormatting.DARK_GRAY));
            list.add(Component.empty());
        }
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(blink(ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE), stack, " (+" + (Math.round(health * 10) * 0.1) + " health)"));
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
        multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, modifier(armor, "NTM Armor Mod Health", health, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return multimap;
    }
}
