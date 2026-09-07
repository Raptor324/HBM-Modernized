package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Port der Plasmaschmiede-Rezepte aus {@code PlasmaForgeRecipes.registerDefaults()} (1.7.10).
 *
 * <p>Von den 36 Originalrezepten laesst sich derzeit nur das Fusionsgefaess bauen - alle uebrigen
 * brauchen Werkstoffe, die es in diesem Port noch nicht gibt (Euphemium, Dineutronium, CFT-Barren,
 * Osmiridium-Gussplatten, Sternenmetall, ICF-Bauteile ...). Sobald die Materialien da sind,
 * gehoeren sie hier hinein.</p>
 */
public final class PlasmaForgeRecipeGenerator {

    private PlasmaForgeRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {

        // plsm.fusionvessel: 6x64 Rohbauteile, 3x64 Rohre, 2x64 Blankets und 4 Quantenschaltkreise.
        // Das Original nutzt 12 Slots a 64 Stueck; hier steht je Slot ein Eintrag mit count 64.
        PlasmaForgeRecipeBuilder builder = PlasmaForgeRecipeBuilder
                .plasmaForgeRecipe(new ItemStack(ModBlocks.TORUS.get()), 1_200, 2_000_000L)
                .inputEnergy(3_000_000L);

        for (int i = 0; i < 6; i++) {
            builder.addIngredient(Ingredient.of(ModBlocks.FUSION_COMPONENT.get()), 64);
        }
        for (int i = 0; i < 3; i++) {
            builder.addIngredient(Ingredient.of(ModBlocks.FUSION_COMPONENT_MOTOR.get()), 64);
        }
        for (int i = 0; i < 2; i++) {
            builder.addIngredient(Ingredient.of(ModBlocks.FUSION_COMPONENT_BLANKET.get()), 64);
        }
        builder.addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 4);

        builder.save(writer, "plasma_forge/fusion_vessel");
    }
}
//?}
