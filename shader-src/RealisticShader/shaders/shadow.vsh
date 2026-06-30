#version 120
#define WAVING_STRENGTH 1.0 //[0.0 0.25 0.5 0.75 1.0 1.25 1.5 1.75 2.0]

varying vec2 texcoord;
varying vec4 color;
uniform float frameTimeCounter;
attribute vec4 mc_Entity;

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    color    = gl_Color;
    vec4 pos = gl_Vertex;
    if (mc_Entity.x == 10.0 || mc_Entity.x == 11.0) {
        float t    = frameTimeCounter * 1.3;
        float hFac = gl_MultiTexCoord0.y < 0.5 ? 1.0 : 0.25;
        pos.x += (sin(t+pos.x*0.7+pos.z*0.5)*0.055+sin(t*1.7+pos.x*1.2)*0.025)*WAVING_STRENGTH*hFac;
        pos.z += cos(t*0.9+pos.z*0.6)*0.04*WAVING_STRENGTH*hFac;
    }
    gl_Position = gl_ProjectionMatrix * gl_ModelViewMatrix * pos;
}
