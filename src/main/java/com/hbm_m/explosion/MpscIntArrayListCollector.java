// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.concurrent.atomic.AtomicReference;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * MPSC-коллектор int'ов: индексы приостановленных лучей, приходящие от многих
 * рабочих потоков в один чанк. drain() забирает всё накопленное одним списком.
 */
public class MpscIntArrayListCollector {

    private final AtomicReference<Node> head = new AtomicReference<>();
    private volatile int drainCapacityHint;

    private static int append(Node head, IntArrayList out) {
        int base = out.size();
        int appended = 0;
        for (Node node = head; node != null; node = node.next) {
            int count = node.values == null ? 1 : node.values.length;
            long required = (long) base + appended + count;
            if (required > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "drained value count exceeds Java array limits: " + required);
            }
            if (node.values == null) {
                out.add(node.v);
            } else {
                for (int i = node.values.length - 1; i >= 0; i--) out.add(node.values[i]);
            }
            appended += count;
        }
        return appended;
    }

    public void push(int i) {
        pushNode(new Node(i, null));
    }

    public void pushBatch(IntList values) {
        int size = values.size();
        if (size == 0) return;
        if (size == 1) {
            push(values.getInt(0));
            return;
        }
        pushNode(new Node(0, values.toIntArray()));
    }

    public void pushBatchOwned(int[] values) {
        if (values.length == 0) return;
        pushNode(new Node(0, values));
    }

    private void pushNode(Node node) {
        while (true) {
            Node h = head.get();
            node.next = h;
            if (head.compareAndSet(h, node)) return;
        }
    }

    public IntArrayList drain() {
        Node h = detach();
        if (h == null) return new IntArrayList(0);
        if (h.next == null && h.values == null) {
            IntArrayList out = new IntArrayList(1);
            out.add(h.v);
            drainCapacityHint = 1;
            return out;
        }
        IntArrayList out = new IntArrayList(drainCapacityHint);
        int drained = append(h, out);
        drainCapacityHint = drained;
        return out;
    }

    public int drainTo(IntArrayList l) {
        Node h = detach();
        return h == null ? 0 : append(h, l);
    }

    private Node detach() {
        while (true) {
            Node h = head.get();
            if (h == null) return null;
            if (head.compareAndSet(h, null)) return h;
        }
    }

    private static final class Node {
        private final int v;
        private final int[] values;
        private Node next;

        private Node(int v, int[] values) {
            this.v = v;
            this.values = values;
        }
    }
}
