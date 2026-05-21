package com.hbm_m.client.render.implementations;


//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.client.machine.AdvancedAssemblerClientTicker;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.ClientRenderFlags;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
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
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class MachineAdvancedAssemblerRenderer extends AbstractPartBasedRenderer<MachineAdvancedAssemblerBlockEntity, MachineAdvancedAssemblerBakedModel> {

    private MachineAdvancedAssemblerVboRenderer gpu;
    private MachineAdvancedAssemblerBakedModel cachedModel;
    
    // Instanced рендереры: два merged static VBO (Base / Base+Frame) + анимированные части
    private static volatile InstancedStaticPartRenderer instancedStaticClusterBase;
    private static volatile InstancedStaticPartRenderer instancedStaticClusterBaseFrame;
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
     * Снимок анимации на один кадр для текущей машины: один раз читаем BE/тикер,
     * считаем кольцо и копируем {@link #matRing} в {@link #animMatRing}.
     */
    private final Matrix4f animMatRing = new Matrix4f();
    @Nullable
    private AdvancedAssemblerClientTicker.AssemblerArm[] animArmsSnapshot;

    /** Immutable quad lists для merged static mesh (vanilla VBO fallback). */
    private static volatile List<BakedQuad> staticClusterBaseQuads = List.of();
    private static volatile List<BakedQuad> staticClusterBaseFrameQuads = List.of();

    /**
     * Multiplier turning degrees into radians as a single float multiply. Replaces
     * {@code (float) Math.toRadians(deg)} in the per-instance arm transform loop -
     * Math.toRadians is a {@code double} operation and forces a double→float cast
     * on every call site (4 calls per BE per frame for this assembler), so a
     * direct float multiply by a precomputed constant is both faster and avoids
     * that lossy conversion.
     */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /** Stable cache key for one {@link LightSampleCache#getOrSample8} per machine per frame. */
    private static final long MACHINE_LIGHT_SAMPLE_KEY = 0x48534D5F41445641L;

    private final Matrix4f tmpMachineLightPose = new Matrix4f();
    private final float[] machineSharedLight8 = new float[16];
    private final float[] machineLightBbox = new float[6];

    /**
     * Смещение от клетки контроллера к центру 3×3 (нижний слой) в локальной сетке
     * мультиблока. Далее в {@link #setRingBaseMatrix} вектор переводится из мир. осей
     * (как в {@link MultiblockStructureHelper#getRotatedPos}) в ось PoseStack после
     * {@link LegacyAnimator#setupBlockTransform} через обратный к
     * {@link MultipartFacingTransforms#legacyBlockEntityBakedRotationY} поворот.
     */
    private static final BlockPos RING_PIVOT_LOCAL = new BlockPos(0, 0, 1);

    private static final float ARM_PIVOT_Y_LOWER = 1.625f;
    private static final float ARM_PIVOT_Y_UPPER = 2.375f;
    private static final float ARM_Z_OFFSET = 0.9375f;
    private static final float ARM_HEAD_Z_SCALE = 0.4667f;
    private static final float RECIPE_ICON_MAX_DIST_SQ = 64.0f * 64.0f;

    /** Cached between {@link #renderParts} and {@link #renderPartsInternal} (render thread only). */
    @Nullable
    private Direction cachedFacing;
    @Nullable
    private AABB cachedRenderBounds;

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

            PartGeometry geoBase = MeshRenderCache.getOrCompilePartGeometry("assembler_Base", model.getPart("Base"));
            if (geoBase.isEmpty()) {
                MainRegistry.LOGGER.error("MachineAdvancedAssemblerRenderer: Base geometry empty");
                clearCaches();
                return;
            }
            List<BakedQuad> baseQuads = geoBase.solidQuads();
            staticClusterBaseQuads = baseQuads;

            var merged = new ArrayList<BakedQuad>(baseQuads);
            BakedModel framePart = model.getPart("Frame");
            if (framePart != null) {
                PartGeometry geoFrame = MeshRenderCache.getOrCompilePartGeometry("assembler_Frame", framePart);
                if (!geoFrame.isEmpty()) {
                    merged.addAll(geoFrame.solidQuads());
                }
            }
            staticClusterBaseFrameQuads = List.copyOf(merged);

            instancedStaticClusterBase = createInstancedFromQuads(staticClusterBaseQuads, "staticCluster_base");
            if (merged.size() > baseQuads.size()) {
                instancedStaticClusterBaseFrame = createInstancedFromQuads(staticClusterBaseFrameQuads, "staticCluster_base_frame");
            } else {
                instancedStaticClusterBaseFrame = instancedStaticClusterBase;
            }

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
    
    /** bone_id в VBO: 0 base/ring, 1 lower, 2 upper, 3 head, 4 spike — см. OLD/render.md */
    private static int assemblerPartBoneId(String partName) {
        if (partName.startsWith("ArmLower")) return 1;
        if (partName.startsWith("ArmUpper")) return 2;
        if (partName.startsWith("Head")) return 3;
        if (partName.startsWith("Spike")) return 4;
        return 0;
    }

    private static InstancedStaticPartRenderer createInstancedForPart(MachineAdvancedAssemblerBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        String cacheKey = "assembler_" + partName;
        PartGeometry geo = MeshRenderCache.getOrCompilePartGeometry(cacheKey, part);
        if (geo.isEmpty()) return null;
        int boneId = assemblerPartBoneId(partName);
        var data = geo.toVboData(partName, boneId);
        if (data == null) return null;
        boolean gpuSkin = boneId >= 1 && boneId <= 4;
        InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, geo.solidQuads(), false, gpuSkin);
        r.setMdiTraceTag("AdvAssembler/" + partName);
        return r;
    }

    private static InstancedStaticPartRenderer createInstancedFromQuads(List<BakedQuad> quads, String vboLabel) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }
        var data = PartGeometry.buildVboDataFromQuads(quads, vboLabel);
        if (data == null) {
            return null;
        }
        InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, quads);
        r.setMdiTraceTag("AdvAssembler/" + vboLabel);
        return r;
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

        Direction facing = getFacing(be);
        cachedFacing = facing;
        var minecraft = Minecraft.getInstance();
        BlockPos blockPos = be.getBlockPos();

        AABB renderBounds;
        if (state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController controller && controller.getStructureHelper() != null) {
            renderBounds = controller.getStructureHelper().getRenderBoundingBox(blockPos, facing, 0.0);
        } else {
            renderBounds = be.getRenderBoundingBox();
        }
        cachedRenderBounds = renderBounds;

        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            cachedFacing = null;
            cachedRenderBounds = null;
            return;
        }
        // Mark visible so render() knows it's safe to draw the recipe icon.
        // visibleThisFrame is reset to false at the top of render() before
        // super.render() runs, so this only stays true when culling passes.
        visibleThisFrame = true;

        if (!useVboGeometry && !renderActive) {
            return;
        }

        float staticFade = RenderDistanceHelper.computeStaticFade(blockPos);
        if (staticFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(staticFade);

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource);
    }

    @Override
    public void render(MachineAdvancedAssemblerBlockEntity be, float partialTick,
                    PoseStack poseStack, MultiBufferSource bufferSource,
                    int packedLight, int packedOverlay) {
        visibleThisFrame = false;
        super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (visibleThisFrame) {
            renderRecipeIconDirect(be, poseStack, bufferSource, packedLight, packedOverlay);
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

        boolean useBatching = useVboPath && ClientRenderFlags.useInstancedBatching();

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
        //? if forge {
        boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive() && (!useBatching || shadowPass);
        //?}
        //? if fabric {
        /*boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive();
        *///?}
        try {
            if (useIrisBatch) {
                try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                    // batch == null means Iris couldn't hand out a usable shader; fall
                    // through to the standalone per-call path which will pick up the
                    // correct fallback (vanilla shader / putBulkData delegation).
                    renderPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource,
                            useVboPath, useBatching);
                }
            } else {
                renderPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource,
                        useVboPath, useBatching);
            }
        } finally {
            cachedFacing = null;
            cachedRenderBounds = null;
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

        float staticFade = SingleMeshVboRenderer.getFadeAlpha();
        float animFade = RenderDistanceHelper.computeAnimatedFade(blockPos);

        boolean anyFading = staticFade < 0.99f || (animFade >= 0 && animFade < 0.99f);
        boolean effectiveBatching = useBatching && !anyFading;
        boolean useGpuBones = effectiveBatching && ClientRenderFlags.gpuBoneSkinning()
                && !ShaderCompatibilityDetector.isExternalShaderActive();

        float[] sharedLight = null;
        if (useVboPath && effectiveBatching) {
            AABB renderBounds = cachedRenderBounds;
            if (renderBounds == null) {
                Direction facing = cachedFacing != null ? cachedFacing : getFacing(be);
                if (blockState.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController controller
                        && controller.getStructureHelper() != null) {
                    renderBounds = controller.getStructureHelper().getRenderBoundingBox(blockPos, facing, 0.0);
                } else {
                    renderBounds = be.getRenderBoundingBox();
                }
            }
            worldBoundsToBlockLocal(renderBounds, blockPos, machineLightBbox);
            tmpMachineLightPose.identity();
            LightSampleCache.getOrSample8Lod(be, MACHINE_LIGHT_SAMPLE_KEY, machineLightBbox, blockPos,
                    tmpMachineLightPose, dynamicLight, machineSharedLight8,
                    RenderDistanceHelper.distanceSqToCamera(blockPos));
            sharedLight = machineSharedLight8;
        }

        // 1. Static parts: один merged VBO (только Base или Base+Frame по BlockState).
        if (useVboPath) {
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f);

            boolean frameVisible = blockState.hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                    && blockState.getValue(MachineAdvancedAssemblerBlock.FRAME);
            InstancedStaticPartRenderer staticCluster = frameVisible ? instancedStaticClusterBaseFrame : instancedStaticClusterBase;
            List<BakedQuad> staticQuads = frameVisible ? staticClusterBaseFrameQuads : staticClusterBaseQuads;

            if (effectiveBatching && staticCluster != null && staticCluster.isInitialized()) {
                poseStack.pushPose();
                staticCluster.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource, sharedLight);
                poseStack.popPose();
            } else if (!staticQuads.isEmpty()) {
                String cacheKey = frameVisible
                        ? MachineAdvancedAssemblerVboRenderer.STATIC_CLUSTER_CACHE_BASE_FRAME
                        : MachineAdvancedAssemblerVboRenderer.STATIC_CLUSTER_CACHE_BASE;
                gpu.renderStaticCluster(poseStack, dynamicLight, blockPos, be, bufferSource, staticQuads, cacheKey);
            }
            poseStack.popPose();
        }

        // 2. Animated parts: fade out at modelUpdateDistance.
        if (animFade < 0) return;
        SingleMeshVboRenderer.setFadeAlpha(Math.min(staticFade, animFade));
        prepareAssemblerAnimation(be, partialTick);
        renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource,
                effectiveBatching, useGpuBones, sharedLight);
        SingleMeshVboRenderer.setFadeAlpha(staticFade);
    }

    private static void worldBoundsToBlockLocal(AABB world, BlockPos origin, float[] out) {
        out[0] = (float) (world.minX - origin.getX());
        out[1] = (float) (world.minY - origin.getY());
        out[2] = (float) (world.minZ - origin.getZ());
        out[3] = (float) (world.maxX - origin.getX());
        out[4] = (float) (world.maxY - origin.getY());
        out[5] = (float) (world.maxZ - origin.getZ());
    }

    /**
     * ВАЖНО: Вызывать в конце рендера ВСЕХ машин для флаша батчей.
     * При useInstancedBatching использует матрицы из события.
     */
    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        flushInstanced(projectionMatrix, instancedStaticClusterBase);
        if (instancedStaticClusterBaseFrame != instancedStaticClusterBase) {
            flushInstanced(projectionMatrix, instancedStaticClusterBaseFrame);
        }
        flushInstanced(projectionMatrix, instancedRing);
        flushInstanced(projectionMatrix, instancedArmLower1);
        flushInstanced(projectionMatrix, instancedArmUpper1);
        flushInstanced(projectionMatrix, instancedHead1);
        flushInstanced(projectionMatrix, instancedSpike1);
        flushInstanced(projectionMatrix, instancedArmLower2);
        flushInstanced(projectionMatrix, instancedArmUpper2);
        flushInstanced(projectionMatrix, instancedHead2);
        flushInstanced(projectionMatrix, instancedSpike2);
    }

    /**
     * Очищает кэши instanced рендереров (вызывается при периодической очистке памяти)
     */
    public static void clearCaches() {
        staticClusterBaseQuads = List.of();
        staticClusterBaseFrameQuads = List.of();
        InstancedStaticPartRenderer bf = instancedStaticClusterBaseFrame;
        InstancedStaticPartRenderer b = instancedStaticClusterBase;
        cleanupInstanced(b);
        if (bf != b) {
            cleanupInstanced(bf);
        }
        instancedStaticClusterBase = null;
        instancedStaticClusterBaseFrame = null;
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

    private static void flushInstanced(org.joml.Matrix4f projectionMatrix,
                                       InstancedStaticPartRenderer r) {
        if (r != null) r.flush(projectionMatrix);
    }

    /** Один проход чтения углов/рук с BE и матрицы кольца на кадр для текущей машины. */
    private void prepareAssemblerAnimation(MachineAdvancedAssemblerBlockEntity be, float partialTick) {
        float ringLerped = Mth.lerp(partialTick, be.getPrevRingAngle(), be.getRingAngle());
        Direction facing = cachedFacing != null ? cachedFacing : getFacing(be);
        setRingBaseMatrix(ringLerped, facing);
        animMatRing.set(matRing);
        animArmsSnapshot = unpackArms(be.getArms());
    }

    @Nullable
    private static AdvancedAssemblerClientTicker.AssemblerArm[] unpackArms(Object arms) {
        if (arms instanceof AdvancedAssemblerClientTicker.AssemblerArm[] a) {
            return a;
        }
        return null;
    }

    private void renderAnimated(MachineAdvancedAssemblerBlockEntity be, float pt,
                                PoseStack pose, int blockLight, BlockPos blockPos,
                                MultiBufferSource bufferSource, boolean useBatching,
                                boolean useGpuBones, @Nullable float[] sharedLight) {
        if (useBatching && instancedRing != null && instancedRing.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(animMatRing);
            instancedRing.addInstance(pose, blockLight, blockPos, be, bufferSource, sharedLight);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, "Ring", animMatRing, blockPos, be, bufferSource);
        }

        AdvancedAssemblerClientTicker.AssemblerArm[] arms = animArmsSnapshot;
        if (arms != null && arms.length >= 2) {
            renderArm(arms[0], false, pt, pose, blockLight, animMatRing, blockPos, be, bufferSource,
                    useBatching, useGpuBones, sharedLight);
            renderArm(arms[1], true, pt, pose, blockLight, animMatRing, blockPos, be, bufferSource,
                    useBatching, useGpuBones, sharedLight);
        }
    }

    private void renderArm(AdvancedAssemblerClientTicker.AssemblerArm arm, boolean inverted,
                           float pt, PoseStack pose, int blockLight, Matrix4f baseTransform,
                           BlockPos blockPos, MachineAdvancedAssemblerBlockEntity be,
                           MultiBufferSource bufferSource, boolean useInstanced, boolean useGpuBones,
                           @Nullable float[] sharedLight) {
        if (arm == null) return;

        // Матрицы костей считаем на CPU (цепочка translate/rotateX). При батчинге без Iris
        // vanilla-instanced путь: {@code addInstanceGpuBones} (матрица части на CPU), см. {@link InstancedStaticPartRenderer}.
        // С активным shader pack (Iris) остаётся CPU-слияние pose*transform — кастомный VS там не наш.
        float a0 = Mth.lerp(pt, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(pt, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(pt, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(pt, arm.prevAngles[3], arm.angles[3]);
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -ARM_Z_OFFSET : ARM_Z_OFFSET;
        float headZ = zBase * ARM_HEAD_Z_SCALE;

        matLower.set(baseTransform)
                .translate(0.5f, ARM_PIVOT_Y_LOWER, 0.5f + zBase)
                .rotateX(angleSign * a0 * DEG_TO_RAD)
                .translate(-0.5f, -ARM_PIVOT_Y_LOWER, -(0.5f + zBase));

        addInstanceOrRender(useInstanced, useGpuBones, inverted ? instancedArmLower2 : instancedArmLower1,
                pose, blockLight, blockPos, be, "ArmLower1", "ArmLower2", matLower, inverted, bufferSource, sharedLight);

        matUpper.set(matLower)
                .translate(0.5f, ARM_PIVOT_Y_UPPER, 0.5f + zBase)
                .rotateX(angleSign * a1 * DEG_TO_RAD)
                .translate(-0.5f, -ARM_PIVOT_Y_UPPER, -(0.5f + zBase));

        addInstanceOrRender(useInstanced, useGpuBones, inverted ? instancedArmUpper2 : instancedArmUpper1,
                pose, blockLight, blockPos, be, "ArmUpper1", "ArmUpper2", matUpper, inverted, bufferSource, sharedLight);

        matHead.set(matUpper)
                .translate(0.5f, ARM_PIVOT_Y_UPPER, 0.5f + headZ)
                .rotateX(angleSign * a2 * DEG_TO_RAD)
                .translate(-0.5f, -ARM_PIVOT_Y_UPPER, -(0.5f + headZ));

        addInstanceOrRender(useInstanced, useGpuBones, inverted ? instancedHead2 : instancedHead1,
                pose, blockLight, blockPos, be, "Head1", "Head2", matHead, inverted, bufferSource, sharedLight);

        matSpike.set(matHead)
                .translate(0, a3, 0);
        addInstanceOrRender(useInstanced, useGpuBones, inverted ? instancedSpike2 : instancedSpike1,
                pose, blockLight, blockPos, be, "Spike1", "Spike2", matSpike, inverted, bufferSource, sharedLight);
    }

    private void addInstanceOrRender(boolean useInstanced, boolean useGpuBones,
            InstancedStaticPartRenderer instanced, PoseStack pose, int blockLight, BlockPos blockPos,
            MachineAdvancedAssemblerBlockEntity be, String name1, String name2, Matrix4f transform,
            boolean inverted, MultiBufferSource bufferSource, @Nullable float[] sharedLight) {
        String partName = inverted ? name2 : name1;
        boolean gpuBones = useGpuBones && useInstanced && instanced != null && instanced.isInitialized()
                && instanced.usesGpuPartBonePath();
        if (gpuBones) {
            instanced.addInstanceGpuBones(pose, transform, blockLight, blockPos, be, bufferSource, sharedLight);
            return;
        }
        if (useInstanced && instanced != null && instanced.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(transform);
            instanced.addInstance(pose, blockLight, blockPos, be, bufferSource, sharedLight);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, partName, transform, blockPos, be, bufferSource);
        }
    }

    private void renderRecipeIconDirect(MachineAdvancedAssemblerBlockEntity be,
                                        PoseStack poseStack,
                                        MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        BlockPos blockPos = be.getBlockPos();
        if (RenderDistanceHelper.computeAnimatedFade(blockPos) < 0) return;
        if (RenderDistanceHelper.distanceSqToCamera(blockPos) > RECIPE_ICON_MAX_DIST_SQ) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack icon = be.getClientRecipeIcon();
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
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    @Override public int getViewDistance() { return RenderDistanceHelper.getStaticViewDistanceBlocks(); }
}

