package com.hbm_m.item.tool;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.AttributeOps;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.api.block.IToolable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.tool.ItemToolingWeapon} ({@code wrench_archineer}: WRENCH, 1000 Haltbarkeit, 12 Schaden).
 * Der Schlag nutzt das Werkzeug nicht ab (hitEntity gibt false zurueck).
 */
public class ItemToolingWeapon extends ItemTooling {

    protected float damage = 0;

    public ItemToolingWeapon(IToolable.ToolType type, int durability, float damage, Properties properties) {
        super(type, durability, properties);
        this.damage = damage;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity player) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    //? if < 1.21.1 {
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
    //?} else {
    /*public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return com.hbm_m.platform.AttributeHooks.fromSlots(this::hbmSlotModifiers);
    }

    private Multimap<net.minecraft.core.Holder<Attribute>, AttributeModifier> hbmSlotModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of();
    *///?}
        return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE, com.hbm_m.platform.AttributeHooks.modifier(com.hbm_m.platform.AttributeHooks.BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damage, AttributeOps.ADDITION));
    }
}
