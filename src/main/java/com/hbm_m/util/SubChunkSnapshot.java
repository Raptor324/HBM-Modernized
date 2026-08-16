package com.hbm_m.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Порт {@code com.hbm.util.SubChunkSnapshot} из 1.7.10 (автор mlbv).
 * Неизменяемый снимок суб-чанка 16×16×16: палитра BlockState + short-индексы.
 *
 * Снимок создаётся ТОЛЬКО на серверном потоке (чтение секций чанка из других
 * потоков в современной MC недопустимо), после чего свободно читается
 * рабочими потоками движка взрыва.
 */
public final class SubChunkSnapshot {

    /** Суб-чанк, заведомо состоящий из воздуха. */
    public static final SubChunkSnapshot EMPTY = new SubChunkSnapshot(new BlockState[]{Blocks.AIR.defaultBlockState()}, null);

    private final BlockState[] palette;
    private final short[] data;

    private SubChunkSnapshot(BlockState[] p, short[] d) {
        this.palette = p;
        this.data = d;
    }

    /**
     * Создаёт снимок суб-чанка. Вызывать ТОЛЬКО на серверном потоке.
     *
     * @param level           серверный мир
     * @param key             идентификатор суб-чанка
     * @param allowGeneration разрешена ли прогрузка чанков (конфиг enableChunkLoading):
     *                        если false и чанк не загружен, возвращается {@link #EMPTY}
     *                        (лучи проходят сквозь него как сквозь воздух — парити с 1.7.10)
     * @return снимок, {@link #EMPTY} для пустого суб-чанка или <b>null</b>, если чанк ещё
     *         не загружен — вызывающий код должен запросить тикет и повторить позже.
     *         Синхронная загрузка через getChunk(...) НЕ выполняется сознательно:
     *         она блокирует серверный поток на время дисковой загрузки/генерации.
     */
    public static SubChunkSnapshot getSnapshot(ServerLevel level, SubChunkKey key, boolean allowGeneration) {
        int cx = key.getChunkX();
        int cz = key.getChunkZ();
        if (!level.hasChunk(cx, cz)) {
            return allowGeneration ? null : EMPTY;
        }

        LevelChunk chunk = level.getChunk(cx, cz);
        // getSectionIndex принимает мировую Y-координату
        LevelChunkSection[] sections = chunk.getSections();
        int sectionIndex = chunk.getSectionIndex(key.getSubY() << 4);
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            return EMPTY;
        }
        LevelChunkSection section = sections[sectionIndex];
        if (section == null || section.hasOnlyAir()) {
            return EMPTY;
        }

        short[] data = new short[16 * 16 * 16];
        List<BlockState> palette = new ArrayList<>();
        palette.add(Blocks.AIR.defaultBlockState());
        Map<BlockState, Short> idxMap = new HashMap<>();
        idxMap.put(Blocks.AIR.defaultBlockState(), (short) 0);
        boolean allAir = true;

        for (int ly = 0; ly < 16; ly++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    BlockState state = section.getBlockState(lx, ly, lz);
                    int idx;
                    if (state.isAir()) {
                        idx = 0;
                    } else {
                        allAir = false;
                        Short e = idxMap.get(state);
                        if (e == null) {
                            idxMap.put(state, (short) palette.size());
                            palette.add(state);
                            idx = palette.size() - 1;
                        } else {
                            idx = e;
                        }
                    }
                    data[(ly << 8) | (lz << 4) | lx] = (short) idx;
                }
            }
        }

        if (allAir) {
            return EMPTY;
        }
        return new SubChunkSnapshot(palette.toArray(new BlockState[0]), data);
    }

    /**
     * @return состояние блока по локальным координатам внутри суб-чанка (0–15)
     */
    public BlockState getBlockState(int x, int y, int z) {
        if (this == EMPTY || data == null) return Blocks.AIR.defaultBlockState();
        short idx = data[(y << 8) | (z << 4) | x];
        return (idx >= 0 && idx < palette.length) ? palette[idx] : Blocks.AIR.defaultBlockState();
    }
}
