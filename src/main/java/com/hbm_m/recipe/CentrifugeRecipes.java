package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.item.ModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Centrifuge recipe registry - adapted from 1.7.10 HBM.
 * Uses direct HashMap lookup for reliable recipe matching.
 */
public class CentrifugeRecipes {

    private static final Map<RecipeInput, ItemStack[]> recipes = new HashMap<>();

    // Recipe input wrapper - can match by item or tag
    public static abstract class RecipeInput {
        public abstract boolean matches(ItemStack stack);
        public abstract List<ItemStack> getDisplayStacks();
    }

    public static class ItemInput extends RecipeInput {
        private final Item item;

        public ItemInput(Item item) {
            this.item = item;
        }

        public ItemInput(ItemStack stack) {
            this.item = stack.getItem();
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(item);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(new ItemStack(item));
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ItemInput other) {
                return this.item == other.item;
            }
            return false;
        }
    }

    public static class TagInput extends RecipeInput {
        private final TagKey<Item> tag;

        public TagInput(String tagName) {
            this.tag = ItemTags.create(ResourceLocation.tryParse(tagName));
        }

        public TagInput(TagKey<Item> tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(tag);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            ForgeRegistries.ITEMS.tags().getTag(tag).forEach(item -> {
                stacks.add(new ItemStack(item));
            });
            return stacks.isEmpty() ? List.of(ItemStack.EMPTY) : stacks;
        }

        @Override
        public int hashCode() {
            return tag.location().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TagInput other) {
                return this.tag.location().equals(other.tag.location());
            }
            return false;
        }

        public TagKey<Item> getTag() {
            return tag;
        }
    }

    public static void registerRecipes() {
        recipes.clear();

        // =========== ORE RECIPES ===========

        // Coal Ore
        addTagRecipe("forge:ores/coal", 
            stack("hbm_m:coal_powder", 2),
            stack("hbm_m:coal_powder", 2),
            stack("hbm_m:coal_powder", 2),
            new ItemStack(Items.GRAVEL));

        // Iron Ore
        addTagRecipe("forge:ores/iron",
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:iron_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Gold Ore
        addTagRecipe("forge:ores/gold",
            stack("hbm_m:gold_powder", 1),
            stack("hbm_m:gold_powder", 1),
            stack("hbm_m:gold_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Copper Ore
        addTagRecipe("forge:ores/copper",
            stack("hbm_m:copper_powder", 1),
            stack("hbm_m:copper_powder", 1),
            stack("hbm_m:gold_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Diamond Ore
        addTagRecipe("forge:ores/diamond",
            stack("hbm_m:diamond_powder", 1),
            stack("hbm_m:diamond_powder", 1),
            stack("hbm_m:diamond_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Emerald Ore
        addTagRecipe("forge:ores/emerald",
            stack("hbm_m:emerald_powder", 1),
            stack("hbm_m:emerald_powder", 1),
            stack("hbm_m:emerald_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Redstone Ore
        addTagRecipe("forge:ores/redstone",
            new ItemStack(Items.REDSTONE, 3),
            new ItemStack(Items.REDSTONE, 3),
            stack("hbm_m:mercury_ingot", 1),
            new ItemStack(Items.GRAVEL));

        // Lapis Ore
        addTagRecipe("forge:ores/lapis",
            stack("hbm_m:lapis_powder", 6),
            stack("hbm_m:cobalt_powder_tiny", 1),
            stack("hbm_m:sodalite_gem", 1),
            new ItemStack(Items.GRAVEL));

        // Quartz Ore
        addTagRecipe("forge:ores/quartz",
            stack("hbm_m:quartz_powder", 1),
            stack("hbm_m:quartz_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1),
            new ItemStack(Items.NETHERRACK));

        // =========== HBM ORE RECIPES ===========

        // Uranium Ore
        addTagRecipe("forge:ores/uranium",
            stack("hbm_m:uranium_powder", 1),
            stack("hbm_m:uranium_powder", 1),
            stack("hbm_m:ra226_ingot", 1),
            new ItemStack(Items.GRAVEL));

        // Thorium Ore
        addTagRecipe("forge:ores/thorium",
            stack("hbm_m:thorium_powder", 1),
            stack("hbm_m:thorium_powder", 1),
            stack("hbm_m:uranium_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Titanium Ore
        addTagRecipe("forge:ores/titanium",
            stack("hbm_m:titanium_powder", 1),
            stack("hbm_m:titanium_powder", 1),
            stack("hbm_m:iron_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Tungsten Ore
        addTagRecipe("forge:ores/tungsten",
            stack("hbm_m:tungsten_powder", 1),
            stack("hbm_m:tungsten_powder", 1),
            stack("hbm_m:iron_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Lead Ore
        addTagRecipe("forge:ores/lead",
            stack("hbm_m:lead_powder", 1),
            stack("hbm_m:lead_powder", 1),
            stack("hbm_m:gold_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Beryllium Ore
        addTagRecipe("forge:ores/beryllium",
            stack("hbm_m:beryllium_powder", 1),
            stack("hbm_m:beryllium_powder", 1),
            stack("hbm_m:emerald_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Fluorite Ore
        addTagRecipe("forge:ores/fluorite",
            stack("hbm_m:fluorite", 3),
            stack("hbm_m:fluorite", 3),
            stack("hbm_m:sodalite_gem", 1),
            new ItemStack(Items.GRAVEL));

        // Aluminum/Bauxite Ore
        addTagRecipe("forge:ores/aluminum",
            stack("hbm_m:titanium_powder", 1),
            stack("hbm_m:aluminum_powder", 1),
            stack("hbm_m:iron_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Sulfur Ore
        addTagRecipe("forge:ores/sulfur",
            stack("hbm_m:sulfur", 3),
            stack("hbm_m:sulfur", 3),
            stack("hbm_m:iron_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Lignite Ore
        addTagRecipe("forge:ores/lignite",
            stack("hbm_m:lignite_powder", 2),
            stack("hbm_m:lignite_powder", 2),
            stack("hbm_m:lignite_powder", 2),
            new ItemStack(Items.GRAVEL));

        // Asbestos Ore
        addTagRecipe("forge:ores/asbestos",
            stack("hbm_m:ite_powder", 2),
            stack("hbm_m:ite_powder", 2),
            stack("hbm_m:calcium_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Cinnabar Ore
        addTagRecipe("forge:ores/cinnabar",
            stack("hbm_m:mercury_ingot", 2),
            stack("hbm_m:mercury_ingot", 2),
            stack("hbm_m:sulfur", 1),
            new ItemStack(Items.GRAVEL));

        // Cobalt Ore
        addTagRecipe("forge:ores/cobalt",
            stack("hbm_m:cobalt_powder", 2),
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:copper_powder", 1),
            new ItemStack(Items.GRAVEL));

        // =========== CRYSTAL RECIPES ===========

        addItemRecipe("hbm_m:crystal_coal",
            stack("hbm_m:coal_powder", 3),
            stack("hbm_m:coal_powder", 3),
            stack("hbm_m:coal_powder", 3),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_iron",
            stack("hbm_m:iron_powder", 2),
            stack("hbm_m:iron_powder", 2),
            stack("hbm_m:titanium_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_gold",
            stack("hbm_m:gold_powder", 2),
            stack("hbm_m:gold_powder", 2),
            stack("hbm_m:mercury_ingot", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_redstone",
            new ItemStack(Items.REDSTONE, 3),
            new ItemStack(Items.REDSTONE, 3),
            new ItemStack(Items.REDSTONE, 3),
            stack("hbm_m:mercury_ingot", 3));

        addItemRecipe("hbm_m:crystal_lapis",
            stack("hbm_m:lapis_powder", 4),
            stack("hbm_m:lapis_powder", 4),
            stack("hbm_m:cobalt_powder", 1),
            stack("hbm_m:sodalite_gem", 2));

        addItemRecipe("hbm_m:crystal_diamond",
            stack("hbm_m:diamond_powder", 1),
            stack("hbm_m:diamond_powder", 1),
            stack("hbm_m:diamond_powder", 1),
            stack("hbm_m:diamond_powder", 1));

        addItemRecipe("hbm_m:crystal_uranium",
            stack("hbm_m:uranium_powder", 2),
            stack("hbm_m:uranium_powder", 2),
            stack("hbm_m:ra226_ingot", 2),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_thorium",
            stack("hbm_m:thorium_powder", 2),
            stack("hbm_m:thorium_powder", 2),
            stack("hbm_m:uranium_powder", 1),
            stack("hbm_m:ra226_ingot", 1));

        addItemRecipe("hbm_m:crystal_plutonium",
            stack("hbm_m:plutonium_powder", 2),
            stack("hbm_m:plutonium_powder", 2),
            stack("hbm_m:polonium_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_titanium",
            stack("hbm_m:titanium_powder", 2),
            stack("hbm_m:titanium_powder", 2),
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_sulfur",
            stack("hbm_m:sulfur", 4),
            stack("hbm_m:sulfur", 4),
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:mercury_ingot", 1));

        addItemRecipe("hbm_m:crystal_copper",
            stack("hbm_m:copper_powder", 2),
            stack("hbm_m:copper_powder", 2),
            stack("hbm_m:sulfur", 1),
            stack("hbm_m:cobalt_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_tungsten",
            stack("hbm_m:tungsten_powder", 2),
            stack("hbm_m:tungsten_powder", 2),
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_aluminium",
            stack("hbm_m:aluminum_powder", 2),
            stack("hbm_m:titanium_powder", 1),
            stack("hbm_m:iron_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_fluorite",
            stack("hbm_m:fluorite", 4),
            stack("hbm_m:fluorite", 4),
            stack("hbm_m:sodalite_gem", 2),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_beryllium",
            stack("hbm_m:beryllium_powder", 2),
            stack("hbm_m:beryllium_powder", 2),
            stack("hbm_m:quartz_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_lead",
            stack("hbm_m:lead_powder", 2),
            stack("hbm_m:lead_powder", 2),
            stack("hbm_m:gold_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_schrabidium",
            stack("hbm_m:schrabidium_powder", 2),
            stack("hbm_m:schrabidium_powder", 2),
            stack("hbm_m:plutonium_powder", 1),
            stack("hbm_m:lithium_powder_tiny", 1));

        addItemRecipe("hbm_m:crystal_lithium",
            stack("hbm_m:lithium_powder", 2),
            stack("hbm_m:lithium_powder", 2),
            stack("hbm_m:quartz_powder", 1),
            stack("hbm_m:fluorite", 1));

        addItemRecipe("hbm_m:crystal_cobalt",
            stack("hbm_m:cobalt_powder", 2),
            stack("hbm_m:iron_powder", 3),
            stack("hbm_m:copper_powder", 3),
            stack("hbm_m:lithium_powder_tiny", 1));

        // =========== MISC RECIPES ===========

        // Blaze Rod
        addItemRecipe(Items.BLAZE_ROD,
            new ItemStack(Items.BLAZE_POWDER, 1),
            new ItemStack(Items.BLAZE_POWDER, 1),
            stack("hbm_m:fire_powder", 1),
            stack("hbm_m:fire_powder", 1));
    }

    // Helper methods
    private static ItemStack stack(String itemId, int count) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    public static void addTagRecipe(String tag, ItemStack... outputs) {
        ItemStack[] sanitized = sanitizeOutputs(outputs);
        recipes.put(new TagInput(tag), sanitized);
    }

    public static void addItemRecipe(String itemId, ItemStack... outputs) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) return;
        addItemRecipe(item, outputs);
    }

    public static void addItemRecipe(Item item, ItemStack... outputs) {
        ItemStack[] sanitized = sanitizeOutputs(outputs);
        recipes.put(new ItemInput(item), sanitized);
    }

    private static ItemStack[] sanitizeOutputs(ItemStack[] outputs) {
        ItemStack[] result = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            if (i < outputs.length && outputs[i] != null && !outputs[i].isEmpty()) {
                result[i] = outputs[i].copy();
            } else {
                result[i] = ItemStack.EMPTY;
            }
        }
        return result;
    }

    /**
     * Get the output for the given input stack.
     * Returns null if no recipe matches.
     */
    public static ItemStack[] getOutput(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // First check exact item matches
        for (Map.Entry<RecipeInput, ItemStack[]> entry : recipes.entrySet()) {
            if (entry.getKey() instanceof ItemInput && entry.getKey().matches(input)) {
                return copyOutputs(entry.getValue());
            }
        }

        // Then check tag matches
        for (Map.Entry<RecipeInput, ItemStack[]> entry : recipes.entrySet()) {
            if (entry.getKey() instanceof TagInput && entry.getKey().matches(input)) {
                return copyOutputs(entry.getValue());
            }
        }

        return null;
    }

    private static ItemStack[] copyOutputs(ItemStack[] outputs) {
        ItemStack[] copy = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            copy[i] = outputs[i].copy();
        }
        return copy;
    }

    /**
     * Get all recipes for JEI display.
     */
    public static Map<RecipeInput, ItemStack[]> getAllRecipes() {
        return recipes;
    }

    /**
     * Get recipes as NonNullList outputs for compatibility.
     */
    public static NonNullList<ItemStack> getOutputsAsList(ItemStack input) {
        ItemStack[] outputs = getOutput(input);
        if (outputs == null) {
            return NonNullList.create();
        }
        NonNullList<ItemStack> list = NonNullList.withSize(4, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(outputs.length, 4); i++) {
            list.set(i, outputs[i]);
        }
        return list;
    }
}
