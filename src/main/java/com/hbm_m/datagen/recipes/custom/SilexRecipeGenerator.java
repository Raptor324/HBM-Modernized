package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов SILEX ({@code hbm_m:silex}).
 *
 * <p>Порт рецептов 1:1 из статического {@code com.hbm_m.recipe.SilexRecipes} — прямый перенос
 * весовых распределений оригинального {@code SILEXRecipes} 1.7.10
 * (U → U235/U238, Pu-Mix → Pu239/Pu240, Am-Mix → Am241/Am242).</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class SilexRecipeGenerator {

    private SilexRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // U -> 1x U235 / 11x U238
        silex(writer, "uranium", ModMaterials.URANIUM,
                ModMaterialItems.stack(ModMaterials.URANIUM235, MaterialShape.NUGGET, 1), 1,
                ModMaterialItems.stack(ModMaterials.URANIUM238, MaterialShape.NUGGET, 1), 11);

        // Pu-Mix -> 6x Pu239 / 3x Pu240
        silex(writer, "pu_mix", ModMaterials.PU_MIX,
                ModMaterialItems.stack(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET, 1), 6,
                ModMaterialItems.stack(ModMaterials.PLUTONIUM240, MaterialShape.NUGGET, 1), 3);

        // Am-Mix -> 3x Am241 / 6x Am242
        silex(writer, "am_mix", ModMaterials.AM_MIX,
                ModMaterialItems.stack(ModMaterials.AM241, MaterialShape.NUGGET, 1), 3,
                ModMaterialItems.stack(ModMaterials.AM242, MaterialShape.NUGGET, 1), 6);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private static ItemStack out(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }

    private static void silex(Consumer<FinishedRecipe> writer, String id, ModMaterials input,
                              ItemStack outA, int weightA, ItemStack outB, int weightB) {
        SilexRecipeBuilder.silexRecipe(
                        Ingredient.of(ModMaterialItems.item(input, MaterialShape.INGOT)),
                        100, 100,
                        new ItemStack[]{outA, outB},
                        new int[]{weightA, weightB})
                .save(writer, "silex/" + id);
    }
}
//?}
