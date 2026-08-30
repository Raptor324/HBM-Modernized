package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
*///?}

/**
 * Кристаллизатор на фабрике {@link MachineRenderers}: корпус живёт в чанк-меше,
 * спиннер — динамическая часть из injected-модели crystallizer_spinner, жидкость —
 * immediate-хук с отложенной отрисовкой (draw в AFTER_BLOCK_ENTITIES после
 * instanced-flush, см. {@link #presentDeferredFluids()}).
 */
public final class MachineCrystallizerRenderer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final RandomSource RANDOM = RandomSource.create(42L);

    public static void register() {
        MachineRenderers.machine("crystallizer", ModBlockEntities.CRYSTALLIZER.get(),
                MachineCrystallizerBlockEntity.class)
            .dynamicPart("Spinner", be -> spinnerQuads(), be -> "spinner")
            .blockTransform(MachineCrystallizerRenderer::applyBlockTransform)
            .hook(MachineCrystallizerRenderer::scheduleFluid)
            .register();

        // Кеш квадов спиннера инвалидируется централизованно
        RenderCacheManager.register(reason -> cachedSpinnerQuads = null);
    }

    private MachineCrystallizerRenderer() {}

    // ── Трансформы (вербатим из легаси applyFacingRotation + matSpinner) ──

    private static void applyBlockTransform(MachineCrystallizerBlockEntity be, LegacyAnimator animator) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST  -> 270F;
            case WEST  -> 90F;
            default -> 0F;
        };
        if (rot != 0F) {
            animator.translate(0.5, 0, 0.5);
            animator.rotate(rot, 0, 1, 0);
            animator.translate(-0.5, 0, -0.5);
        }
    }

    private static boolean animateSpinner(MachineCrystallizerBlockEntity be, float partialTick,
                                          long gameTime, PoseStack pose) {
        float angle = Mth.lerp(partialTick, be.prevAngle, be.angle);
        pose.last().pose().mul(new Matrix4f()
                .translate(0.5f, 0f, 0.5f)
                .rotateY(angle * DEG_TO_RAD)
                .translate(-0.5f, 0f, -0.5f));
        return true;
    }

    // ── Спиннер: injected-модель, квад-кеш ─────────────────────────────

    private static final ResourceLocation SPINNER_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_spinner");

    private static volatile List<BakedQuad> cachedSpinnerQuads;

    private static List<BakedQuad> spinnerQuads() {
        List<BakedQuad> q = cachedSpinnerQuads;
        if (q == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            BakedModel model = PlatformHooks.getModel(modelManager, SPINNER_MODEL_ID);
            if (model == null || model == modelManager.getMissingModel()) return List.of();
            q = collectModelQuads(model, RenderType.cutout());
            if (q.isEmpty()) q = collectModelQuads(model, RenderType.solid());
            cachedSpinnerQuads = q;
        }
        return q;
    }

    private static List<BakedQuad> collectModelQuads(BakedModel model, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        //? if forge {
        quads.addAll(model.getQuads(null, null, RANDOM, ModelData.EMPTY, renderType));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, RANDOM, ModelData.EMPTY, renderType));
        }
        //?}
        //? if fabric {
        /*quads.addAll(model.getQuads(null, null, RANDOM));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, RANDOM));
        }
        *///?}
        return quads;
    }

    // ==================== DEFERRED FLUID ====================

    private record DeferredCrystallizerFluid(
        BlockPos pos,
        Matrix4f pose,
        int packedLight,
        int packedOverlay,
        TextureAtlasSprite sprite,
        float r, float g, float b
    ) {}

    private static final List<DeferredCrystallizerFluid> DEFERRED_FLUIDS = new ArrayList<>();

    /** Сброс очереди жидкости в начале кадра (до BER). */
    public static void clearDeferredFluids() {
        DEFERRED_FLUIDS.clear();
    }

    /** Хук: фиксируем pose/цвет, draw — позже. */
    private static void scheduleFluid(MachineCrystallizerBlockEntity be, float partialTick,
                                      PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay, MachineRenderApi api) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) return;
        if (be.getTank().isEmpty()) return;
        Fluid fluid = be.getTank().getStoredFluid();
        if (fluid == null) return;

        TextureAtlasSprite sprite = getFluidSprite(be, fluid);
        if (sprite == null) return;

        int color = getFluidTint(be, fluid);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        DEFERRED_FLUIDS.add(new DeferredCrystallizerFluid(
            be.getBlockPos(), new Matrix4f(poseStack.last().pose()), packedLight, packedOverlay, sprite, r, g, b));
    }

    /**
     * После flush instanced: depth уже содержит спиннеры, жидкость только depth-test.
     */
    public static void presentDeferredFluids() {
        if (DEFERRED_FLUIDS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            DEFERRED_FLUIDS.clear();
            return;
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = new PoseStack();

        //? if forge {
        try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
            for (DeferredCrystallizerFluid entry : DEFERRED_FLUIDS) {
                poseStack.pushPose();
                poseStack.last().pose().set(entry.pose);
                drawCrystallizerFluidBaked(
                    poseStack, buffers, entry.packedLight, entry.packedOverlay,
                    entry.sprite, entry.r, entry.g, entry.b);
                poseStack.popPose();
            }
            buffers.endBatch();
        }
        //?}
        DEFERRED_FLUIDS.clear();
    }

    private static void drawCrystallizerFluidBaked(PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, int packedOverlay,
                                                   TextureAtlasSprite sprite, float r, float g, float b) {
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        List<BakedQuad> quads = collectFluidQuads();
        if (quads.isEmpty() || sprite == null) return;

        quads = remapQuadsSprite(quads, sprite);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();
        float a = 1.0f;
        for (BakedQuad quad : quads) {
            RenderHooks.putBulkData(vc, pose, quad, r, g, b, a, packedLight, packedOverlay, true);
        }
    }

    private static List<BakedQuad> collectFluidQuads() {
        ResourceLocation fluidModelId =
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_fluid");
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, fluidModelId);
        if (model == null || model == modelManager.getMissingModel()) return List.of();
        List<BakedQuad> quads = collectModelQuads(model, RenderType.translucent());
        if (!quads.isEmpty()) return quads;
        return collectModelQuads(model, RenderType.cutout());
    }

    @Nullable
    private static TextureAtlasSprite getFluidSprite(MachineCrystallizerBlockEntity be, Fluid fluid) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId != null && RefStrings.MODID.equals(fluidId.getNamespace())) {
            String path = fluidId.getPath();
            if (path.endsWith("_flowing")) {
                path = path.substring(0, path.length() - "_flowing".length());
            }
            ResourceLocation blockFluidTexture = ResourceLocation.fromNamespaceAndPath(
                    RefStrings.MODID, "block/fluids/" + path);
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(blockFluidTexture);
        }

        //? if forge {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        ResourceLocation stillTexture = ext.getStillTexture(stack);
        if (stillTexture == null) return null;
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        //?}
        //? if neoforge {
        /*IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        ResourceLocation stillTexture = ext.getStillTexture(stack);
        if (stillTexture == null) return null;
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        *///?}
    }

    private static int getFluidTint(MachineCrystallizerBlockEntity be, Fluid fluid) {
        //? if forge {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        return ext.getTintColor(stack);
        //?}
        //? if neoforge {
        /*IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        return ext.getTintColor(stack);
        *///?}
    }

    private static List<BakedQuad> remapQuadsSprite(List<BakedQuad> source, TextureAtlasSprite newSprite) {
        List<BakedQuad> out = new ArrayList<>(source.size());
        for (BakedQuad quad : source) {
            out.add(remapQuadSprite(quad, newSprite));
        }
        return out;
    }

    private static BakedQuad remapQuadSprite(BakedQuad source, TextureAtlasSprite newSprite) {
        int[] originalVertices = source.getVertices();
        int[] vertices = originalVertices.clone();
        TextureAtlasSprite oldSprite = source.getSprite();

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);

            float localU = (u - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
            float localV = (v - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

            float newU = newSprite.getU0() + localU * (newSprite.getU1() - newSprite.getU0());
            float newV = newSprite.getV0() + localV * (newSprite.getV1() - newSprite.getV0());

            vertices[offset + 4] = Float.floatToRawIntBits(newU);
            vertices[offset + 5] = Float.floatToRawIntBits(newV);
        }

        return new BakedQuad(vertices, source.getTintIndex(), source.getDirection(),
                newSprite, source.isShade());
    }
}
