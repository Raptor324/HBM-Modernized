#version 330 core

// Weapon viewmodel variant of block_lit (audit firearms parity §7.4 Phase 1).
// - Sampler0 = the weapon's own texture (NOT the block atlas)
// - uColor   = per-part RGBA tint from GunGL.color(r,g,b,a)
// - uLightUV = constant lightmap UV derived CPU-side from GunGL.light()
//   (packedLight passed straight through; no BlockPos-anchored 8-corner
//   LightSampleCache probing - the viewmodel is camera-relative)
// - no fog: matches the legacy NEW_ENTITY entityCutout/translucent look
//   (entity renders are not distance-fogged)

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 uColor;
uniform vec2 uLightUV;
// 1.0 = apply vanilla-style diffuse shading, 0.0 = fullbright
// (additive/emissive parts and GunGL fullbright mode)
uniform float uDiffuse;
// Camera-relative light directions (minecraft_mix_light, rendertype_entity_*).
// CPU flips Y for GUI-INVENTORY draws: setupInv mirrors/rotates the model so
// its Y axis points downward in view space (level default = lit from above).
uniform vec3 uLight0;
uniform vec3 uLight1;

out vec2 texCoord;
out vec2 lightmapUV;
out vec4 vColor;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    vec3 viewNormal = normalize(mat3(ModelViewMat) * Normal);
    float diff0 = max(dot(normalize(uLight0), viewNormal), 0.0);
    float diff1 = max(dot(normalize(uLight1), viewNormal), 0.0);
    float diffuse = 0.25 + diff0 * 0.6 + diff1 * 0.4;

    texCoord = UV0;
    lightmapUV = uLightUV;
    vColor = vec4(uColor.rgb * mix(1.0, diffuse, uDiffuse), uColor.a);
}
