package com.hbm_m.util;

// Утилитарный класс для построения тултипов предметов-шаблонов.
// Извлекает информацию о рецепте из шаблона и форматирует её для отображения в тултипе.
// Используется в ItemAssemblyTemplate для добавления информации в тултип.

import com.hbm_m.item.custom.industrial.ItemAssemblyTemplate;
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
        tooltip.add(Component.translatable("tooltip.hbm_m.created_with_template_folder").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.empty());
        addRecipeDetails(recipe, output, tooltip);
    }

    public static void buildRecipeTooltip(AssemblerRecipe recipe, List<Component> tooltip) {
        if (recipe == null) return;
        ItemStack output = recipe.getResultItem(null);
        if (output.isEmpty()) return;
        addRecipeDetails(recipe, output, tooltip);
    }

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
                int count = ing.getItems()[0].getCount();
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
        
        // НОВОЕ: Потребление энергии
        tooltip.add(Component.translatable("tooltip.hbm_m.energy_consumption").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        int powerConsumption = recipe.getPowerConsumption();
        tooltip.add(Component.literal("  " + formatEnergy(powerConsumption) + " HE/t")
                .withStyle(ChatFormatting.YELLOW));
    }
    
    /**
     * Форматирует энергию для удобного чтения (1000 -> 1K, 1000000 -> 1M)
     */
    private static String formatEnergy(int energy) {
        if (energy >= 1_000_000) {
            return String.format("%.1fM", energy / 1_000_000.0);
        } else if (energy >= 1_000) {
            return String.format("%.1fK", energy / 1_000.0);
        } else {
            return String.valueOf(energy);
        }
    }
}