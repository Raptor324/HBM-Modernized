package com.hbm_m.compat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Определяет, рендерится ли BlockEntity внутри фейкового мира контрапшена
 * (Create {@code VirtualRenderWorld}, схема, превью и т.п.), а не в основном
 * клиентском {@link ClientLevel}.
 *
 * <p>Используется рендерерами HBM чтобы обойти frustum/occlusion-отбраковку,
 * опирающуюся на мировые координаты: BE на контрапшене сообщает локальную
 * позицию, AABB в world-space фрустуме её отбраковывает, и модель пропадает
 * (тень при этом рисуется, т.к. shadow-pass скипает фруustum-чек).
 *
 * <p>Чисто ванильная проверка, без зависимости от Create: основной клиентский
 * уровень — всегда {@code ClientLevel}; фейковые миры Create его не наследуют.
 */
public final class ContraptionRenderCompat {

    private ContraptionRenderCompat() {}

    /**
     * @return true если BE висит на фейковом уровне (контрапшен/схема), и
     * position-based culling надо пропустить.
     */
    public static boolean isContraptionRender(BlockEntity be) {
        if (be == null) return false;
        Level level = be.getLevel();
        return level != null && !(level instanceof ClientLevel);
    }
}
