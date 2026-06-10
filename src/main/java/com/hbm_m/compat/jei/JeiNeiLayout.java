package com.hbm_m.compat.jei;

/**
 * Slot layout helpers ported from {@code NEIGenericRecipeHandler} and {@code NEIUniversalHandler}.
 */
public final class JeiNeiLayout {

    private JeiNeiLayout() {
    }

    // --- NEIGenericRecipeHandler ---

    public static int[][] getGenericInputSlotPositions(int count) {
        if (count == 1) return new int[][]{{48, 24}};
        if (count == 2) return new int[][]{{30, 24}, {48, 24}};
        if (count == 3) return new int[][]{{12, 24}, {30, 24}, {48, 24}};
        if (count == 4) return new int[][]{{30, 15}, {48, 15}, {30, 33}, {48, 33}};
        if (count == 5) return new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}};
        if (count == 6) return new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}, {48, 33}};

        int[][] slots = new int[count][2];
        int cols = (count + 2) / 3;

        for (int i = 0; i < count; i++) {
            slots[i][0] = 12 + (i % cols) * 18 - (cols == 4 ? 18 : 0);
            slots[i][1] = 6 + (i / cols) * 18;
        }

        return slots;
    }

    public static int[][] getGenericOutputSlotPositions(int count) {
        return switch (count) {
            case 1 -> new int[][]{{102, 24}};
            case 2 -> new int[][]{{102, 24}, {120, 24}};
            case 3 -> new int[][]{{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][]{{102, 15}, {120, 15}, {102, 33}, {120, 33}};
            case 5 -> new int[][]{{102, 15}, {120, 15}, {102, 33}, {120, 33}, {138, 24}};
            case 6 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}};
            case 7 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}};
            case 8 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}, {138, 42}};
            default -> new int[count][2];
        };
    }

    // --- NEIUniversalHandler ---

    public static int[][] getUniversalInputCoords(int count) {
        return switch (count) {
            case 1 -> new int[][]{{48, 24}};
            case 2 -> new int[][]{{48, 24}, {30, 24}};
            case 3 -> new int[][]{{48, 24}, {30, 24}, {12, 24}};
            case 4 -> new int[][]{{48, 15}, {30, 15}, {48, 33}, {30, 33}};
            case 5 -> new int[][]{{48, 15}, {30, 15}, {12, 24}, {48, 33}, {30, 33}};
            case 6 -> new int[][]{{48, 15}, {30, 15}, {12, 15}, {48, 33}, {30, 33}, {12, 33}};
            case 7 -> new int[][]{{48, 6}, {30, 15}, {12, 15}, {48, 24}, {30, 33}, {12, 33}, {48, 42}};
            case 8 -> new int[][]{{48, 6}, {30, 6}, {12, 15}, {48, 24}, {30, 24}, {12, 33}, {48, 42}, {30, 42}};
            case 9 -> new int[][]{{48, 6}, {30, 6}, {12, 6}, {48, 24}, {30, 24}, {12, 24}, {48, 42}, {30, 42}, {12, 42}};
            default -> {
                int[][] slots = new int[count][2];
                for (int i = 0; i < count; i++) {
                    slots[i] = new int[]{i % 4 * 18, i / 4 * 18};
                }
                yield slots;
            }
        };
    }

    public static int[][] getUniversalOutputCoords(int count) {
        return getGenericOutputSlotPositions(count);
    }
}
