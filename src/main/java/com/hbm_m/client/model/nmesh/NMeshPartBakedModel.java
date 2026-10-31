package com.hbm_m.client.model.nmesh;

import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
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
        this.subMeshes = List.copyOf(subMeshes);
        this.flipV = flipV;
        // Forge ObjModel.makeQuad semantics: the incoming ModelState/root transform is referenced on
        // the CENTER of the block, but OBJ-derived vertex coords are referenced on the CORNER, so the
        // transform must be re-based (blockCenterToCorner) before it is applied to raw vertices.
        // Without this, rotated/translated models shift by the rotation-pivot delta in GUI/hand.
        this.rootTransform = rebaseCenterToCorner(rootTransform != null ? rootTransform : Transformation.identity());
        this.transforms = transforms != null ? transforms : ItemTransforms.NO_TRANSFORMS;
        this.shade = shade;
        this.ambientOcclusion = ambientOcclusion;
        this.cullFaces = cullFaces;
        this.tintIndex = tintIndex;
        this.blockRenderType = blockRenderType;
        this.itemRenderType = itemRenderType;
        this.emissiveAmbient = emissiveAmbient;
        TextureAtlasSprite p = null;
        for (BoundSubMesh b : this.subMeshes) {
            if (b.sprite() != null) {
                p = b.sprite();
                break;
            }
        }
        this.particleSprite = p;
    }

    public boolean isEmpty() {
        for (BoundSubMesh b : subMeshes) {
            if (b.subMesh().vertexCount > 0 && b.subMesh().indices.length > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Direct binary-to-VBO compilation: bypasses BakedQuad allocation and vertex-welding hash tables.
     */
    public @Nullable SingleMeshVboRenderer.VboData buildDirectVboData(int perVertexBoneId, boolean normalizeUv) {
        int totalVerts = 0;
        int totalIndices = 0;
        for (BoundSubMesh b : subMeshes) {
            totalVerts += b.subMesh().vertexCount;
            totalIndices += b.subMesh().indices.length;
        }
        if (totalVerts == 0 || totalIndices == 0) {
            return null;
        }

        final int strideBytes = SingleMeshVboRenderer.MACHINE_PART_VERTEX_STRIDE_BYTES; // 36 bytes
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
                TextureAtlasSprite sprite = bound.sprite();
                float u0 = 0f, v0 = 0f, du = 1f, dv = 1f;
                if (!normalizeUv && sprite != null) {
                    u0 = sprite.getU0();
                    v0 = sprite.getV0();
                    du = sprite.getU1() - u0;
                    dv = sprite.getV1() - v0;
                }

                float[] verts = sm.vertices;
                for (int v = 0; v < sm.vertexCount; v++) {
                    int b = v * 8;
                    float x = verts[b];
                    float y = verts[b + 1];
                    float z = verts[b + 2];
                    float nx = verts[b + 3];
                    float ny = verts[b + 4];
                    float nz = verts[b + 5];
                    float rawU = verts[b + 6];
                    float rawV = flipV ? (1.0f - verts[b + 7]) : verts[b + 7];

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
                    }

                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;

                    float u = u0 + rawU * du;
                    float vt = v0 + rawV * dv;

                    int outOff = (vertBase + v) * 9;
                    vbInts.put(outOff,     Float.floatToRawIntBits(x));
                    vbInts.put(outOff + 1, Float.floatToRawIntBits(y));
                    vbInts.put(outOff + 2, Float.floatToRawIntBits(z));
                    vbInts.put(outOff + 3, Float.floatToRawIntBits(nx));
                    vbInts.put(outOff + 4, Float.floatToRawIntBits(ny));
                    vbInts.put(outOff + 5, Float.floatToRawIntBits(nz));
                    vbInts.put(outOff + 6, Float.floatToRawIntBits(u));
                    vbInts.put(outOff + 7, Float.floatToRawIntBits(vt));
                    vbInts.put(outOff + 8, perVertexBoneId);
                }

                int[] smIdx = sm.indices;
                for (int i = 0; i < smIdx.length; i++) {
                    ib.put(idxCursor++, vertBase + smIdx[i]);
                }
                vertBase += sm.vertexCount;
            }

            vb.position(0).limit(totalVerts * strideBytes);
            ib.position(0).limit(totalIndices);
            if (!Float.isFinite(minX)) {
                minX = minY = minZ = 0f;
                maxX = maxY = maxZ = 0f;
            }
            return new SingleMeshVboRenderer.VboData(vb, ib, minX, minY, minZ, maxX, maxY, maxZ, strideBytes);
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
                if (emissiveAmbient) {
                    int fakeLight = (int) ((sm.kaR + sm.kaG + sm.kaB) * 15 / 3.0f);
                    uv2 = LightTexture.pack(fakeLight, fakeLight);
                    effectiveShade = fakeLight == 0 && shade;
                }

                float[] verts = sm.vertices;
                int[] idx = sm.indices;
                for (int t = 0; t + 2 < idx.length; t += 3) {
                    int[] quadData = new int[32];
                    int i0 = idx[t], i1 = idx[t + 1], i2 = idx[t + 2];
                    int[] tri = { i0, i1, i2, i2 }; // degenerate 4th vertex, same as Forge (Math.min(i, len-1))
                    float[] tx = new float[4], ty = new float[4], tz = new float[4];
                    float[] tnx = new float[4], tny = new float[4], tnz = new float[4];
                    for (int corner = 0; corner < 4; corner++) {
                        int vi = tri[corner];
                        int b = vi * 8;
                        float x = verts[b], y = verts[b + 1], z = verts[b + 2];
                        float nx = verts[b + 3], ny = verts[b + 4], nz = verts[b + 5];
                        float rawU = verts[b + 6];
                        float rawV = flipV ? (1.0f - verts[b + 7]) : verts[b + 7];
                        if (hasTransform) {
                            posTmp.set(x, y, z, 1.0f).mul(posMat);
                            x = posTmp.x; y = posTmp.y; z = posTmp.z;
                            nrmTmp.set(nx, ny, nz).mul(nrmMat);
                            float len = nrmTmp.length();
                            if (len > 1e-6f) { nx = nrmTmp.x / len; ny = nrmTmp.y / len; nz = nrmTmp.z / len; }
                        }
                        tx[corner] = x; ty[corner] = y; tz[corner] = z;
                        tnx[corner] = nx; tny[corner] = ny; tnz[corner] = nz;
                        float u = u0 + rawU * du;
                        float v = v0 + rawV * dv;
                        int nPacked = (((int) Math.round(Math.max(-1f, Math.min(1f, nx)) * 127f)) & 0xFF)
                                | ((((int) Math.round(Math.max(-1f, Math.min(1f, ny)) * 127f)) & 0xFF) << 8)
                                | ((((int) Math.round(Math.max(-1f, Math.min(1f, nz)) * 127f)) & 0xFF) << 16);
                        int o = corner * 8;
                        quadData[o]     = Float.floatToRawIntBits(x);
                        quadData[o + 1] = Float.floatToRawIntBits(y);
                        quadData[o + 2] = Float.floatToRawIntBits(z);
                        quadData[o + 3] = colorPacked;
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
        // IForgeBakedModel default: unqualified `super.` cannot reach interface default methods.
        return ItemBlockRenderTypes.getRenderLayers(state);
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
