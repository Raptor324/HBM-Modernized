package com.hbm_m.client.render;


//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Lazy-built companion VBO+VAO that holds geometry in a vertex format compatible
 * with the {@code ExtendedShader} returned by Iris/Oculus for {@code ShaderKey.BLOCK_ENTITY}.
 * <p>
 * The data is produced by a {@link BufferBuilder} starting from
 * {@link DefaultVertexFormat#NEW_ENTITY}; when Iris is loaded its mixin extends
 * the format on the fly to {@code IrisVertexFormats.ENTITY} so that fields like
 * {@code midTexCoord}, {@code tangent}, {@code entityId}, {@code blockEntityId}
 * and {@code uv_mid_block} are populated automatically.
 * <p>
 * The companion mesh is built once per part on first request from the render
 * thread; if the build fails (e.g. Iris not loaded, build off-thread) the mesh
 * is marked failed and the calling code is expected to fall back to vanilla
 * paths.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class IrisCompanionMesh implements IrisCompanionMeshResource {

    private final List<BakedQuad> quads;
    private int vaoId = -1;
    private int vboId = -1;
    private int eboId = -1;
    private int indexCount = 0;
    private int vertexCount = 0;
    /**
     * GL attribute location of the per-vertex lightmap (UV2) within this VAO.
     * Set during {@link #ensureBuilt()}; -1 when the format does not contain a
     * UV2 element.
     * <p>
     * By default the array for this attribute is <b>disabled</b> in the VAO so
     * callers can supply a per-draw constant via {@code glVertexAttribI2i(uv2Location, ...)}.
     * Callers that need a smooth per-vertex lighting gradient across the mesh
     * can instead flip to <i>per-vertex</i> mode via
     * {@link #activatePerVertexLightmap()} — that path binds an auxiliary VBO
     * ({@link #lightmapVboId}) to this attribute, whose contents are populated
     * per-instance by {@link #writeInstanceLightmap(int, float[])} using
     * precomputed trilinear weights ({@link #perVertexCornerWeights}).
     */
    private int uv2Location = -1;
    private boolean built = false;
    private boolean failed = false;

    // ------------------------------------------------------------------------
    // Per-vertex trilinear lightmap path (used under Iris / Oculus).
    // ------------------------------------------------------------------------

    /**
     * Object-space AABB of the mesh, computed at build time from the raw quad
     * vertex positions. Used as the trilinear interpolation domain for
     * {@link #perVertexCornerWeights}.
     * <p>
     * Laid out as {@code [minX, minY, minZ, maxX, maxY, maxZ]}.
     */
    private final float[] objBbox = new float[6];

    // Переиспользуемый массив для записи. Размер будем подгонять при необходимости.
    private short[] lightmapTempArray = new short[0];

    /**
     * Per-vertex trilinear weights relative to the 8 corners of {@link #objBbox}.
     * Laid out as {@code [vertexCount * 8]}; for vertex {@code v}, indices
     * {@code v*8..v*8+7} are the weight contributions of corners 0..7, where
     * corner index encodes {@code bit 0 = x, bit 1 = y, bit 2 = z} with the
     * bit set meaning "take the max side". This matches
     * {@code LightSampleCache.sample8} and the non-instanced shader path.
     * <p>
     * Each vertex's 8 weights sum to 1, so the per-vertex UV2 computed as
     * {@code sum(weight[c] * cornerUV[c], c=0..7)} is a proper trilinear
     * interpolation at the vertex position. Precomputed once at build time;
     * per frame we only pay the 16 fused mul-adds per vertex per instance to
     * combine them with the instance's 8 corner UV2 samples.
     * <p>
     * {@code null} when the mesh has fewer than 4 vertices (nothing to weight)
     * or if {@link #ensureBuilt} has not run yet.
     */
    private float[] perVertexCornerWeights;
    /**
     * Optional per-vertex weights for a 2x4x2 light-probe lattice (16 probes).
     * Laid out as {@code [vertexCount * 16]} and used when the caller provides
     * a 32-float probe array (see {@link #writeInstanceLightmap(int, float[])}).
     */
    private float[] perVertexSlicedWeights;

    /**
     * GL buffer holding per-instance per-vertex UV2 values. Laid out as
     * {@code instanceSlots[maxInstanceSlots] × vertexCount × 2 × sizeof(USHORT)}
     * = {@code 4 bytes} per vertex per instance.
     * <p>
     * Bound to {@link #uv2Location} in the VAO after the first
     * {@link #activatePerVertexLightmap()} call. Per-draw the caller rebinds
     * the UV2 attribute pointer with a new byte offset ({@code slotIndex *
     * vertexCount * 4}) to select which instance's UV2 data is read.
     * <p>
     * Sized dynamically by {@link #ensureLightmapCapacity(int)}; {@code -1}
     * when not yet allocated.
     */
    private int lightmapVboId = -1;

    /**
     * Native-order CPU scratch for {@link #lightmapVboId}; we write USHORT
     * pairs into it via {@link #writeInstanceLightmap} and flush to the GPU
     * with a single {@code glBufferSubData} call per frame to minimize driver
     * overhead. Sized to match {@link #lightmapVboId}.
     */
    private ByteBuffer lightmapCpuScratch;
    /** Persistent-mapped view of {@link #lightmapVboId} when available (GL 4.4+). */
    private ByteBuffer lightmapMapped;
    /** Short view of either {@link #lightmapMapped} or {@link #lightmapCpuScratch}. */
    private ShortBuffer lightmapShortView;
    /** True when {@link #lightmapMapped} is active and coherent. */
    private boolean lightmapPersistentMapped = false;
    /** True when we wrote into the persistent mapped buffer this frame and need a barrier. */
    private boolean lightmapPersistentDirty = false;

    // Optional staging upload path (Embeddium-style): write into a persistently-mapped staging buffer,
    // then glCopyBufferSubData into lightmapVboId. Guarded by a fence so we never overwrite the
    // staging source region before the driver has consumed it. If the fence hasn't signaled,
    // we transparently fall back to the classic CPU scratch + glBufferSubData path.
    private int lightmapStagingVboId = -1;
    private ByteBuffer lightmapStagingMapped;
    private ShortBuffer lightmapStagingShortView;
    private long lightmapStagingFence = 0L; // GLsync handle stored as long by LWJGL
    private boolean lightmapStagingAvailable = false;

    /**
     * Number of instance slots the current {@link #lightmapVboId} /
     * {@link #lightmapCpuScratch} can hold. Grown (never shrunk) by
     * {@link #ensureLightmapCapacity(int)} as the peak instance count per
     * frame rises.
     */
    private int lightmapInstanceCapacity = 0;

    /**
     * Whether the VAO's UV2 attribute is currently bound to
     * {@link #lightmapVboId} (per-vertex mode) or disabled (per-draw constant
     * mode — the legacy {@code glVertexAttribI2i} path). Toggled by
     * {@link #activatePerVertexLightmap()} and {@link #restoreConstantLightmap()}.
     */
    private boolean perVertexLightmapActive = false;

    /**
     * Current byte offset into {@link #lightmapVboId} used by the VAO's UV2
     * attribute pointer. Cached so {@link #bindLightmapForInstance(int)} can
     * elide redundant {@code glVertexAttribIPointer} calls when adjacent
     * draws use the same instance slot (shouldn't happen in practice, but the
     * check is free and insures against bugs).
     */
    private int lightmapCurrentSlot = -1;

    // ------------------------------------------------------------------------
    // Slot allocator for per-vertex lightmaps (caches identical cornerUV16).
    // ------------------------------------------------------------------------

    /** Hash -> slot index mapping for recently used per-vertex lightmaps. */
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap lightmapKeyToSlot
            = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    /** Reverse mapping slot -> hash key (0 when slot unused). */
    private long[] lightmapSlotKey = new long[0];
    /** Next slot to evict when capacity is full. */
    private int lightmapEvictCursor = 0;
    /** Tracks dirty slot range for fallback upload path. */
    private int lightmapDirtyMinSlot = Integer.MAX_VALUE;
    private int lightmapDirtyMaxSlot = -1;

    /**
     * Cached actual {@link VertexFormat} the buffer was built with - usually
     * {@code IrisVertexFormats.ENTITY} when Iris is loaded, or plain
     * {@link DefaultVertexFormat#NEW_ENTITY} otherwise. Held so
     * {@link #prepareForShader(int)} can look up per-element offsets and bind
     * VBO data at linker-resolved locations.
     */
    private VertexFormat actualFormat;
    /**
     * Byte offset within one vertex of each named element in {@link #actualFormat},
     * keyed by the element's name in the format's element-mapping (e.g.
     * {@code "iris_Entity"}, {@code "mc_midTexCoord"}, {@code "at_tangent"}).
     */
    private final Map<String, Integer> elementOffsets = new HashMap<>();
    /**
     * Reverse lookup from element name to {@link VertexFormatElement} so
     * {@link #prepareForShader(int)} can read the element's GL type and
     * component count without re-iterating the format.
     */
    private final Map<String, VertexFormatElement> elementByName = new HashMap<>();

    /**
     * Small ring buffer of recently-bound shader program IDs. {@link #prepareForShader(int)}
     * skips its (relatively expensive) work whenever the requested program is
     * present in this set - the VAO retains the attribute pointers we set the
     * first time around.
     * <p>
     * <b>Why a ring buffer instead of a single int.</b> When Iris is enabled with
     * shadow casting, every render frame walks our companion mesh through TWO
     * different programs: the {@code SHADOW_*} variant during the shadow pass and
     * the {@code BLOCK_ENTITY} variant during the main pass. A single-slot cache
     * would miss on every other call (shadow → main → shadow → main) and re-do
     * the GLSL attribute resolution + VBO pointer rebinds each time, which the
     * profiler showed accounting for ~5.92% of frame time. A 4-slot ring buffer
     * gives us room for shadow + main + a couple of extras (HAND, ENTITIES_CUTOUT
     * fallbacks) without ever evicting the two we hit every frame.
     * <p>
     * On F3+T the Iris pipeline is rebuilt and {@code ExtendedShader} is given
     * fresh program IDs, which invalidates these slots automatically; on the next
     * draw we re-resolve the linker locations against the new program.
     */
    private static final int CACHED_PROGRAM_SLOTS = 4;
    private final int[] cachedProgramIds = new int[CACHED_PROGRAM_SLOTS];
    private int nextCacheSlot = 0;
    /**
     * Pipeline generation that {@link #cachedProgramIds} was populated under.
     * GL drivers recycle deleted program IDs on pipeline rebuild, so a cached
     * integer ID by itself is not a safe key - after an Iris pack swap or
     * settings-apply the new program can reuse an ID we previously linked,
     * and our ring-buffer hit wrongly skips {@link #bindIrisAttribute}
     * re-resolving against the new program's linker layout. We pair each ID
     * with the {@link com.hbm_m.client.render.shader.IrisExtendedShaderAccess#getPipelineGeneration()
     * pipeline generation}; when the generation moves the entire ring buffer
     * is considered stale and gets wiped.
     */
    private long cachedProgramPipelineGeneration = -1L;

    {
        for (int i = 0; i < CACHED_PROGRAM_SLOTS; i++) cachedProgramIds[i] = -1;
    }

    public IrisCompanionMesh(List<BakedQuad> quads) {
        this.quads = quads;
    }

    /**
     * Attempts to build the companion mesh. Must be called on the render thread.
     * Returns {@code true} on success, {@code false} if a previous build failed
     * or this build attempt failed.
     */
    public boolean ensureBuilt() {
        if (built) return true;
        if (failed) return false;
        if (quads == null || quads.isEmpty()) {
            failed = true;
            return false;
        }
        if (!RenderSystem.isOnRenderThread()) {
            return false;
        }

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        try {
            // First pass: compute the mesh's object-space AABB so we can bake
            // per-vertex trilinear weights relative to it. Done before feeding
            // quads into the BufferBuilder because BufferBuilder.putBulkData
            // hides the raw vertex positions behind format conversion; the
            // bbox must be derived from the unmodified BakedQuad data.
            computeObjBboxAndWeights();

            BufferBuilder builder = new BufferBuilder(quads.size() * 256);
            // Begin with NEW_ENTITY; Iris mixin extends to IrisVertexFormats.ENTITY when loaded.
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

            PoseStack.Pose neutralPose = new PoseStack().last();
            // Bake max sky+block light into UV2 so shader packs that read the
            // lightmap (BSL, Photon, Sildurs, ...) don't render the geometry as
            // pitch black at runtime. The companion VBO is shared between every
            // instance of this part, so we cannot vary the lightmap per machine
            // position without uploading per-instance attributes that the pack
            // shader can read - for now treat machines as fully lit, which
            // approximates the brightness of the baked-model path closely enough.
            final int fullBrightLight = LightTexture.pack(15, 15);
            for (BakedQuad quad : quads) {
                //? if forge {
                builder.putBulkData(neutralPose, quad,
                        1.0F, 1.0F, 1.0F, 1.0F,
                        fullBrightLight,
                        OverlayTexture.NO_OVERLAY,
                        false);
                //?} else {
                /*builder.putBulkData(neutralPose, quad,
                        1.0F, 1.0F, 1.0F,
                        fullBrightLight,
                        OverlayTexture.NO_OVERLAY);
                *///?}
            }

            BufferBuilder.RenderedBuffer rendered = builder.end();
            BufferBuilder.DrawState drawState = rendered.drawState();
            VertexFormat actualFormat = drawState.format();
            this.actualFormat = actualFormat;
            ByteBuffer vertexBytes = rendered.vertexBuffer();
            this.vertexCount = drawState.vertexCount();

            // Record byte offset of every named element in the format so
            // prepareForShader() can hand the linker-resolved locations a
            // pointer to real per-vertex data populated by Iris's
            // MixinBufferBuilder.iris$beforeNext (iris_Entity / mc_midTexCoord
            // / at_tangent). Имена и порядок — из getElementAttributeNames() /
            // getElements(); смещения накапливаем по getByteSize() (как раньше
            // по entrySet getElementMapping), без getElementName/getOffset(Element),
            // которых нет на 1.20.1 Forge.
            elementOffsets.clear();
            elementByName.clear();
            var elements = actualFormat.getElements();
            var names = actualFormat.getElementAttributeNames();
            int runningOffset = 0;
            for (int i = 0; i < elements.size(); i++) {
                String name = names.get(i);
                VertexFormatElement el = elements.get(i);
                elementOffsets.put(name, runningOffset);
                elementByName.put(name, el);
                runningOffset += el.getByteSize();
            }

            this.vaoId = GL30.glGenVertexArrays();
            this.vboId = GL15.glGenBuffers();
            if (vaoId == 0 || vboId == 0) {
                throw new IllegalStateException("Failed to generate companion VAO/VBO");
            }

            GL30.glBindVertexArray(vaoId);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBytes, GL15.GL_STATIC_DRAW);

            int stride = actualFormat.getVertexSize();
            int offset = 0;
            int detectedUv2Location = -1;
            // We only enable the SIX standard Mojang attributes (locations 0..5:
            // Position/Color/UV0/UV1/UV2/Normal). Iris's MixinShaderInstance
            // explicitly binds these names - prefixed with "iris_" - to those
            // exact locations via glBindAttribLocation, so the linker is forced
            // to honor them and our element-index = location assumption holds.
            //
            // The remaining elements in IrisVertexFormats.ENTITY (iris_Entity,
            // mc_midTexCoord, at_tangent) are injected into the transformed GLSL
            // by Iris's transformers WITHOUT layout(location = N) qualifiers and
            // are NOT bound via glBindAttribLocation for ExtendedShader. The
            // GLSL linker assigns them whatever locations it pleases - typically
            // 6/7/8 in declaration order, but driver- and pack-dependent -
            // which never matches the 7/8/9 that VertexFormat.setupBufferState
            // would derive from the element's index in the format. That mismatch
            // is the *real* root cause of the intermittent broken-geometry
            // symptom: the shader reads from a slot we never bound, falling
            // back to whatever value Embeddium's chunk re-bake or another
            // immediate-mode draw left behind in the global "current value" bank.
            //
            // We deliberately leave those slots untouched here and let
            // prepareForShader(programId) bind them at the *correct*,
            // linker-resolved locations using the real per-vertex data Iris's
            // MixinBufferBuilder wrote into our VBO via iris$beforeNext.
            int elementCount = actualFormat.getElements().size();
            for (int location = 0; location < elementCount; location++) {
                VertexFormatElement element = actualFormat.getElements().get(location);
                if (element.getUsage() == VertexFormatElement.Usage.PADDING) {
                    offset += element.getByteSize();
                    continue;
                }
                if (location > 5) {
                    offset += element.getByteSize();
                    continue;
                }
                GL20.glEnableVertexAttribArray(location);
                int glType = element.getType().getGlType();
                int count = element.getCount();
                // Pack shaders declare UV1/UV2 as `ivec2` and Mojang itself binds
                // them with glVertexAttribIPointer (integer pipeline). Using the
                // float-converting glVertexAttribPointer for SHORT-typed integer
                // UV attributes leaves the shader reading garbage from the wrong
                // attribute "bank" - the symptom looks like uniform max/min
                // brightness regardless of the per-vertex/per-draw value.
                if (isIntegerAttribute(element)) {
                    GL30.glVertexAttribIPointer(location, count, glType, stride, offset);
                } else {
                    boolean normalize = shouldNormalize(element);
                    GL20.glVertexAttribPointer(location, count, glType, normalize, stride, offset);
                }
                if (element.getUsage() == VertexFormatElement.Usage.UV && element.getIndex() == 2) {
                    detectedUv2Location = location;
                }
                offset += element.getByteSize();
            }

            // Disable the per-vertex UV2 (lightmap) array inside this VAO so that
            // callers can later inject a per-draw constant via
            // glVertexAttrib2f(uv2Location, blockU, skyV) and still have the pack
            // shader read a sane value from `vaUV2`. The fully-bright values that
            // putBulkData wrote into the buffer remain as a static fallback for
            // any future code path that re-enables this attribute.
            if (detectedUv2Location != -1) {
                GL20.glDisableVertexAttribArray(detectedUv2Location);
            }
            this.uv2Location = detectedUv2Location;

            // Build a triangulated index buffer matching our standard quad layout
            // (0,1,2,2,3,0) so that we can reuse the companion mesh with raw glDrawElements.
            int quadCount = vertexCount / 4;
            int[] indices = new int[quadCount * 6];
            for (int q = 0, idx = 0, base = 0; q < quadCount; q++, base += 4) {
                indices[idx++] = base;
                indices[idx++] = base + 1;
                indices[idx++] = base + 2;
                indices[idx++] = base + 2;
                indices[idx++] = base + 3;
                indices[idx++] = base;
            }
            this.eboId = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
            this.indexCount = indices.length;

            rendered.release();
            built = true;
            MainRegistry.LOGGER.debug(
                "IrisCompanionMesh: built {} quads, format stride {} bytes, {} attribute(s)",
                quadCount, stride, elementCount);
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("IrisCompanionMesh: build failed ({}); will fall back to vanilla path",
                t.getMessage());
            failed = true;
            destroy();
            return false;
        } finally {
            GL30.glBindVertexArray(previousVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }

    /**
     * First pass over the raw {@link BakedQuad} vertex arrays: computes the
     * object-space AABB ({@link #objBbox}) and the per-vertex trilinear
     * weight table ({@link #perVertexCornerWeights}).
     * <p>
     * Vertex iteration order MUST match what {@link BufferBuilder#putBulkData}
     * later writes into the companion VBO — otherwise the GPU draws vertex
     * {@code v} with weight[v'] for some {@code v' != v} and the per-vertex
     * lightmap UV2 bleeds diagonally across the mesh. {@code putBulkData}
     * walks each quad's 4 vertices in index order 0..3, which matches our
     * {@code for (int i = 0; i < 4; i++)} loop below.
     * <p>
     * The BakedQuad vertex layout is Mojang's {@code DefaultVertexFormat.BLOCK}
     * (which is what the bakery produces regardless of the render type used
     * later) — stride 8 ints per vertex, positions at {@code raw[base+0..2]}
     * as {@link Float#intBitsToFloat} of the ints. Same layout as
     * {@code PartGeometry.buildVboDataFromQuads}.
     * <p>
     * Degenerate (zero-extent) axes are clamped to a tiny positive size so
     * the later {@code (pos - min) / size} divide returns a stable value
     * (0.5) instead of NaN — this happens for flat decorative panels that
     * have, say, zero thickness on the Y axis.
     */
    private void computeObjBboxAndWeights() {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        int provisionalVertexCount = 0;
        for (BakedQuad quad : quads) {
            int[] raw = quad.getVertices();
            if (raw == null || raw.length < 32) continue;
            for (int i = 0; i < 4; i++) {
                int base = i * 8;
                float x = Float.intBitsToFloat(raw[base + 0]);
                float y = Float.intBitsToFloat(raw[base + 1]);
                float z = Float.intBitsToFloat(raw[base + 2]);
                if (x < minX) minX = x; if (x > maxX) maxX = x;
                if (y < minY) minY = y; if (y > maxY) maxY = y;
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
                provisionalVertexCount++;
            }
        }

        if (!Float.isFinite(minX) || provisionalVertexCount == 0) {
            objBbox[0] = objBbox[1] = objBbox[2] = 0f;
            objBbox[3] = objBbox[4] = objBbox[5] = 1f;
            perVertexCornerWeights = null;
            perVertexSlicedWeights = null;
            return;
        }

        objBbox[0] = minX; objBbox[1] = minY; objBbox[2] = minZ;
        objBbox[3] = maxX; objBbox[4] = maxY; objBbox[5] = maxZ;

        // Precompute size with a tiny floor so thin/flat parts don't produce
        // NaN weights. 1e-4 (100 microns) is far below the lightmap's 1-block
        // granularity and invisible in the interpolated result.
        float sx = Math.max(maxX - minX, 1e-4f);
        float sy = Math.max(maxY - minY, 1e-4f);
        float sz = Math.max(maxZ - minZ, 1e-4f);
        float invSx = 1f / sx, invSy = 1f / sy, invSz = 1f / sz;

        perVertexCornerWeights = new float[provisionalVertexCount * 8];
        perVertexSlicedWeights = new float[provisionalVertexCount * 16];
        int wBase = 0;
        int wsBase = 0;
        for (BakedQuad quad : quads) {
            int[] raw = quad.getVertices();
            if (raw == null || raw.length < 32) continue;
            for (int i = 0; i < 4; i++) {
                int base = i * 8;
                float x = Float.intBitsToFloat(raw[base + 0]);
                float y = Float.intBitsToFloat(raw[base + 1]);
                float z = Float.intBitsToFloat(raw[base + 2]);
                float wx = Math.max(0f, Math.min(1f, (x - minX) * invSx));
                float wy = Math.max(0f, Math.min(1f, (y - minY) * invSy));
                float wz = Math.max(0f, Math.min(1f, (z - minZ) * invSz));
                float oneMinusWx = 1f - wx, oneMinusWy = 1f - wy, oneMinusWz = 1f - wz;

                // Corner index encoding: bit 0=x, bit 1=y, bit 2=z; set → max side.
                // Unrolled + factored to minimise multiplies (8 muls instead of 24).
                float axy0 = oneMinusWx * oneMinusWy;
                float axy1 = wx * oneMinusWy;
                float axy2 = oneMinusWx * wy;
                float axy3 = wx * wy;
                perVertexCornerWeights[wBase + 0] = axy0 * oneMinusWz;
                perVertexCornerWeights[wBase + 1] = axy1 * oneMinusWz;
                perVertexCornerWeights[wBase + 2] = axy2 * oneMinusWz;
                perVertexCornerWeights[wBase + 3] = axy3 * oneMinusWz;
                perVertexCornerWeights[wBase + 4] = axy0 * wz;
                perVertexCornerWeights[wBase + 5] = axy1 * wz;
                perVertexCornerWeights[wBase + 6] = axy2 * wz;
                perVertexCornerWeights[wBase + 7] = axy3 * wz;
                wBase += 8;

                // 2x4x2 sliced weights: 4 Y slices => interpolate between adjacent slices.
                // Each slice has 4 XZ corners laid out as:
                //   0: x0z0, 1: x1z0, 2: x0z1, 3: x1z1
                float ty = wy * 3.0f;
                int s0 = (int) Math.floor(ty);
                if (s0 < 0) s0 = 0;
                if (s0 > 3) s0 = 3;
                int s1 = Math.min(s0 + 1, 3);
                float fy = ty - (float) s0;
                if (fy < 0f) fy = 0f;
                if (fy > 1f) fy = 1f;
                float w0 = 1f - fy;
                float w1 = fy;

                float b00 = oneMinusWx * oneMinusWz;
                float b10 = wx * oneMinusWz;
                float b01 = oneMinusWx * wz;
                float b11 = wx * wz;

                // Zero 16 weights (array is fresh but we write sequentially; keep explicit for clarity).
                for (int p = 0; p < 16; p++) perVertexSlicedWeights[wsBase + p] = 0f;
                int o0 = wsBase + s0 * 4;
                int o1 = wsBase + s1 * 4;
                perVertexSlicedWeights[o0 + 0] = b00 * w0;
                perVertexSlicedWeights[o0 + 1] = b10 * w0;
                perVertexSlicedWeights[o0 + 2] = b01 * w0;
                perVertexSlicedWeights[o0 + 3] = b11 * w0;
                perVertexSlicedWeights[o1 + 0] += b00 * w1;
                perVertexSlicedWeights[o1 + 1] += b10 * w1;
                perVertexSlicedWeights[o1 + 2] += b01 * w1;
                perVertexSlicedWeights[o1 + 3] += b11 * w1;
                wsBase += 16;
            }
        }
    }

    /**
     * @return {@code true} if per-vertex trilinear lighting is available for
     *         this mesh (weights precomputed + UV2 attribute location known).
     */
    public boolean supportsPerVertexLightmap() {
        return built && uv2Location != -1 && perVertexCornerWeights != null && vertexCount > 0;
    }

    /**
     * @return {@code true} if {@link #writeInstanceLightmap} can combine a
     *         {@code float[32]} 2×4×2 light probe set with {@link #perVertexSlicedWeights}
     *         (Iris / extended path matches tall VBOs that use
     *         {@code LightSampleCache#getOrSample16}).
     */
    public boolean supportsSlicedPerVertexLightmap() {
        return built && uv2Location != -1 && perVertexSlicedWeights != null && vertexCount > 0;
    }

    /**
     * Ensures the auxiliary lightmap VBO + CPU scratch can hold at least
     * {@code requiredInstances} concurrent instance slots. Grows (never
     * shrinks) to the next power-of-two. No-op if already large enough.
     * <p>
     * Each slot holds {@code vertexCount} pairs of 16-bit lightmap values
     * (blockU, skyV) → {@code vertexCount * 4 bytes}. For a 2000-vertex part
     * with 64 slots that is {@code 512 KB} — comfortably small even summed
     * across every distinct multiblock part in the game.
     * <p>
     * Must be called on the render thread; allocates GL resources on first
     * use.
     */
    public void ensureLightmapCapacity(int requiredInstances) {
        if (!supportsPerVertexLightmap()) return;
        if (requiredInstances <= lightmapInstanceCapacity) return;

        // Grow geometrically so repeated small increments don't thrash the
        // driver's buffer allocator. The VRAM cost of over-allocating is tiny
        // relative to the allocation churn; the max realistic peak per
        // machine part is under a few hundred instances anyway.
        int newCapacity = Math.max(8, lightmapInstanceCapacity);
        while (newCapacity < requiredInstances) newCapacity <<= 1;

        int perSlotBytes = vertexCount * 4;
        int totalBytes = newCapacity * perSlotBytes;

        // 1. Полностью удаляем старый буфер (это автоматически снимет mapping)
        if (lightmapVboId != -1) {
            GL15.glDeleteBuffers(lightmapVboId);
            lightmapVboId = -1;
        }
        if (lightmapStagingVboId != -1) {
            GL15.glDeleteBuffers(lightmapStagingVboId);
            lightmapStagingVboId = -1;
        }
        if (lightmapStagingFence != 0L) {
            try {
                GL32.glDeleteSync(lightmapStagingFence);
            } catch (Throwable ignored) {}
            lightmapStagingFence = 0L;
        }

        // 2. Очищаем все ссылки на старую память
        lightmapMapped = null;
        lightmapPersistentMapped = false;
        lightmapPersistentDirty = false;
        lightmapShortView = null;

        lightmapStagingMapped = null;
        lightmapStagingShortView = null;
        lightmapStagingAvailable = false;

        if (lightmapCpuScratch != null) {
            MemoryUtil.memFree(lightmapCpuScratch);
            lightmapCpuScratch = null;
        }

        // 3. Генерируем абсолютно новый VBO
        lightmapVboId = GL15.glGenBuffers();
        if (lightmapVboId == 0) {
            lightmapVboId = -1;
            MainRegistry.LOGGER.warn("IrisCompanionMesh: failed to allocate lightmap VBO; falling back to constant UV2");
            return;
        }

        boolean canPersistentMap = false;
        try {
            var caps = GL.getCapabilities();
            canPersistentMap = caps != null && (caps.OpenGL44 || caps.GL_ARB_buffer_storage);
        } catch (Throwable ignored) {
            canPersistentMap = false;
        }

        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapVboId);

            if (canPersistentMap) {
                try {
                    int storageFlags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT
                            | GL44.GL_MAP_COHERENT_BIT | GL44.GL_DYNAMIC_STORAGE_BIT;
                    if (GL.getCapabilities().OpenGL44) {
                        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, (long) totalBytes, storageFlags);
                    } else {
                        // ARB_buffer_storage path (same entrypoint in LWJGL)
                        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, (long) totalBytes, storageFlags);
                    }
                    int mapFlags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
                    lightmapMapped = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0L, (long) totalBytes, mapFlags, lightmapMapped);
                    if (lightmapMapped != null) {
                        lightmapMapped.order(ByteOrder.nativeOrder());
                        lightmapShortView = lightmapMapped.asShortBuffer();
                        lightmapPersistentMapped = true;
                    }
                } catch (Throwable t) {
                    // Fall back below.
                    lightmapMapped = null;
                    lightmapShortView = null;
                    lightmapPersistentMapped = false;
                    MainRegistry.LOGGER.debug("IrisCompanionMesh: persistent mapping unavailable, falling back ({})", t.toString());
                }
            }

            if (!lightmapPersistentMapped) {
                // Fallback: allocate CPU scratch and a classic DYNAMIC buffer.
                lightmapCpuScratch = MemoryUtil.memAlloc(totalBytes).order(ByteOrder.nativeOrder());
                lightmapShortView = lightmapCpuScratch.asShortBuffer();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) totalBytes, GL15.GL_DYNAMIC_DRAW);
            }
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }

        // Optional staging path: if buffer storage is available, try to allocate a persistently mapped
        // staging buffer with EXPLICIT_FLUSH. This can be used even when coherent mapping of the target
        // buffer failed (some drivers are picky about coherent).
        if (!lightmapPersistentMapped && canPersistentMap) {
            try {
                lightmapStagingVboId = GL15.glGenBuffers();
                if (lightmapStagingVboId != 0) {
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapStagingVboId);
                    int storageFlags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT
                            | GL44.GL_DYNAMIC_STORAGE_BIT;
                    // No COHERENT here: we use explicit flush + fence.
                    GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, (long) totalBytes, storageFlags);
                    int mapFlags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_FLUSH_EXPLICIT_BIT;
                    lightmapStagingMapped = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0L, (long) totalBytes, mapFlags, lightmapStagingMapped);
                    if (lightmapStagingMapped != null) {
                        lightmapStagingMapped.order(ByteOrder.nativeOrder());
                        lightmapStagingShortView = lightmapStagingMapped.asShortBuffer();
                        lightmapStagingAvailable = true;
                    }
                }
            } catch (Throwable t) {
                // If staging allocation fails, just keep the normal fallback path.
                lightmapStagingMapped = null;
                lightmapStagingShortView = null;
                lightmapStagingAvailable = false;
                if (lightmapStagingVboId != -1) {
                    try { GL15.glDeleteBuffers(lightmapStagingVboId); } catch (Throwable ignored) {}
                }
                lightmapStagingVboId = -1;
            } finally {
                // Restore bindings.
                try {
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
                    // Avoid querying GL_COPY_*_BUFFER_BINDING constants across LWJGL variants;
                    // unbind copy targets instead (safe and fast).
                    GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
                    GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
                } catch (Throwable ignored) {}
            }
        }

        lightmapInstanceCapacity = newCapacity;
        // Re-binding the UV2 pointer against the freshly re-sized VBO is the
        // caller's responsibility on the next activatePerVertexLightmap().
        // Invalidate our cached "active" state so the next activate call
        // actually issues the bind.
        perVertexLightmapActive = false;
        lightmapCurrentSlot = -1;

        // Reset slot allocator + dirty tracking to match the new capacity.
        lightmapKeyToSlot.clear();
        lightmapSlotKey = new long[newCapacity];
        lightmapEvictCursor = 0;
        lightmapDirtyMinSlot = Integer.MAX_VALUE;
        lightmapDirtyMaxSlot = -1;
    }

    /**
     * Allocates or reuses a slot for the given lightmap key.
     *
     * @return packed long: low 32 bits = slot, bit 32 = reused flag (1 means slot already contained key).
     */
    public long allocLightmapSlot(long key) {
        if (!supportsPerVertexLightmap()) return 0L;
        if (key == 0L) key = 1L;
        int slot = lightmapKeyToSlot.getOrDefault(key, -1);
        if (slot >= 0 && slot < lightmapInstanceCapacity) {
            return (((long) 1) << 32) | (slot & 0xFFFF_FFFFL);
        }
        // Allocate/evict.
        if (lightmapInstanceCapacity <= 0) return 0L;
        slot = lightmapEvictCursor++;
        if (lightmapEvictCursor >= lightmapInstanceCapacity) lightmapEvictCursor = 0;
        long oldKey = (slot < lightmapSlotKey.length) ? lightmapSlotKey[slot] : 0L;
        if (oldKey != 0L) {
            lightmapKeyToSlot.remove(oldKey);
        }
        if (slot >= lightmapSlotKey.length) {
            long[] grown = new long[Math.max(lightmapInstanceCapacity, slot + 1)];
            System.arraycopy(lightmapSlotKey, 0, grown, 0, lightmapSlotKey.length);
            lightmapSlotKey = grown;
        }
        lightmapSlotKey[slot] = key;
        lightmapKeyToSlot.put(key, slot);
        return (slot & 0xFFFF_FFFFL);
    }

    /**
     * Allocates or reuses a slot for the given lightmap key. The key must be a stable hash
     * of the instance's quantized 8-corner UV2 (typically derived after round+clamp to 0..240).
     * <p>
     * Slot reuse is LRU-ish via simple eviction cursor; bounded by {@link #lightmapInstanceCapacity}.
     */
    public int getOrAllocateLightmapSlot(long key) {
        return (int) (allocLightmapSlot(key) & 0xFFFF_FFFFL);
    }

    /** Marks a slot as dirty for the fallback upload path. */
    private void markSlotDirty(int slot) {
        if (lightmapPersistentMapped) {
            lightmapPersistentDirty = true;
            return;
        }
        if (slot < lightmapDirtyMinSlot) lightmapDirtyMinSlot = slot;
        if (slot > lightmapDirtyMaxSlot) lightmapDirtyMaxSlot = slot;
    }

    /**
     * Uploads all dirty slots to the GPU in one contiguous range, for the fallback (non-persistent) path.
     * No-op when persistent mapping is active.
     */
    public void uploadDirtySlotsIfNeeded() {
        if (!supportsPerVertexLightmap()) return;
        if (lightmapPersistentMapped) {
            // Make client writes visible to subsequent vertex fetches.
            if (lightmapPersistentDirty) {
                try {
                    var caps = GL.getCapabilities();
                    if (caps != null && caps.OpenGL42) {
                        GL42.glMemoryBarrier(GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
                    }
                } catch (Throwable ignored) {}
                lightmapPersistentDirty = false;
            }
            return;
        }
        if (lightmapVboId == -1 || lightmapCpuScratch == null) return;
        if (lightmapDirtyMaxSlot < lightmapDirtyMinSlot) return;

        int firstSlot = lightmapDirtyMinSlot;
        int lastSlot = lightmapDirtyMaxSlot;
        int slotCount = (lastSlot - firstSlot) + 1;
        uploadLightmapRange(firstSlot, slotCount);
        lightmapDirtyMinSlot = Integer.MAX_VALUE;
        lightmapDirtyMaxSlot = -1;
    }

    /**
     * Convenience for callers that wrote into a set of slots and need the GPU-visible contents before drawing.
     * (Persistent mapped path: barrier; fallback: upload dirty slots.)
     */
    public void finishLightmapWrites() {
        uploadDirtySlotsIfNeeded();
    }

    /**
     * Computes per-vertex UV2 (blockU, skyV) for the given instance slot by
     * trilinearly combining the 8 corner lightmap samples with the baked
     * {@link #perVertexCornerWeights}, and writes the result as USHORT pairs
     * into {@link #lightmapCpuScratch} at offset {@code slotIndex * vertexCount * 4}.
     * <p>
     * Does NOT upload to the GPU; batch the uploads via
     * {@link #uploadLightmapRange(int, int)} after all instances for a draw
     * pass have been written (one {@code glBufferSubData} per pass is far
     * cheaper than one per instance).
     *
     * @param slotIndex  0-based slot, must be {@code < lightmapInstanceCapacity}
     * @param corner16   16 floats laid out as
     *                   {@code [c0.blockU, c0.skyV, c1.blockU, c1.skyV, ..., c7.blockU, c7.skyV]};
     *                   exactly the format {@link LightSampleCache#getOrSample8}
     *                   produces, typically already on the 0..240 scale
     */
    public void writeInstanceLightmap(int slotIndex, float[] corner16) {
        if (!supportsPerVertexLightmap()) return;
        if (slotIndex < 0 || slotIndex >= lightmapInstanceCapacity) return;
        if (corner16 == null || corner16.length < 16) return;
        if (lightmapShortView == null) return;
    
        final boolean sliced = corner16.length >= 32 && perVertexSlicedWeights != null;
        final float[] w = sliced ? perVertexSlicedWeights : perVertexCornerWeights;
        final ShortBuffer dst = lightmapShortView;
    
        int requiredLength = vertexCount * 2;
        if (lightmapTempArray.length < requiredLength) {
            lightmapTempArray = new short[requiredLength];
        }
    
        int shortOffset = slotIndex * vertexCount * 2;
        if (shortOffset < 0 || shortOffset + requiredLength > dst.capacity()) return;
    
        if (!sliced) {
            for (int v = 0; v < vertexCount; v++) {
                int wBase = v * 8;
                // Unroll цикла (процессор скажет вам спасибо)
                float blockU = w[wBase] * corner16[0] +
                               w[wBase + 1] * corner16[2] +
                               w[wBase + 2] * corner16[4] +
                               w[wBase + 3] * corner16[6] +
                               w[wBase + 4] * corner16[8] +
                               w[wBase + 5] * corner16[10] +
                               w[wBase + 6] * corner16[12] +
                               w[wBase + 7] * corner16[14];
    
                float skyV =   w[wBase] * corner16[1] +
                               w[wBase + 1] * corner16[3] +
                               w[wBase + 2] * corner16[5] +
                               w[wBase + 3] * corner16[7] +
                               w[wBase + 4] * corner16[9] +
                               w[wBase + 5] * corner16[11] +
                               w[wBase + 6] * corner16[13] +
                               w[wBase + 7] * corner16[15];
    
                // Быстрое округление вместо Math.round()
                int bu = (int) (blockU + 0.5f);
                int sv = (int) (skyV + 0.5f);
    
                // Быстрый clamp
                bu = bu < 0 ? 0 : (bu > 240 ? 240 : bu);
                sv = sv < 0 ? 0 : (sv > 240 ? 240 : sv);
    
                lightmapTempArray[v * 2] = (short) bu;
                lightmapTempArray[v * 2 + 1] = (short) sv;
            }
        } else {
            // Сделайте то же самое (unroll на 16) для sliced ветки
            for (int v = 0; v < vertexCount; v++) {
                float blockU = 0f;
                float skyV = 0f;
                int wBase = v * 16;
                for (int p = 0; p < 16; p++) { // Можно оставить цикл, если unroll на 16 выглядит громоздко, но unroll быстрее
                    float wp = w[wBase + p];
                    blockU += wp * corner16[p * 2];
                    skyV   += wp * corner16[p * 2 + 1];
                }
                int bu = (int) (blockU + 0.5f);
                int sv = (int) (skyV + 0.5f);
                lightmapTempArray[v * 2] = (short) (bu < 0 ? 0 : (bu > 240 ? 240 : bu));
                lightmapTempArray[v * 2 + 1] = (short) (sv < 0 ? 0 : (sv > 240 ? 240 : sv));
            }
        }
    
        // Массовая запись в DirectBuffer — это ОГРОМНЫЙ буст производительности
        int prevPos = dst.position();
        dst.position(shortOffset);
        dst.put(lightmapTempArray, 0, requiredLength);
        dst.position(prevPos);
    
        markSlotDirty(slotIndex);
    }

    /**
     * Uploads the populated CPU-side lightmap data for instance slots
     * {@code [firstSlot, firstSlot + slotCount)} to the GPU.
     * <p>
     * Typically called once per batch flush after all {@link #writeInstanceLightmap}
     * calls for that batch are done — this collapses what would otherwise be
     * one {@code glBufferSubData} per instance into a single driver roundtrip.
     */
    public void uploadLightmapRange(int firstSlot, int slotCount) {
        if (!supportsPerVertexLightmap()) return;
        if (lightmapPersistentMapped) return;
        if (lightmapVboId == -1 || lightmapCpuScratch == null) return;
        if (slotCount <= 0) return;

        int perSlotBytes = vertexCount * 4;
        int byteOffset = firstSlot * perSlotBytes;
        int byteLen = slotCount * perSlotBytes;
        if (byteOffset < 0 || byteOffset + byteLen > lightmapCpuScratch.capacity()) return;

        // Carve a slice out of the CPU scratch so we only upload the dirty range.
        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            lightmapCpuScratch.position(byteOffset);
            lightmapCpuScratch.limit(byteOffset + byteLen);
            if (lightmapStagingAvailable && lightmapStagingVboId != -1 && lightmapStagingMapped != null) {
                // Non-blocking fence check: if the previous copy hasn't completed, fall back
                // to glBufferSubData so we never overwrite staging data that the driver may
                // still be reading from.
                boolean stagingSafe = true;
                if (lightmapStagingFence != 0L) {
                    int wait = GL32.glClientWaitSync(lightmapStagingFence, 0, 0L);
                    if (wait == GL32.GL_TIMEOUT_EXPIRED) {
                        stagingSafe = false;
                    } else {
                        try { GL32.glDeleteSync(lightmapStagingFence); } catch (Throwable ignored) {}
                        lightmapStagingFence = 0L;
                    }
                }
                if (stagingSafe) {
                    // Copy CPU scratch -> staging mapped, flush explicit range, then copy into target VBO.
                    long dstOffset = (long) byteOffset;
                    // Bulk copy via ByteBuffer slice to avoid per-byte overhead.
                    ByteBuffer src = lightmapCpuScratch.duplicate();
                    src.position(byteOffset).limit(byteOffset + byteLen);
                    ByteBuffer dst = lightmapStagingMapped.duplicate();
                    dst.position(byteOffset).limit(byteOffset + byteLen);
                    dst.put(src);
                    try {
                        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapStagingVboId);
                        GL30.glFlushMappedBufferRange(GL15.GL_ARRAY_BUFFER, dstOffset, (long) byteLen);
                        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, lightmapStagingVboId);
                        GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, lightmapVboId);
                        GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, dstOffset, dstOffset, (long) byteLen);
                        lightmapStagingFence = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
                    } finally {
                        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
                        GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
                    }
                } else {
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapVboId);
                    GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, byteOffset, lightmapCpuScratch);
                }
            } else {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapVboId);
                GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, byteOffset, lightmapCpuScratch);
            }
        } finally {
            lightmapCpuScratch.clear();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
    }

    /**
     * Switches this VAO's UV2 attribute to read from {@link #lightmapVboId}
     * (per-vertex mode) instead of returning the per-draw constant set via
     * {@code glVertexAttribI2i}. Idempotent — repeated calls after a no-op
     * return immediately.
     * <p>
     * The VAO state is persistent across frames, so we only need to do the
     * heavy {@code glEnableVertexAttribArray + glVertexAttribIPointer} dance
     * on the first call (or after an {@link #ensureLightmapCapacity} growth
     * invalidated the binding).
     * <p>
     * The caller MUST have this VAO bound (via {@link #getVaoId()}) before
     * invoking this method. On return, the UV2 attribute points at slot 0;
     * use {@link #bindLightmapForInstance(int)} to switch slots per draw.
     */
    public void activatePerVertexLightmap() {
        if (!supportsPerVertexLightmap()) return;
        if (lightmapVboId == -1) return;
        if (perVertexLightmapActive) return;

        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapVboId);
            // Stride 4 bytes = 2 × USHORT. Pack shaders declare vaUV2 as
            // ivec2, so we go through the INTEGER pipeline (see the long
            // comment on isIntegerAttribute). Offset 0 for now —
            // bindLightmapForInstance repoints it per draw.
            GL30.glVertexAttribIPointer(uv2Location, 2, GL11.GL_UNSIGNED_SHORT, 4, 0L);
            GL20.glEnableVertexAttribArray(uv2Location);
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
        perVertexLightmapActive = true;
        lightmapCurrentSlot = 0;
    }

    /**
     * Repoints the VAO's UV2 attribute at the given instance slot within
     * {@link #lightmapVboId}. Must be called between
     * {@link #activatePerVertexLightmap()} and the per-instance
     * {@code glDrawElements}.
     * <p>
     * Skips the GL call when the slot matches the previous one (rare — only
     * happens if one part of the batch draws the same instance twice, e.g.
     * for shadow + main in the same batch, which our pipeline currently
     * splits into distinct passes).
     */
    public void bindLightmapForInstance(int slotIndex) {
        if (!perVertexLightmapActive) return;
        if (slotIndex == lightmapCurrentSlot) return;
        if (slotIndex < 0 || slotIndex >= lightmapInstanceCapacity) return;

        long byteOffset = (long) slotIndex * vertexCount * 4L;

        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapVboId);
            GL30.glVertexAttribIPointer(uv2Location, 2, GL11.GL_UNSIGNED_SHORT, 4, byteOffset);
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }
        lightmapCurrentSlot = slotIndex;
    }

    /**
     * Reverts the VAO back to the per-draw constant UV2 mode (disabled
     * attribute array — callers supply the value via {@code glVertexAttribI2i}).
     * Paired with {@link #activatePerVertexLightmap()}. No-op if we were
     * never in per-vertex mode.
     * <p>
     * Rarely needed in practice — any code path that does per-vertex lighting
     * on one frame wants the same on every frame — but useful for debug
     * toggles and shader-pack hot-reload scenarios where we might need to
     * start clean.
     */
    public void restoreConstantLightmap() {
        if (!perVertexLightmapActive) return;
        if (uv2Location != -1) {
            GL20.glDisableVertexAttribArray(uv2Location);
        }
        perVertexLightmapActive = false;
        lightmapCurrentSlot = -1;
    }

    private static boolean shouldNormalize(VertexFormatElement element) {
        // Color (ubytes) and Normal (signed bytes) are conventionally normalized in
        // Mojang/Iris vertex formats; everything else (positions, ints, packed light) is not.
        return switch (element.getUsage()) {
            case COLOR, NORMAL -> true;
            default -> false;
        };
    }

    /**
     * Whether this element should be bound through {@code glVertexAttribIPointer}
     * (integer attribute pipeline) instead of {@code glVertexAttribPointer}.
     * <p>
     * Mojang/Iris declare UV1 (overlay) and UV2 (lightmap) as {@code ivec2} in the
     * vertex shader and bind them via the integer pipeline. Float-converting them
     * yields a different "current value" bank than the shader reads from, which
     * silently breaks per-draw {@code glVertexAttrib*} overrides on these slots.
     */
    private static boolean isIntegerAttribute(VertexFormatElement element) {
        if (element.getUsage() != VertexFormatElement.Usage.UV) return false;
        int idx = element.getIndex();
        return idx == 1 || idx == 2;
    }

    /**
     * Resolves the GLSL-linker-assigned locations of the Iris-extended
     * attributes ({@code iris_Entity}, {@code mc_midTexCoord},
     * {@code at_tangent}) for the given program and binds our VBO data to
     * those locations on this VAO.
     * <p>
     * <b>Why this is needed.</b> Iris's transformers
     * ({@code EntityPatcher.patchEntityId}, {@code CommonTransformer.patchMultiTexCoord3})
     * inject these attributes into the GLSL <em>without</em>
     * {@code layout(location = N)} qualifiers, and {@code MixinShaderInstance}
     * only adds explicit {@code glBindAttribLocation} calls for the six core
     * Mojang attributes (Position/Color/UV0/UV1/UV2/Normal → 0..5). The linker
     * is therefore free to put {@code iris_Entity} at, say, location 6 while
     * Mojang's {@code VertexFormat.setupBufferState} would have bound the
     * {@code IrisVertexFormats.ENTITY} format's iris_Entity element (which
     * sits at list-index 7 in the format) to GL location 7. Whenever those
     * disagree, the shader reads garbage from a slot nothing ever bound -
     * which is exactly the broken-geometry symptom that appears intermittently
     * near torches and gets dramatically worse when redstone particles or
     * Embeddium chunk re-bakes pollute the global "current attribute value"
     * bank.
     * <p>
     * <b>The fix.</b> We query {@link GL20#glGetAttribLocation} per attribute
     * name to get the real linker-resolved location, then bind a VBO pointer
     * to the correct byte offset within our VBO using the appropriate
     * integer/float pipeline. Iris's {@code MixinBufferBuilder.iris$beforeNext}
     * already wrote correct per-vertex values for these attributes into our
     * VBO when we called {@link BufferBuilder#putBulkData}, so the shader now
     * reads real, stable data - completely immune to any global state
     * pollution from other rendering paths.
     * <p>
     * The caller MUST have this VAO bound (via {@link #getVaoId()}) before
     * invoking this method. The work is cached per program ID, so repeated
     * calls with the same shader are nearly free.
     */
    public void prepareForShader(int programId) {
        if (programId <= 0) return;
        if (actualFormat == null) return;

        // Generation gate: if Iris rebuilt the pipeline since we last filled
        // this cache, every entry is potentially stale (GL recycles program
        // IDs across glDeleteProgram→glLinkProgram cycles, so a raw-int match
        // below would let us skip attribute rebinding against a structurally
        // different program). Wipe the ring buffer so every distinct program
        // in the new generation pays the resolve cost once, then caches.
        long currentGen = com.hbm_m.client.render.shader.IrisExtendedShaderAccess.getPipelineGeneration();
        if (cachedProgramPipelineGeneration != currentGen) {
            for (int i = 0; i < CACHED_PROGRAM_SLOTS; i++) cachedProgramIds[i] = -1;
            nextCacheSlot = 0;
            cachedProgramPipelineGeneration = currentGen;
        }

        // Fast path: scan the small cache for a hit. A linear scan over 4 ints is
        // dramatically faster than the alternative - re-running the
        // glGetAttribLocation + glVertexAttrib*Pointer + glEnableVertexAttribArray
        // sequence three times.
        for (int i = 0; i < CACHED_PROGRAM_SLOTS; i++) {
            if (cachedProgramIds[i] == programId) return;
        }

        int previousArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        try {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
            int stride = actualFormat.getVertexSize();
            bindIrisAttribute(programId, "iris_Entity", stride);
            bindIrisAttribute(programId, "mc_midTexCoord", stride);
            bindIrisAttribute(programId, "at_tangent", stride);
        } finally {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousArrayBuffer);
        }

        // Insert into the ring buffer; oldest entry is evicted. Done AFTER the
        // bind so a thrown exception above doesn't leave us with a stale "cached"
        // claim that skips re-binding next time.
        cachedProgramIds[nextCacheSlot] = programId;
        nextCacheSlot = (nextCacheSlot + 1) % CACHED_PROGRAM_SLOTS;
    }

    /**
     * Looks up {@code attributeName}'s linker-assigned location in
     * {@code programId} and, if present in our format, binds a VBO pointer to
     * its byte offset using the correct integer-vs-float pipeline for the
     * element's GL type (USHORT-backed UV elements like {@code iris_Entity}
     * must use {@link GL30#glVertexAttribIPointer} so the {@code ivec3}
     * declaration in the shader reads from the integer attribute bank instead
     * of silently falling back to the float bank's current value).
     * <p>
     * Returns silently when either the shader does not declare the attribute
     * (linker returned -1, e.g. shader pack does not use it) or the format
     * does not contain that element (Iris not loaded, vanilla NEW_ENTITY).
     */
    private void bindIrisAttribute(int programId, String attributeName, int stride) {
        int location = GL20.glGetAttribLocation(programId, attributeName);
        Integer offsetBoxed = elementOffsets.get(attributeName);
        VertexFormatElement element = elementByName.get(attributeName);
        if (location < 0) return;
        if (offsetBoxed == null || element == null) return;
        int offset = offsetBoxed;
        int glType = element.getType().getGlType();
        int count = element.getCount();
        if (isIntegerGlType(glType)) {
            // ivec*/uvec* attributes - read from the INTEGER bank, no
            // normalisation. iris_Entity is the canonical case (USHORT × 3
            // → ivec3 in shader).
            GL30.glVertexAttribIPointer(location, count, glType, stride, offset);
        } else {
            // Float attributes - mc_midTexCoord (FLOAT × 2 → vec2),
            // at_tangent (BYTE × 4 → vec4 normalised).
            boolean normalize = shouldNormalizeForLinkerBind(attributeName, element);
            GL20.glVertexAttribPointer(location, count, glType, normalize, stride, offset);
        }
        GL20.glEnableVertexAttribArray(location);
    }

    private static boolean isIntegerGlType(int glType) {
        return glType == GL11.GL_BYTE
            || glType == GL11.GL_UNSIGNED_BYTE
            || glType == GL11.GL_SHORT
            || glType == GL11.GL_UNSIGNED_SHORT
            || glType == GL11.GL_INT
            || glType == GL11.GL_UNSIGNED_INT;
    }

    /**
     * Tangent vectors packed as 4 signed bytes in the VBO are read by pack
     * shaders as {@code vec4} components in the [-1, +1] range, so they need
     * GL_TRUE for the normalize parameter when bound through the float
     * pipeline. {@code mc_midTexCoord} is already in the proper [0, 1] UV
     * range as a true float and must NOT be normalised.
     */
    private static boolean shouldNormalizeForLinkerBind(String attributeName, VertexFormatElement element) {
        if ("at_tangent".equals(attributeName)) return true;
        return element.getUsage() == VertexFormatElement.Usage.NORMAL;
    }

    public boolean isBuilt() {
        return built;
    }

    public boolean isFailed() {
        return failed;
    }

    public int getVaoId() {
        return vaoId;
    }

    /** GL handle of the VBO this companion holds - used by debug logging. */
    public int getVboId() {
        return vboId;
    }

    public int getIndexCount() {
        return indexCount;
    }

    /**
     * @return the GL attribute location for UV2 (lightmap) within this VAO, or
     *         {@code -1} if the format does not contain such an element.
     *         Use with {@code glVertexAttrib2f(getUv2Location(), blockU, skyV)}
     *         to override the lightmap on a per-draw basis.
     */
    public int getUv2Location() {
        return uv2Location;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void destroy() {
        final int vboToDelete = this.vboId;
        final int eboToDelete = this.eboId;
        final int vaoToDelete = this.vaoId;
        final int lightmapVboToDelete = this.lightmapVboId;
        final ByteBuffer scratchToFree = this.lightmapCpuScratch;
        final ByteBuffer mappedToUnmap = this.lightmapMapped;
        this.vboId = -1;
        this.eboId = -1;
        this.vaoId = -1;
        this.lightmapVboId = -1;
        this.lightmapCpuScratch = null;
        this.lightmapMapped = null;
        this.lightmapShortView = null;
        this.lightmapPersistentMapped = false;
        this.lightmapPersistentDirty = false;
        this.lightmapInstanceCapacity = 0;
        this.perVertexLightmapActive = false;
        this.lightmapCurrentSlot = -1;
        this.perVertexCornerWeights = null;
        this.perVertexSlicedWeights = null;
        this.indexCount = 0;
        this.vertexCount = 0;
        this.built = false;

        Runnable deleter = () -> {
            try {
                if (vboToDelete != -1) GL15.glDeleteBuffers(vboToDelete);
                if (eboToDelete != -1) GL15.glDeleteBuffers(eboToDelete);
                if (mappedToUnmap != null && lightmapVboToDelete != -1) {
                    try {
                        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, lightmapVboToDelete);
                        GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
                    } catch (Throwable ignored) {}
                }
                if (lightmapVboToDelete != -1) GL15.glDeleteBuffers(lightmapVboToDelete);
                if (vaoToDelete != -1) GL30.glDeleteVertexArrays(vaoToDelete);
            } catch (Throwable t) {
                MainRegistry.LOGGER.error("IrisCompanionMesh: cleanup failed", t);
            }
        };
        if (RenderSystem.isOnRenderThread()) {
            deleter.run();
        } else {
            RenderSystem.recordRenderCall(deleter::run);
        }
        // CPU-side native memory must be freed regardless of thread; MemoryUtil
        // uses the off-heap malloc allocator which is thread-safe.
        if (scratchToFree != null) {
            MemoryUtil.memFree(scratchToFree);
        }
    }
}
