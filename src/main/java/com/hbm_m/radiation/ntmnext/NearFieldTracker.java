// Port-eigene Klasse (HBM-Modernized), Stufe 2 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nahfeld je Dimension: Punktquellen mit Abstandsquadratgesetz und Beer-Lambert-Daempfung entlang
 * des Strahls Quelle -> Abfragepunkt. Ergaenzt das (sektionsweise gemittelte) Diffusionsfeld um
 * die Naehe zu einzelnen strahlenden Bloecken.
 *
 * <p>Punktquellen sind Positionen, an denen wiederholt {@code incrementRad} mit positiver Menge
 * aufgerufen wird (BlockHazard, Reaktoren ...). Einmalige Eintraege (Explosionen, Regen) werden
 * erst ab dem zweiten Aufruf zur Quelle, damit keine Spitzen entstehen. Rate = letzte Menge /
 * Abstand zwischen zwei Aufrufen. Quellen ohne Auffrischung verfallen.
 *
 * <p>Nur Server-Thread, unabhaengig vom asynchronen Solver (kein Warten auf die Simulation).
 * Ergebnisse je Abfrageposition werden {@link NtmRadiationConfig#nearCacheTicks} Ticks gecacht.
 */
final class NearFieldTracker {

    private static final int DEFAULT_INTERVAL = 20;
    private static final int MIN_EXPIRY = 100;

    static final class Emitter {
        final long pos;
        /** RAD/s je Feldart (gamma, neutron, beta) aus dem letzten vollstaendigen Intervall. */
        final float[] rate = new float[RadiationType.FIELD_COUNT];
        /** In diesem Tick gesammelte Mengen. */
        final float[] accum = new float[RadiationType.FIELD_COUNT];
        long lastTick = Long.MIN_VALUE;
        long accumTick = Long.MIN_VALUE;
        int interval = DEFAULT_INTERVAL;
        int calls;

        Emitter(long pos) {
            this.pos = pos;
        }

        boolean active() {
            return calls >= 2;
        }
    }

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<Emitter> emitters = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> bySection = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<float[]> cache = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<long[]> cacheTick = new Long2ObjectOpenHashMap<>();
    private long lastCleanup;

    NearFieldTracker(ServerLevel level) {
        this.level = level;
    }

    /** Emission an einer Blockposition melden (Menge bereits je Art aufgeteilt). */
    void record(BlockPos pos, float[] amountByType) {
        if (!NtmRadiationConfig.nearEnabled) return;
        long now = level.getGameTime();
        long key = pos.asLong();
        Emitter e = emitters.get(key);
        if (e == null) {
            e = new Emitter(key);
            emitters.put(key, e);
            bySection
                    .computeIfAbsent(SectionPos.blockToSection(key), k -> new LongOpenHashSet())
                    .add(key);
        }
        if (now != e.accumTick) {
            if (e.accumTick != Long.MIN_VALUE) {
                // Vorheriges Intervall abschliessen.
                e.interval = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, now - e.lastTick));
                float seconds = e.interval / 20.0F;
                for (int t = 0; t < RadiationType.FIELD_COUNT; t++) e.rate[t] = e.accum[t] / seconds;
            }
            Arrays.fill(e.accum, 0.0F);
            e.accumTick = now;
            e.lastTick = now;
            e.calls++;
        }
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) e.accum[t] += amountByType[t];
        if (now - lastCleanup > 200) cleanup(now);
    }

    private void cleanup(long now) {
        lastCleanup = now;
        ObjectIterator<Long2ObjectMap.Entry<Emitter>> it = emitters.long2ObjectEntrySet().fastIterator();
        LongArrayList removed = new LongArrayList();
        while (it.hasNext()) {
            Emitter e = it.next().getValue();
            if (now - e.lastTick > Math.max(MIN_EXPIRY, 3L * e.interval)) {
                removed.add(e.pos);
                it.remove();
            }
        }
        for (int i = 0; i < removed.size(); i++) {
            long key = removed.getLong(i);
            long sk = SectionPos.blockToSection(key);
            LongOpenHashSet set = bySection.get(sk);
            if (set != null) {
                set.remove(key);
                if (set.isEmpty()) bySection.remove(sk);
            }
        }
        cache.clear();
        cacheTick.clear();
    }

    void clear() {
        emitters.clear();
        bySection.clear();
        cache.clear();
        cacheTick.clear();
    }

    /** Nahfeldbeitrag (RAD/s je Feldart) an der Blockposition. Ergebnis nicht veraendern. */
    float[] query(BlockPos pos) {
        if (!NtmRadiationConfig.nearEnabled || emitters.isEmpty()) return ZERO;
        long now = level.getGameTime();
        if (now - lastCleanup > 200) cleanup(now);
        long key = pos.asLong();
        long[] stamp = cacheTick.get(key);
        if (stamp != null && now - stamp[0] < NtmRadiationConfig.nearCacheTicks) return cache.get(key);

        float[] result = compute(pos, now);
        cache.put(key, result);
        cacheTick.put(key, new long[] {now});
        return result;
    }

    private static final float[] ZERO = new float[RadiationType.FIELD_COUNT];

    private float[] compute(BlockPos pos, long now) {
        int radius = Math.max(1, NtmRadiationConfig.nearRadius);
        double r2max = (double) radius * radius;
        double px = pos.getX() + 0.5D, py = pos.getY() + 0.5D, pz = pos.getZ() + 0.5D;

        // Kandidaten aus den Sektionen im Umkreis sammeln.
        int maxN = Math.max(1, NtmRadiationConfig.nearMaxEmitters);
        Emitter[] best = new Emitter[maxN];
        double[] bestScore = new double[maxN];
        int count = 0;
        int sx0 = SectionPos.blockToSectionCoord(pos.getX() - radius);
        int sx1 = SectionPos.blockToSectionCoord(pos.getX() + radius);
        int sy0 = SectionPos.blockToSectionCoord(pos.getY() - radius);
        int sy1 = SectionPos.blockToSectionCoord(pos.getY() + radius);
        int sz0 = SectionPos.blockToSectionCoord(pos.getZ() - radius);
        int sz1 = SectionPos.blockToSectionCoord(pos.getZ() + radius);
        for (int sx = sx0; sx <= sx1; sx++) {
            for (int sy = sy0; sy <= sy1; sy++) {
                for (int sz = sz0; sz <= sz1; sz++) {
                    LongOpenHashSet set = bySection.get(SectionPos.asLong(sx, sy, sz));
                    if (set == null) continue;
                    for (var it = set.iterator(); it.hasNext(); ) {
                        Emitter e = emitters.get(it.nextLong());
                        if (e == null || !e.active()) continue;
                        if (now - e.lastTick > Math.max(MIN_EXPIRY, 3L * e.interval)) continue;
                        double d2 = dist2(e.pos, px, py, pz);
                        if (d2 >= r2max) continue;
                        float total = e.rate[0] + e.rate[1] + e.rate[2];
                        if (!(total > 0.0F)) continue;
                        double score = total / Math.max(1.0D, d2);
                        // Top-N nach Rate/d^2 halten.
                        if (count < maxN) {
                            best[count] = e;
                            bestScore[count] = score;
                            count++;
                        } else {
                            int min = 0;
                            for (int i = 1; i < maxN; i++) if (bestScore[i] < bestScore[min]) min = i;
                            if (score > bestScore[min]) {
                                best[min] = e;
                                bestScore[min] = score;
                            }
                        }
                    }
                }
            }
        }
        if (count == 0) return ZERO;

        float[] out = new float[RadiationType.FIELD_COUNT];
        double gain = NtmRadiationConfig.nearGain;
        double edge = 1.0D / r2max;
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        double[] tau = new double[RadiationType.FIELD_COUNT];
        for (int i = 0; i < count; i++) {
            Emitter e = best[i];
            double d2 = dist2(e.pos, px, py, pz);
            double geo = 1.0D / Math.max(1.0D, d2) - edge;
            if (!(geo > 0.0D)) continue;
            opticalDepth(e.pos, pos, probe, tau);
            for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
                if (e.rate[t] <= 0.0F) continue;
                out[t] += (float) (gain * e.rate[t] * geo * Math.exp(-tau[t]));
            }
        }
        return out;
    }

    private static double dist2(long epos, double px, double py, double pz) {
        double dx = BlockPos.getX(epos) + 0.5D - px;
        double dy = BlockPos.getY(epos) + 0.5D - py;
        double dz = BlockPos.getZ(epos) + 0.5D - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Beer-Lambert: Summe mu*Weglaenge je Art entlang der Strecke (Halbblock-Schritte). Quell- und
     * Zielblock zaehlen nicht mit (die Quelle sitzt im eigenen Block, der Spieler im Zielblock).
     */
    private void opticalDepth(long epos, BlockPos target, BlockPos.MutableBlockPos probe, double[] tau) {
        Arrays.fill(tau, 0.0D);
        double x0 = BlockPos.getX(epos) + 0.5D, y0 = BlockPos.getY(epos) + 0.5D, z0 = BlockPos.getZ(epos) + 0.5D;
        double x1 = target.getX() + 0.5D, y1 = target.getY() + 0.5D, z1 = target.getZ() + 0.5D;
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) Math.ceil(len * 2.0D);
        if (steps <= 1) return;
        double step = len / steps;
        long sourceKey = epos;
        long targetKey = target.asLong();
        for (int s = 1; s < steps; s++) {
            double f = (s + 0.5D) / steps;
            probe.set(
                    (int) Math.floor(x0 + dx * f),
                    (int) Math.floor(y0 + dy * f),
                    (int) Math.floor(z0 + dz * f));
            long k = probe.asLong();
            if (k == sourceKey || k == targetKey) continue;
            if (!level.isLoaded(probe)) continue;
            BlockState state = level.getBlockState(probe);
            if (state.isAir()) continue;
            for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
                tau[t] += RadiationShieldingTable.mu(RadiationType.FIELD_TYPES[t], state) * step;
            }
        }
    }

    /**
     * Hat diese Position schon frueher (in einem anderen Tick, noch nicht verfallen) emittiert?
     * Dann ist sie eine dauerhafte Quelle (Stufe 3: Feld statt Kontamination).
     */
    boolean isPeriodic(BlockPos pos) {
        Emitter e = emitters.get(pos.asLong());
        if (e == null || e.calls < 1) return false;
        long now = level.getGameTime();
        if (e.accumTick == now && e.calls < 2) return false;
        return now - e.lastTick <= Math.max(MIN_EXPIRY, 3L * e.interval);
    }

    int emitterCount() {
        return emitters.size();
    }
}
