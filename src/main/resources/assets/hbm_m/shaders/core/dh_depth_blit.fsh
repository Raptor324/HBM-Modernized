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
//
// ДВЕ КОНВЕНЦИИ ГЛУБИНЫ DH (DhReverseZ, запрос через RenderUtil.RENDER_DEF
// .getRenderDepth() — матрица события для этого НЕГОДНА, она всегда форвард):
//  - FORWARD_Z (DH <= 3.2.x, 3.3.1+ только под Iris-паком без reverse-Z):
//    близко=0, даль=1, небо=1. Стандартная перспектива:
//    ndc = 2d-1, dist = 2fn/((F+N) - ndc(F-N)).
//  - REVERSE_Z (DH 3.3.1+ по умолчанию без пака; GlDhRenderApiDefinition
//    .getRenderDepth(), glClearDepth = farDepth = 0): близко=1, даль=0.
//    Реверс-матрица DH (RenderUtil.setClipPlanes, байткод 3.3.1):
//    A = m22 = n/(f-n), B = m23 = fn/(f-n)  =>  ndc = A*(f/dist - 1),
//    откуда dist = f*n / (ndc*(f-n) + n) при обычном ndc = 2d-1.
//    Числовая проверка (n=73.41, f=7512.3): d=1000 -> decode 1000.2,
//    d=200 -> 200.02. Небо теперь d = 0.
//    (НЕВАЖНО: пробовавшийся раньше вариант «ndc = 1-2d + форвард-формула»
//    соответствовал СТАНДАРТНОЙ реверс-перспективе m22=(f+n)/(f-n), а у DH
//    своя, см. выше — с ней гриб так и оставался без окклюзии.)

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
// Конвенция глубины DH: > 0.5 = REVERSE_Z, иначе FORWARD_Z.
uniform float DhReverseZ;

in vec2 uv;
out vec4 fragColor;

void main() {
    float d = texture(Sampler0, uv).r;
    bool reverseZ = DhReverseZ > 0.5;
    // Небо DH (LOD не рисовался): глубину не трогаем. Конвенции противоположны.
    if (reverseZ ? (d <= 1.0e-6) : (d >= 0.999999)) {
        discard;
    }
    float ndc = d * 2.0 - 1.0;
    float dist = reverseZ
        ? (DhFar * DhNear) / max(DhNear + ndc * (DhFar - DhNear), 1.0e-6)
        : (2.0 * DhFar * DhNear) / max((DhFar + DhNear) - ndc * (DhFar - DhNear), 1.0e-6);
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
