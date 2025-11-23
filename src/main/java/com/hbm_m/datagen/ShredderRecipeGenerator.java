package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.ModPowders;
import com.hbm_m.lib.RefStrings;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generates shredder recipes, including block conversions and powder automation.
 */
public final class ShredderRecipeGenerator {

    private ShredderRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer,
                                Function<ItemLike, InventoryChangeTrigger.TriggerInstance> hasItem) {
        registerBasicConversions(writer);
        registerMetalPowders(writer);
        generatePowderProcessing(writer, hasItem);
    }

    private static void registerBasicConversions(Consumer<FinishedRecipe> writer) {
        ShredderRecipeBuilder.shredderRecipe(Items.STONE,
                        new ItemStack(Items.GRAVEL, 1))
                .save(writer, "stone_to_gravel");
        ShredderRecipeBuilder.shredderRecipe(Items.COBBLESTONE,
                        new ItemStack(Items.GRAVEL, 1))
                .save(writer, "cobblestone_to_gravel");
        ShredderRecipeBuilder.shredderRecipe(Items.STONE_BRICKS,
                        new ItemStack(Items.GRAVEL, 1))
                .save(writer, "stone_bricks_to_gravel");
        ShredderRecipeBuilder.shredderRecipe(Items.GRAVEL,
                        new ItemStack(Items.SAND, 1))
                .save(writer, "gravel_to_sand");

        ShredderRecipeBuilder.shredderRecipe(Items.GLOWSTONE,
                        new ItemStack(Items.GLOWSTONE_DUST, 4))
                .save(writer, "glowstone_to_dust");

        ShredderRecipeBuilder.shredderRecipe(Items.BRICKS,
                        new ItemStack(Items.CLAY_BALL, 4))
                .save(writer, "bricks_to_clay");
        ShredderRecipeBuilder.shredderRecipe(Items.BRICK,
                        new ItemStack(Items.CLAY_BALL, 1))
                .save(writer, "brick_to_clay");
    }

    private static void registerMetalPowders(Consumer<FinishedRecipe> writer) {
        ShredderRecipeBuilder.shredderRecipe(Items.IRON_INGOT,
                        new ItemStack(ModItems.getPowders(ModPowders.IRON).get(), 1))
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/iron_ingot_to_powder"));

        ShredderRecipeBuilder.shredderRecipe(Items.GOLD_INGOT,
                        new ItemStack(ModItems.getPowders(ModPowders.GOLD).get(), 1))
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/gold_ingot_to_powder"));
    }

    private static void generatePowderProcessing(Consumer<FinishedRecipe> writer,
                                                 Function<ItemLike, InventoryChangeTrigger.TriggerInstance> hasItem) {
        ShredderRecipeBuilder.shredderRecipe(ModItems.SCRAP.get(), new ItemStack(ModItems.DUST.get(), 1))
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/scrap_to_dust"));

        for (ModIngots ingot : ModIngots.values()) {
            var ingotRegistry = ModItems.getIngot(ingot);
            var powderRegistry = ModItems.getPowder(ingot);
            if (ingotRegistry == null || powderRegistry == null) {
                continue;
            }

            var ingotItem = ingotRegistry.get();
            var powderItem = powderRegistry.get();
            var blockRegistry = ModBlocks.getIngotBlock(ingot);
            String ingotName = ingot.getName();

            ShredderRecipeBuilder.shredderRecipe(ingotItem, new ItemStack(powderItem, 1))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/" + ingotName + "_powder"));

            if (blockRegistry != null) {
                ShredderRecipeBuilder.shredderRecipe(blockRegistry.get().asItem(), new ItemStack(powderItem, 9))
                        .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/" + ingotName + "_block_powder"));
            }

            net.minecraft.data.recipes.SimpleCookingRecipeBuilder.smelting(
                            Ingredient.of(powderItem),
                            net.minecraft.data.recipes.RecipeCategory.MISC,
                            ingotItem,
                            0.35f,
                            200)
                    .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, ingotName + "_powder_smelting"));

            net.minecraft.data.recipes.SimpleCookingRecipeBuilder.blasting(
                            Ingredient.of(powderItem),
                            net.minecraft.data.recipes.RecipeCategory.MISC,
                            ingotItem,
                            0.35f,
                            100)
                    .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, ingotName + "_powder_blasting"));

            ModItems.getTinyPowder(ingot).ifPresent(tinyRegistry -> {
                var tinyItem = tinyRegistry.get();
                ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.MISC, powderItem)
                        .pattern("TTT")
                        .pattern("TTT")
                        .pattern("TTT")
                        .define('T', tinyItem)
                        .unlockedBy("has_" + ingotName + "_powder_tiny", hasItem.apply(tinyItem))
                        .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, ingotName + "_powder_from_tiny"));

                ShapelessRecipeBuilder.shapeless(net.minecraft.data.recipes.RecipeCategory.MISC, tinyItem, 9)
                        .requires(powderItem)
                        .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                        .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, ingotName + "_tiny_from_powder"));
            });
        }

        ShapedRecipeBuilder.shaped(net.minecraft.data.recipes.RecipeCategory.MISC, ModItems.DUST.get())
                .pattern("TTT")
                .pattern("TTT")
                .pattern("TTT")
                .define('T', ModItems.DUST_TINY.get())
                .unlockedBy("has_dust_tiny", hasItem.apply(ModItems.DUST_TINY.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "dust_from_tiny"));

        ShapelessRecipeBuilder.shapeless(net.minecraft.data.recipes.RecipeCategory.MISC, ModItems.DUST_TINY.get(), 9)
                .requires(ModItems.DUST.get())
                .unlockedBy("has_dust", hasItem.apply(ModItems.DUST.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "dust_tiny_from_dust"));
    }
}

