package com.hbm_m.client.render.implementations;

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
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.compat.ContraptionRenderCompat;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Advanced assembler on the {@link MachineRenderers} factory:
 * Base is static; Frame is a dynamic part (visible per the blockstate FRAME property);
 * Ring is a rotation animation; 8 arm parts -- lower/upper/head/spike x 2 groups
 * (translate+rotate, instanced into a shared MDI alongside the statics);
 * the recipe icon is an immediate hook.
 */
public final class MachineAdvancedAssemblerRenderer {


    private static final float ARM_PIVOT_Y_LOWER = 1.625f;
    private static final float ARM_PIVOT_Y_UPPER = 2.375f;
    private static final float ARM_Z_OFFSET = 0.9375f;
    private static final float ARM_HEAD_Z_SCALE = 0.4667f;
    private static final float RECIPE_ICON_MAX_DIST_SQ = 64.0f * 64.0f;

    public static void register() {
        MachineRenderers.machine("advassembler", ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(),
                MachineAdvancedAssemblerBlockEntity.class)
            // Base/Frame are statics with the legacy bake offset: they fade out by the static
            // distance and the animation cutoff does not touch them (otherwise at a 0
            // modelUpdateDistance the machine would disappear entirely).
            .staticPart("Base", MachineAdvancedAssemblerRenderer::applyBakeOffset)
            .dynamicPart("Frame", MachineAdvancedAssemblerRenderer::frameQuads,
                    // The key must distinguish FRAME=false/true: a constant key would cache
                    // the renderer from the FIRST state seen (usually empty), forever.
                    be -> frameCacheKey(be),
                    MachineAdvancedAssemblerRenderer::applyBakeOffset)
            // Ring is a parametric GPU animation (the pattern's sample case): rotation
            // around Y through the block center. The legacy math mulPose(R)*T(-0.5,0,-0.5)
            // equals rotating about the pivot (0.5, ., 0.5) in geometry coordinates -- a pure
            // KIND_ROTATE; the CPU does not rebuild the record, the VSH moves the ring itself.
            // The record carries the animator's static tail T(-0.5,0,-0.5) as the base
            // offset (geometry lives in the legacy bake space centered at (0.5,.,0.5)):
            // without it the GPU ring lands half a block off the machine center.
            .parametricPart("Ring", MachineSpec.KIND_ROTATE, 0f, 1f, 0f, 0.5f, 0f, 0.5f,
                    -0.5f, 0f, -0.5f,
                    MachineAdvancedAssemblerRenderer::ringParams,
                    MachineAdvancedAssemblerRenderer::animateRing)
            .part("ArmLower1", (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 0, false))
            .part("ArmUpper1", (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 1, false))
            .part("Head1",     (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 2, false))
            .part("Spike1",    (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 3, false))
            .part("ArmLower2", (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 0, true))
            .part("ArmUpper2", (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 1, true))
            .part("Head2",     (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 2, true))
            .part("Spike2",    (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 3, true))
            .animationEpoch(MachineAdvancedAssemblerRenderer::animationEpoch)
            .hook(MachineAdvancedAssemblerRenderer::renderRecipeIcon)
            .register();
    }

    private MachineAdvancedAssemblerRenderer() {}

    /**
     * The static cluster (Base + merged Frame) was drawn in legacy inside a push of
     * T(-0.5,0,-0.5) on top of the block transform (the JSON model parts' baked space).
     * The animated parts carry this offset in their own matrices (ringMatrix),
     * so only Base/Frame apply it via this "animator".
     */
    private static boolean applyBakeOffset(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        pose.translate(-0.5f, 0f, -0.5f);
        return true;
    }

    private static Direction facing(MachineAdvancedAssemblerBlockEntity be) {
        return be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
    }

    // -- Frame: visible only per the FRAME property ---------------------

    private static String frameCacheKey(MachineAdvancedAssemblerBlockEntity be) {
        var state = be.getBlockState();
        boolean frame = state.hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                && state.getValue(MachineAdvancedAssemblerBlock.FRAME);
        return String.valueOf(frame);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> frameQuads(
            MachineAdvancedAssemblerBlockEntity be) {
        var state = be.getBlockState();
        if (!state.hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                || !state.getValue(MachineAdvancedAssemblerBlock.FRAME)) {
            return List.of();
        }
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if (!(AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw)
                instanceof ConfiguredMultipartBakedModel model)) {
            return List.of();
        }
        BakedModel part = model.getPart("Frame");
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompile("advassembler_Frame", part);
    }

    // -- Animation: ring + arm chains -----------------------------------

    /**
     * The assembler's animation epoch (contract: {@link MachineSpecBuilder#animationEpoch}):
     * prev+curr of the ring and of all angles of both arms. While the machine is idle
     * (all lerps finished, prev==curr), the epoch is constant -- MachineBer confirms the
     * animated parts with a roster assert without running animators; any movement changes
     * at least one (prev, curr) pair -- the machine automatically returns to the full path.
     * Cost: ~20 float reads, called once per machine per frame.
     */
    private static long animationEpoch(MachineAdvancedAssemblerBlockEntity be) {
        AdvancedAssemblerClientTicker ticker =
                be.getClientTicker() instanceof AdvancedAssemblerClientTicker t ? t : null;
        if (ticker == null) {
            return 0L;
        }
        long h = 0x9E3779B97F4A7C15L;
        h = h * 31 + java.lang.Float.floatToRawIntBits(ticker.getRingAngle());
        h = h * 31 + java.lang.Float.floatToRawIntBits(ticker.getPrevRingAngle());
        AdvancedAssemblerClientTicker.AssemblerArm[] arms = ticker.getArms();
        if (arms != null) {
            for (AdvancedAssemblerClientTicker.AssemblerArm arm : arms) {
                if (arm == null) continue;
                for (int i = 0; i < arm.angles.length; i++) {
                    h = h * 31 + java.lang.Float.floatToRawIntBits(arm.angles[i]);
                    h = h * 31 + java.lang.Float.floatToRawIntBits(arm.prevAngles[i]);
                }
            }
        }
        return h;
    }

    /**
     * Per-(machine, frame) animation snapshot: the ticker and the ring are resolved ONCE
     * per frame, not for each of the 9 animated parts (this used to add ~26 Method.invoke
     * calls per machine -- now it is direct ticker field reads).
     * Rendering is single-threaded; the shadow/main passes of one frame share the entry
     * (partialTick and angles are the same within a frame).
     */
    private static final class AsmAnimState {
        MachineAdvancedAssemblerBlockEntity be;
        long frame = -1L;
        @Nullable AdvancedAssemblerClientTicker ticker;
        float ringLerped;
    }

    private static final ThreadLocal<AsmAnimState> ANIM_STATE = ThreadLocal.withInitial(AsmAnimState::new);

    private static AsmAnimState animState(MachineAdvancedAssemblerBlockEntity be, float partialTick) {
        AsmAnimState s = ANIM_STATE.get();
        long frame = com.hbm_m.client.render.IrisShadowBatchCollector.renderFrame();
        if (s.be != be || s.frame != frame) {
            s.be = be;
            s.frame = frame;
            s.ticker = be.getClientTicker() instanceof AdvancedAssemblerClientTicker t ? t : null;
            s.ringLerped = s.ticker == null ? 0f
                    : Mth.lerp(partialTick, s.ticker.getPrevRingAngle(), s.ticker.getRingAngle());
        }
        return s;
    }

    /** Ring: pure rotation + bake offset, directly on the PoseStack (no new Matrix4f in the hot path). */
    private static void applyRingRotation(PoseStack pose, float ringAngleDeg) {
        pose.mulPose(Axis.YP.rotationDegrees(ringAngleDeg));
        pose.translate(-0.5f, 0f, -0.5f);
    }

    private static boolean animateRing(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        applyRingRotation(pose, animState(be, partialTick).ringLerped);
        return true;
    }

    /**
     * The ring's GPU joint parameters (attrib 15): the lerped angle. The value is stable
     * while the ring is idle (prev==curr) -- a skip-write of the record, zero upload.
     */
    private static boolean ringParams(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                      long gameTime, float[] out) {
        out[0] = animState(be, partialTick).ringLerped;
        return true;
    }

    @Nullable
    private static AdvancedAssemblerClientTicker.AssemblerArm[] arms(MachineAdvancedAssemblerBlockEntity be,
                                                                     float partialTick) {
        AdvancedAssemblerClientTicker ticker = animState(be, partialTick).ticker;
        return ticker == null ? null : ticker.getArms();
    }

    /**
     * Arm chain: the part {@code chainIndex} matrix (0=lower, 1=upper, 2=head, 3=spike)
     * = ring * T1*Rx*T1' * T2*Rx*T2' * ... accumulated, as in legacy (matLower->matUpper->matHead->matSpike).
     */
    private static boolean applyArm(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                    PoseStack pose, int armIndex, int chainIndex, boolean inverted) {
        AdvancedAssemblerClientTicker.AssemblerArm[] all = arms(be, partialTick);
        if (all == null || all.length <= armIndex || all[armIndex] == null) return false;
        var arm = all[armIndex];

        float a0 = Mth.lerp(partialTick, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(partialTick, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(partialTick, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(partialTick, arm.prevAngles[3], arm.angles[3]);
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -ARM_Z_OFFSET : ARM_Z_OFFSET;
        float headZ = zBase * ARM_HEAD_Z_SCALE;
        float ringLerped = animState(be, partialTick).ringLerped;

        applyRingRotation(pose, ringLerped);
        for (int i = 0; i <= chainIndex; i++) {
            switch (i) {
                case 0 -> {
                    pose.translate(0.5f, ARM_PIVOT_Y_LOWER, 0.5f + zBase);
                    pose.mulPose(Axis.XP.rotationDegrees(angleSign * a0));
                    pose.translate(-0.5f, -ARM_PIVOT_Y_LOWER, -(0.5f + zBase));
                }
                case 1 -> {
                    pose.translate(0.5f, ARM_PIVOT_Y_UPPER, 0.5f + zBase);
                    pose.mulPose(Axis.XP.rotationDegrees(angleSign * a1));
                    pose.translate(-0.5f, -ARM_PIVOT_Y_UPPER, -(0.5f + zBase));
                }
                case 2 -> {
                    pose.translate(0.5f, ARM_PIVOT_Y_UPPER, 0.5f + headZ);
                    pose.mulPose(Axis.XP.rotationDegrees(angleSign * a2));
                    pose.translate(-0.5f, -ARM_PIVOT_Y_UPPER, -(0.5f + headZ));
                }
                case 3 -> pose.translate(0, a3, 0);
            }
        }
        return true;
    }

    // -- Recipe icon (hook) ---------------------------------------------

    /**
     * The old path from the "raw" stack: R(90)*T(0,1.0625,0)*items (RING_PIVOT_LOCAL = ZERO,
     * the shift to center degenerates). In the current frame (the block transform already applied):
     * R(-90-legacy)*T(-0.5,0,-0.5)*R(90)*T(0,1.0625,0)*items.
     */
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

        float legacyDeg = MultipartFacingTransforms.legacyFacingRotationYDegrees(facing(be));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90f - legacyDeg));
        poseStack.translate(-0.5, 0, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0, 1.0625, 0);

        if (icon.getItem() instanceof BlockItem bi) {
            var blockModel = mc.getBlockRenderer().getBlockModel(bi.getBlock().defaultBlockState());
            if (blockModel.isGui3d()) {
                poseStack.translate(-1, -0.2625, 1);
            } else {
                poseStack.translate(-1, -0.125, 1);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
        } else {
            poseStack.translate(-1, -0.2, 1);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        }

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        mc.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.FIXED,
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
