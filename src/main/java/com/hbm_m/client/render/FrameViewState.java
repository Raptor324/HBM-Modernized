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
 * is factored into the uniform {@code ModelViewMat} = V = R_cam * T(-camPos). This makes
 * records invariant to camera motion (zero dirty traffic in a static scene - see
 * GpuSpanUploader) and removes inter-tick position lag.
 * <p>
 * Contract derived from vanilla sources (verified against 1.20.1 forge / 1.21.1 neoforge):
 * <ul>
 *   <li><b>1.20.1</b>: GameRenderer.renderLevel pushes R_cam (= Rz(roll)*Rx(pitch)*Ry(yaw+180))
 *       onto the POSE stack, which goes into LevelRenderer.renderLevel and is used by the
 *       BE loop (translate(bePos - cam) on top of R_cam). modelViewStack is not touched
 *       until AFTER_BLOCK_ENTITIES (pushed only in the translucent section) -
 *       getModelViewMatrix() is identity there. R_cam^-1 is available via
 *       RenderSystem.getInverseViewRotationMatrix().</li>
 *   <li><b>1.21.1</b>: the reverse - R_cam = frustumMatrix = rotation(camera.rotation().conjugate())
 *       is pushed onto modelViewStack (Matrix4fStack) BEFORE entities and lives until popMatrix
 *       (after AFTER_BLOCK_ENTITIES), while the BE loop uses a plain PoseStack
 *       (translate(bePos - cam), without R_cam).</li>
 * </ul>
 * In both cases composed = getModelViewMatrix()*pose = R_cam*T(rel)*local, so the
 * world transform = invViewRot*composed. Capture is lazy on first access per frame
 * (always inside renderLevel - the BE pass or later), keyed by the serial
 * {@link MdiRenderFrameGate}.
 */
public final class FrameViewState {

    private static final Matrix4f INV_VIEW_ROT = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static float camX, camY, camZ;
    private static float relCamX, relCamY, relCamZ;
    private static net.minecraft.core.BlockPos anchorOrigin = net.minecraft.core.BlockPos.ZERO;
    private static boolean anchorInitialized = false;
    private static long anchorGeneration = 0L;
    private static final double SQR_MAX_ANCHOR_DISTANCE = 256.0 * 256.0;

    private static boolean valid = false;
    private static long capturedSerial = -1L;

    private FrameViewState() {}

    /**
     * Cache reset. Called from emergency frame paths (exception before
     * {@code advanceAfterPresent}): the serial does not advance then, and without
     * the reset the next frame would silently reuse a stale camera.
     */
    public static void invalidate() {
        valid = false;
    }

    public static net.minecraft.core.BlockPos anchorOrigin() {
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
            onAnchorChanged();
            return true;
        }
        double dx = cameraPos.x - anchorOrigin.getX();
        double dy = cameraPos.y - anchorOrigin.getY();
        double dz = cameraPos.z - anchorOrigin.getZ();
        if (dx * dx + dy * dy + dz * dz > SQR_MAX_ANCHOR_DISTANCE) {
            net.minecraft.core.BlockPos oldAnchor = anchorOrigin;
            anchorOrigin = net.minecraft.core.BlockPos.containing(cameraPos.x, cameraPos.y, cameraPos.z);
            anchorGeneration++;
            com.hbm_m.main.MainRegistry.LOGGER.info("[Nucleus] Anchor origin shift: {} -> {} (drift {} m)",
                    oldAnchor, anchorOrigin, (int) Math.sqrt(dx * dx + dy * dy + dz * dz));
            onAnchorChanged();
            return true;
        }
        return false;
    }

    private static void onAnchorChanged() {
        MdiBatchCoordinator.onRenderOriginChanged();
        MdiGeometryAtlas atlas = MdiGeometryAtlas.peekOrNull();
        if (atlas != null && atlas.isReady()) {
            atlas.onRenderOriginChanged();
        }
        InstancedStaticPartRenderer.onRenderOriginChanged();
    }

    /** Render thread, inside renderLevel (BE pass or later stage events). */
    private static void capture() {
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

        capturedSerial = serial;
        valid = true;
    }

    /** R_cam^-1 (rotation without translation). Do not mutate the returned matrix. */
    public static Matrix4f inverseViewRotation() {
        capture();
        return INV_VIEW_ROT;
    }

    /**
     * V = R_cam * T(-relCam): anchor-relative coordinates -> view-space.
     * Set into the ModelViewMat uniform of instanced programs. Do not mutate the result.
     */
    public static Matrix4f viewMatrix() {
        capture();
        // invViewRot = R_cam^-1 -> transpose = R_cam (rotation), then T(-relCam) on the right:
        // V*v = R_cam*(v - relCam).
        VIEW.set(INV_VIEW_ROT).transpose().translate(-relCamX, -relCamY, -relCamZ);
        return VIEW;
    }

    public static float camX() { capture(); return camX; }
    public static float camY() { capture(); return camY; }
    public static float camZ() { capture(); return camZ; }

    public static float relCamX() { capture(); return relCamX; }
    public static float relCamY() { capture(); return relCamY; }
    public static float relCamZ() { capture(); return relCamZ; }
}
