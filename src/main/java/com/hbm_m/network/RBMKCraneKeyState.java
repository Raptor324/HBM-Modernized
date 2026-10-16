package com.hbm_m.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side per-player crane held-key state, matching the original's use of a synced
 * {@code HbmPlayerProps} capability: the client reports which crane-move keys it's holding
 * (see {@link RBMKCraneControlPacket}), and any {@code RBMKCraneConsoleBlockEntity} whose
 * detection AABB currently contains that player reads this state during its own tick.
 */
public class RBMKCraneKeyState {

    public static final class Keys {
        public volatile boolean up, down, left, right, load;
    }

    private static final Map<UUID, Keys> STATE = new ConcurrentHashMap<>();

    public static Keys get(UUID player) {
        return STATE.computeIfAbsent(player, k -> new Keys());
    }

    public static void set(UUID player, boolean up, boolean down, boolean left, boolean right, boolean load) {
        Keys k = get(player);
        k.up = up; k.down = down; k.left = left; k.right = right; k.load = load;
    }

    public static void clear(UUID player) {
        STATE.remove(player);
    }
}
