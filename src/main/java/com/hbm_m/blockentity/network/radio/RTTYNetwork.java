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
 * Wie im Original sendet der Kanal "2012-08-06" in jeder Welt fortlaufend "Song of Storms"
 * ({@link com.hbm_m.util.NoteBuilder#getTestSender}); Zeitstempel = Weltzeit - 1 wie nach dem Flip im Original.
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
        if (TEST_CHANNEL.equals(channelName)) {
            // Original updateBroadcastQueue: je Welt ein frischer Kanal mit getTestSender(timeStamp)
            RttyChannel chan = new RttyChannel();
            chan.timeStamp = level.getGameTime() - 1;
            chan.signal = com.hbm_m.util.NoteBuilder.getTestSender(chan.timeStamp);
            return chan;
        }
        return BROADCAST.get(new ChannelKey(level.dimension(), channelName));
    }

    public static final String TEST_CHANNEL = "2012-08-06";

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

    /**
     * Original {@code selfdestruct} von Empfaenger und Steuerung: Block weg, dann ExplosionVNT(5) ohne Blockschaden,
     * {@code EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F)}, {@code ExplosionEffectWeapon(10, 2.5F, 1F)}.
     */
    public static void selfDestruct(Level level, net.minecraft.core.BlockPos pos) {
        level.destroyBlock(pos, false);
        com.hbm_m.explosion.vanillant.ExplosionVNT vnt = new com.hbm_m.explosion.vanillant.ExplosionVNT(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, null);
        vnt.setEntityProcessor(new com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
        vnt.setPlayerProcessor(new com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard());
        vnt.setSFX(new com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    public static class RttyChannel {
        /** The world's game-time at the moment of publishing (server tick PRE-phase). */
        public long timeStamp = -1;
        /** A signal can be anything: a plain number as a string, or an arbitrary encoded string. */
        public Object signal;
    }
}
