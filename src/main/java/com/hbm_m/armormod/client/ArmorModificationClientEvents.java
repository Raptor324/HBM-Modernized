package com.hbm_m.armormod.client;

import com.hbm_m.client.overlay.GUIArmorTable;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.ArmorTooltipHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ArmorModificationClientEvents {

    @SubscribeEvent
    public static void onArmorTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // Работаем только с броней
        if (!(stack.getItem() instanceof ArmorItem)) {
            return;
        }

        List<Component> tooltip = event.getToolTip();

        // 2. Модификации (показываем, если Shift ИЛИ открыт стол)
        boolean isArmorTableOpen = Minecraft.getInstance().screen instanceof GUIArmorTable;
        ArmorTooltipHandler.getModificationsTooltip(stack, isArmorTableOpen).ifPresent(tooltip::addAll);

        // 3. Сопротивления FSB (Фиолетовый текст) - только с Shift
        ArmorTooltipHandler.getFSBResistancesTooltip(stack).ifPresent(tooltip::addAll);

        // 4. Подсказка "Зажмите Shift" (отображается, если есть что скрывать и шифт НЕ нажат)
        ArmorTooltipHandler.getContextualHelpTooltip(stack, isArmorTableOpen).ifPresent(tooltip::add);

        // 5. Рад защита (в самом низу)
        ArmorTooltipHandler.getRadResistanceTooltip(stack).ifPresent(tooltip::add);
    }
}