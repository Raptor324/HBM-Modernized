package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * One pending instance enqueue - the single argument of
 * {@link InstancedStaticPartRenderer#addInstance} and
 * {@link com.hbm_m.client.render.machine.MachinePartRenderer#enqueue}.
 * <p>
 * Replaces the former global static {@code pending*} fields of
 * InstancedStaticPartRenderer (animParams, fadeMode, staticPose, lightBbox)
 * that were armed before the call and consumed inside it. Statics leaked into
 * an UNRELATED record whenever an exception fired between the setters and
 * addInstance - the 2026-10-09 "parts teleport away" bug. With an explicit
 * request there is nothing to leak: a degraded/throwing enqueue simply leaves
 * the request unread, and the next {@link #obtain()} resets every field.
 * <p>
 * One reusable instance per render thread (rendering is single-threaded;
 * obtain() never allocates after the first call). The BER fills the request
 * per part, then hands it over - the record contents written per instance are
 * identical to the previous pending-fields pipeline.
 */
@OnlyIn(Dist.CLIENT)
public final class PartEnqueueRequest {

    private static final ThreadLocal<PartEnqueueRequest> INSTANCE =
            ThreadLocal.withInitial(PartEnqueueRequest::new);

    /** Returns the render thread's reusable request, reset to defaults. */
    public static PartEnqueueRequest obtain() {
        PartEnqueueRequest rq = INSTANCE.get();
        rq.reset();
        return rq;
    }

    private PartEnqueueRequest() {
    }

    private void reset() {
        poseStack = null;
        packedLight = 0;
        blockPos = null;
        blockEntity = null;
        bufferSource = null;
        sharedCornerUV8 = null;
        uvRect = null;
        tint = null;
        grad = null;
        animP0 = 0f;
        animP1 = 0f;
        animP2 = 0f;
        animP3 = 0f;
        jointIndex = -1;
        fadeMode = InstancedStaticPartRenderer.FADE_MODE_STATIC;
        staticPose = false;
        lightBbox = null;
        clipPlanes = null;
    }

    // -- Draw call arguments (the former addInstance parameter list) ------

    /** Stack with the block transform + part animator (composed pose). */
    public PoseStack poseStack;
    public int packedLight;
    public BlockPos blockPos;
    public @Nullable BlockEntity blockEntity;
    public @Nullable MultiBufferSource bufferSource;
    /** Shared 16-float 8-corner light sample of the machine, or null (per-part sampling). */
    public @Nullable float[] sharedCornerUV8;
    /** {u0,v0,du,dv} atlas remap of a sprite-local VBO, or null (atlas UVs). */
    public @Nullable float[] uvRect;
    /** {r,g,b,a} per-instance color multiplier, or null (white). */
    public @Nullable float[] tint;
    /** {axis, fullCoord, zeroCoord, unused} tint falloff config, or null/axis&lt;0 (off). */
    public @Nullable float[] grad;
    /** Clipping planes {A,B,C,D} (A*x + B*y + C*z + D >= 0 inside), or null (no clipping). */
    public @Nullable float[][] clipPlanes;

    // -- Former pending* statics -------------------------------------------

    /** PACKED binary16 record words (the rig's encodeAnimParams output). */
    public float animP0;
    public float animP1;
    public float animP2;
    public float animP3;
    /** Joint chain base index; -1 = regular (CPU-animated/static) part. */
    public int jointIndex = -1;
    /** Record fade MODE (float 13) - see InstancedStaticPartRenderer.FADE_MODE_*. */
    public float fadeMode = InstancedStaticPartRenderer.FADE_MODE_STATIC;
    /** Frame-invariant pose: addInstance may skip the convert + posQuat compare prefix. */
    public boolean staticPose;
    /** Machine physical bounds paired with {@link #sharedCornerUV8} for spatial lighting; null = objBbox. */
    public @Nullable float[] lightBbox;

    /**
     * Sets the PACKED joint chain parameters (parametric GPU animation).
     * jointIndex &ge; 0 counts a parametric enqueue (F3 diagnostics).
     */
    public void setAnimParams(float p0, float p1, float p2, float p3, int jointIndexIn) {
        animP0 = p0;
        animP1 = p1;
        animP2 = p2;
        animP3 = p3;
        jointIndex = jointIndexIn;
        // Contract: a parametric record is being enqueued (jointIndex < 0 = regular part).
        if (jointIndexIn >= 0) {
            NucleusDebug.recordParametricEnqueue();
        }
    }
}
