package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1-Port des Abschnitts "fusion reactor" aus {@code AssemblyMachineRecipes.registerDefaults()}
 * (1.7.10).
 *
 * <p>Die {@code ANY_*}-Gruppen des Original-OreDicts werden hier als {@link Ingredient} mit beiden
 * Mitgliedern abgebildet - genau wie bei den bereits portierten Rezepten (siehe
 * {@code ass.arcfurnace} mit Polymer/Bakelit):</p>
 * <ul>
 *   <li>{@code ANY_RESISTANTALLOY} = TCAlloy + CDAlloy</li>
 *   <li>{@code ANY_HARDPLASTIC} = PC + PVC</li>
 *   <li>{@code ANY_PLASTIC} = Polymer + Bakelit</li>
 *   <li>{@code ANY_BISMOIDBRONZE} = Bismoidbronze + Arsenbronze</li>
 * </ul>
 *
 * <p>Die {@code inputItemsEx}-Varianten des Originals (Alternativrezepte mit
 * {@code item_expensive}-Baugruppen) sind nicht enthalten - dieses "teure Bauteil"-System gibt es
 * im Port noch nicht.</p>
 */
public final class FusionAssemblerRecipeGenerator {

    private FusionAssemblerRecipeGenerator() {}

    private static Ingredient resistantAlloyWelded() {
        return Ingredient.of(ModItems.PLATE_WELDED_TCALLOY.get(), ModItems.PLATE_WELDED_CDALLOY.get());
    }

    private static Ingredient resistantAlloyCast() {
        return Ingredient.of(ModItems.PLATE_CAST_TCALLOY.get(), ModItems.PLATE_CAST_CDALLOY.get());
    }

    private static Ingredient resistantAlloyIngot() {
        return Ingredient.of(ModItems.getIngot(ModIngots.TCALLOY).get(), ModItems.getIngot(ModIngots.CDALLOY).get());
    }

    private static Ingredient hardPlastic() {
        return Ingredient.of(ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get(), ModItems.getIngot(ModIngots.PVC).get());
    }

    private static Ingredient plastic() {
        return Ingredient.of(ModItems.getIngot(ModIngots.POLYMER).get(), ModItems.getIngot(ModIngots.BAKELITE).get());
    }

    private static Ingredient bismoidBronzeCast() {
        return Ingredient.of(ModItems.PLATE_CAST_BBRONZE.get(), ModItems.PLATE_CAST_ABRONZE.get());
    }

    public static void generate(Consumer<FinishedRecipe> writer) {

        // ass.fusioncore - der Kern, aus dem die Torusform zusammenwaechst.
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.STRUCT_TORUS_CORE.get(), 1), 600, 100)
                .addIngredient(resistantAlloyWelded(), 8)
                .addIngredient(hardPlastic(), 32)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 8)
                .withBlueprintPool("tcalloy")
                .save(writer, "fusioncore");

        // ass.fusionbscco - das rohe Spulenbauteil (wird spaeter mit dem Brenner verschweisst).
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.FUSION_COMPONENT.get(), 2), 100, 100)
                .addIngredient(ModItems.WIRE_DENSE_BSCCO.get(), 1)
                .addIngredient(ModItems.PIPE_COPPER.get(), 1)
                .addIngredient(resistantAlloyIngot(), 1)
                .addIngredient(plastic(), 4)
                .withBlueprintPool("tcalloy")
                .save(writer, "fusionbscco");

        // ass.fusionblanket
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.FUSION_COMPONENT_BLANKET.get(), 4), 100, 100)
                .addIngredient(ModItems.PLATE_WELDED_TUNGSTEN.get(), 1)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 2)
                .addIngredient(ModItems.getIngot(ModIngots.BERYLLIUM).get(), 4)
                .withBlueprintPool("tcalloy")
                .save(writer, "fusionblanket");

        // ass.fusionpipes
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.FUSION_COMPONENT_MOTOR.get(), 4), 100, 100)
                .addIngredient(hardPlastic(), 4)
                .addIngredient(ModItems.PIPE_COPPER.get(), 2)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 1)
                .withBlueprintPool("tcalloy")
                .save(writer, "fusionpipes");

        // ass.fusionklystron
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.KLYSTRON.get(), 1), 300, 100)
                .addIngredient(ModItems.PLATE_WELDED_TUNGSTEN.get(), 4)
                .addIngredient(resistantAlloyCast(), 16)
                .addIngredient(ModItems.PLATE_COPPER.get(), 32)
                .addIngredient(hardPlastic(), 16)
                .addIngredient(ModItems.WIRE_DENSE_BSCCO.get(), 8)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 2)
                .save(writer, "fusionklystron");

        // ass.fusioncollector
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.COLLECTOR.get(), 1), 300, 100)
                .addIngredient(resistantAlloyCast(), 4)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.GRAPHITE).get(), 16)
                .addIngredient(hardPlastic(), 4)
                .save(writer, "fusioncollector");

        // ass.fusionbreeder
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.BREEDER_FUSION.get(), 1), 300, 100)
                .addIngredient(resistantAlloyCast(), 4)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.BORON).get(), 16)
                .addIngredient(hardPlastic(), 16)
                .save(writer, "fusionbreeder");

        // ass.fusionboiler
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.BOILER_FUSION.get(), 1), 300, 100)
                .addIngredient(resistantAlloyCast(), 16)
                .addIngredient(ModItems.SHELL_COPPER.get(), 16)
                .addIngredient(ModItems.PIPE_STEEL.get(), 8)
                .addIngredient(hardPlastic(), 16)
                .save(writer, "fusionboiler");

        // ass.fusionmhdt
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.MHDT.get(), 1), 1200, 100)
                .addIngredient(resistantAlloyWelded(), 16)
                .addIngredient(ModItems.PLATE_WELDED_COPPER.get(), 64)
                .addIngredient(bismoidBronzeCast(), 16)
                .addIngredient(ModItems.WIRE_DENSE_SCHRABIDATE.get(), 64)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 4)
                .withBlueprintPool("chlorophyte")
                .save(writer, "fusionmhdt");

        // ass.fusioncoupler
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.COUPLER.get(), 1), 300, 100)
                .addIngredient(resistantAlloyWelded(), 4)
                .addIngredient(ModItems.PLATE_COPPER.get(), 32)
                .addIngredient(ModItems.WIRE_DENSE_BSCCO.get(), 16)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 4)
                .save(writer, "fusioncoupler");

        // ass.fusionplasmaforge
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.PLASMA_FORGE.get(), 1), 1200, 100)
                .addIngredient(resistantAlloyWelded(), 8)
                .addIngredient(ModItems.WIRE_DENSE_BSCCO.get(), 32)
                .addIngredient(bismoidBronzeCast(), 16)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 4)
                .withBlueprintPool("chlorophyte")
                .save(writer, "fusionplasmaforge");
    }
}
//?}
