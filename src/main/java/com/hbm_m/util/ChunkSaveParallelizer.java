package com.hbm_m.util;

import com.hbm_m.explosion.BlastChunkUtil;
import com.hbm_m.main.MainRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Параллельная сериализация чанков при массовых операциях (кратер MK5, фоллаут).
 *
 * Ванилла (1.20.1 и 1.21.1) сериализует чанк в NBT ({@code ChunkSerializer.write})
 * ПРЯМО НА СЕРВЕРНОМ ПОТОКЕ внутри {@code ChunkMap.save} — асинхронна только запись
 * готовых байтов в region-файл. На абсурдном кратере (78к чанков, spark-профиль
 * RaoqN397l6) это съело 70% серверного потока (1681с из 2393с).
 *
 * Сериализация — чистая функция состояния чанка → NBT, параллелится свободно.
 * {@code submit} выполняет её на собственном пуле, готовый тег возвращается на
 * серверный поток, где постится ChunkDataEvent.Save и вызывается ванильная
 * async-запись. Запись в дисковые region-файлы НЕ трогаем — она уже разведена
 * по регионам у ваниллы и упирается в диск, а не в CPU.
 *
 * Включается счётчиком массовых операций (взрыв/фоллаут —
 * {@link #acquireMassOp}/{@link #releaseMassOp}); вне их — ноль вмешательства.
 * Backpressure: при переполнении in-flight очередь {@code submit} возвращает
 * false, вызывающий (миксин) сериализует синхронно — как ванилла.
 * Kill-switch: -Dhbm.parallelChunkSave=false
 */
public final class ChunkSaveParallelizer {

    private static final int WORKERS = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() / 2));
    private static final int MAX_IN_FLIGHT = WORKERS * 16;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(WORKERS, r -> {
        Thread t = new Thread(r, "HBM-ChunkSave");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicInteger MASS_OPS = new AtomicInteger();
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();

    /**
     * Грейс-окно параллельного сейва после снятия последней масс-операции.
     * Шторм выгрузки/сейва кратера MK5 (~4400 чанков) начинается ПОСЛЕ
     * «Explosion complete» — сущность взрыва снимает счётчик в remove() ровно
     * в этот момент, и без окна тысячи сериализаций возвращаются на серверный
     * поток (11+ сек ступора, см. лог 0929).
     */
    private static volatile long graceUntilMs;

    private ChunkSaveParallelizer() {}

    public static void acquireMassOp() {
        MASS_OPS.incrementAndGet();
    }

    public static void releaseMassOp() {
        int v;
        do {
            v = MASS_OPS.get();
        } while (v > 0 && !MASS_OPS.compareAndSet(v, v - 1));
        if (MASS_OPS.get() == 0) {
            graceUntilMs = System.currentTimeMillis() + 90_000;
        }
    }

    public static boolean isMassOperation() {
        if ("false".equals(System.getProperty("hbm.parallelChunkSave"))) return false;
        return MASS_OPS.get() > 0 || System.currentTimeMillis() < graceUntilMs;
    }

    /**
     * Сериализует чанк на пуле, готовый тег отдаёт на серверный поток.
     *
     * Слот IN_FLIGHT освобождается коллбэком release только когда тег ДОЕХАЛ до
     * диска (1.21.1: completion у write-future) или сразу после вызова записи
     * (1.20.1: сигнала завершения нет). Это делает MAX_IN_FLIGHT жёстким потолком
     * очереди незаписанных тегов — без него 92к тегов × ~100KB клали 11+GB в кучу
     * и душили фоллаут голодом за IO (клин 0929).
     *
     * @return false — пул недоступен (нет масс-операции/шатдаун) или перегружен:
     *         вызывающий должен сериализовать синхронно, как ванилла.
     */
    public static boolean submit(
            ServerLevel level, ChunkAccess chunk,
            java.util.function.BiConsumer<CompoundTag, Runnable> onTagReady, Runnable onFailure) {
        if (!isMassOperation() || BlastChunkUtil.serverShuttingDown(level)) return false;
        if (IN_FLIGHT.get() >= MAX_IN_FLIGHT) return false;
        IN_FLIGHT.incrementAndGet();
        POOL.execute(() -> {
            CompoundTag tag = null;
            Throwable failure = null;
            try {
                tag = ChunkSaveParallelizer.serialize(level, chunk);
            } catch (Throwable t) {
                failure = t;
            }
            final CompoundTag fTag = tag;
            final Throwable fFailure = failure;
            // Шатдаун влетел после постановки в очередь: server.execute на
            // остановленном сервере молча выкинет задачу → тег потерян, чанк уже
            // помечен чистым. Добиваемся inline на воркере (ивент+запись
            // потокобезопасны, см. аудит в ChunkMapSaveMixin).
            Runnable finish = () -> {
                if (fFailure != null) {
                    IN_FLIGHT.decrementAndGet();
                    MainRegistry.LOGGER.error("[HBM] parallel chunk serialize failed at {},{} — falling back to vanilla resave",
                            chunk.getPos().x, chunk.getPos().z, fFailure);
                    chunk.setUnsaved(true);
                    onFailure.run();
                } else {
                    onTagReady.accept(fTag, IN_FLIGHT::decrementAndGet);
                }
            };
            if (BlastChunkUtil.serverShuttingDown(level)) {
                finish.run();
            } else {
                level.getServer().execute(finish);
            }
        });
        return true;
    }

    private static CompoundTag serialize(ServerLevel level, ChunkAccess chunk) {
        return net.minecraft.world.level.chunk.storage.ChunkSerializer.write(level, chunk);
    }
}
