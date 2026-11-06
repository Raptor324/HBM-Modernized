package com.hbm_m.client.render.shader;

import com.hbm_m.particle.nt.ParticleEngineNT;

/**
 * Tracks nuclear explosion sources and calculates continuous explosion physics.
 * Decouples explosion particle tracking and physics simulation from OpenGL rendering.
 *
 * <p>Implements continuous nanosecond timer tracking ({@link System#nanoTime()}),
 * bridging discrete 20 TPS server ticks into fluid rendering without frame-time
 * stepping. Computes Sedov-Taylor shockwave expansion, dynamic fireball ascent,
 * and continuous heat curves.</p>
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

    /** Current frame light sources: 4-tuples (x, y + dynamicLift, z, heat). */
    private static final float[] sources = new float[MAX_SOURCES * 4];

    /** Mushroom cloud scales of sources (NukeTorex.getScale). */
    private static final float[] srcScale = new float[MAX_SOURCES];

    /** Per-source light radius. */
    private static final float[] srcRadius = new float[MAX_SOURCES];

    /** Continuous shockwave parameters: pairs (radius blocks, fade [0..1]). */
    private static final float[] srcShock = new float[MAX_SOURCES * 2];

    /** Extra per-source parameters: 4-tuples (ageSec, scale, groundY, flashPulse). */
    private static final float[] srcExtra = new float[MAX_SOURCES * 4];

    private static int sourceCount;
    private static float timeSeconds;
    private static final long trackerStartNanos = System.nanoTime();

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
     * Updates continuous heat, fireball ascent, and Sedov-Taylor 3D shockwave expansion per frame.
     */
    private static void updateContinuousSources() {
        long now = System.nanoTime();
        timeSeconds = (float) ((now - trackerStartNanos) * 1.0e-9);
        boolean[] matchedSlot = new boolean[MAX_SOURCES];

        for (int i = 0; i < sourceCount; i++) {
            float sx = sources[i * 4];
            float groundY = sources[i * 4 + 1];
            float sz = sources[i * 4 + 2];
            float rawHeat = sources[i * 4 + 3];
            float scale = Math.max(0.5F, srcScale[i]);

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

            // Dynamic fireball ascent: starts low at detonation (~12 * scale) and rises into
            // the mushroom cap (~50 * scale) over the first 10 seconds.
            float ascentProgress = 1.0F - (float) Math.exp(-ageSec / (4.2F * Math.max(0.75F, scale)));
            float dynamicLift = (12.0F + (LIGHT_SOURCE_LIFT + 8.0F - 12.0F) * ascentProgress) * Math.max(0.75F, scale);
            sources[i * 4 + 1] = groundY + dynamicLift;

            float lifeFrac = Math.min(1.0F, ageSec / maxAgeSec);
            float scaleClamped = Math.max(0.75F, scale);
            float flashDecay = (float) Math.exp(-ageSec / (3.5F * scaleClamped));
            float afterglow = (float) Math.pow(1.0F - lifeFrac, 1.35);
            float continuousHeat = HEAT_SCALE * Math.min(1.0F, flashDecay * 0.45F + afterglow * 0.75F);
            sources[i * 4 + 3] = continuousHeat;

            // Double-pulse nuclear flash factor: intense initial spike (0..0.4s) + secondary thermal maximum (0.5..2.8s)
            float pulse1 = (float) Math.exp(-ageSec * 7.5F);
            float pulse2 = (float) Math.exp(-Math.pow((ageSec - 0.85F) / (1.35F * Math.sqrt(scale)), 2.0));
            float flashPulse = Math.min(1.0F, pulse1 * 0.85F + pulse2 * 0.95F);

            // Sedov-Taylor 3D shockwave expansion: longer duration so the 3D dome visibly sweeps across the world
            float waveDuration = 9.5F * (float) Math.pow(scale, 0.65F);
            float u = Math.min(1.45F, ageSec / Math.max(0.5F, waveDuration));
            float progress = (float) Math.pow(u + 0.012F, 0.68F) - (float) Math.pow(0.012F, 0.68F);
            float shockR = srcRadius[i] * 1.08F * progress;
            float shockFade = Math.max(0.0F, 1.0F - u * 0.72F);
            shockFade = shockFade * shockFade * Math.min(1.0F, ageSec * 8.0F);

            srcShock[i * 2] = shockR;
            srcShock[i * 2 + 1] = shockFade;

            srcExtra[i * 4] = ageSec;
            srcExtra[i * 4 + 1] = scale;
            srcExtra[i * 4 + 2] = groundY;
            srcExtra[i * 4 + 3] = flashPulse;
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

    public static float[] getSrcExtra() {
        return srcExtra;
    }

    public static float getTimeSeconds() {
        return timeSeconds;
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
