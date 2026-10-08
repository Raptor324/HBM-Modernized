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
import net.minecraft.world.item.Item;
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
        specials(writer);
        OreDictIngredients.checkMissing("PlasmaForgeRecipeGenerator");
    }

    // ════════ Konfig-Fassungen: Original inputItemsEx (Expensive) und setPools528 ════════

    /** Original {@code PlasmaForgeRecipes.POOL_PREFIX_528 + "chlorophyte"}. */
    private static final String POOL_528 = "528.chlorophyte";

    private static Ingredient it(String... ids) {
        Item[] items = new Item[ids.length];
        for (int i = 0; i < ids.length; i++) items[i] = OreDictIngredients.item(ids[i]);
        return Ingredient.of(items);
    }

    /**
     * Ein Rezept mit seinen Konfig-Fassungen ({@link ConfigRecipes}): {@code make.apply(true)} baut die
     * {@code inputItemsEx}-Fassung, {@code pool528} haengt im 528-Modus den Pool {@value #POOL_528} an.
     */
    private static void cfg(Consumer<FinishedRecipe> writer, String id,
                            java.util.function.Function<Boolean, PlasmaForgeRecipeBuilder> make, boolean ex, boolean pool528) {
        ConfigRecipes.Variants v = ConfigRecipes.variants(writer).base(w -> make.apply(false).save(w, id));
        if (ex && pool528) v.variant(w -> make.apply(true).blueprintPool(POOL_528).save(w, id), "528", "expensive");
        if (pool528) v.variant(w -> make.apply(false).blueprintPool(POOL_528).save(w, id), "528");
        if (ex) v.variant(w -> make.apply(true).save(w, id), "expensive");
        v.save();
    }

    // ════════ plsm.hde / plsm.schrabhammer / ass.fensusan / plsm.gerald ════════

    private static Item mi(ModMaterials m, MaterialShape s) { return ModMaterialItems.item(m, s); }

    /** Original {@code ANY_BISMOIDBRONZE.plateCast()}: Bismut- oder Arsenbronze-Gussplatte. */
    private static Ingredient bismoidCast() {
        return Ingredient.of(mi(ModMaterials.BBRONZE, MaterialShape.PLATE_CAST), mi(ModMaterials.ABRONZE, MaterialShape.PLATE_CAST));
    }

    /** Original {@code ANY_RESISTANTALLOY.plateWelded()}: Technetium- oder Kadmiumstahl-Schweissplatte. */
    private static Ingredient resistWelded() {
        return Ingredient.of(mi(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED), mi(ModMaterials.CDALLOY, MaterialShape.PLATE_WELDED));
    }

    /** Original {@code ANY_HARDPLASTIC.ingot()}: PC- oder PVC-Barren. */
    private static Ingredient hardPlastic() {
        return Ingredient.of(mi(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT), mi(ModMaterials.PVC, MaterialShape.INGOT));
    }

    private static void specials(Consumer<FinishedRecipe> writer) {

        // plsm.hde: Schwerlastelement
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(com.hbm_m.item.PartTabMetaItems.get("part_generic_hde").get()), 600, 25_000_000L)
                .inputEnergy(10_000_000L)
                .addIngredient(bismoidCast(), 2)
                .addIngredient(mi(ModMaterials.CMB, MaterialShape.PLATE_WELDED), 1)
                .addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 1)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .save(writer, "plasma_forge/hde");

        // plsm.schrabhammer: 35 Schrabidiumbloecke, 2x64 Yharonit, UFO-Muenze, 8x64 Meteoritenfragmente
        PlasmaForgeRecipeBuilder hammer = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModItems.SCHRABIDIUM_HAMMER.get()), 6_000, 10_000_000L)
                .inputEnergy(25_000_000L)
                .addIngredient(ModBlocks.getIngotBlock(ModMaterials.SCHRABIDIUM).get().asItem(), 35)
                .addIngredient(mi(ModMaterials.YHARONITE, MaterialShape.BILLET), 64)
                .addIngredient(mi(ModMaterials.YHARONITE, MaterialShape.BILLET), 64)
                .addIngredient(ModItems.COIN_UFO.get(), 1);
        for (int i = 0; i < 8; i++) hammer.addIngredient(ModItems.FRAGMENT_METEORITE.get(), 64);
        hammer.save(writer, "plasma_forge/schrabidium_hammer");

        // ass.fensusan: FEnSU (inputItemsEx im Expensive-Modus)
        cfg(writer, "plasma_forge/machine_battery_redd", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModItems.MACHINE_BATTERY_REDD.get()), 6_000, 50_000_000L)
                    .inputEnergy(50_000_000L);
            if (ex) {
                b.addIngredient(mi(ModMaterials.ELECTRONIUM, MaterialShape.INGOT), 64)
                        .addIngredient(ModItems.BATTERY_PACK_BATTERY_QUANTUM.get(), 1)
                        .addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 64)
                        .addIngredient(it("hbm_m:item_expensive_ferro_plating"), 64)
                        .addIngredient(mi(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 64)
                        .addIngredient(mi(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 64)
                        .addIngredient(mi(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 64)
                        .addIngredient(bismoidCast(), 64)
                        .addIngredient(mi(ModMaterials.CMB, MaterialShape.PLATE_CAST), 64)
                        .addIngredient(mi(ModMaterials.U238M2, MaterialShape.INGOT), 1)
                        .addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 64)
                        .addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 64);
                return b;
            }
            return b.addIngredient(mi(ModMaterials.ELECTRONIUM, MaterialShape.INGOT), 64)
                    .addIngredient(ModItems.BATTERY_PACK_BATTERY_QUANTUM.get(), 1)
                    .addIngredient(mi(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 64)
                    .addIngredient(mi(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 64)
                    .addIngredient(bismoidCast(), 64)
                    .addIngredient(mi(ModMaterials.CMB, MaterialShape.PLATE_CAST), 32)
                    .addIngredient(mi(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE), 32)
                    .addIngredient(mi(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE), 64)
                    .addIngredient(ModItems.POWDER_MAGIC.get(), 64)
                    .addIngredient(mi(ModMaterials.U238M2, MaterialShape.INGOT), 1)
                    .addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 64)
                    .addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 64);
        }, true, false);

        // plsm.gerald: Bauplan-Pool discover.gerald (inputItemsEx im Expensive-Modus); CONTROLLER_QUANTUM = quantum_computer
        cfg(writer, "plasma_forge/sat_gerald", ex -> {
            PlasmaForgeRecipeBuilder gerald = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModItems.SAT_GERALD.get()), 12_000, 50_000_000L)
                    .inputEnergy(25_000_000L);
            if (ex) {
                gerald.addIngredient(mi(ModMaterials.SCHRABIDATE, MaterialShape.PLATE_CAST), 64)
                        .addIngredient(mi(ModMaterials.BSCCO, MaterialShape.WIRE_DENSE), 64)
                        .addIngredient(ModBlocks.DET_NUKE.get().asItem(), 64);
                for (int i = 0; i < 3; i++) gerald.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 64);
                for (int i = 0; i < 3; i++) gerald.addIngredient(com.hbm_m.item.PartTabMetaItems.get("part_generic_hde").get(), 64);
                gerald.addIngredient(it("hbm_m:quantum_computer"), 64)
                        .addIngredient(it("hbm_m:item_expensive_computer"), 64)
                        .addIngredient(ModItems.COIN_UFO.get(), 1);
            } else {
                gerald.addIngredient(mi(ModMaterials.SCHRABIDATE, MaterialShape.PLATE_CAST), 64)
                        .addIngredient(mi(ModMaterials.SCHRABIDATE, MaterialShape.PLATE_CAST), 64)
                        .addIngredient(mi(ModMaterials.BSCCO, MaterialShape.WIRE_DENSE), 64)
                        .addIngredient(mi(ModMaterials.BSCCO, MaterialShape.WIRE_DENSE), 64)
                        .addIngredient(ModBlocks.DET_NUKE.get().asItem(), 64);
                for (int i = 0; i < 4; i++) gerald.addIngredient(com.hbm_m.item.PartTabMetaItems.get("part_generic_hde").get(), 64);
                gerald.addIngredient(it("hbm_m:quantum_computer"), 64)
                        .addIngredient(ModItems.COIN_UFO.get(), 1);
            }
            return gerald.blueprintPool("discover.gerald");
        }, true, false);
    }

    // ════════════════ Dunkler Fusionskern ════════════════

    /**
     * Die DFC-Rezepte aus {@code PlasmaForgeRecipes} (1.7.10). Alle brauchen 50 Millionen
     * Einspeisung und Sternenfluss; der Kern selbst ist mit 12.000 Ticks und 100 Millionen das
     * mit Abstand teuerste Rezept der ganzen Maschine.
     *
     * <p>CONTROLLER_QUANTUM = {@code quantum_computer}, CONTROLLER_ADVANCED = {@code controller_advanced}.
     * Die Emitter-, Empfaenger- und
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
                .addIngredient(ModMaterialItems.item(ModMaterials.DNT, MaterialShape.WIRE_DENSE), 16)
                .addIngredient(ModItems.QUANTUM_COMPUTER.get(), 12)
                .addIngredient(ModItems.SINGULARITY_SPARK.get(), 1)
                .addIngredient(ModItems.POWDER_CHLOROPHYTE.get(), 64)
                .save(writer, "plasma_forge/dfc_core");

        // plsm.dfcstabilizer
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModBlocks.DFC_STABILIZER.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 16)
                .addIngredient(ModItems.QUANTUM_COMPUTER.get(), 8)
                .save(writer, "plasma_forge/dfc_stabilizer");

        // plsm.dfcemitter
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModItems.CORE_EMITTER.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.WIRE_DENSE), 16)
                .addIngredient(ModItems.QUANTUM_COMPUTER.get(), 8)
                .save(writer, "plasma_forge/core_emitter");

        // plsm.dfcreceiver
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModItems.CORE_RECEIVER.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.PLATE_CAST), 16)
                .addIngredient(ModItems.QUANTUM_COMPUTER.get(), 8)
                .save(writer, "plasma_forge/core_receiver");

        // plsm.dfcinjector
        PlasmaForgeRecipeBuilder.plasmaForgeRecipe(
                        new ItemStack(ModItems.CORE_INJECTOR.get()), 1_200, 10_000_000L)
                .inputEnergy(50_000_000L)
                .addFluidInput(ModFluids.STELLAR_FLUX.getSource(), 4_000)
                .addIngredient(ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED), 16)
                .addIngredient(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST), 16)
                .addIngredient(ModItems.CONTROLLER_ADVANCED.get(), 4)
                .save(writer, "plasma_forge/core_injector");
    }


    // ═══════════════════════ ICF ═══════════════════════

    /**
     * Die ICF-Rezepte aus {@code PlasmaForgeRecipes.registerDefaults()} (1.7.10). Alle laufen mit
     * 800 Ticks, 10.000.000 Energie und einer Million Einspeisung - nur der Reaktorkern braucht
     * 3.000 Ticks.
     *
     * <p>Zutaten 1:1: {@code ANY_BISMOIDBRONZE.plateCast()} = Bismut-/Arsenbronze-Gussplatte,
     * {@code ANY_RESISTANTALLOY.plateWelded()} = Technetium-/Kadmiumstahl-Schweissplatte,
     * {@code ANY_HARDPLASTIC.ingot()} = PC/PVC-Barren, {@code ingot_cft} = CFT-Barren.</p>
     */
    private static void icf(Consumer<FinishedRecipe> writer) {

        // plsm.icfcell
        cfg(writer, "plasma_forge/icf_laser_cell", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_LASER_CELL.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 2).addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 8).addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 16);
            else b.addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 2).addIngredient(bismoidCast(), 4).addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 16);
            return b;
        }, true, true);

        // plsm.icfemitter - das einzige ICF-Rezept mit Fluessigkeit
        cfg(writer, "plasma_forge/icf_laser_emitter", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_LASER_EMITTER.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(mi(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED), 8).addIngredient(mi(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE), 16);
            else b.addIngredient(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED), 4).addIngredient(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE), 16);
            b.addFluidInput(ModFluids.XENON.getSource(), 16_000);
            return b;
        }, true, true);

        // plsm.icfcapacitor
        cfg(writer, "plasma_forge/icf_laser_capacitor", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_LASER_CAPACITOR.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_ferro_plating"), 3).addIngredient(mi(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 16).addIngredient(mi(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 2);
            else b.addIngredient(resistWelded(), 1).addIngredient(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 16).addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 2);
            return b;
        }, true, true);

        // plsm.icfturbo
        cfg(writer, "plasma_forge/icf_laser_turbocharger", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_LASER_TURBOCHARGER.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(resistWelded(), 8).addIngredient(mi(ModMaterials.DNT, MaterialShape.WIRE_DENSE), 8).addIngredient(mi(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 4);
            else b.addIngredient(resistWelded(), 2).addIngredient(mi(ModMaterials.DNT, MaterialShape.WIRE_DENSE), 4).addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 4);
            return b;
        }, true, true);

        // plsm.icfcasing
        cfg(writer, "plasma_forge/icf_laser_casing", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_LASER_CASING.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 4).addIngredient(mi(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST), 4).addIngredient(hardPlastic(), 16);
            else b.addIngredient(bismoidCast(), 4).addIngredient(mi(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST), 4).addIngredient(hardPlastic(), 16);
            return b;
        }, true, true);

        // plsm.icfport
        cfg(writer, "plasma_forge/icf_laser_port", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_LASER_PORT.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 4).addIngredient(hardPlastic(), 16).addIngredient(mi(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 16);
            else b.addIngredient(bismoidCast(), 4).addIngredient(hardPlastic(), 16).addIngredient(ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 16);
            return b;
        }, true, true);

        // plsm.icfcontroller
        cfg(writer, "plasma_forge/icf_controller", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_CONTROLLER.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 4).addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 16).addIngredient(hardPlastic(), 16).addIngredient(ModItems.BISMOID_CIRCUIT.get(), 32).addIngredient(it("hbm_m:item_expensive_computer"), 4);
            else b.addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 16).addIngredient(bismoidCast(), 4).addIngredient(hardPlastic(), 16).addIngredient(ModItems.BISMOID_CIRCUIT.get(), 16);
            return b;
        }, true, true);

        // plsm.icfscaffold - im Port heisst der Block ICF_COMPONENT_STRUCTURE
        cfg(writer, "plasma_forge/icf_component", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_COMPONENT.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_steel_plating"), 8);
            else b.addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED), 4).addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED), 2);
            return b;
        }, true, true);

        // plsm.icfvessel
        cfg(writer, "plasma_forge/icf_component_vessel", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_COMPONENT_VESSEL.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            b.addIngredient(mi(ModMaterials.CFT, MaterialShape.INGOT), 1).addIngredient(mi(ModMaterials.CMB, MaterialShape.PLATE_CAST), 1).addIngredient(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED), 2);
            return b;
        }, false, true);

        // plsm.icfstructural
        cfg(writer, "plasma_forge/icf_component_structure", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.ICF_COMPONENT_STRUCTURE.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 1).addIngredient(mi(ModMaterials.STEEL, MaterialShape.PLATE_WELDED), 8);
            else b.addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED), 2).addIngredient(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED), 2).addIngredient(bismoidCast(), 1);
            return b;
        }, true, true);

        // plsm.icfcore - das teuerste Stueck, 3.000 Ticks
        cfg(writer, "plasma_forge/struct_icf_core", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.STRUCT_ICF_CORE.get()), 3_000, 10_000_000L).inputEnergy(1_000_000L);
            if (ex) b.addIngredient(it("hbm_m:item_expensive_bronze_tubes"), 16).addIngredient(mi(ModMaterials.CMB, MaterialShape.PLATE_WELDED), 16).addIngredient(mi(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 32).addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 32).addIngredient(it("hbm_m:item_expensive_computer"), 16);
            else b.addIngredient(mi(ModMaterials.CMB, MaterialShape.PLATE_WELDED), 16).addIngredient(resistWelded(), 16).addIngredient(bismoidCast(), 16).addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 32).addIngredient(ModItems.BISMOID_CIRCUIT.get(), 32).addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 16);
            return b;
        }, true, true);

        // plsm.icfpress
        cfg(writer, "plasma_forge/machine_icf_press", ex -> {
            PlasmaForgeRecipeBuilder b = PlasmaForgeRecipeBuilder.plasmaForgeRecipe(new ItemStack(ModBlocks.MACHINE_ICF_PRESS.get()), 800, 10_000_000L).inputEnergy(1_000_000L);
            b.addIngredient(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST), 8).addIngredient(ModItems.MOTOR.get(), 4).addIngredient(ModItems.BISMOID_CIRCUIT.get(), 1);
            return b;
        }, false, true);
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
        // Original setPools528("528.chlorophyte")
        cfg(writer, "plasma_forge/fusion_vessel", ex -> {
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

            return builder;
        }, false, true);
    }
}
//?}
