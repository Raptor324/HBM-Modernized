// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import static com.hbm_m.util.UnsafeHolder.U;

/**
 * Битсет вне кучи: маски субчанков (4096 бит = 512 Б) при огромных кратерах
 * исчисляются сотнями тысяч — off-heap исключает и расход heap, и GC-давление.
 * Требует явного {@link #free()}.
 */
public final class OffHeapBitSet implements BitMask, Cloneable, AutoCloseable {

    private final int logicalSizeLocal;
    private final int wordCountLocal;
    private long addr;
    private long bitCount;
    private boolean closed;

    public OffHeapBitSet(int logicalSize) {
        if (logicalSize < 0) throw new NegativeArraySizeException("logicalSize < 0: " + logicalSize);
        this.logicalSizeLocal = logicalSize;
        this.wordCountLocal = (int) (((long) logicalSize + 63L) >>> 6);
        long bytes = ((long) wordCountLocal) << 3;
        long p = U.allocateMemory(bytes);
        try {
            if (bytes != 0L) U.setMemory(p, bytes, (byte) 0);
        } catch (RuntimeException | Error failure) {
            if (p != 0L) U.freeMemory(p);
            throw failure;
        }
        this.addr = p;
        this.bitCount = 0L;
    }

    @Override
    public void free() {
        close();
    }

    @Override
    public void close() {
        if (closed) return;
        long p = addr;
        addr = 0L;
        closed = true;
        if (p != 0L) U.freeMemory(p);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("OffHeapBitSet is closed");
    }

    private long wordAddr(int index) {
        ensureOpen();
        return addr + (((long) index) << 3);
    }

    private long getWord(int index) {
        return U.getLong(wordAddr(index));
    }

    private void setWord(int index, long v) {
        U.putLong(wordAddr(index), v);
    }

    @Override
    public boolean get(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal) return false;
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        return (getWord(wi) & mask) != 0L;
    }

    @Override
    public void set(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal) return;
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        long old = getWord(wi);
        if ((old & mask) != 0L) return;
        setWord(wi, old | mask);
        bitCount++;
    }

    @Override
    public boolean getAndSet(int bit) {
        ensureOpen();
        if (bit < 0 || bit >= logicalSizeLocal)
            throw new IndexOutOfBoundsException("bit index out of bounds: " + bit);
        int wi = bit >>> 6;
        long mask = 1L << (bit & 63);
        long old = getWord(wi);
        if ((old & mask) != 0L) return true;
        setWord(wi, old | mask);
        bitCount++;
        return false;
    }

    @Override
    public int nextSetBit(int from) {
        ensureOpen();
        if (from < 0) from = 0;
        int wi = from >>> 6;
        if (wi >= wordCountLocal) return -1;
        long word = getWord(wi) & (~0L << (from & 63));
        while (true) {
            if (word != 0L) {
                int idx = (wi << 6) + Long.numberOfTrailingZeros(word);
                return idx < logicalSizeLocal ? idx : -1;
            }
            wi++;
            if (wi >= wordCountLocal) return -1;
            word = getWord(wi);
        }
    }

    @Override
    public int nextClearBit(int from) {
        ensureOpen();
        if (from < 0) throw new IndexOutOfBoundsException("from < 0: " + from);
        if (from >= logicalSizeLocal) return from;
        int wi = from >>> 6;
        if (wi >= wordCountLocal) return from;
        long word = ~getWord(wi) & (-1L << (from & 63));
        while (true) {
            if (word != 0L) {
                int idx = (wi << 6) + Long.numberOfTrailingZeros(word);
                return Math.min(idx, logicalSizeLocal);
            }
            wi++;
            if (wi >= wordCountLocal) return logicalSizeLocal;
            word = ~getWord(wi);
        }
    }

    @Override
    public int previousSetBit(int from) {
        ensureOpen();
        if (from < 0) return -1;
        if (from >= logicalSizeLocal) from = logicalSizeLocal - 1;
        if (from < 0) return -1;
        int wi = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = getWord(wi) & mask;
        while (true) {
            if (word != 0L) return (wi << 6) + (63 - Long.numberOfLeadingZeros(word));
            wi--;
            if (wi < 0) return -1;
            word = getWord(wi);
        }
    }

    @Override
    public int previousClearBit(int from) {
        ensureOpen();
        if (from < 0) return -1;
        if (from >= logicalSizeLocal) from = logicalSizeLocal - 1;
        if (from < 0) return -1;
        int wi = from >>> 6;
        long mask = ~0L >>> (63 - (from & 63));
        long word = ~getWord(wi) & mask;
        while (true) {
            if (word != 0L) return (wi << 6) + (63 - Long.numberOfLeadingZeros(word));
            wi--;
            if (wi < 0) return -1;
            word = ~getWord(wi);
        }
    }

    @Override
    public boolean isEmpty() {
        ensureOpen();
        for (int i = 0; i < wordCountLocal; i++) {
            if (getWord(i) != 0L) return false;
        }
        return true;
    }

    @Override
    public long cardinality() {
        ensureOpen();
        return bitCount;
    }

    @Override
    public int length() {
        ensureOpen();
        for (int i = wordCountLocal - 1; i >= 0; i--) {
            long w = getWord(i);
            if (w != 0L) return (i << 6) + (64 - Long.numberOfLeadingZeros(w));
        }
        return 0;
    }

    @Override
    public int size() {
        return (int) Math.min(((long) wordCountLocal) << 6, Integer.MAX_VALUE);
    }

    @Override
    public int logicalSize() {
        return logicalSizeLocal;
    }

    @Override
    public long[] toLongArray() {
        ensureOpen();
        long[] out = new long[wordCountLocal];
        for (int i = 0; i < wordCountLocal; i++) out[i] = getWord(i);
        return out;
    }
}
