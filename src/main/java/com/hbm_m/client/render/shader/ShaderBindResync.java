package com.hbm_m.client.render.shader;

import net.minecraft.client.renderer.ShaderInstance;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/**
 * Принудительная ресинхронизация статического кеша программы кастомного
 * {@link ShaderInstance} против реального GL-состояния.
 *
 * <p>Oculus при выключенном шейдерпаке раз за кадр делает сырой
 * {@code GlStateManager._glUseProgram(0)} (VanillaRenderingPipeline.beginLevelRendering),
 * не обновляя приватный статический {@code ShaderInstance.lastProgramId}.
 * Если прошлый кадр закончился нашим шейдером, следующий {@code apply()}
 * видит совпадение programId == lastProgramId и ПРОПУСКАЕТ реальный бинд —
 * все glUniform/glDrawElements уходят в программу 0 («No active program»,
 * чёрная геометрия с мусорной матрицей), причём состояние залипает на все
 * последующие кадры.</p>
 *
 * <p>Лечение без миксинов: перед отрисовкой сравниваем GL_CURRENT_PROGRAM
 * с идентификатором шейдера; при расхождении {@code clear()} обнуляет
 * lastProgramId (-1) и форсирует честный glUseProgram в apply(). В норме
 * это один glGetInteger на проход.</p>
 *
 * <p>Точки вызова: {@code com.hbm_m.client.render.SingleMeshVboRenderer}
 * (меши ракет, block_lit), копия DH-глубины ({@code DhDepthCopy}) и входы
 * проходов AFTER_WEATHER в {@code EngineHandler} (облака/вспышка Torex).</p>
 */
public final class ShaderBindResync {

    private ShaderBindResync() {}

    /** Убедиться, что следующий {@code apply()} шейдера выполнит честный бинд. */
    public static void ensureFreshBind(ShaderInstance shader) {
        if (shader == null) {
            return;
        }
        try {
            if (GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != shader.getId()) {
                shader.clear();
            }
        } catch (Throwable ignored) {
            // GL-контекст недоступен/чужой патч — худший случай равен старому поведению
        }
    }

    /**
     * Ватчдог рассинхрона уровня «весь мир»: если RenderSystem считает текущим
     * какой-то {@link ShaderInstance}, а фактическая GL-программа — 0
     *(сырой сброс от Oculus без follow-up бинда), следующий Vanilla
     * {@code apply()} пропустит честный бинд и МИР ПЕРЕСТАНЁТ ПЕРЕРИСОВЫВАТЬСЯ
     * (экран консервируется до появления любого чужого честного apply).
     * Диагностика fsp это показала напрямую: prog=0 при живом RS-шейдере
     * держится на всех границах фаз, пока не появится другой SingleMeshVboRenderer.
     *
     * Вызывается на КАЖДОМ нашем RenderLevelStageEvent (см. ClientModEvents /
     * EngineHandler) — задолго до следующего vanilla apply, поэтому после него
     * любой {@code apply()} выполнит настоящий glUseProgram и кадр продолжит
     * перерисовываться даже без наших мешей на экране.
     */
    public static void enforceGlProgramConsistency() {
        try {
            ShaderInstance current = com.mojang.blaze3d.systems.RenderSystem.getShader();
            if (current != null && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == 0) {
                current.clear();
            }
        } catch (Throwable ignored) {
        }
    }

    // ── Iris DepthColorStorage watchdog ─────────────────────────────────────
    private static volatile boolean irisChecked;
    private static java.lang.reflect.Field irisDepthColorField;

    /**
     * Читает статический флаг {@code net.irisshaders.iris.mixin.DepthColorStorage}
     *(null, если Iris/Oculus отсутствует). Миксин onTail ставит его при КАЖДОМ
     * apply неизвестного модового ShaderInstance; если флаг остался включённым к
     * концу кадра, Oculus-VanillaPipeline уводит ПРЕЗЕНТ через свой композит
     *(цветовой буфер при этом полностью корректен!) — это и есть «чёрный экран».
     */
    @org.jetbrains.annotations.Nullable
    public static Boolean irisDepthColorDisabled() {
        try {
            if (!irisChecked) {
                irisChecked = true;
                Class<?> cl = Class.forName("net.irisshaders.iris.mixin.DepthColorStorage");
                for (java.lang.reflect.Field f : cl.getDeclaredFields()) {
                    if (f.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        irisDepthColorField = f;
                        break;
                    }
                }
            }
            return irisDepthColorField == null ? null
                    : (Boolean) irisDepthColorField.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Принудительный сброс маскировки Iris (disableDepthColor): возвращает флаг
     * в false и восстанавливает colorMask + depth test управляемым API.
     * Вызывается в конце наших late-pass —,end кадра снова идёт по ванильному
     * present'у.
     */
    public static void forceIrisDepthColorEnabled() {
        try {
            Boolean disabled = irisDepthColorDisabled();
            if (disabled == null || !disabled) {
                return;
            }
            irisDepthColorField.setBoolean(null, false);
            com.mojang.blaze3d.platform.GlStateManager._colorMask(true, true, true, true);
            com.mojang.blaze3d.platform.GlStateManager._enableDepthTest();
            MainRegistry_LOGGER.info("HBM iris DepthColorStorage reset (was masking)");
        } catch (Throwable ignored) {
        }
    }

    /**
     * ПОЛНАЯ инвалидация статического кеша {@code ShaderInstance.lastProgramId}
     * (установить в -1, чтобы СЛЕДУЮЩИЙ apply() любого шейдера сделал честный
     * glUseProgram).
     *
     * ЗАЧЕМ: клоббер Oculus (`_glUseProgram(0)`) прилетает в самом начале кадра,
     * ДО отрисовки неба — а первое, что рисует мир, это skyBuffer через
     * position_color. Если прошлый кадр закончился тем же шейдером (GUI пестрит
     * position_color), apply() неба ПРОПУСКАЕТ бинд, quad неба не растеризуется
     * (core-профиль, программа 0), и весь кадр показывает цвет очистки буфера —
     * в мире на y<0 это почти чёрный (2,2,0). Пошаговые px-замеры подтвердили:
     * кадр уже чёрный на fsp[px.sky]. Стадий раньше неба в Forge нет, поэтому
     * единственная точка лечения — КОНЕЦ предыдущего кадра.
     *
     * Имя поля ищется по ТИПУ (единственный статический int в ShaderInstance —
     * lastProgramId), поэтому рефлексия работает и под SRG в проде.
     */
    public static void invalidateStaticProgramCache() {
        try {
            if (lastProgramIdField == null) {
                for (java.lang.reflect.Field f : ShaderInstance.class.getDeclaredFields()) {
                    if (f.getType() == int.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())
                            && !java.lang.reflect.Modifier.isFinal(f.getModifiers())
                            && !f.isSynthetic()) {
                        f.setAccessible(true);
                        lastProgramIdField = f;
                        break;
                    }
                }
            }
            if (lastProgramIdField != null) {
                lastProgramIdField.setInt(null, -1);
            }
        } catch (Throwable ignored) {
        }
    }

    private static java.lang.reflect.Field lastProgramIdField;

    /**
     * Восстановление стандартной текстурной троицы ВАНИЛЬНЫМИ управляемыми
     * вызовами: атлас блоков (TU0), оверлей (TU1), лайтмап (TU2).
     *
     * ЗАЧЕМ: диагностика px.pad.* показала, что в кадрах под Oculus к началу
     * BE-фазы физические бинды всех текстурных юнитов = 0 (units=[0/0/0]),
     * при этом кеш GlStateManager считает их живыми — управляемые бинды
     * ванили и модов но-опятся, и всё, что рисуется до первого «сырого»
     * block_lit-дро (пусковая с ракетой), получает чёрные текстуры. Данный
     * вызов ставит и физику, и кеш в согласованное состояние — вызывать
     * как можно раньше в кадре (первый RenderLevelStageEvent, до энтити).
     */
    public static void restoreVanillaTextureBindings() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                    .bind(); // управляемо: TU0 + кеш
            mc.gameRenderer.overlayTexture().setupOverlayColor(); // TU1 управляемо
            mc.gameRenderer.lightTexture().turnOnLightLayer();    // TU2 управляемо
            // Управляемо: сырой glActiveTexture рассинхронизировал бы кеш юнита
            com.mojang.blaze3d.platform.GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Принудительный честный ресинк факторов блендинга.
     *
     * GlStateManager._blendFuncSeparate НО-ОПИТСЯ при совпадении факторов с кешем.
     * Если кеш разошёлся с физикой (кто угодно в кадре — наши RenderType'ы, чужие
     * моды, состояние прошлого кадра), ванильные отрисовки с НЕСТАНДАРТНЫМ
     * блендингом ломаются молча:
     *  - виньетка (multiply ZERO/ONE_MINUS_SRC_COLOR) рисуется обычным блендом —
     *    её квад кладётся на экран как непрозрачный тёмный слой («чёрный экран»);
     *  - солнце/луна (аддитивный SRC_ALPHA/ONE) — «чёрный квадрат» вокруг.
     * Sentinel (0,0,0,0) гарантирует реальный GL-вызов, затем восстанавливаем
     * ванильное значение по умолчанию. Дро между вызовами нет — безопасно.
     */
    public static void forceHonestBlendState() {
        try {
            com.mojang.blaze3d.platform.GlStateManager._blendFuncSeparate(0, 0, 0, 0);
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Сброс статического кеша {@code com.mojang.blaze3d.shaders.BlendMode.lastApplied}.
     *
     * КАК ЛОМАЕТ: {@code ShaderInstance.apply()} (1.20.1, строка ~337) применяет
     * blend из JSON шейдера. Наши кастомные шейдеры (nuke_cloud/nuke_add)
     * несут НЕ-opaque режимы (альфа/аддитив) — после их apply() кеш остаётся
     * non-opaque. Следующий ВАНИЛЬНЫЙ шейдер без blend в json (position_tex и
     * т.п., opaque) видит смену opacity и делает RenderSystem.disableBlend() —
     * молча убивая блендинг для ванильных отрисовок, которые включили его
     * вручную: виньетка (multiply) рисуется непрозрачной тёмной текстурой во
     * весь экран («чёрный экран» на Fancy), солнце/луна (аддитив) — чёрными
     * квадратами, полупрозрачные слои GUI — чёрными плашками.
     * После сброса в null следующий apply() выполняет ПОЛНУЮ честную
     * установку (enable/disable + факторы). Поле ищется по ТИПУ (единственный
     * static BlendMode в классе) — работает и под SRG-именами в проде.
     */
    public static void invalidateBlendModeCache() {
        try {
            if (blendLastAppliedField == null) {
                for (java.lang.reflect.Field f : com.mojang.blaze3d.shaders.BlendMode.class
                        .getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                            && !f.isSynthetic()
                            && f.getType() == com.mojang.blaze3d.shaders.BlendMode.class) {
                        f.setAccessible(true);
                        blendLastAppliedField = f;
                        break;
                    }
                }
            }
            if (blendLastAppliedField != null) {
                blendLastAppliedField.set(null, null);
            }
        } catch (Throwable ignored) {
        }
    }

    private static java.lang.reflect.Field blendLastAppliedField;

    private static final org.slf4j.Logger MainRegistry_LOGGER =
            com.hbm_m.main.MainRegistry.LOGGER;
}
