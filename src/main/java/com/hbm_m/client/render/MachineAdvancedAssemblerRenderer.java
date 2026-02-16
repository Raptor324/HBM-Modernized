package com.hbm_m.client.render;

import org.joml.Matrix4f;

import com.hbm_m.block.custom.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.entity.custom.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.custom.machines.MachineAdvancedAssemblerBlockEntity.ClientTicker;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MachineAdvancedAssemblerRenderer extends AbstractPartBasedRenderer<MachineAdvancedAssemblerBlockEntity, MachineAdvancedAssemblerBakedModel> {

    private MachineAdvancedAssemblerVboRenderer gpu;
    private MachineAdvancedAssemblerBakedModel cachedModel;
    
    // Instanced рендереры для анимированных частей (Frame — в BlockState/BakedModel)
    private static volatile InstancedStaticPartRenderer instancedRing;
    private static volatile InstancedStaticPartRenderer instancedArmLower1;
    private static volatile InstancedStaticPartRenderer instancedArmUpper1;
    private static volatile InstancedStaticPartRenderer instancedHead1;
    private static volatile InstancedStaticPartRenderer instancedSpike1;
    private static volatile InstancedStaticPartRenderer instancedArmLower2;
    private static volatile InstancedStaticPartRenderer instancedArmUpper2;
    private static volatile InstancedStaticPartRenderer instancedHead2;
    private static volatile InstancedStaticPartRenderer instancedSpike2;
    private static volatile boolean instancersInitialized = false;


    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineAdvancedAssemblerBakedModel model) {
        if (instancersInitialized) return;
        
        try {
            MainRegistry.LOGGER.info("MachineAdvancedAssemblerRenderer: Initializing instanced renderers...");
            
            // Анимированные части (Frame — в BlockState/BakedModel)
            instancedRing = createInstancedForPart(model, "Ring");
            instancedArmLower1 = createInstancedForPart(model, "ArmLower1");
            instancedArmUpper1 = createInstancedForPart(model, "ArmUpper1");
            instancedHead1 = createInstancedForPart(model, "Head1");
            instancedSpike1 = createInstancedForPart(model, "Spike1");
            instancedArmLower2 = createInstancedForPart(model, "ArmLower2");
            instancedArmUpper2 = createInstancedForPart(model, "ArmUpper2");
            instancedHead2 = createInstancedForPart(model, "Head2");
            instancedSpike2 = createInstancedForPart(model, "Spike2");
            
            // Memory barrier: все записи видны после этого
            instancersInitialized = true;
            
            
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize instanced renderers", e);
            instancedRing = null;
            instancedArmLower1 = null;
            instancedArmUpper1 = null;
            instancedHead1 = null;
            instancedSpike1 = null;
            instancedArmLower2 = null;
            instancedArmUpper2 = null;
            instancedHead2 = null;
            instancedSpike2 = null;
        }
    }
    
    private static InstancedStaticPartRenderer createInstancedForPart(MachineAdvancedAssemblerBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        var data = ObjModelVboBuilder.buildSinglePart(part, partName);
        if (data == null) return null;
        var quads = GlobalMeshCache.getOrCompile("assembler_" + partName, part);
        return new InstancedStaticPartRenderer(data, quads);
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
        var state = be.getBlockState();
        boolean renderActive = state.hasProperty(MachineAdvancedAssemblerBlock.RENDER_ACTIVE) 
                && state.getValue(MachineAdvancedAssemblerBlock.RENDER_ACTIVE);
        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();

        // Шейдер активен + машина спит → всё в BakedModel, BER не рендерит.
        if (shaderActive && !renderActive) {
            return;
        }
                                
        BlockPos blockPos = be.getBlockPos();
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);
        
        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }
        
        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource);
        
        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSrc) {
            bufferSrc.endBatch();
        }
    }

    @Override
    public void render(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                    PoseStack poseStack, MultiBufferSource bufferSource,
                    int packedLight, int packedOverlay) {
        // Рендерим машину (VBO)
        super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        
        //  Рендерим иконку ОТДЕЛЬНО с immediate buffer
        renderRecipeIconDirect(be, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private void renderWithVBO(MachineAdvancedAssemblerBlockEntity be,
                            MachineAdvancedAssemblerBakedModel model,
                            float partialTick,
                            PoseStack poseStack,
                            int dynamicLight,
                            BlockPos blockPos,
                            MultiBufferSource bufferSource) {
        // Нет стороннего шейдера → полный VBO (инстансинг или прямой VBO).
        // Сторонний шейдер активен → статичная геометрия (Base/Frame в чанке) + putBulkData для анимированных частей.
        boolean useVboPath = !ShaderCompatibilityDetector.isExternalShaderActive();

        if (useVboPath && !instancersInitialized) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAdvancedAssemblerVboRenderer(model);
        }

        // Нет шейдера: Base и Frame рендерит BER (VBO). Шейдер активен: Base+Frame в BakedModel.
        if (useVboPath) {
            var blockState = be.getBlockState();
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            gpu.renderStaticBase(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
            if (blockState.hasProperty(MachineAdvancedAssemblerBlock.FRAME) && blockState.getValue(MachineAdvancedAssemblerBlock.FRAME)) {
                poseStack.pushPose();
                poseStack.translate(-0.5f, 0.0f, -0.5f);
                gpu.renderStaticFrame(poseStack, dynamicLight, blockPos, be, bufferSource);
                poseStack.popPose();
            }
        }

        if (!shouldSkipAnimationUpdate(blockPos)) {
            renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath);
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
     * ВАЖНО: Вызывать в конце рендера ВСЕХ машин для флаша батчей.
     * При useInstancedBatching использует матрицы из события.
     */
    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (instancedRing != null) instancedRing.flush(event);
        if (instancedArmLower1 != null) instancedArmLower1.flush(event);
        if (instancedArmUpper1 != null) instancedArmUpper1.flush(event);
        if (instancedHead1 != null) instancedHead1.flush(event);
        if (instancedSpike1 != null) instancedSpike1.flush(event);
        if (instancedArmLower2 != null) instancedArmLower2.flush(event);
        if (instancedArmUpper2 != null) instancedArmUpper2.flush(event);
        if (instancedHead2 != null) instancedHead2.flush(event);
        if (instancedSpike2 != null) instancedSpike2.flush(event);
    }

    /**
     * Очищает кэши instanced рендереров (вызывается при периодической очистке памяти)
     */
    public static void clearCaches() {
        cleanupInstanced(instancedRing); instancedRing = null;
        cleanupInstanced(instancedArmLower1); instancedArmLower1 = null;
        cleanupInstanced(instancedArmUpper1); instancedArmUpper1 = null;
        cleanupInstanced(instancedHead1); instancedHead1 = null;
        cleanupInstanced(instancedSpike1); instancedSpike1 = null;
        cleanupInstanced(instancedArmLower2); instancedArmLower2 = null;
        cleanupInstanced(instancedArmUpper2); instancedArmUpper2 = null;
        cleanupInstanced(instancedHead2); instancedHead2 = null;
        cleanupInstanced(instancedSpike2);         instancedSpike2 = null;
        instancersInitialized = false;
        MachineAdvancedAssemblerVboRenderer.clearGlobalCache();
    }

    private static void cleanupInstanced(InstancedStaticPartRenderer r) {
        if (r != null) r.cleanup();
    }

    private void renderAnimated(MachineAdvancedAssemblerBlockEntity be, float pt,
                                PoseStack pose, int blockLight, BlockPos blockPos,
                                MultiBufferSource bufferSource, boolean useVboPath) {
        float ring = Mth.lerp(pt, be.getPrevRingAngle(), be.getRingAngle());
        Matrix4f ringMat = new Matrix4f()
                .rotateY((float) Math.toRadians(ring))
                .translate(-0.5f, 0.0f, -0.5f);

        // Инстансинг только когда нет стороннего шейдера (VBO путь)
        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();
        if (useBatching && instancedRing != null && instancedRing.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(ringMat);
            instancedRing.addInstance(pose, blockLight, blockPos, be, bufferSource);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, "Ring", ringMat, blockPos, be, bufferSource);
        }

        ClientTicker.AssemblerArm[] arms = be.getArms();
        if (arms.length >= 2) {
            renderArm(arms[0], false, pt, pose, blockLight, ringMat, blockPos, be, bufferSource, useBatching);
            renderArm(arms[1], true, pt, pose, blockLight, ringMat, blockPos, be, bufferSource, useBatching);
        }
    }

    private void renderArm(ClientTicker.AssemblerArm arm, boolean inverted,
                           float pt, PoseStack pose, int blockLight, Matrix4f baseTransform,
                           BlockPos blockPos, MachineAdvancedAssemblerBlockEntity be,
                           MultiBufferSource bufferSource, boolean useInstanced) {
        if (arm == null) return;

        float a0 = Mth.lerp(pt, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(pt, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(pt, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(pt, arm.prevAngles[3], arm.angles[3]);
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -0.9375f : 0.9375f;

        Matrix4f lowerMat = new Matrix4f(baseTransform)
                .translate(0.5f, 1.625f, 0.5f + zBase)
                .rotateX((float) Math.toRadians(angleSign * a0))
                .translate(-0.5f, -1.625f, -(0.5f + zBase));

        addInstanceOrRender(useInstanced, inverted ? instancedArmLower2 : instancedArmLower1,
                pose, blockLight, blockPos, be, "ArmLower1", "ArmLower2", lowerMat, inverted, bufferSource);

        Matrix4f upperMat = new Matrix4f(lowerMat)
                .translate(0.5f, 2.375f, 0.5f + zBase)
                .rotateX((float) Math.toRadians(angleSign * a1))
                .translate(-0.5f, -2.375f, -(0.5f + zBase));

        addInstanceOrRender(useInstanced, inverted ? instancedArmUpper2 : instancedArmUpper1,
                pose, blockLight, blockPos, be, "ArmUpper1", "ArmUpper2", upperMat, inverted, bufferSource);

        Matrix4f headMat = new Matrix4f(upperMat)
                .translate(0.5f, 2.375f, 0.5f + (zBase * 0.4667f))
                .rotateX((float) Math.toRadians(angleSign * a2))
                .translate(-0.5f, -2.375f, -(0.5f + (zBase * 0.4667f)));

        addInstanceOrRender(useInstanced, inverted ? instancedHead2 : instancedHead1,
                pose, blockLight, blockPos, be, "Head1", "Head2", headMat, inverted, bufferSource);

        Matrix4f spikeMat = new Matrix4f(headMat)
                .translate(0, a3, 0);
        addInstanceOrRender(useInstanced, inverted ? instancedSpike2 : instancedSpike1,
                pose, blockLight, blockPos, be, "Spike1", "Spike2", spikeMat, inverted, bufferSource);
    }

    private void addInstanceOrRender(boolean useInstanced, InstancedStaticPartRenderer instanced,
            PoseStack pose, int blockLight, BlockPos blockPos, MachineAdvancedAssemblerBlockEntity be,
            String name1, String name2, Matrix4f transform, boolean inverted,
            MultiBufferSource bufferSource) {
        String partName = inverted ? name2 : name1;
        if (useInstanced && instanced != null && instanced.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(transform);
            instanced.addInstance(pose, blockLight, blockPos, be, bufferSource);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, partName, transform, blockPos, be, bufferSource);
        }
    }

    private void renderRecipeIconDirect(MachineAdvancedAssemblerBlockEntity be,
                                        PoseStack poseStack,
                                        MultiBufferSource bufferSource,
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

        // ВАЖНО: просто используем существующий bufferSource, не создаём новый и не вызываем endBatch()
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

    @Override 
    public boolean shouldRenderOffScreen(MachineAdvancedAssemblerBlockEntity be) { 
        return true; 
    }

    @Override public int getViewDistance() { return 128; }
    
    public void onResourceManagerReload() {
        clearCaches();
        gpu = null;
        cachedModel = null;
        MainRegistry.LOGGER.debug("Assembler renderer resources reloaded");
    }
}
