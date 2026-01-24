#version 330 core

// Vertex shader for thermal vision post-processing
// Simple fullscreen quad shader

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    texCoord = UV0;
}
