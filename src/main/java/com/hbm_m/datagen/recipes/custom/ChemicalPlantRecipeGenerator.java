package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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
        registerBasicBlocks(writer);
        registerExplosives(writer);
        registerDhc(writer);
        registerConstructionMaterials(writer);
        registerFuelsAndElectrodes(writer);
        registerPolymers(writer);
        registerMisc(writer);

        // Genuinely blocked — missing item/tag/enum system in this port (see class javadoc history):
        // chem.hydrogencoke, chem.tarsand, chem.tel, chem.deicer, chem.ducrete,
        // chem.batterylead, chem.batterylithium, chem.batterysodium, chem.batteryschrabidium, chem.batteryquantum,
        // chem.cordite, chem.coltancleaning, chem.coltanpain, chem.yellowcake, chem.balefire, chem.osmiridiumdeath, chem.meth
    }

    /** Concrete/asphalt/aggregate/obsidian family — port of 1.7.10 chem.concrete/concreteasbestos/liquidconk/asphalt/aggregate/obsidian. */
    private static void registerConstructionMaterials(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 500)
            .withIconItem(new ItemStack(Items.OBSIDIAN))
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidInput(ModFluids.LAVA.getSource(), 500)
            .addFluidInput(ModFluids.AIR.getSource(), 4_000)
            .addItemOutput(new ItemStack(Items.OBSIDIAN))
            .withBlueprintPool("discover.stone")
            .save(writer, "chemplant/chem_obsidian");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(320, 500)
            .withIconItem(new ItemStack(Items.GRAVEL))
            .addItemInput(Items.COBBLESTONE, 16)
            .addItemOutput(new ItemStack(Items.GRAVEL, 8))
            .addItemOutput(new ItemStack(Items.SAND, 8))
            .save(writer, "chemplant/chem_aggregate");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(new ItemStack(ModBlocks.CONCRETE.get()))
            .addItemInput(ModMaterialItems.item(ModMaterials.CEMENT, MaterialShape.POWDER), 1)
            .addItemInput(Items.GRAVEL, 8)
            .addItemInput(Items.SAND, 8)
            .addFluidInput(ModFluids.WATER.getSource(), 2_000)
            .addItemOutput(new ItemStack(ModBlocks.CONCRETE.get(), 16))
            .save(writer, "chemplant/chem_concrete");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(new ItemStack(ModBlocks.CONCRETE_ASBESTOS.get()))
.addItemInput(ModMaterialItems.item(ModMaterials.CEMENT, MaterialShape.POWDER), 4)
.addItemInput(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT), 4)
            .addItemInput(Items.SAND, 8)
            .addFluidInput(ModFluids.WATER.getSource(), 2_000)
            .addItemOutput(new ItemStack(ModBlocks.CONCRETE_ASBESTOS.get(), 16))
            .save(writer, "chemplant/chem_concreteasbestos");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.CONCRETE.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.CEMENT, MaterialShape.POWDER), 1)
            .addItemInput(Items.GRAVEL, 8)
            .addItemInput(Items.SAND, 8)
            .addFluidInput(ModFluids.WATER.getSource(), 2_000)
            .addFluidOutput(ModFluids.CONCRETE.getSource(), 16_000)
            .save(writer, "chemplant/chem_liquidconk");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(new ItemStack(ModBlocks.ASPHALT.get()))
            .addItemInput(Items.GRAVEL, 2)
            .addItemInput(Items.SAND, 6)
            .addFluidInput(ModFluids.BITUMEN.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModBlocks.ASPHALT.get(), 16))
            .save(writer, "chemplant/chem_asphalt");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(new ItemStack(ModBlocks.DECO_RUSTY_STEEL.get()))
            .addItemInput(ModBlocks.DECO_STEEL.get().asItem(), 8)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModBlocks.DECO_RUSTY_STEEL.get(), 8))
            .save(writer, "chemplant/chem_rustysteel");
    }

    /** Solid/liquid fuel and arc-electrode chains — port of 1.7.10 chem.biosolidfuel/biooilsolidfuel/oilelectrodes/lubeelectrodes/biogas/rocketfuel. */
    private static void registerFuelsAndElectrodes(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(ModItems.SOLID_FUEL.get())
            .addItemInput(ModItems.BIOMASS_COMPRESSED.get(), 4)
            .addItemOutput(new ItemStack(ModItems.SOLID_FUEL.get(), 1))
            .withBlueprintPool("alt.biosolidfuel")
            .save(writer, "chemplant/chem_biosolidfuel");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(ModItems.SOLID_FUEL.get())
            .addItemInput(ModItems.BIOMASS_COMPRESSED.get(), 2)
            .addFluidInput(ModFluids.HEATINGOIL.getSource(), 100)
            .addItemOutput(new ItemStack(ModItems.SOLID_FUEL.get(), 1))
            .withBlueprintPool("alt.biosolidfuel")
            .save(writer, "chemplant/chem_biooilsolidfuel");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(600, 100)
            .withIconItem(ModItems.ARC_ELECTRODE.get())
            .addFluidInput(ModFluids.HEATINGOIL.getSource(), 4_000)
            .addItemOutput(new ItemStack(ModItems.ARC_ELECTRODE.get(), 1))
            .withBlueprintPool("alt.electrodes")
            .save(writer, "chemplant/chem_oilelectrodes");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(600, 100)
            .withIconItem(ModItems.ARC_ELECTRODE.get())
            .addFluidInput(ModFluids.LUBRICANT.getSource(), 8_000)
            .addItemOutput(new ItemStack(ModItems.ARC_ELECTRODE.get(), 1))
            .withBlueprintPool("alt.electrodes")
            .save(writer, "chemplant/chem_lubeelectrodes");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.BIOGAS.getSource())
            .addItemInput(ModItems.BIOMASS.get(), 16)
            .addFluidInput(ModFluids.AIR.getSource(), 4_000)
            .addFluidOutput(ModFluids.BIOGAS.getSource(), 2_000)
            .save(writer, "chemplant/chem_biogas");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
            .withIconItem(ModItems.ROCKET_FUEL.get())
            .addItemInput(ModItems.SOLID_FUEL.get(), 2)
            .addFluidInput(ModFluids.PETROLEUM.getSource(), 200)
            .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
            .addItemOutput(new ItemStack(ModItems.ROCKET_FUEL.get(), 4))
            .save(writer, "chemplant/chem_rocketfuel");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .withIconItem(ModItems.BALL_DYNAMITE.get())
            .addItemInput(Items.SUGAR, 1)
            .addItemInput(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL), 1)
            .addItemInput(Items.SAND, 1)
            .addItemOutput(new ItemStack(ModItems.BALL_DYNAMITE.get(), 2))
            .save(writer, "chemplant/chem_dynamite");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 5_000)
            .withIconItem(ModItems.BALL_TATB.get())
            .addItemInput(ModItems.BALL_TNT.get(), 1)
            .addFluidInput(ModFluids.SOURGAS.getSource(), 200)
            .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 10)
            .addItemOutput(new ItemStack(ModItems.BALL_TATB.get(), 1))
            .save(writer, "chemplant/chem_tatb");
    }

    /** Polymer/plastic chain — port of 1.7.10 chem.polymer/bakelite/rubber/hardplastic/pvc/desh/deshcracked. */
    private static void registerPolymers(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT))
            .addItemInput(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER), 2)
            .addItemInput(ModItems.FLUORITE.get(), 1)
            .addFluidInput(ModFluids.PETROLEUM.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), 4))
            .save(writer, "chemplant/chem_polymer");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT))
            .addFluidInput(ModFluids.AROMATICS.getSource(), 500)
            .addFluidInput(ModFluids.PETROLEUM.getSource(), 500)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT), 1))
            .save(writer, "chemplant/chem_bakelite");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 200)
            .withIconItem(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
            .addItemInput(ModItems.SULFUR.get(), 1)
            .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT), 2))
            .save(writer, "chemplant/chem_rubber");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT))
            .addItemInput(ModItems.POWDER_DESH_MIX.get(), 1)
            .addFluidInput(ModFluids.LIGHTOIL.getSource(), 200)
            .addFluidInput(ModFluids.MERCURY.getSource(), 200)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT), 1))
            .save(writer, "chemplant/chem_desh");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
            .withIconItem(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT))
            .addItemInput(ModItems.POWDER_DESH_MIX.get(), 1)
            .addFluidInput(ModFluids.LIGHTOIL_CRACK.getSource(), 500)
            .addFluidInput(ModFluids.MERCURY.getSource(), 100)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT), 1))
            .save(writer, "chemplant/chem_deshcracked");
    }

    /** Miscellaneous late additions — port of 1.7.10 chem.laminate/polarized/epearl/meatprocessing/schrabidate/coltancrystal. */
    private static void registerMisc(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
            .withIconItem(new ItemStack(ModBlocks.REINFORCED_LAMINATE.get()))
            .addItemInput(Items.GLASS, 1)
            .addItemInput(ModItems.BOLT_STEEL.get(), 4)
            .addFluidInput(ModFluids.XYLENE.getSource(), 50)
            .addFluidInput(ModFluids.PHOSGENE.getSource(), 50)
            .addItemOutput(new ItemStack(ModBlocks.REINFORCED_LAMINATE.get(), 1))
            .save(writer, "chemplant/chem_laminate");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 500)
            .withIconItem(new ItemStack(ModBlocks.GLASS_POLARIZED.get()))
            .addItemInput(Items.GLASS_PANE, 1)
            .addFluidInput(ModFluids.PETROLEUM.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModBlocks.GLASS_POLARIZED.get(), 16))
            .save(writer, "chemplant/chem_polarized");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 300)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.ENDERJUICE.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.POWDER), 1)
            .addFluidInput(ModFluids.XPJUICE.getSource(), 500)
            .addFluidOutput(ModFluids.ENDERJUICE.getSource(), 100)
            .save(writer, "chemplant/chem_epearl");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 200)
            .withIconItem(ModItems.GLYPHID_MEAT.get())
            .addItemInput(ModItems.GLYPHID_MEAT.get(), 3)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModItems.SULFUR.get(), 4))
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL), 3))
            .addFluidOutput(ModFluids.SALIENT.getSource(), 250)
            .save(writer, "chemplant/chem_meatprocessing");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(150, 5_000)
            .withIconItem(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.INGOT))
            .addItemInput(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER), 1)
            .addFluidInput(ModFluids.SCHRABIDIC.getSource(), 250)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.INGOT), 1))
            .save(writer, "chemplant/chem_schrabidate");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(80, 100)
            .withIconItem(ModItems.GEM_TANTALIUM.get())
            .addFluidInput(ModFluids.PAIN.getSource(), 1_000)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 500)
            .addItemOutput(new ItemStack(ModItems.GEM_TANTALIUM.get(), 1))
            .addItemOutput(new ItemStack(ModItems.DUST.get(), 3))
            .addFluidOutput(ModFluids.WATER.getSource(), 250)
            .save(writer, "chemplant/chem_coltancrystal");
    }

    private static void registerRegularFluids(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.HYDROGEN.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER), 1)
            .addFluidInput(ModFluids.WATER.getSource(), 8_000)
            .addFluidOutput(ModFluids.HYDROGEN.getSource(), 500)
            .save(writer, "chemplant/chem_hydrogen");

        // TODO: chem.hydrogencoke (1.7.10: ANY_COKE.gem()) — нет аналога тега/предмета в Modernized.

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.OXYGEN.getSource())
            .addFluidInput(ModFluids.AIR.getSource(), 8_000)
            .addFluidOutput(ModFluids.OXYGEN.getSource(), 500)
            .save(writer, "chemplant/chem_oxygen");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(300, 1_000)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.XENON.getSource())
            .addFluidInput(ModFluids.AIR.getSource(), 16_000)
            .addFluidOutput(ModFluids.XENON.getSource(), 50)
            .save(writer, "chemplant/chem_xenon");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 1_000)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.XENON.getSource())
            .addFluidInput(ModFluids.AIR.getSource(), 8_000)
            .addFluidInput(ModFluids.OXYGEN.getSource(), 250)
            .addFluidOutput(ModFluids.XENON.getSource(), 50)
            .withBlueprintPool("alt.xenonoxy")
            .save(writer, "chemplant/chem_xenonoxy");

        // TODO: chem.helium3 (1.7.10: ModBlocks.moon_turf) — нет блока/предмета в Modernized.

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.CARBONDIOXIDE.getSource())
            .addFluidInput(ModFluids.GAS.getSource(), 1_000)
            .addFluidOutput(ModFluids.CARBONDIOXIDE.getSource(), 1_000)
            .save(writer, "chemplant/chem_co2");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
            .addFluidInput(ModFluids.CHLOROCALCITE_CLEANED.getSource(), 500)
            .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 8_000)
            .addFluidOutput(ModFluids.POTASSIUM_CHLORIDE.getSource(), 250)
            .addFluidOutput(ModFluids.CALCIUM_CHLORIDE.getSource(), 250)
            .save(writer, "chemplant/chem_cccentrifuge");
    }

    private static void registerOils(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.ETHANOL.getSource())
            .addItemInput(Items.SUGAR, 10)
            .addFluidOutput(ModFluids.ETHANOL.getSource(), 1_000)
            .save(writer, "chemplant/chem_ethanol");

        // TODO: chem.biogas (1.7.10: ModItems.biomass) — нет предмета в Modernized.

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.BIOFUEL.getSource())
            .addFluidInput(ModFluids.BIOGAS.getSource(), 1_500)
            .addFluidInput(ModFluids.ETHANOL.getSource(), 250)
            .addFluidOutput(ModFluids.BIOFUEL.getSource(), 1_000)
            .save(writer, "chemplant/chem_biofuel");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.RECLAIMED.getSource())
            .addFluidInput(ModFluids.SLOP.getSource(), 1_000)
            .addFluidOutput(ModFluids.RECLAIMED.getSource(), 800)
            .save(writer, "chemplant/chem_reoil");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.GASOLINE.getSource())
            .addFluidInput(ModFluids.NAPHTHA.getSource(), 1_000)
            .addFluidOutput(ModFluids.GASOLINE.getSource(), 800)
            .save(writer, "chemplant/chem_gasoline");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.LUBRICANT.getSource())
            .addFluidInput(ModFluids.COALCREOSOTE.getSource(), 1_000)
            .addFluidOutput(ModFluids.LUBRICANT.getSource(), 1_000)
            .withBlueprintPool("alt.lube")
            .save(writer, "chemplant/chem_coallube");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.LUBRICANT.getSource())
            .addFluidInput(ModFluids.HEAVYOIL.getSource(), 2_000)
            .addFluidOutput(ModFluids.LUBRICANT.getSource(), 1_000)
            .withBlueprintPool("alt.lube")
            .save(writer, "chemplant/chem_heavylube");

        // TODO: chem.tarsand (1.7.10: ModBlocks.ore_oil_sand, ANY_TAR) — нет блоков/тегов в Modernized.
        // TODO: chem.tel / chem.deicer (1.7.10: fuel_additive) — нет предметов/enum в Modernized.
    }

    private static void registerAcids(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.PEROXIDE.getSource())
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.PEROXIDE.getSource(), 1_000)
            .save(writer, "chemplant/chem_peroxide");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.SULFURIC_ACID.getSource())
            .addItemInput(ModItems.SULFUR.get(), 1)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 1_000)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.SULFURIC_ACID.getSource(), 2_000)
            .save(writer, "chemplant/chem_sulfuricacid");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.NITRIC_ACID.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL), 1)
            .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 500)
            .addFluidOutput(ModFluids.NITRIC_ACID.getSource(), 1_000)
            .save(writer, "chemplant/chem_nitricacid");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 5_000)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.NITRIC_ACID.getSource())
            .addFluidInput(ModFluids.AIR.getSource(), 8_000)
            .addFluidInput(ModFluids.WATER.getSource(), 2_000)
            .addFluidOutput(ModFluids.NITRIC_ACID.getSource(), 1_000)
            .withBlueprintPool("alt.birkeland")
            .save(writer, "chemplant/chem_birkeland");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 5_000)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.SCHRABIDIC.getSource())
            .addFluidInput(ModFluids.SAS3.getSource(), 2_000)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 2_000)
            .addFluidOutput(ModFluids.SCHRABIDIC.getSource(), 2_000)
            .save(writer, "chemplant/chem_schrabidic");

        // TODO: chem.schrabidate (1.7.10: IRON.dust, powder_schrabidate) — нет порошка/предмета.
    }

    private static void registerCoolants(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.PERFLUOROMETHYL.getSource())
            .addItemInput(ModItems.FLUORITE.get(), 1)
            .addFluidInput(ModFluids.PETROLEUM.getSource(), 1_000)
            .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
            .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 1_000)
            .save(writer, "chemplant/chem_perfluoromethyl");
    }

    private static void registerSteam(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(10, 50)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.STEAM.getSource())
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.STEAM.getSource(), 1_000)
            .save(writer, "chemplant/chem_steam");
    }

    private static void registerOxyhydrogen(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.OXYHYDROGEN.getSource())
            .addFluidInput(ModFluids.HYDROGEN.getSource(), 500)
            .addFluidInput(ModFluids.OXYGEN.getSource(), 250)
            .addFluidOutput(ModFluids.OXYHYDROGEN.getSource(), 500)
            .save(writer, "chemplant/chem_oxyhydrogen");
    }

    private static void registerDeuterium(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1_000)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.DEUTERIUM.getSource())
            .addFluidInput(ModFluids.HEAVYWATER.getSource(), 2_000)
            .addFluidOutput(ModFluids.DEUTERIUM.getSource(), 500)
            .save(writer, "chemplant/chem_deuterium");
    }

    private static void registerUf6(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 500)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.UF6.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.POWDER), 1)
            .addItemInput(ModItems.FLUORITE.get(), 4)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addItemOutput(new ItemStack(ModItems.SULFUR.get(), 2))
            .addFluidOutput(ModFluids.UF6.getSource(), 1_200)
            .save(writer, "chemplant/chem_uf6");

        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 500)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.PUF6.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.POWDER), 1)
            .addItemInput(ModItems.FLUORITE.get(), 3)
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidOutput(ModFluids.PUF6.getSource(), 900)
            .save(writer, "chemplant/chem_puf6");
    }

    private static void registerSchrabidium(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 5_000)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.SAS3.getSource())
            .addItemInput(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.POWDER), 1)
            .addItemInput(ModItems.SULFUR.get(), 2)
            .addFluidInput(ModFluids.PEROXIDE.getSource(), 2_000)
            .addFluidOutput(ModFluids.SAS3.getSource(), 1_000)
            .save(writer, "chemplant/chem_sas3");

    }

    private static void registerKevlar(Consumer<FinishedRecipe> writer) {
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 300)
            .withIconItem(ModItems.PLATE_KEVLAR.get())
            .addFluidInput(ModFluids.AROMATICS.getSource(), 200)
            .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
            .addFluidInput(ModFluids.CHLORINE.getSource(), 100)
            .addItemOutput(new ItemStack(ModItems.PLATE_KEVLAR.get(), 4))
            .save(writer, "chemplant/chem_kevlar");
    }

    private static void registerBasicBlocks(Consumer<FinishedRecipe> writer) {
        // 1.7.10: chem.cobble
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
            .withIconItem(new ItemStack(Items.COBBLESTONE))
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidInput(ModFluids.LAVA.getSource(), 25)
            .addItemOutput(new ItemStack(Items.COBBLESTONE))
            .save(writer, "chemplant/chem_cobble");

        // 1.7.10: chem.stone (discover pool)
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 500)
            .withIconItem(new ItemStack(Items.STONE))
            .addFluidInput(ModFluids.WATER.getSource(), 1_000)
            .addFluidInput(ModFluids.LAVA.getSource(), 25)
            .addFluidInput(ModFluids.AIR.getSource(), 4_000)
            .addItemOutput(new ItemStack(Items.STONE))
            .withBlueprintPool("discover")
            .save(writer, "chemplant/chem_stone");
    }

    private static void registerDhc(Consumer<FinishedRecipe> writer) {
        // 1.7.10: chem.dhc
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(400, 500)
            .withIconItem(ModItems.FLUID_IDENTIFIER.get())
            .withIconFluid(ModFluids.DHC.getSource())
            .addFluidInput(ModFluids.DEUTERIUM.getSource(), 500)
            .addFluidInput(ModFluids.REFORMGAS.getSource(), 250)
            .addFluidInput(ModFluids.SYNGAS.getSource(), 250)
            .addFluidOutput(ModFluids.DHC.getSource(), 500)
            .save(writer, "chemplant/chem_dhc");
    }

    private static void registerExplosives(Consumer<FinishedRecipe> writer) {
        // 1.7.10: chem.tnt
        // KNO.dust() маппим на CRYSTAL_NITER (пока другого аналога в Modernized нет).
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1_000)
            .withIconItem(ModItems.BALL_TNT.get())
            .addItemInput(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL), 1)
            .addFluidInput(ModFluids.AROMATICS.getSource(), 500)
            .addItemOutput(new ItemStack(ModItems.BALL_TNT.get(), 4))
            .save(writer, "chemplant/chem_tnt");

        // 1.7.10: chem.c4
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1_000)
            .withIconItem(ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT))
            .addItemInput(ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL), 1)
            .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
            .addItemOutput(new ItemStack(ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT), 4))
            .save(writer, "chemplant/chem_c4");

        // TODO: chem.cordite (нет sawdust/cordite items)
        // TODO: chem.rocketfuel (нет solid_fuel/rocket_fuel items)
        // TODO: chem.dynamite (нет ball_dynamite)
        // TODO: chem.tatb (нет ball_tatb)
    }

    // TODO: дальше в 1.7.10 идут большие блоки рецептов (бетон/взрывчатка/стекло/и т.д.) —
    // переносим по мере появления контента в Modernized.
}
//?}
