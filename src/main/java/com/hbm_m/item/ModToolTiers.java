package com.hbm_m.item;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.item.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public class ModToolTiers {
    public static final Tier ALLOY = TierSortingRegistry.registerTier(
            new ForgeTier(5, 1500, 5f, 4f, 25,
                    ModTags.Blocks.NEEDS_ALLOY_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
            new ResourceLocation(MainRegistry.MOD_ID, "alloy"), List.of(Tiers.NETHERITE), List.of());

}
