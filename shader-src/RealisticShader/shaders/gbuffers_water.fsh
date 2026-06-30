#version 120
#define WATER_REFLECT 1 //[0 1]

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 worldPos;
varying vec3 viewPos;
varying vec3 normal;

uniform sampler2D texture;
uniform sampler2D lightmap;
uniform vec3  sunPosition;
uniform float frameTimeCounter;
uniform float rainStrength;

float waveH(vec2 p, float t) {
    return sin(p.x*0.55+t)*0.045 + sin(p.y*0.70+t*1.25)*0.030 + sin((p.x+p.y)*0.35+t*0.65)*0.022;
}

vec3 waveNormal(vec3 wp) {
    float t = frameTimeCounter * 0.75;
    float e = 0.12;
    float h0 = waveH(wp.xz, t);
    float hx = waveH(wp.xz + vec2(e,0.0), t);
    float hz = waveH(wp.xz + vec2(0.0,e), t);
    return normalize(cross(normalize(vec3(0.0, hz-h0, e)), normalize(vec3(e, hx-h0, 0.0))));
}

void main() {
    vec3 wn      = normalize(mix(normal, waveNormal(worldPos), 0.88));
    vec3 vd      = normalize(-viewPos);
    float cosA   = max(dot(vd, wn), 0.0);
    float fresnel = mix(0.06, 1.0, pow(1.0 - cosA, 4.0));

    // Deep/shallow water color
    vec3 deep    = vec3(0.004, 0.028, 0.065);
    vec3 shallow = vec3(0.008, 0.10, 0.155);
    vec3 waterCol = mix(deep, shallow, 0.18);

    float sunUp  = clamp(normalize(sunPosition).y, 0.0, 1.0);
    float golden = 1.0 - smoothstep(0.0, 0.2, abs(normalize(sunPosition).y));
    golden *= step(0.0, normalize(sunPosition).y);
    vec3 sunCol  = mix(vec3(1.0,0.97,0.88), vec3(1.1,0.60,0.18), golden);

    float NdotL  = max(dot(wn, normalize(sunPosition)), 0.0);
    float spec   = pow(max(dot(reflect(-normalize(sunPosition), wn), vd), 0.0), 220.0);

    // Sky reflection gradient
    vec3 refDir  = reflect(-vd, wn);
    float skyG   = clamp(refDir.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 skyRef  = mix(vec3(0.55,0.62,0.72), vec3(0.18,0.38,0.78), skyG);
    skyRef       = mix(skyRef, vec3(0.48,0.50,0.56), rainStrength);

    vec3 lmCol   = texture2D(lightmap, clamp(lmcoord,0.0,1.0)).rgb;
    vec3 base    = waterCol * max(NdotL * 0.7 + 0.3, 0.0) * lmCol;

    vec3 finalColor = mix(base, skyRef * lmCol, fresnel * float(WATER_REFLECT));
    finalColor += sunCol * spec * sunUp * (1.0-rainStrength) * 1.8;
    finalColor = max(finalColor, deep * 0.4);

    float alpha = mix(0.88, 0.98, fresnel) * color.a;
    gl_FragData[0] = vec4(finalColor, alpha);
}
