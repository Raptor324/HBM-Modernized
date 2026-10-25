package com.hbm_m.world.gen.nbt;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.block.Block;

/** 1:1 {@code com.hbm.world.gen.nbt.JigsawPiece}: ein baubares NBT-Teil samt Bau-Modifikatoren. */
public class JigsawPiece {

    // Uebersetzt einen Namen in ein Teil (Serialisierung)
    protected static final Map<String, JigsawPiece> jigsawMap = new HashMap<>();

    public final String name;
    public final NBTStructure structure;

    /** Ersetzt passende Bloecke durch das Ergebnis eines BlockSelectors. */
    public Map<Block, BlockSelector> blockTable;

    /** Verschiebt jede einzelne Spalte auf das Gelaende (Graeben, natuerliche Formationen). */
    public boolean conformToTerrain = false;

    /** Richtet dieses Teil einzeln auf die Gelaendehoehe aus, ohne Spalten zu verschieben (wie Dorfhaeuser). */
    public boolean alignToTerrain = false;

    /** Hoehenversatz; -1 versenkt den Boden buendig im Gelaende. */
    public int heightOffset = 0;

    /** Fuellt Luft unter dem Teil mit dem gegebenen Selector auf. */
    public BlockSelector platform;

    /** Groesser 0: begrenzt, wie oft dieses Teil in einer Struktur vorkommen darf. */
    public int instanceLimit = 0;

    /** Gesetzt: Generierung laeuft ueber die Grenzen hinaus weiter, bis dieses Teil mindestens einmal existiert. */
    public boolean required = false;

    public JigsawPiece(String name, NBTStructure structure) {
        this(name, structure, 0);
    }

    public JigsawPiece(String name, NBTStructure structure, int heightOffset) {
        if (name == null) throw new IllegalStateException("A severe error has occurred in NBTStructure! A jigsaw piece has been registered without a valid name!");
        if (jigsawMap.containsKey(name)) throw new IllegalStateException("A severe error has occurred in NBTStructure! A jigsaw piece has been registered with the same name as another: " + name);

        this.name = name;
        this.structure = structure;
        jigsawMap.put(name, this);

        this.heightOffset = heightOffset;
    }

    public static JigsawPiece get(String name) {
        return jigsawMap.get(name);
    }
}
