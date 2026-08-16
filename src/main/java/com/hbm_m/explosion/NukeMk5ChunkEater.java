package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * chunk-by-chunk "crater" for Fat Man / MK5.
 * Do not confuse with {@link com.hbm_m.util.explosions.nuclear.CraterGenerator} (used by other charges).
 */
public class NukeMk5ChunkEater implements IExplosionRay {

    /** Точечный тикет прогрузки чанка на фазе разрушения; снимается после обработки. */
    private static final TicketType<ChunkPos> EATER_LOAD =
            TicketType.create("hbm_m_eater_load", Comparator.comparingLong(p -> (long) p.x << 32 ^ p.z));

    public final Map<ChunkPos, TipStore> perChunk = new HashMap<>();
    public final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final Comparator<ChunkPos> comparator = new CoordComparator();
    /**
     * Мини-набор long-ключей чанков, пересечённых лучом. Луч проходит ≤ ~140 чанков,
     * линейный поиск по long[] в разы быстрее HashMap.put + ChunkPos.hashCode
     * (замер spark: HashSet.add съедал 22% тика при радиусе 555).
     */
    private long[] rayChunkKeys = new long[64];
    private int rayChunkCount = 0;

    private final int posX;
    private final int posY;
    private final int posZ;
    private final Level level;
    private final int strength;
    private final int length;
    private final int speed;

    private int gspNumMax;
    private int gspNum;
    private double gspX;
    private double gspY;
    /** Переиспользуемая позиция для чтения блоков — без мусора BlockPos на каждый шаг луча. */
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

    public boolean isAusf3Complete = false;

    // ── Кеш чанка для чтения блоков ─────────────────────────────────────────
    // Level.getBlockState на КАЖДЫЙ блок лезет в chunk map сервера (замер: ~60%
    // тика взрыва уходило в getChunkFutureMainThread). Лучи почти всегда читают
    // блоки одного и того же чанка подряд — кешируем LevelChunk и читаем напрямую.
    private LevelChunk cachedChunk;
    private int cachedChunkX = Integer.MIN_VALUE;
    private int cachedChunkZ = Integer.MIN_VALUE;

    /** Выданные точечные тикеты прогрузки (фаза разрушения). */
    private final Map<ChunkPos, ChunkPos> eaterTickets = new HashMap<>();
    /** Последний лог ожидания прогрузки (throttle 5 сек, чтобы не спамить). */
    private long lastWaitLog = 0L;
    private boolean destructionLogged = false;
    private boolean completeLogged = false;

    public NukeMk5ChunkEater(Level level, int x, int y, int z, int strength, int speed, int length) {
        this.level = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.speed = speed;
        this.length = length;
        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNum = 1;
        this.gspX = Math.PI;
        this.gspY = 0.0;
    }

    /**
     * Чтение блока с кешем чанка. Для незагруженного чанка возвращает null —
     * синхронная загрузка запрещена (блокирует серверный поток).
     * [FIX] getChunkNow вместо hasChunk+getChunk: Level.getChunk(require=true) на КАЖДЫЙ
     * кеш-мисс добавляет UNKNOWN-тикет в DistanceManager (замер spark: 15% тика).
     * getChunkNow — чистый lookup в chunk map без тикетов и без загрузки.
     */
    private BlockState blockAt(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        if (cx != cachedChunkX || cz != cachedChunkZ) {
            cachedChunk = level.getChunkSource().getChunkNow(cx, cz);
            cachedChunkX = cx;
            cachedChunkZ = cz;
        }
        if (cachedChunk == null) {
            return null;
        }
        scratchPos.set(x, y, z);
        return cachedChunk.getBlockState(scratchPos);
    }

    /** Добавляет long-ключ чанка в мини-набор луча (без бокса и hashCode). */
    private void rayChunkAdd(long key) {
        for (int i = 0; i < rayChunkCount; i++) {
            if (rayChunkKeys[i] == key) return;
        }
        if (rayChunkCount == rayChunkKeys.length) {
            long[] grown = new long[rayChunkKeys.length * 2];
            System.arraycopy(rayChunkKeys, 0, grown, 0, rayChunkCount);
            rayChunkKeys = grown;
        }
        rayChunkKeys[rayChunkCount++] = key;
    }

    private void generateGspUp() {
        if (this.gspNum < this.gspNumMax) {
            int k = this.gspNum + 1;
            double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
            this.gspX = Math.acos(hk);
            double prevLon = this.gspY;
            double lon = prevLon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
            this.gspY = lon % (Math.PI * 2);
        } else {
            this.gspX = 0.0;
            this.gspY = 0.0;
        }
        this.gspNum++;
    }

    public void collectTip(int count) {
        collectTip(count, 0L);
    }

    /**
     * Сбор лучей с опциональным тайм-бюджетом.
     * {@code count} — гарантированный минимум за тик (парити с 1.7.10: speed*10);
     * после его достижения работа продолжается, пока не истечёт deadline.
     * Без бюджета (deadline <= 0) обрабатывается ровно count лучей, как в оригинале.
     */
    public void collectTip(int count, long deadline) {
        int amountProcessed = 0;
        int rayLength = (int) Math.ceil(strength);

        while (this.gspNumMax >= this.gspNum) {
            // направление инлайном: 9.6 млн Vec3 на больших радиусах душили GC
            double dirX = Math.sin(this.gspX) * Math.cos(this.gspY);
            double dirZ = Math.sin(this.gspX) * Math.sin(this.gspY);
            double dirY = Math.cos(this.gspX);

            float res = strength;
            boolean hasLastPos = false;
            float lastX = 0, lastY = 0, lastZ = 0;
            rayChunkCount = 0;

            for (int i = 0; i < rayLength; i++) {
                if (i > this.length) break;

                float x0 = (float) (posX + (dirX * i));
                float y0 = (float) (posY + (dirY * i));
                float z0 = (float) (posZ + (dirZ * i));

                int iX = (int) Math.floor(x0);
                int iY = (int) Math.floor(y0);
                int iZ = (int) Math.floor(z0);

                double fac = 100 - ((double) i) / ((double) rayLength) * 100;
                fac *= 0.07D;

                // null = чанк не загружен: считаем воздухом (без сопротивления и типсов там)
                BlockState state = blockAt(iX, iY, iZ);

                if (state != null) {
                    if (state.getFluidState().isEmpty()) {
                        res -= (float) Math.pow(masqueradeResistance(level, state, scratchPos), 7.5D - fac);
                    }

                    if (res > 0 && !state.isAir()) {
                        hasLastPos = true;
                        lastX = x0;
                        lastY = y0;
                        lastZ = z0;
                        rayChunkAdd(ChunkPos.asLong(iX >> 4, iZ >> 4));
                    }
                }

                if (res <= 0 || i + 1 >= this.length || i == rayLength - 1) {
                    break;
                }
            }

            if (hasLastPos) {
                for (int i = 0; i < rayChunkCount; i++) {
                    ChunkPos pos = new ChunkPos(rayChunkKeys[i]);
                    TipStore store = perChunk.get(pos);
                    if (store == null) {
                        store = new TipStore();
                        perChunk.put(pos, store);
                    }
                    store.add(lastX, lastY, lastZ);
                }
            }

            this.generateGspUp();
            amountProcessed++;
            if (amountProcessed >= count) {
                // минимум набрали: без бюджета выходим сразу (поведение 1.7.10),
                // с бюджетом продолжаем, пока есть время в тике
                if (deadline <= 0L || System.currentTimeMillis() >= deadline) {
                    return;
                }
            }
        }

        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(comparator);
        isAusf3Complete = true;
    }

    public static float masqueradeResistance(Level level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block == Blocks.SANDSTONE) {
            return Blocks.STONE.getExplosionResistance();
        }
        if (block == Blocks.OBSIDIAN) {
            return Blocks.STONE.getExplosionResistance() * 3;
        }
        return state.getBlock().getExplosionResistance();
    }

    private class CoordComparator implements Comparator<ChunkPos> {
        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {
            int chunkX = posX >> 4;
            int chunkZ = posZ >> 4;
            int diff1 = Math.abs(chunkX - o1.x) + Math.abs(chunkZ - o1.z);
            int diff2 = Math.abs(chunkX - o2.x) + Math.abs(chunkZ - o2.z);
            return diff1 - diff2;
        }
    }

    public void processChunk() {
        if (this.perChunk.isEmpty()) return;

        ChunkPos coord = orderedChunks.get(0);
        TipStore store = perChunk.get(coord);
        Set<BlockPos> toRem = new HashSet<>();
        Set<BlockPos> toRemTips = new HashSet<>();

        int chunkX = coord.x;
        int chunkZ = coord.z;

        int enter = Math.min(
                Math.abs(posX - (chunkX << 4)),
                Math.abs(posZ - (chunkZ << 4))) - 16;
        enter = Math.max(enter, 0);

        for (int t = 0; t < store.size(); t++) {
            float x = store.getX(t);
            float y = store.getY(t);
            float z = store.getZ(t);
            Vec3 vec = new Vec3(x - this.posX, y - this.posY, z - this.posZ);
            double len = vec.length();
            if (len <= 0) continue;
            double pX = vec.x / len;
            double pY = vec.y / len;
            double pZ = vec.z / len;

            int tipX = (int) Math.floor(x);
            int tipY = (int) Math.floor(y);
            int tipZ = (int) Math.floor(z);

            boolean inChunk = false;
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int i = enter; i < len; i++) {
                int x0 = (int) Math.floor(posX + pX * i);
                int y0 = (int) Math.floor(posY + pY * i);
                int z0 = (int) Math.floor(posZ + pZ * i);

                mutablePos.set(x0, y0, z0);
                BlockState state = blockAt(x0, y0, z0);
                if (state == null) {
                    continue;
                }

                if ((x0 >> 4) != chunkX || (z0 >> 4) != chunkZ) {
                    if (inChunk) {
                        break;
                    } else {
                        continue;
                    }
                }
                inChunk = true;

                if (shouldClearBlock(state)) {
                    BlockPos pos = new BlockPos(x0, y0, z0);
                    if (x0 == tipX && y0 == tipY && z0 == tipZ) {
                        toRemTips.add(pos);
                    }
                    toRem.add(pos);
                }
            }
        }

        for (BlockPos pos : toRem) {
            if (toRemTips.contains(pos)) {
                handleTip(pos.getX(), pos.getY(), pos.getZ());
            } else {
                clearBlock(pos);
            }
        }

        clearFluidsInChunkColumn(chunkX, chunkZ);

        perChunk.remove(coord);
        orderedChunks.remove(0);
    }

    /** Удаляет жидкости в колонке чанка внутри радиуса кратера (1.20: иначе остаются полоски воды). */
    private void clearFluidsInChunkColumn(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        int maxR = this.length;
        int maxRSq = maxR * maxR;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        // [FIX] Сканируем ПОЛНУЮ высоту колонки (minY..maxY), а не surfaceY+16.
        // После прохода лучей heightmap падает до дна кратера, и вода на уровне
        // океана оказывалась выше surfaceY+16 → не сканировалась → оставалась сеткой.
        // Границы расширены на 1 блок для перекрытия стыков чанков.
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                double dx = x + 0.5D - posX;
                double dz = z + 0.5D - posZ;
                if (dx * dx + dz * dz > (double) maxRSq) {
                    continue;
                }
                for (int y = minY; y < maxY; y++) {
                    mutablePos.set(x, y, z);
                    BlockState state = blockAt(x, y, z);
                    if (state != null && !state.getFluidState().isEmpty()) {
                        clearBlock(mutablePos);
                    }
                }
            }
        }
    }

    private boolean fluidsCleared = false;
    private boolean fluidClearInProgress = false;
    private int fluidClearCursorX;

    /** Финальный проход по сфере — убирает воду, просочившуюся между тиками обработки чанков. */
    public void clearRemainingFluidsInCrater(int budgetMs) {
        if (fluidsCleared) {
            return;
        }

        int maxR = this.length;
        if (!fluidClearInProgress) {
            fluidClearInProgress = true;
            fluidClearCursorX = posX - maxR;
        }

        long deadline = System.currentTimeMillis() + budgetMs;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        outer:
        for (; fluidClearCursorX <= posX + maxR; fluidClearCursorX++) {
            for (int z = posZ - maxR; z <= posZ + maxR; z++) {
                if (System.currentTimeMillis() >= deadline) {
                    break outer;
                }

                double dx = fluidClearCursorX + 0.5D - posX;
                double dz = z + 0.5D - posZ;
                if (dx * dx + dz * dz > (double) maxR * maxR) {
                    continue;
                }

                // [FIX] Полная высота колонки (см. комментарий в clearFluidsInChunkColumn)
                for (int y = minY; y < maxY; y++) {
                    mutablePos.set(fluidClearCursorX, y, z);
                    BlockState state = blockAt(fluidClearCursorX, y, z);
                    if (state != null && !state.getFluidState().isEmpty()) {
                        clearBlock(mutablePos);
                    }
                }
            }
        }

        if (fluidClearCursorX > posX + maxR) {
            fluidsCleared = true;
        }
    }

    protected void handleTip(int x, int y, int z) {
        clearBlock(new BlockPos(x, y, z));
    }

    /**
     * Удаляет блок/жидкость.
     * [FIX] Level.removeBlock(pos, false) — это NO-OP для жидкостей в ванилле 1.20.1:
     * он вызывает setBlock(pos, fluidstate.createLegacyBlock(), 3), т.е. ставит воду
     * ОБРАТНО. Поэтому используем setBlock(AIR) напрямую.
     * Флаг UPDATE_CLIENTS (2) — без neighbor-update, чтобы соседние source-блоки
     * не получили уведомления и не затекли обратно.
     */
    private void clearBlock(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() && state.getFluidState().isEmpty()) return;
        // [FIX] flag UPDATE_CLIENTS | UPDATE_IMMEDIATE (18):
        // UPDATE_IMMEDIATE пропускает updateNeighbourShapes в Level.markAndNotifyBlock,
        // иначе соседняя вода получает neighborChanged и растекается.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
    }

    private static boolean shouldClearBlock(BlockState state) {
        return !state.isAir() || !state.getFluidState().isEmpty();
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public void cacheChunksTick(int processTimeMs) {
        if (!isAusf3Complete) {
            // Пока область кратера не загружена (тикет сущности грузит её асинхронно),
            // сбор лучей не начинаем: иначе лучи уходят в незагруженные чанки,
            // портят форму кратера и провоцируют синхронную загрузку.
            if (ModClothConfig.get().enableChunkLoading && !isAreaLoaded()) {
                long now = System.currentTimeMillis();
                if (now - lastWaitLog >= 5_000L) {
                    lastWaitLog = now;
                    MainRegistry.LOGGER.info("[NUKE MK5] Waiting for crater area to load: {} chunks not loaded",
                            countMissingChunks());
                }
                // Тикет сущности не покрывает внешнее кольцо области (кап радиуса 31):
                // догружаем недостающие чанки сами точечными тикетами радиуса 2 (FULL).
                requestMissingChunks();
                return;
            }
            if (lastWaitLog != 0L) {
                MainRegistry.LOGGER.info("[NUKE MK5] Crater area loaded, starting ray collection (rays={}, chunks affected={})",
                        gspNumMax, perChunk.size());
                lastWaitLog = 0L;
            }
            // [FIX] В оригинале бюджет времени тут игнорировался (фикс. speed*10 лучей/тик),
            // из-за чего у больших зарядов (FatMan: ~962k лучей) сбор занимал ~17 сек
            // до первого разрушенного блока. Теперь минимум остаётся speed*10,
            // но при запасе времени в тике сбор ускоряется насколько возможно.
            collectTip(speed * 10, System.currentTimeMillis() + Math.max(1L, processTimeMs));
        }
    }

    /** Все чанки-колонки в сфере лучей загружены? (hasChunk — без синхронной загрузки) */
    private boolean isAreaLoaded() {
        return countMissingChunks() == 0;
    }

    /** Число чанков сферы лучей, ещё не загруженных полностью (для гейта и лога). */
    private int countMissingChunks() {
        int cr = (length + 15) >> 4;
        int cx0 = posX >> 4;
        int cz0 = posZ >> 4;
        int margin = length + 14;
        long marginSq = (long) margin * margin;
        int missing = 0;
        for (int cx = cx0 - cr; cx <= cx0 + cr; cx++) {
            for (int cz = cz0 - cr; cz <= cz0 + cr; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    double dx = (cx << 4) + 8 - posX;
                    double dz = (cz << 4) + 8 - posZ;
                    if (dx * dx + dz * dz <= (double) marginSq) {
                        missing++;
                    }
                }
            }
        }
        return missing;
    }

    /**
     * Точечные тикеты (радиус 2 = FULL) для всех ещё не загруженных чанков области —
     * тикет сущности не дотягивается до внешнего кольца при больших радиусах.
     * Тикеты снимаются по мере обработки чанков в destructionTick (releaseEaterTicket).
     */
    private void requestMissingChunks() {
        if (!(level instanceof ServerLevel server)) return;
        int cr = (length + 15) >> 4;
        int cx0 = posX >> 4;
        int cz0 = posZ >> 4;
        int margin = length + 14;
        long marginSq = (long) margin * margin;
        for (int cx = cx0 - cr; cx <= cx0 + cr; cx++) {
            for (int cz = cz0 - cr; cz <= cz0 + cr; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    double dx = (cx << 4) + 8 - posX;
                    double dz = (cz << 4) + 8 - posZ;
                    if (dx * dx + dz * dz <= (double) marginSq) {
                        ChunkPos cp = new ChunkPos(cx, cz);
                        if (eaterTickets.putIfAbsent(cp, cp) == null) {
                            server.getChunkSource().addRegionTicket(EATER_LOAD, cp, 2, cp);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void destructionTick(int processTimeMs) {
        if (!isAusf3Complete) return;
        if (!destructionLogged) {
            destructionLogged = true;
            MainRegistry.LOGGER.info("[NUKE MK5] Ray collection done, starting terrain destruction: {} chunks queued", orderedChunks.size());
        }
        long start = System.currentTimeMillis();
        int deferred = 0;
        while (!perChunk.isEmpty() && System.currentTimeMillis() < start + processTimeMs) {
            ChunkPos coord = orderedChunks.get(0);
            if (!level.hasChunk(coord.x, coord.z)) {
                // чанк выгрузился: точечный тикет и в конец очереди — setBlock/getBlockState
                // по незагруженному чанку синхронно грузят его и блокируют поток
                if (level instanceof ServerLevel server) {
                    // радиус 2 обязателен: уровень тикета = 33-2+0 = 31 = FULL.
                    // Радиус 0 держит чанк на уровне 33 (border) — hasChunk всегда false,
                    // чанк НИКОГДА не прогрузится и разрушение зависнет навсегда.
                    if (eaterTickets.putIfAbsent(coord, coord) == null) {
                        server.getChunkSource().addRegionTicket(EATER_LOAD, coord, 2, coord);
                    }
                }
                orderedChunks.remove(0);
                orderedChunks.add(coord);
                // все оставшиеся не загружены — выходим, повторим в следующем тике
                if (++deferred >= orderedChunks.size()) {
                    break;
                }
                continue;
            }
            releaseEaterTicket(coord);
            processChunk();
            deferred = 0;
        }
        if (isAusf3Complete && perChunk.isEmpty()) {
            clearRemainingFluidsInCrater(processTimeMs);
            if (fluidsCleared && !completeLogged) {
                completeLogged = true;
                // снять тикеты чанков, выданных гейтом для области, но не вошедших в
                // кратер (perChunk уже пуст) — иначе они останутся загруженными навсегда
                if (!eaterTickets.isEmpty() && level instanceof ServerLevel server) {
                    for (ChunkPos cp : eaterTickets.keySet()) {
                        server.getChunkSource().removeRegionTicket(EATER_LOAD, cp, 2, cp);
                    }
                    eaterTickets.clear();
                }
                MainRegistry.LOGGER.info("[NUKE MK5] Terrain destruction complete");
            }
        }
    }

    private void releaseEaterTicket(ChunkPos coord) {
        if (eaterTickets.remove(coord) != null && level instanceof ServerLevel server) {
            server.getChunkSource().removeRegionTicket(EATER_LOAD, coord, 2, coord);
        }
    }

    @Override
    public void cancel() {
        isAusf3Complete = true;
        if (perChunk != null) perChunk.clear();
        if (orderedChunks != null) orderedChunks.clear();
        // снять незакрытые точечные тикеты, иначе чанки зависнут загруженными
        if (!eaterTickets.isEmpty() && level instanceof ServerLevel server) {
            for (ChunkPos cp : eaterTickets.keySet()) {
                server.getChunkSource().removeRegionTicket(EATER_LOAD, cp, 2, cp);
            }
            eaterTickets.clear();
        }
    }

    /**
     * Упакованное хранилище типсов лучей: сплошной массив float вместо объектов
     * FloatTriplet. У больших зарядов типсов десятки миллионов — объектная обёртка
     * (заголовок ~16-24Б + ссылка) удваивала-утраивала потребление RAM.
     */
    public static class TipStore {
        private float[] data = new float[256];
        private int size;

        public void add(float x, float y, float z) {
            if (size + 3 > data.length) {
                float[] grown = new float[Math.max(data.length * 2, size + 3)];
                System.arraycopy(data, 0, grown, 0, size);
                data = grown;
            }
            data[size] = x;
            data[size + 1] = y;
            data[size + 2] = z;
            size += 3;
        }

        /** Число типсов (не float'ов). */
        public int size() {
            return size / 3;
        }

        public float getX(int tipIndex) {
            return data[tipIndex * 3];
        }

        public float getY(int tipIndex) {
            return data[tipIndex * 3 + 1];
        }

        public float getZ(int tipIndex) {
            return data[tipIndex * 3 + 2];
        }
    }
}
