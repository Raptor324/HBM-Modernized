package com.hbm_m.platform.legacy.client.model;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * [26.x] Ersatz für den alten Konstruktor {@code new BakedQuad(int[] vertices, tint, direction, sprite, shade)}.
 * Stonecutter leitet {@code new BakedQuad(} hierher um (Regel {@code mc26_bakedquad_ctor}).
 *
 * <p>Erwartet das alte BLOCK-Vertexformat: 8 ints pro Vertex
 * ({@code x, y, z, ABGR-Farbe, u, v, Lightmap, Normal}).
 */
public final class LegacyQuads {

    public static final int STRIDE = 8;

    private LegacyQuads() {}

    public static BakedQuad of(int[] data, int tintIndex, Direction direction, @Nullable TextureAtlasSprite sprite, boolean shade) {
        QuadBakingVertexConsumer qb = new QuadBakingVertexConsumer();
        if (sprite != null) qb.setSprite(new Material.Baked(sprite, false));
        qb.setTintIndex(tintIndex);
        qb.setDirection(direction);
        qb.setShadeOverride(shade ? null : Direction.UP);
        int light = 0;
        for (int v = 0; v < 4; v++) {
            int o = v * STRIDE;
            qb.addVertex(Float.intBitsToFloat(data[o]), Float.intBitsToFloat(data[o + 1]), Float.intBitsToFloat(data[o + 2]));
            int c = data[o + 3];
            qb.setColor(c & 0xFF, (c >>> 8) & 0xFF, (c >>> 16) & 0xFF, (c >>> 24) & 0xFF);
            qb.setUv(Float.intBitsToFloat(data[o + 4]), Float.intBitsToFloat(data[o + 5]));
            light = Math.max(light, (data[o + 6] & 0xFFFF) >> 4);
            int n = data[o + 7];
            qb.setNormal(((byte) (n & 0xFF)) / 127f, ((byte) ((n >> 8) & 0xFF)) / 127f, ((byte) ((n >> 16) & 0xFF)) / 127f);
        }
        if (light > 0) qb.setLightEmission(Math.min(15, light));
        return qb.bakeQuad();
    }
}
