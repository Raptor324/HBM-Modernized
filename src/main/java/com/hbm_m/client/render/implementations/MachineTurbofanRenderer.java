package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Turbofan on the {@link MachineRenderers} factory: Body and Blades are per-frame
 * animator parts (Blades is the rotor, rotation around -Z at height 1.5 - the
 * original 1.7.10 transform), AfterburnerCold/Hot are mutually exclusive nozzle
 * variants (switched by whether the afterburner is burning). All per-frame
 * animators so the run shake in the block transform reaches every part.
 * Orientation is an explicit rotation map relative to the facing; the effect-zone
 * axis in the BE is aligned with it.
 * <p>
 * Nozzle glow: a "hot metal" overlay on the ENTIRE model - a per-instance RGBA
 * tint ({@code tintOverride}, overbright at maximum) with spatial falloff from
 * the nozzle cut toward the machine center ({@code tintFalloff} along model Z,
 * smoothstep in the shader) - no hard boundaries at part cuts; light is pushed
 * toward fullbright only on the nozzle parts. The value comes from the client
 * glowHeat in the BE: it smoothly approaches the target power (fuel/afterburner),
 * does not accumulate over run time, and fades out just as smoothly on shutdown.
 * Tint/falloff/light are per-instance data updated every frame without VBO
 * rebuilds and work identically under MDI and Iris.
 */
public final class MachineTurbofanRenderer {

    public static void register() {
        MachineRenderers.machine("turbofan", com.hbm_m.blockentity.ModBlockEntities.TURBOFAN_BE.get(),
                MachineTurbofanBlockEntity.class)
            // Tint on ALL parts of the model: an overlay only on the nozzle part gives a hard
            // boundary at the cut and looks ugly, so the color is blurred by a spatial
            // falloff along the model Z axis: full glow from -3.0 (a bit deeper than the
            // nozzle cut), fading smoothly to +1.0 (past the center) - the far half with
            // the fan stays clean.
            .tintOverride("Body", MachineTurbofanRenderer::nozzleTint)
            .tintOverride("Blades", MachineTurbofanRenderer::nozzleTint)
            .tintOverride("AfterburnerCold", MachineTurbofanRenderer::nozzleTint)
            .tintOverride("AfterburnerHot", MachineTurbofanRenderer::nozzleTint)
            .tintFalloff("Body", 2, -3.0F, 1.0F)
            .tintFalloff("Blades", 2, -3.0F, 1.0F)
            .tintFalloff("AfterburnerCold", 2, -3.0F, 1.0F)
            .tintFalloff("AfterburnerHot", 2, -3.0F, 1.0F)
            // Body/Blades are per-frame animator parts (NOT the static trust path
            // and NOT the tracked spinningPart): the block transform carries the
            // run shake - a static-trusted Body would freeze the jitter, and the
            // tracked rig records a frame-invariant static pose. The Blades
            // animator reproduces the ORIGINAL 1.7.10 renderer exactly:
            // T(0,1.5,0) . R(spin around -Z) . T(0,-1.5,0) with the angle
            // lerp(lastSpin, spin, partialTick) - same axis, pivot, direction
            // and momentum/2 deg/tick rate the tracked rig gave (360/(mom/2)
            // ticks per cycle = mom/2 deg/tick).
            .staticPart("Body", MachineTurbofanRenderer::animateStatic)
            .staticPart("Blades", MachineTurbofanRenderer::animateBlades)
            .part("AfterburnerCold", MachineTurbofanRenderer::animateCold)
            .part("AfterburnerHot", MachineTurbofanRenderer::animateHot)
            .itemParts("Body", "Blades", "AfterburnerCold")
            .blockTransform(MachineTurbofanRenderer::applyBlockTransform)
            .register();
    }

    private MachineTurbofanRenderer() {}

    // Nozzle glow (hot metal overlay).

    /**
     * Reference points of the color scale {heat, r, g, b}: from a light dark maroon
     * up to glowing orange. The last stop is &gt; 1 - overbright: the gray metal of
     * the texture actually GLOWS instead of just being tinted.
     */
    private static final float[][] HEAT_COLOR_STOPS = {
            {0.00F, 1.00F, 1.00F, 1.00F},
            {0.30F, 0.50F, 0.16F, 0.13F},
            {0.60F, 0.90F, 0.32F, 0.16F},
            {1.00F, 1.55F, 0.52F, 0.22F},
    };
    /** Scratch buffer for the tint (rendering is single-threaded; addInstance copies the value into the record immediately). */
    private static final float[] TINT_SCRATCH = new float[4];

    /** Nozzle glow 0..1; the animation tick advances idempotently (ticker and BER both call it). */
    private static float nozzleGlowHeat(MachineTurbofanBlockEntity be) {
        be.ensureClientAnimationTick();
        return be.getGlowHeat();
    }

    /**
     * RGBA nozzle tint following the HEAT_COLOR_STOPS scale; null = no tint (cold metal).
     * Alpha is the emission strength (heat 0..1): shaders push the lightmap toward
     * fullbright by alpha*gradient, so the glow spreads across the texture together
     * with the tint - like hot metal, without separate lightOverride parts.
     */
    private static float[] nozzleTint(MachineTurbofanBlockEntity be) {
        float heat = nozzleGlowHeat(be);
        if (heat <= 0.0F) return null;

        float[] prev = HEAT_COLOR_STOPS[0];
        float[] next = HEAT_COLOR_STOPS[HEAT_COLOR_STOPS.length - 1];
        for (int i = 1; i < HEAT_COLOR_STOPS.length; i++) {
            if (heat <= HEAT_COLOR_STOPS[i][0]) {
                next = HEAT_COLOR_STOPS[i];
                prev = HEAT_COLOR_STOPS[i - 1];
                break;
            }
        }
        float span = next[0] - prev[0];
        float t = span > 0.0F ? (heat - prev[0]) / span : 0.0F;
        for (int c = 0; c < 3; c++) {
            TINT_SCRATCH[c] = Mth.lerp(t, prev[c + 1], next[c + 1]);
        }
        TINT_SCRATCH[3] = heat;
        return TINT_SCRATCH;
    }

    // Block transform: same rotation map as the effect-zone axis.

    /**
     * Centering + facing rotation + the run shake. The ORIGINAL 1.7.10 renderer
     * has NO shake (RenderTurbofan only translates/rotates), so the amplitude is
     * calibrated small: an aesthetic "running engine" wobble of about a
     * hundredth of a block at full momentum, slightly stronger with the
     * afterburner installed (0.012 base + 0.006 per afterburner level, clamped
     * at 3, scaled by momentum/100). Deterministic per (render frame, position):
     * both the main and the shadow pass (and every degradation path that
     * re-applies the block transform) land on the SAME jitter within a frame.
     */
    private static void applyBlockTransform(MachineTurbofanBlockEntity be,
                                            com.hbm_m.client.render.LegacyAnimator animator) {
        animator.translate(0.5D, 0.0D, 0.5D);
        int momentum = be.getMomentum();
        if (momentum > 0) {
            double amplitude = (momentum / 100.0D)
                    * (0.012D + Math.min(be.getAfterburner(), 3) * 0.006D);
            long seed = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame() * 31L
                    + be.getBlockPos().asLong();
            animator.translate((hashUnit(seed) - 0.5D) * 2.0D * amplitude,
                    (hashUnit(seed ^ 0x9E3779B97F4A7C15L) - 0.5D) * 2.0D * amplitude,
                    0.0D);
        }
        animator.rotate(facingRotation(be.getBlockState().getValue(
                com.hbm_m.block.machines.MachineTurbofanBlock.FACING)), 0, 1, 0);
    }

    /** Deterministic [0, 1) hash of a long seed (SplitMix64 finalizer, top 28 bits). */
    private static double hashUnit(long s) {
        s ^= s >>> 33;
        s *= 0xFF51AFD7ED558CCDL;
        s ^= s >>> 33;
        return (s >>> 36) / (double) (1L << 28);
    }

    /** EAST 0 / NORTH 90 / WEST 180 / SOUTH 270 - the model intake points ClockWise from FACING. */
    private static float facingRotation(Direction direction) {
        return switch (direction) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            default -> 0.0F;
        };
    }

    // Animations.

    /** Static body: drawn every frame, no transform of its own (the run shake rides the block transform). */
    private static boolean animateStatic(MachineTurbofanBlockEntity be, float partialTick,
                                         long gameTime, PoseStack pose) {
        return true;
    }

    /**
     * Rotor: the ORIGINAL 1.7.10 renderer's exact transform - T(0,1.5,0)
     * . R(spin degrees around -Z) . T(0,-1.5,0), the angle being
     * lerp(lastSpin, spin, partialTick) of the client-side spin fields
     * (spin advances by momentum/2 degrees per tick, as in the original).
     */
    private static boolean animateBlades(MachineTurbofanBlockEntity be, float partialTick,
                                         long gameTime, PoseStack pose) {
        be.ensureClientAnimationTick();
        float angle = Mth.lerp(partialTick, be.getLastSpin(), be.getSpin());
        pose.translate(0.0F, 1.5F, 0.0F);
        pose.mulPose(new org.joml.Quaternionf().rotationZ((float) Math.toRadians(-angle)));
        pose.translate(0.0F, -1.5F, 0.0F);
        return true;
    }

    /** Cold nozzle: shown while the afterburner is not burning. */
    private static boolean animateCold(MachineTurbofanBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        return be.getAfterburner() <= 0;
    }

    /** Hot nozzle: shown only while the afterburner is burning. */
    private static boolean animateHot(MachineTurbofanBlockEntity be, float partialTick,
                                      long gameTime, PoseStack pose) {
        return be.getAfterburner() > 0;
    }
}
