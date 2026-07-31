package com.hbm_m.recipe;

import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of crucible alloying reactions (molten material → molten material[]).
 * Port of the alloying half of legacy {@code CrucibleRecipes.registerDefaults()}.
 *
 * <p>Only alloys expressible with materials that already exist as real items in
 * this mod are registered here (no invented ores/items). Left out entirely:
 * <ul>
 *   <li>{@code crucible.ferro} (steel + U-238 → ferrouranium) — no ferrouranium
 *       item exists to cast, and U-238 nuggets/billets are reactor fuel items
 *       elsewhere; repurposing them as a smelting input risks conflicting with
 *       that intended use.</li>
 *   <li>{@code crucible.cdalloy} (steel + cadmium) — no cadmium item exists.</li>
 *   <li>{@code crucible.cmb} (magnetized tungsten + mud) — no magnetized
 *       tungsten ingot/nugget exists (only wire/coil items), and mud is a
 *       fluid bucket, not a solid smeltable item.</li>
 *   <li>{@code crucible.magtung} (tungsten + schrabidium) — same missing
 *       magnetized-tungsten-ingot problem.</li>
 *   <li>{@code crucible.bscco} (bismuth + strontium + calcium + copper) — no
 *       BSCCO, strontium or calcium items exist.</li>
 * </ul>
 * All of the above would require adding brand new items, which is out of
 * scope for a recipe port.
 */
public class MoltenAlloyRecipes {

    private static final List<MoltenAlloyRecipe> RECIPES = new ArrayList<>();
    private static boolean defaultsRegistered = false;

    public static void register(MoltenAlloyRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static List<MoltenAlloyRecipe> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static void registerDefaults() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;

        int n = MaterialStack.MB_PER_NUGGET;
        int i = MaterialStack.MB_PER_INGOT;

        // crucible.steel — 2 iron + 3 carbon -> 2 steel (flux omitted, no flux material in this port)
        register(new MoltenAlloyRecipe("crucible.steel", 20,
                new MaterialStack[] { new MaterialStack(MaterialType.IRON, n * 2), new MaterialStack(MaterialType.CARBON, n * 3) },
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL, n * 2) }));

        // crucible.hss — 5 steel + 3 tungsten + 1 cobalt -> 9 dura steel
        register(new MoltenAlloyRecipe("crucible.hss", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL, n * 5), new MaterialStack(MaterialType.TUNGSTEN, n * 3), new MaterialStack(MaterialType.COBALT, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.DURA_STEEL, n * 9) }));

        // crucible.tcalloy — 8 steel + 1 technetium -> 1 tcalloy ingot
        register(new MoltenAlloyRecipe("crucible.tcalloy", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.STEEL, n * 8), new MaterialStack(MaterialType.TECHNETIUM, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.TCALLOY, i) }));

        // crucible.bbronze — 8 copper + 1 bismuth -> 1 bismuth bronze ingot (flux/slag omitted)
        register(new MoltenAlloyRecipe("crucible.bbronze", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER, n * 8), new MaterialStack(MaterialType.BISMUTH, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.BBRONZE, i) }));

        // crucible.abronze — 8 copper + 1 arsenic -> 1 arsenic bronze ingot (flux/slag omitted)
        register(new MoltenAlloyRecipe("crucible.abronze", 9,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER, n * 8), new MaterialStack(MaterialType.ARSENIC, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.ABRONZE, i) }));

        // crucible.redcopper — 1 copper + 1 redstone -> 2 mingrade (mass conserving, matches original 1:1:2 ratio)
        register(new MoltenAlloyRecipe("crucible.redcopper", 2,
                new MaterialStack[] { new MaterialStack(MaterialType.COPPER, n), new MaterialStack(MaterialType.REDSTONE, n) },
                new MaterialStack[] { new MaterialStack(MaterialType.MINGRADE, n * 2) }));
    }
}
