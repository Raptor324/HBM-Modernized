package com.hbm_m.client.render;

import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * ВРЕМЕННАЯ диагностика «чёрного экрана»: компактный снимок глобального
 * GL-состояния не чаще {@value #INTERVAL_MS} мс НА ТЕГ (одна строка).
 * Формат собирается через String.format — SLF4J-плейсхолдеры не годятся,
 * {:#03x} остаётся литералом и сдвигает всю таблицу аргументов.
 */
public final class FrameStateProbe {

    /** По умолчанию ВЫКЛЮЧЕН: чтение GL-состояния + glReadPixels каждый кадр —
     *  это только для отладки. Включать -Dhbm.fsp=1 (или env HBM_FSP). */
    private static final boolean ENABLED =
            Boolean.getBoolean("hbm.fsp") || System.getenv("HBM_FSP") != null;

    private static final long INTERVAL_MS = 400;
    private static final ConcurrentHashMap<String, Long> lastPerTag = new ConcurrentHashMap<>();

    private FrameStateProbe() {}

    public static void snap(String tag) {
        if (!ENABLED || !RenderSystem.isOnRenderThread()) return;
        long now = System.currentTimeMillis();
        Long prev = lastPerTag.get(tag);
        if (prev != null && now - prev < INTERVAL_MS) return;
        lastPerTag.put(tag, now);
        try {
            int prog = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            int fb = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            // READ-биндинг и отложенный GL-флаг: расхождение fb/rfb = рисуем не
            // туда, куда читаем; glErr != 0 = состояние уже битое в этой фазе.
            int rfbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int glErr = GL11.glGetError();
            int mainFb = net.minecraft.client.Minecraft.getInstance()
                    .getMainRenderTarget().frameBufferId;
            int cmMask;
            boolean depthMask;
            int dFunc;
            int[] viewport = new int[4];
            float fogStart = RenderSystem.getShaderFogStart();
            float fogEnd = RenderSystem.getShaderFogEnd();
            try (MemoryStack st = MemoryStack.stackPush()) {
                java.nio.ByteBuffer b4 = st.malloc(16);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, b4);
                cmMask = (b4.get(0) != 0 ? 1 : 0) | (b4.get(1) != 0 ? 2 : 0)
                        | (b4.get(2) != 0 ? 4 : 0) | (b4.get(3) != 0 ? 8 : 0);
                depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
                dFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
                java.nio.IntBuffer vpi = st.mallocInt(16);
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, vpi);
                viewport[0] = vpi.get(0);
                viewport[1] = vpi.get(1);
                viewport[2] = vpi.get(2);
                viewport[3] = vpi.get(3);
            }
            boolean blendOn = GL11.glIsEnabled(GL11.GL_BLEND);
            int bfSrc = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            int bfDst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean scissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            var shOpt = safeCurrentShader();
            String rsShader = shOpt == null ? "null"
                    : shOpt.getClass().getSimpleName() + "@"
                        + Integer.toHexString(shOpt.getId())
                        + "/prog" + prog;
            var mv = RenderSystem.getModelViewMatrix();
            int[] unitBindings = new int[3];
            try (MemoryStack st2 = MemoryStack.stackPush()) {
                java.nio.IntBuffer b1 = st2.mallocInt(16);
                for (int u = 0; u < 3; u++) {
                    GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + u);
                    GL11.glGetIntegerv(GL11.GL_TEXTURE_BINDING_2D, b1);
                    unitBindings[u] = b1.get(0);
                }
                // ВАЖНО: именно СЫРОЙ вызов. Кеш GlStateManager считает активным
                // TU0 (все управляемые пути завершаются _activeTexture(TU0));
                // управляемый вызов здесь но-опился бы, оставив физику на TU2,
                // и все последующие бинды ушли бы не в тот юнит (мерцание кадра).
                GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            }
            // ЧТЕНИЕ ПИКСЕЛЕЙ (только для px_тегов): центр и углы текущего
            // draw-буфера — отвечает на вопрос «когда кадр УЖЕ чёрный».
            // Глобальные множители цвета: главный подозреваемый «всё чёрное,
            // включая небо» — shaderColor≈0 (умножает ВЕСЬ мир) и/или чёрный
            // fogColor при близком fogStart.
            String colGlobals;
            try {
                float[] sc = RenderSystem.getShaderColor();
                float[] fc = RenderSystem.getShaderFogColor();
                colGlobals = String.format(" sc=[%.2f,%.2f,%.2f,%.2f] fogc=[%.2f,%.2f,%.2f]",
                        sc[0], sc[1], sc[2], sc[3], fc[0], fc[1], fc[2]);
            } catch (Throwable t) {
                colGlobals = " sc=? fc=?";
            }
            String pix = "";
            if (tag.startsWith("px")) {
                try (MemoryStack st3 = MemoryStack.stackPush()) {
                    java.nio.ByteBuffer b3 = st3.malloc(3);
                    int w = viewport[2], h = viewport[3];
                    StringBuilder sb = new StringBuilder(" px[c=");
                    appendPx(sb, w / 2, h / 2, b3);
                    sb.append(" tl=");
                    appendPx(sb, 8, h - 8, b3);
                    sb.append(" br=");
                    appendPx(sb, w - 8, 8, b3);
                    pix = sb.append("]").toString();
                }
            }
            String cache = cacheVsPhys(unitBindings, bfSrc, bfDst);
            MainRegistry.LOGGER.info(String.format(
                "HBM fsp[%s]: prog=%d fb=%d mainFb=%d rfb=%d glErr=%d cmMask=%d dMask=%d dFunc=%d blend=%d(%d/%d) cull=%d scissor=%d vp=%dx%d+%d+%d tex0=%s tex1=%s units=[%d/%d/%d]%s rsShader=%s fog=%.0f/%.0f mv=%.3f/%.3f/%.3f%s%s",
                tag, prog, fb, mainFb, rfbo, glErr, cmMask, depthMask ? 1 : 0, dFunc,
                blendOn ? 1 : 0, bfSrc, bfDst, cull ? 1 : 0, scissorTest ? 1 : 0,
                viewport[2], viewport[3], viewport[0], viewport[1],
                texSlot(0), texSlot(1),
                unitBindings[0], unitBindings[1], unitBindings[2],
                cache,
                rsShader,
                fogStart, fogEnd,
                mv.m00(), mv.m10(), mv.m22(),
                colGlobals,
                pix));
        } catch (Throwable t) {
            MainRegistry.LOGGER.info(String.format(
                "HBM fsp[%s] failed: %s", tag, t.toString()));
        }
    }

    /** RenderSystem shaderTexture-слоты — примитивные id; печатаем текущее значение. */
    private static String texSlot(int index) {
        try {
            return String.valueOf(RenderSystem.getShaderTexture(index));
        } catch (Throwable t) {
            return "?";
        }
    }

    // ── Кеш GlStateManager vs физика: прямое сравнение ──────────────────────
    // Расхождение cached-биндов/активного юнита/факторов блендинга с GL = но-оп
    // следующих управляемых вызовов = «протухшее» состояние. Резолвим поля
    // рефлексией один раз; в проде имена могут не найтись — просто пусто.
    private static volatile boolean cacheResolved;
    private static java.lang.reflect.Field fActiveTexture, fTextures, fBlend,
            fTexBinding, fBlendSrcRgb, fBlendDstRgb;

    private static String cacheVsPhys(int[] physUnits, int physBlendSrc, int physBlendDst) {
        try {
            if (!cacheResolved) {
                cacheResolved = true;
                Class<?> g = com.mojang.blaze3d.platform.GlStateManager.class;
                fActiveTexture = g.getDeclaredField("activeTexture");
                fTextures = g.getDeclaredField("TEXTURES");
                fBlend = g.getDeclaredField("BLEND");
                fActiveTexture.setAccessible(true);
                fTextures.setAccessible(true);
                fBlend.setAccessible(true);
                Class<?> texState = Class.forName("com.mojang.blaze3d.platform.GlStateManager$TextureState");
                fTexBinding = texState.getDeclaredField("binding");
                fTexBinding.setAccessible(true);
                Class<?> blendState = Class.forName("com.mojang.blaze3d.platform.GlStateManager$BlendState");
                fBlendSrcRgb = blendState.getDeclaredField("srcRgb");
                fBlendDstRgb = blendState.getDeclaredField("dstRgb");
                fBlendSrcRgb.setAccessible(true);
                fBlendDstRgb.setAccessible(true);
            }
            if (fActiveTexture == null || fTextures == null || fBlend == null) return "";
            int cachedActive = fActiveTexture.getInt(null) - GL13.GL_TEXTURE0;
            Object textures = fTextures.get(null);
            Object b0 = java.lang.reflect.Array.get(textures, 0);
            Object b2 = java.lang.reflect.Array.get(textures, 2);
            int c0 = fTexBinding.getInt(b0), c2 = fTexBinding.getInt(b2);
            Object blend = fBlend.get(null);
            int cbs = fBlendSrcRgb.getInt(blend), cbd = fBlendDstRgb.getInt(blend);
            return String.format(" cacheAct=%d t0=%d t2=%d bf=%d/%d",
                    cachedActive, c0, c2, cbs, cbd);
        } catch (Throwable t) {
            return " cache=?";
        }
    }

    private static net.minecraft.client.renderer.ShaderInstance safeCurrentShader() {
        try {
            return RenderSystem.getShader();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Читает один пиксель (RGB) из текущего draw-буфера и дописывает в sb. */
    private static void appendPx(StringBuilder sb, int x, int y, java.nio.ByteBuffer b3) {
        try {
            GL11.glReadPixels(x, y, 1, 1, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, b3);
            sb.append(b3.get(0) & 0xFF).append(',').append(b3.get(1) & 0xFF).append(',').append(b3.get(2) & 0xFF);
        } catch (Throwable t) {
            sb.append('?');
        }
    }

    private static long lastGuiFxLogMs;

    /**
     * ЧТЕНИЕ ПИКСЕЛЯ В ПРОЕКЦИИ МИРОВОЙ ТОЧКИ. Диагностика «чёрного
     * прямоугольника возле ракеты»: бисекция прохода AFTER_WEATHER по шагам —
     * проецируем мировую позицию ракеты (Proj × levelRot × (pos-cam)) в окно и
     * читаем именно этот пиксель до меша / после меша / после частиц / после
     * вспышки. Шаг, на котором пиксель чернеет, = виновник. Работает только
     * внутри нашего пуша (levelRot из TLS) и при валидной проекции.
     */
    public static void snapWorld(String tag, double wx, double wy, double wz) {
        if (!ENABLED || !RenderSystem.isOnRenderThread()) return;
        long now = System.currentTimeMillis();
        Long prev = lastPerTag.get(tag);
        if (prev != null && now - prev < INTERVAL_MS) return;
        lastPerTag.put(tag, now);
        try {
            var rot = com.hbm_m.platform.RenderHooks.currentLevelRotation();
            var proj = RenderSystem.getProjectionMatrix();
            if (rot == null || proj == null) {
                MainRegistry.LOGGER.info("HBM fspWorld[{}]: no rot/proj", tag);
                return;
            }
            var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            org.joml.Vector4f v = new org.joml.Vector4f(
                    (float) (wx - cam.x()), (float) (wy - cam.y()), (float) (wz - cam.z()), 1.0F);
            rot.transform(v);
            proj.transform(v);
            if (Math.abs(v.w()) < 1.0E-5F) {
                MainRegistry.LOGGER.info("HBM fspWorld[{}]: degenerate w", tag);
                return;
            }
            float ndcX = v.x() / v.w();
            float ndcY = v.y() / v.w();
            var win = net.minecraft.client.Minecraft.getInstance().getWindow();
            int sw = win.getWidth(), sh = win.getHeight();
            if (ndcX < -1.05F || ndcX > 1.05F || ndcY < -1.05F || ndcY > 1.05F) {
                MainRegistry.LOGGER.info(String.format(
                        "HBM fspWorld[%s]: offscreen ndc=(%.2f,%.2f)", tag, ndcX, ndcY));
                return;
            }
            int px = Math.max(0, Math.min(sw - 1, (int) ((ndcX * 0.5F + 0.5F) * sw)));
            int py = Math.max(0, Math.min(sh - 1, (int) ((1.0F - (ndcY * 0.5F + 0.5F)) * sh)));
            String pix;
            try (org.lwjgl.system.MemoryStack st = org.lwjgl.system.MemoryStack.stackPush()) {
                java.nio.ByteBuffer b3 = st.malloc(3);
                GL11.glReadPixels(px, py, 1, 1, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, b3);
                pix = (b3.get(0) & 0xFF) + "," + (b3.get(1) & 0xFF) + "," + (b3.get(2) & 0xFF);
            }
            MainRegistry.LOGGER.info(String.format(
                    "HBM fspWorld[%s]: px@( %d , %d ) = %s", tag, px, py, pix));
        } catch (Throwable t) {
            MainRegistry.LOGGER.info("HBM fspWorld[" + tag + "] failed: " + t);
        }
    }

    /**
     * Раз в 2 с печатает активные ВАНИЛЬНЫЕ причины полноэкранных GUI-оверлеев:
     * слепота (fullscreen чёрная текстура), тьма (пульсирующий чёрный),
     * подзорная труба (чёрные fill'ы вокруг круга), тошнота и сон.
     * Диагностика «чёрного прямоугольника, едущего при тряске GUI».
     */
    public static void snapGuiEffects() {
        if (!ENABLED || !RenderSystem.isOnRenderThread()) return;
        long now = System.currentTimeMillis();
        if (now - lastGuiFxLogMs < 2000) return;
        lastGuiFxLogMs = now;
        try {
            var p = net.minecraft.client.Minecraft.getInstance().player;
            if (p == null) return;
            String fx = (p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) ? "B" : "-")
                    + (p.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS) ? "D" : "-")
                    + (p.isScoping() ? "S" : "-")
                    + (p.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION) ? "N" : "-");
            MainRegistry.LOGGER.info(String.format(
                    "HBM gui-fx: B/D/S/N=%s sleepTimer=%d (B=blindness, D=darkness, S=spyglass, N=nausea)",
                    fx, p.getSleepTimer()));
        } catch (Throwable ignored) {
        }
    }
}
