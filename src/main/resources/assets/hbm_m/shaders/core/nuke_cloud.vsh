#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
// DH projection: for shader depth testing against LOD terrain.
// At this point ModelViewMat is the vanilla view matrix (rotation-only),
// which matches dhModelViewMatrix, so DhProjMat * ModelViewMat yields
// the DH clip space in which its DEPTH32F was recorded.
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
    // forward-Z window: [0..1], 1.0 = sky / DH clear value
    vDhWinZ = dhClip.z / dhClip.w * 0.5 + 0.5;
}
