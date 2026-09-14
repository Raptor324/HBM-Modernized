package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.block.network.BoxCableBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?} else if neoforge {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

/**
 * Порт RenderBoxDuct (1.7.10) для PowerCableBox: короб рисуется КАК НЕСКОЛЬКО боксов —
 * ядро + рукава к каждому подключению (не один растянутый бокс!), каждая грань — спрайт
 * по 6-битной маске подключений (копия PowerCableBox.getIcon) с uvRotate-поворотами
 * из оригинала (код 1 = 90°, 2 = 270°, 3 = 180°).
 * Грань строго по параметру side; side == null при состоянии блока = пусто (квады уже
 * разложены по сторонам), иначе будет двойная отрисовка.
 */
public class BoxCableBakedModel implements BakedModel {

    private static final ResourceLocation TEX_STRAIGHT = rl("boxduct_cable_straight");
    private static final ResourceLocation TEX_JUNCTION = rl("boxduct_cable_junction");
    private static final ResourceLocation[] TEX_END = {
            rl("boxduct_cable_end_0"), rl("boxduct_cable_end_1"), rl("boxduct_cable_end_2"),
            rl("boxduct_cable_end_3"), rl("boxduct_cable_end_4")
    };
    private static final ResourceLocation TEX_CURVE_TL = rl("boxduct_cable_curve_tl");
    private static final ResourceLocation TEX_CURVE_TR = rl("boxduct_cable_curve_tr");
    private static final ResourceLocation TEX_CURVE_BL = rl("boxduct_cable_curve_bl");
    private static final ResourceLocation TEX_CURVE_BR = rl("boxduct_cable_curve_br");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath("hbm_m", "block/" + path);
    }

    private final int size;
    private final ItemTransforms transforms;
    private TextureAtlasSprite straight;
    private TextureAtlasSprite junction;
    private final TextureAtlasSprite[] ends = new TextureAtlasSprite[5];
    private TextureAtlasSprite curveTL, curveTR, curveBL, curveBR;
    private TextureAtlasSprite particle;
    private boolean spritesReady;

    public BoxCableBakedModel(int size, ItemTransforms transforms) {
        this.size = size;
        this.transforms = transforms;
    }

    private boolean ensureSprites() {
        if (spritesReady) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getModelManager() == null) return false;
        var atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        var missing = atlas.getSprite(MissingTextureAtlasSprite.getLocation());
        straight = atlas.getSprite(TEX_STRAIGHT);
        junction = atlas.getSprite(TEX_JUNCTION);
        curveTL = atlas.getSprite(TEX_CURVE_TL);
        curveTR = atlas.getSprite(TEX_CURVE_TR);
        curveBL = atlas.getSprite(TEX_CURVE_BL);
        curveBR = atlas.getSprite(TEX_CURVE_BR);
        for (int i = 0; i < 5; i++) ends[i] = atlas.getSprite(TEX_END[i]);
        particle = straight;
        spritesReady = straight != null && junction != null && straight != missing;
        return spritesReady;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (side == null) {
            // Предмет (state == null) — все грани одним списком; блок в чанке — пусто,
            // все квады уже отданы в своих side-группах (иначе двойной рендер).
            if (state != null) return List.of();
        }
        if (!ensureSprites()) return List.of();

        List<BakedQuad> out = new ArrayList<>(12);
        double lower = boundsLower(), upper = 1 - lower;

        if (state == null || !state.hasProperty(BoxCableBlock.SIZE)) {
            // Инвентарный вид (RenderBoxDuct.renderInventoryBlock): прогон по Z, торцы = end[size].
            // ВНИМАНИЕ: в Forge 1.7.10 имена uvRotate-полей НЕ совпадают с гранями
            // (проверено по байткоду RenderBlocks): uvRotateNorth крутит WEST(XNeg),
            // uvRotateSouth — EAST(XPos), uvRotateEast — NORTH(ZNeg), uvRotateWest — SOUTH(ZPos).
            // Оригинал: uvRotateNorth=1, uvRotateSouth=2 → реально WEST=90°, EAST=270°
            // (те же повороты торцов, что в мировом Z-прогоне).
            box(out, lower, lower, 0, upper, upper, 1, side, state, 0, 0, 270, 90, 0, 0,
                    (d, st) -> (d == Direction.NORTH || d == Direction.SOUTH) ? ends[size] : straight);
            return out;
        }

        boolean nX = state.getValue(WireBlock.WEST), pX = state.getValue(WireBlock.EAST);
        boolean nY = state.getValue(WireBlock.DOWN), pY = state.getValue(WireBlock.UP);
        boolean nZ = state.getValue(WireBlock.NORTH), pZ = state.getValue(WireBlock.SOUTH);
        int mask = BoxCableBlock.connectionMask(state);
        int count = (pX ? 1 : 0) + (nX ? 1 : 0) + (pY ? 1 : 0) + (nY ? 1 : 0) + (pZ ? 1 : 0) + (nZ ? 1 : 0);

        // Копия RenderBoxDuct.renderWorldBlock: ветки + uvRotate (1=90°, 2=270°, 3=180°)
        if ((mask & 0b001111) == 0 && mask > 0) {
            // Straight along X: длинные грани (top/bottom/N/S) — полоса вдоль провода (поворот 90°
            // транспонирует окно: вдоль = полный текстурный v без стыков, поперёк = профиль с бортиками).
            // Торцы E/W — end, повороты как в оригинале (east=2, west=1).
            box(out, 0, lower, lower, 1, upper, upper, side, state, 90, 90, 270, 90, 90, 90, this::iconFor);
        } else if ((mask & 0b111100) == 0 && mask > 0) {
            // Straight along Z: длинные грани E/W — 90°, торцы N/S — end (north=1, south=2)
            box(out, lower, lower, 0, upper, upper, 1, side, state, 0, 0, 90, 90, 270, 90, this::iconFor);
        } else if ((mask & 0b110011) == 0 && mask > 0) {
            // Straight along Y
            box(out, lower, 0, lower, upper, 1, upper, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
        } else if (count == 2) {
            // Curve: ядро + рукава (РУКАВА РИСУЮТСЯ ЦЕЛИКОМ — их грани не копланарны граням ядра,
            // ядро 2..14, рукав в полосе до края блока: они только соседствуют рёбрами);
            // повороты зависят от осей поворота (общие для ядра и рукавов)
            int rotTop = 0, rotBottom = 0, rotN = 0, rotS = 0, rotE = 0, rotW = 0;
            if ((nY || pY) && (pX || nX)) {
                rotTop = 90;
                rotBottom = 90;
            } else if (!nY && !pY) {
                rotN = 90;
                rotS = 270;
                rotE = 270;
                rotW = 90;
            }
            box(out, lower, lower, lower, upper, upper, upper, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
            if (nY) box(out, lower, 0, lower, upper, lower, upper, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
            if (pY) box(out, lower, upper, lower, upper, 1, upper, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
            if (nX) box(out, 0, lower, lower, lower, upper, upper, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
            if (pX) box(out, upper, lower, lower, 1, upper, upper, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
            if (nZ) box(out, lower, lower, 0, upper, upper, lower, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
            if (pZ) box(out, lower, lower, upper, upper, upper, 1, side, state, rotTop, rotBottom, rotE, rotW, rotN, rotS, this::iconFor);
        } else {
            // Junction: ядро + рукава
            box(out, lower, lower, lower, upper, upper, upper, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
            if (nY) box(out, lower, 0, lower, upper, lower, upper, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
            if (pY) box(out, lower, upper, lower, upper, 1, upper, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
            if (nX) box(out, 0, lower, lower, lower, upper, upper, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
            if (pX) box(out, upper, lower, lower, 1, upper, upper, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
            if (nZ) box(out, lower, lower, 0, upper, upper, lower, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
            if (pZ) box(out, lower, lower, upper, upper, upper, 1, side, state, 0, 0, 0, 0, 0, 0, this::iconFor);
        }
        return out;
    }

    private interface IconFn {
        TextureAtlasSprite get(Direction dir, BlockState state);
    }

    private double boundsLower() {
        return 0.125D + 0.0625D * size;
    }

    /** Один бокс: грань side (или все для side==null в инвентаре) с поворотом UV в градусах по граням (top,bottom,east,west,north,south). */
    private void box(List<BakedQuad> out, double x0, double y0, double z0, double x1, double y1, double z1,
                     @Nullable Direction side, @Nullable BlockState state,
                     int rotTop, int rotBottom, int rotEast, int rotWest, int rotNorth, int rotSouth,
                     IconFn icons) {
        box(out, x0, y0, z0, x1, y1, z1, side, state, rotTop, rotBottom, rotEast, rotWest, rotNorth, rotSouth, null, icons);
    }

    /** Как выше, но с ограничением набора рисуемых граней (для рукавов: исключаем грани, совпадающие с гранями ядра). */
    private void box(List<BakedQuad> out, double x0, double y0, double z0, double x1, double y1, double z1,
                     @Nullable Direction side, @Nullable BlockState state,
                     int rotTop, int rotBottom, int rotEast, int rotWest, int rotNorth, int rotSouth,
                     @Nullable java.util.Set<Direction> draw, IconFn icons) {
        if (side == null) {
            for (Direction d : Direction.values()) {
                if (draw != null && !draw.contains(d)) continue;
                face(out, d, x0, y0, z0, x1, y1, z1, rotationFor(d, rotTop, rotBottom, rotEast, rotWest, rotNorth, rotSouth), icons.get(d, state));
            }
            return;
        }
        if (draw != null && !draw.contains(side)) return;
        face(out, side, x0, y0, z0, x1, y1, z1, rotationFor(side, rotTop, rotBottom, rotEast, rotWest, rotNorth, rotSouth), icons.get(side, state));
    }

    private static int rotationFor(Direction d, int rotTop, int rotBottom, int rotEast, int rotWest, int rotNorth, int rotSouth) {
        return switch (d) {
            case UP -> rotTop;
            case DOWN -> rotBottom;
            case EAST -> rotEast;
            case WEST -> rotWest;
            case NORTH -> rotNorth;
            case SOUTH -> rotSouth;
        };
    }

    /** Точная копия PowerCableBox.getIcon (side-нумерация 1.7.10: 0=down 1=up 2=north 3=south 4=west 5=east). */
    private TextureAtlasSprite iconFor(Direction dir, BlockState state) {
        boolean nX = state.getValue(WireBlock.WEST), pX = state.getValue(WireBlock.EAST);
        boolean nY = state.getValue(WireBlock.DOWN), pY = state.getValue(WireBlock.UP);
        boolean nZ = state.getValue(WireBlock.NORTH), pZ = state.getValue(WireBlock.SOUTH);
        int mask = BoxCableBlock.connectionMask(state);
        int count = (pX ? 1 : 0) + (nX ? 1 : 0) + (pY ? 1 : 0) + (nY ? 1 : 0) + (pZ ? 1 : 0) + (nZ ? 1 : 0);
        int side = dir.ordinal();
        TextureAtlasSprite end = ends[size];

        if ((mask & 0b001111) == 0 && mask > 0) {
            return (side == 4 || side == 5) ? end : straight;
        } else if ((mask & 0b111100) == 0 && mask > 0) {
            return (side == 2 || side == 3) ? end : straight;
        } else if ((mask & 0b110011) == 0 && mask > 0) {
            return (side == 0 || side == 1) ? end : straight;
        }

        if (side == 0 && nY || side == 1 && pY || side == 2 && nZ || side == 3 && pZ || side == 4 && nX || side == 5 && pX)
            return end;

        if (count == 2) {
            if (side == 1 && nY || side == 0 && pY || side == 3 && nZ || side == 2 && pZ || side == 5 && nX || side == 4 && pX)
                return straight;

            if (nY && pZ) return side == 4 ? curveBR : curveBL;
            if (nY && nZ) return side == 5 ? curveBR : curveBL;
            if (nY && pX) return side == 3 ? curveBR : curveBL;
            if (nY && nX) return side == 2 ? curveBR : curveBL;
            if (pY && pZ) return side == 4 ? curveTR : curveTL;
            if (pY && nZ) return side == 5 ? curveTR : curveTL;
            if (pY && pX) return side == 3 ? curveTR : curveTL;
            if (pY && nX) return side == 2 ? curveTR : curveTL;

            if (pX && nZ) return curveTR;
            if (pX && pZ) return curveBR;
            if (nX && nZ) return curveTL;
            if (nX && pZ) return curveBL;
        }

        return junction;
    }

    // --- Квады: конвенция углов из ConnectedDecoBlockBakedModel (FTL/FTR/FBL/FBR per face) ---

    private void face(List<BakedQuad> out, Direction dir,
                      double x0, double y0, double z0, double x1, double y1, double z1,
                      int rotation, TextureAtlasSprite sprite) {
        Vector3f[] c = corners(dir, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1);
        float su0 = sprite.getU0(), su1 = sprite.getU1(), sv0 = sprite.getV0(), sv1 = sprite.getV1();

        // 1:1 с 1.7.10 RenderBlocks: иконка натягивается на ВЕСЬ блок (0..16 текселей),
        // uvRotate вращает координаты вокруг ЦЕНТРА ГРАНИ БЛОКА (8,8), а не вокруг центра
        // окна грани — bounds лишь выбирают, какой кусок повернутой текстуры сэмплировать.
        // Поворот вокруг центра окна (как было раньше) ломает UV на неквадратных гранях
        // рукавов угловых контактов (например 2x12 текселей у верха X-рукава).
        float[][] uv = new float[4][2];
        for (int i = 0; i < 4; i++) {
            Vector3f p = c[i];
            float u, v;
            switch (dir) {
                case SOUTH -> { u = p.x * 16; v = 16 - p.y * 16; }
                case NORTH -> { u = 16 - p.x * 16; v = 16 - p.y * 16; }
                case EAST -> { u = 16 - p.z * 16; v = 16 - p.y * 16; }
                case WEST -> { u = p.z * 16; v = 16 - p.y * 16; }
                case UP -> { u = p.x * 16; v = p.z * 16; }
                default -> { u = p.x * 16; v = 16 - p.z * 16; } // DOWN
            }
            // uvRotate (1.7.10): 1=90°, 2=270°, 3=180° — поворот сэмплирования вокруг (8,8)
            float ru = u, rv = v;
            switch (rotation) {
                case 90 -> { ru = v; rv = 16 - u; }
                case 270 -> { ru = 16 - v; rv = u; }
                case 180 -> { ru = 16 - u; rv = 16 - v; }
            }
            uv[i][0] = net.minecraft.util.Mth.lerp(ru / 16f, su0, su1);
            uv[i][1] = net.minecraft.util.Mth.lerp(rv / 16f, sv0, sv1);
        }

        int[] data = new int[32];
        putVertex(data, 0, c[1], uv[1][0], uv[1][1], dir); // FTR
        putVertex(data, 1, c[0], uv[0][0], uv[0][1], dir); // FTL
        putVertex(data, 2, c[2], uv[2][0], uv[2][1], dir); // FBL
        putVertex(data, 3, c[3], uv[3][0], uv[3][1], dir); // FBR
        out.add(new BakedQuad(data, -1, dir, sprite, true));

        // Внутренняя поверхность (реверс той же грани): текстуры короба имеют вырезанные поля,
        // сквозь них должна быть видна стенка короба, а не пустота/небо (в 1.7.10 solid-пасс
        // шёл с alpha-тестом, и вырезы просвечивали на фоне окружения).
        int[] rev = new int[32];
        int[] perm = {0, 3, 2, 1};
        for (int vi = 0; vi < 4; vi++) {
            System.arraycopy(data, perm[vi] * 8, rev, vi * 8, 8);
        }
        out.add(new BakedQuad(rev, -1, dir, sprite, true));
    }

    private static Vector3f[] corners(Direction face, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case SOUTH -> new Vector3f[] {
                    new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1),
                    new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1)
            };
            case NORTH -> new Vector3f[] {
                    new Vector3f(x1, y1, z0), new Vector3f(x0, y1, z0),
                    new Vector3f(x1, y0, z0), new Vector3f(x0, y0, z0)
            };
            case EAST -> new Vector3f[] {
                    new Vector3f(x1, y1, z1), new Vector3f(x1, y1, z0),
                    new Vector3f(x1, y0, z1), new Vector3f(x1, y0, z0)
            };
            case WEST -> new Vector3f[] {
                    new Vector3f(x0, y1, z0), new Vector3f(x0, y1, z1),
                    new Vector3f(x0, y0, z0), new Vector3f(x0, y0, z1)
            };
            case UP -> new Vector3f[] {
                    new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0),
                    new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1)
            };
            case DOWN -> new Vector3f[] {
                    new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1),
                    new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0)
            };
        };
    }

    private static void putVertex(int[] data, int vertexIndex, Vector3f p, float u, float v, Direction dir) {
        int i = vertexIndex * 8;
        data[i] = Float.floatToRawIntBits(p.x);
        data[i + 1] = Float.floatToRawIntBits(p.y);
        data[i + 2] = Float.floatToRawIntBits(p.z);
        data[i + 3] = -1; // Color (white/opaque)
        data[i + 4] = Float.floatToRawIntBits(u);
        data[i + 5] = Float.floatToRawIntBits(v);
        data[i + 6] = 0; // UV2
        int nx = (byte) (dir.getStepX() * 127) & 0xFF;
        int ny = (byte) (dir.getStepY() * 127) & 0xFF;
        int nz = (byte) (dir.getStepZ() * 127) & 0xFF;
        data[i + 7] = nx | (ny << 8) | (nz << 16);
    }

    @Override
    public boolean useAmbientOcclusion() {
        // Оригинал (RenderBoxDuct) рисовал с плоской яркостью — без AO; AO даёт тёмные полосы
        // на вогнутых стыках ядро/рукав.
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        ensureSprites();
        return particle;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms != null ? transforms : ItemTransforms.NO_TRANSFORMS;
    }

    //? if forge || neoforge {
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        // CUTOUT: текстуры короба с прозрачными полями, 1.7.10 рендерил их с alpha-тестом
        // (поля вырезаны, сквозь них просвечивает окружение). Внутренние реверс-квады
        // (см. face) закрывают вырезы стенками короба.
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }
    //?}
}
