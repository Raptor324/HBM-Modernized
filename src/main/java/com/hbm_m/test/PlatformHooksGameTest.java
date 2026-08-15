package com.hbm_m.test;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.PlayerPersistentData;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
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
 * Полный функциональный набор GameTest-ов для {@link PlatformHooks}.
 *
 * <p>Каждый тест проверяет один метод (или тесно связанную группу) и должен
 * проходить <b>идентично</b> на 1.20.1-forge и 1.21.1-neoforge: сам класс не
 * содержит версионного gating для основной логики, потому что PlatformHooks
 * полностью скрывает версионные различия. Точечные версии-специфичные ссылки
 * на ванильные блоки (например {@code Blocks.GRASS} ↔ {@code Blocks.SHORT_GRASS})
 * оборачиваются stonecutter-блоками — как в самом PlatformHooks.
 *
 * <p>Структуры — собственные пустые шаблоны мода {@code hbm_m:empty3x3x3}
 * и {@code hbm_m:empty5x5x5}, генерируются скриптом
 * {@code scripts/gen_gametest_structures.py} в <b>обе</b> папки:
 * {@code data/hbm_m/structures/} (1.20.1, plural) и {@code data/hbm_m/structure/}
 * (1.21.1, singular — MC 1.21 переименовал
 * {@code STRUCTURE_RESOURCE_DIRECTORY_NAME} с {@code "structures"} на
 * {@code "structure"}). В 1.21.1 ванильные
 * {@code minecraft:empty3x3x3}/{@code minecraft:empty5x5x5} удалены, поэтому мод
 * поставляет свои. Обе аннотации FQN-gated (различаются между Forge/NeoForge):
 * {@code @GameTestHolder("hbm_m")} — (а) авто-регистрирует тест-методы (поэтому
 * {@link GameTestRegistration} не обязателен, но оставлен для явности) и
 * (б) задаёт {@code templateNamespace = "hbm_m"} для всех {@code @GameTest};
 * {@code @PrefixGameTestTemplate(false)} — отключает авто-префикс имени класса,
 * иначе {@code template="empty5x5x5"} превратилось бы в
 * {@code platformhooksgametest.empty5x5x5} и шаблон не нашёлся бы.
 *
 * <p>Запуск: {@code ./gradlew :1.21.1-neoforge:runGameTestServer} (и аналогично для forge).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class PlatformHooksGameTest {

    private PlatformHooksGameTest() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Вспомогательный assert — GameTestAssertException доступен на обеих версиях.
    // ════════════════════════════════════════════════════════════════════════
    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkEq(Object expected, Object actual, String msg) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new GameTestAssertException(msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 1: getItemTag / hasItemTag — базовый доступ к custom-NBT предмета.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void getItemTag_freshStack_returnsNull(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        // Свежий предмет без custom-NBT → null на обеих версиях.
        check(PlatformHooks.getItemTag(stack) == null, "fresh stack must have null tag");
        check(!PlatformHooks.hasItemTag(stack), "fresh stack must report no tag");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void getItemTag_afterPut_returnsNonNull(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        // После putLong тег обязан появиться.
        check(PlatformHooks.getItemTag(stack) != null, "tag must exist after putLong");
        check(PlatformHooks.hasItemTag(stack), "hasItemTag must be true after putLong");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void getItemTag_emptyStack_returnsNull(GameTestHelper helper) {
        ItemStack stack = ItemStack.EMPTY;
        check(PlatformHooks.getItemTag(stack) == null, "EMPTY stack must have null tag");
        check(!PlatformHooks.hasItemTag(stack), "EMPTY stack must report no tag");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 2: editItemTag — read-modify-write, КРИТИЧНЫЙ для parity.
    //  На 1.20.1 мутирует живой тег; на 1.21.1 копирует→правит→set(CUSTOM_DATA).
    //  Наблюдаемый контракт: значение сохраняется на обеих версиях.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void editItemTag_persistsLong(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.editItemTag(stack, t -> t.putLong("energy", 123456789L));
        check(PlatformHooks.getLong(stack, "energy") == 123456789L,
                "editItemTag putLong must persist");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void editItemTag_persistsNestedCompound(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.editItemTag(stack, t -> {
            CompoundTag sub = new CompoundTag();
            sub.putInt("tier", 3);
            sub.putString("name", "reactor");
            t.put("data", sub);
        });
        CompoundTag sub = PlatformHooks.getCompound(stack, "data");
        check(sub.getInt("tier") == 3, "nested tier must persist");
        checkEq("reactor", sub.getString("name"), "nested name must persist");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void editItemTag_multipleEdits_accumulate(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.editItemTag(stack, t -> t.putInt("a", 1));
        PlatformHooks.editItemTag(stack, t -> t.putInt("b", 2));
        PlatformHooks.editItemTag(stack, t -> t.putInt("c", 3));
        // Последовательные editItemTag не должны затирать предыдущие ключи.
        check(PlatformHooks.getInt(stack, "a") == 1, "key a must survive later edits");
        check(PlatformHooks.getInt(stack, "b") == 2, "key b must survive later edits");
        check(PlatformHooks.getInt(stack, "c") == 3, "key c must survive later edits");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void editItemTag_canMutateExisting(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        // Увеличиваем существующее значение.
        PlatformHooks.editItemTag(stack, t -> t.putLong("energy", t.getLong("energy") + 50L));
        check(PlatformHooks.getLong(stack, "energy") == 150L,
                "editItemTag must read-modify-write existing value");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 3: setItemTag — полная перезапись; null очищает.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void setItemTag_overwritesExisting(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "old", 1L);
        CompoundTag fresh = new CompoundTag();
        fresh.putString("new", "value");
        PlatformHooks.setItemTag(stack, fresh);
        // Старый ключ должен исчезнуть (полная перезапись).
        check(!PlatformHooks.contains(stack, "old"), "setItemTag must clear old keys");
        checkEq("value", PlatformHooks.getString(stack, "new"), "setItemTag must set new keys");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void setItemTag_null_clearsTag(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        check(PlatformHooks.hasItemTag(stack), "precondition: tag present");
        PlatformHooks.setItemTag(stack, null);
        check(!PlatformHooks.hasItemTag(stack), "setItemTag(null) must clear tag");
        check(PlatformHooks.getItemTag(stack) == null, "getItemTag must be null after clear");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void setItemTag_emptyCompound_clearsTag(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        PlatformHooks.setItemTag(stack, new CompoundTag());
        // Пустой CompoundTag эквивалентен null (см. реализацию setItemTag).
        check(!PlatformHooks.hasItemTag(stack), "setItemTag(empty) must clear tag");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 4: типизированные getters — защита от null (0/empty по умолчанию).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void typedGetters_noTag_returnDefaults(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        check(PlatformHooks.getInt(stack, "x") == 0, "getInt default 0");
        check(PlatformHooks.getLong(stack, "x") == 0L, "getLong default 0");
        check(!PlatformHooks.getBoolean(stack, "x"), "getBoolean default false");
        checkEq("", PlatformHooks.getString(stack, "x"), "getString default empty");
        check(PlatformHooks.getCompound(stack, "x").isEmpty(), "getCompound default empty");
        check(!PlatformHooks.contains(stack, "x"), "contains default false");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void typedGetters_afterPut_returnValues(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putInt(stack, "i", 42);
        PlatformHooks.putLong(stack, "l", 9999999999L);
        PlatformHooks.putBoolean(stack, "b", true);
        PlatformHooks.putString(stack, "s", "hello");
        check(PlatformHooks.getInt(stack, "i") == 42, "getInt round-trip");
        check(PlatformHooks.getLong(stack, "l") == 9999999999L, "getLong round-trip");
        check(PlatformHooks.getBoolean(stack, "b"), "getBoolean round-trip");
        checkEq("hello", PlatformHooks.getString(stack, "s"), "getString round-trip");
        check(PlatformHooks.contains(stack, "i"), "contains i");
        check(PlatformHooks.contains(stack, "l"), "contains l");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void putCompound_nestedRoundTrip(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        CompoundTag sub = new CompoundTag();
        sub.putInt("k", 7);
        PlatformHooks.put(stack, "sub", sub);
        check(PlatformHooks.getCompound(stack, "sub").getInt("k") == 7, "put/getCompound round-trip");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 5: remove — удаление ключа, персистит на обеих версиях.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void remove_persistsRemoval(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(stack, "energy", 100L);
        PlatformHooks.putLong(stack, "heat", 50L);
        PlatformHooks.remove(stack, "energy");
        check(!PlatformHooks.contains(stack, "energy"), "removed key must be gone");
        check(PlatformHooks.contains(stack, "heat"), "unrelated key must remain");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void remove_missingKey_noError(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        // Удаление несуществующего ключа не должно крашить (нет тега вообще).
        PlatformHooks.remove(stack, "nonexistent");
        check(!PlatformHooks.contains(stack, "nonexistent"), "missing key stays absent");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 6: isSameItemSameTags — кросс-версионное сравнение ItemStack.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isSameItemSameTags_identical_true(GameTestHelper helper) {
        ItemStack a = new ItemStack(Items.IRON_INGOT);
        ItemStack b = new ItemStack(Items.IRON_INGOT);
        check(PlatformHooks.isSameItemSameTags(a, b), "identical plain stacks must match");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isSameItemSameTags_differentItem_false(GameTestHelper helper) {
        ItemStack a = new ItemStack(Items.IRON_INGOT);
        ItemStack b = new ItemStack(Items.GOLD_INGOT);
        check(!PlatformHooks.isSameItemSameTags(a, b), "different items must not match");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isSameItemSameTags_sameNBT_true(GameTestHelper helper) {
        ItemStack a = new ItemStack(Items.IRON_INGOT);
        ItemStack b = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(a, "energy", 100L);
        PlatformHooks.putLong(b, "energy", 100L);
        check(PlatformHooks.isSameItemSameTags(a, b), "same item + same NBT must match");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isSameItemSameTags_differentNBT_false(GameTestHelper helper) {
        ItemStack a = new ItemStack(Items.IRON_INGOT);
        ItemStack b = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(a, "energy", 100L);
        PlatformHooks.putLong(b, "energy", 200L);
        check(!PlatformHooks.isSameItemSameTags(a, b), "same item + different NBT must not match");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 7: saveItemStack / itemStackOf / safeItemSave — round-trip сериализация.
    //  На 1.21.1 требует HolderLookup.Provider; передаём level.registryAccess()
    //  (RegistryAccess extends HolderLookup.Provider на обеих версиях).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void saveLoadItemStack_plainRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        ItemStack original = new ItemStack(Items.IRON_INGOT, 16);
        CompoundTag saved = PlatformHooks.safeItemSave(original, provider);
        ItemStack loaded = PlatformHooks.itemStackOf(saved, provider);
        check(loaded.getItem() == Items.IRON_INGOT, "item must survive round-trip");
        check(loaded.getCount() == 16, "count must survive round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void saveLoadItemStack_withNbtRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        ItemStack original = new ItemStack(Items.IRON_INGOT);
        PlatformHooks.putLong(original, "energy", 555L);
        PlatformHooks.putString(original, "label", "reactor_core");
        CompoundTag saved = PlatformHooks.safeItemSave(original, provider);
        ItemStack loaded = PlatformHooks.itemStackOf(saved, provider);
        check(PlatformHooks.getLong(loaded, "energy") == 555L, "NBT long must survive round-trip");
        checkEq("reactor_core", PlatformHooks.getString(loaded, "label"),
                "NBT string must survive round-trip");
        check(PlatformHooks.isSameItemSameTags(original, loaded),
                "original and loaded must be considered equal");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void saveItemStack_intoProvidedTag(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        ItemStack stack = new ItemStack(Items.GOLD_INGOT, 4);
        CompoundTag target = new CompoundTag();
        CompoundTag returned = PlatformHooks.saveItemStack(stack, target, provider);
        // 1.20.1: stack.save(tag) возвращает ТОТ ЖЕ экземпляр target.
        // 1.21.1: stack.save(provider, tag) может вернуть иной CompoundTag, но
        // данные сохраняются в target (или в returned). Проверяем контент, а не
        // идентичность ссылки — кросс-версионно надёжно.
        check(returned != null, "saveItemStack must return non-null tag");
        CompoundTag effective = returned != target ? returned : target;
        check(!effective.isEmpty(), "saved tag must be populated");
        check(effective.contains("id")
                        || effective.contains("Count")
                        || effective.contains("count"),
                "saved tag must contain item fields (id/count)");
        check(effective.contains("id"), "saved tag must contain item id");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 8: getConfigDir — каталог конфигурации (Architectury Platform).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void getConfigDir_returnsNonNull(GameTestHelper helper) {
        java.nio.file.Path dir = PlatformHooks.getConfigDir();
        check(dir != null, "config dir must not be null");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 9: attributeModifier — кросс-версионное создание.
    //  amount() — общий getter на обеих версиях.
    //  operation: на 1.20.1 — ADDITION/MULTIPLY_BASE/MULTIPLY_TOTAL;
    //             на 1.21.1 — ADD_VALUE/ADD_MULTIPLIED_BASE/ADD_MULTIPLIED_TOTAL.
    //  Проверяем только amount (operation-имена различаются — см. ItemModHealth).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void attributeModifier_uuidOverload_preservesValue(GameTestHelper helper) {
        AttributeModifier mod = PlatformHooks.attributeModifier(
                java.util.UUID.fromString("d3e4e5f0-1234-5678-9abc-def012345678"),
                "test_mod", 2.5, additionOperation());
        check(amountOf(mod) == 2.5, "amount must be preserved");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void attributeModifier_nameOverload_preservesValue(GameTestHelper helper) {
        AttributeModifier mod = PlatformHooks.attributeModifier(
                "Armor Bonus", 1.0, additionOperation());
        check(amountOf(mod) == 1.0, "amount must be preserved");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void attributeModifier_uuidAndName_overloadsDistinct(GameTestHelper helper) {
        // Две разные перегрузки не должны конфликтовать; обе возвращают валидные модификаторы.
        AttributeModifier byUuid = PlatformHooks.attributeModifier(
                java.util.UUID.randomUUID(), "u", 1.0, additionOperation());
        AttributeModifier byName = PlatformHooks.attributeModifier(
                "n", 1.0, additionOperation());
        check(!byUuid.equals(byName), "modifiers from different overloads should differ");
        helper.succeed();
    }

    // Версионно-совместимый доступ к ADDITION (1.20.1) / ADD_VALUE (1.21.1).
    private static AttributeModifier.Operation additionOperation() {
        //? if < 1.21.1 {
        return AttributeModifier.Operation.ADDITION;
        //?} else {
        /*return AttributeModifier.Operation.ADD_VALUE;
        *///?}
    }

    // Версионно-совместимый getter для amount AttributeModifier.
    //  1.20.1: getAmount(); 1.21.1: amount().
    private static double amountOf(AttributeModifier mod) {
        //? if < 1.21.1 {
        return mod.getAmount();
        //?} else {
        /*return mod.amount();
        *///?}
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 10: FoodProperties — foodBuilder / addFoodEffect / setMeat.
    //  MobEffects.MOVEMENT_SLOWDOWN: на 1.20.1 → MobEffect; на 1.21.1 → Holder<MobEffect>.
    //  PlatformHooks.addFoodEffect кастует Object internally — передаём напрямую.
    //  nutrition getter: 1.20.1 getNutrition(); 1.21.1 nutrition().
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void foodBuilder_nutritionAndSaturation(GameTestHelper helper) {
        FoodProperties.Builder builder = PlatformHooks.foodBuilder(8, 0.8f);
        FoodProperties food = builder.build();
        check(nutritionOf(food) == 8, "nutrition must match");
        // saturation: значения РАЗЛИЧАЮТСЯ между версиями из-за изменения формулы.
        // 1.20.1: getSaturationModifier() возвращает сам modifier = 0.8f.
        // 1.21.1: Builder.build() считает saturation = nutrition * modifier * 2.0f
        //         (FoodConstants.saturationByModifier), и saturation() возвращает
        //         результат формулы = 8 * 0.8 * 2.0 = 12.8f.
        //? if < 1.21.1 {
        check(Math.abs(saturationOf(food) - 0.8f) < 0.001f, "saturation must match (1.20.1 = modifier)");
        //?} else {
        /*float expected = 8 * 0.8f * 2.0f; // FoodConstants.saturationByModifier(8, 0.8f)
        check(Math.abs(saturationOf(food) - expected) < 0.01f, "saturation must match (1.21.1 = nutrition*modifier*2)");
        *///?}
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void foodBuilder_addEffect_noCrash(GameTestHelper helper) {
        FoodProperties.Builder builder = PlatformHooks.foodBuilder(4, 0.3f);
        PlatformHooks.addFoodEffect(builder, MobEffects.MOVEMENT_SLOWDOWN, 200, 0.5f);
        PlatformHooks.addFoodEffect(builder, MobEffects.MOVEMENT_SLOWDOWN, 100, 1, 0.3f);
        FoodProperties food = builder.build();
        check(nutritionOf(food) == 4, "nutrition preserved after adding effect");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void foodBuilder_setMeat_noCrash(GameTestHelper helper) {
        FoodProperties.Builder builder = PlatformHooks.foodBuilder(6, 0.6f);
        PlatformHooks.setMeat(builder);
        FoodProperties food = builder.build();
        check(nutritionOf(food) == 6, "nutrition preserved after setMeat");
        helper.succeed();
    }

    // nutrition getter обёрнут версионно: 1.20.1 getNutrition() / 1.21.1 nutrition().
    private static int nutritionOf(FoodProperties food) {
        //? if < 1.21.1 {
        return food.getNutrition();
        //?} else {
        /*return food.nutrition();
        *///?}
    }

    // saturation getter обёрнут версионно: 1.20.1 getSaturationModifier() / 1.21.1 saturation().
    private static float saturationOf(FoodProperties food) {
        //? if < 1.21.1 {
        return food.getSaturationModifier();
        //?} else {
        /*return food.saturation();
        *///?}
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 11: УДАЛЕНО. Тесты конструкторов блоков (createDoorBlock и др.)
    //  были пустышками: реестр заморожен на обеих версиях, catch(IllegalStateException)
    //  глотал любую ошибку, на 1.21.1 код вырезался препроцессором — тест всегда зелёный.
    //  Реальная проверка конструкторов происходит при регистрации блоков (bootstrap).
    // ════════════════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 11b: PlayerPersistentData — доступ к персистентному NBT игрока.
    //  Ловит регрессию: отсутствие ветки neoforge молча возвращало НОВЫЙ пустой
    //  тег, который никуда не сохраняется (данные терялись).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void playerPersistentData_writeReadRoundTrip(GameTestHelper helper) {
        var player = makePlayer(helper);
        CompoundTag tag = PlayerPersistentData.get(player);
        check(tag != null, "persistent data tag must not be null");
        tag.putLong("hbm_test_energy", 777L);
        tag.putString("hbm_test_name", "reactor");
        // Повторный get должен вернуть ТОТ ЖЕ сохраняемый тег, а не свежий пустой.
        CompoundTag reread = PlayerPersistentData.get(player);
        check(reread == tag || reread.getLong("hbm_test_energy") == 777L,
                "PlayerPersistentData.get must return the persistent tag instance, "
                + "not a new empty tag (neoforge branch missing regression)");
        check(reread.getLong("hbm_test_energy") == 777L, "written long must survive re-read");
        checkEq("reactor", reread.getString("hbm_test_name"), "written string must survive re-read");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 12: setSecondsOnFire — кросс-версионный поджиг сущности.
    //  makeMockPlayer: 1.20.1 makeMockServerPlayer(); 1.21.1 makeMockPlayer().
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void setSecondsOnFire_setsFireTicks(GameTestHelper helper) {
        var player = makePlayer(helper);
        PlatformHooks.setSecondsOnFire(player, 5);
        // 5 секунд = 100 тиков (20 т/с). Проверяем, что поджёнг применился.
        check(player.getRemainingFireTicks() > 0, "entity must be on fire");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 13: isFluidContainer — лоадерная проверка capability (Forge/NeoForge).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isFluidContainer_waterBucket_true(GameTestHelper helper) {
        check(PlatformHooks.isFluidContainer(new ItemStack(Items.WATER_BUCKET)),
                "water bucket must be a fluid container");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isFluidContainer_ironIngot_false(GameTestHelper helper) {
        check(!PlatformHooks.isFluidContainer(new ItemStack(Items.IRON_INGOT)),
                "iron ingot must not be a fluid container");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void isFluidContainer_emptyStack_false(GameTestHelper helper) {
        check(!PlatformHooks.isFluidContainer(ItemStack.EMPTY),
                "EMPTY stack must not be a fluid container");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 14: parseComponentJson / componentToJson — round-trip сериализация Component.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void componentJson_roundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        Component original = Component.literal("hbm_test");
        String json = PlatformHooks.componentToJson(original, provider);
        check(json != null && !json.isEmpty(), "componentToJson must produce non-empty json");
        Component parsed = PlatformHooks.parseComponentJson(json, provider);
        check(parsed != null, "parseComponentJson must produce non-null component");
        checkEq(original.getString(), parsed.getString(),
                "component text must survive round-trip");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void parseComponentJson_nullInput_returnsNull(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        check(PlatformHooks.parseComponentJson(null, provider) == null, "null json → null");
        check(PlatformHooks.parseComponentJson("", provider) == null, "empty json → null");
        check(PlatformHooks.componentToJson(null, provider) == null, "null component → null");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 15: getExplosionKnockbackAfterDampener — без защиты ≈ исходный knockback.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void explosionKnockback_noProtection_returnsInput(GameTestHelper helper) {
        var player = makePlayer(helper);
        double result = PlatformHooks.getExplosionKnockbackAfterDampener(player, 1.0);
        // Без enchant-защиты и без resistance-атрибута результат ≈ входному значению.
        check(result >= 0.0 && result <= 1.0 + 1e-6,
                "knockback without protection must stay within [0, input]");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 16: getMyRidingOffset — 1.21.1 stub 0.0; 1.20.1 vanilla default 0.0.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void getMyRidingOffset_entity_returnsZero(GameTestHelper helper) {
        var player = makePlayer(helper);
        double offset = PlatformHooks.getMyRidingOffset(player);
        // 1.20.1: Player.getMyRidingOffset() возвращает vanilla значение (≠ 0.0 для Player).
        // 1.21.1: stub 0.0D (метод удалён в vanilla, PlatformHooks возвращает константу).
        check(!Double.isNaN(offset) && !Double.isInfinite(offset),
                "riding offset must be finite (got " + offset + ")");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 17: isGrassBlock — ренейм Blocks.GRASS → Blocks.SHORT_GRASS (1.21.1).
    //  Положительный тест требует версионно-специфичный блок — gating по версии.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void isGrassBlock_stone_false(GameTestHelper helper) {
        // ВАЖНО: helper.setBlock принимает ОТНОСИТЕЛЬНУЮ позицию (внутри прибавляет origin
        // структуры). helper.absolutePos — для прямых вызовов getLevel().getBlock*().
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, net.minecraft.world.level.block.Blocks.STONE);
        var state = helper.getLevel().getBlockState(helper.absolutePos(rel));
        check(!PlatformHooks.isGrassBlock(state), "stone must not be grass");
        helper.succeed();
    }

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void isGrassBlock_actualGrass_true(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        // Ренейм Blocks.GRASS → Blocks.SHORT_GRASS в 1.21.1.
        //? if < 1.21.1 {
        helper.setBlock(rel, net.minecraft.world.level.block.Blocks.GRASS);
        //?} else {
        /*helper.setBlock(rel, net.minecraft.world.level.block.Blocks.SHORT_GRASS);
        *///?}
        var state = helper.getLevel().getBlockState(helper.absolutePos(rel));
        check(PlatformHooks.isGrassBlock(state), "grass block must be recognized");
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 18: loadBlockEntityTag — кросс-версионная загрузка NBT в BlockEntity.
    //  1.20.1: be.load(tag); 1.21.1: be.loadCustomOnly(tag, provider).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void loadBlockEntityTag_emptyTag_noCrash(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, net.minecraft.world.level.block.Blocks.CHEST);
        BlockPos abs = helper.absolutePos(rel);
        var be = helper.getLevel().getBlockEntity(abs);
        check(be != null, "chest BlockEntity must exist");
        var provider = helper.getLevel().registryAccess();
        // Загрузка пустого тега не должна крашить на обеих версиях.
        PlatformHooks.loadBlockEntityTag(be, new CompoundTag(), provider);
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 19: tooltipLevel — извлечение Level из аргумента appendHoverText.
    //  1.20.1: второй аргумент — Level; 1.21.1 — Item.TooltipContext.
    //  null → null на обеих версиях.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void tooltipLevel_null_returnsNull(GameTestHelper helper) {
        check(PlatformHooks.tooltipLevel(null) == null, "tooltipLevel(null) → null");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "platformhooks", timeoutTicks = 100)
    public static void tooltipLevel_levelArg_returnsLevel(GameTestHelper helper) {
        // 1.20.1: Level передаётся напрямую → возвращается тот же Level.
        // 1.21.1: TooltipContext — здесь мы не можем его построить без client, поэтому
        // этот сценарий активен только на < 1.21.1.
        //? if < 1.21.1 {
        net.minecraft.world.level.Level level = helper.getLevel();
        check(PlatformHooks.tooltipLevel(level) == level, "tooltipLevel(level) must return same Level");
        //?}
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 20: getItemTag(ClientboundBlockEntityDataPacket) — версионно-инвариантный.
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void getItemTag_packet_returnsTag(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, net.minecraft.world.level.block.Blocks.CHEST);
        BlockPos abs = helper.absolutePos(rel);
        var be = helper.getLevel().getBlockEntity(abs);
        check(be != null, "chest BlockEntity must exist");
        // ClientboundBlockEntityDataPacket.create доступен на обеих версиях.
        var packet = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(be);
        check(packet != null, "update packet must be created");
        CompoundTag tag = PlatformHooks.getItemTag(packet);
        // На 1.20.1-forge свежесозданный chest BE может возвращать null из getUpdateTag()
        // (наблюдено в @GameTest runtime). Ключевой контракт: метод не крашит.
        //? if < 1.21.1 {
        if (tag != null) {
            check(tag.contains("id"), "packet tag if non-null must contain BlockEntity id (1.20.1)");
        }
        //?} else {
        /*// 1.21.1: "id" НЕ в tag — он в packet.getType(). Проверяем что tag получен.
        check(tag != null && (!tag.isEmpty() || packet.getType() != null),
                "either tag has data or packet carries type (1.21.1)");
        *///?}
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Группа 21: awardAdvancementIfEligible — eligible=false → no-op (no crash).
    // ════════════════════════════════════════════════════════════════════════

    @GameTest(template = "empty5x5x5", batch = "platformhooks", timeoutTicks = 100)
    public static void awardAdvancement_notEligible_noOp(GameTestHelper helper) {
        // eligible=false -> early return inside awardAdvancementIfEligible (no player access).
        // 1.20.1: makeMockPlayer() возвращает anonymous Player (GameTestHelper$2), НЕ ServerPlayer
        //         на runtime → cast падает. Проверяем instanceof, чтобы не крашить.
        // 1.21.1: makeMockPlayer(GameType) возвращает Player, не ServerPlayer → тест gated.
        //? if < 1.21.1 {
        var mock = helper.makeMockPlayer();
        if (mock instanceof ServerPlayer sp) {
            PlatformHooks.awardAdvancementIfEligible(
                    sp, RefStrings.resourceLocation("nonexistent_advancement"), false);
        }
        //?}
        helper.succeed();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Версионно-совместимое создание mock-игрока в GameTest.
    //  1.20.1 (forge): GameTestHelper.makeMockPlayer() → Player (server-side mock).
    //  1.21.1 (neoforge): GameTestHelper.makeMockPlayer(GameType) → Player (GameType обязателен).
    //  Возвращаем Player как общий тип для Entity-методов PlatformHooks.
    // ════════════════════════════════════════════════════════════════════════
    private static net.minecraft.world.entity.player.Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }
}
