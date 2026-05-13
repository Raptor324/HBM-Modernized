package com.hbm_m.armormod.client;

import com.hbm_m.inventory.gui.GUIArmorTable;
import com.hbm_m.powerarmor.ArmorTooltipHandler;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.main.MainRegistry;
//?}

import java.util.List;

//? if forge {
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public class ArmorModificationClientEvents {

    //? if forge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        init();
    }
    //?}

    public static void init() {
        ClientTooltipEvent.ITEM.register(ArmorModificationClientEvents::onArmorTooltip);
    }

    private static void onArmorTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        // Работаем только с броней
        if (!(stack.getItem() instanceof ArmorItem)) {
            return;
        }

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