package com.hbm_m.blockentity.network.radio;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Port of {@code com.hbm.tileentity.network.RTTYSystem} (1.7.10 Original) - "Redstone Over Radio"
 * (RTTY). A flat, string-keyed, per-dimension pub/sub broadcast board: any block can broadcast to
 * or listen on an arbitrary channel name, with no range limit and no registration step. Delayed by
 * exactly one tick by design (write into {@link #newMessages}, flipped into the readable
 * {@link #broadcast} map once per server tick via {@link #updateBroadcastQueue()}).
 * <p>
 * SCOPE-Vereinfachung: Das Original haelt einen Bonus-"2012-08-06"-Kanal, der in jeder geladenen
 * Welt automatisch eine Melodie ("Song of Storms") sendet (Easter Egg fuer ein RTTY-Notenblock-
 * Geraet). Nicht portiert - rein kosmetisch, kein tragendes Feature.
 */
public final class RTTYNetwork {

    private RTTYNetwork() {}

    public record ChannelKey(ResourceKey<Level> dimension, String channel) {}

    /** Public frequency band for reading purposes, delayed by one tick. */
    private static final Map<ChannelKey, RttyChannel> BROADCAST = new HashMap<>();
    /** New message queue for writing, flipped into {@link #BROADCAST} once per tick. */
    private static final Map<ChannelKey, Object> NEW_MESSAGES = new HashMap<>();

    /** Pushes a new signal to be used next tick. Numeric signals pushed to the same channel in the same tick are summed. */
    public static void broadcast(Level level, String channelName, Object signal) {
        ChannelKey key = new ChannelKey(level.dimension(), channelName);

        Object existing = NEW_MESSAGES.get(key);
        if (existing != null && isNumber(signal) && isNumber(existing)) {
            try {
                long combined = Long.parseLong(String.valueOf(signal)) + Long.parseLong(String.valueOf(existing));
                NEW_MESSAGES.put(key, String.valueOf(combined));
                return;
            } catch (NumberFormatException ignored) {}
        }

        NEW_MESSAGES.put(key, signal);
    }

    /** Returns the RTTY channel with that name, or null. */
    public static RttyChannel listen(Level level, String channelName) {
        return BROADCAST.get(new ChannelKey(level.dimension(), channelName));
    }

    private static long lastProcessedTick = -1;

    /**
     * Moves all new messages into the readable broadcast map once per game tick. Safe to call
     * redundantly from every radio-torch block entity's own tick method (no dedicated global tick
     * event hook needed) - guarded so the actual flip only happens once per {@code gameTime} value.
     */
    public static void tickIfNeeded(long gameTime) {
        if (gameTime == lastProcessedTick) return;
        lastProcessedTick = gameTime;
        updateBroadcastQueue(gameTime);
    }

    /** Moves all new messages into the readable broadcast map with a timestamp, then clears the write queue. */
    private static void updateBroadcastQueue(long gameTime) {
        for (Entry<ChannelKey, Object> entry : NEW_MESSAGES.entrySet()) {
            RttyChannel channel = new RttyChannel();
            channel.timeStamp = gameTime;
            channel.signal = entry.getValue();
            BROADCAST.put(entry.getKey(), channel);
        }
        NEW_MESSAGES.clear();
    }

    private static boolean isNumber(Object o) {
        try {
            Long.parseLong(String.valueOf(o));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static class RttyChannel {
        /** The world's game-time at the moment of publishing (server tick PRE-phase). */
        public long timeStamp = -1;
        /** A signal can be anything: a plain number as a string, or an arbitrary encoded string. */
        public Object signal;
    }
}
