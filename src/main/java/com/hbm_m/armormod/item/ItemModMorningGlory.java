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

/** 1:1 {@code ItemModMorningGlory}: 5% Resistenz V bei Treffer, Wither-Immunitaet. */
public class ItemModMorningGlory extends ItemArmorMod {

    public ItemModMorningGlory() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("5% chance to apply resistance when hit, wither immunity", ChatFormatting.LIGHT_PURPLE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.LIGHT_PURPLE, stack, " (5% for resistance, wither immunity)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (!entity.level().isClientSide && entity.level().random.nextInt(20) == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide && entity.hasEffect(MobEffects.WITHER)) {
            entity.removeEffect(MobEffects.WITHER);
        }
    }
}
