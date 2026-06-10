package com.hbm_m.compat.jei;

import com.hbm_m.recipe.AnvilRecipe.OverlayType;

/**
 * Slot layout for anvil JEI recipes, ported from {@code AnvilRecipeHandler.RecipeSet}.
 */
public final class JeiAnvilLayout {

    public record Layout(int inLine, int outLine, int inOX, int inOY, int outOX, int outOY, int anvX, int anvY,
                         OverlayType shape) {
    }

    private JeiAnvilLayout() {
    }

    public static Layout resolve(OverlayType overlay, int inputCount, int outputCount) {
        OverlayType shape = overlay != OverlayType.NONE
                ? overlay
                : resolveShape(inputCount, outputCount);

        return switch (shape) {
            case SMITHING -> new Layout(1, 1, 48, 24, 102, 24, 75, 31, OverlayType.SMITHING);
            case RECYCLING -> new Layout(1, 6, 12, 24, 48, 6, 30, 31, OverlayType.RECYCLING);
            case CONSTRUCTION -> new Layout(6, 1, 12, 6, 138, 24, 120, 31, OverlayType.CONSTRUCTION);
            default -> new Layout(4, 4, 3, 6, 93, 6, 75, 31, OverlayType.NONE);
        };
    }

    private static OverlayType resolveShape(int inputCount, int outputCount) {
        if (inputCount == 1 && outputCount == 1) {
            return OverlayType.SMITHING;
        }
        if (inputCount == 1 && outputCount > 1) {
            return OverlayType.RECYCLING;
        }
        if (inputCount > 1 && outputCount == 1) {
            return OverlayType.CONSTRUCTION;
        }
        return OverlayType.NONE;
    }

    public static int[][] getInputPositions(Layout layout, int count) {
        int[][] slots = new int[count][2];
        for (int i = 0; i < count; i++) {
            slots[i][0] = layout.inOX() + 18 * (i % layout.inLine());
            slots[i][1] = layout.inOY() + 18 * (i / layout.inLine());
        }
        return slots;
    }

    public static int[][] getOutputPositions(Layout layout, int count) {
        int[][] slots = new int[count][2];
        for (int i = 0; i < count; i++) {
            slots[i][0] = layout.outOX() + 18 * (i % layout.outLine());
            slots[i][1] = layout.outOY() + 18 * (i / layout.outLine());
        }
        return slots;
    }
}
