package com.hbm_m.platform;

import java.util.UUID;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Versionsfassade fuer Attribut-Modifikatoren an Items und Wesen.
 * Единая точка истины для атрибутов (бывшие AttributeOps / PlatformHooks.attribute* / MobHooks.removeModifier).
 *
 * <p>1.20.1: Modifikatoren tragen eine UUID, Items liefern {@code getDefaultAttributeModifiers(EquipmentSlot)}
 * als {@code Multimap<Attribute, AttributeModifier>}.<br>
 * 1.21.1: Modifikatoren tragen eine {@code ResourceLocation} (aus der UUID abgeleitet, siehe
 * {@link #id(UUID)}), Items liefern {@code getDefaultAttributeModifiers(ItemStack)} als
 * {@code ItemAttributeModifiers}-Komponente mit Slotgruppen (siehe {@code fromSlots}/{@code forSlot}).</p>
 */
public final class AttributeHooks {
    private AttributeHooks() {}

    /** Vanilla {@code Item.BASE_ATTACK_DAMAGE_UUID} (1.20.1 nur fuer Unterklassen sichtbar). */
    public static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    /** Vanilla {@code Item.BASE_ATTACK_SPEED_UUID}. */
    public static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    // Operation-константы (бывший AttributeOps): 1.21.1 переименовал
    // ADDITION -> ADD_VALUE, MULTIPLY_BASE -> ADD_MULTIPLIED_BASE, MULTIPLY_TOTAL -> ADD_MULTIPLIED_TOTAL.
    //? if < 1.21.1 {
    public static final AttributeModifier.Operation ADDITION = AttributeModifier.Operation.ADDITION;
    public static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.MULTIPLY_BASE;
    public static final AttributeModifier.Operation MULTIPLY_TOTAL = AttributeModifier.Operation.MULTIPLY_TOTAL;
    //?} else {
    /*public static final AttributeModifier.Operation ADDITION = AttributeModifier.Operation.ADD_VALUE;
    public static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    public static final AttributeModifier.Operation MULTIPLY_TOTAL = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
    *///?}

    /**
     * {@code new AttributeModifier(uuid, name, value, op)} (1.20.1); на 1.21.1 UUID-конструктор
     * удалён — id генерируется в {@link #id(UUID)} (единая точка, включая vanilla BASE_ATTACK_*:
     * Tooltip на 1.21.1 считает базовый урон игрока так же, как 1.20.1).
     */
    public static AttributeModifier modifier(UUID uuid, String name, double value, AttributeModifier.Operation op) {
        //? if < 1.21.1 {
        return new AttributeModifier(uuid, name, value, op);
        //?} else {
        /*return new AttributeModifier(id(uuid), value, op);
        *///?}
    }

    //? if >= 1.21.1 {
    /*// ResourceLayout-Sanitizing einmalig kompiliert (statt replaceAll -> Pattern.compile pro Aufruf).
    private static final java.util.regex.Pattern INVALID_ID_CHARS = java.util.regex.Pattern.compile("[^a-z0-9/._-]");
    *///?}

    /**
     * {@code new AttributeModifier(name, value, op)} (1.20.1); на 1.21.1 string-only конструктор
     * удалён — имя санитизируется под ResourceLayout (скобки/пробелы -> '_').
     */
    public static AttributeModifier modifier(String name, double value, AttributeModifier.Operation op) {
        //? if < 1.21.1 {
        return new AttributeModifier(name, value, op);
        //?} else {
        /*return new AttributeModifier(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID,
                        INVALID_ID_CHARS.matcher(name.toLowerCase(java.util.Locale.ROOT)).replaceAll("_")),
                value, op);
        *///?}
    }

    /** Есть ли на инстансе модификатор, созданный {@link #modifier(UUID, String, double, AttributeModifier.Operation)}? */
    public static boolean hasModifier(AttributeInstance instance, UUID uuid) {
        //? if < 1.21.1 {
        return instance.getModifier(uuid) != null;
        //?} else {
        /*return instance.hasModifier(id(uuid));
        *///?}
    }

    //? if >= 1.21.1 {
    /*// Kennung eines aus einer UUID entstandenen Modifikators — direkt, ohne dummy-AttributeModifier
    // (fruehere Version baute new AttributeModifier(...).id() nur fuer die id — Allokation pro
    // getModifier/removeModifier-Aufruf). Muss mit modifier(...) uebereinstimmen!
    public static net.minecraft.resources.ResourceLocation id(UUID uuid) {
        if (BASE_ATTACK_DAMAGE_UUID.equals(uuid)) return net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID;
        if (BASE_ATTACK_SPEED_UUID.equals(uuid)) return net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID;
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                com.hbm_m.lib.RefStrings.MODID, "am_" + uuid.toString().replace('-', '_'));
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
    /*// Baut die 1.21.1-Komponente aus der 1.20.1-Slotlogik: jeder Slot wird abgefragt, seine
    // Modifikatoren landen in der Slotgruppe genau dieses Slots.
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

    // Gegenrichtung: die Modifikatoren einer Komponente, die fuer {@code slot} gelten
    // (1.20.1 {@code super.getDefaultAttributeModifiers(slot)}).
    public static com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier> forSlot(
            net.minecraft.world.item.component.ItemAttributeModifiers mods, net.minecraft.world.entity.EquipmentSlot slot) {
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeModifier> builder =
                com.google.common.collect.ImmutableMultimap.builder();
        mods.forEach(slot, builder::put);
        return builder.build();
    }
    *///?}
}
