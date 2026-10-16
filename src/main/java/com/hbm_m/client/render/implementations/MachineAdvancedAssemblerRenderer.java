package com.hbm_m.client.render.implementations;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.client.machine.AdvancedAssemblerClientTicker;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.compat.ContraptionRenderCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Advanced assembler on the {@link MachineRenderers} factory:
 * Base is static; Frame is a dynamic part (visible per the blockstate FRAME property);
 * Ring is a TRACKED SPIN (spinningPart, ANIM TRACK FORMAT v2 - a 2-point cycle
 * track the VSH samples from the segment header + uAnimTime); the 8 arm parts
 * are TRACKED CHAINS (node 0 = the same ring spin, then 3 streamed rotates + the
 * spike lift - the per-arm FK math runs in the vertex shader); the recipe icon is
 * an immediate hook.
 *
 * <p>Arm decomposition (see {@link #armNodes}): the legacy per-frame CPU chain was
 * {@code base * J0 * J1 * J2 * J3} with {@code base = rotationY(ring) * T(-0.5,0,-0.5)}.
 * The ring rotation is folded into each chain as NODE 0 (pivot = ring pivot - base
 * offset - the composition is bit-identical to the legacy base) - now as a TRACKED
 * node (keyframe cycle track + segment header) so the record's packed AnimParams
 * carry only the segment header + the 4 streamed arm words, and the epoch-keyed
 * word cache in MachineBer skips the whole rig while the ring segment runs. The
 * four joints follow as STREAMED chain nodes with their exact legacy pivots; each
 * part declares the chain TRUNCATED at its own joint (lower = mount+J0, upper =
 * mount+J0..J1, head = mount+J0..J2, spike = mount+J0..J3), reproducing the
 * legacy accumulated matrices exactly.
 *
 * @credit Spark Profiler Remediation / Forward Kinematics Memoization
 */
public final class MachineAdvancedAssemblerRenderer {


    private static final float ARM_PIVOT_Y_LOWER = 1.625f;
    private static final float ARM_PIVOT_Y_UPPER = 2.375f;
    private static final float ARM_Z_OFFSET = 0.9375f;
    private static final float ARM_HEAD_Z_OFFSET = 0.4375f;
    private static final float RECIPE_ICON_MAX_DIST_SQ = 35.0f * 35.0f;

    public static void register() {
        MachineRenderers.machine("advassembler", ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(),
                MachineAdvancedAssemblerBlockEntity.class)
            // Base/Frame are statics with the legacy bake offset: they fade out by the static
            // distance and the animation cutoff does not touch them (otherwise at a 0
            // modelUpdateDistance the machine would disappear entirely).
            .staticPart("Base", MachineAdvancedAssemblerRenderer::applyBakeOffset)
            .conditionalPart("Frame", MachineAdvancedAssemblerRenderer::applyBakeOffset,
                    be -> be.getBlockState().hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                            && be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FRAME))
            .itemExcept("Frame")
            // Ring is a TRACKED SPIN (spinningPart, ANIM TRACK FORMAT v2):
            // rotation around Y through the block center, one 2-point cycle track
            // (0 -> 360 deg) sampled by the VSH from the segment header
            // Ring: parametric rotation around (0.5, 0, 0.5) with baseOffset (-0.5, 0, -0.5).
            // Smoothly lerped between ticks via ringAngle.
            .rotatingPart("Ring", 0f, 1f, 0f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f,
                    MachineAdvancedAssemblerRenderer::ringAngle)
            // The 8 arm parts: joint chains of 1 ring mount rotation (node 0) + up to 4 GPU joints
            // (3 rotates + the spike lift along Y), each truncated at the part's own depth.
            // Evaluated in the vertex shader on the vanilla MDI path via AnimParams (5 words),
            // with automatic PoseStack CPU fallback on Iris/shadows.
            .jointChainPart("ArmLower1", "ArmLower1",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 1, false))
            .jointChainPart("ArmUpper1", "ArmUpper1",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 2, false))
            .jointChainPart("Head1", "Head1",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 3, false))
            .jointChainPart("Spike1", "Spike1",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 4, false))
            .jointChainPart("ArmLower2", "ArmLower2",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 1, true))
            .jointChainPart("ArmUpper2", "ArmUpper2",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 2, true))
            .jointChainPart("Head2", "Head2",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 3, true))
            .jointChainPart("Spike2", "Spike2",
                    -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 4, true))
            .animationEpoch(MachineAdvancedAssemblerRenderer::animationEpoch)
            // Anonymous hook (not a method ref) to declare mayDrawContent: the
            // recipe icon is the ONLY per-frame content of the spec, and while
            // it is absent the fast-assert may confirm the whole machine. The
            // icon appears only with a recipe in progress - covered by the
            // ring-segment epoch (crafting edge), so the frame content appears
            // the epoch flips and the full path draws it.
            .hook(new MachineRenderHook<MachineAdvancedAssemblerBlockEntity>() {
                @Override
                public void render(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay, MachineRenderApi api) {
                    MachineAdvancedAssemblerRenderer.renderRecipeIcon(
                            be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, api);
                }

                @Override
                public boolean mayDrawContent(MachineAdvancedAssemblerBlockEntity be) {
                    return !be.getClientRecipeIcon().isEmpty();
                }
            })
            .register();
    }

    private MachineAdvancedAssemblerRenderer() {}

    /**
     * The static cluster (Base + merged Frame) was drawn in legacy inside a push of
     * T(-0.5,0,-0.5) on top of the block transform (the JSON model parts' baked space).
     * The animated parts carry this offset in the record statics, so only Base/Frame
     * apply it via this "animator".
     */
    private static boolean applyBakeOffset(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        pose.translate(-0.5f, 0f, -0.5f);
        return true;
    }

    private static Direction facing(MachineAdvancedAssemblerBlockEntity be) {
        return be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
    }


    // -- Animation state (read directly from the client ticker) ---------

    private static @Nullable AdvancedAssemblerClientTicker ticker(MachineAdvancedAssemblerBlockEntity be) {
        return be.getClientTicker() instanceof AdvancedAssemblerClientTicker t ? t : null;
    }

    private static double ringAngle(MachineAdvancedAssemblerBlockEntity be, float partialTick) {
        AdvancedAssemblerClientTicker t = ticker(be);
        return t == null ? 0.0 : t.getRingAngle(partialTick);
    }

    /**
     * The ring-spin node used as NODE 0 of every arm chain: matches the Ring part's
     * rotation exactly so arm mounts and ring mesh stay in 100% lockstep.
     */
    private static MachineSpec.JointNode<MachineAdvancedAssemblerBlockEntity> ringMountNode() {
        return MachineSpec.JointNode.rotate(0f, 1f, 0f, 0.5f, 0f, 0.5f,
                (be, pt, gt, out, off) -> {
                    out[off] = (float) ringAngle(be, pt);
                    return true;
                });
    }

    @SuppressWarnings("unchecked")
    private static MachineSpec.JointNode<MachineAdvancedAssemblerBlockEntity>[] armChainNodes(
            int armIndex, int depth, boolean inverted) {
        MachineSpec.JointNode<MachineAdvancedAssemblerBlockEntity>[] joints =
                armNodes(armIndex, depth, inverted);
        MachineSpec.JointNode<MachineAdvancedAssemblerBlockEntity>[] all =
                new MachineSpec.JointNode[joints.length + 1];
        all[0] = ringMountNode();
        System.arraycopy(joints, 0, all, 1, joints.length);
        return all;
    }

    @Nullable
    private static AdvancedAssemblerClientTicker.AssemblerArm arm(MachineAdvancedAssemblerBlockEntity be,
                                                                  int armIndex) {
        AdvancedAssemblerClientTicker ticker = ticker(be);
        if (ticker == null) return null;
        AdvancedAssemblerClientTicker.AssemblerArm[] all = ticker.getArms();
        return (all == null || all.length <= armIndex) ? null : all[armIndex];
    }

    /**
     * Machine animation epoch: stable when both the ring and arms are stationary
     * (prev == curr for ring, animVersion unchanged for arms), enabling full
     * frozen evaluation skip and zero instance upload when idle.
     */
    private static long animationEpoch(MachineAdvancedAssemblerBlockEntity be) {
        AdvancedAssemblerClientTicker ticker = ticker(be);
        if (ticker == null) {
            return 0L;
        }
        long h = 0x9E3779B97F4A7C15L;
        h = h * 31 + Float.floatToRawIntBits(ticker.getRingAngle());
        h = h * 31 + Float.floatToRawIntBits(ticker.getPrevRingAngle());
        return h * 31 + ticker.animVersion;
    }

    // -- Arm chains: 4 GPU joints, declared per part at its own depth ----

    /**
     * The arm FK chain, node-for-node the legacy {@code buildArmChain} math with the
     * same pivots and the same per-joint lerped angle sources:
     * <pre>
     *   J0 (Lower): Rx through (0.5, 1.625,  0.5 + zBase)             by  sign*a0
     *   J1 (Upper): Rx through (0.5, 2.375,  0.5 + zBase)             by  sign*a1
     *   J2 (Head):  Rx through (0.5, 2.375,  0.5 + headZ)             by  sign*a2
     *   J3 (Spike): translate along Y by the RAW a3 (never sign-flipped - the striker lift)
     * </pre>
     * with {@code zBase = +-0.9375}, {@code headZ = zBase * 0.4667} (static per group -
     * folded into the pivots) and {@code sign = -1} for the mirrored group. The
     * declaration order matches the legacy PoseStack op order; the shader applies the
     * nodes in reverse vertex order, reproducing the product {@code base*J0*J1*J2*J3}.
     * Only the first {@code depth} nodes are declared (the part's own joint - the
     * legacy accumulated matrix out[depth-1]).
     */
    @SuppressWarnings("unchecked")
    private static MachineSpec.JointNode<MachineAdvancedAssemblerBlockEntity>[] armNodes(
            int armIndex, int depth, boolean inverted) {
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -ARM_Z_OFFSET : ARM_Z_OFFSET;
        float headZ = inverted ? -ARM_HEAD_Z_OFFSET : ARM_HEAD_Z_OFFSET;

        MachineSpec.JointNode<MachineAdvancedAssemblerBlockEntity>[] all =
                new MachineSpec.JointNode[] {
                        MachineSpec.JointNode.rotate(1f, 0f, 0f, 0.5f, ARM_PIVOT_Y_LOWER, 0.5f + zBase,
                                armAngleFn(armIndex, 0, angleSign)),
                        MachineSpec.JointNode.rotate(1f, 0f, 0f, 0.5f, ARM_PIVOT_Y_UPPER, 0.5f + zBase,
                                armAngleFn(armIndex, 1, angleSign)),
                        MachineSpec.JointNode.rotate(1f, 0f, 0f, 0.5f, ARM_PIVOT_Y_UPPER, 0.5f + headZ,
                                armAngleFn(armIndex, 2, angleSign)),
                        MachineSpec.JointNode.translate(0f, 1f, 0f,
                                armAngleFn(armIndex, 3, 1f))
                };
        return Arrays.copyOf(all, depth);
    }

    /**
     * One joint's per-frame angle: the ticker's lerped value. Returning false (no
     * ticker / no arm) keeps the legacy "part not drawn" behavior.
     */
    private static MachineSpec.NodeParams<MachineAdvancedAssemblerBlockEntity> armAngleFn(
            int armIndex, int joint, float angleSign) {
        return (be, pt, gt, out, off) -> {
            AdvancedAssemblerClientTicker.AssemblerArm arm = arm(be, armIndex);
            if (arm == null) return false;
            out[off] = angleSign * Mth.lerp(pt, arm.prevAngles[joint], arm.angles[joint]);
            return true;
        };
    }

    // -- Recipe icon (hook) ---------------------------------------------

    private static void renderRecipeIcon(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        // BE overload: bypass fade/cull for contraptions and the Sable sublevel.
        if (RenderSystem.isOnRenderThread()
                && !ContraptionRenderCompat.isContraptionRender(be)
                && com.hbm_m.client.render.RenderDistanceHelper.distanceSqToCamera(api.blockPos()) > RECIPE_ICON_MAX_DIST_SQ) {
            return;
        }

        ItemStack icon = be.getClientRecipeIcon();
        if (icon.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0, 1.0625, 0.0);

        boolean isBlock = icon.getItem() instanceof BlockItem;
        boolean rendered3d = false;
        if (isBlock) {
            BlockItem bi = (BlockItem) icon.getItem();
            var blockModel = mc.getBlockRenderer().getBlockModel(bi.getBlock().defaultBlockState());
            rendered3d = blockModel.isGui3d();
        }

        if (rendered3d) {
            poseStack.translate(0.0, -0.0625, 0.0);
        } else if (isBlock) {
            poseStack.translate(0.0, -0.125, 0.0);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(0.0, -0.25, 0.0);
        }

        poseStack.scale(1.25F, 1.25F, 1.25F);

        if (rendered3d) {
            poseStack.translate(0.0, 0.1F, 0.0);
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.translate(0.0, 0.05F, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.scale(0.25F, 0.25F, 0.25F);
        } else {
            poseStack.translate(0.0, 0.1F, 0.0);
            poseStack.scale(0.5128205F, 0.5128205F, 0.5128205F);
            poseStack.translate(0.0, -0.05F, 0.0);
        }

        mc.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.NONE,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
