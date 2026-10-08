package com.hbm_m.powerarmor;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.AttributeOps;

import java.util.function.Supplier;

import com.google.common.collect.Multimap;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.material.Fluid;

/** 1:1 {@code com.hbm.items.armor.ArmorDesh}: Dampfanzug ({@code steamsuit_*}. */
public class ArmorDesh extends ModArmorFSBFueled {

    public ArmorDesh(ModArmorMaterials material, Type type, Properties properties, String texture,
              Supplier<Fluid> fuelType, int maxFuel, int fillRate, int consumption, int drain) {
        super(material, type, properties, texture, fuelType, maxFuel, fillRate, consumption, drain);
    }

    @Override
    public boolean isObjArmor() {
        return true;
    }

    //? if < 1.21.1 {
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
    //?} else {
    /*@Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(net.minecraft.world.item.ItemStack stack) {
        return com.hbm_m.platform.AttributeHooks.fromSlots(this::hbmSlotModifiers);
    }

    private Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> hbmSlotModifiers(EquipmentSlot slot) {
        Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> base =
                com.hbm_m.platform.AttributeHooks.forSlot(super.getDefaultAttributeModifiers(), slot);
    *///?}
        if (slot != this.getEquipmentSlot()) return base;
        return ArmorAttributes.with(base, java.util.Map.entry(Attributes.MOVEMENT_SPEED,
                PlatformHooks.attributeModifier(ArmorAttributes.fixed(this.getType()), "Armor modifier", -0.025D, AttributeOps.MULTIPLY_BASE)));
    }
}
