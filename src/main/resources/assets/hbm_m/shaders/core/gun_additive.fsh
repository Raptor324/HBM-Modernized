#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    float alpha = tex.a * vertexColor.a;
    // Полностью прозрачные пиксели (фон пламени/спрайтов) не трогаем вообще.
    if (alpha < 0.0039) {
        discard;
    }
    // Классический аддитивный вывод: RGB без премультипликации, честная альфа —
    // ADDITIVE_TRANSPARENCY (SRC_ALPHA, ONE) умножает RGB на альфу сам.
    fragColor = vec4(tex.rgb * vertexColor.rgb * ColorModulator.rgb, alpha);
}
