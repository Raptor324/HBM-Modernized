// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Общий ForkJoinPool для тяжёлых бомбовых расчётов MK5. Пул живёт, пока есть
 * хотя бы один держатель-взрыв; при выгрузке измерения все его взрывы отменяются.
 */
public final class BombForkJoinPool {

    private static final Map<ResourceLocation, Set<IJobCancellable>> JOBS_BY_DIM = new HashMap<>();
    private static final Object LOCK = new Object();
    private static ForkJoinPool pool;
    private static int refs;

    private BombForkJoinPool() {}

    public static ForkJoinPool acquire() {
        BombLifecycleHooks.ensureRegistered();
        synchronized (LOCK) {
            if (pool == null || pool.isShutdown()) {
                pool = new ForkJoinPool(
                        computeWorkers(),
                        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                        BombForkJoinPool::logWorkerFailure,
                        false);
            }
            refs++;
            return pool;
        }
    }

    public static void release(ForkJoinPool acquiredPool) {
        ForkJoinPool toShutdown;
        synchronized (LOCK) {
            if (acquiredPool != pool) return;
            if (refs <= 0) throw new IllegalStateException("BombForkJoinPool reference underflow");
            refs--;
            toShutdown = maybeShutdownLocked();
        }
        if (toShutdown != null) toShutdown.shutdown();
    }

    public static void register(ForkJoinPool acquiredPool, ResourceLocation dimensionId, IJobCancellable job) {
        if (job == null || dimensionId == null || acquiredPool == null) return;
        synchronized (LOCK) {
            if (acquiredPool != pool) return;
            JOBS_BY_DIM.computeIfAbsent(dimensionId, k -> new HashSet<>()).add(job);
        }
    }

    public static void unregister(ForkJoinPool acquiredPool, ResourceLocation dimensionId, IJobCancellable job) {
        if (job == null || dimensionId == null || acquiredPool == null) return;
        ForkJoinPool toShutdown = null;
        synchronized (LOCK) {
            if (acquiredPool != pool) return;
            Set<IJobCancellable> set = JOBS_BY_DIM.get(dimensionId);
            if (set == null) return;
            set.remove(job);
            if (set.isEmpty()) {
                JOBS_BY_DIM.remove(dimensionId);
                toShutdown = maybeShutdownLocked();
            }
        }
        if (toShutdown != null) toShutdown.shutdown();
    }

    public static void onLevelUnload(ServerLevel level) {
        if (level == null) return;
        onLevelUnload(level.dimension().location());
    }

    public static void onLevelUnload(ResourceLocation dimensionId) {
        List<IJobCancellable> jobs = null;
        synchronized (LOCK) {
            Set<IJobCancellable> set = JOBS_BY_DIM.remove(dimensionId);
            if (set != null && !set.isEmpty()) {
                jobs = new ArrayList<>(set);
                set.clear();
            }
        }
        if (jobs != null) {
            for (IJobCancellable job : jobs) {
                try {
                    job.cancelJob();
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("Failed to cancel bomb job on dimension unload {}", dimensionId, t);
                }
            }
        }
        ForkJoinPool toShutdown;
        synchronized (LOCK) {
            toShutdown = maybeShutdownLocked();
        }
        if (toShutdown != null) toShutdown.shutdown();
    }

    public static void onServerStopped() {
        List<IJobCancellable> jobs = null;
        ForkJoinPool toStop;
        synchronized (LOCK) {
            if (!JOBS_BY_DIM.isEmpty()) {
                int approx = 16;
                for (Set<IJobCancellable> set : JOBS_BY_DIM.values()) approx += set.size();
                jobs = new ArrayList<>(approx);
                for (Set<IJobCancellable> set : JOBS_BY_DIM.values()) {
                    jobs.addAll(set);
                    set.clear();
                }
                JOBS_BY_DIM.clear();
            }
            refs = 0;
            toStop = pool;
            pool = null;
        }
        if (jobs != null) {
            for (IJobCancellable job : jobs) {
                try {
                    job.cancelJob();
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("Failed to cancel bomb job on server stop", t);
                }
            }
        }
        if (toStop != null && !toStop.isShutdown()) toStop.shutdown();
    }

    private static ForkJoinPool maybeShutdownLocked() {
        if (pool == null) return null;
        if (refs != 0) return null;
        if (!JOBS_BY_DIM.isEmpty()) return null;
        ForkJoinPool p = pool;
        pool = null;
        return p;
    }

    private static int computeWorkers() {
        int processors = Runtime.getRuntime().availableProcessors();
        int configured = ModClothConfig.get().bombMaxThreads;
        int workers = configured <= 0 ? Math.max(1, processors + configured) : Math.min(configured, processors);
        return Math.max(1, workers);
    }

    private static void logWorkerFailure(Thread thread, Throwable error) {
        MainRegistry.LOGGER.error("Bomb ForkJoinPool worker crashed in {}", thread.getName(), error);
    }

    public interface IJobCancellable {
        void cancelJob();
    }
}
