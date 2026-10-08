package com.hbm_m.item.missile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.missile.ItemCustomMissilePart.FuelType;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartType;
import com.hbm_m.item.missile.ItemCustomMissilePart.Rarity;
import com.hbm_m.item.missile.ItemCustomMissilePart.WarheadType;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;

/**
 * Generiert aus den mp_*-Eintraegen von {@code ModItems} (1.7.10) und {@code MissilePart.registerAllParts}: alle
 * Baukasten-Raketenteile (Kopien wie Tarnlackierungen eigene Gegenstaende) samt Modell/Textur/Hoehe fuer den Renderer.
 * Nicht von Hand pflegen - Quelle ist das Original.
 */
public final class MissilePartItems {

    private MissilePartItems() { }

    /** {@code MissilePart}: Hoehe im Verbund, Hoehe fuer die GUI-Vorschau, OBJ-Modell und Textur (Pfade unter hbm_m). */
    public record RenderInfo(PartType type, double height, double guiHeight, String model, String texture) { }

    private static final Map<String, RegistrySupplier<Item>> ITEMS = new LinkedHashMap<>();
    /** Symbol (Itemtextur) je Teil. */
    public static final Map<String, String> ICONS = new LinkedHashMap<>();
    /** {@code setCreativeTab(null)} des Originals. */
    public static final List<String> HIDDEN = new ArrayList<>();
    public static final Map<String, RenderInfo> RENDER = new HashMap<>();

    private static void reg(String id, String icon, boolean hidden, Supplier<Item> factory) {
        if (ITEMS.containsKey(id)) return;
        ITEMS.put(id, ModItems.ITEMS.register(id, factory));
        ICONS.put(id, icon);
        if (hidden) HIDDEN.add(id);
    }

    public static void registerAll() {
        reg("mp_thruster_10_kerosene", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_10, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 1.5F }, 10F, null, null, null, null));
        reg("mp_thruster_10_solid", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_10, PartSize.NONE, new Object[] { FuelType.SOLID, 1F, 1.5F }, 15F, null, null, null, null));
        reg("mp_thruster_10_xenon", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_10, PartSize.NONE, new Object[] { FuelType.XENON, 1F, 1.5F }, 5F, null, null, null, null));
        reg("mp_thruster_15_kerosene", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 7.5F }, 15F, null, null, null, null));
        reg("mp_thruster_15_kerosene_dual", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 2.5F }, 15F, null, null, null, null));
        reg("mp_thruster_15_kerosene_triple", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 5F }, 15F, null, null, null, null));
        reg("mp_thruster_15_solid", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.SOLID, 1F, 5F }, 20F, null, null, null, null));
        reg("mp_thruster_15_solid_hexdecuple", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.SOLID, 1F, 5F }, 25F, Rarity.UNCOMMON, null, null, null));
        reg("mp_thruster_15_hydrogen", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.HYDROGEN, 1F, 7.5F }, 20F, null, null, null, null));
        reg("mp_thruster_15_hydrogen_dual", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.HYDROGEN, 1F, 2.5F }, 15F, null, null, null, null));
        reg("mp_thruster_15_balefire_short", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.BALEFIRE, 1F, 5F }, 25F, null, null, null, null));
        reg("mp_thruster_15_balefire", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.BALEFIRE, 1F, 5F }, 25F, null, null, null, null));
        reg("mp_thruster_15_balefire_large", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.BALEFIRE, 1F, 7.5F }, 35F, null, null, null, null));
        reg("mp_thruster_15_balefire_large_rad", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_15, PartSize.NONE, new Object[] { FuelType.BALEFIRE, 1F, 7.5F }, 35F, Rarity.UNCOMMON, null, "The Master", null));
        reg("mp_thruster_20_kerosene", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_20, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 100F }, 30F, null, null, null, null));
        reg("mp_thruster_20_kerosene_dual", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_20, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 100F }, 30F, null, null, null, null));
        reg("mp_thruster_20_kerosene_triple", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_20, PartSize.NONE, new Object[] { FuelType.KEROSENE, 1F, 100F }, 30F, null, null, null, null));
        reg("mp_thruster_20_solid", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_20, PartSize.NONE, new Object[] { FuelType.SOLID, 1F, 100F }, 35F, null, null, null, "It's basically just a big hole at the end of the fuel tank."));
        reg("mp_thruster_20_solid_multi", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_20, PartSize.NONE, new Object[] { FuelType.SOLID, 1F, 100F }, 35F, null, null, null, null));
        reg("mp_thruster_20_solid_multier", "mp_thruster", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.THRUSTER, PartSize.SIZE_20, PartSize.NONE, new Object[] { FuelType.SOLID, 1F, 100F }, 35F, null, null, null, "Did I miscount? Hope not."));
        reg("mp_stability_10_flat", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { 0.5F }, 10F, null, null, null, null));
        reg("mp_stability_10_cruise", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { 0.25F }, 5F, null, null, null, null));
        reg("mp_stability_10_space", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { 0.35F }, 5F, Rarity.COMMON, null, null, "Standing there alone, the ship is waiting / All systems are go, are you sure?"));
        reg("mp_stability_15_flat", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { 0.5F }, 10F, null, null, null, null));
        reg("mp_stability_15_thin", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { 0.35F }, 5F, null, null, null, null));
        reg("mp_stability_15_soyuz", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { 0.25F }, 15F, Rarity.COMMON, null, null, "Союз!"));
        reg("mp_s_20", "mp_stability", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FINS, PartSize.SIZE_20, PartSize.SIZE_20, new Object[] { 0.5F }, 0F, null, null, null, null));
        reg("mp_fuselage_10_kerosene", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 20F, null, null, "Hoboy", null));
        reg("mp_fuselage_10_kerosene_camo", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 20F, Rarity.COMMON, "Camo", null, null));
        reg("mp_fuselage_10_kerosene_desert", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 20F, Rarity.COMMON, "Desert Camo", null, null));
        reg("mp_fuselage_10_kerosene_sky", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 20F, Rarity.COMMON, "Sky Camo", null, null));
        reg("mp_fuselage_10_kerosene_flames", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 20F, Rarity.UNCOMMON, "Sick Flames", null, null));
        reg("mp_fuselage_10_kerosene_insulation", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 25F, Rarity.COMMON, "Orange Insulation", null, null));
        reg("mp_fuselage_10_kerosene_sleek", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 35F, Rarity.RARE, "IF-R&D", null, null));
        reg("mp_fuselage_10_kerosene_metal", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 30F, Rarity.UNCOMMON, "Bolted Metal", "Hoboy", null));
        reg("mp_fuselage_10_kerosene_taint", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 2500F }, 20F, Rarity.UNCOMMON, "Tainted", "Sam", null));
        reg("mp_fuselage_10_solid", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 25F, null, null, null, null));
        reg("mp_fuselage_10_solid_flames", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 25F, Rarity.UNCOMMON, "Sick Flames", null, null));
        reg("mp_fuselage_10_solid_insulation", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 30F, Rarity.COMMON, "Orange Insulation", null, null));
        reg("mp_fuselage_10_solid_sleek", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 35F, Rarity.RARE, "IF-R&D", null, null));
        reg("mp_fuselage_10_solid_soviet_glory", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 35F, Rarity.EPIC, "Soviet Glory", "Hoboy", null));
        reg("mp_fuselage_10_solid_cathedral", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 25F, Rarity.RARE, "Unholy Cathedral", "Satan", "Quakeesque!"));
        reg("mp_fuselage_10_solid_moonlit", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 25F, Rarity.UNCOMMON, "Moonlit", "The Master & Hoboy", null));
        reg("mp_fuselage_10_solid_battery", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 30F, Rarity.UNCOMMON, "Ecstatic", "wolfmonster222", "I got caught eating batteries again :("));
        reg("mp_fuselage_10_solid_duracell", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 2500F }, 30F, Rarity.RARE, "Duracell", "Hoboy", "The crunchiest battery on the market!"));
        reg("mp_fuselage_10_xenon", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.XENON, 5000F }, 20F, null, null, null, null));
        reg("mp_fuselage_10_xenon_bhole", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.XENON, 5000F }, 20F, Rarity.RARE, "Morceus-1457", "Sten89", null));
        reg("mp_fuselage_10_long_kerosene", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, null, null, "Hoboy", null));
        reg("mp_fuselage_10_long_kerosene_camo", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.COMMON, "Camo", null, null));
        reg("mp_fuselage_10_long_kerosene_desert", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.COMMON, "Desert Camo", null, null));
        reg("mp_fuselage_10_long_kerosene_sky", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.COMMON, "Sky Camo", null, null));
        reg("mp_fuselage_10_long_kerosene_flames", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.UNCOMMON, "Sick Flames", null, null));
        reg("mp_fuselage_10_long_kerosene_insulation", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 35F, Rarity.COMMON, "Orange Insulation", null, null));
        reg("mp_fuselage_10_long_kerosene_sleek", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 40F, Rarity.RARE, "IF-R&D", null, null));
        reg("mp_fuselage_10_long_kerosene_metal", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 35F, Rarity.UNCOMMON, null, "Hoboy", null));
        reg("mp_fuselage_10_long_kerosene_dash", "mp_fuselage", true, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.EPIC, "Dash", "Sam", "I wash my hands of it."));
        reg("mp_fuselage_10_long_kerosene_taint", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.UNCOMMON, "Tainted", "Sam", null));
        reg("mp_fuselage_10_long_kerosene_vap", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.KEROSENE, 5000F }, 30F, Rarity.EPIC, "Minty Contrail", "VT-6/24", "Upper rivet!"));
        reg("mp_fuselage_10_long_solid", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 35F, null, null, null, null));
        reg("mp_fuselage_10_long_solid_flames", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 35F, Rarity.UNCOMMON, "Sick Flames", null, null));
        reg("mp_fuselage_10_long_solid_insulation", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 40F, Rarity.COMMON, "Orange Insulation", null, null));
        reg("mp_fuselage_10_long_solid_sleek", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 45F, Rarity.RARE, "IF-R&D", null, null));
        reg("mp_fuselage_10_long_solid_soviet_glory", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 45F, Rarity.EPIC, "Soviet Glory", "Hoboy", "Fully Automated Luxury Gay Space Communism!"));
        reg("mp_fuselage_10_long_solid_bullet", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 35F, Rarity.COMMON, "Bullet Bill", "Sam", null));
        reg("mp_fuselage_10_long_solid_silvermoonlight", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_10, new Object[] { FuelType.SOLID, 5000F }, 35F, Rarity.UNCOMMON, "Silver Moonlight", "The Master", null));
        reg("mp_fuselage_10_15_kerosene", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 10000F }, 40F, null, null, null, null));
        reg("mp_fuselage_10_15_solid", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 10000F }, 40F, null, null, null, null));
        reg("mp_fuselage_10_15_hydrogen", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_15, new Object[] { FuelType.HYDROGEN, 10000F }, 40F, null, null, null, null));
        reg("mp_fuselage_10_15_balefire", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_10, PartSize.SIZE_15, new Object[] { FuelType.BALEFIRE, 10000F }, 40F, null, null, null, null));
        reg("mp_fuselage_15_kerosene", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, null, null, "Hoboy", null));
        reg("mp_fuselage_15_kerosene_camo", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, Rarity.COMMON, "Camo", null, null));
        reg("mp_fuselage_15_kerosene_desert", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, Rarity.COMMON, "Desert Camo", null, null));
        reg("mp_fuselage_15_kerosene_sky", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, Rarity.COMMON, "Sky Camo", null, null));
        reg("mp_fuselage_15_kerosene_insulation", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 55F, Rarity.COMMON, "Orange Insulation", null, "Rest in spaghetti Columbia :("));
        reg("mp_fuselage_15_kerosene_metal", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 60F, Rarity.UNCOMMON, "Bolted Metal", "Hoboy", "Metal frame with metal plating reinforced with bolted metal sheets and metal."));
        reg("mp_fuselage_15_kerosene_decorated", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 60F, Rarity.UNCOMMON, "Decorated", "Hoboy", null));
        reg("mp_fuselage_15_kerosene_steampunk", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 60F, Rarity.RARE, "Steampunk", "Hoboy", null));
        reg("mp_fuselage_15_kerosene_polite", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 60F, Rarity.LEGENDARY, "Polite", "Hoboy", null));
        reg("mp_fuselage_15_kerosene_blackjack", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 100F, Rarity.LEGENDARY, "Queen Whiskey", null, null));
        reg("mp_fuselage_15_kerosene_lambda", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 75F, Rarity.RARE, "Lambda Complex", "VT-6/24", "MAGNIFICENT MICROWAVE CASSEROLE"));
        reg("mp_fuselage_15_kerosene_minuteman", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, Rarity.UNCOMMON, "MX 1702", "Spexta", null));
        reg("mp_fuselage_15_kerosene_pip", "mp_fuselage", true, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, Rarity.EPIC, "LittlePip", "The Doctor", "31!"));
        reg("mp_fuselage_15_kerosene_taint", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 50F, Rarity.UNCOMMON, "Tainted", "Sam", "DUN-DUN!"));
        reg("mp_fuselage_15_kerosene_yuck", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.KEROSENE, 15000F }, 60F, Rarity.EPIC, "Flesh", "Hoboy", "Note: Never clean DNA vials with your own spit."));
        reg("mp_fuselage_15_solid", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 60F, null, null, null, null));
        reg("mp_fuselage_15_solid_insulation", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 65F, Rarity.COMMON, "Orange Insulation", null, null));
        reg("mp_fuselage_15_solid_desh", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 80F, Rarity.RARE, "Desh Plating", "Hoboy", null));
        reg("mp_fuselage_15_solid_soviet_glory", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 70F, Rarity.RARE, "Soviet Glory", "Hoboy", null));
        reg("mp_fuselage_15_solid_soviet_stank", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 15F, Rarity.EPIC, "Soviet Stank", "Hoboy", "Aged like a fine wine! Well, almost."));
        reg("mp_fuselage_15_solid_faust", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 250F, Rarity.LEGENDARY, "Mighty Lauren", "Dr.Nostalgia", "Welcome to Subway, may I take your order?"));
        reg("mp_fuselage_15_solid_silvermoonlight", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 60F, Rarity.UNCOMMON, "Silver Moonlight", "The Master", null));
        reg("mp_fuselage_15_solid_snowy", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 60F, Rarity.UNCOMMON, "Chilly Day", "Dr.Nostalgia", null));
        reg("mp_fuselage_15_solid_panorama", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 60F, Rarity.RARE, "Panorama", "Hoboy", null));
        reg("mp_fuselage_15_solid_roses", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 60F, Rarity.UNCOMMON, "Bed of roses", "Hoboy", null));
        reg("mp_fuselage_15_solid_mimi", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.SOLID, 15000F }, 60F, Rarity.RARE, "Mimi-chan", null, null));
        reg("mp_fuselage_15_hydrogen", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.HYDROGEN, 15000F }, 50F, null, null, null, null));
        reg("mp_fuselage_15_hydrogen_cathedral", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.HYDROGEN, 15000F }, 50F, Rarity.UNCOMMON, "Unholy Cathedral", "Satan", null));
        reg("mp_fuselage_15_balefire", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_15, new Object[] { FuelType.BALEFIRE, 15000F }, 75F, null, null, null, null));
        reg("mp_fuselage_15_20_kerosene", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_20, new Object[] { FuelType.KEROSENE, 20000F }, 70F, null, null, "Hoboy", null));
        reg("mp_fuselage_15_20_kerosene_magnusson", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_20, new Object[] { FuelType.KEROSENE, 20000F }, 70F, Rarity.RARE, "White Forest Rocket", "VT-6/24", "And get your cranio-conjugal parasite away from my nose cone!"));
        reg("mp_fuselage_15_20_solid", "mp_fuselage", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.FUSELAGE, PartSize.SIZE_15, PartSize.SIZE_20, new Object[] { FuelType.SOLID, 20000F }, 70F, null, null, null, null));
        reg("mp_warhead_10_he", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.HE, 15F, 1.5F }, 5F, null, null, null, null));
        reg("mp_warhead_10_incendiary", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.INC, 15F, 1.5F }, 5F, null, null, null, null));
        reg("mp_warhead_10_buster", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.BUSTER, 5F, 1.5F }, 5F, null, null, null, null));
        reg("mp_warhead_10_nuclear", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.NUCLEAR, 35F, 1.5F }, 10F, null, "Tater Tot", null, null));
        reg("mp_warhead_10_nuclear_large", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.NUCLEAR, 75F, 2.5F }, 15F, null, "Chernobyl Boris", null, null));
        reg("mp_warhead_10_taint", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.TAINT, 15F, 1.5F }, 20F, Rarity.UNCOMMON, null, null, "Eat my taint! Bureaucracy is dead and we killed it!"));
        reg("mp_warhead_10_cloud", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_10, new Object[] { WarheadType.CLOUD, 15F, 1.5F }, 20F, Rarity.RARE, null, null, null));
        reg("mp_warhead_15_he", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.HE, 50F, 2.5F }, 10F, null, null, null, null));
        reg("mp_warhead_15_incendiary", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.INC, 35F, 2.5F }, 10F, null, null, null, null));
        reg("mp_warhead_15_nuclear", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.NUCLEAR, 125F, 5F }, 15F, null, "Auntie Bertha", null, null));
        reg("mp_warhead_15_nuclear_shark", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.NUCLEAR, 125F, 5F }, 15F, Rarity.UNCOMMON, "Discount Bullet Bill", null, "Nose art on a cannon bullet? Who does that?"));
        reg("mp_warhead_15_nuclear_mimi", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.NUCLEAR, 125F, 5F }, 15F, Rarity.RARE, "FASHIONABLE MISSILE", null, null));
        reg("mp_warhead_15_boxcar", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.TX, 250F, 7.5F }, 35F, Rarity.LEGENDARY, null, null, "?!?!"));
        reg("mp_warhead_15_n2", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.N2, 100F, 5F }, 20F, Rarity.RARE, null, null, "[screams geometrically]"));
        reg("mp_warhead_15_balefire", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.BALEFIRE, 100F, 7.5F }, 15F, Rarity.LEGENDARY, null, "VT-6/24", "Hightower, never forgetti."));
        reg("mp_warhead_15_turbine", "mp_warhead", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.WARHEAD, PartSize.NONE, PartSize.SIZE_15, new Object[] { WarheadType.TURBINE, 200F, 5F }, 250F, Rarity.SEWS_CLOTHES_AND_SUCKS_HORSE_COCK, null, null, null));
        reg("mp_c_1", "mp_c_1", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.CHIP, PartSize.ANY, PartSize.ANY, new Object[] { 0.1F }, 0F, null, null, null, null));
        reg("mp_c_2", "mp_c_2", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.CHIP, PartSize.ANY, PartSize.ANY, new Object[] { 0.05F }, 0F, null, null, null, null));
        reg("mp_c_3", "mp_c_3", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.CHIP, PartSize.ANY, PartSize.ANY, new Object[] { 0.01F }, 0F, null, null, null, null));
        reg("mp_c_4", "mp_c_4", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.CHIP, PartSize.ANY, PartSize.ANY, new Object[] { 0.005F }, 0F, null, null, null, null));
        reg("mp_c_5", "mp_c_5", false, () -> new ItemCustomMissilePart(new Item.Properties(), PartType.CHIP, PartSize.ANY, PartSize.ANY, new Object[] { 0.0F }, 0F, null, null, null, null));
    }

    static {
        RENDER.put("mp_thruster_10_kerosene", new RenderInfo(PartType.THRUSTER, 1, 1, "models/missile_parts/mp_t_10_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_10_kerosene.png"));
        RENDER.put("mp_thruster_10_solid", new RenderInfo(PartType.THRUSTER, 0.5, 1, "models/missile_parts/mp_t_10_solid.obj", "textures/models/missile_parts/thrusters/mp_t_10_solid.png"));
        RENDER.put("mp_thruster_10_xenon", new RenderInfo(PartType.THRUSTER, 0.5, 1, "models/missile_parts/mp_t_10_xenon.obj", "textures/models/missile_parts/thrusters/mp_t_10_xenon.png"));
        RENDER.put("mp_thruster_15_kerosene", new RenderInfo(PartType.THRUSTER, 1.5, 1.5, "models/missile_parts/mp_t_15_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_15_kerosene.png"));
        RENDER.put("mp_thruster_15_kerosene_dual", new RenderInfo(PartType.THRUSTER, 1, 1.5, "models/missile_parts/mp_t_15_kerosene_dual.obj", "textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png"));
        RENDER.put("mp_thruster_15_kerosene_triple", new RenderInfo(PartType.THRUSTER, 1, 1.5, "models/missile_parts/mp_t_15_kerosene_triple.obj", "textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png"));
        RENDER.put("mp_thruster_15_solid", new RenderInfo(PartType.THRUSTER, 0.5, 1, "models/missile_parts/mp_t_15_solid.obj", "textures/models/missile_parts/thrusters/mp_t_15_solid.png"));
        RENDER.put("mp_thruster_15_solid_hexdecuple", new RenderInfo(PartType.THRUSTER, 0.5, 1, "models/missile_parts/mp_t_15_solid_hexdecuple.obj", "textures/models/missile_parts/thrusters/mp_t_15_solid_hexdecuple.png"));
        RENDER.put("mp_thruster_15_hydrogen", new RenderInfo(PartType.THRUSTER, 1.5, 1.5, "models/missile_parts/mp_t_15_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_15_hydrogen.png"));
        RENDER.put("mp_thruster_15_hydrogen_dual", new RenderInfo(PartType.THRUSTER, 1, 1.5, "models/missile_parts/mp_t_15_kerosene_dual.obj", "textures/models/missile_parts/thrusters/mp_t_15_hydrogen_dual.png"));
        RENDER.put("mp_thruster_15_balefire_short", new RenderInfo(PartType.THRUSTER, 2, 2, "models/missile_parts/mp_t_15_balefire_short.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire_short.png"));
        RENDER.put("mp_thruster_15_balefire", new RenderInfo(PartType.THRUSTER, 3, 2.5, "models/missile_parts/mp_t_15_balefire.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire.png"));
        RENDER.put("mp_thruster_15_balefire_large", new RenderInfo(PartType.THRUSTER, 3, 2.5, "models/missile_parts/mp_t_15_balefire_large.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire_large.png"));
        RENDER.put("mp_thruster_15_balefire_large_rad", new RenderInfo(PartType.THRUSTER, 3, 2.5, "models/missile_parts/mp_t_15_balefire_large.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire_large_rad.png"));
        RENDER.put("mp_thruster_20_kerosene", new RenderInfo(PartType.THRUSTER, 3, 2.5, "models/missile_parts/mp_t_20_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_20_kerosene.png"));
        RENDER.put("mp_thruster_20_kerosene_dual", new RenderInfo(PartType.THRUSTER, 2, 2, "models/missile_parts/mp_t_20_kerosene_dual.obj", "textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png"));
        RENDER.put("mp_thruster_20_kerosene_triple", new RenderInfo(PartType.THRUSTER, 2, 2, "models/missile_parts/mp_t_20_kerosene_triple.obj", "textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png"));
        RENDER.put("mp_thruster_20_solid", new RenderInfo(PartType.THRUSTER, 1, 1.75, "models/missile_parts/mp_t_20_solid.obj", "textures/models/missile_parts/thrusters/mp_t_20_solid.png"));
        RENDER.put("mp_thruster_20_solid_multi", new RenderInfo(PartType.THRUSTER, 0.5, 1.5, "models/missile_parts/mp_t_20_solid_multi.obj", "textures/models/missile_parts/thrusters/mp_t_20_solid_multi.png"));
        RENDER.put("mp_thruster_20_solid_multier", new RenderInfo(PartType.THRUSTER, 0.5, 1.5, "models/missile_parts/mp_t_20_solid_multi.obj", "textures/models/missile_parts/thrusters/mp_t_20_solid_multier.png"));
        RENDER.put("mp_stability_10_flat", new RenderInfo(PartType.FINS, 0, 2, "models/missile_parts/mp_s_10_flat.obj", "textures/models/missile_parts/stability/mp_s_10_flat.png"));
        RENDER.put("mp_stability_10_cruise", new RenderInfo(PartType.FINS, 0, 3, "models/missile_parts/mp_s_10_cruise.obj", "textures/models/missile_parts/stability/mp_s_10_cruise.png"));
        RENDER.put("mp_stability_10_space", new RenderInfo(PartType.FINS, 0, 2, "models/missile_parts/mp_s_10_space.obj", "textures/models/missile_parts/stability/mp_s_10_space.png"));
        RENDER.put("mp_stability_15_flat", new RenderInfo(PartType.FINS, 0, 3, "models/missile_parts/mp_s_15_flat.obj", "textures/models/missile_parts/stability/mp_s_15_flat.png"));
        RENDER.put("mp_stability_15_thin", new RenderInfo(PartType.FINS, 0, 3, "models/missile_parts/mp_s_15_thin.obj", "textures/models/missile_parts/stability/mp_s_15_thin.png"));
        RENDER.put("mp_stability_15_soyuz", new RenderInfo(PartType.FINS, 0, 3, "models/missile_parts/mp_s_15_soyuz.obj", "textures/models/missile_parts/stability/mp_s_15_soyuz.png"));
        RENDER.put("mp_s_20", new RenderInfo(PartType.FINS, 0, 3, "models/missile_parts/mp_s_20.obj", "textures/models/TheGadget3_.png"));
        RENDER.put("mp_fuselage_10_kerosene", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene.png"));
        RENDER.put("mp_fuselage_10_kerosene_camo", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_camo.png"));
        RENDER.put("mp_fuselage_10_kerosene_desert", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_desert.png"));
        RENDER.put("mp_fuselage_10_kerosene_sky", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_sky.png"));
        RENDER.put("mp_fuselage_10_kerosene_insulation", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_insulation.png"));
        RENDER.put("mp_fuselage_10_kerosene_flames", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_flames.png"));
        RENDER.put("mp_fuselage_10_kerosene_sleek", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_sleek.png"));
        RENDER.put("mp_fuselage_10_kerosene_metal", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_metal.png"));
        RENDER.put("mp_fuselage_10_kerosene_taint", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_kerosene_taint.png"));
        RENDER.put("mp_fuselage_10_solid", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid.png"));
        RENDER.put("mp_fuselage_10_solid_flames", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_flames.png"));
        RENDER.put("mp_fuselage_10_solid_insulation", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_insulation.png"));
        RENDER.put("mp_fuselage_10_solid_sleek", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_sleek.png"));
        RENDER.put("mp_fuselage_10_solid_soviet_glory", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_soviet_glory.png"));
        RENDER.put("mp_fuselage_10_solid_cathedral", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_solid_cathedral.png"));
        RENDER.put("mp_fuselage_10_solid_moonlit", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_solid_moonlit.png"));
        RENDER.put("mp_fuselage_10_solid_battery", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_solid_battery.png"));
        RENDER.put("mp_fuselage_10_solid_duracell", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_duracell.png"));
        RENDER.put("mp_fuselage_10_xenon", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_xenon.png"));
        RENDER.put("mp_fuselage_10_xenon_bhole", new RenderInfo(PartType.FUSELAGE, 4, 3, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_xenon_bhole.png"));
        RENDER.put("mp_fuselage_10_long_kerosene", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_camo", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_camo.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_desert", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_desert.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_sky", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sky.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_flames", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_flames.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_insulation", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_insulation.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_sleek", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sleek.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_metal", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_metal.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_dash", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_dash.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_taint", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_taint.png"));
        RENDER.put("mp_fuselage_10_long_kerosene_vap", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_vap.png"));
        RENDER.put("mp_fuselage_10_long_solid", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid.png"));
        RENDER.put("mp_fuselage_10_long_solid_flames", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_flames.png"));
        RENDER.put("mp_fuselage_10_long_solid_insulation", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_insulation.png"));
        RENDER.put("mp_fuselage_10_long_solid_sleek", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_sleek.png"));
        RENDER.put("mp_fuselage_10_long_solid_soviet_glory", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_soviet_glory.png"));
        RENDER.put("mp_fuselage_10_long_solid_bullet", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_bullet.png"));
        RENDER.put("mp_fuselage_10_long_solid_silvermoonlight", new RenderInfo(PartType.FUSELAGE, 7, 5, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_silvermoonlight.png"));
        RENDER.put("mp_fuselage_10_15_kerosene", new RenderInfo(PartType.FUSELAGE, 9, 5.5, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_kerosene.png"));
        RENDER.put("mp_fuselage_10_15_solid", new RenderInfo(PartType.FUSELAGE, 9, 5.5, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_solid.png"));
        RENDER.put("mp_fuselage_10_15_hydrogen", new RenderInfo(PartType.FUSELAGE, 9, 5.5, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_hydrogen.png"));
        RENDER.put("mp_fuselage_10_15_balefire", new RenderInfo(PartType.FUSELAGE, 9, 5.5, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_balefire.png"));
        RENDER.put("mp_fuselage_15_kerosene", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene.png"));
        RENDER.put("mp_fuselage_15_kerosene_camo", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_camo.png"));
        RENDER.put("mp_fuselage_15_kerosene_desert", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_desert.png"));
        RENDER.put("mp_fuselage_15_kerosene_sky", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_sky.png"));
        RENDER.put("mp_fuselage_15_kerosene_insulation", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_insulation.png"));
        RENDER.put("mp_fuselage_15_kerosene_metal", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_metal.png"));
        RENDER.put("mp_fuselage_15_kerosene_decorated", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_decorated.png"));
        RENDER.put("mp_fuselage_15_kerosene_steampunk", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_steampunk.png"));
        RENDER.put("mp_fuselage_15_kerosene_polite", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_polite.png"));
        RENDER.put("mp_fuselage_15_kerosene_blackjack", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/base/mp_f_15_kerosene_blackjack.png"));
        RENDER.put("mp_fuselage_15_kerosene_lambda", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_lambda.png"));
        RENDER.put("mp_fuselage_15_kerosene_minuteman", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_minuteman.png"));
        RENDER.put("mp_fuselage_15_kerosene_pip", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_pip.png"));
        RENDER.put("mp_fuselage_15_kerosene_taint", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_taint.png"));
        RENDER.put("mp_fuselage_15_kerosene_yuck", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_yuck.png"));
        RENDER.put("mp_fuselage_15_solid", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid.png"));
        RENDER.put("mp_fuselage_15_solid_insulation", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_insulation.png"));
        RENDER.put("mp_fuselage_15_solid_desh", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_desh.png"));
        RENDER.put("mp_fuselage_15_solid_soviet_glory", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_glory.png"));
        RENDER.put("mp_fuselage_15_solid_soviet_stank", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_stank.png"));
        RENDER.put("mp_fuselage_15_solid_faust", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_solid_faust.png"));
        RENDER.put("mp_fuselage_15_solid_silvermoonlight", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_solid_silvermoonlight.png"));
        RENDER.put("mp_fuselage_15_solid_snowy", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_solid_snowy.png"));
        RENDER.put("mp_fuselage_15_solid_panorama", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_panorama.png"));
        RENDER.put("mp_fuselage_15_solid_roses", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_roses.png"));
        RENDER.put("mp_fuselage_15_solid_mimi", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_mimi.png"));
        RENDER.put("mp_fuselage_15_hydrogen", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_hydrogen.obj", "textures/models/missile_parts/fuselages/mp_f_15_hydrogen.png"));
        RENDER.put("mp_fuselage_15_hydrogen_cathedral", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_hydrogen.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_hydrogen_cathedral.png"));
        RENDER.put("mp_fuselage_15_balefire", new RenderInfo(PartType.FUSELAGE, 10, 6, "models/missile_parts/mp_f_15_hydrogen.obj", "textures/models/missile_parts/fuselages/mp_f_15_balefire.png"));
        RENDER.put("mp_fuselage_15_20_kerosene", new RenderInfo(PartType.FUSELAGE, 16, 10, "models/missile_parts/mp_f_15_20_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_20_kerosene.png"));
        RENDER.put("mp_fuselage_15_20_kerosene_magnusson", new RenderInfo(PartType.FUSELAGE, 16, 10, "models/missile_parts/mp_f_15_20_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_20_kerosene_magnusson.png"));
        RENDER.put("mp_fuselage_15_20_solid", new RenderInfo(PartType.FUSELAGE, 16, 10, "models/missile_parts/mp_f_15_20_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_20_solid.png"));
        RENDER.put("mp_warhead_10_he", new RenderInfo(PartType.WARHEAD, 2, 1.5, "models/missile_parts/mp_w_10_he.obj", "textures/models/missile_parts/warheads/mp_w_10_he.png"));
        RENDER.put("mp_warhead_10_incendiary", new RenderInfo(PartType.WARHEAD, 2.5, 2, "models/missile_parts/mp_w_10_incendiary.obj", "textures/models/missile_parts/warheads/mp_w_10_incendiary.png"));
        RENDER.put("mp_warhead_10_buster", new RenderInfo(PartType.WARHEAD, 0.5, 1, "models/missile_parts/mp_w_10_buster.obj", "textures/models/missile_parts/warheads/mp_w_10_buster.png"));
        RENDER.put("mp_warhead_10_nuclear", new RenderInfo(PartType.WARHEAD, 2, 1.5, "models/missile_parts/mp_w_10_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_10_nuclear.png"));
        RENDER.put("mp_warhead_10_nuclear_large", new RenderInfo(PartType.WARHEAD, 2.5, 1.5, "models/missile_parts/mp_w_10_nuclear_large.obj", "textures/models/missile_parts/warheads/mp_w_10_nuclear_large.png"));
        RENDER.put("mp_warhead_10_taint", new RenderInfo(PartType.WARHEAD, 2.25, 1.5, "models/missile_parts/mp_w_10_taint.obj", "textures/models/missile_parts/warheads/mp_w_10_taint.png"));
        RENDER.put("mp_warhead_10_cloud", new RenderInfo(PartType.WARHEAD, 2.25, 1.5, "models/missile_parts/mp_w_10_taint.obj", "textures/models/missile_parts/warheads/mp_w_10_cloud.png"));
        RENDER.put("mp_warhead_15_he", new RenderInfo(PartType.WARHEAD, 2, 1.5, "models/missile_parts/mp_w_15_he.obj", "textures/models/missile_parts/warheads/mp_w_15_he.png"));
        RENDER.put("mp_warhead_15_incendiary", new RenderInfo(PartType.WARHEAD, 2, 1.5, "models/missile_parts/mp_w_15_incendiary.obj", "textures/models/missile_parts/warheads/mp_w_15_incendiary.png"));
        RENDER.put("mp_warhead_15_nuclear", new RenderInfo(PartType.WARHEAD, 3.5, 2, "models/missile_parts/mp_w_15_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_15_nuclear.png"));
        RENDER.put("mp_warhead_15_nuclear_shark", new RenderInfo(PartType.WARHEAD, 3.5, 2, "models/missile_parts/mp_w_15_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_15_nuclear_shark.png"));
        RENDER.put("mp_warhead_15_nuclear_mimi", new RenderInfo(PartType.WARHEAD, 3.5, 2, "models/missile_parts/mp_w_15_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_15_nuclear_mimi.png"));
        RENDER.put("mp_warhead_15_boxcar", new RenderInfo(PartType.WARHEAD, 2.25, 7.5, "models/missile_parts/mp_w_15_boxcar.obj", "textures/models/boxcar.png"));
        RENDER.put("mp_warhead_15_n2", new RenderInfo(PartType.WARHEAD, 3, 2, "models/missile_parts/mp_w_15_n2.obj", "textures/models/missile_parts/warheads/mp_w_15_n2.png"));
        RENDER.put("mp_warhead_15_balefire", new RenderInfo(PartType.WARHEAD, 2.75, 2, "models/missile_parts/mp_w_15_balefire.obj", "textures/models/missile_parts/warheads/mp_w_15_balefire.png"));
        RENDER.put("mp_warhead_15_turbine", new RenderInfo(PartType.WARHEAD, 2.25, 2, "models/missile_parts/mp_w_15_turbine.obj", "textures/models/missile_parts/warheads/mp_w_15_turbine.png"));
    }

    @Nullable
    public static RegistrySupplier<Item> get(String id) {
        return ITEMS.get(id);
    }

    public static Map<String, RegistrySupplier<Item>> all() {
        return ITEMS;
    }

    @Nullable
    public static RenderInfo render(Item item) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return key == null ? null : RENDER.get(key.getPath());
    }

    /** {@code ItemLootCrate.list10/list15/listMisc}: Teile mit Seltenheit, nach Rumpfgroesse sortiert. */
    public static List<ItemCustomMissilePart> lootList(int which) {
        List<ItemCustomMissilePart> out = new ArrayList<>();
        for (RegistrySupplier<Item> s : ITEMS.values()) {
            if (!(s.get() instanceof ItemCustomMissilePart p) || p.rarity == null) continue;
            int list = p.type == PartType.FUSELAGE ? (p.top == PartSize.SIZE_10 ? 10 : p.top == PartSize.SIZE_15 ? 15 : -1) : 0;
            if (list == which) out.add(p);
        }
        return out;
    }
}
