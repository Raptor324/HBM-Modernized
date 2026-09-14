package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * Port der Plasmaschmiede-Rezepte aus {@code PlasmaForgeRecipes.registerDefaults()} (1.7.10).
 *
 * <p>Enthalten sind das Fusionsgefaess, die beiden Exotenplatten und die komplette
 * Schweissplatten-Reihe. Letztere bildet im Original ueber {@code setGroup("autoswitch.weldPlates")}
 * eine Autoswitch-Gruppe: die Maschine springt selbsttaetig auf das Rezept, dessen Gussblech gerade
 * im ersten Eingabeslot liegt.</p>
 *
 * <p>Dazu kommen die ICF-Rezepte, siehe {@link #icf(Consumer)}.</p>
 *
 * <p><b>Noch nicht portierbar:</b> {@code plsm.hde} (kein HDE-Bauteil) sowie Schrabidiumhammer und
 * Fensu-San (Yharonit, UFO-Muenze, Elektronium, Batteriepakete fehlen).</p>
 */
public final class PlasmaForgeRecipeGenerator {

    private PlasmaForgeRecipeGenerator() {}

    /** Original: {@code String autoPlate = "autoswitch.weldPlates"}. */
    private static final String AUTO_PLATE = "autoswitch.weldPlates";

    public static void generate(Consumer<FinishedRecipe> writer) {
        plates(writer);
        weldedPlates(writer);
        fusionVessel(writer);
        icf(writer);
        darkFusionCore(writer);
    }

    // ════════════════ Dunkler Fusionskern ════════════════

    /**
     * Die DFC-Rezepte aus {@code PlasmaForgeRecipes} (1.7.10). Alle brauchen 50 Millionen
     * Einspeisung und Sternenfluss; der Kern selbst ist mit 12.000 Ticks und 100 Millionen das
     * mit Abstand teuerste Rezept der ganzen Maschine.
     *
     * <p><b>Ersatzstoffe:</b> Dineutronium liegt in diesem Port nur als Platte vor, nicht als
     * Dichtdraht, und der Funken-Singularitaet ({@code singularity_spark}) gibt es hier nicht -
     * an ihrer Stelle steht ein Quantenschaltkreis mehr. Die Emitter-, Empfaenger- und
     * Injektorrezepte liefern die <b>arbeitenden</b> Bloecke ({@code core_*}), nicht die
     * gleichnamigen Platzhalter.
     */
    private static void darkFusionCore(java.util.function.Consumer<FinishedRecipe> writer) {

        // plsm.dfccore
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.DFC_CORE.get()), 12_000, 100_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 12_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 12)
                .addIngredient(ModItems.POWDER_CHLOROPHYTE.get(), 64)
                .save(writer, "plasma_forge/dfc_core");

        // plsm.dfcstabilizer
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.DFC_STABILIZER.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 8)
                .save(writer, "plasma_forge/dfc_stabilizer");

        // plsm.dfcemitter
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModItems.CORE_EMITTER.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.WIRE_DENSE), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 8)
                .save(writer, "plasma_forge/core_emitter");

        // plsm.dfcreceiver
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModItems.CORE_RECEIVER.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 8)
                .save(writer, "plasma_forge/core_receiver");

        // plsm.dfcinjector
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModItems.CORE_INJECTOR.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST), 16)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .save(writer, "plasma_forge/core_injector");
    }


    // ═══════════════════════ ICF ═══════════════════════

    /**
     * Die ICF-Rezepte aus {@code PlasmaForgeRecipes.registerDefaults()} (1.7.10). Alle laufen mit
     * 800 Ticks, 10.000.000 Energie und einer Million Einspeisung - nur der Reaktorkern braucht
     * 3.000 Ticks.
     *
     * <p><b>Ersatzstoffe.</b> Vier Zutaten des Originals hat dieser Port nicht; sie sind hier
     * durchgaengig ersetzt, damit die Rezepte ueberhaupt herstellbar sind:</p>
     * <ul>
     *   <li>Chlorophyt-Barren ({@code ingot_cft}) → Alliance-Stahlbarren</li>
     *   <li>Bismoidbronze-Gussblech → Bismutbronze-Barren (nur der Barren ist portiert)</li>
     *   <li>Dineutronium-Dichtdraht → Dineutronium-Platte</li>
     *   <li>Alliance-Stahl-Gussblech bzw. -Schweissblech → Alliance-Stahlplatte</li>
     * </ul>
     */
    private static void icf(Consumer<FinishedRecipe> writer) {

        // plsm.icfcell
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_LASER_CELL.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT), 2)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 4)
                .addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 16)
                .save(writer, "plasma_forge/icf_laser_cell");

        // plsm.icfemitter - das einzige ICF-Rezept mit Fluessigkeit
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_LASER_EMITTER.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE), 16)
                .addFluidInput(ModFluids.XENON.getSource(), 16_000)
                .save(writer, "plasma_forge/icf_laser_emitter");

        // plsm.icfcapacitor
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_LASER_CAPACITOR.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED), 1)
                .addIngredient(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 2)
                .save(writer, "plasma_forge/icf_laser_capacitor");

        // plsm.icfturbo
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_LASER_TURBOCHARGER.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED), 2)
                .addIngredient(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 4)
                .save(writer, "plasma_forge/icf_laser_turbocharger");

        // plsm.icfcasing
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_LASER_CASING.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT), 16)
                .save(writer, "plasma_forge/icf_laser_casing");

        // plsm.icfport
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_LASER_PORT.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 16)
                .save(writer, "plasma_forge/icf_laser_port");

        // plsm.icfcontroller
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_CONTROLLER.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT), 16)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 16)
                .save(writer, "plasma_forge/icf_controller");

        // plsm.icfscaffold - im Port heisst der Block ICF_COMPONENT_STRUCTURE
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_COMPONENT.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED), 2)
                .save(writer, "plasma_forge/icf_component");

        // plsm.icfvessel
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT), 1)
                .addIngredient(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE), 1)
                .addIngredient(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED), 2)
                .save(writer, "plasma_forge/icf_component_vessel");

        // plsm.icfstructural
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED), 2)
                .addIngredient(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED), 2)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 1)
                .save(writer, "plasma_forge/icf_component_structure");

        // plsm.icfcore - das teuerste Stueck, 3.000 Ticks
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()), 3_000, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 32)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 32)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 16)
                .save(writer, "plasma_forge/struct_icf_core");

        // plsm.icfpress
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.MACHINE_ICF_PRESS.get()), 800, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST), 8)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .addIngredient(ModItems.BISMOID_CIRCUIT.get(), 1)
                .save(writer, "plasma_forge/machine_icf_press");
    }


    // ═══════════════════════════════ Platten ═══════════════════════════════

    private static void plates(Consumer<FinishedRecipe> writer) {

        // plsm.plateeuphemium
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        ModMaterialItems.stack(ModMaterials.EUPHEMIUM, MaterialShape.PLATE, 4), 600, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.INGOT), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.ASTATINE, MaterialShape.POWDER), 3)
                .addIngredient(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.POWDER), 1)
                .addIngredient(ModItems.GEM_VOLCANIC.get(), 1)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.INGOT), 1)
                .save(writer, "plasma_forge/plate_euphemium");

        // plsm.platednt
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        ModMaterialItems.stack(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE, 4), 600, 10_000_000L)
                .inputEnergy(1_000_000L)
                .addIngredient(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.INGOT), 4)
                .addIngredient(ModItems.POWDER_SPARK_MIX.get(), 2)
                .addIngredient(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT), 1)
                .save(writer, "plasma_forge/plate_dineutronium");
    }

    // ══════════════════════════ Schweissplatten ══════════════════════════

    private static void weldedPlates(Consumer<FinishedRecipe> writer) {
        weld(writer, "iron",       ModMaterials.IRON,        50,          100L, null, 0);
        weld(writer, "steel",      ModMaterials.STEEL,       50,          500L, null, 0);
        weld(writer, "copper",     ModMaterials.COPPER,      50,        1_000L, null, 0);
        weld(writer, "titanium",   ModMaterials.TITANIUM,   300,       50_000L, null, 0);
        weld(writer, "zirconium",  ModMaterials.ZIRCONIUM,  300,       10_000L, null, 0);
        weld(writer, "aluminium",  ModMaterials.ALUMINIUM,  150,       10_000L, null, 0);
        weld(writer, "tcalloy",    ModMaterials.TCALLOY,    600,    1_000_000L, ModFluids.OXYGEN.getSource(), 1_000);
        weld(writer, "cdalloy",    ModMaterials.CDALLOY,    600,    1_000_000L, ModFluids.OXYGEN.getSource(), 1_000);
        weld(writer, "tungsten",   ModMaterials.TUNGSTEN,   600,      250_000L, ModFluids.OXYGEN.getSource(), 1_000);
        weld(writer, "cmb",        ModMaterials.CMB,        600,   10_000_000L, ModFluids.REFORMGAS.getSource(), 1_000);
        weld(writer, "osmiridium", ModMaterials.OSMIRIDIUM, 3_000, 50_000_000L, ModFluids.REFORMGAS.getSource(), 16_000);
    }

    /** Zwei Gussbleche werden zu einem Schweissblech - im Original alle in derselben Autoswitch-Gruppe. */
    private static void weld(Consumer<FinishedRecipe> writer, String name, ModMaterials mat,
                             int duration, long power, Fluid fluid, int fluidAmount) {

        PlasmaForgeRecipeBuilder builder = PlasmaForgeRecipeBuilder
                .plasmaForgeRecipe(ModMaterialItems.stack(mat, MaterialShape.PLATE_WELDED, 1), duration, power)
                .inputEnergy(500_000L)
                .addIngredient(Ingredient.of(ModMaterialItems.item(mat, MaterialShape.PLATE_CAST)), 2)
                .autoSwitchGroup(AUTO_PLATE);

        if (fluid != null) builder.addFluidInput(fluid, fluidAmount);

        builder.save(writer, "plasma_forge/weld_" + name);
    }

    // ═══════════════════════════ Fusionsgefaess ═══════════════════════════

    private static void fusionVessel(Consumer<FinishedRecipe> writer) {

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
