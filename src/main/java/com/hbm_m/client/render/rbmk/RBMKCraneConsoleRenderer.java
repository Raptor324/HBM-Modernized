package com.hbm_m.client.render.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of the original's {@code RenderCraneConsole}: the console desk itself plus, when the
 * crane has been set up, the whole gantry hovering over the reactor.
 *
 * <p>The desk animates a joystick that tilts with the crane's travel input and two dial needles
 * showing the loaded rod's heat and enrichment, both of which the original jitters slightly with a
 * sine wave so they never sit perfectly still. Two lamps report the crane's load state and whether
 * it is currently over a column it could interact with.</p>
 *
 * <p>The gantry is assembled from repeated {@code Girder} and {@code Tube} segments sized to the
 * scanned reactor span and height, then the {@code Lift} drops by up to 3.25 blocks as the crane
 * lowers - the same construction, in the same order, as the original.</p>
 */
public class RBMKCraneConsoleRenderer implements BlockEntityRenderer<RBMKCraneConsoleBlockEntity> {

    private static final int FULLBRIGHT = LightTexture.FULL_BRIGHT;

    public RBMKCraneConsoleRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static Map<String, List<float[]>> console() {
        return RBMKColumnRenderer.getObj("models/rbmk/models/crane_console.obj");
    }

    private static Map<String, List<float[]>> crane() {
        return RBMKColumnRenderer.getObj("models/rbmk/models/crane.obj");
    }

    private static void part(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
                              String group, TextureAtlasSprite sprite,
                              float r, float g, float b, int light, int overlay) {
        List<float[]> mesh = obj.get(group);
        if (mesh == null) return;
        RBMKColumnRenderer.renderObjGroup(buf.getBuffer(net.minecraft.client.renderer.RenderType.solid()),
                ps.last().pose(), mesh, sprite, r, g, b, light, overlay);
    }

    /** The original's metadata switch: NORTH(2)→90°, WEST(4)→180°, SOUTH(3)→270°, EAST(5)→0°. */
    private static float facingAngle(Direction facing) {
        return switch (facing) {
            case NORTH -> 90f;
            case WEST  -> 180f;
            case SOUTH -> 270f;
            default    -> 0f;
        };
    }

    @Override
    public void render(RBMKCraneConsoleBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf,
                       int light, int overlay) {
        Map<String, List<float[]>> obj = console();
        TextureAtlasSprite deskTex  = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/crane_console");
        TextureAtlasSprite craneTex = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/rbmk_crane");

        float angle = facingAngle(be.facing);

        // ── The console desk ─────────────────────────────────────────────────
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(angle));
        ps.translate(0.5, 0, 0);

        part(ps, buf, obj, "Console_Coonsole", deskTex, 1f, 1f, 1f, light, overlay);

        // Joystick, tilting with the crane's travel input.
        ps.pushPose();
        ps.translate(0.75, 1, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) Mth.lerp(pt, be.lastTiltFront, be.tiltFront)));
        ps.mulPose(Axis.XP.rotationDegrees((float) Mth.lerp(pt, be.lastTiltLeft, be.tiltLeft)));
        ps.translate(-0.75, -1.015, 0);
        part(ps, buf, obj, "Joystick", deskTex, 1f, 1f, 1f, light, overlay);
        ps.popPose();

        // Heat and enrichment dials; the original adds the same sine jitter to both.
        dial(ps, buf, obj, deskTex, 0.75, be.loadedHeat, light, overlay, "Meter1");
        dial(ps, buf, obj, deskTex, 0.25, be.loadedEnrichment, light, overlay, "Meter2");

        // Lamp 1 - crane load state: yellow while moving, green when loaded, near-black when empty.
        float[] lamp1 = be.isCraneLoading() ? new float[]{0.8f, 0.8f, 0f}
                : be.hasItemLoaded()        ? new float[]{0f, 1f, 0f}
                                            : new float[]{0f, 0.1f, 0f};
        part(ps, buf, obj, "Lamp1", deskTex, lamp1[0], lamp1[1], lamp1[2], FULLBRIGHT, overlay);

        // Lamp 2 - is the crane above something it can interact with?
        boolean valid = be.getLevel() != null && be.getLoadableAtPos(be.getLevel()) != null;
        part(ps, buf, obj, "Lamp2", deskTex, valid ? 0f : 1f, valid ? 1f : 0f, 0f, FULLBRIGHT, overlay);

        ps.popPose();

        if (!be.setUpCrane) return;

        // ── The gantry over the reactor ──────────────────────────────────────
        ps.pushPose();
        ps.translate(0.5, -1, 0.5);

        var pos = be.getBlockPos();
        ps.translate(be.center.getX() - pos.getX(),
                     be.center.getY() - pos.getY() + 1,
                     be.center.getZ() - pos.getZ());
        ps.mulPose(Axis.YP.rotationDegrees(angle));

        double posX = Mth.lerp(pt, be.lastPosFront, be.posFront);
        double posZ = Mth.lerp(pt, be.lastPosLeft, be.posLeft);
        ps.translate(-posX, 0, posZ);
        ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));

        // Girder run: rotated back to world axes, walked out to the far end of the span.
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-be.craneRotationOffset));
        int girderSpan;
        switch (be.craneRotationOffset) {
            case 90 -> {
                girderSpan = be.spanL + be.spanR + 1;
                ps.translate(0, 0, -posZ - be.spanR);
            }
            case 180 -> {
                girderSpan = be.spanF + be.spanB + 1;
                ps.translate(posX - be.spanF, 0, 0);
            }
            case 270 -> {
                girderSpan = be.spanL + be.spanR + 1;
                ps.translate(0, 0, -posZ + be.spanL);
            }
            default -> {
                girderSpan = be.spanF + be.spanB + 1;
                ps.translate(posX + be.spanB, 0, 0);
            }
        }
        ps.mulPose(Axis.YP.rotationDegrees(be.craneRotationOffset));
        for (int i = 0; i < girderSpan; i++) {
            part(ps, buf, crane(), "Girder", craneTex, 1f, 1f, 1f, light, overlay);
            ps.translate(-1, 0, 0);
        }
        ps.popPose();

        part(ps, buf, crane(), "Main", craneTex, 1f, 1f, 1f, light, overlay);

        ps.pushPose();
        for (int i = 0; i < be.height - 6; i++) {
            part(ps, buf, crane(), "Tube", craneTex, 1f, 1f, 1f, light, overlay);
            ps.translate(0, 1, 0);
        }
        ps.translate(0, -1, 0);
        part(ps, buf, crane(), "Carriage", craneTex, 1f, 1f, 1f, light, overlay);
        ps.popPose();

        ps.translate(0, -3.25 * (1 - Mth.lerp(pt, be.lastProgress, be.progress)), 0);
        part(ps, buf, crane(), "Lift", craneTex, 1f, 1f, 1f, light, overlay);

        ps.popPose();
    }

    /** One dial needle; {@code value} is 0-1 and maps onto the original's 135°…-135° sweep. */
    private void dial(PoseStack ps, MultiBufferSource buf, Map<String, List<float[]>> obj,
                       TextureAtlasSprite tex, double z, double value, int light, int overlay, String group) {
        ps.pushPose();
        ps.translate(0, 1.25, z);
        double jitter = Math.sin(System.currentTimeMillis() * 0.01 % 360) * 180 / Math.PI * 0.05;
        ps.mulPose(Axis.XP.rotationDegrees((float) (jitter + 135 - 270 * value)));
        ps.translate(0, -1.25, -z);
        part(ps, buf, obj, group, tex, 1f, 1f, 1f, light, overlay);
        ps.popPose();
    }

    /** The gantry can reach well beyond the console block itself. */
    @Override public int getViewDistance() { return 128; }
    @Override public boolean shouldRenderOffScreen(RBMKCraneConsoleBlockEntity be) { return true; }
}
