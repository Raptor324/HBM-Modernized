package com.hbm_m.client.render.implementations;


import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineAssemblerBlock;
import com.hbm_m.blockentity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.client.model.MachineAssemblerBakedModel;
import com.hbm_m.client.model.ModelHelper;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.ObjModelVboBuilder;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.client.render.shader.IrisRenderBatch;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineAssemblerRenderer extends AbstractPartBasedRenderer<MachineAssemblerBlockEntity, MachineAssemblerBakedModel> {

    private MachineAssemblerVboRenderer gpu;
    private MachineAssemblerBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedBody;
    private static volatile InstancedStaticPartRenderer instancedSlider;
    private static volatile InstancedStaticPartRenderer instancedArm;
    private static volatile InstancedStaticPartRenderer instancedCog;
    /** Body + Slider + Arm + 4×Cog в idle-позе, один VBO / один instanced draw. */
    private static volatile InstancedStaticPartRenderer instancedIdleCombined;
    private static volatile boolean instancersInitialized = false;

    private static final Matrix4f TMP_BAKE = new Matrix4f();
    private static final Matrix4f TMP_INNER_BODY = new Matrix4f();
    private static final Matrix4f TMP_INV_INNER_BODY = new Matrix4f();

    private final Matrix4f matSlider = new Matrix4f();
    private final Matrix4f matArm = new Matrix4f();
    private final Matrix4f matCog = new Matrix4f();

    /** Degrees → radians multiplier; see {@code MachineAdvancedAssemblerRenderer.DEG_TO_RAD}. */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final long MACHINE_LIGHT_SAMPLE_KEY = 0x48534D5F41534D42L;

    private final Matrix4f tmpMachineLightPose = new Matrix4f();
    private final float[] machineSharedLight8 = new float[16];
    private final float[] machineLightBbox = new float[6];

    /**
     * Per-BE flag set inside {@link #renderParts} after the occlusion-culling
     * check passes; consumed by {@link #render} before drawing the recipe icon.
     * Renderer instances are singletons shared across every BE of this type, so
     * the field is read-modify-written by the render thread only - safe.
     * Without this gate, {@code renderRecipeIconDirect} fired on every visible
     * controller chunk regardless of culling, paying a full
     * {@code ItemRenderer.renderStatic} for invisible machines (one of the
     * biggest QoL CPU pigs at 400-machine farm scale).
     */
    private boolean visibleThisFrame = false;

    public MachineAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineAssemblerBakedModel model) {
        if (instancersInitialized) return;

        try {
            MainRegistry.LOGGER.info("MachineAssemblerRenderer: Initializing instanced renderers...");
            instancedBody = createInstancedForPart(model, "Body");
            instancedSlider = createInstancedForPart(model, "Slider");
            instancedArm = createInstancedForPart(model, "Arm");
            instancedCog = createInstancedForPart(model, "Cog");
            List<BakedQuad> idleCombinedQuads = buildIdleCombinedQuads(model);
            instancedIdleCombined = createInstancedFromQuads(idleCombinedQuads, "idleCombined");
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to initialize assembler instanced renderers", e);
            clearCaches();
        }
    }

    private static InstancedStaticPartRenderer createInstancedForPart(MachineAssemblerBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        var data = ObjModelVboBuilder.buildSinglePart(part, partName);
        if (data == null) return null;
        var quads = MeshRenderCache.getOrCompile("assembler_legacy_" + partName, part);
        InstancedStaticPartRenderer r = new InstancedStaticPartRenderer(data, quads);
        r.setMdiTraceTag("Assembler/" + partName);
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
        r.setMdiTraceTag("Assembler/" + vboLabel);
        return r;
    }

    /**
     * Склеивает все части в idle-позе (как {@link #renderAnimated} при {@code !isActive})
     * в координаты instance-pose Body: {@code translate(-0.5, 0, -0.5)} после -90° Y.
     */
    private static List<BakedQuad> buildIdleCombinedQuads(MachineAssemblerBakedModel model) {
        TMP_INNER_BODY.identity().translate(-0.5f, 0f, -0.5f);
        TMP_INV_INNER_BODY.set(TMP_INNER_BODY).invert();

        var merged = new ArrayList<BakedQuad>();
        appendPartQuads(merged, model, "Body", new Matrix4f());
        appendPartQuads(merged, model, "Slider", new Matrix4f());
        appendPartQuads(merged, model, "Arm", new Matrix4f());

        BakedModel cogPart = model.getPart("Cog");
        if (cogPart != null) {
            List<BakedQuad> cogQuads = MeshRenderCache.getOrCompile("assembler_legacy_Cog", cogPart);
            if (cogQuads != null && !cogQuads.isEmpty()) {
                Matrix4f cogBake = new Matrix4f();
                for (float[] pos : COG_IDLE_POSITIONS) {
                    buildCogMatrix(cogBake, pos[0], pos[1], pos[2], 0f);
                    cogBake.mul(TMP_INV_INNER_BODY);
                    merged.addAll(ModelHelper.transformQuadsByMatrix(cogQuads, cogBake));
                }
            }
        }

        if (merged.isEmpty()) {
            MainRegistry.LOGGER.warn("MachineAssemblerRenderer: idle combined mesh is empty");
            return List.of();
        }
        MainRegistry.LOGGER.info("MachineAssemblerRenderer: idle combined mesh — {} quads", merged.size());
        return List.copyOf(merged);
    }

    private static void appendPartQuads(List<BakedQuad> merged, MachineAssemblerBakedModel model,
                                        String partName, Matrix4f bakeMatrix) {
        BakedModel part = model.getPart(partName);
        if (part == null) {
            return;
        }
        List<BakedQuad> quads = MeshRenderCache.getOrCompile("assembler_legacy_" + partName, part);
        if (quads == null || quads.isEmpty()) {
            return;
        }
        merged.addAll(ModelHelper.transformQuadsByMatrix(quads, bakeMatrix));
    }

    private static void buildCogMatrix(Matrix4f out, float cx, float cy, float cz, float rotationDeg) {
        out.identity()
                .translate(cx - 0.5f + VBO_COG_OFFSET_X, cy, cz - 0.5f + VBO_COG_OFFSET_Z)
                .rotateZ(rotationDeg * DEG_TO_RAD)
                .translate(-ROOT_TX, 0f, -ROOT_TZ);
    }

    private void initializeInstancedRenderers(MachineAssemblerBakedModel model) {
        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
        }
    }

    @Override
    protected MachineAssemblerBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineAssemblerBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineAssemblerBlockEntity be) {
        return be.getBlockState().getValue(MachineAssemblerBlock.FACING);
    }

    @Override
    protected void renderParts(MachineAssemblerBlockEntity be,
                               MachineAssemblerBakedModel model,
                               LegacyAnimator animator,
                               float partialTick,
                               int packedLight,
                               int packedOverlay,
                               PoseStack poseStack,
                               MultiBufferSource bufferSource) {
        var state = be.getBlockState();

        Direction facing = getFacing(be);
        BlockPos blockPos = be.getBlockPos();
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        var minecraft = Minecraft.getInstance();
        AABB renderBounds;
        if (state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController controller && controller.getStructureHelper() != null) {
            // Inflate MUST match MachineAssemblerBlockEntity.getRenderBoundingBox() (1.35).
            // The assembler's visual (cogs/arms) extends past the 4×2×4 structure cells;
            // occluding against the un-inflated box culls the BER while the cogs are still
            // in view → whole machine flickers as the box toggles around occluder edges.
            renderBounds = controller.getStructureHelper().getRenderBoundingBox(blockPos, facing, 1.35);
        } else {
            renderBounds = be.getRenderBoundingBox();
        }

        // Куллинг + fade: в контрапшене Create shouldRender() пропускает
        // frustum/ray-march кулинг (frustum/ray-march оперируют world-space координатами
        // реальной BlockPos, которая в контрапшене далеко от камеры).
        if (applyCullingAndStaticFade(be, renderBounds) < 0) {
            return;
        }

        // Mark visible so render() knows it's safe to draw the recipe icon.
        // visibleThisFrame is reset to false at the top of render() before
        // super.render() runs, so this only stays true when culling passes.
        visibleThisFrame = true;

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource);
    }

    @Override
    public void render(MachineAssemblerBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        // Pessimistic default - super.render() may early-out (frustum) without
        // ever invoking renderParts(), in which case the flag stays false and
        // we skip the icon. renderParts() flips it to true ONLY after its
        // OcclusionCullingHelper.shouldRender check passes.
        visibleThisFrame = false;
        super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (visibleThisFrame) {
            renderRecipeIconDirect(be, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private void renderWithVBO(MachineAssemblerBlockEntity be,
                               MachineAssemblerBakedModel model,
                               float partialTick,
                               PoseStack poseStack,
                               int dynamicLight,
                               BlockPos blockPos,
                               MultiBufferSource bufferSource) {
        if (!instancersInitialized) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineAssemblerVboRenderer(model);
        }

        boolean useBatching = ModClothConfig.useInstancedBatching();

        // Iris batching: open ONE shader.apply()/clear() pair for the whole
        // machine when:
        //   1) per-type instancing is OFF - amortise across Body + Slider +
        //      Arm + 4 Cogs (= 7 parts × 2 passes).
        //   2) per-type instancing is ON but we are in a shadow pass - the
        //      end-of-stage flush in RenderLevelStageEvent fires only on the
        //      main pass, so InstancedStaticPartRenderer.addInstance() routes
        //      shadow-pass instances directly through drawSingleWithIrisExtended;
        //      opening a batch here lets those 7 redirected single draws share
        //      one apply()/clear() pair. Without this, the assembler either
        //      fails to cast shadows or duplicates itself in the sky.
        // See MachineAdvancedAssemblerRenderer.renderWithVBO and IrisRenderBatch
        // for the full rationale.
        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        //? if forge || neoforge {
        boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive() && (!useBatching || shadowPass);
        //?} elif fabric {
        /*boolean useIrisBatch = ShaderCompatibilityDetector.isExternalShaderActive();
        *///?}
        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderAssemblerPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useBatching);
            }
        } else {
            renderAssemblerPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useBatching);
        }
    }

    private void renderAssemblerPartsInternal(MachineAssemblerBlockEntity be,
                                              MachineAssemblerBakedModel model,
                                              float partialTick,
                                              PoseStack poseStack,
                                              int dynamicLight,
                                              BlockPos blockPos,
                                              MultiBufferSource bufferSource,
                                              boolean useBatching) {
        float staticFade = SingleMeshVboRenderer.getFadeAlpha();
        // BE-оверлоад: bypass fade/cull для контрапшенов и Sable sublevel
        // (raw BlockPos в sublevel ~40M от origin → distanceSqToCamera гигантский → fade=-1).
        float animFade = RenderDistanceHelper.computeAnimatedFade(be);
        boolean anyFading = staticFade < 0.99f || (animFade >= 0 && animFade < 0.99f);
        boolean effectiveBatching = useBatching && !anyFading;

        float[] sharedLight = null;
        if (effectiveBatching) {
            var state = be.getBlockState();
            Direction facing = getFacing(be);
            AABB renderBounds;
            if (state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController controller
                    && controller.getStructureHelper() != null) {
                renderBounds = controller.getStructureHelper().getRenderBoundingBox(blockPos, facing, 0.0);
            } else {
                renderBounds = be.getRenderBoundingBox();
            }
            worldBoundsToBlockLocal(renderBounds, blockPos, machineLightBbox);
            tmpMachineLightPose.identity();
            LightSampleCache.getOrSample8(be, MACHINE_LIGHT_SAMPLE_KEY, machineLightBbox, blockPos,
                    tmpMachineLightPose, dynamicLight, machineSharedLight8);
            sharedLight = machineSharedLight8;
        }

        boolean isActive = be.isCrafting();
        boolean useIdleCombined = !isActive && effectiveBatching
                && instancedIdleCombined != null && instancedIdleCombined.isInitialized();

        // Match legacy orientation: rotate full assembler 90 degrees clockwise.
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
        poseStack.translate(-0.5f, 0.0f, -0.5f);

        if (useIdleCombined) {
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            instancedIdleCombined.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource, sharedLight);
            poseStack.popPose();
        } else {
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            if (effectiveBatching && instancedBody != null && instancedBody.isInitialized()) {
                poseStack.pushPose();
                instancedBody.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource, sharedLight);
                poseStack.popPose();
            } else {
                gpu.renderStaticBody(poseStack, dynamicLight, blockPos, be, bufferSource);
            }
            poseStack.popPose();

            if (animFade >= 0) {
                SingleMeshVboRenderer.setFadeAlpha(Math.min(staticFade, animFade));
                renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource, effectiveBatching, sharedLight);
                SingleMeshVboRenderer.setFadeAlpha(staticFade);
            }
        }
        poseStack.popPose();
    }

    private static void worldBoundsToBlockLocal(AABB world, BlockPos origin, float[] out) {
        out[0] = (float) (world.minX - origin.getX());
        out[1] = (float) (world.minY - origin.getY());
        out[2] = (float) (world.minZ - origin.getZ());
        out[3] = (float) (world.maxX - origin.getX());
        out[4] = (float) (world.maxY - origin.getY());
        out[5] = (float) (world.maxZ - origin.getZ());
    }

    // ==================== ANIMATION ====================

    private void renderAnimated(MachineAssemblerBlockEntity be, float pt,
                                PoseStack pose, int blockLight, BlockPos blockPos,
                                MultiBufferSource bufferSource, boolean useBatching,
                                @Nullable float[] sharedLight) {
        boolean isActive = be.isCrafting();

        long time = System.currentTimeMillis();

        // Slider: ping-pong 0..500 за 5000ms
        float sliderX = 0;
        if (isActive) {
            long t = (time % 5000) / 5;
            int offset = (int) (t > 500 ? 500 - (t - 500) : t);
            sliderX = offset * 0.003f - 0.75f;
        }

        // Arm sway
        float armZ = 0;
        if (isActive) {
            double swayRaw = (time % 2000) / 2.0;
            float sway = (float) Math.sin(swayRaw / Math.PI / 50);
            armZ = sway * 0.3f;
        }

        // Cog rotation
        float cogRotation = 0;
        if (isActive) {
            cogRotation = (float) ((time % (360L * 5)) / 5.0);
        }

        // Slider + Arm share the same base position
        matSlider.identity().translate(sliderX, 0, 0).translate(-0.5f, 0, -0.5f);
        addInstanceOrRender(useBatching, instancedSlider,
                pose, blockLight, blockPos, be, "Slider", matSlider, bufferSource, sharedLight);

        matArm.identity().translate(sliderX, 0, armZ).translate(-0.5f, 0, -0.5f);
        addInstanceOrRender(useBatching, instancedArm,
                pose, blockLight, blockPos, be, "Arm", matArm, bufferSource, sharedLight);

        // 4 Cogs at specific positions
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                -0.6f, 0.75f, 1.0625f, -cogRotation, sharedLight);
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                0.6f, 0.75f, 1.0625f, cogRotation, sharedLight);
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                -0.6f, 0.75f, -1.0625f, -cogRotation, sharedLight);
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                0.6f, 0.75f, -1.0625f, cogRotation, sharedLight);
    }

    // Root transform from machine_assembler.json shifts model by (1,0,2); cog center is there, not at origin.
    private static final float ROOT_TX = 1f, ROOT_TZ = 2f;
    private static final float VBO_COG_OFFSET_X = 1f, VBO_COG_OFFSET_Z = 2f;

    private static final float[][] COG_IDLE_POSITIONS = {
            {-0.6f, 0.75f, 1.0625f},
            {0.6f, 0.75f, 1.0625f},
            {-0.6f, 0.75f, -1.0625f},
            {0.6f, 0.75f, -1.0625f},
    };

    private void renderCog(PoseStack pose, int blockLight, BlockPos blockPos,
                           MachineAssemblerBlockEntity be, MultiBufferSource bufferSource,
                           boolean useBatching,
                           float cx, float cy, float cz, float rotationDeg,
                           @Nullable float[] sharedLight) {
        buildCogMatrix(matCog, cx, cy, cz, rotationDeg);

        addInstanceOrRender(useBatching, instancedCog,
                pose, blockLight, blockPos, be, "Cog", matCog, bufferSource, sharedLight);
    }

    private void addInstanceOrRender(boolean useInstanced, InstancedStaticPartRenderer instanced,
                                     PoseStack pose, int blockLight, BlockPos blockPos,
                                     MachineAssemblerBlockEntity be, String partName,
                                     Matrix4f transform, MultiBufferSource bufferSource,
                                     @Nullable float[] sharedLight) {
        if (useInstanced && instanced != null && instanced.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(transform);
            instanced.addInstance(pose, blockLight, blockPos, be, bufferSource, sharedLight);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, partName, transform, blockPos, be, bufferSource);
        }
    }

    // ==================== RECIPE ICON ====================

    /**
     * Смещения translate(-1, y, 1) ниже настроены для {@link Direction#NORTH} в мир. осях
     * (pose после BER без FACING). Для остальных направлений вращаем вокруг центра блока
     * на тот же угол, что и vanilla blockstate Y (0=N, 90=E, 180=S, 270=W).
     */
    private static float recipeIconYawFromNorth(Direction facing) {
        // ВАЖНО: конвенции вращения chunk (квады) и PoseStack противоположны.
        // См. MultipartFacingTransforms.poseYawFromChunkYaw.
        return (float) MultipartFacingTransforms.poseYawFromChunkYaw(
                MultipartFacingTransforms.vanillaChunkMeshRotationY(facing));
    }

    private void renderRecipeIconDirect(MachineAssemblerBlockEntity be,
                                        PoseStack poseStack,
                                        MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        // BE-оверлоад: bypass fade/cull для контрапшенов и Sable sublevel.
        if (RenderDistanceHelper.computeAnimatedFade(be) < 0) return;

        ItemStack icon = be.getClientRecipeIcon();
        if (icon.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        poseStack.pushPose();
        // Пивот: центр основания контроллера; 4×4 в лок. сетке, эталон — NORTH
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(recipeIconYawFromNorth(getFacing(be))));
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

    // ==================== DISTANCE CHECK ====================

    private boolean shouldSkipAnimatedRender(BlockPos blockPos) {
        return RenderDistanceHelper.computeAnimatedFade(blockPos) < 0;
    }

    // ==================== INSTANCED BATCHING ====================

    public static void flushInstancedBatches(org.joml.Matrix4f projectionMatrix) {
        if (instancedIdleCombined != null) instancedIdleCombined.flush(projectionMatrix);
        if (instancedBody != null) instancedBody.flush(projectionMatrix);
        if (instancedSlider != null) instancedSlider.flush(projectionMatrix);
        if (instancedArm != null) instancedArm.flush(projectionMatrix);
        if (instancedCog != null) instancedCog.flush(projectionMatrix);
    }

    public static void clearCaches() {
        cleanupInstanced(instancedIdleCombined); instancedIdleCombined = null;
        cleanupInstanced(instancedBody); instancedBody = null;
        cleanupInstanced(instancedSlider); instancedSlider = null;
        cleanupInstanced(instancedArm); instancedArm = null;
        cleanupInstanced(instancedCog); instancedCog = null;
        instancersInitialized = false;
    }

    private static void cleanupInstanced(InstancedStaticPartRenderer r) {
        if (r != null) r.cleanup();
    }
}

