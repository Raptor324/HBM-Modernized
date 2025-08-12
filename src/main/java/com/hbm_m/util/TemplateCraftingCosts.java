package com.hbm_m.util;

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
    private static final Map<Item, NonNullList<Ingredient>> COSTS = new HashMap<>();

    static {
        // ЗАПОЛНЯЕМ СТОИМОСТЬ
        
        // Для обычных рецептов в сборщике (AssemblerRecipe)
        // Стоимость: 1 бумага + 1 любой краситель
        registerCost(ModItems.ASSEMBLY_TEMPLATE.get(), // <-- Это заглушка, см. ниже
                     NonNullList.of(Ingredient.EMPTY,
                                    Ingredient.of(Items.PAPER),
                                    Ingredient.of(Tags.Items.DYES)
                     ));
                     
        // Для идентификаторов
        // Стоимость: 1 железная пластина + 1 любой краситель
        // ЗАМЕНИТЕ ModItems.IRON_PLATE.get() НА ВАШ ПРЕДМЕТ ЖЕЛЕЗНОЙ ПЛАСТИНЫ
        // registerCost(ModItems.IDENTIFIER_TEMPLATE.get(), 
        //              NonNullList.of(Ingredient.EMPTY,
        //                             Ingredient.of(ModItems.IRON_PLATE.get()), 
        //                             Ingredient.of(Tags.Items.DYES)
        //              ));

        // Штамп для пресса и Трек сирены:
        // Мы просто НЕ добавляем их в карту. Логика в пакетном обработчике
        // не найдет для них стоимость и не даст их создать. Это и есть наша "заглушка".
    }

    /**
     * Основной метод регистрации. Он принимает тип шаблона (например, Assembly Template)
     * и его стоимость. Но нам нужно знать стоимость для КАЖДОГО конкретного рецепта.
     * Поэтому мы регистрируем стоимость для "базового" типа шаблона,
     * а затем будем применять её для всех рецептов этого типа.
     */
    private static void registerCost(Item templateType, NonNullList<Ingredient> ingredients) {
        COSTS.put(templateType, ingredients);
    }
    
    /**
     * Получает стоимость создания шаблона для конкретного результата крафта.
     * @param recipeOutput Результат рецепта (например, Алмазный меч).
     * @return Список ингредиентов или null, если стоимость не определена.
     */
    public static NonNullList<Ingredient> getCostFor(ItemStack recipeOutput) {
        // ВАЖНАЯ ЛОГИКА:
        // Здесь мы должны определить, какой тип шаблона нужен для этого предмета.
        // Сейчас у нас только один тип - AssemblerRecipe, поэтому мы всегда
        // возвращаем стоимость для него.
        // Если у вас появятся другие машины, здесь нужно будет добавить логику
        // для определения правильного типа шаблона.

        return COSTS.get(ModItems.ASSEMBLY_TEMPLATE.get());
    }
}