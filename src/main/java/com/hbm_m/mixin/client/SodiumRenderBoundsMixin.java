package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Централизованный render-bounds кэш на call-site Embeddium/Rubidium: BE-куллер
 * вызывает {@code getRenderBoundingBox} для каждого BE каждый кадр, а мультиблочные
 * override'ы каждый раз проходят по structure map (профиль AwszOJcOkb: 8.8% рендер-
 * потока). Редирект направляет вызов через
 * {@link com.hbm_m.client.render.RenderBoundsCache} - один кэш на всех вызывающих
 * (и Nucleus collect, и свет), ни один BE-класс не затронут.
 * <p>
 * Мягкая зависимость, паттерн {@link MixinIrisShadowRenderer}: {@code @Pseudo} +
 * строковый target + {@code require = 0} - без форка Sodium (или с другой
 * сигнатурой вызова) миксин молча не применяется. {@code remap = false}:
 * Sodium не обфусцирован; target-дескриптор - forge-интерфейс
 * {@code IForgeBlockEntity#getRenderBoundingBox} (invokeinterface с call-site),
 * на 1.21.1 такого вызова нет - инъекция не находит точку и скипается.
 * Параметр {@code Object}: класс forge-интерфейса отсутствует на neoforge-
 * класспасе, а супертип в сигнатуре хендлера Mixin принимает.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public class SodiumRenderBoundsMixin {

    @Redirect(method = "renderBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/common/extensions/IForgeBlockEntity;getRenderBoundingBox()Lnet/minecraft/world/phys/AABB;"),
            remap = false,
            require = 0)
    private net.minecraft.world.phys.AABB hbm$cachedRenderBounds(Object be) {
        return com.hbm_m.client.render.RenderBoundsCache.get((net.minecraft.world.level.block.entity.BlockEntity) be);
    }
}
