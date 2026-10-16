/**
 * Forward-Z Single Pass Downsampler reduction and remapping functions.
 *
 * @credit CrankShaft / fewizz / Jdb100
 * Based on FidelityFX SPD v2.1 adapted for OpenGL 4.3 Forward-Z Depth Pyramid in CrankShaft/Flywheel.
 */

uint extractBits(uint e, uint offset, uint count) {
    return (e >> offset) & ((1u << count) - 1u);
}

uint insertBits(uint e, uint newbits, uint offset, uint count) {
    uint countMask = ((1u << count) - 1u);
    return (e & ~(countMask << offset)) | ((newbits & countMask) << offset);
}

uvec2 remap_for_wave_reduction(uint a) {
    return uvec2(
        insertBits(extractBits(a, 2u, 3u), a, 0u, 1u),
        insertBits(extractBits(a, 3u, 3u), extractBits(a, 1u, 2u), 0u, 2u)
    );
}

uvec2 get_xy() {
    uvec2 sub_xy = remap_for_wave_reduction(gl_LocalInvocationIndex % 64u);
    uint x = sub_xy.x + 8u * ((gl_LocalInvocationIndex >> 6u) % 2u);
    uint y = sub_xy.y + 8u * (gl_LocalInvocationIndex >> 7u);
    return uvec2(x, y);
}

const float SKY_DEPTH_THRESHOLD = 0.9999;

float reduce_4(vec4 v) {
    // Forward-Z: the conservative occluder is the farthest surface = MAX depth.
    // Sky is cleared to exactly 1.0 in Forward-Z.
    //
    // A MIXED footprint (sky + geometry) is only PARTLY covered by geometry:
    // storing the near geometry depth there (the previous variant zeroed sky
    // samples) treats the empty sky part of the tile as a solid occluder, and
    // nucleus_cull.comp then falsely culls far machines behind terrain/sky
    // silhouettes. Distant Horizons LODs make it worse: with OutNear=0.05 any
    // DH geometry beyond ~500 blocks already encodes depth >= 0.9999, i.e. it
    // reads back as "sky" and was dropped from the reduction entirely.
    // The conservative answer for a mixed tile is 1.0 ("nothing occludes
    // here"); pure-geometry tiles keep max() and stay fully cullable. Sky
    // dilation into coarser MIPs is a pure culling-efficiency loss, never a
    // correctness loss.
    bvec4 isSky = greaterThanEqual(v, vec4(SKY_DEPTH_THRESHOLD));
    if (any(isSky)) {
        return 1.0;
    }
    return max(max(v.x, v.y), max(v.z, v.w));
}
