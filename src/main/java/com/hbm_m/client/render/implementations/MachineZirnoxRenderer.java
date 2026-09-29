package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineZirnoxBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.util.MultipartFacingTransforms;

/**
 * ZIRNOX on the {@link MachineRenderers} factory - port of 1.7.10 {@code RenderZirnox}:
 * <ul>
 *   <li>a single static part "Plane" ({@code o Plane} in zirnox.obj); no animation -
 *       the original {@code tilted} flag (reactor toppling from the machine gravity
 *       system 528) is not ported, since the modernization has no such subsystem;</li>
 *   <li>FACING rotation follows the original table (metadata-10): N -&gt; 90, S -&gt; 270,
 *       W -&gt; 180, E -&gt; 0, i.e. base 90 degrees + {@code legacyFacingRotationYDegrees};</li>
 *   <li>the model is baked together with the JSON root transform T(0.5,0,-1.5)*R(90)
 *       (the OBJ root-transform trap), so after T(0.5,0,0.5)+R(legacy) we subtract it:
 *       the net result is T(0.5,0,0.5)*R(legacy+90) - exactly the math of the original
 *       TESR (translate x+0.5/z+0.5 -&gt; rotate).</li>
 * </ul>
 * Culling bounds come from {@code getRenderBoundingBox()} in
 * {@link MachineZirnoxBlockEntity} (5x5x5 structure, core at the center of the base).
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
        // Compensate the baked JSON root transform T(0.5,0,-1.5)*R(90).
        animator.translate(-0.5, 0.0, 1.5);
    }
}
