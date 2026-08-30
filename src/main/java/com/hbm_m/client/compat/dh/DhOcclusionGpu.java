package com.hbm_m.client.compat.dh;

/**
 * Утилиты DH-совместимости для дальнего прохода EngineHandler.
 *
 * История: раньше через мост рисовали геометрию прямо в DH FBO с нативным
 * depth-тестом против LOD (beginFarContentPass/endFarContentPass). Этот путь
 * полностью удалён — запись в их FBO отравляла композит apply.frag. Сейчас
 * класс даёт только два параметра пайплайна:
 *  - {@link #vanillaFarPlane()} — граница near/far разбиения контента;
 *  - {@link #getDhActiveDepthTextureId()} — DEPTH32F текстура DH для
 *    попиксельной окклюзии в шейдере nuke_cloud (Sampler1).
 */
public final class DhOcclusionGpu {

    private DhOcclusionGpu() {}

    /**
     * Граница деления near/far контента в БЛОКАХ (не в квадрате).
     *
     * Ловушки, обе были в проде:
     *  1. НЕ читать RenderSystem.getProjectionMatrix(): в момент DH-события
     *     там проекция Iris/DH (в логе: 433 вместо 7681) — пороги пассов
     *     расходились, контент рисовался дважды.
     *  2. Порог = граница ВАНИЛЬНОЙ ГЕОМЕТРИИ (RD*16), НЕ far plane
     *     проекции (RD*16*40). Если взять far plane, контент между чанками
     *     и far plane уходит в ближний пасс, где против него нет геометрии
     *     («всегда перед» горами DH) и вершины клипаются far plane.
     * +1 чанк запаса на пограничные квады.
     */
    public static float vanillaFarPlane() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int rd = mc.options.getEffectiveRenderDistance();
            if (rd > 0) return (rd + 1) * 16.0F;
        } catch (Throwable ignored) {}
        return 1280.0F;
    }

    /** Активная DEPTH32F текстура DH (для Sampler1 шейдера nuke_cloud).
     *  Field кешируется: дергается несколько раз за кадр, а поиск по строкам
     *  (Class.forName/getField) — микроаллокации и строковые сравнения
     *  в горячем цикле рендера. */
    private static java.lang.reflect.Field dhActiveDepthField;
    private static boolean dhDepthFieldInitialized;

    public static int getDhActiveDepthTextureId() {
        if (!dhDepthFieldInitialized) {
            dhDepthFieldInitialized = true;
            try {
                Class<?> c = Class.forName("com.seibel.distanthorizons.core.render.DhApiRenderProxy");
                dhActiveDepthField = c.getField("activeOpenGlDhDepthTextureId");
            } catch (Throwable ignored) {}
        }
        if (dhActiveDepthField != null) {
            try {
                return dhActiveDepthField.getInt(null);
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    /**
     * Репликация зоны недостоверной глубины DH — dither-fade «Fade Nearby DH LODs».
     *
     * КАК РАБОТАЕТ FADE У DH: в фрагментном шейдере террейна
     * (terrain/blaze/frag.fsh) фрагменты в кольце [uClipDistance, 1.5·uClipDistance]
     * отбрасываются стохастически (bayerMatrix4x4 по gl_FragCoord + smoothstep).
     * Отброшенный фрагмент не пишет НИ цвета, НИ глубины → внутри кольца
     * DEPTH32F содержит шум: половина пикселей пустая, половина — с глубиной
     * полурастворённых склонов. Копировать эту глубину в главный z-buffer
     * нельзя: гриб получает ту же байерову рябь и ложные срезы, ползущие
     * вместе с камерой.
     *
     * Формула uClipDistance из RenderUtil.getNearClipPlaneInBlocks():
     *   ratio = overdrawPrevention (конфиг; <0 = авто по ванильному RD,
     *           под паком Iris авто = 0.2)
     *   dist  = max(1, vanillaRD*16 * ratio)
     *   R     = dist / sqrt(1 + tan(35°)²·(aspect² + 1))   // FOV захардкожен у DH = 70°
     *   uClipDistance = R + 16
     * Маска = верх dither-зоны = 1.5·(R + 16).
     *
     * reduceOverdrawWithFastMovement сознательно НЕ реплицируем: без него
     * маска при быстром движении чуть БОЛЬШЕ реального кольца — консервативно.
     *
     * @return дистанция в блоках; 0 = маска не определена (не применять).
     */
    public static float ditherFadeMaskDistance() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int rd = mc.options.getEffectiveRenderDistance();
            if (rd <= 0) {
                return 0.0F;
            }
            float overdraw = getOverdrawPreventionConfigValue();
            float ratio;
            if (overdraw >= 0.0F) {
                ratio = Math.max(0.05F, Math.min(1.0F, overdraw));
            } else {
                boolean pack = com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
                if (pack || rd <= 2) {
                    ratio = 0.2F;
                } else if (rd <= 4) {
                    ratio = 0.3F;
                } else if (rd <= 6) {
                    ratio = 0.6F;
                } else if (rd <= 10) {
                    ratio = 0.8F;
                } else {
                    ratio = 0.9F;
                }
            }
            float distBlocks = Math.max(1.0F, rd * 16.0F * ratio);
            var win = mc.getWindow();
            float aspect = (float) win.getWidth() / (float) win.getHeight();
            double tanHalfFov = Math.tan(Math.toRadians(35.0));
            double r = distBlocks / Math.sqrt(1.0 + tanHalfFov * tanHalfFov * (aspect * aspect + 1.0));
            return 1.5 * (r + 16.0) > Float.MAX_VALUE ? Float.MAX_VALUE : (float) (1.5 * (r + 16.0));
        } catch (Throwable ignored) {}
        return 0.0F;
    }

    /**
     * Конфиг «Overdraw Prevention» (<0 = авто).
     *
     * Рефлексия закеширована, а САМО ЗНАЧЕНИЕ обновляется не чаще раза в
     * секунду: метод дергается каждый кадр (через ditherFadeMaskDistance →
     * DhDepthCopy), а конфиг DH игрок меняет крайне редко — обход
     * graphics.getClass().getMethods() на каждый кадр был чистым оверхедом.
     */
    private static java.lang.reflect.Method overdrawMethod;
    private static java.lang.reflect.Method overdrawGetValueMethod;
    private static Object overdrawConfigProperty;
    private static boolean overdrawResolved;
    private static float cachedOverdrawValue = -1.0F;
    private static long overdrawNextRefreshMs;

    private static float getOverdrawPreventionConfigValue() {
        long now = System.currentTimeMillis();
        if (overdrawResolved && now < overdrawNextRefreshMs) {
            return cachedOverdrawValue;
        }
        overdrawNextRefreshMs = now + 1000L;
        try {
            if (!overdrawResolved) {
                Class<?> cfg = Class.forName("com.seibel.distanthorizons.core.api.external.methods.config.DhApiConfig");
                Object inst = cfg.getField("INSTANCE").get(null);
                Object graphics = cfg.getMethod("graphics").invoke(inst);
                for (java.lang.reflect.Method mm : graphics.getClass().getMethods()) {
                    if (mm.getName().equals("overdrawPreventionRadius")) {
                        overdrawMethod = mm;
                        break;
                    }
                }
                if (overdrawMethod == null) {
                    overdrawResolved = true;
                    return cachedOverdrawValue = -1.0F;
                }
                overdrawConfigProperty = overdrawMethod.invoke(graphics);
                overdrawGetValueMethod = overdrawConfigProperty.getClass().getMethod("getValue");
                overdrawResolved = true;
            }
            Object value = overdrawGetValueMethod.invoke(overdrawConfigProperty);
            return cachedOverdrawValue = value instanceof Number n ? n.floatValue() : -1.0F;
        } catch (Throwable ignored) {}
        return cachedOverdrawValue = -1.0F;
    }
}
