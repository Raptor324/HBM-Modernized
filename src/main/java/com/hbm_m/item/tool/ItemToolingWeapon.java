package com.hbm_m.item.tool;

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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damage, AttributeModifier.Operation.ADDITION));
    }
}
