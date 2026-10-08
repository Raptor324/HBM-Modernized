// Port-eigene Klasse (HBM-Modernized), Stufe 2 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.main.MainRegistry;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Materialabschirmung je BlockState und Strahlungsart nach Halbwertsschicht (HVL, in Bloecken).
 *
 * <p>Beer-Lambert: Durchlass durch eine Strecke L im Material = 2^(-L / HVL) = exp(-mu * L) mit
 * mu = ln2 / HVL. Die Werte sind echte Groessenordnungen (Blei << Beton << Erde fuer Gamma,
 * wasserstoffreiche Stoffe stark gegen Neutronen, Beta von fast allem Festen gestoppt), aber auf
 * HBM-Spielskala gestaucht (1 Block Beton ist hier ~1 HVL statt ~20).
 *
 * <p>Verwendung:
 * <ul>
 *   <li>Feld (Solver): Block-Diffusivitaet D = exp(-mu) als Leitwert je Zelle; die Sektion/Pocket
 *       mittelt harmonisch (Reihenschaltung) wie im Original-Diffusivitaetsmodell.</li>
 *   <li>Nahfeld: exakter Beer-Lambert-Faktor entlang des Strahls Quelle -> Abfragepunkt.</li>
 * </ul>
 *
 * <p>Quellen der Werte: JSON {@code data/<ns>/ntm_radiation/shielding/*.json}, sonst Fallback aus
 * Blockeigenschaften (Luft, Fluessigkeit, Kollisionsform, Klang, Explosionswiderstand), so dass auch
 * Bloecke anderer Mods sinnvolle Werte bekommen. Vorberechnet als Arrays je BlockState-ID.
 */
public final class RadiationShieldingTable {

    static final float INF = Float.POSITIVE_INFINITY;
    /** Obergrenze mu je Block (2^-29 Durchlass), haelt exp() stabil. */
    static final float MU_MAX = 20.0F;
    /** Untergrenze der Block-Diffusivitaet. */
    static final float D_MIN = 1.0e-4F;

    /** Explizite HVL je Block (gamma, neutron, beta); NaN = Fallback nutzen. */
    static final Reference2ObjectOpenHashMap<Block, float[]> EXPLICIT = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectOpenHashMap<Block, Integer> EXPLICIT_PRIORITY =
            new Reference2ObjectOpenHashMap<>();

    /** mu[type][stateId] und D[type][stateId]. */
    private static float[][] mu = new float[RadiationType.FIELD_COUNT][0];
    private static float[][] diff = new float[RadiationType.FIELD_COUNT][0];
    /**
     * Stufe 3: Diffusionswiderstand je Block r = 1 + L * (e^mu - 1), L = wall_length_scale (16).
     * So schwaecht eine einzelne Wandschicht den Fluss durch eine 16er-Spalte genau um den
     * Beer-Lambert-Faktor e^-mu (Spalte: 15 + r = 16 * e^mu), statt nur um r/16 wie mit r = e^mu.
     */
    static float[][] res = new float[RadiationType.FIELD_COUNT][0];
    private static boolean loaded;

    private RadiationShieldingTable() {}

    static void clear() {
        EXPLICIT.clear();
        EXPLICIT_PRIORITY.clear();
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            mu[t] = new float[0];
            diff[t] = new float[0];
            res[t] = new float[0];
        }
        loaded = false;
    }

    /** Eintrag aus JSON; hoehere Prioritaet gewinnt, bei Gleichstand der spaetere. */
    static void put(Block block, float[] hvl, int priority) {
        Integer old = EXPLICIT_PRIORITY.get(block);
        if (old != null && old > priority) return;
        float[] merged = EXPLICIT.get(block);
        if (merged == null || old == null || old < priority) {
            merged = new float[] {Float.NaN, Float.NaN, Float.NaN};
        }
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            if (!Float.isNaN(hvl[t])) merged[t] = hvl[t];
        }
        EXPLICIT.put(block, merged);
        EXPLICIT_PRIORITY.put(block, priority);
    }

    /** Alle BlockStates vorberechnen (Serverstart, nach dem Laden der JSON-Dateien). */
    static void bake() {
        int size = Block.BLOCK_STATE_REGISTRY.size();
        for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
            mu[t] = new float[size];
            diff[t] = new float[size];
            res[t] = new float[size];
            Arrays.fill(diff[t], 1.0F);
            Arrays.fill(res[t], 1.0F);
        }
        int nonNeutral = 0;
        float[] hvl = new float[RadiationType.FIELD_COUNT];
        for (int id = 0; id < size; id++) {
            BlockState state = Block.BLOCK_STATE_REGISTRY.byId(id);
            if (state == null) continue;
            float fill = fallback(state, hvl);
            float[] explicit = EXPLICIT.get(state.getBlock());
            if (explicit != null) {
                for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
                    if (!Float.isNaN(explicit[t])) hvl[t] = explicit[t];
                }
            }
            boolean any = false;
            for (int t = 0; t < RadiationType.FIELD_COUNT; t++) {
                float h = (float) (hvl[t] * NtmRadiationConfig.hvlScale[t]);
                float m = (h > 0.0F && Float.isFinite(h)) ? (float) (Math.log(2.0D) / h) * fill : 0.0F;
                if (m > MU_MAX) m = MU_MAX;
                mu[t][id] = m;
                float r = (float) (1.0D + NtmRadiationConfig.wallScale * (Math.exp(m) - 1.0D));
                if (!Float.isFinite(r) || r > 1.0e9F) r = 1.0e9F;
                res[t][id] = r;
                diff[t][id] = Math.max(D_MIN, 1.0F / r);
                any |= m > 0.0F;
            }
            if (any) nonNeutral++;
        }
        loaded = true;
        MainRegistry.LOGGER.info(
                "[NtmRadiation] shielding table: {} explicit blocks, {} of {} states attenuate",
                EXPLICIT.size(),
                nonNeutral,
                size);
    }

    /**
     * Fallback-HVL aus Blockeigenschaften in {@code out} (gamma, neutron, beta).
     *
     * @return Fuellgrad 0..1 (Teilbloecke wie Stufen/Platten schirmen nur halb).
     */
    static float fallback(BlockState state, float[] out) {
        if (state.isAir()) {
            out[0] = INF;
            out[1] = INF;
            out[2] = INF;
            return 0.0F;
        }
        FluidState fluid = state.getFluidState();
        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            if (fluid.is(FluidTags.LAVA)) {
                set(out, 2.0F, 3.0F, 0.2F);
            } else {
                // Wasser und wasserartige Fluessigkeiten: wasserstoffreich -> Neutronenbremse.
                set(out, 4.0F, 0.5F, 0.5F);
            }
            return 1.0F;
        }
        if (state.is(BlockTags.LEAVES)) {
            set(out, 8.0F, 4.0F, 1.0F);
            return 1.0F;
        }
        boolean emptyShape;
        boolean fullShape;
        try {
            emptyShape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
            fullShape = state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException ex) {
            emptyShape = false;
            fullShape = true;
        }
        if (emptyShape) {
            // Pflanzen, Fackeln, Teppich ...: nur Beta wird etwas gebremst.
            set(out, INF, INF, 2.0F);
            return 1.0F;
        }
        float fill = fullShape ? 1.0F : 0.5F;
        SoundType sound = state.getSoundType();
        float resistance = state.getBlock().getExplosionResistance();
        if (sound == SoundType.GLASS || !state.canOcclude() && fullShape) {
            set(out, 8.0F, 6.0F, 0.2F);
        } else if (sound == SoundType.WOOD
                || sound == SoundType.BAMBOO_WOOD
                || sound == SoundType.NETHER_WOOD
                || sound == SoundType.CHERRY_WOOD
                || sound == SoundType.WOOL) {
            // Organisch, wasserstoffhaltig: schwach gegen Gamma, ordentlich gegen Neutronen.
            set(out, 5.0F, 1.5F, 0.1F);
        } else if (sound == SoundType.METAL
                || sound == SoundType.NETHERITE_BLOCK
                || sound == SoundType.ANVIL
                || sound == SoundType.CHAIN) {
            if (resistance >= 100.0F) set(out, 0.4F, 3.0F, 0.05F);
            else set(out, 0.6F, 3.0F, 0.05F);
        } else if (resistance < 1.0F) {
            // Erde, Sand, Kies, Schnee ...
            set(out, 2.5F, 2.0F, 0.1F);
        } else if (resistance < 10.0F) {
            // Stein, Ziegel aus Vanilla, Erz ...
            set(out, 1.5F, 2.0F, 0.1F);
        } else if (resistance < 100.0F) {
            set(out, 1.0F, 1.2F, 0.08F);
        } else {
            set(out, 0.7F, 1.0F, 0.05F);
        }
        return fill;
    }

    private static void set(float[] out, float gamma, float neutron, float beta) {
        out[0] = gamma;
        out[1] = neutron;
        out[2] = beta;
    }

    static boolean isLoaded() {
        return loaded;
    }

    /** Daempfung mu (je Block Weglaenge) fuer die Art. */
    static float mu(RadiationType type, BlockState state) {
        float[] m = mu[type.ordinal()];
        int id = Block.BLOCK_STATE_REGISTRY.getId(state);
        return id >= 0 && id < m.length ? m[id] : 0.0F;
    }

    /** Block-Diffusivitaet (Leitwert) fuer die Art, 1 = Luft. */
    static float diffusivity(int typeOrdinal, BlockState state) {
        float[] d = diff[typeOrdinal];
        int id = Block.BLOCK_STATE_REGISTRY.getId(state);
        return id >= 0 && id < d.length ? d[id] : 1.0F;
    }

    /** Halbwertsschicht in Bloecken (Anzeige/Debug). */
    public static double halfValueLayer(RadiationType type, BlockState state) {
        if (!type.hasField) return 0.0D;
        float m = mu(type, state);
        return m > 0.0F ? Math.log(2.0D) / m : Double.POSITIVE_INFINITY;
    }
}
