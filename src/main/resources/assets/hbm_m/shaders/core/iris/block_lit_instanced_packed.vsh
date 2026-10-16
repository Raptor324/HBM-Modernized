#version 330 core
// Iris ExtendedShader-вариант block_lit_instanced для deferred-паков со схемой
// gbuffer "packed": gbuffer-таргет пакуется парами 8-битных каналов
// (pack_unorm_2x8: albedo.rg / albedo.b+mask / oct-encoded normal / light
// levels), нормали в мировых осях, свет считается в deferred-композите.
// Отличие от block_lit_instanced_iris.vsh: добавлен out lightLevels (0..1),
// который FSH упаковывает в формат gbuffer пака. worldNormal уже здесь —
// схема "packed" ждёт flat normal в мировых осях.

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;
// int bone_id: резерв под merged mesh (CPU pose уже в InstPos/InstRot).
layout(location = 3) in int BoneId;
layout(location = 4) in vec3 InstPos;
layout(location = 5) in vec4 InstRot;
layout(location = 6) in vec3 InstBboxMin;
// xyz = bbox extent; w = per-instance fade.
layout(location = 7) in vec4 InstBboxSize;
layout(location = 8)  in vec4 InstLightC01;  // corner0.uv, corner1.uv
layout(location = 9)  in vec4 InstLightC23;
layout(location = 10) in vec4 InstLightC45;
layout(location = 11) in vec4 InstLightC67;

uniform mat4 iris_ModelViewMat;
uniform mat4 iris_ProjMat;

out vec2 texCoord;
out vec2 lightmapUV;
out vec2 lightLevels;
out float vertexDistance;
out vec3 fragNormal;
out vec3 worldNormal;
out float vFadeAlpha;

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
    mat4 rotMatrix = quatToMat4(InstRot);
    mat4 translation = mat4(1.0);
    translation[3] = vec4(InstPos, 1.0);
    // InstPos/InstRot — мировые координаты (FrameViewState): камера приходит
    // через iris_ModelViewMat (R_cam*T(-cam)).
    mat4 instBase = translation * rotMatrix;
    mat4 modelView = iris_ModelViewMat * instBase;

    fragNormal = mat3(instBase) * Normal;
    worldNormal = mat3(rotMatrix) * Normal;

    // Safeguard: нулевая ось bbox дала бы NaN на всей вершине.
    vec3 safeSize = max(InstBboxSize.xyz, vec3(1e-4));
    vec3 w = clamp((Position - InstBboxMin) / safeSize, 0.0, 1.0);

    vec2 uvLm = trilinearLightUv(w, InstLightC01, InstLightC23, InstLightC45, InstLightC67);

    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = iris_ProjMat * viewPos;

    texCoord = UV0;
    // Центр 16x16 ячейки lightmap — как ванильный блок UV2 -> texcoord.
    lightmapUV = (uvLm + vec2(8.0)) / 256.0;
    // Сырые уровни света 0..1 — их пакует FSH в формат gbuffer пака.
    lightLevels = uvLm * (1.0 / 240.0);
    vertexDistance = length(viewPos.xyz);
    vFadeAlpha = InstBboxSize.w;
}
