package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineChemicalFactoryBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.util.MultipartFacingTransforms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;

/**
 * Chemical Factory on the {@link MachineRenderers} factory -- port of 1.7.10
 * {@code RenderChemicalFactory}:
 * <ul>
 *   <li>{@code Base} is static; {@code Frame} renders based on the blockstate property
 *       FRAME (visible when a block sits above any cell of the structure's upper belt,
 *       as computed server-side by {@code MultiblockFrameHelper} -- the same system as
 *       advassembler; the 1.7.10 original only checked the block directly above the core);</li>
 *   <li>{@code Fan1}/{@code Fan2} spin around their own pivots
 *       (+-1, 0, 0) at -45 deg/tick (tracked rig, {@code Track.linear(-360, 0)},
 *       8-tick cycle) while at least one line is running
 *       (anim is incremented in the BE only on didProcess, as in the original);</li>
 *   <li>FACING rotation -- as in the original: rotate(90) + table
 *       (N=0, W=90, S=180, E=270), i.e. {@code 90 + legacyFacingRotationYDegrees};</li>
 *   <li>the final {@code translate(-0.5, 0, -0.5)} compensates the baked JSON
 *       root translation (0.5, 0, 0.5), see the pivots below.</li>
 * </ul>
 * <p>
 * Fan pivots in baked coordinates = OBJ pivot (+-1, 0, 0) + JSON translation
 * (0.5, 0, 0.5). If you change the JSON translation -- pivot = OBJ_PIVOT + JSON_translation.
 */
public final class MachineChemicalFactoryRenderer {

    private static final float FAN1_PIVOT_X = 1.5f;
    private static final float FAN2_PIVOT_X = -0.5f;
    private static final float FAN_PIVOT_Z = 0.5f;

    // Fans spin -45 deg/tick: -360 deg over an 8-tick cycle (ANIM TRACK v2).
    private static final MachineSpec.Track FAN_SPIN_TRACK = MachineSpec.Track.linear(-360f, 0f);

    /** The fan segment runs while anim advanced this tick (didProcess on an active lane). */
    private static boolean isFanSpinning(MachineChemicalFactoryBlockEntity be) {
        return be.getAnim(1.0F) != be.getAnim(0.0F);
    }

    public static void register() {
        MachineRenderers.machine("chemfactory", ModBlockEntities.CHEMICAL_FACTORY_BE.get(),
                MachineChemicalFactoryBlockEntity.class)
            .part("Base")
            .trackedChainPart("Fan1",
                    be -> MachineChemicalFactoryRenderer.isFanSpinning(be) ? 1L : 0L,
                    be -> 0.0,
                    be -> isFanSpinning(be) ? 8.0 : 0.0,
                    MachineSpec.JointNode.rotate(0f, 1f, 0f, FAN1_PIVOT_X, 0f, FAN_PIVOT_Z,
                            (be, pt, gt, out, off) -> true, FAN_SPIN_TRACK))
            .trackedChainPart("Fan2",
                    be -> MachineChemicalFactoryRenderer.isFanSpinning(be) ? 1L : 0L,
                    be -> 0.0,
                    be -> isFanSpinning(be) ? 8.0 : 0.0,
                    MachineSpec.JointNode.rotate(0f, 1f, 0f, FAN2_PIVOT_X, 0f, FAN_PIVOT_Z,
                            (be, pt, gt, out, off) -> true, FAN_SPIN_TRACK))
            .conditionalPart("Frame", be -> {
                var state = be.getBlockState();
                return state.hasProperty(MachineChemicalFactoryBlock.FRAME)
                        && state.getValue(MachineChemicalFactoryBlock.FRAME);
            })
            .itemExcept("Frame")
            .blockTransform(MachineChemicalFactoryRenderer::applyBlockTransform)
            .chunkRenderTypes(net.minecraft.client.renderer.RenderType.cutout())
            .register();
    }

    private MachineChemicalFactoryRenderer() {}

    // -- Block transform ------------------------------------------------

    private static void applyBlockTransform(MachineChemicalFactoryBlockEntity be, LegacyAnimator animator) {
        var state = be.getBlockState();
        animator.translate(0.5, 0.0, 0.5);
        if (state.hasProperty(MachineChemicalFactoryBlock.FACING)) {
            float facingRot = MultipartFacingTransforms.legacyFacingRotationYDegrees(
                    state.getValue(MachineChemicalFactoryBlock.FACING));
            animator.rotate(90f + facingRot, 0, 1, 0);
        } else {
            animator.rotate(90, 0, 1, 0);
        }
        // baked-space shift -0.5/-0.5: the parts are baked with the JSON root translation (0.5, 0, 0.5)
        animator.translate(-0.5f, 0.0f, -0.5f);
    }

}

