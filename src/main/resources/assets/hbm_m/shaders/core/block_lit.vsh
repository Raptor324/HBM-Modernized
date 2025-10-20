#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 PackedLight;  // (blockLight, skyLight) в диапазоне 0-240

out vec3 fragNormal;
out vec2 texCoord;
out vec2 lightCoord;
out float vertexDistance;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    // Расстояние до вершины (для тумана)
    vertexDistance = length(viewPos.xyz);
    
    // Нормали (для возможного использования в освещении)
    fragNormal = normalize(mat3(ModelViewMat) * Normal);
    
    // UV текстуры
    texCoord = UV0;

    // Координаты lightmap: нормализуем 0-240 → [0, 1]
    lightCoord = PackedLight / 240.0;
}
