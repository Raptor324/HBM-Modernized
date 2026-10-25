// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.RadiationSystemNT), Commit 3f9a261a.
// Quelle: github.com/Warfactory-Official/ntm-next
//
// Nur der Java-Pfad. Entfallen: RadsimBackend/native Bibliothek, RadVis-Debugansicht,
// asynchrones Mitlesen der Daten im IOWorker (MixinIOWorker/hbm$getRadiation) -- der Port liest
// die eigene Chunk-Datei synchron beim Chunk-Load. Paletten-Scans siehe SectionScanner.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.particle.ModParticleTypes;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Orchestrierung des NTM-Next-Strahlungsfeldes: Tick-Ablauf, asynchroner Solver, Chunk-
 * Ereignisse, Weltwirkungen (Nebel/Zerstoerung) und die statische Abfrage-/Schreib-API.
 *
 * <p>Ablauf je Sim-Schritt (Server-Tick-Ende): auf vorherigen Schritt warten -> Weltwirkungen ->
 * schmutzige Sektionen neu aufbauen -> naechsten Schritt asynchron im {@link #RAD_POOL} starten.
 * Abfragen ({@link #getRadForCoord}) warten nur, falls der Schritt noch laeuft.
 */
public final class NtmRadiationSystem {

    static final int MAX_POCKETS = 2048;
    static final int DIRECT_OVERLAP_MAX_OLD_POCKETS = 16, DIRECT_OVERLAP_MAX_CELLS = 2048;
    static final short NO_POCKET = -1;
    static final int KIND_NONE = 0, KIND_UNI = 1, KIND_SINGLE = 2, KIND_MULTI = 3;
    static final int MAX_SECTIONS_PER_CHUNK = DimensionType.Y_SIZE / SectionPos.SECTION_SIZE;
    static final long CHUNK_DIRTY_MASK = 1L << 63;

    static final int[] FACE_DX = {0, 0, 0, 0, -1, 1},
            FACE_DY = {-1, 1, 0, 0, 0, 0},
            FACE_DZ = {0, 0, -1, 1, 0, 0};
    /** Je Flaeche (unten, oben, N, S, W, O) die 256 lokalen Blockindizes der Randebene. */
    static final int[] FACE_PLANE = new int[6 * 256];

    /** Je Dimension ein Feld pro Strahlungsart (Index = RadiationType.ordinal()). */
    static final Map<ServerLevel, WorldRadiationData[]> worldMap = new HashMap<>(4);
    /** Nahfeld (Punktquellen) je Dimension. */
    static final Map<ServerLevel, NearFieldTracker> nearFields = new HashMap<>(4);
    /** Kontaminationsebene je Dimension (Stufe 3). */
    static final Map<ServerLevel, ContaminationLayer> contaminations = new HashMap<>(4);
    private static final Map<ServerLevel, Double> disabledAmbient = new HashMap<>(4);
    static final int[] BOUNDARY_MASKS = {0, 0, 0xF00, 0xF00, 0xFF0, 0xFF0},
            LINEAR_OFFSETS = {-256, 256, -16, 16, -1, 1};
    static final int PROFILE_WINDOW = 200;
    static final int FOG_DRAIN_LIMIT = 8;
    static final byte MAGIC_0 = (byte) 'N', MAGIC_1 = (byte) 'T', MAGIC_2 = (byte) 'X', FMT = 8;
    static final int FMT8_COMPACT_BYTES = 5 + 32 * (4 + 1 + 2 + 2 + 512 + MAX_POCKETS * 10);
    static final int FMT8_MAX_BYTES =
            5 + MAX_SECTIONS_PER_CHUNK * (4 + 1 + 2 + 2 + 512 + MAX_POCKETS * 10);

    /** Eigener Fork/Join-Pool des Solvers (Original: commonPool). */
    static final ForkJoinPool RAD_POOL = createPool();
    static final int TARGET_TASK_CNT = RAD_POOL.getParallelism() << 2;
    static final RegionChunkRadiationStorage SIDE_CAR = new RegionChunkRadiationStorage();

    static final ThreadLocal<int[]> TL_FF_QUEUE =
            ThreadLocal.withInitial(() -> new int[SectionScanner.SECTION_BLOCK_COUNT]);
    static final ThreadLocal<int[]> TL_VOL_COUNTS =
            ThreadLocal.withInitial(() -> new int[MAX_POCKETS]);
    static final ThreadLocal<double[]> TL_NEW_MASS =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final ThreadLocal<long[]> TL_SUM_XYZ =
            ThreadLocal.withInitial(() -> new long[MAX_POCKETS * 3]);
    static final ThreadLocal<double[]> TL_DENSITIES =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final ThreadLocal<double[]> TL_ADD =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<double[]> TL_SET =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<long[]> TL_BEST_SET_SEQ =
            ThreadLocal.withInitial(() -> new long[MAX_POCKETS + 1]);
    static final ThreadLocal<double[]> TL_SRC_WEIGHT =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<double[]> TL_SRC_NUMERATOR =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS + 1]);
    static final ThreadLocal<int[]> TL_TEMP_ARRAY =
            ThreadLocal.withInitial(() -> new int[MAX_POCKETS]);
    static final ThreadLocal<int[]> TL_TOUCHED = ThreadLocal.withInitial(() -> new int[512]);
    static final ThreadLocal<Long2IntOpenHashMap> TL_EDGE_COUNTS =
            ThreadLocal.withInitial(
                    () -> {
                        Long2IntOpenHashMap map = new Long2IntOpenHashMap(512);
                        map.defaultReturnValue(0);
                        return map;
                    });
    static final ThreadLocal<ByteBuffer> TL_ENCODE_BUF =
            ThreadLocal.withInitial(() -> ByteBuffer.allocate(FMT8_COMPACT_BYTES - 4));
    static final ThreadLocal<double[]> TL_TEMP_DENSITIES =
            ThreadLocal.withInitial(() -> new double[MAX_POCKETS]);
    static final double RAD_EPSILON = 1.0e-5D;
    static final double RAD_MAX = Double.MAX_VALUE / 2.0D;
    static final long DESTROY_PROB_U64 = Long.divideUnsigned(-1L, 100L);
    static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);
    static long ticks;
    static CompletableFuture<Void> radiationFuture = COMPLETED;
    static boolean serverStopping;

    static int tickDelay = 1;
    static double dT = tickDelay / (double) SharedConstants.TICKS_PER_SECOND;

    static {
        int[] rowShifts = {4, 4, 8, 8, 8, 8},
                colShifts = {0, 0, 0, 0, 4, 4},
                bases = {0, 15 << 8, 0, 15 << 4, 0, 15};
        for (int face = 0; face < 6; face++) {
            int base = face << 8;
            int rowShift = rowShifts[face];
            int colShift = colShifts[face];
            int fixedBits = bases[face];
            int t = 0;
            for (int r = 0; r < 16; r++) {
                int rBase = r << rowShift;
                for (int c = 0; c < 16; c++) {
                    FACE_PLANE[base + (t++)] = rBase | (c << colShift) | fixedBits;
                }
            }
        }
    }

    private NtmRadiationSystem() {}

    private static ForkJoinPool createPool() {
        int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ClassLoader loader = NtmRadiationSystem.class.getClassLoader();
        AtomicInteger counter = new AtomicInteger();
        return new ForkJoinPool(
                parallelism,
                pool -> {
                    ForkJoinWorkerThread thread =
                            ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    thread.setName("HBM-Radiation-Solver-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    thread.setContextClassLoader(loader);
                    return thread;
                },
                (thread, ex) ->
                        MainRegistry.LOGGER.error("[NtmRadiation] Uncaught error in solver", ex),
                false);
    }

    /** Fuehrt einen Task im RAD_POOL aus (vom Server-Thread aus blockierend). */
    static void invokeInPool(ForkJoinTask<?> task) {
        if (ForkJoinTask.getPool() == RAD_POOL) task.invoke();
        else RAD_POOL.invoke(task);
    }

    static int getTaskThreshold(int size, int minGrain) {
        int th = size / TARGET_TASK_CNT;
        return Math.max(minGrain, th);
    }

    /** Nach dem Laden der Daten (Serverstart): Tickrate und Parameter pruefen. */
    static void onLoadComplete() {
        tickDelay = NtmRadiationConfig.radTickRate;
        if (tickDelay <= 0) throw new IllegalStateException("Radiation tick rate must be positive");
        dT = tickDelay / (double) SharedConstants.TICKS_PER_SECOND;
        double diffusivity = NtmRadiationConfig.diffusivity;
        if (diffusivity <= 0.0D || !Double.isFinite(diffusivity))
            throw new IllegalStateException("Radiation diffusivity must be positive and finite");
        double hl = NtmRadiationConfig.halfLifeSeconds;
        if (hl <= 0.0D || !Double.isFinite(hl))
            throw new IllegalStateException("Radiation HalfLife must be positive and finite");
    }

    /** Wahrscheinlichkeit p in [0,1] als vorzeichenlose 64-Bit-Schwelle. */
    static long probU64(double p) {
        if (!(p > 0.0D) || !Double.isFinite(p)) return 0L;
        if (p >= 1.0D) return -1L;
        double v = p * 4294967296.0D;
        long hi = (long) v;
        if (hi >= 0x1_0000_0000L) return -1L;
        double frac = v - (double) hi;
        long lo = (long) (frac * 4294967296.0D);
        if (lo >= 0x1_0000_0000L) lo = 0xFFFF_FFFFL;
        return (hi << 32) | (lo & 0xFFFF_FFFFL);
    }

    // ------------------------------------------------------------------
    // Tick-Ablauf
    // ------------------------------------------------------------------

    static void tickSim(MinecraftServer server) {
        if (serverStopping) return;
        if (!NtmRadiationConfig.chunkRadsEnabled()) return;
        ticks++;
        if ((ticks + 17) % tickDelay != 0) return;
        awaitSimulation();
        // Stufe 3: Kontamination zerfaellt und speist das Feld (Server-Thread, Sim ruht gerade).
        if (NtmRadiationConfig.contaminationEnabled
                && ticks % Math.max(1, NtmRadiationConfig.contamTickInterval) == 0) {
            for (ContaminationLayer layer : contaminations.values()) {
                try {
                    layer.tick();
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("[NtmRadiation] Error in contamination step", t);
                }
            }
        }
        runWorldEffects();

        runRebuilds();
        radiationFuture = CompletableFuture.runAsync(NtmRadiationSystem::runSweeps, RAD_POOL);
    }

    static void awaitSimulation() {
        if (radiationFuture == COMPLETED) return;
        try {
            radiationFuture.join();
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] Radiation async step failed", ex);
        } finally {
            radiationFuture = COMPLETED;
        }
    }

    // Server-Thread
    static void runRebuilds() {
        for (WorldRadiationData data : allData()) {
            try {
                data.rebuildLoadedSections();
            } catch (Throwable t) {
                MainRegistry.LOGGER.error(
                        "[NtmRadiation] Error in radiation rebuild in dimension {}",
                        data.world.dimension().location(),
                        t);
            }
        }
    }

    // Server-Thread
    static void runWorldEffects() {
        boolean destroy = NtmRadiationConfig.worldEffectsEnabled();
        for (WorldRadiationData data : allData()) {
            try {
                if (destroy) handleWorldDestruction(data);
                runFog(data);
            } catch (Throwable t) {
                MainRegistry.LOGGER.error(
                        "[NtmRadiation] Error in radiation world effects in dimension {}",
                        data.world.dimension().location(),
                        t);
            }
        }
    }

    // Server-Thread
    static void runFog(WorldRadiationData data) {
        for (int i = 0; i < FOG_DRAIN_LIMIT; i++) {
            long pk = data.fogQueue.poll();
            if (pk == Pools.LongQueue.EMPTY) break;
            spawnFog(data, pk);
        }
        if (data.workEpoch % 200 == 13) data.fogQueue.clear(true);
    }

    // Server-Thread
    static void spawnFog(WorldRadiationData data, long pk) {
        if (!ModClothConfig.get().enableRadFogEffect) return;
        long ck = SectionKeys.sectionToChunkLong(pk);
        int ownerId = data.getId(ck);
        if (ownerId < 0 || ownerId >= data.nextId || data.cks[ownerId] != ck) return;
        LevelChunk chunk = data.mcChunks[ownerId];
        if (chunk == null) return;

        int yz = (int) (pk & 0xFFFFF);
        int slot = yz >>> 11;
        int targetPocketIndex = yz & 0x7FF;
        if (slot >= data.sectionsPerChunk) return;

        int kind = data.getKind(ownerId, slot);
        if (kind == KIND_NONE) return;
        LevelChunkSection[] sections = chunk.getSections();
        LevelChunkSection section = (slot < sections.length) ? sections[slot] : null;

        ServerLevel world = data.world;
        RandomSource rand = world.getRandom();
        int baseX = chunk.getPos().x << 4;
        int baseZ = chunk.getPos().z << 4;
        int baseY = (data.minSectionY + slot) << 4;
        int secIdx = (ownerId * data.sectionsPerChunk) + slot;
        WorldRadiationData.SectionRef sc = (kind == KIND_UNI) ? null : data.complexSecs[secIdx];
        if (kind != KIND_UNI && sc == null) return;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        for (int k = 0; k < 10; k++) {
            int lx = rand.nextInt(16), ly = rand.nextInt(16), lz = rand.nextInt(16);
            int x = baseX + lx, y = baseY + ly, z = baseZ + lz;
            if (sc != null && sc.getPocketIndex(BlockPos.asLong(x, y, z)) != targetPocketIndex)
                continue;
            BlockState state =
                    (section == null || section.hasOnlyAir())
                            ? Blocks.AIR.defaultBlockState()
                            : section.getBlockState(lx, ly, lz);
            if (!state.isAir()) continue;

            boolean nearGround = false;
            for (int d = 1; d <= 6; d++) {
                int yy = y - d;
                if (yy < world.getMinBuildHeight()) break;
                if (!world.getBlockState(probe.set(x, yy, z)).isAir()) {
                    nearGround = true;
                    break;
                }
            }
            if (!nearGround) continue;

            // Original: EffectNT RadFog an alle Spieler im Umkreis von 100 Bloecken.
            double fx = x + 0.5D, fy = y + 0.5D, fz = z + 0.5D;
            for (ServerPlayer player : world.players()) {
                if (player.distanceToSqr(fx, fy, fz) > 100.0D * 100.0D) continue;
                world.sendParticles(
                        player,
                        ModParticleTypes.RAD_FOG_PARTICLE.get(),
                        true,
                        fx,
                        fy,
                        fz,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
            return;
        }
    }

    // Server-Thread
    static void handleWorldDestruction(WorldRadiationData data) {
        if (tickDelay == 1) {
            long pk = data.pocketToDestroy;
            data.pocketToDestroy = Long.MIN_VALUE;
            if (pk != Long.MIN_VALUE) destroyPocket(data, pk);
            return;
        }

        for (int i = 0; i < tickDelay; i++) {
            long pk = data.destructionQueue.poll();
            if (pk == Pools.LongQueue.EMPTY) break;
            destroyPocket(data, pk);
        }
        if (data.workEpoch % 200 == 13) data.destructionQueue.clear(true);
    }

    // Server-Thread
    static void destroyPocket(WorldRadiationData data, long pk) {
        long ck = SectionKeys.sectionToChunkLong(pk);
        int ownerId = data.getId(ck);
        if (ownerId < 0 || ownerId >= data.nextId || data.cks[ownerId] != ck) return;
        LevelChunk chunk = data.mcChunks[ownerId];
        if (chunk == null) return;

        int yz = (int) (pk & 0xFFFFF);
        int slot = yz >>> 11;
        int targetPocketIndex = yz & 0x7FF;
        if (slot >= data.sectionsPerChunk) return;

        int kind = data.getKind(ownerId, slot);
        if (kind == KIND_NONE) return;

        LevelChunkSection[] sections = chunk.getSections();
        if (slot >= sections.length) return;
        LevelChunkSection section = sections[slot];
        if (section == null || section.hasOnlyAir()) return;

        ServerLevel world = data.world;
        RandomSource rand = world.getRandom();
        int baseX = chunk.getPos().x << 4;
        int baseZ = chunk.getPos().z << 4;
        int baseY = (data.minSectionY + slot) << 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (kind == KIND_UNI) {
            if (targetPocketIndex != 0) return;
            for (int i = 0; i < SectionScanner.SECTION_BLOCK_COUNT; i++) {
                if (rand.nextInt(3) != 0) continue;
                decayLocal(world, chunk, section, baseX, baseY, baseZ, i, pos);
            }
            return;
        }

        int secIdx = (ownerId * data.sectionsPerChunk) + slot;
        WorldRadiationData.SectionRef sc = data.complexSecs[secIdx];
        if (sc == null) return;
        for (int i = 0; i < SectionScanner.SECTION_BLOCK_COUNT; i++) {
            if (rand.nextInt(3) != 0) continue;
            int actual = sc.paletteIndexOrNeg(i);
            if (actual < 0 || actual != targetPocketIndex) continue;
            decayLocal(world, chunk, section, baseX, baseY, baseZ, i, pos);
        }
    }

    // Server-Thread
    static void decayLocal(
            ServerLevel world,
            LevelChunk chunk,
            LevelChunkSection section,
            int baseX,
            int baseY,
            int baseZ,
            int i,
            BlockPos.MutableBlockPos pos) {
        int lx = SectionKeys.getLocalX(i), ly = SectionKeys.getLocalY(i), lz = SectionKeys.getLocalZ(i);
        BlockState state = section.getBlockState(lx, ly, lz);
        if (state.isAir()) return;
        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
        int myY = baseY + ly;
        if (myY < topY - 1 || myY > topY) return;
        pos.set(baseX + lx, myY, baseZ + lz);
        RadiationWorldHandler.decayBlock(world, pos, state);
    }

    // ------------------------------------------------------------------
    // Persistenz
    // ------------------------------------------------------------------

    // Server-Thread
    static void writeSidecar(
            WorldRadiationData data, ChunkPos pos, @Nullable byte[] payload, boolean logFailure) {
        try {
            SIDE_CAR.write(data.world, data.storageRoot, pos, payload);
        } catch (RuntimeException ex) {
            if (logFailure) {
                MainRegistry.LOGGER.error(
                        "[NtmRadiation] Radiation data write failed for chunk {} in dimension {} ({})",
                        pos,
                        data.world.dimension().location(),
                        data.type.id,
                        ex);
            }
        }
    }

    /** Welt-Speichern (Autosave / /save-all): geaenderte Chunks einer Dimension schreiben. */
    static void onLevelSave(ServerLevel level) {
        if (!NtmRadiationConfig.chunkRadsEnabled()) return;
        WorldRadiationData[] all = worldMap.get(level);
        if (all == null) return;
        awaitSimulation();
        for (WorldRadiationData data : all) {
            try {
                data.saveRadiationDirty(false);
            } catch (RuntimeException ex) {
                MainRegistry.LOGGER.error(
                        "[NtmRadiation] Radiation data save failed for dimension {} ({})",
                        level.dimension().location(),
                        data.type.id,
                        ex);
            }
        }
        ContaminationLayer layer = contaminations.get(level);
        if (layer != null) layer.saveDirty();
        try {
            SIDE_CAR.flush();
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] Radiation storage flush failed", ex);
        }
    }

    static void saveDirtySidecar(boolean flush) {
        RuntimeException failure = null;
        for (WorldRadiationData data : allData()) {
            try {
                data.saveRadiationDirty(flush);
            } catch (RuntimeException ex) {
                if (!flush) {
                    MainRegistry.LOGGER.error(
                            "[NtmRadiation] Radiation data save failed for dimension {}",
                            data.world.dimension().location(),
                            ex);
                } else if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        if (flush) {
            try {
                SIDE_CAR.flush();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    static void onServerStopping(MinecraftServer server) {
        serverStopping = true;
        RuntimeException simulationFailure = null;
        try {
            radiationFuture.join();
        } catch (RuntimeException ex) {
            simulationFailure = ex;
        } finally {
            radiationFuture = COMPLETED;
        }
        for (ContaminationLayer layer : contaminations.values()) {
            try {
                layer.saveDirty();
            } catch (RuntimeException ex) {
                MainRegistry.LOGGER.error("[NtmRadiation] Final contamination save failed", ex);
            }
        }
        try {
            saveDirtySidecar(true);
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] Final radiation save failed", ex);
        } finally {
            for (WorldRadiationData data : allData()) data.logLifetimeProfiling();
            try {
                SIDE_CAR.close();
            } catch (RuntimeException ex) {
                MainRegistry.LOGGER.error("[NtmRadiation] Radiation storage close failed", ex);
            }
            worldMap.clear();
            nearFields.clear();
            contaminations.clear();
            SectionAxisCache.clear(null);
            disabledAmbient.clear();
        }
        if (simulationFailure != null) {
            MainRegistry.LOGGER.error(
                    "[NtmRadiation] Last radiation step failed", simulationFailure);
        }
    }

    @Nullable
    static byte[] readSidecar(WorldRadiationData data, ChunkPos pos) {
        try {
            return SIDE_CAR.read(data.world, data.storageRoot, pos);
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error(
                    "[NtmRadiation] Radiation data read failed for chunk {} in dimension {} ({}); ignoring it",
                    pos,
                    data.world.dimension().location(),
                    data.type.id,
                    ex);
            discardSidecar(data, pos);
            return null;
        }
    }

    static void discardSidecar(WorldRadiationData data, ChunkPos pos) {
        try {
            SIDE_CAR.discard(data.world, data.storageRoot, pos);
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error(
                    "[NtmRadiation] Failed to discard invalid radiation data for chunk {} in dimension {}",
                    pos,
                    data.world.dimension().location(),
                    ex);
        }
    }

    /** Alle Strahlungsdaten einer Dimension verwerfen (/ntmrad clear). */
    // Server-Thread
    public static boolean jettisonData(ServerLevel world) {
        WorldRadiationData[] all = worldMap.get(world);
        awaitSimulation();
        // Unterordner (Neutron, Beta) zuerst schliessen und loeschen, zuletzt den Gamma-Stamm.
        java.nio.file.Path root = RegionChunkRadiationStorage.dimensionRoot(world);
        ContaminationLayer contamLayer = contaminations.get(world);
        if (contamLayer != null) contamLayer.clear();
        SectionAxisCache.clear(world);
        try {
            SIDE_CAR.deleteDimension(root.resolve("contamination"));
            for (int t = RadiationType.FIELD_COUNT - 1; t >= 0; t--) {
                RadiationType type = RadiationType.FIELD_TYPES[t];
                SIDE_CAR.deleteDimension(type == RadiationType.GAMMA ? root : root.resolve(type.id));
            }
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error(
                    "[NtmRadiation] Radiation data delete failed for dimension {}",
                    world.dimension().location(),
                    ex);
            return false;
        }
        NearFieldTracker near = nearFields.get(world);
        if (near != null) near.clear();
        if (all == null) return true;
        for (WorldRadiationData data : all) {
            data.pocketToDestroy = Long.MIN_VALUE;
            data.destructionQueue.clear(true);
            data.fogQueue.clear(true);
            data.clearQueuedRadiationDirty();
            data.clearAllChunkRefs();
            data.dirtyCk.clearAll();
            data.clearQueuedWrites();
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Schreib-API (Quellen)
    // ------------------------------------------------------------------

    /**
     * Additiver Eintrag (RAD) an einer Blockposition, wird zum naechsten Schritt angewandt. Der
     * Strahlungsarten-Mix kommt aus dem Block an der Position (SourceMixTable).
     */
    // Server-Thread
    public static void incrementRad(ServerLevel world, BlockPos pos, double amount) {
        incrementRad(world, pos, amount, (float[]) null);
    }

    /** Additiver Eintrag mit explizitem Mix (gamma, neutron, beta, alpha); null = aus Block. */
    // Server-Thread
    public static void incrementRad(
            ServerLevel world, BlockPos pos, double amount, @Nullable float[] mix) {
        if (!NtmRadiationConfig.chunkRadsEnabled()
                || Math.abs(amount) < RAD_EPSILON
                || isOutsideWorld(world, pos)) return;
        addOrEmit(world, pos, amount, 0.0D, false, mix);
    }

    /** Saettigende Emission: zieht die Dichte mit Rate {@code emission} gegen {@code saturation}. */
    // Server-Thread
    public static void incrementRad(
            ServerLevel world, BlockPos pos, double emission, double saturation) {
        emitRad(world, pos, emission, saturation, null);
    }

    // Server-Thread
    public static void emitRad(
            ServerLevel world,
            BlockPos pos,
            double emission,
            double saturation,
            @Nullable float[] mix) {
        if (!NtmRadiationConfig.chunkRadsEnabled()
                || Math.abs(emission) < RAD_EPSILON
                || isOutsideWorld(world, pos)) return;
        addOrEmit(world, pos, emission, saturation, true, mix);
    }

    private static void addOrEmit(
            ServerLevel world,
            BlockPos pos,
            double emission,
            double saturation,
            boolean isSource,
            @Nullable float[] mix) {
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = SectionKeys.sectionToChunkLong(sck);
        LevelChunk chunk =
                world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        BlockState here = chunk.getBlockState(pos);
        if (RadiationShielding.RAD_RESISTANT_STATES.contains(here)) return;
        if (mix == null) mix = SourceMixTable.mixFor(here);
        awaitSimulation();
        WorldRadiationData[] all = getAllRadData(world);
        int local = SectionKeys.blockPosToLocal(posLong);
        float[] nearAmounts = null;
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            float share = mix[t];
            if (!(share > 0.0F)) continue;
            WorldRadiationData data = all[t];
            int ownerId = data.onChunkLoaded(chunk.getPos().x, chunk.getPos().z, chunk);
            int slot = data.slotOf(SectionPos.y(sck));
            if (slot < 0) return;
            if (isSource) data.queueEmit(sck, local, emission * share, saturation * share);
            else data.queueAdd(sck, local, emission * share);
            if (data.getKind(ownerId, slot) == KIND_NONE) data.dirtyCk.add(ck, ownerId, slot);
            data.setChunkDirty(ownerId);
            if (emission > 0.0D) {
                if (nearAmounts == null) nearAmounts = new float[RadiationType.FIELD_COUNT];
                nearAmounts[t] = (float) (emission * share);
            }
        }
        if (nearAmounts != null) nearField(world).record(pos, nearAmounts);
    }

    /** Dichte der Pocket an {@code pos} hart setzen (je Art Anteil laut Mix des Blocks). */
    // Server-Thread
    public static void setRadForCoord(ServerLevel world, BlockPos pos, double amount) {
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(world, pos)) return;
        LevelChunk chunk = chunkAt(world, pos);
        if (chunk == null) return;
        float[] mix = SourceMixTable.mixFor(chunk.getBlockState(pos));
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            setRadForCoord(world, pos, RadiationType.FIELD_TYPES[t], amount * mix[t]);
        }
    }

    /** Dichte einer Art an {@code pos} hart setzen (letzter Aufruf gewinnt). */
    // Server-Thread
    public static void setRadForCoord(
            ServerLevel world, BlockPos pos, RadiationType type, double amount) {
        if (!type.hasField) return;
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(world, pos)) return;
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = SectionKeys.sectionToChunkLong(sck);
        LevelChunk chunk =
                world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        if (isResistantAt(chunk, pos)) return;
        awaitSimulation();
        WorldRadiationData data = getWorldRadData(world, type);
        int ownerId = data.onChunkLoaded(chunk.getPos().x, chunk.getPos().z, chunk);
        int slot = data.slotOf(SectionPos.y(sck));
        if (slot < 0) return;

        int local = SectionKeys.blockPosToLocal(posLong);
        data.queueSet(sck, local, amount);
        if (data.getKind(ownerId, slot) == KIND_NONE) data.dirtyCk.add(ck, ownerId, slot);
        data.setChunkDirty(ownerId);
    }

    @Nullable
    private static LevelChunk chunkAt(ServerLevel world, BlockPos pos) {
        return world.getChunkSource()
                .getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    // ------------------------------------------------------------------
    // Abfrage-API
    // ------------------------------------------------------------------

    /** Hintergrundstrahlung der Dimension (radiation_settings: ambient_rad, zaehlt als Gamma). */
    public static double ambientRad(ServerLevel world) {
        if (!NtmRadiationConfig.chunkRadsEnabled()) {
            return disabledAmbient.computeIfAbsent(
                    world, level -> RadiationSettings.forLevel(level).ambientRadOrDefault());
        }
        return getWorldRadData(world, RadiationType.GAMMA).ambientRad;
    }

    /** Dosisleistung: gewichtete Feldsumme + Nahfeld, mindestens Hintergrund. */
    public static double doseAt(ServerLevel world, BlockPos pos) {
        double field = totalAt(world, pos, true);
        double ambient = ambientRad(world);
        return field >= 0.0D ? Math.max(field, ambient) : Math.max(0.0D, field + ambient);
    }

    /**
     * Gewichtete Summe aller Arten (RAD/s), optional mit Nahfeld. Das ist der Wert, den die
     * Raptor-Schnittstelle (Spieler, Geigerzaehler) sieht.
     */
    // Server-Thread
    public static double totalAt(ServerLevel world, BlockPos pos, boolean withNearField) {
        double sum = 0.0D;
        float[] near = withNearField ? nearFieldAt(world, pos) : null;
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            RadiationType type = RadiationType.FIELD_TYPES[t];
            double v = getRadForCoord(world, pos, type);
            if (near != null) v += near[t];
            sum += NtmRadiationConfig.typeWeight[t] * v;
        }
        return sum;
    }

    /** Ungewichtete Feldsumme aller Arten (wie Stufe 1). */
    // Server-Thread
    public static double getRadForCoord(ServerLevel world, BlockPos pos) {
        double sum = 0.0D;
        for (RadiationType type : RadiationType.FIELD_TYPES) sum += getRadForCoord(world, pos, type);
        return sum;
    }

    /** Nahfeld (RAD/s je Feldart) an der Position; nicht veraendern. */
    // Server-Thread
    public static float[] nearFieldAt(ServerLevel world, BlockPos pos) {
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(world, pos))
            return new float[RadiationType.FIELD_COUNT];
        return nearField(world).query(pos);
    }

    /** Felddichte (RAD) einer Art in der Pocket an {@code pos}; 0 in Abschirmbloecken. */
    // Server-Thread
    public static double getRadForCoord(ServerLevel world, BlockPos pos, RadiationType type) {
        if (!type.hasField) return 0D;
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(world, pos)) return 0D;
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = SectionKeys.sectionToChunkLong(sck);
        LevelChunk chunk =
                world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return 0D;
        awaitSimulation();
        WorldRadiationData[] all = worldMap.get(world);
        if (all == null) return 0D;
        WorldRadiationData data = all[type.ordinal()];
        if (isResistantAt(chunk, pos)) return 0D;
        int slot = data.slotOf(SectionPos.y(sck));
        if (slot < 0) return 0D;
        int ownerId = data.getId(ck);
        if (ownerId < 0) return 0D;
        if (ownerId >= data.cks.length || data.cks[ownerId] != ck) return 0D;

        int secIdx = (ownerId * data.sectionsPerChunk) + slot;
        int kind = data.getKind(ownerId, slot);

        if (kind == KIND_NONE) {
            data.dirtyCk.add(ck, ownerId, slot);
            return 0D;
        }
        if (kind == KIND_UNI) return data.uniformRads[secIdx];

        WorldRadiationData.SectionRef sc = data.complexSecs[secIdx];
        if (sc == null || sc.pocketCount <= 0) {
            data.dirtyCk.add(ck, ownerId, slot);
            return 0D;
        }
        int pocketIndex = sc.getPocketIndex(posLong);
        if (pocketIndex < 0) {
            data.dirtyCk.add(ck, ownerId, slot);
            return 0D;
        }
        if (kind == KIND_SINGLE) {
            return data.uniformRads[secIdx];
        } else {
            return ((WorldRadiationData.MultiSectionRef) sc).data[pocketIndex << 1];
        }
    }

    // ------------------------------------------------------------------
    // Topologie-Aenderungen
    // ------------------------------------------------------------------

    // Server-Thread
    public static void markSectionForRebuild(Level world, BlockPos pos) {
        if (world == null || world.isClientSide() || !NtmRadiationConfig.chunkRadsEnabled())
            return;
        if (!(world instanceof ServerLevel server)) return;
        if (isOutsideWorld(server, pos)) return;

        markSectionForRebuild(world, SectionPos.asLong(pos));
    }

    // Server-Thread
    public static void markSectionForRebuild(Level world, long sck) {
        if (world == null || world.isClientSide() || !NtmRadiationConfig.chunkRadsEnabled())
            return;
        if (!(world instanceof ServerLevel ws)) return;
        long ck = SectionKeys.sectionToChunkLong(sck);
        LevelChunk chunk = ws.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        awaitSimulation();
        for (WorldRadiationData data : getAllRadData(ws)) {
            int slot = data.slotOf(SectionPos.y(sck));
            if (slot < 0) return;
            int id = data.onChunkLoaded(chunk.getPos().x, chunk.getPos().z, chunk);
            data.dirtyCk.add(ck, id, slot);
            data.setChunkDirty(id);
        }
    }

    // Server-Thread
    public static void markSectionsForRebuild(Level world, LongIterable sections) {
        if (world == null || world.isClientSide() || !NtmRadiationConfig.chunkRadsEnabled())
            return;
        if (!(world instanceof ServerLevel ws)) return;
        awaitSimulation();
        for (WorldRadiationData data : getAllRadData(ws)) {
            LongIterator it = sections.iterator();
            while (it.hasNext()) {
                long sck = it.nextLong();
                long ck = SectionKeys.sectionToChunkLong(sck);
                LevelChunk chunk =
                        ws.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
                if (chunk == null) continue;

                int slot = data.slotOf(SectionPos.y(sck));
                if (slot < 0) continue;
                int id = data.onChunkLoaded(chunk.getPos().x, chunk.getPos().z, chunk);
                data.dirtyCk.add(ck, id, slot);
                data.setChunkDirty(id);
            }
        }
    }

    /** Ganzen Chunk neu aufbauen (Port: fuer recalculateChunkRadiation nach Explosionen). */
    // Server-Thread
    static void markChunkForRebuild(ServerLevel world, LevelChunk chunk) {
        if (!NtmRadiationConfig.chunkRadsEnabled()) return;
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        if (((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) != 0) return;
        awaitSimulation();
        for (WorldRadiationData data : getAllRadData(world)) {
            int id = data.onChunkLoaded(cx, cz, chunk);
            data.dirtyCk.add(ChunkPos.asLong(cx, cz), id);
            data.setChunkDirty(id);
        }
    }

    // ------------------------------------------------------------------
    // Asynchroner Schritt
    // ------------------------------------------------------------------

    static void runSweeps() {
        java.util.List<WorldRadiationData> all = allData();
        int n = all.size();
        if (n == 0) return;

        if (n == 1) {
            runSweepSafe(all.get(0));
        } else {
            ForkJoinTask<?>[] tasks = new ForkJoinTask<?>[n];
            for (int i = 0; i < n; i++) {
                WorldRadiationData data = all.get(i);
                tasks[i] = ForkJoinTask.adapt(() -> runSweepSafe(data));
            }
            ForkJoinTask.invokeAll(tasks);
        }
    }

    private static void runSweepSafe(WorldRadiationData data) {
        try {
            data.runSweepPhases();
        } catch (Throwable t) {
            MainRegistry.LOGGER.error(
                    "[NtmRadiation] Error in async rad simulation in dimension {} ({})",
                    data.world.dimension().location(),
                    data.type.id,
                    t);
        }
    }

    // ------------------------------------------------------------------
    // Chunk-Ereignisse
    // ------------------------------------------------------------------

    static void onChunkLoad(ServerLevel server, LevelChunk chunk) {
        if (!NtmRadiationConfig.chunkRadsEnabled()) return;
        // Port: waehrend des Herunterfahrens keine neuen Weltdaten anlegen (Original setzte hier
        // serverStopping zurueck; der Port tut das beim naechsten Serverstart).
        if (serverStopping) return;
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        if (((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) != 0) return;
        awaitSimulation();
        if (NtmRadiationConfig.contaminationEnabled) contamination(server).onChunkLoad(chunk);
        for (WorldRadiationData data : getAllRadData(server)) {
            int id = data.onChunkLoaded(cx, cz, chunk);
            data.dirtyCk.add(ChunkPos.asLong(cx, cz), id);

            // Port: eigene Chunk-Datei synchron lesen (Original: vom IOWorker mitgelesen).
            byte[] payload = readSidecar(data, chunk.getPos());
            if (payload == null) continue;
            try {
                byte[] verified = verifyPayload(payload);
                if (verified != null) data.readPayload(cx, cz, verified);
            } catch (BufferUnderflowException | DecodeException ex) {
                MainRegistry.LOGGER.error(
                        "[NtmRadiation] Failed to decode {} data for chunk {} in dimension {}",
                        data.type.id,
                        chunk.getPos(),
                        server.dimension().location(),
                        ex);
                discardSidecar(data, chunk.getPos());
            }
        }
    }

    static void onChunkUnload(ServerLevel server, LevelChunk chunk) {
        if (!NtmRadiationConfig.chunkRadsEnabled()) return;
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        if (((cx ^ (cx << 10) >> 10) | (cz ^ (cz << 10) >> 10)) != 0) return;
        if (serverStopping) return;
        SectionAxisCache.invalidateChunk(server, cx, cz);
        ContaminationLayer layer = contaminations.get(server);
        if (layer != null) layer.onChunkUnload(chunk);
        WorldRadiationData[] all = worldMap.get(server);
        if (all == null) return;
        awaitSimulation();
        long ck = ChunkPos.asLong(cx, cz);
        for (WorldRadiationData data : all) {
            data.saveRadiationDirty(chunk);
            data.unloadChunk(cx, cz);
            data.removeChunkRef(ck);
        }
    }

    /** Dimension entladen (nur beim Herunterfahren): Daten sichern, Speicher freigeben. */
    static void onLevelUnload(ServerLevel level) {
        WorldRadiationData[] all = worldMap.get(level);
        if (all == null) return;
        awaitSimulation();
        for (WorldRadiationData data : all) {
            try {
                data.saveRadiationDirty(true);
            } catch (RuntimeException ex) {
                MainRegistry.LOGGER.error(
                        "[NtmRadiation] Radiation data save failed for dimension {}",
                        level.dimension().location(),
                        ex);
            }
        }
        ContaminationLayer layer = contaminations.remove(level);
        if (layer != null) layer.saveDirty();
        SectionAxisCache.clear(level);
        worldMap.remove(level);
        nearFields.remove(level);
        disabledAmbient.remove(level);
    }

    @Nullable
    static byte[] verifyPayload(byte[] raw) throws DecodeException {
        if (raw.length == 0) return null;
        if (raw.length < 5) throw new DecodeException("Payload too short: " + raw.length);
        if (raw.length > FMT8_MAX_BYTES)
            throw new DecodeException("Payload too large: " + raw.length);
        if (raw[0] != MAGIC_0 || raw[1] != MAGIC_1 || raw[2] != MAGIC_2)
            throw new DecodeException("Invalid magic");
        byte fmt = raw[3];
        if (fmt != FMT) throw new DecodeException("Unknown format: " + fmt);
        return raw;
    }

    /** Alle Felder (eine Instanz je Strahlungsart) einer Dimension. */
    static WorldRadiationData[] getAllRadData(ServerLevel world) {
        return worldMap.computeIfAbsent(
                world,
                w -> {
                    WorldRadiationData[] arr = new WorldRadiationData[RadiationType.FIELD_COUNT];
                    for (int t = 0; t < arr.length; t++)
                        arr[t] = new WorldRadiationData(w, RadiationType.FIELD_TYPES[t]);
                    return arr;
                });
    }

    static WorldRadiationData getWorldRadData(ServerLevel world, RadiationType type) {
        return getAllRadData(world)[type.ordinal()];
    }

    static NearFieldTracker nearField(ServerLevel world) {
        return nearFields.computeIfAbsent(world, NearFieldTracker::new);
    }

    static ContaminationLayer contamination(ServerLevel world) {
        return contaminations.computeIfAbsent(world, ContaminationLayer::new);
    }

    // ------------------------------------------------------------------
    // Stufe 3: Kontamination
    // ------------------------------------------------------------------

    /**
     * Feldeintrag je Art ohne Mix-Ermittlung und ohne Nahfeld (Emission der Kontamination: diffus,
     * keine Punktquelle).
     */
    // Server-Thread
    static void addFieldDirect(ServerLevel world, BlockPos pos, double[] perType) {
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(world, pos)) return;
        long posLong = pos.asLong();
        long sck = SectionPos.blockToSection(posLong);
        long ck = SectionKeys.sectionToChunkLong(sck);
        LevelChunk chunk =
                world.getChunkSource().getChunkNow(ChunkPos.getX(ck), ChunkPos.getZ(ck));
        if (chunk == null) return;
        if (isResistantAt(chunk, pos)) return;
        awaitSimulation();
        WorldRadiationData[] all = getAllRadData(world);
        int local = SectionKeys.blockPosToLocal(posLong);
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            double v = perType[t];
            if (v == 0.0D || !Double.isFinite(v)) continue;
            WorldRadiationData data = all[t];
            int ownerId = data.onChunkLoaded(chunk.getPos().x, chunk.getPos().z, chunk);
            int slot = data.slotOf(SectionPos.y(sck));
            if (slot < 0) return;
            data.queueAdd(sck, local, v);
            if (data.getKind(ownerId, slot) == KIND_NONE) data.dirtyCk.add(ck, ownerId, slot);
            data.setChunkDirty(ownerId);
        }
    }

    /**
     * Eingehende Port-Emission klassifizieren (nur Handler-Pfad, alte API):
     * <ul>
     *   <li>negativ -> Feld (Absorber).</li>
     *   <li>Emission in Luft/ohne Kollision/Fluessigkeit (Explosion, Fallout-Regen, Gas, Leck) ->
     *       Anteil {@code one_off_fraction} als Kontamination, Rest ins Feld.</li>
     *   <li>Block, der wiederholt emittiert, oder bekannter Quellblock (source_mix) -> Feld.</li>
     *   <li>sonstige einmalige Emission an einem Block -> wie Luft.</li>
     * </ul>
     */
    // Server-Thread
    public static void incrementRadClassified(ServerLevel world, BlockPos pos, double amount) {
        if (!(amount > 0.0D) || !NtmRadiationConfig.contaminationEnabled) {
            incrementRad(world, pos, amount);
            return;
        }
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(world, pos)) return;
        LevelChunk chunk = chunkAt(world, pos);
        if (chunk == null) return;
        BlockState state = chunk.getBlockState(pos);
        boolean airLike;
        try {
            airLike =
                    state.isAir()
                            || state.getCollisionShape(world, pos).isEmpty()
                            || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;
        } catch (RuntimeException ex) {
            airLike = false;
        }
        if (!airLike && (nearField(world).isPeriodic(pos) || SourceMixTable.hasEntry(state))) {
            incrementRad(world, pos, amount);
            return;
        }
        double frac = Math.max(0.0D, Math.min(1.0D, NtmRadiationConfig.oneOffContaminationFraction));
        double fieldPart = amount * (1.0D - frac);
        if (fieldPart > 0.0D) incrementRad(world, pos, fieldPart);
        double contam = amount * frac;
        if (!(contam > 0.0D)) return;
        float[] split = SourceMixTable.contaminationSplitFor(state);
        ContaminationLayer layer = contamination(world);
        // Stufe 4: Emission in Luft startet (anteilig) luftgetragen und wird vom Wind verfrachtet.
        double air = airLike ? NtmRadiationConfig.airborneFraction : 0.0D;
        for (ContaminationGroup g : ContaminationGroup.values()) {
            float share = split[g.ordinal()];
            if (!(share > 0.0F)) continue;
            double m = contam * share;
            if (air > 0.0D) layer.addAirborne(pos, g, m * air);
            if (air < 1.0D) layer.add(pos, g, m * (1.0D - air));
        }
    }

    /** Luftgetragene Kontamination eintragen (API, Stufe 4). */
    // Server-Thread
    public static void contaminateAirborne(
            ServerLevel world, BlockPos pos, ContaminationGroup group, double amount) {
        if (!NtmRadiationConfig.chunkRadsEnabled()
                || !NtmRadiationConfig.contaminationEnabled
                || isOutsideWorld(world, pos)) return;
        contamination(world).addAirborne(pos, group, amount);
    }

    public static double airborneAt(ServerLevel world, BlockPos pos, ContaminationGroup group) {
        ContaminationLayer layer = contaminations.get(world);
        return layer == null ? 0.0D : layer.airborneAt(pos, group);
    }

    /** Kontamination eintragen (API). */
    // Server-Thread
    public static void contaminate(
            ServerLevel world, BlockPos pos, ContaminationGroup group, double amount) {
        if (!NtmRadiationConfig.chunkRadsEnabled()
                || !NtmRadiationConfig.contaminationEnabled
                || isOutsideWorld(world, pos)) return;
        contamination(world).add(pos, group, amount);
    }

    public static double contaminationAt(ServerLevel world, BlockPos pos, ContaminationGroup group) {
        ContaminationLayer layer = contaminations.get(world);
        return layer == null ? 0.0D : layer.amountAt(pos, group);
    }

    /** Absorber/Dekon (decrementRad): absolute Menge anteilig abbauen. Rueckgabe: abgebaut. */
    // Server-Thread
    public static double decontaminateAmount(ServerLevel world, BlockPos pos, double amount) {
        ContaminationLayer layer = contaminations.get(world);
        return layer == null ? 0.0D : layer.removeAmount(pos, amount);
    }

    /** Anteil der Kontamination aller Sektionen im Radius entfernen. Rueckgabe: Sektionen. */
    // Server-Thread
    public static int decontaminate(ServerLevel world, BlockPos pos, int radius, double fraction) {
        ContaminationLayer layer = contaminations.get(world);
        return layer == null ? 0 : layer.decontaminate(pos, radius, fraction);
    }

    /**
     * Zeit in Sekunden, bis sich die Kontamination der Sektion halbiert hat (gemischte Gruppen,
     * Bisektion). Unendlich, wenn nichts da ist.
     */
    public static double contaminationHalvingSeconds(ServerLevel world, BlockPos pos) {
        ContaminationLayer layer = contaminations.get(world);
        ContaminationLayer.Entry e = layer == null ? null : layer.entryAt(pos);
        if (e == null || !(e.total() > 0.0D)) return Double.POSITIVE_INFINITY;
        double target = e.total() * 0.5D;
        double lo = 0.0D, hi = 1.0D;
        while (remaining(e, hi) > target && hi < 1.0e12D) hi *= 2.0D;
        for (int i = 0; i < 60; i++) {
            double mid = 0.5D * (lo + hi);
            if (remaining(e, mid) > target) lo = mid;
            else hi = mid;
        }
        return hi;
    }

    private static double remaining(ContaminationLayer.Entry e, double seconds) {
        double s = 0.0D;
        for (int g = 0; g < ContaminationGroup.COUNT; g++) {
            s += e.amount[g] * Math.pow(0.5D, seconds / NtmRadiationConfig.contamHalfLife[g]);
        }
        return s;
    }


    /** Flache Liste aller Feldinstanzen (alle Dimensionen, alle Arten). */
    static java.util.List<WorldRadiationData> allData() {
        java.util.List<WorldRadiationData> out = new java.util.ArrayList<>(worldMap.size() * 3);
        for (WorldRadiationData[] arr : worldMap.values()) java.util.Collections.addAll(out, arr);
        return out;
    }

    static boolean isResistantAt(LevelChunk chunk, BlockPos pos) {
        return RadiationShielding.RAD_RESISTANT_STATES.contains(chunk.getBlockState(pos));
    }

    /**
     * Blockwechsel im Chunk (Mixin auf LevelChunk#setBlockState, vor der Aenderung). Aendert sich
     * Abschirmung oder Quelle, wird die Sektion neu aufgebaut; aendert sich nur die
     * Materialdaempfung, wird nur diese neu gemittelt.
     */
    // Server-Thread
    public static void onBlockStateReplaced(LevelChunk chunk, BlockPos pos, BlockState next) {
        Level level = chunk.getLevel();
        if (level.isClientSide()) return;
        if (RadiationShielding.RAD_RESISTANT_STATES.isEmpty()
                && RadiationSources.RAD_SOURCE_STATES.isEmpty()
                && RadiationDiffusivity.isTrivial()) return;
        BlockState prev = chunk.getBlockState(pos);
        if (prev == next) return;
        SectionAxisCache.invalidate((ServerLevel) level, SectionPos.asLong(pos));
        if (RadiationShielding.RAD_RESISTANT_STATES.contains(prev)
                        != RadiationShielding.RAD_RESISTANT_STATES.contains(next)
                || !Objects.equals(
                        RadiationSources.RAD_SOURCE_STATES.get(prev),
                        RadiationSources.RAD_SOURCE_STATES.get(next))) {
            markSectionForRebuild(level, pos);
        } else {
            for (RadiationType type : RadiationType.FIELD_TYPES) {
                if (RadiationDiffusivity.of(type, prev) != RadiationDiffusivity.of(type, next)) {
                    markDiffusivityDirty((ServerLevel) level, pos);
                    break;
                }
            }
        }
    }

    // Server-Thread
    static void markDiffusivityDirty(ServerLevel level, BlockPos pos) {
        if (!NtmRadiationConfig.chunkRadsEnabled() || isOutsideWorld(level, pos)) return;
        WorldRadiationData[] all = worldMap.get(level);
        if (all == null) return;
        for (WorldRadiationData data : all) {
            if (data.diffusivityTransport) data.diffusivityDirty.add(SectionPos.asLong(pos));
        }
    }

    static boolean isOutsideWorld(ServerLevel world, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int bad = ((x << 6) >> 6) ^ x;
        bad |= ((z << 6) >> 6) ^ z;
        if (bad != 0) return true;
        return y < world.getMinBuildHeight() || y >= world.getMaxBuildHeight();
    }

    /** Pocket-Schluessel: Sektionsschluessel mit (slot << 11 | pocket) im Y-Feld. */
    static long pocketKey(long sectionKey, int pocketIndex, int slot) {
        int yz = (slot << 11) | pocketIndex;
        return SectionKeys.setSectionY(sectionKey, yz);
    }

    static double sanitize(double v, double minBound) {
        if (Double.isNaN(v) || Math.abs(v) < RAD_EPSILON && v > minBound) return 0.0D;
        return Math.max(Math.min(v, RAD_MAX), minBound);
    }

    /** Exakte Loesung des Zwei-Zellen-Austauschs (gleiche Volumina) ueber einen Zeitschritt. */
    static boolean exchangeUniExactXZ(double[] uni, int idxA, int idxB, double uuE) {
        double ra = uni[idxA], rb = uni[idxB];
        if (ra == rb) return false;
        double avg = 0.5d * (ra + rb);
        double halfDiff = 0.5d * (ra - rb);
        uni[idxA] = Math.fma(halfDiff, uuE, avg);
        uni[idxB] = Math.fma(-halfDiff, uuE, avg);
        return true;
    }

    static boolean exchangeUniExactY(double[] uni, int idxA, int idxB, double uuE) {
        double ra = uni[idxA];
        double rb = uni[idxB];
        if (ra == rb) return false;
        double avg = 0.5d * (ra + rb);
        double halfDiff = 0.5d * (ra - rb);
        uni[idxA] = Math.fma(halfDiff, uuE, avg);
        uni[idxB] = Math.fma(-halfDiff, uuE, avg);
        return true;
    }

    static boolean exchangeUniExactDiffusive(
            double[] uni,
            int idxA,
            int idxB,
            double uuE,
            double diffusionDt,
            float diffA,
            float diffB) {
        double dEff = RadiationDiffusivity.edge(8.0d, diffA, 8.0d, diffB);
        if (!(dEff > 0.0d)) return false;
        double e = dEff == 1.0d ? uuE : RadiationDiffusivity.decay((diffusionDt / 128.0d) * dEff);
        return exchangeUniExactXZ(uni, idxA, idxB, e);
    }
}
