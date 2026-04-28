package com.hbm_m.client.render;


//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;//?}

import java.util.List;

import com.hbm_m.block.entity.machines.HeatingOvenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.architectury.utils.Env;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
//? if forge {
/*import net.minecraftforge.client.model.data.ModelData;
*///?}

/**
 * Renderer for HeatingOven block entity.
 * Renders animated door and inner burning state based on original 1.7.10 code.
 */
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
public class HeatingOvenRenderer implements BlockEntityRenderer<HeatingOvenBlockEntity> {

    private static final RandomSource RANDOM = RandomSource.create(42);

    public HeatingOvenRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HeatingOvenBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // TODO: Re-enable BER for door animation and inner burning effects once model system is working
        // Currently the block model is rendered statically via RenderShape.MODEL + forge:composite
    }

    private void renderPart(BakedModel part, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        if (part == null) return;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());

        //? if forge {
        /*List<BakedQuad> quads = part.getQuads(null, null, RANDOM, ModelData.EMPTY, RenderType.solid());
        *///?}
        //? if fabric {
        List<BakedQuad> quads = part.getQuads(null, null, RANDOM);
        //?}
        renderQuads(poseStack, buffer, quads, packedLight, packedOverlay);

        for (Direction dir : Direction.values()) {
            //? if forge {
            /*quads = part.getQuads(null, dir, RANDOM, ModelData.EMPTY, RenderType.solid());
            *///?}
            //? if fabric {
            quads = part.getQuads(null, dir, RANDOM);
            //?}
            renderQuads(poseStack, buffer, quads, packedLight, packedOverlay);
        }
    }

    private void renderDoor(BakedModel doorPart, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay, float doorAngle) {
        if (doorPart == null) return;

        poseStack.pushPose();

        // Door slides open based on angle (from original code: door * 0.75 / 135)
        // When fully open (135 degrees), door translates 0.75 blocks
        float doorTranslation = doorAngle * 0.75f / 135f;
        poseStack.translate(0, 0, doorTranslation);

        renderPart(doorPart, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderInnerBurning(BakedModel innerBurningPart, PoseStack poseStack, 
                                     MultiBufferSource bufferSource, int packedOverlay) {
        if (innerBurningPart == null) return;

        // Full brightness for burning inner
        int fullBright = LightTexture.pack(15, 15);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());

        //? if forge {
        /*List<BakedQuad> quads = innerBurningPart.getQuads(null, null, RANDOM, ModelData.EMPTY, RenderType.solid());
        *///?}
        //? if fabric {
        List<BakedQuad> quads = innerBurningPart.getQuads(null, null, RANDOM);
        //?}
        renderQuads(poseStack, buffer, quads, fullBright, packedOverlay);

        for (Direction dir : Direction.values()) {
            //? if forge {
            /*quads = innerBurningPart.getQuads(null, dir, RANDOM, ModelData.EMPTY, RenderType.solid());
            *///?}
            //? if fabric {
            quads = innerBurningPart.getQuads(null, dir, RANDOM);
            //?}
            renderQuads(poseStack, buffer, quads, fullBright, packedOverlay);
        }
    }

    private void renderQuads(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                             int packedLight, int packedOverlay) {
        if (quads.isEmpty()) return;

        var pose = poseStack.last();
        for (BakedQuad quad : quads) {
            buffer.putBulkData(pose, quad, 1.0f, 1.0f, 1.0f, 1.0f, 
                packedLight, packedOverlay, true);
        }
    }

    private float getRotationFromFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case EAST -> 270;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(HeatingOvenBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
