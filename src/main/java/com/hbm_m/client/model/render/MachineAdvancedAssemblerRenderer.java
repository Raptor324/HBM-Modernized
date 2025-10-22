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
    
    // НОВОЕ: Instanced рендереры для статических частей
    private static InstancedStaticPartRenderer instancedBase;
    private static InstancedStaticPartRenderer instancedFrame;
    private static boolean instancersInitialized = false;

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
        
        // КРИТИЧЕСКИ ВАЖНО: получаем РЕАЛЬНОЕ освещение из мира
        int blockLight = be.getLevel() != null 
            ? be.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, be.getBlockPos())
            : net.minecraft.client.renderer.LightTexture.block(packedLight);
        
        int skyLight = be.getLevel() != null 
            ? be.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, be.getBlockPos())
            : net.minecraft.client.renderer.LightTexture.sky(packedLight);
        
        // Упаковываем обратно в packedLight формат
        int dynamicLight = net.minecraft.client.renderer.LightTexture.pack(blockLight, skyLight);
        
        // Сбрасываем флаг привязки текстур
        AbstractGpuVboRenderer.TextureBinder.resetForAssembler();
        
        // Инициализируем instanced рендереры (один раз)
        if (!instancersInitialized) {
            initializeInstancedRenderers(model);
        }
        
        // Создаём/обновляем GPU рендерер для анимированных частей
        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAdvancedAssemblerVboRenderer(model);
        }
        
        // ОПТИМИЗАЦИЯ: Добавляем Base в батч
        if (instancedBase != null) {
            instancedBase.addInstance(poseStack, dynamicLight);
        } else {
            gpu.renderStaticBase(poseStack, dynamicLight);
        }
        
        // УСЛОВНЫЙ РЕНДЕР: Frame только если be.frame == true
        if (be.frame) {
            if (instancedFrame != null) {
                instancedFrame.addInstance(poseStack, dynamicLight);
            } else {
                gpu.renderStaticFrame(poseStack, dynamicLight);
            }
        }
        
        // Анимированные части — тот же шейдер, но через uniform
        renderAnimated(be, partialTick, poseStack, dynamicLight);
        renderRecipeIcon(be, poseStack, bufferSource, dynamicLight, packedOverlay);
    }

    /**
     * Инициализация instanced рендереров (вызывается один раз)
     */
    private void initializeInstancedRenderers(MachineAdvancedAssemblerBakedModel model) {
        if (instancersInitialized) return;

        try {
            // Создаём VBO для Base
            BakedModel baseModel = model.getPart("Base");
            if (baseModel != null) {
                var baseData = ObjModelVboBuilder.buildSinglePart(baseModel);
                instancedBase = new InstancedStaticPartRenderer(baseData);
            }

            // Создаём VBO для Frame
            BakedModel frameModel = model.getPart("Frame");
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel);
                instancedFrame = new InstancedStaticPartRenderer(frameData);
            }

            instancersInitialized = true;
        } catch (Exception e) {
            // При ошибке используем обычный рендер
            instancedBase = null;
            instancedFrame = null;
        }
    }

    /**
     * ВАЖНО: Вызывать в конце рендера ВСЕХ машин для флаша батчей
     */
    public static void flushInstancedBatches() {
        if (instancedBase != null) instancedBase.flush();
        if (instancedFrame != null) instancedFrame.flush();
    }

    private void renderAnimated(MachineAdvancedAssemblerBlockEntity be, float pt, PoseStack pose, int blockLight) {
        float ring = Mth.lerp(pt, be.prevRingAngle, be.ringAngle);
        Matrix4f ringMat = new Matrix4f().rotateY((float) Math.toRadians(ring));
        gpu.renderPart(pose, blockLight, "Ring", ringMat);

        renderArm(be.arms[0], false, pt, pose, blockLight, ringMat);
        renderArm(be.arms[1], true, pt, pose, blockLight, ringMat);
    }

    private void renderArm(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, boolean inverted,
                           float pt, PoseStack pose, int blockLight, Matrix4f baseTransform) {
        float a0 = Mth.lerp(pt, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(pt, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(pt, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(pt, arm.prevAngles[3], arm.angles[3]);

        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -0.9375f : 0.9375f;
        
        String lowerName = inverted ? "ArmLower2" : "ArmLower1";
        String upperName = inverted ? "ArmUpper2" : "ArmUpper1";
        String headName = inverted ? "Head2" : "Head1";
        String spikeName = inverted ? "Spike2" : "Spike1";

        Matrix4f lowerMat = new Matrix4f(baseTransform)
                .translate(0, 1.625f, zBase)
                .rotateX((float) Math.toRadians(angleSign * a0))
                .translate(0, -1.625f, -zBase);
        gpu.renderPart(pose, blockLight, lowerName, lowerMat);

        Matrix4f upperMat = new Matrix4f(lowerMat)
                .translate(0, 2.375f, zBase)
                .rotateX((float) Math.toRadians(angleSign * a1))
                .translate(0, -2.375f, -zBase);
        gpu.renderPart(pose, blockLight, upperName, upperMat);

        Matrix4f headMat = new Matrix4f(upperMat)
                .translate(0, 2.375f, zBase * 0.4667f)
                .rotateX((float) Math.toRadians(angleSign * a2))
                .translate(0, -2.375f, -zBase * 0.4667f);
        gpu.renderPart(pose, blockLight, headName, headMat);

        Matrix4f spikeMat = new Matrix4f(headMat)
                .translate(0, a3, 0);
        gpu.renderPart(pose, blockLight, spikeName, spikeMat);
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
    public boolean shouldRenderOffScreen(MachineAdvancedAssemblerBlockEntity be) { 
        return false; 
    }

    @Override 
    public int getViewDistance() { 
        return 128; 
    }
    
    public void onResourceManagerReload() {
        if (instancedBase != null) instancedBase.cleanup();
        if (instancedFrame != null) instancedFrame.cleanup();
        instancedBase = null;
        instancedFrame = null;
        instancersInitialized = false;
        
        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
        gpu = null;
        cachedModel = null;
    }
}
