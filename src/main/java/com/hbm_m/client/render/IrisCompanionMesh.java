package com.hbm_m.client.render;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
@OnlyIn(Dist.CLIENT)
public final class IrisCompanionMesh {

    private final List<BakedQuad> quads;
    private int vaoId = -1;
    private int vboId = -1;
    private int eboId = -1;
    private int indexCount = 0;
    private int vertexCount = 0;
    /**
     * GL attribute location of the per-vertex lightmap (UV2) within this VAO.
     * Set during {@link #ensureBuilt()}; -1 when the format does not contain a
     * UV2 element. The array for this attribute is intentionally <b>disabled</b>
     * inside the VAO so that callers can supply a per-draw constant via
     * {@code glVertexAttrib2f(uv2Location, ...)}, which the bound pack shader
     * sees as a single uniform-style lightmap value for the whole draw - that
     * is how we get true per-machine lighting without rebuilding the VBO or
     * patching the shader pack's GLSL.
     */
    private int uv2Location = -1;
    private boolean built = false;
    private boolean failed = false;

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
                builder.putBulkData(neutralPose, quad,
                        1.0F, 1.0F, 1.0F, 1.0F,
                        fullBrightLight,
                        OverlayTexture.NO_OVERLAY,
                        false);
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
            // / at_tangent). The element-mapping ImmutableMap iterates in the
            // same order as getElements() - that's what Mojang's VertexFormat
            // constructor guarantees - so a running offset over its entrySet()
            // matches the per-element layout in the VBO byte stream.
            elementOffsets.clear();
            elementByName.clear();
            int runningOffset = 0;
            for (Map.Entry<String, VertexFormatElement> entry : actualFormat.getElementMapping().entrySet()) {
                elementOffsets.put(entry.getKey(), runningOffset);
                elementByName.put(entry.getKey(), entry.getValue());
                runningOffset += entry.getValue().getByteSize();
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
        this.vboId = -1;
        this.eboId = -1;
        this.vaoId = -1;
        this.indexCount = 0;
        this.vertexCount = 0;
        this.built = false;

        Runnable deleter = () -> {
            try {
                if (vboToDelete != -1) GL15.glDeleteBuffers(vboToDelete);
                if (eboToDelete != -1) GL15.glDeleteBuffers(eboToDelete);
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
    }
}
