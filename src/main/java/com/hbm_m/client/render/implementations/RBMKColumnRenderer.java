package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;
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
import net.minecraft.util.Mth;
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
public class RBMKColumnRenderer<T extends RBMKColumnBlockEntity> implements BlockEntityRenderer<T> {

    // ─── Caches ──────────────────────────────────────────────────────────────

    /** OBJ geometry cache: resourcePath → (groupName → triangles). */
    private static final Map<String, Map<String, List<float[]>>> OBJ_CACHE   = new HashMap<>();
    /** Texture sprite cache. */
    private static final Map<String, TextureAtlasSprite>         SPRITE_CACHE = new HashMap<>();

    // ─── Constructor ─────────────────────────────────────────────────────────

    public RBMKColumnRenderer(BlockEntityRendererProvider.Context ctx) {}

    // ─── Sprite helpers ───────────────────────────────────────────────────────

    static TextureAtlasSprite sprite(String ns, String path) {
        return SPRITE_CACHE.computeIfAbsent(ns + ":" + path, k ->
                Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(ResourceLocation.fromNamespaceAndPath(ns, path)));
    }

    // ─── OBJ loading ─────────────────────────────────────────────────────────

    static Map<String, List<float[]>> getObj(String resourcePath) {
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
        // Every RBMK column type ships 3 top variants - plain (no lid), "_cover" (concrete lid)
        // and "_glass" (glass lid) - but the renderer only ever loaded the plain one, so every
        // column looked the same regardless of whether it actually had a lid on. Matches the
        // reference screenshots' distinct capped/vented tops per lid state.
        // Must gate on hasLid() (overridden to always be false for control rods, which never
        // have a real lid - the rod cap stands in for it) rather than the raw getLidState()
        // field, which still defaults to 1 ("has a concrete lid") even for types that can never
        // actually have one: reading it directly sent every control rod, moderated or not,
        // looking for a "..._cover_top" texture that doesn't exist for that type, rendering as
        // the classic missing-texture magenta/black checkerboard on the whole top face.
        String topSuffix;
        if (!be.hasLid()) {
            topSuffix = "_top";
        } else {
            topSuffix = be.getLidState() == 2 ? "_glass_top" : "_cover_top";
        }
        TextureAtlasSprite topSprite  = sprite(RefStrings.MODID, "block/rbmk/" + prefix + topSuffix);

        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        int height = RBMKDials.COLUMN_HEIGHT + 1;

        // ── For all columns: flat textured sides ──────────────────────────────
        Matrix4f m = ps.last().pose();
        for (int y = 0; y < height; y++)
            flatSides(vc, m, y, sideSprite, packedLight, packedOverlay);

        // Top face: flat for non-fuel; OBJ provides it for fuel channel
        if (!isFuel)
            flatTop(vc, m, height, topSprite, packedLight, packedOverlay);

        // ── Pipe-corner stub decoration ───────────────────────────────────────
        // 1:1 port of RenderRBMKControl (original): CONTROL/BOILER/HEATER all share this
        // renderer and, when they don't currently have a lid rendered on top, show 4 small
        // pipe-corner stubs instead. Control rods never have a lid at all (see
        // RBMKControlBlockEntity#hasLid), so they always get the stubs; boiler/heater only get
        // them when unlidded - matches the original's mutually-exclusive lid-vs-pipe-stub check
        // (only boiler/heater in the original special-case the lid swap at all).
        ColumnType consoleType = be.getConsoleType();
        boolean showsPipeStub = consoleType == ColumnType.CONTROL
                || ((consoleType == ColumnType.BOILER || consoleType == ColumnType.HEATER) && !be.hasLid());
        if (showsPipeStub) {
            TextureAtlasSprite pipeTop  = sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_pipe_top");
            TextureAtlasSprite pipeSide = sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_pipe_side");
            float y0 = height, y1 = height + 0.3f;
            float s0 = 0.15f, s1 = 0.85f;
            quad(vc, m, s1,y0,s0, s0,y0,s0, s0,y1,s0, s1,y1,s0,  0, 0,-1, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y0,s1, s1,y0,s1, s1,y1,s1, s0,y1,s1,  0, 0, 1, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y0,s0, s0,y0,s1, s0,y1,s1, s0,y1,s0, -1, 0, 0, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s1,y0,s1, s1,y0,s0, s1,y1,s0, s1,y1,s1,  1, 0, 0, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y1,s0, s0,y1,s1, s1,y1,s1, s1,y1,s0,  0, 1, 0, pipeTop,  packedLight, packedOverlay, 1,1,1);
        }

        // ── Control rod only: animated rod-cap sliding with insertion level ───
        if (be instanceof RBMKControlBlockEntity ctrl) {
            renderControlRod(ctrl, pt, ps, buf, height, packedLight, packedOverlay);
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

                // 1:1 with the original's RenderRBMKFuelChannel Cherenkov glow: a faint additive
                // cyan haze inside the channel once flux exceeds 5, using the rod's own last
                // published flux (the live accumulator is zeroed every tick right after use, so
                // it would almost always read 0 by the time a frame renders).
                if (rod.lastFluxQuantity > 5) {
                    renderCherenkovGlow(ps, buf, height);
                }
            }
        }
    }

    // ─── Cherenkov glow ──────────────────────────────────────────────────────

    private static void renderCherenkovGlow(PoseStack ps, MultiBufferSource buf, int height) {
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        ps.pushPose();
        ps.translate(0.5, 0.75, 0.5);
        Matrix4f m = ps.last().pose();
        for (float j = 0; j <= height; j += 0.25f) {
            vc.vertex(m, -0.5f, j, -0.5f).color(0.4f, 0.9f, 1.0f, 0.1f).endVertex();
            vc.vertex(m, -0.5f, j,  0.5f).color(0.4f, 0.9f, 1.0f, 0.1f).endVertex();
            vc.vertex(m,  0.5f, j,  0.5f).color(0.4f, 0.9f, 1.0f, 0.1f).endVertex();
            vc.vertex(m,  0.5f, j, -0.5f).color(0.4f, 0.9f, 1.0f, 0.1f).endVertex();
        }
        ps.popPose();
    }

    // ─── Control rod cap animation ───────────────────────────────────────────

    /**
     * 1:1 port of the original's {@code RenderRBMKControlRod}: the rod-cap mesh ({@code Lid}
     * group of {@code rbmk_rods.obj}, already shipped as an asset but never used by this port's
     * renderer) slides vertically with the control rod's current insertion {@code level},
     * interpolated between ticks via {@code lastLevel}/{@code level} exactly like the original's
     * {@code control.lastLevel + (control.level - control.lastLevel) * partialTick}. Without
     * this, every control rod variant (including moderated ones) rendered as a static column
     * with no visible extension/retraction at all.
     */
    private static void renderControlRod(RBMKControlBlockEntity ctrl, float pt, PoseStack ps,
                                          MultiBufferSource buf, int height, int light, int overlay) {
        Map<String, List<float[]>> obj = getObj("models/rbmk/models/rbmk_rods.obj");
        List<float[]> lid = obj.get("Lid");
        if (lid == null) return;

        String capPrefix;
        if (ctrl instanceof RBMKControlManualBlockEntity manual) capPrefix = manual.getCapTexture();
        else if (ctrl instanceof RBMKControlAutoBlockEntity)     capPrefix = "rbmk_control_auto";
        else                                                     capPrefix = "rbmk_control";

        TextureAtlasSprite capSprite = sprite(RefStrings.MODID, "block/rbmk/" + capPrefix);
        double level = Mth.lerp(pt, ctrl.lastLevel, ctrl.level);

        // 1:1 with the original's RenderRBMKControlRod: anchor = `y + offset`, where `offset` is
        // the height of the contiguous same-block-type stack above the base (base + regular
        // dummies + the extra lid-slot dummy, all one Block instance in the original's
        // BlockDummyable multiblock) - i.e. the column's full visual height, which is exactly
        // our `height` (COLUMN_HEIGHT+1), not `height-1`. The earlier `height-1` adjustment was
        // a visual guess made before this source file was available; the previous "floating"
        // symptom it was chasing was most likely the level-defaults-to-1.0 bug fixed alongside
        // it, not this anchor.
        ps.pushPose();
        ps.translate(0.5, height + level, 0.5);
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        renderObjGroup(vc, ps.last().pose(), lid, capSprite, 1, 1, 1, light, overlay);
        ps.popPose();
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
        vc.vertex(m, x0, y0, z0).color(r,g,b,1f).uv(u0,v1).overlayCoords(overlay).uv2(light).normal(nx,ny,nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r,g,b,1f).uv(u1,v1).overlayCoords(overlay).uv2(light).normal(nx,ny,nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r,g,b,1f).uv(u1,v0).overlayCoords(overlay).uv2(light).normal(nx,ny,nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r,g,b,1f).uv(u0,v0).overlayCoords(overlay).uv2(light).normal(nx,ny,nz).endVertex();
    }

    // ─── OBJ geometry rendering ───────────────────────────────────────────────

    static void renderObjGroup(VertexConsumer vc, Matrix4f m,
                                        List<float[]> triangles, TextureAtlasSprite sprite,
                                        float r, float g, float b, int light, int overlay) {
        if (triangles == null || sprite == null) return;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        for (float[] tri : triangles) {
            for (int pass = 0; pass < 4; pass++) {  // 4th is duplicate of 3rd (degenerate quad)
                int base = Math.min(pass, 2) * 8;
                float x  = tri[base], y = tri[base+1], z = tri[base+2];
                float u  = tri[base+3], v = tri[base+4];
                float nx = tri[base+5], ny = tri[base+6], nz = tri[base+7];
                float au = u0 + u * (u1 - u0);
                float av = v0 + (1f - v) * (v1 - v0); // V flipped
                vc.vertex(m, x, y, z).color(r,g,b,1f).uv(au,av).overlayCoords(overlay).uv2(light).normal(nx,ny,nz).endVertex();
            }
        }
    }

    // ─── Culling ─────────────────────────────────────────────────────────────

    // RBMKColumnBlockEntity.getRenderBoundingBox() already expands the AABB to cover the
    // full column height, so vanilla frustum culling against that box is correct on its own -
    // forcing shouldRenderOffScreen(true) with a 96-block view distance defeated all culling
    // and made every column in a large reactor render its full geometry every frame regardless
    // of whether it was on-screen or in range, causing severe lag on big reactors. Falling back
    // to the BlockEntityRenderer defaults (frustum-culled, 64-block view distance) fixes that.
}
