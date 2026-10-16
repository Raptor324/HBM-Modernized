package com.hbm_m.util;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;

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
 * Меню паузы (Esc) в синглплеере простое не считается — тики стоят штатно,
 * дампы из меню не сыплются. Диагностика зависаний при сохранении мира это
 * не глушит: экран «Сохранение мира» — не PauseScreen и в проверку не попадает.
 *
 * Отключение: -Dhbm.saveWatchdog=false
 */
public final class ServerHangWatchdog {

    private static final long HANG_THRESHOLD_NANOS = 90_000_000_000L;
    private static final long POLL_INTERVAL_MS = 10_000;

    private static volatile long lastTickNanos = System.nanoTime();
    private static volatile boolean hangLogged;
    private static volatile boolean pauseLogged;
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
            // Пауза синглплеера: тики намеренно стоят. Держим счётчик простоя
            // сброшенным, чтобы после снятия паузы отсчёт 90 с пошёл заново.
            if (!server.isDedicatedServer() && PlatformHooks.isPauseMenuOpen()) {
                lastTickNanos = System.nanoTime();
                if (!pauseLogged) {
                    pauseLogged = true;
                    MainRegistry.LOGGER.info("[HBM WATCHDOG] pause menu open — stall detection suspended");
                }
                continue;
            }
            if (pauseLogged) {
                pauseLogged = false;
                MainRegistry.LOGGER.info("[HBM WATCHDOG] pause menu closed — stall detection active");
            }
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
            StackTraceElement[] stack = entry.getValue();
            boolean interesting = name.equals("Server thread")
                    || name.contains("light")
                    || name.contains("Worker-Main") && stack.length > 0;
            if (!interesting || stack.length == 0) continue;
            // Воркеры, припаркованные в очереди ForkJoinPool за новой задачей, —
            // чистый шум: они не застряли, им просто нечего делать. Сигнальны
            // работающий или реально заблокированный поток, а не ждущий пул.
            if ((thread.getState() == State.WAITING || thread.getState() == State.TIMED_WAITING)
                    && isParkedInWorkerQueue(stack)) {
                continue;
            }
            StringBuilder sb = new StringBuilder("\nThread \"").append(name).append("\" state=")
                    .append(thread.getState());
            if (thread.getState() == State.BLOCKED || thread.getState() == State.WAITING
                    || thread.getState() == State.TIMED_WAITING) {
                sb.append(" — parked at:");
            }
            MainRegistry.LOGGER.error("[HBM WATCHDOG]{}", sb);
            for (StackTraceElement element : stack) {
                MainRegistry.LOGGER.error("[HBM WATCHDOG]    at {}", element);
            }
        }
    }

    /** Верхние кадры — ожидание задачи в ForkJoinPool (awaitWork): воркер простаивает. */
    private static boolean isParkedInWorkerQueue(StackTraceElement[] stack) {
        int scan = Math.min(stack.length, 6);
        for (int i = 0; i < scan; i++) {
            StackTraceElement e = stack[i];
            if ("java.util.concurrent.ForkJoinPool".equals(e.getClassName())
                    && "awaitWork".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
