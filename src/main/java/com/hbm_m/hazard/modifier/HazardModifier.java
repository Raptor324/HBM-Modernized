package com.hbm_m.hazard.modifier;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public abstract class HazardModifier {

    public abstract float modify(ItemStack stack, LivingEntity holder, float level);

    public static float evalAllModifiers(ItemStack stack, LivingEntity entity, float level, List<HazardModifier> mods) {
        for (HazardModifier mod : mods) {
            level = mod.modify(stack, entity, level);
        }
        return level;
    }
}
