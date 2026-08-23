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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityFalloutRain extends EntityExplosionChunkloading {

    private static final EntityDataAccessor<Integer> SCALE = SynchedEntityData.defineId(EntityFalloutRain.class, EntityDataSerializers.INT);

    private static final TicketType<ChunkPos> FALLOUT_LOAD =
            TicketType.create("hbm_m_fallout_load", Comparator.comparingLong(p -> (long) p.x << 32 ^ p.z));

    private final Map<ChunkPos, ChunkPos> issuedTickets = new HashMap<>();
    private boolean firstTick = true;
    private int tickDelay;
    private final Map<ResourceKey<Biome>, Holder<Biome>> biomeCache = new HashMap<>();
    private final List<Long> chunksToProcess = new ArrayList<>();
    private final List<Long> outerChunksToProcess = new ArrayList<>();

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
                        chunkPos = chunksToProcess.remove(chunksToProcess.size() - 1);
                        outer = false;
                    } else if (!outerChunksToProcess.isEmpty()) {
                        chunkPos = outerChunksToProcess.remove(outerChunksToProcess.size() - 1);
                        outer = true;
                    } else {
                        clearChunkTicket();
                        discard();
                        break;
                    }
                    int chunkPosX = ChunkPos.getX(chunkPos);
                    int chunkPosZ = ChunkPos.getZ(chunkPos);

                    if (processChunkColumns(chunkPosX, chunkPosZ, outer)) {
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
            ChunkPos cp = new ChunkPos(chunkPosX, chunkPosZ);
            if (issuedTickets.putIfAbsent(cp, cp) == null) {
                serverLevel.getChunkSource().addRegionTicket(FALLOUT_LOAD, cp, 2, cp);
            }
            return false;
        }
        releaseTicket(new ChunkPos(chunkPosX, chunkPosZ));

        boolean biomeModified = false;
        int minX = chunkPosX << 4;
        int minZ = chunkPosZ << 4;

        // 1. Быстрая замена биомов по сетке 4x4 (кварты ваниллы 1.21)
        if (ModClothConfig.get().enableCraterBiomes) {
            for (int bx = 0; bx < 16; bx += 4) {
                for (int bz = 0; bz < 16; bz += 4) {
                    int worldX = minX + bx + 2;
                    int worldZ = minZ + bz + 2;
                    double distance = Math.hypot(worldX - getX(), worldZ - getZ());
                    if (outerRing && distance > getScale()) continue;

                    double percent = distance * 100.0 / getScale();
                    ResourceKey<Biome> biomeKey = getBiomeChange(percent, getScale(),
                            serverLevel.getBiome(new BlockPos(worldX, (int) getY(), worldZ)).unwrapKey().orElse(null));

                    if (biomeKey != null) {
                        Holder<Biome> biomeHolder = biomeCache.get(biomeKey);
                        if (biomeHolder != null) {
                            // Задаем биом сразу для всей кварты 4x4
                            for (int qx = 0; qx < 4; qx++) {
                                for (int qz = 0; qz < 4; qz++) {
                                    WorldUtil.setBiomeColumn(serverLevel, minX + bx + qx, minZ + bz + qz, biomeHolder);
                                }
                            }
                            biomeModified = true;
                        }
                    }
                }
            }
        }

        // 2. Радиационная трансформация поверхности
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double distance = Math.hypot(x - getX(), z - getZ());
                if (outerRing && distance > getScale()) continue;

                double percent = distance * 100.0 / getScale();
                stomp(x, z, percent);
            }
        }

        clearChunkFluidsPostStomp(serverLevel, chunk);

        if (biomeModified) {
            WorldUtil.flushChunk(serverLevel, chunk);
        }

        ChunkRadiationManager.getProxy().recalculateChunkRadiation(chunk);
        return true;
    }

    private void releaseTicket(ChunkPos cp) {
        if (issuedTickets.remove(cp) != null && level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().removeRegionTicket(FALLOUT_LOAD, cp, 2, cp);
        }
    }

    private void clearChunkFluidsPostStomp(ServerLevel level, LevelChunk chunk) {
        int craterRadius = (int) (getScale() * 0.4D);
        int craterRadiusSq = craterRadius * craterRadius;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int chunkPosX = chunk.getPos().x;
        int chunkPosZ = chunk.getPos().z;

        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section == null || section.hasOnlyAir()) continue;

            int sectionMinY = level.getSectionYFromSectionIndex(s) << 4;
            int sectionMaxY = sectionMinY + 15;

            for (int x = chunkPosX << 4; x < (chunkPosX << 4) + 16; x++) {
                for (int z = chunkPosZ << 4; z < (chunkPosZ << 4) + 16; z++) {
                    double dx = x + 0.5D - getX();
                    double dz = z + 0.5D - getZ();
                    if (dx * dx + dz * dz > craterRadiusSq) continue;

                    for (int y = sectionMinY; y <= sectionMaxY; y++) {
                        mutable.set(x, y, z);
                        if (!section.getBlockState(x & 15, y & 15, z & 15).getFluidState().isEmpty()) {
                            level.setBlock(mutable, Blocks.AIR.defaultBlockState(), NukeMk5ChunkEater.FAST_BLOCK_FLAGS);
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
                        outerChunksToProcess.add(packed);
                    } else {
                        chunksToProcess.add(packed);
                    }
                }
            }
        }

        // Обработка идет от центра к краям
        Comparator<Long> distComp = (a, b) -> {
            int ax = ChunkPos.getX(a);
            int az = ChunkPos.getZ(a);
            int bx = ChunkPos.getX(b);
            int bz = ChunkPos.getZ(b);
            int d1 = (ax - centerChunkX) * (ax - centerChunkX) + (az - centerChunkZ) * (az - centerChunkZ);
            int d2 = (bx - centerChunkX) * (bx - centerChunkX) + (bz - centerChunkZ) * (bz - centerChunkZ);
            return Integer.compare(d2, d1); // Реверс для быстрого pop с конца List
        };

        chunksToProcess.sort(distComp);
        outerChunksToProcess.sort(distComp);
    }

    private void stomp(int x, int z, double dist) {
        Level level = level();
        int depth = 0;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int topY = Math.min(maxY - 1, surfaceY + 8);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

        for (int y = topY; y >= minY; y--) {
            if (depth >= 3) return;

            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.is(ModBlocks.NUCLEAR_FALLOUT.get())) continue;

            if (!state.getFluidState().isEmpty()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), NukeMk5ChunkEater.FAST_BLOCK_FLAGS);
                continue;
            }

            above.set(x, y + 1, z);
            BlockState aboveState = level.getBlockState(above);

            if (dist < 65 && state.isFlammable(level, pos, Direction.UP)) {
                if (random.nextInt(5) == 0 && aboveState.isAir()) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), NukeMk5ChunkEater.FAST_BLOCK_FLAGS);
                }
            }

            boolean eval = false;
            for (FalloutConfigJSON.FalloutEntry entry : FalloutConfigJSON.entries) {
                if (entry.eval(level, pos, state, dist)) {
                    if (entry.isSolid()) depth++;
                    eval = true;
                    break;
                }
            }

            if (!eval && state.isSolidRender(level, pos)) {
                depth++;
            }
        }

        tryPlaceFalloutLayer(level, x, z, dist);
    }

    private void tryPlaceFalloutLayer(Level level, int x, int z, double dist) {
        int topY = Math.min(level.getMaxBuildHeight() - 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 8);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();

        for (int y = topY; y >= level.getMinBuildHeight(); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(ModBlocks.NUCLEAR_FALLOUT.get())) {
                continue;
            }

            above.set(x, y + 1, z);
            BlockState aboveState = level.getBlockState(above);
            if (!aboveState.isAir() && !(aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty())) {
                return;
            }

            if (!BlockFallout.canSurviveOn(level, pos)) {
                return;
            }

            double d = dist / 100.0;
            double chance = 0.1 - Math.pow(d - 0.7, 2);
            if (chance >= random.nextDouble()) {
                level.setBlock(above, ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState(), NukeMk5ChunkEater.FAST_BLOCK_FLAGS);
            }
            return;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        if (!issuedTickets.isEmpty() && level() instanceof ServerLevel serverLevel) {
            for (ChunkPos cp : issuedTickets.keySet()) {
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
        chunksToProcess.addAll(readChunksFromIntArray(tag.getIntArray("Chunks")));
        outerChunksToProcess.addAll(readChunksFromIntArray(tag.getIntArray("OuterChunks")));
    }

    private Collection<Long> readChunksFromIntArray(int[] data) {
        List<Long> coords = new ArrayList<>();
        for (int i = 0; i + 1 < data.length; i += 2) {
            coords.add(ChunkPos.asLong(data[i], data[i + 1]));
        }
        return coords;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Scale", getScale());
        tag.putIntArray("Chunks", writeChunksToIntArray(chunksToProcess));
        tag.putIntArray("OuterChunks", writeChunksToIntArray(outerChunksToProcess));
    }

    private int[] writeChunksToIntArray(Collection<Long> coords) {
        int[] data = new int[coords.size() * 2];
        int i = 0;
        for (long packed : coords) {
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