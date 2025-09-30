package com.hbm_m.client.model;

// Утилитарный класс для создания кубоидов с настраиваемыми UV координатами для каждой грани. Необходим для процедурной генерации модели провода.
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelHelper {

    public record UVBox(float u0, float v0, float u1, float v1) {}
    public record UVSpec(UVBox box, boolean rotate90) {
        public UVSpec(UVBox box) { this(box, false); }
    }

    public static void createCuboid(List<BakedQuad> quads, Vector3f from, Vector3f to, TextureAtlasSprite sprite, Map<Direction, UVSpec> uvMap, Set<Direction> facesToSkip) {
        for (Direction direction : Direction.values()) {
            if (facesToSkip.contains(direction)) {
                continue;
            }
            UVSpec spec = uvMap.get(direction);
            if (spec != null) {
                quads.add(createQuad(from, to, direction, sprite, spec));
            }
        }
    }

    private static BakedQuad createQuad(Vector3f from, Vector3f to, Direction direction, TextureAtlasSprite sprite, UVSpec spec) {
        QuadBakingVertexConsumer.Buffered builder = new QuadBakingVertexConsumer.Buffered();
        builder.setSprite(sprite);
        builder.setDirection(direction);
        builder.setHasAmbientOcclusion(true);

        Vector3f normal = direction.step();
        float x0 = from.x() / 16f, y0 = from.y() / 16f, z0 = from.z() / 16f;
        float x1 = to.x() / 16f,   y1 = to.y() / 16f,   z1 = to.z() / 16f;

        UVBox uv = spec.box();
        float u0 = sprite.getU(uv.u0), v0 = sprite.getV(uv.v0);
        float u1 = sprite.getU(uv.u1), v1 = sprite.getV(uv.v1);

        switch (direction) {
            case DOWN  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x0, y0, z0, u0, v0},
                                    new float[]{x1, y0, z0, u1, v0}, new float[]{x1, y0, z1, u1, v1});
            case UP    -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y1, z0, u0, v0}, new float[]{x0, y1, z1, u0, v1},
                                    new float[]{x1, y1, z1, u1, v1}, new float[]{x1, y1, z0, u1, v0});
            case NORTH -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y1, z0, u0, v0}, new float[]{x1, y1, z0, u1, v0},
                                    new float[]{x1, y0, z0, u1, v1}, new float[]{x0, y0, z0, u0, v1});
            case SOUTH -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x1, y0, z1, u1, v1},
                                    new float[]{x1, y1, z1, u1, v0}, new float[]{x0, y1, z1, u0, v0});
            case WEST  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x0, y1, z1, u0, v0},
                                    new float[]{x0, y1, z0, u1, v0}, new float[]{x0, y0, z0, u1, v1});
            case EAST  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x1, y0, z0, u0, v1}, new float[]{x1, y1, z0, u0, v0},
                                    new float[]{x1, y1, z1, u1, v0}, new float[]{x1, y0, z1, u1, v1});
        }
        return builder.getQuad();
    }

    private static void putVertices(QuadBakingVertexConsumer builder, Vector3f normal, boolean rotate, float[] v1, float[] v2, float[] v3, float[] v4) {
        if (!rotate) {
            putVertex(builder, normal, v1[0], v1[1], v1[2], v1[3], v1[4]);
            putVertex(builder, normal, v2[0], v2[1], v2[2], v2[3], v2[4]);
            putVertex(builder, normal, v3[0], v3[1], v3[2], v3[3], v3[4]);
            putVertex(builder, normal, v4[0], v4[1], v4[2], v4[3], v4[4]);
        } else {
            putVertex(builder, normal, v1[0], v1[1], v1[2], v2[3], v2[4]);
            putVertex(builder, normal, v2[0], v2[1], v2[2], v3[3], v3[4]);
            putVertex(builder, normal, v3[0], v3[1], v3[2], v4[3], v4[4]);
            putVertex(builder, normal, v4[0], v4[1], v4[2], v1[3], v1[4]);
        }
    }

    private static void putVertex(QuadBakingVertexConsumer builder, Vector3f normal, float x, float y, float z, float u, float v) {
        builder.vertex(x, y, z).uv(u, v).uv2(0, 0).normal(normal.x(), normal.y(), normal.z()).color(-1).endVertex();
    }
}