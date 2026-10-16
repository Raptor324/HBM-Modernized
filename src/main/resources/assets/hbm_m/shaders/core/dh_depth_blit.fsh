#version 150

// COPY DH DEPTH INTO THE MAIN Z-BUFFER.
// The DH composite (apply.frag) transfers only the COLOR of LODs into the main
// FBO; depth there remains sky (1.0), so distant geometry (missile meshes)
// always tests "closer" than LODs in the native depth test. This pass reads
// the DH DEPTH32F texture and writes it into gl_FragDepth of the MAIN buffer.
//
// Conversion: DH projection window-Z -> distance -> window-Z of our extended
// projection (near=0.05, far=8e6 => ndcZ ~= 1 - 0.1/dist) so that comparisons
// against already-written vanilla geometry and subsequent far content are correct.
//
// TWO DH DEPTH CONVENTIONS (DhReverseZ, queried via RenderUtil.RENDER_DEF
// .getRenderDepth() - the event matrix is UNUSABLE for this, it is always forward):
//  - FORWARD_Z (DH <= 3.2.x; 3.3.1+ only under the Iris pack without reverse-Z):
//    near=0, far=1, sky=1. Standard perspective:
//    ndc = 2d-1, dist = 2fn/((F+N) - ndc(F-N)).
//  - REVERSE_Z (DH 3.3.1+ default without the pack; GlDhRenderApiDefinition
//    .getRenderDepth(), glClearDepth = farDepth = 0): near=1, far=0.
//    DH reverse matrix (RenderUtil.setClipPlanes, 3.3.1 bytecode):
//    A = m22 = n/(f-n), B = m23 = fn/(f-n)  =>  ndc = A*(f/dist - 1),
//    hence dist = f*n / (ndc*(f-n) + n) with the usual ndc = 2d-1.
//    Numeric check (n=73.41, f=7512.3): d=1000 -> decode 1000.2,
//    d=200 -> 200.02. Sky now decodes to d = 0.
//    (IRRELEVANT: an earlier attempt, "ndc = 1-2d + forward formula",
//    matched the STANDARD reverse perspective m22=(f+n)/(f-n), while DH uses
//    its own, see above - with it the mushroom stayed unoccluded.)

uniform sampler2D Sampler0;
uniform float DhNear;
uniform float DhFar;
// Clip planes of OUR extended projection (where depth is encoded to).
uniform float OutNear;
uniform float OutFar;
// Lower bound of DH depth validity: mirrors the dither-fade zone of
// "Fade Nearby DH LODs". Inside it the DEPTH32F is stochastic noise
// (bayer-discard in the DH terrain shader) and must not be copied.
uniform float DhFadeMaskDist;
// DH depth convention: > 0.5 = REVERSE_Z, otherwise FORWARD_Z.
uniform float DhReverseZ;

in vec2 uv;
out vec4 fragColor;

void main() {
    float d = texture(Sampler0, uv).r;
    bool reverseZ = DhReverseZ > 0.5;
    // DH sky (no LOD drawn): leave depth untouched. The conventions are opposite.
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
    // Exact window of OUR projection: window = 1 - fnEff/dist, fnEff = F*N/(F-N).
    // IMPORTANT: this used to be "1 - 0.1/dist" - wrong fnEff (correct is 0.05
    // for N=0.05/F=8e6), which made every occluder look twice as close and cut
    // the mushroom while being BEHIND it ("the mushroom slides back on fly-away").
    float fnEff = (OutFar * OutNear) / (OutFar - OutNear);
    gl_FragDepth = clamp((1.0 - fnEff / dist) + 1.0e-6, 0.0, 1.0);
    fragColor = vec4(0.0);
}
