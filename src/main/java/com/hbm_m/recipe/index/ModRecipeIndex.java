package com.hbm_m.recipe.index;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Centralized, common (loader-agnostic) recipe indexes built on top of vanilla JSON recipes.
 *
 * Source of truth remains {@link RecipeManager}. This class only builds fast lookup tables:
 * - by id
 * - ordered list (stable, for GUI)
 * - by ingredient item (for candidate filtering; recipes whose ingredients are dynamic —
 *   empty {@code Ingredient.getItems()} — are always included in every candidate list)
 * - reverse recipe -> id
 * - blueprint pools (recipe -> pool, pool -> recipes)
 * - autoswitch groups (vanilla "group" string -> recipes)
 *
 * Cache key: {@link RecipeManager} identity + observed recipe count per type + global epoch.
 * The epoch is bumped by {@link #invalidate()} (called from server-side reload/sync/tag
 * events) so a same-id recipe redefinition after /reload cannot serve a stale object.
 */
public final class ModRecipeIndex {

    private ModRecipeIndex() {}

    private static final WeakHashMap<RecipeManager, PerManagerCache> CACHES = new WeakHashMap<>();

    /** Monotonic epoch; bumped by invalidate() to drop all cached indexes. */
    private static final AtomicLong EPOCH = new AtomicLong(0);

    /**
     * Drops all cached indexes. Called from server-side glue (AddReloadListenerEvent,
     * OnDatapackSyncEvent, TagsUpdatedEvent) — see com.hbm_m.platform reload wiring.
     */
    public static void invalidate() {
        EPOCH.incrementAndGet();
        CACHES.clear();
    }

    /** Current epoch — consumers (MachineModuleBase etc.) snapshot it for staleness checks. */
    public static long epoch() {
        return EPOCH.get();
    }

    public static ModRecipeIndexView of(RecipeManager manager) {
        return new ModRecipeIndexView(manager);
    }

    public static final class ModRecipeIndexView {
        private final RecipeManager manager;

        private ModRecipeIndexView(RecipeManager manager) {
            this.manager = Objects.requireNonNull(manager, "manager");
        }

        public <T extends Recipe<?>> List<T> getAll(RecipeType<T> type) {
            return getTypeIndex(type).ordered();
        }

        public <T extends Recipe<?>> Optional<T> getById(RecipeType<T> type, ResourceLocation id) {
            if (id == null) return Optional.empty();
            return Optional.ofNullable(getTypeIndex(type).byId().get(id));
        }

        /** Cached shared id→recipe map (unmodifiable) — exposed for RecipeHooks.getAllRecipesById. */
        public <T extends Recipe<?>> Map<ResourceLocation, T> getByIdMap(RecipeType<T> type) {
            return getTypeIndex(type).byId();
        }

        /** O(1) reverse lookup recipe -> id, backed by the index's byRecipe identity map. */
        @Nullable
        public <T extends Recipe<?>> ResourceLocation getIdOf(RecipeType<T> type, T recipe) {
            if (recipe == null) return null;
            return getTypeIndex(type).byRecipe().get(recipe);
        }

        /**
         * Candidate subset for the given non-empty input stacks: union of byIngredient lists
         * for the present items, plus recipes with dynamic (empty getItems()) ingredients.
         * Falls back to the full ordered list when the union is empty or contains no known
         * item — no recipe can be silently missed by a snapshot mismatch.
         */
        public <T extends Recipe<?>> List<T> getCandidatesContaining(RecipeType<T> type, Iterable<ItemStack> inputs) {
            TypeIndex<T> idx = getTypeIndex(type);
            Set<T> candidates = null;
            boolean anyKnownItem = false;
            for (ItemStack stack : inputs) {
                if (stack == null || stack.isEmpty()) continue;
                List<T> list = idx.byIngredient().get(stack.getItem());
                if (list != null) {
                    anyKnownItem = true;
                    if (candidates == null) candidates = new LinkedHashSet<>();
                    candidates.addAll(list);
                }
            }
            // Dynamic/custom ingredients: recipes with empty getItems() are always candidates.
            List<T> dynamic = idx.byDynamicIngredient();
            if (candidates == null) {
                if (dynamic.isEmpty() || !anyKnownItem) return idx.ordered();
                return dynamic;
            }
            if (candidates.isEmpty() && !anyKnownItem) return idx.ordered();
            if (!dynamic.isEmpty()) candidates.addAll(dynamic);
            return List.copyOf(candidates);
        }

        public <T extends Recipe<?>> List<T> getByPool(RecipeType<T> type, String pool) {
            if (pool == null || pool.isEmpty()) return List.of();
            return getTypeIndex(type).byPool().getOrDefault(pool, List.of());
        }

        public <T extends Recipe<?>> List<T> getAutoSwitchGroup(RecipeType<T> type, String group) {
            if (group == null || group.isEmpty()) return List.of();
            return getTypeIndex(type).byGroup().getOrDefault(group, List.of());
        }

        private <T extends Recipe<?>> TypeIndex<T> getTypeIndex(RecipeType<T> type) {
            PerManagerCache per = CACHES.computeIfAbsent(manager, m -> new PerManagerCache());
            return per.getOrBuild(manager, type);
        }
    }

    private static final class PerManagerCache {
        // Identity map: RecipeType instances are singletons, safe to key by identity.
        private final Map<RecipeType<?>, TypeIndex<?>> perType = new IdentityHashMap<>();
        // Rate-limited fallback validation: at most one safeCount comparison per manager
        // per 1000 ms, bounding any missed invalidation event to 1 s of staleness and
        // <= 1 full-list copy per second (instead of 20/s per machine).
        private static final long VALIDATION_INTERVAL_MS = 1000;
        private long lastValidationMillis = Long.MIN_VALUE;

        private <T extends Recipe<?>> TypeIndex<T> getOrBuild(RecipeManager manager, RecipeType<T> type) {
            @SuppressWarnings("unchecked")
            TypeIndex<T> existing = (TypeIndex<T>) perType.get(type);

            if (existing != null && existing.epoch == EPOCH.get() && existing.managerRef.get() == manager) {
                long now = System.currentTimeMillis();
                if (now - lastValidationMillis >= VALIDATION_INTERVAL_MS) {
                    lastValidationMillis = now;
                    if (existing.observedCount != safeCount(manager, type)) {
                        existing = null;
                    }
                }
            }

            if (existing == null) {
                TypeIndex<T> rebuilt = build(manager, type);
                perType.put(type, rebuilt);
                return rebuilt;
            }
            return existing;
        }
    }

    private static int safeCount(RecipeManager manager, RecipeType<?> type) {
        try {
            return manager.getAllRecipesFor((RecipeType) type).size();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static <T extends Recipe<?>> TypeIndex<T> build(RecipeManager manager, RecipeType<T> type) {
        // id → recipe; на 1.21.1 id берётся из RecipeHolder (recipe.getId() удалён,
        // recipe.id == dummy после декода сериализатором).
        // ВАЖНО: напрямую из RecipeManager, НЕ через RecipeHooks.getAllRecipesById —
        // тот теперь идёт через кэш этого же класса (иначе бесконечная рекурсия).
        Map<ResourceLocation, T> byIdSource = new HashMap<>();
        //? if < 1.21.1 {
        for (Object r : manager.getAllRecipesFor((RecipeType) type)) {
            @SuppressWarnings("unchecked")
            T rec = (T) r;
            byIdSource.put(rec.getId(), rec);
        }
        //?} else {
        /*for (Object holder : manager.getAllRecipesFor((RecipeType) type)) {
            net.minecraft.world.item.crafting.RecipeHolder<T> h = (net.minecraft.world.item.crafting.RecipeHolder<T>) holder;
            byIdSource.put(h.id(), h.value());
        }
        *///?}

        // Stable GUI order: by namespace:path (case-insensitive), tie-breaker by toString.
        List<Map.Entry<ResourceLocation, T>> sortedEntries = new ArrayList<>(byIdSource.entrySet());
        sortedEntries.sort(Comparator.comparing((Map.Entry<ResourceLocation, T> e) -> e.getKey().toString().toLowerCase(Locale.ROOT))
                .thenComparing(e -> e.getValue().toString().toLowerCase(Locale.ROOT)));

        List<T> all = new ArrayList<>(sortedEntries.size());
        Map<ResourceLocation, T> byId = new HashMap<>(Math.max(16, sortedEntries.size() * 2));
        Map<String, List<T>> byPoolMutable = new HashMap<>();
        Map<String, List<T>> byGroupMutable = new HashMap<>();
        Map<Item, List<T>> byIngredientMutable = new HashMap<>();
        List<T> dynamicMutable = new ArrayList<>();
        // Reverse lookup recipe -> id; identity-based (recipes are never equal-compared).
        Map<Recipe<?>, ResourceLocation> byRecipe = new IdentityHashMap<>(Math.max(16, sortedEntries.size() * 2));

        for (Map.Entry<ResourceLocation, T> e : sortedEntries) {
            T r = e.getValue();
            all.add(r);
            byId.put(e.getKey(), r);
            byRecipe.put(r, e.getKey());

            String pool = blueprintPoolOf(r);
            if (pool != null && !pool.isEmpty()) {
                byPoolMutable.computeIfAbsent(pool, k -> new ArrayList<>()).add(r);
            }

            String group = safeGroupOf(r);
            if (group != null && !group.isEmpty()) {
                byGroupMutable.computeIfAbsent(group, k -> new ArrayList<>()).add(r);
            }

            indexIngredients(r, byIngredientMutable, dynamicMutable);
        }

        Map<String, List<T>> byPool = freezeMapLists(byPoolMutable);
        Map<String, List<T>> byGroup = freezeMapLists(byGroupMutable);
        Map<Item, List<T>> byIngredient = new HashMap<>(byIngredientMutable.size());
        for (Map.Entry<Item, List<T>> e : byIngredientMutable.entrySet()) {
            byIngredient.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }

        return new TypeIndex<>(manager, safeCount(manager, type), EPOCH.get(),
                Collections.unmodifiableList(all),
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(byIngredient),
                Collections.unmodifiableList(dynamicMutable),
                Collections.unmodifiableMap(byRecipe),
                Collections.unmodifiableMap(byPool),
                Collections.unmodifiableMap(byGroup));
    }

    /** Indexes a recipe by each of its ingredient items; dynamic ingredients go to the always-include list. */
    private static <T extends Recipe<?>> void indexIngredients(T recipe, Map<Item, List<T>> byIngredient, List<T> dynamic) {
        boolean anyItem = false;
        try {
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing == null) continue;
                // Note: getItems() on a tag-based ingredient reflects tag membership at index
                // build time; tag changes without a recipe rebuild are handled by the
                // TagsUpdatedEvent epoch bump in com.hbm_m.platform.
                for (ItemStack stack : ing.getItems()) {
                    if (stack == null || stack.isEmpty()) continue;
                    anyItem = true;
                    byIngredient.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(recipe);
                }
            }
        } catch (Throwable t) {
            // Ingredient expansion failed — treat as dynamic so the recipe is never missed.
            anyItem = false;
        }
        if (!anyItem) {
            dynamic.add(recipe);
        }
    }

    private static <T> Map<String, List<T>> freezeMapLists(Map<String, List<T>> map) {
        if (map.isEmpty()) return Map.of();
        Map<String, List<T>> frozen = new HashMap<>(map.size());
        for (Map.Entry<String, List<T>> e : map.entrySet()) {
            frozen.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    @Nullable
    private static String safeGroupOf(Recipe<?> recipe) {
        try {
            // Vanilla JSON "group" routes here for recipes that support it.
            return recipe.getGroup();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Common blueprint-pool accessor without adding loader-specific APIs.
     *
     * If/when more recipe types adopt blueprint pools, add them here.
     */
    @Nullable
    public static String blueprintPoolOf(Recipe<?> recipe) {
        if (recipe instanceof ChemicalPlantRecipe c) return c.getBlueprintPool();
        if (recipe instanceof AssemblerRecipe a) return a.getBlueprintPool();
        if (recipe instanceof AnvilRecipe an) return an.getBlueprintPool();
        return null;
    }

    private record TypeIndex<T extends Recipe<?>>(
            WeakReference<RecipeManager> managerRef,
            int observedCount,
            long epoch,
            List<T> ordered,
            Map<ResourceLocation, T> byId,
            Map<Item, List<T>> byIngredient,
            List<T> byDynamicIngredient,
            Map<Recipe<?>, ResourceLocation> byRecipe,
            Map<String, List<T>> byPool,
            Map<String, List<T>> byGroup
    ) {
        private TypeIndex(RecipeManager manager,
                          int observedCount,
                          long epoch,
                          List<T> ordered,
                          Map<ResourceLocation, T> byId,
                          Map<Item, List<T>> byIngredient,
                          List<T> byDynamicIngredient,
                          Map<Recipe<?>, ResourceLocation> byRecipe,
                          Map<String, List<T>> byPool,
                          Map<String, List<T>> byGroup) {
            this(new WeakReference<>(manager), observedCount, epoch, ordered, byId, byIngredient,
                    byDynamicIngredient, byRecipe, byPool, byGroup);
        }
    }
}
