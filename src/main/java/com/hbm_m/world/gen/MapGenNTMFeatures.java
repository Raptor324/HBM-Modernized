package com.hbm_m.world.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.config.GeneralConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.world.gen.component.CivilianFeatures.NTMHouse1;
import com.hbm_m.world.gen.component.CivilianFeatures.NTMHouse2;
import com.hbm_m.world.gen.component.CivilianFeatures.NTMLab1;
import com.hbm_m.world.gen.component.CivilianFeatures.NTMLab2;
import com.hbm_m.world.gen.component.CivilianFeatures.RuralHouse1;
import com.hbm_m.world.gen.component.Component;
import com.hbm_m.world.gen.component.OfficeFeatures.LargeOffice;
import com.hbm_m.world.gen.component.OfficeFeatures.LargeOfficeCorner;
import com.hbm_m.world.gen.component.SiloComponent;
import com.hbm_m.world.gen.nbt.SpawnCondition;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/**
 * 1:1 {@code com.hbm.world.gen.MapGenNTMFeatures} (Bauteil-Kleinstrukturen). Die eigene Gitter-Verteilung des
 * Originals ({@code canSpawnStructureAtCoords}) ist dort tot; gestartet wird ueber die SpawnCondition "features".
 */
public class MapGenNTMFeatures {

    public static class Start {

        public static List<StructurePiece> create(SpawnCondition.StartContext ctx) {
            Structure.GenerationContext gen = ctx.context();
            Random rand = ctx.rand();
            List<StructurePiece> components = new ArrayList<>();

            int i = (ctx.chunkX() << 4) + 8;
            int j = (ctx.chunkZ() << 4) + 8;

            // nur das Biom an der Chunk-Ecke
            int y = gen.chunkGenerator().getBaseHeight(i, j, Heightmap.Types.WORLD_SURFACE_WG, gen.heightAccessor(), gen.randomState());
            LegacyBiome biome = LegacyBiome.of(gen.biomeSource().getNoiseBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(y), QuartPos.fromBlock(j), gen.randomState().sampler()));

            if (biome.heightVariation <= 0.25F && rand.nextInt(10) == 0) { // flache Biome
                components.add(new SiloComponent(rand, i, j));
            } else if (biome.temperature >= 1.0 && biome.rainfall == 0 && biome.kind != LegacyBiome.Kind.MESA) { // Wueste & Savanne
                if (rand.nextBoolean()) {
                    components.add(new NTMHouse1(rand, i, j));
                } else {
                    components.add(new NTMHouse2(rand, i, j));
                }
            } else { // alles andere
                switch (rand.nextInt(6)) {
                    case 0: components.add(new NTMLab2(rand, i, j)); break;
                    case 1: components.add(new NTMLab1(rand, i, j)); break;
                    case 2: components.add(new LargeOffice(rand, i, j)); break;
                    case 3: components.add(new LargeOfficeCorner(rand, i, j)); break;
                    case 4:
                    case 5: components.add(new RuralHouse1(rand, i, j)); break;
                }
            }

            // Port: Hoehe beim Zusammenbau statt beim ersten Chunk-Bau
            for (StructurePiece piece : components) {
                if (piece instanceof Component c) c.computeHeight(gen);
            }

            if (GeneralConfig.enableDebugMode) {
                MainRegistry.LOGGER.info("[Debug] StructureStart at {}, 64, {} Components: {}", i, j, components);
            }

            return components;
        }
    }
}
