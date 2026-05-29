package com.hbm_m.hazard.transformer;

import java.util.List;

import com.hbm_m.hazard.HazardEntry;

import net.minecraft.world.item.ItemStack;

/** Container/crate radiation aggregation — wired when storage items are ported. */
public class HazardTransformerRadiationContainer extends HazardTransformerBase {

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) { }
}
