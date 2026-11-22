package com.hbm_m.client.render.armor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Lightweight Wavefront reader following {@code HFRWavefrontObject} (1.7.10): groups ("o"/"g"),
 * triangles and quads, UV with inverted V ({@code 1 - v}), vertex normals if present.
 * {@link #renderPart} corresponds to {@code IModelCustom.renderPart}.
 */
public final class ArmorObjModel {

    private static final Map<ResourceLocation, ArmorObjModel> CACHE = new ConcurrentHashMap<>();

    private record Face(float[][] pos, float[][] uv, float[][] normal) {}

    private final Map<String, List<Face>> groups = new HashMap<>();

    private ArmorObjModel() {}

    public static ArmorObjModel get(ResourceLocation location) {
        return CACHE.computeIfAbsent(location, ArmorObjModel::load);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static ArmorObjModel load(ResourceLocation location) {
        ArmorObjModel model = new ArmorObjModel();

        // nmesh-first: compiled .nmesh data (or NMeshLoader resource pack fallback) instead of custom Wavefront reader.
        var nmesh = com.hbm_m.client.model.nmesh.NMeshLoader.load(location);
        if (nmesh != null) {
            try {
                for (var sm : nmesh.subMeshes()) {
                    String key = sm.objectName != null && !sm.objectName.isEmpty() ? sm.objectName
                            : (sm.groupName != null && !sm.groupName.isEmpty() ? sm.groupName : "Default");
                    float[] verts = sm.vertices;
                    int[] idx = sm.indices;
                    List<Face> faces = model.groups.computeIfAbsent(key, k -> new ArrayList<>());
                    for (int i = 0; i + 2 < idx.length; i += 3) {
                        float[][] pos = new float[3][];
                        float[][] uv = new float[3][];
                        float[][] nor = new float[3][];
                        for (int c = 0; c < 3; c++) {
                            int b = idx[i + c] * 8;
                            pos[c] = new float[] {verts[b + 0], verts[b + 1], verts[b + 2]};
                            // This reader already flips V on load (1 - v); nmesh provides raw V.
                            uv[c] = new float[] {verts[b + 6], 1 - verts[b + 7]};
                            float nx = verts[b + 3], ny = verts[b + 4], nz = verts[b + 5];
                            nor[c] = (nx == 0 && ny == 0 && nz == 0) ? null : new float[] {nx, ny, nz};
                        }
                        faces.add(new Face(pos, uv, nor));
                    }
                }
                return model;
            } catch (Exception e) {
                MainRegistry.LOGGER.warn("ArmorObjModel: nmesh conversion of {} failed: {}", location, e.toString());
                model = new ArmorObjModel(); // Fresh restart for legacy path
            }
        }

        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<float[]> vn = new ArrayList<>();
        List<Face> current = model.groups.computeIfAbsent("Default", k -> new ArrayList<>());

        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                MainRegistry.LOGGER.error("Armor OBJ not found: {}", location);
                return model;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] t = line.split("\\s+");
                    switch (t[0]) {
                        case "v" -> v.add(new float[] {Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                        case "vt" -> vt.add(new float[] {Float.parseFloat(t[1]), 1 - Float.parseFloat(t[2])});
                        case "vn" -> vn.add(new float[] {Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                        case "o", "g" -> current = model.groups.computeIfAbsent(t.length > 1 ? t[1] : "Default", k -> new ArrayList<>());
                        case "f" -> {
                            int n = t.length - 1;
                            float[][] pos = new float[n][];
                            float[][] uv = new float[n][];
                            float[][] nor = new float[n][];
                            for (int i = 0; i < n; i++) {
                                String[] idx = t[i + 1].split("/");
                                pos[i] = v.get(Integer.parseInt(idx[0]) - 1);
                                uv[i] = idx.length > 1 && !idx[1].isEmpty() ? vt.get(Integer.parseInt(idx[1]) - 1) : new float[] {0, 0};
                                nor[i] = idx.length > 2 && !idx[2].isEmpty() ? vn.get(Integer.parseInt(idx[2]) - 1) : null;
                            }
                            current.add(new Face(pos, uv, nor));
                        }
                        default -> { }
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to read armor OBJ {}", location, e);
        }
        return model;
    }

    /** Original {@code renderAll}: all groups. */
    public void renderAll(PoseStack poseStack, VertexConsumer vc, int light, float r, float g, float b, float a) {
        for (String name : groups.keySet()) renderPart(name, poseStack, vc, light, r, g, b, a);
    }

    /** Original {@code renderPart}: write one group with the current matrix to the buffer. */
    public void renderPart(String name, PoseStack poseStack, VertexConsumer vc, int light, float r, float g, float b, float a) {
        List<Face> faces = groups.get(name);
        if (faces == null) return;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f nmat = pose.normal();

        for (Face face : faces) {
            float[] fn = face.normal[0] == null ? faceNormal(face.pos) : null;
            int n = face.pos.length;
            // Render types draw quads: triangles duplicate the final vertex.
            for (int i = 0; i < 4; i++) {
                int k = Math.min(i, n - 1);
                float[] p = face.pos[k];
                float[] uv = face.uv[k];
                float[] no = face.normal[k] != null ? face.normal[k] : fn;
                com.hbm_m.platform.RenderHooks.vertexFull(vc, mat, p[0], p[1], p[2], r, g, b, a,
                        uv[0], uv[1], OverlayTexture.NO_OVERLAY, light, nmat, no[0], no[1], no[2]);
            }
        }
    }

    private static float[] faceNormal(float[][] p) {
        float ax = p[1][0] - p[0][0], ay = p[1][1] - p[0][1], az = p[1][2] - p[0][2];
        float bx = p[2][0] - p[0][0], by = p[2][1] - p[0][1], bz = p[2][2] - p[0][2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) return new float[] {0, 1, 0};
        return new float[] {nx / len, ny / len, nz / len};
    }
}
