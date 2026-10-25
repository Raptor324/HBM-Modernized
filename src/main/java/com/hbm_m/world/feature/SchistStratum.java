package com.hbm_m.world.feature;

import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.LMaterial;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.NoiseGeneratorPerlin;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.world.feature.SchistStratum}: welliger Gneisschiefer-Streifen um y 30 nach 2D-Rauschen
 * (Original im {@code DecorateBiomeEvent.Pre}, Bereich wie im Original um 8 Bloecke versetzt).
 */
public class SchistStratum {

    private NoiseGeneratorPerlin noise;
    private long lastSeed;

    public void onDecorate(LevelAccessor world, long seed, int cX, int cZ) {

        NoiseGeneratorPerlin noise;
        synchronized (this) {
            if (this.noise == null || lastSeed != seed) {
                this.noise = new NoiseGeneratorPerlin(seed, 4);
                lastSeed = seed;
            }
            noise = this.noise;
        }

        double scale = 0.01D;
        int threshold = 5;

        for (int x = cX + 8; x < cX + 24; x++) {
            for (int z = cZ + 8; z < cZ + 24; z++) {

                double n = noise.getValue(x * scale, z * scale);

                if (n > threshold) {
                    int range = (int) ((n - threshold) * 3);

                    if (range > 4)
                        range = 8 - range;

                    if (range < 0)
                        continue;

                    for (int y = 30 - range; y <= 30 + range; y++) {

                        LB target = L.getBlock(world, x, y, z);

                        if (target.isNormalCube() && target.getMaterial() == LMaterial.rock && L.isReplaceableOreGen(world, x, y, z, VB.stone)) {
                            L.setBlock(world, x, y, z, MB.stone_gneiss, 0, 2);
                        }
                    }
                }
            }
        }
    }
}
