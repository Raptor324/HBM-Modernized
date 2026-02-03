#version 150

uniform sampler2D DiffuseSampler; // Основной экран (мир)
uniform sampler2D ThermalSampler; // Наш буфер, где только сущности

in vec2 texCoord;

out vec4 fragColor;

// Функция для черно-белого фона
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    
    // Берем данные из буфера сущностей. Используем Level 0, чтобы избежать размытия мипмапов.
    vec4 thermalColor = textureLod(ThermalSampler, texCoord, 0.0);

    // 1. Обработка фона (делаем его тусклым и серым/синеватым)
    float sceneLuma = luma(sceneColor.rgb);
    vec3 bgColor = vec3(sceneLuma * 0.7); // Просто серый фон, чуть затемненный
    
    // Можно добавить синевы для "холода":
    // bgColor = bgColor * vec3(0.8, 0.8, 1.0); 

    // 2. Обработка сущностей
    vec3 finalColor = bgColor;

    // Проверяем, есть ли в этом пикселе сущность.
    // Если Alpha > 0.01 или есть какой-то цвет, значит мы попали в моба.
    bool isEntityHot = thermalColor.a > 0.01 || dot(thermalColor.rgb, vec3(1.0)) > 0.01;

    if (isEntityHot) {
        // ТУТ МЫ ДЕЛАЕМ ИХ БЕЛЫМИ
        // Игнорируем оригинальный цвет текстуры, просто заливаем цветом.
        
        // Можем использовать яркость оригинальной текстуры для объема, 
        // но ты просил как в исходном моде - "без зависимости от текстуры" для белизны.
        // Чтобы было совсем белое пятно:
        finalColor = vec3(1.0, 1.0, 1.0); 

        // Если хочешь "горячий" градиент (белый центр, красноватые края), можно так:
        // float brightness = luma(thermalColor.rgb);
        // vec3 hotColor = mix(vec3(1.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0), brightness);
        // finalColor = hotColor;
    }

    fragColor = vec4(finalColor, 1.0);
}