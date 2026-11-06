#version 330 core

in vec2 texCoord;
in vec2 lightmapUV;
in vec4 vColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

out vec4 fragColor;

void main() {
    vec4 baseColor = texture(Sampler0, texCoord);
    // Vanilla dynamic lightmap (same texture the block_lit path uses): encodes
    // sky darken, client brightness (gamma), night vision and dimension tint.
    vec3 lm = texture(Sampler2, lightmapUV).rgb;

    vec4 c = baseColor * vColor;
    float alpha = c.a;
    if (alpha < 0.01) {
        discard;
    }
    fragColor = vec4(c.rgb * lm, alpha);
}
