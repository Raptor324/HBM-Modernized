// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import static com.hbm_m.util.UnsafeHolder.U;
import static com.hbm_m.util.UnsafeHolder.offLong;

/**
 * Конкурентный битсет на long[]: set/get через volatile-чтение и CAS.
 * Используется движком MK5 как per-chunk маска разрушения — записи из рабочих
 * потоков ForkJoinPool, чтение с серверного потока без блокировок.
 */
public class ConcurrentBitSet implements BitMask {

    private final long[] words;
    private final int wordCount;
    private final int logicalSize;

    public ConcurrentBitSet(int logicalSize) {
        if (logicalSize < 0) throw new NegativeArraySizeException("logicalSize < 0: " + logicalSize);
        this.logicalSize = logicalSize;
        this.wordCount = (int) (((long) logicalSize + 63L) >>> 6);
        this.words = new long[wordCount];
    }

    @Override
    public boolean get(int bit) {
        if (bit < 0) throw new IndexOutOfBoundsException("bit < 0: " + bit);
        if (bit >= logicalSize) return false;
        int wordIndex = bit >>> 6;
        long mask = 1L << (bit & 63);
        long word = U.getLongVolatile(words, offLong(wordIndex));
        return (word & mask) != 0;
    }

    @Override
    public void set(int bit) {
        if (bit < 0 || bit >= logicalSize) return;
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            long newWord = oldWord | mask;
            if (oldWord == newWord) return;
            if (U.compareAndSwapLong(words, offset, oldWord, newWord)) return;
        }
    }

    @Override
    public boolean getAndSet(int bit) {
        if (bit < 0 || bit >= logicalSize)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wordIndex = bit >>> 6;
        long offset = offLong(wordIndex);
        long mask = 1L << (bit & 63);
        while (true) {
            long oldWord = U.getLongVolatile(words, offset);
            if ((oldWord & mask) != 0) return true;
            long newWord = oldWord | mask;
            if (U.compareAndSwapLong(words, offset, oldWord, newWord)) return false;
        }
    }

    @Override
    public int nextSetBit(int from) {
        if (from < 0) from = 0;
        int wordIndex = from >>> 6;
        if (wordIndex >= wordCount) return -1;
        long word = U.getLongVolatile(words, offLong(wordIndex)) & (~0L << (from & 63));
        while (true) {
            if (word != 0) {
                int idx = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return (idx < logicalSize) ? idx : -1;
            }
            wordIndex++;
            if (wordIndex >= wordCount) return -1;
            word = U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public int nextClearBit(int from) {
        if (from < 0) throw new IndexOutOfBoundsException("from < 0: " + from);
        if (from >= logicalSize) return from;
        int wordIndex = from >>> 6;
        if (wordIndex >= wordCount) return from;
        long word = ~U.getLongVolatile(words, offLong(wordIndex)) & (-1L << (from & 63));
        while (true) {
            if (word != 0) {
                int idx = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                return Math.min(idx, logicalSize);
            }
            wordIndex++;
            if (wordIndex >= wordCount) return logicalSize;
            word = ~U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public int previousSetBit(int from) {
        if (from < 0) return -1;
        if (from >= logicalSize) from = logicalSize - 1;
        if (from < 0) return -1;
        int wordIndex = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = U.getLongVolatile(words, offLong(wordIndex)) & mask;
        while (true) {
            if (word != 0) return (wordIndex << 6) + (63 - Long.numberOfLeadingZeros(word));
            wordIndex--;
            if (wordIndex < 0) return -1;
            word = U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public int previousClearBit(int from) {
        if (from < 0) return -1;
        if (from >= logicalSize) from = logicalSize - 1;
        if (from < 0) return -1;
        int wordIndex = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = ~U.getLongVolatile(words, offLong(wordIndex)) & mask;
        while (true) {
            if (word != 0) return (wordIndex << 6) + (63 - Long.numberOfLeadingZeros(word));
            wordIndex--;
            if (wordIndex < 0) return -1;
            word = ~U.getLongVolatile(words, offLong(wordIndex));
        }
    }

    @Override
    public boolean isEmpty() {
        if (wordCount == 0) return true;
        for (int i = 0; i < wordCount; i++) {
            if (U.getLongVolatile(words, offLong(i)) != 0L) return false;
        }
        return true;
    }

    @Override
    public long cardinality() {
        long total = 0L;
        for (int i = 0; i < wordCount; i++) {
            total += Long.bitCount(U.getLongVolatile(words, offLong(i)));
        }
        return total;
    }

    @Override
    public int length() {
        if (logicalSize == 0) return 0;
        int maxWord = (logicalSize - 1) >>> 6;
        for (int i = maxWord; i >= 0; i--) {
            long w = U.getLongVolatile(words, offLong(i));
            if (w != 0L) {
                return (i << 6) + (64 - Long.numberOfLeadingZeros(w));
            }
        }
        return 0;
    }

    @Override
    public int size() {
        return (int) Math.min(((long) wordCount) << 6, Integer.MAX_VALUE);
    }

    @Override
    public int logicalSize() {
        return logicalSize;
    }

    @Override
    public long[] toLongArray() {
        int len = length();
        if (len == 0) return new long[0];

        int used = (int) (((long) len + 63L) >>> 6);
        long[] out = new long[used];
        for (int i = 0; i < used; i++) {
            out[i] = U.getLongVolatile(words, offLong(i));
        }
        return out;
    }
}
