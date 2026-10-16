package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineZirnoxDestroyedBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineZirnoxDestroyedBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.util.MultipartFacingTransforms;

/**
 * Разрушенный ZIRNOX на фабрике {@link MachineRenderers} — порт 1.7.10
 * {@code RenderZirnoxDestroyed}: единственная статическая часть "Plane"
 * ({@code o Plane} в zirnox_destroyed.obj), без анимации.
 * <ul>
 *   <li>поворот по FACING — та же таблица оригинала: N→90°, S→270°, W→180°, E→0°;</li>
 *   <li>root-трансформ JSON — только T(0.5,0,-1.5), без поворота, поэтому в
 *       отличие от {@link MachineZirnoxRenderer} вычитаем сдвиг после полного
 *       поворота 90°+legacy: суммарно T(0.5,0,0.5)·R(90+legacy).</li>
 * </ul>
 * Габарит куллинга — {@code getRenderBoundingBox()} в
 * {@link MachineZirnoxDestroyedBlockEntity} (обломки выступают за 5×5×2).
 */
public final class MachineZirnoxDestroyedRenderer {

    public static void register() {
        MachineRenderers.machine("zirnox_destroyed", ModBlockEntities.ZIRNOX_DESTROYED_BE.get(),
                MachineZirnoxDestroyedBlockEntity.class)
            .part("Plane")
            .blockTransform(MachineZirnoxDestroyedRenderer::applyBlockTransform)
            .register();
    }

    private MachineZirnoxDestroyedRenderer() {}

    private static void applyBlockTransform(MachineZirnoxDestroyedBlockEntity be, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(90f + MultipartFacingTransforms.legacyFacingRotationYDegrees(
                be.getBlockState().getValue(MachineZirnoxDestroyedBlock.FACING)), 0, 1, 0);
        // Компенсация baked root-трансформа JSON T(0.5,0,-1.5).
        animator.translate(-0.5, 0.0, 1.5);
    }
}
