package com.hbm_m.armormod.client;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.datagen.assets.ModItemTagProvider;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.lib.RefStrings;
//?}

/**
 * Этот класс отвечает за добавление общих подсказок ко всем предметам-модификациям брони.
 * Он слушает событие на стороне клиента через Architectury API.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)//?}
public class ModTooltipHandler {

    //? if forge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        init();
    }
    //?}

    public static void init() {
        ClientTooltipEvent.ITEM.register(ModTooltipHandler::onItemTooltip);
    }

    private static void onItemTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(stack, tooltip);
    }

    public static void addTooltip(net.minecraft.world.item.ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof ItemArmorMod mod)) return;

        // Добавляем отступ, чтобы отделить наши строки от строк эффектов
        tooltip.add(Component.empty());

        // Секция "Применяется к:"
        tooltip.add(Component.translatable("tooltip.hbm_m.applies_to").withStyle(ChatFormatting.DARK_PURPLE));

        boolean requiresHelmet = stack.is(ModItemTagProvider.REQUIRES_HELMET);
        boolean requiresChestplate = stack.is(ModItemTagProvider.REQUIRES_CHESTPLATE);
        boolean requiresLeggings = stack.is(ModItemTagProvider.REQUIRES_LEGGINGS);
        boolean requiresBoots = stack.is(ModItemTagProvider.REQUIRES_BOOTS);

        if (requiresHelmet && requiresChestplate && requiresLeggings && requiresBoots) {
            tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.hbm_m.armor.all")).withStyle(ChatFormatting.GRAY));
        } else {
            if (requiresHelmet) tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.hbm_m.helmet")).withStyle(ChatFormatting.GRAY));
            if (requiresChestplate) tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.hbm_m.chestplate")).withStyle(ChatFormatting.GRAY));
            if (requiresLeggings) tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.hbm_m.leggings")).withStyle(ChatFormatting.GRAY));
            if (requiresBoots) tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.hbm_m.boots")).withStyle(ChatFormatting.GRAY));
        }

        // Секция "Slot:"
        tooltip.add(Component.translatable("tooltip.hbm_m.slot").withStyle(ChatFormatting.DARK_PURPLE));

        Component slotName = switch (mod.type) {
            case 0 -> Component.translatable("tooltip.hbm_m.helmet");
            case 1 -> Component.translatable("tooltip.hbm_m.chestplate");
            case 2 -> Component.translatable("tooltip.hbm_m.leggings");
            case 3 -> Component.translatable("tooltip.hbm_m.boots");
            case 4 -> Component.translatable("tooltip.hbm_m.armor_table.servos_slot");
            case 7 -> Component.translatable("tooltip.hbm_m.armor_table.special_slot");
            case 6 -> Component.translatable("tooltip.hbm_m.armor_table.plating_slot");
            case 5 -> Component.translatable("tooltip.hbm_m.armor_table.casing_slot");
            case 8 -> Component.translatable("tooltip.hbm_m.armor_table.battery_slot");
            default -> Component.literal("Unknown");
        };

        tooltip.add(Component.literal("  ").append(slotName).withStyle(ChatFormatting.GRAY));
    }
}