package com.hbm_m.util;

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
     * @param stack Предмет-шаблон, для которого строится тултип.
     * @param level Мир, в котором находится предмет.
     * @param tooltip Список компонентов, в который будет добавлен тултип.
     */
    
    public static void buildTemplateTooltip(ItemStack stack, Level level, List<Component> tooltip) {
        if (level == null) return;

        // Логика получения рецепта остаётся той же
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
            // Можно добавить сообщение, если рецепт не найден
            return;
        }

        AssemblerRecipe recipe = recipeOpt.get();

        // "Created via Template Folder"
        tooltip.add(Component.translatable("tooltip.hbm_m.created_with_template_folder").withStyle(ChatFormatting.YELLOW));

        // Отступ в одну строку
        tooltip.add(Component.empty());

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
                // Используем getCount() из самого Ingredient, если он поддерживает количество
                // Если нет, то считаем по одному, как и раньше.
                // Для простоты оставим старую логику, но можно расширить.
                ingredientMap.merge(item, 1, Integer::sum);
            }
        }
        
        for (Map.Entry<Item, Integer> entry : ingredientMap.entrySet()) {
            ItemStack ingredientStack = new ItemStack(entry.getKey());
            tooltip.add(Component.literal("  " + entry.getValue() + "x ").withStyle(ChatFormatting.WHITE)
                    .append(ingredientStack.getHoverName()));
        }

        // Время 
        tooltip.add(Component.translatable("tooltip.hbm_m.production_time").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));

        // 1. Получаем время из рецепта. ПРЕДПОЛАГАЕМ, ЧТО ОНО В ТИКАХ.
        // Делим на 20, чтобы получить секунды. Используем 20.0f для деления с плавающей точкой.
        int timeInTicks = recipe.getDuration();

        // 2. Делим на 20.0f, чтобы получить float-секунды.
        // Этот код будет работать и с int, и с float, но теперь он логически верен.
        float timeInSeconds = timeInTicks / 20.0f; 

        String formattedTime = String.format("%.1f", timeInSeconds);
        tooltip.add(Component.literal("  " + formattedTime + " ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.translatable("tooltip.hbm_m.seconds"))); // Добавляем единицу измерения
    }
}