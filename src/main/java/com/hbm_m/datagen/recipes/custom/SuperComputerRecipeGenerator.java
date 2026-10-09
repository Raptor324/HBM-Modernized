package com.hbm_m.datagen.recipes.custom;
//? if forge {

import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code SuperComputerRecipes}: Simulationen (Flug-/Teilchendaten auf Flash-Laufwerken), Aufbereitung der
 * Satellitendaten, Kopieren, Blaupausen und Klaus. Jede Rechenaufgabe gibt es mit drei Kuehlungen (Wasser,
 * Perfluormethyl, Helium-4): bessere Kuehlung ist schneller, aber fehleranfaelliger (defektes Laufwerk).
 */
public final class SuperComputerRecipeGenerator {

    private static final int MIN = 60 * 20;

    private SuperComputerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Simulationen
        simulation(writer, "drive_flash_flightsim", "flightcalc");
        simulation(writer, "drive_flash_particlesim", "particlecalc");

        // Aufbereitung der Satellitendaten
        triplet(writer, "processflight", 30 * MIN, 15 * MIN, 2 * MIN, "drive_disk_flightdata", "drive_disk_flightdata_processed", "drive_disk_broken", 99, 95, 90);
        triplet(writer, "processorbit", 60 * MIN, 30 * MIN, 5 * MIN, "drive_disk_orbitdata", "drive_disk_orbitdata_processed", "drive_disk_broken", 75, 65, 50);

        // Kopieren
        copy(writer, "copyflightcalc", 15 * MIN, "drive_flash_flightsim", "drive_flash_empty", "drive_flash_broken", 95);
        copy(writer, "copyparticlecalc", 15 * MIN, "drive_flash_particlesim", "drive_flash_empty", "drive_flash_broken", 95);
        copy(writer, "copyfligthdata", 15 * MIN, "drive_disk_flightdata_processed", "drive_disk_empty", "drive_disk_broken", 75);

        // Blaupausen-Ordner ohne Kugelfisch: laenger, teurer, aber doppelte Chance
        SuperComputerRecipeBuilder.superComputerRecipe(15 * MIN, 50_000)
                .addItemInput(Items.PAPER, 16)
                .addItemInput(Ingredient.of(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "dyes/blue"))), 16)
                .addChanceOutput(stack("blueprint_folder", 1), 20, new ItemStack(Items.PAPER, 16), 80)
                .save(writer, "supercomputer/blueprints");
        SuperComputerRecipeBuilder.superComputerRecipe(15 * MIN, 50_000)
                .addItemInput(Items.PAPER, 24)
                .addItemInput(item("cinnebar"), 24)
                .addChanceOutput(stack("blueprint_folder_discover", 1), 10, new ItemStack(Items.PAPER, 24), 90)
                .save(writer, "supercomputer/beigeprints");

        // Klaus
        SuperComputerRecipeBuilder.superComputerRecipe(60 * MIN, 5_000_000)
                .addItemInput(item("drive_disk_empty"), 64)
                .addItemInput(item("drive_disk_empty"), 64)
                .addItemInput(item("drive_disk_empty"), 64)
                .addFluidInput(ModFluids.WATER.getSource(), 1_000_000)
                .addItemOutput(stack("drive_klaus", 1))
                .addFluidOutput(ModFluids.SLOP.getSource(), 1_000)
                .save(writer, "supercomputer/klaus");
    }

    /** Original {@code registerSimulation}: leeres Flash-Laufwerk rechnet die Simulation. */
    private static void simulation(Consumer<FinishedRecipe> writer, String output, String name) {
        triplet(writer, name, 15 * MIN, 5 * MIN / 2, MIN, "drive_flash_empty", output, "drive_flash_broken", 95, 50, 25);
    }

    /** Original {@code registerTriplet}: Wasser, Perfluormethyl und Helium-4 als Kuehlung. */
    private static void triplet(Consumer<FinishedRecipe> writer, String name, int time0, int time1, int time2,
                                String input, String output, String broken, int chance0, int chance1, int chance2) {
        SuperComputerRecipeBuilder.superComputerRecipe(time0, 10_000)
                .addItemInput(item(input), 1)
                .addChanceOutput(stack(output, 1), chance0, stack(broken, 1), 100 - chance0)
                .addFluidInput(ModFluids.WATER.getSource(), 16_000)
                .addFluidOutput(ModFluids.SPENTSTEAM.getSource(), 16_000)
                .save(writer, "supercomputer/" + name + "_water");
        SuperComputerRecipeBuilder.superComputerRecipe(time1, 10_000)
                .addItemInput(item(input), 1)
                .addChanceOutput(stack(output, 1), chance1, stack(broken, 1), 100 - chance1)
                .addFluidInput(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 16_000)
                .addFluidOutput(ModFluids.PERFLUOROMETHYL.getSource(), 16_000)
                .save(writer, "supercomputer/" + name + "_pfm");
        SuperComputerRecipeBuilder.superComputerRecipe(time2, 10_000)
                .addItemInput(item(input), 1)
                .addChanceOutput(stack(output, 1), chance2, stack(broken, 1), 100 - chance2)
                .addFluidInput(ModFluids.HELIUM4.getSource(), 16_000)
                .save(writer, "supercomputer/" + name + "_helium");
    }

    /** Original {@code registerCopy}: volles + leeres Laufwerk -> zwei volle (oder zwei defekte). */
    private static void copy(Consumer<FinishedRecipe> writer, String name, int time, String full, String empty, String broken, int chance) {
        SuperComputerRecipeBuilder.superComputerRecipe(time, 10_000)
                .addItemInput(item(full), 1)
                .addItemInput(item(empty), 1)
                .addChanceOutput(stack(full, 2), chance, stack(broken, 2), 100 - chance)
                .save(writer, "supercomputer/" + name);
    }

    private static Item item(String id) {
        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        if (it == Items.AIR) throw new IllegalStateException("Supercomputer: unbekanntes Item hbm_m:" + id);
        return it;
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(item(id), count);
    }
}
//?}
