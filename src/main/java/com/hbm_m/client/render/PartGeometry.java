package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.system.MemoryUtil;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

/**
 * One {@link BakedModel#getQuads} pass for a multipart part: the shared quad list
 * (Iris / putBulkData) and construction of
 * {@link com.hbm_m.client.render.SingleMeshVboRenderer.VboData} from those same quads.
 * <p>
 * Supports both atlas-mapped machine parts (using {@code block_lit.vsh} and
 * {@link net.minecraft.client.renderer.texture.TextureAtlas#LOCATION_BLOCKS}) and
 * standalone-texture models (handheld weapons using {@code weapon_lit.vsh} and discrete PNGs)
 * via mathematical UV unmapping back to normalized [0.0, 1.0] coordinates.
 */
@OnlyIn(Dist.CLIENT)
public record PartGeometry(List<BakedQuad> solidQuads, boolean standalone) {

    public static final long BAKE_SEED = 42L;

    public static final PartGeometry EMPTY = new PartGeometry(List.of(), false);

    /**
     * Backward-compatible constructor defaulting {@code standalone} to false (atlas UVs).
     */
    public PartGeometry(List<BakedQuad> solidQuads) {
        this(solidQuads, false);
    }

    public PartGeometry {
        solidQuads = List.copyOf(solidQuads);
    }

    public boolean isEmpty() {
        return solidQuads.isEmpty();
    }

    /**
     * Collects solid quads with default atlas UV coordinates (for machines).
     * Returns {@link #EMPTY} when there is no geometry.
     */
    public static PartGeometry compile(BakedModel modelPart, String partName) {
        return compile(modelPart, partName, false);
    }

    public static PartGeometry compile(BakedModel modelPart) {
        return compile(modelPart, "unnamed", false);
    }

    public static PartGeometry compile(BakedModel modelPart, boolean standalone) {
        return compile(modelPart, "unnamed", standalone);
    }

    /**
     * Collects solid quads with explicit standalone/atlas UV mode.
     *
     * @param modelPart  baked model part to inspect
     * @param partName   debug name of the part
     * @param standalone if true, UVs will be un-mapped to normalized [0.0, 1.0] coordinates upon VBO compilation
     */
    public static PartGeometry compile(BakedModel modelPart, String partName, boolean standalone) {
        if (modelPart == null) {
            return EMPTY;
        }
        // Unwrap FRAPI wrappers (Continuity, Emissive, etc.) if present on this part
        modelPart = AbstractPartBasedRenderer.unwrapFabricForwardingModels(modelPart);
        List<BakedQuad> quads = collectSolidQuads(modelPart, partName);
        if (quads.isEmpty()) {
            MainRegistry.LOGGER.debug("PartGeometry: Part '{}' has NO QUADS, skipping", partName);
            return EMPTY;
        }
        MainRegistry.LOGGER.debug("PartGeometry: Part '{}' (standalone={}) - {} quads", partName, standalone, quads.size());
        return new PartGeometry(quads, standalone);
    }

    /**
     * Named factory for standalone models (firearms, handheld items) that sample discrete PNG textures.
     * Automatically sets {@code standalone = true}.
     */
    public static PartGeometry compileStandalone(BakedModel modelPart, String partName) {
        return compile(modelPart, partName, true);
    }

    public static PartGeometry compileStandalone(BakedModel modelPart) {
        return compile(modelPart, "unnamed", true);
    }

    /**
     * Deterministic face iteration (same RNG/seed on every getQuads call).
     */
    public static List<BakedQuad> collectSolidQuads(BakedModel modelPart, String partName) {
        if (modelPart == null) {
            return List.of();
        }
        List<BakedQuad> quads = new ArrayList<>();
        RandomSource random = RandomSource.create(BAKE_SEED);

        random.setSeed(BAKE_SEED);
        quads.addAll(com.hbm_m.platform.RenderHooks.getModelQuads(modelPart, null, null, random, RenderType.solid()));

        for (Direction direction : Direction.values()) {
            random.setSeed(BAKE_SEED);
            quads.addAll(com.hbm_m.platform.RenderHooks.getModelQuads(modelPart, null, direction, random, RenderType.solid()));
        }

        return quads.isEmpty() ? List.of() : Collections.unmodifiableList(quads);
    }

    /**
     * VBO built from the already-collected quads (no repeated getQuads).
     * Uses this instance's {@code standalone} flag to decide UV normalization.
     * {@code bone_id} in vertices = 0 (static / ring).
     */
    public SingleMeshVboRenderer.VboData toVboData(String partName) {
        return buildVboDataFromQuads(solidQuads, partName, 0, this.standalone);
    }

    /**
     * @param perVertexBoneId bone index for instanced GPU skinning (0=base, 1=lower arm, ...);
     *                        see {@code MachineAdvancedAssemblerRenderer} and {@code block_lit.vsh}.
     */
    public SingleMeshVboRenderer.VboData toVboData(String partName, int perVertexBoneId) {
        return buildVboDataFromQuads(solidQuads, partName, perVertexBoneId, this.standalone);
    }

    /**
     * Explicit override for UV normalization regardless of instance's {@code standalone} flag.
     */
    public SingleMeshVboRenderer.VboData toVboData(String partName, int perVertexBoneId, boolean normalizeUv) {
        return buildVboDataFromQuads(solidQuads, partName, perVertexBoneId, normalizeUv);
    }

    /**
     * Explicit convenience method to compile normalized VBO data for standalone textures.
     */
    public SingleMeshVboRenderer.VboData toVboDataNormalized(String partName) {
        return buildVboDataFromQuads(solidQuads, partName, 0, true);
    }

    /**
     * Explicit convenience method to compile normalized VBO data with bone index.
     */
    public SingleMeshVboRenderer.VboData toVboDataNormalized(String partName, int perVertexBoneId) {
        return buildVboDataFromQuads(solidQuads, partName, perVertexBoneId, true);
    }

    /**
     * Alias for {@link #toVboDataNormalized(String)} for loader/registry interoperability.
     */
    public SingleMeshVboRenderer.VboData toNormalizedVboData(String partName) {
        return toVboDataNormalized(partName);
    }

    /**
     * Alias for {@link #toVboDataNormalized(String, int)} for loader/registry interoperability.
     */
    public SingleMeshVboRenderer.VboData toNormalizedVboData(String partName, int perVertexBoneId) {
        return toVboDataNormalized(partName, perVertexBoneId);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuads(List<BakedQuad> quads, String partName) {
        return buildVboDataFromQuads(quads, partName, 0, false);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuads(List<BakedQuad> quads, String partName, int perVertexBoneId) {
        return buildVboDataFromQuads(quads, partName, perVertexBoneId, false);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuadsNormalized(List<BakedQuad> quads, String partName) {
        return buildVboDataFromQuads(quads, partName, 0, true);
    }

    public static SingleMeshVboRenderer.VboData buildVboDataFromQuadsNormalized(List<BakedQuad> quads, String partName, int perVertexBoneId) {
        return buildVboDataFromQuads(quads, partName, perVertexBoneId, true);
    }

    /**
     * Builds off-heap {@link SingleMeshVboRenderer.VboData} from baked quads.
     *
     * @param quads           list of baked quads to pack
     * @param partName        part identifier for logging
     * @param perVertexBoneId bone index for vertex layout
     * @param normalizeUv     if true, un-maps quad UVs from atlas sprite coordinates back to [0.0, 1.0]
     * @return populated off-heap VboData with 36-byte vertex stride, or null if no valid quads
     */
    public static SingleMeshVboRenderer.VboData buildVboDataFromQuads(List<BakedQuad> quads, String partName, int perVertexBoneId, boolean normalizeUv) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }

        final int quadCount = quads.size();
        final int vertexCount = quadCount * 4;
        final int indexCapacity = quadCount * 6;
        final int vertexStrideBytes = SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES;

        ByteBuffer vb = null;
        IntBuffer ib = null;
        int indexOffset = 0;

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        try {
            final int intsPerVert = vertexStrideBytes >>> 2; // 9 ints (36 bytes)
            int[] uniqueVerts = new int[vertexCount * intsPerVert];
            int[] validIndices = new int[indexCapacity];
            int tableSize = Integer.highestOneBit(Math.max(16, vertexCount * 2) - 1) << 1;
            int tableMask = tableSize - 1;
            int[] hashTable = new int[tableSize];
            java.util.Arrays.fill(hashTable, -1);
            int uniqueVertCount = 0;
            int validIndexCount = 0;
            int[] quadVi = new int[4];

            for (BakedQuad quad : quads) {
                int[] raw = quad.getVertices();
                if (raw == null || raw.length < 32) {
                    MainRegistry.LOGGER.warn("PartGeometry: Skipping malformed quad in part '{}' (vertex array length = {})",
                            partName, raw == null ? -1 : raw.length);
                    continue;
                }

                // Compute sprite un-mapping parameters once per quad if normalization is requested
                float u0 = 0f, v0 = 0f, invDu = 0f, invDv = 0f;
                boolean shouldNormalize = false;
                if (normalizeUv) {
                    TextureAtlasSprite sprite = quad.getSprite();
                    if (sprite != null) {
                        u0 = sprite.getU0();
                        v0 = sprite.getV0();
                        float du = sprite.getU1() - u0;
                        float dv = sprite.getV1() - v0;
                        if (Math.abs(du) > 1e-7f && Math.abs(dv) > 1e-7f) {
                            invDu = 1.0f / du;
                            invDv = 1.0f / dv;
                            shouldNormalize = true;
                        }
                    }
                }

                for (int i = 0; i < 4; i++) {
                    int base = i * 8;

                    float x = Float.intBitsToFloat(raw[base + 0]);
                    float y = Float.intBitsToFloat(raw[base + 1]);
                    float z = Float.intBitsToFloat(raw[base + 2]);

                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;

                    float u = Float.intBitsToFloat(raw[base + 4]);
                    float v = Float.intBitsToFloat(raw[base + 5]);

                    if (shouldNormalize) {
                        u = (u - u0) * invDu;
                        v = (v - v0) * invDv;
                    }

                    int normalPacked = raw[base + 7];
                    float nx = ((byte) (normalPacked & 0xFF)) / 127.0f;
                    float ny = ((byte) ((normalPacked >> 8) & 0xFF)) / 127.0f;
                    float nz = ((byte) ((normalPacked >> 16) & 0xFF)) / 127.0f;

                    float lenSq = nx * nx + ny * ny + nz * nz;
                    if (lenSq > 1e-6f && Math.abs(lenSq - 1.0f) > 1e-3f) {
                        float invLen = (float) (1.0 / Math.sqrt(lenSq));
                        nx *= invLen;
                        ny *= invLen;
                        nz *= invLen;
                    }

                    int w0 = Float.floatToRawIntBits(x);
                    int w1 = Float.floatToRawIntBits(y);
                    int w2 = Float.floatToRawIntBits(z);
                    int w3 = Float.floatToRawIntBits(nx);
                    int w4 = Float.floatToRawIntBits(ny);
                    int w5 = Float.floatToRawIntBits(nz);
                    int w6 = Float.floatToRawIntBits(u);
                    int w7 = Float.floatToRawIntBits(v);
                    int w8 = perVertexBoneId;

                    int h = w0 * 31 + w1;
                    h = h * 31 + w2;
                    h = h * 31 + w3;
                    h = h * 31 + w4;
                    h = h * 31 + w5;
                    h = h * 31 + w6;
                    h = h * 31 + w7;
                    h = h * 31 + w8;
                    h ^= (h >>> 16);

                    int slot = h & tableMask;
                    int foundIdx = -1;
                    while (true) {
                        int cand = hashTable[slot];
                        if (cand < 0) {
                            foundIdx = uniqueVertCount++;
                            int vo = foundIdx * intsPerVert;
                            uniqueVerts[vo + 0] = w0;
                            uniqueVerts[vo + 1] = w1;
                            uniqueVerts[vo + 2] = w2;
                            uniqueVerts[vo + 3] = w3;
                            uniqueVerts[vo + 4] = w4;
                            uniqueVerts[vo + 5] = w5;
                            uniqueVerts[vo + 6] = w6;
                            uniqueVerts[vo + 7] = w7;
                            uniqueVerts[vo + 8] = w8;
                            hashTable[slot] = foundIdx;
                            break;
                        }
                        int vo = cand * intsPerVert;
                        if (uniqueVerts[vo + 0] == w0 && uniqueVerts[vo + 1] == w1 && uniqueVerts[vo + 2] == w2
                                && uniqueVerts[vo + 3] == w3 && uniqueVerts[vo + 4] == w4 && uniqueVerts[vo + 5] == w5
                                && uniqueVerts[vo + 6] == w6 && uniqueVerts[vo + 7] == w7 && uniqueVerts[vo + 8] == w8) {
                            foundIdx = cand;
                            break;
                        }
                        slot = (slot + 1) & tableMask;
                    }
                    quadVi[i] = foundIdx;
                }

                int i0 = quadVi[0], i1 = quadVi[1], i2 = quadVi[2], i3 = quadVi[3];
                if (i0 != i1 && i1 != i2 && i2 != i0) {
                    validIndices[validIndexCount++] = i0;
                    validIndices[validIndexCount++] = i1;
                    validIndices[validIndexCount++] = i2;
                }
                if (i2 != i3 && i3 != i0 && i0 != i2) {
                    validIndices[validIndexCount++] = i2;
                    validIndices[validIndexCount++] = i3;
                    validIndices[validIndexCount++] = i0;
                }
                indexOffset += 4;
            }

            if (uniqueVertCount == 0 || validIndexCount == 0) {
                MainRegistry.LOGGER.debug("PartGeometry: Part '{}' produced no valid quads for VBO", partName);
                return null;
            }

            vb = MemoryUtil.memAlloc(uniqueVertCount * vertexStrideBytes);
            ib = MemoryUtil.memAllocInt(validIndexCount);
            vb.asIntBuffer().put(uniqueVerts, 0, uniqueVertCount * intsPerVert);
            vb.position(0).limit(uniqueVertCount * vertexStrideBytes);
            ib.put(validIndices, 0, validIndexCount);
            ib.flip();

            if (!Float.isFinite(minX)) {
                minX = minY = minZ = 0f;
                maxX = maxY = maxZ = 0f;
            }

            MainRegistry.LOGGER.debug("PartGeometry VBO: {} vertices, {} indices, bbox min({},{},{}) max({},{},{}), normalizedUv={}",
                    indexOffset, ib.remaining(), minX, minY, minZ, maxX, maxY, maxZ, normalizeUv);
            return new SingleMeshVboRenderer.VboData(vb, ib, minX, minY, minZ, maxX, maxY, maxZ, vertexStrideBytes);

        } catch (Exception | OutOfMemoryError e) {
            if (vb != null) MemoryUtil.memFree(vb);
            if (ib != null) MemoryUtil.memFree(ib);
            throw e;
        }
    }
}
