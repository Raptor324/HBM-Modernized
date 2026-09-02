package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов рудного окислителя ({@code hbm_m:crystallizer}).
 *
 * <p>Порт раскомментированных рецептов из удалённого статического {@code CrystallizerRecipes.registerDefaults()}.
 * TODO-рецепты (предметы/блоки, ещё не портированные в мод) намеренно пропущены — раскомментировать
 * и поправить ссылки, когда соответствующий предмет появится. Рецепты bedrock-ore ( addItemAny / addItem
 * с slop1000@64) также не перенесены за ненадобностью для data-driven flows.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные «кислоты» создаются через {@link FluidStack#create} из {@link ModFluids} (mB).</p>
 *
 * <p><b>Соответствие оригиналу:</b></p>
 * <ul>
 *   <li>{@code BASE_TIME=600} — большинство рудных рецептов (addOreTag по умолчанию).</li>
 *   <li>{@code UTILITY_TIME=100} — лёгкая обработка (ROTTEN_FLESH→LEATHER, COBBLESTONE→REINFORCED_STONE).</li>
 *   <li>{@code MIXING_TIME=20} — смешивание (BONE→16 SLIME, BLACK_DYE→4 SLIME).</li>
 *   <li>{@code peroxide=PEROXIDE@500}, {@code sulfur=SULFURIC_ACID@500}.</li>
 *   <li>{@code productivity=0.05} для всех рудных, 0.25 — для ROTTEN_FLESH→LEATHER.</li>
 * </ul>
 */
public final class CrystallizerRecipeGenerator {

    // Длительности (прямые порты констант из CrystallizerRecipes).
    private static final int BASE_TIME    = 600;  // 30 сек — большинство рудных рецептов
    private static final int UTILITY_TIME = 100;  //  5 сек — лёгкая обработка / превращения
    private static final int MIXING_TIME  = 20;   //  1 сек — смешивания

    // Жидкости по умолчанию (прямые порты из CrystallizerRecipes.registerDefaults).
    private static final FluidStack PEROXIDE = fluid(ModFluids.PEROXIDE,       500);
    private static final FluidStack SULFUR   = fluid(ModFluids.SULFURIC_ACID, 500);

    private CrystallizerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // ═══════════════════════════════════════════════════════════════════
        // БАЗОВЫЕ РУДЫ (перекись водорода 500 mB, baseTime, prod 0.05)
        // ═══════════════════════════════════════════════════════════════════
        ore(writer, "coal",        "forge:ores/coal",       ModMaterialItems.item(ModMaterials.COAL, MaterialShape.CRYSTAL));
        ore(writer, "iron",        "forge:ores/iron",       ModMaterialItems.item(ModMaterials.IRON, MaterialShape.CRYSTAL));
        ore(writer, "gold",        "forge:ores/gold",       ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.CRYSTAL));
        ore(writer, "redstone",    "forge:ores/redstone",   ModMaterialItems.item(ModMaterials.REDSTONE, MaterialShape.CRYSTAL));
        ore(writer, "lapis",       "forge:ores/lapis",      ModMaterialItems.item(ModMaterials.LAPIS, MaterialShape.CRYSTAL));
        ore(writer, "diamond",     "forge:ores/diamond",    ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.CRYSTAL));
        ore(writer, "copper",      "forge:ores/copper",     ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.CRYSTAL));
        ore(writer, "sulfur",      "forge:ores/sulfur",     ModMaterialItems.item(ModMaterials.SULFUR, MaterialShape.CRYSTAL));
        ore(writer, "niter",       "forge:ores/niter",      ModMaterialItems.item(ModMaterials.NITER, MaterialShape.CRYSTAL));
        ore(writer, "aluminum",    "forge:ores/aluminum",   ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.CRYSTAL));
        ore(writer, "fluorite",    "forge:ores/fluorite",   ModMaterialItems.item(ModMaterials.FLUORITE, MaterialShape.CRYSTAL));
        ore(writer, "beryllium",   "forge:ores/beryllium",  ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.CRYSTAL));
        ore(writer, "lead",        "forge:ores/lead",       ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.CRYSTAL));
        ore(writer, "cinnabar",    "forge:ores/cinnabar",   ModMaterialItems.item(ModMaterials.CINNEBAR, MaterialShape.CRYSTAL));

        // ═══════════════════════════════════════════════════════════════════
        // РАДИОАКТИВНЫЕ / ТУГОПЛАВКИЕ РУДЫ (серная кислота 500 mB, baseTime, prod 0.05)
        // ═══════════════════════════════════════════════════════════════════
        ore(writer, "uranium",     "forge:ores/uranium",     ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.CRYSTAL),     SULFUR);
        ore(writer, "thorium",     "forge:ores/thorium",     ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.CRYSTAL),     SULFUR);
        ore(writer, "plutonium",   "forge:ores/plutonium",   ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.CRYSTAL),   SULFUR);
        ore(writer, "titanium",    "forge:ores/titanium",    ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.CRYSTAL),     SULFUR);
        ore(writer, "tungsten",    "forge:ores/tungsten",    ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.CRYSTAL),     SULFUR);
        ore(writer, "lithium",     "forge:ores/lithium",     ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.CRYSTAL),     SULFUR);
        ore(writer, "cobalt",      "forge:ores/cobalt",      ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.CRYSTAL),      SULFUR);
        ore(writer, "schrabidium", "forge:ores/schrabidium", ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL), SULFUR);

        // Редкоземельные руды (forge:ores/rareground) — серная.
        ore(writer, "rareground", "forge:ores/rareground", ModMaterialItems.item(ModMaterials.RARE, MaterialShape.CRYSTAL), SULFUR);

        // ═══════════════════════════════════════════════════════════════════
        // УТИЛИТАРНЫЕ ПРЕОБРАЗОВАНИЯ
        // ═══════════════════════════════════════════════════════════════════
        // Гнилое мясо → кожа (utilityTime, prod 0.25, перекись).
        CrystallizerRecipeBuilder.crystallizerRecipe(
                        Items.ROTTEN_FLESH, 1, PEROXIDE,
                        new ItemStack(Items.LEATHER), UTILITY_TIME, 0.25f)
                .save(writer, "crystallizer/rotten_flesh_to_leather");

        // Булыжник → укреплённый камень (utilityTime, перекись).
        CrystallizerRecipeBuilder.crystallizerRecipe(
                        Items.COBBLESTONE, 1, PEROXIDE,
                        new ItemStack(ModBlocks.REINFORCED_STONE.get()), UTILITY_TIME, 0f)
                .save(writer, "crystallizer/cobblestone_to_reinforced_stone");

        // Кость → 16 слизи (mixing, серная 1000 mB).
        CrystallizerRecipeBuilder.crystallizerRecipe(
                        Items.BONE, 1, fluid(ModFluids.SULFURIC_ACID, 1_000),
                        new ItemStack(Items.SLIME_BALL, 16), MIXING_TIME, 0f)
                .save(writer, "crystallizer/bone_to_slime");

        // Чёрный краситель → 4 слизи (mixing, серная 250 mB).
        CrystallizerRecipeBuilder.crystallizerRecipe(
                        Items.BLACK_DYE, 1, fluid(ModFluids.SULFURIC_ACID, 250),
                        new ItemStack(Items.SLIME_BALL, 4), MIXING_TIME, 0f)
                .save(writer, "crystallizer/black_dye_to_slime");
    }

    /** Руда по forge-тегу с перекисью 500 mB, baseTime, productivity 0.05 (базовый случай). */
    private static void ore(Consumer<FinishedRecipe> writer, String name, String tagId,
                            net.minecraft.world.item.Item output) {
        ore(writer, name, tagId, new ItemStack(output), PEROXIDE);
    }

    /** Руда по forge-тегу с заданной кислотой, baseTime, productivity 0.05. */
    private static void ore(Consumer<FinishedRecipe> writer, String name, String tagId,
                            net.minecraft.world.item.Item output, FluidStack acid) {
        ore(writer, name, tagId, new ItemStack(output), acid);
    }

    /** Руда по forge-тегу с заданной кислотой, baseTime, productivity 0.05 (ItemStack-перегрузка). */
    private static void ore(Consumer<FinishedRecipe> writer, String name, String tagId,
                            ItemStack output, FluidStack acid) {
        CrystallizerRecipeBuilder.crystallizerRecipe(
                        tagId, 1, acid, output, BASE_TIME, 0.05f)
                .save(writer, "crystallizer/ore_" + name);
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
