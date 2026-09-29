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
        if (be instanceof RenderBoundsProvider provider) {
            return provider.getRenderBoundingBox();
        }
        return new AABB(be.getBlockPos());
    }
    *///?}
}
