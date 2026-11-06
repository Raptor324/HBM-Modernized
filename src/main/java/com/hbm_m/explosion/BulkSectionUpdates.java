// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.ArrayDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Конвейер массового редактирования секций: снапшот снимается на серверном
 * потоке, вырезание считается рабочими, публикация (CAS-подобная сверка
 * поколений) — строго на серверном потоке с ограничением глубины.
 */
public final class BulkSectionUpdates implements AutoCloseable {

    private final ServerLevel level;
    private final ForkJoinPool executor;
    private final Consumer<Throwable> failureHandler;
    private final ArrayDeque<Work> captures = new ArrayDeque<>();
    private final ArrayDeque<Work> completed;
    private final int maxInFlight;
    private int retainedSnapshots;
    private boolean closed;
    private volatile int pending;

    public BulkSectionUpdates(ServerLevel level, ForkJoinPool executor, Consumer<Throwable> failureHandler) {
        this.level = level;
        this.executor = executor;
        this.maxInFlight = Math.max(1, executor.getParallelism() * 2);
        this.completed = new ArrayDeque<>(maxInFlight);
        this.failureHandler = failureHandler;
    }

    public void submit(Work work) {
        synchronized (this) {
            if (!closed) {
                captures.addLast(work);
                pending++;
                return;
            }
        }
        work.discard();
    }

    public boolean hasPending() {
        return pending != 0;
    }

    public boolean drain(long deadlineNanos) {
        boolean progressed = false;
        int deferred = 0;
        while (System.nanoTime() < deadlineNanos) {
            Work work;
            synchronized (this) {
                work = deferred < completed.size() ? completed.pollFirst() : null;
                if (work == null && retainedSnapshots < maxInFlight) work = captures.pollFirst();
            }
            if (work == null) return progressed;
            boolean submitted = false;
            try {
                if (work.failure != null) throw work.failure;
                LevelChunk current = BlastChunkUtil.liveChunkNow(level, work.chunkPos);
                if (current == null) {
                    work.missing();
                } else if (work.snapshot != null && !work.readyToPublish(current)) {
                    synchronized (this) {
                        completed.addLast(work);
                        submitted = true;
                        deferred++;
                    }
                    continue;
                } else if (work.snapshot != null && work.snapshot.publish()) {
                    work.published(current);
                } else {
                    work.clearAttempt();
                    boolean firstCapture = work.snapshot == null;
                    work.snapshot = SectionSnapshot.capture(current, work.observedSections());
                    Work captured = work;
                    synchronized (this) {
                        if (firstCapture) retainedSnapshots++;
                    }
                    executor.execute(() -> calculate(captured));
                    submitted = true;
                }
                progressed = true;
                deferred = 0;
            } catch (Throwable failure) {
                failureHandler.accept(
                        new IllegalStateException("Bulk section edit at chunk " + work.chunkPos, failure));
                return true;
            } finally {
                if (!submitted) finish(work);
            }
        }
        return progressed;
    }

    private void calculate(Work work) {
        synchronized (this) {
            if (closed) {
                finish(work);
                return;
            }
        }
        try {
            work.calculate(work.snapshot);
        } catch (Throwable failure) {
            work.failure = failure;
        }
        synchronized (this) {
            if (!closed) {
                completed.addLast(work);
                return;
            }
        }
        finish(work);
    }

    private void finish(Work work) {
        try {
            work.discard();
        } finally {
            synchronized (this) {
                if (work.snapshot != null) {
                    retainedSnapshots--;
                    work.snapshot = null;
                }
                pending--;
            }
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            Work work;
            while ((work = captures.pollFirst()) != null) finish(work);
            while ((work = completed.pollFirst()) != null) finish(work);
        }
    }

    public abstract static class Work {
        private final long chunkPos;
        private SectionSnapshot snapshot;
        private Throwable failure;

        protected Work(long chunkPos) {
            this.chunkPos = chunkPos;
        }

        protected abstract long observedSections();

        protected abstract void calculate(SectionSnapshot snapshot);

        protected boolean readyToPublish(LevelChunk chunk) {
            return true;
        }

        protected abstract void published(LevelChunk chunk);

        protected abstract void missing();

        protected abstract void clearAttempt();

        protected abstract void discard();
    }
}
