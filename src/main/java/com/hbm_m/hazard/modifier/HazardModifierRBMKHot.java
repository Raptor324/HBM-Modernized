package com.hbm_m.hazard.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** RBMK hull heat → hot hazard — logic restored when {@code ItemRBMKRod} is ported. */
public class HazardModifierRBMKHot extends HazardModifier {

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        return 0;
    }
}
