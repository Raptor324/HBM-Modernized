package com.hbm_m.platform;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

/**
 * Клиентские мосты для рендера жидкостей (IClientFluidTypeExtensions).
 *
 * <p>ТОЛЬКО КЛИЕНТ — вызывается из BER. Тела forge/neoforge идентичны
 * (отличаются только пакеты {@code IClientFluidTypeExtensions} и {@code FluidStack}),
 * поэтому весь loader-gating собран здесь, а рендереры остаются чистыми.
 */
public final class ClientFluidHooks {

    private ClientFluidHooks() {
    }

    /**
     * Спрайт "still" текстуры жидкости из атласа блоков.
     *
     * @param fluid  жидкость
     * @param amount количество (мб) — влияет только на выбор still/flowing-логики лоадера
     * @return спрайт или null, если у жидкости нет still-текстуры
     */
    @org.jetbrains.annotations.Nullable
    public static TextureAtlasSprite stillSprite(Fluid fluid, int amount) {
        //? if forge {
        net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions ext =
                net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        net.minecraftforge.fluids.FluidStack stack = new net.minecraftforge.fluids.FluidStack(fluid, amount);
        ResourceLocation stillTexture = ext.getStillTexture(stack);
        if (stillTexture == null) return null;
        return net.minecraft.client.Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        //?} elif neoforge {
        /*net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions ext =
                net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        net.neoforged.neoforge.fluids.FluidStack stack = new net.neoforged.neoforge.fluids.FluidStack(fluid, amount);
        ResourceLocation stillTexture = ext.getStillTexture(stack);
        if (stillTexture == null) return null;
        return net.minecraft.client.Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        *///?} else {
        /*return null;
        *///?}
    }

    /**
     * Tint жидкости (ARGB) для данного объёма.
     */
    public static int tint(Fluid fluid, int amount) {
        //? if forge {
        net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions ext =
                net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        net.minecraftforge.fluids.FluidStack stack = new net.minecraftforge.fluids.FluidStack(fluid, amount);
        return ext.getTintColor(stack);
        //?} elif neoforge {
        /*net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions ext =
                net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        net.neoforged.neoforge.fluids.FluidStack stack = new net.neoforged.neoforge.fluids.FluidStack(fluid, amount);
        return ext.getTintColor(stack);
        *///?} else {
        /*return 0xFFFFFFFF;
        *///?}
    }

    /**
     * Обёртка спрайта в Material атласа блоков (для ретекстура квадов).
     */
    public static Material blockMaterial(ResourceLocation texture) {
        return new Material(InventoryMenu.BLOCK_ATLAS, texture);
    }
}
