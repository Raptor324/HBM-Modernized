package com.hbm_m.client.model.render;

import net.minecraft.client.renderer.ShaderInstance;

public class ModShaders {
    private static ShaderInstance dynamicCutoutShader;
    private static ShaderInstance blockLitShader;

    public static ShaderInstance getDynamicCutoutShader() {
        return dynamicCutoutShader;
    }

    public static void setDynamicCutoutShader(ShaderInstance shader) {
        dynamicCutoutShader = shader;
    }

    // ✅ ИСПРАВЛЕНИЕ: Добавляем геттер и сеттер для blockLitShader
    public static ShaderInstance getBlockLitShader() {
        return blockLitShader;
    }

    public static void setBlockLitShader(ShaderInstance shader) {
        blockLitShader = shader;
    }
}
