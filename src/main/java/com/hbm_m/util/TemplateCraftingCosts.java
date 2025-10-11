package com.hbm_m.util;

// Этот класс управляет стоимостью создания шаблонов крафта для различных машин.
// Стоимость определяется в виде списка ингредиентов.

import com.hbm_m.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.Map;

public class TemplateCraftingCosts {

    // Карта: Предмет-результат -> Список ингредиентов для создания его шаблона
    private static final Map<Item, NonNullList<Ingredient>> TEMPLATE_COSTS = new HashMap<>();

    // Карта: Штамп -> Список ингредиентов для его создания (плоский штамп)
    private static final Map<Item, NonNullList<Ingredient>> STAMP_COSTS = new HashMap<>();

    static {
        // СТОИМОСТЬ ДЛЯ ШАБЛОНОВ СБОРОЧНОЙ МАШИНЫ
        // Стоимость: 1 бумага + 1 любой краситель
        registerTemplateCost(ModItems.ASSEMBLY_TEMPLATE.get(),
                NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(Items.PAPER),
                        Ingredient.of(Tags.Items.DYES)
                ));

        // СТОИМОСТЬ ДЛЯ ШТАМПОВ ПРЕССА

        // Каменные штампы - требуют плоский каменный штамп
        NonNullList<Ingredient> stoneFlatCost = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.STAMP_STONE_FLAT.get()));
        registerStampCost(ModItems.STAMP_STONE_PLATE.get(), stoneFlatCost);
        registerStampCost(ModItems.STAMP_STONE_WIRE.get(), stoneFlatCost);
        registerStampCost(ModItems.STAMP_STONE_CIRCUIT.get(), stoneFlatCost);

        // Железные штампы - требуют плоский железный штамп
        NonNullList<Ingredient> ironFlatCost = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.STAMP_IRON_FLAT.get()));
        registerStampCost(ModItems.STAMP_IRON_PLATE.get(), ironFlatCost);
        registerStampCost(ModItems.STAMP_IRON_WIRE.get(), ironFlatCost);
        registerStampCost(ModItems.STAMP_IRON_CIRCUIT.get(), ironFlatCost);
        registerStampCost(ModItems.STAMP_IRON_9.get(), ironFlatCost);
        registerStampCost(ModItems.STAMP_IRON_44.get(), ironFlatCost);
        registerStampCost(ModItems.STAMP_IRON_50.get(), ironFlatCost);
        registerStampCost(ModItems.STAMP_IRON_357.get(), ironFlatCost);

        // Стальные штампы - требуют плоский стальной штамп
        NonNullList<Ingredient> steelFlatCost = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.STAMP_STEEL_FLAT.get()));
        registerStampCost(ModItems.STAMP_STEEL_PLATE.get(), steelFlatCost);
        registerStampCost(ModItems.STAMP_STEEL_WIRE.get(), steelFlatCost);
        registerStampCost(ModItems.STAMP_STEEL_CIRCUIT.get(), steelFlatCost);

        // Титановые штампы - требуют плоский титановый штамп
        NonNullList<Ingredient> titaniumFlatCost = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.STAMP_TITANIUM_FLAT.get()));
        registerStampCost(ModItems.STAMP_TITANIUM_PLATE.get(), titaniumFlatCost);
        registerStampCost(ModItems.STAMP_TITANIUM_WIRE.get(), titaniumFlatCost);
        registerStampCost(ModItems.STAMP_TITANIUM_CIRCUIT.get(), titaniumFlatCost);

        // Обсидиановые штампы - требуют плоский обсидиановый штамп
        NonNullList<Ingredient> obsidianFlatCost = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.STAMP_OBSIDIAN_FLAT.get()));
        registerStampCost(ModItems.STAMP_OBSIDIAN_PLATE.get(), obsidianFlatCost);
        registerStampCost(ModItems.STAMP_OBSIDIAN_WIRE.get(), obsidianFlatCost);
        registerStampCost(ModItems.STAMP_OBSIDIAN_CIRCUIT.get(), obsidianFlatCost);

        // Desh штампы - требуют плоский desh штамп
        NonNullList<Ingredient> deshFlatCost = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.STAMP_DESH_FLAT.get()));
        registerStampCost(ModItems.STAMP_DESH_PLATE.get(), deshFlatCost);
        registerStampCost(ModItems.STAMP_DESH_WIRE.get(), deshFlatCost);
        registerStampCost(ModItems.STAMP_DESH_CIRCUIT.get(), deshFlatCost);
        registerStampCost(ModItems.STAMP_DESH_9.get(), deshFlatCost);
        registerStampCost(ModItems.STAMP_DESH_44.get(), deshFlatCost);
        registerStampCost(ModItems.STAMP_DESH_50.get(), deshFlatCost);
        registerStampCost(ModItems.STAMP_DESH_357.get(), deshFlatCost);

        // Для других типов шаблонов (идентификаторы, треки сирены и т.д.)
        // можно добавить дополнительные записи здесь

        // Пример для идентификаторов:
        // registerTemplateCost(ModItems.IDENTIFIER_TEMPLATE.get(),
        //              NonNullList.of(Ingredient.EMPTY,
        //                             Ingredient.of(ModItems.IRON_PLATE.get()),
        //                             Ingredient.of(Tags.Items.DYES)
        //              ));
    }

    /**
     * Регистрация стоимости для шаблонов машин
     */
    private static void registerTemplateCost(Item templateType, NonNullList<Ingredient> ingredients) {
        TEMPLATE_COSTS.put(templateType, ingredients);
    }

    /**
     * Регистрация стоимости для штампов пресса
     */
    private static void registerStampCost(Item stampType, NonNullList<Ingredient> ingredients) {
        STAMP_COSTS.put(stampType, ingredients);
    }

    /**
     * Получает стоимость создания шаблона для конкретного результата рецепта.
     * Используется для обычных шаблонов сборочной машины.
     * @param recipeOutput Результат рецепта (например, Алмазный меч).
     * @return Список ингредиентов или null, если стоимость не определена.
     */
    public static NonNullList<Ingredient> getCostForTemplate(ItemStack recipeOutput) {
        // Для всех рецептов сборочной машины используем стоимость ASSEMBLY_TEMPLATE
        return TEMPLATE_COSTS.get(ModItems.ASSEMBLY_TEMPLATE.get());
    }

    /**
     * Получает стоимость создания конкретного штампа.
     * @param stamp ItemStack штампа.
     * @return Список ингредиентов или null, если стоимость не определена.
     */
    public static NonNullList<Ingredient> getCostForStamp(ItemStack stamp) {
        return STAMP_COSTS.get(stamp.getItem());
    }

    /**
     * Проверяет, является ли предмет штампом пресса.
     * @param stack ItemStack для проверки.
     * @return true, если это штамп пресса.
     */
    public static boolean isStamp(ItemStack stack) {
        return STAMP_COSTS.containsKey(stack.getItem());
    }
}