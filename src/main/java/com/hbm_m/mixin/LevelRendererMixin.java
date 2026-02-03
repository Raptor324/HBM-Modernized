// package com.hbm_m.mixin;

// import com.hbm_m.powerarmor.ModEventHandlerClient;
// import com.hbm_m.powerarmor.overlay.ThermalVisionRenderer;
// import com.mojang.blaze3d.vertex.PoseStack;
// import net.minecraft.client.renderer.LevelRenderer;
// import net.minecraft.client.renderer.LightTexture;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
// import net.minecraft.util.Mth;
// import net.minecraft.world.entity.Entity;
// import org.spongepowered.asm.mixin.Final;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.Shadow;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// /**
//  * When thermal vision is active, re-render every entity with FULL_BRIGHT and a white BufferSource
//  * so mobs are drawn as solid white silhouettes (like SBW thermal "white hot").
//  */
// @Mixin(LevelRenderer.class)
// public class LevelRendererMixin {

//     @Shadow
//     @Final
//     private EntityRenderDispatcher entityRenderDispatcher;

//     @Inject(
//         method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
//         at = @At("HEAD"),
//         cancellable = true
//     )
//     private void hbm_m$renderEntityThermalWhite(
//         Entity pEntity,
//         double pCamX,
//         double pCamY,
//         double pCamZ,
//         float pPartialTick,
//         PoseStack pPoseStack,
//         MultiBufferSource pBufferSource,
//         CallbackInfo ci
//     ) {
//         if (!ModEventHandlerClient.isThermalActive()) {
//             return;
//         }
//         ci.cancel();
//         double d0 = Mth.lerp(pPartialTick, pEntity.xOld, pEntity.getX());
//         double d1 = Mth.lerp(pPartialTick, pEntity.yOld, pEntity.getY());
//         double d2 = Mth.lerp(pPartialTick, pEntity.zOld, pEntity.getZ());
//         float f = Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot());
//         MultiBufferSource buffer = ThermalVisionRenderer.getWhiteEntityBufferSource(pBufferSource);
//         this.entityRenderDispatcher.render(
//             pEntity,
//             d0 - pCamX,
//             d1 - pCamY,
//             d2 - pCamZ,
//             f,
//             pPartialTick,
//             pPoseStack,
//             buffer,
//             LightTexture.FULL_BRIGHT
//         );
//     }
// }
