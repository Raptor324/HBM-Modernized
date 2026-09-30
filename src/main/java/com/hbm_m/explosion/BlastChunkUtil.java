// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

import com.hbm_m.platform.LevelHooks;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Чанковая обвязка движка взрыва MK5: зеркальная карта загруженных чанков для
 * off-thread чтений, per-chunk blast-тикеты (radius 0, FULL) с подсчётом ссылок,
 * вырезание блоков по маске прямо в секциях, отложенный свет и wholesale-ресенд.
 */
public final class BlastChunkUtil {

    private static final TicketType<ChunkPos> BLAST_LOAD = TicketType.create(
            "hbm_m_blast_load", java.util.Comparator.comparingLong(ChunkPos::toLong));

    private static final BlockState AIR_DEFAULT_STATE = Blocks.AIR.defaultBlockState();

    private static final Map<net.minecraft.resources.ResourceLocation, NonBlockingHashMapLong<LevelChunk>> chunkMap =
            new WeakHashMap<>();
    private static final Map<ServerLevel, Long2IntOpenHashMap> BLAST_LOAD_CLAIMS = new WeakHashMap<>();
    private static final Object MAP_LOCK = new Object();
    private static final java.util.concurrent.atomic.AtomicInteger activeTask =
            new java.util.concurrent.atomic.AtomicInteger();
    private static volatile boolean eventsRegistered;

    private static final ThreadLocal<int[]> TL_CARVED = ThreadLocal.withInitial(() -> new int[4096]);

    /** Радиус удержания чанка сущностью-взрывом (как у жемчуга Края). */
    public static final int HOLD_RADIUS = 2;

    private BlastChunkUtil() {}

    // ── Зеркальная карта ────────────────────────────────────────────────────

    /** Потокобезопасно (вызывается с серверного потока). */
    public static NonBlockingHashMapLong<LevelChunk> acquireMirrorMap(ServerLevel level) {
        ensureEventsRegistered();
        net.minecraft.resources.ResourceLocation dim = level.dimension().location();
        NonBlockingHashMapLong<LevelChunk> thisDim;
        synchronized (MAP_LOCK) {
            if (activeTask.getAndIncrement() == 0) {
                thisDim = new NonBlockingHashMapLong<>(4096);
                for (ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
                    LevelChunk chunk = fullChunkIfLoaded(holder);
                    if (chunk != null) thisDim.put(chunk.getPos().toLong(), chunk);
                }
                chunkMap.put(dim, thisDim);
            } else {
                thisDim = chunkMap.get(dim);
            }
        }
        return Objects.requireNonNull(thisDim);
    }

    public static void releaseMirrorMap(ServerLevel level) {
        net.minecraft.resources.ResourceLocation dim = level.dimension().location();
        synchronized (MAP_LOCK) {
            if (activeTask.decrementAndGet() == 0) {
                chunkMap.remove(dim);
            }
        }
    }

    public static LevelChunk getLoadedChunk(NonBlockingHashMapLong<LevelChunk> mirror, long chunkPos) {
        return mirror.get(chunkPos);
    }

    public static LevelChunkSection[] getLoadedSections(NonBlockingHashMapLong<LevelChunk> mirror, long chunkPos) {
        LevelChunk chunk = getLoadedChunk(mirror, chunkPos);
        return chunk == null ? null : chunk.getSections();
    }

    public static LevelChunk liveChunkNow(ServerLevel level, long chunkPos) {
        return fullChunkIfLoaded(
                level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos));
    }

    private static LevelChunk fullChunkIfLoaded(ChunkHolder holder) {
        if (holder == null
                || holder.getTicketLevel()
                        > net.minecraft.server.level.ChunkLevel.byStatus(FullChunkStatus.FULL)) {
            return null;
        }
        return joinFullChunk(holder);
    }

    // ── Blast-тикеты ────────────────────────────────────────────────────────

    public static void addBlastTicket(ServerLevel level, long chunkPos) {
        Long2IntOpenHashMap held =
                BLAST_LOAD_CLAIMS.computeIfAbsent(level, ignored -> new Long2IntOpenHashMap());
        if (held.addTo(chunkPos, 1) > 0) return;
        ChunkPos pos = new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        level.getChunkSource().addRegionTicket(BLAST_LOAD, pos, 0, pos);
    }

    public static void flushChunkTickets(ServerLevel level) {
        // runDistanceManagerUpdates protected на обеих версиях (1.20.1 и 1.21.1) —
        // не вызываем: distance manager дойдёт до тикетов на ближайшем тике, admission
        // просто переочередит чанки (задержка в один тик).
    }

    public static CompletableFuture<LevelChunk> blastChunkFuture(ServerLevel level, long chunkPos) {
        ChunkHolder holder = level.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos);
        return holder == null ? null : fullChunkFuture(holder);
    }

    public static void releaseChunkAsync(ServerLevel level, long chunkPos) {
        Long2IntOpenHashMap held = BLAST_LOAD_CLAIMS.get(level);
        if (held == null) return;
        int count = held.addTo(chunkPos, -1);
        if (count > 1) return;
        held.remove(chunkPos);
        if (held.isEmpty()) BLAST_LOAD_CLAIMS.remove(level);
        ChunkPos pos = new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        level.getChunkSource().removeRegionTicket(BLAST_LOAD, pos, 0, pos);
    }

    public static void onServerStopped() {
        synchronized (MAP_LOCK) {
            chunkMap.clear();
            activeTask.set(0);
        }
        BLAST_LOAD_CLAIMS.clear();
    }

    // ── События чанков: держим зеркало актуальным, пока есть взрыв ──────────

    private static void ensureEventsRegistered() {
        if (eventsRegistered) return;
        eventsRegistered = true;
        //? if forge {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ForgeChunkEvents.class);
        //?} else {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(NeoForgeChunkEvents.class);
         *///?}
    }

    private static void onChunkLoad(ServerLevel server, LevelChunk chunk) {
        if (activeTask.get() == 0) return;
        NonBlockingHashMapLong<LevelChunk> mirror = chunkMap.get(server.dimension().location());
        if (mirror != null) mirror.put(chunk.getPos().toLong(), chunk);
    }

    private static void onChunkUnload(ServerLevel server, LevelChunk chunk) {
        if (activeTask.get() == 0) return;
        NonBlockingHashMapLong<LevelChunk> mirror = chunkMap.get(server.dimension().location());
        if (mirror != null) mirror.remove(chunk.getPos().toLong());
    }

    //? if forge {
    public static final class ForgeChunkEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onChunkLoad(net.minecraftforge.event.level.ChunkEvent.Load event) {
            if (event.getChunk() instanceof LevelChunk chunk
                    && event.getLevel() instanceof ServerLevel server) {
                BlastChunkUtil.onChunkLoad(server, chunk);
            }
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onChunkUnload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
            if (event.getChunk() instanceof LevelChunk chunk
                    && event.getLevel() instanceof ServerLevel server) {
                BlastChunkUtil.onChunkUnload(server, chunk);
            }
        }
    }
    //?} else {
    /*public static final class NeoForgeChunkEvents {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
            if (event.getChunk() instanceof LevelChunk chunk
                    && event.getLevel() instanceof ServerLevel server) {
                BlastChunkUtil.onChunkLoad(server, chunk);
            }
        }

        @net.neoforged.bus.api.SubscribeEvent
        public static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
            if (event.getChunk() instanceof LevelChunk chunk
                    && event.getLevel() instanceof ServerLevel server) {
                BlastChunkUtil.onChunkUnload(server, chunk);
            }
        }
    }
    *///?}

    // ── Вырезание по маске ──────────────────────────────────────────────────

    /**
     * Вырезает блоки секции снапшота по локальной маске. Потокобезопасно: снапшот
     * — приватные копии. Неблоковые позиции с BE не вырезаются (сервер очистит их
     * через {@link #replaceEmptied}), но в modifiedOut попадают — для heightmap/света.
     */
    public static boolean carveSnapshot(
            ServerLevel level,
            int chunkX,
            int chunkZ,
            int subY,
            SectionSnapshot snapshot,
            BitMask localMask,
            LongCollection edgeOut,
            it.unimi.dsi.fastutil.longs.Long2ObjectMap<BlockState> modifiedOut) {
        LevelChunkSection src = snapshot.sections[subY];
        if (src == null || src.hasOnlyAir()) return false;

        NonBlockingHashMapLong<LevelChunk> loaded;
        synchronized (MAP_LOCK) {
            loaded = chunkMap.get(level.dimension().location());
        }

        int xBase = chunkX << 4;
        int yBase = LevelHooks.minY(level) + (subY << 4);
        int zBase = chunkZ << 4;

        int[] carved = TL_CARVED.get();
        int carvedCount = 0;
        for (int idx = localMask.nextSetBit(0); idx >= 0 && idx < 4096; idx = localMask.nextSetBit(idx + 1)) {
            int xLocal = idx & 0xF;
            int yLocal = (idx >>> 8) & 0xF;
            int zLocal = (idx >>> 4) & 0xF;

            BlockState old = src.getBlockState(xLocal, yLocal, zLocal);
            if (old.isAir()) continue;

            long packedPos = BlockPos.asLong(xBase | xLocal, yBase | yLocal, zBase | zLocal);
            modifiedOut.put(packedPos, old);

            if (old.hasBlockEntity()) continue;

            if (touchesEdge(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal, zLocal, localMask)) {
                edgeOut.add(packedPos);
            }
            carved[carvedCount++] = idx;
        }
        if (carvedCount == 0) return false;
        // БЕЗ ЭТОГО publish() не свапает секции: маска changed остаётся пустой, вырезка
        // уходит в мусор вместе со снапшотом, и весь конвейер отрабатывает вхолостую
        // («Explosion complete», а кратера нет).
        snapshot.changed(subY);

        for (int i = 0; i < carvedCount; i++) {
            int idx = carved[i];
            src.setBlockState(idx & 0xF, (idx >>> 8) & 0xF, (idx >>> 4) & 0xF, AIR_DEFAULT_STATE, false);
        }
        return true;
    }

    private static boolean touchesEdge(
            NonBlockingHashMapLong<LevelChunk> loaded,
            int chunkX,
            int chunkZ,
            int subY,
            LevelChunkSection src,
            int xLocal,
            int yLocal,
            int zLocal,
            BitMask carveMask) {
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal - 1, yLocal, zLocal, carveMask)) return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal + 1, yLocal, zLocal, carveMask)) return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal - 1, zLocal, carveMask)) return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal + 1, zLocal, carveMask)) return true;
        if (checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal, zLocal - 1, carveMask)) return true;
        return checkNeighbor(loaded, chunkX, chunkZ, subY, src, xLocal, yLocal, zLocal + 1, carveMask);
    }

    private static boolean checkNeighbor(
            NonBlockingHashMapLong<LevelChunk> loaded,
            int chunkX,
            int chunkZ,
            int subY,
            LevelChunkSection src,
            int xLocal,
            int yLocal,
            int zLocal,
            BitMask carveMask) {
        if (xLocal >= 0 && xLocal < 16 && yLocal >= 0 && yLocal < 16 && zLocal >= 0 && zLocal < 16) {
            int idx = (yLocal << 8) | (zLocal << 4) | xLocal;
            if (carveMask.get(idx)) return false;
            return !src.getBlockState(xLocal, yLocal, zLocal).isAir();
        }

        int nCx = chunkX, nCz = chunkZ, nSubY = subY;
        int nx = xLocal, ny = yLocal, nz = zLocal;
        if (xLocal < 0) {
            nCx--;
            nx = 15;
        } else if (xLocal >= 16) {
            nCx++;
            nx = 0;
        }
        if (zLocal < 0) {
            nCz--;
            nz = 15;
        } else if (zLocal >= 16) {
            nCz++;
            nz = 0;
        }
        if (yLocal < 0) {
            nSubY--;
            ny = 15;
        } else if (yLocal >= 16) {
            nSubY++;
            ny = 0;
        }

        if (loaded == null) return true;
        LevelChunk neighbor = loaded.get(ChunkPos.asLong(nCx, nCz));
        if (neighbor == null) return true;
        LevelChunkSection[] nSections = neighbor.getSections();
        if (nSubY < 0 || nSubY >= nSections.length) return false;
        LevelChunkSection s = nSections[nSubY];
        return s != null && !s.hasOnlyAir() && !s.getBlockState(nx, ny, nz).isAir();
    }

    // ── Серверная доводка после публикации снапшота ─────────────────────────

    /** @return true, если свойства света изменились — позиция нужна light engine. */
    public static boolean postCarveBlockUpdate(
            ServerLevel level, LevelChunk chunk, BlockPos pos, BlockState oldState, BlockState newState) {
        dispatchRemovalHook(level, pos, oldState, newState);

        newState.onPlace(level, pos, oldState, false);
        int lx = pos.getX() & 15;
        int ly = pos.getY();
        int lz = pos.getZ() & 15;
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING).update(lx, ly, lz, newState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(lx, ly, lz, newState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR).update(lx, ly, lz, newState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE).update(lx, ly, lz, newState);
        return hasDifferentLightProperties(level, pos, oldState, newState);
    }

    private static boolean hasDifferentLightProperties(
            ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        return oldState.getLightBlock(level, pos) != newState.getLightBlock(level, pos)
                || oldState.getLightEmission() != newState.getLightEmission();
    }

    public static void replaceEmptied(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        BlockPos at = pos.immutable();
        if (level.getBlockState(at).hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(at);

            // на 1.20.1 setLootTable(ResourceLocation) нет — там достаточно очистить содержимое
            //? if < 1.21.1 {
            //?} else {
            /*if (blockEntity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity loot)
                loot.setLootTable(null);
            *///?}
            if (blockEntity instanceof net.minecraft.world.Container container) container.clearContent();
        }
        level.setBlock(at, state, flags);
    }

    public static void dispatchRemovalHook(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        // affectNeighborsAfterRemoval (реакция двойных сундуков/поршней на удаление
        // соседа) появился только в 1.21.2+. Общие shape-апдейты здесь НЕ делаем на
        // обеих версиях: они разбудили бы текучие блоки во время вырезания (wake-и
        // отложены до secondPass); периметр кратера получает апдейты через edge-список.
    }

    // ── Свет и рассылка ─────────────────────────────────────────────────────

    /** Сервер останавливается: блокирующие ожидания (свет, second pass) надо пропускать,
     * иначе shutdown навсегда паркуется на движке, чьи задачи уже никто не разбирает. */
    public static boolean serverShuttingDown(ServerLevel level) {
        net.minecraft.server.MinecraftServer server = level.getServer();
        return !server.isRunning() || server.isStopped() || server.isShutdown();
    }

    /** Диспетчер: bulk-путь (одна light-задача) через миксин, иначе индивидуально. */
    public static CompletableFuture<?> updateLight(
            ServerLevel level, LevelChunk chunk, LongList positions, long sectionMaskFromBottom) {
        if (serverShuttingDown(level)) {
            return CompletableFuture.completedFuture(null);
        }
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        if (lightEngine instanceof BulkLightEngine bulk) {
            return bulk.hbm$updateLight(chunk, positions, sectionMaskFromBottom);
        }
        return updateLightIndividually(level, chunk, positions, sectionMaskFromBottom);
    }

    /** Индивидуальный фолбэк (и путь для подклассов движка вроде Starlight). */
    public static CompletableFuture<?> updateLightIndividually(
            ServerLevel level, LevelChunk chunk, LongList positions, long sectionMaskFromBottom) {
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        if (serverShuttingDown(level)) {
            return CompletableFuture.completedFuture(null);
        }
        int minSectionY = LevelHooks.minSectionY(level);
        LevelChunkSection[] sections = chunk.getSections();
        // ВАЖНО: ванильный LevelChunk.setBlockState обновляет per-колоночные источники
        // небесного света (skyLightSources.update, см. ваниль :239). Наш bulk-путь его
        // обходит — без этого небо не проваливается вниз в вырезанный кратер и чанки
        // под открытым небом остаются чёрными.
        var skyLightSources = chunk.getSkyLightSources();
        for (int i = 0; i < positions.size(); i++) {
            long packed = positions.getLong(i);
            skyLightSources.update(chunk, BlockPos.getX(packed) & 15, BlockPos.getY(packed), BlockPos.getZ(packed) & 15);
        }
        for (long mask = sectionMaskFromBottom; mask != 0; mask &= mask - 1) {
            int subY = Long.numberOfTrailingZeros(mask);
            lightEngine.updateSectionStatus(SectionPos.of(cx, subY + minSectionY, cz), sections[subY].hasOnlyAir());
        }
        for (int i = 0; i < positions.size(); i++) {
            lightEngine.checkBlock(BlockPos.of(positions.getLong(i)));
        }
        //? if < 1.21.1 {
        lightEngine.tryScheduleUpdate();
        return CompletableFuture.completedFuture(null);
        //?} else {
        /*return lightEngine.waitForPendingTasks(cx, cz);
         *///?}
    }

    /** Немедленная перепечатка чанка всем отслеживающим игрокам. Вызывать после готовности света. */
    public static void sendWholesaleResend(ServerLevel level, long chunkPos) {
        int cx = ChunkPos.getX(chunkPos);
        int cz = ChunkPos.getZ(chunkPos);
        LevelChunk fresh = level.getChunkSource().getChunkNow(cx, cz);
        if (fresh == null) return;
        fresh.setUnsaved(true);
        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(fresh.getPos(), false);
        if (players.isEmpty()) return;
        LevelLightEngine engine = level.getChunkSource().getLightEngine();
        ClientboundLevelChunkWithLightPacket chunkPacket =
                new ClientboundLevelChunkWithLightPacket(fresh, engine, null, null);
        for (ServerPlayer p : players) {
            p.connection.send(chunkPacket);
        }
    }

    // ── Хелперы версий ──────────────────────────────────────────────────────

    /** Either/ChunkResult за гейтом: полный чанк или null. */
    private static LevelChunk joinFullChunk(ChunkHolder holder) {
        return fullChunkFuture(holder).getNow(null);
    }

    private static CompletableFuture<LevelChunk> fullChunkFuture(ChunkHolder holder) {
        //? if < 1.21.1 {
        return holder.getFullChunkFuture().thenApply(either -> either.left().orElse(null));
        //?} else {
        /*return holder.getFullChunkFuture().thenApply(result -> result.orElse(null));
         *///?}
    }
}
