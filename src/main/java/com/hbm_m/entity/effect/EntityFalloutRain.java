// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.entity.effect;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockFallout;
import com.hbm_m.config.FalloutConfigJSON;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityExplosionChunkloading;
import com.hbm_m.explosion.BlastChunkUtil;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.util.WorldUtil;
import com.hbm_m.world.biome.ModBiomes;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityFalloutRain extends EntityExplosionChunkloading {

    private static final EntityDataAccessor<Integer> SCALE = SynchedEntityData.defineId(EntityFalloutRain.class, EntityDataSerializers.INT);

    private static final TicketType<ChunkPos> FALLOUT_LOAD =
            TicketType.create("hbm_m_fallout_load", Comparator.comparingLong(p -> (long) p.x << 32 ^ p.z));

    /**
     * Лимит одновременно выданных точечных тикетов догрузки. Без него fallout на больших
     * scale (тысячи/сотни тысяч чанков в очереди) выдавал тикеты на ВСЁ кольцо сразу —
     * чанк-лоадер грузил гигабайты чанков параллельно и сервер захлёбывался GC.
     * 64 в полёте ≈ до ~1500 живых чанков (тикет радиуса 2 подтягивает соседей).
     */
    private static final int MAX_PENDING_LOAD_TICKETS = 64;

    /** Глубина stomp-прохода по колонке (паритет stompColumn из NEXT). */
    private static final int MAX_SOLID_DEPTH = 3;
    /** Мягкие блоки (твёрдость ≤ 6) над воздухом осыпаются падающими сущностями. */
    private static final float HARDNESS_BOUND = 6.0F;

    private final LongSet issuedTickets = new LongOpenHashSet();
    private boolean firstTick = true;
    /** Активен ли наш счётчик массовых операций (защита от двойного release). */
    private boolean hbm$massOpAcquired;
    private int tickDelay;
    private final Map<ResourceKey<Biome>, Holder<Biome>> biomeCache = new HashMap<>();
    private final LongList chunksToProcess = new LongArrayList();
    private final LongList outerChunksToProcess = new LongArrayList();

    /**
     * Переиспользуемые буферы отложенных чанков. Раньше дефер делал add(0, ...) прямо
     * в основную очередь — O(n) сдвиг массива на КАЖДЫЙ отложенный чанк давал O(n^2)
     * на один проход бюджета (на очередях в десятки тысяч чанков это были сотни мс,
     * видимые профилем как self-time tick()). Теперь отложенные копятся в буфер и
     * добавляются в дальний конец очереди одним батчем после выхода из цикла.
     */
    private final LongList deferredInner = new LongArrayList();
    private final LongList deferredOuter = new LongArrayList();

    /** Переиспользуемый буфер кварт 4x4 для смены биомов ([bx * 4 + bz]). */
    @SuppressWarnings("unchecked")
    private final Holder<Biome>[] quartTargets = new Holder[16];

    /** Общая мутируемая позиция для vanilla-API вызовов (getBiome, isFlammable и т.п.). */
    private final BlockPos.MutableBlockPos apiPos = new BlockPos.MutableBlockPos();

    public EntityFalloutRain(EntityType<?> type, Level level) {
        super(type, level);
        this.tickDelay = getFalloutDelay();
    }

    @Override
    protected int getChunkLoadRadius() {
        int scale = getScale();
        return Math.min(12, Math.max(super.getChunkLoadRadius(), (scale + 15) >> 4) + 1);
    }

    private static int getFalloutDelay() {
        try {
            return ModClothConfig.get().falloutDelay;
        } catch (Exception e) {
            return 4;
        }
    }

    private static int getMk5BudgetMs() {
        try {
            return ModClothConfig.get().mk5TickTimeMs;
        } catch (Exception e) {
            return 10;
        }
    }

    private Holder<Biome> getCachedHolder(ResourceKey<Biome> key) {
        return biomeCache.computeIfAbsent(key, k -> this.level().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(k));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateChunkTicket();

            if (firstTick) {
                // Массовая операция: параллельный сейв чанков (ChunkMapSaveMixin)
                hbm$massOpAcquired = true;
                com.hbm_m.util.ChunkSaveParallelizer.acquireMassOp();
            }

            long start = System.currentTimeMillis();

            if (firstTick) {
                if (chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) {
                    gatherChunks();
                }

                if (craterBiomesAllowed()) {
                    biomeCache.put(ModBiomes.INNER_CRATER_KEY, getCachedHolder(ModBiomes.INNER_CRATER_KEY));
                    biomeCache.put(ModBiomes.CRATER_KEY, getCachedHolder(ModBiomes.CRATER_KEY));
                    biomeCache.put(ModBiomes.OUTER_CRATER_KEY, getCachedHolder(ModBiomes.OUTER_CRATER_KEY));
                }

                firstTick = false;
            }

            if (tickDelay == 0) {
                tickDelay = getFalloutDelay();
                int budget = getMk5BudgetMs();
                int deferred = 0;

                deferredInner.clear();
                deferredOuter.clear();

                while (System.currentTimeMillis() < start + budget) {
                    boolean outer;
                    long chunkPos;
                    if (!chunksToProcess.isEmpty()) {
                        chunkPos = chunksToProcess.removeLong(chunksToProcess.size() - 1);
                        outer = false;
                    } else if (!outerChunksToProcess.isEmpty()) {
                        chunkPos = outerChunksToProcess.removeLong(outerChunksToProcess.size() - 1);
                        outer = true;
                    } else {
                        clearChunkTicket();
                        discard();
                        break;
                    }

                    if (processChunkColumns(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos), outer)) {
                        deferred = 0;
                    } else {
                        (outer ? deferredOuter : deferredInner).add(chunkPos);
                        deferred++;
                        int remaining = chunksToProcess.size() + outerChunksToProcess.size();
                        if (deferred >= remaining) {
                            break;
                        }
                    }
                }

                // Отложенные чанки возвращаем в дальний (головной) конец очередей одним
                // батчем — сохраняется исходная семантика "deferred обрабатываются
                // последними", но без квадратичного сдвига массива
                if (!deferredInner.isEmpty()) {
                    chunksToProcess.addAll(0, deferredInner);
                }
                if (!deferredOuter.isEmpty()) {
                    outerChunksToProcess.addAll(0, deferredOuter);
                }
            }

            tickDelay--;
        }
    }

    private boolean processChunkColumns(int chunkPosX, int chunkPosZ, boolean outerRing) {
        if (!(level() instanceof ServerLevel serverLevel)) return true;

        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkPosX, chunkPosZ);
        if (chunk == null) {
            long packed = ChunkPos.asLong(chunkPosX, chunkPosZ);
            if (!issuedTickets.contains(packed)) {
                // Лимит одновременных догрузок: чанк останется в очереди и будет
                // обработан, когда освободится слот — иначе грузим весь кратер разом
                if (issuedTickets.size() >= MAX_PENDING_LOAD_TICKETS) {
                    return false;
                }
                issuedTickets.add(packed);
                ChunkPos cp = new ChunkPos(chunkPosX, chunkPosZ);
                serverLevel.getChunkSource().addRegionTicket(FALLOUT_LOAD, cp, 2, cp);
            }
            return false;
        }

        ChunkEditor ed = new ChunkEditor(serverLevel, chunk);

        boolean modified = false;
        int minX = chunkPosX << 4;
        int minZ = chunkPosZ << 4;
        double ex = getX();
        double ez = getZ();
        double scaleSq = (double) getScale() * getScale();
        double percentPerBlock = 100.0 / getScale();
        boolean biomesEnabled = craterBiomesAllowed();

        // 1. Быстрая замена биомов по сетке 4x4 (кварты ваниллы), один проход по секциям чанка
        if (biomesEnabled) {
            Arrays.fill(quartTargets, null);
            int changed = 0;
            for (int bx = 0; bx < 4; bx++) {
                for (int bz = 0; bz < 4; bz++) {
                    double dx = (minX + bx * 4 + 2) - ex;
                    double dz = (minZ + bz * 4 + 2) - ez;
                    double distSq = dx * dx + dz * dz;
                    if (outerRing && distSq > scaleSq) continue;

                    ResourceKey<Biome> biomeKey = getBiomeChange(Math.sqrt(distSq) * percentPerBlock, getScale(),
                            biomeAt(serverLevel, minX + bx * 4 + 2, minZ + bz * 4 + 2));

                    if (biomeKey != null) {
                        Holder<Biome> biomeHolder = biomeCache.get(biomeKey);
                        if (biomeHolder != null) {
                            quartTargets[bx * 4 + bz] = biomeHolder;
                            changed++;
                        }
                    }
                }
            }
            if (changed > 0) {
                WorldUtil.setBiomeQuarts(chunk, quartTargets);
                modified = true;
            }
        }

        // 2. Радиационная трансформация поверхности
        FalloutConfigJSON.FalloutEntry.BlockWriter writer =
                (lvl, pos, state) -> ed.set(pos.getX(), pos.getY(), pos.getZ(), state);

        LongArrayList spawnFalling = new LongArrayList();
        int chunkTopY = topSolidY(ed);
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double dx = x - ex;
                double dz = z - ez;
                double distSq = dx * dx + dz * dz;
                if (outerRing && distSq > scaleSq) continue;

                stomp(serverLevel, ed, writer, spawnFalling, x, z, Math.sqrt(distSq) * percentPerBlock, chunkTopY);
            }
        }

        // Осыпь краёв — после прохода чанка: fall() сам убирает источник
        // (level.setBlock флагом 3), нельзя звать посреди итерации секций
        if (!spawnFalling.isEmpty()) {
            spawnFallingBlocks(serverLevel, ed, spawnFalling);
        }

        // Батч-свет: одна задача на чанк вместо checkBlock за каждый блок
        // (fire-and-forget: light engine разберёт асинхронно, серверный поток не ждёт)
        if (!ed.lightPositions.isEmpty()) {
            BlastChunkUtil.updateLight(serverLevel, chunk, ed.lightPositions, ed.lightSectionsMask);
            ed.lightPositions.clear();
            ed.lightSectionsMask = 0;
        }

        modified |= ed.modified;

        // Тикет снимаем только после полной обработки чанка — иначе чанк успевает
        // выгрузиться до повторной попытки и внешнее кольцо «пропадает»
        releaseTicket(ChunkPos.asLong(chunkPosX, chunkPosZ));

        if (modified) {
            WorldUtil.flushChunk(serverLevel, chunk);
        }

        ChunkRadiationManager.getProxy().recalculateChunkRadiation(chunk);
        return true;
    }

    private ResourceKey<Biome> biomeAt(ServerLevel level, int x, int z) {
        return level.getBiome(apiPos.set(x, (int) getY(), z)).unwrapKey().orElse(null);
    }

    private void releaseTicket(long packed) {
        if (issuedTickets.remove(packed) && level() instanceof ServerLevel serverLevel) {
            ChunkPos cp = new ChunkPos(packed);
            serverLevel.getChunkSource().removeRegionTicket(FALLOUT_LOAD, cp, 2, cp);
        }
    }

    /**
     * Прямой редактор чанка: чтение/запись через секции с кэшем активной секции.
     * Полностью обходит Level.setBlock (markAndNotifyBlock → lithium hopper check →
     * блокирующая догрузка соседних чанков была главным пожирателем TPS).
     */
    private final class ChunkEditor {
        final ServerLevel level;
        final LevelChunk chunk;
        boolean modified;

        private final BlockPos.MutableBlockPos writePos = new BlockPos.MutableBlockPos();
        private LevelChunkSection section;
        private int sectionIdx = Integer.MIN_VALUE;

        /** Позиции для батч-пересчёта света — сбрасываются в конце прохода чанка. */
        final LongArrayList lightPositions = new LongArrayList();
        long lightSectionsMask;

        ChunkEditor(ServerLevel level, LevelChunk chunk) {
            this.level = level;
            this.chunk = chunk;
        }

        private LevelChunkSection sectionFor(int y) {
            int idx = level.getSectionIndex(y);
            if (idx != sectionIdx) {
                sectionIdx = idx;
                LevelChunkSection[] arr = chunk.getSections();
                section = (idx >= 0 && idx < arr.length) ? arr[idx] : null;
            }
            return section;
        }

        BlockState getState(int x, int y, int z) {
            LevelChunkSection s = sectionFor(y);
            if (s == null || s.hasOnlyAir()) return Blocks.AIR.defaultBlockState();
            return s.getBlockState(x & 15, y & 15, z & 15);
        }

        /** null-секция или hasOnlyAir — колонку на этом 16-блочном отрезке можно пропустить. */
        boolean isSectionEmpty(int y) {
            LevelChunkSection s = sectionFor(y);
            return s == null || s.hasOnlyAir();
        }

        void set(int x, int y, int z, BlockState state) {
            if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) return;
            BlockState old = getState(x, y, z);
            if (old == state) return;

            writePos.set(x, y, z);

            // Блоки с BE (машины, сундуки): полный ванильный путь — BE корректно
            // снимается, содержимое сундуков выпадает. Лут-таблицу НЕРАЗГРАБЛЕННЫХ
            // сундуков затираем: иначе unpackLootTable на удалении порождает лут
            // (карты сокровищ → ExplorationMap → renderBiomePreviewMap) с
            // СИНХРОННОЙ догрузкой чанков на серверном потоке — жёсткие фризы TPS.
            if (old.hasBlockEntity()) {
                net.minecraft.world.level.block.entity.BlockEntity be =
                        chunk.getBlockEntity(writePos, LevelChunk.EntityCreationType.IMMEDIATE);
                if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity loot) {
                    loot.setLootTable(null, 0L);
                }
                if (WorldUtil.setBlockFast(chunk, writePos, state)) {
                    modified = true;
                    LevelChunkSection[] arr = chunk.getSections();
                    if (sectionIdx >= 0 && sectionIdx < arr.length) {
                        section = arr[sectionIdx];
                    } else {
                        section = null;
                        sectionIdx = Integer.MIN_VALUE;
                    }
                }
                return;
            }

            // Блоки без BE: пишем прямо в секцию, минуя onRemove — бамбук/листва/
            // трава уничтожаются, не выпадая предметами (сотни item-сущностей в
            // бамбуковом лесу душили TPS). Heightmap/onPlace — как у вырезки MK5,
            // свет — одним батчем на чанк в конце прохода.
            LevelChunkSection s = sectionFor(y);
            if (s == null) return;
            BlockState prev = s.setBlockState(x & 15, y & 15, z & 15, state);
            if (prev == state || prev == null) return;

            modified = true;
            chunk.setUnsaved(true);
            if (BlastChunkUtil.postCarveBlockUpdate(level, chunk, writePos, prev, state)) {
                lightPositions.add(writePos.asLong());
                lightSectionsMask |= 1L << sectionIdx;
            }
        }
    }

    /** Per-explosion switch: the /hbm_m explosion command's biomes:false was stored on the
     * explosion entity and never read, so the crater biomes appeared regardless. */
    public boolean applyCraterBiomes = true;

    private boolean craterBiomesAllowed() {
        return applyCraterBiomes && ModClothConfig.get().enableCraterBiomes;
    }

    public static ResourceKey<Biome> getBiomeChange(double dist, int scale, ResourceKey<Biome> original) {
        if (!ModClothConfig.get().enableCraterBiomes || original == null) return null;

        if (scale >= 150 && dist < 15) {
            return ModBiomes.INNER_CRATER_KEY;
        }
        if (scale >= 100 && dist < 55 && original != ModBiomes.INNER_CRATER_KEY) {
            return ModBiomes.CRATER_KEY;
        }
        if (scale >= 25
                && original != ModBiomes.INNER_CRATER_KEY
                && original != ModBiomes.CRATER_KEY) {
            return ModBiomes.OUTER_CRATER_KEY;
        }
        return null;
    }

    /**
     * Сверхточный декартов сбор чанков без слепых зон и пропусков.
     */
    private void gatherChunks() {
        chunksToProcess.clear();
        outerChunksToProcess.clear();

        int centerChunkX = (int) getX() >> 4;
        int centerChunkZ = (int) getZ() >> 4;
        int chunkRadius = (getScale() + 15) >> 4;
        double scaleSq = (double) getScale() * getScale();
        double innerCutoffSq = Math.pow(getScale() * 0.85D, 2);

        List<Long> innerTmp = new ArrayList<>();
        List<Long> outerTmp = new ArrayList<>();

        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                int chunkX = centerChunkX + cx;
                int chunkZ = centerChunkZ + cz;

                // Ближайшая точка границы чанка к эпицентру взрыва
                int nearestX = Math.max(chunkX << 4, Math.min((int) getX(), (chunkX << 4) + 15));
                int nearestZ = Math.max(chunkZ << 4, Math.min((int) getZ(), (chunkZ << 4) + 15));

                double dx = nearestX - getX();
                double dz = nearestZ - getZ();
                double distSq = dx * dx + dz * dz;

                if (distSq <= scaleSq) {
                    long packed = ChunkPos.asLong(chunkX, chunkZ);
                    if (distSq >= innerCutoffSq) {
                        outerTmp.add(packed);
                    } else {
                        innerTmp.add(packed);
                    }
                }
            }
        }

        // Обработка идет от центра к краям (реверс для быстрого pop с конца списка)
        Comparator<Long> distComp = (a, b) -> {
            int ax = ChunkPos.getX(a);
            int az = ChunkPos.getZ(a);
            int bx = ChunkPos.getX(b);
            int bz = ChunkPos.getZ(b);
            int d1 = (ax - centerChunkX) * (ax - centerChunkX) + (az - centerChunkZ) * (az - centerChunkZ);
            int d2 = (bx - centerChunkX) * (bx - centerChunkX) + (bz - centerChunkZ) * (bz - centerChunkZ);
            return Integer.compare(d2, d1);
        };

        innerTmp.sort(distComp);
        outerTmp.sort(distComp);

        chunksToProcess.addAll(innerTmp);
        outerChunksToProcess.addAll(outerTmp);
    }

    /**
     * Порт stompColumn из NEXT: сверху вниз по колонке, не глубже {@value #MAX_SOLID_DEPTH}
     * твёрдых блоков. За один проход: слой осадков над первым твёрдым, огонь под
     * горючим (<65%), трансмутации таблицей fallout, вулканическое ядро →
     * радиоактивное, и сбор мягких блоков над воздухом в осыпь (rim collapse —
     * края кратера осыпаются падающими блоками, как в оригинале).
     * Жидкости НЕ вычищаются: вода вне кратера остаётся, в кратер натекает
     * сама (в NEXT осадки и поджиг на жидкость не кладутся).
     */
    private void stomp(ServerLevel level, ChunkEditor ed, FalloutConfigJSON.FalloutEntry.BlockWriter writer,
                       LongArrayList spawnFalling, int x, int z, double distPercent, int topY) {
        int depth = 0;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        if (topY < minY) return;

        BlockState fire = Blocks.FIRE.defaultBlockState();
        Block fallout = ModBlocks.NUCLEAR_FALLOUT.get();

        for (int y = topY; y >= minY; y--) {
            if (depth >= MAX_SOLID_DEPTH) break;

            // Скип целиком воздушных секций: после кратера под поверхностью десятки
            // секций воздуха, поблочный провал до minY был заметной частью тика.
            if (ed.isSectionEmpty(y)) {
                y &= ~15; // прыжок к нижней границе секции (декремент уводит в следующую)
                continue;
            }

            BlockState state = ed.getState(x, y, z);

            if (state.isAir() || state.is(fallout)) continue;

            BlockState aboveState = null;
            int upY = y + 1;
            if (depth == 0 && upY <= maxY) {
                aboveState = ed.getState(x, upY, z);
                boolean replaceable = aboveState.isAir()
                        || (aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty());
                if (replaceable) {
                    double d = distPercent / 100.0D;
                    double chance = 0.1D - Math.pow(d - 0.7D, 2);
                    if (chance >= random.nextDouble()
                            && BlockFallout.canSurviveOn(level, apiPos.set(x, y, z))) {
                        ed.set(x, upY, z, fallout.defaultBlockState());
                    }
                }
            }

            if (distPercent < 65 && upY <= maxY && state.isFlammable(level, apiPos.set(x, y, z), Direction.UP)) {
                if (aboveState == null) aboveState = ed.getState(x, upY, z);
                if (aboveState.isAir() && random.nextInt(5) == 0) {
                    ed.set(x, upY, z, fire);
                }
            }

            boolean transformed = false;
            for (FalloutConfigJSON.FalloutEntry entry : FalloutConfigJSON.entries) {
                if (entry.eval(level, apiPos, state, distPercent, writer)) {
                    if (entry.isSolid()) depth++;
                    transformed = true;
                    break;
                }
            }

            // Мягкий блок (твёрдость ≤ 6) над воздухом — колонка осыпается падающими
            // блоками; собираем позиции, сущности спавним после прохода чанка
            if (y > minY && distPercent < 65) {
                apiPos.set(x, y, z);
                float hardness = state.getDestroySpeed(level, apiPos);
                if (hardness >= 0F && hardness <= HARDNESS_BOUND && ed.getState(x, y - 1, z).isAir()) {
                    for (int i = 0; i <= depth; i++) {
                        int yy = y + i;
                        if (yy > maxY) break;
                        BlockState colState = ed.getState(x, yy, z);
                        if (colState.isAir()) continue;
                        float h = colState.getDestroySpeed(level, apiPos.set(x, yy, z));
                        if (h >= 0F && h <= HARDNESS_BOUND) {
                            spawnFalling.add(BlockPos.asLong(x, yy, z));
                        }
                    }
                }
            }

            if (!transformed && state.canOcclude()) depth++;
        }
    }

    /** Верхний непустой Y чанка (верх верхней непустой секции) — выше могут быть деревья. */
    private int topSolidY(ChunkEditor ed) {
        LevelChunkSection[] sections = ed.chunk.getSections();
        for (int i = sections.length - 1; i >= 0; i--) {
            if (!sections[i].hasOnlyAir()) {
                int top = (ed.level.getSectionYFromSectionIndex(i) << 4) + 15;
                return Math.min(ed.level.getMaxBuildHeight() - 1, top);
            }
        }
        return -1;
    }

    /**
     * Спавн собранной осыпи: ванильный falling entity без дропа предметов.
     * НЕ зовём FallingBlockEntity.fall(): внутри он гасит исходную позицию через
     * Level.setBlock, а Sable заворачивает LevelChunk.setBlockState и на каждое
     * такое изменение сканирует окрестность с синхронной догрузкой соседних
     * чанков на серверном потоке (замер 0929: ~34% тика фоллаута). Исходную
     * позицию гасим через ChunkEditor — тот же быстрый путь, что и вся вырезка.
     */
    private void spawnFallingBlocks(ServerLevel level, ChunkEditor ed, LongArrayList positions) {
        for (int i = 0, n = positions.size(); i < n; i++) {
            long lp = positions.getLong(i);
            BlockPos pos = BlockPos.of(lp);
            BlockState state = ed.getState(pos.getX(), pos.getY(), pos.getZ());
            if (state.isAir()) continue;
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, Boolean.FALSE);
            }
            net.minecraft.world.entity.item.FallingBlockEntity entity =
                    com.hbm_m.platform.PlatformHooks.newFallingBlock(
                            level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state);
            entity.dropItem = false;
            ed.set(pos.getX(), pos.getY(), pos.getZ(), state.getFluidState().createLegacyBlock());
            level.addFreshEntity(entity);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        if (hbm$massOpAcquired) {
            hbm$massOpAcquired = false;
            com.hbm_m.util.ChunkSaveParallelizer.releaseMassOp();
        }
        if (!issuedTickets.isEmpty() && level() instanceof ServerLevel serverLevel) {
            for (long packed : issuedTickets) {
                ChunkPos cp = new ChunkPos(packed);
                serverLevel.getChunkSource().removeRegionTicket(FALLOUT_LOAD, cp, 2, cp);
            }
            issuedTickets.clear();
        }
        super.remove(reason);
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {

var defs = com.hbm_m.platform.EntityDataHooks.sink(this.entityData);
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

var defs = com.hbm_m.platform.EntityDataHooks.sink(builder);
    *///?}

        defs.define(SCALE, 1);
    
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setScale(tag.getInt("Scale"));
        applyCraterBiomes = !tag.contains("ApplyCraterBiomes") || tag.getBoolean("ApplyCraterBiomes");
        readChunksFromIntArray(chunksToProcess, tag.getIntArray("Chunks"));
        readChunksFromIntArray(outerChunksToProcess, tag.getIntArray("OuterChunks"));
    }

    private static void readChunksFromIntArray(LongList list, int[] data) {
        for (int i = 0; i + 1 < data.length; i += 2) {
            list.add(ChunkPos.asLong(data[i], data[i + 1]));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Scale", getScale());
        tag.putBoolean("ApplyCraterBiomes", applyCraterBiomes);
        tag.putIntArray("Chunks", writeChunksToIntArray(chunksToProcess));
        tag.putIntArray("OuterChunks", writeChunksToIntArray(outerChunksToProcess));
    }

    private static int[] writeChunksToIntArray(LongList coords) {
        int[] data = new int[coords.size() * 2];
        int i = 0;
        for (int j = 0; j < coords.size(); j++) {
            long packed = coords.getLong(j);
            data[i++] = ChunkPos.getX(packed);
            data[i++] = ChunkPos.getZ(packed);
        }
        return data;
    }

    private static final double RENDER_DISTANCE_SQ = 100.0 * 100.0;

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) {
        return distanceSq < RENDER_DISTANCE_SQ;
    }

    public void setScale(int scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getScale() {
        int scale = this.entityData.get(SCALE);
        return scale == 0 ? 1 : scale;
    }
}
