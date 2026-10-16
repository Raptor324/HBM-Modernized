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

        // Полный набор CrucibleRecipes.registerDefaults (1.7.10, без GT6-вариантов)

        // crucible.steel — 2 iron + 3 carbon + 1 flux -> 2 steel
        moltenAlloy(writer, "steel", 20,
                new MaterialStack[] { new MaterialStack(MaterialType.IRON,   n * 2),
                                      new MaterialStack(MaterialType.CARBON, n * 3),
                                      new MaterialStack(MaterialType.FLUX,   n) },
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,  n * 2) });

        // crucible.hematite — 2 hematite + 2 flux -> 1 iron + 3 slag
        moltenAlloy(writer, "hematite", 6,
                new MaterialStack[] { new MaterialStack(MaterialType.HEMATITE, i * 2),
                                      new MaterialStack(MaterialType.FLUX,     n * 2) },
                new MaterialStack[] { new MaterialStack(MaterialType.IRON, i),
                                      new MaterialStack(MaterialType.SLAG, n * 3) });

        // crucible.malachite — 2 malachite + 2 flux -> 1 copper + 3 slag
        moltenAlloy(writer, "malachite", 6,
                new MaterialStack[] { new MaterialStack(MaterialType.MALACHITE, i * 2),
                                      new MaterialStack(MaterialType.FLUX,      n * 2) },
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER, i),
                                      new MaterialStack(MaterialType.SLAG,  n * 3) });

        // crucible.redcopper — 1 copper + 1 redstone -> 2 mingrade
        moltenAlloy(writer, "redcopper", 2,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER,   n),
                                      new MaterialStack(MaterialType.REDSTONE, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.MINGRADE, n * 2) });

        // crucible.hss — 5 steel + 3 tungsten + 1 cobalt -> 9 dura steel
        moltenAlloy(writer, "hss", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,    n * 5),
                                      new MaterialStack(MaterialType.TUNGSTEN, n * 3),
                                      new MaterialStack(MaterialType.COBALT,   n) },
                new MaterialStack[] { new MaterialStack(MaterialType.DURA_STEEL, n * 9) });

        // crucible.ferro — 2 steel + 1 U238 -> 3 ferro(ferrouranium)
        moltenAlloy(writer, "ferro", 3,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL, n * 2),
                                      new MaterialStack(MaterialType.URANIUM238, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.FERRO, n * 3) });

        // crucible.tcalloy — 8 steel + 1 technetium -> 1 tcalloy ingot
        moltenAlloy(writer, "tcalloy", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,     n * 8),
                                      new MaterialStack(MaterialType.TECHNETIUM, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.TCALLOY,   i) });

        // crucible.cdalloy — 8 steel + 1 cadmium -> 1 cadmium alloy ingot
        moltenAlloy(writer, "cdalloy", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL,   n * 8),
                                      new MaterialStack(MaterialType.CADMIUM, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.CDALLOY, i) });

        // crucible.bbronze — 8 copper + 1 bismuth + 3 flux -> 1 bismuth bronze + 3 slag
        moltenAlloy(writer, "bbronze", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER,  n * 8),
                                      new MaterialStack(MaterialType.BISMUTH, n),
                                      new MaterialStack(MaterialType.FLUX,    n * 3) },
                new MaterialStack[] { new MaterialStack(MaterialType.BBRONZE, i),
                                      new MaterialStack(MaterialType.SLAG,    n * 3) });

        // crucible.abronze — 8 copper + 1 arsenic + 3 flux -> 1 arsenic bronze + 3 slag
        moltenAlloy(writer, "abronze", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER,  n * 8),
                                      new MaterialStack(MaterialType.ARSENIC, n),
                                      new MaterialStack(MaterialType.FLUX,    n * 3) },
                new MaterialStack[] { new MaterialStack(MaterialType.ABRONZE, i),
                                      new MaterialStack(MaterialType.SLAG,    n * 3) });

        // crucible.cmb — 6 magtung + 3 mud -> 1 CMB ingot
        moltenAlloy(writer, "cmb", 3,
                new MaterialStack[] { new MaterialStack(MaterialType.MAGTUNG, n * 6),
                                      new MaterialStack(MaterialType.MUD,     n * 3) },
                new MaterialStack[] { new MaterialStack(MaterialType.CMB, i) });

        // crucible.magtung — 1 tungsten + 1 schrabidium nugget -> 1 magnetized tungsten
        moltenAlloy(writer, "magtung", 3,
                new MaterialStack[] { new MaterialStack(MaterialType.TUNGSTEN,    i),
                                      new MaterialStack(MaterialType.SCHRABIDIUM, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.MAGTUNG, i) });

        // crucible.bscco — 2 bismuth + 2 strontium + 2 calcium + 3 copper -> 1 BSCCO
        moltenAlloy(writer, "bscco", 3,
                new MaterialStack[] { new MaterialStack(MaterialType.BISMUTH,   n * 2),
                                      new MaterialStack(MaterialType.STRONTIUM, n * 2),
                                      new MaterialStack(MaterialType.CALCIUM,   n * 2),
                                      new MaterialStack(MaterialType.COPPER,    n * 3) },
                new MaterialStack[] { new MaterialStack(MaterialType.BSCCO, i) });
    }

    private static void moltenAlloy(Consumer<FinishedRecipe> writer, String name, int frequency,
                                    MaterialStack[] inputs, MaterialStack[] outputs) {
        new MoltenAlloyRecipeBuilder(inputs, outputs, frequency)
                .save(writer, "molten_alloy/" + name);
    }
}
//?}
