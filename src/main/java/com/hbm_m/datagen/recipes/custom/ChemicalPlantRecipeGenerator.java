package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * 1:1-Port von {@code ChemicalPlantRecipes.registerDefaults()} (Chemiewerk), mit den Konfig-Fassungen ({@link ConfigRecipes}: 528-Druck, LBSM-Chemie).
 *
 * <p>AUTOMATISCH ERZEUGT aus dem Original ({@code com.hbm.inventory.recipes.ChemicalPlantRecipes}) durch {@code rc/transpile.py} - nicht von Hand
 * aendern, sondern Zuordnungen im Skript pflegen. OreDict-Schluessel des Originals sind Item-Tags
 * {@code hbm_m:oredict/...} ({@link OreDictTagProvider}). Mit OFFEN markierte Rezepte haben im Port
 * (noch) keine Entsprechung fuer Zutat oder Ergebnis.</p>
 */
public final class ChemicalPlantRecipeGenerator {

    private ChemicalPlantRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        part0(writer);
        part1(writer);
        part2(writer);
        checkMissing("ChemicalPlantRecipeGenerator");
    }

    private static void part0(Consumer<FinishedRecipe> writer) {
        // chem.hydrogen
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
                .addItemInput(ore("oredict/gem/coal"), 1)
                .addFluidInput(ModFluids.WATER.getSource(), 8000)
                .addFluidOutput(ModFluids.HYDROGEN.getSource(), 500)
                .save(writer, "chemplant/hydrogen");

        // chem.hydrogencoke
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
                .addItemInput(ore("oredict/gem/any_coke"), 1)
                .addFluidInput(ModFluids.WATER.getSource(), 8000)
                .addFluidOutput(ModFluids.HYDROGEN.getSource(), 500)
                .save(writer, "chemplant/hydrogencoke");

        // chem.oxygen
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 400)
                .addFluidInput(ModFluids.AIR.getSource(), 8000)
                .addFluidOutput(ModFluids.OXYGEN.getSource(), 500)
                .save(writer, "chemplant/oxygen");

        // chem.xenon
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(300, 1000)
                .addFluidInput(ModFluids.AIR.getSource(), 16000)
                .addFluidOutput(ModFluids.XENON.getSource(), 50)
                .save(writer, "chemplant/xenon");

        // chem.xenonoxy
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 1000)
                .addFluidInput(ModFluids.AIR.getSource(), 8000)
                .addFluidInput(ModFluids.OXYGEN.getSource(), 250)
                .addFluidOutput(ModFluids.XENON.getSource(), 50)
                .withBlueprintPool("alt..xenonoxy")
                .save(writer, "chemplant/xenonoxy");

        // chem.helium3
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(25, 2000)
                .addItemInput(Ingredient.of(item("hbm_m:moon_turf")), 1)
                .addFluidOutput(ModFluids.HELIUM3.getSource(), 125)
                .save(writer, "chemplant/helium3");

        // chem.co2
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
                .addFluidInput(ModFluids.GAS.getSource(), 1000)
                .addFluidOutput(ModFluids.CARBONDIOXIDE.getSource(), 1000)
                .save(writer, "chemplant/co2");

        // chem.perfluoromethyl
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
                .addItemInput(ore("oredict/dust/fluorite"), 1)
                .addFluidInput(ModFluids.PETROLEUM.getSource(), 1000)
                .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
                .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 1000)
                .save(writer, "chemplant/perfluoromethyl");

        // chem.cccentrifuge
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
                .addFluidInput(ModFluids.CHLOROCALCITE_CLEANED.getSource(), 500)
                .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 8000)
                .addFluidOutput(ModFluids.POTASSIUM_CHLORIDE.getSource(), 250)
                .addFluidOutput(ModFluids.CALCIUM_CHLORIDE.getSource(), 250)
                .save(writer, "chemplant/cccentrifuge");

        // chem.ethanol
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
                .addItemInput(Ingredient.of(item("minecraft:sugar")), 10)
                .addFluidOutput(ModFluids.ETHANOL.getSource(), 1000)
                .save(writer, "chemplant/ethanol");

        // chem.biogas
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
                .addItemInput(Ingredient.of(item("hbm_m:biomass")), 16)
                .addFluidInput(ModFluids.AIR.getSource(), 4000)
                .addFluidOutput(ModFluids.BIOGAS.getSource(), 2000)
                .save(writer, "chemplant/biogas");

        // chem.biofuel
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
                .addFluidInput(ModFluids.BIOGAS.getSource(), 1500)
                .addFluidInput(ModFluids.ETHANOL.getSource(), 250)
                .addFluidOutput(ModFluids.BIOFUEL.getSource(), 1000)
                .save(writer, "chemplant/biofuel");

        // chem.reoil
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addFluidInput(ModFluids.SMEAR.getSource(), 1000)
                .addFluidOutput(ModFluids.RECLAIMED.getSource(), 800)
                .save(writer, "chemplant/reoil");

        // chem.gasoline
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addFluidInput(ModFluids.NAPHTHA.getSource(), 1000)
                .addFluidOutput(ModFluids.GASOLINE.getSource(), 800)
                .save(writer, "chemplant/gasoline");

        // chem.coallube
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addFluidInput(ModFluids.COALCREOSOTE.getSource(), 1000)
                .addFluidOutput(ModFluids.LUBRICANT.getSource(), 1000)
                .withBlueprintPool("alt..lube")
                .save(writer, "chemplant/coallube");

        // chem.heavylube
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addFluidInput(ModFluids.HEAVYOIL.getSource(), 2000)
                .addFluidOutput(ModFluids.LUBRICANT.getSource(), 1000)
                .withBlueprintPool("alt..lube")
                .save(writer, "chemplant/heavylube");

        // chem.tarsand
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
                .addItemInput(Ingredient.of(item("hbm_m:ore_oil_sand")), 16)
                .addItemInput(ore("oredict/any/tar"), 1)
                .addItemOutput(stack("minecraft:sand", 16))
                .addFluidOutput(ModFluids.BITUMEN.getSource(), 1000)
                .save(writer, "chemplant/tarsand");

        // chem.tel
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addItemInput(ore("oredict/any/tar"), 1)
                .addItemInput(ore("oredict/dust/lead"), 1)
                .addFluidInput(ModFluids.PETROLEUM.getSource(), 100)
                .addFluidInput(ModFluids.STEAM.getSource(), 1000)
                .addItemOutput(stack("hbm_m:fuel_additive_antiknock", 1))
                .save(writer, "chemplant/tel");

        // chem.deicer
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addFluidInput(ModFluids.GAS.getSource(), 100)
                .addFluidInput(ModFluids.HYDROGEN.getSource(), 50)
                .addItemOutput(stack("hbm_m:fuel_additive_deicer", 1))
                .save(writer, "chemplant/deicer");

        // chem.cobble
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addFluidInput(ModFluids.LAVA.getSource(), 25)
                .addItemOutput(stack("minecraft:cobblestone", 1))
                .save(writer, "chemplant/cobble");

        // chem.stone
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 500)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addFluidInput(ModFluids.LAVA.getSource(), 25)
                .addFluidInput(ModFluids.AIR.getSource(), 4000)
                .addItemOutput(stack("minecraft:stone", 1))
                .withBlueprintPool("discover..stone")
                .save(writer, "chemplant/stone");

        // chem.obsidian
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 500)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addFluidInput(ModFluids.LAVA.getSource(), 500)
                .addFluidInput(ModFluids.AIR.getSource(), 4000)
                .addItemOutput(stack("minecraft:obsidian", 1))
                .withBlueprintPool("discover..stone")
                .save(writer, "chemplant/obsidian");

        // chem.aggregate
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(320, 500)
                .addItemInput(Ingredient.of(item("minecraft:cobblestone")), 16)
                .addItemOutput(stack("minecraft:gravel", 8))
                .addItemOutput(stack("minecraft:sand", 8))
                .withBlueprintPool("discover..stone")
                .save(writer, "chemplant/aggregate");

        // chem.concrete
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                .addItemInput(Ingredient.of(item("hbm_m:cement_powder")), 1)
                .addItemInput(Ingredient.of(item("minecraft:gravel")), 8)
                .addItemInput(ore("oredict/sand"), 8)
                .addFluidInput(ModFluids.WATER.getSource(), 2000)
                .addItemOutput(stack("hbm_m:concrete_smooth", 16))
                .save(writer, "chemplant/concrete");

        // chem.concreteasbestos [Fassungen: lbsm_chemistry]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:cement_powder")), 4)
                    .addItemInput(ore("oredict/ingot/asbestos"), 4)
                    .addItemInput(ore("oredict/sand"), 8)
                    .addFluidInput(ModFluids.WATER.getSource(), 2000)
                    .addItemOutput(stack("hbm_m:concrete_asbestos", 16))
                    .save(w, "chemplant/concreteasbestos"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:cement_powder")), 4)
                    .addItemInput(ore("oredict/ingot/asbestos"), 1)
                    .addItemInput(ore("oredict/sand"), 8)
                    .addFluidInput(ModFluids.WATER.getSource(), 2000)
                    .addItemOutput(stack("hbm_m:concrete_asbestos", 16))
                    .save(v, "chemplant/concreteasbestos"), "lbsm_chemistry")
                .save();

        // chem.ducrete
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(150, 100)
                .addItemInput(Ingredient.of(item("hbm_m:cement_powder")), 4)
                .addItemInput(ore("oredict/ingot/ferrouranium"), 1)
                .addItemInput(ore("oredict/sand"), 8)
                .addFluidInput(ModFluids.WATER.getSource(), 2000)
                .addItemOutput(stack("hbm_m:ducrete_smooth", 8))
                .save(writer, "chemplant/ducrete");

        // chem.liquidconk
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                .addItemInput(Ingredient.of(item("hbm_m:cement_powder")), 1)
                .addItemInput(Ingredient.of(item("minecraft:gravel")), 8)
                .addItemInput(ore("oredict/sand"), 8)
                .addFluidInput(ModFluids.WATER.getSource(), 2000)
                .addFluidOutput(ModFluids.CONCRETE.getSource(), 16000)
                .save(writer, "chemplant/liquidconk");

        // chem.asphalt
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                .addItemInput(Ingredient.of(item("minecraft:gravel")), 2)
                .addItemInput(ore("oredict/sand"), 6)
                .addFluidInput(ModFluids.BITUMEN.getSource(), 1000)
                .addItemOutput(stack("hbm_m:asphalt", 16))
                .save(writer, "chemplant/asphalt");

        // chem.batterylead
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                .addItemInput(ore("oredict/plate/steel"), 4)
                .addItemInput(ore("oredict/ingot/lead"), 4)
                .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 8000)
                .addItemOutput(stack("hbm_m:battery_pack_battery_lead", 1))
                .save(writer, "chemplant/batterylead");

        // chem.batterylithium
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                .addItemInput(ore("oredict/dust/lithium"), 12)
                .addItemInput(ore("oredict/dust/cobalt"), 8)
                .addItemInput(ore("oredict/ingot/any_plastic"), 4)
                .addFluidInput(ModFluids.OXYGEN.getSource(), 2000)
                .addItemOutput(stack("hbm_m:battery_pack_battery_lithium", 1))
                .save(writer, "chemplant/batterylithium");
    }

    private static void part1(Consumer<FinishedRecipe> writer) {
        // chem.batterysodium
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 10000)
                .addItemInput(ore("oredict/dust/sodium"), 24)
                .addItemInput(ore("oredict/dust/iron"), 24)
                .addItemInput(ore("oredict/ingot/any_hard_plastic"), 12)
                .addItemOutput(stack("hbm_m:battery_pack_battery_sodium", 1))
                .save(writer, "chemplant/batterysodium");

        // chem.batteryschrabidium
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 25000)
                .addItemInput(ore("oredict/dust/schrabidium"), 24)
                .addItemInput(ore("oredict/plate_triple/any_bismoid_bronze"), 8)
                .addFluidInput(ModFluids.HELIUM4.getSource(), 8000)
                .addItemOutput(stack("hbm_m:battery_pack_battery_schrabidium", 1))
                .save(writer, "chemplant/batteryschrabidium");

        // chem.batteryquantum
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100000)
                .addItemInput(ore("oredict/wire_dense/bscco"), 24)
                .addItemInput(Ingredient.of(item("hbm_m:pellet_charged")), 32)
                .addItemInput(Ingredient.of(item("hbm_m:cft_ingot")), 16)
                .addFluidInput(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 8000)
                .addItemOutput(stack("hbm_m:battery_pack_battery_quantum", 1))
                .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 8000)
                .save(writer, "chemplant/batteryquantum");

        // chem.desh [Fassungen: lbsm_chemistry]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:powder_desh_mix")), 1)
                    .addFluidInput(ModFluids.LIGHTOIL.getSource(), 200)
                    .addFluidInput(ModFluids.MERCURY.getSource(), 200)
                    .addItemOutput(stack("hbm_m:desh_ingot", 1))
                    .save(w, "chemplant/desh"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:powder_desh_mix")), 1)
                    .addFluidInput(ModFluids.LIGHTOIL.getSource(), 200)
                    .addItemOutput(stack("hbm_m:desh_ingot", 1))
                    .save(v, "chemplant/desh"), "lbsm_chemistry")
                .save();

        // chem.deshcracked [Fassungen: lbsm_chemistry]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:powder_desh_mix")), 1)
                    .addFluidInput(ModFluids.LIGHTOIL_CRACK.getSource(), 500, 1)
                    .addFluidInput(ModFluids.MERCURY.getSource(), 100)
                    .addItemOutput(stack("hbm_m:desh_ingot", 1))
                    .save(w, "chemplant/deshcracked"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:powder_desh_mix")), 1)
                    .addFluidInput(ModFluids.LIGHTOIL_CRACK.getSource(), 500)
                    .addItemOutput(stack("hbm_m:desh_ingot", 1))
                    .save(v, "chemplant/deshcracked"), "lbsm_chemistry")
                .save();

        // chem.polymer [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(ore("oredict/dust/coal"), 2)
                    .addItemInput(ore("oredict/dust/fluorite"), 1)
                    .addFluidInput(ModFluids.PETROLEUM.getSource(), 1000)
                    .addItemOutput(stack("hbm_m:polymer_ingot", 4))
                    .save(w, "chemplant/polymer"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addItemInput(ore("oredict/dust/coal"), 2)
                    .addItemInput(ore("oredict/dust/fluorite"), 1)
                    .addFluidInput(ModFluids.PETROLEUM.getSource(), 1000, 1)
                    .addItemOutput(stack("hbm_m:polymer_ingot", 4))
                    .save(v, "chemplant/polymer"), "528_pressurized")
                .save();

        // chem.bakelite [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addFluidInput(ModFluids.AROMATICS.getSource(), 500)
                    .addFluidInput(ModFluids.PETROLEUM.getSource(), 500)
                    .addItemOutput(stack("hbm_m:bakelite_ingot", 1))
                    .save(w, "chemplant/bakelite"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 100)
                    .addFluidInput(ModFluids.AROMATICS.getSource(), 500, 1)
                    .addFluidInput(ModFluids.PETROLEUM.getSource(), 500, 1)
                    .addItemOutput(stack("hbm_m:bakelite_ingot", 1))
                    .save(v, "chemplant/bakelite"), "528_pressurized")
                .save();

        // chem.rubber [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 200)
                    .addItemInput(ore("oredict/dust/sulfur"), 1)
                    .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
                    .addItemOutput(stack("hbm_m:rubber_ingot", 2))
                    .save(w, "chemplant/rubber"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 200)
                    .addItemInput(ore("oredict/dust/sulfur"), 1)
                    .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500, 2)
                    .addItemOutput(stack("hbm_m:rubber_ingot", 2))
                    .save(v, "chemplant/rubber"), "528_pressurized")
                .save();

        // chem.hardplastic [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addFluidInput(ModFluids.XYLENE.getSource(), 500)
                    .addFluidInput(ModFluids.PHOSGENE.getSource(), 500)
                    .addItemOutput(stack("hbm_m:pc_ingot", 1))
                    .save(w, "chemplant/hardplastic"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addFluidInput(ModFluids.XYLENE.getSource(), 500, 2)
                    .addFluidInput(ModFluids.PHOSGENE.getSource(), 500, 2)
                    .addItemOutput(stack("hbm_m:pc_ingot", 1))
                    .save(v, "chemplant/hardplastic"), "528_pressurized")
                .save();

        // chem.pvc [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addItemInput(ore("oredict/dust/cadmium"), 1)
                    .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 250)
                    .addFluidInput(ModFluids.CHLORINE.getSource(), 250)
                    .addItemOutput(stack("hbm_m:pvc_ingot", 2))
                    .save(w, "chemplant/pvc"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addItemInput(ore("oredict/dust/cadmium"), 1)
                    .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 250, 2)
                    .addFluidInput(ModFluids.CHLORINE.getSource(), 250, 2)
                    .addItemOutput(stack("hbm_m:pvc_ingot", 2))
                    .save(v, "chemplant/pvc"), "528_pressurized")
                .save();

        // chem.kevlar [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 300)
                    .addFluidInput(ModFluids.AROMATICS.getSource(), 200)
                    .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
                    .addFluidInput(ModFluids.CHLORINE.getSource(), 100)
                    .addItemOutput(stack("hbm_m:plate_kevlar", 4))
                    .save(w, "chemplant/kevlar"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 300)
                    .addFluidInput(ModFluids.AROMATICS.getSource(), 200)
                    .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
                    .addFluidInput(ModFluids.PHOSGENE.getSource(), 100)
                    .addItemOutput(stack("hbm_m:plate_kevlar", 4))
                    .save(v, "chemplant/kevlar"), "528_pressurized")
                .save();

        // chem.meth
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 300)
                .addItemInput(Ingredient.of(item("minecraft:wheat")), 1)
                .addItemInput(Ingredient.of(item("minecraft:cocoa_beans")), 2)
                .addFluidInput(ModFluids.LUBRICANT.getSource(), 400)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 500)
                .addItemOutput(stack("hbm_m:chocolate", 4))
                .save(writer, "chemplant/meth");

        // chem.epearl
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 300)
                .addItemInput(ore("oredict/dust/diamond"), 1)
                .addFluidInput(ModFluids.XPJUICE.getSource(), 500)
                .addFluidOutput(ModFluids.ENDERJUICE.getSource(), 100)
                .save(writer, "chemplant/epearl");

        // chem.meatprocessing
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 200)
                .addItemInput(ore("oredict/glyphid_meat"), 3)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addItemOutput(stack("hbm_m:sulfur", 4))
                .addItemOutput(stack("hbm_m:niter", 3))
                .addFluidOutput(ModFluids.SALIENT.getSource(), 250)
                .save(writer, "chemplant/meatprocessing");

        // chem.rustysteel
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addItemInput(Ingredient.of(item("hbm_m:deco_steel")), 8)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addItemOutput(stack("hbm_m:deco_rusty_steel", 8))
                .save(writer, "chemplant/rustysteel");

        // chem.biosolidfuel
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addItemInput(Ingredient.of(item("hbm_m:biomass_compressed")), 4)
                .addItemOutput(stack("hbm_m:solid_fuel", 1))
                .withBlueprintPool("alt..biosolidfuel")
                .save(writer, "chemplant/biosolidfuel");

        // chem.biooilsolidfuel
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addItemInput(Ingredient.of(item("hbm_m:biomass_compressed")), 2)
                .addFluidInput(ModFluids.HEATINGOIL.getSource(), 100)
                .addItemOutput(stack("hbm_m:solid_fuel", 1))
                .withBlueprintPool("alt..biosolidfuel")
                .save(writer, "chemplant/biooilsolidfuel");

        // chem.oilelectrodes
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(600, 100)
                .addFluidInput(ModFluids.HEATINGOIL.getSource(), 4000)
                .addItemOutput(stack("hbm_m:arc_electrode", 1))
                .withBlueprintPool("alt..electrodes")
                .save(writer, "chemplant/oilelectrodes");

        // chem.lubeelectrodes
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(600, 100)
                .addFluidInput(ModFluids.LUBRICANT.getSource(), 8000)
                .addItemOutput(stack("hbm_m:arc_electrode", 1))
                .withBlueprintPool("alt..electrodes")
                .save(writer, "chemplant/lubeelectrodes");

        // chem.peroxide
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addFluidOutput(ModFluids.PEROXIDE.getSource(), 1000)
                .save(writer, "chemplant/peroxide");

        // chem.sulfuricacid
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
                .addItemInput(ore("oredict/dust/sulfur"), 1)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 1000)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addFluidOutput(ModFluids.SULFURIC_ACID.getSource(), 2000)
                .save(writer, "chemplant/sulfuricacid");

        // chem.nitricacid
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
                .addItemInput(ore("oredict/dust/saltpeter"), 1)
                .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 500)
                .addFluidOutput(ModFluids.NITRIC_ACID.getSource(), 1000)
                .save(writer, "chemplant/nitricacid");

        // chem.birkeland
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 5000)
                .addFluidInput(ModFluids.AIR.getSource(), 8000)
                .addFluidInput(ModFluids.WATER.getSource(), 2000)
                .addFluidOutput(ModFluids.NITRIC_ACID.getSource(), 1000)
                .withBlueprintPool("alt..birkeland")
                .save(writer, "chemplant/birkeland");

        // chem.schrabidic
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 5000)
                .addItemInput(Ingredient.of(item("hbm_m:pellet_charged")), 1)
                .addFluidInput(ModFluids.SAS3.getSource(), 2000)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 2000)
                .addFluidOutput(ModFluids.SCHRABIDIC.getSource(), 2000)
                .save(writer, "chemplant/schrabidic");

        // chem.schrabidate
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(150, 5000)
                .addItemInput(ore("oredict/dust/iron"), 1)
                .addFluidInput(ModFluids.SCHRABIDIC.getSource(), 250)
                .addItemOutput(stack("hbm_m:schrabidate_powder", 1))
                .save(writer, "chemplant/schrabidate");

        // chem.coltancleaning
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(60, 100)
                .addItemInput(ore("oredict/dust/coltan"), 2)
                .addItemInput(ore("oredict/dust/coal"), 1)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 250)
                .addFluidInput(ModFluids.HYDROGEN.getSource(), 500)
                .addItemOutput(stack("hbm_m:powder_coltan", 1))
                .addItemOutput(stack("hbm_m:niobium_powder", 1))
                .addItemOutput(stack("hbm_m:dust", 1))
                .addFluidOutput(ModFluids.WATER.getSource(), 500)
                .save(writer, "chemplant/coltancleaning");

        // chem.coltanpain
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(120, 100)
                .addItemInput(Ingredient.of(item("hbm_m:powder_coltan")), 1)
                .addItemInput(ore("oredict/dust/fluorite"), 1)
                .addFluidInput(ModFluids.GAS.getSource(), 1000)
                .addFluidInput(ModFluids.OXYGEN.getSource(), 500)
                .addFluidOutput(ModFluids.PAIN.getSource(), 1000)
                .save(writer, "chemplant/coltanpain");

        // chem.coltancrystal
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(80, 100)
                .addFluidInput(ModFluids.PAIN.getSource(), 1000)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 500)
                .addItemOutput(stack("hbm_m:gem_tantalium", 1))
                .addItemOutput(stack("hbm_m:dust", 3))
                .addFluidOutput(ModFluids.WATER.getSource(), 250)
                .save(writer, "chemplant/coltancrystal");

        // chem.cordite [Fassungen: lbsm_chemistry]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                    .addItemInput(ore("oredict/dust/saltpeter"), 2)
                    .addItemInput(Ingredient.of(item("hbm_m:sawdust_powder")), 2)
                    .addFluidInput(ModFluids.GAS.getSource(), 200)
                    .addItemOutput(stack("hbm_m:cordite", 4))
                    .save(w, "chemplant/cordite"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                    .addItemInput(ore("oredict/dust/saltpeter"), 2)
                    .addItemInput(Ingredient.of(item("hbm_m:sawdust_powder")), 2)
                    .addFluidInput(ModFluids.HEATINGOIL.getSource(), 200)
                    .addItemOutput(stack("hbm_m:cordite", 4))
                    .save(v, "chemplant/cordite"), "lbsm_chemistry")
                .save();

        // chem.rocketfuel [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:solid_fuel")), 2)
                    .addFluidInput(ModFluids.PETROLEUM.getSource(), 200)
                    .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
                    .addItemOutput(stack("hbm_m:rocket_fuel", 4))
                    .save(w, "chemplant/rocketfuel"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
                    .addItemInput(Ingredient.of(item("hbm_m:solid_fuel")), 2)
                    .addFluidInput(ModFluids.PETROLEUM.getSource(), 200, 1)
                    .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 100)
                    .addItemOutput(stack("hbm_m:rocket_fuel", 4))
                    .save(v, "chemplant/rocketfuel"), "528_pressurized")
                .save();
    }

    private static void part2(Consumer<FinishedRecipe> writer) {
        // chem.dynamite
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 100)
                .addItemInput(Ingredient.of(item("minecraft:sugar")), 1)
                .addItemInput(ore("oredict/dust/saltpeter"), 1)
                .addItemInput(ore("oredict/sand"), 1)
                .addItemOutput(stack("hbm_m:ball_dynamite", 2))
                .save(writer, "chemplant/dynamite");

        // chem.tnt [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addItemInput(ore("oredict/dust/saltpeter"), 1)
                    .addFluidInput(ModFluids.AROMATICS.getSource(), 500)
                    .addItemOutput(stack("hbm_m:ball_tnt", 4))
                    .save(w, "chemplant/tnt"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addItemInput(ore("oredict/dust/saltpeter"), 1)
                    .addFluidInput(ModFluids.AROMATICS.getSource(), 500, 1)
                    .addItemOutput(stack("hbm_m:ball_tnt", 4))
                    .save(v, "chemplant/tnt"), "528_pressurized")
                .save();

        // chem.tatb
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(50, 5000)
                .addItemInput(Ingredient.of(item("hbm_m:ball_tnt")), 1)
                .addFluidInput(ModFluids.SOURGAS.getSource(), 200, 1)
                .addFluidInput(ModFluids.NITRIC_ACID.getSource(), 10)
                .addItemOutput(stack("hbm_m:ball_tatb", 1))
                .save(writer, "chemplant/tatb");

        // chem.c4 [Fassungen: 528_pressurized]
        ConfigRecipes.variants(writer)
                .base(w -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addItemInput(ore("oredict/dust/saltpeter"), 1)
                    .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500)
                    .addItemOutput(stack("hbm_m:c4_ingot", 4))
                    .save(w, "chemplant/c4"))
                .variant(v -> ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 1000)
                    .addItemInput(ore("oredict/dust/saltpeter"), 1)
                    .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 500, 1)
                    .addItemOutput(stack("hbm_m:c4_ingot", 4))
                    .save(v, "chemplant/c4"), "528_pressurized")
                .save();

        // chem.napalm
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(40, 100)
                .addItemInput(Ingredient.of(item("hbm_m:canister_empty")), 1)
                .addFluidInput(ModFluids.GASOLINE.getSource(), 100)
                .addFluidInput(ModFluids.AROMATICS.getSource(), 50)
                .addItemOutput(stack("hbm_m:canister_napalm", 1))
                .save(writer, "chemplant/napalm");

        // chem.laminate
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(20, 100)
                .addItemInput(ore("oredict/block/glass"), 1)
                .addItemInput(ore("oredict/bolt/steel"), 4)
                .addFluidInput(ModFluids.XYLENE.getSource(), 50)
                .addFluidInput(ModFluids.PHOSGENE.getSource(), 50)
                .addItemOutput(stack("hbm_m:reinforced_laminate", 1))
                .save(writer, "chemplant/laminate");

        // chem.polarized
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 500)
                .addItemInput(ore("oredict/pane_glass"), 1)
                .addFluidInput(ModFluids.PETROLEUM.getSource(), 1000)
                .addItemOutput(stack("hbm_m:part_generic_glass_polarized", 16))
                .save(writer, "chemplant/polarized");

        // chem.yellowcake
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(250, 500)
                .addItemInput(ore("oredict/billet/uranium"), 2)
                .addItemInput(ore("oredict/dust/sulfur"), 2)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 500)
                .addItemOutput(stack("hbm_m:yellowcake_powder", 1))
                .save(writer, "chemplant/yellowcake");

        // chem.uf6
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 500)
                .addItemInput(Ingredient.of(item("hbm_m:yellowcake_powder")), 1)
                .addItemInput(ore("oredict/dust/fluorite"), 4)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addItemOutput(stack("hbm_m:sulfur", 2))
                .addFluidOutput(ModFluids.UF6.getSource(), 1200)
                .save(writer, "chemplant/uf6");

        // chem.puf6
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 500)
                .addItemInput(ore("oredict/dust/plutonium"), 1)
                .addItemInput(ore("oredict/dust/fluorite"), 3)
                .addFluidInput(ModFluids.WATER.getSource(), 1000)
                .addFluidOutput(ModFluids.PUF6.getSource(), 900)
                .save(writer, "chemplant/puf6");

        // chem.sas3
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 5000)
                .addItemInput(ore("oredict/dust/schrabidium"), 1)
                .addItemInput(ore("oredict/dust/sulfur"), 2)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 2000)
                .addFluidOutput(ModFluids.SAS3.getSource(), 1000)
                .save(writer, "chemplant/sas3");

        // chem.balefire
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(100, 10000)
                .addItemInput(Ingredient.of(item("hbm_m:egg_balefire_shard")), 1)
                .addFluidInput(ModFluids.KEROSENE.getSource(), 6000)
                .addItemOutput(stack("hbm_m:balefire_powder", 1))
                .addFluidOutput(ModFluids.BALEFIRE.getSource(), 8000)
                .save(writer, "chemplant/balefire");

        // chem.dhc
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(400, 500)
                .addFluidInput(ModFluids.DEUTERIUM.getSource(), 500)
                .addFluidInput(ModFluids.REFORMGAS.getSource(), 250)
                .addFluidInput(ModFluids.SYNGAS.getSource(), 250)
                .addFluidOutput(ModFluids.DHC.getSource(), 500)
                .save(writer, "chemplant/dhc");

        // chem.osmiridiumdeath
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(240, 1000)
                .addItemInput(Ingredient.of(item("hbm_m:paleogenite_powder")), 1)
                .addItemInput(ore("oredict/dust/fluorite"), 8)
                .addItemInput(Ingredient.of(item("hbm_m:nugget_bismuth")), 4)
                .addFluidInput(ModFluids.PEROXIDE.getSource(), 1000, 5)
                .addFluidOutput(ModFluids.DEATH.getSource(), 1000)
                .save(writer, "chemplant/osmiridiumdeath");
    }
}
//?}
