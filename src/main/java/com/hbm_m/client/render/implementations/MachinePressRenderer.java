package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hbm_m.blockentity.machines.MachinePressBlockEntity;
import com.hbm_m.client.model.PressBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Press on the {@link MachineRenderers} factory: Base is the static part,
 * Head is a TRACKED rig (ANIM TRACK FORMAT v2); the stamp and workpiece are
 * drawn by an immediate-mode hook (items, not OBJ). Geometry comes from the
 * multipart blockstate model ({@code hbm_m:press_loader}).
 */
public final class MachinePressRenderer {

    private static final String HEAD_PART = "Head";

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float WORKPIECE_HEIGHT = 1.125F + PIXEL;
    private static final float WORKPIECE_SCALE = 0.55F;
    private static final float STAMP_SCALE = 0.65F;

    // Client copies of the BE's press-cycle constants (MachinePressBlockEntity):
    // stampSpeed = speed * PROGRESS_AT_MAX / MAX_SPEED ticks the press 0..200.
    private static final int MAX_PRESS = 200;
    private static final int PROGRESS_AT_MAX = 25;

    // press.json head_transform.travel = 0.8; the streamed path scaled the
    // progress by max(0, travel - PIXEL) - the same stroke length is baked into
    // the clamp track below (a track is a PART constant; a different travel in
    // press.json must be mirrored here).
    private static final float EFFECTIVE_TRAVEL = Math.max(0.0F, 0.8F - PIXEL);

    // press.json head_transform.translation = T(0, 0.8, 0): the static rest
    // translation the legacy animator applied before the stroke - carried as the
    // tracked rig record's baseOffset below (net Y = 0.8 - dist). A rigid joint
    // cannot scale, so the legacy HEAD_SCALE 0.983 is not reproduced (~1.7%
    // residual size drift, accepted).
    private static final float HEAD_REST_Y = 0.8F;

    /**
     * The head stroke as a CLAMP track over one press cycle, in BLOCKS along
     * the -Y joint axis: downstroke (u 0..0.5, 0 -> EFFECTIVE_TRAVEL), retract
     * (u 0.5..1, back to 0). Frozen states pin u at 0.5 (head down, the craft
     * hold), at 1 (parked) or mid-stroke (stalled sync) - the clamp semantics
     * keep the head exactly on the last reached keyframe.
     */
    private static final MachineSpec.Track HEAD_TRACK = MachineSpec.Track.hermite(
            new float[] {0.0F, 0.5F, 1.0F},
            new float[] {0.0F, EFFECTIVE_TRAVEL, 0.0F},
            false);

    public static void register() {
        MachineRenderers.machine("press", com.hbm_m.blockentity.ModBlockEntities.PRESS_BE.get(),
                MachinePressBlockEntity.class)
            .part("Base")
            // Head: TRACKED rig (one-node chain). The joint is a KIND_TRANSLATE_AXIS
            // along -Y whose block distance comes from HEAD_TRACK (tracked nodes
            // carry no streamed words - the VSH samples the track itself from the
            // segment header t0/u0/durationTicks). baseOffset (the chain record
            // static) reproduces the legacy animator's static rest translation
            // T(0, 0.8, 0) (press.json head_transform.translation): net Y =
            // 0.8 - dist, identical to the streamed behaviour. Without it the head
            // renders one full stroke too low (stuck pressed, punches the floor,
            // stamp 0.8 low).
            // The segment classifier below is a PURE function of the synced BE
            // state + level game time; the epoch (segment start tick) changes only
            // at press-cycle state transitions, so the epoch-keyed AnimParams word
            // cache in MachineBer hits every frame within a segment and the CPU
            // skips the rig evaluation entirely while the head moves.
            // The CPU fallback animator (Iris/shadow/degraded paths) is derived
            // from the SAME declaration (trackedChainPart) - the CPU mirror
            // (MachineSpec.sampleTracked) consumes the same half-rounded
            // u0/duration and uAnimTime as the VSH, so both land on the same u.
            .trackedChainPart(HEAD_PART, HEAD_PART,
                    0f, HEAD_REST_Y, 0f, 0f, null, null,
                    MachinePressRenderer::pressSegmentEpoch,
                    MachinePressRenderer::pressSegmentU0,
                    MachinePressRenderer::pressSegmentDuration,
                    MachineSpec.JointNode.translate(0f, -1f, 0f,
                            // Never called - the node is tracked (its value comes
                            // from HEAD_TRACK).
                            (be, pt, gt, out, off) -> true,
                            HEAD_TRACK))
            .animationEpoch(MachinePressRenderer::pressSegmentEpoch)
            .hook(new com.hbm_m.client.render.machine.MachineRenderHook<MachinePressBlockEntity>() {
                @Override
                public void render(MachinePressBlockEntity be, float pt, PoseStack ps,
                                   MultiBufferSource bs, int light, int overlay, MachineRenderApi api) {
                    renderItems(be, pt, ps, bs, light, overlay, api);
                }

                @Override
                public boolean mayDrawContent(MachinePressBlockEntity be) {
                    return !be.getMaterialStack().isEmpty() || !be.getStampStack().isEmpty();
                }
            })
            .register();
    }

    private MachinePressRenderer() {}

    // -- Head motion segments (tracked rig epoch/header) -----------------
    //
    // The server presses in piecewise-linear segments at stampSpeed = speed *
    // PROGRESS_AT_MAX / MAX_PRESS per tick: down 0 -> 200, hold 5 ticks,
    // retract 200 -> 0, park/delay 5 ticks. All of press/speed/isRetracting are
    // synced to the client (writeNbtData), readable via getBlockEntityData.
    // Per the uAnimTime convention (FrameViewState.animTime, the pilots'
    // Mth.lerp(prev, curr, partialTick) semantics) a segment's t0 is the tick
    // BEFORE the first already-advanced sighting: t0 = now - elapsed where
    // elapsed counts the motion ticks already applied to the synced value.

    private static final int SEG_PARK = 0;         // parked/delay: u0 = 1 (frozen, value 0)
    private static final int SEG_DOWN = 1;         // downstroke: u0 = 0, dur = 2*downTicks
    private static final int SEG_HOLD_DOWN = 2;    // craft hold: u0 = 0.5 (frozen, value = travel)
    private static final int SEG_RETRACT = 3;      // retract: u0 = 0.5, dur = 2*upTicks
    private static final int SEG_FROZEN_DOWN = 4;  // stalled mid-downstroke (stale sync guard)
    private static final int SEG_FROZEN_UP = 5;    // stalled mid-retract (stale sync guard)

    /** Segment start tick (level.getGameTime() domain) - the machine epoch. */
    private static long pressSegmentEpoch(MachinePressBlockEntity be) {
        // CLEAN t0, no kind tag: the epoch rides into the record verbatim as the
        // t0 float32 word ((float) segmentStartTickFn.applyAsLong(be)) - a tag in
        // the high bits destroys the float32 mantissa and freezes the head
        // (same bug as the assembler ring, 2026-10-07). Injectivity across
        // segments still holds on t0 alone: a frozen park's t0 is a past tick,
        // and any motion segment computes t0 = now - elapsed with strictly
        // increasing now; two segments of one machine never classify to the
        // same start tick.
        return classifyPress(be).t0;
    }

    /** Curve position at t0 (the track slot the segment starts from). */
    private static double pressSegmentU0(MachinePressBlockEntity be) {
        return classifyPress(be).u0;
    }

    /** Segment duration in ticks; a decoded 0 freezes u at u0 (park/hold/stalled). */
    private static double pressSegmentDuration(MachinePressBlockEntity be) {
        return classifyPress(be).dur;
    }

    private static final class PressSeg {
        final int kind;
        final long t0;
        final float u0;
        final float dur;

        PressSeg(int kind, long t0, float u0, float dur) {
            this.kind = kind;
            this.t0 = t0;
            this.u0 = u0;
            this.dur = dur;
        }
    }

    /**
     * Classifies the current head motion segment from the synced BE state.
     * Pure (BE state + level game time, no per-frame inputs): while a segment
     * runs, t0/u0/dur are constant - the epoch-keyed word cache hits and the
     * GPU advances the head through the track from uAnimTime alone.
     */
    private static PressSeg classifyPress(MachinePressBlockEntity be) {
        var data = be.getBlockEntityData();
        int press = data.get(0);
        int speed = data.get(4);
        boolean retract = data.get(8) == 1;
        int stampSpeed = speed * PROGRESS_AT_MAX / MAX_PRESS;
        long now = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;

        if (retract) {
            if (press >= MAX_PRESS) {
                return new PressSeg(SEG_HOLD_DOWN, 0L, 0.5F, 0F);
            }
            if (press <= 0) {
                return new PressSeg(SEG_PARK, 0L, 1F, 0F);
            }
            if (stampSpeed > 0) {
                int elapsed = (MAX_PRESS - press) / stampSpeed;
                return new PressSeg(SEG_RETRACT, now - elapsed, 0.5F,
                        (2F * MAX_PRESS) / stampSpeed);
            }
            return new PressSeg(SEG_FROZEN_UP, press, 0.5F + 0.5F * press / MAX_PRESS, 0F);
        }
        if (press > 0) {
            if (stampSpeed > 0) {
                int elapsed = press / stampSpeed;
                return new PressSeg(SEG_DOWN, now - elapsed, 0F,
                        (2F * MAX_PRESS) / stampSpeed);
            }
            return new PressSeg(SEG_FROZEN_DOWN, press, 0.5F * press / MAX_PRESS, 0F);
        }
        return new PressSeg(SEG_PARK, 0L, 1F, 0F);
    }

    private static @Nullable PressBakedModel pressModel(MachinePressBlockEntity blockEntity) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(blockEntity.getBlockState());
        return AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw) instanceof PressBakedModel p ? p : null;
    }

    // Hook: stamp item on the head plus workpiece item on the table.

    private static void renderItems(MachinePressBlockEntity blockEntity, float partialTick,
                                    PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay, MachineRenderApi api) {
        renderWorkpiece(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        renderStampItem(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderWorkpiece(MachinePressBlockEntity blockEntity, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = blockEntity.getMaterialStack();
        if (stack.isEmpty()) {
            return;
        }

        var mc = Minecraft.getInstance();

        poseStack.pushPose();
        // Local (0, 0) here is the block center after the block transform
        poseStack.translate(0.32F, WORKPIECE_HEIGHT, 0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.pushPose();
        poseStack.scale(WORKPIECE_SCALE, WORKPIECE_SCALE, WORKPIECE_SCALE);
        poseStack.translate(-0.5F, -0.5F, 0.32F);

        mc.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderStampItem(MachinePressBlockEntity blockEntity, float partialTick,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        ItemStack stamp = blockEntity.getStampStack();
        if (stamp.isEmpty()) {
            return;
        }

        var mc = Minecraft.getInstance();

        // Stamp height from the BE state directly (same formula as animateHead) -
        // NOT from MachineRenderApi.partTransform: that pose lives in the BER's
        // view space (L * T(rel) * ...), so its translation component is rotated by
        // the camera basis and the stamp floated up/down with camera pitch/yaw.
        PressBakedModel model = pressModel(blockEntity);
        if (model == null) {
            return;
        }
        Vector3f rest = model.getHeadRestOffset();
        float offsetY = rest.y()
                - (blockEntity.getPressAnimationProgress(partialTick)
                        * Math.max(0.0F, model.getHeadTravelDistance() - PIXEL));

        poseStack.pushPose();
        // Block center on X/Z, at the head height plus a small offset
        poseStack.translate(0.32F, offsetY + 0.98F, 0.32F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.pushPose();
        poseStack.scale(STAMP_SCALE, STAMP_SCALE, STAMP_SCALE);
        poseStack.translate(-0.5F, -0.5F, 0.0F);

        mc.getItemRenderer().renderStatic(
                stamp,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                (int) blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
        poseStack.popPose();
    }
}
