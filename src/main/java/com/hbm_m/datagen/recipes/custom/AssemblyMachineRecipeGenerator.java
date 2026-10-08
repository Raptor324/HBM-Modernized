package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * 1:1-Port von {@code AssemblyMachineRecipes.registerDefaults()} (Montagemaschine), mit den Konfig-Fassungen ({@link ConfigRecipes}: Expensive {@code inputItemsEx}, 528-Pools und -Ausschluesse).
 *
 * <p>AUTOMATISCH ERZEUGT aus dem Original ({@code com.hbm.inventory.recipes.AssemblyMachineRecipes}) durch {@code rc/transpile.py} - nicht von Hand
 * aendern, sondern Zuordnungen im Skript pflegen. OreDict-Schluessel des Originals sind Item-Tags
 * {@code hbm_m:oredict/...} ({@link OreDictTagProvider}). Mit OFFEN markierte Rezepte haben im Port
 * (noch) keine Entsprechung fuer Zutat oder Ergebnis.</p>
 */
public final class AssemblyMachineRecipeGenerator {

    private AssemblyMachineRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        part0(writer);
        part1(writer);
        part2(writer);
        part3(writer);
        part4(writer);
        part5(writer);
        part6(writer);
        part7(writer);
        part8(writer);
        part9(writer);
        part10(writer);
        part11(writer);
        checkMissing("AssemblyMachineRecipeGenerator");
    }

    private static void part0(Consumer<FinishedRecipe> writer) {
        // ass.plateiron
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_iron", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/iron"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/plateiron");

        // ass.plategold
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_gold", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/gold"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/plategold");

        // ass.platetitanium
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_titanium", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/titanium"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platetitanium");

        // ass.platealu
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_aluminium", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/aluminum"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platealu");

        // ass.platesteel
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_steel", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/steel"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platesteel");

        // ass.platelead
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_lead", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/lead"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platelead");

        // ass.platecopper
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_copper", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/copper"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platecopper");

        // ass.plateschrab
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_schrabidium", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/schrabidium"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/plateschrab");

        // ass.platecmb
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_combine_steel", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/cmb_steel"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platecmb");

        // ass.plategunmetal
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_gunmetal", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/gun_metal"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/plategunmetal");

        // ass.plateweaponsteel
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_weaponsteel", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/weapon_steel"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/plateweaponsteel");

        // ass.platesaturnite
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_saturnite", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/saturnite"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platesaturnite");

        // ass.platedura
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_dura_steel", 1), 60, 100)
                .addIngredient(ore("oredict/ingot/dura_steel"), 1)
                .withBlueprintPool("alt.plates")
                .save(writer, "assembler/platedura");

        // ass.platemixed
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_mixed", 4), 50, 100)
                .addIngredient(ore("oredict/plate/copper"), 2)
                .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 1)
                .addIngredient(ore("oredict/plate/saturnite"), 1)
                .save(writer, "assembler/platemixed");

        // ass.dalekanium
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_dalekanium", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:block_meteor")), 1)
                .save(writer, "assembler/dalekanium");

        // ass.platedesh
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_desh", 4), 200, 100)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 4)
                .addIngredient(ore("oredict/dust/any_plastic"), 2)
                .addIngredient(ore("oredict/ingot/dura_steel"), 1)
                .save(writer, "assembler/platedesh");

        // ass.platebismuth
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plate_bismuth", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:nugget_bismuth")), 2)
                .addIngredient(ore("oredict/billet/uranium238"), 2)
                .addIngredient(ore("oredict/dust/niobium"), 1)
                .save(writer, "assembler/platebismuth");

        // ass.exsteelplating
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_steel_plating", 1), 200, 400)
                .addIngredient(ore("oredict/plate_triple/steel"), 4)
                .addIngredient(ore("oredict/plate/titanium"), 4)
                .addIngredient(ore("oredict/bolt/steel"), 16)
                .save(writer, "assembler/exsteelplating");

        // ass.exheavyframe
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_heavy_frame", 1), 600, 800)
                .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 3)
                .addIngredient(ore("oredict/ingot/any_plastic"), 8)
                .addIngredient(ore("oredict/plate_sextuple/copper"), 4)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 1)
                .addIngredient(ore("oredict/bolt/dura_steel"), 32)
                .save(writer, "assembler/exheavyframe");

        // ass.excircuit
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_circuit", 1), 400, 4000)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 12)
                .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 8)
                .addIngredient(ore("oredict/ingot/rubber"), 4)
                .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 1000)
                .save(writer, "assembler/excircuit");

        // ass.exleadplating
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_lead_plating", 1), 400, 4000)
                .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 2)
                .addIngredient(ore("oredict/plate_triple/lead"), 8)
                .addIngredient(ore("oredict/ingot/boron"), 2)
                .addIngredient(ore("oredict/bolt/tungsten"), 32)
                .addFluidInput(ModFluids.LUBRICANT.getSource(), 1000)
                .save(writer, "assembler/exleadplating");

        // ass.exferroplating
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_ferro_plating", 1), 1200, 10000)
                .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 3)
                .addIngredient(ore("oredict/plate_triple/ferrouranium"), 4)
                .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                .addFluidInput(ModFluids.UNSATURATEDS.getSource(), 1000)
                .save(writer, "assembler/exferroplating");

        // ass.excomputer
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_computer", 1), 1200, 16000)
                .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:capacitor_board")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 8)
                .addFluidInput(ModFluids.PERFLUOROMETHYL.getSource(), 2000)
                .save(writer, "assembler/excomputer");

        // ass.bronzetubes
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_bronze_tubes", 1), 3000, 250000)
                .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 3)
                .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 1)
                .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 4)
                .addIngredient(ore("oredict/plate_sextuple/zirconium"), 1)
                .addFluidInput(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 4000)
                .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 4000)
                .save(writer, "assembler/bronzetubes");

        // ass.explastic
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_plastic", 1), 600, 20000)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                .addIngredient(ore("oredict/ingot/rubber"), 8)
                .addFluidInput(ModFluids.SOLVENT.getSource(), 1000)
                .save(writer, "assembler/explastic");

        // ass.exgold
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:item_expensive_gold_dust", 1), 600, 10000)
                .addIngredient(ore("oredict/dust/gold"), 64)
                .addIngredient(ore("oredict/dust/gold"), 64)
                .save(writer, "assembler/exgold");

        // ass.hazcloth
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hazmat_cloth", 4), 50, 100)
                .addIngredient(ore("oredict/dust/lead"), 4)
                .addIngredient(Ingredient.of(item("minecraft:string")), 8)
                .save(writer, "assembler/hazcloth");

        // ass.firecloth
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:asbestos_cloth", 4), 50, 100)
                .addIngredient(ore("oredict/ingot/asbestos"), 1)
                .addIngredient(Ingredient.of(item("minecraft:string")), 8)
                .save(writer, "assembler/firecloth");

        // ass.filtercoal
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:filter_coal", 1), 50, 100)
                .addIngredient(ore("oredict/dust/coal"), 4)
                .addIngredient(Ingredient.of(item("minecraft:string")), 2)
                .addIngredient(Ingredient.of(item("minecraft:paper")), 1)
                .save(writer, "assembler/filtercoal");

        // ass.chip [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:microchip", 1), 50, 250)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:silicon_circuit")), 1)
                .addIngredient(ore("oredict/wire_fine/gold"), 1)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/chip");
    }

    private static void part1(Consumer<FinishedRecipe> writer) {
        // ass.chipBismoid [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:bismoid_chip", 1), 100, 1500)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:silicon_circuit")), 2)
                .addIngredient(ore("oredict/nugget/any_bismoid"), 1)
                .addIngredient(ore("oredict/wire_fine/gold"), 2)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/chipbismoid");

        // ass.chipQuantum [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:quantum_chip", 1), 200, 15000)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 2)
                .addIngredient(ore("oredict/wire_dense/bscco"), 1)
                .addIngredient(Ingredient.of(item("hbm_m:pellet_charged")), 1)
                .addIngredient(ore("oredict/wire_fine/gold"), 8)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/chipquantum");

        // ass.atomicClock [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:atomic_clock", 1), 300, 1000)
                .addIngredient(ore("oredict/ingot/any_plastic"), 2)
                .addIngredient(Ingredient.of(item("hbm_m:microchip")), 3)
                .addIngredient(ore("oredict/dust/strontium"), 1)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/atomicclock");

        // ass.analogAlt [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:analog_circuit", 1), 200, 250)
                .addIngredient(Ingredient.of(item("hbm_m:pcb")), 4)
                .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                .addIngredient(ore("oredict/wire_fine/tungsten"), 8)
                .addIngredient(ore("oredict/ingot/niobium"), 1)
                .withBlueprintPool("alt..circuit")
                .save(ConfigRecipes.when(writer, "!528"), "assembler/analogalt");

        // ass.factorioChip [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:integrated_circuit", 1), 300, 20000)
                .addIngredient(ore("oredict/ingot/rubber"), 2)
                .addIngredient(ore("oredict/plate/iron"), 4)
                .addIngredient(ore("oredict/wire_fine/copper"), 8)
                .withBlueprintPool("alt..circuit")
                .save(ConfigRecipes.when(writer, "!528"), "assembler/factoriochip");

        // ass.atomicClockAlt [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:atomic_clock", 4), 300, 20000)
                .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:microchip")), 3)
                .addIngredient(ore("oredict/dust/caesium137"), 1)
                .withBlueprintPool("alt..circuit")
                .save(ConfigRecipes.when(writer, "!528"), "assembler/atomicclockalt");

        // ass.centrifugetower
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:centrifuge_element", 1), 100, 100)
                .addIngredient(ore("oredict/plate/dura_steel"), 4)
                .addIngredient(ore("oredict/plate/titanium"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                .save(writer, "assembler/centrifugetower");

        // ass.reactorcore
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:reactor_core", 1), 100, 100)
                .addIngredient(ore("oredict/plate_triple/lead"), 4)
                .addIngredient(ore("oredict/ingot/beryllium"), 8)
                .addIngredient(ore("oredict/plate/dura_steel"), 8)
                .addIngredient(ore("oredict/ingot/asbestos"), 4)
                .save(writer, "assembler/reactorcore");

        // ass.thermoelement
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:thermo_element", 1), 60, 100)
                .addIngredient(ore("oredict/plate/steel"), 1)
                .addIngredient(ore("oredict/wire_fine/mingrade"), 2)
                .addIngredient(ore("oredict/dust/nether_quartz"), 2)
                .save(writer, "assembler/thermoelement");

        // ass.thermoelementsilicon
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:thermo_element", 1), 60, 100)
                .addIngredient(ore("oredict/plate/steel"), 1)
                .addIngredient(ore("oredict/wire_fine/gold"), 2)
                .addIngredient(ore("oredict/billet/silicon"), 1)
                .save(writer, "assembler/thermoelementsilicon");

        // ass.rtgunit
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rtg_unit", 1), 100, 100)
                .addIngredient(ore("oredict/plate_triple/lead"), 2)
                .addIngredient(ore("oredict/plate/copper"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 2)
                .save(writer, "assembler/rtgunit");

        // ass.magnetron
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:magnetron", 1), 40, 100)
                .addIngredient(ore("oredict/plate/copper"), 3)
                .addIngredient(ore("oredict/wire_fine/tungsten"), 4)
                .save(writer, "assembler/magnetron");

        // ass.titaniumdrill
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drill_titanium", 1), 100, 100)
                .addIngredient(ore("oredict/plate_triple/dura_steel"), 1)
                .addIngredient(ore("oredict/plate/titanium"), 8)
                .save(writer, "assembler/titaniumdrill");

        // ass.entanglementkit
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:entanglement_kit", 1), 200, 100)
                .addIngredient(ore("oredict/plate_triple/dura_steel"), 4)
                .addIngredient(ore("oredict/plate/copper"), 24)
                .addIngredient(ore("oredict/wire_dense/gold"), 16)
                .addFluidInput(ModFluids.XENON.getSource(), 8000)
                .save(writer, "assembler/entanglementkit");

        // ass.protoreactor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dysfunctional_reactor", 1), 200, 100)
                .addIngredient(ore("oredict/shell/steel"), 4)
                .addIngredient(ore("oredict/plate_triple/lead"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:rod_quad_empty")), 10)
                .addIngredient(ore("oredict/dye_brown"), 3)
                .save(writer, "assembler/protoreactor");

        // ass.pilepabe
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pile_rod_mk2_ra226be", 1), 40, 200)
                .addIngredient(Ingredient.of(item("hbm_m:billet_ra226be")), 3)
                .save(writer, "assembler/pilepabe");

        // ass.pilepobe
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pile_rod_mk2_po210be", 1), 40, 200)
                .addIngredient(Ingredient.of(item("hbm_m:billet_po210be")), 3)
                .save(writer, "assembler/pilepobe");

        // ass.pilezr
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pile_rod_mk2_zr", 3), 40, 200)
                .addIngredient(ore("oredict/billet/zirconium"), 1)
                .save(writer, "assembler/pilezr");

        // ass.pilenu
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pile_rod_mk2_nu", 1), 40, 200)
                .addIngredient(ore("oredict/billet/uranium"), 3)
                .save(writer, "assembler/pilenu");

        // ass.partlith
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:part_lithium", 8), 40, 100)
                .addIngredient(ore("oredict/dust/lithium"), 1)
                .save(writer, "assembler/partlith");

        // ass.partberyl
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:part_beryllium", 8), 40, 100)
                .addIngredient(ore("oredict/dust/beryllium"), 1)
                .save(writer, "assembler/partberyl");

        // ass.partcoal
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:part_carbon", 8), 40, 100)
                .addIngredient(ore("oredict/dust/coal"), 1)
                .save(writer, "assembler/partcoal");

        // ass.partcop
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:part_copper", 8), 40, 100)
                .addIngredient(ore("oredict/dust/copper"), 1)
                .save(writer, "assembler/partcop");

        // ass.partplut
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:part_plutonium", 8), 40, 100)
                .addIngredient(ore("oredict/dust/plutonium"), 1)
                .save(writer, "assembler/partplut");

        // ass.cmbtile
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cmb_brick", 8), 100, 100)
                .addIngredient(ore("oredict/any/concrete"), 4)
                .addIngredient(ore("oredict/plate/cmb_steel"), 4)
                .save(writer, "assembler/cmbtile");

        // ass.cmbbrick
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cmb_brick_reinforced", 8), 100, 100)
                .addIngredient(ore("oredict/ingot/magnetized_tungsten"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:ducrete")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:cmb_brick")), 8)
                .save(writer, "assembler/cmbbrick");

        // ass.sealframe
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:seal_frame", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/dura_steel"), 1)
                .addIngredient(ore("oredict/plate_triple/steel"), 1)
                .addIngredient(ore("oredict/wire_dense/mingrade"), 1)
                .save(writer, "assembler/sealframe");

        // ass.sealcontroller
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:seal_controller", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/dura_steel"), 1)
                .addIngredient(ore("oredict/plate_triple/steel"), 1)
                .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                .addIngredient(ore("oredict/wire_dense/mingrade"), 4)
                .save(writer, "assembler/sealcontroller");

        // ass.yellowbarrel
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:yellow_barrel", 1), 400, 400)
                .addIngredient(Ingredient.of(item("hbm_m:tank_steel")), 1)
                .addIngredient(ore("oredict/plate/lead"), 2)
                .addIngredient(Ingredient.of(item("hbm_m:nuclear_waste")), 10)
                .save(writer, "assembler/yellowbarrel");

        // ass.vitrifiedbarrel
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:vitrified_barrel", 1), 400, 400)
                .addIngredient(Ingredient.of(item("hbm_m:tank_steel")), 1)
                .addIngredient(ore("oredict/plate/lead"), 2)
                .addIngredient(Ingredient.of(item("hbm_m:nuclear_waste_vitrified")), 10)
                .save(writer, "assembler/vitrifiedbarrel");
    }

    private static void part2(Consumer<FinishedRecipe> writer) {
        // ass.vaultdoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:vault_door", 1), 600, 100)
                .addIngredient(ore("oredict/ingot/steel"), 32)
                .addIngredient(ore("oredict/ingot/dura_steel"), 32)
                .addIngredient(ore("oredict/plate_triple/lead"), 8)
                .addIngredient(ore("oredict/ingot/any_rubber"), 12)
                .addIngredient(ore("oredict/bolt/dura_steel"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                .save(writer, "assembler/vaultdoor");

        // ass.blastdoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:blast_door", 1), 200, 100)
                .addIngredient(ore("oredict/ingot/steel"), 12)
                .addIngredient(ore("oredict/plate/lead"), 6)
                .addIngredient(ore("oredict/ingot/any_rubber"), 2)
                .addIngredient(ore("oredict/bolt/dura_steel"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                .save(writer, "assembler/blastdoor");

        // ass.firedoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fire_door", 1), 300, 100)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .addIngredient(ore("oredict/bolt/dura_steel"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .save(writer, "assembler/firedoor");

        // ass.seal
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:transition_seal", 1), 1200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cmb_brick_reinforced")), 16)
                .addIngredient(ore("oredict/plate/steel"), 64)
                .addIngredient(ore("oredict/ingot/any_rubber"), 36)
                .addIngredient(ore("oredict/block/steel"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 16)
                .addIngredient(ore("oredict/dye_yellow"), 4)
                .save(writer, "assembler/seal");

        // ass.slidingdoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:sliding_blast_door", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:reinforced_glass")), 4)
                .addIngredient(ore("oredict/ingot/any_rubber"), 4)
                .addIngredient(ore("oredict/bolt/dura_steel"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .save(writer, "assembler/slidingdoor");

        // ass.vehicledoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:large_vehicle_door", 1), 400, 100)
                .addIngredient(ore("oredict/plate_triple/steel"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 4)
                .addIngredient(ore("oredict/bolt/dura_steel"), 16)
                .addIngredient(ore("oredict/dye_green"), 4)
                .save(writer, "assembler/vehicledoor");

        // ass.waterdoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:water_door", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .addIngredient(ore("oredict/bolt/dura_steel"), 4)
                .addIngredient(ore("oredict/dye_red"), 1)
                .save(writer, "assembler/waterdoor");

        // ass.qedoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:qe_containment_door", 1), 400, 100)
                .addIngredient(ore("oredict/plate_triple/steel"), 6)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(ore("oredict/bolt/dura_steel"), 32)
                .addIngredient(ore("oredict/dye_black"), 4)
                .save(writer, "assembler/qedoor");

        // ass.queslidingdoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:qe_sliding_door", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(ore("oredict/bolt/dura_steel"), 4)
                .addIngredient(ore("oredict/dye_white"), 4)
                .addIngredient(Ingredient.of(item("minecraft:glass")), 4)
                .save(writer, "assembler/queslidingdoor");

        // ass.roundairlock
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:round_airlock_door", 1), 400, 100)
                .addIngredient(ore("oredict/plate_triple/steel"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 4)
                .addIngredient(ore("oredict/bolt/dura_steel"), 16)
                .addIngredient(ore("oredict/dye_green"), 4)
                .save(writer, "assembler/roundairlock");

        // ass.secureaccess
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:secure_access_door", 1), 400, 100)
                .addIngredient(ore("oredict/plate_triple/steel"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 4)
                .addIngredient(ore("oredict/bolt/dura_steel"), 32)
                .addIngredient(ore("oredict/dye_red"), 8)
                .save(writer, "assembler/secureaccess");

        // ass.slidingseal
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:sliding_seal_door", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(ore("oredict/bolt/dura_steel"), 4)
                .addIngredient(ore("oredict/dye_white"), 2)
                .save(writer, "assembler/slidingseal");

        // ass.silohatch
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:silo_hatch", 1), 200, 100)
                .addIngredient(ore("oredict/plate_sextuple/steel"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(ore("oredict/bolt/steel"), 16)
                .addIngredient(ore("oredict/dye_green"), 4)
                .save(writer, "assembler/silohatch");

        // ass.silohatchlarge
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:silo_hatch_large", 1), 300, 100)
                .addIngredient(ore("oredict/plate_sextuple/steel"), 6)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(ore("oredict/bolt/steel"), 16)
                .addIngredient(ore("oredict/dye_green"), 8)
                .save(writer, "assembler/silohatchlarge");

        // ass.cargodoor
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cargo_door", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:steel_beam")), 32)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .addIngredient(ore("oredict/bolt/dura_steel"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(ore("oredict/dye_gray"), 1)
                .save(writer, "assembler/cargodoor");

        // ass.capnuka
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:block_cap_nuka", 1), 10, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cap_nuka")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:cap_nuka")), 64)
                .save(writer, "assembler/capnuka");

        // ass.capquantum
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:block_cap_quantum", 1), 10, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cap_quantum")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:cap_quantum")), 64)
                .save(writer, "assembler/capquantum");

        // ass.capsparkle
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:block_cap_sparkle", 1), 10, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cap_sparkle")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:cap_sparkle")), 64)
                .save(writer, "assembler/capsparkle");

        // ass.caprad
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:block_cap_rad", 1), 10, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cap_rad")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:cap_rad")), 64)
                .save(writer, "assembler/caprad");

        // ass.capfritz
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:block_cap_fritz", 1), 10, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cap_fritz")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:cap_fritz")), 64)
                .save(writer, "assembler/capfritz");

        // ass.capkorl
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:block_cap_korl", 1), 10, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cap_korl")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:cap_korl")), 64)
                .save(writer, "assembler/capkorl");

        // ass.shredder [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:shredder", 1), 100, 100)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .save(w, "assembler/shredder"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:shredder", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 1)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .save(v, "assembler/shredder"), "expensive")
                .save();

        // ass.assembler
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:advanced_assembly_machine", 1), 200, 100)
                .addIngredient(ore("oredict/ingot/steel"), 4)
                .addIngredient(ore("oredict/plate/copper"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                .save(writer, "assembler/assembler");

        // ass.chemplant [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chemical_plant", 1), 200, 100)
                    .addIngredient(ore("oredict/ingot/steel"), 8)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_tungsten")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                    .save(w, "assembler/chemplant"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chemical_plant", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 3)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_tungsten")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 3)
                    .save(v, "assembler/chemplant"), "expensive")
                .save();

        // ass.chemplantAlt [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chemical_plant", 1), 200, 100)
                    .addIngredient(ore("oredict/plate/dura_steel"), 8)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .save(w, "assembler/chemplantalt"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chemical_plant", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 5)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 2)
                    .save(v, "assembler/chemplantalt"), "expensive")
                .save();

        // ass.purex [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:purex", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/ntmpipe/rubber"), 8)
                    .addIngredient(ore("oredict/plate_triple/lead"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 4)
                    .save(w, "assembler/purex"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:purex", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 2)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/ntmpipe/rubber"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 2)
                    .save(v, "assembler/purex"), "expensive")
                .save();

        // ass.precass [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_precass", 1), 1200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/ingot/zirconium"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor_board")), 4)
                    .save(w, "assembler/precass"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_precass", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 4)
                    .addIngredient(ore("oredict/ingot/zirconium"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor_board")), 8)
                    .save(v, "assembler/precass"), "expensive")
                .save();

        // ass.centrifuge [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:centrifuge", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:centrifuge_element")), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                    .save(w, "assembler/centrifuge"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:centrifuge", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:centrifuge_element")), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 3)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                    .save(v, "assembler/centrifuge"), "expensive")
                .save();

        // ass.gascent [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:gas_centrifuge", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:centrifuge_element")), 4)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 8)
                    .addIngredient(ore("oredict/ingot/workers_alloy"), 2)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .save(w, "assembler/gascent"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:gas_centrifuge", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:centrifuge_element")), 4)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .withBlueprintPool("528.gascent")
                    .save(v, "assembler/gascent"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:gas_centrifuge", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:centrifuge_element")), 4)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 8)
                    .addIngredient(ore("oredict/ingot/workers_alloy"), 2)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .withBlueprintPool("528.gascent")
                    .save(v, "assembler/gascent"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:gas_centrifuge", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:centrifuge_element")), 4)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .save(v, "assembler/gascent"), "expensive")
                .save();

        // ass.arcfurnace [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:arc_furnace", 1), 200, 100)
                    .addIngredient(ore("oredict/any/concrete"), 12)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:firebrick")), 16)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:machine_transformer")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                    .save(w, "assembler/arcfurnace"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:arc_furnace", 1), 200, 100)
                    .addIngredient(ore("oredict/any/concrete"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:firebrick")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:machine_transformer")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 3)
                    .save(v, "assembler/arcfurnace"), "expensive")
                .save();
    }

    private static void part3(Consumer<FinishedRecipe> writer) {
        // ass.acidizer [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:crystallizer", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 2)
                    .addIngredient(ore("oredict/shell/titanium"), 3)
                    .addIngredient(ore("oredict/ingot/workers_alloy"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 2)
                    .save(w, "assembler/acidizer"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:crystallizer", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 2)
                    .addIngredient(ore("oredict/shell/titanium"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .save(v, "assembler/acidizer"), "expensive")
                .save();

        // ass.electrolyzer [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:electrolyser", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(ore("oredict/shell/titanium"), 3)
                    .addIngredient(ore("oredict/ingot/rubber"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:firebrick")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 8)
                    .save(w, "assembler/electrolyzer"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:electrolyser", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(ore("oredict/shell/titanium"), 3)
                    .addIngredient(ore("oredict/ingot/rubber"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:firebrick")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 4)
                    .save(v, "assembler/electrolyzer"), "expensive")
                .save();

        // ass.rtg
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_rtg", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:rtg_unit")), 3)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .addIngredient(ore("oredict/wire_fine/mingrade"), 16)
                .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                .save(writer, "assembler/rtg");

        // ass.derrick [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:derrick", 1), 200, 100)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(ore("oredict/plate_triple/copper"), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .save(w, "assembler/derrick"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:derrick", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .withBlueprintPool("528.steel")
                    .save(v, "assembler/derrick"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:derrick", 1), 200, 100)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(ore("oredict/plate_triple/copper"), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .withBlueprintPool("528.steel")
                    .save(v, "assembler/derrick"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:derrick", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .save(v, "assembler/derrick"), "expensive")
                .save();

        // ass.pumpjack [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pumpjack", 1), 400, 100)
                    .addIngredient(ore("oredict/plate/dura_steel"), 8)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .save(w, "assembler/pumpjack"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pumpjack", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .withBlueprintPool("528.steel")
                    .save(v, "assembler/pumpjack"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pumpjack", 1), 400, 100)
                    .addIngredient(ore("oredict/plate/dura_steel"), 8)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .withBlueprintPool("528.steel")
                    .save(v, "assembler/pumpjack"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pumpjack", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .save(v, "assembler/pumpjack"), "expensive")
                .save();

        // ass.fracker [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydraulic_frackining_tower", 1), 600, 100)
                    .addIngredient(ore("oredict/shell/steel"), 24)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_smooth")), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:plate_desh")), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 16)
                    .save(w, "assembler/fracker"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydraulic_frackining_tower", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_smooth")), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 4)
                    .withBlueprintPool("528.plastic")
                    .save(v, "assembler/fracker"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydraulic_frackining_tower", 1), 600, 100)
                    .addIngredient(ore("oredict/shell/steel"), 24)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_smooth")), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:plate_desh")), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 16)
                    .withBlueprintPool("528.plastic")
                    .save(v, "assembler/fracker"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydraulic_frackining_tower", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_smooth")), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:drill_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 4)
                    .save(v, "assembler/fracker"), "expensive")
                .save();

        // ass.flarestack [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:flare_stack", 1), 100, 100)
                    .addIngredient(ore("oredict/plate/steel"), 12)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 3)
                    .save(w, "assembler/flarestack"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:flare_stack", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 8)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 3)
                    .withBlueprintPool("528.plastic")
                    .save(v, "assembler/flarestack"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:flare_stack", 1), 100, 100)
                    .addIngredient(ore("oredict/plate/steel"), 12)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 3)
                    .withBlueprintPool("528.plastic")
                    .save(v, "assembler/flarestack"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:flare_stack", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 8)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 3)
                    .save(v, "assembler/flarestack"), "expensive")
                .save();

        // ass.refinery [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:refinery", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 3)
                    .addIngredient(ore("oredict/plate/copper"), 8)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 3)
                    .save(w, "assembler/refinery"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:refinery", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 5)
                    .save(v, "assembler/refinery"), "expensive")
                .save();

        // ass.crackingtower [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cracking_tower", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:steel_scaffold")), 16)
                    .addIngredient(ore("oredict/shell/steel"), 6)
                    .addIngredient(ore("oredict/ingot/workers_alloy"), 12)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .save(w, "assembler/crackingtower"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cracking_tower", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 8)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .withBlueprintPool("528.plastic")
                    .save(v, "assembler/crackingtower"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cracking_tower", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:steel_scaffold")), 16)
                    .addIngredient(ore("oredict/shell/steel"), 6)
                    .addIngredient(ore("oredict/ingot/workers_alloy"), 12)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .withBlueprintPool("528.plastic")
                    .save(v, "assembler/crackingtower"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cracking_tower", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 8)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .save(v, "assembler/crackingtower"), "expensive")
                .save();

        // ass.radiolysis [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:radiolysis", 1), 200, 100)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/plate/lead"), 12)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 8)
                    .save(w, "assembler/radiolysis"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:radiolysis", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:thermo_element")), 8)
                    .save(v, "assembler/radiolysis"), "expensive")
                .save();

        // ass.coker [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:coker", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/plate/copper"), 8)
                    .addIngredient(ore("oredict/ingot/rubber"), 4)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .save(w, "assembler/coker"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:coker", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 8)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .withBlueprintPool("528.rubber")
                    .save(v, "assembler/coker"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:coker", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/plate/copper"), 8)
                    .addIngredient(ore("oredict/ingot/rubber"), 4)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .withBlueprintPool("528.rubber")
                    .save(v, "assembler/coker"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:coker", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 8)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .save(v, "assembler/coker"), "expensive")
                .save();

        // ass.vaccumrefinery [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:vacuum_distill", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_chip")), 4)
                    .save(w, "assembler/vaccumrefinery"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:vacuum_distill", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 2)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_chip")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .save(v, "assembler/vaccumrefinery"), "expensive")
                .save();

        // ass.reformer [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:catalytic_reformer", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 12)
                    .addIngredient(ore("oredict/plate/copper"), 8)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .save(w, "assembler/reformer"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:catalytic_reformer", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 2)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .withBlueprintPool("528.hardplastic")
                    .save(v, "assembler/reformer"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:catalytic_reformer", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 12)
                    .addIngredient(ore("oredict/plate/copper"), 8)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .withBlueprintPool("528.hardplastic")
                    .save(v, "assembler/reformer"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:catalytic_reformer", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 2)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .save(v, "assembler/reformer"), "expensive")
                .save();

        // ass.hydrotreater [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydrotreater", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .save(w, "assembler/hydrotreater"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydrotreater", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 2)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .withBlueprintPool("528.hardplastic")
                    .save(v, "assembler/hydrotreater"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydrotreater", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .withBlueprintPool("528.hardplastic")
                    .save(v, "assembler/hydrotreater"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hydrotreater", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 2)
                    .addIngredient(ore("oredict/ingot/niobium"), 8)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .save(v, "assembler/hydrotreater"), "expensive")
                .save();

        // ass.pyrooven [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pyrooven", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:cft_ingot")), 4)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .save(w, "assembler/pyrooven"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pyrooven", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 6)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:cft_ingot")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_bismuth")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 2)
                    .withBlueprintPool("528.hardplastic")
                    .save(v, "assembler/pyrooven"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pyrooven", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:cft_ingot")), 4)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .withBlueprintPool("528.hardplastic")
                    .save(v, "assembler/pyrooven"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pyrooven", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_bronze_tubes")), 6)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:cft_ingot")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_bismuth")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 2)
                    .save(v, "assembler/pyrooven"), "expensive")
                .save();

        // ass.liquefactor [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:liquefactor", 1), 200, 100)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/plate/copper"), 12)
                    .addIngredient(ore("oredict/any/tar"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_tungsten")), 8)
                    .save(w, "assembler/liquefactor"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:liquefactor", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(ore("oredict/any/tar"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 16)
                    .save(v, "assembler/liquefactor"), "expensive")
                .save();

        // ass.solidifier [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:solidifier", 1), 200, 100)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/plate/aluminum"), 12)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 4)
                    .save(w, "assembler/solidifier"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:solidifier", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 16)
                    .save(v, "assembler/solidifier"), "expensive")
                .save();

        // ass.compressor [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:compressor", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                    .save(w, "assembler/compressor"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:compressor", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 3)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 3)
                    .save(v, "assembler/compressor"), "expensive")
                .save();

        // ass.compactcompressor [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:compressor_compact", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/shell/titanium"), 4)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 4)
                    .save(w, "assembler/compactcompressor"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:compressor_compact", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 4)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .save(v, "assembler/compactcompressor"), "expensive")
                .save();

        // ass.epress [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:epress", 1), 100, 100)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(ore("oredict/ingot/any_rubber"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:part_generic_piston_hydraulic")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .save(w, "assembler/epress"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:epress", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:part_generic_piston_hydraulic")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 4)
                    .save(v, "assembler/epress"), "expensive")
                .save();

        // ass.fel [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fel", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                    .addIngredient(ore("oredict/wire_dense/gold"), 64)
                    .addIngredient(ore("oredict/plate_triple/steel"), 12)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:part_generic_glass_polarized")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:capacitor")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 4)
                    .save(w, "assembler/fel"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fel", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 3)
                    .addIngredient(ore("oredict/wire_dense/gold"), 64)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:part_generic_glass_polarized")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                    .save(v, "assembler/fel"), "expensive")
                .save();

        // ass.silex [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:silex", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 16)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/ingot/workers_alloy"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 8)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .save(w, "assembler/silex"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:silex", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .save(v, "assembler/silex"), "expensive")
                .save();

        // ass.excavator
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mining_drill", 1), 200, 100)
                .addIngredient(Ingredient.of(item("minecraft:stone_bricks")), 8)
                .addIngredient(ore("oredict/ingot/steel"), 8)
                .addIngredient(ore("oredict/ingot/iron"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                .save(writer, "assembler/excavator");

        // ass.drillsteel
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_steel", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/steel"), 12)
                .addIngredient(ore("oredict/ingot/tungsten"), 4)
                .save(writer, "assembler/drillsteel");

        // ass.drillsteeldiamond
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_steel_diamond", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:drillbit_steel")), 1)
                .addIngredient(ore("oredict/dust/diamond"), 16)
                .save(writer, "assembler/drillsteeldiamond");

        // ass.drilldura
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_hss", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/dura_steel"), 12)
                .addIngredient(ore("oredict/ingot/any_plastic"), 12)
                .addIngredient(ore("oredict/ingot/titanium"), 8)
                .save(writer, "assembler/drilldura");

        // ass.drillduradiamond
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_hss_diamond", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:drillbit_hss")), 1)
                .addIngredient(ore("oredict/dust/diamond"), 24)
                .save(writer, "assembler/drillduradiamond");

        // ass.drilldesh
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_desh", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 16)
                .addIngredient(ore("oredict/ingot/rubber"), 12)
                .addIngredient(ore("oredict/ingot/niobium"), 4)
                .save(writer, "assembler/drilldesh");

        // ass.drilldeshdiamond
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_desh_diamond", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:drillbit_desh")), 1)
                .addIngredient(ore("oredict/dust/diamond"), 32)
                .save(writer, "assembler/drilldeshdiamond");

        // ass.drilltc
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_tcalloy", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 20)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 12)
                .addIngredient(ore("oredict/ingot/rubber"), 8)
                .save(writer, "assembler/drilltc");
    }

    private static void part4(Consumer<FinishedRecipe> writer) {
        // ass.drilltcdiamond
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_tcalloy_diamond", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:drillbit_tcalloy")), 1)
                .addIngredient(ore("oredict/dust/diamond"), 48)
                .save(writer, "assembler/drilltcdiamond");

        // ass.drillferro
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_ferro", 1), 100, 100)
                .addIngredient(ore("oredict/ingot/ferrouranium"), 24)
                .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 12)
                .addIngredient(ore("oredict/ingot/bismuth"), 4)
                .save(writer, "assembler/drillferro");

        // ass.drillferrodiamond
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:drillbit_ferro_diamond", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:drillbit_ferro")), 1)
                .addIngredient(ore("oredict/dust/diamond"), 56)
                .save(writer, "assembler/drillferrodiamond");

        // ass.slopper
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:ore_slopper", 1), 200, 100)
                .addIngredient(ore("oredict/plate_triple/steel"), 6)
                .addIngredient(ore("oredict/plate/titanium"), 8)
                .addIngredient(ore("oredict/ntmpipe/copper"), 3)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                .addIngredient(Ingredient.of(item("hbm_m:analog_circuit")), 1)
                .save(writer, "assembler/slopper");

        // ass.mininglaser [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mining_laser", 1), 400, 100)
                    .addIngredient(ore("oredict/plate/steel"), 16)
                    .addIngredient(ore("oredict/shell/titanium"), 4)
                    .addIngredient(ore("oredict/plate/dura_steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:crystal_redstone")), 3)
                    .addIngredient(Ingredient.of(item("minecraft:diamond")), 3)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .save(w, "assembler/mininglaser"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mining_laser", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(ore("oredict/plate/dura_steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:crystal_redstone")), 12)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .save(v, "assembler/mininglaser"), "expensive")
                .save();

        // ass.teleporter
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_teleporter", 1), 100, 100)
                .addIngredient(ore("oredict/plate/titanium"), 12)
                .addIngredient(ore("oredict/plate/dura_steel"), 12)
                .addIngredient(ore("oredict/wire_fine/gold"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:entanglement_kit")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                .save(writer, "assembler/teleporter");

        // ass.radar [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:radar", 1), 300, 100)
                    .addIngredient(ore("oredict/plate/steel"), 12)
                    .addIngredient(ore("oredict/ingot/any_rubber"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 4)
                    .save(w, "assembler/radar"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:radar", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 4)
                    .save(v, "assembler/radar"), "expensive")
                .save();

        // ass.radarlarge [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:large_radar", 1), 400, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 6)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ingot/any_rubber"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 4)
                    .save(w, "assembler/radarlarge"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:large_radar", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/ingot/any_rubber"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 4)
                    .save(v, "assembler/radarlarge"), "expensive")
                .save();

        // ass.forcefield
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_forcefield", 1), 600, 100)
                .addIngredient(ore("oredict/plate/dura_steel"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:plate_desh")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:coil_gold_torus")), 6)
                .addIngredient(Ingredient.of(item("hbm_m:coil_magnetized_tungsten")), 12)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_radius")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_health")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:machine_transformer")), 1)
                .save(writer, "assembler/forcefield");

        // ass.strandcaster [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:strand_caster", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:firebrick")), 16)
                    .addIngredient(ore("oredict/plate_triple/steel"), 6)
                    .addIngredient(ore("oredict/plate_sextuple/copper"), 2)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(ore("oredict/any/concrete"), 8)
                    .save(w, "assembler/strandcaster"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:strand_caster", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:firebrick")), 16)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/any/concrete"), 8)
                    .save(v, "assembler/strandcaster"), "expensive")
                .save();

        // ass.assemfac [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:assembly_factory", 1), 400, 100)
                    .addIngredient(ore("oredict/ingot/dura_steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/ingot/boron"), 8)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 16)
                    .save(w, "assembler/assemfac"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:assembly_factory", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/ingot/boron"), 8)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 4)
                    .save(v, "assembler/assemfac"), "expensive")
                .save();

        // ass.chemfac [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chemical_factory", 1), 400, 100)
                    .addIngredient(ore("oredict/ingot/dura_steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/shell/steel"), 12)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_tungsten")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 16)
                    .save(w, "assembler/chemfac"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chemical_factory", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/shell/steel"), 12)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 4)
                    .save(v, "assembler/chemfac"), "expensive")
                .save();

        // ass.dieselgen [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dieselgen", 1), 200, 100)
                    .addIngredient(ore("oredict/shell/steel"), 1)
                    .addIngredient(ore("oredict/plate_triple/copper"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 4)
                    .save(w, "assembler/dieselgen"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dieselgen", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 1)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 4)
                    .save(v, "assembler/dieselgen"), "expensive")
                .save();

        // ass.combustiongen [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:combustion_engine", 1), 300, 100)
                    .addIngredient(ore("oredict/plate/steel"), 16)
                    .addIngredient(ore("oredict/ingot/copper"), 12)
                    .addIngredient(ore("oredict/wire_dense/gold"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:canister_empty")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .save(w, "assembler/combustiongen"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:combustion_engine", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 2)
                    .addIngredient(ore("oredict/wire_dense/gold"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:canister_empty")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .save(v, "assembler/combustiongen"), "expensive")
                .save();

        // ass.pistonsetsteel
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:piston_set_steel", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .addIngredient(ore("oredict/plate/copper"), 4)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/bolt/tungsten"), 16)
                .save(writer, "assembler/pistonsetsteel");

        // ass.pistonsetdura
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:piston_set_dura", 1), 200, 100)
                .addIngredient(ore("oredict/ingot/dura_steel"), 24)
                .addIngredient(ore("oredict/plate/titanium"), 8)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/bolt/dura_steel"), 16)
                .save(writer, "assembler/pistonsetdura");

        // ass.pistonsetdesh
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:piston_set_desh", 1), 200, 100)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 24)
                .addIngredient(ore("oredict/ingot/any_plastic"), 12)
                .addIngredient(ore("oredict/plate/copper"), 24)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 4)
                .save(writer, "assembler/pistonsetdesh");

        // ass.pistonsetstar
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:piston_set_starmetal", 1), 200, 100)
                .addIngredient(ore("oredict/ingot/starmetal"), 24)
                .addIngredient(ore("oredict/ingot/rubber"), 16)
                .addIngredient(ore("oredict/plate/saturnite"), 24)
                .addIngredient(ore("oredict/ingot/niobium"), 16)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 4)
                .save(writer, "assembler/pistonsetstar");

        // ass.turbofan [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turbofan", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/titanium"), 8)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 4)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_tungsten")), 1)
                    .addIngredient(ore("oredict/wire_dense/gold"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 3)
                    .save(w, "assembler/turbofan"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turbofan", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 3)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_tungsten")), 3)
                    .addIngredient(ore("oredict/wire_dense/gold"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                    .save(v, "assembler/turbofan"), "expensive")
                .save();

        // ass.gasturbine [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turbinegas", 1), 400, 100)
                    .addIngredient(ore("oredict/shell/steel"), 10)
                    .addIngredient(ore("oredict/wire_dense/gold"), 12)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_tungsten")), 1)
                    .addIngredient(ore("oredict/ingot/rubber"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 3)
                    .save(w, "assembler/gasturbine"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turbinegas", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(ore("oredict/wire_dense/gold"), 16)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_tungsten")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                    .save(v, "assembler/gasturbine"), "expensive")
                .save();

        // ass.hephaestus [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hephaestus", 1), 200, 100)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(ore("oredict/ingot/steel"), 24)
                    .addIngredient(ore("oredict/plate/copper"), 24)
                    .addIngredient(ore("oredict/ingot/niobium"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 16)
                    .save(w, "assembler/hephaestus"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:hephaestus", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 8)
                    .addIngredient(ore("oredict/ingot/niobium"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 16)
                    .save(v, "assembler/hephaestus"), "expensive")
                .save();

        // ass.iturbine [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:industrial_turbine", 1), 200, 100)
                    .addIngredient(ore("oredict/plate/steel"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_titanium")), 2)
                    .addIngredient(ore("oredict/wire_dense/gold"), 4)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 2)
                    .save(w, "assembler/iturbine"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:industrial_turbine", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_titanium")), 3)
                    .addIngredient(ore("oredict/wire_dense/gold"), 16)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                    .save(v, "assembler/iturbine"), "expensive")
                .save();

        // ass.leviturbine [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_chungus", 1), 600, 100)
                    .addIngredient(ore("oredict/shell/steel"), 6)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 16)
                    .addIngredient(ore("oredict/plate/titanium"), 12)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_tungsten")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_titanium")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:flywheel_beryllium")), 1)
                    .addIngredient(ore("oredict/wire_dense/gold"), 48)
                    .addIngredient(ore("oredict/ntmpipe/dura_steel"), 16)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 16)
                    .save(w, "assembler/leviturbine"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_chungus", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_tungsten")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:turbine_titanium")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:flywheel_beryllium")), 1)
                    .addIngredient(ore("oredict/wire_dense/gold"), 64)
                    .save(v, "assembler/leviturbine"), "expensive")
                .save();

        // ass.radgen [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_radgen", 1), 400, 100)
                    .addIngredient(ore("oredict/ingot/steel"), 8)
                    .addIngredient(ore("oredict/plate/steel"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_magnetized_tungsten")), 6)
                    .addIngredient(ore("oredict/wire_fine/magnetized_tungsten"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:reactor_core")), 3)
                    .addIngredient(ore("oredict/ingot/starmetal"), 1)
                    .addIngredient(ore("oredict/dye_red"), 1)
                    .withBlueprintPool("discover.radgen")
                    .save(w, "assembler/radgen"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_radgen", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:coil_magnetized_tungsten")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:reactor_core")), 3)
                    .addIngredient(ore("oredict/ingot/starmetal"), 1)
                    .addIngredient(ore("oredict/dye_red"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                    .withBlueprintPool("discover.radgen")
                    .save(v, "assembler/radgen"), "expensive")
                .save();

        // ass.hpcondenser [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:condenser_powered", 1), 600, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 24)
                    .addIngredient(container(ModFluids.LUBRICANT.getSource(), 1000), 4)
                    .save(w, "assembler/hpcondenser"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:condenser_powered", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 24)
                    .addIngredient(container(ModFluids.LUBRICANT.getSource(), 1000), 16)
                    .save(v, "assembler/hpcondenser"), "expensive")
                .save();

        // ass.capacitorgold
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:battery_pack_capacitor_gold", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .addIngredient(ore("oredict/wire_dense/gold"), 16)
                .save(writer, "assembler/capacitorgold");

        // ass.capacitorniobium
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:battery_pack_capacitor_niobium", 1), 100, 1000)
                .addIngredient(ore("oredict/ingot/any_plastic"), 12)
                .addIngredient(ore("oredict/wire_dense/niobium"), 24)
                .save(writer, "assembler/capacitorniobium");

        // ass.capacitortantalum
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:battery_pack_capacitor_tantalum", 1), 100, 10000)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                .addIngredient(ore("oredict/ingot/tantalum"), 24)
                .save(writer, "assembler/capacitortantalum");

        // ass.capacitorbismuth
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:battery_pack_capacitor_bismuth", 1), 100, 25000)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 24)
                .addIngredient(ore("oredict/ingot/bismuth"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:quantum_chip")), 1)
                .save(writer, "assembler/capacitorbismuth");

        // ass.capacitorspark
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:battery_pack_capacitor_spark", 1), 100, 100000)
                .addIngredient(ore("oredict/plate_triple/cmb_steel"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:spark_mix_powder")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:pellet_charged")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:quantum_chip")), 16)
                .addFluidInput(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 8000)
                .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 8000)
                .save(writer, "assembler/capacitorspark");
    }

    private static void part5(Consumer<FinishedRecipe> writer) {
        // ass.tank [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fluid_tank", 1), 200, 100)
                    .addIngredient(ore("oredict/plate/steel"), 8)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .save(w, "assembler/tank"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fluid_tank", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 4)
                    .addIngredient(ore("oredict/any/tar"), 16)
                    .save(v, "assembler/tank"), "expensive")
                .save();

        // ass.bigasstank [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_bigasstank", 1), 200, 100)
                    .addIngredient(ore("oredict/plate/steel"), 16)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:steel_scaffold")), 16)
                    .save(w, "assembler/bigasstank"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_bigasstank", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:steel_scaffold")), 16)
                    .addIngredient(ore("oredict/any/tar"), 16)
                    .save(v, "assembler/bigasstank"), "expensive")
                .save();

        // ass.orbus [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:orbus", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/plate_triple/saturnite"), 4)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_sc_po210")), 1)
                    .save(w, "assembler/orbus"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:orbus", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 8)
                    .addIngredient(ore("oredict/plate_triple/saturnite"), 16)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_sc_po210")), 1)
                    .save(v, "assembler/orbus"), "expensive")
                .save();

        // ass.cyclotron [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cyclotron", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                    .addIngredient(ore("oredict/wire_dense/neodymium"), 32)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 24)
                    .addIngredient(ore("oredict/ingot/rubber"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 16)
                    .save(w, "assembler/cyclotron"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:cyclotron", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 8)
                    .addIngredient(ore("oredict/wire_dense/neodymium"), 32)
                    .addIngredient(ore("oredict/plate_sextuple/aluminum"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 8)
                    .save(v, "assembler/cyclotron"), "expensive")
                .save();

        // ass.beamline [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:beamline", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(ore("oredict/wire_dense/gold"), 4)
                    .save(w, "assembler/beamline"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:beamline", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 4)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(ore("oredict/wire_dense/gold"), 4)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/beamline"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:beamline", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 8)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(ore("oredict/wire_dense/gold"), 4)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/beamline"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:beamline", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 4)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .addIngredient(ore("oredict/wire_dense/gold"), 4)
                    .save(v, "assembler/beamline"), "expensive")
                .save();

        // ass.rfc [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rfc", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/plate/copper"), 64)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .save(w, "assembler/rfc"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rfc", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/plate/copper"), 64)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/rfc"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rfc", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/plate/copper"), 64)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/rfc"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rfc", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/plate/copper"), 64)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .save(v, "assembler/rfc"), "expensive")
                .save();

        // ass.quadrupole [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:quadrupole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 1)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .save(w, "assembler/quadrupole"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:quadrupole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/quadrupole"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:quadrupole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 1)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 1)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/quadrupole"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:quadrupole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .save(v, "assembler/quadrupole"), "expensive")
                .save();

        // ass.dipole [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dipole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 2)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .save(w, "assembler/dipole"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dipole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/dipole"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dipole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 2)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/dipole"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:dipole", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .save(v, "assembler/dipole"), "expensive")
                .save();

        // ass.source [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_source", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 1)
                    .save(w, "assembler/source"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_source", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 4)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/source"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_source", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(ore("oredict/plate_triple/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 1)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/source"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_source", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 4)
                    .save(v, "assembler/source"), "expensive")
                .save();

        // ass.detector [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_detector", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(ore("oredict/plate_triple/steel"), 24)
                    .addIngredient(ore("oredict/wire_dense/gold"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .save(w, "assembler/detector"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_detector", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 32)
                    .addIngredient(ore("oredict/wire_dense/gold"), 64)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 8)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/detector"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_detector", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(ore("oredict/plate_triple/steel"), 24)
                    .addIngredient(ore("oredict/wire_dense/gold"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/detector"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_detector", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:beamline")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 32)
                    .addIngredient(ore("oredict/wire_dense/gold"), 64)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 8)
                    .save(v, "assembler/detector"), "expensive")
                .save();

        // ass.pagold
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_coil_gold", 1), 400, 100)
                .addIngredient(ore("oredict/wire_dense/gold"), 64)
                .addIngredient(ore("oredict/wire_dense/gold"), 64)
                .save(writer, "assembler/pagold");

        // ass.panbti
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_coil_niobium", 1), 400, 100)
                .addIngredient(ore("oredict/wire_dense/niobium"), 64)
                .addIngredient(ore("oredict/wire_dense/titanium"), 64)
                .save(writer, "assembler/panbti");

        // ass.pabscco
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_coil_bscco", 1), 400, 100)
                .addIngredient(ore("oredict/wire_dense/bscco"), 64)
                .addIngredient(ore("oredict/ingot/any_plastic"), 64)
                .save(writer, "assembler/pabscco");

        // ass.pachlorophyte
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pa_coil_chlorophyte", 1), 400, 100)
                .addIngredient(ore("oredict/wire_dense/copper"), 64)
                .addIngredient(ore("oredict/wire_dense/copper"), 64)
                .addIngredient(Ingredient.of(item("hbm_m:powder_chlorophyte")), 16)
                .save(writer, "assembler/pachlorophyte");

        // ass.exposurechamber [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:exposure_chamber", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/aluminum"), 12)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 12)
                    .addIngredient(ore("oredict/wire_dense/gold"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_tantalum")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 16)
                    .save(w, "assembler/exposurechamber"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:exposure_chamber", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 8)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 24)
                    .addIngredient(ore("oredict/wire_dense/gold"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 2)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/exposurechamber"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:exposure_chamber", 1), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/aluminum"), 12)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 12)
                    .addIngredient(ore("oredict/wire_dense/gold"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_tantalum")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:glass_quartz")), 16)
                    .withBlueprintPool("528.chip_quantum")
                    .save(v, "assembler/exposurechamber"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:exposure_chamber", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 8)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 24)
                    .addIngredient(ore("oredict/wire_dense/gold"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 2)
                    .save(v, "assembler/exposurechamber"), "expensive")
                .save();

        // ass.pileblock [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pile_brick", 1), 20, 250)
                    .addIngredient(ore("oredict/plank_wood"), 1)
                    .addIngredient(ore("oredict/ingot/graphite"), 4)
                    .addIngredient(ore("oredict/bolt/steel"), 2)
                    .save(w, "assembler/pileblock"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pile_brick", 1), 20, 250)
                    .addIngredient(ore("oredict/plank_wood"), 1)
                    .addIngredient(ore("oredict/ingot/graphite"), 8)
                    .addIngredient(ore("oredict/plate/steel"), 1)
                    .save(v, "assembler/pileblock"), "expensive")
                .save();

        // ass.cirnox [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:zirnox", 1), 600, 100)
                    .addIngredient(ore("oredict/shell/steel"), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(ore("oredict/ingot/boron"), 8)
                    .addIngredient(ore("oredict/ingot/graphite"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/any/concrete"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 4)
                    .save(w, "assembler/cirnox"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:zirnox", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 4)
                    .addIngredient(ore("oredict/ingot/graphite"), 16)
                    .addIngredient(ore("oredict/ingot/rubber"), 16)
                    .addIngredient(ore("oredict/any/concrete"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 2)
                    .save(v, "assembler/cirnox"), "expensive")
                .save();

        // ass.rbmk [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_blank", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_asbestos")), 4)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 2)
                    .save(w, "assembler/rbmk"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_blank", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_asbestos")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .withBlueprintPool("528.ferrouranium")
                    .save(v, "assembler/rbmk"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_blank", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_asbestos")), 4)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 2)
                    .withBlueprintPool("528.ferrouranium")
                    .save(v, "assembler/rbmk"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_blank", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:concrete_asbestos")), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(ore("oredict/plate/copper"), 16)
                    .save(v, "assembler/rbmk"), "expensive")
                .save();

        // ass.rbmkautoloader [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_autoloader", 1), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 4)
                    .addIngredient(ore("oredict/plate_triple/lead"), 4)
                    .addIngredient(ore("oredict/ingot/boron"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .save(w, "assembler/rbmkautoloader"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_autoloader", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .withBlueprintPool("528.ferrouranium")
                    .save(v, "assembler/rbmkautoloader"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_autoloader", 1), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 4)
                    .addIngredient(ore("oredict/plate_triple/lead"), 4)
                    .addIngredient(ore("oredict/ingot/boron"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .withBlueprintPool("528.ferrouranium")
                    .save(v, "assembler/rbmkautoloader"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rbmk_autoloader", 1), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 3)
                    .save(v, "assembler/rbmkautoloader"), "expensive")
                .save();

        // ass.pwrfuel [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_fuel", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/lead"), 4)
                    .addIngredient(ore("oredict/plate_sextuple/zirconium"), 2)
                    .save(w, "assembler/pwrfuel"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_fuel", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(ore("oredict/plate_sextuple/zirconium"), 2)
                    .save(v, "assembler/pwrfuel"), "expensive")
                .save();

        // ass.pwrcontrol [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_control", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/ingot/boron"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .save(w, "assembler/pwrcontrol"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_control", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 1)
                    .addIngredient(ore("oredict/ingot/boron"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .save(v, "assembler/pwrcontrol"), "expensive")
                .save();

        // ass.pwrchannel [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_channel", 4), 200, 500)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(ore("oredict/plate/copper"), 4)
                    .save(w, "assembler/pwrchannel"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_channel", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .save(v, "assembler/pwrchannel"), "expensive")
                .save();

        // ass.pwrheatex [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_heatex", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 1)
                    .save(w, "assembler/pwrheatex"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_heatex", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 2)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addFluidInput(ModFluids.PERFLUOROMETHYL.getSource(), 4000)
                    .save(v, "assembler/pwrheatex"), "expensive")
                .save();

        // ass.pwrheatsink [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_heatsink", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/saturnite"), 4)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .save(w, "assembler/pwrheatsink"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_heatsink", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/saturnite"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 1)
                    .save(v, "assembler/pwrheatsink"), "expensive")
                .save();

        // ass.pwrreflector [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_reflector", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 4)
                    .save(w, "assembler/pwrreflector"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_reflector", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 16)
                    .save(v, "assembler/pwrreflector"), "expensive")
                .save();

        // ass.pwrreflectoralt [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_reflector", 4), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 16)
                    .save(w, "assembler/pwrreflectoralt"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_reflector", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_steel_plating")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .save(v, "assembler/pwrreflectoralt"), "expensive")
                .save();

        // ass.pwrcasing [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_casing", 4), 200, 500)
                    .addIngredient(ore("oredict/plate/lead"), 4)
                    .addIngredient(ore("oredict/any/concrete"), 4)
                    .save(w, "assembler/pwrcasing"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_casing", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(ore("oredict/any/concrete"), 4)
                    .save(v, "assembler/pwrcasing"), "expensive")
                .save();

        // ass.pwrcontroller [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_controller", 1), 200, 500)
                    .addIngredient(ore("oredict/plate_triple/lead"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 4)
                    .save(w, "assembler/pwrcontroller"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_controller", 1), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 3)
                    .save(v, "assembler/pwrcontroller"), "expensive")
                .save();

        // ass.pwrport [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_port", 4), 200, 500)
                    .addIngredient(ore("oredict/plate/lead"), 4)
                    .addIngredient(ore("oredict/any/concrete"), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .save(w, "assembler/pwrport"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_port", 4), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(ore("oredict/any/concrete"), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .save(v, "assembler/pwrport"), "expensive")
                .save();

        // ass.pwrneutronsource [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_neutron_source", 1), 200, 500)
                    .addIngredient(ore("oredict/plate_sextuple/zirconium"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:billet_ra226be")), 3)
                    .save(w, "assembler/pwrneutronsource"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pwr_neutron_source", 1), 200, 500)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 1)
                    .addIngredient(ore("oredict/plate_sextuple/zirconium"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:billet_ra226be")), 3)
                    .save(v, "assembler/pwrneutronsource"), "expensive")
                .save();
    }

    private static void part6(Consumer<FinishedRecipe> writer) {
        // ass.fusioncore [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:struct_torus_core", 1), 600, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 8)
                    .save(w, "assembler/fusioncore"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:struct_torus_core", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusioncore"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:struct_torus_core", 1), 600, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 8)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusioncore"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:struct_torus_core", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                    .save(v, "assembler/fusioncore"), "expensive")
                .save();

        // ass.fusionbscco [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component", 2), 100, 100)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 1)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 1)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .save(w, "assembler/fusionbscco"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component", 2), 100, 100)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 1)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusionbscco"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component", 2), 100, 100)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 1)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 1)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusionbscco"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component", 2), 100, 100)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 1)
                    .save(v, "assembler/fusionbscco"), "expensive")
                .save();

        // ass.fusionblanket [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_blanket", 4), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/tungsten"), 1)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 2)
                    .addIngredient(ore("oredict/ingot/beryllium"), 4)
                    .save(w, "assembler/fusionblanket"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_blanket", 4), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/tungsten"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(ore("oredict/ingot/beryllium"), 4)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusionblanket"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_blanket", 4), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/tungsten"), 1)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 2)
                    .addIngredient(ore("oredict/ingot/beryllium"), 4)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusionblanket"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_blanket", 4), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/tungsten"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 1)
                    .addIngredient(ore("oredict/ingot/beryllium"), 4)
                    .save(v, "assembler/fusionblanket"), "expensive")
                .save();

        // ass.fusionpipes [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_motor", 4), 100, 100)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .save(w, "assembler/fusionpipes"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_motor", 4), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 1)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusionpipes"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_motor", 4), 100, 100)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/fusionpipes"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fusion_component_motor", 4), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 1)
                    .addIngredient(ore("oredict/ntmpipe/copper"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_circuit")), 1)
                    .save(v, "assembler/fusionpipes"), "expensive")
                .save();

        // ass.fusionklystron [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:klystron", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_sextuple/tungsten"), 4)
                    .addIngredient(ore("oredict/plate_triple/any_resistant_alloy"), 16)
                    .addIngredient(ore("oredict/plate/copper"), 32)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 2)
                    .save(w, "assembler/fusionklystron"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:klystron", 1), 300, 100)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 1)
                    .save(v, "assembler/fusionklystron"), "expensive")
                .save();

        // ass.fusioncollector [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:collector", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_triple/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/plate/steel"), 16)
                    .addIngredient(ore("oredict/ingot/graphite"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .save(w, "assembler/fusioncollector"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:collector", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 2)
                    .addIngredient(ore("oredict/ingot/graphite"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 4)
                    .save(v, "assembler/fusioncollector"), "expensive")
                .save();

        // ass.fusionbreeder [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:breeder_fusion", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_triple/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(ore("oredict/ingot/boron"), 16)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .save(w, "assembler/fusionbreeder"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:breeder_fusion", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 4)
                    .addIngredient(ore("oredict/ingot/boron"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 4)
                    .save(v, "assembler/fusionbreeder"), "expensive")
                .save();

        // ass.fusionboiler [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boiler_fusion", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_triple/any_resistant_alloy"), 16)
                    .addIngredient(ore("oredict/shell/copper"), 16)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                    .save(w, "assembler/fusionboiler"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boiler_fusion", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_heavy_frame")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 16)
                    .save(v, "assembler/fusionboiler"), "expensive")
                .save();

        // ass.fusionmhdt [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mhdt", 1), 1200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 16)
                    .addIngredient(ore("oredict/plate_sextuple/copper"), 64)
                    .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 16)
                    .addIngredient(ore("oredict/wire_dense/schrabidate"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .save(w, "assembler/fusionmhdt"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mhdt", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 16)
                    .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 8)
                    .withBlueprintPool("528.chlorophyte")
                    .save(v, "assembler/fusionmhdt"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mhdt", 1), 1200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 16)
                    .addIngredient(ore("oredict/plate_sextuple/copper"), 64)
                    .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 16)
                    .addIngredient(ore("oredict/wire_dense/schrabidate"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 4)
                    .withBlueprintPool("528.chlorophyte")
                    .save(v, "assembler/fusionmhdt"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mhdt", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 16)
                    .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 8)
                    .save(v, "assembler/fusionmhdt"), "expensive")
                .save();

        // ass.fusioncoupler [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:coupler", 1), 300, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 4)
                    .addIngredient(ore("oredict/plate/copper"), 32)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .save(w, "assembler/fusioncoupler"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:coupler", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 4)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 2)
                    .save(v, "assembler/fusioncoupler"), "expensive")
                .save();

        // ass.fusionplasmaforge [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plasma_forge", 1), 1200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 32)
                    .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .save(w, "assembler/fusionplasmaforge"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plasma_forge", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 16)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 4)
                    .withBlueprintPool("528.chlorophyte")
                    .save(v, "assembler/fusionplasmaforge"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plasma_forge", 1), 1200, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 8)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 32)
                    .addIngredient(ore("oredict/plate_triple/any_bismoid_bronze"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 4)
                    .withBlueprintPool("528.chlorophyte")
                    .save(v, "assembler/fusionplasmaforge"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:plasma_forge", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_plastic")), 16)
                    .addIngredient(ore("oredict/wire_dense/bscco"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_computer")), 4)
                    .save(v, "assembler/fusionplasmaforge"), "expensive")
                .save();

        // ass.watzrod [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_element", 3), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/ingot/zirconium"), 2)
                    .addIngredient(ore("oredict/ingot/saturnite"), 2)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .save(w, "assembler/watzrod"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_element", 3), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 3)
                    .addIngredient(ore("oredict/ingot/saturnite"), 6)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/watzrod"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_element", 3), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/ingot/zirconium"), 2)
                    .addIngredient(ore("oredict/ingot/saturnite"), 2)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/watzrod"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_element", 3), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 3)
                    .addIngredient(ore("oredict/ingot/saturnite"), 6)
                    .addIngredient(ore("oredict/ingot/any_hard_plastic"), 4)
                    .save(v, "assembler/watzrod"), "expensive")
                .save();

        // ass.watzcooler [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_cooler", 3), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 2)
                    .save(w, "assembler/watzcooler"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_cooler", 3), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 3)
                    .addIngredient(ore("oredict/ingot/rubber"), 8)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/watzcooler"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_cooler", 3), 200, 100)
                    .addIngredient(ore("oredict/plate_triple/steel"), 2)
                    .addIngredient(ore("oredict/plate_triple/copper"), 4)
                    .addIngredient(ore("oredict/ingot/rubber"), 2)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/watzcooler"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_cooler", 3), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_ferro_plating")), 3)
                    .addIngredient(ore("oredict/ingot/rubber"), 8)
                    .save(v, "assembler/watzcooler"), "expensive")
                .save();

        // ass.watzcasing [Fassungen: 528+expensive; 528; expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_end", 3), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 1)
                    .addIngredient(ore("oredict/ingot/boron"), 3)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 2)
                    .save(w, "assembler/watzcasing"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_end", 3), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 6)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 1)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/watzcasing"), "528", "expensive")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_end", 3), 100, 100)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 1)
                    .addIngredient(ore("oredict/ingot/boron"), 3)
                    .addIngredient(ore("oredict/plate_sextuple/steel"), 2)
                    .withBlueprintPool("528.tcalloy")
                    .save(v, "assembler/watzcasing"), "528")
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:watz_end", 3), 100, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:item_expensive_lead_plating")), 6)
                    .addIngredient(ore("oredict/plate_sextuple/any_resistant_alloy"), 1)
                    .save(v, "assembler/watzcasing"), "expensive")
                .save();

        // ass.overdrive1 [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:upgrade_overdrive_1", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_effect_3")), 1)
                .addIngredient(ore("oredict/ingot/saturnite"), 16)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 16)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/overdrive1");

        // ass.overdrive2 [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:upgrade_overdrive_2", 1), 600, 100)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_overdrive_1")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_effect_3")), 1)
                .addIngredient(ore("oredict/ingot/saturnite"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:cft_ingot")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:capacitor_board")), 16)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/overdrive2");

        // ass.overdrive3 [nur bei !528]
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:upgrade_overdrive_3", 1), 1200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_overdrive_2")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_effect_3")), 1)
                .addIngredient(ore("oredict/ingot/any_bismoid_bronze"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:cft_ingot")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                .save(ConfigRecipes.when(writer, "!528"), "assembler/overdrive3");

        // ass.chopper
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:chopper", 1), 1200, 100)
                .addIngredient(ore("oredict/plate_triple/cmb_steel"), 24)
                .addIngredient(ore("oredict/plate/steel"), 32)
                .addIngredient(ore("oredict/wire_fine/magnetized_tungsten"), 48)
                .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 1)
                .save(writer, "assembler/chopper");

        // ass.ballsotron
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:spawn_worm", 1), 1200, 100)
                .addIngredient(ore("oredict/plate_sextuple/titanium"), 32)
                .addIngredient(ore("oredict/ingot/rubber"), 64)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 64)
                .addIngredient(ore("oredict/wire_dense/gold"), 64)
                .addIngredient(ore("oredict/block/uranium238"), 10)
                .addIngredient(Ingredient.of(item("hbm_m:mech_key")), 1)
                .save(writer, "assembler/ballsotron");

        // ass.clusterpellets
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pellet_cluster", 1), 50, 100)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 1)
                .save(writer, "assembler/clusterpellets");

        // ass.buckshot
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:pellet_buckshot", 1), 50, 100)
                .addIngredient(ore("oredict/nugget/lead"), 6)
                .save(writer, "assembler/buckshot");

        // ass.minenaval
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mine_naval", 1), 300, 100)
                .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .addIngredient(ore("oredict/ingot/any_plasticexplosive"), 24)
                .save(writer, "assembler/minenaval");

        // ass.gadget [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_gadget", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_flat")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:pedestal_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                    .addIngredient(ore("oredict/dye_gray"), 8)
                    .save(w, "assembler/gadget"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_gadget", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_flat")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:pedestal_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                    .addIngredient(ore("oredict/dye_gray"), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/gadget"), "528")
                .save();

        // ass.littleboy [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_boy", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_small_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 2)
                    .addIngredient(ore("oredict/dye_blue"), 4)
                    .save(w, "assembler/littleboy"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_boy", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_small_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 2)
                    .addIngredient(ore("oredict/dye_blue"), 4)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/littleboy"), "528")
                .save();

        // ass.fatman [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_fat_man", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_big_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                    .addIngredient(ore("oredict/dye_yellow"), 6)
                    .save(w, "assembler/fatman"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_fat_man", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_big_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                    .addIngredient(ore("oredict/dye_yellow"), 6)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/fatman"), "528")
                .save();

        // ass.ivymike [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_mike", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/aluminum"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 8)
                    .addIngredient(ore("oredict/dye_light_gray"), 16)
                    .save(w, "assembler/ivymike"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_mike", 1), 600, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/aluminum"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 8)
                    .addIngredient(ore("oredict/dye_light_gray"), 16)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/ivymike"), "528")
                .save();

        // ass.tsarbomba [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_tsar", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/titanium"), 6)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_tri_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 16)
                    .addIngredient(ore("oredict/dye_black"), 8)
                    .save(w, "assembler/tsarbomba"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_tsar", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/titanium"), 6)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_tri_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 16)
                    .addIngredient(ore("oredict/dye_black"), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/tsarbomba"), "528")
                .save();

        // ass.ninadidnothingwrong [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_prototype", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:dysfunctional_reactor")), 1)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:euphemium_ingot")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 8)
                    .save(w, "assembler/ninadidnothingwrong"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_prototype", 1), 300, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:dysfunctional_reactor")), 1)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:euphemium_ingot")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/ninadidnothingwrong"), "528")
                .save();

        // ass.fleija [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_fleija", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/aluminum"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_quad_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .addIngredient(ore("oredict/dye_white"), 4)
                    .save(w, "assembler/fleija"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_fleija", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/aluminum"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_quad_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .addIngredient(ore("oredict/dye_white"), 4)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/fleija"), "528")
                .save();

        // ass.solinium [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_solinium", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_quad_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .addIngredient(ore("oredict/dye_gray"), 8)
                    .save(w, "assembler/solinium"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_solinium", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_quad_titanium")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .addIngredient(ore("oredict/dye_gray"), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/solinium"), "528")
                .save();
    }

    private static void part7(Consumer<FinishedRecipe> writer) {
        // ass.n2mine [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_n2", 1), 200, 100)
                    .addIngredient(ore("oredict/shell/steel"), 6)
                    .addIngredient(ore("oredict/wire_fine/magnetized_tungsten"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 2)
                    .addIngredient(ore("oredict/dye_green"), 8)
                    .save(w, "assembler/n2mine"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_n2", 1), 200, 100)
                    .addIngredient(ore("oredict/shell/steel"), 6)
                    .addIngredient(ore("oredict/wire_fine/magnetized_tungsten"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 2)
                    .addIngredient(ore("oredict/dye_green"), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/n2mine"), "528")
                .save();

        // ass.balefirebomb [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_fstbmb", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/titanium"), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_big_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:magic_powder")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 4)
                    .addIngredient(ore("oredict/dye_gray"), 8)
                    .save(w, "assembler/balefirebomb"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_fstbmb", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:sphere_steel")), 1)
                    .addIngredient(ore("oredict/shell/titanium"), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_big_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:magic_powder")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 4)
                    .addIngredient(ore("oredict/dye_gray"), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/balefirebomb"), "528")
                .save();

        // ass.customnuke [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_custom", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_small_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 8)
                    .addIngredient(ore("oredict/dye_gray"), 4)
                    .save(w, "assembler/customnuke"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:nuke_custom", 1), 300, 100)
                    .addIngredient(ore("oredict/shell/steel"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:fins_small_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 8)
                    .addIngredient(ore("oredict/dye_gray"), 4)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/customnuke"), "528")
                .save();

        // ass.levibomb
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:float_bomb", 1), 200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 12)
                .addIngredient(ore("oredict/nugget/schrabidium"), 3)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 4)
                .addIngredient(ore("oredict/wire_dense/gold"), 8)
                .save(writer, "assembler/levibomb");

        // ass.endobomb
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:therm_endo", 1), 200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:ice_powder")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:coil_gold")), 4)
                .save(writer, "assembler/endobomb");

        // ass.exobomb
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:therm_exo", 1), 200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 12)
                .addIngredient(ore("oredict/dust/red_phosphorus"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:coil_gold")), 4)
                .save(writer, "assembler/exobomb");

        // ass.explosivelenses1
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fat_man_explosive", 1), 400, 100)
                .addIngredient(ore("oredict/plate/aluminum"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:det_cord")), 8)
                .addIngredient(ore("oredict/plate/saturnite"), 2)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 20)
                .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                .save(writer, "assembler/explosivelenses1");

        // ass.explosivelenses2
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:explosive_lenses", 1), 400, 100)
                .addIngredient(ore("oredict/plate/aluminum"), 8)
                .addIngredient(ore("oredict/ingot/any_plasticexplosive"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 16)
                .addIngredient(ore("oredict/ingot/rubber"), 2)
                .save(writer, "assembler/explosivelenses2");

        // ass.wiring
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:gadget_wireing", 1), 200, 100)
                .addIngredient(ore("oredict/wire_fine/gold"), 24)
                .save(writer, "assembler/wiring");

        // ass.core1
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:gadget_core", 1), 1200, 100)
                .addIngredient(ore("oredict/nugget/plutonium239"), 7)
                .addIngredient(ore("oredict/nugget/uranium238"), 3)
                .save(writer, "assembler/core1");

        // ass.boyshield
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boy_shielding", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 12)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .save(writer, "assembler/boyshield");

        // ass.boytarget
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boy_target", 1), 200, 100)
                .addIngredient(ore("oredict/nugget/uranium235"), 18)
                .save(writer, "assembler/boytarget");

        // ass.boybullet
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boy_bullet", 1), 200, 100)
                .addIngredient(ore("oredict/nugget/uranium235"), 9)
                .save(writer, "assembler/boybullet");

        // ass.boypropellant
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boy_propellant", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:cordite")), 8)
                .addIngredient(ore("oredict/plate/iron"), 8)
                .addIngredient(ore("oredict/plate/aluminum"), 4)
                .addIngredient(ore("oredict/wire_fine/mingrade"), 4)
                .save(writer, "assembler/boypropellant");

        // ass.boyigniter
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:boy_igniter", 1), 200, 100)
                .addIngredient(ore("oredict/shell/aluminum"), 3)
                .addIngredient(ore("oredict/plate_triple/dura_steel"), 1)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .addIngredient(ore("oredict/wire_fine/mingrade"), 16)
                .save(writer, "assembler/boyigniter");

        // ass.manigniter
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fat_man_igniter", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 3)
                .addIngredient(ore("oredict/wire_fine/gold"), 24)
                .save(writer, "assembler/manigniter");

        // ass.mancore
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fat_man_core", 1), 1200, 100)
                .addIngredient(ore("oredict/nugget/plutonium239"), 8)
                .addIngredient(ore("oredict/nugget/beryllium"), 2)
                .save(writer, "assembler/mancore");

        // ass.mikecore
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mike_core", 1), 1200, 100)
                .addIngredient(ore("oredict/nugget/uranium238"), 24)
                .addIngredient(ore("oredict/plate/lead"), 6)
                .save(writer, "assembler/mikecore");

        // ass.mikedeut
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mike_deut", 1), 600, 100)
                .addIngredient(ore("oredict/plate/weapon_steel"), 16)
                .addIngredient(ore("oredict/plate/titanium"), 16)
                .addFluidInput(ModFluids.DEUTERIUM.getSource(), 10000)
                .save(writer, "assembler/mikedeut");

        // ass.mikecooler
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mike_cooling_unit", 1), 300, 100)
                .addIngredient(ore("oredict/plate/dura_steel"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 5)
                .addIngredient(Ingredient.of(item("hbm_m:coil_tungsten")), 5)
                .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                .save(writer, "assembler/mikecooler");

        // ass.fleijaigniter
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fleija_igniter", 1), 200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 6)
                .addIngredient(ore("oredict/wire_fine/schrabidium"), 2)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .save(writer, "assembler/fleijaigniter");

        // ass.fleijacore
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fleija_core", 1), 600, 100)
                .addIngredient(ore("oredict/nugget/uranium235"), 8)
                .addIngredient(ore("oredict/nugget/neptunium237"), 2)
                .addIngredient(ore("oredict/nugget/beryllium"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:coil_copper")), 2)
                .save(writer, "assembler/fleijacore");

        // ass.fleijacharge
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fleija_propellant", 1), 300, 100)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 3)
                .addIngredient(ore("oredict/plate/schrabidium"), 8)
                .save(writer, "assembler/fleijacharge");

        // ass.soliniumigniter
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:solinium_igniter", 1), 200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 4)
                .addIngredient(ore("oredict/wire_fine/mingrade"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:coil_gold")), 1)
                .save(writer, "assembler/soliniumigniter");

        // ass.soliniumcore
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:solinium_core", 1), 600, 100)
                .addIngredient(ore("oredict/nugget/solinium"), 9)
                .addIngredient(ore("oredict/nugget/euphemium"), 1)
                .save(writer, "assembler/soliniumcore");

        // ass.soliniumcharge
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:solinium_propellant", 1), 300, 100)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 3)
                .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:plate_polymer")), 6)
                .addIngredient(ore("oredict/wire_fine/tungsten"), 6)
                .addIngredient(Ingredient.of(item("hbm_m:biomass_compressed")), 4)
                .save(writer, "assembler/soliniumcharge");

        // ass.turretchekhov [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_chekhov", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                    .addIngredient(ore("oredict/gun_mechanism/gun_metal"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_iron")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/turretchekhov"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_chekhov", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                    .addIngredient(ore("oredict/gun_mechanism/gun_metal"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_iron")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/turretchekhov"), "528")
                .save();

        // ass.turretfriendly [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_friendly", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                    .addIngredient(ore("oredict/gun_mechanism/gun_metal"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_iron")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/turretfriendly"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_friendly", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                    .addIngredient(ore("oredict/gun_mechanism/gun_metal"), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_iron")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/turretfriendly"), "528")
                .save();

        // ass.turretjeremy [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_jeremy", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(ore("oredict/shell/steel"), 3)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/turretjeremy"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_jeremy", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(ore("oredict/shell/steel"), 3)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/turretjeremy"), "528")
                .save();

        // ass.turrettauon [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_tauon", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_niobium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(ore("oredict/ingot/copper"), 32)
                    .addIngredient(ore("oredict/gun_mechanism/saturnite"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/turrettauon"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_tauon", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_niobium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 1)
                    .addIngredient(ore("oredict/ingot/copper"), 32)
                    .addIngredient(ore("oredict/gun_mechanism/saturnite"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/turrettauon"), "528")
                .save();
    }

    private static void part8(Consumer<FinishedRecipe> writer) {
        // ass.turretrichard [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_richard", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 2)
                    .addIngredient(ore("oredict/shell/steel"), 8)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/turretrichard"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_richard", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 2)
                    .addIngredient(ore("oredict/shell/steel"), 8)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/turretrichard"), "528")
                .save();

        // ass.turrethoward [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_howard", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 10)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/turrethoward"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_howard", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 10)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:crate_steel")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/turrethoward"), "528")
                .save();

        // ass.maxwell [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_maxwell", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_niobium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(ore("oredict/gun_mechanism/saturnite"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/maxwell"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_maxwell", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_niobium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 2)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                    .addIngredient(ore("oredict/gun_mechanism/saturnite"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 16)
                    .addIngredient(ore("oredict/ingot/any_resistant_alloy"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/maxwell"), "528")
                .save();

        // ass.fritz [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_fritz", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(ore("oredict/gun_mechanism/gun_metal"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:barrel_steel")), 1)
                    .save(w, "assembler/fritz"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_fritz", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lead")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:motor")), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 8)
                    .addIngredient(ore("oredict/gun_mechanism/gun_metal"), 3)
                    .addIngredient(Ingredient.of(item("hbm_m:barrel_steel")), 1)
                    .withBlueprintPool("528.bmg")
                    .save(v, "assembler/fritz"), "528")
                .save();

        // ass.arty [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_arty", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:radar")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/arty"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_arty", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 3)
                    .addIngredient(ore("oredict/ntmpipe/steel"), 12)
                    .addIngredient(ore("oredict/gun_mechanism/weapon_steel"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:radar")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.arty")
                    .save(v, "assembler/arty"), "528")
                .save();

        // ass.himars [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_himars", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 8)
                    .addIngredient(ore("oredict/gun_mechanism/saturnite"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:radar")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .save(w, "assembler/himars"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:turret_himars", 1), 1200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(ore("oredict/ingot/steel"), 64)
                    .addIngredient(ore("oredict/ingot/any_plastic"), 64)
                    .addIngredient(Ingredient.of(item("hbm_m:motor_desh")), 5)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 8)
                    .addIngredient(ore("oredict/gun_mechanism/saturnite"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:radar")), 1)
                    .addIngredient(Ingredient.of(item("hbm_m:crt_display")), 1)
                    .withBlueprintPool("528.arty")
                    .save(v, "assembler/himars"), "528")
                .save();

        // ass.himarssmall
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_standard", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_plastic"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 48)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 48)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 6)
                .save(writer, "assembler/himarssmall");

        // ass.himarssmallhe
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_he", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_plastic"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 48)
                .addIngredient(ore("oredict/ingot/any_plasticexplosive"), 18)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 48)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 6)
                .save(writer, "assembler/himarssmallhe");

        // ass.himarssmallwp
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_wp", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_plastic"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 48)
                .addIngredient(ore("oredict/ingot/white_phosphorus"), 18)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 48)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 6)
                .save(writer, "assembler/himarssmallwp");

        // ass.himarssmalltb
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_thermobaric", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_plastic"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 48)
                .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 32)
                .addIngredient(container(ModFluids.KEROSENE_REFORM.getSource(), 1000), 12)
                .addIngredient(container(ModFluids.PEROXIDE.getSource(), 1000), 12)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 6)
                .save(writer, "assembler/himarssmalltb");

        // ass.himarssmallnuke
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_mini_nuke", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_plastic"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 48)
                .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 6)
                .addIngredient(ore("oredict/nugget/plutonium239"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 12)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 6)
                .save(writer, "assembler/himarssmallnuke");

        // ass.himarssmalllava
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_lava", 1), 100, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 4)
                .addIngredient(ore("oredict/gem/volcanic"), 1)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 6)
                .save(writer, "assembler/himarssmalllava");

        // ass.himarslarge
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_single", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 36)
                .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .save(writer, "assembler/himarslarge");

        // ass.himarslargetb
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:rocket_himars_single_tb", 1), 200, 100)
                .addIngredient(ore("oredict/plate/steel"), 24)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 36)
                .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 24)
                .addIngredient(container(ModFluids.KEROSENE_REFORM.getSource(), 1000), 16)
                .addIngredient(container(ModFluids.PEROXIDE.getSource(), 1000), 16)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .save(writer, "assembler/himarslargetb");

        // ass.missileassembly
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:missile_assembly", 1), 200, 100)
                .addIngredient(ore("oredict/shell/aluminum"), 2)
                .addIngredient(ore("oredict/shell/titanium"), 4)
                .addIngredient(ore("oredict/ingot/any_plastic"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                .save(writer, "assembler/missileassembly");

        // ass.warheadhe1
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_generic_small", 1), 100, 100)
                .addIngredient(ore("oredict/plate/titanium"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:ball_dynamite")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:microchip")), 1)
                .save(writer, "assembler/warheadhe1");

        // ass.warheadhe2
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_generic_medium", 1), 200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 8)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                .save(writer, "assembler/warheadhe2");

        // ass.warheadhe3
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_generic_large", 1), 400, 100)
                .addIngredient(ore("oredict/plate/titanium"), 16)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 8)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 1)
                .save(writer, "assembler/warheadhe3");

        // ass.warheadinc1
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_incendiary_small", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_small")), 1)
                .addIngredient(ore("oredict/dust/red_phosphorus"), 2)
                .save(writer, "assembler/warheadinc1");

        // ass.warheadinc2
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_incendiary_medium", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_medium")), 1)
                .addIngredient(ore("oredict/dust/red_phosphorus"), 4)
                .save(writer, "assembler/warheadinc2");

        // ass.warheadinc3
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_incendiary_large", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_large")), 1)
                .addIngredient(ore("oredict/dust/red_phosphorus"), 8)
                .save(writer, "assembler/warheadinc3");

        // ass.warheadcl1
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_cluster_small", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_small")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:pellet_cluster")), 2)
                .save(writer, "assembler/warheadcl1");

        // ass.warheadcl2
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_cluster_medium", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_medium")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:pellet_cluster")), 4)
                .save(writer, "assembler/warheadcl2");

        // ass.warheadcl3
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_cluster_large", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_large")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:pellet_cluster")), 8)
                .save(writer, "assembler/warheadcl3");

        // ass.warheadbb1
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_buster_small", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_small")), 1)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 2)
                .save(writer, "assembler/warheadbb1");

        // ass.warheadbb2
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_buster_medium", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_medium")), 1)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 4)
                .save(writer, "assembler/warheadbb2");

        // ass.warheadbb3
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_buster_large", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:warhead_generic_large")), 1)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 8)
                .save(writer, "assembler/warheadbb3");

        // ass.warheadnuke [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_nuclear", 1), 400, 100)
                    .addIngredient(ore("oredict/plate_triple/titanium"), 12)
                    .addIngredient(ore("oredict/plate_triple/lead"), 6)
                    .addIngredient(ore("oredict/billet/uranium235"), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:cordite")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .save(w, "assembler/warheadnuke"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_nuclear", 1), 400, 100)
                    .addIngredient(ore("oredict/plate_triple/titanium"), 12)
                    .addIngredient(ore("oredict/plate_triple/lead"), 6)
                    .addIngredient(ore("oredict/billet/uranium235"), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:cordite")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/warheadnuke"), "528")
                .save();

        // ass.warheadthermonuke [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_mirv", 1), 600, 100)
                    .addIngredient(ore("oredict/plate_triple/titanium"), 12)
                    .addIngredient(ore("oredict/plate_triple/lead"), 6)
                    .addIngredient(ore("oredict/billet/plutonium239"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 2)
                    .addFluidInput(ModFluids.DEUTERIUM.getSource(), 4000)
                    .save(w, "assembler/warheadthermonuke"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_mirv", 1), 600, 100)
                    .addIngredient(ore("oredict/plate_triple/titanium"), 12)
                    .addIngredient(ore("oredict/plate_triple/lead"), 6)
                    .addIngredient(ore("oredict/billet/plutonium239"), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 2)
                    .addFluidInput(ModFluids.DEUTERIUM.getSource(), 4000)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/warheadthermonuke"), "528")
                .save();

        // ass.warheadvolcano
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:warhead_volcano", 1), 600, 100)
                .addIngredient(ore("oredict/plate_triple/titanium"), 12)
                .addIngredient(ore("oredict/plate_triple/steel"), 6)
                .addIngredient(Ingredient.of(item("hbm_m:det_nuke")), 3)
                .addIngredient(ore("oredict/block/uranium238"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:capacitor_board")), 5)
                .save(writer, "assembler/warheadvolcano");
    }

    private static void part9(Consumer<FinishedRecipe> writer) {
        // ass.thrusternerva
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:thruster_nuclear", 1), 600, 100)
                .addIngredient(ore("oredict/ingot/dura_steel"), 32)
                .addIngredient(ore("oredict/ingot/boron"), 8)
                .addIngredient(ore("oredict/plate/lead"), 16)
                .addIngredient(ore("oredict/ntmpipe/steel"), 4)
                .save(writer, "assembler/thrusternerva");

        // ass.stealthmissile
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:missile_stealth", 1), 1200, 100)
                .addIngredient(ore("oredict/plate/titanium"), 20)
                .addIngredient(ore("oredict/plate/aluminum"), 20)
                .addIngredient(ore("oredict/dye_black"), 16)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 4)
                .addIngredient(ore("oredict/bolt/steel"), 32)
                .save(writer, "assembler/stealthmissile");

        // ass.shuttlemissile
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:missile_shuttle", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:missile_generic")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:missile_strong")), 1)
                .addIngredient(ore("oredict/dye_orange"), 5)
                .addIngredient(filled("hbm_m:canister_full", ModFluids.GASOLINE_LEADED.getSource()), 24)
                .addIngredient(ore("oredict/ingot/fiberglass"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 3)
                .addIngredient(ore("oredict/ingot/any_plasticexplosive"), 8)
                .addIngredient(ore("oredict/pane_glass"), 6)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .save(writer, "assembler/shuttlemissile");

        // ass.launchpad
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:launch_pad_large", 1), 200, 100)
                .addIngredient(ore("oredict/plate_triple/steel"), 6)
                .addIngredient(ore("oredict/any/concrete"), 64)
                .addIngredient(ore("oredict/ingot/any_plastic"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:steel_scaffold")), 24)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 2)
                .save(writer, "assembler/launchpad");

        // ass.launchpadsilo
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:launch_pad", 1), 200, 100)
                .addIngredient(ore("oredict/plate_sextuple/steel"), 8)
                .addIngredient(ore("oredict/any/concrete"), 8)
                .addIngredient(ore("oredict/ingot/any_hard_plastic"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 4)
                .save(writer, "assembler/launchpadsilo");

        // ass.mpt10kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_10_kerosene", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 1)
                .addIngredient(ore("oredict/ingot/tungsten"), 4)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .save(writer, "assembler/mpt10kero");

        // ass.mpt10solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_10_solid", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 1)
                .addIngredient(ore("oredict/ingot/tungsten"), 4)
                .addIngredient(ore("oredict/plate/steel"), 4)
                .save(writer, "assembler/mpt10solid");

        // ass.mpt10xenon
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_10_xenon", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/plate/dura_steel"), 4)
                .addIngredient(Ingredient.of(item("hbm_m:arc_electrode")), 1)
                .save(writer, "assembler/mpt10xenon");

        // ass.mpt15kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_kerosene", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpt15kero");

        // ass.mpt15kerodual
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_kerosene_dual", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpt15kerodual");

        // ass.mpt15kerotriple
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_kerosene_triple", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpt15kerotriple");

        // ass.mpt15solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_solid", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpt15solid");

        // ass.mpt15solid16
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_solid_hexdecuple", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpt15solid16");

        // ass.mpt15hydro
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_hydrogen", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 4)
                .save(writer, "assembler/mpt15hydro");

        // ass.mpt15hydrodual
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_hydrogen_dual", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 3)
                .addIngredient(ore("oredict/ingot/tungsten"), 8)
                .addIngredient(ore("oredict/ingot/workers_alloy"), 4)
                .save(writer, "assembler/mpt15hydrodual");

        // ass.mpt15bfshort
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_balefire_short", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 5)
                .addIngredient(ore("oredict/plate_triple/tungsten"), 8)
                .addIngredient(ore("oredict/plate/saturnite"), 8)
                .save(writer, "assembler/mpt15bfshort");

        // ass.mpt15bf
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_balefire_short", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 5)
                .addIngredient(ore("oredict/plate_triple/tungsten"), 16)
                .addIngredient(ore("oredict/plate/saturnite"), 16)
                .save(writer, "assembler/mpt15bf");

        // ass.mpt15bflarge
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_15_balefire_large", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 10)
                .addIngredient(ore("oredict/plate_triple/tungsten"), 16)
                .addIngredient(ore("oredict/plate/saturnite"), 24)
                .save(writer, "assembler/mpt15bflarge");

        // ass.mpt20kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_20_kerosene", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 6)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpt20kero");

        // ass.mpt20kerodual
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_20_kerosene_dual", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 6)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpt20kerodual");

        // ass.mpt20kerotriple
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_20_kerosene_triple", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/ntmpipe/steel"), 6)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpt20kerotriple");

        // ass.mpt20solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_20_solid", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 6)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpt20solid");

        // ass.mpt20solidmulti
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_20_solid_multi", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 6)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpt20solidmulti");

        // ass.mpt20solidmultier
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_thruster_20_solid_multier", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/ntmpipe/dura_steel"), 6)
                .addIngredient(ore("oredict/ingot/tungsten"), 16)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpt20solidmultier");

        // ass.mpf10kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_kerosene", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 2)
                .addIngredient(ore("oredict/plate/aluminum"), 12)
                .addIngredient(ore("oredict/plate/steel"), 3)
                .save(writer, "assembler/mpf10kero");

        // ass.mpf10kerolong
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_kerosene", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 2)
                .addIngredient(ore("oredict/plate/aluminum"), 16)
                .addIngredient(ore("oredict/plate/steel"), 6)
                .save(writer, "assembler/mpf10kerolong");

        // ass.mpf10solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_solid", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 2)
                .addIngredient(ore("oredict/plate/titanium"), 12)
                .addIngredient(ore("oredict/plate/steel"), 3)
                .save(writer, "assembler/mpf10solid");

        // ass.mpf10solidlong
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_solid", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 2)
                .addIngredient(ore("oredict/plate/titanium"), 16)
                .addIngredient(ore("oredict/plate/steel"), 6)
                .save(writer, "assembler/mpf10solidlong");

        // ass.mpf10xenon
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_xenon", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 2)
                .addIngredient(ore("oredict/plate/copper"), 12)
                .addIngredient(ore("oredict/plate/steel"), 3)
                .save(writer, "assembler/mpf10xenon");

        // ass.mpf1015kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_15_kerosene", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/plate/aluminum"), 24)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpf1015kero");
    }

    private static void part10(Consumer<FinishedRecipe> writer) {
        // ass.mpf1015solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_15_solid", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/plate/titanium"), 24)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpf1015solid");

        // ass.mpf1015hydro
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_15_hydrogen", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/plate/copper"), 24)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpf1015hydro");

        // ass.mpf1015bf
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_10_15_balefire", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/plate/saturnite"), 24)
                .addIngredient(ore("oredict/plate/steel"), 8)
                .save(writer, "assembler/mpf1015bf");

        // ass.mpf15kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_15_kerosene", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 2)
                .addIngredient(ore("oredict/plate/aluminum"), 32)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .save(writer, "assembler/mpf15kero");

        // ass.mpf15solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_15_solid", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 2)
                .addIngredient(ore("oredict/plate/titanium"), 32)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .save(writer, "assembler/mpf15solid");

        // ass.mpf15hydro
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_15_hydrogen", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 2)
                .addIngredient(ore("oredict/plate/copper"), 32)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .save(writer, "assembler/mpf15hydro");

        // ass.mpf1520kero
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_15_20_kerosene", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/plate/aluminum"), 48)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpf1520kero");

        // ass.mpf1520solid
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_fuselage_15_20_solid", 1), 400, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:seg_20")), 1)
                .addIngredient(ore("oredict/plate/titanium"), 48)
                .addIngredient(ore("oredict/plate/steel"), 16)
                .save(writer, "assembler/mpf1520solid");

        // ass.mpw10he
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_he", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/plate/steel"), 6)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 3)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                .save(writer, "assembler/mpw10he");

        // ass.mpw10inc
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_incendiary", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/plate/steel"), 6)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 2)
                .addIngredient(ore("oredict/dust/red_phosphorus"), 6)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 1)
                .save(writer, "assembler/mpw10inc");

        // ass.mpw10bus
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_buster", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/plate/weapon_steel"), 6)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 6)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 2)
                .save(writer, "assembler/mpw10bus");

        // ass.mpw10nukesmall [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_nuclear", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 16)
                    .addIngredient(ore("oredict/billet/plutonium239"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 4)
                    .addIngredient(ore("oredict/ingot/any_highexplosive"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .save(w, "assembler/mpw10nukesmall"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_nuclear", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 16)
                    .addIngredient(ore("oredict/billet/plutonium239"), 2)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 4)
                    .addIngredient(ore("oredict/ingot/any_highexplosive"), 4)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/mpw10nukesmall"), "528")
                .save();

        // ass.mpw10nukelarge [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_nuclear_large", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 16)
                    .addIngredient(ore("oredict/billet/plutonium239"), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 8)
                    .addIngredient(ore("oredict/ingot/any_highexplosive"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .save(w, "assembler/mpw10nukelarge"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_nuclear_large", 1), 200, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 16)
                    .addIngredient(ore("oredict/billet/plutonium239"), 6)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 8)
                    .addIngredient(ore("oredict/ingot/any_highexplosive"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/mpw10nukelarge"), "528")
                .save();

        // ass.mpw10taint
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_taint", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:det_cord")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:magic_powder")), 12)
                .addIngredient(container(ModFluids.WATZ.getSource(), 1000), 1)
                .save(writer, "assembler/mpw10taint");

        // ass.mpw10cloud
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_10_cloud", 1), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_10")), 1)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:det_cord")), 2)
                .addIngredient(Ingredient.of(item("hbm_m:magic_powder")), 16)
                .save(writer, "assembler/mpw10cloud");

        // ass.mpw15he
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_he", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 12)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 3)
                .save(writer, "assembler/mpw15he");

        // ass.mpw15inc
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_incendiary", 1), 200, 100)
                .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                .addIngredient(ore("oredict/plate/steel"), 12)
                .addIngredient(ore("oredict/ingot/any_highexplosive"), 8)
                .addIngredient(ore("oredict/dust/red_phosphorus"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 3)
                .save(writer, "assembler/mpw15inc");

        // ass.mpw15nuke [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_nuclear", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .addIngredient(ore("oredict/billet/plutonium239"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                    .save(w, "assembler/mpw15nuke"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_nuclear", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .addIngredient(ore("oredict/billet/plutonium239"), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 12)
                    .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 24)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/mpw15nuke"), "528")
                .save();

        // ass.mpw15n2 [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_n2", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 8)
                    .save(w, "assembler/mpw15n2"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_n2", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:ball_tatb")), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:advanced_circuit")), 8)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/mpw15n2"), "528")
                .save();

        // ass.mpw15bf [Fassungen: 528]
        ConfigRecipes.variants(writer)
                .base(w -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_balefire", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magic_powder")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:egg_balefire_shard")), 4)
                    .addIngredient(ore("oredict/ingot/any_highexplosive"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 2)
                    .save(w, "assembler/mpw15bf"))
                .variant(v -> AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:mp_warhead_15_balefire", 1), 400, 100)
                    .addIngredient(Ingredient.of(item("hbm_m:seg_15")), 1)
                    .addIngredient(ore("oredict/plate/weapon_steel"), 32)
                    .addIngredient(Ingredient.of(item("hbm_m:neutron_reflector")), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:magic_powder")), 8)
                    .addIngredient(Ingredient.of(item("hbm_m:egg_balefire_shard")), 4)
                    .addIngredient(ore("oredict/ingot/any_highexplosive"), 16)
                    .addIngredient(Ingredient.of(item("hbm_m:controller")), 2)
                    .withBlueprintPool("528.controller")
                    .save(v, "assembler/mpw15bf"), "528")
                .save();

        // ass.50bmgsm
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:ammo_standard_bmg50_sm", 6), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:casing_large_steel")), 1)
                .addIngredient(ore("oredict/dust/any_smokeless"), 6)
                .addIngredient(ore("oredict/ingot/starmetal"), 3)
                .withBlueprintPool("discover.silverstorm")
                .save(writer, "assembler/50bmgsm");

        // ass.50bmgbypass
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:ammo_secret_bmg50_black", 12), 100, 100)
                .addIngredient(Ingredient.of(item("hbm_m:casing_large_steel")), 2)
                .addIngredient(ore("oredict/dust/any_smokeless"), 24)
                .addIngredient(Ingredient.of(item("hbm_m:item_secret_selenium_steel")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:black_diamond")), 1)
                .withBlueprintPool("secret.psalm")
                .save(writer, "assembler/50bmgbypass");

        // chem.shellchlorine
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:ammo_arty_chlorine", 1), 100, 1000)
                .addIngredient(Ingredient.of(item("hbm_m:ammo_arty")), 1)
                .addIngredient(ore("oredict/ingot/any_plastic"), 1)
                .addFluidInput(ModFluids.CHLORINE.getSource(), 4000)
                .save(writer, "assembler/shellchlorine");

        // ass.shellphosgene
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:ammo_arty_phosgene", 1), 100, 1000)
                .addIngredient(Ingredient.of(item("hbm_m:ammo_arty")), 1)
                .addIngredient(ore("oredict/ingot/any_plastic"), 1)
                .addFluidInput(ModFluids.PHOSGENE.getSource(), 4000)
                .save(writer, "assembler/shellphosgene");

        // ass.shellmustard
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:ammo_arty_mustard_gas", 1), 100, 1000)
                .addIngredient(Ingredient.of(item("hbm_m:ammo_arty")), 1)
                .addIngredient(ore("oredict/ingot/any_plastic"), 1)
                .addFluidInput(ModFluids.MUSTARDGAS.getSource(), 4000)
                .save(writer, "assembler/shellmustard");

        // ass.soyuzcore
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:struct_soyuz_core", 1), 1200, 25000)
                .addIngredient(ore("oredict/plate_sextuple/steel"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:upgrade_power_3")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:battery_pack_battery_lithium")), 1)
                .withBlueprintPool("discover.soyuz")
                .save(writer, "assembler/soyuzcore");

        // ass.soyuz
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:missile_soyuz", 1), 6000, 25000)
                .addIngredient(ore("oredict/shell/titanium"), 32)
                .addIngredient(ore("oredict/ingot/rubber"), 64)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 64)
                .addIngredient(Ingredient.of(item("hbm_m:thruster_small")), 12)
                .addIngredient(Ingredient.of(item("hbm_m:thruster_medium")), 12)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 32)
                .withBlueprintPool("discover.soyuz")
                .save(writer, "assembler/soyuz");

        // ass.lander
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:missile_soyuz_lander", 1), 2400, 25000)
                .addIngredient(ore("oredict/shell/aluminum"), 4)
                .addIngredient(ore("oredict/ingot/rubber"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:rocket_fuel")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:thruster_small")), 3)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 3)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 12)
                .withBlueprintPool("discover.soyuz")
                .save(writer, "assembler/lander");

        // ass.spysat
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_spy", 1), 1200, 25000)
                .addIngredient(ore("oredict/shell/aluminum"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:integrated_circuit")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_glass_polarized")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                .save(writer, "assembler/spysat");

        // ass.scansat
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_scanner", 1), 1200, 25000)
                .addIngredient(ore("oredict/shell/aluminum"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 32)
                .addIngredient(ore("oredict/wire_dense/gold"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                .save(writer, "assembler/scansat");
    }

    private static void part11(Consumer<FinishedRecipe> writer) {
        // ass.radarsat
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_radar", 1), 1200, 25000)
                .addIngredient(ore("oredict/plate_triple/gold"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 3)
                .save(writer, "assembler/radarsat");

        // ass.astrominer
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_miner_astro", 1), 1200, 25000)
                .addIngredient(ore("oredict/plate_triple/saturnite"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:thruster_medium")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:motor_bismuth")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 2)
                .save(writer, "assembler/astrominer");

        // ass.lunarminer
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_miner_lunar", 1), 1200, 25000)
                .addIngredient(ore("oredict/plate_triple/saturnite"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 8)
                .addIngredient(Ingredient.of(item("hbm_m:thruster_medium")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:motor_bismuth")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 2)
                .save(writer, "assembler/lunarminer");

        // ass.orbitallaser
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_precision_laser", 1), 1200, 25000)
                .addIngredient(ore("oredict/shell/weapon_steel"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_tantalum")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:crystal_redstone")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 3)
                .save(writer, "assembler/orbitallaser");

        // ass.deathray
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_death_ray", 1), 1200, 25000)
                .addIngredient(ore("oredict/shell/weapon_steel"), 32)
                .addIngredient(ore("oredict/billet/plutonium_rg"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:battery_pack_capacitor_bismuth")), 1)
                .addIngredient(ore("oredict/gem/emerald"), 32)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 5)
                .save(writer, "assembler/deathray");

        // ass.xenrelay
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_xenium_resonator", 1), 1200, 25000)
                .addIngredient(ore("oredict/plate_triple/gold"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 32)
                .addIngredient(Ingredient.of(item("hbm_m:crystal_xen")), 1)
                .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 24)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 4)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 1)
                .save(writer, "assembler/xenrelay");

        // ass.detectorsat
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_detector", 1), 1200, 25000)
                .addIngredient(ore("oredict/plate_triple/gold"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 64)
                .addIngredient(ore("oredict/wire_dense/bscco"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:bismoid_circuit")), 24)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 1)
                .save(writer, "assembler/detectorsat");

        // ass.rayscansat
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:satellite_ray_scan", 1), 1200, 25000)
                .addIngredient(ore("oredict/shell/saturnite"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:photo_panel")), 32)
                .addIngredient(ore("oredict/wire_dense/schrabidate"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:quantum_circuit")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:part_generic_lde")), 16)
                .addIngredient(Ingredient.of(item("hbm_m:controller_advanced")), 3)
                .save(writer, "assembler/rayscansat");

        // ass.satlink
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:machine_satlink", 1), 100, 1000)
                .addIngredient(Ingredient.of(item("hbm_m:steel_scaffold")), 16)
                .addIngredient(ore("oredict/plate/aluminum"), 16)
                .addIngredient(Ingredient.of(item("hbm_m:magnetron")), 3)
                .addIngredient(Ingredient.of(item("hbm_m:controller")), 1)
                .save(writer, "assembler/satlink");

        // OFFEN ass.nitra: Zufallsausgabe (ChanceOutputMulti)

        // ass.emptypackage
        AssemblerRecipeBuilder.assemblerRecipe(stack("hbm_m:fluid_pack_empty", 1), 40, 100)
                .addIngredient(ore("oredict/plate/titanium"), 4)
                .addIngredient(ore("oredict/ingot/any_plastic"), 2)
                .save(writer, "assembler/emptypackage");
    }
}
//?}
