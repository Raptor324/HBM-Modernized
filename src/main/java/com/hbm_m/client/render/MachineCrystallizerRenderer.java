package com.hbm_m.client.render;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.entity.machines.MachineCrystallizerBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fluids.FluidStack;
*///?}

//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
//?}

/**
 * Простой BER для отрисовки части {@code Fluid} модели Crystallizer текстурой
 * текущей жидкости в баке.
 *
 * <p>Основная модель ({@code crystallizer.json}) рендерится статически и скрывает
 * часть {@code Fluid} через {@code "visibility": { "Fluid": false }}. Этот рендер
 * берёт отдельную модель ({@code crystallizer_fluid.json}) с только частью
 * {@code Fluid} и переотрисовывает её квады с подменой UV на спрайт текущей жидкости.</p>
 *
 * <p>Когда бак пустой — ничего не рисуется. Когда есть жидкость — рисуется в полную
 * высоту (без анимации уровня заполнения, как первая итерация).</p>
 */
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)
//?}
public class MachineCrystallizerRenderer implements BlockEntityRenderer<MachineCrystallizerBlockEntity> {

    /** Идентификатор отдельной модели жидкости — должна быть зарегистрирована в ClientSetup. */
    //? if fabric && < 1.21.1 {
    public static final ResourceLocation FLUID_MODEL_ID =
            new ResourceLocation(RefStrings.MODID, "block/machines/crystallizer_fluid");
    //?} else {
    /*public static final ResourceLocation FLUID_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_fluid");
    *///?}

    //? if fabric && < 1.21.1 {
    public static final ResourceLocation SPINNER_MODEL_ID =
            new ResourceLocation(RefStrings.MODID, "block/machines/crystallizer_spinner");
    //?} else {
    /*public static final ResourceLocation SPINNER_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/machines/crystallizer_spinner");
    *///?}

    private static final RandomSource RANDOM = RandomSource.create(42L);

    public MachineCrystallizerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(MachineCrystallizerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();
        applyFacingRotation(blockEntity, poseStack);

        renderSpinner(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        renderFluid(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private static void applyFacingRotation(MachineCrystallizerBlockEntity blockEntity, PoseStack poseStack) {
        BlockState state = blockEntity.getBlockState();
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return;

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        float rot = switch (facing) {
            case SOUTH -> 180F;
            case EAST  -> 270F;
            case WEST  -> 90F;
            default -> 0F;
        };

        if (rot != 0F) {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            poseStack.translate(-0.5, 0, -0.5);
        }
    }

    private static void renderSpinner(MachineCrystallizerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel spinnerModel = getSpinnerModel();
        if (spinnerModel == null) return;

        float angle = Mth.lerp(partialTick, blockEntity.prevAngle, blockEntity.angle);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5, 0.0, -0.5);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        List<BakedQuad> quads = collectQuads(spinnerModel, RenderType.cutout());
        if (!quads.isEmpty()) {
            renderQuads(poseStack, buffer, quads, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static void renderFluid(MachineCrystallizerBlockEntity blockEntity, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Бак пустой — жидкость не рисуем.
        if (blockEntity.getTank().isEmpty()) return;

        Fluid fluid = blockEntity.getTank().getStoredFluid();
        if (fluid == null) return;

        TextureAtlasSprite sprite = getFluidSprite(blockEntity, fluid);
        if (sprite == null) return;

        // Цвет тинта жидкости (для воды — голубой; для большинства химии — белый).
        int color = getFluidTint(blockEntity, fluid);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = 1.0F;

        BakedModel fluidModel = getFluidModel();
        if (fluidModel == null) return;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        List<BakedQuad> quads = collectQuads(fluidModel, RenderType.translucent());
        if (!quads.isEmpty()) {
            renderQuadsWithSprite(poseStack, buffer, quads, sprite, r, g, b, a, packedLight, packedOverlay);
        }
    }

    private static TextureAtlasSprite getFluidSprite(MachineCrystallizerBlockEntity be, Fluid fluid) {
        // HBM fluids have world textures at textures/block/fluids/<fluid>.png.
        // Using those avoids GUI-only fluid textures and missing atlas sprites in BER.
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
        /*IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        ResourceLocation stillTexture = ext.getStillTexture(stack);
        if (stillTexture == null) return null;
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        *///?}
        //? if fabric {
        var mc = Minecraft.getInstance();
        if (mc.level == null || be.getLevel() == null) return null;
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (handler == null) return null;
        var sprites = handler.getFluidSprites(mc.level, be.getBlockPos(), fluid.defaultFluidState());
        if (sprites == null || sprites.length == 0) return null;
        return sprites[0];
        //?}
    }

    private static int getFluidTint(MachineCrystallizerBlockEntity be, Fluid fluid) {
        //? if forge {
        /*IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        FluidStack stack = new FluidStack(fluid, be.getTank().getFluidAmountMb());
        return ext.getTintColor(stack);
        *///?}
        //? if fabric {
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
        if (handler == null) return 0xFFFFFFFF;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return 0xFFFFFFFF;
        return handler.getFluidColor(mc.level, be.getBlockPos(), fluid.defaultFluidState()) | 0xFF000000;
        //?}
    }

    private static BakedModel getFluidModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(FLUID_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    private static BakedModel getSpinnerModel() {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(SPINNER_MODEL_ID);
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }

    private static List<BakedQuad> collectQuads(BakedModel model, RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        //? if forge {
        /*quads.addAll(model.getQuads(null, null, RANDOM, ModelData.EMPTY, renderType));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, RANDOM, ModelData.EMPTY, renderType));
        }
        *///?}
        //? if fabric {
        quads.addAll(model.getQuads(null, null, RANDOM));
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(null, dir, RANDOM));
        }
        //?}
        return quads;
    }

    private static void renderQuads(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                    float r, float g, float b, float a, int packedLight, int packedOverlay) {
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            //? if forge {
            /*buffer.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay, true);
            *///?} else {
            buffer.putBulkData(pose, quad, r, g, b, packedLight, packedOverlay);
            //?}
        }
    }

    private static void renderQuadsWithSprite(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                              TextureAtlasSprite sprite, float r, float g, float b, float a,
                                              int packedLight, int packedOverlay) {
        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            BakedQuad reskinned = remapQuadSprite(quad, sprite);
            //? if forge {
            /*buffer.putBulkData(pose, reskinned, r, g, b, a, packedLight, packedOverlay, true);
            *///?} else {
            buffer.putBulkData(pose, reskinned, r, g, b, packedLight, packedOverlay);
            //?}
        }
    }

    /**
     * Перевязывает UV-координаты quad'а на новый спрайт. Вершины 1.20.1
     * (DefaultVertexFormat.BLOCK): pos(3f) + color(1i) + uv(2f) + light(1i) + normal(1i) = 8 int'ов.
     */
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

    @Override
    public boolean shouldRenderOffScreen(MachineCrystallizerBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return RenderDistanceHelper.getStaticViewDistanceBlocks();
    }
}
