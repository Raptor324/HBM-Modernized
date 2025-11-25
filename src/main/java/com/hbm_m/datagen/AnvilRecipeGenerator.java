package com.hbm_m.datagen;

import com.hbm_m.block.AnvilTier;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public final class AnvilRecipeGenerator {
    private AnvilRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerTieredRecipes(writer);
    }

    private static void registerTieredRecipes(Consumer<FinishedRecipe> writer) {
        registerTierRecipe(writer, "iron_metal_rod",
                stack(ModItems.PLATE_IRON, 2),
                stack(ModItems.INSULATOR),
                stack(ModItems.METAL_ROD, 2),
                AnvilTier.IRON);

        registerTierRecipe(writer, "steel_combat_axe",
                stack(ModItems.PLATE_STEEL, 2),
                stack(ModItems.PLATE_IRON),
                stack(ModItems.STEEL_AXE),
                AnvilTier.STEEL);

        registerTierRecipe(writer, "oil_detector",
                stack(ModItems.PLATE_COPPER, 2),
                stack(ModItems.PLATE_ALUMINUM),
                stack(ModItems.OIL_DETECTOR),
                AnvilTier.OIL);

        registerTierRecipe(writer, "nuclear_radaway",
                stack(ModItems.PLATE_LEAD, 2),
                stack(ModItems.SULFUR, 3),
                stack(ModItems.RADAWAY, 2),
                AnvilTier.NUCLEAR);

        registerTierRecipe(writer, "rbmk_battery_advanced",
                stack(ModItems.BATTERY),
                stack(ModItems.PLATE_TITANIUM, 2),
                stack(ModItems.BATTERY_ADVANCED),
                AnvilTier.RBMK);

        registerTierRecipe(writer, "fusion_quantum_chip",
                stack(ModItems.SILICON_CIRCUIT),
                stack(ModItems.BISMOID_CIRCUIT),
                stack(ModItems.QUANTUM_CHIP),
                AnvilTier.FUSION);

        registerTierRecipe(writer, "particle_battery_spark",
                stack(ModItems.BATTERY_SCHRABIDIUM),
                stack(ModItems.BATTERY_LITHIUM_CELL),
                stack(ModItems.BATTERY_SPARK),
                AnvilTier.PARTICLE);

        registerTierRecipe(writer, "gerald_quantum_computer",
                stack(ModItems.CONTROLLER_ADVANCED),
                stack(ModItems.QUANTUM_CHIP),
                stack(ModItems.QUANTUM_COMPUTER),
                AnvilTier.GERALD);

        registerTierRecipe(writer, "murky_spark_cell_power",
                stack(ModItems.BATTERY_SPARK_CELL_1000),
                stack(ModItems.QUANTUM_COMPUTER),
                stack(ModItems.BATTERY_SPARK_CELL_POWER),
                AnvilTier.MURKY);
    }

    private static void registerTierRecipe(Consumer<FinishedRecipe> writer, String name,
                                           ItemStack inputA, ItemStack inputB, ItemStack output,
                                           AnvilTier tier) {
        AnvilRecipeBuilder.anvilRecipe(inputA, inputB, output, tier)
                .withOverlay(AnvilRecipe.OverlayType.CONSTRUCTION)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "anvil/" + name));
    }

    private static ItemStack stack(RegistryObject<Item> item) {
        return stack(item, 1);
    }

    private static ItemStack stack(RegistryObject<Item> item, int count) {
        return new ItemStack(item.get(), count);
    }
}

