package com.hbm_m.client.render.retained.alloc;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.BitSet;

/**
 * High-performance, O(1) segmented slab allocator managing persistent instance slots.
 * Features automatic geometric slab reallocation.
 *
 * <p>The GL backing/compacted buffers this class used to own were dead VRAM:
 * nothing bound or drew them - real rendering streams through
 * {@code InstancedStaticPartRenderer}. Only the slot bookkeeping (bitmap +
 * posKey mappings) remains.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class InstanceSlotAllocator {

    public static final int RECORD_STRIDE_FLOATS = 32;
    public static final int RECORD_STRIDE_BYTES = RECORD_STRIDE_FLOATS * 4; // 128 bytes
    public static final int INITIAL_CAPACITY = 65536;                        // slots
    public static final int MAX_ALLOWED_CAPACITY = 1048576;                  // slots

    private int currentCapacity;
    private final int maxCapacity;
    private BitSet allocatedBitmap;
    private int highWaterMark = 0;

    private final Long2IntOpenHashMap posKeyToSlot = new Long2IntOpenHashMap();
    private long[] slotToPosKey;

    public InstanceSlotAllocator() {
        this(INITIAL_CAPACITY, MAX_ALLOWED_CAPACITY);
    }

    public InstanceSlotAllocator(int initialCapacity) {
        this(initialCapacity, MAX_ALLOWED_CAPACITY);
    }

    public InstanceSlotAllocator(int initialCapacity, int maxCapacity) {
        this.currentCapacity = initialCapacity;
        this.maxCapacity = maxCapacity;
        this.allocatedBitmap = new BitSet(initialCapacity);
        this.posKeyToSlot.defaultReturnValue(-1);
        this.slotToPosKey = new long[initialCapacity];
        java.util.Arrays.fill(this.slotToPosKey, Long.MIN_VALUE);
    }

    /**
     * Allocates a persistent instance slot.
     * R4-3.2: the lowest free slot is reused first (bitmap bitscan) instead of a
     * FIFO queue - cyclic create/remove churn no longer walks the allocator up
     * towards ever-higher indices and keeps {@link #highWaterMark} dense.
     */
    public synchronized int allocateSlot() {
        int slot = allocatedBitmap.nextClearBit(0);
        if (slot >= currentCapacity) {
            if (currentCapacity >= maxCapacity) {
                return -1;
            }
            growCapacity();
            slot = allocatedBitmap.nextClearBit(0);
            if (slot >= maxCapacity) {
                return -1;
            }
        }
        if (slot >= maxCapacity) {
            return -1;
        }
        allocatedBitmap.set(slot);
        if (slot >= highWaterMark) {
            highWaterMark = slot + 1;
        }
        return slot;
    }

    /**
     * O(1) persistent slot allocation keyed by 64-bit block position.
     * Returns existing slot if already allocated, otherwise takes the lowest free slot.
     */
    public synchronized int allocateSlot(long posKey) {
        int existing = posKeyToSlot.get(posKey);
        if (existing >= 0) {
            return existing;
        }
        int slot = allocateSlot();
        if (slot < 0) {
            return -1;
        }
        posKeyToSlot.put(posKey, slot);
        if (slotToPosKey != null) {
            if (slot >= slotToPosKey.length) {
                int oldLen = slotToPosKey.length;
                slotToPosKey = java.util.Arrays.copyOf(slotToPosKey, currentCapacity);
                java.util.Arrays.fill(slotToPosKey, oldLen, slotToPosKey.length, Long.MIN_VALUE);
            }
            slotToPosKey[slot] = posKey;
        }
        return slot;
    }

    /**
     * Returns the persistent slot assigned to posKey, or -1 if not allocated.
     */
    public synchronized int getSlot(long posKey) {
        return posKeyToSlot.get(posKey);
    }

    /**
     * O(1) test whether posKey possesses an active persistent slot.
     */
    public synchronized boolean hasSlot(long posKey) {
        return posKeyToSlot.containsKey(posKey);
    }

    /**
     * O(1) deallocation of persistent slot associated with posKey.
     */
    public synchronized void freeSlot(long posKey) {
        int slot = posKeyToSlot.remove(posKey);
        if (slot >= 0) {
            if (slotToPosKey != null && slot < slotToPosKey.length) {
                slotToPosKey[slot] = Long.MIN_VALUE;
            }
            freeSlot(slot);
        }
    }

    /**
     * Deallocates a persistent instance slot in O(1).
     */
    public synchronized void freeSlot(int slot) {
        if (slot < 0 || slot >= currentCapacity) return;

        if (allocatedBitmap.get(slot)) {
            allocatedBitmap.clear(slot);
            if (slotToPosKey != null && slot < slotToPosKey.length && slotToPosKey[slot] != Long.MIN_VALUE) {
                posKeyToSlot.remove(slotToPosKey[slot]);
                slotToPosKey[slot] = Long.MIN_VALUE;
            }
            if (slot == highWaterMark - 1) {
                int prev = allocatedBitmap.previousSetBit(slot - 1);
                highWaterMark = (prev >= 0) ? prev + 1 : 0;
            }
        }
    }

    /**
     * R4-2.3: re-points the keyed slot mapping of {@code posKey} after the instance
     * buffer records were reordered (opaque/fading partition). Only existing keyed
     * entries are touched; anonymous (BER-path) slots are unaffected.
     */
    public synchronized void remapKeyedSlot(long posKey, int newSlot) {
        int old = posKeyToSlot.get(posKey);
        if (old < 0 || old == newSlot) return;
        if (newSlot < 0 || newSlot >= currentCapacity) return;
        posKeyToSlot.put(posKey, newSlot);
        if (slotToPosKey != null) {
            if (old < slotToPosKey.length && slotToPosKey[old] == posKey) {
                slotToPosKey[old] = Long.MIN_VALUE;
            }
            if (newSlot < slotToPosKey.length) {
                slotToPosKey[newSlot] = posKey;
            }
        }
    }

    private void growCapacity() {
        int oldCapacity = currentCapacity;
        int newCapacity = Math.min(oldCapacity * 2, maxCapacity);
        if (newCapacity <= oldCapacity) {
            throw new IllegalStateException("Instance slot hard ceiling reached: " + maxCapacity + " slots");
        }

        BitSet newBitmap = new BitSet(newCapacity);
        newBitmap.or(allocatedBitmap);
        this.allocatedBitmap = newBitmap;

        if (slotToPosKey != null) {
            this.slotToPosKey = java.util.Arrays.copyOf(this.slotToPosKey, newCapacity);
            java.util.Arrays.fill(this.slotToPosKey, oldCapacity, newCapacity, Long.MIN_VALUE);
        }
        this.currentCapacity = newCapacity;
    }

    public synchronized void reset() {
        allocatedBitmap.clear();
        highWaterMark = 0;
        posKeyToSlot.clear();
        if (slotToPosKey != null) {
            java.util.Arrays.fill(slotToPosKey, Long.MIN_VALUE);
        }
    }

    public synchronized void destroy() {
        // No GL resources left to free (the backing/compacted buffers were dead
        // VRAM and are gone) - only the slot bookkeeping is reset.
        reset();
    }

    public synchronized boolean isSlotAllocated(int slot) {
        if (slot < 0 || slot >= currentCapacity) return false;
        return allocatedBitmap.get(slot);
    }

    public int getCurrentCapacity() { return currentCapacity; }
    public int getHighWaterMark() { return highWaterMark; }
}
