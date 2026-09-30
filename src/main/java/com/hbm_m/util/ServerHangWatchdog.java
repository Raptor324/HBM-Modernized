package com.hbm_m.util;

import com.hbm_m.main.MainRegistry;

import net.minecraft.server.MinecraftServer;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.lang.Thread.State;

/**
 * Диагностический watchdog интегрированного/выделенного сервера. Ванильный
 * WatchdogThread работает только на dedicated-сервере, поэтому зависание
 * серверного потока (например, при сохранении мира) в синглплеере выглядит
 * как вечный фриз без единой строки в логе. Этот поток раз в 10 секунд
 * проверяет прогресс тиков; при простое > 90 секунд пишет стек серверного
 * потока и световых воркеров в лог — без краша игры.
 *
 * Отключение: -Dhbm.saveWatchdog=false
 */
public final class ServerHangWatchdog {

    private static final long HANG_THRESHOLD_NANOS = 90_000_000_000L;
    private static final long POLL_INTERVAL_MS = 10_000;

    private static volatile long lastTickNanos = System.nanoTime();
    private static volatile boolean hangLogged;
    private static volatile WeakReference<MinecraftServer> serverRef;
    private static volatile boolean started;

    private ServerHangWatchdog() {}

    /** Вызывать из конца каждого серверного тика. */
    public static void tickFinished(MinecraftServer server) {
        lastTickNanos = System.nanoTime();
        hangLogged = false;
        serverRef = new WeakReference<>(server);
        startIfNeeded();
    }

    private static synchronized void startIfNeeded() {
        if (started) return;
        if ("false".equals(System.getProperty("hbm.saveWatchdog"))) return;
        started = true;
        Thread t = new Thread(ServerHangWatchdog::run, "HBM-ServerHangWatchdog");
        t.setDaemon(true);
        t.start();
    }

    private static void run() {
        while (true) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ignored) {
                return;
            }
            MinecraftServer server = serverRef == null ? null : serverRef.get();
            if (server == null || !server.isRunning()) continue;
            long idle = System.nanoTime() - lastTickNanos;
            if (idle < HANG_THRESHOLD_NANOS || hangLogged) continue;
            hangLogged = true;
            dump(server, idle);
        }
    }

    private static void dump(MinecraftServer server, long idleNanos) {
        MainRegistry.LOGGER.error(
                "[HBM WATCHDOG] server thread made no progress for {}s — dumping stacks (tick watchdog diagnostic)",
                idleNanos / 1_000_000_000L);
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            String name = thread.getName();
            boolean interesting = name.equals("Server thread")
                    || name.contains("light")
                    || name.contains("Worker-Main") && entry.getValue().length > 0;
            if (!interesting || entry.getValue().length == 0) continue;
            StringBuilder sb = new StringBuilder("\nThread \"").append(name).append("\" state=")
                    .append(thread.getState());
            if (thread.getState() == State.BLOCKED || thread.getState() == State.WAITING
                    || thread.getState() == State.TIMED_WAITING) {
                sb.append(" — parked at:");
            }
            MainRegistry.LOGGER.error("[HBM WATCHDOG]{}", sb);
            for (StackTraceElement element : entry.getValue()) {
                MainRegistry.LOGGER.error("[HBM WATCHDOG]    at {}", element);
            }
        }
    }
}
