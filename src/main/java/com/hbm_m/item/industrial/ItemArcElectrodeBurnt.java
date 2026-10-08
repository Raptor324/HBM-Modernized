package com.hbm_m.item.industrial;

import com.hbm_m.item.industrial.ItemArcElectrode.EnumElectrodeType;

import net.minecraft.world.item.Item;

/** 1:1 {@code ItemArcElectrodeBurnt}: geschmolzene Elektrode, je Werkstoff ein Item. */
public class ItemArcElectrodeBurnt extends Item {

    public final EnumElectrodeType type;

    public ItemArcElectrodeBurnt(Properties properties, EnumElectrodeType type) {
        super(properties);
        this.type = type;
    }
}
