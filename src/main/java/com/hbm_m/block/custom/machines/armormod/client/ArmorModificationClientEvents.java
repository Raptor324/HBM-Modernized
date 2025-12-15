package com.hbm_m.block.custom.machines.armormod.client;

// Этот класс отвечает за добавление подсказок к броне с модификациями
import com.hbm_m.client.overlay.GUIArmorTable;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ArmorModificationClientEvents {

    @SubscribeEvent
    public static void onArmorTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ArmorItem)) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        Optional<Component> radResistanceLine = ArmorTooltipHandler.getRadResistanceTooltip(stack);
        radResistanceLine.ifPresent(line -> tooltip.add(1, line));

        
        // 1. Проверяем, является ли текущий открытый экран нашим столом
        boolean isArmorTableOpen = Minecraft.getInstance().screen instanceof GUIArmorTable;
        
        // 2. Вызываем наш обновленный хелпер, передавая ему этот флаг
        Optional<List<Component>> modsTooltipOptional = ArmorTooltipHandler.getModificationsTooltip(stack, isArmorTableOpen);
        
        // 3. Добавляем результат в тултип
        modsTooltipOptional.ifPresent(tooltip::addAll);
    }
}