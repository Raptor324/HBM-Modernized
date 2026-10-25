package com.hbm_m.world.gen.legacy;

import java.util.Random;

import com.hbm_m.world.gen.LegacyBiome;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Port-Hilfe: Bauwerke, die groesser als das 3x3-Chunkfenster der 1.20-Merkmale sind (Original: direkt in die Welt
 * gebaut), werden einmal vollstaendig in diesen Puffer gebaut und danach chunkweise uebertragen
 * ({@link com.hbm_m.world.gen.LegacyDungeonStructure}). Solange ein Puffer aktiv ist ({@link L#virtual}), leiten alle
 * {@code L}-Aufrufe hierher um.
 *
 * <p>Gelaende ausserhalb des Puffers stammt aus dem Rauschen des Generators; die Oberflaeche wird aus Ober-/Fuellblock
 * des Bioms nachgebildet (Hoehlen, Baeume und Erze der echten Welt sind dabei unbekannt).</p>
 */
public final class VirtualWorld {

    public final ChunkGenerator generator;
    public final RandomState randomState;
    public final LevelHeightAccessor height;
    public final Random rand;
    public final long seed;
    /** Ersatz fuer den statischen {@code Library.rand} (deterministisch je Bauwerk). */
    public final Random libraryRand;

    /** Gesetzte Bloecke (BlockPos.asLong). */
    public final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
    /** Losgeloeste Blockeinheiten (Kisten, Spawner ...), deren Daten beim Uebertragen kopiert werden. */
    public final Long2ObjectOpenHashMap<BlockEntity> blockEntities = new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<NoiseColumn> columns = new Long2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap surfaces = new Long2IntOpenHashMap();
    private final Long2ObjectOpenHashMap<LegacyBiome> biomes = new Long2ObjectOpenHashMap<>();

    public VirtualWorld(ChunkGenerator generator, RandomState randomState, LevelHeightAccessor height, long seed, Random rand) {
        this.generator = generator;
        this.randomState = randomState;
        this.height = height;
        this.seed = seed;
        this.rand = rand;
        this.libraryRand = new Random(rand.nextLong());
        this.surfaces.defaultReturnValue(Integer.MIN_VALUE);
    }

    // ================================ Gelaende ================================

    private NoiseColumn column(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        NoiseColumn c = columns.get(key);
        if (c == null) {
            c = generator.getBaseColumn(x, z, height, randomState);
            columns.put(key, c);
        }
        return c;
    }

    /** Erste Luft ueber dem obersten Gelaendeblock (inkl. Wasser) aus dem Rauschen. */
    public int surface(int x, int z) {
        long key = ChunkPos.asLong(x, z);
        int s = surfaces.get(key);
        if (s == Integer.MIN_VALUE) {
            s = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, randomState);
            surfaces.put(key, s);
        }
        return s;
    }

    public LegacyBiome biome(int x, int y, int z) {
        long key = BlockPos.asLong(x >> 2, y >> 2, z >> 2);
        LegacyBiome b = biomes.get(key);
        if (b == null) {
            b = LegacyBiome.of(generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler()));
            biomes.put(key, b);
        }
        return b;
    }

    private BlockState terrain(int x, int y, int z) {
        if (y < height.getMinBuildHeight() || y >= height.getMaxBuildHeight()) return Blocks.AIR.defaultBlockState();
        BlockState s = column(x, z).getBlock(y);
        if (s.is(Blocks.STONE)) {
            int top = surface(x, z);
            // Oberflaeche nachbilden: oberster Block = Oberblock, darunter 3 Fuellbloecke
            if (y == top - 1) return biome(x, y, z).topBlock();
            if (y >= top - 4 && y < top - 1) return biome(x, y, z).fillerBlock();
        }
        return s;
    }

    // ================================ Zugriffe ================================

    public BlockState getState(int x, int y, int z) {
        BlockState s = blocks.get(BlockPos.asLong(x, y, z));
        return s != null ? s : terrain(x, y, z);
    }

    public boolean setState(int x, int y, int z, BlockState state) {
        if (y < height.getMinBuildHeight() || y >= height.getMaxBuildHeight()) return false;
        long key = BlockPos.asLong(x, y, z);
        blocks.put(key, state);
        grow(x, y, z);
        blockEntities.remove(key);
        if (state.hasBlockEntity() && state.getBlock() instanceof EntityBlock eb) {
            try {
                BlockEntity be = eb.newBlockEntity(new BlockPos(x, y, z), state);
                if (be != null) blockEntities.put(key, be);
            } catch (Exception ignored) {
                // Blockeinheit ohne Welt nicht erzeugbar - wird beim Uebertragen vom Block selbst angelegt
            }
        }
        return true;
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return blockEntities.get(BlockPos.asLong(x, y, z));
    }

    /** 1.7.10 {@code getHeightValue}: erste Luft ueber dem obersten bewegungsblockierenden Block oder Fluessigkeit. */
    public int getHeightValue(int x, int z) {
        int y = Math.max(surface(x, z), maxY + 1);
        int min = height.getMinBuildHeight();
        while (y > min) {
            BlockState s = getState(x, y - 1, z);
            if (s.blocksMotion() || !s.getFluidState().isEmpty()) break;
            y--;
        }
        return y;
    }

    // ================================ Uebertragen ================================

    private Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.longs.LongArrayList> byChunk;

    /** Alle gesetzten Positionen eines Chunks (fuer das chunkweise Uebertragen). */
    public synchronized it.unimi.dsi.fastutil.longs.LongList inChunk(int chunkX, int chunkZ) {
        if (byChunk == null) {
            byChunk = new Long2ObjectOpenHashMap<>();
            for (long key : blocks.keySet()) {
                long c = ChunkPos.asLong(BlockPos.getX(key) >> 4, BlockPos.getZ(key) >> 4);
                it.unimi.dsi.fastutil.longs.LongArrayList list = byChunk.get(c);
                if (list == null) byChunk.put(c, list = new it.unimi.dsi.fastutil.longs.LongArrayList());
                list.add(key);
            }
        }
        it.unimi.dsi.fastutil.longs.LongArrayList list = byChunk.get(ChunkPos.asLong(chunkX, chunkZ));
        return list != null ? list : it.unimi.dsi.fastutil.longs.LongLists.EMPTY_LIST;
    }

    // ================================ Rahmen ================================

    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    void grow(int x, int y, int z) {
        if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
        if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
    }

    /** Umfassender Rahmen aller gesetzten Bloecke oder null. */
    public BoundingBox bounds() {
        if (blocks.isEmpty()) return null;
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
