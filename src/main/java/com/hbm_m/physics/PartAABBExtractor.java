// PartAABBExtractor.java

package com.hbm_m.physics;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import java.util.*;

/**
 * Утилита для извлечения AABB из частей BakedModel.
 * Проходит по всем quad'ам модели и вычисляет минимальный охватывающий бокс.
 */
@OnlyIn(Dist.CLIENT)
public final class PartAABBExtractor {

    private static final RandomSource RANDOM = RandomSource.create(0);

    /**
     * Извлекает AABB из одной части модели.
     * @param partModel BakedModel для одной части
     * @return AABB в координатах блока (0..1 по каждой оси) или null если нет геометрии
     */
    public static AABB extract(BakedModel partModel) {
        if (partModel == null) return null;

        List<BakedQuad> allQuads = new ArrayList<>();
        allQuads.addAll(partModel.getQuads(null, null, RANDOM, ModelData.EMPTY, null));
        for (Direction dir : Direction.values()) {
            allQuads.addAll(partModel.getQuads(null, dir, RANDOM, ModelData.EMPTY, null));
        }

        if (allQuads.isEmpty()) return null;

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (BakedQuad quad : allQuads) {
            int[] vertexData = quad.getVertices();
            int vertexSize = DefaultVertexFormat.BLOCK.getIntegerSize();

            for (int v = 0; v < 4; v++) {
                int offset = v * vertexSize;
                float x = Float.intBitsToFloat(vertexData[offset + 0]);
                float y = Float.intBitsToFloat(vertexData[offset + 1]);
                float z = Float.intBitsToFloat(vertexData[offset + 2]);

                // Переводим из пикселей (0..16) в блоки (0..1)
                double bx = x / 16.0;
                double by = y / 16.0;
                double bz = z / 16.0;

                minX = Math.min(minX, bx);
                minY = Math.min(minY, by);
                minZ = Math.min(minZ, bz);
                maxX = Math.max(maxX, bx);
                maxY = Math.max(maxY, by);
                maxZ = Math.max(maxZ, bz);
            }
        }

        if (Double.isInfinite(minX)) return null;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Извлекает AABB для всех частей из карты BakedModel.
     * @param parts Карта partName -> BakedModel
     * @return Карта partName -> AABB в нормализованных координатах (0..1)
     */
    public static Map<String, AABB> extractAll(Map<String, BakedModel> parts) {
        Map<String, AABB> result = new HashMap<>();
        for (Map.Entry<String, BakedModel> entry : parts.entrySet()) {
            AABB aabb = extract(entry.getValue());
            if (aabb != null) {
                result.put(entry.getKey(), aabb);
            }
        }
        return result;
    }

    private PartAABBExtractor() {}
}
