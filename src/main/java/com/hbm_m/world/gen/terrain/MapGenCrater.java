package com.hbm_m.world.gen.terrain;

import java.util.function.Function;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.config.WorldConfig;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;

/**
 * 1:1 {@code com.hbm.world.gen.terrain.MapGenCrater}: Strahlungs-Krater ("Sellafield") in der Wueste
 * ({@code radfreq}, Radius 8-64, Tiefe 0,35 * Radius, Boden aus geloeschtem Sellafit).
 */
public class MapGenCrater extends LegacyMapGenCarver {

    private int frequency = 100;
    private int minSize = 8;
    private int maxSize = 64;

    public BlockState regolith;
    public BlockState rock;

    public String targetBiome;

    public MapGenCrater(Codec<CarverConfiguration> codec) {
        super(codec);
    }

    /** Original HbmWorld.registerNTMTerrain: sellafieldCrater. */
    private void setup() {
        this.frequency = WorldConfig.radfreq;
        this.regolith = this.rock = ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
        this.targetBiome = "desert";
    }

    public void setSize(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    private double depthFunc(double x, double rad, double depth) {
        return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
    }

    // Wird von -range bis +range auf beiden XZ-Achsen durchlaufen.
    @Override
    protected void generate(ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource rand, int offsetX, int offsetZ, int chunkX, int chunkZ) {
        if (!(GeneralConfig.enableRad && WorldConfig.radfreq > 0)) return;
        if (regolith == null) setup();

        if (rand.nextInt(frequency) == 0 && (targetBiome == null || biomeAt(biomeAccessor, offsetX * 16, offsetZ * 16).is(targetBiome))) {
            int xCoord = -offsetX + chunkX;
            int zCoord = -offsetZ + chunkZ;

            double radius = rand.nextInt(maxSize - minSize) + minSize;
            double depth = radius * 0.35D;

            int bottom = chunk.getMinBuildHeight();

            for (int bx = 15; bx >= 0; bx--) { // bx, bz: Koordinate des veraenderten Blocks relativ zum Chunk-Ursprung
                for (int bz = 15; bz >= 0; bz--) {
                    for (int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz) + 1; y >= bottom; y--) {
                        BlockState b = get(chunk, bx, y, bz);

                        if (present(b) && (opaque(b) || !b.getFluidState().isEmpty())) {
                            // x, z: Koordinaten relativ zum Ursprung des virtuellen Ziel-Chunks
                            int x = xCoord * 16 + bx;
                            int z = zCoord * 16 + bz;

                            // y ist jetzt die aktuelle Hoehe
                            double r = Math.sqrt(x * x + z * z);

                            if (r - rand.nextInt(3) <= radius) {
                                // bis zur gewuenschten Tiefe ausheben
                                int dep = (int) Mth.clamp(depthFunc(r, radius, depth), 0, y - bottom - 1);
                                for (int i = 0; i < dep; i++) {
                                    set(chunk, bx, y - i, bz, Blocks.AIR.defaultBlockState());
                                }

                                y -= dep;

                                dep = Math.min(3, y - bottom - 1);

                                // wieder auffuellen
                                if (r + rand.nextInt(3) <= radius / 3D) {
                                    for (int i = 0; i < dep; i++) {
                                        set(chunk, bx, y - i, bz, regolith);
                                    }
                                } else {
                                    for (int i = 0; i < dep; i++) {
                                        set(chunk, bx, y - i, bz, rock);
                                    }
                                }
                            }

                            break;
                        }
                    }
                }
            }
        }
    }
}
