package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;

/**
 * Centrifuge recipe registry - adapted from 1.7.10 HBM.
 * Uses direct HashMap lookup for reliable recipe matching. Code made by TRFFuchs
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
            this.tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagName));
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
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(item -> {
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
            new ItemStack(Items.EMERALD),
            new ItemStack(Items.GRAVEL));

        // Fluorite Ore
        addTagRecipe("forge:ores/fluorite",
            stack("hbm_m:fluorite", 3),
            stack("hbm_m:fluorite", 3),
            stack("hbm_m:rareground_ore_chunk", 1),
            new ItemStack(Items.GRAVEL));

        // Aluminum/Bauxite Ore
        addTagRecipe("forge:ores/aluminum",
            stack("hbm_m:aluminum_powder", 1),
            stack("hbm_m:aluminum_powder", 1),
            stack("hbm_m:titanium_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Sulfur Ore
        addTagRecipe("forge:ores/sulfur",
            stack("hbm_m:sulfur", 4),
            stack("hbm_m:sulfur", 4),
            stack("hbm_m:sulfur", 4),
            new ItemStack(Items.GRAVEL));

        // Lignite Ore
        addTagRecipe("forge:ores/lignite",
            stack("hbm_m:lignite", 2),
            stack("hbm_m:lignite", 2),
            stack("hbm_m:lignite", 2),
            new ItemStack(Items.GRAVEL));

        // Asbestos Ore
        addTagRecipe("forge:ores/asbestos",
            stack("hbm_m:asbestos_powder", 1),
            stack("hbm_m:asbestos_powder", 1),
            stack("hbm_m:asbestos_powder", 1),
            new ItemStack(Items.GRAVEL));

        // Cinnabar Ore
        addTagRecipe("forge:ores/cinnabar",
            stack("hbm_m:cinnabar", 2),
            stack("hbm_m:cinnabar", 2),
            stack("hbm_m:sulfur", 1),
            new ItemStack(Items.GRAVEL));

        // Rareground Ore
        addTagRecipe("forge:ores/rareground",
            stack("hbm_m:rareground_ore_chunk", 1),
            stack("hbm_m:rareground_ore_chunk", 1),
            stack("hbm_m:dust", 2),
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
            stack("hbm_m:gold_powder", 2),
            new ItemStack(Items.GRAVEL));

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

        // =========== BEDROCK ORE CHAIN (Mining Drill / Bedrock refinement) ===========

        // ---- Bedrock Ore chain: LIGHT ----
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_LIGHT.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ROASTED_LIGHT.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_WASHED_LIGHT.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get(), 2), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 9), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 9));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 9), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 9));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_LIGHT.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_LIGHT.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RAD_LIGHT.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_LIGHT.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_LIGHT.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 9), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 9), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NORAD_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 9), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_FIRST_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 18), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SECOND_LIGHT.get(), new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 9), new ItemStack(net.minecraft.world.item.Items.RAW_COPPER, 18), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_SULFURIC_WASHED_LIGHT.get(), new ItemStack(ModItems.TITANIUM_RAW.get(), 6), new ItemStack(ModBlocks.RESOURCE_BAUXITE.get(), 9), new ItemStack(ModItems.CRYOLITE.get(), 3));
        // Original: solvent=CHLOROCALCITE,LITHIUM,SODIUM; rad=CHLOROCALCITE,LITHIUM,SODIUM
        addItemRecipe(ModItems.BEDROCK_ORE_SOLVENT_WASHED_LIGHT.get(), new ItemStack(ModItems.POWDER_CHLOROCALCITE.get(), 5), new ItemStack(ModItems.LITHIUM.get(), 5), new ItemStack(ModItems.POWDER_SODIUM.get(), 3));
        addItemRecipe(ModItems.BEDROCK_ORE_RAD_WASHED_LIGHT.get(), new ItemStack(ModItems.POWDER_CHLOROCALCITE.get(), 6), new ItemStack(ModItems.LITHIUM.get(), 6), new ItemStack(ModItems.POWDER_SODIUM.get(), 6));

        // ---- Bedrock Ore chain: HEAVY ----
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_HEAVY.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ROASTED_HEAVY.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_WASHED_HEAVY.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get(), 2), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 9));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 9));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_HEAVY.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_HEAVY.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RAD_HEAVY.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_HEAVY.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_HEAVY.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NORAD_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_FIRST_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 18), new ItemStack(ModItems.LEAD_RAW.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SECOND_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 18), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_SULFURIC_WASHED_HEAVY.get(), new ItemStack(net.minecraft.world.item.Items.RAW_GOLD, 2), new ItemStack(net.minecraft.world.item.Items.RAW_GOLD, 2), new ItemStack(ModItems.BERYLLIUM_RAW.get(), 3));
        addItemRecipe(ModItems.BEDROCK_ORE_SOLVENT_WASHED_HEAVY.get(), new ItemStack(ModItems.TUNGSTEN_RAW.get(), 9), new ItemStack(ModItems.LEAD_RAW.get(), 9), new ItemStack(net.minecraft.world.item.Items.RAW_GOLD, 5));
        addItemRecipe(ModItems.BEDROCK_ORE_RAD_WASHED_HEAVY.get(), new ItemStack(ingot(ModIngots.BISMUTH), 2), new ItemStack(ingot(ModIngots.TANTALIUM), 2), new ItemStack(net.minecraft.world.item.Items.RAW_GOLD, 6));

        // ---- Bedrock Ore chain: RARE ----
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_RARE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RARE.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ROASTED_RARE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RARE.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_WASHED_RARE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_RARE.get(), 2), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 5), new ItemStack(ModItems.RAREEARTH_RAW.get(), 5));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 5), new ItemStack(ModItems.RAREEARTH_RAW.get(), 5));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_RARE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_RARE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RAD_RARE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_RARE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_RARE.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 5), new ItemStack(ModItems.RAREEARTH_RAW.get(), 5), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_RARE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 5), new ItemStack(ModItems.RAREEARTH_RAW.get(), 5), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_RARE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NORAD_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 5), new ItemStack(ModItems.RAREEARTH_RAW.get(), 5), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_RARE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_FIRST_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 10), new ItemStack(ModItems.RAREEARTH_RAW.get(), 5), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_RARE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SECOND_RARE.get(), new ItemStack(ModItems.COBALT_RAW.get(), 5), new ItemStack(ModItems.RAREEARTH_RAW.get(), 10), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_RARE.get()));
        // Original: acid=BORON,LANTHANIUM,NIOBIUM; solvent=NEODYMIUM,STRONTIUM,ZIRCONIUM; rad=NIOBIUM,NEODYMIUM,STRONTIUM
        addItemRecipe(ModItems.BEDROCK_ORE_SULFURIC_WASHED_RARE.get(), new ItemStack(ingot(ModIngots.BORON), 5), new ItemStack(ingot(ModIngots.LANTHANIUM), 3), new ItemStack(ingot(ModIngots.NIOBIUM), 4));
        addItemRecipe(ModItems.BEDROCK_ORE_SOLVENT_WASHED_RARE.get(), new ItemStack(ingot(ModIngots.NEODYMIUM), 3), new ItemStack(ingot(ModIngots.STRONTIUM), 3), new ItemStack(ingot(ModIngots.ZIRCONIUM), 3));
        addItemRecipe(ModItems.BEDROCK_ORE_RAD_WASHED_RARE.get(), new ItemStack(ingot(ModIngots.NIOBIUM), 5), new ItemStack(ingot(ModIngots.NEODYMIUM), 5), new ItemStack(ingot(ModIngots.STRONTIUM), 3));

        // ---- Bedrock Ore chain: ACTINIDE ----
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ACTINIDE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ROASTED_ACTINIDE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_WASHED_ACTINIDE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get(), 2), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 4), new ItemStack(ModItems.THORIUM_RAW.get(), 4));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 4), new ItemStack(ModItems.THORIUM_RAW.get(), 4));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_ACTINIDE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_ACTINIDE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RAD_ACTINIDE.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 4), new ItemStack(ModItems.THORIUM_RAW.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 4), new ItemStack(ModItems.THORIUM_RAW.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NORAD_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 4), new ItemStack(ModItems.THORIUM_RAW.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_FIRST_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 8), new ItemStack(ModItems.THORIUM_RAW.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SECOND_ACTINIDE.get(), new ItemStack(ModItems.URANIUM_RAW.get(), 4), new ItemStack(ModItems.THORIUM_RAW.get(), 8), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get()));
        // Original: acid=RADIUM,RADIUM,POLONIUM; solvent=RADIUM,RADIUM,POLONIUM; rad=TECHNETIUM,TECHNETIUM,U238
        addItemRecipe(ModItems.BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE.get(), new ItemStack(ModItems.RADIUM_RAW.get(), 2), new ItemStack(ModItems.BILLET_POLONIUM.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE.get(), new ItemStack(ModItems.RADIUM_RAW.get(), 2), new ItemStack(ModItems.BILLET_POLONIUM.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_RAD_WASHED_ACTINIDE.get(), new ItemStack(ModItems.BILLET_TECHNETIUM.get(), 2), new ItemStack(ModItems.BILLET_U238.get()));

        // ---- Bedrock Ore chain: NONMETAL ----
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_NONMETAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ROASTED_NONMETAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_WASHED_NONMETAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get(), 2), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 9), new ItemStack(ModItems.SULFUR.get(), 9));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 9), new ItemStack(ModItems.SULFUR.get(), 9));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_NONMETAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_NONMETAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RAD_NONMETAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_NONMETAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 9), new ItemStack(ModItems.SULFUR.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 9), new ItemStack(ModItems.SULFUR.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NORAD_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 9), new ItemStack(ModItems.SULFUR.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_FIRST_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 18), new ItemStack(ModItems.SULFUR.get(), 9), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SECOND_NONMETAL.get(), new ItemStack(net.minecraft.world.item.Items.COAL, 9), new ItemStack(ModItems.SULFUR.get(), 18), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_SULFURIC_WASHED_NONMETAL.get(), new ItemStack(ModItems.LIGNITE.get(), 9), new ItemStack(ModItems.SALTPETER.get(), 6), new ItemStack(ModItems.FLUORITE.get(), 6));
        addItemRecipe(ModItems.BEDROCK_ORE_SOLVENT_WASHED_NONMETAL.get(), new ItemStack(ModItems.CRYSTAL_PHOSPHORUS.get(), 5), new ItemStack(ModItems.FLUORITE.get(), 6), new ItemStack(ModItems.SULFUR.get(), 6));
        // Original rad=CHLOROCALCITE,SILICON,SILICON
        addItemRecipe(ModItems.BEDROCK_ORE_RAD_WASHED_NONMETAL.get(), new ItemStack(ModItems.POWDER_CHLOROCALCITE.get(), 6), new ItemStack(ingot(ModIngots.SILICON), 2), new ItemStack(ingot(ModIngots.SILICON), 2));

        // ---- Bedrock Ore chain: CRYSTAL ----
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_CRYSTAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_ROASTED_CRYSTAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get()), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_BASE_WASHED_CRYSTAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get(), 2), new ItemStack(Items.GRAVEL));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 9), new ItemStack(ModItems.CINNABAR.get(), 4));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_ROASTED_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 9), new ItemStack(ModItems.CINNABAR.get(), 4));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SULFURIC_CRYSTAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SOLVENT_CRYSTAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_RAD_CRYSTAL.get(), new ItemStack(ModItems.BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL.get(), 2), new ItemStack(ModItems.BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL.get(), 2));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSULFURIC_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 9), new ItemStack(ModItems.CINNABAR.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NOSOLVENT_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 9), new ItemStack(ModItems.CINNABAR.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_NORAD_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 9), new ItemStack(ModItems.CINNABAR.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_FIRST_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 18), new ItemStack(ModItems.CINNABAR.get(), 4), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get()));
        addItemRecipe(ModItems.BEDROCK_ORE_PRIMARY_SECOND_CRYSTAL.get(), new ItemStack(net.minecraft.world.item.Items.REDSTONE, 9), new ItemStack(ModItems.CINNABAR.get(), 8), new ItemStack(ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get()));
        // Original: acid=SODALITE,ASBESTOS,DIAMOND; solvent=CINNABAR,ASBESTOS,EMERALD
        addItemRecipe(ModItems.BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL.get(), new ItemStack(ModItems.GEM_SODALITE.get(), 9), new ItemStack(ingot(ModIngots.ASBESTOS), 6), new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        addItemRecipe(ModItems.BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL.get(), new ItemStack(ModItems.CINNABAR.get(), 3), new ItemStack(ingot(ModIngots.ASBESTOS), 5), new ItemStack(net.minecraft.world.item.Items.EMERALD, 3));
        addItemRecipe(ModItems.BEDROCK_ORE_RAD_WASHED_CRYSTAL.get(), new ItemStack(ModItems.BORAX.get(), 3), new ItemStack(ModItems.MOLYSITE.get(), 3), new ItemStack(ModItems.GEM_SODALITE.get(), 9));

        // Blaze Rod
        addItemRecipe(Items.BLAZE_ROD,
            new ItemStack(Items.BLAZE_POWDER, 1),
            new ItemStack(Items.BLAZE_POWDER, 1),
            stack("hbm_m:fire_powder", 1),
            stack("hbm_m:fire_powder", 1));
    }

    // Helper methods
    private static Item ingot(ModIngots ingot) {
        return ModItems.getIngot(ingot).get();
    }

    private static ItemStack stack(String itemId, int count) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(loc);
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
        Item item = BuiltInRegistries.ITEM.get(loc);
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
