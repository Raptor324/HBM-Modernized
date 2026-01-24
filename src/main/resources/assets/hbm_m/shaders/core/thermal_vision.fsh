#version 330 core

// Fragment shader for thermal vision effect
// Works as a subtle overlay that tints the screen without completely covering it
// Since we can't get screen texture in vanilla Minecraft, we use a tinting approach

in vec2 texCoord;

uniform sampler2D Sampler0; // Screen texture
uniform float WorldTime; // World time for day/night transitions

out vec4 fragColor;

// Noise function for interference
float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = rand(i + vec2(0.0, 0.0));
    float b = rand(i + vec2(1.0, 0.0));
    float c = rand(i + vec2(0.0, 1.0));
    float d = rand(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float smoothTransition(float time) {
    if (time >= 0.0 && time <= 1000.0) {
        return time / 1000.0;
    } else if (time > 1000.0 && time < 12000.0) {
        return 1.0;
    } else if (time >= 12000.0 && time <= 13000.0) {
        return 1.0 - (time - 12000.0) / 1000.0;
    } else {
        return 0.0;
    }
}

// Bayer dithering (from original shader)
float Bayer2(vec2 c) { 
    c = 0.5 * floor(c); 
    return fract(1.5 * fract(c.y) + c.x); 
}
float Bayer4(vec2 c) { 
    return 0.25 * Bayer2(0.5 * c) + Bayer2(c); 
}
float Bayer8(vec2 c) { 
    return 0.25 * Bayer4(0.5 * c) + Bayer2(c); 
}

// Reduced blur to preserve more detail for better visibility
vec3 blurSample(sampler2D tex, vec2 uv, vec2 texelSize) {
    // Use center sample with slight blur instead of full 3x3 blur
    // This preserves more detail while still removing texture colors
    vec3 center = texture(tex, uv).rgb;
    float centerLum = dot(center, vec3(0.299, 0.587, 0.114));
    
    // Light blur: only sample 4 neighbors instead of 9
    vec3 sum = vec3(centerLum) * 2.0; // Center sample weighted more
    float weight = 2.0;
    
    // Sample only 4 diagonal neighbors for lighter blur
    vec2 offsets[4] = vec2[](
        vec2(-1.0, -1.0),
        vec2(1.0, -1.0),
        vec2(-1.0, 1.0),
        vec2(1.0, 1.0)
    );
    
    for (int i = 0; i < 4; i++) {
        vec2 offset = offsets[i] * texelSize;
        vec3 sample = texture(tex, uv + offset).rgb;
        float lum = dot(sample, vec3(0.299, 0.587, 0.114));
        sum += vec3(lum);
        weight += 1.0;
    }
    
    return sum / weight;
}

void main() {
    // PIXELIZATION EFFECT: Reduce resolution for retro thermal imaging look
    // This creates a pixelated appearance similar to older thermal cameras
    float pixelSize = 2.0; // Size of each pixel in screen space (increase for more pixelation)
    
    // Proper pixelization: derive from screen-space coords to avoid diagonal seams
    // Using gl_FragCoord prevents triangle edge artifacts on full-screen quads
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 fragCoord = gl_FragCoord.xy;
    vec2 pixelated = floor(fragCoord / pixelSize) * pixelSize + (pixelSize * 0.5);
    vec2 pixelCenter = pixelated / texSize;
    
    // Clamp UVs to avoid edge artifacts from sampling outside the framebuffer
    vec2 uv = clamp(pixelCenter, vec2(0.001), vec2(0.999));
    
    // Get texture size for blur sampling
    vec2 texelSize = vec2(1.0 / texSize.x, 1.0 / texSize.y);
    
    // CRITICAL: Get ORIGINAL luminance BEFORE blur to preserve lighting information
    // This captures the actual block brightness/lightmap data from the game
    vec3 originalColor = texture(Sampler0, uv).rgb;
    float originalLuminance = dot(originalColor, vec3(0.299, 0.587, 0.114));
    
    // DETECT GLASS/WATER: Make translucent blocks (glass, water) appear opaque
    // Glass typically has: high brightness, neutral color (low saturation)
    // Water typically has: blue tint, medium-high brightness
    // Ice typically has: high brightness with slight blue tint
    // We detect these patterns and make them opaque gray
    bool isGlassOrWater = false;
    
    // Calculate color components
    float r = originalColor.r;
    float g = originalColor.g;
    float b = originalColor.b;
    
    // Calculate saturation (colorfulness) - helps distinguish glass from colored blocks
    float maxColor = max(max(r, g), b);
    float minColor = min(min(r, g), b);
    float saturation = maxColor > 0.001 ? (maxColor - minColor) / maxColor : 0.0;
    
    // Check for blue tint (water/ice) - blue is stronger than red/green
    float blueTint = b - max(r, g);
    bool hasStrongBlueTint = blueTint > 0.2 && b > 0.4; // Strong blue = water
    bool hasWeakBlueTint = blueTint > 0.08 && blueTint < 0.2 && b > 0.5; // Weak blue = ice
    
    // Check for glass-like properties: high brightness + very low saturation (neutral gray/white)
    // Glass is usually bright and colorless (low saturation)
    bool isGlassLike = originalLuminance > 0.45 && saturation < 0.25 && maxColor > 0.55;
    
    // Exclude sky (very bright, high blue) and foliage (green tint)
    float greenTint = g - max(r, b);
    bool isSky = originalLuminance > 0.8 && b > 0.7 && saturation > 0.2; // Bright blue sky
    bool isFoliage = greenTint > 0.15 && g > 0.3; // Green foliage
    
    // Combine checks: glass, water, or ice (but not sky or foliage)
    isGlassOrWater = (hasStrongBlueTint || hasWeakBlueTint || isGlassLike) && !isSky && !isFoliage;
    
    // If detected as glass/water, replace with opaque gray based on luminance
    // This makes them visible but opaque in thermal vision
    if (isGlassOrWater) {
        // Use medium-gray for glass/water blocks, preserve some brightness information
        // Darker water/glass -> darker gray, brighter -> lighter gray
        float glassLuminance = clamp(originalLuminance * 0.6 + 0.25, 0.25, 0.55);
        originalColor = vec3(glassLuminance);
        originalLuminance = glassLuminance;
    }
    
    // Apply reduced blur to preserve more detail while removing texture colors
    // This improves visibility at low brightness settings
    vec3 blurred = blurSample(Sampler0, uv, texelSize);
    
    float timeOfDay = mod(WorldTime, 24000.0);
    float state = smoothTransition(timeOfDay);
    
    // Use original luminance for better lighting information preservation
    // The blur is only for removing texture details, not for luminance calculation
    float luminance = originalLuminance;
    
    // ADAPTIVE BRIGHTNESS BOOST: Use original luminance to detect dark areas
    // Dark areas (low luminance) need more boost, especially at night
    // This preserves the game's brightness settings and block lighting
    float brightnessBoost = 1.0;
    if (state < 0.5) { // Night time
        // At night, aggressively boost dark areas based on original luminance
        // Reduced boost slightly to make night darker
        float darkFactor = 1.0 - smoothstep(0.0, 0.3, originalLuminance);
        brightnessBoost = 1.0 + darkFactor * 2.0; // Up to 3.0x boost (reduced from 3.5x)
    } else { // Day time
        // During day, moderate boost for slightly dark areas
        float darkFactor = 1.0 - smoothstep(0.0, 0.5, originalLuminance);
        brightnessBoost = 1.0 + darkFactor * 0.3; // Up to 1.3x boost
    }
    
    // Apply brightness boost to luminance BEFORE quantization
    // This preserves lighting information from the game
    luminance = clamp(luminance * brightnessBoost, 0.0, 1.0);
    
    // QUANTIZATION: Reduce to 12 gray levels for thermal imaging effect
    // Increased from 8 to 12 levels for smoother transitions while maintaining thermal camera look
    float quantized = floor(luminance * 12.0) / 12.0; // 12 levels of gray
    
    // No dithering - it creates visible patterns that interfere with pixelization effect
    // The pixelization itself provides enough smoothing, and 12 levels is sufficient

    // INCREASED CONTRAST: Less compression to maintain visibility at low brightness
    // Full contrast range (1.0) for maximum separation between light and dark areas
    quantized = 0.5 + (quantized - 0.5) * 1.0;
    
    // Boost hot areas (simulate bright entities/torches) as white highlights
    float heat = pow(quantized, 0.45);
    heat = smoothstep(0.35, 0.9, heat);
    vec3 hotspot = vec3(heat * 0.7);
    
    // BALANCED AMBIENT: Slightly darker at night, darker during day
    // Reduced since we now boost luminance directly based on original brightness
    float ambient = mix(0.25, 0.15, state); // Night: 0.25 (reduced from 0.3), Day: 0.15
    
    // ADAPTIVE BASE BRIGHTNESS: Different multipliers for day/night
    // Reduced multipliers since brightness boost is applied earlier
    float baseMultiplier = mix(1.1, 0.7, state); // Night: 1.1 (reduced from 1.2), Day: 0.7
    vec3 baseThermal = vec3(quantized) * (ambient + baseMultiplier);
    
    // Combine quantized grayscale with hotspots (white), keep overall B/W
    vec3 thermal = clamp(baseThermal + hotspot, 0.0, 1.0);
    
    // CONTRAST ENHANCEMENT: Apply stronger contrast curve, especially at night
    // This helps distinguish dark objects (roads) from background
    // Increased contrast boost for better separation between light and dark areas
    // Night: stronger contrast to separate dark roads from dark background
    // Day: increased contrast to better distinguish landscape features
    float contrastBoost = mix(1.4, 1.2, state); // Night: 1.4x (increased from 1.2x), Day: 1.2x (increased from 1.05x)
    thermal = pow(thermal, vec3(1.0 / contrastBoost)); // Inverse gamma for contrast
    
    // Static horizontal scanlines (thicker and less frequent)
    // Use screen-space Y to avoid diagonal/vertical artifacts
    // Less frequent: every 12 pixels, thicker: 4 pixels wide (33% of period)
    float scanlinePeriod = 12.0;
    float scanlineWidth = 0.33; // 33% of period = 4 pixels thick
    float scanline = step(1.0 - scanlineWidth, fract(gl_FragCoord.y / scanlinePeriod)) * 0.06;
    thermal -= vec3(scanline);
    
    // ADDITIONAL NIGHT LIFT: Extra boost for very dark areas at night
    // This complements the earlier brightness boost for maximum visibility
    float nightLift = (1.0 - state) * 0.15; // Only at night, reduced since we boost earlier
    // Apply lift to remaining dark areas
    float darkMask = 1.0 - smoothstep(0.0, 0.35, thermal.r); // Mask for dark areas
    thermal = thermal + vec3(nightLift * darkMask);
    
    // FINAL ADAPTIVE GAMMA: Apply gamma correction based on time of day
    // Night: slightly higher gamma (darker) for more realistic night look
    // Day: higher gamma (darker) to avoid being too bright
    float gamma = mix(0.85, 1.1, state); // Night: 0.85 (darker than before), Day: 1.1 (darker)
    thermal = pow(thermal, vec3(gamma));
    
    // Final clamp to ensure values stay in valid range
    fragColor = vec4(clamp(thermal, 0.0, 1.0), 1.0);
}
