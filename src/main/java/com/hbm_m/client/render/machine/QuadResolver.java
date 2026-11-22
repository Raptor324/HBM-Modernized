package com.hbm_m.client.render.machine;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Dynamic part geometry depending on BE state (fluid tank walls by fluid type,
 * door DAE nodes, etc.). Returns a quad list per frame; VBO compilation is
 * cached by the engine keyed on {@code cacheKeyFn}.
 */
@FunctionalInterface
public interface QuadResolver<T extends BlockEntity> {
    List<BakedQuad> resolve(T blockEntity);
}
