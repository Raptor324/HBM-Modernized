package com.hbm_m.armormod.item;

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
//? if >= 1.21.1 {
/*import net.minecraft.core.Holder;
*///?}

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
        //? if < 1.21.1 {
        return List.of(Component.literal("+" + this.health + " " + Component.translatable(Attributes.MAX_HEALTH.getDescriptionId()).getString()).withStyle(ChatFormatting.RED));
        //?} else {
        /*return List.of(Component.literal("+" + this.health + " " + Component.translatable(Attributes.MAX_HEALTH.value().getDescriptionId()).getString()).withStyle(ChatFormatting.RED));
        *///?}
    }

    @Override
    public Multimap<
            //? if < 1.21.1 {
            Attribute//?} else {
            /*Holder<Attribute>*///?}
            , AttributeModifier> getModifiers(ItemStack armor) {
        Multimap<
                //? if < 1.21.1 {
                Attribute//?} else {
                /*Holder<Attribute>*///?}
                , AttributeModifier> multimap = HashMultimap.create();
        multimap.put(
            Attributes.MAX_HEALTH,
            createModifier(armor,
                    //? if < 1.21.1 {
                    Attributes.MAX_HEALTH//?} else {
                    /*Attributes.MAX_HEALTH.value()*///?}
                    , "HBM Armor Mod Health", this.health,
                    //? if < 1.21.1 {
                    AttributeModifier.Operation.ADDITION//?} else {
                    /*AttributeModifier.Operation.ADD_VALUE*///?}
            )
        );

        return multimap;
    }
}