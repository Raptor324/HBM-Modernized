package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Liest die Stapel-Notation der 1.7.10-Konfigurationsdateien ({@code SerializableRecipe.readAStack},
 * {@code readItemStackChance}, {@code readFluidStack}) und loest die alten Namen auf Port-Gegenstaende auf:
 * {@code "hbm:item.powder_sawdust"} findet {@code hbm_m:sawdust_powder} ueber die Wortreihenfolge,
 * {@code "minecraft:paper"} bleibt Vanilla, OreDict-Namen ({@code "ingotSteel"}, {@code "plateCastSteel"}) werden
 * zu Forge-Tags oder den Materialformen des Ports. Port-eigene IDs ({@code hbm_m:...}, {@code #forge:...}) gehen direkt.
 */
public final class LegacyStacks {

    private LegacyStacks() { }

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /** {@code AStack}: Zutat mit Menge. */
    public record Input(Ingredient ingredient, int stacksize, String raw) {
        /** {@code matchesRecipe(stack, ignoreSize)}. */
        public boolean matches(ItemStack stack, boolean ignoreSize) {
            if (stack == null || stack.isEmpty()) return false;
            if (!ingredient.test(stack)) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }
    }

    private static Map<String, Item> tokenIndex;

    private static String norm(String s) {
        String[] parts = s.toLowerCase(Locale.US).split("_");
        Arrays.sort(parts);
        return String.join("_", parts);
    }

    /** Alter Registry-Name -> Port-Gegenstand ({@code Items.AIR}, wenn unbekannt). */
    public static Item item(String name) {
        if (name == null) return Items.AIR;
        String n = name.trim();
        String ns = "minecraft";
        String path = n;
        int colon = n.indexOf(':');
        if (colon >= 0) { ns = n.substring(0, colon); path = n.substring(colon + 1); }
        if (path.startsWith("item.")) path = path.substring(5);
        if (path.startsWith("tile.")) path = path.substring(5);

        if (ns.equals("hbm")) ns = RefStrings.MODID;
        ResourceLocation rl = ResourceLocation.tryParse(ns + ":" + path.toLowerCase(Locale.US));
        if (rl != null) {
            Item direct = BuiltInRegistries.ITEM.get(rl);
            if (direct != Items.AIR) return direct;
        }
        if (!ns.equals(RefStrings.MODID)) return Items.AIR;

        if (tokenIndex == null) {
            tokenIndex = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (key.getNamespace().equals(RefStrings.MODID)) tokenIndex.putIfAbsent(norm(key.getPath()), item);
            }
        }
        return tokenIndex.getOrDefault(norm(path), Items.AIR);
    }

    /** Alter Blockname -> Port-Block ({@code Blocks.AIR}, wenn unbekannt). */
    public static Block block(String name) {
        Item item = item(name);
        if (item instanceof net.minecraft.world.item.BlockItem bi) return bi.getBlock();
        String n = name.replace("hbm:tile.", RefStrings.MODID + ":").replace("hbm:", RefStrings.MODID + ":");
        ResourceLocation rl = ResourceLocation.tryParse(n.toLowerCase(Locale.US));
        return rl == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(rl);
    }

    private static final String[][] DICT_PREFIX = {
            { "plateSextuple", "PLATE_WELDED", "" }, { "plateTriple", "PLATE_CAST", "" }, { "plateCast", "PLATE_CAST", "" },
            { "plateWelded", "PLATE_WELDED", "" }, { "dustTiny", "POWDER_TINY", "" }, { "wireDense", "WIRE_DENSE", "" },
            { "wireFine", "WIRE", "wires_fine" }, { "ingot", "INGOT", "ingots" }, { "plate", "PLATE", "plates" },
            { "dust", "POWDER", "powders" }, { "nugget", "NUGGET", "nuggets" }, { "billet", "BILLET", "" },
            { "block", "BLOCK", "storage_blocks" }, { "gem", "CRYSTAL", "gems" }, { "crystal", "CRYSTAL", "" },
            { "ore", "", "ores" }
    };

    private static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.US);
    }

    /** OreDict-Name -> Zutat (Tag und/oder Materialform). */
    public static Ingredient dict(String dict) {
        if (dict.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(dict.substring(1));
            if (rl != null) return Ingredient.of(TagKey.create(Registries.ITEM, rl));
        }
        for (String[] p : DICT_PREFIX) {
            if (!dict.startsWith(p[0]) || dict.length() <= p[0].length()) continue;
            String mat = snake(dict.substring(p[0].length()));
            List<Ingredient.Value> values = new ArrayList<>();
            List<ItemStack> stacks = new ArrayList<>();
            if (!p[1].isEmpty()) {
                for (ModMaterials m : ModMaterials.values()) {
                    if (!m.name().equalsIgnoreCase(mat) && !matId(m).equals(mat) && !matId(m).equals(mat.replace("_", ""))) continue;
                    try {
                        Item i = ModMaterialItems.item(m, MaterialShape.valueOf(p[1]));
                        if (i != null && i != Items.AIR) stacks.add(new ItemStack(i));
                    } catch (Exception ignored) { }
                }
            }
            if (!stacks.isEmpty() && p[2].isEmpty()) return Ingredient.of(stacks.stream());
            if (!p[2].isEmpty()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", p[2] + "/" + mat));
                if (stacks.isEmpty()) return Ingredient.of(tag);
                List<ItemStack> all = new ArrayList<>(stacks);
                BuiltInRegistries.ITEM.getTag(tag).ifPresent(set -> set.forEach(h -> all.add(new ItemStack(h.value()))));
                return Ingredient.of(all.stream());
            }
        }
        LOGGER.error("[hbm_m] Unbekannter OreDict-Name {}", dict);
        return Ingredient.EMPTY;
    }

    private static String matId(ModMaterials m) {
        return m.getId();
    }

    /** {@code readAStack}: {@code ["item", name, count, meta]}, {@code ["dict", name, count]}, {@code ["nbt", name, count, meta, nbt]}. */
    public static Input readAStack(JsonArray array) {
        try {
            String type = array.get(0).getAsString();
            int stacksize = array.size() > 2 ? array.get(2).getAsInt() : 1;
            if ("item".equals(type) || "nbt".equals(type)) {
                Item item = item(array.get(1).getAsString());
                if (item != Items.AIR) return new Input(Ingredient.of(item), stacksize, array.toString());
            }
            if ("dict".equals(type)) {
                return new Input(dict(array.get(1).getAsString()), stacksize, array.toString());
            }
        } catch (Exception ignored) { }
        LOGGER.error("[hbm_m] Error reading stack array {}", array);
        return new Input(Ingredient.EMPTY, 1, array.toString());
    }

    /** {@code readItemStackChance}: {@code [name, (count, (meta, (nbt))), chance]}. */
    public static ChanceStack readItemStackChance(JsonArray array) {
        try {
            Item item = item(array.get(0).getAsString());
            int stacksize = array.size() > 2 ? array.get(1).getAsInt() : 1;
            if (item != Items.AIR) {
                ItemStack stack = new ItemStack(item, stacksize);
                if (array.size() > 4) {
                    try { stack.setTag(net.minecraft.nbt.TagParser.parseTag(array.get(3).getAsString())); } catch (Exception ignored) { }
                }
                float chance = array.get(array.size() - 1).getAsFloat();
                return new ChanceStack(stack, chance);
            }
        } catch (Exception ignored) { }
        LOGGER.error("[hbm_m] Error reading stack array {} - defaulting to NOTHING item!", array);
        return new ChanceStack(ItemStack.EMPTY, 1F);
    }

    public record ChanceStack(ItemStack stack, float chance) { }

    /** {@code readFluidStack}: {@code [name, fill, (pressure)]}; Original-Namen sind GROSS, Port-IDs klein. */
    public record LegacyFluid(net.minecraft.world.level.material.Fluid type, int fill, int pressure) { }

    @Nullable
    public static net.minecraft.world.level.material.Fluid fluid(String name) {
        String lower = name.toLowerCase(Locale.US);
        var e = com.hbm_m.inventory.fluid.ModFluids.getEntry(lower);
        if (e == null) {
            String flat = lower.replace("_", "");
            for (var entry : com.hbm_m.inventory.fluid.ModFluids.getAllEntries().entrySet()) {
                if (entry.getKey().replace("_", "").equals(flat)) { e = entry.getValue(); break; }
            }
        }
        if (e != null) return e.getSource();
        ResourceLocation rl = ResourceLocation.tryParse(lower);
        if (rl != null && BuiltInRegistries.FLUID.containsKey(rl)) return BuiltInRegistries.FLUID.get(rl);
        return null;
    }

    public static LegacyFluid readFluidStack(JsonArray array) {
        try {
            var type = fluid(array.get(0).getAsString());
            int fill = array.get(1).getAsInt();
            int pressure = array.size() < 3 ? 0 : array.get(2).getAsInt();
            if (type != null) return new LegacyFluid(type, fill, pressure);
        } catch (Exception ignored) { }
        LOGGER.error("[hbm_m] Error reading fluid array {}", array);
        return new LegacyFluid(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource(), 0, 0);
    }

    /** Port-Name fuer das Schreiben von Vorlagen. */
    public static String name(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static String fluidName(net.minecraft.world.level.material.Fluid fluid) {
        for (var entry : com.hbm_m.inventory.fluid.ModFluids.getAllEntries().entrySet()) {
            if (entry.getValue().getSource() == fluid) return entry.getKey().toUpperCase(Locale.US);
        }
        return BuiltInRegistries.FLUID.getKey(fluid).toString();
    }
}
