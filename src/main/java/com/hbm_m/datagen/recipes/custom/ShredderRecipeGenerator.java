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
        registerOriginalOreDictGaps(writer);
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

    /**
     * Luecken zum Original-ShredderRecipes: dort erzeugt die OreDict-Schleife (Zeile 39-95) {@code oreX} → {@code dustX} ×2,
     * {@code plateX}/{@code gemX} → {@code dustX} ×1 und {@code blockX} → {@code dustX} ×9; dazu die expliziten Rezepte
     * fuer Holz, Saetzlinge, Lapis, TNT, Quarzstufe, Koepfe, Terrakotta und Wolle (Zeile 147-338).
     * Eintraege, deren Item im Port fehlt, werden uebersprungen.
     */
    private static void registerOriginalOreDictGaps(Consumer<FinishedRecipe> writer) {
        // oreX → dustX ×2 (Original-OreDict; ore_aluminium → Kryolith, diamond_ore und Netherquarz hat der Port schon)
        String[][] ores = {
                {"uranium_powder", "uranium_ore", "uranium_ore_deepslate", "ore_uranium_scorched", "ore_gneiss_uranium", "gneiss_uranium_ore", "ore_gneiss_uranium_scorched", "ore_nether_uranium", "nether_uranium_ore", "ore_nether_uranium_scorched", "ore_sellafield_uranium_scorched"},
                {"thorium_powder", "thorium_ore", "thorium_ore_deepslate"},
                {"plutonium_powder", "ore_nether_plutonium", "nether_plutonium_ore"},
                {"schrabidium_powder", "schrabidium_ore", "schrabidium_ore_gneiss", "schrabidium_ore_nether", "ore_sellafield_schrabidium"},
                {"titanium_powder", "titanium_ore", "titanium_ore_deepslate"},
                {"copper_powder", "ore_copper", "ore_gneiss_copper", "gneiss_copper_ore", "minecraft:copper_ore", "minecraft:deepslate_copper_ore"},
                {"tungsten_powder", "tungsten_ore", "tungsten_ore_deepslate", "ore_nether_tungsten", "nether_tungsten_ore"},
                {"lead_powder", "lead_ore", "lead_ore_deepslate"},
                {"beryllium_powder", "beryllium_ore", "beryllium_ore_deepslate"},
                {"cobalt_powder", "cobalt_ore", "cobalt_ore_deepslate", "ore_nether_cobalt", "nether_cobalt_ore"},
                {"coltan_powder", "ore_coltan", "coltan_ore", "coltan_ore_deepslate"},
                {"asbestos_powder", "asbestos_ore", "asbestos_ore_deepslate", "ore_gneiss_asbestos", "gneiss_asbestos_ore", "ore_basalt_asbestos", "stone_resource_asbestos"},
                {"sulfur", "sulfur_ore", "sulfur_ore_deepslate", "ore_nether_sulfur", "nether_sulfur_ore", "ore_basalt_sulfur", "stone_resource_sulfur"},
                {"niter", "niter_ore", "niter_ore_deepslate"},
                {"fluorite", "fluorite_ore", "fluorite_ore_deepslate", "ore_basalt_fluorite"},
                {"lignite_powder", "lignite_ore", "lignite_ore_deepslate"},
                {"borax", "ore_depth_borax"},
                {"lithium_powder", "lithium_ore", "lithium_ore_deepslate", "ore_gneiss_lithium", "gneiss_lithium_ore"},
                {"zirconium_powder", "ore_depth_zirconium"},
                {"neodymium_powder", "ore_depth_nether_neodymium"},
                {"iron_powder", "ore_gneiss_iron", "gneiss_iron_ore", "minecraft:iron_ore", "minecraft:deepslate_iron_ore"},
                {"gold_powder", "ore_gneiss_gold", "gneiss_gold_ore", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore"},
                {"coal_powder", "minecraft:coal_ore", "minecraft:deepslate_coal_ore", "ore_nether_coal", "nether_coal_ore"},
                {"lapis_powder", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"},
                {"emerald_powder", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore", "ore_sellafield_emerald"},
                {"minecraft:redstone", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"},
        };
        for (String[] row : ores) {
            for (int i = 1; i < row.length; i++) shred(writer, row[i], row[0], 2, "ore");
        }

        // plateX → dustX ×1
        String[][] plates = {
                {"plate_iron", "iron_powder"}, {"plate_gold", "gold_powder"}, {"plate_copper", "copper_powder"},
                {"plate_steel", "steel_powder"}, {"plate_aluminium", "aluminum_powder"}, {"plate_titanium", "titanium_powder"},
                {"plate_lead", "lead_powder"}, {"plate_schrabidium", "schrabidium_powder"}, {"plate_advanced_alloy", "advanced_alloy_powder"},
                {"plate_combine_steel", "combine_steel_powder"}, {"plate_dura_steel", "dura_steel_powder"}, {"plate_desh", "desh_powder"},
                {"plate_bismuth", "bismuth_powder"}, {"plate_polymer", "polymer_powder"}, {"plate_euphemium", "euphemium_powder"},
                {"plate_dineutronium", "dineutronium_powder"},
        };
        for (String[] p : plates) shred(writer, p[0], p[1], 1, "plate");

        // Vanilla-Barren/-Edelsteine ×1 und Speicherbloecke ×9
        shred(writer, "minecraft:copper_ingot", "copper_powder", 1, "ingot");
        shred(writer, "minecraft:diamond", "diamond_powder", 1, "gem");
        shred(writer, "minecraft:emerald", "emerald_powder", 1, "gem");
        shred(writer, "minecraft:lapis_lazuli", "lapis_powder", 1, "gem");
        shred(writer, "lignite", "lignite_powder", 1, "gem");
        String[][] blocks = {
                {"minecraft:iron_block", "iron_powder"}, {"minecraft:gold_block", "gold_powder"}, {"minecraft:copper_block", "copper_powder"},
                {"minecraft:diamond_block", "diamond_powder"}, {"minecraft:emerald_block", "emerald_powder"},
                {"minecraft:lapis_block", "lapis_powder"}, {"minecraft:coal_block", "coal_powder"},
        };
        for (String[] b : blocks) shred(writer, b[0], b[1], 9, "block");

        // Explizite Rezepte (ShredderRecipes.java 147-338)
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(net.minecraft.tags.ItemTags.LOGS), stack("sawdust_powder", 4)).save(writer, "shredder/orig_logs_to_sawdust");
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(net.minecraft.tags.ItemTags.PLANKS), stack("sawdust_powder", 1)).save(writer, "shredder/orig_planks_to_sawdust");
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(net.minecraft.tags.ItemTags.SAPLINGS), new ItemStack(Items.STICK, 1)).save(writer, "shredder/orig_saplings_to_stick");
        shred(writer, "lapis_powder", "cobalt_powder_tiny", 1, "dust");
        shred(writer, "minecraft:tnt", "minecraft:gunpowder", 5, "misc");
        shred(writer, "minecraft:quartz_slab", "quartz_powder", 2, "misc");
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.ZOMBIE_HEAD, Items.PLAYER_HEAD, Items.CREEPER_HEAD),
                stack("biomass", 4)).save(writer, "shredder/orig_skulls_to_biomass");
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(net.minecraft.tags.ItemTags.TERRACOTTA), new ItemStack(Items.CLAY_BALL, 4)).save(writer, "shredder/orig_terracotta_to_clay");
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(net.minecraft.tags.ItemTags.WOOL), new ItemStack(Items.STRING, 4)).save(writer, "shredder/orig_wool_to_string");
    }

    private static Item idItem(String id) {
        net.minecraft.resources.ResourceLocation rl = id.contains(":")
                ? net.minecraft.resources.ResourceLocation.tryParse(id)
                : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, id);
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).orElse(Items.AIR);
    }

    private static ItemStack stack(String id, int count) {
        Item item = idItem(id);
        if (item == Items.AIR) throw new IllegalStateException("Shredder: unbekanntes Item " + id);
        return new ItemStack(item, count);
    }

    /** Ein Rezept {@code in → out ×count}; fehlt Ein- oder Ausgabe im Port, wird es uebersprungen. */
    private static void shred(Consumer<FinishedRecipe> writer, String in, String out, int count, String kind) {
        Item input = idItem(in), output = idItem(out);
        if (input == Items.AIR || output == Items.AIR) return;
        String name = in.substring(in.indexOf(':') + 1);
        ShredderRecipeBuilder.shredderRecipe(input, new ItemStack(output, count)).save(writer, "shredder/orig_" + kind + "_" + name);
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
