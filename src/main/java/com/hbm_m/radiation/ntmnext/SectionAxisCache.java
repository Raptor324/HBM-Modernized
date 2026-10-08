// Port-eigene Klasse (HBM-Modernized), Stufe 3 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * Achsen-Diffusivitaet uniformer Sektionen ("Spaltenmodell", Stufe 3).
 *
 * <p>Stufe 2 mittelte die Block-Leitwerte einer Sektion harmonisch (alles in Reihe). Dadurch
 * wirkte eine einzelne dichte Wand kaum, und eine halb mit Erde gefuellte Sektion bremste die
 * seitliche Ausbreitung ueber dem Boden zu stark. Jetzt wird je Achse gerechnet: jede der 256
 * Blockspalten entlang der Achse ist eine Reihenschaltung (Widerstaende addieren sich), die
 * Spalten liegen parallel (Leitwerte mitteln sich arithmetisch). Ergebnis je Achse:
 * {@code D = mean(16 / sum r)}. Eine geschlossene Wand quer zur Achse sperrt alle Spalten, eine
 * Bodenschicht parallel zur Achse laesst die Luftspalten frei.
 *
 * <p>Kosten: ein Durchlauf ueber 4096 Bloecke fuer alle drei Strahlungsarten zusammen, nur fuer
 * Sektionen mit gemischtem Material (einheitliche Sektionen per Histogramm). Ergebnis je
 * Dimension und Sektion gecacht; invalidiert bei Blockwechsel (Mixin) und Chunk-Entladen.
 */
final class SectionAxisCache {

    private static final Map<ServerLevel, ConcurrentHashMap<Long, float[]>> CACHE =
            new ConcurrentHashMap<>();

    private static final ThreadLocal<double[]> TL_ACC =
            ThreadLocal.withInitial(() -> new double[RadiationType.FIELD_COUNT * 3 * 256]);

    private SectionAxisCache() {}

    /** Schreibt die 3 Achsenwerte (X, Y, Z) der Art von {@code data} nach dst[off..off+2]. */
    static void fill(
            WorldRadiationData data,
            long sectionKey,
            @Nullable LevelChunkSection section,
            float[] dst,
            int off) {
        float[] all;
        if (section == null || section.hasOnlyAir()) {
            all = null;
        } else {
            all =
                    CACHE.computeIfAbsent(data.world, w -> new ConcurrentHashMap<>())
                            .computeIfAbsent(sectionKey, k -> compute(section));
        }
        int base = data.type.ordinal() * 3;
        for (int a = 0; a < 3; a++) {
            dst[off + a] = all == null ? RadiationDiffusivity.NEUTRAL : all[base + a];
        }
    }

    static void invalidate(ServerLevel level, long sectionKey) {
        ConcurrentHashMap<Long, float[]> map = CACHE.get(level);
        if (map != null) map.remove(sectionKey);
    }

    static void invalidateChunk(ServerLevel level, int cx, int cz) {
        ConcurrentHashMap<Long, float[]> map = CACHE.get(level);
        if (map == null) return;
        int min = level.getMinSection();
        int max = level.getMaxSection();
        for (int sy = min; sy < max; sy++) {
            map.remove(net.minecraft.core.SectionPos.asLong(cx, sy, cz));
        }
    }

    static void clear(@Nullable ServerLevel level) {
        if (level == null) CACHE.clear();
        else CACHE.remove(level);
    }

    /** float[9]: je Art (gamma, neutron, beta) die Achsen X, Y, Z. */
    private static float[] compute(LevelChunkSection section) {
        float[] out = new float[RadiationType.FIELD_COUNT * 3];
        Arrays.fill(out, RadiationDiffusivity.NEUTRAL);
        float[][] res = RadiationShieldingTable.res;
        if (res[0].length == 0) return out;
        PalettedContainer<BlockState> container = section.getStates();

        // Einheitliches Material: Spalten identisch -> D = 1/r auf allen Achsen.
        BlockState[] single = {null};
        int[] distinct = {0};
        container.count(
                (state, n) -> {
                    distinct[0]++;
                    single[0] = state;
                });
        if (distinct[0] == 1 && single[0] != null) {
            int id = Block.BLOCK_STATE_REGISTRY.getId(single[0]);
            for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
                float r = id >= 0 && id < res[t].length ? res[t][id] : 1.0F;
                float d = RadiationDiffusivity.clamp(1.0F / r);
                out[t * 3] = d;
                out[t * 3 + 1] = d;
                out[t * 3 + 2] = d;
            }
            return out;
        }

        double[] acc = TL_ACC.get();
        Arrays.fill(acc, 0.0D);
        for (int i = 0; i < SectionScanner.SECTION_BLOCK_COUNT; i++) {
            int x = i & 15, z = (i >>> 4) & 15, y = i >>> 8;
            BlockState state = container.get(x, y, z);
            int id = Block.BLOCK_STATE_REGISTRY.getId(state);
            for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
                double r = id >= 0 && id < res[t].length ? res[t][id] : 1.0D;
                int b = t * 3 * 256;
                acc[b + (y << 4 | z)] += r; // X-Spalte
                acc[b + 256 + (x << 4 | z)] += r; // Y-Spalte
                acc[b + 512 + (x << 4 | y)] += r; // Z-Spalte
            }
        }
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            for (int a = 0; a < 3; a++) {
                int b = (t * 3 + a) * 256;
                double sum = 0.0D;
                for (int line = 0; line < 256; line++) sum += 16.0D / acc[b + line];
                out[t * 3 + a] = RadiationDiffusivity.clamp((float) (sum / 256.0D));
            }
        }
        return out;
    }
}
