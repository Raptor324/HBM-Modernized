package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;

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
        radgen(writer, "waste_short", ModItems.NUCLEAR_WASTE_SHORT.get(),
                1500, 30 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get()));
        radgen(writer, "waste_short_tiny", ModItems.NUCLEAR_WASTE_SHORT_TINY.get(),
                150, 3 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get()));
        radgen(writer, "waste_long", ModItems.NUCLEAR_WASTE_LONG.get(),
                500, 2 * 60 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get()));
        radgen(writer, "waste_long_tiny", ModItems.NUCLEAR_WASTE_LONG_TINY.get(),
                50, 12 * 60 * 20, new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get()));
        // Scrap: сгорает без выхода (result опускается).
        radgen(writer, "scrap", ModItems.SCRAP_NUCLEAR.get(),
                50, 5 * 60 * 20, ItemStack.EMPTY);
        radgen(writer, "gem_rad", ModItems.GEM_RAD.get(),
                25_000, 30 * 60 * 20, new ItemStack(Items.DIAMOND));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private static void radgen(Consumer<FinishedRecipe> writer, String id,
                               net.minecraft.world.item.Item input, int power, int duration, ItemStack output) {
        RadGenRecipeBuilder.radgenRecipe(Ingredient.of(input), power, duration, output)
                .save(writer, "radgen/" + id);
    }
}
//?}
