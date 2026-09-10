package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов радиационного генератора ({@code hbm_m:radgen}).
 *
 * <p>Порт рецептов 1:1 из статического {@code com.hbm_m.recipe.RadGenRecipes} (Java-порт 1.7.10
 * {@code TileEntityMachineRadGen.fuels}). Длительности записаны теми же произведениями
 * секунд×20 тик, что и в оригинале.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class RadGenRecipeGenerator {

    private RadGenRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // Original: die Schleifen ueber {@code WasteClass.values()} - jede Abfallklasse hat ihr
        // eigenes Rezept, damit das abgereicherte Stueck dieselbe Klasse behaelt. Leistung und
        // Dauer sind fuer alle Klassen gleich.
        wasteGroup(writer, "waste_short", "nw_short", "nw_short_dep", 1500, 30 * 60 * 20);
        wasteGroup(writer, "waste_short_tiny", "nw_short_tiny", "nw_short_dep_tiny", 150, 3 * 60 * 20);
        wasteGroup(writer, "waste_long", "nw_long", "nw_long_dep", 500, 2 * 60 * 60 * 20);
        wasteGroup(writer, "waste_long_tiny", "nw_long_tiny", "nw_long_dep_tiny", 50, 12 * 60 * 20);
        // Scrap: сгорает без выхода (result опускается).
        radgen(writer, "scrap", ModMaterialItems.item(ModMaterials.SCRAP_NUCLEAR, MaterialShape.SCRAP),
                50, 5 * 60 * 20, ItemStack.EMPTY);
        radgen(writer, "gem_rad", ModItems.GEM_RAD.get(),
                25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    /**
     * Ein Rezept je Abfallklasse einer Gruppe. Frische und abgereicherte Gruppe stehen in
     * derselben Reihenfolge, Listenplatz {@code i} gehoert also zusammen.
     */
    private static void wasteGroup(Consumer<FinishedRecipe> writer, String idPrefix,
                                   String fresh, String spent, int power, int duration) {

        var freshItems = com.hbm_m.item.PartTabMetaItems.group(fresh);
        var spentItems = com.hbm_m.item.PartTabMetaItems.group(spent);

        for (int i = 0; i < freshItems.size(); i++) {
            ItemStack out = i < spentItems.size() ? new ItemStack(spentItems.get(i)) : ItemStack.EMPTY;
            radgen(writer, idPrefix + "_" + i, freshItems.get(i), power, duration, out);
        }
    }

    private static void radgen(Consumer<FinishedRecipe> writer, String id,
                               net.minecraft.world.item.Item input, int power, int duration, ItemStack output) {
        RadGenRecipeBuilder.radgenRecipe(Ingredient.of(input), power, duration, output)
                .save(writer, "radgen/" + id);
    }
}
//?}
