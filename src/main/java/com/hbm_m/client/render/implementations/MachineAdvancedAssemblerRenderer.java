package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity.ClientTicker;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.MultipartFacingTransforms;
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
    
    // Instanced рендереры
    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
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

    // --- GC Optimization: Reusable Matrices ---
    // Используем поля класса вместо создания новых объектов в каждом кадре
    private final Vector3f ringPivotWork = new Vector3f();
    private final Matrix4f matRing = new Matrix4f();
    private final Matrix4f matLower = new Matrix4f();
    private final Matrix4f matUpper = new Matrix4f();
    private final Matrix4f matHead = new Matrix4f();
    private final Matrix4f matSpike = new Matrix4f();

    /**
     * Multiplier turning degrees into radians as a single float multiply. Replaces
     * {@code (float) Math.toRadians(deg)} in the per-instance arm transform loop -
     * Math.toRadians is a {@code double} operation and forces a double→float cast
     * on every call site (4 calls per BE per frame for this assembler), so a
     * direct float multiply by a precomputed constant is both faster and avoids
     * that lossy conversion.
     */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /**
     * Смещение от клетки контроллера к центру 3×3 (нижний слой) в локальной сетке
     * мультиблока. Далее в {@link #setRingBaseMatrix} вектор переводится из мир. осей
     * (как в {@link MultiblockStructureHelper#getRotatedPos}) в ось PoseStack после
     * {@link LegacyAnimator#setupBlockTransform} через обратный к
     * {@link MultipartFacingTransforms#legacyBlockEntityBakedRotationY} поворот.
     */
    private static final BlockPos RING_PIVOT_LOCAL = new BlockPos(0, 0, 1);

    /**
     * Per-BE flag set inside {@link #renderParts} after the occlusion-culling
     * check passes; consumed by {@link #render} before drawing the recipe icon.
     * Renderer instances are singletons shared across every BE of this type, so
     * the field is read-modify-written by the render thread only - safe.
     * Without this gate, {@code renderRecipeIconDirect} fired on every visible
     * controller chunk regardless of culling, paying a full
     * {@code ItemRenderer.renderStatic} for invisible machines.
     */
    private boolean visibleThisFrame = false;

    public MachineAdvancedAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineAdvancedAssemblerBakedModel model) {
        if (instancersInitialized) return;
        
        try {
            MainRegistry.LOGGER.info("MachineAdvancedAssemblerRenderer: Initializing instanced renderers...");

            instancedBase = createInstancedForPart(model, "Base");
            instancedFrame = createInstancedForPart(model, "Frame");
            
            // Анимированные части (Frame - в BlockState/BakedModel)
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
            // Сброс при ошибке, чтобы попытаться снова или не крашить
            clearCaches();
        }
    }
    
    private static InstancedStaticPartRenderer createInstancedForPart(MachineAdvancedAssemblerBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        String cacheKey = "assembler_" + partName;
        PartGeometry geo = GlobalMeshCache.getOrCompilePartGeometry(cacheKey, part);
        if (geo.isEmpty()) return null;
        var data = geo.toVboData(partName);
        if (data == null) return null;
        return new InstancedStaticPartRenderer(data, geo.solidQuads());
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

    /** Матрица кольца и базы рук: вращение вокруг геом. центра 3×3, не вокруг клетки контроллера. */
    private void setRingBaseMatrix(float ringAngleDeg, Direction facing) {
        BlockPos w = MultiblockStructureHelper.rotate(RING_PIVOT_LOCAL, facing);
        ringPivotWork.set(w.getX(), 0, w.getZ());
        // Мир. смещение (к multiblock) → лок. после 90°+facing в setupBlockTransform
        int berYDeg = MultipartFacingTransforms.legacyBlockEntityBakedRotationY(facing);
        ringPivotWork.rotateY(-berYDeg * DEG_TO_RAD);
        float px = ringPivotWork.x;
        float pz = ringPivotWork.z;
        matRing.identity()
            .translate(px, 0, pz)
            .rotateY(ringAngleDeg * DEG_TO_RAD)
            .translate(-px, 0, -pz)
            .translate(-0.5f, 0, -0.5f);
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
        boolean useVboGeometry = ShaderCompatibilityDetector.useVboGeometry();

        BlockPos blockPos = be.getBlockPos();
        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }
        // Mark visible so render() knows it's safe to draw the recipe icon.
        // visibleThisFrame is reset to false at the top of render() before
        // super.render() runs, so this only stays true when culling passes.
        visibleThisFrame = true;

        // Старый baked-путь под шейдерами + машина спит → всё в BakedModel, BER не рендерит.
        // Под новым VBO путём (useVboGeometry==true) baked пуст и нам нужно рендерить статику самим.
        if (!useVboGeometry && !renderActive) {
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource);
    }

    @Override
    public void render(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                    PoseStack poseStack, MultiBufferSource bufferSource,
                    int packedLight, int packedOverlay) {
        // Pessimistic default - super.render() may early-out (frustum) without
        // ever invoking renderParts(), and renderParts() may early-out via the
        // OcclusionCullingHelper check before flipping the flag. In either case
        // the icon stays unrendered, saving a full ItemRenderer.renderStatic
        // per offscreen / occluded machine.
        
        com.hbm_m.client.render.LightSampleCache.BASE_POSE.get().set(poseStack.last().pose());
        com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(true);

        try {
            // Pessimistic default - super.render() may early-out
            visibleThisFrame = false;
            super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            if (visibleThisFrame) {
                renderRecipeIconDirect(be, poseStack, bufferSource, packedLight, packedOverlay);
            }
        } finally {
            // Обязательно очищаем после рендера машины
            com.hbm_m.client.render.LightSampleCache.BASE_POSE_SET.set(false);
        }
    }

    private void renderWithVBO(MachineAdvancedAssemblerBlockEntity be,
                            MachineAdvancedAssemblerBakedModel model,
                            float partialTick,
                            PoseStack poseStack,
                            int dynamicLight,
                            BlockPos blockPos,
                            MultiBufferSource bufferSource) {
        boolean useVboPath = ShaderCompatibilityDetector.useVboGeometry();

        if (useVboPath && !instancersInitialized) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAdvancedAssemblerVboRenderer(model);
        }

        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();

        // Open an IrisRenderBatch session for the duration of this BlockEntity's
        // part draws when:
        //   1) shader pack is active AND we are routing through the new Iris
        //      VBO path AND per-part-type instancing is OFF - all parts share
        //      a single apply()/clear() pair (3–6× FPS improvement under BSL).
        //   2) shader pack is active AND we are routing through the new Iris
        //      VBO path AND we are in a shadow pass - even with per-part-type
        //      instancing ON, instances added during the shadow pass cannot be
        //      flushed by RenderLevelStageEvent.AFTER_BLOCK_ENTITIES (that
        //      stage fires only for the main pass). InstancedStaticPartRenderer
        //      .addInstance() detects the shadow pass and immediately delegates
        //      to drawSingleWithIrisExtended; opening a batch here lets all 9
        //      redirected single draws share one apply()/clear() pair, restoring
        //      the same amortisation we get on the main pass via instancing.
        //      Without this, machines either fail to cast shadows OR duplicate
        //      themselves "in the sky" at shadow-camera coordinates.
        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean useIrisBatch = ShaderCompatibilityDetector.useNewIrisVboPath() && (!useBatching || shadowPass);
        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                // batch == null means Iris couldn't hand out a usable shader; fall
                // through to the standalone per-call path which will pick up the
                // correct fallback (vanilla shader / putBulkData delegation).
                renderPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching);
            }
        } else {
            renderPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching);
        }
    }

    private void renderPartsInternal(MachineAdvancedAssemblerBlockEntity be,
                                     MachineAdvancedAssemblerBakedModel model,
                                     float partialTick,
                                     PoseStack poseStack,
                                     int dynamicLight,
                                     BlockPos blockPos,
                                     MultiBufferSource bufferSource,
                                     boolean useVboPath,
                                     boolean useBatching) {
        var blockState = be.getBlockState();

        // 1. Рендер статики (Base + Frame) - только когда НЕТ шейдера (useVboPath).
        // При активном шейдере Base и Frame рендерит BakedModel; BER рисует только подвижные части.
        if (useVboPath) {
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f); // Центровка модели

            // Base
            if (useBatching && instancedBase != null && instancedBase.isInitialized()) {
                poseStack.pushPose();
                instancedBase.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
                poseStack.popPose();
            } else {
                gpu.renderStaticBase(poseStack, dynamicLight, blockPos, be, bufferSource);
            }

            // Frame
            if (blockState.hasProperty(MachineAdvancedAssemblerBlock.FRAME) && blockState.getValue(MachineAdvancedAssemblerBlock.FRAME)) {
                if (useBatching && instancedFrame != null && instancedFrame.isInitialized()) {
                    poseStack.pushPose();
                    instancedFrame.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
                    poseStack.popPose();
                } else {
                    gpu.renderStaticFrame(poseStack, dynamicLight, blockPos, be, bufferSource);
                }
            }
            poseStack.popPose();
        }

        // 2. Рендер анимаций (подвижные части - всегда через BER)
        // Если игрок далеко - пропускаем вычисления анимаций, но рисуем детали в дефолтной позе (или не рисуем, если так задумано для LOD)
        boolean skipAnimation = shouldSkipAnimationUpdate(blockPos);
        
        // Если скипаем анимацию, ставим partialTick = 0 (дефолтная поза) или просто рендерим без lerp
        // В данном случае, чтобы детали не исчезали, мы рендерим их, но не обновляем углы (или берем статические 0)
        // Для оптимизации: если очень далеко, можно вообще не рисовать мелкие детали (arms).
        if (!skipAnimation) {
            renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useBatching);
        } else {
            // LOD: Рисуем только кольцо статично, руки не рисуем (они мелкие)
            // Это сэкономит GPU на дальних дистанциях
            renderStaticLOD(poseStack, dynamicLight, blockPos, be, bufferSource, useBatching);
        }
    }

    // Упрощенный рендер для дальних дистанций
    private void renderStaticLOD(PoseStack pose, int blockLight, BlockPos blockPos, 
                                MachineAdvancedAssemblerBlockEntity be, MultiBufferSource bufferSource, boolean useBatching) {
        setRingBaseMatrix(0f, getFacing(be));
        if (useBatching && instancedRing != null) {
            pose.pushPose();
            pose.last().pose().mul(matRing);
            instancedRing.addInstance(pose, blockLight, blockPos, be, bufferSource);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, "Ring", matRing, blockPos, be, bufferSource);
        }
    }

    /**
     *  Проверка, нужно ли пропустить фрейм на основе дистанции
     */
    private boolean shouldSkipAnimationUpdate(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        
        // Вычисляем квадрат дистанции
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
        flushInstanced(event, instancedBase);
        flushInstanced(event, instancedFrame);
        flushInstanced(event, instancedRing);
        flushInstanced(event, instancedArmLower1);
        flushInstanced(event, instancedArmUpper1);
        flushInstanced(event, instancedHead1);
        flushInstanced(event, instancedSpike1);
        flushInstanced(event, instancedArmLower2);
        flushInstanced(event, instancedArmUpper2);
        flushInstanced(event, instancedHead2);
        flushInstanced(event, instancedSpike2);
    }

    /**
     * Очищает кэши instanced рендереров (вызывается при периодической очистке памяти)
     */
    public static void clearCaches() {
        cleanupInstanced(instancedBase); instancedBase = null;
        cleanupInstanced(instancedFrame); instancedFrame = null;
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
    }

    private static void cleanupInstanced(InstancedStaticPartRenderer r) {
        if (r != null) r.cleanup();
    }

    private static void flushInstanced(net.minecraftforge.client.event.RenderLevelStageEvent event,
                                       InstancedStaticPartRenderer r) {
        if (r != null) r.flush(event);
    }

    private void renderAnimated(MachineAdvancedAssemblerBlockEntity be, float pt,
                                PoseStack pose, int blockLight, BlockPos blockPos,
                                MultiBufferSource bufferSource, boolean useVboPath) {
        float ring = Mth.lerp(pt, be.getPrevRingAngle(), be.getRingAngle());
        setRingBaseMatrix(ring, getFacing(be));

        // Инстансинг только когда нет стороннего шейдера (VBO путь)
        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();
        if (useBatching && instancedRing != null && instancedRing.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(matRing);
            instancedRing.addInstance(pose, blockLight, blockPos, be, bufferSource);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, "Ring", matRing, blockPos, be, bufferSource);
        }

        ClientTicker.AssemblerArm[] arms = be.getArms();
        if (arms.length >= 2) {
            renderArm(arms[0], false, pt, pose, blockLight, matRing, blockPos, be, bufferSource, useBatching);
            renderArm(arms[1], true, pt, pose, blockLight, matRing, blockPos, be, bufferSource, useBatching);
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

        matLower.set(baseTransform)
                .translate(0.5f, 1.625f, 0.5f + zBase)
                .rotateX(angleSign * a0 * DEG_TO_RAD)
                .translate(-0.5f, -1.625f, -(0.5f + zBase));

        addInstanceOrRender(useInstanced, inverted ? instancedArmLower2 : instancedArmLower1,
                pose, blockLight, blockPos, be, "ArmLower1", "ArmLower2", matLower, inverted, bufferSource);

        matUpper.set(matLower)
                .translate(0.5f, 2.375f, 0.5f + zBase)
                .rotateX(angleSign * a1 * DEG_TO_RAD)
                .translate(-0.5f, -2.375f, -(0.5f + zBase));

        addInstanceOrRender(useInstanced, inverted ? instancedArmUpper2 : instancedArmUpper1,
                pose, blockLight, blockPos, be, "ArmUpper1", "ArmUpper2", matUpper, inverted, bufferSource);

        matHead.set(matUpper)
                .translate(0.5f, 2.375f, 0.5f + (zBase * 0.4667f))
                .rotateX(angleSign * a2 * DEG_TO_RAD)
                .translate(-0.5f, -2.375f, -(0.5f + (zBase * 0.4667f)));

        addInstanceOrRender(useInstanced, inverted ? instancedHead2 : instancedHead1,
                pose, blockLight, blockPos, be, "Head1", "Head2", matHead, inverted, bufferSource);

        matSpike.set(matHead)
                .translate(0, a3, 0);
        addInstanceOrRender(useInstanced, inverted ? instancedSpike2 : instancedSpike1,
                pose, blockLight, blockPos, be, "Spike1", "Spike2", matSpike, inverted, bufferSource);
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

        if (shouldSkipAnimationUpdate(be.getBlockPos())) return;

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

        BlockPos toCenter = MultiblockStructureHelper.rotate(RING_PIVOT_LOCAL, getFacing(be));

        poseStack.pushPose();
        poseStack.translate(toCenter.getX(), 0, toCenter.getZ());
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
        return !ShaderCompatibilityDetector.isRenderingShadowPass();
    }

    @Override public int getViewDistance() { return 128; }
}
