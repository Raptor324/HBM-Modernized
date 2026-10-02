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

/** 1:1 {@code ItemModShield} (australium_iii): erhoeht den maximalen Schild. */
public class ItemModShield extends ItemArmorMod implements IShieldMod {

    public final float shield;

    public ItemModShield(float shield) {
        super(new Properties().stacksTo(1), ArmorModificationHelper.kevlar, false, true, false, false);
        this.shield = shield;
    }

    @Override
    public float getShield() {
        return shield;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("+" + (Math.round(shield * 10) * 0.1) + " shield", blink(ChatFormatting.YELLOW, ChatFormatting.GOLD)));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(blink(ChatFormatting.YELLOW, ChatFormatting.GOLD), stack, " (+" + (Math.round(shield * 10) * 0.1) + " health)"));
    }
}
