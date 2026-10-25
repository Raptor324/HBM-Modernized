package com.hbm_m.item.tool;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tier;

/**
 * 1:1 {@code com.hbm.items.tool.ModHoe}: die 1.7.10-Hacke ({@code ItemHoe}) - pflugt, nutzt sich ab, hat aber
 * keine Angriffswerte.
 */
public class ModHoe extends HoeItem {

    public ModHoe(Tier material) {
        this(material, new Properties());
    }

    public ModHoe(Tier material, Properties properties) {
        //? if < 1.21.1 {
        super(material, 0, 0F, material.getUses() == 0 ? properties.stacksTo(1) : properties);
        //?} else {
        /*super(material, material.getUses() == 0 ? properties.stacksTo(1) : properties);
        *///?}
    }

    //? if < 1.21.1 {
    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return ImmutableMultimap.of();
    }
    //?} else {
    /*// 1.21.1: ohne Properties.attributes(..) hat die Hacke ohnehin keine Angriffswerte
    *///?}
}
