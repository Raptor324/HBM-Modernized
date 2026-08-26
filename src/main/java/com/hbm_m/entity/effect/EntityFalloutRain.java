package com.hbm_m.entity.effect;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockFallout;
import com.hbm_m.config.FalloutConfigJSON;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityExplosionChunkloading;
import com.hbm_m.explosion.NukeMk5ChunkEater;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

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

    private final LongSet issuedTickets = new LongOpenHashSet();
    private boolean firstTick = true;
    private int tickDelay;
    private final Map<ResourceKey<Biome>, Holder<Biome>> biomeCache = new HashMap<>();
    private final LongList chunksToProcess = new LongArrayList();
    private final LongList outerChunksToProcess = new LongArrayList();

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

            long start = System.currentTimeMillis();

            if (firstTick) {
                if (chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) {
                    gatherChunks();
                }

                if (ModClothConfig.get().enableCraterBiomes) {
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
                        if (outer) {
                            outerChunksToProcess.add(0, chunkPos);
                        } else {
                            chunksToProcess.add(0, chunkPos);
                        }
                        deferred++;
                        int remaining = chunksToProcess.size() + outerChunksToProcess.size();
                        if (deferred >= remaining) {
                            break;
                        }
                    }
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
        boolean biomesEnabled = ModClothConfig.get().enableCraterBiomes;

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

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double dx = x - ex;
                double dz = z - ez;
                double distSq = dx * dx + dz * dz;
                if (outerRing && distSq > scaleSq) continue;

                stomp(serverLevel, ed, writer, x, z, Math.sqrt(distSq) * percentPerBlock);
            }
        }

        clearChunkFluidsPostStomp(ed);

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

        void set(int x, int y, int z, BlockState state) {
            if (WorldUtil.setBlockFast(chunk, writePos.set(x, y, z), state)) {
                modified = true;
                sectionIdx = Integer.MIN_VALUE; // секции могли пересоздаться
            }
        }
    }

    private void clearChunkFluidsPostStomp(ChunkEditor ed) {
        double craterRadius = getScale() * 0.4D;
        double craterRadiusSq = craterRadius * craterRadius;
        double ex = getX();
        double ez = getZ();

        LevelChunk chunk = ed.chunk;
        int baseX = chunk.getPos().x << 4;
        int baseZ = chunk.getPos().z << 4;

        BlockState air = Blocks.AIR.defaultBlockState();
        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section == null || section.hasOnlyAir()) continue;

            int sectionMinY = ed.level.getSectionYFromSectionIndex(s) << 4;

            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx;
                for (int lz = 0; lz < 16; lz++) {
                    double dx = wx + 0.5D - ex;
                    double dz = baseZ + lz + 0.5D - ez;
                    if (dx * dx + dz * dz > craterRadiusSq) continue;

                    for (int ly = 0; ly < 16; ly++) {
                        if (!section.getBlockState(lx, ly, lz).getFluidState().isEmpty()) {
                            ed.set(wx, sectionMinY + ly, baseZ + lz, air);
                        }
                    }
                }
            }
        }
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

    private void stomp(ServerLevel level, ChunkEditor ed, FalloutConfigJSON.FalloutEntry.BlockWriter writer,
                       int x, int z, double distPercent) {
        int depth = 0;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int topY = Math.min(maxY - 1, surfaceY + 8);

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState fire = Blocks.FIRE.defaultBlockState();

        for (int y = topY; y >= minY; y--) {
            if (depth >= 3) return;

            BlockState state = ed.getState(x, y, z);

            if (state.isAir() || state.is(ModBlocks.NUCLEAR_FALLOUT.get())) continue;

            if (!state.getFluidState().isEmpty()) {
                ed.set(x, y, z, air);
                continue;
            }

            BlockState aboveState = ed.getState(x, y + 1, z);
            apiPos.set(x, y, z);

            if (distPercent < 65 && state.isFlammable(level, apiPos, Direction.UP)) {
                if (random.nextInt(5) == 0 && aboveState.isAir()) {
                    ed.set(x, y + 1, z, fire);
                }
            }

            boolean eval = false;
            for (FalloutConfigJSON.FalloutEntry entry : FalloutConfigJSON.entries) {
                if (entry.eval(level, apiPos, state, distPercent, writer)) {
                    if (entry.isSolid()) depth++;
                    eval = true;
                    break;
                }
            }

            if (!eval && state.isSolidRender(level, apiPos)) {
                depth++;
            }
        }

        tryPlaceFalloutLayer(ed, x, z, distPercent);
    }

    private void tryPlaceFalloutLayer(ChunkEditor ed, int x, int z, double distPercent) {
        Level level = level();
        int topY = Math.min(level.getMaxBuildHeight() - 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 8);

        BlockState falloutState = ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState();

        for (int y = topY; y >= level.getMinBuildHeight(); y--) {
            BlockState state = ed.getState(x, y, z);
            if (state.isAir() || state.is(ModBlocks.NUCLEAR_FALLOUT.get())) {
                continue;
            }

            BlockState aboveState = ed.getState(x, y + 1, z);
            if (!aboveState.isAir() && !(aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty())) {
                return;
            }

            if (!BlockFallout.canSurviveOn(level, apiPos.set(x, y, z))) {
                return;
            }

            double chance = 0.1 - Math.pow(distPercent / 100.0 - 0.7, 2);
            if (chance >= random.nextDouble()) {
                ed.set(x, y + 1, z, falloutState);
            }
            return;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
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
        this.entityData.define(SCALE, 1);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        builder.define(SCALE, 1);
    
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setScale(tag.getInt("Scale"));
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
