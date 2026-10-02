package com.hbm_m.item.tool;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 1:1 {@code com.hbm.items.tool.BigSword} ("Great Sword"): ein Schwert, dessen Rechtsklick nichts tut
 * (die Plasma-Strahl-Nutzung ist im Original auskommentiert).
 */
public class BigSword extends SwordItem {

    public BigSword(Tier material) {
        super(material, 4, -2.4F, new Properties());
    }
}
