package com.hbm_m.mixin;

import com.hbm_m.client.model.FabricRenderDataBridge;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if fabric {
/*import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
*///?}

@Mixin(ModelBlockRenderer.class)
@SuppressWarnings("UnstableApiUsage")
public class ModelBlockRendererMixin {

    //? if fabric {
    /*@Inject(method = "tesselateBlock", at = @At("HEAD"))
    private void hbm_m$captureRenderData(BlockAndTintGetter level, BakedModel model, BlockState state,
                                          BlockPos pos, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                          com.mojang.blaze3d.vertex.VertexConsumer consumer, boolean checkSides,
                                          net.minecraft.util.RandomSource random, long seed, int packedOverlay,
                                          CallbackInfo ci) {
        Object data = null;
        if (level instanceof RenderAttachedBlockView attached) {
            data = attached.getBlockEntityRenderAttachment(pos);
        }
        FabricRenderDataBridge.set(data);
    }

    @Inject(method = "tesselateBlock", at = @At("RETURN"))
    private void hbm_m$clearRenderData(BlockAndTintGetter level, BakedModel model, BlockState state,
                                        BlockPos pos, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                        com.mojang.blaze3d.vertex.VertexConsumer consumer, boolean checkSides,
                                        net.minecraft.util.RandomSource random, long seed, int packedOverlay,
                                        CallbackInfo ci) {
        FabricRenderDataBridge.clear();
    }
    *///?}
}
