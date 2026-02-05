#version 150

// Постпроцессинговый шейдер тепловизора.
// Использует:
//  - DiffuseSampler: уже отрендеренный мир
//  - ThermalSampler: наш отдельный буфер с "горячими" сущностями (белые силуэты)

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalSampler;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

// Линейная яркость
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

// Простейший хеш для псевдослучайных значений (для шумовых полос)
float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 78.233))) * 43758.5453);
}

// Упрощённый блюр из старого thermal_vision.fsh:
// лёгкое размытие для подавления текстур, но с сохранением деталей.
vec3 blurSample(sampler2D tex, vec2 uv, vec2 texelSize) {
    // Более сильный 3x3‑блюр, чтобы скрыть "квадратики" текстур,
    // но всё ещё достаточно дешёвый для постпроцесса.
    vec3 sum = vec3(0.0);
    float weight = 0.0;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(x, y) * texelSize;
            float w = (x == 0 && y == 0) ? 4.0 : 1.0; // центр весим сильнее
            sum += texture(tex, uv + offset).rgb * w;
            weight += w;
        }
    }

    return sum / weight;
}

void main() {
    // Маска "горячих" сущностей
    vec4 thermalMask = textureLod(ThermalSampler, texCoord, 0.0);
    bool isEntityHot = thermalMask.a > 0.01 || dot(thermalMask.rgb, vec3(1.0)) > 0.01;

    // Размер экрана берём из основного буфера
    vec2 texSize = vec2(textureSize(DiffuseSampler, 0));

    // Пикселизация в экранном пространстве — ретро‑эффект тепловизора
    float pixelSize = 2.0;
    vec2 fragCoord = gl_FragCoord.xy;
    vec2 pixelated = floor(fragCoord / pixelSize) * pixelSize + (pixelSize * 0.5);
    vec2 uv = clamp(pixelated / texSize, vec2(0.001), vec2(0.999));

    vec2 texelSize = vec2(1.0 / texSize.x, 1.0 / texSize.y);

    // Оригинальный цвет и яркость мира
    vec3 originalColor = texture(DiffuseSampler, uv).rgb;
    float originalLuminance = luma(originalColor);

    // Более сильный блюр мира для подавления "квадратных" текстур
    vec3 blurred = blurSample(DiffuseSampler, uv, texelSize);

    // Для простоты считаем, что всегда "ночной" режим (state=0),
    // как в старом шейдере ночью.
    float state = 0.0;

    // Яркость для термал‑канала берём из размытого мира,
    // чтобы формы ландшафта читались, но текстурные детали сгладились.
    float luminance = luma(blurred);

    // Усиление яркости в тёмных областях.
    // Чуть менее агрессивно, чем в самом первом варианте,
    // но сильнее, чем в прошлом шаге, чтобы ночью не было "true darkness".
    float brightnessBoost = 1.0;
    float darkFactor = 1.0 - smoothstep(0.0, 0.3, originalLuminance);
    brightnessBoost = 1.0 + darkFactor * 1.5;

    luminance = clamp(luminance * brightnessBoost, 0.0, 1.0);

    // Квантизация яркости — "ступенчатый" серый как у тепловизора
    float quantized = floor(luminance * 12.0) / 12.0;

    // Повышенный контраст вокруг середины
    quantized = 0.5 + (quantized - 0.5) * 1.0;

    // Подсветка "горячих" зон по яркости сцены
    float heat = pow(quantized, 0.45);
    heat = smoothstep(0.35, 0.9, heat);
    float hotspotStrength = 0.70;
    vec3 hotspot = vec3(heat * hotspotStrength);

    // Базовая серость фона
    float ambient = 0.25;
    float baseMultiplier = 1.1;
    vec3 baseThermal = vec3(quantized) * (ambient + baseMultiplier);

    // Итоговый B/W‑термал до доп. эффектов
    vec3 thermal = clamp(baseThermal + hotspot, 0.0, 1.0);

    // Горизонтальные скан‑линии (с анимацией по времени).
    float scanlinePeriod = 12.0;
    float scanlineWidth = 0.33;
    // Смещаем координату по Y с течением времени, чтобы полосы "ползли".
    float animatedY = gl_FragCoord.y + Time * 40.0;
    float scanline = step(1.0 - scanlineWidth, fract(animatedY / scanlinePeriod)) * 0.06;
    thermal -= vec3(scanline);

    // Дополнительные хаотичные серые полоски поверх всего:
    // на каждом "ряду" по Y с небольшой вероятностью добавляем шумовую линию.
    float band = floor(gl_FragCoord.y / 8.0);          // высота "полосы"
    float noiseVal = rand(vec2(band, floor(Time * 3.0)));
    if (noiseVal > 0.96) {
        // Чем выше noiseVal, тем ярче полоса, но она остаётся довольно тонкой.
        float stripeIntensity = (noiseVal - 0.96) / 0.04; // 0..1
        thermal += vec3(0.05 * stripeIntensity);
    }


    float nightLift = 0.28;
    float darkMask = 1.0 - smoothstep(0.0, 0.40, thermal.r);
    thermal = thermal + vec3(nightLift * darkMask);
    
    // Финальная гамма‑коррекция.
    // Берём чуть меньшую гамму, чтобы итоговая картинка была заметно светлее ночью.
    float gamma = 0.75;
    thermal = pow(thermal, vec3(gamma));
    
    // Жёсткий нижний порог яркости — гарантируем, что экран никогда не становится слишком тёмным.
    // Это имитирует поведение ПНВ: даже в полной темноте мир виден в тускло‑серых тонах.
    float minBrightness = 0.25;
    thermal = max(thermal, vec3(minBrightness));

    //Виньетка
    // Чуть более заметное затемнение краёв, чтобы взгляд тянуло к центру.
    vec2 centered = (uv - 0.5) * vec2(texSize.x / texSize.y, 1.0);
    float r = length(centered);
    // r ≈ 0 в центре, растёт к углам. На краях уменьшаем яркость примерно до ~55–60%.
    float vignetteStrength = 0.5;
    float edgeFactor = smoothstep(0.4, 0.9, r); // 0 в центре, 1 ближе к углам
    float vignette = 1.0 - vignetteStrength * edgeFactor;
    thermal *= vignette;

    // Силуэты сущностей из нашего буфера — всегда чисто белые
    if (isEntityHot) {
        thermal = vec3(1.0);
    }

    fragColor = vec4(clamp(thermal, 0.0, 1.0), 1.0);
}