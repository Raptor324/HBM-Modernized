package com.hbm_m.client.render.shader;

import net.minecraft.client.renderer.ShaderInstance;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class ModShaders {
    private static ShaderInstance dynamicCutoutShader;
    private static ShaderInstance blockLitSimpleShader;
    private static ShaderInstance blockLitInstancedShader;
    private static ShaderInstance blockLitSimpleSlicedShader;
    private static ShaderInstance blockLitInstancedSlicedShader;
    private static ShaderInstance thermalVisionShader;
    private static ShaderInstance nukeCloudShader;
    private static ShaderInstance nukeAddShader;
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

    public static ShaderInstance getDhDepthBlitShader() {
        return dhDepthBlitShader;
    }

    public static void setDhDepthBlitShader(ShaderInstance shader) {
        dhDepthBlitShader = shader;
    }

    public static ShaderInstance getDynamicCutoutShader() {
        return dynamicCutoutShader;
    }

    public static void setDynamicCutoutShader(ShaderInstance shader) {
        dynamicCutoutShader = shader;
    }

    /**
     * @deprecated Use {@link #getBlockLitInstancedShader()} or {@link #getBlockLitSimpleShader()}
     *             explicitly. This alias returns the instanced variant for backward compatibility.
     */
    @Deprecated
    public static ShaderInstance getBlockLitShader() {
        return blockLitInstancedShader;
    }

    public static ShaderInstance getBlockLitSimpleShader() {
        return blockLitSimpleShader;
    }

    public static void setBlockLitSimpleShader(ShaderInstance shader) {
        blockLitSimpleShader = shader;
    }

    public static ShaderInstance getBlockLitInstancedShader() {
        return blockLitInstancedShader;
    }

    public static void setBlockLitInstancedShader(ShaderInstance shader) {
        blockLitInstancedShader = shader;
    }

    public static ShaderInstance getBlockLitSimpleSlicedShader() {
        return blockLitSimpleSlicedShader;
    }

    public static void setBlockLitSimpleSlicedShader(ShaderInstance shader) {
        blockLitSimpleSlicedShader = shader;
    }

    public static ShaderInstance getBlockLitInstancedSlicedShader() {
        return blockLitInstancedSlicedShader;
    }

    public static void setBlockLitInstancedSlicedShader(ShaderInstance shader) {
        blockLitInstancedSlicedShader = shader;
    }

    public static ShaderInstance getThermalVisionShader() {
        return thermalVisionShader;
    }

    public static void setThermalVisionShader(ShaderInstance shader) {
        thermalVisionShader = shader;
    }
}
