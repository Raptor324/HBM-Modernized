package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

/**
 * Werkbankrezepte fuer Maschinen, die im Port bisher ueberhaupt nicht herstellbar waren.
 *
 * <p>Ohne diese Rezepte existieren die betroffenen Bloecke nur im Kreativmenue - im Ueberleben
 * sind sie unerreichbar, und mit ihnen alles, was auf ihnen aufbaut. Betroffen waren unter
 * anderem die gesamte Stromverkabelung, die Fluidleitungen und mehrere Grundmaschinen.
 *
 * <p>Die Rezepte sind aus dem Original uebernommen ({@code com.hbm.main.CraftingManager} in
 * 1.7.10), Muster und Mengen eins zu eins. Wo der Port ein Material nicht kennt, steht die
 * Ersetzung als Kommentar an der jeweiligen Zeile.
 */
public final class MachineCraftingRecipeGenerator {

    private MachineCraftingRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> writer) {
        powerGrid(writer);
        fluidNetwork(writer);
        machines(writer);
        redstoneRadio(writer);
        logistics(writer);
        cranesAndTubes(writer);
        oddsAndEnds(writer);
    }

    // ─── Strom ────────────────────────────────────────────────────────────────

    private static void powerGrid(Consumer<FinishedRecipe> writer) {
        // Original: " W ", "RRR", " W " mit W = plate_polymer, R = MINGRADE.wireFine()
        shaped(writer, "red_cable", ModBlocks.RED_CABLE.get(), 16,
                new String[]{" W ", "RRR", " W "},
                'W', polymer(MaterialShape.PLATE),
                'R', mat(ModMaterials.RED_COPPER, MaterialShape.WIRE));

        // Original: "WRW", "RIR", "WRW"
        shaped(writer, "red_cable_paintable", ModBlocks.RED_CABLE_PAINTABLE.get(), 16,
                new String[]{"WRW", "RIR", "WRW"},
                'W', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'I', mat(ModMaterials.RED_COPPER, MaterialShape.INGOT),
                'R', mat(ModMaterials.RED_COPPER, MaterialShape.WIRE));

        shapeless(writer, "red_cable_classic", ModBlocks.RED_CABLE_CLASSIC.get(), 1,
                ModBlocks.RED_CABLE.get());
        shapeless(writer, "red_cable_from_classic", ModBlocks.RED_CABLE.get(), 1,
                ModBlocks.RED_CABLE_CLASSIC.get());

        // Original: red_wire_coated + Stahl + Basisschaltkreis; der Port kennt den beschichteten
        // Draht nicht, deshalb das fertige Kabel als Ausgangsteil.
        shapeless(writer, "red_cable_gauge", ModBlocks.RED_CABLE_GAUGE.get(), 1,
                ModBlocks.RED_CABLE.get(),
                mat(ModMaterials.STEEL, MaterialShape.INGOT),
                ModItems.ANALOG_CIRCUIT.get());

        // Original (Amboss, Stufe 2): 8 Beton, 8 Stahlbarren, 12 Polymerplatten, 8 Kupferspulen.
        // Als Werkbankrezept gefasst, damit das Stromnetz nicht am Amboss haengt.
        shaped(writer, "substation", ModBlocks.SUBSTATION.get(), 2,
                new String[]{"CPC", "ISI", "CPC"},
                'C', ModBlocks.BRICK_CONCRETE.get(),
                'P', polymer(MaterialShape.PLATE),
                'I', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'S', ModItems.COIL_COPPER.get());

        // Original (Amboss, Stufe 2): 2 Beton, 8 Stahlgeruest, 8 Polymerplatten, 4 Kupferspulen.
        shaped(writer, "red_pylon_large", ModBlocks.RED_PYLON_LARGE.get(), 1,
                new String[]{"SPS", "SCS", "SBS"},
                'S', ModBlocks.STEEL_SCAFFOLD.get(),
                'P', polymer(MaterialShape.PLATE),
                'C', ModItems.COIL_COPPER.get(),
                'B', ModBlocks.BRICK_CONCRETE.get());

        shaped(writer, "large_pylon", ModBlocks.LARGE_PYLON.get(), 1,
                new String[]{"SCS", "SPS", "SBS"},
                'S', ModBlocks.STEEL_SCAFFOLD.get(),
                'C', ModItems.COIL_COPPER.get(),
                'P', polymer(MaterialShape.PLATE),
                'B', ModBlocks.BRICK_CONCRETE.get());

        // Kondensatoren: im Original ueber die Baugruppen, hier als einfache Werkbankstufe,
        // damit Energiepufferung ueberhaupt erreichbar ist.
        capacitor(writer, "capacitor_copper", ModBlocks.CAPACITOR_COPPER.get(),
                mat(ModMaterials.COPPER, MaterialShape.PLATE));
        capacitor(writer, "capacitor_gold", ModBlocks.CAPACITOR_GOLD.get(), Items.GOLD_INGOT);
        capacitor(writer, "capacitor_tantalium", ModBlocks.CAPACITOR_TANTALIUM.get(),
                mat(ModMaterials.TANTALIUM, MaterialShape.INGOT));

        shaped(writer, "capacitor_bus", ModBlocks.CAPACITOR_BUS.get(), 1,
                new String[]{"PPP", "CCC", "PPP"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'C', mat(ModMaterials.RED_COPPER, MaterialShape.WIRE));

        // Original: "RRR", "WWW", "III"
        shaped(writer, "machine_converter_he_rf", ModBlocks.MACHINE_CONVERTER_HE_RF.get(), 1,
                new String[]{"RRR", "WWW", "III"},
                'R', ModItems.CAPACITOR_BOARD.get(),
                'W', Items.REDSTONE,
                'I', mat(ModMaterials.STEEL, MaterialShape.INGOT));

        shaped(writer, "machine_converter_rf_he", ModBlocks.MACHINE_CONVERTER_RF_HE.get(), 1,
                new String[]{"RRR", "WWW", "III"},
                'R', Items.REDSTONE,
                'W', mat(ModMaterials.RED_COPPER, MaterialShape.WIRE),
                'I', mat(ModMaterials.STEEL, MaterialShape.INGOT));
    }

    private static void capacitor(Consumer<FinishedRecipe> writer, String name, ItemLike out, ItemLike metal) {
        shaped(writer, name, out, 1,
                new String[]{"MPM", "MCM", "MPM"},
                'M', metal,
                'P', polymer(MaterialShape.PLATE),
                'C', ModItems.CAPACITOR_BOARD.get());
    }

    // ─── Fluide ───────────────────────────────────────────────────────────────

    private static void fluidNetwork(Consumer<FinishedRecipe> writer) {
        // Original (Amboss): 1 Stahlplatte + 1 Polymerplatte -> 8 Leitungen.
        // Achtung: die Leitungsbloecke haben kein eigenes BlockItem, ihr Item steht in ModItems.
        shapeless(writer, "fluid_duct", ModItems.FLUID_DUCT.get(), 8,
                mat(ModMaterials.STEEL, MaterialShape.PLATE),
                polymer(MaterialShape.PLATE));

        shapeless(writer, "fluid_duct_colored", ModItems.FLUID_DUCT_COLORED.get(), 1,
                ModItems.FLUID_DUCT.get(), Items.WHITE_DYE);

        shapeless(writer, "fluid_duct_silver", ModItems.FLUID_DUCT_SILVER.get(), 1,
                ModItems.FLUID_DUCT.get(), mat(ModMaterials.ALUMINUM, MaterialShape.PLATE));

        // CraftingManager: fraction_spacer
        shaped(writer, "fraction_spacer", ModBlocks.FRACTION_SPACER.get(), 1,
                new String[]{"BHB"},
                'H', ModItems.SHELL_STEEL.get(),
                'B', Items.IRON_BARS);

        // CraftingManager: crane_partitioner
        shaped(writer, "crane_partitioner", ModBlocks.CRANE_PARTITIONER.get(), 1,
                new String[]{" M ", "BCB"},
                'M', ModItems.MICROCHIP.get(),
                'B', ModItems.CONVEYOR_WAND_REGULAR.get(),
                'C', ModItems.CRATE_STEEL.get());

        // CraftingManager: pipe_anchor
        shaped(writer, "pipe_anchor", ModBlocks.PIPE_ANCHOR.get(), 2,
                new String[]{"P", "P", "S"},
                'P', ModItems.PIPE_STEEL.get(),
                'S', mat(ModMaterials.STEEL, MaterialShape.INGOT));

        // CraftingManager: pipe_anchor_exhaust
        shaped(writer, "pipe_anchor_exhaust", ModBlocks.PIPE_ANCHOR_EXHAUST.get(), 2,
                new String[]{"P", "P", "S"},
                'P', ModItems.PIPE_IRON.get(),
                'S', polymer(MaterialShape.PLATE));

        // CraftingManager: machine_tape_drive (P = beliebiger Kunststoff, C = Leiterplatte)
        shaped(writer, "machine_tape_drive", ModBlocks.MACHINE_TAPE_DRIVE.get(), 1,
                new String[]{"PPP", "CCC", "PPP"},
                'C', com.hbm_m.item.ModItems.PCB.get(),
                'P', OreDictIngredients.ore("oredict/ingot/any_plastic"));

        // CraftingManager: pipe_anchor_pneumatic (P = geschweisste Kupferplatte, S = beliebiger Gummi)
        shaped(writer, "pipe_anchor_pneumatic", ModBlocks.PIPE_ANCHOR_PNEUMATIC.get(), 2,
                new String[]{"P", "P", "S"},
                'P', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED),
                'S', OreDictIngredients.ore("oredict/ingot/any_rubber"));

        // CraftingManager: fluid_duct_paintable / fluid_duct_paintable_block_exhaust
        shaped(writer, "fluid_duct_paintable", ModBlocks.FLUID_DUCT_PAINTABLE.get(), 8,
                new String[]{"SAS", "A A", "SAS"},
                'S', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'A', mat(ModMaterials.ALUMINUM, MaterialShape.PLATE));
        shaped(writer, "fluid_duct_paintable_block_exhaust", ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get(), 8,
                new String[]{"SAS", "A A", "SAS"},
                'S', Items.IRON_INGOT,
                'A', mat(ModMaterials.POLYMER, MaterialShape.PLATE));

        // CraftingManager: fluid_duct_gauge (addShapelessAuto)
        shapeless(writer, "fluid_duct_gauge", ModBlocks.FLUID_DUCT_GAUGE.get(), 1,
                ModBlocks.FLUID_DUCT_PAINTABLE.get(), mat(ModMaterials.STEEL, MaterialShape.INGOT), ModItems.INTEGRATED_CIRCUIT.get());

        // CraftingManager: fluid_valve / fluid_switch / fluid_counter_valve
        shaped(writer, "fluid_valve", ModBlocks.FLUID_VALVE.get(), 1,
                new String[]{"S", "W"},
                'S', Items.LEVER,
                'W', ModBlocks.FLUID_DUCT_PAINTABLE.get());
        shaped(writer, "fluid_switch", ModBlocks.FLUID_SWITCH.get(), 1,
                new String[]{"S", "W"},
                'S', Items.REDSTONE,
                'W', ModBlocks.FLUID_DUCT_PAINTABLE.get());
        shaped(writer, "fluid_counter_valve", ModBlocks.FLUID_COUNTER_VALVE.get(), 1,
                new String[]{"S", "W"},
                'S', ModItems.MICROCHIP.get(),
                'W', ModBlocks.FLUID_SWITCH.get());

        shaped(writer, "fluid_exhaust", ModBlocks.FLUID_EXHAUST.get(), 1,
                new String[]{"P", "D"},
                'P', mat(ModMaterials.IRON, MaterialShape.PLATE),
                'D', ModItems.FLUID_DUCT.get());

        // Original: " S ", "PGP", "IMI"
        shaped(writer, "fluid_pump", ModBlocks.FLUID_PUMP.get(), 1,
                new String[]{" S ", "PGP", "IMI"},
                'S', ModItems.SHELL_STEEL.get(),
                'P', ModItems.PIPE_STEEL.get(),
                'G', mat(ModMaterials.GRAPHITE, MaterialShape.INGOT),
                'I', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'M', ModItems.MOTOR.get());

        // Original: "IPI", "I I", "IPI"
        shaped(writer, "barrel_steel", ModBlocks.BARREL_STEEL.get(), 1,
                new String[]{"IPI", "I I", "IPI"},
                'I', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'P', mat(ModMaterials.STEEL, MaterialShape.INGOT));

        shaped(writer, "barrel_plastic", ModBlocks.BARREL_PLASTIC.get(), 1,
                new String[]{"IPI", "I I", "IPI"},
                'I', polymer(MaterialShape.PLATE),
                'P', mat(ModMaterials.ALUMINUM, MaterialShape.PLATE));

        shaped(writer, "barrel_iron", ModBlocks.BARREL_IRON.get(), 1,
                new String[]{"IPI", "I I", "IPI"},
                'I', mat(ModMaterials.IRON, MaterialShape.PLATE),
                'P', Items.IRON_INGOT);

        // Original: "PPP", "T  ", "PPP"
        shaped(writer, "machine_drain", ModBlocks.MACHINE_DRAIN.get(), 1,
                new String[]{"PPP", "T  ", "PPP"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE_CAST),
                'T', ModItems.TANK_STEEL.get());

        // Giesserei: Original "B B"/"BSB" bzw. mit Stahlring, dazu zwei formlose Rezepte.
        shaped(writer, "foundry_mold", ModBlocks.FOUNDRY_MOLD.get(), 1,
                new String[]{"B B", "BSB"},
                'B', ModItems.FIREBRICK.get(),
                'S', Blocks.STONE_SLAB);

        shaped(writer, "foundry_tank", ModBlocks.FOUNDRY_TANK.get(), 1,
                new String[]{"B B", "I I", "BSB"},
                'B', ModItems.FIREBRICK.get(),
                'I', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'S', Blocks.STONE_SLAB);

        shapeless(writer, "foundry_outlet", ModBlocks.FOUNDRY_OUTLET.get(), 1,
                ModBlocks.FOUNDRY_CHANNEL.get(), mat(ModMaterials.STEEL, MaterialShape.PLATE));

        shapeless(writer, "foundry_slagtap", ModBlocks.FOUNDRY_SLAGTAP.get(), 1,
                ModBlocks.FOUNDRY_CHANNEL.get(), Blocks.STONE_BRICKS);
    }

    // ─── Maschinen ────────────────────────────────────────────────────────────

    private static void machines(Consumer<FinishedRecipe> writer) {
        // Original: "PIP", "GCG", "PMP"; DURA (Duraluminium) kennt der Port nicht -> Titan.
        shaped(writer, "mixer", ModBlocks.MIXER.get(), 1,
                new String[]{"PIP", "GCG", "PMP"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'I', mat(ModMaterials.TITANIUM, MaterialShape.INGOT),
                'G', Blocks.GLASS_PANE,
                'C', ModItems.VACUUM_TUBE.get(),
                'M', ModItems.MOTOR.get());

        // Original: "LLL", "L#L", "LLL"
        shaped(writer, "machine_storage_drum", ModBlocks.MACHINE_STORAGE_DRUM.get(), 1,
                new String[]{"LLL", "L#L", "LLL"},
                'L', mat(ModMaterials.LEAD, MaterialShape.PLATE),
                '#', ModItems.TANK_STEEL.get());

        // Original: "LRL", "BRB", "LRL"
        shaped(writer, "machine_waste_drum", ModBlocks.MACHINE_WASTE_DRUM.get(), 1,
                new String[]{"LRL", "BRB", "LRL"},
                'L', mat(ModMaterials.LEAD, MaterialShape.INGOT),
                'B', Blocks.IRON_BARS,
                'R', ModItems.ROD_QUAD_EMPTY.get());

        // mass_storage (Stahl, Original-Meta 2): nur als Aufwertung aus Desh, siehe RestRecipeGenerator (container_upgrade).

        // Original: "SHS", "DHD", "SHS" (machine_solar_boiler)
        shaped(writer, "solar_boiler", ModBlocks.SOLAR_BOILER.get(), 1,
                new String[]{"SHS", "DHD", "SHS"},
                'S', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'H', ModItems.SHELL_STEEL.get(),
                'D', Items.BLACK_DYE);

        // Original: "AAA", " B ", "SSS" (solar_mirror, 3 Stueck)
        shaped(writer, "solar_mirror", ModBlocks.SOLAR_MIRROR.get(), 3,
                new String[]{"AAA", " B ", "SSS"},
                'A', mat(ModMaterials.ALUMINUM, MaterialShape.PLATE),
                'B', ModBlocks.STEEL_BEAM.get(),
                'S', mat(ModMaterials.STEEL, MaterialShape.INGOT));

        // Original: "SIS", "ICI", "SRS"
        shaped(writer, "machine_siren", ModBlocks.MACHINE_SIREN.get(), 1,
                new String[]{"SIS", "ICI", "SRS"},
                'S', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'I', mat(ModMaterials.RUBBER, MaterialShape.INGOT),
                'C', ModItems.VACUUM_TUBE.get(),
                'R', Items.REDSTONE);

        // Original: "PCP", "SRS", "PCP"
        shaped(writer, "radar_screen", ModBlocks.RADAR_SCREEN.get(), 1,
                new String[]{"PCP", "SRS", "PCP"},
                'P', polymer(MaterialShape.INGOT),
                'C', ModItems.ANALOG_CIRCUIT.get(),
                'S', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'R', ModItems.CRT_DISPLAY.get());

        // Original: "PCP", "WSW", "WSW"
        shaped(writer, "machine_keyforge", ModBlocks.MACHINE_KEYFORGE.get(), 1,
                new String[]{"PCP", "WSW", "WSW"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'S', mat(ModMaterials.TUNGSTEN, MaterialShape.INGOT),
                'C', ModItems.PADLOCK.get(),
                'W', Blocks.OAK_PLANKS);

        // Original: " W", "PCP", "PIP"
        shaped(writer, "radiorec", ModBlocks.RADIOREC.get(), 1,
                new String[]{" W ", "PCP", "PIP"},
                'W', mat(ModMaterials.COPPER, MaterialShape.WIRE),
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'C', ModItems.VACUUM_TUBE.get(),
                'I', polymer(MaterialShape.INGOT));
    }

    // ─── Redstone und Funk ────────────────────────────────────────────────────

    private static void redstoneRadio(Consumer<FinishedRecipe> writer) {
        // Original: alle vier Stueck, Glowstone + Redstonefackel + je nach Typ ein Bauteil.
        torch(writer, "radio_torch_sender", ModBlocks.RADIO_TORCH_SENDER.get(), Items.QUARTZ);
        torch(writer, "radio_torch_receiver", ModBlocks.RADIO_TORCH_RECEIVER.get(), Items.IRON_INGOT);
        torch(writer, "radio_torch_counter", ModBlocks.RADIO_TORCH_COUNTER.get(), ModItems.VACUUM_TUBE.get());
        torch(writer, "radio_torch_logic", ModBlocks.RADIO_TORCH_LOGIC.get(), ModItems.MICROCHIP.get());

        shaped(writer, "radio_torch_reader", ModBlocks.RADIO_TORCH_READER.get(), 4,
                new String[]{" G ", "IRI"},
                'G', Items.GLOWSTONE_DUST,
                'R', Blocks.REDSTONE_TORCH,
                'I', ModItems.VACUUM_TUBE.get());

        shaped(writer, "radio_torch_controller", ModBlocks.RADIO_TORCH_CONTROLLER.get(), 4,
                new String[]{" G ", "IRI"},
                'G', Items.GLOWSTONE_DUST,
                'R', Blocks.REDSTONE_TORCH,
                'I', ModItems.MICROCHIP.get());

        // Original: "TAR", "PAP", "PAP"
        shaped(writer, "radio_autocal", ModBlocks.RADIO_AUTOCAL.get(), 1,
                new String[]{"TAR", "PAP", "PAP"},
                'T', ModBlocks.RADIO_TORCH_SENDER.get(),
                'R', ModBlocks.RADIO_TORCH_RECEIVER.get(),
                'A', ModItems.ANALOG_CIRCUIT.get(),
                'P', mat(ModMaterials.COPPER, MaterialShape.PLATE_CAST));

        // Original: "SCR", "W#W", "WWW"
        shaped(writer, "radio_telex", ModBlocks.RADIO_TELEX.get(), 2,
                new String[]{"SCR", "W#W", "WWW"},
                'S', ModBlocks.RADIO_TORCH_SENDER.get(),
                'C', ModItems.CRT_DISPLAY.get(),
                'R', ModBlocks.RADIO_TORCH_RECEIVER.get(),
                'W', Blocks.OAK_PLANKS,
                '#', ModItems.ANALOG_CIRCUIT.get());

        // Original: "G", "T", "C" - Wegpunkte fuer die Drohnen.
        shaped(writer, "drone_waypoint", ModBlocks.DRONE_WAYPOINT.get(), 4,
                new String[]{"G", "T", "C"},
                'G', Items.GREEN_DYE,
                'T', Blocks.REDSTONE_TORCH,
                'C', ModItems.ANALOG_CIRCUIT.get());

        shaped(writer, "drone_waypoint_request", ModBlocks.DRONE_WAYPOINT_REQUEST.get(), 4,
                new String[]{"G", "T", "C"},
                'G', Items.BLUE_DYE,
                'T', Blocks.REDSTONE_TORCH,
                'C', ModItems.ANALOG_CIRCUIT.get());

        shaped(writer, "drone_crate", ModBlocks.DRONE_CRATE.get(), 1,
                new String[]{"T", "C"},
                'T', ModBlocks.DRONE_WAYPOINT.get(),
                'C', ModBlocks.CRATE_STEEL.get());
    }

    // ─── Zweite Fuhre: Logistik, Speicher, Restmaschinen ─────────────────────

    private static void logistics(Consumer<FinishedRecipe> writer) {
        // Oelleitung bewusst ohne Rezept: der Block ist im Port ein Rumpf. Es gibt weder ein
        // Item noch ein Modell oder einen Blockzustand dafuer - ein Rezept wuerde nur ein
        // unplatzierbares Etwas mit fehlender Textur liefern. Erst den Block fertigbauen.

        // Original: "GGG", "SPS" mit G = Stahlgitter, S = Stahlbarren, P = Hydraulikkolben.
        // Den Kolben kennt der Port nicht, deshalb der Motor.
        shaped(writer, "cargo_elevator", ModBlocks.CARGO_ELEVATOR.get(), 3,
                new String[]{"GGG", "SPS"},
                'G', ModBlocks.STEEL_GRATE.get(),
                'S', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'P', ModItems.MOTOR.get());

        // Original: Wegpunkt + Stahlkiste, die Farbe unterscheidet die Rolle.
        shaped(writer, "drone_crate_provider", ModBlocks.DRONE_CRATE_PROVIDER.get(), 1,
                new String[]{"T", "C", "B"},
                'T', ModBlocks.DRONE_WAYPOINT_REQUEST.get(),
                'C', ModBlocks.CRATE_STEEL.get(),
                'B', Items.ORANGE_DYE);

        shaped(writer, "drone_crate_requester", ModBlocks.DRONE_CRATE_REQUESTER.get(), 1,
                new String[]{"T", "C", "B"},
                'T', ModBlocks.DRONE_WAYPOINT_REQUEST.get(),
                'C', ModBlocks.CRATE_STEEL.get(),
                'B', Items.YELLOW_DYE);

        shaped(writer, "drone_dock", ModBlocks.DRONE_DOCK.get(), 1,
                new String[]{"T", "C", "B"},
                'T', ModBlocks.DRONE_WAYPOINT_REQUEST.get(),
                'C', ModBlocks.CRATE_STEEL.get(),
                'B', ModItems.MICROCHIP.get());

        // Speicheraufwertungen: Original ContainerUpgradeCraftingHandler, siehe RestRecipeGenerator (container_upgrade).

        capacitor(writer, "capacitor_niobium", ModBlocks.CAPACITOR_NIOBIUM.get(),
                mat(ModMaterials.NIOBIUM, MaterialShape.INGOT));
        capacitor(writer, "capacitor_schrabidate", ModBlocks.CAPACITOR_SCHRABIDATE.get(),
                mat(ModMaterials.SCHRABIDATE, MaterialShape.INGOT));

        // Der alte Hochofen aus Ziegeln - die Vorstufe des Maschinenhochofens.
        shaped(writer, "blast_furnace", ModBlocks.BLAST_FURNACE.get(), 1,
                new String[]{"BBB", "B B", "BIB"},
                'B', ModItems.FIREBRICK.get(),
                'I', mat(ModMaterials.IRON, MaterialShape.PLATE));

        shaped(writer, "pedestal", ModBlocks.PEDESTAL.get(), 2,
                new String[]{"S S", " S ", "SSS"},
                'S', Blocks.STONE_BRICKS);
    }

    // --- Dritte Fuhre: Kraene, Rohrpost, Restmaschinen -----------------------

    private static void cranesAndTubes(Consumer<FinishedRecipe> writer) {
        // Kraene 1:1 (CraftingManager): Gehaeuse Steinziegel/Eisen/Stahl ergibt 1/2/4 Stueck,
        // B = Foerderband-Stab, P = Druckluftkolben.
        net.minecraft.world.item.Item piston = pistonPneumatic();
        net.minecraft.world.item.Item wand = ModItems.CONVEYOR_WAND_REGULAR.get();
        Object[][] casings = {
                {Blocks.STONE_BRICKS, 1, "stonebrick"},
                {Items.IRON_INGOT, 2, "iron"},
                {mat(ModMaterials.STEEL, MaterialShape.INGOT), 4, "steel"}};
        for (Object[] c : casings) {
            ItemLike casing = (ItemLike) c[0];
            int amount = (Integer) c[1];
            shaped(writer, "crane_inserter_" + c[2], ModBlocks.CRANE_INSERTER.get(), amount,
                    new String[]{"CCC", "C C", "CBC"}, 'C', casing, 'B', wand);
            shaped(writer, "crane_extractor_" + c[2], ModBlocks.CRANE_EXTRACTOR.get(), amount,
                    new String[]{"CCC", "CPC", "CBC"}, 'C', casing, 'B', wand, 'P', piston);
            shaped(writer, "crane_grabber_" + c[2], ModBlocks.CRANE_GRABBER.get(), amount,
                    new String[]{"C C", "P P", "CBC"}, 'C', casing, 'B', wand, 'P', piston);
        }
        shaped(writer, "crane_boxer", ModBlocks.CRANE_BOXER.get(), 1,
                new String[]{"WWW", "WPW", "CCC"}, 'W', net.minecraft.tags.ItemTags.PLANKS, 'P', piston, 'C', wand);
        shaped(writer, "crane_unboxer", ModBlocks.CRANE_UNBOXER.get(), 1,
                new String[]{"WWW", "WPW", "CCC"}, 'W', Items.STICK, 'P', Items.SHEARS, 'C', wand);
        shaped(writer, "crane_router", ModBlocks.CRANE_ROUTER.get(), 1,
                new String[]{"PIP", "ICI", "PIP"}, 'P', piston, 'I', polymer(MaterialShape.PLATE), 'C', ModItems.INTEGRATED_CIRCUIT.get());
        shaped(writer, "crane_splitter", ModBlocks.CRANE_SPLITTER.get(), 1,
                new String[]{"III", "PCP", "III"}, 'P', piston, 'I', mat(ModMaterials.STEEL, MaterialShape.INGOT), 'C', ModItems.VACUUM_TUBE.get());
        shaped(writer, "conveyor_press", ModBlocks.CONVEYOR_PRESS.get(), 1,
                new String[]{"CPC", "CBC", "CCC"}, 'C', mat(ModMaterials.COPPER, MaterialShape.PLATE), 'P', ModBlocks.EPRESS.get(), 'B', wand);

        // Foerderband-Staebe 1:1 (CraftingManager). EXPRESS braucht 1000 mB Schmiermittel im Behaelter
        // (Fluids.LUBRICANT.getDict) und folgt mit den Fluessigkeitsbehaeltern (R5).
        shaped(writer, "conveyor_wand_leather", wand, 16,
                new String[]{"LLL", "I I", "LLL"}, 'L', Items.LEATHER, 'I', Items.IRON_INGOT);
        shaped(writer, "conveyor_wand_rope", wand, 16,
                new String[]{"RSR", "I I", "RSR"}, 'I', Items.IRON_INGOT, 'R', com.hbm_m.item.PartTabMetaItems.itemOrNull("plant_item_rope"), 'S', mat(ModMaterials.IRON, MaterialShape.PLATE));
        shaped(writer, "conveyor_wand_rubber", wand, 64,
                new String[]{"LLL", "I I", "LLL"}, 'L', com.hbm_m.item.tags_and_tiers.ModTags.Items.RUBBER_BAR, 'I', Items.IRON_INGOT);
        shaped(writer, "conveyor_wand_double", ModItems.CONVEYOR_WAND_DOUBLE.get(), 1,
                new String[]{"CPC"}, 'C', wand, 'P', mat(ModMaterials.IRON, MaterialShape.PLATE));
        shaped(writer, "conveyor_wand_triple", ModItems.CONVEYOR_WAND_TRIPLE.get(), 1,
                new String[]{"DPC"}, 'C', wand, 'D', ModItems.CONVEYOR_WAND_DOUBLE.get(), 'P', mat(ModMaterials.STEEL, MaterialShape.PLATE));

        // Rohrpost: im Original gibt es dafuer kein Werkbankrezept mehr, die Teile stammen aus
        // der Baugruppe. Hier eine durchgaengige Reihe auf Stahl, Polymer und Schaltkreis -
        // in Aufwand vergleichbar mit den Kraenen.
        pneumatic(writer, "pneumatic_storage_mono", ModBlocks.PNEUMATIC_STORAGE_MONO.get(),
                ModItems.VACUUM_TUBE.get());
        pneumatic(writer, "pneumatic_storage_access", ModBlocks.PNEUMATIC_STORAGE_ACCESS.get(),
                ModItems.ANALOG_CIRCUIT.get());
        pneumatic(writer, "pneumatic_storage_importer", ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get(),
                ModItems.MOTOR.get());
        pneumatic(writer, "pneumatic_storage_exporter", ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get(),
                ModItems.PISTON_SET_STEEL.get());
        pneumatic(writer, "pneumatic_storage_clutter", ModBlocks.PNEUMATIC_STORAGE_CLUTTER.get(),
                ModBlocks.CRATE_STEEL.get());
    }

    private static net.minecraft.world.item.Item pistonPneumatic() {
        return com.hbm_m.item.PartTabMetaItems.itemOrNull("part_generic_piston_pneumatic");
    }

    private static void crane(Consumer<FinishedRecipe> writer, String name, ItemLike out, String[] pattern) {
        shaped(writer, name, out, 1, pattern,
                'C', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'I', mat(ModMaterials.STEEL, MaterialShape.INGOT),
                'P', ModItems.PISTON_SET_STEEL.get(),
                'W', Blocks.OAK_PLANKS,
                'S', Items.SHEARS);
    }

    private static void pneumatic(Consumer<FinishedRecipe> writer, String name, ItemLike out, ItemLike core) {
        shaped(writer, name, out, 1,
                new String[]{"SPS", "PCP", "SPS"},
                'S', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'P', polymer(MaterialShape.PLATE),
                'C', core);
    }

    private static void oddsAndEnds(Consumer<FinishedRecipe> writer) {
        // Original: "IPI", "I I", "IPI" - die Faesser unterscheiden sich nur im Material.
        barrel(writer, "barrel_tcalloy", ModBlocks.BARREL_TCALLOY.get(),
                mat(ModMaterials.TCALLOY, MaterialShape.INGOT),
                mat(ModMaterials.TITANIUM, MaterialShape.PLATE));
        barrel(writer, "barrel_antimatter", ModBlocks.BARREL_ANTIMATTER.get(),
                mat(ModMaterials.STARMETAL, MaterialShape.INGOT),
                mat(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE));
        // Das rostige Fass ist im Original Ruinenfund; hier aus einem Stahlfass und Rost.
        shapeless(writer, "barrel_corroded", ModBlocks.BARREL_CORRODED.get(), 1,
                ModBlocks.BARREL_STEEL.get(), Items.COPPER_INGOT);

        // Original: "PSP", "SCS", "PSP" mit P = widerstandsfaehige Legierung, S = Kunststoff
        shaped(writer, "machine_satlinker", ModBlocks.MACHINE_SATLINKER.get(), 1,
                new String[]{"PSP", "SCS", "PSP"},
                'P', mat(ModMaterials.TITANIUM, MaterialShape.INGOT),
                'S', polymer(MaterialShape.INGOT),
                'C', ModItems.SAT_CHIP.get());

        // Original: "PLP", "PSP", "PLP" mit S = Sternmetallring, L = Duraluminiumplatte (-> Titan)
        shaped(writer, "radiobox", ModBlocks.RADIOBOX.get(), 1,
                new String[]{"PLP", "PSP", "PLP"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'S', ModItems.RING_STARMETAL.get(),
                'L', mat(ModMaterials.TITANIUM, MaterialShape.PLATE));

        // Rundfunksender: im Original ohne Rezept. Aufgebaut wie der Empfaenger, eine Stufe teurer.
        shaped(writer, "broadcaster_pc", ModBlocks.BROADCASTER_PC.get(), 1,
                new String[]{"PCP", "PDP", "PIP"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'C', ModItems.CRT_DISPLAY.get(),
                'D', ModItems.INTEGRATED_CIRCUIT.get(),
                'I', polymer(MaterialShape.INGOT));

        // Seemine: Original ass.minenaval (AssemblerRecipeGenerator), kein Werkbankrezept.

        // Batterieblock und Ofenblock: im Original Baugruppen, hier als Werkbankstufe.
        shaped(writer, "bat9000", ModBlocks.BAT9000.get(), 1,
                new String[]{"PCP", "CBC", "PCP"},
                'P', mat(ModMaterials.STEEL, MaterialShape.PLATE),
                'C', ModItems.CAPACITOR_BOARD.get(),
                'B', ModItems.BATTERY_LITHIUM.get());

        // orbus: 1:1 ass.orbus im AssemblerRecipeGenerator (kein Werkbankrezept im Original)
    }

    private static void barrel(Consumer<FinishedRecipe> writer, String name, ItemLike out,
                               ItemLike wall, ItemLike band) {
        shaped(writer, name, out, 1,
                new String[]{"IPI", "I I", "IPI"},
                'I', wall,
                'P', band);
    }

    private static void torch(Consumer<FinishedRecipe> writer, String name, ItemLike out, ItemLike part) {
        shaped(writer, name, out, 4,
                new String[]{"G", "R", "I"},
                'G', Items.GLOWSTONE_DUST,
                'R', Blocks.REDSTONE_TORCH,
                'I', part);
    }

    // ─── Hilfen ───────────────────────────────────────────────────────────────

    private static ItemLike mat(ModMaterials material, MaterialShape shape) {
        return ModMaterialItems.item(material, shape);
    }

    private static ItemLike polymer(MaterialShape shape) {
        return ModMaterialItems.item(ModMaterials.POLYMER, shape);
    }

    private static void shaped(Consumer<FinishedRecipe> writer, String name, ItemLike out, int count,
                               String[] pattern, Object... keys) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, out, count);
        for (String row : pattern) {
            builder.pattern(row);
        }
        String all = String.join("", pattern);
        ItemLike first = null;
        for (int i = 0; i + 1 < keys.length; i += 2) {
            char symbol = (Character) keys[i];
            // Nur Zutaten anmelden, deren Zeichen im Muster vorkommt - sonst lehnt der
            // Rezeptbauer das Rezept ab ("Ingredients are defined but not used in pattern").
            if (all.indexOf(symbol) < 0) {
                continue;
            }
            Object key = keys[i + 1];
            if (key instanceof net.minecraft.tags.TagKey<?> tag) {
                @SuppressWarnings("unchecked")
                net.minecraft.tags.TagKey<net.minecraft.world.item.Item> itemTag = (net.minecraft.tags.TagKey<net.minecraft.world.item.Item>) tag;
                builder.define(symbol, itemTag);
                continue;
            }
            if (key instanceof net.minecraft.world.item.crafting.Ingredient ing) {
                builder.define(symbol, ing);
                continue;
            }
            ItemLike ingredient = (ItemLike) key;
            builder.define(symbol, ingredient);
            if (first == null) {
                first = ingredient;
            }
        }
        builder.unlockedBy("has_ingredient", InventoryChangeTrigger.TriggerInstance.hasItems(first));
        builder.save(writer, id(name));
    }

    private static void shapeless(Consumer<FinishedRecipe> writer, String name, ItemLike out, int count,
                                  ItemLike... ingredients) {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, out, count);
        for (ItemLike ingredient : ingredients) {
            builder.requires(ingredient);
        }
        builder.unlockedBy("has_ingredient", InventoryChangeTrigger.TriggerInstance.hasItems(ingredients[0]));
        builder.save(writer, id(name));
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "crafting/machine/" + name);
    }
}
//?}
