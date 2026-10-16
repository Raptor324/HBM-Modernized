#version 330 core

in vec2 texCoord;
in vec2 lightmapUV;
in float vertexDistance;
in float vFadeAlpha;
// World normal from the VS - for directional shading (camera-independent).
in vec3 worldNormal;
// Per-instance tint (block_lit_instanced writes InstColor; block_lit writes constant white).
in vec4 vColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
// Dither distance fade (nucleusDitherFade, instanced programs only): 1.0 =
// screen-door dissolve instead of alpha blending - no fade-tail sort, no blend
// passes. Not uploaded by the plain block_lit program - stays 0.0.
uniform float uDitherFade;

out vec4 fragColor;

// Interleaved gradient noise (Jimenez): static per-pixel threshold in [0,1).
float fadeDitherThreshold(vec2 fragCoord) {
    return fract(52.9829189 * fract(0.06711056 * fragCoord.x + 0.00583715 * fragCoord.y));
}

void main() {
    // Both texture() samples sit above the dither discard: fadeDitherThreshold
    // varies per pixel, so that discard is non-uniform across a quad, and
    // implicit-derivative texture() lookups in non-uniform control flow are
    // undefined (GLSL spec). Pure reorder - sampling has no side effects.
    vec4 baseColor = texture(Sampler0, texCoord);
    // Vanilla dynamic lightmap: encodes sky darken, client brightness (gamma),
    // night vision, darkness, and dimension tint - same as block models.
    vec3 lm = texture(Sampler2, lightmapUV).rgb;

    if (uDitherFade > 0.5 && vFadeAlpha < 1.0 && vFadeAlpha <= fadeDitherThreshold(gl_FragCoord.xy)) {
        discard;
    }
    vec3 lit = baseColor.rgb * lm * vColor.rgb;

    // Vanilla-compliant directional shading: matches block face weights on cardinal
    // axes (UP: 1.0, DOWN: 0.5, N/S: 0.8, E/W: 0.6) and smoothly interpolates
    // over curved geometry without diagonal dark bands or facing inversion.
    vec3 n = vec3(0.0, 1.0, 0.0);
    if (length(worldNormal) > 1e-4) {
        n = normalize(worldNormal);
    }
    vec3 n2 = n * n;
    float yWeight = n.y > 0.0 ? 1.0 : 0.5;
    float diff = n2.x * 0.6 + n2.y * yWeight + n2.z * 0.8;
    lit *= diff;

    // Dither mode: survivors of the bayer discard above draw at FULL alpha -
    // blending is disabled in that mode, so multiplying by vFadeAlpha here
    // would just write fractional alpha into the main FBO (post-processing /
    // screenshots read it) with no visual fade. Blend mode keeps the fade.
    float alpha = (uDitherFade > 0.5) ? baseColor.a : (baseColor.a * vFadeAlpha);
    if (alpha < 0.01) {
        discard;
    }

    // Guard against a zeroed fog state (FogEnd <= FogStart): with the old
    // max(FogEnd - FogStart, 1e-4) clamp that yielded (dist - 0) / 1e-4 = 1,
    // i.e. full FogColor fill = black machines. Mirrors the guard in
    // iris/block_lit_iris.fsh; identical output whenever FogEnd > FogStart.
    float fogFactor = 0.0;
    if (FogEnd > FogStart) {
        fogFactor = clamp((vertexDistance - FogStart) / (FogEnd - FogStart), 0.0, 1.0);
    }
    vec3 colorWithFog = mix(lit, FogColor.rgb, fogFactor);

    fragColor = vec4(colorWithFog, alpha);
}
