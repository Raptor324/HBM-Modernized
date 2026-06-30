#version 120
#define WAVING_STRENGTH 1.0 //[0.0 0.25 0.5 0.75 1.0 1.25 1.5 1.75 2.0]

varying vec2  texcoord;
varying vec2  lmcoord;
varying vec4  color;
varying vec3  normal;
varying vec3  worldPos;
varying vec4  shadowPos;
varying float viewDist;

uniform mat4  gbufferModelViewInverse;
uniform mat4  shadowProjection;
uniform mat4  shadowModelView;
uniform float frameTimeCounter;
attribute vec4 mc_Entity;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy / 256.0;
    color    = gl_Color;
    normal   = normalize(gl_NormalMatrix * gl_Normal);

    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    worldPos     = (gbufferModelViewInverse * viewPos).xyz;
    viewDist     = length(viewPos.xyz);

    vec4 pos = gl_Vertex;
    if (mc_Entity.x == 10.0 || mc_Entity.x == 11.0) {
        float t    = frameTimeCounter * 1.3;
        float hFac = gl_MultiTexCoord0.y < 0.5 ? 1.0 : 0.25;
        pos.x += (sin(t + worldPos.x*0.7 + worldPos.z*0.5)*0.055 + sin(t*1.7+worldPos.x*1.2)*0.025) * WAVING_STRENGTH * hFac;
        pos.z += cos(t*0.9 + worldPos.z*0.6) * 0.04 * WAVING_STRENGTH * hFac;
        if (mc_Entity.x == 11.0)
            pos.y += sin(t*2.1 + worldPos.y*0.8) * 0.018 * WAVING_STRENGTH * hFac;
    }

    gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * pos;
    vec4 wp = gbufferModelViewInverse * (gl_ModelViewMatrix * pos);
    shadowPos = shadowProjection * shadowModelView * wp;
}
