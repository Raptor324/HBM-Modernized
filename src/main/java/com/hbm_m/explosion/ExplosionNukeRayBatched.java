package com.hbm_m.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Батчированный лучевой взрыв для ядерной бомбы MK5.
 */
public class ExplosionNukeRayBatched implements IExplosionRay {

    /** Карта: чанк -> список конечных точек лучей в этом чанке. */
    public Map<ChunkPos, List<FloatTriplet>> perChunk = new HashMap<>();
    /** Порядок обработки чанков (от центра к периферии). */
    public List<ChunkPos> orderedChunks = new ArrayList<>();
    private final CoordComparator comparator = new CoordComparator();

    int posX;
    int posY;
    int posZ;
    Level level;

    int strength;
    int length;
    int speed;
    int gspNumMax;
    int gspNum;
    double gspX;
    double gspY;

    public boolean isAusf3Complete = false;

    public ExplosionNukeRayBatched(Level level, int x, int y, int z, int strength, int speed, int length) {
        this.level = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.speed = speed;
        this.length = length;
        // Общее количество направлений (точек на сфере)
        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNum = 1;

        // Начальные сферические координаты
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

    /** Преобразует текущие сферические координаты в декартовы. */
    private Vec3 getSpherical2cartesian() {
        double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
        double dz = Math.sin(this.gspX) * Math.sin(this.gspY);
        double dy = Math.cos(this.gspX);
        return new Vec3(dx, dy, dz);
    }

    /**
     * Предварительный проход: собирает кончики лучей и группирует их по чанкам.
     *
     * @param count максимальное количество направлений для обработки за вызов
     */
    public void collectTip(int count) {
        int amountProcessed = 0;

        while (this.gspNumMax >= this.gspNum) {
            Vec3 vec = this.getSpherical2cartesian();

            int maxLen = (int) Math.ceil(strength);
            float res = strength;

            FloatTriplet lastPos = null;
            Set<ChunkPos> chunkCoords = new HashSet<>();

            for (int i = 0; i < maxLen; i++) {

                if (i > this.length) break;

                float x0 = (float) (posX + (vec.x * i));
                float y0 = (float) (posY + (vec.y * i));
                float z0 = (float) (posZ + (vec.z * i));

                int iX = Mth.floor(x0);
                int iY = Mth.floor(y0);
                int iZ = Mth.floor(z0);

                double fac = 100 - ((double) i) / ((double) maxLen) * 100;
                fac *= 0.07D;

                BlockPos pos = new BlockPos(iX, iY, iZ);
                BlockState state = level.getBlockState(pos);

                if (state.getFluidState().isEmpty()) {
                    res -= (float) Math.pow(masqueradeResistance(level, state, pos), 7.5D - fac);
                }

                if (res > 0 && !state.isAir()) {
                    lastPos = new FloatTriplet(x0, y0, z0);
                    // пустые чанки (полностью воздух) не буферим
                    ChunkPos chunkPos = new ChunkPos(iX >> 4, iZ >> 4);
                    chunkCoords.add(chunkPos);
                }

                if (res <= 0 || i + 1 >= this.length || i == maxLen - 1) {
                    break;
                }
            }

            for (ChunkPos pos : chunkCoords) {
                List<FloatTriplet> triplets = perChunk.get(pos);

                if (triplets == null) {
                    triplets = new ArrayList<>();
                    // используем один и тот же объект ключа для экономии памяти
                    perChunk.put(pos, triplets);
                }

                if (lastPos != null) {
                    triplets.add(lastPos);
                }
            }

            // шаг по обобщённой спирали
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

    public class CoordComparator implements Comparator<ChunkPos> {

        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {

            int chunkX = ExplosionNukeRayBatched.this.posX >> 4;
            int chunkZ = ExplosionNukeRayBatched.this.posZ >> 4;

            int diff1 = Math.abs(chunkX - o1.x) + Math.abs(chunkZ - o1.z);
            int diff2 = Math.abs(chunkX - o2.x) + Math.abs(chunkZ - o2.z);

            return diff1 - diff2;
        }
    }

    /** Разрушает блоки вдоль лучей внутри одного чанка. */
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
                Math.abs(posZ - (chunkZ << 4))) - 16; // смещаемся внутрь чанка, чтобы сократить пустые проходы

        enter = Math.max(enter, 0);

        for (FloatTriplet triplet : list) {
            float x = triplet.xCoord;
            float y = triplet.yCoord;
            float z = triplet.zCoord;
            Vec3 vec = new Vec3(x - this.posX, y - this.posY, z - this.posZ);
            double pX = vec.x / vec.length();
            double pY = vec.y / vec.length();
            double pZ = vec.z / vec.length();

            int tipX = Mth.floor(x);
            int tipY = Mth.floor(y);
            int tipZ = Mth.floor(z);

            boolean inChunk = false;
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            for (int i = enter; i < vec.length(); i++) {
                int x0 = Mth.floor(posX + pX * i);
                int y0 = Mth.floor(posY + pY * i);
                int z0 = Mth.floor(posZ + pZ * i);

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
                this.handleTip(pos.getX(), pos.getY(), pos.getZ());
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        perChunk.remove(coord);
        orderedChunks.remove(0);
    }

    /** Обработка «кончика» луча — при желании можно переопределить для спец-блоков. */
    protected void handleTip(int x, int y, int z) {
        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public void cacheChunksTick(int time) {
        if (!isAusf3Complete) {
            // оригинальный код игнорировал лимит времени и просто обрабатывал фиксированное число направлений
            collectTip(speed * 10);
        }
    }

    @Override
    public void destructionTick(int time) {
        if (!isAusf3Complete) return;
        long start = System.currentTimeMillis();
        while (!perChunk.isEmpty() && System.currentTimeMillis() < start + time) {
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
        public float xCoord;
        public float yCoord;
        public float zCoord;

        public FloatTriplet(float x, float y, float z) {
            xCoord = x;
            yCoord = y;
            zCoord = z;
        }
    }
}

