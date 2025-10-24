package com.hbm_m.client.model.render;

import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.config.ModClothConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MachineAdvancedAssemblerRenderer extends AbstractPartBasedRenderer<MachineAdvancedAssemblerBlockEntity, MachineAdvancedAssemblerBakedModel> {

    private MachineAdvancedAssemblerVboRenderer gpu;
    private MachineAdvancedAssemblerBakedModel cachedModel;
    
    // НОВОЕ: Instanced рендереры для статических частей
    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile boolean instancersInitialized = false;

    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineAdvancedAssemblerBakedModel model) {
        if (instancersInitialized) return;  // Double-check pattern
        
        try {
            BakedModel baseModel = model.getPart("Base");
            if (baseModel != null) {
                var baseData = ObjModelVboBuilder.buildSinglePart(baseModel);
                instancedBase = new InstancedStaticPartRenderer(baseData);
            }
            
            BakedModel frameModel = model.getPart("Frame");
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel);
                instancedFrame = new InstancedStaticPartRenderer(frameData);
            }
            
            instancersInitialized = true;
        } catch (Exception e) {
            instancedBase = null;
            instancedFrame = null;
        }
    }
    
    // ✅ Wrapper с double-check locking
    private void initializeInstancedRenderers(MachineAdvancedAssemblerBakedModel model) {
        if (!instancersInitialized) {  // Первая проверка без лока
            initializeInstancedRenderersSync(model);
        }
    }

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
        
        BlockPos blockPos = be.getBlockPos();
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);
        
        // ✅ КРИТИЧНО: Проверяем occlusion ДО добавления в instanced batch!
        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return; // Машина полностью заблокирована - пропускаем ВСЕ части
        }
        
        if (!instancersInitialized) {
            initializeInstancedRenderers(model);
        }
        
        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAdvancedAssemblerVboRenderer(model);
        }
        
        boolean shouldUpdateAnimation = !shouldSkipAnimationUpdate(blockPos);
        
        // Рендер статических частей (ТОЛЬКО если прошли occlusion check)
        if (instancedBase != null) {
            instancedBase.addInstance(poseStack, dynamicLight, blockPos, be);
        } else {
            gpu.renderStaticBase(poseStack, dynamicLight, blockPos);
        }
        
        if (be.frame) {
            if (instancedFrame != null) {
                instancedFrame.addInstance(poseStack, dynamicLight, blockPos, be);
            } else {
                gpu.renderStaticFrame(poseStack, dynamicLight, blockPos);
            }
        }
        
        // Анимация обновляется реже для дальних машин
        if (shouldUpdateAnimation) {
            renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos);
        }
        
        renderRecipeIcon(be, poseStack, bufferSource, dynamicLight, packedOverlay);
    }

    /**
     * ✅ НОВОЕ: Проверка, нужно ли пропустить фрейм на основе дистанции
     */
    private boolean shouldSkipAnimationUpdate(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        
        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;
        
        // ✅ Ближние машины: анимация каждый фрейм
        if (distance <= thresholdBlocks) {
            return false;
        }
        
        // ✅ Дальние машины: вообще без анимации (статичные)
        if (distance > thresholdBlocks * 1.5) {
            return true; // Полностью отключаем анимацию
        }
        
        // ✅ Средняя дистанция: анимация через фрейм
        long frameTime = minecraft.getFrameTimeNs();
        long posHash = blockPos.asLong();
        return (posHash + frameTime) % 2 != 0;
    }

    /**
     * ВАЖНО: Вызывать в конце рендера ВСЕХ машин для флаша батчей
     */
    public static void flushInstancedBatches() {
        if (instancedBase != null) instancedBase.flush();
        if (instancedFrame != null) instancedFrame.flush();
    }

    private void renderAnimated(MachineAdvancedAssemblerBlockEntity be, float pt, PoseStack pose, int blockLight, BlockPos blockPos) {
        float ring = Mth.lerp(pt, be.prevRingAngle, be.ringAngle);
        Matrix4f ringMat = new Matrix4f().rotateY((float) Math.toRadians(ring));
        gpu.renderPart(pose, blockLight, "Ring", ringMat, blockPos);

        renderArm(be.arms[0], false, pt, pose, blockLight, ringMat, blockPos);
        renderArm(be.arms[1], true, pt, pose, blockLight, ringMat, blockPos);
    }

    private void renderArm(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm, boolean inverted,
                      float pt, PoseStack pose, int blockLight, Matrix4f baseTransform, BlockPos blockPos) {
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
        gpu.renderPart(pose, blockLight, lowerName, lowerMat, blockPos);

        Matrix4f upperMat = new Matrix4f(lowerMat)
                .translate(0, 2.375f, zBase)
                .rotateX((float) Math.toRadians(angleSign * a1))
                .translate(0, -2.375f, -zBase);
        gpu.renderPart(pose, blockLight, upperName, upperMat, blockPos);

        Matrix4f headMat = new Matrix4f(upperMat)
                .translate(0, 2.375f, zBase * 0.4667f)
                .rotateX((float) Math.toRadians(angleSign * a2))
                .translate(0, -2.375f, -zBase * 0.4667f);
        gpu.renderPart(pose, blockLight, headName, headMat, blockPos);

        Matrix4f spikeMat = new Matrix4f(headMat)
                .translate(0, a3, 0);
        gpu.renderPart(pose, blockLight, spikeName, spikeMat, blockPos);
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
