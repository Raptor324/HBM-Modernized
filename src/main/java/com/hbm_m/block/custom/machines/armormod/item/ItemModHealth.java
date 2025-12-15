package com.hbm_m.block.custom.machines.armormod.item;

// Это мод, который увеличивает максимальное здоровье игрока при установке на броню.
// Он подходит для всех типов брони и добавляет соответствующую строку в тултип
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemModHealth extends ItemArmorMod {

    private final double health;


    public ItemModHealth(Properties pProperties, int type, double health) {
        // Указываем, что этот мод подходит для всех типов брони
        super(pProperties, type);
        this.health = health;
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.literal("+" + this.health + " " + Component.translatable(Attributes.MAX_HEALTH.getDescriptionId()).getString()).withStyle(ChatFormatting.RED));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getModifiers(ItemStack armor) {
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        multimap.put(
            Attributes.MAX_HEALTH,
            createModifier(armor, Attributes.MAX_HEALTH, "HBM Armor Mod Health", this.health, AttributeModifier.Operation.ADDITION)
        );

        return multimap;
    }
}