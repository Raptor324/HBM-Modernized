package com.hbm_m.world.gen.component;

import java.util.List;
import java.util.Random;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.decorations.TrinketBlockEntity;
import com.hbm_m.block.decorations.TrinketTypes.BobbleType;
import com.hbm_m.config.StructureConfig;
import com.hbm_m.itempool.ItemPool;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.LMaterial;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.gen.nbt.BlockSelector;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * 1:1 {@code com.hbm.world.gen.component.Component} samt der genutzten Teile von 1.7.10
 * {@code StructureComponent}: Bauteil-Strukturen (Haeuser, Labore, Bueros, Silo, Bunker) mit eigener, nicht
 * spiegelnder Drehung ({@code coordBaseMode} 0 Sued, 1 West, 2 Nord, 3 Ost) und Metadaten-Hilfen fuer die
 * Bloecke; Bloecke laufen ueber die Meta-Tabellen {@link MB}/{@link VB}.
 *
 * <p>Abweichung durch 1.20: Die mittlere Gelaendehoehe ({@link #setAverageHeight}) bestimmt das Original beim
 * ersten Bau eines Chunks. Da 1.20 Chunks parallel bebaut, wird sie beim Zusammenbau der Struktur aus der
 * Gelaende-Hoehenkarte ueber die ganze Grundflaeche gemittelt ({@link #computeHeight}).</p>
 */
public abstract class Component extends StructurePiece {

    /** Mittlere Hoehe ("height position"). */
    protected int hpos = -1;

    protected int coordBaseMode;

    protected Component(StructurePieceType type, int componentType) {
        super(type, componentType, new BoundingBox(0, 0, 0, 0, 0, 0));
    }

    protected Component(StructurePieceType type, Random rand, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(type, 0, new BoundingBox(0, 0, 0, 0, 0, 0));
        this.coordBaseMode = rand.nextInt(4);

        switch (this.coordBaseMode) {
            case 0:
                this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxX, minY + maxY, minZ + maxZ);
                break;
            case 1:
                this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxZ, minY + maxY, minZ + maxX);
                break;
            case 2:
                this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxX, minY + maxY, minZ + maxZ);
                break;
            case 3:
                this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxZ, minY + maxY, minZ + maxX);
                break;
            default:
                this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxX, minY + maxY, minZ + maxZ);
        }
    }

    /** Laden aus dem Chunk. */
    protected Component(StructurePieceType type, CompoundTag nbt) {
        super(type, nbt);
        this.hpos = nbt.getInt("HPos");
        this.coordBaseMode = nbt.getInt("CBM");
    }

    @Override
    protected final void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag nbt) {
        nbt.putInt("HPos", this.hpos);
        nbt.putInt("CBM", this.coordBaseMode);
        writeLegacy(nbt);
    }

    /** Original {@code func_143012_a}. */
    protected void writeLegacy(CompoundTag nbt) { }

    /** Original {@code func_143011_b}; die Unterklassen rufen es in ihrem Lade-Konstruktor. */
    protected void readLegacy(CompoundTag nbt) {
        this.hpos = nbt.getInt("HPos");
    }

    @Override
    public void postProcess(WorldGenLevel world, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        addComponentParts(world, new Random(random.nextLong()), box);
    }

    /** Original {@code addComponentParts(World, Random, StructureBoundingBox)}. */
    public abstract boolean addComponentParts(LevelAccessor world, Random rand, BoundingBox box);

    /** Fuer die Bauteil-Fabriken: {@code buildComponent} des 1.7.10-StructureComponent (Standard: nichts). */
    public void buildComponent(Component start, List<StructurePiece> components, Random rand) { }

    public int getComponentType() {
        return this.genDepth;
    }

    public void setBoundingBox(BoundingBox box) {
        this.boundingBox = box;
    }

    public void offset(int x, int y, int z) {
        this.boundingBox.move(x, y, z);
    }

    // ================================ Hoehe ================================

    /**
     * Port: mittlere Gelaendehoehe ({@code max(getTopSolidOrLiquidBlock, 64)}) ueber die Grundflaeche aus der
     * Hoehenkarte vor der Dekoration und sofortige Verschiebung (Original: {@link #setAverageHeight} im ersten Chunk).
     */
    public void computeHeight(Structure.GenerationContext ctx) {
        int h = getAverageHeight(ctx, this.boundingBox);
        if (h < 0) return;

        this.hpos = h; // Mittel ueber die Grundflaeche
        this.boundingBox.move(0, this.hpos - this.boundingBox.minY(), 0);
    }

    /**
     * Mittlere Gelaendehoehe ({@code max(Hoehe, 64)}) einer Flaeche aus der Rauschhoehe. Port: Stichprobe jedes
     * zweiten Blocks, da {@code getBaseHeight} pro Spalte das Gelaenderauschen auswertet.
     */
    public static int getAverageHeight(Structure.GenerationContext ctx, BoundingBox area) {
        int total = 0;
        int iterations = 0;

        for (int z = area.minZ(); z <= area.maxZ(); z += 2) {
            for (int x = area.minX(); x <= area.maxX(); x += 2) {
                total += Math.max(ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState()), 64);
                iterations++;
            }
        }

        return iterations == 0 ? -1 : total / iterations;
    }

    /** Original: Mittel der Hoehen im aktuellen Bauabschnitt; im Port beim Zusammenbau festgelegt. */
    protected boolean setAverageHeight(LevelAccessor world, BoundingBox box, int y) {
        if (this.hpos >= 0) return true;

        int total = 0;
        int iterations = 0;

        for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
            for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
                if (box.isInside(x, y, z)) {
                    total += Math.max(L.getTopSolidOrLiquidBlock(world, x, z), 64);
                    iterations++;
                }
            }
        }

        if (iterations == 0)
            return false;

        this.hpos = total / iterations; // Mittel aller Bloecke im Begrenzungsrahmen
        this.boundingBox.move(0, this.hpos - this.boundingBox.minY(), 0);
        return true;
    }

    protected static int getAverageHeight(LevelAccessor world, BoundingBox area, BoundingBox box, int y) {

        int total = 0;
        int iterations = 0;

        for (int z = area.minZ(); z <= area.maxZ(); z++) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                if (box.isInside(x, y, z)) {
                    total += Math.max(L.getTopSolidOrLiquidBlock(world, x, z), 64);
                    iterations++;
                }
            }
        }

        if (iterations == 0)
            return -1;

        return total / iterations;
    }

    public int getCoordMode() {
        return this.coordBaseMode;
    }

    // ================================ Metadaten fuer Dekoration ================================

    /** Drehbare Saeulen: Bits 2-3 = Achse. */
    protected int getPillarMeta(int metadata) {
        if (this.coordBaseMode % 2 != 0 && this.coordBaseMode != -1)
            metadata = metadata ^ 12;

        return metadata;
    }

    /** DecoBlock: 2 Sued, 3 Nord, 4 Ost, 5 West (Faelle fallen wie im Original durch). */
    protected int getDecoMeta(int metadata) {
        switch (this.coordBaseMode) {
            case 0: // Sued
                switch (metadata) {
                    case 2: return 2;
                    case 3: return 3;
                    case 4: return 4;
                    case 5: return 5;
                }
            case 1: // West
                switch (metadata) {
                    case 2: return 5;
                    case 3: return 4;
                    case 4: return 2;
                    case 5: return 3;
                }
            case 2: // Nord
                switch (metadata) {
                    case 2: return 3;
                    case 3: return 2;
                    case 4: return 5;
                    case 5: return 4;
                }
            case 3: // Ost
                switch (metadata) {
                    case 2: return 4;
                    case 3: return 5;
                    case 4: return 3;
                    case 5: return 2;
                }
        }
        return 0;
    }

    /** BlockDecoModel/Falltueren: 0 Nord, 1 Sued, 2 West, 3 Ost; Ergebnis um 2 Bit verschoben. */
    protected int getDecoModelMeta(int metadata) {
        // N: 0b00, S: 0b01, W: 0b10, E: 0b11

        switch (this.coordBaseMode) {
            default: // Sued
                break;
            case 1: // West
                if ((metadata & 3) < 2) // N & S: beide Bits kippen
                    metadata = metadata ^ 3;
                else // W & E: erstes Bit auf 0
                    metadata = metadata ^ 2;
                break;
            case 2: // Nord
                metadata = metadata ^ 1; // N, W, E & S: erstes Bit kippen
                break;
            case 3: // Ost
                if ((metadata & 3) < 2) // N & S: zweites Bit setzen
                    metadata = metadata ^ 2;
                else // W & E: beide Bits kippen
                    metadata = metadata ^ 3;
                break;
        }
        return metadata << 2; // Platz fuer die Drehbits von BlockDecoModel
    }

    /** CRTs, Toaster und alles mit den Minecraft-Himmelsrichtungen. S: 0, W: 1, N: 2, E: 3 */
    protected int getCRTMeta(int meta) {
        return (meta + this.coordBaseMode) % 4;
    }

    /** Treppen: 0 West, 1 Ost, 2 Nord, 3 Sued. */
    protected int getStairMeta(int metadata) {
        switch (this.coordBaseMode) {
            default: // Sued
                break;
            case 1: // West
                if ((metadata & 3) < 2) // E/W: zweites Bit
                    metadata = metadata ^ 2;
                else
                    metadata = metadata ^ 3; // N/S: beide Bits
                break;
            case 2: // Nord
                metadata = metadata ^ 1; // erstes Bit
                break;
            case 3: // Ost
                if ((metadata & 3) < 2) // E/W: beide Bits
                    metadata = metadata ^ 3;
                else // N/S: zweites Bit
                    metadata = metadata ^ 2;
                break;
        }

        return metadata;
    }

    /**
     * Tuer auf der gegenueberliegenden Seite der Richtung: Ost 0, Sued 1, West 2, Nord 3.
     * Unten Ausrichtung (+ 4 offen), oben 8 (+ 1 Scharnier rechts).
     */
    protected void placeDoor(LevelAccessor world, BoundingBox box, LB door, int dirMeta, boolean opensRight, boolean isOpen, int featureX, int featureY, int featureZ) { // isOpen fuer zufaellig offene Tueren
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return;

        switch (this.coordBaseMode) {
            default: // Sued
                break;
            case 1: // West
                dirMeta = (dirMeta + 1) % 4; break;
            case 2: // Nord
                dirMeta ^= 2; break; // zweites Bit kippen
            case 3: // Ost
                dirMeta = (dirMeta + 3) % 4; break;
        }

        int metaBottom = dirMeta | (isOpen ? 0b100 : 0);

        if (L.doesBlockHaveSolidTopSurface(world, posX, posY - 1, posZ)) {
            // 1.20: beide Haelften tragen Ausrichtung, Scharnier und Offen-Zustand
            BlockState lower = door.state(metaBottom);
            if (lower.hasProperty(BlockStateProperties.DOOR_HINGE)) {
                lower = lower.setValue(BlockStateProperties.DOOR_HINGE, opensRight ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
            }
            L.setState(world, posX, posY, posZ, lower, 2);
            if (lower.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                L.setState(world, posX, posY + 1, posZ, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
            }
        }
    }

    /** 1 Westseite, 2 Ostseite, 3 Nord, 4 Sued. */
    protected void placeLever(LevelAccessor world, BoundingBox box, int dirMeta, boolean on, int featureX, int featureY, int featureZ) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return;

        if (dirMeta <= 0 || dirMeta >= 7) { // Hebel sind furchtbar
            switch (this.coordBaseMode) {
                case 1: case 3: // West / Ost
                    dirMeta ^= 0b111;
            }
        } else if (dirMeta >= 5) {
            switch (this.coordBaseMode) {
                case 1: case 3: // West / Ost
                    dirMeta = (dirMeta + 1) % 2 + 5;
            }
        } else {
            dirMeta = getButtonMeta(dirMeta);
        }

        L.setBlock(world, posX, posY, posZ, VB.lever, on ? dirMeta | 8 : dirMeta, 2);
    }

    /** Auch fuer seitliche Hebel. */
    protected int getButtonMeta(int dirMeta) {
        switch (this.coordBaseMode) {
            case 1: // West
                if (dirMeta <= 2) return dirMeta + 2;
                else if (dirMeta < 4) return dirMeta - 1;
                else return dirMeta - 3;
            case 2: // Nord
                return dirMeta + (dirMeta % 2 == 0 ? -1 : 1);
            case 3: // Ost
                if (dirMeta <= 1) return dirMeta + 3;
                else if (dirMeta <= 2) return dirMeta + 1;
                else return dirMeta - 2;
            default: // Sued
                return dirMeta;
        }
    }

    /** N:0 W:1 S:2 E:3 */
    protected void placeBed(LevelAccessor world, BoundingBox box, int meta, int featureX, int featureY, int featureZ) {
        int xOffset = 0;
        int zOffset = 0;

        switch (meta & 3) {
            default:
                zOffset = 1; break;
            case 1:
                xOffset = -1; break;
            case 2:
                zOffset = -1; break;
            case 3:
                xOffset = 1; break;
        }

        switch (this.coordBaseMode) {
            default: // S
                break;
            case 1: // W
                meta = (meta + 1) % 4; break;
            case 2: // N
                meta ^= 2; break;
            case 3: // E
                meta = (meta - 1) % 4; break;
        }

        placeBlockAtCurrentPosition(world, VB.bed, meta, featureX, featureY, featureZ, box);
        placeBlockAtCurrentPosition(world, VB.bed, meta + 8, featureX + xOffset, featureY, featureZ + zOffset, box);
    }

    /** Stolperdrahthaken: S:0 W:1 N:2 E:3 */
    protected int getTripwireMeta(int metadata) {
        switch (this.coordBaseMode) {
            default:
                return metadata;
            case 1:
                return (metadata + 1) % 4;
            case 2:
                return metadata ^ 2;
            case 3:
                return (metadata - 1) % 4;
        }
    }

    // ================================ Beute ================================

    protected boolean generateInvContents(LevelAccessor world, BoundingBox box, Random rand, LB block, int featureX, int featureY, int featureZ, ItemPool.WeightedContent[] content, int amount) {
        return generateInvContents(world, box, rand, block, 0, featureX, featureY, featureZ, content, amount);
    }

    protected boolean generateInvContents(LevelAccessor world, BoundingBox box, Random rand, LB block, int meta, int featureX, int featureY, int featureZ, ItemPool.WeightedContent[] content, int amount) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ) || L.getBlock(world, posX, posY, posZ) == block) // Ersatz fuer hasPlacedLoot
            return true;

        this.placeBlockAtCurrentPosition(world, block, meta, featureX, featureY, featureZ, box);
        BlockEntity inventory = L.getTileEntity(world, posX, posY, posZ);

        if (ItemPool.isInventory(inventory)) {
            amount = (int) Math.floor(amount * StructureConfig.lootAmountFactor);
            ItemPool.generateChestContents(rand, content, inventory, amount < 1 ? 1 : amount);
            return true;
        }

        return false;
    }

    /** Das Blockentity muss abschliessbar sein. */
    protected boolean generateLockableContents(LevelAccessor world, BoundingBox box, Random rand, LB block, int featureX, int featureY, int featureZ,
            ItemPool.WeightedContent[] content, int amount, double mod) {
        return generateLockableContents(world, box, rand, block, 0, featureX, featureY, featureZ, content, amount, mod);
    }

    protected boolean generateLockableContents(LevelAccessor world, BoundingBox box, Random rand, LB block, int meta, int featureX, int featureY, int featureZ,
            ItemPool.WeightedContent[] content, int amount, double mod) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ) || L.getBlock(world, posX, posY, posZ) == block) // Ersatz fuer hasPlacedLoot
            return false;

        this.placeBlockAtCurrentPosition(world, block, meta, featureX, featureY, featureZ, box);
        BlockEntity tile = L.getTileEntity(world, posX, posY, posZ);

        if (ItemPool.isInventory(tile) && tile instanceof com.hbm_m.api.tile.ILockableTile lock) {
            amount = (int) Math.floor(amount * StructureConfig.lootAmountFactor);
            ItemPool.generateChestContents(rand, content, tile, amount < 1 ? 1 : amount);

            lock.setPins(rand.nextInt(999) + 1);
            lock.setMod(mod);
            lock.lock();
            return true;
        }

        return false;
    }

    protected void generateLoreBook(LevelAccessor world, BoundingBox box, int featureX, int featureY, int featureZ, int slot, ItemStack stack) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return;

        ItemPool.Inv inventory = ItemPool.view(L.getTileEntity(world, posX, posY, posZ));

        if (inventory != null) {
            inventory.set(slot, stack);
        }
    }

    /** Zufaellige Wackelfigur mit zufaelliger Ausrichtung. */
    protected void placeRandomBobble(LevelAccessor world, BoundingBox box, Random rand, int featureX, int featureY, int featureZ) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        placeBlockAtCurrentPosition(world, MB.bobblehead, rand.nextInt(16), featureX, featureY, featureZ, box);
        BlockEntity bobble = box.isInside(posX, posY, posZ) ? L.getTileEntity(world, posX, posY, posZ) : null;

        if (bobble instanceof TrinketBlockEntity trinket) {
            trinket.setType(rand.nextInt(BobbleType.values().length - 1) + 1);
            trinket.setChanged();
        }
    }

    // ================================ Platzierungshilfen ================================

    /** Setzt Bloecke nach unten, bis fester Boden kommt (Fundamente). */
    protected void placeFoundationUnderneath(LevelAccessor world, LB placeBlock, int meta, int minX, int minZ, int maxX, int maxZ, int featureY, BoundingBox box) {

        for (int featureX = minX; featureX <= maxX; featureX++) {
            for (int featureZ = minZ; featureZ <= maxZ; featureZ++) {
                int posX = this.getXWithOffset(featureX, featureZ);
                int posY = this.getYWithOffset(featureY);
                int posZ = this.getZWithOffset(featureX, featureZ);

                if (box.isInside(posX, posY, posZ)) {
                    LB block = L.getBlock(world, posX, posY, posZ);
                    int brake = 0;

                    while ((L.isAirBlock(world, posX, posY, posZ) ||
                            !block.getMaterial().isSolid() ||
                            (block.getMaterial() == LMaterial.plants || block.getMaterial() == LMaterial.leaves)) &&
                            posY > world.getMinBuildHeight() + 1 && brake <= 15) {
                        L.setBlock(world, posX, posY, posZ, placeBlock, meta, 2);
                        block = L.getBlock(world, posX, --posY, posZ);

                        brake++;
                    }
                }
            }
        }
    }

    /** Setzt Bloecke auf vorhandenen Boden (nicht auf Fluessigkeiten), bis zu einer Hoehe - Zaeune, Mauern. */
    protected void placeBlocksOnTop(LevelAccessor world, BoundingBox box, LB block, int minX, int minZ, int maxX, int maxZ, int height) {

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int posX = this.getXWithOffset(x, z);
                int posZ = this.getZWithOffset(x, z);
                int topHeight = L.getTopSolidOrLiquidBlock(world, posX, posZ);

                if (!L.getBlock(world, posX, topHeight, posZ).getMaterial().isLiquid()) {

                    for (int i = 0; i < height; i++) {
                        int posY = topHeight + i;

                        L.setBlock(world, posX, posY, posZ, block, 0, 2);
                    }
                }
            }
        }
    }

    // ============ getXWithOffset & getZWithOffset, die wirklich drehen statt spiegeln ============

    public int getXWithOffset(int x, int z) {
        switch (this.coordBaseMode) {
            case 0:
                return this.boundingBox.minX() + x;
            case 1:
                return this.boundingBox.maxX() - z;
            case 2:
                return this.boundingBox.maxX() - x;
            case 3:
                return this.boundingBox.minX() + z;
            default:
                return x;
        }
    }

    public int getYWithOffset(int y) {
        return this.boundingBox.minY() + y;
    }

    public int getZWithOffset(int x, int z) {
        switch (this.coordBaseMode) {
            case 0:
                return this.boundingBox.minZ() + z;
            case 1:
                return this.boundingBox.minZ() + x;
            case 2:
                return this.boundingBox.maxZ() - z;
            case 3:
                return this.boundingBox.maxZ() - x;
            default:
                return z;
        }
    }

    // ============ 1.7.10 StructureComponent ============

    protected void placeBlockAtCurrentPosition(LevelAccessor world, LB block, int meta, int x, int y, int z, BoundingBox box) {
        int i1 = this.getXWithOffset(x, z);
        int j1 = this.getYWithOffset(y);
        int k1 = this.getZWithOffset(x, z);

        if (box.isInside(i1, j1, k1)) {
            L.setBlock(world, i1, j1, k1, block, meta, 2);
        }
    }

    protected LB getBlockAtCurrentPosition(LevelAccessor world, int x, int y, int z, BoundingBox box) {
        int l = this.getXWithOffset(x, z);
        int i1 = this.getYWithOffset(y);
        int j1 = this.getZWithOffset(x, z);
        return !box.isInside(l, i1, j1) ? VB.air : L.getBlock(world, l, i1, j1);
    }

    /** {@code func_151554_b}: nach unten auffuellen, solange Luft oder Fluessigkeit. */
    protected void func_151554_b(LevelAccessor world, LB block, int meta, int x, int y, int z, BoundingBox box) {
        int i1 = this.getXWithOffset(x, z);
        int j1 = this.getYWithOffset(y);
        int k1 = this.getZWithOffset(x, z);

        if (box.isInside(i1, j1, k1)) {
            while ((L.isAirBlock(world, i1, j1, k1) || L.getBlock(world, i1, j1, k1).getMaterial().isLiquid()) && j1 > world.getMinBuildHeight() + 1) {
                L.setBlock(world, i1, j1, k1, block, meta, 2);
                --j1;
            }
        }
    }

    protected boolean generateStructureChestContents(LevelAccessor world, BoundingBox box, Random rand, int x, int y, int z, ItemPool.WeightedContent[] items, int count) {
        int i1 = this.getXWithOffset(x, z);
        int j1 = this.getYWithOffset(y);
        int k1 = this.getZWithOffset(x, z);

        if (box.isInside(i1, j1, k1) && L.getBlock(world, i1, j1, k1) != VB.chest) {
            L.setBlock(world, i1, j1, k1, VB.chest, 0, 2);
            BlockEntity chest = L.getTileEntity(world, i1, j1, k1);

            if (chest != null) {
                ItemPool.generateChestContents(rand, items, chest, count);
            }

            return true;
        } else {
            return false;
        }
    }

    /** 1.7.10 {@code getMetadataWithOffset} (Vanilla-Drehung fuer Schienen, Tueren, Treppen, Leitern, Knoepfe ...). */
    protected int getMetadataWithOffset(LB block, int meta) {
        if (block == VB.rail) {
            if (this.coordBaseMode == 1 || this.coordBaseMode == 3) {
                if (meta == 1) return 0;
                return 1;
            }
        } else if (block != VB.wooden_door && block != VB.iron_door) {
            if (block != VB.stone_stairs && block != VB.oak_stairs && block != VB.nether_brick_stairs && block != VB.stone_brick_stairs && block != VB.sandstone_stairs) {
                if (block == VB.ladder) {
                    if (this.coordBaseMode == 0) {
                        if (meta == 2) return 3;
                        if (meta == 3) return 2;
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 2) return 4;
                        if (meta == 3) return 5;
                        if (meta == 4) return 2;
                        if (meta == 5) return 3;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 2) return 5;
                        if (meta == 3) return 4;
                        if (meta == 4) return 2;
                        if (meta == 5) return 3;
                    }
                } else if (block == VB.stone_button) {
                    if (this.coordBaseMode == 0) {
                        if (meta == 3) return 4;
                        if (meta == 4) return 3;
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 3) return 1;
                        if (meta == 4) return 2;
                        if (meta == 2) return 3;
                        if (meta == 1) return 4;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 3) return 2;
                        if (meta == 4) return 1;
                        if (meta == 2) return 3;
                        if (meta == 1) return 4;
                    }
                } else if (block != VB.tripwire_hook) {
                    if (block == VB.lever || block == VB.dispenser) {
                        if (this.coordBaseMode == 0) {
                            if (meta == 2 || meta == 3) return meta ^ 1; // Facing.oppositeSide
                        } else if (this.coordBaseMode == 1) {
                            if (meta == 2) return 4;
                            if (meta == 3) return 5;
                            if (meta == 4) return 2;
                            if (meta == 5) return 3;
                        } else if (this.coordBaseMode == 3) {
                            if (meta == 2) return 5;
                            if (meta == 3) return 4;
                            if (meta == 4) return 2;
                            if (meta == 5) return 3;
                        }
                    }
                } else {
                    if (this.coordBaseMode == 0) {
                        if (meta == 0 || meta == 2) return meta ^ 2; // Direction.rotateOpposite
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 2) return 1;
                        if (meta == 0) return 3;
                        if (meta == 1) return 2;
                        if (meta == 3) return 0;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 2) return 3;
                        if (meta == 0) return 1;
                        if (meta == 1) return 2;
                        if (meta == 3) return 0;
                    }
                }
            } else if (this.coordBaseMode == 0) {
                if (meta == 2) return 3;
                if (meta == 3) return 2;
            } else if (this.coordBaseMode == 1) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 0;
                if (meta == 3) return 1;
            } else if (this.coordBaseMode == 3) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 1;
                if (meta == 3) return 0;
            }
        } else if (this.coordBaseMode == 0) {
            if (meta == 0) return 2;
            if (meta == 2) return 0;
        } else {
            if (this.coordBaseMode == 1) return meta + 1 & 3;
            if (this.coordBaseMode == 3) return meta + 3 & 3;
        }

        return meta;
    }

    /** 1.7.10 {@code StructureComponent.findIntersecting}. */
    public static StructurePiece findIntersecting(List<StructurePiece> components, BoundingBox box) {
        for (StructurePiece component : components) {
            if (component.getBoundingBox() != null && component.getBoundingBox().intersects(box)) {
                return component;
            }
        }
        return null;
    }

    // ============ Fuellmethoden (optimiert, ohne unnoetige Ersetzungslogik) ============

    protected void fillWithAir(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        L.setBlock(world, posX, posY, posZ, VB.air, 0, 2);
                    }
                }
            }
        }
    }

    protected void fillWithBlocks(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block, LB replaceBlock, boolean onlyReplace) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (!onlyReplace || L.getBlock(world, posX, posY, posZ).getMaterial() != LMaterial.air) {
                            if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ)
                                L.setBlock(world, posX, posY, posZ, replaceBlock, 0, 2);
                            else
                                L.setBlock(world, posX, posY, posZ, block, 0, 2);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithBlocks(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        L.setBlock(world, posX, posY, posZ, block, 0, 2);
                    }
                }
            }
        }
    }

    protected void fillWithMetadataBlocks(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block, int meta, LB replaceBlock, int replaceMeta, boolean onlyReplace) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (!onlyReplace || L.getBlock(world, posX, posY, posZ).getMaterial() != LMaterial.air) {
                            if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ)
                                L.setBlock(world, posX, posY, posZ, replaceBlock, replaceMeta, 2);
                            else
                                L.setBlock(world, posX, posY, posZ, block, meta, 2);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithMetadataBlocks(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block, int meta) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        L.setBlock(world, posX, posY, posZ, block, meta, 2);
                    }
                }
            }
        }
    }

    protected void fillWithRandomizedBlocks(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean onlyReplace, Random rand, LegacySelector selector) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (!onlyReplace || L.getBlock(world, posX, posY, posZ).getMaterial() != LMaterial.air) {
                            selector.selectBlocks(rand, posX, posY, posZ, x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                            L.setBlock(world, posX, posY, posZ, selector.block, selector.meta, 2);
                        }
                    }
                }
            }
        }
    }

    // TODO des Originals: den BlockSelector durch etwas Besseres ersetzen
    protected void fillWithRandomizedBlocks(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Random rand, LegacySelector selector) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);
                        // fuer die meisten Strukturen ueberfluessig, Vanilla verlaesst sich aber darauf
                        selector.selectBlocks(rand, posX, posY, posZ, false);
                        L.setBlock(world, posX, posY, posZ, selector.block, selector.meta, 2);
                    }
                }
            }
        }
    }

    // Treppen und Co.
    protected void fillWithRandomizedBlocksMeta(LevelAccessor world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Random rand, LegacySelector selector, int meta) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);
                        selector.selectBlocks(rand, posX, posY, posZ, false);
                        L.setBlock(world, posX, posY, posZ, selector.block, meta | selector.meta, 2); // Meta im Konstruktor setzen!
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(LevelAccessor world, BoundingBox box, Random rand, float randLimit, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block, LB replaceBlock, boolean onlyReplace) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (rand.nextFloat() <= randLimit && (!onlyReplace || L.getBlock(world, posX, posY, posZ).getMaterial() != LMaterial.air)) {
                            if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ)
                                L.setBlock(world, posX, posY, posZ, replaceBlock, 0, 2);
                            else
                                L.setBlock(world, posX, posY, posZ, block, 0, 2);
                        }
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(LevelAccessor world, BoundingBox box, Random rand, float randLimit, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (rand.nextFloat() <= randLimit)
                            L.setBlock(world, posX, posY, posZ, block, 0, 2);
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(LevelAccessor world, BoundingBox box, Random rand, float randLimit, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LB block, int meta) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (rand.nextFloat() <= randLimit)
                            L.setBlock(world, posX, posY, posZ, block, meta, 2);
                    }
                }
            }
        }
    }

    protected ForgeDirection getDirection(ForgeDirection dir) {
        switch (coordBaseMode) {
            default: // Sued
                return dir;
            case 1: // West
                return dir.getRotation(ForgeDirection.UP);
            case 2: // Nord
                return dir.getOpposite();
            case 3: // Ost
                return dir.getRotation(ForgeDirection.DOWN);
        }
    }

    /**
     * Kernblock eines Mehrblock-Bauwerks (BlockDummyable). 'dir' zeigt zum Spieler (Gegenrichtung der Blickrichtung).
     * Port: Kern mit FACING; die Teile setzt die Selbstreparatur der Port-Mehrblocklogik beim Laden des Chunks.
     */
    protected void placeCore(LevelAccessor world, BoundingBox box, LB block, ForgeDirection dir, int x, int y, int z) {
        int posX = getXWithOffset(x, z);
        int posZ = getZWithOffset(x, z);
        int posY = getYWithOffset(y);

        if (!box.isInside(posX, posY, posZ)) return;

        if (dir == null)
            dir = ForgeDirection.NORTH;

        dir = getDirection(dir.getOpposite());
        BlockState state = block.state(0);
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && dir.toDirection() != null && dir.toDirection().getAxis().isHorizontal()) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir.toDirection());
        } else if (state.hasProperty(BlockStateProperties.FACING) && dir.toDirection() != null) {
            state = state.setValue(BlockStateProperties.FACING, dir.toDirection());
        }
        L.setState(world, posX, posY, posZ, state, 2);
    }

    /**
     * Original: {@code MultiblockHandlerXR.fillSpace} innerhalb des Rahmens. Port: entfaellt - die Port-Mehrbloecke
     * ergaenzen ihre Teile selbst ({@code attemptAutoRepair} beim Laden des Kerns).
     */
    protected void fillSpace(LevelAccessor world, BoundingBox box, int x, int y, int z, int[] dim, LB block, ForgeDirection dir) {
    }

    /** Original: {@code BlockDummyable.makeExtra}. Port: entfaellt (Anschlusszellen kennt der Port-Mehrblock selbst). */
    public void makeExtra(LevelAccessor world, BoundingBox box, LB block, int x, int y, int z) {
    }

    // ================================ Block-Selektoren ================================

    /** 1.7.10 {@code StructureComponent.BlockSelector} mit Block + Metadatum (fuer die Meta-Tabellen). */
    public abstract static class LegacySelector extends com.hbm_m.world.gen.nbt.BlockSelector {

        protected LB block = VB.air;
        protected int meta;

        @Override
        public abstract void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior);

        public void selectBlocks(Random rand, int posX, int posY, int posZ, boolean notInterior) {
            selectBlocks(RandomSource.create(rand.nextLong()), posX, posY, posZ, notInterior);
        }

        public LB func_151561_a() {
            return block;
        }

        public int getSelectedBlockMetaData() {
            return meta;
        }

        @Override
        public BlockState getSelected() {
            return func_151561_a().state(getSelectedBlockMetaData());
        }
    }

    static class Sandstone extends LegacySelector {

        Sandstone() { }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance > 0.6F) {
                this.block = VB.sandstone;
            } else if (chance < 0.5F) {
                this.block = MB.reinforced_sand;
            } else {
                this.block = VB.sand;
            }
        }
    }

    static class ConcreteBricks extends LegacySelector {

        ConcreteBricks() { }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.4F) {
                this.block = MB.brick_concrete;
            } else if (chance < 0.7F) {
                this.block = MB.brick_concrete_mossy;
            } else if (chance < 0.9F) {
                this.block = MB.brick_concrete_cracked;
            } else {
                this.block = MB.brick_concrete_broken;
            }
        }
    }

    static class ConcreteBricksStairs extends LegacySelector {

        ConcreteBricksStairs() {
            this.meta = 0;
        }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.4F) {
                this.block = MB.brick_concrete_stairs;
            } else if (chance < 0.7F) {
                this.block = MB.brick_concrete_mossy_stairs;
            } else if (chance < 0.9F) {
                this.block = MB.brick_concrete_cracked_stairs;
            } else {
                this.block = MB.brick_concrete_broken_stairs;
            }
        }
    }

    static class ConcreteBricksSlabs extends LegacySelector {

        ConcreteBricksSlabs() {
            this.block = MB.concrete_brick_slab;
            this.meta = 0;
        }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance >= 0.4F && chance < 0.7F) {
                this.meta |= 1;
            } else if (chance < 0.9F) {
                this.meta |= 2;
            } else {
                this.meta |= 3;
            }
        }
    }

    // ag
    static class LabTiles extends LegacySelector {

        LabTiles() { }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.5F) {
                this.block = MB.tile_lab;
            } else if (chance < 0.9F) {
                this.block = MB.tile_lab_cracked;
            } else {
                this.block = MB.tile_lab_broken;
            }
        }
    }

    static class SuperConcrete extends LegacySelector {

        SuperConcrete() {
            this.block = MB.concrete_super;
        }

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            this.meta = rand.nextInt(6) + 10;
        }
    }

    public static class MeteorBricks extends com.hbm_m.world.gen.nbt.BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.4F) {
                this.selected = ModBlocks.METEOR_BRICK.get().defaultBlockState();
            } else if (chance < 0.7F) {
                this.selected = ModBlocks.METEOR_BRICK_MOSSY.get().defaultBlockState();
            } else {
                this.selected = ModBlocks.METEOR_BRICK_CRACKED.get().defaultBlockState();
            }
        }
    }

    public static class SupplyCrates extends com.hbm_m.world.gen.nbt.BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.6F) {
                this.selected = Blocks.AIR.defaultBlockState();
            } else if (chance < 0.8F) {
                this.selected = ModBlocks.CRATE_AMMO.get().defaultBlockState();
            } else if (chance < 0.9F) {
                this.selected = ModBlocks.CRATE_CAN.get().defaultBlockState();
            } else {
                this.selected = ModBlocks.CRATE.get().defaultBlockState();
            }
        }
    }

    public static class CrabSpawners extends com.hbm_m.world.gen.nbt.BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.8F) {
                this.selected = ModBlocks.METEOR_BRICK.get().defaultBlockState();
            } else {
                this.selected = ModBlocks.METEOR_SPAWNER.get().defaultBlockState();
            }
        }
    }

    public static class GreenOoze extends com.hbm_m.world.gen.nbt.BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.8F) {
                this.selected = ModBlocks.TOXIC_BLOCK.get().defaultBlockState();
            } else {
                this.selected = ModBlocks.METEOR_POLISHED.get().defaultBlockState();
            }
        }
    }
}
