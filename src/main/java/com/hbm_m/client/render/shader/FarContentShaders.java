package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.ModShaders;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Выбор шейдера для дальнего контента (NT-частицы, вспышки).
 *
 * ПОЧЕМУ ЭТО НУЖНО: под активным шейдерпаком Iris маскирует ЛЮБОЙ неизвестный
 * ему модовый ShaderInstance — MixinShaderInstance.onTail вызывает
 * DepthColorStorage.disableDepthColor() (colorMask off + depth test off),
 * поэтому кастомный nuke_cloud рисуется «в никуда». Известные же ключи
 * (vanilla core shaders) заменяются на ExtendedShader пака и маршрутизируются
 * в его пайплайн корректно.
 *
 * РЕШЕНИЕ: под паком отдаём ExtendedShader ключа TEXTURED_COLOR
 * (ProgramId.Textured, формат POSITION_TEX_COLOR — совпадает с нашим),
 * полученный рефлексией через ShaderMap. Окклюзия против LOD при этом
 * обеспечивается нативным depth-тестом: DH под Iris рендерит LOD'ы прямо
 * в depth-buffer пака (LodRendererEvents override), отдельная копия глубины
 * не нужна.
 */
public final class FarContentShaders {

    private FarContentShaders() {}

    /** true, если дальний контент должен идти через Iris-pipeline. */
    public static boolean useIrisRouting() {
        return ShaderCompatibilityDetector.isExternalShaderActive();
    }

    /**
     * Шейдер для квадов POSITION_TEX_COLOR. Вызывается на каждый
     * setupRenderState (Supplier в ShaderStateShard), поэтому переключение
     * пака подхватывается без пересоздания RenderType.
     */
    public static ShaderInstance resolveTexColor() {
        if (useIrisRouting()) {
            ShaderInstance iris = IrisExtendedShaderAccess.getTexColorShader(
                    ShaderCompatibilityDetector.isRenderingShadowPass());
            if (iris != null) {
                return iris;
            }
        }
        ShaderInstance custom = ModShaders.getNukeCloudShader();
        if (custom != null) {
            return custom;
        }
        return GameRenderer.getPositionTexColorShader();
    }
}
