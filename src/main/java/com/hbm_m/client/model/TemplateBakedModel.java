package com.hbm_m.client.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Модель предмета, которая показывает результат сборки из шаблона при зажатом Shift.
// Основана на BakedModelWrapper и ItemOverrides.
import com.hbm_m.item.industrial.ItemAssemblyTemplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;

public class TemplateBakedModel extends BakedModelWrapper<BakedModel> {

    private final TemplateItemOverrides itemOverrides;

    public TemplateBakedModel(BakedModel originalModel) {
        super(originalModel);
        this.itemOverrides = new TemplateItemOverrides();
    }

    @NotNull
    @Override
    public ItemOverrides getOverrides() {
        return this.itemOverrides;
    }

    private static class TemplateItemOverrides extends ItemOverrides {
        // Кэш, чтобы каждый раз не запрашивать модель для одного и того же предмета. Критически важно для производительности!
        private final Map<Item, BakedModel> cache = new ConcurrentHashMap<>();

        public TemplateItemOverrides() {
            // 1. Используем пустой конструктор
            super();
        }


        @Override
        @Nullable
        public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (!Screen.hasShiftDown()) {
                return model;
            }

            ItemStack outputStack = ItemAssemblyTemplate.getRecipeOutput(stack);
            if (outputStack.isEmpty()) {
                return model;
            }

            return cache.computeIfAbsent(outputStack.getItem(), item ->
                Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(outputStack)
            );
        }
    }
}