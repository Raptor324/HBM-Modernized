package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * Генератор {@code hbm_m:molten_alloy} (data-driven JSON).
 *
 * <p>Порт 6 рецептов сплавления из удалённого {@code MoltenAlloyRecipes.registerDefaults()}:
 * те же входы/выходы в {@link MaterialStack} (mB), те же {@code frequency}.
 * Материалы разрешаются статически (compile-time known enum), без поиска по реестру —
 * мод-предметы для сплавления не нужны (molten-only materials: carbon, arsenic,
 * technetium, redstone — числовые {@link MaterialType} constants).</p>
 */
public final class MoltenAlloyRecipeGenerator {

    public static void generate(Consumer<FinishedRecipe> writer) {
        int n = MaterialStack.MB_PER_NUGGET;
        int i = MaterialStack.MB_PER_INGOT;

        // crucible.steel — 2 iron + 3 carbon -> 2 steel (flux omitted, as in original port)
        moltenAlloy(writer, "steel", 20,
                new MaterialStack[] { new MaterialStack(MaterialType.IRON,   n * 2),
                                      new MaterialStack(MaterialType.CARBON, n * 3) },
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,  n * 2) });

        // crucible.hss — 5 steel + 3 tungsten + 1 cobalt -> 9 dura steel
        moltenAlloy(writer, "hss", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,    n * 5),
                                      new MaterialStack(MaterialType.TUNGSTEN, n * 3),
                                      new MaterialStack(MaterialType.COBALT,   n) },
                new MaterialStack[] { new MaterialStack(MaterialType.DURA_STEEL, n * 9) });

        // crucible.tcalloy — 8 steel + 1 technetium -> 1 tcalloy ingot
        moltenAlloy(writer, "tcalloy", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,     n * 8),
                                      new MaterialStack(MaterialType.TECHNETIUM, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.TCALLOY,   i) });

        // crucible.bbronze — 8 copper + 1 bismuth -> 1 bismuth bronze ingot (flux/slag omitted)
        moltenAlloy(writer, "bbronze", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER,  n * 8),
                                      new MaterialStack(MaterialType.BISMUTH, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.BBRONZE, i) });

        // crucible.abronze — 8 copper + 1 arsenic -> 1 arsenic bronze ingot (flux/slag omitted)
        moltenAlloy(writer, "abronze", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER,  n * 8),
                                      new MaterialStack(MaterialType.ARSENIC, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.ABRONZE, i) });

        // crucible.redcopper — 1 copper + 1 redstone -> 2 mingrade (mass conserving, 1:1:2 ratio)
        moltenAlloy(writer, "redcopper", 2,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER,   n),
                                      new MaterialStack(MaterialType.REDSTONE, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.MINGRADE, n * 2) });
    }

    private static void moltenAlloy(Consumer<FinishedRecipe> writer, String name, int frequency,
                                    MaterialStack[] inputs, MaterialStack[] outputs) {
        new MoltenAlloyRecipeBuilder(inputs, outputs, frequency)
                .save(writer, "molten_alloy/" + name);
    }
}
//?}
