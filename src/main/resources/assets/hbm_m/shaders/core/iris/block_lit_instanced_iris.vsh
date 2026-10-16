#version 330 core
// Iris ExtendedShader variant of block_lit_instanced: pure instanced path
// (BoneId layout), uniforms use the iris_ prefix: ExtendedShader.getUniform()
// resolves vanilla names via "iris_" + name (see Iris pipeline/programs/ExtendedShader).
// Output conventions are vanilla parity: a single fragColor in the FSH
// (like the iris-fallback ShaderSynthesizer).

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;
// int bone_id: reserved for merged meshes (CPU pose already in InstPos/InstRot).
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
// Sprite-rect remap (identity 0,0,1,1 = passthrough), see block_lit_instanced.vsh.
layout(location = 12) in vec4 InstUvRect;
// Per-instance RGBA tint (heat/glow of parts; RGB may exceed 1 = overbright).
layout(location = 13) in vec4 InstColor;
// Spatial tint falloff in model coordinates: x = axis (0/1/2, <0 = off),
// y = coordinate of full tint, z = coordinate of zero (smoothstep in between).
layout(location = 14) in vec4 GradParams;
// Parametric GPU animation (mirror of block_lit_instanced.vsh). Not yet active
// on Iris paths (CPU animator, see MachineBer.isParametricPath); the attribute
// is declared to keep the VAO layout consistent (16 attributes, divisor 1).
layout(location = 15) in vec4 AnimParams;

uniform mat4 iris_ModelViewMat;
uniform mat4 iris_ProjMat;
// Pack-specific shadow distortion (shadow pass only): the pack applies a
// "fisheye" distortion to its shadow map around the player in its shadow vsh
// and compensates when sampling. Without applying the identical distortion,
// our content samples the wrong texels and shadows "crawl" behind the player.
// Mode and constants are extracted from the pack's shadow program source
// (IrisShadowDistortion). 0 = no distortion.
uniform float iris_hbmShadowDistortionMode; // 0 none, 1 quartic (Photon), 2 linear (BSL/classic)
uniform float iris_hbmShadowDistortion;
uniform float iris_hbmShadowDepthScale;

out vec2 texCoord;
out vec2 lightmapUV;
out float vertexDistance;
out vec3 fragNormal;
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
    // via iris_ModelViewMat (R_cam*T(-cam)); in the shadow pass, shadow MODELVIEW.
    mat4 instBase = translation * rotMatrix;
    mat4 modelView = iris_ModelViewMat * instBase;

    fragNormal = mat3(instBase) * Normal;
    worldNormal = mat3(rotMatrix) * Normal;

    // Safeguard: a zero bbox axis would produce NaN for the whole vertex.
    vec3 safeSize = max(InstBboxSize.xyz, vec3(1e-4));
    vec3 w = clamp((Position - InstBboxMin) / safeSize, 0.0, 1.0);

    vec2 uvLm = trilinearLightUv(w, InstLightC01, InstLightC23, InstLightC45, InstLightC67);

    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = iris_ProjMat * viewPos;

    // Spatial tint falloff + emission: gt is the gradient weight (1 at the
    // source, 0 at the far end, smoothstep); vColor = mix(white, tint, gt);
    // the lightmap is pushed toward fullbright by InstColor.a (heat) * gt, so
    // the glow follows the tint across the whole texture (hot metal), see
    // MachineSpecBuilder.tintOverride.
    float glow = 0.0;
    vColor = InstColor;
    if (GradParams.x >= 0.0) {
        float gCoord = GradParams.x < 0.5 ? Position.x : (GradParams.x < 1.5 ? Position.y : Position.z);
        float gt = clamp((gCoord - GradParams.z) / (GradParams.y - GradParams.z), 0.0, 1.0);
        gt = gt * gt * (3.0 - 2.0 * gt);
        vColor = mix(vec4(1.0), InstColor, gt);
        glow = gt;
    }
    uvLm = mix(uvLm, vec2(240.0), clamp(InstColor.a * glow, 0.0, 1.0));

    if (iris_hbmShadowDistortionMode > 0.5) {
        float d = iris_hbmShadowDistortion;
        // quartic_length (Photon): sqrt(sqrt(x^4+y^4)) via multiplications;
        // pow(x, 4.0) with negative x is undefined in GLSL.
        vec2 p = gl_Position.xy;
        vec2 p2 = p * p;
        float factor = iris_hbmShadowDistortionMode > 1.5
            ? length(p) * d + (1.0 - d)
            : sqrt(sqrt(p2.x * p2.x + p2.y * p2.y)) * d + (1.0 - d);
        gl_Position = vec4(p / factor, gl_Position.z * iris_hbmShadowDepthScale, 1.0);
    }

    texCoord = InstUvRect.xy + UV0 * InstUvRect.zw;
    // Center of the 16x16 lightmap cell, like the vanilla UV2 -> texcoord block.
    lightmapUV = (uvLm + vec2(8.0)) / 256.0;
    vertexDistance = length(viewPos.xyz);
    vFadeAlpha = InstBboxSize.w;
}
