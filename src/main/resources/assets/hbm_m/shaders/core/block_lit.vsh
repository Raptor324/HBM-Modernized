#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

// Optimized Instance attributes (32 bytes total)
layout(location = 3) in vec3 InstPos;        // Position
layout(location = 4) in vec4 InstRot;        // Quaternion (x, y, z, w)
layout(location = 5) in float InstBrightness; // Light

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float Brightness;
uniform int UseInstancing;

out vec2 texCoord;
out float brightness;
out float vertexDistance;
out vec3 fragNormal;

// Функция для конвертации кватерниона в матрицу вращения 4x4
mat4 quatToMat4(vec4 q) {
    float xx = q.x * q.x;
    float yy = q.y * q.y;
    float zz = q.z * q.z;
    float xy = q.x * q.y;
    float xz = q.x * q.z;
    float yz = q.y * q.z;
    float wx = q.w * q.x;
    float wy = q.w * q.y;
    float wz = q.w * q.z;

    return mat4(
        1.0 - 2.0 * (yy + zz), 2.0 * (xy + wz),       2.0 * (xz - wy),       0.0,
        2.0 * (xy - wz),       1.0 - 2.0 * (xx + zz), 2.0 * (yz + wx),       0.0,
        2.0 * (xz + wy),       2.0 * (yz - wx),       1.0 - 2.0 * (xx + yy), 0.0,
        0.0,                   0.0,                   0.0,                   1.0
    );
}

void main() {
    mat4 modelView;
    float bright;
    
    // Переменная для матрицы вращения (нужна и для позиции, и для нормали)
    mat4 rotMatrix; 

    if (UseInstancing == 1) {
        // Вычисляем вращение ОДИН раз
        rotMatrix = quatToMat4(InstRot);
        
        mat4 translation = mat4(1.0);
        translation[3] = vec4(InstPos, 1.0);
        
        // Итоговая матрица: Сначала вращение, потом перенос
        modelView = translation * rotMatrix;
        bright = InstBrightness;
        
        // Нормаль вращаем готовой матрицей (cast to mat3 отбрасывает лишнее)
        fragNormal = mat3(rotMatrix) * Normal;
    } else {
        modelView = ModelViewMat;
        bright = Brightness;
        
        // Fallback для обычного рендера
        fragNormal = mat3(modelView) * Normal; 
    }
    
    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    
    texCoord = UV0;
    brightness = bright;
    vertexDistance = length(viewPos.xyz);
}