#version 150
in vec3 fragNormal;
in vec2 texCoord;
in vec2 lightCoord;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord);
    vec4 lightmapColor = texture(Sampler2, lightCoord);
    fragColor = vec4(color.rgb * lightmapColor.rgb, color.a);
    if (fragColor.a < 0.1) {
        discard;
    }
}
