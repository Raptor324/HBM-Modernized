#version 120
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 normal;
uniform sampler2D texture;
uniform sampler2D lightmap;
uniform vec3 shadowLightPosition;
uniform vec3 sunPosition;
void main() {
    vec4 albedo = texture2D(texture, texcoord) * color;
    if (albedo.a < 0.1) discard;

    float sunUp  = clamp(normalize(sunPosition).y, 0.0, 1.0);
    float NdotL  = max(dot(normalize(normal), normalize(shadowLightPosition)), 0.0);
    vec2  lm     = clamp(lmcoord, 0.0, 1.0);
    float bl     = lm.x * lm.x;

    vec3 light = vec3(0.5 + 0.5 * NdotL) * sunUp
               + vec3(1.0, 0.6, 0.2) * bl * 1.5
               + vec3(0.25, 0.28, 0.38) * lm.y * (1.0 - sunUp)
               + vec3(0.04);

    gl_FragData[0] = vec4(albedo.rgb * light, albedo.a);
}
