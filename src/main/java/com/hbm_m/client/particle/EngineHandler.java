package com.hbm_m.client.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.particle.nt.ParticleEngineNT;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.systems.RenderSystem;

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

    @SubscribeEvent
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ParticleEngineNT.INSTANCE.clear();
    }

    //? if forge {
    @SubscribeEvent
    public static void onRenderTickEnd(net.minecraftforge.event.TickEvent.RenderTickEvent event) {
        // КОНЕЦ кадра: инвалидируем статический lastProgramId, чтобы первый
        // apply() следующего кадра (небо!) сделал честный glUseProgram независимо
        // от клоббера Oculus _glUseProgram(0) в beginLevelRendering.
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            com.hbm_m.client.render.shader.ShaderBindResync.invalidateStaticProgramCache();
        }
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRenderTickEnd(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        com.hbm_m.client.render.shader.ShaderBindResync.invalidateStaticProgramCache();
    }
    *///?}

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
        long now = System.currentTimeMillis();
        diagThisFrame = (now - lastDiagLogMs >= DIAG_INTERVAL_MS);
        if (diagThisFrame) lastDiagLogMs = now;

        // Ресинк кеша программ кастомных шейдеров против сырого _glUseProgram(0)
        // от Oculus-VanillaRenderingPipeline (см. ShaderBindResync).
        // clear() безопасен: каждый дро перенастроит сэмплеры и юниформы заново.
        forceResyncProgram(com.hbm_m.client.render.ModShaders.getNukeCloudShader());
        forceResyncProgram(com.hbm_m.client.render.ModShaders.getNukeAddShader());
        com.hbm_m.client.render.shader.ShaderBindResync.invalidateStaticProgramCache();
        com.hbm_m.client.render.shader.ShaderBindResync.enforceGlProgramConsistency();

        com.hbm_m.client.render.FrameStateProbe.snap("eh.in");
        com.hbm_m.client.render.FrameStateProbe.snap("px.eh_in");

        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindWrite(false);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.setShaderFogStart(100000.0F);
        RenderSystem.setShaderFogEnd(100001.0F);
        RenderSystem.setShaderFogColor(0.0F, 0.0F, 0.0F, 0.0F);

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
        if (ambient != null && diagThisFrame) {
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

            if (diagThisFrame) {
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
                if (!SKIP_MESH) MissileTrackWorldRender.renderFiltered(partialTick, d -> d > splitSq);

                setDhShaderFarMode(1.0F,
                        com.hbm_m.client.compat.dh.DhClientState.dhProjection());
                if (!SKIP_PARTICLES) ParticleEngineNT.INSTANCE.renderFiltered(buffer, event.getCamera(), partialTick, event.getPoseStack(), far);
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
            if (!SKIP_MESH) MissileTrackWorldRender.renderFiltered(partialTick, d -> d <= splitSq);
            if (!SKIP_PARTICLES) ParticleEngineNT.INSTANCE.renderFiltered(buffer, event.getCamera(), partialTick, event.getPoseStack(), near);
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();

            // 3. Вспышка — оверлей поверх всего.
            if (!SKIP_FLASH) ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
        } else {
            // DH не рендерит: полный проход, тоже на захваченной матрице.
            // ЕДИНСТВЕННЫЙ источник отрисовки мешей ракет (дубль в
            // ClientModEvents на AFTER_ENTITIES удалён): painter-порядок
            // «меши пишут глубину → NT-частицы» внутри одного прохода.
            com.hbm_m.client.compat.dh.DhClientCompat.beginCapturedVanillaPass(partialTick);
            setDhShaderFarMode(0.0F, null);
            double[] mx = MissileTrackWorldRender.lastDrawnMissilePos();
            if (mx != null) {
                com.hbm_m.client.render.FrameStateProbe.snapWorld("mx.pre", mx[0], mx[1], mx[2]);
            }
            if (!SKIP_MESH) {
                MissileTrackWorldRender.renderFiltered(partialTick, null);
                // String br = com.hbm_m.client.render.SingleMeshVboRenderer.lastTrackMeshBranch.get();
                // Суффикс ветки в теге даёт независимые рейтлимиты и мгновенную
                // читаемость: s1.mesh.vbo / s1.mesh.quads:shader-null / s1.mesh.
                com.hbm_m.client.render.FrameStateProbe.snap("s1.mesh." + (br == null ? "-" : br));
                com.hbm_m.client.render.FrameStateProbe.snap("px.s1mesh");
                if (mx != null) {
                    com.hbm_m.client.render.FrameStateProbe.snapWorld("mx.mesh", mx[0], mx[1], mx[2]);
                }
            }
            if (!SKIP_PARTICLES) ParticleEngineNT.INSTANCE.render(buffer, event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            // ФИКС «чёрного экрана» при совместном рендере меш+частицы:
            // диаг fsp показала, что после флаша NT-батчей физические бинды
            // TU1/TU2 уезжают в ноль ([0/0/0]), а кеш GlStateManager продолжает
            // считать свет/оверлей живыми — следующий управляемый бинд но-опится,
            // террейн следующих кадров сэмплирует пустоту (чёрный экран).
            // Восстанавливаем управляемо лайтмап и оверлей, возвращаем active TU0.
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            com.mojang.blaze3d.systems.RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            com.hbm_m.client.render.FrameStateProbe.snap("s2.part");
            com.hbm_m.client.render.FrameStateProbe.snap("px.s2part");
            if (mx != null) {
                com.hbm_m.client.render.FrameStateProbe.snapWorld("mx.part", mx[0], mx[1], mx[2]);
            }
            if (!SKIP_FLASH) ParticleEngineNT.INSTANCE.renderFlashOnly(buffer, event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
            if (mx != null) {
                com.hbm_m.client.render.FrameStateProbe.snapWorld("mx.flash", mx[0], mx[1], mx[2]);
            }
            com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();
        }

        // Восстанавливаем GL state как было до нас (weather/worldborder рассчитывают на false).
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        // Страховка и для DH-ветки: лайтмап/оверлей обязаны остаться физически
        // забинденными после нашего прохода (см. фикс в nodh-ветке выше).
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
        com.mojang.blaze3d.systems.RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        // ГЛАВНЫЙ фикс-кандидат «чёрного экрана»: если Oculus-миксин пометил
        // кадр маскировкой depth/color на apply() наших кастомных шейдеров,
        // презент кадра уйдёт через пустой композит Iris при полностью
        // корректном главном буфере. Сбрасываем маску до конца кадра.
        com.hbm_m.client.render.shader.ShaderBindResync.forceIrisDepthColorEnabled();
        com.hbm_m.client.render.FrameStateProbe.snap("eh.out");
        com.hbm_m.client.render.FrameStateProbe.snap("px.ehout");
    }

    private static long lastDiagLogMs = 0;
    private static final long DIAG_INTERVAL_MS = 2000;
    private static boolean diagThisFrame;

    // ── ВРЕМЕННЫЕ бисекторы «чёрного экрана» (2026-08) ──────────────────────
    // Выключают отдельные составляющие AFTER_WEATHER-прохода, чтобы одним
    // запуском локализовать виновника отравы кадра под Oculus:
    //   HBM_SKIP_MESH=1      — меши ракет (MissileTrackWorldRender)
    //   HBM_SKIP_PARTICLES=1 — NT-частицы (шлейф/гриб/пепел)
    //   HBM_SKIP_FLASH=1     — вспышки (renderFlashOnly)
    // Ищется и как system property (-Dhbm.skip.mesh), и как env-переменная.
    private static final boolean SKIP_MESH = false;
    private static final boolean SKIP_PARTICLES = false;
    private static final boolean SKIP_FLASH = false;

    /**
     * Если GL_CURRENT_PROGRAM разошёлся с идентификатором шейдера — обнулить
     * статический lastProgramId через clear(), чтобы следующий apply() выполнил
     * честный glUseProgram. См. ShaderBindResync.
     */
    private static void forceResyncProgram(net.minecraft.client.renderer.ShaderInstance shader) {
        if (shader == null) return;
        try {
            com.hbm_m.client.render.shader.ShaderBindResync.ensureFreshBind(shader);
        } catch (Throwable ignored) {
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
        if (diagThisFrame) {
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