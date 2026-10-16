#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
// Проекция DH: для шейдерного depth-теста против LOD-рельефа.
// ModelViewMat в этот момент — ванильная view-матрица (rotation-only),
// она совпадает с dhModelViewMatrix, поэтому DhProjMat * ModelViewMat
// даёт клип-пространство DH, в котором записан его DEPTH32F.
uniform mat4 DhProjMat;

out float vertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;
out float vDhWinZ;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = length(Position);
    texCoord0 = UV0;
    vertexColor = Color;
    vec4 dhClip = DhProjMat * ModelViewMat * vec4(Position, 1.0);
    // forward-Z окно: [0..1], 1.0 = небо/клир DH
    vDhWinZ = dhClip.z / dhClip.w * 0.5 + 0.5;
}
