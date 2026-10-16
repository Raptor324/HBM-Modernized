package com.hbm_m.client.render.machine;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Динамическая геометрия части, зависящая от состояния BE (стены флюид-танка
 * по типу флюида, DAE-ноды дверей и т.п.). Возвращает квад-лист на кадр;
 * компиляция в VBO кешируется движком по ключу из {@code cacheKeyFn}.
 */
@FunctionalInterface
public interface QuadResolver<T extends BlockEntity> {
    List<BakedQuad> resolve(T blockEntity);
}
