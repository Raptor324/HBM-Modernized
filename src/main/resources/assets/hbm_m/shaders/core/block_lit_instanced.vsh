#version 330 core
// Dedicated instanced source: defines are set by default right here (and duplicated
// by the injection from ClientSetup.ShaderPreDefinitions). Registration used to
// redirect this name to block_lit.vsh, so the InstUvRect/InstColor/GradParams
// attributes were compiled out (on Forge: no part tint + silently disabled door skin sharing).
#ifndef USE_INSTANCING
#define USE_INSTANCING
#endif
#ifndef USE_VERTEX_BONE_ID
#define USE_VERTEX_BONE_ID
#endif

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

#ifdef USE_INSTANCING
// int bone_id: reserved for merged mesh / hierarchy documentation (see OLD/render.md).
// Full part pose comes via InstPos/InstRot (CPU); no UBO/SSBO in the VS - Oculus/Iris compatibility.
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
// Sprite-rect remap: vertex UV from sprite-local [0..1] to the atlas (u0 + uv*du).
// Identity (0,0,1,1) for machines with atlas UVs - plain passthrough.
layout(location = 12) in vec4 InstUvRect;
// Per-instance RGBA tint (part heat/glow; RGB may exceed 1 - overbright).
// White = passthrough; the pointer is set up in InstancedStaticPartRenderer (VAO, divisor 1).
layout(location = 13) in vec4 InstColor;
// Spatial tint falloff in model coordinates: x = axis (0/1/2, <0 = off),
// y = coordinate of full tint, z = coordinate of zero (smooth smoothstep between them).
layout(location = 14) in vec4 GradParams;
// Parametric GPU animation (MachineSpecBuilder.parametricPart): xyz = joint
// parameters (angle in degrees / translation distance), w = joint index into the global
// RGBA32F texture NucleusJointSpecs (2 texels: kind+axis, pivot). w < 0 = part without
// parametrics - no delta applied, the CPU record stays static.
layout(location = 15) in vec4 AnimParams;
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
// Parametric joint specifications (RGBA32F, 2 texels per joint, texelFetch
// by AnimParams.w): bound by NucleusJointSpecs.bindSampler to unit 3.
uniform sampler2D uJointSpecs;

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
// World normal (instance/model rotation without view matrix) for directional shading.
out vec3 worldNormal;
// Per-vertex fade: InstBboxSize.w when instancing (batched flush reads stale uniform otherwise).
out float vFadeAlpha;
// Per-instance part tint (see MachineSpecBuilder.tintOverride).
out vec4 vColor;

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
    vec3 vPos = Position;
    vec3 vNrm = Normal;

#ifdef USE_INSTANCING
#ifdef USE_VERTEX_BONE_ID
    // Parametric GPU animation: joint delta in LOCAL part coordinates,
    // BEFORE the world record (InstPos/InstRot). The CPU does not rebuild the
    // record - the span diff sees only 4 parameter floats (a frozen part costs
    // zero upload). Rotation is Rodrigues' formula around the normalized axis.
    if (AnimParams.w >= 0.0) {
        vec4 specA = texelFetch(uJointSpecs, ivec2(int(AnimParams.w) * 2, 0), 0);
        vec4 specB = texelFetch(uJointSpecs, ivec2(int(AnimParams.w) * 2 + 1, 0), 0);
        float kind = specA.x;
        vec3 axis = specA.yzw;
        vec3 pivot = specB.xyz;
        if (kind < 0.5) {
            float ang = radians(AnimParams.x);
            float c = cos(ang);
            float s = sin(ang);
            vec3 p = vPos - pivot;
            vPos = pivot + p * c + cross(axis, p) * s + axis * dot(axis, p) * (1.0 - c);
            vNrm = vNrm * c + cross(axis, vNrm) * s + axis * dot(axis, vNrm) * (1.0 - c);
        } else {
            vPos += axis * AnimParams.x;
        }
    }
#endif
#endif

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
    // InstPos/InstRot are WORLD coordinates (FrameViewState): camera movement
    // does not change the records (span diff yields zero upload for a static scene).
    // Camera (R_cam*T(-cam)) comes via ModelViewMat, so viewPos stays view-space;
    // fog/depth are unaffected.
    mat4 instBase = translation * rotMatrix;
    modelView = ModelViewMat * instBase;
    worldRot = rotMatrix;
    bboxMin = InstBboxMin;
    bboxSize = InstBboxSize.xyz;
    lc01 = InstLightC01;
    lc23 = InstLightC23;
    lc45 = InstLightC45;
    lc67 = InstLightC67;

    // Part world normal (no view rotation) - for shading; fragNormal keeps
    // its previous semantics (instance rotation, camera-independent).
    fragNormal = mat3(instBase) * vNrm;
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

    // World normal: instance rotation only (or identity for the non-instanced path),
    // no view matrix - shading is camera-rotation independent.
    worldNormal = mat3(worldRot) * vNrm;

    // Safeguard: when bboxSize has a zero axis the division below would NaN the
    // whole vertex. Clamp to a tiny epsilon per-axis so degenerate meshes still
    // render (with a uniform brightness collapsing all corners to one value).
    vec3 safeSize = max(bboxSize, vec3(1e-4));
    vec3 w = clamp((vPos - bboxMin) / safeSize, 0.0, 1.0);

    vec2 uvLm = trilinearLightUv(w, lc01, lc23, lc45, lc67);

    vec4 viewPos = modelView * vec4(vPos, 1.0);
    gl_Position = ProjMat * viewPos;

    // Spatial tint falloff + emission: gt = gradient weight (1 at the source,
    // 0 at the far end, smoothstep); vColor = mix(white, tint, gt); the lightmap
    // is pushed toward fullbright by InstColor.a (heat) * gt - the glow follows
    // the tint across the whole texture (hot metal), see MachineSpecBuilder.tintOverride.
    float glow = 0.0;
    vColor = InstColor;
    if (GradParams.x >= 0.0) {
        float gCoord = GradParams.x < 0.5 ? vPos.x : (GradParams.x < 1.5 ? vPos.y : vPos.z);
        float gt = clamp((gCoord - GradParams.z) / (GradParams.y - GradParams.z), 0.0, 1.0);
        gt = gt * gt * (3.0 - 2.0 * gt);
        vColor = mix(vec4(1.0), InstColor, gt);
        glow = gt;
    }
    uvLm = mix(uvLm, vec2(240.0), clamp(InstColor.a * glow, 0.0, 1.0));

#ifdef USE_VERTEX_BONE_ID
    texCoord = InstUvRect.xy + UV0 * InstUvRect.zw;
#else
    texCoord = UV0;
#endif
    // Center within the 16x16 lightmap cell like vanilla block UV2 -> texcoord.
    lightmapUV = (uvLm + vec2(8.0)) / 256.0;
    vertexDistance = length(viewPos.xyz);

#ifdef USE_INSTANCING
    vFadeAlpha = InstBboxSize.w;
#else
    vFadeAlpha = FadeAlpha;
#endif
}
