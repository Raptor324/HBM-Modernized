package com.hbm_m.client.loader.dae;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Bakes DAE scene meshes into block-atlas {@link BakedQuad}s. Each triangle becomes a
 * degenerate quad (the 4th vertex repeats the 3rd) and the V coordinate is flipped like
 * the NEO baker ({@code v = 1 - uv}), so a baked model textures identically to the block
 * entity renderer's pose-stack geometry. Shared by the item model loader and the
 * transition seal BER.
 */
public final class DaeQuadBaker {

    private DaeQuadBaker() { }

    /** Bakes every scene mesh at the given animation time into one flat quad list. */
    public static List<BakedQuad> bakeScene(DaeModel model, DaeAnimation clip, float time, TextureAtlasSprite sprite) {
        List<BakedQuad> quads = new ArrayList<>();
        Matrix4f root = new Matrix4f();
        for (DaeNode node : model.sceneRoots) {
            collectQuads(node, root, clip, time, sprite, quads);
        }
        return quads;
    }

    private static void collectQuads(DaeNode node, Matrix4f parent, DaeAnimation clip, float time,
                                     TextureAtlasSprite sprite, List<BakedQuad> out) {
        Matrix4f matrix = new Matrix4f(parent).mul(node.localMatrix(time, clip));
        if (node.mesh != null) {
            out.addAll(bakeNodeQuads(node.mesh, matrix, sprite));
        }
        for (DaeNode child : node.children) {
            collectQuads(child, matrix, clip, time, sprite, out);
        }
    }

    /**
     * Converts a DAE mesh (triangle soup) into block-atlas quads, transforming each
     * vertex by the given matrix. An identity matrix yields the local-space geometry.
     */
    public static List<BakedQuad> bakeNodeQuads(DaeMesh mesh, Matrix4f matrix, TextureAtlasSprite sprite) {
        if (mesh == null || mesh.positions == null || mesh.tris.isEmpty()) {
            return List.of();
        }

        float[] positions = mesh.positions;
        float[] normals = mesh.normals;
        if (normals == null || normals.length == 0) {
            normals = DaeModel.computeNormals(mesh);
        }
        float[] uvs = mesh.uvs;
        boolean hasUvs = uvs != null && uvs.length > 0;

        Vector4f v0 = new Vector4f();
        Vector4f v1 = new Vector4f();
        Vector4f v2 = new Vector4f();
        List<BakedQuad> quads = new ArrayList<>(mesh.tris.size());

        for (int[] tri : mesh.tris) {
            if (tri.length < 9) continue;

            int p0 = tri[0] * 3, p1 = tri[3] * 3, p2 = tri[6] * 3;
            if (p0 < 0 || p1 < 0 || p2 < 0 || p0 + 2 >= positions.length || p1 + 2 >= positions.length || p2 + 2 >= positions.length) {
                continue;
            }

            matrix.transform(positions[p0], positions[p0 + 1], positions[p0 + 2], 1F, v0);
            matrix.transform(positions[p1], positions[p1 + 1], positions[p1 + 2], 1F, v1);
            matrix.transform(positions[p2], positions[p2 + 1], positions[p2 + 2], 1F, v2);

            float ax = v0.x, ay = v0.y, az = v0.z;
            float bx = v1.x, by = v1.y, bz = v1.z;
            float cx = v2.x, cy = v2.y, cz = v2.z;

            float nx = (by - ay) * (cz - az) - (bz - az) * (cy - ay);
            float ny = (bz - az) * (cx - ax) - (bx - ax) * (cz - az);
            float nz = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
            Direction dir = faceDirection(nx, ny, nz);

            int[] data = new int[32];
            for (int i = 0; i < 3; i++) {
                int posIdx = tri[i * 3] * 3;
                int normalIdx = tri[i * 3 + 1] * 3;
                int uvIdx = tri[i * 3 + 2] * 2;

                Vector4f v = i == 0 ? v0 : (i == 1 ? v1 : v2);
                float nrmX = normalIdx >= 0 && normalIdx + 2 < normals.length ? normals[normalIdx] : 0F;
                float nrmY = normalIdx >= 0 && normalIdx + 2 < normals.length ? normals[normalIdx + 1] : 1F;
                float nrmZ = normalIdx >= 0 && normalIdx + 2 < normals.length ? normals[normalIdx + 2] : 0F;

                float u = hasUvs && uvIdx >= 0 && uvIdx + 1 < uvs.length ? uvs[uvIdx] : 0F;
                float w = hasUvs && uvIdx >= 0 && uvIdx + 1 < uvs.length ? 1F - uvs[uvIdx + 1] : 0F;

                int base = i * 8;
                data[base] = Float.floatToIntBits(v.x);
                data[base + 1] = Float.floatToIntBits(v.y);
                data[base + 2] = Float.floatToIntBits(v.z);
                data[base + 3] = 0xFFFFFFFF;
                // 1.21.1+: getU/getV принимают нормализованные 0..1 UV; на 1.20.1 — тексели 0..16.
                // getU0/getU1/getV0/getV1 имеют одинаковую семантику на обеих версиях.
                data[base + 4] = Float.floatToIntBits(net.minecraft.util.Mth.lerp(u, sprite.getU0(), sprite.getU1()));
                data[base + 5] = Float.floatToIntBits(net.minecraft.util.Mth.lerp(w, sprite.getV0(), sprite.getV1()));
                data[base + 6] = 0;
                data[base + 7] = packNormal(nrmX, nrmY, nrmZ);
            }

            System.arraycopy(data, 2 * 8, data, 3 * 8, 8);

            quads.add(new BakedQuad(data, -1, dir, sprite, true));
        }

        return quads;
    }

    /** Vanilla FaceBakery-style 3-byte normal packing. */
    private static int packNormal(float x, float y, float z) {
        float len = Mth.sqrt(x * x + y * y + z * z);
        if (len > 1e-6F) {
            x /= len;
            y /= len;
            z /= len;
        } else {
            x = 0F;
            y = 1F;
            z = 0F;
        }
        return ((int) (x * 127) & 0xFF) | (((int) (y * 127) & 0xFF) << 8) | (((int) (z * 127) & 0xFF) << 16);
    }

    private static Direction faceDirection(float x, float y, float z) {
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax >= ay && ax >= az) return x >= 0 ? Direction.EAST : Direction.WEST;
        if (ay >= ax && ay >= az) return y >= 0 ? Direction.UP : Direction.DOWN;
        return z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
