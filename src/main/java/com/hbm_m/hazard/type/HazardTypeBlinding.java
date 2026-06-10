package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.config.RadiationConfig;
import com.hbm_m.hazard.modifier.HazardModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeBlinding extends HazardTypeBase {

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        if (RadiationConfig.disableBlinding) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) Math.ceil(level), 0));
    }

    @Override
    public void updateEntity(ItemEntity item, float level) {
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack,
            List<HazardModifier> modifiers) {
        addBracketedTrait(list, "trait.blinding", ChatFormatting.DARK_AQUA);
    }
}
