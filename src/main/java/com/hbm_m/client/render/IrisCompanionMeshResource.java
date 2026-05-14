package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * GPU-resource-holder surface of {@link IrisCompanionMesh}.
 * <p>
 * Declares only methods that manage VAO/VBO/EBO and auxiliary lightmap VBO
 * state. Draw-call issuance and shader-state interpretation are intentionally
 * excluded — they belong to {@link IrisCompanionMeshRenderer}.
 * <p>
 * {@link IrisCompanionMesh} implements this interface; call sites that only
 * need GPU lifecycle can depend on this type.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public interface IrisCompanionMeshResource {

    /**
     * Attempts to build the companion VAO/VBO/EBO. Must be called on the
     * render thread; returns {@code false} if a previous build failed.
     * <p>
     * Implemented by {@link IrisCompanionMesh#ensureBuilt()}.
     */
    boolean ensureBuilt();

    /**
     * @return GL handle of the companion VAO, or {@code -1} when not built.
     * <p>
     * Implemented by {@link IrisCompanionMesh#getVaoId()}.
     */
    int getVaoId();

    /**
     * @return number of indices in the EBO ({@code quadCount * 6}), or
     *         {@code 0} when not built.
     * <p>
     * Implemented by {@link IrisCompanionMesh#getIndexCount()}.
     */
    int getIndexCount();

    /**
     * Resolves Iris-injected attribute locations (iris_Entity, mc_midTexCoord,
     * at_tangent) for the given GL program and binds VBO pointers on this
     * VAO. Results are cached per program ID and invalidated on Iris pipeline
     * generation changes.
     * <p>
     * Implemented by {@link IrisCompanionMesh#prepareForShader(int)}.
     */
    void prepareForShader(int programId);

    /**
     * @return {@code true} if this mesh has a UV2 attribute and precomputed
     *         per-vertex trilinear weights, i.e. per-vertex lightmap upload
     *         is supported.
     * <p>
     * Implemented by {@link IrisCompanionMesh#supportsPerVertexLightmap()}.
     */
    boolean supportsPerVertexLightmap();

    /**
     * @return GL attribute location of UV2 (lightmap) within this VAO, or
     *         {@code -1} when absent.
     * <p>
     * Implemented by {@link IrisCompanionMesh#getUv2Location()}.
     */
    int getUv2Location();

    /**
     * Grows the auxiliary per-instance lightmap VBO + CPU scratch to hold at
     * least {@code requiredInstances} slots. No-op if already large enough.
     * Render-thread only.
     * <p>
     * Implemented by {@link IrisCompanionMesh#ensureLightmapCapacity(int)}.
     */
    void ensureLightmapCapacity(int requiredInstances);

    /**
     * Computes per-vertex UV2 for the given instance slot by combining the
     * supplied corner lightmap samples with the precomputed trilinear
     * weights, and stages the result for upload.
     * <p>
     * Implemented by {@link IrisCompanionMesh#writeInstanceLightmap(int, float[])}.
     */
    void writeInstanceLightmap(int slotIndex, float[] corner16);

    /**
     * Makes all pending {@link #writeInstanceLightmap(int, float[])} writes
     * visible to subsequent vertex fetches (memory barrier on the
     * persistent-mapped path, {@code glBufferSubData} flush on the fallback
     * path).
     * <p>
     * Implemented by {@link IrisCompanionMesh#finishLightmapWrites()}.
     */
    void finishLightmapWrites();

    /**
     * Switches this VAO's UV2 attribute from per-draw-constant mode to
     * per-vertex mode (reading from the auxiliary lightmap VBO). Idempotent.
     * Caller must have the VAO bound.
     * <p>
     * Implemented by {@link IrisCompanionMesh#activatePerVertexLightmap()}.
     */
    void activatePerVertexLightmap();

    /**
     * Repoints the VAO's UV2 attribute at the given instance slot within the
     * auxiliary lightmap VBO. Must be called between
     * {@link #activatePerVertexLightmap()} and the per-instance draw.
     * <p>
     * Implemented by {@link IrisCompanionMesh#bindLightmapForInstance(int)}.
     */
    void bindLightmapForInstance(int slotIndex);

    /**
     * Reverts the VAO back to per-draw-constant UV2 mode (disabled attribute
     * array). Paired with {@link #activatePerVertexLightmap()}.
     * <p>
     * Implemented by {@link IrisCompanionMesh#restoreConstantLightmap()}.
     */
    void restoreConstantLightmap();

    /**
     * Releases all GL resources (VAO/VBO/EBO + auxiliary lightmap buffers)
     * and native CPU scratch. Safe to call off the render thread - deletion
     * is deferred via {@code RenderSystem.recordRenderCall}.
     * <p>
     * Implemented by {@link IrisCompanionMesh#destroy()}.
     */
    void destroy();
}
