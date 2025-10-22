#version 330 core

in vec2 texCoord;
in vec2 lightCoord;
in float vertexDistance;
in vec3 fragNormal;

uniform sampler2D Sampler0;    // Текстуры блоков (TEXTURE0)
uniform sampler2D Sampler2;    // Lightmap (TEXTURE2)

uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

// Функция линейного затухания тумана (как в ванильном Minecraft)
float linear_fog_fade(float dist, float start, float end) {
    if (dist <= start) return 1.0;
    if (dist >= end)   return 0.0;
    return smoothstep(end, start, dist);
}

void main() {
    // Получаем цвет из атласа
    vec4 baseColor = texture(Sampler0, texCoord);

    // Применяем lightmap (clamp для безопасности)
    vec2 lc = clamp(lightCoord, 0.0, 1.0);
    vec3 lightMapColor = texture(Sampler2, lc).rgb;
    vec3 lit = baseColor.rgb * lightMapColor;

    // Отсечение полупрозрачных фрагментов
    if (baseColor.a < 0.1) {
        discard;
    }

    // Вычисление тумана
    float fogFactor = linear_fog_fade(vertexDistance, FogStart, FogEnd);
    vec3 colorWithFog = mix(FogColor.rgb, lit, fogFactor);

    fragColor = vec4(colorWithFog, baseColor.a);
}
