package com.hbm_m.compat.jei;

import java.util.List;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.gui.FluidGuiRendering;

//? if forge {
import net.minecraftforge.fluids.FluidStack;
//?} elif neoforge {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?}
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * JEI ingredient renderer for {@code net.minecraftforge.fluids.FluidStack}.
 * <p>
 * This mod's custom fluids never register {@code IClientFluidTypeExtensions} (still/flowing
 * textures) — a mod-wide gap, not specific to any one machine — so JEI's built-in Forge
 * FluidStack renderer shows a blank icon for them. All of the mod's own GUIs already render
 * fluids through {@link FluidGuiRendering} (backed by {@link HbmFluidRegistry} tint lookups)
 * instead of the vanilla extension mechanism; this renderer reuses that same path for JEI.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class HbmFluidJeiRenderer implements IIngredientRenderer<FluidStack> {

    private final int width;
    private final int height;

    public HbmFluidJeiRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(GuiGraphics guiGraphics, FluidStack ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return;

        Fluid fluid = ingredient.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY) return;

        dev.architectury.fluid.FluidStack fStack = dev.architectury.fluid.FluidStack.create(fluid, ingredient.getAmount());
        ResourceLocation fluidPng = FluidGuiRendering.guiTexturePngForFluid(fluid, fStack);
        if (fluidPng == null) return;

        int tint = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        float r = (tint >> 16 & 255) / 255.0F;
        float g = (tint >> 8 & 255) / 255.0F;
        float b = (tint & 255) / 255.0F;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0F);
        FluidGuiRendering.renderTiledFluid(guiGraphics, fluidPng, 0, 0, width, height);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    @SuppressWarnings("removal")
    public List<Component> getTooltip(FluidStack ingredient, TooltipFlag flag) {
        if (ingredient == null || ingredient.isEmpty()) return List.of();
        Fluid fluid = ingredient.getFluid();
        Component name = dev.architectury.fluid.FluidStack.create(fluid, ingredient.getAmount()).getName();
        return List.of(name, Component.literal(ingredient.getAmount() + " mB"));
    }
}