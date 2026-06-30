#version 120
varying vec2 texcoord;
varying vec4 color;
uniform sampler2D texture;
void main() {
    if (texture2D(texture, texcoord).a * color.a < 0.1) discard;
}
