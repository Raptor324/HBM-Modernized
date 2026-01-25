package com.hbm_m.util.explosions.trash_that_i_forgot_to_delete;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.main.MainRegistry;

/**
 * ОПТИМИЗИРОВАННЫЙ ТРЕКЕР ЗАТВЕРДЕВАНИЯ СЕЛЛАФИТА v2
 *
 * Основные улучшения:
 * ✅ ConcurrentHashMap для потокобезопасности
 * ✅ Использование long вместо составного ключа
 * ✅ Сокращение создания объектов
 * ✅ Оптимизированное управление памятью
 */
public class SellafitSolidificationTracker {

    /**
     * Карта для отслеживания чанков и времени их загрузки
     * Используем long для быстрого доступа вместо создания ChunkPos
     */
    private static final Map<Long, Integer> chunkSolidificationTime = new ConcurrentHashMap<>();

    /**
     * Конфигурация затвердевания селлафита
     * 1 тик = 50мс, 20 тиков = 1 сек
     */
    private static final int SELLAFIT_SOLIDIFICATION_TICKS = 200; // 10 секунд
    private static final int SAFETY_MARGIN_TICKS = 20; // Дополнительный запас
    private static final int TOTAL_SOLIDIFICATION_TICKS =
            SELLAFIT_SOLIDIFICATION_TICKS + SAFETY_MARGIN_TICKS; // 220 тиков

    // ОПТИМИЗАЦИЯ: Кэш размера для быстрого доступа к количеству чанков
    private static volatile int cachedSize = 0;

    /**
     * Регистрирует момент начала обработки чанка
     */
    public static void registerChunkStart(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        int currentTick = (int)(level.getGameTime() & Integer.MAX_VALUE); // Быстрее чем %
        chunkSolidificationTime.put(chunkKey, currentTick);
        cachedSize++;
    }

    /**
     * Проверяет, достаточно ли времени прошло для затвердевания селлафита в чанке
     */
    public static boolean isChunkSolidified(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        Integer startTickObj = chunkSolidificationTime.get(chunkKey);

        // Если не зарегистрирован, значит он уже твёрдый
        if (startTickObj == null) {
            return true;
        }

        int startTick = startTickObj;
        int currentTick = (int)(level.getGameTime() & Integer.MAX_VALUE);
        int elapsedTicks = currentTick - startTick;

        // Обработка переполнения (переход через Integer.MAX_VALUE)
        if (elapsedTicks < 0) {
            elapsedTicks += Integer.MAX_VALUE;
        }

        return elapsedTicks >= TOTAL_SOLIDIFICATION_TICKS;
    }

    /**
     * Получает оставшееся время затвердевания в тиках
     */
    public static int getRemainingTicks(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        Integer startTickObj = chunkSolidificationTime.get(chunkKey);

        if (startTickObj == null) {
            return 0;
        }

        int startTick = startTickObj;
        int currentTick = (int)(level.getGameTime() & Integer.MAX_VALUE);
        int elapsedTicks = currentTick - startTick;

        if (elapsedTicks < 0) {
            elapsedTicks += Integer.MAX_VALUE;
        }

        return Math.max(0, TOTAL_SOLIDIFICATION_TICKS - elapsedTicks);
    }

    /**
     * Очищает запись о чанке (когда затвердевание завершено)
     */
    public static void clearChunk(int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        if (chunkSolidificationTime.remove(chunkKey) != null) {
            cachedSize--;
        }
    }

    /**
     * ОПТИМИЗИРОВАНА: Вспомогательный метод для создания уникального ключа чанка
     * Использует битовые операции для быстрого создания long из двух int
     */
    private static long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Получает координаты чанка из позиции блока
     */
    public static int[] getChunkCoordinates(BlockPos pos) {
        int chunkX = pos.getX() >> 4; // Быстрее чем / 16
        int chunkZ = pos.getZ() >> 4;
        return new int[]{chunkX, chunkZ};
    }

    /**
     * Получает X координату чанка из позиции блока (оптимизирована)
     */
    public static int getChunkX(BlockPos pos) {
        return pos.getX() >> 4;
    }

    /**
     * Получает Z координату чанка из позиции блока (оптимизирована)
     */
    public static int getChunkZ(BlockPos pos) {
        return pos.getZ() >> 4;
    }

    /**
     * Проверяет, зарегистрирован ли чанк
     */
    public static boolean isChunkRegistered(int chunkX, int chunkZ) {
        return chunkSolidificationTime.containsKey(getChunkKey(chunkX, chunkZ));
    }

    /**
     * Получает количество зарегистрированных чанков
     * ОПТИМИЗИРОВАНО: использует кэшированное значение вместо .size()
     */
    public static int getTrackedChunksCount() {
        return cachedSize;
    }

    /**
     * Очищает все данные (используется при перезагрузке/выгрузке уровня)
     */
    public static void clearAll() {
        chunkSolidificationTime.clear();
        cachedSize = 0;
    }

    /**
     * Получает copy всех отслеживаемых чанков (для отладки)
     */
    public static Set<Long> getAllTrackedChunks() {
        return new HashSet<>(chunkSolidificationTime.keySet());
    }

    /**
     * Очищает старые чанки, затвердевшие более 30 секунд назад
     * ОПТИМИЗИРОВАНО: Периодическая очистка кэша
     */
    public static void cleanupOldEntries(ServerLevel level) {
        int currentTick = (int)(level.getGameTime() & Integer.MAX_VALUE);
        int cleanupThreshold = TOTAL_SOLIDIFICATION_TICKS + 600; // +30 секунд буфер

        List<Long> keysToRemove = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : chunkSolidificationTime.entrySet()) {
            int elapsedTicks = currentTick - entry.getValue();
            if (elapsedTicks < 0) {
                elapsedTicks += Integer.MAX_VALUE;
            }

            if (elapsedTicks > cleanupThreshold) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (Long key : keysToRemove) {
            chunkSolidificationTime.remove(key);
            cachedSize--;
        }

        if (!keysToRemove.isEmpty()) {
            MainRegistry.LOGGER.debug("[SELLAFIT] Очищено старых записей: " + keysToRemove.size());
        }
    }
}