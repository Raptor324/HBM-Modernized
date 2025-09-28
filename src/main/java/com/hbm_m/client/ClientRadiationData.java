package com.hbm_m.client;
/**
 * Этот класс существует ТОЛЬКО НА КЛИЕНТЕ.
 * Он отвечает за хранение данных о радиации, полученных от сервера,
 * чтобы другие клиентские системы (например, дебаг рендер) могли их использовать.
 */
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientRadiationData {
    
    // Очищает все клиентские данные о радиации (например, при смене мира или измерения).
    
    public static void clearAll() {
        radiationByDimension.clear();
    }

    // Мы храним данные для каждого измерения отдельно.
    // Ключ - это ResourceLocation измерения, значение - карта радиации для этого измерения.
    private static final Map<ResourceLocation, Map<ChunkPos, Float>> radiationByDimension = new ConcurrentHashMap<>();

    /**
     * Вызывается из обработчика пакетов для обновления данных о радиации.
     * @param dimension Измерение, для которого пришли данные.
     * @param newData   Карта с новыми значениями радиации от сервера.
     */
    public static void updateRadiationData(ResourceLocation dimension, Map<ChunkPos, Float> newData) {
        // Получаем или создаем карту для конкретного измерения.
        Map<ChunkPos, Float> radiationLevels = radiationByDimension.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());

        for (Map.Entry<ChunkPos, Float> entry : newData.entrySet()) {
            // Если сервер прислал 0 (или очень маленькое значение), удаляем чанк из карты,
            // чтобы она не разрасталась бесконечно.
            if (entry.getValue() < 1e-6f) {
                radiationLevels.remove(entry.getKey());
            } else {
                radiationLevels.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Используется кодом рендеринга для получения уровня радиации в конкретном чанке.
     * @param dimension Измерение, в котором находится чанк.
     * @param pos       Позиция чанка.
     * @return Уровень радиации или 0, если данных нет.
     */
    public static float getRadiationForChunk(ResourceLocation dimension, ChunkPos pos) {
        Map<ChunkPos, Float> radiationLevels = radiationByDimension.get(dimension);
        if (radiationLevels == null) {
            return 0.0f;
        }
        return radiationLevels.getOrDefault(pos, 0.0f);
    }
}