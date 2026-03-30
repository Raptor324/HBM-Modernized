package com.hbm_m.client.render.implementations;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.client.model.ChemicalPlantBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.client.render.InstancedStaticPartRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.OcclusionCullingHelper;
import com.hbm_m.client.render.PartGeometry;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;

@OnlyIn(Dist.CLIENT)
public class ChemicalPlantRenderer extends AbstractPartBasedRenderer<MachineChemicalPlantBlockEntity, ChemicalPlantBakedModel> {

    private ChemicalPlantVboRenderer gpu;
    private ChemicalPlantBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile boolean instancersInitialized = false;

    private final Matrix4f matSlider = new Matrix4f();
    private final Matrix4f matSpinner = new Matrix4f();

    public ChemicalPlantRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(ChemicalPlantBakedModel model) {
        if (instancersInitialized) return;
        try {
            MainRegistry.LOGGER.info("ChemicalPlantRenderer: initializing instanced renderers...");
            instancedBase = createInstancedForPart(model, "Base");
            instancedFrame = createInstancedForPart(model, "Frame");
            instancersInitialized = true;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("ChemicalPlantRenderer: failed to init instanced renderers", e);
            clearCaches();
        }
    }

    private static InstancedStaticPartRenderer createInstancedForPart(ChemicalPlantBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        String cacheKey = "chemplant_" + partName;
        PartGeometry geo = GlobalMeshCache.getOrCompilePartGeometry(cacheKey, part);
        if (geo.isEmpty()) return null;
        var data = geo.toVboData(partName);
        if (data == null) return null;
        return new InstancedStaticPartRenderer(data, geo.solidQuads());
    }

    private void initializeInstancedRenderers(ChemicalPlantBakedModel model) {
        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
        }
    }

    @Override
    protected ChemicalPlantBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof ChemicalPlantBakedModel m ? m : null;
    }

    @Override
    protected Direction getFacing(MachineChemicalPlantBlockEntity be) {
        return be.getBlockState().getValue(MachineChemicalPlantBlock.FACING);
    }

    @Override
    protected void setupBlockTransform(LegacyAnimator animator, MachineChemicalPlantBlockEntity be) {
        var state = be.getBlockState();
        if (state.hasProperty(MachineChemicalPlantBlock.FACING)) {
            animator.setupChemicalPlantBlockTransform(state.getValue(MachineChemicalPlantBlock.FACING));
        } else {
            animator.translate(0.5, 0.0, 0.5);
        }
    }

    @Override
    protected void renderParts(MachineChemicalPlantBlockEntity be, ChemicalPlantBakedModel model, LegacyAnimator animator,
                              float partialTick, int packedLight, int packedOverlay, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        var state = be.getBlockState();
        boolean renderActive = state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)
            && state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();

        var minecraft = Minecraft.getInstance();
        BlockPos blockPos = be.getBlockPos();
        AABB renderBounds = be.getRenderBoundingBox();
        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        // Шейдер + простой: статика и idle-подвижные части в baked; BER только жидкость.
        if (shaderActive && !renderActive) {
            FluidStack fluid = be.getFluid();
            if (!fluid.isEmpty()) {
                renderFluid(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, fluid);
            }
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, renderActive);

        FluidStack fluid = be.getFluid();
        if (!fluid.isEmpty()) {
            renderFluid(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, fluid);
        }
    }

    /** Soft peak sine (BobMathUtil.sps). */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private void renderWithVBO(MachineChemicalPlantBlockEntity be, ChemicalPlantBakedModel model, float partialTick,
                              PoseStack poseStack, int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                              boolean renderActive) {
        boolean useVboPath = !ShaderCompatibilityDetector.isExternalShaderActive();

        if (useVboPath && !instancersInitialized) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new ChemicalPlantVboRenderer(model);
        }

        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();
        var blockState = be.getBlockState();

        /*
         * Статика (Base/Frame) и подвижные части должны быть в одном фрейме: после setupBlockTransform
         * нужен translate(-0.5,0,-0.5), чтобы совпасть с ModelHelper.transformQuadsByFacing / Iris chunk
         * (T(+0.5)*R*T(-0.5) относительно геометрии части). Раньше Slider/Spinner вызывались после popPose()
         * и обходили этот сдвиг — «поворот как будто верный», но меш визуально расходился с baked.
         */
        if (useVboPath || renderActive) {
            poseStack.pushPose();
            poseStack.translate(-0.5f, 0.0f, -0.5f);

            if (useVboPath) {
                if (useBatching && instancedBase != null && instancedBase.isInitialized()) {
                    poseStack.pushPose();
                    instancedBase.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
                    poseStack.popPose();
                } else {
                    gpu.renderStaticBase(poseStack, dynamicLight, blockPos, be, bufferSource);
                }

                if (blockState.hasProperty(MachineChemicalPlantBlock.FRAME) && blockState.getValue(MachineChemicalPlantBlock.FRAME)) {
                    if (useBatching && instancedFrame != null && instancedFrame.isInitialized()) {
                        poseStack.pushPose();
                        instancedFrame.addInstance(poseStack, dynamicLight, blockPos, be, bufferSource);
                        poseStack.popPose();
                    } else {
                        gpu.renderStaticFrame(poseStack, dynamicLight, blockPos, be, bufferSource);
                    }
                }
            }

            boolean skipAnimation = shouldSkipAnimationUpdate(blockPos);
            float anim = skipAnimation ? 0f : be.getAnim(partialTick);

            double sdx = chemicalSps(anim * 0.125) * 0.375;
            // Как 1.7.10: после поворота facing только сдвиг по локальной X, без лишнего T(-0.5) на матрице части.
            matSlider.identity().translate((float) sdx, 0f, 0f);

            gpu.renderAnimatedPart(poseStack, dynamicLight, "Slider", matSlider, blockPos, be, bufferSource);

            float deg = (anim * 15f) % 360f;
            matSpinner.identity()
                .translate(0.5f, 0f, 0.5f)
                .rotateY((float) Math.toRadians(deg))
                .translate(-0.5f, 0f, -0.5f);

            gpu.renderAnimatedPart(poseStack, dynamicLight, "Spinner", matSpinner, blockPos, be, bufferSource);

            poseStack.popPose();
        }
    }

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

    public static void flushInstancedBatches(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        flushInstanced(event, instancedBase);
        flushInstanced(event, instancedFrame);
    }

    public static void clearCaches() {
        cleanupInstanced(instancedBase);
        instancedBase = null;
        cleanupInstanced(instancedFrame);
        instancedFrame = null;
        instancersInitialized = false;
    }

    private static void cleanupInstanced(InstancedStaticPartRenderer r) {
        if (r != null) r.cleanup();
    }

    private static void flushInstanced(net.minecraftforge.client.event.RenderLevelStageEvent event,
                                       InstancedStaticPartRenderer r) {
        if (r != null) r.flush(event);
    }

    /** Жидкость с альфой: только BER {@link RenderType#translucent}; baked/chunk такое не поддерживает. */
    private static void renderFluid(MachineChemicalPlantBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay, FluidStack fluid) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        var stillTexture = ext.getStillTexture(fluid);
        if (stillTexture == null) {
            return;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        int tintColor = ext.getTintColor(fluid);

        float red = ((tintColor >> 16) & 0xFF) / 255.0F;
        float green = ((tintColor >> 8) & 0xFF) / 255.0F;
        float blue = (tintColor & 0xFF) / 255.0F;
        float alpha = 0.85F;

        float fill = blockEntity.getFluidFillFraction();
        if (fill <= 0.001F) {
            return;
        }

        boolean scrollActive = blockEntity.getDidProcess();
        long time = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        float t = (time + partialTick) * 0.02F;
        float scroll = t - (float) Mth.floor(t);

        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();

        float du = scrollActive ? (uMax - uMin) * scroll : 0f;
        float dv = scrollActive ? (vMax - vMin) * scroll : 0f;

        float x0 = 0.20F;
        float z0 = 0.20F;
        float x1 = 0.80F;
        float z1 = 0.80F;
        float y0 = 0.10F;
        float y1 = y0 + 0.60F * fill;

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMat = pose.pose();
        Matrix3f normalMat = pose.normal();

        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 1, 0);

        addVertex(vc, poseMat, normalMat, x0, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x1, y0, z0, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 0, -1);

        addVertex(vc, poseMat, normalMat, x0, y0, z1, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x1, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 0, 1);

        addVertex(vc, poseMat, normalMat, x0, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, -1, 0, 0);

        addVertex(vc, poseMat, normalMat, x1, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 1, 0, 0);

        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer vc, Matrix4f poseMat, Matrix3f normalMat,
                                 float x, float y, float z,
                                 float r, float g, float b, float a,
                                 float u, float v,
                                 int overlay, int light,
                                 float nx, float ny, float nz) {
        vc.vertex(poseMat, x, y, z)
            .color(r, g, b, a)
            .uv(u, v)
            .overlayCoords(overlay)
            .uv2(light)
            .normal(normalMat, nx, ny, nz)
            .endVertex();
    }
}
