package com.hbm_m.datagen.recipes.custom;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.datagen.recipes.ModRecipeProvider;
import com.hbm_m.item.tags_and_tiers.ModIngots;
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
        registerPlateRecipes(writer);
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

    private static void registerPlateRecipes(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_IRON.get(), 2), 60, 100)
                .addIngredient(Items.IRON_INGOT, 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_iron_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_GOLD.get(), 2), 60, 100)
                .addIngredient(Items.GOLD_INGOT, 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_gold_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_STEEL.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_steel_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_SATURNITE.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.SATURNITE).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_saturnite_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_SCHRABIDIUM.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.SCHRABIDIUM).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_schrabidium_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_TITANIUM.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.TITANIUM).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_titanium_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_GUNMETAL.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.GUNMETAL).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_gunmetal_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_GUNSTEEL.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.GUNSTEEL).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_gunsteel_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_LEAD.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.LEAD).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_lead_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_COPPER.get(), 2), 60, 100)
                .addIngredient(Items.COPPER_INGOT, 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_copper_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ADVANCED_ALLOY.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_advanced_alloy_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ALUMINUM.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.ALUMINUM).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_aluminum_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_BISMUTH.get(), 2), 60, 100)
                .addIngredient(ModItems.getIngot(ModIngots.BISMUTH).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_bismuth_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                    new ItemStack(ModItems.PLATE_COMBINE_STEEL.get(), 2), 80, 150)
                .addIngredient(ModItems.getIngot(ModIngots.COMBINE_STEEL).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_combine_steel_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_DURA_STEEL.get(), 2), 80, 150)
                .addIngredient(ModItems.getIngot(ModIngots.DURA_STEEL).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_dura_steel_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_DALEKANIUM.get(), 2), 100, 200)
                .addIngredient(ModItems.getIngot(ModIngots.DIGAMMA).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_dalekanium_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_DESH.get(), 2), 100, 200)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_desh_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_DINEUTRONIUM.get(), 1), 200, 500)
                .addIngredient(ModItems.getIngot(ModIngots.DINEUTRONIUM).get(), 2)
                .withBlueprintPool("plates")
                .save(writer, "plate_dineutronium_from_ingots");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_EUPHEMIUM.get(), 2), 120, 250)
                .addIngredient(ModItems.getIngot(ModIngots.EUPHEMIUM).get(), 3)
                .withBlueprintPool("plates")
                .save(writer, "plate_euphemium_from_ingots");
    }
}