package com.hbm_m.item.industrial;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.util.TemplateTooltipUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Предмет-шаблон для крафтов в сборочной машине.
// Хранит в себе NBT с рецептом сборки (выходной предмет).

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
    public Component getName(@NotNull ItemStack pStack) {
        ItemStack output = getRecipeOutput(pStack);
        if (!output.isEmpty()) {
            return Component.translatable("item.hbm_m.assembly_template", output.getHoverName());
        }
        return Component.translatable(this.getDescriptionId(pStack));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        TemplateTooltipUtil.buildTemplateTooltip(pStack, pLevel, pTooltipComponents);
    }
}
