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

/** 1:1 {@code ItemModAuto} (injector_5htp): nimmt bei 5 Digamma 5 weg, Stabilitaet, +20 HP, verbraucht sich. */
public class ItemModAuto extends ItemArmorMod {

    public ItemModAuto() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, false, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Imported from Japsterdam.", ChatFormatting.BLUE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.BLUE, stack, ""));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide) {
            if (HbmLivingProps.getDigamma(entity) >= 5F) {
                ArmorModificationHelper.removeMod(armor, ArmorModificationHelper.extra);
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:item.syringe"), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                HbmLivingProps.setDigamma(entity, HbmLivingProps.getDigamma(entity) - 5F);
                entity.addEffect(new MobEffectInstance(com.hbm_m.effect.ModEffects.STABILITY.get(), 60 * 20, 0));
                entity.heal(20F);
            }
        }
    }
}
