package com.hbm_m.inventory.material;

import static com.hbm_m.inventory.material.MaterialShapes.*;
import static com.hbm_m.inventory.material.Mats.*;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code MatDistribution.registerDefaults}: feste Schmelzwerte einzelner Items und der Erze (mit Nebenprodukten).
 * Erz-Schluessel sind Forge-Tags ({@code forge:ores/iron}); Port-Erzbloecke ohne Tag werden zusaetzlich namentlich
 * ihrem Schluessel zugeordnet (Original: alle Varianten stehen unter demselben Ore-Dictionary-Namen).
 */
public final class MatDistribution {

    private static boolean loaded = false;

    private MatDistribution() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    public static void registerDefaults() {
        //vanilla crap
        registerOre("forge:stone", MAT_STONE, BLOCK.q(1));
        registerOre("forge:cobblestone", MAT_STONE, BLOCK.q(1));
        registerEntry(Items.OBSIDIAN, MAT_OBSIDIAN, BLOCK.q(1));
        registerEntry(Items.RAIL, MAT_IRON, INGOT.q(6, 16));
        registerEntry(Items.POWERED_RAIL, MAT_GOLD, INGOT.q(6, 6), MAT_REDSTONE, DUST.q(1, 6));
        registerEntry(Items.DETECTOR_RAIL, MAT_IRON, INGOT.q(6, 6), MAT_REDSTONE, DUST.q(1, 6));
        registerEntry(Items.MINECART, MAT_IRON, INGOT.q(5));

        //castables
        registerEntry(port("blade_titanium"),      MAT_TITANIUM,    INGOT.q(3));
        registerEntry(port("blade_tungsten"),      MAT_TUNGSTEN,    INGOT.q(3));
        registerEntry(port("blades_steel"),        MAT_STEEL,       INGOT.q(4));
        registerEntry(port("blades_titanium"),     MAT_TITANIUM,    INGOT.q(4));
        registerEntry(port("stamp_stone_flat"),    MAT_STONE,       INGOT.q(3));
        registerEntry(port("stamp_iron_flat"),     MAT_IRON,        INGOT.q(3));
        registerEntry(port("stamp_steel_flat"),    MAT_STEEL,       INGOT.q(3));
        registerEntry(port("stamp_titanium_flat"), MAT_TITANIUM,    INGOT.q(3));
        registerEntry(port("stamp_obsidian_flat"), MAT_OBSIDIAN,    INGOT.q(3));
        registerEntry(port("pipes_steel"),         MAT_STEEL,       BLOCK.q(3));

        registerEntry(port("casing_small"),        MAT_GUNMETAL,    PLATE.q(1, 4));
        registerEntry(port("casing_small_steel"),  MAT_WEAPONSTEEL, PLATE.q(1, 4));
        registerEntry(port("casing_large"),        MAT_GUNMETAL,    PLATE.q(1, 2));
        registerEntry(port("casing_large_steel"),  MAT_WEAPONSTEEL, PLATE.q(1, 2));
        registerEntry(port("cryolite_chunk"), MAT_ALUMINIUM, INGOT.q(1), MAT_SODIUM, INGOT.q(1));

        //actual ores
        registerOre(ore("iron", "ore_gneiss_iron", "gneiss_iron_ore", "ore_meteor_iron"), MAT_IRON, INGOT.q(2), MAT_TITANIUM, NUGGET.q(3), MAT_STONE, QUART.q(1));
        registerOre(ore("titanium", "titanium_ore", "titanium_ore_deepslate"), MAT_TITANIUM, INGOT.q(2), MAT_IRON, NUGGET.q(3), MAT_STONE, QUART.q(1));
        registerOre(ore("tungsten", "tungsten_ore", "tungsten_ore_deepslate", "nether_tungsten_ore", "ore_nether_tungsten"), MAT_TUNGSTEN, INGOT.q(2), MAT_STONE, QUART.q(1));
        registerOre(ore("aluminum", "ore_aluminium", "aluminum_ore_deepslate", "ore_meteor_aluminium"), MAT_ALUMINIUM, INGOT.q(2), MAT_SODIUM, NUGGET.q(3), MAT_STONE, QUART.q(1));

        registerOre(ore("coal", "nether_coal_ore", "ore_nether_coal"), MAT_CARBON, GEM.q(3), MAT_STONE, QUART.q(1));
        registerOre(ore("gold", "gneiss_gold_ore", "ore_gneiss_gold"), MAT_GOLD, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        registerOre(ore("uranium", "uranium_ore_deepslate", "nether_uranium_ore", "ore_nether_uranium", "gneiss_uranium_ore", "ore_gneiss_uranium"), MAT_URANIUM, INGOT.q(2), MAT_LEAD, NUGGET.q(3), MAT_STONE, QUART.q(1));
        registerOre(ore("thorium", "thorium_ore", "thorium_ore_deepslate"), MAT_THORIUM, INGOT.q(2), MAT_URANIUM, NUGGET.q(3), MAT_STONE, QUART.q(1));
        registerOre(ore("copper", "ore_copper", "gneiss_copper_ore", "ore_gneiss_copper", "ore_meteor_copper"), MAT_COPPER, INGOT.q(2), MAT_STONE, QUART.q(1));
        registerOre(ore("lead", "lead_ore", "lead_ore_deepslate"), MAT_LEAD, INGOT.q(2), MAT_GOLD, NUGGET.q(1), MAT_STONE, QUART.q(1));
        registerOre(ore("beryllium", "beryllium_ore", "beryllium_ore_deepslate"), MAT_BERYLLIUM, INGOT.q(2), MAT_STONE, QUART.q(1));
        registerOre(ore("cobalt", "cobalt_ore", "cobalt_ore_deepslate", "nether_cobalt_ore", "ore_nether_cobalt", "ore_meteor_cobalt"), MAT_COBALT, INGOT.q(1), MAT_STONE, QUART.q(1));
        registerOre(ore("redstone"), MAT_REDSTONE, INGOT.q(4), MAT_STONE, QUART.q(1));

        registerOre(ore("hematite", "stone_resource_hematite"), MAT_HEMATITE, INGOT.q(1));
        registerOre(ore("malachite", "stone_resource_malachite"), MAT_MALACHITE, INGOT.q(6));

        registerEntry(port("stone_resource_limestone"), MAT_FLUX, DUST.q(10));
        registerEntry(port("flux_powder"), MAT_FLUX, DUST.q(1));
        registerEntry(Items.CHARCOAL, MAT_CARBON, NUGGET.q(3));

        registerEntry(port("ash_wood"), MAT_CARBON, NUGGET.q(1));
        registerEntry(port("ash_coal"), MAT_CARBON, NUGGET.q(2));
        registerEntry(port("ash_misc"), MAT_CARBON, NUGGET.q(1));
    }

    private static Item port(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
    }

    /** Erz-Schluessel {@code forge:ores/<name>}; die genannten Port-Bloecke gehoeren ebenfalls dazu. */
    private static String ore(String name, String... portBlocks) {
        String key = "forge:ores/" + name;
        for (String id : portBlocks) {
            oreItemKeys.put(RefStrings.MODID + ":" + id, key);
        }
        return key;
    }

    public static void registerEntry(Item key, Object... matDef) {
        if (key == null || key == Items.AIR) return;
        if (matDef.length % 2 == 1) return;

        List<MaterialStack> stacks = new ArrayList<>();

        for (int i = 0; i < matDef.length; i += 2) {
            stacks.add(new MaterialStack((NTMMaterial) matDef[i], (int) matDef[i + 1]));
        }

        if (stacks.isEmpty()) return;

        materialEntries.put(key, stacks);
    }

    public static void registerOre(String key, Object... matDef) {
        if (matDef.length % 2 == 1) return;

        List<MaterialStack> stacks = new ArrayList<>();

        for (int i = 0; i < matDef.length; i += 2) {
            stacks.add(new MaterialStack((NTMMaterial) matDef[i], (int) matDef[i + 1]));
        }

        if (stacks.isEmpty()) return;

        materialOreEntries.put(key, stacks);
    }
}
