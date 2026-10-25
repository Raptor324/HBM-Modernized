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

/** 1:1 {@code ItemModBandaid}: 3% Chance auf volle Heilung bei Schaden. */
public class ItemModBandaid extends ItemArmorMod {

    public ItemModBandaid() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("3% chance for full heal when damaged", ChatFormatting.RED));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.RED, stack, " (3% chance for full heal)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (entity.level().random.nextInt(100) < 3) {
            event.amount = 0;
            entity.heal(entity.getMaxHealth());
        }
    }
}
