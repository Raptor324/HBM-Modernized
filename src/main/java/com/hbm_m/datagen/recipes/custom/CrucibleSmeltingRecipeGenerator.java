package com.hbm_m.datagen.recipes.custom;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.crafting.Ingredient;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов тигель-плавки ({@code hbm_m:crucible_smelting}).
 *
 * <p>Порт {@code CrucibleSmeltingRecipes.registerDefaults()} (删除анного статического реестра):
 * тот же набор forge-тегов слитков/руд, те же материальные типы, те же объёмы в mB.
 * Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 *
 * <p><b>Соответствие оригиналу:</b> каждый {@code ingotTag("ingots/x", MaterialType.X)}
 * переносится как {@code crucibleSmelting("forge:ingots/x", X, MB_PER_INGOT)}; рудные теги
 * удваивают объём (как в оригинале: {@code MB_PER_INGOT * 2}); алмазные/сырьевые предметы
 * (уголь, уголль, редстоун, nugget'ы) дают {@code MB_PER_NUGGET}; блоки-руды дают
 * {@code MB_PER_INGOT * 2}.</p>
 */
public final class CrucibleSmeltingRecipeGenerator {

    private CrucibleSmeltingRecipeGenerator() {}

    /** Ориг. QUART.q(1) = 162 кванта = 2250 mB (побочка «камень» рудных плавок). */
    private static final int MB_PER_QUART = 2250;

    public static void generate(Consumer<FinishedRecipe> writer) {
        // ═══════════════════════════════════════════════════════════════════
        // СЛИТКИ (forge:ingots/<material>) — MaterialStack.MB_PER_INGOT
        // ═══════════════════════════════════════════════════════════════════
        ingot(writer, "iron",        MaterialType.IRON);
        ingot(writer, "gold",        MaterialType.GOLD);
        ingot(writer, "copper",      MaterialType.COPPER);
        ingot(writer, "titanium",    MaterialType.TITANIUM);
        ingot(writer, "aluminum",    MaterialType.ALUMINIUM);
        ingot(writer, "aluminium",   MaterialType.ALUMINIUM);
        ingot(writer, "tungsten",    MaterialType.TUNGSTEN);
        ingot(writer, "zirconium",   MaterialType.ZIRCONIUM);
        ingot(writer, "osmiridium",  MaterialType.OSMIRIDIUM);
        ingot(writer, "steel",       MaterialType.STEEL);
        // Теги обязаны совпадать с генерируемыми ModItemTagProvider по ModMaterials.getId():
        // advanced_alloy (не alloy), starmetal (не star_metal), combine_steel (не cmb).
        ingot(writer, "advanced_alloy", MaterialType.ALLOY);
        ingot(writer, "dura_steel",  MaterialType.DURA_STEEL);
        ingot(writer, "desh",        MaterialType.DESH);
        ingot(writer, "starmetal",   MaterialType.STAR_METAL);
        ingot(writer, "tcalloy",     MaterialType.TCALLOY);
        ingot(writer, "cdalloy",     MaterialType.CDALLOY);
        ingot(writer, "combine_steel", MaterialType.CMB);
        ingot(writer, "bscco",       MaterialType.BSCCO);
        ingot(writer, "schrabidium", MaterialType.SCHRABIDIUM);
        ingot(writer, "bbronze",     MaterialType.BBRONZE);
        ingot(writer, "abronze",     MaterialType.ABRONZE);
        ingot(writer, "saturnite",   MaterialType.SATURNITE);
        ingot(writer, "lead",        MaterialType.LEAD);
        ingot(writer, "bismuth",     MaterialType.BISMUTH);
        ingot(writer, "beryllium",   MaterialType.BERYLLIUM);
        ingot(writer, "cobalt",      MaterialType.COBALT);
        ingot(writer, "nickel",      MaterialType.NICKEL);
        ingot(writer, "u238",        MaterialType.URANIUM238);
        // Ориг. MAT_URANIUM: слиток урана плавится, как и в 1.7.10.
        ingot(writer, "uranium",     MaterialType.URANIUM);
        ingot(writer, "strontium",   MaterialType.STRONTIUM);
        ingot(writer, "calcium",     MaterialType.CALCIUM);
        ingot(writer, "mud",         MaterialType.MUD);

        // ═══════════════════════════════════════════════════════════════════
        // РУДЫ (forge:ores/<material>) — порт MatDistribution.registerOre:
        // 2 слитка основного + побочные (титан/свинец/золото/натрий) + камень
        // (QUART.q(1) = 162 кванта = 2250 mB). Урановая руда — отдельным блоком ниже.
        // ═══════════════════════════════════════════════════════════════════
        oreTagMulti(writer, "byproduct_vanilla_ore_iron",     "forge:ores/iron",      new MaterialStack(MaterialType.IRON,      MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.TITANIUM,  MaterialStack.MB_PER_NUGGET * 3),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_gold",     "forge:ores/gold",      new MaterialStack(MaterialType.GOLD,      MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.LEAD,      MaterialStack.MB_PER_NUGGET),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_copper",   "forge:ores/copper",    new MaterialStack(MaterialType.COPPER,    MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_titanium", "forge:ores/titanium",  new MaterialStack(MaterialType.TITANIUM,  MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.IRON,      MaterialStack.MB_PER_NUGGET * 3),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_aluminum", "forge:ores/aluminum",  new MaterialStack(MaterialType.ALUMINIUM, MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.SODIUM,    MaterialStack.MB_PER_NUGGET * 3),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_aluminium","forge:ores/aluminium", new MaterialStack(MaterialType.ALUMINIUM, MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.SODIUM,    MaterialStack.MB_PER_NUGGET * 3),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_tungsten", "forge:ores/tungsten",  new MaterialStack(MaterialType.TUNGSTEN,  MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_lead",     "forge:ores/lead",      new MaterialStack(MaterialType.LEAD,      MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.GOLD,      MaterialStack.MB_PER_NUGGET),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_beryllium","forge:ores/beryllium", new MaterialStack(MaterialType.BERYLLIUM, MaterialStack.MB_PER_INGOT * 2),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_cobalt",   "forge:ores/cobalt",    new MaterialStack(MaterialType.COBALT,    MaterialStack.MB_PER_INGOT),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        oreTagMulti(writer, "byproduct_vanilla_ore_redstone", "forge:ores/redstone",  new MaterialStack(MaterialType.REDSTONE,  MaterialStack.MB_PER_INGOT * 4),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));
        // Угольная руда: CARBON GEM.q(3) = 216 квантов = 3000 mB + камень.
        oreTagMulti(writer, "byproduct_vanilla_ore_coal",     "forge:ores/coal",      new MaterialStack(MaterialType.CARBON,    MaterialStack.MB_PER_INGOT * 3),
                                                              new MaterialStack(MaterialType.STONE,     MB_PER_QUART));

        // ═══════════════════════════════════════════════════════════════════
        // ВАНИЛЬНЫЕ ФИКСИРОВАННЫЕ ЗАПИСИ (порт MatDistribution "vanilla crap").
        // ═══════════════════════════════════════════════════════════════════
        nuggetItem(writer, "vanilla_stone",        Blocks.STONE.asItem(),      MaterialType.STONE,    MaterialStack.MB_PER_INGOT * 9);
        nuggetItem(writer, "vanilla_cobblestone",  Blocks.COBBLESTONE.asItem(),MaterialType.STONE,    MaterialStack.MB_PER_INGOT * 9);
        nuggetItem(writer, "vanilla_obsidian",     Blocks.OBSIDIAN.asItem(),   MaterialType.OBSIDIAN, MaterialStack.MB_PER_INGOT * 9);
        // Рельсы: INGOT.q(6,16) = 375 mB железа на рельс.
        nuggetItem(writer, "vanilla_rail",         Items.RAIL,           MaterialType.IRON, 375);
        // Электрический рельс: GOLD INGOT.q(6,6) + REDSTONE DUST.q(1,6).
        nuggetMulti(writer, "vanilla_powered_rail", Items.POWERED_RAIL,
                new MaterialStack(MaterialType.GOLD,     MaterialStack.MB_PER_INGOT),
                new MaterialStack(MaterialType.REDSTONE, MaterialStack.MB_PER_INGOT / 6));
        // Рельс с детектором: IRON INGOT.q(6,6) + REDSTONE DUST.q(1,6).
        nuggetMulti(writer, "vanilla_detector_rail", Items.DETECTOR_RAIL,
                new MaterialStack(MaterialType.IRON,     MaterialStack.MB_PER_INGOT),
                new MaterialStack(MaterialType.REDSTONE, MaterialStack.MB_PER_INGOT / 6));
        // Вагонетка: IRON INGOT.q(5) = 5000 mB.
        nuggetItem(writer, "vanilla_minecart",     Items.MINECART, MaterialType.IRON, MaterialStack.MB_PER_INGOT * 5);
        // Сырьевые руды (1.17+; 1:1 с печью — по слитку на предмет).
        nuggetItem(writer, "vanilla_raw_iron",     Items.RAW_IRON,   MaterialType.IRON,   MaterialStack.MB_PER_INGOT);
        nuggetItem(writer, "vanilla_raw_copper",   Items.RAW_COPPER, MaterialType.COPPER, MaterialStack.MB_PER_INGOT);
        nuggetItem(writer, "vanilla_raw_gold",     Items.RAW_GOLD,   MaterialType.GOLD,   MaterialStack.MB_PER_INGOT);
        // Блоки хранения (ориг. автоген blockIron/blockGold/blockCopper = 9 слитков) и железный самородок.
        nuggetItem(writer, "vanilla_iron_block",   Blocks.IRON_BLOCK.asItem(),   MaterialType.IRON,   MaterialStack.MB_PER_INGOT * 9);
        nuggetItem(writer, "vanilla_gold_block",   Blocks.GOLD_BLOCK.asItem(),   MaterialType.GOLD,   MaterialStack.MB_PER_INGOT * 9);
        nuggetItem(writer, "vanilla_copper_block", Blocks.COPPER_BLOCK.asItem(), MaterialType.COPPER, MaterialStack.MB_PER_INGOT * 9);
        nuggetItem(writer, "vanilla_iron_nugget",  Items.IRON_NUGGET, MaterialType.IRON, MaterialStack.MB_PER_NUGGET);

        // ═══════════════════════════════════════════════════════════════════
        // МОДОВЫЕ ФИКСИРОВАННЫЕ ЗАПИСИ (порт MatDistribution "castables" и пр.).
        // ═══════════════════════════════════════════════════════════════════
        // Литейные изделия (ориг. INGOT.q(3)/q(4), BLOCK.q(3)).
        nuggetModItemAmount(writer, "castable_blade_titanium",  "blade_titanium",  MaterialType.TITANIUM, MaterialStack.MB_PER_INGOT * 3);
        nuggetModItemAmount(writer, "castable_blade_tungsten",  "blade_tungsten",  MaterialType.TUNGSTEN, MaterialStack.MB_PER_INGOT * 3);
        nuggetModItemAmount(writer, "castable_blades_steel",    "blades_steel",    MaterialType.STEEL,    MaterialStack.MB_PER_INGOT * 4);
        nuggetModItemAmount(writer, "castable_blades_titanium", "blades_titanium", MaterialType.TITANIUM, MaterialStack.MB_PER_INGOT * 4);
        nuggetModItemAmount(writer, "castable_pipes_steel",     "pipes_steel",     MaterialType.STEEL,    MaterialStack.MB_PER_INGOT * 27);
        // Штампы (плоские, ориг. INGOT.q(3) = 3000 mB).
        nuggetModItemAmount(writer, "castable_stamp_stone_flat",    "stamp_stone_flat",    MaterialType.STONE,    MaterialStack.MB_PER_INGOT * 3);
        nuggetModItemAmount(writer, "castable_stamp_iron_flat",     "stamp_iron_flat",     MaterialType.IRON,     MaterialStack.MB_PER_INGOT * 3);
        nuggetModItemAmount(writer, "castable_stamp_steel_flat",    "stamp_steel_flat",    MaterialType.STEEL,    MaterialStack.MB_PER_INGOT * 3);
        nuggetModItemAmount(writer, "castable_stamp_titanium_flat", "stamp_titanium_flat", MaterialType.TITANIUM, MaterialStack.MB_PER_INGOT * 3);
        nuggetModItemAmount(writer, "castable_stamp_obsidian_flat", "stamp_obsidian_flat", MaterialType.OBSIDIAN, MaterialStack.MB_PER_INGOT * 3);
        // Гильзы (ориг. PLATE.q(1,4) = 250 mB / PLATE.q(1,2) = 500 mB).
        nuggetModItemAmount(writer, "castable_casing_small",       "casing_small",       MaterialType.GUNMETAL,    MaterialStack.MB_PER_INGOT / 4);
        nuggetModItemAmount(writer, "castable_casing_large",       "casing_large",       MaterialType.GUNMETAL,    MaterialStack.MB_PER_INGOT / 2);
        nuggetModItemAmount(writer, "castable_casing_small_steel", "casing_small_steel", MaterialType.WEAPONSTEEL, MaterialStack.MB_PER_INGOT / 4);
        nuggetModItemAmount(writer, "castable_casing_large_steel", "casing_large_steel", MaterialType.WEAPONSTEEL, MaterialStack.MB_PER_INGOT / 2);
        // Криолитовый кусок: ALUMINIUM INGOT.q(1) + SODIUM INGOT.q(1).
        nuggetModMulti(writer, "castable_cryolite_chunk", "cryolite_chunk",
                new MaterialStack(MaterialType.ALUMINIUM, MaterialStack.MB_PER_INGOT),
                new MaterialStack(MaterialType.SODIUM,    MaterialStack.MB_PER_INGOT));
        // Известняк: FLUX DUST.q(10) = 10000 mB (ориг. stone_resource limestone).
        nuggetModItemAmount(writer, "castable_limestone", "stone_resource_limestone", MaterialType.FLUX, MaterialStack.MB_PER_INGOT * 10);
        // Золы: CARBON NUGGET.q(1)/q(2)/q(1) (ориг. powder_ash WOOD/COAL/MISC).
        nuggetModItemAmount(writer, "carbon_ash_wood", "ash_wood", MaterialType.CARBON, MaterialStack.MB_PER_NUGGET);
        nuggetModItemAmount(writer, "carbon_ash_coal", "ash_coal", MaterialType.CARBON, MaterialStack.MB_PER_NUGGET * 2);
        nuggetModItemAmount(writer, "carbon_ash_misc", "ash_misc", MaterialType.CARBON, MaterialStack.MB_PER_NUGGET);

        // ═══════════════════════════════════════════════════════════════════
        // ФРАГМЕНТЫ БЕДРОКОВОЙ РУДЫ (форма FRAGMENT = 8 квантов = 1 самородок).
        // Оригинал: автоген bedrockorefragment<Mat> → smeltsInto; здесь —
        // только материалы, существующие в MaterialType.
        // ═══════════════════════════════════════════════════════════════════
        nuggetModItem(writer, "fragment_iron",      "bedrock_ore_fragment_iron",      MaterialType.IRON);
        nuggetModItem(writer, "fragment_gold",      "bedrock_ore_fragment_gold",      MaterialType.GOLD);
        nuggetModItem(writer, "fragment_copper",    "bedrock_ore_fragment_copper",    MaterialType.COPPER);
        nuggetModItem(writer, "fragment_titanium",  "bedrock_ore_fragment_titanium",  MaterialType.TITANIUM);
        nuggetModItem(writer, "fragment_tungsten",  "bedrock_ore_fragment_tungsten",  MaterialType.TUNGSTEN);
        nuggetModItem(writer, "fragment_aluminium", "bedrock_ore_fragment_aluminium", MaterialType.ALUMINIUM);
        nuggetModItem(writer, "fragment_lead",      "bedrock_ore_fragment_lead",      MaterialType.LEAD);
        nuggetModItem(writer, "fragment_bismuth",   "bedrock_ore_fragment_bismuth",   MaterialType.BISMUTH);
        nuggetModItem(writer, "fragment_beryllium", "bedrock_ore_fragment_beryllium", MaterialType.BERYLLIUM);
        nuggetModItem(writer, "fragment_cobalt",    "bedrock_ore_fragment_cobalt",    MaterialType.COBALT);
        nuggetModItem(writer, "fragment_zirconium", "bedrock_ore_fragment_zirconium", MaterialType.ZIRCONIUM);
        nuggetModItem(writer, "fragment_sodium",    "bedrock_ore_fragment_sodium",    MaterialType.SODIUM);
        nuggetModItem(writer, "fragment_strontium", "bedrock_ore_fragment_strontium", MaterialType.STRONTIUM);
        nuggetModItem(writer, "fragment_redstone",  "bedrock_ore_fragment_redstone",  MaterialType.REDSTONE);
        nuggetModItem(writer, "fragment_u238",      "bedrock_ore_fragment_u238",      MaterialType.URANIUM238);
        nuggetModItem(writer, "fragment_tc99",      "bedrock_ore_fragment_tc99",      MaterialType.TECHNETIUM);
        // Алмаз: ориг. MAT_DIAMOND → MAT_CARBON 1:1 (конверсия), фрагмент = 111 mB углерода.
        nuggetModItem(writer, "fragment_diamond",   "bedrock_ore_fragment_diamond",   MaterialType.CARBON);

        // ═══════════════════════════════════════════════════════════════════
        // Алмазные/сырьевые предметы (входы для алиирования).
        // Ориг. конверсии: уголь → CARBON 2:1 (gem 72 кв → 500 mB), древесный
        // уголь NUGGET.q(3) = 333 mB.
        // ═══════════════════════════════════════════════════════════════════
        nuggetItem(writer, "carbon_coal",       Items.COAL,                        MaterialType.CARBON, 500);
        nuggetItem(writer, "carbon_charcoal",   Items.CHARCOAL,                    MaterialType.CARBON, MaterialStack.MB_PER_NUGGET * 3);
        nuggetItem(writer, "redstone",          Items.REDSTONE,                    MaterialType.REDSTONE);
        nuggetModItem(writer, "nugget_arsenic",    "nugget_arsenic",    MaterialType.ARSENIC);
        nuggetModItem(writer, "nugget_technetium", "nugget_technetium", MaterialType.TECHNETIUM);
        // Флюс — мод-предмет, объём как у слитка (порошок = 1 слиток, как DUST.q(1) в оригинале)
        nuggetModItemAmount(writer, "powder_flux", "flux_powder", MaterialType.FLUX, MaterialStack.MB_PER_INGOT);

        // ═══════════════════════════════════════════════════════════════════
        // Рудные материалы — ADDITIVE (оригинал): сплавляются во вторую ступень (hematite/malachite рецепты)
        // ═══════════════════════════════════════════════════════════════════
        // Шлак плавится обратно (оригинал: оредикт ingotSlag → автоген формы INGOT).
        nuggetModItemAmount(writer, "ingot_slag", "ingot_slag", MaterialType.SLAG, MaterialStack.MB_PER_INGOT);

        // Ориг. registerOre: hematite → INGOT.q(1) = 1000 mB, malachite → INGOT.q(6) = 6000 mB.
        nuggetModItemAmount(writer, "resource_hematite",        "resource_hematite",        MaterialType.HEMATITE,  MaterialStack.MB_PER_INGOT);
        nuggetModItemAmount(writer, "stone_resource_hematite",  "stone_resource_hematite",  MaterialType.HEMATITE,  MaterialStack.MB_PER_INGOT);
        nuggetModItemAmount(writer, "resource_malachite",       "resource_malachite",       MaterialType.MALACHITE, MaterialStack.MB_PER_INGOT * 6);
        nuggetModItemAmount(writer, "stone_resource_malachite", "stone_resource_malachite", MaterialType.MALACHITE, MaterialStack.MB_PER_INGOT * 6);

        // ═══════════════════════════════════════════════════════════════════
        // Авто-генерация по формам (порт цикла getSmeltingRecipes: материал × форма
        // с oredict-записью). Дубликаты с tag-входами выше не страшны — тигель
        // дедуплицирует выходы по материалу.
        // ═══════════════════════════════════════════════════════════════════
        shapeAutogen(writer);

        // ═══════════════════════════════════════════════════════════════════
        // Руды с побочными продуктами (порт MatDistribution.registerOre).
        // Каменная и содовая (натриевая) побочки не портированы — материалов нет в реестре.
        // Урановая руда перенесена отдельным блоком ниже (MaterialType.URANIUM).
        // ═══════════════════════════════════════════════════════════════════
        oreByproducts(writer, "byproduct_ore_gneiss_iron", "ore_gneiss_iron", new MaterialStack(MaterialType.IRON,     MaterialStack.MB_PER_INGOT * 2),
                                                    new MaterialStack(MaterialType.TITANIUM, MaterialStack.MB_PER_NUGGET * 3));
        oreByproducts(writer, "byproduct_ore_aluminium", "ore_aluminium",      new MaterialStack(MaterialType.ALUMINIUM, MaterialStack.MB_PER_INGOT * 2));
        oreByproducts(writer, "byproduct_ore_nether_tungsten", "ore_nether_tungsten", new MaterialStack(MaterialType.TUNGSTEN, MaterialStack.MB_PER_INGOT * 2));
        oreByproducts(writer, "byproduct_ore_gneiss_gold", "ore_gneiss_gold",    new MaterialStack(MaterialType.GOLD, MaterialStack.MB_PER_INGOT * 2),
                                                    new MaterialStack(MaterialType.LEAD, MaterialStack.MB_PER_NUGGET));
        oreByproducts(writer, "byproduct_ore_copper", "ore_copper",         new MaterialStack(MaterialType.COPPER, MaterialStack.MB_PER_INGOT * 2));
        oreByproducts(writer, "byproduct_ore_gneiss_copper", "ore_gneiss_copper",  new MaterialStack(MaterialType.COPPER, MaterialStack.MB_PER_INGOT * 2));
        oreByproducts(writer, "byproduct_ore_nether_cobalt", "ore_nether_cobalt",  new MaterialStack(MaterialType.COBALT, MaterialStack.MB_PER_INGOT));
        oreByproducts(writer, "byproduct_ore_nether_coal", "ore_nether_coal",    new MaterialStack(MaterialType.CARBON, MaterialStack.MB_PER_INGOT * 3));

        // ═══════════════════════════════════════════════════════════════════
        // УРАНОВАЯ РУДА (порт MatDistribution:77 registerOre(U.ore(), ...)):
        // MAT_URANIUM INGOT.q(2) = 2000 mB + MAT_LEAD NUGGET.q(3) = 333 mB
        // + MAT_STONE QUART.q(1) = 2250 mB. Все варианты руды (штраф от пропуска
        // урана исправлен 2026-09-20 — возвращён из 1.7.10).
        // ═══════════════════════════════════════════════════════════════════
        oreTagMulti(writer, "byproduct_ore_uranium", "forge:ores/uranium",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_deepslate", "uranium_ore_deepslate",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_nether", "nether_uranium_ore",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_gneiss", "gneiss_uranium_ore",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_gneiss2", "ore_gneiss_uranium",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_gneiss_scorched", "ore_gneiss_uranium_scorched",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_nether2", "ore_nether_uranium",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_nether2_scorched", "ore_nether_uranium_scorched",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
        oreByproducts(writer, "byproduct_ore_uranium_scorched", "ore_uranium_scorched",
                new MaterialStack(MaterialType.URANIUM, MaterialStack.MB_PER_INGOT * 2),
                new MaterialStack(MaterialType.LEAD,    MaterialStack.MB_PER_NUGGET * 3),
                new MaterialStack(MaterialType.STONE,   MB_PER_QUART));
    }

    /** mB на форму (порт оригинальных квантов MaterialShapes; 1 квант = 1000/72 ≈ 13.89 mB). */
    private static int shapeMb(com.hbm_m.item.material.MaterialShape shape) {
        return switch (shape) {
            case NUGGET      -> MaterialStack.MB_PER_NUGGET;                      // 111 (8 квантов)
            case POWDER_TINY -> MaterialStack.MB_PER_NUGGET;                      // 111 (dustTiny = 8 квантов)
            case BILLET      -> MaterialStack.MB_PER_NUGGET * 6;                  // 667
            case INGOT       -> MaterialStack.MB_PER_INGOT;                       // 1000
            case CRYSTAL     -> MaterialStack.MB_PER_INGOT;
            case POWDER      -> MaterialStack.MB_PER_INGOT;
            case PLATE       -> MaterialStack.MB_PER_INGOT;
            case PLATE_CAST  -> MaterialStack.MB_PER_INGOT * 3;                   // 3000 (plateTriple)
            case PLATE_WELDED-> MaterialStack.MB_PER_INGOT * 6;                   // 6000 (plateSextuple)
            case BLOCK       -> MaterialStack.MB_PER_INGOT * 9;                   // 9000
            case WIRE        -> MaterialStack.MB_PER_NUGGET * 9 / 8;              // 125 (9 квантов)
            case WIRE_DENSE  -> MaterialStack.MB_PER_INGOT;
            default          -> 0;
        };
    }

    /** Порт авто-генерации: материал × форма (все объявленные формы предмета). */
    private static void shapeAutogen(Consumer<FinishedRecipe> writer) {
        for (com.hbm_m.item.material.ModMaterials mat : com.hbm_m.item.material.ModMaterials.values()) {
            // Общий маппер несоответствий имён (ferrouranium→FERRO, combine_steel→CMB и т.д.)
            MaterialType mt = MaterialType.of(mat);
            if (mt == null || mt.smeltable != MaterialType.SmeltingBehavior.SMELTABLE) continue;

            for (com.hbm_m.item.material.MaterialShape shape : mat.getShapes()) {
                int mb = shapeMb(shape);
                if (mb <= 0) continue;
                // Блоки хранения не имеют Item-регистрации в ModMaterialItems (их
                // регистрирует ModBlocks как BlockHazard) — ищем block_<id> в реестре.
                Item item;
                if (shape == com.hbm_m.item.material.MaterialShape.BLOCK) {
                    ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("hbm_m", shape.itemId(mat));
                    if (!BuiltInRegistries.ITEM.containsKey(blockId)) continue;
                    item = BuiltInRegistries.ITEM.get(blockId);
                } else {
                    item = com.hbm_m.item.material.ModMaterialItems.item(mat, shape);
                    if (item == null) continue;
                }
                nuggetItem(writer, "shape_" + mt.name + "_" + shape.name().toLowerCase(java.util.Locale.ROOT), item, mt, mb);
            }
        }
    }

    /** Руда с побочными продуктами (порт MatDistribution.registerOre). */
    private static void oreByproducts(Consumer<FinishedRecipe> writer, String fileName, String itemPath, MaterialStack... outputs) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", itemPath);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(Ingredient.of(item), java.util.List.of(outputs))
                .save(writer, "crucible_smelting/" + fileName);
    }

    /** Руда с побочными продуктами по предметному тегу (ванильные/forge руды). */
    private static void oreTagMulti(Consumer<FinishedRecipe> writer, String fileName, String tagId, MaterialStack... outputs) {
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(Ingredient.of(forgeTag(tagId)), java.util.List.of(outputs))
                .save(writer, "crucible_smelting/" + fileName);
    }

    /** Ванильный предмет с мульти-выходом (рельсы и пр.). */
    private static void nuggetMulti(Consumer<FinishedRecipe> writer, String id, Item item, MaterialStack... outputs) {
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, java.util.List.of(outputs))
                .save(writer, "crucible_smelting/" + id);
    }

    /** Мод-предмет с мульти-выходом по id строки. */
    private static void nuggetModMulti(Consumer<FinishedRecipe> writer, String path, String itemId, MaterialStack... outputs) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", itemId);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, java.util.List.of(outputs))
                .save(writer, "crucible_smelting/" + path);
    }

    /** Слиток по forge-тегу {@code forge:ingots/<name>} → {@code MB_PER_INGOT} материала. */
    private static void ingot(Consumer<FinishedRecipe> writer, String name, MaterialType mat) {
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(
                        "forge:ingots/" + name, mat, MaterialStack.MB_PER_INGOT)
                .save(writer, "crucible_smelting/ingot_" + name);
    }

    /** Руда по forge-тегу {@code forge:ores/<name>} → {@code MB_PER_INGOT * 2} материала. */
    private static void ore(Consumer<FinishedRecipe> writer, String name, MaterialType mat) {
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(
                        "forge:ores/" + name, mat, MaterialStack.MB_PER_INGOT * 2)
                .save(writer, "crucible_smelting/ore_" + name);
    }

    /** Ванильный предмет → {@code MB_PER_NUGGET} материала (сырьевые/алмазные входы). */
    private static void nuggetItem(Consumer<FinishedRecipe> writer, String id, Item item, MaterialType mat) {
        nuggetItem(writer, id, item, mat, MaterialStack.MB_PER_NUGGET);
    }

    /** Предмет → материал с явным объёмом (авто-генерация по формам). */
    private static void nuggetItem(Consumer<FinishedRecipe> writer, String id, Item item, MaterialType mat, int amountMb) {
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, mat, amountMb)
                .save(writer, "crucible_smelting/" + id);
    }

    /** Предмет мода по id строки ({@code hbm_m:<path>}) → {@code MB_PER_NUGGET} материала. */
    private static void nuggetModItem(Consumer<FinishedRecipe> writer, String path, String itemId, MaterialType mat) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", itemId);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return;  // предмет может отсутствовать
        Item item = BuiltInRegistries.ITEM.get(id);
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, mat, MaterialStack.MB_PER_NUGGET)
                .save(writer, "crucible_smelting/" + path);
    }

    /** Предмет мода по id строки с явным объёмом. */
    private static void nuggetModItemAmount(Consumer<FinishedRecipe> writer, String path, String itemId, MaterialType mat, int amount) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", itemId);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, mat, amount)
                .save(writer, "crucible_smelting/" + path);
    }

    /** Блок-руда мода по id строки ({@code hbm_m:<path>}) → {@code MB_PER_INGOT * 2} материала. */
    private static void blockOre(Consumer<FinishedRecipe> writer, String path, MaterialType mat) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return;  // блок может отсутствовать
        Item item = BuiltInRegistries.ITEM.get(id);
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, mat, MaterialStack.MB_PER_INGOT * 2)
                .save(writer, "crucible_smelting/" + path);
    }

    /** Утилита: forge {@link TagKey} предмета по строке вида {@code "forge:ingots/iron"}. */
    @SuppressWarnings("unused")
    private static TagKey<Item> forgeTag(String id) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
    }

    /** Утилита: {@link Ingredient} по forge-тегу (для случаев, где нужен сам Ingredient). */
    @SuppressWarnings("unused")
    private static Ingredient forgeIngredient(String id) {
        return Ingredient.of(forgeTag(id));
    }
}
