#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

#ifdef USE_INSTANCING
layout(location = 3)  in vec3 InstPos;
layout(location = 4)  in vec4 InstRot;
layout(location = 5)  in vec3 InstBboxMin;
layout(location = 6)  in vec3 InstBboxSize;
#ifdef USE_SLICED_LIGHT
layout(location = 7)  in vec4 InstLightS0C01;
layout(location = 8)  in vec4 InstLightS0C23;
layout(location = 9)  in vec4 InstLightS1C01;
layout(location = 10) in vec4 InstLightS1C23;
layout(location = 11) in vec4 InstLightS2C01;
layout(location = 12) in vec4 InstLightS2C23;
layout(location = 13) in vec4 InstLightS3C01;
layout(location = 14) in vec4 InstLightS3C23;
layout(location = 15) in float InstFadeAlpha;
#else
layout(location = 7)  in vec4 InstLightC01;  // corner0.uv, corner1.uv
layout(location = 8)  in vec4 InstLightC23;
layout(location = 9)  in vec4 InstLightC45;
layout(location = 10) in vec4 InstLightC67;
layout(location = 11) in float InstFadeAlpha;
#endif
#endif

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float FadeAlpha;

// 8-corner trilinear lightmap uniforms (used by the non-instanced path).
// Corner index encoding matches LightSampleCache.getOrSample8:
//   bit 0 -> x, bit 1 -> y, bit 2 -> z; set = max side.
uniform vec3 BboxMin;
uniform vec3 BboxSize;
#ifdef USE_SLICED_LIGHT
// 2x4x2 probe lattice: 4 Y-slices, each holds 4 X/Z corners packed into 2 vec4s.
// Order inside each slice: (x0z0, x1z0) in C01; (x0z1, x1z1) in C23.
uniform vec4 LightS0C01;
uniform vec4 LightS0C23;
uniform vec4 LightS1C01;
uniform vec4 LightS1C23;
uniform vec4 LightS2C01;
uniform vec4 LightS2C23;
uniform vec4 LightS3C01;
uniform vec4 LightS3C23;
#else
uniform vec4 LightC01;
uniform vec4 LightC23;
uniform vec4 LightC45;
uniform vec4 LightC67;
#endif

out vec2 texCoord;
// Vanilla lightmap UV: sampled from Sampler2 in block_lit.fsh.
out vec2 lightmapUV;
out float vertexDistance;
out vec3 fragNormal;
// Per-vertex fade: InstFadeAlpha when instancing (batched flush reads stale uniform otherwise).
out float vFadeAlpha;

#ifdef USE_INSTANCING
mat4 quatToMat4(vec4 q) {
    float xx = q.x * q.x;
    float yy = q.y * q.y;
    float zz = q.z * q.z;
    float xy = q.x * q.y;
    float xz = q.x * q.z;
    float yz = q.y * q.z;
    float wx = q.w * q.x;
    float wy = q.w * q.y;
    float wz = q.w * q.z;

    return mat4(
        1.0 - 2.0 * (yy + zz), 2.0 * (xy + wz),       2.0 * (xz - wy),       0.0,
        2.0 * (xy - wz),       1.0 - 2.0 * (xx + zz), 2.0 * (yz + wx),       0.0,
        2.0 * (xz + wy),       2.0 * (yz - wx),       1.0 - 2.0 * (xx + yy), 0.0,
        0.0,                   0.0,                   0.0,                   1.0
    );
}
#endif

// Trilinear blend of corner (block, sky) samples on the 0..240 lightmap grid.
// Raw values are fed into the vanilla dynamic lightmap texture in the fragment
// shader so client brightness, night vision, and dimension curves match blocks.
vec2 trilinearLightUv(vec3 w, vec4 c01, vec4 c23, vec4 c45, vec4 c67) {
    vec2 c0 = c01.xy;
    vec2 c1 = c01.zw;
    vec2 c2 = c23.xy;
    vec2 c3 = c23.zw;
    vec2 c4 = c45.xy;
    vec2 c5 = c45.zw;
    vec2 c6 = c67.xy;
    vec2 c7 = c67.zw;

    vec2 x00 = mix(c0, c1, w.x);
    vec2 x10 = mix(c2, c3, w.x);
    vec2 x01 = mix(c4, c5, w.x);
    vec2 x11 = mix(c6, c7, w.x);
    vec2 y0  = mix(x00, x10, w.y);
    vec2 y1  = mix(x01, x11, w.y);
    return mix(y0, y1, w.z);
}

#ifdef USE_SLICED_LIGHT
vec2 bilinearLightUv(vec2 wxz, vec4 c01, vec4 c23) {
    vec2 c00 = c01.xy;
    vec2 c10 = c01.zw;
    vec2 c01v = c23.xy;
    vec2 c11 = c23.zw;

    vec2 x0 = mix(c00, c10, wxz.x);
    vec2 x1 = mix(c01v, c11, wxz.x);
    return mix(x0, x1, wxz.y);
}
#endif

void main() {
    mat4 modelView;
    vec3 bboxMin;
    vec3 bboxSize;
    vec4 lc01;
    vec4 lc23;
    vec4 lc45;
    vec4 lc67;

#ifdef USE_INSTANCING
    mat4 rotMatrix = quatToMat4(InstRot);
    mat4 translation = mat4(1.0);
    translation[3] = vec4(InstPos, 1.0);

    modelView = translation * rotMatrix;
    bboxMin = InstBboxMin;
    bboxSize = InstBboxSize;
#ifndef USE_SLICED_LIGHT
    lc01 = InstLightC01;
    lc23 = InstLightC23;
    lc45 = InstLightC45;
    lc67 = InstLightC67;
#endif

    fragNormal = mat3(rotMatrix) * Normal;
#else
    modelView = ModelViewMat;
    bboxMin = BboxMin;
    bboxSize = BboxSize;
#ifndef USE_SLICED_LIGHT
    lc01 = LightC01;
    lc23 = LightC23;
    lc45 = LightC45;
    lc67 = LightC67;
#endif

    fragNormal = mat3(modelView) * Normal;
#endif

    // Safeguard: when bboxSize has a zero axis the division below would NaN the
    // whole vertex. Clamp to a tiny epsilon per-axis so degenerate meshes still
    // render (with a uniform brightness collapsing all corners to one value).
    vec3 safeSize = max(bboxSize, vec3(1e-4));
    vec3 w = clamp((Position - bboxMin) / safeSize, 0.0, 1.0);

    vec2 uvLm;
#ifdef USE_SLICED_LIGHT
    float ty = w.y * 3.0;
    float s0 = floor(ty);
    float fy = clamp(ty - s0, 0.0, 1.0);
    int i0 = int(clamp(s0, 0.0, 3.0));
    int i1 = min(i0 + 1, 3);

    vec2 uv0;
    vec2 uv1;

#ifdef USE_INSTANCING
    if (i0 == 0) uv0 = bilinearLightUv(vec2(w.x, w.z), InstLightS0C01, InstLightS0C23);
    else if (i0 == 1) uv0 = bilinearLightUv(vec2(w.x, w.z), InstLightS1C01, InstLightS1C23);
    else if (i0 == 2) uv0 = bilinearLightUv(vec2(w.x, w.z), InstLightS2C01, InstLightS2C23);
    else uv0 = bilinearLightUv(vec2(w.x, w.z), InstLightS3C01, InstLightS3C23);

    if (i1 == 0) uv1 = bilinearLightUv(vec2(w.x, w.z), InstLightS0C01, InstLightS0C23);
    else if (i1 == 1) uv1 = bilinearLightUv(vec2(w.x, w.z), InstLightS1C01, InstLightS1C23);
    else if (i1 == 2) uv1 = bilinearLightUv(vec2(w.x, w.z), InstLightS2C01, InstLightS2C23);
    else uv1 = bilinearLightUv(vec2(w.x, w.z), InstLightS3C01, InstLightS3C23);
#else
    if (i0 == 0) uv0 = bilinearLightUv(vec2(w.x, w.z), LightS0C01, LightS0C23);
    else if (i0 == 1) uv0 = bilinearLightUv(vec2(w.x, w.z), LightS1C01, LightS1C23);
    else if (i0 == 2) uv0 = bilinearLightUv(vec2(w.x, w.z), LightS2C01, LightS2C23);
    else uv0 = bilinearLightUv(vec2(w.x, w.z), LightS3C01, LightS3C23);

    if (i1 == 0) uv1 = bilinearLightUv(vec2(w.x, w.z), LightS0C01, LightS0C23);
    else if (i1 == 1) uv1 = bilinearLightUv(vec2(w.x, w.z), LightS1C01, LightS1C23);
    else if (i1 == 2) uv1 = bilinearLightUv(vec2(w.x, w.z), LightS2C01, LightS2C23);
    else uv1 = bilinearLightUv(vec2(w.x, w.z), LightS3C01, LightS3C23);
#endif

    uvLm = mix(uv0, uv1, fy);
#else
    uvLm = trilinearLightUv(w, lc01, lc23, lc45, lc67);
#endif

    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    texCoord = UV0;
    // Center within the 16×16 lightmap cell like vanilla block UV2 → texcoord.
    lightmapUV = (uvLm + vec2(8.0)) / 256.0;
    vertexDistance = length(viewPos.xyz);

#ifdef USE_INSTANCING
    vFadeAlpha = InstFadeAlpha;
#else
    vFadeAlpha = FadeAlpha;
#endif
}
