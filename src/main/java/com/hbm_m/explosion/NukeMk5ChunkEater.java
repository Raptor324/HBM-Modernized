package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * chunk-by-chunk "crater" for Fat Man / MK5.
 * Do not confuse with {@link com.hbm_m.util.explosions.nuclear.CraterGenerator} (used by other charges).
 */
public class NukeMk5ChunkEater implements IExplosionRay {

    public final Map<ChunkPos, List<FloatTriplet>> perChunk = new HashMap<>();
    public final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final CoordComparator comparator = new CoordComparator();

    private final int posX;
    private final int posY;
    private final int posZ;
    private final Level level;
    private final int strength;
    private final int length;
    private final int speed;

    private int gspNumMax;
    private int gspNum;
    private double gspX;
    private double gspY;

    public boolean isAusf3Complete = false;

    public NukeMk5ChunkEater(Level level, int x, int y, int z, int strength, int speed, int length) {
        this.level = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.speed = speed;
        this.length = length;
        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNum = 1;
        this.gspX = Math.PI;
        this.gspY = 0.0;
    }

    private void generateGspUp() {
        if (this.gspNum < this.gspNumMax) {
            int k = this.gspNum + 1;
            double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
            this.gspX = Math.acos(hk);
            double prevLon = this.gspY;
            double lon = prevLon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
            this.gspY = lon % (Math.PI * 2);
        } else {
            this.gspX = 0.0;
            this.gspY = 0.0;
        }
        this.gspNum++;
    }

    private Vec3 getSpherical2cartesian() {
        double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
        double dz = Math.sin(this.gspX) * Math.sin(this.gspY);
        double dy = Math.cos(this.gspX);
        return new Vec3(dx, dy, dz);
    }

    public void collectTip(int count) {
        int amountProcessed = 0;
        int rayLength = (int) Math.ceil(strength);

        while (this.gspNumMax >= this.gspNum) {
            Vec3 vec = this.getSpherical2cartesian();
            float res = strength;
            FloatTriplet lastPos = null;
            Set<ChunkPos> chunkCoords = new HashSet<>();

            for (int i = 0; i < rayLength; i++) {
                if (i > this.length) break;

                float x0 = (float) (posX + (vec.x * i));
                float y0 = (float) (posY + (vec.y * i));
                float z0 = (float) (posZ + (vec.z * i));

                int iX = (int) Math.floor(x0);
                int iY = (int) Math.floor(y0);
                int iZ = (int) Math.floor(z0);

                double fac = 100 - ((double) i) / ((double) rayLength) * 100;
                fac *= 0.07D;

                BlockPos pos = new BlockPos(iX, iY, iZ);
                BlockState state = level.getBlockState(pos);

                if (state.getFluidState().isEmpty()) {
                    res -= (float) Math.pow(masqueradeResistance(level, state, pos), 7.5D - fac);
                }

                if (res > 0 && !state.isAir()) {
                    lastPos = new FloatTriplet(x0, y0, z0);
                    ChunkPos chunkPos = new ChunkPos(iX >> 4, iZ >> 4);
                    chunkCoords.add(chunkPos);
                }

                if (res <= 0 || i + 1 >= this.length || i == rayLength - 1) {
                    break;
                }
            }

            for (ChunkPos pos : chunkCoords) {
                List<FloatTriplet> triplets = perChunk.get(pos);
                if (triplets == null) {
                    triplets = new ArrayList<>();
                    perChunk.put(pos, triplets);
                }
                if (lastPos != null) {
                    triplets.add(lastPos);
                }
            }

            this.generateGspUp();
            amountProcessed++;
            if (amountProcessed >= count) {
                return;
            }
        }

        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(comparator);
        isAusf3Complete = true;
    }

    public static float masqueradeResistance(Level level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block == Blocks.SANDSTONE) {
            return Blocks.STONE.defaultBlockState().getExplosionResistance(level, pos, null);
        }
        if (block == Blocks.OBSIDIAN) {
            return Blocks.STONE.defaultBlockState().getExplosionResistance(level, pos, null) * 3;
        }
        return state.getExplosionResistance(level, pos, null);
    }

    private class CoordComparator implements Comparator<ChunkPos> {
        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {
            int chunkX = posX >> 4;
            int chunkZ = posZ >> 4;
            int diff1 = Math.abs(chunkX - o1.x) + Math.abs(chunkZ - o1.z);
            int diff2 = Math.abs(chunkX - o2.x) + Math.abs(chunkZ - o2.z);
            return diff1 - diff2;
        }
    }

    public void processChunk() {
        if (this.perChunk.isEmpty()) return;

        ChunkPos coord = orderedChunks.get(0);
        List<FloatTriplet> list = perChunk.get(coord);
        Set<BlockPos> toRem = new HashSet<>();
        Set<BlockPos> toRemTips = new HashSet<>();

        int chunkX = coord.x;
        int chunkZ = coord.z;

        int enter = Math.min(
                Math.abs(posX - (chunkX << 4)),
                Math.abs(posZ - (chunkZ << 4))) - 16;
        enter = Math.max(enter, 0);

        for (FloatTriplet triplet : list) {
            float x = triplet.xCoord;
            float y = triplet.yCoord;
            float z = triplet.zCoord;
            Vec3 vec = new Vec3(x - this.posX, y - this.posY, z - this.posZ);
            double len = vec.length();
            if (len <= 0) continue;
            double pX = vec.x / len;
            double pY = vec.y / len;
            double pZ = vec.z / len;

            int tipX = (int) Math.floor(x);
            int tipY = (int) Math.floor(y);
            int tipZ = (int) Math.floor(z);

            boolean inChunk = false;
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int i = enter; i < len; i++) {
                int x0 = (int) Math.floor(posX + pX * i);
                int y0 = (int) Math.floor(posY + pY * i);
                int z0 = (int) Math.floor(posZ + pZ * i);

                mutablePos.set(x0, y0, z0);
                BlockState state = level.getBlockState(mutablePos);

                if ((x0 >> 4) != chunkX || (z0 >> 4) != chunkZ) {
                    if (inChunk) {
                        break;
                    } else {
                        continue;
                    }
                }
                inChunk = true;

                if (!state.isAir()) {
                    BlockPos pos = new BlockPos(x0, y0, z0);
                    if (x0 == tipX && y0 == tipY && z0 == tipZ) {
                        toRemTips.add(pos);
                    }
                    toRem.add(pos);
                }
            }
        }

        for (BlockPos pos : toRem) {
            if (toRemTips.contains(pos)) {
                handleTip(pos.getX(), pos.getY(), pos.getZ());
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        perChunk.remove(coord);
        orderedChunks.remove(0);
    }

    protected void handleTip(int x, int y, int z) {
        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public void cacheChunksTick(int processTimeMs) {
        if (!isAusf3Complete) {
            collectTip(speed * 10);
        }
    }

    @Override
    public void destructionTick(int processTimeMs) {
        if (!isAusf3Complete) return;
        long start = System.currentTimeMillis();
        while (!perChunk.isEmpty() && System.currentTimeMillis() < start + processTimeMs) {
            processChunk();
        }
    }

    @Override
    public void cancel() {
        isAusf3Complete = true;
        if (perChunk != null) perChunk.clear();
        if (orderedChunks != null) orderedChunks.clear();
    }

    public static class FloatTriplet {
        public final float xCoord;
        public final float yCoord;
        public final float zCoord;

        public FloatTriplet(float x, float y, float z) {
            this.xCoord = x;
            this.yCoord = y;
            this.zCoord = z;
        }
    }
}
