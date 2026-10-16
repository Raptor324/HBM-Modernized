#version 150

#moj_import <fog.glsl>

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
in float vDhWinZ;

uniform sampler2D Sampler0;
// DH depth texture (DEPTH32F, forward-Z). Only active in the far pass
// (DhDepthTest > 0.5): per-pixel occlusion against LOD terrain.
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
        // DIAGNOSTIC: R = our window-Z (DhProjMat*MVM*pos), G = DH depth sample.
        // Both channels vary => sampler and math work.
        // G == 0 everywhere => sampler not bound / reading garbage.
        // R == 0 or > 1 everywhere => DhProjMat not applied.
        fragColor = vec4(vDhWinZ, texture(Sampler1, gl_FragCoord.xy / DhViewport).r, 0.0, 1.0);
    } else {
        // OCCLUSION AGAINST LOD - ONLY via GL depth test against the copy of
        // DH depth (DhDepthCopy.copyToMain into the main z-buffer). There used
        // to be an ADDITIONAL discard "vDhWinZ > lodDepth" here: it duplicated
        // the GL test with worse precision (comparing window-Z of different
        // conventions) and cut the mushroom by INVISIBLE DH depth sources -
        // fogged distant LODs and DH's own clouds - which looked like "the
        // mushroom flies away / recedes" as the camera moved. When the depth
        // copy failed, an empty Sampler1 killed the whole mushroom outright.
        vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    }
}
