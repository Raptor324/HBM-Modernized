#version 150

#moj_import <fog.glsl>

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
in float vDhWinZ;

uniform sampler2D Sampler0;
// Depth-текстура DH (DEPTH32F, forward-Z). Активен только в дальнем
// проходе (DhDepthTest > 0.5): попиксельная окклюзия против LOD-рельефа.
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform float DhDepthTest;
uniform vec2 DhViewport;

out vec4 fragColor;

void main() {
    if (DhDepthTest > 1.5) {
        // ДИАГНОСТИКА: R = наш window-Z (DhProjMat*MVM*pos), G = сэмпл DH-глубины.
        // Оба канала меняются => сэмплер и математика работают.
        // G == 0 везде => сэмплер не привязан/читает мусор.
        // R == 0 или > 1 везде => DhProjMat не применился.
        fragColor = vec4(vDhWinZ, texture(Sampler1, gl_FragCoord.xy / DhViewport).r, 0.0, 1.0);
        return;
    }
    // ОККЛЮЗИЯ ПРОТИВ LOD — ТОЛЬКО через GL depth-тест против копии DH-глубины
    // (DhDepthCopy.copyToMain в главный z-buffer). Раньше здесь был ДОПОЛНИТЕЛЬНЫЙ
    // discard «vDhWinZ > lodDepth»: он дублировал GL-тест, но с худшей точностью
    // (сравнение window-Z разных конвенций) и резал гриб по НЕвидимым источникам
    // глубины DH — затуманенным дальним LOD'ам и собственным облакам DH — что
    // при движении камеры выглядело как «гриб улетает/отступает». При
    // несработавшем копировании глубины пустой Sampler1 вообще убивал весь гриб.
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
