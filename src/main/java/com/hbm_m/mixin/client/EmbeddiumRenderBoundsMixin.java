package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.21.1 counterpart of {@link SodiumRenderBoundsMixin}. Embeddium 1.0.x
 * renamed the renderer to {@code org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer}
 * and moved the BE frustum-cull call from
 * {@code IForgeBlockEntity#getRenderBoundingBox()} (a Forge-only BE extension
 * that does not exist on NeoForge) to
 * {@code BlockEntityRenderer#getRenderBoundingBox(be)} - a NeoForge
 * {@code IBlockEntityRendererExtension} default, invoked from
 * {@code isBlockEntityRendererVisible} for both the culled and global loops.
 * The redirect routes it through {@link com.hbm_m.client.render.RenderBoundsCache}
 * exactly like the 1.20.1 mixin does.
 * <p>
 * {@code @Pseudo} + {@code require = 0}: without Embeddium 1.21.1 the mixin
 * silently does not apply. Native Sodium 0.6+ has no
 * {@code getRenderBoundingBox} call at all (it culls with vanilla
 * {@code getBoundingBoxForCulling()}, whose default is trivial and needs no
 * cache), so no counterpart is needed there.
 */
@Pseudo
@Mixin(targets = "org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer", remap = false)
public class EmbeddiumRenderBoundsMixin {

    @Redirect(method = "isBlockEntityRendererVisible",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;getRenderBoundingBox(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/world/phys/AABB;"),
            remap = false,
            require = 0)
    private net.minecraft.world.phys.AABB hbm$cachedRenderBounds(
            net.minecraft.client.renderer.blockentity.BlockEntityRenderer renderer,
            net.minecraft.world.level.block.entity.BlockEntity be) {
        return com.hbm_m.client.render.RenderBoundsCache.get(be);
    }
}
