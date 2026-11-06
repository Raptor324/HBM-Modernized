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
//    .getDepthDirection(), glClearDepth = farDepth = 0): near=1, far=0.
//    The DEPTH32F attachment holds WINDOW depth, not raw NDC: GL's default
//    window transform ndc*0.5+0.5 always applies (DH never calls glClipControl
//    or glDepthRange, and DH's own fog.frag calcViewPosition decodes the
//    sampled depth through ndc.xyz = ndc.xyz*2.0 - 1.0). With DH's reverse
//    matrix (RenderUtil.setDhProjectionMatrix -> setClipPlanes(mat, far, near,
//    true): m22 = N/(F-N), m23 = F*N/(F-N)) the clip/NDC depth of terrain is
//    ndc = (N/(F-N))*(F/dist - 1)  =>  1 at near, 0 at the far plane, and the
//    STORED texture value is d = 0.5 + 0.5*ndc - so terrain sits in (0.5, 1.0],
//    sky is cleared to 0.0, and the decode is
//    dist = F*N / (N + ndc*(F-N))  with ndc = 2d - 1.
//    Numeric check (n=7.5, f=12309.31, dist=1000): ndc = 0.00689 ->
//    stored d = 0.50345 -> decode 7.5*12309.31/(7.5 + 0.00689*12301.81) ~ 1000.
//    (The old comment claimed d was already the raw [0..1] reverse value; its
//    numeric check had computed the NDC value 0.0643, not the stored window
//    depth 0.5321. Feeding the window value without the 2d-1 remap decoded
//    every LOD to [N, 2N] blocks - below the fade mask - making the whole copy
//    a silent no-op: the DH 3.3.1 "mushroom through blocks without shaders"
//    regression. Only unwritten/sky texels (d <= 0.5) would go negative.)

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
    // REVERSE_Z stores WINDOW depth: DH clears to 0.0 and valid terrain is
    // (0.5, 1.0] (0.5 = the far plane itself), so anything <= 0.5 is sky or
    // beyond-far garbage - decoding it would only produce distances >= DhFar.
    if ((reverseZ && d <= 0.5) || (!reverseZ && d >= 0.999999)) {
        discard;
    }
    // REVERSE_Z: the texture holds WINDOW depth (ndc*0.5+0.5); remap to NDC
    // first - the same window->NDC step the FORWARD branch applies. Distances
    // then decode exactly: ndc = 2d-1 = (N/(F-N))*(F/dist - 1)
    // => dist = F*N / (N + ndc*(F-N)) (1 at near, ~F at the far plane).
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
