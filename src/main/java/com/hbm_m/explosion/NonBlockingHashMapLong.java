// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;

/**
 * Минимальный конкурентный map с long-ключами над ConcurrentHashMap.
 * API-поверхность — ровно то, что использует движок взрыва MK5.
 */
public class NonBlockingHashMapLong<T> {

    private final ConcurrentHashMap<Long, T> backing;
    private final T defaultReturnValue;

    public NonBlockingHashMapLong(int expectedSize) {
        this.backing = new ConcurrentHashMap<>(Math.max(16, expectedSize));
        this.defaultReturnValue = null;
    }

    public T get(long key) {
        return backing.get(key);
    }

    public T put(long key, T value) {
        return backing.put(key, value);
    }

    public T putIfAbsent(long key, T value) {
        return backing.putIfAbsent(key, value);
    }

    public T remove(long key) {
        return backing.remove(key);
    }

    public T computeIfAbsent(long key, LongFunction<? extends T> mapping) {
        return backing.computeIfAbsent(key, (Long k) -> mapping.apply(k));
    }

    public boolean containsKey(long key) {
        return backing.containsKey(key);
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public int size() {
        return backing.size();
    }

    public void clear() {
        backing.clear();
    }

    /** Копия набора ключей; порядок произвольный. */
    public long[] keySetLong() {
        long[] out = new long[backing.size()];
        int i = 0;
        for (Long k : backing.keySet()) out[i++] = k;
        return out;
    }

    public Iterable<Long> keySet() {
        return backing.keySet();
    }

    public T defaultReturnValue() {
        return defaultReturnValue;
    }
}
