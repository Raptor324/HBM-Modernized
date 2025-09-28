package com.hbm_m.radiation;

// TODO: Эта система пока что ВООБЩЕ не работает. Будет исправлено в будущем, пока что просто как заглушка

import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkRadiationHandlerPRISM extends ChunkRadiationHandler {

    public ConcurrentHashMap<Level, RadPerWorld> perWorld = new ConcurrentHashMap<>();
    public static int cycles = 0;

    public static final float MAX_RADIATION = ModClothConfig.get().maxRad;
    private static final String NBT_KEY_CHUNK_RADIATION = "hfr_prism_radiation_";
    private static final String NBT_KEY_CHUNK_RESISTANCE = "hfr_prism_resistance_";
    private static final String NBT_KEY_CHUNK_EXISTS = "hfr_prism_exists_";

    @Override
    public float getRadiation(Level level, int x, int y, int z) {
        RadPerWorld system = perWorld.get(level);

        if (system != null) {
            ChunkPos coords = new ChunkPos(x >> 4, z >> 4);
            int yReg = Mth.clamp(y >> 4, 0, 15);
            SubChunk[] subChunks = system.radiation.get(coords);
            if (subChunks != null) {
                SubChunk rad = subChunks[yReg];
                if (rad != null) return rad.radiation;
            }
        }
        return 0;
    }

    @Override
    public void recalculateChunkRadiation(LevelChunk chunk) {
    }

    @Override
    public void setRadiation(Level level, int x, int y, int z, float rad) {
        if (Float.isNaN(rad)) rad = 0;

        RadPerWorld system = perWorld.get(level);

        if (system != null) {
            ChunkPos coords = new ChunkPos(x >> 4, z >> 4);
            int yReg = Mth.clamp(y >> 4, 0, 15);
            SubChunk[] subChunks = system.radiation.get(coords);
            if (subChunks == null) {
                subChunks = new SubChunk[16];
                system.radiation.put(coords, subChunks);
            }
            if (subChunks[yReg] == null) subChunks[yReg] = new SubChunk().rebuild(level, x, y, z);
            subChunks[yReg].radiation = Mth.clamp(rad, 0, MAX_RADIATION);
            
            if (level.hasChunk(coords.x, coords.z)) {
                LevelChunk chunk = level.getChunk(coords.x, coords.z);
                SubChunk finalSubChunk = subChunks[yReg]; // Create a final variable
                chunk.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY).ifPresent(cap -> {
                    cap.setBlockRadiation(finalSubChunk.radiation);
                    chunk.setUnsaved(true);
                });
            }
        }
    }

    @Override
    public void incrementRad(Level level, int x, int y, int z, float rad) {
        setRadiation(level, x, y, z, getRadiation(level, x, y, z) + rad);
    }

    @Override
    public void decrementRad(Level level, int x, int y, int z, float rad) {
        setRadiation(level, x, y, z, Math.max(getRadiation(level, x, y, z) - rad, 0));
    }

    @Override
    public void incrementBlockRadiation(Level level, BlockPos pos, float diff) {
        // TODO: Реализовать логику для инкрементального обновления в системе PRISM,
        // когда она будет полностью введена в эксплуатацию.
        // Пока что это пустая реализация для компиляции.
    }

    @Override
    public void receiveWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            perWorld.put(level, new RadPerWorld());
        }
    }

    @Override
    public void receiveWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            perWorld.remove(level);
        }
    }

    @Override
    public void receiveChunkLoad(ChunkDataEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            MainRegistry.LOGGER.debug("Loading chunk data for chunk: {}", event.getChunk().getPos());
            RadPerWorld radWorld = perWorld.get(level);

            if (radWorld != null) {
                SubChunk[] chunk = new SubChunk[16];

                for (int i = 0; i < 16; i++) {
                    if (!event.getData().getBoolean(NBT_KEY_CHUNK_EXISTS + i)) { // Если подчанк не существует в NBT
                        chunk[i] = new SubChunk(); // Создаем новый SubChunk
                        chunk[i].needsRebuild = true; // Помечаем его как нуждающийся в перестройке
                        MainRegistry.LOGGER.debug("  SubChunk {} did not exist, marked for rebuild.", i);
                        continue;
                    }
                    SubChunk sub = new SubChunk();
                    chunk[i] = sub;
                    sub.radiation = event.getData().getFloat(NBT_KEY_CHUNK_RADIATION + i);
                    for (int j = 0; j < 16; j++) sub.xResist[j] = event.getData().getFloat(NBT_KEY_CHUNK_RESISTANCE + "x_" + j + "_" + i);
                    for (int j = 0; j < 16; j++) sub.yResist[j] = event.getData().getFloat(NBT_KEY_CHUNK_RESISTANCE + "y_" + j + "_" + i);
                    for (int j = 0; j < 16; j++) sub.zResist[j] = event.getData().getFloat(NBT_KEY_CHUNK_RESISTANCE + "z_" + j + "_" + i);
                    MainRegistry.LOGGER.debug("  Loaded SubChunk {} with radiation: {}", i, sub.radiation);
                }
                radWorld.radiation.put(event.getChunk().getPos(), chunk);
                MainRegistry.LOGGER.debug("Finished loading chunk data for chunk: {}", event.getChunk().getPos());
            } else {
                MainRegistry.LOGGER.warn("RadPerWorld not found for level {} during chunk load.", level.dimension().location());
            }
        }
    }

    @Override
    public void receiveChunkLoad(LevelChunk chunk) {
        // Оставляем пустым, так как PRISM использует событие ChunkDataEvent.Load
    }

    @Override
    public void receiveChunkSave(ChunkDataEvent.Save event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide()) {
            MainRegistry.LOGGER.debug("Saving chunk data for chunk: {}", event.getChunk().getPos());
            RadPerWorld radWorld = perWorld.get(level);
            if (radWorld != null) {
                SubChunk[] chunk = radWorld.radiation.get(event.getChunk().getPos());
                if (chunk != null) {
                    for (int i = 0; i < 16; i++) {
                        SubChunk sub = chunk[i];
                        if (sub != null) {
                            float rad = sub.radiation;
                            event.getData().putFloat(NBT_KEY_CHUNK_RADIATION + i, rad);
                            for (int j = 0; j < 16; j++) event.getData().putFloat(NBT_KEY_CHUNK_RESISTANCE + "x_" + j + "_" + i, sub.xResist[j]);
                            for (int j = 0; j < 16; j++) event.getData().putFloat(NBT_KEY_CHUNK_RESISTANCE + "y_" + j + "_" + i, sub.yResist[j]);
                            for (int j = 0; j < 16; j++) event.getData().putFloat(NBT_KEY_CHUNK_RESISTANCE + "z_" + j + "_" + i, sub.zResist[j]);
                            event.getData().putBoolean(NBT_KEY_CHUNK_EXISTS + i, true);
                            MainRegistry.LOGGER.debug("  Saved SubChunk {} with radiation: {}", i, sub.radiation);
                        } else {
                            event.getData().putBoolean(NBT_KEY_CHUNK_EXISTS + i, false); // Mark as not existing if null
                            MainRegistry.LOGGER.debug("  SubChunk {} was null, marked as not existing.", i);
                        }
                    }
                } else {
                    MainRegistry.LOGGER.warn("No SubChunk data found for chunk {} during save.", event.getChunk().getPos());
                }
                MainRegistry.LOGGER.debug("Finished saving chunk data for chunk: {}", event.getChunk().getPos());
            } else {
                MainRegistry.LOGGER.warn("RadPerWorld not found for level {} during chunk save.", level.dimension().location());
            }
        }
    }

    @Override
    public void receiveChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            ChunkPos pos = event.getChunk().getPos();
            Map<ChunkPos, SubChunk[]> map = perWorld.get(level) != null ? perWorld.get(level).radiation : null;
            if (map != null) map.remove(pos);
        }
    }

    public static final HashMap<ChunkPos, SubChunk[]> newAdditions = new HashMap<>();

    @Override
    public void updateSystem() {
        cycles++;
        MainRegistry.LOGGER.debug("Updating radiation system. Cycle: {}", cycles);

        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            RadPerWorld system = perWorld.get(level);
            if (system == null) {
                MainRegistry.LOGGER.warn("RadPerWorld not found for level {} during system update.", level.dimension().location());
                continue;
            }

            int rebuildAllowance = 25;

            // Process existing chunks for rebuilding and radiation decay/spread
            Iterator<Map.Entry<ChunkPos, SubChunk[]>> chunkIterator = system.radiation.entrySet().iterator();
            while (chunkIterator.hasNext()) {
                Map.Entry<ChunkPos, SubChunk[]> chunkEntry = chunkIterator.next();
                ChunkPos coord = chunkEntry.getKey();
                SubChunk[] subChunksInChunk = chunkEntry.getValue();

                for (int i = 0; i < 16; i++) {
                    SubChunk sub = subChunksInChunk[i];

                    boolean hasTriedRebuild = false;

                    if (sub != null) {
                        sub.prevRadiation = sub.radiation;
                        sub.radiation = 0; // Reset radiation for recalculation

                        if (rebuildAllowance > 0 && sub.needsRebuild) {
                            MainRegistry.LOGGER.debug("  Rebuilding SubChunk (needsRebuild) at chunk: {}, yReg: {}", coord, i);
                            sub.rebuild(level, coord.x << 4, i << 4, coord.z << 4);
                            if (!sub.needsRebuild) {
                                rebuildAllowance--;
                                hasTriedRebuild = true;
                            }
                        }

                        // Periodically check for block changes and rebuild if checksum differs
                        if (!hasTriedRebuild && Math.abs(coord.x * coord.z) % 5 == cycles % 5 && level.getChunkSource().hasChunk(coord.x, coord.z)) {
                            LevelChunk c = level.getChunk(coord.x, coord.z);
                            int currentChecksum = 0;
                            
                            for (int iX = 0; iX < 16; iX++) {
                                for (int iY = 0; iY < 16; iY++) {
                                    for (int iZ = 0; iZ < 16; iZ++) {
                                        BlockPos blockPos = new BlockPos((coord.x << 4) + iX, (i << 4) + iY, (coord.z << 4) + iZ);
                                        BlockState blockState = c.getBlockState(blockPos);
                                        currentChecksum += blockState.hashCode();
                                    }
                                }
                            }

                            if (currentChecksum != sub.checksum) {
                                MainRegistry.LOGGER.debug("  Rebuilding SubChunk (checksum mismatch) at chunk: {}, yReg: {}", coord, i);
                                sub.rebuild(level, coord.x << 4, i << 4, coord.z << 4);
                            }
                        }
                    }
                }
            }

            // Spread radiation
            Iterator<Map.Entry<ChunkPos, SubChunk[]>> spreadIterator = system.radiation.entrySet().iterator();
            while (spreadIterator.hasNext()) {
                Map.Entry<ChunkPos, SubChunk[]> chunkEntry = spreadIterator.next();
                if (getPrevChunkRadiation(chunkEntry.getValue()) <= 0) continue; // Skip if no previous radiation

                for (int i = 0; i < 16; i++) {
                    SubChunk sub = chunkEntry.getValue()[i];

                    if (sub != null) {
                        if (sub.prevRadiation <= 0 || Float.isNaN(sub.prevRadiation) || Float.isInfinite(sub.prevRadiation)) continue;
                        
                        float radSpread = 0;
                        for (Direction dir : Direction.values()) {
                            radSpread += spreadRadiation(level, sub, i, chunkEntry.getKey(), chunkEntry.getValue(), system.radiation, dir);
                        }
                        sub.radiation += (sub.prevRadiation - radSpread) * 0.95F; // Apply decay and spread
                        sub.radiation -= 1F; // Base decay
                        sub.radiation = Mth.clamp(sub.radiation, 0, MAX_RADIATION);
                        if (sub.radiation > 0) {
                            MainRegistry.LOGGER.debug("  SubChunk at chunk: {}, yReg: {} has radiation: {}", chunkEntry.getKey(), i, sub.radiation);
                        }
                    }
                }
            }

            system.radiation.putAll(newAdditions);
            newAdditions.clear();
            MainRegistry.LOGGER.debug("Finished updating radiation system for level: {}", level.dimension().location());
        }
    }

    private static float spreadRadiation(Level level, SubChunk source, int y, ChunkPos origin, SubChunk[] chunk, ConcurrentHashMap<ChunkPos, SubChunk[]> map, Direction dir) {
        float spread = 0.1F;
        float amount = source.prevRadiation * spread;

        if (amount <= 1F) return 0;

        if (dir.getAxis().isHorizontal()) { // X or Z axis
            ChunkPos newPos = new ChunkPos(origin.x + dir.getStepX(), origin.z + dir.getStepZ());
            if (!level.getChunkSource().hasChunk(newPos.x, newPos.z)) return amount;
            SubChunk[] newChunk = map.get(newPos);
            if (newChunk == null) {
                newChunk = new SubChunk[16];
                newAdditions.put(newPos, newChunk);
            }
            if (newChunk[y] == null) newChunk[y] = new SubChunk().rebuild(level, newPos.x << 4, y << 4, newPos.z << 4);
            SubChunk to = newChunk[y];
            return spreadRadiationTo(source, to, amount, dir);
        } else { // Y axis
            if (dir == Direction.UP && y == 15) return amount; // out of world
            if (dir == Direction.DOWN && y == 0) return amount; // out of world
            if (chunk[y + dir.getStepY()] == null) chunk[y + dir.getStepY()] = new SubChunk().rebuild(level, origin.x << 4, (y + dir.getStepY()) << 4, origin.z << 4);
            SubChunk to = chunk[y + dir.getStepY()];
            return spreadRadiationTo(source, to, amount, dir);
        }
    }

    private static float spreadRadiationTo(SubChunk from, SubChunk to, float amount, Direction movement) {
        float resistance = from.getResistanceValue(movement.getOpposite()) + to.getResistanceValue(movement);
        double fun = Math.pow(Math.E, -resistance / 10_000D);
        float toMove = (float) Math.min(amount * fun, amount);
        to.radiation += toMove;
        return toMove;
    }

    private static float getPrevChunkRadiation(SubChunk[] chunk) {
        float rad = 0;
        for (SubChunk sub : chunk) if (sub != null) rad += sub.prevRadiation;
        return rad;
    }

    @Override
    public void clearSystem(Level level) {
        RadPerWorld system = perWorld.get(level);
        if (system != null) system.radiation.clear();
    }

    public static class RadPerWorld {
        public ConcurrentHashMap<ChunkPos, SubChunk[]> radiation = new ConcurrentHashMap<>();
    }

    @Override
    public void onBlockUpdated(Level level, BlockPos pos) {
        // TODO: Реализуйте здесь логику для PRISM системы.
        // Например, если PRISM тоже должен пересчитывать источники радиации в чанке,
        // то соответствующий код должен быть вызван здесь.
        // Пока что можно оставить пустым, чтобы код скомпилировался.
    }

    public static class SubChunk {
        public float prevRadiation;
        public float radiation;
        public float[] xResist = new float[16];
        public float[] yResist = new float[16];
        public float[] zResist = new float[16];
        public boolean needsRebuild = false;
        public int checksum = 0;

        public SubChunk rebuild(Level level, int x, int y, int z) {
            long startTime = System.nanoTime(); // Объявляем startTime здесь
            MainRegistry.LOGGER.debug("Starting rebuild of SubChunk at X: {}, Y: {}, Z: {}", x, y, z);
            needsRebuild = true;
            int cX = x >> 4;
            int cY = Mth.clamp(y >> 4, 0, 15);
            int cZ = z >> 4;

            if (!level.getChunkSource().hasChunk(cX, cZ)) return this;

            int tX = cX << 4;
            int tY = cY << 4;
            int tZ = cZ << 4; // Corrected from cX << 4

            for (int i = 0; i < 16; i++) xResist[i] = yResist[i] = zResist[i] = 0;

            LevelChunk chunk = level.getChunk(cX, cZ);
            checksum = 0;

            for (int iX = 0; iX < 16; iX++) {
                for (int iY = 0; iY < 16; iY++) {
                    for (int iZ = 0; iZ < 16; iZ++) {
                        BlockPos blockPos = new BlockPos(tX + iX, tY + iY, tZ + iZ);
                        BlockState blockState = chunk.getBlockState(blockPos);
                        Block block = blockState.getBlock();

                        if (blockState.isAir()) continue;

                        float resistance = Math.min(blockState.getExplosionResistance(level, blockPos, null), 100);
                        xResist[iX] += resistance;
                        yResist[iY] += resistance;
                        zResist[iZ] += resistance;
                        checksum += blockState.hashCode();
                        
                    }
                }
            }

            needsRebuild = false;
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // миллисекунды
            MainRegistry.LOGGER.debug("Finished rebuild of SubChunk at X: {}, Y: {}, Z: {}. Took {} ms.", x, y, z, duration);
            return this;
        }

        public float getResistanceValue(Direction movement) {
            if (movement == Direction.EAST) return getResistanceFromArray(xResist, true);
            if (movement == Direction.WEST) return getResistanceFromArray(xResist, false);
            if (movement == Direction.UP) return getResistanceFromArray(yResist, true);
            if (movement == Direction.DOWN) return getResistanceFromArray(yResist, false);
            if (movement == Direction.SOUTH) return getResistanceFromArray(zResist, true);
            if (movement == Direction.NORTH) return getResistanceFromArray(zResist, false);
            return 0;
        }

        private float getResistanceFromArray(float[] resist, boolean reverse) {
            float res = 0F;
            for (int i = 1; i < 16; i++) {
                int index = reverse ? 15 - i : i;
                res += resist[index] / 15F * i;
            }
            return res;
        }
    }
}