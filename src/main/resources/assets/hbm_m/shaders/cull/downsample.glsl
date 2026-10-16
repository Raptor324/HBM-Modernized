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

float reduce_4(vec4 v) {
    // Forward-Z: the conservative occluder is the farthest surface = MAX depth
    return max(max(v.x, v.y), max(v.z, v.w));
}
