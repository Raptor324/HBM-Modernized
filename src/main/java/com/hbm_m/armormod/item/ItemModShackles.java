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

/** 1:1 {@code ItemModShackles}: unbegrenzte Wiederbelebung gegen Strahlung (HbmForgeEvents.onEntityDeathFirst). */
public class ItemModShackles extends ItemArmorMod {

    public ItemModShackles() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, false, false, true, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("You will speak when I ask you to.", ChatFormatting.RED));
        list.add(line("You will eat when I tell you to.", ChatFormatting.RED));
        list.add(Component.literal("You will die when I allow you to.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        list.add(Component.empty());
        list.add(line("\u221e revives left", ChatFormatting.GOLD));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.GOLD, stack, " (\u221e revives left)"));
    }
}
