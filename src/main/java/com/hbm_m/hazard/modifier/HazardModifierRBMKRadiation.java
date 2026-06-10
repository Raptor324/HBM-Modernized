package com.hbm_m.hazard.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** RBMK rod/pellet radiation scaling — logic restored when RBMK fuel items are ported. */
public class HazardModifierRBMKRadiation extends HazardModifier {

    float target;
    boolean linear = false;

    public HazardModifierRBMKRadiation(float target, boolean linear) {
        this.target = target;
        this.linear = linear;
    }

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        return level;
    }
}
