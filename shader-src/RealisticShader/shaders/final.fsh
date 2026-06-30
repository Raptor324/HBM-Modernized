#version 120
#define BLOOM_STRENGTH 0.45 //[0.0 0.1 0.2 0.3 0.4 0.45 0.5 0.6 0.7 0.8 0.9 1.0 1.2 1.5]

varying vec2 texcoord;
uniform sampler2D colortex0;
uniform sampler2D colortex1;

void main() {
    vec3 color = texture2D(colortex0, texcoord).rgb;
    vec3 bloom = texture2D(colortex1, texcoord).rgb;

    color += bloom * BLOOM_STRENGTH;

    // Vignette
    vec2  c = texcoord - 0.5;
    color  *= 1.0 - dot(c,c) * 0.75;

    // Gamma
    color = pow(clamp(color, 0.0, 1.0), vec3(1.0/2.2));

    gl_FragColor = vec4(color, 1.0);
}
