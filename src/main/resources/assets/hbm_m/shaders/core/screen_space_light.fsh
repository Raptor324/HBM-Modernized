#version 150

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uDepthTex0;
uniform sampler2D uDhDepthTex;
uniform sampler2D uSceneColor;
uniform int       uDhReverseZ;
uniform int       uDhPresent;
uniform float     uDhNear;
uniform float     uDhFar;
uniform float     uDhFadeDist;
uniform mat4      uProj;
uniform mat4      uModelView;
uniform mat4      uModelViewInv;
uniform vec3      uCamPos;
uniform vec4      uSources[4];
uniform int       uSourceCount;
uniform float     uSrcRadius[4];
uniform vec2      uSrcShock[4];
uniform vec4      uSrcExtra[4];
uniform float     uTime;
uniform float     uVanillaFar;
uniform float     uIntensity;
uniform float     uHaloStrength;
uniform float     uHeatScale;
uniform vec2      uResolution;
uniform float     uNormalScale;
uniform int       uDebugReplace;
uniform int       uSolidAdd;
uniform int       uHasScene;
uniform float     uCine;
uniform int       uDhBlittedInDepth0;

const int   SHADOW_STEPS     = 10;
const float SHADOW_THICKNESS = 1.8;
const float OCCLUDER_MAX_LEN = 36.0;
const int   GODRAY_STEPS     = 12;

// --- Noise utilities for 3D plasma, Wilson cloud and heat shimmer ---
float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i + vec3(0,0,0));
    float n100 = hash31(i + vec3(1,0,0));
    float n010 = hash31(i + vec3(0,1,0));
    float n110 = hash31(i + vec3(1,1,0));
    float n001 = hash31(i + vec3(0,0,1));
    float n101 = hash31(i + vec3(1,0,1));
    float n011 = hash31(i + vec3(0,1,1));
    float n111 = hash31(i + vec3(1,1,1));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm3D(vec3 p) {
    float v = 0.0;
    float a = 0.55;
    vec3 shift = vec3(100.0);
    for (int i = 0; i < 3; i++) {
        v += a * noise3D(p);
        p = p * 2.04 + shift;
        a *= 0.5;
    }
    return v;
}

// Intersect ray (camPos, dir) with sphere (sc, r). Returns vec2(tNear, tFar).
// If no hit, returns vec2(-1.0).
vec2 raySphere(vec3 ro, vec3 rd, vec3 sc, float r) {
    vec3 oc = ro - sc;
    float b = dot(oc, rd);
    float c = dot(oc, oc) - r * r;
    float h = b * b - c;
    if (h < 0.0) return vec2(-1.0);
    h = sqrt(h);
    return vec2(-b - h, -b + h);
}

float sampleRawDepth(vec2 uv) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;
    return texture(uDepthTex0, uv).r;
}

float sampleDhRawDepth(vec2 uv) {
    if (uDhPresent == 0 || uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return -1.0;
    return texture(uDhDepthTex, uv).r;
}

float linearizeVanillaDepth(float zNdc) {
    float zN = zNdc * 2.0 - 1.0;
    // Forward projection matrix coefficients (uProjInv[2][2] is always 0.0 and
    // uProjInv[3][2] is -1.0, which collapsed every distance to ~1/zN ~= 1 m).
    return -uProj[3][2] / max(-uProj[2][2] - zN, 1e-6);
}

// DH stores WINDOW depth (GL's default window transform ndc*0.5+0.5 - DH never
// calls glClipControl/glDepthRange, and DH's own fog.frag decodes the sampled
// depth through ndc.xyz = ndc.xyz*2.0 - 1.0). REVERSE_Z: DH's reverse matrix
// (m22 = N/(F-N), m23 = F*N/(F-N)) gives ndc = (N/(F-N))*(F/dist - 1), so
// terrain sits in (0.5, 1.0] and sky is cleared to 0.0; decode through the same
// 2d-1 remap the forward branch applies: dist = N*F / (N + ndc*(F-N)).
// (Feeding the window value without the remap decoded every LOD to [N, 2N]
// blocks: LODs classified as ~8-15 block geometry, shadows collapsed, the
// shockwave shell culled everywhere except sky.)
float linearizeDhDepth(float d) {
    if (d < 0.0) return 1e9;
    float near = uDhNear;
    float far  = uDhFar;
    float ndc = d * 2.0 - 1.0;
    float denom = (uDhReverseZ != 0)
        ? near + ndc * (far - near)
        : (far + near) - ndc * (far - near);
    return (denom > 1e-6) ? (near * far / denom) : far;
}

// DH sky / unwritten texel. The conventions are opposite: REVERSE_Z clears the
// DEPTH32F to 0.0 and stores window depth (terrain > 0.5), FORWARD_Z to 1.0.
bool dhLayerEmpty(float dDh) {
    return (uDhReverseZ != 0) ? (dDh <= 0.5) : (dDh >= 0.999999);
}

// True when d0 in uDepthTex0 was written by dh_depth_blit.fsh rather than the
// frame's own geometry: the blit re-encodes DH distances with the EXTENDED
// projection (near=0.05, far=8e6, window = 1 - fnEff/dist), NOT the frame
// projection linearizeVanillaDepth decodes with - a misdecoded blit pixel
// collapses to tens-hundreds of blocks and breaks classification, normals,
// shadows and the shockwave shell (this guard existed in HEAD, the rewrite
// dropped it leaving the uDhBlittedInDepth0 flag dead).
bool isBlittedDhDepth(float d0, float dhDist) {
    if (dhDist >= 1e8 || dhDist < uDhFadeDist) {
        return false;
    }
    float fnEff = (8000000.0 * 0.05) / (8000000.0 - 0.05);
    float expectedBlit = clamp((1.0 - fnEff / dhDist) + 1.0e-6, 0.0, 1.0);
    return abs(d0 - expectedBlit) < 2.5e-5;
}

float sampleUnifiedDistance(vec2 uv) {
    float d0 = sampleRawDepth(uv);

    // DH layer decoded once: it is both the primary LOD source (d0 = sky) and
    // the reference for detecting blitted DH pixels inside uDepthTex0.
    float linDh = 1e9;
    bool dhGeometry = false;
    if (uDhPresent != 0) {
        float dDh = sampleDhRawDepth(uv);
        if (dDh >= 0.0 && !dhLayerEmpty(dDh)) {
            linDh = linearizeDhDepth(dDh);
            // Dither-fade ring exclusion: inside [uClipDistance, 1.5*uClipDist]
            // DH's DEPTH32F is stochastic noise (bayer-discard in the DH terrain
            // shader) - it must not classify as geometry or feed
            // normals/shadows (HEAD excluded the ring; uDhFadeDist <= 0 = gate off).
            dhGeometry = linDh < uDhFar * 0.999
                && (uDhFadeDist <= 0.0 || linDh >= uDhFadeDist);
        }
    }

    if (d0 < 1.0) {
        bool blitted = dhGeometry && uDhBlittedInDepth0 != 0 && isBlittedDhDepth(d0, linDh);
        if (!blitted) {
            float lin0 = linearizeVanillaDepth(d0);
            if (lin0 <= uVanillaFar) {
                return lin0;
            }
            // A sub-1.0 depth decoding beyond the vanilla far plane cannot be
            // real frame-rendered geometry (e.g. DH-encoded depth when the blit
            // flag was missed) - fall through to the DH layer instead of
            // returning garbage distances (HEAD's 'length(p) <= uVanillaFar').
        }
    }

    if (dhGeometry) {
        return linDh;
    }
    return 1e9;
}

vec3 getViewRayDir(vec2 uv) {
    vec2 ndc = uv * 2.0 - 1.0;
    vec3 viewRay = vec3(ndc.x / uProj[0][0], ndc.y / uProj[1][1], -1.0);
    return normalize(viewRay);
}

vec3 reconstructWorldPos(vec2 uv, float linDist) {
    vec3 viewDir = getViewRayDir(uv);
    vec3 viewPos = viewDir * (linDist / max(1e-4, -viewDir.z));
    vec3 worldOffset = mat3(uModelViewInv) * viewPos;
    return uCamPos + worldOffset;
}

vec3 deriveNormal(vec2 uv, vec3 P, float viewDist) {
    vec2 off = uNormalScale * 2.0 / max(uResolution, vec2(1.0));
    float dR = sampleUnifiedDistance(uv + vec2(off.x, 0.0));
    float dL = sampleUnifiedDistance(uv - vec2(off.x, 0.0));
    float dU = sampleUnifiedDistance(uv + vec2(0.0, off.y));
    float dD = sampleUnifiedDistance(uv - vec2(0.0, off.y));

    // Symmetric edge-aware reconstruction: pick the neighbor whose depth is closest
    // to this pixel (stays on the same surface across depth discontinuities) and
    // mirror the direction when sampling backwards, keeping a consistent winding so
    // normals point toward the camera without any forced flipping.
    vec3 dX = (abs(dR - viewDist) < abs(dL - viewDist))
            ? (reconstructWorldPos(uv + vec2(off.x, 0.0), dR) - P)
            : (P - reconstructWorldPos(uv - vec2(off.x, 0.0), dL));
    vec3 dY = (abs(dU - viewDist) < abs(dD - viewDist))
            ? (reconstructWorldPos(uv + vec2(0.0, off.y), dU) - P)
            : (P - reconstructWorldPos(uv - vec2(0.0, off.y), dD));

    vec3 n = cross(dX, dY);
    float lenSq = dot(n, n);
    if (lenSq < 1e-10) return vec3(0.0, 1.0, 0.0);
    return n * inversesqrt(lenSq);
}

// Project a world-space point safely to screen UV coordinates.
// If behind camera (spView.z >= -0.1), smoothly pushes UV beyond the screen border
// along the correct direction rather than inverting upside-down through the screen center.
vec3 projectToScreen(vec3 wp) {
    vec3 spView = mat3(uModelView) * (wp - uCamPos);
    if (spView.z >= -0.15) {
        vec2 dir2D = normalize(vec2(spView.x, spView.y) + vec2(1e-4, 1e-4));
        return vec3(0.5 + dir2D * 3.5, -1.0);
    }
    vec2 ndc = vec2(spView.x * uProj[0][0], spView.y * uProj[1][1]) / (-spView.z);
    return vec3(ndc * 0.5 + 0.5, -spView.z);
}

float interleavedGradientNoise(vec2 fragCoord) {
    return fract(52.9829189 * fract(0.06711056 * fragCoord.x + 0.00583715 * fragCoord.y));
}

// Soft raymarched screen-space shadow with jittered steps to eliminate banding
float shadowSoft(vec3 P, vec3 S, float lightDist) {
    float jitter = interleavedGradientNoise(gl_FragCoord.xy);
    float occ = 0.0;
    float marchDist = min(lightDist, 96.0);
    vec3 rayDir = (S - P) / max(lightDist, 1e-4);
    for (int i = 1; i <= SHADOW_STEPS; i++) {
        float tFrac = (float(i) - jitter * 0.85) / float(SHADOW_STEPS);
        vec3 sp = P + rayDir * (marchDist * tFrac);
        vec3 sc = projectToScreen(sp);
        if (sc.z < 0.0 || sc.x < 0.01 || sc.x > 0.99 || sc.y < 0.01 || sc.y > 0.99) {
            continue;
        }
        float sceneDist = sampleUnifiedDistance(sc.xy);
        float rayDist = sc.z;
        if (sceneDist < rayDist - SHADOW_THICKNESS) {
            float distPastOccluder = rayDist - sceneDist;
            if (distPastOccluder < OCCLUDER_MAX_LEN) {
                float occluderFalloff = 1.0 - (distPastOccluder / OCCLUDER_MAX_LEN);
                occ = max(occ, (1.0 - tFrac * 0.65) * occluderFalloff);
            }
        }
    }
    return clamp(1.0 - occ, 0.0, 1.0);
}

// Smooth analytical atmospheric in-scattering restricted to the forward hemisphere
float airMarch(vec3 P, vec3 S, float viewDist, float R) {
    vec3 dir = normalize(P - uCamPos);
    vec3 toLight = S - uCamPos;
    float camToLightDist = length(toLight);
    vec3 lDir = toLight / max(camToLightDist, 1e-4);
    float fwd = max(0.0, dot(dir, lDir));
    if (fwd <= 0.0) return 0.0;

    float tEnd = min(viewDist, R * 1.5);
    float tMid = clamp(dot(toLight, dir), 0.0, tEnd);
    vec3 pNear = uCamPos + dir * tMid;
    float dClosest = length(pNear - S);
    float coreScatter = 1.0 / (1.0 + pow(dClosest / (R * 0.14 + 1e-4), 1.85));
    float airColumn = clamp(tEnd / max(camToLightDist, 32.0), 0.0, 1.0);
    return coreScatter * fwd * airColumn;
}

// Jittered screen-space volumetric godrays restricted to forward hemisphere
float godraysAndWrap(vec2 uv, vec3 S, vec3 dir, float viewDist) {
    vec3 toLight = normalize(S - uCamPos);
    float cosA = dot(dir, toLight);
    if (cosA <= 0.0) return 0.0;

    vec3 sProj = projectToScreen(S);
    if (sProj.z < 0.0) return 0.0;
    vec2 sUv = sProj.xy;
    vec2 delta = (sUv - uv);
    float len = length(delta);
    if (len < 1e-4) return 0.0;

    float jitter = interleavedGradientNoise(gl_FragCoord.xy);
    vec2 stepUv = (delta / len) * min(len, 0.35) / float(GODRAY_STEPS);
    vec2 cur = uv + stepUv * jitter;
    float accum = 0.0;
    float weight = 1.0;

    for (int i = 0; i < GODRAY_STEPS; i++) {
        cur += stepUv;
        if (cur.x < 0.0 || cur.x > 1.0 || cur.y < 0.0 || cur.y > 1.0) break;
        float tapDist = sampleUnifiedDistance(cur);
        bool openAir = (tapDist > 1e8) || (tapDist > viewDist * 1.6 + 3.5);
        if (openAir) {
            accum += weight;
        }
        weight *= 0.86;
    }

    float mie = 0.55 * (1.0 - 0.72 * 0.72) / pow(max(0.01, 1.0 + 0.72 * 0.72 - 2.0 * 0.72 * cosA), 1.5);
    return (accum / float(GODRAY_STEPS)) * mie * cosA * 2.4;
}

// Comprehensive 3D spherical shockwave with Fresnel limb, Wilson cloud, and camera pulse
// surfEuclid: EUCLIDEAN distance of the classified surface along this pixel's
// ray (length(P - uCamPos)). The ray-sphere hit parameter is euclidean too -
// comparing it against a view-axis Z-depth (off-axis smaller by cos(angle))
// over-culled the shell toward the screen edges, leaving it visible only
// against sky.
vec3 shockwave3DShell(vec3 dir, vec2 uv, vec3 sc, float shockR, float shockFade, float surfEuclid, float ageSec, float scale, inout vec2 uvDistort) {
    if (shockFade <= 0.001 || shockR <= 0.5) return vec3(0.0);

    vec2 hit = raySphere(uCamPos, dir, sc, shockR);
    if (hit.y <= 0.0) return vec3(0.0);

    // sFront is euclidean (raySphere takes a unit dir); hit.x <= 0 means the
    // camera is inside the sphere - use the exit crossing then.
    float sFront = hit.x > 0.0 ? hit.x : hit.y;
    if (sFront >= surfEuclid) return vec3(0.0);

    vec3 hitP = uCamPos + dir * sFront;
    vec3 sNorm = normalize(hitP - sc);

    // Fresnel refraction at the curved boundary
    float fresnel = 1.0 - abs(dot(-dir, sNorm));
    float shellThickness = shockR * 0.045 + 1.2;
    float depthInSphere = hit.y - hit.x;
    float shellDensity = smoothstep(0.0, shellThickness, depthInSphere) * pow(fresnel, 3.5);

    // Screen UV distortion for refraction
    vec3 screenNorm = (uModelView * vec4(sNorm, 0.0)).xyz;
    vec2 distortOffset = screenNorm.xy * (0.038 * shockFade * fresnel);
    uvDistort += distortOffset;

    // 3D Wilson condensation cloud
    float wilsonLife = smoothstep(0.3, 1.2, ageSec) * (1.0 - smoothstep(1.5, 6.8 * scale, ageSec));
    float wilsonCloud = 0.0;
    if (wilsonLife > 0.01) {
        vec3 wCoord = hitP * 0.045 + vec3(0.0, uTime * 0.25, 0.0);
        float wNoise = fbm3D(wCoord);
        float upperDome = smoothstep(sc.y - 5.0, sc.y + shockR * 0.65, hitP.y);
        wilsonCloud = smoothstep(0.42, 0.72, wNoise) * upperDome * wilsonLife * fresnel * 1.8;
    }

    // Camera overpressure pulse when shockwave intersects player head
    float camDist = length(uCamPos - sc);
    float camProximity = abs(camDist - shockR);
    float pulseDist = 12.0 + shockR * 0.03;
    float camPulse = (1.0 - smoothstep(0.0, pulseDist, camProximity)) * shockFade;
    if (camPulse > 0.01) {
        vec2 fromCenter = uv - vec2(0.5);
        uvDistort += normalize(fromCenter + 1e-4) * (0.045 * camPulse);
    }

    vec3 shockColor = vec3(0.9, 1.15, 1.5) * shellDensity * 1.6;
    shockColor += vec3(1.4, 1.45, 1.5) * wilsonCloud;
    shockColor += vec3(1.8, 1.7, 1.4) * (camPulse * 0.7);

    // Scaled by uIntensity (HEAD scaled the shockwave band by it): at
    // nukeLightIntensity=0 the shell goes dark while the refraction
    // distortion above still applies ("искажение без свечения").
    return shockColor * shockFade * uIntensity;
}

// Sky Dome Glow & Cloud Lighting
vec3 skyGlow(vec3 dir, vec3 S) {
    vec3 toLight = S - uCamPos;
    float dLight = length(toLight);
    vec3 lDir = toLight / max(dLight, 1e-4);

    float cosTheta = dot(dir, lDir);
    float fwd = max(0.0, cosTheta);

    float rayleigh = 0.45 * (1.0 + cosTheta * cosTheta);
    float mie = 0.85 * pow(max(0.0, fwd), 6.0) + 1.8 * pow(max(0.0, fwd), 32.0);

    vec3 colRayleigh = vec3(1.8, 0.75, 0.25);
    vec3 colMie = vec3(2.5, 1.9, 1.2);
    vec3 sky = (colRayleigh * rayleigh + colMie * mie) * 0.45;

    float horiz = pow(1.0 - max(0.0, dir.y), 4.0);
    sky += vec3(1.6, 0.6, 0.12) * horiz * 0.35;

    return sky;
}

void main() {
    if (uSourceCount <= 0) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 uv = vTexCoord;
    vec2 uvDistort = vec2(0.0);

    vec3 viewDir = getViewRayDir(uv);
    vec3 dir = normalize(mat3(uModelViewInv) * viewDir);

    float viewDist = sampleUnifiedDistance(uv);
    bool geometry = (viewDist < 1e8);
    vec3 P = geometry ? reconstructWorldPos(uv, viewDist) : (uCamPos + dir * 1000.0);
    vec3 N = geometry ? deriveNormal(uv, P, viewDist) : vec3(0.0, 1.0, 0.0);

    // Euclidean surface distance along this pixel's ray. viewDist is a
    // view-axis Z-depth (like-for-like with both depth layers and godray taps),
    // but the shockwave shell cull needs the euclidean metric to match its
    // ray-sphere hit parameter (see shockwave3DShell).
    float surfEuclid = geometry ? length(P - uCamPos) : viewDist;

    vec3 accumLight = vec3(0.0);
    vec3 accumCinematics = vec3(0.0);

    for (int i = 0; i < uSourceCount; i++) {
        vec4 src = uSources[i];
        vec3 S = src.xyz;
        float heat = src.w;
        if (heat <= 0.0) continue;

        float R = uSrcRadius[i];
        vec2 shock = uSrcShock[i];
        vec4 extra = uSrcExtra[i];
        float ageSec = extra.x;
        float scale = extra.y;
        float groundY = extra.z;
        float flashPulse = extra.w;

        // 1. Shockwave 3D Shell calculation and refraction accumulation.
        // Cinematics config gate: nukeCinematics=0 = "classic additive light"
        // (ModClothConfig) - no shell, no refraction/overpressure distortion,
        // no Wilson cloud (HEAD gated the shockwave band by uCine too).
        if (uCine > 0.001) {
            accumCinematics += shockwave3DShell(dir, uv, vec3(S.x, groundY, S.z), shock.x, shock.y, surfEuclid, ageSec, scale, uvDistort);
        }

        // 2. Direct Surface Lighting with soft shadows and sky-bounce
        vec3 toLight = S - P;
        float lightDist = length(toLight);
        vec3 L = toLight / max(lightDist, 1e-4);

        float atten = clamp(1.0 - (lightDist / R), 0.0, 1.0);
        atten = atten * atten;

        float NdotL = max(0.0, dot(N, L));
        float shadow = geometry ? shadowSoft(P, S, lightDist) : 1.0;

        // Warm sky-bounce indirect reflection (prevents pitch black shadows behind walls)
        vec3 skyBounce = vec3(0.35, 0.16, 0.06) * max(0.0, N.y * 0.5 + 0.5) * atten * 0.45;

        vec3 surfaceColor = (vec3(2.2, 1.45, 0.85) * (NdotL * shadow) + skyBounce) * atten * (heat / uHeatScale);
        if (geometry) {
            accumLight += surfaceColor;
        }

        float camDist = length(uCamPos - S);
        float distFade = clamp(1.0 - (camDist / (R * 1.4)), 0.0, 1.0);
        distFade *= distFade;

        // 3. Volumetric Obstacle-Wrapping Godrays + Edge Glow
        float godrays = godraysAndWrap(uv, S, dir, viewDist);
        accumLight += vec3(1.8, 1.1, 0.55) * godrays * (heat / uHeatScale) * distFade * 0.40;

        // 4. Atmospheric Line-of-sight Air In-scattering
        float inScatter = airMarch(P, S, viewDist, R);
        accumLight += vec3(1.4, 0.75, 0.3) * inScatter * (heat / uHeatScale) * distFade * 0.25;

        // 5. Sky & Horizon Glow (full on sky, subtle distance haze on far geometry)
        float skyMask = geometry ? clamp(viewDist / 256.0, 0.0, 0.20) : 1.0;
        accumLight += skyGlow(dir, S) * (heat / uHeatScale) * uHaloStrength * distFade * skyMask * 0.85;

        // 7. Double-pulse initial nuclear thermal bleaching flash
        // Cinematics-layer term: gated by uCine and scaled by uIntensity so
        // nukeCinematics=0 and nukeLightIntensity=0 both silence it.
        if (uCine > 0.001 && flashPulse > 0.01) {
            float flashAtten = clamp(1.0 - (camDist / (R * 1.5)), 0.0, 1.0);
            flashAtten *= flashAtten;
            accumCinematics += vec3(1.8, 1.7, 1.5) * (flashPulse * flashAtten * 0.85 * uIntensity);
        }
    }

    accumLight *= uIntensity;

    // Heat shimmer distortion offset (only evaluated when scene sampling is active and light is non-zero)
    if (uHasScene != 0 && uSolidAdd == 0 && uDebugReplace == 0 && viewDist < 80.0
            && dot(accumLight + accumCinematics, vec3(1.0)) > 0.01) {
        vec3 shimmerCoord = P * 0.12 + vec3(0.0, uTime * 2.5, 0.0);
        vec2 shimmerOffset = (vec2(fbm3D(shimmerCoord), fbm3D(shimmerCoord + 12.5)) - 0.5) * 0.007;
        uvDistort += shimmerOffset * clamp(1.0 - viewDist / 80.0, 0.0, 1.0);
    }

    // Apply distortion and composite with scene if uHasScene == 1
    if (uHasScene != 0 && (uSolidAdd == 0) && (uDebugReplace == 0)) {
        vec3 contribution = accumLight + accumCinematics;
        vec2 sceneUv = clamp(uv + uvDistort, 0.001, 0.999);
        vec3 baseScene = texture(uSceneColor, sceneUv).rgb;

        // Pixels with neither light nor distortion are written back UNGRADED
        // (HEAD discarded them): the ACES curve below must not repaint the
        // whole frame just because some source is alive somewhere on screen.
        if (dot(contribution, vec3(1.0)) <= 0.0 && uvDistort == vec2(0.0)) {
            fragColor = vec4(baseScene, 1.0);
            return;
        }

        vec3 combined = baseScene + contribution;

        // ACES-style gentle highlight compression
        combined = (combined * (2.51 * combined + 0.03)) / (combined * (2.43 * combined + 0.59) + 0.14);

        fragColor = vec4(combined, 1.0);
    } else {
        vec3 combined = accumLight + accumCinematics;
        fragColor = vec4(combined, 1.0);
    }
}
