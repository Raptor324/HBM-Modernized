package com.hbm_m.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.serialization.Codec;
//? if >= 1.21.1 {
/*import com.mojang.serialization.MapCodec;
*///?}

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Фикс соединений блоков, зависящих от соседей (железные решётки, стеклянные
 * пани), после спавна структур.
 *
 * <p>Ванилла расставляет куски джигсога по очереди с флагами 18 (без обновлений
 * соседей), а пересчёт форм запускает только для блоков текущего куска — стыки
 * между кусками и соседи, появляющиеся позже, остаются несоединёнными (в
 * ванильных структурах это скрыто тем, что соединённые состояния уже запечены
 * в NBT). Процессор запоминает позиции соединяемых блоков, а обновление
 * выполняется отложенно на серверном тике, когда все куски уже расставлены.</p>
 */
public class StructureConnectionFixProcessor extends StructureProcessor {

    //? if < 1.21.1 {
    public static final Codec<StructureConnectionFixProcessor> CODEC = Codec.unit(StructureConnectionFixProcessor::new);
    //?} else {
    /*public static final MapCodec<StructureConnectionFixProcessor> CODEC = MapCodec.unit(StructureConnectionFixProcessor::new);
    *///?}

    public static final TagKey<Block> CONNECTABLES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "structure_connectables"));

    /** Отложенные позиции (с счётчиком попыток) по измерению. */
    private static final Map<ResourceKey<Level>, Queue<PendingPos>> PENDING = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 200;

    private record PendingPos(BlockPos pos, int attempts) {}

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldGen.CONNECTION_FIX_PROCESSOR.get();
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level, BlockPos pos1, BlockPos pos2,
            StructureTemplate.StructureBlockInfo info1,
            StructureTemplate.StructureBlockInfo info2,
            StructurePlaceSettings settings) {
        if (!info2.state().is(CONNECTABLES)) {
            return info2;
        }
        ResourceKey<Level> dim = level instanceof ServerLevel sl ? sl.dimension() : Level.OVERWORLD;
        // processBlock вызывается из воркеров генерации чанков, tick — из серверного
        // потока: только конкурентная очередь, никаких ArrayList под общий мутекс
        PENDING.computeIfAbsent(dim, k -> new ConcurrentLinkedQueue<>())
                .add(new PendingPos(info2.pos().immutable(), 0));
        return info2;
    }

    /**
     * Вызывается из серверного тика (MainRegistry): обходит все измерения.
     */
    public static void tickIfReady(net.minecraft.server.MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tick(level);
        }
    }

    /**
     * Вызывается из серверного тика: пересчитывает соединения у отложенных
     * позиций, чьи чанки уже загружены.
     */
    public static void tick(ServerLevel level) {
        Queue<PendingPos> queue = PENDING.get(level.dimension());
        if (queue == null || queue.isEmpty()) return;
        List<PendingPos> remaining = new ArrayList<>();
        // Drain-паттерн: poll до пустой очереди, незагруженные — возвращаем в хвост.
        // Новые записи, добавленные воркерами во время обработки, просто обработаются
        // на следующем тике; clear()/итерация по списку под нагрузкой не используется.
        PendingPos entry;
        while ((entry = queue.poll()) != null) {
            BlockPos pos = entry.pos();
            if (!level.isLoaded(pos)) {
                if (entry.attempts() < MAX_ATTEMPTS) {
                    remaining.add(new PendingPos(pos, entry.attempts() + 1));
                }
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() instanceof CrossCollisionBlock) {
                BlockState fixed = Block.updateFromNeighbourShapes(current, level, pos);
                if (fixed != current) {
                    level.setBlock(pos, fixed, 3);
                }
            }
        }
        queue.addAll(remaining);
    }
}
