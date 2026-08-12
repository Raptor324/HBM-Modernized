package com.hbm_m.item.industrial;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.TemplateTooltipUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

// Предмет-шаблон для крафтов в сборочной машине.
// Хранит в себе NBT с рецептом сборки (выходной предмет).

public class ItemAssemblyTemplate extends Item implements ITooltipProvider {
    public ItemAssemblyTemplate(Properties pProperties) {
        super(pProperties);
    }

    // Логика NBT для хранения рецепта
    public static void writeRecipeOutput(ItemStack templateStack, ItemStack outputStack) {
        if (templateStack.getItem() instanceof ItemAssemblyTemplate) {
            CompoundTag outputNbt = new CompoundTag();
            //? if < 1.21.1 {
            outputStack.save(outputNbt);
            //?} else {
            /*// Item-метод без Level в области видимости: провайдер берётся из клиентского Level
            // (call-site'ы — tooltip/render/getName, все клиентские). saveItemStack совместим с null-провайдером.
            outputNbt = PlatformHooks.saveItemStack(outputStack, outputNbt, PlatformHooks.clientProvider());
            *///?}
            final CompoundTag finalOutputNbt = outputNbt;
            PlatformHooks.editItemTag(templateStack, nbt -> nbt.put("recipeOutput", finalOutputNbt));
        }
    }

    public static ItemStack getRecipeOutput(ItemStack templateStack) {
        if (PlatformHooks.hasItemTag(templateStack) && PlatformHooks.getItemTag(templateStack).contains("recipeOutput")) {
            CompoundTag outputNbt = PlatformHooks.getItemTag(templateStack).getCompound("recipeOutput");
            //? if < 1.21.1 {
            return ItemStack.of(outputNbt);
            //?} else {
            /*// Без Level провайдер из клиентского Level; если недоступен — EMPTY (template treated as no-output).
            net.minecraft.core.HolderLookup.Provider provider = PlatformHooks.clientProvider();
            return provider != null ? PlatformHooks.itemStackOf(outputNbt, provider) : ItemStack.EMPTY;
            *///?}
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
    public void appendHbmTooltip(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        TemplateTooltipUtil.buildTemplateTooltip(pStack, pLevel, pTooltipComponents);
    }
}
