package com.hbm_m.inventory.filter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port von {@code com.hbm.module.ModulePatternMatcher} (1.7.10 Original). Item-Filter-Modul, das
 * fuer eine feste Anzahl "Filter-Slots" jeweils einen Match-Modus haelt (keine eigene Inventar-Klasse
 * - die Filter-Items selbst leben im Hauptinventar der besitzenden BlockEntity, dieses Modul haelt
 * nur die parallele Modus-Metadaten pro Slot-Index, exakt wie im Original).
 * <p>
 * SCOPE-Vereinfachung: Das Original kennt neben EXACT/WILDCARD auch einen "bedrock"-Modus (spezielle
 * Bedrock-Erz-Gradient-Behandlung) und beliebige Ore-Dictionary-Schluessel-Strings als impliziten
 * vierten Modus. Ore-Dictionary existiert in diesem Port nicht 1:1 - stattdessen wird ein generischer
 * TAG-Modus verwendet, der gegen einen gespeicherten Forge-{@link TagKey} matcht (funktional
 * aequivalent zum Ore-Dict-Modus des Originals, moderner Weg). Der "bedrock"-Sondermodus entfaellt,
 * da er nur fuer den Pyro-Oven-Sonderfall relevant war (dort bereits separat geloest) und hier nicht
 * gebraucht wird.
 */
public class ModulePatternMatcher {

    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public static final String MODE_TAG_PREFIX = "tag:";

    private final String[] modes;

    public ModulePatternMatcher(int size) {
        this.modes = new String[size];
    }

    public int size() {
        return modes.length;
    }

    public String getMode(int index) {
        return modes[index];
    }

    public void setMode(int index, String mode) {
        modes[index] = mode;
    }

    /** Waehlt den Startmodus, wenn ein Filter-Item in Slot {@code index} platziert wird. */
    public void initPattern(int index, ItemStack filterStack) {
        modes[index] = filterStack.isEmpty() ? null : MODE_EXACT;
    }

    /** Zykelt EXACT -> WILDCARD -> EXACT (Rechtsklick auf den Filter-Slot in der GUI). */
    public void nextMode(int index) {
        String current = modes[index];
        if (current == null) return;
        modes[index] = MODE_EXACT.equals(current) ? MODE_WILDCARD : MODE_EXACT;
    }

    public boolean isValidForFilter(ItemStack filterStack, int index, ItemStack input) {
        if (index < 0 || index >= modes.length) return false;
        String mode = modes[index];
        if (mode == null || filterStack.isEmpty()) return false;
        if (input.isEmpty()) return false;

        if (MODE_WILDCARD.equals(mode)) return true;

        if (mode.startsWith(MODE_TAG_PREFIX)) {
            ResourceLocation tagId = ResourceLocation.tryParse(mode.substring(MODE_TAG_PREFIX.length()));
            if (tagId == null) return false;
            TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
            return input.is(tag);
        }

        // MODE_EXACT (default fallback fuer unbekannte Werte)
        return ItemStack.isSameItem(filterStack, input);
    }

    public static String getLabel(String mode) {
        if (mode == null) return "";
        if (MODE_WILDCARD.equals(mode)) return "Any";
        if (mode.startsWith(MODE_TAG_PREFIX)) return "Tag: " + mode.substring(MODE_TAG_PREFIX.length());
        return "Exact";
    }

    public void writeToNBT(CompoundTag tag) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) tag.putString("mode" + i, modes[i]);
        }
    }

    public void readFromNBT(CompoundTag tag) {
        for (int i = 0; i < modes.length; i++) {
            modes[i] = tag.contains("mode" + i) ? tag.getString("mode" + i) : null;
        }
    }
}
