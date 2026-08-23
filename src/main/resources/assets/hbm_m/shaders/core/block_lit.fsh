#version 330 core

in vec2 texCoord;
in vec2 lightmapUV;
in float vertexDistance;
in float vFadeAlpha;
// Мировая нормаль из VS — для направленного затенения (не зависит от камеры).
in vec3 worldNormal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(Sampler0, texCoord);

    // Vanilla dynamic lightmap: encodes sky darken, client brightness (gamma),
    // night vision, darkness, and dimension tint — same as block models.
    vec3 lm = texture(Sampler2, lightmapUV).rgb;
    vec3 lit = baseColor.rgb * lm;
    // lit *= 0;

    // Направленное затенение в стиле vanilla: верх 1.0, бока 0.8, низ 0.6.
    // Верхние грани сохраняют прежнюю яркость (0.8), остальные темнее — модель
    // не станет ярче, чем раньше.
    vec3 n = normalize(worldNormal);
    float shade = 0.8 + 0.2 * n.y;
    lit *= shade;

    float alpha = baseColor.a * vFadeAlpha;
    if (alpha < 0.01) {
        discard;
    }

    float fogFactor = clamp((FogEnd - vertexDistance) / (FogEnd - FogStart), 0.0, 1.0);
    vec3 colorWithFog = mix(FogColor.rgb, lit, fogFactor);

    fragColor = vec4(colorWithFog, alpha);
}
