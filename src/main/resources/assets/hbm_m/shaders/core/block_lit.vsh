#version 330 core

// Per-vertex attributes
layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

// Instance attributes (используются только если UseInstancing == 1)
layout(location = 3) in vec4 InstMatRow0;
layout(location = 4) in vec4 InstMatRow1;
layout(location = 5) in vec4 InstMatRow2;
layout(location = 6) in vec4 InstMatRow3;
layout(location = 7) in vec2 InstLight;

// Uniforms
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 PackedLight;
uniform int UseInstancing; // 0 = uniform mode, 1 = instanced mode

// Output
out vec2 texCoord;
out vec2 lightCoord;
out float vertexDistance;
out vec3 fragNormal;

void main() {
    mat4 modelView;
    vec2 light;
    
    if (UseInstancing == 1) {
        // Instanced rendering: используем instance attributes
        modelView = mat4(InstMatRow0, InstMatRow1, InstMatRow2, InstMatRow3);
        light = InstLight;
    } else {
        // Non-instanced rendering: используем uniforms
        modelView = ModelViewMat;
        light = PackedLight;
    }
    
    // Трансформация
    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    vertexDistance = length(viewPos.xyz);
    texCoord = UV0;
    
    // ИСПРАВЛЕНО: light уже в диапазоне [0,1], используем напрямую
    // Minecraft передаёт координаты в виде (blockLight, skyLight) в [0,1]
    lightCoord = clamp(light, 0.0005, 0.9995);
    
    fragNormal = normalize(mat3(modelView) * Normal);
}
