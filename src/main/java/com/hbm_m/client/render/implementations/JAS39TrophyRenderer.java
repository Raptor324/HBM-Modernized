package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.JAS39TrophyBlockEntity;
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

/**
 * Same OBJ/MTL-loading trophy pattern as {@link SU47TrophyRenderer}, applied to the Saab JAS 39
 * Gripen E model.
 * <p>
 * PERFORMANCE-NOTIZ: Dieses "free" heruntergeladene Modell hat ~29.500 Dreiecke (Su-47 zum
 * Vergleich: ~4.100) - eine erste GPU-VertexBuffer-Bake-Optimierung fuehrte zu korrupter Geometrie
 * (falsches Rendering) und wurde zurueckgerollt. Dieser Renderer sendet daher wie beim Original
 * jedes Dreieck jeden Frame per {@link VertexConsumer} neu ein (korrekt, aber bei diesem
 * hochaufloesenden Modell spuerbar teurer als bei Su-47) - eine sichere GPU-Cache-Loesung braeuchte
 * mehr Test-/Debug-Zeit als hier verfuegbar war.
 */
public class JAS39TrophyRenderer implements BlockEntityRenderer<JAS39TrophyBlockEntity> {

    private static final Map<String, List<float[]>> OBJ_DATA = new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> MTL_MAP = new HashMap<>();
    private static boolean loaded = false;

    private static final String OBJ_PATH = "models/trophy/jas39/saab_jas_39_gripen_-_fighter_jet_-_free.obj";
    private static final String MTL_PATH = "models/trophy/jas39/saab_jas_39_gripen_-_fighter_jet_-_free.mtl";

    // Scale and centering constants (from vertex analysis, same approach as SU47TrophyRenderer)
    private static final float SCALE = 0.013f;    // largest raw dimension (X, ~152.25) * 0.013 ≈ 1.98 blocks
    private static final float OFF_X = -31.1195f; // -center X ((-45.006+107.245)/2)
    private static final float OFF_Y = 20.1322f;  // -min Y, lifts model to y=0
    private static final float OFF_Z = 0f;        // Z already centered (±42.93)

    public JAS39TrophyRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try { loadMtl(); } catch (Exception ignored) {}
        try { loadObj(); } catch (Exception ignored) {}
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
                    tex = tex.replaceAll(".*[\\\\/]", "").replace(" ", "_");
                    if (!tex.endsWith(".png")) tex += ".png";
                    MTL_MAP.put(curMat, ResourceLocation.fromNamespaceAndPath(
                            RefStrings.MODID, "textures/block/trophy/jas39/" + tex));
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

    @Override
    public void render(JAS39TrophyBlockEntity be, float pt, PoseStack ps,
                       MultiBufferSource buf, int packedLight, int packedOverlay) {
        ensureLoaded();
        if (OBJ_DATA.isEmpty()) return;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.scale(SCALE, SCALE, SCALE);
        ps.translate(OFF_X, OFF_Y, OFF_Z);

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
                    float u = tri[base+3], v = 1f - tri[base+4];
                    float nx = tri[base+5], ny = tri[base+6], nz = tri[base+7];
                    vc.vertex(m, x, y, z).color(1f,1f,1f,1f)
                      .uv(u, v).overlayCoords(packedOverlay).uv2(packedLight)
                      .normal(nx, ny, nz).endVertex();
                }
            }
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(JAS39TrophyBlockEntity be) { return false; }
    @Override public int getViewDistance() { return 64; }
}
