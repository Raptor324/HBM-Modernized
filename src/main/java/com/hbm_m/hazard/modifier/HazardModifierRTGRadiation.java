package com.hbm_m.hazard.modifier;

import com.hbm_m.item.machine.ItemRTGPellet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code HazardModifierRTGRadiation}: scales RTG pellet radiation toward {@code target} as it decays. */
public class HazardModifierRTGRadiation extends HazardModifier {

    float target;

    public HazardModifierRTGRadiation(float target) {
        this.target = target;
    }

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        if (stack.getItem() instanceof ItemRTGPellet fuel && fuel.doesDecay() && fuel.getMaxLifespan() > 0L) {
            double depletion = 1.0D - ((double) fuel.getLifespan(stack) / (double) fuel.getMaxLifespan());
            level = (float) (level + (this.target - level) * depletion);
        }
        return level;
    }
}
