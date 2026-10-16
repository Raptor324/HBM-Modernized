package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineZirnoxBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.util.MultipartFacingTransforms;

/**
 * ZIRNOX на фабрике {@link MachineRenderers} — порт 1.7.10 {@code RenderZirnox}:
 * <ul>
 *   <li>единственная статическая часть "Plane" ({@code o Plane} в zirnox.obj);
 *       анимации нет, оригинальный флаг {@code tilted} (падение реактора из
 *       системы machine gravity 528) не портирован — в модернизации этой
 *       подсистемы нет;</li>
 *   <li>поворот по FACING — таблица оригинала (metadata−10): N→90°, S→270°,
 *       W→180°, E→0°, т.е. базовые 90° + {@code legacyFacingRotationYDegrees};</li>
 *   <li>модель выпекается вместе с root-трансформом JSON T(0.5,0,-1.5)·R(90)
 *       (ловушка OBJ root-transform), поэтому после T(0.5,0,0.5)+R(legacy)
 *       вычитаем его: суммарно T(0.5,0,0.5)·R(legacy+90) — ровно математика
 *       оригинального TESR (translate x+0.5/z+0.5 → rotate).</li>
 * </ul>
 * Габарит куллинга — {@code getRenderBoundingBox()} в
 * {@link MachineZirnoxBlockEntity} (структура 5×5×5, ядро в центре основания).
 */
public final class MachineZirnoxRenderer {

    public static void register() {
        MachineRenderers.machine("zirnox", ModBlockEntities.ZIRNOX_BE.get(),
                MachineZirnoxBlockEntity.class)
            .part("Plane")
            .blockTransform(MachineZirnoxRenderer::applyBlockTransform)
            .register();
    }

    private MachineZirnoxRenderer() {}

    private static void applyBlockTransform(MachineZirnoxBlockEntity be, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(MultipartFacingTransforms.legacyFacingRotationYDegrees(
                be.getBlockState().getValue(MachineZirnoxBlock.FACING)), 0, 1, 0);
        // Компенсация baked root-трансформа JSON T(0.5,0,-1.5)·R(90).
        animator.translate(-0.5, 0.0, 1.5);
    }
}
