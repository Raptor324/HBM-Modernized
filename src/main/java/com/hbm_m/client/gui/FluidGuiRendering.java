package com.hbm_m.client.gui;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/**
 * Утилиты для отрисовки жидкости в GUI так же, как в 1.7.10 {@code FluidType} /
 * {@code FluidTank.renderTank}: отдельный PNG под {@code textures/gui/fluids/}, тайлинг 16×16.
 *
 * <p>В оригинале путь задаётся как
 * {@code new ResourceLocation(RefStrings.MODID + ":textures/gui/fluids/" + name.toLowerCase(Locale.US) + ".png");}
 * либо для кастомных — {@code texName} без изменения регистра
 * ({@code RefStrings.MODID + ":textures/gui/fluids/" + texName + ".png"}).
 *
 * <p>Для Forge 1.20 {@link IClientFluidTypeExtensions#getStillTexture(FluidStack)} возвращает id вида
 * {@code hbm_m:gui/fluids/water}; соответствующий файл — {@code assets/hbm_m/textures/gui/fluids/water.png}.
 * Используйте {@link #texturePngFromStillSpriteId(ResourceLocation)} или {@link #guiTexturePngForStack(FluidStack)}.
 */
public final class FluidGuiRendering {

    /** Как в 1.7.10: одна ячейка иконки для тайлинга в баке. */
    public static final int FLUID_GUI_TILE_SIZE = 16;

    private FluidGuiRendering() {}

    /**
     * Эквивалент конструктора {@code FluidType(String name, ...)}:
     * {@code MODID:textures/gui/fluids/}{@code fluidTextureBaseName.toLowerCase(Locale.US)}{@code .png}.
     */
    public static ResourceLocation hbmGuiFluidPng(String fluidTextureBaseName) {
        String safe = fluidTextureBaseName == null ? "" : fluidTextureBaseName.toLowerCase(Locale.US);
        return ResourceLocation.fromNamespaceAndPath(
                MainRegistry.MOD_ID,
                "textures/gui/fluids/" + safe + ".png");
    }

    /**
     * Эквивалент {@code FluidType.setupCustom(..., String texName, ...)}:
     * {@code MODID:textures/gui/fluids/}{@code textureFileBaseName}{@code .png} без изменения регистра.
     */
    public static ResourceLocation hbmGuiFluidPngCustom(String textureFileBaseName) {
        String name = textureFileBaseName == null ? "" : textureFileBaseName;
        return ResourceLocation.fromNamespaceAndPath(
                MainRegistry.MOD_ID,
                "textures/gui/fluids/" + name + ".png");
    }

    /**
     * Id стоячей текстуры Forge ({@code namespace:path} без {@code textures/} и без {@code .png})
     * → полный {@link ResourceLocation} PNG для {@link GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int, int, int)}.
     */
    public static ResourceLocation texturePngFromStillSpriteId(ResourceLocation stillId) {
        return ResourceLocation.fromNamespaceAndPath(
                stillId.getNamespace(),
                "textures/" + stillId.getPath() + ".png");
    }

    @Nullable
    public static ResourceLocation guiTexturePngForStack(FluidStack stack) {
        if (stack.isEmpty()) return null;
        return guiTexturePngForFluid(stack.getFluid(), stack);
    }

    @Nullable
    public static ResourceLocation guiTexturePngForFluid(Fluid fluid, FluidStack stackForStillTexture) {
        ResourceLocation still = IClientFluidTypeExtensions.of(fluid).getStillTexture(stackForStillTexture);
        return still == null ? null : texturePngFromStillSpriteId(still);
    }

    /**
     * Тайлинг снизу вверх (заполнение снизу) и слева направо, как {@code FluidTank.renderTank} в 1.7.10.
     * Цвет и blend выставляет вызывающий (например {@link com.mojang.blaze3d.systems.RenderSystem#setShaderColor}).
     */
    public static void renderTiledFluid(GuiGraphics guiGraphics, ResourceLocation fluidPng, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        int tex = FLUID_GUI_TILE_SIZE;

        for (int drawX = 0; drawX < width; drawX += tex) {
            int currentWidth = Math.min(tex, width - drawX);

            for (int drawY = 0; drawY < height; drawY += tex) {
                int currentHeight = Math.min(tex, height - drawY);
                int yOffset = height - drawY - currentHeight;
                int quadX = x + drawX;
                int quadY = y + yOffset;

                int texU = 0;
                int texV = (currentHeight == tex) ? 0 : (tex - currentHeight);

                guiGraphics.blit(fluidPng, quadX, quadY, texU, texV, currentWidth, currentHeight, tex, tex);
            }
        }
    }
}
