// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (ChunkRadiationStorage + RegionChunkRadiationStorage), Commit 3f9a261a.
//
// Eigene Chunk-Datei: <welt>/<dimension>/hbm_m/radiation_ntmnext/r.X.Z.mca (Anvil-Regionsformat,
// ein CompoundTag {"payload": byte[]} je Chunk). Raptors Strahlungsdaten (Chunk-Capability im
// Chunk-NBT) werden dadurch nie beruehrt; Zurueckschalten auf RAPTOR ist verlustfrei.
//
// Port-Abweichung: RegionFileStorage ist in 1.20.1 nicht oeffentlich konstruierbar; der Port
// verwaltet die RegionFile-Instanzen selbst (LRU-Cache wie RegionFileStorage, max. 256 offen).

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.main.MainRegistry;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

/** Persistenz der Strahlungsdichten je Chunk (Rohbytes, Format siehe WorldRadiationData). */
final class RegionChunkRadiationStorage {
    private static final String PAYLOAD_KEY = "payload";
    private static final String ANVIL_EXTENSION = ".mca";
    private static final int MAX_OPEN_REGIONS = 256;

    private final ConcurrentHashMap<Path, RadiationRegionStorage> storages =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, Object> rootLocks = new ConcurrentHashMap<>();

    static Path dimensionRoot(ServerLevel level) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        return DimensionType.getStorageFolder(level.dimension(), root)
                .resolve("hbm_m")
                .resolve("radiation_ntmnext");
    }

    private static Path regionPath(Path root, ChunkPos pos) {
        return root.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ANVIL_EXTENSION);
    }

    static Path retireDirectory(Path root) throws IOException {
        Path trash = nextTrashPath(root);
        Files.move(root, trash, StandardCopyOption.ATOMIC_MOVE);
        return trash;
    }

    private static Path nextTrashPath(Path root) {
        Path parent = root.getParent();
        String base =
                root.getFileName() + ".trash." + Long.toUnsignedString(System.currentTimeMillis());
        Path trash = parent.resolve(base);
        int suffix = 0;
        while (Files.exists(trash)) trash = parent.resolve(base + "." + ++suffix);
        return trash;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            Iterator<Path> it = paths.sorted(Comparator.reverseOrder()).iterator();
            while (it.hasNext()) {
                Files.deleteIfExists(it.next());
            }
        }
    }

    @Nullable
    byte[] read(ServerLevel level, Path root, ChunkPos pos) {
        synchronized (rootLock(root)) {
            RadiationRegionStorage open = storages.get(root);
            if (open == null) {
                Path region = regionPath(root, pos);
                if (!Files.exists(region)) return null;
                try {
                    if (Files.size(region) == 0L) {
                        Files.deleteIfExists(region);
                        return null;
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            return (open != null ? open : storage(level, root)).read(pos);
        }
    }

    void write(ServerLevel level, Path root, ChunkPos pos, @Nullable byte[] payload) {
        synchronized (rootLock(root)) {
            RadiationRegionStorage open = storages.get(root);
            if (payload == null && open == null) {
                Path region = regionPath(root, pos);
                if (!Files.exists(region)) return;
                try {
                    if (Files.size(region) == 0L) {
                        Files.deleteIfExists(region);
                        return;
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            (open != null ? open : storage(level, root)).write(pos, payload);
        }
    }

    void discard(ServerLevel level, Path root, ChunkPos pos) {
        write(level, root, pos, null);
    }

    void flush() {
        RuntimeException failure = null;
        for (RadiationRegionStorage storage : storages.values()) {
            try {
                storage.flush();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        if (failure != null) throw failure;
    }

    /** Verwirft alle Daten einer Dimension (Verzeichnis umbenennen, asynchron loeschen). */
    void deleteDimension(Path root) {
        Path trash;
        synchronized (rootLock(root)) {
            RadiationRegionStorage storage = storages.get(root);
            if (storage != null) {
                storage.flush();
                storage.closeAfterFlush();
                storages.remove(root, storage);
            }
            if (!Files.exists(root)) return;
            try {
                trash = retireDirectory(root);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        Util.ioPool()
                .execute(
                        () -> {
                            try {
                                deleteRecursively(trash);
                            } catch (IOException ex) {
                                MainRegistry.LOGGER.warn(
                                        "Failed to delete retired radiation directory {}",
                                        trash,
                                        ex);
                            }
                        });
    }

    void close() {
        RuntimeException failure = null;
        for (RadiationRegionStorage storage : storages.values()) {
            try {
                storage.close();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
        }
        storages.clear();
        rootLocks.clear();
        if (failure != null) throw failure;
    }

    private RadiationRegionStorage storage(ServerLevel level, Path root) {
        return storages.computeIfAbsent(
                root, r -> new RadiationRegionStorage(r, level.getServer().forceSynchronousWrites()));
    }

    private Object rootLock(Path root) {
        return rootLocks.computeIfAbsent(root, ignored -> new Object());
    }

    /** Regionsdateien einer Dimension, inkl. ausstehender (fehlgeschlagener) Schreibvorgaenge. */
    static final class RadiationRegionStorage {
        private final Path root;
        private final boolean sync;
        private final Long2ObjectLinkedOpenHashMap<RegionFile> regions =
                new Long2ObjectLinkedOpenHashMap<>();
        private final Long2ObjectOpenHashMap<PendingWrite> pendingWrites =
                new Long2ObjectOpenHashMap<>();
        private boolean needsFlush;

        RadiationRegionStorage(Path root, boolean sync) {
            this.root = root;
            this.sync = sync;
        }

        private boolean hasRegion(ChunkPos pos) throws IOException {
            if (regions.containsKey(ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ())))
                return true;
            Path region = regionPath(root, pos);
            if (!Files.exists(region)) return false;
            if (Files.size(region) != 0L) return true;
            Files.deleteIfExists(region);
            return false;
        }

        private RegionFile region(ChunkPos pos) throws IOException {
            long key = ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ());
            RegionFile file = regions.getAndMoveToFirst(key);
            if (file != null) return file;
            if (regions.size() >= MAX_OPEN_REGIONS) regions.removeLast().close();
            Files.createDirectories(root);
            file = new RegionFile(regionPath(root, pos), root, sync);
            regions.putAndMoveToFirst(key, file);
            return file;
        }

        synchronized @Nullable byte[] read(ChunkPos pos) {
            PendingWrite pending = pendingWrites.get(pos.toLong());
            if (pending != null) return pending.payload;
            try {
                if (!hasRegion(pos)) return null;
                CompoundTag tag;
                try (DataInputStream in = region(pos).getChunkDataInputStream(pos)) {
                    if (in == null) return null;
                    tag = NbtIo.read(in);
                }
                return tag.contains(PAYLOAD_KEY, Tag.TAG_BYTE_ARRAY)
                        ? tag.getByteArray(PAYLOAD_KEY)
                        : null;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        synchronized void write(ChunkPos pos, @Nullable byte[] payload) {
            PendingWrite pending = new PendingWrite(pos, payload);
            pendingWrites.put(pos.toLong(), pending);
            writePending(pending);
        }

        private void writePending(PendingWrite pending) {
            try {
                if (pending.payload == null || pending.payload.length == 0) {
                    if (!hasRegion(pending.pos)) {
                        acknowledge(pending);
                        return;
                    }
                    region(pending.pos).clear(pending.pos);
                } else {
                    CompoundTag tag = new CompoundTag();
                    tag.putByteArray(PAYLOAD_KEY, pending.payload);
                    try (DataOutputStream out = region(pending.pos).getChunkDataOutputStream(pending.pos)) {
                        NbtIo.write(tag, out);
                    }
                }
                needsFlush = true;
                acknowledge(pending);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private void acknowledge(PendingWrite pending) {
            long key = pending.pos.toLong();
            if (pendingWrites.get(key) == pending) pendingWrites.remove(key);
        }

        synchronized void flush() {
            PendingWrite[] pending = pendingWrites.values().toArray(PendingWrite[]::new);
            RuntimeException failure = null;
            for (PendingWrite write : pending) {
                try {
                    writePending(write);
                } catch (RuntimeException ex) {
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            if (failure != null) throw failure;
            if (!needsFlush) return;
            try {
                for (RegionFile file : regions.values()) file.flush();
                needsFlush = false;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        synchronized void close() {
            RuntimeException failure = null;
            try {
                flush();
            } catch (RuntimeException ex) {
                failure = ex;
            }
            try {
                closeAfterFlush();
            } catch (RuntimeException ex) {
                if (failure == null) failure = ex;
                else failure.addSuppressed(ex);
            }
            if (failure != null) throw failure;
        }

        synchronized void closeAfterFlush() {
            IOException failure = null;
            for (RegionFile file : regions.values()) {
                try {
                    file.close();
                } catch (IOException ex) {
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
            regions.clear();
            if (failure != null) throw new UncheckedIOException(failure);
        }

        private record PendingWrite(ChunkPos pos, @Nullable byte[] payload) {}
    }
}
