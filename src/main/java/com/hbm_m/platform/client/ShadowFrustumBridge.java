package com.hbm_m.platform.client;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.lang.reflect.Field;
import java.util.Map;
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

    private static final Map<Class<?>, Field> PLANES_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> COUNT_FIELD_CACHE = new ConcurrentHashMap<>();
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
     * @return true if planes were successfully extracted, false otherwise.
     */
    public static boolean extractShadowPlanes(@Nullable Object frustum, float[] out13Planes) {
        if (frustum == null) {
            frustum = getIrisShadowFrustum();
        }
        if (frustum == null || out13Planes == null || out13Planes.length < TOTAL_PLANE_FLOATS) {
            return false;
        }

        try {
            Class<?> clazz = frustum.getClass();
            Field planesField = getPlanesField(clazz);
            if (planesField == null) {
                return false;
            }

            Object planesObj = planesField.get(frustum);
            if (planesObj == null) {
                return false;
            }

            int planeCount = getPlaneCountInternal(clazz, frustum, planesObj);
            int count = Math.min(planeCount, MAX_SHADOW_PLANES);

            // Handle Oculus 1.20.1: org.joml.Vector4f[]
            if (planesObj instanceof org.joml.Vector4f[] vecArray) {
                for (int i = 0; i < count; i++) {
                    org.joml.Vector4f v = vecArray[i];
                    if (v != null) {
                        out13Planes[i * 4 + 0] = v.x;
                        out13Planes[i * 4 + 1] = v.y;
                        out13Planes[i * 4 + 2] = v.z;
                        out13Planes[i * 4 + 3] = v.w;
                    } else {
                        out13Planes[i * 4 + 0] = 0f;
                        out13Planes[i * 4 + 1] = 0f;
                        out13Planes[i * 4 + 2] = 0f;
                        out13Planes[i * 4 + 3] = 0f;
                    }
                }
                return true;
            }

            // Handle Iris 1.21.1: float[][]
            if (planesObj instanceof float[][] floatArray) {
                for (int i = 0; i < count; i++) {
                    float[] row = floatArray[i];
                    if (row != null && row.length >= 4) {
                        out13Planes[i * 4 + 0] = row[0];
                        out13Planes[i * 4 + 1] = row[1];
                        out13Planes[i * 4 + 2] = row[2];
                        out13Planes[i * 4 + 3] = row[3];
                    } else {
                        out13Planes[i * 4 + 0] = 0f;
                        out13Planes[i * 4 + 1] = 0f;
                        out13Planes[i * 4 + 2] = 0f;
                        out13Planes[i * 4 + 3] = 0f;
                    }
                }
                return true;
            }

            // Generic fallback via reflection array elements
            if (planesObj.getClass().isArray()) {
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
                return true;
            }
        } catch (Throwable ignored) {
            // Shield against any runtime reflection failure
        }
        return false;
    }

    /**
     * Returns the plane count declared on the frustum, or 0 if unresolvable.
     */
    public static int getPlaneCount(@Nullable Object frustum) {
        if (frustum == null) {
            frustum = getIrisShadowFrustum();
        }
        if (frustum == null) {
            return 0;
        }
        try {
            Class<?> clazz = frustum.getClass();
            Field planesField = getPlanesField(clazz);
            if (planesField == null) {
                return 0;
            }
            Object planesObj = planesField.get(frustum);
            if (planesObj == null) {
                return 0;
            }
            return getPlaneCountInternal(clazz, frustum, planesObj);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Checks if the given axis-aligned bounding box intersects the shadow frustum.
     */
    public static boolean isAabbVisibleInShadow(@Nullable Object frustum, double minX, double minY, double minZ,
                                               double maxX, double maxY, double maxZ) {
        if (frustum == null) {
            frustum = getIrisShadowFrustum();
        }
        if (frustum instanceof Frustum mcFrustum) {
            try {
                return mcFrustum.isVisible(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            } catch (Throwable ignored) {}
        }
        return true; // Conservative fallback: render shadow
    }

    /**
     * Tests a bounding sphere against the extracted shadow planes.
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
            float len = (float) Math.sqrt(px * px + py * py + pz * pz);
            if (len > 1e-6f) {
                float dist = (px * cx + py * cy + pz * cz + pw) / len;
                if (dist < -radius) {
                    return false; // Completely outside plane
                }
            }
        }
        return true;
    }

    @Nullable
    private static Field getPlanesField(Class<?> clazz) {
        return PLANES_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Class<?> curr = c;
            while (curr != null && curr != Object.class) {
                try {
                    Field f = curr.getDeclaredField("planes");
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {}
                curr = curr.getSuperclass();
            }
            return null;
        });
    }

    private static int getPlaneCountInternal(Class<?> clazz, Object frustum, Object planesObj) {
        Field countField = COUNT_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Class<?> curr = c;
            while (curr != null && curr != Object.class) {
                try {
                    Field f = curr.getDeclaredField("planeCount");
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {}
                curr = curr.getSuperclass();
            }
            return null;
        });

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
