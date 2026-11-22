// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.LevelHooks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.server.level.ServerLevel;

/**
 * Снапшот секций чанка для off-thread вырезания кратера: рабочие потоки правят
 * копии, серверный поток публикует их только если живые секции не менялись
 * (проверка по инстансу и счётчику поколений из {@link SectionGenerationAccess}).
 */
public final class SectionSnapshot {

    private final LevelChunk chunk;
    private final LevelChunkSection[] original;
    private final long[] generations;
    private final long observed;
    public final LevelChunkSection[] sections;
    private long changed;

    private SectionSnapshot(LevelChunk chunk, long observed) {
        this.chunk = chunk;
        this.observed = observed;
        LevelChunkSection[] live = chunk.getSections();
        this.original = new LevelChunkSection[live.length];
        this.generations = new long[live.length];
        this.sections = new LevelChunkSection[live.length];
        for (long mask = observed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            LevelChunkSection source = live[index];
            original[index] = source;
            generations[index] = ((SectionGenerationAccess) source).hbm$getGeneration();
            sections[index] = copyOf(source);
        }
    }

    static long representable(int sectionCount) {
        if (sectionCount > Long.SIZE) {
            throw new IllegalStateException(
                    "dimension has " + sectionCount
                            + " sections; bulk section edits span " + Long.SIZE);
        }
        return sectionCount == Long.SIZE ? -1L : (1L << sectionCount) - 1;
    }

    public static SectionSnapshot capture(LevelChunk chunk, long observed) {
        return new SectionSnapshot(chunk, observed & representable(chunk.getSections().length));
    }

    private static LevelChunkSection copyOf(LevelChunkSection source) {
        // ВАЖНО: PalettedContainer.copy() на 1.20.1/1.21.1 копирует палитру, НО оставляет
        // в ней resize-listener'ом ОРИГИНАЛЬНЫЙ контейнер. Первая же запись в копию нового
        // для палитры стейта (воздух в однородную секцию) ресайзит ЖИВОЙ чанк и оставляет
        // копию с рассинхроном палитра/storage («value 16 not in 0..15»). Ваниль никогда
        // не пишет в скопированные контейнеры, поэтому у неё этого нет. Собираем копию
        // вручную: публичный ctor пересоздаёт палитру со слушателем-копией.
        PalettedContainer<BlockState> src = source.getStates();
        PalettedContainer.Data<BlockState> data = src.data;
        int bits = data.storage().getBits();
        int capacity = bits >= 30 ? Integer.MAX_VALUE : (1 << bits);
        int palSize = data.palette().getSize();
        if (palSize > capacity) {
            // Исходник рассинхронизирован (записи с id >= 2^bits недостижимы из storage) —
            // обрезаем недостижимый хвост, иначе первая запись в копию упадёт так же.
            MainRegistry.LOGGER.error(
                    "[NUKE MK5 ENGINE] desynced source palette: bits={}, size={} — truncating to capacity", bits, palSize);
            palSize = capacity;
        }
        List<BlockState> entries = new ArrayList<>(palSize);
        for (int i = 0; i < palSize; i++) entries.add(data.palette().valueFor(i));
        PalettedContainer<BlockState> states = new PalettedContainer<>(
                Block.BLOCK_STATE_REGISTRY,
                PalettedContainer.Strategy.SECTION_STATES,
                data.configuration(),
                data.storage().copy(),
                entries);
        return new LevelChunkSection(states, source.getBiomes().recreate());
    }

    public void changed(int index) {
        changed |= 1L << index;
    }

    public LevelChunk chunk() {
        return chunk;
    }

    /** @return false, если живые секции менялись с момента захвата — работу надо пересчитать. */
    public boolean publish() {
        LevelChunkSection[] live = chunk.getSections();
        for (long mask = observed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            if (live[index] != original[index]
                    || ((SectionGenerationAccess) live[index]).hbm$getGeneration() != generations[index]) {
                return false;
            }
        }
        ServerLevel level = (ServerLevel) chunk.getLevel();
        int minSectionY = LevelHooks.minSectionY(level);
        for (long mask = changed; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            // lithium/radium: оповестить AI block-sensing трекеры об инвалидации секции
            LithiumSectionCompat.replaced(
                    level,
                    net.minecraft.core.SectionPos.asLong(chunk.getPos().x, minSectionY + index, chunk.getPos().z),
                    original[index]);
            live[index] = sections[index];
        }
        if (changed != 0) chunk.setUnsaved(true);
        return true;
    }
}
