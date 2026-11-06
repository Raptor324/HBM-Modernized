package com.hbm_m.client.render;

import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Precomputed cardinal orientation quaternions, rotation matrices, and transform helpers.
 * Completely eliminates runtime trigonometry, heap allocations, and PoseStack churn
 * during block facing transformations.
 */
public final class CardinalOrientation {

    private static final float INV_SQRT_2 = 0.7071067811865475f;

    /**
     * Precomputed cardinal quaternions indexed by Direction.get3DDataValue() (0..5):
     * DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5.
     * Evaluates total rotation (90° base + legacy facing rotation Y):
     * NORTH -> 90°, SOUTH -> 270°, WEST -> 180°, EAST -> 0° (Identity).
     */
    public static final Quaternionf[] CARDINAL_QUATERNIONS = new Quaternionf[] {
        new Quaternionf(0.0f, INV_SQRT_2, 0.0f, INV_SQRT_2),  // 0: DOWN (default North, 90°)
        new Quaternionf(0.0f, INV_SQRT_2, 0.0f, INV_SQRT_2),  // 1: UP (default North, 90°)
        new Quaternionf(0.0f, INV_SQRT_2, 0.0f, INV_SQRT_2),  // 2: NORTH (90°)
        new Quaternionf(0.0f, INV_SQRT_2, 0.0f, -INV_SQRT_2), // 3: SOUTH (270°)
        new Quaternionf(0.0f, 1.0f, 0.0f, 0.0f),               // 4: WEST (180°)
        new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)                // 5: EAST (0° / Identity)
    };

    /**
     * Precomputed cardinal quaternions for Chemical Plant block transforms,
     * indexed by Direction.get3DDataValue() (0..5).
     */
    public static final Quaternionf[] CHEMICAL_PLANT_QUATERNIONS = new Quaternionf[6];

    /**
     * Precomputed packed VBO quaternion words (word0 = [qx, qy], word1 = [qz, qw] in snorm16).
     * Eliminates runtime call to packQuatWord during instance record generation.
     */
    public static final int[][] CARDINAL_PACKED_QUAT_WORDS = new int[][] {
        { 0x5A820000, 0x5A820000 },  // 0: DOWN
        { 0x5A820000, 0x5A820000 },  // 1: UP
        { 0x5A820000, 0x5A820000 },  // 2: NORTH
        { 0x5A820000, 0xA57E0000 },  // 3: SOUTH
        { 0x7FFF0000, 0x00000000 },  // 4: WEST
        { 0x00000000, 0x7FFF0000 }   // 5: EAST
    };

    static {
        for (Direction d : Direction.values()) {
            float rotY = MultipartFacingTransforms.chemicalPlantPoseRotationY(d);
            CHEMICAL_PLANT_QUATERNIONS[d.get3DDataValue()] = Axis.YP.rotationDegrees(rotY);
        }
    }

    private CardinalOrientation() {}

    /**
     * Returns the precomputed static cardinal quaternion for the given facing.
     */
    public static Quaternionf get(Direction facing) {
        if (facing == null) return CARDINAL_QUATERNIONS[2]; // NORTH fallback
        return CARDINAL_QUATERNIONS[facing.get3DDataValue()];
    }

    /**
     * Returns the precomputed static cardinal quaternion for Chemical Plant with the given facing.
     */
    public static Quaternionf getChemicalPlant(Direction facing) {
        if (facing == null) return CHEMICAL_PLANT_QUATERNIONS[2]; // NORTH fallback
        return CHEMICAL_PLANT_QUATERNIONS[facing.get3DDataValue()];
    }

    /**
     * Whether the cardinal rotation for this facing is identity (0°).
     * For EAST facing machines, rotation can be completely skipped.
     */
    public static boolean isIdentity(Direction facing) {
        return facing == Direction.EAST;
    }

    /**
     * Rotates a local translation vector (lx, ly, lz) by the cardinal facing around Y in-place.
     * Pure coordinate swapping and sign inversion without trigonometric calls or matrix allocations.
     */
    public static void rotateLocalVector(Direction facing, float lx, float ly, float lz, Vector3f out) {
        if (facing == null) facing = Direction.NORTH;
        switch (facing) {
            case NORTH -> out.set(lz, ly, -lx);
            case SOUTH -> out.set(-lz, ly, lx);
            case WEST  -> out.set(-lx, ly, -lz);
            case EAST  -> out.set(lx, ly, lz);
            default    -> out.set(lz, ly, -lx);
        }
    }
}
