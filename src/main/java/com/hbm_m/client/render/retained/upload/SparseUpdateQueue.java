package com.hbm_m.client.render.retained.upload;

import com.hbm_m.client.render.FrameViewState;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.client.render.retained.DirtyMask;
import com.hbm_m.client.render.retained.MachineInstanceHandle;
import com.hbm_m.client.render.retained.RetainedInstanceGraph;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Batches, coalesces, and uploads sparse instance slot updates.
 *
 * <p><b>Upload path retired.</b> The retained graph's global backing buffer was
 * dead VRAM: nothing ever bound or drew it - real rendering streams through
 * {@code spec.syncInstanceRecord} -> {@code InstancedStaticPartRenderer} ->
 * {@code MdiBatchCoordinator} -> {@code MdiGeometryAtlas}. The 2 MB off-heap
 * staging buffer, the dirty-op arrays and the GL upload paths are gone; the
 * enqueue* methods are API stubs. What remains alive here is the RECORD
 * BOOKKEEPING: {@code enqueueInitialBake} / {@code enqueueUpdate} still sample
 * light, pose parts and push records into the per-machine CPU-side copies via
 * {@code MachineSpec.syncInstanceRecord} / {@code syncInstanceSlice}, which is
 * what {@code MachineBer.recordMatchesBuffer} compares against.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class SparseUpdateQueue {

    private final InstanceSlotAllocator allocator;
    private int pendingCount = 0;

    // Thread-local scratch for baking records (render thread only)
    private static final ThreadLocal<float[]> SCRATCH_RECORD = ThreadLocal.withInitial(() -> new float[32]);
    private static final ThreadLocal<Vector3f> SCRATCH_VEC = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<Quaternionf> SCRATCH_QUAT = ThreadLocal.withInitial(Quaternionf::new);
    private static final ThreadLocal<PoseStack> SCRATCH_POSE = ThreadLocal.withInitial(PoseStack::new);
    // Separate pose stack for sampleMachineLight: it runs while the caller's
    // scratch pose may already carry the machine's block transform.
    private static final ThreadLocal<PoseStack> SCRATCH_LIGHT_POSE = ThreadLocal.withInitial(PoseStack::new);
    private static final ThreadLocal<float[]> SCRATCH_LIGHT8 = ThreadLocal.withInitial(() -> new float[16]);
    private static final ThreadLocal<float[]> SCRATCH_LIGHT4 = ThreadLocal.withInitial(() -> new float[4]);
    private static final ThreadLocal<float[]> SCRATCH_BBOX = ThreadLocal.withInitial(() -> new float[6]);
    private static final ThreadLocal<float[]> SCRATCH_PARAMS =
            ThreadLocal.withInitial(() -> new float[MachineSpec.CHAIN_PARAM_BUDGET]);
    private static final ThreadLocal<float[]> SCRATCH_ANIM = ThreadLocal.withInitial(() -> new float[4]);
    private static final ThreadLocal<float[]> SCRATCH_TRANS = ThreadLocal.withInitial(() -> new float[5]);

    public SparseUpdateQueue(InstanceSlotAllocator allocator) {
        this.allocator = allocator;
    }

    /** No-op: the retained graph has no GPU upload path (see class javadoc). */
    public synchronized void enqueueSlot(int slotIndex, long sourceData128Bytes) {
    }

    /** No-op: the retained graph has no GPU upload path (see class javadoc). */
    public synchronized void enqueueSlotFloats(int slotIndex, float[] floats32) {
    }

    /** No-op: the retained graph has no GPU upload path (see class javadoc). */
    public synchronized void enqueueSlotSlice(int slotIndex, int floatOffset, float[] data) {
    }

    /** No-op: the retained graph has no GPU upload path (see class javadoc). */
    public synchronized void enqueueSlotPartial(int slotIndex, int floatOffset, float[] data) {
    }

    private static void sampleMachineLight(BlockEntity be, MachineInstanceHandle handle, float[] out4Floats) {
        sampleMachineLight(be, handle, out4Floats, SCRATCH_BBOX.get());
    }

    private static void sampleMachineLight(BlockEntity be, MachineInstanceHandle handle, float[] out4Floats, @org.jetbrains.annotations.Nullable float[] outObjBbox) {
        if (be == null || be.getLevel() == null) {
            float zeroFloat = Float.intBitsToFloat(0);
            out4Floats[0] = zeroFloat;
            out4Floats[1] = zeroFloat;
            out4Floats[2] = zeroFloat;
            out4Floats[3] = zeroFloat;
            return;
        }
        if (outObjBbox == null) {
            outObjBbox = SCRATCH_BBOX.get();
        }
        BlockPos bp = be.getBlockPos();
        net.minecraft.world.phys.AABB localBounds = com.hbm_m.client.render.RenderBoundsCache.getLocalPhysicalBounds(be);
        if (localBounds != null) {
            outObjBbox[0] = (float) localBounds.minX;
            outObjBbox[1] = (float) localBounds.minY;
            outObjBbox[2] = (float) localBounds.minZ;
            outObjBbox[3] = (float) localBounds.maxX;
            outObjBbox[4] = (float) localBounds.maxY;
            outObjBbox[5] = (float) localBounds.maxZ;
        } else {
            // Model space is centered on the block center (translate(0.5,0,0.5)):
            // keep this aligned with RenderBoundsCache.computeLocalPhysicalBounds
            // and the bake fallback below (both bake paths sample through here).
            outObjBbox[0] = -0.5f; outObjBbox[1] = 0f; outObjBbox[2] = -0.5f;
            outObjBbox[3] = 0.5f; outObjBbox[4] = 1f; outObjBbox[5] = 0.5f;
        }
        // Sample the 8-corner light grid through the machine's REAL block transform
        // (spec blockTransform / facingResolver), not a hardcoded HORIZONTAL_FACING
        // rotation - the bake path (enqueueInitialBake) poses geometry through the
        // same transform, so the light corners must live in the same model space.
        PoseStack lightPoseStack = SCRATCH_LIGHT_POSE.get();
        lightPoseStack.setIdentity();
        LegacyAnimator lightAnimator = LegacyAnimator.create(lightPoseStack);
        try {
            @SuppressWarnings("rawtypes")
            MachineSpec lightSpec = com.hbm_m.client.render.retained.RetainedInstanceGraph.get().getSpec(be.getType());
            if (lightSpec != null && lightSpec.blockTransform() != null) {
                lightSpec.blockTransform().apply(be, lightAnimator);
            } else {
                Direction f = Direction.NORTH;
                if (lightSpec != null && lightSpec.facingResolver() != null) {
                    f = (Direction) lightSpec.facingResolver().apply(be);
                } else {
                    var state = be.getBlockState();
                    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                        f = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                    }
                }
                if (f == null) f = Direction.NORTH;
                lightAnimator.setupBlockTransform(f);
            }
        } catch (Throwable ignored) {}
        Matrix4f localPose = lightPoseStack.last().pose();
        int packedLight = LightSampleCache.getOrSamplePacked(be, 0);
        float[] light8 = SCRATCH_LIGHT8.get();
        LightSampleCache.getOrSample8(be, 0L, outObjBbox, bp, localPose, packedLight, light8);

        out4Floats[0] = Float.intBitsToFloat(InstancedStaticPartRenderer.packLightWord(light8[0], light8[1], light8[2], light8[3]));
        out4Floats[1] = Float.intBitsToFloat(InstancedStaticPartRenderer.packLightWord(light8[4], light8[5], light8[6], light8[7]));
        out4Floats[2] = Float.intBitsToFloat(InstancedStaticPartRenderer.packLightWord(light8[8], light8[9], light8[10], light8[11]));
        out4Floats[3] = Float.intBitsToFloat(InstancedStaticPartRenderer.packLightWord(light8[12], light8[13], light8[14], light8[15]));
    }

    /**
     * Fills InstColor (floats 20..23) and GradParams (floats 24..27) of a record
     * exactly like the CPU-path {@link InstancedStaticPartRenderer#addInstance}:
     * no tint - RGB white, alpha 0 (the block_lit shader treats InstColor.a as
     * part heat/glow); no falloff - GradParams.x &lt; 0 (off); GradParams.w -
     * joint-chain base index of a GPU-parametric rig, &lt;0 = no parametrics
     * (the VSH joint block stays off at w &gt;= 0). Without this the retained
     * bake diverged from the streamed path: a=1 lit every part as glowing and
     * w=0 falsely entered the parametric joint block.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeTintAndGrad(float[] rec, MachineSpec spec,
                                         MachineSpec.PartDef partDef, BlockEntity be) {
        float tr = 1.0f, tg = 1.0f, tb = 1.0f, ta = 0.0f;
        float g0 = -1.0f, g1 = 0.0f, g2 = 0.0f;
        int jointIdx = -1;
        if (partDef != null) {
            if (spec != null) {
                float[] tint = spec.resolveTint(partDef, be);
                if (tint != null && tint.length >= 4) {
                    tr = tint[0];
                    tg = tint[1];
                    tb = tint[2];
                    ta = tint[3];
                }
            }
            float[] falloff = partDef.tintFalloff();
            if (falloff != null && falloff.length >= 4 && falloff[0] >= 0f) {
                g0 = falloff[0];
                g1 = falloff[1];
                g2 = falloff[2];
            }
            if (partDef.parametric() != null) {
                // Same base index the streamed path derives in MachineBer.jointIndexFor.
                try {
                    jointIdx = partDef.parametric().registerSpecs();
                } catch (Throwable ignored) {}
            }
        }
        rec[20] = tr;
        rec[21] = tg;
        rec[22] = tb;
        rec[23] = ta;
        rec[24] = g0;
        rec[25] = g1;
        rec[26] = g2;
        rec[27] = (float) jointIdx;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends BlockEntity> void enqueueInitialBake(MachineInstanceHandle handle, BlockEntity be, MachineSpec<T> spec) {
        if (handle == null || be == null || spec == null) return;
        int[] slots = handle.getSlotIndices();
        if (slots == null || slots.length == 0) return;

        BlockPos bp = be.getBlockPos();
        BlockPos anchor = FrameViewState.anchorOrigin();
        float beRelX = (float) (bp.getX() - anchor.getX());
        float beRelY = (float) (bp.getY() - anchor.getY());
        float beRelZ = (float) (bp.getZ() - anchor.getZ());

        PoseStack poseStack = SCRATCH_POSE.get();
        poseStack.setIdentity();
        poseStack.translate(beRelX, beRelY, beRelZ);
        LegacyAnimator animator = LegacyAnimator.create(poseStack);

        var customTransform = spec.blockTransform();
        if (customTransform != null) {
            try {
                customTransform.apply((T) be, animator);
            } catch (Throwable ignored) {}
        } else {
            Direction facing = Direction.NORTH;
            try {
                if (spec.facingResolver() != null) {
                    facing = spec.facingResolver().apply((T) be);
                }
            } catch (Throwable ignored) {}
            if (facing == null) facing = Direction.NORTH;
            animator.setupBlockTransform(facing);
        }

        float[] lightFloats = SCRATCH_LIGHT4.get();
        float[] bakedBbox = SCRATCH_BBOX.get();
        sampleMachineLight(be, handle, lightFloats, bakedBbox);

        float[] rec = SCRATCH_RECORD.get();
        float[] paramScratch = SCRATCH_PARAMS.get();
        float[] encodedAnim = SCRATCH_ANIM.get();
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        java.util.List<? extends MachineSpec.PartDef<T>> specParts = spec.parts();
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            boolean invisible = false;
            java.util.Arrays.fill(encodedAnim, 0.0f);
            MachineSpec.PartDef<T> partDef = (specParts != null && i < specParts.size()) ? specParts.get(i) : null;

            poseStack.pushPose();
            if (partDef != null) {
                if (partDef.isConditional() && !partDef.isVisible((T) be)) {
                    invisible = true;
                }
                if (partDef.parametric() != null) {
                    MachineSpec.ParametricRig rig = partDef.parametric();
                    // Full base record statics (baseYaw + baseOffset) - applying only
                    // the offset skewed every yawed parametric part sideways.
                    rig.applyRecordStatic(poseStack, be, 0f);
                    if (partDef.parametricParams() != null) {
                        java.util.Arrays.fill(paramScratch, 0.0f);
                        try {
                            if (partDef.parametricParams().params((T) be, 0f, gameTime, paramScratch)) {
                                rig.encodeAnimParams(paramScratch, encodedAnim);
                            } else {
                                invisible = true;
                            }
                        } catch (Throwable ignored) {
                            invisible = true;
                        }
                    }
                } else if (partDef.animator() != null) {
                    try {
                        if (!partDef.animator().animate((T) be, 0f, gameTime, poseStack)) {
                            invisible = true;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            Matrix4f partPose = poseStack.last().pose();
            Vector3f trans = partPose.getTranslation(SCRATCH_VEC.get());
            Quaternionf rot = partPose.getNormalizedRotation(SCRATCH_QUAT.get());
            int q0 = InstancedStaticPartRenderer.packQuatWord(rot.x, rot.y);
            int q1 = InstancedStaticPartRenderer.packQuatWord(rot.z, rot.w);
            poseStack.popPose();

            java.util.Arrays.fill(rec, 0.0f);
            // InstPos (floats 0..2)
            rec[0] = trans.x;
            rec[1] = trans.y;
            rec[2] = trans.z;
            // InstRot (floats 3..4 packed snorm16)
            rec[3] = Float.intBitsToFloat(q0);
            rec[4] = Float.intBitsToFloat(q1);
            // InstBboxMin (floats 5..7)
            rec[5] = bakedBbox[0];
            rec[6] = bakedBbox[1];
            rec[7] = bakedBbox[2];
            // InstBboxSize (floats 8..10) - real sampled bounds, UNPADDED:
            // nucleus_cull.comp expands the cull sphere itself for GPU-parametric
            // parts (GradParams.w >= 0), while block_lit_instanced.vsh interpolates
            // the 8-corner lightmap with (vPos - bboxMin)/safeSize and
            // recordMatchesBuffer compares against MachineBer's sharedLightBbox -
            // a padded bbox corrupted both.
            rec[8] = bakedBbox[3] - bakedBbox[0];
            rec[9] = bakedBbox[4] - bakedBbox[1];
            rec[10] = bakedBbox[5] - bakedBbox[2];
            // FadeMode (float 11)
            boolean isAnimPart = partDef != null && (partDef.animated() || partDef.parametric() != null);
            rec[11] = invisible
                    ? InstancedStaticPartRenderer.FADE_MODE_INVISIBLE
                    : (isAnimPart ? InstancedStaticPartRenderer.FADE_MODE_MIN_ANIM : InstancedStaticPartRenderer.FADE_MODE_STATIC);
            // InstLight (floats 12..15 packed 8 corners)
            rec[12] = lightFloats[0];
            rec[13] = lightFloats[1];
            rec[14] = lightFloats[2];
            rec[15] = lightFloats[3];
            // InstUvRect (floats 16..19)
            float uv0 = 0.0f, uv1 = 0.0f, uv2 = 1.0f, uv3 = 1.0f;
            if (partDef != null && partDef.uvRectResolver() != null) {
                try {
                    float[] uv = partDef.uvRectResolver().apply((T) be);
                    if (uv != null && uv.length >= 4) {
                        uv0 = uv[0];
                        uv1 = uv[1];
                        uv2 = uv[2];
                        uv3 = uv[3];
                    }
                } catch (Throwable ignored) {}
            }
            rec[16] = uv0;
            rec[17] = uv1;
            rec[18] = uv2;
            rec[19] = uv3;
            // InstColor (floats 20..23) + GradParams (floats 24..27)
            writeTintAndGrad(rec, spec, partDef, be);
            // AnimParams (floats 28..31)
            rec[28] = encodedAnim[0];
            rec[29] = encodedAnim[1];
            rec[30] = encodedAnim[2];
            rec[31] = encodedAnim[3];

            enqueueSlotFloats(slot, rec);

            if (spec != null) {
                spec.syncInstanceRecord(be, handle.getBlockPosKey(), i, rec);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void enqueueUpdate(MachineInstanceHandle handle, BlockEntity be, int dirtyMask) {
        if (handle == null || be == null || !handle.isActive()) return;
        int[] slots = handle.getSlotIndices();
        if (slots == null || slots.length == 0) return;

        BlockPos bp = be.getBlockPos();
        BlockPos anchor = FrameViewState.anchorOrigin();
        float beRelX = (float) (bp.getX() - anchor.getX());
        float beRelY = (float) (bp.getY() - anchor.getY());
        float beRelZ = (float) (bp.getZ() - anchor.getZ());

        // Fine-grained slice: LIGHT (any mask containing the LIGHT bit - R4-3.4.4;
        // the strict == used to push every combined mask to the full 32-float
        // rebuild with a CPU light sample).
        if ((dirtyMask & DirtyMask.LIGHT) != 0 && be.getLevel() != null) {
            float[] lightFloats = SCRATCH_LIGHT4.get();
            sampleMachineLight(be, handle, lightFloats);
            for (int slot : slots) {
                enqueueSlotSlice(slot, 12, lightFloats);
            }
            MachineSpec<?> rawSpec = RetainedInstanceGraph.get().getSpec(be.getType());
            if (rawSpec != null) {
                rawSpec.syncInstanceSlice(be, handle.getBlockPosKey(), 12, lightFloats);
            }
            dirtyMask &= ~DirtyMask.LIGHT;
            if (dirtyMask == 0) return;
        }

        MachineSpec<?> rawSpec = RetainedInstanceGraph.get().getSpec(be.getType());
        PoseStack poseStack = SCRATCH_POSE.get();
        poseStack.setIdentity();
        poseStack.translate(beRelX, beRelY, beRelZ);
        LegacyAnimator animator = LegacyAnimator.create(poseStack);
        if (rawSpec != null && rawSpec.blockTransform() != null) {
            try {
                ((MachineSpec) rawSpec).blockTransform().apply(be, animator);
            } catch (Throwable ignored) {}
        } else {
            Direction facing = Direction.NORTH;
            if (rawSpec != null && rawSpec.facingResolver() != null) {
                try {
                    facing = (Direction) ((MachineSpec) rawSpec).facingResolver().apply(be);
                } catch (Throwable ignored) {}
            }
            if (facing == null) facing = Direction.NORTH;
            animator.setupBlockTransform(facing);
        }

        java.util.List<?> rawParts = rawSpec != null ? rawSpec.parts() : null;

        // Fine-grained slice: TRANSFORM (any mask still containing the TRANSFORM
        // bit - R4-3.4.4 layered writes; combos like LIGHT|TRANSFORM now take the
        // two slice paths instead of the full rebuild).
        if ((dirtyMask & DirtyMask.TRANSFORM) != 0) {
            float[] transFloats = SCRATCH_TRANS.get();
            long sliceGameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
            for (int i = 0; i < slots.length; i++) {
                int slot = slots[i];
                poseStack.pushPose();
                if (rawParts != null && i < rawParts.size()) {
                    MachineSpec.PartDef partDef = (MachineSpec.PartDef) rawParts.get(i);
                    if (partDef.parametric() != null) {
                        // Full base statics (baseYaw + baseOffset), not the offset only -
                        // a yawed parametric part got a sideways-skewed transform here.
                        partDef.parametric().applyRecordStatic(poseStack, be, 0f);
                    } else if (partDef.animator() != null) {
                        try {
                            partDef.animator().animate(be, 0f, sliceGameTime, poseStack);
                        } catch (Throwable ignored) {}
                    }
                }
                Matrix4f partPose = poseStack.last().pose();
                Vector3f trans = partPose.getTranslation(SCRATCH_VEC.get());
                Quaternionf rot = partPose.getNormalizedRotation(SCRATCH_QUAT.get());
                int q0 = InstancedStaticPartRenderer.packQuatWord(rot.x, rot.y);
                int q1 = InstancedStaticPartRenderer.packQuatWord(rot.z, rot.w);
                poseStack.popPose();

                transFloats[0] = trans.x;
                transFloats[1] = trans.y;
                transFloats[2] = trans.z;
                transFloats[3] = Float.intBitsToFloat(q0);
                transFloats[4] = Float.intBitsToFloat(q1);
                enqueueSlotSlice(slot, 0, transFloats);
                if (rawSpec != null) {
                    // Per-part write: the old all-parts syncInstanceSlice re-wrote
                    // EVERY part's transform slot with part i's data N times.
                    rawSpec.syncPartInstanceSlice(be, handle.getBlockPosKey(), i, 0, transFloats);
                }
            }
            dirtyMask &= ~DirtyMask.TRANSFORM;
            if (dirtyMask == 0) return;
        }

        // Full update or combination of masks (e.g. DirtyMask.ALL)
        // R3-2.1.2: sample the REAL physical bounds here (same as
        // enqueueInitialBake) - the old hardcode (-0.5,0,-0.5)/(1,1,1) shrank
        // every large machine's GPU-cull sphere to its anchor block after the
        // first non-LIGHT/non-TRANSFORM update.
        float[] lightFloats = SCRATCH_LIGHT4.get();
        float[] bakedBbox = SCRATCH_BBOX.get();
        sampleMachineLight(be, handle, lightFloats, bakedBbox);

        float[] rec = SCRATCH_RECORD.get();
        float[] paramScratch = SCRATCH_PARAMS.get();
        float[] encodedAnim = SCRATCH_ANIM.get();
        long gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            boolean invisible = false;
            java.util.Arrays.fill(encodedAnim, 0.0f);
            MachineSpec.PartDef partDef = (rawParts != null && i < rawParts.size()) ? (MachineSpec.PartDef) rawParts.get(i) : null;

            poseStack.pushPose();
            if (partDef != null) {
                if (partDef.isConditional() && !partDef.isVisible(be)) {
                    invisible = true;
                }
                if (partDef.parametric() != null) {
                    // Full base statics (baseYaw + baseOffset), plus real AnimParams
                    // (the hardwired zeros froze every GPU-parametric part at rest).
                    partDef.parametric().applyRecordStatic(poseStack, be, 0f);
                    if (partDef.parametricParams() != null) {
                        java.util.Arrays.fill(paramScratch, 0.0f);
                        try {
                            if (partDef.parametricParams().params(be, 0f, gameTime, paramScratch)) {
                                partDef.parametric().encodeAnimParams(paramScratch, encodedAnim);
                            } else {
                                invisible = true;
                            }
                        } catch (Throwable ignored) {
                            invisible = true;
                        }
                    }
                } else if (partDef.animator() != null) {
                    try {
                        if (!partDef.animator().animate(be, 0f, gameTime, poseStack)) {
                            invisible = true;
                        }
                    } catch (Throwable ignored) {}
                }
            }
            Matrix4f partPose = poseStack.last().pose();
            Vector3f trans = partPose.getTranslation(SCRATCH_VEC.get());
            Quaternionf rot = partPose.getNormalizedRotation(SCRATCH_QUAT.get());
            int q0 = InstancedStaticPartRenderer.packQuatWord(rot.x, rot.y);
            int q1 = InstancedStaticPartRenderer.packQuatWord(rot.z, rot.w);
            poseStack.popPose();

            java.util.Arrays.fill(rec, 0.0f);
            rec[0] = trans.x;
            rec[1] = trans.y;
            rec[2] = trans.z;
            rec[3] = Float.intBitsToFloat(q0);
            rec[4] = Float.intBitsToFloat(q1);
            // InstBboxMin/Size (floats 5..10) - real sampled bounds
            rec[5] = bakedBbox[0];
            rec[6] = bakedBbox[1];
            rec[7] = bakedBbox[2];
            rec[8] = bakedBbox[3] - bakedBbox[0];
            rec[9] = bakedBbox[4] - bakedBbox[1];
            rec[10] = bakedBbox[5] - bakedBbox[2];
            // InstBboxMin/Size stay UNPADDED: nucleus_cull.comp expands the cull
            // sphere itself for GPU-parametric parts (GradParams.w >= 0), and
            // block_lit_instanced.vsh interpolates the 8-corner lightmap with
            // (vPos - bboxMin)/safeSize - a padded bbox corrupted that gradient
            // and broke recordMatchesBuffer against MachineBer's sharedLightBbox.
            boolean isAnimPart = partDef != null && (partDef.animated() || partDef.parametric() != null);
            rec[11] = invisible
                    ? InstancedStaticPartRenderer.FADE_MODE_INVISIBLE
                    : (isAnimPart ? InstancedStaticPartRenderer.FADE_MODE_MIN_ANIM : InstancedStaticPartRenderer.FADE_MODE_STATIC);
            rec[12] = lightFloats[0];
            rec[13] = lightFloats[1];
            rec[14] = lightFloats[2];
            rec[15] = lightFloats[3];
            float uv0 = 0.0f, uv1 = 0.0f, uv2 = 1.0f, uv3 = 1.0f;
            if (partDef != null && partDef.uvRectResolver() != null) {
                try {
                    float[] uv = (float[]) partDef.uvRectResolver().apply(be);
                    if (uv != null && uv.length >= 4) {
                        uv0 = uv[0];
                        uv1 = uv[1];
                        uv2 = uv[2];
                        uv3 = uv[3];
                    }
                } catch (Throwable ignored) {}
            }
            rec[16] = uv0;
            rec[17] = uv1;
            rec[18] = uv2;
            rec[19] = uv3;
            // InstColor (floats 20..23) + GradParams (floats 24..27)
            writeTintAndGrad(rec, rawSpec, partDef, be);
            // AnimParams (floats 28..31)
            rec[28] = encodedAnim[0];
            rec[29] = encodedAnim[1];
            rec[30] = encodedAnim[2];
            rec[31] = encodedAnim[3];
            enqueueSlotFloats(slot, rec);

            if (rawSpec != null) {
                rawSpec.syncInstanceRecord(be, handle.getBlockPosKey(), i, rec);
            }
        }
    }

    /**
     * No-op: the retained graph has no GPU upload path (see class javadoc).
     */
    public synchronized void flushUpdates(int targetVbo) {
    }

    public synchronized void clear() {
        pendingCount = 0;
    }

    public synchronized void destroy() {
        pendingCount = 0;
    }

    public int getPendingCount() {
        return pendingCount;
    }
}
