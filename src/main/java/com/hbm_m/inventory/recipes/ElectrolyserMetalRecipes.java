package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.industrial.BedrockOreType;
import com.hbm_m.item.industrial.BedrockOreType.BedrockOreOutput;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ElectrolyserMetalRecipes}: Kristalle und Grundgestein-Erze werden in zwei Schmelzen plus feste
 * Nebenprodukte zerlegt. Die Grundgestein-Erzrezepte entstehen wie im Original aus den Primaerausgaben der
 * sechs Erztypen ({@code makeBedrockOreProduct}).
 *
 * <p>Seit der Datapack-Umstellung ist die Code-Tabelle leer — Quelle sind die {@code hbm_m:electrolyser_metal}-
 * JSONs (Datagen: {@code ElectrolyserMetalRecipeGenerator}). Weil das BlockEntity (und die Item-Erkennung von
 * Slot 14) die Tabelle <i>ohne</i> Level abfragt, materialisiert {@link #noteManager} die JSONs direkt in die
 * statische Map; angestossen wird das vom Fluid-Shim ({@code ElectrolyserFluidRecipes.all}), den das BlockEntity
 * im selben Tick vor der Metallpruefung laeuft (beide Energie-Schwellen sind identisch).</p>
 */
public final class ElectrolyserMetalRecipes {

    public static final Map<Item, ElectrolysisMetalRecipe> recipes = new LinkedHashMap<>();
    private static boolean loaded;

    private ElectrolyserMetalRecipes() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    public static Map<Item, ElectrolysisMetalRecipe> all() {
        ensureLoaded();
        return recipes;
    }

    // ---------------------------------------------------------------- Datapack-Rezepte (hbm_m:electrolyser_metal)

    private static java.lang.ref.WeakReference<net.minecraft.world.item.crafting.RecipeManager> notedManager;
    private static int notedFingerprint;
    /** Zuletzt eingetragene Datapack-Schluessel — wird beim Neuaufbau zuerst wieder entfernt. */
    private static final Map<Item, ElectrolysisMetalRecipe> appliedDatapack = new LinkedHashMap<>();

    /**
     * Uebernimmt die {@code hbm_m:electrolyser_metal}-JSONs des Managers in die Tabelle (gleiches Eingangs-Item =
     * ersetzt, deterministische Reihenfolge nach Rezept-ID; gleicher Manager + unveraenderter Rezeptbestand =
     * nichts zu tun). Vorherige Datapack-Eintraege werden zuerst entfernt, per Map eingetragene Fremdeintraege
     * bleiben erhalten.
     */
    public static synchronized void noteManager(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        if (manager == null) return;
        Map<net.minecraft.resources.ResourceLocation, com.hbm_m.recipe.ElectrolyserMetalRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, com.hbm_m.recipe.ElectrolyserMetalRecipe.Type.INSTANCE);
        int fp = 1;
        for (Object r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        if (notedManager != null && manager == notedManager.get() && fp == notedFingerprint) return;

        for (Item key : appliedDatapack.keySet()) recipes.remove(key);
        appliedDatapack.clear();
        ensureLoaded();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(Object::toString))).forEach(e -> {
            Item in = inputItem(e.getValue());
            ElectrolysisMetalRecipe rec = toElectrolysis(e.getValue());
            if (in != null && rec != null) {
                recipes.put(in, rec);
                appliedDatapack.put(in, rec);
            }
        });
        notedManager = new java.lang.ref.WeakReference<>(manager);
        notedFingerprint = fp;
    }

    /** Tabelle inkl. Datapack ({@code level} null = nur Code-Tabelle). */
    public static synchronized Map<Item, ElectrolysisMetalRecipe> all(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        noteManager(manager);
        return all();
    }

    /** {@code ElectrolyserMetalRecipe} (JSON) als Code-{@code ElectrolysisMetalRecipe}: Schmelzausgänge über die Formen-Items. */
    private static ElectrolysisMetalRecipe toElectrolysis(com.hbm_m.recipe.ElectrolyserMetalRecipe r) {
        ItemStack[] bp = new ItemStack[r.getByproducts().length];
        for (int i = 0; i < bp.length; i++) bp[i] = r.getByproducts()[i].copy();
        return new ElectrolysisMetalRecipe(molten(r.getOutputA()), molten(r.getOutputB()), r.getDuration(), bp);
    }

    /** Formen-Item (Barren/Pulver/Klumpen) als Schmelzmenge in Quanten; {@code null} = kein Schmelzausgang. */
    @Nullable
    private static MaterialStack molten(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Mats.MatShape shape = Mats.getShape(stack);
        if (shape == null) return null;
        return new MaterialStack(shape.material(), shape.shape().q(stack.getCount()));
    }

    @Nullable
    private static Item inputItem(com.hbm_m.recipe.ElectrolyserMetalRecipe r) {
        ItemStack[] items = r.getInput().getItems();
        return items != null && items.length > 0 ? items[0].getItem() : null;
    }

    /** Datapack-Umstellung: die 18 Kristall- und 18 Grundgestein-Erzrezepte liegen als {@code hbm_m:electrolyser_metal}-
     * JSONs im Datagen ({@code ElectrolyserMetalRecipeGenerator}); die Code-Tabelle bleibt leer. */
    private static void registerDefaults() { }

    /** 1:1 {@code makeBedrockOreProduct}. */
    public static ElectrolysisMetalRecipe makeBedrockOreProduct(List<Object[]> products) {
        List<MaterialStack> moltenProducts = new ArrayList<>();
        List<ItemStack> solidProducts = new ArrayList<>();

        for (Object[] product : products) {
            Object key = product[0];
            int value = (Integer) product[1];
            if (moltenProducts.size() < 2 && key instanceof BedrockOreOutput out) {
                MaterialStack melt = BedrockOreType.toFluid(out, value);
                if (melt != null) {
                    moltenProducts.add(melt);
                    continue;
                }
            }

            if (key instanceof BedrockOreOutput out) solidProducts.add(BedrockOreType.extract(out, value));
            if (key instanceof ItemStack stack) solidProducts.add(stack.copy());
        }
        if (moltenProducts.isEmpty()) moltenProducts.add(new MaterialStack(Mats.MAT_SLAG, MaterialShapes.INGOT.q(2)));

        return new ElectrolysisMetalRecipe(
                moltenProducts.get(0),
                moltenProducts.size() > 1 ? moltenProducts.get(1) : null,
                20,
                solidProducts.toArray(new ItemStack[0]));
    }

    /** Original {@code getRecipe}: Vergleich nach Item (ComparableStack, Menge 1). */
    @Nullable
    public static ElectrolysisMetalRecipe getRecipe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ensureLoaded();
        return recipes.get(stack.getItem());
    }

    /** Rezept inkl. Datapack ({@code level} null = nur Code-Tabelle). */
    @Nullable
    public static ElectrolysisMetalRecipe getRecipe(@Nullable net.minecraft.world.level.Level level, ItemStack stack) {
        if (level != null) noteManager(level.getRecipeManager());
        return getRecipe(stack);
    }

    public static class ElectrolysisMetalRecipe {

        @Nullable public MaterialStack output1;
        @Nullable public MaterialStack output2;
        public ItemStack[] byproduct;
        public int duration;

        public ElectrolysisMetalRecipe(@Nullable MaterialStack output1, @Nullable MaterialStack output2, ItemStack... byproduct) {
            this(output1, output2, 600, byproduct);
        }

        public ElectrolysisMetalRecipe(@Nullable MaterialStack output1, @Nullable MaterialStack output2, int duration, ItemStack... byproduct) {
            this.output1 = output1;
            this.output2 = output2;
            this.byproduct = byproduct;
            this.duration = duration;
        }
    }
}
