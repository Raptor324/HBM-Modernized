package com.hbm_m.client.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.particle.nt.ParticleEngineNT;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
*///?}
public class EngineHandler {

    private static long farDiagCounter = 0;

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ParticleEngineNT.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        // Матрица поворота камеры кадра (frustumMatrix): на ней строятся ВСЕ
        // camera-relative вершины прохода. Больше НЕ полагаемся на ambient
        // RenderSystem model-view (к флашу батчей он может быть сброшен
        // чужими хуками DH/Iris → «гриб улетает при движении камеры»).
        //? if < 1.21.1 {
        org.joml.Matrix4f levelRotation = new org.joml.Matrix4f(event.getPoseStack().last().pose());
        //?} else {
        /*org.joml.Matrix4f levelRotation = new org.joml.Matrix4f(event.getModelViewMatrix());
        *///?}
        com.hbm_m.platform.RenderHooks.pushLevelModelView(levelRotation);
        try {
            renderAfterWeather(event);
        } finally {
            com.hbm_m.platform.RenderHooks.popLevelModelView();
        }
    }

    private static void renderAfterWeather(RenderLevelStageEvent event) {
        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindWrite(false);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        FogRenderer.setupNoFog();

        //? if < 1.21.1 {
        float partialTick = event.getPartialTick();
        //?} else {
        /*float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?}

        // Порог деления = граница ванильной прорисовки: ближе рисуем с нативным
        // depth-тестом, дальше — тем же ванильным путём, но с удлинённой
        // проекцией и попиксельной окклюзией против DH LOD в шейдере.
        boolean dhRenderedThisFrame = com.hbm_m.client.compat.dh.DhClientState.isActive();
        float splitDist = com.hbm_m.client.compat.dh.DhOcclusionGpu.vanillaFarPlane();
        double splitSq = (double) splitDist * (double) splitDist;
        net.minecraft.world.phys.Vec3 camPos = event.getCamera().getPosition();

        // ДИАГНОСТИКА: печать ambient-проекции ДО наших подмен. ВНИМАНИЕ на
        // индексы JOML (mXY = колонка X, строка Y): у нормальной перспективы
        // m23 == -1 (w-строка), m32 ≈ -2fn/(f-n) ≈ -0.1, m33 == 0.
        Matrix4f ambient = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        if (ambient != null && ++ambientDiagCounter % 600 == 1) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "HBM ambient proj @AFTER_WEATHER: m00={} m11={} m22={} m23={} m32={} m33={} m03={} m13={}",
                    String.format("%.5f", ambient.m00()), String.format("%.5f", ambient.m11()),
                    String.format("%.5f", ambient.m22()), String.format("%.5f", ambient.m23()),
                    String.format("%.5f", ambient.m32()), String.format("%.5f", ambient.m33()),
                    String.format("%.5f", ambient.m03()), String.format("%.5f", ambient.m13()));
        }
        java.util.function.Predicate<com.hbm_m.particle.nt.ParticleNT> near = p -> {
            double dx = p.x - camPos.x, dy = p.y - camPos.y, dz = p.z - camPos.z;
            return dx * dx + dy * dy + dz * dz <= splitSq;
        };
        java.util.function.Predicate<com.hbm_m.particle.nt.ParticleNT> far = near.negate();

        if (++farDiagCounter % 200 == 1) {
            int alive = 0;
            for (java.util.List<com.hbm_m.particle.nt.ParticleNT> l : java.util.List.copyOf(
                    com.hbm_m.particle.nt.ParticleEngineNT.INSTANCE.debugBatches().values())) {
                alive += l.size();
            }
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "HBM AFTER_WEATHER diag: dhActive={}, particlesAlive={}, splitDist={}",
                    dhRenderedThisFrame, alive, (int) splitDist);
        }

        if (dhRenderedThisFrame) {
            // ПОРЯДОК: far -> near -> flash (painter's algorithm).
            // Облачные квады пишут только цвет (COLOR_WRITE, без глубины),
            // поэтому межбатчевое перекрытие решается порядком отрисовки.
            // Разбиение near/far идёт по дистанции от камеры: вдоль любого
            // луча ближний контент ВСЕГДА впереди дальнего, так что
            // «сначала дальний» корректно для любой комбинации взрывов —
            // иначе дальний гриб накладывался бы поверх ближнего.

            // 1. Дальний контент: рисуем ЗДЕСЬ, в главный FBO (композит DH уже
            // отработал — мы поверх, небо не помеха). Проекция удлинённая
            // (клипа за far plane нет), окклюзия против LOD — в шейдере
            // nuke_cloud сэмплированием DH depth-текстуры.
            com.hbm_m.client.compat.dh.DhClientCompat.beginVanillaExtendedPass(partialTick);
            // Чужие батчи флашим ДО биндинга DH depth в Sampler1, иначе они
            // улетели бы в GPU с нашей depth-текстурой вместо их сэмплеров.
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            try {
                // ОККЛЮЗИЯ ПРОТИВ LOD:
                //  - без Iris: копия DH-глубины в главный z-buffer (композит DH
                //    переносит только цвет, глубина там = небо).
                //  - под Iris: DH-глубина живёт в ОТДЕЛЬНОЙ DEPTH32F текстуре
                //    (Iris рендерит LOD'ы через override-программы в свои FB),
                //    а кастомные ShaderInstance маскируются disableDepthColor.
                //    Поэтому применяем ExtendedShader контента (он биндит целевой
                //    FB пайплайна) и копируем DH-глубину туда СЫРОЙ GL-программой
                //    (RawDhDepthCopy) — маскирование на неё не действует.
                boolean irisActive = com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
                if (irisActive) {
                    try {
                        net.minecraft.client.renderer.ShaderInstance contentShader =
                                com.hbm_m.client.render.shader.FarContentShaders.resolveTexColor();
                        if (contentShader != null) {
                            contentShader.apply(); // биндит целевой FB пайплайна
                        }
                        com.hbm_m.client.render.shader.RawDhDepthCopy.copyIntoBoundFramebuffer(
                                com.hbm_m.client.compat.dh.DhClientState.dhNear(),
                                com.hbm_m.client.compat.dh.DhClientState.dhFar(),
                                com.hbm_m.client.compat.dh.DhOcclusionGpu.getDhActiveDepthTextureId());
                    } catch (Throwable t) {
                        com.hbm_m.main.MainRegistry.LOGGER.info("HBM iris depth copy failed: {}", t.toString());
                    }
                } else {
                    com.hbm_m.client.compat.dh.DhDepthCopy.copyToMain(
                            com.hbm_m.client.compat.dh.DhClientState.dhNear(),
                            com.hbm_m.client.compat.dh.DhClientState.dhFar());
                }

                // Дальние меши ракет: расширенная проекция (нет клипа far plane),
                // пишут глубину — полупрозрачный контент позади них отсекается.
                // ВАЖНО: рисуем ДО биндинга DH depth в Sampler1 — стандартные
                // entity-шейдеры используют слот 1 как overlay-маску, и получили
                // бы глубину вместо неё (тинт на корпусе ракеты).
                MissileTrackWorldRender.renderFiltered(partialTick, d -> d > splitSq);

                setDhShaderFarMode(1.0F,
                        com.hbm_m.client.compat.dh.DhClientState.dhProjection());
                logNtDrawState("far");
                ParticleEngineNT.INSTANCE.renderFiltered(buffer, event.getCamera(), partialTick, event.getPoseStack(), far);
                farReadbackProbe(buffer, event.getCamera().getPosition());
                buffer.endBatch();
                ParticleEngineNT.buffer().endBatch();
            } finally {
                // Сброс Sampler1 сразу после флаша: RenderSystem.shaderTextures —
                // глобальное состояние, следующее чужое дро не должно получить
                // DH depth-текстуру как сэмплер.
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(1, 0);
                setDhShaderFarMode(0.0F, null);
                com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();
            }

            // 2. Ближний контент поверх дальнего: сначала меши ракет (пишут
            // глубину), затем NT-частицы. САДИМСЯ НА ЗАХВАЧЕННУЮ матрицу кадра:
            // ambient к этому моменту может быть загрязнён поздними фазами
            // (см. ambient-proj диагностику) — а захваченная матрица в точности
            // та, которой террейн писал свой z-buffer (глубина корректна).
            com.hbm_m.client.compat.dh.DhClientCompat.beginCapturedVanillaPass(partialTick);
            setDhShaderFarMode(0.0F, null);
            MissileTrackWorldRender.renderFiltered(partialTick, d -> d <= splitSq);
            logNtDrawState("near");
            ParticleEngineNT.INSTANCE.renderFiltered(buffer, event.getCamera(), partialTick, event.getPoseStack(), near);
            flushWithPixelProbe(buffer, camPos, "near");
            com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();

            // 3. Вспышка — оверлей поверх всего.
            ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
        } else {
            // DH не рендерит: полный проход, тоже на захваченной матрице.
            com.hbm_m.client.compat.dh.DhClientCompat.beginCapturedVanillaPass(partialTick);
            setDhShaderFarMode(0.0F, null);
            MissileTrackWorldRender.renderFiltered(partialTick, null);
            logNtDrawState("nodh");
            ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), partialTick, event.getPoseStack());
            flushWithPixelProbe(buffer, camPos, "nodh");
            ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();
        }

        // Восстанавливаем GL state как было до нас (weather/worldborder рассчитывают на false).
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
    }

    private static long farModeDiagCounter = 0;
    private static long farReadbackCounter = 0;
    private static long ambientDiagCounter = 0;
    private static long drawStateDiagCounter = 0;
    private static long nearReadbackCounter = 0;

    /**
     * Диагностика «чёрного фона» частиц (мигает внутри сессии): состояние GL и
     * выбранного шейдера на момент флаша NT-батчей. Лог раз в ~120 вызовов.
     * Ожидания: shader = nuke_cloud ShaderInstance, Color loc=2, UV0 loc=1,
     * GL_BLEND=true, colorMask=(true,true,true,true), fogStart=MAX (no fog),
     * shaderColor=(1,1,1,1).
     */
    private static void logNtDrawState(String tag) {
        if (++drawStateDiagCounter % 120 != 1) return;
        try {
            net.minecraft.client.renderer.ShaderInstance sh =
                    com.hbm_m.client.render.shader.FarContentShaders.resolveTexColor();
            int prog = sh != null ? sh.getId() : 0;
            int locColor = prog != 0 ? org.lwjgl.opengl.GL20.glGetAttribLocation(prog, "Color") : -99;
            int locUv = prog != 0 ? org.lwjgl.opengl.GL20.glGetAttribLocation(prog, "UV0") : -99;
            boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
            int srcRgb = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
            int dstRgb = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
            java.nio.ByteBuffer cm = java.nio.ByteBuffer.allocateDirect(4);
            org.lwjgl.opengl.GL11.glGetBooleanv(org.lwjgl.opengl.GL11.GL_COLOR_WRITEMASK, cm);
            float fogStart = com.mojang.blaze3d.systems.RenderSystem.getShaderFogStart();
            float fogEnd = com.mojang.blaze3d.systems.RenderSystem.getShaderFogEnd();
            float[] fogCol = com.mojang.blaze3d.systems.RenderSystem.getShaderFogColor();
            float[] shCol = com.mojang.blaze3d.systems.RenderSystem.getShaderColor();
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "HBM NT draw state [{}]: shader={} prog={} Color@{} UV0@{} blend={} func=({},{}) colorMask={} fog=[{},{}] fogColor=({}, {}, {}) shaderColor=({},{},{},{})",
                    tag,
                    sh == null ? "NULL" : sh.getClass().getSimpleName(),
                    prog, locColor, locUv, blend, srcRgb, dstRgb,
                    new boolean[]{cm.get(0) != 0, cm.get(1) != 0, cm.get(2) != 0, cm.get(3) != 0},
                    fogStart == Float.MAX_VALUE ? "MAX" : String.format("%.1f", fogStart),
                    fogEnd == Float.MAX_VALUE ? "MAX" : String.format("%.1f", fogEnd),
                    String.format("%.2f", fogCol[0]), String.format("%.2f", fogCol[1]), String.format("%.2f", fogCol[2]),
                    String.format("%.2f", shCol[0]), String.format("%.2f", shCol[1]),
                    String.format("%.2f", shCol[2]), String.format("%.2f", shCol[3]));
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.info("HBM NT draw state probe failed: {}", t.toString());
        }
    }

    /**
     * Диагностика дальнего прохода: проецируем первый дальний партикл ТЕМИ ЖЕ
     * матрицами RenderSystem, что пойдут в шейдер, и читаем пиксель главного
     * FBO до и после флаша. Отвечает: на экране ли NDC, растеризуется ли квад.
     */
    private static void farReadbackProbe(MultiBufferSource.BufferSource buffer, net.minecraft.world.phys.Vec3 camPos) {
        try {
            var engine = ParticleEngineNT.INSTANCE;
            com.hbm_m.particle.nt.ParticleNT probeP = null;
            outer:
            for (java.util.List<com.hbm_m.particle.nt.ParticleNT> list : engine.debugBatches().values()) {
                for (com.hbm_m.particle.nt.ParticleNT p : list) {
                    double dx = p.x - camPos.x, dy = p.y - camPos.y, dz = p.z - camPos.z;
                    double splitDist = com.hbm_m.client.compat.dh.DhOcclusionGpu.vanillaFarPlane();
                    if (dx * dx + dy * dy + dz * dz > splitDist * splitDist) { probeP = p; break outer; }
                }
            }
            if (probeP == null) return;
            if (farReadbackCounter++ % 120 != 0) return;

            org.joml.Matrix4f proj = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
            org.joml.Matrix4f mvm = com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();
            org.joml.Vector4f v = new org.joml.Vector4f(
                    (float) (probeP.x - camPos.x), (float) (probeP.y - camPos.y),
                    (float) (probeP.z - camPos.z), 1.0F);
            mvm.transform(v);
            float eyeX = v.x(), eyeY = v.y(), eyeZ = v.z();
            if (proj != null) proj.transform(v);
            float ndcX = v.x() / v.w(), ndcY = v.y() / v.w(), ndcZ = v.z() / v.w();
            var win = Minecraft.getInstance().getWindow();
            int px = (int) ((ndcX * 0.5F + 0.5F) * win.getWidth());
            int py = (int) ((ndcY * 0.5F + 0.5F) * win.getHeight());
            String projDesc = proj == null ? "NULL"
                    : String.format("m22=%.4f m23=%.4f", proj.m22(), proj.m23());

            var mc = Minecraft.getInstance();
            int mainFbo = mc.getMainRenderTarget().frameBufferId;
            int prevRead = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING);
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, mainFbo);
            java.nio.ByteBuffer pre = org.lwjgl.BufferUtils.createByteBuffer(20);
            org.lwjgl.opengl.GL11.glReadPixels(px, py, 1, 1,
                    org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT, org.lwjgl.opengl.GL11.GL_FLOAT, pre);
            java.nio.ByteBuffer rgba = org.lwjgl.BufferUtils.createByteBuffer(16);
            org.lwjgl.opengl.GL11.glReadPixels(px, py, 1, 1,
                    org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_FLOAT, rgba);

            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();

            java.nio.ByteBuffer post = org.lwjgl.BufferUtils.createByteBuffer(20);
            org.lwjgl.opengl.GL11.glReadPixels(px, py, 1, 1,
                    org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT, org.lwjgl.opengl.GL11.GL_FLOAT, post);
            java.nio.ByteBuffer rgba2 = org.lwjgl.BufferUtils.createByteBuffer(16);
            org.lwjgl.opengl.GL11.glReadPixels(px, py, 1, 1,
                    org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_FLOAT, rgba2);
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, prevRead);

            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "HBM far draw probe: proj[{}] mvm[m30={},m31={},m32={}] eye=({},{},{}) dist={} ndc=({},{},{}) pre(d={},rgb={},{},{} a={}) -> post(d={},rgb={},{},{} a={}) onScreen={}",
                    projDesc,
                    String.format("%.3f", mvm.m30()), String.format("%.3f", mvm.m31()), String.format("%.3f", mvm.m32()),
                    String.format("%.2f", eyeX), String.format("%.2f", eyeY), String.format("%.4f", eyeZ),
                    String.format("%.1f", Math.sqrt(eyeX * eyeX + eyeY * eyeY + eyeZ * eyeZ)),
                    String.format("%.4f", ndcX), String.format("%.4f", ndcY), String.format("%.5f", ndcZ),
                    String.format("%.6f", pre.getFloat(0)),
                    (int) (rgba.getFloat(0) * 255), (int) (rgba.getFloat(4) * 255),
                    (int) (rgba.getFloat(8) * 255), (int) (rgba.getFloat(12) * 255),
                    String.format("%.6f", post.getFloat(0)),
                    (int) (rgba2.getFloat(0) * 255), (int) (rgba2.getFloat(4) * 255),
                    (int) (rgba2.getFloat(8) * 255), (int) (rgba2.getFloat(12) * 255),
                    Math.abs(ndcX) <= 1 && Math.abs(ndcY) <= 1 && ndcZ >= -1 && ndcZ <= 1);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.info("HBM far draw probe failed: {}", t.toString());
        }
    }

    /**
     * Пиксельная истина для ближнего/бесшейдерного прохода: проецируем первый
     * партикл текущими матрицами, читаем пиксель главного FBO до/после флаша.
     */
    private static void flushWithPixelProbe(MultiBufferSource.BufferSource buffer, net.minecraft.world.phys.Vec3 camPos, String tag) {
        com.hbm_m.particle.nt.ParticleNT probeP = null;
        if (++nearReadbackCounter % 120 == 1) {
            outer:
            for (java.util.List<com.hbm_m.particle.nt.ParticleNT> list : ParticleEngineNT.INSTANCE.debugBatches().values()) {
                for (com.hbm_m.particle.nt.ParticleNT p : list) { probeP = p; break outer; }
            }
        }
        if (probeP == null) {
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            return;
        }
        try {
            org.joml.Matrix4f proj = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
            org.joml.Matrix4f mvm = com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();
            org.joml.Vector4f v = new org.joml.Vector4f(
                    (float) (probeP.x - camPos.x), (float) (probeP.y - camPos.y),
                    (float) (probeP.z - camPos.z), 1.0F);
            mvm.transform(v);
            float eyeZ = v.z();
            if (proj != null) proj.transform(v);
            float ndcX = v.x() / v.w(), ndcY = v.y() / v.w(), ndcZ = v.z() / v.w();
            var win = Minecraft.getInstance().getWindow();
            int px = (int) ((ndcX * 0.5F + 0.5F) * win.getWidth());
            int py = (int) ((ndcY * 0.5F + 0.5F) * win.getHeight());
            var mc = Minecraft.getInstance();
            int mainFbo = mc.getMainRenderTarget().frameBufferId;
            int prevRead = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING);
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, mainFbo);
            java.nio.ByteBuffer rgba = org.lwjgl.BufferUtils.createByteBuffer(16);
            org.lwjgl.opengl.GL11.glReadPixels(px, py, 1, 1,
                    org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_FLOAT, rgba);

            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();

            java.nio.ByteBuffer rgba2 = org.lwjgl.BufferUtils.createByteBuffer(16);
            org.lwjgl.opengl.GL11.glReadPixels(px, py, 1, 1,
                    org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_FLOAT, rgba2);
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER, prevRead);

            boolean onScreen = Math.abs(ndcX) <= 1 && Math.abs(ndcY) <= 1 && ndcZ >= -1 && ndcZ <= 1;
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "HBM {} pixel probe: eyeZ={} ndc=({},{},{}) onScreen={} rgb=({},{},{}) a={} -> ({},{},{}) a={}",
                    tag,
                    String.format("%.2f", eyeZ),
                    String.format("%.3f", ndcX), String.format("%.3f", ndcY), String.format("%.5f", ndcZ), onScreen,
                    (int) (rgba.getFloat(0) * 255), (int) (rgba.getFloat(4) * 255),
                    (int) (rgba.getFloat(8) * 255), (int) (rgba.getFloat(12) * 255),
                    (int) (rgba2.getFloat(0) * 255), (int) (rgba2.getFloat(4) * 255),
                    (int) (rgba2.getFloat(8) * 255), (int) (rgba2.getFloat(12) * 255));
        } catch (Throwable t) {
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            com.hbm_m.main.MainRegistry.LOGGER.info("HBM {} pixel probe failed: {}", tag, t.toString());
        }
    }

    /**
     * Настраивает шейдер nuke_cloud.
     * mode 0 — обычный ванильный depth-тест (ближний проход/вспышка);
     * mode 1 — попиксельная окклюзия против DH LOD (Sampler1 = DEPTH32F);
     * mode 2 — диагностика: цвет = (vDhWinZ, DH-глубина, 0), без discard.
     * Под Iris-паком не вызывается по смыслу: там контент рисуется через
     * ExtendedShader пака (FarContentShaders), окклюзия — нативным depth-тестом
     * против единого z-buffer'а (LOD'ы рендерятся DH прямо в него).
     */
    private static void setDhShaderFarMode(float mode, org.joml.Matrix4f dhProj) {
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()) {
            return;
        }
        net.minecraft.client.renderer.ShaderInstance sh = com.hbm_m.client.render.ModShaders.getNukeCloudShader();
        if (sh == null) return;
        boolean farMode = mode > 0.5F;
        try {
            sh.safeGetUniform("DhDepthTest").set(mode);
            if (!farMode) return;
            var mc = Minecraft.getInstance();
            var win = mc.getWindow();
            sh.safeGetUniform("DhViewport").set((float) win.getWidth(), (float) win.getHeight());
            if (dhProj != null) {
                sh.safeGetUniform("DhProjMat").set(new org.joml.Matrix4f(dhProj));
            }
            int texId = com.hbm_m.client.compat.dh.DhOcclusionGpu.getDhActiveDepthTextureId();
            if (texId > 0) {
                // КРИТИЧНО: НЕ sh.setSampler("Sampler1", ...). VertexBuffer._drawWithShader
                // перед КАЖДЫМ дро вызывает shader.setDefaultUniforms(), а тот делает
                // setSampler("Sampler" + i, RenderSystem.getShaderTexture(i)) для i=0..11 —
                // значение, записанное через sh.setSampler, затиралось нулём до apply(),
                // шейдер читал пустой юнит: lodDepth == 0 и «if (vDhWinZ > lodDepth) discard»
                // убивал ВЕСЬ гриб в дальнем проходе (исчезал при отгрузке ванильных чанков).
                // До apply() доживает только канал RenderSystem shaderTextures.
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(1, texId);
            }
            if (++farModeDiagCounter % 600 == 1) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "HBM far shader mode: mode={}, dhProj={}, depthTex={}, viewport={}x{}",
                        mode, dhProj != null ? "ok" : "NULL", texId,
                        win.getWidth(), win.getHeight());
            }
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.info("HBM DH shader far mode setup failed: {}", t.toString());
        }
    }

    //? if forge {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused()) {
            // Ленивая привязка DH-моста (DI-реестр DH может очиститься при его
            // инициализации). Класс DhRenderBridge наследует DH-класс — грузить
            // его можно только при наличии DH (guard строго до упоминания).
            if (com.hbm_m.compat.dh.DhCompat.isModPresent()) {
                com.hbm_m.client.compat.dh.DhRenderBridge.tryRegister();
            }
            ParticleEngineNT.INSTANCE.tick();
        }
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (!Minecraft.getInstance().isPaused()) {
            // Ленивая привязка DH-моста (см. forge-ветку).
            if (com.hbm_m.compat.dh.DhCompat.isModPresent()) {
                com.hbm_m.client.compat.dh.DhRenderBridge.tryRegister();
            }
            ParticleEngineNT.INSTANCE.tick();
        }
    }
    *///?}
}