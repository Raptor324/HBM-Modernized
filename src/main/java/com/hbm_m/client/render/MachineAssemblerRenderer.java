package com.hbm_m.client.render;

import org.joml.Matrix4f;

import com.hbm_m.block.custom.machines.MachineAssemblerBlock;
import com.hbm_m.block.entity.custom.machines.MachineAssemblerBlockEntity;
import com.hbm_m.client.model.MachineAssemblerBakedModel;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.custom.industrial.ItemAssemblyTemplate;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

@OnlyIn(Dist.CLIENT)
public class MachineAssemblerRenderer extends AbstractPartBasedRenderer<MachineAssemblerBlockEntity, MachineAssemblerBakedModel> {

    private MachineAssemblerVboRenderer gpu;
    private MachineAssemblerBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedBody;
    private static volatile InstancedStaticPartRenderer instancedSlider;
    private static volatile InstancedStaticPartRenderer instancedArm;
    private static volatile InstancedStaticPartRenderer instancedCog;
    private static volatile boolean instancersInitialized = false;

    private final Matrix4f matSlider = new Matrix4f();
    private final Matrix4f matArm = new Matrix4f();
    private final Matrix4f matCog = new Matrix4f();

    public MachineAssemblerRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineAssemblerBakedModel model) {
        if (instancersInitialized) return;

        try {
            MainRegistry.LOGGER.info("MachineAssemblerRenderer: Initializing instanced renderers...");
            instancedBody = createInstancedForPart(model, "Body");
            instancedSlider = createInstancedForPart(model, "Slider");
            instancedArm = createInstancedForPart(model, "Arm");
            instancedCog = createInstancedForPart(model, "Cog");
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
        var quads = GlobalMeshCache.getOrCompile("assembler_legacy_" + partName, part);
        return new InstancedStaticPartRenderer(data, quads);
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
        boolean renderActive = state.hasProperty(MachineAssemblerBlock.RENDER_ACTIVE)
                && state.getValue(MachineAssemblerBlock.RENDER_ACTIVE);
        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();

        BlockPos blockPos = be.getBlockPos();
        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        var minecraft = Minecraft.getInstance();
        AABB renderBounds = be.getRenderBoundingBox();
        if (!OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        if (!shaderActive) {
            // No shaders: render everything via VBO/instancing.
            renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource);
        } else if (renderActive && be.isCrafting()) {
            // Shader + active: baked draws Base, BER draws only moving parts via putBulkData.
            renderAnimatedWithBulkData(model, animator, be, partialTick);
        } else {
            // Shader + idle: fully baked model, BER does nothing.
            return;
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource bufferSrc) {
            bufferSrc.endBatch();
        }
    }

    @Override
    public void render(MachineAssemblerBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        renderRecipeIconDirect(be, poseStack, bufferSource, packedLight, packedOverlay);
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
        // Match legacy orientation: rotate full assembler 90 degrees clockwise.
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
        poseStack.translate(-0.5f, 0.0f, -0.5f);

        poseStack.pushPose();
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        if (useBatching && instancedBody != null && instancedBody.isInitialized()) {
            poseStack.pushPose();
            instancedBody.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
            poseStack.popPose();
        } else {
            gpu.renderStaticBody(poseStack, dynamicLight, blockPos, be, bufferSource);
        }
        poseStack.popPose();

        boolean skipAnimation = shouldSkipAnimationUpdate(blockPos);
        if (!skipAnimation) {
            renderAnimated(be, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useBatching);
        }
        poseStack.popPose();
    }

    // ==================== ANIMATION ====================

    private void renderAnimated(MachineAssemblerBlockEntity be, float pt,
                                PoseStack pose, int blockLight, BlockPos blockPos,
                                MultiBufferSource bufferSource, boolean useBatching) {
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
                pose, blockLight, blockPos, be, "Slider", matSlider, bufferSource);

        matArm.identity().translate(sliderX, 0, armZ).translate(-0.5f, 0, -0.5f);
        addInstanceOrRender(useBatching, instancedArm,
                pose, blockLight, blockPos, be, "Arm", matArm, bufferSource);

        // 4 Cogs at specific positions
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                -0.6f, 0.75f, 1.0625f, -cogRotation);
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                0.6f, 0.75f, 1.0625f, cogRotation);
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                -0.6f, 0.75f, -1.0625f, -cogRotation);
        renderCog(pose, blockLight, blockPos, be, bufferSource, useBatching,
                0.6f, 0.75f, -1.0625f, cogRotation);
    }

    // Root transform from machine_assembler.json shifts model by (1,0,2); cog center is there, not at origin.
    private static final float ROOT_TX = 1f, ROOT_TZ = 2f;
    // Position offset to place cogs in slots: VBO vs baked use different coordinate systems.
    private static final float VBO_COG_OFFSET_X = 1f, VBO_COG_OFFSET_Z = 2f;
    private static final double BAKED_COG_OFFSET_X = 0.5, BAKED_COG_OFFSET_Z = 1.5;

    private void renderCog(PoseStack pose, int blockLight, BlockPos blockPos,
                           MachineAssemblerBlockEntity be, MultiBufferSource bufferSource,
                           boolean useBatching,
                           float cx, float cy, float cz, float rotationDeg) {
        // Compensate root transform so pivot = cog center: T(pos+offset)*R*T(-root)
        matCog.identity()
                .translate(cx - 0.5f + VBO_COG_OFFSET_X, cy, cz - 0.5f + VBO_COG_OFFSET_Z)
                .rotateZ((float) Math.toRadians(rotationDeg))
                .translate(-ROOT_TX, 0f, -ROOT_TZ);

        addInstanceOrRender(useBatching, instancedCog,
                pose, blockLight, blockPos, be, "Cog", matCog, bufferSource);
    }

    private void addInstanceOrRender(boolean useInstanced, InstancedStaticPartRenderer instanced,
                                     PoseStack pose, int blockLight, BlockPos blockPos,
                                     MachineAssemblerBlockEntity be, String partName,
                                     Matrix4f transform, MultiBufferSource bufferSource) {
        if (useInstanced && instanced != null && instanced.isInitialized()) {
            pose.pushPose();
            pose.last().pose().mul(transform);
            instanced.addInstance(pose, blockLight, blockPos, be, bufferSource);
            pose.popPose();
        } else {
            gpu.renderAnimatedPart(pose, blockLight, partName, transform, blockPos, be, bufferSource);
        }
    }

    private void renderAnimatedWithBulkData(MachineAssemblerBakedModel model,
                                            LegacyAnimator animator,
                                            MachineAssemblerBlockEntity be,
                                            float pt) {
        if (shouldSkipAnimationUpdate(be.getBlockPos())) return;

        boolean isActive = be.isCrafting();
        if (!isActive) return;

        long time = System.currentTimeMillis();

        long t = (time % 5000) / 5;
        int offset = (int) (t > 500 ? 500 - (t - 500) : t);
        float sliderX = offset * 0.003f - 0.75f;

        double swayRaw = (time % 2000) / 2.0;
        float sway = (float) Math.sin(swayRaw / Math.PI / 50);
        float armZ = sway * 0.3f;

        float cogRotation = (float) ((time % (360L * 5)) / 5.0);

        renderPartBulk(animator, model, "Slider", sliderX, 0, 0, 0);
        renderPartBulk(animator, model, "Arm", sliderX, 0, armZ, 0);
        renderPartBulk(animator, model, "Cog", -0.6, 0.75, 1.0625, -cogRotation);
        renderPartBulk(animator, model, "Cog", 0.6, 0.75, 1.0625, cogRotation);
        renderPartBulk(animator, model, "Cog", -0.6, 0.75, -1.0625, -cogRotation);
        renderPartBulk(animator, model, "Cog", 0.6, 0.75, -1.0625, cogRotation);
    }

    private void renderPartBulk(LegacyAnimator animator, MachineAssemblerBakedModel model, String partName,
                                double x, double y, double z, float rotZDeg) {
        BakedModel part = model.getPart(partName);
        if (part == null) return;
        var quads = GlobalMeshCache.getOrCompile("assembler_legacy_" + partName, part);
        if (quads == null || quads.isEmpty()) return;

        animator.push();
        // Keep orientation consistent with VBO path (90 deg clockwise).
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(-90, 0, 1, 0);
        animator.translate(-0.5, 0.0, -0.5);

        animator.translate(x, y, z);
        if (rotZDeg != 0) {
            animator.translate(BAKED_COG_OFFSET_X, 0.0, BAKED_COG_OFFSET_Z);  // Cog slot offset
            animator.rotate(rotZDeg, 0, 0, 1);
            animator.translate(-ROOT_TX, 0.0, -ROOT_TZ);  // Compensate root transform for cog pivot
        } else {
            animator.translate(-0.5, 0.0, -0.5);  // Slider/Arm block-space centering
        }
        animator.renderQuads(quads);
        animator.pop();
    }

    // ==================== RECIPE ICON ====================

    private void renderRecipeIconDirect(MachineAssemblerBlockEntity be,
                                        PoseStack poseStack,
                                        MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        if (shouldSkipAnimationUpdate(be.getBlockPos())) return;

        ItemStack icon = getRecipeOutput(be);
        if (icon.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        poseStack.pushPose();
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

    private ItemStack getRecipeOutput(MachineAssemblerBlockEntity be) {
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null) return ItemStack.EMPTY;

        ItemStack template = handler.getStackInSlot(4);
        if (template.isEmpty() || !(template.getItem() instanceof ItemAssemblyTemplate)) {
            return ItemStack.EMPTY;
        }

        return ItemAssemblyTemplate.getRecipeOutput(template);
    }

    // ==================== DISTANCE CHECK ====================

    private boolean shouldSkipAnimationUpdate(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();

        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;

        return distanceSquared > thresholdBlocks * thresholdBlocks;
    }

    // ==================== INSTANCED BATCHING ====================

    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (instancedBody != null) instancedBody.flush(event);
        if (instancedSlider != null) instancedSlider.flush(event);
        if (instancedArm != null) instancedArm.flush(event);
        if (instancedCog != null) instancedCog.flush(event);
    }

    public static void clearCaches() {
        cleanupInstanced(instancedBody); instancedBody = null;
        cleanupInstanced(instancedSlider); instancedSlider = null;
        cleanupInstanced(instancedArm); instancedArm = null;
        cleanupInstanced(instancedCog); instancedCog = null;
        instancersInitialized = false;
        MachineAssemblerVboRenderer.clearGlobalCache();
    }

    private static void cleanupInstanced(InstancedStaticPartRenderer r) {
        if (r != null) r.cleanup();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineAssemblerBlockEntity be) {
        return !ShaderCompatibilityDetector.isRenderingShadowPass();
    }

    @Override
    public int getViewDistance() { return 128; }

    public void onResourceManagerReload() {
        clearCaches();
        gpu = null;
        cachedModel = null;
        MainRegistry.LOGGER.debug("Assembler legacy renderer resources reloaded");
    }
}
