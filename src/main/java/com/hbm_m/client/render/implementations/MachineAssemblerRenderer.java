package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Assembler on the {@link MachineRenderers} factory: Body is static; Slider/Arm and
 * the 4 gears are TRACKED rigs (ANIM TRACK FORMAT v2, {@code trackedChainPart}).
 * Slider and Arm are KIND_TRANSLATE_AXIS chains (a tracked node cannot be
 * KIND_TRANSLATE -- a scalar track has no 3-word semantic): node 0 rides the X
 * SLIDER_TRACK triangle wave, the Arm adds a second Z node on the ARM_SWAY_TRACK
 * (rateScale 2.5 -- 40-tick sway period inside the 100-tick segment). The gears are
 * KIND_ROTATE nodes over a single "Cog" model part (four logical keys CogA..CogD,
 * each with its own pivot base offset) on 36-tick linear cycle tracks (A/C negative,
 * B/D positive). The recipe icon is an immediate hook.
 * <p>
 * The animation epoch IS the crafting state ({@code isCrafting() ? 1 : 0}): the
 * segment header words are written once on craft start/stop, and while the segment
 * runs the VSH samples the keyframe-pool tracks from uAnimTime alone -- zero params
 * evaluation and zero record upload per frame (the epoch-keyed word cache in
 * MachineBer). The CPU animators of the tracked declaration are derived from the
 * same rig (the degradation fallback paths land on the same pose by construction).
 * <p>
 * Static transforms of the legacy animators split by position relative to the joint:
 * rotations applied BEFORE the joint go into the record pose ({@code baseYawDegrees},
 * the -90 degree legacy yaw group), translations applied AFTER the joint go into
 * {@code baseOffset} -- they commute with the joint delta, pre-rotations do not.
 * <p>
 * Difference from legacy: the idle-combined mesh (merging Body+Slider+Arm+4xCog into one VBO
 * for idle machines) is not ported -- with automatic MDI the parts of all machines of a type
 * already coalesce into a single multi-draw, so the extra merge no longer pays off.
 *
 * @credit Spark Profiler Remediation / Idle Animation Epoch
 */
public final class MachineAssemblerRenderer {

    // Root transform from machine_assembler.json shifts model by (1,0,2); cog center is there, not at origin.
    private static final float ROOT_TX = 1f, ROOT_TZ = 2f;

    private static final float[][] COG_POSITIONS = {
            {-0.6f, 0.75f, 1.0625f},
            {0.6f, 0.75f, 1.0625f},
            {-0.6f, 0.75f, -1.0625f},
            {0.6f, 0.75f, -1.0625f},
    };

    /** The static yaw group every legacy animator opens with (see {@link #applyLegacyYaw}). */
    private static final float LEGACY_YAW_DEG = -90f;

    // ==================== ANIM TRACKS (ANIM TRACK FORMAT v2) ====================

    /**
     * Slider stroke: legacy ping-pong 0..500 over 5000ms along X mapped to
     * -0.75..+0.75 blocks, baked as a triangle-wave cycle track; rest pose is
     * exactly 0 (u0 = 0 when idle). Shared by the Slider and the Arm.
     */
    private static final MachineSpec.Track SLIDER_TRACK = MachineSpec.Track
            .samplesOf(new float[] {0f, 0.25f, 0.75f, 1.0f},
                       new float[] {0f, 0.75f, -0.75f, 0f}, true)
            .withRateScale(1.0f);

    /**
     * Arm sway along Z: 25-point cyclic sine, sampled from the legacy equation
     * {@code sin((u * 1000) / PI / 50) * 0.3}. rateScale 2.5 against the
     * 100-tick segment gives the 40-tick sway period.
     */
    private static final MachineSpec.Track ARM_SWAY_TRACK = buildSwayTrack();

    private static MachineSpec.Track buildSwayTrack() {
        float[] times = new float[25];
        float[] values = new float[25];
        for (int i = 0; i < 25; i++) {
            float u = i / 24.0f;
            times[i] = u;
            values[i] = (float) (Math.sin((u * 1000.0) / Math.PI / 50.0) * 0.3);
        }
        values[0] = 0.0f;
        return MachineSpec.Track.samplesOf(times, values, true).withRateScale(2.5f);
    }

    /** Cogs A/C spin negative: one revolution per 36-tick cycle. */
    private static final MachineSpec.Track COG_NEG_TRACK = MachineSpec.Track.linear(-360f, 0f);
    /** Cogs B/D spin positive: one revolution per 36-tick cycle. */
    private static final MachineSpec.Track COG_POS_TRACK = MachineSpec.Track.linear(360f, 0f);

    // -- Tracked segment header (pure functions of BE state; the epoch) -----

    /** Segment start tick = the epoch: flips exactly on craft start/stop. */
    private static long animSegmentStart(MachineAssemblerBlockEntity be) {
        return be.isCrafting() ? 1L : 0L;
    }

    /** Curve position at segment start: always the track head (rest pose = 0). */
    private static double animU0(MachineAssemblerBlockEntity be) {
        return 0.0;
    }

    /** Slider/Arm segment: 100 ticks while crafting; a 0 duration freezes u at u0. */
    private static double animDuration(MachineAssemblerBlockEntity be) {
        return be.isCrafting() ? 100.0 : 0.0;
    }

    /** Cog cycle: 36 ticks while crafting; a 0 duration freezes u at u0. */
    private static double cogDuration(MachineAssemblerBlockEntity be) {
        return be.isCrafting() ? 36.0 : 0.0;
    }

    public static void register() {
        MachineRenderers.machine("assembler", ModBlockEntities.MACHINE_ASSEMBLER_BE.get(),
                MachineAssemblerBlockEntity.class)
            // Body is static: the "animator" only applies the legacy yaw/bake offsets.
            .staticPart("Body", MachineAssemblerRenderer::animateBody)
            // Slider: tracked KIND_TRANSLATE_AXIS along X (a tracked node cannot
            // be KIND_TRANSLATE -- a scalar track has no 3-word semantic).
            // baseYawDegrees carries the legacy -90 degree group (pre-joint static
            // rotation -> record pose); baseOffset carries the animator's static
            // tail T(-0.5,0,-0.5) (post-joint translation).
            .trackedChainPart("Slider", "Slider",
                    -0.5f, 0f, -0.5f,
                    LEGACY_YAW_DEG, null, null,
                    MachineAssemblerRenderer::animSegmentStart,
                    MachineAssemblerRenderer::animU0,
                    MachineAssemblerRenderer::animDuration,
                    MachineSpec.JointNode.translate(1f, 0f, 0f,
                            (be, pt, gt, out, off) -> true,  // never called: tracked node
                            SLIDER_TRACK))
            // Arm rides WITH the Slider (same SLIDER_TRACK on node 0) and adds
            // its own Z sway (node 1, ARM_SWAY_TRACK, 40-tick period).
            .trackedChainPart("Arm", "Arm",
                    -0.5f, 0f, -0.5f,
                    LEGACY_YAW_DEG, null, null,
                    MachineAssemblerRenderer::animSegmentStart,
                    MachineAssemblerRenderer::animU0,
                    MachineAssemblerRenderer::animDuration,
                    MachineSpec.JointNode.translate(1f, 0f, 0f,
                            (be, pt, gt, out, off) -> true,
                            SLIDER_TRACK),
                    MachineSpec.JointNode.translate(0f, 0f, 1f,
                            (be, pt, gt, out, off) -> true,
                            ARM_SWAY_TRACK))
            // Four logical parts over the single "Cog" mesh. In VBO coordinates the spin
            // axis is the z-line through (ROOT_TX, 0): the pivot undoes the JSON root
            // offset, so the mesh spins about (1, 0) in xy and the z pivot coordinate is
            // free. baseOffset places the rotated mesh at the cog center:
            // b = (cx - 0.5, cy, cz - 0.5).
            .trackedChainPart("Cog", "CogA",
                    cogOffX(0), COG_POSITIONS[0][1], cogOffZ(0),
                    LEGACY_YAW_DEG, null, null,
                    MachineAssemblerRenderer::animSegmentStart,
                    MachineAssemblerRenderer::animU0,
                    MachineAssemblerRenderer::cogDuration,
                    MachineSpec.JointNode.rotate(0f, 0f, 1f, ROOT_TX, 0f, 0f,
                            (be, pt, gt, out, off) -> true,
                            COG_NEG_TRACK))
            .trackedChainPart("Cog", "CogB",
                    cogOffX(1), COG_POSITIONS[1][1], cogOffZ(1),
                    LEGACY_YAW_DEG, null, null,
                    MachineAssemblerRenderer::animSegmentStart,
                    MachineAssemblerRenderer::animU0,
                    MachineAssemblerRenderer::cogDuration,
                    MachineSpec.JointNode.rotate(0f, 0f, 1f, ROOT_TX, 0f, 0f,
                            (be, pt, gt, out, off) -> true,
                            COG_POS_TRACK))
            .trackedChainPart("Cog", "CogC",
                    cogOffX(2), COG_POSITIONS[2][1], cogOffZ(2),
                    LEGACY_YAW_DEG, null, null,
                    MachineAssemblerRenderer::animSegmentStart,
                    MachineAssemblerRenderer::animU0,
                    MachineAssemblerRenderer::cogDuration,
                    MachineSpec.JointNode.rotate(0f, 0f, 1f, ROOT_TX, 0f, 0f,
                            (be, pt, gt, out, off) -> true,
                            COG_NEG_TRACK))
            .trackedChainPart("Cog", "CogD",
                    cogOffX(3), COG_POSITIONS[3][1], cogOffZ(3),
                    LEGACY_YAW_DEG, null, null,
                    MachineAssemblerRenderer::animSegmentStart,
                    MachineAssemblerRenderer::animU0,
                    MachineAssemblerRenderer::cogDuration,
                    MachineSpec.JointNode.rotate(0f, 0f, 1f, ROOT_TX, 0f, 0f,
                            (be, pt, gt, out, off) -> true,
                            COG_POS_TRACK))
            .hook(new com.hbm_m.client.render.machine.MachineRenderHook<MachineAssemblerBlockEntity>() {
                @Override
                public void render(MachineAssemblerBlockEntity be, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay, MachineRenderApi api) {
                    renderRecipeIcon(be, partialTick, poseStack, bufferSource,
                            packedLight, packedOverlay, api);
                }

                @Override
                public boolean mayDrawContent(MachineAssemblerBlockEntity be) {
                    return !be.getClientRecipeIcon().isEmpty();
                }
            })
            .register();
    }

    private MachineAssemblerRenderer() {}

    /** baseOffset.x of cog {@code i} (see the registration comment for the derivation). */
    private static float cogOffX(int i) {
        return COG_POSITIONS[i][0] - 0.5f;
    }

    /** baseOffset.z of cog {@code i}. */
    private static float cogOffZ(int i) {
        return COG_POSITIONS[i][2] - 0.5f;
    }

    // ==================== LEGACY CPU ANIMATORS (fallback paths) ====================
    // The Slider/Arm/cog fallback animators are auto-derived by trackedChainPart
    // from the SAME rig (MachineSpec.sampleTracked consumes the same half-rounded
    // u0/duration and uAnimTime as the VSH), so no hand-written mirrors exist
    // anymore. Below stays only the Body animator, active on the degraded paths
    // (Iris Tier-1/immediate, shadow pass, forceVanillaImmediate).

    // Legacy model orientation: a full -90 degree rotation around the block center
    // (on top of the block transform that setupBlockTransform already provides).

    private static void applyLegacyYaw(PoseStack pose) {
        pose.translate(0.5f, 0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
        pose.translate(-0.5f, 0f, -0.5f);
    }

    /**
     * In legacy, Body was drawn inside the same -90-degree group as the animated parts,
     * with the T(-0.5,0,-0.5) compensation (the JSON root translation (1,0,2) in baked
     * coordinates of the multipart model). The factory's static part does not carry this
     * offset -- apply it via the "animator" transform (the engine strips the matrix itself).
     */
    private static boolean animateBody(MachineAssemblerBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        applyLegacyYaw(pose);
        pose.translate(-0.5f, 0f, -0.5f);
        return true;
    }

    // ==================== RECIPE ICON (hook) ====================

    /**
     * Recipe icon above the machine. The hook stack already carries the block transform
     * (T(0.5,0,0.5)*R(90)*R(legacy facing)); the old path from the "raw" stack did
     * T(0.5)*R(legacy)*T(-0.5)*R(90)*T(0,1.0625,0)*items, which in the current frame
     * equals R(-90)*T(-0.5,0,-0.5)*R(90)*T(0,1.0625,0)*items.
     * Visibility is already filtered by culling and the engine's animation distance.
     */
    private static final float RECIPE_ICON_MAX_DIST_SQ = 35.0f * 35.0f;
    private static final float FRAMED_SPRITE_SCALE = 0.5128205F;
    private static final float FRAMED_BOB = 0.1F;

    private static void renderRecipeIcon(MachineAssemblerBlockEntity be, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        ItemStack icon = be.getClientRecipeIcon();
        if (icon.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double distSq = mc.player.distanceToSqr(
                be.getBlockPos().getX() + 0.5,
                be.getBlockPos().getY() + 1.0,
                be.getBlockPos().getZ() + 0.5
        );
        if (distSq > RECIPE_ICON_MAX_DIST_SQ) return;

        poseStack.pushPose();
        // The poseStack entering this hook is at T(0.5, 0, 0.5) * R_facing.
        // In 1.7.10 / ntm-next the preview starts from (x+0.5, y, z+0.5) with facing rotation,
        // then adds glRotated(90, 0, 1, 0) and glTranslated(0, 1.0625, 0).
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0, 1.0625, 0.0);

        boolean isBlock = icon.getItem() instanceof BlockItem;
        if (isBlock) {
            poseStack.translate(0.0, -0.0625, 0.0);
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.translate(0.0, FRAMED_BOB, 0.0);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(0.0, -0.25, 0.0);
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.translate(0.0, FRAMED_BOB, 0.0);
            poseStack.scale(FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE);
            poseStack.translate(0.0, -0.05, 0.0);
        }

        mc.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.NONE,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
