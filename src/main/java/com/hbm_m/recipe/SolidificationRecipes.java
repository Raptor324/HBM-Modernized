package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code SolidificationRecipes} (1.7.10 Original, {@code
 * com.hbm.inventory.recipes.SolidificationRecipes}). Feste Java-Map statt JSON-Rezeptsystem,
 * analog zu {@link CokerRecipes}/{@link PyroOvenRecipes}.
 * <p>
 * SCOPE-Entscheidung: Die Original-Teer-Rezepte ({@code oil_tar} mit {@code EnumTarType}
 * CRUDE/CRACK/COAL/WOOD/PARAFFIN fuer OIL/CRACKOIL/COALOIL/HEAVYOIL/HEAVYOIL_VACUUM/BITUMEN/
 * COALCREOSOTE/WOODOIL/LUBRICANT) entfallen ersatzlos - kein Teer-Item in diesem Port vorhanden.
 * SLOP -> Oelsand-Erz und SALIENT -> Bio-Waffel entfallen ebenso (fehlende Items/Bloecke).
 * MERCURY-Rezept nutzt {@link ModItems#NUGGET_MERCURY} statt eines (hier nicht existierenden)
 * Quecksilber-Barrens.
 */
public final class SolidificationRecipes {

    private SolidificationRecipes() {}

    public record Recipe(int fillMb, ItemStack output) {}

    private static final Map<Fluid, Recipe> RECIPES = new HashMap<>();

    private static void registerRecipe(FluidEntry fluid, int mb, ItemStack output) {
        RECIPES.put(fluid.getSource(), new Recipe(mb, output));
    }

    private static void registerSFAuto(FluidEntry fluid) {
        registerSFAuto(fluid, 1_440_000L, ModItems.SOLID_FUEL.get());
    }

    private static void registerSFAuto(FluidEntry fluid, long tuPerSF, Item output) {
        Fluid f = fluid.getSource();
        FT_Flammable flammable = FluidType.getTrait(f, FT_Flammable.class);
        if (flammable == null || flammable.getHeatEnergy() <= 0) return; // kein Trait hinterlegt - Rezept entfaellt.

        double penalty = 1.25D;
        int mB = (int) (tuPerSF * 1000L * penalty / flammable.getHeatEnergy());
        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);
        mB = Math.max(mB, 1);

        registerRecipe(fluid, mB, new ItemStack(output));
    }

    static {
        registerRecipe(ModFluids.WATER, 1000, new ItemStack(Items.ICE));
        registerRecipe(ModFluids.LAVA, 1000, new ItemStack(Items.OBSIDIAN));
        registerRecipe(ModFluids.MERCURY, 125, new ItemStack(ModItems.NUGGET_MERCURY.get()));
        registerRecipe(ModFluids.BIOGAS, 250, new ItemStack(ModItems.BIOMASS_COMPRESSED.get(), 4));
        registerRecipe(ModFluids.ENDERJUICE, 100, new ItemStack(Items.ENDER_PEARL));
        registerRecipe(ModFluids.WATZ, 1000, new ItemStack(ModItems.getIngot(ModIngots.MUD).get()));
        registerRecipe(ModFluids.REDMUD, 450, new ItemStack(Items.IRON_INGOT));
        registerRecipe(ModFluids.SODIUM, 100, new ItemStack(ModItems.POWDER_SODIUM.get()));
        registerRecipe(ModFluids.LEAD, 100, new ItemStack(ModItems.getIngot(ModIngots.LEAD).get()));

        registerRecipe(ModFluids.BALEFIRE, 250, new ItemStack(ModItems.SOLID_FUEL_BF.get()));

        registerSFAuto(ModFluids.SMEAR);
        registerSFAuto(ModFluids.HEATINGOIL);
        registerSFAuto(ModFluids.HEATINGOIL_VACUUM);
        registerSFAuto(ModFluids.RECLAIMED);
        registerSFAuto(ModFluids.PETROIL);
        registerSFAuto(ModFluids.NAPHTHA);
        registerSFAuto(ModFluids.NAPHTHA_CRACK);
        registerSFAuto(ModFluids.DIESEL);
        registerSFAuto(ModFluids.DIESEL_REFORM);
        registerSFAuto(ModFluids.DIESEL_CRACK);
        registerSFAuto(ModFluids.DIESEL_CRACK_REFORM);
        registerSFAuto(ModFluids.LIGHTOIL);
        registerSFAuto(ModFluids.LIGHTOIL_CRACK);
        registerSFAuto(ModFluids.LIGHTOIL_VACUUM);
        registerSFAuto(ModFluids.KEROSENE);
        registerSFAuto(ModFluids.KEROSENE_REFORM);
        registerSFAuto(ModFluids.SOURGAS);
        registerSFAuto(ModFluids.REFORMGAS);
        registerSFAuto(ModFluids.SYNGAS);
        registerSFAuto(ModFluids.PETROLEUM);
        registerSFAuto(ModFluids.LPG);
        registerSFAuto(ModFluids.BIOFUEL);
        registerSFAuto(ModFluids.AROMATICS);
        registerSFAuto(ModFluids.UNSATURATEDS);
        registerSFAuto(ModFluids.REFORMATE);
        registerSFAuto(ModFluids.XYLENE);
        registerSFAuto(ModFluids.BALEFIRE, 24_000_000L, ModItems.SOLID_FUEL_BF.get());
    }

    public static Recipe get(Fluid fluid) {
        return RECIPES.get(fluid);
    }

    public static Map<Fluid, Recipe> getAll() {
        return Map.copyOf(RECIPES);
    }
}
