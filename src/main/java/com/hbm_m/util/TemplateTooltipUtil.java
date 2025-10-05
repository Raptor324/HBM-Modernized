package com.hbm_m.util;

// Утилитарный класс для построения тултипов предметов-шаблонов.
// Извлекает информацию о рецепте из шаблона и форматирует её для отображения в тултипе.
// Используется в ItemAssemblyTemplate для добавления информации в тултип.

import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.recipe.AssemblerRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TemplateTooltipUtil {

    /**
     * Строит и добавляет форматированный тултип для предметов-шаблонов.
     * Включает строку "Created via Template Folder".
     * @param stack Предмет-шаблон, для которого строится тултип.
     * @param level Мир, в котором находится предмет.
     * @param tooltip Список компонентов, в который будет добавлен тултип.
     */
    public static void buildTemplateTooltip(ItemStack stack, Level level, List<Component> tooltip) {
        if (level == null) return;
        
        ItemStack output = ItemAssemblyTemplate.getRecipeOutput(stack);
        if (output.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.hbm_m.template_broken").withStyle(ChatFormatting.RED));
            return;
        }
        
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<AssemblerRecipe> recipeOpt = recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE)
            .stream()
            .filter(r -> ItemStack.isSameItemSameTags(r.getResultItem(null), output))
            .findFirst();
        
        if (recipeOpt.isEmpty()) {
            return;
        }
        
        AssemblerRecipe recipe = recipeOpt.get();
        
        // "Created via Template Folder" - ТОЛЬКО ДЛЯ ШАБЛОНОВ
        tooltip.add(Component.translatable("tooltip.hbm_m.created_with_template_folder").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.empty());
        
        // Добавляем детали рецепта
        addRecipeDetails(recipe, output, tooltip);
    }
    
    /**
     * Строит и добавляет форматированный тултип для рецептов (БЕЗ строки о папке шаблонов).
     * Используется в GUI выбора рецептов.
     * @param recipe Рецепт для отображения.
     * @param tooltip Список компонентов, в который будет добавлен тултип.
     */
    public static void buildRecipeTooltip(AssemblerRecipe recipe, List<Component> tooltip) {
        if (recipe == null) return;
        
        ItemStack output = recipe.getResultItem(null);
        if (output.isEmpty()) return;
        
        // БЕЗ строки "Created via Template Folder"
        addRecipeDetails(recipe, output, tooltip);
    }
    
    /**
     * Внутренний метод для добавления деталей рецепта в тултип.
     * Общая логика для шаблонов и рецептов.
     */
    private static void addRecipeDetails(AssemblerRecipe recipe, ItemStack output, List<Component> tooltip) {
        // Выход
        tooltip.add(Component.translatable("tooltip.hbm_m.output").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  " + output.getCount() + "x ").withStyle(ChatFormatting.WHITE)
            .append(output.getHoverName()));
        
        // Вход
        tooltip.add(Component.translatable("tooltip.hbm_m.input").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Map<Item, Integer> ingredientMap = new HashMap<>();
        
        for (Ingredient ing : ingredients) {
            if (ing.getItems().length > 0) {
                Item item = ing.getItems()[0].getItem();
                int count = ing.getItems()[0].getCount(); // Получаем количество из стака
                ingredientMap.merge(item, count, Integer::sum);
            }
        }
        
        for (Map.Entry<Item, Integer> entry : ingredientMap.entrySet()) {
            ItemStack ingredientStack = new ItemStack(entry.getKey());
            tooltip.add(Component.literal("  " + entry.getValue() + "x ").withStyle(ChatFormatting.WHITE)
                .append(ingredientStack.getHoverName()));
        }
        
        // Время производства
        tooltip.add(Component.translatable("tooltip.hbm_m.production_time").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        int timeInTicks = recipe.getDuration();
        float timeInSeconds = timeInTicks / 20.0f;
        String formattedTime = String.format("%.1f", timeInSeconds);
        tooltip.add(Component.literal("  " + formattedTime + " ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.translatable("tooltip.hbm_m.seconds")));
        
        // Потребление энергии (если есть метод getEnergyCost())
        // TODO: Раскомментируйте, когда у AssemblerRecipe будет метод getEnergyCost()
        /*
        tooltip.add(Component.translatable("tooltip.hbm_m.energy_cost").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  " + recipe.getEnergyCost() + " FE")
            .withStyle(ChatFormatting.WHITE));
        */
    }
}