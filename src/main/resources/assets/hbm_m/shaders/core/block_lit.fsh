#version 330 core

in vec2 texCoord;
in vec2 lightmapUV;
in float vertexDistance;
in float vFadeAlpha;
// Мировая нормаль из VS — для направленного затенения (не зависит от камеры).
in vec3 worldNormal;
// Per-instance тинт (block_lit_instanced пишет InstColor; block_lit — константу white).
in vec4 vColor;

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
    vec3 lit = baseColor.rgb * lm * vColor.rgb;
    // lit *= 0;

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

    float fogDiff = max(FogEnd - FogStart, 1e-4);
    float fogFactor = clamp((vertexDistance - FogStart) / fogDiff, 0.0, 1.0);
    vec3 colorWithFog = mix(lit, FogColor.rgb, fogFactor);

    fragColor = vec4(colorWithFog, alpha);
}
