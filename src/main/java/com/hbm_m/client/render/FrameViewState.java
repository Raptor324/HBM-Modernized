package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import org.joml.Matrix4f;

import com.hbm_m.client.render.culling.MdiRenderFrameGate;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

@OnlyIn(Dist.CLIENT)
/**
 * Per-frame snapshot of camera-dependent quantities for world-space instance records.
 * <p>
 * Instance records (InstPos/InstRot) store WORLD-space position/rotation - the camera
 * is factored into the uniform {@code ModelViewMat} = V = modelView * L * T(-camPos),
 * where L is the level pose basis (see {@link #noteFrameBasis}). This makes
 * records invariant to camera motion (zero dirty traffic in a static scene - see
 * GpuSpanUploader) and removes inter-tick position lag.
 * <p>
 * <b>View bob:</b> vanilla applies bobbing to the SAME poseStack the BE loop reads,
 * so an uncorrected BER pose leaks bob into the "world-space" records - every
 * record-reuse path (roster asserts, MDI clean-frame reuse, GPU bake memcmp) would
 * then freeze records at a stale bob phase and animated parts visibly float while
 * walking. Therefore records are built from {@code L^-1 * pose} (bob-stripped,
 * genuinely static) and the bob is re-applied at draw time through the view matrix.
 * L is noted once per frame at AFTER_ENTITIES from the level event poseStack.
 * <p>
 * Contract derived from vanilla sources (verified against 1.20.1 forge / 1.21.1 neoforge):
 * <ul>
 *   <li><b>1.20.1</b>: GameRenderer.renderLevel pushes R_cam (= Rz(roll)*Rx(pitch)*Ry(yaw+180))
 *       AND the bob transforms onto the POSE stack, which goes into LevelRenderer.renderLevel
 *       and is used by the BE loop (translate(bePos - cam) on top of L). modelViewStack is
 *       not touched until AFTER_BLOCK_ENTITIES (pushed only in the translucent section) -
 *       getModelViewMatrix() is identity there. L = R_cam * bob on this version.</li>
 *   <li><b>1.21.1</b>: the reverse - R_cam = frustumMatrix = rotation(camera.rotation().conjugate())
 *       is pushed onto modelViewStack (Matrix4fStack) BEFORE entities and lives until popMatrix
 *       (after AFTER_BLOCK_ENTITIES), while the BE loop translates the level poseStack
 *       (carrying only bob). L = bob on this version.</li>
 * </ul>
 * In both cases composed = getModelViewMatrix()*pose = modelView*L*T(rel)*local, so the
 * true clip transform is reproduced exactly by {@code (modelView*L*T(-relCam)) * (T(relCam)*L^-1*pose)}
 * = modelView*pose. Capture is lazy on first access per frame (always inside renderLevel
 * - the BE pass or later), keyed by the serial {@link MdiRenderFrameGate}.
 */
public final class FrameViewState {

    private static final Matrix4f INV_VIEW_ROT = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    // Level pose basis L (R_cam*bob on 1.20.1, bob on 1.21.1) - noted once per
    // frame at AFTER_ENTITIES. Persists across frames: consumers reached before
    // the note (Iris shadow pass, first dispatcher renders) use the previous
    // frame's basis - a one-frame bob phase lag, invisible in practice.
    private static final Matrix4f LEVEL_BASIS = new Matrix4f();
    private static final Matrix4f LEVEL_BASIS_INV = new Matrix4f();
    // modelView * L: rotation part of the draw-time view (no translation).
    private static final Matrix4f VIEW_BASIS = new Matrix4f();
    private static float camX, camY, camZ;
    private static float relCamX, relCamY, relCamZ;
    private static net.minecraft.core.BlockPos anchorOrigin = net.minecraft.core.BlockPos.ZERO;
    private static boolean anchorInitialized = false;
    private static long anchorGeneration = 0L;
    private static final double SQR_MAX_ANCHOR_DISTANCE = 256.0 * 256.0;

    private static boolean valid = false;
    private static long capturedSerial = -1L;

    static {
        // Session teardown: forget the anchor so the next world join takes the
        // init path of checkAnchorDrift -> onAnchorChanged ->
        // InstancedStaticPartRenderer.onRenderOriginChanged -> worldGen bump,
        // which bulk-clears RenderBoundsCache and every per-BER render-state map (audit
        // 2026-10-05 §3.1). SESSION_END only: a mid-session reset would
        // re-anchor (and bump worldGen) after every resource reload.
        com.hbm_m.client.render.cache.RenderCacheManager.register(reason -> {
            if (reason == com.hbm_m.client.render.cache.RenderCacheManager.Reason.SESSION_END) {
                resetSessionAnchor();
            }
        });
    }

    private FrameViewState() {}

    /**
     * Cache reset. Called from emergency frame paths (exception before
     * {@code advanceAfterPresent}): the serial does not advance then, and without
     * the reset the next frame would silently reuse a stale camera.
     */
    public static void invalidate() {
        valid = false;
    }

    /**
     * Anchor teardown on world exit: the next level render re-initialises the
     * anchor at the new camera position instead of measuring drift against the
     * previous world's anchor.
     */
    public static void resetSessionAnchor() {
        anchorInitialized = false;
        anchorOrigin = net.minecraft.core.BlockPos.ZERO;
    }

    /**
     * Current anchor origin. Forces the per-frame capture first, so a consumer
     * reached BEFORE the frame's checkAnchorDrift (retained initial bakes read
     * this from SparseUpdateQueue) never observes the (0,0,0) pre-init anchor -
     * a zero-delta init there would skip the rebase in
     * RetainedInstanceGraph.onRenderOriginChanged / InstancedStaticPartRenderer
     * and bake machines displaced by the full anchor offset. Render-thread only
     * (like the other capture() accessors).
     */
    public static net.minecraft.core.BlockPos anchorOrigin() {
        capture();
        return anchorOrigin;
    }

    public static long anchorGeneration() {
        return anchorGeneration;
    }

    public static boolean checkAnchorDrift(net.minecraft.world.phys.Vec3 cameraPos) {
        if (!anchorInitialized) {
            anchorOrigin = net.minecraft.core.BlockPos.containing(cameraPos.x, cameraPos.y, cameraPos.z);
            anchorInitialized = true;
            anchorGeneration++;
            onAnchorChanged(0, 0, 0);
            return true;
        }
        double dx = cameraPos.x - anchorOrigin.getX();
        double dy = cameraPos.y - anchorOrigin.getY();
        double dz = cameraPos.z - anchorOrigin.getZ();
        if (dx * dx + dy * dy + dz * dz > SQR_MAX_ANCHOR_DISTANCE) {
            net.minecraft.core.BlockPos oldAnchor = anchorOrigin;
            anchorOrigin = net.minecraft.core.BlockPos.containing(cameraPos.x, cameraPos.y, cameraPos.z);
            anchorGeneration++;
            int deltaX = anchorOrigin.getX() - oldAnchor.getX();
            int deltaY = anchorOrigin.getY() - oldAnchor.getY();
            int deltaZ = anchorOrigin.getZ() - oldAnchor.getZ();
            com.hbm_m.main.MainRegistry.LOGGER.info("[Nucleus] Anchor origin shift: {} -> {} (delta {}, {}, {})",
                    oldAnchor, anchorOrigin, deltaX, deltaY, deltaZ);
            onAnchorChanged(deltaX, deltaY, deltaZ);
            return true;
        }
        return false;
    }

    private static void onAnchorChanged(int deltaX, int deltaY, int deltaZ) {
        MdiBatchCoordinator.onRenderOriginChanged();
        MdiGeometryAtlas atlas = MdiGeometryAtlas.peekOrNull();
        if (atlas != null && atlas.isReady()) {
            atlas.onRenderOriginChanged();
        }
        InstancedStaticPartRenderer.onRenderOriginChanged(deltaX, deltaY, deltaZ);
        com.hbm_m.client.render.retained.RetainedInstanceGraph rig =
                com.hbm_m.client.render.retained.RetainedInstanceGraph.get();
        if (rig != null) {
            rig.onRenderOriginChanged(deltaX, deltaY, deltaZ);
        }
        com.hbm_m.client.render.IrisShadowBatchCollector.onRenderOriginChanged();
    }

    private static void onAnchorChanged() {
        onAnchorChanged(0, 0, 0);
    }

    /** Render thread, inside renderLevel (BE pass or later stage events). */
    private static void capture() {
        // Never capture mid-shadow-pass: the Iris shadow pass is the FIRST pass of
        // frame N+1, so without this guard a capture() triggered there (e.g. via
        // anchorOrigin() from the shadow prebake consumers) would bump the drift
        // check / rebase every renderer (onAnchorChanged -> memFree of live
        // PrebakeSnapshot records - use-after-free in IrisShadowBatchCollector)
        // and latch capturedSerial so the subsequent main pass would skip its own
        // capture. Consumers reached during the shadow pass keep the LAST MAIN
        // pass's snapshot, which is exactly what the shadow records were built
        // against; the main pass re-captures as usual (capturedSerial lags).
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        long serial = MdiRenderFrameGate.currentSerial();
        if (valid && capturedSerial == serial) {
            return;
        }
        // invViewRot = R_cam^-1 (pure rotation).
        //? if < 1.21.1 {
        INV_VIEW_ROT.set(RenderSystem.getInverseViewRotationMatrix());
        //?} else {
        /*// rotation(camera.rotation()) = R_cam^-1: vanilla's frustumMatrix =
        // rotation(rotation().conjugate()) = R_cam, i.e. the quaternion already carries
        // the INVERSE view rotation - an extra .invert() is NOT needed here.
        INV_VIEW_ROT.rotation(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        *///?}
        var camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        camX = (float) camPos.x;
        camY = (float) camPos.y;
        camZ = (float) camPos.z;

        checkAnchorDrift(camPos);

        relCamX = (float) (camPos.x - anchorOrigin.getX());
        relCamY = (float) (camPos.y - anchorOrigin.getY());
        relCamZ = (float) (camPos.z - anchorOrigin.getZ());

        // Refresh VIEW_BASIS here too: viewMatrix()/viewBasisRotation() can be
        // called (e.g. from onBeforeBlockEntities) BEFORE noteFrameBasis ran this
        // frame - without this they returned the previous frame's basis (or
        // identity on the first frame).
        VIEW_BASIS.set(RenderSystem.getModelViewMatrix()).mul(LEVEL_BASIS);

        capturedSerial = serial;
        valid = true;
    }

    /** R_cam^-1 (rotation without translation). Do not mutate the returned matrix. */
    public static Matrix4f inverseViewRotation() {
        capture();
        return INV_VIEW_ROT;
    }

    /**
     * Notes the level pose basis L: the poseStack the BE loop translates on top of
     * (R_cam*bob on 1.20.1, bob on 1.21.1 - wherever the version composes it).
     * Called once per frame at AFTER_ENTITIES, BEFORE machine collection.
     * <p>
     * The BER pose seen by the record builder is {@code L * T(rel) * local}; composing
     * with {@code L^-1} (see {@link #poseToWorldRotation()}) yields the bob-stripped
     * static record {@code T(rel)*local}, and the bob returns at draw time through
     * {@link #viewMatrix()} / {@link #viewBasisRotation()}.
     */
    public static void noteFrameBasis(Matrix4f levelBasis) {
        capture();
        // LEVEL_BASIS must be stored too: capture() composes VIEW_BASIS from it
        // (viewMatrix() before noteFrameBasis reads the previous frame's basis,
        // which silently degraded to identity on the first frame).
        LEVEL_BASIS.set(levelBasis);
        LEVEL_BASIS_INV.set(levelBasis).invert();
        VIEW_BASIS.set(RenderSystem.getModelViewMatrix()).mul(levelBasis);
    }

    /**
     * Record basis: {@code L^-1} - converts a BER pose {@code L*T(rel)*local} into the
     * bob-stripped static record {@code T(rel)*local} in ONE matrix multiply per part
     * (the add of relCam happens in convertToWorldRecord). Machine-independent.
     * Do not mutate the result.
     */
    public static Matrix4f poseToWorldRotation() {
        capture();
        return LEVEL_BASIS_INV;
    }

    /**
     * V = modelView * L * T(-relCam): anchor-relative world records -> view-space,
     * with the level basis (view bob included) re-applied. Identity check:
     * {@code V * (T(relCam) * L^-1 * pose) = modelView * pose} - the exact vanilla
     * BER clip transform. Set into the ModelViewMat uniform of instanced programs.
     * Do not mutate the result.
     */
    public static Matrix4f viewMatrix() {
        capture();
        VIEW.set(VIEW_BASIS).translate(-relCamX, -relCamY, -relCamZ);
        return VIEW;
    }

    /** Rotation part of {@link #viewMatrix()} (modelView * L) without the translation. Do not mutate. */
    public static Matrix4f viewBasisRotation() {
        capture();
        return VIEW_BASIS;
    }

    public static float camX() { capture(); return camX; }
    public static float camY() { capture(); return camY; }
    public static float camZ() { capture(); return camZ; }

    public static float relCamX() { capture(); return relCamX; }
    public static float relCamY() { capture(); return relCamY; }
    public static float relCamZ() { capture(); return relCamZ; }

    // -- Animation time (uAnimTime uniform, keyframed joint tracks v2) ---
    // Single source of truth for the continuous animation time in the
    // level.getGameTime() tick domain at which the CURRENT frame renders
    // tick-boundary state. HBM machines step their animated state once per
    // client tick and interpolate with Mth.lerp(partialTick, prev, curr)
    // between the two most recent tick-end values (pilot machines:
    // MachinePressBlockEntity.clientTick shifts prev <- curr per tick and
    // getPressAnimationProgress lerps (partialTick, prev, curr);
    // MachineAdvancedAssemblerRenderer.ringLerped does the same), so a frame
    // rendered at (gameTime, partialTick) shows the state at effective
    // continuous time (gameTime - 1) + partialTick. The clock therefore
    // carries a -1 offset against the raw tick counter: a tracked rig whose
    // epoch t0 is the segment start tick (the first tick whose END state
    // already advanced) and whose u0 is the segment value at t0 gets
    // u = u0 exactly at ticks == t0 - during tick t0's frames u stays
    // clamped at u0 and motion progresses during tick t0+1's frames,
    // frame-for-frame identical to the streamed prev/curr path. The VSH and
    // the CPU mirror both evaluate u = u0 + ((ticks - t0) + pt) / duration,
    // so both land on the same u.
    // Memoized per render frame (the shadow and main passes of one frame
    // share one partial tick - the same counter MachineBer.frameGameTime
    // uses), so every upload of a frame is the same value.
    private static long animTimeFrameStamp = -1L;
    private static float animTicksMemo = 0.0f;
    private static float animPartialTickMemo = 0.0f;

    private static void captureAnimTime() {
        long frame = IrisShadowBatchCollector.renderFrame();
        if (animTimeFrameStamp != frame) {
            var level = Minecraft.getInstance().level;
            long gameTime = level != null ? level.getGameTime() : 0L;
            animTicksMemo = (float) (gameTime - 1L);
            animPartialTickMemo = com.hbm_m.platform.RenderHooks.getPartialTick();
            animTimeFrameStamp = frame;
        }
    }

    /**
     * Whole-tick part of the animation clock: {@code (gameTime - 1)} as float32.
     * Carried SEPARATELY from the partial tick and uploaded as the .x of the
     * vec2 {@code uAnimTime}: integer ticks are exact in float32 up to 2^24,
     * but the old fused {@code (float)(gameTime - 1) + partialTick} collapsed
     * the partial tick into half-tick steps once the tick counter passed 2^22
     * (float32 spacing 0.5) - every tracked rig visibly juddered, its u
     * advancing only twice per tick (the assembler ring's "jerky spin",
     * 2026-10-07). The VSH reconstructs the elapsed time as
     * {@code (ticks - t0) + pt}: both operands of the subtraction are exact
     * integer floats, so it is exact and the partial tick survives.
     */
    public static float animTicks() {
        captureAnimTime();
        return animTicksMemo;
    }

    /** Partial-tick part of the animation clock (see {@link #animTicks()}). */
    public static float animPartialTick() {
        captureAnimTime();
        return animPartialTickMemo;
    }
}
