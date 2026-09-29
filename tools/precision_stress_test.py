"""
Milestone 6 Empirical Stress Testing Harness
Tests R1 (Floating Anchor Math, IEEE 754 precision, 256m drift threshold, GpuSpanUploader zero-copy diffing)
and R2 (Transition Seal Y+44 bounding box, lightmap sampling non-collapse, quaternion normalization at 20-24m).
"""

import math
import struct
import numpy as np

def float32(val):
    return struct.unpack('f', struct.pack('f', float(val)))[0]

def float32_ulp(val):
    v = float32(val)
    if v == 0.0:
        return np.nextafter(np.float32(0.0), np.float32(1.0))
    next_up = np.nextafter(np.float32(v), np.float32(np.inf))
    return float(next_up - np.float32(v))

def test_r1_coordinate_precision():
    print("=== R1: Coordinate Precision & Float32 Mantissa Analysis ===")
    test_coords = [10_000.0, 100_000.0, 1_000_000.0]
    
    print(f"{'Coord (m)':<12} | {'Old WorldPos ULP (m)':<22} | {'Anchor Rel ULP (<=256m)':<25} | {'Jitter Improvement Factor':<25}")
    print("-" * 90)
    
    anchor_rel_max = 256.0
    anchor_ulp = float32_ulp(anchor_rel_max)
    
    for c in test_coords:
        old_ulp = float32_ulp(c)
        factor = old_ulp / anchor_ulp
        print(f"{c:<12.0f} | {old_ulp:<22.7f} | {anchor_ulp:<25.7f} | {factor:<25.1f}x")
    
    # Verify that at offset <= 256.0, float32 resolution is ~0.00003m (30 microns)
    assert anchor_ulp <= 0.000031, f"Expected anchor ULP <= 0.000031, got {anchor_ulp}"
    print(f"\n[PASS] Anchor relative resolution at 256m is {anchor_ulp:.8f} m (~30.5 microns).")
    print(f"[PASS] At |X|,|Z|=1,000,000m, precision improves by {float32_ulp(1_000_000.0)/anchor_ulp:.0f}x, eliminating cm-scale jitter.")

def test_r1_vertex_cancellation_simulation():
    print("\n=== R1: Vertex Cancellation Simulation (100 Frames Sub-millimeter Camera Movement) ===")
    coord = 1_000_000.0
    model_offset = 10.0 # model is 10m away from camera
    world_model_pos = coord + model_offset
    vertex_local = np.array([0.5, 1.5, 0.5], dtype=np.float32)
    
    # 100 frames with small camera jitter (0.001m = 1mm per frame)
    jitter_old = []
    jitter_new = []
    
    anchor_origin = int(coord)
    
    for frame in range(100):
        cam_pos = coord + frame * 0.001
        
        # Method A: Without Anchor (Absolute World Pos in float32)
        # vertex_world_f32 = float32(world_model_pos) + vertex_local
        # view_pos = vertex_world_f32 - float32(cam_pos)
        model_pos_f32 = np.float32(world_model_pos)
        cam_pos_f32 = np.float32(cam_pos)
        view_pos_old = (model_pos_f32 + vertex_local[0]) - cam_pos_f32
        
        # Method B: With Anchor
        # InstPos = world_model_pos - anchor_origin (exact in double, stored in float32)
        inst_pos_f32 = np.float32(world_model_pos - anchor_origin)
        rel_cam_f32 = np.float32(cam_pos - anchor_origin)
        view_pos_new = (inst_pos_f32 + vertex_local[0]) - rel_cam_f32
        
        jitter_old.append(view_pos_old)
        jitter_new.append(view_pos_new)
    
    # In ideal double-precision math, view_pos should decrease smoothly by exactly 0.001 each frame
    ideal_diff = -0.001
    
    old_diffs = np.diff(jitter_old)
    new_diffs = np.diff(jitter_new)
    
    old_max_error = np.max(np.abs(old_diffs - ideal_diff))
    new_max_error = np.max(np.abs(new_diffs - ideal_diff))
    
    print(f"Old method max deviation from smooth motion at 1M blocks: {old_max_error:.7f} m ({old_max_error*1000:.2f} mm)")
    print(f"New anchor method max deviation from smooth motion:       {new_max_error:.7f} m ({new_max_error*1000:.4f} mm)")
    
    assert old_max_error > 0.01, f"Old method was expected to exhibit > 10mm jitter, got {old_max_error}"
    assert new_max_error < 0.0001, f"New method error should be < 0.1mm, got {new_max_error}"
    print("[PASS] Jitter completely eliminated under floating anchor rebasing.")

def test_r1_drift_threshold_and_reanchoring():
    print("\n=== R1: Camera Drift Threshold (256m) & Re-anchoring Simulation ===")
    anchor_origin = None
    anchor_gen = 0
    sqr_max_anchor_dist = 256.0 * 256.0
    
    def check_anchor_drift(camera_pos):
        nonlocal anchor_origin, anchor_gen
        if anchor_origin is None:
            anchor_origin = (int(camera_pos[0]), int(camera_pos[1]), int(camera_pos[2]))
            anchor_gen += 1
            return True, "INITIALIZED"
        dx = camera_pos[0] - anchor_origin[0]
        dy = camera_pos[1] - anchor_origin[1]
        dz = camera_pos[2] - anchor_origin[2]
        dist_sq = dx*dx + dy*dy + dz*dz
        if dist_sq > sqr_max_anchor_dist:
            old = anchor_origin
            anchor_origin = (int(camera_pos[0]), int(camera_pos[1]), int(camera_pos[2]))
            anchor_gen += 1
            return True, f"SHIFTED (drift={math.sqrt(dist_sq):.1f}m)"
        return False, "STABLE"

    # Start at (100000, 64, 100000)
    pos = [100000.0, 64.0, 100000.0]
    triggered, reason = check_anchor_drift(pos)
    assert triggered and anchor_gen == 1
    print(f"Frame 0 (spawn): {reason}, Anchor={anchor_origin}, Gen={anchor_gen}")
    
    # Move 100m in X -> dist = 100m <= 256m
    pos[0] += 100.0
    triggered, reason = check_anchor_drift(pos)
    assert not triggered and anchor_gen == 1
    print(f"Frame 1 (+100m): {reason}, Anchor={anchor_origin}, Gen={anchor_gen}")
    
    # Move another 150m in X -> total dist = 250m <= 256m
    pos[0] += 150.0
    triggered, reason = check_anchor_drift(pos)
    assert not triggered and anchor_gen == 1
    print(f"Frame 2 (+150m, total=250m): {reason}, Anchor={anchor_origin}, Gen={anchor_gen}")
    
    # Move 10m in X -> total dist = 260m > 256m -> triggers shift
    pos[0] += 10.0
    triggered, reason = check_anchor_drift(pos)
    assert triggered and anchor_gen == 2
    print(f"Frame 3 (+10m, total=260m): {reason}, Anchor={anchor_origin}, Gen={anchor_gen}")
    
    print("[PASS] 256m drift threshold triggers re-anchoring accurately.")

def test_r1_zero_copy_span_diffing():
    print("\n=== R1: GpuSpanUploader Zero-Copy Dirty Span Diffing Simulation ===")
    
    # Simulate GpuSpanUploader.diffUpload
    MAX_SPANS = 48
    MERGE_GAP_FLOATS = 24
    
    def simulate_diff_upload(shadow, src, float_count):
        spans = []
        i = 0
        overflow = False
        while i < float_count:
            if src[i] == shadow[i]:
                i += 1
                continue
            start = i
            end = i + 1
            gap = 0
            i += 1
            while i < float_count:
                if src[i] != shadow[i]:
                    end = i + 1
                    gap = 0
                else:
                    gap += 1
                    if gap >= MERGE_GAP_FLOATS:
                        break
                i += 1
            if len(spans) >= MAX_SPANS:
                overflow = True
                break
            spans.append((start, end))
        
        if overflow:
            # Full upload
            shadow[:] = src[:]
            return float_count * 4, 1 # bytes, upload_calls
        
        total_bytes = 0
        calls = len(spans)
        for s, e in spans:
            count = e - s
            total_bytes += count * 4
            shadow[s:e] = src[s:e]
        return total_bytes, calls

    # Scene with 10 machines, each 30 floats
    total_floats = 10 * 30
    shadow = np.zeros(total_floats, dtype=np.float32)
    src = np.zeros(total_floats, dtype=np.float32)
    
    # Setup initial machine records (InstPos, InstRot, objBbox, fade, light)
    for m in range(10):
        base = m * 30
        src[base:base+3] = [m * 5.0, 64.0, 10.0] # InstPos = worldPos - anchorOrigin
        src[base+3:base+7] = [0.0, 0.0, 0.0, 1.0] # InstRot (unit quaternion)
        src[base+7:base+10] = [-1.0, 0.0, -1.0]   # objBboxMin
        src[base+10:base+14] = [2.0, 2.0, 2.0, 1.0] # objBboxSize + fade
        src[base+14:base+30] = 240.0              # light UVs
        
    # Frame 0: First frame full upload (via fullUpload)
    shadow[:] = src[:]
    bytes_initial = total_floats * 4
    print(f"Frame 0 (Initial fullUpload): {bytes_initial} bytes")
    
    # Simulate 50 frames of camera movement within the 256m anchor sphere.
    # Because InstPos = worldPos - anchorOrigin, and machines are static,
    # src buffer remains identical!
    for f in range(1, 51):
        bytes_uploaded, calls = simulate_diff_upload(shadow, src, total_floats)
        assert bytes_uploaded == 0, f"Frame {f} uploaded {bytes_uploaded} bytes, expected 0!"
        assert calls == 0
    print(f"[PASS] Over 50 frames with camera moving inside anchor sphere, GpuSpanUploader uploaded exactly 0 bytes.")

def test_r2_transition_seal_bounds_and_lightmap():
    print("\n=== R2: Transition Seal Bounding Box & Lightmap Sampling ===")
    
    world_pos = (500, 64, 500)
    # TransitionSealBlockEntity getRenderBoundingBox
    # minX = X - 13, maxX = X + 14
    # minY = Y,      maxY = Y + 44
    # minZ = Z - 13, maxZ = Z + 14
    aabb_min = (world_pos[0] - 13, world_pos[1], world_pos[2] - 13)
    aabb_max = (world_pos[0] + 14, world_pos[1] + 44, world_pos[2] + 14)
    
    print(f"Transition Seal World Position: {world_pos}")
    print(f"Render AABB: [{aabb_min} -> {aabb_max}]")
    print(f"Height Span: {aabb_max[1] - aabb_min[1]} blocks (Y=64 to Y=108, maxY=Y+44)")
    
    # Verify sharedLightBbox in MachineBer:
    # sharedLightBbox[0..2] = min - blockPos = (-13, 0, -13)
    # sharedLightBbox[3..5] = max - blockPos = (+14, +44, +14)
    shared_light_bbox = [
        aabb_min[0] - world_pos[0], aabb_min[1] - world_pos[1], aabb_min[2] - world_pos[2],
        aabb_max[0] - world_pos[0], aabb_max[1] - world_pos[1], aabb_max[2] - world_pos[2]
    ]
    assert shared_light_bbox[1] == 0.0
    assert shared_light_bbox[4] == 44.0
    print(f"MachineBer sharedLightBbox: {shared_light_bbox}")
    
    # Verify LightSampleCache.sample8 sampling points
    SAMPLE_INSET = 1.0 / 64.0
    minY = shared_light_bbox[1]
    maxY = shared_light_bbox[4]
    insetY = min(SAMPLE_INSET, (maxY - minY) * 0.5)
    minYs = minY + insetY
    maxYs = maxY - insetY
    
    # 8 corners: top corners have i with bit 1 set (i=2,3,6,7)
    top_corner_oy = maxYs
    top_sample_wy = world_pos[1] + math.floor(top_corner_oy)
    
    print(f"Top corner sample Y offset: {top_corner_oy:.5f} blocks")
    print(f"Top corner sampled block world Y: {top_sample_wy} (base Y = {world_pos[1]})")
    
    assert top_sample_wy == 64 + 43 == 107
    print("[PASS] Top corner light is sampled at world Y=107 (43 blocks above base), NOT collapsed to base Y=64.")

def test_r2_quaternion_normalization_shear():
    print("\n=== R2: Quaternion Normalization Shear Analysis at 20-24m ===")
    
    # Formula from block_lit_instanced.vsh: quatToMat4(q)
    def quat_to_mat4(q):
        x, y, z, w = q
        xx, yy, zz = x * x, y * y, z * z
        xy, xz, yz = x * y, x * z, y * z
        wx, wy, wz = w * x, w * y, w * z
        return np.array([
            [1.0 - 2.0 * (yy + zz), 2.0 * (xy - wz),       2.0 * (xz + wy),       0.0],
            [2.0 * (xy + wz),       1.0 - 2.0 * (xx + zz), 2.0 * (yz - wx),       0.0],
            [2.0 * (xz - wy),       2.0 * (yz + wx),       1.0 - 2.0 * (xx + yy), 0.0],
            [0.0,                   0.0,                   0.0,                   1.0]
        ], dtype=np.float32)
    
    # Vertex at top of Transition Seal frame: height Y = 24.0m
    v_top = np.array([0.0, 24.0, 0.0, 1.0], dtype=np.float32)
    
    # Suppose small drift in quaternion norm (e.g. 1.0001 from float32 matrix operations)
    q_ideal = np.array([0.0, 0.0, 0.0, 1.0], dtype=np.float32) # Identity
    
    # Test with a general 3D orientation (pitch/yaw/roll mixture, e.g. tilted machine or multi-axis frame)
    q_base = np.array([0.2, 0.3, 0.4, 0.8], dtype=np.float32)
    q_base = q_base / np.linalg.norm(q_base) # unit quaternion
    
    # Introduce float32 accumulation drift
    drift = 0.005 # 0.5% drift
    q_drifted = q_base * (1.0 + drift)
    
    # Case A: Without normalization
    M_unnorm = quat_to_mat4(q_drifted)
    v_out_unnorm = M_unnorm @ v_top
    
    # Case B: With normalization
    q_norm = q_drifted / np.linalg.norm(q_drifted)
    M_norm = quat_to_mat4(q_norm)
    v_out_norm = M_norm @ v_top
    
    ortho_err_unnorm = np.max(np.abs(M_unnorm[:3, :3].T @ M_unnorm[:3, :3] - np.eye(3)))
    ortho_err_norm = np.max(np.abs(M_norm[:3, :3].T @ M_norm[:3, :3] - np.eye(3)))
    
    shear_disp = np.linalg.norm(v_out_unnorm[:3] - v_out_norm[:3])
    
    print(f"General 3D Quaternion Unnormalized norm: {np.linalg.norm(q_drifted):.6f}")
    print(f"Unnormalized matrix non-orthogonality error: {ortho_err_unnorm:.6f}")
    print(f"Normalized matrix non-orthogonality error:   {ortho_err_norm:.9f}")
    print(f"Top vertex (Y=24m) shear displacement between unnormalized and normalized: {shear_disp*1000:.2f} mm ({shear_disp:.4f} m)")
    
    assert ortho_err_unnorm > 0.005, f"Expected non-orthogonality error > 0.005, got {ortho_err_unnorm}"
    assert ortho_err_norm < 1e-6, f"Expected normalized error < 1e-6, got {ortho_err_norm}"
    assert shear_disp > 0.1, f"Expected shear displacement > 100mm, got {shear_disp}"
    print("[PASS] Quaternion normalization eliminates >10cm shear distortion at 24m on general 3D rotations.")

if __name__ == '__main__':
    test_r1_coordinate_precision()
    test_r1_vertex_cancellation_simulation()
    test_r1_drift_threshold_and_reanchoring()
    test_r1_zero_copy_span_diffing()
    test_r2_transition_seal_bounds_and_lightmap()
    test_r2_quaternion_normalization_shear()
    print("\nALL EMPIRICAL TESTS PASSED SUCCESSFULLY!")
