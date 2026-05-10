package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hbm_m.item.tags_and_tiers.ModTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelData.Builder;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Connected-textures модель (как CT в 1.7.10): для каждой грани выбираются 4 фрагмента (TL/TR/BL/BR)
 * на основе соседей вокруг этой грани. Данные о соединениях вычисляются в {@link #getModelData}.
 *
 * Важно: getQuads() не получает доступ к миру/позиции, поэтому мы используем Forge ModelData.
 */
public class ConnectedDecoSteelBakedModel extends BakedModelWrapper<BakedModel> {

    public static final ModelProperty<int[]> CT_INDICES = new ModelProperty<>();

    private final ResourceLocation fullTex;
    private final ResourceLocation ctTex;
    private TextureAtlasSprite fullSprite;
    private TextureAtlasSprite ctSprite;
    private final TextureAtlasSprite fallbackSprite;

    public ConnectedDecoSteelBakedModel(BakedModel original, ResourceLocation fullTex, ResourceLocation ctTex) {
        super(original);
        this.fullTex = fullTex;
        this.ctTex = ctTex;
        TextureAtlasSprite particle = null;
        try {
            particle = original.getParticleIcon(ModelData.EMPTY);
        } catch (Throwable ignored) {
        }
        this.fallbackSprite = particle;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            ModelData extraData, @Nullable RenderType renderType) {
        // Item / inventory render: используем оригинальную модель (без world-CT).
        if (state == null) {
            return originalModel.getQuads(null, side, rand, extraData, renderType);
        }

        // Первый вход в мир: atlas может быть ещё не прогрет. Делаем ленивое получение спрайтов,
        // чтобы CT включался сам без F3+T.
        if (!ensureSpritesReady()) {
            return originalModel.getQuads(state, side, rand, extraData, renderType);
        }

        int[] indices = extraData.get(CT_INDICES);
        if (indices == null || indices.length != 24) {
            return originalModel.getQuads(state, side, rand, extraData, renderType);
        }

        if (side == null) {
            List<BakedQuad> all = new ArrayList<>(24);
            for (Direction d : Direction.values()) {
                all.addAll(buildFaceQuads(d, indices));
            }
            return all;
        }

        return buildFaceQuads(side, indices);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (level == null || pos == null || state == null) {
            return modelData;
        }

        int[] indices = new int[24];
        for (Direction face : Direction.values()) {
            boolean[] cons = collectConnections(level, pos, face);

            // 0 1 2
            // 3   4
            // 5 6 7
            int itl = CTBits.T | CTBits.L | cornerType(cons[3], cons[0], cons[1]);
            int itr = CTBits.T | CTBits.R | cornerType(cons[4], cons[2], cons[1]);
            int ibl = CTBits.B | CTBits.L | cornerType(cons[3], cons[5], cons[6]);
            int ibr = CTBits.B | CTBits.R | cornerType(cons[4], cons[7], cons[6]);

            int base = face.ordinal() * 4;
            indices[base] = itl;
            indices[base + 1] = itr;
            indices[base + 2] = ibl;
            indices[base + 3] = ibr;
        }

        Builder b = ModelData.builder();
        b.with(CT_INDICES, indices);
        return b.build();
    }

    private List<BakedQuad> buildFaceQuads(Direction face, int[] indices) {
        int base = face.ordinal() * 4;
        int itl = indices[base];
        int itr = indices[base + 1];
        int ibl = indices[base + 2];
        int ibr = indices[base + 3];

        List<BakedQuad> out = new ArrayList<>(4);
        Vector3f[] corners = faceCorners(face);

        // В оригинале RenderBlocksCT.drawFace() делит грань на 4 под-грани через средние точки,
        // а затем drawSubFace() пишет вершины в ROTATIONAL порядке (FTR, FTL, FBL, FBR) с UV:
        // (maxU,minV), (minU,minV), (minU,maxV), (maxU,maxV).
        BakedQuad qTl = buildSubQuad(corners, SubFace.TL, face, spriteFor(itl), uvFor(itl));
        if (qTl != null) out.add(qTl);
        BakedQuad qTr = buildSubQuad(corners, SubFace.TR, face, spriteFor(itr), uvFor(itr));
        if (qTr != null) out.add(qTr);
        BakedQuad qBl = buildSubQuad(corners, SubFace.BL, face, spriteFor(ibl), uvFor(ibl));
        if (qBl != null) out.add(qBl);
        BakedQuad qBr = buildSubQuad(corners, SubFace.BR, face, spriteFor(ibr), uvFor(ibr));
        if (qBr != null) out.add(qBr);

        return out;
    }

    private enum SubFace { TL, TR, BL, BR }

    /**
     * Возвращает 4 угла грани (FTL/FTR/FBL/FBR) в 0..1 координатах,
     * в ТОЧНОСТИ как в оригинале {@code RenderBlocksCT} (лексический порядок).
     */
    private static Vector3f[] faceCorners(Direction face) {
        return switch (face) {
            case SOUTH -> new Vector3f[] {
                    new Vector3f(0f, 1f, 1f), // ftl
                    new Vector3f(1f, 1f, 1f), // ftr
                    new Vector3f(0f, 0f, 1f), // fbl
                    new Vector3f(1f, 0f, 1f)  // fbr
            };
            case NORTH -> new Vector3f[] {
                    new Vector3f(1f, 1f, 0f), // ftl
                    new Vector3f(0f, 1f, 0f), // ftr
                    new Vector3f(1f, 0f, 0f), // fbl
                    new Vector3f(0f, 0f, 0f)  // fbr
            };
            case EAST -> new Vector3f[] {
                    new Vector3f(1f, 1f, 1f), // ftl
                    new Vector3f(1f, 1f, 0f), // ftr
                    new Vector3f(1f, 0f, 1f), // fbl
                    new Vector3f(1f, 0f, 0f)  // fbr
            };
            case WEST -> new Vector3f[] {
                    new Vector3f(0f, 1f, 0f), // ftl
                    new Vector3f(0f, 1f, 1f), // ftr
                    new Vector3f(0f, 0f, 0f), // fbl
                    new Vector3f(0f, 0f, 1f)  // fbr
            };
            case UP -> new Vector3f[] {
                    new Vector3f(0f, 1f, 0f), // ftl
                    new Vector3f(1f, 1f, 0f), // ftr
                    new Vector3f(0f, 1f, 1f), // fbl
                    new Vector3f(1f, 1f, 1f)  // fbr
            };
            case DOWN -> new Vector3f[] {
                    new Vector3f(0f, 0f, 1f), // ftl
                    new Vector3f(1f, 0f, 1f), // ftr
                    new Vector3f(0f, 0f, 0f), // fbl
                    new Vector3f(1f, 0f, 0f)  // fbr
            };
        };
    }

    private static BakedQuad buildSubQuad(Vector3f[] face, SubFace sub, Direction dir,
                                          TextureAtlasSprite sprite, ModelHelper.UVBox uv) {
        if (sprite == null) return null;

        // ftl,ftr,fbl,fbr
        Vector3f ftl = face[0];
        Vector3f ftr = face[1];
        Vector3f fbl = face[2];
        Vector3f fbr = face[3];

        Vector3f ftc = avg(ftl, ftr);
        Vector3f fbc = avg(fbl, fbr);
        Vector3f fcl = avg(ftl, fbl);
        Vector3f fcr = avg(ftr, fbr);
        Vector3f fcc = avg(ftc, fbc);

        // Под-грани как в RenderBlocksCT.drawFace (лексический порядок углов)
        Vector3f sftl, sftr, sfbl, sfbr;
        switch (sub) {
            case TL -> { sftl = ftl; sftr = ftc; sfbl = fcl; sfbr = fcc; }
            case TR -> { sftl = ftc; sftr = ftr; sfbl = fcc; sfbr = fcr; }
            case BL -> { sftl = fcl; sftr = fcc; sfbl = fbl; sfbr = fbc; }
            case BR -> { sftl = fcc; sftr = fcr; sfbl = fbc; sfbr = fbr; }
            default -> { return null; }
        }

        float u0 = sprite.getU(uv.u0());
        float v0 = sprite.getV(uv.v0());
        float u1 = sprite.getU(uv.u1());
        float v1 = sprite.getV(uv.v1());

        // ROTATIONAL order (как в RenderBlocksCT.drawSubFace):
        // ftr, ftl, fbl, fbr
        QuadBakingVertexConsumer.Buffered b = new QuadBakingVertexConsumer.Buffered();
        b.setSprite(sprite);
        b.setDirection(dir);
        b.setHasAmbientOcclusion(true);
        var n = dir.step();

        put(b, n, sftr, u1, v0);
        put(b, n, sftl, u0, v0);
        put(b, n, sfbl, u0, v1);
        put(b, n, sfbr, u1, v1);

        return b.getQuad();
    }

    private static Vector3f avg(Vector3f a, Vector3f b) {
        return new Vector3f((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, (a.z + b.z) * 0.5f);
    }

    private static void put(QuadBakingVertexConsumer builder, org.joml.Vector3f normal, Vector3f p, float u, float v) {
        builder.vertex(p.x, p.y, p.z)
                .uv(u, v)
                .uv2(0, 0)
                .normal(normal.x(), normal.y(), normal.z())
                .color(-1)
                .endVertex();
    }

    private TextureAtlasSprite spriteFor(int type) {
        TextureAtlasSprite s = type < 4 ? fullSprite : ctSprite;
        if (s != null) return s;
        return fallbackSprite != null ? fallbackSprite : originalModel.getParticleIcon(ModelData.EMPTY);
    }

    private boolean ensureSpritesReady() {
        if (fullSprite != null && ctSprite != null) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getModelManager() == null) {
            return false;
        }
        var atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        var missing = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
        TextureAtlasSprite full = atlas.getSprite(fullTex);
        TextureAtlasSprite ct = atlas.getSprite(ctTex);
        if (full == null || ct == null || full == missing || ct == missing) {
            return false;
        }
        this.fullSprite = full;
        this.ctSprite = ct;
        return true;
    }

    private ModelHelper.UVBox uvFor(int type) {
        int sub = (type < 4) ? 2 : 4;
        float len = 16f / sub;

        float du = 0f;
        float dv = 0f;

        // coarse positioning for ct sprite (4x4): V/J is right half, H/J is bottom half
        if (type >= 4) {
            if (CTBits.isV(type) || CTBits.isJ(type)) du += len * 2f;
            if (CTBits.isH(type) || CTBits.isJ(type)) dv += len * 2f;
        }

        // fine positioning: L/R and T/B
        if (CTBits.isR(type)) du += len;
        if (CTBits.isB(type)) dv += len;

        return new ModelHelper.UVBox(du, dv, du + len, dv + len);
    }

    private static int cornerType(boolean hor, boolean corner, boolean vert) {
        if (vert && hor && corner) return CTBits.C;
        if (vert && hor) return CTBits.J;
        if (vert) return CTBits.V;
        if (hor) return CTBits.H;
        return CTBits.F;
    }

    private static boolean[] collectConnections(BlockAndTintGetter level, BlockPos pos, Direction face) {
        int[][] offsets = FaceOffsets.get(face);
        boolean[] cons = new boolean[8];
        for (int i = 0; i < 8; i++) {
            int[] o = offsets[i];
            BlockState neighbor = level.getBlockState(pos.offset(o[0], o[1], o[2]));
            cons[i] = neighbor != null && neighbor.is(ModTags.Blocks.DECO_STEEL_CONNECTABLE);
        }
        return cons;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return fallbackSprite != null ? fallbackSprite : originalModel.getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return fallbackSprite != null ? fallbackSprite : originalModel.getParticleIcon(data);
    }

    @Override
    public ItemOverrides getOverrides() {
        return originalModel.getOverrides();
    }

    @Override
    public ItemTransforms getTransforms() {
        return originalModel.getTransforms();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.solid());
    }

    private static final class CTBits {
        static final int L = 0;     // left
        static final int R = 1;     // right
        static final int T = 0;     // top
        static final int B = 2;     // bottom
        static final int F = 0;     // full/unconnected
        static final int C = 4;     // connected
        static final int J = 8;     // junction
        static final int H = 12;    // horizontal
        static final int V = 16;    // vertical

        static boolean isR(int i) { return (i & R) != 0; }
        static boolean isB(int i) { return (i & B) != 0; }
        static boolean isH(int i) { return i >= H && i < H + 4; }
        static boolean isV(int i) { return i >= V && i < V + 4; }
        static boolean isJ(int i) { return i >= J && i < J + 4; }
    }

    /**
     * Порт логики CTContext.access из 1.7.10: для каждой грани задаём "up" и "left"
     * в локальной системе координат грани, затем строим 8 соседей в лексическом порядке:
     *
     * 0 1 2
     * 3   4
     * 5 6 7
     */
    private static final class FaceOffsets {
        private static final Map<Direction, int[][]> CACHE = new EnumMap<>(Direction.class);

        static {
            CACHE.put(Direction.DOWN, lexicalOffsets(Direction.SOUTH, Direction.WEST));   // DOWN guess (как в 1.7.10)
            CACHE.put(Direction.UP, lexicalOffsets(Direction.NORTH, Direction.WEST));    // UP guess
            CACHE.put(Direction.NORTH, lexicalOffsets(Direction.UP, Direction.EAST));    // NORTH
            CACHE.put(Direction.SOUTH, lexicalOffsets(Direction.UP, Direction.WEST));    // SOUTH
            CACHE.put(Direction.WEST, lexicalOffsets(Direction.UP, Direction.NORTH));    // WEST
            CACHE.put(Direction.EAST, lexicalOffsets(Direction.UP, Direction.SOUTH));    // EAST
        }

        static int[][] get(Direction face) {
            int[][] v = CACHE.get(face);
            return v != null ? v : lexicalOffsets(Direction.UP, Direction.NORTH);
        }

        private static int[][] lexicalOffsets(Direction up, Direction left) {
            Direction down = up.getOpposite();
            Direction right = left.getOpposite();
            return new int[][] {
                    sum(up, left),      // 0 TL
                    sum(up),            // 1 TC
                    sum(up, right),     // 2 TR
                    sum(left),          // 3 CL
                    sum(right),         // 4 CR
                    sum(down, left),    // 5 BL
                    sum(down),          // 6 BC
                    sum(down, right),   // 7 BR
            };
        }

        private static int[] sum(Direction... dirs) {
            int x = 0, y = 0, z = 0;
            for (Direction d : dirs) {
                x += d.getStepX();
                y += d.getStepY();
                z += d.getStepZ();
            }
            return new int[] { x, y, z };
        }
    }
}

