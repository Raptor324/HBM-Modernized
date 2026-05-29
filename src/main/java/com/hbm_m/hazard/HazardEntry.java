package com.hbm_m.hazard;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.hazard.type.HazardTypeBase;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HazardEntry {

    public HazardTypeBase type;
    public float baseLevel;
    public final List<HazardModifier> mods = new ArrayList<>();

    public HazardEntry(HazardTypeBase type) {
        this(type, 1F);
    }

    public HazardEntry(HazardTypeBase type, float level) {
        this.type = type;
        this.baseLevel = level;
    }

    public HazardEntry addMod(HazardModifier mod) {
        this.mods.add(mod);
        return this;
    }

    public void applyHazard(ItemStack stack, LivingEntity entity) {
        type.onUpdate(entity, HazardModifier.evalAllModifiers(stack, entity, baseLevel, mods), stack);
    }

    public HazardTypeBase getType() {
        return this.type;
    }
}
