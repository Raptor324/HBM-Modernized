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
        ingot(writer, "u238",        MaterialType.URANIUM238);
        ingot(writer, "strontium",   MaterialType.STRONTIUM);
        ingot(writer, "calcium",     MaterialType.CALCIUM);
        ingot(writer, "mud",         MaterialType.MUD);

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
        // Флюс — мод-предмет, объём как у слитка (порошок = 1 слиток, как DUST.q(1) в оригинале)
        nuggetModItemAmount(writer, "powder_flux", "flux_powder", MaterialType.FLUX, MaterialStack.MB_PER_INGOT);

        // ═══════════════════════════════════════════════════════════════════
        // Рудные материалы — ADDITIVE (оригинал): сплавляются во вторую ступень (hematite/malachite рецепты)
        // ═══════════════════════════════════════════════════════════════════
        // Шлак плавится обратно (оригинал: оредикт ingotSlag → автоген формы INGOT).
        nuggetModItemAmount(writer, "ingot_slag", "ingot_slag", MaterialType.SLAG, MaterialStack.MB_PER_INGOT);

        blockOre(writer, "resource_hematite",        MaterialType.HEMATITE);
        blockOre(writer, "stone_resource_hematite",  MaterialType.HEMATITE);
        blockOre(writer, "resource_malachite",       MaterialType.MALACHITE);
        blockOre(writer, "stone_resource_malachite", MaterialType.MALACHITE);

        // ═══════════════════════════════════════════════════════════════════
        // Авто-генерация по формам (порт цикла getSmeltingRecipes: материал × форма
        // с oredict-записью). Дубликаты с tag-входами выше не страшны — тигель
        // дедуплицирует выходы по материалу.
        // ═══════════════════════════════════════════════════════════════════
        shapeAutogen(writer);

        // ═══════════════════════════════════════════════════════════════════
        // Руды с побочными продуктами (порт MatDistribution.registerOre).
        // Каменная и содовая (натриевая) побочки не портированы — материалов нет в реестре.
        // Урановая руда пропущена: MAT_URANIUM отсутствует в MaterialType (уран идёт
        // через химцепочку, не через тигель).
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
    }

    /** Соответствие MaterialType ↔ ModMaterials для авто-генерации по формам. */
    @Nullable
    private static com.hbm_m.item.material.ModMaterials modMaterialsOf(MaterialType type) {
        com.hbm_m.item.material.ModMaterials byId = com.hbm_m.item.material.ModMaterials.byId(type.name);
        if (byId != null) return byId;
        return switch (type) {
            case FERRO    -> com.hbm_m.item.material.ModMaterials.FERROURANIUM;
            case MAGTUNG  -> com.hbm_m.item.material.ModMaterials.MAGNETIZED_TUNGSTEN;
            case MINGRADE -> com.hbm_m.item.material.ModMaterials.RED_COPPER;
            default       -> null;
        };
    }

    /** mB на форму (оригинальные кванты MaterialShapes × 13.89 ≈ mB, 1 слиток = 1000 mB). */
    private static int shapeMb(com.hbm_m.item.material.MaterialShape shape) {
        return switch (shape) {
            case NUGGET     -> MaterialStack.MB_PER_NUGGET;                       // 111
            case BILLET     -> MaterialStack.MB_PER_NUGGET * 6;                   // 667
            case INGOT      -> MaterialStack.MB_PER_INGOT;                        // 1000
            case CRYSTAL    -> MaterialStack.MB_PER_INGOT;
            case POWDER     -> MaterialStack.MB_PER_INGOT;
            case PLATE      -> MaterialStack.MB_PER_INGOT;
            case BLOCK      -> MaterialStack.MB_PER_INGOT * 9;                    // 9000
            case WIRE       -> MaterialStack.MB_PER_NUGGET * 9 / 8;               // 125 (9 квантов)
            case WIRE_DENSE -> MaterialStack.MB_PER_INGOT;
            default         -> 0;
        };
    }

    /** Порт авто-генерации: материал × форма (все объявленные формы предмета). */
    private static void shapeAutogen(Consumer<FinishedRecipe> writer) {
        for (com.hbm_m.item.material.ModMaterials mat : com.hbm_m.item.material.ModMaterials.values()) {
            // Обратная разрешалка несовпадающих имён (ferrouranium→FERRO и т.п.)
            MaterialType mt = switch (mat) {
                case FERROURANIUM        -> MaterialType.FERRO;
                case MAGNETIZED_TUNGSTEN -> MaterialType.MAGTUNG;
                case RED_COPPER          -> MaterialType.MINGRADE;
                default                  -> MaterialType.byName(mat.getId());
            };
            if (mt == null || mt.smeltable != MaterialType.SmeltingBehavior.SMELTABLE) continue;

            for (com.hbm_m.item.material.MaterialShape shape : mat.getShapes()) {
                int mb = shapeMb(shape);
                if (mb <= 0) continue;
                Item item = com.hbm_m.item.material.ModMaterialItems.item(mat, shape);
                if (item == null) continue;
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
//?}
