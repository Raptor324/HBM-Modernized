package com.hbm_m.client.render;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModShaders {
    private static ShaderInstance dynamicCutoutShader;
    private static ShaderInstance blockLitShader;

    public static ShaderInstance getDynamicCutoutShader() {
        return dynamicCutoutShader;
    }

    public static void setDynamicCutoutShader(ShaderInstance shader) {
        dynamicCutoutShader = shader;
    }


    public static ShaderInstance getBlockLitShader() {
        return blockLitShader;
    }

    public static void setBlockLitShader(ShaderInstance shader) {
        blockLitShader = shader;
    }
}
