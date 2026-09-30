package com.hbm_m.mixin;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.UnsafeHolder;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Чинит вечное «Saving worlds»/выгрузку при выходе из мира (ванильный баг 1.21.x,
 * проявляющийся после массовых загрузок чанков — например, после MK5-взрыва).
 *
 * {@code ChunkMap.scheduleUnload} перекидывает чанк через {@code unloadQueue}, пока
 * {@code isReadyForSaving()} == false (счётчик generationRefCount чанкового
 * генерационного конвейера никогда не падает до нуля, если генерационная задача
 * умрёт до релиза; либо навсегда висит сцепленный с {@code saveSync} future).
 * На шатдауне пумпа выгрузки идёт с hasMoreTime=true, и один подвисший holder
 * зацикливает её навсегда со 100% CPU — сервер никогда не доходит до сохранения.
 *
 * <p>В живой игре застрявший чанк тоже не выгружается никогда: после MK5-кратера
 * (~4400 чанков) они навсегда остаются в куче — прямой путь к OOM и постоянному
 * GC-давлению. Поэтому после wall-time-гейтов утёкшее состояние форсируется.
 * Гейты БОЛЬШИЕ (180с/180с/300с) и вот почему: refs чанка держат генерационные
 * задачи соседних чанков (клеймы региона), поэтому завершённость собственных
 * future holder'а утечку НЕ доказывает — после MK5-шторма живые задачи держат
 * клеймы минуты (замер 0929: релизы на 97-124-199-й секунде). Чересчур ранний
 * форс-релиз выгружает чанк из-под живой задачи → churn перезагрузок → OOM в
 * light engine (SkyLightSectionStorage). Запоздалые релизы живых задач после
 * форс-релиза проглатываются ({@code "More releases than claims"} — глотается
 * релиз при счётчике <= 0, самобалансится на любом числе клеймов):
 * <ul>
 *   <li>{@code refs > 0} при всех завершённых future — по истечении большого
 *       гейта ссылки принудительно обнуляются (настоящая утечка — задача умерла
 *       до релиза — живёт вечно, живой шторм укладывается в минуты);</li>
 *   <li>{@code refs == 0} и есть pending future — конвейер мёртв (ждать
 *       некому): фьючерсы завершаются {@code UNLOADED_CHUNK}, как это делает
 *       ванильный {@code failAndClearPendingFuture} при демоушене чанка —
 *       повторный запрос перестроит задачу, ожидающие получают честный
 *       «чанк недоступен»;</li>
 *   <li>{@code refs > 0} и pending future дольше 60с — последний рубеж (риск
 *       для реально живой задачи минимален против гарантированного OOM).</li>
 * </ul>
 *
 * <p>Тот же гейт стоит на {@code processUnloads}: holder с утёкшими ссылками
 * ванилла молча пропускает строкой {@code refs != 0 -> continue}, и он вообще
 * не доходит до очереди выгрузки.
 *
 * <p>Вызов {@code isReadyForSaving()} находится ВНУТРИ лямбды thenRunAsync —
 * синтетический {@code lambda$scheduleUnload$12} (проверено javap по dev-jar
 * 1.21.1; нумерация лямбд — от компиляции Mojang, стабильна в рамках 1.21.1).
 * Хендлер принимает только ресивер вызова — позицию чанка берём из
 * {@code holder.getPos()}.
 *
 * <p>На 1.20.1 чанковая система другая ({@code GenerationChunkHolder} не
 * существует) — класс остаётся пустой заглушкой.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    //? if 1.21.1 {

    /*@Unique
    private static final int hbm$MAX_UNLOAD_RETRIES = 512;

    // Живой сервер: время, после которого застрявшее состояние считается утечкой.
    // ВАЖНО: refs чанка держат задачи СОСЕДНИХ чанков (клеймы региона), их future
    // лежат в чужих holder'ах — «все свои future завершены» утечку НЕ доказывает.
    // После MK5-шторма живые задачи держат клеймы по 2-3 минуты (замер 0929:
    // релизы приходили на 97-124-199-й секунде), поэтому гейты должны быть больше
    // самого тяжёлого шторма; форс-релиз живого клейма = выгрузка чанка из-под
    // задачи, churn перезагрузок и OOM в light engine. Подстраиваются через
    // -Dhbm.unloadLeakRefsMs / hbm.unloadDeadPipelineMs / hbm.unloadHardMs.
    @Unique
    private static final long hbm$LEAK_REFS_MS = Integer.getInteger("hbm.unloadLeakRefsMs", 180_000);
    @Unique
    private static final long hbm$LEAK_DEAD_PIPELINE_MS = Integer.getInteger("hbm.unloadDeadPipelineMs", 180_000);
    @Unique
    private static final long hbm$LEAK_HARD_MS = Integer.getInteger("hbm.unloadHardMs", 300_000);

    // Глобальный троттлинг варнов: шторм из тысяч застрявших чанков не зальёт лог.
    @Unique
    private static volatile long hbm$lastWarnAt;

    @Shadow
    @Final
    private net.minecraft.server.level.ServerLevel level;

    @Unique
    private final Long2IntOpenHashMap hbm$unloadRetries = new Long2IntOpenHashMap();

    // Позиция чанка -> ms первого наблюдения «не готов к сейву» (wall-time, TPS-независимо).
    @Unique
    private final Long2LongOpenHashMap hbm$firstNotReadyAt = new Long2LongOpenHashMap();

    // Троттлинг варнов о проглоченных фантомных релизах.
    @Unique
    private static volatile long hbm$lastSwallowWarnAt;

    @Unique
    private static boolean hbm$futuresOffsetResolved;

    @Unique
    private static long hbm$futuresOffset;

    // Лениво: резолвим приватное поле GenerationChunkHolder.futures на первом использовании,
    // а не при загрузке класса-миксина.
    @Unique
    private static long hbm$futuresOffset() {
        if (!hbm$futuresOffsetResolved) {
            hbm$futuresOffset = UnsafeHolder.fieldOffset(
                    net.minecraft.server.level.GenerationChunkHolder.class, "futures");
            hbm$futuresOffsetResolved = true;
        }
        return hbm$futuresOffset;
    }

    @Unique
    private static boolean hbm$forceReleaseEnabled() {
        return !"false".equals(System.getProperty("hbm.unloadForceRelease"));
    }

    @Redirect(
            method = "lambda$scheduleUnload$12",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;isReadyForSaving()Z"))
    private boolean hbm$capUnloadRetries(ChunkHolder holder) {
        if (holder.isReadyForSaving()) {
            hbm$unloadRetries.remove(holder.getPos().toLong());
            hbm$firstNotReadyAt.remove(holder.getPos().toLong());
            return true;
        }

        int retries = hbm$unloadRetries.addTo(holder.getPos().toLong(), 1);
        if (retries < hbm$MAX_UNLOAD_RETRIES) return false;

        MinecraftServer server = this.level.getServer();
        if (server.isRunning()) {
            if (retries % (hbm$MAX_UNLOAD_RETRIES * 4) == 0 && System.currentTimeMillis() - hbm$lastWarnAt > 1000) {
                hbm$lastWarnAt = System.currentTimeMillis();
                MainRegistry.LOGGER.warn(
                        "[HBM] {} chunks stuck in the unload queue (leaked generation ref?) — force-release armed for long waits",
                        hbm$unloadRetries.size());
            }
            if (hbm$unloadRetries.size() > 10_000) hbm$unloadRetries.clear();
            if (hbm$forceReleaseEnabled() && holder instanceof net.minecraft.server.level.GenerationChunkHolder gen) {
                hbm$releaseLeakedState(gen, holder.getPos().toLong(), System.currentTimeMillis());
            }
            return holder.isReadyForSaving();
        }

        if (!(holder instanceof net.minecraft.server.level.GenerationChunkHolder gen)) return false;
        int refs = gen.getGenerationRefCount();
        if (refs <= 0) return false; // saveSync не завершён — не наш случай, ждём честно

        MainRegistry.LOGGER.error(
                "[HBM] chunk {} stuck in the unload queue for {} rounds with {} generation references — force-releasing during shutdown",
                holder.getPos(), retries, refs);
        while (gen.getGenerationRefCount() > 0) {
            gen.decreaseGenerationRefCount();
        }
        hbm$unloadRetries.remove(holder.getPos().toLong());
        return gen.getGenerationRefCount() == 0 && holder.getSaveSyncFuture().isDone();
    }

    // Ванилла молча пропускает holder со ссылками в toDrop — даём ему тот же гейт утечки,
    // иначе такой чанк вообще не доходит до очереди выгрузки.
    @Redirect(
            method = "processUnloads",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkHolder;getGenerationRefCount()I"))
    private int hbm$guardToDropRefCheck(ChunkHolder holder) {
        int refs = holder.getGenerationRefCount();
        if (refs > 0
                && hbm$forceReleaseEnabled()
                && this.level.getServer().isRunning()
                && holder instanceof net.minecraft.server.level.GenerationChunkHolder gen) {
            hbm$releaseLeakedState(gen, holder.getPos().toLong(), System.currentTimeMillis());
            refs = gen.getGenerationRefCount();
        }
        return refs;
    }

    // Разбирает застрявшее состояние holder'а по wall-time гейтам (описание — в шапке
    // класса). Потокобезопасности хватает серверного потока: оба редиректа (unloadQueue
    // и processUnloads) выполняются только на нём.
    @Unique
    private void hbm$releaseLeakedState(
            net.minecraft.server.level.GenerationChunkHolder gen, long posLong, long now) {
        long first = hbm$firstNotReadyAt.get(posLong);
        if (first == 0L) {
            hbm$firstNotReadyAt.put(posLong, now);
            return;
        }
        long waited = now - first;

        java.util.concurrent.atomic.AtomicReferenceArray futures =
                (java.util.concurrent.atomic.AtomicReferenceArray)
                        UnsafeHolder.U.getObject(gen, hbm$futuresOffset());
        if (futures == null) return;

        int pending = 0;
        for (int i = 0; i < futures.length(); i++) {
            Object future = futures.get(i);
            if (future instanceof java.util.concurrent.CompletableFuture cf && !cf.isDone()) pending++;
        }
        int refs = gen.getGenerationRefCount();

        if (pending == 0) {
            if (refs > 0 && waited >= hbm$LEAK_REFS_MS) {
                while (gen.getGenerationRefCount() > 0) gen.decreaseGenerationRefCount();
                if (now - hbm$lastWarnAt > 1000) {
                    hbm$lastWarnAt = now;
                    MainRegistry.LOGGER.warn(
                            "[HBM] chunk {} leaked {} generation refs (all futures done, waited {}ms) — force-releasing, chunk will save & unload",
                            gen.getPos(), refs, waited);
                }
            }
            return;
        }

        boolean deadPipeline = refs == 0 && waited >= hbm$LEAK_DEAD_PIPELINE_MS;
        boolean hardStuck = refs > 0 && waited >= hbm$LEAK_HARD_MS;
        if (!deadPipeline && !hardStuck) return;

        int failed = 0;
        for (int i = 0; i < futures.length(); i++) {
            Object future = futures.get(i);
            if (!(future instanceof java.util.concurrent.CompletableFuture cf) || cf.isDone()) continue;
            // Семантика ванильного failAndClearPendingFuture: UNLOADED_CHUNK ожидающим,
            // слот чистится — повторный запрос построит fresh future
            if (cf.complete(net.minecraft.server.level.GenerationChunkHolder.UNLOADED_CHUNK)) {
                futures.compareAndSet(i, future, null);
                failed++;
            }
        }
        if (refs > 0) {
            while (gen.getGenerationRefCount() > 0) gen.decreaseGenerationRefCount();
        }
        if (now - hbm$lastWarnAt > 1000) {
            hbm$lastWarnAt = now;
            MainRegistry.LOGGER.warn(
                    "[HBM] chunk {} stuck {}ms: completed {} dead futures, force-released {} gen refs ({}), chunk will save & unload",
                    gen.getPos(), waited, failed, refs, hardStuck ? "hard deadline" : "dead pipeline");
        }
    }

    // Фантомный релиз: форс-релиз обнулил счётчик, а живая генерационная задача
    // ещё держит клеймы и позже делает releaseClaim — по одному на каждый клейм
    // своего региона (StaticCache2D.forEach). Одноразового флага недостаточно:
    // первый релиз гасится, второй проваливает счётчик в минус, исключение убивает
    // ChunkGenerationTask — его недорелизенные клеймы на соседних чанках утекают
    // каскадом, и любой синхронный getChunk по такому чанку виснет в managedBlock
    // навсегда. Поэтому глотаем ровно то, что ванилла сама считает ошибкой:
    // релиз при счётчике <= 0 (ваниль бросает "More releases than claims").
    // Живые клеймы (счётчик >= 1) проходят честно, самобалансируется.
    @Inject(
            method = "releaseGeneration(Lnet/minecraft/server/level/GenerationChunkHolder;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$swallowForcedRelease(
            net.minecraft.server.level.GenerationChunkHolder chunk, CallbackInfo ci) {
        if (chunk.getGenerationRefCount() > 0) return;
        long now = System.currentTimeMillis();
        if (now - hbm$lastSwallowWarnAt > 1000) {
            hbm$lastSwallowWarnAt = now;
            MainRegistry.LOGGER.warn(
                    "[HBM] swallowed phantom generation release(s) after force-release on chunk {} (live task claims already dropped)",
                    chunk.getPos());
        }
        ci.cancel();
    }

    *///?}
}
