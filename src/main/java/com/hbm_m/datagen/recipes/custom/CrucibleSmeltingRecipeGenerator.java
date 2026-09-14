package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

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
        ingot(writer, "alloy",       MaterialType.ALLOY);
        ingot(writer, "dura_steel",  MaterialType.DURA_STEEL);
        ingot(writer, "desh",        MaterialType.DESH);
        ingot(writer, "star_metal",  MaterialType.STAR_METAL);
        ingot(writer, "tcalloy",     MaterialType.TCALLOY);
        ingot(writer, "cdalloy",     MaterialType.CDALLOY);
        ingot(writer, "cmb",         MaterialType.CMB);
        ingot(writer, "schrabidium", MaterialType.SCHRABIDIUM);
        ingot(writer, "bbronze",     MaterialType.BBRONZE);
        ingot(writer, "abronze",     MaterialType.ABRONZE);
        ingot(writer, "saturnite",   MaterialType.SATURNITE);
        ingot(writer, "lead",        MaterialType.LEAD);
        ingot(writer, "bismuth",     MaterialType.BISMUTH);
        ingot(writer, "beryllium",   MaterialType.BERYLLIUM);
        ingot(writer, "cobalt",      MaterialType.COBALT);
        ingot(writer, "nickel",      MaterialType.NICKEL);

        // ═══════════════════════════════════════════════════════════════════
        // РУДЫ (forge:ores/<material>) — MB_PER_INGOT * 2 (как в оригинале)
        // ═══════════════════════════════════════════════════════════════════
        ore(writer, "iron",     MaterialType.IRON);
        ore(writer, "copper",   MaterialType.COPPER);
        ore(writer, "gold",     MaterialType.GOLD);
        ore(writer, "titanium", MaterialType.TITANIUM);

        // ═══════════════════════════════════════════════════════════════════
        // Алмазные/сырьевые предметы — MB_PER_NUGGET (алмазные/сырьевые входы для алиирования)
        // ═══════════════════════════════════════════════════════════════════
        nuggetItem(writer, "carbon_coal",       Items.COAL,                        MaterialType.CARBON);
        nuggetItem(writer, "carbon_charcoal",   Items.CHARCOAL,                    MaterialType.CARBON);
        nuggetItem(writer, "redstone",          Items.REDSTONE,                    MaterialType.REDSTONE);
        nuggetModItem(writer, "nugget_arsenic",    "nugget_arsenic",    MaterialType.ARSENIC);
        nuggetModItem(writer, "nugget_technetium", "nugget_technetium", MaterialType.TECHNETIUM);

        // ═══════════════════════════════════════════════════════════════════
        // Блоки-руды (ресурсы гематита/малахита) — MB_PER_INGOT * 2
        // ═══════════════════════════════════════════════════════════════════
        blockOre(writer, "resource_hematite",        MaterialType.IRON);
        blockOre(writer, "stone_resource_hematite",  MaterialType.IRON);
        blockOre(writer, "resource_malachite",       MaterialType.COPPER);
        blockOre(writer, "stone_resource_malachite", MaterialType.COPPER);
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
        CrucibleSmeltingRecipeBuilder.crucibleSmelting(item, mat, MaterialStack.MB_PER_NUGGET)
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
//?}
