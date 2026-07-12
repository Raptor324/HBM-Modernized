package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.datagen.recipes.ModRecipeProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

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
        registerCastPlateRecipes(writer);
        registerDoorRecipes(writer);
        registerMissileRecipes(writer);
        registerMachineParts(writer);
        registerMachines(writer);
        registerGenerators(writer);
        registerAccelerators(writer);
        registerReactors(writer);
        registerFusionReactor(writer);
        registerUpgrades(writer);
        registerBombParts(writer);
        registerMissileParts(writer);
        registerAmmo(writer);
        registerSpace(writer);
        registerTurrets(writer);

        // Genuinely blocked — original recipe needs an item/block/enum system that doesn't exist in this
        // port yet (ItemExpensive, colored keys, BLOCK_CAP, modular missile parts, nuke output
        // blocks, ore-dict-style tag aggregates like ANY_RESISTANTALLOY/ANY_HIGHEXPLOSIVE, PA_COIL, etc.).
        // See AssemblerRecipeGenerator gap-analysis notes for the full skipped list.
    }

    /**
     * MVP-Turret-Varianten (Original: Assembly-Machine-Rezepte in AssemblyMachineRecipes.java,
     * ass.turretX). Exakte Original-Zutaten nicht 1:1 uebernommen (das Original nutzt materialspezifische
     * Formen wie GUNMETAL.mechanism(), die es in diesem Port nicht gibt) - stattdessen plausible,
     * nach Tier skalierte Annaeherung mit vorhandenen Items.
     */
    private static void registerTurrets(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_CHEKHOV.get()), 200, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.PART_MECHANISM.get(), 1)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 1)
                .addIngredient(ModItems.SILICON_CIRCUIT.get(), 1)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_chekhov");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_FRIENDLY.get()), 200, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.PART_MECHANISM.get(), 1)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 1)
                .addIngredient(ModItems.SILICON_CIRCUIT.get(), 1)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_friendly");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_JEREMY.get()), 200, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.PART_MECHANISM.get(), 1)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 1)
                .addIngredient(ModItems.SILICON_CIRCUIT.get(), 1)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_jeremy");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_TAUON.get()), 240, 150)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.PART_MECHANISM.get(), 2)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 1)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 1)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_tauon");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_RICHARD.get()), 240, 150)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.PART_MECHANISM.get(), 2)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 1)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 1)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_richard");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_HOWARD.get()), 280, 200)
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.PART_MECHANISM.get(), 2)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 2)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 2)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_howard");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_MAXWELL.get()), 280, 200)
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.PART_MECHANISM.get(), 2)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 2)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 2)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "turret_maxwell");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_FRITZ.get()), 320, 250)
                .addIngredient(ModItems.PLATE_DESH.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.PART_MECHANISM.get(), 2)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 2)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 2)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 2)
                .save(writer, "turret_fritz");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_ARTY.get()), 400, 300)
                .addIngredient(ModItems.PLATE_DESH.get(), 6)
                .addIngredient(ModItems.MOTOR.get(), 3)
                .addIngredient(ModItems.PART_MECHANISM.get(), 3)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 3)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 3)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 2)
                .save(writer, "turret_arty");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.TURRET_HIMARS.get()), 480, 350)
                .addIngredient(ModItems.PLATE_DESH.get(), 8)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .addIngredient(ModItems.PART_MECHANISM.get(), 4)
                .addIngredient(Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get().asItem()), 4)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 3)
                .save(writer, "turret_himars");
    }

    /** Cloth / small parts — port of 1.7.10 ass.platemixed, ass.hazcloth, ass.firecloth, ass.filtercoal. */
    private static void registerMachineParts(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PLATE_MIXED.get(), 4), 50, 100)
                .addIngredient(ModItems.PLATE_COPPER.get(), 2)
                .addIngredient(ModItems.NEUTRON_REFLECTOR.get(), 1)
                .addIngredient(ModItems.PLATE_SATURNITE.get(), 1)
                .save(writer, "platemixed");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.HAZMAT_CLOTH.get(), 4), 50, 100)
                .addIngredient(ModItems.getPowder(ModIngots.LEAD).get(), 4)
                .addIngredient(Items.STRING, 8)
                .save(writer, "hazcloth");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.ASBESTOS_CLOTH.get(), 4), 50, 100)
                .addIngredient(ModItems.getIngot(ModIngots.ASBESTOS).get(), 1)
                .addIngredient(Items.STRING, 8)
                .save(writer, "firecloth");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.FILTER_COAL.get(), 1), 50, 100)
                .addIngredient(ModItems.getPowders(com.hbm_m.item.tags_and_tiers.ModPowders.COAL).get(), 4)
                .addIngredient(Items.STRING, 2)
                .addIngredient(Items.PAPER, 1)
                .save(writer, "filtercoal");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.DRILL_TITANIUM.get(), 1), 100, 100)
                .addIngredient(ModItems.PLATE_CAST_DURA_STEEL.get(), 1)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 8)
                .save(writer, "titaniumdrill");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.ENTANGLEMENT_KIT.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_DURA_STEEL.get(), 4)
                .addIngredient(ModItems.PLATE_COPPER.get(), 24)
                .addIngredient(ModItems.WIRE_DENSE_GOLD.get(), 16)
                .save(writer, "entanglementkit");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PART_LITHIUM.get(), 8), 40, 100)
                .addIngredient(ModItems.getPowder(ModIngots.LITHIUM_INGOT).get(), 1)
                .save(writer, "partlith");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PART_BERYLLIUM.get(), 8), 40, 100)
                .addIngredient(ModItems.getPowder(ModIngots.BERYLLIUM).get(), 1)
                .save(writer, "partberyl");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PART_CARBON.get(), 8), 40, 100)
                .addIngredient(ModItems.getPowders(com.hbm_m.item.tags_and_tiers.ModPowders.COAL).get(), 1)
                .save(writer, "partcoal");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PART_PLUTONIUM.get(), 8), 40, 100)
                .addIngredient(ModItems.getPowder(ModIngots.PLUTONIUM).get(), 1)
                .save(writer, "partplut");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.CMB_BRICK.get(), 8), 100, 100)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get().asItem()), 4)
                .addIngredient(ModItems.PLATE_COMBINE_STEEL.get(), 4)
                .save(writer, "cmbtile");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.CMB_BRICK_REINFORCED.get(), 8), 100, 100)
                .addIngredient(ModItems.getIngot(ModIngots.MAGNETIZED_TUNGSTEN).get(), 8)
                .addIngredient(ModBlocks.DUCRETE.get().asItem(), 4)
                .addIngredient(ModBlocks.CMB_BRICK.get().asItem(), 8)
                .save(writer, "cmbbrick");
    }

    /** Standalone machines — port of 1.7.10 pumpjack/flarestack/crackingtower/coker/compressor/silex/drillbits/slopper/mininglaser/strandcaster. */
    private static void registerMachines(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PUMPJACK.get(), 1), 400, 100)
                .addIngredient(ModItems.PLATE_DURA_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 8)
                .addIngredient(ModItems.PIPE_STEEL.get(), 12)
                .addIngredient(ModItems.MOTOR_DESH.get(), 1)
                .addIngredient(ModItems.DRILL_TITANIUM.get(), 1)
                .save(writer, "pumpjack");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.FLARE_STACK.get(), 1), 100, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 12)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(ModItems.SHELL_STEEL.get(), 4)
                .addIngredient(ModItems.THERMO_ELEMENT.get(), 3)
                .save(writer, "flarestack");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.CRACKING_TOWER.get(), 1), 200, 100)
                .addIngredient(ModBlocks.STEEL_SCAFFOLD.get().asItem(), 16)
                .addIngredient(ModItems.SHELL_STEEL.get(), 6)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.NIOBIUM).get(), 4)
                .save(writer, "crackingtower");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.COKER.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 8)
                .addIngredient(ModItems.SHELL_STEEL.get(), 4)
                .addIngredient(ModItems.PLATE_COPPER.get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.NIOBIUM).get(), 4)
                .save(writer, "coker");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.COMPRESSOR.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(ModItems.SHELL_STEEL.get(), 2)
                .addIngredient(ModItems.MOTOR.get(), 3)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 1)
                .save(writer, "compressor");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SILEX.get(), 1), 400, 100)
                .addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 16)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 8)
                .addIngredient(ModItems.PIPE_STEEL.get(), 8)
                .save(writer, "silex");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.DRILLBIT_STEEL.get(), 1), 100, 100)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.TUNGSTEN).get(), 4)
                .save(writer, "drillsteel");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.DRILLBIT_HSS.get(), 1), 100, 100)
                .addIngredient(ModItems.getIngot(ModIngots.DURA_STEEL).get(), 12)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()), 12)
                .addIngredient(ModItems.getIngot(ModIngots.TITANIUM).get(), 8)
                .save(writer, "drilldura");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.DRILLBIT_DESH.get(), 1), 100, 100)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.NIOBIUM).get(), 4)
                .save(writer, "drilldesh");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.ORE_SLOPPER.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 6)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 8)
                .addIngredient(ModItems.PIPE_COPPER.get(), 3)
                .addIngredient(ModItems.MOTOR.get(), 3)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 1)
                .save(writer, "slopper");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.MINING_LASER.get(), 1), 400, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.SHELL_TITANIUM.get(), 4)
                .addIngredient(ModItems.PLATE_DURA_STEEL.get(), 4)
                .addIngredient(ModItems.CRYSTAL_REDSTONE.get(), 3)
                .addIngredient(Items.DIAMOND, 3)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()), 8)
                .addIngredient(ModItems.MOTOR.get(), 3)
                .save(writer, "mininglaser");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.STRAND_CASTER.get(), 1), 200, 100)
                .addIngredient(ModItems.FIREBRICK.get(), 16)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 6)
                .addIngredient(ModItems.PLATE_WELDED_COPPER.get(), 2)
                .addIngredient(ModItems.SHELL_STEEL.get(), 2)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get().asItem()), 8)
                .save(writer, "strandcaster");
    }

    /** Generators / pistons — port of 1.7.10 dieselgen, pistonset(steel/desh/starmetal), hephaestus. */
    private static void registerGenerators(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.DIESELGEN.get(), 1), 200, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 1)
                .addIngredient(ModItems.PLATE_CAST_COPPER.get(), 2)
                .addIngredient(ModItems.COIL_COPPER.get(), 4)
                .save(writer, "dieselgen");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PISTON_SET_STEEL.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.TUNGSTEN).get(), 8)
                .addIngredient(ModItems.BOLT_TUNGSTEN.get(), 16)
                .save(writer, "pistonsetsteel");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PISTON_SET_DESH.get(), 1), 200, 100)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 24)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()), 12)
                .addIngredient(ModItems.PLATE_COPPER.get(), 24)
                .addIngredient(ModItems.getIngot(ModIngots.TUNGSTEN).get(), 16)
                .addIngredient(ModItems.PIPE_DURA_STEEL.get(), 4)
                .save(writer, "pistonsetdesh");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.PISTON_SET_STARMETAL.get(), 1), 200, 100)
                .addIngredient(ModItems.getIngot(ModIngots.STARMETAL).get(), 24)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 16)
                .addIngredient(ModItems.PLATE_SATURNITE.get(), 24)
                .addIngredient(ModItems.getIngot(ModIngots.NIOBIUM).get(), 16)
                .addIngredient(ModItems.PIPE_DURA_STEEL.get(), 4)
                .save(writer, "pistonsetstar");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.HEPHAESTUS.get(), 1), 200, 100)
                .addIngredient(ModItems.PIPE_STEEL.get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 24)
                .addIngredient(ModItems.PLATE_COPPER.get(), 24)
                .addIngredient(ModItems.getIngot(ModIngots.NIOBIUM).get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 12)
                .addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 16)
                .save(writer, "hephaestus");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CAPACITOR_TANTALUM.get(), 1), 100, 10_000)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .addIngredient(ModItems.getIngot(ModIngots.TANTALIUM).get(), 24)
                .save(writer, "capacitortantalum");
    }

    /** Particle accelerator components — port of 1.7.10 beamline/rfc/quadrupole/dipole/source/detector/exposurechamber. */
    private static void registerAccelerators(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.BEAMLINE.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_COPPER.get(), 16)
                .addIngredient(ModItems.WIRE_DENSE_GOLD.get(), 4)
                .save(writer, "beamline");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.RFC.get(), 1), 400, 100)
                .addIngredient(ModBlocks.BEAMLINE.get().asItem(), 3)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 16)
                .addIngredient(ModItems.PLATE_COPPER.get(), 64)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .addIngredient(ModItems.MAGNETRON.get(), 16)
                .save(writer, "rfc");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.QUADRUPOLE.get(), 1), 400, 100)
                .addIngredient(ModBlocks.BEAMLINE.get().asItem(), 1)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 1)
                .save(writer, "quadrupole");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.DIPOLE.get(), 1), 400, 100)
                .addIngredient(ModBlocks.BEAMLINE.get().asItem(), 2)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 32)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 4)
                .save(writer, "dipole");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SOURCE.get(), 1), 400, 100)
                .addIngredient(ModBlocks.BEAMLINE.get().asItem(), 3)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .addIngredient(ModItems.MAGNETRON.get(), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 1)
                .save(writer, "source");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.DETECTOR.get(), 1), 400, 100)
                .addIngredient(ModBlocks.BEAMLINE.get().asItem(), 3)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 24)
                .addIngredient(ModItems.WIRE_DENSE_GOLD.get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 4)
                .save(writer, "detector");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get(), 1), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_ALUMINIUM.get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get(), 4)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 12)
                .addIngredient(ModItems.WIRE_DENSE_GOLD.get(), 32)
                .addIngredient(ModItems.MOTOR_DESH.get(), 2)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 4)
                .addIngredient(ModItems.CAPACITOR_TANTALUM.get(), 1)
                .addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 16)
                .save(writer, "exposurechamber");
    }

    /** RBMK / PWR reactor components — port of 1.7.10 ass.rbmk and the pwr* family. */
    private static void registerReactors(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.RBMK_BLANK.get(), 1), 100, 100)
                .addIngredient(ModBlocks.CONCRETE_ASBESTOS.get().asItem(), 4)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 2)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 2)
                .save(writer, "rbmk");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_CONTROL.get(), 4), 200, 500)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 2)
                .addIngredient(ModItems.getIngot(ModIngots.BORON).get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .save(writer, "pwrcontrol");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_CHANNEL.get(), 4), 200, 500)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .save(writer, "pwrchannel");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_HEATEX.get(), 4), 200, 500)
                .addIngredient(ModItems.PLATE_CAST_COPPER.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .save(writer, "pwrheatex");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_HEATSINK.get(), 4), 200, 500)
                .addIngredient(ModItems.PLATE_CAST_SATURNITE.get(), 4)
                .addIngredient(ModItems.PLATE_CAST_COPPER.get(), 4)
                .save(writer, "pwrheatsink");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_REFLECTOR.get(), 4), 200, 500)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 2)
                .addIngredient(ModItems.NEUTRON_REFLECTOR.get(), 4)
                .save(writer, "pwrreflector");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_CASING.get(), 4), 200, 500)
                .addIngredient(ModItems.PLATE_LEAD.get(), 4)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get().asItem()), 4)
                .save(writer, "pwrcasing");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_PORT.get(), 4), 200, 500)
                .addIngredient(ModItems.PLATE_LEAD.get(), 4)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get().asItem()), 4)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .save(writer, "pwrport");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.PWR_NEUTRON_SOURCE.get(), 1), 200, 500)
                .addIngredient(ModItems.PLATE_WELDED_ZIRCONIUM.get(), 1)
                .addIngredient(ModItems.BILLET_RA226BE.get(), 3)
                .save(writer, "pwrneutronsource");
    }

    /** Fusion reactor components — port of 1.7.10 fusionblanket/fusionpipes/fusioncollector/fusionbreeder/fusionboiler. */
    private static void registerFusionReactor(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.FUSION_COMPONENT_BLANKET.get(), 4), 100, 100)
                .addIngredient(ModItems.PLATE_WELDED_TUNGSTEN.get(), 1)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 2)
                .addIngredient(ModItems.getIngot(ModIngots.BERYLLIUM).get(), 4)
                .save(writer, "fusionblanket");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.FUSION_COMPONENT_MOTOR.get(), 4), 100, 100)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 4)
                .addIngredient(ModItems.PIPE_COPPER.get(), 2)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 1)
                .save(writer, "fusionpipes");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.COLLECTOR.get(), 1), 300, 100)
                .addIngredient(ModItems.PLATE_CAST_ALLOY.get(), 4)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.GRAPHITE).get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 4)
                .save(writer, "fusioncollector");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.BREEDER_FUSION.get(), 1), 300, 100)
                .addIngredient(ModItems.PLATE_CAST_ALLOY.get(), 4)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.BORON).get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .save(writer, "fusionbreeder");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.BOILER_FUSION.get(), 1), 300, 100)
                .addIngredient(ModItems.PLATE_CAST_ALLOY.get(), 16)
                .addIngredient(ModItems.SHELL_COPPER.get(), 16)
                .addIngredient(ModItems.PIPE_STEEL.get(), 8)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .save(writer, "fusionboiler");
    }

    /** WATZ reactor rods — port of 1.7.10 ass.watzrod / ass.watzcooler. */
    private static void registerUpgrades(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.WATZ_ELEMENT.get(), 3), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 2)
                .addIngredient(ModItems.getIngot(ModIngots.ZIRCONIUM).get(), 2)
                .addIngredient(ModItems.getIngot(ModIngots.SATURNITE).get(), 2)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 4)
                .save(writer, "watzrod");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.WATZ_COOLER.get(), 3), 200, 100)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 2)
                .addIngredient(ModItems.PLATE_CAST_COPPER.get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 2)
                .save(writer, "watzcooler");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.UPGRADE_OVERDRIVE_1.get(), 1), 200, 100)
                .addIngredient(ModItems.UPGRADE_SPEED_3.get(), 1)
                .addIngredient(ModItems.UPGRADE_EFFECT_3.get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.SATURNITE).get(), 16)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.PVC).get(),
                        ModItems.getIngot(ModIngots.POLYMER_COMPOSITE).get()), 16)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 16)
                .save(writer, "overdrive1");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.UPGRADE_OVERDRIVE_2.get(), 1), 600, 100)
                .addIngredient(ModItems.UPGRADE_OVERDRIVE_1.get(), 1)
                .addIngredient(ModItems.UPGRADE_SPEED_3.get(), 1)
                .addIngredient(ModItems.UPGRADE_EFFECT_3.get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.SATURNITE).get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.CFT).get(), 8)
                .addIngredient(ModItems.CAPACITOR_BOARD.get(), 16)
                .save(writer, "overdrive2");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.UPGRADE_OVERDRIVE_3.get(), 1), 1200, 100)
                .addIngredient(ModItems.UPGRADE_OVERDRIVE_2.get(), 1)
                .addIngredient(ModItems.UPGRADE_SPEED_3.get(), 1)
                .addIngredient(ModItems.UPGRADE_EFFECT_3.get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.BISMUTH_BRONZE).get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.CFT).get(), 16)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 16)
                .save(writer, "overdrive3");
    }

    /** Nuclear bomb components — port of 1.7.10 wiring/core1/boyshield/boytarget/boybullet/manigniter/mancore/mikecore/mikedeut/mikecooler/fleijacore/soliniumcore. */
    private static void registerBombParts(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.GADGET_WIREING.get(), 1), 200, 100)
                .addIngredient(ModItems.WIRE_GOLD.get(), 24)
                .save(writer, "wiring");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.GADGET_CORE.get(), 1), 1200, 100)
                .addIngredient(ModItems.NUGGET_PU239.get(), 7)
                .addIngredient(ModItems.NUGGET_U238.get(), 3)
                .save(writer, "core1");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.BOY_SHIELDING.get(), 1), 200, 100)
                .addIngredient(ModItems.NEUTRON_REFLECTOR.get(), 12)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .save(writer, "boyshield");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.BOY_TARGET.get(), 1), 200, 100)
                .addIngredient(ModItems.NUGGET_U235.get(), 18)
                .save(writer, "boytarget");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.BOY_BULLET.get(), 1), 200, 100)
                .addIngredient(ModItems.NUGGET_U235.get(), 9)
                .save(writer, "boybullet");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MAN_IGNITER.get(), 1), 200, 100)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 3)
                .addIngredient(ModItems.WIRE_GOLD.get(), 24)
                .save(writer, "manigniter");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MAN_CORE.get(), 1), 1200, 100)
                .addIngredient(ModItems.NUGGET_PU239.get(), 8)
                .addIngredient(ModItems.NUGGET_BERYLLIUM.get(), 2)
                .save(writer, "mancore");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MIKE_CORE.get(), 1), 1200, 100)
                .addIngredient(ModItems.NUGGET_U238.get(), 24)
                .addIngredient(ModItems.PLATE_LEAD.get(), 6)
                .save(writer, "mikecore");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MIKE_DEUT.get(), 1), 600, 100)
                .addIngredient(ModItems.PLATE_GUNSTEEL.get(), 16)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 16)
                .save(writer, "mikedeut");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MIKE_COOLING_UNIT.get(), 1), 300, 100)
                .addIngredient(ModItems.PLATE_DURA_STEEL.get(), 8)
                .addIngredient(ModItems.COIL_COPPER.get(), 5)
                .addIngredient(ModItems.COIL_TUNGSTEN.get(), 5)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .save(writer, "mikecooler");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.FLEIJA_CORE.get(), 1), 600, 100)
                .addIngredient(ModItems.NUGGET_U235.get(), 8)
                .addIngredient(ModItems.NUGGET_NEPTUNIUM.get(), 2)
                .addIngredient(ModItems.NUGGET_BERYLLIUM.get(), 4)
                .addIngredient(ModItems.COIL_COPPER.get(), 2)
                .save(writer, "fleijacore");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SOLINIUM_CORE.get(), 1), 600, 100)
                .addIngredient(ModItems.NUGGET_SOLINIUM.get(), 9)
                .addIngredient(ModItems.NUGGET_EUPHEMIUM.get(), 1)
                .save(writer, "soliniumcore");
    }

    /** Missile warheads/thrusters — port of 1.7.10 warheadhe1/warheadcl1-3/thrusternerva. */
    private static void registerMissileParts(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.WARHEAD_GENERIC_SMALL.get(), 1), 100, 100)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(ModItems.BALL_DYNAMITE.get(), 2)
                .addIngredient(ModItems.MICROCHIP.get(), 1)
                .save(writer, "warheadhe1");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL.get(), 1), 100, 100)
                .addIngredient(ModItems.WARHEAD_GENERIC_SMALL.get(), 1)
                .addIngredient(ModItems.PELLET_CLUSTER.get(), 2)
                .save(writer, "warheadcl1");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get(), 1), 200, 100)
                .addIngredient(ModItems.WARHEAD_GENERIC_MEDIUM.get(), 1)
                .addIngredient(ModItems.PELLET_CLUSTER.get(), 4)
                .save(writer, "warheadcl2");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE.get(), 1), 400, 100)
                .addIngredient(ModItems.WARHEAD_GENERIC_LARGE.get(), 1)
                .addIngredient(ModItems.PELLET_CLUSTER.get(), 8)
                .save(writer, "warheadcl3");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.THRUSTER_NUCLEAR.get(), 1), 600, 100)
                .addIngredient(ModItems.getIngot(ModIngots.DURA_STEEL).get(), 32)
                .addIngredient(ModItems.getIngot(ModIngots.BORON).get(), 8)
                .addIngredient(ModItems.PLATE_LEAD.get(), 16)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .save(writer, "thrusternerva");
    }

    /** Chemical artillery shells — port of 1.7.10 shellchlorine/shellphosgene/shellmustard.
     * NOTE: original recipes also consumed 4000mB of the respective gas fluid; the assembler recipe
     * system in this port has no fluid-input support, so the fluid cost is omitted here. */
    private static void registerAmmo(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.AMMO_ARTY_CHLORINE.get(), 1), 100, 1_000)
                .addIngredient(ModItems.AMMO_ARTY.get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.POLYMER).get(), 1)
                .save(writer, "shellchlorine");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.AMMO_ARTY_PHOSGENE.get(), 1), 100, 1_000)
                .addIngredient(ModItems.AMMO_ARTY.get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.POLYMER).get(), 1)
                .save(writer, "shellphosgene");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.AMMO_ARTY_MUSTARD_GAS.get(), 1), 100, 1_000)
                .addIngredient(ModItems.AMMO_ARTY.get(), 1)
                .addIngredient(ModItems.getIngot(ModIngots.POLYMER).get(), 1)
                .save(writer, "shellmustard");
    }

    /** Space program — port of 1.7.10 soyuzcore/satellitemapper/satellitescanner/satelliteradar/satelliteresonator. */
    private static void registerSpace(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get(), 1), 1200, 100)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 16)
                .addIngredient(ModItems.UPGRADE_SPEED_3.get(), 1)
                .addIngredient(ModItems.UPGRADE_POWER_3.get(), 1)
                .addIngredient(ModItems.CONTROLLER.get(), 4)
                .addIngredient(ModItems.BATTERY_LITHIUM.get(), 1)
                .save(writer, "soyuzcore");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_MAPPER.get(), 1), 600, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 3)
                .addIngredient(ModItems.PLATE_DESH.get(), 4)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 8)
                .save(writer, "satellitemapper");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_SCANNER.get(), 1), 600, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 3)
                .addIngredient(ModItems.PLATE_CAST_TITANIUM.get(), 8)
                .addIngredient(ModItems.PLATE_DESH.get(), 4)
                .addIngredient(ModItems.MAGNETRON.get(), 8)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 8)
                .save(writer, "satellitescanner");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_RADAR.get(), 1), 600, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 3)
                .addIngredient(ModItems.PLATE_CAST_TITANIUM.get(), 12)
                .addIngredient(ModItems.MAGNETRON.get(), 12)
                .addIngredient(ModItems.COIL_GOLD.get(), 16)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .save(writer, "satelliteradar");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_RESONATOR.get(), 1), 600, 100)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 6)
                .addIngredient(ModItems.getIngot(ModIngots.STARMETAL).get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.POLYMER).get(), 48)
                .addIngredient(ModItems.CRYSTAL_XEN.get(), 1)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 16)
                .save(writer, "satelliteresonator");
    }


    private static void registerMainRecipes(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SHREDDER.get(), 1), 80, 150)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .save(writer, "shredder");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.FAT_MAN_CORE.get(), 1), 160, 250)
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

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CHEMICAL_PLANT.get(), 1), 200, 300)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 8)
                .addIngredient(ModItems.PIPE_COPPER.get(), 2)
                .addIngredient(ModItems.INSULATOR.get(), 16)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.COIL_TUNGSTEN.get(), 2)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 1)
                .save(writer, "chemical_plant");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CENTRIFUGE.get(), 1), 40, 125)
                .addIngredient(ModItems.CENTRIFUGE_ELEMENT.get(), 1)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()), 4)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 1)
                .save(writer, "centrifuge");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.GAS_CENTRIFUGE.get(), 1), 80, 150)
                .addIngredient(ModItems.CENTRIFUGE_ELEMENT.get(), 4)
                .addIngredient(ModItems.getIngot(ModIngots.POLYMER).get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 2)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 1)
                .save(writer, "gas_centrifuge");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.FLUID_TANK.get(), 1), 40, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.SHELL_STEEL.get(), 4)
                .save(writer, "fluid_tank");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CRYSTALLIZER.get(), 1), 40, 100)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 2)
                .addIngredient(ModItems.SHELL_TITANIUM.get(), 3)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 2)
                .save(writer, "ore_acidizer");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.REFINERY.get(), 1), 160, 250)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 3)
                .addIngredient(ModItems.PLATE_COPPER.get(), 8)
                .addIngredient(ModItems.SHELL_STEEL.get(), 4)
                .addIngredient(ModItems.PIPE_STEEL.get(), 12)
                .addIngredient(ModItems.INSULATOR.get(), 8)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 3)
                .save(writer, "refinery");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.ZIRNOX.get(), 1), 240, 400)
                .addIngredient(ModItems.SHELL_STEEL.get(), 4)
                .addIngredient(ModItems.PIPE_STEEL.get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.BORON).get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.GRAPHITE).get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 16)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get()), 16)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 4)
                .save(writer, "zirnox");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.TURBINE.get(), 1), 160, 250)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.getIngot(ModIngots.BIORUBBER).get(), 4)
                .addIngredient(ModItems.TURBINE_TITANIUM.get(), 2)
                .addIngredient(ModItems.WIRE_DENSE_GOLD.get(), 4)
                .addIngredient(ModItems.PIPE_COPPER.get(), 4)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 1)
                .save(writer, "turbine");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.INDUSTRIAL_TURBINE.get(), 1), 240, 400)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 4)
                .addIngredient(ModItems.TURBINE_TITANIUM.get(), 2)
                .addIngredient(ModItems.WIRE_DENSE_GOLD.get(), 4)
                .addIngredient(ModItems.PIPE_DURA_STEEL.get(), 4)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 2)
                .save(writer, "industrial_turbine");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.RADAR.get(), 1), 160, 250)
                .addIngredient(ModItems.PLATE_STEEL.get(), 12)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 12)
                .addIngredient(ModItems.MAGNETRON.get(), 5)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 8)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 4)
                .save(writer, "radar");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.LARGE_RADAR.get(), 1), 240, 400)
                .addIngredient(ModItems.PLATE_WELDED_STEEL.get(), 6)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.CADMIUM).get(),
                        ModItems.getIngot(ModIngots.TECHNETIUM).get()), 4)
                .addIngredient(Ingredient.of(
                        ModItems.getIngot(ModIngots.RUBBER).get(),
                        ModItems.getIngot(ModIngots.BIORUBBER).get()), 24)
                .addIngredient(ModItems.MAGNETRON.get(), 16)
                .addIngredient(ModItems.getIngot(ModIngots.DESH).get(), 1)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 4)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .save(writer, "large_radar");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.DERRICK.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_CAST_COPPER.get(), 2)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(ModItems.DRILL_TITANIUM.get(), 1)
                .save(writer, "derrick");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SOLDERING_STATION.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_CAST_STEEL.get(), 2)
                .addIngredient(ModItems.COIL_COPPER.get(), 4)
                .addIngredient(ModItems.BOLT_TUNGSTEN.get(), 4)
                .addIngredient(ModItems.VACUUM_TUBE.get(), 2)
                .save(writer, "soldering_station");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.TOWER_SMALL.get(), 1), 240, 400)
                .addIngredient(Ingredient.of(ModBlocks.BRICK_CONCRETE.get()), 64)
                .addIngredient(Items.IRON_BARS, 128)
                .addIngredient(Ingredient.of(ModBlocks.STEAM_CONDENSER.get()), 4)
                .save(writer, "tower_small");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.COOLING_TOWER.get(), 1), 240, 400)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get()), 128)
                .addIngredient(Ingredient.of(ModBlocks.DECO_STEEL_SCAFFOLD.get()), 32)
                .addIngredient(Ingredient.of(ModBlocks.STEAM_CONDENSER.get()), 16)
                .addIngredient(ModItems.PIPE_STEEL.get(), 8)
                .save(writer, "cooling_tower");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.HYDRAULIC_FRACKINING_TOWER.get(), 1), 240, 400)
                .addIngredient(ModItems.SHELL_STEEL.get(), 24)
                .addIngredient(ModItems.PIPE_STEEL.get(), 12)
                .addIngredient(Ingredient.of(ModBlocks.CONCRETE.get()), 64)
                .addIngredient(ModItems.DRILL_TITANIUM.get(), 1)
                .addIngredient(ModItems.MOTOR_DESH.get(), 2)
                .addIngredient(ModItems.PLATE_DESH.get(), 24)
                .addIngredient(ModItems.CAPACITOR.get(), 16)
                .save(writer, "hydraulic_frackining_tower");
    }

    private static void registerDoorRecipes(Consumer<FinishedRecipe> writer) {
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.LARGE_VEHICLE_DOOR.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_CAST_DARK.get(), 16)
                .addIngredient(ModItems.INSULATOR.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 16)
                .addIngredient(Ingredient.of(Tags.Items.DYES_GREEN), 4)
                .save(writer, "large_vehicle_door");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SILO_HATCH.get(), 1), 40, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4) //Welded Plate needs added.
                .addIngredient(ModItems.INSULATOR.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.BOLT_STEEL.get(), 16)
                .addIngredient(Ingredient.of(Tags.Items.DYES_GREEN), 4)
                .save(writer, "silo_hatch");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SILO_HATCH_LARGE.get(), 1), 60, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 6) //Welded Plate needs added.
                .addIngredient(ModItems.INSULATOR.get(), 8)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.BOLT_STEEL.get(), 16)
                .addIngredient(Ingredient.of(Tags.Items.DYES_GREEN), 8)
                .save(writer, "silo_hatch_large");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.WATER_DOOR.get(), 1), 40, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.BOLT_STEEL.get(), 4) //NEEDS DURA-STEEL BOLT
                .addIngredient(Ingredient.of(Tags.Items.DYES_RED), 1)
                .save(writer, "water_door");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.QE_CONTAINMENT.get(), 1), 80, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4) //CAST STEEL PLATE NEEDED
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 4)
                .addIngredient(ModItems.INSULATOR.get(), 8)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 32)
                .addIngredient(Ingredient.of(Tags.Items.DYES_BLACK), 4)
                .save(writer, "door_qe_containment");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.QE_SLIDING.get(), 1), 40, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.INSULATOR.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 4)
                .addIngredient(ModItems.FLUORITE.get(), 4)
                .addIngredient(Items.GLASS, 4)
                .save(writer, "door_qe_sliding");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.ROUND_AIRLOCK_DOOR.get(), 1), 80, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 12) //CAST STEEL PLATE NEEDED
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 8)
                .addIngredient(ModItems.INSULATOR.get(), 16)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 16)
                .addIngredient(Ingredient.of(Tags.Items.DYES_GREEN), 4)
                .save(writer, "round_airlock_door");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SECURE_ACCESS_DOOR.get(), 1), 80, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 12) //CAST STEEL PLATE NEEDED
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 16)
                .addIngredient(ModItems.INSULATOR.get(), 8)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 32)
                .addIngredient(Ingredient.of(Tags.Items.DYES_RED), 4)
                .save(writer, "secure_access_door");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.FIRE_DOOR.get(), 1), 60, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 16)
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 8)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 8)
                .save(writer, "fire_door");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.VAULT_DOOR.get(), 1), 120, 150)
                .addIngredient(ModItems.getIngot(ModIngots.STEEL).get(), 32)
                .addIngredient(ModItems.getIngot(ModIngots.DURA_STEEL).get(), 32)
                .addIngredient(ModItems.PLATE_LEAD.get(), 8) //PLATE_CAST_LEAD NEEDED
                .addIngredient(ModItems.MOTOR.get(), 3)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 32)
                .addIngredient(ModItems.getIngot(ModIngots.RUBBER).get(), 12)
                .save(writer, "door_vault_tech");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SLIDING_SEAL_DOOR.get(), 1), 40, 125)
                .addIngredient(ModItems.PLATE_STEEL.get(), 12)
                .addIngredient(ModItems.INSULATOR.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.BOLT_HIGHSPEED_STEEL.get(), 4)
                .addIngredient(Ingredient.of(Tags.Items.DYES_WHITE), 2)
                .save(writer, "door_sliding_seal");
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

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CENTRIFUGE_ELEMENT.get(), 1), 20, 100)
                .addIngredient(ModItems.PLATE_DURA_STEEL.get(), 4)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .save(writer, "centrifuge_element");
    }

    private static void registerCastPlateRecipes(Consumer<FinishedRecipe> writer) {
        // 2 regular plates → 1 cast plate (pressing / pouring into mold)
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_IRON.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_IRON.get(), 2)
                .save(writer, "plate_cast_iron_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_STEEL.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .save(writer, "plate_cast_steel_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_COPPER.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_COPPER.get(), 2)
                .save(writer, "plate_cast_copper_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_GOLD.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_GOLD.get(), 2)
                .save(writer, "plate_cast_gold_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_TITANIUM.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 2)
                .save(writer, "plate_cast_titanium_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_ALUMINIUM.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_ALUMINUM.get(), 2)
                .save(writer, "plate_cast_aluminium_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_DURA_STEEL.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_DURA_STEEL.get(), 2)
                .save(writer, "plate_cast_dura_steel_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_DESH.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_DESH.get(), 2)
                .save(writer, "plate_cast_desh_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_SCHRABIDIUM.get(), 1), 120, 300)
                .addIngredient(ModItems.PLATE_SCHRABIDIUM.get(), 2)
                .save(writer, "plate_cast_schrabidium_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModItems.PLATE_CAST_SATURNITE.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_SATURNITE.get(), 2)
                .save(writer, "plate_cast_saturnite_from_plates");
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

    private static void registerMissileRecipes(Consumer<FinishedRecipe> writer) { // TODO: WIP RECIPES, NEEDS REWORK WHEN FULL PROCESSING IS PORTED
        // Tier 0 — micro / ABM (cheap test crafts)
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_TEST.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .addIngredient(Items.IRON_INGOT, 4)
                .addIngredient(Items.REDSTONE, 2)
                .addIngredient(Items.GUNPOWDER, 4)
                .save(writer, "missile_test");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_ABM.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 3)
                .addIngredient(Items.IRON_INGOT, 4)
                .addIngredient(Items.REDSTONE, 4)
                .addIngredient(Items.GUNPOWDER, 2)
                .save(writer, "missile_abm");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_MICRO.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 1)
                .addIngredient(Items.IRON_INGOT, 2)
                .addIngredient(Items.GUNPOWDER, 4)
                .save(writer, "missile_micro");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_SCHRABIDIUM.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_SCHRABIDIUM.get(), 2)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .addIngredient(Items.REDSTONE, 4)
                .addIngredient(Items.GUNPOWDER, 4)
                .save(writer, "missile_schrabidium");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_BHOLE.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(Items.OBSIDIAN, 4)
                .addIngredient(Items.IRON_INGOT, 4)
                .addIngredient(Items.ENDER_PEARL, 2)
                .save(writer, "missile_bhole");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_TAINT.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .addIngredient(Items.ROTTEN_FLESH, 8)
                .addIngredient(Items.GUNPOWDER, 4)
                .addIngredient(Items.REDSTONE, 2)
                .save(writer, "missile_taint");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_EMP.get(), 1), 60, 100)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .addIngredient(Items.REDSTONE, 8)
                .addIngredient(ModItems.CAPACITOR.get(), 2)
                .save(writer, "missile_emp");

        // Tier 1 — V2 / stealth
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_GENERIC.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(Items.GUNPOWDER, 8)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .addIngredient(Items.IRON_INGOT, 4)
                .save(writer, "missile_generic");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_INCENDIARY.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(Items.BLAZE_POWDER, 4)
                .addIngredient(Items.GUNPOWDER, 6)
                .addIngredient(Items.FIRE_CHARGE, 4)
                .save(writer, "missile_incendiary");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_CLUSTER.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.GRENADE.get(), 2)
                .addIngredient(Items.GUNPOWDER, 4)
                .addIngredient(Items.IRON_INGOT, 2)
                .save(writer, "missile_cluster");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_BUSTER.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_STEEL.get(), 6)
                .addIngredient(Items.IRON_BLOCK, 2)
                .addIngredient(Items.GUNPOWDER, 8)
                .addIngredient(ModItems.MOTOR.get(), 1)
                .save(writer, "missile_buster");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_DECOY.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_ALUMINUM.get(), 4)
                .addIngredient(ModItems.PLATE_STEEL.get(), 2)
                .addIngredient(Items.REDSTONE, 4)
                .addIngredient(Items.PAPER, 4)
                .save(writer, "missile_decoy");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_STEALTH.get(), 1), 80, 150)
                .addIngredient(ModItems.PLATE_ADVANCED_ALLOY.get(), 2)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(Items.COAL, 8)
                .addIngredient(Items.GUNPOWDER, 4)
                .save(writer, "missile_stealth");

        // Tier 2 — strong
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_STRONG.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_STEEL.get(), 6)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(Items.GUNPOWDER, 12)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .save(writer, "missile_strong");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 2)
                .addIngredient(Items.BLAZE_POWDER, 8)
                .addIngredient(Items.GUNPOWDER, 8)
                .save(writer, "missile_incendiary_strong");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_CLUSTER_STRONG.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 2)
                .addIngredient(ModItems.GRENADEHE.get(), 2)
                .addIngredient(Items.GUNPOWDER, 6)
                .save(writer, "missile_cluster_strong");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_BUSTER_STRONG.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 6)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(Items.IRON_BLOCK, 4)
                .addIngredient(Items.GUNPOWDER, 12)
                .save(writer, "missile_buster_strong");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_EMP_STRONG.get(), 1), 100, 200)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(ModItems.PLATE_COPPER.get(), 4)
                .addIngredient(Items.REDSTONE, 16)
                .addIngredient(ModItems.CAPACITOR.get(), 4)
                .save(writer, "missile_emp_strong");

        // Tier 3 — huge / shuttle
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_BURST.get(), 1), 120, 250)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_DESH.get(), 2)
                .addIngredient(ModItems.BALL_TNT.get(), 2)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .save(writer, "missile_burst");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_INFERNO.get(), 1), 120, 250)
                .addIngredient(ModItems.PLATE_STEEL.get(), 6)
                .addIngredient(ModItems.PLATE_DESH.get(), 2)
                .addIngredient(Items.BLAZE_POWDER, 16)
                .addIngredient(Items.GUNPOWDER, 8)
                .save(writer, "missile_inferno");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_RAIN.get(), 1), 120, 250)
                .addIngredient(ModItems.PLATE_STEEL.get(), 6)
                .addIngredient(ModItems.PLATE_DESH.get(), 2)
                .addIngredient(ModItems.GRENADE.get(), 4)
                .addIngredient(Items.GUNPOWDER, 8)
                .save(writer, "missile_rain");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_DRILL.get(), 1), 120, 250)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 8)
                .addIngredient(ModItems.PLATE_STEEL.get(), 4)
                .addIngredient(ModItems.DRILL_TITANIUM.get(), 1)
                .addIngredient(Items.GUNPOWDER, 8)
                .save(writer, "missile_drill");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_SHUTTLE.get(), 1), 120, 250)
                .addIngredient(ModItems.PLATE_ALUMINUM.get(), 8)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 2)
                .save(writer, "missile_shuttle");

        // Tier 4 — atlas / doomsday
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_NUCLEAR.get(), 1), 160, 300)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 2)
                .addIngredient(ModItems.BALL_TNT.get(), 4)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 1)
                .save(writer, "missile_nuclear");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER.get(), 1), 160, 300)
                .addIngredient(ModItems.PLATE_STEEL.get(), 6)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 2)
                .addIngredient(ModItems.GRENADE_NUC.get(), 1)
                .addIngredient(Items.GUNPOWDER, 8)
                .save(writer, "missile_nuclear_cluster");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_VOLCANO.get(), 1), 160, 300)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.PLATE_TITANIUM.get(), 4)
                .addIngredient(Items.MAGMA_BLOCK, 4)
                .addIngredient(ModItems.BALL_TNT.get(), 4)
                .save(writer, "missile_volcano");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_DOOMSDAY.get(), 1), 160, 300)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 4)
                .addIngredient(ModItems.PLATE_SCHRABIDIUM.get(), 4)
                .addIngredient(ModItems.BALL_TNT.get(), 8)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 2)
                .save(writer, "missile_doomsday");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get(), 1), 160, 300)
                .addIngredient(ModItems.PLATE_STEEL.get(), 8)
                .addIngredient(ModItems.BILLET_PLUTONIUM.get(), 2)
                .addIngredient(Items.IRON_NUGGET, 16)
                .addIngredient(ModItems.BALL_TNT.get(), 2)
                .save(writer, "missile_doomsday_rusted");
    }
}
//?}