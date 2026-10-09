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
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.helper.IParticleCreator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.armor.ArmorDiesel}: Dieselanzug ({@code dieselsuit_*}. */
public class ArmorDiesel extends ModArmorFSBFueled {

    public ArmorDiesel(ModArmorMaterials material, Type type, Properties properties, String texture,
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
        return ArmorAttributes.with(base, java.util.Map.entry(Attributes.KNOCKBACK_RESISTANCE,
                PlatformHooks.attributeModifier(ArmorAttributes.fixed(this.getType()), "Armor modifier", 0.25D, AttributeOps.MULTIPLY_BASE)));
    }

    @Override
    protected void armorTick(ItemStack stack, Level world, Player player) {
        super.armorTick(stack, world, player);

        if (!world.isClientSide && this == ModItems.DIESELSUIT_LEGS.get() && hasFSBArmor(player) && world.getGameTime() % 3 == 0) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "bnuuy");
            data.putInt("player", player.getId());
            IParticleCreator.sendPacket((ServerLevel) world, player.getX(), player.getY(), player.getZ(), 100, data);
        }
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return type == ModFluids.DIESEL.getSource() || type == ModFluids.DIESEL_CRACK.getSource();
    }
}
