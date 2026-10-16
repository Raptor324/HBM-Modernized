package com.hbm_m.api.render;

import net.minecraft.world.phys.AABB;

/**
 * Общий (кросс-лоадерный) доступ к render bounding box'у block entity.
 *
 * На Forge 1.20.1 метод {@code BlockEntity#getRenderBoundingBox} — расширение
 * лоадера, и ванильный BER-пасс использует его сам. На NeoForge 1.21.1 такого
 * метода у BlockEntity нет — фрустум-куллинг идёт через
 * {@code BlockEntityRenderer#getRenderBoundingBox(be)} (default = 1 блок у
 * позиции BE). Поэтому рендереры HBM ({@code HbmBerBounds}) на 1.21.1 сами
 * делегируют в этот интерфейс, а существующие оверрайды
 * {@code getRenderBoundingBox()} в BE-классах автоматически становятся
 * реализациями интерфейса.
 */
public interface RenderBoundsProvider {

    /** AABB рендера BE в мировых координатах (для frustum-куллинга BER-пасса). */
    AABB getRenderBoundingBox();
}
