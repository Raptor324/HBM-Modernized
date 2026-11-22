#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    float alpha = tex.a * vertexColor.a;
    // Discard fully transparent pixels (flame/sprite background).
    if (alpha < 0.0039) {
        discard;
    }
    // Classic additive output: unmultiplied RGB with true alpha;
    // ADDITIVE_TRANSPARENCY (SRC_ALPHA, ONE) multiplies RGB by alpha internally.
    fragColor = vec4(tex.rgb * vertexColor.rgb * ColorModulator.rgb, alpha);
}
