package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

// Утилитарный класс для создания кубоидов с настраиваемыми UV координатами для каждой грани. Необходим для процедурной генерации модели провода.
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
//? if forge {
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
*///?}

public class ModelHelper {

    public record UVBox(float u0, float v0, float u1, float v1) {}
    public record UVSpec(UVBox box, boolean rotate90) {
        public UVSpec(UVBox box) { this(box, false); }
    }

    public static void createCuboid(List<BakedQuad> quads, Vector3f from, Vector3f to, TextureAtlasSprite sprite, Map<Direction, UVSpec> uvMap, Set<Direction> facesToSkip) {
        for (Direction direction : Direction.values()) {
            if (facesToSkip.contains(direction)) {
                continue;
            }
            UVSpec spec = uvMap.get(direction);
            if (spec != null) {
                quads.add(createQuad(from, to, direction, sprite, spec));
            }
        }
    }

    private static BakedQuad createQuad(Vector3f from, Vector3f to, Direction direction, TextureAtlasSprite sprite, UVSpec spec) {
        //? if forge {
        QuadBakingVertexConsumer.Buffered builder = new QuadBakingVertexConsumer.Buffered();
        builder.setSprite(sprite);
        builder.setDirection(direction);
        builder.setHasAmbientOcclusion(true);

        Vector3f normal = direction.step();
        float x0 = from.x() / 16f, y0 = from.y() / 16f, z0 = from.z() / 16f;
        float x1 = to.x() / 16f,   y1 = to.y() / 16f,   z1 = to.z() / 16f;

        UVBox uv = spec.box();
        // UVBox в текселях 0..16; getU/getV на 1.21.1 ждут 0..1, поэтому ремапим сами.
        float u0 = net.minecraft.util.Mth.lerp(uv.u0() / 16f, sprite.getU0(), sprite.getU1());
        float v0 = net.minecraft.util.Mth.lerp(uv.v0() / 16f, sprite.getV0(), sprite.getV1());
        float u1 = net.minecraft.util.Mth.lerp(uv.u1() / 16f, sprite.getU0(), sprite.getU1());
        float v1 = net.minecraft.util.Mth.lerp(uv.v1() / 16f, sprite.getV0(), sprite.getV1());

        switch (direction) {
            case DOWN  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x0, y0, z0, u0, v0},
                                    new float[]{x1, y0, z0, u1, v0}, new float[]{x1, y0, z1, u1, v1});
            case UP    -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y1, z0, u0, v0}, new float[]{x0, y1, z1, u0, v1},
                                    new float[]{x1, y1, z1, u1, v1}, new float[]{x1, y1, z0, u1, v0});
            case NORTH -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y1, z0, u0, v0}, new float[]{x1, y1, z0, u1, v0},
                                    new float[]{x1, y0, z0, u1, v1}, new float[]{x0, y0, z0, u0, v1});
            case SOUTH -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x1, y0, z1, u1, v1},
                                    new float[]{x1, y1, z1, u1, v0}, new float[]{x0, y1, z1, u0, v0});
            case WEST  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x0, y1, z1, u0, v0},
                                    new float[]{x0, y1, z0, u1, v0}, new float[]{x0, y0, z0, u1, v1});
            case EAST  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x1, y0, z0, u0, v1}, new float[]{x1, y1, z0, u0, v0},
                                    new float[]{x1, y1, z1, u1, v0}, new float[]{x1, y0, z1, u1, v1});
        }
        return builder.getQuad();
        //?}

        //? if neoforge {
        /*// NeoForge 1.21.1: QuadBakingVertexConsumer — самостоятельный класс (не Buffered),
        // использует 1.21 VertexConsumer API (addVertex/setUv/setNormal вместо vertex().endVertex()).
        // Логика полностью зеркальна forge-ветке.
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setDirection(direction);
        builder.setHasAmbientOcclusion(true);

        Vector3f normal = direction.step();
        float x0 = from.x() / 16f, y0 = from.y() / 16f, z0 = from.z() / 16f;
        float x1 = to.x() / 16f, y1 = to.y() / 16f, z1 = to.z() / 16f;
        // UVBox в текселях 0..16; setUv ждёт атласные UV, поэтому ремапим через спрайт.
        float u0 = net.minecraft.util.Mth.lerp(spec.box().u0() / 16f, sprite.getU0(), sprite.getU1());
        float u1 = net.minecraft.util.Mth.lerp(spec.box().u1() / 16f, sprite.getU0(), sprite.getU1());
        float v0 = net.minecraft.util.Mth.lerp(spec.box().v0() / 16f, sprite.getV0(), sprite.getV1());
        float v1 = net.minecraft.util.Mth.lerp(spec.box().v1() / 16f, sprite.getV0(), sprite.getV1());

        switch (direction) {
            case DOWN  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x0, y0, z0, u0, v0},
                                    new float[]{x1, y0, z0, u1, v0}, new float[]{x1, y0, z1, u1, v1});
            case UP    -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y1, z0, u0, v0}, new float[]{x0, y1, z1, u0, v1},
                                    new float[]{x1, y1, z1, u1, v1}, new float[]{x1, y1, z0, u1, v0});
            case NORTH -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y1, z0, u0, v0}, new float[]{x1, y1, z0, u1, v0},
                                    new float[]{x1, y0, z0, u1, v1}, new float[]{x0, y0, z0, u0, v1});
            case SOUTH -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x1, y0, z1, u1, v1},
                                    new float[]{x1, y1, z1, u1, v0}, new float[]{x0, y1, z1, u0, v0});
            case WEST  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x0, y0, z1, u0, v1}, new float[]{x0, y1, z1, u0, v0},
                                    new float[]{x0, y1, z0, u1, v0}, new float[]{x0, y0, z0, u1, v1});
            case EAST  -> putVertices(builder, normal, spec.rotate90(),
                                    new float[]{x1, y0, z0, u0, v1}, new float[]{x1, y1, z0, u0, v0},
                                    new float[]{x1, y1, z1, u1, v0}, new float[]{x1, y0, z1, u1, v1});
        }
        return builder.bakeQuad();
        *///?}
    }

    //? if forge {
    private static void putVertices(QuadBakingVertexConsumer builder, Vector3f normal, boolean rotate, float[] v1, float[] v2, float[] v3, float[] v4) {
        if (!rotate) {
            putVertex(builder, normal, v1[0], v1[1], v1[2], v1[3], v1[4]);
            putVertex(builder, normal, v2[0], v2[1], v2[2], v2[3], v2[4]);
            putVertex(builder, normal, v3[0], v3[1], v3[2], v3[3], v3[4]);
            putVertex(builder, normal, v4[0], v4[1], v4[2], v4[3], v4[4]);
        } else {
            putVertex(builder, normal, v1[0], v1[1], v1[2], v2[3], v2[4]);
            putVertex(builder, normal, v2[0], v2[1], v2[2], v3[3], v3[4]);
            putVertex(builder, normal, v3[0], v3[1], v3[2], v4[3], v4[4]);
            putVertex(builder, normal, v4[0], v4[1], v4[2], v1[3], v1[4]);
        }
    }

    private static void putVertex(QuadBakingVertexConsumer builder, Vector3f normal, float x, float y, float z, float u, float v) {
        builder.vertex(x, y, z).uv(u, v).uv2(0, 0).normal(normal.x(), normal.y(), normal.z()).color(-1).endVertex();
    }
    //?}

    //? if neoforge {
    /*private static void putVertices(QuadBakingVertexConsumer builder, Vector3f normal, boolean rotate, float[] v1, float[] v2, float[] v3, float[] v4) {
        if (!rotate) {
            putVertex(builder, normal, v1[0], v1[1], v1[2], v1[3], v1[4]);
            putVertex(builder, normal, v2[0], v2[1], v2[2], v2[3], v2[4]);
            putVertex(builder, normal, v3[0], v3[1], v3[2], v3[3], v3[4]);
            putVertex(builder, normal, v4[0], v4[1], v4[2], v4[3], v4[4]);
        } else {
            putVertex(builder, normal, v1[0], v1[1], v1[2], v2[3], v2[4]);
            putVertex(builder, normal, v2[0], v2[1], v2[2], v3[3], v3[4]);
            putVertex(builder, normal, v3[0], v3[1], v3[2], v4[3], v4[4]);
            putVertex(builder, normal, v4[0], v4[1], v4[2], v1[3], v1[4]);
        }
    }

    private static void putVertex(QuadBakingVertexConsumer builder, Vector3f normal, float x, float y, float z, float u, float v) {
        builder.addVertex(x, y, z).setColor(-1, -1, -1, -1).setUv(u, v).setUv2(0, 0).setNormal(normal.x(), normal.y(), normal.z());
    }
    *///?}

    /** Position: первые 3 int (x,y,z). Normal: последний int вершины (Embeddium=8 ints/vertex, Vanilla=9). */
    private static final int POSITION_OFFSET = 0;
    private static final int MIN_STRIDE = 8; // Embeddium/Sodium compact format

    /**
     * Reusable scratch vectors for {@link #transformQuadsByMatrix}. Render-thread
     * only; the transform helpers do not recurse, so per-class scratch is safe and
     * avoids a {@code new Vector4f} per quad (door open/close, BER part matrix).
     */
    private static final Vector4f TMP_POS = new Vector4f();
    private static final Vector4f TMP_NORM = new Vector4f();

    /**
     * Поворачивает квады вокруг оси Y через центр блока (0.5, 0.5, 0.5).
     * @param quads исходные квады
     * @param angleDeg угол в градусах (0=North, 90=East, 180=South, 270=West)
     */
    public static List<BakedQuad> transformQuadsByFacing(List<BakedQuad> quads, int angleDeg) {
        if (quads == null || quads.isEmpty() || angleDeg == 0) {
            return quads;
        }
        float angleRad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        float cx = 0.5f, cz = 0.5f;

        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int normalOffset = stride - 1; // normal в последнем int вершины

            int[] newVerts = verts.clone();

            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float x = Float.intBitsToFloat(newVerts[i + POSITION_OFFSET]);
                float y = Float.intBitsToFloat(newVerts[i + POSITION_OFFSET + 1]);
                float z = Float.intBitsToFloat(newVerts[i + POSITION_OFFSET + 2]);

                float dx = x - cx, dz = z - cz;
                float nx = dx * cos - dz * sin + cx;
                float nz = dx * sin + dz * cos + cz;

                newVerts[i] = Float.floatToIntBits(nx);
                newVerts[i + 1] = Float.floatToIntBits(y);
                newVerts[i + 2] = Float.floatToIntBits(nz);

                if (normalOffset < stride) {
                    int packedNormal = newVerts[i + normalOffset];
                    byte nxb = (byte) (packedNormal & 0xFF);
                    byte nyb = (byte) ((packedNormal >> 8) & 0xFF);
                    byte nzb = (byte) ((packedNormal >> 16) & 0xFF);
                    float nxf = (nxb) / 127f;
                    float nyf = (nyb) / 127f;
                    float nzf = (nzb) / 127f;
                    float rnx = nxf * cos - nzf * sin;
                    float rnz = nxf * sin + nzf * cos;
                    int newPacked = ((byte) (rnx * 127) & 0xFF) | (((byte) (nyf * 127) & 0xFF) << 8) | (((byte) (rnz * 127) & 0xFF) << 16);
                    newVerts[i + normalOffset] = newPacked;
                }
            }
            Direction rotatedDir = rotateDirection(quad.getDirection(), angleDeg);
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), rotatedDir, quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    /**
     * Аффинный ремап UV квадов из области спрайта {@code from} в область {@code to}.
     * Основа дедупликации скинов дверей: каноничные квады запекаются один раз с одним
     * спрайтом, текстурные варианты получают свои UV ленивым ремапом (скины отличаются
     * ТОЛЬКО аффинным rect'ом спрайта в атласе).
     * @return исходный список, если ремап не нужен (from == to / null / пустой список).
     */
    public static List<BakedQuad> remapQuadUvs(List<BakedQuad> quads, TextureAtlasSprite from, TextureAtlasSprite to) {
        if (quads == null || quads.isEmpty() || from == null || to == null || from == to) {
            return quads;
        }
        return remapQuadUvsUnit(quads,
                from.getU0(), from.getV0(), from.getU1() - from.getU0(), from.getV1() - from.getV0(),
                to.getU0(), to.getV0(), to.getU1() - to.getU0(), to.getV1() - to.getV0(),
                to);
    }

    /**
     * Нормализация UV в sprite-local [0..1] (обратное развёртывание из области {@code sprite}).
     * Спрайт-метадата квадов сохраняется (потребители типа PartGeometry её не читают).
     */
    public static List<BakedQuad> normalizeQuadUvs(List<BakedQuad> quads, TextureAtlasSprite sprite) {
        if (quads == null || quads.isEmpty() || sprite == null) {
            return quads;
        }
        return remapQuadUvsUnit(quads,
                sprite.getU0(), sprite.getV0(), sprite.getU1() - sprite.getU0(), sprite.getV1() - sprite.getV0(),
                0f, 0f, 1f, 1f,
                sprite);
    }

    /**
     * Допуск «волосков» при tile-split ({@link #normalizeWithTileSplit}): доли тайла
     * уже этого размера (в единицах тайла) считаются вырожденными и отбрасываются
     * (счётчик {@code tileSplitSlivers}). Целые тайлы не теряются никогда.
     */
    public static final float UV_EPS_MIN = 1e-4f;

    /**
     * null-безопасная проверка «identity-rect» {0,0,1,1}: ремап таким rect'ом
     * математически тождественен отсутствию ремапа. ЕДИНЫЙ guard для
     * {@code expandQuadUvsUnit}-путей (MachinePartRenderer.renderQuadsFallback) и
     * клип-тира (DoorPartClipper) — дедупликация бывших двух копий инварианта
     * «non-identity uvRect».
     */
    public static boolean isIdentityRect(@Nullable float[] uvRect) {
        return uvRect == null || uvRect.length < 4
                || (uvRect[0] == 0f && uvRect[1] == 0f && uvRect[2] == 1f && uvRect[3] == 1f);
    }

    /**
     * Выбор terrain RenderType для immediate-путей партов (клип-тир и fallback):
     * fade-альфа &lt; 0.99 — translucent (alpha blending), иначе cutout (alpha test
     * прозрачных текселей стекла/окон; solid не годится — без alpha test прозрачные
     * тексели выходят чёрными). ЕДИНАЯ точка выбора для бывших двух копий инварианта
     * (MachinePartRenderer.renderClippedQuads/renderQuadsFallback).
     */
    public static net.minecraft.client.renderer.RenderType clipRenderType(float alpha) {
        return alpha < 0.99f
                ? net.minecraft.client.renderer.RenderType.translucent()
                : net.minecraft.client.renderer.RenderType.cutout();
    }

    // ==================== Tile-split + нормализация (defense-in-depth) ====================

    /** Счётчик отброшенных «волосков» (доли тайла &lt; {@link #UV_EPS_MIN}). */
    private static long tileSplitSlivers;
    /** One-shot guard warn-лога первого отброшенного «волоска» и срабатывания >64-гейта. */
    private static boolean tileSplitWarned;

    /** Число отброшенных tile-split'ом вырожденных кусков (диагностика потери геометрии). */
    public static long tileSplitSliverCount() {
        return tileSplitSlivers;
    }

    /**
     * Композиция «tile-split → frac → sprite-нормализация» (defense-in-depth против
     * легаси-тайловых квадов, рев.2 дизайна UV дверей).
     * <ol>
     *   <li><b>tile-split</b> — Sutherland-Hodgman квада по целочисленным UV-плоскостям
     *       тайлов (каждый квад режется на подквады, целиком лежащие в одной ячейке
     *       [tu..tu+1]×[tv..tv+1]);</li>
     *   <li><b>frac</b> — UV подквада (u−tu, v−tv) ∈ [0..1];</li>
     *   <li><b>sprite-нормализация</b> подквада СВОИМ спрайтом: (u−u0)/du, (v−v0)/dv —
     *       именно в этом порядке; frac без (в) уводит текстуру (для атласного входа
     *       frac ≠ sprite-нормализация).</li>
     * </ol>
     * Для АТЛАСНОГО входа (квад лежит внутри атласного rect'а спрайта, как на текущих
     * call-site'ах MachineDoorRenderer.resolveDoorPartQuads/bakeDaeNodeQuads —
     * DaeQuadBaker лерпит raw UV в rect спрайта, OBJ-печка идёт через texture.getU)
     * split-фаза — no-op: квад не пересекает целочисленные плоскости, и одноячеечный
     * квад идёт по прямому пути, ТОЖДЕСТВЕННОМУ {@link #normalizeQuadUvsPerQuad}
     * (включая бит-в-бит формулу (u−u0)/du). «Легаси uv 5.77 → пять подквадов» через
     * эти call-site'ы недостижим — функция страховка на случай будущих тайловых
     * источников квадов.
     * <p>
     * Отброс «волосков»: после клипа куски с UV-протяжённостью &lt; {@link #UV_EPS_MIN}
     * по обеим осям отбрасываются (u=1+1e-7 после floorI даёт вырожденный кусок —
     * допустимы исчезающие волоски, недопустимы потерянные целые тайлы). Guard
     * патологического UV (&gt;64 тайлов) — passthrough всего квада + однократный warn.
     */
    public static List<BakedQuad> normalizeWithTileSplit(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) {
            return quads;
        }
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            List<BakedQuad> pieces = splitQuadTiles(quad);
            if (pieces == null) {
                // Одноячеечный квад ОБЯЗАН пройти sprite-нормализацию ((u−u0)/du),
                // как в normalizeQuadUvsPerQuad: вход call-site'ов — атласные UV,
                // а контракт share-кеша — нормализованные [0..1]. Passthrough без
                // формулы оставлял в кеше атласные UV → ремап uvRect «протекал» атласом.
                BakedQuad normalized = normalizeQuadUvsPerQuad(java.util.List.of(quad)).get(0);
                if (result != null) {
                    result.add(normalized);
                }
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(quads.size());
                for (int j = 0; j < i; j++) {
                    result.add(quads.get(j));
                }
            }
            result.addAll(pieces);
        }
        return result != null ? result : quads;
    }

    /** Ёмкость скратч-полигона разбивки (квад × 4 полуплоскости тайла — максимум 8 вершин). */
    private static final int SPLIT_CAP = 8;
    /** Максимальный поддерживаемый stride вершин в разбивке (BLOCK=8, vanilla=9). */
    private static final int SPLIT_MAX_STRIDE = 9;
    /** Максимальное число тайлов одного квада (патологический UV — passthrough). */
    private static final int SPLIT_MAX_TILES = 64;

    // Скратчи разбивки (рендер однопоточный).
    private static final int[][] SPLIT_A = new int[SPLIT_CAP][SPLIT_MAX_STRIDE];
    private static final int[][] SPLIT_B = new int[SPLIT_CAP][SPLIT_MAX_STRIDE];
    private static final float[] SPLIT_DIST = new float[SPLIT_CAP];
    /** Переиспользуемый выход разбивки одного квада. */
    private static final ArrayList<BakedQuad> SPLIT_TMP = new ArrayList<>();

    /**
     * Разбивает один квад по целочисленным UV-тайлам и нормализует подквады своим
     * спрайтом. null — квад не нуждается в разбивке (одноячеечный/нецелевой формат —
     * вызывающий берёт его как есть; одноячеечный путь тождественен
     * {@link #normalizeQuadUvsPerQuad}).
     */
    @Nullable
    private static List<BakedQuad> splitQuadTiles(BakedQuad quad) {
        TextureAtlasSprite s = quad.getSprite();
        int[] data = quad.getVertices();
        int stride = data.length / 4;
        if (s == null || stride < MIN_STRIDE || stride > SPLIT_MAX_STRIDE
                || data.length != 4 * stride || s.getU1() - s.getU0() <= 0f
                || s.getV1() - s.getV0() <= 0f) {
            return null;
        }
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float u = Float.intBitsToFloat(data[i * stride + 4]);
            float vv = Float.intBitsToFloat(data[i * stride + 5]);
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, vv); maxV = Math.max(maxV, vv);
        }
        // floorI(u - UV_EPS): корректен на u=1.0; вырожденный кусок от u=1+1e-7
        // отсекается послойно отбросом «волосков» < UV_EPS_MIN.
        int tu0 = floorI(minU - UV_EPS_MIN), tu1 = floorI(maxU - UV_EPS_MIN);
        int tv0 = floorI(minV - UV_EPS_MIN), tv1 = floorI(maxV - UV_EPS_MIN);
        if (tu0 == tu1 && tv0 == tv1) {
            return null; // одна ячейка — split-фаза no-op, прямой путь
        }
        if ((long) (tu1 - tu0 + 1) * (tv1 - tv0 + 1) > SPLIT_MAX_TILES) {
            if (!tileSplitWarned) {
                tileSplitWarned = true;
                com.hbm_m.main.MainRegistry.LOGGER.warn(
                        "[HBM-M] normalizeWithTileSplit: quad with >{} uv tiles (u[{}, {}] v[{}, {}]) -- passthrough",
                        SPLIT_MAX_TILES, minU, maxU, minV, maxV);
            }
            return null; // НЕ режем: passthrough, потери нет
        }

        SPLIT_TMP.clear();
        final float su0 = s.getU0(), sv0 = s.getV0();
        final float du = s.getU1() - s.getU0(), dv = s.getV1() - s.getV0();
        for (int tu = tu0; tu <= tu1; tu++) {
            for (int tv = tv0; tv <= tv1; tv++) {
                int count = 4;
                for (int i = 0; i < 4; i++) {
                    System.arraycopy(data, i * stride, SPLIT_A[i], 0, stride);
                }
                count = clipUvPlane(SPLIT_A, count, stride, 1.0f, 0f, -(float) tu, SPLIT_B);      // u >= tu
                if (count == 0) continue;
                count = clipUvPlane(SPLIT_B, count, stride, -1.0f, 0f, (float) tu + 1f, SPLIT_A); // u <= tu+1
                if (count == 0) continue;
                count = clipUvPlane(SPLIT_A, count, stride, 0f, 1.0f, -(float) tv, SPLIT_B);      // v >= tv
                if (count == 0) continue;
                count = clipUvPlane(SPLIT_B, count, stride, 0f, -1.0f, (float) tv + 1f, SPLIT_A); // v <= tv+1
                if (count == 0) continue;
                emitTile(SPLIT_A, count, stride, quad, s, (float) tu, (float) tv, su0, sv0, du, dv, SPLIT_TMP);
            }
        }
        return SPLIT_TMP.isEmpty() ? null : SPLIT_TMP;
    }

    private static int floorI(float x) {
        return (int) Math.floor(x);
    }

    /**
     * Режет полигон полуплоскостью в UV-пространстве: внутри = a*u + b*v + c >= 0.
     * Вершины полного формата ({@code stride} int); t ребра считается по (u,v).
     */
    private static int clipUvPlane(int[][] src, int count, int stride, float a, float b, float c, int[][] dst) {
        int out = 0;
        for (int i = 0; i < count; i++) {
            int[] vt = src[i];
            SPLIT_DIST[i] = a * Float.intBitsToFloat(vt[4]) + b * Float.intBitsToFloat(vt[5]) + c;
        }
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            boolean inI = SPLIT_DIST[i] >= 0f;
            boolean inJ = SPLIT_DIST[j] >= 0f;
            if (inI) {
                System.arraycopy(src[i], 0, dst[out], 0, stride);
                out++;
            }
            if (inI != inJ) {
                float dI = SPLIT_DIST[i], dJ = SPLIT_DIST[j];
                float t = Math.abs(dI - dJ) < 1e-9f ? 0.5f : dI / (dI - dJ);
                // Линейная интерполяция вершины: позиция/uv (0..2, 4..5) — raw float,
                // цвет (3) — покомпонентно, light/normal (6..) — от внутренней вершины i.
                int[] va = src[i], vb = src[j], vo = dst[out];
                for (int k = 0; k < stride; k++) {
                    vo[k] = va[k];
                }
                for (int k = 0; k < 3; k++) {
                    vo[k] = Float.floatToRawIntBits(lerpF(
                            Float.intBitsToFloat(va[k]), Float.intBitsToFloat(vb[k]), t));
                }
                vo[3] = lerpPackedColor(va[3], vb[3], t);
                vo[4] = Float.floatToRawIntBits(lerpF(
                        Float.intBitsToFloat(va[4]), Float.intBitsToFloat(vb[4]), t));
                vo[5] = Float.floatToRawIntBits(lerpF(
                        Float.intBitsToFloat(va[5]), Float.intBitsToFloat(vb[5]), t));
                out++;
            }
        }
        return out;
    }

    private static float lerpF(float p, float q, float t) {
        return p + (q - p) * t;
    }

    /** Покомпонентная интерполяция пакованного цвета (r в младшем байте). */
    private static int lerpPackedColor(int p, int q, float t) {
        int r = Math.round(lerpF(p & 0xFF, q & 0xFF, t));
        int g = Math.round(lerpF((p >> 8) & 0xFF, (q >> 8) & 0xFF, t));
        int b = Math.round(lerpF((p >> 16) & 0xFF, (q >> 16) & 0xFF, t));
        int a = Math.round(lerpF((p >> 24) & 0xFF, (q >> 24) & 0xFF, t));
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Собирает подквады фан-триангуляцией: frac (u−tu, v−tv), затем sprite-нормализация
     * своим спрайтом ((u−u0)/du) — порядок критичен (см. javadoc
     * {@link #normalizeWithTileSplit}). «Волоски» (&lt; {@link #UV_EPS_MIN} по обеим
     * осям) отбрасываются со счётчиком.
     */
    private static void emitTile(int[][] poly, int n, int stride, BakedQuad src, TextureAtlasSprite s,
                                 float tu, float tv, float su0, float sv0, float du, float dv,
                                 List<BakedQuad> out) {
        if (n < 3) return;
        // Отброс вырожденных кусков: UV-протяжённость после клипа.
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float u = Float.intBitsToFloat(poly[i][4]);
            float vv = Float.intBitsToFloat(poly[i][5]);
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, vv); maxV = Math.max(maxV, vv);
        }
        if (maxU - minU < UV_EPS_MIN && maxV - minV < UV_EPS_MIN) {
            tileSplitSlivers++;
            if (!tileSplitWarned) {
                tileSplitWarned = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] normalizeWithTileSplit: dropped first degenerate uv sliver ({} total so far)",
                        tileSplitSlivers);
            }
            return;
        }
        // frac -> sprite-нормализация, in-place по скратч-полигону.
        for (int i = 0; i < n; i++) {
            int[] vt = poly[i];
            float u = Float.intBitsToFloat(vt[4]);
            float vv = Float.intBitsToFloat(vt[5]);
            vt[4] = Float.floatToRawIntBits((u - tu - su0) / du);
            vt[5] = Float.floatToRawIntBits((vv - tv - sv0) / dv);
        }
        // Фан: n==3 квад(0,1,2,2); n==4 как есть; n>=5 квад(0..3) + треугольники (0,k,k+1,k+1).
        if (n <= 4) {
            out.add(packTile(poly, new int[] { 0, 1, 2, n - 1 }, stride, src));
            return;
        }
        out.add(packTile(poly, new int[] { 0, 1, 2, 3 }, stride, src));
        for (int k = 3; k + 1 < n; k++) {
            out.add(packTile(poly, new int[] { 0, k, k + 1, k + 1 }, stride, src));
        }
    }

    /** Пакует подквад из вершин-индексов {@code idx} скратч-полигона. */
    private static BakedQuad packTile(int[][] poly, int[] idx, int stride, BakedQuad src) {
        int[] packed = new int[4 * stride];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(poly[idx[i]], 0, packed, i * stride, stride);
        }
        return new BakedQuad(packed, src.getTintIndex(), src.getDirection(), src.getSprite(), src.isShade());
    }

    /**
     * Нормализация UV в sprite-local [0..1] ПО КАЖДОМУ кваду его собственным спрайтом.
     * Версия для мешей, где части могут нести разные спрайты (canonical-квады дверей).
     * Квады без спрайта остаются как есть.
     */
    public static List<BakedQuad> normalizeQuadUvsPerQuad(List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) {
            return quads;
        }
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite s = quad.getSprite();
            if (s == null || s.getU1() - s.getU0() <= 0f || s.getV1() - s.getV0() <= 0f) {
                result.add(quad);
                continue;
            }
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE || verts.length / 4 < 6) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int[] newVerts = verts.clone();
            float fdu = s.getU1() - s.getU0(), fdv = s.getV1() - s.getV0();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float u = Float.intBitsToFloat(newVerts[i + 4]);
                float vv = Float.intBitsToFloat(newVerts[i + 5]);
                newVerts[i + 4] = Float.floatToIntBits((u - s.getU0()) / fdu);
                newVerts[i + 5] = Float.floatToIntBits((vv - s.getV0()) / fdv);
            }
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(), s, quad.isShade()));
        }
        return result;
    }

    /**
     * Развёртывание sprite-local [0..1] UV в область {@code to} (u0,v0 + du,dv).
     * Обратна {@link #normalizeQuadUvs}.
     */
    public static List<BakedQuad> expandQuadUvsUnit(List<BakedQuad> quads,
                                                    float u0, float v0, float du, float dv,
                                                    TextureAtlasSprite spriteMeta) {
        if (quads == null || quads.isEmpty()) {
            return quads;
        }
        return remapQuadUvsUnit(quads, 0f, 0f, 1f, 1f, u0, v0, du, dv, spriteMeta);
    }

    /** Общий аффинный ремап: uv' = toOrigin + (uv - fromOrigin)/fromSize * toSize. */
    private static List<BakedQuad> remapQuadUvsUnit(List<BakedQuad> quads,
                                                    float fu0, float fv0, float fdu, float fdv,
                                                    float tu0, float tv0, float tdu, float tdv,
                                                    TextureAtlasSprite spriteMeta) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            if (stride < 6) {
                result.add(quad);
                continue;
            }
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float u = Float.intBitsToFloat(newVerts[i + 4]);
                float vv = Float.intBitsToFloat(newVerts[i + 5]);
                float lu = (u - fu0) / fdu;
                float lv = (vv - fv0) / fdv;
                newVerts[i + 4] = Float.floatToIntBits(tu0 + lu * tdu);
                newVerts[i + 5] = Float.floatToIntBits(tv0 + lv * tdv);
            }
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(), spriteMeta, quad.isShade()));
        }
        return result;
    }

    /**
     * Смещение UV всех вершин квада (stride 8 ints: u,v в {@code base+4}, {@code base+5}).
     * Для текстурной анимации как GL_TEXTURE в 1.7.10.
     */
    public static List<BakedQuad> offsetQuadUvs(List<BakedQuad> quads, float du, float dv) {
        if (quads == null || quads.isEmpty() || (du == 0f && dv == 0f)) {
            return quads;
        }
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            if (stride < 6) {
                result.add(quad);
                continue;
            }
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float u = Float.intBitsToFloat(newVerts[i + 4]);
                float vv = Float.intBitsToFloat(newVerts[i + 5]);
                newVerts[i + 4] = Float.floatToIntBits(u + du);
                newVerts[i + 5] = Float.floatToIntBits(vv + dv);
            }
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    /**
     * Смещение UV с wrap внутри sprite bounds.
     * du, dv — смещение в нормализованном [0,1] пространстве текстуры (как GL_TEXTURE matrix в 1.7.10).
     * uMin/uMax/vMin/vMax — bounds спрайта в атласе.
     */
    public static List<BakedQuad> offsetQuadUvsWrapped(List<BakedQuad> quads, float du, float dv,
                                                        float uMin, float uMax, float vMin, float vMax) {
        if (quads == null || quads.isEmpty() || (du == 0f && dv == 0f)) {
            return quads;
        }
        float uRange = uMax - uMin;
        float vRange = vMax - vMin;
        if (uRange <= 0f || vRange <= 0f) {
            return quads;
        }

        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            if (stride < 6) {
                result.add(quad);
                continue;
            }
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float uSprite = Float.intBitsToFloat(newVerts[i + 4]);
                float vSprite = Float.intBitsToFloat(newVerts[i + 5]);

                float uRaw = (uSprite - uMin) / uRange;
                float vRaw = (vSprite - vMin) / vRange;

                float newURaw = uRaw + du;
                float newVRaw = vRaw + dv;
                newURaw = newURaw - Mth.floor(newURaw);
                newVRaw = newVRaw - Mth.floor(newVRaw);

                newVerts[i + 4] = Float.floatToIntBits(uMin + newURaw * uRange);
                newVerts[i + 5] = Float.floatToIntBits(vMin + newVRaw * vRange);
            }
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    /**
     * Смещает квады на (dx, dy, dz). Для выравнивания baked model с BER (translate 0.5, 0, 0.5).
     */
    public static List<BakedQuad> translateQuads(List<BakedQuad> quads, float dx, float dy, float dz) {
        if (quads == null || quads.isEmpty() || (dx == 0 && dy == 0 && dz == 0)) {
            return quads;
        }
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float x = Float.intBitsToFloat(verts[i + POSITION_OFFSET]);
                float y = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 1]);
                float z = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 2]);
                newVerts[i] = Float.floatToIntBits(x + dx);
                newVerts[i + 1] = Float.floatToIntBits(y + dy);
                newVerts[i + 2] = Float.floatToIntBits(z + dz);
            }
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    private static Direction rotateDirection(Direction dir, int angleDeg) {
        if (dir.getAxis() == Direction.Axis.Y) return dir;
        int steps = (angleDeg / 90) % 4;
        if (steps < 0) steps += 4;
        Direction r = dir;
        for (int i = 0; i < steps; i++) {
            r = r.getClockWise(Direction.Axis.Y);
        }
        return r;
    }

    /**
     * Применяет произвольную матрицу 4x4 к вершинам квадов (позиция и нормаль).
     * Используется для трансформации door panel в open/closed состоянии в BakedModel.
     */
    public static List<BakedQuad> transformQuadsByMatrix(List<BakedQuad> quads, Matrix4f matrix) {
        if (quads == null || quads.isEmpty()) {
            return quads;
        }
        Matrix4f normalMatrix = new Matrix4f(matrix).invert().transpose();
        List<BakedQuad> result = new ArrayList<>(quads.size());
        // Reuse class-level scratch (render-thread only, no recursion).
        Vector4f pos = TMP_POS;
        Vector4f norm = TMP_NORM;
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            if (verts.length < 4 * MIN_STRIDE) {
                result.add(quad);
                continue;
            }
            int stride = verts.length / 4;
            int normalOffset = stride - 1;
            int[] newVerts = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float x = Float.intBitsToFloat(verts[i + POSITION_OFFSET]);
                float y = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 1]);
                float z = Float.intBitsToFloat(verts[i + POSITION_OFFSET + 2]);
                pos.set(x, y, z, 1f);
                // Column-vector convention (matrix · pos) — matches QuadTransforms.transform
                // and the JOML-standard matrices callers build (translate/rotate/mul).
                // pos.mul(matrix) applied the transpose, diverging from QuadTransforms and
                // silently transposing door/cog part transforms.
                matrix.transform(pos);
                newVerts[i] = Float.floatToIntBits(pos.x());
                newVerts[i + 1] = Float.floatToIntBits(pos.y());
                newVerts[i + 2] = Float.floatToIntBits(pos.z());
                if (normalOffset < stride) {
                    int packedNormal = verts[i + normalOffset];
                    byte nxb = (byte) (packedNormal & 0xFF);
                    byte nyb = (byte) ((packedNormal >> 8) & 0xFF);
                    byte nzb = (byte) ((packedNormal >> 16) & 0xFF);
                    norm.set(nxb / 127f, nyb / 127f, nzb / 127f, 0f);
                    normalMatrix.transform(norm);
                    norm.normalize();
                    int newPacked = ((byte) (norm.x * 127) & 0xFF)
                            | (((byte) (norm.y * 127) & 0xFF) << 8)
                            | (((byte) (norm.z * 127) & 0xFF) << 16);
                    newVerts[i + normalOffset] = newPacked;
                }
            }
            int packedN = newVerts[normalOffset];
            byte nxb = (byte) (packedN & 0xFF);
            byte nyb = (byte) ((packedN >> 8) & 0xFF);
            byte nzb = (byte) ((packedN >> 16) & 0xFF);
            Direction transformedDir = directionFromNormal(nxb / 127f, nyb / 127f, nzb / 127f);
            if (transformedDir == null) transformedDir = quad.getDirection();
            result.add(new BakedQuad(newVerts, quad.getTintIndex(), transformedDir, quad.getSprite(), quad.isShade()));
        }
        return result;
    }

    private static Direction directionFromNormal(float dx, float dy, float dz) {
        float ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (az >= ax && az >= ay) return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }
}