package com.hbm_m.platform;

import net.minecraft.world.phys.AABB;

/**
 * Versionsfassade fuer Render-Bounding-Boxen von BlockEntities.
 * <ul>
 *   <li>1.20.1 Forge: {@code IForgeBlockEntity.INFINITE_EXTENT_AABB}, BE ueberschreibt {@code getRenderBoundingBox()}.</li>
 *   <li>1.21.1 NeoForge: {@code AABB.INFINITE}; die Box gehoert dem Renderer
 *       ({@code IBlockEntityRendererExtension#getRenderBoundingBox(T)}). Die BE-Methode bleibt (ohne
 *       {@code @Override}, per Weiche), der BER delegiert auf neo darauf - siehe VERSIONPORT.md.</li>
 * </ul>
 */
public final class RenderBounds {
    private RenderBounds() {}

    //? if forge {
    public static final AABB INFINITE = net.minecraftforge.common.extensions.IForgeBlockEntity.INFINITE_EXTENT_AABB;
    //?} elif >= 1.21.1 {
    /*public static final AABB INFINITE = AABB.INFINITE;
    *///?} else {
    /*public static final AABB INFINITE = new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    *///?}

    /**
     * Единый доступ к расширенному AABB рендера BlockEntity (куллинг фрустом/светом).
     * Forge: расширение IForgeBlockEntity#getRenderBoundingBox (наши BE переопределяют его).
     * На 1.21.1 метода расширения нет — явный интерфейс RenderBoundsProvider
     * (реализован в BaseHbmBlockEntity), остальным — 1 блок + запас.
     * Живёт здесь (не в RenderHooks): сигнатуры содержат только common-типы, вызов
     * безопасен и из общего кода; RenderHooks — @OnlyIn(CLIENT).
     */
    public static AABB get(net.minecraft.world.level.block.entity.BlockEntity be) {
        //? if forge {
        return ((net.minecraftforge.common.extensions.IForgeBlockEntity) be).getRenderBoundingBox();
        //?} else {
        /*// BaseHbmBlockEntity реализует RenderBoundsProvider — сюда попадают и машины, и двери (отдельные instanceof были мёртвыми ветками).
        if (be instanceof com.hbm_m.api.render.RenderBoundsProvider p) {
            return p.getRenderBoundingBox();
        }
        return new AABB(be.getBlockPos()).inflate(1.0D);
        *///?}
    }
}
