package com.hbm_m.world.gen.terrain;

import java.util.function.Function;

import com.hbm_m.world.gen.LegacyBiome;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

/**
 * Port-Hilfe: 1.7.10-{@code MapGenBase} (vom {@code MapGenChainloader} an den Hoehlengenerator gehaengt) als
 * 1.20-{@link WorldCarver}. Beide arbeiten gleich: fuer jeden Chunk wird jeder Start-Chunk im Umkreis mit einem nur vom
 * Start-Chunk abhaengigen Zufall befragt ({@code func_151538_a(world, offsetX, offsetZ, chunkX, chunkZ, blocks)}) und
 * darf die Bloecke des aktuellen Chunks veraendern.
 *
 * <p>Die Unterklassen arbeiten wie das Original auf Chunk-lokalen Koordinaten ({@code bx}, {@code bz} 0-15) und
 * absoluten y-Werten der 1.7.10-Welt; {@link #get}/{@link #set} uebersetzen.</p>
 */
public abstract class LegacyMapGenCarver extends WorldCarver<CarverConfiguration> {

    public LegacyMapGenCarver(Codec<CarverConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean isStartChunk(CarverConfiguration config, RandomSource random) {
        // die Haeufigkeit pruefen die Originalklassen selbst; ein Zug fuer die Vanilla-Wahrscheinlichkeit
        return random.nextFloat() <= config.probability;
    }

    @Override
    public boolean carve(CarvingContext ctx, CarverConfiguration config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor,
            RandomSource random, Aquifer aquifer, ChunkPos startChunk, CarvingMask mask) {
        ChunkPos pos = chunk.getPos();
        generate(chunk, biomeAccessor, random, startChunk.x, startChunk.z, pos.x, pos.z);
        return true;
    }

    /** {@code func_151538_a(world, offsetX, offsetZ, chunkX, chunkZ, blocks)}. */
    protected abstract void generate(ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource rand, int offsetX, int offsetZ, int chunkX, int chunkZ);

    protected static LegacyBiome biomeAt(Function<BlockPos, Holder<Biome>> biomeAccessor, int x, int z) {
        return LegacyBiome.of(biomeAccessor.apply(new BlockPos(x, 64, z)));
    }

    protected static BlockState get(ChunkAccess chunk, int bx, int y, int bz) {
        if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) return Blocks.AIR.defaultBlockState();
        return chunk.getBlockState(new BlockPos(chunk.getPos().getMinBlockX() + bx, y, chunk.getPos().getMinBlockZ() + bz));
    }

    protected static void set(ChunkAccess chunk, int bx, int y, int bz, BlockState state) {
        if (y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) return;
        chunk.setBlockState(new BlockPos(chunk.getPos().getMinBlockX() + bx, y, chunk.getPos().getMinBlockZ() + bz), state, false);
    }

    /** 1.7.10 {@code isOpaqueCube}. */
    protected static boolean opaque(BlockState s) {
        return !s.isAir() && s.canOcclude();
    }

    /** {@code blocks[index] != null} (nicht Luft). */
    protected static boolean present(BlockState s) {
        return !s.isAir();
    }

    protected static java.util.Random javaRandom(RandomSource rand) {
        return new java.util.Random(rand.nextLong());
    }
}
