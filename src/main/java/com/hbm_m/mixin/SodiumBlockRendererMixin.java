package com.hbm_m.mixin;

//? if fabric {
import com.hbm_m.client.model.FabricRenderDataBridge;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public class SodiumBlockRendererMixin {

    @Dynamic("Sodium BlockRenderer")
    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private void hbm_m$captureRenderData(
            me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext ctx,
            me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers buffers,
            CallbackInfo ci) {
        Object data = ((RenderAttachedBlockView) ctx.world()).getBlockEntityRenderAttachment(ctx.pos());
        FabricRenderDataBridge.set(data);
    }

    @Dynamic("Sodium BlockRenderer")
    @Inject(method = "renderModel", at = @At("RETURN"), require = 0)
    private void hbm_m$clearRenderData(
            me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext ctx,
            me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers buffers,
            CallbackInfo ci) {
        FabricRenderDataBridge.clear();
    }
}
//?}
