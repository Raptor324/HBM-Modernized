package com.hbm_m.datagen.recipes.custom;

import com.hbm_m.datagen.assets.ModItemTagProvider;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Generates all press recipes (plates, wires, circuits).
 */
public final class PressRecipeGenerator {

    private PressRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        generatePlates(writer);
        generateWires(writer);
        generateCircuits(writer);
    }

    private static void generatePlates(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_IRON.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(Items.IRON_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_iron"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_COPPER.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(Items.COPPER_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_copper"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GOLD.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(Items.GOLD_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_gold"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_STEEL.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.STEEL).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_steel"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_LEAD.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_lead"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_ADVANCED_ALLOY.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_advanced_alloy"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_SATURNITE.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.SATURNITE).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_saturnite"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_SCHRABIDIUM.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.SCHRABIDIUM).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_schrabidium"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_TITANIUM.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.TITANIUM).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_titanium"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_ALUMINUM.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_aluminium"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GUNSTEEL.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.GUNSTEEL).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_gunsteel"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_COMBINE_STEEL.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.COMBINE_STEEL).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_combine_steel"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GUNMETAL.get()))
                .stamp(ModItemTagProvider.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.GUNMETAL).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_gunmetal"));
    }

    private static void generateWires(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_COPPER.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(Items.COPPER_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_copper"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_GOLD.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(Items.GOLD_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_gold"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ADVANCED_ALLOY.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_advanced_alloy"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_aluminium"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_CARBON.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_carbon"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_FINE.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(Items.IRON_INGOT)
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_fine"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.MAGNETIZED_TUNGSTEN).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_magnetized_tungsten"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_RED_COPPER.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.RED_COPPER).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_red_copper"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_SCHRABIDIUM.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.SCHRABIDIUM).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_schrabidium"));
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_TUNGSTEN.get(), 8))
                .stamp(ModItemTagProvider.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_tungsten"));

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.SILICON_CIRCUIT.get()))
                .stamp(ModItemTagProvider.STAMPS_CIRCUIT)
                .material(ModItems.BILLET_SILICON.get())
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silicon_circuit"));
    }


    private static void generateCircuits(Consumer<FinishedRecipe> writer) {

    }
}

