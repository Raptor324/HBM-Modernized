package com.hbm_m.handler.pollution;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1:1-Port von {@code com.hbm.handler.pollution.PollutionHandler} (1.7.10): ein Raster aus
 * 64x64-Zellen je Welt, das Russ, Gift, Schwermetall und Fallout haelt, sich ausbreitet, abklingt
 * und ab einer Giftschwelle die Vegetation zerstoert.
 *
 * <p>Die Ablage laeuft ueber {@link PollutionSavedData} statt ueber eine eigene
 * {@code hbmpollution.dat}; die Werte und Formeln sind unveraendert. Der Tick haengt in
 * {@link PollutionEvents} an den Architectury-Serverticks.</p>
 */
public class PollutionHandler {

    /** Original: Russ je Sekunde fuer eine ofengleiche Maschine. */
    public static final float SOOT_PER_SECOND = 1F / 25F;
    /** Original: Schwermetall je Sekunde, geeicht auf die Russwerte von Verbrennungsmotoren. */
    public static final float HEAVY_METAL_PER_SECOND = 1F / 50F;
    /** Original: Gift je Sekunde beim Verschuetten. */
    public static final float POISON_PER_SECOND = 1F / 50F;

    /** Original: {@code MathHelper.clamp_float(..., 0F, 10_000F)}. */
    private static final float MAX_POLLUTION = 10_000F;

    /** Original: die Zelle ist 64 Bloecke breit ({@code x >> 6}), nicht ein Chunk. */
    private static final int CELL_SHIFT = 6;
    private static final int CELL_SIZE = 1 << CELL_SHIFT;

    private PollutionHandler() {}

    // ═══════════════════════════════ Zugriff ═══════════════════════════════

    private static ChunkPos cell(int x, int z) {
        return new ChunkPos(x >> CELL_SHIFT, z >> CELL_SHIFT);
    }

    private static boolean enabled() {
        return ModClothConfig.get().enablePollution;
    }

    /** Liefert die Zelle und legt sie bei Bedarf an. */
    private static PollutionData getOrCreate(ServerLevel level, int x, int z) {
        PollutionSavedData saved = PollutionSavedData.get(level);
        PollutionData data = saved.pollution.get(cell(x, z));

        if (data == null) {
            data = new PollutionData();
            saved.pollution.put(cell(x, z), data);
        }
        saved.setDirty();
        return data;
    }

    public static void incrementPollution(Level level, int x, int y, int z, PollutionType type, float amount) {
        if (!enabled() || !(level instanceof ServerLevel serverLevel)) return;

        PollutionData data = getOrCreate(serverLevel, x, z);
        data.pollution[type.ordinal()] = Mth.clamp(
                (float) (data.pollution[type.ordinal()] + amount * ModClothConfig.get().pollutionMult),
                0F, MAX_POLLUTION);
    }

    public static void incrementPollution(Level level, BlockPos pos, PollutionType type, float amount) {
        incrementPollution(level, pos.getX(), pos.getY(), pos.getZ(), type, amount);
    }

    public static void decrementPollution(Level level, int x, int y, int z, PollutionType type, float amount) {
        incrementPollution(level, x, y, z, type, -amount);
    }

    public static void setPollution(Level level, int x, int y, int z, PollutionType type, float amount) {
        if (!enabled() || !(level instanceof ServerLevel serverLevel)) return;

        getOrCreate(serverLevel, x, z).pollution[type.ordinal()] = amount;
    }

    public static float getPollution(Level level, int x, int y, int z, PollutionType type) {
        PollutionData data = getPollutionData(level, x, y, z);
        return data == null ? 0F : data.pollution[type.ordinal()];
    }

    public static float getPollution(Level level, BlockPos pos, PollutionType type) {
        return getPollution(level, pos.getX(), pos.getY(), pos.getZ(), type);
    }

    /** Kann {@code null} sein, wenn die Zelle noch nie verschmutzt wurde. */
    public static PollutionData getPollutionData(Level level, int x, int y, int z) {
        if (!enabled() || !(level instanceof ServerLevel serverLevel)) return null;

        return PollutionSavedData.get(serverLevel).pollution.get(cell(x, z));
    }

    // ═══════════════════════════ Ausbreitung ═══════════════════════════

    /**
     * Original: der Rumpf von {@code updateSystem}, dort alle 60 Serverticks. Russ und Gift geben
     * einen Teil an die vier Nachbarzellen ab, alles klingt ab; Schwermetall bleibt fast liegen.
     */
    public static void updateSystem(ServerLevel level) {
        if (!enabled()) return;

        PollutionSavedData saved = PollutionSavedData.get(level);
        if (saved.pollution.isEmpty()) return;

        Map<ChunkPos, PollutionData> newPollution = new HashMap<>();

        final int S = PollutionType.SOOT.ordinal();
        final int H = PollutionType.HEAVYMETAL.ordinal();
        final int P = PollutionType.POISON.ordinal();

        for (Map.Entry<ChunkPos, PollutionData> chunk : saved.pollution.entrySet()) {
            ChunkPos pos = chunk.getKey();
            PollutionData data = chunk.getValue();

            float[] forNeighbors = new float[PollutionType.values().length];

            /* BERECHNUNG */
            if (data.pollution[S] > 10) {
                forNeighbors[S] = data.pollution[S] * 0.05F;
                data.pollution[S] *= 0.8F;
            }

            data.pollution[S] *= 0.99F;
            data.pollution[H] *= 0.9995F;

            if (data.pollution[P] > 10) {
                forNeighbors[P] = data.pollution[P] * 0.025F;
                data.pollution[P] *= 0.9F;
            } else {
                data.pollution[P] *= 0.995F;
            }

            /* AUSBREITUNG */
            addInto(newPollution, pos, data.pollution);

            for (int[] offset : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                addInto(newPollution, new ChunkPos(pos.x + offset[0], pos.z + offset[1]), forNeighbors);
            }
        }

        saved.pollution.clear();
        saved.pollution.putAll(newPollution);
        saved.setDirty();
    }

    /** Original: die Zelle wird nur uebernommen, wenn irgendein Wert groesser null ist. */
    private static void addInto(Map<ChunkPos, PollutionData> target, ChunkPos pos, float[] amounts) {
        PollutionData data = target.get(pos);
        if (data == null) data = new PollutionData();

        boolean shouldPut = false;
        for (int i = 0; i < data.pollution.length; i++) {
            data.pollution[i] += amounts[i];
            if (data.pollution[i] > 0) shouldPut = true;
        }

        if (shouldPut) target.put(pos, data);
    }

    // ═══════════════════════ Zersetzung der Welt ═══════════════════════

    protected static final float DESTRUCTION_THRESHOLD = 15F;
    protected static final int DESTRUCTION_COUNT = 5;

    /**
     * Original: {@code handleWorldDestruction} - ab 15 Gift wird pro Servertick fuenfmal
     * ausgewuerfelt; Gras wird zu grober Erde, Pflanzen und Laub verschwinden.
     */
    public static void handleWorldDestruction(ServerLevel level) {
        if (!enabled()) return;

        PollutionSavedData saved = PollutionSavedData.get(level);
        if (saved.pollution.isEmpty()) return;

        for (Map.Entry<ChunkPos, PollutionData> entry : saved.pollution.entrySet()) {
            if (entry.getValue().pollution[PollutionType.POISON.ordinal()] < DESTRUCTION_THRESHOLD) continue;

            ChunkPos cell = entry.getKey();

            for (int i = 0; i < DESTRUCTION_COUNT; i++) {
                int x = (cell.x << CELL_SHIFT) + level.random.nextInt(CELL_SIZE);
                int z = (cell.z << CELL_SHIFT) + level.random.nextInt(CELL_SIZE);

                // Original: provider.chunkExists(...) - geladene Chunks nicht nachladen.
                if (!level.hasChunk(x >> 4, z >> 4)) continue;

                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - level.random.nextInt(3) + 1;
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);

                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                    // Original: Erde mit Metadaten 1, also grobe Erde.
                    level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                } else if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof BushBlock) {
                    // Original: Material.leaves und Material.plants - im Port ueber Tag und Klasse.
                    level.removeBlock(pos, false);
                }
            }
        }
    }
}
