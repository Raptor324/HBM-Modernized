package com.hbm_m.item.missile;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code MissileStruct}: Sprengkopf, Rumpf, Leitwerk und Triebwerk einer Baukasten-Rakete. */
public class MissileStruct {

    @Nullable public ItemCustomMissilePart warhead;
    @Nullable public ItemCustomMissilePart fuselage;
    @Nullable public ItemCustomMissilePart fins;
    @Nullable public ItemCustomMissilePart thruster;

    public MissileStruct() { }

    public MissileStruct(@Nullable Item w, @Nullable Item f, @Nullable Item s, @Nullable Item t) {
        if (w instanceof ItemCustomMissilePart p) warhead = p;
        if (f instanceof ItemCustomMissilePart p) fuselage = p;
        if (s instanceof ItemCustomMissilePart p) fins = p;
        if (t instanceof ItemCustomMissilePart p) thruster = p;
    }

    public MissileStruct(ItemStack w, ItemStack f, ItemStack s, ItemStack t) {
        this(w == null ? null : w.getItem(), f == null ? null : f.getItem(), s == null ? null : s.getItem(), t == null ? null : t.getItem());
    }
}
