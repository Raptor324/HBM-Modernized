#version 150

// Фуллскрин-квад в NDC: глубину берём напрямую из DH depth-текстуры по UV.
in vec3 Position;

out vec2 uv;

void main() {
    gl_Position = vec4(Position, 1.0);
    uv = Position.xy * 0.5 + 0.5;
}
