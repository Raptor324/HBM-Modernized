package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineZirnoxDestroyedBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineZirnoxDestroyedBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.util.MultipartFacingTransforms;

/**
 * Destroyed ZIRNOX on the {@link MachineRenderers} factory - port of 1.7.10
 * {@code RenderZirnoxDestroyed}: a single static part "Plane"
 * ({@code o Plane} in zirnox_destroyed.obj), no animation.
 * <ul>
 *   <li>FACING rotation uses the same original table: N -&gt; 90, S -&gt; 270, W -&gt; 180, E -&gt; 0;</li>
 *   <li>the JSON root transform is only T(0.5,0,-1.5), without rotation, so unlike
 *       {@link MachineZirnoxRenderer} the offset is subtracted after the full
 *       90+legacy rotation: the net result is T(0.5,0,0.5)*R(90+legacy).</li>
 * </ul>
 * Culling bounds come from {@code getRenderBoundingBox()} in
 * {@link MachineZirnoxDestroyedBlockEntity} (debris sticks out beyond 5x5x2).
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
        // Compensate the baked JSON root transform T(0.5,0,-1.5).
        animator.translate(-0.5, 0.0, 1.5);
    }
}
