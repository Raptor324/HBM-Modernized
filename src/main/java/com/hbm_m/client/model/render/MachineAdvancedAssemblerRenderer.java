package com.hbm_m.client.model.render;

import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Оптимизированный рендерер с GPU буферами для статики и кэшированными квадами для анимаций.
 */
public class MachineAdvancedAssemblerRenderer 
    extends AbstractPartBasedRenderer<MachineAdvancedAssemblerBlockEntity, MachineAdvancedAssemblerBakedModel> {
    
    // GPU буферы для статических частей
    private VertexBuffer gpuBaseBuffer;
    private VertexBuffer gpuFrameBuffer;
    
    // Кэшированные квады для анимированных частей
    private List<BakedQuad> cachedRingQuads;
    private List<BakedQuad> cachedArm1LowerQuads;
    private List<BakedQuad> cachedArm1UpperQuads;
    private List<BakedQuad> cachedArm1HeadQuads;
    private List<BakedQuad> cachedArm1SpikeQuads;
    private List<BakedQuad> cachedArm2LowerQuads;
    private List<BakedQuad> cachedArm2UpperQuads;
    private List<BakedQuad> cachedArm2HeadQuads;
    private List<BakedQuad> cachedArm2SpikeQuads;
    
    private boolean meshesCompiled = false;
    
    // Кэш рецептов
    private ResourceLocation cachedRecipeId;
    private AssemblerRecipe cachedRecipe;
    
    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context context) {}
    
    @Override
    protected MachineAdvancedAssemblerBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineAdvancedAssemblerBakedModel model ? model : null;
    }
    
    @Override
    protected Direction getFacing(MachineAdvancedAssemblerBlockEntity blockEntity) {
        return blockEntity.getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);
    }
    
    private void ensureMeshesCompiled(MachineAdvancedAssemblerBakedModel model) {
        if (meshesCompiled) return;
        
        String baseKey = MachineAdvancedAssemblerBakedModel.class.getSimpleName() + ":Base";
        String frameKey = MachineAdvancedAssemblerBakedModel.class.getSimpleName() + ":Frame";
        
        // GPU буферы для статики
        gpuBaseBuffer = GlobalMeshCache.getOrCreateGPUBuffer(baseKey, model.getPart("Base"));
        gpuFrameBuffer = GlobalMeshCache.getOrCreateGPUBuffer(frameKey, model.getPart("Frame"));
        
        // Квады для анимаций
        cachedRingQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "Ring", model.getPart("Ring"));
        
        cachedArm1LowerQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "ArmLower1", model.getPart("ArmLower1"));
        cachedArm1UpperQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "ArmUpper1", model.getPart("ArmUpper1"));
        cachedArm1HeadQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "Head1", model.getPart("Head1"));
        cachedArm1SpikeQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "Spike1", model.getPart("Spike1"));
        
        cachedArm2LowerQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "ArmLower2", model.getPart("ArmLower2"));
        cachedArm2UpperQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "ArmUpper2", model.getPart("ArmUpper2"));
        cachedArm2HeadQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "Head2", model.getPart("Head2"));
        cachedArm2SpikeQuads = GlobalMeshCache.getOrCompile(
            MachineAdvancedAssemblerBakedModel.class, "Spike2", model.getPart("Spike2"));
        
        meshesCompiled = true;
    }
    
    @Override
    protected void renderParts(MachineAdvancedAssemblerBlockEntity be,
                                MachineAdvancedAssemblerBakedModel model,
                                LegacyAnimator animator,
                                float partialTick, int packedLight, int packedOverlay,
                                PoseStack poseStack, MultiBufferSource bufferSource) {
        
        ensureMeshesCompiled(model);
        
        // Статика через GPU
        animator.renderGPUBuffer(gpuBaseBuffer);
        if (be.frame) {
            animator.renderGPUBuffer(gpuFrameBuffer);
        }
        
        // Кольцо (анимация)
        animator.push();
        float ringSpin = Mth.lerp(partialTick, be.prevRingAngle, be.ringAngle);
        animator.rotate(ringSpin, 0, 1, 0);
        animator.renderQuads(cachedRingQuads);
        animator.pop();
        
        // Манипуляторы
        renderArm(animator, be.arms[0], cachedArm1LowerQuads, cachedArm1UpperQuads,
                  cachedArm1HeadQuads, cachedArm1SpikeQuads, false, partialTick);
        renderArm(animator, be.arms[1], cachedArm2LowerQuads, cachedArm2UpperQuads,
                  cachedArm2HeadQuads, cachedArm2SpikeQuads, true, partialTick);
        
        // Иконка рецепта
        Minecraft mc = getMinecraft();
        if (mc.player != null) {
            double distSq = mc.player.distanceToSqr(
                be.getBlockPos().getX() + 0.5,
                be.getBlockPos().getY() + 0.5,
                be.getBlockPos().getZ() + 0.5
            );
            
            if (distSq < 100) {
                renderRecipeIcon(be, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }
    }
    
    private void renderArm(LegacyAnimator animator,
                           MachineAdvancedAssemblerBlockEntity.AssemblerArm arm,
                           List<BakedQuad> lowerQuads, List<BakedQuad> upperQuads,
                           List<BakedQuad> headQuads, List<BakedQuad> spikeQuads,
                           boolean inverted, float partialTick) {
        
        float angle0 = Mth.lerp(partialTick, arm.prevAngles[0], arm.angles[0]);
        float angle1 = Mth.lerp(partialTick, arm.prevAngles[1], arm.angles[1]);
        float angle2 = Mth.lerp(partialTick, arm.prevAngles[2], arm.angles[2]);
        float angle3 = Mth.lerp(partialTick, arm.prevAngles[3], arm.angles[3]);
        
        float sign = inverted ? -1 : 1;
        float zBase = inverted ? -0.9375f : 0.9375f;
        
        animator.push();
        
        // Lower
        animator.translate(0, 1.625, zBase);
        animator.rotate(sign * angle0, 1, 0, 0);
        animator.translate(0, -1.625, -zBase);
        animator.renderQuads(lowerQuads);
        
        // Upper
        animator.translate(0, 2.375, zBase);
        animator.rotate(sign * angle1, 1, 0, 0);
        animator.translate(0, -2.375, -zBase);
        animator.renderQuads(upperQuads);
        
        // Head
        animator.translate(0, 2.375, zBase * 0.4667f);
        animator.rotate(sign * angle2, 1, 0, 0);
        animator.translate(0, -2.375, -zBase * 0.4667f);
        animator.renderQuads(headQuads);
        
        // Spike
        animator.translate(0, angle3, 0);
        animator.renderQuads(spikeQuads);
        
        animator.pop();
    }
    
    private void renderRecipeIcon(MachineAdvancedAssemblerBlockEntity be,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {
        
        ResourceLocation selectedRecipeId = be.getSelectedRecipeId();
        if (selectedRecipeId == null) return;
        
        Minecraft mc = getMinecraft();
        if (mc.player == null) return;
        
        if (cachedRecipe == null || !selectedRecipeId.equals(cachedRecipeId)) {
            cachedRecipeId = selectedRecipeId;
            if (be.getLevel() == null) return;
            
            cachedRecipe = be.getLevel().getRecipeManager()
                .byKey(selectedRecipeId)
                .filter(r -> r instanceof AssemblerRecipe)
                .map(r -> (AssemblerRecipe) r)
                .orElse(null);
        }
        
        if (cachedRecipe == null) return;
        
        ItemStack iconStack = cachedRecipe.getResultItem(null);
        if (iconStack.isEmpty()) return;
        
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0, 1.0625, 0);
        
        if (iconStack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BakedModel blockModel = mc.getBlockRenderer().getBlockModel(block.defaultBlockState());
            
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
        
        ItemRenderer itemRenderer = mc.getItemRenderer();
        itemRenderer.renderStatic(iconStack, ItemDisplayContext.FIXED, packedLight,
            packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
        
        poseStack.popPose();
    }
    
    @Override
    public boolean shouldRenderOffScreen(MachineAdvancedAssemblerBlockEntity blockEntity) {
        return false;
    }
    
    public void onResourceManagerReload() {
        meshesCompiled = false;
        cachedRecipe = null;
        cachedRecipeId = null;
        GlobalMeshCache.clear();
    }
}
