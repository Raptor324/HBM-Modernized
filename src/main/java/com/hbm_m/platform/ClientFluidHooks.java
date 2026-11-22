package com.hbm_m.platform;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентские мосты для рендера жидкостей (IClientFluidTypeExtensions).
 *
 * <p>ТОЛЬКО КЛИЕНТ — вызывается из BER. Тела единые: пакеты
 * {@code IClientFluidTypeExtensions} и {@code FluidStack} переписываются
 * автозаменой {@code !loader_packages} при neoforge-prepare.
 */
@OnlyIn(Dist.CLIENT)
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
        // Вызывается из BER каждый кадр: без FluidStack-аллокации — перегрузка со стеком
        // в обоих лоадерах лишь дефолт-делегирует к безаргументной, а наши жидкости её не переопределяют.
        net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions ext =
                net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        ResourceLocation stillTexture = ext.getStillTexture();
        if (stillTexture == null) return null;
        return net.minecraft.client.Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
    }

    /**
     * Tint жидкости (ARGB) для данного объёма.
     */
    public static int tint(Fluid fluid, int amount) {
        net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions ext =
                net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
        return ext.getTintColor();
    }

    /**
     * Обёртка спрайта в Material атласа блоков (для ретекстура квадов).
     */
    public static Material blockMaterial(ResourceLocation texture) {
        return new Material(InventoryMenu.BLOCK_ATLAS, texture);
    }
}
