package com.hbm_m.main;

import java.util.Map;

/** Gemeinsame Tabelle alter Port-IDs (Forge: MissingMappingsEvent, NeoForge: Registry-Alias). */
public final class LegacyIds {

    /**
     * Umbenannte Port-IDs (alt -> neu), damit bestehende Welten die Bloecke/Gegenstaende behalten.
     * barrel_yellow: doppelte Fass-Variante, jetzt unter der Original-ID yellow_barrel.
     * barrel_vitrified: verglastes Fass, jetzt unter der Original-ID vitrified_barrel.
     * metal_door / deco_steel_scaffold: fruehere Port-IDs von door_metal / steel_scaffold.
     * insulator: doppelte Port-ID von plate_polymer (Original ModItems.plate_polymer "Insulator").
     */
    public static final Map<String, String> MAP = Map.ofEntries(
            Map.entry("barrel_yellow", "yellow_barrel"),
            Map.entry("barrel_vitrified", "vitrified_barrel"),
            Map.entry("metal_door", "door_metal"),
            Map.entry("deco_steel_scaffold", "steel_scaffold"),
            Map.entry("insulator", "plate_polymer"),
            // naval_mine: doppelte Port-ID der Original-Mine mine_naval.
            Map.entry("naval_mine", "mine_naval"),
            // key_pin: nur die Grundklasse ItemKeyPin, im Original nie als eigenes Item registriert.
            Map.entry("key_pin", "key"),
            // fensu: Port-Platzhalter "Industrial Fan (WIP)" mit FENSU-Modell, echte FENSU = machine_fensu.
            Map.entry("fensu", "machine_fensu"),
            // turbine: WIP-Duplikat der grossen Dampfturbine (Original machine_large_turbine), Block + Item entfernt.
            Map.entry("turbine", "machine_large_turbine"),
            // Doppelte Port-IDs -> Original-ID (Original: ingot_aluminium, plate_aluminium, block_aluminium, ore_aluminium,
            // ingot_th232, powder_ash@WOOD, part_generic@LDE, missile_anti_ballistic).
            Map.entry("aluminum_ingot", "ingot_aluminium"),
            Map.entry("plate_aluminum", "plate_aluminium"),
            Map.entry("block_aluminum", "block_aluminium"),
            Map.entry("aluminum_ore", "ore_aluminium"),
            Map.entry("thorium_ingot", "th232_ingot"),
            Map.entry("wood_ash_powder", "ash_wood"),
            Map.entry("low_density_element", "part_generic_lde"),
            Map.entry("missile_abm", "missile_anti_ballistic"),
            // cinnabar: doppelte Port-ID, Original ModItems.cinnebar (Schreibweise "cinnebar")
            Map.entry("cinnabar", "cinnebar"),
            // Port-Bloecke concrete_colored_<typ>/concrete_hazard = Original concrete_colored_ext@<TYP>
            Map.entry("concrete_colored_bronze", "concrete_colored_ext_bronze"),
            Map.entry("concrete_colored_indigo", "concrete_colored_ext_indigo"),
            Map.entry("concrete_colored_machine", "concrete_colored_ext_machine"),
            Map.entry("concrete_colored_machine_stripe", "concrete_colored_ext_machine_stripe"),
            Map.entry("concrete_colored_pink", "concrete_colored_ext_pink"),
            Map.entry("concrete_colored_purple", "concrete_colored_ext_purple"),
            Map.entry("concrete_colored_sand", "concrete_colored_ext_sand"),
            Map.entry("concrete_hazard", "concrete_colored_ext_hazard"));

    private LegacyIds() {}
}
