package com.hbm_m.client.render.implementations;


import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;
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
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.architectury.fluid.FluidStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class MachineChemicalPlantRenderer extends AbstractPartBasedRenderer<MachineChemicalPlantBlockEntity, MachineChemicalPlantBakedModel> {

    private MachineChemicalPlantVboRenderer gpu;
    private MachineChemicalPlantBakedModel cachedModel;

    private static volatile InstancedStaticPartRenderer instancedBase;
    private static volatile InstancedStaticPartRenderer instancedFrame;
    private static volatile boolean instancersInitialized = false;

    private final Matrix4f matSlider = new Matrix4f();
    private final Matrix4f matSpinner = new Matrix4f();

    /** Degrees → radians multiplier; see {@code MachineAdvancedAssemblerRenderer.DEG_TO_RAD}. */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public MachineChemicalPlantRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static synchronized void initializeInstancedRenderersSync(MachineChemicalPlantBakedModel model) {
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

    private static InstancedStaticPartRenderer createInstancedForPart(MachineChemicalPlantBakedModel model, String partName) {
        BakedModel part = model.getPart(partName);
        if (part == null) return null;
        String cacheKey = "chemplant_" + partName;
        PartGeometry geo = GlobalMeshCache.getOrCompilePartGeometry(cacheKey, part);
        if (geo.isEmpty()) return null;
        var data = geo.toVboData(partName);
        if (data == null) return null;
        return new InstancedStaticPartRenderer(data, geo.solidQuads());
    }

    private void initializeInstancedRenderers(MachineChemicalPlantBakedModel model) {
        if (!instancersInitialized) {
            initializeInstancedRenderersSync(model);
        }
    }

    @Override
    protected MachineChemicalPlantBakedModel getModelType(BakedModel rawModel) {
        return rawModel instanceof MachineChemicalPlantBakedModel m ? m : null;
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
    protected void renderParts(MachineChemicalPlantBlockEntity be, MachineChemicalPlantBakedModel model, LegacyAnimator animator,
                              float partialTick, int packedLight, int packedOverlay, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        var state = be.getBlockState();
        boolean renderActive = state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)
            && state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);
        boolean useVboGeometry = ShaderCompatibilityDetector.useVboGeometry();

        var minecraft = Minecraft.getInstance();
        BlockPos blockPos = be.getBlockPos();
        AABB renderBounds = be.getRenderBoundingBox();
        if (minecraft.level == null || !OcclusionCullingHelper.shouldRender(blockPos, minecraft.level, renderBounds)) {
            return;
        }

        ChemicalPlantRecipeVisual visual = getRecipeVisual(be);

        // Старый baked-путь под шейдерами + простой: статика и idle-подвижные в baked; BER только жидкость/эффекты.
        // Под новым VBO путём (useVboGeometry==true) baked пуст и нам нужно рендерить всё самим.
        if (!useVboGeometry && !renderActive) {
            if (visual != null) {
                renderSwirl(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
            }
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        renderWithVBO(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, renderActive);

        if (visual != null) {
            renderSwirl(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
        }
    }

    /** Soft peak sine (BobMathUtil.sps). */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private void renderWithVBO(MachineChemicalPlantBlockEntity be, MachineChemicalPlantBakedModel model, float partialTick,
                              PoseStack poseStack, int dynamicLight, BlockPos blockPos, MultiBufferSource bufferSource,
                              boolean renderActive) {
        boolean useVboPath = ShaderCompatibilityDetector.useVboGeometry();

        if (useVboPath && !instancersInitialized) {
            initializeInstancedRenderers(model);
        }

        if (cachedModel != model || gpu == null) {
            cachedModel = model;
            gpu = new MachineChemicalPlantVboRenderer(model);
        }

        boolean useBatching = useVboPath && ModClothConfig.useInstancedBatching();

        // Iris batching: amortise apply()/clear() across Base + Frame + Slider + Spinner.
        // Slider/Spinner always use SingleMeshVboRenderer (never instanced); they need
        // IrisRenderBatch.active() so drawCompanion reuses the shared program + direct
        // matrix uploads. If we only open the batch when (!batching || shadow) — like
        // machines whose animated parts are fully instanced — animated parts hit the
        // standalone apply/clear path per frame and GL spams INVALID_OPERATION / No
        // active program; geometry can vanish or project wrong.
        // Instanced Base/Frame flush later calls flushBatchIris (own apply/clear);
        // ClientModEvents closes any persistent batch before those flushes so ACTIVE
        // is not left stale after shader.clear().
        boolean shadowPass = ShaderCompatibilityDetector.isRenderingShadowPass();
        boolean useIrisBatch = useVboPath && ShaderCompatibilityDetector.useNewIrisVboPath();
        if (useIrisBatch) {
            try (IrisRenderBatch batch = IrisRenderBatch.begin(shadowPass, RenderSystem.getProjectionMatrix())) {
                renderChemicalPlantPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching, renderActive);
            }
        } else {
            renderChemicalPlantPartsInternal(be, model, partialTick, poseStack, dynamicLight, blockPos, bufferSource, useVboPath, useBatching, renderActive);
        }
    }

    private void renderChemicalPlantPartsInternal(MachineChemicalPlantBlockEntity be,
                                                  MachineChemicalPlantBakedModel model,
                                                  float partialTick,
                                                  PoseStack poseStack,
                                                  int dynamicLight,
                                                  BlockPos blockPos,
                                                  MultiBufferSource bufferSource,
                                                  boolean useVboPath,
                                                  boolean useBatching,
                                                  boolean renderActive) {
        var blockState = be.getBlockState();

        /*
         * Статика (Base/Frame) и подвижные части должны быть в одном фрейме: после setupBlockTransform
         * нужен translate(-0.5,0,-0.5), чтобы совпасть с ModelHelper.transformQuadsByFacing / Iris chunk
         * (T(+0.5)*R*T(-0.5) относительно геометрии части). Раньше Slider/Spinner вызывались после popPose()
         * и обходили этот сдвиг - «поворот как будто верный», но меш визуально расходился с baked.
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
                .rotateY(deg * DEG_TO_RAD)
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

    private record ChemicalPlantRecipeVisual(FluidStack textureFluid, float r, float g, float b) {}

    @Nullable
    private static ChemicalPlantRecipeVisual getRecipeVisual(MachineChemicalPlantBlockEntity be) {
        if (!be.getDidProcess()) return null;
        if (Minecraft.getInstance().level == null) return null;
        ResourceLocation id = be.getSelectedRecipeId();
        if (id == null) return null;

        Optional<ChemicalPlantRecipe> recipeOpt = Minecraft.getInstance().level.getRecipeManager()
                .byKey(id)
                .filter(r -> r instanceof ChemicalPlantRecipe)
                .map(r -> (ChemicalPlantRecipe) r);
        if (recipeOpt.isEmpty()) return null;

        ChemicalPlantRecipe recipe = recipeOpt.get();

        // Как 1.7.10: цвет = средний по output fluids, иначе по input fluids.
        List<FluidStack> colorFluids = !recipe.getFluidOutputs().isEmpty()
                ? recipe.getFluidOutputs()
                : List.of();
        if (colorFluids.isEmpty() && !recipe.getFluidInputs().isEmpty()) {
            List<FluidStack> tmp = new java.util.ArrayList<>();
            for (var fin : recipe.getFluidInputs()) {
                var fluid = BuiltInRegistries.FLUID.get(fin.fluidId());
                if (fluid == null) continue;
                tmp.add(FluidStack.create(fluid, (long) fin.amount()));
            }
            colorFluids = tmp;
        }
        if (colorFluids.isEmpty()) return null;

        int colors = 0;
        float rr = 0, gg = 0, bb = 0;
        for (FluidStack fs : colorFluids) {
            if (fs.isEmpty()) continue;
            int tint = IClientFluidTypeExtensions.of(fs.getFluid()).getTintColor(FluidStackHooksForge.toForge(fs));
            rr += ((tint >> 16) & 0xFF) / 255.0F;
            gg += ((tint >> 8) & 0xFF) / 255.0F;
            bb += (tint & 0xFF) / 255.0F;
            colors++;
        }
        if (colors <= 0) return null;
        rr /= colors;
        gg /= colors;
        bb /= colors;

        // Текстура: первая output fluid, иначе первая input fluid.
        FluidStack texFluid = null;
        for (FluidStack out : recipe.getFluidOutputs()) {
            if (!out.isEmpty()) { texFluid = out; break; }
        }
        if (texFluid == null) {
            for (var fin : recipe.getFluidInputs()) {
                var fluid = BuiltInRegistries.FLUID.get(fin.fluidId());
                if (fluid == null) continue;
                texFluid = FluidStack.create(fluid, (long) fin.amount());
                break;
            }
        }
        if (texFluid == null || texFluid.isEmpty()) return null;
        return new ChemicalPlantRecipeVisual(texFluid, rr, gg, bb);
    }

    /** Полупрозрачный «swirl» как 1.7.10: только BER {@link RenderType#translucent}. */
    private static void renderSwirl(MachineChemicalPlantBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay, ChemicalPlantRecipeVisual visual) {
        FluidStack fluid = visual.textureFluid();
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        var stillTexture = ext.getStillTexture(FluidStackHooksForge.toForge(fluid));
        if (stillTexture == null) {
            return;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        float red = visual.r();
        float green = visual.g();
        float blue = visual.b();
        float alpha = 0.85F;
        float fill = 1.0F;

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

    @Override public int getViewDistance() { return 128; }
}
