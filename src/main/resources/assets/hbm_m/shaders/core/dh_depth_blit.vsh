#version 150

// Fullscreen quad in NDC: depth is read directly from the DH depth texture by UV.
in vec3 Position;

out vec2 uv;

void main() {
    gl_Position = vec4(Position, 1.0);
    uv = Position.xy * 0.5 + 0.5;
}
