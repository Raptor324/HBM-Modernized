package com.hbm_m.client.render.armor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Compiled Wavefront/NMesh geometry holder for 3D entity armors, accessories, and armor items.
 * Loads precompiled {@code .nmesh} assets (with Wavefront {@code .obj} resourcepack fallback) and
 * flattens triangles into contiguous quad-ready float arrays for allocation-free rendering.
 */
public final class ArmorObjModel {

    private static final int FLOATS_PER_VERTEX = 8; // x, y, z, nx, ny, nz, u, v
    private static final int VERTICES_PER_QUAD = 4;
    private static final int FLOATS_PER_QUAD = FLOATS_PER_VERTEX * VERTICES_PER_QUAD;

    private static final Map<ResourceLocation, ArmorObjModel> CACHE = new ConcurrentHashMap<>();

    private record PartBatch(float[] quadVerts, float kdR, float kdG, float kdB) {}

    private static final class PartMesh {
        final List<PartBatch> batches = new ArrayList<>(1);
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        void expandBounds(float x, float y, float z) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        boolean hasBounds() {
            return minX <= maxX && minY <= maxY && minZ <= maxZ;
        }
    }

    private final Map<String, PartMesh> parts = new HashMap<>();

    private ArmorObjModel() {}

    public static ArmorObjModel get(ResourceLocation location) {
        return CACHE.computeIfAbsent(location, ArmorObjModel::load);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public boolean hasPart(String name) {
        return parts.containsKey(name);
    }

    /**
     * Returns the axis-aligned bounding box {@code [minX, minY, minZ, maxX, maxY, maxZ]} across the
     * given part names (in entity pixel units), or {@code null} if none of the parts exist.
     */
    @Nullable
    public float[] getBounds(Iterable<String> partNames) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        boolean any = false;
        for (String name : partNames) {
            PartMesh mesh = parts.get(name);
            if (mesh == null || !mesh.hasBounds()) continue;
            if (mesh.minX < minX) minX = mesh.minX;
            if (mesh.minY < minY) minY = mesh.minY;
            if (mesh.minZ < minZ) minZ = mesh.minZ;
            if (mesh.maxX > maxX) maxX = mesh.maxX;
            if (mesh.maxY > maxY) maxY = mesh.maxY;
            if (mesh.maxZ > maxZ) maxZ = mesh.maxZ;
            any = true;
        }
        return any ? new float[] { minX, minY, minZ, maxX, maxY, maxZ } : null;
    }

    private static ArmorObjModel load(ResourceLocation location) {
        ArmorObjModel model = loadFromNMesh(location);
        if (model != null) {
            return model;
        }
        return loadFromLegacyObj(location);
    }

    @Nullable
    private static ArmorObjModel loadFromNMesh(ResourceLocation location) {
        var nmesh = com.hbm_m.client.model.nmesh.NMeshLoader.load(location);
        if (nmesh == null) {
            return null;
        }
        try {
            ArmorObjModel model = new ArmorObjModel();
            for (var sm : nmesh.subMeshes()) {
                String key = (sm.objectName != null && !sm.objectName.isEmpty())
                        ? sm.objectName
                        : ((sm.groupName != null && !sm.groupName.isEmpty()) ? sm.groupName : "Default");
                int iCount = sm.indexCount();
                int triCount = iCount / 3;
                if (triCount <= 0) continue;

                PartMesh partMesh = model.parts.computeIfAbsent(key, k -> new PartMesh());
                float[] quadVerts = new float[triCount * FLOATS_PER_QUAD];
                int out = 0;

                for (int t = 0; t + 2 < iCount; t += 3) {
                    int v0 = sm.index(t);
                    int v1 = sm.index(t + 1);
                    int v2 = sm.index(t + 2);

                    float[] fn = null;
                    if (isZeroNormal(sm, v0) || isZeroNormal(sm, v1) || isZeroNormal(sm, v2)) {
                        fn = computeFaceNormal(
                                sm.posX(v0), sm.posY(v0), sm.posZ(v0),
                                sm.posX(v1), sm.posY(v1), sm.posZ(v1),
                                sm.posX(v2), sm.posY(v2), sm.posZ(v2));
                    }

                    int[] vis = { v0, v1, v2, v2 }; // duplicate 3rd vertex for QUADS vertex format
                    for (int c = 0; c < 4; c++) {
                        int vi = vis[c];
                        float x = sm.posX(vi);
                        float y = sm.posY(vi);
                        float z = sm.posZ(vi);
                        partMesh.expandBounds(x, y, z);

                        float nx = sm.normX(vi);
                        float ny = sm.normY(vi);
                        float nz = sm.normZ(vi);
                        if (nx == 0.0F && ny == 0.0F && nz == 0.0F && fn != null) {
                            nx = fn[0];
                            ny = fn[1];
                            nz = fn[2];
                        }
                        quadVerts[out++] = x;
                        quadVerts[out++] = y;
                        quadVerts[out++] = z;
                        quadVerts[out++] = nx;
                        quadVerts[out++] = ny;
                        quadVerts[out++] = nz;
                        quadVerts[out++] = sm.u(vi);
                        quadVerts[out++] = 1.0F - sm.v(vi);
                    }
                }
                partMesh.batches.add(new PartBatch(quadVerts, sm.kdR, sm.kdG, sm.kdB));
            }
            return model;
        } catch (Exception e) {
            MainRegistry.LOGGER.warn("ArmorObjModel: nmesh conversion of {} failed: {}", location, e.toString());
            return null;
        }
    }

    private static ArmorObjModel loadFromLegacyObj(ResourceLocation location) {
        ArmorObjModel model = new ArmorObjModel();
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<float[]> vn = new ArrayList<>();
        Map<String, List<Float>> rawQuads = new HashMap<>();
        List<Float> current = rawQuads.computeIfAbsent("Default", k -> new ArrayList<>());

        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                MainRegistry.LOGGER.error("Armor OBJ/NMesh not found: {}", location);
                return model;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] t = line.split("\\s+");
                    switch (t[0]) {
                        case "v" -> v.add(new float[] { Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
                        case "vt" -> vt.add(new float[] { Float.parseFloat(t[1]), 1.0F - Float.parseFloat(t[2]) });
                        case "vn" -> vn.add(new float[] { Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
                        case "o", "g" -> current = rawQuads.computeIfAbsent(t.length > 1 ? t[1] : "Default", k -> new ArrayList<>());
                        case "f" -> appendLegacyFace(t, v, vt, vn, current);
                        default -> { }
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to read armor OBJ {}", location, e);
        }

        for (var entry : rawQuads.entrySet()) {
            List<Float> floats = entry.getValue();
            if (floats.isEmpty()) continue;
            float[] arr = new float[floats.size()];
            PartMesh mesh = model.parts.computeIfAbsent(entry.getKey(), k -> new PartMesh());
            for (int i = 0; i < arr.length; i++) {
                arr[i] = floats.get(i);
            }
            for (int i = 0; i < arr.length; i += FLOATS_PER_VERTEX) {
                mesh.expandBounds(arr[i], arr[i + 1], arr[i + 2]);
            }
            mesh.batches.add(new PartBatch(arr, 1.0F, 1.0F, 1.0F));
        }
        return model;
    }

    private static void appendLegacyFace(String[] tokens, List<float[]> v, List<float[]> vt, List<float[]> vn, List<Float> out) {
        int n = tokens.length - 1;
        if (n < 3) return;
        float[][] pos = new float[n][];
        float[][] uv = new float[n][];
        float[][] nor = new float[n][];
        for (int i = 0; i < n; i++) {
            String[] idx = tokens[i + 1].split("/");
            pos[i] = v.get(Integer.parseInt(idx[0]) - 1);
            uv[i] = idx.length > 1 && !idx[1].isEmpty() ? vt.get(Integer.parseInt(idx[1]) - 1) : new float[] { 0.0F, 0.0F };
            nor[i] = idx.length > 2 && !idx[2].isEmpty() ? vn.get(Integer.parseInt(idx[2]) - 1) : null;
        }
        if (n <= 4) {
            emitLegacyQuad(pos, uv, nor, 0, 1, 2, Math.min(3, n - 1), out);
        } else {
            for (int i = 1; i + 1 < n; i++) {
                emitLegacyQuad(pos, uv, nor, 0, i, i + 1, i + 1, out);
            }
        }
    }

    private static void emitLegacyQuad(float[][] pos, float[][] uv, float[][] nor,
                                       int i0, int i1, int i2, int i3, List<Float> out) {
        float[] fn = computeFaceNormal(
                pos[i0][0], pos[i0][1], pos[i0][2],
                pos[i1][0], pos[i1][1], pos[i1][2],
                pos[i2][0], pos[i2][1], pos[i2][2]);
        int[] indices = { i0, i1, i2, i3 };
        for (int k : indices) {
            float[] p = pos[k];
            float[] t = uv[k];
            float[] no = nor[k] != null ? nor[k] : fn;
            out.add(p[0]); out.add(p[1]); out.add(p[2]);
            out.add(no[0]); out.add(no[1]); out.add(no[2]);
            out.add(t[0]); out.add(t[1]);
        }
    }

    public void renderAll(PoseStack poseStack, VertexConsumer vc, int light, float r, float g, float b, float a) {
        for (String name : parts.keySet()) {
            renderPart(name, poseStack, vc, light, r, g, b, a);
        }
    }

    public void renderPart(String name, PoseStack poseStack, VertexConsumer vc, int light, float r, float g, float b, float a) {
        renderPart(name, poseStack, vc, light, r, g, b, a, false);
    }

    public void renderPart(String name, PoseStack poseStack, VertexConsumer vc, int light,
                           float r, float g, float b, float a, boolean applyMtlKd) {
        PartMesh mesh = parts.get(name);
        if (mesh == null) return;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f nmat = pose.normal();

        for (int bi = 0; bi < mesh.batches.size(); bi++) {
            PartBatch batch = mesh.batches.get(bi);
            float cr = applyMtlKd ? r * batch.kdR : r;
            float cg = applyMtlKd ? g * batch.kdG : g;
            float cb = applyMtlKd ? b * batch.kdB : b;
            float[] q = batch.quadVerts;
            for (int i = 0; i < q.length; i += FLOATS_PER_VERTEX) {
                RenderHooks.vertexFull(vc, mat,
                        q[i], q[i + 1], q[i + 2],
                        cr, cg, cb, a,
                        q[i + 6], q[i + 7],
                        OverlayTexture.NO_OVERLAY, light,
                        nmat, q[i + 3], q[i + 4], q[i + 5]);
            }
        }
    }

    private static boolean isZeroNormal(com.hbm_m.client.model.nmesh.NMeshData.SubMesh sm, int v) {
        return (sm.packedNormals[v] & 0x00FFFFFF) == 0;
    }

    private static float[] computeFaceNormal(float x0, float y0, float z0,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2) {
        float ax = x1 - x0, ay = y1 - y0, az = z1 - z0;
        float bx = x2 - x0, by = y2 - y0, bz = z2 - z0;
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) {
            return new float[] { 0.0F, 1.0F, 0.0F };
        }
        return new float[] { nx / len, ny / len, nz / len };
    }
}
