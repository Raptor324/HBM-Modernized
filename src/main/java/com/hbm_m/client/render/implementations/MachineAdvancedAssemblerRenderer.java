package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.client.machine.AdvancedAssemblerClientTicker;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
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
 * Продвинутый сборщик на фабрике {@link MachineRenderers}:
 * Base — статика; Frame — динамическая часть (видима по blockstate-свойству FRAME);
 * Ring — анимация вращения; 8 частей рук — две chain-группы (GPU bone skinning
 * включается автоматически, IDDS 1..4 на группу: lower/upper/head/spike);
 * иконка рецепта — immediate-хук.
 */
public final class MachineAdvancedAssemblerRenderer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final float ARM_PIVOT_Y_LOWER = 1.625f;
    private static final float ARM_PIVOT_Y_UPPER = 2.375f;
    private static final float ARM_Z_OFFSET = 0.9375f;
    private static final float ARM_HEAD_Z_SCALE = 0.4667f;
    private static final float RECIPE_ICON_MAX_DIST_SQ = 64.0f * 64.0f;

    public static void register() {
        MachineRenderers.machine("advassembler", ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(),
                MachineAdvancedAssemblerBlockEntity.class)
            .part("Base", MachineAdvancedAssemblerRenderer::applyBakeOffset)
            .dynamicPart("Frame", MachineAdvancedAssemblerRenderer::applyBakeOffset,
                    MachineAdvancedAssemblerRenderer::frameQuads, be -> "frame")
            .part("Ring", MachineAdvancedAssemblerRenderer::animateRing)
            .part("ArmLower1", (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 0, false))
            .part("ArmUpper1", (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 1, false))
            .part("Head1",     (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 2, false))
            .part("Spike1",    (be, pt, t, pose) -> applyArm(be, pt, pose, 0, 3, false))
            .part("ArmLower2", (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 0, true))
            .part("ArmUpper2", (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 1, true))
            .part("Head2",     (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 2, true))
            .part("Spike2",    (be, pt, t, pose) -> applyArm(be, pt, pose, 1, 3, true))
            .chain("ArmLower1", "ArmUpper1", "Head1", "Spike1")
            .chain("ArmLower2", "ArmUpper2", "Head2", "Spike2")
            .hook(MachineAdvancedAssemblerRenderer::renderRecipeIcon)
            .register();
    }

    private MachineAdvancedAssemblerRenderer() {}

    /**
     * Статический кластер (Base + merged Frame) в легаси рисовался внутри пуша
     * T(-0.5,0,-0.5) поверх блочного трансформа (baked-space частей JSON-модели).
     * Анимированные части этот офсет несут в собственных матрицах (ringMatrix),
     * поэтому только Base/Frame применяют его этим «аниматором».
     */
    private static boolean applyBakeOffset(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        pose.translate(-0.5f, 0f, -0.5f);
        return true;
    }

    private static Direction facing(MachineAdvancedAssemblerBlockEntity be) {
        return be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
    }

    // ── Frame: видима только по свойству FRAME ─────────────────────────

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> frameQuads(
            MachineAdvancedAssemblerBlockEntity be) {
        var state = be.getBlockState();
        if (!state.hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                || !state.getValue(MachineAdvancedAssemblerBlock.FRAME)) {
            return List.of();
        }
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if (!(AbstractPartBasedRenderer.unwrapFabricForwardingModels(raw)
                instanceof MachineAdvancedAssemblerBakedModel model)) {
            return List.of();
        }
        BakedModel part = model.getPart("Frame");
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompile("advassembler_Frame", part);
    }

    // ── Анимация: кольцо + цепочки рук ─────────────────────────────────

    /** Матрица кольца. RING_PIVOT_LOCAL = ZERO → pivot-компенсация вырождается в чистый поворот. */
    private static Matrix4f ringMatrix(float ringAngleDeg) {
        return new Matrix4f()
                .rotateY(ringAngleDeg * DEG_TO_RAD)
                .translate(-0.5f, 0f, -0.5f);
    }

    private static boolean animateRing(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        float ringLerped = Mth.lerp(partialTick, be.getPrevRingAngle(), be.getRingAngle());
        pose.last().pose().mul(ringMatrix(ringLerped));
        return true;
    }

    @Nullable
    private static AdvancedAssemblerClientTicker.AssemblerArm[] arms(MachineAdvancedAssemblerBlockEntity be) {
        Object arms = be.getArms();
        return arms instanceof AdvancedAssemblerClientTicker.AssemblerArm[] a ? a : null;
    }

    /**
     * Цепочка руки: матрица части {@code chainIndex} (0=lower, 1=upper, 2=head, 3=spike)
     * = ring · T1·Rx·T1' · T2·Rx·T2' · … накопительно, как в легаси (matLower→matUpper→matHead→matSpike).
     */
    private static boolean applyArm(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                    PoseStack pose, int armIndex, int chainIndex, boolean inverted) {
        AdvancedAssemblerClientTicker.AssemblerArm[] all = arms(be);
        if (all == null || all.length <= armIndex || all[armIndex] == null) return false;
        var arm = all[armIndex];

        float a0 = Mth.lerp(partialTick, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(partialTick, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(partialTick, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(partialTick, arm.prevAngles[3], arm.angles[3]);
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -ARM_Z_OFFSET : ARM_Z_OFFSET;
        float headZ = zBase * ARM_HEAD_Z_SCALE;
        float ringLerped = Mth.lerp(partialTick, be.getPrevRingAngle(), be.getRingAngle());

        Matrix4f m = ringMatrix(ringLerped);
        for (int i = 0; i <= chainIndex; i++) {
            switch (i) {
                case 0 -> m.translate(0.5f, ARM_PIVOT_Y_LOWER, 0.5f + zBase)
                        .rotateX(angleSign * a0 * DEG_TO_RAD)
                        .translate(-0.5f, -ARM_PIVOT_Y_LOWER, -(0.5f + zBase));
                case 1 -> m.translate(0.5f, ARM_PIVOT_Y_UPPER, 0.5f + zBase)
                        .rotateX(angleSign * a1 * DEG_TO_RAD)
                        .translate(-0.5f, -ARM_PIVOT_Y_UPPER, -(0.5f + zBase));
                case 2 -> m.translate(0.5f, ARM_PIVOT_Y_UPPER, 0.5f + headZ)
                        .rotateX(angleSign * a2 * DEG_TO_RAD)
                        .translate(-0.5f, -ARM_PIVOT_Y_UPPER, -(0.5f + headZ));
                case 3 -> m.translate(0, a3, 0);
            }
        }
        pose.last().pose().mul(m);
        return true;
    }

    // ── Иконка рецепта (hook) ──────────────────────────────────────────

    /**
     * Старый путь из «сырого» стека: R(90)·T(0,1.0625,0)·items (RING_PIVOT_LOCAL = ZERO,
     * сдвиг к центру вырождается). В текущем фрейме (блочный трансформ уже применён):
     * R(-90-legacy)·T(-0.5,0,-0.5)·R(90)·T(0,1.0625,0)·items.
     */
    private static void renderRecipeIcon(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        // BE-оверлоад: bypass fade/cull для контрапшенов и Sable sublevel.
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
