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
}
