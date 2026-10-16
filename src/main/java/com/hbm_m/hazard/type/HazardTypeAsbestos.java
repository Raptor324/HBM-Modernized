package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.config.RadiationConfig;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeAsbestos extends HazardTypeBase {

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        if (RadiationConfig.disableAsbestos) {
            return;
        }

        if (!ArmorRegistry.hasProtection(target, 3, HazardClass.PARTICLE_FINE)) {
            HbmLivingProps.incrementAsbestos(target, (int) Math.min(level, 10));
        } else {
            ArmorUtil.damageGasMaskFilter(target, (int) level);
        }
    }

    @Override
    public void updateEntity(ItemEntity item, float level) {
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack,
            List<HazardModifier> modifiers) {
        addBracketedTrait(list, "trait.asbestos", ChatFormatting.WHITE);
    }
}
