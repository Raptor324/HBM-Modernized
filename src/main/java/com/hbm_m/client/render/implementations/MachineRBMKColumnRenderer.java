package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

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
import com.hbm_m.blockentity.machines.rbmk.RBMKReflectorBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKStorageBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Nucleus specs for all RBMK column blocks (the 12 column BEs: ROD / BLANK /
 * ABSORBER / REFLECTOR / COOLER / BOILER / HEATER / MODERATOR / OUTGASSER /
 * STORAGE / CONTROL / CONTROL_AUTO). The rbmk_autoloader is NOT registered
 * here: {@link MachineRbmkControlRenderer} owns its baked-parts spec. (An
 * uncommitted working-tree state did hold a duplicate hook spec here, which -
 * registered later - silently overwrote the baked one in
 * {@code MachineRenderRegistry.BY_TYPE}; it was removed in this migration.)
 *
 * <p>Unlike the previous all-hook version, every column's geometry now lives
 * in Nucleus dynamic parts ({@code dynamicPart} with a per-BE quad resolver,
 * foundry-channel pattern): the VBO is compiled once per distinct cache key
 * (texture prefix / height / lid state / stub flags) and instanced through the
 * MDI fleet across all identically-shaped columns. The spec's block transform
 * is IDENTITY: the synthesized geometry is authored in block-local coordinates
 * with the origin at the block corner (x/z in [0,1]), exactly like the legacy
 * renderer drew it - the default center+facing transform would displace it by
 * half a block. RBMK columns have no facing anyway (rotationally symmetric
 * panels).</p>
 *
 * <p>Part layout per column type:</p>
 * <ul>
 * <li>{@code Body} (all types): flat textured quads for the 4 side faces per
 * section (the tall panel look), the top face for non-fuel types, the bottom
 * face, the concrete/glass lid plate, and the pipe-corner stubs.</li>
 * <li>{@code Channel} (fuel only): the {@code Inner} + {@code Cap} groups of
 * {@code rbmk_element.obj} per section, creating the characteristic hollow
 * octagonal channel appearance visible from the top.</li>
 * <li>{@code Rods} (fuel only): the fuel rod mesh stacked per section, tinted
 * per instance via {@code tintOverride} from the rod's {@code rodColor}
 * (per-vertex quad colors do not survive the part VBO layout, which carries
 * pos/normal/uv only).</li>
 * <li>{@code Cap} (control only): the {@code Lid} group of {@code rbmk_rods.obj}
 * on a Y {@code translatingPart} joint, sliding with the insertion level.</li>
 * <li>One immediate hook on fuel types only: the Cherenkov additive haze, which
 * needs a custom additive render type - hooks are the engine's sanctioned path
 * for those.</li>
 * </ul>
 */
public final class MachineRBMKColumnRenderer {

    public static void register() {
        column("rbmk_column_rod",         ModBlockEntities.RBMK_ROD_BE,         RBMKRodBlockEntity.class,          true,  false);
        column("rbmk_column_blank",       ModBlockEntities.RBMK_BLANK_BE,       RBMKBlankBlockEntity.class,        false, false);
        column("rbmk_column_absorber",    ModBlockEntities.RBMK_ABSORBER_BE,    RBMKAbsorberBlockEntity.class,     false, false);
        column("rbmk_column_reflector",   ModBlockEntities.RBMK_REFLECTOR_BE,   RBMKReflectorBlockEntity.class,    false, false);
        column("rbmk_column_cooler",      ModBlockEntities.RBMK_COOLER_BE,      RBMKCoolerBlockEntity.class,       false, false);
        column("rbmk_column_boiler",      ModBlockEntities.RBMK_BOILER_BE,      RBMKBoilerBlockEntity.class,       false, false);
        column("rbmk_column_heater",      ModBlockEntities.RBMK_HEATER_BE,      RBMKHeaterBlockEntity.class,       false, false);
        column("rbmk_column_moderator",   ModBlockEntities.RBMK_MODERATOR_BE,   RBMKModeratorBlockEntity.class,    false, false);
        column("rbmk_column_outgasser",   ModBlockEntities.RBMK_OUTGASSER_BE,   RBMKOutgasserBlockEntity.class,    false, false);
        column("rbmk_column_storage",     ModBlockEntities.RBMK_STORAGE_BE,     RBMKStorageBlockEntity.class,      false, false);
        column("rbmk_column_control",     ModBlockEntities.RBMK_CONTROL_BE,     RBMKControlManualBlockEntity.class,false, true);
        column("rbmk_column_control_auto",ModBlockEntities.RBMK_CONTROL_AUTO_BE,RBMKControlAutoBlockEntity.class,  false, true);
    }

    /**
     * One spec per column BE type. The identity {@code blockTransform} keeps the
     * synthesized geometry at the block-corner origin; 64 blocks matches the
     * vanilla BER default view distance the legacy renderer ran at.
     */
    private static <T extends RBMKColumnBlockEntity> void column(
            String id,
            dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> type,
            Class<T> cls, boolean fuel, boolean control) {
        MachineSpecBuilder<T> spec = MachineRenderers.machine(id, type.get(), cls)
            .blockTransform((be, animator) -> { /* identity: block-corner geometry */ })
            .dynamicPart("Body", MachineRBMKColumnRenderer::bodyQuads, MachineRBMKColumnRenderer::bodyKey);
        if (fuel) {
            spec.dynamicPart("Channel", MachineRBMKColumnRenderer::channelQuads, MachineRBMKColumnRenderer::channelKey)
                // tintOverride must be declared BEFORE its part (overrides are read at part() time)
                .tintOverride("Rods", MachineRBMKColumnRenderer::rodTint)
                .dynamicPart("Rods", MachineRBMKColumnRenderer::rodQuads, MachineRBMKColumnRenderer::rodKey)
                .animationEpoch(MachineRBMKColumnRenderer::fuelEpoch)
                .hook(new MachineRenderHook<T>() {
                    @Override
                    public void render(T be, float pt, PoseStack ps, MultiBufferSource buf,
                                       int light, int overlay, MachineRenderApi api) {
                        if (be instanceof RBMKRodBlockEntity rod && rod.hasRod && rod.lastFluxQuantity > 5)
                            renderCherenkovGlow(ps, buf, RBMKColumnRenderer.columnHeight(be));
                    }

                    @Override
                    public boolean mayDrawContent(T be) {
                        return be instanceof RBMKRodBlockEntity rod && rod.hasRod && rod.lastFluxQuantity > 5;
                    }
                });
        }
        if (control) {
            // The rod-cap mesh slides vertically: distance in blocks from the block-corner
            // origin, exactly the original's `glTranslated(0, offset + level, 0)` with
            // offset = the top block index. columnHeight is height, level in [0,1].
            spec.translatingPart("Cap", 0f, 1f, 0f, 0f, 0f, 0f,
                    MachineRBMKColumnRenderer::capQuads, MachineRBMKColumnRenderer::capKey,
                    MachineRBMKColumnRenderer::capDistance);
        }
        spec.viewDistance(64).register();
    }

    // ─── Texture helpers (1:1 with the legacy renderer) ──────────────────────

    private static TextureAtlasSprite sprite(String path) {
        return RBMKColumnRenderer.sprite(RefStrings.MODID, path);
    }

    private static boolean isFuel(RBMKColumnBlockEntity be) {
        return be.getConsoleType() == ColumnType.FUEL || be.getConsoleType() == ColumnType.FUEL_SIM;
    }

    /**
     * Which types can have a lid plate drawn on top. hasOwnLid() types (control
     * rods, panel devices) are logically lidded - hasLid() is true for them so
     * they do not leak neutron flux - but they ship no "_cover_*"/"_glass_*"
     * textures, so the generic plate must not be drawn on them. Their own cap
     * mesh stands in for it. Matches RBMKBase.getIcon's "if(!hasOwnLid())"
     * guard in the original.
     */
    private static boolean showsLidPlate(RBMKColumnBlockEntity be) {
        return be.hasLid() && !be.hasOwnLid();
    }

    // ─── Body part ────────────────────────────────────────────────────────────

    /**
     * The full column body: 4 flat sides per section, the top face (non-fuel
     * only - the fuel channel's OBJ {@code Cap} group provides its own), the
     * bottom face, the lid plate and the pipe-corner stubs. The lid plate is
     * deliberately NOT a texture swap on the top face but a real 0.25-block
     * plate (its underside is omitted: coplanar with the column's top face =
     * z-fighting), which is what the "_cover_side"/"_glass_side" rim textures
     * are for. Glass lids keep their binary-alpha textures: the engine's part
     * paths alpha-test (the chemplant Frame precedent), unlike the old solid
     * RenderType the legacy renderer had to work around with a dedicated
     * cutout buffer.
     */
    private static List<BakedQuad> bodyQuads(RBMKColumnBlockEntity be) {
        String prefix = be.getRenderTexturePrefix();
        TextureAtlasSprite sideSprite = sprite("block/rbmk/" + prefix + "_side");
        // Every RBMK column type ships 3 top variants - plain (no lid), "_cover"
        // (concrete lid) and "_glass" (glass lid) - and the plate's rim/top use
        // them below. The column body itself always wears its plain "_top".
        // Lid gating goes through showsLidPlate(): reading the raw getLidState()
        // field sent types that can never have a lid looking for a "..._cover_top"
        // texture that doesn't exist, rendering the classic missing-texture
        // checkerboard on the whole top face.
        TextureAtlasSprite topSprite = sprite("block/rbmk/" + prefix + "_top");

        List<BakedQuad> quads = new ArrayList<>();
        int height = RBMKColumnRenderer.columnHeight(be);

        // For all columns: flat textured sides
        for (int y = 0; y < height; y++) {
            float y0 = y, y1 = y + 1;
            quads.add(RBMKColumnRenderer.flatQuad(sideSprite, net.minecraft.core.Direction.NORTH, 1,y0,0, 0,y0,0, 0,y1,0, 1,y1,0)); // N
            quads.add(RBMKColumnRenderer.flatQuad(sideSprite, net.minecraft.core.Direction.SOUTH, 0,y0,1, 1,y0,1, 1,y1,1, 0,y1,1)); // S
            quads.add(RBMKColumnRenderer.flatQuad(sideSprite, net.minecraft.core.Direction.WEST,  0,y0,0, 0,y0,1, 0,y1,1, 0,y1,0)); // W
            quads.add(RBMKColumnRenderer.flatQuad(sideSprite, net.minecraft.core.Direction.EAST,  1,y0,1, 1,y0,0, 1,y1,0, 1,y1,1)); // E
        }

        // Top face: flat for non-fuel; OBJ provides it for fuel channel
        if (!isFuel(be))
            quads.add(RBMKColumnRenderer.flatQuad(topSprite, net.minecraft.core.Direction.UP,
                    0,height,0, 0,height,1, 1,height,1, 1,height,0));

        // Bottom face. The renderer draws the whole column itself (the block model is skipped via
        // ENTITYBLOCK_ANIMATED), so without this the column is see-through from below - visible as
        // a hollow shell when standing under a reactor. Matches 1.7.10 RBMKControl/RBMKControlAuto:
        // ReaSim control rods use their dedicated "_bottom" texture; all other columns use "_top".
        TextureAtlasSprite bottomSprite = ("rbmk_control_reasim".equals(prefix) || "rbmk_control_reasim_auto".equals(prefix))
                ? sprite("block/rbmk/" + prefix + "_bottom")
                : topSprite;
        quads.add(RBMKColumnRenderer.flatQuad(bottomSprite, net.minecraft.core.Direction.DOWN,
                0,0,0, 1,0,0, 1,0,1, 0,0,1));

        // 1:1 with 1.7.10 RenderRBMKControl#renderWorldBlock (RBMKPipedBase):
        // Four corner pipe nozzles of height 0.125 on top of CONTROL (always), BOILER and HEATER
        // (whenever lidless).
        ColumnType consoleType = be.getConsoleType();
        boolean showsPipeStub =
                   consoleType == ColumnType.CONTROL
                || (consoleType == ColumnType.BOILER && !be.hasLid())
                || (consoleType == ColumnType.HEATER && !be.hasLid());
        if (showsPipeStub) {
            TextureAtlasSprite pipeTop  = sprite("block/rbmk/" + prefix + "_pipe_top");
            TextureAtlasSprite pipeSide = sprite("block/rbmk/" + prefix + "_pipe_side");
            float y0 = height, y1 = height + 0.125f;
            float[][] pipes = {
                    {0.0625f, 0.0625f, 0.4375f, 0.4375f},
                    {0.0625f, 0.5625f, 0.4375f, 0.9375f},
                    {0.5625f, 0.5625f, 0.9375f, 0.9375f},
                    {0.5625f, 0.0625f, 0.9375f, 0.4375f}
            };
            for (float[] p : pipes) {
                float x0 = p[0], z0 = p[1], x1 = p[2], z1 = p[3];
                quads.add(RBMKColumnRenderer.flatSubQuad(pipeSide, net.minecraft.core.Direction.NORTH,
                        x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 1f - x1, 0.875f, 1f - x0, 1f));
                quads.add(RBMKColumnRenderer.flatSubQuad(pipeSide, net.minecraft.core.Direction.SOUTH,
                        x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, x0, 0.875f, x1, 1f));
                quads.add(RBMKColumnRenderer.flatSubQuad(pipeSide, net.minecraft.core.Direction.WEST,
                        x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, z0, 0.875f, z1, 1f));
                quads.add(RBMKColumnRenderer.flatSubQuad(pipeSide, net.minecraft.core.Direction.EAST,
                        x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1f - z1, 0.875f, 1f - z0, 1f));
                quads.add(RBMKColumnRenderer.flatTopSubQuad(pipeTop, x0, y1, z0, x1, z1));
            }
        }

        // Lid: a real plate sitting on top of the column.
        // Geometry taken from the lid's own mesh (models/rbmk/models/deb_lid.obj, the debris entity
        // that pops off when a lid is knocked loose): a full-width plate 0.25 blocks thick. Placed
        // with its underside on the column's top face, its top ends up at height + 0.25 - exactly
        // level with the top of a control rod cap at level 0, which is how the two read as one
        // continuous reactor floor.
        if (showsLidPlate(be)) {
            String lidSuffix = be.getLidState() == 2 ? "_glass" : "_cover";
            TextureAtlasSprite lidTop  = sprite("block/rbmk/" + prefix + lidSuffix + "_top");
            TextureAtlasSprite lidSide = sprite("block/rbmk/" + prefix + lidSuffix + "_side");
            float ly0 = height, ly1 = height + 0.25f;
            // No underside: it would be coplanar with the column's own top face and z-fight.
            quads.add(RBMKColumnRenderer.flatQuad(lidSide, net.minecraft.core.Direction.NORTH, 1,ly0,0, 0,ly0,0, 0,ly1,0, 1,ly1,0));
            quads.add(RBMKColumnRenderer.flatQuad(lidSide, net.minecraft.core.Direction.SOUTH, 0,ly0,1, 1,ly0,1, 1,ly1,1, 0,ly1,1));
            quads.add(RBMKColumnRenderer.flatQuad(lidSide, net.minecraft.core.Direction.WEST,  0,ly0,0, 0,ly0,1, 0,ly1,1, 0,ly1,0));
            quads.add(RBMKColumnRenderer.flatQuad(lidSide, net.minecraft.core.Direction.EAST,  1,ly0,1, 1,ly0,0, 1,ly1,0, 1,ly1,1));
            quads.add(RBMKColumnRenderer.flatQuad(lidTop,  net.minecraft.core.Direction.UP,    0,ly1,0, 0,ly1,1, 1,ly1,1, 1,ly1,0));
        }
        return quads;
    }

    /** Cache key: everything the body geometry depends on. */
    private static String bodyKey(RBMKColumnBlockEntity be) {
        return be.getRenderTexturePrefix()
                + "|h" + RBMKColumnRenderer.columnHeight(be)
                + "|lid" + (showsLidPlate(be) ? be.getLidState() : -1);
    }

    // ─── Fuel channel parts ───────────────────────────────────────────────────

    /** Inner + Cap groups of rbmk_element.obj per section (the hollow channel). */
    private static List<BakedQuad> channelQuads(RBMKColumnBlockEntity be) {
        String prefix = be.getRenderTexturePrefix();
        TextureAtlasSprite innerSprite = sprite("block/rbmk/" + prefix + "_inner");
        TextureAtlasSprite capSprite   = sprite("block/rbmk/" + prefix + "_top");
        var obj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element.obj");
        List<BakedQuad> quads = new ArrayList<>();
        // OBJ is centered at origin: x/z shift +0.5 puts it in block-corner coordinates,
        // +1 per section up the channel (the legacy translate loop).
        for (int y = 0; y < RBMKColumnRenderer.columnHeight(be); y++) {
            quads.addAll(RBMKColumnRenderer.objGroupQuads(obj.get("Inner"), innerSprite, 0.5f, y, 0.5f));
            quads.addAll(RBMKColumnRenderer.objGroupQuads(obj.get("Cap"),   capSprite,   0.5f, y, 0.5f));
        }
        return quads;
    }

    private static String channelKey(RBMKColumnBlockEntity be) {
        return be.getRenderTexturePrefix() + "|h" + RBMKColumnRenderer.columnHeight(be);
    }

    /** Fuel rod mesh per section; hidden entirely (empty geometry) when no rod is loaded. */
    private static List<BakedQuad> rodQuads(RBMKColumnBlockEntity be) {
        if (!(be instanceof RBMKRodBlockEntity rod) || !rod.hasRod) return List.of();
        TextureAtlasSprite fuelSprite = sprite("block/rbmk/rbmk_element_fuel");
        var rodsObj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_element_rods.obj");
        List<BakedQuad> quads = new ArrayList<>();
        // The rod mesh is one block tall (OBJ Y spans 0..1) and is stacked up the whole
        // channel, one copy per section, like the Inner/Cap geometry above.
        for (int y = 0; y < RBMKColumnRenderer.columnHeight(be); y++) {
            quads.addAll(RBMKColumnRenderer.objGroupQuads(rodsObj.get("Rods"), fuelSprite, 0.5f, y, 0.5f));
        }
        return quads;
    }

    private static String rodKey(RBMKColumnBlockEntity be) {
        // hasRod MUST be part of the key: the resolver runs once per key, so a
        // channel built while empty would stay empty forever otherwise.
        return RBMKColumnRenderer.columnHeight(be) + "|" + (be instanceof RBMKRodBlockEntity rod && rod.hasRod);
    }

    /**
     * Per-instance rod color tint. Alpha is the engine's emission channel (NOT
     * transparency) - the rods are opaque, so 0.
     */
    private static float[] rodTint(RBMKColumnBlockEntity be) {
        if (!(be instanceof RBMKRodBlockEntity rod) || !rod.hasRod) return null;
        int color = rod.rodColor;
        return new float[] {
                ((color >> 16) & 0xFF) / 255f,
                ((color >>  8) & 0xFF) / 255f,
                ( color        & 0xFF) / 255f,
                0f
        };
    }

    /**
     * Epoch mixing prev/curr-visible state of everything that can change
     * without a geometry key change: rod loading, color (burnup), and the
     * Cherenkov hook's visibility gate. Height changes also move the
     * geometry keys, but mixing them here costs nothing and keeps the
     * roster assert honest.
     */
    private static long fuelEpoch(RBMKColumnBlockEntity be) {
        long h = RBMKColumnRenderer.columnHeight(be);
        if (be instanceof RBMKRodBlockEntity rod) {
            h = h * 1000003L + rod.rodColor;
            h = h * 31L + (rod.hasRod ? 1 : 0);
            h = h * 31L + (rod.lastFluxQuantity > 5 ? 1 : 0);
        }
        return h;
    }

    // ─── Control rod cap part ─────────────────────────────────────────────────

    /**
     * 1:1 port of the original's {@code RenderRBMKControlRod}: the rod-cap mesh
     * ({@code Lid} group of {@code rbmk_rods.obj}) slides vertically with the
     * control rod's current insertion {@code level}, interpolated between ticks
     * via {@code lastLevel}/{@code level}. The anchor is the bottom of the
     * column's topmost block offset by the level - the original's
     * {@code glTranslated(0, offset + level, 0)} where its {@code offset} is the
     * index of the top block (RBMKDials column height 3 → a 4-block column →
     * offset 3). Here the column spans y in [0, height) with
     * height = COLUMN_HEIGHT + 1, so that same anchor is height - 1.
     */
    private static double capDistance(RBMKColumnBlockEntity be, Float pt) {
        RBMKControlBlockEntity ctrl = (RBMKControlBlockEntity) be;
        return RBMKColumnRenderer.columnHeight(be) + Mth.lerp(pt, ctrl.lastLevel, ctrl.level) - 1.0;
    }

    private static List<BakedQuad> capQuads(RBMKColumnBlockEntity be) {
        var obj = RBMKColumnRenderer.getObj("models/rbmk/models/rbmk_rods.obj");
        TextureAtlasSprite capSprite = sprite("block/rbmk/" + capTexture(be));
        // OBJ centered at origin: +0.5 x/z puts it in block-corner coordinates; the Y
        // position comes from the translatingPart joint (capDistance).
        return RBMKColumnRenderer.objGroupQuads(obj.get("Lid"), capSprite, 0.5f, 0f, 0.5f);
    }

    private static String capKey(RBMKColumnBlockEntity be) {
        return capTexture(be);
    }

    private static String capTexture(RBMKColumnBlockEntity be) {
        if (be instanceof RBMKControlManualBlockEntity manual) return manual.getCapTexture();
        if (be instanceof RBMKControlAutoBlockEntity) return "rbmk_control_auto";
        return "rbmk_control";
    }

    // ─── Cherenkov glow (the one remaining immediate hook) ────────────────────

    /**
     * 1:1 with the original's RenderRBMKFuelChannel Cherenkov glow: a faint
     * additive cyan haze inside the channel once flux exceeds 5, using the rod's
     * own last published flux (the live accumulator is zeroed every tick right
     * after use, so it would almost always read 0 by the time a frame renders).
     */
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

    private MachineRBMKColumnRenderer() {}
}
