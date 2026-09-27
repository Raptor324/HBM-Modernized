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
 * Покадровый снимок камеро-зависимых величин для мировых координат инстанс-записей.
 * <p>
 * Инстанс-записи (InstPos/InstRot) хранят МИРОВЫЕ позицию/поворот — камера
 * вынесена в uniform {@code ModelViewMat} = V = R_cam · T(-camPos). Это делает
 * записи инвариантными к движению камеры (нулевой dirty-поток при статичной
 * сцене — см. GpuSpanUploader) и убирает межтиковый лаг позиций.
 * <p>
 * Контракт по ванильным исходникам (проверено по 1.20.1 forge / 1.21.1 neoforge):
 * <ul>
 *   <li><b>1.20.1</b>: GameRenderer.renderLevel пушит R_cam (= Rz(roll)·Rx(pitch)·Ry(yaw+180))
 *       в ПОЗИЦИОННЫЙ poseStack, который идёт в LevelRenderer.renderLevel и используется
 *       BE-циклом (translate(bePos - cam) поверх R_cam). modelViewStack ДО
 *       AFTER_BLOCK_ENTITIES не трогается (пуш только в секции translucent) —
 *       getModelViewMatrix() там identity. R_cam⁻¹ доступен через
 *       RenderSystem.getInverseViewRotationMatrix().</li>
 *   <li><b>1.21.1</b>: наоборот — R_cam = frustumMatrix = rotation(camera.rotation().conjugate())
 *       пушится в modelViewStack (Matrix4fStack) ПЕРЕД сущностями и живёт до popMatrix
 *       (после AFTER_BLOCK_ENTITIES), а BE-цикл ходит по чистому PoseStack
 *       (translate(bePos - cam), без R_cam).</li>
 * </ul>
 * В обоих случаях composed = getModelViewMatrix()·pose = R_cam·T(rel)·local, поэтому
 * мировой трансформ = invViewRot·composed. Захват делается лениво при первом обращении
 * за кадр (всегда внутри renderLevel — BE-проход или позже), ключ — serial
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
     * Сброс кеша. Вызывается из аварийных путей кадра (исключение до
     * {@code advanceAfterPresent}): serial при этом не растёт, и без сброса
     * следующий кадр молча переиспользовал бы протухшую камеру.
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

    /** Render thread, внутри renderLevel (BE-проход или stage-события позже). */
    private static void capture() {
        long serial = MdiRenderFrameGate.currentSerial();
        if (valid && capturedSerial == serial) {
            return;
        }
        // invViewRot = R_cam⁻¹ (чистая ротация).
        //? if < 1.21.1 {
        INV_VIEW_ROT.set(RenderSystem.getInverseViewRotationMatrix());
        //?} else {
        /*// rotation(camera.rotation()) = R_cam⁻¹: frustumMatrix ванили =
        // rotation(rotation().conjugate()) = R_cam, т.е. quaternion уже несёт
        // ОБРАТНУЮ view-ротацию — дополнительный .invert() здесь НЕ нужен.
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

    /** R_cam⁻¹ (ротация без трансляции). Не мутировать возвращаемую матрицу. */
    public static Matrix4f inverseViewRotation() {
        capture();
        return INV_VIEW_ROT;
    }

    /**
     * V = R_cam · T(-relCam): anchor-relative координаты → view-space.
     * Кладётся в uniform ModelViewMat инстанс-программ. Не мутировать результат.
     */
    public static Matrix4f viewMatrix() {
        capture();
        // invViewRot = R_cam⁻¹ → transpose = R_cam (ротация), затем T(-relCam) справа:
        // V·v = R_cam·(v - relCam).
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
