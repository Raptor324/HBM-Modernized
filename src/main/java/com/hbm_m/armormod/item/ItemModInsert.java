package com.hbm_m.armormod.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemModInsert}: Schutzplatten mit Haltbarkeit, Schadens-/Projektil-/Explosionsfaktor, Tempo. */
public class ItemModInsert extends ItemArmorMod {

    float damageMod;
    float projectileMod;
    float explosionMod;
    float speed;

    public ItemModInsert(int durability, float damageMod, float projectileMod, float explosionMod, float speed) {
        super(new Properties().durability(durability), ArmorModificationHelper.kevlar, false, true, false, false);
        this.damageMod = damageMod;
        this.projectileMod = projectileMod;
        this.explosionMod = explosionMod;
        this.speed = speed;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        if (damageMod != 1F)
            list.add(line((damageMod < 1 ? "-" : "+") + Math.abs(Math.round((1F - damageMod) * 100)) + "% damage", ChatFormatting.RED));
        if (projectileMod != 1F)
            list.add(line("-" + Math.round((1F - projectileMod) * 100) + "% projectile damage", ChatFormatting.YELLOW));
        if (explosionMod != 1F)
            list.add(line("-" + Math.round((1F - explosionMod) * 100) + "% explosion damage", ChatFormatting.YELLOW));
        if (speed != 1F)
            list.add(line("-" + Math.round((1F - speed) * 100) + "% speed", ChatFormatting.BLUE));
        if (this == ModItems.INSERT_POLONIUM.get())
            list.add(line("+100 RAD/s", ChatFormatting.DARK_RED));

        list.add(Component.literal((stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() + "HP"));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        List<String> desc = new java.util.ArrayList<>();
        if (damageMod != 1F)
            desc.add((damageMod < 1 ? "-" : "+") + Math.abs(Math.round((1F - damageMod) * 100)) + "% dmg");
        if (projectileMod != 1F)
            desc.add("-" + Math.round((1F - projectileMod) * 100) + "% proj");
        if (explosionMod != 1F)
            desc.add("-" + Math.round((1F - explosionMod) * 100) + "% exp");
        if (explosionMod != 1F)
            desc.add("-" + Math.round((1F - speed) * 100) + "% speed");
        if (this == ModItems.INSERT_POLONIUM.get())
            desc.add("+100 RAD/s");

        String join = String.join(" / ", desc);
        list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (" + join + " / " + (stack.getMaxDamage() - stack.getDamageValue()) + "HP)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        event.amount *= damageMod;

        if (event.source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE))
            event.amount *= projectileMod;

        if (event.source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION))
            event.amount *= explosionMod;

        ItemStack insert = ArmorModificationHelper.pryMods(armor)[ArmorModificationHelper.kevlar];

        if (insert == null || insert.isEmpty())
            return;

        insert.setDamageValue(insert.getDamageValue() + 1);

        if (!entity.level().isClientSide && this == ModItems.INSERT_ERA.get()) {
            entity.level().explode(entity, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 0.05F, false, Level.ExplosionInteraction.NONE);
        }

        if (insert.getDamageValue() >= insert.getMaxDamage()) {
            ArmorModificationHelper.removeMod(armor, ArmorModificationHelper.kevlar);
        } else {
            ArmorModificationHelper.applyMod(armor, insert);
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide && this == ModItems.INSERT_POLONIUM.get()) {
            HbmLivingProps.incrementRadiation(entity, 100F);
        }
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        if (speed == 1)
            return null;
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
        multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, modifier(armor, "NTM Armor Mod Speed", -1F + speed, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        return multimap;
    }
}
