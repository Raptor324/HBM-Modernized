package com.hbm_m.multiblock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Uebersetzt die Multiblock-Beschreibung des 1.7.10-Originals
 * ({@code BlockDummyable#getAllDimensions()} + {@code makeExtra(...)}) 1:1 in einen
 * {@link MultiblockStructureHelper}.
 *
 * <h3>Koordinatensysteme</h3>
 * <p>Im Original ist die Referenzausrichtung {@code ForgeDirection.SOUTH} - nur dort gibt
 * {@code MultiblockHandlerXR.rotate} die Dimensionen unveraendert zurueck. Ein Quader wird als
 * {@code {U, D, N, S, W, E}} beschrieben, also als Ausdehnung in die sechs Richtungen um den Kern;
 * einzelne Werte duerfen negativ sein (dann liegt der Quader komplett auf einer Seite).</p>
 *
 * <p>Der {@link MultiblockStructureHelper} nutzt dagegen {@code NORTH} als Referenz
 * ({@link MultiblockStructureHelper#rotate}). Beide Systeme unterscheiden sich also um 180 Grad,
 * weshalb dieser Builder X und Z spiegelt:</p>
 * <pre>
 *   lokal.x = -kanonisch.x   ->  x aus [-E, +W]
 *   lokal.y =  kanonisch.y   ->  y aus [-D, +U]
 *   lokal.z = -kanonisch.z   ->  z aus [-S, +N]
 * </pre>
 *
 * <p>{@code makeExtra} wird im Original mit {@code dir} (Blickrichtung, kanonisch +Z) und
 * {@code rot = dir.getRotation(UP)} (kanonisch -X) gerechnet. {@link #extra(int, int, int)}
 * nimmt genau diese drei Faktoren entgegen.</p>
 *
 * <p>Der Platzierungs-Offset ({@code BlockDummyable#getOffset()}) ergibt sich beim Helper
 * automatisch aus dem kleinsten lokalen Z - das entspricht der Zelle, auf die der Spieler klickt.
 * Passt das bei einer Maschine nicht (weil das Original einen abweichenden Offset setzt), kann er
 * ueber {@link #placementOffset(int)} explizit gesetzt werden.</p>
 */
public final class DummyableStructureBuilder {

    /** Gewoehnlicher Dummy-Block ohne eigene Anbindung (Original: Meta 0-5). */
    private static final char SYMBOL_PART = 'O';
    /** Anschlusszelle aus {@code makeExtra} (Original: Meta 6-11, {@code TileEntityProxyCombo}). */
    private static final char SYMBOL_PORT = 'B';
    /** Der Kern selbst (Original: Meta 12+). */
    private static final char SYMBOL_CORE = 'C';

    private final Set<BlockPos> parts = new HashSet<>();
    private final Set<BlockPos> ports = new HashSet<>();
    private Integer forcedPlacementOffset = null;

    private DummyableStructureBuilder() {}

    public static DummyableStructureBuilder create() {
        return new DummyableStructureBuilder();
    }

    /**
     * Ein Quader aus {@code getDimensions()} / {@code getAllDimensions()} des Originals.
     * Reihenfolge wie dort: {@code up, down, north, south, west, east}.
     */
    public DummyableStructureBuilder box(int up, int down, int north, int south, int west, int east) {
        for (int x = -east; x <= west; x++) {
            for (int y = -down; y <= up; y++) {
                for (int z = -south; z <= north; z++) {
                    parts.add(new BlockPos(x, y, z));
                }
            }
        }
        return this;
    }

    /** Bequemlichkeitsvariante fuer die {@code int[]}-Arrays aus {@code getAllDimensions()}. */
    public DummyableStructureBuilder box(int[] dim) {
        return box(dim[0], dim[1], dim[2], dim[3], dim[4], dim[5]);
    }

    /**
     * Ein Quader, dessen Ursprung nicht der Kern ist - im Original ein
     * {@code MultiblockHandlerXR.fillSpace(...)} mit verschobenem Mittelpunkt
     * (z.B. die MHD-Turbine, die einen Kasten drei Bloecke vor dem Kern setzt).
     *
     * @param forward Vielfaches von {@code dir}
     * @param up      Vielfaches von {@code ForgeDirection.UP}
     * @param side    Vielfaches von {@code rot = dir.getRotation(UP)}
     */
    public DummyableStructureBuilder boxAt(int forward, int up, int side,
                                           int boxUp, int boxDown, int north, int south, int west, int east) {
        BlockPos origin = new BlockPos(side, up, -forward);
        for (int x = -east; x <= west; x++) {
            for (int y = -boxDown; y <= boxUp; y++) {
                for (int z = -south; z <= north; z++) {
                    parts.add(origin.offset(x, y, z));
                }
            }
        }
        return this;
    }

    /** Eine einzelne zusaetzliche Dummy-Zelle (kein Anschluss). */
    public DummyableStructureBuilder cell(int forward, int up, int side) {
        parts.add(new BlockPos(side, up, -forward));
        return this;
    }

    /**
     * Eine {@code makeExtra}-Zelle des Originals.
     *
     * @param forward Vielfaches von {@code dir} (Blickrichtung der Maschine)
     * @param up      Vielfaches von {@code ForgeDirection.UP}
     * @param side    Vielfaches von {@code rot = dir.getRotation(UP)}
     */
    public DummyableStructureBuilder extra(int forward, int up, int side) {
        ports.add(new BlockPos(side, up, -forward));
        return this;
    }

    /** Setzt {@code BlockDummyable#getOffset()} explizit, falls die Automatik nicht passt. */
    public DummyableStructureBuilder placementOffset(int offset) {
        this.forcedPlacementOffset = offset;
        return this;
    }

    public MultiblockStructureHelper build(Supplier<BlockState> phantomBlockState) {
        Map<BlockPos, Supplier<BlockState>> structureMap = new LinkedHashMap<>();
        Map<BlockPos, Character> positionSymbolMap = new LinkedHashMap<>();
        BlockPos core = BlockPos.ZERO;

        for (BlockPos pos : parts) {
            if (pos.equals(core) || ports.contains(pos)) continue;
            structureMap.put(pos, phantomBlockState);
            positionSymbolMap.put(pos, SYMBOL_PART);
        }

        for (BlockPos pos : ports) {
            if (pos.equals(core)) continue;
            structureMap.put(pos, phantomBlockState);
            positionSymbolMap.put(pos, SYMBOL_PORT);
        }

        positionSymbolMap.put(core, SYMBOL_CORE);

        Map<Character, PartRole> roleMap = new HashMap<>();
        roleMap.put(SYMBOL_PART, PartRole.DEFAULT);
        roleMap.put(SYMBOL_PORT, PartRole.UNIVERSAL_CONNECTOR);
        roleMap.put(SYMBOL_CORE, PartRole.CONTROLLER);

        MultiblockStructureHelper helper = new MultiblockStructureHelper(
                structureMap, phantomBlockState, roleMap, positionSymbolMap, core);

        if (forcedPlacementOffset != null) helper = helper.withPlacementOffset(forcedPlacementOffset);
        return helper;
    }
}
