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

/** 1:1 {@code ItemModWD40}: -80% Ruestungsverschleiss, +4 HP, rote Funken bei Treffer. */
public class ItemModWD40 extends ItemArmorMod {

    public ItemModWD40() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Highly reduces damage taken by armor, +2 HP", blink(ChatFormatting.BLUE, ChatFormatting.YELLOW)));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(blink(ChatFormatting.BLUE, ChatFormatting.YELLOW), stack, " (-80% armor wear / +2 HP)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (!entity.level().isClientSide && armor.getDamageValue() > 0 && entity.getRandom().nextInt(5) != 0) {
            armor.setDamageValue(armor.getDamageValue() - 1);
        }
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getModifiers(ItemStack armor) {
        com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> multimap = com.google.common.collect.HashMultimap.create();
        multimap.put(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, modifier(armor, "NTM Armor Mod Health", 4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return multimap;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide && entity.hurtTime > 0) {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putString("type", "vanillaExt");
            data.putString("mode", "reddust");
            data.putDouble("posX", entity.getX() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 2);
            data.putDouble("posY", entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight());
            data.putDouble("posZ", entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * entity.getBbWidth() * 2);
            data.putDouble("mX", 0.01);
            data.putDouble("mY", 0.5);
            data.putDouble("mZ", 0.8);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }
}
