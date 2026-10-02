package com.hbm_m.client.render;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Einfaches Gegenstueck zu {@code AdvancedModelLoader.loadModel(...).renderAll()/renderPart()} der 1.7.10: liest
 * eine .obj-Datei beim ersten Zeichnen (nach Objekt-/Gruppennamen getrennt) und gibt die Dreiecke ueber einen
 * {@link VertexConsumer} aus. UV-v wird wie beim Forge-OBJ-Lader der 1.7.10 gespiegelt.
 */
public class SimpleObjModel {

    private final ResourceLocation location;
    private final Map<String, List<float[]>> parts = new LinkedHashMap<>();
    private boolean loaded = false;

    public SimpleObjModel(ResourceLocation location) {
        this.location = location;
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        var res = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
        if (res == null) return;

        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nrm = new ArrayList<>();
        String curPart = "default";

        try (var r = new BufferedReader(new InputStreamReader(res.open()))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    pos.add(new float[] {Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    uv.add(new float[] {Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } else if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    nrm.add(new float[] {Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("o ") || line.startsWith("g ")) {
                    curPart = line.substring(2).trim();
                } else if (line.startsWith("f ")) {
                    String[] verts = line.substring(2).trim().split("\\s+");
                    List<float[]> vs = new ArrayList<>();
                    for (String v : verts) {
                        String[] idx = v.split("/");
                        int vi = Integer.parseInt(idx[0]) - 1;
                        int ti = idx.length > 1 && !idx[1].isEmpty() ? Integer.parseInt(idx[1]) - 1 : -1;
                        int ni = idx.length > 2 && !idx[2].isEmpty() ? Integer.parseInt(idx[2]) - 1 : -1;
                        float[] p = pos.get(vi);
                        float u = ti >= 0 ? uv.get(ti)[0] : 0, v2 = ti >= 0 ? uv.get(ti)[1] : 0;
                        float nx = ni >= 0 ? nrm.get(ni)[0] : 0, ny = ni >= 0 ? nrm.get(ni)[1] : 1, nz = ni >= 0 ? nrm.get(ni)[2] : 0;
                        vs.add(new float[] {p[0], p[1], p[2], u, v2, nx, ny, nz});
                    }
                    List<float[]> tris = parts.computeIfAbsent(curPart, k -> new ArrayList<>());
                    for (int i = 1; i < vs.size() - 1; i++) {
                        float[] t = new float[24];
                        System.arraycopy(vs.get(0), 0, t, 0, 8);
                        System.arraycopy(vs.get(i), 0, t, 8, 8);
                        System.arraycopy(vs.get(i + 1), 0, t, 16, 8);
                        tris.add(t);
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    public void renderAll(PoseStack pose, VertexConsumer vc, int light) {
        ensureLoaded();
        for (List<float[]> tris : parts.values()) render(pose, vc, light, tris);
    }

    public void renderPart(String part, PoseStack pose, VertexConsumer vc, int light) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris != null) render(pose, vc, light, tris);
    }

    /** renderPart mit Farbfaktor (Original glColor4f vor renderPart). */
    public void renderPartTinted(String part, PoseStack pose, VertexConsumer vc, int light, float r, float g, float b) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris != null) render(pose, vc, light, tris, (int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    /** Ein Teil nur mit Position + Farbe (POSITION_COLOR/TRIANGLES). */
    public void renderPartColor(String part, PoseStack pose, VertexConsumer vc, float r, float g, float b, float a) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris == null) return;
        Matrix4f m = pose.last().pose();
        for (float[] tri : tris) {
            for (int v = 0; v < 3; v++) {
                vc.vertex(m, tri[v * 8], tri[v * 8 + 1], tri[v * 8 + 2]).color(r, g, b, a).endVertex();
            }
        }
    }

    /** Nur Position + Farbe (fuer POSITION_COLOR/TRIANGLES-Puffer, Original: untexturiertes renderAll). */
    public void renderAllColor(PoseStack pose, VertexConsumer vc, float r, float g, float b, float a) {
        ensureLoaded();
        Matrix4f m = pose.last().pose();
        for (List<float[]> tris : parts.values()) {
            for (float[] tri : tris) {
                for (int v = 0; v < 3; v++) {
                    vc.vertex(m, tri[v * 8], tri[v * 8 + 1], tri[v * 8 + 2]).color(r, g, b, a).endVertex();
                }
            }
        }
    }

    private static void render(PoseStack pose, VertexConsumer vc, int light, List<float[]> tris) {
        render(pose, vc, light, tris, 255, 255, 255);
    }

    private static void render(PoseStack pose, VertexConsumer vc, int light, List<float[]> tris, int cr, int cg, int cb) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        Vector3f normal = new Vector3f();
        for (float[] tri : tris) {
            // Dreiecke als entartete Vierecke (letzter Punkt doppelt) fuer QUADS-Puffer
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                normal.set(tri[base + 5], tri[base + 6], tri[base + 7]).mul(n);
                RenderHooks.vertexFull(vc, m, tri[base], tri[base + 1], tri[base + 2], cr, cg, cb, 255,
                        tri[base + 3], 1F - tri[base + 4], OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
            }
        }
    }
}
