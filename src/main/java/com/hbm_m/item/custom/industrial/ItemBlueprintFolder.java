package com.hbm_m.item.custom.industrial;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.hbm_m.recipe.AssemblerRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

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
    public Component getName(@Nonnull ItemStack stack) {
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
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level,
                            @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        String pool = getBlueprintPool(stack);
        
        // Если NBT пустой — показываем "пустой шаблон"
        if (pool.isEmpty()) {
            tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.empty")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        
        tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.desc")
            .withStyle(ChatFormatting.GRAY));
        
        // Проверяем, есть ли рецепты в этой группе
        if (level != null && level.getRecipeManager() != null) {
            List<AssemblerRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE)
                .stream()
                .filter(r -> pool.equals(r.getBlueprintPool()))
                .toList();
            
            // ИСПРАВЛЕНО: Если рецептов нет — показываем "пустой шаблон"
            if (recipes.isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.obsolete")
                    .withStyle(ChatFormatting.RED));
                return;
            }
            
            // Рецепты есть — показываем их список
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.hbm_m.blueprint_folder.recipes")
                .withStyle(ChatFormatting.GOLD));
            
            int count = 0;
            for (AssemblerRecipe recipe : recipes) {
                if (count >= 10) {
                    tooltip.add(Component.literal(" ...")
                        .withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
                
                tooltip.add(Component.literal(" • ")
                    .append(recipe.getResultItem(level.registryAccess()).getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
                count++;
            }
        }
    }
}
