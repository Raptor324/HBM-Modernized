package com.hbm_m.entity.effect;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockFallout;
import com.hbm_m.config.FalloutConfigJSON;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityExplosionChunkloading;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Радиоактивный fallout после ядерного взрыва MK5.
 * Chunk-by-chunk stomp: sellafite, подмена руд, fallout-слой, биомы (порт 1.7.10 FalloutRain).
 */
public class EntityFalloutRain extends EntityExplosionChunkloading {

    private static final EntityDataAccessor<Integer> SCALE = SynchedEntityData.defineId(EntityFalloutRain.class, EntityDataSerializers.INT);

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

                while (System.currentTimeMillis() < start + budget) {
                    if (!chunksToProcess.isEmpty()) {
                        long chunkPos = chunksToProcess.remove(chunksToProcess.size() - 1);
                        int chunkPosX = (int) (chunkPos & 4294967295L);
                        int chunkPosZ = (int) (chunkPos >> 32 & 4294967295L);
                        processChunkColumns(chunkPosX, chunkPosZ, false);
                    } else if (!outerChunksToProcess.isEmpty()) {
                        long chunkPos = outerChunksToProcess.remove(outerChunksToProcess.size() - 1);
                        int chunkPosX = (int) (chunkPos & 4294967295L);
                        int chunkPosZ = (int) (chunkPos >> 32 & 4294967295L);
                        processChunkColumns(chunkPosX, chunkPosZ, true);
                    } else {
                        clearChunkTicket();
                        discard();
                        break;
                    }
                }
            }

            tickDelay--;
        }
    }

    private void processChunkColumns(int chunkPosX, int chunkPosZ, boolean outerRing) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        LevelChunk chunk = serverLevel.getChunk(chunkPosX, chunkPosZ);
        boolean biomeModified = false;

        for (int x = chunkPosX << 4; x < (chunkPosX << 4) + 16; x++) {
            for (int z = chunkPosZ << 4; z < (chunkPosZ << 4) + 16; z++) {
                double distance = Math.hypot(x - getX(), z - getZ());
                if (outerRing && distance > getScale()) continue;

                double percent = distance * 100 / getScale();
                stomp(x, z, percent);

                ResourceKey<Biome> biomeKey = getBiomeChange(percent, getScale(),
                        serverLevel.getBiome(new BlockPos(x, (int) getY(), z)).unwrapKey().orElse(null));
                if (biomeKey != null) {
                    Holder<Biome> biomeHolder = biomeCache.get(biomeKey);
                    if (biomeHolder != null) {
                        WorldUtil.setBiomeColumn(serverLevel, x, z, biomeHolder);
                        biomeModified = true;
                    }
                }
            }
        }

        if (biomeModified) {
            WorldUtil.flushChunk(serverLevel, chunk);
        }

        ChunkRadiationManager.getProxy().recalculateChunkRadiation(chunk);
    }

    /**
     * 1:1 порт {@code EntityFalloutRain.getBiomeChange} (1.7.10):
     * <ul>
     *   <li>{@code scale >= 150 && dist < 15} → INNER (эпицентр крупных взрывов)</li>
     *   <li>{@code scale >= 100 && dist < 55} → CRATER (средняя зона, если не INNER)</li>
     *   <li>{@code scale >= 25} → OUTER (внешняя зона, если не INNER/CRATER)</li>
     * </ul>
     * {@code dist} — процент радиуса fallout от эпицентра (0..100).
     */
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

    private void gatherChunks() {
        Set<Long> chunks = new LinkedHashSet<>();
        Set<Long> outerChunks = new LinkedHashSet<>();
        int outerRange = getScale();
        int adjustedMaxAngle = Math.max(18, 20 * outerRange / 32);

        for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
            Vec3 vector = new Vec3(outerRange, 0, 0)
                    .yRot((float) (angle * Math.PI / 180.0 / (adjustedMaxAngle / 360.0)));
            outerChunks.add(ChunkPos.asLong((int) (getX() + vector.x) >> 4, (int) (getZ() + vector.z) >> 4));
        }

        for (int distance = 0; distance <= outerRange; distance += 8) {
            for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
                Vec3 vector = new Vec3(distance, 0, 0)
                        .yRot((float) (angle * Math.PI / 180.0 / (adjustedMaxAngle / 360.0)));
                long chunkCoord = ChunkPos.asLong((int) (getX() + vector.x) >> 4, (int) (getZ() + vector.z) >> 4);
                if (!outerChunks.contains(chunkCoord)) {
                    chunks.add(chunkCoord);
                }
            }
        }

        chunksToProcess.addAll(chunks);
        outerChunksToProcess.addAll(outerChunks);
        Collections.reverse(chunksToProcess);
        Collections.reverse(outerChunksToProcess);
    }

    private void stomp(int x, int z, double dist) {
        Level level = level();
        int depth = 0;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        // Сканируем от поверхности вниз, а не весь столб мира (1.7.10: y=255..0, но в кратере это ~400 блоков/колонну).
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int topY = Math.min(maxY - 1, surfaceY + 8);

        for (int y = topY; y >= minY; y--) {
            if (depth >= 3) return;

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.is(ModBlocks.NUCLEAR_FALLOUT.get())) continue;

            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);

            if (dist < 65 && state.isFlammable(level, pos, Direction.UP)) {
                if (random.nextInt(5) == 0 && level.getBlockState(above).isAir()) {
                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
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

            float hardness = state.getDestroySpeed(level, pos);
            if (y > minY && dist < 65
                    && hardness <= Blocks.STONE_BRICKS.defaultBlockState().getDestroySpeed(level, pos)
                    && hardness >= 0) {
                if (level.getBlockState(pos.below()).isAir()) {
                    for (int i = 0; i <= depth; i++) {
                        BlockPos fallingPos = pos.offset(0, i, 0);
                        BlockState fallingState = level.getBlockState(fallingPos);
                        float h = fallingState.getDestroySpeed(level, fallingPos);
                        if (h <= Blocks.STONE_BRICKS.defaultBlockState().getDestroySpeed(null, null) && h >= 0) {
                            FallingBlockEntity falling = FallingBlockEntity.fall(level, fallingPos, fallingState);
                            if (falling != null) {
                                falling.dropItem = false;
                            }
                        }
                    }
                }
            }

            if (!eval && state.isSolidRender(level, pos)) {
                depth++;
            }
        }

        tryPlaceFalloutLayer(level, x, z, dist);
    }

    /** Слой осадков — после подмены блоков в колонке, чтобы опора не ломалась mid-stomp. */
    private void tryPlaceFalloutLayer(Level level, int x, int z, double dist) {
        int minY = level.getMinBuildHeight();
        int topY = Math.min(level.getMaxBuildHeight() - 1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 8);

        for (int y = topY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(ModBlocks.NUCLEAR_FALLOUT.get())) {
                continue;
            }

            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            if (!aboveState.isAir() && !(aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty())) {
                return;
            }

            if (!BlockFallout.canSurviveOn(level, pos)) {
                return;
            }

            double d = dist / 100;
            double chance = 0.1 - Math.pow(d - 0.7, 2);
            if (chance >= random.nextDouble()) {
                level.setBlock(above, ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState(), 3);
            }
            return;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCALE, 1);
    }

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
