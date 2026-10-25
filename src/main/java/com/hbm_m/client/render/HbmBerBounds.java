package com.hbm_m.client.render;

import com.hbm_m.api.render.RenderBoundsProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * See {@link RenderBoundsProvider}.
 *
 * <p>On 1.21.1 NeoForge the vanilla BER pass culls block entities via
 * {@code BlockEntityRenderer#getRenderBoundingBox(be)} (default = a 1-block cube at the BE
 * position), which made multiblocks disappear as soon as the controller block left the screen.
 * We delegate to the BE: if it implements {@link RenderBoundsProvider}, its AABB (usually the
 * whole structure) is used. On 1.20.1 there is no override - BlockEntity#getRenderBoundingBox
 * is called by Forge itself, so this interface is empty there.
 */
public interface HbmBerBounds<T extends BlockEntity> extends BlockEntityRenderer<T> {

    //? if >= 1.21.1 {
    /*@Override
    default AABB getRenderBoundingBox(T be) {
        // Route through the cross-frame cache: vanilla calls this per BE per
        // frame, and multiblock providers (e.g. the advanced assembler) build
        // their AABB + structure lookup on every call - 2.6% of the frame on
        // the 5000-machine fleet, 1.21.1 spark 2026-10-07 (1.20.1 is covered
        // by the Sodium/Embeddium mixin instead, which already consults the
        // cache). The cache dispatches to RenderHooks.getRenderBoundingBox
        // (provider AABB or the 1-block default) with a 40-tick TTL and
        // worldGen/render-dirty invalidation.
        return RenderBoundsCache.get(be);
    }
    *///?}
}
