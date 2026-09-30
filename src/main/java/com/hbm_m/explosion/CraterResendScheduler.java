package com.hbm_m.explosion;

import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;

import it.unimi.dsi.fastutil.longs.LongLists;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Потоковая рассылка перепечаток чанков после вырезания кратера.
 *
 * <p>Второй проход кратера может затрагивать тысячи чанков; рассылка их полными
 * пакетами (чанк+свет) за один тик кладёт клиент (взрыв памяти и ребилд мешей).
 * Очередь размазывает рассылку по тикам с бюджетом, и каждый чанк отправляется
 * только после того, как его собственный свет рассчитан.
 */
public final class CraterResendScheduler {

    private record Pending(ServerLevel level, long chunkPos, long sectionMask) {}

    private static final ArrayDeque<Pending> QUEUE = new ArrayDeque<>();
    /** Чанков на тик: клиент успевает переварить, мир наполняется за десятки секунд. */
    private static final int PER_TICK = 16;
    private static volatile boolean registered;

    private CraterResendScheduler() {}

    public static void schedule(ServerLevel level, long chunkPos, long sectionMask) {
        ensureRegistered();
        synchronized (QUEUE) {
            QUEUE.add(new Pending(level, chunkPos, sectionMask));
        }
    }

    /** Полная очистка при остановке сервера. */
    public static void clear() {
        synchronized (QUEUE) {
            QUEUE.clear();
        }
    }

    static void serverTick() {
        for (int i = 0; i < PER_TICK; i++) {
            Pending pending;
            synchronized (QUEUE) {
                pending = QUEUE.pollFirst();
            }
            if (pending == null) return;
            ServerLevel level = pending.level();
            if (BlastChunkUtil.serverShuttingDown(level)) continue;
            int cx = ChunkPos.getX(pending.chunkPos());
            int cz = ChunkPos.getZ(pending.chunkPos());
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) continue;
            CompletableFutureLighting light =
                    new CompletableFutureLighting(level, chunk, pending.chunkPos(), pending.sectionMask());
            light.run();
        }
    }

    /** Свет чанка → перепечатка; всё с бюджетом и гейтами на шатдаун. */
    private static final class CompletableFutureLighting implements Runnable {
        private final ServerLevel level;
        private final LevelChunk chunk;
        private final long chunkPos;
        private final long sectionMask;

        CompletableFutureLighting(ServerLevel level, LevelChunk chunk, long chunkPos, long sectionMask) {
            this.level = level;
            this.chunk = chunk;
            this.chunkPos = chunkPos;
            this.sectionMask = sectionMask;
        }

        @Override
        public void run() {
            CompletableFuture<?> lighting = BlastChunkUtil.updateLight(
                    level, chunk, LongLists.EMPTY_LIST, sectionMask);
            lighting.thenRunAsync(
                    () -> BlastChunkUtil.sendWholesaleResend(level, chunkPos),
                    level.getServer());
        }
    }

    private static void ensureRegistered() {
        if (registered) return;
        registered = true;
        //? if forge {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BombLifecycleHooks.class);
        //?} else {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(BombLifecycleHooks.class);
         *///?}
    }
}
