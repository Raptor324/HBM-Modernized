package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * Port of {@code ElectrolyserFluidRecipes} + {@code ElectrolyserMetalRecipes} (1.7.10 Original),
 * combined into one file since both feed the same machine's two operating modes.
 * <p>
 * Simplifications:
 * <ul>
 *   <li>Metal-mode outputs are direct item stacks instead of the original's molten-metal
 *       Crucible-pour visual (same simplification used elsewhere this session, e.g. Rotary Furnace) -
 *       both material outputs are produced as real ingots immediately rather than accumulated as
 *       a pourable buffer.</li>
 *   <li>The original's NUGGET-shaped secondary outputs are approximated as a reduced ingot count
 *       (nugget = 1/9 ingot conventionally) rather than adding a separate nugget item system.</li>
 *   <li>The Bedrock Ore PRIMARY_FIRST/PRIMARY_SECOND/CRUMBS electrolysis path from the original is
 *       skipped here - this port's Centrifuge already provides an equivalent path to the same raw
 *       materials for those grades (see {@code CentrifugeRecipes}), so no progression is blocked.</li>
 *   <li>Byproduct items with no directly-matching name in this port use the closest equivalent
 *       (e.g. {@code NUGGET_MERCURY} instead of a nonexistent {@code ingot_mercury}).</li>
 * </ul>
 */
public final class ElectrolyserRecipes {

    private ElectrolyserRecipes() {}

    // ==================== Fluid mode ====================

    public record FluidRecipe(int amount, Fluid outA, int fillA, Fluid outB, int fillB, ItemStack[] byproducts) {}

    private static final Map<Fluid, FluidRecipe> FLUID_RECIPES = new HashMap<>();

    private static void putFluid(com.hbm_m.inventory.fluid.ModFluids.FluidEntry in, int amount,
                                  com.hbm_m.inventory.fluid.ModFluids.FluidEntry outA, int fillA,
                                  com.hbm_m.inventory.fluid.ModFluids.FluidEntry outB, int fillB,
                                  ItemStack... byproducts) {
        FLUID_RECIPES.put(in.getSource(), new FluidRecipe(amount, outA.getSource(), fillA, outB.getSource(), fillB, byproducts));
    }

    static {
        putFluid(ModFluids.WATER, 2_000, ModFluids.HYDROGEN, 200, ModFluids.OXYGEN, 200);
        putFluid(ModFluids.HEAVYWATER, 2_000, ModFluids.DEUTERIUM, 200, ModFluids.OXYGEN, 200);
        putFluid(ModFluids.VITRIOL, 1_000, ModFluids.SULFURIC_ACID, 500, ModFluids.CHLORINE, 500,
                new ItemStack(ModItems.getPowders(com.hbm_m.item.tags_and_tiers.ModPowders.IRON).get()), new ItemStack(ModItems.NUGGET_MERCURY.get()));
        putFluid(ModFluids.REDMUD, 450, ModFluids.MERCURY, 150, ModFluids.LYE, 50,
                new ItemStack(ModItems.getPowder(ModIngots.TITANIUM).get(), 3),
                new ItemStack(ModItems.getPowders(com.hbm_m.item.tags_and_tiers.ModPowders.IRON).get(), 3),
                new ItemStack(ModItems.getPowder(ModIngots.ALUMINUM).get(), 2));
        putFluid(ModFluids.POTASSIUM_CHLORIDE, 250, ModFluids.CHLORINE, 125, ModFluids.NONE, 0);
        putFluid(ModFluids.CALCIUM_CHLORIDE, 250, ModFluids.CHLORINE, 125, ModFluids.CALCIUM_SOLUTION, 125);
    }

    @Nullable
    public static FluidRecipe getFluidRecipe(Fluid input) {
        return FLUID_RECIPES.get(input);
    }

    public static Map<Fluid, FluidRecipe> getAllFluidRecipes() {
        return Map.copyOf(FLUID_RECIPES);
    }

    // ==================== Metal mode ====================

    public record MetalRecipe(ItemStack outA, ItemStack outB, ItemStack[] byproducts, int duration) {}

    private static final Map<Item, MetalRecipe> METAL_RECIPES = new HashMap<>();

    private static void putMetal(Item crystal, ItemStack outA, ItemStack outB, ItemStack... byproducts) {
        METAL_RECIPES.put(crystal, new MetalRecipe(outA, outB, byproducts, 600));
    }

    private static ItemStack ingot(ModIngots mat, int count) {
        return new ItemStack(ModItems.getIngot(mat).get(), count);
    }

    static {
        putMetal(ModItems.CRYSTAL_IRON.get(), ingot(ModIngots.STEEL, 6), ingot(ModIngots.TITANIUM, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_GOLD.get(), new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 6), ingot(ModIngots.LEAD, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3), new ItemStack(ModItems.NUGGET_MERCURY.get(), 2));
        putMetal(ModItems.CRYSTAL_URANIUM.get(), ingot(ModIngots.URANIUM, 6), new ItemStack(ModItems.RADIUM_RAW.get(), 4),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_THORIUM.get(), ingot(ModIngots.THORIUM232, 6), ingot(ModIngots.URANIUM, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_PLUTONIUM.get(), ingot(ModIngots.PLUTONIUM, 6), ingot(ModIngots.POLONIUM, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_TITANIUM.get(), ingot(ModIngots.TITANIUM, 6), ingot(ModIngots.STEEL, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_COPPER.get(), new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 6), ingot(ModIngots.LEAD, 1),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3), new ItemStack(ModItems.SULFUR.get(), 2));
        putMetal(ModItems.CRYSTAL_TUNGSTEN.get(), ingot(ModIngots.TUNGSTEN, 6), ingot(ModIngots.STEEL, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_ALUMINIUM.get(), ingot(ModIngots.ALUMINUM, 2), ingot(ModIngots.STEEL, 2),
                new ItemStack(ModItems.CRYOLITE.get(), 4), new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_BERYLLIUM.get(), ingot(ModIngots.BERYLLIUM, 6), ingot(ModIngots.LEAD, 1),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3), new ItemStack(ModItems.QUARTZ_POWDER.get(), 2));
        putMetal(ModItems.CRYSTAL_LEAD.get(), ingot(ModIngots.LEAD, 6), new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_SCHRARANIUM.get(), ingot(ModIngots.SCHRABIDIUM, 5), ingot(ModIngots.URANIUM, 2),
                new ItemStack(ModItems.NUGGET_NEPTUNIUM.get(), 2));
        putMetal(ModItems.CRYSTAL_SCHRABIDIUM.get(), ingot(ModIngots.SCHRABIDIUM, 6), ingot(ModIngots.PLUTONIUM, 2),
                new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
        putMetal(ModItems.CRYSTAL_RARE.get(), ingot(ModIngots.ZIRCONIUM, 6), ingot(ModIngots.BORON, 2),
                new ItemStack(ModItems.POWDER_DESH_MIX.get(), 3));
        putMetal(ModItems.CRYSTAL_TRIXITE.get(), ingot(ModIngots.PLUTONIUM, 3), ingot(ModIngots.COBALT, 4),
                new ItemStack(ModItems.getPowder(ModIngots.NIOBIUM).get(), 4), new ItemStack(ModItems.POWDER_NITAN_MIX.get(), 2));
        putMetal(ModItems.CRYSTAL_LITHIUM.get(), ingot(ModIngots.LITHIUM_INGOT, 6), ingot(ModIngots.BORON, 2),
                new ItemStack(ModItems.QUARTZ_POWDER.get(), 2), new ItemStack(ModItems.FLUORITE.get(), 2));
        putMetal(ModItems.CRYSTAL_STARMETAL.get(), ingot(ModIngots.DURA_STEEL, 4), ingot(ModIngots.COBALT, 4),
                new ItemStack(ModItems.getPowder(ModIngots.ASTATINE).get(), 3), new ItemStack(ModItems.NUGGET_MERCURY.get(), 8));
        putMetal(ModItems.CRYSTAL_COBALT.get(), ingot(ModIngots.COBALT, 3), ingot(ModIngots.STEEL, 4),
                new ItemStack(ModItems.COPPER_POWDER.get(), 4), new ItemStack(ModItems.LITHIUM_POWDER_TINY.get(), 3));
    }

    @Nullable
    public static MetalRecipe getMetalRecipe(ItemStack input) {
        if (input.isEmpty()) return null;
        return METAL_RECIPES.get(input.getItem());
    }

    public static boolean hasMetalRecipe(ItemStack input) {
        return getMetalRecipe(input) != null;
    }

    public static Map<Item, MetalRecipe> getAllMetalRecipes() {
        return Map.copyOf(METAL_RECIPES);
    }
}
