#version 120
#define SHADOW_QUALITY 1 //[0 1 2]

varying vec2  texcoord;
varying vec2  lmcoord;
varying vec4  color;
varying vec3  normal;
varying vec3  worldPos;
varying vec4  shadowPos;
varying float viewDist;

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform sampler2D shadowtex0;   // raw depth — manual PCSS, no hardware filtering
uniform vec3  sunPosition;
uniform vec3  shadowLightPosition;
uniform vec3  skyColor;
uniform float rainStrength;
uniform float shadowDistance;

// --- Photon-style lift: brings up dark areas for natural-looking contrast ---
float lift(float x, float amount) {
    return (x + x * amount) / max(1.0 + x * amount, 0.0001);
}

// --- Vogel disk sample (PCSS blocker/filter) ---
vec2 vogelDisk(int i, int n, float phi) {
    float r     = sqrt(float(i) + 0.5) / sqrt(float(n));
    float theta = float(i) * 2.39996 + phi; // golden angle
    return r * vec2(cos(theta), sin(theta));
}

// --- Manual depth comparison (no hardware filtering) ---
float shadowSample(vec2 uv, float refDepth, float bias) {
    uv = clamp(uv, 0.0005, 0.9995);
    float d = texture2D(shadowtex0, uv).r;
    return step(refDepth - bias, d); // 1.0 = lit
}

// --- PCSS soft shadow ---
float getShadow(vec4 sp, float dist) {
    vec3 sc = sp.xyz * 0.5 + 0.5;
    if (sc.x < 0.001 || sc.x > 0.999 || sc.y < 0.001 || sc.y > 0.999 || sc.z > 0.999)
        return 1.0;

    float bias    = 0.0008 + 0.0004 * (1.0 - dot(normalize(normal), normalize(shadowLightPosition)));
    float pxSize  = 1.0 / 2048.0;
    float phi     = fract(sin(dot(floor(worldPos * 8.0), vec3(127.1, 311.7, 74.7))) * 43758.5);

#if SHADOW_QUALITY == 0
    // 4 samples, small radius (fast)
    float s = 0.0;
    for (int i = 0; i < 4; i++)
        s += shadowSample(sc.xy + vogelDisk(i, 4, phi) * pxSize * 1.5, sc.z, bias);
    float shadow = s * 0.25;
#elif SHADOW_QUALITY == 2
    // 16 samples, larger radius (quality)
    float s = 0.0;
    for (int i = 0; i < 16; i++)
        s += shadowSample(sc.xy + vogelDisk(i, 16, phi) * pxSize * 3.0, sc.z, bias);
    float shadow = s / 16.0;
#else
    // 8 samples, medium radius
    float s = 0.0;
    for (int i = 0; i < 8; i++)
        s += shadowSample(sc.xy + vogelDisk(i, 8, phi) * pxSize * 2.0, sc.z, bias);
    float shadow = s / 8.0;
#endif

    float fade = 1.0 - smoothstep(max(shadowDistance,32.0)*0.72, max(shadowDistance,32.0)*0.97, dist);
    return mix(1.0, shadow, clamp(fade, 0.0, 1.0));
}

void main() {
    vec4 albedo = texture2D(texture, texcoord) * color;
    if (albedo.a < 0.1) discard;

    vec2  lm       = clamp(lmcoord, 0.0, 1.0);
    float skyLit   = lm.y;
    float blockLit = lm.x;

    float sunH  = normalize(sunPosition).y;
    float sunUp = clamp(sunH, 0.0, 1.0);

    // Golden-hour detection
    float golden = (1.0 - smoothstep(0.0, 0.22, abs(sunH))) * smoothstep(-0.06, 0.04, sunH);

    // Sun/moon color
    vec3 sunCol  = mix(vec3(1.02, 0.98, 0.88), vec3(1.12, 0.58, 0.12), golden);
    sunCol       = mix(sunCol, vec3(0.60, 0.68, 0.88), rainStrength * 0.75);
    vec3 moonCol = vec3(0.50, 0.56, 0.80) * 0.12;

    // PCSS shadow
    float NdotL  = max(dot(normalize(normal), normalize(shadowLightPosition)), 0.0);
    float shadow = getShadow(shadowPos, viewDist);
    shadow       = mix(1.0, shadow, skyLit * sunUp);

    // --- Photon-style diffuse: lifted NdotL ---
    // Much less floor-lifting than before - we WANT visible contrast between lit/shadowed.
    float diffuse = lift(NdotL, 0.15);
    diffuse       = mix(0.55, 1.15, diffuse); // sun-facing surfaces get a real boost

    // Direct sunlight contribution - this is the dominant term in direct daylight
    vec3 directSun  = sunCol * diffuse * shadow * sunUp * 1.8;

    // Sky hemisphere ambient - kept deliberately dim so shadow areas read as DARK,
    // not just slightly-less-bright. This is what gives Photon-style contrast.
    float skyFacing = clamp(dot(normalize(normal), vec3(0.0, 1.0, 0.0)) * 0.5 + 0.5, 0.0, 1.0);
    vec3 skyAmb     = mix(vec3(0.10, 0.13, 0.22), vec3(0.16, 0.19, 0.26), skyFacing);
    skyAmb          = mix(skyAmb, vec3(0.22, 0.24, 0.28), rainStrength * 0.7);
    skyAmb         *= skyLit;

    // Moon ambient
    vec3 moonAmb = moonCol * skyLit * clamp(-sunH, 0.0, 1.0);

    // Blocklight — Photon-style quadratic falloff with warm color
    float bl2       = blockLit * blockLit;
    float bl4       = bl2 * bl2;
    vec3 blockLight = (vec3(1.05, 0.58, 0.16) * bl4
                    + vec3(0.9, 0.5, 0.1) * bl2 * 0.18
                    + vec3(0.8, 0.4, 0.05) * blockLit * 0.06) * 0.9;

    // Cave minimum — never pure black indoors
    vec3 caveAmb = vec3(0.018, 0.018, 0.022) * (1.0 - skyLit);

    vec3 light = directSun + skyAmb + moonAmb + blockLight + caveAmb;
    light = mix(light, vec3(dot(light,vec3(0.33))) * 0.65, rainStrength * 0.45);

    vec3 finalColor = albedo.rgb * light;

    // Rim highlight (grazing angle sun bounce)
    vec3 vd  = normalize(worldPos);
    float rim = pow(max(1.0 - dot(normalize(normal), -vd), 0.0), 5.0);
    finalColor += rim * sunCol * NdotL * shadow * sunUp * 0.06;

    gl_FragData[0] = vec4(max(finalColor, vec3(0.0)), albedo.a);
}
