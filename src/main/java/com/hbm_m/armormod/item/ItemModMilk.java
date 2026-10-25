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

/** 1:1 {@code ItemModMilk} (spider_milk): entfernt schaedliche Effekte. */
public class ItemModMilk extends ItemArmorMod {

    public ItemModMilk() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Removes bad potion effects", ChatFormatting.WHITE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.WHITE, stack, " (Removes bad potion effects)"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        List<net.minecraft.world.effect.MobEffect> bad = new java.util.ArrayList<>();
        for (MobEffectInstance eff : entity.getActiveEffects()) {
            //? if < 1.21.1 {
            net.minecraft.world.effect.MobEffect e = eff.getEffect();
            //?} else {
            /*net.minecraft.world.effect.MobEffect e = eff.getEffect().value();
            *///?}
            if (e.getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) bad.add(e);
        }
        //? if < 1.21.1 {
        for (net.minecraft.world.effect.MobEffect e : bad) entity.removeEffect(e);
        //?} else {
        /*for (net.minecraft.world.effect.MobEffect e : bad) entity.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e));
        *///?}
    }
}
