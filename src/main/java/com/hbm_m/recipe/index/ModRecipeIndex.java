package com.hbm_m.recipe.index;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Centralized, common (loader-agnostic) recipe indexes built on top of vanilla JSON recipes.
 *
 * Source of truth remains {@link RecipeManager}. This class only builds fast lookup tables:
 * - by id
 * - ordered list (stable, for GUI)
 * - blueprint pools (recipe -> pool, pool -> recipes)
 * - autoswitch groups (vanilla "group" string -> recipes)
 *
 * Cache key: {@link RecipeManager} identity + observed recipe count per type.
 */
public final class ModRecipeIndex {

    private ModRecipeIndex() {}

    private static final WeakHashMap<RecipeManager, PerManagerCache> CACHES = new WeakHashMap<>();

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

        private <T extends Recipe<?>> TypeIndex<T> getOrBuild(RecipeManager manager, RecipeType<T> type) {
            @SuppressWarnings("unchecked")
            TypeIndex<T> existing = (TypeIndex<T>) perType.get(type);

            int observedCount = safeCount(manager, type);
            if (existing != null && existing.observedCount == observedCount && existing.managerRef.get() == manager) {
                return existing;
            }

            TypeIndex<T> rebuilt = build(manager, type, observedCount);
            perType.put(type, rebuilt);
            return rebuilt;
        }
    }

    private static int safeCount(RecipeManager manager, RecipeType<?> type) {
        try {
            return manager.getAllRecipesFor((RecipeType) type).size();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static <T extends Recipe<?>> TypeIndex<T> build(RecipeManager manager, RecipeType<T> type, int observedCount) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<T> all = new ArrayList<>((List) manager.getAllRecipesFor((RecipeType) type));

        // Stable GUI order: by namespace:path (case-insensitive), tie-breaker by toString.
        all.sort(Comparator.comparing((T r) -> r.getId().toString().toLowerCase(Locale.ROOT))
                .thenComparing(r -> r.toString().toLowerCase(Locale.ROOT)));

        Map<ResourceLocation, T> byId = new java.util.HashMap<>(Math.max(16, all.size() * 2));
        Map<String, List<T>> byPoolMutable = new java.util.HashMap<>();
        Map<String, List<T>> byGroupMutable = new java.util.HashMap<>();

        for (T r : all) {
            byId.put(r.getId(), r);

            String pool = blueprintPoolOf(r);
            if (pool != null && !pool.isEmpty()) {
                byPoolMutable.computeIfAbsent(pool, k -> new ArrayList<>()).add(r);
            }

            String group = safeGroupOf(r);
            if (group != null && !group.isEmpty()) {
                byGroupMutable.computeIfAbsent(group, k -> new ArrayList<>()).add(r);
            }
        }

        Map<String, List<T>> byPool = freezeMapLists(byPoolMutable);
        Map<String, List<T>> byGroup = freezeMapLists(byGroupMutable);

        return new TypeIndex<>(manager, observedCount,
                Collections.unmodifiableList(all),
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(byPool),
                Collections.unmodifiableMap(byGroup));
    }

    private static <T> Map<String, List<T>> freezeMapLists(Map<String, List<T>> map) {
        if (map.isEmpty()) return Map.of();
        Map<String, List<T>> frozen = new java.util.HashMap<>(map.size());
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
            List<T> ordered,
            Map<ResourceLocation, T> byId,
            Map<String, List<T>> byPool,
            Map<String, List<T>> byGroup
    ) {
        private TypeIndex(RecipeManager manager,
                          int observedCount,
                          List<T> ordered,
                          Map<ResourceLocation, T> byId,
                          Map<String, List<T>> byPool,
                          Map<String, List<T>> byGroup) {
            this(new WeakReference<>(manager), observedCount, ordered, byId, byPool, byGroup);
        }
    }
}

