#version 150

in vec3 Position;
in vec3 Normal;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec2 PackedLight; // 0..15

out vec3 fragNormal;
out vec2 texCoord;
out vec2 lightCoord;
out float vertexDistance;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    // ✅ Вычисляем расстояние до вершины (для fog и других эффектов)
    vertexDistance = length(viewPos.xyz);

    // Нормали
    fragNormal = normalize(mat3(ModelViewMat) * Normal);

    texCoord = UV0;

    // Перевод 0..15 в координаты 16×16 c центрированием к центру субпикселя
    lightCoord = (PackedLight + vec2(0.5)) / 16.0;
}
