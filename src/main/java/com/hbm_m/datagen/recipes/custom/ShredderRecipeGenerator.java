package com.hbm_m.datagen.recipes.custom;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModPowders;
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

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generates shredder recipes, including block conversions and powder automation.
 */
public final class ShredderRecipeGenerator {

    // ✅ ВАШ СПИСОК Порошков!
    private static final Set<String> ENABLED_POWDERS = Set.of(
            "uranium", "u233", "u235", "u238", "th232", "plutonium", "pu238", "pu239", "pu240", "pu241",
            "actinium", "steel", "advanced_alloy", "aluminum", "schrabidium", "saturnite", "lead",
            "gunmetal", "gunsteel", "red_copper", "asbestos", "titanium", "cobalt", "tungsten",
            "starmetal", "beryllium", "bismuth", "polymer", "bakelite", "rubber", "desh", "graphite",
            "phosphorus", "les", "magnetized_tungsten", "combine_steel", "dura_steel", "pc",
            "euphemium", "dineutronium", "electronium", "australium", "solinium", "tantalium",
            "chainsteel", "meteorite", "lanthanium", "neodymium", "niobium", "cerium", "cadmium",
            "caesium", "strontium", "bromide", "tennessine", "zirconium", "arsenic", "iodine",
            "astatine", "americium", "neptunium", "polonium", "technetium", "boron", "schrabidate",
            "schraranium", "au198", "pb209", "ra226", "thorium", "osmiridium", "selenium", "co60",
            "sr90", "am241", "am242", "steel_dusted", "calcium", "graphene", "mox_fuel", "smore",
            "schrabidium_fuel", "uranium_fuel", "thorium_fuel", "plutonium_fuel", "neptunium_fuel",
            "americium_fuel", "bismuth_bronze", "arsenic_bronze", "crystalline", "mud", "silicon",
            "fiberglass", "ceramic", "pu_mix", "am_mix", "pet", "ferrouranium", "pvc", "biorubber",
            "cdalloy", "bscco"
    );

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
        ShredderRecipeBuilder.shredderRecipe(Items.COAL,
                        new ItemStack(ModItems.POWDER_COAL.get(), 1))
                .save(writer, "coal_to_powder");
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
        // ✅ ПРОВЕРКА NULL для ModPowders!
        if (ModItems.getPowders(ModPowders.IRON) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.IRON_INGOT,
                            new ItemStack(ModItems.getPowders(ModPowders.IRON).get(), 1))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/iron_ingot_to_powder"));
        }

        if (ModItems.getPowders(ModPowders.GOLD) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.GOLD_INGOT,
                            new ItemStack(ModItems.getPowders(ModPowders.GOLD).get(), 1))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/gold_ingot_to_powder"));
        }

        // ✅ Остальные с проверками
        if (ModItems.getPowders(ModPowders.COAL) != null) {
            if (ModItems.POWDER_COAL_SMALL != null) {
                ShredderRecipeBuilder.shredderRecipe(ModItems.getPowders(ModPowders.COAL).get(),
                                new ItemStack(ModItems.POWDER_COAL_SMALL.get(), 9))
                        .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/coal_to_small_powder"));
            }
            ShredderRecipeBuilder.shredderRecipe(Items.COAL,
                            new ItemStack(ModItems.getPowders(ModPowders.COAL).get(), 1))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/coal_to_powder"));
        }
    }

    private static void generatePowderProcessing(Consumer<FinishedRecipe> writer,
                                                 Function<ItemLike, InventoryChangeTrigger.TriggerInstance> hasItem) {
        ShredderRecipeBuilder.shredderRecipe(ModItems.SCRAP.get(), new ItemStack(ModItems.DUST.get(), 1))
                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/scrap_to_dust"));

        // ✅ ЦИКЛ ТОЛЬКО по ВАШЕМУ списку ENABLED_POWDERS!
        for (String powderName : ENABLED_POWDERS) {
            ModIngots ingot = ModIngots.byName(powderName).orElse(null);
            if (ingot == null) continue;

            var ingotRegistry = ModItems.getIngot(ingot);
            var powderRegistry = ModItems.getPowder(ingot);

            // Если нет предмета слитка или порошка - пропускаем
            if (ingotRegistry == null || powderRegistry == null) {
                continue;
            }

            var ingotItem = ingotRegistry.get();
            var powderItem = powderRegistry.get();
            String ingotName = ingot.getName();

            // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
            // Безопасно получаем блок. Если его нет - будет null, но без краша.
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.level.block.Block> blockRegistry = null;

            if (ModBlocks.hasIngotBlock(ingot)) {
                blockRegistry = ModBlocks.getIngotBlock(ingot);
            }
            // -----------------------

            // 1. Рецепт Шреддера: Слиток → Порошок (Всегда есть, если мы тут)
            ShredderRecipeBuilder.shredderRecipe(ingotItem, new ItemStack(powderItem, 1))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/" + ingotName + "_powder"));

            // 2. Рецепт Шреддера: Блок → Порошки (ТОЛЬКО ЕСЛИ БЛОК СУЩЕСТВУЕТ)
            if (blockRegistry != null) {
                ShredderRecipeBuilder.shredderRecipe(blockRegistry.get().asItem(), new ItemStack(powderItem, 9))
                        .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "shredder/" + ingotName + "_block_powder"));
            }

            // ... Дальше ваш код плавки (smelting/blasting/tiny) без изменений ...

            // Плавка порошка → слиток
            net.minecraft.data.recipes.SimpleCookingRecipeBuilder.smelting(
                            Ingredient.of(powderItem),
                            net.minecraft.data.recipes.RecipeCategory.MISC,
                            ingotItem,
                            0.35f,
                            200)
                    .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, ingotName + "_powder_smelting"));

            // Доменная печь
            net.minecraft.data.recipes.SimpleCookingRecipeBuilder.blasting(
                            Ingredient.of(powderItem),
                            net.minecraft.data.recipes.RecipeCategory.MISC,
                            ingotItem,
                            0.35f,
                            100)
                    .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                    .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, ingotName + "_powder_blasting"));

            // Крафт из крошечных порошков
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

        // Общие рецепты пыли
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
