// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.util.ObjectPool, com.hbm.lib.TLPool,
// com.hbm.lib.queues.MpscUnboundedXaddArrayLongQueue), Commit 3f9a261a.
// Die Unsafe-/JCTools-Varianten des Originals sind durch einfache JDK-17-Strukturen ersetzt
// (gleiche Semantik: Pool mit Reset, MPSC-Long-Queue mit EMPTY-Sentinel).

package com.hbm_m.radiation.ntmnext;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class Pools {

    private Pools() {}

    /** Einfacher, nicht threadsicherer Objektpool (nur Server-Thread bzw. waehrend der Sim). */
    static final class ObjectPool<T> {
        private final Object[] pool;
        private final Supplier<? extends T> factory;
        private final Consumer<? super T> reset;
        private int size;

        ObjectPool(Supplier<? extends T> factory, Consumer<? super T> reset, int cap) {
            this.factory = Objects.requireNonNull(factory);
            this.reset = Objects.requireNonNull(reset);
            if (cap < 1) throw new IllegalArgumentException("cap must be >= 1");
            this.pool = new Object[cap];
        }

        @SuppressWarnings("unchecked")
        T borrow() {
            if (size != 0) {
                int index = --size;
                T t = (T) pool[index];
                pool[index] = null;
                return t;
            }
            T t = factory.get();
            if (t == null) throw new NullPointerException();
            return t;
        }

        void recycle(T t) {
            if (t == null) throw new NullPointerException();
            reset.accept(t);
            if (size == pool.length) return;
            pool[size++] = t;
        }

        void clear() {
            Arrays.fill(pool, 0, size, null);
            size = 0;
        }
    }

    /**
     * Threadsicherer Pool (ersetzt TLPool). Wird parallel aus den Rebuild-Tasks benutzt.
     * Ein ThreadLocal-Vorcache wie im Original ist fuer die Pocket-Arrays nicht noetig.
     */
    static final class ConcurrentPool<T> {
        private final ConcurrentLinkedQueue<T> shared = new ConcurrentLinkedQueue<>();
        private final AtomicInteger size = new AtomicInteger();
        private final Supplier<? extends T> factory;
        private final Consumer<? super T> reset;
        private final int cap;

        ConcurrentPool(Supplier<? extends T> factory, Consumer<? super T> reset, int cap) {
            this.factory = Objects.requireNonNull(factory);
            this.reset = Objects.requireNonNull(reset);
            this.cap = cap;
        }

        T borrow() {
            T t = shared.poll();
            if (t != null) {
                size.decrementAndGet();
                return t;
            }
            t = factory.get();
            if (t == null) throw new NullPointerException();
            return t;
        }

        void recycle(T t) {
            if (t == null) throw new NullPointerException();
            reset.accept(t);
            if (size.incrementAndGet() <= cap) shared.offer(t);
            else size.decrementAndGet();
        }
    }

    /**
     * Mehr-Produzenten-Long-Queue mit {@link #EMPTY} als Leer-Sentinel. Produzenten sind die
     * Sim-Worker (Nebel/Zerstoerung), Konsument ist der Server-Thread.
     */
    static final class LongQueue {
        static final long EMPTY = Long.MIN_VALUE;

        private final LongArrayFIFOQueue queue;

        LongQueue(int initialCapacity) {
            this.queue = new LongArrayFIFOQueue(initialCapacity);
        }

        boolean offer(long v) {
            if (v == EMPTY) {
                throw new IllegalArgumentException("Long.MIN_VALUE is reserved as EMPTY sentinel");
            }
            synchronized (queue) {
                queue.enqueue(v);
            }
            return true;
        }

        long poll() {
            synchronized (queue) {
                return queue.isEmpty() ? EMPTY : queue.dequeueLong();
            }
        }

        /** {@code trim} gibt wie im Original den Speicher frei. */
        void clear(boolean trim) {
            synchronized (queue) {
                queue.clear();
                if (trim) queue.trim();
            }
        }
    }
}
