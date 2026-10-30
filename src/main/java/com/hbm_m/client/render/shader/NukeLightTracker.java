package com.hbm_m.client.render.shader;

import com.hbm_m.particle.nt.ParticleEngineNT;

/**
 * Tracks nuclear explosion sources and calculates continuous explosion physics.
 * Decouples explosion particle tracking and physics simulation from OpenGL rendering.
 *
 * <p>Implements continuous nanosecond timer tracking ({@link System#nanoTime()}),
 * bridging discrete 20 TPS server ticks into fluid rendering without frame-time
 * stepping. Computes Sedov-Taylor shockwave expansion and continuous heat curves.</p>
 */
public final class NukeLightTracker {

    private NukeLightTracker() {}

    /** Maximum simultaneous explosion light sources. */
    public static final int MAX_SOURCES = 4;

    /** Unit light radius, blocks: per-source radius = R_UNIT * scale * radiusScale. */
    public static final float R_UNIT = 768.0F;

    /** Heat scale normalization constant. */
    public static final float HEAT_SCALE = 50.0F;

    /** Actual maximum heat of NukeTorex (used to estimate age from rawHeat). */
    public static final float NUKE_TOREX_MAX_HEAT = 75.0F;

    /** Base vertical lift for the light source, blocks. */
    public static final float LIGHT_SOURCE_LIFT = 42.0F;

    /** Current frame light sources: 4-tuples (x, y + LIFT, z, heat). */
    private static final float[] sources = new float[MAX_SOURCES * 4];

    /** Mushroom cloud scales of sources (NukeTorex.getScale). */
    private static final float[] srcScale = new float[MAX_SOURCES];

    /** Per-source light radius. */
    private static final float[] srcRadius = new float[MAX_SOURCES];

    /** Continuous shockwave parameters: pairs (radius blocks, fade [0..1]). */
    private static final float[] srcShock = new float[MAX_SOURCES * 2];

    private static int sourceCount;

    // Continuous nanosecond timer tracking
    private static final float[] trackedX = new float[MAX_SOURCES];
    private static final float[] trackedZ = new float[MAX_SOURCES];
    private static final long[] trackedBirthNanos = new long[MAX_SOURCES];
    private static final boolean[] trackedAlive = new boolean[MAX_SOURCES];

    public static float radiusScale() {
        return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeLightRadiusScale;
    }

    /**
     * Collects live nuclear explosion sources from {@link ParticleEngineNT} and
     * performs continuous nanosecond age and shockwave tracking.
     *
     * @return count of active explosion sources
     */
    public static int collectAndTrackSources() {
        sourceCount = ParticleEngineNT.INSTANCE.collectNukeSources(sources, srcScale, MAX_SOURCES);
        if (sourceCount == 0) {
            for (int j = 0; j < MAX_SOURCES; j++) {
                trackedAlive[j] = false;
            }
            return 0;
        }
        updateContinuousSources();
        return sourceCount;
    }

    /**
     * Updates continuous heat and Sedov-Taylor shockwave expansion per frame.
     */
    private static void updateContinuousSources() {
        long now = System.nanoTime();
        boolean[] matchedSlot = new boolean[MAX_SOURCES];

        for (int i = 0; i < sourceCount; i++) {
            float sx = sources[i * 4];
            float sz = sources[i * 4 + 2];
            float rawHeat = sources[i * 4 + 3];
            float scale = Math.max(0.5F, srcScale[i]);

            float lift = LIGHT_SOURCE_LIFT * Math.max(0.75F, scale);
            sources[i * 4 + 1] += lift;
            srcRadius[i] = R_UNIT * scale * radiusScale();

            // Total lifetime of NukeTorex: getMaxAge() = 45 * 20 * scale ticks.
            float maxAgeSec = 45.0F * scale;

            int slot = -1;
            for (int j = 0; j < MAX_SOURCES; j++) {
                if (trackedAlive[j] && !matchedSlot[j]) {
                    float dx = trackedX[j] - sx;
                    float dz = trackedZ[j] - sz;
                    if (dx * dx + dz * dz < 64.0F) {
                        slot = j;
                        break;
                    }
                }
            }
            if (slot == -1) {
                for (int j = 0; j < MAX_SOURCES; j++) {
                    if (!trackedAlive[j] && !matchedSlot[j]) {
                        slot = j;
                        break;
                    }
                }
                if (slot == -1) {
                    slot = i;
                }
                trackedAlive[slot] = true;
                trackedX[slot] = sx;
                trackedZ[slot] = sz;
                float normH = Math.max(0.0F, Math.min(1.0F, rawHeat / NUKE_TOREX_MAX_HEAT));
                float estAgeSec = rawHeat >= NUKE_TOREX_MAX_HEAT - 1.0F ? 0.0F : (1.0F - normH) * maxAgeSec;
                trackedBirthNanos[slot] = now - (long) (estAgeSec * 1.0e9F);
            } else {
                trackedX[slot] = sx;
                trackedZ[slot] = sz;
            }
            matchedSlot[slot] = true;

            float ageSec = Math.max(0.0F, (now - trackedBirthNanos[slot]) * 1.0e-9F);
            float serverAgeSec = (1.0F - Math.max(0.0F, Math.min(1.0F, rawHeat / NUKE_TOREX_MAX_HEAT))) * maxAgeSec;
            if (Math.abs(ageSec - serverAgeSec) > 2.5F) {
                trackedBirthNanos[slot] = now - (long) (serverAgeSec * 1.0e9F);
                ageSec = serverAgeSec;
            }

            float continuousHeat = Math.max(0.0F, HEAT_SCALE * (1.0F - ageSec / maxAgeSec));
            sources[i * 4 + 3] = continuousHeat;

            // Sedov-Taylor shockwave non-linear expansion
            float waveDuration = 5.5F * (float) Math.pow(scale, 0.65F);
            float u = Math.min(1.35F, ageSec / Math.max(0.5F, waveDuration));
            float progress = (float) Math.pow(u + 0.015F, 0.72F) - (float) Math.pow(0.015F, 0.72F);
            float shockR = srcRadius[i] * 1.05F * progress;
            float shockFade = Math.max(0.0F, 1.0F - u * 0.78F);
            shockFade = shockFade * shockFade * Math.min(1.0F, ageSec * 6.0F);

            srcShock[i * 2] = shockR;
            srcShock[i * 2 + 1] = shockFade;
        }

        for (int j = 0; j < MAX_SOURCES; j++) {
            if (!matchedSlot[j]) {
                trackedAlive[j] = false;
            }
        }
    }

    public static int getSourceCount() {
        return sourceCount;
    }

    public static float[] getSources() {
        return sources;
    }

    public static float[] getSrcRadius() {
        return srcRadius;
    }

    public static float[] getSrcShock() {
        return srcShock;
    }

    public static void reset() {
        sourceCount = 0;
        for (int j = 0; j < MAX_SOURCES; j++) {
            trackedAlive[j] = false;
        }
    }

    public static String src0String() {
        if (sourceCount <= 0) {
            return "none";
        }
        return "(" + sources[0] + ", " + sources[1] + ", " + sources[2] + ", heat=" + sources[3] + ")";
    }

    public static double camDist0(float camX, float camY, float camZ) {
        if (sourceCount <= 0) {
            return -1.0;
        }
        double dx = sources[0] - camX;
        double dy = sources[1] - camY;
        double dz = sources[2] - camZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
