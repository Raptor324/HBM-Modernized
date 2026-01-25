package com.hbm_m.item.custom.industrial;

// Предмет-шаблон для крафтов в сборочной машине.
// Хранит в себе NBT с рецептом сборки (выходной предмет).

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.util.TemplateTooltipUtil;

import java.util.List;

import javax.annotation.Nonnull;

public class ItemAssemblyTemplate extends Item {
    public ItemAssemblyTemplate(Properties pProperties) {
        super(pProperties);
    }

    // Логика NBT для хранения рецепта 
    public static void writeRecipeOutput(ItemStack templateStack, ItemStack outputStack) {
        if (templateStack.getItem() instanceof ItemAssemblyTemplate) {
            CompoundTag nbt = templateStack.getOrCreateTag();
            CompoundTag outputNbt = new CompoundTag();
            outputStack.save(outputNbt);
            nbt.put("recipeOutput", outputNbt);
        }
    }

    public static ItemStack getRecipeOutput(ItemStack templateStack) {
        if (templateStack.hasTag() && templateStack.getTag().contains("recipeOutput")) {
            CompoundTag outputNbt = templateStack.getTag().getCompound("recipeOutput");
            return ItemStack.of(outputNbt);
        }
        return ItemStack.EMPTY;
    }
    
    // Кастомное название и тултип 
   @Override
    public Component getName(@Nonnull ItemStack pStack) {
        ItemStack output = getRecipeOutput(pStack);
        if (!output.isEmpty()) {
            // Используем ключ локализации и передаем имя предмета как аргумент
            return Component.translatable("item.hbm_m.assembly_template", output.getHoverName());
        }
        // Возвращаем стандартное имя, если шаблон пуст
        return Component.translatable(this.getDescriptionId(pStack));
    }

    // @Override
    // public Optional<TooltipComponent> getTooltipImage(ItemStack pStack) {
    //     // Если шифт НЕ нажат, показываем иконку содержимого
    //     if (!Screen.hasShiftDown()) {
    //         ItemStack output = getRecipeOutput(pStack);
    //         if (!output.isEmpty()) {
    //             // Возвращаем наш кастомный компонент
    //             return Optional.of(new ItemTooltipComponent(output));
    //         }
    //     }
    //     return Optional.empty();
    // }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {

        TemplateTooltipUtil.buildTemplateTooltip(pStack, pLevel, pTooltipComponents);
    }
}