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
 *       W -&gt; 180, E -&gt; 0. The JSON root transform carries the base +90
 *       ({@code origin: corner, T(0.5,0,0.5), R(0,90,0)} - the repo-wide convention for
 *       rotated machine models, cf. crystallizer/zirnox_destroyed), so the block transform
 *       is the standard pattern {@code T(0.5,0,0.5) * R(legacy) * T(-0.5,0,-0.5)} and the
 *       net result is {@code T(0.5,0,0.5) * R(legacy+90)} - exactly the math of the
 *       original TESR (translate x+0.5/z+0.5 -&gt; rotate).</li>
 *   <li>the model is 5x5 in OBJ units (x/z -2.5..+2.5, y 0..5) and covers the whole
 *       5x5x5 multiblock; item geometry ends up centered at (0.5,·,0.5), the display
 *       section of the JSON is calibrated against the original {@code ItemRenderBase}
 *       (inventory scale 2.8*0.75 = 2.1 of a 16-unit icon -&gt; 0.13, entity scale
 *       1.5*0.25*0.75 = 0.28125 -&gt; 0.28, inventory camera -30/45 + point reflection
 *       -&gt; 30/135).</li>
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
        // Standard pattern (crystallizer): the JSON root transform T(0.5,0,0.5)*R(90)
        // is baked into the part quads, so the facing yaw goes in the middle.
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(MultipartFacingTransforms.legacyFacingRotationYDegrees(
                be.getBlockState().getValue(MachineZirnoxBlock.FACING)), 0, 1, 0);
        animator.translate(-0.5f, 0.0f, -0.5f);
    }
}
