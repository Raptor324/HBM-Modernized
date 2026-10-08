package com.hbm_m.powerarmor;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.AttributeOps;

import java.util.UUID;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;

/** Hilfen fuer die Original-Attributmodifikatoren der Ruestungen ({@code getItemAttributeModifiers}). */
public final class ArmorAttributes {

    private ArmorAttributes() {}

    /** Original {@code armorType}: 0 Helm, 1 Brust, 2 Beine, 3 Stiefel. */
    public static int index(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 0;
            case CHESTPLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
            //? if >= 1.21.1 {
            /*default -> 1;
            *///?}
        };
    }

    public static UUID fixed(ArmorItem.Type type) {
        return ArmorModificationHelper.fixedUUIDs[index(type)];
    }

    /** Vanilla-Werte des Slots plus die zusaetzlichen Modifikatoren. */
    @SafeVarargs
    public static <K> Multimap<K, AttributeModifier> with(Multimap<K, AttributeModifier> base,
                                                              java.util.Map.Entry<K, AttributeModifier>... extra) {
        ImmutableMultimap.Builder<K, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(base);
        for (var e : extra) builder.put(e.getKey(), e.getValue());
        return builder.build();
    }

    /** Original: {@code removeAttributeModifiers} und bei Bedarf {@code applyAttributeModifiers} mit fester UUID. */
    public static void speed(LivingEntity player, UUID uuid, String name, double amount, boolean apply) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        com.hbm_m.platform.AttributeHooks.removeModifier(speed, uuid);
        if (apply) speed.addTransientModifier(PlatformHooks.attributeModifier(uuid, name, amount, AttributeOps.ADDITION));
    }

    public static boolean isSlot(ArmorItem item, EquipmentSlot slot) {
        return item.getEquipmentSlot() == slot;
    }
}
