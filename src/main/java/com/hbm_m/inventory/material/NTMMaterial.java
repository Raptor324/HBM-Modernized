package com.hbm_m.inventory.material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.network.chat.Component;

/**
 * 1:1 {@code com.hbm.inventory.material.NTMMaterial}: ein Stoff des Giessereisystems mit fester ID, Ore-Dictionary-
 * Namen, Schmelzverhalten, Farben und Umwandlung (Kohle -> Kohlenstoff usw.). Mengen werden in Quanten gerechnet
 * (1 Barren = 72). Zusaetzlich kennt jedes Material die Port-IDs, unter denen seine Formen-Items registriert sind.
 */
public class NTMMaterial {

    public final int id;
    public String[] names;
    public Set<MaterialShapes> autogen = new HashSet<>();
    public Set<MatTraits> traits = new HashSet<>();
    public SmeltingBehavior smeltable = SmeltingBehavior.NOT_SMELTABLE;
    public int solidColorLight = 0xFF4A00;
    public int solidColorDark = 0x802000;
    public int moltenColor = 0xFF4A00;

    public NTMMaterial smeltsInto;
    public int convIn;
    public int convOut;

    /** Port: Material-IDs der Port-Items ({@code <id>_ingot}, {@code plate_<id>} ...). */
    public final List<String> portIds = new ArrayList<>();

    public NTMMaterial(int id, String... names) {

        this.names = names;
        this.id = id;

        this.smeltsInto = this;
        this.convIn = 1;
        this.convOut = 1;

        for (String name : names) {
            Mats.matByName.put(name, this);
        }

        Mats.orderedList.add(this);
        Mats.matById.put(id, this);
    }

    public String getUnlocalizedName() {
        return "hbmmat." + this.names[0].toLowerCase(Locale.US);
    }

    public Component getLocalizedName() {
        return Component.translatable(getUnlocalizedName());
    }

    public NTMMaterial setConversion(NTMMaterial mat, int in, int out) {
        this.smeltsInto = mat;
        this.convIn = in;
        this.convOut = out;
        return this;
    }

    /** Shapes for autogen */
    public NTMMaterial setAutogen(MaterialShapes... shapes) {
        for (MaterialShapes shape : shapes) this.autogen.add(shape);
        return this;
    }

    /** Traits for recipe detection */
    public NTMMaterial setTraits(MatTraits... traits) {
        for (MatTraits trait : traits) this.traits.add(trait);
        return this;
    }

    public NTMMaterial m() { this.traits.add(MatTraits.METAL); return this; }
    public NTMMaterial n() { this.traits.add(MatTraits.NONMETAL); return this; }

    /** Defines smelting behavior */
    public NTMMaterial smeltable(SmeltingBehavior behavior) {
        this.smeltable = behavior;
        return this;
    }

    public NTMMaterial setSolidColor(int colorLight, int colorDark) {
        this.solidColorLight = colorLight;
        this.solidColorDark = colorDark;
        return this;
    }

    public NTMMaterial setMoltenColor(int color) {
        this.moltenColor = color;
        return this;
    }

    /** Port: Item-IDs dieses Materials im Port. */
    public NTMMaterial port(String... ids) {
        for (String s : ids) this.portIds.add(s);
        return this;
    }

    public enum SmeltingBehavior {
        NOT_SMELTABLE,  //anything that can't be smelted or otherwise doesn't belong in a smelter, like diamond.
        VAPORIZES,      //can't be smelted because the material would skadoodle
        BREAKS,         //can't be smelted because the material doesn't survive the temperatures
        SMELTABLE,      //mostly metal
        ADDITIVE        //stuff like coal which isn't smeltable but can be put in a crucible anyway
    }

    public enum MatTraits {
        METAL,      //metal(like), smeltable by arc furnaces
        NONMETAL;   //non-metal(like), for gems, non-alloy compounds and similar
    }
}
