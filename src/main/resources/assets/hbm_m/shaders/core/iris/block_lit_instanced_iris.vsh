#version 330 core
// Iris ExtendedShader variant of block_lit_instanced: pure instanced path
// (BoneId layout), uniforms use the iris_ prefix: ExtendedShader.getUniform()
// resolves vanilla names via "iris_" + name (see Iris pipeline/programs/ExtendedShader).
// Output conventions are vanilla parity: a single fragColor in the FSH
// (like the iris-fallback ShaderSynthesizer).
//
// @credit Spark Profiler Remediation / Rodrigues GPU Animation Parity

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
// w = parametric joint CHAIN base index into uJointSpecs (<0 = no parametrics).
layout(location = 14) in vec4 GradParams;
// Parametric GPU animation (mirror of block_lit_instanced.vsh): the per-frame
// parameters of the joint chain PACKED as up to 8 binary16 words (2 per float,
// low half first; decoded by halfDecode - unpackHalf2x16 is GLSL 4.20 core,
// not available on #version 330 - encoded CPU-side by HalfFloat) -
// one word per rotate/axis-translate parameter (rotate = angle in degrees
// wrapped to (-180, 180], axis translate = the distance in blocks), three
// words per vector translate, all in node DECLARATION order. v2 layout, per
// RIG: a tracked rig (any node trackKind != 0) parks the motion clock in the
// first two floats and streams only the untracked nodes' words from slot 4
// (tracked nodes sample the keyframe pool - trackedValue below). The
// chain base index into uJointSpecs is GradParams.w (<0 = no parametrics).
// This vsh is selected for the Iris SHADOW pass only (IrisInstancedEncoders
// .shaderName(true)); the main pass uses the packed program, and the Tier-1
// compute bake reads the record's pos/quat/bbox/light words only. Parametric
// records never reach it either way: MachineBer.isParametricPath is false
// while an external shader is active, and shadow records force GradParams.w
// = -1 (IrisShadowBatchCollector), so the joint block below stays dormant -
// it exists to mirror block_lit_instanced.vsh. The attribute is declared to
// keep the VAO layout consistent (16 attributes, divisor 1).
layout(location = 15) in vec4 AnimParams;

uniform sampler2D iris_uJointSpecs;
uniform mat4 iris_ModelViewMat;
uniform mat4 iris_ProjMat;
// Tracked-rig animation clock (mirror of block_lit_instanced.vsh): whole ticks
// + fractional tick, split because a vec2 uniform NPEs the vanilla-style JSON
// parser (see the vanilla vsh comment).
uniform float iris_uAnimTicks;
uniform float iris_uPartialTick;

// GPU distance fade uniforms (Iris ExtendedShader prefix: iris_)
uniform float iris_uStaticFadeStart;
uniform float iris_uStaticFadeEnd;
uniform float iris_uAnimFadeStart;
uniform float iris_uAnimFadeEnd;

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
// Extra varyings kept from the retired general MRT encoder (disabled - see
// IrisInstancedEncoders): raw light levels and the camera-space normal. The
// shipped FSHs never declare them, which is legal - an unconsumed vertex
// output is discarded.
out vec2 lightLevels;
out vec3 viewNormal;

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

// binary16 -> float, pure GLSL 330 (see block_lit_instanced.vsh: unpackHalf2x16
// is GLSL 4.20 core / ARB_gpu_shader5, NOT available on #version 330 without
// the extension directive - the decode is done by hand instead).
// Packed AnimParams word fetch: word w of up to 8 binary16 joint parameters
// (2 per record float, low half first - see HalfFloat, the CPU encoder).
float halfDecode(uint h) {
    uint s = (h & 0x8000u) << 16u;
    uint e = (h >> 10u) & 0x1Fu;
    uint m = h & 0x3FFu;
    if (e == 0u) {
        float val = float(m) * 5.9604645e-8;
        if ((h & 0x8000u) != 0u) {
            return -val;
        }
        return val;
    }
    if (e == 31u) {
        return uintBitsToFloat(s | 0x7F800000u | (m << 13u));
    }
    return uintBitsToFloat(s | ((e + 112u) << 23u) | (m << 13u));
}

float animWord(int w) {
    if (w < 0 || w >= 8) return 0.0;
    uint u = floatBitsToUint(AnimParams[w >> 1]);
    uint halfBits = ((w & 1) == 0) ? (u & 0xFFFFu) : (u >> 16u);
    return halfDecode(halfBits);
}

// Keyframe pool sample: pool texels follow the joint region, one per keyframe =
// (t01, value, 0, 0), t01 ascending within a track; ptr is the ABSOLUTE texel
// index of the track's first keyframe, len the keyframe count (>= 2). Bounded
// texelFetch loop ending at the segment t_i <= u <= t_i+1 (linear mix); u
// outside the key range pins to the first/last keyframe's value.
// (v2 joint-spec layout mirror of block_lit_instanced.vsh.)
float trackSample(int ptr, int len, float u) {
    if (len <= 0) return 0.0;
    if (len == 1) return texelFetch(iris_uJointSpecs, ivec2(ptr, 0), 0).y;
    vec4 first = texelFetch(iris_uJointSpecs, ivec2(ptr, 0), 0);
    vec4 last = texelFetch(iris_uJointSpecs, ivec2(ptr + len - 1, 0), 0);
    if (u <= first.x) return first.y;
    if (u >= last.x) return last.y;
    vec4 prev = first;
    for (int i = 1; i < len; i++) {
        vec4 cur = texelFetch(iris_uJointSpecs, ivec2(ptr + i, 0), 0);
        if (u <= cur.x) {
            float span = cur.x - prev.x;
            float factor = span > 0.0 ? (u - prev.x) / span : 0.0;
            return mix(prev.y, cur.y, factor);
        }
        prev = cur;
    }
    return last.y;
}

// Tracked-node parameter (texel C = rateScale/flags/trackPtr/trackLen):
// u advances from (t0 = AnimParams.x, u0 = low half of AnimParams.y) by the
// clock; durationTicks = high half, decoded 0 means FROZEN (u stays u0).
// flags bit0 selects the wrap mode: 1 = cycle -> fract(u), 0 = clamp.
// flags bit1 selects Hermite reciprocating stroke spline evaluation.
// (Mirror of block_lit_instanced.vsh.trackedValue.)
float trackedValue(vec4 specC) {
    // "packed" is a GLSL keyword (block layout qualifier) - the local is "raw".
    uint raw = floatBitsToUint(AnimParams.y);
    float u0 = halfDecode(raw & 0xFFFFu);
    float dur = halfDecode(raw >> 16u);
    float u = u0;
    if (dur != 0.0) {
        // specC.x carries the track's rateScale (frequency multiplier): the clock
        // advances the curve position rateScale times faster/slower than ticks.
        u += (((iris_uAnimTicks - AnimParams.x) + iris_uPartialTick) / dur) * specC.x;
    }
    if ((int(specC.y) & 1) == 1) {
        u = fract(u);
    } else {
        u = clamp(u, 0.0, 1.0);
    }
    if ((int(specC.y) & 2) != 0) {
        float stroke = u < 0.5 ? (4.0 * u * u * (3.0 - 4.0 * u))
                               : (4.0 * (1.0 - u) * (1.0 - u) * (3.0 - 4.0 * (1.0 - u)));
        return trackSample(int(specC.z), int(specC.w), stroke);
    }
    return trackSample(int(specC.z), int(specC.w), u);
}

float fadeRamp(float dist, float start, float end) {
    return clamp(1.0 - (dist - start) / (end - start), 0.0, 1.0);
}

float gpuFadeAlpha(float modeW, vec3 instPos) {
    if (modeW < 0.0) {
        return 0.0;
    }
    // FADE_MODE_NONE_DIVIDER = 0.99995 (режим FADE_MODE_NONE = 1.0)
    if (modeW > 0.99995) {
        return 1.0;
    }
    // Защита: если юниформы не настроены, остаемся полностью непрозрачными
    if (iris_uStaticFadeEnd <= iris_uStaticFadeStart || iris_uStaticFadeEnd <= 0.0) {
        return 1.0;
    }

    float dist = length((iris_ModelViewMat * vec4(instPos, 1.0)).xyz);
    float sf = fadeRamp(dist, iris_uStaticFadeStart, iris_uStaticFadeEnd);

    // FADE_MODE_MIN_ANIM_DIVIDER = 0.99985 (режим FADE_MODE_STATIC = 0.9999)
    if (modeW > 0.99985) {
        return sf;
    }

    // Режим FADE_MODE_MIN_ANIM = 0.9998
    if (iris_uAnimFadeEnd <= iris_uAnimFadeStart || iris_uAnimFadeEnd <= 0.0) {
        return sf;
    }
    float af = fadeRamp(dist, iris_uAnimFadeStart, iris_uAnimFadeEnd);
    return min(sf, af);
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
    vec3 vPos = Position;
    vec3 vNrm = Normal;
    // Joint CHAIN delta, identical to block_lit_instanced.vsh (see the full
    // comment there): GradParams.w = base node, node 0's pivot texel .w = node
    // count (0 = legacy single joint), params PACKED as binary16 words in
    // declaration order (decoded by animWord). PoseStack ops compose
    // M = N1*N2*...*Nk (ops declared first act on the vertex last), so node 0
    // is the leftmost matrix factor. Length-1 chains (all current rigs) are
    // bit-identical to the pre-chain single-joint block; longer chains compose
    // each node's rigid transform into one mat4 and apply it once - array-free
    // (the old dynamically indexed specA[8]/specB[8]/offs[8] locals pressed
    // per-vertex registers), mathematically identical, not bit-identical.
    if (GradParams.w >= 0.0) {
        int base = int(GradParams.w);
        float countW = texelFetch(iris_uJointSpecs, ivec2(base * 3 + 1, 0), 0).w;
        int n = 1;
        if (countW >= 0.5) {
            n = int(countW);
        }
        if (n > 8) n = 8; // shader bound; the CPU validates the same limit
        if (n == 1) {
            vec4 specA = texelFetch(iris_uJointSpecs, ivec2(base * 3, 0), 0);
            vec4 specB = texelFetch(iris_uJointSpecs, ivec2(base * 3 + 1, 0), 0);
            vec4 specC = texelFetch(iris_uJointSpecs, ivec2(base * 3 + 2, 0), 0);
            // With a single node the rig's trackedness IS this node's trackKind,
            // so a streamed word is always at slot 0 - exactly the pre-v2 fetch.
            // specC.x: rateScale (0.0 = untracked/streamed, >0 = tracked).
            bool tracked = specC.x > 0.0;
            vec3 axis = specA.yzw;
            vec3 pivot = specB.xyz;
            float jointVal = tracked ? trackedValue(specC) : animWord(0);
            if (specA.x < 0.5) {
                float ang = radians(jointVal);
                float c = cos(ang);
                float s = sin(ang);
                vec3 p = vPos - pivot;
                vPos = pivot + p * c + cross(axis, p) * s + axis * dot(axis, p) * (1.0 - c);
                vNrm = vNrm * c + cross(axis, vNrm) * s + axis * dot(axis, vNrm) * (1.0 - c);
            } else if (specA.x < 1.5) {
                // KIND_TRANSLATE consumes 3 consecutive words: a tracked
                // KIND_TRANSLATE is not a valid rig (CPU validation never emits
                // one) - render it unposed instead of inventing a direction.
                if (!tracked) vPos += vec3(animWord(0), animWord(1), animWord(2));
            } else {
                // KIND_TRANSLATE_AXIS: distance along the node's normalized axis.
                vPos += axis * jointVal;
            }
        } else {
            // Multi-node chain: single forward pass, sequential accumulation, no
            // local arrays. Post-multiplying keeps node 0 the leftmost factor
            // (it acts on the vertex last) - the exact FK product above.
            mat4 chain = mat4(1.0);
            // Streamed-word base: an untracked rig streams from slot 0, a tracked
            // rig from slot 4 (its words 0..3 carry t0/u0/duration). The layout
            // is per-RIG, so one bounded pass over texel C decides it up front.
            int wordBase = 0;
            for (int i = 0; i < n; i++) {
                if (texelFetch(iris_uJointSpecs, ivec2((base + i) * 3 + 2, 0), 0).x > 0.0) {
                    wordBase = 4;
                    break;
                }
            }
            int acc = wordBase;
            for (int i = 0; i < n; i++) {
                vec4 specA = texelFetch(iris_uJointSpecs, ivec2((base + i) * 3, 0), 0);
                vec4 specB = texelFetch(iris_uJointSpecs, ivec2((base + i) * 3 + 1, 0), 0);
                vec4 specC = texelFetch(iris_uJointSpecs, ivec2((base + i) * 3 + 2, 0), 0);
                // A tracked node is driven by its keyframe-pool track and
                // consumes NO streamed slot; streamed nodes read their words at
                // the running cursor (with wordBase = 0 this is bit-for-bit the
                // pre-v2 walk).
                // specC.x: rateScale (0.0 = untracked/streamed, >0 = tracked).
                bool tracked = specC.x > 0.0;
                int offs = acc;
                // rotate costs 1 word, vector translate 3, axis translate 1
                if (!tracked) {
                    if (specA.x < 0.5) {
                        acc += 1;
                    } else if (specA.x < 1.5) {
                        acc += 3;
                    } else {
                        acc += 1;
                    }
                }
                vec3 axis = specA.yzw;
                vec3 pivot = specB.xyz;
                mat4 node = mat4(1.0);
                float jointVal = tracked ? trackedValue(specC) : animWord(offs);
                if (specA.x < 0.5) {
                    float ang = radians(jointVal);
                    float c = cos(ang);
                    float s = sin(ang);
                    // Rodrigues rotation about the line (axis through pivot):
                    // R = c*I + (1-c)*axis*axis^T + s*[axis]x; the pivot goes
                    // into the translation column (pivot - R*pivot).
                    mat3 rot = c * mat3(1.0)
                             + (1.0 - c) * outerProduct(axis, axis)
                             + s * mat3( 0.0,     axis.z,  -axis.y,
                                        -axis.z,  0.0,      axis.x,
                                         axis.y, -axis.x,   0.0);
                    node = mat4(rot);
                    node[3].xyz = pivot - rot * pivot;
                } else if (specA.x < 1.5) {
                    // Tracked KIND_TRANSLATE is not a valid rig (see the single
                    // joint above) - the node stays at its rest pose.
                    if (!tracked) node[3].xyz = vec3(animWord(offs), animWord(offs + 1), animWord(offs + 2));
                } else {
                    node[3].xyz = axis * jointVal;
                }
                chain = chain * node;
            }
            vPos = (chain * vec4(vPos, 1.0)).xyz;
            vNrm = mat3(chain) * vNrm;
        }
    }
    mat4 rotMatrix = quatToMat4(InstRot);
    mat4 translation = mat4(1.0);
    translation[3] = vec4(InstPos, 1.0);
    // InstPos/InstRot are world coordinates (FrameViewState): the camera comes
    // via iris_ModelViewMat (R_cam*T(-cam)); in the shadow pass, shadow MODELVIEW.
    mat4 instBase = translation * rotMatrix;
    mat4 modelView = iris_ModelViewMat * instBase;

    fragNormal = mat3(instBase) * vNrm;
    worldNormal = mat3(rotMatrix) * vNrm;
    // Camera-space normal (same rotation the vertex transform applies, minus
    // translation): what most deferred composites expect in colortex1. In the
    // shadow pass this becomes light-space, but the shadow FSH never reads it.
    viewNormal = mat3(modelView) * vNrm;

    // Safeguard: a zero bbox axis would produce NaN for the whole vertex.
    vec3 safeSize = max(InstBboxSize.xyz, vec3(1e-4));
    vec3 w = clamp((vPos - InstBboxMin) / safeSize, 0.0, 1.0);

    // Corner light: packed u8, normalized fetch decodes to the 0..240 texel grid
    // by x240 (byte x 240/255; CPU encoder InstancedStaticPartRenderer.packLightU8).
    vec2 uvLm = trilinearLightUv(w, InstLightC01 * 240.0, InstLightC23 * 240.0,
                                 InstLightC45 * 240.0, InstLightC67 * 240.0);

    vec4 viewPos = modelView * vec4(vPos, 1.0);
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
        float gCoord = vPos.z;
        if (GradParams.x < 0.5) {
            gCoord = vPos.x;
        } else if (GradParams.x < 1.5) {
            gCoord = vPos.y;
        }
        float denom = GradParams.y - GradParams.z;
        // Epsilon guard: zero-length gradient span must not produce Inf/NaN.
        float gt = clamp((gCoord - GradParams.z) / (abs(denom) < 1e-4 ? 1e-4 : denom), 0.0, 1.0);
        gt = gt * gt * (3.0 - 2.0 * gt);
        vColor = mix(vec4(1.0), InstColor, gt);
        glow = gt;
    }
    uvLm = mix(uvLm, vec2(240.0), clamp(InstColor.a * glow, 0.0, 1.0));
    // Raw light levels 0..1 (glow already pushed toward fullbright above);
    // declared for the retired MRT encoder, unconsumed by the shipped FSHs.
    lightLevels = uvLm * (1.0 / 240.0);

    if (iris_hbmShadowDistortionMode > 0.5) {
        float d = iris_hbmShadowDistortion;
        // quartic_length (Photon): sqrt(sqrt(x^4+y^4)) via multiplications;
        // pow(x, 4.0) with negative x is undefined in GLSL.
        vec2 p = gl_Position.xy;
        vec2 p2 = p * p;
        float factor;
        if (iris_hbmShadowDistortionMode > 1.5) {
            factor = length(p) * d + (1.0 - d);
        } else {
            factor = sqrt(sqrt(p2.x * p2.x + p2.y * p2.y)) * d + (1.0 - d);
        }
        gl_Position = vec4(p / factor, gl_Position.z * iris_hbmShadowDepthScale, 1.0);
    }

    texCoord = InstUvRect.xy + UV0 * InstUvRect.zw;
    // Center of the 16x16 lightmap cell, like the vanilla UV2 -> texcoord block.
    lightmapUV = (uvLm + vec2(8.0)) / 256.0;
    vertexDistance = length(viewPos.xyz);

    // Вычисляем честный фейд по коду режима из InstBboxSize.w
    float instFade = gpuFadeAlpha(InstBboxSize.w, InstPos);
    vFadeAlpha = instFade;

    // Клиппинг: выполняется ПОСЛЕ Shadow Distortion!
    // Невидимые объекты не пишут глубину в основном проходе и не отбрасывают фантомные тени в shadow pass.
    if (instFade <= 0.0) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
    }
}