package com.hbm_m.hazard.modifier;

import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 port of the original's {@code HazardModifierRBMKHot}: carried/worn RBMK fuel burns the
 *  holder proportionally to its hull heat, up to a 60-tick fire cap. */
public class HazardModifierRBMKHot extends HazardModifier {

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        level = 0;

        if (stack.getItem() instanceof RBMKRodItem) {
            double heat = RBMKRodItem.getHullHeat(stack);
            level = (float) Math.min(Math.ceil((heat - 100) / 10D), 60);
        }

        return level;
    }
}
