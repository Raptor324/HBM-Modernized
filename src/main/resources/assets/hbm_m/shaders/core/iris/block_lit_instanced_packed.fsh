#version 330 core
// Iris ExtendedShader FSH для deferred-паков со схемой gbuffer "packed"
// (детект — IrisInstancedEncoders по исходнику gbuffers пака):
//   target.x = packUnorm2x8(albedo.rg)
//   target.y = packUnorm2x8(albedo.b, material_mask)
//   target.z = packUnorm2x8(encodeUnitVector(flat_normal))
//   target.w = packUnorm2x8(light_levels)
// Свет pak считает в deferred-композите — в gbuffer albedo НЕ умножается на
// lightmap. flat normal — в мировых осях (mat3(gbufferModelViewInverse)*normal
// на стороне пака). material_mask = 0: модовые блоки не отображены в
// block.properties пака, спец-материалы им и через pack-программу не назначаются.
// Побайтовая математика packing/oct-encode совместима с декодером пака
// (unpack_unorm_2x8: hi*256+lo в 16-бит unorm-канале).

in vec2 texCoord;
in vec2 lightLevels;
in vec3 worldNormal;
in float vFadeAlpha;

uniform sampler2D iris_Sampler0;

out vec4 fragColor;

// Два 8-битных байта (hi = v.x, lo = v.y) в 16-битный unorm-канал:
// value = (hi*256 + lo) / 65535, с округлением к ближайшему байту.
float packUnorm2x8(vec2 v) {
    v = clamp(v, vec2(0.0), vec2(1.0));
    vec2 bytes = floor(255.0 * v + 0.5);
    return dot(bytes, vec2(1.0 / 65535.0, 256.0 / 65535.0));
}

float packUnorm2x8(float x, float y) {
    return packUnorm2x8(vec2(x, y));
}

vec2 signNonZero(vec2 v) {
    return mix(vec2(-1.0), vec2(1.0), greaterThanEqual(v, vec2(0.0)));
}

// Октаздрическое кодирование единичного вектора в [0,1]^2:
// проекция сферы на октаэдр и разворот нижней полусферы по диагоналям.
vec2 encodeUnitVector(vec3 n) {
    vec2 p = n.xy * (1.0 / (abs(n.x) + abs(n.y) + abs(n.z)));
    p = n.z <= 0.0 ? ((1.0 - abs(p.yx)) * signNonZero(p)) : p;
    return 0.5 * p + 0.5;
}

void main() {
    vec4 baseColor = texture(iris_Sampler0, texCoord);
    float alpha = baseColor.a * vFadeAlpha;
    if (alpha < 0.1) {
        discard;
    }

    vec3 n = normalize(worldNormal);

    fragColor = vec4(
        packUnorm2x8(baseColor.rg),
        packUnorm2x8(baseColor.b, 0.0),
        packUnorm2x8(encodeUnitVector(n)),
        packUnorm2x8(lightLevels)
    );
}
