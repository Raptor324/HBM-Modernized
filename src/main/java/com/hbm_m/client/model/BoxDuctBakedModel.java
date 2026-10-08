package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.hbm_m.block.network.BoxDuctBlock;
import com.hbm_m.block.network.BoxDuctGeometry;
import com.hbm_m.block.network.BoxDuctGeometry.Kind;
import com.hbm_m.block.network.BoxDuctGeometry.Part;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chunk-Modell der Kastenrohre, 1:1 nach {@code RenderBoxDuct}: jeder Teilquader wird wie mit
 * {@code RenderBlocks.renderStandardBlock} gezeichnet - alle sechs Seiten, Textur aus {@code getIcon} und die
 * UV-Drehungen ({@code uvRotateTop/Bottom/East/West/North/South}) der 1.7.10-Flaechenroutinen.
 */
public class BoxDuctBakedModel implements BakedModel {

    private final Kind kind;
    private final BakedModel original;
    private final Map<BlockState, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public BoxDuctBakedModel(Kind kind, BakedModel original) {
        this.kind = kind;
        this.original = original;
    }

    static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath("hbm_m", "block/" + name));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (side != null || state == null || !(state.getBlock() instanceof BoxDuctBlock)) return Collections.emptyList();
        return cache.computeIfAbsent(state, this::build);
    }

    private List<BakedQuad> build(BlockState s) {
        int meta = s.getValue(BoxDuctBlock.META);
        boolean pX = s.getValue(BoxDuctBlock.EAST), nX = s.getValue(BoxDuctBlock.WEST), pY = s.getValue(BoxDuctBlock.UP),
                nY = s.getValue(BoxDuctBlock.DOWN), pZ = s.getValue(BoxDuctBlock.SOUTH), nZ = s.getValue(BoxDuctBlock.NORTH);
        int tint = kind == Kind.FLUID ? 0 : -1;
        List<BakedQuad> out = new ArrayList<>();
        TextureAtlasSprite[] icons = new TextureAtlasSprite[6];
        for (int side = 0; side < 6; side++) icons[side] = sprite(BoxDuctGeometry.icon(kind, meta, side, pX, nX, pY, nY, pZ, nZ));
        for (Part p : BoxDuctGeometry.renderParts(kind, meta, pX, nX, pY, nY, pZ, nZ))
            for (int side = 0; side < 6; side++) out.add(face(p.minX(), p.minY(), p.minZ(), p.maxX(), p.maxY(), p.maxZ(), side, p.uvRotate(), icons[side], tint));
        return out;
    }

    /** {@code renderInventoryBlock}: gerades Stueck laengs Z, Stirnseiten mit der End-Textur. */
    static List<BakedQuad> inventory(Kind kind, int meta) {
        double[] s = BoxDuctGeometry.sizes(kind, meta);
        double lower = s[0], upper = s[1];
        String straight, end;
        if (kind == Kind.CABLE) {
            straight = "boxduct_cable_straight";
            end = "boxduct_cable_end_" + meta;
        } else {
            String base = kind == Kind.EXHAUST ? "boxduct_exhaust" : "boxduct_" + new String[] { "silver", "copper", "white" }[Math.abs(meta % 3)];
            straight = base + "_straight";
            end = base + "_end";
        }
        int[] rot = { 0, 0, 0, 0, 1, 2 };
        List<BakedQuad> out = new ArrayList<>();
        for (int side = 0; side < 6; side++)
            out.add(face(lower, lower, 0, upper, upper, 1, side, rot, sprite(side == 2 || side == 3 ? end : straight), -1));
        return out;
    }

    // ------------------------------------------------------------------------------------------------
    // RenderBlocks.renderFace*: UV (in Texturpixeln 0-16) und Eckreihenfolge wie in 1.7.10

    static BakedQuad face(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int side, int[] r, TextureAtlasSprite icon, int tint) {
        double[][] v = new double[4][];
        double d3, d4, d5, d6, d7, d8, d9, d10;
        Direction dir;
        switch (side) {
            case 0 -> { // YNeg, uvRotateBottom
                dir = Direction.DOWN;
                d3 = minX * 16; d4 = maxX * 16; d5 = minZ * 16; d6 = maxZ * 16;
                d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                int rot = r[0];
                if (rot == 2) {
                    d3 = minZ * 16; d5 = 16 - maxX * 16; d4 = maxZ * 16; d6 = 16 - minX * 16;
                    d9 = d5; d10 = d6; d7 = d3; d8 = d4; d5 = d6; d6 = d9;
                } else if (rot == 1) {
                    d3 = 16 - maxZ * 16; d5 = minX * 16; d4 = 16 - minZ * 16; d6 = maxX * 16;
                    d7 = d4; d8 = d3; d3 = d4; d4 = d8; d9 = d6; d10 = d5;
                } else if (rot == 3) {
                    d3 = 16 - minX * 16; d4 = 16 - maxX * 16; d5 = 16 - minZ * 16; d6 = 16 - maxZ * 16;
                    d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                }
                v[0] = new double[] { minX, minY, maxZ, d8, d10 };
                v[1] = new double[] { minX, minY, minZ, d3, d5 };
                v[2] = new double[] { maxX, minY, minZ, d7, d9 };
                v[3] = new double[] { maxX, minY, maxZ, d4, d6 };
            }
            case 1 -> { // YPos, uvRotateTop
                dir = Direction.UP;
                d3 = minX * 16; d4 = maxX * 16; d5 = minZ * 16; d6 = maxZ * 16;
                d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                int rot = r[1];
                if (rot == 1) {
                    d3 = minZ * 16; d5 = 16 - maxX * 16; d4 = maxZ * 16; d6 = 16 - minX * 16;
                    d9 = d5; d10 = d6; d7 = d3; d8 = d4; d5 = d6; d6 = d9;
                } else if (rot == 2) {
                    d3 = 16 - maxZ * 16; d5 = minX * 16; d4 = 16 - minZ * 16; d6 = maxX * 16;
                    d7 = d4; d8 = d3; d3 = d4; d4 = d8; d9 = d6; d10 = d5;
                } else if (rot == 3) {
                    d3 = 16 - minX * 16; d4 = 16 - maxX * 16; d5 = 16 - minZ * 16; d6 = 16 - maxZ * 16;
                    d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                }
                v[0] = new double[] { maxX, maxY, maxZ, d4, d6 };
                v[1] = new double[] { maxX, maxY, minZ, d7, d9 };
                v[2] = new double[] { minX, maxY, minZ, d3, d5 };
                v[3] = new double[] { minX, maxY, maxZ, d8, d10 };
            }
            case 2 -> { // ZNeg, uvRotateEast (RenderBlocksNT, Grund-U gespiegelt)
                dir = Direction.NORTH;
                double minU = 16 - minX * 16, maxU = 16 - maxX * 16, maxV = 16 - maxY * 16, minV = 16 - minY * 16;
                double minU2 = minU, maxU2 = maxU, maxV2 = maxV, minV2 = minV;
                int rot = r[2];
                if (rot == 2) {
                    maxU = minY * 16; minU = maxY * 16; maxV = 16 - minX * 16; minV = 16 - maxX * 16;
                    maxV2 = maxV; minV2 = minV; minU2 = maxU; maxU2 = minU; maxV = minV; minV = maxV2;
                } else if (rot == 1) {
                    maxU = 16 - maxY * 16; minU = 16 - minY * 16; maxV = maxX * 16; minV = minX * 16;
                    minU2 = minU; maxU2 = maxU; maxU = minU; minU = maxU2; maxV2 = minV; minV2 = maxV;
                } else if (rot == 3) {
                    maxU = 16 - minX * 16; minU = 16 - maxX * 16; maxV = maxY * 16; minV = minY * 16;
                    minU2 = minU; maxU2 = maxU; maxV2 = maxV; minV2 = minV;
                }
                v[0] = new double[] { minX, maxY, minZ, minU2, maxV2 };
                v[1] = new double[] { maxX, maxY, minZ, maxU, maxV };
                v[2] = new double[] { maxX, minY, minZ, maxU2, minV2 };
                v[3] = new double[] { minX, minY, minZ, minU, minV };
            }
            case 3 -> { // ZPos, uvRotateWest
                dir = Direction.SOUTH;
                d3 = minX * 16; d4 = maxX * 16; d5 = 16 - maxY * 16; d6 = 16 - minY * 16;
                d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                int rot = r[3];
                if (rot == 1) {
                    d3 = minY * 16; d6 = 16 - minX * 16; d4 = maxY * 16; d5 = 16 - maxX * 16;
                    d9 = d5; d10 = d6; d7 = d3; d8 = d4; d5 = d6; d6 = d9;
                } else if (rot == 2) {
                    d3 = 16 - maxY * 16; d5 = minX * 16; d4 = 16 - minY * 16; d6 = maxX * 16;
                    d7 = d4; d8 = d3; d3 = d4; d4 = d8; d9 = d6; d10 = d5;
                } else if (rot == 3) {
                    d3 = 16 - minX * 16; d4 = 16 - maxX * 16; d5 = maxY * 16; d6 = minY * 16;
                    d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                }
                v[0] = new double[] { minX, maxY, maxZ, d3, d5 };
                v[1] = new double[] { minX, minY, maxZ, d8, d10 };
                v[2] = new double[] { maxX, minY, maxZ, d4, d6 };
                v[3] = new double[] { maxX, maxY, maxZ, d7, d9 };
            }
            case 4 -> { // XNeg, uvRotateNorth
                dir = Direction.WEST;
                d3 = minZ * 16; d4 = maxZ * 16; d5 = 16 - maxY * 16; d6 = 16 - minY * 16;
                d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                int rot = r[4];
                if (rot == 1) {
                    d3 = minY * 16; d5 = 16 - maxZ * 16; d4 = maxY * 16; d6 = 16 - minZ * 16;
                    d9 = d5; d10 = d6; d7 = d3; d8 = d4; d5 = d6; d6 = d9;
                } else if (rot == 2) {
                    d3 = 16 - maxY * 16; d5 = minZ * 16; d4 = 16 - minY * 16; d6 = maxZ * 16;
                    d7 = d4; d8 = d3; d3 = d4; d4 = d8; d9 = d6; d10 = d5;
                } else if (rot == 3) {
                    d3 = 16 - minZ * 16; d4 = 16 - maxZ * 16; d5 = maxY * 16; d6 = minY * 16;
                    d7 = d4; d8 = d3; d9 = d5; d10 = d6;
                }
                v[0] = new double[] { minX, maxY, maxZ, d7, d9 };
                v[1] = new double[] { minX, maxY, minZ, d3, d5 };
                v[2] = new double[] { minX, minY, minZ, d8, d10 };
                v[3] = new double[] { minX, minY, maxZ, d4, d6 };
            }
            default -> { // XPos, uvRotateSouth (RenderBlocksNT)
                dir = Direction.EAST;
                double minU = 16 - minZ * 16, maxU = 16 - maxZ * 16, maxV = 16 - maxY * 16, minV = 16 - minY * 16;
                double minU2 = minU, maxU2 = maxU, maxV2 = maxV, minV2 = minV;
                int rot = r[5];
                if (rot == 2) {
                    maxU = minY * 16; maxV = 16 - minZ * 16; minU = maxY * 16; minV = 16 - maxZ * 16;
                    maxV2 = maxV; minV2 = minV; minU2 = maxU; maxU2 = minU; maxV = minV; minV = maxV2;
                } else if (rot == 1) {
                    maxU = 16 - maxY * 16; maxV = maxZ * 16; minU = 16 - minY * 16; minV = minZ * 16;
                    minU2 = minU; maxU2 = maxU; maxU = minU; minU = maxU2; maxV2 = minV; minV2 = maxV;
                } else if (rot == 3) {
                    maxU = 16 - minZ * 16; minU = 16 - maxZ * 16; maxV = maxY * 16; minV = minY * 16;
                    minU2 = minU; maxU2 = maxU; maxV2 = maxV; minV2 = minV;
                }
                v[0] = new double[] { maxX, minY, maxZ, maxU2, minV2 };
                v[1] = new double[] { maxX, minY, minZ, minU, minV };
                v[2] = new double[] { maxX, maxY, minZ, minU2, maxV2 };
                v[3] = new double[] { maxX, maxY, maxZ, maxU, maxV };
            }
        }
        return bake(dir, sortToFaceInfo(dir, v, minX, minY, minZ, maxX, maxY, maxZ), icon, tint);
    }

    /** Ecken in die Reihenfolge von {@link FaceInfo} bringen, damit die Umgebungsverdeckung die richtigen Ecken trifft. */
    private static double[][] sortToFaceInfo(Direction dir, double[][] v, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double[] bounds = new double[6];
        bounds[FaceInfo.Constants.MIN_X] = minX; bounds[FaceInfo.Constants.MIN_Y] = minY; bounds[FaceInfo.Constants.MIN_Z] = minZ;
        bounds[FaceInfo.Constants.MAX_X] = maxX; bounds[FaceInfo.Constants.MAX_Y] = maxY; bounds[FaceInfo.Constants.MAX_Z] = maxZ;
        FaceInfo info = FaceInfo.fromFacing(dir);
        double[][] out = new double[4][];
        for (int i = 0; i < 4; i++) {
            FaceInfo.VertexInfo vi = info.getVertexInfo(i);
            double x = bounds[vi.xFace], y = bounds[vi.yFace], z = bounds[vi.zFace];
            for (double[] c : v) if (c[0] == x && c[1] == y && c[2] == z) { out[i] = c; break; }
            if (out[i] == null) return v;
        }
        return out;
    }

    private static BakedQuad bake(Direction dir, double[][] v, TextureAtlasSprite icon, int tint) {
        int[] data = new int[32];
        int normal = ((dir.getStepX() * 127) & 0xFF) | (((dir.getStepY() * 127) & 0xFF) << 8) | (((dir.getStepZ() * 127) & 0xFF) << 16);
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            data[o] = Float.floatToRawIntBits((float) v[i][0]);
            data[o + 1] = Float.floatToRawIntBits((float) v[i][1]);
            data[o + 2] = Float.floatToRawIntBits((float) v[i][2]);
            data[o + 3] = -1;
            //? if < 1.21.1 {
            data[o + 4] = Float.floatToRawIntBits(icon.getU(v[i][3]));
            data[o + 5] = Float.floatToRawIntBits(icon.getV(v[i][4]));
            //?} else {
            /*// 1.21.1: getU/getV erwarten 0..1 statt 0..16
            data[o + 4] = Float.floatToRawIntBits(icon.getU((float) (v[i][3] / 16.0D)));
            data[o + 5] = Float.floatToRawIntBits(icon.getV((float) (v[i][4] / 16.0D)));
            *///?}
            data[o + 6] = 0;
            data[o + 7] = normal;
        }
        return new BakedQuad(data, tint, dir, icon, true);
    }

    // ------------------------------------------------------------------------------------------------

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sprite(kind == Kind.CABLE ? "boxduct_cable_straight" : kind == Kind.EXHAUST ? "boxduct_exhaust_straight" : "boxduct_silver_straight");
    }

    @Override public ItemTransforms getTransforms() { return original.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    // ------------------------------------------------------------------------------------------------

    /** Gegenstandsmodell: je Metadaten (BlockStateTag) das Inventar-Stueck. */
    public static class Item implements BakedModel {
        private final Kind kind;
        private final BakedModel original;
        private final Map<Integer, BakedModel> perMeta = new ConcurrentHashMap<>();
        private final ItemOverrides overrides;

        public Item(Kind kind, BakedModel original) {
            this.kind = kind;
            this.original = original;
            this.overrides = new ItemOverrides() {
                @Override
                public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                    return forMeta(BoxDuctBlock.metaOf(stack));
                }
            };
        }

        BakedModel forMeta(int meta) {
            int m = Math.max(0, Math.min(kind == Kind.CABLE ? 4 : 14, meta));
            return perMeta.computeIfAbsent(m, k -> new Fixed(inventory(kind, k), original));
        }

        @Override public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction side, RandomSource r) { return side == null ? forMeta(0).getQuads(s, null, r) : Collections.emptyList(); }
        @Override public boolean useAmbientOcclusion() { return true; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
        @Override public ItemTransforms getTransforms() { return original.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return overrides; }
    }

    private record Fixed(List<BakedQuad> quads, BakedModel original) implements BakedModel {
        @Override public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction side, RandomSource r) { return side == null ? quads : Collections.emptyList(); }
        @Override public boolean useAmbientOcclusion() { return true; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
        @Override public ItemTransforms getTransforms() { return original.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}
