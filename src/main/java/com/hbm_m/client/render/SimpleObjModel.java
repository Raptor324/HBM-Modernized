package com.hbm_m.client.render;

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

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Simple counterpart to 1.7.10 {@code AdvancedModelLoader.loadModel(...).renderAll()/renderPart()}: loads
 * model geometry on first draw (grouped by object/group names) and emits triangles through a
 * {@link VertexConsumer}. UV-v is flipped when rendering, matching the 1.7.10 Forge OBJ loader.
 *
 * @deprecated Use {@link com.hbm_m.client.model.nmesh.NMeshPartBakedModel} or {@link SingleMeshVboRenderer} instead.
 */
@Deprecated
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

    /** Next allowed load attempt after a failure (ms); prevents file access every frame. */
    private long retryAt = 0L;
    private boolean warned = false;

    private void ensureLoaded() {
        if (loaded) return;
        long now = System.currentTimeMillis();
        if (now < retryAt) return;

        // Load geometry via NMeshLoader (handles both compiled .nmesh and fallback .obj).
        var nmesh = com.hbm_m.client.model.nmesh.NMeshLoader.load(location);
        if (nmesh == null) {
            retryAt = now + 1000L;
            if (!warned) {
                warned = true;
                org.slf4j.LoggerFactory.getLogger("hbm_m").warn("SimpleObjModel: {} not found, will retry", location);
            }
            return;
        }

        try {
            parts.clear();
            for (var sm : nmesh.subMeshes()) {
                String key = sm.objectName != null && !sm.objectName.isEmpty() ? sm.objectName
                        : (sm.groupName != null && !sm.groupName.isEmpty() ? sm.groupName : "default");
                float[] verts = sm.vertices;
                int[] idx = sm.indices;
                List<float[]> tris = parts.computeIfAbsent(key, k -> new ArrayList<>());
                for (int i = 0; i + 2 < idx.length; i += 3) {
                    float[] t = new float[24];
                    for (int c = 0; c < 3; c++) {
                        int b = idx[i + c] * 8;
                        // SimpleObjModel stores u,v BEFORE normals and flips V when rendering (1 - v):
                        // take raw u,v from index 6..7, normals from index 3..5.
                        t[c * 8 + 0] = verts[b + 0];
                        t[c * 8 + 1] = verts[b + 1];
                        t[c * 8 + 2] = verts[b + 2];
                        t[c * 8 + 3] = verts[b + 6];
                        t[c * 8 + 4] = verts[b + 7];
                        t[c * 8 + 5] = verts[b + 3];
                        t[c * 8 + 6] = verts[b + 4];
                        t[c * 8 + 7] = verts[b + 5];
                    }
                    tris.add(t);
                }
            }
            loaded = true;
        } catch (Exception e) {
            loaded = true;
            if (!warned) {
                warned = true;
                org.slf4j.LoggerFactory.getLogger("hbm_m").warn("SimpleObjModel: Failed to load mesh from {}: {}", location, e.toString());
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

    /** renderPart with color tint (equivalent to glColor4f before renderPart). */
    public void renderPartTinted(String part, PoseStack pose, VertexConsumer vc, int light, float r, float g, float b) {
        ensureLoaded();
        List<float[]> tris = parts.get(part);
        if (tris != null) render(pose, vc, light, tris, (int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    /** Resource location of the underlying {@code .obj} (mesh cache key for {@link com.hbm_m.client.render.weapon.WeaponRenderRegistry}). */
    public ResourceLocation getLocation() {
        return location;
    }

    /** Names of all parts ({@code o}/{@code g} groups) in file order. */
    public java.util.Set<String> getPartNames() {
        ensureLoaded();
        return parts.keySet();
    }

    /** Like {@link #renderPartEntity}, for all parts. */
    public void renderAllEntity(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        ensureLoaded();
        for (String part : parts.keySet()) renderPartEntity(part, pose, vc, light, overlay, r, g, b, a);
    }

    /**
     * Part with shifted texture coordinate u (equivalent to {@code glMatrixMode(GL_TEXTURE); glTranslatef(du, 0, 0)}),
     * for animated scrolling indicators. The texture must use repeat wrapping.
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

    /** For entity models: part with overlay (hurt red) and color with alpha. */
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

    /** A part rendered only with position + color (POSITION_COLOR/TRIANGLES). */
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
     * Like {@link #renderPartColor}, but offset by {@code dy} and clipped at the plane {@code y >= clipY}
     * (replacement for {@code GL_CLIP_PLANE0} with {@code {0, 1, 0, -clipY}}, set before translation).
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
     * Textured part with clipping plane (replacement for {@code glClipPlane(GL_CLIP_PLANE0, {a, b, c, d})}):
     * The plane applies in the coordinate space of {@code pose}; {@code local} is the subsequent
     * part transformation. Preserves geometry where {@code a*x + b*y + c*z + d >= 0}.
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
                // Triangle as degenerate quad (duplicate last vertex) for QUADS buffer
                for (int f = 0; f < 4; f++) {
                    float[] p = poly[f == 0 ? 0 : (f == 1 ? k : k + 1)];
                    nv.set(p[5], p[6], p[7]).mul(n);
                    RenderHooks.vertexFull(vc, m, p[0], p[1], p[2], 255, 255, 255, 255,
                            p[3], 1F - p[4], OverlayTexture.NO_OVERLAY, light, nv.x, nv.y, nv.z);
                }
            }
        }
    }

    /** Position + color only (for POSITION_COLOR/TRIANGLES buffer, original untextured renderAll). */
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
            // Triangles as degenerate quads (duplicate last vertex) for QUADS buffer
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                normal.set(tri[base + 5], tri[base + 6], tri[base + 7]).mul(n);
                RenderHooks.vertexFull(vc, m, tri[base], tri[base + 1], tri[base + 2], cr, cg, cb, 255,
                        tri[base + 3], 1F - tri[base + 4], OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
            }
        }
    }
}
