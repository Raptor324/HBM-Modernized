#version 120
varying vec4 color;
varying vec3 normal;
void main() {
    color  = gl_Color;
    normal = normalize(gl_NormalMatrix * gl_Normal);
    gl_Position = ftransform();
}
