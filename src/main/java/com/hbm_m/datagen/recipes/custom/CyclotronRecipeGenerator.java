package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов циклотрона ({@code hbm_m:cyclotron}).
 *
 * <p>Порт рецептов из удалённого статического {@code CyclotronRecipes.registerRecipes()}
 * (legacy 1.7.10 defaults). Оригинал регистрировал каждую пару «target + input → output + amat»,
 * резолвя входы через теги {@code forge:powders/<element>} (или {@code forge:dusts/<element>}),
 * а выходы как конкретные {@code hbm_m:<id>} предметы через {@code BuiltInRegistries.ITEM}.
 * Предметы, отсутствующие в реестре, пропускались (как в оригинале).</p>
 *
 * <p><b>Упрощение для data-driven:</b> вместо того, чтобы плодить дубликаты с двумя
 * tag-кандидатами ({@code powders/X} и {@code dusts/X}), генератор создаёт ОДИН рецепт
 * на каждый {@code addLegacy*}-вызов, используя {@code forge:powders/<element>} для тегов.
 * Item-входы (strontium_powder, mercury_ingot, etc.) берутся напрямую из реестра по id;
 * если предмета нет — рецепт пропускается (поведение оригинала).</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class CyclotronRecipeGenerator {

    private CyclotronRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // ═══════════════════════════════════════════════════════════════════
        // PART_LITHIUM — amatProduced = 50
        // ═══════════════════════════════════════════════════════════════════
        tag(writer, "li_lithium",     "part_lithium", "lithium",    50, "beryllium_powder");
        tag(writer, "li_beryllium",   "part_lithium", "beryllium",  50, "boron_powder");
        tag(writer, "li_boron",       "part_lithium", "boron",      50, "coal_powder");
        tag(writer, "li_quartz",      "part_lithium", "quartz",     50, "fire_powder");
        tag(writer, "li_phosphorus",  "part_lithium", "phosphorus", 50, "sulfur");
        tag(writer, "li_iron",        "part_lithium", "iron",       50, "cobalt_powder");
        item(writer, "li_strontium", "part_lithium",  "strontium_powder",            50, "zirconium_powder");
        tag(writer, "li_gold",        "part_lithium", "gold",       50, "mercury_ingot");
        tag(writer, "li_polonium",    "part_lithium", "polonium",   50, "astatine_powder");
        tag(writer, "li_lanthanium",  "part_lithium", "lanthanium", 50, "cerium_powder");
        tag(writer, "li_actinium",    "part_lithium", "actinium",   50, "thorium_powder");
        tag(writer, "li_uranium",     "part_lithium", "uranium",    50, "neptunium_powder");
        tag(writer, "li_neptunium",   "part_lithium", "neptunium",   50, "plutonium_powder");

        // ═══════════════════════════════════════════════════════════════════
        // PART_BERYLLIUM — amatProduced = 25
        // ═══════════════════════════════════════════════════════════════════
        tag(writer, "be_lithium", "part_beryllium", "lithium",  25, "boron_powder");
        tag(writer, "be_quartz", "part_beryllium",  "quartz",   25, "sulfur");
        tag(writer, "be_titanium", "part_beryllium","titanium", 25, "iron_powder");
        tag(writer, "be_cobalt", "part_beryllium",  "cobalt",   25, "copper_powder");
        item(writer, "be_strontium", "part_beryllium", "strontium_powder",  25, "niobium_powder");
        item(writer, "be_cerium",    "part_beryllium", "cerium_powder",     25, "neodymium_powder");
        tag(writer, "be_thorium", "part_beryllium",   "thorium",  25, "uranium_powder");

        // ═══════════════════════════════════════════════════════════════════
        // PART_CARBON — amatProduced = 10
        // ═══════════════════════════════════════════════════════════════════
        tag(writer, "ca_boron",    "part_carbon", "boron",     10, "aluminium_powder");
        tag(writer, "ca_sulfur",   "part_carbon", "sulfur",    10, "titanium_powder");
        tag(writer, "ca_titanium", "part_carbon", "titanium",  10, "cobalt_powder");
        item(writer, "ca_caesium", "part_carbon",  "caesium_powder",     10, "lanthanium_powder");
        item(writer, "ca_neodymium", "part_carbon", "neodymium_powder",  10, "gold_powder");
        item(writer, "ca_mercury", "part_carbon",  "mercury_ingot",      10, "polonium_powder");
        tag(writer, "ca_lead",     "part_carbon", "lead",      10, "ra226_powder");
        item(writer, "ca_astatine","part_carbon",  "astatine_powder",     10, "actinium_powder");

        // ═══════════════════════════════════════════════════════════════════
        // PART_COPPER — amatProduced = 15
        // ═══════════════════════════════════════════════════════════════════
        tag(writer, "cu_beryllium", "part_copper", "beryllium",  15, "quartz_powder");
        tag(writer, "cu_coal",     "part_copper", "coal",        15, "bromine_powder");
        tag(writer, "cu_titanium", "part_copper", "titanium",    15, "strontium_powder");
        tag(writer, "cu_iron",     "part_copper", "iron",        15, "niobium_powder");
        item(writer, "cu_bromine", "part_copper",  "bromine_powder",     15, "iodine_powder");
        item(writer, "cu_strontium","part_copper", "strontium_powder",   15, "neodymium_powder");
        item(writer, "cu_niobium", "part_copper",  "niobium_powder",     15, "caesium_powder");
        item(writer, "cu_iodine",  "part_copper",  "iodine_powder",      15, "polonium_powder");
        item(writer, "cu_caesium", "part_copper",  "caesium_powder",     15, "actinium_powder");
        tag(writer, "cu_gold",     "part_copper", "gold",        15, "uranium_powder");

        // ═══════════════════════════════════════════════════════════════════
        // PART_PLUTONIUM — amatProduced = 100 (tennessine/australium chain)
        // ═══════════════════════════════════════════════════════════════════
        tag(writer, "pl_phosphorus", "part_plutonium", "phosphorus", 100, "tennessine_powder");
        tag(writer, "pl_plutonium",  "part_plutonium", "plutonium",  100, "tennessine_powder");
        item(writer, "pl_tennessine","part_plutonium", "tennessine_powder",   100, "australium_powder");
        item(writer, "pl_pellet",   "part_plutonium", "pellet_charged",     1000, "nugget_schrabidium");
    }

    /**
     * Пара {@code addLegacyTag}: target = mod-предмет ({@code part_*}), input = forge-тег
     * {@code "forge:powders/<element>"}, output — mod-предмет по строковому id.
     * Если target или выходной предмет отсутствуют в {@code BuiltInRegistries.ITEM} — рецепт пропускается.
     */
    private static void tag(Consumer<FinishedRecipe> writer, String name, String targetId,
                           String inputElement, int amat, String outputId) {
        Item target = modItem(targetId);
        Item outItem = modItem(outputId);
        if (target == null || outItem == null) return;  // нет предмета → нет рецепта (как в оригинале)
        CyclotronRecipeBuilder.cyclotronRecipe(
                        target, "forge:powders/" + inputElement, new ItemStack(outItem), amat)
                .save(writer, "cyclotron/" + name);
    }

    /**
     * Пара {@code addLegacyItem}: target + input — mod-предметы по строковым id.
     * Если target/input/output отсутствуют — рецепт пропускается.
     */
    private static void item(Consumer<FinishedRecipe> writer, String name, String targetId,
                            String inputId, int amat, String outputId) {
        Item target = modItem(targetId);
        Item input  = modItem(inputId);
        Item outItem = modItem(outputId);
        if (target == null || input == null || outItem == null) return;
        CyclotronRecipeBuilder.cyclotronRecipe(target, input, new ItemStack(outItem), amat)
                .save(writer, "cyclotron/" + name);
    }

    private static Item modItem(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) return null;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? null : item;
    }
}
//?}
