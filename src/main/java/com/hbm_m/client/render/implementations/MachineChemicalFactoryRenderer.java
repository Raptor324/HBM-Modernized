package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineChemicalFactoryBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.vertex.PoseStack;

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
 *       (+-1, 0, 0) at {@code -anim*45 deg}/tick while at least one line is running
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

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final float FAN1_PIVOT_X = 1.5f;
    private static final float FAN2_PIVOT_X = -0.5f;
    private static final float FAN_PIVOT_Z = 0.5f;

    public static void register() {
        MachineRenderers.machine("chemfactory", ModBlockEntities.CHEMICAL_FACTORY_BE.get(),
                MachineChemicalFactoryBlockEntity.class)
            .part("Base")
            .part("Fan1", MachineChemicalFactoryRenderer::animateFan1)
            .part("Fan2", MachineChemicalFactoryRenderer::animateFan2)
            .dynamicPart("Frame", MachineChemicalFactoryRenderer::frameQuads,
                    // The key must distinguish FRAME=false/true: a constant key would cache
                    // the renderer from the first state seen, forever (see advassembler/chemplant).
                    MachineChemicalFactoryRenderer::frameCacheKey)
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

    // -- Parts ----------------------------------------------------------

    private static boolean animateFan1(MachineChemicalFactoryBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        return animateFan(be, partialTick, pose, FAN1_PIVOT_X);
    }

    private static boolean animateFan2(MachineChemicalFactoryBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        return animateFan(be, partialTick, pose, FAN2_PIVOT_X);
    }

    /** Original: translate(+-1,0,0) -> rotate(-anim*45 % 360) -> translate(-+1,0,0). */
    private static boolean animateFan(MachineChemicalFactoryBlockEntity be, float partialTick,
                                      PoseStack pose, float pivotX) {
        float anim = be.getAnim(partialTick);
        float deg = (-anim * 45f) % 360f;
        if (deg < 0f) deg += 360f;
        pose.last().pose()
                .translate(pivotX, 0f, FAN_PIVOT_Z)
                .rotateY(deg * DEG_TO_RAD)
                .translate(-pivotX, 0f, -FAN_PIVOT_Z);
        return true;
    }

    // -- Frame: visible only per the FRAME property ---------------------

    private static String frameCacheKey(MachineChemicalFactoryBlockEntity be) {
        var state = be.getBlockState();
        boolean frame = state.hasProperty(MachineChemicalFactoryBlock.FRAME)
                && state.getValue(MachineChemicalFactoryBlock.FRAME);
        return String.valueOf(frame);
    }

    private static List<BakedQuad> frameQuads(MachineChemicalFactoryBlockEntity be) {
        var state = be.getBlockState();
        if (!state.hasProperty(MachineChemicalFactoryBlock.FRAME)
                || !state.getValue(MachineChemicalFactoryBlock.FRAME)) {
            return List.of();
        }
        BakedModel part = factoryPart(be, "Frame");
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompile("chemfactory_Frame", part);
    }

    private static @Nullable BakedModel factoryPart(MachineChemicalFactoryBlockEntity be, String partName) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        return raw instanceof AbstractMultipartBakedModel mp ? mp.getPart(partName) : null;
    }
}
