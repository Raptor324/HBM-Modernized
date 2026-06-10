package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.hbm_m.item.industrial.ItemAssemblyTemplate;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

/**
 * Shift-preview шаблона сборки подменяет {@link net.minecraft.client.resources.model.BakedModel} на выход рецепта,
 * но {@link ItemStack} остаётся шаблоном. BEWLR (ракеты: {@link com.hbm_m.client.render.item.ItemRenderMissileGeneric})
 * берётся из {@code IClientItemExtensions.of(stack)} — без подмены стека рендер пустой или неверный.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRendererTemplatePreview {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack hbm_m$swapAssemblyTemplatePreviewStack(ItemStack stack) {
        if (!Screen.hasShiftDown()) {
            return stack;
        }
        if (!(stack.getItem() instanceof ItemAssemblyTemplate)) {
            return stack;
        }
        ItemStack output = ItemAssemblyTemplate.getRecipeOutput(stack);
        return output.isEmpty() ? stack : output;
    }
}
