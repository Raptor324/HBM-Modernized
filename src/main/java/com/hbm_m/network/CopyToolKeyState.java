package com.hbm_m.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверное per-player состояние зажатых клавиш копирки (ALT/CTRL) - порт
 * {@code HbmPlayerProps.keyPressed} из 1.7.10: клиент репортит состояние по фронту
 * (см. {@link CopyToolKeyPacket}), сервер читает его из {@code inventoryTick} предмета
 * и при вставке в трубы.
 */
public class CopyToolKeyState {

    public static final class Keys {
        public volatile boolean alt, ctrl;
    }

    private static final Map<UUID, Keys> STATE = new ConcurrentHashMap<>();

    /**
     * Дебаунс листания индекса вставки (оригинальный {@code inputDelay} на стеке):
     * держится на сервере, чтобы не грязнить DataComponents предмета каждый тик.
     */
    private static final Map<UUID, Integer> CYCLE_COOLDOWN = new ConcurrentHashMap<>();

    public static Keys get(UUID player) {
        return STATE.computeIfAbsent(player, k -> new Keys());
    }

    public static void set(UUID player, boolean alt, boolean ctrl) {
        Keys k = get(player);
        k.alt = alt;
        k.ctrl = ctrl;
    }

    public static int getCooldown(UUID player) {
        return CYCLE_COOLDOWN.getOrDefault(player, 0);
    }

    public static void setCooldown(UUID player, int ticks) {
        CYCLE_COOLDOWN.put(player, ticks);
    }

    public static void clear(UUID player) {
        STATE.remove(player);
        CYCLE_COOLDOWN.remove(player);
    }
}
