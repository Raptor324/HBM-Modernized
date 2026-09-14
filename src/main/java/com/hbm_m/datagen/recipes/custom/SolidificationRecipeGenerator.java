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
 * Генератор JSON-рецептов солидификатора ({@code hbm_m:solidification}).
 *
 * <p>Порт рецептов из удалённого статического {@code SolidificationRecipes} (static-блок;
 * оригинал 1.7.10 — {@code com.hbm.inventory.recipes.SolidificationRecipes}): жидкость (mB) → предмет.</p>
 *
 * <p><b>Авто-рецепты запечены литералами.</b> Оригинал вычислял 27 «SF-auto» рецептов из
 * {@code FT_Flammable#getHeatEnergy()} жидкости по формуле
 * {@code mB = tuPerSF * 1000 * 1.25 / heatEnergy} (с округлением вниз до 1000/100/10 mB).
 * Трейты бутстрапятся рантаймом ({@code ModFluidTraitsBootstrap}/{@code ModFluidCalculatedFuel}) и
 * недоступны датагену, поэтому значения ниже — результат точного прогона той же формулы на тех же
 * константах ({@code tuPerSF = 1_440_000} TU/{@code SOLID_FUEL}, balefire — {@code 24_000_000} TU/
 * {@code SOLID_FUEL_BF}). Семантика «нет flammable-trait → нет рецепта» сохраняется тем, что
 * не-воспламеняющиеся жидкости здесь просто не портированы. Чистый ванильный 1.20.1 код внутри
 * {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class SolidificationRecipeGenerator {

    private SolidificationRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerManual(writer);
        registerSolidFuelAuto(writer);
    }

    /** 9 явных рецептов исходного static-блока. */
    private static void registerManual(Consumer<FinishedRecipe> writer) {
        put(writer, "water",       ModFluids.WATER,       1000, new ItemStack(Items.ICE));
        put(writer, "lava",        ModFluids.LAVA,        1000, new ItemStack(Items.OBSIDIAN));
        put(writer, "mercury",     ModFluids.MERCURY,      125, new ItemStack(ModItems.NUGGET_MERCURY.get()));
        put(writer, "biogas",      ModFluids.BIOGAS,       250, new ItemStack(ModItems.BIOMASS_COMPRESSED.get(), 4));
        put(writer, "enderjuice",  ModFluids.ENDERJUICE,   100, new ItemStack(Items.ENDER_PEARL));
        put(writer, "watz",        ModFluids.WATZ,        1000, new ItemStack(ModMaterialItems.item(ModMaterials.MUD, MaterialShape.INGOT)));
        put(writer, "redmud",      ModFluids.REDMUD,       450, new ItemStack(Items.IRON_INGOT));
        put(writer, "sodium",      ModFluids.SODIUM,       100, new ItemStack(ModItems.POWDER_SODIUM.get()));
        put(writer, "lead",        ModFluids.LEAD,         100, new ItemStack(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT)));
        // BALEFIRE 250 mB -> SOLID_FUEL_BF из manual-блока ПЕРЕЗАПИСЫВАЛСЯ в оригинале последним
        // registerSFAuto(BALEFIRE, 24_000_000L) — поэтому эмитим только финальное (auto) значение.
    }

    /** 27 auto-рецептов: литеральные значения формулы SF-auto (см. javadoc класса). */
    private static void registerSolidFuelAuto(Consumer<FinishedRecipe> writer) {
        ItemStack solidFuel = new ItemStack(ModItems.SOLID_FUEL.get());
        ItemStack solidFuelBf = new ItemStack(ModItems.SOLID_FUEL_BF.get());

        // heat=    82_500 → 21_000 mB
        put(writer, "smear",               ModFluids.SMEAR,               21_000, solidFuel);
        // heat=   391_000 →  4_600 mB (calculated; перекрывает структурные 150_000)
        put(writer, "heatingoil",          ModFluids.HEATINGOIL,           4_600, solidFuel);
        // heat= 1_310_000 →  1_300 mB
        put(writer, "heatingoil_vacuum",   ModFluids.HEATINGOIL_VACUUM,    1_300, solidFuel);
        // heat=   113_000 → 15_000 mB
        put(writer, "reclaimed",           ModFluids.RECLAIMED,           15_000, solidFuel);
        // heat=   130_000 → 13_000 mB
        put(writer, "petroil",             ModFluids.PETROIL,             13_000, solidFuel);
        // heat=   110_000 → 16_000 mB
        put(writer, "naphtha",             ModFluids.NAPHTHA,             16_000, solidFuel);
        // heat=    85_900 → 20_000 mB
        put(writer, "naphtha_crack",       ModFluids.NAPHTHA_CRACK,       20_000, solidFuel);
        // heat=   550_000 →  3_200 mB
        put(writer, "diesel",              ModFluids.DIESEL,               3_200, solidFuel);
        // heat= 1_370_000 →  1_300 mB
        put(writer, "diesel_reform",       ModFluids.DIESEL_REFORM,        1_300, solidFuel);
        // heat=   515_000 →  3_400 mB
        put(writer, "diesel_crack",        ModFluids.DIESEL_CRACK,         3_400, solidFuel);
        // heat= 1_280_000 →  1_400 mB
        put(writer, "diesel_crack_reform", ModFluids.DIESEL_CRACK_REFORM,  1_400, solidFuel);
        // heat= 1_460_000 →  1_200 mB
        put(writer, "lightoil",            ModFluids.LIGHTOIL,             1_200, solidFuel);
        // heat=   916_000 →  1_900 mB
        put(writer, "lightoil_crack",      ModFluids.LIGHTOIL_CRACK,       1_900, solidFuel);
        // heat= 3_000_000 →    600 mB
        put(writer, "lightoil_vacuum",     ModFluids.LIGHTOIL_VACUUM,        600, solidFuel);
        // heat= 2_560_000 →    700 mB
        put(writer, "kerosene",            ModFluids.KEROSENE,               700, solidFuel);
        // heat= 6_400_000 →    280 mB
        put(writer, "kerosene_reform",     ModFluids.KEROSENE_REFORM,        280, solidFuel);
        // heat=   250_000 →  7_200 mB
        put(writer, "sourgas",             ModFluids.SOURGAS,              7_200, solidFuel);
        // heat=10_500_000 →    170 mB
        put(writer, "reformgas",           ModFluids.REFORMGAS,              170, solidFuel);
        // heat= 1_650_000 →  1_000 mB
        put(writer, "syngas",              ModFluids.SYNGAS,               1_000, solidFuel);
        // heat= 1_650_000 →  1_000 mB
        put(writer, "petroleum",           ModFluids.PETROLEUM,            1_000, solidFuel);
        // heat= 1_810_000 →    990 mB
        put(writer, "lpg",                 ModFluids.LPG,                    990, solidFuel);
        // heat=   500_000 →  3_600 mB
        put(writer, "biofuel",             ModFluids.BIOFUEL,              3_600, solidFuel);
        // heat=   458_000 →  3_900 mB
        put(writer, "aromatics",           ModFluids.AROMATICS,           3_900, solidFuel);
        // heat= 3_660_000 →    490 mB
        put(writer, "unsaturateds",        ModFluids.UNSATURATEDS,           490, solidFuel);
        // heat= 2_400_000 →    750 mB
        put(writer, "reformate",           ModFluids.REFORMATE,              750, solidFuel);
        // heat= 3_150_000 →    570 mB
        put(writer, "xylene",              ModFluids.XYLENE,                 570, solidFuel);
        // heat=256_000_000 →    110 mB (tuPerSF = 24_000_000; перекрывает manual-запись 250 mB)
        put(writer, "balefire",            ModFluids.BALEFIRE,               110, solidFuelBf);
    }

    private static void put(Consumer<FinishedRecipe> writer, String id, ModFluids.FluidEntry fluid,
                            int amountMb, ItemStack output) {
        SolidificationRecipeBuilder.solidificationRecipe(FluidStack.create(fluid.getSource(), (long) amountMb), output)
                .save(writer, "solidification/" + id);
    }
}
//?}
