package com.hbm_m.platform.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Cross-version platform bridge for shadow frustum plane extraction between
 * Oculus (Forge 1.20.1, where {@code AdvancedShadowCullingFrustum.planes} is {@code org.joml.Vector4f[]})
 * and Iris (NeoForge 1.21.1, where {@code AdvancedShadowCullingFrustum.planes} is {@code float[][]}).
 * <p>
 * Completely shielded with safe reflection caching so zero reflection exceptions
 * or ClassNotFoundErrors escape to the render loop.
 *
 * @credit Oculus / Iris
 */
@OnlyIn(Dist.CLIENT)
public final class ShadowFrustumBridge {

    public static final int MAX_SHADOW_PLANES = 13;
    public static final int FLOATS_PER_PLANE = 4;
    public static final int TOTAL_PLANE_FLOATS = MAX_SHADOW_PLANES * FLOATS_PER_PLANE; // 52

    // Optional в значении: ConcurrentHashMap не хранит null — без обёртки класс фрустума
    // без поля "planes" перелезал бы через getDeclaredField/NoSuchFieldException КАЖДЫЙ кадр.
    private static final Map<Class<?>, Optional<Field>> PLANES_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Field>> COUNT_FIELD_CACHE = new ConcurrentHashMap<>();
    private static volatile Field shadowRendererFrustumField = null;
    private static volatile boolean shadowRendererFrustumLookedUp = false;

    private ShadowFrustumBridge() {}

    /**
     * Retrieves the current Iris/Oculus shadow frustum instance from
     * {@code net.irisshaders.iris.shadows.ShadowRenderer.FRUSTUM} if available.
     */
    @Nullable
    public static Object getIrisShadowFrustum() {
        if (!shadowRendererFrustumLookedUp) {
            synchronized (ShadowFrustumBridge.class) {
                if (!shadowRendererFrustumLookedUp) {
                    shadowRendererFrustumLookedUp = true;
                    try {
                        Class<?> srClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
                        Field f = srClass.getDeclaredField("FRUSTUM");
                        f.setAccessible(true);
                        shadowRendererFrustumField = f;
                    } catch (Throwable ignored) {
                        shadowRendererFrustumField = null;
                    }
                }
            }
        }
        if (shadowRendererFrustumField != null) {
            try {
                return shadowRendererFrustumField.get(null);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    /**
     * Extracts up to 13 shadow planes into the provided 52-float array.
     * Works seamlessly across:
     * <ul>
     *   <li>Oculus 1.20.1 Forge: {@code Vector4f[] planes}</li>
     *   <li>Iris 1.21.1 NeoForge: {@code float[][] planes}</li>
     * </ul>
     *
     * @param frustum the shadow frustum instance (e.g. from {@link #getIrisShadowFrustum()}); if null, automatically resolved.
     * @param out13Planes output float array of length >= 52 (13 planes * 4 components: nx, ny, nz, d).
     * @return the number of extracted (normalized) planes, 0 on failure.
     */
    public static int extractShadowPlanes(@Nullable Object frustum, float[] out13Planes) {
        if (frustum == null) {
            frustum = getIrisShadowFrustum();
        }
        if (frustum == null || out13Planes == null || out13Planes.length < TOTAL_PLANE_FLOATS) {
            return 0;
        }

        try {
            // Предзаливка нулями: null-элементы и неизвестные типы фоллбэка не оставляют
            // stale-данных от предыдущего кадра
            java.util.Arrays.fill(out13Planes, 0, TOTAL_PLANE_FLOATS, 0f);

            Class<?> clazz = frustum.getClass();
            Field planesField = getPlanesField(clazz);
            if (planesField == null) {
                return 0;
            }

            Object planesObj = planesField.get(frustum);
            if (planesObj == null) {
                return 0;
            }

            int planeCount = getPlaneCountInternal(clazz, frustum, planesObj);
            int count = Math.min(planeCount, MAX_SHADOW_PLANES);

            boolean success = false;
            // Handle Oculus 1.20.1: org.joml.Vector4f[]
            if (planesObj instanceof org.joml.Vector4f[] vecArray) {
                for (int i = 0; i < count; i++) {
                    org.joml.Vector4f v = vecArray[i];
                    if (v != null) {
                        out13Planes[i * 4 + 0] = v.x;
                        out13Planes[i * 4 + 1] = v.y;
                        out13Planes[i * 4 + 2] = v.z;
                        out13Planes[i * 4 + 3] = v.w;
                    }
                }
                success = true;
            } else if (planesObj instanceof float[][] floatArray) {
                // Handle Iris 1.21.1: float[][]
                for (int i = 0; i < count; i++) {
                    float[] row = floatArray[i];
                    if (row != null && row.length >= 4) {
                        out13Planes[i * 4 + 0] = row[0];
                        out13Planes[i * 4 + 1] = row[1];
                        out13Planes[i * 4 + 2] = row[2];
                        out13Planes[i * 4 + 3] = row[3];
                    }
                }
                success = true;
            } else if (planesObj.getClass().isArray()) {
                // Generic fallback via reflection array elements
                int len = Math.min(java.lang.reflect.Array.getLength(planesObj), count);
                for (int i = 0; i < len; i++) {
                    Object elem = java.lang.reflect.Array.get(planesObj, i);
                    if (elem instanceof org.joml.Vector4f v) {
                        out13Planes[i * 4 + 0] = v.x;
                        out13Planes[i * 4 + 1] = v.y;
                        out13Planes[i * 4 + 2] = v.z;
                        out13Planes[i * 4 + 3] = v.w;
                    } else if (elem instanceof float[] row && row.length >= 4) {
                        out13Planes[i * 4 + 0] = row[0];
                        out13Planes[i * 4 + 1] = row[1];
                        out13Planes[i * 4 + 2] = row[2];
                        out13Planes[i * 4 + 3] = row[3];
                    }
                }
                success = true;
            }

            if (success) {
                // Плоскости нормализуются ОДИН РАЗ здесь: isSphereVisibleInShadow тогда
                // чистое скалярное произведение — без sqrt/деления до 13 раз на сферу в кадре.
                for (int i = 0; i < count; i++) {
                    int o = i * 4;
                    float len = (float) Math.sqrt(out13Planes[o] * out13Planes[o]
                            + out13Planes[o + 1] * out13Planes[o + 1]
                            + out13Planes[o + 2] * out13Planes[o + 2]);
                    if (len > 1e-6f) {
                        out13Planes[o] /= len;
                        out13Planes[o + 1] /= len;
                        out13Planes[o + 2] /= len;
                        out13Planes[o + 3] /= len;
                    }
                }
                return count;
            }
        } catch (Throwable ignored) {
            // Shield against any runtime reflection failure
        }
        return 0;
    }

    /**
     * Checks the given cached {@link AABB} without allocating a new one per frame
     * (callers holding a cached render box use this overload).
     */
    public static boolean isAabbVisibleInShadow(@Nullable Object frustum, AABB aabb) {
        if (aabb == null) {
            return true;
        }
        if (frustum == null) {
            frustum = getIrisShadowFrustum();
        }
        if (frustum instanceof Frustum mcFrustum) {
            try {
                return mcFrustum.isVisible(aabb);
            } catch (Throwable ignored) {}
        }
        return true; // Conservative fallback: render shadow
    }

    /**
     * Tests a bounding sphere against the extracted shadow planes.
     * Требует НОРМАЛИЗОВАННЫЕ плоскости из {@link #extractShadowPlanes} (нормализация
     * выполняется там один раз при извлечении) — здесь только скалярное произведение.
     */
    public static boolean isSphereVisibleInShadow(float[] planes, int planeCount,
                                                 float cx, float cy, float cz, float radius) {
        if (planes == null || planeCount <= 0) {
            return true;
        }
        int count = Math.min(planeCount, MAX_SHADOW_PLANES);
        for (int i = 0; i < count; i++) {
            float px = planes[i * 4 + 0];
            float py = planes[i * 4 + 1];
            float pz = planes[i * 4 + 2];
            float pw = planes[i * 4 + 3];
            if (px * cx + py * cy + pz * cz + pw < -radius) {
                return false; // Completely outside plane
            }
        }
        return true;
    }

    @Nullable
    private static Field findFieldInHierarchy(Class<?> clazz, String name) {
        Class<?> curr = clazz;
        while (curr != null && curr != Object.class) {
            try {
                Field f = curr.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
            curr = curr.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Field getPlanesField(Class<?> clazz) {
        return PLANES_FIELD_CACHE.computeIfAbsent(clazz, c -> Optional.ofNullable(findFieldInHierarchy(c, "planes"))).orElse(null);
    }

    private static int getPlaneCountInternal(Class<?> clazz, Object frustum, Object planesObj) {
        Field countField = COUNT_FIELD_CACHE.computeIfAbsent(clazz, c -> Optional.ofNullable(findFieldInHierarchy(c, "planeCount"))).orElse(null);

        if (countField != null) {
            try {
                return countField.getInt(frustum);
            } catch (Throwable ignored) {}
        }

        if (planesObj.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(planesObj);
        }
        return 0;
    }
}
