package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code RotaryFurnaceRecipes}: bis zu drei Eingaben plus optionales Fluid werden unter Dampfverbrauch zu einer
 * Schmelze ({@link MaterialStack}), die der Drehrohrofen in eine Giessanlage giesst. Der Inhalt der Code-Tabelle ist
 * seit der Datapack-Umstellung leer — Quelle sind die {@code hbm_m:rotary_furnace}-JSONs (Datagen:
 * {@code RotaryFurnaceRecipeGenerator}). Weil das BlockEntity die Tabelle ohne Level abfragt, materialisiert
 * {@link #noteManager} die JSONs direkt in die Liste (remove-then-reapply); {@link #all(net.minecraft.world.item.crafting.RecipeManager)}
 * liefert sie daneben als Merge-Ansicht (MERGED-Cache je RecipeManager, Reihenfolge Rezept-ID).
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

    /** W-R9B5-6 (Space-Addon): Rezept anhaengen (nach den HBM-Standards). */
    public static synchronized void addExternal(RotaryFurnaceRecipe recipe) {
        ensureLoaded();
        recipes.add(recipe);
        baseVersion++;
    }

    public static List<RotaryFurnaceRecipe> all() {
        ensureLoaded();
        return recipes;
    }

    // ---------------------------------------------------------------- Datapack-Rezepte (hbm_m:rotary_furnace)

    private static int baseVersion;

    private record Merged(int baseVersion, int fingerprint, List<RotaryFurnaceRecipe> list) { }

    private static final java.util.WeakHashMap<net.minecraft.world.item.crafting.RecipeManager, Merged> MERGED = new java.util.WeakHashMap<>();

    private static java.lang.ref.WeakReference<net.minecraft.world.item.crafting.RecipeManager> notedManager;
    private static int notedFingerprint;
    /** Zuletzt eingetragene Datapack-Eintraege — wird beim Neuaufbau zuerst wieder entfernt. */
    private static final List<RotaryFurnaceRecipe> appliedDatapack = new ArrayList<>();

    /**
     * Uebernimmt die {@code hbm_m:rotary_furnace}-JSONs des Managers in die Tabelle (remove-then-reapply,
     * deterministische Reihenfolge nach Rezept-ID; gleicher Manager + unveraenderter Rezeptbestand = nichts zu
     * tun). Per {@link #addExternal} eingetragene Fremdeintraege bleiben erhalten.
     */
    public static synchronized void noteManager(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        if (manager == null) return;
        Map<net.minecraft.resources.ResourceLocation, com.hbm_m.recipe.RotaryFurnaceRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, com.hbm_m.recipe.RotaryFurnaceRecipe.Type.INSTANCE);
        int fp = 1;
        for (Object r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        if (notedManager != null && manager == notedManager.get() && fp == notedFingerprint) return;

        recipes.removeAll(appliedDatapack);
        appliedDatapack.clear();
        ensureLoaded();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Object::toString))).forEach(e -> {
            RotaryFurnaceRecipe rec = toInner(e.getValue());
            if (rec != null) {
                recipes.add(rec);
                appliedDatapack.add(rec);
            }
        });
        notedManager = new java.lang.ref.WeakReference<>(manager);
        notedFingerprint = fp;
    }

    /**
     * Tabelle inkl. Datapack als Merge-Ansicht: Code-Tabelle plus {@code hbm_m:rotary_furnace}-Rezepte des
     * RecipeManagers (deterministisch nach Rezept-ID sortiert, Cache je Manager). Der Manager wird daneben an
     * {@link #noteManager} weitergereicht — das BlockEntity fragt dieselbe Tabelle ohne Level ab und sieht die
     * JSONs deshalb ueber die materialisierten Eintraege (siehe oben).
     */
    public static synchronized List<RotaryFurnaceRecipe> all(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        if (manager == null) return all();
        noteManager(manager);
        Map<net.minecraft.resources.ResourceLocation, com.hbm_m.recipe.RotaryFurnaceRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, com.hbm_m.recipe.RotaryFurnaceRecipe.Type.INSTANCE);
        if (data.isEmpty()) return all();
        int fp = 1;
        for (Object r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        Merged m = MERGED.get(manager);
        if (m != null && m.baseVersion == baseVersion && m.fingerprint == fp) return m.list;
        List<RotaryFurnaceRecipe> merged = new ArrayList<>(recipes);
        merged.removeAll(appliedDatapack); // JSONs laufen unten frisch ein, nicht doppelt
        data.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Object::toString))).forEach(e -> {
            RotaryFurnaceRecipe rec = toInner(e.getValue());
            if (rec != null) merged.add(rec);
        });
        MERGED.put(manager, new Merged(baseVersion, fp, merged));
        return merged;
    }

    /** {@code com.hbm_m.recipe.RotaryFurnaceRecipe} (JSON) als Code-Rezept der Maschinentabelle. */
    @Nullable
    private static RotaryFurnaceRecipe toInner(com.hbm_m.recipe.RotaryFurnaceRecipe r) {
        MaterialStack output = molten(r.getOutput());
        if (output == null) return null; // Ausgabe ist kein Formen-Item -> fuer den Ofen unbrauchbar

        RecipeInput[] ingredients = new RecipeInput[r.getInputs().length];
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = r.getInputs()[i];
            int count = r.getInputCount(i);
            List<ItemStack> display = new ArrayList<>();
            for (ItemStack s : ingredient.getItems()) {
                ItemStack copy = s.copy();
                copy.setCount(count);
                display.add(copy);
            }
            ingredients[i] = new RecipeInput(ingredient::test, display, count);
        }
        FluidInput fluid = r.getFluid() != null ? new FluidInput(r.getFluid().getFluid(), r.getFluidAmountMb()) : null;
        return new RotaryFurnaceRecipe(output, r.getDuration(), r.getSteam(), fluid, ingredients);
    }

    /** Formen-Item (Barren/Pulver/Fragment) des Ausgangs als Schmelzmenge in Quanten; {@code null} = kein Formen-Item. */
    @Nullable
    private static MaterialStack molten(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Mats.MatShape shape = Mats.getShape(stack);
        if (shape == null) return null;
        return new MaterialStack(shape.material(), shape.shape().q(stack.getCount()));
    }

    /**
     * Datapack-Umstellung: die 13 FFA-Standardeintraege (fuenfmal Stahl aus Eisen/Kohle-Koks/Flussmittel, Desh aus
     * Oel, Rotguss, Waffenstahl aus Koksgas, zweimal Saturnit aus Reformgas, zweimal Aluminium aus Natriumaluminat,
     * Titan aus Titantetrachlorid) liegen als {@code hbm_m:rotary_furnace}-JSONs im Datagen
     * ({@code RotaryFurnaceRecipeGenerator}); die Code-Tabelle bleibt leer.
     */
    private static void registerDefaults() { }

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

    /** Rezept inkl. Datapack ({@code level} null = nur Code-Tabelle). */
    @Nullable
    public static RotaryFurnaceRecipe getRecipe(@Nullable net.minecraft.world.level.Level level, ItemStack... inputs) {
        if (level != null) noteManager(level.getRecipeManager());
        return getRecipe(inputs);
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
