package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKAbsorberBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKBlankBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKBoilerBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity.ColumnType;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKCoolerBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKHeaterBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKModeratorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKPanelBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKReflectorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKStorageBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

/**
 * Nucleus spec for all RBMK column blocks (the 13 column BEs: ROD / BLANK /
 * ABSORBER / REFLECTOR / COOLER / BOILER / HEATER / MODERATOR / OUTGASSER /
 * STORAGE / CONTROL / CONTROL_AUTO / PANEL).
 *
 * <p>The columns carry no baked machine-parts geometry: every column type draws
 * flat textured quads for its 4 side faces (giving the tall panel look) and the
 * FUEL CHANNEL additionally draws the {@code Inner} + {@code Cap} groups of
 * {@code rbmk_element.obj} per section, creating the characteristic hollow
 * octagonal channel appearance visible from the top. All of that lives in the
 * {@link MachineRenderHook}, which the engine runs on the immediate path with
 * Nucleus culling / distance fade / Iris tiers applied around it. The spec's
 * block transform is IDENTITY: the hook's geometry is authored in block-local
 * coordinates with the origin at the block corner (x/z in [0,1]), exactly like
 * the legacy {@code RBMKColumnRenderer} drew it - the default
 * center+facing transform would displace it by half a block. RBMK columns have
 * no facing anyway (rotationally symmetric panels).</p>
 *
 * <p>One spec per BlockEntityType (the factory keys on the BE type); all 13
 * share the same render logic, dispatched on
 * {@link RBMKColumnBlockEntity#getConsoleType()} and {@code instanceof} checks
 * inside the hook, 1:1 with the legacy renderer.</p>
 */
public final class MachineRBMKColumnRenderer {

    public static void register() {
        column("rbmk_column_rod",         ModBlockEntities.RBMK_ROD_BE,         RBMKRodBlockEntity.class);
        column("rbmk_column_blank",       ModBlockEntities.RBMK_BLANK_BE,       RBMKBlankBlockEntity.class);
        column("rbmk_column_absorber",    ModBlockEntities.RBMK_ABSORBER_BE,    RBMKAbsorberBlockEntity.class);
        column("rbmk_column_reflector",   ModBlockEntities.RBMK_REFLECTOR_BE,   RBMKReflectorBlockEntity.class);
        column("rbmk_column_cooler",      ModBlockEntities.RBMK_COOLER_BE,      RBMKCoolerBlockEntity.class);
        column("rbmk_column_boiler",      ModBlockEntities.RBMK_BOILER_BE,      RBMKBoilerBlockEntity.class);
        column("rbmk_column_heater",      ModBlockEntities.RBMK_HEATER_BE,      RBMKHeaterBlockEntity.class);
        column("rbmk_column_moderator",   ModBlockEntities.RBMK_MODERATOR_BE,   RBMKModeratorBlockEntity.class);
        column("rbmk_column_outgasser",   ModBlockEntities.RBMK_OUTGASSER_BE,   RBMKOutgasserBlockEntity.class);
        column("rbmk_column_storage",     ModBlockEntities.RBMK_STORAGE_BE,     RBMKStorageBlockEntity.class);
        column("rbmk_column_control",     ModBlockEntities.RBMK_CONTROL_BE,     RBMKControlManualBlockEntity.class);
        column("rbmk_column_control_auto",ModBlockEntities.RBMK_CONTROL_AUTO_BE,RBMKControlAutoBlockEntity.class);
        column("rbmk_column_panel",       ModBlockEntities.RBMK_PANEL_BE,       RBMKPanelBlockEntity.class);
    }

    /**
     * One spec per column BE type. The identity {@code blockTransform} keeps the
     * hook stack at the block-corner origin (see the class comment); the hook is
     * never empty (the column body always draws), so {@code mayDrawContent} is
     * constant true. 64 blocks matches the vanilla BER default view distance the
     * legacy renderer ran at.
     */
    private static <T extends RBMKColumnBlockEntity> void column(
            String id,
            dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> type,
            Class<T> cls) {
        MachineRenderers.machine(id, type.get(), cls)
            .blockTransform((be, animator) -> { /* identity: block-local geometry */ })
            .hook(new MachineRenderHook<T>() {
                @Override
                public void render(T be, float pt, PoseStack ps, MultiBufferSource buf,
                                   int light, int overlay, com.hbm_m.client.render.machine.MachineRenderApi api) {
                    renderColumn(be, pt, ps, buf, light, overlay);
                }

                @Override
                public boolean mayDrawContent(T be) {
                    return true;
                }
            })
            .viewDistance(64)
            .register();
    }

    // ─── Render (1:1 from the legacy RBMKColumnRenderer.render) ─────────────

    private static void renderColumn(RBMKColumnBlockEntity be, float pt, PoseStack ps,
                                     MultiBufferSource buf, int packedLight, int packedOverlay) {

        boolean isFuel = be.getConsoleType() == ColumnType.FUEL;
        String prefix  = be.getRenderTexturePrefix();
        TextureAtlasSprite sideSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_side");
        // Every RBMK column type ships 3 top variants - plain (no lid), "_cover" (concrete lid)
        // and "_glass" (glass lid) - but the renderer only ever loaded the plain one, so every
        // column looked the same regardless of whether it actually had a lid on. Matches the
        // reference screenshots' distinct capped/vented tops per lid state.
        // Must gate on hasLid() && !hasOwnLid() rather than the raw getLidState() field, which
        // still defaults to 1 ("has a concrete lid") even for types that can never actually have
        // one: reading it directly sent every control rod, moderated or not, looking for a
        // "..._cover_top" texture that doesn't exist for that type, rendering as the classic
        // missing-texture magenta/black checkerboard on the whole top face.
        // The column body always wears its plain "_top". A lid is NOT a texture swap on that face -
        // it is a real 0.25-block plate drawn on top of the column further down (see the lid box
        // below), which is what the "_cover_side"/"_glass_side" textures are for: they are the
        // plate's rim, and nothing in this renderer had ever used them. Swapping only the top face
        // also left a lidded column visibly flat next to a fully-inserted control rod, whose cap
        // mesh stands 0.25 blocks proud of the column.
        TextureAtlasSprite topSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_top");
        String lidSuffix = be.getLidState() == 2 ? "_glass" : "_cover";

        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        int height = RBMKDials.COLUMN_HEIGHT + 1;

        // ── For all columns: flat textured sides ──────────────────────────────
        Matrix4f m = ps.last().pose();
        for (int y = 0; y < height; y++)
            flatSides(vc, m, y, sideSprite, packedLight, packedOverlay);

        // Top face: flat for non-fuel; OBJ provides it for fuel channel
        if (!isFuel)
            flatTop(vc, m, height, topSprite, packedLight, packedOverlay);

        // Bottom face. The renderer draws the whole column itself (the block model is skipped via
        // ENTITYBLOCK_ANIMATED), so without this the column is see-through from below - visible as
        // a hollow shell when standing under a reactor. Every column type uses its plain "_top"
        // texture down here, never a lid variant, matching the original block models' "down" face.
        flatBottom(vc, m, 0, RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_top"),
                packedLight, packedOverlay);

        // ── Pipe-corner stub decoration ───────────────────────────────────────
        // CE draws four small pipe stubs on top of a CONTROL/BOILER/HEATER column whenever that
        // column has no lid (RBMKControlBakedModel.buildWorldQuads: lid box, or else addPipes).
        //
        // DELIBERATE DEVIATION FROM CE, on request: the stubs are suppressed for the steam boiler
        // and for control rods, which are the two the player actually sees a lot of - a control rod
        // never has a lid, so it wore them permanently, and since columns are now placed lidless
        // the boiler grew them too. The heat exchanger keeps CE's behaviour. Flip the two flags
        // below to restore full CE parity.
        final boolean CE_STUBS_ON_CONTROL = false;
        final boolean CE_STUBS_ON_BOILER  = false;

        ColumnType consoleType = be.getConsoleType();
        boolean showsPipeStub =
                   (CE_STUBS_ON_CONTROL && consoleType == ColumnType.CONTROL)
                || (CE_STUBS_ON_BOILER  && consoleType == ColumnType.BOILER && !be.hasLid())
                || (consoleType == ColumnType.HEATER && !be.hasLid());
        if (showsPipeStub) {
            TextureAtlasSprite pipeTop  = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_pipe_top");
            TextureAtlasSprite pipeSide = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + "_pipe_side");
            float y0 = height, y1 = height + 0.3f;
            float s0 = 0.15f, s1 = 0.85f;
            quad(vc, m, s1,y0,s0, s0,y0,s0, s0,y1,s0, s1,y1,s0,  0, 0,-1, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y0,s1, s1,y0,s1, s1,y1,s1, s0,y1,s1,  0, 0, 1, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y0,s0, s0,y0,s1, s0,y1,s1, s0,y1,s0, -1, 0, 0, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s1,y0,s1, s1,y0,s0, s1,y1,s0, s1,y1,s1,  1, 0, 0, pipeSide, packedLight, packedOverlay, 1,1,1);
            quad(vc, m, s0,y1,s0, s0,y1,s1, s1,y1,s1, s1,y1,s0,  0, 1, 0, pipeTop,  packedLight, packedOverlay, 1,1,1);
        }

        // ── Lid: a real plate sitting on top of the column ────────────────────
        // Geometry taken from the lid's own mesh (models/rbmk/models/deb_lid.obj, the debris entity
        // that pops off when a lid is knocked loose): a full-width plate 0.25 blocks thick. Placed
        // with its underside on the column's top face, its top ends up at height + 0.25 - exactly
        // level with the top of a control rod cap at level 0, which is how the two read as one
        // continuous reactor floor.
        // hasOwnLid() types (control rods, panel devices) are logically lidded - hasLid() is true
        // for them so they do not leak neutron flux - but they ship no "_cover_*"/"_glass_*"
        // textures, so the generic plate must not be drawn on them. Their own cap mesh stands in
        // for it. Matches RBMKBase.getIcon's "if(!hasOwnLid())" guard in the original.
        if (be.hasLid() && !be.hasOwnLid()) {
            TextureAtlasSprite lidTop  = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + lidSuffix + "_top");
            TextureAtlasSprite lidSide = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + prefix + lidSuffix + "_side");
            // A glass lid has to go on RenderType.cutout(): its textures are mostly hole (191 of 256
            // pixels on the top face, 220 of 256 on the rim) with strictly binary alpha, and
            // RenderType.solid() ignores alpha outright - which drew the glass lid as an opaque
            // slab you could not see the channel through. Concrete lids are fully opaque and stay
            // on the solid layer.
            VertexConsumer lvc = be.getLidState() == 2 ? buf.getBuffer(RenderType.cutout()) : vc;
            float ly0 = height, ly1 = height + 0.25f;
            quad(lvc, m, 1,ly0,0, 0,ly0,0, 0,ly1,0, 1,ly1,0,  0, 0,-1, lidSide, packedLight, packedOverlay, 1,1,1);
            quad(lvc, m, 0,ly0,1, 1,ly0,1, 1,ly1,1, 0,ly1,1,  0, 0, 1, lidSide, packedLight, packedOverlay, 1,1,1);
            quad(lvc, m, 0,ly0,0, 0,ly0,1, 0,ly1,1, 0,ly1,0, -1, 0, 0, lidSide, packedLight, packedOverlay, 1,1,1);
            quad(lvc, m, 1,ly0,1, 1,ly0,0, 1,ly1,0, 1,ly1,1,  1, 0, 0, lidSide, packedLight, packedOverlay, 1,1,1);
            // No underside: it would be coplanar with the column's own top face and z-fight.
            quad(lvc, m, 0,ly1,0, 0,ly1,1, 1,ly1,1, 1,ly1,0,  0, 1, 0, lidTop, packedLight, packedOverlay, 1,1,1);
        }

        // ── Control rod only: animated rod-cap sliding with insertion level ───
        if (be instanceof RBMKControlBlockEntity ctrl) {
            renderControlRod(ctrl, pt, ps, buf, height, packedLight, packedOverlay);
        }

        // ── Fuel channel only: OBJ inner geometry (hollow look) ───────────────
        if (isFuel) {
            TextureAtlasSprite innerSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/rbmk_element_inner");
            TextureAtlasSprite capSprite   = topSprite;
            Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element.obj");

            ps.pushPose();
            ps.translate(0.5, 0, 0.5); // OBJ is centered at origin
            for (int y = 0; y < height; y++) {
                Matrix4f om = ps.last().pose();
                RBMKColumnRenderer.renderObjGroup(vc, om, obj.get("Inner"), innerSprite, 1, 1, 1, packedLight, packedOverlay);
                RBMKColumnRenderer.renderObjGroup(vc, om, obj.get("Cap"),   capSprite,   1, 1, 1, packedLight, packedOverlay);
                ps.translate(0, 1, 0);
            }
            ps.popPose();

            // Fuel rod geometry (only when fuel is loaded)
            if (be instanceof RBMKRodBlockEntity rod && rod.hasRod) {
                int color = rod.rodColor;
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >>  8) & 0xFF) / 255f;
                float b = ( color        & 0xFF) / 255f;
                Map<String, List<float[]>> rodsObj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element_rods.obj");
                TextureAtlasSprite fuelSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/rbmk_element_fuel");
                // The rod mesh is one block tall (OBJ Y spans 0..1) and has to be stacked up the
                // whole channel, exactly like the "Inner"/"Cap" geometry above. It was drawn once
                // at the base instead, so a loaded fuel channel showed its rods only in the bottom
                // block and looked empty for the remaining COLUMN_HEIGHT blocks.
                ps.pushPose();
                ps.translate(0.5, 0, 0.5);
                for (int y = 0; y < height; y++) {
                    RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), rodsObj.get("Rods"), fuelSprite, r, g, b, packedLight, packedOverlay);
                    ps.translate(0, 1, 0);
                }
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
        // NOT RenderType.lightning(): its WEATHER_TARGET output state makes the haze render
        // through blocks under Fabulous graphics. See CustomRenderTypes.RBMK_CHERENKOV.
        VertexConsumer vc = buf.getBuffer(
                com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.RBMK_CHERENKOV);
        ps.pushPose();
        ps.translate(0.5, 0.75, 0.5);
        Matrix4f m = ps.last().pose();
        for (float j = 0; j <= height; j += 0.25f) {
            RenderHooks.vertexColor(vc, m, -0.5f, j, -0.5f, 102, 230, 255, 25);
            RenderHooks.vertexColor(vc, m, -0.5f, j,  0.5f, 102, 230, 255, 25);
            RenderHooks.vertexColor(vc, m,  0.5f, j,  0.5f, 102, 230, 255, 25);
            RenderHooks.vertexColor(vc, m,  0.5f, j, -0.5f, 102, 230, 255, 25);
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
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_rods.obj");
        List<float[]> lid = obj.get("Lid");
        if (lid == null) return;

        String capPrefix;
        if (ctrl instanceof RBMKControlManualBlockEntity manual) capPrefix = manual.getCapTexture();
        else if (ctrl instanceof RBMKControlAutoBlockEntity)     capPrefix = "rbmk_control_auto";
        else                                                     capPrefix = "rbmk_control";

        TextureAtlasSprite capSprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/rbmk/" + capPrefix);
        double level = Mth.lerp(pt, ctrl.lastLevel, ctrl.level);

        // Anchor = the bottom of the column's topmost block, then offset upwards by the rod's
        // insertion level - exactly the original's `glTranslated(0, offset + level, 0)`, where
        // its `offset` is the index of the top block (RBMKDials column height 3 -> a 4-block
        // column -> offset 3). Here the column spans y in [0, height) with height =
        // COLUMN_HEIGHT + 1, so that same anchor is `height - 1`.
        ps.pushPose();
        ps.translate(0.5, height + level - 1.0, 0.5);
        VertexConsumer vc = buf.getBuffer(RenderType.solid());
        RBMKColumnRenderer.renderObjGroup(vc, ps.last().pose(), lid, capSprite, 1, 1, 1, light, overlay);
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

    private static void flatBottom(VertexConsumer vc, Matrix4f m, int y,
                                    TextureAtlasSprite s, int light, int overlay) {
        quad(vc, m, 0,y,0, 1,y,0, 1,y,1, 0,y,1,  0, -1, 0, s, light, overlay, 1,1,1);
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
        RenderHooks.vertexFull(vc, m, x0, y0, z0, ir, ig, ib, 255, u0, v1, overlay, light, nx, ny, nz);
        RenderHooks.vertexFull(vc, m, x1, y1, z1, ir, ig, ib, 255, u1, v1, overlay, light, nx, ny, nz);
        RenderHooks.vertexFull(vc, m, x2, y2, z2, ir, ig, ib, 255, u1, v0, overlay, light, nx, ny, nz);
        RenderHooks.vertexFull(vc, m, x3, y3, z3, ir, ig, ib, 255, u0, v0, overlay, light, nx, ny, nz);
    }

    private MachineRBMKColumnRenderer() {}
}
