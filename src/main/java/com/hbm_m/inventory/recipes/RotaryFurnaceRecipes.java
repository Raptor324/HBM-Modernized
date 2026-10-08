package com.hbm_m.inventory.recipes;

import static com.hbm_m.inventory.material.MaterialShapes.*;
import static com.hbm_m.inventory.material.Mats.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.item.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code RotaryFurnaceRecipes}: bis zu drei Eingaben plus optionales Fluid werden unter Dampfverbrauch zu einer
 * Schmelze ({@link MaterialStack}), die der Drehrohrofen in eine Giessanlage giesst.
 */
public final class RotaryFurnaceRecipes {

    public static final List<RotaryFurnaceRecipe> recipes = new ArrayList<>();
    private static boolean loaded;

    private RotaryFurnaceRecipes() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    public static List<RotaryFurnaceRecipe> all() {
        ensureLoaded();
        return recipes;
    }

    private static void registerDefaults() {

        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.q(1)), 100, 100, shape(MAT_IRON, INGOT, 1), coal()));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.q(1)), 100, 100, shape(MAT_IRON, INGOT, 1), anyCoke()));

        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.q(2)), 200, 25, shape(MAT_IRON, FRAGMENT, 9), coal()));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.q(3)), 200, 25, shape(MAT_IRON, FRAGMENT, 9), anyCoke()));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_STEEL, INGOT.q(4)), 400, 25, shape(MAT_IRON, FRAGMENT, 9), anyCoke(), item(ModItems.POWDER_FLUX.get(), 1)));

        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_DESH, INGOT.q(1)), 100, 200, fluid(ModFluids.LIGHTOIL.getSource(), 100), item(ModItems.POWDER_DESH_READY.get(), 1)));

        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_GUNMETAL, INGOT.q(4)), 200, 100, shape(MAT_COPPER, INGOT, 3), shape(MAT_ALUMINIUM, INGOT, 1)));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_WEAPONSTEEL, INGOT.q(1)), 200, 400, fluid(ModFluids.GAS_COKER.getSource(), 100), shape(MAT_STEEL, INGOT, 1), item(ModItems.POWDER_FLUX.get(), 2)));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_SATURN, INGOT.q(2)), 200, 400, fluid(ModFluids.REFORMGAS.getSource(), 250), shape(MAT_DURA, DUST, 4), shape(MAT_COPPER, DUST, 1)));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_SATURN, INGOT.q(4)), 200, 300, fluid(ModFluids.REFORMGAS.getSource(), 250), shape(MAT_DURA, DUST, 4), shape(MAT_COPPER, DUST, 1), shape(MAT_BORAX, DUST, 1)));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_ALUMINIUM, INGOT.q(2)), 100, 400, fluid(ModFluids.SODIUM_ALUMINATE.getSource(), 150)));
        recipes.add(new RotaryFurnaceRecipe(new MaterialStack(MAT_ALUMINIUM, INGOT.q(3)), 40, 200, fluid(ModFluids.SODIUM_ALUMINATE.getSource(), 150), item(ModItems.POWDER_FLUX.get(), 2)));
    }

    // ═══════════════════════════ Eingaben ═══════════════════════════

    /** Gegenstueck zu {@code AStack}: Pruefung plus Anzeigestapel und Menge. */
    public record RecipeInput(Predicate<ItemStack> test, List<ItemStack> display, int stacksize) {
        /** Original {@code matchesRecipe(stack, true)}: Sorte stimmt, Menge wird separat geprueft. */
        public boolean matchesRecipe(ItemStack stack) {
            return !stack.isEmpty() && test.test(stack);
        }
    }

    /** Gegenstueck zu {@code FluidStack}. */
    public record FluidInput(Fluid type, int fill) { }

    /** {@code OreDictStack(MAT.shape(), n)}: jedes Item, das das Materialsystem als diese Form erkennt. */
    private static RecipeInput shape(NTMMaterial mat, MaterialShapes shape, int count) {
        Item display = Mats.getItemForShape(mat, shape);
        List<ItemStack> list = display == null ? List.of() : List.of(new ItemStack(display, count));
        return new RecipeInput(stack -> {
            Mats.MatShape ms = Mats.getShape(stack);
            return ms != null && ms.material() == mat && ms.shape() == shape;
        }, list, count);
    }

    /** {@code ComparableStack(item, n)}. */
    private static RecipeInput item(Item item, int count) {
        return new RecipeInput(stack -> stack.is(item), List.of(new ItemStack(item, count)), count);
    }

    /** {@code OreDictStack(COAL.gem())}. */
    private static RecipeInput coal() {
        return new RecipeInput(stack -> stack.is(Items.COAL), List.of(new ItemStack(Items.COAL)), 1);
    }

    /** {@code OreDictStack(ANY_COKE.gem())}: Kohle-, Petrol- und Braunkohlekoks. */
    private static RecipeInput anyCoke() {
        Item[] cokes = { ModItems.COAL_COKE.get(), ModItems.COKE_PETROLEUM.get(), ModItems.LIGNITE_COKE.get() };
        List<ItemStack> list = new ArrayList<>();
        for (Item i : cokes) list.add(new ItemStack(i));
        return new RecipeInput(stack -> {
            for (Item i : cokes) if (stack.is(i)) return true;
            return false;
        }, list, 1);
    }

    private static FluidInput fluid(Fluid type, int fill) {
        return new FluidInput(type, fill);
    }

    // ═══════════════════════════ Suche ═══════════════════════════

    /** 1:1 {@code getRecipe(ItemStack...)}: jede belegte Eingabe muss genau eine Rezeptzutat verbrauchen. */
    @Nullable
    public static RotaryFurnaceRecipe getRecipe(ItemStack... inputs) {
        ensureLoaded();

        outer:
        for (RotaryFurnaceRecipe recipe : recipes) {

            List<RecipeInput> recipeList = new ArrayList<>(List.of(recipe.ingredients));

            for (ItemStack inputStack : inputs) {

                if (inputStack != null && !inputStack.isEmpty()) {

                    boolean hasMatch = false;
                    Iterator<RecipeInput> iterator = recipeList.iterator();

                    while (iterator.hasNext()) {
                        RecipeInput recipeStack = iterator.next();

                        if (recipeStack.matchesRecipe(inputStack) && inputStack.getCount() >= recipeStack.stacksize()) {
                            hasMatch = true;
                            iterator.remove();
                            break;
                        }
                    }

                    if (!hasMatch) {
                        continue outer;
                    }
                }
            }

            if (recipeList.isEmpty()) return recipe;
        }

        return null;
    }

    public static class RotaryFurnaceRecipe {

        public RecipeInput[] ingredients;
        @Nullable public FluidInput fluid;
        public MaterialStack output;
        public int duration;
        public int steam;

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, @Nullable FluidInput fluid, RecipeInput... ingredients) {
            this.ingredients = ingredients;
            this.fluid = fluid;
            this.output = output;
            this.duration = duration;
            this.steam = steam;
        }

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, RecipeInput... ingredients) {
            this(output, duration, steam, null, ingredients);
        }
    }
}
