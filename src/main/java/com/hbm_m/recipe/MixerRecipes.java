package com.hbm_m.recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.inventory.recipes.MixerRecipes}: je Ausgabefluid eine Liste von Rezepten (bis zu zwei Fluide, ein
 * fester Stoff), gewaehlt ueber den Fluid-ID-Platz des Mischers und den Rezeptindex. Der fruehere Inhalt von
 * {@code registerDefaults} (FFA-Tabelle) ist 1:1 ins Datagen ({@code MixerRecipeGenerator}, {@code hbm_m:mixer}-JSONs)
 * gewandert; diese Klasse ist seither der Runtime-Aggregator: {@link #getRecipes(net.minecraft.world.item.crafting.RecipeManager)}
 * gruppiert die JSON-Rezepte nach Ausgabefluid (deterministisch nach Rezept-ID sortiert) fuer Maschine/GUI/JEI auf.
 */
public final class MixerRecipes {

    private MixerRecipes() { }

    private static Map<Fluid, MixerRecipe[]> recipes;

    /** Fluid samt Menge (Original {@code FluidStack}). */
    public record FluidIn(Fluid type, int fill) { }

    /** Fester Stoff (Original {@code AStack}): Pruefung ohne Stueckzahl, dazu die Menge. */
    public record SolidIn(Predicate<ItemStack> matcher, Supplier<ItemStack[]> display, int stacksize) {
        public boolean matchesRecipe(ItemStack stack) {
            return !stack.isEmpty() && matcher.test(stack);
        }

        public ItemStack extractForCyclingDisplay(int cycle) {
            ItemStack[] all = display.get();
            if (all.length == 0) return ItemStack.EMPTY;
            ItemStack s = all[(int) ((System.currentTimeMillis() / (cycle * 50L)) % all.length)].copy();
            s.setCount(stacksize);
            return s;
        }
    }

    public static class MixerRecipe {
        @Nullable public FluidIn input1;
        @Nullable public FluidIn input2;
        @Nullable public SolidIn solidInput;
        public int processTime;
        public int output;

        public MixerRecipe(int output, int processTime) {
            this.output = output;
            this.processTime = processTime;
        }

        public MixerRecipe setStack1(FluidIn stack) { input1 = stack; return this; }
        public MixerRecipe setStack2(FluidIn stack) { input2 = stack; return this; }
        public MixerRecipe setSolid(SolidIn stack) { solidInput = stack; return this; }
    }

    public static synchronized Map<Fluid, MixerRecipe[]> getRecipes() {
        if (recipes == null) {
            recipes = new LinkedHashMap<>();
            registerDefaults();
        }
        return recipes;
    }

    /** Seit dem Data-Driven-Port leer: der FFA-Inhalt lebt 1:1 in {@code MixerRecipeGenerator} (Datagen, {@code hbm_m:mixer}). */
    private static void registerDefaults() { }

    // ---------------------------------------------------------------- Datapack-Rezepte (hbm_m:mixer)

    private static int baseVersion;

    private record Merged(int baseVersion, int fingerprint, Map<Fluid, MixerRecipe[]> map) { }

    private static final java.util.WeakHashMap<net.minecraft.world.item.crafting.RecipeManager, Merged> MERGED = new java.util.WeakHashMap<>();

    /**
     * Alle {@code hbm_m:mixer}-Rezepte aus dem Datapack des RecipeManagers (Server oder synchronisierter Client),
     * nach Ausgabefluid gruppiert und deterministisch nach Rezept-ID sortiert (die Datagen-IDs sind stabil, daher
     * reicht die ID-Sortierung statt order/replace). Ohne JSONs identisch mit der (leeren) Code-Tabelle
     * {@link #getRecipes()}.
     */
    public static synchronized Map<Fluid, MixerRecipe[]> getRecipes(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        Map<Fluid, MixerRecipe[]> base = getRecipes();
        if (manager == null) return base;
        Map<net.minecraft.resources.ResourceLocation, com.hbm_m.recipe.MixerRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, com.hbm_m.recipe.MixerRecipe.Type.INSTANCE);
        if (data.isEmpty()) return base;
        int fp = 1;
        for (com.hbm_m.recipe.MixerRecipe r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        Merged m = MERGED.get(manager);
        if (m != null && m.baseVersion == baseVersion && m.fingerprint == fp) return m.map;

        java.util.List<net.minecraft.resources.ResourceLocation> ids = new java.util.ArrayList<>(data.keySet());
        java.util.Collections.sort(ids);
        Map<Fluid, java.util.List<MixerRecipe>> extra = new LinkedHashMap<>();
        for (net.minecraft.resources.ResourceLocation id : ids) {
            com.hbm_m.recipe.MixerRecipe r = data.get(id);
            if (r.getOutput().isEmpty()) continue;
            extra.computeIfAbsent(r.getOutput().getFluid(), k -> new java.util.ArrayList<>()).add(fromDatapack(r));
        }
        Map<Fluid, MixerRecipe[]> merged = new LinkedHashMap<>(base);
        for (Map.Entry<Fluid, java.util.List<MixerRecipe>> e : extra.entrySet()) {
            MixerRecipe[] old = merged.get(e.getKey());
            java.util.List<MixerRecipe> all = new java.util.ArrayList<>();
            if (old != null) all.addAll(java.util.Arrays.asList(old));
            all.addAll(e.getValue());
            merged.put(e.getKey(), all.toArray(new MixerRecipe[0]));
        }
        MERGED.put(manager, new Merged(baseVersion, fp, merged));
        return merged;
    }

    /** Datapack-Rezept ({@code hbm_m:mixer}) als Code-Rezept der FFA-Maschinentabelle. */
    private static MixerRecipe fromDatapack(com.hbm_m.recipe.MixerRecipe r) {
        MixerRecipe rec = new MixerRecipe((int) Math.min(Integer.MAX_VALUE, r.getOutput().getAmount()), r.getDuration());
        if (!r.getInputA().isEmpty()) {
            rec.setStack1(new FluidIn(r.getInputA().getFluid(), (int) Math.min(Integer.MAX_VALUE, r.getInputA().getAmount())));
        }
        if (!r.getInputB().isEmpty()) {
            rec.setStack2(new FluidIn(r.getInputB().getFluid(), (int) Math.min(Integer.MAX_VALUE, r.getInputB().getAmount())));
        }
        if (r.getSolidInput() != null) {
            rec.setSolid(new SolidIn(r.getSolidInput(), r.getSolidInput()::getItems, r.getSolidInputCount()));
        }
        return rec;
    }

    /** Rezepte fuer ein Ausgabefluid inkl. Datapack ({@code level} null = nur Code-Rezepte). */
    @Nullable
    public static MixerRecipe[] getOutput(@Nullable net.minecraft.world.level.Level level, Fluid type) {
        return getRecipes(level != null ? level.getRecipeManager() : null).get(type);
    }
}
