package com.hbm_m.worldgen;

import java.util.Random;

import com.hbm_m.config.WorldConfig;
import com.hbm_m.lib.HbmWorldGen;
import com.hbm_m.world.feature.OreCave;
import com.hbm_m.world.feature.OreLayer3D;
import com.hbm_m.world.feature.SchistStratum;
import com.hbm_m.world.gen.legacy.MB;
import com.mojang.serialization.Codec;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * R9: Einhaengepunkte der 1.7.10-Weltgenerierung als 1.20-Merkmale (einmal pro Chunk):
 * <ul>
 * <li>{@link #strata}: die Ereignis-Generatoren aus {@code MainRegistry} ({@code DecorateBiomeEvent.Pre}) -
 * Gneisschiefer, Schwefel-/Asbesthoehlen, Haematit/Bauxit/Malachit-Lagen (in dieser Reihenfolge registriert);</li>
 * <li>{@link #worldgen}: {@link HbmWorldGen} ({@code IWorldGenerator}, nach der Dekoration).</li>
 * </ul>
 */
public class HbmWorldGenFeature extends Feature<NoneFeatureConfiguration> {

    private final boolean strata;

    private static final HbmWorldGen WORLD_GEN = new HbmWorldGen();

    // Original MainRegistry: SchistStratum, OreCave (Schwefel, Asbest), OreLayer3D (Haematit, Bauxit, Malachit)
    private static final SchistStratum SCHIST = new SchistStratum();
    private static OreCave sulfurCave;
    private static OreCave asbestosCave;
    private static OreLayer3D hematite;
    private static OreLayer3D bauxite;
    private static OreLayer3D malachite;

    private static synchronized void initStrata() {
        if (sulfurCave != null) return;
        // EnumStoneType: SULFUR 0, ASBESTOS 1, HEMATITE 2, MALACHITE 3, LIMESTONE 4, BAUXITE 5
        sulfurCave = new OreCave(MB.stone_resource, 0).setThreshold(1.5D).setRangeMult(20).setYLevel(30).setMaxRange(20).withFluid(MB.sulfuric_acid_block);
        asbestosCave = new OreCave(MB.stone_resource, 1).setThreshold(1.75D).setRangeMult(20).setYLevel(25).setMaxRange(20);
        hematite = new OreLayer3D(MB.stone_resource, 2).setScaleH(0.04D).setScaleV(0.25D).setThreshold(230);
        bauxite = new OreLayer3D(MB.stone_resource, 5).setScaleH(0.03D).setScaleV(0.15D).setThreshold(300);
        malachite = new OreLayer3D(MB.stone_resource, 3).setScaleH(0.1D).setScaleV(0.15D).setThreshold(275);
    }

    public HbmWorldGenFeature(Codec<NoneFeatureConfiguration> codec, boolean strata) {
        super(codec);
        this.strata = strata;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        Random rand = new Random(context.random().nextLong());

        if (strata) {
            if (world.getLevel().dimension() != Level.OVERWORLD) return false;
            initStrata();
            long seed = world.getSeed();
            int cX = chunkX * 16;
            int cZ = chunkZ * 16;
            SCHIST.onDecorate(world, seed, cX, cZ);
            if (WorldConfig.enableSulfurCave) sulfurCave.onDecorate(world, rand, seed, cX, cZ);
            if (WorldConfig.enableAsbestosCave) asbestosCave.onDecorate(world, rand, seed, cX, cZ);
            if (WorldConfig.enableHematite) hematite.onDecorate(world, seed, cX, cZ);
            if (WorldConfig.enableBauxite) bauxite.onDecorate(world, seed, cX, cZ);
            if (WorldConfig.enableMalachite) malachite.onDecorate(world, seed, cX, cZ);
            return true;
        }

        int dim;
        if (world.getLevel().dimension() == Level.OVERWORLD) dim = 0;
        else if (world.getLevel().dimension() == Level.NETHER) dim = -1;
        else if (world.getLevel().dimension() == Level.END) dim = 1;
        else dim = 2;

        WORLD_GEN.generate(rand, chunkX, chunkZ, world, dim);
        return true;
    }
}
