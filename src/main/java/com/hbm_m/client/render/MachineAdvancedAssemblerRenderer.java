package com.hbm_m.client.render;

import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.shader.ImmediateFallbackRenderer;
import com.hbm_m.client.render.shader.RenderPathManager;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MachineAdvancedAssemblerRenderer extends AbstractPartBasedRenderer<MachineAdvancedAssemblerBlockEntity, MachineAdvancedAssemblerBakedModel> {

    private MachineAdvancedAssemblerVboRenderer gpu;
    private MachineAdvancedAssemblerBakedModel cachedModel;
    
    // НОВОЕ: Instanced рендереры для статических частей
    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile boolean instancersInitialized = false;
    private static MultiBufferSource.BufferSource cachedIconBuffer = null;
    private static final ConcurrentHashMap<String, ImmediateFallbackRenderer> fallbackRenderers = new ConcurrentHashMap<>();


    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineAdvancedAssemblerBakedModel model) {
        if (instancersInitialized) return;
        
        try {
            BakedModel baseModel = model.getPart("Base");
            InstancedStaticPartRenderer tempBase = null;
            if (baseModel != null) {
                var baseData = ObjModelVboBuilder.buildSinglePart(baseModel);
                tempBase = new InstancedStaticPartRenderer(baseData);
            }
            
            BakedModel frameModel = model.getPart("Frame");
            InstancedStaticPartRenderer tempFrame = null;
            if (frameModel != null) {
                var frameData = ObjModelVboBuilder.buildSinglePart(frameModel);
                tempFrame = new InstancedStaticPartRenderer(frameData);
            }
            
            //  КРИТИЧНО: Устанавливаем поля ДО флага
            instancedBase = tempBase;
            instancedFrame = tempFrame;
            
            //  Memory barrier: все записи видны после этого
            instancersInitialized = true;
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize instanced renderers", e);
            instancedBase = null;
            instancedFrame = null;
        }
    }
    
    //  Wrapper с double-check locking
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
        
        //  Occlusion check

        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }
        
        //  НОВОЕ: Определяем путь рендера
        boolean useFallback = RenderPathManager.shouldUseFallback();
        
        if (useFallback) {
            // ═══════════════════════════════════════════════════════
            // FALLBACK PATH: Immediate mode рендер (совместим с шейдерами)
            // ═══════════════════════════════════════════════════════
            renderWithFallback(be, model, partialTick, poseStack, dynamicLight);
        } else {
            // ═══════════════════════════════════════════════════════
            // OPTIMIZED PATH: VBO рендер с кастомным шейдером
            // ═══════════════════════════════════════════════════════
            renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos);
        }
        
        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSrc) {
            bufferSrc.endBatch();
        }
    }

    private void renderWithFallback(MachineAdvancedAssemblerBlockEntity be,
        MachineAdvancedAssemblerBakedModel model,
        float partialTick,
        PoseStack poseStack,
        int packedLight) {
    
        try {
            // Рендер статических частей - каждая часть рендерится НЕЗАВИСИМО
            renderFallbackPart("Base", model.getPart("Base"), poseStack, packedLight, null);
            
            if (be.frame) {
                renderFallbackPart("Frame", model.getPart("Frame"), poseStack, packedLight, null);
            }
            
            // Рендер анимированных частей
            boolean shouldUpdateAnimation = !shouldSkipAnimationUpdate(be.getBlockPos());
            if (shouldUpdateAnimation) {
                renderAnimatedFallback(be, model, partialTick, poseStack, packedLight);
            }
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error in fallback render", e);
        }
    }

    /**
     *  УПРОЩЕННЫЙ: Рендер одной части модели
     */
    private void renderFallbackPart(String partName, BakedModel partModel,
            PoseStack poseStack, int packedLight,
            Matrix4f additionalTransform) {
        
        if (partModel == null) {
            return;
        }
        
        try {
            // Получаем/создаем рендер для этой части
            ImmediateFallbackRenderer renderer = fallbackRenderers.computeIfAbsent(
                partName,
                k -> new ImmediateFallbackRenderer(partModel)
            );
            
            // Рендерим - просто и надежно
            renderer.render(poseStack, packedLight, additionalTransform);
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error rendering fallback part {}: {}", partName, e.getMessage());
            // Удаляем проблемный рендер из кэша для повторного создания
            fallbackRenderers.remove(partName);
        }
    }

    /**
     *  НОВЫЙ МЕТОД: Анимированный рендер в fallback режиме
     */
    private void renderAnimatedFallback(MachineAdvancedAssemblerBlockEntity be,
                                    MachineAdvancedAssemblerBakedModel model,
                                    float partialTick,
                                    PoseStack poseStack,
                                    int packedLight) {
        // Кольцо с вращением
        float ring = Mth.lerp(partialTick, be.prevRingAngle, be.ringAngle);
        Matrix4f ringMat = new Matrix4f().rotateY((float) Math.toRadians(ring));
        renderFallbackPart("Ring", model.getPart("Ring"), poseStack, packedLight, ringMat);
        
        // Руки с анимацией
        renderArmFallback(be.arms[0], false, model, partialTick, poseStack, packedLight, ringMat);
        renderArmFallback(be.arms[1], true, model, partialTick, poseStack, packedLight, ringMat);
    }

    /**
     *  НОВЫЙ МЕТОД: Рендер руки в fallback режиме
     */
    private void renderArmFallback(MachineAdvancedAssemblerBlockEntity.AssemblerArm arm,
                                boolean inverted,
                                MachineAdvancedAssemblerBakedModel model,
                                float partialTick,
                                PoseStack poseStack,
                                int packedLight,
                                Matrix4f baseTransform) {
        float a0 = Mth.lerp(partialTick, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(partialTick, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(partialTick, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(partialTick, arm.prevAngles[3], arm.angles[3]);
        
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -0.9375f : 0.9375f;
        
        String lowerName = inverted ? "ArmLower2" : "ArmLower1";
        String upperName = inverted ? "ArmUpper2" : "ArmUpper1";
        String headName = inverted ? "Head2" : "Head1";
        String spikeName = inverted ? "Spike2" : "Spike1";
        
        // Нижняя часть
        Matrix4f lowerMat = new Matrix4f(baseTransform)
            .translate(0, 1.625f, zBase)
            .rotateX((float) Math.toRadians(angleSign * a0))
            .translate(0, -1.625f, -zBase);
        renderFallbackPart(lowerName, model.getPart(lowerName), poseStack, packedLight, lowerMat);
        
        // Верхняя часть
        Matrix4f upperMat = new Matrix4f(lowerMat)
            .translate(0, 2.375f, zBase)
            .rotateX((float) Math.toRadians(angleSign * a1))
            .translate(0, -2.375f, -zBase);
        renderFallbackPart(upperName, model.getPart(upperName), poseStack, packedLight, upperMat);
        
        // Голова
        Matrix4f headMat = new Matrix4f(upperMat)
            .translate(0, 2.375f, zBase * 0.4667f)
            .rotateX((float) Math.toRadians(angleSign * a2))
            .translate(0, -2.375f, -zBase * 0.4667f);
        renderFallbackPart(headName, model.getPart(headName), poseStack, packedLight, headMat);
        
        // Шип
        Matrix4f spikeMat = new Matrix4f(headMat)
            .translate(0, a3, 0);
        renderFallbackPart(spikeName, model.getPart(spikeName), poseStack, packedLight, spikeMat);
    }

    @Override
    public void render(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                    PoseStack poseStack, MultiBufferSource bufferSource,
                    int packedLight, int packedOverlay) {
        // Рендерим машину (VBO)
        super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        
        //  Рендерим иконку ОТДЕЛЬНО с immediate buffer
        renderRecipeIconDirect(be, poseStack, packedLight, packedOverlay);
    }

    /**
     *  СУЩЕСТВУЮЩИЙ МЕТОД: VBO рендер (без изменений)
     */
    private void renderWithVBO(MachineAdvancedAssemblerBlockEntity be,
                            MachineAdvancedAssemblerBakedModel model,
                            float partialTick,
                            PoseStack poseStack,
                            int dynamicLight,
                            BlockPos blockPos) {
        // Существующий код VBO рендера
        if (!instancersInitialized) {
            initializeInstancedRenderers(model);
        }
        
        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAdvancedAssemblerVboRenderer(model);
        }
        
        boolean shouldUpdateAnimation = !shouldSkipAnimationUpdate(blockPos);
        
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
        
        if (shouldUpdateAnimation) {
            renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos);
        }
    }

    /**
     *  НОВОЕ: Проверка, нужно ли пропустить фрейм на основе дистанции
     */
    private boolean shouldSkipAnimationUpdate(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        
        // Вычисляем квадрат дистанции (избегаем sqrt для производительности)
        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        
        // Пороговое значение из конфига (в чанках -> блоки -> квадрат)
        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;
        double thresholdSquared = thresholdBlocks * thresholdBlocks;
        
        // Если дальше порога - отключаем анимацию
        return distanceSquared > thresholdSquared;
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

    private void renderRecipeIconDirect(MachineAdvancedAssemblerBlockEntity be,
                                    PoseStack poseStack,
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

        //  Используем СТАНДАРТНЫЙ vanilla buffers напрямую, БЕЗ immediate!
        // Создаём в самом конце цикла рендера, когда Embeddium уже закончил свою оптимизацию
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
        
        //  КРИТИЧНО: Используем ГЛОБАЛЬНЫЙ mc.renderBuffers().bufferSource()
        // Это гарантирует, что все батчи Embeddium УЖЕ завершены!
        MultiBufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        //  Привязываем TextureAtlas ПЕРЕД КАЖДЫМ ИСПОЛЬЗОВАНИЕМ
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
        
        //  Флашим СРАЗУ же после renderStatic
        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSrc) {
            bufferSrc.endBatch();
        }
        
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
        if (instancedBase != null) {
            instancedBase.cleanup();
            instancedBase = null;
        }
        if (instancedFrame != null) {
            instancedFrame.cleanup();
            instancedFrame = null;
        }
        instancersInitialized = false;
        
        //  Очищаем fallback кэш
        fallbackRenderers.clear();
        
        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
        gpu = null;
        cachedModel = null;
        cachedIconBuffer = null;
        
        RenderPathManager.reset();
    }
}
