#version 150

in vec3 fragNormal;
in vec2 texCoord;
in vec2 lightCoord;
in float vertexDistance;

uniform sampler2D Sampler0;  // Block Atlas (TEXTURE0)
uniform sampler2D Sampler2;  // Lightmap    (TEXTURE2)

// Ванильные uniform'ы тумана (автоматически передаются Minecraft)
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

// Ванильная функция тумана из rendertype_solid.fsh
float linear_fog_fade(float distanceToCamera, float fogStart, float fogEnd) {
    if (distanceToCamera <= fogStart) {
        return 1.0;
    } else if (distanceToCamera >= fogEnd) {
        return 0.0;
    }
    return smoothstep(fogEnd, fogStart, distanceToCamera);
}

void main() {
    // Базовый цвет из текстуры
    vec4 color = texture(Sampler0, texCoord);
    
    // ИСПРАВЛЕНО: Clamp lightCoord к [0, 1] для предотвращения артефактов
    vec2 clampedLight = clamp(lightCoord, 0.0, 1.0);
    vec4 lightmapColor = texture(Sampler2, clampedLight);
    
    // Применяем освещение
    vec4 litColor = vec4(color.rgb * lightmapColor.rgb, color.a);
    
    // Альфа-тест
    if (litColor.a < 0.1) {
        discard;
    }
    
    // Применяем туман (как в vanilla Minecraft)
    float fogFactor = linear_fog_fade(vertexDistance, FogStart, FogEnd);
    fragColor = vec4(mix(FogColor.rgb, litColor.rgb, fogFactor), litColor.a);
}
