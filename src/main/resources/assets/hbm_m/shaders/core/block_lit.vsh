#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

// Instance attributes
layout(location = 3) in vec4 InstMatRow0;
layout(location = 4) in vec4 InstMatRow1;
layout(location = 5) in vec4 InstMatRow2;
layout(location = 6) in vec4 InstMatRow3;
layout(location = 7) in float InstBrightness; //  ИЗМЕНЕНО: теперь просто яркость

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float Brightness; //  НОВОЕ: для non-instanced
uniform int UseInstancing;

out vec2 texCoord;
out float brightness; //  ИЗМЕНЕНО: передаем яркость
out float vertexDistance;
out vec3 fragNormal;

void main() {
    mat4 modelView;
    float bright;
    
    if (UseInstancing == 1) {
        modelView = mat4(InstMatRow0, InstMatRow1, InstMatRow2, InstMatRow3);
        bright = InstBrightness;
    } else {
        modelView = ModelViewMat;
        bright = Brightness;
    }
    
    vec4 viewPos = modelView * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;
    
    texCoord = UV0;
    brightness = bright;
    vertexDistance = length(viewPos.xyz);
    fragNormal = Normal;
}
