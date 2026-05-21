package com.hbm_m.client.render.culling;

import org.joml.Matrix4f;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Лёгкий CPU AABB-vs-frustum культинг. Fallback в {@link OcclusionCullingHelper},
 * когда vanilla {@link net.minecraft.client.renderer.culling.Frustum} недоступен.
 *
 * <p>Стоимость: 6 dot-product сравнений на BE + одна квадратичная дистанция.
 * На 1000 BE это &lt;0.1 ms, против 7.8 ms у raycast-варианта.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class CpuFrustumCuller {

    /** 6 plane equations: nx, ny, nz, d. plane.dot(p)+d >= 0 inside. */
    private static final float[] PLANES = new float[6 * 4];
    private static volatile boolean planesValid = false;

    private CpuFrustumCuller() {}

    /**
     * Извлекает 6 плоскостей фрустума из viewProj-матрицы (Gribb/Hartmann).
     * Должен вызываться раз в кадр в render-thread с актуальной матрицей
     * проекции * вида (без перевода в block-space).
     */
    public static void updateFrustum(Matrix4f viewProj) {
        float m00 = viewProj.m00(), m01 = viewProj.m01(), m02 = viewProj.m02(), m03 = viewProj.m03();
        float m10 = viewProj.m10(), m11 = viewProj.m11(), m12 = viewProj.m12(), m13 = viewProj.m13();
        float m20 = viewProj.m20(), m21 = viewProj.m21(), m22 = viewProj.m22(), m23 = viewProj.m23();
        float m30 = viewProj.m30(), m31 = viewProj.m31(), m32 = viewProj.m32(), m33 = viewProj.m33();

        // Left:   row3 + row0
        setPlane(0, m03 + m00, m13 + m10, m23 + m20, m33 + m30);
        // Right:  row3 - row0
        setPlane(1, m03 - m00, m13 - m10, m23 - m20, m33 - m30);
        // Bottom: row3 + row1
        setPlane(2, m03 + m01, m13 + m11, m23 + m21, m33 + m31);
        // Top:    row3 - row1
        setPlane(3, m03 - m01, m13 - m11, m23 - m21, m33 - m31);
        // Near:   row3 + row2
        setPlane(4, m03 + m02, m13 + m12, m23 + m22, m33 + m32);
        // Far:    row3 - row2
        setPlane(5, m03 - m02, m13 - m12, m23 - m22, m33 - m32);

        planesValid = true;
    }

    private static void setPlane(int idx, float a, float b, float c, float d) {
        float invLen = 1.0f / (float) Math.sqrt(a * a + b * b + c * c);
        PLANES[idx * 4]     = a * invLen;
        PLANES[idx * 4 + 1] = b * invLen;
        PLANES[idx * 4 + 2] = c * invLen;
        PLANES[idx * 4 + 3] = d * invLen;
    }

    /** True если хотя бы одна корня AABB внутри/пересекает фрустум. */
    public static boolean isVisible(AABB box) {
        if (!planesValid) return true;
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        for (int i = 0; i < 6; i++) {
            float a = PLANES[i * 4];
            float b = PLANES[i * 4 + 1];
            float c = PLANES[i * 4 + 2];
            float d = PLANES[i * 4 + 3];
            // p-vertex (corner farthest along plane normal)
            float px = (a >= 0) ? maxX : minX;
            float py = (b >= 0) ? maxY : minY;
            float pz = (c >= 0) ? maxZ : minZ;
            if (a * px + b * py + c * pz + d < 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static boolean isVisibleWithDistance(AABB box, Vec3 cameraPos, double maxDistSq) {
        double cx = (box.minX + box.maxX) * 0.5 - cameraPos.x;
        double cy = (box.minY + box.maxY) * 0.5 - cameraPos.y;
        double cz = (box.minZ + box.maxZ) * 0.5 - cameraPos.z;
        double distSq = cx * cx + cy * cy + cz * cz;
        if (distSq < 16.0) return true;
        if (maxDistSq > 0 && distSq > maxDistSq) return false;
        return isVisible(box);
    }

    public static void invalidate() {
        planesValid = false;
    }

    public static boolean isReady() {
        return planesValid;
    }
}
