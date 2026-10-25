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

/** 1:1 {@code ItemModCharm} (protection_charm / meteor_charm). */
public class ItemModCharm extends ItemArmorMod {

    public ItemModCharm() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.helmet_only, true, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("You feel blessed.", ChatFormatting.AQUA));
        if (this == ModItems.PROTECTION_CHARM.get()) {
            list.add(line("Diverts meteors away from the player.", ChatFormatting.AQUA));
            list.add(line("Meteors no longer destroy blocks.", ChatFormatting.AQUA));
            list.add(line("Halves broadcaster damage", ChatFormatting.AQUA));
        }
        if (this == ModItems.METEOR_CHARM.get()) {
            list.add(line("Disables meteorite spawning.", ChatFormatting.AQUA));
            list.add(line("Negates broadcaster damage", ChatFormatting.AQUA));
        }
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.GOLD, stack, ""));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (event.source.is(com.hbm_m.damagesource.ModDamageTypes.BROADCAST)) {
            if (this == ModItems.PROTECTION_CHARM.get())
                event.amount *= 0.5F;
            if (this == ModItems.METEOR_CHARM.get())
                event.amount = 0F;
        }
    }
}
