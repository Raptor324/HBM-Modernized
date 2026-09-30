// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

import com.hbm_m.explosion.BlastChunkUtil;
import com.hbm_m.explosion.BulkLightEngine;
import com.hbm_m.platform.LevelHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;

/**
 * Bulk-обновление света для кратера MK5: один {@code addTask(PRE_UPDATE)} на все
 * пустые секции и все изменённые позиции, вместо задачи на каждый вызов
 * {@code updateSectionStatus}/{@code checkBlock}. Перед задачей обновляются
 * per-колоночные источники небесного света (иначе небо не проваливается в кратер).
 * Подклассы движка (Starlight/ScalableLux) уходят в индивидуальный фолбэк.
 */
@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngineMixin extends LevelLightEngine implements BulkLightEngine {

    protected ThreadedLevelLightEngineMixin(LightChunkGetter chunkGetter, boolean blockLight, boolean skyLight) {
        super(chunkGetter, blockLight, skyLight);
    }

    @Shadow
    private void addTask(int x, int z, ThreadedLevelLightEngine.TaskType type, Runnable task) {
        throw new AssertionError();
    }

    @Shadow
    private void addTask(int x, int z, IntSupplier completedLevel, ThreadedLevelLightEngine.TaskType type, Runnable task) {
        throw new AssertionError();
    }

    @Override
    public CompletableFuture<?> hbm$updateLight(LevelChunk chunk, LongList positions, long sectionMask) {
        ThreadedLevelLightEngine engine = (ThreadedLevelLightEngine) (Object) this;
        ServerLevel level = (ServerLevel) chunk.getLevel();

        if (engine.getClass() != ThreadedLevelLightEngine.class) {
            return BlastChunkUtil.updateLightIndividually(level, chunk, positions, sectionMask);
        }

        // Сервер останавливается: light engine больше не разбирает задачи, ожидание
        // навсегда запаркнет shutdown. Свет пересчитается при следующей загрузке мира.
        if (BlastChunkUtil.serverShuttingDown(level)) {
            return CompletableFuture.completedFuture(null);
        }

        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        if (sectionMask == 0 && positions.isEmpty()) {
            //? if forge {
            return CompletableFuture.completedFuture(null);
            //?} else {
            /*return engine.waitForPendingTasks(cx, cz);
             *///?}
        }

        long emptySections = 0;
        LevelChunkSection[] sections = chunk.getSections();
        for (long mask = sectionMask; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            if (index < sections.length && sections[index].hasOnlyAir()) emptySections |= 1L << index;
        }
        long emptyMask = emptySections;
        int minSectionY = LevelHooks.minSectionY(level);

        // источники небесного света обновляются ДО постановки задачи (chunk state,
        // не light engine state) — см. ванильный LevelChunk.setBlockState
        var skyLightSources = chunk.getSkyLightSources();
        for (int i = 0; i < positions.size(); i++) {
            long packed = positions.getLong(i);
            skyLightSources.update(chunk, BlockPos.getX(packed) & 15, BlockPos.getY(packed), BlockPos.getZ(packed) & 15);
        }

        Runnable checks = () -> {
            try {
                for (long mask = sectionMask; mask != 0; mask &= mask - 1) {
                    int index = Long.numberOfTrailingZeros(mask);
                    super.updateSectionStatus(SectionPos.of(cx, minSectionY + index, cz), (emptyMask & (1L << index)) != 0);
                }
                for (int i = 0; i < positions.size(); i++) {
                    super.checkBlock(BlockPos.of(positions.getLong(i)));
                }
            } catch (Throwable t) {
                // Исключение из задачи убивает поток light engine — тогда все будущие
                // waitForPendingTasks (включая ванильный save) паркуются навсегда.
                com.hbm_m.main.MainRegistry.LOGGER.error(
                        "[NUKE MK5] bulk light task failed for chunk {},{}", cx, cz, t);
            }
        };

        //? if forge {
        // 1.20.1: waitForPendingTasks(int,int) нет — future завершаем из самой задачи
        CompletableFuture<Object> done = new CompletableFuture<>();
        Runnable withCompletion = () -> {
            try {
                checks.run();
            } finally {
                done.complete(null);
            }
        };
        if (sectionMask != 0) this.addTask(cx, cz, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, withCompletion);
        else this.addTask(cx, cz, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, withCompletion);
        return done;
        //?} else {
        /*if (sectionMask != 0) this.addTask(cx, cz, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, checks);
        else this.addTask(cx, cz, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, checks);
        return engine.waitForPendingTasks(cx, cz);
         *///?}
    }
}
