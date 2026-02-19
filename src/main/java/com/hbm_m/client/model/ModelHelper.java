package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// Утилитарный класс для создания кубоидов с настраиваемыми UV координатами для каждой грани. Необходим для процедурной генерации модели провода.
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

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

    /** Position: первые 3 int (x,y,z). Normal: последний int вершины (Embeddium=8 ints/vertex, Vanilla=9). */
    private static final int POSITION_OFFSET = 0;
    private static final int MIN_STRIDE = 8; // Embeddium/Sodium compact format

    /**
     * Поворачивает квады вокруг оси Y через центр блока (0.5, 0.5, 0.5).
     * @param quads исходные квады
     * @param angleDeg угол в градусах (0=North, 90=East, 180=South, 270=West)
     */
    public static List<BakedQuad> transformQuadsByFacing(List<BakedQuad> quads, int angleDeg) {
        if (quads == null || quads.isEmpty() || angleDeg == 0) {
            return quads;
        }
        float angleRad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float cx = 0.5f, cz = 0.5f;

        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int normalOffset = stride - 1; // normal в последнем int вершины

            int[] newVerts = verts.clone();

            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float x = Float.intBitsToFloat(newVerts[i + POSITION_OFFSET]);
                float y = Float.intBitsToFloat(newVerts[i + POSITION_OFFSET + 1]);
                float z = Float.intBitsToFloat(newVerts[i + POSITION_OFFSET + 2]);

                float dx = x - cx, dz = z - cz;
                float nx = dx * cos - dz * sin + cx;
                float nz = dx * sin + dz * cos + cz;

                newVerts[i] = Float.floatToIntBits(nx);
                newVerts[i + 1] = Float.floatToIntBits(y);
                newVerts[i + 2] = Float.floatToIntBits(nz);

                if (normalOffset < stride) {
                    int packedNormal = newVerts[i + normalOffset];
                    byte nxb = (byte) (packedNormal & 0xFF);
                    byte nyb = (byte) ((packedNormal >> 8) & 0xFF);
                    byte nzb = (byte) ((packedNormal >> 16) & 0xFF);
                    float nxf = (nxb) / 127f;
                    float nyf = (nyb) / 127f;
                    float nzf = (nzb) / 127f;
                    float rnx = nxf * cos - nzf * sin;
                    float rnz = nxf * sin + nzf * cos;
                    int newPacked = ((byte) (rnx * 127) & 0xFF) | (((byte) (nyf * 127) & 0xFF) << 8) | (((byte) (rnz * 127) & 0xFF) << 16);
                    newVerts[i + normalOffset] = newPacked;
                }
            }
            Direction rotatedDir = rotateDirection(quad.getDirection(), angleDeg);
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), rotatedDir, quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    /**
     * Смещает квады на (dx, dy, dz). Для выравнивания baked model с BER (translate 0.5, 0, 0.5).
     */
    public static List<BakedQuad> translateQuads(List<BakedQuad> quads, float dx, float dy, float dz) {
        if (quads == null || quads.isEmpty() || (dx == 0 && dy == 0 && dz == 0)) {
            return quads;
        }
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float x = Float.intBitsToFloat(verts[i + POSITION_OFFSET]);
                float y = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 1]);
                float z = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 2]);
                newVerts[i] = Float.floatToIntBits(x + dx);
                newVerts[i + 1] = Float.floatToIntBits(y + dy);
                newVerts[i + 2] = Float.floatToIntBits(z + dz);
            }
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    private static Direction rotateDirection(Direction dir, int angleDeg) {
        if (dir.getAxis() == Direction.Axis.Y) return dir;
        int steps = (angleDeg / 90) % 4;
        if (steps < 0) steps += 4;
        Direction r = dir;
        for (int i = 0; i < steps; i++) {
            r = r.getClockWise(Direction.Axis.Y);
        }
        return r;
    }

    /**
     * Применяет произвольную матрицу 4x4 к вершинам квадов (позиция и нормаль).
     * Используется для трансформации door panel в open/closed состоянии в BakedModel.
     */
    public static List<BakedQuad> transformQuadsByMatrix(List<BakedQuad> quads, Matrix4f matrix) {
        if (quads == null || quads.isEmpty()) {
            return quads;
        }
        Matrix4f normalMatrix = new Matrix4f(matrix).invert().transpose();
        List<BakedQuad> result = new ArrayList<>(quads.size());
        Vector4f pos = new Vector4f();
        Vector4f norm = new Vector4f();
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int normalOffset = stride - 1;
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float x = Float.intBitsToFloat(verts[i + POSITION_OFFSET]);
                float y = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 1]);
                float z = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 2]);
                pos.set(x, y, z, 1f);
                pos.mul(matrix);
                newVerts[i] = Float.floatToIntBits(pos.x());
                newVerts[i + 1] = Float.floatToIntBits(pos.y());
                newVerts[i + 2] = Float.floatToIntBits(pos.z());
                if (normalOffset < stride) {
                    int packedNormal = verts[i + normalOffset];
                    byte nxb = (byte) (packedNormal & 0xFF);
                    byte nyb = (byte) ((packedNormal >> 8) & 0xFF);
                    byte nzb = (byte) ((packedNormal >> 16) & 0xFF);
                    norm.set(nxb / 127f, nyb / 127f, nzb / 127f, 0f);
                    norm.mul(normalMatrix);
                    norm.normalize();
                    int newPacked = ((byte) (norm.x * 127) & 0xFF)
                            | (((byte) (norm.y * 127) & 0xFF) << 8)
                            | (((byte) (norm.z * 127) & 0xFF) << 16);
                    newVerts[i + normalOffset] = newPacked;
                }
            }
            int packedN = newVerts[normalOffset];
            byte nxb = (byte) (packedN & 0xFF);
            byte nyb = (byte) ((packedN >> 8) & 0xFF);
            byte nzb = (byte) ((packedN >> 16) & 0xFF);
            Direction transformedDir = directionFromNormal(nxb / 127f, nyb / 127f, nzb / 127f);
            if (transformedDir == null) transformedDir = quad.getDirection();
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), transformedDir, quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    private static Direction directionFromNormal(float dx, float dy, float dz) {
        float ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (az >= ax && az >= ay) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }
}