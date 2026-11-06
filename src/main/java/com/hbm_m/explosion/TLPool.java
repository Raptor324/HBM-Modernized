// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Пул объектов с thread-local кэшем и общим MPSC-хвостом.
 * Рабочие потоки взрыва переиспользуют аккумуляторы/списки вместо аллокаций.
 */
public final class TLPool<T> {

    private final ThreadLocal<LocalCache<T>> local;
    private final ConcurrentLinkedQueue<T> shared;
    private final Supplier<? extends T> factory;
    private final Consumer<? super T> reset;
    private final int localCap;

    public TLPool(Supplier<? extends T> factory, Consumer<? super T> reset, int localCap, int sharedCap) {
        this.factory = Objects.requireNonNull(factory);
        this.reset = Objects.requireNonNull(reset);
        if (localCap < 0) throw new IllegalArgumentException("localCap must be >= 0");
        this.localCap = localCap;
        this.shared = new ConcurrentLinkedQueue<>();
        this.local = ThreadLocal.withInitial(() -> new LocalCache<>(Math.max(0, localCap - 1)));
    }

    public T borrow() {
        LocalCache<T> q = local.get();
        T t = q.poll();
        if (t != null) return t;

        int moved = 0;
        while (moved < localCap) {
            T s = shared.poll();
            if (s == null) break;
            q.add(s);
            moved++;
        }

        t = q.poll();
        return t != null ? t : newInstance();
    }

    public void recycle(T t) {
        if (t == null) throw new NullPointerException();
        reset.accept(t);
        LocalCache<T> q = local.get();
        if (q.add(t)) return;
        shared.offer(t);
    }

    private T newInstance() {
        T value = factory.get();
        if (value == null) throw new NullPointerException();
        return value;
    }

    public void clearLocal() {
        local.remove();
    }

    private static final class LocalCache<T> {
        private final Object[] elements;
        private T top;
        private int size;

        private LocalCache(int capacity) {
            elements = new Object[Math.max(0, capacity)];
        }

        boolean add(T value) {
            if (top == null) {
                top = value;
                return true;
            }
            if (size == elements.length) return false;
            elements[size++] = top;
            top = value;
            return true;
        }

        @SuppressWarnings("unchecked")
        T poll() {
            T value = top;
            if (value == null) return null;
            if (size == 0) {
                top = null;
            } else {
                int index = --size;
                top = (T) elements[index];
                elements[index] = null;
            }
            return value;
        }
    }
}
