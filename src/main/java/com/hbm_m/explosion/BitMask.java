// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

/** Маска битов; реализации — {@link ConcurrentBitSet} и {@link OffHeapBitSet}. */
public interface BitMask {

    boolean get(int bit);

    void set(int bit);

    boolean getAndSet(int bit);

    int nextSetBit(int from);

    int nextClearBit(int from);

    int previousSetBit(int from);

    int previousClearBit(int from);

    boolean isEmpty();

    long cardinality();

    int length();

    int size();

    int logicalSize();

    long[] toLongArray();

    default void free() {}
}
