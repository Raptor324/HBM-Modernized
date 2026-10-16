package com.hbm_m.datagen.recipes.custom;
//? if forge {
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
 * Генератор JSON-рецептов кокера ({@code hbm_m:coker}).
 *
 * <p>Порт 32 рецептов из удалённого статического {@code CokerRecipes}. Auto-рецепты
 * ({@code registerAuto}: 820.000 TU = 1 бонусный кокс) рассчитывались в рантайме из
 * {@code FT_Flammable}/{@code FT_Combustible}-энергии жидкости по формуле
 * {@code mB = 820_000 * 1000 / max(tuFlammable, tuCombustible)} с округлением вниз до
 * 1000/100/10 mB и побочным выходом {@code max(10, mB / 10)}. Здесь эти значения
 * <b>запечены литералами 1:1</b> (трейты в датагене недоступны); каждое mB получено повторением
 * точной формулы поверх {@code ModFluidCalculatedFuel.apply()}. Жидкости без burn-трейтов
 * пропускались и в рантайме — в этом порте таких нет, все 24 auto + 1 SFauto рецепты на месте.
 * CALCIUM_SOLUTION-рецепт отсутствует и в оригинале порта (нет порошка кальция) — документированная лакуна.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class CokerRecipeGenerator {

    private CokerRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        ItemStack coke = new ItemStack(ModItems.COKE_PETROLEUM.get());

        // ── registerAuto (820.000 TU → 1 coke_petroleum; побочный выход = mB/10) ──────────
        auto(writer, "heavyoil",           ModFluids.HEAVYOIL,           11_000, ModFluids.OIL_COKER,      1_100, coke);
        auto(writer, "heavyoil_vacuum",    ModFluids.HEAVYOIL_VACUUM,     3_500, ModFluids.REFORMATE,        350, coke);
        auto(writer, "coalcreosote",       ModFluids.COALCREOSOTE,        3_200, ModFluids.NAPHTHA_COKER,   320, coke);
        auto(writer, "smear",              ModFluids.SMEAR,               7_900, ModFluids.OIL_COKER,       790, coke);
        auto(writer, "heatingoil",         ModFluids.HEATINGOIL,          1_600, ModFluids.OIL_COKER,       160, coke);
        auto(writer, "heatingoil_vacuum",  ModFluids.HEATINGOIL_VACUUM,     500, ModFluids.OIL_COKER,        50, coke);
        auto(writer, "reclaimed",          ModFluids.RECLAIMED,           5_800, ModFluids.NAPHTHA_COKER,  580, coke);
        auto(writer, "naphtha",            ModFluids.NAPHTHA,             4_900, ModFluids.NAPHTHA_COKER,  490, coke);
        auto(writer, "naphtha_ds",         ModFluids.NAPHTHA_DS,          2_400, ModFluids.NAPHTHA_COKER,  240, coke);
        auto(writer, "naphtha_crack",      ModFluids.NAPHTHA_CRACK,       6_400, ModFluids.NAPHTHA_COKER,  640, coke);
        auto(writer, "diesel",             ModFluids.DIESEL,                590, ModFluids.NAPHTHA_COKER,   59, coke);
        auto(writer, "diesel_reform",      ModFluids.DIESEL_REFORM,         230, ModFluids.NAPHTHA_COKER,   23, coke);
        auto(writer, "diesel_crack",       ModFluids.DIESEL_CRACK,          640, ModFluids.GAS_COKER,       64, coke);
        auto(writer, "diesel_crack_reform", ModFluids.DIESEL_CRACK_REFORM,  250, ModFluids.GAS_COKER,       25, coke);
        auto(writer, "lightoil",           ModFluids.LIGHTOIL,              370, ModFluids.GAS_COKER,       37, coke);
        auto(writer, "lightoil_ds",        ModFluids.LIGHTOIL_DS,           180, ModFluids.GAS_COKER,       18, coke);
        auto(writer, "lightoil_crack",     ModFluids.LIGHTOIL_CRACK,        590, ModFluids.GAS_COKER,       59, coke);
        auto(writer, "lightoil_vacuum",    ModFluids.LIGHTOIL_VACUUM,       180, ModFluids.GAS_COKER,       18, coke);
        auto(writer, "biofuel",            ModFluids.BIOFUEL,               650, ModFluids.GAS_COKER,       65, coke);
        auto(writer, "aromatics",          ModFluids.AROMATICS,           1_700, ModFluids.GAS_COKER,      170, coke);
        auto(writer, "reformate",          ModFluids.REFORMATE,              130, ModFluids.GAS_COKER,       13, coke);
        auto(writer, "xylene",             ModFluids.XYLENE,                 100, ModFluids.GAS_COKER,       10, coke);
        auto(writer, "fishoil",            ModFluids.FISHOIL,             10_000, ModFluids.MERCURY,      1_000, coke);
        auto(writer, "sunfloweroil",       ModFluids.SUNFLOWEROIL,        16_000, ModFluids.GAS_COKER,    1_600, coke);

        // ── registerSFAuto: древесное масло, 340.000 TU → 1 уголь (запечено литералом) ────
        auto(writer, "woodoil",            ModFluids.WOODOIL,             3_000, ModFluids.GAS_COKER,      300, new ItemStack(Items.CHARCOAL));

        // ── Ручные рецепты (registerRecipe) ─────────────────────────────────────────────
        // Watz -> грязевые слитки (без побочной жидкости).
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.WATZ, 4_000),
                new ItemStack(ModMaterialItems.item(ModMaterials.MUD, MaterialShape.INGOT), 4),
                null
        ).save(writer, "coker/watz");

        // Красный шлам -> железо + ртуть.
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.REDMUD, 450),
                new ItemStack(Items.IRON_INGOT, 1),
                fluid(ModFluids.MERCURY, 50)
        ).save(writer, "coker/redmud");

        // Битум -> кокс + кокеровое масло.
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.BITUMEN, 16_000),
                new ItemStack(ModItems.COKE_PETROLEUM.get()),
                fluid(ModFluids.OIL_COKER, 1_600)
        ).save(writer, "coker/bitumen");

        // Смазочное масло -> кокс + кокеровое масло.
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.LUBRICANT, 12_000),
                new ItemStack(ModItems.COKE_PETROLEUM.get()),
                fluid(ModFluids.OIL_COKER, 1_200)
        ).save(writer, "coker/lubricant");

        // Кислый газ -> сера + кокеровый газ.
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.SOURGAS, 1_000),
                new ItemStack(ModItems.SULFUR.get()),
                fluid(ModFluids.GAS_COKER, 150)
        ).save(writer, "coker/sourgas");

        // Шлам -> известняковый порошок + коллоид.
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.SLOP, 1_000),
                new ItemStack(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER)),
                fluid(ModFluids.COLLOID, 250)
        ).save(writer, "coker/slop");

        // Купорос -> железный порошок + серная кислота.
        CokerRecipeBuilder.cokerRecipe(
                fluid(ModFluids.VITRIOL, 4_000),
                new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER)),
                fluid(ModFluids.SULFURIC_ACID, 500)
        ).save(writer, "coker/vitriol");
    }

    /** Обёртка auto/SFauto-рецепта: вход (mB) → предмет + побочная жидкость (mB). */
    private static void auto(Consumer<FinishedRecipe> writer, String id, ModFluids.FluidEntry in,
                              int inMb, ModFluids.FluidEntry byproduct, int byMb, ItemStack output) {
        CokerRecipeBuilder.cokerRecipe(fluid(in, inMb), output, fluid(byproduct, byMb))
                .save(writer, "coker/" + id);
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
