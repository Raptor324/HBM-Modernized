package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hbm_m.datagen.recipes.custom.CraftingGen.p;
import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * Werkbank-Rezepte des Originals ({@code CraftingManager} + {@code crafting/*Recipes}), die im Port fehlten oder
 * abwichen - 1:1 uebersetzt. AUTOMATISCH ERZEUGT durch {@code rc/craftgen.py}; die dadurch ersetzten
 * Port-Rezepte stehen in {@link SupersededRecipes}. OreDict-Zutaten sind die Tags {@code hbm_m:oredict/...}.
 */
public final class OrigCraftingRecipeGenerator {

    private OrigCraftingRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        CraftingGen g = new CraftingGen(writer, "orig");
        part0(g);
        part1(g);
        part2(g);
        part3(g);
        part4(g);
        part5(g);
        part6(g);
        part7(g);
        part8(g);
        part9(g);
        part10(g);
        part11(g);
        part12(g);
        part13(g);
        part14(g);
        part15(g);
        part16(g);
        checkMissing("OrigCraftingRecipeGenerator");
    }

    private static void part0(CraftingGen g) {
        // CraftingManager.java:100 (FEHLT)
        g.shaped(item("hbm_m:plate_cast_iron"), 1, p("BPB", "BPB", "BPB"), 'B', ore("oredict/bolt/steel"), 'P', ore("oredict/plate/iron"));
        // CraftingManager.java:101 (FEHLT)
        g.shaped(item("hbm_m:hazmat_cloth_red"), 1, p("C", "R", "C"), 'C', Ingredient.of(item("hbm_m:hazmat_cloth")), 'R', ore("oredict/dust/redstone"));
        // CraftingManager.java:102 (FEHLT)
        g.shaped(item("hbm_m:hazmat_cloth_grey"), 1, p(" P ", "ICI", " L "), 'C', Ingredient.of(item("hbm_m:hazmat_cloth_red")), 'P', ore("oredict/plate/iron"), 'L', ore("oredict/plate/lead"), 'I', ore("oredict/ingot/any_rubber"));
        // CraftingManager.java:103 (FEHLT)
        g.shaped(item("hbm_m:asbestos_cloth"), 8, p("SCS", "CPC", "SCS"), 'S', Ingredient.of(item("minecraft:string")), 'P', ore("oredict/dust/bromine"), 'C', Ingredient.of(item("minecraft:white_wool")));
        // CraftingManager.java:104 (FEHLT)
        g.shaped(item("hbm_m:bolt_spike"), 2, p("BB", "B ", "B "), 'B', ore("oredict/bolt/steel"));
        // CraftingManager.java:105 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 8, p("DD"), 'D', ore("oredict/ingot/any_plastic"));
        // CraftingManager.java:106 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 8, p("DD"), 'D', ore("oredict/ingot/any_rubber"));
        // CraftingManager.java:107 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 16, p("DD"), 'D', ore("oredict/ingot/fiberglass"));
        // CraftingManager.java:108 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 16, p("DD"), 'D', ore("oredict/ingot/asbestos"));
        // CraftingManager.java:109 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 4, p("SWS"), 'S', Ingredient.of(item("minecraft:string")), 'W', Ingredient.of(item("minecraft:white_wool")));
        // CraftingManager.java:110 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 4, p("BB"), 'B', ore("oredict/ingot/brick"));
        // CraftingManager.java:111 (FEHLT)
        g.shaped(item("hbm_m:plate_polymer"), 4, p("BB"), 'B', ore("oredict/ingot/nether_brick"));
        // MineralRecipes.java:348 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:plate_polymer"), 9, Ingredient.of(item("hbm_m:block_insulator")));
        // CraftingManager.java:113 (ABW)
        g.shaped(item("hbm_m:vacuum_tube"), 1, p("G", "W", "I"), 'G', ore("oredict/pane_glass"), 'W', ore("oredict/wire_fine/tungsten"), 'I', Ingredient.of(item("hbm_m:plate_polymer")));
        // CraftingManager.java:114 (ABW)
        g.shaped(item("hbm_m:vacuum_tube"), 1, p("G", "W", "I"), 'G', ore("oredict/pane_glass"), 'W', ore("oredict/wire_fine/carbon"), 'I', Ingredient.of(item("hbm_m:plate_polymer")));
        // CraftingManager.java:115 (FEHLT)
        g.shaped(item("hbm_m:circuit_numitron"), 3, p("G", "W", "I"), 'G', ore("oredict/pane_glass"), 'W', Ingredient.of(item("hbm_m:coil_tungsten")), 'I', ore("oredict/plate/copper"));
        // CraftingManager.java:116 (ABW)
        g.shaped(item("hbm_m:capacitor"), 1, p("I", "N", "W"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'N', ore("oredict/nugget/niobium"), 'W', ore("oredict/wire_fine/aluminum"));
        // CraftingManager.java:117 (ABW)
        g.shaped(item("hbm_m:capacitor"), 1, p("I", "N", "W"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'N', ore("oredict/nugget/niobium"), 'W', ore("oredict/wire_fine/copper"));
        // CraftingManager.java:118 (ABW)
        g.shaped(item("hbm_m:capacitor"), 2, p("IAI", "W W"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'A', ore("oredict/dust/aluminum"), 'W', ore("oredict/wire_fine/aluminum"));
        // CraftingManager.java:119 (ABW)
        g.shaped(item("hbm_m:capacitor"), 2, p("IAI", "W W"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'A', ore("oredict/dust/aluminum"), 'W', ore("oredict/wire_fine/copper"));
        // CraftingManager.java:120 (ABW)
        g.shaped(item("hbm_m:capacitor_tantalum"), 1, p("I", "N", "W"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'N', ore("oredict/nugget/tantalum"), 'W', ore("oredict/wire_fine/aluminum"));
        // CraftingManager.java:121 (ABW)
        g.shaped(item("hbm_m:capacitor_tantalum"), 1, p("I", "N", "W"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'N', ore("oredict/nugget/tantalum"), 'W', ore("oredict/wire_fine/copper"));
        // CraftingManager.java:122 (ABW)
        g.shaped(item("hbm_m:pcb"), 1, p("I", "P"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'P', ore("oredict/plate/copper"));
        // CraftingManager.java:123 (ABW)
        g.shaped(item("hbm_m:pcb"), 4, p("I", "P"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'P', ore("oredict/plate/gold"));
        // CraftingManager.java:124 (ABW)
        g.shaped(item("hbm_m:controller_chassis"), 1, p("PPP", "CBB", "PPP"), 'P', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:crt_display")), 'B', Ingredient.of(item("hbm_m:pcb")));
        // CraftingManager.java:128 (FEHLT)
        g.shaped(item("hbm_m:cell_empty"), 6, p(" S ", "G G", " S "), 'S', ore("oredict/plate/steel"), 'G', ore("oredict/pane_glass"));
        // CraftingManager.java:129 (FEHLT)
        g.shaped(item("hbm_m:cell_deuterium"), 8, p("DDD", "DTD", "DDD"), 'D', Ingredient.of(item("hbm_m:cell_empty")), 'T', Ingredient.of(item("hbm_m:mike_deut")));
        // CraftingManager.java:130 (FEHLT)
        g.shaped(item("hbm_m:particle_empty"), 2, p("STS", "G G", "STS"), 'S', ore("oredict/plate_triple/lead"), 'T', Ingredient.of(item("hbm_m:coil_gold")), 'G', ore("oredict/pane_glass"));
        // CraftingManager.java:131 (FEHLT)
        g.shapeless(item("hbm_m:particle_copper"), 1, Ingredient.of(item("hbm_m:particle_empty")), ore("oredict/dust/copper"), Ingredient.of(item("hbm_m:pellet_charged")));
        // CraftingManager.java:132 (FEHLT)
        g.shapeless(item("hbm_m:particle_lead"), 1, Ingredient.of(item("hbm_m:particle_empty")), ore("oredict/dust/lead"), Ingredient.of(item("hbm_m:pellet_charged")));
        // CraftingManager.java:134 (ABW)
        g.shaped(item("hbm_m:canister_empty"), 2, p("S ", "AA", "AA"), 'S', ore("oredict/plate/steel"), 'A', ore("oredict/plate/aluminum"));
        // CraftingManager.java:136 (FEHLT)
        g.shapeless(item("hbm_m:block_waste_painted"), 1, ore("oredict/dye_yellow"), Ingredient.of(item("hbm_m:block_waste")));
        // CraftingManager.java:138 (FEHLT)
        g.shaped(item("hbm_m:ingot_aluminium"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/aluminum"));
        // MineralRecipes.java:39 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:ingot_aluminium"), 9, Ingredient.of(item("hbm_m:block_aluminium")));
        // CraftingManager.java:139 (FEHLT)
        g.shaped(item("minecraft:copper_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/copper"));
        // MineralRecipes.java:306 (FEHLT, 1x1)
        g.shapeless(item("minecraft:copper_ingot"), 9, Ingredient.of(item("hbm_m:block_copper")));
        // CraftingManager.java:140 (ABW)
        g.shaped(item("hbm_m:tungsten_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/tungsten"));
        // CraftingManager.java:141 (ABW)
        g.shaped(item("hbm_m:red_copper_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/mingrade"));
        // CraftingManager.java:142 (FEHLT)
        g.shaped(item("minecraft:gold_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/gold"));
        // CraftingManager.java:143 (ABW)
        g.shaped(item("hbm_m:schrabidium_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/schrabidium"));
        // MineralRecipes.java:136 (ABW)
        g.shapeless(item("hbm_m:schrabidium_ingot"), 2, Ingredient.of(item("hbm_m:billet_schrabidium")), Ingredient.of(item("hbm_m:billet_schrabidium")), Ingredient.of(item("hbm_m:billet_schrabidium")));
        // MineralRecipes.java:379 (ABW)
        g.shaped(item("hbm_m:schrabidium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_schrabidium")));
        // CraftingManager.java:144 (FEHLT)
        g.shaped(item("hbm_m:magnetized_tungsten_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/magnetized_tungsten"));
        // MineralRecipes.java:323 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:magnetized_tungsten_ingot"), 9, Ingredient.of(item("hbm_m:block_magnetized_tungsten")));
        // CraftingManager.java:145 (FEHLT)
        g.shaped(item("hbm_m:graphite_ingot"), 1, p("###", "###", "###"), '#', ore("oredict/wire_fine/carbon"));
        // MineralRecipes.java:40 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:graphite_ingot"), 9, Ingredient.of(item("hbm_m:block_graphite")));
        // CraftingManager.java:147 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")));
        // CraftingManager.java:148 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:apple")), Ingredient.of(item("minecraft:apple")), Ingredient.of(item("minecraft:apple")));
        // CraftingManager.java:149 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:sugar_cane")), Ingredient.of(item("minecraft:sugar_cane")), Ingredient.of(item("minecraft:sugar_cane")));
        // CraftingManager.java:150 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:rotten_flesh")), Ingredient.of(item("minecraft:rotten_flesh")), Ingredient.of(item("minecraft:rotten_flesh")));
        // CraftingManager.java:151 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:carrot")), Ingredient.of(item("minecraft:carrot")), Ingredient.of(item("minecraft:carrot")), Ingredient.of(item("minecraft:carrot")), Ingredient.of(item("minecraft:carrot")), Ingredient.of(item("minecraft:carrot")));
        // CraftingManager.java:152 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:potato")), Ingredient.of(item("minecraft:potato")), Ingredient.of(item("minecraft:potato")), Ingredient.of(item("minecraft:potato")), Ingredient.of(item("minecraft:potato")), Ingredient.of(item("minecraft:potato")));
        // CraftingManager.java:153 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), ore("oredict/tree_sapling"), ore("oredict/tree_sapling"), ore("oredict/tree_sapling"), ore("oredict/tree_sapling"), ore("oredict/tree_sapling"), ore("oredict/tree_sapling"));
        // CraftingManager.java:154 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), ore("oredict/tree_leaves"), ore("oredict/tree_leaves"), ore("oredict/tree_leaves"), ore("oredict/tree_leaves"), ore("oredict/tree_leaves"), ore("oredict/tree_leaves"));
        // CraftingManager.java:155 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:pumpkin")));
        // CraftingManager.java:156 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:melon")));
        // CraftingManager.java:157 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:cactus")), Ingredient.of(item("minecraft:cactus")), Ingredient.of(item("minecraft:cactus")));
        // CraftingManager.java:158 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("hbm_m:sawdust_powder")), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:wheat")));
        // CraftingManager.java:159 (FEHLT)
        g.shapeless(item("hbm_m:biomass"), 4, Ingredient.of(item("hbm_m:plant_flower_weed")), Ingredient.of(item("hbm_m:plant_flower_weed")), Ingredient.of(item("hbm_m:plant_flower_weed")), Ingredient.of(item("hbm_m:plant_flower_weed")), Ingredient.of(item("hbm_m:plant_flower_weed")), Ingredient.of(item("hbm_m:plant_flower_weed")));
        // CraftingManager.java:171 (FEHLT)
        g.shaped(item("hbm_m:tank_steel"), 2, p("STS", "S S", "STS"), 'S', ore("oredict/plate/steel"), 'T', ore("oredict/plate/titanium"));
    }

    private static void part1(CraftingGen g) {
        // CraftingManager.java:174 (ABW)
        g.shaped(item("hbm_m:motor_desh"), 1, p("PCP", "DMD", "PCP"), 'P', ore("oredict/ingot/any_plastic"), 'C', ore("oredict/wire_dense/gold"), 'D', ore("oredict/ingot/workers_alloy"), 'M', Ingredient.of(item("hbm_m:motor")));
        // CraftingManager.java:175 (FEHLT)
        g.shaped(item("hbm_m:motor_bismuth"), 1, p("BCB", "SDS", "BCB"), 'B', ore("oredict/nugget/bismuth"), 'C', ore("oredict/wire_dense/neodymium"), 'S', ore("oredict/plate_triple/steel"), 'D', ore("oredict/ingot/dura_steel"));
        // CraftingManager.java:176 (FEHLT)
        g.shaped(item("hbm_m:deuterium_filter"), 1, p("TST", "SCS", "TST"), 'T', ore("oredict/ingot/any_resistant_alloy"), 'S', ore("oredict/dust/sulfur"), 'C', Ingredient.of(item("hbm_m:catalyst_clay")));
        // CraftingManager.java:178 (FEHLT)
        g.shaped(item("hbm_m:fins_flat"), 1, p("IP", "PP", "IP"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:179 (FEHLT)
        g.shaped(item("hbm_m:fins_small_steel"), 1, p(" PP", "PII", " PP"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:180 (FEHLT)
        g.shaped(item("hbm_m:fins_big_steel"), 1, p(" PI", "III", " PI"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:181 (FEHLT)
        g.shaped(item("hbm_m:fins_tri_steel"), 1, p(" PI", "IIB", " PI"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"), 'B', ore("oredict/block/steel"));
        // CraftingManager.java:182 (FEHLT)
        g.shaped(item("hbm_m:fins_quad_titanium"), 1, p(" PP", "III", " PP"), 'P', ore("oredict/plate/titanium"), 'I', ore("oredict/ingot/titanium"));
        // CraftingManager.java:183 (FEHLT)
        g.shaped(item("hbm_m:sphere_steel"), 1, p("PIP", "I I", "PIP"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:184 (FEHLT)
        g.shaped(item("hbm_m:pedestal_steel"), 1, p("P P", "P P", "III"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:185 (FEHLT)
        g.shaped(item("hbm_m:lemon"), 1, p(" D ", "DSD", " D "), 'D', ore("oredict/dye_yellow"), 'S', ore("oredict/stone"));
        // CraftingManager.java:186 (ABW)
        g.shaped(item("hbm_m:blade_titanium"), 2, p("TP", "TP", "TT"), 'P', ore("oredict/plate/titanium"), 'T', ore("oredict/ingot/titanium"));
        // CraftingManager.java:187 (FEHLT)
        g.shaped(item("hbm_m:turbine_titanium"), 1, p("BBB", "BSB", "BBB"), 'B', Ingredient.of(item("hbm_m:blade_titanium")), 'S', ore("oredict/ingot/steel"));
        // CraftingManager.java:188 (FEHLT)
        g.shaped(item("hbm_m:shimmer_head"), 1, p("SSS", "DTD", "SSS"), 'S', ore("oredict/ingot/steel"), 'D', ore("oredict/block/workers_alloy"), 'T', ore("oredict/block/tungsten"));
        // CraftingManager.java:189 (FEHLT)
        g.shaped(item("hbm_m:shimmer_axe_head"), 1, p("PII", "PBB", "PII"), 'P', ore("oredict/plate/steel"), 'B', ore("oredict/block/workers_alloy"), 'I', ore("oredict/ingot/tungsten"));
        // CraftingManager.java:190 (FEHLT)
        g.shaped(item("hbm_m:shimmer_handle"), 1, p("GP", "GP", "GP"), 'G', ore("oredict/plate/gold"), 'P', ore("oredict/ingot/any_plastic"));
        // CraftingManager.java:191 (FEHLT)
        g.shaped(item("hbm_m:shimmer_sledge"), 1, p("H", "G", "G"), 'G', Ingredient.of(item("hbm_m:shimmer_handle")), 'H', Ingredient.of(item("hbm_m:shimmer_head")));
        // CraftingManager.java:192 (FEHLT)
        g.shaped(item("hbm_m:shimmer_axe"), 1, p("H", "G", "G"), 'G', Ingredient.of(item("hbm_m:shimmer_handle")), 'H', Ingredient.of(item("hbm_m:shimmer_axe_head")));
        // CraftingManager.java:193 (FEHLT)
        g.shapeless(item("hbm_m:definitelyfood"), 4, ore("oredict/ingot/any_rubber"), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:rotten_flesh")), ore("oredict/tree_sapling"));
        // CraftingManager.java:194 (FEHLT)
        g.shapeless(item("hbm_m:definitelyfood"), 4, ore("oredict/ingot/any_rubber"), Ingredient.of(item("minecraft:wheat")), Ingredient.of(item("minecraft:rotten_flesh")), Ingredient.of(item("minecraft:wheat_seeds")), Ingredient.of(item("minecraft:wheat_seeds")), Ingredient.of(item("minecraft:wheat_seeds")));
        // CraftingManager.java:195 (FEHLT)
        g.shaped(item("hbm_m:turbine_tungsten"), 1, p("BBB", "BSB", "BBB"), 'B', Ingredient.of(item("hbm_m:blade_tungsten")), 'S', ore("oredict/ingot/dura_steel"));
        // CraftingManager.java:196 (FEHLT)
        g.shaped(item("hbm_m:ring_starmetal"), 1, p(" S ", "S S", " S "), 'S', ore("oredict/ingot/starmetal"));
        // CraftingManager.java:197 (FEHLT)
        g.shaped(item("hbm_m:flywheel_beryllium"), 1, p("IBI", "BTB", "IBI"), 'B', ore("oredict/block/beryllium"), 'I', ore("oredict/plate_triple/iron"), 'T', ore("oredict/ntmpipe/dura_steel"));
        // CraftingManager.java:199 (FEHLT)
        g.shapeless(item("hbm_m:powder_poison"), 1, Ingredient.of(item("hbm_m:plant_flower_nightshade")));
        // CraftingManager.java:507 (FEHLT)
        g.shapeless(item("hbm_m:powder_poison"), 4, Ingredient.of(item("minecraft:spider_eye")), ore("oredict/dust/redstone"), ore("oredict/gem/nether_quartz"));
        // CraftingManager.java:200 (ABW)
        g.shapeless(item("hbm_m:syringe_metal_stimpak"), 1, Ingredient.of(item("hbm_m:syringe_metal_empty")), Ingredient.of(item("minecraft:carrot")), Ingredient.of(item("hbm_m:plant_flower_foxglove")));
        // CraftingManager.java:201 (FEHLT)
        g.shapeless(item("hbm_m:pill_herbal"), 1, ore("oredict/dust/coal"), Ingredient.of(item("minecraft:poisonous_potato")), Ingredient.of(item("minecraft:nether_wart")), Ingredient.of(item("hbm_m:plant_flower_foxglove")));
        // CraftingManager.java:202 (FEHLT)
        g.shapeless(item("hbm_m:plant_item_rope"), 1, Ingredient.of(item("minecraft:string")), Ingredient.of(item("minecraft:string")), Ingredient.of(item("minecraft:string")));
        // CraftingManager.java:203 (FEHLT)
        g.shaped(item("hbm_m:plant_item_rope"), 4, p("W", "W", "W"), 'W', Ingredient.of(item("hbm_m:plant_flower_weed")));
        // CraftingManager.java:204 (FEHLT)
        g.shapeless(item("minecraft:string"), 3, Ingredient.of(item("hbm_m:plant_flower_weed")));
        // CraftingManager.java:205 (FEHLT)
        g.shaped(item("minecraft:paper"), 3, p("SSS"), 'S', Ingredient.of(item("hbm_m:sawdust_powder")));
        // CraftingManager.java:207 (FEHLT)
        g.shaped(item("hbm_m:wrench"), 1, p(" S ", " IS", "I  "), 'S', ore("oredict/ingot/steel"), 'I', ore("oredict/ingot/iron"));
        // CraftingManager.java:208 (FEHLT)
        g.shaped(item("hbm_m:wrench_flipped"), 1, p("S", "D", "W"), 'S', Ingredient.of(item("minecraft:iron_sword")), 'D', Ingredient.of(item("hbm_m:ducttape")), 'W', Ingredient.of(item("hbm_m:wrench")));
        // CraftingManager.java:209 (FEHLT)
        g.shaped(item("hbm_m:memespoon"), 1, p("CGC", "PSP", "IAI"), 'C', Ingredient.of(item("hbm_m:yellowcake_powder")), 'G', ore("oredict/block/thorium232"), 'P', Ingredient.of(item("hbm_m:photo_panel")), 'S', Ingredient.of(item("hbm_m:steel_shovel")), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'A', ore("oredict/ingot/australium"));
        // CraftingManager.java:212 (FEHLT)
        g.shaped(item("hbm_m:ducttape"), 4, p("F", "P", "S"), 'F', Ingredient.of(item("minecraft:string")), 'S', ore("oredict/slimeball"), 'P', Ingredient.of(item("minecraft:paper")));
        // CraftingManager.java:220 (ABW)
        g.shaped(item("hbm_m:radio_telex"), 2, p("SCR", "W#W", "WWW"), 'S', Ingredient.of(item("hbm_m:radio_torch_sender")), 'C', Ingredient.of(item("hbm_m:crt_display")), 'R', Ingredient.of(item("hbm_m:radio_torch_receiver")), 'W', ore("oredict/plank_wood"), '#', Ingredient.of(item("hbm_m:analog_circuit")));
        // CraftingManager.java:230 (FEHLT)
        g.shaped(item("hbm_m:electric_furnace"), 1, p("BBB", "WFW", "RRR"), 'B', ore("oredict/ingot/beryllium"), 'R', Ingredient.of(item("hbm_m:coil_tungsten")), 'W', ore("oredict/plate_triple/copper"), 'F', Ingredient.of(item("minecraft:furnace")));
        // CraftingManager.java:237 (ABW)
        g.shaped(item("hbm_m:cable_diode"), 1, p(" Q ", "CAC", " Q "), 'Q', ore("oredict/nugget/silicon"), 'C', Ingredient.of(item("hbm_m:red_cable")), 'A', ore("oredict/ingot/aluminum"));
        // CraftingManager.java:238 (FEHLT)
        g.shaped(item("hbm_m:detector"), 1, p("IRI", "CTC", "IRI"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'R', ore("oredict/dust/redstone"), 'C', ore("oredict/wire_fine/mingrade"), 'T', Ingredient.of(item("hbm_m:coil_tungsten")));
        // CraftingManager.java:242 (ABW)
        g.shapeless(item("hbm_m:red_cable_gauge"), 1, Ingredient.of(item("hbm_m:red_wire_coated")), ore("oredict/ingot/steel"), Ingredient.of(item("hbm_m:integrated_circuit")));
        // CraftingManager.java:244 (ABW)
        g.shaped(item("hbm_m:red_connector_super"), 2, p("CCC", "III", " S "), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'S', ore("oredict/ingot/any_resistant_alloy"));
        // CraftingManager.java:245 (ABW)
        g.shaped(item("hbm_m:red_pylon"), 4, p("CWC", "PWP", " S "), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'W', ore("oredict/plank_wood"), 'P', Ingredient.of(item("hbm_m:plate_polymer")), 'S', ore("oredict/cobblestone"));
        // CraftingManager.java:246 (ABW)
        g.shaped(item("hbm_m:red_pylon_steel"), 4, p("CWC", "PWP", " S "), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'W', ore("oredict/ntmpipe/steel"), 'P', Ingredient.of(item("hbm_m:plate_polymer")), 'S', ore("oredict/cobblestone"));
        // CraftingManager.java:247 (ABW)
        g.shaped(item("hbm_m:red_pylon_medium_wood"), 2, p("CCW", "IIW", "  S"), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'W', ore("oredict/plank_wood"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'S', ore("oredict/cobblestone"));
        // CraftingManager.java:249 (ABW)
        g.shaped(item("hbm_m:red_pylon_medium_steel"), 2, p("CCW", "IIW", "  S"), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'W', ore("oredict/ntmpipe/steel"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'S', ore("oredict/cobblestone"));
        // CraftingManager.java:252 (ABW)
        g.shaped(item("hbm_m:machine_converter_he_rf"), 1, p("RRR", "WWW", "III"), 'R', Ingredient.of(item("hbm_m:capacitor")), 'W', ore("oredict/dust/redstone"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:257 (ABW)
        g.shaped(item("hbm_m:machine_battery_socket"), 1, p("I I", "I I", "IRI"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'R', Ingredient.of(item("hbm_m:coil_copper")));
        // CraftingManager.java:260 (ABW)
        g.shaped(item("hbm_m:battery_pack_capacitor_copper"), 1, p("IRI", "PRP", "IRI"), 'I', ore("oredict/plate/steel"), 'R', ore("oredict/block/copper"), 'P', Ingredient.of(item("hbm_m:plate_polymer")));
        // CraftingManager.java:283 (FEHLT)
        g.shaped(item("hbm_m:machine_autocrafter"), 1, p("SCS", "MWM", "SCS"), 'S', ore("oredict/plate/steel"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'M', Ingredient.of(item("hbm_m:motor")), 'W', Ingredient.of(item("minecraft:crafting_table")));
        // CraftingManager.java:284 (FEHLT)
        g.shaped(item("hbm_m:machine_funnel"), 1, p("S S", "SRS", " S "), 'S', ore("oredict/ingot/steel"), 'R', ore("oredict/dust/redstone"));
        // CraftingManager.java:285 (FEHLT)
        g.shaped(item("minecraft:hopper"), 1, p("S S", "S S", " S "), 'S', ore("oredict/ingot/steel"));
        // CraftingManager.java:286 (FEHLT)
        g.shaped(item("minecraft:bucket"), 1, p("S S", " S "), 'S', ore("oredict/ingot/steel"));
        // CraftingManager.java:289 (FEHLT)
        g.shaped(item("hbm_m:ammo_press"), 1, p("IPI", "C C", "SSS"), 'I', ore("oredict/ingot/iron"), 'P', Ingredient.of(item("minecraft:piston")), 'C', ore("oredict/ingot/copper"), 'S', Ingredient.of(item("minecraft:stone")));
        // CraftingManager.java:290 (ABW)
        g.shaped(item("hbm_m:machine_siren"), 1, p("SIS", "ICI", "SRS"), 'S', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/any_rubber"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'R', ore("oredict/dust/redstone"));
        // CraftingManager.java:291 (FEHLT)
        g.shaped(item("hbm_m:microwave"), 1, p("III", "SGM", "IDI"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'S', ore("oredict/plate/steel"), 'G', ore("oredict/pane_glass"), 'M', Ingredient.of(item("hbm_m:magnetron")), 'D', Ingredient.of(item("hbm_m:motor")));
        // CraftingManager.java:292 (ABW)
        g.shaped(item("hbm_m:solar_boiler"), 1, p("SHS", "DHD", "SHS"), 'S', ore("oredict/ingot/steel"), 'H', ore("oredict/shell/steel"), 'D', ore("oredict/dye_black"));
        // CraftingManager.java:293 (ABW)
        g.shaped(item("hbm_m:solar_mirror"), 3, p("AAA", " B ", "SSS"), 'A', ore("oredict/plate/aluminum"), 'B', Ingredient.of(item("hbm_m:steel_beam")), 'S', ore("oredict/ingot/steel"));
        // CraftingManager.java:296 (FEHLT)
        g.shaped(item("hbm_m:anvil_murky"), 1, p("UUU", "UAU", "UUU"), 'U', Ingredient.of(item("hbm_m:undefined")), 'A', Ingredient.of(item("hbm_m:anvil_steel")));
        // CraftingManager.java:297 (FEHLT)
        g.shaped(item("hbm_m:fraction_tower"), 1, p("H", "G", "H"), 'H', ore("oredict/plate_sextuple/steel"), 'G', Ingredient.of(item("hbm_m:steel_grate")));
        // CraftingManager.java:299 (FEHLT)
        g.shaped(item("hbm_m:furnace_brick"), 1, p("III", "I I", "BBB"), 'I', Ingredient.of(item("minecraft:brick")), 'B', Ingredient.of(item("minecraft:stone")));
    }

    private static void part2(CraftingGen g) {
        // CraftingManager.java:300 (FEHLT)
        g.shaped(item("hbm_m:furnace_iron"), 1, p("III", "IFI", "BBB"), 'I', ore("oredict/ingot/iron"), 'F', Ingredient.of(item("minecraft:furnace")), 'B', Ingredient.of(item("minecraft:stone_bricks")));
        // CraftingManager.java:301 (ABW)
        g.shaped(item("hbm_m:mixer"), 1, p("PIP", "GCG", "PMP"), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/dura_steel"), 'G', ore("oredict/pane_glass"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'M', Ingredient.of(item("hbm_m:motor")));
        // CraftingManager.java:304 (FEHLT)
        g.shaped(item("hbm_m:upgrade_muffler"), 16, p("III", "IWI", "III"), 'I', ore("oredict/ingot/any_rubber"), 'W', Ingredient.of(item("minecraft:white_wool")));
        // CraftingManager.java:305 (FEHLT)
        g.shaped(item("hbm_m:upgrade_template"), 1, p("WIW", "PCP", "WIW"), 'W', ore("oredict/wire_fine/copper"), 'I', ore("oredict/plate/iron"), 'C', Ingredient.of(item("hbm_m:analog_circuit")), 'P', Ingredient.of(item("hbm_m:plate_polymer")));
        // CraftingManager.java:306 (FEHLT)
        g.shaped(item("hbm_m:upgrade_template"), 1, p("WIW", "PCP", "WIW"), 'W', ore("oredict/wire_fine/copper"), 'I', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'P', Ingredient.of(item("hbm_m:plate_polymer")));
        // CraftingManager.java:320 (ABW)
        g.shapeless(item("hbm_m:multi_detonator"), 1, Ingredient.of(item("hbm_m:detonator")), Ingredient.of(item("hbm_m:advanced_circuit")));
        // CraftingManager.java:321 (ABW)
        g.shapeless(item("hbm_m:range_detonator"), 1, Ingredient.of(item("hbm_m:rangefinder")), Ingredient.of(item("hbm_m:advanced_circuit")), ore("oredict/ingot/rubber"), ore("oredict/wire_dense/gold"));
        // CraftingManager.java:322 (FEHLT)
        g.shapeless(item("hbm_m:detonator_deadman"), 1, Ingredient.of(item("hbm_m:detonator")), Ingredient.of(item("hbm_m:defuser")), Ingredient.of(item("hbm_m:ducttape")));
        // CraftingManager.java:323 (FEHLT)
        g.shaped(item("hbm_m:detonator_de"), 1, p("T", "D", "T"), 'T', Ingredient.of(item("minecraft:tnt")), 'D', Ingredient.of(item("hbm_m:detonator_deadman")));
        // CraftingManager.java:325 (FEHLT)
        g.shaped(item("hbm_m:singularity"), 1, p("ESE", "SBS", "ESE"), 'E', ore("oredict/nugget/euphemium"), 'S', Ingredient.of(item("hbm_m:cell_anti_schrabidium")), 'B', ore("oredict/block/schrabidium"));
        // CraftingManager.java:326 (FEHLT)
        g.shaped(item("hbm_m:singularity_counter_resonant"), 1, p("CTC", "TST", "CTC"), 'C', ore("oredict/plate/cmb_steel"), 'T', ore("oredict/ingot/magnetized_tungsten"), 'S', Ingredient.of(item("hbm_m:singularity")));
        // CraftingManager.java:327 (FEHLT)
        g.shaped(item("hbm_m:singularity_super_heated"), 1, p("CTC", "TST", "CTC"), 'C', ore("oredict/plate/saturnite"), 'T', Ingredient.of(item("hbm_m:powder_power")), 'S', Ingredient.of(item("hbm_m:singularity")));
        // CraftingManager.java:328 (FEHLT)
        g.shaped(item("hbm_m:black_hole"), 1, p("SSS", "SCS", "SSS"), 'C', Ingredient.of(item("hbm_m:singularity")), 'S', Ingredient.of(item("hbm_m:crystal_xen")));
        // CraftingManager.java:329 (FEHLT)
        g.shaped(item("hbm_m:crystal_xen"), 1, p("EEE", "EIE", "EEE"), 'E', Ingredient.of(item("hbm_m:powder_power")), 'I', ore("oredict/ingot/euphemium"));
        // CraftingManager.java:331 (FEHLT)
        g.shapeless(item("hbm_m:fuse"), 1, ore("oredict/plate/steel"), Ingredient.of(item("hbm_m:plate_polymer")), ore("oredict/wire_fine/tungsten"));
        // CraftingManager.java:333 (FEHLT)
        g.shaped(item("hbm_m:blades_steel"), 1, p(" P ", "PIP", " P "), 'P', ore("oredict/plate/steel"), 'I', ore("oredict/ingot/steel"));
        // CraftingManager.java:337 (FEHLT)
        g.shaped(item("hbm_m:blades_steel"), 1, p("PIP"), 'P', ore("oredict/plate/steel"), 'I', Ingredient.of(item("hbm_m:blades_steel")));
        // CraftingManager.java:334 (FEHLT)
        g.shaped(item("hbm_m:blades_titanium"), 1, p(" P ", "PIP", " P "), 'P', ore("oredict/plate/titanium"), 'I', ore("oredict/ingot/titanium"));
        // CraftingManager.java:338 (FEHLT)
        g.shaped(item("hbm_m:blades_titanium"), 1, p("PIP"), 'P', ore("oredict/plate/titanium"), 'I', Ingredient.of(item("hbm_m:blades_titanium")));
        // CraftingManager.java:335 (FEHLT)
        g.shaped(item("hbm_m:blades_desh"), 1, p(" P ", "PBP", " P "), 'P', Ingredient.of(item("hbm_m:plate_desh")), 'B', Ingredient.of(item("hbm_m:blades_titanium")));
        // CraftingManager.java:340 (ABW)
        g.shaped(item("hbm_m:laser_crystal_co2"), 1, p("QDQ", "NCN", "QDQ"), 'Q', Ingredient.of(item("hbm_m:glass_quartz")), 'D', ore("oredict/ingot/workers_alloy"), 'N', ore("oredict/ingot/niobium"), 'C', filled("hbm_m:fluid_tank_full", ModFluids.CARBONDIOXIDE.getSource()));
        // CraftingManager.java:341 (FEHLT)
        g.shaped(item("hbm_m:laser_crystal_bismuth"), 1, p("QUQ", "BCB", "QTQ"), 'Q', Ingredient.of(item("hbm_m:glass_quartz")), 'U', ore("oredict/ingot/uranium"), 'T', ore("oredict/ingot/thorium232"), 'B', Ingredient.of(item("hbm_m:nugget_bismuth")), 'C', Ingredient.of(item("hbm_m:crystal_rare")));
        // CraftingManager.java:342 (FEHLT)
        g.shaped(item("hbm_m:laser_crystal_cmb"), 1, p("QBQ", "CSC", "QBQ"), 'Q', Ingredient.of(item("hbm_m:glass_quartz")), 'B', ore("oredict/ingot/cmb_steel"), 'C', ore("oredict/ingot/schrabidate"), 'S', Ingredient.of(item("hbm_m:cell_anti_schrabidium")));
        // CraftingManager.java:343 (FEHLT)
        g.shaped(item("hbm_m:laser_crystal_dnt"), 1, p("QDQ", "SBS", "QDQ"), 'Q', Ingredient.of(item("hbm_m:glass_quartz")), 'D', ore("oredict/ingot/dineutronium"), 'B', Ingredient.of(item("hbm_m:egg_balefire")), 'S', Ingredient.of(item("hbm_m:spark_mix_powder")));
        // CraftingManager.java:344 (FEHLT)
        g.shaped(item("hbm_m:laser_crystal_digamma"), 1, p("QUQ", "UEU", "QUQ"), 'Q', Ingredient.of(item("hbm_m:glass_quartz")), 'U', Ingredient.of(item("hbm_m:undefined")), 'E', Ingredient.of(item("hbm_m:electronium_ingot")));
        // CraftingManager.java:360 (FEHLT)
        g.shaped(item("hbm_m:brick_light"), 4, p("FBF", "BFB", "FBF"), 'F', Ingredient.of(item("minecraft:oak_fence")), 'B', Ingredient.of(item("minecraft:bricks")));
        // CraftingManager.java:361 (FEHLT)
        g.shaped(item("hbm_m:brick_asbestos"), 2, p(" A ", "ABA", " A "), 'B', Ingredient.of(item("hbm_m:brick_light")), 'A', ore("oredict/ingot/asbestos"));
        // CraftingManager.java:362 (FEHLT)
        g.shaped(item("hbm_m:concrete"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")));
        // CraftingManager.java:363 (ABW)
        g.shaped(item("hbm_m:concrete_pillar"), 6, p("CBC", "CBC", "CBC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), 'B', Ingredient.of(item("minecraft:iron_bars")));
        // CraftingManager.java:364 (ABW)
        g.shaped(item("hbm_m:brick_concrete"), 4, p(" C ", "CBC", " C "), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), 'B', Ingredient.of(item("minecraft:clay_ball")));
        // CraftingManager.java:365 (ABW)
        g.shaped(item("hbm_m:brick_concrete"), 4, p(" C ", "CBC", " C "), 'C', Ingredient.of(item("hbm_m:concrete")), 'B', Ingredient.of(item("minecraft:clay_ball")));
        // CraftingManager.java:366 (FEHLT)
        g.shaped(item("hbm_m:brick_concrete_mossy"), 8, p("CCC", "CVC", "CCC"), 'C', Ingredient.of(item("hbm_m:brick_concrete")), 'V', Ingredient.of(item("minecraft:vine")));
        // CraftingManager.java:367 (FEHLT)
        g.shaped(item("hbm_m:brick_concrete_cracked"), 6, p(" C ", "C C", " C "), 'C', Ingredient.of(item("hbm_m:brick_concrete")));
        // CraftingManager.java:368 (FEHLT)
        g.shaped(item("hbm_m:brick_concrete_broken"), 6, p(" C ", "C C", " C "), 'C', Ingredient.of(item("hbm_m:brick_concrete_cracked")));
        // CraftingManager.java:369 (FEHLT)
        g.shaped(item("hbm_m:ducrete"), 4, p("DD", "DD"), 'D', Ingredient.of(item("hbm_m:ducrete_smooth")));
        // CraftingManager.java:370 (FEHLT)
        g.shaped(item("hbm_m:brick_ducrete"), 4, p("CDC", "DLD", "CDC"), 'D', Ingredient.of(item("hbm_m:ducrete_smooth")), 'C', Ingredient.of(item("minecraft:clay_ball")), 'L', Ingredient.of(item("hbm_m:plate_lead")));
        // CraftingManager.java:371 (FEHLT)
        g.shaped(item("hbm_m:brick_ducrete"), 4, p("CDC", "DLD", "CDC"), 'D', Ingredient.of(item("hbm_m:ducrete")), 'C', Ingredient.of(item("minecraft:clay_ball")), 'L', Ingredient.of(item("hbm_m:plate_lead")));
        // CraftingManager.java:372 (FEHLT)
        g.shaped(item("hbm_m:reinforced_ducrete"), 4, p("DSD", "SUS", "DSD"), 'D', Ingredient.of(item("hbm_m:brick_ducrete")), 'S', Ingredient.of(item("hbm_m:plate_steel")), 'U', ore("oredict/billet/uranium238"));
        // CraftingManager.java:373 (FEHLT)
        g.shaped(item("hbm_m:brick_obsidian"), 4, p("FBF", "BFB", "FBF"), 'F', Ingredient.of(item("minecraft:iron_bars")), 'B', Ingredient.of(item("minecraft:obsidian")));
        // CraftingManager.java:374 (FEHLT)
        g.shaped(item("hbm_m:meteor_polished"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:block_meteor_broken")));
        // CraftingManager.java:375 (FEHLT)
        g.shaped(item("hbm_m:meteor_pillar"), 2, p("C", "C"), 'C', Ingredient.of(item("hbm_m:meteor_polished")));
        // CraftingManager.java:376 (FEHLT)
        g.shaped(item("hbm_m:meteor_brick"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:meteor_polished")));
        // CraftingManager.java:377 (FEHLT)
        g.shaped(item("hbm_m:meteor_brick_mossy"), 8, p("CCC", "CVC", "CCC"), 'C', Ingredient.of(item("hbm_m:meteor_brick")), 'V', Ingredient.of(item("minecraft:vine")));
        // CraftingManager.java:378 (FEHLT)
        g.shaped(item("hbm_m:meteor_brick_cracked"), 6, p(" C ", "C C", " C "), 'C', Ingredient.of(item("hbm_m:meteor_brick")));
        // CraftingManager.java:379 (FEHLT)
        g.shaped(item("hbm_m:meteor_battery"), 1, p("MSM", "MWM", "MSM"), 'M', Ingredient.of(item("hbm_m:meteor_polished")), 'S', ore("oredict/block/starmetal"), 'W', ore("oredict/wire_fine/schrabidium"));
        // CraftingManager.java:380 (FEHLT)
        g.shaped(item("hbm_m:tile_lab"), 4, p("CBC", "CBC", "CBC"), 'C', Ingredient.of(item("minecraft:brick")), 'B', ore("oredict/ingot/asbestos"));
        // CraftingManager.java:381 (FEHLT)
        g.shaped(item("hbm_m:tile_lab_cracked"), 6, p(" C ", "C C", " C "), 'C', Ingredient.of(item("hbm_m:tile_lab")));
        // CraftingManager.java:382 (FEHLT)
        g.shaped(item("hbm_m:tile_lab_broken"), 6, p(" C ", "C C", " C "), 'C', Ingredient.of(item("hbm_m:tile_lab_cracked")));
        // CraftingManager.java:383 (FEHLT)
        g.shapeless(item("hbm_m:asphalt_light"), 1, Ingredient.of(item("hbm_m:asphalt")), Ingredient.of(item("minecraft:glowstone_dust")));
        // CraftingManager.java:384 (FEHLT)
        g.shapeless(item("hbm_m:asphalt"), 1, Ingredient.of(item("hbm_m:asphalt_light")));
        // CraftingManager.java:392 (FEHLT)
        g.shapeless(item("hbm_m:concrete_smooth"), 1, Ingredient.of(item("hbm_m:concrete_black"), item("hbm_m:concrete_blue"), item("hbm_m:concrete_brown"), item("hbm_m:concrete_cyan"), item("hbm_m:concrete_gray"), item("hbm_m:concrete_green"), item("hbm_m:concrete_light_blue"), item("hbm_m:concrete_lime"), item("hbm_m:concrete_magenta"), item("hbm_m:concrete_orange"), item("hbm_m:concrete_pink"), item("hbm_m:concrete_purple"), item("hbm_m:concrete_red"), item("hbm_m:concrete_silver"), item("hbm_m:concrete_white"), item("hbm_m:concrete_yellow")));
        // CraftingManager.java:393 (FEHLT)
        g.shapeless(item("hbm_m:concrete_smooth"), 1, Ingredient.of(item("hbm_m:concrete_colored_ext_bronze"), item("hbm_m:concrete_colored_ext_hazard"), item("hbm_m:concrete_colored_ext_indigo"), item("hbm_m:concrete_colored_ext_machine"), item("hbm_m:concrete_colored_ext_machine_stripe"), item("hbm_m:concrete_colored_ext_pink"), item("hbm_m:concrete_colored_ext_purple"), item("hbm_m:concrete_colored_ext_sand")));
        // CraftingManager.java:400 (FEHLT)
        g.shapeless(item("hbm_m:platemetal_base"), 1, Ingredient.of(item("hbm_m:platemetal_base"), item("hbm_m:platemetal_black"), item("hbm_m:platemetal_blue"), item("hbm_m:platemetal_cyan"), item("hbm_m:platemetal_green"), item("hbm_m:platemetal_light_blue"), item("hbm_m:platemetal_light_gray"), item("hbm_m:platemetal_lime"), item("hbm_m:platemetal_magenta"), item("hbm_m:platemetal_orange"), item("hbm_m:platemetal_pink"), item("hbm_m:platemetal_purple"), item("hbm_m:platemetal_red"), item("hbm_m:platemetal_white"), item("hbm_m:platemetal_yellow")));
        // CraftingManager.java:402 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_machine"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_brown"), '2', ore("oredict/dye_gray"));
        // CraftingManager.java:403 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_machine_stripe"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_brown"), '2', ore("oredict/dye_black"));
        // CraftingManager.java:404 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_indigo"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_blue"), '2', ore("oredict/dye_purple"));
        // CraftingManager.java:405 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_purple"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_purple"), '2', ore("oredict/dye_purple"));
        // CraftingManager.java:406 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_pink"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_pink"), '2', ore("oredict/dye_red"));
        // CraftingManager.java:407 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_hazard"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_yellow"), '2', ore("oredict/dye_black"));
        // CraftingManager.java:408 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_sand"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_yellow"), '2', ore("oredict/dye_gray"));
    }

    private static void part3(CraftingGen g) {
        // CraftingManager.java:409 (FEHLT)
        g.shaped(item("hbm_m:concrete_colored_ext_bronze"), 6, p("CCC", "1 2", "CCC"), 'C', Ingredient.of(item("hbm_m:concrete_smooth")), '1', ore("oredict/dye_orange"), '2', ore("oredict/dye_brown"));
        // CraftingManager.java:411 (FEHLT)
        g.shaped(item("hbm_m:gneiss_tile"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:stone_gneiss")));
        // CraftingManager.java:412 (FEHLT)
        g.shaped(item("hbm_m:gneiss_brick"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:gneiss_tile")));
        // CraftingManager.java:413 (FEHLT)
        g.shapeless(item("hbm_m:gneiss_chiseled"), 1, Ingredient.of(item("hbm_m:gneiss_tile")));
        // CraftingManager.java:414 (FEHLT)
        g.shaped(item("hbm_m:depth_brick"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:stone_depth")));
        // CraftingManager.java:415 (FEHLT)
        g.shaped(item("hbm_m:depth_tiles"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:depth_brick")));
        // CraftingManager.java:416 (FEHLT)
        g.shaped(item("hbm_m:depth_nether_brick"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:stone_depth_nether")));
        // CraftingManager.java:417 (FEHLT)
        g.shaped(item("hbm_m:depth_nether_tiles"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:depth_nether_brick")));
        // CraftingManager.java:418 (FEHLT)
        g.shaped(item("hbm_m:basalt_polished"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:basalt_smooth")));
        // CraftingManager.java:419 (FEHLT)
        g.shaped(item("hbm_m:basalt_brick"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:basalt_polished")));
        // CraftingManager.java:420 (FEHLT)
        g.shaped(item("hbm_m:basalt_tiles"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:basalt_brick")));
        // CraftingManager.java:422 (FEHLT)
        g.shapeless(item("hbm_m:lightstone_unrefined"), 4, Ingredient.of(item("minecraft:stone")), Ingredient.of(item("minecraft:stone")), Ingredient.of(item("minecraft:stone")), Ingredient.of(item("hbm_m:limestone_powder")));
        // CraftingManager.java:423 (FEHLT)
        g.shaped(item("hbm_m:lightstone_tile"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:lightstone_unrefined")));
        // CraftingManager.java:424 (FEHLT)
        g.shaped(item("hbm_m:lightstone_bricks"), 4, p("CC", "CC"), 'C', Ingredient.of(item("hbm_m:lightstone_tile")));
        // CraftingManager.java:425 (FEHLT)
        g.shapeless(item("hbm_m:lightstone_bricks_chiseled"), 1, Ingredient.of(item("hbm_m:lightstone_bricks")));
        // CraftingManager.java:426 (FEHLT)
        g.shapeless(item("hbm_m:lightstone_chiseled"), 1, Ingredient.of(item("hbm_m:lightstone_unrefined")));
        // CraftingManager.java:428 (FEHLT)
        g.shaped(item("hbm_m:reinforced_brick"), 4, p("FBF", "BFB", "FBF"), 'F', Ingredient.of(item("minecraft:iron_bars")), 'B', Ingredient.of(item("hbm_m:brick_concrete")));
        // CraftingManager.java:429 (FEHLT)
        g.shaped(item("hbm_m:brick_compound"), 4, p("FBF", "BTB", "FBF"), 'F', ore("oredict/bolt/steel"), 'B', Ingredient.of(item("hbm_m:reinforced_brick")), 'T', ore("oredict/any/tar"));
        // CraftingManager.java:430 (ABW)
        g.shaped(item("hbm_m:reinforced_glass"), 4, p("FBF", "BFB", "FBF"), 'F', Ingredient.of(item("minecraft:iron_bars")), 'B', Ingredient.of(item("minecraft:glass")));
        // CraftingManager.java:431 (FEHLT)
        g.shaped(item("hbm_m:reinforced_glass_pane"), 16, p("   ", "GGG", "GGG"), 'G', Ingredient.of(item("hbm_m:reinforced_glass")));
        // CraftingManager.java:432 (FEHLT)
        g.shaped(item("hbm_m:reinforced_laminate_pane"), 16, p("   ", "LLL", "LLL"), 'L', Ingredient.of(item("hbm_m:reinforced_laminate")));
        // CraftingManager.java:433 (FEHLT)
        g.shaped(item("hbm_m:reinforced_light"), 1, p("FFF", "FBF", "FFF"), 'F', Ingredient.of(item("minecraft:iron_bars")), 'B', Ingredient.of(item("minecraft:glowstone")));
        // CraftingManager.java:434 (FEHLT)
        g.shaped(item("hbm_m:reinforced_lamp_off"), 1, p("FFF", "FBF", "FFF"), 'F', Ingredient.of(item("minecraft:iron_bars")), 'B', Ingredient.of(item("minecraft:redstone_lamp")));
        // CraftingManager.java:435 (FEHLT)
        g.shaped(item("hbm_m:reinforced_sand"), 4, p("FBF", "BFB", "FBF"), 'F', Ingredient.of(item("minecraft:iron_bars")), 'B', Ingredient.of(item("minecraft:sandstone")));
        // CraftingManager.java:437 (ABW)
        g.shapeless(item("hbm_m:lamp_tritium_green_off"), 1, ore("oredict/block/glass"), ore("oredict/dust/red_phosphorus"), container(ModFluids.TRITIUM.getSource(), 1000), ore("oredict/dust/sulfur"));
        // CraftingManager.java:438 (ABW)
        g.shapeless(item("hbm_m:lamp_tritium_blue_off"), 1, ore("oredict/block/glass"), ore("oredict/dust/red_phosphorus"), container(ModFluids.TRITIUM.getSource(), 1000), ore("oredict/dust/aluminum"));
        // CraftingManager.java:445 (ABW)
        g.shaped(item("hbm_m:barbed_wire"), 16, p("AIA", "I I", "AIA"), 'A', ore("oredict/wire_fine/steel"), 'I', ore("oredict/ingot/iron"));
        // CraftingManager.java:446 (ABW)
        g.shaped(item("hbm_m:barbed_wire_fire"), 8, p("BBB", "BIB", "BBB"), 'B', Ingredient.of(item("hbm_m:barbed_wire")), 'I', ore("oredict/dust/red_phosphorus"));
        // CraftingManager.java:447 (ABW)
        g.shaped(item("hbm_m:barbed_wire_poison"), 8, p("BBB", "BIB", "BBB"), 'B', Ingredient.of(item("hbm_m:barbed_wire")), 'I', Ingredient.of(item("hbm_m:powder_poison")));
        // CraftingManager.java:448 (ABW)
        g.shaped(item("hbm_m:barbed_wire_acid"), 8, p("BBB", "BIB", "BBB"), 'B', Ingredient.of(item("hbm_m:barbed_wire")), 'I', filled("hbm_m:fluid_tank_full", ModFluids.PEROXIDE.getSource()));
        // CraftingManager.java:450 (FEHLT)
        g.shaped(item("hbm_m:barbed_wire_ultradeath"), 4, p("BCB", "CIC", "BCB"), 'B', Ingredient.of(item("hbm_m:barbed_wire")), 'C', Ingredient.of(item("hbm_m:yellowcake_powder")), 'I', Ingredient.of(item("hbm_m:nuclear_waste")));
        // CraftingManager.java:452 (FEHLT)
        g.shapeless(item("hbm_m:sandbags"), 4, Ingredient.of(item("hbm_m:plate_polymer")), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"));
        // CraftingManager.java:470 (ABW)
        g.shaped(item("hbm_m:steel_scaffold"), 8, p("SSS", "SDS", "SSS"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'D', ore("oredict/dye_gray"));
        // CraftingManager.java:471 (ABW)
        g.shaped(item("hbm_m:steel_scaffold_red"), 8, p("SSS", "SDS", "SSS"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'D', ore("oredict/dye_red"));
        // CraftingManager.java:472 (ABW)
        g.shaped(item("hbm_m:steel_scaffold_white"), 8, p("SSS", "SDS", "SSS"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'D', ore("oredict/dye_white"));
        // CraftingManager.java:473 (ABW)
        g.shaped(item("hbm_m:steel_scaffold_yellow"), 8, p("SSS", "SDS", "SSS"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'D', ore("oredict/dye_yellow"));
        // CraftingManager.java:475 (FEHLT)
        g.shaped(item("hbm_m:wood_barrier"), 8, p("SFS", "SFS"), 'S', ore("oredict/slab_wood"), 'F', Ingredient.of(item("minecraft:oak_fence")));
        // CraftingManager.java:476 (FEHLT)
        g.shaped(item("hbm_m:wood_structure"), 16, p("SSS", "F F"), 'S', ore("oredict/slab_wood"), 'F', Ingredient.of(item("minecraft:oak_fence")));
        // CraftingManager.java:477 (FEHLT)
        g.shaped(item("hbm_m:wood_structure_ceiling"), 16, p("F F", "SSS"), 'S', ore("oredict/slab_wood"), 'F', Ingredient.of(item("minecraft:oak_fence")));
        // CraftingManager.java:478 (FEHLT)
        g.shaped(item("hbm_m:wood_structure_scaffold"), 4, p("SSS", "F F", "F F"), 'S', ore("oredict/slab_wood"), 'F', Ingredient.of(item("minecraft:oak_fence")));
        // CraftingManager.java:485 (FEHLT)
        g.shaped(item("hbm_m:sat_dock"), 1, p("SSS", "PCP"), 'S', ore("oredict/ingot/steel"), 'P', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:crate_iron")));
        // CraftingManager.java:486 (ABW)
        g.shaped(item("hbm_m:book_guide"), 1, p("IBI", "LBL", "IBI"), 'B', Ingredient.of(item("minecraft:book")), 'I', ore("oredict/dye_black"), 'L', ore("oredict/dye_blue"));
        // CraftingManager.java:505 (FEHLT)
        g.shaped(item("hbm_m:bomb_multi"), 1, p("AAD", "CHF", "AAD"), 'A', ore("oredict/wire_fine/aluminum"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'H', ore("oredict/shell/aluminum"), 'F', Ingredient.of(item("hbm_m:fins_quad_titanium")), 'D', ore("oredict/dye_white"));
        // CraftingManager.java:506 (FEHLT)
        g.shapeless(item("hbm_m:ice_powder"), 4, Ingredient.of(item("minecraft:snowball")), ore("oredict/dust/saltpeter"), ore("oredict/dust/redstone"));
        // CraftingManager.java:508 (FEHLT)
        g.shapeless(item("hbm_m:pellet_gas"), 2, Ingredient.of(item("minecraft:water_bucket")), ore("oredict/dust/glowstone"), ore("oredict/plate/steel"));
        // CraftingManager.java:510 (FEHLT)
        g.shaped(item("hbm_m:flame_pony"), 1, p(" O ", "DPD", " O "), 'D', ore("oredict/dye_pink"), 'O', ore("oredict/dye_yellow"), 'P', Ingredient.of(item("minecraft:paper")));
        // CraftingManager.java:512 (FEHLT)
        g.shaped(item("hbm_m:flame_politics"), 1, p(" I ", "IPI", " I "), 'P', Ingredient.of(item("minecraft:paper")), 'I', ore("oredict/dye_black"));
        // CraftingManager.java:513 (FEHLT)
        g.shaped(item("hbm_m:flame_opinion"), 1, p(" R ", "RPR", " R "), 'P', Ingredient.of(item("minecraft:paper")), 'R', ore("oredict/dye_red"));
        // CraftingManager.java:515 (FEHLT)
        g.shaped(item("hbm_m:solid_fuel_presto"), 1, p(" P ", "SRS", " P "), 'P', Ingredient.of(item("minecraft:paper")), 'S', Ingredient.of(item("hbm_m:solid_fuel")), 'R', ore("oredict/dust/redstone"));
        // CraftingManager.java:516 (FEHLT)
        g.shapeless(item("hbm_m:solid_fuel_presto_triplet"), 1, Ingredient.of(item("hbm_m:solid_fuel_presto")), Ingredient.of(item("hbm_m:solid_fuel_presto")), Ingredient.of(item("hbm_m:solid_fuel_presto")), Ingredient.of(item("hbm_m:ball_dynamite")));
        // CraftingManager.java:517 (FEHLT)
        g.shaped(item("hbm_m:solid_fuel_presto_bf"), 1, p(" P ", "SRS", " P "), 'P', Ingredient.of(item("minecraft:paper")), 'S', Ingredient.of(item("hbm_m:solid_fuel_bf")), 'R', ore("oredict/dust/redstone"));
        // CraftingManager.java:518 (FEHLT)
        g.shapeless(item("hbm_m:solid_fuel_presto_triplet_bf"), 1, Ingredient.of(item("hbm_m:solid_fuel_presto_bf")), Ingredient.of(item("hbm_m:solid_fuel_presto_bf")), Ingredient.of(item("hbm_m:solid_fuel_presto_bf")), Ingredient.of(item("hbm_m:c4_ingot")));
        // CraftingManager.java:524 (ABW)
        g.shaped(item("hbm_m:det_miner"), 4, p("FFF", "ITI", "ITI"), 'F', Ingredient.of(item("minecraft:flint")), 'I', ore("oredict/plate/iron"), 'T', Ingredient.of(item("hbm_m:ball_dynamite")));
        // CraftingManager.java:525 (ABW)
        g.shaped(item("hbm_m:det_miner"), 12, p("FFF", "ITI", "ITI"), 'F', Ingredient.of(item("minecraft:flint")), 'I', ore("oredict/plate/steel"), 'T', ore("oredict/ingot/any_plasticexplosive"));
        // CraftingManager.java:532 (FEHLT)
        g.shaped(item("hbm_m:hev_battery"), 4, p(" W ", "IEI", "ICI"), 'W', ore("oredict/wire_fine/gold"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'E', ore("oredict/dust/redstone"), 'C', ore("oredict/dust/cobalt"));
        // CraftingManager.java:533 (FEHLT)
        g.shaped(item("hbm_m:hev_battery"), 4, p(" W ", "ICI", "IEI"), 'W', ore("oredict/wire_fine/gold"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'E', ore("oredict/dust/redstone"), 'C', ore("oredict/dust/cobalt"));
        // CraftingManager.java:534 (FEHLT)
        g.shapeless(item("hbm_m:hev_battery"), 1, Ingredient.of(item("hbm_m:hev_battery")));
        // CraftingManager.java:535 (FEHLT)
        g.shapeless(item("hbm_m:hev_battery"), 1, Ingredient.of(item("hbm_m:hev_battery")));
        // CraftingManager.java:546 (FEHLT)
        g.shaped(item("hbm_m:igniter"), 1, p(" W", "SC", "CE"), 'S', ore("oredict/plate/steel"), 'W', ore("oredict/wire_fine/schrabidium"), 'C', Ingredient.of(item("hbm_m:advanced_circuit")), 'E', ore("oredict/ingot/euphemium"));
        // CraftingManager.java:547 (FEHLT)
        g.shaped(item("hbm_m:watch"), 1, p("LYL", "EWE", "LYL"), 'E', ore("oredict/ingot/euphemium"), 'L', ore("oredict/dye_blue"), 'W', Ingredient.of(item("minecraft:clock")), 'Y', Ingredient.of(item("hbm_m:billet_yharonite")));
    }

    private static void part4(CraftingGen g) {
        // CraftingManager.java:549 (FEHLT)
        g.shaped(item("hbm_m:key"), 1, p("  B", " B ", "P  "), 'P', ore("oredict/plate/steel"), 'B', ore("oredict/bolt/steel"));
        // CraftingManager.java:551 (FEHLT)
        g.shaped(item("hbm_m:key_red"), 1, p("RCA", "CIC", "KCR"), 'R', ore("oredict/dye_red"), 'C', ore("oredict/wire_dense/starmetal"), 'A', Ingredient.of(item("hbm_m:gem_alexandrite")), 'I', Ingredient.of(item("hbm_m:chainsteel_ingot")), 'K', Ingredient.of(item("hbm_m:key")));
        // CraftingManager.java:552 (FEHLT)
        g.shaped(item("hbm_m:pin"), 1, p("W ", " W", " W"), 'W', ore("oredict/wire_fine/copper"));
        // CraftingManager.java:553 (FEHLT)
        g.shaped(item("hbm_m:padlock_rusty"), 1, p("I", "B", "I"), 'I', ore("oredict/ingot/iron"), 'B', ore("oredict/bolt/steel"));
        // CraftingManager.java:554 (FEHLT)
        g.shaped(item("hbm_m:padlock"), 1, p(" P ", "PBP", "PPP"), 'P', ore("oredict/plate/steel"), 'B', ore("oredict/bolt/steel"));
        // CraftingManager.java:555 (FEHLT)
        g.shaped(item("hbm_m:padlock_reinforced"), 1, p(" P ", "PBP", "PDP"), 'P', ore("oredict/plate/dura_steel"), 'D', Ingredient.of(item("hbm_m:plate_desh")), 'B', ore("oredict/bolt/dura_steel"));
        // CraftingManager.java:556 (FEHLT)
        g.shaped(item("hbm_m:padlock_unbreakable"), 1, p(" P ", "PBP", "PDP"), 'P', ore("oredict/plate/saturnite"), 'D', ore("oredict/gem/diamond"), 'B', ore("oredict/bolt/dura_steel"));
        // CraftingManager.java:562 (FEHLT)
        g.shaped(item("hbm_m:polaroid"), 1, p(" C ", "RPY", " B "), 'B', ore("oredict/dust/lapis"), 'C', ore("oredict/dust/coal"), 'R', ore("oredict/dust/mingrade"), 'Y', ore("oredict/dust/gold"), 'P', Ingredient.of(item("minecraft:paper")));
        // CraftingManager.java:565 (ABW)
        g.shapeless(item("hbm_m:crystal_charred"), 1, ore("oredict/dust/strontium"), ore("oredict/dust/cobalt"), ore("oredict/dust/bromine"), ore("oredict/dust/niobium"), ore("oredict/dust/tennessine"), ore("oredict/dust/cerium"), Ingredient.of(item("hbm_m:block_meteor")), ore("oredict/block/aluminum"), Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:569 (ABW)
        g.shaped(item("hbm_m:fluid_duct"), 8, p("SAS", "   ", "SAS"), 'S', ore("oredict/plate/steel"), 'A', ore("oredict/plate/aluminum"));
        // CraftingManager.java:663 (ABW)
        g.shapeless(item("hbm_m:fluid_duct"), 1, Ingredient.of(item("hbm_m:fluid_duct")));
        // CraftingManager.java:570 (ABW)
        g.shaped(item("hbm_m:fluid_duct_silver"), 8, p("IAI", "   ", "IAI"), 'I', ore("oredict/plate/iron"), 'A', ore("oredict/plate/aluminum"));
        // CraftingManager.java:571 (ABW)
        g.shaped(item("hbm_m:fluid_duct_colored"), 8, p("ASA", "   ", "ASA"), 'S', ore("oredict/plate/steel"), 'A', ore("oredict/plate/aluminum"));
        // CraftingManager.java:572 (ABW)
        g.shaped(item("hbm_m:fluid_duct_paintable"), 8, p("SAS", "A A", "SAS"), 'S', ore("oredict/ingot/steel"), 'A', ore("oredict/plate/aluminum"));
        // CraftingManager.java:579 (ABW)
        g.shaped(item("hbm_m:pneumatic_tube"), 8, p("CRC"), 'C', ore("oredict/plate_triple/copper"), 'R', ore("oredict/ingot/any_rubber"));
        // CraftingManager.java:580 (ABW)
        g.shaped(item("hbm_m:pneumatic_tube"), 24, p("CRC"), 'C', ore("oredict/plate_sextuple/copper"), 'R', ore("oredict/ingot/any_rubber"));
        // CraftingManager.java:581 (ABW)
        g.shaped(item("hbm_m:pneumatic_tube_paintable"), 4, p("SAS", "A A", "SAS"), 'S', ore("oredict/plate/steel"), 'A', Ingredient.of(item("hbm_m:pneumatic_tube")));
        // CraftingManager.java:584 (ABW)
        g.shaped(item("hbm_m:template_folder"), 1, p("LPL", "BPB", "LPL"), 'P', Ingredient.of(item("minecraft:paper")), 'L', ore("oredict/dye"), 'B', ore("oredict/dye"));
        // CraftingManager.java:585 (FEHLT)
        g.shaped(item("hbm_m:pellet_antimatter"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:cell_antimatter")));
        // CraftingManager.java:586 (ABW)
        g.shaped(item("hbm_m:fluid_tank_empty"), 8, p("121", "1G1", "121"), '1', ore("oredict/plate/aluminum"), '2', ore("oredict/plate/iron"), 'G', ore("oredict/pane_glass"));
        // CraftingManager.java:588 (ABW)
        g.shaped(item("hbm_m:fluid_barrel_empty"), 2, p("121", "1G1", "121"), '1', ore("oredict/plate/steel"), '2', ore("oredict/plate/aluminum"), 'G', ore("oredict/pane_glass"));
        // CraftingManager.java:591 [nur bei !528] (ABW)
        g.when("!528").shaped(item("hbm_m:inf_water"), 1, p("222", "131", "222"), '1', container(ModFluids.WATER.getSource(), 1000), '2', ore("oredict/plate/aluminum"), '3', ore("oredict/gem/diamond"));
        // CraftingManager.java:592 [nur bei !528] (FEHLT)
        g.when("!528").shaped(item("hbm_m:inf_water_mk2"), 1, p("BPB", "PTP", "BPB"), 'B', Ingredient.of(item("hbm_m:inf_water")), 'P', ore("oredict/ntmpipe/steel"), 'T', ore("oredict/shell/steel"));
        // CraftingManager.java:596 (FEHLT)
        g.shaped(item("hbm_m:piston_selenium"), 1, p("SSS", "STS", " D "), 'S', ore("oredict/plate/steel"), 'T', ore("oredict/ingot/tungsten"), 'D', ore("oredict/bolt/dura_steel"));
        // CraftingManager.java:597 (FEHLT)
        g.shapeless(item("hbm_m:catalyst_clay"), 1, ore("oredict/dust/iron"), Ingredient.of(item("minecraft:clay_ball")));
        // CraftingManager.java:598 (FEHLT)
        g.shaped(item("hbm_m:singularity_spark"), 1, p("XAX", "BCB", "XAX"), 'X', Ingredient.of(item("hbm_m:plate_dineutronium")), 'A', Ingredient.of(item("hbm_m:singularity_counter_resonant")), 'B', Ingredient.of(item("hbm_m:singularity_super_heated")), 'C', Ingredient.of(item("hbm_m:black_hole")));
        // CraftingManager.java:599 (FEHLT)
        g.shaped(item("hbm_m:singularity_spark"), 1, p("XBX", "ACA", "XBX"), 'X', Ingredient.of(item("hbm_m:plate_dineutronium")), 'A', Ingredient.of(item("hbm_m:singularity_counter_resonant")), 'B', Ingredient.of(item("hbm_m:singularity_super_heated")), 'C', Ingredient.of(item("hbm_m:black_hole")));
        // CraftingManager.java:600 (FEHLT)
        g.shaped(item("hbm_m:ams_core_sing"), 1, p("EAE", "ASA", "EAE"), 'E', Ingredient.of(item("hbm_m:plate_euphemium")), 'A', Ingredient.of(item("hbm_m:cell_anti_schrabidium")), 'S', Ingredient.of(item("hbm_m:singularity")));
        // CraftingManager.java:601 (FEHLT)
        g.shaped(item("hbm_m:ams_core_wormhole"), 1, p("DPD", "PSP", "DPD"), 'D', Ingredient.of(item("hbm_m:plate_dineutronium")), 'P', Ingredient.of(item("hbm_m:spark_mix_powder")), 'S', Ingredient.of(item("hbm_m:singularity")));
        // CraftingManager.java:602 (ABW)
        g.shaped(item("hbm_m:ams_core_eyeofharmony"), 1, p("ALA", "LSL", "ALA"), 'A', Ingredient.of(item("hbm_m:plate_dalekanium")), 'L', filled("hbm_m:fluid_barrel_full", ModFluids.LAVA.getSource()), 'S', Ingredient.of(item("hbm_m:black_hole")));
        // CraftingManager.java:603 (FEHLT)
        g.shaped(item("hbm_m:ams_core_thingy"), 1, p("NSN", "NGN", "G G"), 'N', ore("oredict/nugget/gold"), 'G', ore("oredict/ingot/gold"), 'S', Ingredient.of(item("hbm_m:battery_pack_battery_quantum")));
        // CraftingManager.java:604 (FEHLT)
        g.shaped(item("hbm_m:photo_panel"), 1, p(" G ", "IPI", " C "), 'G', ore("oredict/pane_glass"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'P', ore("oredict/billet/silicon"), 'C', Ingredient.of(item("hbm_m:pcb")));
        // CraftingManager.java:605 (ABW)
        g.shaped(item("hbm_m:machine_satlinker"), 1, p("PSP", "SCS", "PSP"), 'P', ore("oredict/ingot/any_resistant_alloy"), 'S', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:sat_chip")));
        // CraftingManager.java:606 (ABW)
        g.shaped(item("hbm_m:machine_keyforge"), 1, p("PCP", "WSW", "WSW"), 'P', ore("oredict/plate/steel"), 'S', ore("oredict/ingot/tungsten"), 'C', Ingredient.of(item("hbm_m:padlock")), 'W', ore("oredict/plank_wood"));
        // CraftingManager.java:607 (ABW)
        g.shapeless(item("hbm_m:geiger_counter"), 1, Ingredient.of(item("hbm_m:geiger_counter")));
        // ToolRecipes.java:113 (ABW)
        g.shaped(item("hbm_m:geiger_counter"), 1, p("GPP", "WCS", "WBB"), 'W', ore("oredict/wire_fine/gold"), 'P', ore("oredict/ingot/any_rubber"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'G', ore("oredict/ingot/gold"), 'S', ore("oredict/plate/steel"), 'B', ore("oredict/ingot/beryllium"));
        // ToolRecipes.java:115 (ABW)
        g.shapeless(item("hbm_m:geiger_counter"), 1, Ingredient.of(item("hbm_m:geiger_counter")));
        // CraftingManager.java:608 (FEHLT)
        g.shaped(item("hbm_m:sat_coord"), 1, p("SII", "SCA", "SPP"), 'I', ore("oredict/ingot/steel"), 'S', ore("oredict/ingot/starmetal"), 'P', Ingredient.of(item("hbm_m:plate_polymer")), 'C', Ingredient.of(item("hbm_m:sat_chip")), 'A', Ingredient.of(item("hbm_m:advanced_circuit")));
        // CraftingManager.java:609 (FEHLT)
        g.shaped(item("hbm_m:machine_transformer"), 1, p("SCS", "MDM", "SCS"), 'S', ore("oredict/ingot/iron"), 'D', ore("oredict/ingot/mingrade"), 'M', Ingredient.of(item("hbm_m:coil_copper")), 'C', Ingredient.of(item("hbm_m:capacitor")));
        // CraftingManager.java:610 (ABW)
        g.shaped(item("hbm_m:radiobox"), 1, p("PLP", "PSP", "PLP"), 'P', ore("oredict/plate/steel"), 'S', Ingredient.of(item("hbm_m:ring_starmetal")), 'L', ore("oredict/plate/dura_steel"));
        // CraftingManager.java:611 (ABW)
        g.shaped(item("hbm_m:radiorec"), 1, p("  W", "PCP", "PIP"), 'W', ore("oredict/wire_fine/copper"), 'P', ore("oredict/plate/steel"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'I', ore("oredict/ingot/any_plastic"));
        // CraftingManager.java:614 (FEHLT)
        g.shaped(item("hbm_m:vent_chlorine"), 1, p("IGI", "ICI", "IDI"), 'I', ore("oredict/plate/iron"), 'G', Ingredient.of(item("minecraft:iron_bars")), 'C', Ingredient.of(item("hbm_m:pellet_gas")), 'D', Ingredient.of(item("minecraft:dispenser")));
        // CraftingManager.java:615 (FEHLT)
        g.shaped(item("hbm_m:vent_chlorine_seal"), 1, p("ISI", "SCS", "ISI"), 'I', ore("oredict/ingot/saturnite"), 'S', ore("oredict/ingot/starmetal"), 'C', Ingredient.of(item("hbm_m:chlorine_pinwheel")));
        // CraftingManager.java:616 (FEHLT)
        g.shaped(item("hbm_m:spikes"), 4, p("BBB", "BBB", "TTT"), 'B', ore("oredict/bolt/steel"), 'T', ore("oredict/ingot/steel"));
        // CraftingManager.java:617 (FEHLT)
        g.shaped(item("hbm_m:custom_fall"), 1, p("IIP", "CHW", "IIP"), 'I', ore("oredict/ingot/any_rubber"), 'P', ore("oredict/plate/saturnite"), 'C', Ingredient.of(item("hbm_m:advanced_circuit")), 'H', ore("oredict/shell/steel"), 'W', Ingredient.of(item("hbm_m:coil_copper")));
        // CraftingManager.java:618 (FEHLT)
        g.shaped(item("hbm_m:machine_controller"), 1, p("TDT", "DCD", "TDT"), 'T', ore("oredict/ingot/any_resistant_alloy"), 'D', Ingredient.of(item("hbm_m:crt_display")), 'C', Ingredient.of(item("hbm_m:advanced_circuit")));
        // CraftingManager.java:619 (FEHLT)
        g.shaped(item("hbm_m:containment_box"), 1, p("LUL", "UCU", "LUL"), 'L', ore("oredict/plate/lead"), 'U', ore("oredict/ingot/ferrouranium"), 'C', Ingredient.of(item("hbm_m:crate_steel")));
        // CraftingManager.java:620 (FEHLT)
        g.shaped(item("hbm_m:casing_bag"), 1, p(" L ", "LGL", " L "), 'L', Ingredient.of(item("minecraft:leather")), 'G', ore("oredict/plate/gun_metal"));
        // CraftingManager.java:621 (FEHLT)
        g.shaped(item("hbm_m:casing_bag"), 1, p(" L ", "LGL", " L "), 'L', ore("oredict/ingot/any_rubber"), 'G', ore("oredict/plate/gun_metal"));
        // CraftingManager.java:622 (FEHLT)
        g.shaped(item("hbm_m:ammo_bag"), 1, p("LLL", "MGM", "LLL"), 'L', Ingredient.of(item("minecraft:leather")), 'G', ore("oredict/plate/weapon_steel"), 'M', ore("oredict/gun_mechanism/weapon_steel"));
        // CraftingManager.java:623 (FEHLT)
        g.shaped(item("hbm_m:ammo_bag"), 1, p("LLL", "MGM", "LLL"), 'L', ore("oredict/ingot/any_rubber"), 'G', ore("oredict/plate/weapon_steel"), 'M', ore("oredict/gun_mechanism/weapon_steel"));
        // CraftingManager.java:631 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:pink_planks"), 4, Ingredient.of(item("hbm_m:pink_log")));
        // CraftingManager.java:635 (ABW)
        g.shaped(item("hbm_m:cargo_elevator"), 3, p("GGG", "SPS"), 'G', Ingredient.of(item("hbm_m:steel_grate")), 'S', ore("oredict/ingot/steel"), 'P', Ingredient.of(item("hbm_m:part_generic_piston_hydraulic")));
        // CraftingManager.java:637 (ABW)
        g.shaped(item("hbm_m:door_metal"), 1, p("II", "SS", "II"), 'I', ore("oredict/plate/iron"), 'S', ore("oredict/plate/steel"));
        // CraftingManager.java:638 (ABW)
        g.shaped(item("hbm_m:door_office"), 1, p("II", "SS", "II"), 'I', ore("oredict/plank_wood"), 'S', ore("oredict/plate/iron"));
        // CraftingManager.java:639 (ABW)
        g.shaped(item("hbm_m:door_bunker"), 1, p("II", "SS", "II"), 'I', ore("oredict/plate/steel"), 'S', ore("oredict/plate/lead"));
        // CraftingManager.java:665 (FEHLT)
        g.shaped(item("minecraft:torch"), 3, p("L", "S"), 'L', ore("oredict/gem/lignite"), 'S', ore("oredict/stick_wood"));
        // CraftingManager.java:666 (FEHLT)
        g.shaped(item("minecraft:torch"), 8, p("L", "S"), 'L', ore("oredict/gem/any_coke"), 'S', ore("oredict/stick_wood"));
        // CraftingManager.java:668 (ABW)
        g.shaped(item("hbm_m:machine_missile_assembly"), 1, p("PWP", "SSS", "CCC"), 'P', Ingredient.of(item("hbm_m:pedestal_steel")), 'W', Ingredient.of(item("hbm_m:wrench")), 'S', ore("oredict/plate/steel"), 'C', Ingredient.of(item("hbm_m:steel_scaffold")));
        // CraftingManager.java:669 (FEHLT)
        g.shaped(item("hbm_m:struct_launcher"), 8, p("PPP", "SDS", "CCC"), 'P', ore("oredict/plate/steel"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'D', ore("oredict/ntmpipe/steel"), 'C', ore("oredict/any/concrete"));
    }

    private static void part5(CraftingGen g) {
        // CraftingManager.java:670 (FEHLT)
        g.shaped(item("hbm_m:struct_scaffold"), 8, p("SSS", "DCD", "SSS"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'D', Ingredient.of(item("hbm_m:fluid_duct"), item("hbm_m:fluid_duct_colored"), item("hbm_m:fluid_duct_silver")), 'C', Ingredient.of(item("hbm_m:red_cable")));
        // CraftingManager.java:672 (FEHLT)
        g.shaped(item("hbm_m:seg_10"), 1, p("P", "S", "B"), 'P', ore("oredict/plate/aluminum"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'B', Ingredient.of(item("hbm_m:steel_beam")));
        // CraftingManager.java:673 (FEHLT)
        g.shaped(item("hbm_m:seg_15"), 1, p("PP", "SS", "BB"), 'P', ore("oredict/plate/titanium"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'B', Ingredient.of(item("hbm_m:steel_beam")));
        // CraftingManager.java:674 (FEHLT)
        g.shaped(item("hbm_m:seg_20"), 1, p("PGP", "SSS", "BBB"), 'P', ore("oredict/plate/steel"), 'G', ore("oredict/plate/gold"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'B', Ingredient.of(item("hbm_m:steel_beam")));
        // CraftingManager.java:678 (FEHLT)
        g.shaped(item("hbm_m:fence_metal"), 6, p("BIB", "BIB"), 'B', Ingredient.of(item("minecraft:iron_bars")), 'I', Ingredient.of(item("minecraft:iron_ingot")));
        // CraftingManager.java:680 (FEHLT)
        g.shapeless(item("hbm_m:fence_metal"), 1, Ingredient.of(item("hbm_m:fence_metal_post")));
        // CraftingManager.java:679 (FEHLT)
        g.shapeless(item("hbm_m:fence_metal_post"), 1, Ingredient.of(item("hbm_m:fence_metal")));
        // CraftingManager.java:682 (FEHLT)
        g.shapeless(item("hbm_m:waste_trinitite"), 1, Ingredient.of(item("minecraft:sand")), Ingredient.of(item("hbm_m:trinitite")));
        // CraftingManager.java:683 (FEHLT)
        g.shapeless(item("hbm_m:waste_trinitite_red"), 1, Ingredient.of(item("minecraft:red_sand")), Ingredient.of(item("hbm_m:trinitite")));
        // CraftingManager.java:684 (FEHLT)
        g.shapeless(item("hbm_m:sand_uranium"), 8, ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/dust/uranium"));
        // CraftingManager.java:685 (FEHLT)
        g.shapeless(item("hbm_m:sand_polonium"), 8, ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/dust/polonium210"));
        // CraftingManager.java:686 (FEHLT)
        g.shapeless(item("hbm_m:sand_boron"), 8, ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/dust/boron"));
        // CraftingManager.java:687 (FEHLT)
        g.shapeless(item("hbm_m:sand_lead"), 8, ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/sand"), ore("oredict/dust/lead"));
        // CraftingManager.java:688 (FEHLT)
        g.shapeless(item("hbm_m:sand_quartz"), 1, ore("oredict/sand"), ore("oredict/sand"), ore("oredict/dust/nether_quartz"), ore("oredict/dust/nether_quartz"));
        // CraftingManager.java:690 (FEHLT)
        g.shaped(item("hbm_m:rune_blank"), 1, p("PSP", "SDS", "PSP"), 'P', Ingredient.of(item("hbm_m:magic_powder")), 'S', ore("oredict/ingot/starmetal"), 'D', Ingredient.of(item("hbm_m:bismoid_circuit")));
        // CraftingManager.java:691 (FEHLT)
        g.shapeless(item("hbm_m:rune_isa"), 1, Ingredient.of(item("hbm_m:rune_blank")), Ingredient.of(item("hbm_m:spark_mix_powder")), Ingredient.of(item("hbm_m:singularity_counter_resonant")));
        // CraftingManager.java:692 (FEHLT)
        g.shapeless(item("hbm_m:rune_dagaz"), 1, Ingredient.of(item("hbm_m:rune_blank")), Ingredient.of(item("hbm_m:spark_mix_powder")), Ingredient.of(item("hbm_m:singularity")));
        // CraftingManager.java:693 (FEHLT)
        g.shapeless(item("hbm_m:rune_hagalaz"), 1, Ingredient.of(item("hbm_m:rune_blank")), Ingredient.of(item("hbm_m:spark_mix_powder")), Ingredient.of(item("hbm_m:singularity_super_heated")));
        // CraftingManager.java:694 (FEHLT)
        g.shapeless(item("hbm_m:rune_jera"), 1, Ingredient.of(item("hbm_m:rune_blank")), Ingredient.of(item("hbm_m:spark_mix_powder")), Ingredient.of(item("hbm_m:singularity_spark")));
        // CraftingManager.java:695 (FEHLT)
        g.shapeless(item("hbm_m:rune_thurisaz"), 1, Ingredient.of(item("hbm_m:rune_blank")), Ingredient.of(item("hbm_m:spark_mix_powder")), Ingredient.of(item("hbm_m:black_hole")));
        // CraftingManager.java:696 (FEHLT)
        g.shaped(item("hbm_m:ams_lens"), 1, p("PDP", "GDG", "PDP"), 'P', Ingredient.of(item("hbm_m:plate_dineutronium")), 'G', Ingredient.of(item("hbm_m:reinforced_glass")), 'D', Ingredient.of(item("minecraft:diamond_block")));
        // CraftingManager.java:697 (FEHLT)
        g.shaped(item("hbm_m:ams_catalyst_blank"), 1, p("TET", "ETE", "TET"), 'T', ore("oredict/dust/tennessine"), 'E', ore("oredict/ingot/euphemium"));
        // CraftingManager.java:698 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_lithium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_jera")), ore("oredict/dust/lithium"), ore("oredict/dust/lithium"), ore("oredict/dust/lithium"), ore("oredict/dust/lithium"));
        // CraftingManager.java:699 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_beryllium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_jera")), ore("oredict/dust/beryllium"), ore("oredict/dust/beryllium"), ore("oredict/dust/beryllium"), ore("oredict/dust/beryllium"));
        // CraftingManager.java:700 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_copper"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_jera")), ore("oredict/dust/copper"), ore("oredict/dust/copper"), ore("oredict/dust/copper"), ore("oredict/dust/copper"));
        // CraftingManager.java:701 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_cobalt"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_jera")), ore("oredict/dust/cobalt"), ore("oredict/dust/cobalt"), ore("oredict/dust/cobalt"), ore("oredict/dust/cobalt"));
        // CraftingManager.java:702 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_tungsten"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_jera")), ore("oredict/dust/tungsten"), ore("oredict/dust/tungsten"), ore("oredict/dust/tungsten"), ore("oredict/dust/tungsten"));
        // CraftingManager.java:703 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_aluminium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/aluminum"), ore("oredict/dust/aluminum"), ore("oredict/dust/aluminum"), ore("oredict/dust/aluminum"));
        // CraftingManager.java:704 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_iron"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/iron"), ore("oredict/dust/iron"), ore("oredict/dust/iron"), ore("oredict/dust/iron"));
        // CraftingManager.java:705 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_strontium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/strontium"), ore("oredict/dust/strontium"), ore("oredict/dust/strontium"), ore("oredict/dust/strontium"));
        // CraftingManager.java:706 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_niobium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/niobium"), ore("oredict/dust/niobium"), ore("oredict/dust/niobium"), ore("oredict/dust/niobium"));
        // CraftingManager.java:707 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_cerium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_jera")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/cerium"), ore("oredict/dust/cerium"), ore("oredict/dust/cerium"), ore("oredict/dust/cerium"));
        // CraftingManager.java:708 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_caesium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_thurisaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/caesium"), ore("oredict/dust/caesium"), ore("oredict/dust/caesium"), ore("oredict/dust/caesium"));
        // CraftingManager.java:709 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_thorium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_isa")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/thorium232"), ore("oredict/dust/thorium232"), ore("oredict/dust/thorium232"), ore("oredict/dust/thorium232"));
        // CraftingManager.java:710 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_euphemium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/euphemium"), ore("oredict/dust/euphemium"), ore("oredict/dust/euphemium"), ore("oredict/dust/euphemium"));
        // CraftingManager.java:711 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_schrabidium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_dagaz")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/schrabidium"), ore("oredict/dust/schrabidium"), ore("oredict/dust/schrabidium"), ore("oredict/dust/schrabidium"));
        // CraftingManager.java:712 (FEHLT)
        g.shapeless(item("hbm_m:ams_catalyst_dineutronium"), 1, Ingredient.of(item("hbm_m:ams_catalyst_blank")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_hagalaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), Ingredient.of(item("hbm_m:rune_thurisaz")), ore("oredict/dust/dineutronium"), ore("oredict/dust/dineutronium"), ore("oredict/dust/dineutronium"), ore("oredict/dust/dineutronium"));
        // CraftingManager.java:713 (ABW)
        g.shaped(item("hbm_m:barrel_plastic"), 1, p("IPI", "I I", "IPI"), 'I', Ingredient.of(item("hbm_m:plate_polymer")), 'P', ore("oredict/plate/aluminum"));
        // CraftingManager.java:716 (ABW)
        g.shaped(item("hbm_m:barrel_antimatter"), 1, p("IPI", "I I", "IPI"), 'I', ore("oredict/plate/saturnite"), 'P', ore("oredict/wire_dense/bscco"));
        // CraftingManager.java:717 (FEHLT)
        g.shaped(item("hbm_m:tesla"), 1, p("CCC", "PIP", "WTW"), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'I', ore("oredict/ingot/iron"), 'P', ore("oredict/ingot/any_plastic"), 'T', Ingredient.of(item("hbm_m:machine_transformer")), 'W', ore("oredict/plank_wood"));
        // CraftingManager.java:719 (FEHLT)
        g.shaped(item("hbm_m:catalytic_converter"), 1, p("PCP", "PBP", "PCP"), 'P', ore("oredict/ingot/any_hard_plastic"), 'C', ore("oredict/dust/cobalt"), 'B', ore("oredict/ingot/any_bismoid"));
        // CraftingManager.java:721 (FEHLT)
        g.shaped(item("hbm_m:upgrade_nullifier"), 1, p("SPS", "PUP", "SPS"), 'S', ore("oredict/plate/steel"), 'P', Ingredient.of(item("hbm_m:fire_powder")), 'U', Ingredient.of(item("hbm_m:upgrade_template")));
        // CraftingManager.java:725 (ABW)
        g.shaped(item("hbm_m:upgrade_crystallizer"), 1, p("PHP", "CUC", "DTD"), 'P', filled("hbm_m:fluid_barrel_full", ModFluids.PEROXIDE.getSource()), 'H', Ingredient.of(item("hbm_m:advanced_circuit")), 'C', Ingredient.of(item("hbm_m:barrel_steel")), 'U', Ingredient.of(item("hbm_m:upgrade_centrifuge")), 'D', Ingredient.of(item("hbm_m:motor")), 'T', Ingredient.of(item("hbm_m:machine_transformer")));
        // CraftingManager.java:729 (FEHLT)
        g.shaped(item("hbm_m:upgrade_stack_1"), 1, p(" C ", "PUP", " C "), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'P', Ingredient.of(item("hbm_m:part_generic_piston_pneumatic")), 'U', Ingredient.of(item("hbm_m:upgrade_template")));
        // CraftingManager.java:730 (FEHLT)
        g.shaped(item("hbm_m:upgrade_stack_2"), 1, p(" C ", "PUP", " C "), 'C', Ingredient.of(item("hbm_m:capacitor")), 'P', Ingredient.of(item("hbm_m:part_generic_piston_hydraulic")), 'U', Ingredient.of(item("hbm_m:upgrade_stack_1")));
        // CraftingManager.java:731 (FEHLT)
        g.shaped(item("hbm_m:upgrade_stack_3"), 1, p(" C ", "PUP", " C "), 'C', Ingredient.of(item("hbm_m:microchip")), 'P', Ingredient.of(item("hbm_m:part_generic_piston_electric")), 'U', Ingredient.of(item("hbm_m:upgrade_stack_2")));
        // CraftingManager.java:732 (FEHLT)
        g.shaped(item("hbm_m:upgrade_ejector_1"), 1, p(" C ", "PUP", " C "), 'C', Ingredient.of(item("hbm_m:plate_copper")), 'P', Ingredient.of(item("hbm_m:motor")), 'U', Ingredient.of(item("hbm_m:upgrade_template")));
        // CraftingManager.java:733 (FEHLT)
        g.shaped(item("hbm_m:upgrade_ejector_2"), 1, p(" C ", "PUP", " C "), 'C', Ingredient.of(item("hbm_m:plate_gold")), 'P', Ingredient.of(item("hbm_m:motor")), 'U', Ingredient.of(item("hbm_m:upgrade_ejector_1")));
        // CraftingManager.java:734 (FEHLT)
        g.shaped(item("hbm_m:upgrade_ejector_3"), 1, p(" C ", "PUP", " C "), 'C', Ingredient.of(item("hbm_m:plate_saturnite")), 'P', Ingredient.of(item("hbm_m:motor")), 'U', Ingredient.of(item("hbm_m:upgrade_ejector_2")));
        // CraftingManager.java:736 (FEHLT)
        g.shaped(item("hbm_m:mech_key"), 1, p("MCM", "MKM", "MMM"), 'M', Ingredient.of(item("hbm_m:meteorite_forged_ingot")), 'C', Ingredient.of(item("hbm_m:coin_maskman")), 'K', Ingredient.of(item("hbm_m:key")));
        // CraftingManager.java:737 (FEHLT)
        g.shaped(item("hbm_m:spawn_ufo"), 1, p("MMM", "DCD", "MMM"), 'M', Ingredient.of(item("hbm_m:meteorite_ingot")), 'D', ore("oredict/ingot/dineutronium"), 'C', Ingredient.of(item("hbm_m:coin_worm")));
        // CraftingManager.java:748 (ABW)
        g.shaped(item("hbm_m:fireworks"), 1, p("PPP", "PPP", "WIW"), 'P', Ingredient.of(item("minecraft:paper")), 'W', ore("oredict/plank_wood"), 'I', ore("oredict/ingot/iron"));
        // CraftingManager.java:749 (FEHLT)
        g.shaped(item("hbm_m:safety_fuse"), 8, p("SSS", "SGS", "SSS"), 'S', Ingredient.of(item("minecraft:string")), 'G', Ingredient.of(item("minecraft:gunpowder")));
        // CraftingManager.java:767 (ABW)
        g.shaped(item("hbm_m:rbmk_element_reasim_mod"), 1, p("BGB", "GRG", "BGB"), 'G', ore("oredict/block/graphite"), 'R', Ingredient.of(item("hbm_m:rbmk_element_reasim")), 'B', ore("oredict/ingot/any_resistant_alloy"));
        // CraftingManager.java:784 (FEHLT)
        g.shaped(item("hbm_m:rtty_pager"), 1, p("R", "C", "S"), 'R', Ingredient.of(item("hbm_m:radio_torch_receiver")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'S', ore("oredict/plate/steel"));
        // CraftingManager.java:791 (FEHLT)
        g.shaped(item("hbm_m:ladder_sturdy"), 8, p("LLL", "L#L", "LLL"), 'L', Ingredient.of(item("minecraft:ladder")), '#', ore("oredict/plank_wood"));
        // CraftingManager.java:792 (FEHLT)
        g.shaped(item("hbm_m:ladder_gold"), 8, p("LLL", "L#L", "LLL"), 'L', Ingredient.of(item("minecraft:ladder")), '#', ore("oredict/ingot/gold"));
        // CraftingManager.java:793 (FEHLT)
        g.shaped(item("hbm_m:ladder_copper"), 8, p("LLL", "L#L", "LLL"), 'L', Ingredient.of(item("minecraft:ladder")), '#', ore("oredict/ingot/copper"));
        // CraftingManager.java:794 (FEHLT)
        g.shaped(item("hbm_m:ladder_titanium"), 8, p("LLL", "L#L", "LLL"), 'L', Ingredient.of(item("minecraft:ladder")), '#', ore("oredict/ingot/titanium"));
        // CraftingManager.java:795 (FEHLT)
        g.shaped(item("hbm_m:ladder_steel"), 8, p("LLL", "L#L", "LLL"), 'L', Ingredient.of(item("minecraft:ladder")), '#', ore("oredict/ingot/steel"));
    }

    private static void part6(CraftingGen g) {
        // CraftingManager.java:797 (FEHLT)
        g.shapeless(item("hbm_m:trapdoor_steel"), 1, Ingredient.of(item("minecraft:oak_trapdoor")), ore("oredict/ingot/steel"));
        // CraftingManager.java:801 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe"), 6, p("PP"), 'P', ore("oredict/ntmpipe/steel"));
        // CraftingManager.java:802 (FEHLT)
        g.shapeless(item("hbm_m:deco_pipe"), 1, Ingredient.of(item("hbm_m:deco_pipe_rim")));
        // CraftingManager.java:803 (FEHLT)
        g.shapeless(item("hbm_m:deco_pipe"), 1, Ingredient.of(item("hbm_m:deco_pipe_framed")));
        // CraftingManager.java:804 (FEHLT)
        g.shapeless(item("hbm_m:deco_pipe"), 1, Ingredient.of(item("hbm_m:deco_pipe_quad")));
        // CraftingManager.java:806 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rim"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe")), 'C', ore("oredict/plate/steel"));
        // CraftingManager.java:807 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_quad"), 4, p("PP", "PP"), 'P', Ingredient.of(item("hbm_m:deco_pipe")));
        // CraftingManager.java:808 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe")), 'C', Ingredient.of(item("minecraft:iron_bars")));
        // CraftingManager.java:809 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_rim")), 'C', Ingredient.of(item("minecraft:iron_bars")));
        // CraftingManager.java:811 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:812 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rim_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_rim")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:813 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_quad_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_quad")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:814 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_framed")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:815 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_green"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:816 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rim_green"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_rim")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:817 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_quad_green"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_quad")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:818 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed_green"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_framed")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:819 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_green_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_green")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:820 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rim_green_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_rim_green")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:821 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_quad_green_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_quad_green")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:822 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed_green_rusted"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_framed_green")), 'C', ore("oredict/dust/iron"));
        // CraftingManager.java:823 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_red"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe")), 'C', ore("oredict/dye_red"));
        // CraftingManager.java:824 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rim_red"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_rim")), 'C', ore("oredict/dye_red"));
        // CraftingManager.java:825 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_quad_red"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_quad")), 'C', ore("oredict/dye_red"));
        // CraftingManager.java:826 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed_red"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_framed")), 'C', ore("oredict/dye_red"));
        // CraftingManager.java:827 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_marked"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_green")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:828 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_rim_marked"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_rim_green")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:829 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_quad_marked"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_quad_green")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:830 (FEHLT)
        g.shaped(item("hbm_m:deco_pipe_framed_marked"), 8, p("PPP", "PCP", "PPP"), 'P', Ingredient.of(item("hbm_m:deco_pipe_framed_green")), 'C', ore("oredict/dye_green"));
        // CraftingManager.java:834 (FEHLT)
        g.shaped(item("minecraft:name_tag"), 1, p("SB ", "BPB", " BP"), 'S', Ingredient.of(item("minecraft:string")), 'B', ore("oredict/slimeball"), 'P', Ingredient.of(item("minecraft:paper")));
        // CraftingManager.java:835 (FEHLT)
        g.shaped(item("minecraft:name_tag"), 1, p("SB ", "BPB", " BP"), 'S', Ingredient.of(item("minecraft:string")), 'B', ore("oredict/any/tar"), 'P', Ingredient.of(item("minecraft:paper")));
        // CraftingManager.java:836 (FEHLT)
        g.shaped(item("minecraft:lead"), 4, p("RSR"), 'R', Ingredient.of(item("hbm_m:plant_item_rope")), 'S', ore("oredict/slimeball"));
        // CraftingManager.java:837 (FEHLT)
        g.shaped(item("hbm_m:rag"), 4, p("SW", "WS"), 'S', Ingredient.of(item("minecraft:string")), 'W', Ingredient.of(item("minecraft:white_wool")));
        // CraftingManager.java:847 (FEHLT)
        g.shaped(item("hbm_m:charger"), 1, p("G", "S", "C"), 'G', Ingredient.of(item("minecraft:glowstone_dust")), 'S', ore("oredict/ingot/steel"), 'C', Ingredient.of(item("hbm_m:coil_copper")));
        // CraftingManager.java:848 (FEHLT)
        g.shaped(item("hbm_m:charger"), 16, p("G", "S", "C"), 'G', Ingredient.of(item("minecraft:glowstone")), 'S', ore("oredict/block/steel"), 'C', Ingredient.of(item("hbm_m:coil_copper_torus")));
        // CraftingManager.java:851 (FEHLT)
        g.shaped(item("hbm_m:fluid_identifier_multi"), 1, p("D", "C", "P"), 'D', ore("oredict/dye"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'P', ore("oredict/plate/iron"));
        // CraftingManager.java:854 (FEHLT)
        g.shaped(item("hbm_m:teleanchor"), 1, p("ODO", "EAE", "ODO"), 'O', Ingredient.of(item("minecraft:obsidian")), 'D', ore("oredict/gem/diamond"), 'E', Ingredient.of(item("hbm_m:magic_powder")), 'A', Ingredient.of(item("hbm_m:gem_alexandrite")));
        // CraftingManager.java:855 (FEHLT)
        g.shaped(item("hbm_m:field_disturber"), 1, p("ICI", "CAC", "ICI"), 'I', ore("oredict/ingot/starmetal"), 'C', Ingredient.of(item("hbm_m:bismoid_circuit")), 'A', Ingredient.of(item("hbm_m:gem_alexandrite")));
        // CraftingManager.java:860 (FEHLT)
        g.shaped(item("hbm_m:part_generic_piston_pneumatic"), 4, p(" I ", "CPC", " I "), 'I', ore("oredict/ingot/iron"), 'C', ore("oredict/ingot/copper"), 'P', ore("oredict/plate/iron"));
        // CraftingManager.java:862 (FEHLT)
        g.shaped(item("hbm_m:part_generic_piston_electric"), 4, p(" I ", "CPC", " I "), 'I', ore("oredict/ingot/any_resistant_alloy"), 'C', ore("oredict/ingot/any_plastic"), 'P', Ingredient.of(item("hbm_m:motor")));
        // CraftingManager.java:881 (ABW)
        g.shaped(item("hbm_m:crane_boxer"), 1, p("WWW", "WPW", "CCC"), 'W', ore("oredict/plank_wood"), 'P', Ingredient.of(item("hbm_m:part_generic_piston_pneumatic")), 'C', Ingredient.of(item("hbm_m:conveyor_wand_regular")));
        // CraftingManager.java:888 (ABW)
        g.shaped(item("hbm_m:radar_screen"), 1, p("PCP", "SRS", "PCP"), 'P', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'S', ore("oredict/plate/steel"), 'R', Ingredient.of(item("hbm_m:crt_display")));
        // CraftingManager.java:889 (FEHLT)
        g.shaped(item("hbm_m:radar_linker"), 1, p("S", "C", "P"), 'S', Ingredient.of(item("hbm_m:crt_display")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'P', ore("oredict/plate/steel"));
        // CraftingManager.java:891 (FEHLT)
        g.shaped(item("hbm_m:drone_patrol"), 2, p(" P ", "HCH", " B "), 'P', ore("oredict/ingot/any_plastic"), 'H', ore("oredict/ntmpipe/steel"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'B', ore("oredict/shell/steel"));
        // CraftingManager.java:896 (FEHLT)
        g.shapeless(item("hbm_m:drone_patrol"), 1, Ingredient.of(item("hbm_m:drone_patrol_chunkloading")));
        // CraftingManager.java:892 (FEHLT)
        g.shaped(item("hbm_m:drone_patrol_chunkloading"), 1, p("E", "D"), 'E', Ingredient.of(item("minecraft:ender_pearl")), 'D', Ingredient.of(item("hbm_m:drone_patrol")));
        // CraftingManager.java:897 (ABW)
        g.shapeless(item("hbm_m:drone_patrol_express"), 1, Ingredient.of(item("hbm_m:drone_patrol_express_chunkloading")));
        // CraftingManager.java:894 (ABW)
        g.shaped(item("hbm_m:drone_patrol_express_chunkloading"), 1, p("E", "D"), 'E', Ingredient.of(item("minecraft:ender_pearl")), 'D', Ingredient.of(item("hbm_m:drone_patrol_express")));
        // CraftingManager.java:898 (FEHLT)
        g.shaped(item("hbm_m:drone_request"), 1, p("E", "D"), 'E', Ingredient.of(item("hbm_m:microchip")), 'D', Ingredient.of(item("hbm_m:drone_patrol")));
        // CraftingManager.java:900 (FEHLT)
        g.shaped(item("hbm_m:drone_linker"), 1, p("T", "C"), 'T', Ingredient.of(item("hbm_m:drone_waypoint")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")));
        // CraftingManager.java:901 (ABW)
        g.shaped(item("hbm_m:drone_waypoint"), 4, p("G", "T", "C"), 'G', ore("oredict/dye_green"), 'T', Ingredient.of(item("minecraft:redstone_torch")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")));
        // CraftingManager.java:903 (ABW)
        g.shaped(item("hbm_m:drone_waypoint_request"), 4, p("G", "T", "C"), 'G', ore("oredict/dye_blue"), 'T', Ingredient.of(item("minecraft:redstone_torch")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")));
        // CraftingManager.java:904 (ABW)
        g.shaped(item("hbm_m:drone_crate_requester"), 1, p("T", "C", "B"), 'T', Ingredient.of(item("hbm_m:drone_waypoint_request")), 'C', Ingredient.of(item("hbm_m:crate_steel")), 'B', ore("oredict/dye_yellow"));
        // CraftingManager.java:905 (ABW)
        g.shaped(item("hbm_m:drone_crate_provider"), 1, p("T", "C", "B"), 'T', Ingredient.of(item("hbm_m:drone_waypoint_request")), 'C', Ingredient.of(item("hbm_m:crate_steel")), 'B', ore("oredict/dye_orange"));
        // CraftingManager.java:906 (ABW)
        g.shaped(item("hbm_m:drone_dock"), 1, p("T", "C", "B"), 'T', Ingredient.of(item("hbm_m:drone_waypoint_request")), 'C', Ingredient.of(item("hbm_m:crate_steel")), 'B', Ingredient.of(item("hbm_m:advanced_circuit")));
        // CraftingManager.java:908 (FEHLT)
        g.shaped(item("hbm_m:ball_resin"), 1, p("DD", "DD"), 'D', Ingredient.of(item("minecraft:dandelion")));
        // CraftingManager.java:910 (FEHLT)
        g.shapeless(item("hbm_m:parts_legendary_tier1"), 1, Ingredient.of(item("hbm_m:chainsteel_ingot")), ore("oredict/ingot/asbestos"), Ingredient.of(item("hbm_m:gem_alexandrite")));
        // CraftingManager.java:911 (FEHLT)
        g.shapeless(item("hbm_m:parts_legendary_tier1"), 3, Ingredient.of(item("hbm_m:parts_legendary_tier2")));
        // CraftingManager.java:912 (FEHLT)
        g.shapeless(item("hbm_m:parts_legendary_tier2"), 1, Ingredient.of(item("hbm_m:chainsteel_ingot")), Ingredient.of(item("hbm_m:bismuth_ingot")), Ingredient.of(item("hbm_m:gem_alexandrite")), Ingredient.of(item("hbm_m:gem_alexandrite")));
        // CraftingManager.java:913 (FEHLT)
        g.shapeless(item("hbm_m:parts_legendary_tier2"), 3, Ingredient.of(item("hbm_m:parts_legendary_tier3")));
    }

    private static void part7(CraftingGen g) {
        // CraftingManager.java:914 (FEHLT)
        g.shapeless(item("hbm_m:parts_legendary_tier3"), 1, Ingredient.of(item("hbm_m:chainsteel_ingot")), Ingredient.of(item("hbm_m:smore_ingot")), Ingredient.of(item("hbm_m:gem_alexandrite")), Ingredient.of(item("hbm_m:gem_alexandrite")), Ingredient.of(item("hbm_m:gem_alexandrite")));
        // CraftingManager.java:918 (FEHLT)
        g.shaped(item("hbm_m:sawblade"), 1, p("III", "ICI", "III"), 'I', ore("oredict/plate/steel"), 'C', ore("oredict/ingot/iron"));
        // CraftingManager.java:921 (ABW)
        g.shaped(item("hbm_m:foundry_mold"), 1, p("B B", "BSB"), 'B', Ingredient.of(item("hbm_m:firebrick")), 'S', Ingredient.of(item("minecraft:smooth_stone_slab")));
        // CraftingManager.java:923 (ABW)
        g.shaped(item("hbm_m:foundry_tank"), 1, p("B B", "I I", "BSB"), 'B', Ingredient.of(item("hbm_m:firebrick")), 'I', ore("oredict/ingot/steel"), 'S', Ingredient.of(item("minecraft:smooth_stone_slab")));
        // CraftingManager.java:926 (FEHLT)
        g.shaped(item("hbm_m:mold_base"), 1, p(" B ", "BIB", " B "), 'B', Ingredient.of(item("hbm_m:firebrick")), 'I', ore("oredict/ingot/iron"));
        // CraftingManager.java:927 (FEHLT)
        g.shaped(item("hbm_m:brick_fire"), 1, p("BB", "BB"), 'B', Ingredient.of(item("hbm_m:firebrick")));
        // CraftingManager.java:928 (FEHLT)
        g.shapeless(item("hbm_m:firebrick"), 4, Ingredient.of(item("hbm_m:brick_fire")));
        // PowderRecipes.java:84 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:firebrick"), 4, p("BN", "NB"), 'B', Ingredient.of(item("minecraft:brick")), 'N', Ingredient.of(item("minecraft:nether_brick")));
        // CraftingManager.java:931 (FEHLT)
        g.shaped(item("hbm_m:intake"), 1, p("GGG", "PMP", "PTP"), 'G', Ingredient.of(item("hbm_m:steel_grate")), 'P', ore("oredict/plate/steel"), 'M', Ingredient.of(item("hbm_m:motor")), 'T', Ingredient.of(item("hbm_m:tank_steel")));
        // CraftingManager.java:941 (ABW)
        g.shapeless(item("hbm_m:bdcl"), 1, ore("oredict/any/tar"), container(ModFluids.WATER.getSource(), 1000), ore("oredict/dye_white"));
        // CraftingManager.java:946 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:cordite"), 3, Ingredient.of(item("hbm_m:ballistite")), Ingredient.of(item("minecraft:gunpowder")), Ingredient.of(item("minecraft:white_wool")));
        // CraftingManager.java:947 [nur bei lbsm_crafting] (ABW)
        g.when("lbsm_crafting").shapeless(item("hbm_m:semtex_ingot"), 3, Ingredient.of(item("minecraft:slime_ball")), Ingredient.of(item("minecraft:tnt")), ore("oredict/dust/saltpeter"));
        // CraftingManager.java:950 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:uranium_ore"), 1, Ingredient.of(item("hbm_m:ore_uranium_scorched")), Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:951 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:uranium_ore"), 8, p("OOO", "OBO", "OOO"), 'O', Ingredient.of(item("hbm_m:ore_uranium_scorched")), 'B', Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:956 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:uranium_ore"), 1, Ingredient.of(item("hbm_m:ore_sellafield_uranium_scorched")), Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:957 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:uranium_ore"), 8, p("OOO", "OBO", "OOO"), 'O', Ingredient.of(item("hbm_m:ore_sellafield_uranium_scorched")), 'B', Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:952 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:nether_uranium_ore"), 1, Ingredient.of(item("hbm_m:ore_nether_uranium_scorched")), Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:953 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:nether_uranium_ore"), 8, p("OOO", "OBO", "OOO"), 'O', Ingredient.of(item("hbm_m:ore_nether_uranium_scorched")), 'B', Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:954 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:gneiss_uranium_ore"), 1, Ingredient.of(item("hbm_m:ore_gneiss_uranium_scorched")), Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:955 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:gneiss_uranium_ore"), 8, p("OOO", "OBO", "OOO"), 'O', Ingredient.of(item("hbm_m:ore_gneiss_uranium_scorched")), 'B', Ingredient.of(item("minecraft:water_bucket")));
        // CraftingManager.java:959 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_iron"), 4, p("##", "##"), '#', ore("oredict/ingot/iron"));
        // CraftingManager.java:960 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_gold"), 4, p("##", "##"), '#', ore("oredict/ingot/gold"));
        // CraftingManager.java:961 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_aluminium"), 4, p("##", "##"), '#', ore("oredict/ingot/aluminum"));
        // CraftingManager.java:962 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_titanium"), 4, p("##", "##"), '#', ore("oredict/ingot/titanium"));
        // CraftingManager.java:963 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_copper"), 4, p("##", "##"), '#', ore("oredict/ingot/copper"));
        // CraftingManager.java:964 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_lead"), 4, p("##", "##"), '#', ore("oredict/ingot/lead"));
        // CraftingManager.java:965 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_steel"), 4, p("##", "##"), '#', ore("oredict/ingot/steel"));
        // CraftingManager.java:966 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_schrabidium"), 4, p("##", "##"), '#', ore("oredict/ingot/schrabidium"));
        // CraftingManager.java:967 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_saturnite"), 4, p("##", "##"), '#', ore("oredict/ingot/saturnite"));
        // CraftingManager.java:968 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:plate_combine_steel"), 4, p("##", "##"), '#', ore("oredict/ingot/cmb_steel"));
        // CraftingManager.java:969 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shaped(item("hbm_m:neutron_reflector"), 4, p("##", "##"), '#', ore("oredict/ingot/tungsten"));
        // CraftingManager.java:982 (FEHLT)
        g.shaped(item("hbm_m:struct_launcher_core"), 1, p("SCS", "SIS", "BEB"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'I', Ingredient.of(item("minecraft:iron_bars")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'B', Ingredient.of(item("hbm_m:struct_launcher")), 'E', Ingredient.of(item("hbm_m:battery_pack_battery_lead")));
        // CraftingManager.java:983 (FEHLT)
        g.shaped(item("hbm_m:struct_launcher_core_large"), 1, p("SIS", "ICI", "BEB"), 'S', Ingredient.of(item("hbm_m:advanced_circuit")), 'I', Ingredient.of(item("minecraft:iron_bars")), 'C', Ingredient.of(item("hbm_m:advanced_circuit")), 'B', Ingredient.of(item("hbm_m:struct_launcher")), 'E', Ingredient.of(item("hbm_m:battery_pack_battery_lead")));
        // CraftingManager.java:986 [nur bei !528] (FEHLT)
        g.when("!528").shaped(item("hbm_m:reactor_sensor"), 1, p("WPW", "CMC", "PPP"), 'W', ore("oredict/wire_fine/tungsten"), 'P', ore("oredict/plate/lead"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'M', Ingredient.of(item("hbm_m:magnetron")));
        // CraftingManager.java:992 [nur bei !528] (ABW)
        g.when("!528").shaped(item("hbm_m:rbmk_heater"), 1, p("CIC", "PRP", "CIC"), 'C', ore("oredict/ntmpipe/copper"), 'P', ore("oredict/shell/steel"), 'R', Ingredient.of(item("hbm_m:rbmk_blank")), 'I', ore("oredict/ingot/any_plastic"));
        // CraftingManager.java:996 (FEHLT)
        g.shapeless(item("hbm_m:launch_code"), 1, Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:launch_code_piece")), Ingredient.of(item("hbm_m:advanced_circuit")));
        // ArmorRecipes.java:70 (ABW)
        g.shapeless(item("hbm_m:ajro_helmet"), 1, Ingredient.of(item("hbm_m:ajr_helmet")), ore("oredict/dye_red"), ore("oredict/dye_black"));
        // ArmorRecipes.java:71 (ABW)
        g.shapeless(item("hbm_m:ajro_plate"), 1, Ingredient.of(item("hbm_m:ajr_plate")), ore("oredict/dye_red"), ore("oredict/dye_black"));
        // ArmorRecipes.java:72 (ABW)
        g.shapeless(item("hbm_m:ajro_legs"), 1, Ingredient.of(item("hbm_m:ajr_legs")), ore("oredict/dye_red"), ore("oredict/dye_black"));
        // ArmorRecipes.java:73 (ABW)
        g.shapeless(item("hbm_m:ajro_boots"), 1, Ingredient.of(item("hbm_m:ajr_boots")), ore("oredict/dye_red"), ore("oredict/dye_black"));
        // ArmorRecipes.java:76 (ABW)
        g.shaped(item("hbm_m:bj_plate_jetpack"), 1, p("NFN", "TPT", "ICI"), 'N', Ingredient.of(item("hbm_m:plate_armor_lunar")), 'F', Ingredient.of(item("hbm_m:fins_quad_titanium")), 'T', filled("hbm_m:fluid_tank_full", ModFluids.XENON.getSource()), 'P', Ingredient.of(item("hbm_m:bj_plate")), 'I', Ingredient.of(item("hbm_m:mp_thruster_10_xenon")), 'C', Ingredient.of(item("hbm_m:crystal_phosphorus")));
        // ArmorRecipes.java:121 (ABW)
        g.shaped(item("hbm_m:jetpack_fly"), 1, p("ACA", "TLT", "D D"), 'A', ore("oredict/plate/aluminum"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'T', Ingredient.of(item("hbm_m:tank_steel")), 'L', Ingredient.of(item("minecraft:leather")), 'D', Ingredient.of(item("hbm_m:thruster_small")));
        // ArmorRecipes.java:160 (ABW)
        g.shaped(item("hbm_m:gas_mask_m65"), 1, p("PPP", "GPG", " F "), 'G', ore("oredict/pane_glass"), 'P', ore("oredict/ingot/any_rubber"), 'F', ore("oredict/plate/iron"));
        // ArmorRecipes.java:162 (ABW)
        g.shaped(item("hbm_m:gas_mask_mono"), 1, p(" P ", "PPP", " F "), 'P', ore("oredict/ingot/any_rubber"), 'F', ore("oredict/plate/iron"));
        // ConsumableRecipes.java:34 (FEHLT)
        g.shaped(item("hbm_m:bomb_caller_napalm"), 1, p("TTT", "TRT", "TTT"), 'T', Ingredient.of(item("hbm_m:grenade_filling_inc")), 'R', Ingredient.of(item("hbm_m:rangefinder")));
        // MineralRecipes.java:48 (ABW, 1x1)
        g.shapeless(item("hbm_m:smore_ingot"), 9, Ingredient.of(item("hbm_m:block_smore")));
        // ConsumableRecipes.java:73 (ABW)
        g.shaped(item("hbm_m:can_empty"), 1, p("P", "P"), 'P', ore("oredict/plate/aluminum"));
        // ConsumableRecipes.java:80 (ABW)
        g.shapeless(item("hbm_m:mucho_mango"), 1, Ingredient.of(item("minecraft:potion")), Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("minecraft:sugar")), ore("oredict/dye_orange"));
        // ConsumableRecipes.java:123 (FEHLT)
        g.shapeless(item("hbm_m:cigarette"), 16, ore("oredict/ingot/asbestos"), ore("oredict/any/tar"), ore("oredict/nugget/polonium210"), Ingredient.of(item("hbm_m:plant_item_tobacco")));
        // ConsumableRecipes.java:127 [nur bei lbsm_medicine] (ABW)
        g.when("lbsm_medicine").shapeless(item("hbm_m:siox"), 8, ore("oredict/dust/coal"), ore("oredict/dust/asbestos"), ore("oredict/nugget/gold"));
        // ConsumableRecipes.java:128 [nur bei lbsm_medicine] (ABW)
        g.when("lbsm_medicine").shapeless(item("hbm_m:xanax"), 1, ore("oredict/dust/coal"), ore("oredict/dust/saltpeter"), ore("oredict/dust/nether_quartz"));
        // ConsumableRecipes.java:178 (ABW)
        g.shaped(item("hbm_m:attachment_mask"), 1, p("DID", "IGI", " F "), 'D', Ingredient.of(item("hbm_m:ducttape")), 'I', ore("oredict/ingot/any_rubber"), 'G', ore("oredict/pane_glass"), 'F', ore("oredict/plate/iron"));
        // ConsumableRecipes.java:179 (ABW)
        g.shaped(item("hbm_m:attachment_mask_mono"), 1, p(" D ", "DID", " F "), 'D', Ingredient.of(item("hbm_m:ducttape")), 'I', ore("oredict/ingot/any_rubber"), 'F', ore("oredict/plate/iron"));
        // ConsumableRecipes.java:187 (FEHLT)
        g.shaped(item("hbm_m:armor_battery"), 1, p("PWP", "PCP", "PWP"), 'P', ore("oredict/plate/steel"), 'C', Ingredient.of(item("hbm_m:battery_pack_capacitor_gold")), 'W', ore("oredict/wire_dense/mingrade"));
        // ConsumableRecipes.java:188 (FEHLT)
        g.shaped(item("hbm_m:armor_battery_mk2"), 1, p("PWP", "PCP", "PWP"), 'P', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:battery_pack_capacitor_niobium")), 'W', ore("oredict/wire_dense/mingrade"));
        // ConsumableRecipes.java:189 (FEHLT)
        g.shaped(item("hbm_m:armor_battery_mk3"), 1, p("PWP", "PCP", "PWP"), 'P', ore("oredict/plate/gold"), 'C', Ingredient.of(item("hbm_m:battery_pack_capacitor_tantalum")), 'W', ore("oredict/wire_dense/mingrade"));
        // ConsumableRecipes.java:194 (ABW)
        g.shaped(item("hbm_m:heart_container"), 1, p("HAH", "ACA", "HAH"), 'H', Ingredient.of(item("hbm_m:heart_piece")), 'A', ore("oredict/ingot/aluminum"), 'C', Ingredient.of(item("hbm_m:coin_creeper")));
        // ConsumableRecipes.java:197 (ABW)
        g.shaped(item("hbm_m:ink"), 1, p("FPF", "PIP", "FPF"), 'F', Ingredient.of(item("minecraft:poppy")), 'P', Ingredient.of(item("hbm_m:armor_polish")), 'I', ore("oredict/dye_black"));
        // ConsumableRecipes.java:203 (FEHLT)
        g.shaped(item("hbm_m:shackles"), 1, p("CIC", "C C", "I I"), 'I', Ingredient.of(item("hbm_m:chainsteel_ingot")), 'C', Ingredient.of(item("hbm_m:dungeon_chain")));
        // ConsumableRecipes.java:211 (ABW)
        g.shaped(item("hbm_m:night_vision"), 1, p("P P", "GCG"), 'P', ore("oredict/ingot/any_plastic"), 'G', ore("oredict/block/glass"), 'C', Ingredient.of(item("hbm_m:integrated_circuit")));
    }

    private static void part8(CraftingGen g) {
        // MineralRecipes.java:36 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:coal_powder_tiny"), 9, Ingredient.of(item("hbm_m:coal_powder")));
        // MineralRecipes.java:36 (FEHLT)
        g.shaped(item("hbm_m:coal_powder"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:coal_powder_tiny")));
        // MineralRecipes.java:37 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_mercury_tiny"), 9, Ingredient.of(item("hbm_m:nugget_mercury")));
        // MineralRecipes.java:37 (FEHLT)
        g.shaped(item("hbm_m:nugget_mercury"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_mercury_tiny")));
        // MineralRecipes.java:247 (FEHLT)
        g.shapeless(item("hbm_m:nugget_mercury"), 2, Ingredient.of(item("hbm_m:pellet_rtg_depleted_mercury")));
        // MineralRecipes.java:420 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_mercury"), 8, Ingredient.of(item("hbm_m:bottle_mercury")));
        // MineralRecipes.java:39 (FEHLT)
        g.shaped(item("hbm_m:block_aluminium"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:ingot_aluminium")));
        // MineralRecipes.java:40 (FEHLT)
        g.shaped(item("hbm_m:block_graphite"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:graphite_ingot")));
        // MineralRecipes.java:84 (ABW)
        g.shaped(item("hbm_m:ra226_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_ra226")));
        // MineralRecipes.java:134 (ABW)
        g.shapeless(item("hbm_m:ra226_ingot"), 2, Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_ra226")));
        // MineralRecipes.java:85 (ABW)
        g.shaped(item("hbm_m:actinium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_actinium")));
        // MineralRecipes.java:135 (ABW)
        g.shapeless(item("hbm_m:actinium_ingot"), 2, Ingredient.of(item("hbm_m:billet_actinium")), Ingredient.of(item("hbm_m:billet_actinium")), Ingredient.of(item("hbm_m:billet_actinium")));
        // MineralRecipes.java:47 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:fragment_coltan"), 9, Ingredient.of(item("hbm_m:block_coltan")));
        // MineralRecipes.java:47 (FEHLT)
        g.shaped(item("hbm_m:block_coltan"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:fragment_coltan")));
        // MineralRecipes.java:48 (FEHLT)
        g.shaped(item("hbm_m:block_smore"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:smore_ingot")));
        // MineralRecipes.java:51 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:polymer_ingot"), 9, Ingredient.of(item("hbm_m:block_polymer")));
        // MineralRecipes.java:51 (FEHLT)
        g.shaped(item("hbm_m:block_polymer"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:polymer_ingot")));
        // MineralRecipes.java:52 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:bakelite_ingot"), 9, Ingredient.of(item("hbm_m:block_bakelite")));
        // MineralRecipes.java:52 (FEHLT)
        g.shaped(item("hbm_m:block_bakelite"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:bakelite_ingot")));
        // MineralRecipes.java:53 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:rubber_ingot"), 9, Ingredient.of(item("hbm_m:block_rubber")));
        // MineralRecipes.java:53 (FEHLT)
        g.shaped(item("hbm_m:block_rubber"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:rubber_ingot")));
        // MineralRecipes.java:58 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:ingot_slag"), 9, Ingredient.of(item("hbm_m:block_slag")));
        // MineralRecipes.java:58 (FEHLT)
        g.shaped(item("hbm_m:block_slag"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:ingot_slag")));
        // MineralRecipes.java:64 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_niobium"), 9, Ingredient.of(item("hbm_m:niobium_ingot")));
        // MineralRecipes.java:64 (ABW)
        g.shaped(item("hbm_m:niobium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_niobium")));
        // MineralRecipes.java:65 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_bismuth"), 9, Ingredient.of(item("hbm_m:bismuth_ingot")));
        // MineralRecipes.java:65 (ABW)
        g.shaped(item("hbm_m:bismuth_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_bismuth")));
        // MineralRecipes.java:153 (ABW)
        g.shapeless(item("hbm_m:bismuth_ingot"), 2, Ingredient.of(item("hbm_m:billet_bismuth")), Ingredient.of(item("hbm_m:billet_bismuth")), Ingredient.of(item("hbm_m:billet_bismuth")));
        // MineralRecipes.java:66 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_tantalium"), 9, Ingredient.of(item("hbm_m:tantalium_ingot")));
        // MineralRecipes.java:66 (FEHLT)
        g.shaped(item("hbm_m:tantalium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_tantalium")));
        // MineralRecipes.java:66 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:tantalium_ingot"), 9, Ingredient.of(item("hbm_m:block_tantalium")));
        // MineralRecipes.java:66 (FEHLT)
        g.shaped(item("hbm_m:block_tantalium"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:tantalium_ingot")));
        // MineralRecipes.java:67 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_zirconium"), 9, Ingredient.of(item("hbm_m:zirconium_ingot")));
        // MineralRecipes.java:67 (ABW)
        g.shaped(item("hbm_m:zirconium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_zirconium")));
        // MineralRecipes.java:152 (ABW)
        g.shapeless(item("hbm_m:zirconium_ingot"), 2, Ingredient.of(item("hbm_m:billet_zirconium")), Ingredient.of(item("hbm_m:billet_zirconium")), Ingredient.of(item("hbm_m:billet_zirconium")));
        // MineralRecipes.java:68 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_dineutronium"), 9, Ingredient.of(item("hbm_m:dineutronium_ingot")));
        // MineralRecipes.java:68 (ABW)
        g.shaped(item("hbm_m:dineutronium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_dineutronium")));
        // MineralRecipes.java:69 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nuclear_waste_vitrified_tiny"), 9, Ingredient.of(item("hbm_m:nuclear_waste_vitrified")));
        // MineralRecipes.java:69 (FEHLT)
        g.shaped(item("hbm_m:nuclear_waste_vitrified"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nuclear_waste_vitrified_tiny")));
        // MineralRecipes.java:69 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nuclear_waste_vitrified"), 9, Ingredient.of(item("hbm_m:block_waste_vitrified")));
        // MineralRecipes.java:69 (FEHLT)
        g.shaped(item("hbm_m:block_waste_vitrified"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nuclear_waste_vitrified")));
        // MineralRecipes.java:71 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_silicon"), 9, Ingredient.of(item("hbm_m:silicon_ingot")));
        // MineralRecipes.java:71 (FEHLT)
        g.shaped(item("hbm_m:silicon_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_silicon")));
        // MineralRecipes.java:154 (FEHLT)
        g.shapeless(item("hbm_m:silicon_ingot"), 2, Ingredient.of(item("hbm_m:billet_silicon")), Ingredient.of(item("hbm_m:billet_silicon")), Ingredient.of(item("hbm_m:billet_silicon")));
        // MineralRecipes.java:75 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:xe135_powder_tiny"), 9, Ingredient.of(item("hbm_m:xe135_powder")));
        // MineralRecipes.java:75 (FEHLT)
        g.shaped(item("hbm_m:xe135_powder"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:xe135_powder_tiny")));
        // MineralRecipes.java:76 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:cs137_powder_tiny"), 9, Ingredient.of(item("hbm_m:cs137_powder")));
        // MineralRecipes.java:76 (FEHLT)
        g.shaped(item("hbm_m:cs137_powder"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:cs137_powder_tiny")));
        // MineralRecipes.java:77 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:i131_powder_tiny"), 9, Ingredient.of(item("hbm_m:i131_powder")));
        // MineralRecipes.java:77 (FEHLT)
        g.shaped(item("hbm_m:i131_powder"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:i131_powder_tiny")));
        // MineralRecipes.java:79 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_technetium"), 9, Ingredient.of(item("hbm_m:technetium_ingot")));
        // MineralRecipes.java:79 (FEHLT)
        g.shaped(item("hbm_m:technetium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_technetium")));
        // MineralRecipes.java:131 (FEHLT)
        g.shapeless(item("hbm_m:technetium_ingot"), 2, Ingredient.of(item("hbm_m:billet_technetium")), Ingredient.of(item("hbm_m:billet_technetium")), Ingredient.of(item("hbm_m:billet_technetium")));
        // MineralRecipes.java:80 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_co60"), 9, Ingredient.of(item("hbm_m:co60_ingot")));
        // MineralRecipes.java:80 (FEHLT)
        g.shaped(item("hbm_m:co60_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_co60")));
        // MineralRecipes.java:113 (FEHLT)
        g.shapeless(item("hbm_m:co60_ingot"), 2, Ingredient.of(item("hbm_m:billet_co60")), Ingredient.of(item("hbm_m:billet_co60")), Ingredient.of(item("hbm_m:billet_co60")));
        // MineralRecipes.java:81 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_sr90"), 9, Ingredient.of(item("hbm_m:sr90_ingot")));
        // MineralRecipes.java:81 (FEHLT)
        g.shaped(item("hbm_m:sr90_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_sr90")));
        // MineralRecipes.java:114 (FEHLT)
        g.shapeless(item("hbm_m:sr90_ingot"), 2, Ingredient.of(item("hbm_m:billet_sr90")), Ingredient.of(item("hbm_m:billet_sr90")), Ingredient.of(item("hbm_m:billet_sr90")));
        // MineralRecipes.java:82 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_au198"), 9, Ingredient.of(item("hbm_m:au198_ingot")));
    }

    private static void part9(CraftingGen g) {
        // MineralRecipes.java:82 (FEHLT)
        g.shaped(item("hbm_m:au198_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_au198")));
        // MineralRecipes.java:132 (FEHLT)
        g.shapeless(item("hbm_m:au198_ingot"), 2, Ingredient.of(item("hbm_m:billet_au198")), Ingredient.of(item("hbm_m:billet_au198")), Ingredient.of(item("hbm_m:billet_au198")));
        // MineralRecipes.java:83 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_pb209"), 9, Ingredient.of(item("hbm_m:pb209_ingot")));
        // MineralRecipes.java:83 (FEHLT)
        g.shaped(item("hbm_m:pb209_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_pb209")));
        // MineralRecipes.java:133 (FEHLT)
        g.shapeless(item("hbm_m:pb209_ingot"), 2, Ingredient.of(item("hbm_m:billet_pb209")), Ingredient.of(item("hbm_m:billet_pb209")), Ingredient.of(item("hbm_m:billet_pb209")));
        // MineralRecipes.java:84 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_ra226"), 9, Ingredient.of(item("hbm_m:ra226_ingot")));
        // MineralRecipes.java:85 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_actinium"), 9, Ingredient.of(item("hbm_m:actinium_ingot")));
        // MineralRecipes.java:86 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_arsenic"), 9, Ingredient.of(item("hbm_m:arsenic_ingot")));
        // MineralRecipes.java:86 (FEHLT)
        g.shaped(item("hbm_m:arsenic_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_arsenic")));
        // MineralRecipes.java:88 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_pu241"), 9, Ingredient.of(item("hbm_m:pu241_ingot")));
        // MineralRecipes.java:88 (ABW)
        g.shaped(item("hbm_m:pu241_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_pu241")));
        // MineralRecipes.java:124 (ABW)
        g.shapeless(item("hbm_m:pu241_ingot"), 2, Ingredient.of(item("hbm_m:billet_pu241")), Ingredient.of(item("hbm_m:billet_pu241")), Ingredient.of(item("hbm_m:billet_pu241")));
        // MineralRecipes.java:89 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_am241"), 9, Ingredient.of(item("hbm_m:am241_ingot")));
        // MineralRecipes.java:89 (FEHLT)
        g.shaped(item("hbm_m:am241_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_am241")));
        // MineralRecipes.java:126 (FEHLT)
        g.shapeless(item("hbm_m:am241_ingot"), 2, Ingredient.of(item("hbm_m:billet_am241")), Ingredient.of(item("hbm_m:billet_am241")), Ingredient.of(item("hbm_m:billet_am241")));
        // MineralRecipes.java:90 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_am242"), 9, Ingredient.of(item("hbm_m:am242_ingot")));
        // MineralRecipes.java:90 (FEHLT)
        g.shaped(item("hbm_m:am242_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_am242")));
        // MineralRecipes.java:127 (FEHLT)
        g.shapeless(item("hbm_m:am242_ingot"), 2, Ingredient.of(item("hbm_m:billet_am242")), Ingredient.of(item("hbm_m:billet_am242")), Ingredient.of(item("hbm_m:billet_am242")));
        // MineralRecipes.java:91 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_am_mix"), 9, Ingredient.of(item("hbm_m:am_mix_ingot")));
        // MineralRecipes.java:91 (FEHLT)
        g.shaped(item("hbm_m:am_mix_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_am_mix")));
        // MineralRecipes.java:128 (FEHLT)
        g.shapeless(item("hbm_m:am_mix_ingot"), 2, Ingredient.of(item("hbm_m:billet_am_mix")), Ingredient.of(item("hbm_m:billet_am_mix")), Ingredient.of(item("hbm_m:billet_am_mix")));
        // MineralRecipes.java:449 (FEHLT)
        g.shapeless(item("hbm_m:am_mix_ingot"), 1, ore("oredict/nugget/americium241"), ore("oredict/nugget/americium241"), ore("oredict/nugget/americium241"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"));
        // MineralRecipes.java:450 (FEHLT)
        g.shapeless(item("hbm_m:am_mix_ingot"), 1, ore("oredict/tiny/am241"), ore("oredict/tiny/am241"), ore("oredict/tiny/am241"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"));
        // MineralRecipes.java:92 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_americium_fuel"), 9, Ingredient.of(item("hbm_m:americium_fuel_ingot")));
        // MineralRecipes.java:92 (FEHLT)
        g.shaped(item("hbm_m:americium_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_americium_fuel")));
        // MineralRecipes.java:94 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_gh336"), 9, Ingredient.of(item("hbm_m:gh336_ingot")));
        // MineralRecipes.java:94 (FEHLT)
        g.shaped(item("hbm_m:gh336_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_gh336")));
        // MineralRecipes.java:138 (FEHLT)
        g.shapeless(item("hbm_m:gh336_ingot"), 2, Ingredient.of(item("hbm_m:billet_gh336")), Ingredient.of(item("hbm_m:billet_gh336")), Ingredient.of(item("hbm_m:billet_gh336")));
        // MineralRecipes.java:106 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:fallout"), 9, Ingredient.of(item("hbm_m:block_fallout")));
        // MineralRecipes.java:107 (FEHLT)
        g.shaped(item("hbm_m:fallout"), 2, p("##"), '#', Ingredient.of(item("hbm_m:fallout")));
        // MineralRecipes.java:106 (FEHLT)
        g.shaped(item("hbm_m:block_fallout"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:fallout")));
        // MineralRecipes.java:109 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_pu_mix"), 9, Ingredient.of(item("hbm_m:pu_mix_ingot")));
        // MineralRecipes.java:109 (FEHLT)
        g.shaped(item("hbm_m:pu_mix_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_pu_mix")));
        // MineralRecipes.java:109 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:pu_mix_ingot"), 9, Ingredient.of(item("hbm_m:block_pu_mix")));
        // MineralRecipes.java:125 (FEHLT)
        g.shapeless(item("hbm_m:pu_mix_ingot"), 2, Ingredient.of(item("hbm_m:billet_pu_mix")), Ingredient.of(item("hbm_m:billet_pu_mix")), Ingredient.of(item("hbm_m:billet_pu_mix")));
        // MineralRecipes.java:448 (FEHLT)
        g.shapeless(item("hbm_m:pu_mix_ingot"), 1, ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu240"), ore("oredict/tiny/pu240"), ore("oredict/tiny/pu240"));
        // MineralRecipes.java:109 (FEHLT)
        g.shaped(item("hbm_m:block_pu_mix"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:pu_mix_ingot")));
        // MineralRecipes.java:110 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_neptunium_fuel"), 9, Ingredient.of(item("hbm_m:neptunium_fuel_ingot")));
        // MineralRecipes.java:110 (FEHLT)
        g.shaped(item("hbm_m:neptunium_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_neptunium_fuel")));
        // MineralRecipes.java:142 (FEHLT)
        g.shapeless(item("hbm_m:neptunium_fuel_ingot"), 2, Ingredient.of(item("hbm_m:billet_neptunium_fuel")), Ingredient.of(item("hbm_m:billet_neptunium_fuel")), Ingredient.of(item("hbm_m:billet_neptunium_fuel")));
        // MineralRecipes.java:112 (ABW)
        g.shaped(item("hbm_m:billet_cobalt"), 3, p("##"), '#', Ingredient.of(item("hbm_m:cobalt_ingot")));
        // MineralRecipes.java:112 (ABW)
        g.shapeless(item("hbm_m:cobalt_ingot"), 2, Ingredient.of(item("hbm_m:billet_cobalt")), Ingredient.of(item("hbm_m:billet_cobalt")), Ingredient.of(item("hbm_m:billet_cobalt")));
        // MineralRecipes.java:113 (ABW)
        g.shaped(item("hbm_m:billet_co60"), 3, p("##"), '#', Ingredient.of(item("hbm_m:co60_ingot")));
        // MineralRecipes.java:114 (ABW)
        g.shaped(item("hbm_m:billet_sr90"), 3, p("##"), '#', Ingredient.of(item("hbm_m:sr90_ingot")));
        // MineralRecipes.java:115 (ABW)
        g.shaped(item("hbm_m:billet_uranium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:uranium_ingot")));
        // MineralRecipes.java:201 (ABW)
        g.shapeless(item("hbm_m:billet_uranium"), 2, Ingredient.of(item("hbm_m:billet_uranium_fuel")), Ingredient.of(item("hbm_m:billet_u238")));
        // MineralRecipes.java:202 (ABW)
        g.shapeless(item("hbm_m:billet_uranium"), 2, Ingredient.of(item("hbm_m:billet_u238")), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium235"));
        // MineralRecipes.java:203 (ABW)
        g.shapeless(item("hbm_m:billet_uranium"), 2, Ingredient.of(item("hbm_m:billet_u238")), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u235"));
        // MineralRecipes.java:364 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_uranium"), 9, Ingredient.of(item("hbm_m:uranium_ingot")));
        // MineralRecipes.java:115 (ABW)
        g.shapeless(item("hbm_m:uranium_ingot"), 2, Ingredient.of(item("hbm_m:billet_uranium")), Ingredient.of(item("hbm_m:billet_uranium")), Ingredient.of(item("hbm_m:billet_uranium")));
        // MineralRecipes.java:363 (ABW)
        g.shaped(item("hbm_m:uranium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_uranium")));
        // MineralRecipes.java:116 (ABW)
        g.shaped(item("hbm_m:billet_u233"), 3, p("##"), '#', Ingredient.of(item("hbm_m:u233_ingot")));
        // MineralRecipes.java:366 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_u233"), 9, Ingredient.of(item("hbm_m:u233_ingot")));
        // MineralRecipes.java:116 (ABW)
        g.shapeless(item("hbm_m:u233_ingot"), 2, Ingredient.of(item("hbm_m:billet_u233")), Ingredient.of(item("hbm_m:billet_u233")), Ingredient.of(item("hbm_m:billet_u233")));
        // MineralRecipes.java:365 (ABW)
        g.shaped(item("hbm_m:u233_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_u233")));
        // MineralRecipes.java:117 (ABW)
        g.shaped(item("hbm_m:billet_u235"), 3, p("##"), '#', Ingredient.of(item("hbm_m:u235_ingot")));
        // MineralRecipes.java:368 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_u235"), 9, Ingredient.of(item("hbm_m:u235_ingot")));
        // MineralRecipes.java:117 (ABW)
        g.shapeless(item("hbm_m:u235_ingot"), 2, Ingredient.of(item("hbm_m:billet_u235")), Ingredient.of(item("hbm_m:billet_u235")), Ingredient.of(item("hbm_m:billet_u235")));
        // MineralRecipes.java:367 (ABW)
        g.shaped(item("hbm_m:u235_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_u235")));
        // MineralRecipes.java:118 (ABW)
        g.shaped(item("hbm_m:billet_u238"), 3, p("##"), '#', Ingredient.of(item("hbm_m:u238_ingot")));
    }

    private static void part10(CraftingGen g) {
        // MineralRecipes.java:370 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_u238"), 9, Ingredient.of(item("hbm_m:u238_ingot")));
        // MineralRecipes.java:118 (ABW)
        g.shapeless(item("hbm_m:u238_ingot"), 2, Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")));
        // MineralRecipes.java:369 (ABW)
        g.shaped(item("hbm_m:u238_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_u238")));
        // MineralRecipes.java:119 (ABW)
        g.shaped(item("hbm_m:billet_th232"), 3, p("##"), '#', Ingredient.of(item("hbm_m:th232_ingot")));
        // MineralRecipes.java:362 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_th232"), 9, Ingredient.of(item("hbm_m:th232_ingot")));
        // MineralRecipes.java:119 (FEHLT)
        g.shapeless(item("hbm_m:th232_ingot"), 2, Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_th232")));
        // MineralRecipes.java:315 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:th232_ingot"), 9, Ingredient.of(item("hbm_m:block_thorium")));
        // MineralRecipes.java:361 (FEHLT)
        g.shaped(item("hbm_m:th232_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_th232")));
        // MineralRecipes.java:120 (ABW)
        g.shaped(item("hbm_m:billet_plutonium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:plutonium_ingot")));
        // MineralRecipes.java:354 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_plutonium"), 9, Ingredient.of(item("hbm_m:plutonium_ingot")));
        // MineralRecipes.java:120 (ABW)
        g.shapeless(item("hbm_m:plutonium_ingot"), 2, Ingredient.of(item("hbm_m:billet_plutonium")), Ingredient.of(item("hbm_m:billet_plutonium")), Ingredient.of(item("hbm_m:billet_plutonium")));
        // MineralRecipes.java:353 (ABW)
        g.shaped(item("hbm_m:plutonium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_plutonium")));
        // MineralRecipes.java:121 (ABW)
        g.shaped(item("hbm_m:billet_pu238"), 3, p("##"), '#', Ingredient.of(item("hbm_m:pu238_ingot")));
        // MineralRecipes.java:356 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_pu238"), 9, Ingredient.of(item("hbm_m:pu238_ingot")));
        // MineralRecipes.java:121 (ABW)
        g.shapeless(item("hbm_m:pu238_ingot"), 2, Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_pu238")));
        // MineralRecipes.java:355 (ABW)
        g.shaped(item("hbm_m:pu238_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_pu238")));
        // MineralRecipes.java:122 (ABW)
        g.shaped(item("hbm_m:billet_pu239"), 3, p("##"), '#', Ingredient.of(item("hbm_m:pu239_ingot")));
        // MineralRecipes.java:358 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_pu239"), 9, Ingredient.of(item("hbm_m:pu239_ingot")));
        // MineralRecipes.java:122 (ABW)
        g.shapeless(item("hbm_m:pu239_ingot"), 2, Ingredient.of(item("hbm_m:billet_pu239")), Ingredient.of(item("hbm_m:billet_pu239")), Ingredient.of(item("hbm_m:billet_pu239")));
        // MineralRecipes.java:357 (ABW)
        g.shaped(item("hbm_m:pu239_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_pu239")));
        // MineralRecipes.java:123 (ABW)
        g.shaped(item("hbm_m:billet_pu240"), 3, p("##"), '#', Ingredient.of(item("hbm_m:pu240_ingot")));
        // MineralRecipes.java:360 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_pu240"), 9, Ingredient.of(item("hbm_m:pu240_ingot")));
        // MineralRecipes.java:123 (ABW)
        g.shapeless(item("hbm_m:pu240_ingot"), 2, Ingredient.of(item("hbm_m:billet_pu240")), Ingredient.of(item("hbm_m:billet_pu240")), Ingredient.of(item("hbm_m:billet_pu240")));
        // MineralRecipes.java:359 (ABW)
        g.shaped(item("hbm_m:pu240_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_pu240")));
        // MineralRecipes.java:124 (ABW)
        g.shaped(item("hbm_m:billet_pu241"), 3, p("##"), '#', Ingredient.of(item("hbm_m:pu241_ingot")));
        // MineralRecipes.java:125 (ABW)
        g.shaped(item("hbm_m:billet_pu_mix"), 3, p("##"), '#', Ingredient.of(item("hbm_m:pu_mix_ingot")));
        // MineralRecipes.java:165 (ABW)
        g.shapeless(item("hbm_m:billet_pu_mix"), 3, Ingredient.of(item("hbm_m:billet_pu239")), Ingredient.of(item("hbm_m:billet_pu239")), Ingredient.of(item("hbm_m:billet_pu240")));
        // MineralRecipes.java:166 (ABW)
        g.shapeless(item("hbm_m:billet_pu_mix"), 1, ore("oredict/nugget/plutonium239"), ore("oredict/nugget/plutonium239"), ore("oredict/nugget/plutonium239"), ore("oredict/nugget/plutonium239"), ore("oredict/nugget/plutonium240"), ore("oredict/nugget/plutonium240"));
        // MineralRecipes.java:167 (ABW)
        g.shapeless(item("hbm_m:billet_pu_mix"), 1, ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu239"), ore("oredict/tiny/pu240"), ore("oredict/tiny/pu240"));
        // MineralRecipes.java:126 (ABW)
        g.shaped(item("hbm_m:billet_am241"), 3, p("##"), '#', Ingredient.of(item("hbm_m:am241_ingot")));
        // MineralRecipes.java:127 (ABW)
        g.shaped(item("hbm_m:billet_am242"), 3, p("##"), '#', Ingredient.of(item("hbm_m:am242_ingot")));
        // MineralRecipes.java:128 (ABW)
        g.shaped(item("hbm_m:billet_am_mix"), 3, p("##"), '#', Ingredient.of(item("hbm_m:am_mix_ingot")));
        // MineralRecipes.java:171 (ABW)
        g.shapeless(item("hbm_m:billet_am_mix"), 3, Ingredient.of(item("hbm_m:billet_am241")), Ingredient.of(item("hbm_m:billet_am242")), Ingredient.of(item("hbm_m:billet_am242")));
        // MineralRecipes.java:172 (ABW)
        g.shapeless(item("hbm_m:billet_am_mix"), 1, ore("oredict/nugget/americium241"), ore("oredict/nugget/americium241"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"), ore("oredict/nugget/americium242"));
        // MineralRecipes.java:173 (ABW)
        g.shapeless(item("hbm_m:billet_am_mix"), 1, ore("oredict/tiny/am241"), ore("oredict/tiny/am241"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"), ore("oredict/tiny/am242"));
        // MineralRecipes.java:129 (ABW)
        g.shaped(item("hbm_m:billet_neptunium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:neptunium_ingot")));
        // MineralRecipes.java:248 (ABW)
        g.shapeless(item("hbm_m:billet_neptunium"), 3, Ingredient.of(item("hbm_m:pellet_rtg_depleted_neptunium")));
        // MineralRecipes.java:372 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_neptunium"), 9, Ingredient.of(item("hbm_m:neptunium_ingot")));
        // MineralRecipes.java:129 (ABW)
        g.shapeless(item("hbm_m:neptunium_ingot"), 2, Ingredient.of(item("hbm_m:billet_neptunium")), Ingredient.of(item("hbm_m:billet_neptunium")), Ingredient.of(item("hbm_m:billet_neptunium")));
        // MineralRecipes.java:371 (ABW)
        g.shaped(item("hbm_m:neptunium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_neptunium")));
        // MineralRecipes.java:130 (ABW)
        g.shaped(item("hbm_m:billet_polonium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:polonium_ingot")));
        // MineralRecipes.java:374 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_polonium"), 9, Ingredient.of(item("hbm_m:polonium_ingot")));
        // MineralRecipes.java:130 (FEHLT)
        g.shapeless(item("hbm_m:polonium_ingot"), 2, Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_polonium")));
        // MineralRecipes.java:335 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:polonium_ingot"), 9, Ingredient.of(item("hbm_m:polonium210_block")));
        // MineralRecipes.java:373 (FEHLT)
        g.shaped(item("hbm_m:polonium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_polonium")));
        // MineralRecipes.java:131 (ABW)
        g.shaped(item("hbm_m:billet_technetium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:technetium_ingot")));
        // MineralRecipes.java:132 (ABW)
        g.shaped(item("hbm_m:billet_au198"), 3, p("##"), '#', Ingredient.of(item("hbm_m:au198_ingot")));
        // MineralRecipes.java:133 (ABW)
        g.shaped(item("hbm_m:billet_pb209"), 3, p("##"), '#', Ingredient.of(item("hbm_m:pb209_ingot")));
        // MineralRecipes.java:134 (ABW)
        g.shaped(item("hbm_m:billet_ra226"), 3, p("##"), '#', Ingredient.of(item("hbm_m:ra226_ingot")));
        // MineralRecipes.java:135 (ABW)
        g.shaped(item("hbm_m:billet_actinium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:actinium_ingot")));
        // MineralRecipes.java:136 (ABW)
        g.shaped(item("hbm_m:billet_schrabidium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:schrabidium_ingot")));
        // MineralRecipes.java:380 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_schrabidium"), 9, Ingredient.of(item("hbm_m:schrabidium_ingot")));
        // MineralRecipes.java:137 (ABW)
        g.shaped(item("hbm_m:billet_solinium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:solinium_ingot")));
        // MineralRecipes.java:416 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_solinium"), 9, Ingredient.of(item("hbm_m:solinium_ingot")));
        // MineralRecipes.java:137 (ABW)
        g.shapeless(item("hbm_m:solinium_ingot"), 2, Ingredient.of(item("hbm_m:billet_solinium")), Ingredient.of(item("hbm_m:billet_solinium")), Ingredient.of(item("hbm_m:billet_solinium")));
        // MineralRecipes.java:415 (ABW)
        g.shaped(item("hbm_m:solinium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_solinium")));
        // MineralRecipes.java:138 (ABW)
        g.shaped(item("hbm_m:billet_gh336"), 3, p("##"), '#', Ingredient.of(item("hbm_m:gh336_ingot")));
        // MineralRecipes.java:139 (ABW)
        g.shaped(item("hbm_m:billet_uranium_fuel"), 3, p("##"), '#', Ingredient.of(item("hbm_m:uranium_fuel_ingot")));
        // MineralRecipes.java:159 (ABW)
        g.shapeless(item("hbm_m:billet_uranium_fuel"), 6, Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u235")));
        // MineralRecipes.java:160 (ABW)
        g.shapeless(item("hbm_m:billet_uranium_fuel"), 1, ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium235"));
    }

    private static void part11(CraftingGen g) {
        // MineralRecipes.java:161 (ABW)
        g.shapeless(item("hbm_m:billet_uranium_fuel"), 1, ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u235"));
        // MineralRecipes.java:382 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_uranium_fuel"), 9, Ingredient.of(item("hbm_m:uranium_fuel_ingot")));
        // MineralRecipes.java:139 (ABW)
        g.shapeless(item("hbm_m:uranium_fuel_ingot"), 2, Ingredient.of(item("hbm_m:billet_uranium_fuel")), Ingredient.of(item("hbm_m:billet_uranium_fuel")), Ingredient.of(item("hbm_m:billet_uranium_fuel")));
        // MineralRecipes.java:381 (ABW)
        g.shaped(item("hbm_m:uranium_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_uranium_fuel")));
        // MineralRecipes.java:140 (ABW)
        g.shaped(item("hbm_m:billet_thorium_fuel"), 3, p("##"), '#', Ingredient.of(item("hbm_m:thorium_fuel_ingot")));
        // MineralRecipes.java:156 (ABW)
        g.shapeless(item("hbm_m:billet_thorium_fuel"), 6, Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_th232")), Ingredient.of(item("hbm_m:billet_u233")));
        // MineralRecipes.java:157 (ABW)
        g.shapeless(item("hbm_m:billet_thorium_fuel"), 1, ore("oredict/nugget/thorium232"), ore("oredict/nugget/thorium232"), ore("oredict/nugget/thorium232"), ore("oredict/nugget/thorium232"), ore("oredict/nugget/thorium232"), ore("oredict/nugget/uranium233"));
        // MineralRecipes.java:158 (ABW)
        g.shapeless(item("hbm_m:billet_thorium_fuel"), 1, ore("oredict/tiny/th232"), ore("oredict/tiny/th232"), ore("oredict/tiny/th232"), ore("oredict/tiny/th232"), ore("oredict/tiny/th232"), ore("oredict/tiny/u233"));
        // MineralRecipes.java:384 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_thorium_fuel"), 9, Ingredient.of(item("hbm_m:thorium_fuel_ingot")));
        // MineralRecipes.java:140 (ABW)
        g.shapeless(item("hbm_m:thorium_fuel_ingot"), 2, Ingredient.of(item("hbm_m:billet_thorium_fuel")), Ingredient.of(item("hbm_m:billet_thorium_fuel")), Ingredient.of(item("hbm_m:billet_thorium_fuel")));
        // MineralRecipes.java:383 (ABW)
        g.shaped(item("hbm_m:thorium_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_thorium_fuel")));
        // MineralRecipes.java:141 (ABW)
        g.shaped(item("hbm_m:billet_plutonium_fuel"), 3, p("##"), '#', Ingredient.of(item("hbm_m:plutonium_fuel_ingot")));
        // MineralRecipes.java:162 (ABW)
        g.shapeless(item("hbm_m:billet_plutonium_fuel"), 3, Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_pu_mix")));
        // MineralRecipes.java:163 (ABW)
        g.shapeless(item("hbm_m:billet_plutonium_fuel"), 1, Ingredient.of(item("hbm_m:nugget_pu_mix")), Ingredient.of(item("hbm_m:nugget_pu_mix")), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"));
        // MineralRecipes.java:164 (ABW)
        g.shapeless(item("hbm_m:billet_plutonium_fuel"), 1, Ingredient.of(item("hbm_m:nugget_pu_mix")), Ingredient.of(item("hbm_m:nugget_pu_mix")), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"));
        // MineralRecipes.java:386 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_plutonium_fuel"), 9, Ingredient.of(item("hbm_m:plutonium_fuel_ingot")));
        // MineralRecipes.java:141 (ABW)
        g.shapeless(item("hbm_m:plutonium_fuel_ingot"), 2, Ingredient.of(item("hbm_m:billet_plutonium_fuel")), Ingredient.of(item("hbm_m:billet_plutonium_fuel")), Ingredient.of(item("hbm_m:billet_plutonium_fuel")));
        // MineralRecipes.java:385 (ABW)
        g.shaped(item("hbm_m:plutonium_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_plutonium_fuel")));
        // MineralRecipes.java:142 (ABW)
        g.shaped(item("hbm_m:billet_neptunium_fuel"), 3, p("##"), '#', Ingredient.of(item("hbm_m:neptunium_fuel_ingot")));
        // MineralRecipes.java:174 (ABW)
        g.shapeless(item("hbm_m:billet_neptunium_fuel"), 3, Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_neptunium")));
        // MineralRecipes.java:175 (ABW)
        g.shapeless(item("hbm_m:billet_neptunium_fuel"), 1, ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"));
        // MineralRecipes.java:176 (ABW)
        g.shapeless(item("hbm_m:billet_neptunium_fuel"), 1, ore("oredict/tiny/np237"), ore("oredict/tiny/np237"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"));
        // MineralRecipes.java:143 (ABW)
        g.shaped(item("hbm_m:billet_mox_fuel"), 3, p("##"), '#', Ingredient.of(item("hbm_m:mox_fuel_ingot")));
        // MineralRecipes.java:177 (ABW)
        g.shapeless(item("hbm_m:billet_mox_fuel"), 3, Ingredient.of(item("hbm_m:billet_uranium_fuel")), Ingredient.of(item("hbm_m:billet_uranium_fuel")), ore("oredict/billet/plutonium239"));
        // MineralRecipes.java:178 (ABW)
        g.shapeless(item("hbm_m:billet_mox_fuel"), 1, ore("oredict/nugget/plutonium239"), ore("oredict/nugget/plutonium239"), Ingredient.of(item("hbm_m:nugget_uranium_fuel")), Ingredient.of(item("hbm_m:nugget_uranium_fuel")), Ingredient.of(item("hbm_m:nugget_uranium_fuel")), Ingredient.of(item("hbm_m:nugget_uranium_fuel")));
        // MineralRecipes.java:388 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_mox_fuel"), 9, Ingredient.of(item("hbm_m:mox_fuel_ingot")));
        // MineralRecipes.java:143 (ABW)
        g.shapeless(item("hbm_m:mox_fuel_ingot"), 2, Ingredient.of(item("hbm_m:billet_mox_fuel")), Ingredient.of(item("hbm_m:billet_mox_fuel")), Ingredient.of(item("hbm_m:billet_mox_fuel")));
        // MineralRecipes.java:387 (ABW)
        g.shaped(item("hbm_m:mox_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_mox_fuel")));
        // MineralRecipes.java:144 (ABW)
        g.shaped(item("hbm_m:billet_les"), 3, p("##"), '#', Ingredient.of(item("hbm_m:ingot_les")));
        // MineralRecipes.java:394 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_les"), 9, Ingredient.of(item("hbm_m:ingot_les")));
        // MineralRecipes.java:144 (FEHLT)
        g.shapeless(item("hbm_m:ingot_les"), 2, Ingredient.of(item("hbm_m:billet_les")), Ingredient.of(item("hbm_m:billet_les")), Ingredient.of(item("hbm_m:billet_les")));
        // MineralRecipes.java:393 (FEHLT)
        g.shaped(item("hbm_m:ingot_les"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_les")));
        // MineralRecipes.java:446 (FEHLT)
        g.shapeless(item("hbm_m:ingot_les"), 1, ore("oredict/nugget/schrabidium"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:145 (ABW)
        g.shaped(item("hbm_m:billet_schrabidium_fuel"), 3, p("##"), '#', Ingredient.of(item("hbm_m:schrabidium_fuel_ingot")));
        // MineralRecipes.java:179 (ABW)
        g.shapeless(item("hbm_m:billet_schrabidium_fuel"), 3, Ingredient.of(item("hbm_m:billet_schrabidium")), Ingredient.of(item("hbm_m:billet_neptunium")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:180 (ABW)
        g.shapeless(item("hbm_m:billet_schrabidium_fuel"), 1, Ingredient.of(item("hbm_m:nugget_schrabidium")), Ingredient.of(item("hbm_m:nugget_schrabidium")), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:181 (ABW)
        g.shapeless(item("hbm_m:billet_schrabidium_fuel"), 1, Ingredient.of(item("hbm_m:nugget_schrabidium")), Ingredient.of(item("hbm_m:nugget_schrabidium")), ore("oredict/tiny/np237"), ore("oredict/tiny/np237"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:390 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_schrabidium_fuel"), 9, Ingredient.of(item("hbm_m:schrabidium_fuel_ingot")));
        // MineralRecipes.java:145 (ABW)
        g.shapeless(item("hbm_m:schrabidium_fuel_ingot"), 2, Ingredient.of(item("hbm_m:billet_schrabidium_fuel")), Ingredient.of(item("hbm_m:billet_schrabidium_fuel")), Ingredient.of(item("hbm_m:billet_schrabidium_fuel")));
        // MineralRecipes.java:389 (ABW)
        g.shaped(item("hbm_m:schrabidium_fuel_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_schrabidium_fuel")));
        // MineralRecipes.java:444 (ABW)
        g.shapeless(item("hbm_m:schrabidium_fuel_ingot"), 1, ore("oredict/nugget/schrabidium"), ore("oredict/nugget/schrabidium"), ore("oredict/nugget/schrabidium"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:146 (ABW)
        g.shaped(item("hbm_m:billet_hes"), 3, p("##"), '#', Ingredient.of(item("hbm_m:ingot_hes")));
        // MineralRecipes.java:392 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_hes"), 9, Ingredient.of(item("hbm_m:ingot_hes")));
        // MineralRecipes.java:146 (FEHLT)
        g.shapeless(item("hbm_m:ingot_hes"), 2, Ingredient.of(item("hbm_m:billet_hes")), Ingredient.of(item("hbm_m:billet_hes")), Ingredient.of(item("hbm_m:billet_hes")));
        // MineralRecipes.java:391 (FEHLT)
        g.shaped(item("hbm_m:ingot_hes"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_hes")));
        // MineralRecipes.java:445 (FEHLT)
        g.shapeless(item("hbm_m:ingot_hes"), 1, ore("oredict/nugget/schrabidium"), ore("oredict/nugget/schrabidium"), ore("oredict/nugget/schrabidium"), ore("oredict/nugget/schrabidium"), ore("oredict/nugget/schrabidium"), ore("oredict/nugget/neptunium237"), ore("oredict/nugget/neptunium237"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:147 (ABW)
        g.shaped(item("hbm_m:billet_australium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:australium_ingot")));
        // MineralRecipes.java:396 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_australium"), 9, Ingredient.of(item("hbm_m:australium_ingot")));
        // MineralRecipes.java:147 (ABW)
        g.shapeless(item("hbm_m:australium_ingot"), 2, Ingredient.of(item("hbm_m:billet_australium")), Ingredient.of(item("hbm_m:billet_australium")), Ingredient.of(item("hbm_m:billet_australium")));
        // MineralRecipes.java:395 (ABW)
        g.shaped(item("hbm_m:australium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_australium")));
        // MineralRecipes.java:150 (ABW)
        g.shaped(item("hbm_m:billet_nuclear_waste"), 1, p("###", "###"), '#', Ingredient.of(item("hbm_m:nuclear_waste_tiny")));
        // MineralRecipes.java:150 (ABW)
        g.shaped(item("hbm_m:billet_nuclear_waste"), 3, p("##"), '#', Ingredient.of(item("hbm_m:nuclear_waste")));
        // MineralRecipes.java:150 (FEHLT)
        g.shapeless(item("hbm_m:nuclear_waste_tiny"), 6, Ingredient.of(item("hbm_m:billet_nuclear_waste")));
        // MineralRecipes.java:418 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nuclear_waste_tiny"), 9, Ingredient.of(item("hbm_m:nuclear_waste")));
        // MineralRecipes.java:150 (FEHLT)
        g.shapeless(item("hbm_m:nuclear_waste"), 2, Ingredient.of(item("hbm_m:billet_nuclear_waste")), Ingredient.of(item("hbm_m:billet_nuclear_waste")), Ingredient.of(item("hbm_m:billet_nuclear_waste")));
        // MineralRecipes.java:318 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nuclear_waste"), 9, Ingredient.of(item("hbm_m:block_waste")));
        // MineralRecipes.java:319 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nuclear_waste"), 9, Ingredient.of(item("hbm_m:block_waste_painted")));
        // MineralRecipes.java:417 (FEHLT)
        g.shaped(item("hbm_m:nuclear_waste"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nuclear_waste_tiny")));
        // MineralRecipes.java:151 (ABW)
        g.shaped(item("hbm_m:billet_beryllium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:beryllium_ingot")));
        // MineralRecipes.java:378 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_beryllium"), 9, Ingredient.of(item("hbm_m:beryllium_ingot")));
    }

    private static void part12(CraftingGen g) {
        // MineralRecipes.java:151 (ABW)
        g.shapeless(item("hbm_m:beryllium_ingot"), 2, Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:377 (ABW)
        g.shaped(item("hbm_m:beryllium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:152 (ABW)
        g.shaped(item("hbm_m:billet_zirconium"), 3, p("##"), '#', Ingredient.of(item("hbm_m:zirconium_ingot")));
        // MineralRecipes.java:249 (ABW)
        g.shapeless(item("hbm_m:billet_zirconium"), 3, Ingredient.of(item("hbm_m:pellet_rtg_depleted_zirconium")));
        // MineralRecipes.java:153 (ABW)
        g.shaped(item("hbm_m:billet_bismuth"), 3, p("##"), '#', Ingredient.of(item("hbm_m:bismuth_ingot")));
        // MineralRecipes.java:245 (ABW)
        g.shapeless(item("hbm_m:billet_bismuth"), 3, Ingredient.of(item("hbm_m:pellet_rtg_depleted_bismuth")));
        // MineralRecipes.java:154 (ABW)
        g.shaped(item("hbm_m:billet_silicon"), 3, p("##"), '#', Ingredient.of(item("hbm_m:silicon_ingot")));
        // MineralRecipes.java:168 (ABW)
        g.shapeless(item("hbm_m:billet_americium_fuel"), 3, Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_am_mix")));
        // MineralRecipes.java:169 (ABW)
        g.shapeless(item("hbm_m:billet_americium_fuel"), 1, Ingredient.of(item("hbm_m:nugget_am_mix")), Ingredient.of(item("hbm_m:nugget_am_mix")), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"), ore("oredict/nugget/uranium238"));
        // MineralRecipes.java:170 (ABW)
        g.shapeless(item("hbm_m:billet_americium_fuel"), 1, Ingredient.of(item("hbm_m:nugget_am_mix")), Ingredient.of(item("hbm_m:nugget_am_mix")), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"), ore("oredict/tiny/u238"));
        // MineralRecipes.java:183 (FEHLT)
        g.shapeless(item("hbm_m:billet_po210be"), 1, ore("oredict/nugget/polonium210"), ore("oredict/nugget/polonium210"), ore("oredict/nugget/polonium210"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:186 (FEHLT)
        g.shapeless(item("hbm_m:billet_po210be"), 2, Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:189 (FEHLT)
        g.shapeless(item("hbm_m:billet_po210be"), 6, Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:184 (FEHLT)
        g.shapeless(item("hbm_m:billet_pu238be"), 1, ore("oredict/nugget/plutonium238"), ore("oredict/nugget/plutonium238"), ore("oredict/nugget/plutonium238"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:187 (FEHLT)
        g.shapeless(item("hbm_m:billet_pu238be"), 2, Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:190 (FEHLT)
        g.shapeless(item("hbm_m:billet_pu238be"), 6, Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:185 (FEHLT)
        g.shapeless(item("hbm_m:billet_ra226be"), 1, ore("oredict/nugget/radium226"), ore("oredict/nugget/radium226"), ore("oredict/nugget/radium226"), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")), Ingredient.of(item("hbm_m:nugget_beryllium")));
        // MineralRecipes.java:188 (FEHLT)
        g.shapeless(item("hbm_m:billet_ra226be"), 2, Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:191 (FEHLT)
        g.shapeless(item("hbm_m:billet_ra226be"), 6, Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")), Ingredient.of(item("hbm_m:billet_beryllium")));
        // MineralRecipes.java:193 (FEHLT)
        g.shapeless(item("hbm_m:billet_zfb_bismuth"), 1, ore("oredict/nugget/zirconium"), ore("oredict/nugget/zirconium"), ore("oredict/nugget/zirconium"), ore("oredict/nugget/uranium"), ore("oredict/nugget/plutonium241"), ore("oredict/nugget/bismuth"));
        // MineralRecipes.java:196 (FEHLT)
        g.shapeless(item("hbm_m:billet_zfb_bismuth"), 6, ore("oredict/billet/zirconium"), ore("oredict/billet/zirconium"), ore("oredict/billet/zirconium"), ore("oredict/billet/uranium"), ore("oredict/billet/plutonium241"), ore("oredict/billet/bismuth"));
        // MineralRecipes.java:194 (FEHLT)
        g.shapeless(item("hbm_m:billet_zfb_pu241"), 1, ore("oredict/nugget/zirconium"), ore("oredict/nugget/zirconium"), ore("oredict/nugget/zirconium"), ore("oredict/nugget/uranium235"), ore("oredict/nugget/plutonium240"), ore("oredict/nugget/plutonium241"));
        // MineralRecipes.java:197 (FEHLT)
        g.shapeless(item("hbm_m:billet_zfb_pu241"), 6, ore("oredict/billet/zirconium"), ore("oredict/billet/zirconium"), ore("oredict/billet/zirconium"), ore("oredict/billet/uranium235"), ore("oredict/billet/plutonium240"), ore("oredict/billet/plutonium241"));
        // MineralRecipes.java:195 (FEHLT)
        g.shapeless(item("hbm_m:billet_zfb_am_mix"), 1, ore("oredict/nugget/zirconium"), ore("oredict/nugget/zirconium"), ore("oredict/nugget/zirconium"), ore("oredict/nugget/plutonium241"), ore("oredict/nugget/plutonium241"), ore("oredict/nugget/americium_rg"));
        // MineralRecipes.java:198 (FEHLT)
        g.shapeless(item("hbm_m:billet_zfb_am_mix"), 6, ore("oredict/billet/zirconium"), ore("oredict/billet/zirconium"), ore("oredict/billet/zirconium"), ore("oredict/billet/plutonium241"), ore("oredict/billet/plutonium241"), ore("oredict/billet/americium_rg"));
        // MineralRecipes.java:233 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg"), 1, Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_pu238")), Ingredient.of(item("hbm_m:billet_pu238")), ore("oredict/plate/iron"));
        // MineralRecipes.java:234 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_radium"), 1, Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_ra226")), Ingredient.of(item("hbm_m:billet_ra226")), ore("oredict/plate/iron"));
        // MineralRecipes.java:235 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_weak"), 1, Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_u238")), Ingredient.of(item("hbm_m:billet_pu238")), ore("oredict/plate/iron"));
        // MineralRecipes.java:236 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_strontium"), 1, Ingredient.of(item("hbm_m:billet_sr90")), Ingredient.of(item("hbm_m:billet_sr90")), Ingredient.of(item("hbm_m:billet_sr90")), ore("oredict/plate/iron"));
        // MineralRecipes.java:237 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_cobalt"), 1, Ingredient.of(item("hbm_m:billet_co60")), Ingredient.of(item("hbm_m:billet_co60")), Ingredient.of(item("hbm_m:billet_co60")), ore("oredict/plate/iron"));
        // MineralRecipes.java:238 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_actinium"), 1, Ingredient.of(item("hbm_m:billet_actinium")), Ingredient.of(item("hbm_m:billet_actinium")), Ingredient.of(item("hbm_m:billet_actinium")), ore("oredict/plate/iron"));
        // MineralRecipes.java:239 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_polonium"), 1, Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_polonium")), Ingredient.of(item("hbm_m:billet_polonium")), ore("oredict/plate/iron"));
        // MineralRecipes.java:240 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_lead"), 1, Ingredient.of(item("hbm_m:billet_pb209")), Ingredient.of(item("hbm_m:billet_pb209")), Ingredient.of(item("hbm_m:billet_pb209")), ore("oredict/plate/iron"));
        // MineralRecipes.java:241 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_gold"), 1, Ingredient.of(item("hbm_m:billet_au198")), Ingredient.of(item("hbm_m:billet_au198")), Ingredient.of(item("hbm_m:billet_au198")), ore("oredict/plate/iron"));
        // MineralRecipes.java:242 (FEHLT)
        g.shapeless(item("hbm_m:pellet_rtg_americium"), 1, Ingredient.of(item("hbm_m:billet_am241")), Ingredient.of(item("hbm_m:billet_am241")), Ingredient.of(item("hbm_m:billet_am241")), ore("oredict/plate/iron"));
        // MineralRecipes.java:246 (ABW)
        g.shapeless(item("hbm_m:lead_ingot"), 2, Ingredient.of(item("hbm_m:pellet_rtg_depleted_lead")));
        // MineralRecipes.java:375 (ABW)
        g.shaped(item("hbm_m:lead_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_lead")));
        // MineralRecipes.java:255 (FEHLT)
        g.shaped(item("hbm_m:block_copper"), 1, p("###", "###", "###"), '#', Ingredient.of(item("minecraft:copper_ingot")));
        // MineralRecipes.java:256 (FEHLT)
        g.shaped(item("hbm_m:block_fluorite"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:fluorite")));
        // MineralRecipes.java:257 (FEHLT)
        g.shaped(item("hbm_m:block_niter"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:niter")));
        // MineralRecipes.java:260 (FEHLT)
        g.shaped(item("hbm_m:block_sulfur"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:sulfur")));
        // MineralRecipes.java:264 (ABW)
        g.shaped(item("hbm_m:block_thorium"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:th232_ingot")));
        // MineralRecipes.java:266 (FEHLT)
        g.shaped(item("hbm_m:block_trinitite"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:trinitite")));
        // MineralRecipes.java:267 (FEHLT)
        g.shaped(item("hbm_m:block_waste"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nuclear_waste")));
        // MineralRecipes.java:268 (FEHLT)
        g.shaped(item("hbm_m:block_scrap"), 1, p("##", "##"), '#', Ingredient.of(item("hbm_m:scrap")));
        // MineralRecipes.java:269 (FEHLT)
        g.shaped(item("hbm_m:block_scrap"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:dust")));
        // MineralRecipes.java:272 (FEHLT)
        g.shaped(item("hbm_m:block_schrabidium_cluster"), 1, p("#S#", "SXS", "#S#"), '#', Ingredient.of(item("hbm_m:schrabidium_ingot")), 'S', Ingredient.of(item("hbm_m:starmetal_ingot")), 'X', Ingredient.of(item("hbm_m:schrabidate_ingot")));
        // MineralRecipes.java:274 (FEHLT)
        g.shaped(item("hbm_m:block_magnetized_tungsten"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:magnetized_tungsten_ingot")));
        // MineralRecipes.java:279 (FEHLT)
        g.shaped(item("hbm_m:block_meteor_cobble"), 1, p("##", "##"), '#', Ingredient.of(item("hbm_m:fragment_meteorite")));
        // MineralRecipes.java:280 (FEHLT)
        g.shaped(item("hbm_m:block_meteor_broken"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:fragment_meteorite")));
        // MineralRecipes.java:281 (FEHLT)
        g.shaped(item("hbm_m:block_yellowcake"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:yellowcake_powder")));
        // MineralRecipes.java:288 (FEHLT)
        g.shaped(item("hbm_m:polonium210_block"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:polonium_ingot")));
        // MineralRecipes.java:298 (FEHLT)
        g.shaped(item("hbm_m:block_lithium"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:lithium")));
        // MineralRecipes.java:299 (FEHLT)
        g.shaped(item("hbm_m:block_white_phosphorus"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:phosphorus_ingot")));
        // MineralRecipes.java:300 (FEHLT)
        g.shaped(item("hbm_m:block_red_phosphorus"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:fire_powder")));
        // MineralRecipes.java:301 (FEHLT)
        g.shaped(item("hbm_m:block_insulator"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:plate_polymer")));
        // MineralRecipes.java:302 (FEHLT)
        g.shaped(item("hbm_m:block_asbestos"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:asbestos_ingot")));
        // MineralRecipes.java:303 (FEHLT)
        g.shaped(item("hbm_m:block_fiberglass"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:fiberglass_ingot")));
        // MineralRecipes.java:307 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:fluorite"), 9, Ingredient.of(item("hbm_m:block_fluorite")));
        // MineralRecipes.java:308 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:niter"), 9, Ingredient.of(item("hbm_m:block_niter")));
    }

    private static void part13(CraftingGen g) {
        // MineralRecipes.java:311 (ABW, 1x1)
        g.shapeless(item("hbm_m:sulfur"), 9, Ingredient.of(item("hbm_m:block_sulfur")));
        // MineralRecipes.java:317 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:trinitite"), 9, Ingredient.of(item("hbm_m:block_trinitite")));
        // MineralRecipes.java:433 (ABW)
        g.shaped(item("hbm_m:euphemium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_euphemium")));
        // MineralRecipes.java:328 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:yellowcake_powder"), 9, Ingredient.of(item("hbm_m:block_yellowcake")));
        // MineralRecipes.java:345 (ABW, 1x1)
        g.shapeless(item("hbm_m:lithium"), 9, Ingredient.of(item("hbm_m:block_lithium")));
        // MineralRecipes.java:346 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:phosphorus_ingot"), 9, Ingredient.of(item("hbm_m:block_white_phosphorus")));
        // MineralRecipes.java:347 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:fire_powder"), 9, Ingredient.of(item("hbm_m:block_red_phosphorus")));
        // MineralRecipes.java:349 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:asbestos_ingot"), 9, Ingredient.of(item("hbm_m:block_asbestos")));
        // MineralRecipes.java:350 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:fiberglass_ingot"), 9, Ingredient.of(item("hbm_m:block_fiberglass")));
        // MineralRecipes.java:376 (ABW, 1x1)
        g.shapeless(item("hbm_m:nugget_lead"), 9, Ingredient.of(item("hbm_m:lead_ingot")));
        // MineralRecipes.java:399 (ABW)
        g.shaped(item("hbm_m:lithium_powder"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:lithium_powder_tiny")));
        // MineralRecipes.java:400 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:lithium_powder_tiny"), 9, Ingredient.of(item("hbm_m:lithium_powder")));
        // PowderRecipes.java:53 (ABW)
        g.shapeless(item("hbm_m:meteorite_powder"), 4, ore("oredict/dust/iron"), ore("oredict/dust/copper"), ore("oredict/dust/lithium"), ore("oredict/dust/nether_quartz"));
        // MineralRecipes.java:419 (FEHLT)
        g.shaped(item("hbm_m:bottle_mercury"), 1, p("###", "#B#", "###"), '#', Ingredient.of(item("hbm_m:nugget_mercury")), 'B', Ingredient.of(item("minecraft:glass_bottle")));
        // MineralRecipes.java:421 (FEHLT)
        g.shaped(item("hbm_m:egg_balefire"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:egg_balefire_shard")));
        // MineralRecipes.java:422 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:egg_balefire_shard"), 9, Ingredient.of(item("hbm_m:egg_balefire")));
        // MineralRecipes.java:430 (FEHLT)
        g.shaped(item("hbm_m:egg_balefire_shard"), 1, p("##", "##"), '#', Ingredient.of(item("hbm_m:balefire_powder")));
        // MineralRecipes.java:431 (FEHLT)
        g.shaped(item("hbm_m:egg_balefire_shard"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:cell_balefire")));
        // MineralRecipes.java:423 (FEHLT)
        g.shaped(item("hbm_m:nitra"), 1, p("##", "##"), '#', Ingredient.of(item("hbm_m:nitra_small")));
        // MineralRecipes.java:424 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nitra_small"), 4, Ingredient.of(item("hbm_m:nitra")));
        // MineralRecipes.java:426 (FEHLT)
        g.shaped(item("hbm_m:glass_polarized"), 4, p("##", "##"), '#', Ingredient.of(item("hbm_m:part_generic_glass_polarized")));
        // MineralRecipes.java:427 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:paleogenite_powder_tiny"), 9, Ingredient.of(item("hbm_m:paleogenite_powder")));
        // MineralRecipes.java:427 (FEHLT)
        g.shaped(item("hbm_m:paleogenite_powder"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:paleogenite_powder_tiny")));
        // MineralRecipes.java:428 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_osmiridium"), 9, Ingredient.of(item("hbm_m:osmiridium_ingot")));
        // MineralRecipes.java:428 (FEHLT)
        g.shaped(item("hbm_m:osmiridium_ingot"), 1, p("###", "###", "###"), '#', Ingredient.of(item("hbm_m:nugget_osmiridium")));
        // MineralRecipes.java:434 (FEHLT, 1x1)
        g.shapeless(item("hbm_m:nugget_euphemium"), 9, Ingredient.of(item("hbm_m:euphemium_ingot")));
        // MineralRecipes.java:452 (FEHLT)
        g.shapeless(item("hbm_m:ball_fireclay"), 4, Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")), ore("oredict/dust/aluminum"));
        // MineralRecipes.java:453 (FEHLT)
        g.shapeless(item("hbm_m:ball_fireclay"), 4, Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")), ore("oredict/ore/aluminum"));
        // MineralRecipes.java:454 (FEHLT)
        g.shapeless(item("hbm_m:ball_fireclay"), 4, Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("hbm_m:stone_resource_limestone")), ore("oredict/sand"));
        // PowderRecipes.java:28 (FEHLT)
        g.shapeless(item("hbm_m:ballistite"), 3, Ingredient.of(item("minecraft:gunpowder")), ore("oredict/dust/saltpeter"), Ingredient.of(item("minecraft:sugar")));
        // PowderRecipes.java:29 (FEHLT)
        g.shapeless(item("hbm_m:ball_dynamite"), 2, ore("oredict/dust/saltpeter"), Ingredient.of(item("minecraft:sugar")), Ingredient.of(item("minecraft:sand")), ore("oredict/ntmchemistryset"));
        // PowderRecipes.java:32 (FEHLT)
        g.shapeless(item("hbm_m:semtex_mix_powder"), 3, Ingredient.of(item("hbm_m:solid_fuel")), Ingredient.of(item("hbm_m:cordite")), ore("oredict/dust/saltpeter"));
        // PowderRecipes.java:33 (FEHLT)
        g.shapeless(item("hbm_m:semtex_mix_powder"), 1, Ingredient.of(item("hbm_m:solid_fuel")), Ingredient.of(item("hbm_m:ballistite")), ore("oredict/dust/saltpeter"));
        // PowderRecipes.java:35 (ABW)
        g.shapeless(item("minecraft:clay_ball"), 4, Ingredient.of(item("minecraft:clay")));
        // PowderRecipes.java:36 (ABW)
        g.shapeless(item("hbm_m:cement_powder"), 4, ore("oredict/dust/limestone"), Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")), Ingredient.of(item("minecraft:clay_ball")));
        // PowderRecipes.java:43 (FEHLT)
        g.shapeless(item("minecraft:gunpowder"), 3, ore("oredict/dust/sulfur"), ore("oredict/dust/saltpeter"), ore("oredict/gem/coal"));
        // PowderRecipes.java:44 (FEHLT)
        g.shapeless(item("minecraft:gunpowder"), 3, ore("oredict/dust/sulfur"), ore("oredict/dust/saltpeter"), Ingredient.of(item("minecraft:charcoal")));
        // PowderRecipes.java:45 (FEHLT)
        g.shapeless(item("minecraft:gunpowder"), 3, ore("oredict/dust/sulfur"), ore("oredict/dust/saltpeter"), ore("oredict/gem/coal"));
        // PowderRecipes.java:46 (FEHLT)
        g.shapeless(item("minecraft:gunpowder"), 3, ore("oredict/dust/sulfur"), ore("oredict/dust/saltpeter"), Ingredient.of(item("minecraft:charcoal")));
        // PowderRecipes.java:49 (FEHLT)
        g.shapeless(item("hbm_m:powder_power"), 3, ore("oredict/dust/glowstone"), ore("oredict/dust/diamond"), ore("oredict/dust/magnetized_tungsten"));
        // PowderRecipes.java:50 (FEHLT)
        g.shapeless(item("hbm_m:powder_nitan_mix"), 6, ore("oredict/dust/neptunium237"), ore("oredict/dust/iodine"), ore("oredict/dust/thorium232"), ore("oredict/dust/astatine"), ore("oredict/dust/neodymium"), ore("oredict/dust/caesium"));
        // PowderRecipes.java:51 (FEHLT)
        g.shapeless(item("hbm_m:powder_nitan_mix"), 6, ore("oredict/dust/strontium"), ore("oredict/dust/cobalt"), ore("oredict/dust/bromine"), ore("oredict/dust/tennessine"), ore("oredict/dust/niobium"), ore("oredict/dust/cerium"));
        // PowderRecipes.java:52 (FEHLT)
        g.shapeless(item("hbm_m:spark_mix_powder"), 3, ore("oredict/dust/workers_alloy"), ore("oredict/dust/euphemium"), Ingredient.of(item("hbm_m:powder_power")));
        // PowderRecipes.java:54 (FEHLT)
        g.shapeless(item("hbm_m:thermite_powder"), 4, ore("oredict/dust/iron"), ore("oredict/dust/iron"), ore("oredict/dust/iron"), ore("oredict/dust/aluminum"));
        // PowderRecipes.java:56 (FEHLT)
        g.shapeless(item("hbm_m:powder_desh_mix"), 1, ore("oredict/dust_tiny/boron"), ore("oredict/dust_tiny/boron"), ore("oredict/dust_tiny/lanthanum"), ore("oredict/dust_tiny/lanthanum"), ore("oredict/dust_tiny/cerium"), ore("oredict/dust_tiny/cobalt"), ore("oredict/dust_tiny/lithium"), ore("oredict/dust_tiny/neodymium"), ore("oredict/dust_tiny/niobium"));
        // PowderRecipes.java:57 (FEHLT)
        g.shapeless(item("hbm_m:powder_desh_mix"), 9, ore("oredict/dust/boron"), ore("oredict/dust/boron"), ore("oredict/dust/lanthanum"), ore("oredict/dust/lanthanum"), ore("oredict/dust/cerium"), ore("oredict/dust/cobalt"), ore("oredict/dust/lithium"), ore("oredict/dust/neodymium"), ore("oredict/dust/niobium"));
        // PowderRecipes.java:58 (FEHLT)
        g.shapeless(item("hbm_m:desh_ready_powder"), 1, Ingredient.of(item("hbm_m:powder_desh_mix")), Ingredient.of(item("hbm_m:nugget_mercury")), Ingredient.of(item("hbm_m:nugget_mercury")), ore("oredict/dust/coal"));
        // PowderRecipes.java:67 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 1, Ingredient.of(item("minecraft:charcoal")), ore("oredict/sand"));
        // PowderRecipes.java:68 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 2, ore("oredict/dust/coal"), ore("oredict/sand"));
        // PowderRecipes.java:69 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 4, ore("oredict/dust/fluorite"), ore("oredict/sand"));
        // PowderRecipes.java:70 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 8, ore("oredict/dust/lead"), ore("oredict/dust/sulfur"), ore("oredict/sand"));
        // PowderRecipes.java:71 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 12, ore("oredict/dust/limestone"), ore("oredict/sand"));
        // PowderRecipes.java:72 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 12, ore("oredict/dust/calcium"), ore("oredict/sand"));
        // PowderRecipes.java:73 (FEHLT)
        g.shapeless(item("hbm_m:flux_powder"), 16, ore("oredict/dust/borax"), ore("oredict/sand"));
        // PowderRecipes.java:75 (FEHLT)
        g.shapeless(item("hbm_m:fertilizer_powder"), 4, ore("oredict/dust/calcium"), ore("oredict/dust/red_phosphorus"), ore("oredict/dust/saltpeter"), ore("oredict/dust/sulfur"));
        // PowderRecipes.java:76 (FEHLT)
        g.shapeless(item("hbm_m:fertilizer_powder"), 4, ore("oredict/any/ash"), ore("oredict/dust/red_phosphorus"), ore("oredict/dust/saltpeter"), ore("oredict/dust/sulfur"));
        // PowderRecipes.java:79 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:red_copper_powder"), 2, ore("oredict/dust/redstone"), ore("oredict/dust/copper"));
        // PowderRecipes.java:80 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:dura_steel_powder"), 2, ore("oredict/dust/steel"), ore("oredict/dust/tungsten"));
        // PowderRecipes.java:81 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:dura_steel_powder"), 2, ore("oredict/dust/steel"), ore("oredict/dust/cobalt"));
        // PowderRecipes.java:82 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:dura_steel_powder"), 4, ore("oredict/dust/iron"), ore("oredict/dust/coal"), ore("oredict/dust/tungsten"), ore("oredict/dust/tungsten"));
    }

    private static void part14(CraftingGen g) {
        // PowderRecipes.java:83 [nur bei lbsm_crafting] (FEHLT)
        g.when("lbsm_crafting").shapeless(item("hbm_m:dura_steel_powder"), 4, ore("oredict/dust/iron"), ore("oredict/dust/coal"), ore("oredict/dust/cobalt"), ore("oredict/dust/cobalt"));
        // PowderRecipes.java:88 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_gray"), 2, Ingredient.of(item("hbm_m:chemical_dye_black")), Ingredient.of(item("hbm_m:chemical_dye_white")));
        // PowderRecipes.java:89 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_silver"), 2, Ingredient.of(item("hbm_m:chemical_dye_gray")), Ingredient.of(item("hbm_m:chemical_dye_white")));
        // PowderRecipes.java:90 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_orange"), 2, Ingredient.of(item("hbm_m:chemical_dye_red")), Ingredient.of(item("hbm_m:chemical_dye_yellow")));
        // PowderRecipes.java:91 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_lime"), 2, Ingredient.of(item("hbm_m:chemical_dye_green")), Ingredient.of(item("hbm_m:chemical_dye_white")));
        // PowderRecipes.java:92 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_cyan"), 2, Ingredient.of(item("hbm_m:chemical_dye_blue")), Ingredient.of(item("hbm_m:chemical_dye_green")));
        // PowderRecipes.java:93 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_purple"), 2, Ingredient.of(item("hbm_m:chemical_dye_red")), Ingredient.of(item("hbm_m:chemical_dye_blue")));
        // PowderRecipes.java:94 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_brown"), 2, Ingredient.of(item("hbm_m:chemical_dye_orange")), Ingredient.of(item("hbm_m:chemical_dye_black")));
        // PowderRecipes.java:95 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_magenta"), 2, Ingredient.of(item("hbm_m:chemical_dye_pink")), Ingredient.of(item("hbm_m:chemical_dye_purple")));
        // PowderRecipes.java:96 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_lightblue"), 2, Ingredient.of(item("hbm_m:chemical_dye_blue")), Ingredient.of(item("hbm_m:chemical_dye_white")));
        // PowderRecipes.java:97 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_pink"), 2, Ingredient.of(item("hbm_m:chemical_dye_red")), Ingredient.of(item("hbm_m:chemical_dye_white")));
        // PowderRecipes.java:98 (FEHLT)
        g.shapeless(item("hbm_m:chemical_dye_green"), 2, Ingredient.of(item("hbm_m:chemical_dye_blue")), Ingredient.of(item("hbm_m:chemical_dye_yellow")));
        // ToolRecipes.java:88 (FEHLT)
        g.shaped(item("hbm_m:bottle_opener"), 1, p("S", "P"), 'S', ore("oredict/plate/steel"), 'P', ore("oredict/plank_wood"));
        // ToolRecipes.java:89 (FEHLT)
        g.shaped(item("minecraft:saddle"), 1, p("LLL", "LRL", " S "), 'S', ore("oredict/ingot/steel"), 'L', Ingredient.of(item("minecraft:leather")), 'R', Ingredient.of(item("hbm_m:plant_item_rope")));
        // ToolRecipes.java:92 (FEHLT)
        g.shaped(item("hbm_m:matchstick"), 16, p("I", "S"), 'I', ore("oredict/dust/sulfur"), 'S', ore("oredict/stick_wood"));
        // ToolRecipes.java:93 (FEHLT)
        g.shaped(item("hbm_m:matchstick"), 24, p("I", "S"), 'I', ore("oredict/dust/red_phosphorus"), 'S', ore("oredict/stick_wood"));
        // ToolRecipes.java:96 (FEHLT)
        g.shaped(item("hbm_m:wood_gavel"), 1, p("SWS", " R ", " R "), 'S', ore("oredict/slab_wood"), 'W', ore("oredict/log_wood"), 'R', ore("oredict/stick_wood"));
        // ToolRecipes.java:97 (FEHLT)
        g.shaped(item("hbm_m:lead_gavel"), 1, p("PIP", "IGI", "PIP"), 'P', Ingredient.of(item("hbm_m:pellet_buckshot")), 'I', ore("oredict/ingot/lead"), 'G', Ingredient.of(item("hbm_m:wood_gavel")));
        // ToolRecipes.java:100 (FEHLT)
        g.shaped(item("hbm_m:pipe_lead"), 1, p("II", " I", " I"), 'I', ore("oredict/ntmpipe/lead"));
        // ToolRecipes.java:101 (FEHLT)
        g.shaped(item("hbm_m:ullapool_caber"), 1, p("ITI", " S ", " S "), 'I', ore("oredict/plate/iron"), 'T', Ingredient.of(item("minecraft:tnt")), 'S', ore("oredict/stick_wood"));
        // ToolRecipes.java:105 (ABW)
        g.shaped(item("hbm_m:designator"), 1, p("  A", "#B#", "#B#"), '#', ore("oredict/ingot/any_plastic"), 'A', ore("oredict/plate/steel"), 'B', Ingredient.of(item("hbm_m:integrated_circuit")));
        // ToolRecipes.java:108 (FEHLT)
        g.shapeless(item("hbm_m:designator_arty_range"), 1, Ingredient.of(item("hbm_m:rangefinder")), Ingredient.of(item("hbm_m:advanced_circuit")), ore("oredict/ingot/any_plastic"));
        // ToolRecipes.java:109 (FEHLT)
        g.shaped(item("hbm_m:linker"), 1, p("I I", "ICI", "GGG"), 'I', ore("oredict/plate/iron"), 'G', ore("oredict/plate/gold"), 'C', Ingredient.of(item("hbm_m:advanced_circuit")));
        // ToolRecipes.java:112 (FEHLT)
        g.shaped(item("hbm_m:survey_scanner"), 1, p("SWS", " G ", "PCP"), 'W', ore("oredict/wire_fine/gold"), 'P', ore("oredict/ingot/any_plastic"), 'C', Ingredient.of(item("hbm_m:advanced_circuit")), 'S', ore("oredict/plate/steel"), 'G', ore("oredict/ingot/gold"));
        // ToolRecipes.java:114 (ABW)
        g.shaped(item("hbm_m:dosimeter"), 1, p("WGW", "WCW", "WBW"), 'W', ore("oredict/plank_wood"), 'G', ore("oredict/pane_glass"), 'C', Ingredient.of(item("hbm_m:vacuum_tube")), 'B', ore("oredict/ingot/beryllium"));
        // ToolRecipes.java:116 (FEHLT)
        g.shapeless(item("hbm_m:digamma_diagnostic"), 1, Ingredient.of(item("hbm_m:geiger_counter")), ore("oredict/billet/polonium210"), ore("oredict/ingot/asbestos"));
        // ToolRecipes.java:117 (FEHLT)
        g.shaped(item("hbm_m:pollution_detector"), 1, p("SFS", "SCS", " S "), 'S', ore("oredict/plate/steel"), 'F', Ingredient.of(item("hbm_m:filter_coal")), 'C', Ingredient.of(item("hbm_m:vacuum_tube")));
        // ToolRecipes.java:118 (FEHLT)
        g.shaped(item("hbm_m:ore_density_scanner"), 1, p("VVV", "CSC", "GGG"), 'V', Ingredient.of(item("hbm_m:vacuum_tube")), 'C', Ingredient.of(item("hbm_m:capacitor")), 'S', Ingredient.of(item("hbm_m:controller_chassis")), 'G', ore("oredict/plate/gold"));
        // ToolRecipes.java:119 (ABW)
        g.shaped(item("hbm_m:defuser"), 1, p(" PS", "P P", " P "), 'P', ore("oredict/ingot/any_plastic"), 'S', ore("oredict/plate/steel"));
        // ToolRecipes.java:120 (ABW)
        g.shaped(item("hbm_m:coltan_tool"), 1, p("ACA", "CXC", "ACA"), 'A', ore("oredict/ingot/copper"), 'C', ore("oredict/crystal/cinnabar"), 'X', Ingredient.of(item("minecraft:compass")));
        // ToolRecipes.java:121 (FEHLT)
        g.shaped(item("hbm_m:reacher"), 1, p("BIB", "P P", "B B"), 'B', ore("oredict/bolt/tungsten"), 'I', ore("oredict/ingot/tungsten"), 'P', ore("oredict/ingot/any_rubber"));
        // ToolRecipes.java:122 (FEHLT)
        g.shaped(item("hbm_m:sat_designator"), 1, p("RRD", "PIC", "  P"), 'P', ore("oredict/plate/gold"), 'R', Ingredient.of(item("minecraft:redstone")), 'C', Ingredient.of(item("hbm_m:advanced_circuit")), 'D', Ingredient.of(item("hbm_m:sat_chip")), 'I', ore("oredict/ingot/gold"));
        // ToolRecipes.java:123 (FEHLT)
        g.shapeless(item("hbm_m:sat_relay"), 1, Ingredient.of(item("hbm_m:sat_chip")), Ingredient.of(item("hbm_m:ducttape")), Ingredient.of(item("hbm_m:radar_linker")));
        // ToolRecipes.java:124 (FEHLT)
        g.shaped(item("hbm_m:settings_tool"), 1, p(" P ", "PCP", "III"), 'P', ore("oredict/plate/iron"), 'C', Ingredient.of(item("hbm_m:analog_circuit")), 'I', Ingredient.of(item("hbm_m:plate_polymer")));
        // ToolRecipes.java:126 (FEHLT)
        g.shaped(item("hbm_m:pipette"), 1, p("  L", " G ", "G  "), 'L', ore("oredict/ingot/any_rubber"), 'G', ore("oredict/block/glass_colorless"));
        // ToolRecipes.java:127 (FEHLT)
        g.shaped(item("hbm_m:pipette_boron"), 1, p("  P", " B ", "B  "), 'P', ore("oredict/ingot/rubber"), 'B', Ingredient.of(item("hbm_m:glass_boron")));
        // ToolRecipes.java:128 (FEHLT)
        g.shaped(item("hbm_m:pipette_laboratory"), 1, p("  C", " R ", "P  "), 'C', Ingredient.of(item("hbm_m:microchip")), 'R', ore("oredict/ingot/rubber"), 'P', Ingredient.of(item("hbm_m:pipette_boron")));
        // ToolRecipes.java:130 (FEHLT)
        g.shaped(item("hbm_m:siphon"), 1, p(" GR", " GR", " G "), 'G', ore("oredict/block/glass_colorless"), 'R', ore("oredict/ingot/any_rubber"));
        // ToolRecipes.java:132 (FEHLT)
        g.shaped(item("hbm_m:mirror_tool"), 1, p(" A ", " IA", "I  "), 'A', ore("oredict/ingot/aluminum"), 'I', ore("oredict/ingot/iron"));
        // ToolRecipes.java:135 (FEHLT)
        g.shaped(item("hbm_m:analysis_tool"), 1, p("  G", " S ", "S  "), 'G', ore("oredict/pane_glass"), 'S', ore("oredict/ingot/steel"));
        // ToolRecipes.java:140 (FEHLT)
        g.shaped(item("hbm_m:screwdriver_desh"), 1, p("  I", " I ", "S  "), 'S', ore("oredict/ingot/any_plastic"), 'I', ore("oredict/ingot/workers_alloy"));
        // ToolRecipes.java:141 (FEHLT)
        g.shaped(item("hbm_m:hand_drill"), 1, p(" D", "S ", " S"), 'D', ore("oredict/ingot/dura_steel"), 'S', ore("oredict/stick_wood"));
        // ToolRecipes.java:142 (FEHLT)
        g.shaped(item("hbm_m:hand_drill_desh"), 1, p(" D", "S ", " S"), 'D', ore("oredict/ingot/workers_alloy"), 'S', ore("oredict/ingot/any_plastic"));
        // ToolRecipes.java:143 (FEHLT)
        g.shaped(item("hbm_m:chemistry_set"), 1, p("GIG", "GCG"), 'G', ore("oredict/block/glass"), 'I', ore("oredict/ingot/iron"), 'C', ore("oredict/ingot/copper"));
        // ToolRecipes.java:144 (FEHLT)
        g.shaped(item("hbm_m:chemistry_set_boron"), 1, p("GIG", "GCG"), 'G', Ingredient.of(item("hbm_m:glass_boron")), 'I', ore("oredict/ingot/steel"), 'C', ore("oredict/ingot/cobalt"));
        // ToolRecipes.java:147 (FEHLT)
        g.shaped(item("hbm_m:boltgun"), 1, p("DPS", " RD", " D "), 'D', ore("oredict/ingot/dura_steel"), 'P', Ingredient.of(item("hbm_m:part_generic_piston_pneumatic")), 'R', ore("oredict/ingot/rubber"), 'S', ore("oredict/shell/steel"));
        // ToolRecipes.java:151 (FEHLT)
        g.shapeless(item("hbm_m:bobmazon"), 1, Ingredient.of(item("minecraft:book")), Ingredient.of(item("minecraft:gold_nugget")), Ingredient.of(item("minecraft:string")), ore("oredict/dye_blue"));
        // ToolRecipes.java:161 (FEHLT)
        g.shaped(item("hbm_m:boat_rubber"), 1, p("L L", "LLL"), 'L', ore("oredict/ingot/any_rubber"));
        // WeaponRecipes.java:44 (ABW)
        g.shaped(item("hbm_m:part_stock_wood"), 1, p("WWW", "  W"), 'W', ore("oredict/plank_wood"));
        // WeaponRecipes.java:45 (ABW)
        g.shaped(item("hbm_m:part_grip_wood"), 1, p("W ", " W", " W"), 'W', ore("oredict/plank_wood"));
        // WeaponRecipes.java:62 (ABW)
        g.shaped(item("hbm_m:gun_pepperbox"), 1, p("IIW", "  C"), 'I', ore("oredict/ingot/iron"), 'W', ore("oredict/plank_wood"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:147 (ABW)
        g.shaped(item("hbm_m:weapon_mod_special_furniture_black"), 1, p("PDS", "  G"), 'P', ore("oredict/ingot/any_plastic"), 'D', ore("oredict/dye_black"), 'S', ore("oredict/stock/any_plastic"), 'G', ore("oredict/grip/any_plastic"));
        // WeaponRecipes.java:154 (ABW)
        g.shaped(item("hbm_m:weapon_mod_special_drill_hss"), 1, p(" IP", "IIM", " IP"), 'I', ore("oredict/ingot/dura_steel"), 'P', ore("oredict/ingot/any_plastic"), 'M', ore("oredict/gun_mechanism/gun_metal"));
        // WeaponRecipes.java:163 (ABW)
        g.shaped(item("hbm_m:weapon_mod_special_sifter"), 1, p("IGI", "IGI"), 'I', ore("oredict/ingot/dura_steel"), 'G', Ingredient.of(item("hbm_m:steel_grate")));
        // WeaponRecipes.java:174 (FEHLT)
        g.shapeless(item("hbm_m:missile_bhole"), 1, Ingredient.of(item("hbm_m:missile_assembly")), Ingredient.of(item("hbm_m:ducttape")), Ingredient.of(item("hbm_m:black_hole")), Ingredient.of(item("hbm_m:controller_advanced")));
        // WeaponRecipes.java:175 (FEHLT)
        g.shapeless(item("hbm_m:missile_schrabidium"), 1, Ingredient.of(item("hbm_m:missile_assembly")), Ingredient.of(item("hbm_m:ducttape")), Ingredient.of(item("hbm_m:cell_anti_schrabidium")), Ingredient.of(item("hbm_m:quantum_circuit")));
        // WeaponRecipes.java:176 (FEHLT)
        g.shapeless(item("hbm_m:missile_emp"), 1, Ingredient.of(item("hbm_m:missile_assembly")), Ingredient.of(item("hbm_m:ducttape")), Ingredient.of(item("hbm_m:emp_bomb")));
        // WeaponRecipes.java:181 (ABW)
        g.shaped(item("hbm_m:mp_stability_10_space"), 1, p("ASA", "PSP"), 'A', ore("oredict/plate/aluminum"), 'P', ore("oredict/ingot/steel"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")));
        // WeaponRecipes.java:182 (ABW)
        g.shaped(item("hbm_m:mp_stability_15_flat"), 1, p("ASA", "PSP"), 'A', ore("oredict/plate/aluminum"), 'P', ore("oredict/plate/steel"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")));
        // WeaponRecipes.java:183 (ABW)
        g.shaped(item("hbm_m:mp_stability_15_thin"), 1, p("A A", "PSP", "PSP"), 'A', ore("oredict/plate/aluminum"), 'P', ore("oredict/plate/steel"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")));
    }

    private static void part15(CraftingGen g) {
        // WeaponRecipes.java:211 (ABW)
        g.shaped(item("hbm_m:turret_sentry"), 1, p("PPL", " MD", " SC"), 'P', ore("oredict/plate/steel"), 'M', Ingredient.of(item("hbm_m:motor")), 'L', ore("oredict/gun_mechanism/gun_metal"), 'S', Ingredient.of(item("hbm_m:steel_scaffold")), 'C', Ingredient.of(item("hbm_m:integrated_circuit")), 'D', Ingredient.of(item("hbm_m:crt_display")));
        // WeaponRecipes.java:221 (FEHLT)
        g.shaped(item("hbm_m:assembly_nuke"), 1, p(" WP", "SEP", " WP"), 'W', ore("oredict/wire_fine/gold"), 'P', ore("oredict/plate/weapon_steel"), 'S', ore("oredict/shell/weapon_steel"), 'E', Ingredient.of(item("hbm_m:ball_tatb")));
        // WeaponRecipes.java:224 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell"), 4, p(" T ", "GHG", "CCC"), 'T', Ingredient.of(item("hbm_m:tnt_ntm")), 'G', Ingredient.of(item("minecraft:gunpowder")), 'H', ore("oredict/shell/steel"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:225 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell"), 4, p(" T ", "GHG", "CCC"), 'T', Ingredient.of(item("hbm_m:tnt_ntm")), 'G', Ingredient.of(item("hbm_m:ballistite")), 'H', ore("oredict/shell/steel"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:226 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell"), 6, p(" T ", "GHG", "CCC"), 'T', Ingredient.of(item("hbm_m:tnt_ntm")), 'G', Ingredient.of(item("hbm_m:cordite")), 'H', ore("oredict/shell/steel"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:227 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_explosive"), 4, p(" T ", "GHG", "CCC"), 'T', ore("oredict/ingot/any_plasticexplosive"), 'G', Ingredient.of(item("minecraft:gunpowder")), 'H', ore("oredict/shell/steel"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:228 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_explosive"), 4, p(" T ", "GHG", "CCC"), 'T', ore("oredict/ingot/any_plasticexplosive"), 'G', Ingredient.of(item("hbm_m:ballistite")), 'H', ore("oredict/shell/steel"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:229 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_explosive"), 6, p(" T ", "GHG", "CCC"), 'T', ore("oredict/ingot/any_plasticexplosive"), 'G', Ingredient.of(item("hbm_m:cordite")), 'H', ore("oredict/shell/steel"), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:230 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_apfsds_t"), 4, p(" I ", "GIG", "CCC"), 'I', ore("oredict/ingot/tungsten"), 'G', Ingredient.of(item("minecraft:gunpowder")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:231 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_apfsds_t"), 4, p(" I ", "GIG", "CCC"), 'I', ore("oredict/ingot/tungsten"), 'G', Ingredient.of(item("hbm_m:ballistite")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:232 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_apfsds_t"), 6, p(" I ", "GIG", "CCC"), 'I', ore("oredict/ingot/tungsten"), 'G', Ingredient.of(item("hbm_m:cordite")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:233 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_apfsds_du"), 4, p(" I ", "GIG", "CCC"), 'I', ore("oredict/ingot/uranium238"), 'G', Ingredient.of(item("minecraft:gunpowder")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:234 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_apfsds_du"), 4, p(" I ", "GIG", "CCC"), 'I', ore("oredict/ingot/uranium238"), 'G', Ingredient.of(item("hbm_m:ballistite")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:235 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_apfsds_du"), 6, p(" I ", "GIG", "CCC"), 'I', ore("oredict/ingot/uranium238"), 'G', Ingredient.of(item("hbm_m:cordite")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:236 (FEHLT)
        g.shaped(item("hbm_m:ammo_shell_w9"), 1, p(" P ", "NSN", " P "), 'P', ore("oredict/nugget/plutonium239"), 'N', Ingredient.of(item("hbm_m:neutron_reflector")), 'S', Ingredient.of(item("hbm_m:ammo_shell_explosive")));
        // WeaponRecipes.java:239 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty"), 1, p("CIC", "CSC", "CCC"), 'C', Ingredient.of(item("hbm_m:cordite")), 'I', ore("oredict/block/iron"), 'S', ore("oredict/shell/copper"));
        // WeaponRecipes.java:240 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_classic"), 1, p(" D ", "DSD", " D "), 'D', Ingredient.of(item("hbm_m:ball_dynamite")), 'S', Ingredient.of(item("hbm_m:ammo_arty")));
        // WeaponRecipes.java:241 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_he"), 1, p("TTT", "TST", "TTT"), 'T', Ingredient.of(item("hbm_m:ball_tnt")), 'S', Ingredient.of(item("hbm_m:ammo_arty")));
        // WeaponRecipes.java:242 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_phosphorus"), 1, p("D", "S", "D"), 'D', ore("oredict/ingot/white_phosphorus"), 'S', Ingredient.of(item("hbm_m:ammo_arty")));
        // WeaponRecipes.java:243 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_phosphorus_multi"), 1, p("DSD", "SCS", "DSD"), 'D', ore("oredict/ingot/white_phosphorus"), 'S', Ingredient.of(item("hbm_m:ammo_arty_phosphorus")), 'C', Ingredient.of(item("hbm_m:det_cord")));
        // WeaponRecipes.java:244 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_mini_nuke"), 1, p(" P ", "NSN", " P "), 'P', ore("oredict/nugget/plutonium239"), 'N', Ingredient.of(item("hbm_m:neutron_reflector")), 'S', Ingredient.of(item("hbm_m:ammo_arty")));
        // WeaponRecipes.java:245 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_mini_nuke_multi"), 1, p("DSD", "SCS", "DSD"), 'D', Ingredient.of(item("hbm_m:neutron_reflector")), 'S', Ingredient.of(item("hbm_m:ammo_arty_mini_nuke")), 'C', Ingredient.of(item("hbm_m:det_cord")));
        // WeaponRecipes.java:246 (FEHLT)
        g.shapeless(item("hbm_m:ammo_arty_nuke"), 1, Ingredient.of(item("hbm_m:ammo_arty_he")), Ingredient.of(item("hbm_m:boy_bullet")), Ingredient.of(item("hbm_m:boy_target")), Ingredient.of(item("hbm_m:boy_shielding")), Ingredient.of(item("hbm_m:controller")), Ingredient.of(item("hbm_m:ducttape")));
        // WeaponRecipes.java:247 (FEHLT)
        g.shaped(item("hbm_m:ammo_arty_cargo"), 1, p(" I ", " S ", "CCC"), 'C', Ingredient.of(item("hbm_m:cordite")), 'I', Ingredient.of(item("hbm_m:sphere_steel")), 'S', ore("oredict/shell/copper"));
        // WeaponRecipes.java:250 (FEHLT)
        // Entfernt im aktuellen Original (CIWS-Gurt kommt aus der Munitionspresse): g.shaped(item("hbm_m:ammo_dgk"), 1, p("LLL", "GGG", "CCC"), 'L', ore("oredict/plate/lead"), 'G', Ingredient.of(item("hbm_m:ballistite")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:251 (FEHLT)
        // Entfernt im aktuellen Original (CIWS-Gurt kommt aus der Munitionspresse): g.shaped(item("hbm_m:ammo_dgk"), 1, p("LLL", "GGG", "CCC"), 'L', ore("oredict/plate/lead"), 'G', Ingredient.of(item("hbm_m:cordite")), 'C', ore("oredict/ingot/copper"));
        // WeaponRecipes.java:254 (ABW)
        g.shaped(item("hbm_m:ammo_fireext"), 1, p(" P ", "BDB", " P "), 'P', ore("oredict/plate/steel"), 'B', ore("oredict/bolt/steel"), 'D', filled("hbm_m:fluid_tank_full", ModFluids.WATER.getSource()));
        // WeaponRecipes.java:255 (FEHLT)
        g.shaped(item("hbm_m:ammo_fireext_foam"), 1, p(" N ", "NFN", " N "), 'N', ore("oredict/dust/saltpeter"), 'F', Ingredient.of(item("hbm_m:ammo_fireext")));
        // WeaponRecipes.java:256 (FEHLT)
        g.shaped(item("hbm_m:ammo_fireext_sand"), 1, p("NNN", "NFN", "NNN"), 'N', Ingredient.of(item("hbm_m:sand_boron")), 'F', Ingredient.of(item("hbm_m:ammo_fireext")));
        // WeaponRecipes.java:260 (ABW)
        g.shaped(item("hbm_m:grenade_shell_frag"), 4, p("B", "P", "S"), 'B', ore("oredict/bolt/steel"), 'P', ore("oredict/plate/aluminum"), 'S', ore("oredict/shell/steel"));
        // WeaponRecipes.java:261 (ABW)
        g.shaped(item("hbm_m:grenade_shell_stick"), 4, p("S", "B", "W"), 'B', ore("oredict/bolt/steel"), 'S', ore("oredict/shell/steel"), 'W', ore("oredict/plank_wood"));
        // WeaponRecipes.java:287 (FEHLT)
        g.shaped(item("hbm_m:stick_dynamite"), 4, p(" S ", "PDP", "PDP"), 'S', Ingredient.of(item("hbm_m:safety_fuse")), 'P', Ingredient.of(item("minecraft:paper")), 'D', Ingredient.of(item("hbm_m:ball_dynamite")));
        // WeaponRecipes.java:288 (FEHLT)
        g.shapeless(item("hbm_m:stick_dynamite_fishing"), 1, Ingredient.of(item("hbm_m:stick_dynamite")), Ingredient.of(item("hbm_m:stick_dynamite")), Ingredient.of(item("hbm_m:stick_dynamite")), Ingredient.of(item("minecraft:paper")), ore("oredict/any/tar"));
        // WeaponRecipes.java:289 (FEHLT)
        g.shaped(item("hbm_m:stick_tnt"), 4, p(" S ", "PDP", "PDP"), 'S', Ingredient.of(item("hbm_m:det_cord")), 'P', Ingredient.of(item("minecraft:paper")), 'D', Ingredient.of(item("hbm_m:ball_tnt")));
        // WeaponRecipes.java:290 (FEHLT)
        g.shaped(item("hbm_m:stick_semtex"), 4, p(" S ", "PDP", "PDP"), 'S', Ingredient.of(item("hbm_m:det_cord")), 'P', Ingredient.of(item("minecraft:paper")), 'D', Ingredient.of(item("hbm_m:semtex_ingot")));
        // WeaponRecipes.java:291 (FEHLT)
        g.shaped(item("hbm_m:stick_c4"), 4, p(" S ", "PDP", "PDP"), 'S', Ingredient.of(item("hbm_m:det_cord")), 'P', Ingredient.of(item("minecraft:paper")), 'D', Ingredient.of(item("hbm_m:c4_ingot")));
        // WeaponRecipes.java:297 (FEHLT)
        g.shaped(item("hbm_m:c4"), 1, p("DDD", "DSD", "DDD"), 'D', Ingredient.of(item("hbm_m:stick_c4")), 'S', Ingredient.of(item("hbm_m:safety_fuse")));
        // WeaponRecipes.java:307 (FEHLT)
        g.shaped(item("hbm_m:n2_charge"), 1, p(" D ", "ERE", " D "), 'D', Ingredient.of(item("hbm_m:ducttape")), 'E', Ingredient.of(item("hbm_m:det_charge")), 'R', ore("oredict/block/redstone"));
        // WeaponRecipes.java:308 (FEHLT)
        g.shaped(item("hbm_m:battery_spark"), 1, p(" W ", "DSD", "DSD"), 'W', ore("oredict/wire_dense/magnetized_tungsten"), 'D', Ingredient.of(item("hbm_m:plate_dineutronium")), 'S', Ingredient.of(item("hbm_m:spark_mix_powder")));
        // WeaponRecipes.java:309 (FEHLT)
        g.shaped(item("hbm_m:battery_trixite"), 1, p(" W ", "DSD", "DTD"), 'W', ore("oredict/wire_dense/magnetized_tungsten"), 'D', ore("oredict/plate_triple/saturnite"), 'S', Ingredient.of(item("hbm_m:powder_power")), 'T', Ingredient.of(item("hbm_m:crystal_trixite")));
        // WeaponRecipes.java:310 (FEHLT)
        g.shaped(item("hbm_m:battery_trixite"), 1, p(" W ", "DTD", "DSD"), 'W', ore("oredict/wire_dense/magnetized_tungsten"), 'D', ore("oredict/plate_triple/saturnite"), 'S', Ingredient.of(item("hbm_m:powder_power")), 'T', Ingredient.of(item("hbm_m:crystal_trixite")));
        // WeaponRecipes.java:313 (FEHLT)
        g.shaped(item("hbm_m:custom_tnt"), 1, p(" C ", "TIT", "TIT"), 'C', ore("oredict/plate/copper"), 'I', ore("oredict/plate/iron"), 'T', ore("oredict/ingot/any_highexplosive"));
        // WeaponRecipes.java:314 (FEHLT)
        g.shaped(item("hbm_m:custom_nuke"), 1, p(" C ", "LUL", "LUL"), 'C', ore("oredict/plate/copper"), 'L', ore("oredict/plate/lead"), 'U', ore("oredict/ingot/uranium235"));
        // WeaponRecipes.java:315 (FEHLT)
        g.shaped(item("hbm_m:custom_hydro"), 1, p(" C ", "LTL", "LIL"), 'C', ore("oredict/plate/copper"), 'L', ore("oredict/plate/lead"), 'I', ore("oredict/plate/iron"), 'T', Ingredient.of(item("hbm_m:cell_tritium")));
        // WeaponRecipes.java:316 (FEHLT)
        g.shaped(item("hbm_m:custom_amat"), 1, p(" C ", "MMM", "AAA"), 'C', ore("oredict/plate/copper"), 'A', ore("oredict/plate/aluminum"), 'M', Ingredient.of(item("hbm_m:cell_antimatter")));
        // WeaponRecipes.java:317 (FEHLT)
        g.shaped(item("hbm_m:custom_dirty"), 1, p(" C ", "WLW", "WLW"), 'C', ore("oredict/plate/copper"), 'L', ore("oredict/plate/lead"), 'W', Ingredient.of(item("hbm_m:nuclear_waste")));
        // WeaponRecipes.java:318 (FEHLT)
        g.shaped(item("hbm_m:custom_schrab"), 1, p(" C ", "LUL", "LUL"), 'C', ore("oredict/plate/copper"), 'L', ore("oredict/plate/lead"), 'U', ore("oredict/ingot/schrabidium"));
    }
    /** Lueckenabgleich 2026-10: Rezepte des Originals, deren Ergebnis im Port existiert, die aber noch fehlten. */
    private static void part16(CraftingGen g) {
        // CraftingManager.java:919
        g.shaped(item("hbm_m:gear_large_steel"), 1, p("III", "ICI", "III"), 'I', ore("oredict/plate/steel"), 'C', ore("oredict/ingot/titanium"));
        // PowderRecipes.java:100 (Farbstoff-Schleife, weiss)
        g.shapeless(item("hbm_m:crayon_white"), 4, Ingredient.of(item("hbm_m:chemical_dye_white")), ore("oredict/any/tar"), Ingredient.of(item("minecraft:paper")));
        // WeaponRecipes.java:318 (crucible, Meta 3)
        g.shaped(item("hbm_m:crucible"), 1, p("MEM", "YDY", "YCY"), 'M', Ingredient.of(item("hbm_m:meteorite_forged_ingot")), 'E', ore("oredict/ingot/euphemium"),
                'Y', Ingredient.of(item("hbm_m:billet_yharonite")), 'D', Ingredient.of(item("hbm_m:demon_core_closed")), 'C', Ingredient.of(item("hbm_m:chainsteel_ingot")));

        // CraftingManager.java:876-880 (Kran-Varianten mit dem Foerderband-Block): entfallen, der Port hat das Foerderband nur als Stab-Item

        // CraftingManager.java:161-169: Spulen mit Stahlkern (die Eisenvarianten gibt es schon)
        g.shaped(item("hbm_m:coil_copper"), 1, p("WWW", "WIW", "WWW"), 'W', ore("oredict/wire_fine/mingrade"), 'I', ore("oredict/ingot/steel"));
        g.shaped(item("hbm_m:coil_gold"), 1, p("WWW", "WIW", "WWW"), 'W', ore("oredict/wire_fine/gold"), 'I', ore("oredict/ingot/steel"));
        g.shaped(item("hbm_m:coil_magnetized_tungsten"), 1, p("WWW", "WIW", "WWW"), 'W', ore("oredict/wire_fine/magnetized_tungsten"), 'I', ore("oredict/ingot/steel"));
        g.shaped(item("hbm_m:coil_tungsten"), 1, p("WWW", "WIW", "WWW"), 'W', ore("oredict/wire_fine/tungsten"), 'I', ore("oredict/ingot/steel"));
        g.shaped(item("hbm_m:coil_copper_torus"), 2, p(" C ", "CPC", " C "), 'C', Ingredient.of(item("hbm_m:coil_copper")), 'P', ore("oredict/plate/steel"));
        g.shaped(item("hbm_m:coil_gold_torus"), 2, p(" C ", "CPC", " C "), 'C', Ingredient.of(item("hbm_m:coil_gold")), 'P', ore("oredict/plate/steel"));

        // CraftingManager.java:347-353: flache Stempel auch mit Netherziegeln
        Ingredient nb = Ingredient.of(item("minecraft:nether_brick"));
        g.shaped(item("hbm_m:stamp_stone_flat"), 1, p("III", "SSS"), 'I', nb, 'S', ore("oredict/stone"));
        g.shaped(item("hbm_m:stamp_iron_flat"), 1, p("III", "SSS"), 'I', nb, 'S', ore("oredict/ingot/iron"));
        g.shaped(item("hbm_m:stamp_steel_flat"), 1, p("III", "SSS"), 'I', nb, 'S', ore("oredict/ingot/steel"));
        g.shaped(item("hbm_m:stamp_titanium_flat"), 1, p("III", "SSS"), 'I', nb, 'S', ore("oredict/ingot/titanium"));
        g.shaped(item("hbm_m:stamp_obsidian_flat"), 1, p("III", "SSS"), 'I', nb, 'S', Ingredient.of(item("minecraft:obsidian")));
        // CraftingManager.java:354: Desh-Stempel hat ein eigenes Muster mit Ferrouran-Kern (Ziegel und Netherziegel)
        for (Ingredient brick : new Ingredient[] {Ingredient.of(item("minecraft:brick")), nb}) {
            g.shaped(item("hbm_m:stamp_desh_flat"), 1, p("BDB", "DSD", "BDB"), 'B', brick, 'D', ore("oredict/ingot/desh"), 'S', ore("oredict/ingot/ferrouranium"));
        }

        // CraftingManager.java:90-95 (BlockMultiSlab/BlockGenericStairs.recipeGen): 6 Stufen bzw. 4 Treppen
        String[][] slabs = {
                {"concrete_slab", "concrete_smooth"}, {"concrete_asbestos_slab", "concrete_asbestos"}, {"asphalt_slab", "asphalt"},
                {"brick_concrete_mossy_slab", "brick_concrete_mossy"}, {"brick_concrete_cracked_slab", "brick_concrete_cracked"},
                {"brick_concrete_broken_slab", "brick_concrete_broken"}, {"brick_ducrete_slab", "brick_ducrete"},
                {"reinforced_brick_slab", "reinforced_brick"}, {"brick_obsidian_slab", "brick_obsidian"}, {"brick_light_slab", "brick_light"},
                {"brick_compound_slab", "brick_compound"}, {"brick_asbestos_slab", "brick_asbestos"}, {"brick_fire_slab", "brick_fire"},
                {"stones_slab_tile", "lightstone_tile"}, {"stones_slab_bricks", "lightstone_bricks"},
        };
        for (String[] s : slabs) g.shaped(item("hbm_m:" + s[0]), 6, p("###"), '#', Ingredient.of(item("hbm_m:" + s[1])));
        String[][] stairs = {
                {"concrete_smooth_stairs", "concrete_smooth"}, {"concrete_stairs", "concrete"}, {"concrete_asbestos_stairs", "concrete_asbestos"},
                {"ducrete_smooth_stairs", "ducrete_smooth"}, {"ducrete_stairs", "ducrete"},
                {"brick_concrete_mossy_stairs", "brick_concrete_mossy"}, {"brick_concrete_cracked_stairs", "brick_concrete_cracked"},
                {"brick_concrete_broken_stairs", "brick_concrete_broken"}, {"brick_ducrete_stairs", "brick_ducrete"},
                {"reinforced_brick_stairs", "reinforced_brick"}, {"brick_obsidian_stairs", "brick_obsidian"}, {"brick_light_stairs", "brick_light"},
                {"brick_compound_stairs", "brick_compound"}, {"brick_asbestos_stairs", "brick_asbestos"}, {"brick_fire_stairs", "brick_fire"},
                {"lightstone_tile_stairs", "lightstone_tile"}, {"lightstone_bricks_stairs", "lightstone_bricks"},
        };
        for (String[] s : stairs) g.shaped(item("hbm_m:" + s[0]), 4, p("#  ", "## ", "###"), '#', Ingredient.of(item("hbm_m:" + s[1])));
    }

}
//?}
