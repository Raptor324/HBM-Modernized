#version 330 core
// Iris ExtendedShader FSH for deferred shader packs with the "packed" gbuffer
// scheme (detected by IrisInstancedEncoders from the pack's gbuffers source):
//   target.x = packUnorm2x8(albedo.rg)
//   target.y = packUnorm2x8(albedo.b, material_mask)
//   target.z = packUnorm2x8(encodeUnitVector(flat_normal))
//   target.w = packUnorm2x8(light_levels)
// The pack computes lighting in its deferred composite, so the gbuffer albedo
// is NOT multiplied by the lightmap. The flat normal is in world axes
// (mat3(gbufferModelViewInverse)*normal on the pack side). material_mask = 0:
// the mod's blocks are not listed in the pack's block.properties, and the pack
// program assigns no special materials to them.
// The byte math of packing/oct-encode is compatible with the pack decoder
// (unpack_unorm_2x8: hi*256+lo in a 16-bit unorm channel).

in vec2 texCoord;
in vec2 lightLevels;
in vec3 worldNormal;
in float vFadeAlpha;
// Per-instance part tint (InstColor from block_lit_instanced_packed.vsh).
// The pack's gbuffer albedo is 8-bit unorm: overbright (RGB > 1) is clamped
// away by the packing here; heat glow light comes from lightLevels (fullbright).
in vec4 vColor;

uniform sampler2D iris_Sampler0;

out vec4 fragColor;

// Two 8-bit bytes (hi = v.x, lo = v.y) into a 16-bit unorm channel:
// value = (hi*256 + lo) / 65535, with rounding to the nearest byte.
float packUnorm2x8(vec2 v) {
    v = clamp(v, vec2(0.0), vec2(1.0));
    vec2 bytes = floor(255.0 * v + 0.5);
    return dot(bytes, vec2(1.0 / 65535.0, 256.0 / 65535.0));
}

float packUnorm2x8(float x, float y) {
    return packUnorm2x8(vec2(x, y));
}

vec2 signNonZero(vec2 v) {
    return mix(vec2(-1.0), vec2(1.0), greaterThanEqual(v, vec2(0.0)));
}

// Octahedral encoding of a unit vector into [0,1]^2: project the sphere onto
// an octahedron and unfold the lower hemisphere along the diagonals.
vec2 encodeUnitVector(vec3 n) {
    vec2 p = n.xy * (1.0 / (abs(n.x) + abs(n.y) + abs(n.z)));
    p = n.z <= 0.0 ? ((1.0 - abs(p.yx)) * signNonZero(p)) : p;
    return 0.5 * p + 0.5;
}

void main() {
    vec4 baseColor = texture(iris_Sampler0, texCoord);
    float alpha = baseColor.a * vFadeAlpha;
    if (alpha < 0.1) {
        discard;
    }

    vec3 n = normalize(worldNormal);

    vec3 albedo = baseColor.rgb * vColor.rgb;
    fragColor = vec4(
        packUnorm2x8(albedo.rg),
        packUnorm2x8(albedo.b, 0.0),
        packUnorm2x8(encodeUnitVector(n)),
        packUnorm2x8(lightLevels)
    );
}
