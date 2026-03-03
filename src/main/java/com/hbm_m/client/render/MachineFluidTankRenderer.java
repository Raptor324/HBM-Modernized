package com.hbm_m.client.render;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.block.machines.MachineFluidTankBlock;
import com.hbm_m.client.model.MachineFluidTankBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class MachineFluidTankRenderer extends AbstractPartBasedRenderer<MachineFluidTankBlockEntity, BakedModel> {
    
    public MachineFluidTankRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected BakedModel getModelType(BakedModel rawModel) {
        return rawModel;
    }

    @Override
    protected Direction getFacing(MachineFluidTankBlockEntity be) {
        return be.getBlockState().getValue(MachineFluidTankBlock.FACING);
    }

    @Override
    protected void renderParts(MachineFluidTankBlockEntity be, BakedModel model,
                               LegacyAnimator animator, float partialTick, int packedLight,
                               int packedOverlay, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (bufferSource == null || !(model instanceof MachineFluidTankBakedModel tankModel)) return;

        var mc = Minecraft.getInstance();
        if (mc.level == null || !OcclusionCullingHelper.shouldRender(be.getBlockPos(), mc.level, be.getRenderBoundingBox())) {
            return;
        }

        int blockLight = LightTexture.block(packedLight);
        int skyLight = LightTexture.sky(packedLight);
        int dynamicLight = LightTexture.pack(blockLight, skyLight);

        poseStack.pushPose();
        // setupTankTransform(be, poseStack, animator);
        
        BakedModel frameModel = tankModel.getPart("Frame");
        BakedModel liquidModel = tankModel.getPart("Tank");

        // 1. Рендерим раму (обычный рендер)
        if (frameModel != null) {
            VertexConsumer frameConsumer = bufferSource.getBuffer(RenderType.solid());
            renderSimpleModel(frameModel, poseStack, frameConsumer, dynamicLight, packedOverlay);
        }

        // 2. Рендерим бак
        if (liquidModel != null) {
            ResourceLocation fluidTexture = getTankTextureLocation(be);
            RenderType dynamicTankRenderType = RenderType.entityCutout(fluidTexture);
            VertexConsumer dynamicConsumer = bufferSource.getBuffer(dynamicTankRenderType);
            
            renderDynamicModelWithFixedUV(liquidModel, poseStack, dynamicConsumer, dynamicLight, packedOverlay);
        }

        poseStack.popPose();
    }
    
    protected ResourceLocation getTankTextureLocation(MachineFluidTankBlockEntity be) {
        Fluid fluid = be.getFluidTank().getTankType();
        if (fluid == null || fluid == Fluids.EMPTY) {
            fluid = be.getFilterFluid();
        }

        if (fluid == null || fluid == Fluids.EMPTY) {
            return ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/block/tank/tank_none.png");
        }

        ResourceLocation typeId = net.minecraftforge.registries.ForgeRegistries.FLUID_TYPES.get().getKey(fluid.getFluidType());
        String fluidName = typeId != null ? typeId.getPath() : "none";
        
        return ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/block/tank/tank_" + fluidName + ".png");
    }

    private void renderSimpleModel(BakedModel bakedModel, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        RandomSource random = RandomSource.create();
        for (Direction dir : Direction.values()) {
            for (var quad : bakedModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, light, overlay, false);
            }
        }
        for (var quad : bakedModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, light, overlay, false);
        }
    }

    private void renderDynamicModelWithFixedUV(BakedModel bakedModel, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        RandomSource random = RandomSource.create();
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : bakedModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), fixQuadUVs(quad), 1.0f, 1.0f, 1.0f, 1.0f, light, overlay, false);
            }
        }
        for (BakedQuad quad : bakedModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(poseStack.last(), fixQuadUVs(quad), 1.0f, 1.0f, 1.0f, 1.0f, light, overlay, false);
        }
    }

    /**
     * Преобразует координаты запеченной модели из формата атласа блоков в формат 0.0-1.0.
     * Это убирает черный цвет и заставляет отдельную текстуру корректно ложиться на модель.
     */
    private BakedQuad fixQuadUVs(BakedQuad original) {
        int[] oldData = original.getVertices();
        int[] newData = new int[oldData.length];
        System.arraycopy(oldData, 0, newData, 0, oldData.length);

        TextureAtlasSprite sprite = original.getSprite();
        if (sprite == null) return original; // Защита от непредвиденных ошибок

        float uDiff = sprite.getU1() - sprite.getU0();
        float vDiff = sprite.getV1() - sprite.getV0();
        
        if (uDiff == 0 || vDiff == 0) return original;

        int vertexSize = oldData.length / 4; // Обычно 8 int на вершину

        for (int i = 0; i < 4; i++) {
            int offset = i * vertexSize;
            
            // В DefaultVertexFormat.BLOCK индексы U и V лежат под номерами 4 и 5
            float oldU = Float.intBitsToFloat(oldData[offset + 4]);
            float oldV = Float.intBitsToFloat(oldData[offset + 5]);

            // Нормализуем координаты к диапазону 0.0 - 1.0
            float newU = (oldU - sprite.getU0()) / uDiff;
            float newV = (oldV - sprite.getV0()) / vDiff;

            // Записываем обратно
            newData[offset + 4] = Float.floatToRawIntBits(newU);
            newData[offset + 5] = Float.floatToRawIntBits(newV);
        }

        return new BakedQuad(newData, original.getTintIndex(), original.getDirection(), sprite, original.isShade());
    }
    
    // private void setupTankTransform(MachineFluidTankBlockEntity be, PoseStack poseStack, LegacyAnimator animator) {
    //     Direction facing = getFacing(be);
    //     animator.setupBlockTransform(facing);
        
    //     poseStack.translate(0.5f, 0.5f, 0.5f);
    //     poseStack.translate(-1.5f, 0.0f, -0.5f);
    //     poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
    // }
}