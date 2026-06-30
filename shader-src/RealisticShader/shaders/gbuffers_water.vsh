#version 120
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 color;
varying vec3 worldPos;
varying vec3 viewPos;
varying vec3 normal;
uniform mat4  gbufferModelViewInverse;
uniform float frameTimeCounter;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy / 256.0;
    color    = gl_Color;
    normal   = normalize(gl_NormalMatrix * gl_Normal);
    vec4 vp  = gl_ModelViewMatrix * gl_Vertex;
    worldPos = (gbufferModelViewInverse * vp).xyz;
    vec4 pos = gl_Vertex;
    // Animate top surface
    if (normal.y > 0.5) {
        float t   = frameTimeCounter * 0.75;
        pos.y += sin(worldPos.x * 0.55 + t) * 0.045
               + sin(worldPos.z * 0.70 + t * 1.25) * 0.03
               + sin((worldPos.x + worldPos.z) * 0.35 + t * 0.65) * 0.022;
    }
    vec4 mvp = gl_ModelViewMatrix * pos;
    viewPos  = mvp.xyz;
    gl_Position = gl_ProjectionMatrix * mvp;
}
