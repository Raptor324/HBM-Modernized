package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.blockentity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
//?} elif fabric {
/*import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
*///?} elif neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
*///?}

/**
 * VBO-проход спиннера и baked-жидкости Crystallizer.
 * <p>
 * Спиннер — {@link MeshRenderCache} / instancing. Жидкость с подменой спрайта на блок —
 * вне instancing (как chemplant Fluid), через {@link MachineCrystallizerRenderer#presentDeferredFluids()}.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineCrystallizerVboRenderer {

    public static final String SPINNER_CACHE_KEY = "crystallizer:spinner";

    private static final RandomSource RANDOM = RandomSource.create(42L);
    private static final RenderType FLUID_RENDER_TYPE = RenderType.translucent();

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation FLUID_MODEL_ID =
            new ResourceLocation(RefStrings.MODID, "block/machines/crystallizer_fluid");
    public static final ResourceLocation SPINNER_MODEL_ID =
            new ResourceLocation(RefStrings.MODID, "block/machines/crystallizer_spinner");
    *///?} else {
    public static final ResourceLocation FLUID_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_fluid");
    public static final ResourceLocation SPINNER_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_spinner");
    //?}

    @Nullable
    public static BakedModel getFluidModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, FLUID_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    @Nullable
    public static BakedModel getSpinnerModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, SPINNER_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    /** Квады спиннера (cutout OBJ) для VBO / Iris. */
    public static List<BakedQuad> collectSpinnerQuads() {
        BakedModel model = getSpinnerModel();
        if (model == null) return List.of();
        List<BakedQuad> quads = collectModelQuads(model, RenderType.cutout());
        if (!quads.isEmpty()) return quads;
        return collectModelQuads(model, RenderType.solid());
    }

    /** Квады жидкости (translucent OBJ) для deferred baked-прохода. */
    public static List<BakedQuad> collectFluidQuads() {
        BakedModel model = getFluidModel();
        if (model == null) return List.of();
        List<BakedQuad> quads = collectModelQuads(model, RenderType.translucent());
        if (!quads.isEmpty()) return quads;
        return collectModelQuads(model, RenderType.cutout());
    }

    public void renderSpinner(PoseStack poseStack, int packedLight, BlockPos blockPos,
                              @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        List<BakedQuad> quads = collectSpinnerQuads();
        if (quads.isEmpty()) return;
        var r = MeshRenderCache.getOrCreateRendererFromQuadList(SPINNER_CACHE_KEY, quads);
        if (r != null) {
            r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    /**
     * Ставит жидкость в очередь до {@link MachineCrystallizerRenderer#presentDeferredFluids()}.
     * Caller уже применил {@code applyFacingRotation}.
     */
    public static void scheduleDeferredFluid(MachineCrystallizerBlockEntity be, PoseStack poseStack,
                                             int packedLight, int packedOverlay,
                                             TextureAtlasSprite sprite, float r, float g, float b) {
        MachineCrystallizerRenderer.scheduleDeferredFluid(
            be.getBlockPos(), new Matrix4f(poseStack.last().pose()), packedLight, packedOverlay, sprite, r, g, b);
    }

    //? if forge {
    public static void drawCrystallizerFluidBaked(PoseStack poseStack, MultiBufferSource bufferSource,
                                                  int packedLight, int packedOverlay,
                                                  TextureAtlasSprite sprite, float r, float g, float b) {
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        List<BakedQuad> quads = collectFluidQuads();
        if (quads.isEmpty() || sprite == null) return;

        quads = remapQuadsSprite(quads, sprite);
        VertexConsumer vc = bufferSource.getBuffer(FLUID_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        float a = 1.0f;
        for (BakedQuad quad : quads) {
            vc.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay, true);
        }
    }
    //?}

    //? if fabric {
    /*public static void drawCrystallizerFluidBaked(PoseStack poseStack, MultiBufferSource bufferSource,
                                                  int packedLight, int packedOverlay,
                                                  TextureAtlasSprite sprite, float r, float g, float b) {
        List<BakedQuad> quads = collectFluidQuads();
        if (quads.isEmpty() || sprite == null) return;

        quads = remapQuadsSprite(quads, sprite);
        VertexConsumer vc = bufferSource.getBuffer(FLUID_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMat = pose.pose();
        Matrix3f normalMat = pose.normal();
        float a = 1.0f;
        for (BakedQuad quad : quads) {
            emitQuadWithAlpha(vc, poseMat, normalMat, quad, r, g, b, a, packedOverlay, packedLight);
        }
    }

    private static void emitQuadWithAlpha(VertexConsumer vc, Matrix4f poseMat, Matrix3f normalMat,
                                          BakedQuad quad, float r, float g, float b, float a,
                                          int packedOverlay, int packedLight) {
        int[] v = quad.getVertices();
        Direction dir = quad.getDirection();
        float nx = dir.getStepX();
        float ny = dir.getStepY();
        float nz = dir.getStepZ();
        for (int i = 0; i < 4; i++) {
            int base = i * 8;
            float x = Float.intBitsToFloat(v[base]);
            float y = Float.intBitsToFloat(v[base + 1]);
            float z = Float.intBitsToFloat(v[base + 2]);
            float u = Float.intBitsToFloat(v[base + 4]);
            float vv = Float.intBitsToFloat(v[base + 5]);
            vc.vertex(poseMat, x, y, z)
                .color(r, g, b, a)
                .uv(u, vv)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
        }
    }
    *///?}

    @Nullable
    public static TextureAtlasSprite getFluidSprite(MachineCrystallizerBlockEntity be, Fluid fluid) {
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
        //? if fabric {
        /*var mc = Minecraft.getInstance();
        if (mc.level == null || be.getLevel() == null) return null;
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (handler == null) return null;
        var sprites = handler.getFluidSprites(mc.level, be.getBlockPos(), fluid.defaultFluidState());
        if (sprites == null || sprites.length == 0) return null;
        return sprites[0];
        *///?}
        //? if neoforge {
        /*IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        ResourceLocation stillTexture = ext.getStillTexture(stack);
        if (stillTexture == null) return null;
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        *///?}
    }

    public static int getFluidTint(MachineCrystallizerBlockEntity be, Fluid fluid) {
        //? if forge {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        return ext.getTintColor(stack);
        //?}
        //? if fabric {
        /*var handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (handler == null) return 0xFFFFFFFF;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return 0xFFFFFFFF;
        return handler.getFluidColor(mc.level, be.getBlockPos(), fluid.defaultFluidState()) | 0xFF000000;
        *///?}
        //? if neoforge {
        /*IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        return ext.getTintColor(stack);
        *///?}
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
