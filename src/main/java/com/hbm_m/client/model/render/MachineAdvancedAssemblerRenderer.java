package com.hbm_m.client.model.render;

import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class MachineAdvancedAssemblerRenderer extends AbstractPartBasedRenderer<MachineAdvancedAssemblerBlockEntity, MachineAdvancedAssemblerBakedModel> {

    private MachineAdvancedAssemblerVboRenderer gpu;
    private MachineAdvancedAssemblerBakedModel cachedModel;

    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    protected MachineAdvancedAssemblerBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineAdvancedAssemblerBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineAdvancedAssemblerBlockEntity be) {
        return be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
    }

    @Override
    protected void renderParts(MachineAdvancedAssemblerBlockEntity be,
                                MachineAdvancedAssemblerBakedModel model,
                                LegacyAnimator animator,
                                float partialTick,
                                int packedLight,
                                int packedOverlay,
                                PoseStack poseStack,
                                MultiBufferSource bufferSource) {
        // ✅ Создаем рендерер только один раз
        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAdvancedAssemblerVboRenderer(model);
        }

        // ✅ GPU-рендер с правильным освещением
        gpu.renderStaticBase(poseStack, packedLight);
        if (be.frame) {
            gpu.renderStaticFrame(poseStack, packedLight);
        }

        renderAnimated(be, partialTick, poseStack, packedLight);

        // CPU-рендер для иконки рецепта
        renderRecipeIcon(be, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderAnimated(MachineAdvancedAssemblerBlockEntity be, float pt, PoseStack pose, int packedLight) {
        // Кольцо
        float ring = Mth.lerp(pt, be.prevRingAngle, be.ringAngle);
        Matrix4f ringMat = new Matrix4f().rotateY((float) Math.toRadians(ring));
        gpu.renderPart(pose, packedLight, "Ring", ringMat);

        // Руки
        renderArm(be.arms[0], false, pt, pose, packedLight, 0.9375f);
        renderArm(be.arms[1], true, pt, pose, packedLight, -0.9375f);
    }

    private void renderArm(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, boolean inverted,
                           float pt, PoseStack pose, int packedLight, float zBase) {
        float a0 = Mth.lerp(pt, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(pt, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(pt, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(pt, arm.prevAngles[3], arm.angles[3]);
        float sign = inverted ? -1f : 1f;

        // lower
        Matrix4f lower = new Matrix4f()
                .translate(0, 1.625f, zBase)
                .rotateX((float) Math.toRadians(sign * a0))
                .translate(0, -1.625f, -zBase);
        gpu.renderPart(pose, packedLight, inverted ? "ArmLower2" : "ArmLower1", lower);

        // upper
        Matrix4f upper = new Matrix4f()
                .translate(0, 2.375f, zBase)
                .rotateX((float) Math.toRadians(sign * a1))
                .translate(0, -2.375f, -zBase);
        gpu.renderPart(pose, packedLight, inverted ? "ArmUpper2" : "ArmUpper1", upper);

        // head
        Matrix4f head = new Matrix4f()
                .translate(0, 2.375f, zBase * 0.4667f)
                .rotateX((float) Math.toRadians(sign * a2))
                .translate(0, -2.375f, -zBase * 0.4667f);
        gpu.renderPart(pose, packedLight, inverted ? "Head2" : "Head1", head);

        // spike
        Matrix4f spike = new Matrix4f().translate(0, a3, 0);
        gpu.renderPart(pose, packedLight, inverted ? "Spike2" : "Spike1", spike);
    }

    private void renderRecipeIcon(MachineAdvancedAssemblerBlockEntity be,
                                   PoseStack poseStack, MultiBufferSource buffers,
                                   int packedLight, int packedOverlay) {
        var selectedRecipeId = be.getSelectedRecipeId();
        if (selectedRecipeId == null) return;
        
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        var recipe = be.getLevel() == null ? null : be.getLevel().getRecipeManager()
                .byKey(selectedRecipeId)
                .filter(r -> r instanceof com.hbm_m.recipe.AssemblerRecipe)
                .map(r -> (com.hbm_m.recipe.AssemblerRecipe) r)
                .orElse(null);
        
        if (recipe == null) return;
        
        ItemStack icon = recipe.getResultItem(null);
        if (icon.isEmpty()) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0, 1.0625, 0);

        if (icon.getItem() instanceof BlockItem bi) {
            var blockModel = mc.getBlockRenderer().getBlockModel(bi.getBlock().defaultBlockState());
            if (blockModel.isGui3d()) {
                poseStack.translate(0, -0.0625, 0);
            } else {
                poseStack.translate(0, -0.125, 0);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.translate(-0.5, -0.5, -0.03);
        }

        mc.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffers,
                be.getLevel(),
                0
        );
        
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineAdvancedAssemblerBlockEntity be) { return false; }

    @Override
    public int getViewDistance() { return 128; }

    public void onResourceManagerReload() {
        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
        gpu = null;
        cachedModel = null;
    }
}
