#version 330 core

in vec2 texCoord;
in vec2 lightmapUV;
in float vertexDistance;
in float vFadeAlpha;

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
    lit *= 0.6;

    float alpha = baseColor.a * vFadeAlpha;
    if (alpha < 0.01) {
        discard;
    }

    float fogFactor = clamp((FogEnd - vertexDistance) / (FogEnd - FogStart), 0.0, 1.0);
    vec3 colorWithFog = mix(FogColor.rgb, lit, fogFactor);

    fragColor = vec4(colorWithFog, alpha);
}
