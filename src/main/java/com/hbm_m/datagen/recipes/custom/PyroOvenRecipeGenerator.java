package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.worldgen.BedrockOreDensity.Type;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов пиро-печи ({@code hbm_m:pyro_oven}).
 *
 * <p>Порт 69 рецептов из удалённого статического {@code PyroOvenRecipes}:</p>
 * <ul>
 *   <li>27 «Solid Fuel»-auto-рецептов: {@code mB = 1_440_000 * 1000 * 0.5 / FT_Flammable-энергия}
 *       (BALEFIRE: {@code 24_000_000 TU} → {@code SOLID_FUEL_BF}), с округлением вниз до 1000/100/10 mB,
 *       {@code max(mB, 1)}, длительность 60. Трейты в датагене недоступны — значения <b>запечены
 *       литералами 1:1</b>, каждое получено повторением точной формулы поверх
 *       {@code ModFluidCalculatedFuel.apply()}. У всех исходных жидкостей трейт был — пропусков нет.</li>
 *   <li>30 roast-рецептов bedrock-руд: цикл 6 {@link Type} × 5 Grade-пар (предмет → roasted-предмет +
 *       50 mB купороса, длительность 10) — цикл сохранён, JSON пишется по одному файлу на рецепт.</li>
 *   <li>12 ручных рецептов (Syngas/Wolframcarbid, Kohle → Syngas/Kohlegas/Schweröl, Biomasse, Reformgas,
 *       Erdgas) — 1:1 из статического блока.</li>
 * </ul>
 *
 * <p><b>Порядок имеет значение:</b> оригинал проверял рецепты по порядку, первым побеждал
 * безжидкостный coal/coke-рецепт (он «затенял» жидкостные варианты с тем же предметом). Имена файлов
 * подобраны так, чтобы при алфавитной сортировке путей {@code coal_all_*} шли раньше
 * {@code coalgas_*}/{@code heavyoil_*} — поведение 1:1.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class PyroOvenRecipeGenerator {

    private static final int SF_DURATION = 60;

    private PyroOvenRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerSolidFuel(writer);
        registerBedrockRoasts(writer);
        registerManual(writer);
    }

    // ─── Solid Fuel из горючих жидкостей (запеченные auto-значения) ───────────────────

    private static void registerSolidFuel(Consumer<FinishedRecipe> writer) {
        ItemStack solidFuel = new ItemStack(ModItems.SOLID_FUEL.get());

        sf(writer, "smear",               ModFluids.SMEAR,               8_700, solidFuel);
        sf(writer, "heatingoil",          ModFluids.HEATINGOIL,          1_800, solidFuel);
        sf(writer, "heatingoil_vacuum",   ModFluids.HEATINGOIL_VACUUM,     540, solidFuel);
        sf(writer, "reclaimed",           ModFluids.RECLAIMED,           6_300, solidFuel);
        sf(writer, "petroil",             ModFluids.PETROIL,             5_500, solidFuel);
        sf(writer, "naphtha",             ModFluids.NAPHTHA,             6_500, solidFuel);
        sf(writer, "naphtha_crack",       ModFluids.NAPHTHA_CRACK,       8_300, solidFuel);
        sf(writer, "diesel",              ModFluids.DIESEL,              1_300, solidFuel);
        sf(writer, "diesel_reform",       ModFluids.DIESEL_REFORM,         520, solidFuel);
        sf(writer, "diesel_crack",        ModFluids.DIESEL_CRACK,        1_300, solidFuel);
        sf(writer, "diesel_crack_reform", ModFluids.DIESEL_CRACK_REFORM,   560, solidFuel);
        sf(writer, "lightoil",            ModFluids.LIGHTOIL,              490, solidFuel);
        sf(writer, "lightoil_crack",      ModFluids.LIGHTOIL_CRACK,        780, solidFuel);
        sf(writer, "lightoil_vacuum",     ModFluids.LIGHTOIL_VACUUM,       240, solidFuel);
        sf(writer, "kerosene",            ModFluids.KEROSENE,              280, solidFuel);
        sf(writer, "kerosene_reform",     ModFluids.KEROSENE_REFORM,       110, solidFuel);
        sf(writer, "sourgas",             ModFluids.SOURGAS,             2_800, solidFuel);
        sf(writer, "reformgas",           ModFluids.REFORMGAS,              68, solidFuel);
        sf(writer, "syngas",              ModFluids.SYNGAS,                430, solidFuel);
        sf(writer, "petroleum",           ModFluids.PETROLEUM,             430, solidFuel);
        sf(writer, "lpg",                 ModFluids.LPG,                   390, solidFuel);
        sf(writer, "biofuel",             ModFluids.BIOFUEL,             1_400, solidFuel);
        sf(writer, "aromatics",           ModFluids.AROMATICS,           1_500, solidFuel);
        sf(writer, "unsaturateds",        ModFluids.UNSATURATEDS,          190, solidFuel);
        sf(writer, "reformate",           ModFluids.REFORMATE,             300, solidFuel);
        sf(writer, "xylene",              ModFluids.XYLENE,                220, solidFuel);
        // Balefire: 24.000.000 TU → SOLID_FUEL_BF (запечено литералом).
        sf(writer, "balefire",            ModFluids.BALEFIRE,               46, new ItemStack(ModItems.SOLID_FUEL_BF.get()));
    }

    /** Жидкость (mB) → предмет, длительность 60 — обёртка бывшего registerSFAuto. */
    private static void sf(Consumer<FinishedRecipe> writer, String id, ModFluids.FluidEntry in, int mB, ItemStack output) {
        PyroOvenRecipeBuilder.pyroOvenRecipe(fluid(in, mB), null, 1, output, null, SF_DURATION)
                .save(writer, "pyro_oven/sf_" + id);
    }

    // ─── Bedrock-Erz-Röstung: 6 Type × 5 Grade-Paare = 30 Rezepte ─────────────────────

    private static void registerBedrockRoasts(Consumer<FinishedRecipe> writer) {
        for (Type type : Type.values()) {
            roast(writer, Grade.BASE, Grade.BASE_ROASTED, type);
            roast(writer, Grade.PRIMARY, Grade.PRIMARY_ROASTED, type);
            roast(writer, Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ROASTED, type);
            roast(writer, Grade.SOLVENT_BYPRODUCT, Grade.SOLVENT_ROASTED, type);
            roast(writer, Grade.RAD_BYPRODUCT, Grade.RAD_ROASTED, type);
        }
    }

    /** Предмет (1 шт.) → roasted-предмет + 50 mB купороса, длительность 10. */
    private static void roast(Consumer<FinishedRecipe> writer, Grade rawGrade, Grade roastedGrade, Type type) {
        ItemStack in = bedrockOre(rawGrade, type);
        ItemStack out = bedrockOre(roastedGrade, type);
        if (in.isEmpty() || out.isEmpty()) return; // Item fehlt in diesem Port - Rezept entfaellt (wie im Runtime-Original).
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                null,
                Ingredient.of(in.getItem()), 1,
                out,
                fluid(ModFluids.VITRIOL, 50),
                10
        ).save(writer, "pyro_oven/roast_" + rawGrade.key + "_" + type.name().toLowerCase(Locale.ROOT));
    }

    /** Registry-Lookup 1:1 wie im Original: {@code bedrock_ore_<grade>_<type>}. */
    private static ItemStack bedrockOre(Grade grade, Type type) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m",
                "bedrock_ore_" + grade.key + "_" + type.name().toLowerCase(Locale.ROOT));
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ─── Ручные рецепты ───────────────────────────────────────────────────────────────

    private static void registerManual(Consumer<FinishedRecipe> writer) {
        // Syngas + Wolframpulver -> Wolframcarbid + verbrauchter Dampf (Steam:Syngas 1:2, Syngas:LPS 2:1).
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.SYNGAS, 2_000),
                Ingredient.of(ModItems.getPowder(ModIngots.TUNGSTEN).get()), 1,
                new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get()),
                fluid(ModFluids.SPENTSTEAM, 1_000),
                300
        ).save(writer, "pyro_oven/tungsten_carbide");

        // Syngas aus Kohle (ohne Fluid-Anforderung - «coal_all_*» sortiert vor coalgas_*/heavyoil_*,
        // damit die Schatten-Reihenfolge des Originals erhalten bleibt).
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                null, Ingredient.of(Items.COAL), 1,
                null, fluid(ModFluids.SYNGAS, 1_000), 100
        ).save(writer, "pyro_oven/coal_all_to_syngas");
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                null, Ingredient.of(ModItems.getPowders(ModPowders.COAL).get()), 1,
                null, fluid(ModFluids.SYNGAS, 1_000), 100
        ).save(writer, "pyro_oven/coal_all_powder_to_syngas");
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                null, Ingredient.of(ModItems.COKE_PETROLEUM.get()), 1,
                null, fluid(ModFluids.SYNGAS, 1_000), 100
        ).save(writer, "pyro_oven/coal_all_coke_to_syngas");

        // Syngas aus Biomasse (+ Holzkohle-Nebenprodukt).
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                null, Ingredient.of(ModItems.BIOMASS.get()), 4,
                new ItemStack(Items.CHARCOAL), fluid(ModFluids.SYNGAS, 1_000), 100
        ).save(writer, "pyro_oven/biomass_to_charcoal");

        // Schweroel aus Kohle (Wasserstoff + Kohle).
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.HYDROGEN, 500), Ingredient.of(Items.COAL), 1,
                null, fluid(ModFluids.HEAVYOIL, 1_000), 100
        ).save(writer, "pyro_oven/heavyoil_from_hydrogen_coal");
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.HYDROGEN, 500), Ingredient.of(ModItems.getPowders(ModPowders.COAL).get()), 1,
                null, fluid(ModFluids.HEAVYOIL, 1_000), 100
        ).save(writer, "pyro_oven/heavyoil_from_hydrogen_coal_powder");

        // Kohlegas aus Kohle (Schweroel + Kohle).
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.HEAVYOIL, 500), Ingredient.of(Items.COAL), 1,
                null, fluid(ModFluids.COALGAS, 1_000), 50
        ).save(writer, "pyro_oven/coalgas_from_coal");
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.HEAVYOIL, 500), Ingredient.of(ModItems.getPowders(ModPowders.COAL).get()), 1,
                null, fluid(ModFluids.COALGAS, 1_000), 50
        ).save(writer, "pyro_oven/coalgas_from_coal_powder");
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.HEAVYOIL, 500), Ingredient.of(ModItems.COKE_PETROLEUM.get()), 1,
                null, fluid(ModFluids.COALGAS, 1_000), 50
        ).save(writer, "pyro_oven/coalgas_from_coke");

        // Reformgas aus Koker-Gas.
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.GAS_COKER, 4_000), null, 1,
                null, fluid(ModFluids.REFORMGAS, 100), 60
        ).save(writer, "pyro_oven/reformgas_from_gas_coker");

        // Wasserstoff + Graphit aus Erdgas.
        PyroOvenRecipeBuilder.pyroOvenRecipe(
                fluid(ModFluids.GAS, 12_000), null, 1,
                new ItemStack(ModItems.getIngot(ModIngots.GRAPHITE).get()),
                fluid(ModFluids.HYDROGEN, 8_000), 60
        ).save(writer, "pyro_oven/hydrogen_from_gas");
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
