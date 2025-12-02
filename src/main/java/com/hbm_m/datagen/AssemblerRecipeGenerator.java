package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Groups all assembler recipes so they can be maintained separately from {@link ModRecipeProvider}.
 */
public final class AssemblerRecipeGenerator {

    private AssemblerRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerMainRecipes(writer);
        registerElectronics(writer);

    }


    private static void registerMainRecipes(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SHREDDER.get(), 1), 80, 150)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .save(writer, "shredder");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MAN_CORE.get(), 1), 160, 250)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.BERYLLIUM).get(), 4)
                .save(writer, "man_core");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.GRENADE_NUC.get(), 3), 160, 250)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 1)
                .addIngredient(ModItems.WIRE_RED_COPPER.get(), 6)
                .addIngredient(ModItems.PLATE_STEEL.get(), 3)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 3)
                .save(writer, "grenade_nuc");
    }


    private static void registerElectronics(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(

                        new ItemStack(ModItems.ANALOG_CIRCUIT.get(), 1), 80, 150)
                .addIngredient(ModItems.CAPACITOR.get(), 2)
                .addIngredient(ModItems.VACUUM_TUBE.get(), 3)
                .addIngredient(ModItems.WIRE_CARBON.get(), 4)
                .addIngredient(ModItems.PCB.get(), 4)
                .save(writer, "analog_circuit");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.INTEGRATED_CIRCUIT.get(), 1), 80, 150)
                .addIngredient(ModItems.WIRE_CARBON.get(), 4)
                .addIngredient(ModItems.CAPACITOR.get(), 2)
                .addIngredient(ModItems.MICROCHIP.get(), 4)
                .addIngredient(ModItems.PCB.get(), 4)
                .save(writer, "integrated_circuit");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CAPACITOR_BOARD.get(), 1), 80, 150)
                .addIngredient(ModItems.CAPACITOR.get(), 3)
                .addIngredient(ModItems.WIRE_CARBON.get(), 3)
                .addIngredient(ModItems.PCB.get(), 1)
                .save(writer, "capacitor_board");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.ADVANCED_CIRCUIT.get(), 1), 120, 250)
                .addIngredient(ModItems.WIRE_CARBON.get(), 8)
                .addIngredient(ModItems.CAPACITOR.get(), 4)
                .addIngredient(ModItems.MICROCHIP.get(), 32)
                .addIngredient(ModItems.PCB.get(), 8)
                .save(writer, "advanced_circuit");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CONTROLLER.get(), 1), 120, 250)
                .addIngredient(ModItems.WIRE_CARBON.get(), 16)
                .addIngredient(ModItems.CAPACITOR.get(), 64)
                .addIngredient(ModItems.MICROCHIP.get(), 32)
                .addIngredient(ModItems.CONTROLLER_CHASSIS.get(), 1)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 1)
                .addIngredient(ModItems.PCB.get(), 16)
                .save(writer, "controller");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.MACHINE_BATTERY.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_STEEL.get(), 12)
                .addIngredient(ModItems.SULFUR.get(), 12)
                .addIngredient(ModItems.getPowder(ModIngots.LEAD).get(), 12)
                .save(writer, "battery");

    }

}