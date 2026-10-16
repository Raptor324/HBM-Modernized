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
// Packed quat: 4 snorm16 halves in 2 record words, read as GL_SHORT normalized
// ([-1,1] floats - no shader decode needed; CPU encoder packSnorm16).
layout(location = 5) in vec4 InstRot;
layout(location = 6) in vec3 InstBboxMin;
// xyz = bbox extent; w = per-instance fade (keeps attrib count <= 16 with BoneId).
layout(location = 7) in vec4 InstBboxSize;
// Corner light, PACKED u8 (normalized 0..1; decode to the 0..240 texel grid by x240):
layout(location = 8)  in vec4 InstLightC01;  // corner0.u, corner0.v, corner1.u, corner1.v
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
// w = parametric joint CHAIN base index into uJointSpecs (<0 = no parametrics).
layout(location = 14) in vec4 GradParams;
// Parametric GPU animation (MachineSpecBuilder.parametricPart / jointChainPart):
// the per-frame parameters of the joint chain, PACKED: the 4 floats carry up to
// 8 binary16 words (2 per float, low half first; decoded by halfDecode below -
// encoded CPU-side by HalfFloat; unpackHalf2x16 itself is GLSL 4.20 core, not
// available on #version 330). One word per rotate/axis-translate parameter
// (rotate = angle in degrees wrapped to (-180, 180]; axis translate = the
// distance in blocks), three words per vector translate - all in node
// DECLARATION order.
// v2 layout, selected per RIG: an untracked rig (every node trackKind == 0)
// streams all 8 words as above. A tracked rig (any node trackKind != 0) parks
// the motion clock in the first two floats instead - x = t0 (float32, segment
// start in game ticks), y = low half u0 (curve position at t0) + high half
// durationTicks (binary16; decoded 0 = FROZEN: u stays u0) - and streams ONLY
// the trackKind == 0 nodes' words in z/w (animWord slots 4..7, declaration
// order; tracked nodes consume no slot). Each tracked node advances
// u = u0 + (uAnimTime - t0) / duration and samples its keyframe-pool track
// (trackedValue / trackSample below).
// The chain base index (first node's texel triple in the global RGBA32F texture
// NucleusJointSpecs) is GradParams.w; < 0 = part without parametrics - no delta
// applied, the CPU record stays static.
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
// Parametric joint specifications (RGBA32F, v2: 3 texels per joint/chain node -
// A = kind/axis, B = pivot (+ node count on node 0), C = track spec - texelFetch
// by GradParams.w; the keyframe pool is appended after the joint region): bound
// by NucleusJointSpecs.bindSampler to unit 4.
uniform sampler2D uJointSpecs;
// Tracked-rig animation clock: current level game time + partial tick. The BEs
// interpolate machine state with Mth.lerp(partialTick, prev, curr), so a motion
// segment that began at tick t0 renders at elapsed (gameTime - t0) + partialTick
// - exactly the offset u(t) below applies; no extra constant. CPU mirror
// (MachineBer tracked-rig sampler) must decode the same halves with the same
// HalfFloat codec and use the same formula to land on the same u.
// Split animation clock (the vanilla shader-JSON parser knows only int/float/
// matrixNxN - "vec2" parses as type -1 and the Uniform buffer is allocated
// integer, so a vec2 uniform NPEs the whole MDI dispatch). uAnimTicks carries
// whole ticks (gameTime - 1, exact in float32 up to 2^24), uPartialTick the
// fractional tick; the elapsed time (uAnimTicks - t0) + uPartialTick stays
// exact at old worlds - a fused float collapsed partialTick into half-tick
// steps past 2^22 ticks (float32 spacing 0.5) and every tracked rig visibly
// juddered.
uniform float uAnimTicks;
uniform float uPartialTick;

// GPU distance fade ramp: the instance record stores a static fade MODE
// (InstancedStaticPartRenderer.FADE_MODE_NONE/STATIC/MIN_ANIM encoded as
// alpha-compatible floats 1.0/0.9999/0.9998), NOT a per-frame alpha - a
// camera-dependent alpha would dirty the record every frame of motion and the
// span-diff would never converge. The ramp is evaluated per instance from the
// view-space distance to the instance origin. CPU mirror (must stay in sync):
// RenderDistanceHelper.evalInstanceAlpha.
uniform float uStaticFadeStart;
uniform float uStaticFadeEnd;
uniform float uAnimFadeStart;
uniform float uAnimFadeEnd;

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
// Distance fade ramp, linear between start and end (blocks). CPU mirror:
// RenderDistanceHelper.evalRamp - the two must stay in sync (the CPU decides
// the opaque/fading draw split from the same formula).
float fadeRamp(float dist, float start, float end) {
    return clamp(1.0 - (dist - start) / (end - start), 0.0, 1.0);
}

// Per-instance GPU fade from the record's static MODE:
//   mode 0 (w == 1.0)   -> no distance fade (missiles, contraptions),
//   mode 1 (w ~ 0.9999) -> static-zone ramp,
//   mode 2 (w ~ 0.9998) -> min(static, anim).
// The anim ramp is CONTINUOUS: past its end it returns 0, not 1. Animated parts
// beyond the anim cutoff are not recorded at all (MachineBer skips them), but a
// part whose origin sits offset from the machine anchor can still carry a record
// while the ANCHOR is inside the cutoff. A pinned "1.0 past the end" branch
// rendered such a part frozen-opaque: it popped in fully opaque at the cutoff
// and only then ramped - the "opaque flash -> semi -> smooth restore" artifact.
// CrankShaft parity: a single continuous ramp (linear_fade.glsl), no pinned zone.
float gpuFadeAlpha(float modeW, vec3 instPos) {
    if (modeW < 0.0) return 0.0;
    float mode = (1.0 - modeW) * 10000.0;
    if (mode < 0.5) return 1.0;
    // View-space instance origin: ModelViewMat = R_cam * T(-relCam), so its
    // length is the distance from the camera to the instance origin (anchor-
    // relative record pos) - the same distance the CPU membership split uses.
    float dist = length((ModelViewMat * vec4(instPos, 1.0)).xyz);
    float sf = fadeRamp(dist, uStaticFadeStart, uStaticFadeEnd);
    if (mode < 1.5) return sf;
    return min(sf, fadeRamp(dist, uAnimFadeStart, uAnimFadeEnd));
}

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

// binary16 -> float, pure GLSL 330. unpackHalf2x16 is NOT 3.30 core (it is
// GLSL 4.20 core / ARB_gpu_shader5 - on a #version 330 shader it is an
// undeclared function without the extension directive), so the decode is done
// by hand; floatBitsToUint/uintBitsToFloat ARE 3.30 core (ARB_shader_bit_encoding).
// Same semantics as unpackHalf2x16(.x): RNE is lost on the encode side only -
// the decode here is exact.
#ifdef USE_VERTEX_BONE_ID
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
        // Inf / NaN (the quiet bit lives in m).
        return uintBitsToFloat(s | 0x7F800000u | (m << 13u));
    }
    // Normal: exponent rebias 15 -> 127.
    return uintBitsToFloat(s | ((e + 112u) << 23u) | (m << 13u));
}

// Packed AnimParams word fetch: word w of up to 8 binary16 joint parameters
// (2 per record float, low half first - see HalfFloat, the CPU encoder).
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
float trackSample(int ptr, int len, float u) {
    if (len <= 0) return 0.0;
    if (len == 1) return texelFetch(uJointSpecs, ivec2(ptr, 0), 0).y;
    vec4 first = texelFetch(uJointSpecs, ivec2(ptr, 0), 0);
    vec4 last = texelFetch(uJointSpecs, ivec2(ptr + len - 1, 0), 0);
    if (u <= first.x) return first.y;
    if (u >= last.x) return last.y;
    vec4 prev = first;
    for (int i = 1; i < len; i++) {
        vec4 cur = texelFetch(uJointSpecs, ivec2(ptr + i, 0), 0);
        if (u <= cur.x) {
            float span = cur.x - prev.x;
            float factor = span > 0.0 ? (u - prev.x) / span : 0.0;
            return mix(prev.y, cur.y, factor);
        }
        prev = cur;
    }
    return last.y;
}

// Tracked-node parameter (texel C = trackKind/flags/trackPtr/trackLen):
// u advances from (t0 = AnimParams.x, u0 = low half of AnimParams.y) by the
// clock; durationTicks = high half, decoded 0 means FROZEN (u stays u0).
// flags bit0 selects the wrap mode: 1 = cycle -> fract(u), 0 = clamp.
// flags bit1 selects Hermite reciprocating stroke spline evaluation.
float trackedValue(vec4 specC) {
    // "packed" is a GLSL keyword (block layout qualifier) - the local is "raw".
    uint raw = floatBitsToUint(AnimParams.y);
    float u0 = halfDecode(raw & 0xFFFFu);
    float dur = halfDecode(raw >> 16u);
    float u = u0;
    if (dur != 0.0) {
        // specC.x carries the track's rateScale (frequency multiplier): the clock
        // advances the curve position rateScale times faster/slower than ticks.
        u += (((uAnimTicks - AnimParams.x) + uPartialTick) / dur) * specC.x;
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
#endif
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
    // Parametric GPU animation: joint CHAIN delta in LOCAL part coordinates,
    // BEFORE the world record (InstPos/InstRot). The CPU does not rebuild the
    // record - the span diff sees only the 16-byte AnimParams tail (a frozen
    // part costs zero upload). Rotation is Rodrigues' formula around the
    // normalized axis.
    //
    // Chain layout (MachineSpecBuilder.jointChainPart): GradParams.w = base node
    // index; the chain occupies that many consecutive texel triples (v2: A =
    // kind/axis, B = pivot, C = track spec), node 0's pivot texel .w carries the
    // node count (0 = a legacy single joint - read as 1). Per-node parameters
    // are PACKED into AnimParams as binary16 words in DECLARATION order (rotate
    // 1, axis translate 1, vector translate 3; 2 words per record float, low
    // first - decoded by animWord), so a declaration-order prefix-sum of the
    // per-node costs gives each node's word offset. On a TRACKED rig (any node
    // trackKind != 0) the first two floats carry t0/u0/duration instead and the
    // streamed words start at slot 4: tracked nodes skip the slot cursor and
    // sample their keyframe-pool track (trackedValue); an untracked rig is
    // bit-for-bit the pre-v2 walk. PoseStack ops compose M = N1*N2*...*Nk (ops
    // declared first act on the vertex last), so node 0 is the LEFTMOST matrix
    // factor.
    //
    // Array-free evaluation: every chain input (GradParams.w, AnimParams, the
    // uJointSpecs texels) is per-instance, so the old version recomputed it in
    // every vertex AND staged specA[8]/specB[8]/offs[8] locals indexed by the
    // loop variable - register pressure (possible spilling). Length-1 chains
    // (all current rigs: press head, assembler slider/arm/cogs) keep the
    // pre-chain single-joint block verbatim (bit-identical); longer chains
    // compose each node's rigid transform into one mat4 in a single forward
    // pass and apply it to vPos/vNrm once - mathematically identical, not
    // bit-identical (no shipped rig takes that path yet).
    if (GradParams.w >= 0.0) {
        int base = int(GradParams.w);
        float countW = texelFetch(uJointSpecs, ivec2(base * 3 + 1, 0), 0).w;
        int n = 1;
        if (countW >= 0.5) {
            n = int(countW);
        }
        if (n > 8) n = 8; // shader bound; the CPU validates the same limit
        if (n == 1) {
            vec4 specA = texelFetch(uJointSpecs, ivec2(base * 3, 0), 0);
            vec4 specB = texelFetch(uJointSpecs, ivec2(base * 3 + 1, 0), 0);
            vec4 specC = texelFetch(uJointSpecs, ivec2(base * 3 + 2, 0), 0);
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
                // KIND_TRANSLATE consumes 3 consecutive words: the translation
                // vector (single-axis producers write it pre-scaled by the axis).
                // A scalar track has no 3-word semantic - a tracked KIND_TRANSLATE
                // is not a valid rig (CPU validation never emits one); render it
                // unposed instead of inventing a direction.
                if (!tracked) vPos += vec3(animWord(0), animWord(1), animWord(2));
            } else {
                // KIND_TRANSLATE_AXIS: the single word is the distance along the
                // node's (normalized) axis.
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
                if (texelFetch(uJointSpecs, ivec2((base + i) * 3 + 2, 0), 0).x > 0.0) {
                    wordBase = 4;
                    break;
                }
            }
            int acc = wordBase;
            for (int i = 0; i < n; i++) {
                vec4 specA = texelFetch(uJointSpecs, ivec2((base + i) * 3, 0), 0);
                vec4 specB = texelFetch(uJointSpecs, ivec2((base + i) * 3 + 1, 0), 0);
                vec4 specC = texelFetch(uJointSpecs, ivec2((base + i) * 3 + 2, 0), 0);
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
    // Corner light: the record packs 8 corners as u8 over the full byte range -
    // the normalized (0..1) fetch decodes to the 0..240 texel grid by x240
    // (byte x 240/255; CPU encoder InstancedStaticPartRenderer.packLightU8).
    lc01 = InstLightC01 * 240.0;
    lc23 = InstLightC23 * 240.0;
    lc45 = InstLightC45 * 240.0;
    lc67 = InstLightC67 * 240.0;

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
    // No gradient (GradParams.x < 0): glow at full weight, matching vColor =
    // InstColor below - the emission (InstColor.a) must still work for parts
    // with a uniform tint and no falloff.
    float glow = 1.0;
    vColor = InstColor;
    if (GradParams.x >= 0.0) {
        float gCoord = vPos.z;
        if (GradParams.x < 0.5) {
            gCoord = vPos.x;
        } else if (GradParams.x < 1.5) {
            gCoord = vPos.y;
        }
        float denom = GradParams.y - GradParams.z;
        float gt = clamp((gCoord - GradParams.z) / (abs(denom) < 1e-4 ? 1e-4 : denom), 0.0, 1.0);
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
#ifdef USE_VERTEX_BONE_ID
    float instFade = gpuFadeAlpha(InstBboxSize.w, InstPos);
#else
    float instFade = gpuFadeAlpha(InstFadeAlpha, InstPos);
#endif
    vFadeAlpha = instFade;
    // A fully faded record draws nothing: clip the instance out instead of
    // blending alpha 0 - the fading phase writes depth, an invisible record
    // would punch holes into things behind it.
    if (instFade <= 0.0) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
    }
#else
    vFadeAlpha = FadeAlpha;
#endif
}
