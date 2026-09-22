package com.hbm_m.client.render.shader;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
/**
 * Фабрика наших {@code ExtendedShader} для истинного glDrawElementsInstanced под
 * Iris/Oculus: шейдер {@code hbm_m:iris/block_lit_instanced_iris} (наш GLSL,
 * юниформы с префиксом {@code iris_} — ExtendedShader.getUniform() резолвит
 * {@code "iris_" + name}) рисует в gbuffer/shadow-FB активного пайплайна.
 * <p>
 * <b>Кадры (framebuffer)</b>: {@code ExtendedShader.apply()} сам биндит
 * {@code writingToBefore/AfterTranslucent} — поэтому мы СНАПШОТИМ живой
 * {@code GL_DRAW_FRAMEBUFFER_BINDING} (в момент вызова pack-шейдер уже забиндил
 * правильный gbuffer/shadow-FB): читаем имена аттачментов 0..7 + depth и строим
 * свои {@code GlFramebuffer} с теми же текстурами. Оба слота пары наполняются
 * одинаково — какой из них apply() выберет по {@code isBeforeTranslucent}, не важно.
 * {@code drawBuffers([COLOR_ATTACHMENT0])} — vanilla-parity вывода: наш FSH имеет
 * один {@code fragColor} (как iris-fallback ShaderSynthesizer), остальные
 * аттачменты не получают undefined-мусор.
 * <p>
 * Конструктор ExtendedShader <b>идентичен</b> на Iris 1.20.1 (1.7.6) и 1.21.1 (1.8+)
 * — одна рефлексия без адаптера. Consumer/BiConsumer передаются как стёртые
 * {@code Consumer<Object>} (дженерики erased — Iris-классы в компайл-тайм не нужны).
 * <p>
 * Отказобезопасность: любая ошибка рефлексии/GL → {@code available=false} и
 * вызывающий код остаётся на прежнем (companion per-instance) пути.
 */
public final class IrisInstancedShaders {

    private static volatile boolean reflectionTried = false;
    private static volatile boolean available = false;

    private static Constructor<?> extendedShaderCtor;
    private static Object alphaTestOff;          // AlphaTests.OFF
    private static Object emptyCustomUniforms;   // CustomUniforms.Builder().build()
    private static Object pipeline;              // IrisRenderingPipeline активная

    /** Пер-проходный кеш: (поколение пайплайна, fboId, сигнатура аттачментов) → шейдер. */
    private static final long[] cachedGen = { -1L, -1L };
    private static final int[] cachedFbo = { -1, -1 };
    private static final int[] cachedAttachSig = { 0, 0 };
    private static final ShaderInstance[] cachedShader = { null, null };
    private static final String[] cachedShaderName = { null, null };
    private static final Object[][] cachedFbPair = { null, null }; // [before, after] GlFramebuffer

    // Кэш ПРОВАЛА конструкции: без него упавший (например, GLSL-ошибка) шейдер
    // ретраился бы КАЖДЫЙ флаш КАЖДОГО рендерера — компиляция шейдера в цикле =
    // смерть ФПС + спам лога. Провал живёт до пересборки пайплайна.
    private static final boolean[] constructionFailed = { false, false };
    private static final long[] failedAtGeneration = { -1L, -1L };

    // Снапшот gbuffer/shadow-FB per проход (заполняется в captureLiveFramebuffer
    // из IrisRenderBatch.setupOuter — единственный момент, когда правильный FB
    // гарантированно забинджен; к моменту AFTER_BLOCK_ENTITIES
    // closePersistentIfActive уже перекидывает биндинг на main-RenderTarget).
    private static final int[] capturedFbo = { -1, -1 };
    private static final long[] capturedGen = { -1L, -1L };
    private static final int[] capturedSig = { 0, 0 };
    private static final int[][] capturedColors = { new int[8], new int[8] };
    private static final int[] capturedColorCount = { 0, 0 };
    private static final int[] capturedDepth = { -1, -1 };

    private static boolean constructionLogged = false;

    private IrisInstancedShaders() {}

    private static int ours_programId(Object shader) {
        try {
            java.lang.reflect.Method m = shader.getClass().getMethod("getId");
            return (int) m.invoke(shader);
        } catch (Throwable t) {
            return -1;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Одноразовый снапшот живого FB (нужен только как аргумент конструктора
     * ExtendedShader; в рантайме эти FB не используются). Вызывается из
     * IrisRenderBatch.setupOuter после pack-apply.
     */
    public static void captureLiveFramebuffer(boolean shadowPass) {
        if (!ensureReflected()) {
            return;
        }
        int slot = shadowPass ? 1 : 0;
        int liveFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (capturedFbo[slot] == liveFbo) {
            return;
        }
        if (!snapshotAttachments(slot)) {
            MainLog.warn("IrisInstancedShaders: capture(slot=" + slot + ") failed — liveFbo=" + liveFbo);
            return;
        }
        capturedFbo[slot] = liveFbo;
        com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM-M] IrisInstancedShaders: captured slot={}, liveFbo={}, colors=[{}..{}] (n={}), depth={}",
                slot, liveFbo,
                capturedColorCount[slot] > 0 ? capturedColors[slot][0] : -1,
                capturedColorCount[slot] > 0 ? capturedColors[slot][capturedColorCount[slot]-1] : -1,
                capturedColorCount[slot],
                capturedDepth[slot] == -1 ? "none" : String.valueOf(capturedDepth[slot]));
    }

    /** Совместимость: хук остался в InstancedRenderFrame, ныне no-op. */
    public static void onFrameStart() {
    }

    /**
     * Лениво строит наш ExtendedShader (один на проход на всю сессию).
     * КОНСТРУКТОРУ нужны FB-аргументы — отдаём одноразовый снапшот с
     * {@link #captureLiveFramebuffer}; в рантайме мы НИКОГДА не зовём
     * apply()/clear() этого шейдера (они бы забиндили эти FB) — биндинг
     * фреймбуфера делает pack-шейдер, мы подменяем только программу.
     * Поэтому FB-аргументы после конструкции не играют роли, и весь
     * resize/пинг-понг аттачментов пака нам не страшен.
     *
     * @return наш шейдер или null (снапшота ещё не было / рефлексия недоступна).
     */
    public static ShaderInstance getOrCreate(boolean shadowPass) {
        if (!ensureReflected()) {
            return null;
        }
        int slot = shadowPass ? 1 : 0;
        if (cachedShader[slot] != null) {
            // Профиль (схема gbuffer пака) сменился — пересобираем с другим FSH.
            String wanted = IrisInstancedEncoders.shaderName(shadowPass);
            if (!wanted.equals(cachedShaderName[slot])) {
                disposeSlot(slot);
            } else {
                return cachedShader[slot];
            }
        }
        if (capturedFbo[slot] == -1) {
            return null; // FB-снапшот для аргументов конструктора ещё не снят
        }
        long generation = IrisExtendedShaderAccess.getPipelineGeneration();
        if (constructionFailed[slot] && failedAtGeneration[slot] == generation) {
            return null; // уже падало на этой сборке пайплайна — не ретраить каждый кадр
        }
        try {
            Object[] fbs = buildFramebuffers(slot);
            if (fbs == null) {
                return null;
            }
            VertexFormat format = requireVertexFormat();
            String shaderName = IrisInstancedEncoders.shaderName(shadowPass);
            Object shader = extendedShaderCtor.newInstance(
                    Minecraft.getInstance().getResourceManager(),
                    shaderName,
                    format,
                    Boolean.FALSE,
                    fbs[0], fbs[1],
                    null,
                    alphaTestOff,
                    (java.util.function.Consumer<Object>) o -> { },
                    (java.util.function.BiConsumer<Object, Object>) (a, b) -> { },
                    Boolean.FALSE,
                    pipeline,
                    null,
                    emptyCustomUniforms);
            cachedShader[slot] = (ShaderInstance) shader;
            cachedShaderName[slot] = shaderName;
            cachedFbPair[slot] = fbs;
            constructionFailed[slot] = false;
            if (!constructionLogged) {
                constructionLogged = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] IrisInstancedShaders: ExtendedShader built (slot={}, programId={}, captureFbo={})",
                        slot, ours_programId(shader), capturedFbo[slot]);
            }
            return cachedShader[slot];
        } catch (Throwable t) {
            constructionFailed[slot] = true;
            failedAtGeneration[slot] = generation;
            MainLog.warn("IrisInstancedShaders: ExtendedShader construction failed (slot=" + slot
                    + ") - disabled until pipeline rebuild - legacy path", t);
            return null;
        }
    }

    private static VertexFormat requireVertexFormat() {
        ShaderInstance vanilla = ModShaders.getBlockLitInstancedShader();
        if (vanilla == null) {
            throw new IllegalStateException("vanilla block_lit_instanced shader not registered yet");
        }
        return vanilla.getVertexFormat();
    }

    private static Object[] buildFramebuffers(int slot) {
        try {
            Object before = GlFramebufferHolder.newFramebuffer();
            Object after = GlFramebufferHolder.newFramebuffer();
            fillAttachments(slot, before);
            fillAttachments(slot, after);
            return new Object[] { before, after };
        } catch (Throwable t) {
            MainLog.warn("IrisInstancedShaders: framebuffer capture failed", t);
            return null;
        }
    }

    private static void fillAttachments(int slot, Object framebuffer) throws Exception {
        for (int i = 0; i < capturedColorCount[slot]; i++) {
            GlFramebufferHolder.addColorAttachment(framebuffer, i, capturedColors[slot][i]);
        }
        if (capturedDepth[slot] != -1) {
            GlFramebufferHolder.addDepthAttachment(framebuffer, capturedDepth[slot]);
        }
        // На запись только color attachment 0 (наш FSH имеет один выход).
        GlFramebufferHolder.drawBuffers0(framebuffer, capturedColorCount[slot] > 0 ? 1 : 0);
    }

    /** Снимает имена текстур текущего GL_DRAW_FRAMEBUFFER в стейджинг. */
    private static boolean snapshotAttachments(int slot) {
        for (int i = 0; i < capturedColors[slot].length; i++) {
            capturedColors[slot][i] = 0;
        }
        capturedColorCount[slot] = 0;
        capturedDepth[slot] = -1;
        for (int i = 0; i < 8; i++) {
            int name = queryAttachment(GL30.GL_COLOR_ATTACHMENT0 + i);
            if (name != 0) {
                capturedColors[slot][capturedColorCount[slot]++] = name;
            }
        }
        int depthType = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (depthType == GL11.GL_TEXTURE) {
            capturedDepth[slot] = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        }
        if (capturedColorCount[slot] == 0) {
            MainLog.warn("IrisInstancedShaders: live framebuffer has no color attachments — skipping");
            return false;
        }
        return true;
    }

    private static int attachmentSignature(int slot) {
        int sig = capturedColorCount[slot] * 31 + capturedDepth[slot];
        for (int i = 0; i < capturedColorCount[slot]; i++) {
            sig = sig * 31 + capturedColors[slot][i];
        }
        return sig;
    }

    private static int queryAttachment(int attachment) {
        int type = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER, attachment, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        if (type == GL11.GL_TEXTURE) {
            return GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_DRAW_FRAMEBUFFER, attachment, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        }
        return 0;
    }

    private static synchronized boolean ensureReflected() {
        if (reflectionTried) {
            return available;
        }
        reflectionTried = true;
        try {
            Class<?> extendedShader = Class.forName("net.irisshaders.iris.pipeline.programs.ExtendedShader");
            Class<?> glFramebuffer = Class.forName("net.irisshaders.iris.gl.framebuffer.GlFramebuffer");

            extendedShaderCtor = extendedShader.getDeclaredConstructor(
                    net.minecraft.server.packs.resources.ResourceProvider.class,
                    String.class,
                    VertexFormat.class,
                    boolean.class,
                    glFramebuffer,
                    glFramebuffer,
                    Class.forName("net.irisshaders.iris.gl.blending.BlendModeOverride"),
                    Class.forName("net.irisshaders.iris.gl.blending.AlphaTest"),
                    java.util.function.Consumer.class,
                    java.util.function.BiConsumer.class,
                    boolean.class,
                    Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline"),
                    Class.forName("java.util.List"),
                    Class.forName("net.irisshaders.iris.uniforms.custom.CustomUniforms"));
            extendedShaderCtor.setAccessible(true);

            Field off = Class.forName("net.irisshaders.iris.gl.blending.AlphaTests").getField("OFF");
            alphaTestOff = off.get(null);

            Object builder = Class.forName("net.irisshaders.iris.uniforms.custom.CustomUniforms$Builder")
                    .getDeclaredConstructor().newInstance();
            Method build = builder.getClass().getMethod("build", java.util.function.Consumer[].class);
            emptyCustomUniforms = build.invoke(builder, new Object[] { new java.util.function.Consumer[0] });

            Object manager = iris().getMethod("getPipelineManager").invoke(null);
            Object nullable = manager.getClass().getMethod("getPipelineNullable").invoke(manager);
            if (nullable == null) {
                MainLog.warn("IrisInstancedShaders: Iris pipeline is null — instanced path disabled", null);
                return false;
            }
            pipeline = nullable;
            available = true;
        } catch (Throwable t) {
            MainLog.warn("IrisInstancedShaders: reflection init failed — instanced Iris path disabled", t);
            available = false;
        }
        return available;
    }

    private static Class<?> iris() throws ClassNotFoundException {
        return Class.forName("net.irisshaders.iris.Iris");
    }

    private static void disposeFramebuffers(int slot) {
        if (cachedFbPair[slot] != null) {
            for (Object fb : cachedFbPair[slot]) {
                if (fb instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        cachedFbPair[slot] = null;
    }

    private static void disposeSlot(int slot) {
        if (cachedShader[slot] != null) {
            try {
                cachedShader[slot].close();
            } catch (Throwable ignored) {
            }
            cachedShader[slot] = null;
        }
        disposeFramebuffers(slot);
        cachedFbo[slot] = -1;
        cachedGen[slot] = -1L;
        cachedAttachSig[slot] = 0;
        cachedShaderName[slot] = null;
        constructionFailed[slot] = false;
        failedAtGeneration[slot] = -1L;
    }

    /** Отложенный хост для GlFramebuffer-рефлексии (классы Iris только по имени). */
    private static final class GlFramebufferHolder {
        private static final Constructor<?> NO_ARG_CTOR;
        private static final Method ADD_COLOR;
        private static final Method ADD_DEPTH;
        private static final Method DRAW_BUFFERS;

        static {
            try {
                Class<?> fb = Class.forName("net.irisshaders.iris.gl.framebuffer.GlFramebuffer");
                NO_ARG_CTOR = fb.getDeclaredConstructor();
                NO_ARG_CTOR.setAccessible(true);
                ADD_COLOR = fb.getMethod("addColorAttachment", int.class, int.class);
                ADD_DEPTH = fb.getMethod("addDepthAttachment", int.class);
                DRAW_BUFFERS = fb.getMethod("drawBuffers", int[].class);
            } catch (Throwable t) {
                throw new ExceptionInInitializerError(t);
            }
        }

        static Object newFramebuffer() throws Exception {
            return NO_ARG_CTOR.newInstance();
        }

        static void addColorAttachment(Object fb, int index, int texture) throws Exception {
            ADD_COLOR.invoke(fb, index, texture);
        }

        static void addDepthAttachment(Object fb, int texture) throws Exception {
            ADD_DEPTH.invoke(fb, texture);
        }

        static void drawBuffers0(Object fb, int count) throws Exception {
            // ВНИМАНИЕ: Iris GlFramebuffer.drawBuffers принимает ИНДЕКСЫ аттачментов
            // и сам прибавляет GL_COLOR_ATTACHMENT0 (иначе IllegalArgumentException
            // "attachment with index 36064" — см. debug.log 0913).
            int[] buffers = new int[count];
            for (int i = 0; i < count; i++) {
                buffers[i] = i;
            }
            DRAW_BUFFERS.invoke(fb, (Object) buffers);
        }
    }

    /** Локальный логгер — не тащить MainRegistry в каждое место рефлексии. */
    private static final class MainLog {
        static void warn(String message) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(message);
        }

        static void warn(String message, Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(message, t);
        }
    }
}
