package com.hbm_m.world.gen.terrain;

import java.util.function.Function;
import java.util.function.Predicate;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.WorldConfig;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.legacy.MB;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;

/**
 * 1:1 {@code com.hbm.world.gen.terrain.MapGenBubble}: abgeplattete Erdoel-Blasen (3 x 1 x 3), breiter als ein
 * Chunk, plus Oberflaechenflecken mit Bohrloch. Zwei Auspraegungen wie in {@code HbmWorld.registerNTMTerrain}:
 * Gestein ({@code oilSpawn}, r 8-16) und Oelsand (1/200, nur warme Trockenbiome, y 56-71, r 16-48, unscharf).
 */
public class MapGenBubble extends LegacyMapGenCarver {

    private final boolean sand;

    private int frequency;
    private int minSize = 8;
    private int maxSize = 64;

    public int minY = 15;
    public int rangeY = 25;

    public boolean fuzzy;

    public BlockState block;
    public Predicate<BlockState> replace = s -> s.is(Blocks.STONE);

    public Predicate<LegacyBiome> canSpawn;

    private boolean ready;

    public MapGenBubble(Codec<CarverConfiguration> codec, boolean sand) {
        super(codec);
        this.sand = sand;
    }

    private synchronized void setup() {
        if (ready) return;
        if (!sand) {
            // oilBubble
            this.frequency = WorldConfig.oilSpawn;
            this.block = ModBlocks.ORE_OIL.get().defaultBlockState();
            // 1.20: Granit/Diorit/Andesit/Tiefenschiefer gehoeren zum "stone" der 1.7.10-Welt
            this.replace = s -> s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            setSize(8, 16);
        } else {
            // sandOilBubble
            this.frequency = 200;
            this.replace = s -> s.is(Blocks.SAND) || s.is(Blocks.RED_SAND);
            this.block = ModBlocks.ORE_OIL_SAND.get().defaultBlockState();
            this.canSpawn = biome -> !biome.canSpawnLightningBolt() && biome.temperature >= 1.5F;
            this.minY = 56;
            this.rangeY = 16;
            setSize(16, 48);
            this.fuzzy = true;
        }
        ready = true;
    }

    public void setSize(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    @Override
    protected void generate(ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeAccessor, RandomSource rand, int offsetX, int offsetZ, int chunkX, int chunkZ) {
        if (!ready) setup();
        if (frequency <= 0) return;

        int effecFreq = frequency;
        LegacyBiome biome = biomeAt(biomeAccessor, offsetX * 16, offsetZ * 16);
        if (biome.temperature >= 2 && biome.rainfall < 0.1) effecFreq /= 3;
        if (effecFreq <= 0) effecFreq = 1;

        if (rand.nextInt(effecFreq) == effecFreq - 1 && (canSpawn == null || canSpawn.test(biome))) {
            int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
            int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

            int yCoord = rand.nextInt(rangeY) + minY;

            double radius = rand.nextInt(maxSize - minSize) + minSize;
            double radiusSqr = (radius * radius) / 2; // die erste OilBubble-Fassung teilte das Quadrat schon durch 2

            int yMin = Math.max(1, Mth.floor(yCoord - radius));
            int yMax = Math.min(127, Mth.ceil(yCoord + radius));

            for (int bx = 15; bx >= 0; bx--) // bx, bz: Koordinate des veraenderten Blocks relativ zum Chunk-Ursprung
            for (int bz = 15; bz >= 0; bz--)
            for (int by = yMin; by < yMax; by++) {
                if (replace.test(get(chunk, bx, by, bz))) {
                    // x, z: Koordinaten relativ zum Ursprung des virtuellen Ziel-Chunks
                    int x = xCoord + bx;
                    int z = zCoord + bz;
                    int y = yCoord - by;

                    double rSqr = x * x + z * z + y * y * 3;
                    if (fuzzy) rSqr -= rand.nextDouble() * radiusSqr / 3;
                    if (rSqr < radiusSqr) {
                        set(chunk, bx, by, bz, block);
                    }
                }
            }

            if (rand.nextInt(1) == 0) {
                addSurfaceSpot(chunk, rand, xCoord, zCoord);
            }
        }
    }

    protected void addSurfaceSpot(ChunkAccess chunk, RandomSource rand, int xCoord, int zCoord) {

        int deadMetaCount = 5; // EnumDeadPlantType
        int spotCount = 150;
        int spotWidth = 7;

        // Oelflecken-Schaden
        for (int i = 0; i < spotCount; i++) {
            int offX = (int) (rand.nextGaussian() * spotWidth);
            int offZ = (int) (rand.nextGaussian() * spotWidth);
            int rx = offX - xCoord;
            int rz = offZ - zCoord;

            if (rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
                // Bodenhoehe suchen
                for (int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, rx, rz) + 1; y >= chunk.getMinBuildHeight(); y--) {
                    BlockState top = get(chunk, rx, y, rz);

                    if (present(top) && opaque(top)) {
                        for (int oy = 1; oy > -3; oy--) {
                            int subY = y + oy;
                            BlockState sub = get(chunk, rx, subY, rz);

                            int distSq = offX * offX + offZ * offZ;
                            boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

                            if (sub.is(Blocks.GRASS_BLOCK) || sub.is(Blocks.DIRT)) {
                                set(chunk, rx, subY, rz, inner ? MB.dirt_oily.state(0) : MB.dirt_dead.state(0));

                                // passiert VOR der Dekoration, es gibt also noch keine Pflanzen - stattdessen neue setzen
                                if (!inner && oy == 0 && rand.nextInt(20) == 0) {
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

        // und jetzt das Loch(tm)
        for (int i = 1; i < 6; i++) {
            ForgeDirection dir = ForgeDirection.getOrientation(i);
            int x = dir.offsetX - xCoord;
            int z = dir.offsetZ - zCoord;

            if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                int solids = 0;

                for (int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1; y >= chunk.getMinBuildHeight(); y--) {
                    BlockState b = get(chunk, x, y, z);
                    if (!present(b)) continue;

                    if (!b.getFluidState().isEmpty()) break;

                    if (opaque(b)) {
                        solids++;

                        // bricht bei Loechern und Unebenheiten etwas, ist aber egal
                        if (i > 1) {
                            set(chunk, x, y, z, MB.stone_cracked.state(0));
                            if (solids >= 4) break;
                        } else {
                            if (solids < 3) set(chunk, x, y, z, Blocks.AIR.defaultBlockState());
                            if (solids == 3) set(chunk, x, y, z, MB.oil_spill.state(0));
                            if (solids > 3 && solids < 7) set(chunk, x, y, z, MB.stone_cracked.state(0));
                            if (solids >= 7) break;
                        }
                    }
                }
            }
        }
    }
}
