#version 150

// КОПИЯ DH-ГЛУБИНЫ В ГЛАВНЫЙ Z-BUFFER.
// Комозит DH (apply.frag) переносит в главный FBO только ЦВЕТ LOD'ов,
// глубина там остаётся небом (1.0) => дальняя геометрия (меши ракет)
// нативным depth-тестом против LOD всегда «ближе». Этот проход читает
// DEPTH32F текстуру DH и пишет её в gl_FragDepth ГЛАВНОГО буфера.
//
// Конвертация: window-Z проекции DH -> дистанция -> window-Z нашей
// расширенной проекции (near=0.05, far=8e6 => ndcZ ~= 1 - 0.1/dist),
// чтобы сравнения с уже записанной ванильной геометрией и последующим
// дальним контентом были корректны.

uniform sampler2D Sampler0;
uniform float DhNear;
uniform float DhFar;
// Клип-плоскости НАШЕЙ расширенной проекции (куда кодируем глубину).
uniform float OutNear;
uniform float OutFar;
// Нижняя граница достоверности DH-глубины: репликация зоны dither-fade
// «Fade Nearby DH LODs». Внутри неё DEPTH32F — стохастический шум
// (bayer-discard террейн-шейдера DH), копировать нельзя.
uniform float DhFadeMaskDist;

in vec2 uv;
out vec4 fragColor;

void main() {
    float d = texture(Sampler0, uv).r;
    // Небо DH (LOD не рисовался): глубину не трогаем — остаётся 1.0.
    if (d >= 0.999999) {
        discard;
    }
    // Линеаризация forward-Z: ndc = ((F+N)d - 2FN) / ((F-N)d)  =>  d:
    float ndc = d * 2.0 - 1.0;
    float denom = (DhFar + DhNear) - ndc * (DhFar - DhNear);
    float dist = (2.0 * DhFar * DhNear) / max(denom, 1e-6);
    if (dist < DhFadeMaskDist) {
        discard;
    }
    // Точное окно НАШЕЙ проекции: window = 1 - fnEff/dist, fnEff = F*N/(F-N).
    // ВАЖНО: раньше стояло «1 - 0.1/dist» — ошибочный fnEff (правильно 0.05
    // для N=0.05/F=8e6), из-за чего каждый окклудер выглядел вдвое ближе и
    // резал гриб, находясь ЗА ним («гриб уезжает назад при отлёте»).
    float fnEff = (OutFar * OutNear) / (OutFar - OutNear);
    gl_FragDepth = clamp((1.0 - fnEff / dist) + 1.0e-6, 0.0, 1.0);
    fragColor = vec4(0.0);
}
