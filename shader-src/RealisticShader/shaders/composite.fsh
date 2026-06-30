#version 120
#define EXPOSURE 1.0 //[0.4 0.5 0.6 0.7 0.8 0.9 1.0 1.1 1.2 1.3 1.4 1.5 1.6 1.8 2.0]

varying vec2 texcoord;

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform sampler2D shadowtex0;

uniform float near;
uniform float far;
uniform float rainStrength;
uniform float frameTimeCounter;
uniform float viewWidth;
uniform float viewHeight;

uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform vec3 upPosition;
uniform vec3 fogColor;
uniform vec3 skyColor;

uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowModelView;

// =====================================================================
// Utility
// =====================================================================
float linDepth(float d) {
    return (2.0*near*far) / (far+near - (d*2.0-1.0)*(far-near));
}

vec3 worldPos(vec2 uv, float d) {
    vec4 clip = vec4(uv*2.0-1.0, d*2.0-1.0, 1.0);
    vec4 view = gbufferProjectionInverse * clip;
    view     /= view.w;
    return (gbufferModelViewInverse * view).xyz;
}

float hash11(float p) {
    return fract(sin(p * 127.1) * 43758.5453);
}
float hash21(vec2 p) {
    vec3 q = fract(vec3(p.xyx)*0.1031);
    q += dot(q, q.yzx+33.33);
    return fract((q.x+q.y)*q.z);
}
float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x+p.y)*p.z);
}

// =====================================================================
// Analytical Atmosphere  (Rayleigh + Mie single-scatter approximation)
// =====================================================================
// Rayleigh coefficients (RGB, λ⁻⁴ weighting gives blue sky)
const vec3  bR = vec3(5.8e-6, 13.5e-6, 33.1e-6);
// Mie coefficient (slightly wavelength independent, greyer)
const float bM = 2.1e-5;
// Scale heights
const float HR = 8000.0;   // Rayleigh
const float HM = 1200.0;   // Mie

// Optical depth along a ray from surface to top of atmosphere
// Approximated via Chapman function: od ≈ H / max(mu, 0.02)
vec2 opticalDepth(float mu) {
    mu = max(mu, 0.01);
    return vec2(HR, HM) / mu;
}

// Henyey-Greenstein phase
float miePhase(float cosTheta, float g) {
    float g2 = g*g;
    return (1.0-g2) / (4.0*3.14159 * pow(max(1.0+g2 - 2.0*g*cosTheta, 0.0001), 1.5));
}

float rayleighPhase(float cosTheta) {
    return 0.75 * (1.0 + cosTheta*cosTheta);
}

// Full sky color from view direction + sun direction
vec3 atmosphere(vec3 dir, vec3 sunDir, float sunUp, float rain) {
    float mu        = max(dir.y, 0.001);        // sin(view elevation)
    float muS       = max(sunDir.y, -0.1);       // sin(sun elevation)
    float cosTheta  = dot(dir, sunDir);

    // Optical depths
    vec2  od        = opticalDepth(mu);
    vec2  odS       = opticalDepth(max(muS, 0.03));

    // Transmittance to space along view ray
    vec3  TR        = exp(-bR * od.x);
    float TM        = exp(-bM * od.y);

    // Sun transmittance (light arriving from sun direction)
    vec3  TRs       = exp(-bR * odS.x);
    float TMs       = exp(-bM * odS.y);

    // Phase functions
    float phR       = rayleighPhase(cosTheta);
    float phM       = miePhase(cosTheta, 0.76);

    // Scattered light
    vec3  scatter   = (bR * phR * TR + bM * phM * TM) * TRs * TMs;
    scatter        *= 22.0; // gain

    // Night: very dark residual sky
    scatter        = mix(scatter, scatter * 0.008 + vec3(0.004,0.006,0.012),
                         clamp(-sunDir.y * 5.0, 0.0, 1.0));

    // Rain: grey and dim
    float rainGrey  = dot(scatter, vec3(0.33));
    scatter         = mix(scatter, vec3(rainGrey)*0.55, rain*0.75);

    return scatter;
}

// Sun disc
vec3 sunDisc(vec3 dir, vec3 sunDir, float sunUp) {
    float d = dot(dir, sunDir);
    float disc = smoothstep(0.9997, 0.99985, d);
    return disc * vec3(6.0, 5.5, 4.5) * sunUp;
}

// Moon disc
vec3 moonDisc(vec3 dir, vec3 moonDir, float sunUp) {
    float d    = dot(dir, normalize(moonDir));
    float disc = smoothstep(0.9995, 0.9998, d);
    return disc * vec3(1.5, 1.6, 2.0) * (1.0-sunUp);
}

// =====================================================================
// Procedural stars (Photon-style hash + blackbody color)
// =====================================================================
vec3 blackbody(float t) {
    // Simplified Planck curve mapped to RGB, temp in [4000,9000] K
    vec3 c;
    t /= 100.0;
    c.r = t<=66.0 ? 1.0 : clamp(329.698727*(pow(t-60.0,-0.1332047))/255.0, 0.0, 1.0);
    c.g = t<=66.0 ? clamp((99.4708025*log(t)-161.1195681)/255.0,0.0,1.0)
                  : clamp(288.1221695*(pow(t-60.0,-0.0755148))/255.0,0.0,1.0);
    c.b = t>=66.0 ? 1.0 : (t<=19.0 ? 0.0 : clamp((138.5177312*log(t-10.0)-305.0447927)/255.0,0.0,1.0));
    return c;
}

vec3 stars(vec3 dir, float sunUp) {
    float nightBlend = clamp((-sunUp - 0.05)*8.0, 0.0, 1.0);
    if (nightBlend < 0.001) return vec3(0.0);

    // Project onto sphere grid
    float lat = asin(clamp(dir.y,-1.0,1.0));
    float lon = atan(dir.z, dir.x);
    vec2  uv  = vec2(lon / 6.28318, lat / 3.14159) * 200.0;
    vec2  cell = floor(uv);
    vec2  frac = fract(uv);

    vec3 col = vec3(0.0);
    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            vec2  nc  = cell + vec2(float(dx), float(dy));
            float h   = hash21(nc);
            vec2  pos = vec2(hash11(h), hash11(h*2.7+1.3)); // star position in cell
            float dist2 = dot(frac - vec2(float(dx),float(dy)) - pos,
                              frac - vec2(float(dx),float(dy)) - pos);
            float star = max(0.0, 0.004 - dist2) / 0.004;
            if (star > 0.0) {
                float temp  = mix(4500.0, 9000.0, hash11(h*3.1));
                float twinkle = 1.0 - 0.3 * cos(frameTimeCounter*2.0 + h*100.0);
                float bright = smoothstep(0.85, 1.0, h) * 1.8 * twinkle;
                col += blackbody(temp) * star * bright;
            }
        }
    }
    return col * nightBlend;
}

// =====================================================================
// SSAO
// =====================================================================
float ssao(vec2 uv, float linD) {
    if (linD > far*0.5) return 1.0;
    float rad  = 0.7 / max(linD, 1.0);
    float rot  = hash21(uv*vec2(viewWidth,viewHeight)) * 6.28318;
    float cr   = cos(rot), sr = sin(rot);

    const vec2 d0 = vec2(1.0,0.0); const vec2 d1 = vec2(-1.0,0.0);
    const vec2 d2 = vec2(0.0,1.0); const vec2 d3 = vec2(0.0,-1.0);
    const vec2 d4 = vec2(0.707,0.707); const vec2 d5 = vec2(-0.707,0.707);
    const vec2 d6 = vec2(0.707,-0.707); const vec2 d7 = vec2(-0.707,-0.707);

    float occ = 0.0;
    vec2 dirs[8];
    dirs[0]=vec2(d0.x*cr-d0.y*sr,d0.x*sr+d0.y*cr);
    dirs[1]=vec2(d1.x*cr-d1.y*sr,d1.x*sr+d1.y*cr);
    dirs[2]=vec2(d2.x*cr-d2.y*sr,d2.x*sr+d2.y*cr);
    dirs[3]=vec2(d3.x*cr-d3.y*sr,d3.x*sr+d3.y*cr);
    dirs[4]=vec2(d4.x*cr-d4.y*sr,d4.x*sr+d4.y*cr);
    dirs[5]=vec2(d5.x*cr-d5.y*sr,d5.x*sr+d5.y*cr);
    dirs[6]=vec2(d6.x*cr-d6.y*sr,d6.x*sr+d6.y*cr);
    dirs[7]=vec2(d7.x*cr-d7.y*sr,d7.x*sr+d7.y*cr);

    for (int i = 0; i < 8; i++) {
        float sd = linDepth(texture2D(depthtex0, uv + dirs[i]*rad).r);
        float diff = linD - sd;
        if (diff > 0.04 && diff < 2.0)
            occ += clamp(diff/2.0, 0.0, 1.0);
    }
    return clamp(1.0-(occ/8.0)*1.15, 0.18, 1.0);
}

// =====================================================================
// Volumetric light (Henyey-Greenstein scattering)
// =====================================================================
float vlLight(vec3 endPos, float dither) {
    const int S = 12;
    vec3  step = endPos / float(S);
    vec3  p    = step * dither;
    float acc  = 0.0;
    for (int i = 0; i < S; i++) {
        vec4 sc = shadowProjection * shadowModelView * vec4(p, 1.0);
        vec3 sv = sc.xyz * 0.5 + 0.5;
        if (sv.x>0.001&&sv.x<0.999&&sv.y>0.001&&sv.y<0.999&&sv.z<1.0)
            if (sv.z - 0.0015 < texture2D(shadowtex0, sv.xy).r)
                acc += 1.0;
        p += step;
    }
    return acc / float(S);
}

// =====================================================================
// ACES tonemap (Narkowicz approximation + saturation, per Photon spec)
// =====================================================================
vec3 tonemapACES(vec3 x) {
    const float a=2.51, b=0.03, c=2.43, d=0.59, e=0.14;
    return clamp((x*(a*x+b))/(x*(c*x+d)+e), 0.0, 1.0);
}

// =====================================================================
// Main
// =====================================================================
void main() {
    vec3  color = texture2D(colortex0, texcoord).rgb;
    float depth = texture2D(depthtex0, texcoord).r;
    bool  isSky = (depth >= 1.0);

    float sunH   = normalize(sunPosition).y;
    float sunUp  = clamp(sunH, 0.0, 1.0);
    float rain   = clamp(rainStrength, 0.0, 1.0);
    float golden = (1.0-smoothstep(0.0,0.22,abs(sunH))) * smoothstep(-0.06,0.04,sunH);

    vec3 sunDir = normalize(sunPosition);
    vec3 moonDir= normalize(moonPosition);

    if (isSky) {
        // --- Enhance vanilla sky with atmospheric effects ---
        // We KEEP the vanilla sky/cloud color as base (replacing it kills clouds).
        // Instead we additively layer atmosphere-inspired enhancements on top.
        vec3 p0  = worldPos(texcoord, 0.0001);
        vec3 p1  = worldPos(texcoord, 0.9999);
        vec3 dir = normalize(p1 - p0);

        // Atmospheric tint: shift vanilla sky toward our Rayleigh/Mie-based color.
        // Detect clouds by WHITENESS (low saturation), not brightness - a clear blue
        // sky can be just as bright as a cloud, so luma alone wrongly flagged plain
        // sky as "cloud" and suppressed almost all atmosphere blending.
        vec3 atmColor = atmosphere(dir, sunDir, sunUp, rain) * 0.85;
        float maxC = max(max(color.r, color.g), color.b);
        float minC = min(min(color.r, color.g), color.b);
        float saturation = (maxC - minC) / max(maxC, 0.001);
        float cloudFactor = 1.0 - smoothstep(0.08, 0.22, saturation); // low sat = white = cloud
        color = mix(atmColor, color, cloudFactor * 0.85);

        // Vertical gradient: deeper/darker blue toward zenith, lighter toward horizon
        float zenith = clamp(dir.y, 0.0, 1.0);
        vec3 zenithTint = mix(vec3(1.0), vec3(0.55, 0.65, 0.95), zenith * (1.0 - cloudFactor));
        color *= mix(vec3(1.0), zenithTint, 0.7);

        // Sun halo (additive, safe)
        float sdot = max(dot(dir, sunDir), 0.0);
        float halo = pow(sdot, 420.0)*0.5 + pow(sdot, 44.0)*0.05;
        color += halo * mix(vec3(1.0,0.80,0.45), vec3(1.0,0.96,0.88), sunUp) * sunUp * (1.0-rain);

        // Horizon brightening
        float horiz = pow(1.0-abs(dir.y), 8.0) * sunUp * 0.12 * (1.0-rain);
        color += horiz * mix(vec3(1.0,0.75,0.35), vec3(0.75,0.85,1.0), sunUp);

        // Stars (additive, only visible in dark sky areas at night)
        color += stars(dir, sunUp) * (1.0 - cloudFactor);

    } else {
        float linD = linDepth(depth);

        // SSAO
        color *= ssao(texcoord, linD);

        // Volumetric light shafts
        if (sunUp > 0.04 && rain < 0.92) {
            vec3  wp    = worldPos(texcoord, depth);
            float wl    = length(wp);
            vec3  wpC   = wp * (min(wl, 80.0) / max(wl, 0.0001));
            float dith  = fract(hash21(texcoord*vec2(viewWidth,viewHeight)) + frameTimeCounter*0.618);
            float vl    = vlLight(wpC, dith);

            // Henyey-Greenstein phase along view ray vs sun
            vec3  vdir  = normalize(worldPos(texcoord, 0.9999) - worldPos(texcoord, 0.0001));
            float cosV  = dot(vdir, sunDir);
            float hg    = miePhase(cosV, 0.5)*0.5 + miePhase(cosV,-0.2)*0.5;
            hg          = hg * 6.0 / (6.0 + 1.0); // normalize

            float fade  = smoothstep(0.03, 0.4, linD/far);
            vec3  vlCol = mix(vec3(1.05,0.68,0.22), vec3(1.0,0.96,0.88), sunUp);
            color      += vl * vlCol * sunUp * (1.0-rain) * fade * hg * 0.08;
        }

        // Aerial perspective / fog
        float fogNear = mix(0.62, 0.08, rain);
        float fogFar  = mix(0.99, 0.82, rain);
        float fogF    = clamp(smoothstep(fogNear, fogFar, linD/far), 0.0, mix(0.55, 0.92, rain));

        // Fog color from atmosphere at horizon
        vec3 horizDir = vec3(0.0, 0.02, 1.0);
        vec3 fogAtm   = atmosphere(normalize(horizDir), sunDir, sunUp, rain) * 0.8;
        fogAtm        = mix(fogAtm, vec3(0.50,0.52,0.55), rain*0.6);
        color         = mix(color, fogAtm, fogF);
    }

    // ---- Global color grading ----
    color *= EXPOSURE;

    // ACES
    float drive = mix(1.45, 1.7, golden*(1.0-rain));
    color = tonemapACES(color * drive);

    // Rain: desaturate + darken
    float luma = dot(color, vec3(0.2126,0.7152,0.0722));
    color = mix(color, vec3(luma)*0.6, rain*0.5);

    // Saturation — Photon uses ~1.45-1.6
    color = mix(vec3(luma), color, mix(1.55, 0.72, rain));

    // Golden-hour warm push
    color += vec3(0.065,-0.015,-0.075)*golden*(1.0-rain);
    color  = clamp(color, 0.0, 1.0);

    // Split-tone (Photon characteristic): shadows cool-blue, highlights warm-yellow
    color  = mix(color * vec3(0.86,0.93,1.12),
                 color * vec3(1.07,1.01,0.89), luma);

    gl_FragData[0] = vec4(color, 1.0);
}
