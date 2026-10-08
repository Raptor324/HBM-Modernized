// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.DirtyChunkTracker), Commit 3f9a261a.

package com.hbm_m.radiation.ntmnext;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;

/**
 * Epochen-Hashtabelle "Chunk -> Bitmaske schmutziger Sektionen" (offene Adressierung).
 * {@link #reset()} leert in O(1) ueber den Epochenzaehler.
 */
final class DirtyChunkTracker {
    static final float LOAD_FACTOR = 0.6f;
    long[] keys;
    int[] ids;
    int[] masks;
    int[] stamps;
    int[] slots;
    int mask, size, epoch, slotSize;
    final int wordsPerChunk;

    DirtyChunkTracker(int expectedChunks) {
        this(expectedChunks, 1);
    }

    DirtyChunkTracker(int expectedChunks, int wordsPerChunk) {
        this.wordsPerChunk = wordsPerChunk;
        int cap = HashCommon.nextPowerOfTwo(Math.max(16, (int) (expectedChunks / LOAD_FACTOR) + 1));
        keys = new long[cap];
        ids = new int[cap];
        masks = new int[cap * wordsPerChunk];
        stamps = new int[cap];
        Arrays.fill(keys, Long.MIN_VALUE);
        Arrays.fill(ids, -1);
        mask = cap - 1;
        slots = new int[Math.max(16, expectedChunks)];
        epoch = 1;
    }

    /** Markiert eine einzelne Sektion (slot) des Chunks. */
    void add(long ck, int id, int slot) {
        if (wordsPerChunk == 1) addCompact(ck, id, slot);
        else addWide(ck, id, slot);
    }

    private void addCompact(long ck, int id, int slot) {
        int bit = 1 << slot;
        if (size + 1 > (int) (keys.length * LOAD_FACTOR))
            rehash(HashCommon.nextPowerOfTwo(keys.length + (keys.length >>> 1) + 16));
        int pos = SectionKeys.hash(ck) & mask;
        while (true) {
            if (stamps[pos] != epoch) {
                stamps[pos] = epoch;
                keys[pos] = ck;
                ids[pos] = id;
                masks[pos] = bit;
                size++;
                int i = slotSize;
                if (i == slots.length)
                    slots = Arrays.copyOf(slots, slots.length + (slots.length >>> 1) + 16);
                slots[i] = pos;
                slotSize = i + 1;
                return;
            }
            if (keys[pos] == ck) {
                masks[pos] |= bit;
                ids[pos] = id;
                return;
            }
            pos = (pos + 1) & mask;
        }
    }

    private void addWide(long ck, int id, int slot) {
        int bit = 1 << (slot & 31);
        if (size + 1 > (int) (keys.length * LOAD_FACTOR))
            rehash(HashCommon.nextPowerOfTwo(keys.length + (keys.length >>> 1) + 16));
        int pos = SectionKeys.hash(ck) & mask;
        while (true) {
            if (stamps[pos] != epoch) {
                stamps[pos] = epoch;
                keys[pos] = ck;
                ids[pos] = id;
                int base = pos * wordsPerChunk;
                Arrays.fill(masks, base, base + wordsPerChunk, 0);
                masks[base + (slot >>> 5)] = bit;
                size++;
                int i = slotSize;
                if (i == slots.length)
                    slots = Arrays.copyOf(slots, slots.length + (slots.length >>> 1) + 16);
                slots[i] = pos;
                slotSize = i + 1;
                return;
            }
            if (keys[pos] == ck) {
                masks[pos * wordsPerChunk + (slot >>> 5)] |= bit;
                ids[pos] = id;
                return;
            }
            pos = (pos + 1) & mask;
        }
    }

    /** Markiert alle Sektionen des Chunks. */
    void add(long ck, int id) {
        if (wordsPerChunk == 1) addCompact(ck, id);
        else addWide(ck, id);
    }

    private void addCompact(long ck, int id) {
        if (size + 1 > (int) (keys.length * LOAD_FACTOR))
            rehash(HashCommon.nextPowerOfTwo(keys.length + (keys.length >>> 1) + 16));
        int pos = SectionKeys.hash(ck) & mask;
        while (true) {
            if (stamps[pos] != epoch) {
                stamps[pos] = epoch;
                keys[pos] = ck;
                ids[pos] = id;
                masks[pos] = -1;
                size++;
                int i = slotSize;
                if (i == slots.length)
                    slots = Arrays.copyOf(slots, slots.length + (slots.length >>> 1) + 16);
                slots[i] = pos;
                slotSize = i + 1;
                return;
            }
            if (keys[pos] == ck) {
                masks[pos] = -1;
                ids[pos] = id;
                return;
            }
            pos = (pos + 1) & mask;
        }
    }

    private void addWide(long ck, int id) {
        if (size + 1 > (int) (keys.length * LOAD_FACTOR))
            rehash(HashCommon.nextPowerOfTwo(keys.length + (keys.length >>> 1) + 16));
        int pos = SectionKeys.hash(ck) & mask;
        while (true) {
            if (stamps[pos] != epoch) {
                stamps[pos] = epoch;
                keys[pos] = ck;
                ids[pos] = id;
                Arrays.fill(masks, pos * wordsPerChunk, (pos + 1) * wordsPerChunk, -1);
                size++;
                int i = slotSize;
                if (i == slots.length)
                    slots = Arrays.copyOf(slots, slots.length + (slots.length >>> 1) + 16);
                slots[i] = pos;
                slotSize = i + 1;
                return;
            }
            if (keys[pos] == ck) {
                Arrays.fill(masks, pos * wordsPerChunk, (pos + 1) * wordsPerChunk, -1);
                ids[pos] = id;
                return;
            }
            pos = (pos + 1) & mask;
        }
    }

    int getMask(long ck) {
        return getMask(ck, 0);
    }

    int getMask(long ck, int word) {
        int pos = SectionKeys.hash(ck) & mask;
        int e = epoch;
        while (true) {
            if (stamps[pos] != e) return 0;
            if (keys[pos] == ck) return masks[pos * wordsPerChunk + word];
            pos = (pos + 1) & mask;
        }
    }

    void reset() {
        size = 0;
        slotSize = 0;

        int e = epoch + 1;
        if (e == 0) {
            Arrays.fill(stamps, 0);
            e = 1;
        }
        epoch = e;
    }

    void clearAll() {
        Arrays.fill(keys, Long.MIN_VALUE);
        Arrays.fill(ids, -1);
        Arrays.fill(masks, 0);
        Arrays.fill(stamps, 0);
        size = 0;
        slotSize = 0;
        epoch = 1;
    }

    void rehash(int newCap) {
        long[] newKeys = new long[newCap];
        int[] newIds = new int[newCap];
        int[] newMasks = new int[newCap * wordsPerChunk];
        int[] newStamps = new int[newCap];
        Arrays.fill(newKeys, Long.MIN_VALUE);
        Arrays.fill(newIds, -1);
        int newMask = newCap - 1;

        int[] newSlots = new int[Math.max(slots.length, slotSize)];
        int ns = 0;

        for (int i = 0; i < slotSize; i++) {
            int oldPos = slots[i];
            if (stamps[oldPos] != epoch) continue;
            long ck = keys[oldPos];
            int id = ids[oldPos];

            int pos = SectionKeys.hash(ck) & newMask;
            while (newStamps[pos] == epoch) pos = (pos + 1) & newMask;

            newStamps[pos] = epoch;
            newKeys[pos] = ck;
            newIds[pos] = id;
            System.arraycopy(
                    masks, oldPos * wordsPerChunk, newMasks, pos * wordsPerChunk, wordsPerChunk);
            newSlots[ns++] = pos;
        }

        keys = newKeys;
        ids = newIds;
        masks = newMasks;
        stamps = newStamps;
        mask = newMask;
        slots = newSlots;
        slotSize = ns;
        size = ns;
    }
}
