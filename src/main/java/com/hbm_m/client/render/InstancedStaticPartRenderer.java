package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}


import java.lang.ref.Cleaner;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Instanced renderer for static parts (Base/Frame).
 * Without shaders it renders all machines of one type with a single {@code glDrawElementsInstanced}.
 * Under Iris/Oculus it switches to per-machine draws via {@code ExtendedShader}
 * + a companion VBO with the {@code IrisVertexFormats.ENTITY} layout, which yields correct
 * G-buffer / shadow pass / pack uniforms.
 * <p>
 * Flush logic is delegated to {@link VanillaInstancedBatchRenderer} (vanilla path)
 * and {@link IrisInstancedBatchRenderer} (Iris/Oculus path).
 * GL compatibility helpers live in {@link InstancedGlCompat}.
 */
@OnlyIn(Dist.CLIENT)
public class InstancedStaticPartRenderer extends AbstractGpuMesh
        implements VanillaInstancedMeshRenderer, IrisCompanionMeshRenderer {

    /** Per-part instance cap (one renderer = one mesh part, e.g. ChemPlant/Base). */
    final int maxInstances = ClientRenderFlags.maxInstances();

    /**
     * Lazy CPU capacity: per-instance arrays + the instanceBuffer staging start
     * small and grow geometrically via {@link #ensureCpuInstanceCapacity} - a
     * renderer hosting a few machines must not commit maxInstances slots of
     * off-heap + heap arrays upfront.
     */
    private static final int INITIAL_CPU_INSTANCES = 64;
    /** Current allocation size of the per-instance CPU arrays and instanceBuffer. */
    private int currentAllocatedInstances = Math.min(INITIAL_CPU_INSTANCES, maxInstances);
    private static final java.util.concurrent.atomic.AtomicInteger OVERFLOW_ADD_COUNT =
            new java.util.concurrent.atomic.AtomicInteger();

    /** Diagnostics: consumed by {@link com.hbm_m.client.render.culling.InstancedRenderStats}. */
    public static int drainOverflowAddCount() {
        return OVERFLOW_ADD_COUNT.getAndSet(0);
    }

    // Per-instance layout (32 floats = 128 bytes; every field offset below is the
    // single source of truth — the GLSL programs consume them via injected defines,
    // see NucleusGpuCuller / NucleusGpuBaker):
    //   InstPos       vec3 (loc 4) @ 0
    //   InstRot       vec4 (loc 5) @ 3  — PACKED: 4 snorm16 halves in 2 float words
    //                                     (x,y in word 3, z,w in word 4, low half first);
    //                                     the VAO reads it as 4xGL_SHORT normalized so the
    //                                     vsh sees floats in [-1,1] with no shader decode;
    //                                     the compute shaders decode with plain ALU.
    //   InstBboxMin   vec3 (loc 6) @ 5
    //   InstBboxSize  vec4 (loc 7) @ 8 — xyz extent, w = fade
    //   InstLightC01  vec4 (loc 8) @ 12  — PACKED u8: c0.u, c0.v, c1.u, c1.v
    //   InstLightC23  vec4 (loc 9) @ 13  — PACKED u8: c2.u, c2.v, c3.u, c3.v
    //   InstLightC45  vec4 (loc 10) @ 14 — PACKED u8: c4.u, c4.v, c5.u, c5.v
    //   InstLightC67  vec4 (loc 11) @ 15 — PACKED u8: c6.u, c6.v, c7.u, c7.v
    //                                     (8 corners x 2 u8 in 4 words; GL_UNSIGNED_BYTE
    //                                     normalized in the VAO, x240 decode in the vsh —
    //                                     a 0..240 texel value costs ~0.94 texel quantization)
    //   InstUvRect    vec4 (loc 12) @ 16  -- uv0.x, uv0.y, du, dv (sprite-local VBO → atlas;
    //                                     identity 0,0,1,1 for regular machines, see UVRECT_FLOAT_OFFSET)
    //   InstColor     vec4 (loc 13) @ 20  -- per-instance RGBA tint (part heat/glow; RGB
    //                                     may be > 1 - overbright), white = passthrough
    //   GradParams    vec4 (loc 14) @ 24  -- spatial falloff of the tint in MODEL coordinates:
    //                                     x = axis (0/1/2, <0 = off), y = full-tint
    //                                     coordinate (source), z = zero coordinate; smooth
    //                                     smoothstep between them (block_lit_instanced.vsh);
    //                                     w = parametric joint CHAIN base index in
    //                                     NucleusJointSpecs, <0 = part without parametrics
    //   AnimParams    vec4 (loc 15) @ 28  -- per-frame parameters of the joint chain
    //                                     (MachineSpecBuilder.parametricPart / jointChainPart),
    //                                     PACKED: up to 8 binary16 words, 2 per float (low
    //                                     first; the VSH decodes with unpackHalf2x16, the
    //                                     encoder is HalfFloat). One word per rotate/axis-
    //                                     translate parameter, three per vector translate,
    //                                     in node declaration order. The chain base index
    //                                     (first node's texel pair; node 0's pivot texel .w =
    //                                     node count) lives in GradParams.w.
    static final int INSTANCE_ATTRIB_FIRST = 4;
    public static final int POS_FLOAT_OFFSET = 0;
    /** Packed quat: 4 snorm16 halves in 2 words starting here (see packQuatWord). */
    public static final int QUAT_FLOAT_OFFSET = 3;
    public static final int BBOX_MIN_FLOAT_OFFSET = 5;
    public static final int BBOX_SIZE_FLOAT_OFFSET = 8;
    /** Fade MODE word (InstBboxSize.w) - single source of truth for every CPU reader. */
    public static final int FADE_FLOAT_OFFSET = 11;
    /** Packed corner light: 8 corners x 2 u8 in 4 words starting here (see packLightWord). */
    static final int LIGHT_FLOAT_OFFSET = 12;
    static final int UVRECT_FLOAT_OFFSET = 16;
    static final int TINT_FLOAT_OFFSET = 20;
    static final int GRAD_FLOAT_OFFSET = 24;
    static final int ANIM_FLOAT_OFFSET = 28;
    /** public: read by the culling pipeline (NucleusGpuCuller) and modding artifacts. */
    public static final int INSTANCE_DATA_SIZE = 32;

    // ── Packed-field encoders (the ONLY encode/decode pair for the compressed
    // record fields; used by both writers and the skip-write matcher) ──

    /** snorm16 encode: [-1,1] -> signed 16-bit half (step ~3.05e-5, invisible). */
    static int packSnorm16(float v) {
        return (short) Math.round(Math.max(-1.0f, Math.min(1.0f, v)) * 32767.0f);
    }

    /** One packed quat word: two snorm16 halves, low half first. */
    public static int packQuatWord(float lo, float hi) {
        return (packSnorm16(lo) & 0xFFFF) | (packSnorm16(hi) << 16);
    }

    /** snorm16 decode (compute-side mirror of the VAO's GL_SHORT normalized fetch). */
    public static float decodeSnorm16(int half) {
        return Math.max(-1.0f, Math.min(1.0f, (short) half / 32767.0f));
    }

    /** Corner light encode: 0..240 texel units -> u8 over the FULL byte range. */
    public static int packLightU8(float c) {
        int b = Math.round(Math.max(0.0f, Math.min(240.0f, c)) * (255.0f / 240.0f));
        return Math.max(0, Math.min(255, b));
    }

    /** Corner light decode: u8 -> 0..240 texel units (byte x 240/255). */
    public static float decodeLightU8(int b) {
        return (b & 0xFF) * (240.0f / 255.0f);
    }

    /**
     * One packed corner-light word: FOUR u8 values, low byte first —
     * c2j.u, c2j.v, c2j+1.u, c2j+1.v for the corner pair (2j, 2j+1). The vsh
     * reads the same 4 bytes as a normalized GL_UNSIGNED_BYTE vec4 (x240 decode);
     * the compute shaders decode via plain ALU shifts.
     */
    public static int packLightWord(float u0, float v0, float u1, float v1) {
        return packLightU8(u0) | (packLightU8(v0) << 8) | (packLightU8(u1) << 16) | (packLightU8(v1) << 24);
    }

    // ── Fade MODE stored in the record (word 11 / attrib InstBboxSize.w) ──
    // The record must stay camera-independent (span-diff converges to zero in a
    // static scene), so instead of a per-frame distance-fade alpha it stores a
    // static MODE and the ramp is evaluated on the GPU per instance (vertex
    // shader, uStaticFadeStart/End + uAnimFadeStart/End uniforms; CPU mirror:
    // RenderDistanceHelper.evalInstanceAlpha). The mode values are encoded as
    // alpha-compatible floats just below 1.0: consumers that still read the
    // field as a plain alpha (pack-program paths: Iris companion flush, GPU
    // bake) degrade gracefully to "no distance fade" instead of disappearing.
    public static final float FADE_MODE_INVISIBLE = -1.0f;  // invisible / culled
    public static final float FADE_MODE_NONE = 1.0f;        // no distance fade (missiles, contraptions)
    public static final float FADE_MODE_STATIC = 0.9999f;   // static-zone ramp
    public static final float FADE_MODE_MIN_ANIM = 0.9998f; // min(static, anim) ramp
    /** > this: FADE_MODE_NONE. */
    public static final float FADE_MODE_NONE_DIVIDER = 0.99995f;
    /** > this (and <= NONE divider): FADE_MODE_STATIC; else FADE_MODE_MIN_ANIM. */
    public static final float FADE_MODE_MIN_ANIM_DIVIDER = 0.99985f;

    final int instanceDataSize;
    final int instanceAttribLast;
    final int instanceFadeFloatOffset;
    final int lightFloatCount;

    private static final java.util.Set<InstancedStaticPartRenderer> ACTIVE_RENDERERS =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private final InstanceSlotAllocator slotAllocator = new InstanceSlotAllocator(currentAllocatedInstances, maxInstances);

    public InstanceSlotAllocator getSlotAllocator() {
        return slotAllocator;
    }

    public int allocateSlot(long posKey) {
        int slot = slotAllocator.allocateSlot(posKey);
        // External callers take the raw slot and write records into it directly
        // (e.g. via putInstanceRecord-style native writes); guarantee their slot
        // has backing CPU arrays even if they never route through the internal
        // ensureCpuInstanceCapacity call sites.
        if (slot >= 0 && slot < maxInstances) {
            ensureCpuInstanceCapacity(slot);
        }
        return slot;
    }

    public int getSlot(long posKey) {
        return slotAllocator.getSlot(posKey);
    }

    public boolean hasSlot(long posKey) {
        return slotAllocator.hasSlot(posKey);
    }

    public void freeSlot(long posKey) {
        if (!initialized || slotAllocator == null) return;
        int slot = slotAllocator.getSlot(posKey);
        // The keyed allocator mapping is authoritative: both add paths allocate keyed
        // slots and partitionAndRemapAllocator re-points them via remapKeyedSlot, so a
        // negative answer means this position is not hosted here at all. No fallback
        // scan over instanceOcclusionKeys - that was an O(N) pass on the teardown hot path.
        if (slot < 0) return;
        freeSlotInternal(slot, posKey);

        // 6. Update instanceCount to current allocator high-water mark
        int hwm = slotAllocator.getHighWaterMark();
        this.instanceCount = Math.min(maxInstances, hwm);
        if (instanceBuffer != null) {
            int targetPos = Math.min(instanceDataSize * instanceCount, instanceBuffer.capacity());
            ((Buffer) instanceBuffer).position(targetPos);
        }

        // 7. If all instances of this renderer were freed, immediately evict from MDI coordinator
        if (hwm == 0) {
            MdiBatchCoordinator coord = MdiBatchCoordinator.active();
            if (coord != null) {
                coord.evictRenderer(this);
            }
            MdiBatchCoordinator.evict(this);
        }
    }

    private void freeSlotInternal(int slot, long posKey) {
        // 1. Deallocate slot in allocator
        slotAllocator.freeSlot(slot);
        slotAllocator.freeSlot(posKey);

        // 2. Invalidate metadata arrays
        if (slot < instanceOcclusionKeys.length) {
            instanceOcclusionKeys[slot] = Long.MIN_VALUE;
        }
        if (slot < instanceCullIndices.length) {
            instanceCullIndices[slot] = -1;
        }
        if (slot < slotPosQuatStable.length) {
            slotPosQuatStable[slot] = false;
            slotPosQuatVerified[slot] = false;
        }

        // 3. Zero out native memory record and set fadeMode = FADE_MODE_INVISIBLE (-1.0f)
        if (instanceBufferAddress != 0L) {
            int baseFloat = slot * instanceDataSize;
            long recordAddr = instanceBufferAddress + (long) baseFloat * 4L;
            MemoryUtil.memSet(recordAddr, 0, (long) instanceDataSize * 4L);
            MemoryUtil.memPutFloat(recordAddr + (long) FADE_FLOAT_OFFSET * 4L, FADE_MODE_INVISIBLE);
        }

        // 4. Invalidate direct VBO shadow if present
        if (vboShadow != null && vboShadowValid) {
            vboShadowValid = false;
            vboShadowValidFloats = 0;
        }

        // 5. Break clean MDI sync to ensure coordinator updates snapshot (without wiping all slots' stability flags)
        this.mdiRecordWriteHappened = true;
        this.mdiBufferSynced = false;
        this.recordMutationEpoch++;
        this.slotLayoutEpoch++;
    }

    /**
     * Stores or updates an instance record directly from retained scene graph / sparse queue,
     * allocating a persistent slot for posKey and writing the 32 floats into native memory.
     */
    public void putInstanceRecord(long posKey, float[] record32) {
        if (!initialized || instanceBuffer == null || instanceBufferAddress == 0L || slotAllocator == null || record32 == null || record32.length < 32) {
            return;
        }
        int slot = slotAllocator.allocateSlot(posKey);
        if (slot >= maxInstances) return;
        ensureCpuInstanceCapacity(slot);
        if (slot < instanceCullIndices.length) {
            instanceCullIndices[slot] = -1;
        }
        if (slot < instanceOcclusionKeys.length) {
            instanceOcclusionKeys[slot] = posKey;
        }
        long recordAddr = instanceBufferAddress + (long) slot * instanceDataSize * 4L;
        for (int i = 0; i < 32; i++) {
            MemoryUtil.memPutFloat(recordAddr + ((long) i * 4L), record32[i]);
        }
        if (slot < slotPosQuatStable.length) {
            slotPosQuatStable[slot] = true;
            slotPosQuatVerified[slot] = false; // not compare-verified; only addInstance verifies
        }
        this.mdiRecordWriteHappened = true;
        this.mdiBufferSynced = false;
        this.recordMutationEpoch++;
        this.slotLayoutEpoch++;
        this.vboShadowValid = false;
        this.vboShadowValidFloats = 0;
        this.instanceCount = Math.min(maxInstances, Math.max(instanceCount, slotAllocator.getHighWaterMark()));
        if (instanceBuffer != null) {
            int targetPos = Math.min(instanceDataSize * instanceCount, instanceBuffer.capacity());
            ((Buffer) instanceBuffer).position(targetPos);
        }
    }

    /**
     * Updates a sub-range (slice) of an existing instance record in native memory (e.g. light, transform).
     */
    public void putInstanceSlice(long posKey, int floatOffset, float[] data) {
        if (!initialized || instanceBuffer == null || instanceBufferAddress == 0L || slotAllocator == null || data == null) {
            return;
        }
        int slot = slotAllocator.getSlot(posKey);
        if (slot < 0 || slot >= maxInstances) return;
        ensureCpuInstanceCapacity(slot);
        int count = Math.min(data.length, instanceDataSize - floatOffset);
        if (count <= 0) return;
        long targetAddr = instanceBufferAddress + (long) (slot * instanceDataSize + floatOffset) * 4L;
        for (int i = 0; i < count; i++) {
            MemoryUtil.memPutFloat(targetAddr + ((long) i * 4L), data[i]);
        }
        if (floatOffset == 0 && slot < slotPosQuatStable.length) {
            slotPosQuatStable[slot] = true;
            slotPosQuatVerified[slot] = false; // not compare-verified; only addInstance verifies
        }
        this.mdiRecordWriteHappened = true;
        this.mdiBufferSynced = false;
        this.recordMutationEpoch++;
        this.vboShadowValid = false;
        this.vboShadowValidFloats = 0;
    }

    int instanceCount = 0;
    // Per-instance arrays start at currentAllocatedInstances and grow by
    // Arrays.copyOf in ensureCpuInstanceCapacity - hence no `final`.
    int[] instanceCullIndices = new int[currentAllocatedInstances];
    long[] instanceOcclusionKeys = new long[currentAllocatedInstances];
    /**
     * Per-slot posQuat stability: the last write/assert for this slot did not change
     * the record's pos/quat vs the previous flush (tolerance compare of
     * {@link #recordMatchesBuffer}). Consumed by the SHADOW-EPOCH prebake calibration
     * ({@link IrisShadowBatchCollector#publishPrebake}): only frame-invariant part
     * poses may calibrate the shadow basis or stand proxy for the shadow animator's
     * output. Lifecycle mirrors instanceOcclusionKeys (persists across flushes).
     */
    boolean[] slotPosQuatStable = new boolean[currentAllocatedInstances];
    /**
     * Provenance of {@link #slotPosQuatStable}: true only when the slot's posQuat was
     * verified by an actual {@link #recordMatchesBuffer} compare THIS frame. The stable
     * flag alone is also set by trust (roster assert, static-pose skip-write), which is
     * sound for the main pass but must never feed shadow-basis calibration - a trusted
     * slot whose pose changed since the snapshot would poison the solved SHADOW-EPOCH
     * basis (see IrisShadowBatchCollector#notePrebakeCalibration).
     */
    boolean[] slotPosQuatVerified = new boolean[currentAllocatedInstances];

    /** Incremented whenever any float in instanceBuffer is written or modified. */
    private long recordMutationEpoch = 1L;
    /** Incremented whenever slot allocation, freeing, or partitioning changes slot-to-posKey layout. */
    private long slotLayoutEpoch = 1L;

    public long getRecordMutationEpoch() {
        return recordMutationEpoch;
    }

    public long getSlotLayoutEpoch() {
        return slotLayoutEpoch;
    }

    float batchSkyDarken = -1f;
    private boolean overflowLogged = false;
    /** During this phase at least one actual write to instanceBuffer happened (skip-write failed at least once). */
    boolean mdiRecordWriteHappened = false;
    /**
     * The instanceBuffer contents are in sync with the MDI coordinator's snapshot:
     * the last flush went out as an accepted submit and no writes happened since.
     * Reset by the direct path (partition with reordering), renderSingle, and failed
     * submits; being true lets {@code submitClean} reuse the snapshot.
     */
    boolean mdiBufferSynced = false;
    static volatile boolean warnedInstancedShaderNullFlush;

    /**
     * Phase-2 deferred fading of the direct path (MDI fallback): {@code flush()} on the
     * direct path draws only opaque instances,
     * while fading ones are copied into {@link #fadingSnapshot}; they are picked up by
     * {@link #flushFading(Matrix4f)} AFTER the MDI multi-draw (see
     * InstancedRenderFrame). Otherwise translucent geometry drawn earlier than the
     * opaque MDI base (it goes to the coordinator and is drawn at the end of the flush)
     * writes depth and depth-rejects the base - the chunk shows through instead of it.
     */
    int deferredFadingCount = 0;
    /** Lazily allocated in {@link #deferFading} (pow2 growth); null until something fades. */
    FloatBuffer fadingSnapshot;

    float[] instanceLightUV = new float[currentAllocatedInstances * 2];

    /**
     * Current record uvRect ({u0, v0, du, dv}); identity {0,0,1,1} for regular machines.
     * Filled by {@link #addInstance} (the new 7-arg overload with uvRect) before writing
     * the record and reading {@code recordMatchesBuffer}; the shadow batch writes the same into its records.
     */
    final float[] tmpUvRect = {0f, 0f, 1f, 1f};
    private static final float[] IDENTITY_UV_RECT = {0f, 0f, 1f, 1f};

    /** Active uvRect (for shadow-batch records); always length 4. */
    float[] getActiveUvRect() {
        return tmpUvRect;
    }

    /**
     * Current record RGBA tint; RGB white = passthrough, A = emission strength (heat 0..1) -
     * shaders push the lightmap toward fullbright by tint*gradient (glow follows the tint).
     * Filled by {@link #addInstance} before writing the record; the shadow batch writes the same
     * into its records (the shadow FB does not use color - only the stride alignment matters).
     */
    final float[] tmpTint = {1f, 1f, 1f, 0f};
    private static final float[] WHITE_TINT = {1f, 1f, 1f, 0f};

    /**
     * Current spatial tint falloff {axis, fullCoord, zeroCoord, chainBase};
     * axis &lt; 0 = off (multiplier 1). The config is static per part
     * ({@code MachineSpecBuilder.tintFalloff}), but is written into the instance record -
     * the shader computes the gradient from the vertex's model coordinates.
     * The 4th component is the parametric joint CHAIN base index
     * ({@code <0} = no parametrics) - it was a zero pad before the chain system.
     */
    final float[] tmpGrad = {-1f, 0f, 0f, -1f};

    /**
     * Current per-frame parameters of the record's joint chain (attrib 15
     * AnimParams, PACKED binary16 words via {@code HalfFloat} - the rig's
     * {@code encodeAnimParams}); the chain base index travels in
     * {@link #tmpGrad}[3].
     */
    final float[] tmpAnimParams = {0f, 0f, 0f, 0f};

    /**
     * Fade mode of the record being built (float 13 / attrib InstBboxSize.w) - see
     * {@link #FADE_MODE_NONE}. Filled from {@link PartEnqueueRequest#fadeMode} in addInstance.
     */
    float tmpFadeMode = FADE_MODE_STATIC;

    /** Active falloff (for shadow-batch records); always length 4. */
    float[] getActiveGrad() {
        return tmpGrad;
    }

    /** Active tint (for shadow-batch records); always length 4. */
    float[] getActiveTint() {
        return tmpTint;
    }

    final Vector3f posTmp = new Vector3f();
    final Quaternionf rotTmp = new Quaternionf();
    final Matrix4f tmpLocalPose = new Matrix4f();
    final Matrix4f tmpInvViewRot = new Matrix4f();
    /** Scratch for converting composed(view) to a world transform (see {@link #convertToWorldRecord}). */
    private final Matrix4f tmpWorldMat = new Matrix4f();
    final float[] tmpCornerUV;

    int instanceVboId = -1;
    FloatBuffer instanceBuffer;
    private long instanceBufferAddress;

    // -- Direct-path VBO shadow (span-diff uploads) ----------------------
    // CPU copy of the instanceVboId contents: drawInstanceRange/flushFadingVanilla/
    // renderSingleVanilla upload only changed spans (see GpuSpanUploader).
    // Lazy alloc: only renderers that actually take the direct path (not MDI).
    private FloatBuffer vboShadow;
    private int vboShadowFloats = 0;
    // How many floats of vboShadow actually mirror the GPU window (from the
    // window's shadow offset). Words beyond this are uninitialized alloc tail -
    // diffUpload must treat them as changed (tail span) instead of diffing
    // against zeros. Reset to 0 wherever vboShadowValid is reset.
    private int vboShadowValidFloats = 0;
    private boolean vboShadowValid = false;
    private static long activeAnchorGen = -1L;
    private long lastAnchorGen = -1L;

    // -- Phase-2 fading instance VBO (own buffer + shadow) ----------------
    // flushFadingVanilla must NOT reuse instanceVboId region [0, count): the
    // opaque phase uploaded its records there and synced vboShadow against
    // them - overwriting the region with fading records (and syncing the same
    // shadow to them) destroys the differential-upload cache BOTH ways: every
    // frame each phase would full-mismatch against the other phase's shadow.
    // The fading records live in their own VBO (same layout, drawn by
    // re-pointing the VAO instance attribs at it for the phase-2 draw), so
    // each phase keeps a stable GPU region and its own shadow.
    int fadingInstanceVboId = -1;
    private FloatBuffer fadingVboShadow;
    private int fadingVboShadowFloats = 0;
    // Fading twin of vboShadowValidFloats - see the comment there.
    private int fadingVboShadowValidFloats = 0;
    private boolean fadingVboShadowValid = false;

    public static void onRenderOriginChanged(int deltaX, int deltaY, int deltaZ) {
        activeAnchorGen = FrameViewState.anchorGeneration();
        // Records are anchor-relative: an anchor shift stales them all - the fast-path
        // roster assert must report a worldGen miss and fall back to a full rebuild.
        NucleusRenderVersion.bump();
        synchronized (ACTIVE_RENDERERS) {
            for (InstancedStaticPartRenderer renderer : ACTIVE_RENDERERS) {
                if (renderer != null && renderer.initialized) {
                    renderer.rebaseAnchor(deltaX, deltaY, deltaZ);
                }
            }
        }
    }

    public static void onRenderOriginChanged() {
        onRenderOriginChanged(0, 0, 0);
    }

    public void rebaseAnchor(int deltaX, int deltaY, int deltaZ) {
        if (!initialized || instanceBuffer == null || instanceBufferAddress == 0L || slotAllocator == null) {
            return;
        }
        int maxSlot = slotAllocator.getHighWaterMark();
        if (maxSlot <= 0) {
            this.lastAnchorGen = activeAnchorGen;
            return;
        }
        for (int s = 0; s < maxSlot; s++) {
            long slotAddr = instanceBufferAddress + (long) s * instanceDataSize * 4L;
            if (slotAllocator.isSlotAllocated(s)) {
                if (deltaX != 0 || deltaY != 0 || deltaZ != 0) {
                    long posAddr = slotAddr + (long) POS_FLOAT_OFFSET * 4L;
                    float x = MemoryUtil.memGetFloat(posAddr);
                    float y = MemoryUtil.memGetFloat(posAddr + 4);
                    float z = MemoryUtil.memGetFloat(posAddr + 8);
                    MemoryUtil.memPutFloat(posAddr, x - (float) deltaX);
                    MemoryUtil.memPutFloat(posAddr + 4, y - (float) deltaY);
                    MemoryUtil.memPutFloat(posAddr + 8, z - (float) deltaZ);
                }
            } else {
                // Inactive / freed slot: force fadeMode to -1.0f so shader clips it out
                long fadeAddr = slotAddr + (long) FADE_FLOAT_OFFSET * 4L;
                MemoryUtil.memPutFloat(fadeAddr, FADE_MODE_INVISIBLE);
            }
        }
        this.lastAnchorGen = activeAnchorGen;
        this.vboShadowValid = false;
        this.vboShadowValidFloats = 0;
        this.mdiBufferSynced = false;
        this.mdiRecordWriteHappened = true;
        this.recordMutationEpoch++;
    }
    /** VRAM estimate of this renderer (vertices+indices+instance VBO); 0 = not accounted. */
    private long vramBytes = 0;

    java.nio.ByteBuffer atlasVertexBytesRetained;
    java.nio.IntBuffer atlasIndicesRetained;
    int atlasIndexCountRetained;
    @Nullable
    private String mdiTraceTag;

    private static final Cleaner CLEANER = Cleaner.create();
    private Cleaner.Cleanable instanceBufferCleanable;

    /**
     * R4-2.1: mutable snapshot of the renderer's GL names + VRAM accounting, read
     * (and consumed) by the second Cleaner action. The action MUST NOT reference
     * the renderer (per {@link Cleaner} javadoc an action touching the registered
     * object's fields keeps it strongly reachable forever) - it closes over this
     * holder instead. cleanup() consumes ALL name fields (super.cleanup() already
     * schedules the VAO/VBO/EBO deletion; the instance VBOs are deleted by
     * cleanup() itself) so a later GC-driven clean() is a no-op for them. The
     * queued path fires only for renderers dropped WITHOUT an explicit cleanup().
     */
    private static final class GlReleaseState {
        int vaoId, vboId, eboId, instanceVboId, fadingInstanceVboId;
        long vramBytes;
    }

    private GlReleaseState glReleaseState;
    private Cleaner.Cleanable glNamesCleanable;

    /**
     * Mutable addresses of the off-heap buffers owned by this renderer (instance
     * staging buffer + phase-2 fading snapshot), read by the first Cleaner action.
     * fadingSnapshot is reallocated on demand, so its address travels through this
     * holder (guarded by the holder monitor) instead of a constructor final local -
     * otherwise the Cleaner would free a stale/dangling address or leak the
     * reallocated buffer.
     */
    private static final class OffHeapAddrs {
        long instanceAddr;
        long fadingAddr;
    }

    @Nullable
    private OffHeapAddrs offHeapAddrs;

    /**
     * GL names parked by the {@link Cleaner} for renderers dropped WITHOUT an explicit
     * {@link #cleanup()}: the Cleaner thread has no current GL context, so deletion must
     * not run there - the names wait here and are deleted by
     * {@link #drainPendingGlDeletions()} on the render thread (next cleanup flush /
     * RenderCacheManager invalidation / per-frame replay queue).
     * Layout: {@code {vaoId, vboId, eboId, instanceVboId, fadingInstanceVboId}}; entries {@code <= 0} are
     * skipped, so a renderer already released by cleanup() enqueues nothing live.
     */
    private static final java.util.Queue<int[]> PENDING_GL_DELETIONS =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    /**
     * Builds the Cleaner action for the GL-names snapshot. Static on purpose: a lambda
     * created here cannot capture {@code this}, only the passed {@link GlReleaseState} -
     * so the renderer stays collectible and {@link #PENDING_GL_DELETIONS} actually fires
     * for renderers dropped without an explicit {@link #cleanup()}.
     */
    private static Runnable createGlNamesCleanAction(GlReleaseState glState) {
        return () -> {
            try {
                PENDING_GL_DELETIONS.add(new int[] { glState.vaoId, glState.vboId, glState.eboId,
                        glState.instanceVboId, glState.fadingInstanceVboId });
                // Same consume-and-zero pattern as cleanup(): an implicitly GC'd
                // renderer must release its F3 counter bytes too; if cleanup()
                // already ran, this reads 0 and is a no-op.
                glState.vaoId = 0;
                glState.vboId = 0;
                glState.eboId = 0;
                glState.instanceVboId = 0;
                glState.fadingInstanceVboId = 0;
                final long vramToRelease = glState.vramBytes;
                glState.vramBytes = 0L;
                if (vramToRelease != 0L) {
                    NucleusDebug.removeRendererVram(vramToRelease);
                }
                // Prompt processing: recordRenderCall is safe from any thread
                // (plain ConcurrentLinkedQueue.add) and replays on the render
                // thread every frame.
                RenderSystem.recordRenderCall(InstancedStaticPartRenderer::drainPendingGlDeletions);
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("Failed to park GL names for deferred deletion", t);
            }
        };
    }

    /** Render thread only: deletes GL names parked by the Cleaner (see {@link #PENDING_GL_DELETIONS}). */
    private static void drainPendingGlDeletions() {
        // The driver re-issues freed numeric IDs: if a queued entry carries a number the
        // live MdiGeometryAtlas owns RIGHT NOW, deleting it destroys the atlas VAO/EBO
        // mid-frame and the next MDI draw faults in nvoglv64.dll (hs_err_pid50040:
        // atlas vao=161475 re-initialised after RESOURCE_RELOAD, killed by a stale
        // deferred delete one second later). Skipping is safe - the number is owned by
        // a live object, and the orphaned renderer's true names were already deleted
        // (that is how the number became free for reuse in the first place).
        final int[] atlasNames = com.hbm_m.client.render.MdiGeometryAtlas.liveGlNames();
        int[] names;
        while ((names = PENDING_GL_DELETIONS.poll()) != null) {
            try {
                if (names[0] > 0 && !ownedByAtlas(names[0], atlasNames)) GL30.glDeleteVertexArrays(names[0]);
                if (names[1] > 0 && !ownedByAtlas(names[1], atlasNames)) GL15.glDeleteBuffers(names[1]);
                if (names[2] > 0 && !ownedByAtlas(names[2], atlasNames)) GL15.glDeleteBuffers(names[2]);
                if (names[3] > 0 && !ownedByAtlas(names[3], atlasNames)) GL15.glDeleteBuffers(names[3]);
                if (names[4] > 0 && !ownedByAtlas(names[4], atlasNames)) GL15.glDeleteBuffers(names[4]);
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("Failed to delete Cleaner-orphaned GL objects", t);
            }
        }
    }

    private static boolean ownedByAtlas(int name, int[] atlasNames) {
        if (name <= 0) return false;
        for (int live : atlasNames) {
            if (live == name) {
                MainRegistry.LOGGER.warn(
                        "[HBM-M MDI] Deferred deletion skipped GL name {} - it was re-issued to the live MdiGeometryAtlas", name);
                return true;
            }
        }
        return false;
    }

    /** One guarded cleanup step: a throw from one step must not cancel the frees of the later steps. */
    private static void tryCleanupStep(String name, Runnable step) {
        try {
            step.run();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer.cleanup step '{}' failed", name, t);
        }
    }

    final List<BakedQuad> quadsForIris;

    // ── Delegate helpers ───────────────────────────────────────────────
    final VanillaInstancedBatchRenderer vanillaHelper;
    private final IrisInstancedBatchRenderer irisHelper;

    // ── Scratch ─────────────────────────────────────────────────────────

    // ── Constructors ───────────────────────────────────────────────────

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data) {
        this(data, null);
    }

    public InstancedStaticPartRenderer(SingleMeshVboRenderer.VboData data, List<BakedQuad> quadsForIris) {
        this.quadsForIris = quadsForIris;
        this.instanceDataSize = INSTANCE_DATA_SIZE;
        this.instanceAttribLast = 15;
        this.instanceFadeFloatOffset = FADE_FLOAT_OFFSET; // InstBboxSize.w
        this.lightFloatCount = 16;
        this.tmpCornerUV = new float[lightFloatCount];

        this.vanillaHelper = new VanillaInstancedBatchRenderer(this);
        this.irisHelper = new IrisInstancedBatchRenderer(this);

        if (data == null) {
            MainRegistry.LOGGER.error("InstancedStaticPartRenderer: Received NULL VboData! Cannot create renderer.");
            initialized = false;
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Skipping initialization because this is not render thread.");
            data.close();
            initialized = false;
            return;
        }
        if (GLFW.glfwGetCurrentContext() == 0L) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: No current GLFW OpenGL context; falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }
        if (!InstancedGlCompat.supportsInstancedAttributeDivisor()) {
            MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Instancing entrypoints unavailable. Falling back to non-instanced render path.");
            data.close();
            initialized = false;
            return;
        }

        indexCount = data.indices != null ? data.indices.remaining() : 0;
        setObjBboxFrom(data);

        if (data.bytesPerVertex != SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES) {
            throw new IllegalStateException("InstancedStaticPartRenderer expects VboData.bytesPerVertex="
                    + SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES + " got " + data.bytesPerVertex);
        }

        // LAZY OWN-GL (Tier 1 savings): the renderer's VAO/VBO/EBO + instance VBO are
        // NOT created here. On the MDI tier everything draws from the shared
        // MdiGeometryAtlas; the own objects only serve the Tier-2 fallback
        // (flushBatchVanilla/flushFadingVanilla/drawShadowInstances) and renderSingle,
        // so they are built on first need by {@link #ensureOwnGlObjects()} - from the
        // same retained atlas copies that feed the atlas (one CPU copy of the
        // geometry, no per-renderer VRAM mesh while MDI owns the draw). Renderers
        // without a retained copy (degenerate geometry, or the retention below
        // failed) fall back to eager creation right here, before data is closed.
        boolean noAtlasCopyPossible = data.byteBuffer == null || data.indices == null
                || data.indices.remaining() <= 0;
        if (noAtlasCopyPossible && !createOwnGlObjects(data.byteBuffer, data.indices)) {
            throw new IllegalStateException("Failed to create own VAO/VBO/EBO!");
        }

        try {
            // calloc: every slot must start FADE_MODE_INVISIBLE. A high-water-mark
            // walk over unwritten slots would otherwise read heap garbage with
            // fadeMode >= 0 and render corrupted instances. Lazy CPU capacity:
            // starts at currentAllocatedInstances, grows via ensureCpuInstanceCapacity.
            instanceBuffer = MemoryUtil.memCallocFloat(currentAllocatedInstances * instanceDataSize);
            this.instanceBufferAddress = MemoryUtil.memAddress0(instanceBuffer);
            for (int s = 0; s < currentAllocatedInstances; s++) {
                MemoryUtil.memPutFloat(
                        this.instanceBufferAddress + (long) (s * instanceDataSize + FADE_FLOAT_OFFSET) * 4L,
                        FADE_MODE_INVISIBLE);
            }
            // fadingSnapshot is allocated lazily in deferFading (most renderers never
            // fade and under Iris nothing is ever deferred) - the Cleaner must track
            // it through a MUTABLE holder: a final local would go stale (dangling /
            // leak) the first time the snapshot is reallocated.
            final OffHeapAddrs offHeap = new OffHeapAddrs();
            // Same value as instanceBufferAddress: position is 0 right after
            // memCallocFloat, but keep the holder seeded with the
            // position-independent base (memAddress0) so the growth path in
            // ensureCpuInstanceCapacity can free the old block exactly once.
            offHeap.instanceAddr = MemoryUtil.memAddress0(instanceBuffer);
            this.offHeapAddrs = offHeap;
            instanceBufferCleanable = CLEANER.register(this, () -> {
                try {
                    synchronized (offHeap) {
                        if (offHeap.instanceAddr != 0L) {
                            MemoryUtil.nmemFree(offHeap.instanceAddr);
                            offHeap.instanceAddr = 0L;
                        }
                        if (offHeap.fadingAddr != 0L) {
                            MemoryUtil.nmemFree(offHeap.fadingAddr);
                            offHeap.fadingAddr = 0L;
                        }
                    }
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("Failed to free instanceBuffer via Cleaner", t);
                }
            });
            // Second Cleaner action for the GL names (VAO/VBO/EBO/instance VBO): they
            // must NOT be deleted on the Cleaner thread (no current GL context), so
            // they are parked for the render thread (drainPendingGlDeletions). An
            // explicit cleanup() resets the fields to 0 first, so a cleaned renderer
            // enqueues nothing live here (no double delete).
            // R4-2.1 (as corrected): the watched object is this renderer and the action
            // is built by the static factory below, so the action captures ONLY the
            // GlReleaseState snapshot - never the renderer. A lambda reading
            // this.vaoId/this.vramBytes would capture the renderer itself, keeping it
            // strongly reachable from the Cleaner forever (permanent Java-heap +
            // GL-name leak of every dropped renderer). Note the action must also not
            // be registered against glState itself: Cleaner holds a strong reference
            // to the action, so a self-capturing action would make glState reachable
            // forever and the GL-release path would never fire.
            GlReleaseState glState = new GlReleaseState();
            glState.vaoId = vaoId;
            glState.vboId = vboId;
            glState.eboId = eboId;
            glState.instanceVboId = instanceVboId;
            glState.fadingInstanceVboId = fadingInstanceVboId;
            glState.vramBytes = vramBytes;
            this.glReleaseState = glState;
            // Watched object MUST be this renderer; the action is built by a static
            // factory so it captures ONLY glState. A lambda that both captures glState
            // and is registered against glState never fires: Cleaner holds a STRONG
            // reference to the action until cleaning, so glState could never become
            // phantom-reachable - GL names of renderers dropped without cleanup()
            // would leak forever (Java heap + VRAM + F3 counters).
            this.glNamesCleanable = CLEANER.register(this, createGlNamesCleanAction(glState));

            if (!noAtlasCopyPossible) {
                try {
                    java.nio.ByteBuffer srcVb = data.byteBuffer.duplicate();
                    atlasVertexBytesRetained = MemoryUtil.memAlloc(srcVb.remaining());
                    atlasVertexBytesRetained.put(srcVb);
                    atlasVertexBytesRetained.flip();

                    java.nio.IntBuffer srcIb = data.indices.duplicate();
                    atlasIndexCountRetained = srcIb.remaining();
                    atlasIndicesRetained = MemoryUtil.memAllocInt(atlasIndexCountRetained);
                    atlasIndicesRetained.put(srcIb);
                    atlasIndicesRetained.flip();
                } catch (Throwable t) {
                    MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: failed to retain MDI atlas copy ({}); MDI path will skip this renderer", t.toString());
                    if (atlasVertexBytesRetained != null) {
                        MemoryUtil.memFree(atlasVertexBytesRetained);
                        atlasVertexBytesRetained = null;
                    }
                    if (atlasIndicesRetained != null) {
                        MemoryUtil.memFree(atlasIndicesRetained);
                        atlasIndicesRetained = null;
                    }
                    atlasIndexCountRetained = 0;
                    // No CPU copy to defer the own GL objects from - build them right
                    // now, from data, while it is still open (the old always-eager
                    // capability): Tier-2 fallback keeps working, only MDI skips.
                    if (!createOwnGlObjects(data.byteBuffer, data.indices)) {
                        throw new IllegalStateException("Failed to create own VAO/VBO/EBO after atlas-copy retention failure!");
                    }
                }
            }

            // VRAM accounting: with lazy own-GL a Tier-1 renderer holds NO mesh/instance
            // VRAM here (ensureOwnGlObjects adds the bytes via adjustVram on first
            // Tier-2 use); the eager fallback paths above are accounted immediately.
            long vram = (vaoId > 0)
                    ? (data.byteBuffer != null ? data.byteBuffer.remaining() : 0L)
                      + ((long) indexCount * 4L)
                      + ((long) instanceVboCapFloats * 4L)
                    : 0L;

            data.close();
            initialized = true;
            vramBytes = vram;
            if (glReleaseState != null) {
                glReleaseState.vramBytes = vram; // keep the Cleaner snapshot current (R4-2.1)
            }
            NucleusDebug.addRendererVram(vram);
            synchronized (ACTIVE_RENDERERS) {
                ACTIVE_RENDERERS.add(this);
            }

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize InstancedStaticPartRenderer", e);
            try { data.close(); } catch (Throwable ignored) {}
            if (vaoId != -1) {
                try { GL30.glDeleteVertexArrays(vaoId); } catch (Throwable ignored) {}
                vaoId = -1;
            }
            if (vboId != -1) {
                try { GL15.glDeleteBuffers(vboId); } catch (Throwable ignored) {}
                vboId = -1;
            }
            if (eboId != -1) {
                try { GL15.glDeleteBuffers(eboId); } catch (Throwable ignored) {}
                eboId = -1;
            }
            if (instanceVboId != -1) {
                try { GL15.glDeleteBuffers(instanceVboId); } catch (Throwable ignored) {}
                instanceVboId = -1;
            }
            // R4-2.1: these names are already deleted - consume them in the Cleaner
            // snapshot so a later GC-driven clean() cannot park (and double-delete,
            // possibly a recycled name) them.
            if (glReleaseState != null) {
                glReleaseState.vaoId = 0;
                glReleaseState.vboId = 0;
                glReleaseState.eboId = 0;
                glReleaseState.instanceVboId = 0;
            }
            cleanup();
            initialized = false;
        }
    }

    /**
     * Creates the renderer's OWN VAO/VBO/EBO + instance VBO from the given CPU
     * mesh bytes and records attribs 0..3 (mesh) and 4..15 (instances) into the
     * VAO. Render thread only (GL context required); restores the previous
     * VAO/ARRAY_BUFFER bindings. On success the names are published to the
     * {@link GlReleaseState} Cleaner snapshot so a GC'd renderer cannot leak them.
     *
     * @return true on success; on failure any partial objects are deleted and the
     *         fields reset to -1 (the caller decides whether that is fatal).
     */
    private boolean createOwnGlObjects(@Nullable java.nio.ByteBuffer vertexBytes,
                                       @Nullable java.nio.IntBuffer indices) {
        try {
            int previousVao = GlVaoSafety.currentBinding();
            int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            try {
                vaoId = GL30.glGenVertexArrays();
                vboId = GL15.glGenBuffers();

                if (vaoId == 0 || vboId == 0) {
                    throw new IllegalStateException("Failed to generate VAO/VBO!");
                }

                int meshStride = SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES;

                GlVaoSafety.bindVertexArray(vaoId);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBytes, GL15.GL_STATIC_DRAW);

                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, meshStride, 0);
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, meshStride, 12);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, meshStride, 24);
                GL30.glEnableVertexAttribArray(3);
                GL30.glVertexAttribIPointer(3, 1, GL11.GL_INT, meshStride, 32);

                if (indices != null && indices.remaining() > 0) {
                    eboId = GL15.glGenBuffers();
                    if (eboId == 0) {
                        throw new IllegalStateException("Failed to generate EBO!");
                    }
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
                    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
                }

                instanceVboId = GL15.glGenBuffers();
                if (instanceVboId == 0) {
                    throw new IllegalStateException("Failed to generate instance VBO!");
                }

                // Lazy GROWTH of the data store: it starts at a small fixed allocation
                // and is grown on the first uploadToInstanceVboSpanned call
                // (ensureInstanceVboStore) instead of committing maxInstances*stride
                // bytes of VRAM per part up front. The store itself must exist NOW:
                // attribs 4..15 below record pointers into this buffer, and a buffer
                // object without a data store makes every draw against this VAO an
                // invalid operation / driver fault.
                instanceVboCapFloats = 64 * instanceDataSize;
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) instanceVboCapFloats * 4L, GL15.GL_STREAM_DRAW);

                int stride = instanceDataSize * 4;

                GL20.glEnableVertexAttribArray(4);
                GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, POS_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(4, 1);

                // InstRot (loc 5): 4 snorm16 halves packed in 2 words — GL_SHORT
                // normalized makes the vsh see [-1,1] floats with no decode.
                GL20.glEnableVertexAttribArray(5);
                GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, true, stride, QUAT_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(5, 1);

                GL20.glEnableVertexAttribArray(6);
                GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, BBOX_MIN_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(6, 1);

                GL20.glEnableVertexAttribArray(7);
                GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, BBOX_SIZE_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(7, 1);

                // InstLightC0x (locs 8-11): 4 u8 corners per word, GL_UNSIGNED_BYTE
                // normalized (0..1); the vsh decodes to the 0..240 texel grid by x240.
                GL20.glEnableVertexAttribArray(8);
                GL20.glVertexAttribPointer(8, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 0) * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(8, 1);
                GL20.glEnableVertexAttribArray(9);
                GL20.glVertexAttribPointer(9, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 1) * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(9, 1);
                GL20.glEnableVertexAttribArray(10);
                GL20.glVertexAttribPointer(10, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 2) * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(10, 1);
                GL20.glEnableVertexAttribArray(11);
                GL20.glVertexAttribPointer(11, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 3) * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(11, 1);

                // InstUvRect vec4 (loc 12): sprite-rect remap from normalized VBO to atlas.
                GL20.glEnableVertexAttribArray(12);
                GL20.glVertexAttribPointer(12, 4, GL11.GL_FLOAT, false, stride, UVRECT_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(12, 1);

                // InstColor vec4 (loc 13): per-instance RGBA tint (white = passthrough).
                GL20.glEnableVertexAttribArray(13);
                GL20.glVertexAttribPointer(13, 4, GL11.GL_FLOAT, false, stride, TINT_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(13, 1);

                // GradParams vec4 (loc 14): spatial tint falloff (axis<0 = off).
                GL20.glEnableVertexAttribArray(14);
                GL20.glVertexAttribPointer(14, 4, GL11.GL_FLOAT, false, stride, GRAD_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(14, 1);

                // AnimParams vec4 (loc 15): parametric GPU animation (xyz = parameters,
                // w = joint index; w < 0 = no parametrics - the shader skips the joint delta).
                GL20.glEnableVertexAttribArray(15);
                GL20.glVertexAttribPointer(15, 4, GL11.GL_FLOAT, false, stride, ANIM_FLOAT_OFFSET * 4L);
                InstancedGlCompat.glVertexAttribDivisorCompat(15, 1);

                GlVaoSafety.bindVertexArray(0);
            } finally {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
                GlVaoSafety.bindVertexArray(previousVao);
            }
            // Publish to the Cleaner snapshot (the lazy analogue of the constructor
            // snapshot): a renderer GC'd without cleanup() must park these names for
            // drainPendingGlDeletions, and cleanup()'s consume step must see them.
            if (glReleaseState != null) {
                glReleaseState.vaoId = vaoId;
                glReleaseState.vboId = vboId;
                glReleaseState.eboId = eboId;
                glReleaseState.instanceVboId = instanceVboId;
            }
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("Failed to create own VAO/VBO/EBO/instance VBO", t);
            if (vaoId > 0) {
                try { GL30.glDeleteVertexArrays(vaoId); } catch (Throwable ignored) {}
                vaoId = -1;
            }
            if (vboId > 0) {
                try { GL15.glDeleteBuffers(vboId); } catch (Throwable ignored) {}
                vboId = -1;
            }
            if (eboId > 0) {
                try { GL15.glDeleteBuffers(eboId); } catch (Throwable ignored) {}
                eboId = -1;
            }
            if (instanceVboId > 0) {
                try { GL15.glDeleteBuffers(instanceVboId); } catch (Throwable ignored) {}
                instanceVboId = -1;
            }
            instanceVboCapFloats = 0;
            if (glReleaseState != null) {
                glReleaseState.vaoId = 0;
                glReleaseState.vboId = 0;
                glReleaseState.eboId = 0;
                glReleaseState.instanceVboId = 0;
            }
            return false;
        }
    }

    /**
     * Lazy gate for every own-GL draw path (Tier-2 fallback: flushBatchVanilla /
     * flushFadingVanilla / drawShadowInstances, and renderSingle): creates the
     * renderer's VAO/VBO/EBO/instance VBO on first need, uploading the mesh from
     * the SAME retained CPU copies that feed {@link MdiGeometryAtlas} - one copy
     * of the geometry serves the atlas registration, the atlas re-registration
     * after a repack/RESOURCE_RELOAD, and the Tier-2 upload. On a pure Tier-1
     * (MDI) renderer this never runs: no per-part mesh VRAM is ever committed.
     */
    boolean ensureOwnGlObjects() {
        if (vaoId > 0) {
            return true;
        }
        if (!initialized) {
            return false;
        }
        if (atlasVertexBytesRetained == null || atlasIndicesRetained == null
                || atlasIndexCountRetained <= 0) {
            return false;
        }
        if (!createOwnGlObjects(atlasVertexBytesRetained, atlasIndicesRetained)) {
            return false;
        }
        // Same bytes the constructor used to account up front (mesh + indices +
        // the initial 64-record instance store).
        adjustVram((long) atlasVertexBytesRetained.remaining()
                + ((long) atlasIndexCountRetained * 4L)
                + ((long) instanceVboCapFloats * 4L));
        return true;
    }

    // ── Instance data write ────────────────────────────────────────────

    /**
     * Converts the BER part pose (raw {@code poseStack.last().pose()}) into world-space
     * posTmp/rotTmp: {@code world = T(relCam) * L^-1 * pose}, where {@code L} is the
     * per-frame level pose basis noted by {@link FrameViewState#noteFrameBasis}
     * ({@code R_cam * bob} on 1.20.1, bob on 1.21.1). One matrix multiply per part.
     * The {@code L^-1} strips the view bob, so the record is genuinely static: camera
     * AND bob motion do not "dirty" the instances (GpuSpanUploader gives zero upload
     * for a static scene); both are applied in the vsh via ModelViewMat =
     * {@link FrameViewState#viewMatrix()} = modelView * L * T(-relCam).
     */
    void convertToWorldRecord(Matrix4f pose) {
        tmpWorldMat.set(FrameViewState.poseToWorldRotation()).mul(pose);
        tmpWorldMat.getTranslation(posTmp);
        posTmp.add(FrameViewState.relCamX(), FrameViewState.relCamY(), FrameViewState.relCamZ());
        tmpWorldMat.getNormalizedRotation(rotTmp);
    }

    void memPutInstanceRecordAtBaseFloat(int baseFloatIndex, float[] lightBbox) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        MemoryUtil.memPutFloat(a + (long) POS_FLOAT_OFFSET * 4L, posTmp.x);
        MemoryUtil.memPutFloat(a + (long) POS_FLOAT_OFFSET * 4L + 4, posTmp.y);
        MemoryUtil.memPutFloat(a + (long) POS_FLOAT_OFFSET * 4L + 8, posTmp.z);
        // Quat: 4 snorm16 halves packed into 2 words (see packQuatWord) - the VAO
        // reads them as GL_SHORT normalized, the compute shaders decode via ALU.
        MemoryUtil.memPutFloat(a + (long) QUAT_FLOAT_OFFSET * 4L,
                Float.intBitsToFloat(packQuatWord(rotTmp.x, rotTmp.y)));
        MemoryUtil.memPutFloat(a + (long) QUAT_FLOAT_OFFSET * 4L + 4,
                Float.intBitsToFloat(packQuatWord(rotTmp.z, rotTmp.w)));
        MemoryUtil.memPutFloat(a + (long) BBOX_MIN_FLOAT_OFFSET * 4L, lightBbox[0]);
        MemoryUtil.memPutFloat(a + (long) BBOX_MIN_FLOAT_OFFSET * 4L + 4, lightBbox[1]);
        MemoryUtil.memPutFloat(a + (long) BBOX_MIN_FLOAT_OFFSET * 4L + 8, lightBbox[2]);
        float sx = lightBbox[3] - lightBbox[0];
        float sy = lightBbox[4] - lightBbox[1];
        float sz = lightBbox[5] - lightBbox[2];
        MemoryUtil.memPutFloat(a + (long) BBOX_SIZE_FLOAT_OFFSET * 4L, sx);
        MemoryUtil.memPutFloat(a + (long) BBOX_SIZE_FLOAT_OFFSET * 4L + 4, sy);
        MemoryUtil.memPutFloat(a + (long) BBOX_SIZE_FLOAT_OFFSET * 4L + 8, sz);
        // Fade slot: static MODE (see FADE_MODE_*), not a per-frame alpha - the
        // GPU evaluates the distance ramp (uStaticFadeStart/End uniforms), so the
        // record stays camera-independent and the span-diff converges to zero.
        // (The old quantized per-frame alpha churned every frame of motion for
        // every machine inside the fade ring.)
        MemoryUtil.memPutFloat(a + (long) FADE_FLOAT_OFFSET * 4L, tmpFadeMode);
        // Corner light: 8 corners x 2 u8 packed into 4 words (see packLightWord).
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int w = 0; w < 4; w++) {
            int b = w * 4;
            MemoryUtil.memPutFloat(lightA + (long) w * 4L, Float.intBitsToFloat(
                    packLightWord(tmpCornerUV[b], tmpCornerUV[b + 1], tmpCornerUV[b + 2], tmpCornerUV[b + 3])));
        }
        long uvA = a + (long) UVRECT_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(uvA, tmpUvRect[0]);
        MemoryUtil.memPutFloat(uvA + 4, tmpUvRect[1]);
        MemoryUtil.memPutFloat(uvA + 8, tmpUvRect[2]);
        MemoryUtil.memPutFloat(uvA + 12, tmpUvRect[3]);
        long tintA = a + (long) TINT_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(tintA, tmpTint[0]);
        MemoryUtil.memPutFloat(tintA + 4, tmpTint[1]);
        MemoryUtil.memPutFloat(tintA + 8, tmpTint[2]);
        MemoryUtil.memPutFloat(tintA + 12, tmpTint[3]);
        long gradA = a + (long) GRAD_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(gradA, tmpGrad[0]);
        MemoryUtil.memPutFloat(gradA + 4, tmpGrad[1]);
        MemoryUtil.memPutFloat(gradA + 8, tmpGrad[2]);
        MemoryUtil.memPutFloat(gradA + 12, tmpGrad[3]);
        long animA = a + (long) ANIM_FLOAT_OFFSET * 4L;
        MemoryUtil.memPutFloat(animA, tmpAnimParams[0]);
        MemoryUtil.memPutFloat(animA + 4, tmpAnimParams[1]);
        MemoryUtil.memPutFloat(animA + 8, tmpAnimParams[2]);
        MemoryUtil.memPutFloat(animA + 12, tmpAnimParams[3]);
        this.recordMutationEpoch++;
    }



    /**
     * Whether the renderer is ready for {@code MdiBatchCoordinator.submitClean}: during this
     * phase not a single write to the buffer happened, and the buffer is in sync with the coordinator's snapshot.
     */
    boolean canSubmitMdiClean() {
        return ClientRenderFlags.mdiCleanFrameReuse() && !mdiRecordWriteHappened && mdiBufferSynced;
    }

    /** The buffer matches the coordinator's snapshot again (accepted submit / clean re-assert). */
    void noteMdiDispatched() {
        mdiBufferSynced = true;
        mdiRecordWriteHappened = false;
    }

    /** Sync lost: direct path, partition reordering, renderSingle, submit refusal. */
    void noteMdiDispatchLost() {
        mdiBufferSynced = false;
        // The buffer layout behind slotPosQuatStable is void: renderSingle rewrote
        // slot 0 and the direct-path fade partition (partitionInstancesOpaqueFirst)
        // permutes records+keys WITHOUT permuting the stability flags. Drop them all -
        // the next add phase re-establishes them through the full posQuat compare
        // (static-pose short-circuit safety: a stale flag would let a slot skip the
        // posQuat validation while holding ANOTHER machine's pose).
        java.util.Arrays.fill(slotPosQuatStable, false);
        java.util.Arrays.fill(slotPosQuatVerified, false);
        this.recordMutationEpoch++;
        this.slotLayoutEpoch++;
    }

    // Lazy data stores of instanceVboId / fadingInstanceVboId, in floats. Both are
    // created by createOwnGlObjects / on first fading use and sized here on the
    // first upload - maxInstances*stride VRAM is only committed by renderers that
    // actually grow that big.
    private int instanceVboCapFloats = 0;
    private int fadingVboCapFloats = 0;

    private void adjustVram(long delta) {
        if (delta == 0L) {
            return;
        }
        vramBytes += delta;
        NucleusDebug.addRendererVram(delta);
        if (glReleaseState != null) {
            glReleaseState.vramBytes = vramBytes;
        }
    }

    /**
     * Lazily creates or grows the main instance VBO data store to at least
     * {@code needFloats} (power-of-two). Safe against stale reads: every draw path
     * uploads its full window through {@link #uploadToInstanceVboSpanned} right
     * before drawing, so a freshly (re)specified store never serves content that
     * was never written.
     */
    private void ensureInstanceVboStore(int needFloats) {
        if (needFloats <= instanceVboCapFloats || instanceVboId <= 0) {
            return;
        }
        long oldBytes = (long) instanceVboCapFloats * 4L;
        int cap = Math.max(64 * instanceDataSize, instanceVboCapFloats);
        while (cap < needFloats) {
            cap <<= 1;
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) cap * 4L, GL15.GL_STREAM_DRAW);
        // Re-specifying the store orphans the old GPU contents; the shadow no longer
        // describes it (the next upload rewrites the full window anyway).
        this.vboShadowValid = false;
        this.vboShadowValidFloats = 0;
        instanceVboCapFloats = cap;
        adjustVram((long) cap * 4L - oldBytes);
    }

    /** Fading-VBO twin of {@link #ensureInstanceVboStore(int)}. */
    private void ensureFadingVboStore(int needFloats) {
        if (needFloats <= fadingVboCapFloats || fadingInstanceVboId <= 0) {
            return;
        }
        long oldBytes = (long) fadingVboCapFloats * 4L;
        int cap = Math.max(64 * instanceDataSize, fadingVboCapFloats);
        while (cap < needFloats) {
            cap <<= 1;
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fadingInstanceVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) cap * 4L, GL15.GL_STREAM_DRAW);
        this.fadingVboShadowValid = false;
        this.fadingVboShadowValidFloats = 0;
        fadingVboCapFloats = cap;
        adjustVram((long) cap * 4L - oldBytes);
    }

    /**
     * Lazily allocates or grows {@link #fadingSnapshot} to hold {@code records}
     * instances (power-of-two floats). The old buffer is freed through the
     * {@link OffHeapAddrs} holder (the same address the Cleaner would free) so the
     * holder never keeps a dangling address.
     */
    private void ensureFadingSnapshot(int records) {
        int need = records * instanceDataSize;
        if (need <= 0 || (fadingSnapshot != null && fadingSnapshot.capacity() >= need)) {
            return;
        }
        int cap = 64 * instanceDataSize;
        while (cap < need) {
            cap <<= 1;
        }
        FloatBuffer fresh = MemoryUtil.memAllocFloat(cap);
        OffHeapAddrs oh = this.offHeapAddrs;
        if (oh != null) {
            synchronized (oh) {
                if (oh.fadingAddr != 0L) {
                    MemoryUtil.nmemFree(oh.fadingAddr);
                }
                // Position-independent base (fresh is at position 0 here, but the
                // holder address is freed by the Cleaner - never derive it from
                // a mutable cursor).
                oh.fadingAddr = MemoryUtil.memAddress0(fresh);
            }
        }
        fadingSnapshot = fresh;
    }

    /**
     * Geometrically grows the per-instance CPU storage (instanceBuffer staging +
     * every per-slot array) so slot {@code requiredSlot} becomes addressable,
     * capped at {@link #maxInstances}. Content-preserving: existing records are
     * copied, new slots start FADE_MODE_INVISIBLE (the calloc invariant from the
     * constructor), the occlusion-key range of the new slots is poisoned with
     * Long.MIN_VALUE, and the Cleaner's off-heap address travels through the
     * {@link OffHeapAddrs} holder so a GC-driven free never sees a dangling
     * pointer. Called right after a successful slot allocation.
     */
    private void ensureCpuInstanceCapacity(int requiredSlot) {
        if (requiredSlot < 0 || requiredSlot < currentAllocatedInstances || requiredSlot >= maxInstances) {
            return;
        }
        int newCap = Math.min(maxInstances, Math.max(currentAllocatedInstances * 2, requiredSlot + 1));
        if (newCap <= currentAllocatedInstances) {
            return;
        }
        int oldCap = currentAllocatedInstances;
        // Preserve the staging position across the reallocation: callers treat
        // instanceBuffer.position() as the byte/float write cursor
        // (instanceDataSize * instanceCount) and the flush paths flip from it.
        int prevPos = instanceBuffer != null ? instanceBuffer.position() : 0;
        // IMPORTANT: address everything through the position-independent bases
        // (memAddress0 / this.instanceBufferAddress). MemoryUtil.memAddress(buf)
        // adds buf.position() * 4 to the base pointer, so with the cursor at
        // instanceDataSize * 64 the old code computed an address one byte past
        // the end of the freshly allocated buffer and memCopy'd FROM it —
        // EXCEPTION_ACCESS_VIOLATION (see hs_err on the instanceCount == 64
        // growth edge).
        FloatBuffer fresh = MemoryUtil.memCallocFloat(newCap * instanceDataSize);
        long freshAddr = MemoryUtil.memAddress0(fresh);
        if (this.instanceBufferAddress != 0L) {
            MemoryUtil.memCopy(this.instanceBufferAddress, freshAddr,
                    (long) oldCap * instanceDataSize * 4L);
        }
        // New slots: zeroed by calloc, but the fade word must carry the invisible
        // MODE explicitly (see the constructor's calloc comment).
        for (int s = oldCap; s < newCap; s++) {
            MemoryUtil.memPutFloat(freshAddr + (long) (s * instanceDataSize + FADE_FLOAT_OFFSET) * 4L,
                    FADE_MODE_INVISIBLE);
        }
        ((Buffer) fresh).position(prevPos);
        long oldAddr = this.instanceBufferAddress;
        this.instanceBufferAddress = freshAddr;
        this.instanceBuffer = fresh;
        OffHeapAddrs oh = this.offHeapAddrs;
        if (oh != null) {
            synchronized (oh) {
                // oh.instanceAddr IS oldAddr (same allocation, kept in sync by the
                // constructor and this method) - free the old block exactly once
                // and re-point the Cleaner holder at the fresh base.
                if (oh.instanceAddr != 0L && oh.instanceAddr != oldAddr) {
                    // Stale holder (should not happen): free what it tracks.
                    MemoryUtil.nmemFree(oh.instanceAddr);
                }
                oh.instanceAddr = freshAddr;
            }
        } else if (oldAddr != 0L) {
            // No Cleaner holder (partially constructed renderer): free directly.
            MemoryUtil.nmemFree(oldAddr);
        }
        this.instanceCullIndices = java.util.Arrays.copyOf(this.instanceCullIndices, newCap);
        this.instanceOcclusionKeys = java.util.Arrays.copyOf(this.instanceOcclusionKeys, newCap);
        // Unwritten slots must never alias a live posKey.
        java.util.Arrays.fill(this.instanceOcclusionKeys, oldCap, newCap, Long.MIN_VALUE);
        this.slotPosQuatStable = java.util.Arrays.copyOf(this.slotPosQuatStable, newCap);
        this.slotPosQuatVerified = java.util.Arrays.copyOf(this.slotPosQuatVerified, newCap);
        this.instanceLightUV = java.util.Arrays.copyOf(this.instanceLightUV, newCap * 2);
        this.currentAllocatedInstances = newCap;
    }

    /**
     * Span upload of the data window into the direct-path instance VBO (offset 0):
     * compares against the shadow copy and uploads only changed ranges.
     * Orphaning removed: when spans are skipped the GPU buffer must KEEP the old
     * content; glBufferData(orphan) would destroy it.
     */
    void uploadToInstanceVboSpanned(FloatBuffer src, int srcFloatOffset, int destFloatOffset, int floats) {
        if (floats <= 0 || instanceVboId <= 0) {
            return;
        }
        ensureInstanceVboStore(destFloatOffset + floats);
        if (this.lastAnchorGen != activeAnchorGen) {
            this.lastAnchorGen = activeAnchorGen;
            this.vboShadowValid = false;
            this.vboShadowValidFloats = 0;
            this.mdiBufferSynced = false;
        }
        int need = 64;
        while (need < floats) {
            need <<= 1;
        }
        if (vboShadow == null || vboShadowFloats < floats) {
            if (vboShadow != null) {
                MemoryUtil.memFree(vboShadow);
            }
            vboShadow = MemoryUtil.memAllocFloat(need);
            vboShadowFloats = need;
            vboShadowValid = false;
            vboShadowValidFloats = 0;
        }
        // Diff against the shadow when it mirrors the GPU window (differential
        // span upload); a full upload only (re)seeds it.
        if (vboShadowValid) {
            GpuSpanUploader.diffUpload(vboShadow, destFloatOffset, vboShadowValidFloats,
                    src, srcFloatOffset, destFloatOffset, floats, instanceVboId);
        } else {
            GpuSpanUploader.fullUpload(vboShadow, destFloatOffset, src, srcFloatOffset, floats, instanceVboId);
            vboShadowValid = true;
        }
        vboShadowValidFloats = destFloatOffset + floats;
    }

    void uploadInstanceStreamToBoundVbo() {
        // instanceBuffer is already flipped: valid records are [0, remaining).
        uploadToInstanceVboSpanned(instanceBuffer, 0, 0, instanceBuffer.remaining());
    }

    /**
     * Span upload of the phase-2 fading window into the dedicated fading VBO
     * (lazily created), diffed against its own shadow. See the field comment on
     * {@link #fadingInstanceVboId} for why the fading phase must not share the
     * opaque phase's VBO region.
     */
    void uploadToFadingVboSpanned(FloatBuffer src, int floats) {
        if (floats <= 0 || instanceVboId <= 0) {
            return;
        }
        if (fadingInstanceVboId <= 0) {
            fadingInstanceVboId = GL15.glGenBuffers();
            // R4-2.1: created lazily after the Cleaner snapshot was taken - keep the
            // snapshot current or a GC'd renderer leaks this GL name.
            if (glReleaseState != null) {
                glReleaseState.fadingInstanceVboId = fadingInstanceVboId;
            }
            // The data store is sized lazily in ensureFadingVboStore below.
            fadingVboShadowValid = false;
            fadingVboShadowValidFloats = 0;
        }
        if (this.lastAnchorGen != activeAnchorGen) {
            this.lastAnchorGen = activeAnchorGen;
            this.fadingVboShadowValid = false;
            this.fadingVboShadowValidFloats = 0;
        }
        int need = 64;
        while (need < floats) {
            need <<= 1;
        }
        ensureFadingVboStore(floats);
        if (fadingVboShadow == null || fadingVboShadowFloats < floats) {
            if (fadingVboShadow != null) {
                MemoryUtil.memFree(fadingVboShadow);
            }
            fadingVboShadow = MemoryUtil.memAllocFloat(need);
            fadingVboShadowFloats = need;
            fadingVboShadowValid = false;
            fadingVboShadowValidFloats = 0;
        }
        // Differential span upload against the fading shadow (see
        // uploadToInstanceVboSpanned); full upload only (re)seeds it.
        if (fadingVboShadowValid) {
            GpuSpanUploader.diffUpload(fadingVboShadow, 0, fadingVboShadowValidFloats,
                    src, 0, 0, floats, fadingInstanceVboId);
        } else {
            GpuSpanUploader.fullUpload(fadingVboShadow, 0, src, 0, floats, fadingInstanceVboId);
            fadingVboShadowValid = true;
        }
        fadingVboShadowValidFloats = floats;
    }

    /**
     * Re-points the instance attribs (4..15) of the CURRENTLY BOUND VAO at
     * {@code vboId}. Used to draw the phase-2 fading window from
     * {@link #fadingInstanceVboId} and to restore the attribs back to
     * {@link #instanceVboId} afterwards (attrib pointers are VAO state - a
     * later plain ARRAY_BUFFER bind does not re-point them).
     */
    void pointInstanceAttribsTo(int vboId) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        int stride = instanceDataSize * 4;
        GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, POS_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, true, stride, QUAT_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, BBOX_MIN_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, BBOX_SIZE_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(8, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 0) * 4L);
        GL20.glVertexAttribPointer(9, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 1) * 4L);
        GL20.glVertexAttribPointer(10, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 2) * 4L);
        GL20.glVertexAttribPointer(11, 4, GL11.GL_UNSIGNED_BYTE, true, stride, (LIGHT_FLOAT_OFFSET + 3) * 4L);
        GL20.glVertexAttribPointer(12, 4, GL11.GL_FLOAT, false, stride, UVRECT_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(13, 4, GL11.GL_FLOAT, false, stride, TINT_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(14, 4, GL11.GL_FLOAT, false, stride, GRAD_FLOAT_OFFSET * 4L);
        GL20.glVertexAttribPointer(15, 4, GL11.GL_FLOAT, false, stride, ANIM_FLOAT_OFFSET * 4L);
    }

    protected List<BakedQuad> getQuadsForIrisPath() {
        return quadsForIris;
    }

    // ── renderSingle ───────────────────────────────────────────────────

    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity) {
        renderSingle(poseStack, packedLight, blockPos, blockEntity, null);
    }

    @Override
    public void renderSingle(PoseStack poseStack, int packedLight, BlockPos blockPos,
                             @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        if (!initialized || !ensureOwnGlObjects() || indexCount <= 0 || instanceVboId <= 0 || instanceBuffer == null) return;

        if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                // Fallback goes to bufferSource (drawn at endBatch) - the companion VAO
                // of the shadow batch must be detached (per-part release in shadow is disabled).
                com.hbm_m.client.render.shader.IrisRenderBatch.detachCompanionVaoForVanillaWork();
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    RenderHooks.putBulkDataShaded(consumer, pose, quad, fade, packedLight, OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        vanillaHelper.renderSingleVanilla(poseStack, packedLight, blockPos, blockEntity, bufferSource);
    }

    // ── addInstance ─────────────────────────────────────────────────────

    /**
     * Fast-path dirty-skip: can slot {@code instanceCount} be confirmed by a roster
     * assert for machine {@code posKey} without rebuilding the record?
     * Roster keys live in {@link #instanceOcclusionKeys} (= pos.asLong(), written both
     * in addInstance and in the assert) - the buffer contents match slot k as long as
     * no record in [0, k] has been reordered/overwritten by another machine.
     * Read-only - safe to call during the checking phase (before commit).
     */
    public boolean canAssertInstance(long posKey) {
        return initialized && instanceBuffer != null
                && slotAllocator != null
                && slotAllocator.hasSlot(posKey);
    }

    // The former 2-arg canAssertInstance(posKey, expectedQuantizedFade) checked the
    // per-frame distance fade of the buffer record: the record's fade slot stored a
    // camera-dependent alpha that changed every frame of motion without a dirty
    // event, and the assert had to verify it. The slot now stores a static fade
    // MODE (see FADE_MODE_*) - the distance ramp lives entirely on the GPU, the
    // record cannot go stale, and the key-only assert is sufficient.

    /**
     * Commits a roster assert for machine 64-bit coordinate {@code posKey}: marks slot stability flags
     * without streaming counter increments.
     * Call ONLY after {@link #canAssertInstance}.
     */
    public boolean assertCleanInstance(long posKey) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false;
        }
        if (!initialized || instanceBuffer == null || slotAllocator == null) {
            return false;
        }
        int slot = slotAllocator.getSlot(posKey);
        if (slot < 0 || slot >= maxInstances) {
            return false;
        }
        ensureCpuInstanceCapacity(slot);
        if (batchSkyDarken < 0f) {
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }
        if (slot < instanceCullIndices.length) {
            instanceCullIndices[slot] = -1;
        }
        if (slot < slotPosQuatStable.length) {
            slotPosQuatStable[slot] = true;
            slotPosQuatVerified[slot] = true;
        }
        instanceCount = Math.min(maxInstances, Math.max(instanceCount, slotAllocator.getHighWaterMark()));
        int targetPos = Math.min(instanceDataSize * instanceCount, instanceBuffer.capacity());
        ((Buffer) instanceBuffer).position(targetPos);
        return true;
    }

    /**
     * Commits a roster assert for machine {@code blockPos}: delegates to {@link #assertCleanInstance(long)}.
     */
    public boolean assertCleanInstance(BlockPos blockPos) {
        if (blockPos == null) {
            return false;
        }
        long key = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        return assertCleanInstance(key);
    }

    /**
     * Lock-free O(1) slot verification for a frozen roster entry (MachineRenderState): checks that {@code slot} is
     * still owned by {@code posKey} via a direct array read - no Long2IntOpenHashMap
     * lookup, no synchronized monitor (the per-frame roster assert path calls this
     * ~50k times per frame; the map+monitor cost was 12 ms/frame in spark).
     */
    public boolean canAssertSlot(int slot, long posKey) {
        return initialized && instanceBuffer != null
                && slot >= 0 && slot < maxInstances
                && slot < instanceOcclusionKeys.length
                && instanceOcclusionKeys[slot] == posKey;
    }

    /**
     * Lock-free O(1) slot commit for a frozen roster entry after {@link #canAssertSlot(int, long)}
     * succeeded - same flags as {@link #assertCleanInstance(long)} without the map lookup.
     */
    public boolean assertCleanSlot(int slot) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false;
        }
        if (!initialized || instanceBuffer == null
                || slot < 0 || slot >= maxInstances) {
            return false;
        }
        if (batchSkyDarken < 0f) {
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }
        if (slot < instanceCullIndices.length) {
            instanceCullIndices[slot] = -1;
        }
        slotPosQuatStable[slot] = true;
        slotPosQuatVerified[slot] = true;
        if (slot + 1 > instanceCount) {
            instanceCount = Math.min(maxInstances, Math.max(instanceCount, slotAllocator.getHighWaterMark()));
            int targetPos = Math.min(instanceDataSize * instanceCount, instanceBuffer.capacity());
            ((Buffer) instanceBuffer).position(targetPos);
        }
        return true;
    }

    /**
     * Fast-path 4-float AnimParams tail update for an already-verified roster entry
     * slot (call {@link #canAssertSlot(int, long)} first). Skips allocateSlot
     * (Long2IntOpenHashMap), light sampling, matrix conversion and the 28-float
     * prefix compare/write entirely: an actively animating GPU-parametric part only
     * ever changes words 28..31 (and its fade mode word). Raw-int compares keep the
     * skip-write exact (no -0.0/NaN drift).
     */
    public void updateCleanSlotAnimParams(int slot, float a0, float a1, float a2, float a3, float fadeMode) {
        assertCleanSlot(slot);
        long baseAddr = instanceBufferAddress + (long) (slot * instanceDataSize) * 4L;
        long fadeAddr = baseAddr + (long) FADE_FLOAT_OFFSET * 4L;
        if (Float.floatToRawIntBits(MemoryUtil.memGetFloat(fadeAddr)) != Float.floatToRawIntBits(fadeMode)) {
            MemoryUtil.memPutFloat(fadeAddr, fadeMode);
            mdiRecordWriteHappened = true;
            this.recordMutationEpoch++;
        }
        long animAddr = baseAddr + (long) ANIM_FLOAT_OFFSET * 4L;
        if (Float.floatToRawIntBits(MemoryUtil.memGetFloat(animAddr)) != Float.floatToRawIntBits(a0)
                || Float.floatToRawIntBits(MemoryUtil.memGetFloat(animAddr + 4L)) != Float.floatToRawIntBits(a1)
                || Float.floatToRawIntBits(MemoryUtil.memGetFloat(animAddr + 8L)) != Float.floatToRawIntBits(a2)
                || Float.floatToRawIntBits(MemoryUtil.memGetFloat(animAddr + 12L)) != Float.floatToRawIntBits(a3)) {
            MemoryUtil.memPutFloat(animAddr, a0);
            MemoryUtil.memPutFloat(animAddr + 4L, a1);
            MemoryUtil.memPutFloat(animAddr + 8L, a2);
            MemoryUtil.memPutFloat(animAddr + 12L, a3);
            mdiRecordWriteHappened = true;
            this.recordMutationEpoch++;
        }
    }

    /**
     * The instance-buffer contents no longer match the roster keys
     * (renderSingle overwrote the slot-0 RECORD).
     * <p>
     * Poisoning {@link #instanceOcclusionKeys} alone is NOT enough:
     * {@link #canAssertInstance} consults ONLY {@code slotAllocator.hasSlot},
     * so the machine that owned slot 0 would keep passing the assert and render
     * with renderSingle's leftover single-record matrix. Free the owner's
     * allocator slot too - it re-allocates and fully rebuilds its record on the
     * next add phase (matching the old javadoc promise).
     */
    void invalidateRoster() {
        if (instanceOcclusionKeys.length > 0) {
            long staleKey = instanceOcclusionKeys[0];
            if (slotAllocator != null && staleKey != Long.MIN_VALUE
                    && slotAllocator.getSlot(staleKey) == 0) {
                // Frees the bitmap slot AND the posKey->slot mapping (freeSlot(int)
                // clears slotToPosKey + posKeyToSlot via the allocator).
                slotAllocator.freeSlot(0);
            }
            instanceOcclusionKeys[0] = Long.MIN_VALUE;
        }
        // The slot-0 record was overwritten - its posQuat stability is gone.
        if (slotPosQuatStable.length > 0) {
            slotPosQuatStable[0] = false;
            slotPosQuatVerified[0] = false;
        }
    }

    /**
     * Enqueues one instance from a fully-filled {@link PartEnqueueRequest}
     * (obtained via {@link PartEnqueueRequest#obtain()}); see the request fields
     * for the parameter semantics. There are no overloads and no global pending
     * state: a degraded or throwing enqueue leaves the request unread instead of
     * leaking armed statics into an unrelated record (the teleport bug, 10-09).
     */
    @Override
    public void addInstance(PartEnqueueRequest rq) {
        if (!initialized) return;

        tmpAnimParams[0] = rq.animP0;
        tmpAnimParams[1] = rq.animP1;
        tmpAnimParams[2] = rq.animP2;
        tmpAnimParams[3] = rq.animP3;
        tmpFadeMode = rq.fadeMode;

        float[] uvRect = rq.uvRect;
        float[] tint = rq.tint;
        float[] grad = rq.grad;
        if (uvRect != null && uvRect.length >= 4) {
            tmpUvRect[0] = uvRect[0];
            tmpUvRect[1] = uvRect[1];
            tmpUvRect[2] = uvRect[2];
            tmpUvRect[3] = uvRect[3];
        } else {
            tmpUvRect[0] = 0f;
            tmpUvRect[1] = 0f;
            tmpUvRect[2] = 1f;
            tmpUvRect[3] = 1f;
        }
        if (tint != null && tint.length >= 4) {
            tmpTint[0] = tint[0];
            tmpTint[1] = tint[1];
            tmpTint[2] = tint[2];
            tmpTint[3] = tint[3];
        } else {
            tmpTint[0] = 1f;
            tmpTint[1] = 1f;
            tmpTint[2] = 1f;
            tmpTint[3] = 0f;
        }
        if (grad != null && grad.length >= 4 && grad[0] >= 0f) {
            tmpGrad[0] = grad[0];
            tmpGrad[1] = grad[1];
            tmpGrad[2] = grad[2];
            tmpGrad[3] = -1f;
        } else {
            tmpGrad[0] = -1f;
            tmpGrad[1] = 0f;
            tmpGrad[2] = 0f;
            tmpGrad[3] = -1f;
        }
        // Parametric joint CHAIN base (GradParams.w): the request's jointIndex
        // (<0 = no parametrics - the VSH joint block stays off). Written AFTER
        // the falloff config: the falloff no longer owns the 4th component. Without
        // this write every record carried -1 and ALL parametric parts froze at rest
        // (the freeze hunt of 10-05: params/encoder/VRAM all proved live, only the
        // gate word was dead).
        tmpGrad[3] = (float) rq.jointIndex;

        // The machine bounds paired with the shared 8-corner light; null = per-part objBbox.
        float[] lightBbox = rq.lightBbox != null ? rq.lightBbox : objBbox;

        PoseStack poseStack = rq.poseStack;
        int packedLight = rq.packedLight;
        BlockPos blockPos = rq.blockPos;
        BlockEntity blockEntity = rq.blockEntity;
        MultiBufferSource bufferSource = rq.bufferSource;
        float[] sharedCornerUV8 = rq.sharedCornerUV8;
        boolean staticPose = rq.staticPose;

        // Shadow pass (both platforms): immediate drawing through the ACTIVE per-BE
        // batch (fast path, see IrisRenderBatch.begin). This block used to be forge-only:
        // on neoforge 1.21.1 instances accumulated in shadow with shadow matrices and were
        // then flushed in the main pass. Fallback without a batch - putBulkData via
        // bufferSource (SHADOW_BLOCK at endBatch).
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            // Global shadow batch: record instead of an immediate draw - one flush
            // for the whole BE phase (Iris mixin in ShadowRenderer.renderShadows). With
            // the batch disabled/failed - the previous immediate path.
            if (irisHelper.tryRecordShadowInstance(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity)) {
                return;
            }
            if (quadsForIris != null && !quadsForIris.isEmpty() && bufferSource != null) {
                // Fallback to bufferSource with the shadow batch open - see the detach comment in renderSingle.
                // The tint is read from tmpTint (filled earlier in this addInstance) - shadows do
                // not use color, but alpha/overbright must not be lost on the fallback path.
                com.hbm_m.client.render.shader.IrisRenderBatch.detachCompanionVaoForVanillaWork();
                float fade = SingleMeshVboRenderer.getFadeAlpha();
                float tr = tmpTint[0], tg = tmpTint[1], tb = tmpTint[2];
                VertexConsumer consumer = bufferSource.getBuffer(fade < 0.99f ? RenderType.translucent() : RenderType.solid());
                var pose = poseStack.last();
                for (BakedQuad quad : quadsForIris) {
                    float shade = RenderHooks.quadShade(quad.getDirection());
                    RenderHooks.putBulkData(consumer, pose, quad,
                            shade * tr, shade * tg, shade * tb, fade, packedLight,
                            OverlayTexture.NO_OVERLAY, false);
                }
            }
            return;
        }

        long posKey = OcclusionCullingHelper.occlusionKeyForBlock(blockPos);
        int slot = slotAllocator.allocateSlot(posKey);
        if (slot < 0 || slot >= maxInstances) {
            OVERFLOW_ADD_COUNT.incrementAndGet();
            if (!overflowLogged) {
                overflowLogged = true;
                MainRegistry.LOGGER.warn(
                        "InstancedStaticPartRenderer overflow: slot {} >= maxInstances {} reached for tag={}, skipping extra instances until next flush",
                        slot, maxInstances, mdiTraceTag);
            }
            return;
        }
        ensureCpuInstanceCapacity(slot);
        if (instanceCount == 0) {
            overflowLogged = false;
            // NOTE: do NOT reset mdiRecordWriteHappened here. flush() zeroes
            // instanceCount every frame; writes made between frames via
            // putInstanceSlice/freeSlot/rebaseAnchor set the flag, and wiping it
            // on the next addInstance made canSubmitMdiClean() report a clean
            // buffer that was actually changed (stale MDI snapshot).
            var level = Minecraft.getInstance().level;
            batchSkyDarken = (level != null) ? level.getSkyDarken(1.0f) : -1f;
        }

        Matrix4f mat = poseStack.last().pose();

        fillInstanceCornerLight(blockEntity, packedLight, blockPos, mat, sharedCornerUV8, slot);

        instanceCullIndices[slot] = -1;
        if (instanceOcclusionKeys[slot] != posKey) {
            this.slotLayoutEpoch++;
            this.recordMutationEpoch++;
        }
        instanceOcclusionKeys[slot] = posKey;
        int baseFloat = slot * instanceDataSize;

        if (staticPose && slotPosQuatStable[slot] && recordMatchesBuffer(baseFloat, true, lightBbox)) {
            // Static pose is frame-invariant by definition and its record matched the buffer:
            // keep it verified so SHADOW-EPOCH calibration can use static machine bases!
            slotPosQuatVerified[slot] = true;
        } else {
            convertToWorldRecord(mat);
            boolean posQuatMatch = posQuatMatchesBuffer(baseFloat);
            slotPosQuatStable[slot] = posQuatMatch;
            slotPosQuatVerified[slot] = posQuatMatch;
            if (!recordMatchesBuffer(baseFloat, false, lightBbox)) {
                memPutInstanceRecordAtBaseFloat(baseFloat, lightBbox);
                mdiRecordWriteHappened = true;
            }
        }
        instanceCount = Math.min(maxInstances, Math.max(instanceCount, slotAllocator.getHighWaterMark()));
        int targetPos = Math.min(instanceDataSize * instanceCount, instanceBuffer.capacity());
        ((Buffer) instanceBuffer).position(targetPos);
    }

    /**
     * Compares the record's pos/quat prefix (3 pos floats + 2 packed quat words,
     * {@code posTmp/rotTmp} vs instanceBuffer at {@code baseFloatIndex}).
     * Position uses a tolerance (recEq - world-space ulp jitter on camera
     * rotation); the packed quat words are compared at record precision via raw
     * bit patterns - sub-quantization jitter rounds to the same snorm16 word.
     *
     * @return true if the posQuat prefix is unchanged since the last flush.
     */
    private boolean posQuatMatchesBuffer(int baseFloatIndex) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        if (!recEq(MemoryUtil.memGetFloat(a), posTmp.x, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 4), posTmp.y, POS_EPS)) return false;
        if (!recEq(MemoryUtil.memGetFloat(a + 8), posTmp.z, POS_EPS)) return false;
        if (Float.floatToRawIntBits(MemoryUtil.memGetFloat(a + (long) QUAT_FLOAT_OFFSET * 4L))
                != packQuatWord(rotTmp.x, rotTmp.y)) return false;
        return Float.floatToRawIntBits(
                MemoryUtil.memGetFloat(a + (long) QUAT_FLOAT_OFFSET * 4L + 4))
                == packQuatWord(rotTmp.z, rotTmp.w);
    }

    /**
     * Compares the new record ({@code posTmp/rotTmp}, light bbox, fade mode,
     * corner light, uvRect, tint, grad, animParams) with the contents of
     * {@code instanceBuffer} at slot offset {@code baseFloatIndex}. Records of
     * the previous flush stay in the buffer ({@code clear()} resets only the
     * position), so a full match means "the machine did not change since the
     * last flush" - the memPut can be skipped and this phase can stay a clean
     * submit ({@code mdiRecordWriteHappened} stays false). With
     * {@code skipPosQuat} (trusted static pose) the 5-word prefix is not read -
     * {@code posTmp/rotTmp} were not recomputed for this part.
     */
    private boolean recordMatchesBuffer(int baseFloatIndex, boolean skipPosQuat, float[] lightBbox) {
        long a = instanceBufferAddress + (long) baseFloatIndex * 4L;
        if (!skipPosQuat && !posQuatMatchesBuffer(baseFloatIndex)) return false;
        if (MemoryUtil.memGetFloat(a + (long) BBOX_MIN_FLOAT_OFFSET * 4L) != lightBbox[0]) return false;
        if (MemoryUtil.memGetFloat(a + (long) BBOX_MIN_FLOAT_OFFSET * 4L + 4) != lightBbox[1]) return false;
        if (MemoryUtil.memGetFloat(a + (long) BBOX_MIN_FLOAT_OFFSET * 4L + 8) != lightBbox[2]) return false;
        if (MemoryUtil.memGetFloat(a + (long) BBOX_SIZE_FLOAT_OFFSET * 4L) != lightBbox[3] - lightBbox[0]) return false;
        if (MemoryUtil.memGetFloat(a + (long) BBOX_SIZE_FLOAT_OFFSET * 4L + 4) != lightBbox[4] - lightBbox[1]) return false;
        if (MemoryUtil.memGetFloat(a + (long) BBOX_SIZE_FLOAT_OFFSET * 4L + 8) != lightBbox[5] - lightBbox[2]) return false;
        if (MemoryUtil.memGetFloat(a + (long) FADE_FLOAT_OFFSET * 4L) != tmpFadeMode) return false;
        long lightA = a + (long) LIGHT_FLOAT_OFFSET * 4L;
        for (int w = 0; w < 4; w++) {
            int b = w * 4;
            if (Float.floatToRawIntBits(MemoryUtil.memGetFloat(lightA + (long) w * 4L))
                    != packLightWord(tmpCornerUV[b], tmpCornerUV[b + 1], tmpCornerUV[b + 2], tmpCornerUV[b + 3])) {
                return false;
            }
        }
        long uvA = a + (long) UVRECT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < 4; i++) {
            if (MemoryUtil.memGetFloat(uvA + (long) i * 4L) != tmpUvRect[i]) return false;
        }
        long tintA = a + (long) TINT_FLOAT_OFFSET * 4L;
        for (int i = 0; i < 4; i++) {
            if (MemoryUtil.memGetFloat(tintA + (long) i * 4L) != tmpTint[i]) return false;
        }
        long gradA = a + (long) GRAD_FLOAT_OFFSET * 4L;
        for (int i = 0; i < 4; i++) {
            if (MemoryUtil.memGetFloat(gradA + (long) i * 4L) != tmpGrad[i]) return false;
        }
        long animA = a + (long) ANIM_FLOAT_OFFSET * 4L;
        for (int i = 0; i < 4; i++) {
            if (MemoryUtil.memGetFloat(animA + (long) i * 4L) != tmpAnimParams[i]) return false;
        }
        return true;
    }

    /** |a-b| &le; eps (NaN is not equal to anything - conservatively writes the record). */
    private static boolean recEq(float a, float b, float eps) {
        return Math.abs(a - b) <= eps;
    }

    /** Record world-coordinate tolerance (~1 mm - sub-pixel even up close). */
    private static final float POS_EPS = 1.0e-3f;

    /** Companion mesh for the global shadow batch ({@link IrisShadowBatchCollector}); built lazily. */
    @Nullable
    IrisCompanionMesh getOrBuildCompanionForShadowBatch() {
        return irisHelper.getOrBuildIrisCompanion();
    }

    /**
     * Instanced draw of shadow records: our ExtendedShader (vanilla-parity FSH - the
     * shadow FB needs no pack format), the parent VAO, span-diff of records into the instance
     * VBO, ONE {@code glDrawElementsInstanced} per part-renderer.
     * <p>
     * Records are LIGHT-space shadow-poseStack decompositions (exact pos/quat of
     * the Iris shadow poseStack's MV_shadow * T(bePos - camPos) * part - the
     * light rotation rides inside the record, the camera offset is folded into
     * the translation; see IrisShadowBatchCollector.appendRecord), so
     * ModelViewMat = T(-cam) folds the camera back out at draw time: the viewPos
     * equals the old camera-relative contract bit-for-bit (w - cam).
     * iris_ModelViewMat from RenderSystem must not be used (on Oculus 1.8
     * the stack in shadow may carry a shadow-MV - that was the "crawling shadows" of
     * the early variant). Clip = P_shadow * (record - cam), exactly the same contract
     * as the pack-program drawCompanion flush.
     * {@code records} - position at the end of writing; the method flips/clears it itself.
     */
    boolean drawShadowInstances(java.nio.FloatBuffer records, int count,
                                ShaderInstance shader, Matrix4f proj,
                                float camX, float camY, float camZ) {
        if (!initialized || !ensureOwnGlObjects() || instanceBuffer == null || count <= 0) {
            return false;
        }
        try (RenderStateGuard ignored = RenderStateGuard.snapshot()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();

            GlVaoSafety.bindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVboId);
            // Self-heal the VAO instance attribs (the phase-2 fading draw may have
            // re-pointed them at the fading VBO and thrown before restoring).
            pointInstanceAttribsTo(instanceVboId);
            FloatBuffer uploadBuf = records.duplicate();
            uploadBuf.position(0);
            uploadBuf.limit(count * instanceDataSize);
            uploadToInstanceVboSpanned(uploadBuf, 0, 0, count * instanceDataSize);
            vanillaHelper.enableVertexAttribsForDraw();

            IrisShadowBatchCollector.buildShadowModelView(SHADOW_WORLD_MV, camX, camY, camZ);
            vanillaHelper.useIrisProgram(shader, proj, SHADOW_WORLD_MV);
            SingleMeshVboRenderer.primeIrisInstancedSamplerMap(shader, Minecraft.getInstance());
            // Pack shadow distortion - must match the pack's sampler
            // (IrisShadowDistortion extracts the mode/constants from the pack source).
            // ALL uniforms are declared "float" - set(int) on a float uniform throws
            // NPE (Uniform.intValues == null, debug.log 0913 23:41, BSL).
            com.mojang.blaze3d.shaders.Uniform uMode = shader.getUniform("hbmShadowDistortionMode");
            if (uMode != null) { uMode.set((float) com.hbm_m.client.render.shader.IrisShadowDistortion.mode()); uMode.upload(); }
            com.mojang.blaze3d.shaders.Uniform uDist = shader.getUniform("hbmShadowDistortion");
            if (uDist != null) { uDist.set(com.hbm_m.client.render.shader.IrisShadowDistortion.distortion()); uDist.upload(); }
            com.mojang.blaze3d.shaders.Uniform uScale = shader.getUniform("hbmShadowDepthScale");
            if (uScale != null) { uScale.set(com.hbm_m.client.render.shader.IrisShadowDistortion.depthScale()); uScale.upload(); }

            InstancedGlCompat.glDrawElementsInstancedCompat(GL11.GL_TRIANGLES, indexCount,
                    GL11.GL_UNSIGNED_INT, 0, count);
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Iris shadow instanced draw failed", e);
            return false;
        }
        return true;
    }

    /**
     * T(-cam) for the shadow instanced draw: the records are light-space
     * shadow-poseStack decompositions with the camera offset already in the
     * translation (see {@link IrisShadowBatchCollector}); T(-cam) folds it out here.
     */
    private static final Matrix4f SHADOW_WORLD_MV = new Matrix4f();

    /**
     * Fills {@link #tmpCornerUV} for the instanced VBO and, when Iris flush needs it,
     * {@link #instanceLightUV} for the current slot.
     *
     * <p>When {@code sharedCornerUV8} is supplied (one 8-corner sample per machine per frame),
     * vanilla path skips {@link LightSampleCache#getOrSample} and {@code tmpLocalPose} work.
     */
    private void fillInstanceCornerLight(@Nullable BlockEntity blockEntity, int packedLight,
                                         BlockPos blockPos, Matrix4f worldPose,
                                         @Nullable float[] sharedCornerUV8, int slot) {
        boolean hasSharedCorners = sharedCornerUV8 != null && sharedCornerUV8.length >= 16;
        boolean needsIrisInstanceLight = ShaderCompatibilityDetector.canUseIrisExtendedShader()
                || ShaderCompatibilityDetector.isExternalShaderActive();

        if (needsIrisInstanceLight) {
            int sampleBase = slot * 2;
            LightSampleCache.getOrSample(blockEntity, packedLight, instanceLightUV, sampleBase);
        }

        if (hasSharedCorners) {
            System.arraycopy(sharedCornerUV8, 0, tmpCornerUV, 0, 16);
            return;
        }

        if (LightSampleCache.BASE_POSE_SET.get()) {
            tmpLocalPose.set(LightSampleCache.BASE_POSE.get()).invert().mul(worldPose);
        } else {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            // L^-1 (FrameViewState): strips R_cam AND the view bob - the local pose
            // is static while walking, light sample points stop jittering. (History:
            // an early extra .invert() here rotated sample points by R_cam^2 and the
            // light "crawled" over the machine on camera rotation.)
            tmpLocalPose.set(FrameViewState.poseToWorldRotation()).mul(worldPose);
            tmpLocalPose.m30(tmpLocalPose.m30() - (float) (blockPos.getX() - cam.x));
            tmpLocalPose.m31(tmpLocalPose.m31() - (float) (blockPos.getY() - cam.y));
            tmpLocalPose.m32(tmpLocalPose.m32() - (float) (blockPos.getZ() - cam.z));
        }

        long partHash = System.identityHashCode(this);
        LightSampleCache.getOrSample8(blockEntity, partHash, objBbox, blockPos, tmpLocalPose,
                packedLight, tmpCornerUV);
    }

    // ── Flush ──────────────────────────────────────────────────────────

    @Override
    public void flush() {
        flush(RenderSystem.getProjectionMatrix());
    }


    /**
     * Mandatory re-bind of atlas + lightmap after {@link ShaderInstance#apply()} and before glDraw*.
     * <p>
     * <b>REGRESSION STOP:</b> without this, instanced machines render white (Sampler2 reads unit 0 = atlas).
     * Delegates to {@link SingleMeshVboRenderer#bindBlockLitSamplerTextures}; do not duplicate the logic here.
     */
    static void bindBlockLitTexturesBeforeDraw(ShaderInstance shader) {
        SingleMeshVboRenderer.bindBlockLitSamplerTextures(shader);
    }

    /**
     * Called from {@link com.hbm_m.client.render.culling.InstancedRenderFrame#presentAfterBlockEntities}
     * in the same frame as addInstance - do not defer the flush to the end of the level.
     */
    @Override
    public void flush(Matrix4f projectionMatrix) {
        // Last phase's deferred fading was either already drawn by flushFading(),
        // or staled (a second AFTER_BLOCK_ENTITIES pass per frame) - reset.
        deferredFadingCount = 0;
        // Emergency fallback owns every draw: discard any records that may have
        // accumulated before the flag flipped (e.g. toggled live mid-session) so
        // no stale instanced copy can reach a flush next to the immediate output.
        if (ClientRenderFlags.forceVanillaImmediate()) {
            instanceCount = 0;
            if (instanceBuffer != null) {
                instanceBuffer.clear();
            }
            return;
        }
        if (instanceCount == 0 && slotAllocator.getHighWaterMark() > 0) {
            instanceCount = Math.min(maxInstances, slotAllocator.getHighWaterMark());
            if (instanceBuffer != null) {
                int targetPos = Math.min(instanceDataSize * instanceCount, instanceBuffer.capacity());
                ((Buffer) instanceBuffer).position(targetPos);
            }
        }
        if (instanceCount == 0) {
            com.hbm_m.client.render.retained.RetainedInstanceGraph rig =
                    com.hbm_m.client.render.retained.RetainedInstanceGraph.get();
            if (rig != null && !rig.hasActiveInstances(this)) {
                return;
            }
            MdiBatchCoordinator coord = MdiBatchCoordinator.active();
            if (coord != null && coord.tryAssertRetainedPresence(this)) {
                noteMdiDispatched();
                return;
            }
            return;
        }

        // NOTE: no own-GL requirement here - the coordinator submit inside
        // flushBatchVanilla/flushBatchIris is Tier 1 and runs WITHOUT this
        // renderer's VAO/VBO/EBO. The draw helpers build the own GL objects on
        // first need (ensureOwnGlObjects) only when they actually fall through
        // to the Tier-2 direct draw.
        if (!initialized || instanceBuffer == null) {
            instanceCount = 0;
            if (instanceBuffer != null) {
                instanceBuffer.clear();
            }
            return;
        }

        // SHADOW-EPOCH: publish the flushed world records as the prebake snapshot for
        // the next Iris shadow pass (collector copies them, so later buffer mutations
        // are harmless). Gated on an active shaderpack + shadow batching: without Iris
        // nothing is published and the shadow pass keeps the pure fallback path. The
        // vanilla/MDI partition below never runs while this gate is open (it is an
        // external-shader-only path), so record slots stay in publish order.
        if (ShaderCompatibilityDetector.isExternalShaderActive()
                && IrisShadowBatchCollector.isBatchingEnabled()) {
            IrisShadowBatchCollector.publishPrebake(this, instanceBuffer, instanceCount,
                    instanceOcclusionKeys, slotPosQuatStable, slotPosQuatVerified);
        }
        // The verified flags mean "compare-verified THIS frame" - the snapshot above
        // has consumed them (collector copies), so drop stale flags before the next
        // frame; only addInstance's posQuatMatchesBuffer compares may set them again.
        java.util.Arrays.fill(slotPosQuatVerified, 0, instanceCount, false);

        boolean useIrisFlush = ShaderCompatibilityDetector.canUseIrisExtendedShader();
        if (useIrisFlush) {
            irisHelper.flushBatchIris(projectionMatrix);
        } else if (ShaderCompatibilityDetector.isExternalShaderActive()) {
            // Iris/Oculus owns the GL program; vanilla instanced shaders would draw with
            // "No active program". Callers must fall back to per-BE VBO / putBulkData.
            if (!warnedInstancedShaderNullFlush) {
                warnedInstancedShaderNullFlush = true;
                MainRegistry.LOGGER.warn("InstancedStaticPartRenderer: Dropped {} instances because an external shader is active but ExtendedShader is unavailable for tag={}", instanceCount, mdiTraceTag);
            }
        } else {
            vanillaHelper.flushBatchVanilla(projectionMatrix);
        }

        instanceCount = 0;
        instanceBuffer.clear();
        overflowLogged = false;
    }

    /**
     * Phase 2 (called from InstancedRenderFrame AFTER the MDI dispatch): picks up the
     * fading instances deferred by the direct path in {@link #deferFading}.
     * Unconditionally clears the deferred state - skipping the call cannot
     * "leak" into the next frame. Under Iris there is never anything deferred
     * (flushBatchVanilla is not called there) - a natural no-op.
     */
    public void flushFading(Matrix4f projectionMatrix) {
        int count = deferredFadingCount;
        deferredFadingCount = 0;
        if (count <= 0) return;
        if (!initialized || !ensureOwnGlObjects() || fadingSnapshot == null) return;
        if (ShaderCompatibilityDetector.isExternalShaderActive()) return;
        vanillaHelper.flushFadingVanilla(projectionMatrix, count);
    }

    /**
     * Copies the fading records [firstRecord, instanceCount) of the main buffer into the
     * phase-2 snapshot. Called from {@code flushBatchVanilla} after the partition;
     * instanceBuffer is at position=0 at that moment (after flip), with content already
     * reordered [opaque | fading].
     */
    void deferFading(int firstRecord, int count) {
        deferredFadingCount = count;
        if (count <= 0) return;
        // Lazy snapshot: most renderers never fade (and under Iris nothing is ever
        // deferred), so the buffer is only allocated on the first fading frame.
        ensureFadingSnapshot(count);
        // Position-independent base: callers keep instanceBuffer at position=0
        // (post-flip) when reaching here, but memAddress() would silently shift
        // by position*4 if that invariant ever broke - use the stored base.
        long srcAddr = this.instanceBufferAddress
                + (long) firstRecord * instanceDataSize * 4L;
        MemoryUtil.memCopy(srcAddr, MemoryUtil.memAddress(fadingSnapshot),
                (long) count * instanceDataSize * 4L);
    }

    /**
     * Sort key for direct-path fading windows: the distSq of the FARTHEST fading instance
     * (record 0 of the snapshot - back-to-front within the renderer). -1 = no fade.
     * Used by MachineSpec.flushFading for the global back-to-front order of windows.
     */
    public float fadingSortKeyDistSq() {
        if (deferredFadingCount <= 0 || fadingSnapshot == null) return -1f;
        float dx = fadingSnapshot.get(0) - FrameViewState.relCamX();
        float dy = fadingSnapshot.get(1) - FrameViewState.relCamY();
        float dz = fadingSnapshot.get(2) - FrameViewState.relCamZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Reorders the instance-buffer records into [opaque in original order |
     * fading back-to-front by distance-to-camera] and returns the number of opaque records.
     * Buffer: position 0, valid records [0, instanceCount*stride).
     * <p>
     * Shared phase-order G mechanism for MDI snapshots
     * ({@code MdiBatchCoordinator.partitionOpaqueFirst}) and the direct path
     * ({@code flushBatchVanilla}): "opaque before fading" must hold
     * identically across ALL render paths.
     *
     * @param parallelKeys optional parallel array of roster keys
     *                     (machine per slot): reordered in sync with the records so
     *                     the "key <-> record in slot" correspondence survives
     *                     the partition (fast-path dirty-skip). null - leave untouched.
     */
    /** Partition scratch: a growing persistent buffer instead of memAllocFloat/Free on every dirty frame. */
    private static FloatBuffer partitionScratch = null;
    private static long[] partitionScratchKeys = null;
    private static int[] partitionFadeIdx = null;
    private static float[] partitionFadeDistSq = null;

    static {
        // Session-end release of the scratch high-water, via the central invalidation
        // point (self-registration keeps this cache owner self-contained; if this class
        // never loads, no scratch exists and no hook is needed).
        RenderCacheManager.register(reason -> freePartitionScratch());
        // Invalidation (reload / disconnect) is a guaranteed render-thread point:
        // flush GL names the Cleaner parked for renderers dropped without cleanup().
        RenderCacheManager.register(reason -> drainPendingGlDeletions());
    }

    /** Frees the persistent partition scratch (session end / resource reload; re-grows on demand). */
    private static void freePartitionScratch() {
        if (partitionScratch != null) {
            MemoryUtil.memFree(partitionScratch);
            partitionScratch = null;
        }
        partitionScratchKeys = null;
        partitionFadeIdx = null;
        partitionFadeDistSq = null;
    }

    private static FloatBuffer partitionScratch(int floats) {
        FloatBuffer buf = partitionScratch;
        if (buf == null || buf.capacity() < floats) {
            if (buf != null) {
                MemoryUtil.memFree(buf);
            }
            buf = MemoryUtil.memAllocFloat(Math.max(floats, 4096));
            partitionScratch = buf;
        }
        return buf;
    }

    static int partitionInstancesOpaqueFirst(FloatBuffer buf, int instanceCount, int floatsPerInstance, int fadeOffset,
                                             long[] parallelKeys, int[] parallelCull,
                                             boolean[] parallelPosQuatStable, boolean[] parallelPosQuatVerified) {
        int n = instanceCount;
        if (buf == null || n <= 0) return 0;

        // Fading predicate: GPU-ramp parity. The record stores a static fade MODE
        // (not a per-frame alpha); membership is the CPU mirror of the vertex
        // shader ramp (RenderDistanceHelper.evalInstanceAlpha) on the instance
        // origin distance to the camera (both anchor-relative).
        float camX = FrameViewState.relCamX();
        float camY = FrameViewState.relCamY();
        float camZ = FrameViewState.relCamZ();
        int fadeTotal = 0;
        for (int i = 0; i < n; i++) {
            if (instanceAlpha(buf, i, floatsPerInstance, fadeOffset, camX, camY, camZ) < 1.0f) {
                fadeTotal++;
            }
        }
        if (fadeTotal == 0) return n;

        FloatBuffer tmp = partitionScratch(n * floatsPerInstance);
        long[] tmpKeys = parallelKeys != null
                ? (partitionScratchKeys != null && partitionScratchKeys.length >= n
                        ? partitionScratchKeys
                        : (partitionScratchKeys = new long[n]))
                : null;
        int[] tmpCull = parallelCull != null ? new int[n] : null;
        boolean[] tmpStable = parallelPosQuatStable != null ? new boolean[n] : null;
        boolean[] tmpVerified = parallelPosQuatVerified != null ? new boolean[n] : null;
        int[] fadeIdx = partitionFadeIdx != null && partitionFadeIdx.length >= fadeTotal
                ? partitionFadeIdx : (partitionFadeIdx = new int[fadeTotal]);
        float[] fadeDistSq = partitionFadeDistSq != null && partitionFadeDistSq.length >= fadeTotal
                ? partitionFadeDistSq : (partitionFadeDistSq = new float[fadeTotal]);
        try {
            int opaqueWrote = 0;
            int k = 0;
            for (int i = 0; i < n; i++) {
                int recBase = i * floatsPerInstance;
                float x = buf.get(recBase);
                float y = buf.get(recBase + 1);
                float z = buf.get(recBase + 2);
                float dx = x - camX, dy = y - camY, dz = z - camZ;
                if (RenderDistanceHelper.evalInstanceAlpha(
                        dx * (double) dx + dy * (double) dy + dz * (double) dz,
                        buf.get(recBase + fadeOffset)) < 1.0f) {
                    fadeIdx[k] = i;
                    fadeDistSq[k] = dx * dx + dy * dy + dz * dz;
                    k++;
                } else {
                    copyInstanceRecord(buf, recBase, tmp, opaqueWrote * floatsPerInstance, floatsPerInstance);
                    if (tmpKeys != null) {
                        tmpKeys[opaqueWrote] = parallelKeys[i];
                    }
                    if (tmpCull != null) tmpCull[opaqueWrote] = parallelCull[i];
                    if (tmpStable != null) tmpStable[opaqueWrote] = parallelPosQuatStable[i];
                    if (tmpVerified != null) tmpVerified[opaqueWrote] = parallelPosQuatVerified[i];
                    opaqueWrote++;
                }
            }
            // insertion sort by descending distSq (back-to-front); fading instances are usually few
            for (int a = 1; a < fadeTotal; a++) {
                int idx = fadeIdx[a];
                float d = fadeDistSq[a];
                int b = a - 1;
                while (b >= 0 && fadeDistSq[b] < d) {
                    fadeIdx[b + 1] = fadeIdx[b];
                    fadeDistSq[b + 1] = fadeDistSq[b];
                    b--;
                }
                fadeIdx[b + 1] = idx;
                fadeDistSq[b + 1] = d;
            }
            int dst = opaqueWrote;
            for (int a = 0; a < fadeTotal; a++, dst++) {
                copyInstanceRecord(buf, fadeIdx[a] * floatsPerInstance, tmp, dst * floatsPerInstance, floatsPerInstance);
                if (tmpKeys != null) {
                    tmpKeys[dst] = parallelKeys[fadeIdx[a]];
                }
                if (tmpCull != null) tmpCull[dst] = parallelCull[fadeIdx[a]];
                if (tmpStable != null) tmpStable[dst] = parallelPosQuatStable[fadeIdx[a]];
                if (tmpVerified != null) tmpVerified[dst] = parallelPosQuatVerified[fadeIdx[a]];
            }
            MemoryUtil.memCopy(MemoryUtil.memAddress(tmp), MemoryUtil.memAddress(buf),
                    (long) n * floatsPerInstance * 4L);
            if (tmpKeys != null) {
                System.arraycopy(tmpKeys, 0, parallelKeys, 0, n);
            }
            if (tmpCull != null) {
                System.arraycopy(tmpCull, 0, parallelCull, 0, n);
            }
            if (tmpStable != null) {
                System.arraycopy(tmpStable, 0, parallelPosQuatStable, 0, n);
            }
            if (tmpVerified != null) {
                System.arraycopy(tmpVerified, 0, parallelPosQuatVerified, 0, n);
            }
            return n - fadeTotal;
        } finally {
            // Scratch persists within a session; freed at session end (freePartitionScratch).
        }
    }

    private static void copyInstanceRecord(FloatBuffer src, int srcRecBase, FloatBuffer dst, int dstRecBase, int floats) {
        long srcAddr = MemoryUtil.memAddress(src) + (long) srcRecBase * 4L;
        long dstAddr = MemoryUtil.memAddress(dst) + (long) dstRecBase * 4L;
        MemoryUtil.memCopy(srcAddr, dstAddr, (long) floats * 4L);
    }

    /**
     * R4-2.3: direct-path partition that keeps the renderer's slot-indexed state
     * consistent with the reordered buffer. Partitioning used to permute the
     * records + occlusion keys only, so a subsequent putInstanceRecord/Slice -
     * which writes at {@code slotAllocator.getSlot(posKey)} - landed in another
     * machine's record. After the permutation every keyed slot is re-pointed at
     * its record's new position (and the cull/posQuat parallel arrays are
     * permuted in sync with the records).
     */
    int partitionAndRemapAllocator() {
        int opaque = partitionInstancesOpaqueFirst(instanceBuffer, instanceCount, instanceDataSize,
                instanceFadeFloatOffset, instanceOcclusionKeys, instanceCullIndices,
                slotPosQuatStable, slotPosQuatVerified);
        if (opaque < instanceCount) {
            for (int dst = 0; dst < instanceCount; dst++) {
                long key = instanceOcclusionKeys[dst];
                if (key != Long.MIN_VALUE) {
                    slotAllocator.remapKeyedSlot(key, dst);
                }
            }
        }
        return opaque;
    }

    /**
     * CPU mirror of the vertex-shader fade ramp for one instance: alpha in [0,1]
     * from the stored fade MODE and the instance-origin distance to the camera
     * (anchor-relative record pos vs {@link FrameViewState#relCamX()}).
     */
    static float instanceAlpha(FloatBuffer buf, int i, int floatsPerInstance, int fadeOffset,
                               float camX, float camY, float camZ) {
        int recBase = i * floatsPerInstance;
        float dx = buf.get(recBase) - camX;
        float dy = buf.get(recBase + 1) - camY;
        float dz = buf.get(recBase + 2) - camZ;
        double distSq = dx * (double) dx + dy * (double) dy + dz * (double) dz;
        return RenderDistanceHelper.evalInstanceAlpha(distSq, buf.get(recBase + fadeOffset));
    }

    // ── IrisCompanionMeshRenderer interface ────────────────────────────

    @Override
    public void flushBatchIris(Matrix4f projectionMatrix) {
        irisHelper.flushBatchIris(projectionMatrix);
    }

    @Override
    public boolean drawSingleWithIrisExtended(PoseStack poseStack, int packedLight,
                                              BlockPos blockPos, @Nullable BlockEntity blockEntity) {
        return irisHelper.drawSingleWithIrisExtended(poseStack, packedLight, blockPos, blockEntity);
    }

    // ── State queries ──────────────────────────────────────────────────

    @Override
    public boolean isInitialized() {
        // "The constructor succeeded - the renderer can take the instanced
        // pipeline (MDI submit, roster asserts, record writes)". NOT "own GL
        // exists": with lazy own-GL a Tier-1 renderer has no VAO/VBO/EBO until a
        // Tier-2 path calls ensureOwnGlObjects(). Every own-GL draw site gates on
        // ensureOwnGlObjects() itself.
        return initialized;
    }

    @Override
    public int getInstanceCount() {
        return instanceCount;
    }

    @Nullable
    private com.hbm_m.client.render.machine.MachineSpec<?> ownerSpec;
    @Nullable
    private net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType;

    public void setOwnerSpec(@Nullable com.hbm_m.client.render.machine.MachineSpec<?> spec) {
        this.ownerSpec = spec;
        if (spec != null) {
            this.blockEntityType = spec.type();
        }
    }

    @Nullable
    public com.hbm_m.client.render.machine.MachineSpec<?> getOwnerSpec() {
        return ownerSpec;
    }

    public void setBlockEntityType(@Nullable net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        this.blockEntityType = type;
    }

    @Nullable
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getBlockEntityType() {
        return blockEntityType;
    }

    public void setMdiTraceTag(@Nullable String tag) {
        this.mdiTraceTag = tag;
        if (this.irisHelper != null && tag != null) {
            this.irisHelper.onTraceTagUpdated(tag);
        }
    }

    @Nullable
    public String getMdiTraceTag() {
        return mdiTraceTag;
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    @Override
    public void cleanup() {
        super.cleanup();

        synchronized (ACTIVE_RENDERERS) {
            ACTIVE_RENDERERS.remove(this);
        }

        // The coordinator's retained record for this renderer is no longer valid:
        // evictRendererIfRegistered will tear down the atlas slot, and the flags forbid submitClean.
        this.mdiBufferSynced = false;
        this.mdiRecordWriteHappened = false;

        final long vramToRelease = this.vramBytes;
        this.vramBytes = 0L;
        if (vramToRelease != 0L) {
            NucleusDebug.removeRendererVram(vramToRelease);
        }

        final int instanceVboToDelete = this.instanceVboId;
        final int fadingVboToDelete = this.fadingInstanceVboId;
        this.fadingInstanceVboId = -1;
        final Cleaner.Cleanable bufferCleanable = this.instanceBufferCleanable;
        final FloatBuffer shadowToFree = this.vboShadow;
        this.vboShadow = null;
        this.vboShadowFloats = 0;
        this.vboShadowValid = false;
        this.vboShadowValidFloats = 0;
        final FloatBuffer fadingShadowToFree = this.fadingVboShadow;
        this.fadingVboShadow = null;
        this.fadingVboShadowFloats = 0;
        this.fadingVboShadowValid = false;
        this.fadingVboShadowValidFloats = 0;
        final java.nio.ByteBuffer atlasVbToFree = this.atlasVertexBytesRetained;
        final java.nio.IntBuffer atlasIbToFree = this.atlasIndicesRetained;
        this.atlasVertexBytesRetained = null;
        this.atlasIndicesRetained = null;
        this.atlasIndexCountRetained = 0;

        this.instanceVboId = -1;
        this.instanceBuffer = null;
        this.fadingSnapshot = null;
        this.instanceBufferCleanable = null;
        // R4-2.1: consume ALL names in the GL Cleaner snapshot and unregister the
        // Cleanable - the action then enqueues nothing live. super.cleanup() above
        // already scheduled glDeleteVertexArrays/glDeleteBuffers for the
        // VAO/VBO/EBO names; leaving them in the snapshot made the GC-driven
        // clean() park them in PENDING_GL_DELETIONS, and drainPendingGlDeletions
        // deleted them a SECOND time after the driver had potentially re-issued
        // the same numeric IDs to freshly created objects (native crash in
        // nvoglv64.dll on the next MDI draw). The queued path must only fire for
        // renderers dropped WITHOUT an explicit cleanup().
        final GlReleaseState glStateToConsume = this.glReleaseState;
        final Cleaner.Cleanable glCleanableToClean = this.glNamesCleanable;
        this.glReleaseState = null;
        this.glNamesCleanable = null;
        if (glStateToConsume != null) {
            glStateToConsume.vaoId = 0;
            glStateToConsume.vboId = 0;
            glStateToConsume.eboId = 0;
            glStateToConsume.instanceVboId = 0;
            glStateToConsume.fadingInstanceVboId = 0;
            glStateToConsume.vramBytes = 0L;
        }
        if (glCleanableToClean != null) {
            try {
                glCleanableToClean.clean();
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("Failed to clean GL-names Cleaner action", t);
            }
        }
        vanillaHelper.invalidateShaderCache();

        irisHelper.cleanup();

        final InstancedStaticPartRenderer mdiEvictTarget = this;
        // Per-resource isolation (each step its own guard, Throwable so an Error
        // cannot skip the later frees either): an exception in one step used to
        // leak the instance VBO GL name and all three native buffers.
        // Run synchronously when already on the render thread: recordRenderCall
        // ALWAYS queues (both 1.20.1 and 1.21.1) and replays only at flipFrame, so
        // a cleanup during RESOURCE_RELOAD would park the GL deletes until end of
        // frame - after the driver re-issued the freed numeric IDs to the freshly
        // created MdiGeometryAtlas, whose VAO the replayed delete then killed.
        Runnable teardown = () -> {
            // Per-resource isolation (each step its own guard, Throwable so an Error
            // cannot skip the later frees either): an exception in one step used to
            // leak the instance VBO GL name and all three native buffers.
            drainPendingGlDeletions();
            tryCleanupStep("evictRendererIfRegistered",
                    () -> com.hbm_m.client.render.MdiGeometryAtlas.evictRendererIfRegistered(mdiEvictTarget));
            tryCleanupStep("slotAllocator.destroy()", () -> slotAllocator.destroy());
            tryCleanupStep("onRendererDestroyed",
                    () -> com.hbm_m.client.render.IrisShadowBatchCollector.onRendererDestroyed(mdiEvictTarget));
            tryCleanupStep("glDeleteBuffers(instanceVbo)", () -> {
                if (instanceVboToDelete != -1) {
                    GL15.glDeleteBuffers(instanceVboToDelete);
                }
            });
            tryCleanupStep("glDeleteBuffers(fadingInstanceVbo)", () -> {
                if (fadingVboToDelete != -1) {
                    GL15.glDeleteBuffers(fadingVboToDelete);
                }
            });
            tryCleanupStep("bufferCleanable.clean", () -> {
                if (bufferCleanable != null) {
                    bufferCleanable.clean();
                }
            });
            tryCleanupStep("memFree(vboShadow)", () -> {
                if (shadowToFree != null) {
                    MemoryUtil.memFree(shadowToFree);
                }
            });
            tryCleanupStep("memFree(fadingVboShadow)", () -> {
                if (fadingShadowToFree != null) {
                    MemoryUtil.memFree(fadingShadowToFree);
                }
            });
            tryCleanupStep("memFree(atlasVertexBytesRetained)", () -> {
                if (atlasVbToFree != null) {
                    MemoryUtil.memFree(atlasVbToFree);
                }
            });
            tryCleanupStep("memFree(atlasIndicesRetained)", () -> {
                if (atlasIbToFree != null) {
                    MemoryUtil.memFree(atlasIbToFree);
                }
            });
        };
        if (RenderSystem.isOnRenderThread()) {
            teardown.run();
        } else {
            RenderSystem.recordRenderCall(teardown::run);
        }
    }
}
