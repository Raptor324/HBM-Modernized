#version 150

// Thermal vision post-processing shader.
// Uses:
//  - DiffuseSampler: the already-rendered world
//  - ThermalSampler: our separate buffer with "hot" entities (white silhouettes)

uniform sampler2D DiffuseSampler;
uniform sampler2D ThermalSampler;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

// Linear luminance
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

// Simple hash for pseudo-random values (for noise bands)
float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 78.233))) * 43758.5453);
}

// Simplified blur from the old thermal_vision.fsh:
// light blur to suppress textures while keeping detail.
vec3 blurSample(sampler2D tex, vec2 uv, vec2 texelSize) {
    // Stronger 3x3 blur to hide texture "blockiness",
    // still cheap enough for post-processing.
    vec3 sum = vec3(0.0);
    float weight = 0.0;

    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(x, y) * texelSize;
            float w = (x == 0 && y == 0) ? 4.0 : 1.0; // weight the center more
            sum += texture(tex, uv + offset).rgb * w;
            weight += w;
        }
    }

    return sum / weight;
}

void main() {
    // Mask of "hot" entities
    vec4 thermalMask = textureLod(ThermalSampler, texCoord, 0.0);
    bool isEntityHot = thermalMask.a > 0.01 || dot(thermalMask.rgb, vec3(1.0)) > 0.01;

    // Screen size from the main buffer
    vec2 texSize = vec2(textureSize(DiffuseSampler, 0));

    // Screen-space pixelation - retro thermal vision effect
    float pixelSize = 2.0;
    vec2 fragCoord = gl_FragCoord.xy;
    vec2 pixelated = floor(fragCoord / pixelSize) * pixelSize + (pixelSize * 0.5);
    vec2 uv = clamp(pixelated / texSize, vec2(0.001), vec2(0.999));

    vec2 texelSize = vec2(1.0 / texSize.x, 1.0 / texSize.y);

    // Original world color and luminance
    vec3 originalColor = texture(DiffuseSampler, uv).rgb;
    float originalLuminance = luma(originalColor);

    // Stronger world blur to suppress "blocky" textures
    vec3 blurred = blurSample(DiffuseSampler, uv, texelSize);

    // For simplicity, always assume "night" mode (state=0),
    // like the old shader at night.
    float state = 0.0;

    // Thermal-channel luminance is taken from the blurred world,
    // so landforms stay readable but texture detail is smoothed.
    float luminance = luma(blurred);

    // Brightness boost in dark areas.
    // Slightly less aggressive than the very first variant,
    // but stronger than the previous step, so night has no "true darkness".
    float brightnessBoost = 1.0;
    float darkFactor = 1.0 - smoothstep(0.0, 0.3, originalLuminance);
    brightnessBoost = 1.0 + darkFactor * 1.5;

    luminance = clamp(luminance * brightnessBoost, 0.0, 1.0);

    // Luminance quantization - "stepped" gray like a thermal imager
    float quantized = floor(luminance * 12.0) / 12.0;

    // Increased contrast around the middle
    quantized = 0.5 + (quantized - 0.5) * 1.0;

    // Highlight "hot" zones by scene brightness
    float heat = pow(quantized, 0.45);
    heat = smoothstep(0.35, 0.9, heat);
    float hotspotStrength = 0.70;
    vec3 hotspot = vec3(heat * hotspotStrength);

    // Base background gray level
    float ambient = 0.25;
    float baseMultiplier = 1.1;
    vec3 baseThermal = vec3(quantized) * (ambient + baseMultiplier);

    // Final B/W thermal image before extra effects
    vec3 thermal = clamp(baseThermal + hotspot, 0.0, 1.0);

    // Horizontal scanlines (time-animated).
    float scanlinePeriod = 12.0;
    float scanlineWidth = 0.33;
    // Offset the Y coordinate over time so the bands "crawl".
    float animatedY = gl_FragCoord.y + Time * 40.0;
    float scanline = step(1.0 - scanlineWidth, fract(animatedY / scanlinePeriod)) * 0.06;
    thermal -= vec3(scanline);

    // Additional chaotic gray stripes on top of everything:
    // each Y "row" has a small chance of adding a noise line.
    float band = floor(gl_FragCoord.y / 8.0);          // band height
    float noiseVal = rand(vec2(band, floor(Time * 3.0)));
    if (noiseVal > 0.96) {
        // The higher noiseVal, the brighter the stripe, but it stays quite thin.
        float stripeIntensity = (noiseVal - 0.96) / 0.04; // 0..1
        thermal += vec3(0.05 * stripeIntensity);
    }


    float nightLift = 0.28;
    float darkMask = 1.0 - smoothstep(0.0, 0.40, thermal.r);
    thermal = thermal + vec3(nightLift * darkMask);
    
    // Final gamma correction.
    // A slightly smaller gamma is used so the image is noticeably brighter at night.
    float gamma = 0.75;
    thermal = pow(thermal, vec3(gamma));
    
    // Hard lower brightness floor - guarantees the screen never gets too dark.
    // This mimics night vision behavior: even in total darkness the world is
    // visible in dim gray tones.
    float minBrightness = 0.25;
    thermal = max(thermal, vec3(minBrightness));

    // Vignette
    // Slightly stronger edge darkening to draw the eye toward the center.
    vec2 centered = (uv - 0.5) * vec2(texSize.x / texSize.y, 1.0);
    float r = length(centered);
    // r ~ 0 at the center, grows toward the corners. At the edges brightness
    // drops to roughly ~55-60%.
    float vignetteStrength = 0.5;
    float edgeFactor = smoothstep(0.4, 0.9, r); // 0 at the center, 1 near the corners
    float vignette = 1.0 - vignetteStrength * edgeFactor;
    thermal *= vignette;

    // Entity silhouettes from our buffer - always pure white
    if (isEntityHot) {
        thermal = vec3(1.0);
    }

    fragColor = vec4(clamp(thermal, 0.0, 1.0), 1.0);
}