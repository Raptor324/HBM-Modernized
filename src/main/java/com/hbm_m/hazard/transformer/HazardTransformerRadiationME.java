package com.hbm_m.hazard.transformer;

import java.util.List;

import com.hbm_m.hazard.HazardEntry;

import net.minecraft.world.item.ItemStack;

/** AE2 cell radiation aggregation — wired when ME compat is ported. */
public class HazardTransformerRadiationME extends HazardTransformerBase {

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) { }
}
