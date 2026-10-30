#version 330 core
// Iris ExtendedShader variant of block_lit_instanced for deferred shader packs
// with the "packed" gbuffer scheme: gbuffer targets are packed in pairs of
// 8-bit channels (pack_unorm_2x8: albedo.rg / albedo.b+mask / oct-encoded
// normal / light levels), normals in world axes, lighting computed in the
// deferred composite.
// Real differences from block_lit_instanced_iris.vsh (this program runs ONLY
// in the Iris main pass, Tier-2 direct instancing: IrisInstancedBatchRenderer
// gated on IrisInstancedEncoders.hasEncoder() = the PACKED_SCHEME signature;
// the iris variant is the shadow-pass program):
// - adds out lightLevels (0..1): the FSH packs it into the pack's gbuffer
//   format. worldNormal is already in world axes here: the "packed" scheme
//   expects the flat normal in world axes.
// - no AnimParams / uJointSpecs / joint block: parametric records never exist
//   under Iris (MachineBer.isParametricPath is false while an external shader
//   is active), so jointed parts keep the CPU-animator pose in InstPos/InstRot.
// - no fade-ramp uniforms / gpuFadeAlpha / clip-out: vFadeAlpha = InstBboxSize.w
//   forwards the raw fade MODE (1.0 / 0.9999 / 0.9998) as near-opaque alpha,
//   consumed only by the FSH's discard threshold. Distance fade is a no-op on
//   the whole Iris path (the Tier-1 compute bake bakes all fade modes opaque
//   too): machines pop at the cutoff instead of dissolving - shared, by design.
// - no shadow-distortion uniforms: the shadow pass never selects this program.

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;
// int bone_id: reserved for merged meshes (CPU pose already in InstPos/InstRot).
layout(location = 3) in int BoneId;
layout(location = 4) in vec3 InstPos;
// Packed quat: 4 snorm16 halves in 2 record words, read as GL_SHORT normalized
// ([-1,1] floats - no shader decode needed; CPU encoder packSnorm16).
layout(location = 5) in vec4 InstRot;
layout(location = 6) in vec3 InstBboxMin;
// xyz = bbox extent; w = per-instance fade.
layout(location = 7) in vec4 InstBboxSize;
// Corner light, PACKED u8 (normalized 0..1; decode to the 0..240 texel grid by x240):
layout(location = 8)  in vec4 InstLightC01;  // corner0.u, corner0.v, corner1.u, corner1.v
layout(location = 9)  in vec4 InstLightC23;
layout(location = 10) in vec4 InstLightC45;
layout(location = 11) in vec4 InstLightC67;
// Sprite-rect remap (identity 0,0,1,1 = passthrough), see block_lit_instanced.vsh.
layout(location = 12) in vec4 InstUvRect;
// Per-instance RGBA tint (heat/glow of parts; RGB may exceed 1 = overbright).
layout(location = 13) in vec4 InstColor;
// Spatial tint falloff in model coordinates: x = axis (0/1/2, <0 = off),
// y = coordinate of full tint, z = coordinate of zero (smoothstep in between).
layout(location = 14) in vec4 GradParams;

uniform mat4 iris_ModelViewMat;
uniform mat4 iris_ProjMat;

out vec2 texCoord;
out vec2 lightLevels;
out vec3 worldNormal;
out float vFadeAlpha;
out vec4 vColor;

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
    // InstPos/InstRot are world coordinates (FrameViewState): the camera comes
    // via iris_ModelViewMat (R_cam*T(-cam)).
    mat4 instBase = translation * rotMatrix;
    mat4 modelView = iris_ModelViewMat * instBase;

    worldNormal = mat3(rotMatrix) * Normal;

    // Safeguard: a zero bbox axis would produce NaN for the whole vertex.
    vec3 safeSize = max(InstBboxSize.xyz, vec3(1e-4));
    vec3 w = clamp((Position - InstBboxMin) / safeSize, 0.0, 1.0);

    // Corner light: packed u8, normalized fetch decodes to the 0..240 texel grid
    // by x240 (byte x 240/255; CPU encoder InstancedStaticPartRenderer.packLightU8).
    vec2 uvLm = trilinearLightUv(w, InstLightC01 * 240.0, InstLightC23 * 240.0,
                                 InstLightC45 * 240.0, InstLightC67 * 240.0);

    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = iris_ProjMat * viewPos;

    // Spatial tint falloff + emission: gt is the gradient weight (1 at the
    // source, 0 at the far end, smoothstep); vColor = mix(white, tint, gt);
    // the lightmap is pushed toward fullbright by InstColor.a (heat) * gt, so
    // the glow follows the tint across the whole texture (hot metal), see
    // MachineSpecBuilder.tintOverride.
    // Init 1.0: no gradient (GradParams.x < 0) means full-weight emission, so
    // uniformly heated parts keep their InstColor.a glow (matches
    // block_lit_instanced.vsh).
    float glow = 1.0;
    vColor = InstColor;
    // Fail-closed gate: axis must be finite and in [0, 2] (NaN fails every
    // comparison, so a zeroed/corrupt word disables the gradient entirely,
    // never divides by zero -> +Inf -> full-tint).
    if (GradParams.x >= 0.0 && GradParams.x <= 2.0) {
        float gCoord = Position.z;
        if (GradParams.x < 0.5) {
            gCoord = Position.x;
        } else if (GradParams.x < 1.5) {
            gCoord = Position.y;
        }
        float denom = GradParams.y - GradParams.z;
        // Epsilon guard: zero-length gradient span must not produce Inf/NaN.
        float gt = clamp((gCoord - GradParams.z) / (abs(denom) < 1e-4 ? 1e-4 : denom), 0.0, 1.0);
        gt = gt * gt * (3.0 - 2.0 * gt);
        vColor = mix(vec4(1.0), InstColor, gt);
        glow = gt;
    }
    uvLm = mix(uvLm, vec2(240.0), clamp(InstColor.a * glow, 0.0, 1.0));

    texCoord = InstUvRect.xy + UV0 * InstUvRect.zw;
    // Raw light levels 0..1, packed by the FSH into the pack's gbuffer format.
    lightLevels = uvLm * (1.0 / 240.0);
    vFadeAlpha = InstBboxSize.w;
}
