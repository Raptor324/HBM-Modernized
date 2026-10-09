package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.recipe.AnvilRecipe.OverlayType;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;
import static com.hbm_m.datagen.recipes.custom.AnvilRecipeBuilder.box;
import static com.hbm_m.datagen.recipes.custom.AnvilRecipeBuilder.boxStack;
import static com.hbm_m.datagen.recipes.custom.AnvilRecipeBuilder.stirling;

/**
 * 1:1-Port von {@code AnvilRecipes.registerSmithing()} und {@code AnvilRecipes.registerConstruction()} (Amboss).
 *
 * <p>AUTOMATISCH ERZEUGT aus dem Original ({@code com.hbm.inventory.recipes.anvil.AnvilRecipes}) durch
 * {@code rc/anvil.py} - nicht von Hand aendern. Die zweite Zahl je Rezept ist die Original-Reihenfolge (GUI-Liste,
 * erste passende Schmiede-Regel). Konfig-Fassungen ({@code if(!enable528)}, {@code exp ? ..}) ueber {@link ConfigRecipes};
 * LBSM-Ambossfreischaltung wertet {@code AnvilRecipe.Serializer} beim Laden aus.
 * Mit OFFEN markierte Rezepte haben im Port (noch) keine Entsprechung fuer Zutat oder Ergebnis.</p>
 */
public final class AnvilConstructionRecipeGenerator {

    private AnvilConstructionRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        smithing(writer);
        construction0(writer);
        construction1(writer);
        construction2(writer);
        construction3(writer);
        construction4(writer);
        construction5(writer);
        construction6(writer);
        checkMissing("AnvilConstructionRecipeGenerator");
    }

    private static void smithing(Consumer<FinishedRecipe> writer) {
        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 1, stack("hbm_m:anvil_steel", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/steel"), 10)
                .save(writer, "anvil/smithing/anvil_steel");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 2, stack("hbm_m:anvil_desh", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/workers_alloy"), 10)
                .save(writer, "anvil/smithing/anvil_desh");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 3, stack("hbm_m:anvil_saturnite", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/saturnite"), 10)
                .save(writer, "anvil/smithing/anvil_saturnite");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 4, stack("hbm_m:anvil_ferrouranium", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, Ingredient.of(item("hbm_m:ferrouranium_ingot")), 10)
                .save(writer, "anvil/smithing/anvil_ferrouranium");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 5, stack("hbm_m:anvil_bismuth_bronze", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/bismuth_bronze"), 10)
                .save(writer, "anvil/smithing/anvil_bismuth_bronze");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 6, stack("hbm_m:anvil_arsenic_bronze", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/arsenic_bronze"), 10)
                .save(writer, "anvil/smithing/anvil_arsenic_bronze");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 7, stack("hbm_m:anvil_schrabidate", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/schrabidate"), 10)
                .save(writer, "anvil/smithing/anvil_schrabidate");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 8, stack("hbm_m:anvil_dnt", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/dineutronium"), 10)
                .save(writer, "anvil/smithing/anvil_dnt");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 9, stack("hbm_m:anvil_osmiridium", 1), Ingredient.of(item("hbm_m:anvil_iron")), 1, ore("oredict/ingot/osmiridium"), 10)
                .save(writer, "anvil/smithing/anvil_osmiridium");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 10, stack("hbm_m:anvil_steel", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/steel"), 10)
                .save(writer, "anvil/smithing/anvil_steel_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 11, stack("hbm_m:anvil_desh", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/workers_alloy"), 10)
                .save(writer, "anvil/smithing/anvil_desh_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 12, stack("hbm_m:anvil_saturnite", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/saturnite"), 10)
                .save(writer, "anvil/smithing/anvil_saturnite_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 13, stack("hbm_m:anvil_ferrouranium", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, Ingredient.of(item("hbm_m:ferrouranium_ingot")), 10)
                .save(writer, "anvil/smithing/anvil_ferrouranium_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 14, stack("hbm_m:anvil_bismuth_bronze", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/bismuth_bronze"), 10)
                .save(writer, "anvil/smithing/anvil_bismuth_bronze_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 15, stack("hbm_m:anvil_arsenic_bronze", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/arsenic_bronze"), 10)
                .save(writer, "anvil/smithing/anvil_arsenic_bronze_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 16, stack("hbm_m:anvil_schrabidate", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/schrabidate"), 10)
                .save(writer, "anvil/smithing/anvil_schrabidate_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 17, stack("hbm_m:anvil_dnt", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/dineutronium"), 10)
                .save(writer, "anvil/smithing/anvil_dnt_2");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 18, stack("hbm_m:anvil_osmiridium", 1), Ingredient.of(item("hbm_m:anvil_lead")), 1, ore("oredict/ingot/osmiridium"), 10)
                .save(writer, "anvil/smithing/anvil_osmiridium_2");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 19, stack("hbm_m:steel_dusted_ingot_1", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_1");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 20, stack("hbm_m:steel_dusted_ingot_2", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_1")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_1")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_2");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 21, stack("hbm_m:steel_dusted_ingot_3", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_2")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_2")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_3");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 22, stack("hbm_m:steel_dusted_ingot_4", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_3")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_3")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_4");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 23, stack("hbm_m:steel_dusted_ingot_5", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_4")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_4")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_5");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 24, stack("hbm_m:steel_dusted_ingot_6", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_5")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_5")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_6");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 25, stack("hbm_m:steel_dusted_ingot_7", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_6")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_6")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_7");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 26, stack("hbm_m:steel_dusted_ingot_8", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_7")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_7")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_8");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 27, stack("hbm_m:steel_dusted_ingot_9", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_8")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_8")), 1).hot()
                .save(writer, "anvil/smithing/steel_dusted_ingot_9");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 28, stack("hbm_m:chainsteel_ingot", 1), Ingredient.of(item("hbm_m:steel_dusted_ingot_9")), 1, Ingredient.of(item("hbm_m:steel_dusted_ingot_9")), 1).hot()
                .save(writer, "anvil/smithing/chainsteel_ingot");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 29, stack("hbm_m:meteorite_forged_ingot", 1), Ingredient.of(item("hbm_m:meteorite_ingot")), 1, Ingredient.of(item("hbm_m:meteorite_ingot")), 1).hot()
                .save(writer, "anvil/smithing/meteorite_forged_ingot");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 30, stack("hbm_m:blade_meteorite", 1), Ingredient.of(item("hbm_m:meteorite_forged_ingot")), 1, Ingredient.of(item("hbm_m:meteorite_forged_ingot")), 1).hot()
                .save(writer, "anvil/smithing/blade_meteorite");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 31, stack("hbm_m:meteorite_sword_reforged", 1), Ingredient.of(item("hbm_m:meteorite_sword_seared")), 1, Ingredient.of(item("hbm_m:meteorite_forged_ingot")), 1).hot()
                .save(writer, "anvil/smithing/meteorite_sword_reforged");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 32, stack("hbm_m:cobalt_decorated_sword", 1), Ingredient.of(item("hbm_m:cobalt_sword")), 1, Ingredient.of(item("hbm_m:meteorite_ingot")), 1).hot()
                .save(writer, "anvil/smithing/cobalt_decorated_sword");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 33, stack("hbm_m:cobalt_decorated_pickaxe", 1), Ingredient.of(item("hbm_m:cobalt_pickaxe")), 1, Ingredient.of(item("hbm_m:meteorite_ingot")), 1).hot()
                .save(writer, "anvil/smithing/cobalt_decorated_pickaxe");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 34, stack("hbm_m:cobalt_decorated_axe", 1), Ingredient.of(item("hbm_m:cobalt_axe")), 1, Ingredient.of(item("hbm_m:meteorite_ingot")), 1).hot()
                .save(writer, "anvil/smithing/cobalt_decorated_axe");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 35, stack("hbm_m:cobalt_decorated_shovel", 1), Ingredient.of(item("hbm_m:cobalt_shovel")), 1, Ingredient.of(item("hbm_m:meteorite_ingot")), 1).hot()
                .save(writer, "anvil/smithing/cobalt_decorated_shovel");

        // AnvilSmithingHotRecipe Stufe 3
        AnvilRecipeBuilder.smithing(3, 36, stack("hbm_m:cobalt_decorated_hoe", 1), Ingredient.of(item("hbm_m:cobalt_hoe")), 1, Ingredient.of(item("hbm_m:meteorite_ingot")), 1).hot()
                .save(writer, "anvil/smithing/cobalt_decorated_hoe");

        // AnvilSmithingRecipe Stufe 1916169
        AnvilRecipeBuilder.smithing(1916169, 37, stack("hbm_m:wings_murk", 1), Ingredient.of(item("hbm_m:wings_limp")), 1, Ingredient.of(item("hbm_m:particle_tachyon")), 1)
                .save(writer, "anvil/smithing/wings_murk");

        // AnvilSmithingRecipe Stufe 4
        AnvilRecipeBuilder.smithing(4, 38, stack("hbm_m:flask_infusion", 1), Ingredient.of(item("hbm_m:gem_alexandrite")), 1, Ingredient.of(item("hbm_m:bottle_nuka")), 1)
                .save(writer, "anvil/smithing/flask_infusion");

        // AnvilSmithingRecipe Stufe 1
        AnvilRecipeBuilder.smithing(1, 39, stack("hbm_m:gunmetal_ingot", 1), ore("oredict/ingot/copper"), 1, ore("oredict/ingot/aluminum"), 1)
                .save(writer, "anvil/smithing/gunmetal_ingot");

        // AnvilSmithingMold 0
        AnvilRecipeBuilder.smithing(1, 40, stack("hbm_m:mold_nugget", 1), ore("oredict/nugget/gold"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("nugget", 1)
                .save(writer, "anvil/smithing/mold_0");

        // AnvilSmithingMold 1
        AnvilRecipeBuilder.smithing(1, 41, stack("hbm_m:mold_billet", 1), ore("oredict/billet/uranium"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("billet", 1)
                .save(writer, "anvil/smithing/mold_1");

        // AnvilSmithingMold 2
        AnvilRecipeBuilder.smithing(1, 42, stack("hbm_m:mold_ingot", 1), ore("oredict/ingot/iron"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("ingot", 1)
                .save(writer, "anvil/smithing/mold_2");

        // AnvilSmithingMold 3
        AnvilRecipeBuilder.smithing(1, 43, stack("hbm_m:mold_plate", 1), ore("oredict/plate/iron"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("plate", 1)
                .save(writer, "anvil/smithing/mold_3");

        // AnvilSmithingMold 19
        AnvilRecipeBuilder.smithing(1, 44, stack("hbm_m:mold_plate_cast", 1), ore("oredict/plate_triple/iron"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("plateTriple", 1)
                .save(writer, "anvil/smithing/mold_19");

        // AnvilSmithingMold 13
        AnvilRecipeBuilder.smithing(1, 45, stack("hbm_m:mold_plates_cast", 1), ore("oredict/plate_triple/iron"), 3, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("plateTriple", 3)
                .save(writer, "anvil/smithing/mold_13");

        // AnvilSmithingMold 4
        AnvilRecipeBuilder.smithing(1, 46, stack("hbm_m:mold_wire", 1), ore("oredict/wire_fine/copper"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("wireFine", 1)
                .save(writer, "anvil/smithing/mold_4");

        // AnvilSmithingMold 5
        AnvilRecipeBuilder.smithing(1, 47, stack("hbm_m:mold_blade", 1), Ingredient.of(item("hbm_m:blade_titanium")), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldItems(item("hbm_m:blade_titanium"), item("hbm_m:blade_tungsten"))
                .save(writer, "anvil/smithing/mold_5");

        // AnvilSmithingMold 6
        AnvilRecipeBuilder.smithing(1, 48, stack("hbm_m:mold_blades", 1), Ingredient.of(item("hbm_m:blades_steel")), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldItems(item("hbm_m:blades_steel"), item("hbm_m:blades_titanium"))
                .save(writer, "anvil/smithing/mold_6");

        // AnvilSmithingMold 7
        AnvilRecipeBuilder.smithing(1, 49, stack("hbm_m:mold_stamp", 1), Ingredient.of(item("hbm_m:stamp_iron_flat")), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldItems(item("hbm_m:stamp_stone_flat"), item("hbm_m:stamp_iron_flat"), item("hbm_m:stamp_steel_flat"), item("hbm_m:stamp_titanium_flat"), item("hbm_m:stamp_obsidian_flat"))
                .save(writer, "anvil/smithing/mold_7");

        // AnvilSmithingMold 8
        AnvilRecipeBuilder.smithing(1, 50, stack("hbm_m:mold_shell", 1), ore("oredict/shell/steel"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("shell", 1)
                .save(writer, "anvil/smithing/mold_8");

        // AnvilSmithingMold 9
        AnvilRecipeBuilder.smithing(1, 51, stack("hbm_m:mold_pipe", 1), ore("oredict/ntmpipe/steel"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("ntmpipe", 1)
                .save(writer, "anvil/smithing/mold_9");

        // AnvilSmithingMold 10
        AnvilRecipeBuilder.smithing(1, 52, stack("hbm_m:mold_ingots", 1), ore("oredict/ingot/iron"), 9, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("ingot", 9)
                .save(writer, "anvil/smithing/mold_10");

        // AnvilSmithingMold 11
        AnvilRecipeBuilder.smithing(1, 53, stack("hbm_m:mold_plates", 1), ore("oredict/plate/iron"), 9, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("plate", 9)
                .save(writer, "anvil/smithing/mold_11");

        // AnvilSmithingMold 12
        AnvilRecipeBuilder.smithing(1, 54, stack("hbm_m:mold_block", 1), ore("oredict/block/iron"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("block", 1)
                .save(writer, "anvil/smithing/mold_12");

        // AnvilSmithingMold 20
        AnvilRecipeBuilder.smithing(1, 55, stack("hbm_m:mold_wire_dense", 1), ore("oredict/wire_dense/mingrade"), 1, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("wireDense", 1)
                .save(writer, "anvil/smithing/mold_20");

        // AnvilSmithingMold 21
        AnvilRecipeBuilder.smithing(1, 56, stack("hbm_m:mold_wires_dense", 1), ore("oredict/wire_dense/mingrade"), 9, Ingredient.of(item("hbm_m:mold_base")), 1).keepLeft().moldShape("wireDense", 9)
                .save(writer, "anvil/smithing/mold_21");

        // AnvilSmithingCyanideRecipe
        AnvilRecipeBuilder.smithing(1, 57, stack("minecraft:bread", 1), Ingredient.of(item("minecraft:bread")), 1, Ingredient.of(item("hbm_m:plan_c")), 1).special("cyanide")
                .save(writer, "anvil/smithing/cyanide");

        // AnvilSmithingRenameRecipe
        AnvilRecipeBuilder.smithing(1, 58, stack("minecraft:iron_sword", 1), Ingredient.of(item("minecraft:iron_sword")), 1, Ingredient.of(item("minecraft:name_tag")), 1).keepRight().special("rename")
                .save(writer, "anvil/smithing/rename");
    }

    private static void construction0(Consumer<FinishedRecipe> writer) {
        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 0, OverlayType.SMITHING)
                .input(ore("oredict/ingot/iron"), 1)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/plate_iron");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 1, OverlayType.SMITHING)
                .input(ore("oredict/ingot/gold"), 1)
                .output(stack("hbm_m:plate_gold", 1))
                .save(writer, "anvil/construction/plate_gold");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 2, OverlayType.SMITHING)
                .input(ore("oredict/ingot/titanium"), 1)
                .output(stack("hbm_m:plate_titanium", 1))
                .save(writer, "anvil/construction/plate_titanium");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 3, OverlayType.SMITHING)
                .input(ore("oredict/ingot/aluminum"), 1)
                .output(stack("hbm_m:plate_aluminium", 1))
                .save(writer, "anvil/construction/plate_aluminium");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 4, OverlayType.SMITHING)
                .input(ore("oredict/ingot/steel"), 1)
                .output(stack("hbm_m:plate_steel", 1))
                .save(writer, "anvil/construction/plate_steel");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 5, OverlayType.SMITHING)
                .input(ore("oredict/ingot/lead"), 1)
                .output(stack("hbm_m:plate_lead", 1))
                .save(writer, "anvil/construction/plate_lead");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 6, OverlayType.SMITHING)
                .input(ore("oredict/ingot/copper"), 1)
                .output(stack("hbm_m:plate_copper", 1))
                .save(writer, "anvil/construction/plate_copper");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 7, OverlayType.SMITHING)
                .input(ore("oredict/ingot/gun_metal"), 1)
                .output(stack("hbm_m:plate_gunmetal", 1))
                .save(writer, "anvil/construction/plate_gunmetal");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 8, OverlayType.SMITHING)
                .input(ore("oredict/ingot/weapon_steel"), 1)
                .output(stack("hbm_m:plate_weaponsteel", 1))
                .save(writer, "anvil/construction/plate_weaponsteel");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 9, OverlayType.SMITHING)
                .input(ore("oredict/ingot/saturnite"), 1)
                .output(stack("hbm_m:plate_saturnite", 1))
                .save(writer, "anvil/construction/plate_saturnite");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 10, OverlayType.SMITHING)
                .input(ore("oredict/ingot/dura_steel"), 1)
                .output(stack("hbm_m:plate_dura_steel", 1))
                .save(writer, "anvil/construction/plate_dura_steel");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 11, OverlayType.SMITHING)
                .input(ore("oredict/ingot/schrabidium"), 1)
                .output(stack("hbm_m:plate_schrabidium", 1))
                .save(writer, "anvil/construction/plate_schrabidium");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 12, OverlayType.SMITHING)
                .input(ore("oredict/ingot/cmb_steel"), 1)
                .output(stack("hbm_m:plate_combine_steel", 1))
                .save(writer, "anvil/construction/plate_combine_steel");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 13, OverlayType.SMITHING)
                .input(ore("oredict/ingot/carbon"), 1)
                .output(stack("hbm_m:wire_carbon", 8))
                .save(writer, "anvil/construction/wire_carbon");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 14, OverlayType.SMITHING)
                .input(ore("oredict/ingot/gold"), 1)
                .output(stack("hbm_m:wire_gold", 8))
                .save(writer, "anvil/construction/wire_gold");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 15, OverlayType.SMITHING)
                .input(ore("oredict/ingot/schrabidium"), 1)
                .output(stack("hbm_m:wire_schrabidium", 8))
                .save(writer, "anvil/construction/wire_schrabidium");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 16, OverlayType.SMITHING)
                .input(ore("oredict/ingot/copper"), 1)
                .output(stack("hbm_m:wire_copper", 8))
                .save(writer, "anvil/construction/wire_copper");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 17, OverlayType.SMITHING)
                .input(ore("oredict/ingot/tungsten"), 1)
                .output(stack("hbm_m:wire_tungsten", 8))
                .save(writer, "anvil/construction/wire_tungsten");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 18, OverlayType.SMITHING)
                .input(ore("oredict/ingot/aluminum"), 1)
                .output(stack("hbm_m:wire_aluminium", 8))
                .save(writer, "anvil/construction/wire_aluminium");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 19, OverlayType.SMITHING)
                .input(ore("oredict/ingot/lead"), 1)
                .output(stack("hbm_m:wire_lead", 8))
                .save(writer, "anvil/construction/wire_lead");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 20, OverlayType.SMITHING)
                .input(ore("oredict/ingot/zirconium"), 1)
                .output(stack("hbm_m:wire_zirconium", 8))
                .save(writer, "anvil/construction/wire_zirconium");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 21, OverlayType.SMITHING)
                .input(ore("oredict/ingot/steel"), 1)
                .output(stack("hbm_m:wire_steel", 8))
                .save(writer, "anvil/construction/wire_steel");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 22, OverlayType.SMITHING)
                .input(ore("oredict/ingot/mingrade"), 1)
                .output(stack("hbm_m:wire_red_copper", 8))
                .save(writer, "anvil/construction/wire_red_copper");

        // Stufe 4, SMITHING
        AnvilRecipeBuilder.construction(4, 23, OverlayType.SMITHING)
                .input(ore("oredict/ingot/magnetized_tungsten"), 1)
                .output(stack("hbm_m:wire_magnetized_tungsten", 8))
                .save(writer, "anvil/construction/wire_magnetized_tungsten");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 24, OverlayType.SMITHING)
                .input(ore("oredict/dust/coal"), 1)
                .output(stack("minecraft:coal", 1))
                .save(writer, "anvil/construction/coal");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 25, OverlayType.SMITHING)
                .input(ore("oredict/dust/nether_quartz"), 1)
                .output(stack("minecraft:quartz", 1))
                .save(writer, "anvil/construction/quartz");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 26, OverlayType.SMITHING)
                .input(ore("oredict/dust/lapis"), 1)
                .output(stack("minecraft:lapis_lazuli", 1))
                .save(writer, "anvil/construction/lapis_lazuli");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 27, OverlayType.SMITHING)
                .input(ore("oredict/dust/diamond"), 1)
                .output(stack("minecraft:diamond", 1))
                .save(writer, "anvil/construction/diamond");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 28, OverlayType.SMITHING)
                .input(ore("oredict/dust/emerald"), 1)
                .output(stack("minecraft:emerald", 1))
                .save(writer, "anvil/construction/emerald");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 29, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone_bricks")), 16, true)
                .input(Ingredient.of(item("hbm_m:firebrick")), 16, true)
                .input(ore("oredict/ingot/iron"), 8)
                .input(ore("oredict/ingot/copper"), 8)
                .output(stack("hbm_m:annihilator", 1))
                .save(writer, "anvil/construction/annihilator");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 30, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .output(stack("hbm_m:platemetal_base", 4))
                .save(writer, "anvil/construction/platemetal_base");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 31, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/aluminum"), 1)
                .output(stack("hbm_m:deco_aluminum", 4))
                .save(writer, "anvil/construction/deco_aluminum");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 32, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/beryllium"), 1)
                .output(stack("hbm_m:deco_beryllium", 4))
                .save(writer, "anvil/construction/deco_beryllium");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 33, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/lead"), 1)
                .output(stack("hbm_m:deco_lead", 4))
                .save(writer, "anvil/construction/deco_lead");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 34, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/mingrade"), 1)
                .output(stack("hbm_m:deco_red_copper", 4))
                .save(writer, "anvil/construction/deco_red_copper");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 35, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/steel"), 1)
                .output(stack("hbm_m:deco_steel", 4))
                .save(writer, "anvil/construction/deco_steel");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 36, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/titanium"), 1)
                .output(stack("hbm_m:deco_titanium", 4))
                .save(writer, "anvil/construction/deco_titanium");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 37, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/tungsten"), 1)
                .output(stack("hbm_m:deco_tungsten", 4))
                .save(writer, "anvil/construction/deco_tungsten");

        // Stufe 1916169, CONSTRUCTION
        AnvilRecipeBuilder.construction(1916169, 38, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/dineutronium"), 4)
                .input(Ingredient.of(item("hbm_m:depth_brick")), 1, true)
                .output(stack("hbm_m:depth_dnt", 1))
                .save(writer, "anvil/construction/depth_dnt");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 39, OverlayType.SMITHING)
                .input(ore("oredict/plate/titanium"), 4)
                .output(stack("hbm_m:shell_titanium", 1))
                .save(writer, "anvil/construction/shell_titanium");
    }

    private static void construction1(Consumer<FinishedRecipe> writer) {
        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 40, OverlayType.SMITHING)
                .input(ore("oredict/plate/copper"), 4)
                .output(stack("hbm_m:shell_copper", 1))
                .save(writer, "anvil/construction/shell_copper");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 41, OverlayType.SMITHING)
                .input(ore("oredict/plate/aluminum"), 4)
                .output(stack("hbm_m:shell_aluminum", 1))
                .save(writer, "anvil/construction/shell_aluminum");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 42, OverlayType.SMITHING)
                .input(ore("oredict/plate/steel"), 4)
                .output(stack("hbm_m:shell_steel", 1))
                .save(writer, "anvil/construction/shell_steel");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 43, OverlayType.SMITHING)
                .input(ore("oredict/plate/weapon_steel"), 4)
                .output(stack("hbm_m:shell_weaponsteel", 1))
                .save(writer, "anvil/construction/shell_weaponsteel");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 44, OverlayType.SMITHING)
                .input(ore("oredict/plate/saturnite"), 4)
                .output(stack("hbm_m:shell_saturnite", 1))
                .save(writer, "anvil/construction/shell_saturnite");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 45, OverlayType.SMITHING)
                .input(ore("oredict/plate/iron"), 3)
                .output(stack("hbm_m:pipe_iron", 1))
                .save(writer, "anvil/construction/pipe_iron");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 46, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 47, OverlayType.SMITHING)
                .input(ore("oredict/plate/copper"), 3)
                .output(stack("hbm_m:pipe_copper", 1))
                .save(writer, "anvil/construction/pipe_copper");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 48, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus_2");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 49, OverlayType.SMITHING)
                .input(ore("oredict/plate/aluminum"), 3)
                .output(stack("hbm_m:pipe_aluminum", 1))
                .save(writer, "anvil/construction/pipe_aluminum");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 50, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus_3");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 51, OverlayType.SMITHING)
                .input(ore("oredict/plate/lead"), 3)
                .output(stack("hbm_m:pipe_lead", 1))
                .save(writer, "anvil/construction/pipe_lead");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 52, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus_4");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 53, OverlayType.SMITHING)
                .input(ore("oredict/plate/steel"), 3)
                .output(stack("hbm_m:pipe_steel", 1))
                .save(writer, "anvil/construction/pipe_steel");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 54, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus_5");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 55, OverlayType.SMITHING)
                .input(ore("oredict/plate/dura_steel"), 3)
                .output(stack("hbm_m:pipe_dura_steel", 1))
                .save(writer, "anvil/construction/pipe_dura_steel");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 56, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus_6");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 57, OverlayType.SMITHING)
                .input(ore("oredict/ingot/rubber"), 3)
                .output(stack("hbm_m:pipe_rubber", 1))
                .save(writer, "anvil/construction/pipe_rubber");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 58, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 2, true)
                .output(stack("hbm_m:coil_copper_torus", 1))
                .save(writer, "anvil/construction/coil_copper_torus_7");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 59, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:coil_gold")), 2, true)
                .output(stack("hbm_m:coil_gold_torus", 1))
                .save(writer, "anvil/construction/coil_gold_torus");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 60, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 2)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 1, true)
                .input(Ingredient.of(item("hbm_m:coil_copper_torus")), 1, true)
                .output(stack("hbm_m:motor", 2))
                .save(writer, "anvil/construction/motor");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 61, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:motor")), 1, true)
                .input(ore("oredict/ingot/any_plastic"), 2)
                .input(ore("oredict/ingot/workers_alloy"), 2)
                .input(ore("oredict/wire_dense/gold"), 1)
                .output(stack("hbm_m:motor_desh", 1))
                .save(writer, "anvil/construction/motor_desh");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 62, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone_bricks")), 4, true)
                .input(Ingredient.of(item("hbm_m:firebrick")), 32, true)
                .input(ore("oredict/plate/copper"), 8)
                .output(stack("hbm_m:machine_blast_furnace", 1))
                .save(writer, "anvil/construction/machine_blast_furnace");

        // Stufe 2, CONSTRUCTION (Original AnvilRecipes l.228: Steinmuehle)
        AnvilRecipeBuilder.construction(2, 62, OverlayType.CONSTRUCTION)
                .input(ore("oredict/stone"), 16)
                .input(ore("oredict/plate/steel"), 4)
                .input(ore("oredict/ntmpipe/copper"), 1)
                .input(Ingredient.of(item("hbm_m:motor")), 1, true)
                .output(stack("hbm_m:machine_rockmill", 1))
                .save(writer, "anvil/construction/machine_rockmill");

        // Stufe 2, CONSTRUCTION [Fassungen: expensive]
        ConfigRecipes.variants(writer)
                .base(w -> AnvilRecipeBuilder.construction(2, 63, OverlayType.CONSTRUCTION)
                    .input(ore("oredict/ingot/steel"), 8)
                    .input(ore("oredict/plate/copper"), 4)
                    .input(Ingredient.of(item("hbm_m:motor")), 2, true)
                    .input(Ingredient.of(item("hbm_m:vacuum_tube")), 4, true)
                    .output(stack("hbm_m:advanced_assembly_machine", 1))
                    .save(w, "anvil/construction/advanced_assembly_machine"))
                .variant(v -> AnvilRecipeBuilder.construction(2, 63, OverlayType.CONSTRUCTION)
                    .input(ore("oredict/ingot/steel"), 8)
                    .input(ore("oredict/plate/copper"), 4)
                    .input(Ingredient.of(item("hbm_m:motor")), 2, true)
                    .input(Ingredient.of(item("hbm_m:analog_circuit")), 2, true)
                    .output(stack("hbm_m:advanced_assembly_machine", 1))
                    .save(v, "anvil/construction/advanced_assembly_machine"), "expensive")
                .save();

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 64, OverlayType.CONSTRUCTION)
                .input(ore("oredict/cobblestone"), 8)
                .input(ore("oredict/plank_wood"), 16)
                .input(ore("oredict/plate/copper"), 8)
                .input(ore("oredict/ntmpipe/lead"), 2)
                .output(stack("hbm_m:pump_steam", 1))
                .save(writer, "anvil/construction/pump_steam");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 65, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone_bricks")), 8, true)
                .input(ore("oredict/plate/steel"), 16)
                .input(ore("oredict/ntmpipe/lead"), 4)
                .input(Ingredient.of(item("hbm_m:motor")), 2, true)
                .input(Ingredient.of(item("hbm_m:vacuum_tube")), 4, true)
                .output(stack("hbm_m:pump_electric", 1))
                .save(writer, "anvil/construction/pump_electric");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 66, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:furnace")), 1, true)
                .input(ore("oredict/plate/steel"), 8)
                .input(ore("oredict/ingot/copper"), 8)
                .output(stack("hbm_m:firebox", 1))
                .save(writer, "anvil/construction/firebox");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 67, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:firebrick")), 16, true)
                .input(ore("oredict/plate/steel"), 4)
                .input(ore("oredict/ingot/copper"), 8)
                .output(stack("hbm_m:heating_oven", 1))
                .save(writer, "anvil/construction/heating_oven");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 68, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone")), 8, true)
                .input(ore("oredict/plate/steel"), 2)
                .input(ore("oredict/ingot/iron"), 4)
                .output(stack("hbm_m:ashpit", 1))
                .save(writer, "anvil/construction/ashpit");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 69, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:tank_steel")), 4, true)
                .input(ore("oredict/ntmpipe/steel"), 3)
                .input(ore("oredict/ingot/titanium"), 12)
                .input(ore("oredict/ingot/copper"), 8)
                .output(stack("hbm_m:oilburner", 1))
                .save(writer, "anvil/construction/oilburner");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 70, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/any_plastic"), 4)
                .input(ore("oredict/ingot/copper"), 8)
                .input(ore("oredict/plate/steel"), 8)
                .input(Ingredient.of(item("hbm_m:coil_tungsten")), 8, true)
                .input(Ingredient.of(item("hbm_m:integrated_circuit")), 1, true)
                .output(stack("hbm_m:electric_heater", 1))
                .save(writer, "anvil/construction/electric_heater");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 71, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/rubber"), 4)
                .input(ore("oredict/ingot/copper"), 16)
                .input(ore("oredict/plate/steel"), 16)
                .input(ore("oredict/ntmpipe/steel"), 3)
                .output(stack("hbm_m:heatex", 1))
                .save(writer, "anvil/construction/heatex");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 72, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone_bricks")), 16, true)
                .input(ore("oredict/ingot/iron"), 4)
                .input(ore("oredict/plate/steel"), 16)
                .input(ore("oredict/ingot/copper"), 8)
                .input(Ingredient.of(item("hbm_m:steel_grate")), 16, true)
                .output(stack("hbm_m:furnace_steel", 1))
                .save(writer, "anvil/construction/furnace_steel");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 73, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone_bricks")), 8, true)
                .input(ore("oredict/log_wood"), 16)
                .input(ore("oredict/plate_triple/copper"), 2)
                .input(ore("oredict/ingot/brick"), 16)
                .output(stack("hbm_m:combination_oven", 1))
                .save(writer, "anvil/construction/combination_oven");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 74, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:stone_bricks")), 8, true)
                .input(Ingredient.of(item("hbm_m:firebrick")), 16, true)
                .input(ore("oredict/ingot/iron"), 4)
                .input(ore("oredict/plate/copper"), 8)
                .output(stack("hbm_m:rotary_furnace", 1))
                .save(writer, "anvil/construction/rotary_furnace");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 75, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plank_wood"), 16)
                .input(ore("oredict/plate/steel"), 6)
                .input(ore("oredict/ingot/copper"), 8)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 4, true)
                .input(Ingredient.of(item("hbm_m:gear_large")), 1, true)
                .output(stack("hbm_m:stirling", 1))
                .save(writer, "anvil/construction/stirling");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 76, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 16)
                .input(ore("oredict/ingot/beryllium"), 6)
                .input(ore("oredict/ingot/copper"), 4)
                .input(Ingredient.of(item("hbm_m:coil_gold")), 8, true)
                .input(Ingredient.of(item("hbm_m:gear_large_steel")), 1, true)
                .output(stack("hbm_m:stirling_steel", 1))
                .save(writer, "anvil/construction/stirling_steel");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 77, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:reinforced_stone")), 16, true)
                .input(ore("oredict/plate/steel"), 12)
                .input(ore("oredict/shell/steel"), 2)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 4, true)
                .input(Ingredient.of(item("hbm_m:gear_large")), 1, true)
                .output(stack("hbm_m:steam_engine", 1))
                .save(writer, "anvil/construction/steam_engine");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 78, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plank_wood"), 16)
                .input(ore("oredict/plate/steel"), 6)
                .input(ore("oredict/ingot/copper"), 8)
                .input(ore("oredict/ingot/iron"), 4)
                .input(Ingredient.of(item("hbm_m:sawblade")), 1, true)
                .output(stack("hbm_m:sawmill", 1))
                .save(writer, "anvil/construction/sawmill");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 79, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:firebrick")), 20, true)
                .input(ore("oredict/ingot/copper"), 8)
                .input(ore("oredict/plate/steel"), 8)
                .output(stack("hbm_m:crucible", 1))
                .save(writer, "anvil/construction/crucible");
    }

    private static void construction2(Consumer<FinishedRecipe> writer) {
        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 80, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/steel"), 4)
                .input(ore("oredict/plate/copper"), 16)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 8, true)
                .output(stack("hbm_m:boiler", 1))
                .save(writer, "anvil/construction/boiler");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 81, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate_triple/steel"), 2)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 4, true)
                .input(ore("oredict/bolt/tungsten"), 4)
                .input(Ingredient.of(item("hbm_m:vacuum_tube")), 2, true)
                .output(stack("hbm_m:soldering_station", 1))
                .save(writer, "anvil/construction/soldering_station");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 82, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate_triple/steel"), 4)
                .input(ore("oredict/ingot/tungsten"), 8)
                .input(Ingredient.of(item("hbm_m:machine_transformer")), 1, true)
                .input(Ingredient.of(item("hbm_m:arc_electrode")), 2, true)
                .output(stack("hbm_m:arc_welder", 1))
                .save(writer, "anvil/construction/arc_welder");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 83, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate_triple/steel"), 8)
                .input(ore("oredict/ingot/copper"), 8)
                .input(ore("oredict/ingot/any_plastic"), 4)
                .output(stack("hbm_m:industrial_boiler", 1))
                .save(writer, "anvil/construction/industrial_boiler");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 84, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 4)
                .input(ore("oredict/ingot/iron"), 12)
                .input(ore("oredict/ingot/copper"), 2)
                .input(Ingredient.of(item("hbm_m:vacuum_tube")), 2, true)
                .input(Ingredient.of(item("hbm_m:sawblade")), 1, true)
                .output(stack("hbm_m:autosaw", 1))
                .save(writer, "anvil/construction/autosaw");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 85, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 8)
                .input(ore("oredict/ingot/iron"), 12)
                .input(ore("oredict/ingot/copper"), 2)
                .input(Ingredient.of(item("hbm_m:vacuum_tube")), 1, true)
                .output(stack("hbm_m:thresher", 1))
                .save(writer, "anvil/construction/thresher");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 86, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:brick_concrete")), 64, true)
                .input(Ingredient.of(item("minecraft:iron_bars")), 128, true)
                .input(Ingredient.of(item("hbm_m:steam_condenser")), 4, true)
                .output(stack("hbm_m:tower_small", 1))
                .save(writer, "anvil/construction/tower_small");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 87, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:concrete_smooth")), 128, true)
                .input(Ingredient.of(item("hbm_m:steel_scaffold")), 32, true)
                .input(Ingredient.of(item("hbm_m:steam_condenser")), 16, true)
                .input(ore("oredict/ntmpipe/steel"), 8)
                .output(stack("hbm_m:cooling_tower", 1))
                .save(writer, "anvil/construction/cooling_tower");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 88, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("minecraft:bone")), 16, true)
                .input(Ingredient.of(item("minecraft:leather")), 4, true)
                .input(Ingredient.of(item("minecraft:feather")), 24, true)
                .output(stack("hbm_m:wings_limp", 1))
                .save(writer, "anvil/construction/wings_limp");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 89, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:sulfur")), 12, true)
                .input(ore("oredict/shell/steel"), 4)
                .input(ore("oredict/plate_triple/copper"), 6)
                .input(Ingredient.of(item("hbm_m:integrated_circuit")), 2, true)
                .output(stack("hbm_m:machine_deuterium_extractor", 1))
                .save(writer, "anvil/construction/machine_deuterium_extractor");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 90, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:deuterium_filter")), 2, true)
                .input(ore("oredict/shell/steel"), 5)
                .input(ore("oredict/ntmpipe/steel"), 12)
                .input(Ingredient.of(item("hbm_m:concrete_asbestos")), 8, true)
                .input(Ingredient.of(item("hbm_m:steel_scaffold")), 16, true)
                .input(container(ModFluids.SOURGAS.getSource(), 1000), 8)
                .output(stack("hbm_m:deuterium_tower", 1))
                .save(writer, "anvil/construction/deuterium_tower");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 91, OverlayType.CONSTRUCTION)
                .input(ore("oredict/any/concrete"), 2)
                .input(Ingredient.of(item("hbm_m:steel_scaffold")), 8, true)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 8, true)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 4, true)
                .output(stack("hbm_m:red_pylon_large", 1))
                .save(writer, "anvil/construction/red_pylon_large");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 92, OverlayType.CONSTRUCTION)
                .input(ore("oredict/any/concrete"), 8)
                .input(ore("oredict/ingot/steel"), 8)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 12, true)
                .input(Ingredient.of(item("hbm_m:coil_copper")), 8, true)
                .output(stack("hbm_m:substation", 2))
                .save(writer, "anvil/construction/substation");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 93, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 4)
                .input(Ingredient.of(item("minecraft:bricks")), 16, true)
                .input(Ingredient.of(item("hbm_m:steel_grate")), 2, true)
                .output(stack("hbm_m:chimney_brick", 1))
                .save(writer, "anvil/construction/chimney_brick");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 94, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 16)
                .input(ore("oredict/any/concrete"), 64)
                .input(Ingredient.of(item("hbm_m:steel_grate")), 4, true)
                .input(Ingredient.of(item("hbm_m:filter_coal")), 4, true)
                .output(stack("hbm_m:chimney_industrial", 1))
                .save(writer, "anvil/construction/chimney_industrial");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 95, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:tank_steel")), 1, true)
                .input(ore("oredict/plate/lead"), 2)
                .input(Ingredient.of(item("hbm_m:nuclear_waste")), 10, true)
                .output(stack("hbm_m:yellow_barrel", 1))
                .save(writer, "anvil/construction/yellow_barrel");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 96, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:tank_steel")), 1, true)
                .input(ore("oredict/plate/lead"), 2)
                .input(Ingredient.of(item("hbm_m:nuclear_waste_vitrified")), 10, true)
                .output(stack("hbm_m:vitrified_barrel", 1))
                .save(writer, "anvil/construction/vitrified_barrel");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 97, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:fat_man_core")), 1, true)
                .input(ore("oredict/ingot/beryllium"), 4)
                .input(Ingredient.of(item("hbm_m:screwdriver")), 1, true)
                .output(stack("hbm_m:demon_core_open", 1))
                .save(writer, "anvil/construction/demon_core_open");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 98, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/workers_alloy"), 4)
                .input(ore("oredict/dust/any_plastic"), 2)
                .input(ore("oredict/ingot/dura_steel"), 1)
                .output(stack("hbm_m:plate_desh", 4))
                .save(writer, "anvil/construction/plate_desh");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 99, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:nugget_bismuth")), 2, true)
                .input(ore("oredict/billet/uranium238"), 2)
                .input(ore("oredict/dust/niobium"), 1)
                .output(stack("hbm_m:plate_bismuth", 1))
                .save(writer, "anvil/construction/plate_bismuth");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 100, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/titanium"), 2)
                .input(ore("oredict/ingot/steel"), 1)
                .input(ore("oredict/bolt/steel"), 4)
                .output(stack("hbm_m:plate_armor_titanium", 1))
                .save(writer, "anvil/construction/plate_armor_titanium");

        // Stufe 3, CONSTRUCTION
        AnvilRecipeBuilder.construction(3, 101, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 6)
                .input(ore("oredict/ingot/niobium"), 1)
                .input(Ingredient.of(item("hbm_m:plate_armor_titanium")), 1, true)
                .output(stack("hbm_m:plate_armor_ajr", 2))
                .save(writer, "anvil/construction/plate_armor_ajr");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 102, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/dura_steel"), 4)
                .input(Ingredient.of(item("hbm_m:plate_armor_titanium")), 1, true)
                .input(ore("oredict/wire_fine/tungsten"), 8)
                .output(stack("hbm_m:plate_armor_hev", 1))
                .save(writer, "anvil/construction/plate_armor_hev");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 103, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/weapon_steel"), 4)
                .input(ore("oredict/ingot/starmetal"), 1)
                .input(ore("oredict/wire_fine/magnetized_tungsten"), 8)
                .output(stack("hbm_m:plate_armor_lunar", 1))
                .save(writer, "anvil/construction/plate_armor_lunar");

        // Stufe 6, CONSTRUCTION
        AnvilRecipeBuilder.construction(6, 104, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:meteorite_forged_ingot")), 4, true)
                .input(ore("oredict/ingot/workers_alloy"), 1)
                .input(Ingredient.of(item("hbm_m:billet_yharonite")), 1, true)
                .output(stack("hbm_m:plate_armor_fau", 1))
                .save(writer, "anvil/construction/plate_armor_fau");

        // Stufe 7, CONSTRUCTION
        AnvilRecipeBuilder.construction(7, 105, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:plate_dineutronium")), 4, true)
                .input(Ingredient.of(item("hbm_m:particle_sparkticle")), 1, true)
                .input(Ingredient.of(item("hbm_m:plate_armor_fau")), 6, true)
                .output(stack("hbm_m:plate_armor_dnt", 1))
                .save(writer, "anvil/construction/plate_armor_dnt");

        // Stufe 5, CONSTRUCTION
        AnvilRecipeBuilder.construction(5, 106, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:missile_doomsday_rusted")), 1, true)
                .input(ore("oredict/ingot/any_hard_plastic"), 8)
                .input(ore("oredict/plate_sextuple/aluminum"), 2)
                .input(ore("oredict/billet/plutonium239"), 3)
                .output(stack("hbm_m:missile_doomsday", 1))
                .save(writer, "anvil/construction/missile_doomsday");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 107, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 0, 1))
                .save(writer, "anvil/construction/fluid_duct_box_0");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 108, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/copper"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 1, 1))
                .save(writer, "anvil/construction/fluid_duct_box_1");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 109, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/aluminum"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 2, 1))
                .save(writer, "anvil/construction/fluid_duct_box_2");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 110, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 0), 1, true)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_0");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 111, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 1), 1, true)
                .output(stack("hbm_m:plate_copper", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_1");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 112, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 2), 1, true)
                .output(stack("hbm_m:plate_aluminium", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_2");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 113, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.FLUID_DUCT_EXHAUST, 0, 8))
                .save(writer, "anvil/construction/fluid_duct_exhaust_0");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 114, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_EXHAUST, 0), 8, true)
                .output(stack("hbm_m:plate_iron", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_exhaust_0");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 115, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 3, 1))
                .save(writer, "anvil/construction/fluid_duct_box_3");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 116, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/copper"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 4, 1))
                .save(writer, "anvil/construction/fluid_duct_box_4");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 117, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/aluminum"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 5, 1))
                .save(writer, "anvil/construction/fluid_duct_box_5");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 118, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 3), 1, true)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_3");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 119, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 4), 1, true)
                .output(stack("hbm_m:plate_copper", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_4");
    }

    private static void construction3(Consumer<FinishedRecipe> writer) {
        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 120, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 5), 1, true)
                .output(stack("hbm_m:plate_aluminium", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_5");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 121, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.FLUID_DUCT_EXHAUST, 3, 8))
                .save(writer, "anvil/construction/fluid_duct_exhaust_3");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 122, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_EXHAUST, 3), 8, true)
                .output(stack("hbm_m:plate_iron", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_exhaust_3");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 123, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 6, 1))
                .save(writer, "anvil/construction/fluid_duct_box_6");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 124, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/copper"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 7, 1))
                .save(writer, "anvil/construction/fluid_duct_box_7");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 125, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/aluminum"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 8, 1))
                .save(writer, "anvil/construction/fluid_duct_box_8");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 126, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 6), 1, true)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_6");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 127, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 7), 1, true)
                .output(stack("hbm_m:plate_copper", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_7");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 128, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 8), 1, true)
                .output(stack("hbm_m:plate_aluminium", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_8");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 129, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.FLUID_DUCT_EXHAUST, 6, 8))
                .save(writer, "anvil/construction/fluid_duct_exhaust_6");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 130, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_EXHAUST, 6), 8, true)
                .output(stack("hbm_m:plate_iron", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_exhaust_6");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 131, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 9, 1))
                .save(writer, "anvil/construction/fluid_duct_box_9");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 132, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/copper"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 10, 1))
                .save(writer, "anvil/construction/fluid_duct_box_10");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 133, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/aluminum"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 11, 1))
                .save(writer, "anvil/construction/fluid_duct_box_11");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 134, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 9), 1, true)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_9");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 135, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 10), 1, true)
                .output(stack("hbm_m:plate_copper", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_10");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 136, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 11), 1, true)
                .output(stack("hbm_m:plate_aluminium", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_11");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 137, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.FLUID_DUCT_EXHAUST, 9, 8))
                .save(writer, "anvil/construction/fluid_duct_exhaust_9");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 138, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_EXHAUST, 9), 8, true)
                .output(stack("hbm_m:plate_iron", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_exhaust_9");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 139, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 12, 1))
                .save(writer, "anvil/construction/fluid_duct_box_12");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 140, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/copper"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 13, 1))
                .save(writer, "anvil/construction/fluid_duct_box_13");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 141, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/aluminum"), 1)
                .output(boxStack(ModBlocks.FLUID_DUCT_BOX, 14, 1))
                .save(writer, "anvil/construction/fluid_duct_box_14");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 142, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 12), 1, true)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_12");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 143, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 13), 1, true)
                .output(stack("hbm_m:plate_copper", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_13");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 144, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_BOX, 14), 1, true)
                .output(stack("hbm_m:plate_aluminium", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_box_14");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 145, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/iron"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.FLUID_DUCT_EXHAUST, 12, 8))
                .save(writer, "anvil/construction/fluid_duct_exhaust_12");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 146, OverlayType.RECYCLING)
                .input(box(ModBlocks.FLUID_DUCT_EXHAUST, 12), 8, true)
                .output(stack("hbm_m:plate_iron", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_fluid_duct_exhaust_12");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 147, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/mingrade"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.RED_CABLE_BOX, 0, 16))
                .save(writer, "anvil/construction/red_cable_box_0");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 148, OverlayType.RECYCLING)
                .input(box(ModBlocks.RED_CABLE_BOX, 0), 16, true)
                .output(stack("hbm_m:red_copper_ingot", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_red_cable_box_0");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 149, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/mingrade"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.RED_CABLE_BOX, 1, 16))
                .save(writer, "anvil/construction/red_cable_box_1");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 150, OverlayType.RECYCLING)
                .input(box(ModBlocks.RED_CABLE_BOX, 1), 16, true)
                .output(stack("hbm_m:red_copper_ingot", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_red_cable_box_1");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 151, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/mingrade"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.RED_CABLE_BOX, 2, 16))
                .save(writer, "anvil/construction/red_cable_box_2");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 152, OverlayType.RECYCLING)
                .input(box(ModBlocks.RED_CABLE_BOX, 2), 16, true)
                .output(stack("hbm_m:red_copper_ingot", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_red_cable_box_2");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 153, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/mingrade"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.RED_CABLE_BOX, 3, 16))
                .save(writer, "anvil/construction/red_cable_box_3");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 154, OverlayType.RECYCLING)
                .input(box(ModBlocks.RED_CABLE_BOX, 3), 16, true)
                .output(stack("hbm_m:red_copper_ingot", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_red_cable_box_3");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 155, OverlayType.CONSTRUCTION)
                .input(ore("oredict/ingot/mingrade"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(boxStack(ModBlocks.RED_CABLE_BOX, 4, 16))
                .save(writer, "anvil/construction/red_cable_box_4");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 156, OverlayType.RECYCLING)
                .input(box(ModBlocks.RED_CABLE_BOX, 4), 16, true)
                .output(stack("hbm_m:red_copper_ingot", 1))
                .output(stack("hbm_m:plate_polymer", 1))
                .save(writer, "anvil/construction/recycle_red_cable_box_4");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 157, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_stone_flat")), 1, true)
                .output(stack("hbm_m:stamp_stone_plate", 1))
                .save(writer, "anvil/construction/stamp_stone_plate");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 158, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_stone_flat")), 1, true)
                .output(stack("hbm_m:stamp_stone_wire", 1))
                .save(writer, "anvil/construction/stamp_stone_wire");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 159, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_stone_flat")), 1, true)
                .output(stack("hbm_m:stamp_stone_circuit", 1))
                .save(writer, "anvil/construction/stamp_stone_circuit");
    }

    private static void construction4(Consumer<FinishedRecipe> writer) {
        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 160, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_iron_flat")), 1, true)
                .output(stack("hbm_m:stamp_iron_plate", 1))
                .save(writer, "anvil/construction/stamp_iron_plate");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 161, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_iron_flat")), 1, true)
                .output(stack("hbm_m:stamp_iron_wire", 1))
                .save(writer, "anvil/construction/stamp_iron_wire");

        // Stufe 1, SMITHING
        AnvilRecipeBuilder.construction(1, 162, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_iron_flat")), 1, true)
                .output(stack("hbm_m:stamp_iron_circuit", 1))
                .save(writer, "anvil/construction/stamp_iron_circuit");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 163, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_steel_flat")), 1, true)
                .output(stack("hbm_m:stamp_steel_plate", 1))
                .save(writer, "anvil/construction/stamp_steel_plate");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 164, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_steel_flat")), 1, true)
                .output(stack("hbm_m:stamp_steel_wire", 1))
                .save(writer, "anvil/construction/stamp_steel_wire");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 165, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_steel_flat")), 1, true)
                .output(stack("hbm_m:stamp_steel_circuit", 1))
                .save(writer, "anvil/construction/stamp_steel_circuit");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 166, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_titanium_flat")), 1, true)
                .output(stack("hbm_m:stamp_titanium_plate", 1))
                .save(writer, "anvil/construction/stamp_titanium_plate");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 167, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_titanium_flat")), 1, true)
                .output(stack("hbm_m:stamp_titanium_wire", 1))
                .save(writer, "anvil/construction/stamp_titanium_wire");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 168, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_titanium_flat")), 1, true)
                .output(stack("hbm_m:stamp_titanium_circuit", 1))
                .save(writer, "anvil/construction/stamp_titanium_circuit");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 169, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_obsidian_flat")), 1, true)
                .output(stack("hbm_m:stamp_obsidian_plate", 1))
                .save(writer, "anvil/construction/stamp_obsidian_plate");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 170, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_obsidian_flat")), 1, true)
                .output(stack("hbm_m:stamp_obsidian_wire", 1))
                .save(writer, "anvil/construction/stamp_obsidian_wire");

        // Stufe 2, SMITHING
        AnvilRecipeBuilder.construction(2, 171, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_obsidian_flat")), 1, true)
                .output(stack("hbm_m:stamp_obsidian_circuit", 1))
                .save(writer, "anvil/construction/stamp_obsidian_circuit");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 172, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_desh_flat")), 1, true)
                .output(stack("hbm_m:stamp_desh_plate", 1))
                .save(writer, "anvil/construction/stamp_desh_plate");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 173, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_desh_flat")), 1, true)
                .output(stack("hbm_m:stamp_desh_wire", 1))
                .save(writer, "anvil/construction/stamp_desh_wire");

        // Stufe 3, SMITHING
        AnvilRecipeBuilder.construction(3, 174, OverlayType.SMITHING)
                .input(Ingredient.of(item("hbm_m:stamp_desh_flat")), 1, true)
                .output(stack("hbm_m:stamp_desh_circuit", 1))
                .save(writer, "anvil/construction/stamp_desh_circuit");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 175, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:stamp_iron_flat")), 1, true)
                .input(ore("oredict/ingot/gun_metal"), 2)
                .output(stack("hbm_m:stamp_9", 1))
                .save(writer, "anvil/construction/stamp_9");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 176, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:stamp_iron_flat")), 1, true)
                .input(ore("oredict/ingot/gun_metal"), 2)
                .output(stack("hbm_m:stamp_50", 1))
                .save(writer, "anvil/construction/stamp_50");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 177, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:stamp_desh_flat")), 1, true)
                .input(ore("oredict/ingot/weapon_steel"), 4)
                .output(stack("hbm_m:stamp_desh_9", 1))
                .save(writer, "anvil/construction/stamp_desh_9");

        // Stufe 4, CONSTRUCTION
        AnvilRecipeBuilder.construction(4, 178, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:stamp_desh_flat")), 1, true)
                .input(ore("oredict/ingot/weapon_steel"), 4)
                .output(stack("hbm_m:stamp_desh_50", 1))
                .save(writer, "anvil/construction/stamp_desh_50");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 179, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/iron"), 2)
                .output(stack("hbm_m:mold_c9", 1))
                .save(writer, "anvil/construction/mold_c9");

        // Stufe 1, CONSTRUCTION
        AnvilRecipeBuilder.construction(1, 180, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/iron"), 2)
                .output(stack("hbm_m:mold_c50", 1))
                .save(writer, "anvil/construction/mold_c50");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 181, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_barrel_light", 1))
                .save(writer, "anvil/construction/mold_barrel_light");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 182, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_barrel_heavy", 1))
                .save(writer, "anvil/construction/mold_barrel_heavy");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 183, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_receiver_light", 1))
                .save(writer, "anvil/construction/mold_receiver_light");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 184, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_receiver_heavy", 1))
                .save(writer, "anvil/construction/mold_receiver_heavy");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 185, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_mechanism", 1))
                .save(writer, "anvil/construction/mold_mechanism");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 186, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_stock", 1))
                .save(writer, "anvil/construction/mold_stock");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 187, OverlayType.CONSTRUCTION)
                .input(Ingredient.of(item("hbm_m:mold_base")), 1, true)
                .input(ore("oredict/ingot/steel"), 4)
                .output(stack("hbm_m:mold_grip", 1))
                .save(writer, "anvil/construction/mold_grip");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 188, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_hatch", 1))
                .save(writer, "anvil/construction/cassette_hatch");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 189, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_autopilot", 1))
                .save(writer, "anvil/construction/cassette_autopilot");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 190, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_ams_siren", 1))
                .save(writer, "anvil/construction/cassette_ams_siren");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 191, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_blast_door", 1))
                .save(writer, "anvil/construction/cassette_blast_door");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 192, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_apc_loop", 1))
                .save(writer, "anvil/construction/cassette_apc_loop");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 193, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_klaxon", 1))
                .save(writer, "anvil/construction/cassette_klaxon");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 194, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_klaxon_a", 1))
                .save(writer, "anvil/construction/cassette_klaxon_a");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 195, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_klaxon_b", 1))
                .save(writer, "anvil/construction/cassette_klaxon_b");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 196, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_regular_siren", 1))
                .save(writer, "anvil/construction/cassette_regular_siren");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 197, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_classic_siren", 1))
                .save(writer, "anvil/construction/cassette_classic_siren");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 198, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_bank_alarm", 1))
                .save(writer, "anvil/construction/cassette_bank_alarm");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 199, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_beep_siren", 1))
                .save(writer, "anvil/construction/cassette_beep_siren");
    }

    private static void construction5(Consumer<FinishedRecipe> writer) {
        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 200, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_container_alarm", 1))
                .save(writer, "anvil/construction/cassette_container_alarm");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 201, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_sweep_siren", 1))
                .save(writer, "anvil/construction/cassette_sweep_siren");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 202, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_strider_siren", 1))
                .save(writer, "anvil/construction/cassette_strider_siren");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 203, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_air_raid", 1))
                .save(writer, "anvil/construction/cassette_air_raid");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 204, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_nostromo_siren", 1))
                .save(writer, "anvil/construction/cassette_nostromo_siren");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 205, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_eas_alarm", 1))
                .save(writer, "anvil/construction/cassette_eas_alarm");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 206, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_apc_pass", 1))
                .save(writer, "anvil/construction/cassette_apc_pass");

        // Stufe 2, CONSTRUCTION
        AnvilRecipeBuilder.construction(2, 207, OverlayType.CONSTRUCTION)
                .input(ore("oredict/plate/steel"), 1)
                .input(Ingredient.of(item("hbm_m:plate_polymer")), 1, true)
                .output(stack("hbm_m:cassette_razortrain", 1))
                .save(writer, "anvil/construction/cassette_razortrain");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 208, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rareground_ore_chunk")), 1, true)
                .output(stack("hbm_m:fragment_boron", 1))
                .output(stack("hbm_m:fragment_boron", 1), 0.5F)
                .output(stack("hbm_m:fragment_lanthanium", 1), 0.1F)
                .output(stack("hbm_m:fragment_cobalt", 1))
                .output(stack("hbm_m:fragment_cobalt", 1), 0.5F)
                .output(stack("hbm_m:fragment_cerium", 1), 0.1F)
                .output(stack("hbm_m:fragment_neodymium", 1), 0.5F)
                .output(stack("hbm_m:fragment_niobium", 1), 0.5F)
                .save(writer, "anvil/construction/recycle_rareground_ore_chunk");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 209, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_titanium")), 4, true)
                .output(stack("hbm_m:titanium_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_titanium");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 210, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_red_copper")), 4, true)
                .output(stack("hbm_m:red_copper_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_red_copper");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 211, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_tungsten")), 4, true)
                .output(stack("hbm_m:tungsten_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_tungsten");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 212, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_aluminum")), 4, true)
                .output(stack("hbm_m:ingot_aluminium", 1))
                .save(writer, "anvil/construction/recycle_deco_aluminum");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 213, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_steel")), 4, true)
                .output(stack("hbm_m:steel_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_steel");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 214, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_rusty_steel")), 8, true)
                .output(stack("hbm_m:steel_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_rusty_steel");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 215, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_lead")), 4, true)
                .output(stack("hbm_m:lead_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_lead");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 216, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_beryllium")), 4, true)
                .output(stack("hbm_m:beryllium_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_beryllium");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 217, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:deco_asbestos")), 4, true)
                .output(stack("hbm_m:asbestos_ingot", 1))
                .save(writer, "anvil/construction/recycle_deco_asbestos");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 218, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:platemetal_base")), 4, true)
                .output(stack("hbm_m:plate_iron", 1))
                .save(writer, "anvil/construction/recycle_platemetal_base");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 219, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:firebox")), 1, true)
                .output(stack("hbm_m:plate_steel", 8))
                .output(stack("minecraft:copper_ingot", 6))
                .save(writer, "anvil/construction/recycle_firebox");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 220, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:heating_oven")), 1, true)
                .output(stack("hbm_m:firebrick", 16))
                .output(stack("minecraft:copper_ingot", 8))
                .save(writer, "anvil/construction/recycle_heating_oven");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 221, OverlayType.RECYCLING)
                .input(stirling(ModBlocks.STIRLING, 0), 1, true)
                .output(stack("hbm_m:plate_steel", 6))
                .output(stack("minecraft:copper_ingot", 8))
                .output(stack("hbm_m:coil_copper", 4))
                .output(stack("hbm_m:gear_large", 1))
                .save(writer, "anvil/construction/recycle_stirling");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 222, OverlayType.RECYCLING)
                .input(stirling(ModBlocks.STIRLING, 1), 1, true)
                .output(stack("hbm_m:plate_steel", 6))
                .output(stack("minecraft:copper_ingot", 8))
                .output(stack("hbm_m:coil_copper", 4))
                .save(writer, "anvil/construction/recycle_stirling_2");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 223, OverlayType.RECYCLING)
                .input(stirling(ModBlocks.STIRLING_STEEL, 0), 1, true)
                .output(stack("hbm_m:plate_steel", 16))
                .output(stack("hbm_m:beryllium_ingot", 6))
                .output(stack("minecraft:copper_ingot", 4))
                .output(stack("hbm_m:coil_gold", 8))
                .output(stack("hbm_m:gear_large_steel", 1))
                .save(writer, "anvil/construction/recycle_stirling_steel");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 224, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:gear_large_steel")), 1, true)
                .output(stack("hbm_m:plate_steel", 8))
                .output(stack("hbm_m:titanium_ingot", 1))
                .save(writer, "anvil/construction/recycle_gear_large_steel");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 225, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:gear_large")), 1, true)
                .output(stack("hbm_m:plate_iron", 8))
                .output(stack("minecraft:copper_ingot", 1))
                .save(writer, "anvil/construction/recycle_gear_large");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 226, OverlayType.RECYCLING)
                .input(stirling(ModBlocks.STIRLING_STEEL, 1), 1, true)
                .output(stack("hbm_m:plate_steel", 16))
                .output(stack("hbm_m:beryllium_ingot", 6))
                .output(stack("minecraft:copper_ingot", 4))
                .output(stack("hbm_m:coil_gold", 8))
                .save(writer, "anvil/construction/recycle_stirling_steel_2");

        // Stufe 3, RECYCLING
        AnvilRecipeBuilder.construction(3, 227, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:bat9000")), 1, true)
                .output(stack("hbm_m:plate_welded_tcalloy", 4))
                .output(stack("hbm_m:plate_steel", 16))
                .save(writer, "anvil/construction/recycle_bat9000");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 228, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:puter")), 1, true)
                .output(stack("hbm_m:crt_display", 1))
                .output(stack("hbm_m:scrap", 3))
                .output(stack("hbm_m:wire_copper", 4))
                .output(stack("hbm_m:pcb", 2))
                .output(stack("hbm_m:vacuum_tube", 1), 0.5F)
                .output(stack("hbm_m:capacitor", 1), 0.75F)
                .output(stack("hbm_m:capacitor", 1), 0.5F)
                .output(stack("hbm_m:analog_circuit", 1), 0.1F)
                .save(writer, "anvil/construction/recycle_puter");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 229, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:crt_clean"), item("hbm_m:crt_broken"), item("hbm_m:crt_bsod")), 1, true)
                .output(stack("hbm_m:crt_display", 1))
                .output(stack("hbm_m:scrap", 2))
                .output(stack("hbm_m:wire_copper", 2))
                .output(stack("hbm_m:wire_gold", 2), 0.25F)
                .output(stack("hbm_m:vacuum_tube", 1), 0.25F)
                .save(writer, "anvil/construction/recycle_crt_clean");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 230, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:toaster")), 1, true)
                .output(stack("hbm_m:plate_iron", 3))
                .output(stack("hbm_m:scrap", 1))
                .output(stack("hbm_m:coil_tungsten", 1))
                .output(stack("minecraft:bread", 1), 0.5F)
                .output(stack("hbm_m:fusion_core", 1), 0.01F)
                .save(writer, "anvil/construction/recycle_toaster");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 231, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:toaster_steel")), 1, true)
                .output(stack("hbm_m:plate_steel", 3))
                .output(stack("hbm_m:scrap", 1))
                .output(stack("hbm_m:coil_tungsten", 2))
                .output(stack("minecraft:bread", 1), 0.5F)
                .output(stack("hbm_m:battery_sc_ra226", 1), 0.1F)
                .output(stack("hbm_m:fusion_core", 1), 0.05F)
                .save(writer, "anvil/construction/recycle_toaster_steel");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 232, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:toaster_wood")), 1, true)
                .output(stack("hbm_m:sawdust_powder", 4))
                .output(stack("hbm_m:scrap", 1))
                .output(stack("hbm_m:coil_tungsten", 4))
                .output(stack("minecraft:bread", 1), 0.5F)
                .output(stack("hbm_m:fusion_core", 1), 0.5F)
                .output(stack("hbm_m:fusion_core", 1), 0.5F)
                .output(stack("hbm_m:gem_alexandrite", 1), 0.25F)
                .output(stack("hbm_m:flame_pony", 1), 0.01F)
                .save(writer, "anvil/construction/recycle_toaster_wood");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 233, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:radiorec")), 1, true)
                .output(stack("hbm_m:plate_steel", 4))
                .output(stack("hbm_m:wire_copper", 1))
                .output(stack("hbm_m:vacuum_tube", 1), 0.5F)
                .output(stack("hbm_m:polymer_ingot", 1), 0.25F)
                .save(writer, "anvil/construction/recycle_radiorec");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 234, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:tape_recorder")), 1, true)
                .output(stack("hbm_m:steel_ingot", 1))
                .output(stack("hbm_m:tungsten_ingot", 1), 0.25F)
                .save(writer, "anvil/construction/recycle_tape_recorder");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 235, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:antenna_top")), 1, true)
                .output(stack("hbm_m:tungsten_ingot", 3))
                .output(stack("hbm_m:red_copper_ingot", 1))
                .output(stack("hbm_m:beryllium_ingot", 2))
                .output(stack("hbm_m:beryllium_ingot", 1), 0.5F)
                .save(writer, "anvil/construction/recycle_antenna_top");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 236, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:pole_satellite_receiver")), 1, true)
                .output(stack("hbm_m:steel_ingot", 3))
                .output(stack("hbm_m:steel_ingot", 2), 0.5F)
                .output(stack("hbm_m:vacuum_tube", 1), 0.5F)
                .output(stack("hbm_m:wire_red_copper", 1))
                .save(writer, "anvil/construction/recycle_pole_satellite_receiver");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 237, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:file_cabinet")), 1, true)
                .output(stack("hbm_m:plate_steel", 2))
                .output(stack("hbm_m:plate_steel", 2), 0.5F)
                .output(stack("hbm_m:plate_polymer", 2), 0.25F)
                .output(stack("hbm_m:scrap", 1))
                .save(writer, "anvil/construction/recycle_file_cabinet");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 238, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:billet_ra226be")), 3, true)
                .output(stack("hbm_m:pile_rod_mk2_ra226be", 1))
                .save(writer, "anvil/construction/recycle_billet_ra226be");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 239, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:billet_po210be")), 3, true)
                .output(stack("hbm_m:pile_rod_mk2_po210be", 1))
                .save(writer, "anvil/construction/recycle_billet_po210be");
    }

    private static void construction6(Consumer<FinishedRecipe> writer) {
        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 240, OverlayType.RECYCLING)
                .input(ore("oredict/billet/zirconium"), 3)
                .output(stack("hbm_m:pile_rod_mk2_zr", 1))
                .save(writer, "anvil/construction/pile_rod_mk2_zr");

        // Stufe 2, RECYCLING
        AnvilRecipeBuilder.construction(2, 241, OverlayType.RECYCLING)
                .input(ore("oredict/billet/uranium"), 3)
                .output(stack("hbm_m:pile_rod_mk2_nu", 1))
                .save(writer, "anvil/construction/pile_rod_mk2_nu");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 242, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_moderator")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:block_graphite", 4))
                .save(writer, "anvil/construction/recycle_rbmk_moderator");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 243, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_absorber")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:boron_ingot", 8))
                .save(writer, "anvil/construction/recycle_rbmk_absorber");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 244, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_reflector")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:neutron_reflector", 8))
                .save(writer, "anvil/construction/recycle_rbmk_reflector");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 245, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_control")), 1, true)
                .output(stack("hbm_m:rbmk_absorber", 1))
                .output(stack("hbm_m:graphite_ingot", 2))
                .output(stack("hbm_m:motor", 2))
                .save(writer, "anvil/construction/recycle_rbmk_control");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 246, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_control_mod")), 1, true)
                .output(stack("hbm_m:rbmk_control", 1))
                .output(stack("hbm_m:block_graphite", 4))
                .output(stack("hbm_m:nugget_bismuth", 4))
                .save(writer, "anvil/construction/recycle_rbmk_control_mod");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 247, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_control_auto")), 1, true)
                .output(stack("hbm_m:rbmk_control", 1))
                .output(stack("hbm_m:advanced_circuit", 1))
                .output(stack("hbm_m:crt_display", 1))
                .save(writer, "anvil/construction/recycle_rbmk_control_auto");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 248, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_element_reasim")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:zirconium_ingot", 4))
                .output(stack("hbm_m:shell_steel", 2))
                .save(writer, "anvil/construction/recycle_rbmk_element_reasim");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 249, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_element_reasim_mod")), 1, true)
                .output(stack("hbm_m:rbmk_element_reasim", 1))
                .output(stack("hbm_m:block_graphite", 4))
                .output(stack("hbm_m:tcalloy_ingot", 4))
                .save(writer, "anvil/construction/recycle_rbmk_element_reasim_mod");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 250, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_outgasser")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:steel_grate", 6))
                .output(stack("hbm_m:tank_steel", 1))
                .output(stack("minecraft:hopper", 1))
                .save(writer, "anvil/construction/recycle_rbmk_outgasser");

        // Stufe 4, RECYCLING
        AnvilRecipeBuilder.construction(4, 251, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_storage")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:crate_steel", 2))
                .save(writer, "anvil/construction/recycle_rbmk_storage");

        // Stufe 4, RECYCLING [nur bei !528]
        AnvilRecipeBuilder.construction(4, 252, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_element")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:shell_steel", 2))
                .save(ConfigRecipes.when(writer, "!528"), "anvil/construction/recycle_rbmk_element");

        // Stufe 4, RECYCLING [nur bei !528]
        AnvilRecipeBuilder.construction(4, 253, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_element_mod")), 1, true)
                .output(stack("hbm_m:rbmk_element", 1))
                .output(stack("hbm_m:block_graphite", 4))
                .output(stack("hbm_m:nugget_bismuth", 4))
                .save(ConfigRecipes.when(writer, "!528"), "anvil/construction/recycle_rbmk_element_mod");

        // Stufe 4, RECYCLING [nur bei !528]
        AnvilRecipeBuilder.construction(4, 254, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_boiler")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:pipe_copper", 6))
                .output(stack("hbm_m:shell_copper", 2))
                .save(ConfigRecipes.when(writer, "!528"), "anvil/construction/recycle_rbmk_boiler");

        // Stufe 4, RECYCLING [nur bei !528]
        AnvilRecipeBuilder.construction(4, 255, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:rbmk_cooler")), 1, true)
                .output(stack("hbm_m:rbmk_blank", 1))
                .output(stack("hbm_m:steel_grate", 4))
                .output(stack("hbm_m:plate_polymer", 4))
                .save(ConfigRecipes.when(writer, "!528"), "anvil/construction/recycle_rbmk_cooler");

        // Stufe 4, RECYCLING [nur bei !528]
        AnvilRecipeBuilder.construction(4, 256, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:reactor_research")), 1, true)
                .output(stack("hbm_m:steel_ingot", 8))
                .output(stack("hbm_m:tcalloy_ingot", 4))
                .output(stack("hbm_m:motor_desh", 2))
                .output(stack("hbm_m:boron_ingot", 5))
                .output(stack("hbm_m:plate_lead", 8))
                .output(stack("hbm_m:crt_display", 3))
                .output(stack("hbm_m:integrated_circuit", 1))
                .output(stack("hbm_m:integrated_circuit", 1), 0.5F)
                .save(ConfigRecipes.when(writer, "!528"), "anvil/construction/recycle_reactor_research");

        // Stufe 3, RECYCLING
        AnvilRecipeBuilder.construction(3, 257, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:steam_turbine")), 1, true) // Original machine_turbine = Port steam_turbine
                .output(stack("hbm_m:turbine_titanium", 1))
                .output(stack("hbm_m:coil_copper", 2))
                .output(stack("hbm_m:steel_ingot", 4))
                .save(writer, "anvil/construction/recycle_turbine");

        // Stufe 3, RECYCLING
        AnvilRecipeBuilder.construction(3, 258, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:yellow_barrel")), 1, true)
                .output(stack("hbm_m:tank_steel", 1))
                .output(stack("hbm_m:plate_lead", 2))
                .output(stack("hbm_m:nuclear_waste", 10))
                .save(writer, "anvil/construction/recycle_yellow_barrel");

        // Stufe 3, RECYCLING
        AnvilRecipeBuilder.construction(3, 259, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:vitrified_barrel")), 1, true)
                .output(stack("hbm_m:tank_steel", 1))
                .output(stack("hbm_m:plate_lead", 2))
                .output(stack("hbm_m:nuclear_waste_vitrified", 10))
                .save(writer, "anvil/construction/recycle_vitrified_barrel");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 260, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:egg_glyphid")), 1, true)
                .output(stack("hbm_m:glyphid_meat", 2))
                .output(stack("hbm_m:glyphid_meat", 1), 0.5F)
                .output(stack("minecraft:bone", 1), 0.75F)
                .output(stack("minecraft:experience_bottle", 1), 0.5F)
                .save(writer, "anvil/construction/recycle_egg_glyphid");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 261, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:fusion_heater")), 1, true)
                .output(stack("hbm_m:pipe_steel", 4))
                .output(stack("hbm_m:pipe_copper", 2))
                .output(stack("hbm_m:analog_circuit", 1), 0.5F)
                .save(writer, "anvil/construction/recycle_fusion_heater");

        // Stufe 1, RECYCLING
        AnvilRecipeBuilder.construction(1, 262, OverlayType.RECYCLING)
                .input(Ingredient.of(item("hbm_m:fusion_hatch")), 1, true)
                .output(stack("hbm_m:pipe_steel", 4))
                .output(stack("hbm_m:pipe_copper", 4))
                .output(stack("hbm_m:analog_circuit", 1), 0.75F)
                .save(writer, "anvil/construction/recycle_fusion_hatch");
    }
}
//?}
