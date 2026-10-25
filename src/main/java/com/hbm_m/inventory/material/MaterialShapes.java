package com.hbm_m.inventory.material;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1 {@code com.hbm.inventory.material.MaterialShapes}: Formen mit ihrer Menge in Quanten (1/72 Barren) und den
 * Ore-Dictionary-Praefixen. Zusaetzlich die Port-Namensmuster der Formen-Items ({@code %s} = Port-Material-ID) und
 * der Forge-Tag-Ordner.
 */
public class MaterialShapes {

    public static final List<MaterialShapes> allShapes = new ArrayList<>();
    /** Original {@code Mats.prefixByName} - hier gehalten, damit die Initialisierung nicht im Kreis laeuft. */
    public static final java.util.Map<String, MaterialShapes> prefixByName = new java.util.LinkedHashMap<>();

    public static final MaterialShapes ANY = new MaterialShapes(0, "any").noAutogen();
    public static final MaterialShapes ONLY_ORE = new MaterialShapes(0, "ore").noAutogen();
    public static final MaterialShapes ORE = new MaterialShapes(0, "ore", "oreNether").noAutogen();
    public static final MaterialShapes ORENETHER = new MaterialShapes(0, "oreNether").noAutogen();

    public static final MaterialShapes QUANTUM = new MaterialShapes(1); // 1/72 of an ingot
    public static final MaterialShapes NUGGET = new MaterialShapes(8, "nugget", "tiny").pattern("nugget_%s").tag("nuggets");
    public static final MaterialShapes TINY = new MaterialShapes(8, "tiny").noAutogen();
    public static final MaterialShapes FRAGMENT = new MaterialShapes(8, "bedrockorefragment").pattern("bedrock_ore_fragment_%s");
    public static final MaterialShapes DUSTTINY = new MaterialShapes(NUGGET.quantity, "dustTiny").pattern("%s_powder_tiny").tag("tiny_dusts");
    public static final MaterialShapes WIRE = new MaterialShapes(9, "wireFine").pattern("wire_%s").tag("wires_fine");
    public static final MaterialShapes BOLT = new MaterialShapes(9, "bolt").pattern("bolt_%s").tag("bolts");
    public static final MaterialShapes BILLET = new MaterialShapes(NUGGET.quantity * 6, "billet").pattern("billet_%s");
    public static final MaterialShapes INGOT = new MaterialShapes(NUGGET.quantity * 9, "ingot").pattern("%s_ingot", "ingot_%s").tag("ingots");
    public static final MaterialShapes GEM = new MaterialShapes(INGOT.quantity, "gem").pattern("gem_%s").tag("gems");
    public static final MaterialShapes CRYSTAL = new MaterialShapes(INGOT.quantity, "crystal").pattern("crystal_%s");
    public static final MaterialShapes DUST = new MaterialShapes(INGOT.quantity, "dust").pattern("%s_powder", "powder_%s").tag("dusts", "powders");
    public static final MaterialShapes DENSEWIRE = new MaterialShapes(INGOT.quantity, "wireDense").pattern("wire_dense_%s");
    public static final MaterialShapes PLATE = new MaterialShapes(INGOT.quantity, "plate").pattern("plate_%s").tag("plates");
    public static final MaterialShapes CASTPLATE = new MaterialShapes(INGOT.quantity * 3, "plateTriple").pattern("plate_cast_%s");
    public static final MaterialShapes WELDEDPLATE = new MaterialShapes(INGOT.quantity * 6, "plateSextuple").pattern("plate_welded_%s");
    public static final MaterialShapes SHELL = new MaterialShapes(INGOT.quantity * 4, "shell").pattern("shell_%s");
    public static final MaterialShapes PIPE = new MaterialShapes(INGOT.quantity * 3, "ntmpipe").pattern("pipe_%s");
    public static final MaterialShapes QUART = new MaterialShapes(162);
    public static final MaterialShapes BLOCK = new MaterialShapes(INGOT.quantity * 9, "block").pattern("block_%s").tag("storage_blocks");

    public static final MaterialShapes LIGHTBARREL =   new MaterialShapes(INGOT.quantity * 3, "barrelLight").pattern("part_barrel_light_%s");
    public static final MaterialShapes HEAVYBARREL =   new MaterialShapes(INGOT.quantity * 6, "barrelHeavy").pattern("part_barrel_heavy_%s");
    public static final MaterialShapes LIGHTRECEIVER = new MaterialShapes(INGOT.quantity * 4, "receiverLight").pattern("part_receiver_light_%s");
    public static final MaterialShapes HEAVYRECEIVER = new MaterialShapes(INGOT.quantity * 9, "receiverHeavy").pattern("part_receiver_heavy_%s");
    public static final MaterialShapes MECHANISM =     new MaterialShapes(INGOT.quantity * 4, "gunMechanism").pattern("part_mechanism_%s");
    public static final MaterialShapes STOCK =         new MaterialShapes(INGOT.quantity * 4, "stock").pattern("part_stock_%s");
    public static final MaterialShapes GRIP =          new MaterialShapes(INGOT.quantity * 2, "grip").pattern("part_grip_%s");

    public boolean noAutogen = false;
    private final int quantity;
    public final String[] prefixes;
    /** Port: Namensmuster der Items dieser Form. */
    public String[] portPatterns = new String[0];
    /** Port: Forge-Tag-Ordner dieser Form ({@code forge:<ordner>/<material>}). */
    public String[] tagFolders = new String[0];

    private MaterialShapes(int quantity, String... prefixes) {
        this.quantity = quantity;
        this.prefixes = prefixes;

        for (String prefix : prefixes) {
            prefixByName.put(prefix, this);
        }

        allShapes.add(this);
    }

    /** Disables recipe autogen for special cases like compatibility prefixes (TINY, ORENETHER), technical prefixes (ANY) or prefixes that have to be handled manually (ORE) */
    public MaterialShapes noAutogen() {
        this.noAutogen = true;
        return this;
    }

    private MaterialShapes pattern(String... patterns) {
        this.portPatterns = patterns;
        return this;
    }

    private MaterialShapes tag(String... folders) {
        this.tagFolders = folders;
        return this;
    }

    public int q(int amount) {
        return this.quantity * amount;
    }

    public int q(int unitsUsed, int itemsProduced) { //eg rails: INOGT.q(6, 16) since the recipe uses 6 iron ingots producing 16 individual rail blocks
        return this.quantity * unitsUsed / itemsProduced;
    }

    public String name() {
        return (prefixes != null && prefixes.length > 0) ? prefixes[0] : "unknown";
    }

    public String make(NTMMaterial mat) {
        return this.name() + mat.names[0];
    }
}
