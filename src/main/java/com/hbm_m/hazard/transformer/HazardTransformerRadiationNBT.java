package com.hbm_m.hazard.transformer;

import java.util.List;

import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardRegistry;

import net.minecraft.world.item.ItemStack;

public class HazardTransformerRadiationNBT extends HazardTransformerBase {

    public static final String RAD_KEY = "hfrHazRadiation";

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        if (stack.hasTag() && stack.getTag().contains(RAD_KEY)) {
            entries.add(new HazardEntry(HazardRegistry.RADIATION, stack.getTag().getFloat(RAD_KEY)));
        }
    }
}
