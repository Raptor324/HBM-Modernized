#version 120
varying vec2 texcoord;
varying vec4 color;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    color    = gl_Color;
    gl_Position = ftransform();
}
