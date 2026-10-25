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

/** 1:1 {@code ItemModCard} (card_aos / card_qos). Die AoS-Munitionsersparnis kommt mit dem Waffensystem. */
public class ItemModCard extends ItemArmorMod {

    public ItemModCard() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.helmet_only, true, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        if (this == ModItems.CARD_AOS.get()) {
            list.add(line("Top of the line!", ChatFormatting.RED));
            list.add(line("Guns now have a 33% chance to not consume ammo.", ChatFormatting.RED));
        }
        if (this == ModItems.CARD_QOS.get()) {
            list.add(line("Power!", ChatFormatting.RED));
            list.add(line("Adds a 33% chance to tank damage with no cap.", ChatFormatting.RED));
        }
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(stack.getHoverName().copy().withStyle(ChatFormatting.RED));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (this == ModItems.CARD_QOS.get() && entity.getRandom().nextInt(3) == 0 && entity instanceof Player player) {
            HbmPlayerProps.plink(player, net.minecraft.sounds.SoundEvents.ITEM_BREAK, 0.5F, 1.0F + entity.getRandom().nextFloat() * 0.5F);
            event.amount = 0;
            event.canceled = true;
        }
    }
}
