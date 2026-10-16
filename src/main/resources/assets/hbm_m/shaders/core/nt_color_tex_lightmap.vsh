#version 150

// Byte-identical clone of the vanilla position_color_tex_lightmap core shader.
// It exists under our namespace so that the NT particle render types can bind a
// KNOWN-OURS ShaderInstance instead of the shared vanilla instance: under an
// Iris pack, unknown mod shaders are masked (color/depth writes disabled by
// MixinShaderInstance.onTail) - the mask is undone for the duration of the NT
// pass by the NtDirectPass window + VertexBufferMixin, which restores exactly
// this vanilla look (texture x vertexColor, alpha < 0.1 discard, no pack fog /
// lighting) instead of the pack's translucent material treatment.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord2;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
    texCoord2 = UV2;
}
