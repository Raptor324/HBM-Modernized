package com.hbm_m.client.model.nmesh;

import com.hbm_m.client.render.HalfFloat;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Zero-upfront-quad {@link BakedModel} for a single machine/door/battery part backed by {@link NMeshData}.
 * World VBOs compile directly from the pre-welded binary arrays; {@link BakedQuad}s are materialized
 * lazily only if an item render (GUI/hand) or CPU door clipper requests {@link #getQuads}.
 */
public final class NMeshPartBakedModel implements BakedModel {

    public record BoundSubMesh(NMeshData.SubMesh subMesh, TextureAtlasSprite sprite) {}

    private final List<BoundSubMesh> subMeshes;
    private final boolean flipV;
    private final Transformation rootTransform;
    private final ItemTransforms transforms;
    private final TextureAtlasSprite particleSprite;
    /** BakedQuad shade flag ("shade_quads" in the model JSON). */
    private final boolean shade;
    /** Per-vertex ambient occlusion ("ambientocclusion"/"ao"). */
    private final boolean ambientOcclusion;
    /**
     * When true, quads are bucketed per dominant Direction so the chunk renderer can cull
     * faces against opaque neighbours (ObjModel "automatic_culling": true semantics).
     * When false, all quads live in the side == null bucket and are never culled.
     */
    private final boolean cullFaces;
    /** BakedQuad tint index ("tint_index"; -1 = untinted). */
    private final int tintIndex;
    /** Chunk render type from the model JSON "render_type" hint; null = block default. */
    @Nullable private final RenderType blockRenderType;
    /** Item render type derived from the same hint; null = default item fallback. */
    @Nullable private final RenderType itemRenderType;
    /**
     * Forge ObjModel "emissive_ambient" model setting (default true): when on, each sub-mesh's
     * MTL Ka drives a baked fake lightmap (full-bright effect) and disables quad shading.
     */
    private final boolean emissiveAmbient;
    /**
     * Smooth per-vertex shading ("smooth_shade" in the model JSON): the vanilla block face weights
     * (UP 1.0, DOWN 0.5, N/S 0.8, E/W 0.6) are evaluated on each vertex's TRUE normal and baked
     * into the vertex colour, so the GPU interpolates the diffuse across curved triangles — the
     * 1.7.10 fixed-function GL_SMOOTH look. Off = flat per-mesh Kd colour; the per-quad
     * "shade_quads" flag stays off when this is on (it would double-darken).
     */
    private final boolean smoothShade;
    private volatile List<BakedQuad> lazyQuads;
    /** Culled faces per direction. */
    private volatile Map<Direction, List<BakedQuad>> lazyQuadsBySide;
    /** Unculled faces (Forge addUnculledFace semantics); served from the side == null bucket. */
    private volatile List<BakedQuad> lazyQuadsUnculled = Collections.emptyList();

    /**
     * Legacy constructor: shade = true, AO = true, no face culling (all quads in the null bucket),
     * no tint, block-default render types, no MTL fake-light. Matches the pre-M3 behaviour of
     * machine/door/weapon models.
     */
    public NMeshPartBakedModel(List<BoundSubMesh> subMeshes, boolean flipV,
                               Transformation rootTransform, ItemTransforms transforms) {
        this(subMeshes, flipV, rootTransform, transforms, true, true, false, -1, null, null, false);
    }

    public NMeshPartBakedModel(List<BoundSubMesh> subMeshes, boolean flipV,
                               Transformation rootTransform, ItemTransforms transforms,
                               boolean shade, boolean ambientOcclusion, boolean cullFaces, int tintIndex,
                               @Nullable RenderType blockRenderType, @Nullable RenderType itemRenderType) {
        this(subMeshes, flipV, rootTransform, transforms, shade, ambientOcclusion, cullFaces, tintIndex,
                blockRenderType, itemRenderType, false);
    }

    public NMeshPartBakedModel(List<BoundSubMesh> subMeshes, boolean flipV,
                               Transformation rootTransform, ItemTransforms transforms,
                               boolean shade, boolean ambientOcclusion, boolean cullFaces, int tintIndex,
                               @Nullable RenderType blockRenderType, @Nullable RenderType itemRenderType,
                               boolean emissiveAmbient) {
        this(subMeshes, flipV, rootTransform, transforms, shade, ambientOcclusion, cullFaces, tintIndex,
                blockRenderType, itemRenderType, emissiveAmbient, false);
    }

    public NMeshPartBakedModel(List<BoundSubMesh> subMeshes, boolean flipV,
                               Transformation rootTransform, ItemTransforms transforms,
                               boolean shade, boolean ambientOcclusion, boolean cullFaces, int tintIndex,
                               @Nullable RenderType blockRenderType, @Nullable RenderType itemRenderType,
                               boolean emissiveAmbient, boolean smoothShade) {
        this(subMeshes, flipV, rootTransform, transforms, shade, ambientOcclusion, cullFaces, tintIndex,
                blockRenderType, itemRenderType, emissiveAmbient, smoothShade, false);
    }

    private NMeshPartBakedModel(List<BoundSubMesh> subMeshes, boolean flipV,
                               Transformation rootTransform, ItemTransforms transforms,
                               boolean shade, boolean ambientOcclusion, boolean cullFaces, int tintIndex,
                               @Nullable RenderType blockRenderType, @Nullable RenderType itemRenderType,
                               boolean emissiveAmbient, boolean smoothShade, boolean alreadyRebased) {
        this.subMeshes = List.copyOf(subMeshes);
        this.flipV = flipV;
        // Forge ObjModel.makeQuad semantics: the incoming ModelState/root transform is referenced on
        // the CENTER of the block, but OBJ-derived vertex coords are referenced on the CORNER, so the
        // transform must be re-based (blockCenterToCorner) before it is applied to raw vertices.
        // Without this, rotated/translated models shift by the rotation-pivot delta in GUI/hand.
        this.rootTransform = alreadyRebased && rootTransform != null
                ? rootTransform
                : rebaseCenterToCorner(rootTransform != null ? rootTransform : Transformation.identity());
        this.transforms = transforms != null ? transforms : ItemTransforms.NO_TRANSFORMS;
        this.shade = shade;
        this.ambientOcclusion = ambientOcclusion;
        this.cullFaces = cullFaces;
        this.tintIndex = tintIndex;
        this.blockRenderType = blockRenderType;
        this.itemRenderType = itemRenderType;
        this.emissiveAmbient = emissiveAmbient;
        this.smoothShade = smoothShade;
        TextureAtlasSprite p = null;
        for (BoundSubMesh b : this.subMeshes) {
            if (b.sprite() != null) {
                p = b.sprite();
                break;
            }
        }
        this.particleSprite = p;
    }

    /**
     * Copy with smooth per-vertex shading enabled. Used by part loaders that bake through the
     * legacy constructor (shade = true) but read a "smooth_shade" JSON key: the per-quad shade
     * flag is dropped together with the change (it would double-darken).
     */
    public NMeshPartBakedModel withSmoothShade() {
        if (this.smoothShade) {
            return this;
        }
        return new NMeshPartBakedModel(subMeshes, flipV, rootTransform, transforms,
                shade, ambientOcclusion, cullFaces, tintIndex, blockRenderType, itemRenderType,
                emissiveAmbient, true, true);
    }

    /**
     * Merges multiple {@link NMeshPartBakedModel}s into a single composite model without
     * decompressing to {@link BakedQuad}s, provided they share compatible baking flags and
     * root transform. Returns {@code null} if any parameter mismatches.
     */
    public static @Nullable NMeshPartBakedModel tryMerge(List<NMeshPartBakedModel> models) {
        if (models == null || models.isEmpty()) return null;
        if (models.size() == 1) return models.get(0);
        NMeshPartBakedModel first = models.get(0);
        int totalSubMeshes = 0;
        for (NMeshPartBakedModel m : models) {
            if (m == null) return null;
            if (m.flipV != first.flipV
                    || m.shade != first.shade
                    || m.ambientOcclusion != first.ambientOcclusion
                    || m.cullFaces != first.cullFaces
                    || m.tintIndex != first.tintIndex
                    || m.emissiveAmbient != first.emissiveAmbient
                    || m.smoothShade != first.smoothShade
                    || !java.util.Objects.equals(m.blockRenderType, first.blockRenderType)
                    || !java.util.Objects.equals(m.itemRenderType, first.itemRenderType)
                    || !m.rootTransform.equals(first.rootTransform)) {
                return null;
            }
            totalSubMeshes += m.subMeshes.size();
        }
        List<BoundSubMesh> combined = new java.util.ArrayList<>(totalSubMeshes);
        for (NMeshPartBakedModel m : models) {
            combined.addAll(m.subMeshes);
        }
        return new NMeshPartBakedModel(combined, first.flipV, first.rootTransform, first.transforms,
                first.shade, first.ambientOcclusion, first.cullFaces, first.tintIndex,
                first.blockRenderType, first.itemRenderType, first.emissiveAmbient, first.smoothShade, true);
    }

    public boolean isEmpty() {
        for (BoundSubMesh b : subMeshes) {
            if (b.subMesh().vertexCount > 0 && b.subMesh().indexCount() > 0) {
                return false;
            }
        }
        return true;
    }

    public boolean hasMaterialColors() {
        for (BoundSubMesh b : subMeshes) {
            NMeshData.SubMesh sm = b.subMesh();
            if (sm.kdR != 1f || sm.kdG != 1f || sm.kdB != 1f) return true;
            if (emissiveAmbient && (sm.kaR > 0f || sm.kaG > 0f || sm.kaB > 0f)) return true;
        }
        return false;
    }

    public @Nullable float[] getMaterialColorTint() {
        if (subMeshes.isEmpty()) return null;
        NMeshData.SubMesh first = subMeshes.get(0).subMesh();
        float fakeLight = emissiveAmbient ? ((first.kaR + first.kaG + first.kaB) / 3.0f) : 0f;
        return new float[] { first.kdR, first.kdG, first.kdB, Math.min(1f, fakeLight) };
    }

    public record DirectIrisCompanionData(
            ByteBuffer vertexBuffer,
            int[] indices,
            int vertexCount,
            float[] weldedPositions,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int stride
    ) implements AutoCloseable {
        @Override
        public void close() {
            if (vertexBuffer != null) {
                MemoryUtil.memFree(vertexBuffer);
            }
        }
    }

    /**
     * Direct binary compilation for IrisCompanionMesh: reads welded vertices, tangents, indices,
     * MTL Kd/Ka, and true barycenter mc_midTexCoord directly from NMeshData without un-welding
     * into degenerate BakedQuads.
     */
    public @Nullable DirectIrisCompanionData buildDirectIrisCompanionData(
            VertexFormat actualFormat,
            Map<String, Integer> elementOffsets,
            int stride) {
        return buildDirectIrisCompanionData(actualFormat, elementOffsets, stride, false);
    }

    public @Nullable DirectIrisCompanionData buildDirectIrisCompanionData(
            VertexFormat actualFormat,
            Map<String, Integer> elementOffsets,
            int stride,
            boolean normalizeUv) {
        int totalVerts = 0;
        int totalIndices = 0;
        for (BoundSubMesh b : subMeshes) {
            totalVerts += b.subMesh().vertexCount;
            totalIndices += b.subMesh().indexCount();
        }
        if (totalVerts == 0 || totalIndices == 0) {
            return null;
        }

        int posOff = elementOffsets.getOrDefault("position", 0);
        int colorOff = elementOffsets.getOrDefault("color", -1);
        int uv0Off = elementOffsets.getOrDefault("uv0", -1);
        int uv1Off = elementOffsets.getOrDefault("uv1", -1);
        int uv2Off = elementOffsets.getOrDefault("uv2", -1);
        int normOff = elementOffsets.getOrDefault("normal", -1);
        int irisEntityOff = elementOffsets.getOrDefault("iris_Entity", -1);
        int midTexOff = elementOffsets.getOrDefault("mc_midTexCoord", -1);
        int tangentOff = elementOffsets.getOrDefault("at_tangent", -1);

        ByteBuffer vb = MemoryUtil.memCalloc(totalVerts, stride);
        long baseAddr = MemoryUtil.memAddress(vb);
        int[] combinedIndices = new int[totalIndices];
        float[] weldedPositions = new float[totalVerts * 3];

        boolean hasTransform = !rootTransform.isIdentity();
        Matrix4f posMat = hasTransform ? rootTransform.getMatrix() : null;
        Matrix3f nrmMat = hasTransform ? rootTransform.getNormalMatrix() : null;
        Vector4f posTmp = new Vector4f();
        Vector3f nrmTmp = new Vector3f();
        Vector3f tanTmp = new Vector3f();

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        int vertBase = 0;
        int idxCursor = 0;

        try {
            for (BoundSubMesh bound : subMeshes) {
                NMeshData.SubMesh sm = bound.subMesh();
                if (!hasTransform) {
                    if (sm.minX < minX) minX = sm.minX;
                    if (sm.minY < minY) minY = sm.minY;
                    if (sm.minZ < minZ) minZ = sm.minZ;
                    if (sm.maxX > maxX) maxX = sm.maxX;
                    if (sm.maxY > maxY) maxY = sm.maxY;
                    if (sm.maxZ > maxZ) maxZ = sm.maxZ;
                }
                TextureAtlasSprite sprite = bound.sprite();
                float u0 = 0f, v0 = 0f, du = 1f, dv = 1f;
                if (!normalizeUv && sprite != null) {
                    u0 = sprite.getU0();
                    v0 = sprite.getV0();
                    du = sprite.getU1() - u0;
                    dv = sprite.getV1() - v0;
                }

                // Material diffuse color (Kd) and fakeLight from ambient (Ka)
                float kdR = sm.kdR, kdG = sm.kdG, kdB = sm.kdB;
                int colorPacked = (clampByte(kdR)) | (clampByte(kdG) << 8) | (clampByte(kdB) << 16) | (255 << 24);

                int uv2 = LightTexture.pack(15, 15);
                if (emissiveAmbient && (sm.kaR > 0f || sm.kaG > 0f || sm.kaB > 0f)) {
                    int fakeLight = (int) ((sm.kaR + sm.kaG + sm.kaB) * 15 / 3.0f);
                    uv2 = LightTexture.pack(Math.min(15, fakeLight), Math.min(15, fakeLight));
                }

                // Precompute midTexCoord as the true barycenter (UV0 + UV1 + UV2) / 3.0f for all triangles in this sub-mesh
                float[] midUArr = null;
                float[] midVArr = null;
                int smICount = sm.indexCount();
                if (midTexOff >= 0) {
                    midUArr = new float[sm.vertexCount];
                    midVArr = new float[sm.vertexCount];
                    int[] triCountPerVert = new int[sm.vertexCount];
                    for (int t = 0; t + 2 < smICount; t += 3) {
                        int i0 = sm.index(t), i1 = sm.index(t + 1), i2 = sm.index(t + 2);
                        float uA = u0 + sm.u(i0) * du;
                        float vA = v0 + (flipV ? (1.0f - sm.v(i0)) : sm.v(i0)) * dv;
                        float uB = u0 + sm.u(i1) * du;
                        float vB = v0 + (flipV ? (1.0f - sm.v(i1)) : sm.v(i1)) * dv;
                        float uC = u0 + sm.u(i2) * du;
                        float vC = v0 + (flipV ? (1.0f - sm.v(i2)) : sm.v(i2)) * dv;
                        float triMidU = (uA + uB + uC) / 3.0f;
                        float triMidV = (vA + vB + vC) / 3.0f;

                        midUArr[i0] += triMidU; midVArr[i0] += triMidV; triCountPerVert[i0]++;
                        midUArr[i1] += triMidU; midVArr[i1] += triMidV; triCountPerVert[i1]++;
                        midUArr[i2] += triMidU; midVArr[i2] += triMidV; triCountPerVert[i2]++;
                    }
                    for (int v = 0; v < sm.vertexCount; v++) {
                        if (triCountPerVert[v] > 0) {
                            midUArr[v] /= triCountPerVert[v];
                            midVArr[v] /= triCountPerVert[v];
                        } else {
                            midUArr[v] = u0 + sm.u(v) * du;
                            midVArr[v] = v0 + (flipV ? (1.0f - sm.v(v)) : sm.v(v)) * dv;
                        }
                    }
                }

                int[] smTangents = tangentOff >= 0 ? computePackedTangents(sm) : null;

                for (int v = 0; v < sm.vertexCount; v++) {
                    float x = sm.posX(v);
                    float y = sm.posY(v);
                    float z = sm.posZ(v);
                    float nx = sm.normX(v);
                    float ny = sm.normY(v);
                    float nz = sm.normZ(v);
                    float rawU = sm.u(v);
                    float rawV = flipV ? (1.0f - sm.v(v)) : sm.v(v);

                    int nPacked = sm.packedNormal(v);
                    if (hasTransform) {
                        posTmp.set(x, y, z, 1.0f).mul(posMat);
                        x = posTmp.x;
                        y = posTmp.y;
                        z = posTmp.z;
                        nrmTmp.set(nx, ny, nz).mul(nrmMat);
                        float len = nrmTmp.length();
                        if (len > 1e-6f) {
                            nx = nrmTmp.x / len;
                            ny = nrmTmp.y / len;
                            nz = nrmTmp.z / len;
                        }
                        nPacked = NMeshData.SubMesh.packNormal(nx, ny, nz);
                        if (x < minX) minX = x; if (x > maxX) maxX = x;
                        if (y < minY) minY = y; if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
                    }

                    float u = u0 + rawU * du;
                    float vt = v0 + rawV * dv;

                    int globalV = vertBase + v;
                    weldedPositions[globalV * 3]     = x;
                    weldedPositions[globalV * 3 + 1] = y;
                    weldedPositions[globalV * 3 + 2] = z;

                    long vAddr = baseAddr + (long) globalV * stride;
                    MemoryUtil.memPutFloat(vAddr + posOff, x);
                    MemoryUtil.memPutFloat(vAddr + posOff + 4L, y);
                    MemoryUtil.memPutFloat(vAddr + posOff + 8L, z);

                    if (colorOff >= 0) {
                        MemoryUtil.memPutInt(vAddr + colorOff, colorPacked);
                    }
                    if (uv0Off >= 0) {
                        MemoryUtil.memPutFloat(vAddr + uv0Off, u);
                        MemoryUtil.memPutFloat(vAddr + uv0Off + 4L, vt);
                    }
                    if (uv1Off >= 0) {
                        MemoryUtil.memPutInt(vAddr + uv1Off, OverlayTexture.NO_OVERLAY);
                    }
                    if (uv2Off >= 0) {
                        MemoryUtil.memPutInt(vAddr + uv2Off, uv2);
                    }
                    if (normOff >= 0) {
                        MemoryUtil.memPutInt(vAddr + normOff, nPacked);
                    }
                    if (irisEntityOff >= 0) {
                        MemoryUtil.memPutInt(vAddr + irisEntityOff, 0);
                    }
                    if (midTexOff >= 0 && midUArr != null) {
                        MemoryUtil.memPutFloat(vAddr + midTexOff, midUArr[v]);
                        MemoryUtil.memPutFloat(vAddr + midTexOff + 4L, midVArr[v]);
                    }
                    if (tangentOff >= 0) {
                        int packedTangent;
                        if (smTangents != null && smTangents.length > v) {
                            if (hasTransform) {
                                int rawTan = smTangents[v];
                                float tx = ((byte) (rawTan & 0xFF)) / 127.0f;
                                float ty = ((byte) ((rawTan >> 8) & 0xFF)) / 127.0f;
                                float tz = ((byte) ((rawTan >> 16) & 0xFF)) / 127.0f;
                                int bw = (rawTan >> 24) & 0xFF;
                                tanTmp.set(tx, ty, tz).mul(nrmMat);
                                float tlen = tanTmp.length();
                                if (tlen > 1e-6f) {
                                    tx = tanTmp.x / tlen;
                                    ty = tanTmp.y / tlen;
                                    tz = tanTmp.z / tlen;
                                }
                                int bx = ((int) Math.round(Math.max(-1f, Math.min(1f, tx)) * 127f)) & 0xFF;
                                int by = ((int) Math.round(Math.max(-1f, Math.min(1f, ty)) * 127f)) & 0xFF;
                                int bz = ((int) Math.round(Math.max(-1f, Math.min(1f, tz)) * 127f)) & 0xFF;
                                packedTangent = bx | (by << 8) | (bz << 16) | (bw << 24);
                            } else {
                                packedTangent = smTangents[v];
                            }
                        } else {
                            packedTangent = 127 | (127 << 24); // default +X unit tangent, +1 handedness
                        }
                        MemoryUtil.memPutInt(vAddr + tangentOff, packedTangent);
                    }
                }

                for (int i = 0; i < smICount; i++) {
                    combinedIndices[idxCursor++] = vertBase + sm.index(i);
                }
                vertBase += sm.vertexCount;
            }

            vb.position(0).limit(totalVerts * stride);
            if (!Float.isFinite(minX)) {
                minX = minY = minZ = 0f;
                maxX = maxY = maxZ = 0f;
            }
            return new DirectIrisCompanionData(vb, combinedIndices, totalVerts, weldedPositions,
                    minX, minY, minZ, maxX, maxY, maxZ, stride);
        } catch (Throwable t) {
            MemoryUtil.memFree(vb);
            throw t;
        }
    }

    /**
     * Direct binary-to-VBO compilation: bypasses BakedQuad allocation and vertex-welding hash tables.
     */
    public @Nullable SingleMeshVboRenderer.VboData buildDirectVboData(int perVertexBoneId, boolean normalizeUv) {
        int totalVerts = 0;
        int totalIndices = 0;
        for (BoundSubMesh b : subMeshes) {
            totalVerts += b.subMesh().vertexCount;
            totalIndices += b.subMesh().indexCount();
        }
        if (totalVerts == 0 || totalIndices == 0) {
            return null;
        }

        final int strideBytes = SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES; // 28 bytes
        final int intsPerVert = strideBytes >>> 2; // 7 ints

        // Material colors preservation: check if submeshes have non-default diffuse/ambient
        boolean hasMaterialColor = false;
        boolean multiMaterialMismatch = false;
        float firstKdR = 1f, firstKdG = 1f, firstKdB = 1f, firstFakeLight = 0f;
        boolean firstSubmesh = true;

        for (BoundSubMesh b : subMeshes) {
            NMeshData.SubMesh sm = b.subMesh();
            float kdR = sm.kdR, kdG = sm.kdG, kdB = sm.kdB;
            float fakeLight = emissiveAmbient ? ((sm.kaR + sm.kaG + sm.kaB) / 3.0f) : 0f;
            if (kdR != 1f || kdG != 1f || kdB != 1f || fakeLight > 0f) {
                hasMaterialColor = true;
            }
            if (firstSubmesh) {
                firstKdR = kdR; firstKdG = kdG; firstKdB = kdB; firstFakeLight = fakeLight;
                firstSubmesh = false;
            } else {
                if (Math.abs(kdR - firstKdR) > 1e-4f || Math.abs(kdG - firstKdG) > 1e-4f
                        || Math.abs(kdB - firstKdB) > 1e-4f || Math.abs(fakeLight - firstFakeLight) > 1e-4f) {
                    multiMaterialMismatch = true;
                }
            }
        }

        // Conflicting material colors across submeshes cannot be represented in a single 36-byte unshaded VBO
        // (which lacks per-vertex color attributes). Return null to trigger the BakedQuad fallback where each quad
        // carries its submesh's distinct material color.
        if (multiMaterialMismatch) {
            return null;
        }

        float[] defaultTint = hasMaterialColor
                ? new float[] { firstKdR, firstKdG, firstKdB, Math.min(1f, firstFakeLight) }
                : null;

        ByteBuffer vb = MemoryUtil.memAlloc(totalVerts * strideBytes);
        IntBuffer ib = MemoryUtil.memAllocInt(totalIndices);
        IntBuffer vbInts = vb.asIntBuffer();

        boolean hasTransform = !rootTransform.isIdentity();
        Matrix4f posMat = hasTransform ? rootTransform.getMatrix() : null;
        Matrix3f nrmMat = hasTransform ? rootTransform.getNormalMatrix() : null;
        Vector4f posTmp = new Vector4f();
        Vector3f nrmTmp = new Vector3f();

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        int vertBase = 0;
        int idxCursor = 0;
        try {
            for (BoundSubMesh bound : subMeshes) {
                NMeshData.SubMesh sm = bound.subMesh();
                if (!hasTransform) {
                    if (sm.minX < minX) minX = sm.minX;
                    if (sm.minY < minY) minY = sm.minY;
                    if (sm.minZ < minZ) minZ = sm.minZ;
                    if (sm.maxX > maxX) maxX = sm.maxX;
                    if (sm.maxY > maxY) maxY = sm.maxY;
                    if (sm.maxZ > maxZ) maxZ = sm.maxZ;
                }
                TextureAtlasSprite sprite = bound.sprite();
                float u0 = 0f, v0 = 0f, du = 1f, dv = 1f;
                if (!normalizeUv && sprite != null) {
                    u0 = sprite.getU0();
                    v0 = sprite.getV0();
                    du = sprite.getU1() - u0;
                    dv = sprite.getV1() - v0;
                }

                for (int v = 0; v < sm.vertexCount; v++) {
                    float x = sm.posX(v);
                    float y = sm.posY(v);
                    float z = sm.posZ(v);
                    int nPacked = sm.packedNormal(v);
                    float rawU = sm.u(v);
                    float rawV = flipV ? (1.0f - sm.v(v)) : sm.v(v);

                    if (hasTransform) {
                        posTmp.set(x, y, z, 1.0f).mul(posMat);
                        x = posTmp.x;
                        y = posTmp.y;
                        z = posTmp.z;
                        nrmTmp.set(sm.normX(v), sm.normY(v), sm.normZ(v)).mul(nrmMat);
                        float len = nrmTmp.length();
                        if (len > 1e-6f) {
                            nPacked = NMeshData.SubMesh.packNormal(nrmTmp.x / len, nrmTmp.y / len, nrmTmp.z / len);
                        }

                        if (x < minX) minX = x; if (x > maxX) maxX = x;
                        if (y < minY) minY = y; if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
                    }

                    float u = (normalizeUv || sprite == null) ? rawU : (u0 + rawU * du);
                    float vt = (normalizeUv || sprite == null) ? rawV : (v0 + rawV * dv);

                    int outOff = (vertBase + v) * intsPerVert;
                    vbInts.put(outOff,     Float.floatToRawIntBits(x));
                    vbInts.put(outOff + 1, Float.floatToRawIntBits(y));
                    vbInts.put(outOff + 2, Float.floatToRawIntBits(z));
                    vbInts.put(outOff + 3, nPacked);
                    vbInts.put(outOff + 4, Float.floatToRawIntBits(u));
                    vbInts.put(outOff + 5, Float.floatToRawIntBits(vt));
                    vbInts.put(outOff + 6, perVertexBoneId);
                }

                int smICount = sm.indexCount();
                for (int i = 0; i < smICount; i++) {
                    ib.put(idxCursor++, vertBase + sm.index(i));
                }
                vertBase += sm.vertexCount;
            }

            vb.position(0).limit(totalVerts * strideBytes);
            ib.position(0).limit(totalIndices);
            if (!Float.isFinite(minX)) {
                minX = minY = minZ = 0f;
                maxX = maxY = maxZ = 0f;
            }
            return new SingleMeshVboRenderer.VboData(vb, ib, minX, minY, minZ, maxX, maxY, maxZ, strideBytes, defaultTint);
        } catch (Throwable t) {
            MemoryUtil.memFree(vb);
            MemoryUtil.memFree(ib);
            throw t;
        }
    }

    /**
     * Lazily materializes BakedQuads for item rendering (GUI/hand), IrisCompanionMesh, or DoorPartClipper.
     */
    public List<BakedQuad> getOrBuildQuads() {
        buildQuadsIfNeeded();
        return this.lazyQuads;
    }

    private void buildQuadsIfNeeded() {
        List<BakedQuad> q = this.lazyQuads;
        if (q != null) {
            return;
        }
        synchronized (this) {
            if (this.lazyQuads != null) {
                return;
            }
            List<BakedQuad> built = new ArrayList<>();
            Map<Direction, List<BakedQuad>> bySide = cullFaces ? new EnumMap<>(Direction.class) : null;
            List<BakedQuad> unculled = cullFaces ? new ArrayList<>() : null;
            boolean hasTransform = !rootTransform.isIdentity();
            Matrix4f posMat = hasTransform ? rootTransform.getMatrix() : null;
            Matrix3f nrmMat = hasTransform ? rootTransform.getNormalMatrix() : null;
            Vector4f posTmp = new Vector4f();
            Vector3f nrmTmp = new Vector3f();

            for (BoundSubMesh bound : subMeshes) {
                NMeshData.SubMesh sm = bound.subMesh();
                // Null sprite (programmatic pre-bake during ModelEvent.ModifyBakingResult, where the
                // block atlas is not yet registered and ModelManager.getMissingModel() is still null)
                // maps the raw [0,1] OBJ UVs straight through: standalone-texture models (weapons)
                // need exactly that, and it avoids a hard ModelManager dependency at bake time.
                TextureAtlasSprite sprite = bound.sprite();
                float u0 = 0f, v0 = 0f, du = 1f, dv = 1f;
                if (sprite != null) {
                    u0 = sprite.getU0();
                    v0 = sprite.getV0();
                    du = sprite.getU1() - u0;
                    dv = sprite.getV1() - v0;
                }

                // Forge ObjModel semantics (ObjModel.makeQuad, 1.20.1):
                // - vertex colour = the material's Kd diffuse colour (obj vertex colours are not stored);
                // - "emissive_ambient" (default true): MTL Ka drives a fake lightmap and kills shading;
                // - quad direction comes from the TRANSFORMED NORMAL OF VERTEX 0 (not an average);
                // - automatic culling assigns a cull face ONLY when all four vertices lie exactly on
                //   the block-boundary plane (Mth.equal, 1e-5) AND the vertex-0 normal points outward;
                //   every other face stays in the unculled (null-side) bucket.
                float kdR = sm.kdR, kdG = sm.kdG, kdB = sm.kdB;
                int colorPacked = (clampByte(kdR)) | (clampByte(kdG) << 8) | (clampByte(kdB) << 16) | (255 << 24);
                int uv2 = 0;
                boolean effectiveShade = shade;
                if (smoothShade) {
                    // The per-vertex colour below carries the directional diffuse; the per-quad
                    // shade flag would multiply the same weights a second time.
                    effectiveShade = false;
                }
                if (emissiveAmbient) {
                    int fakeLight = (int) ((sm.kaR + sm.kaG + sm.kaB) * 15 / 3.0f);
                    uv2 = LightTexture.pack(fakeLight, fakeLight);
                    effectiveShade = effectiveShade && (fakeLight == 0);
                }

                int smICount = sm.indexCount();
                for (int t = 0; t + 2 < smICount; t += 3) {
                    int[] quadData = new int[32];
                    int i0 = sm.index(t), i1 = sm.index(t + 1), i2 = sm.index(t + 2);
                    int[] tri = { i0, i1, i2, i2 }; // degenerate 4th vertex, same as Forge (Math.min(i, len-1))
                    float[] tx = new float[4], ty = new float[4], tz = new float[4];
                    float[] tnx = new float[4], tny = new float[4], tnz = new float[4];
                    for (int corner = 0; corner < 4; corner++) {
                        int vi = tri[corner];
                        float x = sm.posX(vi), y = sm.posY(vi), z = sm.posZ(vi);
                        float nx = sm.normX(vi), ny = sm.normY(vi), nz = sm.normZ(vi);
                        float rawU = sm.u(vi);
                        float rawV = flipV ? (1.0f - sm.v(vi)) : sm.v(vi);
                        int nPacked = sm.packedNormal(vi);
                        if (hasTransform) {
                            posTmp.set(x, y, z, 1.0f).mul(posMat);
                            x = posTmp.x; y = posTmp.y; z = posTmp.z;
                            nrmTmp.set(nx, ny, nz).mul(nrmMat);
                            float len = nrmTmp.length();
                            if (len > 1e-6f) { nx = nrmTmp.x / len; ny = nrmTmp.y / len; nz = nrmTmp.z / len; }
                            nPacked = NMeshData.SubMesh.packNormal(nx, ny, nz);
                        }
                        tx[corner] = x; ty[corner] = y; tz[corner] = z;
                        tnx[corner] = nx; tny[corner] = ny; tnz[corner] = nz;
                        // Smooth shading (vanilla face weights on the transformed TRUE normal,
                        // interpolated by the GPU across the triangle — no per-quad posterization).
                        int cornerColor = colorPacked;
                        if (smoothShade) {
                            float snx = tnx[corner], sny = tny[corner], snz = tnz[corner];
                            float slen = (float) Math.sqrt(snx * snx + sny * sny + snz * snz);
                            if (slen > 1e-6f) {
                                snx /= slen; sny /= slen; snz /= slen;
                            }
                            float n2x = snx * snx, n2y = sny * sny, n2z = snz * snz;
                            float diff = n2x * 0.6f + n2y * (sny > 0f ? 1f : 0.5f) + n2z * 0.8f;
                            cornerColor = (clampByte(kdR * diff)) | (clampByte(kdG * diff) << 8)
                                    | (clampByte(kdB * diff) << 16) | (255 << 24);
                        }
                        float u = u0 + rawU * du;
                        float v = v0 + rawV * dv;
                        int o = corner * 8;
                        quadData[o]     = Float.floatToRawIntBits(x);
                        quadData[o + 1] = Float.floatToRawIntBits(y);
                        quadData[o + 2] = Float.floatToRawIntBits(z);
                        quadData[o + 3] = cornerColor;
                        quadData[o + 4] = Float.floatToRawIntBits(u);
                        quadData[o + 5] = Float.floatToRawIntBits(v);
                        quadData[o + 6] = uv2;
                        quadData[o + 7] = nPacked;
                    }
                    Direction dir = Direction.getNearest(tnx[0], tny[0], tnz[0]);
                    BakedQuad quad = new BakedQuad(quadData, tintIndex, dir, sprite, effectiveShade);
                    built.add(quad);
                    if (bySide != null) {
                        Direction cull = automaticCullFace(tx, ty, tz, tnx, tny, tnz);
                        if (cull != null) {
                            bySide.computeIfAbsent(cull, d -> new ArrayList<>()).add(quad);
                        } else {
                            unculled.add(quad);
                        }
                    }
                }
            }
            this.lazyQuads = Collections.unmodifiableList(built);
            this.lazyQuadsBySide = bySide != null
                    ? Collections.unmodifiableMap(bySide)
                    : Collections.emptyMap();
            this.lazyQuadsUnculled = unculled != null
                    ? Collections.unmodifiableList(unculled)
                    : Collections.emptyList();
        }
    }

    /**
     * Lazily computes packed tangents (tx, ty, tz, tw as 4 signed bytes) when an Iris/Oculus
     * shaderpack requests {@code at_tangent}.
     */
    private static int[] computePackedTangents(NMeshData.SubMesh sm) {
        int vertexCount = sm.vertexCount;
        float[] tan1 = new float[vertexCount * 3];
        float[] tan2 = new float[vertexCount * 3];
        int iCount = sm.indexCount();
        for (int i = 0; i + 2 < iCount; i += 3) {
            int i0 = sm.index(i), i1 = sm.index(i + 1), i2 = sm.index(i + 2);
            float dx1 = sm.posX(i1) - sm.posX(i0), dy1 = sm.posY(i1) - sm.posY(i0), dz1 = sm.posZ(i1) - sm.posZ(i0);
            float dx2 = sm.posX(i2) - sm.posX(i0), dy2 = sm.posY(i2) - sm.posY(i0), dz2 = sm.posZ(i2) - sm.posZ(i0);
            float u0 = sm.u(i0), v0 = sm.v(i0);
            float du1 = sm.u(i1) - u0;
            // Minecraft uses flipped V (1.0 - V) at render time, so dv -> -dv
            float dv1 = -(sm.v(i1) - v0);
            float du2 = sm.u(i2) - u0;
            float dv2 = -(sm.v(i2) - v0);
            float det = du1 * dv2 - du2 * dv1;
            if (Math.abs(det) < 1e-8f) continue;
            float r = 1.0f / det;
            float sx = (dv2 * dx1 - dv1 * dx2) * r;
            float sy = (dv2 * dy1 - dv1 * dy2) * r;
            float sz = (dv2 * dz1 - dv1 * dz2) * r;
            float tx = (du1 * dx2 - du2 * dx1) * r;
            float ty = (du1 * dy2 - du2 * dy1) * r;
            float tz = (du1 * dz2 - du2 * dz1) * r;
            for (int k = 0; k < 3; k++) {
                int idx = sm.index(i + k);
                int tOff = idx * 3;
                tan1[tOff] += sx; tan1[tOff + 1] += sy; tan1[tOff + 2] += sz;
                tan2[tOff] += tx; tan2[tOff + 1] += ty; tan2[tOff + 2] += tz;
            }
        }
        int[] packed = new int[vertexCount];
        for (int v = 0; v < vertexCount; v++) {
            int tOff = v * 3;
            float nx = sm.normX(v), ny = sm.normY(v), nz = sm.normZ(v);
            float tx = tan1[tOff], ty = tan1[tOff + 1], tz = tan1[tOff + 2];
            float dot = nx * tx + ny * ty + nz * tz;
            tx -= nx * dot; ty -= ny * dot; tz -= nz * dot;
            float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (len > 1e-6f) {
                tx /= len; ty /= len; tz /= len;
            } else {
                tx = 1f; ty = 0f; tz = 0f;
            }
            float cx = ny * tz - nz * ty, cy = nz * tx - nx * tz, cz = nx * ty - ny * tx;
            float handedness = (cx * tan2[tOff] + cy * tan2[tOff + 1] + cz * tan2[tOff + 2]) < 0f ? -1f : 1f;
            int bx = ((int) Math.round(Math.max(-1f, Math.min(1f, tx)) * 127f)) & 0xFF;
            int by = ((int) Math.round(Math.max(-1f, Math.min(1f, ty)) * 127f)) & 0xFF;
            int bz = ((int) Math.round(Math.max(-1f, Math.min(1f, tz)) * 127f)) & 0xFF;
            int bw = (handedness < 0f ? -127 : 127) & 0xFF;
            packed[v] = bx | (by << 8) | (bz << 16) | (bw << 24);
        }
        return packed;
    }

    private static int clampByte(float v) {
        int i = (int) (v * 255f + 0.5f);
        return Math.max(0, Math.min(255, i)) & 0xFF;
    }

    /**
     * Forge {@code Transformation.blockCenterToCorner()}: re-bases a center-referenced transform
     * {@code M} so it can be applied raw to corner-referenced coords, i.e.
     * {@code v' = 0.5 + M(v - 0.5)}. Implemented inline with JOML because the Forge patch method
     * does not exist on the NeoForge Transformation class.
     */
    private static Transformation rebaseCenterToCorner(Transformation t) {
        if (t.isIdentity()) {
            return t;
        }
        Matrix4f m = new Matrix4f()
                .translate(0.5f, 0.5f, 0.5f)
                .mul(t.getMatrix())
                .translate(-0.5f, -0.5f, -0.5f);
        return new Transformation(m);
    }

    /**
     * Forge {@code ObjModel.makeQuad} automatic-culling chain (1.20.1): a face is culled only when
     * ALL four transformed vertices lie exactly on the block-boundary plane (Mth.equal, 1e-5) and
     * the vertex-0 normal points outward. Everything else returns null (unculled).
     */
    @Nullable
    private static Direction automaticCullFace(float[] tx, float[] ty, float[] tz,
                                               float[] tnx, float[] tny, float[] tnz) {
        if (equal4(tx, 0f) && tnx[0] < 0) return Direction.WEST;
        if (equal4(tx, 1f) && tnx[0] > 0) return Direction.EAST;
        if (equal4(tz, 0f) && tnz[0] < 0) return Direction.NORTH;
        if (equal4(tz, 1f) && tnz[0] > 0) return Direction.SOUTH;
        if (equal4(ty, 0f) && tny[0] < 0) return Direction.DOWN;
        if (equal4(ty, 1f) && tny[0] > 0) return Direction.UP;
        return null;
    }

    /** {@code Mth.equal(a, b)} is |a - b| < 1e-5; applied to all four vertices like Forge does. */
    private static boolean equal4(float[] v, float plane) {
        return Math.abs(v[0] - plane) < 1.0e-5f && Math.abs(v[1] - plane) < 1.0e-5f
                && Math.abs(v[2] - plane) < 1.0e-5f && Math.abs(v[3] - plane) < 1.0e-5f;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (cullFaces) {
            // "automatic_culling": true, Forge ObjModel semantics: culled faces live in per-direction
            // buckets; every face that does not sit exactly on a block-boundary plane stays in the
            // null (unculled) bucket so both the chunk renderer and ItemRenderer#renderModelLists
            // (which fetches the six Direction buckets and then the null one) draw it.
            buildQuadsIfNeeded();
            if (side == null) {
                return lazyQuadsUnculled;
            }
            List<BakedQuad> bucket = lazyQuadsBySide.get(side);
            return bucket != null ? bucket : Collections.emptyList();
        }
        if (side != null) {
            return Collections.emptyList();
        }
        return getOrBuildQuads();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        return getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() { return ambientOcclusion; }

    /** Honours the model JSON "render_type" hint for chunk rendering (e.g. minecraft:cutout). */
    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
                                                      @NotNull ModelData data) {
        if (blockRenderType != null) {
            return ChunkRenderTypeSet.of(blockRenderType);
        }
        return BakedModel.super.getRenderTypes(state, rand, data);
    }

    /** Honours the model JSON "render_type" hint for item rendering (entity-layer equivalent). */
    @Override
    public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
        if (itemRenderType != null) {
            return List.of(itemRenderType);
        }
        // IForgeBakedModel default (see above).
        return List.of(RenderTypeHelper.getFallbackItemRenderType(itemStack, this, fabulous));
    }

    @Override
    public boolean isGui3d() { return true; }

    @Override
    public boolean usesBlockLight() { return true; }

    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        if (particleSprite != null) {
            return particleSprite;
        }
        // getMissingModel() is null while the bake event (ModifyBakingResult) is still running.
        try {
            return Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
        } catch (NullPointerException | IllegalStateException e) {
            return null;
        }
    }

    @Override
    public ItemTransforms getTransforms() { return transforms; }

    @Override
    public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
}
