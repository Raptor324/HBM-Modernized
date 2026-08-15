package com.hbm_m.test;

import com.hbm_m.platform.PlatformHooks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Паритетный набор: проверяет, что НАБЛЮДАЕМЫЙ контракт {@link PlatformHooks}
 * одинаков на 1.20.1-forge и 1.21.1-neoforge.
 *
 * <p>Главный риск мульти-версионности — разница между <b>живой ссылкой</b>
 * (1.20.1 {@code stack.getTag()}) и <b>копией</b> (1.21.1 {@code data.copyTag()}).
 * Эти тесты фиксируют инварианты, которые <b>должны</b> выполняться на обеих
 * версиях, и таким образом ловят регрессии, если кто-то нарушит read-modify-write
 * контракт в {@code editItemTag} или случайно вернёт живой тег на 1.21.1.
 *
 * <p>Дополнительно: тесты на «копия-не-сохраняется» включают версионно-зависимое
 * ожидание через stonecutter-gating — на 1.21.1 мутация возвращённого {@code getItemTag}
 * тега НЕ должна персистить, на 1.20.1 — наоборот, должна (живая ссылка).
 * Это документирует ключевое семантическое различие, а не баг.
 *
 * <p>Структуры — собственные пустые шаблоны мода {@code hbm_m:empty3x3x3} и
 * {@code hbm_m:empty5x5x5}, генерируются скриптом
 * {@code scripts/gen_gametest_structures.py} в <b>обе</b> папки
 * ({@code data/hbm_m/structures/} для 1.20.1 и {@code data/hbm_m/structure/}
 * для 1.21.1 — MC 1.21 переименовал директорию ресурсов структур). Обе
 * аннотации FQN-gated (различаются между Forge/NeoForge):
 * {@code @GameTestHolder("hbm_m")} — (а) авто-регистрирует тест-методы
 * (поэтому {@link GameTestRegistration} не обязателен, но оставлен для явности)
 * и (б) задаёт {@code templateNamespace = "hbm_m"} для всех {@code @GameTest};
 * {@code @PrefixGameTestTemplate(false)} — отключает авто-префикс имени класса,
 * иначе {@code template="empty3x3x3"} превратилось бы в
 * {@code crossloaderparitygametest.empty3x3x3} и шаблон не нашёлся бы.
 *
 * <p>Запуск: {@code ./gradlew :1.21.1-neoforge:runGameTestServer} и
 * {@code ./gradlew "Set active project to 1.20.1-forge" && ./gradlew :1.20.1-forge:runGameTestServer}.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class CrossLoaderParityGameTest {

    private CrossLoaderParityGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 1: putLong → getLong round-trip. Одинаково на обеих версиях.
    //  Это базовый инвариант: независимо от того, копия это или живая ссылка,
    //  типизированный getter обязан вернуть записанное значение.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putLongGetLong_positive(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 1_000_000L);
        check(PlatformHooks.getLong(stack, "energy") == 1_000_000L,
                "putLong/getLong round-trip must hold on both versions");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putLongGetLong_zero(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 0L);
        check(PlatformHooks.getLong(stack, "energy") == 0L,
                "putLong(0) must round-trip on both versions");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putLongGetLong_maxLong(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", Long.MAX_VALUE);
        check(PlatformHooks.getLong(stack, "energy") == Long.MAX_VALUE,
                "putLong(Long.MAX_VALUE) must round-trip on both versions");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putLongGetLong_negative(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "debt", -500L);
        check(PlatformHooks.getLong(stack, "debt") == -500L,
                "putLong(negative) must round-trip on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 2: putInt → getInt round-trip.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putIntGetInt_roundTrip(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putInt(stack, "count", 42);
        check(PlatformHooks.getInt(stack, "count") == 42,
                "putInt/getInt round-trip must hold on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 3: putBoolean → getBoolean round-trip (оба значения).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putBooleanGetBoolean_true(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putBoolean(stack, "active", true);
        check(PlatformHooks.getBoolean(stack, "active"),
                "putBoolean(true)/getBoolean must round-trip on both versions");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putBooleanGetBoolean_false(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putBoolean(stack, "active", false);
        check(!PlatformHooks.getBoolean(stack, "active"),
                "putBoolean(false)/getBoolean must round-trip on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 4: putString → getString round-trip (включая UTF-8 — проект требует UTF-8).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putStringGetString_ascii(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putString(stack, "label", "reactor_core");
        check("reactor_core".equals(PlatformHooks.getString(stack, "label")),
                "putString/getString ASCII must round-trip on both versions");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_putStringGetString_utf8(GameTestHelper helper) {
        // Проект требует UTF-8 (см. java-coding-standards, build.*.gradle.kts).
        // Кириллица — частый случай в tooltip`ах и названиях.
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putString(stack, "ru", "Реактор");
        check("Реактор".equals(PlatformHooks.getString(stack, "ru")),
                "putString/GetString UTF-8 must round-trip on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 5: editItemTag — read-modify-write НЕ теряет соседние ключи.
    //  Это критичный инвариант: на 1.21.1 каждое editItemTag копирует→правит→set,
    //  поэтому легко потерять ключи, если редактор работает с пустым тегом.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_editItemTag_preservesAllKeys(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        PlatformHooks.putInt(stack, "tier", 2);
        PlatformHooks.putString(stack, "name", "core");
        PlatformHooks.putBoolean(stack, "active", true);
        // Теперь одну модификацию через editItemTag.
        PlatformHooks.editItemTag(stack, t -> t.putLong("energy", t.getLong("energy") + 50L));
        // Все ключи обязаны сохраниться + energy увеличился.
        check(PlatformHooks.getLong(stack, "energy") == 150L, "edited key must update");
        check(PlatformHooks.getInt(stack, "tier") == 2, "unrelated int key must persist");
        check("core".equals(PlatformHooks.getString(stack, "name")), "unrelated string key must persist");
        check(PlatformHooks.getBoolean(stack, "active"), "unrelated boolean key must persist");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 6: setItemTag(null) → getItemTag(null) — очистка контракта.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_setNullClears_tagBecomesNull(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        check(PlatformHooks.getItemTag(stack) != null, "precondition: tag present");
        PlatformHooks.setItemTag(stack, null);
        check(PlatformHooks.getItemTag(stack) == null,
                "after setItemTag(null), getItemTag must be null on both versions");
        check(!PlatformHooks.hasItemTag(stack),
                "after setItemTag(null), hasItemTag must be false on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 7: contains — наличие/отсутствие ключа.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_contains_afterPut_true(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 1L);
        check(PlatformHooks.contains(stack, "energy"),
                "contains must be true for existing key on both versions");
        check(!PlatformHooks.contains(stack, "missing"),
                "contains must be false for missing key on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 8: remove — удаляет ключ, сохраняет остальные.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_remove_isolatesKey(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "a", 1L);
        PlatformHooks.putLong(stack, "b", 2L);
        PlatformHooks.putLong(stack, "c", 3L);
        PlatformHooks.remove(stack, "b");
        check(!PlatformHooks.contains(stack, "b"), "removed key must be gone");
        check(PlatformHooks.contains(stack, "a"), "sibling a must persist");
        check(PlatformHooks.contains(stack, "c"), "sibling c must persist");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 9: save/load ItemStack с NBT — round-trip сохраняет custom-NBT.
    //  КРИТИЧНО: на 1.21.1 NBT хранится в CUSTOM_DATA DataComponent.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_saveLoad_preservesCustomNbt(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        ItemStack original = new ItemStack(Items.DIAMOND);
        PlatformHooks.putLong(original, "energy", 4242L);
        PlatformHooks.putString(original, "serial", "RX-001");
        PlatformHooks.putInt(original, "tier", 5);

        CompoundTag saved = PlatformHooks.safeItemSave(original, provider);
        ItemStack loaded = PlatformHooks.itemStackOf(saved, provider);

        check(loaded.getItem() == Items.DIAMOND, "item type must survive save/load");
        check(PlatformHooks.getLong(loaded, "energy") == 4242L, "custom long must survive save/load");
        check("RX-001".equals(PlatformHooks.getString(loaded, "serial")), "custom string must survive save/load");
        check(PlatformHooks.getInt(loaded, "tier") == 5, "custom int must survive save/load");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_saveLoad_preservesCount(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        ItemStack original = new ItemStack(Items.IRON_INGOT, 64);
        CompoundTag saved = PlatformHooks.safeItemSave(original, provider);
        ItemStack loaded = PlatformHooks.itemStackOf(saved, provider);
        check(loaded.getCount() == 64, "stack count must survive save/load on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 10: isSameItemSameTags — эквивалентность после save/load.
    //  Если round-trip проходит, оригинал и загруженный стек должны быть «одинаковыми».
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_saveLoad_isSameItemSameTags(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        ItemStack original = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(original, "energy", 777L);
        CompoundTag saved = PlatformHooks.safeItemSave(original, provider);
        ItemStack loaded = PlatformHooks.itemStackOf(saved, provider);
        check(PlatformHooks.isSameItemSameTags(original, loaded),
                "save/load round-trip must yield isSameItemSameTags == true on both versions");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Документация семантического различия (НЕ паритет, а фиксация контракта):
    //
    //  На 1.20.1 getItemTag() возвращает ЖИВУЮ ссылку — мутация сохраняется.
    //  На 1.21.1 getItemTag() возвращает КОПИЮ — мутация НЕ сохраняется.
    //
    //  Эти тесты НЕ должны падать — они фиксируют РАЗНОЕ ожидание на разных
    //  версиях, тем самым документируя, почему editItemTag — единственный
    //  безопасный способ мутации на обеих версиях.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void semantic_getItemTagMutation(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        check(tag != null, "precondition: tag present");
        // Мутируем возвращённый тег напрямую (НЕ через editItemTag).
        tag.putLong("energy", 999L);
        //? if < 1.21.1 {
        // 1.20.1: живая ссылка — мутация сохраняется.
        check(PlatformHooks.getLong(stack, "energy") == 999L,
                "1.20.1: getItemTag returns live reference, mutation must persist");
        //?} else {
        /*// 1.21.1: копия — мутация НЕ сохраняется. Значение осталось 100.
        check(PlatformHooks.getLong(stack, "energy") == 100L,
                "1.21.1: getItemTag returns copy, direct mutation must NOT persist (use editItemTag)");
        *///?}
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void semantic_editItemTag_alwaysPersists(GameTestHelper helper) {
        // В отличие от прямого getItemTag, editItemTag ОБЯЗАН персистить на обеих версиях.
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        PlatformHooks.editItemTag(stack, t -> t.putLong("energy", 999L));
        check(PlatformHooks.getLong(stack, "energy") == 999L,
                "editItemTag must persist on BOTH versions (read-modify-write contract)");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 11: ItemStack.EMPTY — граничный случай, не должен крашить.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_emptyStack_safeAccessors(GameTestHelper helper) {
        // Все типизированные getters на EMPTY стеке должны давать дефолты, не крашить.
        check(PlatformHooks.getInt(ItemStack.EMPTY, "x") == 0, "EMPTY getInt default");
        check(PlatformHooks.getLong(ItemStack.EMPTY, "x") == 0L, "EMPTY getLong default");
        check(!PlatformHooks.getBoolean(ItemStack.EMPTY, "x"), "EMPTY getBoolean default");
        check("".equals(PlatformHooks.getString(ItemStack.EMPTY, "x")), "EMPTY getString default");
        check(!PlatformHooks.contains(ItemStack.EMPTY, "x"), "EMPTY contains false");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Паритет 12: многократные перезаписи одного ключа — последнее значение выигрывает.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "parity", timeoutTicks = 100)
    public static void parity_overwriteKey_lastValueWins(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 1L);
        PlatformHooks.putLong(stack, "energy", 2L);
        PlatformHooks.putLong(stack, "energy", 3L);
        check(PlatformHooks.getLong(stack, "energy") == 3L,
                "last putLong for same key must win on both versions");
        helper.succeed();
    }
}
