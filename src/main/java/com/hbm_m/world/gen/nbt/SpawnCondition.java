package com.hbm_m.world.gen.nbt;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.world.gen.LegacyBiome;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/** 1:1 {@code com.hbm.world.gen.nbt.SpawnCondition}: Bedingungen und Aufbau einer Weltgen-Struktur. */
public class SpawnCondition {

    public final String name;

    /** Gesetzt: ein einzelnes Jigsaw-Teil (Einzelstrukturen). */
    public JigsawPiece structure;

    /** Gesetzt: eine Bauteil-Struktur ohne .nbt (Original {@code StructureStart}-Fabrik). */
    public Function<StartContext, List<? extends StructurePiece>> start;

    /** Gesetzt: ersetzt die normale Platzsuche (feste Koordinaten oder Sonderregeln). */
    public Predicate<WorldCoordinate> checkCoordinates;

    /** Darf im aktuellen Biom erscheinen. */
    public Predicate<LegacyBiome> canSpawn;

    /** Haeufigkeit relativ zu den anderen Strukturen. */
    public int spawnWeight = 1;

    /** Benannte Jigsaw-Pools, auf die die Jigsaw-Bloecke der Struktur verweisen. */
    public Map<String, JigsawPool> pools;

    /** Name des Startpools (muss in {@link #pools} stehen). */
    public String startPool;

    /**
     * Hoechstzahl an Teilen; danach nur noch Ersatzteile.
     * Hartes Limit von 1024 Teilen gegen endlose Generierung, auch bei Pflichtteilen!
     */
    public int sizeLimit = 8;

    /** Wie weit die Struktur horizontal vom Mittelpunkt reichen darf, hoechstens 128. */
    public int rangeLimit = 128;

    /** Hoehengrenzen fuer den Start (U-Boote unter Wasser, Bunker unter der Erde ...). */
    public int minHeight = 1;

    /** @see #minHeight */
    public int maxHeight = 128;

    protected SpawnCondition(int weight, Predicate<LegacyBiome> predicate) {
        name = null;
        spawnWeight = weight;
        canSpawn = predicate;
    }

    public SpawnCondition(String name) {
        this.name = name;
    }

    // Darf im aktuellen Biom erscheinen
    protected boolean isValid(LegacyBiome biome) {
        if (canSpawn == null) return true;
        return canSpawn.test(biome);
    }

    public JigsawPool getPool(String name) {
        JigsawPool pool = pools.get(name);
        return pool != null ? pool.copy() : null;
    }

    /** Baut alle Pools in Reihen auf (nur mit Struktur-Debug, nie waehrend der Generierung). */
    public void buildAll(Level world, int x, int y, int z) {
        if (!ModClothConfig.get().structureDebug) return;

        int padding = 5;
        int oz = 0;

        for (JigsawPool pool : pools.values()) {
            int highestWidth = 0;
            int ox = 0;

            for (JigsawPool.Entry entry : pool.pieces) {
                NBTStructure structure = entry.piece().structure;
                structure.build(world, x + ox + (structure.getSizeX() / 2), y, z + oz + (structure.getSizeZ() / 2));

                ox += structure.getSizeX() + padding;
                highestWidth = Math.max(highestWidth, structure.getSizeZ());
            }

            oz += highestWidth + padding;
        }
    }

    /** Informationen zum aktuellen Strukturchunk; fuer konsistentes Seeding den enthaltenen Zufall nutzen! */
    public static class WorldCoordinate {

        public final Structure.GenerationContext context;
        public final ChunkPos coords;
        public final Random rand;

        protected WorldCoordinate(Structure.GenerationContext context, ChunkPos coords, Random rand) {
            this.context = context;
            this.coords = coords;
            this.rand = rand;
        }
    }

    /** Argumente der {@link #start}-Fabrik (Original {@code Quartet<World, Random, chunkX, chunkZ>}). */
    public record StartContext(Structure.GenerationContext context, Random rand, int chunkX, int chunkZ) { }
}
