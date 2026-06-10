package com.hbm_m.hazard.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** RTG pellet depletion scaling — logic restored when {@code ItemRTGPellet} is ported. */
public class HazardModifierRTGRadiation extends HazardModifier {

    float target;

    public HazardModifierRTGRadiation(float target) {
        this.target = target;
    }

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        return level;
    }
}
