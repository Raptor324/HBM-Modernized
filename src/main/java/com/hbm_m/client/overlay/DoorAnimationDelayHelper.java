package com.hbm_m.client.overlay;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.client.render.DoorChunkInvalidationHelper;

/**
 * Client-only helper for door animation delay (overlap period between BER and baked model).
 * Kept separate from DoorBlockEntity so the server never loads client-only code.
 */
public final class DoorAnimationDelayHelper {

    private static final long ANIMATION_DELAY_MS = 500;

    private static final List<DelayEntry> QUEUE = new ArrayList<>();
    private static final Map<DoorBlockEntity, Long> DELAY_UNTIL_MAP = new ConcurrentHashMap<>();

    private DoorAnimationDelayHelper() {}

    private record DelayEntry(long untilMs, WeakReference<DoorBlockEntity> ref) {}

    /** Called from DoorBlockEntity.load() when on client and transitioning from moving to static. */
    public static void addToQueue(DoorBlockEntity be, long delayMs) {
        long untilMs = System.currentTimeMillis() + delayMs;
        DELAY_UNTIL_MAP.put(be, untilMs);
        QUEUE.add(new DelayEntry(untilMs, new WeakReference<>(be)));
    }

    /** Called from ClientTickEvent. Processes expired entries. */
    public static void processQueue() {
        long now = System.currentTimeMillis();
        Iterator<DelayEntry> it = QUEUE.iterator();
        while (it.hasNext()) {
            DelayEntry entry = it.next();
            if (now >= entry.untilMs) {
                DoorBlockEntity be = entry.ref.get();
                if (be != null && !be.isRemoved() && be.getLevel() != null) {
                    DELAY_UNTIL_MAP.remove(be);
                    be.clearAnimationDelayClient();
                    DoorChunkInvalidationHelper.scheduleChunkInvalidation(be.getBlockPos());
                }
                it.remove();
            }
        }
    }

    /** Returns true if this entity is in the overlap delay period. */
    public static boolean isInDelayPeriod(DoorBlockEntity be) {
        Long untilMs = DELAY_UNTIL_MAP.get(be);
        return untilMs != null && System.currentTimeMillis() < untilMs;
    }
}
