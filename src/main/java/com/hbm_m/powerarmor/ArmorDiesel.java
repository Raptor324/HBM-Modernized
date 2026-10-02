package com.hbm_m.powerarmor;

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

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != this.getEquipmentSlot()) return base;
        return ArmorAttributes.with(base, java.util.Map.entry(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(ArmorAttributes.fixed(this.getType()), "Armor modifier", 0.25D, AttributeModifier.Operation.MULTIPLY_BASE)));
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
