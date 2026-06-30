#version 120
/* DRAWBUFFERS:1 */

varying vec2 texcoord;
uniform sampler2D colortex0;
uniform float viewWidth;
uniform float viewHeight;

vec3 brightPass(vec2 uv) {
    vec3 c = texture2D(colortex0, clamp(uv, 0.0, 1.0)).rgb;
    float l = dot(c, vec3(0.2126,0.7152,0.0722));
    return c * smoothstep(0.60, 1.0, l);
}

void main() {
    // Dual Kawase-inspired blur for softer, wider bloom
    vec2 tx = vec2(2.5/viewWidth, 2.5/viewHeight);
    vec3 b  = brightPass(texcoord)*4.0;
    b += brightPass(texcoord+vec2( tx.x, 0.0))*2.0;
    b += brightPass(texcoord+vec2(-tx.x, 0.0))*2.0;
    b += brightPass(texcoord+vec2( 0.0,  tx.y))*2.0;
    b += brightPass(texcoord+vec2( 0.0, -tx.y))*2.0;
    b += brightPass(texcoord+vec2( tx.x,  tx.y));
    b += brightPass(texcoord+vec2(-tx.x,  tx.y));
    b += brightPass(texcoord+vec2( tx.x, -tx.y));
    b += brightPass(texcoord+vec2(-tx.x, -tx.y));

    // Second wider pass
    vec2 tx2 = tx * 3.0;
    b += brightPass(texcoord+vec2( tx2.x, 0.0  ))*0.5;
    b += brightPass(texcoord+vec2(-tx2.x, 0.0  ))*0.5;
    b += brightPass(texcoord+vec2( 0.0,   tx2.y))*0.5;
    b += brightPass(texcoord+vec2( 0.0,  -tx2.y))*0.5;

    gl_FragData[0] = vec4(b / 18.0, 1.0);
}
