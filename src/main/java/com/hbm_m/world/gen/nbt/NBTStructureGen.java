package com.hbm_m.world.gen.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.hbm_m.config.StructureConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.NTMWorldGenerator;
import com.hbm_m.world.gen.nbt.NBTStructure.JigsawConnection;
import com.hbm_m.worldgen.ModWorldGen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * 1:1 Weltgen-Teil von {@code com.hbm.world.gen.nbt.NBTStructure}: {@code Component} (Bauteil), {@code Start}
 * (Jigsaw-Zusammenbau) und {@code GenStructure} (Platzwahl/Gewichtung) - abgebildet auf eine einzige 1.20-Struktur
 * {@code hbm_m:ntm_structures} mit eigener Platzierung {@code hbm_m:ntm_grid}.
 *
 * <p>Ablauf wie im Original: Gitter aus {@code structureMaxChunks} Chunks, pro Zelle ein zufaelliger Chunk
 * ({@code structureMaxChunks - structureMinChunks}), dort Biom in Chunkmitte, gewichtete Wahl aller fuer das Biom
 * gueltigen SpawnConditions (inkl. Null-Gewichte), dann Aufbau. Der Zufall wird wie im Original geseedet.</p>
 *
 * <p>Abweichung durch 1.20: Die Hoehe eines Teils bestimmt das Original erst beim Bauen im ersten Chunk (Mittel der
 * Gelaendehoehe dieses Chunkausschnitts). In 1.20 werden Chunks parallel und ohne gemeinsamen Zustand bebaut,
 * deshalb wird die Hoehe schon beim Zusammenbau aus der Gelaende-Hoehenkarte ueber die ganze Grundflaeche
 * gemittelt.</p>
 */
public final class NBTStructureGen {

    private NBTStructureGen() { }

    private static final int DIMENSION = 0;

    // ============================================================================================================
    // Platzierung
    // ============================================================================================================

    /** Original {@code GenStructure.getSpawnAtCoords} Gitterteil als Vanilla-Platzierung. */
    public static class GridPlacement extends StructurePlacement {

        public static final Codec<GridPlacement> CODEC = RecordCodecBuilder.create(instance ->
                placementCodec(instance).apply(instance, GridPlacement::new));

        public GridPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt, Optional<ExclusionZone> exclusionZone) {
            super(locateOffset, method, frequency, salt, exclusionZone);
        }

        @Override
        protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
            return isGridChunk(state.getLevelSeed(), chunkX, chunkZ, new Random());
        }

        @Override
        public StructurePlacementType<?> type() {
            return ModWorldGen.NTM_GRID_PLACEMENT.get();
        }
    }

    /** Seedet {@code rand} wie das Original und prueft, ob dies der Strukturchunk seiner Gitterzelle ist. */
    static boolean isGridChunk(long seed, int chunkX, int chunkZ, Random rand) {
        if (StructureConfig.enableStructures == 0) return false;

        int max = StructureConfig.structureMaxChunks;
        int min = StructureConfig.structureMinChunks;

        int x = chunkX;
        int z = chunkZ;

        if (x < 0) x -= max - 1;
        if (z < 0) z -= max - 1;

        x /= max;
        z /= max;
        rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L + seed + (long) 996996996 - DIMENSION);
        x *= max;
        z *= max;
        x += rand.nextInt(max - min);
        z += rand.nextInt(max - min);

        return chunkX == x && chunkZ == z;
    }

    // ============================================================================================================
    // Struktur (GenStructure)
    // ============================================================================================================

    public static class GenStructure extends Structure {

        public static final Codec<GenStructure> CODEC = simpleCodec(GenStructure::new);

        private static final Map<LegacyBiome, WeightedSpawnList> validBiomeCache = new HashMap<>();

        public GenStructure(StructureSettings settings) {
            super(settings);
        }

        @Override
        public StructureType<?> type() {
            return ModWorldGen.NTM_STRUCTURE_TYPE.get();
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            NTMWorldGenerator.init();

            ChunkPos chunk = ctx.chunkPos();
            Random rand = new Random();

            // Sonderplaetze (Original customSpawnMap) - Zufall wie MapGenStructure je Chunk
            List<SpawnCondition> custom = NBTStructure.customSpawnMap.get(DIMENSION);
            if (custom != null) {
                Random crand = new Random(ctx.seed());
                long l = crand.nextLong();
                long i1 = crand.nextLong();
                crand.setSeed((long) chunk.x * l ^ (long) chunk.z * i1 ^ ctx.seed());
                crand.nextInt();
                SpawnCondition.WorldCoordinate coords = new SpawnCondition.WorldCoordinate(ctx, chunk, crand);
                for (SpawnCondition spawn : custom) {
                    if ((spawn.pools != null || spawn.structure != null) && spawn.checkCoordinates.test(coords)) {
                        return buildStub(ctx, spawn, crand);
                    }
                }
            }

            if (!isGridChunk(ctx.seed(), chunk.x, chunk.z, rand)) return Optional.empty();

            int cx = chunk.getMinBlockX() + 8;
            int cz = chunk.getMinBlockZ() + 8;
            int cy = ctx.chunkGenerator().getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            Holder<Biome> biomeHolder = ctx.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(cy), QuartPos.fromBlock(cz), ctx.randomState().sampler());
            LegacyBiome biome = LegacyBiome.of(biomeHolder);

            SpawnCondition spawn = findSpawn(biome, rand);

            if (spawn != null && (spawn.pools != null || spawn.start != null || spawn.structure != null)) {
                return buildStub(ctx, spawn, rand);
            }

            return Optional.empty();
        }

        private static synchronized SpawnCondition findSpawn(LegacyBiome biome, Random rand) {
            WeightedSpawnList filteredList = validBiomeCache.get(biome);
            if (filteredList == null) {
                List<SpawnCondition> spawnList = NBTStructure.spawnMap.get(DIMENSION);

                filteredList = new WeightedSpawnList();
                if (spawnList != null) {
                    for (SpawnCondition spawn : spawnList) {
                        if (spawn.isValid(biome)) {
                            filteredList.add(spawn);
                            filteredList.totalWeight += spawn.spawnWeight;
                        }
                    }
                }

                validBiomeCache.put(biome, filteredList);
            }

            if (filteredList.totalWeight == 0) return null;

            int weight = rand.nextInt(filteredList.totalWeight);

            for (SpawnCondition spawn : filteredList) {
                weight -= spawn.spawnWeight;

                if (weight < 0) {
                    return spawn;
                }
            }

            return null;
        }

        private Optional<GenerationStub> buildStub(GenerationContext ctx, SpawnCondition spawn, Random rand) {
            ChunkPos chunk = ctx.chunkPos();
            List<StructurePiece> pieces = new ArrayList<>();

            if (spawn.start != null) {
                pieces.addAll(spawn.start.apply(new SpawnCondition.StartContext(ctx, rand, chunk.x, chunk.z)));
            } else {
                pieces.addAll(new Start(ctx, rand, spawn, chunk.x, chunk.z).components);
            }

            if (pieces.isEmpty()) return Optional.empty();

            BlockPos pos = new BlockPos(chunk.getMiddleBlockX(), pieces.get(0).getBoundingBox().minY(), chunk.getMiddleBlockZ());
            return Optional.of(new GenerationStub(pos, (StructurePiecesBuilder builder) -> {
                for (StructurePiece piece : pieces) builder.addPiece(piece);
            }));
        }
    }

    private static class WeightedSpawnList extends ArrayList<SpawnCondition> {
        int totalWeight = 0;
    }

    // ============================================================================================================
    // Bauteil (Component)
    // ============================================================================================================

    public static class Component extends StructurePiece {

        JigsawPiece piece;

        int minHeight = 1;
        int maxHeight = 128;

        boolean heightUpdated = false;

        int priority;

        String structureName;

        int coordBaseMode;

        private JigsawConnection connectedFrom;

        public Component(SpawnCondition spawn, JigsawPiece piece, Random rand, int x, int z) {
            this(spawn, piece, rand, x, 0, z, rand.nextInt(4));
        }

        public Component(SpawnCondition spawn, JigsawPiece piece, Random rand, int x, int y, int z, int coordBaseMode) {
            super(ModWorldGen.NBT_COMPONENT.get(), 0, makeBox(piece, x, y, z, coordBaseMode));
            this.coordBaseMode = coordBaseMode;
            this.piece = piece;
            this.minHeight = spawn.minHeight;
            this.maxHeight = spawn.maxHeight;
            this.structureName = spawn.name;
        }

        /** Laden aus dem Chunk (Original {@code func_143011_b}). */
        public Component(CompoundTag nbt) {
            super(ModWorldGen.NBT_COMPONENT.get(), nbt);
            NTMWorldGenerator.init();
            piece = JigsawPiece.get(nbt.getString("piece"));
            minHeight = nbt.getInt("min");
            maxHeight = nbt.getInt("max");
            heightUpdated = nbt.getBoolean("hasHeight");
            coordBaseMode = nbt.getInt("coordBaseMode");
            structureName = nbt.contains("structure") ? nbt.getString("structure") : null;
        }

        private static BoundingBox makeBox(JigsawPiece piece, int x, int y, int z, int coordBaseMode) {
            NBTStructure s = piece.structure;
            switch (coordBaseMode) {
                case 1:
                case 3:
                    return new BoundingBox(x, y, z, x + s.getSizeZ() - 1, y + s.getSizeY() - 1, z + s.getSizeX() - 1);
                default:
                    return new BoundingBox(x, y, z, x + s.getSizeX() - 1, y + s.getSizeY() - 1, z + s.getSizeZ() - 1);
            }
        }

        public Component connectedFrom(JigsawConnection connection) {
            this.connectedFrom = connection;
            return this;
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag nbt) {
            nbt.putString("piece", piece != null ? piece.name : "NULL");
            nbt.putInt("min", minHeight);
            nbt.putInt("max", maxHeight);
            nbt.putBoolean("hasHeight", heightUpdated);
            nbt.putInt("coordBaseMode", coordBaseMode);
            if (structureName != null) nbt.putString("structure", structureName);
        }

        @Override
        public void postProcess(WorldGenLevel world, StructureManager structureManager, ChunkGenerator generator, RandomSource rand, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
            if (piece == null) return;
            piece.structure.build(world, piece, boundingBox, box, coordBaseMode, structureName, rand);
        }

        public void offsetYHeight(int y) {
            this.move(0, y, 0);
            heightUpdated = true;
        }

        // eigene Drehung (Original ueberschreibt Mojangs spiegelnde Drehungen)
        protected int getXWithOffset(int x, int z) {
            return boundingBox.minX() + piece.structure.rotateX(x, z, coordBaseMode);
        }

        protected int getYWithOffset(int y) {
            return boundingBox.minY() + y;
        }

        protected int getZWithOffset(int x, int z) {
            return boundingBox.minZ() + piece.structure.rotateZ(x, z, coordBaseMode);
        }

        private ForgeDirection rotateDir(ForgeDirection dir) {
            if (dir == ForgeDirection.UP || dir == ForgeDirection.DOWN) return dir;
            switch (coordBaseMode) {
                default: return dir;
                case 1: return dir.getRotation(ForgeDirection.UP);
                case 2: return dir.getOpposite();
                case 3: return dir.getRotation(ForgeDirection.DOWN);
            }
        }

        private int getNextCoordBase(JigsawConnection fromConnection, JigsawConnection toConnection, Random rand) {
            if (fromConnection.dir == ForgeDirection.DOWN || fromConnection.dir == ForgeDirection.UP) {
                if (fromConnection.isRollable) return rand.nextInt(4);
                return coordBaseMode;
            }

            return directionOffsetToCoordBase(fromConnection.dir.getOpposite(), toConnection.dir);
        }

        private int directionOffsetToCoordBase(ForgeDirection from, ForgeDirection to) {
            for (int i = 0; i < 4; i++) {
                if (from == to) return (i + coordBaseMode) % 4;
                from = from.getRotation(ForgeDirection.DOWN);
            }
            return coordBaseMode;
        }

        protected boolean hasIntersectionIgnoringSelf(List<Component> components, BoundingBox box) {
            for (Component component : components) {
                if (component == this) continue;
                if (component.getBoundingBox() == null) continue;

                if (component.getBoundingBox().intersects(box)) return true;
            }

            return false;
        }

        protected boolean isInsideIgnoringSelf(List<Component> components, int x, int y, int z) {
            for (Component component : components) {
                if (component == this) continue;
                if (component.getBoundingBox() == null) continue;

                if (component.getBoundingBox().isInside(x, y, z)) return true;
            }

            return false;
        }
    }

    // ============================================================================================================
    // Zusammenbau (Start)
    // ============================================================================================================

    static class Start {

        final String name;
        final List<Component> components = new ArrayList<>();
        private final int chunkX, chunkZ;

        Start(Structure.GenerationContext ctx, Random rand, SpawnCondition spawn, int chunkX, int chunkZ) {
            this.name = spawn.name;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;

            int x = chunkX << 4;
            int z = chunkZ << 4;

            JigsawPiece startPiece = spawn.structure != null ? spawn.structure : spawn.pools.get(spawn.startPool).get(rand);

            Component startComponent = new Component(spawn, startPiece, rand, x, z);

            components.add(startComponent);

            List<Component> queuedComponents = new ArrayList<>();
            if (spawn.structure == null) queuedComponents.add(startComponent);

            Set<JigsawPiece> requiredPieces = findRequiredPieces(spawn);

            // alle Teile aufbauen, die erzeugt werden sollen
            while (!queuedComponents.isEmpty()) {
                queuedComponents.sort((a, b) -> b.priority - a.priority); // nach Platzierungsprioritaet absteigend
                int matchPriority = queuedComponents.get(0).priority;
                int max = 1;
                while (max < queuedComponents.size()) {
                    if (queuedComponents.get(max).priority != matchPriority) break;
                    max++;
                }

                final int i = rand.nextInt(max);
                Component fromComponent = queuedComponents.remove(i);

                if (fromComponent.piece.structure.getFromConnections() == null) continue;

                int distance = getDistanceTo(fromComponent.getBoundingBox());

                // ab der Groessengrenze nur noch Ersatzteile, ausser ein Pflichtteil fehlt
                // hartes Limit von 1024 Teilen gegen endlose Generierung
                boolean fallbacksOnly = requiredPieces.size() == 0 && (components.size() >= spawn.sizeLimit || distance >= spawn.rangeLimit) || components.size() > 1024;

                for (List<JigsawConnection> unshuffledList : fromComponent.piece.structure.getFromConnections()) {
                    List<JigsawConnection> connectionList = new ArrayList<>(unshuffledList);
                    Collections.shuffle(connectionList, rand);

                    for (JigsawConnection fromConnection : connectionList) {
                        if (fromComponent.connectedFrom == fromConnection) continue; // schon von hier angeschlossen

                        if (fallbacksOnly) {
                            JigsawPool pool = spawn.pools.get(fromConnection.poolName);
                            String fallback = pool != null ? pool.fallback : null;

                            if (fallback != null) {
                                Component fallbackComponent = buildNextComponent(rand, spawn, spawn.pools.get(fallback), fromComponent, fromConnection);
                                addComponent(fallbackComponent, fromConnection.placementPriority);
                            }

                            continue;
                        }

                        JigsawPool nextPool = spawn.getPool(fromConnection.poolName);
                        if (nextPool == null) {
                            MainRegistry.LOGGER.warn("[Jigsaw] Jigsaw block points to invalid pool: " + fromConnection.poolName);
                            continue;
                        }

                        Component nextComponent = null;

                        // zufaellig durch den Pool, bis ein Teil passt
                        while (nextPool.totalWeight > 0) {
                            nextComponent = buildNextComponent(rand, spawn, nextPool, fromComponent, fromConnection);
                            if (nextComponent != null && !fromComponent.hasIntersectionIgnoringSelf(components, nextComponent.getBoundingBox())) break;
                            nextComponent = null;
                        }

                        if (nextComponent != null) {
                            addComponent(nextComponent, fromConnection.placementPriority);
                            queuedComponents.add(nextComponent);

                            requiredPieces.remove(nextComponent.piece);
                        } else {
                            // nichts passt: Ersatzteil ohne Kollisionspruefung, ausser es stoesst direkt an ein
                            // anderes Teil (Rasteranordnungen)
                            if (nextPool.fallback != null) {
                                BlockPos checkPos = getConnectionTargetPosition(fromComponent, fromConnection);

                                if (!fromComponent.isInsideIgnoringSelf(components, checkPos.getX(), checkPos.getY(), checkPos.getZ())) {
                                    nextComponent = buildNextComponent(rand, spawn, spawn.pools.get(nextPool.fallback), fromComponent, fromConnection);
                                    addComponent(nextComponent, fromConnection.placementPriority); // nicht in die Warteschlange
                                }
                            }
                        }
                    }
                }
            }

            if (com.hbm_m.config.GeneralConfig.enableDebugMode) {
                MainRegistry.LOGGER.info("[Debug] Spawning NBT structure " + name + " with " + components.size() + " piece(s) at: " + chunkX * 16 + ", " + chunkZ * 16);
                StringBuilder componentList = new StringBuilder("[Debug] Components: ");
                for (Component component : this.components) {
                    componentList.append(component.piece.structure.getName()).append(" ");
                }
                MainRegistry.LOGGER.info(componentList.toString());
            }

            applyHeights(ctx);
        }

        /** Hoehen festlegen (Original: erst in addComponentParts). */
        private void applyHeights(Structure.GenerationContext ctx) {
            boolean isFlatWorld = ctx.chunkGenerator() instanceof FlatLevelSource;

            for (Component component : components) {
                if (component.piece.conformToTerrain || component.heightUpdated) continue;

                int averageHeight = getAverageHeight(ctx, component.getBoundingBox()) + component.piece.heightOffset;
                int y = isFlatWorld ? averageHeight : Mth.clamp(averageHeight, component.minHeight, component.maxHeight);

                if (!component.piece.alignToTerrain) {
                    offsetYHeight(y);
                } else {
                    component.offsetYHeight(y);
                }
            }
        }

        /** Mittel der Gelaendehoehe (= getTopSolidOrLiquidBlock) ueber die Grundflaeche, bei grossen Teilen gerastert. */
        private static int getAverageHeight(Structure.GenerationContext ctx, BoundingBox box) {
            int w = box.getXSpan();
            int d = box.getZSpan();
            int step = Math.max(1, (int) Math.ceil(Math.sqrt((double) (w * d) / 256.0D)));

            long total = 0;
            int iterations = 0;

            for (int z = box.minZ(); z <= box.maxZ(); z += step) {
                for (int x = box.minX(); x <= box.maxX(); x += step) {
                    total += ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                    iterations++;
                }
            }

            if (iterations == 0)
                return 64;

            return (int) (total / iterations);
        }

        private void addComponent(Component component, int placementPriority) {
            if (component == null) return;
            components.add(component);

            component.priority = placementPriority;
        }

        private BlockPos getConnectionTargetPosition(Component component, JigsawConnection connection) {
            // Richtung, in die dieses Teil weiterwaechst (ABSOLUT)
            ForgeDirection extendDir = component.rotateDir(connection.dir);

            // Startpunkt des naechsten Teils = Position des Verbinderblocks
            int x = component.getXWithOffset(connection.pos[0], connection.pos[2]) + extendDir.offsetX;
            int y = component.getYWithOffset(connection.pos[1]) + extendDir.offsetY;
            int z = component.getZWithOffset(connection.pos[0], connection.pos[2]) + extendDir.offsetZ;

            return new BlockPos(x, y, z);
        }

        private Set<JigsawPiece> findRequiredPieces(SpawnCondition spawn) {
            Set<JigsawPiece> requiredPieces = new HashSet<>();

            if (spawn.pools == null) return requiredPieces;

            for (JigsawPool pool : spawn.pools.values()) {
                for (JigsawPool.Entry weight : pool.pieces) {
                    if (weight.piece().required) {
                        requiredPieces.add(weight.piece());
                    }
                }
            }

            return requiredPieces;
        }

        private Component buildNextComponent(Random rand, SpawnCondition spawn, JigsawPool pool, Component fromComponent, JigsawConnection fromConnection) {
            JigsawPiece nextPiece = pool.get(rand);
            if (nextPiece == null) {
                MainRegistry.LOGGER.warn("[Jigsaw] Pool returned null piece: " + fromConnection.poolName);
                return null;
            }

            if (nextPiece.instanceLimit > 0) {
                int instances = 0;
                for (Component component : components) {
                    if (component.piece == nextPiece) {
                        instances++;

                        if (instances >= nextPiece.instanceLimit) return null;
                    }
                }
            }

            List<JigsawConnection> connectionPool = nextPiece.structure.getConnectionPool(fromConnection.dir, fromConnection.targetName);
            if (connectionPool == null || connectionPool.isEmpty()) {
                MainRegistry.LOGGER.warn("[Jigsaw] No valid connections for: " + fromConnection.targetName + " - in piece: " + nextPiece.name);
                return null;
            }

            JigsawConnection toConnection = connectionPool.get(rand.nextInt(connectionPool.size()));

            // naechstes Teil drehen, damit es passt
            int nextCoordBase = fromComponent.getNextCoordBase(fromConnection, toConnection, rand);

            BlockPos pos = getConnectionTargetPosition(fromComponent, fromConnection);

            // Startpunkt auf den Anschlusspunkt verschieben
            int ox = nextPiece.structure.rotateX(toConnection.pos[0], toConnection.pos[2], nextCoordBase);
            int oy = toConnection.pos[1];
            int oz = nextPiece.structure.rotateZ(toConnection.pos[0], toConnection.pos[2], nextCoordBase);

            return new Component(spawn, nextPiece, rand, pos.getX() - ox, pos.getY() - oy, pos.getZ() - oz, nextCoordBase).connectedFrom(toConnection);
        }

        private int getDistanceTo(BoundingBox box) {
            int x = box.getCenter().getX();
            int z = box.getCenter().getZ();

            return Math.max(Math.abs(x - (chunkX << 4)), Math.abs(z - (chunkZ << 4)));
        }

        public void offsetYHeight(int y) {
            for (Component component : components) {
                if (component.heightUpdated || component.piece.conformToTerrain || component.piece.alignToTerrain) continue;
                component.offsetYHeight(y);
            }
        }
    }

    /** Registriereintrag fuer {@link StructureType}. */
    public static StructureType<GenStructure> structureType() {
        return () -> GenStructure.CODEC;
    }

    /** Registriereintrag fuer {@link StructurePlacementType}. */
    public static StructurePlacementType<GridPlacement> placementType() {
        return () -> GridPlacement.CODEC;
    }
}
