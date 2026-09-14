package com.hbm_m.client.compat.dh;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;

/**
 * Утилиты матриц для дальнего прохода EngineHandler (AFTER_WEATHER):
 * чистая удлинённая проекция (FOV камеры + боб + far plane в миллионы блоков)
 * и кросс-версионная установка RenderSystem-матриц через рефлексию.
 *
 * Рендер дальнего контента (меши ракет, NT-частицы) выполняется самим
 * EngineHandler в главном FBO — рисование в DH FBO полностью удалено
 * (запись геометрии в их FBO отравляла композит apply.frag — «чёрная земля»).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
 *///?}
public final class DhClientCompat {

    private DhClientCompat() {}


    /** Far plane растянут до миллионов блоков, остальное — как у ванили. */
    private static final float EXTENDED_NEAR = 0.05F;
    private static final float EXTENDED_FAR = 8_000_000F;

    /** Плоскости расширенной проекции — для энкода копии DH-глубины (DhDepthCopy). */
    public static float extendedNear() { return EXTENDED_NEAR; }
    public static float extendedFar() { return EXTENDED_FAR; }

    /** Чистая ванильная проекция кадра (см. captureVanillaProjection). */
    private static volatile Matrix4f capturedVanillaProjection;
    /** Ожидаемый FOV (градусы), при котором была захвачена capturedVanillaProjection. */
    private static volatile float capturedFovDeg = -1.0F;

    /**
     * Захват чистой ванильной проекции кадра.
     *
     * RenderSystem-проекция пригодна НЕ ВСЕГДА: поздние фазы кадра (наши же
     * подмены/чужие моды) могут оставить там нестандартную матрицу, поэтому
     * источник проверяется санити-чеком (см. isSanePerspective — ВАЖНО про
     * индексы JOMЛ!) И FOV-валидацией (см. fovMatches). Источники по приоритету:
     *  1. Iris CapturedRenderingState.gbufferProjection — параметр renderLevel
     *     текущего кадра (чистая перспектива с бобом);
     *  2. RenderSystem — если прошла санити + FOV;
     *  3. реконструкция: публичный GameRenderer.getProjectionMatrix(getFov)
     *     (включает zoom) + bobHurt/bobView — точная ванильная последовательность.
     */
    public static void captureVanillaProjection(float partialTick) {
        double expectedFov = expectedFovDeg(partialTick);
        Matrix4f candidate = null;

        // 1. Iris gbufferProjection.
        if (candidate == null) {
            Matrix4f iris = captureFromIris();
            if (iris != null && isSanePerspective(iris) && fovMatches(iris, expectedFov)) {
                candidate = iris;
            }
        }

        // 2. RenderSystem — только если прошёл санити + FOV (может быть утёкшей
        //    чужой матрицей: в логах ловили интермиттирующую перспективу с
        //    fovY≈10° (m00≈6.43 против нормальных 0.39) — посторонний мод
        //    оставляет её в глобальном состоянии, и без FOV-проверки она
        //    проходила санити и отравляла ОБА прохода NT-частиц).
        if (candidate == null) {
            Matrix4f rs = RenderSystem.getProjectionMatrix();
            if (rs != null && isSanePerspective(rs) && fovMatches(rs, expectedFov)) {
                candidate = new Matrix4f(rs);
            }
        }

        // 3. Реконструкция без Iris: точная ванильная последовательность кадра.
        //    Санина по построению (getFovCompat — тот же источник ожидаемого FOV).
        if (candidate == null) {
            candidate = reconstructVanillaProjection(partialTick);
        }

        if (candidate != null && isSanePerspective(candidate)) {
            capturedVanillaProjection = new Matrix4f(candidate);
            capturedFovDeg = (float) expectedFov;
        } else {
            if (++captureRejectDiag % 300 == 1) {
                com.hbm_m.main.MainRegistry.LOGGER.debug(
                        "HBM capture VANILLA PROJ REJECTED: {}",
                        candidate == null ? "no source"
                                : String.format("joml m22=%.5f m23=%.5f m32=%.5f m33=%.5f",
                                        candidate.m22(), candidate.m23(), candidate.m32(), candidate.m33()));
            }
        }
    }

    /**
     * Ожидаемый вертикальный FOV кадра (градусы).
     *
     * ПРИОРИТЕТ — фактическая матрица кадра (event.getProjectionMatrix,
     * см. offerFrameProjection): fovY = 2*atan(1/m11). Раньше здесь была
     * рефлексия GameRenderer.getFov — в production-именовании SRG она НЕ
     * находится ("getFov" vs "m_109090_"), фолбэки возвращали «сырой» FOV
     * настроек, и вся FOV-валидация + реконструкция теряли spyglass-зум и
     * speed-FOV: частицы/меш не приближались в spyglass и не следовали за
     * изменением FOV. Матрица кадра — истина по определению.
     */
    private static double expectedFovDeg(float partialTick) {
        Matrix4f frame = eventFrameProjection;
        if (frame != null && Math.abs(frame.m11()) > 1.0E-6F) {
            double fovY = Math.toDegrees(2.0 * Math.atan(1.0 / frame.m11()));
            if (fovY > 1.0 && fovY < 175.0) {
                return fovY;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        return getFovCompat(mc.gameRenderer, mc.gameRenderer.getMainCamera(), partialTick);
    }

    private static long useRejectDiag = 0;

    /** Рейтлимитированный лог: захват был, но забракован при использовании (отравлен). */
    private static void logCaptureRejectedAtUse() {
        if (++useRejectDiag % 300 == 1) {
            Matrix4f c = capturedVanillaProjection;
            com.hbm_m.main.MainRegistry.LOGGER.debug(
                    "HBM capture REJECTED AT USE x{}: storedFov={}, actualFovY={}{}",
                    useRejectDiag,
                    String.format("%.2f", capturedFovDeg),
                    c == null ? "n/a" : String.format("%.2f", Math.toDegrees(2.0 * Math.atan(1.0 / c.m11()))),
                    " — fallback to clean perspective");
        }
    }

    /**
     * FOV-валидация: фактический вертикальный угол кандидата (2*atan(1/m11))
     * не должен расходиться с ожидаемым сильнее допуска. Допуск ~±30%:
     * покрывает speed-эффекты/динамику FOV (ожидание берётся из того же
     * GameRenderer.getFov), но отсекает ЧУЖИЕ матрицы — пойманная утечка имела
     * fovY≈10° против нормальных ~70°, расхождение ×7.
     */
    private static boolean fovMatches(Matrix4f m, double expectedDeg) {
        if (m == null || Math.abs(m.m11()) < 1.0E-6F || expectedDeg <= 0.0) return false;
        double actualDeg = Math.toDegrees(2.0 * Math.atan(1.0 / m.m11()));
        return actualDeg > expectedDeg * 0.72 && actualDeg < expectedDeg * 1.38;
    }

    /**
     * Пригодность захвата ПРЯМО СЕЙЧАС: матрица есть и её FOV согласуется с
     * текущим ожидаемым (к моменту использования кадр тот же — расхождений
     * быть не должно; расхождение = захват устарел/отравлен).
     */
    private static boolean capturedUsable(float partialTick) {
        Matrix4f c = capturedVanillaProjection;
        if (c == null || capturedFovDeg <= 0.0F) return false;
        double expected = expectedFovDeg(partialTick);
        double stored = capturedFovDeg;
        if (expected <= 0.0) return true;
        return stored > expected * 0.72 && stored < expected * 1.38;
    }

    /**
     * Нормальная перспектива: w-строка [0,0,-1,0].
     *
     * ВНИМАНИЕ НА ИНДЕКСЫ JOML: mXY = КОЛОНКА X, СТРОКА Y (не строка-колонка!).
     * Стандартная перспективная матрица:
     *   joml m23() == -1      (w-строка, коэф. z — то, что GLSL-имя M[3][2])
     *   joml m32() == -2fn/(f-n) ≈ -0.1  (глубинный коэффициент, НЕ -1!)
     *   joml m33() == 0
     * Ранняя версия проверки требовала m32()==-1 — из-за путаницы
     * строка/колонка она ОТВЕРГАЛА каждую нормальную проекцию, захват
     * всегда падал, и весь дальний/ближний контент рисовался без боба —
     * отсюда «гриб улетает» при любом движении камеры.
     */
    private static boolean isSanePerspective(Matrix4f m) {
        return Math.abs(m.m23() + 1.0F) <= 0.01F && Math.abs(m.m33()) <= 0.001F;
    }

    // ── Источник 1: Iris CapturedRenderingState ─────────────────────────────
    private static Object irisCapturedStateInstance;
    private static java.lang.reflect.Method irisGetGbufferProjection;
    private static volatile boolean irisCaptureInitialized = false;

    private static Matrix4f captureFromIris() {
        if (!irisCaptureInitialized) {
            irisCaptureInitialized = true;
            try {
                Class<?> cl = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");
                irisCapturedStateInstance = cl.getField("INSTANCE").get(null);
                irisGetGbufferProjection = cl.getMethod("getGbufferProjection");
            } catch (Throwable ignored) {}
        }
        if (irisCapturedStateInstance == null || irisGetGbufferProjection == null) {
            return null;
        }
        try {
            Object proj = irisGetGbufferProjection.invoke(irisCapturedStateInstance);
            if (proj instanceof org.joml.Matrix4fc m) {
                return new Matrix4f(m);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ── Источник 2: реконструкция ванильной последовательности кадра ────────
    /**
     * GameRenderer.getProjectionMatrix(getFov(camera,f,true)) [публичный,
     * включает zoom] -> mul(bobHurt -> bobView). Точное повторение
     * GameRenderer.renderLevel до resetProjectionMatrix. Nausea-трансформ
     * не реплицируется (только под эффектом тошноты, редкий кейс).
     */
    private static Matrix4f reconstructVanillaProjection(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        var gr = mc.gameRenderer;
        var cam = gr.getMainCamera();
        try {
            double fovDeg = getFovCompat(gr, cam, partialTick);
            Matrix4f base = gr.getProjectionMatrix(fovDeg);

            PoseStack bob = new PoseStack();
            float hurtTick;
            //? if < 1.21.1 {
            hurtTick = partialTick;
            //?} else {
            /*hurtTick = cam.getPartialTickTime();
             *///?}
            invokeBob(gr, "bobHurt", bob, hurtTick);
            if (mc.options.bobView().get()) {
                invokeBob(gr, "bobView", bob, partialTick);
            }
            base.mul(bob.last().pose());
            return base;
        } catch (Throwable t) {
            if (++captureRejectDiag % 300 == 1) {
                com.hbm_m.main.MainRegistry.LOGGER.debug(
                        "HBM reconstruct proj failed: {}", t.toString());
            }
            return null;
        }
    }

    /** Рефлексия к приватным GameRenderer.bobHurt/bobView(PoseStack,float).
     *  Method-объекты кешируются: вызывается каждый кадр (reconstructVanillaProjection),
     *  getDeclaredMethod по строкам в горячем цикле — микроаллокации. */
    private static java.lang.reflect.Method bobHurtMethod;
    private static java.lang.reflect.Method bobViewMethod;

    private static void invokeBob(net.minecraft.client.renderer.GameRenderer gr, String name, PoseStack poseStack, float partialTick) throws Exception {
        boolean hurt = "bobHurt".equals(name);
        java.lang.reflect.Method m = hurt ? bobHurtMethod : bobViewMethod;
        if (m == null) {
            m = net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethod(name, PoseStack.class, float.class);
            m.setAccessible(true);
            if (hurt) bobHurtMethod = m; else bobViewMethod = m;
        }
        m.invoke(gr, poseStack, partialTick);
    }

    private static int captureRejectDiag = 0;

    /**
     * Удлинённая проекция дальнего прохода: ЗАХВАЧЕННАЯ ванильная матрица
     * кадра с заменённым только far plane (m22/m23).
     *
     * КРИТИЧНО: НЕ наследуем RenderSystem.getProjectionMatrix() во время
     * самого прохода — к моменту AFTER_WEATHER там лежит МУСОРНАЯ матрица
     * (транспонированная перспектива, w-строка [0,0,-0.1,0]; гибрид с ней
     * клипал весь дальний контент). Поэтому захват делается заранее,
     * а здесь только подменяются m22/m23 у заведомо нормальной базы
     * (m32 == -1 проверяется при захвате).
     *
     * РАННЕЕ здесь пытались реплицировать view bobbing вручную
     * (bobHurt/bobView рефлексией поверх чистой перспективы) — это давало
     * смещение гриба: остаточный трансформ боба не нулевой даже в полёте,
     * а любое расхождение реплики с ванилью видно на стыке near/far.
     * Захват реальной матрицы устраняет проблему целиком.
     */
    /**
     * Удлинённая проекция дальнего прохода.
     *
     * ВАЖНО ПРО БОБ: матрица дальнего контента должна СОВПАДАТЬ с той,
     * которой рисовался видимый мир, иначе возникает угловое рассогласование
     * (ошибка в долях градуса × дистанция = десятки блоков визуального
     * сдвига — «гриб уезжает от кратера / уходит под землю»).
     *
     *  - БЕЗ пака: мир рисуется через RenderSystem-проекцию, куда ваниль
     *    запекла FOV/zoom/bob/nausea — используем её захват (AFTER_SKY).
     *  - ПОД ПАКОМ IRIS: террейн рендерят pack-программы через
     *    gbufferProjection БЕЗ view-bob (боб паковщики компенсируют сами),
     *    поэтому боб в нашей матрице давать НЕЛЬЗЯ — строим чистую
     *    перспективу из FOV камеры.
     */
    private static Matrix4f cleanExtendedProjection(float partialTick) {
        boolean irisActive = com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
        // FOV-валидация ПРИ ИСПОЛЬЗОВАНИИ: захваченная матрица могла быть
        // отравлена утечкой чужой перспективы (fovY≈10°) — рисовать с ней
        // нельзя, падаем на чистую реконструкцию из FOV камеры.

        // ИСТОЧНИК 0 (приоритетный, в т.ч. ПОД IRIS): матрица кадра из
        // RenderLevelStageEvent.getProjectionMatrix() — это ТО ЖЕ, что
        // gbufferProjection, которой пак рисовал террейн: актуальный speed-
        // FOV (ускорение/спринт), zoom, без боба (боб — на стороне
        // modelview, пак компенсирует сам). Без неё под Iris строилась
        // ЧИСТАЯ перспектива из FOV — при изменении FOV она расходилась с
        // реальной проекцией кадра, и частицы «отлетали» от своих мест.
        Matrix4f captured = frameProjectionUsable(partialTick) ? eventFrameProjection : null;

        if (captured == null && !irisActive && capturedUsable(partialTick)) {
            captured = capturedVanillaProjection;
        }
        if (captured == null && !irisActive && capturedVanillaProjection != null) {
            logCaptureRejectedAtUse();
        }
        if (captured != null) {
            Matrix4f p = new Matrix4f(captured);
            p.m22((EXTENDED_FAR + EXTENDED_NEAR) / (EXTENDED_NEAR - EXTENDED_FAR));
            // ИНДЕКСЫ JOML (mXY = колонка X, строка Y): глубинный коэффициент
            // 2fn/(n-f) живёт в m32, а m23 — элемент w-строки, у перспективы он
            // ВСЕГДА -1. Запись в m23 затирала w-строку (w' = -0.1*z вместо
            // z-независимого), оставляя ванильный мусор в m32: ndc.z ≈ 10 при
            // любой дистанции → дальний контент целиком клипался за NDC
            // («DH-пасс не рендерится», см. far draw probe: ndcZ≈9.98).
            p.m32(2.0F * EXTENDED_NEAR * EXTENDED_FAR / (EXTENDED_NEAR - EXTENDED_FAR));
            return p;
        }
        return cleanVanillaPerspective(partialTick);
    }

    /**
     * Матрица кадра, переданная в RenderLevelStageEvent текущего кадра
     * ({@link #offerFrameProjection}). null — кадр без захвата.
     */
    private static volatile Matrix4f eventFrameProjection;

    /**
     * Захват точной проекции уровня (вызывается из RenderLevelStageEvent
     * каждый кадр ДО наших проходов). Матрица проходит санити + FOV-
     * валидацию при offer, чтобы отравленная/чужая матрица не дожила до
     * отрисовки.
     */
    public static void offerFrameProjection(Matrix4f projection) {
        if (projection == null) {
            eventFrameProjection = null;
            return;
        }
        // ВАЖНО: БЕЗ fovMatches против ожидаемого FOV — сама эта матрица и
        // есть эталон FOV кадра (см. expectedFovDeg). Сравнение с рефлексией
        // getFov в production (SRG-имена, рефлексия недоступна → «сырой» FOV
        // настроек) отбраковывало зумленную матрицу spyglass/speed-FOV, и
        // частицы+меш переставали приближаться. Остаётся санити перспективы.
        if (isSanePerspective(projection)) {
            eventFrameProjection = new Matrix4f(projection);
        }
    }

    /** Захват свежий (этот же кадр) и годится по санити/FOV. */
    private static boolean frameProjectionUsable(float partialTick) {
        Matrix4f m = eventFrameProjection;
        if (m == null) {
            return false;
        }
        double expectedFov = expectedFovDeg(partialTick);
        return isSanePerspective(m) && fovMatches(m, expectedFov);
    }

    /** Чистая перспектива из FOV камеры с расширенным far plane, без боба. */
    private static Matrix4f cleanVanillaPerspective(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        double fovDeg = getFovCompat(mc.gameRenderer, mc.gameRenderer.getMainCamera(), partialTick);
        var win = mc.getWindow();
        return new Matrix4f().perspective(
                (float) (fovDeg * (Math.PI / 180.0)),
                (float) win.getWidth() / (float) win.getHeight(),
                EXTENDED_NEAR, EXTENDED_FAR);
    }

    /** GameRenderer.getFov(Camera,float,boolean) приватен — рефлексия, затем фолбэки.
     *  ВАЖНО: в production-рантайме имена SRG — "getFov" не находится, поэтому
     *  первым фолбэком идёт m_109090_ (без него терялись spyglass/speed-FOV).
     *  Method-объекты кешируются: getFov вызывается несколько раз за кадр. */
    private static java.lang.reflect.Method getFovMethod;
    private static java.lang.reflect.Method getFovSrgMethod;

    private static double getFovCompat(net.minecraft.client.renderer.GameRenderer gr, net.minecraft.client.Camera cam, float partialTick) {
        try {
            java.lang.reflect.Method m = getFovMethod;
            if (m == null) {
                m = net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethod("getFov", net.minecraft.client.Camera.class, float.class, boolean.class);
                m.setAccessible(true);
                getFovMethod = m;
            }
            Object r = m.invoke(gr, cam, partialTick, true);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Method m = getFovSrgMethod;
            if (m == null) {
                m = net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethod("m_109090_", net.minecraft.client.Camera.class, float.class, boolean.class);
                m.setAccessible(true);
                getFovSrgMethod = m;
            }
            Object r = m.invoke(gr, cam, partialTick, true);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {}
        return Minecraft.getInstance().options.fov().get();
    }

    /**
     * Удлинённая чистая проекция для ВАНИЛЬНОГО прохода дальних частиц:
     * гигантские билборды у границы far plane не клипаются, шов между
     * near/far контентом отсутствует.
     */
    public static void beginVanillaExtendedPass(float partialTick) {
        RenderSystem.backupProjectionMatrix();
        setProjectionMatrix(cleanExtendedProjection(partialTick));
        forceVertexSortingNonNull();
    }

    public static void endVanillaExtendedPass() {
        RenderSystem.restoreProjectionMatrix();
    }

    /**
     * Ближний проход — В ТОЙ ЖЕ расширенной проекции, что и дальний.
     *
     * ПОЧЕМУ ТАК ЧЕСТНЕЕ: depth-тест корректен только внутри ОДНОЙ конвенции.
     * Раньше ближний бакет рисовался в захваченной ванильной проекции
     * (fn_eff ≈ 0.05), а копия DH-глубины кодирована в расширенной (fn_eff =
     * 0.1) — прямое сравнение давало: партикл на дистанции D отсекался любым
     * LOD дальше 2·D, т.е. гриб рисуется «за скалой», стоя ПЕРЕД ней
     * (0.1/d_LOD < 0.05/d_NT ⟺ d_NT < d_LOD/2). Единая проекция устраняет
     * весь этот класс ошибок; расхождение с ванильным террейном (тоже другая
     * конвенция) даёт лишь ограниченную погрешность «NT поверх террейна,
     * который вдвое ближе» — направление безопасное, а террейн у ближнего
     * контента почти всегда ниже/дальше по лучу.
     */
    public static void beginCapturedVanillaPass(float partialTick) {
        RenderSystem.backupProjectionMatrix();
        setProjectionMatrix(cleanExtendedProjection(partialTick));
        forceVertexSortingNonNull();
    }

    private static final String VERTEX_SORTING_CLASS = "com.mojang.blaze3d.vertex.VertexSorting";

    /**
     * «Безопасная» сортировка для наших дальних отрисовок.
     * ВАЖНО: на 1.20.x есть VertexSorting.UNSORTED, но на 1.21.1 его НЕТ
     * (только DISTANCE_TO_ORIGIN / ORTHOGRAPHIC_Z) — поэтому цепочка фолбэков.
     * Передавать null в setProjectionMatrix(proj, sorting) НЕЛЬЗЯ:
     * поле vertexSorting станет null и Iris/Sodium упадут с NPE при
     * сортировке полупрозрачных квадов.
     */
    private static Object vertexSortingSafe() {
        Class<?> cl;
        try {
            cl = Class.forName(VERTEX_SORTING_CLASS);
        } catch (Throwable t) {
            return null;
        }
        // UNSORTED НЕ используем: квады полупрозрачного гриба должны
        // сортироваться по дистанции до камеры (вершины camera-relative,
        // origin == камера). ORTHOGRAPHIC_Z даёт неверный порядок на
        // перспективной проекции (кольца «впереди» шляпки).
        for (String name : new String[] {"DISTANCE_TO_ORIGIN", "ORTHOGRAPHIC_Z"}) {
            try {
                Object v = cl.getField(name).get(null);
                if (v != null) return v;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static final Object VERTEX_SORTING_SAFE = vertexSortingSafe();

    private static java.lang.reflect.Field vertexSortingField;

    /**
     * Если текущий vertexSorting == null (его обнулил одиночный setProjectionMatrix),
     * выставляем VertexSorting.UNSORTED напрямую в приватное поле.
     * На 1.20.1-forge поле в SRG-именах и там это не нужно (2-арг метод
     * принимает сортировку явно) — молча игнорируем неудачу.
     * Field кешируется: вызывается на границе каждого нашего прохода.
     */
    private static void forceVertexSortingNonNull() {
        if (VERTEX_SORTING_SAFE == null) return;
        try {
            java.lang.reflect.Field f = vertexSortingField;
            if (f == null) {
                f = RenderSystem.class.getDeclaredField("vertexSorting");
                f.setAccessible(true);
                vertexSortingField = f;
            }
            if (f.get(null) == null) {
                f.set(null, VERTEX_SORTING_SAFE);
            }
        } catch (Throwable ignored) {}
    }

    /** Кеш методов RenderSystem.setProjectionMatrix (вызывается 3-5 раз за кадр
     *  при входе/выходе из расширенных проходов — поиск по getMethods() в
     *  горячем цикле убран). */
    private static java.lang.reflect.Method setProjMethod1Arg;
    private static java.lang.reflect.Method setProjMethod2Arg;
    private static boolean setProjMethodsResolved;

    private static void setProjectionMatrix(Matrix4f mat) {
        // Handle both 1.20.1 (2-arg) and 1.21.1+ (1-arg) signatures via reflection,
        // avoiding compile-time dependency on VertexSorting location which moved.
        if (!setProjMethodsResolved) {
            setProjMethodsResolved = true;
            try {
                var m1 = RenderSystem.class.getMethod("setProjectionMatrix", Matrix4f.class);
                if (m1.getParameterCount() == 1) {
                    setProjMethod1Arg = m1;
                }
            } catch (Throwable ignored) {}
            if (setProjMethod1Arg == null) {
                // 2-arg variant: second param may be VertexSorting in different packages
                for (var m : RenderSystem.class.getMethods()) {
                    if (!m.getName().equals("setProjectionMatrix")) continue;
                    if (m.getParameterCount() != 2) continue;
                    if (m.getParameterTypes()[0] != Matrix4f.class) continue;
                    setProjMethod2Arg = m;
                    break;
                }
            }
        }
        try {
            if (setProjMethod1Arg != null) {
                setProjMethod1Arg.invoke(null, mat);
                return;
            }
            if (setProjMethod2Arg != null) {
                // VertexSorting (интерфейс на всех версиях): UNSORTED безопасен.
                setProjMethod2Arg.invoke(null, mat, vertexSortingSafe());
                return;
            }
        } catch (Throwable ignored) {}
        // Fallback: try calling 1-arg via accessible method even if public lookup failed
        try {
            var m = RenderSystem.class.getDeclaredMethod("setProjectionMatrix", Matrix4f.class);
            m.setAccessible(true);
            m.invoke(null, mat);
        } catch (Throwable ignored) {}
    }

}