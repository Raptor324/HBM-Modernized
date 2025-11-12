package com.hbm_m.multiblock;

import net.minecraft.world.phys.AABB;
import com.hbm_m.main.MainRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Общий (common) реестр базовых AABB частей дверей.
 * Заполняется клиентом при bake модели и используется сервером для коллизий.
 * Thread-safe и работает на обеих сторонах.
 */
public final class DoorPartAABBRegistry {

    // doorId -> (partName -> базовый AABB в координатах контроллера)
    private static final Map<String, Map<String, AABB>> REGISTRY = new ConcurrentHashMap<>();

    /**
     * Регистрирует базовые AABB для двери (вызывается из клиентского DoorBakedModel).
     */
    public static synchronized void register(String doorId, Map<String, AABB> partAABBs) {
        if (partAABBs == null || partAABBs.isEmpty()) {
            MainRegistry.LOGGER.warn("DoorPartAABBRegistry: Attempted to register empty AABB map for doorId={}", doorId);
            return;
        }
        REGISTRY.put(doorId, Collections.unmodifiableMap(new HashMap<>(partAABBs)));
        MainRegistry.LOGGER.info("DoorPartAABBRegistry: Registered {} part AABBs for doorId={}", partAABBs.size(), doorId);
    }

    /**
     * Получает базовый AABB для конкретной части двери.
     * @return AABB или null если не зарегистрирован
     */
    public static AABB get(String doorId, String partName) {
        Map<String, AABB> parts = REGISTRY.get(doorId);
        return (parts != null) ? parts.get(partName) : null;
    }

    /**
     * Получает все зарегистрированные AABB для двери.
     * @return Неизменяемая карта partName -> AABB или пустая если не зарегистрирована
     */
    public static Map<String, AABB> getAll(String doorId) {
        Map<String, AABB> parts = REGISTRY.get(doorId);
        return (parts != null) ? parts : Collections.emptyMap();
    }

    /**
     * Проверяет, зарегистрирована ли дверь.
     */
    public static boolean contains(String doorId) {
        return REGISTRY.containsKey(doorId);
    }

    /**
     * Очистить реестр (для reload ресурсов).
     */
    public static void clear() {
        REGISTRY.clear();
    }

    private DoorPartAABBRegistry() {}
}
