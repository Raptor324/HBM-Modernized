package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusDispatcherBypass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Байпас диспетчера для машин Nucleus (см. {@link NucleusDispatcherBypass}):
 * первый вызов render() для нашей машины в проходе запускает плоский collect
 * всего живого списка, остальные — отменяются. Покрывает ОБА пути итерации
 * (Sodium main + Iris shadow — оба зовут BlockEntityRenderDispatcher.render).
 * Не-Nucleus BlockEntity не затрагиваются вовсе.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void nucleus$bypassDispatch(BlockEntity be, float partialTick,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        CallbackInfo ci) {
        if (NucleusDispatcherBypass.shouldBypass(be, partialTick, poseStack, bufferSource)) {
            ci.cancel();
        }
    }
}
