package com.hbm_m.world.feature;

import java.util.Random;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.LMaterial;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.world.feature.OreCave}: Erzhoehlen-Schicht (Schwefel mit Saeure, Asbest) nach 2D-Rauschen;
 * Erz nur an Luftflaechen, dazu Tropfsteine des Erzes in den Hohlraeumen.
 */
public class OreCave {

    private NoiseGeneratorPerlin noise;
    private long lastSeed;
    private final MetaBlock ore;
    /** Pauschaler Abzug vom Rauschwert; groesser = seltenere Schichten. */
    private double threshold = 2D;
    /** Multiplikator fuer den Rest nach dem Abzug; groesser = welliger. */
    private int rangeMult = 3;
    /** Hoechste Reichweite nach dem Multiplizieren; darueber wird von (maxRange * 2) abgezogen. Groesser = dicker. */
    private int maxRange = 4;
    /** Mittlere y-Hoehe der Schicht. */
    private int yLevel = 30;
    private LB fluid;
    int dim = 0;

    public OreCave(LB ore) {
        this(ore, 0);
    }

    public OreCave(LB ore, int meta) {
        this.ore = new MetaBlock(ore, meta);
    }

    public OreCave setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public OreCave setRangeMult(int rangeMult) {
        this.rangeMult = rangeMult;
        return this;
    }

    public OreCave setMaxRange(int maxRange) {
        this.maxRange = maxRange;
        return this;
    }

    public OreCave setYLevel(int yLevel) {
        this.yLevel = yLevel;
        return this;
    }

    public OreCave withFluid(LB fluid) {
        this.fluid = fluid;
        return this;
    }

    public OreCave setDimension(int dim) {
        this.dim = dim;
        return this;
    }

    /** {@code ore.getID()} des Originals (numerische Block-ID, je Welt verschieden) - im Port stabil aus dem Namen. */
    private int oreId() {
        return ore.block.name.hashCode() & 0xFFF;
    }

    public void onDecorate(LevelAccessor world, Random rand, long seed, int cX, int cZ) {

        NoiseGeneratorPerlin noise;
        synchronized (this) {
            if (this.noise == null || lastSeed != seed) {
                this.noise = new NoiseGeneratorPerlin(seed + (oreId() * 31L) + yLevel, 2);
                lastSeed = seed;
            }
            noise = this.noise;
        }

        double scale = 0.01D;

        for (int x = cX + 8; x < cX + 24; x++) {
            for (int z = cZ + 8; z < cZ + 24; z++) {

                double n = noise.getValue(x * scale, z * scale);

                if (n > threshold) {
                    int range = (int) ((n - threshold) * rangeMult);

                    if (range > maxRange)
                        range = (maxRange * 2) - range;

                    if (range < 0)
                        continue;

                    for (int y = yLevel - range; y <= yLevel + range; y++) {
                        LB genTarget = L.getBlock(world, x, y, z);

                        if (genTarget.isNormalCube() && (genTarget.getMaterial() == LMaterial.rock || genTarget.getMaterial() == LMaterial.ground) && L.isReplaceableOreGen(world, x, y, z, VB.stone)) {

                            boolean shouldGen = false;
                            boolean canGenFluid = rand.nextBoolean();

                            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                                LB neighbor = L.getBlock(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
                                if (neighbor.getMaterial() == LMaterial.air || isStalagmite(neighbor)) {
                                    shouldGen = true;
                                }

                                if (shouldGen && (fluid == null || !canGenFluid))
                                    break;

                                if (fluid != null) {
                                    switch (dir) {
                                        case UP: if (neighbor.getMaterial() != LMaterial.air && !isStalagmite(neighbor)) canGenFluid = false; break;
                                        case DOWN: if (!neighbor.isNormalCube()) canGenFluid = false; break;
                                        case NORTH:
                                        case SOUTH:
                                        case EAST:
                                        case WEST:
                                            if (!neighbor.isNormalCube() && neighbor != fluid) canGenFluid = false; break;
                                        default: break;
                                    }
                                }
                            }

                            if (fluid != null && canGenFluid) {
                                L.setBlock(world, x, y, z, fluid, 0, 2);
                                L.setBlock(world, x, y - 1, z, ore.block, ore.meta, 2);

                                for (int i = 2; i < 6; i++) {
                                    ForgeDirection dir = ForgeDirection.getOrientation(i);
                                    int clX = x + dir.offsetX;
                                    int clZ = z + dir.offsetZ;
                                    LB neighbor = L.getBlock(world, clX, y, clZ);

                                    if (neighbor.isNormalCube())
                                        L.setBlock(world, clX, y, clZ, ore.block, ore.meta, 2);
                                }

                            } else if (shouldGen) {
                                L.setBlock(world, x, y, z, ore.block, ore.meta, 2);
                            }

                        } else {

                            if ((genTarget.getMaterial() == LMaterial.air || !genTarget.isNormalCube()) && rand.nextInt(5) == 0 && !genTarget.getMaterial().isLiquid()) {

                                // BlockStalagmite.getMetaFromResource(meta) == meta
                                if (MB.stalactite.canPlaceBlockAt(world, x, y, z)) {
                                    L.setBlock(world, x, y, z, MB.stalactite, ore.meta, 2);
                                } else {
                                    if (MB.stalagmite.canPlaceBlockAt(world, x, y, z)) {
                                        L.setBlock(world, x, y, z, MB.stalagmite, ore.meta, 2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isStalagmite(LB lb) {
        return lb == MB.stalactite || lb == MB.stalagmite || lb.name.startsWith("hbm_m:stalactite") || lb.name.startsWith("hbm_m:stalagmite");
    }
}
