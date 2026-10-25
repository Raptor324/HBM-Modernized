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

/** 1:1 {@code ItemModRevive} (scrumpy, wild_p): Wiederbelebungen (HbmForgeEvents.onEntityDeathFirst). */
public class ItemModRevive extends ItemArmorMod {

    public ItemModRevive(int durability) {
        super(new Properties().durability(durability), ArmorModificationHelper.extra, false, false, true, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        if (this == ModItems.SCRUMPY.get()) {
            list.add(line("But how did you survive?", ChatFormatting.GOLD));
            list.add(line("I was drunk.", ChatFormatting.RED));
        }
        if (this == ModItems.WILD_P.get()) {
            list.add(Component.literal("Explosive ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Reactive ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("Plot ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Armor").withStyle(ChatFormatting.RED)));
        }
        list.add(Component.empty());
        list.add(line((stack.getMaxDamage() - stack.getDamageValue()) + " revives left", ChatFormatting.GOLD));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.GOLD, stack, " (" + (stack.getMaxDamage() - stack.getDamageValue()) + " revives left)"));
    }
}
