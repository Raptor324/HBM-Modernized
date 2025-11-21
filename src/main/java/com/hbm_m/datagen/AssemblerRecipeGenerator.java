package com.hbm_m.datagen;

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
        registerVanillaExamples(writer);
        registerPlateRecipes(writer);
        registerArmorPlates(writer);
        registerElectronics(writer);
        registerArmorSets(writer);
        registerCastPlates(writer);
        registerSpecialPlates(writer);
        registerFuelPlates(writer);
    }

    private static void registerVanillaExamples(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(Items.DIAMOND_SWORD), 100, 500)
                .addIngredient(Items.DIAMOND, 2)
                .addIngredient(Items.STICK, 1)
                .save(writer, "diamond_sword_from_assembler");
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
    }

    private static void registerArmorPlates(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ARMOR_AJR.get(), 1), 100, 200)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 4)
                .addIngredient(ModItems.INSULATOR.get(), 2)
                .save(writer, "plate_armor_ajr");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ARMOR_DNT_RUSTED.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_ARMOR_DNT.get(), 1)
                .addIngredient(Items.WATER_BUCKET, 1)
                .save(writer, "plate_armor_dnt_rusted");
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
                        new ItemStack(ModItems.ADVANCED_CIRCUIT.get(), 1), 120, 250)
                .addIngredient(ModItems.WIRE_CARBON.get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 2)
                .addIngredient(ModItems.CAPACITOR.get(), 4)
                .addIngredient(ModItems.MICROCHIP.get(), 16)
                .addIngredient(ModItems.PCB.get(), 8)
                .save(writer, "advanced_circuit");
    }

    private static void registerArmorSets(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ARMOR_FAU.get(), 1), 120, 250)
                .addIngredient(ModItems.getIngot(ModIngots.TITANIUM).get(), 4)
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 2)
                .save(writer, "plate_armor_fau");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ARMOR_HEV.get(), 1), 120, 250)
                .addIngredient(ModItems.getIngot(ModIngots.TITANIUM).get(), 4)
                .addIngredient(ModItems.PLATE_KEVLAR.get(), 2)
                .save(writer, "plate_armor_hev");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ARMOR_LUNAR.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 4)
                .addIngredient(ModItems.PLATE_ARMOR_TITANIUM.get(), 2)
                .save(writer, "plate_armor_lunar");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_ARMOR_TITANIUM.get(), 1), 100, 200)
                .addIngredient(ModItems.getIngot(ModIngots.TITANIUM).get(), 4)
                .save(writer, "plate_armor_titanium");
    }

    private static void registerCastPlates(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_CAST.get(), 1), 40, 50)
                .addIngredient(Items.IRON_INGOT, 2)
                .save(writer, "plate_cast");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_CAST_ALT.get(), 1), 40, 50)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 2)
                .save(writer, "plate_cast_alt");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_CAST_BISMUTH.get(), 1), 40, 50)
                .addIngredient(ModItems.getIngot(ModIngots.BISMUTH).get(), 2)
                .save(writer, "plate_cast_bismuth");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_CAST_DARK.get(), 1), 40, 50)
                .addIngredient(ModItems.getIngot(ModIngots.TUNGSTEN).get(), 2)
                .save(writer, "plate_cast_dark");
    }

    private static void registerSpecialPlates(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_COMBINE_STEEL.get(), 2), 80, 150)
                .addIngredient(ModItems.getIngot(ModIngots.COMBINE_STEEL).get(), 3)
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

    private static void registerFuelPlates(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_MOX.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.PLUTONIUM).get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.URANIUM).get(), 1)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_mox");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_PU238BE.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.PLUTONIUM238).get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.BERYLLIUM).get(), 1)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_pu238be");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_PU239.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.PLUTONIUM239).get(), 2)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_pu239");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_RA226BE.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.RA226).get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.BERYLLIUM).get(), 1)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_ra226be");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_SA326.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.SCHRABIDIUM).get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.ALUMINUM).get(), 1)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_sa326");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_U233.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.URANIUM233).get(), 2)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_u233");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_FUEL_U235.get(), 1), 150, 300)
                .addIngredient(ModItems.getIngot(ModIngots.URANIUM235).get(), 2)
                .addIngredient(ModItems.PLATE_CAST.get(), 1)
                .save(writer, "plate_fuel_u235");
    }
}

