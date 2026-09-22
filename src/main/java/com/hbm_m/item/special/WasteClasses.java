package com.hbm_m.item.special;

import java.util.List;

import com.hbm_m.item.PartTabMetaItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port der beiden {@code WasteClass}-Tabellen aus {@code ItemWasteLong} und
 * {@code ItemWasteShort} (1.7.10).
 *
 * <p>Die Abfallklasse sagt, aus welchem Brennstoff ein Stueck Muell stammt - und damit, wieviel
 * fluessiger und gasfoermiger Abfall beim Zerfall im Lagerfass anfaellt. Das ist der eigentliche
 * Grund, warum die vielen Muellsorten ueberhaupt getrennt gefuehrt werden: Thorium-Muell gibt beim
 * Zerfallen praktisch nichts ab, Plutonium-241 und aufwaerts je einen vollen Eimer Gas. Ein
 * Fasslager legt man deshalb nicht pauschal aus, sondern nach dem schmutzigsten Brennstoff, den man
 * fahren will.</p>
 *
 * <p>Wo das Original die Klasse in den Metadaten eines Items fuehrt, hat dieser Port sie laengst zu
 * eigenen Items entfaltet ({@link PartTabMetaItems}, Gruppen {@code nw_long}, {@code nw_short} und
 * die zugehoerigen {@code _dep}-Gruppen). Die Reihenfolge dieser Gruppen entspricht genau der
 * Reihenfolge der Enum-Werte hier - Listenplatz {@code i} ist Klasse {@code i}.</p>
 */
public final class WasteClasses {

    private WasteClasses() {}

    /** 1:1 aus {@code ItemWasteLong.WasteClass}. */
    public enum Long {
        URANIUM235("Uranium-235", 0, 0),
        URANIUM233("Uranium-233", 0, 50),
        NEPTUNIUM("Neptunium-237", 0, 100),
        THORIUM("Thorium-232", 0, 0),
        SCHRABIDIUM("Schrabidium-326", 0, 250);

        public final String label;
        /** Fluessiger Abfall je Zerfall, in Millibucket. */
        public final int liquid;
        /** Gasfoermiger Abfall je Zerfall, in Millibucket. */
        public final int gas;

        Long(String label, int liquid, int gas) {
            this.label = label;
            this.liquid = liquid;
            this.gas = gas;
        }
    }

    /** 1:1 aus {@code ItemWasteShort.WasteClass}. */
    public enum Short {
        URANIUM235("Uranium-235", 0, 100),
        URANIUM233("Uranium-233", 50, 100),
        NEPTUNIUM("Neptunium-237", 150, 500),
        PLUTONIUM239("Plutonium-239", 250, 1000),
        PLUTONIUM240("Plutonium-240", 350, 1000),
        PLUTONIUM241("Plutonium-241", 500, 1000),
        AMERICIUM242("Americium-242", 750, 1000),
        SCHRABIDIUM("Schrabidium-326", 1000, 1000);

        public final String label;
        public final int liquid;
        public final int gas;

        Short(String label, int liquid, int gas) {
            this.label = label;
            this.liquid = liquid;
            this.gas = gas;
        }
    }

    /**
     * Ein Zerfallspfad: welche Gruppe zerfaellt in welche, wieviel dabei anfaellt und wie oft es
     * ueberhaupt passiert.
     *
     * @param fresh   die Gruppe des frischen Muells
     * @param spent   die Gruppe des abgereicherten Muells, gleicher Listenplatz
     * @param liquid  Fluessigkeit je Klasse, in Millibucket
     * @param gas     Gas je Klasse, in Millibucket
     */
    public record DecayPath(String fresh, String spent, int[] liquid, int[] gas, boolean tiny) {

        /**
         * Der Listenplatz des Stapels in dieser Gruppe, oder {@code -1} wenn er nicht dazugehoert.
         */
        public int indexOf(ItemStack stack) {
            List<Item> items = PartTabMetaItems.group(fresh);
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) == stack.getItem()) return i;
            }
            return -1;
        }

        /** Das abgereicherte Gegenstueck zum Listenplatz {@code index}. */
        public ItemStack spentStack(int index) {
            List<Item> items = PartTabMetaItems.group(spent);
            return index < items.size() ? new ItemStack(items.get(index)) : ItemStack.EMPTY;
        }

        /** Original: kleine Brocken liefern ein Zehntel. */
        public int liquidAt(int index) {
            int base = index < liquid.length ? liquid[index] : 0;
            return tiny ? base / 10 : base;
        }

        public int gasAt(int index) {
            int base = index < gas.length ? gas[index] : 0;
            return tiny ? base / 10 : base;
        }
    }

    private static int[] liquids(Long[] classes) {
        int[] out = new int[classes.length];
        for (int i = 0; i < classes.length; i++) out[i] = classes[i].liquid;
        return out;
    }

    private static int[] gases(Long[] classes) {
        int[] out = new int[classes.length];
        for (int i = 0; i < classes.length; i++) out[i] = classes[i].gas;
        return out;
    }

    private static int[] liquids(Short[] classes) {
        int[] out = new int[classes.length];
        for (int i = 0; i < classes.length; i++) out[i] = classes[i].liquid;
        return out;
    }

    private static int[] gases(Short[] classes) {
        int[] out = new int[classes.length];
        for (int i = 0; i < classes.length; i++) out[i] = classes[i].gas;
        return out;
    }

    /** Die vier Zerfallspfade des Lagerfasses, 1:1 in der Reihenfolge des Originals. */
    public static final List<DecayPath> LONG_PATHS = List.of(
            new DecayPath("nw_long", "nw_long_dep",
                    liquids(Long.values()), gases(Long.values()), false),
            new DecayPath("nw_long_tiny", "nw_long_dep_tiny",
                    liquids(Long.values()), gases(Long.values()), true));

    public static final List<DecayPath> SHORT_PATHS = List.of(
            new DecayPath("nw_short", "nw_short_dep",
                    liquids(Short.values()), gases(Short.values()), false),
            new DecayPath("nw_short_tiny", "nw_short_dep_tiny",
                    liquids(Short.values()), gases(Short.values()), true));
}
