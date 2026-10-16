package com.hbm_m.client.render.machine;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Контекст, доступный {@link MachineRenderHook}'ам при рендеринге кадра станка.
 */
public interface MachineRenderApi {

    /** Итоговый fade этого BE в этом кадре (min(static, animated)). */
    float fadeAlpha();

    /**
     * Итоговая матрица части в этом кадре (block-relative, как на стеке после аниматора),
     * или {@code null}, если часть не рендерилась. Копия — мутировать безопасно.
     * Так хуки (предметы-штампы и т.п.) позиционируются относительно анимированной части.
     */
    @Nullable Matrix4f partTransform(String partName);

    /** Позиция BlockEntity. */
    net.minecraft.core.BlockPos blockPos();
}
