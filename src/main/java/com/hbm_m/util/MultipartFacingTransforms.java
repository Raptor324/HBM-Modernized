package com.hbm_m.util;

import net.minecraft.core.Direction;

/**
 * Ориентация multipart-моделей по {@link Direction}.
 * <p>
 * BER ({@code LegacyAnimator.setupBlockTransform}): {@code translate(0.5,0,0.5)} → {@code rotateY(90°)}
 * → {@code rotateY(legacyFacingRotationYDegrees)} — только для PoseStack, не подставлять эту таблицу
 * напрямую в {@code ModelHelper.transformQuadsByFacing}.</p>
 * <p>
 * Baked/chunk квады используют ту же конвенцию угла, что и blockstate rotationY / javadoc
 * {@code ModelHelper}: 0° North, 90° East, 180° South, 270° West — см.
 * {@link #vanillaChunkMeshRotationY(Direction)}.</p>
 */
public final class MultipartFacingTransforms {

    private MultipartFacingTransforms() {}

    /**
     * Второй поворот по Y в {@code setupBlockTransform} (после базовых 90°).
     */
    public static float legacyFacingRotationYDegrees(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    /**
     * Угол для {@code ModelHelper.transformQuadsByFacing} в стиле vanilla blockstate rotationY
     * (N=0, E=90, S=180, W=270). Подходит для assembler-like моделей в Iris/chunk пути.
     */
    public static int vanillaChunkMeshRotationY(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
    }

    /**
     * Advanced assembler: исторически baked-меш крутят ещё на 270° относительно той же vanilla-таблицы.
     */
    public static int advancedAssemblerBakedRotationY(Direction facing) {
        return normalizeDeg360(vanillaChunkMeshRotationY(facing) + 270);
    }

    /**
     * Единственный поворот Y для chunk mesh multipart-моделей, которые <b>не</b> задают {@code rotationY}
     * в blockstate: тот же суммарный угол, что {@code LegacyAnimator.setupBlockTransform}
     * (90° + {@link #legacyFacingRotationYDegrees}) — синхрон с VBO/BER.
     * <p>
     * Если в blockstate уже есть vanilla {@code rotationY}, не суммировать: либо убрать y из JSON,
     * либо не вызывать этот метод для квадов (двойной поворот).
     */
    public static int legacyBlockEntityBakedRotationY(Direction facing) {
        return normalizeDeg360(90 + (int) Math.round(legacyFacingRotationYDegrees(facing)));
    }

    /**
     * Единый источник истины для химзавода в chunk-конвенции (угол для квада в
     * {@code ModelHelper.transformQuadsByFacing}): N/S получают +180° к legacy-сумме, E/W без сдвига.
     */
    public static int chemicalPlantCanonicalRotationY(Direction facing) {
        int y = legacyBlockEntityBakedRotationY(facing);
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            return normalizeDeg360(y + 180);
        }
        return y;
    }

    /**
     * Угол для baked/chunk пути химзавода.
     */
    public static int chemicalPlantBakedRotationY(Direction facing) {
        return chemicalPlantCanonicalRotationY(facing);
    }

    /**
     * Переводит chunk-угол (квады) в эквивалентный угол для PoseStack rotateY.
     * Конвенции вращения в CPU-квадах и PoseStack направлены противоположно.
     */
    public static int poseYawFromChunkYaw(int chunkYawDeg) {
        return normalizeDeg360(-chunkYawDeg);
    }

    /**
     * Угол для BER/VBO пути химзавода (PoseStack), рассчитанный из того же canonical-угла.
     */
    public static int chemicalPlantPoseRotationY(Direction facing) {
        return poseYawFromChunkYaw(chemicalPlantCanonicalRotationY(facing));
    }

    /**
     * @deprecated Ошибочно смешивал второй шаг BER с конвенцией {@link #vanillaChunkMeshRotationY}; не использовать для квадов.
     */
    @Deprecated
    public static int berChunkMeshRotationY(Direction facing) {
        return normalizeDeg360(
            90 + (int) Math.round(legacyFacingRotationYDegrees(facing)));
    }

    /**
     * @deprecated см. {@link #berChunkMeshRotationY(Direction)}
     */
    @Deprecated
    public static int berChunkMeshRotationY(Direction facing, int extraOffsetDeg) {
        return normalizeDeg360(berChunkMeshRotationY(facing) + extraOffsetDeg);
    }

    /**
     * Двери: историческая таблица «facingDeg» + 90° + offset из {@code DoorDecl}.
     * Не совпадает с {@link #vanillaChunkMeshRotationY} / BER — отдельная таблица для сохранения вида.
     */
    public static int doorChunkMeshRotationY(Direction facing, int declOffsetDeg) {
        int facingDeg = switch (facing) {
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> 270;
            default -> 180;
        };
        return normalizeDeg360(90 + facingDeg + declOffsetDeg);
    }

    public static int normalizeDeg360(int degrees) {
        int d = degrees % 360;
        if (d < 0) {
            d += 360;
        }
        return d;
    }
}
