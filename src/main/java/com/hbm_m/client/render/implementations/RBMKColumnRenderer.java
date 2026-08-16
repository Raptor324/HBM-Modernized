package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Generic BESR for all RBMK column blocks.
 *
 * All columns render flat textured quads for their 4 side faces (giving the
 * tall panel look). The FUEL CHANNEL additionally loads {@code rbmk_element.obj}
 * and renders its {@code Inner} + {@code Cap} groups per section, creating the
 * characteristic hollow octagonal channel appearance visible from the top.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class RBMKColumnRenderer<T extends RBMKColumnBlockEntity> implements com.hbm_m.client.render.HbmBerBounds<T> {

    // ─── Caches ──────────────────────────────────────────────────────────────

    /** OBJ geometry cache: resourcePath → (groupName → triangles). */
    private static final Map<String, Map<String, List<float[]>>> OBJ_CACHE   = new HashMap<>();
    /** Texture sprite cache. */
    private static final Map<String, TextureAtlasSprite>         SPRITE_CACHE = new HashMap<>();

    // ─── Constructor ─────────────────────────────────────────────────────────

    public RBMKColumnRenderer(BlockEntityRendererProvider.Context ctx) {}

    // ─── Sprite helpers ───────────────────────────────────────────────────────

    private static TextureAtlasSprite sprite(String ns, String path) {
        return SPRITE_CACHE.computeIfAbsent(ns + ":" + path, k ->
                Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(ResourceLocation.fromNamespaceAndPath(ns, path)));
    }

    // ─── OBJ loading ─────────────────────────────────────────────────────────

    private static Map<String, List<float[]>> getObj(String resourcePath) {
        return OBJ_CACHE.computeIfAbsent(resourcePath, path -> {
            try {
                var res = Minecraft.getInstance().getResourceManager()
                        .getResource(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path))
                        .orElse(null);
                if (res == null) return Map.of();
                return parseObj(new BufferedReader(new InputStreamReader(res.open())));
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    private static Map<String, List<float[]>> parseObj(BufferedReader reader) throws Exception {
        List<float[]> pos = new ArrayList<>(), uv = new ArrayList<>(), nrm = new ArrayList<>();
        Map<String, List<float[]>> result = new LinkedHashMap<>();
        String cur = "default";
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("o ")) {
                cur = line.substring(2).trim();
            } else if (line.startsWith("v ") && !line.startsWith("vt") && !line.startsWith("vn")) {
                String[] p = line.split("\\s+");
                pos.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
            } else if (line.startsWith("vt ")) {
                String[] p = line.split("\\s+");
                uv.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])});
            } else if (line.startsWith("vn ")) {
                String[] p = line.split("\\s+");
                nrm.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
            } else if (line.startsWith("f ")) {
                String[] verts = line.substring(2).trim().split("\\s+");
                List<float[]> vs = new ArrayList<>();
                for (String vert : verts) {
                    String[] idx = vert.split("/");
                    int vi = Integer.parseInt(idx[0]) - 1;
                    int ti = idx.length > 1 && !idx[1].isEmpty() ? Integer.parseInt(idx[1]) - 1 : -1;
                    int ni = idx.length > 2 && !idx[2].isEmpty() ? Integer.parseInt(idx[2]) - 1 : -1;
                    float[] p = pos.get(vi);
                    float u  = ti >= 0 ? uv.get(ti)[0] : 0, v = ti >= 0 ? uv.get(ti)[1] : 0;
                    float nx = ni >= 0 ? nrm.get(ni)[0] : 0;
                    float ny = ni >= 0 ? nrm.get(ni)[1] : 1;
                    float nz = ni >= 0 ? nrm.get(ni)[2] : 0;
                    vs.add(new float[]{p[0], p[1], p[2], u, v, nx, ny, nz});
                }
                List<float[]> tris = result.computeIfAbsent(cur, k -> new ArrayList<>());
                for (int i = 1; i < vs.size() - 1; i++) {
                    float[] t = new float[24];
                    System.arraycopy(vs.get(0), 0, t, 0, 8);
                    System.arraycopy(vs.get(i), 0, t, 8, 8);
                    System.arraycopy(vs.get(i + 1), 0, t, 16, 8);
                    tris.add(t);
                }
            }
        }
        return result;
    }

    // ─── Render ──────────────────────────────────────────────────────────────

    @Override
    public void render(T be, float pt, PoseStack ps, MultiBufferSource buf,
                       int packedLight, int packedOverlay) {

        boolean isFuel = be.getConsoleType() == ColumnType.FUEL;
        String prefix  = be.getRenderTexturePrefix();
        TextureAtlasSprite sideSprite = sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_side");
        TextureAtlasSprite topSprite  = sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_top");

        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        int height = RBMKDials.COLUMN_HEIGHT + 1;

        // ── For all columns: flat textured sides ──────────────────────────────
        Matrix4f m = ps.last().pose();
        for (int y = 0; y < height; y++)
            flatSides(vc, m, y, sideSprite, packedLight, packedOverlay);

        boolean isBoiler = be.getConsoleType() == ColumnType.BOILER;

        // Top face: flat for non-fuel/non-boiler; OBJ provides it for fuel channel
        if (!isFuel)
            flatTop(vc, m, height, topSprite, packedLight, packedOverlay);

        // ── Boiler: ONE central pipe stub on top ─────────────────────────────
        // (place 4 boiler blocks in a 2×2 → 4 separate stubs, matching the original)
        if (isBoiler) {
            TextureAtlasSprite pipeTop  = sprite(RefStrings.MODID, "block/rbmk/rbmk_boiler_pipe_top");
            TextureAtlasSprite pipeSide = sprite(RefStrings.MODID, "block/rbmk/rbmk_boiler_pipe_side");
            float y0 = height, y1 = height + 0.3f;
            float s0 = 0.15f, s1 = 0.85f;
            quad(vc, m, s1,y0,s0, s0,y0,s0, s0,y1,s0, s1,y1,s0,  0, 0,-1, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y0,s1, s1,y0,s1, s1,y1,s1, s0,y1,s1,  0, 0, 1, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y0,s0, s0,y0,s1, s0,y1,s1, s0,y1,s0, -1, 0, 0, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s1,y0,s1, s1,y0,s0, s1,y1,s0, s1,y1,s1,  1, 0, 0, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y1,s0, s0,y1,s1, s1,y1,s1, s1,y1,s0,  0, 1, 0, pipeTop,  packedLight, packedOverlay, 1,1,1);
        }

        // ── Fuel channel only: OBJ inner geometry (hollow look) ───────────────
        if (isFuel) {
            TextureAtlasSprite innerSprite = sprite(RefStrings.MODID, "block/rbmk/rbmk_element_inner");
            TextureAtlasSprite capSprite   = topSprite;
            Map<String, List<float[]>> obj = getObj("models/rbmk/models/rbmk_element.obj");

            ps.pushPose();
            ps.translate(0.5, 0, 0.5); // OBJ is centered at origin
            for (int y = 0; y < height; y++) {
                Matrix4f om = ps.last().pose();
                renderObjGroup(vc, om, obj.get("Inner"), innerSprite, 1, 1, 1, packedLight, packedOverlay);
                renderObjGroup(vc, om, obj.get("Cap"),   capSprite,   1, 1, 1, packedLight, packedOverlay);
                ps.translate(0, 1, 0);
            }
            ps.popPose();

            // Fuel rod geometry (only when fuel is loaded)
            if (be instanceof RBMKRodBlockEntity rod && rod.hasRod) {
                int color = rod.rodColor;
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >>  8) & 0xFF) / 255f;
                float b = ( color        & 0xFF) / 255f;
                Map<String, List<float[]>> rodsObj = getObj("models/rbmk/models/rbmk_element_rods.obj");
                TextureAtlasSprite fuelSprite = sprite(RefStrings.MODID, "block/rbmk/rbmk_element_fuel");
                ps.pushPose();
                ps.translate(0.5, 0, 0.5);
                renderObjGroup(vc, ps.last().pose(), rodsObj.get("Rods"), fuelSprite, r, g, b, packedLight, packedOverlay);
                ps.popPose();
            }
        }
    }

    // ─── Flat geometry ────────────────────────────────────────────────────────

    private static void flatSides(VertexConsumer vc, Matrix4f m, int y,
                                   TextureAtlasSprite s, int light, int overlay) {
        float y0 = y, y1 = y + 1;
        quad(vc, m, 1,y0,0, 0,y0,0, 0,y1,0, 1,y1,0,  0, 0,-1, s, light, overlay, 1,1,1); // N
        quad(vc, m, 0,y0,1, 1,y0,1, 1,y1,1, 0,y1,1,  0, 0, 1, s, light, overlay, 1,1,1); // S
        quad(vc, m, 0,y0,0, 0,y0,1, 0,y1,1, 0,y1,0, -1, 0, 0, s, light, overlay, 1,1,1); // W
        quad(vc, m, 1,y0,1, 1,y0,0, 1,y1,0, 1,y1,1,  1, 0, 0, s, light, overlay, 1,1,1); // E
    }

    private static void flatTop(VertexConsumer vc, Matrix4f m, int y,
                                 TextureAtlasSprite s, int light, int overlay) {
        quad(vc, m, 0,y,0, 0,y,1, 1,y,1, 1,y,0,  0, 1, 0, s, light, overlay, 1,1,1);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float x2, float y2, float z2, float x3, float y3, float z3,
                              float nx, float ny, float nz,
                              TextureAtlasSprite s, int light, int overlay,
                              float r, float g, float b) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        int ir = (int) (r * 255), ig = (int) (g * 255), ib = (int) (b * 255);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x0, y0, z0, ir, ig, ib, 255, u0, v1, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x1, y1, z1, ir, ig, ib, 255, u1, v1, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x2, y2, z2, ir, ig, ib, 255, u1, v0, overlay, light, nx, ny, nz);
        com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x3, y3, z3, ir, ig, ib, 255, u0, v0, overlay, light, nx, ny, nz);
    }

    // ─── OBJ geometry rendering ───────────────────────────────────────────────

    private static void renderObjGroup(VertexConsumer vc, Matrix4f m,
                                        List<float[]> triangles, TextureAtlasSprite sprite,
                                        float r, float g, float b, int light, int overlay) {
        if (triangles == null || sprite == null) return;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        int ir = (int) (r * 255), ig = (int) (g * 255), ib = (int) (b * 255);
        for (float[] tri : triangles) {
            for (int pass = 0; pass < 4; pass++) {  // 4th is duplicate of 3rd (degenerate quad)
                int base = Math.min(pass, 2) * 8;
                float x  = tri[base], y = tri[base+1], z = tri[base+2];
                float u  = tri[base+3], v = tri[base+4];
                float nx = tri[base+5], ny = tri[base+6], nz = tri[base+7];
                float au = u0 + u * (u1 - u0);
                float av = v0 + (1f - v) * (v1 - v0); // V flipped
                com.hbm_m.platform.RenderHooks.vertexFull(vc, m, x, y, z, ir, ig, ib, 255, au, av, overlay, light, nx, ny, nz);
            }
        }
    }

    // ─── Culling ─────────────────────────────────────────────────────────────

    @Override public boolean shouldRenderOffScreen(T be) { return true; }
    @Override public int getViewDistance() { return 96; }
}
