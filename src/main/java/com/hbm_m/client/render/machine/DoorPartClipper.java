package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;

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
        // isIdentityRect-rect математически тождественен отсутствию ремапа — не плодим
        // мемо-запись (тот же guard, что ModelHelper.expandQuadUvsUnit в fallback-пути).
        boolean remap = uvRect != null
                && !com.hbm_m.client.model.ModelHelper.isIdentityRect(uvRect);
        List<BakedQuad> src = remap ? projectUv(quads, uvRect) : quads;
        List<BakedQuad> out = null;
        for (int i = 0; i < src.size(); i++) {
            BakedQuad quad = src.get(i);
            // Fast-path: все 4 вершины внутри всех плоскостей — квад как есть
            // (без копирования и нового BakedQuad). При remap квад уже спроецирован
            // мемо-списком, повторная UV-операция не нужна.
            if (isFullyInside(quad, planes)) {
                if (out != null) {
                    out.add(quad);
                }
                continue;
            }
            out = copyHead(out, src, i);
            clipQuad(quad, planes, out);
        }
        // Ни один квад не изменён — входной список без аллокаций.
        return out != null ? out : src;
    }

    // ==================== Кеш проекции (remap uvRect -> атлас) ====================

    /** Ёмкость мемо-кеша проекции (пар×скинов в кадре — единицы). */
    private static final int MEMO_CAP = 8;

    /** Запись кеша: rect — снапшот ЗНАЧЕНИЙ (НЕ ссылка на UV_RECT_SCRATCH). */
    private static final class MemoEntry {
        final long epoch;
        final float u0, v0, du, dv;
        final List<BakedQuad> src;
        List<BakedQuad> projected;

        MemoEntry(long epoch, float[] rect, List<BakedQuad> src) {
            this.epoch = epoch;
            this.u0 = rect[0];
            this.v0 = rect[1];
            this.du = rect[2];
            this.dv = rect[3];
            this.src = src;
        }
    }

    /** Мемо-кеш проекции (рендер однопоточный); вытеснение — самая старая запись. */
    private static final ArrayList<MemoEntry> MEMO = new ArrayList<>(MEMO_CAP);

    /** Проецирует квады в атлас текущим rect'ом с мемо-кешем (см. javadoc класса). */
    private static List<BakedQuad> projectUv(List<BakedQuad> quads, float[] uvRect) {
        long epoch = com.hbm_m.client.render.implementations.MachineDoorRenderer.shareEpoch();
        for (int i = 0; i < MEMO.size(); i++) {
            MemoEntry e = MEMO.get(i);
            if (e.epoch == epoch && e.src == quads
                    && e.u0 == uvRect[0] && e.v0 == uvRect[1]
                    && e.du == uvRect[2] && e.dv == uvRect[3]) {
                if (e.projected == null) {
                    e.projected = remapQuads(quads, e.u0, e.v0, e.du, e.dv);
                }
                return e.projected;
            }
        }
        MemoEntry fresh = new MemoEntry(epoch, uvRect, quads);
        fresh.projected = remapQuads(quads, fresh.u0, fresh.v0, fresh.du, fresh.dv);
        if (MEMO.size() >= MEMO_CAP) {
            MEMO.remove(0);
        }
        MEMO.add(fresh);
        return fresh.projected;
    }

    /** Сборка спроецированного списка (аллокация — только на мемо-промахе). */
    private static List<BakedQuad> remapQuads(List<BakedQuad> quads, float u0, float v0, float du, float dv) {
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            int[] verts = quad.getVertices();
            if (verts.length != 4 * VERTEX_SIZE) {
                out.add(quad); // нецелевой формат — passthrough
                continue;
            }
            int[] packed = verts.clone();
            remapUv(packed, u0, v0, du, dv);
            out.add(new BakedQuad(packed, quad.getTintIndex(), quad.getDirection(),
                    quad.getSprite(), quad.isShade()));
        }
        return out;
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

    private static void clipQuad(BakedQuad quad, float[][] planes, List<BakedQuad> out) {
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
        emitFan(src, count, quad, out);
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
    private static void emitFan(int[][] poly, int n, BakedQuad src, List<BakedQuad> out) {
        if (n == 0) return;
        if (n == 4) {
            out.add(makeQuad(poly, 4, src));
            return;
        }
        if (n == 3) {
            System.arraycopy(poly[2], 0, poly[3], 0, VERTEX_SIZE);
            out.add(makeQuad(poly, 4, src));
            return;
        }
        // n >= 5: квад poly[0..3] + треугольник poly[0], poly[3], poly[4], poly[4].
        out.add(makeQuad(poly, 4, src));
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
    private static BakedQuad makeQuad(int[][] poly, int n, BakedQuad src) {
        int[] packed = new int[4 * VERTEX_SIZE];
        for (int i = 0; i < n; i++) {
            System.arraycopy(poly[i], 0, packed, i * VERTEX_SIZE, VERTEX_SIZE);
        }
        return new BakedQuad(packed, src.getTintIndex(), src.getDirection(),
                src.getSprite(), src.isShade());
    }

    /** Аффинный ремап UV в прямоугольник (u' = u0 + u*du), in-place по packed BLOCK-формату. */
    private static int[] remapUv(int[] packed, float u0, float v0, float du, float dv) {
        for (int v = 0; v < 4; v++) {
            int i = v * VERTEX_SIZE;
            packed[i + 4] = Float.floatToRawIntBits(u0 + Float.intBitsToFloat(packed[i + 4]) * du);
            packed[i + 5] = Float.floatToRawIntBits(v0 + Float.intBitsToFloat(packed[i + 5]) * dv);
        }
        return packed;
    }
}
