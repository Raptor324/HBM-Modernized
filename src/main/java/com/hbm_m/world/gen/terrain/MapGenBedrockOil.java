package com.hbm_m.world.gen.terrain;

import java.util.function.Function;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.WorldConfig;
import com.hbm_m.world.gen.legacy.MB;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;

/**
 * 1:1 {@code com.hbm.world.gen.terrain.MapGenBedrockOil}: Grundgestein-Erdoelvorkommen (Oktaeder aus
 * {@code ore_bedrock_oil} in den untersten 5 Schichten) mit Oelflecken an der Oberflaeche (oelige/tote Erde,
 * Rohrkolben, tote Pflanzen, verschmutzter Sand, rissiger Stein).
 *
 * <p>Port: Die untersten 5 Schichten liegen in 1.20 ab {@code getMinBuildHeight()}; "stone" schliesst dort den
 * Tiefenschiefer ein.</p>
 */
public class MapGenBedrockOil extends LegacyMapGenCarver {

    private int frequency;

    public BlockState block;

    public int spotWidth = 5;
    public int spotCount = 50;
    public boolean addWillows = true;

    public MapGenBedrockOil(Codec<CarverConfiguration> codec) {
        super(codec);
    }

    @Override
    protected void generate(ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource rand, int offsetX, int offsetZ, int chunkX, int chunkZ) {
        this.frequency = WorldConfig.bedrockOilSpawn;
        if (frequency <= 0) return;
        if (block == null) block = ModBlocks.ORE_BEDROCK_OIL.get().defaultBlockState();

        if (rand.nextInt(frequency) == frequency - 2) {
            int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
            int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

            int bottom = chunk.getMinBuildHeight();

            // das Vorkommen im Grundgestein
            for (int bx = 15; bx >= 0; bx--)
            for (int bz = 15; bz >= 0; bz--)
            for (int y = 0; y < 5; y++) {
                BlockState b = get(chunk, bx, bottom + y, bz);

                if (b.is(BlockTags.STONE_ORE_REPLACEABLES) || b.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || b.is(Blocks.BEDROCK)) {
                    // x, z: Koordinaten relativ zum Ursprung des virtuellen Ziel-Chunks
                    int x = xCoord + bx;
                    int z = zCoord + bz;

                    if (Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
                        set(chunk, bx, bottom + y, bz, block);
                    }
                }
            }

            int deadMetaCount = 5; // EnumDeadPlantType

            // Oelflecken-Schaden
            for (int i = 0; i < spotCount; i++) {
                int rx = (int) (rand.nextGaussian() * spotWidth) - xCoord;
                int rz = (int) (rand.nextGaussian() * spotWidth) - zCoord;

                if (rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
                    // Bodenhoehe suchen
                    for (int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, rx, rz) + 1; y >= bottom; y--) {
                        BlockState top = get(chunk, rx, y, rz);

                        if (present(top) && opaque(top)) {
                            for (int oy = 1; oy > -3; oy--) {
                                int subY = y + oy;
                                BlockState sub = get(chunk, rx, subY, rz);

                                if (sub.is(Blocks.GRASS_BLOCK) || sub.is(Blocks.DIRT)) {
                                    set(chunk, rx, subY, rz, rand.nextInt(10) == 0 ? MB.dirt_oily.state(0) : MB.dirt_dead.state(0));

                                    if (addWillows && oy == 0 && rand.nextInt(50) == 0) {
                                        set(chunk, rx, subY + 1, rz, MB.plant_flower.state(4)); // CD0
                                    }

                                    // passiert VOR der Dekoration, es gibt also noch keine Pflanzen - stattdessen neue setzen
                                    if (oy == 0 && rand.nextInt(20) == 0) {
                                        set(chunk, rx, subY + 1, rz, MB.plant_dead.state(rand.nextInt(deadMetaCount)));
                                    }

                                    break;
                                } else if (sub.is(Blocks.SAND) || sub.is(Blocks.RED_SAND) || sub.is(ModBlocks.ORE_OIL_SAND.get())) {
                                    if (sub.is(Blocks.RED_SAND)) {
                                        set(chunk, rx, subY, rz, MB.sand_dirty_red.state(0));
                                    } else {
                                        set(chunk, rx, subY, rz, MB.sand_dirty.state(0));
                                    }
                                    break;
                                } else if (sub.is(Blocks.STONE)) {
                                    set(chunk, rx, subY, rz, MB.stone_cracked.state(0));
                                    break;
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
