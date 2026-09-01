package com.hbm_m.client.particle;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.particle.nt.ParticleEngineNT;

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
            // NOTE: invalidateBlendModeCache здесь НЕ вызываем — сброс на
            // границе кадров заставлял первые ванильные apply() кадра делать
            // полную установку с disableBlend, ломая небо/spyglass. Достаточно
            // прицельного сброса в конце AFTER_WEATHER-прохода.
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
        // Точная проекция кадра (та же, которой пак/ваниль рисовали террейн):
        // база для cleanExtendedProjection — иначе под Iris частицы строили
        // приближённую перспективу и «отлетали» при изменении FOV (ускорение).
        com.hbm_m.client.compat.dh.DhClientCompat.offerFrameProjection(
                new org.joml.Matrix4f(event.getProjectionMatrix()));
        try {
            renderAfterWeather(event);
        } finally {
            com.hbm_m.platform.RenderHooks.popLevelModelView();
        }
    }

    private static void renderAfterWeather(RenderLevelStageEvent event) {
        // РАДИКАЛЬНАЯ СТРАХОВКА: если рисовать нечего (DH не рендерит, NT-частиц
        // нет, треков ракет нет) — НЕ ТРОГАЕМ НИЧЕГО в кадре вообще. Любая наша
        // манипуляция состоянием/FBO в этом окне — чистый риск для ванильного
        // рендера; на пустом проходе она и была источником артефактов.
        boolean hasParticles = !com.hbm_m.particle.nt.ParticleEngineNT.INSTANCE.debugBatches().isEmpty();
        boolean hasTracks = com.hbm_m.client.missile.track.MissileTrackClient.isEnabled()
                && com.hbm_m.client.missile.track.MissileTrackClient.entries().iterator().hasNext();
        if (!com.hbm_m.client.compat.dh.DhClientState.isActive() && !hasParticles && !hasTracks) {
            return;
        }

        // Честный блендинг перед нашими кастомными отрисовками: страхуемся от
        // залипших факторов кеша (см. ShaderBindResync.forceHonestBlendState).
        com.hbm_m.client.render.shader.ShaderBindResync.forceHonestBlendState();

        // Ресинк кеша программ кастомных шейдеров против сырого _glUseProgram(0)
        // от Oculus-VanillaRenderingPipeline (см. ShaderBindResync).
        // clear() безопасен: каждый дро перенастроит сэмплеры и юниформы заново.
        forceResyncProgram(com.hbm_m.client.render.shader.ModShaders.getNukeCloudShader());
        forceResyncProgram(com.hbm_m.client.render.shader.ModShaders.getNukeAddShader());
        com.hbm_m.client.render.shader.ShaderBindResync.invalidateStaticProgramCache();
        com.hbm_m.client.render.shader.ShaderBindResync.enforceGlProgramConsistency();

        // ЦЕЛЕВОЙ FBO: всегда главный. На Fabulous рендертайпы NT-частиц несут
        // шардинг TRANSLUCENT_TARGET: каждый батч прыгает в translucentTarget и
        // teardown'ом возвращается в MAIN — если мы остались в weatherTarget,
        // контент расщепляется между путями композита (мерцание/призраки).
        // Единый main даёт согласованный кадр; transparencyChain в конце кадра
        // поверх него кладёт только полупрозрачные слои.
        com.mojang.blaze3d.pipeline.RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindWrite(false);
        // Честное восстановление: на Fast/Fancy ваниль ставит depthMask(false)
        // ПЕРЕД погодой, на Fabulous шард WEATHER_TARGET оставляет true.
        // Чтение сырое — glGet не рассинхронизирует кеш GlStateManager.
        boolean prevDepthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        // ТУМАН НЕ ТРОГАЕМ. Раньше здесь глушали туман (start=100000): наш
        // контент рисовался ярко и без тумана, пока мир вокруг (например,
        // кратерный туман CraterFogHandler 0.5/180) был в плотном тумане —
        // отсюда «затемнение меша при переходе entity<->track» и мигание.
        // Единый туман сцены = бесшовный переход между путями рендера.

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

        // ДИАГНОСТИКА УДАЛЕНА (ambient proj / particlesAlive): спамили каждый
        // кадр; при необходимости вернуть — git-история 2026-08-28.

        // Фильтры near/far — примитивными параметрами в renderFiltered:
        // лямбды-предикаты с захватом camPos/splitSq аллоцировали 4 объекта
        // на кадр — чистый GC churn в горячем пути.

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
                MissileTrackWorldRender.renderFiltered(partialTick, splitSq, true);

                setDhShaderFarMode(1.0F,
                        com.hbm_m.client.compat.dh.DhClientState.dhProjection());
                ParticleEngineNT.INSTANCE.renderFiltered(ParticleEngineNT.buffer(), event.getCamera(), partialTick, event.getPoseStack(), splitSq, true);
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
            MissileTrackWorldRender.renderFiltered(partialTick, splitSq, false);
            ParticleEngineNT.INSTANCE.renderFiltered(ParticleEngineNT.buffer(), event.getCamera(), partialTick, event.getPoseStack(), splitSq, false);
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();

            // 3. Вспышка — оверлей поверх всего.
            ParticleEngineNT.INSTANCE.renderFlashOnly(ParticleEngineNT.buffer(), event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
        } else {
            // DH не рендерит: полный проход, тоже на захваченной матрице.
            // ЕДИНСТВЕННЫЙ источник отрисовки мешей ракет (дубль в
            // ClientModEvents на AFTER_ENTITIES удалён): painter-порядок
            // «меши пишут глубину → NT-частицы» внутри одного прохода.
            com.hbm_m.client.compat.dh.DhClientCompat.beginCapturedVanillaPass(partialTick);
            setDhShaderFarMode(0.0F, null);
            MissileTrackWorldRender.renderFiltered(partialTick, Double.NaN, false);
            ParticleEngineNT.INSTANCE.render(ParticleEngineNT.buffer(), event.getCamera(), partialTick, event.getPoseStack());
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
            ParticleEngineNT.INSTANCE.renderFlashOnly(ParticleEngineNT.buffer(), event.getCamera(), partialTick, event.getPoseStack());
            buffer.endBatch();
            ParticleEngineNT.buffer().endBatch();
            com.hbm_m.client.compat.dh.DhClientCompat.endVanillaExtendedPass();
        }

        // Восстанавливаем GL state как было до нас: depthMask — значение,
        // с которым кадр вошёл в проход (Fast/Fancy: false; Fabulous: true).
        // Туман не трогали — восстанавливать нечего.
        com.mojang.blaze3d.systems.RenderSystem.depthMask(prevDepthMask);
        // Наш nuke_cloud/nuke_add оставили BlendMode.lastApplied = non-opaque
        // (аддитив/альфа из JSON) — следующий ванильный opaque-шейдер молча
        // выключит блендинг (чёрная виньетка/солнце/GUI). Сбрасываем кеш.
        com.hbm_m.client.render.shader.ShaderBindResync.invalidateBlendModeCache();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
        com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        // ГЛАВНЫЙ фикс-кандидат «чёрного экрана»: если Oculus-миксин пометил
        // кадр маскировкой depth/color на apply() наших кастомных шейдеров,
        // презент кадра уйдёт через пустой композит Iris при полностью
        // корректном главном буфере. Сбрасываем маску до конца кадра.
        com.hbm_m.client.render.shader.ShaderBindResync.forceIrisDepthColorEnabled();
    }

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
        net.minecraft.client.renderer.ShaderInstance sh = com.hbm_m.client.render.shader.ModShaders.getNukeCloudShader();
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