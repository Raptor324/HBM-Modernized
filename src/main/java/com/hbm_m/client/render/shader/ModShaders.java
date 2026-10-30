package com.hbm_m.client.render.shader;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

@OnlyIn(Dist.CLIENT)
public class ModShaders {
    private static ShaderInstance blockLitSimpleShader;
    /** Weapon viewmodel variant of block_lit_simple (uColor tint, weapon Sampler0, constant packedLight). */
    private static ShaderInstance weaponLitShader;
    private static ShaderInstance blockLitInstancedShader;
    private static ShaderInstance nukeCloudShader;
    private static ShaderInstance nukeAddShader;
    private static ShaderInstance gunAdditiveShader;
    private static ShaderInstance ntColorTexLightmapShader;
    private static ShaderInstance dhDepthBlitShader;

    public static ShaderInstance getNukeCloudShader() {
        return nukeCloudShader;
    }

    public static void setNukeCloudShader(ShaderInstance shader) {
        nukeCloudShader = shader;
    }

    public static ShaderInstance getNukeAddShader() {
        return nukeAddShader;
    }

    public static void setNukeAddShader(ShaderInstance shader) {
        nukeAddShader = shader;
    }

    public static ShaderInstance getGunAdditiveShader() {
        return gunAdditiveShader;
    }

    public static void setGunAdditiveShader(ShaderInstance shader) {
        gunAdditiveShader = shader;
    }

    /** Vanilla position_color_tex_lightmap clone for NT particle render types (see the shader header). */
    public static ShaderInstance getNtColorTexLightmapShader() {
        return ntColorTexLightmapShader;
    }

    public static void setNtColorTexLightmapShader(ShaderInstance shader) {
        ntColorTexLightmapShader = shader;
    }

    public static ShaderInstance getDhDepthBlitShader() {
        return dhDepthBlitShader;
    }

    public static void setDhDepthBlitShader(ShaderInstance shader) {
        dhDepthBlitShader = shader;
    }

    public static ShaderInstance getBlockLitSimpleShader() {
        return blockLitSimpleShader;
    }

    public static void setBlockLitSimpleShader(ShaderInstance shader) {
        blockLitSimpleShader = shader;
    }

    public static ShaderInstance getWeaponLitShader() {
        return weaponLitShader;
    }

    public static void setWeaponLitShader(ShaderInstance shader) {
        weaponLitShader = shader;
    }

    public static ShaderInstance getBlockLitInstancedShader() {
        return blockLitInstancedShader;
    }

    public static void setBlockLitInstancedShader(ShaderInstance shader) {
        blockLitInstancedShader = shader;
    }

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
        return resolvePositionTexColor(getNukeCloudShader());
    }

    /**
     * Additive shader for POSITION_TEX_COLOR (flashes, glare).
     * Under Iris - the pack's ExtendedShader; without Iris - nuke_add.
     */
    public static ShaderInstance resolveAddTexColor() {
        return resolvePositionTexColor(getNukeAddShader());
    }

    /**
     * Shared POSITION_TEX_COLOR routing: the pack's ExtendedShader when Iris is
     * active (shadow-pass aware), else our custom variant, else vanilla.
     */
    private static ShaderInstance resolvePositionTexColor(ShaderInstance custom) {
        if (useIrisRouting()) {
            ShaderInstance iris = IrisExtendedShaderAccess.getTexColorShader(
                    ShaderCompatibilityDetector.isRenderingShadowPass());
            if (iris != null) {
                return iris;
            }
        }
        if (custom != null) {
            return custom;
        }
        return GameRenderer.getPositionTexColorShader();
    }

    /**
     * Returns true if {@code shader} is one of our custom mod {@link ShaderInstance}s
     * that is NOT an Iris {@code ExtendedShader} and therefore gets its color/depth mask
     * locked by Oculus/Iris {@code MixinShaderInstance.iris$onTail} when applied directly.
     */
    public static boolean isModDirectShader(ShaderInstance shader) {
        if (shader == null) return false;
        return shader == gunAdditiveShader
                || shader == nukeCloudShader
                || shader == nukeAddShader
                || shader == ntColorTexLightmapShader
                || shader == weaponLitShader
                || shader == blockLitSimpleShader;
    }

    /**
     * Returns true if {@code shader} requires additive blending (SRC_ALPHA, ONE) with
     * depth writes disabled, even if an active Iris shaderpack tried to override blending.
     */
    public static boolean isModAdditiveShader(ShaderInstance shader) {
        if (shader == null) return false;
        return shader == gunAdditiveShader || shader == nukeAddShader;
    }
}
