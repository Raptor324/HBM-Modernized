package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.datagen.recipes.ModRecipeProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;

/**
 * Port-Notbehelfe fuer die Montagemaschine. Die Originalrezepte stehen 1:1 in {@link AssemblyMachineRecipeGenerator};
 * hier bleiben nur Rezepte fuer Dinge, die das Original NICHT in der Montagemaschine baut (Amboss-Konstruktion,
 * Werkbank, Lichtbogenschweisser), deren Original-Rezeptweg im Port aber noch fehlt - sonst waeren sie im
 * Survival nicht herstellbar - sowie Port-eigene Inhalte (9M723, Topol, mobile Startrampe). Sobald der
 * Original-Weg portiert ist, gehoert das jeweilige Rezept hier entfernt.
 */
public final class AssemblerRecipeGenerator {

    private AssemblerRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerMainRecipes(writer);
        registerElectronics(writer);
        registerCastPlateRecipes(writer);
        registerMissileRecipes(writer);
        // registerAccelerators: entfernt - erzeugte den Platzhalterblock "source"; die echte Quelle (pa_source) kommt aus assembler/source.json
        registerSpace(writer);
        registerGapMachines(writer);
    }

    /**
     * Gap-fill: machines that were fully implemented (block/blockentity/menu/GUI) but had no way to be
     * crafted in survival. Ported from 1.7.10 AssemblyMachineRecipes.java where a matching ass.* entry
     * exists; ANY_RESISTANTALLOY is substituted with ADVANCED_ALLOY (no oredict tag-aggregate equivalent
     * in this port, same substitution convention as elsewhere in this file), ANY_TAR with RUBBER (no
     * tar-specific material exists in this port).
     * (Amboss-Konstruktionen wie Verbrennungsofen, Deuteriumturm, Sterling, Saege usw. stehen jetzt 1:1 im
     * AnvilConstructionRecipeGenerator und sind hier entfernt.) The plain reactor Breeder
     * (not Fusion Breeder) had no craftable recipe anywhere in the original (loot/starter-kit only) — its
     * recipe below is invented, scaled to the same tier as the other reactor multiblock parts in
     * registerReactors().
     */
    private static void registerGapMachines(Consumer<FinishedRecipe> writer) {

        // Foerderbaender: nur ueber den Foerderband-Stab (MachineCraftingRecipeGenerator), wie im Original.

        // Compressor already has an Assembler recipe registered further down this file (from an
        // earlier pass) - not duplicating it here.

        // Foerderbandpresse: Werkbankrezept 1:1 im MachineCraftingRecipeGenerator.

        // Industrial Generator — no ass.* recipe existed in the original 1.7.10 either; invented here.
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.INDUSTRIAL_GENERATOR.get(), 1), 200, 120)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT), 24)
                .addIngredient(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE), 12)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.PIPE_STEEL.get(), 4)
                .save(writer, "industrial_generator");

        // Breeder (plain fission reactor breeder, not Fusion Breeder) — genuinely had no craftable recipe
        // anywhere in the original (obtainable only via loot/starter kit). Invented here, scaled to the
        // same tier as the other reactor multiblock components in registerReactors().
        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModBlocks.BREEDER.get(), 1), 300, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE), 8)
                .addIngredient(ModItems.NEUTRON_REFLECTOR.get(), 4)
                .addIngredient(ModItems.PIPE_STEEL.get(), 6)
                .addIngredient(ModItems.MOTOR.get(), 2)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 2)
                .save(writer, "breeder");

    }

    // Teilchenbeschleuniger-Spulen (pa_coil): jetzt 1:1 in AssemblyMachineRecipeGenerator (ass.pa_coil*).

    /** Particle accelerator components — port of 1.7.10 beamline/rfc/quadrupole/dipole/source/detector/exposurechamber. */
    private static void registerAccelerators(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.SOURCE.get(), 1), 400, 100)
                .addIngredient(ModBlocks.BEAMLINE.get().asItem(), 3)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST), 16)
                .addIngredient(Ingredient.of(
                        ModMaterialItems.item(ModMaterials.PVC, MaterialShape.INGOT),
                        ModMaterialItems.item(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT)), 16)
                .addIngredient(ModItems.MAGNETRON.get(), 16)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 1)
                .save(writer, "source");

    }

    // Die Fusionsreaktor-Rezepte stehen jetzt vollstaendig in FusionAssemblerRecipeGenerator:
    // dort sind alle elf Originalrezepte enthalten (inkl. Klystron, MHDT, Koppler, Plasmaschmiede
    // und Torus-Kern) und die ANY_RESISTANTALLOY-/Schaltkreis-Zuordnung entspricht dem Original.

    /** Space program — port of 1.7.10 soyuzcore/satellitemapper/satellitescanner/satelliteradar/satelliteresonator. */
    private static net.minecraft.world.item.Item rid(String id) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        if (item == net.minecraft.world.item.Items.AIR) throw new IllegalStateException("Assembler: unbekannter Gegenstand " + id);
        return item;
    }

    private static void registerSpace(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_MAPPER.get(), 1), 600, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 3)
                .addIngredient(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE), 4)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .addIngredient(ModBlocks.GLASS_QUARTZ.get().asItem(), 8)
                .save(writer, "satellitemapper");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_SCANNER.get(), 1), 600, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 3)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE), 4)
                .addIngredient(ModItems.MAGNETRON.get(), 8)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 8)
                .save(writer, "satellitescanner");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_RADAR.get(), 1), 600, 100)
                .addIngredient(ModItems.SHELL_STEEL.get(), 3)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST), 12)
                .addIngredient(ModItems.MAGNETRON.get(), 12)
                .addIngredient(ModItems.COIL_GOLD.get(), 16)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 4)
                .save(writer, "satelliteradar");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.SAT_HEAD_RESONATOR.get(), 1), 600, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST), 6)
                .addIngredient(ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.INGOT), 12)
                .addIngredient(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), 48)
                .addIngredient(ModMaterialItems.item(ModMaterials.XEN, MaterialShape.CRYSTAL), 1)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 16)
                .save(writer, "satelliteresonator");
    }

    private static void registerMainRecipes(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.GRENADE_NUC.get(), 3), 160, 250)
                .addIngredient(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET), 1)
                .addIngredient(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE), 6)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 3)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 3)
                .save(writer, "grenade_nuc");

        // machine_large_turbine — no ass.machineLargeTurbine recipe existed in the original 1.7.10 either; invented here.
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.MACHINE_LARGE_TURBINE.get(), 1), 320, 500)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 24)
                .addIngredient(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT), 8)
                .addIngredient(ModItems.TURBINE_TUNGSTEN.get(), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE_DENSE), 8)
                .addIngredient(ModItems.PIPE_DURA_STEEL.get(), 8)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 4)
                .save(writer, "machine_large_turbine");

        // reactor_research — no ass.reactorResearch recipe existed in the original 1.7.10 either; invented here.
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.REACTOR_RESEARCH.get(), 1), 200, 350)
                .addIngredient(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT), 8)
                .addIngredient(ModItems.INTEGRATED_CIRCUIT.get(), 2)
                .save(writer, "reactor_research");

    }

    private static void registerElectronics(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.CONTROLLER.get(), 1), 120, 250)
                .addIngredient(ModMaterialItems.item(ModMaterials.CARBON, MaterialShape.WIRE), 16)
                .addIngredient(ModItems.CAPACITOR.get(), 64)
                .addIngredient(ModItems.MICROCHIP.get(), 32)
                .addIngredient(ModItems.CONTROLLER_CHASSIS.get(), 1)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 1)
                .addIngredient(ModItems.PCB.get(), 16)
                .save(writer, "controller");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.MACHINE_BATTERY.get(), 1), 80, 150)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 12)
                .addIngredient(ModItems.SULFUR.get(), 12)
                .addIngredient(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.POWDER), 12)
                .save(writer, "battery");

    }

    private static void registerCastPlateRecipes(Consumer<FinishedRecipe> writer) {

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST), 1), 60, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_steel_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST), 1), 60, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_copper_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE_CAST), 1), 60, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_gold_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST), 1), 80, 150)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_titanium_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST), 1), 60, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_aluminium_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE_CAST), 1), 100, 200)
                .addIngredient(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_dura_steel_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE_CAST), 1), 100, 200)
                .addIngredient(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_desh_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE_CAST), 1), 120, 300)
                .addIngredient(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_schrabidium_from_plates");

        AssemblerRecipeBuilder.assemblerRecipe(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE_CAST), 1), 80, 150)
                .addIngredient(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE), 2)
                .save(writer, "plate_cast_saturnite_from_plates");
    }

    private static void registerMissileRecipes(Consumer<FinishedRecipe> writer) { // TODO: WIP RECIPES, NEEDS REWORK WHEN FULL PROCESSING IS PORTED

        // Tier 0 — micro / ABM (cheap test crafts)
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_TEST.get(), 1), 60, 100)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 2)
                .addIngredient(Items.IRON_INGOT, 4)
                .addIngredient(Items.REDSTONE, 2)
                .addIngredient(Items.GUNPOWDER, 4)
                .save(writer, "missile_test");

        // missile_taint: Werkbankrezept 1:1 (WeaponRecipes, Watz-Schlamm im Behaelter) im ModVanillaRecipeProvider

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_DECOY.get(), 1), 80, 150)
                .addIngredient(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE), 4)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 2)
                .addIngredient(Items.REDSTONE, 4)
                .addIngredient(Items.PAPER, 4)
                .save(writer, "missile_decoy");

        // Tier 2 — strong (missile_strong собирается в дуговой сварке из warhead/fuel_tank/thruster)

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_INFERNO.get(), 1), 120, 250)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 6)
                .addIngredient(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE), 2)
                .addIngredient(Items.BLAZE_POWDER, 16)
                .addIngredient(Items.GUNPOWDER, 8)
                .save(writer, "missile_inferno");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_9M723.get(), 1), 160, 250)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 6)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 2)
                .addIngredient(ModItems.BALL_TNT.get(), 4)
                .save(writer, "missile_9m723");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_9M723_BUSTER.get(), 1), 160, 250)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 6)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 2)
                .addIngredient(ModItems.DRILL_TITANIUM.get(), 1)
                .save(writer, "missile_9m723_buster");

        // Mobile Startrampe: Fahrgestell aus Stahl und Titan, dazu die Elektronik der Rampe.
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MOBILE_LAUNCH_PAD.get(), 1), 400, 400)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 24)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 16)
                .addIngredient(ModItems.MOTOR.get(), 8)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 6)
                .save(writer, "mobile_launch_pad");

        // Topol-M-Werfer: das groesste Fahrzeug der Mod, entsprechend teuer.
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.TOPOL_LAUNCH_PAD.get(), 1), 600, 600)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 48)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 32)
                .addIngredient(ModItems.MOTOR.get(), 16)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 12)
                .save(writer, "topol_launch_pad");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_TOPOL.get(), 1), 400, 500)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 24)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 16)
                .addIngredient(ModItems.ANALOG_CIRCUIT.get(), 4)
                .addIngredient(ModItems.MOTOR.get(), 4)
                .save(writer, "missile_topol");

        // ─── Grossmaschinen, die bisher gar nicht herstellbar waren ──────────────
        // Im Original haengen sie an Bauteilen, die es im Port noch nicht gibt (Schwerrahmen,
        // Hydraulikkolben, SC-Batterien). Die Rezepte hier sind darum keine Eins-zu-eins-Kopie,
        // sondern eine Naeherung auf vorhandenen Teilen - in Aufwand und Stufe vergleichbar.

        // Traegheitsfusion und Druckwasserreaktor: im Original Baugruppen mit Bauteilen, die der
        // Port noch nicht hat. Naeherung auf vorhandenen Teilen, Stufe bleibt spaetes Spiel.
        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.ICF.get().asItem(), 1), 800, 600)
                .addIngredient(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST), 32)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 24)
                .addIngredient(ModItems.QUANTUM_CIRCUIT.get(), 4)
                .addIngredient(ModItems.COIL_TUNGSTEN.get(), 16)
                .save(writer, "icf");

        // Laserwaffenstellung: Naeherung. (Die Sojus-Rampe entsteht wie im Original nur ueber struct_soyuz_core.)

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModBlocks.LPW2.get().asItem(), 1), 600, 500)
                .addIngredient(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 24)
                .addIngredient(ModItems.COIL_TUNGSTEN.get(), 12)
                .addIngredient(ModItems.ADVANCED_CIRCUIT.get(), 6)
                .addIngredient(ModItems.CRT_DISPLAY.get(), 1)
                .save(writer, "lpw2");

        AssemblerRecipeBuilder.assemblerRecipe(
                        new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED.get(), 1), 160, 300)
                .addIngredient(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 8)
                .addIngredient(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET), 2)
                .addIngredient(Items.IRON_NUGGET, 16)
                .addIngredient(ModItems.BALL_TNT.get(), 2)
                .save(writer, "missile_doomsday_rusted");

    }

}
//?}