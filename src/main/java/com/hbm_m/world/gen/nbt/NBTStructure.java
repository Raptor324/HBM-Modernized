package com.hbm_m.world.gen.nbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockWand;
import com.hbm_m.blockentity.generic.WandTandemBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.LegacyBiome;
import com.hbm_m.world.gen.NTMWorldGenerator;
import com.hbm_m.world.gen.nbt.selector.BiomeBlockSelector;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 1:1 {@code com.hbm.world.gen.nbt.NBTStructure}: .nbt-Strukturen speichern/laden/aufbauen und als Weltgen-Strukturen
 * mit Jigsaw-Verbindungen, Spawnbedingungen und Pools generieren (Start/Component/GenStructure: {@link NBTStructureGen}).
 *
 * <p>Abweichungen durch 1.20: Die Palette fuehrt echte Blockzustaende ({@code Name} + {@code Properties}) statt der
 * 1.7.10-Metadaten; die Original-Dateien wurden dafuer einmalig umgesetzt (Strukturbloecke wand_jigsaw/loot/logic/
 * tandem samt Daten bleiben erhalten). Die eigene {@code itemPalette} entfaellt, Gegenstaende stehen per Namen in der
 * Datei. Gedreht wird ueber {@link BlockState#rotate} statt ueber die Meta-Tabellen von {@code INBTBlockTransformable}.
 * Geladen wird beim ersten Zugriff (beim Klassenladen sind die Bloecke noch nicht registriert).</p>
 */
public class NBTStructure {

    /*
     * Die Strukturart wird im Bauteil gespeichert, daher kann diese Klasse beliebige Strukturen erzeugen,
     * ohne jede einzeln definieren und registrieren zu muessen.
     */

    private static final Map<String, SpawnCondition> namedMap = new HashMap<>();

    protected static final Map<Integer, List<SpawnCondition>> spawnMap = new HashMap<>();
    protected static final Map<Integer, List<SpawnCondition>> customSpawnMap = new HashMap<>();

    // Uebersetzung alter Blocknamen auf neue (Original "BOB PATCH")
    private static final Map<String, String> substitutions = new HashMap<>();
    static {
        substitutions.put("hbm:tile.ore_coal_oil", "minecraft:coal_ore");
    }

    private String name;

    private boolean isLoaded;
    private ResourceLocation lazyResource;

    protected int sizeX, sizeY, sizeZ;
    private StructBlockState[][][] blockArray;

    private List<List<JigsawConnection>> fromConnections;
    private Map<String, List<JigsawConnection>> toTopConnections;
    private Map<String, List<JigsawConnection>> toBottomConnections;
    private Map<String, List<JigsawConnection>> toHorizontalConnections;

    public NBTStructure(ResourceLocation resource) {
        // erst beim ersten Zugriff laden (Original: sofort im statischen StructureManager)
        this.lazyResource = resource;
        this.name = resource.getPath();
    }

    public NBTStructure(String name, InputStream stream) {
        this.name = name;
        loadStructure(stream);
    }

    public NBTStructure(File file) throws FileNotFoundException {
        this.name = file.getName();
        InputStream stream = new FileInputStream(file);
        loadStructure(stream);
        try { stream.close(); } catch (IOException ignored) { }
    }

    private synchronized void ensureLoaded() {
        if (lazyResource == null) return;
        ResourceLocation resource = lazyResource;
        // Server kennen keine Client-Ressourcen, daher wie im Original ueber den Klassenpfad
        InputStream stream = NBTStructure.class.getResourceAsStream("/assets/" + resource.getNamespace() + "/" + resource.getPath());
        if (stream != null) {
            loadStructure(stream);
        } else {
            MainRegistry.LOGGER.error("NBT Structure not found: " + resource.getPath());
        }
        lazyResource = null;
    }

    public String getName() {
        return name.substring(0, name.length() - 4); // .nbt abschneiden
    }

    public int getSizeX() { ensureLoaded(); return sizeX; }
    public int getSizeY() { ensureLoaded(); return sizeY; }
    public int getSizeZ() { ensureLoaded(); return sizeZ; }

    public boolean isLoaded() { ensureLoaded(); return isLoaded; }

    // ======================== Registrierung der Weltgen-Strukturen ========================

    /** Registriert eine Struktur fuer eine Dimension (0 = Oberwelt, -1 = Nether, 1 = End). */
    public static void registerStructure(int dimensionId, SpawnCondition spawn) {
        if (namedMap.containsKey(spawn.name) && namedMap.get(spawn.name) != spawn)
            throw new IllegalStateException("A severe error has occurred in NBTStructure! A SpawnCondition has been registered with the same name as another: " + spawn.name);

        namedMap.put(spawn.name, spawn);

        if (spawn.checkCoordinates != null) {
            List<SpawnCondition> spawnList = customSpawnMap.computeIfAbsent(dimensionId, integer -> new ArrayList<>());
            spawnList.add(spawn);
            return;
        }

        List<SpawnCondition> spawnList = spawnMap.computeIfAbsent(dimensionId, integer -> new ArrayList<>());
        spawnList.add(spawn);
    }

    public static void registerStructure(SpawnCondition spawn, int[] dimensionIds) {
        for (int dimensionId : dimensionIds) {
            registerStructure(dimensionId, spawn);
        }
    }

    // Chance, dass an einem gueltigen Ort nichts erscheint
    public static void registerNullWeight(int dimensionId, int weight) {
        registerNullWeight(dimensionId, weight, null);
    }

    public static void registerNullWeight(int dimensionId, int weight, Predicate<LegacyBiome> predicate) {
        SpawnCondition spawn = new SpawnCondition(weight, predicate);

        List<SpawnCondition> spawnList = spawnMap.computeIfAbsent(dimensionId, integer -> new ArrayList<>());
        spawnList.add(spawn);
    }

    // Liste aller (bisher) registrierten Strukturen
    public static List<String> listStructures() {
        NTMWorldGenerator.init();
        List<String> names = new ArrayList<>(namedMap.keySet());
        names.sort(String::compareTo);
        return names;
    }

    // Registrierte Struktur nach Namen, sonst null
    public static SpawnCondition getStructure(String name) {
        NTMWorldGenerator.init();
        return name == null ? null : namedMap.get(name);
    }

    // ======================== Speichern ========================

    /** Strukturverzeichnis {@code <Spielordner>/structures} (Original: {@code mcDataDir/structures}). */
    public static File getStructureDirectory() {
        File dir = dev.architectury.platform.Platform.getGameFolder().resolve("structures").toFile();
        dir.mkdir();
        return dir;
    }

    /** Speichert den Bereich als NBT-Struktur (+ Blockentities); {@code exclude} wird uebersprungen. */
    public static CompoundTag saveArea(Level world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockState> exclude) {
        CompoundTag structure = new CompoundTag();
        ListTag nbtBlocks = new ListTag();
        ListTag nbtPalette = new ListTag();

        Map<BlockState, Integer> palette = new HashMap<>();

        structure.putInt("version", 1);
        structure.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        int ox = Math.min(x1, x2);
        int oy = Math.min(y1, y2);
        int oz = Math.min(z1, z2);

        for (int x = ox; x <= Math.max(x1, x2); x++) {
            for (int y = oy; y <= Math.max(y1, y2); y++) {
                for (int z = oz; z <= Math.max(z1, z2); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState block = world.getBlockState(pos);

                    if (exclude.contains(block)) continue;

                    if (block.getBlock() instanceof BlockWand wand) {
                        block = wand.exportAs.get().defaultBlockState();
                    }

                    int paletteId = palette.size();
                    if (palette.containsKey(block)) {
                        paletteId = palette.get(block);
                    } else {
                        palette.put(block, paletteId);
                        nbtPalette.add(NbtUtils.writeBlockState(block));
                    }

                    CompoundTag nbtBlock = new CompoundTag();
                    nbtBlock.putInt("state", paletteId);

                    ListTag nbtPos = new ListTag();
                    nbtPos.add(IntTag.valueOf(x - ox));
                    nbtPos.add(IntTag.valueOf(y - oy));
                    nbtPos.add(IntTag.valueOf(z - oz));
                    nbtBlock.put("pos", nbtPos);

                    BlockEntity te = world.getBlockEntity(pos);
                    if (te != null) {
                        CompoundTag nbt = PlatformHooks.saveBlockEntityWithoutMetadata(te, world.registryAccess());
                        nbt.remove("x");
                        nbt.remove("y");
                        nbt.remove("z");
                        nbtBlock.put("nbt", nbt);
                    }

                    nbtBlocks.add(nbtBlock);
                }
            }
        }

        structure.put("blocks", nbtBlocks);
        structure.put("palette", nbtPalette);

        ListTag nbtSize = new ListTag();
        nbtSize.add(IntTag.valueOf(Math.abs(x1 - x2) + 1));
        nbtSize.add(IntTag.valueOf(Math.abs(y1 - y2) + 1));
        nbtSize.add(IntTag.valueOf(Math.abs(z1 - z2) + 1));
        structure.put("size", nbtSize);

        structure.put("entities", new ListTag());

        return structure;
    }

    /** Schreibt den Bereich als .nbt-Datei mit dem gegebenen Namen in das Strukturverzeichnis. */
    public static File quickSaveArea(String filename, Level world, int x1, int y1, int z1, int x2, int y2, int z2, Set<BlockState> exclude) {
        CompoundTag structure = saveArea(world, x1, y1, z1, x2, y2, z2, exclude);

        try {
            File structureFile = new File(getStructureDirectory(), filename);
            try (OutputStream out = new FileOutputStream(structureFile)) {
                NbtIo.writeCompressed(structure, out);
            }
            return structureFile;
        } catch (Exception ex) {
            MainRegistry.LOGGER.warn("Failed to save NBT structure", ex);
            return null;
        }
    }

    // ======================== Laden ========================

    private void loadStructure(InputStream inputStream) {
        try {
            CompoundTag data = NbtIo.readCompressed(inputStream);

            // GROESSE (fuer die Zentrierung)
            int[] size = parsePos(data.getList("size", Tag.TAG_INT));
            sizeX = size[0];
            sizeY = size[1];
            sizeZ = size[2];

            boolean debug = ModClothConfig.get().structureDebug;

            // BLOCKPALETTE
            ListTag paletteList = data.getList("palette", Tag.TAG_COMPOUND);
            BlockState[] palette = new BlockState[paletteList.size()];

            for (int i = 0; i < paletteList.size(); i++) {
                CompoundTag p = paletteList.getCompound(i).copy();

                String blockName = p.getString("Name");
                if (substitutions.containsKey(blockName)) {
                    p.putString("Name", substitutions.get(blockName));
                }

                palette[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), p);

                if (debug && palette[i].isAir()) {
                    palette[i] = ModBlocks.WAND_AIR.get().defaultBlockState();
                }
            }

            // BLOECKE
            ListTag blockData = data.getList("blocks", Tag.TAG_COMPOUND);
            blockArray = new StructBlockState[sizeX][sizeY][sizeZ];

            List<JigsawConnection> connections = new ArrayList<>();

            for (int i = 0; i < blockData.size(); i++) {
                CompoundTag block = blockData.getCompound(i);
                int state = block.getInt("state");
                int[] pos = parsePos(block.getList("pos", Tag.TAG_INT));

                StructBlockState blockState = new StructBlockState(palette[state]);

                if (block.contains("nbt")) {
                    CompoundTag nbt = block.getCompound("nbt");
                    blockState.nbt = nbt;

                    // Verbindungspunkte der Jigsaw-Bloecke einlesen
                    if (blockState.definition.getBlock() == ModBlocks.WAND_JIGSAW.get()) {
                        if (toTopConnections == null) toTopConnections = new HashMap<>();
                        if (toBottomConnections == null) toBottomConnections = new HashMap<>();
                        if (toHorizontalConnections == null) toHorizontalConnections = new HashMap<>();

                        int selectionPriority = nbt.getInt("selection");
                        int placementPriority = nbt.getInt("placement");
                        ForgeDirection direction = ForgeDirection.getOrientation(nbt.getInt("direction"));
                        String poolName = nbt.getString("pool");
                        String ourName = nbt.getString("name");
                        String targetName = nbt.getString("target");
                        boolean isRollable = nbt.getBoolean("roll");

                        JigsawConnection connection = new JigsawConnection(pos, direction, poolName, targetName, isRollable, selectionPriority, placementPriority);

                        connections.add(connection);

                        Map<String, List<JigsawConnection>> toConnections;
                        if (direction == ForgeDirection.UP) {
                            toConnections = toTopConnections;
                        } else if (direction == ForgeDirection.DOWN) {
                            toConnections = toBottomConnections;
                        } else {
                            toConnections = toHorizontalConnections;
                        }

                        List<JigsawConnection> namedConnections = toConnections.computeIfAbsent(ourName, n -> new ArrayList<>());
                        namedConnections.add(connection);

                        if (!debug) {
                            BlockState replace = nbt.contains("block") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("block")) : Blocks.AIR.defaultBlockState();
                            blockState = new StructBlockState(replace);
                        }
                    }
                }

                blockArray[pos[0]][pos[1]][pos[2]] = blockState;
            }

            // VERBINDUNGEN + PRIORITAETEN
            if (connections.size() > 0) {
                fromConnections = new ArrayList<>();

                connections.sort((a, b) -> b.selectionPriority - a.selectionPriority); // absteigend, hoechste zuerst

                // pro Prioritaetsstufe eine eigene Liste
                List<JigsawConnection> innerList = null;
                int currentPriority = 0;
                for (JigsawConnection connection : connections) {
                    if (innerList == null || currentPriority != connection.selectionPriority) {
                        innerList = new ArrayList<>();
                        fromConnections.add(innerList);
                        currentPriority = connection.selectionPriority;
                    }

                    innerList.add(connection);
                }
            }

            isLoaded = true;

        } catch (Exception e) {
            MainRegistry.LOGGER.error("Exception reading NBT Structure format", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // still
            }
        }
    }

    List<List<JigsawConnection>> getFromConnections() {
        ensureLoaded();
        return fromConnections;
    }

    // ======================== Aufbau ========================

    private BlockEntity buildTileEntity(LevelAccessor world, BlockPos pos, CompoundTag nbt, int coordBaseMode, String structureName) {
        nbt = nbt.copy();
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());

        BlockEntity te = world.getBlockEntity(pos);
        if (te == null) return null;

        PlatformHooks.loadBlockEntityTag(te, nbt, world.registryAccess());

        if (te instanceof INBTTileEntityTransformable transformable) {
            transformable.transformTE(world, coordBaseMode);
        }

        if (te instanceof WandTandemBlockEntity tandem) {
            tandem.arm(getStructure(structureName));
        }

        te.setChanged();
        return te;
    }

    public void build(LevelAccessor world, int x, int y, int z) {
        build(world, x, y, z, 0, true, false);
    }

    public void build(LevelAccessor world, int x, int y, int z, int coordBaseMode) {
        build(world, x, y, z, coordBaseMode, true, false);
    }

    public void build(LevelAccessor world, int x, int y, int z, int coordBaseMode, boolean center, boolean wipeExisting) {
        ensureLoaded();
        if (!isLoaded) {
            MainRegistry.LOGGER.info("NBTStructure is invalid");
            return;
        }

        if (center) {
            boolean swizzle = coordBaseMode == 1 || coordBaseMode == 3;
            x -= (swizzle ? sizeZ : sizeX) / 2;
            z -= (swizzle ? sizeX : sizeZ) / 2;
        }

        Rotation rotation = toRotation(coordBaseMode);

        for (int bx = 0; bx < sizeX; bx++) {
            for (int bz = 0; bz < sizeZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + x;
                int rz = rotateZ(bx, bz, coordBaseMode) + z;

                for (int by = 0; by < sizeY; by++) {
                    StructBlockState state = blockArray[bx][by][bz];
                    if (state == null) {
                        if (wipeExisting) world.setBlock(new BlockPos(rx, by + y, rz), Blocks.AIR.defaultBlockState(), 2);
                        continue;
                    }

                    int ry = by + y;
                    if (ry < world.getMinBuildHeight() + 1) continue;

                    BlockPos pos = new BlockPos(rx, ry, rz);
                    BlockState block = coordBaseMode == 0 ? state.definition : state.definition.rotate(rotation);

                    world.setBlock(pos, block, 2);

                    if (state.nbt != null) {
                        buildTileEntity(world, pos, state.nbt, coordBaseMode, null);
                    }
                }
            }
        }
    }

    /** Fuer Tandems: Teil direkt an Koordinaten bauen. */
    public void build(LevelAccessor world, JigsawPiece piece, int x, int y, int z, int coordBaseMode, String structureName) {
        ensureLoaded();
        BoundingBox bb;
        switch (coordBaseMode) {
            case 1:
            case 3:
                bb = new BoundingBox(x, y, z, x + piece.structure.getSizeZ() - 1, y + piece.structure.getSizeY() - 1, z + piece.structure.getSizeX() - 1);
                break;
            default:
                bb = new BoundingBox(x, y, z, x + piece.structure.getSizeX() - 1, y + piece.structure.getSizeY() - 1, z + piece.structure.getSizeZ() - 1);
                break;
        }

        build(world, piece, bb, bb, coordBaseMode, structureName, world.getRandom());
    }

    /** Baut den Teil von {@code totalBounds}, der in {@code generatingBounds} liegt (Weltgen: der aktuelle Chunk). */
    protected boolean build(LevelAccessor world, JigsawPiece piece, BoundingBox totalBounds, BoundingBox generatingBounds, int coordBaseMode, String structureName, RandomSource rand) {
        ensureLoaded();
        if (!isLoaded) {
            MainRegistry.LOGGER.info("NBTStructure is invalid");
            return false;
        }

        int sizeX = totalBounds.maxX() - totalBounds.minX();
        int sizeZ = totalBounds.maxZ() - totalBounds.minZ();

        // Voxelgitter-Transformationen koennen einen fertig machen
        int absMinX = Math.max(generatingBounds.minX() - totalBounds.minX(), 0);
        int absMaxX = Math.min(generatingBounds.maxX() - totalBounds.minX(), sizeX);
        int absMinZ = Math.max(generatingBounds.minZ() - totalBounds.minZ(), 0);
        int absMaxZ = Math.min(generatingBounds.maxZ() - totalBounds.minZ(), sizeZ);

        // liegt der zu erzeugende Bereich ueberhaupt im Teil?
        if (absMinX > sizeX || absMaxX < 0 || absMinZ > sizeZ || absMaxZ < 0) return true;

        int rotMinX = unrotateX(absMinX, absMinZ, coordBaseMode);
        int rotMaxX = unrotateX(absMaxX, absMaxZ, coordBaseMode);
        int rotMinZ = unrotateZ(absMinX, absMinZ, coordBaseMode);
        int rotMaxZ = unrotateZ(absMaxX, absMaxZ, coordBaseMode);

        int minX = Math.min(rotMinX, rotMaxX);
        int maxX = Math.max(rotMinX, rotMaxX);
        int minZ = Math.min(rotMinZ, rotMaxZ);
        int maxZ = Math.max(rotMinZ, rotMaxZ);

        if (piece.blockTable != null || piece.platform != null) {
            LegacyBiome biome = LegacyBiome.of(world.getBiome(new BlockPos(generatingBounds.getCenter().getX(), world.getSeaLevel(), generatingBounds.getCenter().getZ())));

            if (piece.blockTable != null) {
                for (BlockSelector selector : piece.blockTable.values()) {
                    if (selector instanceof BiomeBlockSelector bbs) {
                        bbs.nextBiome = biome;
                    }
                }
            }

            if (piece.platform instanceof BiomeBlockSelector bbs) {
                bbs.nextBiome = biome;
            }
        }

        Rotation rotation = toRotation(coordBaseMode);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                int rx = rotateX(bx, bz, coordBaseMode) + totalBounds.minX();
                int rz = rotateZ(bx, bz, coordBaseMode) + totalBounds.minZ();
                int oy = piece.conformToTerrain ? getTopSolidOrLiquidBlock(world, rx, rz) + piece.heightOffset : totalBounds.minY();

                boolean hasBase = false;

                for (int by = 0; by < this.sizeY; by++) {
                    StructBlockState state = blockArray[bx][by][bz];
                    if (state == null) continue;

                    int ry = by + oy;

                    BlockState block = transformBlock(state.definition, piece.blockTable, rand, rotation, coordBaseMode);

                    if (ry < world.getMinBuildHeight() + 1) continue;

                    BlockPos pos = new BlockPos(rx, ry, rz);
                    world.setBlock(pos, block, 2);

                    if (state.nbt != null) {
                        buildTileEntity(world, pos, state.nbt, coordBaseMode, structureName);
                    }

                    if (by == 0 && piece.platform != null && !block.canBeReplaced()) hasBase = true;
                }

                if (hasBase && !piece.conformToTerrain) {
                    for (int y = oy - 1; y > world.getMinBuildHeight(); y--) {
                        BlockPos p = new BlockPos(rx, y, rz);
                        if (!world.getBlockState(p).canBeReplaced()) break;
                        BlockState plat;
                        synchronized (piece.platform) {
                            piece.platform.selectBlocks(rand, 0, 0, 0, false);
                            plat = piece.platform.getSelected();
                        }
                        world.setBlock(p, plat, 2);
                    }
                }
            }
        }

        return true;
    }

    /**
     * 1.7.10 {@code World.getTopSolidOrLiquidBlock}: oberster bewegungsblockierender Block ohne Laub (Wasser zaehlt
     * dabei nicht) + 1 - entspricht der Gelaende-Hoehenkarte vor der Dekoration.
     */
    public static int getTopSolidOrLiquidBlock(LevelAccessor world, int x, int z) {
        // waehrend der Weltgenerierung die Gelaendekarte, in fertigen Chunks (Tandem) die laufende Karte
        return world.getHeight(world instanceof net.minecraft.server.level.WorldGenRegion ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR, x, z);
    }

    private BlockState transformBlock(BlockState definition, Map<Block, BlockSelector> blockTable, RandomSource rand, Rotation rotation, int coordBaseMode) {
        if (blockTable != null && blockTable.containsKey(definition.getBlock())) {
            final BlockSelector selector = blockTable.get(definition.getBlock());
            // Port: Chunks werden parallel bebaut, Selektoren sind geteilt
            synchronized (selector) {
                selector.selectBlocks(rand, 0, 0, 0, false);
                return selector.getSelected();
            }
        }

        return coordBaseMode == 0 ? definition : definition.rotate(rotation);
    }

    public List<JigsawConnection> getConnectionPool(ForgeDirection dir, String target) {
        ensureLoaded();
        if (dir == ForgeDirection.DOWN) {
            return toTopConnections == null ? null : toTopConnections.get(target);
        } else if (dir == ForgeDirection.UP) {
            return toBottomConnections == null ? null : toBottomConnections.get(target);
        }

        return toHorizontalConnections == null ? null : toHorizontalConnections.get(target);
    }

    /** coordBaseMode der 1.7.10 als Vanilla-Drehung (1 = Ost->Sued, 2 = halbe Drehung, 3 = Ost->Nord). */
    public static Rotation toRotation(int coordBaseMode) {
        switch (coordBaseMode) {
            case 1: return Rotation.CLOCKWISE_90;
            case 2: return Rotation.CLOCKWISE_180;
            case 3: return Rotation.COUNTERCLOCKWISE_90;
            default: return Rotation.NONE;
        }
    }

    private int[] parsePos(ListTag pos) {
        return new int[] { pos.getInt(0), pos.getInt(1), pos.getInt(2) };
    }

    public int rotateX(int x, int z, int coordBaseMode) {
        ensureLoaded();
        switch (coordBaseMode) {
            case 1: return sizeZ - 1 - z;
            case 2: return sizeX - 1 - x;
            case 3: return z;
            default: return x;
        }
    }

    public int rotateZ(int x, int z, int coordBaseMode) {
        ensureLoaded();
        switch (coordBaseMode) {
            case 1: return x;
            case 2: return sizeZ - 1 - z;
            case 3: return sizeX - 1 - x;
            default: return z;
        }
    }

    private int unrotateX(int x, int z, int coordBaseMode) {
        switch (coordBaseMode) {
            case 3: return sizeX - 1 - z;
            case 2: return sizeX - 1 - x;
            case 1: return z;
            default: return x;
        }
    }

    private int unrotateZ(int x, int z, int coordBaseMode) {
        switch (coordBaseMode) {
            case 3: return x;
            case 2: return sizeZ - 1 - z;
            case 1: return sizeZ - 1 - x;
            default: return z;
        }
    }

    private static class StructBlockState {

        final BlockState definition;
        CompoundTag nbt;

        StructBlockState(BlockState definition) {
            this.definition = definition;
        }
    }

    /** Jeder Jigsaw-Block einer Struktur erzeugt eine dieser Verbindungen. */
    public static class JigsawConnection {

        public final int[] pos;
        public final ForgeDirection dir;

        // in welchem Pool nach einem Anschluss gesucht wird
        final String poolName;

        // welche Verbindungen im gefundenen Teil angesteuert werden
        final String targetName;

        final boolean isRollable;

        final int selectionPriority;
        final int placementPriority;

        private JigsawConnection(int[] pos, ForgeDirection dir, String poolName, String targetName, boolean isRollable, int selectionPriority, int placementPriority) {
            this.pos = pos;
            this.dir = dir;
            this.poolName = poolName;
            this.targetName = targetName;
            this.isRollable = isRollable;
            this.selectionPriority = selectionPriority;
            this.placementPriority = placementPriority;
        }
    }
}
