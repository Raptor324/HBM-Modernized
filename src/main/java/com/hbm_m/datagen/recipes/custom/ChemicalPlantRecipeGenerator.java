package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Groups all chemical plant recipes (ported from 1.7.10 {@code ChemicalPlantRecipes.registerDefaults()}).
 *
 * <p>Рецепты пишутся в data-pack как custom recipes {@code hbm_m:chemical_plant}.</p>
 */
public final class ChemicalPlantRecipeGenerator {

    private ChemicalPlantRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerRegularFluids(writer);
        registerOils(writer);
        registerAcids(writer);
        registerCoolants(writer);
        registerSteam(writer);
        registerOxyhydrogen(writer);
        registerDeuterium(writer);
        registerUf6(writer);
        registerSchrabidium(writer);
        registerKevlar(writer);

        // TODO (follow-up in this same todo): перенести оставшиеся блоки рецептов из 1.7.10
        // (бетоны/взрывчатка/стекло/ядерная переработка/и т.д.).
    }

    private static void registerRegularFluids(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
            .addItemInput(ModItems.getPowders(ModPowders.COAL).get(), 1)
            .addFluidInput(ModFluids.WATER.getSource(), 8_000)
            .addFluidOutput(ModFluids.HYDROGEN.getSource(), 500)
            .save(writer, "chemplant/chem_hydrogen");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
            .addFluidInput(ModFluids.AIR.getSource(), 8_000)
            .addFluidOutput(ModFluids.OXYGEN.getSource(), 500)
            .save(writer, "chemplant/chem_oxygen");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(300, 1_000)
            .addFluidInput(ModFluids.AIR.getSource(), 16_000)
            .addFluidOutput(ModFluids.XENON.getSource(), 50)
            .save(writer, "chemplant/chem_xenon");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
            .addFluidInput(ModFluids.GAS.getSource(), 1_000)
            .addFluidOutput(ModFluids.CARBONDIOXIDE.getSource(), 1_000)
            .save(writer, "chemplant/chem_co2");
    }

    private static void registerOils(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .addItemInput(Items.SUGAR, 10)
            .addFluidOutput(ModFluids.ETHANOL.getSource(), 1_000)
            .save(writer, "chemplant/chem_ethanol");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
            .addFluidInput(ModFluids.BIOGAS.getSource(), 1_500)
            .addFluidInput(ModFluids.ETHANOL.getSource(), 250)
            .addFluidOutput(ModFluids.BIOFUEL.getSource(), 1_000)
            .save(writer, "chemplant/chem_biofuel");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .addFluidInput(ModFluids.SLOP.getSource(), 1_000)
            .addFluidOutput(ModFluids.RECLAIMED.getSource(), 800)
            .save(writer, "chemplant/chem_reoil");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .addFluidInput(ModFluids.NAPHTHA.getSource(), 1_000)
            .addFluidOutput(ModFluids.GASOLINE.getSource(), 800)
            .save(writer, "chemplant/chem_gasoline");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .addFluidInput(ModFluids.COALCREOSOTE.getSource(), 1_000)
            .addFluidOutput(ModFluids.LUBRICANT.getSource(), 1_000)
            .withBlueprintPool("alt.lube")
            .save(writer, "chemplant/chem_coallube");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .addFluidInput(ModFluids.HEAVYOIL.getSource(), 2_000)
            .addFluidOutput(ModFluids.LUBRICANT.getSource(), 1_000)
            .withBlueprintPool("alt.lube")
            .save(writer, "chemplant/chem_heavylube");
    }

    private static void registerAcids(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.PEROXIDE.getSource(), 1_000)
            .save(writer, "chemplant/chem_peroxide");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .addItemInput(ModItems.SULFUR.get(), 1)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 1_000)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.SULFURIC_ACID.getSource(), 2_000)
            .save(writer, "chemplant/chem_sulfuricacid");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .addItemInput(ModItems.CRYSTAL_NITER.get(), 1)
            .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 500)
            .addFluidOutput(ModFluids.NITRIC_ACID.getSource(), 1_000)
            .save(writer, "chemplant/chem_nitricacid");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 5_000)
            .addFluidInput(ModFluids.AIR.getSource(), 8_000)
            .addFluidInput(ModFluids.WATER.getSource(), 2_000)
            .addFluidOutput(ModFluids.NITRIC_ACID.getSource(), 1_000)
            .withBlueprintPool("alt.birkeland")
            .save(writer, "chemplant/chem_birkeland");
    }

    private static void registerCoolants(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
            .addItemInput(ModItems.FLUORITE.get(), 1)
            .addFluidInput(ModFluids.PETROLEUM.getSource(), 1_000)
            .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
            .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 1_000)
            .save(writer, "chemplant/chem_perfluoromethyl");
    }

    private static void registerSteam(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(10, 50)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.STEAM.getSource(), 1_000)
            .save(writer, "chemplant/chem_steam");
    }

    private static void registerOxyhydrogen(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
            .addFluidInput(ModFluids.HYDROGEN.getSource(), 500)
            .addFluidInput(ModFluids.OXYGEN.getSource(), 250)
            .addFluidOutput(ModFluids.OXYHYDROGEN.getSource(), 500)
            .save(writer, "chemplant/chem_oxyhydrogen");
    }

    private static void registerDeuterium(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1_000)
            .addFluidInput(ModFluids.HEAVYWATER.getSource(), 2_000)
            .addFluidOutput(ModFluids.DEUTERIUM.getSource(), 500)
            .save(writer, "chemplant/chem_deuterium");
    }

    private static void registerUf6(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 500)
            .addItemInput(ModItems.getPowder(ModIngots.URANIUM).get(), 1)
            .addItemInput(ModItems.FLUORITE.get(), 4)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModItems.SULFUR.get(), 2))
            .addFluidOutput(ModFluids.UF6.getSource(), 1_200)
            .save(writer, "chemplant/chem_uf6");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 500)
            .addItemInput(ModItems.getPowder(ModIngots.PLUTONIUM).get(), 1)
            .addItemInput(ModItems.FLUORITE.get(), 3)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.PUF6.getSource(), 900)
            .save(writer, "chemplant/chem_puf6");
    }

    private static void registerSchrabidium(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 5_000)
            .addItemInput(ModItems.getPowder(ModIngots.SCHRABIDIUM).get(), 1)
            .addItemInput(ModItems.SULFUR.get(), 2)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 2_000)
            .addFluidOutput(ModFluids.SAS3.getSource(), 1_000)
            .save(writer, "chemplant/chem_sas3");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 5_000)
            .addFluidInput(ModFluids.SAS3.getSource(), 2_000)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 2_000)
            .addFluidOutput(ModFluids.SCHRABIDIC.getSource(), 2_000)
            .save(writer, "chemplant/chem_schrabidic");
    }

    private static void registerKevlar(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 300)
            .addFluidInput(ModFluids.AROMATICS.getSource(), 200)
            .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
            .addFluidInput(ModFluids.CHLORINE.getSource(), 100)
            .addItemOutput(new ItemStack(ModItems.PLATE_KEVLAR.get(), 4))
            .save(writer, "chemplant/chem_kevlar");
    }
}
//?}
