package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.SU47TrophyBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class SU47TrophyRenderer implements BlockEntityRenderer<SU47TrophyBlockEntity> {

    // ─── OBJ geometry: material name → list of triangles [x,y,z,u,v,nx,ny,nz] × 3 ──
    private static final Map<String, List<float[]>> OBJ_DATA = new LinkedHashMap<>();
    // ─── MTL: material name → ResourceLocation of texture ────────────────────────────
    private static final Map<String, ResourceLocation> MTL_MAP = new HashMap<>();
    private static boolean loaded = false;

    private static final String OBJ_PATH = "models/trophy/su47/su47.obj";
    private static final String MTL_PATH = "models/trophy/su47/su47.mtl";

    // Scale and centering constants (from vertex analysis)
    private static final float SCALE  = 0.18f;  // 1 / 5.19 ≈ 0.193, slightly smaller for margin
    private static final float OFF_X  = 0f;      // X is already centered (±2.13)
    private static final float OFF_Y  = -0.013f; // lift model to y=0
    private static final float OFF_Z  = -1.689f; // center Z ((-0.906+4.284)/2 = 1.689)

    public SU47TrophyRenderer(BlockEntityRendererProvider.Context ctx) {}

    // ─── Lazy load ────────────────────────────────────────────────────────────

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try { loadMtl(); } catch (Exception ignored) {}
        try { loadObj(); } catch (Exception ignored) {}
        // Pre-register textures so entityCutoutNoCull can bind them during rendering.
        // Without this, TextureManager may not find them during render phase.
        var tm = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation tex : MTL_MAP.values()) {
            tm.register(tex, new SimpleTexture(tex));
        }
    }

    private static void loadMtl() throws Exception {
        var res = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, MTL_PATH))
                .orElse(null);
        if (res == null) return;
        String curMat = null;
        try (var r = new BufferedReader(new InputStreamReader(res.open()))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("newmtl ")) {
                    curMat = line.substring(7).trim();
                } else if (line.startsWith("map_Kd ") && curMat != null) {
                    String tex = line.substring(7).trim();
                    // strip path segments and clean name
                    tex = tex.replaceAll(".*[\\\\/]", "")   // last path segment only
                             .replace(" (2).png", ".png")
                             .replace(" ", "_");
                    if (!tex.endsWith(".png")) tex += ".png";
                    // entityCutoutNoCull needs full texture path: textures/<path>
                    MTL_MAP.put(curMat, ResourceLocation.fromNamespaceAndPath(
                            RefStrings.MODID, "textures/block/trophy/su47/" + tex));
                }
            }
        }
    }

    private static void loadObj() throws Exception {
        var res = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, OBJ_PATH))
                .orElse(null);
        if (res == null) return;

        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nrm = new ArrayList<>();
        String curMat = "default";

        try (var r = new BufferedReader(new InputStreamReader(res.open()))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ") && !line.startsWith("vt") && !line.startsWith("vn")) {
                    String[] p = line.split("\\s+");
                    pos.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("vt ")) {
                    String[] p = line.split("\\s+");
                    uv.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
                } else if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    nrm.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
                } else if (line.startsWith("usemtl ")) {
                    curMat = line.substring(7).trim();
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
                        vs.add(new float[]{p[0], p[1], p[2], u, v2, nx, ny, nz});
                    }
                    List<float[]> tris = OBJ_DATA.computeIfAbsent(curMat, k -> new ArrayList<>());
                    for (int i = 1; i < vs.size() - 1; i++) {
                        float[] t = new float[24];
                        System.arraycopy(vs.get(0), 0, t, 0, 8);
                        System.arraycopy(vs.get(i), 0, t, 8, 8);
                        System.arraycopy(vs.get(i + 1), 0, t, 16, 8);
                        tris.add(t);
                    }
                }
            }
        }
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    @Override
    public void render(SU47TrophyBlockEntity be, float pt, PoseStack ps,
                       MultiBufferSource buf, int packedLight, int packedOverlay) {
        ensureLoaded();
        if (OBJ_DATA.isEmpty()) return;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);           // center in block (X,Z)
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(OFF_X, OFF_Y, OFF_Z);   // align model

        Matrix4f m = ps.last().pose();

        for (var entry : OBJ_DATA.entrySet()) {
            ResourceLocation tex = MTL_MAP.getOrDefault(entry.getKey(),
                    ResourceLocation.withDefaultNamespace("block/iron_block"));
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(
                    tex != null ? tex : ResourceLocation.withDefaultNamespace("textures/block/iron_block.png")));
            for (float[] tri : entry.getValue()) {
                for (int pass = 0; pass < 4; pass++) {
                    int base = Math.min(pass, 2) * 8;
                    float x = tri[base], y = tri[base+1], z = tri[base+2];
                    float u = tri[base+3], v = 1f - tri[base+4]; // flip V
                    float nx = tri[base+5], ny = tri[base+6], nz = tri[base+7];
                    vc.vertex(m, x, y, z).color(1f,1f,1f,1f)
                      .uv(u, v).overlayCoords(packedOverlay).uv2(packedLight)
                      .normal(nx, ny, nz).endVertex();
                }
            }
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(SU47TrophyBlockEntity be) { return false; }
    @Override public int getViewDistance() { return 64; }
}
