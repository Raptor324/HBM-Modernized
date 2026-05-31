package com.hbm_m.hazard.modifier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HazardModifierFuelRadiation extends HazardModifier {

    float target;

    public HazardModifierFuelRadiation(float target) {
        this.target = target;
    }

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        float wear = stack.getMaxDamage() == 0 ? 0F : (float) stack.getDamageValue() / stack.getMaxDamage();
        double depletion = Math.pow(wear, 0.4D);
        return (float) (level + (this.target - level) * depletion);
    }
}
