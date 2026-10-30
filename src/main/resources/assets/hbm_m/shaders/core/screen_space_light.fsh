#version 150
uniform sampler2D uDepthTex0;
uniform sampler2D uDhDepthTex;
uniform mat4 uProj;
uniform mat4 uModelView;
uniform mat4 uModelViewInv;
uniform vec3 uCamPos;
uniform vec4 uSources[4];
uniform int uSourceCount;
uniform float uSrcRadius[4];
// Непрерывные параметры ударной волны (CPU, nanoTime-трекер):
// x = радиус 3D-сферы волны (блоки), y = затухание силы [0..1].
uniform vec2 uSrcShock[4];
uniform float uIntensity;
uniform float uHeatScale;
uniform float uDebugReplace;
uniform float uSolidAdd;
uniform float uDhReverseZ;
uniform float uDhPresent;
uniform float uDhNear;
uniform float uDhFar;
uniform float uDhFadeDist;
uniform mat4 uProjInv;
// Кинематографический слой: копия цвета сцены (альбедо-освещение,
// рефракция ударной волны) и его сила (0 = классический аддитив).
uniform sampler2D uSceneColor;
uniform float uHasScene;
uniform float uCine;
// Граница ванильной геометрии, блоки (DhOcclusionGpu.vanillaFarPlane =
// (RD+1)*16): под-пороговый контент главного RT дальше неё - не
// геометрия (F-1, round-5).
uniform float uVanillaFar;
uniform vec2 uResolution;
uniform float uNormalScale;
in vec2 uv;
out vec4 fragColor;

const int   SHADOW_STEPS     = 16;
const float SHADOW_SKIP_TAIL = 0.2;
const float SHADOW_THICKNESS = 1.5;
// Полностью затенённая поверхность: остаточный множитель.
const float SHADOW_FLOOR     = 0.04;
// Нижняя обёртка ndl.
// Воздушная подсветка: 8-шаговый march по лучу взгляда - только
// небо/DH-пусто и только без кинематики (там godrays).
const int   AIR_MARCH_STEPS     = 8;
const float AIR_OCCLUDE_RESIDUE = 0.03;

// Линеаризация DH-глубины. На вход dDh в [0..1]: reverse-Z (1 = near,
// 0 = far) или стандартный Z (через ndc). Раньше в reverse-Z ветку
// подставляли ndc [-1..1]: отрицательный знаменатель зажимался
// max(...,1e-6) и все лоды дальше ~15 блоков декодировались в ~3e10
// блоков - подсветка лодов гасла полностью.
float decodeDhLinearZ(float dDh) {
    if (uDhReverseZ > 0.5) {
        return (uDhFar * uDhNear) / max(uDhNear + dDh * (uDhFar - uDhNear), 1e-6);
    }
    float ndc = dDh * 2.0 - 1.0;
    return (2.0 * uDhFar * uDhNear) / max((uDhFar + uDhNear) - ndc * (uDhFar - uDhNear), 1e-6);
}

bool dhLayerEmpty(float dDh) {
    return uDhReverseZ > 0.5 ? (dDh <= 1.0e-6) : (dDh >= 0.999999);
}

// Реконструкция DH-лода на луче пикселя: dist - линейная дистанция;
// точка np на near-плоскости MC-проекции по построению имеет
// z_view = -near, поэтому масштаб dist / -nearView.z.
vec3 dhViewPos(vec2 suv, float dDh) {
    float dist = decodeDhLinearZ(dDh);
    vec4 np = uProjInv * vec4(suv * 2.0 - 1.0, -1.0, 1.0);
    vec3 nearView = np.xyz / np.w;
    return nearView * (dist / max(-nearView.z, 1.0e-6));
}

// ЕДИНЫЙ декодер сцены (main, нормали, raymarch-тени, godrays):
// ванильная ветка с F-1 фильтром uVanillaFar, затем DH-слой.
vec3 sampleSceneViewPos(vec2 suv, out bool isDh) {
    isDh = false;
    float d0 = texture(uDepthTex0, suv).r;
    if (d0 < 0.999999) {
        vec4 vp = uProjInv * vec4(suv * 2.0 - 1.0, d0 * 2.0 - 1.0, 1.0);
        vec3 p = vp.xyz / vp.w;
        if (length(p) <= uVanillaFar) {
            return p;
        }
    }
    if (uDhPresent > 0.5) {
        float dDh = texture(uDhDepthTex, suv).r;
        if (!dhLayerEmpty(dDh)) {
            isDh = true;
            return dhViewPos(suv, dDh);
        }
    }
    return vec3(0.0);
}

// Декодер воздушного марша: ваниль с отсечкой uVanillaFar, DH с
// СОХРАНЁННЫМ гейтом uDhFadeDist (кольцо round-2 остаётся закрытым).
vec3 layerViewPosAir(vec2 suv) {
    float d = texture(uDepthTex0, suv).r;
    if (d < 0.999999) {
        vec4 vp = uProjInv * vec4(suv * 2.0 - 1.0, d * 2.0 - 1.0, 1.0);
        vec3 p = vp.xyz / vp.w;
        if (length(p) > uVanillaFar) {
            return vec3(0.0);
        }
        return p;
    }
    if (uDhPresent > 0.5) {
        float dDh = texture(uDhDepthTex, suv).r;
        if (!dhLayerEmpty(dDh)) {
            if (decodeDhLinearZ(dDh) < uDhFadeDist) {
                return vec3(0.0);
            }
            return dhViewPos(suv, dDh);
        }
    }
    return vec3(0.0);
}

// ── Воздушная подсветка: замкнутый интеграл (1-x/R)^2 по хорде ──
float airAnti(float u, float a2, float ap, float R2, float invR) {
    float x = sqrt(a2 + u * u);
    return u + (a2 * u + u * u * u / 3.0) / R2
         - invR * 0.5 * (x * u + ap * ap * log(x + u + ap * 1.0e-6));
}

float airTerm(vec3 rel, vec3 dir, float R, float L, out float uMax, out float t) {
    uMax = 0.0;
    t = max(dot(rel, dir), 0.0);
    float a2 = max(dot(rel, rel) - t * t, 0.0);
    float a = sqrt(a2);
    if (a >= R) {
        return 0.0;
    }
    float ap = max(a, 1.0e-4);
    uMax = sqrt(max(R * R - a2, 0.0));
    float u0 = clamp(-t, -uMax, uMax);
    float u1 = clamp(min(L, t + uMax) - t, -uMax, uMax);
    if (u1 <= u0) {
        return 0.0;
    }
    float R2 = R * R;
    float invR = 2.0 / R;
    return max(airAnti(u1, a2, ap, R2, invR) - airAnti(u0, a2, ap, R2, invR), 0.0);
}

float airSoftCap(float air, float cap) {
    return cap * (1.0 - exp(-air / max(cap, 1.0e-4)));
}

// РАЗДЕЛЬНЫЕ кривые времени (репорт боя: ореол прятал гриб, а земля
// гасла вместе с ним):
// 1) Воздушный ореол - короткая детонационная вспышка (первые секунды,
//    heat > 0.72) + деликатный фоновый зарев ПОД частицами гриба.
float airFlashCurve(float h) {
    float baseGlow = h * h * 0.42;
    float initialFlash = pow(max(h - 0.72, 0.0) / 0.28, 2.0) * 0.95;
    return baseGlow + initialFlash;
}

// 2) Поверхностный свет земли и лодов - мощный и долгий, всю жизнь гриба.
float groundFlashCurve(float h) {
    return pow(h, 1.25) * 1.65;
}

// Цветовая температура взрыва (спектр чёрного тела, огрубленный):
// coreFactor 0 = край радиуса / остывание, 1 = ядро при пике heat.
vec3 nukeColor(float coreFactor, float heat) {
    vec3 flashWhite  = vec3(0.92, 0.96, 1.00);
    vec3 coreGold    = vec3(1.00, 0.82, 0.45);
    vec3 midOrange   = vec3(1.00, 0.42, 0.10);
    vec3 edgeCrimson = vec3(0.70, 0.12, 0.03);
    vec3 ramp = mix(edgeCrimson, midOrange, smoothstep(0.0, 0.42, coreFactor));
    ramp = mix(ramp, coreGold, smoothstep(0.42, 0.85, coreFactor));
    ramp = mix(ramp, flashWhite, smoothstep(0.78, 1.0, coreFactor * heat));
    return ramp;
}

// Хеньи-Гринштейн, нормирован к 1 вперёд: взгляду НА взрыв - полная
// яркость ореола, от взрыва - доли (Mie-кастинг зарева).
float miePhaseNorm(float cosTheta, float g) {
    float g2 = g * g;
    float f = (1.0 - g2)
            / (12.5663706 * pow(max(1.0 + g2 - 2.0 * g * cosTheta, 1.0e-4), 1.5));
    float f0 = (1.0 - g2) / (12.5663706 * (1.0 - g) * (1.0 - g));
    return clamp(f / max(f0, 1.0e-6), 0.0, 4.0);
}

// Окно видимости гриба: в эллипсоиде ножки/шапки (частицы NukeTorex
// уже нарисованы в uSceneColor, глубину НЕ пишут = класс «небо»)
// плавно убираем накладываемый туман, оставляя корону вокруг гриба.
// В первые доли секунды (heat > 0.82) вспышка пробивает окно.
float mushroomCloudWindow(vec3 relToSrc, vec3 rayDir, float R, float heat) {
    float t = max(dot(relToSrc, rayDir), 0.0);
    vec3 closestPt = rayDir * t - relToSrc;
    vec2 ellip = vec2(length(closestPt.xz) / (R * 0.22), abs(closestPt.y) / (R * 0.26));
    float dCloud = length(ellip);
    float flashBypass = smoothstep(0.82, 0.98, heat) * 0.65;
    return mix(0.14 + flashBypass, 1.0, smoothstep(0.35, 1.25, dCloud));
}

// Экранные godrays (кинематика): 12 шагов по экрану от пикселя к
// проекции центра взрыва. Шаг пропускает свет, если сэмпл - небо
// или дальше центра; горы/крыши блокируют = сумеречные лучи.
float godrays(vec2 suv0, vec2 srcUv, float srcDist, float dither) {
    vec2 delta = (srcUv - suv0) / 12.0;
    float illum = 0.0;
    float wsum = 0.0;
    float w = 1.0;
    for (int i = 0; i < 12; i++) {
        vec2 suv = suv0 + delta * (float(i) + dither);
        wsum += w;
        if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
            illum += w;
        } else {
            bool grDh;
            vec3 sp = sampleSceneViewPos(suv, grDh);
            if (dot(sp, sp) < 1.0e-12 || length(sp) > srcDist - 8.0) {
                illum += w;
            }
        }
        w *= 0.92;
    }
    return illum / max(wsum, 1.0e-4);
}

// Окклюзия зарева (БЕЗ кинематики): 8 шагов вдоль луча взгляда.
float airMarchOcc(vec3 rayDir, float t, float uMax, float dither) {
    float s0 = max(0.0, t - uMax);
    float s1 = t + uMax;
    int blocked = 0;
    for (int j = 0; j < AIR_MARCH_STEPS; j++) {
        float sj = s0 + (float(j) + dither) * (s1 - s0) / float(AIR_MARCH_STEPS);
        vec3 sp = uCamPos + rayDir * sj;
        vec4 spView = uModelView * vec4(sp - uCamPos, 1.0);
        if (spView.w <= 0.0) {
            continue;
        }
        vec4 clip = uProj * spView;
        vec3 ndc = clip.xyz / clip.w;
        vec2 suv = ndc.xy * 0.5 + 0.5;
        if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
            continue;
        }
        vec3 spLayer = layerViewPosAir(suv);
        if (dot(spLayer, spLayer) > 1.0e-12 && length(spLayer) < sj - 1.5) {
            blocked++;
        }
    }
    return float(blocked) / float(AIR_MARCH_STEPS);
}

// Мягкие тени: 16-шаговый march вдоль луча пиксель->источник.
float shadowSoft(vec3 worldPos, vec3 dirToSrc, float dist, float marchSin) {
    float stepLen = dist * (1.0 - SHADOW_SKIP_TAIL) / float(SHADOW_STEPS);
    float stride = sqrt(marchSin * marchSin + 0.0625);
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    int unblocked = 0;
    for (int s = 0; s < SHADOW_STEPS; s++) {
        vec3 sp = worldPos + dirToSrc * (stepLen * (float(s) + dither) / stride);
        vec4 spView = uModelView * vec4(sp - uCamPos, 1.0);
        if (spView.w <= 0.0) {
            unblocked++;
            continue;
        }
        float rayDist = length(spView.xyz);
        vec4 clip = uProj * spView;
        vec3 ndc = clip.xyz / clip.w;
        vec2 suv = ndc.xy * 0.5 + 0.5;
        if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0) {
            unblocked++;
            continue;
        }
        bool tapDh;
        vec3 spLayer = sampleSceneViewPos(suv, tapDh);
        float sceneDist = length(spLayer);
        if (dot(spLayer, spLayer) > 1.0e-12 && sceneDist < rayDist - SHADOW_THICKNESS) {
            // блокер ближе к камере, чем шаг (минус толщина)
        } else {
            unblocked++;
        }
    }
    return float(unblocked) / float(SHADOW_STEPS);
}

void main() {
    if (uDebugReplace > 0.5) {
        fragColor = vec4(1.0, 0.0, 1.0, 1.0);
        return;
    }
    if (uSolidAdd > 0.5) {
        fragColor = vec4(0.5, 0.25, 0.1, 1.0);
        return;
    }

    bool fromDh;
    vec3 viewPos = sampleSceneViewPos(uv, fromDh);
    bool geometry = dot(viewPos, viewPos) > 1.0e-12;

    vec4 farView = uProjInv * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec3 rayDir = normalize((uModelViewInv * vec4(farView.xyz / farView.w, 0.0)).xyz);
    float airDither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));

    // Кинематика: поверхностный (альбедо), зарево и пыльное кольцо
    // копятся РАЗДЕЛЬНО; рефракция - экранное смещение копии сцены.
    vec3 surfAccum = vec3(0.0);
    vec3 glowAccum = vec3(0.0);
    vec3 ringAccum = vec3(0.0);
    float refrAmp = 0.0;
    vec2 refrDir = vec2(0.0);

    if (geometry) {
        vec3 worldPos = uCamPos + (uModelViewInv * vec4(viewPos, 1.0)).xyz;
        float viewDist = length(viewPos);
        // Нормаль: 5-таповый min-of-diffs (±x, ±y) - на каждой оси
        // берётся сосед с меньшей разностью глубины.
        float ox = max(1.0, viewDist * uNormalScale / uResolution.x);
        float oy = max(1.0, viewDist * uNormalScale / uResolution.y);
        bool dT1; bool dT2; bool dT3; bool dT4;
        vec3 pxl = sampleSceneViewPos(uv + vec2(-ox / uResolution.x, 0.0), dT1);
        vec3 pxr = sampleSceneViewPos(uv + vec2( ox / uResolution.x, 0.0), dT2);
        vec3 pyd = sampleSceneViewPos(uv + vec2(0.0, -oy / uResolution.y), dT3);
        vec3 pyu = sampleSceneViewPos(uv + vec2(0.0,  oy / uResolution.y), dT4);
        bool hasL = dot(pxl, pxl) > 1.0e-12;
        bool hasR = dot(pxr, pxr) > 1.0e-12;
        bool hasD = dot(pyd, pyd) > 1.0e-12;
        bool hasU = dot(pyu, pyu) > 1.0e-12;
        vec3 dX = vec3(0.0);
        vec3 dY = vec3(0.0);
        if (hasL && hasR) {
            dX = length(pxl - viewPos) <= length(pxr - viewPos) ? viewPos - pxl : pxr - viewPos;
        } else if (hasR) {
            dX = pxr - viewPos;
        } else if (hasL) {
            dX = viewPos - pxl;
        }
        if (hasD && hasU) {
            dY = length(pyd - viewPos) <= length(pyu - viewPos) ? viewPos - pyd : pyu - viewPos;
        } else if (hasU) {
            dY = pyu - viewPos;
        } else if (hasD) {
            dY = viewPos - pyd;
        }
        vec3 nView;
        if (dot(dX, dX) > 1.0e-12 && dot(dY, dY) > 1.0e-12) {
            nView = cross(dX, dY);
        } else {
            nView = vec3(0.0, 0.0, 1.0); // плоско к камере
        }
        nView = normalize(nView);
        if (dot(nView, viewPos) > 0.0) {
            nView = -nView; // нормаль смотрит к камере
        }
        vec3 nWorld = normalize((uModelViewInv * vec4(nView, 0.0)).xyz);
        float wDist = smoothstep(0.5 * uVanillaFar, uVanillaFar, viewDist);
        nWorld = normalize(mix(nWorld, vec3(0.0, 1.0, 0.0), wDist));

        for (int i = 0; i < uSourceCount; i++) {
            float heatA = clamp(uSources[i].w / uHeatScale, 0.0, 1.0);
            float airFlash = airFlashCurve(heatA);
            float groundFlash = groundFlashCurve(heatA);
            float RA = uSrcRadius[i];
            vec3 relA = uSources[i].xyz - uCamPos;

            // 1. Воздушная дымка до геометрии (с окном видимости гриба)
            float tA;
            float uMaxA;
            float airRaw = airTerm(relA, rayDir, RA, viewDist, uMaxA, tA);
            float airNorm = airRaw * 3.0 / (2.0 * RA);
            float cosTA = dot(rayDir, normalize(relA));
            float cloudWin = mushroomCloudWindow(relA, rayDir, RA, heatA);
            vec3 airCol = nukeColor(clamp(1.0 - sqrt(max(dot(relA, relA) - tA * tA, 0.0)) / RA, 0.0, 1.0), heatA);
            float air = airNorm * airNorm * airFlash * uIntensity * cloudWin
                    * mix(0.28, 0.85, miePhaseNorm(cosTA, 0.55));
            glowAccum += airCol * airSoftCap(air, 0.28 * uIntensity * max(airFlash, 0.6));

            // 2. Поверхностный свет земли и лодов. Купольный
            // Wrap-Lambert: угол падения считается на ВЕРХКУПКУ гриба
            // (~0.14*R выше центра) - иначе на дальних лодах
            // ndl = lift/dist ~ 0.06 и земля мёртвая.
            vec3 toSrc = uSources[i].xyz - worldPos;
            float dist = length(toSrc);
            vec3 domeToSrc = toSrc + vec3(0.0, RA * 0.14, 0.0);
            vec3 dirToSrc = toSrc / max(dist, 1e-4);
            vec3 dirToDome = normalize(domeToSrc);

            float fallLin = clamp(1.0 - dist / RA, 0.0, 1.0);
            float fall = mix(fallLin, fallLin * fallLin, 0.52);
            float gate = fall * groundFlash;
            if (gate > 0.0) {
                float rawNdl = clamp(dot(nWorld, dirToDome), 0.0, 1.0);
                float ndl = mix(0.34, 1.0, rawNdl);

                float graze = dot(worldPos - uCamPos, dirToSrc) / max(viewDist, 1e-4);
                float marchSin = sqrt(max(1.0 - graze * graze, 0.0));
                float soft = shadowSoft(worldPos, dirToSrc, dist, marchSin);
                vec3 colS = nukeColor(fallLin, heatA);
                surfAccum += colS
                        * (gate * uIntensity * 1.45 * ndl
                           * (SHADOW_FLOOR + (1.0 - SHADOW_FLOOR) * soft));
            }

            // 3. Ударная волна: непрерывный CPU-таймер (uSrcShock) +
            // асимметричный профиль Фридлендера по ГОРИЗОНТАЛЬНОЙ
            // дистанции - резкий передний фронт, широкий пылевой
            // шлейф; скользит по ступеням лодов без рывков.
            float shockR = uSrcShock[i].x;
            float shockFade = uSrcShock[i].y;
            if (uCine > 0.001 && shockFade > 0.001 && shockR > 1.0) {
                float horizDist = length(toSrc.xz);
                float dr = horizDist - shockR;
                float sigma = dr >= 0.0 ? 18.0 : 65.0;
                float band = exp(-(dr * dr) / (sigma * sigma)) * shockFade * uCine;
                if (band > 0.002) {
                    vec3 shockCol = mix(vec3(1.0, 0.55, 0.20), vec3(1.0, 0.88, 0.70), smoothstep(-25.0, 6.0, dr));
                    ringAccum += shockCol * (band * uIntensity * 0.85);
                    vec4 sClip = uProj * (uModelView * vec4(relA, 1.0));
                    if (sClip.w > 0.0) {
                        vec2 sUv = (sClip.xy / sClip.w) * 0.5 + 0.5;
                        vec2 dS = uv - sUv;
                        refrDir += normalize(dS + vec2(1.0e-5, 0.0)) * band;
                        refrAmp += band;
                    }
                }
            }
        }
    } else {
        // Небо и сам ядерный гриб (частицы глубину не пишут):
        for (int i = 0; i < uSourceCount; i++) {
            float heat = clamp(uSources[i].w / uHeatScale, 0.0, 1.0);
            float airFlash = airFlashCurve(heat);
            float R = uSrcRadius[i];
            vec3 relA = uSources[i].xyz - uCamPos;
            float tAir;
            float uMaxAir;
            float airRaw = airTerm(relA, rayDir, R, 1.0e30, uMaxAir, tAir);
            float airNorm = airRaw * 3.0 / (2.0 * R);
            float cosTA = dot(rayDir, normalize(relA));
            float cloudWin = mushroomCloudWindow(relA, rayDir, R, heat);
            float air = airNorm * airNorm * airFlash * uIntensity * cloudWin
                    * mix(0.28, 0.85, miePhaseNorm(cosTA, 0.55));
            vec3 airCol = nukeColor(clamp(1.0 - sqrt(max(dot(relA, relA) - tAir * tAir, 0.0)) / R, 0.0, 1.0), heat);
            if (uCine > 0.001) {
                // godrays: та же окклюзия плюс сумеречные лучи.
                vec4 sClip = uProj * (uModelView * vec4(relA, 1.0));
                if (sClip.w > 0.0) {
                    vec2 sUv = (sClip.xy / sClip.w) * 0.5 + 0.5;
                    float gr = godrays(uv, sUv, length(relA), airDither);
                    air *= mix(0.15, 1.0, gr);
                }
            } else if (airRaw > max(0.02 * uIntensity, 0.05) && uMaxAir > 0.0) {
                float occ = airMarchOcc(rayDir, tAir, uMaxAir, airDither);
                air *= mix(1.0, AIR_OCCLUDE_RESIDUE, occ);
            }
            glowAccum += airCol * airSoftCap(air, 0.28 * uIntensity * max(airFlash, 0.6));
        }
    }

    vec3 added = surfAccum + glowAccum + ringAccum;
    // Классический путь (кинематика выкл или копия сцены недоступна):
    // чистый аддитив (поверхность - с половинным весом: альбедо-модуляции нет).
    if (uHasScene < 0.5 || uCine <= 0.001) {
        if (added == vec3(0.0)) {
            discard;
        }
        fragColor = vec4(pow(glowAccum + ringAccum + surfAccum * 0.5, vec3(1.0 / 2.2)), 1.0);
        return;
    }
    // Рефракция должна писаться и без света (искажение без свечения).
    if (added == vec3(0.0) && refrAmp < 0.003) {
        discard;
    }

    vec3 base;
    if (refrAmp >= 0.003) {
        vec2 off = (refrDir / max(refrAmp, 1.0e-4)) * (0.0038 * min(refrAmp, 1.2));
        base = vec3(
                texture(uSceneColor, uv + off).r,
                texture(uSceneColor, uv + off * 0.55).g,
                texture(uSceneColor, uv + off * 0.25).b);
    } else {
        base = texture(uSceneColor, uv).rgb;
    }

    // ── Раздельный композитинг земли и воздуха ──────────────────
    // 1. Земля/лоды: альбедо-освещение + собственный цветовой вклад.
    vec3 litSurface = base;
    if (geometry) {
        vec3 surfSoft = surfAccum / (1.0 + surfAccum * 0.35);
        litSurface = base * (1.0 + surfSoft * 4.2) + surfSoft * 0.32 + ringAccum;
    }

    // 2. Воздушное зарево: мягкая гамма + Screen-блендинг
    //    (не выжигает яркие детали гриба в белый квадрат).
    vec3 glowGamma = pow(max(glowAccum, vec3(0.0)), vec3(1.0 / 1.8));
    glowGamma = glowGamma / (1.0 + glowGamma * 0.65);

    vec3 finalColor = litSurface + glowGamma * max(vec3(1.0) - litSurface * 0.88, vec3(0.08));
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
