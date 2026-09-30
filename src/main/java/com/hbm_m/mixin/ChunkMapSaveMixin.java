package com.hbm_m.mixin;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.ChunkSaveParallelizer;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Параллельная сериализация чанков при массовых операциях (кратер MK5/фоллаут) —
 * см. {@link ChunkSaveParallelizer}. Перехватывает {@code ChunkMap.save}.
 *
 * ПОТОКОБЕЗОПАСНОСТЬ (аудит по ванильным сурсам 1.20.1/1.21.1):
 * 1. ChunkSerializer.write читает PalettedContainer секций, heightmaps,
 *    block entities и LIGHT ENGINE (DataLayer-массивы). Палитры/BE/heightmaps
 *    мутируют только тикающие чанки (random ticks, scheduled ticks, BE ticker).
 * 2. ГЛАВНЫЙ ГВАРД: чанк уходит в параллель ТОЛЬКО при ticketLevel > 31
 *    (не FULL) — такой чанк гарантированно не тикает и не мутируется геймплеем.
 *    Чанки рядом с игроком (level <= 31) и весь живой автосейв идут ванильным
 *    синхронным путём. Выгружаемые из кратера чанки после снятия blast-тикетов
 *    как раз имеют высокий level — им параллелизм и нужен.
 * 3. Light engine: DataLayer может подменяться световым потоком; чтение атомарно
 *    по ссылке на массив → worst case чуть устаревший свет в сохранении (не
 *    краш, свет дозаполняется апдейтами). У выгружаемого чанка световых задач
 *    почти нет.
 * 4. Двойная сериализация невозможна: после постановки в очередь
 *    setUnsaved(false) (как в ванилле до записи) → повторный save() выходит
 *    по !isUnsaved().
 * 5. ChunkDataEvent.Save постится на СЕРВЕРНОМ потоке (слушатели модов ожидают
 *    его там); запись — ванильный асинхронный write(pos, tag). При шатдауне
 *    влетевшая задача добивается inline на воркере, чтобы тег не потерялся.
 */
@Mixin(net.minecraft.server.level.ChunkMap.class)
public abstract class ChunkMapSaveMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Final
    private PoiManager poiManager;

    @Shadow
    @Final
    private Long2ByteMap chunkTypeCache;

    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    // write(ChunkPos, CompoundTag) — public, УНАСЛЕДОВАН обоими версиями от
    // ChunkStorage (1.20.1: void, 1.21.1: CompletableFuture<Void>); AP миксинов
    // и рантайм-инвокер по иерархии не ходят → зовём обычным кастом к
    // суперклассу в hbm$writeChunk.

    @Inject(
            method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$parallelSave(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (!ChunkSaveParallelizer.isMassOperation()) return;
        // Proto/Imposter-чанки: ванильный путь (там синхронное isExistingChunkFull-чтение)
        if (!(chunk instanceof LevelChunk)) return;

        ChunkPos pos = chunk.getPos();
        ChunkHolder holder = this.visibleChunkMap.get(pos.toLong());
        // ГВАРД ПОТОКОБЕЗОПАСНОСТИ: тикающий чанк сериализуем синхронно, как ванилла.
        if (holder == null || holder.getTicketLevel() <= 31) return;

        this.poiManager.flush(pos);
        if (!chunk.isUnsaved()) {
            cir.setReturnValue(false);
            return;
        }
        chunk.setUnsaved(false);
        // markPosition(LEVELCHUNK): ванилла пишет (byte)(proto ? -1 : 1)
        this.chunkTypeCache.put(pos.toLong(), (byte) 1);

        final LevelChunk levelChunk = (LevelChunk) chunk;
        boolean submitted = ChunkSaveParallelizer.submit(
                this.level,
                chunk,
                // серверный поток: ивент (моды правят тег) + ванильная async-запись;
                // release — слот in-flight освобождается только по доездке до диска
                (tag, release) -> {
                    CompoundTag data = hbm$postSaveEvent(levelChunk, tag);
                    hbm$writeChunk(pos, data, release);
                },
                // сериализация на воркере упала — вернём грязность, ванилла пересейвит
                () -> levelChunk.setUnsaved(true));
        if (!submitted) {
            // Backpressure/шатдаун: сериализуем здесь же синхронно, как ванилла.
            CompoundTag tag = ChunkSerializer.write(this.level, chunk);
            CompoundTag data = hbm$postSaveEvent(levelChunk, tag);
            hbm$writeChunk(pos, data, () -> {});
        }
        cir.setReturnValue(true);
    }

    private void hbm$writeChunk(ChunkPos pos, CompoundTag tag, Runnable release) {
        // write — public унаследованный метод ChunkStorage на обеих версиях:
        // 1.20.1 → void (сигнала завершения нет — release сразу после вызова),
        // 1.21.1 → CompletableFuture<Void> (release после ДОЕЗДКИ записи до диска —
        // это и есть бэкспрешер очереди незаписанных тегов)
        //? if < 1.21.1 {
        ((net.minecraft.world.level.chunk.storage.ChunkStorage) (Object) this).write(pos, tag);
        release.run();
        //?} else {
        /*((net.minecraft.world.level.chunk.storage.ChunkStorage) (Object) this)
                .write(pos, tag)
                .whenComplete((nothing, failure) -> {
                    if (failure != null) {
                        MainRegistry.LOGGER.error("[HBM] parallel chunk write failed at {},{}", pos.x, pos.z, failure);
                    }
                    release.run();
                });
        *///?}
    }

    private static CompoundTag hbm$postSaveEvent(LevelChunk chunk, CompoundTag tag) {
        //? if forge {
        net.minecraftforge.event.level.ChunkDataEvent.Save event =
                new net.minecraftforge.event.level.ChunkDataEvent.Save(chunk, chunk.getLevel(), tag);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return event.getData();
        //?} else {
        /*net.neoforged.neoforge.event.level.ChunkDataEvent.Save event =
                new net.neoforged.neoforge.event.level.ChunkDataEvent.Save(chunk, chunk.getLevel(), tag);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return event.getData();
        *///?}
    }
}
