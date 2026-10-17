package com.hbm_m.client.render.shader;

import com.hbm_m.client.compat.dh.DhClientState;
import com.hbm_m.client.compat.dh.DhOcclusionGpu;
import com.hbm_m.client.render.GlVaoSafety;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.mojang.blaze3d.platform.GlStateManager;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Позиционный свет ядерного взрыва, свет v2 (мягкие тени и нормали): raw-GL
 * fullscreen-пасс, инъецируемый в TAIL {@code finalizeLevelRendering}
 * (Iris/Oculus) и рисующий ПРЯМО В ГЛАВНЫЙ render target Minecraft -
 * единственную цель, которая доходит до экрана на любом пайплайне Oculus
 * (перехват 2026-10-11: после renderAll() привязан ванильный main FBO;
 * флип-буферы Iris на пересозданных пайплайнах недоступны снаружи - кэш
 * биндингов композитов).
 *
 * <p>Свет v2: до K источников (живые NukeTorex: x, y, z, heat из ParticleEngineNT,
 * y поднимается на {@link #LIGHT_SOURCE_LIFT} к центру огненного шара НА CPU),
 * радиальный гейт R_i = {@link #R_UNIT} * масштаб гриба с затуханием fall²,
 * тёплый цвет. NdotL по нормалям из
 * глубины (min-of-diffs, 3 тапа, НЕ dFdx/dFdy); 16-шаговый raymarch вдоль луча
 * пиксель→источник - мягкие тени (репроекция world→uv через uModelView/uProj,
 * thickness-гейт, dither-старт против бандинга, полная тень = SHADOW_FLOOR = 0.01,
 * почти в ноль (round-7));
 * пост-тонемап компенсация pow(1/2.2) - мы аддитивим ПОСЛЕ tonemap пака.
 * Sky-пиксели (гриб/облака глубину НЕ пишут, небо = depth 1.0) - воздушная
 * подсветка по лучу взгляда, без нормалей/теней.</p>
 *
 * <p>Компромиссы TAIL: пост-тонемап - свет не блумится паком (имитируется мягким
 * широким затуханием; приближение обратной гаммы - отчёт §7.12) и не проходит
 * через TAA пакета. Взамен - ПОЛНАЯ стабильность на всех паках и reload'ах.</p>
 *
 * <p>Форк {@link RawDhDepthCopy}: свой VAO/VBO квада, свой raw-GL program вне
 * MC/Iris-пайплайна, failure-latch для структурных неудач (сбой отдельного
 * кадра терпит до 60 подряд - см. MAX_CONSECUTIVE_FAILURES); transient-состояния
 * (пайплайн пересобирается) - тихий скип кадра. Полный save/restore GL-стейта
 * по чек-листу отчёта §7.14; GL_NEAREST на depth-биндингах с восстановлением.
 * Iris/Oculus - опциональная зависимость: ноль Iris-импортов, рефлексия
 * (CapturedRenderingState для gbuffer modelView, инверсия своя); без Iris - тихий no-op.</p>
 *
 * <p>Стейт-мутации пасса (blend/colorMask/cull/depth/scissor/viewport) - ТОЛЬКО
 * raw GL (GL11/GL14): Mojiang-вызовы (GlStateManager/RenderSystem) под Oculus
 * перехвачены Iris-миксинами (MixinGlStateManager_BlendOverride/
 * DepthColorOverride) и при активном локе отменяются с отложенными аргументами -
 * реального GL-вызова нет. Raw GL не перехватывается никем; чтение прежних
 * значений тоже raw, restore raw - кэш GlStateManager синхронизируется с
 * реальностью. FBO/текстурные биндинги - через GlStateManager (доказанно
 * работают). Матрицы кадра снапшотятся в HEAD finalizeLevelRendering
 * (см. {@link com.hbm_m.mixin.client.MixinIrisRenderingPipelinePreComposite}):
 * снапшот оборонительный - в Iris 1.8.14/Oculus 1.8 композиты
 * CapturedRenderingState НЕ перезаписывают (аудит IRIS-F1: сеттеры - один
 * вызов за кадр на renderLevel HEAD), но на прочих версиях гарантий нет.</p>
 */
public final class RawScreenSpaceLight {

    private RawScreenSpaceLight() {}

    /** Максимум одновременных взрывов-источников. */
    private static final int MAX_SOURCES = 4;
    /** Единичный радиус света, блоки: пер-источниковый радиус = R_UNIT * масштаб
     * гриба (NukeTorex.getScale, clamp 0.5..5 из NukeTorexCreator). Round-5
     * (R-1/R-2/R-3): раньше радиус был один на все взрывы - усилитель менял
     * только размер гриба и время жизни, сфера света всегда была 768. Границы
     * 384..3840 наследуются от clamp масштаба, новый кламп не нужен. */
    private static final float R_UNIT = 768.0F;
    /** Интенсивность света - пользовательский конфиг (Cloth: nukeLightIntensity). */
    private static float intensity() {
        return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeLightIntensity;
    }
    /** Модификатор радиуса ореола - пользовательский конфиг (Cloth:
     * nukeLightRadiusScale). Умножается на R_UNIT * масштаб гриба. */
    private static float radiusScale() {
        return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeLightRadiusScale;
    }
    /** Сила кинематографического слоя - пользовательский конфиг (Cloth:
     * nukeCinematics). 0 = классический аддитивный свет round-8. */
    private static float cinematics() {
        return com.hbm_m.config.ModClothConfig.get().graphicEffects.nukeCinematics;
    }
    /** Нормировка heat (шейдер клампит heat/50). ФАКТИЧЕСКИЙ максимум
     * NukeTorex: maxHeat = (int)(50*cs), cs = 1.5 константа => 75.0 -
     * потому нормировка rawHeat в трекере идёт по NUKE_TOREX_MAX_HEAT. */
    private static final float HEAT_SCALE = 50.0F;
    /** Реальный максимум heat NukeTorex (для перевода rawHeat в возраст). */
    private static final float NUKE_TOREX_MAX_HEAT = 75.0F;
    /** Базовый подъём источника, блоки; масштабируется max(0.75, scale гриба). */
    private static final float LIGHT_SOURCE_LIFT = 42.0F;
    /** Масштаб texel-offset нормалей (свет v2; uniform uNormalScale, ~1.0). */
    private static final float NORMAL_SCALE = 1.0F;
    /**
     * Диагностика канала записи (cause#1): запись ЗАМЕНОЙ цвета (сплошная
     * маджента, блендинг выключен) - конфигурация, доказанно видимая пользователю
     * в диагностической сборке. Включение: -Dhbm.screenSpaceLight.debugReplace=true.
     */
    private static final boolean DEBUG_REPLACE = Boolean.getBoolean("hbm.screenSpaceLight.debugReplace");
    /** A/B №2: аддитив сплошным цветом МИМО всех гейтов шейдера (blend остаётся ONE,ONE). */
    private static final boolean SOLID_ADD = Boolean.getBoolean("hbm.screenSpaceLight.solidAdd");
    private static int program = -1;
    private static int uDepthTex0Loc = -1;
    private static int uDhDepthTexLoc = -1;
    private static int uDhReverseZLoc = -1;
    private static int uDhPresentLoc = -1;
    private static int uDhNearLoc = -1;
    private static int uDhFarLoc = -1;
    private static int uDhFadeDistLoc = -1;
    private static int uMcNearLoc = -1;
    private static int uProjLoc = -1;
    private static int uModelViewLoc = -1;
    private static int uModelViewInvLoc = -1;
    private static int uCamPosLoc = -1;
    private static int uSourcesLoc = -1;
    private static int uSourceCountLoc = -1;
    private static int uSrcRadiusLoc = -1;
    private static int uVanillaFarLoc = -1;
    private static int uIntensityLoc = -1;
    private static int uHeatScaleLoc = -1;
    private static int uResolutionLoc = -1;
    private static int uNormalScaleLoc = -1;
    private static int uDebugReplaceLoc = -1;
    private static int uSolidAddLoc = -1;
    private static int uSrcShockLoc = -1;
    private static int uSceneColorLoc = -1;
    private static int uHasSceneLoc = -1;
    private static int uCineLoc = -1;
    private static int vao = -1;
    private static int vbo = -1;

    // ── Кинематографический слой (nukeCinematics) ────────────────────
    // Копия цвета main RT (glCopyImageSubData каждый кадр): шейдер читает
    // альбедо сцены и рисует СОСТАВЛЕННУЮ картинку с отключённым блендингом
    // (base*(1+surf) + аддитив). Сэмплировать сам аттачмент нельзя - feedback.
    // Первый сбой копии (чужой внутренний формат) навсегда переключает пасс
    // на классический аддитивный путь (uHasScene=0).
    private static int sceneTex = -1;
    private static int sceneTexW = -1;
    private static int sceneTexH = -1;
    private static boolean sceneCopySupported = true;

    /** draw-buffers нашего color-only FBO; константа: glDrawBuffers зовётся каждый кадр. */
    private static final int[] FBO_DRAW_BUFFERS = {GL30.GL_COLOR_ATTACHMENT0};

    /** Наш color-only FBO с цветовой текстурой main RT в COLOR_ATTACHMENT0. */
    private static int fbo = -1;
    private static int fboAttachedTexture = -1;
    private static int fboAttachedAlt = -1;

    /** Структурный latch (program/quad/FBO init): пасс отключается до перезапуска игры.
     * Сбои отдельных кадров latch'а НЕ ставят - только счётчик подряд (см. drawIfReady). */
    private static volatile boolean latched = false;
    /** Подряд идущие неудачи кадра до latch'а: разовый сбой не хоронит свет на сессию. */
    private static final int MAX_CONSECUTIVE_FAILURES = 60;
    private static int consecutiveFailures;
    private static boolean warnedResolveRace;

    /** Источники света текущего кадра: четвёрки (x, y + LIFT, z, heat). */
    private static final float[] sources = new float[MAX_SOURCES * 4];
    /** Масштабы грибов источников (NukeTorex.getScale) - порядок тот же, что в sources. */
    private static final float[] srcScale = new float[MAX_SOURCES];
    /** Пер-источниковый радиус света: R_UNIT * srcScale[i] (round-5, R-2). */
    private static final float[] srcRadius = new float[MAX_SOURCES];
    /** Непрерывные параметры ударной волны: пары (радиус блоков, затухание [0..1]). */
    private static final float[] srcShock = new float[MAX_SOURCES * 2];
    private static int sourceCount;

    // ── Непрерывный наносекундный таймер источников ─────────────────────
    // rawHeat из NukeTorex дискретен: 20 TPS без partialTick (тепло стоит
    // 4-7 кадров, потом скачок), а в самом NukeTorex было целочисленное
    // деление (floor(age/18) тиков!). Трекер восстанавливает непрерывный
    // возраст каждого взрыва по System.nanoTime() и выдаёт гладкий heat и
    // радиус ударной волны каждый кадр рендера (144+ FPS без рывков).
    private static final float[] trackedX = new float[MAX_SOURCES];
    private static final float[] trackedZ = new float[MAX_SOURCES];
    private static final long[] trackedBirthNanos = new long[MAX_SOURCES];
    private static final boolean[] trackedAlive = new boolean[MAX_SOURCES];

    /** CPU-сторона матриц текущего кадра (column-major для glUniformMatrix4fv). */
    private static final FloatBuffer projBuf = directFloats(16);
    private static final FloatBuffer modelViewBuf = directFloats(16);
    private static final FloatBuffer modelViewInvBuf = directFloats(16);
    private static float camX;
    private static float camY;
    private static float camZ;

    /** Java-копии матриц кадра (для CPU-unproject пробы и HEAD-снапшота). */
    private static final Matrix4f hbmProj = new Matrix4f();
    private static final Matrix4f hbmMv = new Matrix4f();
    private static final Matrix4f hbmMvInv = new Matrix4f();

    /** Снапшот HEAD finalizeLevelRendering: матрицы сняты ДО композитов. */
    private static volatile boolean headSnapshotValid;

    /**
     * Вычисляет абсолютно плавный heat (уже нормированный к HEAT_SCALE) и
     * физический радиус 3D ударной волны по наносекундному таймеру каждого
     * кадра. Возраст взрыва сверяется с серверным rawHeat (допуск 2.5 c - пауза,
     * лаг) и восстанавливается при подключении к уже идущему взрыву.
     */
    private static void updateContinuousSources() {
        long now = System.nanoTime();
        boolean[] matchedSlot = new boolean[MAX_SOURCES];

        for (int i = 0; i < sourceCount; i++) {
            float sx = sources[i * 4];
            float sz = sources[i * 4 + 2];
            float rawHeat = sources[i * 4 + 3];
            float scale = Math.max(0.5F, srcScale[i]);

            float lift = LIGHT_SOURCE_LIFT * Math.max(0.75F, scale);
            sources[i * 4 + 1] += lift;
            srcRadius[i] = R_UNIT * scale * radiusScale();

            // Полное время жизни NukeTorex: getMaxAge() = 45*20*scale тиков.
            float maxAgeSec = 45.0F * scale;

            int slot = -1;
            for (int j = 0; j < MAX_SOURCES; j++) {
                if (trackedAlive[j] && !matchedSlot[j]) {
                    float dx = trackedX[j] - sx;
                    float dz = trackedZ[j] - sz;
                    if (dx * dx + dz * dz < 64.0F) {
                        slot = j;
                        break;
                    }
                }
            }
            if (slot == -1) {
                for (int j = 0; j < MAX_SOURCES; j++) {
                    if (!trackedAlive[j] && !matchedSlot[j]) {
                        slot = j;
                        break;
                    }
                }
                if (slot == -1) {
                    slot = i;
                }
                trackedAlive[slot] = true;
                trackedX[slot] = sx;
                trackedZ[slot] = sz;
                // Свежий взрыв (rawHeat на максимуме) - точный старт; иначе
                // восстанавливаем возраст из rawHeat (max = 75, см. NukeTorex).
                float normH = Math.max(0.0F, Math.min(1.0F, rawHeat / NUKE_TOREX_MAX_HEAT));
                float estAgeSec = rawHeat >= NUKE_TOREX_MAX_HEAT - 1.0F ? 0.0F : (1.0F - normH) * maxAgeSec;
                trackedBirthNanos[slot] = now - (long) (estAgeSec * 1.0e9F);
            } else {
                trackedX[slot] = sx;
                trackedZ[slot] = sz;
            }
            matchedSlot[slot] = true;

            float ageSec = Math.max(0.0F, (now - trackedBirthNanos[slot]) * 1.0e-9F);
            // Мягкая подстройка часов при расхождении с серверным тиком.
            float serverAgeSec = (1.0F - Math.max(0.0F, Math.min(1.0F, rawHeat / NUKE_TOREX_MAX_HEAT))) * maxAgeSec;
            if (Math.abs(ageSec - serverAgeSec) > 2.5F) {
                trackedBirthNanos[slot] = now - (long) (serverAgeSec * 1.0e9F);
                ageSec = serverAgeSec;
            }

            // Непрерывный heat [0..HEAT_SCALE] - без ступеней 20 TPS.
            float continuousHeat = Math.max(0.0F, HEAT_SCALE * (1.0F - ageSec / maxAgeSec));
            sources[i * 4 + 3] = continuousHeat;

            // Физический радиус 3D ударной волны: быстрый старт Седова-Тейлора
            // без бесконечной производной в нуле, пробег за ~5.5*c^0.65 секунд.
            float waveDuration = 5.5F * (float) Math.pow(scale, 0.65F);
            float u = Math.min(1.35F, ageSec / Math.max(0.5F, waveDuration));
            float progress = (float) Math.pow(u + 0.015F, 0.72F) - (float) Math.pow(0.015F, 0.72F);
            float shockR = srcRadius[i] * 1.05F * progress;
            float shockFade = Math.max(0.0F, 1.0F - u * 0.78F);
            shockFade = shockFade * shockFade * Math.min(1.0F, ageSec * 6.0F);

            srcShock[i * 2] = shockR;
            srcShock[i * 2 + 1] = shockFade;
        }

        for (int j = 0; j < MAX_SOURCES; j++) {
            if (!matchedSlot[j]) {
                trackedAlive[j] = false;
            }
        }
    }

    private static FloatBuffer directFloats(int floats) {
        return java.nio.ByteBuffer.allocateDirect(floats * 4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer();
    }

    private static final String VSH = """
            #version 150
            in vec3 Position;
            out vec2 uv;
            void main() {
                gl_Position = vec4(Position, 1.0);
                uv = Position.xy * 0.5 + 0.5;
            }
            """;

    /**
     * Свет v3 (round-10). Геометрия: единый декодер сцены
     * {@code sampleSceneViewPos} (ваниль с F-1 фильтром uVanillaFar + DH-слой,
     * reverse-Z декод без ndc-ремапа), 5-таповые min-of-diffs нормали,
     * 16-шаговый raymarch-тени.
     *
     * <p>Время: НЕПРЕРЫВНЫЙ heat с CPU-nanoTime-трекера (ступени 20 TPS и
     * int-деления NukeTorex убраны) и РАЗДЕЛЬНЫЕ кривые - airFlashCurve
     * (короткая вспышка + деликатный зарев, окно видимости гриба
     * mushroomCloudWindow не даёт ореолу залить частицы гриба) и
     * groundFlashCurve (мощный долгий свет земли/лодов с купольным
     * Wrap-Lambert).</p>
     *
     * <p>Кинематика (nukeCinematics): альбедо-освещение из копии цвета сцены,
     * screen-композит зарева, спектр nukeColor, Mie-фаза, godrays,
     * ударная волна Фридлендера по непрерывному радиусу uSrcShock
     * (кольцо + хроматическая рефракция). Без кинематики - аддитив round-8.</p>
     */
    private static final String FSH = """
            #version 150
            uniform sampler2D uDepthTex0;
            uniform sampler2D uDhDepthTex;
            uniform mat4 uProj;
            uniform mat4 uModelView;
            uniform mat4 uModelViewInv;
            uniform vec3 uCamPos;
            uniform vec4 uSources[4];
            uniform int uSourceCount;
            uniform float uSrcRadius[4];
            // Непрерывные параметры ударной волны (CPU, nanoTime-трекер):
            // x = радиус 3D-сферы волны (блоки), y = затухание силы [0..1].
            uniform vec2 uSrcShock[4];
            uniform float uIntensity;
            uniform float uHeatScale;
            uniform float uDebugReplace;
            uniform float uSolidAdd;
            uniform float uDhReverseZ;
            uniform float uDhPresent;
            uniform float uDhNear;
            uniform float uDhFar;
            uniform float uDhFadeDist;
            uniform float uMcNear;
            // Кинематографический слой: копия цвета сцены (альбедо-освещение,
            // рефракция ударной волны) и его сила (0 = классический аддитив).
            uniform sampler2D uSceneColor;
            uniform float uHasScene;
            uniform float uCine;
            // Граница ванильной геометрии, блоки (DhOcclusionGpu.vanillaFarPlane =
            // (RD+1)*16): под-пороговый контент главного RT дальше неё - не
            // геометрия (F-1, round-5).
            uniform float uVanillaFar;
            uniform vec2 uResolution;
            uniform float uNormalScale;
            in vec2 uv;
            out vec4 fragColor;

            const int   SHADOW_STEPS     = 16;
            const float SHADOW_SKIP_TAIL = 0.2;
            const float SHADOW_THICKNESS = 1.5;
            // Полностью затенённая поверхность: остаточный множитель.
            const float SHADOW_FLOOR     = 0.04;
            // Нижняя обёртка ndl.
            const float NDL_MIN          = 0.08;
            // Воздушная подсветка: 8-шаговый march по лучу взгляда - только
            // небо/DH-пусто и только без кинематики (там godrays).
            const int   AIR_MARCH_STEPS     = 8;
            const float AIR_OCCLUDE_RESIDUE = 0.03;

            mat4 gProjInv; // inverse(uProj), один расчёт на пиксель

            // Линеаризация DH-глубины. На вход dDh в [0..1]: reverse-Z (1 = near,
            // 0 = far) или стандартный Z (через ndc). Раньше в reverse-Z ветку
            // подставляли ndc [-1..1]: отрицательный знаменатель зажимался
            // max(...,1e-6) и все лоды дальше ~15 блоков декодировались в ~3e10
            // блоков - подсветка лодов гасла полностью.
            float decodeDhLinearZ(float dDh) {
                if (uDhReverseZ > 0.5) {
                    return (uDhFar * uDhNear) / max(uDhNear + dDh * (uDhFar - uDhNear), 1e-6);
                }
                float ndc = dDh * 2.0 - 1.0;
                return (2.0 * uDhFar * uDhNear) / max((uDhFar + uDhNear) - ndc * (uDhFar - uDhNear), 1e-6);
            }

            bool dhLayerEmpty(float dDh) {
                return uDhReverseZ > 0.5 ? (dDh <= 1.0e-6) : (dDh >= 0.999999);
            }

            // Реконструкция DH-лода на луче пикселя: dist - линейная дистанция;
            // точка np на near-плоскости MC-проекции по построению имеет
            // z_view = -near, поэтому масштаб dist / -nearView.z.
            vec3 dhViewPos(vec2 suv, float dDh) {
                float dist = decodeDhLinearZ(dDh);
                vec4 np = gProjInv * vec4(suv * 2.0 - 1.0, -1.0, 1.0);
                vec3 nearView = np.xyz / np.w;
                return nearView * (dist / max(-nearView.z, 1.0e-6));
            }

            // ЕДИНЫЙ декодер сцены (main, нормали, raymarch-тени, godrays):
            // ванильная ветка с F-1 фильтром uVanillaFar, затем DH-слой.
            vec3 sampleSceneViewPos(vec2 suv, out bool isDh) {
                isDh = false;
                float d0 = texture(uDepthTex0, suv).r;
                if (d0 < 0.999999) {
                    vec4 vp = gProjInv * vec4(suv * 2.0 - 1.0, d0 * 2.0 - 1.0, 1.0);
                    vec3 p = vp.xyz / vp.w;
                    if (length(p) <= uVanillaFar) {
                        return p;
                    }
                }
                if (uDhPresent > 0.5) {
                    float dDh = texture(uDhDepthTex, suv).r;
                    if (!dhLayerEmpty(dDh)) {
                        isDh = true;
                        return dhViewPos(suv, dDh);
                    }
                }
                return vec3(0.0);
            }

            // Декодер воздушного марша: ваниль с отсечкой uVanillaFar, DH с
            // СОХРАНЁННЫМ гейтом uDhFadeDist (кольцо round-2 остаётся закрытым).
            vec3 layerViewPosAir(vec2 suv) {
                float d = texture(uDepthTex0, suv).r;
                if (d < 0.999999) {
                    vec4 vp = gProjInv * vec4(suv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
                    vec3 p = vp.xyz / vp.w;
                    if (length(p) > uVanillaFar) {
                        return vec3(0.0);
                    }
                    return p;
                }
                if (uDhPresent > 0.5) {
                    float dDh = texture(uDhDepthTex, suv).r;
                    if (!dhLayerEmpty(dDh)) {
                        if (decodeDhLinearZ(dDh) < uDhFadeDist) {
                            return vec3(0.0);
                        }
                        return dhViewPos(suv, dDh);
                    }
                }
                return vec3(0.0);
            }

            // ── Воздушная подсветка: замкнутый интеграл (1-x/R)^2 по хорде ──
            float airAnti(float u, float a2, float ap, float R2, float invR) {
                float x = sqrt(a2 + u * u);
                return u + (a2 * u + u * u * u / 3.0) / R2
                     - invR * 0.5 * (x * u + ap * ap * log(x + u + ap * 1.0e-6));
            }

            float airTerm(vec3 rel, vec3 dir, float R, float L, out float uMax, out float t) {
                uMax = 0.0;
                t = max(dot(rel, dir), 0.0);
                float a2 = max(dot(rel, rel) - t * t, 0.0);
                float a = sqrt(a2);
                if (a >= R) {
                    return 0.0;
                }
                float ap = max(a, 1.0e-4);
                uMax = sqrt(max(R * R - a2, 0.0));
                float u0 = clamp(-t, -uMax, uMax);
                float u1 = clamp(min(L, t + uMax) - t, -uMax, uMax);
                if (u1 <= u0) {
                    return 0.0;
                }
                float R2 = R * R;
                float invR = 2.0 / R;
                return max(airAnti(u1, a2, ap, R2, invR) - airAnti(u0, a2, ap, R2, invR), 0.0);
            }

            float airSoftCap(float air, float cap) {
                return cap * (1.0 - exp(-air / max(cap, 1.0e-4)));
            }

            // РАЗДЕЛЬНЫЕ кривые времени (репорт боя: ореол прятал гриб, а земля
            // гасла вместе с ним):
            // 1) Воздушный ореол - короткая детонационная вспышка (первые секунды,
            //    heat > 0.72) + деликатный фоновый зарев ПОД частицами гриба.
            float airFlashCurve(float h) {
                float baseGlow = h * h * 0.42;
                float initialFlash = pow(max(h - 0.72, 0.0) / 0.28, 2.0) * 0.95;
                return baseGlow + initialFlash;
            }

            // 2) Поверхностный свет земли и лодов - мощный и долгий, всю жизнь гриба.
            float groundFlashCurve(float h) {
                return pow(h, 1.25) * 1.65;
            }

            // Цветовая температура взрыва (спектр чёрного тела, огрубленный):
            // coreFactor 0 = край радиуса / остывание, 1 = ядро при пике heat.
            vec3 nukeColor(float coreFactor, float heat) {
                vec3 flashWhite  = vec3(0.92, 0.96, 1.00);
                vec3 coreGold    = vec3(1.00, 0.82, 0.45);
                vec3 midOrange   = vec3(1.00, 0.42, 0.10);
                vec3 edgeCrimson = vec3(0.70, 0.12, 0.03);
                vec3 ramp = mix(edgeCrimson, midOrange, smoothstep(0.0, 0.42, coreFactor));
                ramp = mix(ramp, coreGold, smoothstep(0.42, 0.85, coreFactor));
                ramp = mix(ramp, flashWhite, smoothstep(0.78, 1.0, coreFactor * heat));
                return ramp;
            }

            // Хеньи-Гринштейн, нормирован к 1 вперёд: взгляду НА взрыв - полная
            // яркость ореола, от взрыва - доли (Mie-кастинг зарева).
            float miePhaseNorm(float cosTheta, float g) {
                float g2 = g * g;
                float f = (1.0 - g2)
                        / (12.5663706 * pow(max(1.0 + g2 - 2.0 * g * cosTheta, 1.0e-4), 1.5));
                float f0 = (1.0 - g2) / (12.5663706 * (1.0 - g) * (1.0 - g));
                return clamp(f / max(f0, 1.0e-6), 0.0, 4.0);
            }

            // Окно видимости гриба: в эллипсоиде ножки/шапки (частицы NukeTorex
            // уже нарисованы в uSceneColor, глубину НЕ пишут = класс «небо»)
            // плавно убираем накладываемый туман, оставляя корону вокруг гриба.
            // В первые доли секунды (heat > 0.82) вспышка пробивает окно.
            float mushroomCloudWindow(vec3 relToSrc, vec3 rayDir, float R, float heat) {
                float t = max(dot(relToSrc, rayDir), 0.0);
                vec3 closestPt = rayDir * t - relToSrc;
                vec2 ellip = vec2(length(closestPt.xz) / (R * 0.22), abs(closestPt.y) / (R * 0.26));
                float dCloud = length(ellip);
                float flashBypass = smoothstep(0.82, 0.98, heat) * 0.65;
                return mix(0.14 + flashBypass, 1.0, smoothstep(0.35, 1.25, dCloud));
            }

            // Экранные godrays (кинематика): 12 шагов по экрану от пикселя к
            // проекции центра взрыва. Шаг пропускает свет, если сэмпл - небо
            // или дальше центра; горы/крыши блокируют = сумеречные лучи.
            float godrays(vec2 suv0, vec2 srcUv, float srcDist, float dither) {
                vec2 delta = (srcUv - suv0) / 12.0;
                float illum = 0.0;
                float wsum = 0.0;
                float w = 1.0;
                for (int i = 0; i < 12; i++) {
                    vec2 suv = suv0 + delta * (float(i) + dither);
                    wsum += w;
                    if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
                        illum += w;
                    } else {
                        bool grDh;
                        vec3 sp = sampleSceneViewPos(suv, grDh);
                        if (dot(sp, sp) < 1.0e-12 || length(sp) > srcDist - 8.0) {
                            illum += w;
                        }
                    }
                    w *= 0.92;
                }
                return illum / max(wsum, 1.0e-4);
            }

            // Окклюзия зарева (БЕЗ кинематики): 8 шагов вдоль луча взгляда.
            float airMarchOcc(vec3 rayDir, float t, float uMax, float dither) {
                float s0 = max(0.0, t - uMax);
                float s1 = t + uMax;
                int blocked = 0;
                for (int j = 0; j < AIR_MARCH_STEPS; j++) {
                    float sj = s0 + (float(j) + dither) * (s1 - s0) / float(AIR_MARCH_STEPS);
                    vec3 sp = uCamPos + rayDir * sj;
                    vec4 spView = uModelView * vec4(sp - uCamPos, 1.0);
                    if (spView.w <= 0.0) {
                        continue;
                    }
                    vec4 clip = uProj * spView;
                    vec3 ndc = clip.xyz / clip.w;
                    vec2 suv = ndc.xy * 0.5 + 0.5;
                    if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
                        continue;
                    }
                    vec3 spLayer = layerViewPosAir(suv);
                    if (dot(spLayer, spLayer) > 1.0e-12 && length(spLayer) < sj - 1.5) {
                        blocked++;
                    }
                }
                return float(blocked) / float(AIR_MARCH_STEPS);
            }

            // Мягкие тени: 16-шаговый march вдоль луча пиксель->источник.
            float shadowSoft(vec3 worldPos, vec3 dirToSrc, float dist, float marchSin) {
                float stepLen = dist * (1.0 - SHADOW_SKIP_TAIL) / float(SHADOW_STEPS);
                float stride = sqrt(marchSin * marchSin + 0.0625);
                float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
                int unblocked = 0;
                for (int s = 0; s < SHADOW_STEPS; s++) {
                    vec3 sp = worldPos + dirToSrc * (stepLen * (float(s) + dither) / stride);
                    vec4 spView = uModelView * vec4(sp - uCamPos, 1.0);
                    if (spView.w <= 0.0) {
                        unblocked++;
                        continue;
                    }
                    float rayDist = length(spView.xyz);
                    vec4 clip = uProj * spView;
                    vec3 ndc = clip.xyz / clip.w;
                    vec2 suv = ndc.xy * 0.5 + 0.5;
                    if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
                        unblocked++;
                        continue;
                    }
                    bool tapDh;
                    vec3 spLayer = sampleSceneViewPos(suv, tapDh);
                    float sceneDist = length(spLayer);
                    if (dot(spLayer, spLayer) > 1.0e-12 && sceneDist < rayDist - SHADOW_THICKNESS) {
                        // блокер ближе к камере, чем шаг (минус толщина)
                    } else {
                        unblocked++;
                    }
                }
                return float(unblocked) / float(SHADOW_STEPS);
            }

            void main() {
                gProjInv = inverse(uProj);
                if (uDebugReplace > 0.5) {
                    fragColor = vec4(1.0, 0.0, 1.0, 1.0);
                    return;
                }
                if (uSolidAdd > 0.5) {
                    fragColor = vec4(0.5, 0.25, 0.1, 1.0);
                    return;
                }

                bool fromDh;
                vec3 viewPos = sampleSceneViewPos(uv, fromDh);
                bool geometry = dot(viewPos, viewPos) > 1.0e-12;

                vec4 farView = gProjInv * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
                vec3 rayDir = normalize((uModelViewInv * vec4(farView.xyz / farView.w, 0.0)).xyz);
                float airDither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

                // Кинематика: поверхностный (альбедо), зарево и пыльное кольцо
                // копятся РАЗДЕЛЬНО; рефракция - экранное смещение копии сцены.
                vec3 surfAccum = vec3(0.0);
                vec3 glowAccum = vec3(0.0);
                vec3 ringAccum = vec3(0.0);
                float refrAmp = 0.0;
                vec2 refrDir = vec2(0.0);

                if (geometry) {
                    vec3 worldPos = uCamPos + (uModelViewInv * vec4(viewPos, 1.0)).xyz;
                    float viewDist = length(viewPos);
                    // Нормаль: 5-таповый min-of-diffs (±x, ±y) - на каждой оси
                    // берётся сосед с меньшей разностью глубины.
                    float ox = max(1.0, viewDist * uNormalScale / uResolution.x);
                    float oy = max(1.0, viewDist * uNormalScale / uResolution.y);
                    bool dT1; bool dT2; bool dT3; bool dT4;
                    vec3 pxl = sampleSceneViewPos(uv + vec2(-ox / uResolution.x, 0.0), dT1);
                    vec3 pxr = sampleSceneViewPos(uv + vec2( ox / uResolution.x, 0.0), dT2);
                    vec3 pyd = sampleSceneViewPos(uv + vec2(0.0, -oy / uResolution.y), dT3);
                    vec3 pyu = sampleSceneViewPos(uv + vec2(0.0,  oy / uResolution.y), dT4);
                    bool hasL = dot(pxl, pxl) > 1.0e-12;
                    bool hasR = dot(pxr, pxr) > 1.0e-12;
                    bool hasD = dot(pyd, pyd) > 1.0e-12;
                    bool hasU = dot(pyu, pyu) > 1.0e-12;
                    vec3 dX = vec3(0.0);
                    vec3 dY = vec3(0.0);
                    if (hasL && hasR) {
                        dX = length(pxl - viewPos) <= length(pxr - viewPos) ? viewPos - pxl : pxr - viewPos;
                    } else if (hasR) {
                        dX = pxr - viewPos;
                    } else if (hasL) {
                        dX = viewPos - pxl;
                    }
                    if (hasD && hasU) {
                        dY = length(pyd - viewPos) <= length(pyu - viewPos) ? viewPos - pyd : pyu - viewPos;
                    } else if (hasU) {
                        dY = pyu - viewPos;
                    } else if (hasD) {
                        dY = viewPos - pyd;
                    }
                    vec3 nView;
                    if (dot(dX, dX) > 1.0e-12 && dot(dY, dY) > 1.0e-12) {
                        nView = cross(dX, dY);
                    } else {
                        nView = vec3(0.0, 0.0, 1.0); // плоско к камере
                    }
                    nView = normalize(nView);
                    if (dot(nView, viewPos) > 0.0) {
                        nView = -nView; // нормаль смотрит к камере
                    }
                    vec3 nWorld = normalize((uModelViewInv * vec4(nView, 0.0)).xyz);
                    float wDist = smoothstep(0.5 * uVanillaFar, uVanillaFar, viewDist);
                    nWorld = normalize(mix(nWorld, vec3(0.0, 1.0, 0.0), wDist));

                    for (int i = 0; i < uSourceCount; i++) {
                        float heatA = clamp(uSources[i].w / uHeatScale, 0.0, 1.0);
                        float airFlash = airFlashCurve(heatA);
                        float groundFlash = groundFlashCurve(heatA);
                        float RA = uSrcRadius[i];
                        vec3 relA = uSources[i].xyz - uCamPos;

                        // 1. Воздушная дымка до геометрии (с окном видимости гриба)
                        float tA;
                        float uMaxA;
                        float airRaw = airTerm(relA, rayDir, RA, viewDist, uMaxA, tA);
                        float airNorm = airRaw * 3.0 / (2.0 * RA);
                        float cosTA = dot(rayDir, normalize(relA));
                        float cloudWin = mushroomCloudWindow(relA, rayDir, RA, heatA);
                        vec3 airCol = nukeColor(clamp(1.0 - sqrt(max(dot(relA, relA) - tA * tA, 0.0)) / RA, 0.0, 1.0), heatA);
                        float air = airNorm * airNorm * airFlash * uIntensity * cloudWin
                                * mix(0.28, 0.85, miePhaseNorm(cosTA, 0.55));
                        glowAccum += airCol * airSoftCap(air, 0.28 * uIntensity * max(airFlash, 0.6));

                        // 2. Поверхностный свет земли и лодов. Купольный
                        // Wrap-Lambert: угол падения считается на ВЕРХКУПКУ гриба
                        // (~0.14*R выше центра) - иначе на дальних лодах
                        // ndl = lift/dist ~ 0.06 и земля мёртвая.
                        vec3 toSrc = uSources[i].xyz - worldPos;
                        float dist = length(toSrc);
                        vec3 domeToSrc = toSrc + vec3(0.0, RA * 0.14, 0.0);
                        vec3 dirToSrc = toSrc / max(dist, 1e-4);
                        vec3 dirToDome = normalize(domeToSrc);

                        float fallLin = clamp(1.0 - dist / RA, 0.0, 1.0);
                        float fall = mix(fallLin, fallLin * fallLin, 0.52);
                        float gate = fall * groundFlash;
                        if (gate > 0.0) {
                            float rawNdl = clamp(dot(nWorld, dirToDome), 0.0, 1.0);
                            float ndl = mix(0.34, 1.0, rawNdl);

                            float graze = dot(worldPos - uCamPos, dirToSrc) / max(viewDist, 1e-4);
                            float marchSin = sqrt(max(1.0 - graze * graze, 0.0));
                            float soft = shadowSoft(worldPos, dirToSrc, dist, marchSin);
                            vec3 colS = nukeColor(fallLin, heatA);
                            surfAccum += colS
                                    * (gate * uIntensity * 1.45 * ndl
                                       * (SHADOW_FLOOR + (1.0 - SHADOW_FLOOR) * soft));
                        }

                        // 3. Ударная волна: непрерывный CPU-таймер (uSrcShock) +
                        // асимметричный профиль Фридлендера по ГОРИЗОНТАЛЬНОЙ
                        // дистанции - резкий передний фронт, широкий пылевой
                        // шлейф; скользит по ступеням лодов без рывков.
                        float shockR = uSrcShock[i].x;
                        float shockFade = uSrcShock[i].y;
                        if (uCine > 0.001 && shockFade > 0.001 && shockR > 1.0) {
                            float horizDist = length(toSrc.xz);
                            float dr = horizDist - shockR;
                            float sigma = dr >= 0.0 ? 18.0 : 65.0;
                            float band = exp(-(dr * dr) / (sigma * sigma)) * shockFade * uCine;
                            if (band > 0.002) {
                                vec3 shockCol = mix(vec3(1.0, 0.55, 0.20), vec3(1.0, 0.88, 0.70), smoothstep(-25.0, 6.0, dr));
                                ringAccum += shockCol * (band * uIntensity * 0.85);
                                vec4 sClip = uProj * (uModelView * vec4(relA, 1.0));
                                if (sClip.w > 0.0) {
                                    vec2 sUv = (sClip.xy / sClip.w) * 0.5 + 0.5;
                                    vec2 dS = uv - sUv;
                                    refrDir += normalize(dS + vec2(1.0e-5, 0.0)) * band;
                                    refrAmp += band;
                                }
                            }
                        }
                    }
                } else {
                    // Небо и сам ядерный гриб (частицы глубину не пишут):
                    for (int i = 0; i < uSourceCount; i++) {
                        float heat = clamp(uSources[i].w / uHeatScale, 0.0, 1.0);
                        float airFlash = airFlashCurve(heat);
                        float R = uSrcRadius[i];
                        vec3 relA = uSources[i].xyz - uCamPos;
                        float tAir;
                        float uMaxAir;
                        float airRaw = airTerm(relA, rayDir, R, 1.0e30, uMaxAir, tAir);
                        float airNorm = airRaw * 3.0 / (2.0 * R);
                        float cosTA = dot(rayDir, normalize(relA));
                        float cloudWin = mushroomCloudWindow(relA, rayDir, R, heat);
                        float air = airNorm * airNorm * airFlash * uIntensity * cloudWin
                                * mix(0.28, 0.85, miePhaseNorm(cosTA, 0.55));
                        vec3 airCol = nukeColor(clamp(1.0 - sqrt(max(dot(relA, relA) - tAir * tAir, 0.0)) / R, 0.0, 1.0), heat);
                        if (uCine > 0.001) {
                            // godrays: та же окклюзия плюс сумеречные лучи.
                            vec4 sClip = uProj * (uModelView * vec4(relA, 1.0));
                            if (sClip.w > 0.0) {
                                vec2 sUv = (sClip.xy / sClip.w) * 0.5 + 0.5;
                                float gr = godrays(uv, sUv, length(relA), airDither);
                                air *= mix(0.15, 1.0, gr);
                            }
                        } else if (airRaw > max(0.02 * uIntensity, 0.05) && uMaxAir > 0.0) {
                            float occ = airMarchOcc(rayDir, tAir, uMaxAir, airDither);
                            air *= mix(1.0, AIR_OCCLUDE_RESIDUE, occ);
                        }
                        glowAccum += airCol * airSoftCap(air, 0.28 * uIntensity * max(airFlash, 0.6));
                    }
                }

                vec3 added = surfAccum + glowAccum + ringAccum;
                // Классический путь (кинематика выкл или копия сцены недоступна):
                // чистый аддитив (поверхность - с половинным весом: альбедо-модуляции нет).
                if (uHasScene < 0.5 || uCine <= 0.001) {
                    if (added == vec3(0.0)) {
                        discard;
                    }
                    fragColor = vec4(pow(glowAccum + ringAccum + surfAccum * 0.5, vec3(1.0 / 2.2)), 1.0);
                    return;
                }
                // Рефракция должна писаться и без света (искажение без свечения).
                if (added == vec3(0.0) && refrAmp < 0.003) {
                    discard;
                }

                vec3 base;
                if (refrAmp >= 0.003) {
                    vec2 off = (refrDir / max(refrAmp, 1.0e-4)) * (0.0038 * min(refrAmp, 1.2));
                    base = vec3(
                            texture(uSceneColor, uv + off).r,
                            texture(uSceneColor, uv + off * 0.55).g,
                            texture(uSceneColor, uv + off * 0.25).b);
                } else {
                    base = texture(uSceneColor, uv).rgb;
                }

                // ── Раздельный композитинг земли и воздуха ──────────────────
                // 1. Земля/лоды: альбедо-освещение + собственный цветовой вклад.
                vec3 litSurface = base;
                if (geometry) {
                    vec3 surfSoft = surfAccum / (1.0 + surfAccum * 0.35);
                    litSurface = base * (1.0 + surfSoft * 4.2) + surfSoft * 0.32 + ringAccum;
                }

                // 2. Воздушное зарево: мягкая гамма + Screen-блендинг
                //    (не выжигает яркие детали гриба в белый квадрат).
                vec3 glowGamma = pow(max(glowAccum, vec3(0.0)), vec3(1.0 / 1.8));
                glowGamma = glowGamma / (1.0 + glowGamma * 0.65);

                vec3 finalColor = litSurface + glowGamma * max(vec3(1.0) - litSurface * 0.88, vec3(0.08));
                fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
            }
            """;

    private record Inputs(int colorTexture, int width, int height, int depthTexture,
                          int dhTexture, float dhNear, float dhFar, boolean dhReverseZ) {}

    /**
     * Точка входа миксина (TAIL {@code finalizeLevelRendering}). Все гейты до
     * тяжёлой работы: latch, конфиг, активный пак.
     */
    public static void drawIfReady(Object mixinPipeline) {
        if (latched) {
            return;
        }
        // Конфиг-тумблер милстоуна (A/B-тесты и аварийное отключение).
        if (!com.hbm_m.config.ModClothConfig.get().graphicEffects.screenSpaceLightFill
                || "false".equalsIgnoreCase(System.getProperty("hbm.screenSpaceLight", "true"))) {
            return;
        }
        frameCounter++;
        // Гейт на активность пайплайна: без пака пасс не имеет смысла (кэш
        // ShaderCompatibilityDetector уже опрошен этим кадром - O(1)).
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            noteOnce("pack-inactive", "external shader inactive - pass idle (ожидаемо при выключенном паке)");
            return;
        }
        try {
            Inputs inputs = resolveTailInputs();
            if (inputs == null) {
                return; // временное состояние - не latch, причина записана skipFrame()
            }
            if (!ensureProgram() || !ensureQuad()) {
                latch("program/quad init failed");
                return;
            }
            sourceCount = ParticleEngineNT.INSTANCE.collectNukeSources(sources, srcScale, MAX_SOURCES);
            if (sourceCount == 0) {
                for (int j = 0; j < MAX_SOURCES; j++) {
                    trackedAlive[j] = false;
                }
                noteEvery("no-sources", STATE_EVERY_FRAMES, "no live NukeTorex sources - pass idle");
                return;
            }
            // Свет v2: подъём источника, per-source радиус, НЕПРЕРЫВНЫЙ heat
            // (nanoTime-трекер, глушит ступени 20 TPS / int-деления NukeTorex)
            // и параметры ударной волны - всё в одном проходе, до шейдера и логов.
            updateContinuousSources();
            if (!fillFrameMatrices()) {
                return;
            }
            if (!loggedFirstDraw) {
                loggedFirstDraw = true;
                MainRegistry.LOGGER.info(
                        "RawScreenSpaceLight: FIRST LIGHT frame #{}: {}x{} colorTex={} depthTex={}"
                                + " | sources={} src0={} cam=({}, {}, {}) camDist={} radius0={} intensity={}"
                                + " | lift={} normalScale={}"
                                + " | DH: tex={} near={} far={} reverseZ={} present={}"
                                + " | headSnap={} debugReplace={}",
                        frameCounter, inputs.width(), inputs.height(), inputs.colorTexture(), inputs.depthTexture(),
                        sourceCount, src0String(), camX, camY, camZ, camDist0(), srcRadius[0], intensity(),
                        LIGHT_SOURCE_LIFT, NORMAL_SCALE,
                        inputs.dhTexture(), inputs.dhNear(), inputs.dhFar(), inputs.dhReverseZ(),
                        inputs.dhTexture() > 0, headSnapshotValid, DEBUG_REPLACE);
            } else if (frameCounter % STATE_EVERY_FRAMES == 0) {
                MainRegistry.LOGGER.info(
                        "RawScreenSpaceLight: state frame #{}: drawn={} skipped={} lastSkip={} sources={}"
                                + " src0={} cam=({}, {}, {}) camDist={} radius0={} snap={}"
                                + " dh=(tex={}, near={}, far={}, revZ={}, present={})",
                        frameCounter, framesDrawn, framesSkipped, lastSkipReason, sourceCount,
                        src0String(), camX, camY, camZ, camDist0(), srcRadius[0], headSnapshotValid,
                        inputs.dhTexture(), inputs.dhNear(), inputs.dhFar(), inputs.dhReverseZ(),
                        inputs.dhTexture() > 0);
            }
            draw(inputs);
            framesDrawn++;
            consecutiveFailures = 0;
        } catch (Throwable t) {
            // Разовый сбой (гонка reload'а, пересборка пайплайна) не должен тихо
            // хоронить свет до перезапуска: считаем ПОДРЯД идущие неудачи, успех
            // сбрасывает счётчик; latch - только после MAX_CONSECUTIVE_FAILURES
            // (устойчиво мёртвое окружение, не транзиент).
            consecutiveFailures++;
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                latched = true;
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: " + consecutiveFailures
                        + " consecutive frame failures - permanently disabled after failure:", t);
            } else if (consecutiveFailures == 1) {
                // Полный трейс вместо t.toString(): строка без стека не чинится.
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: frame failed - retrying (up to "
                        + MAX_CONSECUTIVE_FAILURES + " in a row):", t);
            } else if (consecutiveFailures % 10 == 0) {
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: {} frames failed in a row (last: {})",
                        consecutiveFailures, t.toString());
            }
        }
    }

    private static void latch(String reason) {
        latched = true;
        MainRegistry.LOGGER.warn("RawScreenSpaceLight: permanently disabled after failure: {}", reason);
    }

    // ------------------------------------------------------------------
    // Диагностика. Политика: первые ALWAYS_LOG_FRAMES кадров логируются всегда,
    // дальше - первый случай каждой причины, перескок причины раз в минуту,
    // сводка состояния раз в ~20 с.
    // ------------------------------------------------------------------
    private static final Map<String, Boolean> notedOnce = new HashMap<>();
    private static long frameCounter;
    private static long framesDrawn;
    private static long framesSkipped;
    private static String lastSkipReason = "(none)";
    private static boolean loggedFirstDraw;
    private static final long ALWAYS_LOG_FRAMES = 10;
    private static final long SKIP_RESURFACE_FRAMES = 3600; // ~1 мин при 60 fps
    private static final long STATE_EVERY_FRAMES = 1200;    // ~20 с при 60 fps

    private static void noteOnce(String key, String message) {
        if (notedOnce.putIfAbsent(key, Boolean.TRUE) == null) {
            MainRegistry.LOGGER.info("RawScreenSpaceLight [{}]: {}", key, message);
        }
    }

    private static void noteEvery(String key, long everyN, String message) {
        if (frameCounter % everyN == 0) {
            MainRegistry.LOGGER.info("RawScreenSpaceLight [{}]: {}", key, message);
        }
    }

    /** Тихий return null из resolveTailInputs, но с зафиксированной причиной. */
    private static Inputs skipFrame(String reason) {
        lastSkipReason = reason;
        framesSkipped++;
        if (frameCounter <= ALWAYS_LOG_FRAMES || frameCounter % SKIP_RESURFACE_FRAMES == 0
                || notedOnce.putIfAbsent("skip:" + reason, Boolean.TRUE) == null) {
            MainRegistry.LOGGER.info("RawScreenSpaceLight: frame #{} skipped ({}), retry next frame", frameCounter, reason);
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Рефлексия: только CapturedRenderingState (gbuffer projection + modelView;
    // modelViewInverse инвертируем сами - такого геттера в Iris 1.8+/Oculus нет).
    // Буферы Iris в TAIL-фазе не нужны вовсе.
    // ------------------------------------------------------------------

    private static boolean reflectionInitialized;
    private static Object capturedRenderingState;
    private static Method getGbufferProjection;
    private static Method getGbufferModelView;

    private static synchronized void initReflection() {
        if (reflectionInitialized) {
            return;
        }
        reflectionInitialized = true;
        Class<?> capturedClass = null;
        try {
            capturedClass = firstClass(
                    "net.irisshaders.iris.uniforms.CapturedRenderingState",
                    "net.coderbot.iris.uniforms.CapturedRenderingState");
            capturedRenderingState = capturedClass.getField("INSTANCE").get(null);
            getGbufferProjection = capturedClass.getMethod("getGbufferProjection");
        } catch (Throwable t) {
            capturedRenderingState = null;
            MainRegistry.LOGGER.info("RawScreenSpaceLight: CapturedRenderingState unavailable ({}), pass stays off", t.toString());
            return;
        }
        try {
            getGbufferModelView = capturedClass.getMethod("getGbufferModelView");
        } catch (Throwable t) {
            // Отсутствие - skipFrame каждый кадр с причиной, не latch.
            getGbufferModelView = null;
        }
    }

    private static Class<?> firstClass(String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException("none of " + String.join(", ", names));
    }

    /**
     * HEAD {@code finalizeLevelRendering} (миксин): снапшот gbuffer-матриц и
     * позиции камеры ДО композитов. В Iris 1.8.14/Oculus 1.8 композиты
     * CapturedRenderingState не перезаписывают (сеттеры - один вызов за кадр,
     * renderLevel HEAD) - снапшот оборонительный для прочих версий. Неудача резолва -
     * headSnapshotValid=false и TAIL ведёт себя как раньше (резолв на месте,
     * skipFrame); latch не ставим.
     */
    public static void captureFrameMatrices(Object mixinPipeline) {
        headSnapshotValid = false;
        if (latched) {
            return;
        }
        if (!com.hbm_m.config.ModClothConfig.get().graphicEffects.screenSpaceLightFill
                || "false".equalsIgnoreCase(System.getProperty("hbm.screenSpaceLight", "true"))) {
            return;
        }
        try {
            if (!reflectionInitialized) {
                initReflection();
            }
            if (capturedRenderingState == null || getGbufferProjection == null || getGbufferModelView == null) {
                return;
            }
            Matrix4fc proj = (Matrix4fc) getGbufferProjection.invoke(capturedRenderingState);
            Matrix4fc mv = (Matrix4fc) getGbufferModelView.invoke(capturedRenderingState);
            if (proj == null || mv == null) {
                return;
            }
            hbmProj.set(proj);
            projBuf.clear();
            hbmProj.get(projBuf);
            projBuf.limit(16).position(0); // JOML get() пишет абсолютно и НЕ двигает позицию: flip() давал limit=0 => remaining=0 => count=0 у glUniformMatrix4fv (тихий no-op, матрицы не грузились вовсе)
            // uModelView - матрица ДО инверсии (для репроекции world→uv в тенях).
            hbmMv.set(mv);
            modelViewBuf.clear();
            hbmMv.get(modelViewBuf);
            modelViewBuf.limit(16).position(0);
            hbmMvInv.set(mv).invert();
            modelViewInvBuf.clear();
            hbmMvInv.get(modelViewInvBuf);
            modelViewInvBuf.limit(16).position(0);
            net.minecraft.world.phys.Vec3 cam = net.minecraft.client.Minecraft.getInstance()
                    .gameRenderer.getMainCamera().getPosition();
            camX = (float) cam.x;
            camY = (float) cam.y;
            camZ = (float) cam.z;
            headSnapshotValid = true;
            noteOnce("head-snap-ok", "HEAD matrix snapshot captured - TAIL draws with pre-composite matrices");
        } catch (Throwable t) {
            noteOnce("head-capture-failed", "HEAD matrix capture failed (" + t + ") - TAIL will resolve its own");
        }
    }

    /**
     * TAIL-входы: главный render target Minecraft. Глубина сцены - его
     * depth-текстура (цветовые композиты её не трогают); DH-слой как обычно.
     */
    private static Inputs resolveTailInputs() throws Throwable {
        if (!reflectionInitialized) {
            initReflection();
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        // var: тип RenderTarget в 1.20.1 (net.minecraft.client.renderer) и 1.21.1
        // (com.mojang.blaze3d.pipeline) лежит в разных пакетах.
        var main = mc.getMainRenderTarget();
        int color = main.getColorTextureId();
        int depth = main.getDepthTextureId();
        int width = main.width;
        int height = main.height;
        if (color <= 0 || depth <= 0 || width <= 0 || height <= 0) {
            return skipFrame("main RT invalid: color=" + color + " depth=" + depth + " " + width + "x" + height);
        }
        int dhTex = -1;
        float dhNear = 0.05F;
        float dhFar = 4096.0F;
        boolean dhReverseZ = false;
        if (DhClientState.isActive()) {
            int candidate = DhOcclusionGpu.getDhActiveDepthTextureId();
            if (candidate > 0) {
                dhTex = candidate;
                dhNear = DhClientState.dhNear();
                dhFar = DhClientState.dhFar();
                dhReverseZ = DhClientState.dhReverseZ();
                if (!(dhNear > 0.0F) || !(dhFar > dhNear)) {
                    dhTex = -1; // параметры декода не пригодны - слой пуст по конвенции
                    noteOnce("dh-params-invalid", "DH clip params invalid: near=" + dhNear + " far=" + dhFar
                            + " - LOD layer OFF, LOD pixels will read as sky");
                }
            } else {
                noteOnce("dh-tex-missing", "DhClientState active but DhOcclusionGpu returned texture id "
                        + candidate + " - LOD layer OFF (reflective read into DH failed?)");
            }
        } else {
            noteEvery("dh-inactive", STATE_EVERY_FRAMES,
                    "DhClientState.isActive()==false (DH absent or bridge silent >500 ms) - LOD layer OFF");
        }
        return new Inputs(color, width, height, depth, dhTex, dhNear, dhFar, dhReverseZ);
    }

    /** Проекция текущего кадра (для unproject в шейдере) + modelView(±Inverse) + камера. */
    private static boolean fillFrameMatrices() throws Throwable {
        if (headSnapshotValid) {
            // Гарантия корректного состояния буферов на момент upload: если кто-то
            // читал их как источник, position/limit съезжают, у glUniformMatrix4fv
            // count становится 0 и upload МОЛЧА не происходит (uniform остаётся
            // нулевым, inverse(uProj) мусор, шейдер discard-ит весь кадр - наш
            // no-op без ошибок). Три копии матриц дешевле повторного теста.
            projBuf.clear();
            hbmProj.get(projBuf);
            projBuf.limit(16).position(0); // JOML get() пишет абсолютно и НЕ двигает позицию: flip() давал limit=0 => remaining=0 => count=0 у glUniformMatrix4fv (тихий no-op, матрицы не грузились вовсе)
            modelViewBuf.clear();
            hbmMv.get(modelViewBuf);
            modelViewBuf.limit(16).position(0);
            modelViewInvBuf.clear();
            hbmMvInv.get(modelViewInvBuf);
            modelViewInvBuf.limit(16).position(0);
            return true;
        }
        if (capturedRenderingState == null || getGbufferProjection == null) {
            skipFrame("CapturedRenderingState unresolved");
            return false;
        }
        Matrix4fc proj = (Matrix4fc) getGbufferProjection.invoke(capturedRenderingState);
        if (proj == null) {
            skipFrame("gbuffer projection null");
            return false;
        }
        hbmProj.set(proj);
        projBuf.clear();
        hbmProj.get(projBuf);
        projBuf.limit(16).position(0); // JOML get() пишет абсолютно и НЕ двигает позицию: flip() давал limit=0 => remaining=0 => count=0 у glUniformMatrix4fv (тихий no-op, матрицы не грузились вовсе)

        if (getGbufferModelView == null) {
            skipFrame("gbuffer modelView unresolved (reflection)");
            return false;
        }
        // getGbufferModelViewInverse в Iris 1.8+/Oculus 1.8 не существует (javap по
        // Oculus 1.8.0 и исходнику Iris 1.21.1: есть только modelView/projection).
        Matrix4fc mv = (Matrix4fc) getGbufferModelView.invoke(capturedRenderingState);
        if (mv == null) {
            skipFrame("gbuffer modelView null");
            return false;
        }
        // uModelView - матрица ДО инверсии (репроекция world→uv в тенях).
        hbmMv.set(mv);
        modelViewBuf.clear();
        hbmMv.get(modelViewBuf);
        modelViewBuf.limit(16).position(0);
        hbmMvInv.set(mv).invert();
        modelViewInvBuf.clear();
        hbmMvInv.get(modelViewInvBuf);
        modelViewInvBuf.limit(16).position(0);

        net.minecraft.world.phys.Vec3 cam = net.minecraft.client.Minecraft.getInstance()
                .gameRenderer.getMainCamera().getPosition();
        camX = (float) cam.x;
        camY = (float) cam.y;
        camZ = (float) cam.z;
        return true;
    }

    /**
     * Дистанция до near-плоскости MC-проекции (модуль view-z точки, которую
     * gProjInv отображает в NDC z=-1): для перспективной матрицы z_view =
     * m32/(1-m22) (JOML m<col><row>!), ваниль даёт 0.05. Раньше брали m23():
     * в JOML это элемент w-строки перспективной матрицы, ВСЕГДА -1.0, и при
     * m22≈-1 формула выдавала 0.5 вместо 0.05 - DH-лоды притягивались к
     * камере в 10 раз, гася и их свет, и воздушный ореол за ними.
     */
    private static float mcNearPlane() {
        float denom = 1.0F - hbmProj.m22();
        if (Math.abs(denom) < 1.0E-6F) {
            return 0.05F;
        }
        float z = hbmProj.m32() / denom;
        return Float.isFinite(z) && Math.abs(z) > 1.0E-4F ? Math.abs(z) : 0.05F;
    }

    // ------------------------------------------------------------------
    // Отрисовка с расширенным save/restore GL-стейта (отчёт §7.14).
    // Капы/атрибуты - ТОЛЬКО raw GL (см. javadoc класса): Mojiang-обёртки под
    // Iris/Oculus могут быть перехвачены миксинами локов. FBO/текстурные
    // биндинги - через GlStateManager (доказанно работают).
    // ------------------------------------------------------------------

    private static void draw(Inputs in) {
        // --- SAVE (чтение прежних значений - raw) ---
        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevActiveTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int prevBlendSrcRgb = GL11.glGetInteger(GL20.GL_BLEND_SRC_RGB);
        int prevBlendDstRgb = GL11.glGetInteger(GL20.GL_BLEND_DST_RGB);
        int prevBlendSrcAlpha = GL11.glGetInteger(GL20.GL_BLEND_SRC_ALPHA);
        int prevBlendDstAlpha = GL11.glGetInteger(GL20.GL_BLEND_DST_ALPHA);
        // Уравнение - отдельное состояние, не входящее в src/dst-факторы: пак,
        // оставивший GL_MAX/FUNC_SUBTRACT, превратил бы ONE,ONE в max()/вычитание
        // (внешне - «света нет»), хотя проба факторов зелёная.
        int prevBlendEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        boolean stencilWasEnabled = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        // Снапшот ГЛУШИТЕЛЕЙ растеризации: каждый из них даёт ИМЕННО наш симптом -
        // glDrawArrays без GL-ошибки и без единого записанного пикселя. Оставить
        // включённым мог паковый compute-пасс или соседний инстанс-рендер к моменту
        // TAIL. Restore - в finally (идемпотентно на любом выходе).
        boolean rasterizerDiscardWasEnabled = GL11.glIsEnabled(0x8C89); // GL_RASTERIZER_DISCARD
        boolean logicOpWasEnabled = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
        int logicOpMode = logicOpWasEnabled ? GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE) : 0;
        boolean sampleMaskWasEnabled = GL11.glIsEnabled(0x8E51); // GL_SAMPLE_MASK
        // glGetIntegerv(GL_POLYGON_MODE) кладёт пару [front, back] (GL_POINT/LINE/FILL).
        int[] prevPolyMode = new int[2];
        GL11.glGetIntegerv(0x0B40 /* GL_POLYGON_MODE */, prevPolyMode);
        boolean[] clipWasEnabled = new boolean[8];
        for (int i = 0; i < clipWasEnabled.length; i++) {
            clipWasEnabled[i] = GL11.glIsEnabled(0x3000 + i); // GL_CLIP_DISTANCE0+i
        }

        int prevViewportX;
        int prevViewportY;
        int prevViewportW;
        int prevViewportH;
        boolean[] prevColorMask = new boolean[4];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(16);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            prevViewportX = viewport.get(0);
            prevViewportY = viewport.get(1);
            prevViewportW = viewport.get(2);
            prevViewportH = viewport.get(3);
            ByteBuffer mask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < 4; i++) {
                prevColorMask[i] = mask.get(i) != 0;
            }
        }

        // draw buffer(s) чужого FBO НЕ сохраняем и НЕ восстанавливаем:
        // glDrawBuffers зовётся только на СОБСТВЕННОМ FBO (ensureFbo), пер-FBO
        // состояние чужих таргетов мы мутировать не можем в принципе. Прежний
        // «контрольный restore» был дефектом: он сравнивал GL_DRAW_BUFFER0 НАШЕГО
        // ещё привязанного FBO (COLOR_ATTACHMENT0) с сохранённым значением чужого
        // FBO и при расхождении звал glDrawBuffers на НАШ FBO - легальный для FBO
        // GL_NONE превращал наш ПОСТОЯННЫЙ FBO в молча не пишущий навсегда
        // (ensureFbo на пути переиспользования glDrawBuffers заново не зовёт).

        // Биндинги текстур на юнитах, которые мы занимаем (0 = depthtex0, 1 = dhDepth).
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        int prevUnit0Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13.GL_TEXTURE1);
        int prevUnit1Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(GL13.GL_TEXTURE2);
        int prevUnit2Tex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // Кинематографический слой: жив только при рабочей копии цвета сцены
        // и nukeCinematics > 0; тогда пасс рисует СОСТАВЛЕННУЮ картинку с
        // выключенным блендингом. Иначе - классический аддитив round-8.
        float cine = cinematics();
        boolean sceneActive = false;
        if (sceneCopySupported && cine > 0.001F && !DEBUG_REPLACE && !SOLID_ADD) {
            sceneActive = copySceneTexture(in.colorTexture(), in.width(), in.height());
        }

        // FBO создаём/проверяем ДО любых мутаций стейта: неудача здесь (ребилд
        // пайплайна) - тихий скип кадра; save-блок выше ушёл с юнита
        // prevActiveTex, поэтому возвращаем его явно (чек-лист §2.1 - полный
        // restore на любом выходе).
        if (!ensureFbo(in.colorTexture())) {
            GlStateManager._activeTexture(prevActiveTex);
            return;
        }

        boolean depthBindingsActive = false;
        int[] savedFilters0 = new int[2];
        int[] savedFilters1 = new int[2];
        try {
            // --- ИЗМЕНЕНИЕ СТЕЙТА - только raw GL ---
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (stencilWasEnabled) {
                // В MC 1.20.1/1.21.1 нет Mojang-API тумблера стенсила
                // (GlStateManager без _enable/_disableStencil, RenderSystem без
                // enableStencil) - raw GL безопасен: никто не кеширует этот cap.
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            }
            if (scissorWasEnabled) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
            GL11.glColorMask(true, true, true, true);
            if (DEBUG_REPLACE) {
                // Диагностика cause#1: замена цвета (маджента из FSH), блендинг
                // выключен - конфигурация, доказанно видимая пользователю.
                GL11.glDisable(GL11.GL_BLEND);
            } else if (sceneActive) {
                // Кинематика: шейдер пишет СОСТАВЛЕННУЮ картинку (альбедо из
                // копии сцены + аддитив с выгоранием) - блендинг должен быть
                // выключен, иначе состав заменяется на сложение.
                GL11.glDisable(GL11.GL_BLEND);
            } else {
                // Аддитивная заливка: цвет складывается с цветом пака, альфа не
                // трогается (пакеты кладут в альфу colortex0 свои данные).
                GL11.glEnable(GL11.GL_BLEND);
                GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
                GL20.glBlendEquation(GL20.GL_FUNC_ADD);
            }
            // depthMask НЕ меняем: наш FBO вообще без depth-attachment, писать
            // глубину негде; сохранённое значение восстановлено ниже по чек-листу.

            // --- Глушители растеризации: гасим (restore в finally) ---
            if (rasterizerDiscardWasEnabled) {
                GL11.glDisable(0x8C89);
            }
            if (logicOpWasEnabled) {
                GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
            }
            if (sampleMaskWasEnabled) {
                GL11.glDisable(0x8E51);
            }
            GL11.glPolygonMode(0x408 /* FRONT_AND_BACK */, 0x1B02 /* FILL */);
            for (int i = 0; i < clipWasEnabled.length; i++) {
                if (clipWasEnabled[i]) {
                    GL11.glDisable(0x3000 + i);
                }
            }

            GL11.glViewport(0, 0, in.width(), in.height());

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            // Переаттач КАЖДЫЙ кадр (GL 4.6 §9.2.8.1). RenderTarget.resize() удаляет
            // текстуру main RT и GL переиспользует освободившееся имя - id НЕ меняется,
            // а по §9.2.6 удаление НЕ отцепляет текстуру от небаунд-FBO: аттачмент
            // остаётся ссылаться на мёртвый образ (статус COMPLETE, glErr 0x0), дров
            // молча уходит в никуда до рестарта процесса. Повторный attach по имени
            // вяжет ЖИВОЙ объект - лечит zombie при любом ресайзе, включая same-size.
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, in.colorTexture(), 0);
            fboAttachedTexture = in.colorTexture();
            // Дровер-бафферы своего FBO тоже перезаявляем: исторический дефект
            // оставлял тут GL_NONE и не лечился до рестарта (см. коммент выше).
            GL20.glDrawBuffers(FBO_DRAW_BUFFERS);

            GL20.glUseProgram(program);
            GL20.glUniform1i(uDepthTex0Loc, 0);
            GL20.glUniform1i(uDhDepthTexLoc, 1);
            GL20.glUniformMatrix4fv(uProjLoc, false, projBuf);
            GL20.glUniformMatrix4fv(uModelViewLoc, false, modelViewBuf);
            GL20.glUniformMatrix4fv(uModelViewInvLoc, false, modelViewInvBuf);
            GL20.glUniform3f(uCamPosLoc, camX, camY, camZ);
            GL20.glUniform4fv(uSourcesLoc, sources);
            GL20.glUniform1i(uSourceCountLoc, sourceCount);
            GL20.glUniform1fv(uSrcRadiusLoc, srcRadius);
            GL20.glUniform1f(uIntensityLoc, intensity());
            GL20.glUniform1f(uHeatScaleLoc, HEAT_SCALE);
            GL20.glUniform2f(uResolutionLoc, in.width(), in.height());
            GL20.glUniform1f(uNormalScaleLoc, NORMAL_SCALE);
            GL20.glUniform1f(uDebugReplaceLoc, DEBUG_REPLACE ? 1.0F : 0.0F);
            GL20.glUniform1f(uSolidAddLoc, SOLID_ADD ? 1.0F : 0.0F);
            // Непрерывные параметры ударной волны (радиус, затухание) - пары.
            GL20.glUniform2fv(uSrcShockLoc, srcShock);
            GL20.glUniform1i(uSceneColorLoc, 2);
            GL20.glUniform1f(uHasSceneLoc, sceneActive ? 1.0F : 0.0F);
            GL20.glUniform1f(uCineLoc, cine);
            GL20.glUniform1f(uDhReverseZLoc, in.dhReverseZ() ? 1.0F : 0.0F);
            GL20.glUniform1f(uDhPresentLoc, in.dhTexture() > 0 ? 1.0F : 0.0F);
            GL20.glUniform1f(uDhNearLoc, in.dhTexture() > 0 ? in.dhNear() : 1.0F);
            GL20.glUniform1f(uDhFarLoc, in.dhTexture() > 0 ? in.dhFar() : 2.0F);
            GL20.glUniform1f(uDhFadeDistLoc, in.dhTexture() > 0
                    ? com.hbm_m.client.compat.dh.DhOcclusionGpu.ditherFadeMaskDistance() : 0.0F);
            // Дистанция до near-плоскости MC-проекции (NDC z=-1). В шейдере DH-
            // реконструкция не зависит от uniform: масштаб dist/-nearView.z.
            // Остаётся для диагностики (логи mcNear).
            GL20.glUniform1f(uMcNearLoc, mcNearPlane());
            // Граница ванильной геометрии (DhOcclusionGpu.vanillaFarPlane = (RD+1)*16):
            // главный RT дальше неё - под-пороговый не-геометрический контент,
            // в ванильную ветку не годится (F-1, round-5).
            GL20.glUniform1f(uVanillaFarLoc, DhOcclusionGpu.vanillaFarPlane());

            bindDepthTexture(GL13.GL_TEXTURE0, in.depthTexture(), savedFilters0);
            depthBindingsActive = true;
            if (in.dhTexture() > 0) {
                bindDepthTexture(GL13.GL_TEXTURE1, in.dhTexture(), savedFilters1);
            }
            if (sceneActive) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE2);
                GlStateManager._bindTexture(sceneTex);
            }

            GlStateManager._glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            int drawErr = GL11.glGetError();
            if (drawErr != 0) {
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: glDrawArrays set GL error 0x{} - дров не выполнился",
                        Integer.toHexString(drawErr));
            }

            // --- RESTORE (в порядке, обратном изменению) - тоже raw GL ---
            // Юнит 1: фильтры + биндинг (через GlStateManager, чтобы кеш был честным).
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            if (in.dhTexture() > 0) {
                restoreDepthFilters(in.dhTexture(), savedFilters1);
                GlStateManager._bindTexture(prevUnit1Tex);
            }
            // Юнит 0: как в RawDhDepthCopy - вернуть ПРЕДЫДУЩУЮ текстуру юнита 0,
            // иначе после нас ванильные _bindTexture станут no-op (чёрные квадраты).
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            restoreDepthFilters(in.depthTexture(), savedFilters0);
            GlStateManager._bindTexture(prevUnit0Tex);
            // Юнит 2 - наша копия сцены: вернуть предыдущий биндинг.
            GlStateManager._activeTexture(GL13.GL_TEXTURE2);
            GlStateManager._bindTexture(prevUnit2Tex);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            // Юниты и фильтры вернуты: finally их больше не трогает (иначе
            // вернул бы фильтры глубины на чужую текстуру, уже повешенную на юнит).
            depthBindingsActive = false;

            GL11.glViewport(prevViewportX, prevViewportY, prevViewportW, prevViewportH);
            GL11.glColorMask(prevColorMask[0], prevColorMask[1], prevColorMask[2], prevColorMask[3]);
            if (blendWasEnabled) {
                GL11.glEnable(GL11.GL_BLEND);
            } else {
                // Мы включали блендинг сами - вернуть выключенным, иначе весь
                // последующий кадр рисуется с лишним блендингом.
                GL11.glDisable(GL11.GL_BLEND);
            }
            GL14.glBlendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);
            GL20.glBlendEquation(prevBlendEquation);
            if (scissorWasEnabled) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            if (stencilWasEnabled) {
                GL11.glEnable(GL11.GL_STENCIL_TEST);
            }
            GL11.glDepthFunc(prevDepthFunc);
            if (depthTestWasEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthMask(prevDepthMask);
            if (cullWasEnabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        } finally {
            // Глушители растеризации вернуть ДОЖИВШИМИ до исключения тоже:
            // утёкший LINE/RASTERIZER_DISCARD ломал бы рендер всей игры после нас.
            if (rasterizerDiscardWasEnabled) {
                GL11.glEnable(0x8C89);
            }
            if (logicOpWasEnabled) {
                GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
                GL11.glLogicOp(logicOpMode);
            }
            if (sampleMaskWasEnabled) {
                GL11.glEnable(0x8E51);
            }
            if (prevPolyMode[0] != 0x1B02) {
                // Только FRONT_AND_BACK: FRONT/BACK по отдельности в core profile
                // дают GL_INVALID_ENUM ("Polygon modes for <face> are disabled").
                GL11.glPolygonMode(0x408 /* FRONT_AND_BACK */, prevPolyMode[0]);
            }
            for (int i = 0; i < clipWasEnabled.length; i++) {
                if (clipWasEnabled[i]) {
                    GL11.glEnable(0x3000 + i);
                }
            }
            // Полный restore на ЛЮБОМ выходе (чек-лист §7.14 restore-всё-или-ничего):
            // на нормальном пути всё идемпоцентно; при исключении между биндингами
            // глубины и restore возвращает NEAREST-фильтры и биндинги юнитов 0/1,
            // active texture, program, VAO и оба FBO-биндинга.
            if (depthBindingsActive) {
                GlStateManager._activeTexture(GL13.GL_TEXTURE1);
                if (in.dhTexture() > 0) {
                    restoreDepthFilters(in.dhTexture(), savedFilters1);
                    GlStateManager._bindTexture(prevUnit1Tex);
                }
                GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                restoreDepthFilters(in.depthTexture(), savedFilters0);
                GlStateManager._bindTexture(prevUnit0Tex);
                GlStateManager._activeTexture(GL13.GL_TEXTURE2);
                GlStateManager._bindTexture(prevUnit2Tex);
            }
            GL20.glUseProgram(prevProgram);
            GlStateManager._glBindVertexArray(prevVao);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);
            GlStateManager._activeTexture(prevActiveTex);
        }
    }

    /**
     * FBO с цветовой текстурой main RT в COLOR_ATTACHMENT0. Главный FBO Minecraft
     * в этот момент не привязан, поэтому чтение его depth-текстуры как сэмплера
     * при записи цвета через НАШ FBO - без feedback (у нашего FBO depth нет).
     * Оба биндинга (DRAW и READ) сохраняются и восстанавливаются на ЛЮБОМ исходе:
     * glBindFramebuffer(GL_FRAMEBUFFER) ставит и READ, который при удалении
     * incomplete-FBO по спеке GL откатился бы в 0, а не в прежнее значение.
     */
    private static boolean ensureFbo(int colorTexture) {
        // Гарда по id текстуры убрана как условие переиспользования: resize
        // переиспользует имя и id совпадает при мёртвом аттачменте. Живой
        // переаттач теперь делает draw() каждый кадр; здесь только валидность FBO.
        if (fbo != -1 && GL30.glIsFramebuffer(fbo)) {
            return true;
        }
        if (fbo != -1) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = -1;
            fboAttachedTexture = -1;
            fboAttachedAlt = -1;
        }
        fbo = GL30.glGenFramebuffers();
        int prevDrawFb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, colorTexture, 0);
        GL20.glDrawBuffers(FBO_DRAW_BUFFERS);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        // Restore обоих биндингов ДО проверки статуса - на failure-пути тоже.
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFb);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = -1;
            return false;
        }
        fboAttachedTexture = colorTexture;
        fboAttachedAlt = -1;
        return true;
    }

    /**
     * Биндинг depth-текстуры с принудительным GL_NEAREST (DEPTH32F + LINEAR = 0.0).
     * Предыдущие фильтры читаются ДО перезаписи и складываются в {@code savedOut}.
     */
    private static void bindDepthTexture(int unit, int textureId, int[] savedOut) {
        GlStateManager._activeTexture(unit);
        GlStateManager._bindTexture(textureId);
        savedOut[0] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        savedOut[1] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    private static void restoreDepthFilters(int textureId, int[] savedFilters) {
        if (textureId <= 0) {
            return;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, savedFilters[0]);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, savedFilters[1]);
    }

    /**
     * Копия цвета main RT в нашу RGBA8-текстуру (GL 4.3 texture-to-texture,
     * FBO-биндинги не трогаются). Первый сбой (несовместимый внутренний формат
     * чужого пайплайна) навсегда глушит кинематографический слой - пасс
     * возвращается к классическому аддитиву round-8.
     */
    private static boolean copySceneTexture(int srcTexture, int w, int h) {
        if (sceneTex != -1 && (sceneTexW != w || sceneTexH != h)) {
            GL11.glDeleteTextures(sceneTex);
            sceneTex = -1;
        }
        if (sceneTex == -1) {
            sceneTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            // LINEAR: копию сэмплирует рефракция ударной волны со смещёнными UV.
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 0x812D /* GL_CLAMP_TO_EDGE */);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 0x812D /* GL_CLAMP_TO_EDGE */);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            sceneTexW = w;
            sceneTexH = h;
        }
        GL43.glCopyImageSubData(srcTexture, GL11.GL_TEXTURE_2D, 0, 0, 0, 0,
                sceneTex, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, w, h, 1);
        boolean ok = GL11.glGetError() == GL11.GL_NO_ERROR;
        if (!ok) {
            sceneCopySupported = false;
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: scene color copy failed (GL error) -"
                    + " cinematics permanently disabled, falling back to additive light");
        }
        return ok;
    }

    // ------------------------------------------------------------------
    // Одноразовые CPU-пробы следующего прогона (только лог, без latch)
    // ------------------------------------------------------------------

    /** src0 как строка для state/FIRST LIGHT строк (уже с LIFT по y). */
    private static String src0String() {
        if (sourceCount <= 0) {
            return "none";
        }
        return "(" + sources[0] + ", " + sources[1] + ", " + sources[2] + ", heat=" + sources[3] + ")";
    }

    /** Дистанция камера -> источник 0 (уже с LIFT по y; или -1 при отсутствии источников). */
    private static double camDist0() {
        if (sourceCount <= 0) {
            return -1.0;
        }
        double dx = sources[0] - camX;
        double dy = sources[1] - camY;
        double dz = sources[2] - camZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ------------------------------------------------------------------
    // GL-ресурсы (форк RawDhDepthCopy.ensureProgram/ensureQuad)
    // ------------------------------------------------------------------

    private static boolean ensureProgram() {
        if (program != -1 && GL20.glIsProgram(program)) {
            return true;
        }
        try {
            int vs = compile(GL20.GL_VERTEX_SHADER, VSH);
            int fs = compile(GL20.GL_FRAGMENT_SHADER, FSH);
            if (vs == 0 || fs == 0) return false;
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vs);
            GL20.glAttachShader(program, fs);
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                MainRegistry.LOGGER.warn("RawScreenSpaceLight: link failed: {}", GL20.glGetProgramInfoLog(program, 4096));
                program = -1;
                return false;
            }
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            uDepthTex0Loc = GL20.glGetUniformLocation(program, "uDepthTex0");
            uDhDepthTexLoc = GL20.glGetUniformLocation(program, "uDhDepthTex");
            uDhReverseZLoc = GL20.glGetUniformLocation(program, "uDhReverseZ");
            uDhPresentLoc = GL20.glGetUniformLocation(program, "uDhPresent");
            uDhNearLoc = GL20.glGetUniformLocation(program, "uDhNear");
            uDhFarLoc = GL20.glGetUniformLocation(program, "uDhFar");
            uDhFadeDistLoc = GL20.glGetUniformLocation(program, "uDhFadeDist");
            uMcNearLoc = GL20.glGetUniformLocation(program, "uMcNear");
            uProjLoc = GL20.glGetUniformLocation(program, "uProj");
            uModelViewLoc = GL20.glGetUniformLocation(program, "uModelView");
            uModelViewInvLoc = GL20.glGetUniformLocation(program, "uModelViewInv");
            uCamPosLoc = GL20.glGetUniformLocation(program, "uCamPos");
            uSourcesLoc = GL20.glGetUniformLocation(program, "uSources[0]");
            uSourceCountLoc = GL20.glGetUniformLocation(program, "uSourceCount");
            uSrcRadiusLoc = GL20.glGetUniformLocation(program, "uSrcRadius[0]");
            uVanillaFarLoc = GL20.glGetUniformLocation(program, "uVanillaFar");
            uIntensityLoc = GL20.glGetUniformLocation(program, "uIntensity");
            uHeatScaleLoc = GL20.glGetUniformLocation(program, "uHeatScale");
            uResolutionLoc = GL20.glGetUniformLocation(program, "uResolution");
            uNormalScaleLoc = GL20.glGetUniformLocation(program, "uNormalScale");
            uDebugReplaceLoc = GL20.glGetUniformLocation(program, "uDebugReplace");
            uSolidAddLoc = GL20.glGetUniformLocation(program, "uSolidAdd");
            uSrcShockLoc = GL20.glGetUniformLocation(program, "uSrcShock[0]");
            uSceneColorLoc = GL20.glGetUniformLocation(program, "uSceneColor");
            uHasSceneLoc = GL20.glGetUniformLocation(program, "uHasScene");
            uCineLoc = GL20.glGetUniformLocation(program, "uCine");
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: program init failed: {}", t.toString());
            program = -1;
            return false;
        }
    }

    private static int compile(int type, String source) {
        int sh = GL20.glCreateShader(type);
        com.hbm_m.platform.RenderHooks.safeShaderSource(sh, source);
        GL20.glCompileShader(sh);
        if (GL20.glGetShaderi(sh, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: compile failed: {}", GL20.glGetShaderInfoLog(sh, 4096));
            return 0;
        }
        return sh;
    }

    private static boolean ensureQuad() {
        if (vao != -1 && GL30.glIsVertexArray(vao)) {
            return true;
        }
        try {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            int prevArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GlStateManager._glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            // TRIANGLE_STRIP: BL, BR, TL, TR
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, new float[]{
                    -1f, -1f, 0f,
                    1f, -1f, 0f,
                    -1f, 1f, 0f,
                    1f, 1f, 0f}, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
            GlVaoSafety.bindVertexArray(prevVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prevArrayBuffer);
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("RawScreenSpaceLight: quad init failed: {}", t.toString());
            return false;
        }
    }
}
