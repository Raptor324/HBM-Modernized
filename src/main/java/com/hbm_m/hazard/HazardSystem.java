package com.hbm_m.hazard;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Централизованная система для определения опасностей, исходящих от предметов.
 * Позволяет регистрировать правила для Тегов и конкретных Предметов.
 * Система работает по принципу приоритетов: Теги < Предметы.
 * Это статичный класс-утилита, его не нужно создавать.
 */

public final class HazardSystem {

    // Приватный конструктор, чтобы никто не мог создать экземпляр этого класса.
    private HazardSystem() {}

    // ВНУТРЕННИЕ ХРАНИЛИЩА ПРАВИЛ 
    // Они приватные, чтобы гарантировать целостность данных. Все взаимодействие идет через публичные методы.

    // Правила для тегов (самый низкий приоритет)
    private static final Map<TagKey<Item>, HazardData> TAG_RULES = new ConcurrentHashMap<>();
    // Правила для конкретных предметов (самый высокий приоритет)
    private static final Map<Item, HazardData> ITEM_RULES = new ConcurrentHashMap<>();
    // Хранилище защиты от радиации для брони
    private static final Map<Item, Float> ARMOR_PROTECTION_RULES = new ConcurrentHashMap<>();
    // Кэш для уже вычисленных результатов. Ключ - Item, Значение - финальный список опасностей.
    private static final Map<Item, List<HazardEntry>> HAZARD_CACHE = new ConcurrentHashMap<>();

    // ПУБЛИЧНЫЕ МЕТОДЫ РЕГИСТРАЦИИ 

    /**
     * Регистрирует правило опасности для всех предметов с указанным тегом.
     * @param tag  Ключ тега (например, TagKeys.create(Registries.ITEM, new ResourceLocation("forge", "ingots/uranium")))
     * @param data Данные об опасности.
     */
    public static void register(TagKey<Item> tag, HazardData data) {
        TAG_RULES.put(tag, data);
    }

    /**
     * Регистрирует правило опасности для конкретного предмета.
     * Это правило имеет более высокий приоритет, чем правила для тегов.
     * @param item Предмет (например, ModItems.URANIUM_INGOT.get())
     * @param data Данные об опасности.
     */

    public static void register(Item item, HazardData data) {
        ITEM_RULES.put(item, data);
    }

    /**
     * Удобный метод-обертка для регистрации опасности для блока.
     * @param block Блок (например, ModBlocks.URANIUM_BLOCK.get())
     * @param data  Данные об опасности.
     */

    public static void register(Block block, HazardData data) {
        register(block.asItem(), data);
    }

    /**
     * Регистрирует абсолютное значение защиты от радиации для предмета брони.
     */
    public static void registerArmorProtection(Item armorItem, float absoluteProtection) {
        ARMOR_PROTECTION_RULES.put(armorItem, absoluteProtection);
    }


    // ПУБЛИЧНЫЕ МЕТОДЫ ПОЛУЧЕНИЯ ДАННЫХ 

    /**
     * Главный метод. Возвращает итоговый список опасностей для указанного ItemStack,
     * применяя все правила приоритетов и переопределений.
     *
     * @param stack ItemStack для проверки.
     * @return Финальный список HazardEntry.
     */
    public static List<HazardEntry> getHazardsFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }

        Item item = stack.getItem();

        // 1. Сначала проверяем кэш
        if (HAZARD_CACHE.containsKey(item)) {
            return HAZARD_CACHE.get(item);
        }

        // Если в кэше нет, выполняем полную логику (ваш существующий код) 
        List<HazardData> applicableData = new ArrayList<>();
        stack.getTags().forEach(tag -> {
            if (TAG_RULES.containsKey(tag)) {
                applicableData.add(TAG_RULES.get(tag));
            }
        });

        if (ITEM_RULES.containsKey(item)) {
            applicableData.add(ITEM_RULES.get(item));
        }

        if (applicableData.isEmpty()) {
            HAZARD_CACHE.put(item, Collections.emptyList()); // Кэшируем и пустой результат
            return Collections.emptyList();
        }

        // 2. "Сворачиваем" правила в итоговый список опасностей
        List<HazardEntry> finalEntries = new ArrayList<>();
        int mutex = 0;

        for (HazardData data : applicableData) {
            // Если правило помечено как "переопределяющее", очищаем все, что было найдено до него.
            if (data.doesOverride) {
                finalEntries.clear();
                mutex = 0; // Сбрасываем и мьютекс
            }

            // Проверяем мьютекс (пока не используется, но логика готова)
            if ((data.getMutex() & mutex) == 0) {
                finalEntries.addAll(data.entries);
                mutex |= data.getMutex();
            }
        }
        HAZARD_CACHE.put(item, finalEntries);
        return finalEntries;
    }

    /**
     * Удобный метод для получения числового значения конкретной опасности для предмета.
     *
     * @param stack ItemStack для проверки.
     * @param type  Тип искомой опасности.
     * @return Уровень опасности или 0.0f, если не найдено.
     */
    
     public static float getHazardLevelFromStack(ItemStack stack, HazardType type) {
        // Получаем список опасностей и дополнительно защищаемся от возможного null
        List<HazardEntry> entries = getHazardsFromStack(stack);
        if (entries == null || entries.isEmpty()) {
            return 0.0f;
        }
    
        // Обычный цикл без stream, чуть быстрее и без лишных объектов
        for (HazardEntry entry : entries) {
            if (entry.type == type) {
                // В будущем здесь можно будет применить модификаторы
                return entry.baseLevel;
            }
        }
    
        return 0.0f;
    }

    public static float getHazardLevelFromState(BlockState state, HazardType type) {
        // Блоки без предмета (как огонь) не могут иметь опасности в этой системе
        if (state.isAir() || state.getBlock().asItem() == Items.AIR) {
            return 0.0f;
        }
        // Создаем временный пустой стак, чтобы передать его в основной метод.
        // С кэшированием это будет быстро.
        return getHazardLevelFromStack(new ItemStack(state.getBlock()), type);
    }

    /**
     * Получает абсолютное значение защиты от радиации для стака брони.
     */
    public static float getArmorProtection(ItemStack armorStack) {
        if (armorStack.isEmpty()) return 0.0f;
        return ARMOR_PROTECTION_RULES.getOrDefault(armorStack.getItem(), 0.0f);
    }
}