package com.hbm_m.block.network;

import net.minecraft.core.Direction;

/**
 * 1:1 {@code BlockCraneBase.getIcon(IBlockAccess, ...)} und {@code getRotationFromSide} fuer das Datagen: welche Textur auf
 * welcher Seite liegt, abhaengig von Eingang und wirksamem Ausgang, und wie die Oberseite gedreht ist.
 */
public final class CraneTextures {

    private CraneTextures() {}

    /** Texturnamen eines Kranes (Original: die Icons aus {@code registerBlockIcons}). */
    public record Set(String top, String side, String in, String sideIn, String out, String sideOut, String prefix, boolean extractorRotation) {

        public String directional() { return prefix + "_top"; }
        public String directionalUp() { return prefix + "_side_up"; }
        public String directionalDown() { return prefix + "_side_down"; }
        public String turnLeft() { return prefix + "_top_left"; }
        public String turnRight() { return prefix + "_top_right"; }
        public String sideLeftTurnUp() { return prefix + "_side_left_turn_up"; }
        public String sideRightTurnUp() { return prefix + "_side_right_turn_up"; }
        public String sideLeftTurnDown() { return prefix + "_side_left_turn_down"; }
        public String sideRightTurnDown() { return prefix + "_side_right_turn_down"; }
        public String sideUpTurnLeft() { return prefix + "_side_up_turn_left"; }
        public String sideUpTurnRight() { return prefix + "_side_up_turn_right"; }
        public String sideDownTurnLeft() { return prefix + "_side_down_turn_left"; }
        public String sideDownTurnRight() { return prefix + "_side_down_turn_right"; }
    }

    public static Set standard(String prefix, boolean extractorRotation) {
        return new Set("crane_top", "crane_side", "crane_in", "crane_side_in", "crane_out", "crane_side_out", prefix, extractorRotation);
    }

    /** Forge 1.7.10 {@code ForgeDirection.ROTATION_MATRIX} (Index = 3D-Datenwert). */
    private static final int[][] ROTATION_MATRIX = {
            { 0, 1, 4, 5, 3, 2 },
            { 0, 1, 5, 4, 2, 3 },
            { 5, 4, 2, 3, 0, 1 },
            { 4, 5, 2, 3, 1, 0 },
            { 2, 3, 1, 0, 4, 5 },
            { 3, 2, 0, 1, 4, 5 },
    };

    /** {@code dir.getRotation(axis)}. */
    public static Direction getRotation(Direction dir, Direction axis) {
        return Direction.from3DDataValue(ROTATION_MATRIX[axis.get3DDataValue()][dir.get3DDataValue()]);
    }

    /** Textur der Seite {@code side} im Weltmodell. */
    public static String icon(Set t, Direction inputSide, Direction output, Direction side) {
        boolean outputSideOverridden = output.getOpposite() != inputSide;
        Direction outputSide = outputSideOverridden ? output : inputSide.getOpposite();

        // linke Hand: Daumen = Eingang, Zeigefinger = Ausgang, Mittelfinger = diese Richtung
        Direction leftHandRotation = getRotation(outputSide, inputSide);

        if (side == Direction.DOWN || side == Direction.UP) {
            if (side == outputSide) return t.out();
            if (side == inputSide) return t.in();

            if (side == Direction.UP) {
                if (outputSideOverridden) {
                    if (leftHandRotation == Direction.UP) return t.turnLeft();
                    if (leftHandRotation == Direction.DOWN) return t.turnRight();
                } else return t.directional();
            }

            return t.top();
        }

        if (side == outputSide) return t.sideOut();
        if (side == inputSide) return t.sideIn();

        if (outputSideOverridden) {
            if (leftHandRotation == side) {
                if (outputSide == Direction.UP) return t.sideLeftTurnUp();
                if (outputSide == Direction.DOWN) return t.sideRightTurnDown();
                if (inputSide == Direction.UP) return t.sideUpTurnRight();
                if (inputSide == Direction.DOWN) return t.sideDownTurnLeft();
            }
            if (leftHandRotation.getOpposite() == side) {
                if (outputSide == Direction.UP) return t.sideRightTurnUp();
                if (outputSide == Direction.DOWN) return t.sideLeftTurnDown();
                if (inputSide == Direction.UP) return t.sideUpTurnLeft();
                if (inputSide == Direction.DOWN) return t.sideDownTurnRight();
            }
        } else {
            if (outputSide == Direction.UP) return t.directionalUp();
            if (outputSide == Direction.DOWN) return t.directionalDown();
        }

        return t.side();
    }

    /** Textur im Inventar (Original: {@code getIcon(side, 0)}). */
    public static String itemIcon(Set t, Direction side) {
        if (side == Direction.DOWN) return t.out();
        if (side == Direction.UP) return t.in();
        return t.directionalUp();
    }

    /**
     * {@code getRotationFromSide(..., 1)} der Oberseite als {@code uvRotateTop} (0-3). Nur bei waagrechtem Eingang.
     */
    public static int topRotation(Set t, Direction inputSide, Direction output) {
        int meta = inputSide.get3DDataValue();
        if (meta <= 1) return 0;

        if (t.extractorRotation()) {
            boolean overridden = output.getOpposite() != inputSide;
            Direction outputSide = overridden ? output : inputSide.getOpposite();
            Direction leftHandDirection = getRotation(outputSide, inputSide);
            if (leftHandDirection == Direction.UP) {
                if (meta == 2) return 2;
                if (meta == 3) return 1;
                if (meta == 4) return 3;
                if (meta == 5) return 0;
            }
            if (leftHandDirection == Direction.DOWN) {
                if (meta == 2) return 1;
                if (meta == 3) return 2;
                if (meta == 4) return 0;
                if (meta == 5) return 3;
            }
            if (meta == 2) return 0;
            if (meta == 3) return 3;
            if (meta == 4) return 2;
            return 1;
        }

        if (meta == 2) return 3;
        if (meta == 3) return 0;
        if (meta == 4) return 1;
        return 2;
    }

    /** RenderBlocks {@code uvRotateTop} -> Modell-Rotation im Uhrzeigersinn. */
    public static int topRotationDegrees(int uvRotateTop) {
        return switch (uvRotateTop) {
            case 1 -> 90;
            case 2 -> 270;
            case 3 -> 180;
            default -> 0;
        };
    }
}
