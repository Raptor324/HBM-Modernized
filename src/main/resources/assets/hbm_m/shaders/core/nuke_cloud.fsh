#version 150

#moj_import <fog.glsl>

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
in float vDhWinZ;

uniform sampler2D Sampler0;
// DH depth texture (DEPTH32F, forward-Z). Active only in the distant
// pass (DhDepthTest > 0.5): per-pixel occlusion against LOD terrain.
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
        // DIAGNOSTICS: R = window-Z (DhProjMat*MVM*pos), G = sampled DH depth.
        // Both channels varying => sampler and math are active.
        // G == 0 everywhere => sampler unassigned or reading uninitialized memory.
        // R == 0 or > 1 everywhere => DhProjMat not applied.
        fragColor = vec4(vDhWinZ, texture(Sampler1, gl_FragCoord.xy / DhViewport).r, 0.0, 1.0);
    } else {
        // Occlusion against distant LOD terrain is handled via hardware GL depth test against copied DH depth buffer.
        vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
        if (color.a <= 0.005) {
            discard;
        }
        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    }
}
