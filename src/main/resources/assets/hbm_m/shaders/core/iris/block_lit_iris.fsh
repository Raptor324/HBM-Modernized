#version 330 core
// Iris ExtendedShader-вариант block_lit.fsh: сэмплеры/юниформы с префиксом iris_
// (ExtendedShader.getUniform() резолвит "iris_" + name).
// Вывод — ОДИН fragColor: vanilla-parity с iris-fallback (ShaderSynthesizer.fsh),
// который пишет единственный выход в полный gbuffer — паки рассчитаны на это.

in vec2 texCoord;
in vec2 lightmapUV;
in float vertexDistance;
in float vFadeAlpha;
in vec3 worldNormal;
// Per-instance тинт части (InstColor из block_lit_instanced_iris.vsh).
in vec4 vColor;

uniform sampler2D iris_Sampler0;
uniform sampler2D iris_Sampler2;
uniform vec4 iris_FogColor;
uniform float iris_FogStart;
uniform float iris_FogEnd;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(iris_Sampler0, texCoord);

    // Vanilla dynamic lightmap: sky darken, gamma, night vision, dimension tint.
    vec3 lm = texture(iris_Sampler2, lightmapUV).rgb;
    vec3 lit = baseColor.rgb * lm * vColor.rgb;

    // Vanilla-compliant directional shading: matches block face weights on cardinal
    // axes (UP: 1.0, DOWN: 0.5, N/S: 0.8, E/W: 0.6) and smoothly interpolates
    // over curved geometry without diagonal dark bands or facing inversion.
    vec3 n = length(worldNormal) > 1e-4 ? normalize(worldNormal) : vec3(0.0, 1.0, 0.0);
    vec3 n2 = n * n;
    float yWeight = n.y > 0.0 ? 1.0 : 0.5;
    float diff = n2.x * 0.6 + n2.y * yWeight + n2.z * 0.8;
    lit *= diff;

    float alpha = baseColor.a * vFadeAlpha;
    if (alpha < 0.01) {
        discard;
    }

    // Защита от нулевого vanilla-fog стейта под паком: FogEnd <= FogStart -> тумана нет
    // (иначе (dist-0)/1e-4 = 1 -> полная заливка FogColor = чёрные машины).
    float fogFactor = 0.0;
    if (iris_FogEnd > iris_FogStart) {
        fogFactor = clamp((vertexDistance - iris_FogStart) / (iris_FogEnd - iris_FogStart), 0.0, 1.0);
    }
    vec3 colorWithFog = mix(lit, iris_FogColor.rgb, fogFactor);

    fragColor = vec4(colorWithFog, alpha);
}
