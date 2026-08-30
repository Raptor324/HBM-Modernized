package com.hbm_m.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Определяет, рендерится ли BlockEntity внутри фейкового мира контрапшена
 * (Create {@code VirtualRenderWorld}, схема, превью и т.п.), а не в основном
 * клиентском {@link ClientLevel}, либо на «трансформированном» рендере —
 * Sable/Aeronautics sublevel (корабль), где блоки лежат в ТОМ ЖЕ ClientLevel,
 * но в plot-grid на координатах ~160k+ блоков от origin.
 *
 * <p>Используется рендерерами HBM чтобы обойти frustum/occlusion-отбраковку,
 * опирающуюся на мировые координаты: BE на контрапшене сообщает локальную
 * позицию, AABB в world-space фрустуме её отбраковывает, и модель пропадает
 * (тень при этом рисуется, т.к. shadow-pass скипает фруustum-чек).
 *
 * <p>Чисто ванильная проверка, без зависимости от Create: основной клиентский
 * уровень — всегда {@code ClientLevel}; фейковые миры Create его не наследуют.
 * Для Sable sublevel'ов уровень КАК РАЗ ClientLevel, поэтому добавлен второй
 * критерий — сохранённый BlockPos BE находится аномально далеко от камеры
 * (ванильный BER вызывается только в пределах view distance ≤ 512 блоков,
 * так что порог 4096 блоков надёжен: plot-grid Sable начинается с чанка 10000).
 */
public final class ContraptionRenderCompat {

    private ContraptionRenderCompat() {}

    /**
     * Порог «аномальной дальности» сохранённого BlockPos от камеры (в блоках).
     * Ваниль вызывает BER максимум в пределах view distance (32 чанка = 512 б),
     * поэтому любой BER-вызов дальше этого порога — спец-рендерер
     * (Sable sublevel / plot-grid), а не обычная машина в мире.
     */
    private static final double SPECIAL_RENDER_MAX_DIST_SQ = 4096.0 * 4096.0;

    /**
     * @return true если сохранённый {@code pos} аномально далеко от камеры —
     * признак того, что BE рендерится спец-диспетчером (Sable sublevel),
     * хотя уровень формально основной {@link ClientLevel}.
     */
    public static boolean isFarFromCamera(BlockPos pos) {
        if (pos == null) return false;
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (cam == null) return false;
        Vec3 camPos = cam.getPosition();
        double dx = pos.getX() + 0.5 - camPos.x;
        double dy = pos.getY() + 0.5 - camPos.y;
        double dz = pos.getZ() + 0.5 - camPos.z;
        return dx * dx + dy * dy + dz * dz > SPECIAL_RENDER_MAX_DIST_SQ;
    }

    /**
     * @return true если BE висит на фейковом уровне (контрапшен/схема) ИЛИ его
     * сохранённая позиция аномально далеко от камеры (Sable sublevel), и
     * position-based culling надо пропустить.
     */
    public static boolean isContraptionRender(BlockEntity be) {
        if (be == null) return false;
        if (isContraptionRenderLevel(be.getLevel())) return true;
        // Sable/Aeronautics sublevel: блоки физически лежат в том же ClientLevel,
        // но в plot-grid (~160k+ блоков). Фрустум/фейд по сохранённой позиции
        // убивают модель — считаем такой рендер «контрапшен-эквивалентным».
        return isFarFromCamera(be.getBlockPos());
    }

    /**
     * Проверка по {@link Level} напрямую — для путей, где BE недоступен (например,
     * {@code OcclusionCullingHelper.shouldRender(BlockPos, Level, AABB)} получает
     * только уровень). Возвращает true для любого уровня, который НЕ является
     * основным клиентским {@link ClientLevel} — Create {@code VirtualRenderWorld} /
     * {@code ContraptionWorld} наследуют {@link Level} (через WrappedLevel), а не ClientLevel.
     */
    public static boolean isContraptionRenderLevel(Level level) {
        return level != null && !(level instanceof ClientLevel);
    }
}
