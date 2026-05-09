package com.hbm_m.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor к списку слоёв рендера.
 *
 * Почему не @Invoker(addLayer):
 * в некоторых маппингах/окружениях Mixin AP не может однозначно найти target метод.
 * Поле списка слоёв обычно стабильно и позволяет безопасно добавить слой напрямую.
 */

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {
    @Accessor("layers")
    java.util.List<RenderLayer<?, ?>> hbm_m$getLayers();
}