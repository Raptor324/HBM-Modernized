// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MPSC-очередь long'ов (координаты чанков, ожидающих загрузки).
 * Частота операций — единицы на чанк, поэтому обёртка над
 * ConcurrentLinkedQueue достаточна; box-аллокации пренебрежимы.
 */
public final class MpscLongQueue {

    private final ConcurrentLinkedQueue<Long> backing = new ConcurrentLinkedQueue<>();

    public void offer(long value) {
        backing.offer(value);
    }

    /** @return следующий элемент или {@code null}, если очередь пуста. */
    public Long relaxedPoll() {
        return backing.poll();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public void clear() {
        backing.clear();
    }
}
