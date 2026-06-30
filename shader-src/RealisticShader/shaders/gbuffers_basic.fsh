#version 120
varying vec4 color;
varying vec3 normal;
uniform vec3 sunPosition;

void main() {
    vec4 out4 = color;
    // Cloud tinting: translucent flat geometry with vertical normal
    if (color.a > 0.05 && color.a < 0.98 && abs(normal.y) > 0.8) {
        float sunUp   = clamp(normalize(sunPosition).y, 0.0, 1.0);
        float golden  = 1.0 - smoothstep(0.0, 0.2, abs(normalize(sunPosition).y));
        golden       *= smoothstep(-0.05, 0.05, normalize(sunPosition).y);
        vec3 topTint  = mix(vec3(0.62,0.65,0.72), vec3(1.0,1.0,1.02), sunUp);
        topTint       = mix(topTint, vec3(1.05,0.82,0.55), golden);
        // Underside of clouds is darker/greyer
        float underside = clamp(1.0 - dot(normalize(normal), vec3(0.0,1.0,0.0)), 0.0, 1.0);
        vec3 bottomTint = topTint * mix(0.55, 0.8, sunUp);
        out4.rgb *= mix(topTint, bottomTint, underside);
    }
    gl_FragData[0] = out4;
}
