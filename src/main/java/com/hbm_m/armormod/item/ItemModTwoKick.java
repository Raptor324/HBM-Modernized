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

/** 1:1 {@code ItemModTwoKick} (ballistic_gauntlet). Die Schrotschlaege selbst kommen mit dem Waffensystem. */
public class ItemModTwoKick extends ItemArmorMod {

    public ItemModTwoKick() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.servos, false, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("\"I've had worse\"", ChatFormatting.ITALIC));
        list.add(line("Punches fire 12 gauge shells", ChatFormatting.YELLOW));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.YELLOW, stack, " (Shotgun punches)"));
    }
}
