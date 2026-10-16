package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * CPU-клиппер дверных партов (Sutherland-Hodgman по BakedQuad). Аналог glClipPlane /
 * ntm-next GroupObject.emitClipped: полигоны режутся плоскостями на CPU, меш не
 * пересобирается — клипуемая часть рисуется per-frame immediate.
 * <p>
 * <b>Знаковой контракт</b> (конвенция DoorDecl, подтверждена ревью): вход — плоскости
 * {A,B,C,D}, «внутри» = {@code A*x + B*y + C*z + D >= 0}. Классификация выполняется
 * НЕПОСРЕДСТВЕННО в этой конвенции ({@code da = n·a + D}); это НЕ дословный порт
 * GroupObject.classify (там {@code n·x - d >= 0} — D противоположного знака; эквивалент —
 * negate D при входе). Срез — открытый (как glClipPlane / ntm-next): по линии среза новой
 * грани не создаётся.
 * <p>
 * Пересечение ребра: {@code t = da/(da-db)}, линейная интерполяция position/uv/color
 * (прецедент StrandCasterRenderer.clipQuad/lerpVertex); light/normal — от внутренней
 * вершины. 0 вершин внутри — квад выбрасывается, 4 внутри — рисуется как есть; промежуточный
 * полигон — фан-триангуляция. Скратч-полигональные буферы переиспользуются (рендер
 * однопоточный).
 * <p>
 * <b>UV-пространство: один маркер, одна операция</b> (канонический дизайн рев.2). Маркер
 * NORMALIZED выводится атомарно из {@code uvRect != null} в момент enqueue — оба резолвера
 * ({@code doorUvRect} / {@code daeUvRect} в MachineDoorRenderer) возвращают null вне
 * share-режима, отдельного «space-тега» НЕТ: рассинхрон тега с uvRect невозможен по
 * построению.
 * <ul>
 *   <li>{@code uvRect != null} — квады кеша несут нормализованные sprite-local [0..1] UV;
 *       каждый квад ОБЯЗАТЕЛЬНО проходит единственную UV-операцию
 *       {@code remapUv} (u' = u0 + u*du) — тем же rect'ом, что InstUvRect /
 *       expandQuadUvsUnit (rect ТЕКУЩЕГО скина). Ремап-список мемоизируется
 *       (кеш проекции ниже).</li>
 *   <li>{@code uvRect == null} — квады кеша атласные; чистый геометрический клип,
 *       UV не трогается. Никакой «репарации легаси» не существует: состояние share
 *       детерминировано (per-frame desired!=current + shareEpoch, см.
 *       MachineDoorRenderer.syncShareState), рассинхрон построения меша и кадра
 *       невозможен.</li>
 * </ul>
 * <p>
 * <b>Горячий путь без аллокаций</b> (установившийся кадр закрытой двери): квад, целиком
 * лежащий внутри всех плоскостей, добавляется в результат КАК ЕСТЬ — без копирования
 * вершин и без нового BakedQuad; если не изменён ни один квад, входной список
 * возвращается без единой аллокации. При remap весь вход проходит мемо-хит кеша
 * проекции (см. ниже), поэтому обещание истинно и в share-режиме.
 * <p>
 * <b>Кеш проекции</b>: ключ — снапшот ЗНАЧЕНИЙ uvRect[0..3] (uvRect приходит как общий
 * UV_RECT_SCRATCH, перезаписываемый на каждый парт каждый кадр — хранение ссылки
 * портило бы ключ кросс-партово) + reference-equality (==) списка квадов + shareEpoch.
 * Идентификация — линейный скан маленького открытого списка записей (пар×скинов на
 * экране — единицы): identityHashCode-коллизии и «ремап чужим rect» невозможны.
 * Переполнение — вытеснение самой старой записи; epoch в ключе делает записи прежнего
 * share-режима мёртвыми сразу после flip'а (syncShareState), resource reload — через
 * новую идентичность списков квадов (старые записи вытесняются по capacity).
 */
final class DoorPartClipper {

    /** BLOCK vertex format: 8 int на вершину (pos x3, color, uv x2, light, normal). */
    private static final int VERTEX_SIZE = 8;
    /** Ёмкость скратч-полигона (4 входные вершины; 3 плоскости максимум у дверных партов). */
    private static final int CAPACITY = 8;

    // Переиспользуемые скратч-буферы (рендер однопоточный).
    private static final int[][] polyA = new int[CAPACITY][VERTEX_SIZE];
    private static final int[][] polyB = new int[CAPACITY][VERTEX_SIZE];
    private static final float[] dist = new float[CAPACITY];

    private DoorPartClipper() {}

    /**
     * Клипует список квадов плоскостями {@code planes} ({{A,B,C,D}, ...}, конвенция
     * «+D», локальные координаты геометрии партов) и возвращает результат. Квады,
     * целиком лежащие внутри всех плоскостей, возвращаются как есть; если не изменён
     * ни один квад — входной (при remap — спроецированный мемо-список) без аллокаций;
     * пустой выход — пустой список.
     * <p>
     * {@code uvRect} {u0,v0,du,dv} != null — маркер NORMALIZED: инлайн-ремап
     * sprite-local UV в атлас (u' = u0 + u*du) каждого квада; null — атласные квады,
     * чистый геометрический клип. Потеря квадов структурно невозможна: операций
     * с «отказом» в клипере нет.
     */
    static List<BakedQuad> clipQuads(List<BakedQuad> quads, float[][] planes, @Nullable float[] uvRect) {
        // isIdentityRect-rect математически тождественен отсутствию ремапа (тот же
        // guard, что ModelHelper.expandQuadUvsUnit в fallback-пути).
        boolean remap = uvRect != null
                && !com.hbm_m.client.model.ModelHelper.isIdentityRect(uvRect);
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            // Тайловый квад (UV-повторы за пределами спрайта): режется по
            // целочисленным UV-тайлам, каждый кусок получает frac(uv) и ремапится
            // в целевой rect (uvRect скина при share, иначе rect своего спрайта).
            // Для НОРМАЛИЗОВАННОГО кеша ([0..1]) разбивка не запускается — диапазон
            // в пределах единицы, обычный ремап. Без разбивки тайловый квад в атласе
            // сэмплирует чужие тайлы — «сетка полигонов»/«протекающий атлас».
            if (isTiledQuad(quad) && !(uvRect != null && isUnitRange(quad))) {
                out = copyHead(out, quads, i);
                SPLIT_TMP.clear();
                splitTiledQuad(quad, SPLIT_TMP, uvRect);
                for (int k = 0; k < SPLIT_TMP.size(); k++) {
                    BakedQuad piece = SPLIT_TMP.get(k);
                    if (isFullyInside(piece, planes)) {
                        out.add(piece);
                    } else {
                        clipQuad(piece, planes, null, out);
                    }
                }
                continue;
            }
            // Fast-path: все 4 вершины внутри всех плоскостей — квад как есть.
            // ТОЛЬКО при !remap: нормализованные квады (uvRect != null) обязаны
            // пройти ремап, иначе сэмплируют весь атлас («атлас протекает»).
            if (!remap && isFullyInside(quad, planes)) {
                if (out != null) {
                    out.add(quad);
                }
                continue;
            }
            out = copyHead(out, quads, i);
            clipQuad(quad, planes, remap ? uvRect : null, out);
        }
        // Ни один квад не изменён — входной список без аллокаций.
        return out != null ? out : quads;
    }

    /** Ленивая инициализация выхода: копия головы входа до первого изменённого квада. */
    private static List<BakedQuad> copyHead(@Nullable List<BakedQuad> out, List<BakedQuad> src, int exclusive) {
        if (out != null) {
            return out;
        }
        List<BakedQuad> fresh = new ArrayList<>(src.size());
        for (int j = 0; j < exclusive; j++) {
            fresh.add(src.get(j));
        }
        return fresh;
    }

    /**
     * true, если все 4 вершины квада лежат внутри всех плоскостей (dist >= 0) —
     * Sutherland-Hodgman над таким квадом тождественен, его можно отдать как есть.
     * Квады нецелевого формата считаются нетронутыми (passthrough).
     */
    private static boolean isFullyInside(BakedQuad quad, float[][] planes) {
        int[] v = quad.getVertices();
        int stride = v.length / 4;
        if (stride != VERTEX_SIZE) {
            return true;
        }
        for (float[] plane : planes) {
            final float a = plane[0], b = plane[1], c = plane[2], d = plane[3];
            for (int i = 0; i < 4; i++) {
                int o = i * stride;
                if (a * Float.intBitsToFloat(v[o])
                        + b * Float.intBitsToFloat(v[o + 1])
                        + c * Float.intBitsToFloat(v[o + 2]) + d < 0f) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void clipQuad(BakedQuad quad, float[][] planes, @Nullable float[] uvRect, List<BakedQuad> out) {
        int[] data = quad.getVertices();
        int vertexSize = data.length / 4;
        if (vertexSize != VERTEX_SIZE) {
            // Нецелевой формат (не BLOCK) — не режем, рисуем как есть.
            out.add(quad);
            return;
        }

        // Локальные ссылки на static-скратч (рендер однопоточный): свап без реассайна полей.
        int[][] src = polyA;
        int[][] dst = polyB;
        int count = 4;
        for (int i = 0; i < 4; i++) {
            System.arraycopy(data, i * vertexSize, src[i], 0, vertexSize);
        }

        for (float[] plane : planes) {
            count = clipPlane(src, count, plane, dst);
            if (count == 0) {
                return; // целиком снаружи — выбросить
            }
            int[][] tmp = src;
            src = dst;
            dst = tmp;
        }
        emitFan(src, count, quad, uvRect, out);
    }

    /**
     * Режет полигон {@code src}/{@code count} одной плоскостью в {@code dst},
     * возвращает число вершин результата.
     */
    private static int clipPlane(int[][] src, int count, float[] plane, int[][] dst) {
        final float a = plane[0], b = plane[1], c = plane[2], d = plane[3];
        int out = 0;
        // da можно кешировать: dist[i] = классификация вершины i.
        for (int i = 0; i < count; i++) {
            int[] v = src[i];
            dist[i] = a * Float.intBitsToFloat(v[0])
                    + b * Float.intBitsToFloat(v[1])
                    + c * Float.intBitsToFloat(v[2]) + d;
        }
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            boolean inI = dist[i] >= 0f;
            boolean inJ = dist[j] >= 0f;
            if (inI) {
                System.arraycopy(src[i], 0, dst[out], 0, VERTEX_SIZE);
                out++;
            }
            if (inI != inJ) {
                // t = da / (da - db)
                float t = dist[i] / (dist[i] - dist[j]);
                lerpVertex(src[i], src[j], t, dst[out]);
                out++;
            }
        }
        return out;
    }

    /** Линейная интерполяция position/uv/color; light и normal — от внутренней вершины {@code a}. */
    private static void lerpVertex(int[] a, int[] b, float t, int[] out) {
        out[0] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[0]), Float.intBitsToFloat(b[0]), t));
        out[1] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[1]), Float.intBitsToFloat(b[1]), t));
        out[2] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[2]), Float.intBitsToFloat(b[2]), t));
        out[3] = lerpColor(a[3], b[3], t);
        out[4] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[4]), Float.intBitsToFloat(b[4]), t));
        out[5] = Float.floatToRawIntBits(lerp(Float.intBitsToFloat(a[5]), Float.intBitsToFloat(b[5]), t));
        out[6] = a[6]; // light
        out[7] = a[7]; // normal
    }

    private static float lerp(float p, float q, float t) {
        return p + (q - p) * t;
    }

    /** Покомпонентная интерполяция пакованного цвета (r в младшем байте). */
    private static int lerpColor(int p, int q, float t) {
        int r = Math.round(lerp(p & 0xFF, q & 0xFF, t));
        int g = Math.round(lerp((p >> 8) & 0xFF, q & 0xFF, t));
        int b = Math.round(lerp((p >> 16) & 0xFF, q & 0xFF, t));
        int a = Math.round(lerp((p >> 24) & 0xFF, q & 0xFF, t));
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Фан-триангуляция полигона в квады: n==4 — как есть; n==3 — квад с дублем
     * последней вершины; n>=5 — квад + треугольник (прецедент GroupObject fan emit).
     * UV уже спроецированы (вход клипера при remap — мемо-список projectUv), поэтому
     * собранный квад наследует UV скратч-полигона как есть.
     */
    private static void emitFan(int[][] poly, int n, BakedQuad src, @Nullable float[] uvRect, List<BakedQuad> out) {
        if (n == 0) return;
        if (n == 4) {
            out.add(makeQuad(poly, 4, src, uvRect));
            return;
        }
        if (n == 3) {
            System.arraycopy(poly[2], 0, poly[3], 0, VERTEX_SIZE);
            out.add(makeQuad(poly, 4, src, uvRect));
            return;
        }
        // n >= 5: квад poly[0..3] + треугольник poly[0], poly[3], poly[4], poly[4].
        out.add(makeQuad(poly, 4, src, uvRect));
        // Треугольник собирается напрямую в packed (не трогаем poly[4] — он нужен дважды).
        int[] packed = new int[4 * VERTEX_SIZE];
        System.arraycopy(poly[0], 0, packed, 0, VERTEX_SIZE);
        System.arraycopy(poly[3], 0, packed, VERTEX_SIZE, VERTEX_SIZE);
        System.arraycopy(poly[4], 0, packed, 2 * VERTEX_SIZE, VERTEX_SIZE);
        System.arraycopy(poly[4], 0, packed, 3 * VERTEX_SIZE, VERTEX_SIZE);
        out.add(new BakedQuad(packed, src.getTintIndex(), src.getDirection(),
                src.getSprite(), src.isShade()));
    }

    /** Собирает BakedQuad из первых {@code n} вершин скратч-полигона. */
    private static BakedQuad makeQuad(int[][] poly, int n, BakedQuad src, @Nullable float[] uvRect) {
        int[] packed = new int[4 * VERTEX_SIZE];
        for (int i = 0; i < n; i++) {
            System.arraycopy(poly[i], 0, packed, i * VERTEX_SIZE, VERTEX_SIZE);
        }
        // Ремап нормализованных UV в атлас по rect'у скина — ОБЯЗАТЕЛЕН: без него
        // sprite-local [0..1] сэмплируют весь атлас («атлас протекает» в share-режиме).
        return new BakedQuad(remapUv(packed, uvRect), src.getTintIndex(), src.getDirection(),
                src.getSprite(), src.isShade());
    }

    /** Аффинный ремап UV в прямоугольник (u' = u0 + u*du), in-place по packed BLOCK-формату. */
    /** Ремап пакованных UV прямым rect'ом {u0,v0,du,dv} (null — как есть). */
    private static int[] remapUv(int[] packed, @org.jetbrains.annotations.Nullable float[] uvRect) {
        if (uvRect == null) return packed;
        return remapUv(packed, uvRect[0], uvRect[1], uvRect[2], uvRect[3]);
    }
    private static int[] remapUv(int[] packed, float u0, float v0, float du, float dv) {
        for (int v = 0; v < 4; v++) {
            int i = v * VERTEX_SIZE;
            packed[i + 4] = Float.floatToRawIntBits(u0 + Float.intBitsToFloat(packed[i + 4]) * du);
            packed[i + 5] = Float.floatToRawIntBits(v0 + Float.intBitsToFloat(packed[i + 5]) * dv);
        }
        return packed;
    }

    // ============ UV-тайлинг: разбивка тайловых квадов (только uvRect == null) ============

    /** Емкость скратч-полигона разбивки (квад x 4 полуплоскости тайла — максимум 8 вершин). */
    private static final int SPLIT_CAP = 8;
    private static final int[][] splitA = new int[SPLIT_CAP][VERTEX_SIZE];
    private static final int[][] splitB = new int[SPLIT_CAP][VERTEX_SIZE];
    private static final float[] splitDist = new float[SPLIT_CAP];
    /** Переиспользуемый выход разбивки одного квада (рендер однопоточный). */
    private static final java.util.ArrayList<BakedQuad> SPLIT_TMP = new java.util.ArrayList<>();

    /**
     * true, если квад тайловый: хоть одна UV вне атласного прямоугольника спрайта.
     * Формат строго BLOCK (тот же guard, что в splitTiledQuad — рассинхрон давал
     * исчезновение квадов), eps — по обеим осям.
     */
    /**
     * true, если все UV квада в диапазоне [−eps, 1+eps] — это sprite-local
     * (нормализованный) квад: разбивка тайлов ему не нужна, только ремап.
     */
    private static boolean isUnitRange(BakedQuad quad) {
        int[] v = quad.getVertices();
        if (v.length != 4 * VERTEX_SIZE) return true;
        final float eps = 1e-3f;
        for (int i = 0; i < 4; i++) {
            int o = i * VERTEX_SIZE;
            float u = Float.intBitsToFloat(v[o + 4]);
            float vv = Float.intBitsToFloat(v[o + 5]);
            if (u < -eps || u > 1f + eps || vv < -eps || vv > 1f + eps) {
                return false;
            }
        }
        return true;
    }
    private static boolean isTiledQuad(BakedQuad quad) {
        TextureAtlasSprite s = quad.getSprite();
        if (s == null) return false;
        int[] v = quad.getVertices();
        if (v.length != 4 * VERTEX_SIZE) return false;
        final float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        float eu = (u1 - u0) * 0.001f + 1e-5f;
        float ev = (v1 - v0) * 0.001f + 1e-5f;
        for (int i = 0; i < 4; i++) {
            int o = i * VERTEX_SIZE;
            float u = Float.intBitsToFloat(v[o + 4]);
            float vv = Float.intBitsToFloat(v[o + 5]);
            if (u < u0 - eu || u > u1 + eu || vv < v0 - ev || vv > v1 + ev) {
                return true;
            }
        }
        return false;
    }

    /**
     * Режет тайловый квад по целочисленным UV-тайлам (Sutherland-Hodgman в UV-
     * пространстве) и добавляет под-квады в out: позиция/цвет/свет лерпятся, UV
     * куска ремапится в атласный rect СПРАЙТА САМОГО КВАДА (ветка share-off).
     * Неразрезаемый (guard >64 тайлов, null-спрайт) — добавляется как есть:
     * потеря квада недопустима (прошлая версия молча роняла его — «дырявые створки»).
     */
    private static void splitTiledQuad(BakedQuad quad, java.util.List<BakedQuad> out,
                              @org.jetbrains.annotations.Nullable float[] uvRect) {
        TextureAtlasSprite s = quad.getSprite();
        int[] data = quad.getVertices();
        if (s == null || data.length != 4 * VERTEX_SIZE) {
            out.add(quad);
            return;
        }
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            int o = i * VERTEX_SIZE;
            float u = Float.intBitsToFloat(data[o + 4]);
            float vv = Float.intBitsToFloat(data[o + 5]);
            minU = Math.min(minU, u); maxU = Math.max(maxU, u);
            minV = Math.min(minV, vv); maxV = Math.max(maxV, vv);
        }
        int tu0 = floorI(minU), tu1 = floorI(maxU);
        int tv0 = floorI(minV), tv1 = floorI(maxV);
        if ((long) (tu1 - tu0 + 1) * (tv1 - tv0 + 1) > 64L) {
            out.add(quad); // патология — рисуем как есть, лучше артефакт чем дыра
            return;
        }
        // Целевой rect ремапа кусков: uvRect скина (share) или rect своего спрайта.
        final float su0 = uvRect != null ? uvRect[0] : s.getU0();
        final float sv0 = uvRect != null ? uvRect[1] : s.getV0();
        final float du = uvRect != null ? uvRect[2] : s.getU1() - s.getU0();
        final float dv = uvRect != null ? uvRect[3] : s.getV1() - s.getV0();
        for (int tu = tu0; tu <= tu1; tu++) {
            for (int tv = tv0; tv <= tv1; tv++) {
                int count = loadPoly(data, splitA);
                count = clipUvPlane(splitA, count, 1.0f, 0f, -(float) tu, splitB);          // u >= tu
                if (count == 0) continue;
                count = clipUvPlane(splitB, count, -1.0f, 0f, (float) tu + 1f, splitA);     // u <= tu+1
                if (count == 0) continue;
                count = clipUvPlane(splitA, count, 0f, 1.0f, -(float) tv, splitB);          // v >= tv
                if (count == 0) continue;
                count = clipUvPlane(splitB, count, 0f, -1.0f, (float) tv + 1f, splitA);     // v <= tv+1
                if (count == 0) continue;
                emitTile(splitA, count, quad, s, (float) tu, (float) tv, du, dv, out);
            }
        }
    }

    private static int floorI(float x) {
        return (int) Math.floor(x);
    }

    /** Загружает 4 вершины квада в скратч-полигон. */
    private static int loadPoly(int[] data, int[][] poly) {
        for (int i = 0; i < 4; i++) {
            System.arraycopy(data, i * VERTEX_SIZE, poly[i], 0, VERTEX_SIZE);
        }
        return 4;
    }

    /**
     * Режет полигон полуплоскостью в UV-пространстве: внутри = a*u + b*v + c >= 0.
     * Вершины полного формата (VERTEX_SIZE); t ребра считается по (u,v).
     */
    private static int clipUvPlane(int[][] src, int count, float a, float b, float c, int[][] dst) {
        int out = 0;
        for (int i = 0; i < count; i++) {
            int[] vt = src[i];
            splitDist[i] = a * Float.intBitsToFloat(vt[4]) + b * Float.intBitsToFloat(vt[5]) + c;
        }
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            boolean inI = splitDist[i] >= 0f;
            boolean inJ = splitDist[j] >= 0f;
            if (inI) {
                System.arraycopy(src[i], 0, dst[out], 0, VERTEX_SIZE);
                out++;
            }
            if (inI != inJ) {
                float dI = splitDist[i], dJ = splitDist[j];
                float t = Math.abs(dI - dJ) < 1e-9f ? 0.5f : dI / (dI - dJ);
                lerpVertex(src[i], src[j], t, dst[out]);
                out++;
            }
        }
        return out;
    }

    /** Собирает под-квады фан-триангуляцией; UV ремапится в атласный rect спрайта. */
    private static void emitTile(int[][] poly, int n, BakedQuad src, TextureAtlasSprite s,
                                 float tu, float tv, float du, float dv,
                                 java.util.List<BakedQuad> out) {
        if (n < 3) return;
        final float su0 = s.getU0(), sv0 = s.getV0();
        // UV -> атлас: u' = su0 + (u - tu) * du (в границах спрайта после клипа).
        for (int i = 0; i < n; i++) {
            int[] vt = poly[i];
            float u = Float.intBitsToFloat(vt[4]);
            float vv = Float.intBitsToFloat(vt[5]);
            vt[4] = Float.floatToRawIntBits(su0 + (u - tu) * du);
            vt[5] = Float.floatToRawIntBits(sv0 + (vv - tv) * dv);
        }
        // Фан: n==3 квад(0,1,2,2); n==4 как есть; n>=5 квад(0..3) + треугольники (0,k,k+1,k+1).
        if (n <= 4) {
            int[][] quadVerts = new int[][] { poly[0], poly[1], poly[2], poly[n - 1] };
            out.add(new BakedQuad(pack(quadVerts), src.getTintIndex(), src.getDirection(),
                    src.getSprite(), src.isShade()));
            return;
        }
        int[][] head = new int[][] { poly[0], poly[1], poly[2], poly[3] };
        out.add(new BakedQuad(pack(head), src.getTintIndex(), src.getDirection(),
                src.getSprite(), src.isShade()));
        for (int k = 3; k + 1 < n; k++) {
            int[][] tri = new int[][] { poly[0], poly[k], poly[k + 1], poly[k + 1] };
            out.add(new BakedQuad(pack(tri), src.getTintIndex(), src.getDirection(),
                    src.getSprite(), src.isShade()));
        }
    }

    private static int[] pack(int[][] verts) {
        int[] packed = new int[4 * VERTEX_SIZE];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(verts[i], 0, packed, i * VERTEX_SIZE, VERTEX_SIZE);
        }
        return packed;
    }
}
