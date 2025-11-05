#version 330 core

in vec2 texCoord;
in float brightness;
in float vertexDistance;
in vec3 fragNormal;

uniform sampler2D Sampler0;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(Sampler0, texCoord);
    
    vec3 lit = baseColor.rgb * brightness;  // Сначала применяем освещение
    lit *= 0.6; 
    
    if (baseColor.a < 0.1) {
        discard;
    }
    
    // Fog
    float fogFactor = clamp((FogEnd - vertexDistance) / (FogEnd - FogStart), 0.0, 1.0);
    vec3 colorWithFog = mix(FogColor.rgb, lit, fogFactor);
    
    fragColor = vec4(colorWithFog, baseColor.a);
}
