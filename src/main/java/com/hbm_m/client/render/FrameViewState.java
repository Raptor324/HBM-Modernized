package com.hbm_m.client.render;

import org.joml.Matrix4f;

import com.hbm_m.client.render.culling.MdiRenderFrameGate;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
        capturedSerial = serial;
        valid = true;
    }

    /** R_cam⁻¹ (ротация без трансляции). Не мутировать возвращаемую матрицу. */
    public static Matrix4f inverseViewRotation() {
        capture();
        return INV_VIEW_ROT;
    }

    /**
     * V = R_cam · T(-camPos): мировые координаты → view-space.
     * Кладётся в uniform ModelViewMat инстанс-программ. Не мутировать результат.
     */
    public static Matrix4f viewMatrix() {
        capture();
        // invViewRot = R_cam⁻¹ → transpose = R_cam (ротация), затем T(-cam) справа:
        // V·v = R_cam·(v - camPos).
        VIEW.set(INV_VIEW_ROT).transpose().translate(-camX, -camY, -camZ);
        return VIEW;
    }

    public static float camX() { capture(); return camX; }
    public static float camY() { capture(); return camY; }
    public static float camZ() { capture(); return camZ; }
}
