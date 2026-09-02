package com.hbm_m.hazard.transformer;

import java.util.List;

import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class HazardTransformerRadiationNBT extends HazardTransformerBase {

    public static final String RAD_KEY = "hfrHazRadiation";

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag != null && tag.contains(RAD_KEY)) {
            entries.add(new HazardEntry(HazardRegistry.RADIATION, tag.getFloat(RAD_KEY)));
        }
    }
}
