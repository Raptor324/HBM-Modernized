package com.hbm_m.recipe;

import java.util.List;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Central recipe registry for the crucible machine.
 *
 * <p>Modern port of legacy {@code CrucibleRecipes extends GenericRecipes<CrucibleRecipe>}.
 * In the legacy code this class was both the alloying-recipe list and the source of
 * dynamically-generated smelting / mold recipes. Here the three recipe types are held in
 * separate dedicated registries:
 * <ul>
 *   <li>{@link CrucibleAlloyingRecipes} — ordered list of {@link CrucibleAlloyingRecipe}s</li>
 *   <li>{@link CrucibleSmeltingRecipes} — input → output(s) smelting map</li>
 *   <li>{@link CrucibleMoldRecipes}     — [material, mold, placeholder, output] arrays</li>
 * </ul>
 *
 * <p>This class acts as a façade so call-sites that use {@code CrucibleRecipes.INSTANCE} or
 * the static helper methods continue to compile after the port.
 *
 * <p><b>MaterialStack / Mats TODO:</b> All {@code registerDefaults()} entries that depend on
 * {@code MaterialStack}, {@code Mats.*}, {@code NTMMaterial}, {@code MaterialShapes},
 * {@code ItemScraps}, or {@code OreDictionary} are stubbed with TODO comments.
 * Restore them once {@code com.hbm_m.inventory.material} is ported.
 */
public class CrucibleRecipes {

    /** Singleton — mirrors legacy {@code CrucibleRecipes.INSTANCE}. */
    public static final CrucibleRecipes INSTANCE = new CrucibleRecipes();
    private static boolean defaultsRegistered = false;

    private CrucibleRecipes() {}

    // -------------------------------------------------------------------------
    // Default recipe registration
    // -------------------------------------------------------------------------

    /**
     * Registers all built-in crucible recipes.
     * Call once during mod initialisation (e.g. from the common setup event).
     *
     * <p>Legacy method: {@code CrucibleRecipes.INSTANCE.registerDefaults()}.
     */
    public void registerDefaults() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;

        // Temporary seed recipes so JEI categories are not empty until MaterialStack/Mats is ported.
        CrucibleAlloyingRecipes.register(new CrucibleAlloyingRecipe("crucible.demo_alloy")
            .setup(2, new ItemStack(Items.IRON_INGOT))
            .inputs(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COAL))
            .outputs(new ItemStack(Items.IRON_NUGGET, 3)));

        CrucibleSmeltingRecipes.register(
            new ItemStack(Items.IRON_ORE),
            List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.STONE)));

        CrucibleSmeltingRecipes.register(
            new ItemStack(Items.IRON_INGOT),
            List.of(new ItemStack(Items.IRON_INGOT)));

        CrucibleSmeltingRecipes.register(
            new ItemStack(Items.COPPER_INGOT),
            List.of(new ItemStack(Items.COPPER_INGOT)));

        CrucibleMoldRecipes.register(
            new ItemStack(Items.CLAY_BALL),
            new ItemStack(Items.BRICK),
            new ItemStack(Items.FLOWER_POT));

        /* -----------------------------------------------------------------------
         * ALLOYING RECIPES
         * Legacy source: CrucibleRecipes.registerDefaults(), each this.register(...)
         *
         * All entries below are stubs.  Once MaterialStack / Mats is ported:
         *   1. Replace ItemStack inputs/outputs with MaterialStack arrays.
         *   2. Use MaterialShapes.NUGGET.q(1) / INGOT.q(1) for amounts.
         *   3. Restore Compat.isModLoaded(Compat.MOD_GT6) conditional block.
         *   4. Use ModItems.* for icons.
         * ----------------------------------------------------------------------- */

        // crucible.steel — 2× iron nugget + carbon nugget → 2× steel nugget
        // TODO: CrucibleAlloyingRecipes.register(new CrucibleAlloyingRecipe("crucible.steel")
        //         .setup(2, new ItemStack(ModItems.INGOT_STEEL.get()))
        //         .inputs(new MaterialStack(Mats.MAT_IRON, n*2), new MaterialStack(Mats.MAT_CARBON, n))
        //         .outputs(new MaterialStack(Mats.MAT_STEEL, n*2)));

        // crucible.hematite — 2× hematite ingot + 2× flux nugget → iron ingot + 3× slag nugget
        // TODO: CrucibleAlloyingRecipes.register(new CrucibleAlloyingRecipe("crucible.hematite")
        //         .setup(6, DictFrame.fromOne(ModBlocks.STONE_RESOURCE.get(), EnumStoneType.HEMATITE))
        //         .inputs(new MaterialStack(Mats.MAT_HEMATITE, i*2), new MaterialStack(Mats.MAT_FLUX, n*2))
        //         .outputs(new MaterialStack(Mats.MAT_IRON, i), new MaterialStack(Mats.MAT_SLAG, n*3)));

        // crucible.malachite — 2× malachite + 2× flux → copper + slag
        // TODO: register similarly

        // crucible.redcopper — copper + redstone → mingrade
        // crucible.aa — steel + mingrade → advanced alloy
        // crucible.hss — 5× steel + 3× tungsten + cobalt → dura steel ×9
        // crucible.ferro — 2× steel + U238 → ferrouranium ×3
        // crucible.tcalloy — 8× steel + technetium → tcalloy ingot
        // crucible.cdalloy — 8× steel + cadmium → cdalloy ingot
        // crucible.bbronze — 8× copper + bismuth + 3× flux → bismuth bronze + slag
        // crucible.abronze — 8× copper + arsenic + 3× flux → arsenic bronze + slag
        // crucible.cmb — 6× magtung + 3× mud → combine steel ingot
        // crucible.magtung — tungsten ingot + schrabidium nugget → magnetized tungsten ingot
        // crucible.bscco — 2×Bi + 2×Sr + 2×Ca + 3×Cu → bscco ingot
        // TODO: register all of the above once MaterialStack / Mats is ported

        /* -----------------------------------------------------------------------
         * MOLD (CASTING) RECIPES
         * Legacy source: CrucibleRecipes.registerMoldsForNEI()
         * Iterates Mats.orderedList × ItemMold.molds to generate scrap→mold→output entries.
         * TODO: call CrucibleMoldRecipes.register(...) here once NTMMaterial / ItemMold ported.
         * ----------------------------------------------------------------------- */

        /* -----------------------------------------------------------------------
         * SMELTING RECIPES
         * Legacy source: CrucibleRecipes.getSmeltingRecipes() (dynamic, not stored in INSTANCE).
         * Smelting recipes are generated at query-time from Mats.orderedList,
         * Mats.materialOreEntries and Mats.materialEntries.
         * TODO: populate CrucibleSmeltingRecipes once NTMMaterial / Mats / ItemScraps ported.
         * ----------------------------------------------------------------------- */
    }

    // -------------------------------------------------------------------------
    // Accessor façade — delegates to the split registries
    // -------------------------------------------------------------------------

    /**
     * Returns all alloying recipes in insertion order.
     * Mirrors legacy usage of {@code CrucibleRecipes.INSTANCE.recipeOrderedList}.
     */
    public List<CrucibleAlloyingRecipe> getAlloyingRecipes() {
        return CrucibleAlloyingRecipes.getRecipes();
    }

    /**
     * Returns the smelting recipe map (input ItemStack → list of output ItemStacks).
     * Mirrors legacy {@code CrucibleRecipes.getSmeltingRecipes()}.
     *
     * <p>TODO: replace ItemStack keys with AStack/OreDict keys once RecipesCommon is ported.
     */
    public static Map<ItemStack, List<ItemStack>> getSmeltingRecipes() {
        return CrucibleSmeltingRecipes.getRecipes();
    }

    /**
     * Returns all mold-casting recipes as [material, mold, placeholder, output] arrays.
     * Mirrors legacy {@code CrucibleRecipes.getMoldRecipes()}.
     */
    public static List<ItemStack[]> getMoldRecipes() {
        return CrucibleMoldRecipes.getMoldRecipes();
    }
}
