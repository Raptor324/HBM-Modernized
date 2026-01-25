package com.hbm_m.item.custom.tools_and_armor;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public class ModToolTiers {
    public static final Tier ALLOY = TierSortingRegistry.registerTier(
            new ForgeTier(4, 1500, 6f, 6f, 25,
                    ModTags.Blocks.NEEDS_ALLOY_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "alloy"), List.of(Tiers.NETHERITE), List.of());

    public static final Tier STEEL = TierSortingRegistry.registerTier(
            new ForgeTier(3, 600, 4f, 4f, 18,
                    ModTags.Blocks.NEEDS_STEEL_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "steel"), List.of(Tiers.IRON), List.of());

    public static final Tier STARMETAL = TierSortingRegistry.registerTier(
            new ForgeTier(5, 19000, 9f, 8f, 25,
                    ModTags.Blocks.NEEDS_STARMETAL_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "starmetal"), List.of(Tiers.IRON), List.of());

    public static final Tier TITANIUM = TierSortingRegistry.registerTier(
            new ForgeTier(3, 750, 3.25f, 3f, 15,
                    ModTags.Blocks.NEEDS_TITANIUM_TOOL, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "titanium"), List.of(Tiers.IRON), List.of());

}
