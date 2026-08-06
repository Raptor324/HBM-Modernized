package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.ModFluids.FluidEntry;

import net.minecraft.world.level.material.Fluid;

/**
 * Direkter Java-Port von {@code CrackingRecipes} (1.7.10 Original, {@code com.hbm.inventory.recipes.CrackingRecipes}):
 * der Katalytische Cracker ({@code TileEntityMachineCatalyticCracker} im Original) wandelt 100mB eines
 * Oel-Fluids + 200mB Dampf in zwei leichtere Fraktionen + 2mB Restdampf (SPENTSTEAM) um.
 * Zweite Ausgabe kann {@link ModFluids#NONE} sein (Original: {@code Fluids.NONE}) - dann entfaellt Tank 3.
 */
public final class CrackingTowerRecipes {

    private CrackingTowerRecipes() {}

    public record Crack(Fluid outA, int amountA, Fluid outB, int amountB) {}

    public static final int STEAM_PER_100_INPUT = 200;
    public static final int SPENTSTEAM_PRODUCED = 2;

    private static final Map<Fluid, Crack> RECIPES = new HashMap<>();

    private static void put(FluidEntry in, FluidEntry outA, int amountA, FluidEntry outB, int amountB) {
        RECIPES.put(in.getSource(), new Crack(outA.getSource(), amountA, outB.getSource(), amountB));
    }

    static {
        put(ModFluids.OIL_BASE,          ModFluids.CRACKOIL,     80, ModFluids.PETROLEUM,     20);
        put(ModFluids.BITUMEN,           ModFluids.OIL_BASE,     80, ModFluids.AROMATICS,     20);
        put(ModFluids.SMEAR,             ModFluids.NAPHTHA,      60, ModFluids.PETROLEUM,     40);
        put(ModFluids.GAS,               ModFluids.PETROLEUM,    30, ModFluids.UNSATURATEDS,  20);
        put(ModFluids.DIESEL,            ModFluids.KEROSENE,     40, ModFluids.PETROLEUM,     30);
        put(ModFluids.DIESEL_CRACK,      ModFluids.KEROSENE,     40, ModFluids.PETROLEUM,     30);
        put(ModFluids.KEROSENE,          ModFluids.PETROLEUM,    60, ModFluids.NONE,           0);
        put(ModFluids.WOODOIL,           ModFluids.HEATINGOIL,   40, ModFluids.AROMATICS,     10);
        put(ModFluids.XYLENE,            ModFluids.AROMATICS,    80, ModFluids.PETROLEUM,     20);
        put(ModFluids.HEATINGOIL_VACUUM, ModFluids.HEATINGOIL,   80, ModFluids.REFORMGAS,     20);
        put(ModFluids.REFORMATE,         ModFluids.UNSATURATEDS, 40, ModFluids.REFORMGAS,     60);
        put(ModFluids.BIOGAS,            ModFluids.PETROLEUM,    20, ModFluids.AROMATICS,     20);
    }

    public static boolean has(Fluid fluid) {
        return RECIPES.containsKey(fluid);
    }

    public static Crack get(Fluid fluid) {
        return RECIPES.get(fluid);
    }

    public static Map<Fluid, Crack> getAll() {
        return Map.copyOf(RECIPES);
    }
}
