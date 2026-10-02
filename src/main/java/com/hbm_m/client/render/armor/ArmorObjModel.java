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
 * Schlanker Wavefront-Leser nach dem Vorbild von {@code HFRWavefrontObject} (1.7.10): Gruppen ("o"/"g"),
 * Dreiecke und Vierecke, UV mit gespiegeltem V ({@code 1 - v}), Vertexnormalen falls vorhanden.
 * {@link #renderPart} entspricht {@code IModelCustom.renderPart}.
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

    /** Original {@code renderAll}: alle Gruppen. */
    public void renderAll(PoseStack poseStack, VertexConsumer vc, int light, float r, float g, float b, float a) {
        for (String name : groups.keySet()) renderPart(name, poseStack, vc, light, r, g, b, a);
    }

    /** Original {@code renderPart}: eine Gruppe mit der aktuellen Matrix in den Puffer schreiben. */
    public void renderPart(String name, PoseStack poseStack, VertexConsumer vc, int light, float r, float g, float b, float a) {
        List<Face> faces = groups.get(name);
        if (faces == null) return;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f nmat = pose.normal();

        for (Face face : faces) {
            float[] fn = face.normal[0] == null ? faceNormal(face.pos) : null;
            int n = face.pos.length;
            // Die Render-Typen zeichnen Vierecke: Dreiecke bekommen den letzten Eckpunkt doppelt.
            for (int i = 0; i < 4; i++) {
                int k = Math.min(i, n - 1);
                float[] p = face.pos[k];
                float[] uv = face.uv[k];
                float[] no = face.normal[k] != null ? face.normal[k] : fn;
                //? if < 1.21.1 {
                vc.vertex(mat, p[0], p[1], p[2]).color(r, g, b, a).uv(uv[0], uv[1]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light).normal(nmat, no[0], no[1], no[2]).endVertex();
                //?} else {
                /*vc.addVertex(mat, p[0], p[1], p[2]).setColor(r, g, b, a).setUv(uv[0], uv[1]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(pose, no[0], no[1], no[2]);
                *///?}
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
