package com.hbm_m.client.render;

import com.hbm_m.api.render.RenderBoundsProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * См. {@link RenderBoundsProvider}.
 *
 * На 1.21.1 NeoForge ванильный BER-пасс куллит block entities через
 * {@code BlockEntityRenderer#getRenderBoundingBox(be)} (default = 1 блок у
 * позиции BE), из-за чего мультиблоки исчезали, как только блок-контроллер
 * выходил за экран. Делегируем в BE: если он реализует
 * {@link RenderBoundsProvider}, используем его AABB (обычно вся структура).
 * На 1.20.1 оверрайда нет — BlockEntity#getRenderBoundingBox зовётся самим
 * Forge, и интерфейс пуст.
 */
public interface HbmBerBounds<T extends BlockEntity> extends BlockEntityRenderer<T> {

    //? if >= 1.21.1 {
    /*@Override
    default AABB getRenderBoundingBox(T be) {
        if (be instanceof RenderBoundsProvider provider) {
            return provider.getRenderBoundingBox();
        }
        return new AABB(be.getBlockPos());
    }
    *///?}
}
