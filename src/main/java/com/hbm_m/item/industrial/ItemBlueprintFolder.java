package com.hbm_m.item.industrial;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemBlueprintFolder extends Item {

    public ItemBlueprintFolder(Properties properties) {
        super(properties);
    }

    // Записать пул в NBT
    public static void writeBlueprintPool(ItemStack folderStack, String poolName) {
        if (folderStack.getItem() instanceof ItemBlueprintFolder) {
            CompoundTag nbt = folderStack.getOrCreateTag();
            nbt.putString("blueprintPool", poolName);
        }
    }

    // Получить пул из NBT
    public static String getBlueprintPool(ItemStack folderStack) {
        if (folderStack.hasTag() && folderStack.getTag().contains("blueprintPool")) {
            return folderStack.getTag().getString("blueprintPool");
        }
        return "";
    }

    @Override
    public Component getName(@NotNull ItemStack stack) {
        String pool = getBlueprintPool(stack);
        if (!pool.isEmpty()) {
            // Формат: "Папка шаблонов машин: <название группы>"
            return Component.translatable("item.hbm_m.blueprint_folder.named")
                    .append(": ")
                    .append(pool);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        String pool = getBlueprintPool(stack);
        
        // Если NBT пустой - показываем "пустой шаблон"
        if (pool.isEmpty()) {
            tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.empty")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        
        tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.desc")
            .withStyle(ChatFormatting.GRAY));
        
        // Проверяем, есть ли рецепты в этой группе (сборщик + химзавод используют один NBT pool)
        if (level != null && level.getRecipeManager() != null) {
            List<AssemblerRecipe> assemblerRecipes = level.getRecipeManager()
                .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE)
                .stream()
                .filter(r -> pool.equals(r.getBlueprintPool()))
                .toList();
            List<ChemicalPlantRecipe> chemicalRecipes = level.getRecipeManager()
                .getAllRecipesFor(ChemicalPlantRecipe.Type.INSTANCE)
                .stream()
                .filter(r -> pool.equals(r.getBlueprintPool()))
                .toList();

            if (assemblerRecipes.isEmpty() && chemicalRecipes.isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.obsolete")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.recipes")
                .withStyle(ChatFormatting.GOLD));

            int count = 0;
            for (AssemblerRecipe recipe : assemblerRecipes) {
                if (count >= 10) {
                    tooltip.add(Component.literal(" ...")
                        .withStyle(ChatFormatting.DARK_GRAY));
                    return;
                }
                tooltip.add(Component.literal(" • ")
                    .append(recipe.getResultItem(level.registryAccess()).getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
                count++;
            }
            for (ChemicalPlantRecipe recipe : chemicalRecipes) {
                if (count >= 10) {
                    tooltip.add(Component.literal(" ...")
                        .withStyle(ChatFormatting.DARK_GRAY));
                    return;
                }
                tooltip.add(Component.literal(" • ")
                    .append(recipe.getResultItem(level.registryAccess()).getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
                count++;
            }
        }
    }
}
