#version 120
varying vec2 texcoord;
varying vec4 color;
uniform sampler2D texture;
void main() {
    vec4 albedo = texture2D(texture, texcoord) * color;
    if (albedo.a < 0.1) discard;
    gl_FragData[0] = albedo;
}
