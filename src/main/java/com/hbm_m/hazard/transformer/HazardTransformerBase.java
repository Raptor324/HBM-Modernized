package com.hbm_m.hazard.transformer;

import java.util.List;

import com.hbm_m.hazard.HazardEntry;

import net.minecraft.world.item.ItemStack;

public abstract class HazardTransformerBase {

    public abstract void transformPre(ItemStack stack, List<HazardEntry> entries);

    public abstract void transformPost(ItemStack stack, List<HazardEntry> entries);
}
