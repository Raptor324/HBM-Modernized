package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generates shredder recipes, including block conversions and powder automation.
 *
 * <p>Использует {@code save(writer, "id")} из {@link BaseRecipeBuilder} и статический
 * {@link BaseRecipeBuilder#resLoc(String)} для ванильных билдеров — Stonecutter-блоки
 * с {@code ResourceLocation} больше не нужны.</p>
 */
public final class ShredderRecipeGenerator {

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
        registerDecoPipes(writer);
        registerSteelDeco(writer);
        registerMetalPowders(writer);
        registerModRawOreRecipes(writer);
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

        // Restport: Original "Sellafite scrapping"
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_SLAKED.get().asItem(), new ItemStack(Items.GRAVEL, 1)).save(writer, "sellafield_slaked_to_gravel");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_0.get().asItem(), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), 1)).save(writer, "sellafield_0_to_scrap_nuclear");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_1.get().asItem(), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), 2)).save(writer, "sellafield_1_to_scrap_nuclear");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_2.get().asItem(), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), 3)).save(writer, "sellafield_2_to_scrap_nuclear");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_3.get().asItem(), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), 5)).save(writer, "sellafield_3_to_scrap_nuclear");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_4.get().asItem(), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), 7)).save(writer, "sellafield_4_to_scrap_nuclear");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.SELLAFIELD_5.get().asItem(), new ItemStack(ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP), 15)).save(writer, "sellafield_5_to_scrap_nuclear");
        // Original: jeder Wackelkopf (Meta = BobbleType) -> 1 scrap_plastic mit Meta BobbleType.scrap
        for (com.hbm_m.block.decorations.TrinketTypes.BobbleType type : com.hbm_m.block.decorations.TrinketTypes.BobbleType.values()) {
            ItemStack bobble = com.hbm_m.item.TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), type.ordinal());
            ShredderRecipeBuilder.shredderRecipe(net.minecraftforge.common.crafting.PartialNBTIngredient.of(bobble.getItem(), bobble.getTag()),
                            new ItemStack(ModItems.scrapPlastic(type.scrap), 1))
                    .save(writer, "bobblehead_" + type.name().toLowerCase(java.util.Locale.ROOT) + "_to_scrap_plastic");
        }

        ShredderRecipeBuilder.shredderRecipe(Items.GLOWSTONE,
                        new ItemStack(Items.GLOWSTONE_DUST, 4))
                .save(writer, "glowstone_to_dust");

        ShredderRecipeBuilder.shredderRecipe(Items.BRICKS,
                        new ItemStack(Items.CLAY_BALL, 4))
                .save(writer, "bricks_to_clay");
        ShredderRecipeBuilder.shredderRecipe(Items.BRICK,
                        new ItemStack(Items.CLAY_BALL, 1))
                .save(writer, "brick_to_clay");
        if (ModMaterialItems.get(ModMaterials.LIMESTONE, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(ModItems.LIMESTONE.get(),
                            ModMaterialItems.get(ModMaterials.LIMESTONE, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/limestone_to_powder");
        }
    }

    /** ShredderRecipes: boxcar -> 32 Stahlstaub, steel_poles -> 2 kleiner Stahlstaub, steel_corner -> 18 kleiner Stahlstaub. */
    private static void registerSteelDeco(Consumer<FinishedRecipe> writer) {
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.BOXCAR.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER), 32)
                .save(writer, "shredder/boxcar");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_POLE.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 2)
                .save(writer, "shredder/steel_pole");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_CORNER.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 18)
                .save(writer, "shredder/steel_corner");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_SCAFFOLD.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 4)
                .save(writer, "shredder/steel_scaffold");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_SCAFFOLD_RED.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 4)
                .save(writer, "shredder/steel_scaffold_red");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_SCAFFOLD_WHITE.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 4)
                .save(writer, "shredder/steel_scaffold_white");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_SCAFFOLD_YELLOW.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 4)
                .save(writer, "shredder/steel_scaffold_yellow");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.DUNGEON_CHAIN.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 1)
                .save(writer, "shredder/dungeon_chain");
        ShredderRecipeBuilder.shredderRecipe(ModBlocks.STEEL_GRATE.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER_TINY), 3)
                .save(writer, "shredder/steel_grate");
    }

    /** ShredderRecipes: alle 24 deco_pipe-Varianten -> 1 Stahlstaub. */
    private static void registerDecoPipes(Consumer<FinishedRecipe> writer) {
        for (var pipe : java.util.List.of(ModBlocks.DECO_PIPE, ModBlocks.DECO_PIPE_RUSTED, ModBlocks.DECO_PIPE_GREEN, ModBlocks.DECO_PIPE_GREEN_RUSTED, ModBlocks.DECO_PIPE_RED, ModBlocks.DECO_PIPE_MARKED, ModBlocks.DECO_PIPE_RIM, ModBlocks.DECO_PIPE_RIM_RUSTED, ModBlocks.DECO_PIPE_RIM_GREEN, ModBlocks.DECO_PIPE_RIM_GREEN_RUSTED, ModBlocks.DECO_PIPE_RIM_RED, ModBlocks.DECO_PIPE_RIM_MARKED, ModBlocks.DECO_PIPE_FRAMED, ModBlocks.DECO_PIPE_FRAMED_RUSTED, ModBlocks.DECO_PIPE_FRAMED_GREEN, ModBlocks.DECO_PIPE_FRAMED_GREEN_RUSTED, ModBlocks.DECO_PIPE_FRAMED_RED, ModBlocks.DECO_PIPE_FRAMED_MARKED, ModBlocks.DECO_PIPE_QUAD, ModBlocks.DECO_PIPE_QUAD_RUSTED, ModBlocks.DECO_PIPE_QUAD_GREEN, ModBlocks.DECO_PIPE_QUAD_GREEN_RUSTED, ModBlocks.DECO_PIPE_QUAD_RED, ModBlocks.DECO_PIPE_QUAD_MARKED)) {
            ShredderRecipeBuilder.shredderRecipe(pipe.get().asItem(), ModMaterialItems.get(ModMaterials.STEEL, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/" + pipe.getId().getPath());
        }
    }

    private static void registerMetalPowders(Consumer<FinishedRecipe> writer) {
        //  Передаём powder как RegistrySupplier — .get() вызывается лениво на стороне билдера,
        //  а не остаётся в коде генератора. Проверки на null сохранены (предмет может быть
        //  отключён в конфиге).
        if (ModMaterialItems.get(ModMaterials.IRON, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.IRON_INGOT, ModMaterialItems.get(ModMaterials.IRON, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/iron_ingot_to_powder");
        }
        if (ModMaterialItems.get(ModMaterials.IRON, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.RAW_IRON, ModMaterialItems.get(ModMaterials.IRON, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/raw_iron_to_powder");
        }
        if (ModMaterialItems.get(ModMaterials.IRON, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.RAW_IRON_BLOCK, ModMaterialItems.get(ModMaterials.IRON, MaterialShape.POWDER), 8)
                    .save(writer, "shredder/raw_iron_block_to_powder");
        }

        if (ModMaterialItems.get(ModMaterials.GOLD, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.GOLD_INGOT, ModMaterialItems.get(ModMaterials.GOLD, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/gold_ingot_to_powder");
        }
        if (ModMaterialItems.get(ModMaterials.GOLD, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.RAW_GOLD, ModMaterialItems.get(ModMaterials.GOLD, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/raw_gold_to_powder");
        }
        if (ModMaterialItems.get(ModMaterials.GOLD, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.RAW_GOLD_BLOCK, ModMaterialItems.get(ModMaterials.GOLD, MaterialShape.POWDER), 9)
                    .save(writer, "shredder/raw_gold_block_to_powder");
        }

        if (ModMaterialItems.get(ModMaterials.COPPER, MaterialShape.POWDER) != null) {
            ShredderRecipeBuilder.shredderRecipe(Items.RAW_COPPER, ModMaterialItems.get(ModMaterials.COPPER, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/raw_copper_to_powder");
            ShredderRecipeBuilder.shredderRecipe(Items.RAW_COPPER_BLOCK, ModMaterialItems.get(ModMaterials.COPPER, MaterialShape.POWDER), 9)
                    .save(writer, "shredder/raw_copper_block_to_powder");
        }

        //  Остальные с проверками
        if (ModMaterialItems.get(ModMaterials.COAL, MaterialShape.POWDER) != null) {
            if (ModMaterialItems.get(ModMaterials.COAL, MaterialShape.POWDER_TINY) != null) {
                ShredderRecipeBuilder.shredderRecipe(
                                ModMaterialItems.get(ModMaterials.COAL, MaterialShape.POWDER), ModMaterialItems.get(ModMaterials.COAL, MaterialShape.POWDER_TINY), 9)
                        .save(writer, "shredder/coal_to_small_powder");
            }
            ShredderRecipeBuilder.shredderRecipe(Items.COAL, ModMaterialItems.get(ModMaterials.COAL, MaterialShape.POWDER), 1)
                    .save(writer, "shredder/coal_to_powder");
        }
    }

    /*
     * Raw mod ores → matching powder (1.7.10 auto-generated these from ore-dict "ore*" entries).
     */
    private static void registerModRawOreRecipes(Consumer<FinishedRecipe> writer) {
        registerRawToPowder(writer, ModItems.URANIUM_RAW, ModMaterials.URANIUM);
        registerRawToPowder(writer, ModItems.LEAD_RAW, ModMaterials.LEAD);
        registerRawToPowder(writer, ModItems.BERYLLIUM_RAW, ModMaterials.BERYLLIUM);
        registerRawToPowder(writer, ModItems.ALUMINUM_RAW, ModMaterials.ALUMINUM);
        registerRawToPowder(writer, ModItems.TITANIUM_RAW, ModMaterials.TITANIUM);
        registerRawToPowder(writer, ModItems.THORIUM_RAW, ModMaterials.THORIUM);
        registerRawToPowder(writer, ModItems.COBALT_RAW, ModMaterials.COBALT);
        registerRawToPowder(writer, ModItems.TUNGSTEN_RAW, ModMaterials.TUNGSTEN);
    }

    private static void registerRawToPowder(Consumer<FinishedRecipe> writer,
                                            RegistrySupplier<Item> raw,
                                            ModMaterials ingot) {
        var powderRegistry = ModMaterialItems.get(ingot, MaterialShape.POWDER);
        if (raw == null || powderRegistry == null) {
            return;
        }
        String name = ingot.getId();
        ShredderRecipeBuilder.shredderRecipe(raw.get(), new ItemStack(powderRegistry.get(), 1))
                .save(writer, "shredder/raw_" + name + "_to_powder");
    }

    private static void generatePowderProcessing(Consumer<FinishedRecipe> writer,
                                                 Function<ItemLike, InventoryChangeTrigger.TriggerInstance> hasItem) {
        ShredderRecipeBuilder.shredderRecipe(ModMaterialItems.item(ModMaterials.SCRAP, MaterialShape.SCRAP), new ItemStack(ModItems.DUST.get(), 1))
                .save(writer, "shredder/scrap_to_dust");

        //  ЦИКЛ ТОЛЬКО по ВАШЕМУ списку ENABLED_POWDERS!
        for (String powderName : ENABLED_POWDERS) {
            ModMaterials ingot = ModMaterials.byId(powderName);
            if (ingot == null) continue;

            var ingotRegistry = ModMaterialItems.get(ModMaterialItems.ingotMaterial(ingot), MaterialShape.INGOT);
            var powderRegistry = ModMaterialItems.get(ingot, MaterialShape.POWDER);

            // Если нет предмета слитка или порошка - пропускаем
            if (ingotRegistry == null || powderRegistry == null) {
                continue;
            }

            var ingotItem = ingotRegistry.get();
            var powderItem = powderRegistry.get();
            String ingotName = ingot.getId();

            // Безопасно получаем блок. Если его нет - будет null, но без краша.
            dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.Block> blockRegistry = null;

            if (ModBlocks.hasIngotBlock(ingot)) {
                blockRegistry = ModBlocks.getIngotBlock(ingot);
            }

            // 1. Рецепт Шреддера: Слиток → Порошок (Всегда есть, если мы тут)
            ShredderRecipeBuilder.shredderRecipe(ingotItem, new ItemStack(powderItem, 1))
                    .save(writer, "shredder/" + ingotName + "_powder");

            // 2. Рецепт Шреддера: Блок → Порошки (ТОЛЬКО ЕСЛИ БЛОК СУЩЕСТВУЕТ)
            if (blockRegistry != null) {
                ShredderRecipeBuilder.shredderRecipe(blockRegistry.get().asItem(), new ItemStack(powderItem, 9))
                        .save(writer, "shredder/" + ingotName + "_block_powder");
            }

            // Плавка порошка → слиток
            SimpleCookingRecipeBuilder.smelting(
                            Ingredient.of(powderItem),
                            RecipeCategory.MISC,
                            ingotItem,
                            0.35f,
                            200)
                    .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                    .save(writer, BaseRecipeBuilder.resLoc(ingotName + "_powder_smelting"));

            // Доменная печь
            SimpleCookingRecipeBuilder.blasting(
                            Ingredient.of(powderItem),
                            RecipeCategory.MISC,
                            ingotItem,
                            0.35f,
                            100)
                    .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                    .save(writer, BaseRecipeBuilder.resLoc(ingotName + "_powder_blasting"));

            // Крафт из крошечных порошков (POWDER_TINY — Item напрямую, null если формы нет)
            Item tinyItem = ModMaterialItems.item(ingot, MaterialShape.POWDER_TINY);
            if (tinyItem != null) {
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, powderItem)
                        .pattern("TTT")
                        .pattern("TTT")
                        .pattern("TTT")
                        .define('T', tinyItem)
                        .unlockedBy("has_" + ingotName + "_powder_tiny", hasItem.apply(tinyItem))
                        .save(writer, BaseRecipeBuilder.resLoc(ingotName + "_powder_from_tiny"));

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, tinyItem, 9)
                        .requires(powderItem)
                        .unlockedBy("has_" + ingotName + "_powder", hasItem.apply(powderItem))
                        .save(writer, BaseRecipeBuilder.resLoc(ingotName + "_tiny_from_powder"));
            }
        }

        // Общие рецепты пыли
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DUST.get())
                .pattern("TTT")
                .pattern("TTT")
                .pattern("TTT")
                .define('T', ModItems.DUST_TINY.get())
                .unlockedBy("has_dust_tiny", hasItem.apply(ModItems.DUST_TINY.get()))
                .save(writer, BaseRecipeBuilder.resLoc("dust_from_tiny"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.DUST_TINY.get(), 9)
                .requires(ModItems.DUST.get())
                .unlockedBy("has_dust", hasItem.apply(ModItems.DUST.get()))
                .save(writer, BaseRecipeBuilder.resLoc("dust_tiny_from_dust"));
    }
}
//?}
