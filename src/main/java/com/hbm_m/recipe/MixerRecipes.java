package com.hbm_m.recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.inventory.recipes.MixerRecipes}: je Ausgabefluid eine Liste von Rezepten (bis zu zwei Fluide, ein
 * fester Stoff), gewaehlt ueber den Fluid-ID-Platz des Mischers und den Rezeptindex. Ore-Dictionary-Eintraege des
 * Originals sind hier die entsprechenden Port-Gegenstaende (z.B. {@code KNO.dust()} = Salpeter).
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

    private static FluidIn fs(com.hbm_m.inventory.fluid.ModFluids.FluidEntry e, int amount) {
        return new FluidIn(e.getSource(), amount);
    }

    private static SolidIn item(Supplier<? extends Item> item) {
        return item(item, 1);
    }

    private static SolidIn item(Supplier<? extends Item> item, int count) {
        return new SolidIn(s -> s.is(item.get()), () -> new ItemStack[] { new ItemStack(item.get()) }, count);
    }

    @SafeVarargs
    private static SolidIn anyOf(Supplier<? extends Item>... items) {
        return new SolidIn(s -> {
            for (Supplier<? extends Item> i : items) if (s.is(i.get())) return true;
            return false;
        }, () -> {
            ItemStack[] arr = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) arr[i] = new ItemStack(items[i].get());
            return arr;
        }, 1);
    }

    private static Supplier<Item> mat(ModMaterials m, MaterialShape shape) {
        return () -> ModMaterialItems.item(m, shape);
    }

    public static synchronized Map<Fluid, MixerRecipe[]> getRecipes() {
        if (recipes == null) {
            recipes = new LinkedHashMap<>();
            registerDefaults();
        }
        return recipes;
    }

    private static void registerDefaults() {
        register(ModFluids.COOLANT.getSource(), new MixerRecipe(2_000, 50).setStack1(fs(ModFluids.WATER, 1_800)).setSolid(item(ModItems.NITER)));
        register(ModFluids.CRYOGEL.getSource(), new MixerRecipe(2_000, 50).setStack1(fs(ModFluids.COOLANT, 1_800)).setSolid(item(ModItems.POWDER_ICE)));
        register(ModFluids.NITAN.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.KEROSENE, 600)).setStack2(fs(ModFluids.MERCURY, 200)).setSolid(item(ModItems.POWDER_NITAN_MIX)));
        register(ModFluids.FRACKSOL.getSource(),
                new MixerRecipe(1_000, 20).setStack1(fs(ModFluids.SULFURIC_ACID, 900)).setStack2(fs(ModFluids.PETROLEUM, 100)),
                new MixerRecipe(1_000, 20).setStack1(fs(ModFluids.WATER, 1000)).setStack2(fs(ModFluids.PETROLEUM, 100)).setSolid(item(ModItems.SULFUR)));
        register(ModFluids.ENDERJUICE.getSource(), new MixerRecipe(100, 100).setStack1(fs(ModFluids.XPJUICE, 500)).setSolid(item(mat(ModMaterials.DIAMOND, MaterialShape.POWDER))));
        register(ModFluids.SALIENT.getSource(), new MixerRecipe(1000, 20).setStack1(fs(ModFluids.SEEDSLURRY, 500)).setStack2(fs(ModFluids.BLOOD, 500)));
        register(ModFluids.COLLOID.getSource(), new MixerRecipe(500, 20).setStack1(fs(ModFluids.WATER, 500)).setSolid(item(ModItems.DUST)));
        register(ModFluids.PHOSGENE.getSource(), new MixerRecipe(1000, 20).setStack1(fs(ModFluids.UNSATURATEDS, 500)).setStack2(fs(ModFluids.CHLORINE, 500)));
        register(ModFluids.MUSTARDGAS.getSource(), new MixerRecipe(1000, 20).setStack1(fs(ModFluids.REFORMGAS, 750)).setStack2(fs(ModFluids.CHLORINE, 250)).setSolid(item(ModItems.SULFUR)));
        register(ModFluids.IONGEL.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.WATER, 1000)).setStack2(fs(ModFluids.HYDROGEN, 200)).setSolid(item(ModItems.PELLET_CHARGED)));
        register(ModFluids.EGG.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.RADIOSOLVENT, 500)).setSolid(item(() -> Items.EGG)));
        register(ModFluids.FISHOIL.getSource(), new MixerRecipe(100, 50).setSolid(new SolidIn(s -> s.is(ItemTags.FISHES),
                () -> new ItemStack[] { new ItemStack(Items.COD), new ItemStack(Items.SALMON), new ItemStack(Items.TROPICAL_FISH), new ItemStack(Items.PUFFERFISH) }, 1)));
        register(ModFluids.SUNFLOWEROIL.getSource(), new MixerRecipe(100, 50).setSolid(item(() -> Items.SUNFLOWER)));
        register(ModFluids.FULLERENE.getSource(), new MixerRecipe(250, 50).setStack1(fs(ModFluids.RADIOSOLVENT, 500)).setSolid(item(ModItems.ASH_SOOT)));

        register(ModFluids.SOLVENT.getSource(),
                new MixerRecipe(1000, 50).setStack1(fs(ModFluids.NAPHTHA, 500)).setStack2(fs(ModFluids.AROMATICS, 500)),
                new MixerRecipe(1000, 50).setStack1(fs(ModFluids.NAPHTHA_CRACK, 500)).setStack2(fs(ModFluids.AROMATICS, 500)),
                new MixerRecipe(1000, 50).setStack1(fs(ModFluids.NAPHTHA_DS, 500)).setStack2(fs(ModFluids.AROMATICS, 500)),
                new MixerRecipe(1000, 50).setStack1(fs(ModFluids.NAPHTHA_COKER, 500)).setStack2(fs(ModFluids.AROMATICS, 500)));
        register(ModFluids.SULFURIC_ACID.getSource(), new MixerRecipe(500, 50).setStack1(fs(ModFluids.PEROXIDE, 800)).setSolid(item(ModItems.SULFUR)));
        register(ModFluids.NITRIC_ACID.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.SULFURIC_ACID, 500)).setSolid(item(ModItems.NITER)));
        register(ModFluids.RADIOSOLVENT.getSource(), new MixerRecipe(1000, 50).setStack1(fs(ModFluids.REFORMGAS, 750)).setStack2(fs(ModFluids.CHLORINE, 250)));
        register(ModFluids.SCHRABIDIC.getSource(), new MixerRecipe(16_000, 100).setStack1(fs(ModFluids.SAS3, 8_000)).setStack2(fs(ModFluids.PEROXIDE, 6_000)).setSolid(item(ModItems.PELLET_CHARGED)));

        register(ModFluids.PETROIL.getSource(), new MixerRecipe(1_000, 30).setStack1(fs(ModFluids.RECLAIMED, 800)).setStack2(fs(ModFluids.LUBRICANT, 200)));
        register(ModFluids.LUBRICANT.getSource(),
                new MixerRecipe(1_000, 20).setStack1(fs(ModFluids.HEATINGOIL, 500)).setStack2(fs(ModFluids.UNSATURATEDS, 500)),
                new MixerRecipe(1_000, 20).setStack1(fs(ModFluids.FISHOIL, 800)).setStack2(fs(ModFluids.ETHANOL, 200)),
                new MixerRecipe(1_000, 20).setStack1(fs(ModFluids.SUNFLOWEROIL, 800)).setStack2(fs(ModFluids.ETHANOL, 200)));
        register(ModFluids.BIOFUEL.getSource(),
                new MixerRecipe(250, 20).setStack1(fs(ModFluids.FISHOIL, 500)).setStack2(fs(ModFluids.WOODOIL, 500)),
                new MixerRecipe(200, 20).setStack1(fs(ModFluids.SUNFLOWEROIL, 500)).setStack2(fs(ModFluids.WOODOIL, 500)));
        register(ModFluids.NITROGLYCERIN.getSource(),
                new MixerRecipe(1000, 20).setStack1(fs(ModFluids.PETROLEUM, 1_000)).setStack2(fs(ModFluids.NITRIC_ACID, 1_000)),
                new MixerRecipe(1000, 20).setStack1(fs(ModFluids.FISHOIL, 500)).setStack2(fs(ModFluids.NITRIC_ACID, 500)));

        register(ModFluids.THORIUM_SALT.getSource(), new MixerRecipe(1_000, 30).setStack1(fs(ModFluids.CHLORINE, 1000)).setSolid(item(mat(ModMaterials.THORIUM, MaterialShape.POWDER))));

        register(ModFluids.SYNGAS.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.COALOIL, 500)).setStack2(fs(ModFluids.STEAM, 500)));
        register(ModFluids.OXYHYDROGEN.getSource(),
                new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.HYDROGEN, 500)).setStack2(fs(ModFluids.AIR, 2_000)),
                new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.HYDROGEN, 500)).setStack2(fs(ModFluids.OXYGEN, 500)));

        register(ModFluids.PETROIL_LEADED.getSource(), new MixerRecipe(12_000, 40).setStack1(fs(ModFluids.PETROIL, 10_000)).setSolid(item(ModItems.FUEL_ADDITIVE_ANTIKNOCK)));
        register(ModFluids.GASOLINE_LEADED.getSource(), new MixerRecipe(12_000, 40).setStack1(fs(ModFluids.GASOLINE, 10_000)).setSolid(item(ModItems.FUEL_ADDITIVE_ANTIKNOCK)));
        register(ModFluids.COALGAS_LEADED.getSource(), new MixerRecipe(12_000, 40).setStack1(fs(ModFluids.COALGAS, 10_000)).setSolid(item(ModItems.FUEL_ADDITIVE_ANTIKNOCK)));

        register(ModFluids.DIESEL_REFORM.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.DIESEL, 900)).setStack2(fs(ModFluids.REFORMATE, 100)));
        register(ModFluids.DIESEL_CRACK_REFORM.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.DIESEL_CRACK, 900)).setStack2(fs(ModFluids.REFORMATE, 100)));
        register(ModFluids.KEROSENE_REFORM.getSource(), new MixerRecipe(1_000, 50).setStack1(fs(ModFluids.KEROSENE, 900)).setStack2(fs(ModFluids.REFORMATE, 100)));

        register(ModFluids.CHLOROCALCITE_SOLUTION.getSource(), new MixerRecipe(500, 50).setStack1(fs(ModFluids.WATER, 250)).setStack2(fs(ModFluids.NITRIC_ACID, 250)).setSolid(item(ModItems.POWDER_CHLOROCALCITE)));
        register(ModFluids.CHLOROCALCITE_MIX.getSource(), new MixerRecipe(1000, 50).setStack1(fs(ModFluids.CHLOROCALCITE_SOLUTION, 500)).setStack2(fs(ModFluids.SULFURIC_ACID, 500)).setSolid(item(ModItems.POWDER_FLUX)));
        register(ModFluids.PHEROMONE_M.getSource(), new MixerRecipe(2000, 10).setStack1(fs(ModFluids.PHEROMONE, 1500)).setStack2(fs(ModFluids.BLOOD, 500)).setSolid(item(ModItems.PILL_HERBAL)));

        register(ModFluids.BAUXITE_SOLUTION.getSource(), new MixerRecipe(300, 80).setStack1(fs(ModFluids.LYE, 50)).setSolid(item(() -> ModBlocks.STONE_RESOURCE_BAUXITE.get().asItem())));
        register(ModFluids.LYE.getSource(), new MixerRecipe(100, 100).setStack1(fs(ModFluids.WATER, 100)).setSolid(item(ModItems.ASH_WOOD)));
        register(ModFluids.ALUMINA.getSource(), new MixerRecipe(200, 40).setStack1(fs(ModFluids.SODIUM_ALUMINATE, 150)).setSolid(item(ModItems.FLUORITE, 3)),
                new MixerRecipe(300, 40).setStack1(fs(ModFluids.SODIUM_ALUMINATE, 150)).setSolid(item(ModItems.CRYOLITE_CHUNK)));

        register(ModFluids.PERFLUOROMETHYL.getSource(), new MixerRecipe(1000, 20).setStack1(fs(ModFluids.PETROLEUM, 1000)).setStack2(fs(ModFluids.UNSATURATEDS, 500)).setSolid(item(ModItems.FLUORITE)));

        // Original ANY_TAR: Rohoel-, Kohle-, Crack- und Holzteer
        register(ModFluids.BITUMEN.getSource(), new MixerRecipe(50, 20).setSolid(anyOf(ModItems.OIL_TAR_CRUDE, ModItems.OIL_TAR_COAL, ModItems.OIL_TAR_CRACK, ModItems.OIL_TAR_WOOD)));
    }

    public static void register(Fluid type, MixerRecipe... rec) {
        recipes.put(type, rec);
        baseVersion++;
    }

    /**
     * W-R9B5-1 (Space-Addon): Rezepte fuer ein Ausgabefluid anhaengen (bestehende HBM-Rezepte bleiben, Reihenfolge =
     * Aufrufreihenfolge). Sicher vor und nach dem ersten {@link #getRecipes()}.
     */
    public static synchronized void addExternal(Fluid type, MixerRecipe... rec) {
        Map<Fluid, MixerRecipe[]> map = getRecipes();
        MixerRecipe[] old = map.get(type);
        if (old == null) {
            map.put(type, rec);
        } else {
            MixerRecipe[] merged = java.util.Arrays.copyOf(old, old.length + rec.length);
            System.arraycopy(rec, 0, merged, old.length, rec.length);
            map.put(type, merged);
        }
        baseVersion++;
    }

    // ---------------------------------------------------------------- W-R9B5-1: Datapack-Rezepte (hbm_m:mixer)

    private static int baseVersion;

    private record Merged(int baseVersion, int fingerprint, Map<Fluid, MixerRecipe[]> map) { }

    private static final java.util.WeakHashMap<net.minecraft.world.item.crafting.RecipeManager, Merged> MERGED = new java.util.WeakHashMap<>();

    /**
     * Code-Rezepte plus {@link MixerDataRecipe}s aus dem Datapack des RecipeManagers (Server oder synchronisierter Client).
     * Ohne {@code hbm_m:mixer}-JSONs (HBM liefert keine) identisch mit {@link #getRecipes()}.
     */
    public static synchronized Map<Fluid, MixerRecipe[]> getRecipes(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        Map<Fluid, MixerRecipe[]> base = getRecipes();
        if (manager == null) return base;
        Map<net.minecraft.resources.ResourceLocation, MixerDataRecipe> data =
                com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(manager, MixerDataRecipe.Type.INSTANCE);
        if (data.isEmpty()) return base;
        int fp = 1;
        for (MixerDataRecipe r : data.values()) fp = 31 * fp + System.identityHashCode(r);
        Merged m = MERGED.get(manager);
        if (m != null && m.baseVersion == baseVersion && m.fingerprint == fp) return m.map;

        java.util.List<Map.Entry<net.minecraft.resources.ResourceLocation, MixerDataRecipe>> sorted = new java.util.ArrayList<>(data.entrySet());
        sorted.sort(java.util.Comparator.<Map.Entry<net.minecraft.resources.ResourceLocation, MixerDataRecipe>>comparingInt(e -> e.getValue().getOrder())
                .thenComparing(e -> e.getKey().toString()));
        Map<Fluid, java.util.List<MixerRecipe>> extra = new LinkedHashMap<>();
        java.util.Set<Fluid> replaced = new java.util.HashSet<>();
        for (Map.Entry<net.minecraft.resources.ResourceLocation, MixerDataRecipe> e : sorted) {
            Fluid out = e.getValue().outputFluid();
            MixerRecipe rec = e.getValue().toMixerRecipe();
            if (out == null || rec == null) continue;
            extra.computeIfAbsent(out, k -> new java.util.ArrayList<>()).add(rec);
            if (e.getValue().isReplace()) replaced.add(out);
        }
        Map<Fluid, MixerRecipe[]> merged = new LinkedHashMap<>(base);
        for (Map.Entry<Fluid, java.util.List<MixerRecipe>> e : extra.entrySet()) {
            MixerRecipe[] old = replaced.contains(e.getKey()) ? null : merged.get(e.getKey());
            java.util.List<MixerRecipe> all = new java.util.ArrayList<>();
            if (old != null) all.addAll(java.util.Arrays.asList(old));
            all.addAll(e.getValue());
            merged.put(e.getKey(), all.toArray(new MixerRecipe[0]));
        }
        MERGED.put(manager, new Merged(baseVersion, fp, merged));
        return merged;
    }

    /** Rezepte fuer ein Ausgabefluid inkl. Datapack ({@code level} null = nur Code-Rezepte). */
    @Nullable
    public static MixerRecipe[] getOutput(@Nullable net.minecraft.world.level.Level level, Fluid type) {
        return getRecipes(level != null ? level.getRecipeManager() : null).get(type);
    }

    @Nullable
    public static MixerRecipe[] getOutput(Fluid type) {
        return getRecipes().get(type);
    }

    @Nullable
    public static MixerRecipe getOutput(Fluid type, int index) {
        MixerRecipe[] recs = getRecipes().get(type);
        if (recs == null) return null;
        return recs[index % recs.length];
    }
}
