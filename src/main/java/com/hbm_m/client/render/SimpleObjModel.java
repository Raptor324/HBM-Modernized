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

    // Reusable render-thread scratch buffers (rendering is single-threaded);
    // keeps the per-triangle clipping loops heap-allocation-free.
    private static final Vector3f SCRATCH_NORMAL = new Vector3f();
    private static final org.joml.Vector4f SCRATCH_P4 = new org.joml.Vector4f();
    private static final Matrix3f SCRATCH_LN = new Matrix3f();
    private static final float[][] SCRATCH_IN_3 = new float[3][8];
    private static final float[][] SCRATCH_CLIP_POLY = new float[8][8];

    private final ResourceLocation location;
    private final Map<String, List<float[]>> parts = new LinkedHashMap<>();
    private boolean loaded = false;

    public SimpleObjModel(ResourceLocation location) {
        this.location = location;
    }

    /** Naechster erlaubter Ladeversuch nach einem Fehlschlag (ms); verhindert Dateizugriffe in jedem Frame. */
    private long retryAt = 0L;
    private boolean warned = false;

    private void ensureLoaded() {
        if (loaded) return;
        long now = System.currentTimeMillis();
        if (now < retryAt) return;

        // Fehlschlag nicht dauerhaft merken: fehlte die Datei beim ersten Zeichnen kurz (Ressourcen-Neuladen,
        // build/resources im Dev-Client gerade neu kopiert), blieb das Modell bis zum Neustart unsichtbar
        // (Spieltest: Mini-UFO nur als Hitbox sichtbar). Jetzt: erneuter Versuch nach einer Sekunde.
        var res = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
        if (res == null) {
            retryAt = now + 1000L;
            if (!warned) {
                warned = true;
                org.slf4j.LoggerFactory.getLogger("hbm_m").warn("SimpleObjModel: {} nicht gefunden, neuer Versuch folgt", location);
            }
            return;
        }
        parts.clear();

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
            loaded = true;
        } catch (Exception e) {
            loaded = true; // Datei vorhanden, aber fehlerhaft: Teilstand behalten wie bisher, nicht jede Sekunde neu parsen
            if (!warned) {
                warned = true;
                org.slf4j.LoggerFactory.getLogger("hbm_m").warn("SimpleObjModel: {} nicht lesbar: {}", location, e.toString());
            }
        }
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

    /** Resource location of the underlying {@code .obj} (mesh cache key for {@link com.hbm_m.client.render.weapon.WeaponRenderRegistry}). */
    public ResourceLocation getLocation() {
        return location;
    }

    /** Namen aller Teile ({@code o}/{@code g}-Gruppen) in Dateireihenfolge. */
    public java.util.Set<String> getPartNames() {
        ensureLoaded();
        return parts.keySet();
    }

    /** Wie {@link #renderPartEntity}, fuer alle Teile. */
    public void renderAllEntity(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        ensureLoaded();
        for (String part : parts.keySet()) renderPartEntity(part, pose, vc, light, overlay, r, g, b, a);
    }

    /**
     * Teil mit verschobener Texturkoordinate u (Original {@code glMatrixMode(GL_TEXTURE); glTranslatef(du, 0, 0)}),
     * fuer Lauflichter. Die Textur muss wiederholend gesampelt werden (normale Modelltexturen tun das).
     */
    public void renderPartShiftedU(String part, PoseStack pose, VertexConsumer vc, int light, float r, float g, float b, float du) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris == null) return;
        Matrix4f m = pose.last().pose();
        org.joml.Matrix3f n = pose.last().normal();
        org.joml.Vector3f normal = SCRATCH_NORMAL;
        int cr = (int) (r * 255), cg = (int) (g * 255), cb = (int) (b * 255);
        for (float[] tri : tris) {
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                normal.set(tri[base + 5], tri[base + 6], tri[base + 7]).mul(n);
                RenderHooks.vertexFull(vc, m, tri[base], tri[base + 1], tri[base + 2], cr, cg, cb, 255,
                        tri[base + 3] + du, 1F - tri[base + 4], net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
            }
        }
    }

    /** Fuer Wesenmodelle: Teil mit Overlay (Treffer-Rot) und Farbe samt Alpha. */
    public void renderPartEntity(String part, PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris == null) return;
        Matrix4f m = pose.last().pose();
        org.joml.Matrix3f n = pose.last().normal();
        org.joml.Vector3f normal = SCRATCH_NORMAL;
        int cr = (int) (r * 255), cg = (int) (g * 255), cb = (int) (b * 255), ca = (int) (a * 255);
        for (float[] tri : tris) {
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                normal.set(tri[base + 5], tri[base + 6], tri[base + 7]).mul(n);
                RenderHooks.vertexFull(vc, m, tri[base], tri[base + 1], tri[base + 2], cr, cg, cb, ca,
                        tri[base + 3], 1F - tri[base + 4], overlay, light, normal.x, normal.y, normal.z);
            }
        }
    }

    /** Ein Teil nur mit Position + Farbe (POSITION_COLOR/TRIANGLES). */
    public void renderPartColor(String part, PoseStack pose, VertexConsumer vc, float r, float g, float b, float a) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris == null) return;
        Matrix4f m = pose.last().pose();
        for (float[] tri : tris) {
            for (int v = 0; v < 3; v++) {
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, tri[v * 8], tri[v * 8 + 1], tri[v * 8 + 2], r, g, b, a);
            }
        }
    }

    /**
     * Wie {@link #renderPartColor}, aber um {@code dy} verschoben und an der Ebene {@code y >= clipY} beschnitten
     * (Ersatz fuer {@code GL_CLIP_PLANE0} mit {@code {0, 1, 0, -clipY}}, gesetzt vor der Verschiebung).
     */
    public void renderPartColorClippedY(String part, PoseStack pose, VertexConsumer vc, float r, float g, float b, float a, float dy, float clipY) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris == null) return;
        Matrix4f m = pose.last().pose();
        float[][] in = SCRATCH_IN_3;
        float[][] poly = SCRATCH_CLIP_POLY;
        for (float[] tri : tris) {
            for (int v = 0; v < 3; v++) {
                in[v][0] = tri[v * 8];
                in[v][1] = tri[v * 8 + 1] + dy;
                in[v][2] = tri[v * 8 + 2];
            }
            int polyCount = 0;
            for (int v = 0; v < 3; v++) {
                float[] cur = in[v], nxt = in[(v + 1) % 3];
                boolean cIn = cur[1] >= clipY, nIn = nxt[1] >= clipY;
                if (cIn) {
                    poly[polyCount][0] = cur[0];
                    poly[polyCount][1] = cur[1];
                    poly[polyCount][2] = cur[2];
                    polyCount++;
                }
                if (cIn != nIn) {
                    float t = (clipY - cur[1]) / (nxt[1] - cur[1]);
                    float[] cut = poly[polyCount];
                    cut[0] = cur[0] + (nxt[0] - cur[0]) * t;
                    cut[1] = clipY;
                    cut[2] = cur[2] + (nxt[2] - cur[2]) * t;
                    polyCount++;
                }
            }
            if (polyCount < 3) continue;
            for (int k = 1; k + 1 < polyCount; k++) {
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, poly[0][0], poly[0][1], poly[0][2], r, g, b, a);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, poly[k][0], poly[k][1], poly[k][2], r, g, b, a);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, poly[k + 1][0], poly[k + 1][1], poly[k + 1][2], r, g, b, a);
            }
        }
    }

    /**
     * Texturiertes Teil mit Schnittebene (Ersatz fuer {@code glClipPlane(GL_CLIP_PLANE0, {a, b, c, d})}): Die Ebene
     * gilt im Koordinatensystem von {@code pose} (dort, wo das Original {@code glClipPlane} aufruft); {@code local} ist
     * die danach folgende Verschiebung/Drehung des Teils. Erhalten bleibt alles mit {@code a*x + b*y + c*z + d >= 0}.
     */
    public void renderPartClipped(String part, PoseStack pose, Matrix4f local, VertexConsumer vc, int light,
                                  float a, float b, float c, float d) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris == null) return;
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        Matrix3f ln = SCRATCH_LN.set(local).invert().transpose();
        org.joml.Vector4f p4 = SCRATCH_P4;
        Vector3f nv = SCRATCH_NORMAL;
        float[][] in = SCRATCH_IN_3;
        float[][] poly = SCRATCH_CLIP_POLY;
        for (float[] tri : tris) {
            for (int v = 0; v < 3; v++) {
                int o = v * 8;
                p4.set(tri[o], tri[o + 1], tri[o + 2], 1F).mul(local);
                nv.set(tri[o + 5], tri[o + 6], tri[o + 7]).mul(ln);
                float[] row = in[v];
                row[0] = p4.x;
                row[1] = p4.y;
                row[2] = p4.z;
                row[3] = tri[o + 3];
                row[4] = tri[o + 4];
                row[5] = nv.x;
                row[6] = nv.y;
                row[7] = nv.z;
            }
            int polyCount = 0;
            for (int v = 0; v < 3; v++) {
                float[] cur = in[v], nxt = in[(v + 1) % 3];
                float dc = a * cur[0] + b * cur[1] + c * cur[2] + d;
                float dn = a * nxt[0] + b * nxt[1] + c * nxt[2] + d;
                if (dc >= 0) {
                    float[] dst = poly[polyCount++];
                    for (int k = 0; k < 8; k++) dst[k] = cur[k];
                }
                if ((dc >= 0) != (dn >= 0)) {
                    float t = dc / (dc - dn);
                    float[] cut = poly[polyCount++];
                    for (int k = 0; k < 8; k++) cut[k] = cur[k] + (nxt[k] - cur[k]) * t;
                }
            }
            if (polyCount < 3) continue;
            for (int k = 1; k + 1 < polyCount; k++) {
                // Dreieck als entartetes Viereck (letzter Punkt doppelt) fuer QUADS-Puffer
                for (int f = 0; f < 4; f++) {
                    float[] p = poly[f == 0 ? 0 : (f == 1 ? k : k + 1)];
                    nv.set(p[5], p[6], p[7]).mul(n);
                    RenderHooks.vertexFull(vc, m, p[0], p[1], p[2], 255, 255, 255, 255,
                            p[3], 1F - p[4], OverlayTexture.NO_OVERLAY, light, nv.x, nv.y, nv.z);
                }
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
                    com.hbm_m.platform.RenderHooks.vertexColor(vc, m, tri[v * 8], tri[v * 8 + 1], tri[v * 8 + 2], r, g, b, a);
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
        Vector3f normal = SCRATCH_NORMAL;
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
