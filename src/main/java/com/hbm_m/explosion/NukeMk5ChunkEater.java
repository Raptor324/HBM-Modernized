package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

public class NukeMk5ChunkEater implements IExplosionRay {

    // Флаг 50 = UPDATE_CLIENTS(2) | UPDATE_KNOWN_SHAPE(16) | UPDATE_SUPPRESS_DROPS(32)
    // Гарантированно глушит updateNeighbourShapes и выпадение дропа!
    public static final int FAST_BLOCK_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final TicketType<ChunkPos> EATER_LOAD =
            TicketType.create("hbm_m_eater_load", Comparator.comparingLong(p -> (long) p.x << 32 ^ p.z));

    public final Map<ChunkPos, TipStore> perChunk = new HashMap<>();
    public final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final Comparator<ChunkPos> comparator = new CoordComparator();

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
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

    public boolean isAusf3Complete = false;

    // Быстрый 2-уровневый кэш: чанк + активная секция 16x16x16
    private LevelChunk cachedChunk;
    private int cachedChunkX = Integer.MIN_VALUE;
    private int cachedChunkZ = Integer.MIN_VALUE;
    private LevelChunkSection cachedSection;
    private int cachedSectionIdx = Integer.MIN_VALUE;

    private final Map<ChunkPos, ChunkPos> eaterTickets = new HashMap<>();
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
        // Ограничиваем предельное количество лучей, сохраняя идеальную плотность сферы
        int rawGsp = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNumMax = Math.min(rawGsp, 1_500_000);
        this.gspNum = 1;
        this.gspX = Math.PI;
        this.gspY = 0.0;
    }

    private BlockState blockAt(int x, int y, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        if (cx != cachedChunkX || cz != cachedChunkZ) {
            cachedChunk = level.getChunkSource().getChunkNow(cx, cz);
            cachedChunkX = cx;
            cachedChunkZ = cz;
            cachedSection = null;
            cachedSectionIdx = Integer.MIN_VALUE;
        }
        if (cachedChunk == null) return null;

        int sIdx = level.getSectionIndex(y);
        if (sIdx < 0 || sIdx >= cachedChunk.getSections().length) return null;

        if (sIdx != cachedSectionIdx) {
            cachedSection = cachedChunk.getSections()[sIdx];
            cachedSectionIdx = sIdx;
        }

        if (cachedSection == null || cachedSection.hasOnlyAir()) {
            return Blocks.AIR.defaultBlockState();
        }

        return cachedSection.getBlockState(x & 15, y & 15, z & 15);
    }

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

    public void collectTip(int count, long deadline) {
        int amountProcessed = 0;
        int rayLength = (int) Math.ceil(strength);

        while (this.gspNumMax >= this.gspNum) {
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

                BlockState state = blockAt(iX, iY, iZ);

                if (state != null) {
                    scratchPos.set(iX, iY, iZ);
                    float blockRes = masqueradeResistance(level, state, scratchPos);
                    res -= (float) Math.pow(blockRes, Math.max(1.0D, 7.5D - fac));

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
                    TipStore store = perChunk.computeIfAbsent(pos, k -> new TipStore());
                    store.add(lastX, lastY, lastZ);
                }
            }

            this.generateGspUp();
            amountProcessed++;
            if (amountProcessed >= count) {
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
        if (!state.getFluidState().isEmpty()) {
            return 0.05F; // Жидкости не тормозят ударную волну
        }
        Block block = state.getBlock();
        if (block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE) {
            return Blocks.STONE.getExplosionResistance();
        }
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return Blocks.STONE.getExplosionResistance() * 2.5F;
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
        if (store == null) {
            orderedChunks.remove(0);
            return;
        }

        LongSet toRem = new LongOpenHashSet(store.size() * 4);
        LongSet toRemTips = new LongOpenHashSet();

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
            for (int i = enter; i < len; i++) {
                int x0 = (int) Math.floor(posX + pX * i);
                int y0 = (int) Math.floor(posY + pY * i);
                int z0 = (int) Math.floor(posZ + pZ * i);

                if ((x0 >> 4) != chunkX || (z0 >> 4) != chunkZ) {
                    if (inChunk) break;
                    else continue;
                }
                inChunk = true;

                BlockState state = blockAt(x0, y0, z0);
                if (state == null) continue;

                if (shouldClearBlock(state)) {
                    long packed = BlockPos.asLong(x0, y0, z0);
                    if (x0 == tipX && y0 == tipY && z0 == tipZ) {
                        toRemTips.add(packed);
                    }
                    toRem.add(packed);
                }
            }
        }

        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        for (long packed : toRem) {
            mutPos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (toRemTips.contains(packed)) {
                handleTip(mutPos.getX(), mutPos.getY(), mutPos.getZ());
            } else {
                clearBlock(mutPos);
            }
        }

        // Очистка жидкостей
        clearFluidsInChunkColumn(chunkX, chunkZ);

        // Ультрабыстрый де-спайкинг (только по границе удаленных блоков!)
        cleanupCraterSpikesFast(chunkX, chunkZ, toRem);

        perChunk.remove(coord);
        orderedChunks.remove(0);
    }

    /**
     * Высокопроизводительный алгоритм удаления "палок" и артефактов затенения.
     * Проверяет ТОЛЬКО блоки, соприкасающиеся с зоной разрушения (занимает < 0.05ms!).
     */
    private void cleanupCraterSpikesFast(int chunkX, int chunkZ, LongSet destroyedBlocks) {
        if (destroyedBlocks.isEmpty()) return;

        LongSet candidates = new LongOpenHashSet();
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        int maxRSq = this.length * this.length;

        // Собираем кандидатов на проверку: только твердые блоки вокруг удаленных
        for (long packed : destroyedBlocks) {
            int bx = BlockPos.getX(packed);
            int by = BlockPos.getY(packed);
            int bz = BlockPos.getZ(packed);

            addCandidate(bx + 1, by, bz, minX, maxX, minZ, maxZ, maxRSq, candidates);
            addCandidate(bx - 1, by, bz, minX, maxX, minZ, maxZ, maxRSq, candidates);
            addCandidate(bx, by + 1, bz, minX, maxX, minZ, maxZ, maxRSq, candidates);
            addCandidate(bx, by - 1, bz, minX, maxX, minZ, maxZ, maxRSq, candidates);
            addCandidate(bx, by, bz + 1, minX, maxX, minZ, maxZ, maxRSq, candidates);
            addCandidate(bx, by, bz - 1, minX, maxX, minZ, maxZ, maxRSq, candidates);
        }

        LongSet toPurge = new LongOpenHashSet();
        for (long packed : candidates) {
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);

            int airNeighbors = 0;
            boolean oppXAir = isAirOrFluid(x - 1, y, z) && isAirOrFluid(x + 1, y, z);
            boolean oppYAir = isAirOrFluid(x, y - 1, z) && isAirOrFluid(x, y + 1, z);
            boolean oppZAir = isAirOrFluid(x, y, z - 1) && isAirOrFluid(x, y, z + 1);

            if (isAirOrFluid(x + 1, y, z)) airNeighbors++;
            if (isAirOrFluid(x - 1, y, z)) airNeighbors++;
            if (isAirOrFluid(x, y + 1, z)) airNeighbors++;
            if (isAirOrFluid(x, y - 1, z)) airNeighbors++;
            if (isAirOrFluid(x, y, z + 1)) airNeighbors++;
            if (isAirOrFluid(x, y, z - 1)) airNeighbors++;

            if (airNeighbors >= 4 || (oppXAir && oppZAir) || (oppYAir && (oppXAir || oppZAir))) {
                toPurge.add(packed);
            }
        }

        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        for (long packed : toPurge) {
            cur.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            clearBlock(cur);
        }
    }

    private void addCandidate(int x, int y, int z, int minX, int maxX, int minZ, int maxZ, int maxRSq, LongSet candidates) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) return;
        double dx = x + 0.5D - posX;
        double dy = y + 0.5D - posY;
        double dz = z + 0.5D - posZ;
        if (dx * dx + dy * dy + dz * dz > maxRSq) return;

        BlockState state = blockAt(x, y, z);
        if (state == null || state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() == Blocks.BEDROCK) return;

        candidates.add(BlockPos.asLong(x, y, z));
    }

    private boolean isAirOrFluid(int x, int y, int z) {
        BlockState state = blockAt(x, y, z);
        return state == null || state.isAir() || !state.getFluidState().isEmpty();
    }

    private void clearFluidsInChunkColumn(int chunkX, int chunkZ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) return;

        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        int maxRSq = this.length * this.length;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section == null || section.hasOnlyAir()) continue;

            int sectionMinY = level.getSectionYFromSectionIndex(s) << 4;
            int sectionMaxY = sectionMinY + 15;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = x + 0.5D - posX;
                    double dz = z + 0.5D - posZ;
                    if (dx * dx + dz * dz > (double) maxRSq) continue;

                    for (int y = sectionMinY; y <= sectionMaxY; y++) {
                        mutablePos.set(x, y, z);
                        BlockState state = chunk.getBlockState(mutablePos);
                        if (!state.getFluidState().isEmpty()) {
                            clearBlock(mutablePos);
                        }
                    }
                }
            }
        }
    }

    private boolean fluidsCleared = false;
    private boolean fluidClearInProgress = false;
    private int fluidClearCursorX;

    public void clearRemainingFluidsInCrater(int budgetMs) {
        if (fluidsCleared) return;

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

    private void clearBlock(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() && state.getFluidState().isEmpty()) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FAST_BLOCK_FLAGS);
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
            if (ModClothConfig.get().enableChunkLoading && !isAreaLoaded()) {
                long now = System.currentTimeMillis();
                if (now - lastWaitLog >= 5_000L) {
                    lastWaitLog = now;
                    MainRegistry.LOGGER.info("[NUKE MK5] Waiting for crater area to load: {} chunks not loaded",
                            countMissingChunks());
                }
                requestMissingChunks();
                return;
            }
            if (lastWaitLog != 0L) {
                MainRegistry.LOGGER.info("[NUKE MK5] Crater area loaded, starting ray collection (rays={}, chunks affected={})",
                        gspNumMax, perChunk.size());
                lastWaitLog = 0L;
            }
            collectTip(speed * 10, System.currentTimeMillis() + Math.max(1L, processTimeMs));
        }
    }

    private boolean isAreaLoaded() {
        return countMissingChunks() == 0;
    }

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
                if (level instanceof ServerLevel server) {
                    if (eaterTickets.putIfAbsent(coord, coord) == null) {
                        server.getChunkSource().addRegionTicket(EATER_LOAD, coord, 2, coord);
                    }
                }
                orderedChunks.remove(0);
                orderedChunks.add(coord);
                if (++deferred >= orderedChunks.size()) break;
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