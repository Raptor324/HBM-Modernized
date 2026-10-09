package com.hbm_m.platform;

import java.util.UUID;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Versionsfassade fuer Attribut-Modifikatoren an Items und Wesen (Phase B, Agent C).
 *
 * <p>1.20.1: Modifikatoren tragen eine UUID, Items liefern {@code getDefaultAttributeModifiers(EquipmentSlot)}
 * als {@code Multimap<Attribute, AttributeModifier>}.<br>
 * 1.21.1: Modifikatoren tragen eine {@code ResourceLocation} (aus der UUID abgeleitet, siehe
 * {@link PlatformHooks#attributeModifier(UUID, String, double, AttributeModifier.Operation)}), Items liefern
 * {@code getDefaultAttributeModifiers(ItemStack)} als {@code ItemAttributeModifiers}-Komponente mit Slotgruppen.
 * Die Port-Items behalten ihre Slot-Logik und werden ueber {@code fromSlots}/{@code forSlot} umgesetzt.</p>
 */
public final class AttributeHooks {
    private AttributeHooks() {}

    /** Vanilla {@code Item.BASE_ATTACK_DAMAGE_UUID} (1.20.1 nur fuer Unterklassen sichtbar). */
    public static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    /** Vanilla {@code Item.BASE_ATTACK_SPEED_UUID}. */
    public static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * Wie {@link PlatformHooks#attributeModifier(UUID, String, double, AttributeModifier.Operation)}; auf 1.21.1
     * werden die beiden Vanilla-Basis-UUIDs auf {@code Item.BASE_ATTACK_DAMAGE_ID}/{@code BASE_ATTACK_SPEED_ID}
     * abgebildet (Tooltip rechnet dort wie 1.20.1 den Grundwert des Spielers ein).
     */
    public static AttributeModifier modifier(UUID uuid, String name, double value, AttributeModifier.Operation op) {
        //? if >= 1.21.1 {
        /*if (BASE_ATTACK_DAMAGE_UUID.equals(uuid)) return new AttributeModifier(net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID, value, op);
        if (BASE_ATTACK_SPEED_UUID.equals(uuid)) return new AttributeModifier(net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID, value, op);
        *///?}
        return PlatformHooks.attributeModifier(uuid, name, value, op);
    }

    //? if >= 1.21.1 {
    /*/^* Kennung eines Modifikators, der mit {@link #modifier}/{@link PlatformHooks#attributeModifier} aus einer UUID entstand. ^/
    public static net.minecraft.resources.ResourceLocation id(UUID uuid) {
        return modifier(uuid, "", 0, AttributeOps.ADDITION).id();
    }
    *///?}

    /** {@code instance.removeModifier(uuid)}. */
    public static void removeModifier(AttributeInstance instance, UUID uuid) {
        //? if < 1.21.1 {
        instance.removeModifier(uuid);
        //?} else {
        /*instance.removeModifier(id(uuid));
        *///?}
    }

    /** {@code instance.getModifier(uuid)}. */
    public static AttributeModifier getModifier(AttributeInstance instance, UUID uuid) {
        //? if < 1.21.1 {
        return instance.getModifier(uuid);
        //?} else {
        /*return instance.getModifier(id(uuid));
        *///?}
    }

    //? if >= 1.21.1 {
    /*/^*
     * Baut die 1.21.1-Komponente aus der 1.20.1-Slotlogik: jeder Slot wird abgefragt, seine Modifikatoren
     * landen in der Slotgruppe genau dieses Slots.
     ^/
    public static net.minecraft.world.item.component.ItemAttributeModifiers fromSlots(
            java.util.function.Function<net.minecraft.world.entity.EquipmentSlot,
                    com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier>> perSlot) {
        net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            var map = perSlot.apply(slot);
            if (map == null || map.isEmpty()) continue;
            net.minecraft.world.entity.EquipmentSlotGroup group = net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
            map.forEach((attr, mod) -> builder.add(attr, mod, group));
        }
        return builder.build();
    }

    /^* Gegenrichtung: die Modifikatoren einer Komponente, die fuer {@code slot} gelten (1.20.1 {@code super.getDefaultAttributeModifiers(slot)}). ^/
    public static com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier> forSlot(
            net.minecraft.world.item.component.ItemAttributeModifiers mods, net.minecraft.world.entity.EquipmentSlot slot) {
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier> builder =
                com.google.common.collect.ImmutableMultimap.builder();
        mods.forEach(slot, builder::put);
        return builder.build();
    }
    *///?}
}
