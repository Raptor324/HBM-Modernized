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
 *   <li>FACING rotation follows the original table (metadata-10): N -&gt; 90, S -&gt; 270,
 *       W -&gt; 180, E -&gt; 0;</li>
 *   <li>the JSON root transform was rewritten on 2026-10-08 to the repo-wide convention
 *       {@code origin: corner, T(0.5,0,0.5), R(0,90,0)} (same as intact zirnox), so the
 *       block transform is the standard pattern {@code T(0.5,0,0.5) * R(legacy) *
 *       T(-0.5,0,-0.5)} and the net result is {@code T(0.5,0,0.5) * R(90+legacy)} -
 *       exactly the math of the original TESR. The old compensation
 *       {@code translate(-0.5, 0, +1.5)} still described the pre-rewrite JSON and
 *       displaced the model by 2 blocks along the facing axis.</li>
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
        // Standard pattern (crystallizer): the JSON root transform T(0.5,0,0.5)*R(90)
        // is baked into the part quads, so the facing yaw goes in the middle.
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(MultipartFacingTransforms.legacyFacingRotationYDegrees(
                be.getBlockState().getValue(MachineZirnoxDestroyedBlock.FACING)), 0, 1, 0);
        animator.translate(-0.5f, 0.0f, -0.5f);
    }
}
