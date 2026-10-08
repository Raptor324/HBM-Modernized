package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * 1:1-Port von {@code BlastFurnaceRecipes} (Legierungsofen) und {@code BlastFurnaceRecipesNT} (Hochofen, mit Schlacke).
 *
 * <p>AUTOMATISCH ERZEUGT aus dem Original ({@code com.hbm.inventory.recipes.BlastFurnaceRecipes / BlastFurnaceRecipesNT}) durch {@code rc/transpile.py} - nicht von Hand
 * aendern, sondern Zuordnungen im Skript pflegen. OreDict-Schluessel des Originals sind Item-Tags
 * {@code hbm_m:oredict/...} ({@link OreDictTagProvider}). Mit OFFEN markierte Rezepte haben im Port
 * (noch) keine Entsprechung fuer Zutat oder Ergebnis.</p>
 */
public final class BlastFurnaceRecipeGenerator {

    private BlastFurnaceRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        part0(writer);
        checkMissing("BlastFurnaceRecipeGenerator");
    }

    private static void part0(Consumer<FinishedRecipe> writer) {
        // BlastFurnaceRecipes: ['ingotIron', 'plateIron', 'gemIron', 'dustIron'] + ['ingotCoal', 'plateCoal', 'gemCoal', 'dustCoal']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 1), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/iron"), ore("oredict/plate/iron"), ore("oredict/dust/iron")), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/gem/coal"), ore("oredict/dust/coal")))
                .save(writer, "blast_furnace/legacy_steel_ingot_0");

        // BlastFurnaceRecipes: ['ingotIron', 'plateIron', 'gemIron', 'dustIron'] + ['ingotAnyCoke', 'plateAnyCoke', 'gemAnyCoke', 'dustAnyCoke']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 1), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/iron"), ore("oredict/plate/iron"), ore("oredict/dust/iron")), ore("oredict/gem/any_coke"))
                .save(writer, "blast_furnace/legacy_steel_ingot_1");

        // BlastFurnaceRecipes: ['oreIron'] + ['ingotCoal', 'plateCoal', 'gemCoal', 'dustCoal']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 2), ore("oredict/ore/iron"), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/gem/coal"), ore("oredict/dust/coal")))
                .save(writer, "blast_furnace/legacy_steel_ingot_2");

        // BlastFurnaceRecipes: ['oreIron'] + ['ingotAnyCoke', 'plateAnyCoke', 'gemAnyCoke', 'dustAnyCoke']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 3), ore("oredict/ore/iron"), ore("oredict/gem/any_coke"))
                .save(writer, "blast_furnace/legacy_steel_ingot_3");

        // BlastFurnaceRecipes: ['oreIron'] + ModItems.powder_flux x1
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 3), ore("oredict/ore/iron"), Ingredient.of(item("hbm_m:flux_powder")))
                .save(writer, "blast_furnace/legacy_steel_ingot_4");

        // BlastFurnaceRecipes: ['ingotCopper', 'plateCopper', 'gemCopper', 'dustCopper'] + ['ingotRedstone', 'plateRedstone', 'gemRedstone', 'dustRedstone']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:red_copper_ingot", 2), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/copper"), ore("oredict/plate/copper"), ore("oredict/dust/copper")), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/redstone"), ore("oredict/dust/redstone")))
                .save(writer, "blast_furnace/legacy_red_copper_ingot_5");

        // BlastFurnaceRecipes: ModItems.canister_full@EXPR:Fluids.GASOLINE.getID() x1 + ['slimeball']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:canister_napalm", 1), filled("hbm_m:canister_full", ModFluids.GASOLINE.getSource()), ore("oredict/slimeball"))
                .save(writer, "blast_furnace/legacy_canister_napalm_6");

        // BlastFurnaceRecipes: ['ingotTungsten', 'plateTungsten', 'gemTungsten', 'dustTungsten'] + ['nuggetSchrabidium']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:magnetized_tungsten_ingot", 1), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/tungsten"), ore("oredict/dust/tungsten")), ore("oredict/nugget/schrabidium"))
                .save(writer, "blast_furnace/legacy_magnetized_tungsten_ingot_7");

        // BlastFurnaceRecipes: ['ingotSteel', 'plateSteel', 'gemSteel', 'dustSteel'] + ['nuggetTechnetium99']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:tcalloy_ingot", 1), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/steel"), ore("oredict/plate/steel"), ore("oredict/dust/steel")), ore("oredict/nugget/technetium99"))
                .save(writer, "blast_furnace/legacy_tcalloy_ingot_8");

        // BlastFurnaceRecipes: ['plateGold'] + ModItems.plate_mixed x1
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:plate_paa", 2), ore("oredict/plate/gold"), Ingredient.of(item("hbm_m:plate_mixed")))
                .save(writer, "blast_furnace/legacy_plate_paa_9");

        // BlastFurnaceRecipes: ['ingotSaturnite', 'plateSaturnite', 'gemSaturnite', 'dustSaturnite'] + ModItems.ingot_meteorite x1
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:starmetal_ingot", 2), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/saturnite"), ore("oredict/plate/saturnite")), Ingredient.of(item("hbm_m:meteorite_ingot")))
                .save(writer, "blast_furnace/legacy_starmetal_ingot_10");

        // BlastFurnaceRecipes: ['ingotCobalt', 'plateCobalt', 'gemCobalt', 'dustCobalt'] + ModItems.powder_meteorite x1
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:meteorite_ingot", 1), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/cobalt"), ore("oredict/dust/cobalt")), Ingredient.of(item("hbm_m:meteorite_powder")))
                .save(writer, "blast_furnace/legacy_meteorite_ingot_11");

        // BlastFurnaceRecipes: ModItems.meteorite_sword_hardened x1 + ['ingotCobalt', 'plateCobalt', 'gemCobalt', 'dustCobalt']
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:meteorite_sword_alloyed", 1), Ingredient.of(item("hbm_m:meteorite_sword_hardened")), net.minecraftforge.common.crafting.CompoundIngredient.of(ore("oredict/ingot/cobalt"), ore("oredict/dust/cobalt")))
                .save(writer, "blast_furnace/legacy_meteorite_sword_alloyed_12");

        // blast.steelFromIngot
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 2), ore("oredict/ingot/iron"), ore("oredict/sand")).nt()
                .counts(2, 1).duration(800)
                .secondaryOutput(stack("hbm_m:ingot_slag", 1))
                .save(writer, "blast_furnace/steelfromingot");

        // blast.steelFromDust
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 2), ore("oredict/dust/iron"), ore("oredict/sand")).nt()
                .counts(2, 1).duration(800)
                .secondaryOutput(stack("hbm_m:ingot_slag", 1))
                .save(writer, "blast_furnace/steelfromdust");

        // blast.steelFromOre
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 2), ore("oredict/ore/iron"), ore("oredict/sand")).nt()
                .counts(1, 1).duration(800)
                .secondaryOutput(stack("hbm_m:ingot_slag", 2))
                .save(writer, "blast_furnace/steelfromore");

        // blast.steelWithFlux
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:steel_ingot", 3), ore("oredict/ore/iron"), Ingredient.of(item("hbm_m:flux_powder"))).nt()
                .counts(1, 1).duration(1200)
                .secondaryOutput(stack("hbm_m:ingot_slag", 2))
                .save(writer, "blast_furnace/steelwithflux");

        // blast.mingrade
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:red_copper_ingot", 2), ore("oredict/ingot/copper"), ore("oredict/dust/redstone")).nt()
                .counts(1, 1).duration(400)
                .save(writer, "blast_furnace/mingrade");

        // blast.mingradeDust
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:red_copper_ingot", 2), ore("oredict/dust/copper"), ore("oredict/dust/redstone")).nt()
                .counts(1, 1).duration(400)
                .save(writer, "blast_furnace/mingradedust");

        // blast.mingradeIngot
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:red_copper_ingot", 2), ore("oredict/ingot/copper"), ore("oredict/ingot/redstone")).nt()
                .counts(1, 1).duration(400)
                .save(writer, "blast_furnace/mingradeingot");

        // blast.mingradeCursed
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:red_copper_ingot", 2), ore("oredict/dust/copper"), ore("oredict/ingot/redstone")).nt()
                .counts(1, 1).duration(400)
                .save(writer, "blast_furnace/mingradecursed");

        // blast.mingradeOre
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:red_copper_ingot", 6), ore("oredict/ore/copper"), ore("oredict/dust/redstone")).nt()
                .counts(1, 6).duration(1200)
                .secondaryOutput(stack("hbm_m:ingot_slag", 1))
                .save(writer, "blast_furnace/mingradeore");

        // blast.meteorSword
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:meteorite_sword_alloyed", 1), ore("oredict/ingot/cobalt"), Ingredient.of(item("hbm_m:meteorite_sword_hardened"))).nt()
                .counts(1, 1).duration(1200)
                .save(writer, "blast_furnace/meteorsword");

        // blast.meteor
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:meteorite_ingot", 1), ore("oredict/ingot/cobalt"), Ingredient.of(item("hbm_m:meteorite_powder"))).nt()
                .counts(1, 1).duration(600)
                .save(writer, "blast_furnace/meteor");

        // blast.starmetal
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:starmetal_ingot", 1), ore("oredict/ingot/saturnite"), Ingredient.of(item("hbm_m:meteorite_ingot"))).nt()
                .counts(1, 1).duration(600)
                .save(writer, "blast_furnace/starmetal");

        // blast.paa
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:plate_paa", 1), ore("oredict/ingot/gold"), Ingredient.of(item("hbm_m:plate_mixed"))).nt()
                .counts(1, 1).duration(600)
                .save(writer, "blast_furnace/paa");

        // blast.firebrick
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:firebrick", 8), ore("oredict/dust/aluminum"), Ingredient.of(item("minecraft:clay_ball"))).nt()
                .counts(1, 7).duration(800)
                .save(writer, "blast_furnace/firebrick");

        // blast.firebrickLimestone
        BlastFurnaceRecipeBuilder.blastFurnaceRecipe(stack("hbm_m:firebrick", 8), ore("oredict/ore/limestone"), Ingredient.of(item("minecraft:clay_ball"))).nt()
                .counts(1, 6).duration(800)
                .save(writer, "blast_furnace/firebricklimestone");
    }
}
//?}
