package com.hbm_m.world.gen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.hbm_m.config.WorldConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.world.dungeon.AncientTomb;
import com.hbm_m.world.dungeon.DesertAtom001;
import com.hbm_m.world.dungeon.Spaceship;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.gen.legacy.VirtualWorld;
import com.hbm_m.world.generator.CellularDungeonFactory;
import com.hbm_m.world.generator.JungleDungeon;
import com.hbm_m.world.generator.TimedGenerator;
import com.hbm_m.worldgen.ModWorldGen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Port: Die grossen Bauwerke aus {@code HbmWorldGen.generateSurface} (Wuesten-Kraftwerk {@code DesertAtom001},
 * Raumschiff {@code Spaceship}, Dschungelverlies {@code CellularDungeonFactory.jungle}, Pyramide {@code AncientTomb})
 * passen nicht in das 3x3-Chunkfenster der 1.20-Merkmale. Sie werden je Chunk mit derselben Wahrscheinlichkeit und
 * denselben Biombedingungen wie im Original ausgewuerfelt, einmal vollstaendig in einen {@link VirtualWorld}-Puffer
 * gebaut und anschliessend chunkweise in die Welt uebertragen.
 */
public class LegacyDungeonStructure extends Structure {

    public enum Kind {
        DESERT_ATOM("desert_atom", 1),
        SPACESHIP("spaceship", 2),
        JUNGLE_DUNGEON("jungle_dungeon", 3),
        PYRAMID("pyramid", 4);

        public final String id;
        final long salt;

        Kind(String id, long salt) {
            this.id = id;
            this.salt = salt * 0x9E3779B97F4A7C15L;
        }

        static Kind of(String id) {
            for (Kind k : values()) if (k.id.equals(id)) return k;
            throw new IllegalArgumentException("Unbekanntes Bauwerk " + id);
        }

        /** WorldConfig-Haeufigkeit (1 in n Chunks). */
        int frequency() {
            switch (this) {
                case DESERT_ATOM: return WorldConfig.atomStructure;
                case SPACESHIP: return WorldConfig.spaceshipStructure;
                case JUNGLE_DUNGEON: return WorldConfig.jungleStructure;
                default: return WorldConfig.pyramidStructure;
            }
        }

        /** Biombedingung wie in {@code HbmWorldGen.generateSurface}. */
        boolean canSpawn(LegacyBiome biome) {
            switch (this) {
                case DESERT_ATOM: return !biome.canSpawnLightningBolt() && biome.temperature >= 1.5F;
                case SPACESHIP: return true;
                case JUNGLE_DUNGEON: return biome.is("jungle") || biome.is("jungleEdge") || biome.is("jungleHills");
                default: return biome.temperature >= 2.0F && !biome.canSpawnLightningBolt();
            }
        }
    }

    //? if < 1.21.1 {
    public static final Codec<LegacyDungeonStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    //?} else {
    /*public static final com.mojang.serialization.MapCodec<LegacyDungeonStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    *///?}
            settingsCodec(instance),
            Codec.STRING.fieldOf("dungeon").forGetter(s -> s.kind.id)
    ).apply(instance, LegacyDungeonStructure::new));

    public final Kind kind;

    public LegacyDungeonStructure(StructureSettings settings, String dungeon) {
        super(settings);
        this.kind = Kind.of(dungeon);
    }

    @Override
    public StructureType<?> type() {
        return ModWorldGen.LEGACY_DUNGEON_TYPE.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int freq = kind.frequency();
        if (freq <= 0) return Optional.empty();
        // Original: nur im enableDungeons-Block (0 = aus; der Weltschalter "Strukturen" greift ueber das Strukturen-System)
        if (com.hbm_m.config.GeneralConfig.enableDungeons == 0) return Optional.empty();

        ChunkPos chunk = ctx.chunkPos();
        Random rand = new Random(ctx.seed() ^ ((long) chunk.x * 341873128712L + (long) chunk.z * 132897987541L) ^ kind.salt);

        if (rand.nextInt(freq) != 0) return Optional.empty();

        int i = chunk.getMinBlockX();
        int j = chunk.getMinBlockZ();

        // Biom wie HbmWorldGen.biomeAt(i, j)
        int h = ctx.chunkGenerator().getBaseHeight(i, j, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        LegacyBiome biome = LegacyBiome.of(ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(h), QuartPos.fromBlock(j), ctx.randomState().sampler()));
        if (!kind.canSpawn(biome)) return Optional.empty();

        int x = i + rand.nextInt(16);
        int z = j + rand.nextInt(16);
        long buildSeed = rand.nextLong();

        VirtualWorld vw = buffer(kind, x, z, buildSeed, ctx.seed(), ctx.chunkGenerator(), ctx.randomState(), ctx.heightAccessor());
        BoundingBox bb = vw.bounds();
        if (bb == null) return Optional.empty(); // Platz ungeeignet (LocationIsValidSpawn)

        return Optional.of(new GenerationStub(new BlockPos(x, bb.minY(), z), builder -> builder.addPiece(new Piece(kind, x, z, buildSeed, bb))));
    }

    // ================================ Puffer ================================

    /** Zuletzt gebaute Puffer; fehlt einer (z.B. nach Neustart), wird er aus demselben Seed neu gebaut. */
    private static final Map<String, VirtualWorld> CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, VirtualWorld> eldest) {
            return size() > 8;
        }
    };

    static VirtualWorld buffer(Kind kind, int x, int z, long buildSeed, long worldSeed, ChunkGenerator gen, RandomState rs, LevelHeightAccessor height) {
        String key = kind.id + "/" + x + "/" + z + "/" + buildSeed + "/" + worldSeed;
        synchronized (CACHE) {
            VirtualWorld vw = CACHE.get(key);
            if (vw == null) {
                vw = new VirtualWorld(gen, rs, height, worldSeed, new Random(buildSeed));
                build(kind, vw, x, z);
                CACHE.put(key, vw);
            }
            return vw;
        }
    }

    private static void build(Kind kind, VirtualWorld vw, int x, int z) {
        Random rand = vw.rand;
        try {
            L.runVirtual(vw, () -> {
                switch (kind) {
                    case DESERT_ATOM: {
                        int y = L.getHeightValue(null, x, z);
                        new DesertAtom001().generate(null, rand, x, y, z);
                        break;
                    }
                    case SPACESHIP: {
                        int y = L.getHeightValue(null, x, z);
                        new Spaceship().generate(null, rand, x, y, z);
                        break;
                    }
                    case JUNGLE_DUNGEON: {
                        // ein Verlies-Objekt fuer alle drei Ebenen wie im Original (gemeinsamer Zustand hasHole/cells)
                        JungleDungeon jungle = CellularDungeonFactory.jungle();
                        try {
                            jungle.generate(null, x, 20, z, rand);
                            jungle.generate(null, x, 24, z, rand);
                            jungle.generate(null, x, 28, z, rand);

                            int y = L.getHeightValue(null, x, z);

                            for (int f = 0; f < 3; f++)
                                L.setBlock(null, x, y + f, z, MB.deco_titanium);
                            L.setBlock(null, x, y + 3, z, VB.redstone_block);

                            TimedGenerator.runAll();
                        } finally {
                            TimedGenerator.clear();
                        }
                        break;
                    }
                    case PYRAMID: {
                        int y = L.getHeightValue(null, x, z);
                        new AncientTomb().build(null, rand, x, y, z);
                        break;
                    }
                }
            });
        } catch (Exception ex) {
            MainRegistry.LOGGER.error("[LegacyDungeon] Bau von {} bei {} {} fehlgeschlagen", kind.id, x, z, ex);
            vw.blocks.clear();
            vw.blockEntities.clear();
        }
    }

    // ================================ Bauteil ================================

    public static class Piece extends StructurePiece {

        private final Kind kind;
        private final int x;
        private final int z;
        private final long buildSeed;

        public Piece(Kind kind, int x, int z, long buildSeed, BoundingBox box) {
            super(ModWorldGen.LEGACY_DUNGEON_PIECE.get(), 0, box);
            this.kind = kind;
            this.x = x;
            this.z = z;
            this.buildSeed = buildSeed;
        }

        public Piece(CompoundTag tag) {
            super(ModWorldGen.LEGACY_DUNGEON_PIECE.get(), tag);
            this.kind = Kind.of(tag.getString("dungeon"));
            this.x = tag.getInt("ox");
            this.z = tag.getInt("oz");
            this.buildSeed = tag.getLong("seed");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
            tag.putString("dungeon", kind.id);
            tag.putInt("ox", x);
            tag.putInt("oz", z);
            tag.putLong("seed", buildSeed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator gen, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            RandomState rs = level.getLevel().getChunkSource().randomState();
            VirtualWorld vw = buffer(kind, x, z, buildSeed, level.getSeed(), gen, rs, level);

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (long key : vw.inChunk(chunkPos.x, chunkPos.z)) {
                pos.set(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
                if (!box.isInside(pos)) continue;

                BlockState state = vw.blocks.get(key);
                level.setBlock(pos, state, 2);

                BlockEntity virtual = vw.blockEntities.get(key);
                if (virtual != null) {
                    BlockEntity real = level.getBlockEntity(pos);
                    if (real != null) {
                        try {
                            //? if < 1.21.1 {
                            real.load(virtual.saveWithoutMetadata());
                            //?} else {
                            /*com.hbm_m.platform.PlatformHooks.loadBlockEntityTag(real, virtual.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
                            *///?}
                            real.setChanged();
                        } catch (Exception ex) {
                            MainRegistry.LOGGER.debug("[LegacyDungeon] Blockeinheit bei {} nicht uebertragbar", pos, ex);
                        }
                    }
                }
            }
        }
    }

    /** Registriereintrag fuer {@link StructureType}. */
    public static StructureType<LegacyDungeonStructure> structureType() {
        return () -> CODEC;
    }
}
