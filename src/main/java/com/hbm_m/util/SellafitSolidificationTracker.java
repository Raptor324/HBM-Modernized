package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

public class SellafitSolidificationTracker {

    /**
     * Карта для отслеживания чанков и времени их загрузки
     * Ключ: позиция чанка (ChunkPos), Значение: время затвердевания в тиках
     */
    private static final Map<Long, Integer> chunkSolidificationTime = new HashMap<>();

    /**
     * Конфигурация затвердевания селлафита
     * Примерное время в тиках (1 тик = 50мс, 20 тиков = 1 сек)
     */
    private static final int SELLAFIT_SOLIDIFICATION_TICKS = 200; // 10 секунд
    private static final int SAFETY_MARGIN_TICKS = 20; // Дополнительный запас
    private static final int TOTAL_SOLIDIFICATION_TICKS = SELLAFIT_SOLIDIFICATION_TICKS + SAFETY_MARGIN_TICKS;

    /**
     * Регистрирует момент начала обработки чанка
     */
    public static void registerChunkStart(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        int currentTick = (int)(level.getGameTime() % Integer.MAX_VALUE);
        chunkSolidificationTime.put(chunkKey, currentTick);
    }

    /**
     * Проверяет, достаточно ли времени прошло для затвердевания селлафита в чанке
     */
    public static boolean isChunkSolidified(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        if (!chunkSolidificationTime.containsKey(chunkKey)) {
            return true; // Если не зарегистрирован, значит он уже твёрдый
        }

        int startTick = chunkSolidificationTime.get(chunkKey);
        int currentTick = (int)(level.getGameTime() % Integer.MAX_VALUE);
        int elapsedTicks = currentTick - startTick;
        if (elapsedTicks < 0) {
            elapsedTicks += Integer.MAX_VALUE; // Обработка переполнения
        }

        return elapsedTicks >= TOTAL_SOLIDIFICATION_TICKS;
    }

    /**
     * Получает оставшееся время затвердевания в тиках
     */
    public static int getRemainingTicks(ServerLevel level, int chunkX, int chunkZ) {
        long chunkKey = getChunkKey(chunkX, chunkZ);
        if (!chunkSolidificationTime.containsKey(chunkKey)) {
            return 0;
        }

        int startTick = chunkSolidificationTime.get(chunkKey);
        int currentTick = (int)(level.getGameTime() % Integer.MAX_VALUE);
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
        chunkSolidificationTime.remove(chunkKey);
    }

    /**
     * Вспомогательный метод для создания уникального ключа чанка
     */
    private static long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Получает позицию чанка из позиции блока
     */
    public static int[] getChunkCoordinates(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return new int[]{chunkX, chunkZ};
    }

    /**
     * Очищает все данные (используется при перезагрузке/выгрузке уровня)
     */
    public static void clearAll() {
        chunkSolidificationTime.clear();
    }
}