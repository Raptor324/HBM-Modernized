// Port-eigene Klasse (HBM-Modernized), Stufe 3/4 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.main.MainRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Kontaminationsebene einer Dimension: radioaktives Material je Sektion und Isotopengruppe,
 * getrennt in Boden- und luftgetragene Menge (Stufe 4).
 *
 * <p>Je Schritt ({@link NtmRadiationConfig#contamTickInterval} Ticks):
 * <ol>
 *   <li>Zerfall exakt exponentiell je Gruppe; zerfallene Aktivitaet geht ins Feld (Stufe 3).</li>
 *   <li>Luftgetragenes sinkt ab ({@code settle_rate}, bei Regen/Schnee zusaetzlich Auswaschung) und
 *       landet als Boden auf der Oberflaechen-Sektion der Spalte.</li>
 *   <li>Wind: Upwind-Advektion des luftgetragenen Anteils in die Nachbar-Chunks (massenerhaltend),
 *       nur ueber Gelaende; ueber hoeherem Gelaende wird angehoben, unter Tage gibt es keine
 *       Advektion (sofortiges Absetzen).</li>
 *   <li>Regen: Abfluss eines kleinen Anteils der Bodenkontamination zur tiefsten Nachbar-Oberflaeche.</li>
 *   <li>Wasser: Ausgleich zwischen benachbarten Gewaesser-Sektionen, Verduennung im Ozean.</li>
 * </ol>
 * Transfers gehen nur in geladene Chunks: am Rand des geladenen Bereichs bleibt das Material
 * liegen und wandert erst weiter, wenn der Nachbar geladen ist (nie verlustbehaftet).
 *
 * <p>Nur Server-Thread. Speicherung in {@code <dim>/hbm_m/radiation_ntmnext/contamination/},
 * Format 2 (Format 1 aus Stufe 3 wird gelesen, luftgetragen = 0).
 */
final class ContaminationLayer {

    private static final byte FORMAT_V1 = 1;
    private static final byte FORMAT = 2;
    private static final double MIN_AMOUNT = 1.0e-4D;
    private static final int G = ContaminationGroup.COUNT;
    private static final int[][] DIRS = {{16, 0}, {-16, 0}, {0, 16}, {0, -16}};

    static final class Entry {
        /** Bodenkontamination je Gruppe. */
        final double[] amount = new double[G];
        /** Luftgetragene Kontamination je Gruppe (Stufe 4). */
        final double[] airborne = new double[G];
        /** Lokaler Blockindex (y<<8|z<<4|x), an dem emittiert wird. */
        int anchor;

        double total() {
            double s = 0.0D;
            for (int g = 0; g < G; g++) s += amount[g] + airborne[g];
            return s;
        }

        double groundTotal() {
            double s = 0.0D;
            for (double a : amount) s += a;
            return s;
        }

        double airborneTotal() {
            double s = 0.0D;
            for (double a : airborne) s += a;
            return s;
        }
    }

    static final class ChunkContamination {
        long lastTick;
        final Int2ObjectOpenHashMap<Entry> bySectionY = new Int2ObjectOpenHashMap<>();
    }

    /** Vorgemerkter Transfer (wird nach dem Durchlauf angewandt). */
    private record Transfer(int x, int y, int z, double[] amounts, boolean airborne) {}

    final ServerLevel level;
    final Path root;
    private final Long2ObjectOpenHashMap<ChunkContamination> chunks = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet dirty = new LongOpenHashSet();

    ContaminationLayer(ServerLevel level) {
        this.level = level;
        this.root = RegionChunkRadiationStorage.dimensionRoot(level).resolve("contamination");
    }

    // ------------------------------------------------------------------
    // Schreiben / Lesen
    // ------------------------------------------------------------------

    void add(BlockPos pos, ContaminationGroup group, double amount) {
        add(pos.getX(), pos.getY(), pos.getZ(), group.ordinal(), amount, false);
    }

    void addAirborne(BlockPos pos, ContaminationGroup group, double amount) {
        add(pos.getX(), pos.getY(), pos.getZ(), group.ordinal(), amount, true);
    }

    private void add(int x, int y, int z, int g, double amount, boolean airborne) {
        if (!(amount > 0.0D) || !Double.isFinite(amount)) return;
        Entry e = entryFor(x, y, z, true);
        if (e == null) return;
        if (airborne) e.airborne[g] += amount;
        else e.amount[g] += amount;
        dirty.add(ChunkPos.asLong(x >> 4, z >> 4));
    }

    /** Eintrag der Sektion; legt ihn an, wenn {@code create}. Null, wenn Chunk nicht geladen. */
    @Nullable
    private Entry entryFor(int x, int y, int z, boolean create) {
        int cx = x >> 4, cz = z >> 4;
        long ck = ChunkPos.asLong(cx, cz);
        ChunkContamination c = chunks.get(ck);
        if (c == null) {
            if (!create || level.getChunkSource().getChunkNow(cx, cz) == null) return null;
            c = new ChunkContamination();
            c.lastTick = level.getGameTime();
            chunks.put(ck, c);
        }
        int sy = SectionPos.blockToSectionCoord(y);
        Entry e = c.bySectionY.get(sy);
        if (e == null && create) {
            e = new Entry();
            e.anchor = SectionKeys.blockPosToLocal(BlockPos.asLong(x, y, z));
            c.bySectionY.put(sy, e);
        }
        return e;
    }

    @Nullable
    Entry entryAt(BlockPos pos) {
        ChunkContamination c = chunks.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return c == null ? null : c.bySectionY.get(SectionPos.blockToSectionCoord(pos.getY()));
    }

    /** Boden + luftgetragen einer Gruppe. */
    double amountAt(BlockPos pos, ContaminationGroup group) {
        Entry e = entryAt(pos);
        return e == null ? 0.0D : e.amount[group.ordinal()] + e.airborne[group.ordinal()];
    }

    double airborneAt(BlockPos pos, ContaminationGroup group) {
        Entry e = entryAt(pos);
        return e == null ? 0.0D : e.airborne[group.ordinal()];
    }

    /** Absolute Menge anteilig ueber alle Gruppen der Sektion abbauen (Absorber, Dekon). */
    double removeAmount(BlockPos pos, double amount) {
        if (!(amount > 0.0D)) return 0.0D;
        Entry e = entryAt(pos);
        if (e == null) return 0.0D;
        double total = e.total();
        if (!(total > 0.0D)) return 0.0D;
        double f = Math.max(0.0D, 1.0D - amount / total);
        for (int g = 0; g < G; g++) {
            e.amount[g] *= f;
            e.airborne[g] *= f;
        }
        dirty.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return total * (1.0D - f);
    }

    /** Anteil {@code fraction} aller Sektionen im Wuerfel um pos entfernen. */
    int decontaminate(BlockPos pos, int radius, double fraction) {
        double keep = 1.0D - Math.min(1.0D, Math.max(0.0D, fraction));
        int n = 0;
        int cx0 = (pos.getX() - radius) >> 4, cx1 = (pos.getX() + radius) >> 4;
        int cz0 = (pos.getZ() - radius) >> 4, cz1 = (pos.getZ() + radius) >> 4;
        int sy0 = SectionPos.blockToSectionCoord(pos.getY() - radius);
        int sy1 = SectionPos.blockToSectionCoord(pos.getY() + radius);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                long ck = ChunkPos.asLong(cx, cz);
                ChunkContamination c = chunks.get(ck);
                if (c == null) continue;
                for (int sy = sy0; sy <= sy1; sy++) {
                    Entry e = c.bySectionY.get(sy);
                    if (e == null) continue;
                    for (int g = 0; g < G; g++) {
                        e.amount[g] *= keep;
                        e.airborne[g] *= keep;
                    }
                    n++;
                    dirty.add(ck);
                }
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // Schritt
    // ------------------------------------------------------------------

    void tick() {
        if (chunks.isEmpty()) return;
        long now = level.getGameTime();
        double[] perType = new double[RadiationType.FIELD_COUNT];
        BlockPos.MutableBlockPos anchor = new BlockPos.MutableBlockPos();
        List<Transfer> transfers = new ArrayList<>();

        boolean wind = WindModel.hasWind(level);
        double[] v = wind ? WindModel.velocity(level) : new double[2];
        boolean raining = NtmRadiationConfig.rainEnabled && level.isRaining();
        boolean thunder = level.isThundering();

        ObjectIterator<Long2ObjectMap.Entry<ChunkContamination>> it =
                chunks.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Long2ObjectMap.Entry<ChunkContamination> ce = it.next();
            ChunkContamination c = ce.getValue();
            double dt = (now - c.lastTick) / 20.0D;
            c.lastTick = now;
            if (!(dt > 0.0D)) continue;
            long ck = ce.getLongKey();
            int cx = ChunkPos.getX(ck), cz = ChunkPos.getZ(ck);
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);

            ObjectIterator<Int2ObjectMap.Entry<Entry>> eit = c.bySectionY.int2ObjectEntrySet().fastIterator();
            while (eit.hasNext()) {
                Int2ObjectMap.Entry<Entry> ee = eit.next();
                Entry e = ee.getValue();
                int sy = ee.getIntKey();
                int ax = (cx << 4) + SectionKeys.getLocalX(e.anchor);
                int ay = (sy << 4) + SectionKeys.getLocalY(e.anchor);
                int az = (cz << 4) + SectionKeys.getLocalZ(e.anchor);

                // 1) Zerfall + Emission ins Feld (Boden und Luft).
                Arrays.fill(perType, 0.0D);
                for (int g = 0; g < G; g++) {
                    double a = e.amount[g] + e.airborne[g];
                    if (a <= 0.0D) continue;
                    double lambda = Math.log(2.0D) / NtmRadiationConfig.contamHalfLife[g];
                    double decay = Math.exp(-lambda * dt);
                    double emitted = NtmRadiationConfig.contamEmission[g] * a * (1.0D - decay) / lambda;
                    float[] mix = NtmRadiationConfig.contamMix[g];
                    for (int t = 0; t < RadiationType.FIELD_COUNT; t++) perType[t] += emitted * mix[t];
                    e.amount[g] = clean(e.amount[g] * decay);
                    e.airborne[g] = clean(e.airborne[g] * decay);
                }
                anchor.set(ax, ay, az);
                NtmRadiationSystem.addFieldDirect(level, anchor, perType);

                // 2) Wetter (Stufe 4).
                if (chunk != null) {
                    int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, ax & 15, az & 15) + 1;
                    weather(e, sy, ax, az, surface, dt, wind, v, raining, thunder, transfers);
                    ground(e, sy, ax, az, surface, chunk, dt, raining, transfers);
                }

                if (e.total() <= 0.0D) eit.remove();
            }
            dirty.add(ck);
            if (c.bySectionY.isEmpty()) it.remove();
        }

        // Transfers anwenden (Ziele wurden beim Vormerken als geladen geprueft).
        for (Transfer t : transfers) {
            for (int g = 0; g < G; g++) {
                if (t.amounts[g] > 0.0D) add(t.x, t.y, t.z, g, t.amounts[g], t.airborne);
            }
        }
    }

    private static double clean(double a) {
        return a < MIN_AMOUNT ? 0.0D : a;
    }

    /** Absetzen, Auswaschen und Wind fuer den luftgetragenen Anteil. */
    private void weather(
            Entry e,
            int sy,
            int ax,
            int az,
            int surface,
            double dt,
            boolean wind,
            double[] v,
            boolean raining,
            boolean thunder,
            List<Transfer> transfers) {
        if (e.airborneTotal() <= 0.0D) return;
        int sectionTop = (sy << 4) + 15;
        boolean aboveGround = sectionTop >= surface;

        // Absetzrate inkl. Niederschlag.
        double rate = NtmRadiationConfig.settleRate;
        if (raining && aboveGround) {
            BlockPos top = new BlockPos(ax, surface, az);
            Biome.Precipitation p = level.getBiome(top).value().getPrecipitationAt(top);
            double wash =
                    p == Biome.Precipitation.RAIN
                            ? NtmRadiationConfig.washoutRate
                            : p == Biome.Precipitation.SNOW ? NtmRadiationConfig.snowWashoutRate : 0.0D;
            if (thunder) wash *= NtmRadiationConfig.thunderMult;
            rate += wash;
        }
        // Unter Tage keine Luftstroemung: sofort absetzen.
        double settleFrac = aboveGround ? 1.0D - Math.exp(-rate * dt) : 1.0D;

        double[] settled = new double[G];
        for (int g = 0; g < G; g++) {
            double a = e.airborne[g];
            if (a <= 0.0D) continue;
            double s = a * settleFrac;
            settled[g] = s;
            e.airborne[g] = a - s;
        }
        // Abgesetztes landet auf der Oberflaechen-Sektion der Spalte.
        // (auf den obersten Block, damit Abfluss/Gewaesser dieselbe Sektion sehen).
        transfers.add(new Transfer(ax, Math.min(surface - 1, sectionTop), az, settled, false));

        if (!wind || !aboveGround || e.airborneTotal() <= 0.0D) return;
        double fx = Math.abs(v[0]) * dt / 16.0D;
        double fz = Math.abs(v[1]) * dt / 16.0D;
        double sum = fx + fz;
        double max = NtmRadiationConfig.windMaxFraction;
        if (sum > max && sum > 0.0D) {
            fx *= max / sum;
            fz *= max / sum;
        }
        // Beide Anteile vom Ausgangsbestand (Upwind, explizit).
        double[] base = e.airborne.clone();
        if (fx > 0.0D) advect(e, base, sy, ax + (v[0] >= 0 ? 16 : -16), az, fx, transfers);
        if (fz > 0.0D) advect(e, base, sy, ax, az + (v[1] >= 0 ? 16 : -16), fz, transfers);
    }

    /** Upwind-Schritt: Anteil {@code frac} der Luftmenge in den Nachbarchunk (nur wenn geladen). */
    private void advect(
            Entry e, double[] base, int sy, int tx, int tz, double frac, List<Transfer> transfers) {
        LevelChunk target = level.getChunkSource().getChunkNow(tx >> 4, tz >> 4);
        if (target == null) return; // am Rand des geladenen Bereichs sammeln
        int tSurface = target.getHeight(Heightmap.Types.WORLD_SURFACE, tx & 15, tz & 15) + 1;
        int ty = (sy << 4) + 8;
        if (ty < tSurface) ty = tSurface; // ueber hoeheres Gelaende anheben
        if (ty >= level.getMaxBuildHeight()) return;
        double[] out = new double[G];
        for (int g = 0; g < G; g++) {
            double m = Math.min(e.airborne[g], base[g] * frac);
            out[g] = m;
            e.airborne[g] -= m;
        }
        transfers.add(new Transfer(tx, ty, tz, out, true));
    }

    /** Abfluss bei Regen und Gewaesser-Austausch fuer die Bodenkontamination. */
    private void ground(
            Entry e,
            int sy,
            int ax,
            int az,
            int surface,
            LevelChunk chunk,
            double dt,
            boolean raining,
            List<Transfer> transfers) {
        if (e.groundTotal() <= 0.0D) return;
        // Nur die Oberflaechen-Sektion der Spalte nimmt an Abfluss/Gewaesser teil.
        if (SectionPos.blockToSectionCoord(surface - 1) != sy) return;
        int lx = ax & 15, lz = az & 15;

        int depth = surface - 1 - chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, lx, lz);
        boolean water = NtmRadiationConfig.waterEnabled && depth >= NtmRadiationConfig.waterMinDepth;

        if (water) {
            BlockPos here = new BlockPos(ax, surface - 1, az);
            if (level.getBiome(here).is(BiomeTags.IS_OCEAN)) {
                // Verduennung im Ozean: Material verteilt sich und faellt aus dem Spiel.
                double keep = Math.pow(0.5D, dt / NtmRadiationConfig.oceanDilutionHalfLife);
                for (int g = 0; g < G; g++) e.amount[g] = clean(e.amount[g] * keep);
            }
            // Ausgleich mit benachbarten Gewaesser-Sektionen (Richtung geringere Menge).
            double frac = Math.min(0.25D, NtmRadiationConfig.waterMixRate * dt);
            for (int[] d : DIRS) {
                int nx = ax + d[0], nz = az + d[1];
                LevelChunk n = level.getChunkSource().getChunkNow(nx >> 4, nz >> 4);
                if (n == null) continue;
                int nTop = n.getHeight(Heightmap.Types.WORLD_SURFACE, nx & 15, nz & 15);
                int nDepth = nTop - n.getHeight(Heightmap.Types.OCEAN_FLOOR, nx & 15, nz & 15);
                if (nDepth < NtmRadiationConfig.waterMinDepth) continue;
                Entry ne = entryFor(nx, nTop, nz, false);
                double[] out = new double[G];
                boolean any = false;
                for (int g = 0; g < G; g++) {
                    double other = ne == null ? 0.0D : ne.amount[g];
                    double diff = e.amount[g] - other;
                    if (diff <= 0.0D) continue;
                    double m = diff * 0.5D * frac;
                    out[g] = m;
                    e.amount[g] -= m;
                    any = true;
                }
                if (any) transfers.add(new Transfer(nx, nTop, nz, out, false));
            }
            return;
        }

        if (!raining || !NtmRadiationConfig.runoffEnabled) return;
        if (!level.isRainingAt(new BlockPos(ax, surface, az))) return;
        // Abfluss zur tiefsten Nachbar-Oberflaeche (Hot-Spots in Senken).
        int bestX = 0, bestZ = 0, bestY = surface - NtmRadiationConfig.runoffMinDrop;
        boolean found = false;
        for (int[] d : DIRS) {
            int nx = ax + d[0], nz = az + d[1];
            LevelChunk n = level.getChunkSource().getChunkNow(nx >> 4, nz >> 4);
            if (n == null) continue;
            int ny = n.getHeight(Heightmap.Types.WORLD_SURFACE, nx & 15, nz & 15) + 1;
            if (ny <= bestY) {
                bestY = ny;
                bestX = nx;
                bestZ = nz;
                found = true;
            }
        }
        if (!found) return;
        double frac = Math.min(0.5D, NtmRadiationConfig.runoffRate * dt);
        double[] out = new double[G];
        for (int g = 0; g < G; g++) {
            double m = e.amount[g] * frac;
            out[g] = m;
            e.amount[g] -= m;
        }
        transfers.add(new Transfer(bestX, bestY - 1, bestZ, out, false));
    }

    // ------------------------------------------------------------------
    // Chunks / Persistenz
    // ------------------------------------------------------------------

    void onChunkLoad(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        byte[] raw;
        try {
            raw = NtmRadiationSystem.SIDE_CAR.read(level, root, pos);
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] contamination read failed for {}", pos, ex);
            return;
        }
        if (raw == null || raw.length == 0) return;
        try {
            ChunkContamination c = decode(raw);
            if (c == null || c.bySectionY.isEmpty()) return;
            // Nachzerfall fuer die Zeit, in der der Chunk nicht geladen war; Luftgetragenes hat sich
            // nach mehr als 10 Minuten abgesetzt.
            long now = level.getGameTime();
            double dt = Math.max(0L, now - c.lastTick) / 20.0D;
            for (Entry e : c.bySectionY.values()) {
                for (int g = 0; g < G; g++) {
                    double keep = dt > 0.0D ? Math.pow(0.5D, dt / NtmRadiationConfig.contamHalfLife[g]) : 1.0D;
                    if (dt > 600.0D) {
                        e.amount[g] += e.airborne[g];
                        e.airborne[g] = 0.0D;
                    }
                    e.amount[g] = clean(e.amount[g] * keep);
                    e.airborne[g] = clean(e.airborne[g] * keep);
                }
            }
            c.lastTick = now;
            ChunkContamination existing = chunks.get(pos.toLong());
            if (existing != null) {
                for (Int2ObjectMap.Entry<Entry> ee : existing.bySectionY.int2ObjectEntrySet()) {
                    Entry target = c.bySectionY.get(ee.getIntKey());
                    if (target == null) {
                        c.bySectionY.put(ee.getIntKey(), ee.getValue());
                    } else {
                        for (int g = 0; g < G; g++) {
                            target.amount[g] += ee.getValue().amount[g];
                            target.airborne[g] += ee.getValue().airborne[g];
                        }
                    }
                }
            }
            chunks.put(pos.toLong(), c);
        } catch (BufferUnderflowException | IllegalStateException ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] contamination data invalid for {}, dropped", pos, ex);
        }
    }

    void onChunkUnload(LevelChunk chunk) {
        long ck = chunk.getPos().toLong();
        if (dirty.remove(ck)) write(ck, chunks.get(ck));
        chunks.remove(ck);
    }

    void saveDirty() {
        for (LongIterator it = dirty.iterator(); it.hasNext(); ) {
            long ck = it.nextLong();
            write(ck, chunks.get(ck));
        }
        dirty.clear();
    }

    void clear() {
        chunks.clear();
        dirty.clear();
    }

    private void write(long ck, @Nullable ChunkContamination c) {
        byte[] payload = c == null ? null : encode(c);
        try {
            NtmRadiationSystem.SIDE_CAR.write(
                    level, root, new ChunkPos(ChunkPos.getX(ck), ChunkPos.getZ(ck)), payload);
        } catch (RuntimeException ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] contamination write failed", ex);
        }
    }

    @Nullable
    private static byte[] encode(ChunkContamination c) {
        int n = 0;
        for (Entry e : c.bySectionY.values()) if (e.total() > 0.0D) n++;
        if (n == 0) return null;
        ByteBuffer buf = ByteBuffer.allocate(1 + 8 + 4 + n * (4 + 2 + 16 * G));
        buf.put(FORMAT).putLong(c.lastTick).putInt(n);
        for (Int2ObjectMap.Entry<Entry> ee : c.bySectionY.int2ObjectEntrySet()) {
            Entry e = ee.getValue();
            if (!(e.total() > 0.0D)) continue;
            buf.putInt(ee.getIntKey()).putShort((short) e.anchor);
            for (double a : e.amount) buf.putDouble(a);
            for (double a : e.airborne) buf.putDouble(a);
        }
        return buf.array();
    }

    @Nullable
    private static ChunkContamination decode(byte[] raw) {
        ByteBuffer buf = ByteBuffer.wrap(raw);
        byte format = buf.get();
        if (format != FORMAT && format != FORMAT_V1)
            throw new IllegalStateException("Unknown contamination format " + format);
        ChunkContamination c = new ChunkContamination();
        c.lastTick = buf.getLong();
        int n = buf.getInt();
        if (n < 0 || n > 4096) throw new IllegalStateException("Invalid entry count " + n);
        for (int i = 0; i < n; i++) {
            int sy = buf.getInt();
            Entry e = new Entry();
            e.anchor = buf.getShort() & 0xFFF;
            for (int g = 0; g < G; g++) e.amount[g] = finite(buf.getDouble());
            if (format >= FORMAT) for (int g = 0; g < G; g++) e.airborne[g] = finite(buf.getDouble());
            c.bySectionY.put(sy, e);
        }
        return c;
    }

    private static double finite(double a) {
        return Double.isFinite(a) && a > 0.0D ? a : 0.0D;
    }

    int loadedSections() {
        int n = 0;
        for (ChunkContamination c : chunks.values()) n += c.bySectionY.size();
        return n;
    }
}
