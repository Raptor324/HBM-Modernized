package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.Tags;

/**
 * Port of {@code com.hbm.inventory.recipes.RotaryFurnaceRecipes} (1.7.10 Original).
 * <p>
 * The original produces a {@code MaterialStack} that is poured as molten metal via
 * {@code CrucibleUtil} instead of dropped as an item. This port has no equivalent
 * "pour molten metal onto the ground" mechanic (same simplification used elsewhere this
 * session), so the output is a real ingot {@link ItemStack} placed into an output slot.
 * The original's steam/pollution byproducts are dropped for the same reason - this port's
 * FluidTank input slot only models the recipe's required input fluid, not the steam loop.
 */
public final class RotaryFurnaceRecipes {

    private RotaryFurnaceRecipes() {}

    public record Ingredient(Item item, int count) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(item) && stack.getCount() >= count;
        }
    }

    public record RecipeFluid(Fluid fluid, int amountMb) {}

    public record RotaryFurnaceRecipe(ItemStack output, int duration, Ingredient[] ingredients, @Nullable RecipeFluid fluid) {}

    private static final List<RotaryFurnaceRecipe> RECIPES = new ArrayList<>();

    private static void add(ItemStack output, int duration, @Nullable RecipeFluid fluid, Ingredient... ingredients) {
        RECIPES.add(new RotaryFurnaceRecipe(output, duration, ingredients, fluid));
    }

    private static Ingredient of(Item item, int count) { return new Ingredient(item, count); }

    static {
        // IRON + COAL/COKE -> STEEL x1 (recipes.add(..., 100, 100, IRON.ingot, COAL/ANY_COKE))
        add(new ItemStack(ModItems.getIngot(ModIngots.STEEL).get()), 100, null,
                of(Items.IRON_INGOT, 1), of(Items.COAL, 1));

        // 9x iron fragment (nugget) + COAL -> STEEL x2 (duration 200)
        add(new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 2), 200, null,
                of(Items.IRON_NUGGET, 9), of(Items.COAL, 1));

        // 9x iron fragment + coke + flux -> STEEL x4 (duration 400)
        add(new ItemStack(ModItems.getIngot(ModIngots.STEEL).get(), 4), 400, null,
                of(Items.IRON_NUGGET, 9), of(Items.COAL, 1), of(ModItems.POWDER_FLUX.get(), 1));

        // LIGHTOIL(100) + powder_desh_ready -> DESH x1 (duration 100)
        add(new ItemStack(ModItems.getIngot(ModIngots.DESH).get()), 100, new RecipeFluid(ModFluids.LIGHTOIL.getSource(), 100),
                of(ModItems.POWDER_DESH_READY.get(), 1));

        // 3x CU + 1x AL -> GUNMETAL x4 (duration 200)
        add(new ItemStack(ModItems.getIngot(ModIngots.GUNMETAL).get(), 4), 200, null,
                of(Items.COPPER_INGOT, 3), of(ModItems.getIngot(ModIngots.ALUMINUM).get(), 1));

        // GAS_COKER(100) + STEEL + 2x flux -> WEAPONSTEEL x1 (duration 200)
        add(new ItemStack(ModItems.getIngot(ModIngots.WEAPONSTEEL).get()), 200, new RecipeFluid(ModFluids.GAS_COKER.getSource(), 100),
                of(ModItems.getIngot(ModIngots.STEEL).get(), 1), of(ModItems.POWDER_FLUX.get(), 2));

        // REFORMGAS(250) + 4x dura_steel dust + Cu dust -> SATURNITE x2 (duration 200)
        add(new ItemStack(ModItems.getIngot(ModIngots.SATURNITE).get(), 2), 200, new RecipeFluid(ModFluids.REFORMGAS.getSource(), 250),
                of(ModItems.getPowder(ModIngots.DURA_STEEL).get(), 4), of(ModItems.COPPER_POWDER.get(), 1));

        // REFORMGAS(250) + 4x dura_steel dust + Cu dust + borax (no dedicated "dust" item in this port,
        // the raw borax item is used directly) -> SATURNITE x4 (duration 300)
        add(new ItemStack(ModItems.getIngot(ModIngots.SATURNITE).get(), 4), 300, new RecipeFluid(ModFluids.REFORMGAS.getSource(), 250),
                of(ModItems.getPowder(ModIngots.DURA_STEEL).get(), 4), of(ModItems.COPPER_POWDER.get(), 1), of(ModItems.BORAX.get(), 1));

        // SODIUM_ALUMINATE(150) -> ALUMINUM x2 (duration 100)
        add(new ItemStack(ModItems.getIngot(ModIngots.ALUMINUM).get(), 2), 100, new RecipeFluid(ModFluids.SODIUM_ALUMINATE.getSource(), 150));

        // SODIUM_ALUMINATE(150) + 2x flux -> ALUMINUM x3 (duration 40)
        add(new ItemStack(ModItems.getIngot(ModIngots.ALUMINUM).get(), 3), 40, new RecipeFluid(ModFluids.SODIUM_ALUMINATE.getSource(), 150),
                of(ModItems.POWDER_FLUX.get(), 2));
    }

    @Nullable
    public static RotaryFurnaceRecipe getRecipe(ItemStack in0, ItemStack in1, ItemStack in2, Fluid tankFluid, int tankFill) {
        outer:
        for (RotaryFurnaceRecipe recipe : RECIPES) {
            ItemStack[] pool = { in0.copy(), in1.copy(), in2.copy() };
            for (Ingredient ing : recipe.ingredients()) {
                boolean found = false;
                for (int i = 0; i < pool.length; i++) {
                    if (ing.matches(pool[i])) {
                        pool[i].shrink(ing.count());
                        found = true;
                        break;
                    }
                }
                if (!found) continue outer;
            }
            if (recipe.fluid() != null) {
                if (tankFluid == null || tankFluid.isSame(net.minecraft.world.level.material.Fluids.EMPTY)) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tankFluid, recipe.fluid().fluid())) continue;
                if (tankFill < recipe.fluid().amountMb()) continue;
            }
            return recipe;
        }
        return null;
    }

    public static List<RotaryFurnaceRecipe> getAll() {
        return List.copyOf(RECIPES);
    }
}
