package com.hbm_m.armormod.item;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Multimap;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.armor.ItemArmorMod}: Slot-Typ plus die vier Flags, an welche
 * Ruestungsteile die Mod passt. Haken: {@link #modUpdate} (jeder Tick), {@link #modDamage}
 * ({@code LivingHurtEvent}), {@link #getModifiers} (Attribute), {@link #addDesc} (Zeile im Tooltip
 * der Ruestung unter "Mods:").
 */
public abstract class ItemArmorMod extends Item implements ITooltipProvider {

    /** Original {@code LivingHurtEvent}: Quelle, veraenderbarer Schaden, Abbruch. */
    public static final class Hurt {
        public final DamageSource source;
        public float amount;
        public boolean canceled;

        public Hurt(DamageSource source, float amount) {
            this.source = source;
            this.amount = amount;
        }
    }

    public final int type;
    public final boolean helmet;
    public final boolean chestplate;
    public final boolean leggings;
    public final boolean boots;

    public ItemArmorMod(Properties properties, int type, boolean helmet, boolean chestplate, boolean leggings, boolean boots) {
        super(properties);
        this.type = type;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    /** Die eigenen Zeilen der Mod ({@code addInformation} vor dem {@code super}-Aufruf). */
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) { }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        addInformation(stack, level, list);

        list.add(Component.translatable("armorMod.applicableTo").withStyle(ChatFormatting.DARK_PURPLE));

        if (helmet && chestplate && leggings && boots) {
            list.add(Component.literal("  ").append(Component.translatable("armorMod.all")));
        } else {
            if (helmet) list.add(Component.literal("  ").append(Component.translatable("armorMod.helmets")));
            if (chestplate) list.add(Component.literal("  ").append(Component.translatable("armorMod.chestplates")));
            if (leggings) list.add(Component.literal("  ").append(Component.translatable("armorMod.leggings")));
            if (boots) list.add(Component.literal("  ").append(Component.translatable("armorMod.boots")));
        }
        list.add(Component.literal("Slot:").withStyle(ChatFormatting.DARK_PURPLE));

        String key = switch (this.type) {
            case ArmorModificationHelper.helmet_only -> "armorMod.type.helmet";
            case ArmorModificationHelper.plate_only -> "armorMod.type.chestplate";
            case ArmorModificationHelper.legs_only -> "armorMod.type.leggings";
            case ArmorModificationHelper.boots_only -> "armorMod.type.boots";
            case ArmorModificationHelper.servos -> "armorMod.type.servo";
            case ArmorModificationHelper.cladding -> "armorMod.type.cladding";
            case ArmorModificationHelper.kevlar -> "armorMod.type.insert";
            case ArmorModificationHelper.extra -> "armorMod.type.special";
            case ArmorModificationHelper.battery -> "armorMod.type.battery";
            default -> null;
        };
        if (key != null) list.add(Component.literal("  ").append(Component.translatable(key)));
    }

    /** Zeile unter "Mods:" im Tooltip der Ruestung. */
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(stack.getHoverName());
    }

    public void modUpdate(LivingEntity entity, ItemStack armor) { }

    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) { }

    @Nullable
    public Multimap<
            //? if < 1.21.1 {
            Attribute//?} else {
            /*net.minecraft.core.Holder<Attribute>*///?}
            , AttributeModifier> getModifiers(ItemStack armor) {
        return null;
    }

    /** Original {@code ((ItemArmor) armor.getItem()).armorType}: 0 Helm, 1 Brust, 2 Beine, 3 Stiefel. */
    public static int armorType(ItemStack armor) {
        if (armor.getItem() instanceof ArmorItem a) {
            return switch (a.getType()) {
                case HELMET -> 0;
                case CHESTPLATE -> 1;
                case LEGGINGS -> 2;
                case BOOTS -> 3;
                //? if >= 1.21.1 {
                /*default -> 1;
                *///?}
            };
        }
        return 0;
    }

    /** {@code new AttributeModifier(ArmorModHandler.UUIDs[armorType], name, value, op)}. */
    protected static AttributeModifier modifier(ItemStack armor, String name, double value, AttributeModifier.Operation op) {
        return PlatformHooks.attributeModifier(ArmorModificationHelper.UUIDs[armorType(armor)], name, value, op);
    }

    protected static AttributeModifier modifier(UUID uuid, String name, double value, AttributeModifier.Operation op) {
        return PlatformHooks.attributeModifier(uuid, name, value, op);
    }

    /** Original: Farbwechsel im Halbsekundentakt ({@code System.currentTimeMillis() % 1000 < 500}). */
    protected static ChatFormatting blink(ChatFormatting a, ChatFormatting b) {
        return System.currentTimeMillis() % 1000 < 500 ? a : b;
    }

    protected static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    protected static Component descLine(ChatFormatting color, ItemStack stack, String suffix) {
        return Component.literal("  ").append(stack.getHoverName()).append(suffix).withStyle(color);
    }
}
