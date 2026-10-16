package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.shader.ModShaders;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Shader selection for far content (NT particles, flashes).
 *
 * <p>Why this is needed: under an active Iris shader pack, Iris masks ANY
 * unknown mod {@link ShaderInstance} - MixinShaderInstance.onTail calls
 * DepthColorStorage.disableDepthColor() (colorMask off + depth test off),
 * so a custom nuke_cloud shader draws "into nowhere". Known keys (vanilla
 * core shaders) are replaced by the pack's ExtendedShader and routed into
 * its pipeline correctly.</p>
 *
 * <p>Solution: under a pack, return the ExtendedShader for the TEXTURED_COLOR
 * key (ProgramId.Textured, POSITION_TEX_COLOR format - same as ours),
 * obtained reflectively via ShaderMap. Occlusion against LODs is provided by
 * the native depth test: DH under Iris renders LODs directly into the pack's
 * depth buffer (LodRendererEvents override), so a separate depth copy is not
 * needed.</p>
 */
public final class FarContentShaders {

    private FarContentShaders() {}

    /** Returns true if far content should be routed through the Iris pipeline. */
    public static boolean useIrisRouting() {
        return ShaderCompatibilityDetector.isExternalShaderActive();
    }

    /**
     * Shader for POSITION_TEX_COLOR quads. Called on every setupRenderState
     * (Supplier in ShaderStateShard), so pack switches are picked up without
     * recreating the RenderType.
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

    /**
     * Additive shader for POSITION_TEX_COLOR (flashes, glare).
     * Under Iris - the pack's ExtendedShader; without Iris - nuke_add.
     */
    public static ShaderInstance resolveAddTexColor() {
        if (useIrisRouting()) {
            ShaderInstance iris = IrisExtendedShaderAccess.getTexColorShader(
                    ShaderCompatibilityDetector.isRenderingShadowPass());
            if (iris != null) {
                return iris;
            }
        }
        ShaderInstance custom = ModShaders.getNukeAddShader();
        if (custom != null) {
            return custom;
        }
        return GameRenderer.getPositionTexColorShader();
    }
}
