package com.hbm_m.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Состояние HBM-дверей на Create-контрапшенах, синкаемое кастомным S2C-пакетом
 * (см. {@code DoorContraptionStatePacket}). Намеренно ВНЕ зависимости от Create:
 * только vanilla-типы, чтобы core-блоки (DoorBlock, UniversalMachinePartBlock)
 * и DoorRenderer могли читать состояние без импорта Create.
 *
 * <p>Ключ — {@link Level} (по identity):
 * <ul>
 *   <li>коллизия: getCollisionShape получает {@code ContraptionWorld} (сервер и клиент — разные экземпляры);</li>
 *   <li>рендер: DoorRenderer читает состояние из {@code VirtualRenderWorld} (клиент).</li>
 * </ul>
 * Пакет-хендлер populate обе карты (ContraptionWorld + VirtualRenderWorld) на клиенте;
 * behaviour populate серверный ContraptionWorld. WeakHashMap — авто-очистка при смерти контрапшена.
 */
public final class ContraptionDoorState {

    /** Коллизия: per-level, per-block-pos (контроллера) → shape в блок-локальных координатах. */
    private static final WeakHashMap<Level, Map<Long, VoxelShape>> SHAPES = new WeakHashMap<>();
    /** Открытое состояние: per-level, controller-local-pos → open. */
    private static final WeakHashMap<Level, Map<Long, Boolean>> OPEN = new WeakHashMap<>();
    
    /** Уровни (миры), которые являются контрапшенами. */
    private static final WeakHashMap<Level, Boolean> CONTRAPTION_WORLDS = new WeakHashMap<>();
    
    /** Маппинг частей к контроллеру: per-level, part-local-pos → controller-local-pos. */
    private static final WeakHashMap<Level, Map<Long, Long>> PART_TO_CONTROLLER = new WeakHashMap<>();

    private ContraptionDoorState() {}

    public static void markContraptionWorld(Level level) {
        if (level != null) CONTRAPTION_WORLDS.put(level, Boolean.TRUE);
    }

    public static boolean isContraptionWorld(Level level) {
        return level != null && CONTRAPTION_WORLDS.containsKey(level);
    }

    public static void setShape(Level level, BlockPos pos, VoxelShape shape) {
        SHAPES.computeIfAbsent(level, k -> new ConcurrentHashMap<>()).put(pos.asLong(), shape);
    }

    public static VoxelShape getShape(Level level, BlockPos pos) {
        Map<Long, VoxelShape> m = SHAPES.get(level);
        return m == null ? null : m.get(pos.asLong());
    }

    public static void setOpen(Level level, BlockPos controllerPos, boolean open) {
        OPEN.computeIfAbsent(level, k -> new ConcurrentHashMap<>()).put(controllerPos.asLong(), open);
    }

    /** Текущий open-таргет контроллера; если записи нет — fallback. */
    public static boolean getOpen(Level level, BlockPos controllerPos) {
        Map<Long, Boolean> m = OPEN.get(level);
        return m != null && Boolean.TRUE.equals(m.get(controllerPos.asLong()));
    }

    public static boolean hasOpenEntry(Level level, BlockPos controllerPos) {
        Map<Long, Boolean> m = OPEN.get(level);
        return m != null && m.containsKey(controllerPos.asLong());
    }

    public static void setPartController(Level level, BlockPos partPos, BlockPos controllerPos) {
        if (level == null || partPos == null || controllerPos == null) return;
        PART_TO_CONTROLLER.computeIfAbsent(level, k -> new ConcurrentHashMap<>())
            .put(partPos.asLong(), controllerPos.asLong());
    }

    public static BlockPos getControllerForPart(Level level, BlockPos partPos) {
        if (level == null || partPos == null) return null;
        Map<Long, Long> m = PART_TO_CONTROLLER.get(level);
        if (m != null) {
            Long c = m.get(partPos.asLong());
            if (c != null) return BlockPos.of(c);
        }
        return null;
    }
}