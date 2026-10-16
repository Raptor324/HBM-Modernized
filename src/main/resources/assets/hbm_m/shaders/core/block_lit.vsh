#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

#ifdef USE_INSTANCING
// int bone_id: резерв под merged mesh / документация иерархии (см. OLD/render.md).
// Полный pose части задаётся в InstPos/InstRot (CPU), UBO/SSBO в VS не используем — совместимость с Oculus/Iris.
#ifdef USE_VERTEX_BONE_ID
layout(location = 3) in int BoneId;
layout(location = 4) in vec3 InstPos;
layout(location = 5) in vec4 InstRot;
layout(location = 6) in vec3 InstBboxMin;
// xyz = bbox extent; w = per-instance fade (keeps attrib count <= 16 with BoneId).
layout(location = 7) in vec4 InstBboxSize;
layout(location = 8)  in vec4 InstLightC01;  // corner0.uv, corner1.uv
layout(location = 9)  in vec4 InstLightC23;
layout(location = 10) in vec4 InstLightC45;
layout(location = 11) in vec4 InstLightC67;
#else
layout(location = 3)  in vec3 InstPos;
layout(location = 4)  in vec4 InstRot;
layout(location = 5)  in vec3 InstBboxMin;
layout(location = 6)  in vec3 InstBboxSize;
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
uniform vec4 LightC01;
uniform vec4 LightC23;
uniform vec4 LightC45;
uniform vec4 LightC67;

out vec2 texCoord;
// Vanilla lightmap UV: sampled from Sampler2 in block_lit.fsh.
out vec2 lightmapUV;
out float vertexDistance;
out vec3 fragNormal;
// Мировая нормаль (поворот инстанса/модели без view-матрицы) для направленного затенения.
out vec3 worldNormal;
// Per-vertex fade: InstBboxSize.w when instancing (batched flush reads stale uniform otherwise).
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

void main() {
    mat4 modelView;
    mat4 worldRot = mat4(1.0);
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
    mat4 instBase = translation * rotMatrix;
    modelView = instBase;
    worldRot = rotMatrix;
    bboxMin = InstBboxMin;
    bboxSize = InstBboxSize.xyz;
    lc01 = InstLightC01;
    lc23 = InstLightC23;
    lc45 = InstLightC45;
    lc67 = InstLightC67;

    fragNormal = mat3(modelView) * Normal;
#else
    modelView = ModelViewMat;
    bboxMin = BboxMin;
    bboxSize = BboxSize;
    lc01 = LightC01;
    lc23 = LightC23;
    lc45 = LightC45;
    lc67 = LightC67;

    fragNormal = mat3(modelView) * Normal;
#endif

    // Мировая нормаль: только поворот инстанса (или identity для не-instanced пути),
    // без view-матрицы — затенение не зависит от поворота камеры.
    worldNormal = mat3(worldRot) * Normal;

    // Safeguard: when bboxSize has a zero axis the division below would NaN the
    // whole vertex. Clamp to a tiny epsilon per-axis so degenerate meshes still
    // render (with a uniform brightness collapsing all corners to one value).
    vec3 safeSize = max(bboxSize, vec3(1e-4));
    vec3 w = clamp((Position - bboxMin) / safeSize, 0.0, 1.0);

    vec2 uvLm = trilinearLightUv(w, lc01, lc23, lc45, lc67);

    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    texCoord = UV0;
    // Center within the 16×16 lightmap cell like vanilla block UV2 → texcoord.
    lightmapUV = (uvLm + vec2(8.0)) / 256.0;
    vertexDistance = length(viewPos.xyz);

#ifdef USE_INSTANCING
    vFadeAlpha = InstBboxSize.w;
#else
    vFadeAlpha = FadeAlpha;
#endif
}
