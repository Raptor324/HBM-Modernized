package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.ConcurrentBitSet;
import com.hbm_m.util.SubChunkKey;
import com.hbm_m.util.SubChunkSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Потоковый движок взрыва MK5 (алгоритмы 6.06 = 1 и 2; оба режима теперь идентичны).
 *
 * <p>Отличие от первой версии порта: трассировка использует ТОЧНУЮ энергомодель
 * Legacy-генератора ({@link NukeMk5ChunkEater}): луч гаснет по
 * {@code res -= pow(resistance, 7.5 - fac)} и уничтожает весь непрерывный путь
 * до точки смерти. Именно эта модель даёт у Legacy гладкие стенки кратера без
 * "швейцарского сыша"; прежняя модель {@code (res+1)^(3d/r)} давала изрезанные края.
 *
 * <p>Архитектура потокобезопасности:
 * <ul>
 *   <li>рабочие потоки читают ТОЛЬКО неизменяемые снимки {@link SubChunkSnapshot}
 *       и concurrent-коллекции — прямой доступ к миру из потоков запрещён;</li>
 *   <li>снимки создаются и блоки уничтожаются только на серверном потоке
 *       ({@link #cacheChunksTick}/{@link #destructionTick} вызываются из tick() сущности);</li>
 *   <li>луч, дошедший до неснапшотированного суб-чанка, паркуется в "зале ожидания"
 *       и возобновляется после создания снимка;</li>
 *   <li>прогрузка чанков — ТОЛЬКО асинхронная через region-тикеты (синхронный getChunk
 *       блокирует серверный поток на дисковой загрузке/генерации).</li>
 * </ul>
 *
 * <p>Память: без damage-накопления (десятки млн DoubleAdder), без списка направлений
 * (считаются по индексу замкнутой формулой сферы Фибоначчи), без pre-fill очереди задач
 * (лучи раздаются рабочим через AtomicInteger). Деструктивный результат — битсет
 * блоков на чанк.
 */
public class ExplosionNukeRayParallelized implements IExplosionRay {

    /** Тикет точечной прогрузки чанка под снапшот; снимается сразу после копирования. */
    private static final TicketType<ChunkPos> SNAPSHOT_LOAD =
            TicketType.create("hbm_m_nuke_snapshot", Comparator.comparingLong(p -> (long) p.x << 32 ^ p.z));

    private final ServerLevel level;
    private final double explosionX, explosionY, explosionZ;
    private final int originX, originY, originZ;
    private final int strength;
    private final int radius;
    /** Длина луча в блоках — как в Legacy: rayLength = ceil(strength), кап по length. */
    private final int rayLength;

    /** Динамическая высота мира (1.20+: 384 вместо фиксированных 256 в 1.7.10). */
    private final int minY;
    private final int worldHeight;
    private final int bitsetSize;

    private final ConcurrentMap<ChunkPos, ConcurrentBitSet> destructionMap;
    private final ConcurrentMap<SubChunkKey, SubChunkSnapshot> snapshots;
    private final ConcurrentMap<SubChunkKey, ConcurrentLinkedQueue<RayTask>> waitingRoom;
    /** Очередь ТОЛЬКО для припаркованных лучей — новых задач сюда не кладём. */
    private final BlockingQueue<RayTask> parkedRayQueue;
    private final ExecutorService pool;
    private final CountDownLatch latch;
    private final Thread latchWatcherThread;
    private final List<ChunkPos> orderedChunks;
    private final BlockingQueue<SubChunkKey> highPriorityReactiveQueue;
    private final Iterator<SubChunkKey> lowPriorityProactiveIterator;
    /** Раздача индексов лучей рабочим потокам без предварительного создания задач. */
    private final AtomicInteger nextRayIndex = new AtomicInteger(0);
    private final int rayCount;
    /** Ключи, чьи чанки ещё не загружены — повторяются в следующих тиках после запроса тикета. */
    private final java.util.Set<SubChunkKey> retryKeys = ConcurrentHashMap.newKeySet();
    /** Выданные точечные тикеты прогрузки (чанк → значение тикета). */
    private final ConcurrentMap<ChunkPos, ChunkPos> issuedTickets = new ConcurrentHashMap<>();
    private volatile boolean collectFinished = false;
    private volatile boolean destroyFinished = false;
    /** Throttle для лога ожидания тикетов прогрузки. */
    private volatile long lastRetryLog = 0L;

    public ExplosionNukeRayParallelized(ServerLevel level, double x, double y, double z, int strength, int speed, int radius) {
        this.level = level;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;

        this.originX = (int) Math.floor(x);
        this.originY = (int) Math.floor(y);
        this.originZ = (int) Math.floor(z);

        this.strength = strength;
        this.radius = radius;
        this.rayLength = (int) Math.ceil(strength);

        this.minY = level.getMinBuildHeight();
        this.worldHeight = level.getHeight();
        this.bitsetSize = 16 * worldHeight * 16;

        // число лучей как у Legacy: 2.5 * PI * strength^2
        this.rayCount = Math.max(0, (int) (2.5 * Math.PI * strength * (double) strength));
        this.latch = new CountDownLatch(rayCount);
        List<SubChunkKey> sortedSubChunks = getAllSubChunks();
        this.lowPriorityProactiveIterator = sortedSubChunks.iterator();
        this.highPriorityReactiveQueue = new LinkedBlockingQueue<>();

        int initialChunkCapacity = (int) sortedSubChunks.stream().map(SubChunkKey::getPos).distinct().count();

        this.destructionMap = new ConcurrentHashMap<>(initialChunkCapacity);

        int subChunkCount = sortedSubChunks.size();
        this.snapshots = new ConcurrentHashMap<>(subChunkCount);
        this.waitingRoom = new ConcurrentHashMap<>(subChunkCount);
        this.orderedChunks = new ArrayList<>();
        this.parkedRayQueue = new LinkedBlockingQueue<>();

        int workers = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.pool = Executors.newWorkStealingPool(workers);

        for (int i = 0; i < workers; i++) pool.submit(new Worker());

        this.latchWatcherThread = new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                collectFinished = true;
                MainRegistry.LOGGER.info("[NUKE MK5/PAR] Ray tracing finished: {} chunks marked for destruction",
                        destructionMap.size());
            }
        }, "ExplosionNuke-LatchWatcher-" + System.nanoTime());
        this.latchWatcherThread.setDaemon(true);
        this.latchWatcherThread.start();

        MainRegistry.LOGGER.info("[NUKE MK5/PAR] Threaded engine started: strength={}, radius={}, rays={}, subchunks={}",
                strength, radius, rayCount, subChunkCount);
    }

    /** Сопротивление блока — 1:1 с {@link NukeMk5ChunkEater#masqueradeResistance}. */
    private static float masqueradeResistance(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.SANDSTONE) {
            return Blocks.STONE.getExplosionResistance();
        }
        if (block == Blocks.OBSIDIAN) {
            return Blocks.STONE.getExplosionResistance() * 3;
        }
        return block.getExplosionResistance();
    }

    private List<SubChunkKey> getAllSubChunks() {
        List<SubChunkKey> keys = new ArrayList<>();
        int cr = (radius + 15) >> 4;
        int minCX = (originX >> 4) - cr;
        int maxCX = (originX >> 4) + cr;
        int minCZ = (originZ >> 4) - cr;
        int maxCZ = (originZ >> 4) + cr;
        int minSubY = Math.max(minY >> 4, (originY - radius) >> 4);
        int maxSubY = Math.min(((minY + worldHeight - 1) >> 4), (originY + radius) >> 4);
        int originSubY = originY >> 4;

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                for (int subY = minSubY; subY <= maxSubY; subY++) {
                    int chunkCenterX = (cx << 4) + 8;
                    int chunkCenterY = (subY << 4) + 8;
                    int chunkCenterZ = (cz << 4) + 8;
                    double dx = chunkCenterX - explosionX;
                    double dy = chunkCenterY - explosionY;
                    double dz = chunkCenterZ - explosionZ;
                    if (dx * dx + dy * dy + dz * dz <= (radius + 14) * (radius + 14)) { // +14 запас на погрешность
                        keys.add(new SubChunkKey(cx, cz, subY));
                    }
                }
            }
        }
        int originCX = originX >> 4;
        int originCZ = originZ >> 4;
        keys.sort(Comparator.comparingInt(key -> {
            int distCX = key.getChunkX() - originCX;
            int distCZ = key.getChunkZ() - originCZ;
            int distSubY = key.getSubY() - originSubY;
            return distCX * distCX + distCZ * distCZ + distSubY * distSubY;
        }));
        return keys;
    }

    @Override
    public void cacheChunksTick(int timeBudgetMs) {
        if (collectFinished) return;
        // повторная попытка для чанков, ожидающих прогрузки по тикету
        if (!retryKeys.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastRetryLog >= 5_000L) {
                lastRetryLog = now;
                MainRegistry.LOGGER.info("[NUKE MK5/PAR] Waiting for chunk tickets: {} subchunks pending, {} snapshots done",
                        retryKeys.size(), snapshots.size());
            }
            highPriorityReactiveQueue.addAll(retryKeys);
            retryKeys.clear();
        }
        final long deadline = System.nanoTime() + (timeBudgetMs * 1_000_000L);
        while (System.nanoTime() < deadline) {
            SubChunkKey ck = highPriorityReactiveQueue.poll();
            if (ck == null) break;
            processCacheKey(ck);
        }
        while (System.nanoTime() < deadline && lowPriorityProactiveIterator.hasNext()) {
            SubChunkKey ck = lowPriorityProactiveIterator.next();
            processCacheKey(ck);
        }
        // [FIX] Самовосстановление: лучи запаркованы, но их ключи нигде не стоят
        // в очереди (гонка паркинга) — без этого latch не сходится и взрыв висит.
        // processCacheKey по готовому снапшоту только дренит waiters — дёшево.
        if (!collectFinished && retryKeys.isEmpty() && highPriorityReactiveQueue.isEmpty() && !waitingRoom.isEmpty()) {
            highPriorityReactiveQueue.addAll(waitingRoom.keySet());
        }
    }

    private void processCacheKey(SubChunkKey ck) {
        if (!snapshots.containsKey(ck)) {
            ChunkPos cp = ck.getPos();
            SubChunkSnapshot snap = SubChunkSnapshot.getSnapshot(level, ck, ModClothConfig.get().enableChunkLoading);
            if (snap == null) {
                // чанк не загружен: запрашиваем асинхронную прогрузку тикетом и повторим позже.
                // Синхронный getChunk здесь запрещён — он блокирует серверный поток.
                if (issuedTickets.putIfAbsent(cp, cp) == null) {
                    // радиус 2 обязателен: уровень 33-2 = 31 = FULL. Радиус 0 держит чанк
                    // на уровне 33 (border) — hasChunk всегда false, чанк никогда не прогрузится.
                    level.getChunkSource().addRegionTicket(SNAPSHOT_LOAD, cp, 2, cp);
                }
                retryKeys.add(ck);
                return;
            }
            snapshots.put(ck, snap);
            // снимок — копия, чанк больше не нужен; снимаем точечный тикет, чанк выгрузится
            releaseTicket(cp);
        }
        // [FIX] Расспарковка лучей даже если снимок уже существовал: возможна гонка,
        // когда луч запарковался в waitingRoom ПОСЛЕ создания снимка (snapshots.get
        // вернул null чуть раньше put). Ранний return при готовом снимке оставлял
        // таких лучей ждать вечно — latch не сходился и взрыв зависал без кратера.
        ConcurrentLinkedQueue<RayTask> waiters = waitingRoom.remove(ck);
        if (waiters != null) parkedRayQueue.addAll(waiters);
    }

    private void releaseTicket(ChunkPos cp) {
        if (issuedTickets.remove(cp) != null) {
            level.getChunkSource().removeRegionTicket(SNAPSHOT_LOAD, cp, 2, cp);
        }
    }

    @Override
    public void destructionTick(int timeBudgetMs) {
        if (!collectFinished || destroyFinished) return;

        final long deadline = System.nanoTime() + timeBudgetMs * 1_000_000L;

        if (orderedChunks.isEmpty() && !destructionMap.isEmpty()) {
            orderedChunks.addAll(destructionMap.keySet());
            int originCX = originX >> 4;
            int originCZ = originZ >> 4;
            orderedChunks.sort(Comparator.comparingInt(c -> Math.abs(originCX - c.x) + Math.abs(originCZ - c.z)));
        }

        Iterator<ChunkPos> it = orderedChunks.iterator();
        while (it.hasNext() && System.nanoTime() < deadline) {
            ChunkPos cp = it.next();
            ConcurrentBitSet bs = destructionMap.get(cp);
            if (bs == null) {
                it.remove();
                continue;
            }

            // чанк мог выгрузиться после снапшота (тикет снят): setBlock синхронно
            // загрузил бы его и заблокировал поток — запрашиваем тикет и пропускаем до следующего тика
                if (!level.hasChunk(cp.x, cp.z)) {
                    if (issuedTickets.putIfAbsent(cp, cp) == null) {
                        level.getChunkSource().addRegionTicket(SNAPSHOT_LOAD, cp, 2, cp);
                    }
                    continue;
                }

            // диапазон битов секции subY: старший бит = верхний Y
            for (int subY = minY >> 4; subY <= (minY + worldHeight - 1) >> 4; subY++) {
                int yTop = (subY << 4) + 15;
                int yNormTop = yTop - minY;
                if (yNormTop < 0 || yNormTop - 15 >= worldHeight) continue;

                int startBit = (worldHeight - 1 - Math.min(yNormTop, worldHeight - 1)) << 8;
                int endBit = (worldHeight - 1 - Math.max(yNormTop - 15, 0)) << 8 | 0xFF;

                int bit = bs.nextSetBit(startBit);
                while (bit >= 0 && bit <= endBit && System.nanoTime() < deadline) {
                    int yNorm = worldHeight - 1 - (bit >>> 8);
                    int xGlobal = (cp.x << 4) | ((bit >>> 4) & 0xF);
                    int zGlobal = (cp.z << 4) | (bit & 0xF);
                    int yGlobal = minY + yNorm;

                    clearBlock(xGlobal, yGlobal, zGlobal);
                    bs.clear(bit);
                    bit = bs.nextSetBit(bit + 1);
                }
            }

            if (bs.isEmpty()) {
                destructionMap.remove(cp);
                for (int subY = minY >> 4; subY <= (minY + worldHeight - 1) >> 4; subY++) {
                    snapshots.remove(new SubChunkKey(cp, subY));
                }
                releaseTicket(cp);
                it.remove();
            }
        }

        if (orderedChunks.isEmpty() && destructionMap.isEmpty()) {
            destroyFinished = true;
            MainRegistry.LOGGER.info("[NUKE MK5/PAR] Terrain destruction complete");
            if (pool != null) pool.shutdown();
        }
    }

    /**
     * Удаляет блок. Флаги как в {@link NukeMk5ChunkEater}: UPDATE_CLIENTS | UPDATE_IMMEDIATE,
     * чтобы соседние source-жидкости не затекали обратно в кратер.
     */
    private void clearBlock(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir() && state.getFluidState().isEmpty()) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
    }

    @Override
    public boolean isComplete() {
        return collectFinished && destroyFinished;
    }

    @Override
    public void cancel() {
        this.collectFinished = true;
        this.destroyFinished = true;

        if (this.parkedRayQueue != null) this.parkedRayQueue.clear();
        if (this.waitingRoom != null) this.waitingRoom.clear();

        if (this.latch != null) while (this.latch.getCount() > 0) this.latch.countDown();
        if (this.latchWatcherThread != null && this.latchWatcherThread.isAlive()) this.latchWatcherThread.interrupt();

        if (this.pool != null && !this.pool.isShutdown()) {
            this.pool.shutdownNow();
            try {
                if (!this.pool.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    MainRegistry.LOGGER.error("ExplosionNukeRayParallelized thread pool did not terminate promptly on cancel.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!this.pool.isShutdown()) this.pool.shutdownNow();
            }
        }
        if (this.destructionMap != null) this.destructionMap.clear();
        if (this.snapshots != null) this.snapshots.clear();
        if (this.orderedChunks != null) this.orderedChunks.clear();
        if (this.retryKeys != null) this.retryKeys.clear();
        // снять все незакрытые тикеты прогрузки, иначе чанки зависнут в памяти
        if (this.issuedTickets != null && this.level != null) {
            for (ChunkPos cp : this.issuedTickets.keySet()) {
                this.level.getChunkSource().removeRegionTicket(SNAPSHOT_LOAD, cp, 2, cp);
            }
            this.issuedTickets.clear();
        }
    }

    /**
     * Направление луча по индексу — сфера Фибоначчи (замкнутая формула, без общего
     * состояния и без хранения списка направлений: для радиуса 500 это ~7.85 млн лучей).
     */
    private double[] directionAt(int index) {
        if (rayCount == 1) {
            return new double[]{1, 0, 0};
        }
        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        double y = 1.0 - (index / (double) (rayCount - 1)) * 2.0;
        double r = Math.sqrt(1.0 - y * y);
        double t = phi * index;
        return new double[]{Math.cos(t) * r, y, Math.sin(t) * r};
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            try {
                while (!collectFinished && !Thread.currentThread().isInterrupted()) {
                    RayTask task = null;
                    // сначала берём нераспределённый луч, потом припаркованный
                    while (true) {
                        int idx = nextRayIndex.get();
                        if (idx >= rayCount) break;
                        if (nextRayIndex.compareAndSet(idx, idx + 1)) {
                            task = new RayTask(idx);
                            break;
                        }
                    }
                    if (task == null) {
                        task = parkedRayQueue.poll(100, TimeUnit.MILLISECONDS);
                    }
                    if (task != null) task.trace();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Луч с энергомоделью Legacy: DDA-обход вокселей, res гаснет по
     * {@code pow(resistance, 7.5 - fac)}, блок помечается на уничтожение, если луч
     * пережил прохождение сквозь него ({@code res > 0}). Это воспроизводит
     * непрерывное "выедание" пути луча у NukeMk5ChunkEater.
     */
    private class RayTask {
        private static final double RAY_DIRECTION_EPSILON = 1e-6;

        final int dirIndex;
        double px, py, pz;
        int x, y, z;
        float res;
        double tMaxX, tMaxY, tMaxZ, tDeltaX, tDeltaY, tDeltaZ;
        int stepX, stepY, stepZ;
        boolean initialised = false;
        double currentRayPosition;

        private int lastCX = Integer.MIN_VALUE, lastCZ = Integer.MIN_VALUE, lastSubY = Integer.MIN_VALUE;
        private SubChunkKey currentSubChunkKey = null;

        RayTask(int dirIdx) {
            this.dirIndex = dirIdx;
        }

        void init() {
            double[] dir = directionAt(this.dirIndex);
            this.res = strength;
            this.px = explosionX;
            this.py = explosionY;
            this.pz = explosionZ;
            this.x = originX;
            this.y = originY;
            this.z = originZ;
            this.currentRayPosition = 0.0;

            double absDirX = Math.abs(dir[0]);
            this.stepX = (absDirX < RAY_DIRECTION_EPSILON) ? 0 : (dir[0] > 0 ? 1 : -1);
            this.tDeltaX = (stepX == 0) ? Double.POSITIVE_INFINITY : 1.0 / absDirX;
            this.tMaxX = (stepX == 0) ? Double.POSITIVE_INFINITY : ((stepX > 0 ? (this.x + 1 - this.px) : (this.px - this.x)) * this.tDeltaX);

            double absDirY = Math.abs(dir[1]);
            this.stepY = (absDirY < RAY_DIRECTION_EPSILON) ? 0 : (dir[1] > 0 ? 1 : -1);
            this.tDeltaY = (stepY == 0) ? Double.POSITIVE_INFINITY : 1.0 / absDirY;
            this.tMaxY = (stepY == 0) ? Double.POSITIVE_INFINITY : ((stepY > 0 ? (this.y + 1 - this.py) : (this.py - this.y)) * this.tDeltaY);

            double absDirZ = Math.abs(dir[2]);
            this.stepZ = (absDirZ < RAY_DIRECTION_EPSILON) ? 0 : (dir[2] > 0 ? 1 : -1);
            this.tDeltaZ = (stepZ == 0) ? Double.POSITIVE_INFINITY : 1.0 / absDirZ;
            this.tMaxZ = (stepZ == 0) ? Double.POSITIVE_INFINITY : ((stepZ > 0 ? (this.z + 1 - this.pz) : (this.pz - this.z)) * this.tDeltaZ);

            this.initialised = true;
        }

        void trace() {
            if (!initialised) init();
            if (res <= 0) {
                latch.countDown();
                return;
            }

            while (res > 0) {
                if (y < minY || y >= minY + worldHeight || Thread.currentThread().isInterrupted()) break;
                if (currentRayPosition >= radius) break;

                int cx = x >> 4;
                int cz = z >> 4;
                int subY = y >> 4;
                if (cx != lastCX || cz != lastCZ || subY != lastSubY) {
                    currentSubChunkKey = new SubChunkKey(cx, cz, subY);
                    lastCX = cx;
                    lastCZ = cz;
                    lastSubY = subY;
                }

                SubChunkSnapshot snap = snapshots.get(currentSubChunkKey);
                if (snap == null) {
                    final boolean[] amFirst = {false};
                    ConcurrentLinkedQueue<RayTask> waiters = waitingRoom.computeIfAbsent(currentSubChunkKey, k -> {
                        amFirst[0] = true;
                        return new ConcurrentLinkedQueue<>();
                    });
                    if (amFirst[0]) highPriorityReactiveQueue.add(currentSubChunkKey);
                    waiters.add(this);
                    return;
                }

                if (snap != SubChunkSnapshot.EMPTY) {
                    BlockState state = snap.getBlockState(x & 0xF, y & 0xF, z & 0xF);
                    if (!state.isAir()) {
                        // fac — как в Legacy: затухание экспоненты с расстоянием
                        double fac = (100.0 - currentRayPosition / rayLength * 100.0) * 0.07D;
                        if (state.getFluidState().isEmpty()) {
                            res -= (float) Math.pow(masqueradeResistance(state), 7.5D - fac);
                        }
                        if (res > 0) {
                            int yNorm = y - minY;
                            int bitIndex = ((worldHeight - 1 - yNorm) << 8) | ((x & 0xF) << 4) | (z & 0xF);
                            destructionMap.computeIfAbsent(currentSubChunkKey.getPos(), posKey -> new ConcurrentBitSet(bitsetSize)).set(bitIndex);
                        }
                    }
                }

                double tExitVoxel = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
                this.currentRayPosition = tExitVoxel;
                if (res <= 0 || currentRayPosition >= radius) break;

                if (tMaxX < tMaxY) {
                    if (tMaxX < tMaxZ) {
                        x += stepX;
                        tMaxX += tDeltaX;
                    } else {
                        z += stepZ;
                        tMaxZ += tDeltaZ;
                    }
                } else {
                    if (tMaxY < tMaxZ) {
                        y += stepY;
                        tMaxY += tDeltaY;
                    } else {
                        z += stepZ;
                        tMaxZ += tDeltaZ;
                    }
                }
            }
            latch.countDown();
        }
    }
}
