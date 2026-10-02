package com.hbm_m.armormod.client;

import com.hbm_m.inventory.gui.GUIArmorTable;
import com.hbm_m.powerarmor.ArmorTooltipHandler;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register(ArmorModificationClientEvents::onArmorTooltip);
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, tooltip, context, flag) -> onArmorTooltip(stack, tooltip, flag));
        *///?}
    }

    private static void onArmorTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        boolean isArmorTableOpen = Minecraft.getInstance().screen instanceof GUIArmorTable;
        ArmorTooltipHandler.drawTooltip(stack, tooltip, isArmorTableOpen);
    }
}