package com.hbm_m.recipe.index;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * BE-agnostic driver for the cached recipe-scan pattern (вынесено из гейта
 * MachineModuleBase). Кэширует результат сканирования рецептов на тик+подпись:
 * повторные вызовы в пределах одного тика с той же подписью не пересканируют список.
 *
 * <p>Скан идёт по {@link ModRecipeIndex#of} {@code getAll(type)} — общий
 * PerManagerCache-список (на 1.21.1 разворачивает {@code RecipeHolder}), а НЕ через
 * {@code RecipeHooks.getAllRecipes} (там копия списка на каждый вызов).
 * Эпоха {@link ModRecipeIndex#epoch()} сбрасывает кэш при {@code /reload} даже при
 * неизменных входных стаках.
 *
 * <p>CONTRACT 1 (scan purity): the Scanner contains ONLY the recipe match over the inputs
 * encoded in the signature. Anything depending on state outside the signature (output-slot
 * headroom, canAcceptResult, canAcceptOutput, energy) is re-evaluated PER TICK by the caller
 * against the returned cached recipe — mirroring MachineModuleBase's canProcess-per-tick
 * split. A cached null means 'no recipe matches these inputs', never 'the machine cannot
 * process right now'.
 *
 * <p>CONTRACT 2 (value keying): the signature is built from the STACK ARGUMENT(s) of the
 * findXxxRecipe method (см. {@link com.hbm_m.platform.PlatformHooks#stackFingerprint}) —
 * never from a positional slot/queue index; identical stacks legitimately share the cached
 * answer.
 *
 * <p>NOTE: source-verified only — the resolver never touches RecipeManager input APIs
 * (only {@link ModRecipeIndex#getAll}), so the {@code R}-generic stays RecipeInput-free
 * on both 1.20.1 and 1.21.1.
 */
public final class CachedRecipeResolver<R extends Recipe<?>> {

    /** Чистый матч рецепта по входам, закодированным в подписи. Не смотрит в состояние машины вне подписи. */
    public interface Scanner<R> {
        @Nullable R scan(Level level, List<R> all);
    }

    private long sig = Long.MIN_VALUE;
    private long epoch = -1L;
    private boolean scanned = false;
    @Nullable
    private R cached;

    /**
     * Возвращает кэшированный рецепт (или null) для данной подписи входов.
     * Кэшируется и негативный результат (null = 'нет рецепта под эти входы').
     */
    @Nullable
    public R resolve(Level level, RecipeType<R> type, long signature, Scanner<R> scanner) {
        long now = ModRecipeIndex.epoch();
        if (scanned && epoch == now && signature == this.sig) return cached; // negative cached too
        this.sig = signature;
        this.epoch = now;
        this.scanned = true;
        this.cached = scanner.scan(level, ModRecipeIndex.of(level.getRecipeManager()).getAll(type));
        return this.cached;
    }

    /** Принудительный сброс (обычно не нужен — epoch покрывает /reload). */
    public void invalidate() {
        scanned = false;
    }

    /** Хелпер сборки подписи: seed + произвольные long-части (фингерпринты стаков/жидкостей). */
    public static long sign(long seed, long... parts) {
        long s = seed * 31L + 17L;
        for (long p : parts) s = s * 31L + p;
        return s;
    }
}
